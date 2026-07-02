import type { DatabaseShape, Student, Teacher } from "./types";
import {
  attendanceRate,
  studentAverage,
  balanceDue,
  totalPaidFor,
  liabilityMonths,
  currencySymbol,
  formatMoney,
} from "./analytics";
import { GRADES, gradeLabel } from "./constants";

/** Constructs the structured prompt required by the spec. */
export function buildPrompt(db: DatabaseShape, student: Student): string {
  const sym = currencySymbol(db);
  const attRate = Math.round(attendanceRate(db, {}));
  const avg = studentAverage(db, student.id);
  const due = balanceDue(db, student);
  const paid = totalPaidFor(db, student.id);
  const months = liabilityMonths(student);

  const grades = db.examGrades
    .filter((g) => g.studentId === student.id)
    .map((g) => {
      const exam = db.exams.find((e) => e.id === g.examId);
      return exam
        ? `- ${exam.name}: ${g.obtainedGrade}/${exam.maxGrade} (${Math.round(
            (g.obtainedGrade / exam.maxGrade) * 100,
          )}%)`
        : "";
    })
    .filter(Boolean)
    .join("\n");

  const homework = db.assignments
    .filter((a) => student.groupIds?.includes(a.groupId))
    .map((a) => `- ${a.title} (due ${new Date(a.dueDate).toLocaleDateString()})`)
    .join("\n");

  return `You are an expert academic advisor for an educational center.
Analyze the following student and provide:
1. A concise performance summary
2. Key strengths
3. Areas needing improvement
4. Three actionable recommendations for the teacher and parent

STUDENT:
- Name: ${student.name}
- Code: ${student.id}
- Grade level: ${student.grade}

METRICS:
- Attendance rate: ${attRate}%
- Average exam grade: ${avg != null ? Math.round(avg) + "%" : "no data"}
- Fee liability: ${months} months, total paid ${formatMoney(paid, sym)}, balance due ${formatMoney(due, sym)}

EXAM GRADES:
${grades || "- none"}

ACTIVE HOMEWORK:
${homework || "- none"}

Provide a warm, encouraging, and specific analysis.`;
}

const ENDPOINT =
  "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";

export interface AiResult {
  text: string;
  usedApi: boolean;
}

/** Calls Gemini if an API key is present, otherwise returns a local mock. */
export async function generateInsight(
  db: DatabaseShape,
  student: Student,
  apiKey?: string,
): Promise<AiResult> {
  const prompt = buildPrompt(db, student);

  if (apiKey && apiKey.trim()) {
    try {
      const res = await fetch(`${ENDPOINT}?key=${apiKey.trim()}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          contents: [{ parts: [{ text: prompt }] }],
          generationConfig: { temperature: 0.7, maxOutputTokens: 800 },
        }),
      });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();
      const text =
        data?.candidates?.[0]?.content?.parts?.[0]?.text ??
        "No response text returned.";
      return { text, usedApi: true };
    } catch (e) {
      return {
        text: mockAnalysis(db, student) + `\n\n⚠️ Gemini call failed (${(e as Error).message}). Showing local analysis.`,
        usedApi: false,
      };
    }
  }
  return { text: mockAnalysis(db, student), usedApi: false };
}

/* ========================= Local AI Command Processor ========================= */

export interface LocalCommandAction {
  type: "upsert" | "remove";
  coll: string;
  item?: any;
  id?: string;
}

export interface LocalCommandResult {
  text: string;
  action?: LocalCommandAction;
}

/**
 * Parses a natural language command (Arabic or English) and returns
 * either informational text or a mutation action to execute.
 */
export function processLocalCommand(
  input: string,
  db: DatabaseShape,
  lang: "ar" | "en",
): LocalCommandResult {
  const t = input.trim();
  const isAr = lang === "ar";

  // --- ADD STUDENT ---
  const addStudentMatch = parseAddStudent(t, isAr);
  if (addStudentMatch) {
    const { name, grade } = addStudentMatch;
    const existing = db.students.find((s) => s.name === name);
    if (existing) {
      return { text: isAr ? `الطالب "${name}" موجود بالفعل برقم ${existing.id}` : `Student "${name}" already exists with code ${existing.id}` };
    }
    const gradeId = grade ? findGrade(grade, isAr) : db.students.length > 0 ? db.students[0].grade : GRADES[0].id;
    const newStudent: Student = {
      id: `S${Date.now()}`,
      name,
      grade: gradeId,
      groupIds: [],
      teachers: [],
      studentPhone: "",
      parentName: "",
      parentPhone: "",
      discount: 0,
      isExempt: false,
      qrCode: "",
      registrationDate: Date.now(),
      lastUpdated: Date.now(),
    };
    return {
      text: isAr ? `✅ تم تجهيز الطالب "${name}"${grade ? ` في ${gradeLabel(gradeId, lang)}` : ""}. اضغط زر التأكيد للإضافة.` : `✅ Student "${name}" ready${grade ? ` in ${gradeLabel(gradeId, lang)}` : ""}. Click confirm to add.`,
      action: { type: "upsert", coll: "students", item: newStudent },
    };
  }

  // --- DELETE STUDENT ---
  const delStudentMatch = parseDeleteStudent(t, isAr, db);
  if (delStudentMatch) {
    const student = delStudentMatch;
    return {
      text: isAr ? `🗑️ هل تريد حذف الطالب "${student.name}" (${student.id})؟ اضغط للتأكيد.` : `🗑️ Delete student "${student.name}" (${student.id})? Click to confirm.`,
      action: { type: "remove", coll: "students", id: student.id },
    };
  }

  // --- SHOW STUDENT REPORT ---
  const reportMatch = parseShowStudentReport(t, isAr, db);
  if (reportMatch) {
    const student = reportMatch;
    return { text: buildStudentReport(db, student, isAr) };
  }

  // --- LIST STUDENTS ---
  if (matchesList(t, "students", isAr)) {
    if (db.students.length === 0) {
      return { text: isAr ? "لا يوجد طلاب مسجلون." : "No students registered." };
    }
    const lines = db.students.map((s, i) =>
      `${i + 1}. ${s.name} — ${gradeLabel(s.grade, lang)}${s.groupIds.length ? ` (${s.groupIds.length} مجموعة)` : ""}`
    );
    return { text: (isAr ? "📋 قائمة الطلاب:\n" : "📋 Student List:\n") + lines.join("\n") };
  }

  // --- LIST TEACHERS ---
  if (matchesList(t, "teachers", isAr)) {
    if (db.teachers.length === 0) {
      return { text: isAr ? "لا يوجد معلمون مسجلون." : "No teachers registered." };
    }
    const lines = db.teachers.map((s, i) =>
      `${i + 1}. ${s.name} — ${s.subjects?.join(", ") || ""}`
    );
    return { text: (isAr ? "📋 قائمة المعلمين:\n" : "📋 Teacher List:\n") + lines.join("\n") };
  }

  // --- TEACHER REPORT ---
  const teacherReportMatch = parseTeacherReport(t, isAr, db);
  if (teacherReportMatch) {
    const teacher = teacherReportMatch;
    const students = db.students.filter((s) => s.teachers?.some((tr) => tr.teacherId === teacher.id));
    return {
      text: isAr
        ? `📊 تقرير المعلم "${teacher.name}":\n- المواد: ${teacher.subjects?.join(", ") || "—"}\n- عدد الطلاب: ${students.length}\n- الطلاب: ${students.map((s) => s.name).join(", ") || "لا يوجد"}`
        : `📊 Teacher Report "${teacher.name}":\n- Subjects: ${teacher.subjects?.join(", ") || "—"}\n- Students: ${students.length}\n- Names: ${students.map((s) => s.name).join(", ") || "none"}`,
    };
  }

  // --- CENTER STATS ---
  if (matchesStat(t, isAr)) {
    const totalStudents = db.students.length;
    const totalTeachers = db.teachers.length;
    const totalGroups = db.groups.length;
    const monthlyRev = db.payments.filter((p) => {
      const d = new Date(p.date);
      const now = new Date();
      return d.getMonth() === now.getMonth() && d.getFullYear() === now.getFullYear();
    }).reduce((s, p) => s + p.amount, 0);
    return {
      text: isAr
        ? `📈 إحصائيات السنتر:\n- الطلاب: ${totalStudents}\n- المعلمين: ${totalTeachers}\n- المجموعات: ${totalGroups}\n- الإيراد الشهري: ${monthlyRev} ج.م`
        : `📈 Center Stats:\n- Students: ${totalStudents}\n- Teachers: ${totalTeachers}\n- Groups: ${totalGroups}\n- Monthly Revenue: ${monthlyRev} EGP`,
    };
  }

  // --- HELP ---
  if (matchesHelp(t, isAr)) {
    return { text: getHelpText(isAr) };
  }

  // Fallback — analyze the most recent student if possible
  const latest = db.students[db.students.length - 1];
  if (latest) {
    return { text: isAr ? "لم أفهم الأمر. إليك تحليل سريع لآخر طالب:" : "I didn't understand. Here's a quick analysis of the latest student:" + "\n\n" + mockAnalysis(db, latest) };
  }
  return { text: getHelpText(isAr) };
}

/* ====================== Parser Helpers ====================== */

function parseAddStudent(t: string, isAr: boolean): { name: string; grade?: string } | null {
  if (isAr) {
    const m = t.match(/اضف|أضف|إضافة|اضافة|تسجيل/i);
    if (!m) return null;
    const nameM = t.match(/(?:طالب\s*)?اسمه\s*[""']?([^""'،,\d]+)/i) || t.match(/طالب\s+[""']?([^""'،,\d]{3,})/i) || t.match(/(?:اسم|الطالب)\s*[""']?([^""'،,\d]{3,})/i);
    if (!nameM) return null;
    let name = nameM[1].trim().replace(/["']/g, "").replace(/\s+في\s+.*$/, "").trim();
    if (!name) return null;
    const gradeM = t.match(/(?:في|صف|grade|مرحلة|المستوى)\s*(.+)/i);
    const grade = gradeM ? gradeM[1].trim() : undefined;
    return { name, grade };
  }
  // English
  const m = t.match(/add|create|new|insert/i);
  if (!m) return null;
  const nameM = t.match(/(?:student\s+)?(?:named|called|name\s*:?)\s*[""']?([^""',\d]{3,})/i) || t.match(/student\s+[""']?([^""',\d]{3,})/i);
  if (!nameM) return null;
  let name = nameM[1].trim().replace(/["']/g, "").replace(/\s+in\s+.*$/, "").replace(/\s+grade\s+.*$/i, "").trim();
  if (!name) return null;
  const gradeM = t.match(/(?:in|grade|for)\s+(.+)/i);
  const grade = gradeM ? gradeM[1].trim() : undefined;
  return { name, grade };
}

function parseDeleteStudent(t: string, isAr: boolean, db: DatabaseShape): Student | null {
  if (isAr) {
    const m = t.match(/حذف|مسح|إزالة|احذف/i);
    if (!m) return null;
    const nameM = t.match(/(?:طالب|الطالب)\s*[""']?([^""'،,\d]{2,})/i) || t.match(/[""']?([^""'،,\d]{3,})/i);
    if (!nameM) return null;
    const name = nameM[1].trim().replace(/["']/g, "");
    return findStudentByName(db, name) ?? null;
  }
  const m = t.match(/delete|remove|erase/i);
  if (!m) return null;
  const nameM = t.match(/(?:student\s+)?[""']?([^""',\d]{3,})/i);
  if (!nameM) return null;
  const name = nameM[1].trim().replace(/["']/g, "");
  return findStudentByName(db, name) ?? null;
}

function parseShowStudentReport(t: string, isAr: boolean, db: DatabaseShape): Student | null {
  let name: string | undefined;
  if (isAr) {
    const m = t.match(/تقرير|report/i);
    if (!m) return null;
    const nameM = t.match(/(?:طالب|الطالب)\s*[""']?([^""'،,\d]{2,})/i) || t.match(/عن\s*[""']?([^""'،,\d]{2,})/i);
    if (nameM) name = nameM[1].trim().replace(/["']/g, "");
  } else {
    const m = t.match(/report|overview|summary/i);
    if (!m) return null;
    const nameM = t.match(/(?:for|of|about|student)\s+[""']?([^""',\d]{2,})/i);
    if (nameM) name = nameM[1].trim().replace(/["']/g, "");
  }
  if (!name) return null;
  return findStudentByName(db, name) ?? null;
}

function parseTeacherReport(t: string, isAr: boolean, db: DatabaseShape): Teacher | null {
  let name: string | undefined;
  if (isAr) {
    const m = t.match(/تقرير|report|بيانات/i);
    if (!m) return null;
    const nameM = t.match(/(?:معلم|المعلم|أستاذ|الأستاذ)\s*[""']?([^""'،,\d]{2,})/i);
    if (nameM) name = nameM[1].trim().replace(/["']/g, "");
  } else {
    const m = t.match(/report|overview/i);
    if (!m) return null;
    const nameM = t.match(/(?:teacher|for teacher)\s+[""']?([^""',\d]{2,})/i);
    if (nameM) name = nameM[1].trim().replace(/["']/g, "");
  }
  if (!name) return null;
  const lower = name.toLowerCase();
  return db.teachers.find((t) => t.name.toLowerCase().includes(lower)) ?? null;
}

function findStudentByName(db: DatabaseShape, name: string): Student | undefined {
  const lower = name.toLowerCase();
  return db.students.find((s) => s.name.toLowerCase().includes(lower));
}

function findGrade(text: string, isAr: boolean): string {
  if (isAr) {
    for (const g of GRADES) {
      if (g.ar.includes(text) || text.includes(g.ar) || text.includes(g.id)) return g.id;
    }
  } else {
    for (const g of GRADES) {
      if (g.en.toLowerCase().includes(text.toLowerCase()) || text.includes(g.id)) return g.id;
    }
  }
  return GRADES[0].id;
}

function matchesList(t: string, type: "students" | "teachers", isAr: boolean): boolean {
  if (isAr) {
    const kw = type === "students" ? /(?:قائمة|عرض|الطلاب|كل الطلاب|اظهر)/i : /(?:قائمة|عرض|المعلمين|كل المعلمين|اظهر)/i;
    const target = type === "students" ? /طالب|طلاب/i : /معلم|مدرس|المعلمين/i;
    return kw.test(t) && target.test(t);
  }
  const kw = /list|show|display|all/i;
  const target = type === "students" ? /student/i : /teacher/i;
  return kw.test(t) && target.test(t);
}

function matchesStat(t: string, isAr: boolean): boolean {
  if (isAr) return /إحصاء|احصائيات|إحصائيات|stats|إجمالي/i.test(t);
  return /stat|overview|center info|dashboard/i.test(t);
}

function matchesHelp(t: string, isAr: boolean): boolean {
  if (isAr) return /مساعدة|مساعدة|الأوامر|help|تعليمات/i.test(t);
  return /help|commands|what can you/i.test(t);
}

function getHelpText(isAr: boolean): string {
  return isAr
    ? `🤖 **المساعد الذكي المحلي**\n\nالأوامر المتاحة:\n- أضف طالب اسمه "أحمد"\n- أضف طالب اسمه "أحمد" في الصف الأول الابتدائي\n- احذف الطالب "أحمد"\n- تقرير عن الطالب "أحمد"\n- تقرير المعلم "أحمد"\n- قائمة الطلاب / قائمة المعلمين\n- إحصائيات السنتر\n- مساعدة`
    : `🤖 **Local AI Assistant**\n\nAvailable commands:\n- Add student named "Ahmed"\n- Add student "Ahmed" in Grade 1\n- Delete student "Ahmed"\n- Report for student "Ahmed"\n- Report for teacher "Ahmed"\n- List students / List teachers\n- Center statistics\n- Help`;
}

function buildStudentReport(db: DatabaseShape, student: Student, isAr: boolean): string {
  const attRate = Math.round(attendanceRate(db, {}));
  const avg = studentAverage(db, student.id);
  const due = balanceDue(db, student);
  const paid = totalPaidFor(db, student.id);
  const groups = db.groups.filter((g) => student.groupIds?.includes(g.id)).map((g) => g.name).join(", ") || "—";
  const teachers = student.teachers?.map((tr) => db.teachers.find((t) => t.id === tr.teacherId)?.name).filter(Boolean).join(", ") || "—";

  if (isAr) {
    return `📊 **تقرير الطالب**\n\n**${student.name}** (${student.id})\n- الصف: ${gradeLabel(student.grade, "ar")}\n- المجموعات: ${groups}\n- المعلمون: ${teachers}\n- نسبة الحضور: ${attRate}%\n- متوسط الدرجات: ${avg != null ? Math.round(avg) + "%" : "لا يوجد"}\n- المدفوع: ${paid} ج.م\n- المتبقي: ${due} ج.م`;
  }
  return `📊 **Student Report**\n\n**${student.name}** (${student.id})\n- Grade: ${gradeLabel(student.grade, "en")}\n- Groups: ${groups}\n- Teachers: ${teachers}\n- Attendance: ${attRate}%\n- Avg Grade: ${avg != null ? Math.round(avg) + "%" : "N/A"}\n- Paid: ${paid} EGP\n- Balance: ${due} EGP`;
}

function mockAnalysis(db: DatabaseShape, student: Student): string {
  const attRate = Math.round(attendanceRate(db, {}));
  const avg = studentAverage(db, student.id);
  const due = balanceDue(db, student);

  const lines: string[] = [];
  lines.push(`**Performance Summary**`);
  lines.push(
    `${student.name} (${student.id}) shows an attendance rate of ${attRate}% and an average exam grade of ${
      avg != null ? Math.round(avg) + "%" : "no graded exams yet"
    }.`,
  );

  lines.push(`\n**Key Strengths**`);
  if (attRate >= 85) lines.push("• Excellent commitment — attendance is well above the class average.");
  else lines.push("• Consistent presence in sessions.");
  if (avg != null && avg >= 75) lines.push("• Strong academic grasp, scoring above 75% on average.");

  lines.push(`\n**Areas for Improvement**`);
  if (attRate < 80) lines.push("• Attendance needs attention; missing sessions affects continuity.");
  if (avg != null && avg < 60) lines.push("• Core concepts need reinforcement — consider extra practice.");
  if (due > 0) lines.push(`• Outstanding fees of ${formatMoney(due)} may be a stressor; coordinate with the family.`);

  lines.push(`\n**Recommendations**`);
  lines.push("1. Assign targeted practice on the weakest recent exam topics.");
  lines.push("2. Set a short weekly check-in to keep motivation high.");
  lines.push("3. Share this report with the parent and agree on one shared goal for next month.");

  return lines.join("\n");
}
