import { useMemo, useState } from "react";
import {
  Plus, Trash2, Wallet, TrendingUp, TrendingDown, ArrowDownLeft, ArrowUpRight, Building2,
} from "lucide-react";
import { useApp } from "../context/AppContext";
import {
  PageHeader, Button, Card, Input, Select, Textarea, Field, Badge, Modal, Tabs, EmptyState, pushToast,
} from "../components/ui";
import { Combobox } from "../components/ui";
import { StatCard } from "../components/widgets";
import { cn } from "../utils/cn";
import type { Payment, Expense, PaymentType, ExpenseCategory } from "../lib/types";
import { now, startOfDay, monthKey } from "../lib/db";
import {
  monthlyRevenue, monthlyExpenses, paymentTarget,
  currencySymbol, formatMoney,
} from "../lib/analytics";

const PAY_TYPES: PaymentType[] = ["MONTHLY_FEE", "EXAM_FEE", "BOOKS", "CENTER_SUBSCRIPTION", "OTHER"];
const EXP_CATS: ExpenseCategory[] = ["Rent", "Salaries", "Electricity", "Internet", "Tools", "Other"];

export function Finance() {
  const { db, t, upsert, remove, can } = useApp();
  const sym = currencySymbol(db);
  const [tab, setTab] = useState("payments");
  const income = useMemo(() => monthlyRevenue(db), [db]);
  const outcome = useMemo(() => monthlyExpenses(db), [db]);

  return (
    <div className="animate-fade-in space-y-5">
      <PageHeader title={t("fin.title")} subtitle={t("fin.subtitle")} />
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
        <StatCard icon={ArrowDownLeft} tone="emerald" label={t("fin.income")} value={formatMoney(income, sym)} sub={monthKey(now())} />
        <StatCard icon={ArrowUpRight} tone="rose" label={t("fin.outcome")} value={formatMoney(outcome, sym)} sub={monthKey(now())} />
        <StatCard icon={Wallet} tone={income - outcome >= 0 ? "brand" : "rose"} label={t("fin.balance")} value={formatMoney(income - outcome, sym)} />
      </div>
      <Tabs active={tab} onChange={setTab} tabs={[
        { id: "payments", label: t("fin.payments"), icon: <TrendingUp className="h-4 w-4" /> },
        { id: "expenses", label: t("fin.expenses"), icon: <TrendingDown className="h-4 w-4" /> },
      ]} />
      {tab === "payments" ? <Payments /> : <Expenses />}
    </div>
  );

  function Payments() {
    const [open, setOpen] = useState(false);
    const [form, setForm] = useState<Payment>({
      id: "", studentId: "", amount: 0, date: startOfDay(now()),
      type: "MONTHLY_FEE", month: monthKey(now()), teacherId: undefined, forCenter: false, notes: "", lastUpdated: now(),
    });
    const [monthMode, setMonthMode] = useState<"this" | "other">("this");
    const [err, setErr] = useState("");

    const studentOptions = useMemo(
      () => db.students.map((s) => ({ value: s.id, label: `${s.name} · ${s.id}` })),
      [db.students],
    );
    const selectedStudent = db.students.find((s) => s.id === form.studentId);
    const isCenter = form.type === "CENTER_SUBSCRIPTION";
    const isOther = form.type === "OTHER";

    const openCreate = () => {
      setForm({ id: "", studentId: "", amount: 0, date: startOfDay(now()), type: "MONTHLY_FEE", month: monthKey(now()), teacherId: undefined, forCenter: false, notes: "", lastUpdated: now() });
      setMonthMode("this"); setErr(""); setOpen(true);
    };
    const pickStudent = (id: string) => {
      const s = db.students.find((x) => x.id === id);
      setForm((f) => ({ ...f, studentId: id, amount: s && !s.isExempt ? s.teachers[0]?.fee ?? f.amount : f.amount, teacherId: s?.teachers[0]?.teacherId }));
    };
    const save = () => {
      setErr("");
      if (!form.studentId || form.amount <= 0) { setErr(t("misc.required")); return; }
      if (isOther && !form.notes?.trim()) { setErr(t("fin.otherRequired")); return; }
      const month = monthMode === "this" ? monthKey(now()) : form.month;
      upsert("payments", { ...form, amount: Math.round(form.amount), month, teacherId: isCenter ? undefined : form.teacherId, forCenter: isCenter });
      pushToast(t("toast.saved"));
      setOpen(false);
    };
    const set = <K extends keyof Payment>(k: K, v: Payment[K]) => setForm((f) => ({ ...f, [k]: v }));

    const list = useMemo(() => [...db.payments].sort((a, b) => b.date - a.date), [db.payments]);

    return (
      <Card className="overflow-hidden">
        <div className="flex items-center justify-between border-b border-line p-3">
          <h3 className="text-sm font-semibold text-ink">{t("fin.payments")}</h3>
          {can("finance.manage") && <Button size="sm" onClick={openCreate}><Plus className="h-4 w-4" />{t("fin.newPayment")}</Button>}
        </div>
        {list.length === 0 ? <div className="p-6"><EmptyState title={t("fin.empty")} /></div> : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead><tr className="border-b border-line text-[11px] uppercase text-faint">
                <th className="px-4 py-2.5 text-start font-semibold">{t("students.name")}</th>
                <th className="px-4 py-2.5 text-start font-semibold">{t("fin.paidTo")}</th>
                <th className="px-4 py-2.5 text-start font-semibold">{t("fin.type")}</th>
                <th className="px-4 py-2.5 text-start font-semibold">{t("fin.month")}</th>
                <th className="px-4 py-2.5 text-end font-semibold">{t("fin.amount")}</th>
                <th className="px-4 py-2.5"></th>
              </tr></thead>
              <tbody>
                {list.slice(0, 60).map((p) => {
                  const s = db.students.find((x) => x.id === p.studentId);
                  return (
                    <tr key={p.id} className="border-b border-line/60 last:border-0 hover:bg-elevated/40">
                      <td className="px-4 py-2.5 font-medium text-ink">{s?.name ?? "—"}</td>
                      <td className="px-4 py-2.5">
                        {p.forCenter || !p.teacherId
                          ? <Badge tone="info"><Building2 className="h-3 w-3" />{t("fin.toCenter")}</Badge>
                          : <Badge tone="violet">{paymentTarget(db, p)}</Badge>}
                      </td>
                      <td className="px-4 py-2.5"><Badge tone="success">{t(`fin.type.${p.type}`)}</Badge></td>
                      <td className="px-4 py-2.5 text-muted">{p.month}</td>
                      <td className="px-4 py-2.5 text-end font-bold text-emerald-600">+{formatMoney(p.amount, sym)}</td>
                      <td className="px-4 py-2.5 text-end">{can("finance.manage") && <Button variant="ghost" size="icon" onClick={() => remove("payments", p.id)}><Trash2 className="h-4 w-4 text-rose-500" /></Button>}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        <Modal open={open} onClose={() => setOpen(false)} title={t("fin.newPayment")} size="lg"
          footer={<><Button variant="secondary" onClick={() => setOpen(false)}>{t("action.cancel")}</Button>
            <Button onClick={save}>{t("action.save")}</Button></>}>
          <div className="space-y-3">
            {/* autocomplete student search */}
            <Field label={t("students.name")} required>
              <Combobox value={form.studentId} onChange={pickStudent} options={studentOptions}
                placeholder={t("fin.searchStudent")} allowCustom={false}
                searchLabel={t("fin.searchStudent")} emptyLabel={t("fin.noResults")} />
            </Field>

            {/* teacher allocation (hidden for center subscription) */}
            {selectedStudent && !isCenter && (
              <div className="rounded-xl border border-line p-3">
                <p className="mb-2 text-xs font-semibold text-ink">{t("fin.allocate")}</p>
                {selectedStudent.teachers.length > 1 ? (
                  <div className="grid grid-cols-1 gap-1.5 sm:grid-cols-2">
                    {selectedStudent.teachers.map((tr) => {
                      const tc = db.teachers.find((x) => x.id === tr.teacherId);
                      const on = form.teacherId === tr.teacherId;
                      return (
                        <button key={tr.teacherId} onClick={() => set("teacherId", tr.teacherId)}
                          className={cn("flex items-center justify-between rounded-lg border px-2.5 py-1.5 text-xs transition",
                            on ? "border-brand-300 bg-brand-50 text-brand-700 dark:border-brand-500/40 dark:bg-brand-500/15 dark:text-brand-200" : "border-line text-muted hover:bg-elevated")}>
                          <span className="truncate">{tc?.name}</span><span className="font-medium">{formatMoney(tr.fee, sym)}</span>
                        </button>
                      );
                    })}
                  </div>
                ) : <p className="text-xs text-muted">{db.teachers.find((x) => x.id === selectedStudent.teachers[0]?.teacherId)?.name}</p>}
              </div>
            )}

            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <Field label={`${t("fin.amountInt")} (${sym})`}><Input type="number" step={1} min={0} value={form.amount} onChange={(e) => set("amount", Math.round(+e.target.value))} /></Field>
              <Field label={t("fin.type")}>
                <Select value={form.type} onChange={(e) => set("type", e.target.value as PaymentType)}>
                  {PAY_TYPES.map((ty) => <option key={ty} value={ty}>{t(`fin.type.${ty}`)}</option>)}
                </Select>
              </Field>
            </div>

            {/* "Other" requires a description */}
            {isOther && (
              <Field label={t("fin.otherRequired")} required>
                <Input value={form.notes ?? ""} onChange={(e) => set("notes", e.target.value)} placeholder={t("fin.otherRequired")} />
              </Field>
            )}

            {/* month picker */}
            <div className="rounded-xl border border-line p-3">
              <p className="mb-2 text-xs font-semibold text-ink">{t("fin.monthPicker")}</p>
              <div className="flex flex-wrap items-center gap-3">
                <label className="inline-flex items-center gap-1.5 text-xs text-ink">
                  <input type="radio" checked={monthMode === "this"} onChange={() => setMonthMode("this")} className="accent-brand-600" />
                  {t("fin.thisMonth")} ({monthKey(now())})
                </label>
                <label className="inline-flex items-center gap-1.5 text-xs text-ink">
                  <input type="radio" checked={monthMode === "other"} onChange={() => setMonthMode("other")} className="accent-brand-600" />
                  {t("fin.otherMonth")}
                </label>
                {monthMode === "other" && (
                  <Input type="month" value={form.month} onChange={(e) => set("month", e.target.value)} className="w-40" />
                )}
              </div>
            </div>

            {!isOther && (
              <Field label={t("att.date")}><Input type="date" value={new Date(form.date).toISOString().slice(0, 10)} onChange={(e) => set("date", startOfDay(new Date(e.target.value).getTime()))} /></Field>
            )}
            {err && <p className="text-xs font-medium text-rose-600">{err}</p>}
          </div>
        </Modal>
      </Card>
    );
  }

  function Expenses() {
    const [open, setOpen] = useState(false);
    const [form, setForm] = useState<Expense>({ id: "", title: "", amount: 0, category: "Rent", date: startOfDay(now()), notes: "", lastUpdated: now() });
    const openCreate = () => { setForm({ id: "", title: "", amount: 0, category: "Rent", date: startOfDay(now()), notes: "", lastUpdated: now() }); setOpen(true); };
    const save = () => { if (!form.title.trim() || form.amount <= 0) return; upsert("expenses", { ...form, amount: Math.round(form.amount) }); pushToast(t("toast.saved")); setOpen(false); };
    const set = <K extends keyof Expense>(k: K, v: Expense[K]) => setForm((f) => ({ ...f, [k]: v }));
    const list = useMemo(() => [...db.expenses].sort((a, b) => b.date - a.date), [db.expenses]);

    return (
      <Card className="overflow-hidden">
        <div className="flex items-center justify-between border-b border-line p-3">
          <h3 className="text-sm font-semibold text-ink">{t("fin.expenses")}</h3>
          {can("finance.manage") && <Button size="sm" onClick={openCreate}><Plus className="h-4 w-4" />{t("fin.newExpense")}</Button>}
        </div>
        {list.length === 0 ? <div className="p-6"><EmptyState title={t("fin.empty")} /></div> : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead><tr className="border-b border-line text-[11px] uppercase text-faint">
                <th className="px-4 py-2.5 text-start font-semibold">{t("fin.title2")}</th>
                <th className="px-4 py-2.5 text-start font-semibold">{t("fin.category")}</th>
                <th className="px-4 py-2.5 text-start font-semibold">{t("att.date")}</th>
                <th className="px-4 py-2.5 text-end font-semibold">{t("fin.amount")}</th>
                <th className="px-4 py-2.5"></th>
              </tr></thead>
              <tbody>
                {list.map((e) => (
                  <tr key={e.id} className="border-b border-line/60 last:border-0 hover:bg-elevated/40">
                    <td className="px-4 py-2.5 font-medium text-ink">{e.title}</td>
                    <td className="px-4 py-2.5"><Badge tone="warning">{t(`fin.cat.${e.category}`)}</Badge></td>
                    <td className="px-4 py-2.5 text-muted">{new Date(e.date).toLocaleDateString()}</td>
                    <td className="px-4 py-2.5 text-end font-bold text-rose-600">-{formatMoney(e.amount, sym)}</td>
                    <td className="px-4 py-2.5 text-end">{can("finance.manage") && <Button variant="ghost" size="icon" onClick={() => remove("expenses", e.id)}><Trash2 className="h-4 w-4 text-rose-500" /></Button>}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <Modal open={open} onClose={() => setOpen(false)} title={t("fin.newExpense")}
          footer={<><Button variant="secondary" onClick={() => setOpen(false)}>{t("action.cancel")}</Button><Button onClick={save}>{t("action.save")}</Button></>}>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <Field label={t("fin.title2")} className="sm:col-span-2"><Input value={form.title} onChange={(e) => set("title", e.target.value)} placeholder="Rent · January" /></Field>
            <Field label={`${t("fin.amountInt")} (${sym})`}><Input type="number" step={1} min={0} value={form.amount} onChange={(e) => set("amount", Math.round(+e.target.value))} /></Field>
            <Field label={t("fin.category")}>
              <Select value={form.category} onChange={(e) => set("category", e.target.value as ExpenseCategory)}>
                {EXP_CATS.map((c) => <option key={c} value={c}>{t(`fin.cat.${c}`)}</option>)}
              </Select>
            </Field>
            <Field label={t("att.date")}><Input type="date" value={new Date(form.date).toISOString().slice(0, 10)} onChange={(e) => set("date", startOfDay(new Date(e.target.value).getTime()))} /></Field>
            <Field label={t("classes.notes")}><Textarea rows={2} value={form.notes} onChange={(e) => set("notes", e.target.value)} /></Field>
          </div>
        </Modal>
      </Card>
    );
  }
}
