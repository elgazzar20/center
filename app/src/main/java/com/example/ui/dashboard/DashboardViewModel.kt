package com.example.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.*
import com.example.data.repository.AppRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AppRepository(database.appDao())
        observeDashboardData()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            // Brief artificial delay to show pull-to-refresh visual effect and load updated data
            delay(1000)
            _isRefreshing.value = false
        }
    }

    private fun observeDashboardData() {
        viewModelScope.launch {
            combine(
                repository.profile,
                repository.allStudents,
                repository.allAttendance,
                repository.allPayments,
                repository.allPaymentHistory,
                repository.allExpenses,
                repository.allTeachers,
                repository.allGroups,
                repository.getAllStudentTeacherCrossRefs()
            ) { array ->
                val profile = array[0] as Profile?
                @Suppress("UNCHECKED_CAST")
                val studentsList = array[1] as List<Student>
                @Suppress("UNCHECKED_CAST")
                val attendanceList = array[2] as List<Attendance>
                @Suppress("UNCHECKED_CAST")
                val paymentsList = array[3] as List<Payment>
                @Suppress("UNCHECKED_CAST")
                val paymentHistoryList = array[4] as List<PaymentHistory>
                @Suppress("UNCHECKED_CAST")
                val expensesList = array[5] as List<Expense>
                @Suppress("UNCHECKED_CAST")
                val teachersList = array[6] as List<Teacher>
                @Suppress("UNCHECKED_CAST")
                val groupsList = array[7] as List<Group>
                @Suppress("UNCHECKED_CAST")
                val crossRefsList = array[8] as List<StudentTeacherCrossRef>
                
                val sysType = profile?.systemType ?: "center"
                val currency = profile?.currency ?: "ج.م"
                
                // Filter data based on systemType ("teacher" only sees teacherId == "me")
                val filteredStudents = if (sysType == "teacher") {
                    studentsList.filter { it.teacherId == "me" }
                } else {
                    studentsList
                }

                val filteredAttendance = if (sysType == "teacher") {
                    attendanceList.filter { it.teacherId == "me" }
                } else {
                    attendanceList
                }

                val filteredExpenses = expensesList

                // 1. Total active students
                val activeStudents = filteredStudents.filter { it.isActive && !it.isArchived }
                val totalStudents = activeStudents.size

                // 2. Exempt students
                val exemptStudents = activeStudents.count { it.isExempt }

                // 3. Attendance Today (present, absent)
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val todayStart = cal.timeInMillis
                val todayEnd = todayStart + 24 * 60 * 60 * 1000 - 1

                val todayAttendance = filteredAttendance.filter { it.date in todayStart..todayEnd }
                val presentToday = todayAttendance.count { it.status == "present" || it.status == "late" }
                val absentToday = todayAttendance.count { it.status == "absent" }

                // 4. Financial computations:
                // Total monthly fees due: netFee of active students (taking individual teacher custom fees into account)
                val totalFeesDue = activeStudents.sumOf { it.getActualNetFee(crossRefsList) }

                // Total fees collected: sum of payments in the current month using the new payment history
                val calNow = Calendar.getInstance()
                val currentMonthCode = String.format("%02d", calNow.get(Calendar.MONTH) + 1)
                val currentYearStr = calNow.get(Calendar.YEAR).toString()

                val totalFeesCollected = paymentHistoryList
                    .filter { it.month == currentMonthCode && it.year == currentYearStr }
                    .sumOf { it.amount }

                // Arrears = Due - Collected per student for maximum accuracy
                val totalArrears = activeStudents.sumOf { student ->
                    val paidForStudent = paymentHistoryList
                        .filter { it.studentId == student.id && it.month == currentMonthCode && it.year == currentYearStr }
                        .sumOf { it.amount }
                    val net = student.getActualNetFee(crossRefsList)
                    maxOf(0.0, net - paidForStudent)
                }

                // 5. Attendance Last 7 Days (Attendance ratio/percentage or counts)
                val attendanceLast7Days = ArrayList<AttendanceDayData>()
                val sdfDayName = SimpleDateFormat("E", Locale("ar"))
                for (i in 6 downTo 0) {
                    val dayCal = Calendar.getInstance()
                    dayCal.add(Calendar.DAY_OF_YEAR, -i)
                    dayCal.set(Calendar.HOUR_OF_DAY, 0)
                    dayCal.set(Calendar.MINUTE, 0)
                    dayCal.set(Calendar.SECOND, 0)
                    dayCal.set(Calendar.MILLISECOND, 0)
                    val dayStart = dayCal.timeInMillis
                    val dayEnd = dayStart + 24 * 60 * 60 * 1000 - 1

                    val dayAtts = filteredAttendance.filter { it.date in dayStart..dayEnd }
                    val present = dayAtts.count { it.status == "present" || it.status == "late" }
                    val absent = dayAtts.count { it.status == "absent" }

                    val label = sdfDayName.format(dayCal.time)
                    attendanceLast7Days.add(AttendanceDayData(label, present, absent, dayStart))
                }

                // 6. Revenue Last 6 Months (using PaymentHistory)
                val revenueLast6Months = ArrayList<RevenueMonthData>()
                val sdfMonthName = SimpleDateFormat("MMM", Locale("ar"))
                val sdfMonthKey = SimpleDateFormat("yyyy-MM", Locale.US)
                for (i in 5 downTo 0) {
                    val monthCal = Calendar.getInstance()
                    monthCal.add(Calendar.MONTH, -i)
                    val mCode = String.format("%02d", monthCal.get(Calendar.MONTH) + 1)
                    val yCode = monthCal.get(Calendar.YEAR).toString()
                    val key = sdfMonthKey.format(monthCal.time)
                    val name = sdfMonthName.format(monthCal.time)

                    val monthPaymentsHistory = paymentHistoryList.filter { it.month == mCode && it.year == yCode }
                    val monthExpenses = filteredExpenses.filter { it.month == key }

                    val rev = monthPaymentsHistory.sumOf { it.amount }
                    val exp = monthExpenses.sumOf { it.amount }

                    revenueLast6Months.add(RevenueMonthData(name, rev, exp, key))
                }

                val todaysIncome = paymentHistoryList
                    .filter { it.paymentDate in todayStart..todayEnd }
                    .sumOf { it.amount }
                
                val totalRevenue = paymentsList.sumOf { it.amount }
                val totalExpenses = filteredExpenses.sumOf { it.amount }
                val netProfit = totalRevenue - totalExpenses
                val totalTeachers = teachersList.size
                
                val studentsLateInPayment = activeStudents.count { student ->
                    val paidForStudent = paymentHistoryList
                        .filter { it.studentId == student.id && it.month == currentMonthCode && it.year == currentYearStr }
                        .sumOf { it.amount }
                    (student.getActualNetFee(crossRefsList) - paidForStudent) > 0
                }

                val todayName = sdfDayName.format(cal.time)
                val groupsToday = groupsList.filter { it.schedule.contains(todayName) }
                val sessionsToday = groupsToday.size
                val occupiedHalls = groupsToday.map { it.classroom }.filter { it.isNotBlank() }.distinct().size

                val currentClasses = mutableListOf<com.example.data.model.Group>()
                val upcomingClasses = mutableListOf<com.example.data.model.Group>()
                val currentHour = cal.get(Calendar.HOUR_OF_DAY)
                
                // Simple logic for current/upcoming based on schedule string matching numbers
                groupsToday.forEach { group ->
                    val match = Regex("\\d+").find(group.schedule)
                    if (match != null) {
                        var hour = match.value.toInt()
                        if (group.schedule.contains("م") && hour != 12) hour += 12
                        if (hour == currentHour || hour == currentHour - 1) {
                            currentClasses.add(group)
                        } else if (hour > currentHour) {
                            upcomingClasses.add(group)
                        }
                    }
                }
                
                val recentLogs = com.example.util.ActivityLogManager.getAllLogs().take(5)

                DashboardUiState.Success(
                    totalStudents = totalStudents,
                    presentToday = presentToday,
                    absentToday = absentToday,
                    exemptStudents = exemptStudents,
                    totalFeesDue = totalFeesDue,
                    totalFeesCollected = totalFeesCollected,
                    totalArrears = totalArrears,
                    attendanceLast7Days = attendanceLast7Days,
                    revenueLast6Months = revenueLast6Months,
                    currency = currency,
                    todaysIncome = todaysIncome,
                    totalRevenue = totalRevenue,
                    totalExpenses = totalExpenses,
                    netProfit = netProfit,
                    totalTeachers = totalTeachers,
                    studentsLateInPayment = studentsLateInPayment,
                    sessionsToday = sessionsToday,
                    occupiedHalls = occupiedHalls,
                    currentClasses = currentClasses,
                    upcomingClasses = upcomingClasses,
                    recentOperations = recentLogs
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}
