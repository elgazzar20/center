import { useMemo, useState } from "react";
import {
  FileText, FileSpreadsheet, Users, Wallet, Send, GraduationCap, UserRound,
  BarChart3, CheckCircle2, Building2, CalendarRange, Download, Mail,
} from "lucide-react";
import { useApp } from "../context/AppContext";
import { PageHeader, Button, Card, Field, Badge, pushToast } from "../components/ui";
import { Combobox } from "../components/ui";
import {
  exportCenterPdf, exportCenterExcel, exportStudentsExcel, exportFinanceExcel,
} from "../lib/reports";
import { generateStudentPdf } from "../lib/pdf";
import { exportStudents as exportTeacherStudents, exportFinancial as exportTeacherFinancial, exportSchedule as exportTeacherSchedule } from "../lib/teacherReports";
import { subjectLabel } from "../lib/constants";
import { monthlyRevenue, monthlyExpenses, formatMoney, teacherRevenue, currencySymbol } from "../lib/analytics";
import { cn } from "../utils/cn";

export function Reports() {
  const { db, t, lang } = useApp();
  const sym = currencySymbol(db);
  const [teacherId, setTeacherId] = useState(db.teachers[0]?.id ?? "");
  const [studentId, setStudentId] = useState(db.students[0]?.id ?? "");
  const [sent, setSent] = useState<string[]>([]);

  const studentOptions = useMemo(
    () => db.students.map((s) => ({ value: s.id, label: `${s.name} · ${s.id}` })),
    [db.students],
  );
  const teacherOptions = useMemo(
    () => db.teachers.map((tc) => ({ value: tc.id, label: tc.name })),
    [db.teachers],
  );
  const teacher = db.teachers.find((x) => x.id === teacherId);
  const student = db.students.find((s) => s.id === studentId);

  const income = monthlyRevenue(db);
  const expenses = monthlyExpenses(db);
  const net = income - expenses;

  // ONLY the student report can be sent — to the parent. Everything else is
  // download/view only (per access policy).
  const sendStudentToParent = () => {
    if (!student) return;
    generateStudentPdf(db, student);
    setSent((p) => [...new Set([...p, `student-${student.id}`])]);
    pushToast(t("toast.reportParent"));
  };

  return (
    <div className="animate-fade-in space-y-5">
      <PageHeader title={t("reports.title")} subtitle={t("reports.subtitle")} />

      {/* premium hero with quick stats */}
      <Card className="mesh-brand relative overflow-hidden border-0 text-white shadow-[var(--shadow-brand)]">
        <div className="orb float-soft -right-8 -top-10 h-40 w-40 bg-white/12" />
        <div className="orb float-soft -bottom-16 left-1/3 h-44 w-44 bg-accent-400/20" style={{ animationDelay: "1s" }} />
        <div className="relative flex flex-wrap items-center justify-between gap-5 p-6">
          <div className="relative">
            <p className="flex items-center gap-1.5 text-xs font-medium text-white/70"><Building2 className="h-3.5 w-3.5" />{db.profile.name}</p>
            <h2 className="mt-1 text-2xl font-bold">{t("reports.title")}</h2>
            <p className="mt-1 flex items-center gap-1.5 text-xs text-white/70"><CalendarRange className="h-3.5 w-3.5" />{new Date().toLocaleDateString()}</p>
          </div>
          <div className="relative grid grid-cols-3 gap-3">
            <div className="rounded-xl bg-white/12 px-4 py-3 text-center ring-1 ring-white/15 backdrop-blur">
              <p className="text-lg font-bold">{db.students.length}</p>
              <p className="text-[10px] text-white/70">{t("dash.totalStudents")}</p>
            </div>
            <div className="rounded-xl bg-white/12 px-4 py-3 text-center ring-1 ring-white/15 backdrop-blur">
              <p className="text-lg font-bold">{db.teachers.length}</p>
              <p className="text-[10px] text-white/70">{t("teachers.title")}</p>
            </div>
            <div className="rounded-xl bg-white/12 px-4 py-3 text-center ring-1 ring-white/15 backdrop-blur">
              <p className="text-lg font-bold">{formatMoney(net, sym)}</p>
              <p className="text-[10px] text-white/70">{t("dash.netProfit")}</p>
            </div>
          </div>
        </div>
      </Card>

      {/* policy note */}
      <div className="flex items-center gap-2 rounded-xl border border-amber-200/60 bg-amber-50/60 px-4 py-2.5 text-xs text-amber-800 dark:border-amber-500/20 dark:bg-amber-500/10 dark:text-amber-300">
        <Mail className="h-4 w-4 shrink-0" />
        <span>{t("reports.policy")}</span>
      </div>

      {/* report cards */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        {/* full center — download/view only */}
        <ReportCard
          icon={<BarChart3 className="h-6 w-6" />} gradient="from-brand-500 to-brand-700"
          title={t("reports.title")} subtitle={db.profile.name}
          actions={[
            { label: t("reports.fullPdf"), icon: <FileText className="h-4 w-4" />, onClick: () => exportCenterPdf(db, lang) },
            { label: t("reports.fullExcel"), icon: <FileSpreadsheet className="h-4 w-4" />, onClick: () => exportCenterExcel(db, lang) },
            { label: t("reports.studentsExcel"), icon: <Users className="h-4 w-4" />, onClick: () => exportStudentsExcel(db, lang) },
            { label: t("reports.financeExcel"), icon: <Wallet className="h-4 w-4" />, onClick: () => exportFinanceExcel(db) },
          ]}
        />

        {/* teacher — download/view only */}
        <ReportCard
          icon={<UserRound className="h-6 w-6" />} gradient="from-violet-500 to-violet-700"
          title={t("reports.teacher")} subtitle={teacher ? teacher.subjects.map((s) => subjectLabel(s, lang)).join(" · ") : "—"}
          actions={teacher ? [
            { label: t("students.title"), icon: <FileText className="h-4 w-4" />, onClick: () => exportTeacherStudents(db, teacher, lang) },
            { label: t("fin.title"), icon: <Wallet className="h-4 w-4" />, onClick: () => exportTeacherFinancial(db, teacher, lang) },
            { label: t("schedule.title"), icon: <FileText className="h-4 w-4" />, onClick: () => exportTeacherSchedule(db, teacher, lang) },
          ] : []}
        >
          <Field label={t("teachers.title")}>
            <Combobox value={teacherId} onChange={setTeacherId} options={teacherOptions}
              placeholder={t("teachers.title")} allowCustom={false}
              searchLabel={t("action.search")} emptyLabel={t("teachers.empty")} />
          </Field>
          {teacher && (
            <div className="mt-2 flex items-center justify-between rounded-lg bg-elevated/60 px-3 py-2 text-xs">
              <span className="text-muted">{t("teachers.revenue")}</span>
              <span className="font-bold text-emerald-600">{formatMoney(teacherRevenue(db, teacher.id), sym)}</span>
            </div>
          )}
        </ReportCard>
      </div>

      {/* student report — the ONLY one that can be sent (to the parent) */}
      <ReportCard
        icon={<GraduationCap className="h-6 w-6" />} gradient="from-emerald-500 to-emerald-700"
        title={t("reports.student")} subtitle={t("reports.studentHint")}
        full
        actions={student ? [
          { label: t("parent.exportReport"), icon: <Download className="h-4 w-4" />, onClick: () => generateStudentPdf(db, student) },
        ] : []}
        sendLabel={t("reports.sendParent")}
        sent={!!student && sent.includes(`student-${student.id}`)}
        canSend={!!student}
        onSend={sendStudentToParent}
      >
        <Field label={t("students.title")}>
          <Combobox value={studentId} onChange={setStudentId} options={studentOptions}
            placeholder={t("fin.searchStudent")} allowCustom={false}
            searchLabel={t("fin.searchStudent")} emptyLabel={t("fin.noResults")} />
        </Field>
      </ReportCard>

      {sent.length > 0 && (
        <div className="flex items-center gap-2 rounded-xl bg-emerald-50 px-4 py-3 text-sm text-emerald-700 dark:bg-emerald-500/10 dark:text-emerald-300">
          <CheckCircle2 className="h-4 w-4" />{t("toast.reportParent")}
        </div>
      )}
    </div>
  );
}

/* --------------------------- Reusable report card --------------------------- */
function ReportCard({
  icon, gradient, title, subtitle, actions, children, full, sendLabel, sent, canSend, onSend,
}: {
  icon: React.ReactNode;
  gradient: string;
  title: string;
  subtitle?: string;
  actions: { label: string; icon: React.ReactNode; onClick: () => void }[];
  children?: React.ReactNode;
  full?: boolean;
  sendLabel?: string;
  sent?: boolean;
  canSend?: boolean;
  onSend?: () => void;
}) {
  return (
    <Card className={cn("card-hover overflow-hidden", full && "lg:col-span-2")}>
      <div className="flex items-center gap-3 border-b border-line bg-elevated/40 p-4">
        <div className={cn("flex h-12 w-12 items-center justify-center rounded-xl bg-gradient-to-br text-white shadow-lg", gradient)}>{icon}</div>
        <div className="min-w-0 flex-1">
          <h3 className="truncate text-[15px] font-semibold tracking-tight text-ink">{title}</h3>
          {subtitle && <p className="truncate text-xs text-muted">{subtitle}</p>}
        </div>
        {sent && <Badge tone="success"><CheckCircle2 className="h-3 w-3" />{t0("view.pinned").replace("Pinned", "Sent")}</Badge>}
      </div>
      <div className="p-4">
        {children}
        <div className={cn("grid gap-2", full ? "sm:grid-cols-3" : "sm:grid-cols-2")}>
          {actions.map((a, i) => (
            <Button key={i} size="sm" variant="secondary" onClick={a.onClick}>
              {a.icon}{a.label}
            </Button>
          ))}
        </div>
        {sendLabel && (
          <Button size="sm" variant="subtle" className="mt-2 w-full" onClick={onSend} disabled={!canSend}>
            {sent ? <CheckCircle2 className="h-4 w-4" /> : <Send className="h-4 w-4" />}
            {sendLabel}
          </Button>
        )}
      </div>
    </Card>
  );
}

// tiny helper to access translations inside the non-hook ReportCard
import { useApp as useApp0 } from "../context/AppContext";
function t0(key: string) {
  return useApp0().t(key);
}
