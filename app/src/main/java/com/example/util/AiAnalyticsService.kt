package com.example.util

import com.example.data.model.*
import java.text.SimpleDateFormat
import java.util.*

sealed class AiResult {
    data class TextResult(
        val text: String
    ) : AiResult()

    data class StudentsListResult(
        val title: String,
        val students: List<StudentItem>,
        val subtitle: String = ""
    ) : AiResult()

    data class RevenueResult(
        val title: String,
        val totalRevenue: Double,
        val totalExpenses: Double,
        val netProfit: Double,
        val monthName: String,
        val currency: String,
        val details: String = ""
    ) : AiResult()

    data class AttendanceResult(
        val title: String,
        val percentage: Double,
        val presentCount: Int,
        val absentCount: Int,
        val lateCount: Int,
        val excusedCount: Int,
        val totalCount: Int,
        val period: String
    ) : AiResult()
}

data class StudentItem(
    val id: String,
    val name: String,
    val detail: String,
    val subDetail: String = "",
    val parentPhone: String = "",
    val studentPhone: String = "",
    val grade: String = ""
)

object AiAnalyticsService {

    fun executeQuery(
        params: QueryParams,
        students: List<Student>,
        attendance: List<Attendance>,
        payments: List<Payment>,
        exams: List<Exam>,
        examGrades: List<ExamGrade>,
        expenses: List<Expense>,
        currency: String = "ج.م"
    ): AiResult {
        return when (params.intent) {
            AiIntent.MOST_ABSENT -> analyzeMostAbsent(students, attendance)
            AiIntent.REVENUE_BY_MONTH -> analyzeRevenueByMonth(payments, expenses, params.monthNumber, params.yearString, params.monthNameAr, currency)
            AiIntent.LATE_PAYMENTS -> analyzeLatePayments(students, payments, currency)
            AiIntent.TOP_PERFORMING_STUDENTS -> analyzeTopPerforming(students, exams, examGrades)
            AiIntent.ATTENDANCE_RATE -> analyzeAttendanceRate(attendance)
            AiIntent.UNKNOWN -> AiResult.TextResult(
                "عذراً، لم أفهم استفسارك بدقة. 🤖\n\nيمكنك تجربة أحد الأسئلة المقترحة بالأسفل مثل:\n" +
                "• من أكثر الطلاب غياباً هذا الشهر؟\n" +
                "• كم الإيرادات في مايو؟\n" +
                "• اعرض الطلاب المتأخرين في الدفع.\n" +
                "• من أفضل الطلاب أداءً؟\n" +
                "• ما نسبة الحضور هذا الأسبوع؟"
            )
        }
    }

    private fun analyzeMostAbsent(students: List<Student>, attendance: List<Attendance>): AiResult {
        val calendar = Calendar.getInstance()
        val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.US).format(calendar.time)

        // Filter absences for the current month
        var monthlyAbsences = attendance.filter { 
            it.status.lowercase() == "absent" && it.month == currentMonthStr 
        }

        // If no absences found for current month, check overall history
        val isOverall = monthlyAbsences.isEmpty()
        if (isOverall) {
            monthlyAbsences = attendance.filter { it.status.lowercase() == "absent" }
        }

        if (monthlyAbsences.isEmpty()) {
            return AiResult.TextResult("ممتاز! لا يوجد أي حالات غياب مسجلة للطلاب حتى الآن. جميع الطلاب ملتزمون بالحضور! 🌟👏")
        }

        val absenceCounts = monthlyAbsences.groupBy { it.studentId }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(5) // Top 5 absent students

        val studentItems = absenceCounts.mapNotNull { (studentId, count) ->
            val student = students.find { it.id == studentId }
            if (student != null) {
                StudentItem(
                    id = student.id,
                    name = student.name,
                    detail = "غائب $count ${if (count > 10) "مرة" else "مرات"}",
                    subDetail = "الصف: ${student.grade.ifBlank { "غير محدد" }}",
                    parentPhone = student.parentPhone,
                    studentPhone = student.studentPhone,
                    grade = student.grade
                )
            } else null
        }

        val periodText = if (isOverall) "إجمالي غيابات الطلاب" else "خلال هذا الشهر (${SimpleDateFormat("MMMM", Locale("ar")).format(calendar.time)})"
        return AiResult.StudentsListResult(
            title = "أكثر الطلاب غياباً 🚨",
            subtitle = "الطلاب الأكثر تسجيلاً لحالات الغياب $periodText:",
            students = studentItems
        )
    }

    private fun analyzeRevenueByMonth(
        payments: List<Payment>,
        expenses: List<Expense>,
        monthNum: String?,
        yearStr: String?,
        monthName: String?,
        currency: String
    ): AiResult {
        val calendar = Calendar.getInstance()
        val targetMonth = monthNum ?: String.format(Locale.US, "%02d", calendar.get(Calendar.MONTH) + 1)
        val targetYear = yearStr ?: calendar.get(Calendar.YEAR).toString()
        val monthCode = "$targetYear-$targetMonth"

        val monthlyRevenues = payments.filter { it.month == monthCode }.sumOf { it.amount }
        val monthlyExpenses = expenses.filter { it.month == monthCode }.sumOf { it.amount }
        val netProfit = monthlyRevenues - monthlyExpenses

        val arabicMonthName = monthName ?: when (targetMonth) {
            "01" -> "يناير"
            "02" -> "فبراير"
            "03" -> "مارس"
            "04" -> "أبريل"
            "05" -> "مايو"
            "06" -> "يونيو"
            "07" -> "يوليو"
            "08" -> "أغسطس"
            "09" -> "سبتمبر"
            "10" -> "أكتوبر"
            "11" -> "نوفمبر"
            "12" -> "ديسمبر"
            else -> "هذا الشهر"
        }

        return AiResult.RevenueResult(
            title = "تقرير الإيرادات لشهر $arabicMonthName $targetYear 💰",
            totalRevenue = monthlyRevenues,
            totalExpenses = monthlyExpenses,
            netProfit = netProfit,
            monthName = arabicMonthName,
            currency = currency,
            details = "تحليل الإيرادات والربح الصافي بناءً على العمليات المسجلة محلياً في هذا الشهر."
        )
    }

    private fun analyzeLatePayments(students: List<Student>, payments: List<Payment>, currency: String): AiResult {
        val calendar = Calendar.getInstance()
        val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.US).format(calendar.time)

        // Filter active non-exempt students who haven't paid this month
        val unpaidStudents = students.filter { student ->
            student.isActive && !student.isArchived && !student.isExempt &&
                    payments.none { it.studentId == student.id && it.month == currentMonthStr }
        }

        if (unpaidStudents.isEmpty()) {
            return AiResult.TextResult("رائع! جميع الطلاب النشطين قاموا بسداد اشتراكات هذا الشهر بالكامل! 💯💵")
        }

        val studentItems = unpaidStudents.map { student ->
            StudentItem(
                id = student.id,
                name = student.name,
                detail = "المبلغ المطلوب: ${student.netFee} $currency",
                subDetail = "الصف: ${student.grade.ifBlank { "غير محدد" }}",
                parentPhone = student.parentPhone,
                studentPhone = student.studentPhone,
                grade = student.grade
            )
        }

        val currentMonthName = SimpleDateFormat("MMMM", Locale("ar")).format(calendar.time)
        return AiResult.StudentsListResult(
            title = "الطلاب المتأخرون في سداد اشتراك $currentMonthName ⏳",
            subtitle = "قائمة الطلاب النشطين الذين لم يسددوا اشتراك هذا الشهر حتى الآن:",
            students = studentItems
        )
    }

    private fun analyzeTopPerforming(
        students: List<Student>,
        exams: List<Exam>,
        examGrades: List<ExamGrade>
    ): AiResult {
        if (exams.isEmpty() || examGrades.isEmpty()) {
            return AiResult.TextResult("لم يتم العثور على اختبارات أو درجات مسجلة في التطبيق حتى الآن لحساب أداء الطلاب. 📝")
        }

        // Calculate average grade percentage for each student
        val studentsPerformance = students.mapNotNull { student ->
            val grades = examGrades.filter { it.studentId == student.id }
            if (grades.isEmpty()) null
            else {
                var totalPct = 0.0
                var examCount = 0
                for (g in grades) {
                    val ex = exams.find { it.id == g.examId }
                    if (ex != null && ex.totalMarks > 0) {
                        totalPct += (g.score / ex.totalMarks) * 100.0
                        examCount++
                    }
                }
                if (examCount > 0) {
                    val averagePct = totalPct / examCount
                    student to averagePct
                } else null
            }
        }.sortedByDescending { it.second }.take(5)

        if (studentsPerformance.isEmpty()) {
            return AiResult.TextResult("لا توجد بيانات اختبارات كافية لحساب متوسطات درجات الطلاب حالياً.")
        }

        val studentItems = studentsPerformance.map { (student, averagePct) ->
            val formattedPct = String.format(Locale.US, "%.1f", averagePct)
            StudentItem(
                id = student.id,
                name = student.name,
                detail = "متوسط الدرجات: $formattedPct%",
                subDetail = "الصف: ${student.grade.ifBlank { "غير محدد" }}",
                parentPhone = student.parentPhone,
                studentPhone = student.studentPhone,
                grade = student.grade
            )
        }

        return AiResult.StudentsListResult(
            title = "أفضل الطلاب أداءً في الاختبارات 🏆",
            subtitle = "أفضل 5 طلاب أداءً بناءً على متوسط درجاتهم في جميع الاختبارات:",
            students = studentItems
        )
    }

    private fun analyzeAttendanceRate(attendance: List<Attendance>): AiResult {
        val calendar = Calendar.getInstance()
        
        // Let's filter for the last 7 days
        val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        var recentAttendance = attendance.filter { it.date >= sevenDaysAgo }
        var isThisWeek = true

        // Fallback to current month if no records found for the past 7 days
        if (recentAttendance.isEmpty()) {
            val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.US).format(calendar.time)
            recentAttendance = attendance.filter { it.month == currentMonthStr }
            isThisWeek = false
        }

        if (recentAttendance.isEmpty()) {
            return AiResult.TextResult("لم يتم تسجيل أي عمليات حضور أو غياب مؤخراً لحساب النسبة. 📊")
        }

        val total = recentAttendance.size
        val present = recentAttendance.count { it.status.lowercase() == "present" }
        val absent = recentAttendance.count { it.status.lowercase() == "absent" }
        val late = recentAttendance.count { it.status.lowercase() == "late" }
        val excused = recentAttendance.count { it.status.lowercase() == "excused" }

        // M3 style definition: Attendance rate = (Present + Late) / Total
        val rate = ((present + late).toDouble() / total) * 100.0

        val periodText = if (isThisWeek) "خلال السبعة أيام الأخيرة" else "خلال هذا الشهر"

        return AiResult.AttendanceResult(
            title = "معدل الحضور والالتزام 📊",
            percentage = rate,
            presentCount = present,
            absentCount = absent,
            lateCount = late,
            excusedCount = excused,
            totalCount = total,
            period = periodText
        )
    }
}
