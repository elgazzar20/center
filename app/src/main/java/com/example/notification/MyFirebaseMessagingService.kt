package com.example.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "إشعار جديد"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: ""
        
        NotificationHelper.showNotification(
            context = applicationContext,
            title = title,
            body = body,
            channelId = NotificationHelper.ATTENDANCE_CHANNEL_ID
        )
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // FCM token refreshed
    }
}
