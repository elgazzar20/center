package com.example.util

import com.example.data.model.*

enum class EvaluationStatus {
    EXCELLENT,
    NEEDS_ATTENTION,
    AT_RISK
}

data class StudentEvaluation(
    val status: EvaluationStatus,
    val arabicStatusLabel: String,
    val reason: String,
    val colorHex: String
)

object StudentEvaluationService {
    fun evaluateStudent(analytics: StudentAnalytics, student: Student): StudentEvaluation {
        // Check if student has no records (New Student)
        if (analytics.totalAttendanceCount == 0 && analytics.gradedExamsCount == 0) {
            return StudentEvaluation(
                status = EvaluationStatus.EXCELLENT,
                arabicStatusLabel = "طالب جديد",
                reason = "تم تسجيل الطالب حديثاً في النظام وهو قيد التقييم والمتابعة الأكاديمية.",
                colorHex = "#4CAF50"
            )
        }

        // Check for At Risk conditions
        val isAtRisk = (analytics.totalAttendanceCount > 0 && analytics.attendanceRate < 70.0) ||
                (analytics.gradedExamsCount > 0 && analytics.averageGrade < 50.0) ||
                (analytics.unpaidMonthsCount >= 2 && !student.isExempt)

        // Check for Excellent conditions
        val isExcellent = !isAtRisk && 
                (analytics.attendanceRate >= 85.0) &&
                (analytics.gradedExamsCount == 0 || analytics.averageGrade >= 80.0) &&
                (student.isExempt || analytics.paymentCommitmentRate >= 85.0)

        return when {
            isExcellent -> {
                StudentEvaluation(
                    status = EvaluationStatus.EXCELLENT,
                    arabicStatusLabel = "ممتاز",
                    reason = buildString {
                        append("ممتاز بسبب ")
                        val reasons = mutableListOf<String>()
                        if (analytics.totalAttendanceCount > 0) reasons.add("انتظام الحضور بنسبة ${analytics.attendanceRate.toInt()}%")
                        if (analytics.gradedExamsCount > 0) reasons.add("تفوقه الدراسي بمعدل ${analytics.averageGrade.toInt()}%")
                        if (!student.isExempt && analytics.paymentCommitmentRate >= 85.0) reasons.add("الالتزام التام بسداد الرسوم")
                        else if (student.isExempt) reasons.add("الإعفاء المستحق")
                        
                        if (reasons.isEmpty()) {
                            append("السلوك الإيجابي والالتزام العام.")
                        } else {
                            append(reasons.joinToString(" و"))
                            append(".")
                        }
                    },
                    colorHex = "#2E7D32" // Green
                )
            }
            isAtRisk -> {
                StudentEvaluation(
                    status = EvaluationStatus.AT_RISK,
                    arabicStatusLabel = "معرض للتراجع",
                    reason = buildString {
                        append("معرض للتراجع بسبب ")
                        val reasons = mutableListOf<String>()
                        if (analytics.totalAttendanceCount > 0 && analytics.attendanceRate < 70.0) {
                            reasons.add("كثرة الغياب وتدني نسبة الحضور لـ ${analytics.attendanceRate.toInt()}%")
                        }
                        if (analytics.gradedExamsCount > 0 && analytics.averageGrade < 50.0) {
                            reasons.add("انخفاض الدرجات بمتوسط ${analytics.averageGrade.toInt()}%")
                        }
                        if (analytics.unpaidMonthsCount >= 2 && !student.isExempt) {
                            reasons.add("تأخر سداد الرسوم لمدة ${analytics.unpaidMonthsCount} أشهر")
                        }
                        
                        if (reasons.isEmpty()) {
                            append("تراجع الأداء العام واحتياجه لدعم عاجل.")
                        } else {
                            append(reasons.joinToString(" و"))
                            append(".")
                        }
                    },
                    colorHex = "#D32F2F" // Red
                )
            }
            else -> {
                // Needs Attention
                StudentEvaluation(
                    status = EvaluationStatus.NEEDS_ATTENTION,
                    arabicStatusLabel = "يحتاج متابعة",
                    reason = buildString {
                        append("يحتاج متابعة بسبب ")
                        val reasons = mutableListOf<String>()
                        if (analytics.totalAttendanceCount > 0 && analytics.attendanceRate in 70.0..84.9) {
                            reasons.add("بعض الغيابات المتكررة (${analytics.attendanceRate.toInt()}%)")
                        }
                        if (analytics.gradedExamsCount > 0 && analytics.averageGrade in 50.0..79.9) {
                            reasons.add("تذبذب مستوى الدرجات (${analytics.averageGrade.toInt()}%)")
                        }
                        if (analytics.unpaidMonthsCount == 1 && !student.isExempt) {
                            reasons.add("تأخر سداد مصاريف الشهر الحالي")
                        }
                        
                        if (reasons.isEmpty()) {
                            append("تذبذب الأداء وحاجته لمزيد من الجهد والتركيز.")
                        } else {
                            append(reasons.joinToString(" و"))
                            append(".")
                        }
                    },
                    colorHex = "#F57C00" // Orange/Amber
                )
            }
        }
    }
}
