package com.example.ui.backup

import android.content.Context
import android.net.Uri
import com.example.data.dao.AppDao
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class RestoreManager(
    private val context: Context,
    private val appDao: AppDao
) {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    suspend fun restoreBackupFromJson(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val stringBuilder = StringBuilder()
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stringBuilder.append(line)
                    }
                }
            }

            val jsonString = stringBuilder.toString()
            val adapter = moshi.adapter(BackupPayload::class.java)
            val payload = adapter.fromJson(jsonString) ?: return@withContext false

            // Clear existing database tables
            appDao.clearProfiles()
            appDao.clearTeachers()
            appDao.clearStudents()
            appDao.clearAttendance()
            appDao.clearPayments()
            appDao.clearExpenses()
            appDao.clearPaymentHistory()
            appDao.clearGroups()
            appDao.clearAssignments()
            appDao.clearExams()
            appDao.clearExamGrades()

            // Insert restored entities
            payload.profiles.firstOrNull()?.let {
                appDao.insertProfile(it)
            }

            payload.teachers.forEach {
                appDao.insertTeacher(it)
            }

            payload.students.forEach {
                appDao.insertStudent(it)
            }

            if (payload.attendance.isNotEmpty()) {
                appDao.insertAttendance(payload.attendance)
            }

            payload.payments.forEach {
                appDao.insertPayment(it)
            }

            payload.expenses.forEach {
                appDao.insertExpense(it)
            }

            payload.paymentHistory.forEach {
                appDao.insertPaymentHistory(it)
            }

            payload.groups.forEach {
                appDao.insertGroup(it)
            }

            payload.assignments.forEach {
                appDao.insertAssignment(it)
            }

            payload.exams.forEach {
                appDao.insertExam(it)
            }

            payload.examGrades.forEach {
                appDao.insertExamGrade(it)
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
