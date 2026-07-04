package com.example.vtbsales.notifications

import java.time.LocalTime
import java.time.ZonedDateTime

object ReminderSchedule {
    val reminderTime: LocalTime = LocalTime.of(17, 30)

    fun nextTriggerAfter(now: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime {
        val todayTrigger = now
            .withHour(reminderTime.hour)
            .withMinute(reminderTime.minute)
            .withSecond(0)
            .withNano(0)
        return if (todayTrigger.isAfter(now)) todayTrigger else todayTrigger.plusDays(1)
    }
}
