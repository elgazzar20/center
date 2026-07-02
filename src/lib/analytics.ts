import type {
  DatabaseShape,
  Student,
  Teacher,
  Exam,
  AttendanceStatus,
  DayOfWeek,
  Payment,
} from "./types";
import { monthKey, startOfDay, addDays, now } from "./db";
import { currencySymbolOf } from "./constants";

/** Mirrors Kotlin's coerceAtLeast(0f) — never let a chart dimension go negative. */
export const clampNonNegative = (n: number) => Math.max(0, n);

/* ------------------------------- students ------------------------------- */
export function studentInGroup(s: Student, groupId: string): boolean {
  return s.groupIds?.includes(groupId) ?? false;
}

/** Sum of all teacher fees, minus the discount. */
export function studentNetFee(s: Student): number {
  if (s.isExempt) return 0;
  const total = (s.teachers ?? []).reduce((sum, t) => sum + (t.fee || 0), 0);
  return Math.max(0, total - (s.discount || 0));
}

export function studentTeacherIds(s: Student): string[] {
  return (s.teachers ?? []).map((t) => t.teacherId);
}

export function monthsSince(ts: number, ref = now()): number {
  const diff = Math.max(0, ref - ts);
  return Math.max(1, Math.round(diff / (30 * 86_400_000)));
}

/** Months the student is liable for since registration. */
export function liabilityMonths(student: Student): number {
  if (student.isExempt || studentNetFee(student) <= 0) return 0;
  return monthsSince(student.registrationDate);
}

export function totalPaidFor(db: DatabaseShape, studentId: string): number {
  return db.payments
    .filter((p) => p.studentId === studentId)
    .reduce((sum, p) => sum + p.amount, 0);
}

/** Amount paid allocated to a specific teacher for a student. */
export function paidForTeacher(
  db: DatabaseShape,
  studentId: string,
  teacherId: string,
): number {
  return db.payments
    .filter((p) => p.studentId === studentId && p.teacherId === teacherId)
    .reduce((sum, p) => sum + p.amount, 0);
}

/**
 * Balance due = sum over each teacher of (months liable * teacher.fee - paid).
 * Falls back to whole-student paid total when payments aren't teacher-tagged.
 */
export function balanceDue(db: DatabaseShape, student: Student): number {
  if (student.isExempt) return 0;
  const months = liabilityMonths(student);
  if (months <= 0) return 0;

  let due = 0;
  for (const t of student.teachers ?? []) {
    const liable = months * (t.fee || 0);
    const paid = paidForTeacher(db, student.id, t.teacherId);
    due += Math.max(0, liable - paid);
  }
  // subtract discount once for the current month
  due = Math.max(0, due - student.discount);
  return due;
}

/* -------------------------------- finance ------------------------------- */
export function monthlyRevenue(db: DatabaseShape, ref = now()): number {
  const key = monthKey(ref);
  return db.payments
    .filter((p) => p.month === key)
    .reduce((s, p) => s + p.amount, 0);
}

export function monthlyExpenses(db: DatabaseShape, ref = now()): number {
  const key = monthKey(ref);
  return db.expenses
    .filter((e) => monthKey(e.date) === key)
    .reduce((s, e) => s + e.amount, 0);
}

/** Center income = teacher commission/fees + center subscription payments. */
export function monthlyCenterIncome(db: DatabaseShape, ref = now()): number {
  const key = monthKey(ref);
  return db.payments
    .filter((p) => p.month === key)
    .reduce((sum, p) => {
      if (p.forCenter || !p.teacherId) return sum + p.amount;
      const t = db.teachers.find((x) => x.id === p.teacherId);
      if (!t) return sum + p.amount;
      return sum + centerShareOf(t, p.amount);
    }, 0);
}

export interface MonthPoint {
  month: string;
  revenue: number;
  expenses: number;
}

export function monthlySeries(db: DatabaseShape, count = 6): MonthPoint[] {
  const points: MonthPoint[] = [];
  for (let i = count - 1; i >= 0; i--) {
    const ref = addDays(startOfDay(now()), -i * 30);
    const key = monthKey(ref);
    points.push({
      month: key,
      revenue: db.payments
        .filter((p) => p.month === key)
        .reduce((s, p) => s + p.amount, 0),
      expenses: db.expenses
        .filter((e) => monthKey(e.date) === key)
        .reduce((s, e) => s + e.amount, 0),
    });
  }
  return points;
}

/* -------------------------------- teachers ------------------------------ */
/** Total fees collected on behalf of a teacher (across all students). */
export function teacherRevenue(db: DatabaseShape, teacherId: string): number {
  return db.payments
    .filter((p) => p.teacherId === teacherId)
    .reduce((s, p) => s + p.amount, 0);
}

export function teacherRevenueThisMonth(
  db: DatabaseShape,
  teacherId: string,
  ref = now(),
): number {
  const key = monthKey(ref);
  return db.payments
    .filter((p) => p.teacherId === teacherId && p.month === key)
    .reduce((s, p) => s + p.amount, 0);
}

/** How much the center earns from a single payment of `amount`. */
export function centerShareOf(teacher: Teacher, amount: number): number {
  if (teacher.payType === "fixed") return Math.min(amount, teacher.fixedAmount);
  return (amount * (teacher.commissionRate || 0)) / 100;
}

export function teacherCenterShare(db: DatabaseShape, teacher: Teacher): number {
  const rev = teacherRevenue(db, teacher.id);
  if (teacher.payType === "fixed") return teacher.fixedAmount;
  return (rev * (teacher.commissionRate || 0)) / 100;
}

export function teacherNet(db: DatabaseShape, teacher: Teacher): number {
  const rev = teacherRevenue(db, teacher.id);
  return rev - teacherCenterShare(db, teacher);
}

export function studentsOfTeacher(db: DatabaseShape, teacherId: string): Student[] {
  return db.students.filter((s) => studentTeacherIds(s).includes(teacherId));
}

export function groupsOfTeacher(db: DatabaseShape, teacherId: string) {
  return db.groups.filter((g) => g.teacherId === teacherId);
}

/* ------------------------------ attendance ------------------------------ */
const PRESENT_LIKE: AttendanceStatus[] = ["PRESENT", "LATE"];

export type SessionStatus = AttendanceStatus | "UNMARKED";

export interface MonthSession {
  date: number;
  status: SessionStatus;
}

export interface MonthAttendance {
  month: string; // "yyyy-MM"
  expected: number;
  present: number;
  late: number;
  absent: number;
  excused: number;
  unmarked: number;
  /** attendance rate based only on recorded sessions */
  rate: number;
  sessions: MonthSession[];
}

/**
 * Computes a student's attendance for a given month, driven by the meeting
 * days of their groups. A student is NEVER counted absent on a day their
 * group doesn't meet. Expected sessions with no record are flagged UNMARKED.
 */
export function studentMonthAttendance(
  db: DatabaseShape,
  studentId: string,
  month: string,
): MonthAttendance {
  const student = db.students.find((s) => s.id === studentId);
  const groups = (student?.groupIds ?? [])
    .map((id) => db.groups.find((g) => g.id === id))
    .filter(Boolean) as { days?: DayOfWeek[] }[];

  // union of meeting days across the student's groups
  const daySet = new Set<number>();
  for (const g of groups) for (const d of g.days ?? []) daySet.add(d);

  const [yy, mm] = month.split("-").map(Number);
  const daysInMonth = new Date(yy, mm, 0).getDate();
  const toDow = (ts: number) => {
    const js = new Date(ts).getDay(); // 0=Sun
    return js === 0 ? 7 : js;
  };

  // expected dates (meeting days in the month)
  const expectedDates: number[] = [];
  if (daySet.size > 0) {
    for (let d = 1; d <= daysInMonth; d++) {
      const ts = startOfDay(new Date(yy, mm - 1, d).getTime());
      if (daySet.has(toDow(ts))) expectedDates.push(ts);
    }
  }

  // student's records for that month, keyed by date
  const records = db.attendance.filter(
    (a) => a.studentId === studentId && monthKey(a.date) === month,
  );
  const byDate = new Map<number, AttendanceStatus>();
  for (const r of records) byDate.set(r.date, r.status);

  // if no meeting days are defined, fall back to the actual record dates
  if (expectedDates.length === 0) {
    const set = new Set(records.map((r) => r.date));
    for (const ts of set) expectedDates.push(ts);
    expectedDates.sort((a, b) => a - b);
  }

  let present = 0, late = 0, absent = 0, excused = 0, unmarked = 0;
  const sessions: MonthSession[] = expectedDates.map((date) => {
    const status = byDate.get(date);
    if (!status) { unmarked++; return { date, status: "UNMARKED" }; }
    if (status === "PRESENT") present++;
    else if (status === "LATE") late++;
    else if (status === "ABSENT") absent++;
    else if (status === "EXCUSED") excused++;
    return { date, status };
  });

  const recorded = present + late + absent + excused;
  const rate = recorded ? ((present + late) / recorded) * 100 : 0;

  return {
    month,
    expected: expectedDates.length,
    present, late, absent, excused, unmarked, rate,
    sessions: sessions.sort((a, b) => b.date - a.date),
  };
}

export function attendanceRate(
  db: DatabaseShape,
  opts: { groupId?: string; days?: number } = {},
): number {
  const { groupId, days } = opts;
  let records = db.attendance;
  if (groupId) records = records.filter((r) => r.groupId === groupId);
  if (days) {
    const cutoff = startOfDay(addDays(now(), -days));
    records = records.filter((r) => r.date >= cutoff);
  }
  if (records.length === 0) return 0;
  const present = records.filter((r) => PRESENT_LIKE.includes(r.status)).length;
  return (present / records.length) * 100;
}

export interface DayTrend {
  label: string;
  rate: number;
}

export function attendanceTrend(db: DatabaseShape, days = 14): DayTrend[] {
  const out: DayTrend[] = [];
  for (let i = days - 1; i >= 0; i--) {
    const day = startOfDay(addDays(now(), -i));
    const recs = db.attendance.filter((r) => r.date === day);
    const present = recs.filter((r) => PRESENT_LIKE.includes(r.status)).length;
    const rate = recs.length ? (present / recs.length) * 100 : 0;
    const d = new Date(day);
    out.push({ label: `${d.getDate()}/${d.getMonth() + 1}`, rate });
  }
  return out;
}

/* -------------------------------- grades -------------------------------- */
export interface GradeBucket {
  label: string;
  count: number;
}

export function gradeDistribution(db: DatabaseShape): GradeBucket[] {
  const buckets = [
    { label: "<50%", min: 0, max: 0.5, count: 0 },
    { label: "50-65%", min: 0.5, max: 0.65, count: 0 },
    { label: "65-80%", min: 0.65, max: 0.8, count: 0 },
    { label: "80-90%", min: 0.8, max: 0.9, count: 0 },
    { label: "90%+", min: 0.9, max: 1.01, count: 0 },
  ];
  for (const g of db.examGrades) {
    const exam = db.exams.find((e) => e.id === g.examId);
    if (!exam || exam.maxGrade <= 0) continue;
    const ratio = g.obtainedGrade / exam.maxGrade;
    const b = buckets.find((b) => ratio >= b.min && ratio < b.max);
    if (b) b.count++;
  }
  return buckets.map((b) => ({ label: b.label, count: b.count }));
}

export function studentAverage(db: DatabaseShape, studentId: string): number | null {
  const grades = db.examGrades.filter((g) => g.studentId === studentId);
  if (!grades.length) return null;
  let sumRatio = 0;
  let counted = 0;
  for (const g of grades) {
    const exam = db.exams.find((e) => e.id === g.examId);
    if (!exam || exam.maxGrade <= 0) continue;
    sumRatio += g.obtainedGrade / exam.maxGrade;
    counted++;
  }
  return counted ? (sumRatio / counted) * 100 : null;
}

export function examAverage(db: DatabaseShape, exam: Exam): number | null {
  const grades = db.examGrades.filter((g) => g.examId === exam.id);
  if (!grades.length || exam.maxGrade <= 0) return null;
  const sum = grades.reduce((s, g) => s + g.obtainedGrade / exam.maxGrade, 0);
  return (sum / grades.length) * 100;
}

/* ------------------------------- currency ------------------------------- */
export function currencySymbol(db: DatabaseShape): string {
  return currencySymbolOf(db.profile.currency);
}

export function formatMoney(amount: number, symbol = "$"): string {
  return `${symbol}${Math.round(amount).toLocaleString()}`;
}

/** Human label for a payment (teacher or center subscription). */
export function paymentTarget(db: DatabaseShape, p: Payment): string {
  if (p.forCenter || !p.teacherId) return "Center";
  return db.teachers.find((t) => t.id === p.teacherId)?.name ?? "Center";
}
