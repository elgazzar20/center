import { useMemo, useState } from "react";
import {
  Lock, FileText, ClipboardCheck, Award, Wallet, BookOpen, StickyNote,
  Bell, GraduationCap, CalendarClock, ChevronLeft, Search, Send,
} from "lucide-react";
import { useApp } from "../context/AppContext";
import {
  PageHeader, Button, Card, Input, Textarea, Badge, EmptyState, pushToast,
} from "../components/ui";
import { cn } from "../utils/cn";
import { Donut } from "../components/charts";
import { MonthlyAttendance } from "../components/MonthlyAttendance";
import { generateStudentPdf } from "../lib/pdf";
import type { Student } from "../lib/types";
import { startOfDay, now } from "../lib/db";
import { gradeLabel } from "../lib/constants";
import {
  attendanceRate, studentAverage, totalPaidFor, balanceDue,
  currencySymbol, formatMoney,
} from "../lib/analytics";

export function ParentPortal({
  external,
  onClose,
}: {
  external?: boolean;
  onClose?: () => void;
} = {}) {
  const { db, t, lang } = useApp();
  const [code, setCode] = useState("");
  const [phone, setPhone] = useState("");
  const [unlocked, setUnlocked] = useState<Student | null>(null);
  const [error, setError] = useState(false);

  const find = (): Student | undefined => {
    const c = code.trim().toLowerCase();
    const p = phone.trim();
    return db.students.find(
      (s) =>
        (c && (s.id.toLowerCase() === c || s.qrCode.toLowerCase() === c)) ||
        (p && (s.parentPhone ?? "") === p),
    );
  };

  const unlock = () => {
    const s = find();
    if (s) {
      setUnlocked(s);
      setError(false);
    } else {
      setError(true);
    }
  };

  if (unlocked) {
    return <StudentView student={unlocked} onBack={() => setUnlocked(null)} />;
  }

  const sample = db.students.slice(0, 3);

  return (
    <div className="animate-fade-in space-y-5">
      <PageHeader
        title={t("parent.title")}
        subtitle={t("parent.subtitle")}
        actions={
          external && onClose ? (
            <Button variant="secondary" onClick={onClose}>
              <ChevronLeft className="h-4 w-4 rtl:rotate-180" />
              {t("action.back")}
            </Button>
          ) : undefined
        }
      />

      <div className="mx-auto max-w-md">
        <Card className="overflow-hidden">
          <div className="relative bg-gradient-to-br from-brand-600 to-indigo-700 p-7 text-center text-white">
            <div className="absolute -right-8 -top-8 h-32 w-32 rounded-full bg-white/10 blur-2xl" />
            <div className="relative mx-auto mb-3 flex h-14 w-14 items-center justify-center rounded-2xl bg-white/15 backdrop-blur">
              <Lock className="h-7 w-7" />
            </div>
            <h3 className="relative text-lg font-bold">{t("parent.gateTitle")}</h3>
            <p className="relative mt-1 text-xs text-white/80">{t("parent.gateDesc")}</p>
          </div>
          <div className="space-y-3 p-6">
            <div className="relative">
              <GraduationCap className="pointer-events-none absolute inset-y-0 start-3 my-auto h-4 w-4 text-faint" />
              <Input
                placeholder={t("parent.code")}
                value={code}
                onChange={(e) => setCode(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && unlock()}
                className="font-mono ps-9"
              />
            </div>
            <div className="relative">
              <Search className="pointer-events-none absolute inset-y-0 start-3 my-auto h-4 w-4 text-faint" />
              <Input
                placeholder={t("parent.phone")}
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && unlock()}
                className="ps-9"
              />
            </div>
            {error && <p className="text-xs font-medium text-rose-600">{t("parent.notFound")}</p>}
            <Button className="w-full" onClick={unlock}>
              <Lock className="h-4 w-4" />
              {t("parent.unlock")}
            </Button>
            {sample.length > 0 && (
              <div className="pt-1">
                <p className="mb-1.5 text-[11px] text-faint">{lang === "ar" ? "أكواد للتجربة:" : "Sample codes:"}</p>
                <div className="flex flex-wrap gap-1.5">
                  {sample.map((s) => (
                    <button
                      key={s.id}
                      onClick={() => setCode(s.id)}
                      className="rounded-md border border-line px-2 py-1 font-mono text-[10px] text-muted transition hover:border-brand-300 hover:bg-brand-50 hover:text-brand-600 dark:hover:bg-brand-500/10"
                    >
                      {s.id}
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>
        </Card>
      </div>
    </div>
  );
}

/* ------------------------------ Student view ----------------------------- */
function StudentView({ student, onBack }: { student: Student; onBack: () => void }) {
  const { db, t, lang, upsert } = useApp();
  const sym = currencySymbol(db);
  const attRate = Math.round(attendanceRate(db, {}));
  const avg = studentAverage(db, student.id);
  const paid = totalPaidFor(db, student.id);
  const due = balanceDue(db, student);

  const grades = useMemo(
    () =>
      db.examGrades
        .filter((g) => g.studentId === student.id)
        .map((g) => ({ g, exam: db.exams.find((e) => e.id === g.examId) }))
        .filter((x) => x.exam)
        .slice(-6)
        .reverse(),
    [db, student.id],
  );
  const homework = db.assignments.filter((a) => student.groupIds.includes(a.groupId));
  const notes = db.studentNotes
    .filter((n) => n.studentId === student.id)
    .sort((a, b) => b.date - a.date);
  const teachers = student.teachers
    .map((tr) => db.teachers.find((x) => x.id === tr.teacherId))
    .filter((t): t is NonNullable<typeof t> => t !== undefined);

  // notifications feed: upcoming exams + published grades + homework
  const notifications = useMemo(() => {
    const today = startOfDay(now());
    type Item = { id: string; kind: "exam" | "grade" | "hw"; title: string; detail: string; ts: number };
    const items: Item[] = [];
    for (const e of db.exams) {
      if (student.groupIds.includes(e.groupId) && e.date >= today) {
        items.push({ id: `ex_${e.id}`, kind: "exam", title: e.name, detail: `${t("parent.examSoon")} · ${new Date(e.date).toLocaleDateString()}`, ts: e.date });
      }
    }
    for (const g of db.examGrades) {
      if (g.studentId !== student.id || !g.published) continue;
      const exam = db.exams.find((x) => x.id === g.examId);
      if (exam) items.push({ id: `gr_${g.id}`, kind: "grade", title: exam.name, detail: `${t("parent.gradePosted")}: ${g.obtainedGrade}/${exam.maxGrade}`, ts: g.publishedAt ?? g.lastUpdated });
    }
    for (const a of db.assignments) {
      if (student.groupIds.includes(a.groupId)) {
        items.push({ id: `hw_${a.id}`, kind: "hw", title: a.title, detail: `${t("parent.hwAssigned")} · ${new Date(a.dueDate).toLocaleDateString()}`, ts: a.dueDate });
      }
    }
    return items.sort((a, b) => b.ts - a.ts).slice(0, 8);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [db.exams, db.examGrades, db.assignments, student.id, student.groupIds]);

  const notifMeta: Record<string, { icon: typeof Bell; tone: "info" | "success" | "violet" }> = {
    exam: { icon: CalendarClock, tone: "info" },
    grade: { icon: Award, tone: "success" },
    hw: { icon: BookOpen, tone: "violet" },
  };

  // Parent note to center
  const [parentNote, setParentNote] = useState("");
  const sendParentNote = () => {
    if (!parentNote.trim()) return;
    upsert("studentNotes", {
      id: `note_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`,
      studentId: student.id,
      teacherId: "parent_note",
      text: parentNote.trim(),
      date: startOfDay(now()),
      lastUpdated: now(),
    });
    setParentNote("");
    pushToast(t("parent.noteSent"));
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
          <div className="relative flex h-14 w-14 items-center justify-center rounded-2xl bg-white/15 text-lg font-bold backdrop-blur">
            {student.name.split(" ").map((p) => p[0]).slice(0, 2).join("")}
          </div>
          <div className="relative min-w-0 flex-1">
            <h1 className="text-xl font-bold">{student.name}</h1>
            <p className="mt-0.5 text-xs text-white/80">{student.id} · {gradeLabel(student.grade, lang)}</p>
            {teachers.length > 0 && (
              <div className="mt-2 flex flex-wrap gap-1">
                {teachers.map((tc) => tc && (
                  <span key={tc.id} className="inline-flex items-center gap-1 rounded-md bg-white/15 px-2 py-0.5 text-[11px] font-medium backdrop-blur">
                    <GraduationCap className="h-3 w-3" />{tc.name}
                  </span>
                ))}
              </div>
            )}
          </div>
          <Button variant="secondary" onClick={() => generateStudentPdf(db, student)} className="relative border-0 bg-white/15 text-white hover:bg-white/25">
            <FileText className="h-4 w-4" />{t("parent.exportReport")}
          </Button>
        </div>
      </Card>

      {/* notifications */}
      <Card className="p-5">
        <div className="mb-3 flex items-center gap-2">
          <span className="relative flex h-7 w-7 items-center justify-center rounded-lg bg-brand-50 text-brand-600 dark:bg-brand-500/15">
            <Bell className="h-4 w-4" />
            {notifications.length > 0 && <span className="absolute -end-0.5 -top-0.5 flex h-3 w-3"><span className="live-dot absolute inline-flex h-3 w-3 rounded-full bg-rose-400 opacity-75" /><span className="relative inline-flex h-3 w-3 rounded-full bg-rose-500" /></span>}
          </span>
          <h3 className="text-sm font-semibold text-ink">{t("parent.notifications")}</h3>
        </div>
        {notifications.length === 0 ? (
          <p className="py-6 text-center text-xs text-muted">{t("parent.noNotifications")}</p>
        ) : (
          <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
            {notifications.map((n) => {
              const meta = notifMeta[n.kind];
              const Icon = meta.icon;
              return (
                <div key={n.id} className="flex items-start gap-2.5 rounded-lg border border-line p-2.5">
                  <Badge tone={meta.tone}><Icon className="h-3 w-3" /></Badge>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-xs font-medium text-ink">{n.title}</p>
                    <p className="text-[11px] text-muted">{n.detail}</p>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </Card>

      {/* stat cards */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <Card className="flex flex-col items-center gap-2 p-5">
          <div className="flex w-full items-center gap-2"><ClipboardCheck className="h-4 w-4 text-brand-600" /><h3 className="text-sm font-semibold text-ink">{t("parent.attendance")}</h3></div>
          <Donut value={attRate} color="#6366f1" label={`${attRate}%`} />
        </Card>

        <Card className="p-5">
          <div className="mb-3 flex items-center gap-2"><Award className="h-4 w-4 text-amber-600" /><h3 className="text-sm font-semibold text-ink">{t("parent.grades")}</h3></div>
          {grades.length === 0 ? (
            <p className="py-6 text-center text-xs text-muted">{t("exams.empty")}</p>
          ) : (
            <div className="space-y-2">
              {grades.map(({ g, exam }) => (
                <div key={g.id} className="flex items-center gap-2">
                  <span className="min-w-0 flex-1 truncate text-xs text-ink">{exam!.name}</span>
                  <Badge tone={g.obtainedGrade / exam!.maxGrade >= 0.5 ? "success" : "danger"}>{g.obtainedGrade}/{exam!.maxGrade}</Badge>
                </div>
              ))}
              {avg != null && (
                <div className="mt-2 flex items-center justify-between border-t border-line pt-2">
                  <span className="text-xs text-muted">{t("exams.avg")}</span>
                  <span className="text-sm font-bold text-ink">{Math.round(avg)}%</span>
                </div>
              )}
            </div>
          )}
        </Card>

        <Card className="p-5">
          <div className="mb-3 flex items-center gap-2"><Wallet className="h-4 w-4 text-emerald-600" /><h3 className="text-sm font-semibold text-ink">{t("parent.fees")}</h3></div>
          <div className="space-y-2.5">
            <div className="flex items-center justify-between rounded-lg bg-emerald-50 px-3 py-2 dark:bg-emerald-500/10">
              <span className="text-xs text-muted">{t("students.paid")}</span>
              <span className="text-sm font-bold text-emerald-600">{formatMoney(paid, sym)}</span>
            </div>
            <div className="flex items-center justify-between rounded-lg bg-rose-50 px-3 py-2 dark:bg-rose-500/10">
              <span className="text-xs text-muted">{t("students.balance")}</span>
              <span className="text-sm font-bold text-rose-600">{formatMoney(due, sym)}</span>
            </div>
            <Badge tone={due > 0 ? "danger" : "success"} className="w-full justify-center py-1">
              {due > 0 ? formatMoney(due, sym) : t("students.paid")}
            </Badge>
          </div>
        </Card>
      </div>

      {/* monthly attendance (auto-shown to parents) */}
      <MonthlyAttendance studentId={student.id} />

      {/* homework + notes */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <Card className="p-5">
          <div className="mb-3 flex items-center gap-2"><BookOpen className="h-4 w-4 text-violet-600" /><h3 className="text-sm font-semibold text-ink">{t("parent.homework")}</h3></div>
          {homework.length === 0 ? (
            <EmptyState title={t("exams.empty")} />
          ) : (
            <div className="space-y-2">
              {homework.map((a) => (
                <div key={a.id} className="rounded-lg border border-line p-2.5">
                  <p className="text-sm font-medium text-ink">{a.title}</p>
                  {a.description && <p className="text-[11px] text-muted">{a.description}</p>}
                  <p className="mt-1 text-[10px] text-faint">{t("exams.due")}: {new Date(a.dueDate).toLocaleDateString()}</p>
                </div>
              ))}
            </div>
          )}
        </Card>

        <Card className="p-5">
          <div className="mb-3 flex items-center gap-2"><StickyNote className="h-4 w-4 text-sky-600" /><h3 className="text-sm font-semibold text-ink">{t("parent.notes")}</h3></div>

          {/* Send note to center */}
          <div className="mb-3 space-y-2">
            <Textarea rows={2} value={parentNote} onChange={(e) => setParentNote(e.target.value)} placeholder={t("parent.sendNotePlaceholder")} />
            <Button size="sm" className="w-full" onClick={sendParentNote} disabled={!parentNote.trim()}>
              <Send className="h-3.5 w-3.5" />{t("parent.sendNote")}
            </Button>
          </div>

          {/* Notes from center + notes from parent */}
          {notes.length === 0 ? (
            <p className="py-6 text-center text-xs text-muted">{t("parent.noNotes")}</p>
          ) : (
            <div className="space-y-2 max-h-60 overflow-y-auto">
              {notes.map((n) => (
                <div key={n.id} className={cn("rounded-lg p-2.5", n.teacherId === "parent_note" ? "bg-brand-50 dark:bg-brand-500/10" : "bg-elevated/50")}>
                  {n.teacherId === "parent_note" && <Badge tone="brand" className="mb-1">{t("parent.noteFromParent")}</Badge>}
                  <p className="text-xs text-ink">{n.text}</p>
                  <p className="mt-1 text-[10px] text-faint">{new Date(n.date).toLocaleDateString()}</p>
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}


