package com.example.ui.dashboard

data class AttendanceDayData(
    val dayName: String, // e.g. "الأحد" or date "06/23"
    val presentCount: Int,
    val absentCount: Int,
    val timestamp: Long
)

data class RevenueMonthData(
    val monthName: String, // e.g. "يونيو" or "06"
    val revenue: Double,
    val expenses: Double,
    val monthKey: String // YYYY-MM
)

sealed interface DashboardUiState {
    object Loading : DashboardUiState
    
    data class Success(
        val totalStudents: Int,
        val presentToday: Int,
        val absentToday: Int,
        val exemptStudents: Int,
        val totalFeesDue: Double,
        val totalFeesCollected: Double,
        val totalArrears: Double,
        val attendanceLast7Days: List<AttendanceDayData>,
        val revenueLast6Months: List<RevenueMonthData>,
        val currency: String = "ج.م",
        // New metrics
        val todaysIncome: Double,
        val totalRevenue: Double,
        val totalExpenses: Double,
        val netProfit: Double,
        val totalTeachers: Int,
        val studentsLateInPayment: Int,
        val sessionsToday: Int,
        val occupiedHalls: Int,
        val currentClasses: List<com.example.data.model.Group> = emptyList(),
        val upcomingClasses: List<com.example.data.model.Group> = emptyList(),
        val recentOperations: List<com.example.data.model.RemoteActivityLog> = emptyList()
    ) : DashboardUiState

    data class Error(val message: String) : DashboardUiState
}
