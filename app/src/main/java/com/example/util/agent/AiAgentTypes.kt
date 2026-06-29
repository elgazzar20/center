package com.example.util.agent

import com.example.data.model.*
import java.util.UUID

enum class AgentIntent {
    ADD_STUDENT,
    EDIT_STUDENT,
    DELETE_STUDENT,
    MARK_ATTENDANCE,
    MARK_ABSENCE,
    CREATE_GROUP,
    ADD_PAYMENT,
    GENERATE_REPORT,
    STUDENT_ANALYTICS,
    FINANCIAL_ANALYTICS,
    SEND_PARENT_REPORT,
    CREATE_ASSIGNMENT,
    ADD_EXAM_RESULT,
    ARCHIVE_STUDENT,
    VIEW_STATISTICS,
    SEARCH_STUDENT,
    CONFIRM_ACTION,
    CANCEL_ACTION,
    UNKNOWN
}

sealed class AgentResult {
    data class Success(val message: String, val actionExecuted: String? = null) : AgentResult()
    data class Error(val message: String) : AgentResult()
    data class ConfirmationRequired(
        val message: String,
        val pendingAction: PendingAgentAction
    ) : AgentResult()
    data class ReportResult(
        val title: String,
        val type: String, // "student", "group", "attendance", "financial", "profits", "expenses"
        val dataTable: List<Map<String, String>>,
        val summaryText: String,
        val pdfGenerated: Boolean = false,
        val pdfUri: String? = null
    ) : AgentResult()
    data class AnalyticsResult(
        val title: String,
        val statsList: List<StatItem>,
        val details: String
    ) : AgentResult()
    data class RiskReportResult(
        val title: String,
        val atRiskCount: Int,
        val attentionCount: Int,
        val excellentCount: Int,
        val riskRecords: List<RiskRecord>
    ) : AgentResult()
}

data class StatItem(
    val label: String,
    val value: String,
    val isPositive: Boolean = true
)

data class RiskRecord(
    val studentId: String,
    val studentName: String,
    val grade: String,
    val statusLabel: String, // "ممتاز", "يحتاج متابعة", "معرض للتراجع"
    val statusType: String,  // "EXCELLENT", "NEEDS_ATTENTION", "AT_RISK"
    val reason: String,
    val colorHex: String
)

sealed class PendingAgentAction {
    data class DeleteStudent(val studentId: String, val studentName: String) : PendingAgentAction()
    data class ArchiveStudent(val studentId: String, val studentName: String) : PendingAgentAction()
    data class DeleteGroup(val groupId: String, val groupName: String) : PendingAgentAction()
    data class ResetData(val confirmationCode: String) : PendingAgentAction()
}

data class ParsedCommand(
    val intent: AgentIntent,
    val confidence: Float,
    val rawText: String,
    val entities: Map<String, String> = emptyMap()
)

interface AiProvider {
    val name: String
    val providerId: String
    fun isAvailable(): Boolean
    suspend fun generateResponse(prompt: String, contextText: String): String?
}
