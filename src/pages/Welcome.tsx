import { useState } from "react";
import { motion, AnimatePresence, useScroll, useTransform } from "framer-motion";
import {
  GraduationCap, Sun, Moon, Languages, Cloud, ShieldCheck, Wallet,
  QrCode, BarChart3, Sparkles, Users2, CalendarRange, DatabaseBackup,
  FileText, MessageCircle, Star, ArrowRight, ArrowDown,
  Zap, Lock, Globe, TrendingUp, Bot, CheckCircle2,
  Users, ScanLine, ClipboardCheck, Send, Cpu, KeyRound,
  Mail, ExternalLink,
} from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { useApp } from "../context/AppContext";
import { Button } from "../components/ui";
import { Reveal, Counter, Tilt, Float, Spotlight } from "../components/motion";
import { cn } from "../utils/cn";

type T = (k: string) => string;

/* ============================================================= EXPORT */
export function Welcome({
  onSignIn,
  onParentPortal,
}: {
  onSignIn: (mode: "in" | "up") => void;
  onParentPortal: () => void;
}) {
  const { t, lang, toggleLang, theme, toggleTheme } = useApp();
  const scrollTo = (id: string) => document.getElementById(id)?.scrollIntoView({ behavior: "smooth" });

  return (
    <div className="noise-overlay relative min-h-screen overflow-x-hidden bg-bg text-ink">
      <Nav
        theme={theme}
        toggleLang={toggleLang}
        toggleTheme={toggleTheme}
        onSignIn={() => onSignIn("in")}
        onSignUp={() => onSignIn("up")}
        t={t}
      />

      <Hero
        t={t}
        lang={lang}
        onSignUp={() => onSignIn("up")}
        onParentPortal={onParentPortal}
        onWatch={() => scrollTo("features")}
      />

      <TrustBar t={t} />
      <Stats t={t} />
      <BentoFeatures t={t} />
      <Preview t={t} lang={lang} />
      <AISection t={t} lang={lang} />
      <Security t={t} />
      <Testimonials t={t} lang={lang} />
      <Steps t={t} />
      <DeveloperSignature t={t} lang={lang} />
      <FinalCTA t={t} onSignUp={() => onSignIn("up")} onSignIn={() => onSignIn("in")} />
      <Footer t={t} />
    </div>
  );
}

/* ============================================================= NAV */
function Nav({ theme, toggleLang, toggleTheme, onSignIn, onSignUp, t }: { theme: string; toggleLang: () => void; toggleTheme: () => void; onSignIn: () => void; onSignUp: () => void; t: T }) {
  return (
    <motion.header
      initial={{ y: -30, opacity: 0 }}
      animate={{ y: 0, opacity: 1 }}
      transition={{ duration: 0.6, ease: [0.22, 1, 0.36, 1] }}
      className="fixed inset-x-0 top-0 z-50 flex justify-center px-3 pt-3"
    >
      <div className="glass-panel flex w-full max-w-6xl items-center gap-3 rounded-2xl px-4 py-2.5">
        <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-br from-brand-500 to-brand-700 text-white shadow-lg shadow-brand-600/30">
          <GraduationCap className="h-[18px] w-[18px]" />
        </div>
        <div>
          <p className="text-sm font-bold tracking-tight">{t("app.name")}</p>
          <p className="hidden text-[10px] text-muted sm:block">{t("app.tagline")}</p>
        </div>
        <nav className="ms-auto hidden items-center gap-6 md:flex">
          <a href="#features" className="text-sm font-medium text-muted transition hover:text-ink">{t("land.footer.features")}</a>
          <a href="#preview" className="text-sm font-medium text-muted transition hover:text-ink">{t("land.previewTitle")}</a>
          <a href="#security" className="text-sm font-medium text-muted transition hover:text-ink">{t("privacy.title")}</a>
        </nav>
        <div className="ms-auto flex items-center gap-2 md:ms-0">
          <IconBtn onClick={toggleLang}><Languages className="h-4 w-4" /></IconBtn>
          <IconBtn onClick={toggleTheme}>{theme === "light" ? <Moon className="h-4 w-4" /> : <Sun className="h-4 w-4" />}</IconBtn>
          <Button size="sm" variant="secondary" onClick={onSignIn} className="hidden sm:inline-flex">{t("auth.signIn")}</Button>
          <Button size="sm" onClick={onSignUp} className="shine">{t("welcome.cta")}</Button>
        </div>
      </div>
    </motion.header>
  );
}

function IconBtn({ children, onClick }: { children: React.ReactNode; onClick: () => void }) {
  return (
    <button onClick={onClick} className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-line transition hover:bg-elevated">
      {children}
    </button>
  );
}

/* ============================================================= HERO */
function Hero({ t, lang, onSignUp, onWatch }: { t: T; lang: string; onSignUp: () => void; onParentPortal: () => void; onWatch: () => void }) {
  const isAr = lang === "ar";
  const { scrollY } = useScroll();
  const yText = useTransform(scrollY, [0, 400], [0, -50]);
  const yMock = useTransform(scrollY, [0, 400], [0, 80]);

  return (
    <section className="aurora-bg noise-overlay relative overflow-hidden pb-20 pt-32 sm:pt-40">
      {/* animated orbs */}
      <div className="pointer-events-none absolute inset-0 -z-10">
        <div className="aurora absolute -left-32 top-10 h-[420px] w-[420px] rounded-full bg-brand-500/20 blur-[120px]" />
        <div className="aurora absolute -right-32 top-40 h-[480px] w-[480px] rounded-full bg-accent-500/20 blur-[130px]" style={{ animationDelay: "3s" }} />
        <div className="aurora absolute left-1/3 top-0 h-[360px] w-[360px] rounded-full bg-sky-400/15 blur-[110px]" style={{ animationDelay: "6s" }} />
        <div className="grid-bg absolute inset-0 opacity-50 [mask-image:radial-gradient(70%_50%_at_50%_30%,black,transparent)]" />
      </div>

      <div className="mx-auto grid max-w-6xl items-center gap-12 px-4 sm:px-6 lg:grid-cols-[1.1fr_1fr]">
        {/* copy */}
        <motion.div style={{ y: yText }} className="text-center lg:text-start">
          <motion.div
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ duration: 0.5 }}
            className="inline-flex items-center gap-2 rounded-full border border-brand-200/60 bg-brand-50/70 px-4 py-1.5 text-xs font-semibold text-brand-700 backdrop-blur dark:border-brand-500/30 dark:bg-brand-500/10 dark:text-brand-200"
          >
            <span className="relative flex h-2 w-2">
              <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-brand-400 opacity-75" />
              <span className="relative inline-flex h-2 w-2 rounded-full bg-brand-500" />
            </span>
            {t("welcome.tagline")}
          </motion.div>

          <motion.h1
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.1, ease: [0.22, 1, 0.36, 1] }}
            className="text-balance mt-6 text-4xl font-extrabold leading-[1.05] tracking-tight sm:text-5xl lg:text-[4rem]"
          >
            {isAr ? (
              <>أدر مركزك التعليمي <span className="text-grad">بالكامل</span><br className="hidden sm:block" /> من مكان واحد</>
            ) : (
              <>Run your entire center<br className="hidden sm:block" /> <span className="text-grad">in one place</span></>
            )}
          </motion.h1>

          <motion.p
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.7, delay: 0.2 }}
            className="text-pretty mx-auto mt-5 max-w-xl text-base text-muted sm:text-lg lg:mx-0"
          >
            {t("land.subHero")}
          </motion.p>

          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.7, delay: 0.3 }}
            className="mt-8 flex flex-wrap items-center justify-center gap-3 lg:justify-start"
          >
            <Button size="lg" onClick={onSignUp} className="magnetic-btn shine px-7 text-base">
              {t("welcome.cta")} <ArrowRight className="h-5 w-5 rtl:rotate-180" />
            </Button>
            <button onClick={onWatch} className="group inline-flex items-center gap-2 rounded-xl border border-line bg-surface/60 px-5 py-3 text-sm font-semibold backdrop-blur transition hover:border-brand-300 hover:bg-brand-50/50 dark:hover:bg-brand-500/10">
              <span className="flex h-7 w-7 items-center justify-center rounded-full bg-brand-100 text-brand-600 dark:bg-brand-500/20">
                <ArrowDown className="h-3.5 w-3.5" />
              </span>
              {t("land.watchFeatures")}
            </button>
          </motion.div>

          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ duration: 0.7, delay: 0.45 }}
            className="mt-8 flex items-center justify-center gap-4 lg:justify-start"
          >
            <div className="flex -space-x-2">
              {["from-brand-400 to-brand-600", "from-accent-400 to-accent-600", "from-emerald-400 to-emerald-600", "from-amber-400 to-amber-600", "from-sky-400 to-sky-600"].map((c, i) => (
                <div key={i} className={cn("h-8 w-8 rounded-full bg-gradient-to-br ring-2 ring-bg", c)} />
              ))}
            </div>
            <div className="text-start">
              <div className="flex items-center gap-0.5">{[...Array(5)].map((_, i) => <Star key={i} className="h-3.5 w-3.5 fill-amber-400 text-amber-400" />)}</div>
              <p className="text-[11px] text-muted">{isAr ? "موثوق من +1000 مركز" : "Trusted by 1000+ centers"}</p>
            </div>
          </motion.div>
        </motion.div>

        {/* 3D mockup */}
        <motion.div style={{ y: yMock }} className="relative">
          <HeroMockup t={t} />
        </motion.div>
      </div>
    </section>
  );
}

function HeroMockup({ t }: { t: T }) {
  return (
    <div className="relative mx-auto max-w-lg">
      {/* floating badges */}
      <Float delay={0.5} className="pointer-events-none absolute -left-6 -top-6 z-20 sm:-left-10">
        <div className="glass-panel flex items-center gap-2 rounded-2xl px-3 py-2">
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-emerald-500 text-white"><TrendingUp className="h-4 w-4" /></div>
          <div>
            <p className="text-[10px] text-muted">{t("dash.attendanceRate")}</p>
            <p className="text-sm font-bold text-emerald-600">94% ↑</p>
          </div>
        </div>
      </Float>
      <Float delay={1.2} distance={16} className="pointer-events-none absolute -right-4 top-1/3 z-20 sm:-right-8">
        <div className="glass-panel flex items-center gap-2 rounded-2xl px-3 py-2">
          <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-brand-500 text-white"><Wallet className="h-4 w-4" /></div>
          <div>
            <p className="text-[10px] text-muted">{t("dash.monthlyRevenue")}</p>
            <p className="text-sm font-bold text-brand-600">42,180</p>
          </div>
        </div>
      </Float>

      <Tilt intensity={7} className="relative">
        {/* glow behind */}
        <div className="conic-glow absolute -inset-3 rounded-[2rem] opacity-30" />
        <div className="relative overflow-hidden rounded-3xl border border-line bg-surface shadow-[0_40px_90px_-20px_rgba(15,23,42,0.4)]">
          {/* window chrome */}
          <div className="flex items-center gap-2 border-b border-line bg-elevated/60 px-4 py-3">
            <span className="h-3 w-3 rounded-full bg-rose-400" />
            <span className="h-3 w-3 rounded-full bg-amber-400" />
            <span className="h-3 w-3 rounded-full bg-emerald-400" />
            <span className="ms-3 text-[11px] font-medium text-faint">{t("dash.title")}</span>
          </div>
          {/* KPI grid */}
          <div className="grid grid-cols-2 gap-3 p-4">
            {[
              { l: t("dash.totalStudents"), v: "248", tone: "from-brand-500 to-brand-600", ic: GraduationCap },
              { l: t("dash.activeGroups"), v: "18", tone: "from-violet-500 to-purple-600", ic: CalendarRange },
              { l: t("dash.monthlyRevenue"), v: "42k", tone: "from-emerald-500 to-green-600", ic: Wallet },
              { l: t("teachers.title"), v: "12", tone: "from-sky-500 to-blue-600", ic: Users },
            ].map((k, i) => {
              const Ic = k.ic;
              return (
                <motion.div
                  key={i}
                  initial={{ opacity: 0, scale: 0.9 }}
                  whileInView={{ opacity: 1, scale: 1 }}
                  viewport={{ once: true }}
                  transition={{ delay: 0.3 + i * 0.1 }}
                  className="rounded-xl border border-line bg-surface p-3"
                >
                  <div className={cn("mb-2 flex h-8 w-8 items-center justify-center rounded-lg bg-gradient-to-br text-white", k.tone)}><Ic className="h-4 w-4" /></div>
                  <p className="text-xl font-bold">{k.v}</p>
                  <p className="truncate text-[10px] text-muted">{k.l}</p>
                </motion.div>
              );
            })}
          </div>
          {/* animated chart */}
          <div className="flex items-end gap-2 px-4 pb-4">
            {[40, 65, 50, 80, 60, 92, 72, 85, 68, 95].map((h, i) => (
              <motion.div
                key={i}
                className="flex-1 rounded-t-md bg-gradient-to-t from-brand-500 to-accent-400"
                initial={{ height: 0 }}
                whileInView={{ height: `${h}px` }}
                viewport={{ once: true }}
                transition={{ duration: 0.8, delay: 0.5 + i * 0.06, ease: [0.22, 1, 0.36, 1] }}
              />
            ))}
          </div>
        </div>
      </Tilt>
    </div>
  );
}

/* ============================================================= TRUST BAR */
function TrustBar({ t }: { t: T }) {
  const pills = [
    { icon: Zap, label: t("welcome.bannerOffline") },
    { icon: Cloud, label: t("welcome.bannerCloud") },
    { icon: Sparkles, label: t("welcome.bannerAI") },
    { icon: QrCode, label: t("welcome.bannerQR") },
    { icon: FileText, label: t("welcome.bannerPDF") },
    { icon: DatabaseBackup, label: t("feat.backup") },
  ];
  return (
    <section className="border-y border-line bg-surface/50 py-6">
      <div className="mx-auto max-w-6xl px-4 sm:px-6">
        <Reveal className="mb-4 text-center text-xs font-semibold uppercase tracking-widest text-faint">{t("land.trustTitle")}</Reveal>
        <div className="flex flex-wrap items-center justify-center gap-2.5">
          {pills.map((p, i) => {
            const Ic = p.icon;
            return (
              <Reveal key={i} delay={i * 0.06}>
                <div className="inline-flex items-center gap-2 rounded-full border border-line bg-surface px-4 py-2 text-xs font-medium shadow-sm transition hover:-translate-y-0.5 hover:border-brand-300 hover:shadow-md">
                  <Ic className="h-4 w-4 text-brand-500" />
                  {p.label}
                </div>
              </Reveal>
            );
          })}
        </div>
      </div>
    </section>
  );
}

/* ============================================================= STATS */
function Stats({ t }: { t: T }) {
  const stats = [
    { value: 10000, suffix: "+", label: t("land.stat1"), icon: Users2, tone: "text-brand-500" },
    { value: 98, suffix: "%", label: t("land.stat2"), icon: CheckCircle2, tone: "text-emerald-500" },
    { value: 24, suffix: "/7", label: t("land.stat3"), icon: Globe, tone: "text-sky-500" },
    { value: 100, suffix: "%", label: t("land.stat4"), icon: Lock, tone: "text-violet-500" },
  ];
  return (
    <section className="mx-auto max-w-6xl px-4 py-16 sm:px-6">
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        {stats.map((s, i) => {
          const Ic = s.icon;
          return (
            <Reveal key={i} delay={i * 0.08}>
              <div className="spotlight-card group relative overflow-hidden rounded-2xl bg-surface p-6 text-center ring-1 ring-line transition hover:-translate-y-1 hover:shadow-xl">
                <Ic className={cn("mx-auto mb-3 h-7 w-7 transition-transform group-hover:scale-110", s.tone)} />
                <p className="text-grad text-4xl font-extrabold sm:text-5xl">
                  <Counter to={s.value} suffix={s.suffix} />
                </p>
                <p className="mt-2 text-sm font-medium text-muted">{s.label}</p>
              </div>
            </Reveal>
          );
        })}
      </div>
    </section>
  );
}

/* ============================================================= BENTO FEATURES */
function BentoFeatures({ t }: { t: T }) {
  return (
    <section id="features" className="mx-auto max-w-6xl px-4 py-16 sm:px-6">
      <Reveal className="mb-12 text-center">
        <span className="text-xs font-bold uppercase tracking-widest text-brand-500">{t("land.footer.features")}</span>
        <h2 className="text-balance mt-2 text-3xl font-extrabold tracking-tight sm:text-4xl">{t("land.featuresTitle")}</h2>
        <p className="text-pretty mx-auto mt-3 max-w-lg text-sm text-muted">{t("land.featuresSub")}</p>
      </Reveal>

      <div className="grid auto-rows-[minmax(170px,auto)] grid-cols-2 gap-4 lg:grid-cols-4">
        {/* QR - large */}
        <Reveal className="col-span-2 row-span-2 lg:col-span-2">
          <BentoCard tone="from-sky-500 to-blue-600" icon={QrCode} title={t("feat.attendance")} desc={t("feat.attendanceD")} big>
            <div className="mt-5 flex items-center gap-4">
              <div className="relative flex h-20 w-20 items-center justify-center rounded-2xl border-2 border-dashed border-sky-300 bg-sky-50 dark:bg-sky-500/10">
                <ScanLine className="h-9 w-9 text-sky-500" />
                <motion.span className="absolute inset-x-3 h-0.5 rounded-full bg-sky-400 shadow-[0_0_8px_2px] shadow-sky-400/50" animate={{ top: ["20%", "80%", "20%"] }} transition={{ duration: 2, repeat: Infinity, ease: "easeInOut" }} />
              </div>
              <div className="flex-1 space-y-2">
                {[85, 70].map((w, i) => (
                  <motion.div key={i} className="h-2.5 rounded-full bg-gradient-to-r from-sky-400 to-blue-500" initial={{ width: 0 }} whileInView={{ width: `${w}%` }} viewport={{ once: true }} transition={{ duration: 0.9, delay: i * 0.2 }} />
                ))}
              </div>
            </div>
          </BentoCard>
        </Reveal>

        {/* AI - wide */}
        <Reveal className="col-span-2" delay={0.05}>
          <BentoCard tone="from-violet-500 to-purple-600" icon={Sparkles} title={t("feat.ai")} desc={t("feat.aiD")} wide>
            <div className="mt-4 space-y-2">
              <div className="flex items-center gap-2">
                <Bot className="h-5 w-5 shrink-0 text-violet-500" />
                <motion.div className="h-2 rounded-full bg-violet-200 dark:bg-violet-500/25" initial={{ width: 0 }} whileInView={{ width: "80%" }} viewport={{ once: true }} transition={{ delay: 0.3, duration: 0.8 }} />
              </div>
              <div className="flex items-center gap-2">
                <Sparkles className="h-5 w-5 shrink-0 text-violet-400" />
                <motion.div className="h-2 rounded-full bg-violet-100 dark:bg-violet-500/15" initial={{ width: 0 }} whileInView={{ width: "60%" }} viewport={{ once: true }} transition={{ delay: 0.45, duration: 0.8 }} />
              </div>
            </div>
          </BentoCard>
        </Reveal>

        {/* Finance */}
        <Reveal delay={0.1}>
          <BentoCard tone="from-emerald-500 to-green-600" icon={Wallet} title={t("feat.finance")} desc={t("feat.financeD")} />
        </Reveal>

        {/* Students */}
        <Reveal delay={0.15}>
          <BentoCard tone="from-brand-500 to-brand-600" icon={Users} title={t("feat.staff")} desc={t("feat.staffD")} />
        </Reveal>

        {/* Exams */}
        <Reveal delay={0.2}>
          <BentoCard tone="from-amber-500 to-orange-600" icon={FileText} title={t("feat.exams")} desc={t("feat.examsD")} />
        </Reveal>

        {/* Schedule */}
        <Reveal delay={0.25}>
          <BentoCard tone="from-rose-500 to-pink-600" icon={CalendarRange} title={t("feat.schedule")} desc={t("feat.scheduleD")} />
        </Reveal>

        {/* Reports */}
        <Reveal delay={0.3}>
          <BentoCard tone="from-indigo-500 to-blue-700" icon={BarChart3} title={t("feat.reports")} desc={t("feat.reportsD")} />
        </Reveal>

        {/* Backup */}
        <Reveal delay={0.35}>
          <BentoCard tone="from-teal-500 to-cyan-600" icon={DatabaseBackup} title={t("feat.backup")} desc={t("feat.backupD")} />
        </Reveal>
      </div>
    </section>
  );
}

function BentoCard({ tone, icon: Icon, title, desc, big, wide, children }: { tone: string; icon: LucideIcon; title: string; desc: string; big?: boolean; wide?: boolean; children?: React.ReactNode }) {
  return (
    <Spotlight className="spotlight-card group h-full overflow-hidden rounded-2xl bg-surface p-5 ring-1 ring-line transition hover:-translate-y-1 hover:shadow-2xl">
      <div className={cn("mb-3 flex items-center justify-center rounded-xl bg-gradient-to-br text-white shadow-lg transition-transform duration-300 group-hover:scale-110 group-hover:rotate-3", big ? "h-14 w-14" : "h-11 w-11", tone)}>
        <Icon className={big ? "h-7 w-7" : "h-5 w-5"} />
      </div>
      <h3 className={cn("font-bold tracking-tight", big ? "text-xl" : "text-base")}>{title}</h3>
      <p className={cn("mt-1.5 text-muted", big ? "text-sm" : "text-xs", wide && "max-w-md")}>{desc}</p>
      {children}
    </Spotlight>
  );
}

/* ============================================================= PREVIEW */
function Preview({ t, lang }: { t: T; lang: string }) {
  const [tab, setTab] = useState("students");
  const tabs = [
    { id: "students", label: t("land.preview.students"), icon: GraduationCap },
    { id: "attendance", label: t("land.preview.attendance"), icon: ClipboardCheck },
    { id: "finance", label: t("land.preview.finance"), icon: Wallet },
    { id: "ai", label: t("land.preview.ai"), icon: Sparkles },
  ];
  return (
    <section id="preview" className="mx-auto max-w-6xl px-4 py-16 sm:px-6">
      <Reveal className="mb-10 text-center">
        <span className="text-xs font-bold uppercase tracking-widest text-brand-500">{t("land.previewTitle")}</span>
        <h2 className="text-balance mt-2 text-3xl font-extrabold tracking-tight sm:text-4xl">{t("land.previewTitle")}</h2>
        <p className="text-pretty mx-auto mt-3 max-w-lg text-sm text-muted">{t("land.previewSub")}</p>
      </Reveal>

      <Reveal>
        <div className="mb-6 flex flex-wrap justify-center gap-2">
          {tabs.map((tb) => {
            const Ic = tb.icon;
            const active = tab === tb.id;
            return (
              <button key={tb.id} onClick={() => setTab(tb.id)} className={cn("inline-flex items-center gap-2 rounded-xl border px-4 py-2.5 text-sm font-semibold transition", active ? "border-transparent bg-gradient-to-br from-brand-500 to-brand-700 text-white shadow-lg shadow-brand-600/25" : "border-line bg-surface text-muted hover:text-ink")}>
                <Ic className="h-4 w-4" />{tb.label}
              </button>
            );
          })}
        </div>
      </Reveal>

      <Reveal delay={0.1}>
        <div className="relative mx-auto max-w-4xl">
          <div className="conic-glow absolute -inset-2 rounded-[1.75rem] opacity-25" />
          <div className="relative overflow-hidden rounded-3xl border border-line bg-surface shadow-[0_30px_70px_-20px_rgba(15,23,42,0.3)]">
            <div className="flex items-center gap-2 border-b border-line bg-elevated/60 px-4 py-3">
              <span className="h-3 w-3 rounded-full bg-rose-400" />
              <span className="h-3 w-3 rounded-full bg-amber-400" />
              <span className="h-3 w-3 rounded-full bg-emerald-400" />
              <span className="ms-3 text-[11px] font-medium text-faint">{tabs.find((x) => x.id === tab)?.label}</span>
            </div>
            <AnimatePresence mode="wait">
              <motion.div key={tab} initial={{ opacity: 0, y: 14 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -14 }} transition={{ duration: 0.35, ease: [0.22, 1, 0.36, 1] }} className="p-5">
                <PreviewPane tab={tab} lang={lang} t={t} />
              </motion.div>
            </AnimatePresence>
          </div>
        </div>
      </Reveal>
    </section>
  );
}

function PreviewPane({ tab, lang, t }: { tab: string; lang: string; t: T }) {
  const isAr = lang === "ar";
  if (tab === "students") {
    return (
      <div className="space-y-2">
        {(isAr ? [["عمر حسن", "حاضر"], ["لينا عادل", "متأخرة"], ["يوسف سامي", "حاضر"], ["مريم خالد", "غائبة"]] : [["Omar Hassan", "Present"], ["Lina Adel", "Late"], ["Youssef Sami", "Present"], ["Mariam Khaled", "Absent"]]).map(([n, st], i) => (
          <div key={i} className="flex items-center gap-3 rounded-xl border border-line p-2.5 transition hover:bg-elevated/40">
            <div className="flex h-9 w-9 items-center justify-center rounded-full bg-gradient-to-br from-brand-400 to-brand-600 text-xs font-bold text-white">{n.split(" ").map((p) => p[0]).join("")}</div>
            <div className="min-w-0 flex-1"><p className="text-sm font-medium">{n}</p><p className="font-mono text-[10px] text-faint">STU_{1001 + i}</p></div>
            <span className={cn("rounded-full px-2.5 py-0.5 text-[10px] font-bold", st.includes("Pr") || st.includes("ح") ? "bg-emerald-50 text-emerald-600 dark:bg-emerald-500/15" : st.includes("La") || st.includes("مت") ? "bg-amber-50 text-amber-600 dark:bg-amber-500/15" : "bg-rose-50 text-rose-600 dark:bg-rose-500/15")}>{st}</span>
          </div>
        ))}
      </div>
    );
  }
  if (tab === "attendance") {
    return (
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        {[
          { l: t("att.present"), v: 212, c: "text-emerald-600", b: "from-emerald-500 to-green-600", ic: CheckCircle2 },
          { l: t("att.absent"), v: 18, c: "text-rose-600", b: "from-rose-500 to-pink-600", ic: ShieldCheck },
          { l: t("att.late"), v: 12, c: "text-amber-600", b: "from-amber-500 to-orange-600", ic: Zap },
          { l: t("att.excused"), v: 6, c: "text-sky-600", b: "from-sky-500 to-blue-600", ic: FileText },
        ].map((k, i) => {
          const Ic = k.ic;
          return (
            <div key={i} className="rounded-xl border border-line p-3 text-center">
              <div className={cn("mx-auto mb-2 flex h-9 w-9 items-center justify-center rounded-lg bg-gradient-to-br text-white", k.b)}><Ic className="h-4 w-4" /></div>
              <p className={cn("text-2xl font-bold", k.c)}>{k.v}</p>
              <p className="text-[10px] text-muted">{k.l}</p>
            </div>
          );
        })}
      </div>
    );
  }
  if (tab === "finance") {
    return (
      <div className="space-y-3">
        <div className="grid grid-cols-3 gap-3">
          {[{ l: t("fin.income"), v: "42,180", c: "text-emerald-600" }, { l: t("fin.outcome"), v: "18,400", c: "text-rose-600" }, { l: t("fin.balance"), v: "23,780", c: "text-brand-600" }].map((k, i) => (
            <div key={i} className="rounded-xl border border-line p-4"><p className="text-[10px] text-muted">{k.l}</p><p className={cn("mt-1 text-xl font-bold", k.c)}>{k.v}</p></div>
          ))}
        </div>
        <div className="flex items-end gap-2 rounded-xl border border-line p-4">
          {[45, 70, 55, 85, 60, 90, 75, 65, 95, 80].map((h, i) => (
            <motion.div key={i} className="flex-1 rounded-t bg-gradient-to-t from-brand-500 to-accent-400" initial={{ height: 0 }} animate={{ height: `${h}px` }} transition={{ duration: 0.6, delay: i * 0.06 }} />
          ))}
        </div>
      </div>
    );
  }
  // ai
  return (
    <div className="space-y-3">
      <div className="ms-auto max-w-[75%] rounded-2xl rounded-tl-sm bg-brand-600 px-4 py-2.5 text-sm text-white">
        {isAr ? "حلّل أداء طالب الصف الثالث" : "Analyze Grade 3 student performance"}
      </div>
      <div className="max-w-[80%] rounded-2xl rounded-tr-sm border border-line bg-elevated/60 px-4 py-2.5 text-sm">
        <div className="mb-2 flex items-center gap-2"><Sparkles className="h-4 w-4 text-violet-500" /><span className="font-semibold">AI</span></div>
        <p className="text-muted">{isAr ? "معدل الحضور 92% ومتوسط الدرجات 78%. الطالب يحتاج تركيزاً في الجبر. أنصح بجلسة مراجعة أسبوعية." : "Attendance 92%, average 78%. Student needs focus on Algebra. Recommend a weekly review session."}</p>
      </div>
    </div>
  );
}

/* ============================================================= AI SECTION */
function AISection({ t, lang }: { t: T; lang: string }) {
  const isAr = lang === "ar";
  const feats = [
    { icon: TrendingUp, key: "ai.f1" },
    { icon: ShieldCheck, key: "ai.f2" },
    { icon: BarChart3, key: "ai.f3" },
    { icon: Sparkles, key: "ai.f4" },
  ];
  return (
    <section className="noise-overlay relative overflow-hidden py-20">
      <div className="aurora pointer-events-none absolute -left-20 top-0 h-96 w-96 rounded-full bg-violet-500/15 blur-[100px]" />
      <div className="aurora pointer-events-none absolute -right-20 bottom-0 h-96 w-96 rounded-full bg-brand-500/15 blur-[100px]" style={{ animationDelay: "4s" }} />
      <div className="mx-auto grid max-w-6xl items-center gap-12 px-4 sm:px-6 lg:grid-cols-2">
        <Reveal>
          <span className="inline-flex items-center gap-2 rounded-full border border-violet-200/60 bg-violet-50/70 px-3 py-1 text-xs font-semibold text-violet-700 dark:border-violet-500/30 dark:bg-violet-500/10 dark:text-violet-300">
            <Cpu className="h-3.5 w-3.5" />{t("feat.ai")}
          </span>
          <h2 className="text-balance mt-4 text-3xl font-extrabold tracking-tight sm:text-4xl">{t("land.aiTitle")}</h2>
          <p className="text-pretty mt-3 text-muted">{t("land.aiSub")}</p>
          <div className="mt-6 grid gap-3 sm:grid-cols-2">
            {feats.map((f, i) => {
              const Ic = f.icon;
              return (
                <Reveal key={i} delay={i * 0.08}>
                  <div className="flex items-start gap-3 rounded-xl border border-line bg-surface p-3 transition hover:-translate-y-0.5 hover:shadow-md">
                    <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-gradient-to-br from-violet-500 to-purple-600 text-white"><Ic className="h-4 w-4" /></div>
                    <p className="pt-1 text-sm font-medium">{t(`land.${f.key}`)}</p>
                  </div>
                </Reveal>
              );
            })}
          </div>
        </Reveal>

        <Reveal delay={0.15}>
          <Tilt intensity={4} className="relative">
            <div className="conic-glow absolute -inset-3 rounded-[2rem] opacity-25" />
            <div className="relative rounded-3xl border border-line bg-surface p-5 shadow-[0_30px_70px_-20px_rgba(15,23,42,0.3)]">
              <div className="mb-4 flex items-center gap-2 border-b border-line pb-3">
                <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-br from-violet-500 to-purple-600 text-white"><Bot className="h-5 w-5" /></div>
                <div><p className="text-sm font-bold">Center AI</p><p className="flex items-center gap-1 text-[10px] text-emerald-500"><span className="h-1.5 w-1.5 rounded-full bg-emerald-500 live-dot" />Online</p></div>
              </div>
              <div className="space-y-3">
                <div className="ms-auto max-w-[80%] rounded-2xl rounded-tl-sm bg-brand-600 px-4 py-2.5 text-sm text-white">
                  {isAr ? "من هم الطلاب المتعثرون هذا الشهر؟" : "Who are the struggling students this month?"}
                </div>
                <motion.div initial={{ opacity: 0, y: 10 }} whileInView={{ opacity: 1, y: 0 }} viewport={{ once: true }} transition={{ delay: 0.3 }} className="max-w-[88%] rounded-2xl rounded-tr-sm border border-line bg-elevated/60 px-4 py-3 text-sm">
                  <p className="mb-2 font-semibold text-violet-600">{isAr ? "وجدت ٤ طلاب:" : "Found 4 students:"}</p>
                  {["Omar · Algebra 48%", "Lina · Physics 52%", "Youssef · Chemistry 55%"].map((s, i) => (
                    <p key={i} className="flex items-center gap-2 py-0.5 text-muted"><span className="h-1.5 w-1.5 rounded-full bg-rose-400" />{s}</p>
                  ))}
                </motion.div>
                <div className="flex items-center gap-2">
                  <div className="flex-1 rounded-full border border-line bg-surface px-4 py-2 text-xs text-faint">{t("messages.placeholder")}</div>
                  <button className="flex h-9 w-9 items-center justify-center rounded-full bg-brand-600 text-white"><Send className="h-4 w-4 rtl:rotate-180" /></button>
                </div>
              </div>
            </div>
          </Tilt>
        </Reveal>
      </div>
    </section>
  );
}

/* ============================================================= SECURITY */
function Security({ t }: { t: T }) {
  const items = [
    { icon: DatabaseBackup, key: "sec.local" },
    { icon: Cloud, key: "sec.sync" },
    { icon: KeyRound, key: "sec.rbac" },
    { icon: Lock, key: "sec.backup" },
  ];
  return (
    <section id="security" className="mx-auto max-w-6xl px-4 py-16 sm:px-6">
      <Reveal className="mb-10 text-center">
        <span className="text-xs font-bold uppercase tracking-widest text-emerald-500">{t("privacy.title")}</span>
        <h2 className="text-balance mt-2 text-3xl font-extrabold tracking-tight sm:text-4xl">{t("land.securityTitle")}</h2>
        <p className="text-pretty mx-auto mt-3 max-w-lg text-sm text-muted">{t("land.securitySub")}</p>
      </Reveal>
      <div className="relative grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div className="absolute inset-x-0 top-7 hidden h-px bg-gradient-to-r from-transparent via-emerald-300 to-transparent lg:block" />
        {items.map((it, i) => {
          const Ic = it.icon;
          return (
            <Reveal key={i} delay={i * 0.1}>
              <div className="spotlight-card relative rounded-2xl bg-surface p-5 text-center ring-1 ring-line transition hover:-translate-y-1 hover:shadow-xl">
                <div className="relative z-10 mx-auto mb-3 flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-emerald-500 to-teal-600 text-white shadow-lg ring-4 ring-bg">
                  <Ic className="h-6 w-6" />
                </div>
                <h3 className="font-bold">{t(`land.${it.key}`)}</h3>
                <p className="mt-1 text-xs text-muted">{t(`land.${it.key}D`)}</p>
              </div>
            </Reveal>
          );
        })}
      </div>
    </section>
  );
}

/* ============================================================= TESTIMONIALS */
function Testimonials({ t, lang }: { t: T; lang: string }) {
  const isAr = lang === "ar";
  const items = isAr ? [
    { name: "أ. أحمد مصطفى", role: "صاحب سنتر النخبة", text: "وفّر عليّ البرنامج ساعات يومياً. الحضور والرسوم أصبحت بنقرة واحدة." },
    { name: "أ. سارة عبد الله", role: "مديرة سنتر المستقبل", text: "التقارير الذكية والذكاء الاصطناعي ساعداني أكتشف الطلاب المتعثرين مبكراً." },
    { name: "أ. خالد فؤاد", role: "مدير أكاديمية المعرفة", text: "أفضل نظام إدارة استخدمته. خصوصية البيانات راحة بال حقيقية." },
  ] : [
    { name: "Ahmed Mostafa", role: "Elite Center Owner", text: "This app saves me hours every day. Attendance and fees are now one click." },
    { name: "Sara Abdullah", role: "Future Center Manager", text: "Smart reports and AI helped me catch struggling students early." },
    { name: "Khaled Fouad", role: "Knowledge Academy", text: "The best management system I've used. Data privacy is real peace of mind." },
  ];
  return (
    <section className="mx-auto max-w-6xl px-4 py-16 sm:px-6">
      <Reveal className="mb-10 text-center">
        <span className="text-xs font-bold uppercase tracking-widest text-amber-500">{t("land.testimonialsTitle")}</span>
        <h2 className="text-balance mt-2 text-3xl font-extrabold tracking-tight sm:text-4xl">{t("land.testimonialsTitle")}</h2>
        <p className="text-pretty mx-auto mt-3 max-w-lg text-sm text-muted">{t("land.testimonialsSub")}</p>
      </Reveal>
      <div className="grid gap-4 md:grid-cols-3">
        {items.map((it, i) => (
          <Reveal key={i} delay={i * 0.1}>
            <div className="spotlight-card h-full rounded-2xl bg-surface p-6 ring-1 ring-line transition hover:-translate-y-1 hover:shadow-xl">
              <div className="mb-3 flex items-center gap-0.5">{[...Array(5)].map((_, j) => <Star key={j} className="h-4 w-4 fill-amber-400 text-amber-400" />)}</div>
              <p className="text-pretty text-sm leading-relaxed text-ink">“{it.text}”</p>
              <div className="mt-4 flex items-center gap-3 border-t border-line pt-4">
                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-gradient-to-br from-brand-400 to-accent-600 text-xs font-bold text-white">{it.name[0]}</div>
                <div><p className="text-sm font-bold">{it.name}</p><p className="text-[11px] text-muted">{it.role}</p></div>
              </div>
            </div>
          </Reveal>
        ))}
      </div>
    </section>
  );
}

/* ============================================================= STEPS */
function Steps({ t }: { t: T }) {
  const steps = [
    { icon: ShieldCheck, title: t("land.step1"), desc: t("land.step1D"), n: "01" },
    { icon: GraduationCap, title: t("land.step2"), desc: t("land.step2D"), n: "02" },
    { icon: Zap, title: t("land.step3"), desc: t("land.step3D"), n: "03" },
  ];
  return (
    <section id="steps" className="mx-auto max-w-5xl px-4 py-16 sm:px-6">
      <Reveal className="mb-12 text-center">
        <span className="text-xs font-bold uppercase tracking-widest text-brand-500">{t("land.stepsTitle")}</span>
        <h2 className="text-balance mt-2 text-3xl font-extrabold tracking-tight sm:text-4xl">{t("land.stepsTitle")}</h2>
        <p className="text-pretty mx-auto mt-3 max-w-lg text-sm text-muted">{t("land.stepsSub")}</p>
      </Reveal>
      <div className="relative grid gap-6 md:grid-cols-3">
        <div className="absolute inset-x-0 top-9 hidden h-px bg-gradient-to-r from-transparent via-brand-300 to-transparent md:block" />
        {steps.map((s, i) => {
          const Ic = s.icon;
          return (
            <Reveal key={i} delay={i * 0.12}>
              <div className="relative text-center">
                <div className="relative z-10 mx-auto mb-5 flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-brand-500 to-brand-700 text-white shadow-lg shadow-brand-600/30 ring-4 ring-bg">
                  <Ic className="h-7 w-7" />
                  <span className="absolute -end-1 -top-1 flex h-7 w-7 items-center justify-center rounded-full bg-accent-500 text-[11px] font-bold text-white ring-2 ring-bg">{s.n}</span>
                </div>
                <h3 className="text-lg font-bold">{s.title}</h3>
                <p className="text-pretty mx-auto mt-1.5 max-w-xs text-sm text-muted">{s.desc}</p>
              </div>
            </Reveal>
          );
        })}
      </div>
    </section>
  );
}

/* ============================================================= FINAL CTA */
function FinalCTA({ t, onSignUp, onSignIn }: { t: T; onSignUp: () => void; onSignIn: () => void }) {
  return (
    <section className="mx-auto max-w-6xl px-4 py-12 sm:px-6">
      <Reveal>
        <div className="noise-overlay relative overflow-hidden rounded-[32px] shadow-[0_30px_80px_-20px_rgba(79,70,229,0.4)]">
          <div className="mesh-hero relative px-6 py-20 text-center text-white sm:px-12 sm:py-24">
            <div className="aurora pointer-events-none absolute -right-10 -top-10 h-72 w-72 rounded-full bg-white/10 blur-[80px]" />
            <div className="aurora pointer-events-none absolute -bottom-10 -left-10 h-72 w-72 rounded-full bg-accent-400/20 blur-[80px]" style={{ animationDelay: "2s" }} />
            <div className="relative">
              <h2 className="text-balance mx-auto max-w-2xl text-3xl font-extrabold tracking-tight sm:text-5xl">{t("land.ctaTitle")}</h2>
              <p className="text-pretty mx-auto mt-4 max-w-md text-sm text-white/80 sm:text-base">{t("land.ctaSub")}</p>
              <div className="mt-9 flex flex-wrap items-center justify-center gap-3">
                <button onClick={onSignUp} className="shine inline-flex items-center gap-2 rounded-2xl bg-white px-8 py-4 text-base font-bold text-brand-700 shadow-xl transition hover:-translate-y-0.5 hover:bg-white/90 active:scale-95">
                  {t("land.ctaFree")} <ArrowRight className="h-5 w-5 rtl:rotate-180" />
                </button>
                <button onClick={onSignIn} className="inline-flex items-center gap-2 rounded-2xl border border-white/30 bg-white/10 px-7 py-4 text-base font-bold text-white backdrop-blur transition hover:-translate-y-0.5 hover:bg-white/20">
                  {t("auth.signIn")}
                </button>
              </div>
            </div>
          </div>
        </div>
      </Reveal>
    </section>
  );
}

/* ============================================================= FOOTER */
function Footer({ t }: { t: T }) {
  const cols = [
    { title: t("land.footer.product"), links: [t("nav.dashboard"), t("nav.students"), t("nav.teachers"), t("nav.finance")] },
    { title: t("land.footer.features"), links: [t("feat.attendance"), t("feat.ai"), t("feat.reports"), t("feat.schedule")] },
    { title: t("land.footer.support"), links: [t("welcome.contact"), t("land.footer.privacy"), "FAQ"] },
  ];
  return (
    <footer className="relative overflow-hidden border-t border-line bg-surface">
      <div className="aurora pointer-events-none absolute -left-20 -bottom-20 h-72 w-72 rounded-full bg-brand-500/5 blur-[100px]" />
      <div className="relative mx-auto max-w-6xl px-4 py-16 sm:px-6">
        <div className="grid gap-10 sm:grid-cols-2 lg:grid-cols-4">
          {/* brand */}
          <div className="lg:pe-8">
            <div className="flex items-center gap-2.5">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-brand-500 to-brand-700 text-white shadow-lg shadow-brand-600/30"><GraduationCap className="h-5 w-5" /></div>
              <div>
                <p className="font-bold tracking-tight">{t("app.name")}</p>
                <p className="text-[10px] text-muted">{t("app.tagline")}</p>
              </div>
            </div>
            <p className="text-pretty mt-4 text-xs leading-relaxed text-muted">{t("welcome.tagline")}</p>
            <div className="mt-4 flex gap-2">
              {[Globe, Cloud, ShieldCheck].map((Ic, i) => (
                <div key={i} className="flex h-8 w-8 items-center justify-center rounded-lg border border-line text-muted transition hover:border-brand-300 hover:text-brand-600"><Ic className="h-4 w-4" /></div>
              ))}
            </div>
          </div>
          {/* link columns */}
          {cols.map((c, i) => (
            <div key={i}>
              <p className="text-sm font-bold text-ink">{c.title}</p>
              <ul className="mt-4 space-y-2.5">
                {c.links.map((l, j) => (
                  <li key={j}><a href="#" className="text-xs text-muted transition hover:text-brand-600">{l}</a></li>
                ))}
              </ul>
            </div>
          ))}
        </div>
        {/* copyright */}
        <div className="mt-12 border-t border-line pt-6">
          <p className="text-center font-mono text-[11px] text-faint">{t("land.copyright")}</p>
        </div>
      </div>
    </footer>
  );
}

/* ============================================================= DEVELOPER SIGNATURE */
function DeveloperSignature({ t, lang }: { t: T; lang: string }) {
  const isAr = lang === "ar";
  return (
    <section className="noise-overlay relative overflow-hidden py-20 sm:py-28">
      <div className="aurora pointer-events-none absolute -left-20 -top-10 h-72 w-72 rounded-full bg-brand-500/10 blur-[90px]" />
      <div className="aurora pointer-events-none absolute -right-20 bottom-0 h-72 w-72 rounded-full bg-accent-500/10 blur-[90px]" style={{ animationDelay: "3s" }} />
      <div className="relative mx-auto max-w-3xl px-4 sm:px-6">
        <Reveal>
          <div className="glass-panel relative overflow-hidden rounded-[2rem] p-8 text-center shadow-[0_30px_70px_-20px_rgba(15,23,42,0.18)] sm:p-12">
            {/* subtle top accent */}
            <div className="absolute inset-x-0 top-0 h-1 bg-gradient-to-r from-brand-500 via-accent-500 to-brand-600" />

            {/* avatar */}
            <Reveal delay={0.05}>
              <div className="relative mx-auto mb-6 w-fit">
                <div className="conic-glow absolute -inset-2 rounded-full opacity-50" />
                <div className="relative flex h-28 w-28 items-center justify-center rounded-full bg-gradient-to-br from-brand-500 via-accent-500 to-brand-700 text-3xl font-extrabold text-white shadow-2xl ring-4 ring-bg">
                  {isAr ? "م ج" : "ME"}
                </div>
                <span className="absolute -bottom-1 -end-1 flex h-9 w-9 items-center justify-center rounded-full bg-emerald-500 text-white shadow-lg ring-4 ring-surface"><Star className="h-4 w-4 fill-current" /></span>
              </div>
            </Reveal>

            <Reveal delay={0.1}>
              <p className="text-xs font-bold uppercase tracking-widest text-faint">{t("land.dev.title")}</p>
              <h3 className="mt-2 text-3xl font-extrabold tracking-tight text-ink sm:text-4xl">{isAr ? t("welcome.creatorNameAr") : t("welcome.creatorName")}</h3>
              <p className="mt-2 text-sm font-semibold text-brand-600">{t("land.dev.role")}</p>
              <p className="text-pretty mx-auto mt-4 max-w-lg text-sm leading-relaxed text-muted">{t("land.dev.desc")}</p>
            </Reveal>

            {/* contact buttons */}
            <Reveal delay={0.18}>
              <div className="mt-8 flex flex-wrap items-center justify-center gap-3">
                <a href="https://wa.me/201009617278" target="_blank" rel="noopener noreferrer"
                  className="shine group inline-flex items-center gap-2.5 rounded-2xl bg-[#25D366] px-6 py-3.5 text-sm font-bold text-white shadow-lg shadow-emerald-600/30 transition hover:-translate-y-0.5 hover:shadow-xl active:scale-95">
                  <MessageCircle className="h-5 w-5 transition group-hover:scale-110" />
                  <span className="text-start leading-tight">
                    <span className="block">{t("welcome.contact")}</span>
                    <span className="block font-mono text-[11px] opacity-90" dir="ltr">{t("land.dev.phone")}</span>
                  </span>
                </a>
                <a href="https://linkedin.com" target="_blank" rel="noopener noreferrer"
                  className="group inline-flex h-[52px] w-[52px] items-center justify-center rounded-2xl border border-line bg-surface text-muted shadow-sm transition hover:-translate-y-0.5 hover:border-brand-300 hover:text-brand-600 hover:shadow-lg">
                  <ExternalLink className="h-5 w-5 transition group-hover:scale-110" />
                </a>
                <a href="mailto:hello@centerplus.app"
                  className="group inline-flex h-[52px] w-[52px] items-center justify-center rounded-2xl border border-line bg-surface text-muted shadow-sm transition hover:-translate-y-0.5 hover:border-brand-300 hover:text-brand-600 hover:shadow-lg">
                  <Mail className="h-5 w-5 transition group-hover:scale-110" />
                </a>
              </div>
            </Reveal>
          </div>
        </Reveal>
      </div>
    </section>
  );
}


