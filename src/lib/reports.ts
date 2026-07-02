import { jsPDF } from "jspdf";
import type { DatabaseShape } from "./types";
import {
  monthlyRevenue,
  monthlyExpenses,
  monthlyCenterIncome,
  teacherRevenue,
  teacherCenterShare,
  studentNetFee,
  balanceDue,
  totalPaidFor,
  currencySymbol,
  formatMoney,
} from "./analytics";
import { gradeLabel, subjectLabel } from "./constants";
import type { Lang } from "../i18n/translations";

const BRAND: [number, number, number] = [79, 70, 229];
const DARK: [number, number, number] = [30, 41, 59];
const MUTED: [number, number, number] = [100, 116, 139];
const LINE: [number, number, number] = [226, 232, 240];

/* ----------------------------- shared header ---------------------------- */
function head(doc: jsPDF, db: DatabaseShape, subtitle: string) {
  const W = 210;
  doc.setFillColor(...BRAND);
  doc.rect(0, 0, W, 26, "F");
  doc.setTextColor(255, 255, 255);
  doc.setFont("helvetica", "bold");
  doc.setFontSize(16);
  doc.text(db.profile.name || "Center Plus", 14, 12);
  doc.setFont("helvetica", "normal");
  doc.setFontSize(9);
  doc.text(subtitle, 14, 19);
  doc.text(`Generated ${new Date().toLocaleDateString()}`, W - 14, 19, { align: "right" });
}

function sectionTitle(doc: jsPDF, title: string, y: number): number {
  doc.setFont("helvetica", "bold");
  doc.setFontSize(12);
  doc.setTextColor(...BRAND);
  doc.text(title, 14, y);
  doc.setDrawColor(...LINE);
  doc.line(14, y + 2, 196, y + 2);
  return y + 7;
}

function ensureSpace(doc: jsPDF, y: number): number {
  if (y > 282) {
    doc.addPage();
    return 20;
  }
  return y;
}

/* --------------------------- full center report ------------------------- */
export function exportCenterPdf(db: DatabaseShape, lang: Lang) {
  const sym = currencySymbol(db);
  const doc = new jsPDF();
  head(doc, db, "Full Center Report");
  let y = 34;

  // ---- overview KPIs ----
  y = sectionTitle(doc, "Overview", y);
  const rev = monthlyRevenue(db);
  const exp = monthlyExpenses(db);
  const centerInc = monthlyCenterIncome(db);
  const rows: [string, string][] = [
    ["Total Students", String(db.students.length)],
    ["Teachers", String(db.teachers.length)],
    ["Groups", String(db.groups.length)],
    ["Classrooms", String(db.classrooms.length)],
    ["Monthly Revenue", formatMoney(rev, sym)],
    ["Monthly Expenses", formatMoney(exp, sym)],
    ["Net Profit", formatMoney(rev - exp, sym)],
    ["Center Income (this month)", formatMoney(centerInc, sym)],
  ];
  doc.setFontSize(9);
  for (const [k, v] of rows) {
    doc.setFont("helvetica", "normal");
    doc.setTextColor(...MUTED);
    doc.text(k, 16, y);
    doc.setFont("helvetica", "bold");
    doc.setTextColor(...DARK);
    doc.text(v, 196, y, { align: "right" });
    doc.setDrawColor(...LINE);
    doc.line(16, y + 1.5, 196, y + 1.5);
    y += 6.5;
    y = ensureSpace(doc, y);
  }
  y += 4;

  // ---- teachers ----
  y = ensureSpace(doc, y);
  y = sectionTitle(doc, "Teachers", y);
  doc.setFontSize(8);
  doc.setTextColor(...MUTED);
  doc.setFont("helvetica", "bold");
  doc.text("Name", 16, y); doc.text("Subjects", 70, y); doc.text("Students", 150, y); doc.text("Net", 196, y, { align: "right" });
  y += 4;
  doc.setFont("helvetica", "normal");
  doc.setTextColor(...DARK);
  for (const tc of db.teachers) {
    y = ensureSpace(doc, y);
    const stud = db.students.filter((s) => s.teachers.some((t) => t.teacherId === tc.id)).length;
    const net = teacherRevenue(db, tc.id) - teacherCenterShare(db, tc);
    doc.text(tc.name.slice(0, 28), 16, y);
    doc.text(tc.subjects.map((s) => subjectLabel(s, lang)).join(", ").slice(0, 24), 70, y);
    doc.text(String(stud), 150, y);
    doc.text(formatMoney(net, sym), 196, y, { align: "right" });
    y += 5.5;
  }
  y += 4;

  // ---- students ----
  y = ensureSpace(doc, y);
  y = sectionTitle(doc, "Students", y);
  doc.setFontSize(8);
  doc.setTextColor(...MUTED);
  doc.setFont("helvetica", "bold");
  doc.text("Code", 16, y); doc.text("Name", 40, y); doc.text("Grade", 110, y); doc.text("Fee", 150, y); doc.text("Balance", 196, y, { align: "right" });
  y += 4;
  doc.setFont("helvetica", "normal");
  doc.setTextColor(...DARK);
  for (const s of db.students) {
    y = ensureSpace(doc, y);
    doc.text(s.id, 16, y);
    doc.text(s.name.slice(0, 22), 40, y);
    doc.text(gradeLabel(s.grade, lang).slice(0, 18), 110, y);
    doc.text(formatMoney(studentNetFee(s), sym), 150, y);
    doc.text(formatMoney(balanceDue(db, s), sym), 196, y, { align: "right" });
    y += 5.2;
  }
  y += 4;

  // ---- recent payments ----
  y = ensureSpace(doc, y);
  y = sectionTitle(doc, "Recent Payments", y);
  doc.setFontSize(8);
  doc.setTextColor(...DARK);
  const payments = [...db.payments].sort((a, b) => b.date - a.date).slice(0, 30);
  for (const p of payments) {
    y = ensureSpace(doc, y);
    const st = db.students.find((s) => s.id === p.studentId);
    doc.text(new Date(p.date).toLocaleDateString(), 16, y);
    doc.text((st?.name ?? "—").slice(0, 24), 44, y);
    doc.text(p.type.replace(/_/g, " "), 120, y);
    doc.text(p.month, 160, y);
    doc.setTextColor(16, 185, 129);
    doc.text("+" + formatMoney(p.amount, sym), 196, y, { align: "right" });
    doc.setTextColor(...DARK);
    y += 5.2;
  }

  // footer
  doc.setDrawColor(...LINE);
  doc.line(14, 288, 196, 288);
  doc.setFontSize(7);
  doc.setTextColor(...MUTED);
  doc.text(`/centers/${db.profile.centerId} · Center Plus Desktop`, 14, 292);

  doc.save(`${db.profile.name}_full_report.pdf`);
}

/* --------------------------------- Excel -------------------------------- */
/** Builds a CSV (Excel-compatible) string and triggers a download. */
function downloadCsv(filename: string, rows: (string | number)[][]) {
  const csv = rows
    .map((r) =>
      r
        .map((cell) => {
          const s = String(cell ?? "");
          return /[",\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
        })
        .join(","),
    )
    .join("\n");
  // BOM so Excel reads UTF-8 (Arabic) correctly
  const blob = new Blob(["\uFEFF" + csv], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename.endsWith(".csv") ? filename : `${filename}.csv`;
  a.click();
  URL.revokeObjectURL(url);
}

export function exportCenterExcel(db: DatabaseShape, lang: Lang) {
  const rows: (string | number)[][] = [["Center Plus — Full Export"]];
  rows.push(["Center", db.profile.name, "Currency", db.profile.currency, "Generated", new Date().toLocaleDateString()]);
  rows.push([]);
  // students
  rows.push(["STUDENTS"]);
  rows.push(["Code", "Name", "Grade", "Groups", "Teachers", "Net Fee", "Paid", "Balance", "Exempt"]);
  for (const s of db.students) {
    rows.push([
      s.id, s.name, gradeLabel(s.grade, lang),
      db.groups.filter((g) => s.groupIds.includes(g.id)).map((g) => g.name).join(" | "),
      s.teachers.map((t) => db.teachers.find((x) => x.id === t.teacherId)?.name ?? "—").join(" | "),
      studentNetFee(s), totalPaidFor(db, s.id), balanceDue(db, s), s.isExempt ? "Yes" : "No",
    ]);
  }
  rows.push([]);
  rows.push(["TEACHERS"]);
  rows.push(["Name", "Subjects", "Pay Type", "Rate/Fixed", "Revenue", "Center Share", "Net"]);
  for (const tc of db.teachers) {
    const rev = teacherRevenue(db, tc.id);
    const share = teacherCenterShare(db, tc);
    rows.push([
      tc.name, tc.subjects.map((s) => subjectLabel(s, lang)).join(" | "),
      tc.payType === "percentage" ? `% ${tc.commissionRate}` : `Fixed ${tc.fixedAmount}`,
      rev, share, rev - share,
    ]);
  }
  rows.push([]);
  rows.push(["PAYMENTS"]);
  rows.push(["Date", "Student", "Code", "Type", "Month", "Amount", "Teacher/Center"]);
  for (const p of [...db.payments].sort((a, b) => b.date - a.date)) {
    const st = db.students.find((s) => s.id === p.studentId);
    rows.push([
      new Date(p.date).toLocaleDateString(), st?.name ?? "—", p.studentId, p.type, p.month,
      p.amount, p.forCenter || !p.teacherId ? "Center" : db.teachers.find((t) => t.id === p.teacherId)?.name ?? "—",
    ]);
  }
  rows.push([]);
  rows.push(["EXPENSES"]);
  rows.push(["Date", "Title", "Category", "Amount"]);
  for (const e of [...db.expenses].sort((a, b) => b.date - a.date)) {
    rows.push([new Date(e.date).toLocaleDateString(), e.title, e.category, e.amount]);
  }
  downloadCsv(`${db.profile.name}_full_export`, rows);
}

export function exportStudentsExcel(db: DatabaseShape, lang: Lang) {
  const rows: (string | number)[][] = [
    ["Code", "Name", "Grade", "Parent", "Parent Phone", "Net Fee", "Paid", "Balance", "Exempt", "Registered"],
  ];
  for (const s of db.students) {
    rows.push([
      s.id, s.name, gradeLabel(s.grade, lang), s.parentName ?? "", s.parentPhone ?? "",
      studentNetFee(s), totalPaidFor(db, s.id), balanceDue(db, s), s.isExempt ? "Yes" : "No",
      new Date(s.registrationDate).toLocaleDateString(),
    ]);
  }
  downloadCsv("students", rows);
}

export function exportFinanceExcel(db: DatabaseShape) {
  const rows: (string | number)[][] = [
    ["INCOME — Payments"],
    ["Date", "Student", "Type", "Month", "Amount"],
  ];
  for (const p of [...db.payments].sort((a, b) => a.date - b.date)) {
    const st = db.students.find((s) => s.id === p.studentId);
    rows.push([new Date(p.date).toLocaleDateString(), st?.name ?? "—", p.type, p.month, p.amount]);
  }
  rows.push([], ["EXPENSES"], ["Date", "Title", "Category", "Amount"]);
  for (const e of [...db.expenses].sort((a, b) => a.date - b.date)) {
    rows.push([new Date(e.date).toLocaleDateString(), e.title, e.category, e.amount]);
  }
  const income = db.payments.reduce((s, p) => s + p.amount, 0);
  const outcome = db.expenses.reduce((s, e) => s + e.amount, 0);
  rows.push([], ["Total Income", income], ["Total Expenses", outcome], ["Net", income - outcome]);
  downloadCsv("finance", rows);
}
