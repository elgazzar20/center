import { useMemo, useState } from "react";
import {
  Plus, Search, Pencil, Trash2, Users, X, FileDown, Upload,
  UsersRound, CalendarRange, StickyNote, Phone, Mail, ChevronLeft,
} from "lucide-react";
import { useApp } from "../context/AppContext";
import {
  PageHeader, Button, Card, Input, Field, Badge, Modal, EmptyState,
  MultiCombobox, FilterSelect, Textarea, pushToast,
} from "../components/ui";
import type { Teacher } from "../lib/types";
import { now } from "../lib/db";
import { SUBJECTS, subjectLabel, gradeLabel, formatTime12 } from "../lib/constants";
import {
  teacherRevenue, teacherCenterShare, teacherNet, studentsOfTeacher,
  groupsOfTeacher, paidForTeacher, currencySymbol, formatMoney,
} from "../lib/analytics";
import {
  exportStudents as exportTeacherStudents,
  exportFinancial as exportTeacherFinancial,
  exportSchedule as exportTeacherSchedule,
} from "../lib/teacherReports";
import { parseTeachersExcel } from "../lib/excel-import";
import { usePersistentView } from "../components/PersistentViewToggle";
import { ViewToggle } from "../components/ViewToggle";
import { cn } from "../utils/cn";

function blankTeacher(): Teacher {
  return {
    id: "", name: "", phone: "", email: "", subjects: [],
    payType: "percentage", commissionRate: 10, fixedAmount: 500, lastUpdated: now(),
  };
}

export function Teachers() {
  const { db, t, lang, upsert, remove, can } = useApp();
  const sym = currencySymbol(db);
  const [query, setQuery] = useState("");
  const [editing, setEditing] = useState<Teacher | null>(null);
  const [creating, setCreating] = useState(false);
  const [detail, setDetail] = useState<Teacher | null>(null);
  const [form, setForm] = useState<Teacher>(blankTeacher());
  const [fSubject, setFSubject] = useState("");
  const [fPay, setFPay] = useState("");
  const { view, change: setView } = usePersistentView("teachers", "grid");

  const filtered = useMemo(() => {
    const q = query.toLowerCase().trim();
    return db.teachers
      .filter((x) => {
        if (q && !x.name.toLowerCase().includes(q) && !x.subjects.join(" ").toLowerCase().includes(q)) return false;
        if (fSubject && !x.subjects.includes(fSubject)) return false;
        if (fPay && x.payType !== fPay) return false;
        return true;
      })
      .sort((a, b) => teacherRevenue(db, b.id) - teacherRevenue(db, a.id));
  }, [db, query, fSubject, fPay]);

  const { canAddTeacher, subscriptionPlan } = useApp();

  const openCreate = () => {
    if (!canAddTeacher()) {
      const limits: Record<string, number> = { free: 2, pro: 30, enterprise: 99999 };
      pushToast(`وصلت إلى الحد الأقصى (${limits[subscriptionPlan] || 2} معلم) في خطة ${subscriptionPlan === "free" ? "المجاني" : subscriptionPlan === "pro" ? "الاحترافي" : "المؤسسي"}. قم بالترقية لإضافة المزيد.`, "error");
      window.dispatchEvent(new CustomEvent("navigate", { detail: "upgrade" }));
      return;
    }
    setForm({ ...blankTeacher(), subjects: [SUBJECTS[0]] });
    setCreating(true);
  };
  const openEdit = (x: Teacher) => {
    setForm({ ...x, subjects: [...x.subjects] });
    setEditing(x);
  };
  const save = () => {
    if (!form.name.trim() || !form.subjects.length) return;
    upsert("teachers", form);
    pushToast(t("toast.saved"));
    setCreating(false);
    setEditing(null);
  };
  const set = <K extends keyof Teacher>(k: K, v: Teacher[K]) => setForm((f) => ({ ...f, [k]: v }));

  const subjectOptions = SUBJECTS.map((s) => ({ value: s, label: subjectLabel(s, lang) }));

  if (detail) {
    return <TeacherDetailPage teacher={detail} onBack={() => setDetail(null)} />;
  }

  // Excel import
  const [importing, setImporting] = useState(false);
  const handleImport = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setImporting(true);
    const result = await parseTeachersExcel(file);
    setImporting(false);
    if (result.success && result.data.length > 0) {
      result.data.forEach((tc) => upsert("teachers", tc));
      pushToast(t("teachers.importSuccess", { n: result.count }));
    } else {
      pushToast(result.errors[0] || "Import failed", "error");
    }
    e.target.value = "";
  };

  return (
    <div className="animate-fade-in space-y-5">
      <PageHeader title={t("teachers.title")} subtitle={t("teachers.subtitle")}
        actions={<div className="flex items-center gap-2">
          {can("teachers.manage") && (
            <>
              <label className="inline-flex h-9 cursor-pointer items-center gap-1.5 rounded-lg border border-line bg-elevated px-3 text-xs font-medium text-ink transition hover:bg-line">
                <Upload className="h-3.5 w-3.5" />
                {importing ? "..." : t("teachers.importExcel")}
                <input type="file" accept=".xlsx,.xls,.csv" className="hidden" onChange={handleImport} disabled={importing} />
              </label>
              <Button onClick={openCreate}><Plus className="h-4 w-4" />{t("teachers.new")}</Button>
            </>
          )}
        </div>} />

      {/* toolbar */}
      <Card className="space-y-3 p-3">
        <div className="relative max-w-sm">
          <Search className="pointer-events-none absolute inset-y-0 start-3 my-auto h-4 w-4 text-faint" />
          <Input placeholder={t("action.search")} value={query} onChange={(e) => setQuery(e.target.value)} className="ps-9" />
        </div>
        <div className="flex flex-wrap items-center gap-3">
          <FilterSelect label={t("teachers.filter.subject")} value={fSubject} onChange={setFSubject}
            options={[{ value: "", label: t("students.filter.all") }, ...subjectOptions]} />
          <FilterSelect label={t("teachers.filter.pay")} value={fPay} onChange={setFPay}
            options={[
              { value: "", label: t("students.filter.all") },
              { value: "percentage", label: t("teachers.pay.percentage") },
              { value: "fixed", label: t("teachers.pay.fixed") },
            ]} />
          <div className="ms-auto flex items-center gap-2">
            <Badge tone="neutral">{t("teachers.results", { n: filtered.length })}</Badge>
            <ViewToggle value={view} onChange={setView} />
          </div>
        </div>
      </Card>

      {filtered.length === 0 ? (
        <Card className="p-6"><EmptyState icon={<Users className="h-6 w-6" />} title={t("teachers.empty")} /></Card>
      ) : view === "grid" ? (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {filtered.map((tc) => {
            const net = teacherNet(db, tc);
            return (
              <Card key={tc.id} className="card-hover group p-4">
                <button onClick={() => setDetail(tc)} className="flex w-full items-start gap-3 text-start">
                  <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl text-sm font-bold text-white" style={{ background: `linear-gradient(135deg, ${tc.color ?? "#6366f1"}, #4f46e5)` }}>
                    {tc.name.split(" ").map((p) => p[0]).slice(0, 2).join("")}
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate font-semibold text-ink">{tc.name}</p>
                    <div className="mt-0.5 flex flex-wrap gap-1">
                      {tc.subjects.slice(0, 2).map((s) => <Badge key={s} tone="violet">{subjectLabel(s, lang)}</Badge>)}
                      {tc.subjects.length > 2 && <Badge tone="neutral">+{tc.subjects.length - 2}</Badge>}
                    </div>
                  </div>
                </button>
                <div className="mt-3 grid grid-cols-3 gap-2 border-t border-line pt-3 text-center">
                  <div><p className="text-sm font-bold text-ink">{studentsOfTeacher(db, tc.id).length}</p><p className="text-[10px] text-faint">{t("students.title")}</p></div>
                  <div><p className="text-sm font-bold text-ink">{groupsOfTeacher(db, tc.id).length}</p><p className="text-[10px] text-faint">{t("classes.groups")}</p></div>
                  <div><p className="text-sm font-bold text-emerald-600">{formatMoney(net, sym)}</p><p className="text-[10px] text-faint">{t("teachers.netIncome")}</p></div>
                </div>
                {can("teachers.manage") && (
                  <div className="mt-3 flex justify-end gap-1 opacity-0 transition group-hover:opacity-100">
                    <Button variant="ghost" size="icon" onClick={() => openEdit(tc)}><Pencil className="h-4 w-4" /></Button>
                    <Button variant="ghost" size="icon" onClick={() => { remove("teachers", tc.id); pushToast(t("toast.deleted")); }}><Trash2 className="h-4 w-4 text-rose-500" /></Button>
                  </div>
                )}
              </Card>
            );
          })}
        </div>
      ) : view === "compact" ? (
        <Card className="overflow-hidden">
          <div className="divide-y divide-line/60">
            {filtered.map((tc) => {
              const net = teacherNet(db, tc);
              return (
                <div key={tc.id} className="flex items-center gap-3 px-4 py-2.5 hover:bg-elevated/40">
                  <button onClick={() => setDetail(tc)} className="flex min-w-0 flex-1 items-center gap-2.5 text-start">
                    <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-[10px] font-bold text-white" style={{ background: `linear-gradient(135deg, ${tc.color ?? "#6366f1"}, #4f46e5)` }}>{tc.name.split(" ").map((p) => p[0]).slice(0, 2).join("")}</div>
                    <div className="min-w-0">
                      <p className="truncate text-sm font-medium text-ink hover:text-brand-600">{tc.name}</p>
                      <p className="truncate text-[10px] text-faint">{tc.subjects.map((s) => subjectLabel(s, lang)).join(" · ")}</p>
                    </div>
                  </button>
                  <span className="hidden text-xs text-muted sm:inline">{studentsOfTeacher(db, tc.id).length} {t("students.title")}</span>
                  <span className="hidden text-xs text-muted sm:inline">{groupsOfTeacher(db, tc.id).length} {t("classes.groups")}</span>
                  <Badge tone="success">{formatMoney(net, sym)}</Badge>
                </div>
              );
            })}
          </div>
        </Card>
      ) : (
        <Card className="overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead><tr className="border-b border-line text-[11px] uppercase text-faint">
                <th className="px-4 py-2.5 text-start font-semibold">{t("teachers.name")}</th>
                <th className="px-4 py-2.5 text-start font-semibold">{t("teachers.subjects")}</th>
                <th className="px-4 py-2.5 text-center font-semibold">{t("students.title")}</th>
                <th className="px-4 py-2.5 text-center font-semibold">{t("classes.groups")}</th>
                <th className="px-4 py-2.5 text-end font-semibold">{t("teachers.netIncome")}</th>
              </tr></thead>
              <tbody>
                {filtered.map((tc) => {
                  const net = teacherNet(db, tc);
                  return (
                    <tr key={tc.id} className="border-b border-line/60 last:border-0 hover:bg-elevated/40">
                      <td className="px-4 py-2.5">
                        <button onClick={() => setDetail(tc)} className="flex items-center gap-2.5 text-start">
                          <div className="flex h-8 w-8 items-center justify-center rounded-full text-[10px] font-bold text-white" style={{ background: `linear-gradient(135deg, ${tc.color ?? "#6366f1"}, #4f46e5)` }}>{tc.name.split(" ").map((p) => p[0]).slice(0, 2).join("")}</div>
                          <span className="font-medium text-ink hover:text-brand-600">{tc.name}</span>
                        </button>
                      </td>
                      <td className="px-4 py-2.5">
                        <div className="flex flex-wrap gap-1">{tc.subjects.slice(0, 2).map((s) => <Badge key={s} tone="violet">{subjectLabel(s, lang)}</Badge>)}{tc.subjects.length > 2 && <Badge tone="neutral">+{tc.subjects.length - 2}</Badge>}</div>
                      </td>
                      <td className="px-4 py-2.5 text-center text-ink">{studentsOfTeacher(db, tc.id).length}</td>
                      <td className="px-4 py-2.5 text-center text-ink">{groupsOfTeacher(db, tc.id).length}</td>
                      <td className="px-4 py-2.5 text-end font-bold text-emerald-600">{formatMoney(net, sym)}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      {/* create / edit */}
      <Modal open={creating || !!editing} onClose={() => { setCreating(false); setEditing(null); }}
        title={editing ? t("teachers.edit") : t("teachers.new")} size="lg"
        footer={<><Button variant="secondary" onClick={() => { setCreating(false); setEditing(null); }}>{t("action.cancel")}</Button>
          <Button onClick={save} disabled={!form.subjects.length}>{t("action.save")}</Button></>}>
        <div className="space-y-4">
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <Field label={t("teachers.name")} required><Input value={form.name} onChange={(e) => set("name", e.target.value)} /></Field>
            <Field label={t("students.phone")}><Input value={form.phone} onChange={(e) => set("phone", e.target.value)} /></Field>
            <Field label={t("teachers.email")}><Input type="email" value={form.email} onChange={(e) => set("email", e.target.value)} /></Field>
          </div>

          {/* searchable localized subject picker */}
          <div className="rounded-xl border border-line p-3">
            <div className="mb-2 flex items-center justify-between">
              <span className="flex items-center gap-1.5 text-xs font-semibold text-ink">
                {t("teachers.subjects")}
                <span className="text-rose-500">*</span>
              </span>
              <span className="text-[10px] text-faint">{form.subjects.length} {t("teachers.subjects")}</span>
            </div>
            {form.subjects.length > 0 && (
              <div className="mb-2 flex flex-wrap gap-1.5">
                {form.subjects.map((s) => (
                  <span key={s} className="inline-flex items-center gap-1 rounded-lg bg-violet-50 px-2 py-0.5 text-xs font-medium text-violet-700 dark:bg-violet-500/15 dark:text-violet-300">
                    {subjectLabel(s, lang)}
                    <button type="button" onClick={() => set("subjects", form.subjects.filter((x) => x !== s))}><X className="h-3 w-3" /></button>
                  </span>
                ))}
              </div>
            )}
            <MultiCombobox
              selected={form.subjects}
              onChange={(v) => set("subjects", v)}
              options={subjectOptions}
              placeholder={t("teachers.addSubject")}
              searchLabel={t("combo.search")}
              selectedLabel={() => t("teachers.addSubject")}
              emptyLabel={t("combo.none")}
              allowCustom
              addLabel={t("teachers.addSubject")}
            />
          </div>

          {/* payment model */}
          <div className="rounded-xl border border-line p-3">
            <p className="mb-2 text-xs font-semibold text-ink">{t("teachers.payType")}</p>
            <div className="grid grid-cols-2 gap-2">
              <button type="button" onClick={() => set("payType", "percentage")}
                className={cn("rounded-lg border p-2.5 text-start transition", form.payType === "percentage" ? "border-brand-300 bg-brand-50 dark:border-brand-500/40 dark:bg-brand-500/15" : "border-line hover:bg-elevated")}>
                <p className="text-xs font-semibold text-ink">{t("teachers.pay.percentage")}</p>
                <p className="text-[10px] text-muted">{t("teachers.centerShare")} %</p>
              </button>
              <button type="button" onClick={() => set("payType", "fixed")}
                className={cn("rounded-lg border p-2.5 text-start transition", form.payType === "fixed" ? "border-brand-300 bg-brand-50 dark:border-brand-500/40 dark:bg-brand-500/15" : "border-line hover:bg-elevated")}>
                <p className="text-xs font-semibold text-ink">{t("teachers.pay.fixed")}</p>
                <p className="text-[10px] text-muted">{sym}</p>
              </button>
            </div>
            {form.payType === "percentage" ? (
              <Field label={t("teachers.centerShare") + " (%)"} className="mt-3"><Input type="number" value={form.commissionRate} onChange={(e) => set("commissionRate", +e.target.value)} /></Field>
            ) : (
              <Field label={t("teachers.pay.fixed") + ` (${sym})`} className="mt-3"><Input type="number" value={form.fixedAmount} onChange={(e) => set("fixedAmount", +e.target.value)} /></Field>
            )}
          </div>
        </div>
      </Modal>
    </div>
  );
}

/* --------------------------- Full teacher page --------------------------- */
function TeacherDetailPage({ teacher, onBack }: { teacher: Teacher; onBack: () => void }) {
  const { db, t, lang, upsert } = useApp();
  const sym = currencySymbol(db);
  const rev = teacherRevenue(db, teacher.id);
  const share = teacherCenterShare(db, teacher);
  const net = teacherNet(db, teacher);
  const students = studentsOfTeacher(db, teacher.id);
  const groups = groupsOfTeacher(db, teacher.id);
  const payments = useMemo(
    () => [...db.payments].filter((p) => p.teacherId === teacher.id).sort((a, b) => b.date - a.date).slice(0, 10),
    [db.payments, teacher.id],
  );
  const sessions = useMemo(
    () => db.scheduleEvents
      .filter((e) => groups.some((g) => g.id === e.groupId))
      .sort((a, b) => a.dayOfWeek - b.dayOfWeek || a.startTime.localeCompare(b.startTime)),
    [db.scheduleEvents, groups],
  );
  const DAY_KEY: Record<number, string> = { 1: "mon", 2: "tue", 3: "wed", 4: "thu", 5: "fri", 6: "sat", 7: "sun" };
  const [noteText, setNoteText] = useState(teacher.notes ?? "");
  const [stuSearch, setStuSearch] = useState("");
  const [stuGrade, setStuGrade] = useState("");
  const [stuLimit, setStuLimit] = useState(20);

  const filteredStudents = useMemo(() => {
    const q = stuSearch.toLowerCase().trim();
    return students
      .filter((s) => (!stuGrade || s.grade === stuGrade) && (!q || s.name.toLowerCase().includes(q) || s.id.toLowerCase().includes(q)))
      .sort((a, b) => a.name.localeCompare(b.name));
  }, [students, stuSearch, stuGrade]);
  const teacherGrades = useMemo(
    () => Array.from(new Set(students.map((s) => s.grade).filter(Boolean))),
    [students],
  );

  const saveNote = () => {
    upsert("teachers", { ...teacher, notes: noteText, lastUpdated: now() });
    pushToast(t("toast.saved"));
  };

  return (
    <div className="animate-fade-in space-y-5">
      {/* header */}
      <Card className="overflow-hidden">
        <div className="relative flex flex-wrap items-center gap-4 bg-gradient-to-br from-brand-600 to-indigo-800 p-5 text-white">
          <div className="absolute -right-10 -top-10 h-40 w-40 rounded-full bg-white/10 blur-2xl" />
          <Button variant="secondary" size="icon" onClick={onBack} className="relative bg-white/15 text-white hover:bg-white/25 border-0">
            <ChevronLeft className="h-5 w-5 rtl:rotate-180" />
          </Button>
          <div className="relative flex h-14 w-14 items-center justify-center rounded-2xl bg-white/15 text-lg font-bold backdrop-blur">
            {teacher.name.split(" ").map((p) => p[0]).slice(0, 2).join("")}
          </div>
          <div className="relative min-w-0 flex-1">
            <h1 className="text-xl font-bold">{teacher.name}</h1>
            <div className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-white/80">
              {teacher.phone && <span className="flex items-center gap-1"><Phone className="h-3 w-3" />{teacher.phone}</span>}
              {teacher.email && <span className="flex items-center gap-1"><Mail className="h-3 w-3" />{teacher.email}</span>}
            </div>
            <div className="mt-2 flex flex-wrap gap-1">
              {teacher.subjects.map((s) => <span key={s} className="rounded-md bg-white/15 px-2 py-0.5 text-[11px] font-medium backdrop-blur">{subjectLabel(s, lang)}</span>)}
            </div>
          </div>
          <div className="relative grid grid-cols-2 gap-2 sm:grid-cols-4">
            {[{ l: t("teachers.revenue"), v: formatMoney(rev, sym) }, { l: t("teachers.centerShare"), v: formatMoney(share, sym) }, { l: t("teachers.netIncome"), v: formatMoney(net, sym) }, { l: t("students.title"), v: String(students.length) }].map((k) => (
              <div key={k.l} className="rounded-xl bg-white/10 px-3 py-2 text-center backdrop-blur">
                <p className="text-sm font-bold">{k.v}</p>
                <p className="text-[10px] text-white/70">{k.l}</p>
              </div>
            ))}
          </div>
        </div>
      </Card>

      {/* export actions */}
      <div className="flex flex-wrap gap-2">
        <Button size="sm" variant="secondary" onClick={() => exportTeacherStudents(db, teacher, lang)}><FileDown className="h-4 w-4" />{t("teachers.exportStudents")}</Button>
        <Button size="sm" variant="secondary" onClick={() => exportTeacherFinancial(db, teacher, lang)}><FileDown className="h-4 w-4" />{t("teachers.exportFinancial")}</Button>
        <Button size="sm" variant="secondary" onClick={() => exportTeacherSchedule(db, teacher, lang)}><FileDown className="h-4 w-4" />{t("teachers.exportSchedule")}</Button>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        {/* left: groups + sessions */}
        <div className="space-y-4 lg:col-span-2">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Card className="p-4">
              <h3 className="mb-2 flex items-center gap-1.5 text-sm font-semibold text-ink"><UsersRound className="h-4 w-4 text-brand-600" />{t("classes.groups")} ({groups.length})</h3>
              <div className="space-y-1.5">
                {groups.length === 0 ? <p className="py-3 text-center text-xs text-muted">{t("classes.emptyGroups")}</p> : groups.map((g) => (
                  <div key={g.id} className="flex items-center gap-2 rounded-lg border border-line p-2 text-xs">
                    <span className="min-w-0 flex-1 truncate font-medium text-ink">{g.name}</span>
                    <Badge tone="neutral">{gradeLabel(g.grade, lang)}</Badge>
                    <Badge tone="brand">{db.students.filter((s) => s.groupIds.includes(g.id)).length}</Badge>
                  </div>
                ))}
              </div>
            </Card>
            <Card className="p-4">
              <h3 className="mb-2 flex items-center gap-1.5 text-sm font-semibold text-ink"><CalendarRange className="h-4 w-4 text-brand-600" />{t("teachers.sessions")} ({sessions.length})</h3>
              <div className="space-y-1.5">
                {sessions.length === 0 ? <p className="py-3 text-center text-xs text-muted">{t("schedule.empty")}</p> : sessions.map((e) => {
                  const g = db.groups.find((x) => x.id === e.groupId);
                  const room = db.classrooms.find((c) => c.id === e.classroomId);
                  return (
                    <div key={e.id} className="flex items-center gap-2 rounded-lg border border-line p-2 text-xs">
                      <span className="w-10 shrink-0 font-bold text-brand-600">{t(`schedule.days.${DAY_KEY[e.dayOfWeek]}`)}</span>
                      <span className="min-w-0 flex-1 truncate text-muted">{g?.name ?? "—"}</span>
                      <span className="font-medium text-ink">{formatTime12(e.startTime, lang)}</span>
                      <span className="hidden text-faint sm:inline">{room?.name ?? "—"}</span>
                    </div>
                  );
                })}
              </div>
            </Card>
          </div>

          {/* students */}
          <Card className="overflow-hidden">
            <div className="flex flex-wrap items-center gap-2 border-b border-line px-4 py-2.5">
              <span className="text-sm font-semibold text-ink">{t("students.title")} ({students.length})</span>
              <div className="ms-auto flex flex-wrap items-center gap-1.5">
                <div className="relative">
                  <Search className="pointer-events-none absolute inset-y-0 start-2 my-auto h-3.5 w-3.5 text-faint" />
                  <input value={stuSearch} onChange={(e) => setStuSearch(e.target.value)} placeholder={t("action.search")} className="h-8 w-32 rounded-md border border-line bg-surface py-1 ps-7 pe-2 text-xs text-ink focus:outline-none" />
                </div>
                <select value={stuGrade} onChange={(e) => setStuGrade(e.target.value)} className="h-8 max-w-[140px] rounded-md border border-line bg-surface px-2 text-xs text-ink focus:outline-none">
                  <option value="">{t("students.filter.all")}</option>
                  {teacherGrades.map((g) => <option key={g} value={g}>{gradeLabel(g, lang)}</option>)}
                </select>
              </div>
            </div>
            <div className="max-h-72 overflow-y-auto">
              {filteredStudents.slice(0, stuLimit).map((s) => {
                const fee = s.teachers.find((x) => x.teacherId === teacher.id)?.fee ?? 0;
                const paid = paidForTeacher(db, s.id, teacher.id);
                return (
                  <div key={s.id} className="flex items-center gap-2 border-b border-line/50 px-4 py-2 text-xs last:border-0">
                    <span className="min-w-0 flex-1 truncate text-ink">{s.name}</span>
                    <span className="hidden text-faint sm:inline">{gradeLabel(s.grade, lang)}</span>
                    <span className="text-muted">{formatMoney(fee, sym)}</span>
                    <Badge tone={paid >= fee && fee > 0 ? "success" : "warning"}>{formatMoney(paid, sym)}</Badge>
                  </div>
                );
              })}
              {filteredStudents.length === 0 && <p className="py-6 text-center text-xs text-muted">{t("combo.none")}</p>}
            </div>
            {filteredStudents.length > stuLimit && (
              <button onClick={() => setStuLimit((l) => l + 20)} className="w-full border-t border-line py-2 text-xs font-medium text-brand-600 hover:bg-elevated">
                {t("action.all")} ({filteredStudents.length - stuLimit}+)
              </button>
            )}
          </Card>

          {/* recent payments */}
          <Card className="overflow-hidden">
            <div className="border-b border-line px-4 py-2.5 text-sm font-semibold text-ink">{t("dash.recentActivity")}</div>
            <div className="max-h-60 overflow-y-auto">
              {payments.length === 0 ? <p className="py-6 text-center text-xs text-muted">{t("fin.empty")}</p> : payments.map((p) => {
                const st = db.students.find((s) => s.id === p.studentId);
                return (
                  <div key={p.id} className="flex items-center gap-2 border-b border-line/50 px-4 py-2 text-xs last:border-0">
                    <span className="min-w-0 flex-1 truncate text-ink">{st?.name ?? "—"}</span>
                    <span className="text-faint">{p.month}</span>
                    <span className="font-bold text-emerald-600">+{formatMoney(p.amount, sym)}</span>
                  </div>
                );
              })}
            </div>
          </Card>
        </div>

        {/* right: notes */}
        <Card className="flex h-fit flex-col p-4">
          <h3 className="mb-2 flex items-center gap-1.5 text-sm font-semibold text-ink"><StickyNote className="h-4 w-4 text-amber-500" />{t("teacher.note")}</h3>
          <Textarea rows={10} value={noteText} onChange={(e) => setNoteText(e.target.value)} placeholder={t("teachers.addNote")} />
          <Button className="mt-3 self-end" size="sm" onClick={saveNote}>{t("action.save")}</Button>
        </Card>
      </div>
     </div>
   );
}

