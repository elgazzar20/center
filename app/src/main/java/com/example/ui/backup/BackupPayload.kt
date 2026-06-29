package com.example.ui.backup

import com.example.data.model.*
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BackupPayload(
    val profiles: List<Profile> = emptyList(),
    val teachers: List<Teacher> = emptyList(),
    val students: List<Student> = emptyList(),
    val attendance: List<Attendance> = emptyList(),
    val payments: List<Payment> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val paymentHistory: List<PaymentHistory> = emptyList(),
    val groups: List<Group> = emptyList(),
    val assignments: List<Assignment> = emptyList(),
    val exams: List<Exam> = emptyList(),
    val examGrades: List<ExamGrade> = emptyList(),
    val backupTimestamp: Long = System.currentTimeMillis()
)
