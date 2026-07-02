import { useState } from "react";
import {
  Building2, Palette, Database, Save, RotateCcw, Cloud, CloudOff,
  Shield, Sun, Moon, Check, Archive, Download, Upload,
  GraduationCap, AlertTriangle, Crown,
} from "lucide-react";
import { useApp } from "../context/AppContext";
import { CURRENCIES } from "../lib/constants";
import {
  PageHeader, Button, Card, Input, Select, Field, Badge, Modal, pushToast,
} from "../components/ui";
import { cn } from "../utils/cn";

export function Settings() {
  const {
    db, t, user, lang, updateProfile, resetData, online, setOnline, syncLog,
    setLang, theme, toggleTheme, fontScale, setFontScale, can,
    backups, createBackup, restoreFromBackup, removeBackup, exportBackup, restoreBackupFromFile, promoteYear,
  } = useApp();
  const [name, setName] = useState(db.profile.name);
  const [currency, setCurrency] = useState(db.profile.currency);
  const [logo, setLogo] = useState(db.profile.logoText ?? "");
  const [saved, setSaved] = useState(false);

  // year promotion
  const [yearOpen, setYearOpen] = useState(false);
  const [countdown, setCountdown] = useState(0);

  const saveProfile = () => {
    updateProfile({ name, currency, logoText: logo.slice(0, 2).toUpperCase() || "C" });
    setSaved(true);
    setTimeout(() => setSaved(false), 1800);
  };

  const startPromote = () => {
    setCountdown(10);
    setYearOpen(true);
    const tick = (n: number) => {
      if (n <= 0) {
        const { promoted, skipped } = promoteYear();
        setYearOpen(false);
        pushToast(t("year.promoted", { p: promoted, s: skipped }));
        return;
      }
      setCountdown(n);
      setTimeout(() => tick(n - 1), 1000);
    };
    createBackup(t("backup.auto"));
    setTimeout(() => tick(10), 100);
  };

  return (
    <div className="animate-fade-in space-y-5">
      <PageHeader title={t("settings.title")} />

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        {/* profile */}
        <Card className="p-5">
          <div className="mb-4 flex items-center gap-2">
            <Building2 className="h-4 w-4 text-brand-600" />
            <h3 className="text-[15px] font-semibold tracking-tight text-ink">{t("settings.profile")}</h3>
          </div>
          <div className="space-y-3">
            <Field label={t("settings.centerName")}><Input value={name} onChange={(e) => setName(e.target.value)} /></Field>
            <div className="grid grid-cols-2 gap-3">
              <Field label={t("settings.currency")}>
                <Select value={currency} onChange={(e) => setCurrency(e.target.value)}>
                  {CURRENCIES.map((c) => <option key={c.code} value={c.code}>{lang === "ar" ? c.ar : c.en} ({c.symbol})</option>)}
                </Select>
              </Field>
              <Field label="Logo"><Input value={logo} maxLength={2} onChange={(e) => setLogo(e.target.value)} /></Field>
            </div>
            <Button onClick={saveProfile} variant={saved ? "subtle" : "primary"}>
              {saved ? <Check className="h-4 w-4" /> : <Save className="h-4 w-4" />}
              {saved ? t("settings.saved") : t("action.save")}
            </Button>
          </div>
        </Card>

        {/* appearance */}
        <Card className="p-5">
          <div className="mb-4 flex items-center gap-2">
            <Palette className="h-4 w-4 text-violet-600" />
            <h3 className="text-[15px] font-semibold tracking-tight text-ink">{t("settings.appearance")}</h3>
          </div>
          <div className="space-y-4">
            <div>
              <p className="mb-2 text-xs font-medium text-muted">{t("settings.language")}</p>
              <div className="grid grid-cols-2 gap-2">
                {(["en", "ar"] as const).map((l) => (
                  <button key={l} onClick={() => setLang(l)}
                    className={cn("rounded-lg border px-3 py-2 text-sm font-medium transition", lang === l ? "border-brand-300 bg-brand-50 text-brand-700 dark:border-brand-500/40 dark:bg-brand-500/15 dark:text-brand-200" : "border-line text-muted hover:bg-elevated")}>
                    {l === "en" ? "English" : "العربية"}
                  </button>
                ))}
              </div>
            </div>
            <div>
              <p className="mb-2 text-xs font-medium text-muted">{t("settings.theme")}</p>
              <div className="grid grid-cols-2 gap-2">
                <button onClick={() => theme !== "light" && toggleTheme()} className={cn("flex items-center justify-center gap-2 rounded-lg border px-3 py-2 text-sm font-medium", theme === "light" ? "border-brand-300 bg-brand-50 text-brand-700 dark:border-brand-500/40 dark:bg-brand-500/15 dark:text-brand-200" : "border-line text-muted hover:bg-elevated")}>
                  <Sun className="h-4 w-4" /> {t("settings.light")}
                </button>
                <button onClick={() => theme !== "dark" && toggleTheme()} className={cn("flex items-center justify-center gap-2 rounded-lg border px-3 py-2 text-sm font-medium", theme === "dark" ? "border-brand-300 bg-brand-50 text-brand-700 dark:border-brand-500/40 dark:bg-brand-500/15 dark:text-brand-200" : "border-line text-muted hover:bg-elevated")}>
                  <Moon className="h-4 w-4" /> {t("settings.dark")}
                </button>
              </div>
            </div>
            {/* font size */}
            <div>
              <p className="mb-1 text-xs font-medium text-muted">{t("settings.fontSize")}</p>
              <p className="mb-2 text-[10px] text-faint">{t("settings.fontSizeHint")}</p>
              <div className="grid grid-cols-3 gap-2">
                {([
                  { v: "small" as const, label: t("settings.fontSizeSmall"), size: "text-xs" },
                  { v: "medium" as const, label: t("settings.fontSizeMedium"), size: "text-sm" },
                  { v: "large" as const, label: t("settings.fontSizeLarge"), size: "text-base" },
                ]).map((opt) => (
                  <button key={opt.v} onClick={() => setFontScale(opt.v)}
                    className={cn("flex flex-col items-center gap-1 rounded-lg border px-2 py-2.5 transition", fontScale === opt.v ? "border-brand-300 bg-brand-50 dark:border-brand-500/40 dark:bg-brand-500/15" : "border-line text-muted hover:bg-elevated")}>
                    <span className={cn("font-semibold", opt.size)}>Aa</span>
                    <span className="text-[10px]">{opt.label}</span>
                  </button>
                ))}
              </div>
            </div>
            {user && (
              <div className="flex items-center gap-2 rounded-lg bg-elevated/60 px-3 py-2 text-xs">
                <Shield className="h-4 w-4 text-brand-600" />
                <span className="text-muted">{t("settings.role")}:</span>
                <Badge tone="brand">{t(`role.${user.role}`)}</Badge>
              </div>
            )}
          </div>
        </Card>
      </div>

      {/* Subscription / Upgrade */}
      <Card className="relative overflow-hidden border-brand-200/60 p-5 dark:border-brand-500/20">
        <div className="mesh-brand pointer-events-none absolute inset-0 opacity-[0.03]" />
        <div className="relative">
          <div className="mb-3 flex items-center gap-2">
            <Crown className="h-4 w-4 text-amber-500" />
            <h3 className="text-[15px] font-semibold tracking-tight text-ink">{lang === "ar" ? "الاشتراك" : "Subscription"}</h3>
          </div>
          <div className="flex items-center justify-between rounded-xl bg-elevated/60 px-4 py-3">
            <div>
              <p className="text-xs text-muted">{lang === "ar" ? "خطتك الحالية" : "Current Plan"}</p>
              <p className="text-lg font-bold text-ink">{lang === "ar" ? "مجاني" : "Free"}</p>
            </div>
            <div className="text-end">
              <p className="text-[11px] text-muted">{lang === "ar" ? "الحد الأقصى" : "Limits"}</p>
              <p className="text-sm font-semibold text-ink">{lang === "ar" ? "30 طالب · 2 معلم" : "30 students · 2 teachers"}</p>
            </div>
          </div>
          <Button
            className="mt-3 w-full bg-gradient-to-br from-brand-500 to-brand-700"
            onClick={() => window.dispatchEvent(new CustomEvent("navigate", { detail: "upgrade" }))}
          >
            <Crown className="h-4 w-4" />
            {lang === "ar" ? "ترقية الآن" : "Upgrade Now"}
          </Button>
        </div>
      </Card>

      {/* data engine */}
      <Card className="p-5">
        <div className="mb-4 flex items-center gap-2">
          <Database className="h-4 w-4 text-emerald-600" />
          <h3 className="text-[15px] font-semibold tracking-tight text-ink">{t("settings.dataEngine")}</h3>
        </div>
        <div className="grid grid-cols-1 gap-3 md:grid-cols-3">
          <div className="space-y-1 rounded-lg bg-elevated/60 p-3">
            <p className="text-[11px] text-faint">{t("settings.tenantPath")}</p>
            <p className="font-mono text-xs text-ink">/centers/{db.profile.centerId}</p>
          </div>
          <div className="space-y-1 rounded-lg bg-elevated/60 p-3">
            <p className="text-[11px] text-faint">Conflict rule</p>
            <p className="text-xs text-ink">lastUpdated (latest wins)</p>
          </div>
          <div className="flex items-center gap-3 rounded-lg bg-elevated/60 p-3">
            <button onClick={() => setOnline(!online)} className={cn("flex h-9 w-9 items-center justify-center rounded-lg", online ? "bg-emerald-100 text-emerald-600 dark:bg-emerald-500/15" : "bg-amber-100 text-amber-600 dark:bg-amber-500/15")}>
              {online ? <Cloud className="h-4 w-4" /> : <CloudOff className="h-4 w-4" />}
            </button>
            <div>
              <p className="text-xs font-medium text-ink">{online ? t("status.online") : t("status.offline")}</p>
              <p className="text-[10px] text-faint">SQLite + Firestore</p>
            </div>
          </div>
        </div>
        <div className="mt-4">
          <p className="mb-2 text-xs font-medium text-muted">Sync queue</p>
          <div className="max-h-36 space-y-1 overflow-y-auto rounded-lg border border-line p-2">
            {syncLog.length === 0 ? (
              <p className="py-3 text-center text-[11px] text-faint">{t("status.synced")}</p>
            ) : syncLog.slice(0, 12).map((e) => (
              <div key={e.id} className="flex items-center gap-2 text-[11px]">
                <span className={cn("h-1.5 w-1.5 rounded-full", e.status === "pushed" ? "bg-emerald-500" : "bg-amber-500")} />
                <span className="font-mono text-faint">{e.path}</span>
                <Badge tone="neutral" className="ms-auto">{e.op}</Badge>
              </div>
            ))}
          </div>
        </div>
        <div className="mt-4 flex justify-end">
          <Button variant="danger" onClick={resetData}><RotateCcw className="h-4 w-4" />{t("settings.resetData")}</Button>
        </div>
      </Card>

      {/* backup & restore */}
      <Card className="p-5">
        <div className="mb-4 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Archive className="h-4 w-4 text-amber-500" />
            <div>
              <h3 className="text-[15px] font-semibold tracking-tight text-ink">{t("backup.title")}</h3>
              <p className="text-xs text-muted">{t("backup.subtitle")}</p>
            </div>
          </div>
          <div className="flex flex-wrap gap-2">
            <Button size="sm" variant="secondary" onClick={() => { createBackup("manual"); pushToast(t("toast.backup")); }}><Archive className="h-4 w-4" />{t("backup.create")}</Button>
            <Button size="sm" variant="secondary" onClick={exportBackup}><Download className="h-4 w-4" />{t("backup.export")}</Button>
            <label className="inline-flex h-8 cursor-pointer items-center gap-1.5 rounded-lg border border-line bg-elevated px-3 text-xs font-medium text-ink transition hover:bg-line">
              <Upload className="h-4 w-4" />{t("backup.import")}
              <input type="file" accept="application/json,.json" className="hidden" onChange={async (e) => {
                const file = e.target.files?.[0];
                if (!file) return;
                const ok = await restoreBackupFromFile(file);
                pushToast(ok ? t("toast.restored") : t("backup.invalid"), ok ? "success" : "error");
                e.target.value = "";
              }} />
            </label>
          </div>
        </div>
        {backups.length === 0 ? (
          <p className="py-4 text-center text-xs text-muted">{t("backup.empty")}</p>
        ) : (
          <div className="space-y-2">
            {backups.map((b) => (
              <div key={b.ts} className="flex items-center gap-2 rounded-lg border border-line p-2.5">
                <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-amber-50 text-amber-600 dark:bg-amber-500/15"><Archive className="h-4 w-4" /></div>
                <div className="min-w-0 flex-1">
                  <p className="text-xs font-medium text-ink">{b.label}</p>
                  <p className="text-[10px] text-faint">{new Date(b.ts).toLocaleString()}</p>
                </div>
                <Button size="sm" variant="subtle" onClick={() => { restoreFromBackup(b.ts); pushToast(t("toast.restored")); }}><Upload className="h-3.5 w-3.5" />{t("backup.restore")}</Button>
                <Button size="sm" variant="ghost" onClick={() => removeBackup(b.ts)}><RotateCcw className="h-3.5 w-3.5 text-rose-500" /></Button>
              </div>
            ))}
          </div>
        )}
      </Card>

      {/* academic year promotion */}
      {can("settings.manage") && (
        <Card className="border-amber-200/60 p-5 dark:border-amber-500/20">
          <div className="mb-3 flex items-center gap-2">
            <GraduationCap className="h-4 w-4 text-amber-600" />
            <div>
              <h3 className="text-[15px] font-semibold tracking-tight text-ink">{t("year.title")}</h3>
              <p className="text-xs text-muted">{t("year.subtitle")}</p>
            </div>
          </div>
          <p className="mb-3 rounded-lg bg-amber-50 px-3 py-2 text-[11px] leading-relaxed text-amber-700 dark:bg-amber-500/10 dark:text-amber-300">
            {t("year.warning")}
          </p>
          <Button variant="danger" onClick={startPromote}><GraduationCap className="h-4 w-4" />{t("year.promote")}</Button>
        </Card>
      )}

      {/* year promotion confirmation with countdown */}
      <Modal open={yearOpen} onClose={() => setYearOpen(false)} title={t("year.promote")} size="sm">
        <div className="flex flex-col items-center gap-4 py-2 text-center">
          <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-amber-50 text-amber-600 dark:bg-amber-500/15">
            <AlertTriangle className="h-7 w-7" />
          </div>
          <p className="text-sm text-ink">{t("year.warning")}</p>
          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-gradient-to-br from-amber-500 to-rose-600 text-2xl font-bold text-white">
            {countdown}
          </div>
          <p className="text-xs font-medium text-muted">{t("year.countdown", { n: countdown })}</p>
          <div className="flex w-full gap-2">
            <Button variant="secondary" className="flex-1" onClick={() => setYearOpen(false)}>{t("year.cancel")}</Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
