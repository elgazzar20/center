package com.example.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class MonthlyFeeReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            NotificationHelper.showNotification(
                context = applicationContext,
                title = "💰 ذكّر أولياء الأمور بالرسوم",
                body = "تذكير شهري: حان الوقت لتذكير أولياء الأمور بسداد الرسوم المستحقة والاشتراكات الشهرية للطلاب.",
                channelId = NotificationHelper.REMINDER_CHANNEL_ID
            )
            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }
}
