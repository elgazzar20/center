package com.example.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.database.AppDatabase
import kotlinx.coroutines.flow.first
import java.util.Calendar

class DailyAttendanceCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            val database = AppDatabase.getDatabase(applicationContext)
            val dao = database.appDao()

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val todayStart = calendar.timeInMillis

            val allAttendanceList = dao.getAllAttendance().first()
            val hasAttendanceToday = allAttendanceList.any { it.date >= todayStart }

            if (!hasAttendanceToday) {
                NotificationHelper.showNotification(
                    context = applicationContext,
                    title = "📋 لم تسجل الحضور اليوم",
                    body = "تذكير: لم تقم بتسجيل حضور أو غياب الطلاب لليوم بعد. افتح التطبيق لتحديث القوائم.",
                    channelId = NotificationHelper.REMINDER_CHANNEL_ID
                )
            }
            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }
}
