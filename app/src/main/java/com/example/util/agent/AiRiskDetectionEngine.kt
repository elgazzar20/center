package com.example.util.agent

import com.example.data.model.*
import com.example.util.StudentAnalyticsEngine
import com.example.util.StudentEvaluationService
import com.example.util.EvaluationStatus

object AiRiskDetectionEngine {

    fun generateRiskReport(
        students: List<Student>,
        attendance: List<Attendance>,
        payments: List<Payment>,
        exams: List<Exam>,
        examGrades: List<ExamGrade>
    ): AgentResult.RiskReportResult {
        
        val riskRecords = mutableListOf<RiskRecord>()
        var atRiskCount = 0
        var attentionCount = 0
        var excellentCount = 0

        val activeStudents = students.filter { it.isActive && !it.isArchived }

        for (student in activeStudents) {
            val analytics = StudentAnalyticsEngine.calculateAnalytics(
                student = student,
                allAttendance = attendance,
                allPayments = payments,
                allExams = exams,
                allExamGrades = examGrades
            )
            val evaluation = StudentEvaluationService.evaluateStudent(analytics, student)

            when (evaluation.status) {
                EvaluationStatus.AT_RISK -> atRiskCount++
                EvaluationStatus.NEEDS_ATTENTION -> attentionCount++
                EvaluationStatus.EXCELLENT -> excellentCount++
            }

            riskRecords.add(
                RiskRecord(
                    studentId = student.id,
                    studentName = student.name,
                    grade = student.grade,
                    statusLabel = evaluation.arabicStatusLabel,
                    statusType = evaluation.status.name,
                    reason = evaluation.reason,
                    colorHex = evaluation.colorHex
                )
            )
        }

        // Sort: show AT_RISK first, then NEEDS_ATTENTION, then EXCELLENT
        val sortedRecords = riskRecords.sortedWith(
            compareBy<RiskRecord> {
                when (it.statusType) {
                    "AT_RISK" -> 0
                    "NEEDS_ATTENTION" -> 1
                    else -> 2
                }
            }.thenBy { it.studentName }
        )

        return AgentResult.RiskReportResult(
            title = "تقرير التقييم الذكي وكشف المخاطر 🛡️",
            atRiskCount = atRiskCount,
            attentionCount = attentionCount,
            excellentCount = excellentCount,
            riskRecords = sortedRecords
        )
    }
}
