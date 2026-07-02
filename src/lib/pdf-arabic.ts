/**
 * Arabic-Supported PDF Generator
 * ================================
 * Uses html2canvas to render HTML content (which properly displays Arabic
 * and RTL text) into images, then embeds them in a jsPDF document.
 * This guarantees perfect Arabic rendering with correct font shaping and RTL.
 */

import { jsPDF } from "jspdf";
import html2canvas from "html2canvas";
import type { DatabaseShape, Student } from "./types";
import {
  attendanceRate, studentAverage, totalPaidFor, balanceDue,
  currencySymbol, formatMoney,
} from "./analytics";
import { monthKey } from "./db";
import { gradeLabel } from "./constants";
import type { Lang } from "../i18n/translations";

/**
 * Generates a student PDF report.
 * Accepts the current language to render in Arabic or English.
 */
export async function generateStudentPdf(db: DatabaseShape, student: Student, lang: Lang = "ar"): Promise<void> {
  const isAr = lang === "ar";
  const sym = currencySymbol(db);
  const fontFamily = isAr ? "'Cairo', 'Tahoma', sans-serif" : "'Inter', sans-serif";

  // Build the report HTML
  const html = buildStudentReportHtml(db, student, isAr, sym, fontFamily);

  // Create a temporary container
  const container = document.createElement("div");
    container.style.cssText = `
    position: fixed; left: -9999px; top: 0; width: 800px; padding: 0;
    background: white; font-family: ${fontFamily};
  `;
  container.innerHTML = html;
  document.body.appendChild(container);

  try {
    // Render to canvas
    const canvas = await html2canvas(container, {
      scale: 2,
      useCORS: true,
      backgroundColor: "#ffffff",
      logging: false,
    });

    // Create PDF from canvas
    const imgData = canvas.toDataURL("image/png");
    const pdf = new jsPDF({ orientation: "portrait", unit: "mm", format: "a4" });
    const pdfWidth = pdf.internal.pageSize.getWidth();
    const pdfHeight = pdf.internal.pageSize.getHeight();
    const imgWidth = pdfWidth;
    const imgHeight = (canvas.height * imgWidth) / canvas.width;

    let heightLeft = imgHeight;
    let position = 0;

    // First page
    pdf.addImage(imgData, "PNG", 0, position, imgWidth, imgHeight);
    heightLeft -= pdfHeight;

    // Additional pages if content is long
    while (heightLeft > 0) {
      position = heightLeft - imgHeight;
      pdf.addPage();
      pdf.addImage(imgData, "PNG", 0, position, imgWidth, imgHeight);
      heightLeft -= pdfHeight;
    }

    pdf.save(`${student.id}_statement.pdf`);
  } finally {
    document.body.removeChild(container);
  }
}

function buildStudentReportHtml(db: DatabaseShape, student: Student, isAr: boolean, sym: string, fontFamily: string): string {
  const attRate = Math.round(attendanceRate(db, {}));
  const avg = studentAverage(db, student.id);
  const paid = totalPaidFor(db, student.id);
  const due = balanceDue(db, student);
  const group = db.groups.find((g) => student.groupIds?.includes(g.id));

  const grades = db.examGrades
    .filter((g) => g.studentId === student.id)
    .map((g) => ({ g, exam: db.exams.find((e) => e.id === g.examId) }))
    .filter((x) => x.exam)
    .slice(-8)
    .reverse();

  const payments = db.payments
    .filter((p) => p.studentId === student.id)
    .sort((a, b) => b.date - a.date)
    .slice(0, 10);

  const L = isAr ? {
    title: "تقرير الطالب",
    name: "الاسم", code: "الكود", grade: "الصف", group: "المجموعة",
    attendance: "نسبة الحضور", avgGrade: "متوسط الدرجات", totalPaid: "إجمالي المدفوع", balanceDue: "الرصيد المستحق",
    liability: "الالتزام", paid: "مدفوع", due: "مستحق",
    transactions: "عمليات الدفع", date: "التاريخ", type: "النوع", amount: "المبلغ", month: "الشهر",
    recentGrades: "أحدث الدرجات", exam: "الامتحان", gradeCol: "الدرجة",
    noData: "لا توجد بيانات",
  } : {
    title: "Student Report",
    name: "Name", code: "Code", grade: "Grade", group: "Group",
    attendance: "Attendance Rate", avgGrade: "Average Grade", totalPaid: "Total Paid", balanceDue: "Balance Due",
    liability: "Liability", paid: "Paid", due: "Due",
    transactions: "Payment Transactions", date: "Date", type: "Type", amount: "Amount", month: "Month",
    recentGrades: "Recent Grades", exam: "Exam", gradeCol: "Grade",
    noData: "No data available",
  };

  return `
  <div style="width: 800px; padding: 40px; background: white; color: #0f172a; font-family: ${fontFamily};">
    <!-- Header -->
    <div style="background: linear-gradient(135deg, #6D5DFC, #4F46E5); color: white; padding: 25px 30px; border-radius: 12px; margin-bottom: 25px;">
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <div>
          <div style="font-size: 22px; font-weight: 800;">${db.profile.name || "Center Plus"}</div>
          <div style="font-size: 13px; opacity: 0.8; margin-top: 4px;">${L.title} · ${new Date().toLocaleDateString(isAr ? "ar-EG" : "en-US")}</div>
        </div>
        <div style="text-align: ${isAr ? "left" : "right"}; opacity: 0.9; font-size: 11px;">
          ${isAr ? "تم الإنشاء بواسطة" : "Generated by"}<br/>Center Plus Desktop
        </div>
      </div>
    </div>

    <!-- Student Info -->
    <div style="margin-bottom: 20px;">
      <div style="font-size: 26px; font-weight: 800; color: #0f172a;">${student.name}</div>
      <div style="font-size: 13px; color: #64748b; margin-top: 4px;">
        ${student.id} · ${gradeLabel(student.grade, isAr ? "ar" : "en")} · ${group?.name ?? "—"}
      </div>
    </div>

    <!-- KPI Cards -->
    <div style="display: flex; gap: 12px; margin-bottom: 25px;">
      ${buildKpi(L.attendance, `${attRate}%`, "#10B981")}
      ${buildKpi(L.avgGrade, avg != null ? `${Math.round(avg)}%` : "—", "#6D5DFC")}
      ${buildKpi(L.totalPaid, formatMoney(paid, sym), "#059669")}
      ${buildKpi(L.balanceDue, formatMoney(due, sym), "#DC2626")}
    </div>

    <!-- Grades Table -->
    <div style="margin-bottom: 25px;">
      <div style="font-size: 16px; font-weight: 700; margin-bottom: 10px; color: #4F46E5; border-bottom: 2px solid #E2E8F0; padding-bottom: 8px;">
        ${L.recentGrades}
      </div>
      ${grades.length === 0 ? `<div style="color: #94A3B8; text-align: center; padding: 20px;">${L.noData}</div>` : `
        <table style="width: 100%; border-collapse: collapse; font-size: 13px;">
          <thead>
            <tr style="background: #F1F5F9;">
              <th style="padding: 8px 12px; text-align: ${isAr ? "right" : "left"}; border-bottom: 1px solid #E2E8F0;">${L.exam}</th>
              <th style="padding: 8px 12px; text-align: center; border-bottom: 1px solid #E2E8F0;">${L.gradeCol}</th>
              <th style="padding: 8px 12px; text-align: center; border-bottom: 1px solid #E2E8F0;">%</th>
            </tr>
          </thead>
          <tbody>
            ${grades.map(({ g, exam }) => {
              const pct = exam!.maxGrade > 0 ? Math.round((g.obtainedGrade / exam!.maxGrade) * 100) : 0;
              return `
              <tr>
                <td style="padding: 8px 12px; border-bottom: 1px solid #F1F5F9;">${exam!.name}</td>
                <td style="padding: 8px 12px; text-align: center; border-bottom: 1px solid #F1F5F9;">${g.obtainedGrade} / ${exam!.maxGrade}</td>
                <td style="padding: 8px 12px; text-align: center; border-bottom: 1px solid #F1F5F9; color: ${pct >= 50 ? "#059669" : "#DC2626"}; font-weight: 700;">${pct}%</td>
              </tr>`;
            }).join("")}
          </tbody>
        </table>
      `}
    </div>

    <!-- Payment Transactions -->
    <div style="margin-bottom: 25px;">
      <div style="font-size: 16px; font-weight: 700; margin-bottom: 10px; color: #4F46E5; border-bottom: 2px solid #E2E8F0; padding-bottom: 8px;">
        ${L.transactions}
      </div>
      ${payments.length === 0 ? `<div style="color: #94A3B8; text-align: center; padding: 20px;">${L.noData}</div>` : `
        <table style="width: 100%; border-collapse: collapse; font-size: 13px;">
          <thead>
            <tr style="background: #F1F5F9;">
              <th style="padding: 8px 12px; text-align: ${isAr ? "right" : "left"}; border-bottom: 1px solid #E2E8F0;">${L.date}</th>
              <th style="padding: 8px 12px; text-align: ${isAr ? "right" : "left"}; border-bottom: 1px solid #E2E8F0;">${L.type}</th>
              <th style="padding: 8px 12px; text-align: center; border-bottom: 1px solid #E2E8F0;">${L.month}</th>
              <th style="padding: 8px 12px; text-align: ${isAr ? "left" : "right"}; border-bottom: 1px solid #E2E8F0;">${L.amount}</th>
            </tr>
          </thead>
          <tbody>
            ${payments.map((p) => `
              <tr>
                <td style="padding: 8px 12px; border-bottom: 1px solid #F1F5F9;">${new Date(p.date).toLocaleDateString(isAr ? "ar-EG" : "en-US")}</td>
                <td style="padding: 8px 12px; border-bottom: 1px solid #F1F5F9;">${p.type.replace(/_/g, " ")}</td>
                <td style="padding: 8px 12px; text-align: center; border-bottom: 1px solid #F1F5F9;">${p.month || monthKey(p.date)}</td>
                <td style="padding: 8px 12px; text-align: ${isAr ? "left" : "right"}; border-bottom: 1px solid #F1F5F9; color: #059669; font-weight: 700;">${formatMoney(p.amount, sym)}</td>
              </tr>
            `).join("")}
          </tbody>
        </table>
      `}
    </div>

    <!-- Footer -->
    <div style="margin-top: 30px; padding-top: 15px; border-top: 1px solid #E2E8F0; text-align: center; font-size: 10px; color: #94A3B8;">
      Center Plus Desktop · /centers/${db.profile.centerId}/students/${student.id}
    </div>
  </div>`;
}

function buildKpi(label: string, value: string, color: string): string {
  return `
    <div style="flex: 1; background: ${color}15; border: 1px solid ${color}30; border-radius: 10px; padding: 12px; text-align: center;">
      <div style="font-size: 11px; color: #64748b; margin-bottom: 4px;">${label}</div>
      <div style="font-size: 20px; font-weight: 800; color: ${color};">${value}</div>
    </div>
  `;
}

/** Backward-compatible export */
export { generateStudentPdf as default };
