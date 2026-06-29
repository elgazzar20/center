package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.example.MainActivity
import java.util.concurrent.TimeUnit

object NotificationHelper {
    const val ATTENDANCE_CHANNEL_ID = "attendance_channel"
    const val REMINDER_CHANNEL_ID = "reminder_channel"
    
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attendanceChannel = NotificationChannel(
                ATTENDANCE_CHANNEL_ID,
                "إشعارات الحضور والغياب",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات تسجيل الحضور والغياب الفورية وتنبيهات المعلم والسنتر"
            }

            val reminderChannel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                "التذكيرات الدورية",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "تنبيهات دورية لتسجيل الحضور وتذكير الرسوم الشهرية"
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(attendanceChannel)
            notificationManager.createNotificationChannel(reminderChannel)
        }
    }

    fun showNotification(
        context: Context,
        title: String,
        body: String,
        channelId: String = ATTENDANCE_CHANNEL_ID,
        notificationId: Int = System.currentTimeMillis().toInt()
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(if (channelId == ATTENDANCE_CHANNEL_ID) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Android 13+ permission not granted yet
        }
    }

    fun scheduleDailyAttendanceCheck(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<DailyAttendanceCheckWorker>(24, TimeUnit.HOURS)
            .setConstraints(Constraints.NONE)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "DailyAttendanceCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    fun scheduleMonthlyFeeReminder(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<MonthlyFeeReminderWorker>(28, TimeUnit.DAYS)
            .setConstraints(Constraints.NONE)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "MonthlyFeeReminder",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
