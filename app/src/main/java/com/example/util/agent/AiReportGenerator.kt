package com.example.util.agent

import com.example.data.model.*
import java.text.SimpleDateFormat
import java.util.*

object AiReportGenerator {

    fun generateStudentReport(student: Student, attendance: List<Attendance>, payments: List<Payment>, grades: List<ExamGrade>, exams: List<Exam>, currency: String): AgentResult.ReportResult {
        val studentAttendance = attendance.filter { it.studentId == student.id }
        val studentPayments = payments.filter { it.studentId == student.id }
        val studentGrades = grades.filter { it.studentId == student.id }

        val totalClasses = studentAttendance.size
        val presentCount = studentAttendance.count { it.status.lowercase() == "present" }
        val absentCount = studentAttendance.count { it.status.lowercase() == "absent" }
        val totalPaid = studentPayments.sumOf { it.amount }

        val dataTable = mutableListOf<Map<String, String>>()
        
        // Populate data table with recent activities
        studentAttendance.take(5).forEach { att ->
            val dateStr = SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(att.date))
            val statusAr = when(att.status.lowercase()) {
                "present" -> "حاضر"
                "absent" -> "غائب"
                "late" -> "متأخر"
                else -> "بعذر"
            }
            dataTable.add(
                mapOf(
                    "التاريخ" to dateStr,
                    "البيان" to "تسجيل حالة حضور",
                    "الحالة" to statusAr
                )
            )
        }

        studentPayments.take(5).forEach { pay ->
            val dateStr = SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(pay.date))
            dataTable.add(
                mapOf(
                    "التاريخ" to dateStr,
                    "البيان" to "دفع رسوم شهر ${pay.month}",
                    "الحالة" to "+ ${pay.amount} $currency"
                )
            )
        }

        studentGrades.take(5).forEach { grade ->
            val exam = exams.find { it.id == grade.examId }
            val examName = exam?.name ?: "اختبار"
            val totalMarks = exam?.totalMarks ?: 100.0
            dataTable.add(
                mapOf(
                    "التاريخ" to SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(grade.createdAt)),
                    "البيان" to "اختبار: $examName",
                    "الحالة" to "${grade.score} / $totalMarks"
                )
            )
        }

        val summary = """
            تقرير شامل للطالب: ${student.name}
            الصف الدراسي: ${student.grade.ifBlank { "غير محدد" }}
            نسبة الالتزام بالحضور: ${if (totalClasses > 0) ((presentCount * 100) / totalClasses) else 100}% (حاضر: $presentCount، غائب: $absentCount)
            إجمالي المدفوعات المسجلة: $totalPaid $currency
            عدد الاختبارات المجتازة: ${studentGrades.size} اختبارات.
        """.trimIndent()

        return AgentResult.ReportResult(
            title = "تقرير الطالب: ${student.name} 📝",
            type = "student",
            dataTable = dataTable.sortedByDescending { it["التاريخ"] },
            summaryText = summary,
            pdfGenerated = true,
            pdfUri = "content://com.example.nexora/reports/pdf/student_${student.id}.pdf"
        )
    }

    fun generateGroupReport(group: Group, students: List<Student>, attendance: List<Attendance>): AgentResult.ReportResult {
        val groupStudents = students.filter { it.groupId == group.id || it.customCourse.contains(group.name) }
        val dataTable = groupStudents.mapIndexed { idx, student ->
            val studentAttendance = attendance.filter { it.studentId == student.id }
            val presentCount = studentAttendance.count { it.status.lowercase() == "present" }
            val absentCount = studentAttendance.count { it.status.lowercase() == "absent" }
            val rate = if (studentAttendance.isNotEmpty()) "${((presentCount * 100) / studentAttendance.size)}%" else "100%"

            mapOf(
                "م" to (idx + 1).toString(),
                "اسم الطالب" to student.name,
                "الهاتف" to student.studentPhone.ifBlank { student.parentPhone },
                "الالتزام" to rate,
                "الغياب" to "$absentCount غيابات"
            )
        }

        val summary = """
            تقرير المجموعة: ${group.name}
            مدرس المجموعة: ${group.teacherName.ifBlank { "المشرف" }}
            القاعة/المكان: ${group.classroom.ifBlank { "غير محدد" }}
            عدد الطلاب المسجلين: ${groupStudents.size} طالباً وطالبة.
        """.trimIndent()

        return AgentResult.ReportResult(
            title = "كشف طلاب مجموعة [${group.name}] 👥",
            type = "group",
            dataTable = dataTable,
            summaryText = summary,
            pdfGenerated = true,
            pdfUri = "content://com.example.nexora/reports/pdf/group_${group.id}.pdf"
        )
    }

    fun generateAttendanceReport(attendance: List<Attendance>, students: List<Student>): AgentResult.ReportResult {
        val calendar = Calendar.getInstance()
        val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.US).format(calendar.time)
        val currentMonthAr = SimpleDateFormat("MMMM yyyy", Locale("ar")).format(calendar.time)

        val monthlyRecords = attendance.filter { it.month == currentMonthStr }
        val groupedRecords = monthlyRecords.groupBy { it.studentId }

        val dataTable = groupedRecords.mapNotNull { (studentId, list) ->
            val student = students.find { it.id == studentId } ?: return@mapNotNull null
            val present = list.count { it.status.lowercase() == "present" }
            val absent = list.count { it.status.lowercase() == "absent" }
            val late = list.count { it.status.lowercase() == "late" }

            mapOf(
                "اسم الطالب" to student.name,
                "حاضر" to present.toString(),
                "متأخر" to late.toString(),
                "غائب" to absent.toString()
            )
        }

        val totalPresent = monthlyRecords.count { it.status.lowercase() == "present" }
        val totalAbsent = monthlyRecords.count { it.status.lowercase() == "absent" }
        val rate = if (monthlyRecords.isNotEmpty()) "${((totalPresent + monthlyRecords.count { it.status.lowercase() == "late" }) * 100) / monthlyRecords.size}%" else "100%"

        val summary = """
            تقرير الحضور والغياب لشهر: $currentMonthAr
            إجمالي حالات تسجيل الحضور: $totalPresent
            إجمالي حالات تسجيل الغياب: $totalAbsent
            متوسط التزام الحضور الإجمالي: $rate
        """.trimIndent()

        return AgentResult.ReportResult(
            title = "تقرير الحضور الشهري لشهر $currentMonthAr 📊",
            type = "attendance",
            dataTable = dataTable,
            summaryText = summary,
            pdfGenerated = true,
            pdfUri = "content://com.example.nexora/reports/pdf/attendance_$currentMonthStr.pdf"
        )
    }

    fun generateFinancialReport(payments: List<Payment>, expenses: List<Expense>, currency: String): AgentResult.ReportResult {
        val calendar = Calendar.getInstance()
        val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.US).format(calendar.time)
        val currentMonthAr = SimpleDateFormat("MMMM yyyy", Locale("ar")).format(calendar.time)

        val monthlyRevenues = payments.filter { it.month == currentMonthStr }.sumOf { it.amount }
        val monthlyExpenses = expenses.filter { it.month == currentMonthStr }.sumOf { it.amount }
        val netProfit = monthlyRevenues - monthlyExpenses

        val dataTable = mutableListOf<Map<String, String>>()
        
        // Add revenues summary
        dataTable.add(
            mapOf(
                "البند" to "إيرادات واشتراكات الطلاب",
                "النوع" to "دخل (+)",
                "القيمة" to "$monthlyRevenues $currency"
            )
        )

        // Add expenses breakdown
        expenses.filter { it.month == currentMonthStr }.forEach { exp ->
            dataTable.add(
                mapOf(
                    "البند" to exp.title.ifBlank { exp.category },
                    "النوع" to "مصروفات (-)",
                    "القيمة" to "${exp.amount} $currency"
                )
            )
        }

        val summary = """
            التقرير المالي لشهر: $currentMonthAr
            إجمالي الإيرادات المقبوضة: $monthlyRevenues $currency
            إجمالي المصروفات المدفوعة: $monthlyExpenses $currency
            صافي الأرباح المحققة: $netProfit $currency
        """.trimIndent()

        return AgentResult.ReportResult(
            title = "تقرير الأرباح والمصروفات لشهر $currentMonthAr 💰",
            type = "financial",
            dataTable = dataTable,
            summaryText = summary,
            pdfGenerated = true,
            pdfUri = "content://com.example.nexora/reports/pdf/financial_$currentMonthStr.pdf"
        )
    }
}
