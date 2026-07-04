package com.example.vtbsales.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.ZonedDateTime

object ReminderScheduler {
    private const val REQUEST_CODE = 1730

    fun scheduleDaily(context: Context) {
        SalesNotificationCenter.ensureChannel(context)
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            ReminderSchedule.nextTriggerAfter(ZonedDateTime.now()).toInstant().toEpochMilli(),
            AlarmManager.INTERVAL_DAY,
            reminderIntent(context)
        )
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(reminderIntent(context))
    }

    private fun reminderIntent(context: Context): PendingIntent {
        val intent = Intent(context, DailyReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
