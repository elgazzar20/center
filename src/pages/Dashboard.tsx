import { useMemo } from "react";
import {
  GraduationCap, Boxes, ClipboardCheck, TrendingUp, TrendingDown, Wallet, Coins,
  Clock, AlertCircle, ArrowRight, Crown, Building2, Sparkles,
} from "lucide-react";
import { useApp } from "../context/AppContext";
import { cn } from "../utils/cn";
import { PageHeader, Card, Badge, EmptyState } from "../components/ui";
import { StatCard, ChartCard } from "../components/widgets";
import { LineAreaChart, BarChart, Donut } from "../components/charts";
import {
  monthlyRevenue, monthlyExpenses, monthlyCenterIncome, monthlySeries,
  attendanceRate, attendanceTrend, gradeDistribution, balanceDue,
  teacherRevenue, currencySymbol, formatMoney,
} from "../lib/analytics";
import { dayOfWeekOf } from "../lib/db";
import { GRADES, formatTime12 } from "../lib/constants";

export function Dashboard({ onNavigate }: { onNavigate: (id: string) => void }) {
  const { db, t, lang, can } = useApp();
  const sym = currencySymbol(db);
  const isAr = lang === "ar";
  // revenue visibility is gated by RBAC permissions
  const showCenterRevenue = can("revenue.center");
  const showTeacherRevenue = can("revenue.teachers");

  const stats = useMemo(() => {
    const rev = monthlyRevenue(db);
    const exp = monthlyExpenses(db);
    const net = rev - exp;
    const centerIncome = monthlyCenterIncome(db);
    return {
      students: db.students.length,
      groups: db.groups.length,
      attRate: attendanceRate(db, { days: 30 }),
      rev, exp, net, centerIncome,
      collections: db.payments.filter((p) => p.month === monthKeyOf(now2())).length,
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [db]);

  const series = useMemo(() => monthlySeries(db, 6), [db]);
  const attTrend = useMemo(() => attendanceTrend(db, 14), [db]);
  const grades = useMemo(() => gradeDistribution(db), [db]);

  const byGrade = useMemo(() => {
    return GRADES.map((g) => ({
      label: (isAr ? g.ar : g.en).replace(/Grade |الصف |Primary|الابتدائي|Preparatory|الإعدادي|Secondary|الثانوي|Kindergarten |التمهيدي/g, "").trim() || g.id,
      value: db.students.filter((s) => s.grade === g.id).length,
    })).filter((x) => x.value > 0);
  }, [db.students, isAr]);

  const topTeachers = useMemo(
    () => db.teachers
      .map((tc) => ({ tc, rev: teacherRevenue(db, tc.id) }))
      .sort((a, b) => b.rev - a.rev)
      .slice(0, 5),
    [db],
  );

  const feeStatus = useMemo(() => {
    let full = 0, outstanding = 0, exempt = 0;
    for (const s of db.students) {
      if (s.isExempt) { exempt++; continue; }
      if (balanceDue(db, s) > 0) outstanding++; else full++;
    }
    return { full, outstanding, exempt };
  }, [db]);

  const todayEvents = useMemo(() => {
    const today = dayOfWeekOf(Date.now());
    return db.scheduleEvents
      .filter((e) => e.dayOfWeek === today)
      .sort((a, b) => a.startTime.localeCompare(b.startTime))
      .map((e) => ({ ...e, group: db.groups.find((g) => g.id === e.groupId), room: db.classrooms.find((c) => c.id === e.classroomId) }));
  }, [db]);

  const pendingFees = useMemo(
    () => db.students.map((s) => ({ s, due: balanceDue(db, s) })).filter((x) => x.due > 0).sort((a, b) => b.due - a.due).slice(0, 5),
    [db],
  );
  const recentPayments = useMemo(
    () => [...db.payments].sort((a, b) => b.date - a.date).slice(0, 6).map((p) => ({ ...p, student: db.students.find((s) => s.id === p.studentId) })),
    [db],
  );

  const fmt = (v: number) => (v >= 1000 ? `${Math.round(v / 1000)}k` : `${Math.round(v)}`);
  const feeTotal = feeStatus.full + feeStatus.outstanding + feeStatus.exempt || 1;

  return (
    <div className="animate-fade-in space-y-6">
      <PageHeader title={t("dash.title")} subtitle={t("dash.subtitle")} />

      {/* Subscription Status Banner */}
      <SubscriptionBanner />

      {/* hero banner: center income (gated by revenue.center permission) */}
      {showCenterRevenue ? (
      <Card className="mesh-brand relative overflow-hidden border-0 text-white shadow-[var(--shadow-brand)]">
        <div className="orb float-soft -right-8 -top-12 h-40 w-40 bg-white/12" />
        <div className="orb float-soft -bottom-16 right-1/3 h-44 w-44 bg-accent-400/20" style={{ animationDelay: "1s" }} />
        <div className="relative flex flex-wrap items-center justify-between gap-4 p-5">
          <div className="relative">
            <p className="flex items-center gap-1.5 text-xs font-medium text-white/70">
              <Building2 className="h-3.5 w-3.5" />
              {t("dash.centerIncome")} · {monthKeyOf(now2())}
            </p>
            <p className="mt-1 text-3xl font-bold tracking-tight">{formatMoney(stats.centerIncome, sym)}</p>
            <p className="mt-1 text-xs text-white/70">
              {t("dash.collections")}: {stats.collections} · {t("dash.netProfit")}: {formatMoney(stats.net, sym)}
            </p>
          </div>
          <div className="relative flex gap-2">
            <div className="rounded-xl bg-white/12 px-4 py-2 text-center ring-1 ring-white/15 backdrop-blur transition hover:bg-white/20">
              <p className="text-lg font-bold">{stats.students}</p>
              <p className="text-[10px] text-white/70">{t("dash.totalStudents")}</p>
            </div>
            <div className="rounded-xl bg-white/12 px-4 py-2 text-center ring-1 ring-white/15 backdrop-blur transition hover:bg-white/20">
              <p className="text-lg font-bold">{stats.groups}</p>
              <p className="text-[10px] text-white/70">{t("dash.activeGroups")}</p>
            </div>
            <div className="rounded-xl bg-white/12 px-4 py-2 text-center ring-1 ring-white/15 backdrop-blur transition hover:bg-white/20">
              <p className="text-lg font-bold">{Math.round(stats.attRate)}%</p>
              <p className="text-[10px] text-white/70">{t("dash.attendanceRate")}</p>
            </div>
          </div>
        </div>
      </Card>
      ) : null}

      {/* KPI cards */}
      <div className="stagger grid grid-cols-2 gap-3 md:grid-cols-3 xl:grid-cols-6">
        <StatCard icon={GraduationCap} tone="brand" label={t("dash.totalStudents")} value={stats.students} />
        <StatCard icon={Boxes} tone="violet" label={t("dash.activeGroups")} value={stats.groups} />
        <StatCard icon={ClipboardCheck} tone="sky" label={t("dash.attendanceRate")} value={`${Math.round(stats.attRate)}%`} />
        {showCenterRevenue && <StatCard icon={Wallet} tone="emerald" label={t("dash.monthlyRevenue")} value={formatMoney(stats.rev, sym)} />}
        {showCenterRevenue && <StatCard icon={Coins} tone="amber" label={t("dash.monthlyExpenses")} value={formatMoney(stats.exp, sym)} />}
        {showCenterRevenue && <StatCard icon={stats.net >= 0 ? TrendingUp : TrendingDown} tone={stats.net >= 0 ? "emerald" : "rose"} label={t("dash.netProfit")} value={formatMoney(stats.net, sym)} />}
      </div>

      {/* charts row 1 */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        {showCenterRevenue && (
        <ChartCard className="lg:col-span-2" title={t("dash.revenueVsExpenses")} subtitle={isAr ? "آخر ٦ أشهر" : "Last 6 months"}>
          <div className="mb-4 grid grid-cols-3 gap-2">
            <div className="rounded-xl border border-emerald-500/20 bg-emerald-50/60 p-2.5 dark:bg-emerald-500/10">
              <p className="text-[10px] font-medium text-emerald-700 dark:text-emerald-300">{t("dash.monthlyRevenue")}</p>
              <p className="text-base font-bold text-emerald-600">{formatMoney(stats.rev, sym)}</p>
            </div>
            <div className="rounded-xl border border-rose-500/20 bg-rose-50/60 p-2.5 dark:bg-rose-500/10">
              <p className="text-[10px] font-medium text-rose-700 dark:text-rose-300">{t("dash.monthlyExpenses")}</p>
              <p className="text-base font-bold text-rose-600">{formatMoney(stats.exp, sym)}</p>
            </div>
            <div className={cn("rounded-xl border p-2.5", stats.net >= 0 ? "border-brand-500/20 bg-brand-50/60 dark:bg-brand-500/10" : "border-rose-500/20 bg-rose-50/60 dark:bg-rose-500/10")}>
              <p className={cn("text-[10px] font-medium", stats.net >= 0 ? "text-brand-700 dark:text-brand-300" : "text-rose-700 dark:text-rose-300")}>{t("dash.netProfit")}</p>
              <p className={cn("text-base font-bold", stats.net >= 0 ? "text-brand-600" : "text-rose-600")}>{formatMoney(stats.net, sym)}</p>
            </div>
          </div>
          <LineAreaChart
            labels={series.map((s) => s.month.slice(5))}
            series={[
              { name: t("fin.payments"), color: "#10b981", values: series.map((s) => s.revenue) },
              { name: t("fin.expenses"), color: "#f43f5e", values: series.map((s) => s.expenses) },
            ]}
            formatY={fmt}
          />
        </ChartCard>
        )}

        <ChartCard className={showCenterRevenue ? "" : "lg:col-span-3"} title={t("dash.feeStatus")} subtitle={`${feeStatus.full + feeStatus.outstanding + feeStatus.exempt} ${t("dash.totalStudents")}`}>
          <div className="flex flex-col items-center gap-3 py-2">
            <Donut
              value={(feeStatus.full / feeTotal) * 100}
              color="#10b981"
              label={`${feeStatus.full}`}
              sublabel={t("dash.full")}
            />
            <div className="grid w-full grid-cols-3 gap-2 text-center">
              <div className="rounded-lg bg-emerald-50 p-2 dark:bg-emerald-500/10">
                <p className="text-base font-bold text-emerald-600">{feeStatus.full}</p>
                <p className="text-[10px] text-muted">{t("dash.full")}</p>
              </div>
              <div className="rounded-lg bg-rose-50 p-2 dark:bg-rose-500/10">
                <p className="text-base font-bold text-rose-600">{feeStatus.outstanding}</p>
                <p className="text-[10px] text-muted">{t("dash.outstanding")}</p>
              </div>
              <div className="rounded-lg bg-sky-50 p-2 dark:bg-sky-500/10">
                <p className="text-base font-bold text-sky-600">{feeStatus.exempt}</p>
                <p className="text-[10px] text-muted">{t("dash.exempt")}</p>
              </div>
            </div>
          </div>
        </ChartCard>
      </div>

      {/* charts row 2 */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <ChartCard title={t("dash.attendanceTrends")} subtitle={isAr ? "آخر ١٤ يوماً" : "Last 14 days"}>
          <LineAreaChart height={180} labels={attTrend.map((d) => d.label)} series={[{ name: t("dash.attendanceRate"), color: "#6366f1", values: attTrend.map((d) => d.rate) }]} formatY={(v) => `${Math.round(v)}%`} />
        </ChartCard>
        <ChartCard title={t("dash.gradeDistribution")} subtitle={isAr ? "كل الامتحانات" : "Across all exams"}>
          <BarChart data={grades.map((g) => ({ label: g.label, value: g.count }))} color="#8b5cf6" />
        </ChartCard>
      </div>

      {/* by grade + top teachers */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <ChartCard title={t("dash.byGrade")} subtitle={isAr ? "توزيع الطلاب" : "Student distribution"}>
          {byGrade.length === 0 ? <EmptyState title={t("students.empty")} /> : <BarChart data={byGrade} color="#0ea5e9" />}
        </ChartCard>
        <Card className="p-5">
          <div className="mb-3 flex items-center gap-2"><Crown className="h-4 w-4 text-amber-500" /><h3 className="text-sm font-semibold text-ink">{t("dash.topTeachers")}</h3></div>
          {topTeachers.length === 0 ? <EmptyState title={t("teachers.empty")} /> : (
            <div className="space-y-2">
              {topTeachers.map(({ tc, rev }, i) => (
                <div key={tc.id} className="flex items-center gap-2.5">
                  <span className={`flex h-6 w-6 items-center justify-center rounded-full text-[11px] font-bold ${i === 0 ? "bg-amber-100 text-amber-700 dark:bg-amber-500/20 dark:text-amber-300" : "bg-elevated text-muted"}`}>{i + 1}</span>
                  <div className="h-7 w-7 shrink-0 rounded-full text-[10px] font-bold text-white grid place-items-center" style={{ background: tc.color ?? "#6366f1" }}>
                    {tc.name.split(" ").map((p) => p[0]).slice(0, 2).join("")}
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-xs font-medium text-ink">{tc.name}</p>
                    <p className="truncate text-[10px] text-faint">{tc.subjects.join(" · ")}</p>
                  </div>
                  {showTeacherRevenue && <span className="text-xs font-bold text-emerald-600">{formatMoney(rev, sym)}</span>}
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>

      {/* schedule + fees + activity */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <Card className="card-hover p-5">
          <div className="mb-3 flex items-center gap-2"><Clock className="h-4 w-4 text-brand-600" /><h3 className="text-sm font-semibold text-ink">{t("dash.upcoming")}</h3></div>
          {todayEvents.length === 0 ? <p className="py-6 text-center text-xs text-muted">{t("dash.noSchedule")}</p> : (
            <div className="space-y-2">
              {todayEvents.map((e) => (
                <div key={e.id} className="flex items-center gap-3 rounded-lg border border-line p-2.5">
                  <div className="flex h-9 w-14 shrink-0 flex-col items-center justify-center rounded-lg bg-brand-50 text-brand-700 dark:bg-brand-500/15 dark:text-brand-200">
                    <span className="text-[10px] font-bold leading-none">{formatTime12(e.startTime, lang)}</span>
                    <span className="text-[9px] leading-none opacity-70">{formatTime12(e.endTime, lang)}</span>
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-xs font-semibold text-ink">{e.group?.name ?? "—"}</p>
                    <p className="truncate text-[11px] text-muted">{e.room?.name ?? "—"}</p>
                  </div>
                </div>
              ))}
            </div>
          )}
        </Card>

        <Card className="p-5">
          <div className="mb-3 flex items-center justify-between gap-2">
            <div className="flex items-center gap-2"><AlertCircle className="h-4 w-4 text-amber-600" /><h3 className="text-sm font-semibold text-ink">{t("dash.pendingFees")}</h3></div>
            <button onClick={() => onNavigate("finance")} className="text-[11px] font-medium text-brand-600 hover:underline">{t("dash.viewReport")}</button>
          </div>
          {pendingFees.length === 0 ? <p className="py-6 text-center text-xs text-muted">{t("fin.empty")}</p> : (
            <div className="space-y-2">
              {pendingFees.map(({ s, due }) => (
                <div key={s.id} className="flex items-center gap-2.5">
                  <div className="min-w-0 flex-1"><p className="truncate text-xs font-medium text-ink">{s.name}</p><p className="text-[10px] text-faint">{s.id}</p></div>
                  {showCenterRevenue ? <Badge tone="danger">{formatMoney(due, sym)}</Badge> : <Badge tone="warning">{t("dash.outstanding")}</Badge>}
                </div>
              ))}
            </div>
          )}
        </Card>

        <Card className="p-5">
          <div className="mb-3 flex items-center justify-between gap-2">
            <h3 className="text-sm font-semibold text-ink">{t("dash.recentActivity")}</h3>
            <button onClick={() => onNavigate("finance")} className="text-[11px] font-medium text-brand-600 hover:underline">{t("action.view")}</button>
          </div>
          {recentPayments.length === 0 ? <EmptyState title={t("fin.empty")} /> : (
            <div className="space-y-2.5">
              {recentPayments.map((p) => (
                <div key={p.id} className="flex items-center gap-2.5">
                  <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-emerald-50 text-emerald-600 dark:bg-emerald-500/15"><ArrowRight className="h-3.5 w-3.5 rtl:rotate-180" /></div>
                  <div className="min-w-0 flex-1"><p className="truncate text-xs font-medium text-ink">{p.student?.name ?? "—"}</p><p className="text-[10px] text-faint">{t(`fin.type.${p.type}`)}</p></div>
                  {showCenterRevenue && <span className="text-xs font-bold text-emerald-600">+{formatMoney(p.amount, sym)}</span>}
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}

/* ====================== Subscription Banner ====================== */
function SubscriptionBanner() {
  const { subscriptionPlan } = useApp();
  const planLabels: Record<string, string> = { free: "مجاني", pro: "احترافي", enterprise: "مؤسسي" };
  const planLabel = planLabels[subscriptionPlan] || "مجاني";
  const isFree = subscriptionPlan === "free";

  if (!isFree) {
    // Show active plan banner
    return (
      <Card className="flex items-center justify-between gap-3 border-emerald-200/60 bg-gradient-to-r from-emerald-50 to-teal-50 p-4 dark:border-emerald-500/20 dark:from-emerald-500/5 dark:to-teal-500/5">
        <div className="flex items-center gap-2.5">
          <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-emerald-500 text-white">
            <Crown className="h-5 w-5" />
          </div>
          <div>
            <p className="text-xs text-muted">اشتراكك الحالي</p>
            <p className="text-sm font-bold text-emerald-700 dark:text-emerald-300">الخطة {planLabel}</p>
          </div>
        </div>
        <Badge tone="success">نشط</Badge>
      </Card>
    );
  }

  // Show upgrade prompt for free users
  return (
    <Card className="flex flex-wrap items-center justify-between gap-3 border-amber-200/60 bg-gradient-to-r from-amber-50 to-orange-50 p-4 dark:border-amber-500/20 dark:from-amber-500/5 dark:to-orange-500/5">
      <div className="flex items-center gap-2.5">
        <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-amber-500 text-white">
          <Sparkles className="h-5 w-5" />
        </div>
        <div>
          <p className="text-xs text-muted">خطتك الحالية</p>
          <p className="text-sm font-bold text-amber-700 dark:text-amber-300">مجاني (محدود)</p>
        </div>
      </div>
      <button
        onClick={() => window.dispatchEvent(new CustomEvent("navigate", { detail: "upgrade" }))}
        className="inline-flex items-center gap-1.5 rounded-xl bg-gradient-to-br from-amber-500 to-orange-600 px-4 py-2 text-xs font-bold text-white shadow-lg transition hover:brightness-110"
      >
        <Crown className="h-4 w-4" />
        ترقية الآن
      </button>
    </Card>
  );
}

/* tiny local helpers to avoid importing time utils repeatedly */
function now2() { return Date.now(); }
function monthKeyOf(ts: number) {
  const d = new Date(ts);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
}
