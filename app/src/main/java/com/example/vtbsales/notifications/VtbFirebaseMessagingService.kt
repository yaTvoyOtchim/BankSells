package com.example.vtbsales.notifications

import com.example.vtbsales.session.SessionStore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

@Suppress("OVERRIDE_DEPRECATION")
class VtbFirebaseMessagingService : FirebaseMessagingService() {
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        SessionStore(this).saveFcmToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title
            ?: message.data["title"]
            ?: SalesNotificationCenter.DEFAULT_TITLE
        val body = message.notification?.body
            ?: message.data["body"]
            ?: SalesNotificationCenter.DEFAULT_BODY
        SalesNotificationCenter.showReminder(this, title, body)
    }
}
