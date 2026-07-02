import { useState, useEffect, useCallback, useMemo } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  Building2, GraduationCap, UserCog, CreditCard, BarChart3,
  ScrollText, Search, LogOut, ShieldCheck, Ban, CheckCircle2, Trash2,
  TrendingUp, DollarSign, AlertCircle, Loader2, RefreshCw, Flag,
  CalendarPlus, Zap, Eye, SlidersHorizontal, Send,
  ArrowLeft, Mail, Crown,
} from "lucide-react";
import {
  fetchAllCenters, fetchAuditLogs,
  updateCenterStatus, deleteCenterRecord, toggleFeatureFlag,
  activateSubscription, extendSubscription, cancelSubscription,
  fetchCenterFeatures, updateCenterLimits, sendOwnerMessage,
  applyPlanFeatures, getFeaturesForPlan, syncUsersToCenters,
  FEATURE_FLAGS, PLAN_DEFINITIONS, DEFAULT_LIMITS,
  type CenterRecord, type AuditLog,
  type AccountStatus, type SubscriptionPlan, type CenterLimits,
} from "../../lib/superadmin";
import { cn } from "../../utils/cn";

type Tab = "overview" | "centers" | "control" | "audit";

/** Gets feature label in Arabic */
function fl(f: { label: string; labelAr: string }) { return f.labelAr; }
function fd(f: { description: string; descriptionAr: string }) { return f.descriptionAr; }

const planLabels: Record<string, string> = { free: "مجاني", basic: "أساسي", pro: "احترافي", enterprise: "مؤسسي", all: "الكل" };

/* ============================== MAIN ============================== */
export function SuperAdminDashboard({
  adminUid,
  adminEmail,
  onSignOut,
}: {
  adminUid: string;
  adminEmail: string;
  onSignOut: () => void;
}) {
  const [tab, setTab] = useState<Tab>("overview");
  const [centers, setCenters] = useState<CenterRecord[]>([]);
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedCenter, setSelectedCenter] = useState<CenterRecord | null>(null);
  const [syncing, setSyncing] = useState(false);
  const [syncMsg, setSyncMsg] = useState("");

  const admin = useMemo(() => ({ uid: adminUid, email: adminEmail }), [adminUid, adminEmail]);

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      setSyncing(true);
      const result = await syncUsersToCenters(admin);
      if (result.created > 0) setSyncMsg(`تم إنشاء ${result.created} سنتر جديد`);
      setSyncing(false);
    } catch { setSyncing(false); }
    const [c, l] = await Promise.all([fetchAllCenters(), fetchAuditLogs()]);
    setCenters(c);
    setLogs(l);
    setLoading(false);
  }, [admin]);

  useEffect(() => { refresh(); }, [refresh]);

  const nav: { id: Tab; label: string; icon: typeof Building2 }[] = [
    { id: "overview", label: "نظرة عامة", icon: BarChart3 },
    { id: "centers", label: "السناتر والمستخدمون", icon: Building2 },
    { id: "control", label: "الاشتراكات والمميزات", icon: Crown },
    { id: "audit", label: "السجلات", icon: ScrollText },
  ];

  if (selectedCenter) {
    return <CenterDetailDrawer center={selectedCenter} admin={admin} onClose={() => setSelectedCenter(null)} onUpdate={refresh} />;
  }

  return (
    <div className="flex min-h-screen bg-bg">
      {/* Sidebar */}
      <aside className="hidden w-64 flex-col border-e border-line bg-surface md:flex">
        <div className="flex items-center gap-2.5 px-5 py-4">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-rose-500 to-rose-700 text-white shadow-lg shadow-rose-600/30">
            <ShieldCheck className="h-5 w-5" />
          </div>
          <div>
            <p className="text-sm font-bold text-ink">Super Admin</p>
            <p className="text-[10px] text-muted">Platform Control</p>
          </div>
        </div>
        <nav className="flex-1 space-y-1 px-3 py-2">
          {nav.map((n) => {
            const Icon = n.icon;
            const active = tab === n.id;
            return (
              <button key={n.id} onClick={() => setTab(n.id)}
                className={cn("flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-all",
                  active ? "bg-gradient-to-br from-rose-500/10 to-rose-600/5 text-rose-700 ring-1 ring-rose-500/20 dark:text-rose-200" : "text-muted hover:bg-elevated hover:text-ink")}>
                <Icon className={cn("h-[18px] w-[18px]", active && "text-rose-600 dark:text-rose-300")} />
                {n.label}
              </button>
            );
          })}
        </nav>
        <div className="border-t border-line p-3">
          <div className="mb-2 flex items-center gap-2 rounded-xl bg-elevated/60 px-3 py-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-gradient-to-br from-rose-500 to-rose-700 text-[10px] font-bold text-white">SA</div>
            <div className="min-w-0 flex-1">
              <p className="truncate text-xs font-semibold text-ink">{adminEmail}</p>
              <p className="text-[9px] text-muted">Super Admin</p>
            </div>
          </div>
          <button onClick={onSignOut} className="flex w-full items-center gap-2 rounded-xl px-3 py-2 text-xs font-medium text-rose-600 transition hover:bg-rose-50 dark:hover:bg-rose-500/10">
            <LogOut className="h-4 w-4" /> تسجيل الخروج
          </button>
        </div>
      </aside>

      {/* Content */}
      <main className="flex-1 overflow-y-auto">
        <div className="flex gap-1 overflow-x-auto border-b border-line bg-surface px-4 py-2 md:hidden">
          {nav.map((n) => (
            <button key={n.id} onClick={() => setTab(n.id)}
              className={cn("whitespace-nowrap rounded-lg px-3 py-1.5 text-xs font-semibold transition", tab === n.id ? "bg-rose-600 text-white" : "text-muted")}>
              {n.label}
            </button>
          ))}
        </div>

        <div className="mx-auto max-w-7xl p-4 sm:p-6">
          <div className="mb-6 flex items-center justify-between">
            <div>
              <h1 className="text-xl font-bold tracking-tight text-ink sm:text-2xl">{nav.find((n) => n.id === tab)?.label}</h1>
              <p className="mt-0.5 text-xs text-muted">{centers.length} سنتر مسجل · إدارة شاملة لمنصة سنتر بلس</p>
            </div>
            <div className="flex items-center gap-2">
              {syncMsg && <span className="rounded-lg bg-emerald-50 px-3 py-1.5 text-[11px] font-bold text-emerald-700 dark:bg-emerald-500/10 dark:text-emerald-300">{syncMsg}</span>}
              <button onClick={async () => { setSyncMsg(""); await refresh(); }} className="inline-flex items-center gap-1.5 rounded-xl border border-line bg-surface px-3 py-2 text-xs font-medium text-muted transition hover:text-ink">
                <RefreshCw className={cn("h-3.5 w-3.5", (loading || syncing) && "animate-spin")} />
                {syncing ? "جارٍ المزامنة..." : "مزامنة + تحديث"}
              </button>
            </div>
          </div>

          {loading ? (
            <div className="flex items-center justify-center py-20"><Loader2 className="h-8 w-8 animate-spin text-rose-500" /></div>
          ) : (
            <motion.div key={tab} initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3 }}>
              {tab === "overview" && <Overview centers={centers} />}
              {tab === "centers" && <CentersTab centers={centers} admin={admin} onUpdate={refresh} onView={setSelectedCenter} />}
              {tab === "control" && <ControlTab centers={centers} admin={admin} onUpdate={refresh} />}
              {tab === "audit" && <AuditTab logs={logs} />}
            </motion.div>
          )}
        </div>
      </main>
    </div>
  );
}

/* ============================== OVERVIEW ============================== */
function Overview({ centers }: { centers: CenterRecord[] }) {
  const totalStudents = centers.reduce((s, c) => s + (c.studentCount || 0), 0);
  const totalTeachers = centers.reduce((s, c) => s + (c.teacherCount || 0), 0);
  const activeSubs = centers.filter((c) => c.subscriptionStatus === "active").length;
  const expiredSubs = centers.filter((c) => c.subscriptionStatus === "expired" || c.subscriptionStatus === "canceled").length;
  const blocked = centers.filter((c) => c.status === "suspended" || c.status === "disabled").length;

  const monthlyRevenue = centers.filter((c) => c.subscriptionStatus === "active")
    .reduce((s, c) => { const plan = PLAN_DEFINITIONS.find((p) => p.id === c.subscriptionPlan); return s + (plan?.price ?? 0); }, 0);

  const stats = [
    { label: "إجمالي السناتر", value: centers.length, icon: Building2, tone: "from-brand-500 to-brand-600" },
    { label: "إجمالي الطلاب", value: totalStudents, icon: GraduationCap, tone: "from-emerald-500 to-green-600" },
    { label: "إجمالي المعلمين", value: totalTeachers, icon: UserCog, tone: "from-sky-500 to-blue-600" },
    { label: "الإيراد الشهري", value: `${monthlyRevenue} ج.م`, icon: DollarSign, tone: "from-amber-500 to-orange-600" },
    { label: "اشتراكات نشطة", value: activeSubs, icon: CreditCard, tone: "from-teal-500 to-cyan-600" },
    { label: "حسابات موقوفة", value: blocked, icon: Ban, tone: "from-rose-500 to-pink-600" },
    { label: "اشتراكات منتهية", value: expiredSubs, icon: AlertCircle, tone: "from-red-400 to-red-600" },
    { label: "خطط مؤسسية", value: centers.filter(c => c.subscriptionPlan === "enterprise").length, icon: Crown, tone: "from-violet-500 to-purple-600" },
  ];

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
        {stats.map((s, i) => {
          const Icon = s.icon;
          return (
            <motion.div key={i} initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: i * 0.04 }}
              className="rounded-2xl border border-line bg-surface p-4 shadow-sm">
              <div className={cn("mb-3 flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br text-white shadow-sm", s.tone)}>
                <Icon className="h-5 w-5" />
              </div>
              <p className="text-2xl font-bold text-ink">{s.value}</p>
              <p className="text-[11px] text-muted">{s.label}</p>
            </motion.div>
          );
        })}
      </div>
      <div className="grid gap-4 lg:grid-cols-3">
        <div className="rounded-2xl border border-line bg-gradient-to-br from-emerald-50 to-teal-50 p-5 dark:from-emerald-500/5 dark:to-teal-500/5 lg:col-span-2">
          <div className="mb-2 flex items-center gap-2"><TrendingUp className="h-5 w-5 text-emerald-500" /><h3 className="text-sm font-bold text-ink">الإيراد السنوي المقدر</h3></div>
          <p className="text-4xl font-extrabold text-emerald-600">{monthlyRevenue * 12} <span className="text-lg font-normal">ج.م</span></p>
          <p className="mt-1 text-xs text-muted">بناءً على {activeSubs} اشتراك نشط</p>
        </div>
        <div className="rounded-2xl border border-line bg-surface p-5">
          <div className="mb-3 flex items-center gap-2"><Building2 className="h-5 w-5 text-brand-500" /><h3 className="text-sm font-bold text-ink">حالة السناتر</h3></div>
          <div className="space-y-2">
            <div className="flex items-center justify-between"><span className="text-xs text-muted">نشط</span><span className="text-lg font-bold text-emerald-600">{centers.filter(c => c.status === "active").length}</span></div>
            <div className="flex items-center justify-between"><span className="text-xs text-muted">موقوف</span><span className="text-lg font-bold text-amber-600">{centers.filter(c => c.status === "suspended").length}</span></div>
            <div className="flex items-center justify-between"><span className="text-xs text-muted">محظور</span><span className="text-lg font-bold text-rose-600">{centers.filter(c => c.status === "disabled").length}</span></div>
          </div>
        </div>
      </div>
    </div>
  );
}

/* ============================== CENTERS + USERS COMBINED ============================== */
function CentersTab({ centers, admin, onUpdate, onView }: {
  centers: CenterRecord[];
  admin: { uid: string; email: string };
  onUpdate: () => void;
  onView: (c: CenterRecord) => void;
}) {
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [planFilter, setPlanFilter] = useState("");

  const filtered = centers.filter((c) => {
    const matchSearch = c.name?.toLowerCase().includes(search.toLowerCase()) ||
      c.ownerEmail?.toLowerCase().includes(search.toLowerCase()) ||
      c.id?.toLowerCase().includes(search.toLowerCase());
    const matchStatus = !statusFilter || c.status === statusFilter;
    const matchPlan = !planFilter || c.subscriptionPlan === planFilter;
    return matchSearch && matchStatus && matchPlan;
  });

  const act = async (c: CenterRecord, status: AccountStatus) => { await updateCenterStatus(c.id, status, admin); onUpdate(); };
  const del = async (c: CenterRecord) => { await deleteCenterRecord(c.id, admin); onUpdate(); };

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center gap-3">
        <div className="relative max-w-md flex-1">
          <Search className="pointer-events-none absolute inset-y-0 start-3 my-auto h-4 w-4 text-faint" />
          <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="ابحث بالاسم أو البريد أو ID..."
            className="h-10 w-full rounded-xl border border-line bg-surface ps-9 pe-3 text-sm text-ink placeholder:text-faint focus:border-brand-400 focus:outline-none" />
        </div>
        <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} className="h-10 rounded-xl border border-line bg-surface px-3 text-xs text-ink">
          <option value="">كل الحالات</option>
          <option value="active">نشط</option>
          <option value="suspended">موقوف</option>
          <option value="disabled">محظور</option>
        </select>
        <select value={planFilter} onChange={(e) => setPlanFilter(e.target.value)} className="h-10 rounded-xl border border-line bg-surface px-3 text-xs text-ink">
          <option value="">كل الخطط</option>
          {PLAN_DEFINITIONS.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
        </select>
      </div>

      {/* Responsive Cards Grid instead of Table */}
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {filtered.length === 0 ? (
          <div className="col-span-full rounded-2xl border border-line bg-surface py-12 text-center text-muted">لا توجد نتائج</div>
        ) : filtered.map((c) => (
          <motion.div key={c.id} initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }}
            className="flex flex-col rounded-2xl border border-line bg-surface p-4 shadow-sm">
            {/* Header */}
            <div className="flex items-start justify-between gap-2">
              <div className="flex min-w-0 items-center gap-2.5">
                <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-gradient-to-br from-brand-400 to-brand-600 text-sm font-bold text-white">{(c.name || "C")[0]}</div>
                <div className="min-w-0">
                  <p className="truncate font-medium text-ink">{c.name || "بدون اسم"}</p>
                  <p className="truncate text-[10px] text-faint">{c.ownerEmail || "—"}</p>
                </div>
              </div>
              <StatusBadge status={c.status} />
            </div>
            {/* Plan + Stats */}
            <div className="mt-3 flex items-center justify-between gap-2">
              <PlanBadge plan={c.subscriptionPlan} />
              <div className="flex gap-3 text-[11px] text-muted">
                <span className="flex items-center gap-0.5"><GraduationCap className="h-3 w-3" />{c.studentCount || 0}</span>
                <span className="flex items-center gap-0.5"><UserCog className="h-3 w-3" />{c.teacherCount || 0}</span>
              </div>
            </div>
            {/* Actions */}
            <div className="mt-3 flex items-center gap-1 border-t border-line pt-3">
              <button onClick={() => onView(c)} className="flex flex-1 items-center justify-center gap-1.5 rounded-lg bg-brand-600 py-2 text-xs font-bold text-white transition hover:bg-brand-700">
                <Eye className="h-3.5 w-3.5" /> فتح الملف
              </button>
              {c.status === "active" ? (
                <ActionBtn icon={Ban} color="amber" onClick={() => act(c, "suspended")} title="إيقاف" />
              ) : (
                <ActionBtn icon={CheckCircle2} color="emerald" onClick={() => act(c, "active")} title="تفعيل" />
              )}
              <ActionBtn icon={Trash2} color="rose" onClick={() => del(c)} title="حذف" />
            </div>
          </motion.div>
        ))}
      </div>
    </div>
  );
}

/* ============================== CENTER DETAIL DRAWER ============================== */
function CenterDetailDrawer({ center, admin, onClose, onUpdate }: {
  center: CenterRecord;
  admin: { uid: string; email: string };
  onClose: () => void;
  onUpdate: () => void;
}) {
  const [tab, setTab] = useState<"overview" | "features" | "limits" | "subscription" | "message">("overview");
  const [features, setFeatures] = useState<Record<string, boolean>>({});
  const [loadingF, setLoadingF] = useState(true);
  const [limits, setLimits] = useState<CenterLimits>(center.customLimits ?? DEFAULT_LIMITS[center.subscriptionPlan || "free"] ?? DEFAULT_LIMITS.free);
  const [msg, setMsg] = useState("");
  const [plan, setPlan] = useState<SubscriptionPlan>(center.subscriptionPlan || "free");
  const [days, setDays] = useState(30);

  useEffect(() => {
    setLoadingF(true);
    fetchCenterFeatures(center.id).then((f) => { setFeatures(f); setLoadingF(false); });
  }, [center.id]);

  const toggleF = async (key: string) => {
    const newVal = !features[key];
    setFeatures((p) => ({ ...p, [key]: newVal }));
    await toggleFeatureFlag(center.id, key, newVal, admin);
  };

  const saveLimits = async () => { await updateCenterLimits(center.id, limits, admin); onUpdate(); };

  const sendMessage = async () => {
    if (!msg.trim()) return;
    await sendOwnerMessage(center.id, center.ownerId, msg.trim(), admin);
    setMsg(""); alert("تم إرسال الرسالة");
  };

  const [actionMsg, setActionMsg] = useState("");

  const doActivate = async () => {
    setActionMsg("جارٍ التفعيل...");
    try {
      await activateSubscription(center.id, plan, days, admin);
      setActionMsg(`تم تفعيل الخطة ${planLabels[plan]} لمدة ${days} يوم بنجاح`);
      onUpdate();
    } catch (e) {
      setActionMsg("فشل التفعيل: " + (e instanceof Error ? e.message : String(e)));
    }
  };
  const doExtend = async () => {
    setActionMsg("جارٍ التمديد...");
    try {
      await extendSubscription(center.id, days, admin);
      setActionMsg(`تم تمديد الاشتراك ${days} يوم`);
      onUpdate();
    } catch (e) {
      setActionMsg("فشل التمديد");
    }
  };
  const doCancel = async () => {
    setActionMsg("جارٍ الإلغاء...");
    try {
      await cancelSubscription(center.id, admin);
      setActionMsg("تم إلغاء الاشتراك");
      onUpdate();
    } catch (e) {
      setActionMsg("فشل الإلغاء");
    }
  };
  const doApplyPlan = async (p: SubscriptionPlan) => {
    setLoadingF(true);
    await applyPlanFeatures(center.id, p, admin);
    setFeatures(getFeaturesForPlan(p));
    setLoadingF(false);
  };

  const tabs = [
    { id: "overview" as const, label: "معلومات", icon: Eye },
    { id: "features" as const, label: "المميزات", icon: Flag },
    { id: "limits" as const, label: "الحدود", icon: SlidersHorizontal },
    { id: "subscription" as const, label: "الاشتراك", icon: CreditCard },
    { id: "message" as const, label: "رسالة", icon: Mail },
  ];

  const durations = [
    { l: "3 أيام", v: 3 }, { l: "أسبوع", v: 7 }, { l: "أسبوعين", v: 14 },
    { l: "شهر", v: 30 }, { l: "شهرين", v: 60 }, { l: "3 أشهر", v: 90 },
    { l: "5 أشهر", v: 150 }, { l: "6 أشهر", v: 180 }, { l: "سنة", v: 365 },
  ];

  return (
    <div className="min-h-screen bg-bg">
      <div className="sticky top-0 z-30 border-b border-line bg-surface/90 backdrop-blur-xl">
        <div className="mx-auto flex h-16 max-w-5xl items-center gap-3 px-4">
          <button onClick={onClose} className="inline-flex items-center gap-1.5 text-sm font-medium text-muted hover:text-ink">
            <ArrowLeft className="h-4 w-4 rtl:rotate-180" /> رجوع
          </button>
          <div className="ms-auto flex items-center gap-2">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-br from-brand-500 to-brand-700 text-sm font-bold text-white shadow-lg">{(center.name || "C")[0]}</div>
            <div><p className="text-sm font-bold text-ink">{center.name || "بدون اسم"}</p><p className="text-[10px] text-muted">{center.ownerEmail}</p></div>
          </div>
        </div>
      </div>

      <div className="mx-auto max-w-5xl p-4 sm:p-6">
        <div className="mb-6 flex gap-1 overflow-x-auto rounded-xl border border-line bg-surface p-1">
          {tabs.map((t) => (
            <button key={t.id} onClick={() => setTab(t.id)}
              className={cn("inline-flex items-center gap-1.5 whitespace-nowrap rounded-lg px-4 py-2 text-xs font-semibold transition",
                tab === t.id ? "bg-brand-600 text-white shadow-sm" : "text-muted hover:text-ink")}>
              <t.icon className="h-3.5 w-3.5" /> {t.label}
            </button>
          ))}
        </div>

        <AnimatePresence mode="wait">
          <motion.div key={tab} initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }} transition={{ duration: 0.2 }}>

            {tab === "overview" && (
              <div className="grid grid-cols-2 gap-4 sm:grid-cols-3">
                <StatCard label="الطلاب" value={center.studentCount || 0} icon={GraduationCap} tone="from-emerald-500 to-green-600" />
                <StatCard label="المعلمون" value={center.teacherCount || 0} icon={UserCog} tone="from-sky-500 to-blue-600" />
                <StatCard label="الحالة" value={center.status} icon={ShieldCheck} tone="from-violet-500 to-purple-600" />
                <StatCard label="الخطة" value={planLabels[center.subscriptionPlan || "free"]} icon={CreditCard} tone="from-amber-500 to-orange-600" />
                <StatCard label="حالة الاشتراك" value={center.subscriptionStatus || "none"} icon={CheckCircle2} tone="from-teal-500 to-cyan-600" />
                <StatCard label="تاريخ الانتهاء" value={center.subscriptionEndDate ? new Date(center.subscriptionEndDate).toLocaleDateString() : "—"} icon={CalendarPlus} tone="from-rose-500 to-pink-600" />
              </div>
            )}

            {tab === "features" && (
              loadingF ? <div className="flex justify-center py-10"><Loader2 className="h-6 w-6 animate-spin text-brand-500" /></div> : (
                <div className="space-y-3">
                  <div className="flex flex-wrap items-center gap-2 rounded-xl bg-elevated/50 p-3">
                    <span className="text-xs text-muted">تطبيق خطة بسرعة:</span>
                    {(["free", "pro", "enterprise"] as const).map((p) => (
                      <button key={p} onClick={() => doApplyPlan(p)} className="rounded-lg border border-brand-200 bg-brand-50 px-3 py-1.5 text-[11px] font-bold text-brand-700 transition hover:bg-brand-100 dark:border-brand-500/30 dark:bg-brand-500/10 dark:text-brand-200">
                        {planLabels[p]}
                      </button>
                    ))}
                  </div>
                  <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                    {FEATURE_FLAGS.map((f) => {
                      const enabled = features[f.key] ?? false;
                      return (
                        <div key={f.key} className="flex items-center gap-3 rounded-xl border border-line bg-surface p-3">
                          <div className={cn("flex h-10 w-10 shrink-0 items-center justify-center rounded-xl", enabled ? "bg-emerald-50 text-emerald-600 dark:bg-emerald-500/15" : "bg-elevated text-faint")}>
                            <Zap className="h-5 w-5" />
                          </div>
                          <div className="min-w-0 flex-1">
                            <p className="text-sm font-semibold text-ink">{fl(f)}</p>
                            <p className="text-[11px] text-muted">{fd(f)}</p>
                            <span className="mt-0.5 inline-block rounded bg-elevated px-1.5 py-0.5 text-[9px] font-bold">{planLabels[f.plan] || f.plan}</span>
                          </div>
                          <button onClick={() => toggleF(f.key)} className={cn("relative h-6 w-11 rounded-full transition", enabled ? "bg-emerald-500" : "bg-line")}>
                            <span className={cn("absolute top-0.5 h-5 w-5 rounded-full bg-white shadow transition-all", enabled ? "start-[1.4rem]" : "start-0.5")} />
                          </button>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )
            )}

            {tab === "limits" && (
              <div className="rounded-2xl border border-line bg-surface p-5">
                <div className="mb-4 flex items-center gap-2"><SlidersHorizontal className="h-4 w-4 text-brand-500" /><h3 className="text-sm font-bold text-ink">الحدود المخصصة للسنتر</h3></div>
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                  {([
                    { key: "maxStudents" as const, label: "أقصى عدد طلاب" },
                    { key: "maxTeachers" as const, label: "أقصى عدد معلمين" },
                    { key: "maxStaff" as const, label: "أقصى عدد موظفين" },
                    { key: "maxGroups" as const, label: "أقصى عدد مجموعات" },
                    { key: "maxClassrooms" as const, label: "أقصى عدد قاعات" },
                    { key: "maxSchedules" as const, label: "أقصى عدد حصص" },
                  ]).map((item) => (
                    <div key={item.key}>
                      <label className="mb-1.5 block text-xs font-medium text-muted">{item.label}</label>
                      <input type="number" value={limits[item.key] ?? 0} onChange={(e) => setLimits((p) => ({ ...p, [item.key]: +e.target.value }))}
                        className="h-10 w-full rounded-lg border border-line bg-surface px-3 text-sm text-ink focus:border-brand-400 focus:outline-none focus:ring-2 focus:ring-brand-500/20" />
                    </div>
                  ))}
                </div>
                <button onClick={saveLimits} className="mt-5 inline-flex items-center gap-2 rounded-xl bg-brand-600 px-5 py-2.5 text-sm font-bold text-white shadow-lg transition hover:bg-brand-700">
                  <CheckCircle2 className="h-4 w-4" /> حفظ الحدود
                </button>
              </div>
            )}

            {tab === "subscription" && (
              <div className="space-y-4">
                {actionMsg && (
                  <div className={cn("rounded-xl px-4 py-3 text-sm font-medium", actionMsg.includes("بنجاح") || actionMsg.includes("تم") ? "bg-emerald-50 text-emerald-700 dark:bg-emerald-500/10 dark:text-emerald-300" : actionMsg.includes("فشل") ? "bg-rose-50 text-rose-700 dark:bg-rose-500/10 dark:text-rose-300" : "bg-brand-50 text-brand-700 dark:bg-brand-500/10 dark:text-brand-300")}>
                    {actionMsg}
                  </div>
                )}
                <div className="rounded-2xl border border-line bg-surface p-5">
                  <div className="grid gap-4 sm:grid-cols-2">
                    <div>
                      <p className="mb-2 text-xs font-semibold text-muted">الخطة</p>
                      <select value={plan} onChange={(e) => setPlan(e.target.value as SubscriptionPlan)} className="h-10 w-full rounded-lg border border-line bg-surface px-3 text-sm text-ink">
                        {PLAN_DEFINITIONS.map((p) => <option key={p.id} value={p.id}>{p.name} — {p.price} ج.م/شهر</option>)}
                      </select>
                    </div>
                    <div>
                      <p className="mb-2 text-xs font-semibold text-muted">المدة</p>
                      <div className="flex flex-wrap gap-2">
                        {durations.map((d) => (
                          <button key={d.v} onClick={() => setDays(d.v)} className={cn("rounded-lg border px-3 py-2 text-xs font-semibold transition", days === d.v ? "border-brand-400 bg-brand-50 text-brand-700 dark:bg-brand-500/15 dark:text-brand-200" : "border-line text-muted hover:bg-elevated")}>{d.l}</button>
                        ))}
                      </div>
                    </div>
                  </div>
                  {/* Calculated end date */}
                  <div className="mt-4 rounded-xl border border-line bg-elevated/50 p-3 text-center">
                    <p className="text-xs text-muted">تاريخ الانتهاء المتوقع</p>
                    <p className="text-lg font-bold text-ink">{new Date(Date.now() + days * 86400000).toLocaleDateString("ar-EG", { year: "numeric", month: "long", day: "numeric" })}</p>
                  </div>
                  <div className="mt-5 flex flex-wrap gap-2">
                    <button onClick={doActivate} className="inline-flex items-center gap-2 rounded-xl bg-emerald-600 px-5 py-2.5 text-sm font-bold text-white shadow-lg transition hover:bg-emerald-700"><CheckCircle2 className="h-4 w-4" /> تفعيل الاشتراك</button>
                    <button onClick={doExtend} className="inline-flex items-center gap-2 rounded-xl bg-brand-600 px-5 py-2.5 text-sm font-bold text-white shadow-lg transition hover:bg-brand-700"><CalendarPlus className="h-4 w-4" /> تمديد</button>
                    <button onClick={doCancel} className="inline-flex items-center gap-2 rounded-xl border border-rose-300 bg-rose-50 px-5 py-2.5 text-sm font-bold text-rose-600 transition hover:bg-rose-100 dark:border-rose-500/30 dark:bg-rose-500/10"><Ban className="h-4 w-4" /> إلغاء</button>
                  </div>
                </div>
                <div className="rounded-2xl border border-line bg-surface p-5">
                  <p className="mb-2 text-xs font-semibold text-muted">عرض الخطط المتاحة:</p>
                  <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
                    {PLAN_DEFINITIONS.map((p) => (
                      <div key={p.id} className={cn("rounded-xl border p-3", plan === p.id ? "border-brand-400 bg-brand-50/50 dark:bg-brand-500/5" : "border-line")}>
                        <p className="text-sm font-bold text-ink">{p.name}</p>
                        <p className="text-lg font-extrabold text-brand-600">{p.price} <span className="text-xs font-normal text-muted">ج.م/شهر</span></p>
                        <p className="text-[10px] text-muted">{p.maxStudents === 99999 ? "طلاب غير محدود" : `حتى ${p.maxStudents} طالب`}</p>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            )}

            {tab === "message" && (
              <div className="rounded-2xl border border-line bg-surface p-5">
                <div className="mb-3 flex items-center gap-2"><Mail className="h-4 w-4 text-brand-500" /><h3 className="text-sm font-bold text-ink">إرسال رسالة لمالك السنتر</h3></div>
                <textarea rows={4} value={msg} onChange={(e) => setMsg(e.target.value)} placeholder="اكتب رسالتك هنا..."
                  className="w-full rounded-xl border border-line bg-surface p-3 text-sm text-ink placeholder:text-faint focus:border-brand-400 focus:outline-none focus:ring-2 focus:ring-brand-500/20" />
                <button onClick={sendMessage} disabled={!msg.trim()} className="mt-3 inline-flex items-center gap-2 rounded-xl bg-brand-600 px-5 py-2.5 text-sm font-bold text-white shadow-lg transition hover:bg-brand-700 disabled:opacity-50">
                  <Send className="h-4 w-4" /> إرسال
                </button>
              </div>
            )}

          </motion.div>
        </AnimatePresence>
      </div>
    </div>
  );
}

/* ============================== CONTROL TAB (Combined Features + Subscriptions) ============================== */
function ControlTab({ centers, admin, onUpdate }: { centers: CenterRecord[]; admin: { uid: string; email: string }; onUpdate: () => void }) {
  const [search, setSearch] = useState("");

  const filtered = centers.filter((c) => c.name?.toLowerCase().includes(search.toLowerCase()) || c.ownerEmail?.toLowerCase().includes(search.toLowerCase()));

  const changePlan = async (c: CenterRecord, plan: SubscriptionPlan) => {
    await activateSubscription(c.id, plan, 365, admin);
    onUpdate();
  };

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="relative max-w-md flex-1">
          <Search className="pointer-events-none absolute inset-y-0 start-3 my-auto h-4 w-4 text-faint" />
          <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="ابحث عن سنتر..."
            className="h-10 w-full rounded-xl border border-line bg-surface ps-9 pe-3 text-sm text-ink placeholder:text-faint focus:border-brand-400 focus:outline-none" />
        </div>
        <div className="flex items-center gap-2">
          {PLAN_DEFINITIONS.map((p) => (
            <div key={p.id} className={cn("rounded-lg px-3 py-1.5 text-[10px] font-bold", p.color)}>
              {p.name} · {p.price} ج.م
            </div>
          ))}
        </div>
      </div>

      <div className="overflow-hidden rounded-2xl border border-line bg-surface">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead><tr className="border-b border-line text-[11px] uppercase text-faint">
              <th className="px-4 py-3 text-start font-semibold">السنتر</th>
              <th className="px-4 py-3 text-center font-semibold">الخطة الحالية</th>
              <th className="px-4 py-3 text-center font-semibold">حالة الاشتراك</th>
              <th className="px-4 py-3 text-center font-semibold">تغيير الخطة</th>
              <th className="px-4 py-3 text-center font-semibold">المميزات</th>
              <th className="px-4 py-3 text-center font-semibold">الانتهاء</th>
            </tr></thead>
            <tbody>
              {filtered.map((c) => (
                <tr key={c.id} className="border-b border-line/50 last:border-0 hover:bg-elevated/40">
                  <td className="px-4 py-3"><p className="font-medium text-ink">{c.name || "بدون اسم"}</p><p className="text-[10px] text-faint">{c.ownerEmail}</p></td>
                  <td className="px-4 py-3 text-center"><PlanBadge plan={c.subscriptionPlan} /></td>
                  <td className="px-4 py-3 text-center">
                    <span className={cn("rounded-full px-2 py-0.5 text-[10px] font-bold",
                      c.subscriptionStatus === "active" ? "bg-emerald-50 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-300" :
                      c.subscriptionStatus === "canceled" ? "bg-rose-50 text-rose-700 dark:bg-rose-500/15 dark:text-rose-300" :
                      "bg-elevated text-muted")}>
                      {c.subscriptionStatus === "active" ? "نشط" : c.subscriptionStatus === "trialing" ? "تجريبي" : c.subscriptionStatus === "canceled" ? "ملغي" : "لا يوجد"}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-center">
                    <select value={c.subscriptionPlan || "free"} onChange={(e) => changePlan(c, e.target.value as SubscriptionPlan)}
                      className="h-8 rounded-lg border border-line bg-surface px-2 text-xs text-ink">
                      {PLAN_DEFINITIONS.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
                    </select>
                  </td>
                  <td className="px-4 py-3 text-center">
                    <button onClick={async () => { await applyPlanFeatures(c.id, c.subscriptionPlan || "free", admin); onUpdate(); }}
                      className="inline-flex items-center gap-1 rounded-lg border border-brand-200 bg-brand-50 px-2 py-1 text-[10px] font-bold text-brand-700 transition hover:bg-brand-100 dark:border-brand-500/30 dark:bg-brand-500/10 dark:text-brand-200">
                      <Flag className="h-3 w-3" /> تطبيق مميزات الخطة
                    </button>
                  </td>
                  <td className="px-4 py-3 text-center text-xs text-muted">{c.subscriptionEndDate ? new Date(c.subscriptionEndDate).toLocaleDateString() : "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

/* ============================== AUDIT LOGS ============================== */
function AuditTab({ logs }: { logs: AuditLog[] }) {
  return (
    <div className="space-y-2">
      {logs.length === 0 ? (
        <div className="rounded-2xl border border-line bg-surface p-8 text-center">
          <ScrollText className="mx-auto mb-3 h-10 w-10 text-faint" />
          <p className="text-sm text-muted">لا توجد سجلات بعد</p>
        </div>
      ) : logs.map((l) => (
        <div key={l.id} className="flex items-center gap-3 rounded-xl border border-line bg-surface p-3">
          <div className={cn("flex h-8 w-8 shrink-0 items-center justify-center rounded-lg",
            l.action.includes("delete") || l.action.includes("disable") ? "bg-rose-50 text-rose-600 dark:bg-rose-500/15" :
            l.action.includes("suspend") || l.action.includes("cancel") ? "bg-amber-50 text-amber-600 dark:bg-amber-500/15" :
            l.action.includes("enable") || l.action.includes("activate") ? "bg-emerald-50 text-emerald-600 dark:bg-emerald-500/15" :
            "bg-brand-50 text-brand-600 dark:bg-brand-500/15")}>
            {l.action.includes("delete") ? <Trash2 className="h-4 w-4" /> : l.action.includes("suspend") || l.action.includes("disable") ? <Ban className="h-4 w-4" /> : l.action.includes("enable") || l.action.includes("activate") ? <CheckCircle2 className="h-4 w-4" /> : <CreditCard className="h-4 w-4" />}
          </div>
          <div className="min-w-0 flex-1">
            <p className="text-xs font-medium text-ink">{l.action} ← {l.targetType}:{l.targetName || l.targetId}</p>
            <p className="text-[10px] text-faint">بواسطة {l.adminEmail} · {new Date(l.timestamp).toLocaleString()}</p>
          </div>
          {l.newValue && <span className="rounded bg-elevated px-2 py-0.5 text-[9px] font-mono text-muted">{l.newValue}</span>}
        </div>
      ))}
    </div>
  );
}

/* ============================== SHARED ============================== */
function StatCard({ label, value, icon: Icon, tone }: { label: string; value: React.ReactNode; icon: typeof Eye; tone: string }) {
  return (
    <div className="rounded-2xl border border-line bg-surface p-4 shadow-sm">
      <div className={cn("mb-3 flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br text-white shadow-sm", tone)}>
        <Icon className="h-5 w-5" />
      </div>
      <p className="text-lg font-bold text-ink capitalize">{value}</p>
      <p className="text-[11px] text-muted">{label}</p>
    </div>
  );
}

function PlanBadge({ plan }: { plan: SubscriptionPlan | undefined }) {
  const def = PLAN_DEFINITIONS.find((p) => p.id === plan);
  if (!def) return <span className="rounded-full bg-elevated px-2 py-0.5 text-[10px] font-bold">مجاني</span>;
  return <span className={cn("rounded-full px-2 py-0.5 text-[10px] font-bold", def.color)}>{def.name}</span>;
}

function StatusBadge({ status }: { status: AccountStatus }) {
  const tone = status === "active" ? "bg-emerald-50 text-emerald-700 dark:bg-emerald-500/15 dark:text-emerald-300"
    : status === "suspended" ? "bg-amber-50 text-amber-700 dark:bg-amber-500/15 dark:text-amber-300"
    : "bg-rose-50 text-rose-700 dark:bg-rose-500/15 dark:text-rose-300";
  const label = status === "active" ? "نشط" : status === "suspended" ? "موقوف" : "محظور";
  return <span className={cn("rounded-full px-2 py-0.5 text-[10px] font-bold", tone)}>{label}</span>;
}

function ActionBtn({ icon: Icon, color, onClick, title }: { icon: typeof Ban; color: "amber" | "emerald" | "rose" | "brand"; onClick: () => void; title: string }) {
  const colors = {
    amber: "text-amber-600 hover:bg-amber-50 dark:hover:bg-amber-500/10",
    emerald: "text-emerald-600 hover:bg-emerald-50 dark:hover:bg-emerald-500/10",
    rose: "text-rose-600 hover:bg-rose-50 dark:hover:bg-rose-500/10",
    brand: "text-brand-600 hover:bg-brand-50 dark:hover:bg-brand-500/10",
  };
  return <button onClick={onClick} title={title} className={cn("rounded-lg p-1.5 transition", colors[color])}><Icon className="h-4 w-4" /></button>;
}
