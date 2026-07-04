package com.example.vtbsales.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        SalesNotificationCenter.showReminder(context)
    }
}
