package com.example.util

import com.example.data.model.*
import java.text.SimpleDateFormat
import java.util.*

data class StudentAnalytics(
    val studentId: String,
    val attendanceRate: Double, // 0.0 to 100.0
    val absenceRate: Double,    // 0.0 to 100.0
    val averageGrade: Double,   // 0.0 to 100.0
    val paymentCommitmentRate: Double, // 0.0 to 100.0
    val totalAttendanceCount: Int,
    val presentCount: Int,
    val absentCount: Int,
    val lateCount: Int,
    val excusedCount: Int,
    val totalExamsCount: Int,
    val gradedExamsCount: Int,
    val totalPaymentsAmount: Double,
    val unpaidMonthsCount: Int
)

object StudentAnalyticsEngine {
    fun calculateAnalytics(
        student: Student,
        allAttendance: List<Attendance>,
        allPayments: List<Payment>,
        allExams: List<Exam>,
        allExamGrades: List<ExamGrade>
    ): StudentAnalytics {
        // 1. Attendance & Absence Rates
        val studentAttendance = allAttendance.filter { it.studentId == student.id }
        val totalAttendanceCount = studentAttendance.size
        
        val presentCount = studentAttendance.count { it.status.lowercase() == "present" }
        val absentCount = studentAttendance.count { it.status.lowercase() == "absent" }
        val lateCount = studentAttendance.count { it.status.lowercase() == "late" }
        val excusedCount = studentAttendance.count { it.status.lowercase() == "excused" }
        
        val attendanceRate = if (totalAttendanceCount > 0) {
            ((presentCount + lateCount).toDouble() / totalAttendanceCount) * 100.0
        } else {
            100.0
        }
        
        val absenceRate = if (totalAttendanceCount > 0) {
            (absentCount.toDouble() / totalAttendanceCount) * 100.0
        } else {
            0.0
        }

        // 2. Average Grade
        val studentGrades = allExamGrades.filter { it.studentId == student.id }
        val gradedExamsCount = studentGrades.size
        
        var totalGradePercentage = 0.0
        var matchingExamsCount = 0
        
        for (grade in studentGrades) {
            val exam = allExams.find { it.id == grade.examId }
            if (exam != null && exam.totalMarks > 0) {
                val percentage = (grade.score / exam.totalMarks) * 100.0
                totalGradePercentage += percentage
                matchingExamsCount++
            }
        }
        
        val averageGrade = if (matchingExamsCount > 0) {
            totalGradePercentage / matchingExamsCount
        } else {
            0.0
        }

        // 3. Payment Commitment Rate
        val paymentCommitmentRate: Double
        val unpaidMonthsCount: Int
        if (student.isExempt) {
            paymentCommitmentRate = 100.0
            unpaidMonthsCount = 0
        } else {
            val regDate = if (student.registrationDate < 1577836800000L) { // Jan 1st 2020
                System.currentTimeMillis()
            } else {
                student.registrationDate
            }
            val enrolledMonths = getEnrolledMonths(regDate)
            val studentPayments = allPayments.filter { it.studentId == student.id }
            val paidMonths = studentPayments.map { it.month }.toSet()
            
            val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
            val previousEnrolledMonths = enrolledMonths.filter { it != currentMonthStr }
            
            var paidCount = 0
            var unpaidCount = 0
            for (month in enrolledMonths) {
                if (paidMonths.contains(month)) {
                    paidCount++
                } else if (month != currentMonthStr) {
                    unpaidCount++
                }
            }
            
            unpaidMonthsCount = unpaidCount
            
            val previousPaidCount = previousEnrolledMonths.count { paidMonths.contains(it) }
            paymentCommitmentRate = if (previousEnrolledMonths.isNotEmpty()) {
                (previousPaidCount.toDouble() / previousEnrolledMonths.size) * 100.0
            } else {
                100.0
            }
        }
        
        val totalPaymentsAmount = allPayments.filter { it.studentId == student.id }.sumOf { it.amount }

        return StudentAnalytics(
            studentId = student.id,
            attendanceRate = attendanceRate,
            absenceRate = absenceRate,
            averageGrade = averageGrade,
            paymentCommitmentRate = paymentCommitmentRate,
            totalAttendanceCount = totalAttendanceCount,
            presentCount = presentCount,
            absentCount = absentCount,
            lateCount = lateCount,
            excusedCount = excusedCount,
            totalExamsCount = allExams.size,
            gradedExamsCount = gradedExamsCount,
            totalPaymentsAmount = totalPaymentsAmount,
            unpaidMonthsCount = unpaidMonthsCount
        )
    }

    private fun getEnrolledMonths(registrationDate: Long): List<String> {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        val startCal = Calendar.getInstance().apply {
            timeInMillis = registrationDate
        }
        val endCal = Calendar.getInstance() // Now
        
        val months = mutableListOf<String>()
        
        // Loop through calendar months
        while (!startCal.after(endCal)) {
            months.add(sdf.format(startCal.time))
            startCal.add(Calendar.MONTH, 1)
        }
        
        val currentMonthStr = sdf.format(Date())
        if (months.isEmpty() || !months.contains(currentMonthStr)) {
            months.add(currentMonthStr)
        }
        
        return months.distinct()
    }
}
