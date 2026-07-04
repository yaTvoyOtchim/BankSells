package com.example.vtbsales.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.example.vtbsales.MainActivity
import com.example.vtbsales.R

object SalesNotificationCenter {
    const val CHANNEL_ID = "daily_sales_reminders"
    const val REMINDER_NOTIFICATION_ID = 1730
    const val ACTION_OPEN_FROM_REMINDER = "com.example.vtbsales.OPEN_FROM_REMINDER"
    const val DEFAULT_TITLE = "ВТБ"
    const val DEFAULT_BODY = "Внесите продажи за день"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Напоминания о продажах",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Ежедневное напоминание внести продажи за день"
        }
        manager.createNotificationChannel(channel)
    }

    fun showReminder(
        context: Context,
        title: String = DEFAULT_TITLE,
        body: String = DEFAULT_BODY
    ) {
        ensureChannel(context)
        if (!canPostNotifications(context)) return
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_vtb)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setColor(0xFF0057FF.toInt())
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent(context))
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(REMINDER_NOTIFICATION_ID, notification)
    }

    fun openAppPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_FROM_REMINDER
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            REMINDER_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun canPostNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}
