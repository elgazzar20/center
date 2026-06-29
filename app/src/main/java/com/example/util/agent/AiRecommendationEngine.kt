package com.example.util.agent

import com.example.data.model.*
import com.example.util.StudentAnalyticsEngine
import com.example.util.StudentEvaluationService
import com.example.util.EvaluationStatus
import java.text.SimpleDateFormat
import java.util.*

data class SmartRecommendation(
    val title: String,
    val description: String,
    val iconName: String, // "alert", "trend_up", "trend_down", "money"
    val severity: String // "high", "medium", "info"
)

object AiRecommendationEngine {

    fun generateRecommendations(
        students: List<Student>,
        attendance: List<Attendance>,
        payments: List<Payment>,
        exams: List<Exam>,
        examGrades: List<ExamGrade>
    ): List<SmartRecommendation> {
        val recommendations = mutableListOf<SmartRecommendation>()
        val activeStudents = students.filter { it.isActive && !it.isArchived }
        if (activeStudents.isEmpty()) return recommendations

        val calendar = Calendar.getInstance()
        val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.US).format(calendar.time)

        // 1. Unpaid students check
        val unpaidCount = activeStudents.count { student ->
            !student.isExempt && payments.none { it.studentId == student.id && it.month == currentMonthStr }
        }
        if (unpaidCount > 0) {
            recommendations.add(
                SmartRecommendation(
                    title = "شؤون مالية متأخرة 💵",
                    description = "هناك $unpaidCount طالباً لم يسددوا اشتراكات الشهر الحالي حتى الآن.",
                    iconName = "money",
                    severity = "high"
                )
            )
        }

        // 2. Attendance warning (overall rate)
        val lastSevenDays = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        val recentAttendance = attendance.filter { it.date >= lastSevenDays }
        if (recentAttendance.isNotEmpty()) {
            val absentCount = recentAttendance.count { it.status.lowercase() == "absent" }
            val totalCount = recentAttendance.size
            val absenceRate = (absentCount.toDouble() / totalCount) * 100.0
            if (absenceRate > 20.0) {
                recommendations.add(
                    SmartRecommendation(
                        title = "تنبيه ارتفاع الغياب 🚨",
                        description = "ارتفعت نسبة الغياب خلال الأسبوع الأخير لتصل إلى ${absenceRate.toInt()}%. ننصح بإرسال تنبيهات جماعية لأولياء الأمور.",
                        iconName = "trend_up",
                        severity = "high"
                    )
                )
            }
        }

        // 3. Find top 1 At Risk student
        var atRiskStudent: Student? = null
        var atRiskReason = ""
        for (student in activeStudents) {
            val analytics = StudentAnalyticsEngine.calculateAnalytics(student, attendance, payments, exams, examGrades)
            val evaluation = StudentEvaluationService.evaluateStudent(analytics, student)
            if (evaluation.status == EvaluationStatus.AT_RISK) {
                atRiskStudent = student
                atRiskReason = evaluation.reason
                break
            }
        }

        if (atRiskStudent != null) {
            recommendations.add(
                SmartRecommendation(
                    title = "متابعة الطالب ${atRiskStudent.name} 🛡️",
                    description = "الطالب معرض للتراجع الدراسي. السبب: $atRiskReason",
                    iconName = "alert",
                    severity = "medium"
                )
            )
        }

        // 4. Low exam scores warning
        if (exams.isNotEmpty() && examGrades.isNotEmpty()) {
            val lastExam = exams.maxByOrNull { it.date }
            if (lastExam != null) {
                val gradesForLast = examGrades.filter { it.examId == lastExam.id }
                if (gradesForLast.isNotEmpty()) {
                    val averageScore = gradesForLast.map { it.score }.average()
                    val successRate = (averageScore / lastExam.totalMarks) * 100.0
                    if (successRate < 60.0) {
                        recommendations.add(
                            SmartRecommendation(
                                title = "تحليل نتائج اختبار [${lastExam.name}] 📝",
                                description = "متوسط درجات الطلاب منخفض نسبيًا (${successRate.toInt()}%). قد تحتاج إلى تخصيص مراجعة سريعة لهذه المادة.",
                                iconName = "trend_down",
                                severity = "medium"
                            )
                        )
                    }
                }
            }
        }

        // Fallback info if list is empty
        if (recommendations.isEmpty()) {
            recommendations.add(
                SmartRecommendation(
                    title = "السنتر في حالة ممتازة ✨",
                    description = "لا توجد مؤشرات خطر حالياً. الحضور سليم والمدفوعات منتظمة والنتائج مستقرة.",
                    iconName = "trend_up",
                    severity = "info"
                )
            )
        }

        return recommendations
    }
}
