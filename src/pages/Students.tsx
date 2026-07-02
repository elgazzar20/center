import { useMemo, useState } from "react";
import {
  Plus, Search, Pencil, Trash2, QrCode, GraduationCap, Download,
  X, UserCog, ExternalLink, Upload,
} from "lucide-react";
import { useApp } from "../context/AppContext";
import {
  PageHeader, Button, Card, Input, Select, Field, Toggle, Badge, Modal,
  EmptyState, Combobox, MultiCombobox, FilterSelect, pushToast,
} from "../components/ui";
import { QRCodeImage } from "../components/QRCode";
import { ViewToggle, type ViewMode } from "../components/ViewToggle";
import { StudentProfile } from "./StudentProfile";
import type { Student, StudentTeacher } from "../lib/types";
import { nextStudentCode, now, startOfDay } from "../lib/db";
import { GRADES, STAGE_TONE, gradeLabel } from "../lib/constants";
import {
  balanceDue, totalPaidFor, studentNetFee, currencySymbol, formatMoney,
} from "../lib/analytics";
import { parseStudentsExcel } from "../lib/excel-import";
import { cn } from "../utils/cn";

function blankStudent(): Student {
  return {
    id: "", name: "", grade: GRADES[0].id, groupIds: [], teachers: [],
    studentPhone: "", parentName: "", parentPhone: "", discount: 0,
    isExempt: false, qrCode: "", registrationDate: startOfDay(now()), lastUpdated: now(),
  };
}
function toDateInput(ts: number) { return new Date(ts).toISOString().slice(0, 10); }

export function Students() {
  const { db, t, lang, upsert, remove, can, canAdd, canDelete, canAddStudent, subscriptionPlan } = useApp();
  const sym = currencySymbol(db);
  const [query, setQuery] = useState("");
  const [editing, setEditing] = useState<Student | null>(null);
  const [creating, setCreating] = useState(false);
  const [qrStudent, setQrStudent] = useState<Student | null>(null);
  const [form, setForm] = useState<Student>(blankStudent());

  // filters
  const [fStage, setFStage] = useState("");
  const [fGrade, setFGrade] = useState("");
  const [fTeacher, setFTeacher] = useState("");
  const [fStatus, setFStatus] = useState("");
  const [profile, setProfile] = useState<Student | null>(null);
  const [view, setView] = useState<ViewMode>("table");

  const teachers = db.teachers;
  const gradeOptions = GRADES.map((g) => ({ value: g.id, label: lang === "ar" ? g.ar : g.en }));
  const groupOptions = db.groups.map((g) => ({ value: g.id, label: g.name }));

  const filtered = useMemo(() => {
    const q = query.toLowerCase().trim();
    return db.students
      .filter((s) => {
        if (q && !s.name.toLowerCase().includes(q) && !s.id.toLowerCase().includes(q) && !(s.parentPhone ?? "").includes(q)) return false;
        if (fStage) { const g = GRADES.find((x) => x.id === s.grade); if (!g || g.stage !== fStage) return false; }
        if (fGrade && s.grade !== fGrade) return false;
        if (fTeacher && !s.teachers.some((tr) => tr.teacherId === fTeacher)) return false;
        if (fStatus === "exempt" && !s.isExempt) return false;
        if (fStatus === "outstanding" && (s.isExempt || balanceDue(db, s) <= 0)) return false;
        if (fStatus === "fullPaid" && (s.isExempt || balanceDue(db, s) > 0)) return false;
        return true;
      })
      .sort((a, b) => a.name.localeCompare(b.name));
  }, [db, query, fStage, fGrade, fTeacher, fStatus]);

  if (profile) {
    const live = db.students.find((s) => s.id === profile.id) ?? profile;
    return <StudentProfile student={live} onBack={() => setProfile(null)} />;
  }



  const openCreate = () => {
    if (!canAddStudent()) {
      const limits: Record<string, number> = { free: 30, pro: 500, enterprise: 99999 };
      pushToast(`وصلت إلى الحد الأقصى (${limits[subscriptionPlan] || 30} طالب) في خطة ${subscriptionPlan === "free" ? "المجاني" : subscriptionPlan === "pro" ? "الاحترافي" : "المؤسسي"}. قم بالترقية لإضافة المزيد.`, "error");
      window.dispatchEvent(new CustomEvent("navigate", { detail: "upgrade" }));
      return;
    }
    const b = blankStudent();
    b.id = nextStudentCode(db.students);
    b.qrCode = `CPD:${b.id}`;
    if (teachers[0]) b.teachers = [{ teacherId: teachers[0].id, fee: 300 }];
    setForm(b);
    setCreating(true);
  };
  const openEdit = (s: Student) => { setForm({ ...s, teachers: s.teachers.map((x) => ({ ...x })) }); setEditing(s); };

  const save = () => {
    if (!form.name.trim() || !form.teachers.length) return;
    upsert("students", { ...form, qrCode: form.qrCode || `CPD:${form.id}`, teachers: form.teachers.filter((x) => x.teacherId) });
    pushToast(t("toast.saved"));
    setEditing(null);
    setCreating(false);
  };
  const set = <K extends keyof Student>(k: K, v: Student[K]) => setForm((f) => ({ ...f, [k]: v }));
  const updateTeacher = (idx: number, patch: Partial<StudentTeacher>) =>
    setForm((f) => ({ ...f, teachers: f.teachers.map((x, i) => (i === idx ? { ...x, ...patch } : x)) }));
  const teacherName = (id: string) => teachers.find((x) => x.id === id)?.name ?? "—";

  // Excel import
  const [importing, setImporting] = useState(false);
  const handleImport = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setImporting(true);
    const result = await parseStudentsExcel(file, db.students);
    setImporting(false);
    if (result.success && result.data.length > 0) {
      result.data.forEach((s) => upsert("students", s));
      pushToast(t("students.importSuccess", { n: result.count }));
    } else {
      pushToast(t("students.importError", { error: result.errors[0] || "Unknown" }), "error");
    }
    e.target.value = "";
  };

  return (
    <div className="animate-fade-in space-y-5">
      <PageHeader title={t("students.title")} subtitle={t("students.subtitle")}
        actions={<div className="flex items-center gap-2">
          {can("students.manage") && canAdd() && (
            <>
              <label className="inline-flex h-9 cursor-pointer items-center gap-1.5 rounded-lg border border-line bg-elevated px-3 text-xs font-medium text-ink transition hover:bg-line">
                <Upload className="h-3.5 w-3.5" />
                {importing ? t("students.importing") : t("students.importExcel")}
                <input type="file" accept=".xlsx,.xls,.csv" className="hidden" onChange={handleImport} disabled={importing} />
              </label>
              <Button onClick={openCreate}><Plus className="h-4 w-4" />{t("students.new")}</Button>
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
          <FilterSelect label={t("students.filter.stage")} value={fStage} onChange={setFStage}
            options={[{ value: "", label: t("students.filter.all") },
              { value: "pre", label: lang === "ar" ? "تمهيدي" : "Pre-Primary" },
              { value: "primary", label: lang === "ar" ? "ابتدائي" : "Primary" },
              { value: "prep", label: lang === "ar" ? "إعدادي" : "Preparatory" },
              { value: "secondary", label: lang === "ar" ? "ثانوي" : "Secondary" }]} />
          <FilterSelect label={t("students.filter.grade")} value={fGrade} onChange={setFGrade}
            options={[{ value: "", label: t("students.filter.all") }, ...gradeOptions]} />
          <FilterSelect label={t("students.filter.teacher")} value={fTeacher} onChange={setFTeacher}
            options={[{ value: "", label: t("students.filter.all") }, ...teachers.map((tc) => ({ value: tc.id, label: tc.name }))]} />
          <FilterSelect label={t("students.filter.status")} value={fStatus} onChange={setFStatus}
            options={[{ value: "", label: t("students.filter.all") },
              { value: "exempt", label: t("students.filter.exemptOnly") },
              { value: "outstanding", label: t("students.filter.outstanding") },
              { value: "fullPaid", label: t("students.filter.fullPaid") }]} />
          <div className="ms-auto flex items-center gap-2">
            <Badge tone="brand">{t("students.results", { n: filtered.length })}</Badge>
            <ViewToggle value={view} onChange={setView} />
          </div>
        </div>
      </Card>

      <Card className="overflow-hidden p-0">
        {filtered.length === 0 ? (
          <div className="p-6"><EmptyState icon={<GraduationCap className="h-6 w-6" />} title={t("students.empty")} action={can("students.manage") ? <Button onClick={openCreate} size="sm"><Plus className="h-4 w-4" />{t("students.new")}</Button> : undefined} /></div>
        ) : view === "grid" ? (
          <div className="grid grid-cols-1 gap-3 p-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {filtered.map((s) => {
              const due = balanceDue(db, s);
              const grade = GRADES.find((g) => g.id === s.grade);
              return (
                <div key={s.id} className="card-hover flex flex-col gap-2 rounded-xl border border-line bg-surface p-3">
                  <button onClick={() => setProfile(s)} className="flex items-center gap-2.5 text-start">
                    <div className="flex h-10 w-10 items-center justify-center rounded-full bg-gradient-to-br from-brand-400 to-brand-600 text-sm font-bold text-white">{s.name.split(" ").map((p) => p[0]).slice(0, 2).join("")}</div>
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-semibold text-ink hover:text-brand-600">{s.name}</p>
                      <p className="font-mono text-[10px] text-faint">{s.id}</p>
                    </div>
                  </button>
                  <div className="flex flex-wrap items-center gap-1">
                    {grade && <span className={cn("rounded px-1.5 py-0.5 text-[10px] font-medium", STAGE_TONE[grade.stage])}>{gradeLabel(s.grade, lang)}</span>}
                    {s.isExempt ? <Badge tone="info">{t("students.exempt")}</Badge>
                      : due > 0 ? <Badge tone="danger">{formatMoney(due, sym)}</Badge>
                        : <Badge tone="success">{t("students.paid")}</Badge>}
                  </div>
                  <p className="truncate text-[11px] text-muted">{teacherName(s.teachers[0]?.teacherId ?? "")}{s.teachers.length > 1 && ` +${s.teachers.length - 1}`}</p>
                  <div className="flex items-center justify-between border-t border-line pt-2">
                    <span className="text-xs font-bold text-ink">{formatMoney(studentNetFee(s), sym)}</span>
                    <div className="flex gap-1">
                      <Button variant="ghost" size="icon" onClick={() => setProfile(s)}><ExternalLink className="h-3.5 w-3.5" /></Button>
                      <Button variant="ghost" size="icon" onClick={() => setQrStudent(s)}><QrCode className="h-3.5 w-3.5" /></Button>
                      {can("students.manage") && canDelete() && <Button variant="ghost" size="icon" onClick={() => { remove("students", s.id); pushToast(t("toast.deleted")); }}><Trash2 className="h-3.5 w-3.5 text-rose-500" /></Button>}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        ) : view === "compact" ? (
          <div className="divide-y divide-line/60">
            {filtered.map((s) => {
              const due = balanceDue(db, s);
              return (
                <div key={s.id} className="flex items-center gap-3 px-4 py-2 hover:bg-elevated/40">
                  <button onClick={() => setProfile(s)} className="flex min-w-0 flex-1 items-center gap-2.5 text-start">
                    <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-brand-400 to-brand-600 text-[10px] font-bold text-white">{s.name.split(" ").map((p) => p[0]).slice(0, 2).join("")}</div>
                    <div className="min-w-0">
                      <p className="truncate text-sm font-medium text-ink hover:text-brand-600">{s.name}</p>
                      <p className="font-mono text-[10px] text-faint">{s.id} · {teacherName(s.teachers[0]?.teacherId ?? "")}</p>
                    </div>
                  </button>
                  {s.isExempt ? <Badge tone="info">{t("students.exempt")}</Badge>
                    : due > 0 ? <Badge tone="danger">{formatMoney(due, sym)}</Badge>
                      : <Badge tone="success">{t("students.paid")}</Badge>}
                  <span className="hidden text-xs font-bold text-ink sm:inline">{formatMoney(studentNetFee(s), sym)}</span>
                  <Button variant="ghost" size="icon" onClick={() => setProfile(s)}><ExternalLink className="h-3.5 w-3.5" /></Button>
                </div>
              );
            })}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-line text-start text-[11px] uppercase tracking-wide text-faint">
                  <th className="px-4 py-2.5 text-start font-semibold">{t("students.code")}</th>
                  <th className="px-4 py-2.5 text-start font-semibold">{t("students.name")}</th>
                  <th className="px-4 py-2.5 text-start font-semibold">{t("students.teachers")}</th>
                  <th className="px-4 py-2.5 text-start font-semibold">{t("students.totalFee")}</th>
                  <th className="px-4 py-2.5 text-start font-semibold">{t("students.balance")}</th>
                  <th className="px-4 py-2.5 text-end font-semibold">{t("action.details")}</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((s) => {
                  const due = balanceDue(db, s);
                  const grade = GRADES.find((g) => g.id === s.grade);
                  return (
                    <tr key={s.id} className="border-b border-line/60 last:border-0 hover:bg-elevated/40">
                      <td className="px-4 py-2.5"><span className="font-mono text-xs text-muted">{s.id}</span></td>
                      <td className="px-4 py-2.5">
                        <button onClick={() => setProfile(s)} className="flex items-center gap-2.5 text-start transition hover:opacity-80">
                          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-gradient-to-br from-brand-400 to-brand-600 text-[11px] font-bold text-white">
                            {s.name.split(" ").map((p) => p[0]).slice(0, 2).join("")}
                          </div>
                          <div className="min-w-0">
                            <p className="font-medium text-ink hover:text-brand-600">{s.name}</p>
                            {grade && <span className={cn("inline-flex rounded px-1.5 py-0.5 text-[10px] font-medium", STAGE_TONE[grade.stage])}>{gradeLabel(s.grade, lang)}</span>}
                          </div>
                          {s.isExempt && <Badge tone="info">{t("students.exempt")}</Badge>}
                        </button>
                      </td>
                      <td className="px-4 py-2.5">
                        {s.teachers.length > 1
                          ? <Badge tone="brand">{t("students.multiTeacher", { n: s.teachers.length })}</Badge>
                          : <span className="text-xs text-muted">{teacherName(s.teachers[0]?.teacherId ?? "")}</span>}
                      </td>
                      <td className="px-4 py-2.5 font-medium text-ink">{formatMoney(studentNetFee(s), sym)}</td>
                      <td className="px-4 py-2.5">
                        {s.isExempt ? <Badge tone="neutral">{t("students.exempt")}</Badge>
                          : due > 0 ? <Badge tone="danger">{formatMoney(due, sym)}</Badge>
                            : <Badge tone="success">{t("students.paid")}</Badge>}
                      </td>
                      <td className="px-4 py-2.5">
                        <div className="flex items-center justify-end gap-1">
                          <Button variant="ghost" size="icon" onClick={() => setProfile(s)} title={t("action.view")}><ExternalLink className="h-4 w-4" /></Button>
                          <Button variant="ghost" size="icon" onClick={() => setQrStudent(s)} title={t("students.qr")}><QrCode className="h-4 w-4" /></Button>
                          {can("students.manage") && (<>
                            <Button variant="ghost" size="icon" onClick={() => openEdit(s)}><Pencil className="h-4 w-4" /></Button>
                            {canDelete() && <Button variant="ghost" size="icon" onClick={() => { remove("students", s.id); pushToast(t("toast.deleted")); }}><Trash2 className="h-4 w-4 text-rose-500" /></Button>}
                          </>)}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {/* create / edit modal */}
      <Modal open={creating || !!editing} onClose={() => { setCreating(false); setEditing(null); }}
        title={editing ? t("students.edit") : t("students.new")} size="lg"
        footer={<><Button variant="secondary" onClick={() => { setCreating(false); setEditing(null); }}>{t("action.cancel")}</Button>
          <Button onClick={save} disabled={!form.teachers.length}>{t("action.save")}</Button></>}>
        <div className="space-y-4">
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <Field label={t("students.code")}><Input value={form.id} disabled className="font-mono" /></Field>
            <Field label={t("students.name")} required><Input value={form.name} onChange={(e) => set("name", e.target.value)} placeholder="Ahmed Ali" /></Field>
            <Field label={t("students.grade")}>
              <Combobox value={form.grade} onChange={(v) => set("grade", v)} options={gradeOptions}
                placeholder={t("students.grade")} allowCustom
                searchLabel={t("combo.search")} addLabel={t("combo.add")} emptyLabel={t("combo.none")} />
            </Field>
            <Field label={t("students.registered")}><Input type="date" value={toDateInput(form.registrationDate)} onChange={(e) => set("registrationDate", startOfDay(new Date(e.target.value).getTime()))} /></Field>
            <Field label={`${t("students.parentName")}`}><Input value={form.parentName} onChange={(e) => set("parentName", e.target.value)} /></Field>
            <Field label={`${t("students.parentPhone")}`}><Input value={form.parentPhone} onChange={(e) => set("parentPhone", e.target.value)} /></Field>
            <Field label={`${t("students.studentPhone")}`}><Input value={form.studentPhone} onChange={(e) => set("studentPhone", e.target.value)} /></Field>
            <Field label={`${t("students.discount")} (${sym})`}><Input type="number" min={0} value={form.discount} onChange={(e) => set("discount", +e.target.value)} /></Field>
          </div>

          {/* teachers with per-teacher fees */}
          <div className="rounded-xl border border-line p-3">
            <div className="mb-2 flex items-center justify-between">
              <span className="flex items-center gap-1.5 text-xs font-semibold text-ink"><UserCog className="h-4 w-4 text-brand-600" />{t("students.teachers")}</span>
              <Button size="sm" variant="subtle" onClick={() => set("teachers", [...form.teachers, { teacherId: teachers[0]?.id ?? "", fee: 300 }])} disabled={!teachers.length}>
                <Plus className="h-3.5 w-3.5" />{t("students.addTeacher")}
              </Button>
            </div>
            {!teachers.length && <p className="py-2 text-center text-[11px] text-amber-600">{t("teachers.empty")}</p>}
            <div className="space-y-2">
              {form.teachers.map((tr, idx) => (
                <div key={idx} className="flex items-center gap-2">
                  <Select value={tr.teacherId} onChange={(e) => updateTeacher(idx, { teacherId: e.target.value })} className="flex-1">
                    <option value="">{t("students.selectTeacher")}</option>
                    {teachers.map((tc) => <option key={tc.id} value={tc.id}>{tc.name}</option>)}
                  </Select>
                  <div className="relative w-32">
                    <Input type="number" min={0} value={tr.fee} onChange={(e) => updateTeacher(idx, { fee: +e.target.value })} className="ps-8" />
                    <span className="pointer-events-none absolute inset-y-0 start-2.5 my-auto text-[10px] text-faint">{sym}</span>
                  </div>
                  <Button variant="ghost" size="icon" onClick={() => set("teachers", form.teachers.filter((_, i) => i !== idx))}><X className="h-4 w-4 text-rose-500" /></Button>
                </div>
              ))}
            </div>
            <div className="mt-2 flex items-center justify-between border-t border-line pt-2 text-xs">
              <Toggle checked={form.isExempt} onChange={(v) => set("isExempt", v)} label={t("students.exempt")} />
              <span className="font-semibold text-ink">{t("students.totalFee")}: {formatMoney(form.isExempt ? 0 : studentNetFee(form), sym)}</span>
            </div>
            {!form.teachers.length && <p className="mt-1 text-[11px] text-rose-500">{t("students.noTeachers")}</p>}
          </div>

          {/* groups dropdown */}
          <Field label={`${t("students.groups")}`} hint={t("combo.selectGroups")}>
            <MultiCombobox
              selected={form.groupIds} onChange={(v) => set("groupIds", v)}
              options={groupOptions}
              placeholder={t("combo.selectGroups")}
              searchLabel={t("combo.search")}
              selectedLabel={(n) => t("combo.selected", { n })}
              emptyLabel={t("classes.emptyGroups")}
            />
            {form.groupIds.length > 0 && (
              <div className="mt-2 flex flex-wrap gap-1.5">
                {form.groupIds.map((gid) => {
                  const g = db.groups.find((x) => x.id === gid);
                  return (
                    <span key={gid} className="inline-flex items-center gap-1 rounded-lg bg-brand-50 px-2 py-0.5 text-xs font-medium text-brand-700 dark:bg-brand-500/15 dark:text-brand-200">
                      {g?.name ?? gid}
                      <button type="button" onClick={() => set("groupIds", form.groupIds.filter((x) => x !== gid))}><X className="h-3 w-3" /></button>
                    </span>
                  );
                })}
              </div>
            )}
          </Field>
        </div>
      </Modal>

      {/* QR modal */}
      <Modal open={!!qrStudent} onClose={() => setQrStudent(null)} title={t("students.qr")} size="sm"
        footer={<Button variant="secondary" onClick={() => setQrStudent(null)}>{t("action.close")}</Button>}>
        {qrStudent && (
          <div className="flex flex-col items-center gap-3 py-2">
            <div className="rounded-xl border border-line bg-white p-3"><QRCodeImage value={qrStudent.qrCode} size={180} /></div>
            <div className="text-center">
              <p className="font-mono text-sm font-bold text-ink">{qrStudent.id}</p>
              <p className="text-sm text-muted">{qrStudent.name}</p>
              <p className="mt-1 text-[11px] text-faint">{t("students.paid")}: {formatMoney(totalPaidFor(db, qrStudent.id), sym)}</p>
            </div>
            <a href="#" onClick={(e) => { e.preventDefault(); downloadQr(qrStudent); }} className="inline-flex items-center gap-1.5 text-xs font-medium text-brand-600 hover:underline">
              <Download className="h-3.5 w-3.5" />{t("action.export")}
            </a>
          </div>
        )}
      </Modal>
    </div>
  );
}

async function downloadQr(s: Student) {
  const { default: QRCode } = await import("qrcode");
  const url = await QRCode.toDataURL(s.qrCode, { width: 480, margin: 2 });
  const a = document.createElement("a");
  a.href = url;
  a.download = `${s.id}_qr.png`;
  a.click();
}
