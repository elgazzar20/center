import { useMemo, useState } from "react";
import { CalendarCheck, CheckCircle2, XCircle, Clock, FileText } from "lucide-react";
import { useApp } from "../context/AppContext";
import { Card, Select } from "./ui";
import { studentMonthAttendance, type SessionStatus } from "../lib/analytics";
import { monthKey, now } from "../lib/db";
import { cn } from "../utils/cn";

const STATUS_META: Record<SessionStatus, { tone: string; ring: string; icon: typeof CheckCircle2 }> = {
  PRESENT: { tone: "text-emerald-600 bg-emerald-50 dark:bg-emerald-500/10", ring: "ring-emerald-500/30", icon: CheckCircle2 },
  LATE: { tone: "text-amber-600 bg-amber-50 dark:bg-amber-500/10", ring: "ring-amber-500/30", icon: Clock },
  ABSENT: { tone: "text-rose-600 bg-rose-50 dark:bg-rose-500/10", ring: "ring-rose-500/30", icon: XCircle },
  EXCUSED: { tone: "text-sky-600 bg-sky-50 dark:bg-sky-500/10", ring: "ring-sky-500/30", icon: FileText },
  UNMARKED: { tone: "text-faint bg-elevated", ring: "ring-line", icon: FileText },
};

/** Lists the last N months as "yyyy-MM" + label options. */
function recentMonths(count: number) {
  const out: { value: string; label: string }[] = [];
  for (let i = 0; i < count; i++) {
    const d = new Date();
    d.setDate(1);
    d.setMonth(d.getMonth() - i);
    const value = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
    out.push({ value, label: d.toLocaleDateString(undefined, { month: "long", year: "numeric" }) });
  }
  return out;
}

export function MonthlyAttendance({ studentId }: { studentId: string }) {
  const { db, t, lang } = useApp();
  const [month, setMonth] = useState(monthKey(now()));
  const months = useMemo(() => recentMonths(6), []);

  const data = useMemo(
    () => studentMonthAttendance(db, studentId, month),
    [db, studentId, month],
  );

  const counts = [
    { key: "PRESENT" as const, n: data.present, label: t("att.presentShort") },
    { key: "ABSENT" as const, n: data.absent, label: t("att.absentShort") },
    { key: "LATE" as const, n: data.late, label: t("att.lateShort") },
    { key: "EXCUSED" as const, n: data.excused, label: t("att.excusedShort") },
  ];

  return (
    <Card className="card-hover p-5">
      <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
        <h3 className="flex items-center gap-1.5 text-[15px] font-semibold tracking-tight text-ink">
          <CalendarCheck className="h-4 w-4 text-brand-600" />{t("att.monthTitle")}
        </h3>
        <Select value={month} onChange={(e) => setMonth(e.target.value)} className="h-9 w-auto py-1.5 text-xs">
          {months.map((m) => <option key={m.value} value={m.value}>{m.label}</option>)}
        </Select>
      </div>

      {data.expected === 0 ? (
        <p className="py-8 text-center text-xs text-muted">{t("att.noSessions")}</p>
      ) : (
        <>
          {/* rate bar */}
          <div className="mb-3">
            <div className="mb-1 flex items-center justify-between text-xs">
              <span className="font-bold text-ink">{Math.round(data.rate)}%</span>
              <span className="text-faint">{t("att.ofSessions", { n: data.expected })}</span>
            </div>
            <div className="flex h-2.5 overflow-hidden rounded-full bg-elevated">
              {counts.map((c) => {
                const w = data.expected ? (c.n / data.expected) * 100 : 0;
                const color =
                  c.key === "PRESENT" ? "bg-emerald-500" :
                  c.key === "LATE" ? "bg-amber-500" :
                  c.key === "ABSENT" ? "bg-rose-500" : "bg-sky-500";
                return <div key={c.key} className={color} style={{ width: `${Math.max(0, w)}%` }} />;
              })}
            </div>
          </div>

          {/* counts */}
          <div className="mb-4 grid grid-cols-4 gap-2">
            {counts.map((c) => {
              const meta = STATUS_META[c.key];
              const Icon = meta.icon;
              return (
                <div key={c.key} className={cn("rounded-xl p-2 text-center ring-1", meta.tone, meta.ring)}>
                  <Icon className="mx-auto mb-0.5 h-4 w-4" />
                  <p className="text-lg font-bold leading-none">{c.n}</p>
                  <p className="mt-0.5 text-[10px] opacity-80">{c.label}</p>
                </div>
              );
            })}
          </div>

          {/* session-by-session history */}
          <div className="max-h-64 space-y-1.5 overflow-y-auto pe-1">
            {data.sessions.map((s, i) => {
              const meta = STATUS_META[s.status];
              const Icon = meta.icon;
              const d = new Date(s.date);
              return (
                <div key={i} className="flex items-center gap-2.5 rounded-lg border border-line px-2.5 py-1.5 text-xs">
                  <div className={cn("flex h-7 w-7 items-center justify-center rounded-lg", meta.tone)}>
                    <Icon className="h-3.5 w-3.5" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="font-medium text-ink">
                      {d.toLocaleDateString(lang === "ar" ? "ar-EG" : undefined, { weekday: "short", day: "numeric", month: "short" })}
                    </p>
                  </div>
                  <span className={cn("rounded-full px-2 py-0.5 text-[10px] font-semibold", meta.tone)}>
                    {s.status === "UNMARKED" ? t("att.unmarked") : t(`att.${s.status.toLowerCase()}`)}
                  </span>
                </div>
              );
            })}
          </div>
          {data.unmarked > 0 && (
            <p className="mt-2 text-center text-[10px] text-faint">{t("att.unmarked")}: {data.unmarked}</p>
          )}
        </>
      )}
    </Card>
  );
}
