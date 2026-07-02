import { useMemo, useState } from "react";
import {
  ChevronLeft, ClipboardCheck, Award, Wallet,
  BookOpen, StickyNote, Send, Phone, User, Users2, CalendarClock, FileDown,
} from "lucide-react";
import { cn } from "../utils/cn";
import { useApp } from "../context/AppContext";
import { Button, Card, Textarea, Badge, EmptyState, Modal, pushToast } from "../components/ui";
import { StatCard } from "../components/widgets";
import { Donut } from "../components/charts";
import { MonthlyAttendance } from "../components/MonthlyAttendance";
import { QRCodeImage } from "../components/QRCode";
import { generateStudentPdf } from "../lib/pdf";
import type { Student, StudentNote } from "../lib/types";
import { now, startOfDay, uid } from "../lib/db";
import { GRADES, STAGE_TONE, gradeLabel } from "../lib/constants";
import {
  attendanceRate, studentAverage, balanceDue, paidForTeacher,
  studentNetFee, currencySymbol, formatMoney,
} from "../lib/analytics";

export function StudentProfile({ student, onBack }: { student: Student; onBack: () => void }) {
  const { db, t, lang, upsert } = useApp();
  const sym = currencySymbol(db);
  const attRate = Math.round(attendanceRate(db, {}));
  const avg = studentAverage(db, student.id);
  const due = balanceDue(db, student);
  const net = studentNetFee(student);
  const grade = GRADES.find((g) => g.id === student.grade);

  const teachers = student.teachers.map((tr) => ({ tr, tc: db.teachers.find((x) => x.id === tr.teacherId) }));
  const groups = db.groups.filter((g) => student.groupIds.includes(g.id));
  const grades = useMemo(
    () =>
      db.examGrades
        .filter((g) => g.studentId === student.id)
        .map((g) => ({ g, exam: db.exams.find((e) => e.id === g.examId) }))
        .filter((x): x is { g: typeof x.g; exam: NonNullable<typeof x.exam> } => x.exam !== undefined)
        .slice(-6)
        .reverse(),
    [db, student.id],
  );
  const notes = useMemo(
    () => db.studentNotes.filter((n) => n.studentId === student.id).sort((a, b) => b.date - a.date),
    [db.studentNotes, student.id],
  );

  const [noteText, setNoteText] = useState("");
  const [qrOpen, setQrOpen] = useState(false);

  const sendNote = () => {
    if (!noteText.trim()) return;
    const note: StudentNote = {
      id: uid("note"),
      studentId: student.id,
      teacherId: student.teachers[0]?.teacherId,
      text: noteText.trim(),
      date: startOfDay(now()),
      lastUpdated: now(),
    };
    upsert("studentNotes", note);
    setNoteText("");
    pushToast(t("toast.sent"));
  };

  return (
    <div className="animate-fade-in space-y-5">
      {/* header banner */}
      <Card className="overflow-hidden">
        <div className="relative flex flex-wrap items-center gap-4 bg-gradient-to-br from-brand-600 to-indigo-800 p-5 text-white">
          <div className="absolute -right-10 -top-10 h-40 w-40 rounded-full bg-white/10 blur-2xl" />
          <Button variant="secondary" size="icon" onClick={onBack} className="relative border-0 bg-white/15 text-white hover:bg-white/25">
            <ChevronLeft className="h-5 w-5 rtl:rotate-180" />
          </Button>
          <button onClick={() => setQrOpen(true)} className="relative flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-white p-1.5">
            <QRCodeImage value={student.qrCode} size={44} />
          </button>
          <div className="relative min-w-0 flex-1">
            <div className="flex items-center gap-2">
              <h1 className="text-xl font-bold">{student.name}</h1>
              {grade && <span className={cn(STAGE_TONE[grade.stage], "rounded px-1.5 py-0.5 text-[10px] font-medium")}>{gradeLabel(student.grade, lang)}</span>}
            </div>
            <p className="mt-0.5 font-mono text-xs text-white/80">{student.id}</p>
            <div className="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-white/80">
              {student.parentName && <span className="flex items-center gap-1"><User className="h-3 w-3" />{student.parentName}</span>}
              {student.parentPhone && <span className="flex items-center gap-1"><Phone className="h-3 w-3" />{student.parentPhone}</span>}
              {student.studentPhone && <span className="flex items-center gap-1"><Phone className="h-3 w-3" />{student.studentPhone}</span>}
            </div>
          </div>
          <Button variant="secondary" onClick={() => generateStudentPdf(db, student)} className="relative border-0 bg-white/15 text-white hover:bg-white/25">
            <FileDown className="h-4 w-4" />{t("parent.exportReport")}
          </Button>
        </div>
      </Card>

      {/* KPI cards */}
      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <StatCard icon={Wallet} tone="emerald" label={t("students.totalFee")} value={formatMoney(net, sym)} />
        <StatCard icon={ClipboardCheck} tone="brand" label={t("dash.attendanceRate")} value={`${attRate}%`} />
        <StatCard icon={Award} tone="amber" label={t("exams.avg")} value={avg != null ? `${Math.round(avg)}%` : "—"} />
        <StatCard icon={Wallet} tone={due > 0 ? "rose" : "emerald"} label={t("students.balance")} value={formatMoney(due, sym)} />
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        {/* left column */}
        <div className="space-y-4 lg:col-span-2">
          {/* teachers */}
          <Card className="p-5">
            <h3 className="mb-3 flex items-center gap-2 text-sm font-semibold text-ink"><Users2 className="h-4 w-4 text-brand-600" />{t("students.teachers")}</h3>
            <div className="space-y-2">
              {teachers.map(({ tr, tc }) => (
                <div key={tr.teacherId} className="flex items-center gap-3 rounded-lg border border-line p-2.5">
                  <div className="flex h-9 w-9 items-center justify-center rounded-full text-[11px] font-bold text-white" style={{ background: `linear-gradient(135deg, ${tc?.color ?? "#6366f1"}, #4f46e5)` }}>
                    {tc?.name.split(" ").map((p) => p[0]).slice(0, 2).join("")}
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium text-ink">{tc?.name ?? "—"}</p>
                    <p className="text-[11px] text-muted">{tc?.subjects.map((s) => gradeLabel(s, lang)).join(" · ")}</p>
                  </div>
                  <div className="text-end">
                    <p className="text-sm font-bold text-ink">{formatMoney(tr.fee, sym)}</p>
                    <p className="text-[10px] text-faint">{t("students.paid")}: {formatMoney(paidForTeacher(db, student.id, tr.teacherId), sym)}</p>
                  </div>
                </div>
              ))}
            </div>
          </Card>

          {/* groups + attendance */}
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Card className="p-5">
              <h3 className="mb-3 flex items-center gap-2 text-sm font-semibold text-ink"><BookOpen className="h-4 w-4 text-violet-600" />{t("students.groups")}</h3>
              {groups.length === 0 ? <p className="py-4 text-center text-xs text-muted">{t("classes.emptyGroups")}</p> : (
                <div className="space-y-1.5">
                  {groups.map((g) => (
                    <div key={g.id} className="flex items-center gap-2 rounded-lg border border-line p-2 text-xs">
                      <span className="min-w-0 flex-1 truncate font-medium text-ink">{g.name}</span>
                      <Badge tone="brand">{gradeLabel(g.grade, lang)}</Badge>
                    </div>
                  ))}
                </div>
              )}
            </Card>
            <Card className="flex flex-col items-center gap-2 p-5">
              <h3 className="flex w-full items-center gap-2 text-sm font-semibold text-ink"><ClipboardCheck className="h-4 w-4 text-sky-600" />{t("parent.attendance")}</h3>
              <Donut value={attRate} color="#0ea5e9" label={`${attRate}%`} />
            </Card>
          </div>

          {/* grades */}
          <Card className="overflow-hidden">
            <div className="border-b border-line px-4 py-2.5 text-sm font-semibold text-ink">{t("parent.grades")}</div>
            {grades.length === 0 ? <p className="py-6 text-center text-xs text-muted">{t("exams.empty")}</p> : (
              <table className="w-full text-sm">
                <tbody>
                  {grades.map(({ g, exam }) => (
                    <tr key={g.id} className="border-b border-line/50 last:border-0">
                      <td className="px-4 py-2 text-ink">{exam.name}</td>
                      <td className="px-4 py-2 text-end">
                        <Badge tone={g.obtainedGrade / exam.maxGrade >= 0.5 ? "success" : "danger"}>{g.obtainedGrade}/{exam.maxGrade}</Badge>
                      </td>
                      <td className="px-4 py-2 text-end">
                        {g.published ? <Badge tone="success">{t("exams.published")}</Badge> : <Badge tone="neutral">{t("exams.notPublished")}</Badge>}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </Card>

          {/* monthly attendance session-by-session */}
          <MonthlyAttendance studentId={student.id} />
        </div>

        {/* right column: send note to parent + history */}
        <div className="space-y-4">
          <Card className="p-5">
            <h3 className="mb-2 flex items-center gap-2 text-sm font-semibold text-ink"><Send className="h-4 w-4 text-brand-600" />{t("teachers.addNote")}</h3>
            <p className="mb-2 text-[11px] text-muted">{t("parent.notes")}</p>
            <Textarea rows={4} value={noteText} onChange={(e) => setNoteText(e.target.value)} placeholder={t("teachers.addNote")} />
            <Button className="mt-2 w-full" onClick={sendNote} disabled={!noteText.trim()}>
              <Send className="h-4 w-4" />{t("exams.publish")}
            </Button>
          </Card>

          <Card className="p-5">
            <h3 className="mb-3 flex items-center gap-2 text-sm font-semibold text-ink"><StickyNote className="h-4 w-4 text-amber-500" />{t("parent.notes")} ({notes.length})</h3>
            {notes.length === 0 ? (
              <EmptyState title={t("parent.noNotes")} />
            ) : (
              <div className="space-y-2">
                {notes.map((n) => (
                  <div key={n.id} className="rounded-lg border border-line p-2.5">
                    <p className="text-xs text-ink">{n.text}</p>
                    <p className="mt-1 flex items-center gap-1 text-[10px] text-faint"><CalendarClock className="h-3 w-3" />{new Date(n.date).toLocaleDateString()}</p>
                  </div>
                ))}
              </div>
            )}
          </Card>
        </div>
      </div>

      {/* ===== COMPLETE ARCHIVE ===== */}
      <ArchiveSection student={student} />

      {/* QR modal */}
      <Modal open={qrOpen} onClose={() => setQrOpen(false)} title={t("students.qr")} size="sm"
        footer={<Button variant="secondary" onClick={() => setQrOpen(false)}>{t("action.close")}</Button>}>
        <div className="flex flex-col items-center gap-3 py-2">
          <div className="rounded-xl border border-line bg-white p-3"><QRCodeImage value={student.qrCode} size={180} /></div>
          <div className="text-center">
            <p className="font-mono text-sm font-bold text-ink">{student.id}</p>
            <p className="text-sm text-muted">{student.name}</p>
          </div>
        </div>
      </Modal>
    </div>
  );
}

/* ===================== COMPLETE STUDENT ARCHIVE ===================== */
function ArchiveSection({ student }: { student: Student }) {
  const { db, t, lang } = useApp();
  const [tab, setTab] = useState("grades");

  const allGrades = useMemo(() =>
    db.examGrades
      .filter((g) => g.studentId === student.id)
      .map((g) => ({ g, exam: db.exams.find((e) => e.id === g.examId) }))
      .filter((x): x is { g: typeof x.g; exam: NonNullable<typeof x.exam> } => x.exam !== undefined)
      .sort((a, b) => b.exam.date - a.exam.date),
    [db.examGrades, db.exams, student.id],
  );

  const allAttendance = useMemo(() =>
    db.attendance
      .filter((a) => a.studentId === student.id)
      .sort((a, b) => b.date - a.date),
    [db.attendance, student.id],
  );

  const allPayments = useMemo(() =>
    [...db.payments]
      .filter((p) => p.studentId === student.id)
      .sort((a, b) => b.date - a.date),
    [db.payments, student.id],
  );

  const allHomework = useMemo(() =>
    db.assignments
      .filter((a) => student.groupIds.includes(a.groupId))
      .sort((a, b) => b.dueDate - a.dueDate),
    [db.assignments, student.groupIds],
  );

  const allNotes = useMemo(() =>
    db.studentNotes
      .filter((n) => n.studentId === student.id)
      .sort((a, b) => b.date - a.date),
    [db.studentNotes, student.id],
  );

  const tabs = [
    { id: "grades", label: t("archive.tab.grades"), count: allGrades.length },
    { id: "attendance", label: t("archive.tab.attendance"), count: allAttendance.length },
    { id: "payments", label: t("archive.tab.payments"), count: allPayments.length },
    { id: "homework", label: t("archive.tab.homework"), count: allHomework.length },
    { id: "notes", label: t("archive.tab.notes"), count: allNotes.length },
  ];

  const attStatus = (s: string) => {
    if (s === "PRESENT") return "bg-emerald-50 text-emerald-600 dark:bg-emerald-500/15";
    if (s === "ABSENT") return "bg-rose-50 text-rose-600 dark:bg-rose-500/15";
    if (s === "LATE") return "bg-amber-50 text-amber-600 dark:bg-amber-500/15";
    return "bg-sky-50 text-sky-600 dark:bg-sky-500/15";
  };

  return (
    <Card className="overflow-hidden">
      <div className="flex items-center gap-2 border-b border-line bg-elevated/40 px-5 py-3">
        <FileDown className="h-4 w-4 text-brand-500" />
        <h3 className="text-[15px] font-semibold tracking-tight text-ink">{t("archive.title")}</h3>
        <span className="text-[11px] text-faint">· {t("archive.subtitle")}</span>
      </div>

      {/* tabs */}
      <div className="flex flex-wrap gap-1 border-b border-line px-3 py-2">
        {tabs.map((tb) => (
          <button key={tb.id} onClick={() => setTab(tb.id)}
            className={cn("inline-flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-semibold transition", tab === tb.id ? "bg-brand-600 text-white shadow-sm" : "text-muted hover:bg-elevated")}>
            {tb.label}
            <span className={cn("rounded-full px-1.5 text-[9px]", tab === tb.id ? "bg-white/20" : "bg-elevated")}>{tb.count}</span>
          </button>
        ))}
      </div>

      {/* content */}
      <div className="max-h-[420px] overflow-y-auto p-4">
        {tab === "grades" && (
          allGrades.length === 0 ? <ArchiveEmpty t={t} /> : (
            <div className="space-y-2">
              {allGrades.map(({ g, exam }) => {
                const pct = exam.maxGrade > 0 ? (g.obtainedGrade / exam.maxGrade) * 100 : 0;
                return (
                  <div key={g.id} className="flex items-center gap-3 rounded-lg border border-line p-2.5">
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-medium text-ink">{exam.name}</p>
                      <p className="text-[10px] text-faint">{new Date(exam.date).toLocaleDateString()}</p>
                    </div>
                    <Badge tone={pct >= 50 ? "success" : "danger"}>{g.obtainedGrade}/{exam.maxGrade} · {Math.round(pct)}%</Badge>
                  </div>
                );
              })}
            </div>
          )
        )}

        {tab === "attendance" && (
          <>
            {/* monthly summary with navigation */}
            <MonthlyAttendance studentId={student.id} />
            {/* full list */}
            {allAttendance.length === 0 ? <ArchiveEmpty t={t} /> : (
              <div className="mt-3 space-y-1">
                {allAttendance.slice(0, 50).map((a) => (
                  <div key={a.id} className="flex items-center gap-3 rounded-lg px-2 py-1.5 text-xs">
                    <span className="text-faint">{new Date(a.date).toLocaleDateString(lang === "ar" ? "ar-EG" : undefined, { weekday: "short", day: "numeric", month: "short" })}</span>
                    <span className={cn("ms-auto rounded-full px-2 py-0.5 text-[10px] font-bold", attStatus(a.status))}>{t(`att.${a.status.toLowerCase()}`)}</span>
                  </div>
                ))}
              </div>
            )}
          </>
        )}

        {tab === "payments" && (
          allPayments.length === 0 ? <ArchiveEmpty t={t} /> : (
            <div className="space-y-2">
              {allPayments.map((p) => (
                <div key={p.id} className="flex items-center gap-3 rounded-lg border border-line p-2.5">
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium text-ink">{t(`fin.type.${p.type}`)}</p>
                    <p className="text-[10px] text-faint">{new Date(p.date).toLocaleDateString()} · {p.month}</p>
                  </div>
                  <Badge tone="success">{formatMoney(p.amount, currencySymbol(db))}</Badge>
                </div>
              ))}
            </div>
          )
        )}

        {tab === "homework" && (
          allHomework.length === 0 ? <ArchiveEmpty t={t} /> : (
            <div className="space-y-2">
              {allHomework.map((a) => {
                const overdue = a.dueDate < Date.now();
                return (
                  <div key={a.id} className="rounded-lg border border-line p-2.5">
                    <p className="text-sm font-medium text-ink">{a.title}</p>
                    {a.description && <p className="text-[11px] text-muted">{a.description}</p>}
                    <p className={cn("mt-1 text-[10px]", overdue ? "text-rose-500" : "text-faint")}>{t("exams.due")}: {new Date(a.dueDate).toLocaleDateString()}</p>
                  </div>
                );
              })}
            </div>
          )
        )}

        {tab === "notes" && (
          allNotes.length === 0 ? <ArchiveEmpty t={t} /> : (
            <div className="space-y-2">
              {allNotes.map((n) => (
                <div key={n.id} className="rounded-lg border border-line p-2.5">
                  <p className="text-sm text-ink">{n.text}</p>
                  <p className="mt-1 text-[10px] text-faint">{new Date(n.date).toLocaleDateString()}</p>
                </div>
              ))}
            </div>
          )
        )}
      </div>
    </Card>
  );
}

function ArchiveEmpty({ t }: { t: (k: string) => string }) {
  return <p className="py-8 text-center text-xs text-muted">{t("exams.empty")}</p>;
}
