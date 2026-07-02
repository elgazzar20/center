import { jsPDF } from "jspdf";
import type { DatabaseShape, Teacher } from "./types";
import type { Lang } from "../i18n/translations";
import {
  teacherRevenue, teacherCenterShare, studentsOfTeacher, groupsOfTeacher,
  currencySymbol, formatMoney,
} from "./analytics";
import { gradeLabel, formatTime12 } from "./constants";

function head(doc: jsPDF, db: DatabaseShape, teacher: Teacher) {
  const W = 210;
  doc.setFillColor(79, 70, 229);
  doc.rect(0, 0, W, 24, "F");
  doc.setTextColor(255, 255, 255);
  doc.setFont("helvetica", "bold");
  doc.setFontSize(15);
  doc.text(db.profile.name, 14, 11);
  doc.setFont("helvetica", "normal");
  doc.setFontSize(9);
  doc.text(`Teacher Report — ${teacher.name}`, 14, 17);
  doc.text(`Generated ${new Date().toLocaleDateString()}`, W - 14, 17, { align: "right" });
}

export function exportStudents(db: DatabaseShape, teacher: Teacher, lang: Lang) {
  const sym = currencySymbol(db);
  const doc = new jsPDF();
  head(doc, db, teacher);
  let y = 32;
  doc.setTextColor(30, 41, 59);
  doc.setFont("helvetica", "bold");
  doc.setFontSize(11);
  doc.text("Students", 14, y);
  y += 6;
  doc.setFontSize(8);
  doc.setTextColor(100, 116, 139);
  doc.text("Code", 14, y); doc.text("Name", 50, y); doc.text("Grade", 120, y); doc.text("Fee", 196, y, { align: "right" });
  y += 3;
  doc.setDrawColor(226, 232, 240); doc.line(14, y, 196, y); y += 5;
  doc.setTextColor(30, 41, 59); doc.setFont("helvetica", "normal");
  for (const s of studentsOfTeacher(db, teacher.id)) {
    if (y > 278) { doc.addPage(); y = 20; }
    const fee = s.teachers.find((x) => x.teacherId === teacher.id)?.fee ?? 0;
    doc.text(s.id, 14, y); doc.text(s.name.slice(0, 26), 50, y);
    doc.text(gradeLabel(s.grade, lang).slice(0, 20), 120, y); doc.text(formatMoney(fee, sym), 196, y, { align: "right" });
    y += 5.5;
  }
  doc.save(`${teacher.name}_students.pdf`);
}

export function exportFinancial(db: DatabaseShape, teacher: Teacher, _lang: Lang) {
  const sym = currencySymbol(db);
  const rev = teacherRevenue(db, teacher.id);
  const share = teacherCenterShare(db, teacher);
  const net = rev - share;
  const doc = new jsPDF();
  head(doc, db, teacher);
  let y = 34;
  const rows: [string, string][] = [
    ["Total Revenue", formatMoney(rev, sym)],
    ["Center Share", formatMoney(share, sym)],
    ["Net to Teacher", formatMoney(net, sym)],
    ["Pay Model", teacher.payType === "percentage" ? `${teacher.commissionRate}% of revenue` : `Fixed ${formatMoney(teacher.fixedAmount, sym)}`],
    ["Students", String(studentsOfTeacher(db, teacher.id).length)],
  ];
  doc.setFontSize(10);
  for (const [k, v] of rows) {
    doc.setFont("helvetica", "normal"); doc.setTextColor(100, 116, 139); doc.text(k, 14, y);
    doc.setFont("helvetica", "bold"); doc.setTextColor(30, 41, 59); doc.text(v, 196, y, { align: "right" });
    doc.setDrawColor(226, 232, 240); doc.line(14, y + 2, 196, y + 2); y += 9;
  }
  doc.save(`${teacher.name}_financial.pdf`);
}

export function exportSchedule(db: DatabaseShape, teacher: Teacher, lang: Lang) {
  const doc = new jsPDF();
  head(doc, db, teacher);
  const groups = groupsOfTeacher(db, teacher.id);
  const DAY_KEY: Record<number, string> = { 1: "Mon", 2: "Tue", 3: "Wed", 4: "Thu", 5: "Fri", 6: "Sat", 7: "Sun" };
  const sessions = db.scheduleEvents.filter((e) => groups.some((g) => g.id === e.groupId)).sort((a, b) => a.dayOfWeek - b.dayOfWeek || a.startTime.localeCompare(b.startTime));
  let y = 32;
  doc.setFont("helvetica", "bold"); doc.setFontSize(11); doc.setTextColor(30, 41, 59); doc.text("Weekly Schedule", 14, y); y += 8;
  doc.setFontSize(8);
  for (const e of sessions) {
    if (y > 278) { doc.addPage(); y = 20; }
    const g = db.groups.find((x) => x.id === e.groupId);
    const room = db.classrooms.find((c) => c.id === e.classroomId);
    doc.setTextColor(79, 70, 229); doc.text(DAY_KEY[e.dayOfWeek], 14, y);
    doc.setTextColor(30, 41, 59); doc.text(g?.name ?? "—", 40, y);
    doc.setTextColor(100, 116, 139); doc.text(formatTime12(e.startTime, lang), 130, y); doc.text(room?.name ?? "—", 196, y, { align: "right" });
    y += 6;
  }
  doc.save(`${teacher.name}_schedule.pdf`);
}
