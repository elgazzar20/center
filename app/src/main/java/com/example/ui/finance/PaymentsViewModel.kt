package com.example.ui.finance

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.*
import com.example.data.repository.AppRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class PaymentsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository

    // State for selected month and year
    private val _selectedMonth = MutableStateFlow(String.format("%02d", Calendar.getInstance().get(Calendar.MONTH) + 1))
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR).toString())
    val selectedYear: StateFlow<String> = _selectedYear.asStateFlow()

    val profile: StateFlow<Profile?>
    val students: StateFlow<List<Student>>
    val teachers: StateFlow<List<Teacher>>
    val paymentHistory: StateFlow<List<PaymentHistory>>
    val studentTeacherCrossRefs: StateFlow<List<StudentTeacherCrossRef>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AppRepository(database.appDao())

        profile = repository.profile.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
        students = repository.allStudents.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        teachers = repository.allTeachers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        paymentHistory = repository.allPaymentHistory.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        studentTeacherCrossRefs = repository.getAllStudentTeacherCrossRefs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun setMonth(month: String) {
        _selectedMonth.value = month
    }

    fun setYear(year: String) {
        _selectedYear.value = year
    }

    fun addPayment(studentId: String, amount: Double, method: String, notes: String, month: String, year: String, date: Long) {
        viewModelScope.launch {
            val record = PaymentHistory(
                studentId = studentId,
                amount = amount,
                paymentDate = date,
                paymentMethod = method,
                notes = notes,
                month = month,
                year = year
            )
            repository.addPaymentHistory(record)
        }
    }

    fun updatePayment(payment: PaymentHistory) {
        viewModelScope.launch {
            repository.addPaymentHistory(payment)
        }
    }

    fun deletePayment(id: String) {
        viewModelScope.launch {
            repository.deletePaymentHistory(id)
        }
    }
}
