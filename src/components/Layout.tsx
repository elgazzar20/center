import { useState, type ReactNode } from "react";
import {
  LayoutDashboard,
  GraduationCap,
  Users,
  Boxes,
  CalendarDays,
  ClipboardCheck,
  Wallet,
  FileText,
  BarChart3,
  MessageSquare,
  Sparkles,
  UserCog,
  Settings as SettingsIcon,
  Cloud,
  CloudOff,
  RefreshCw,
  Sun,
  Moon,
  Languages,
  LogOut,
  Menu,
  X,
  Check,
  Crown,
  ArrowRight,
  Building2,
  ChevronDown,
  Lock,
  type LucideIcon,
} from "lucide-react";
import { useApp, type Permission } from "../context/AppContext";
import { cn } from "../utils/cn";
import { Avatar } from "./ui";

export interface NavItem {
  id: string;
  labelKey: string;
  icon: LucideIcon;
  section: "management" | "operations" | "insights";
  perm?: Permission;
  /** Show only for non-owner staff (secretary/admin/teacher). */
  staffOnly?: boolean;
  /** Feature gate key — hidden if subscription doesn't include it */
  featureGate?: string;
}

export const NAV: NavItem[] = [
  { id: "dashboard", labelKey: "nav.dashboard", icon: LayoutDashboard, section: "management" },
  { id: "branches", labelKey: "branch.title", icon: Building2, section: "management", perm: "settings.manage" },
  { id: "students", labelKey: "nav.students", icon: GraduationCap, section: "management", perm: "students.manage" },
  { id: "teachers", labelKey: "nav.teachers", icon: Users, section: "management", perm: "teachers.manage" },
  { id: "classes", labelKey: "nav.classes", icon: Boxes, section: "management", perm: "classes.manage", featureGate: "classes" },
  { id: "schedule", labelKey: "nav.schedule", icon: CalendarDays, section: "operations", perm: "schedule.manage" },
  { id: "attendance", labelKey: "nav.attendance", icon: ClipboardCheck, section: "operations", perm: "attendance.manage" },
  { id: "finance", labelKey: "nav.finance", icon: Wallet, section: "operations", perm: "finance.manage" },
  { id: "exams", labelKey: "nav.exams", icon: FileText, section: "operations", perm: "exams.manage" },
  { id: "reports", labelKey: "reports.title", icon: BarChart3, section: "insights", perm: "reports.view" },
  { id: "messages", labelKey: "messages.title", icon: MessageSquare, section: "insights", staffOnly: true },
  { id: "staff", labelKey: "staff.title", icon: UserCog, section: "insights", perm: "staff.manage", featureGate: "staff_management" },
  { id: "ai", labelKey: "nav.aiAssistant", icon: Sparkles, section: "insights", perm: "ai.use", featureGate: "ai_assistant" },
  { id: "settings", labelKey: "nav.settings", icon: SettingsIcon, section: "insights" },
];

const SECTION_KEYS = {
  management: "nav.groupSection",
  operations: "nav.opsSection",
  insights: "nav.intelSection",
} as const;

function BranchSwitcher() {
  const { db, t, currentBranchId, switchBranch } = useApp();
  const [open, setOpen] = useState(false);
  const current = db.branches.find((b) => b.id === currentBranchId) ?? db.branches[0];

  return (
    <div className="px-3 py-1">
      <div className="relative">
        <button
          onClick={() => setOpen((o) => !o)}
          className="flex w-full items-center gap-2.5 rounded-xl border border-line bg-elevated/50 px-3 py-2 text-start transition hover:border-brand-300"
        >
          <Building2 className="h-4 w-4 shrink-0 text-brand-500" />
          <div className="min-w-0 flex-1">
            <p className="truncate text-xs font-bold text-ink">{current?.name ?? t("branch.main")}</p>
            <p className="text-[9px] text-faint">{t("branch.title")}</p>
          </div>
          <ChevronDown className={cn("h-3.5 w-3.5 shrink-0 text-faint transition", open && "rotate-180")} />
        </button>
        {open && (
          <div className="animate-scale-in absolute z-30 mt-1 w-full overflow-hidden rounded-xl border border-line bg-surface shadow-xl">
            {db.branches.map((b) => {
              const active = b.id === currentBranchId;
              return (
                <button
                  key={b.id}
                  onClick={() => { switchBranch(b.id); setOpen(false); }}
                  className={cn("flex w-full items-center gap-2 px-3 py-2 text-start text-xs transition", active ? "bg-brand-50 text-brand-700 dark:bg-brand-500/15 dark:text-brand-200" : "hover:bg-elevated")}
                >
                  <Building2 className="h-3.5 w-3.5 shrink-0" />
                  <span className="flex-1 truncate font-medium">{b.name}</span>
                  {b.isMain && <span className="rounded bg-brand-100 px-1 py-0.5 text-[8px] font-bold text-brand-600 dark:bg-brand-500/20">{t("branch.main")}</span>}
                  {active && <Check className="h-3.5 w-3.5 shrink-0 text-brand-600" />}
                </button>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}

export function Layout({
  current,
  onNavigate,
  children,
}: {
  current: string;
  onNavigate: (id: string) => void;
  children: ReactNode;
}) {
  const { t, user, db, online, setOnline, syncStatus, pendingCount, lastSync, lang, toggleLang, theme, toggleTheme, signOut, can, canUseFeature } =
    useApp();
  const [mobileOpen, setMobileOpen] = useState(false);

  const items = NAV.filter((n) => {
    if (n.perm && !can(n.perm)) return false;
    // "staffOnly" items show for non-owner, non-parent staff only
    if (n.staffOnly) return !!user && user.role !== "OWNER" && user.role !== "PARENT";
    return true;
  });
  const sections: NavItem["section"][] = ["management", "operations", "insights"];

  const go = (id: string) => {
    onNavigate(id);
    setMobileOpen(false);
  };

  const sidebar = (
    <div className="flex h-full w-[266px] flex-col border-e border-line bg-surface/95 backdrop-blur-xl">
      {/* brand */}
      <div className="flex items-center gap-3 px-5 py-4">
        <div className="relative flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-brand-500 to-brand-700 text-sm font-bold text-white shadow-[var(--shadow-brand)] ring-1 ring-white/20">
          {db.profile.logoText || "C+"}
        </div>
        <div className="min-w-0">
          <p className="truncate text-sm font-bold tracking-tight text-ink">{t("app.name")}</p>
          <p className="truncate text-[11px] text-muted">{t("app.tagline")}</p>
        </div>
        <button
          className="ms-auto rounded-lg p-1.5 text-muted hover:bg-elevated lg:hidden"
          onClick={() => setMobileOpen(false)}
        >
          <X className="h-5 w-5" />
        </button>
      </div>

      {/* branch switcher */}
      {db.branches.length > 1 && (
        <BranchSwitcher />
      )}

      {/* nav */}
      <nav className="flex-1 space-y-5 overflow-y-auto px-3 py-2">
        {sections.map((section) => {
          const sectionItems = items.filter((i) => i.section === section);
          if (!sectionItems.length) return null;
          return (
            <div key={section}>
              <p className="px-3 pb-1.5 text-[10px] font-bold uppercase tracking-wider text-faint">
                {t(SECTION_KEYS[section])}
              </p>
              <div className="space-y-0.5">
                {sectionItems.map((item) => {
                  const active = current === item.id;
                  const Icon = item.icon;
                  const locked = item.featureGate && !canUseFeature(item.featureGate);
                  return (
                    <button
                      key={item.id}
                      onClick={() => locked ? go("upgrade") : go(item.id)}
                      className={cn(
                        "group relative flex w-full items-center gap-3 rounded-xl px-3 py-2 text-sm font-medium transition-all duration-150",
                        locked
                          ? "text-faint hover:bg-elevated/50 cursor-pointer"
                          : active
                            ? "bg-gradient-to-br from-brand-500/12 to-accent-500/10 text-brand-700 ring-1 ring-brand-500/15 dark:from-brand-500/20 dark:to-accent-500/15 dark:text-brand-200"
                            : "text-muted hover:bg-elevated hover:text-ink",
                      )}
                    >
                      {active && !locked && (
                        <span className="absolute inset-y-2 start-0 w-1 rounded-full bg-gradient-to-b from-brand-400 to-brand-600" />
                      )}
                      {locked ? (
                        <Lock className="h-[18px] w-[18px] shrink-0" />
                      ) : (
                        <Icon className={cn("h-[18px] w-[18px] transition-transform group-hover:scale-110", active && "text-brand-600 dark:text-brand-300")} />
                      )}
                      <span className="truncate">{t(item.labelKey)}</span>
                    </button>
                  );
                })}
              </div>
            </div>
          );
        })}
      </nav>

      {/* Upgrade CTA */}
      <div className="px-3 pb-2">
        <button
          onClick={() => onNavigate("upgrade")}
          className="group flex w-full items-center gap-2.5 rounded-xl border border-amber-200/60 bg-gradient-to-br from-amber-50 to-orange-50 px-3 py-2.5 text-start transition hover:shadow-md dark:border-amber-500/20 dark:from-amber-500/10 dark:to-orange-500/5"
        >
          <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-gradient-to-br from-amber-400 to-orange-500 text-white shadow-sm">
            <Crown className="h-4 w-4" />
          </div>
          <div className="min-w-0 flex-1">
            <p className="truncate text-xs font-bold text-amber-700 dark:text-amber-300">{lang === "ar" ? "ترقية للاحترافي" : "Upgrade to Pro"}</p>
            <p className="truncate text-[9px] text-amber-600/70 dark:text-amber-400/60">{lang === "ar" ? "مميزات غير محدودة" : "Unlimited features"}</p>
          </div>
          <ArrowRight className="h-3.5 w-3.5 shrink-0 text-amber-500 rtl:rotate-180" />
        </button>
      </div>

      {/* sync engine status */}
      <div className="px-3 pb-2">
        <div className="rounded-xl border border-line bg-elevated/60 p-2.5">
          <div className="flex items-center gap-2">
            <span
              className={cn(
                "h-2 w-2 rounded-full",
                online ? "bg-emerald-500 live-dot" : "bg-amber-500",
              )}
            />
            <span className="text-xs font-medium text-ink">
              {syncStatus === "syncing"
                ? t("status.syncing")
                : online
                  ? t("status.cloudConnected")
                  : t("status.localOnly")}
            </span>
            <button
              onClick={() => setOnline(!online)}
              className="ms-auto rounded-md p-1 text-muted hover:bg-surface hover:text-ink"
              title={online ? t("status.online") : t("status.offline")}
            >
              {online ? <Cloud className="h-3.5 w-3.5" /> : <CloudOff className="h-3.5 w-3.5" />}
            </button>
          </div>
          <p className="mt-1 px-0.5 text-[10px] text-faint">
            {pendingCount > 0
              ? `${pendingCount} ${t("status.queued")}`
              : `${t("status.lastSync")}: ${timeAgo(lastSync, lang)}`}
          </p>
        </div>
      </div>

      {/* user */}
      {user && (
        <div className="flex items-center gap-2.5 border-t border-line px-3 py-2.5">
          <Avatar name={user.displayName} className="h-9 w-9" />
          <div className="min-w-0 flex-1">
            <p className="truncate text-xs font-semibold text-ink">{user.displayName}</p>
            <p className="truncate text-[11px] text-muted">{t(`role.${user.role}`)}</p>
          </div>
          <button
            onClick={signOut}
            className="rounded-lg p-1.5 text-muted hover:bg-elevated hover:text-rose-600"
            title={t("action.close")}
          >
            <LogOut className="h-4 w-4" />
          </button>
        </div>
      )}
    </div>
  );

  return (
    <div className="flex h-screen overflow-hidden bg-bg">
      {/* desktop sidebar */}
      <aside className="hidden lg:block">{sidebar}</aside>

      {/* mobile drawer */}
      {mobileOpen && (
        <div className="fixed inset-0 z-40 lg:hidden">
          <div className="absolute inset-0 bg-slate-950/50 backdrop-blur-sm" onClick={() => setMobileOpen(false)} />
          <div className="animate-fade-in absolute inset-y-0 start-0">{sidebar}</div>
        </div>
      )}

      <div className="flex min-w-0 flex-1 flex-col">
        {/* topbar */}
        <header className="z-10 flex h-14 shrink-0 items-center gap-2 border-b border-line bg-surface/70 px-4 backdrop-blur-xl">
          <button
            className="rounded-lg p-2 text-muted hover:bg-elevated lg:hidden"
            onClick={() => setMobileOpen(true)}
          >
            <Menu className="h-5 w-5" />
          </button>
          <div className="flex items-center gap-1.5 text-sm text-muted">
            <span className="font-medium text-ink">{db.profile.name}</span>
          </div>

          <div className="ms-auto flex items-center gap-1">
            <button
              onClick={toggleLang}
              className="inline-flex h-9 items-center gap-1.5 rounded-lg border border-line px-2.5 text-xs font-semibold text-ink hover:bg-elevated"
              title="Language"
            >
              <Languages className="h-4 w-4" />
              {lang === "en" ? "ع" : "EN"}
            </button>
            <button
              onClick={toggleTheme}
              className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-line text-ink hover:bg-elevated"
              title="Theme"
            >
              {theme === "light" ? <Moon className="h-4 w-4" /> : <Sun className="h-4 w-4" />}
            </button>
            <button
              onClick={() => setOnline(!online)}
              className={cn(
                "inline-flex h-9 items-center gap-1.5 rounded-lg border border-line px-2.5 text-xs font-semibold hover:bg-elevated",
                online ? "text-emerald-600" : "text-amber-600",
              )}
              title="Sync"
            >
              {syncStatus === "syncing" ? (
                <RefreshCw className="h-3.5 w-3.5 animate-spin" />
              ) : online ? (
                <Check className="h-3.5 w-3.5" />
              ) : (
                <CloudOff className="h-3.5 w-3.5" />
              )}
              <span className="hidden sm:inline">{online ? t("status.online") : t("status.offline")}</span>
            </button>
          </div>
        </header>

        <main className="cp-scroll flex-1 overflow-y-auto">
          <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">{children}</div>
        </main>
      </div>
    </div>
  );
}

function timeAgo(ts: number, lang: string) {
  const s = Math.max(0, Math.floor((Date.now() - ts) / 1000));
  if (s < 5) return lang === "ar" ? "الآن" : "now";
  if (s < 60) return lang === "ar" ? `قبل ${s}ث` : `${s}s ago`;
  const m = Math.floor(s / 60);
  if (m < 60) return lang === "ar" ? `قبل ${m}د` : `${m}m ago`;
  const h = Math.floor(m / 60);
  return lang === "ar" ? `قبل ${h}س` : `${h}h ago`;
}
