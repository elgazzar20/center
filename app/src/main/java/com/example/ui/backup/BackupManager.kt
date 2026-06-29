package com.example.ui.backup

import android.content.Context
import android.net.Uri
import com.example.data.dao.AppDao
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter

class BackupManager(
    private val context: Context,
    private val appDao: AppDao
) {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    suspend fun createBackupPayload(): BackupPayload = withContext(Dispatchers.IO) {
        val profiles = appDao.getProfile().first()?.let { listOf(it) } ?: emptyList()
        val teachers = appDao.getAllTeachers().first()
        val students = appDao.getAllStudents().first()
        val attendance = appDao.getAllAttendance().first()
        val payments = appDao.getAllPayments().first()
        val expenses = appDao.getAllExpenses().first()
        val paymentHistory = appDao.getAllPaymentHistory().first()
        val groups = appDao.getAllGroups().first()
        val assignments = appDao.getAllAssignments().first()
        val exams = appDao.getAllExams().first()
        val examGrades = appDao.getAllExamGrades().first()

        BackupPayload(
            profiles = profiles,
            teachers = teachers,
            students = students,
            attendance = attendance,
            payments = payments,
            expenses = expenses,
            paymentHistory = paymentHistory,
            groups = groups,
            assignments = assignments,
            exams = exams,
            examGrades = examGrades,
            backupTimestamp = System.currentTimeMillis()
        )
    }

    suspend fun exportBackupToJson(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = createBackupPayload()
            val adapter = moshi.adapter(BackupPayload::class.java)
            val jsonString = adapter.toJson(payload)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(jsonString)
                }
            }
            
            // Save last backup time
            saveLastBackupTimestamp(payload.backupTimestamp)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun saveLastBackupTimestamp(timestamp: Long) {
        val prefs = context.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("last_backup_time", timestamp).apply()
    }

    fun getLastBackupTimestamp(): Long {
        val prefs = context.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
        return prefs.getLong("last_backup_time", 0L)
    }
}
