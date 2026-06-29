package com.example.util.agent

import com.example.data.model.*
import com.example.data.viewmodel.AppViewModel
import kotlinx.coroutines.flow.StateFlow

/**
 * AiDatabaseTools provides safe, read-only data access layer to all Room database tables for the AI Agent,
 * ensuring no direct modifications can occur outside the designated AiActionExecutor.
 */
class AiDatabaseTools(private val viewModel: AppViewModel) {

    fun getStudents(): StateFlow<List<Student>> = viewModel.students

    fun getAttendance(): StateFlow<List<Attendance>> = viewModel.attendance

    fun getPayments(): StateFlow<List<Payment>> = viewModel.payments

    fun getPaymentHistory(): StateFlow<List<PaymentHistory>> = viewModel.paymentHistory

    fun getGroups(): StateFlow<List<Group>> = viewModel.groups

    fun getExams(): StateFlow<List<Exam>> = viewModel.exams

    fun getExamGrades(): StateFlow<List<ExamGrade>> = viewModel.examGrades

    fun getExpenses(): StateFlow<List<Expense>> = viewModel.expenses

    fun getAssignments(): StateFlow<List<Assignment>> = viewModel.assignments
}
