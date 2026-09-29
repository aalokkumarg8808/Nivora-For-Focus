package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

class FocusReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val actionType = intent.getStringExtra(FocusNotificationManager.EXTRA_ACTION_TYPE)
        val taskId = intent.getStringExtra(FocusNotificationManager.EXTRA_TASK_ID)

        if (actionType == FocusNotificationManager.ACTION_TYPE_TASK || taskId != null) {
            val validTaskId = taskId ?: return
            val title = intent.getStringExtra(FocusNotificationManager.EXTRA_TITLE) ?: "Task"
            val category = intent.getStringExtra(FocusNotificationManager.EXTRA_CATEGORY) ?: "Study"
            val notificationId = intent.getIntExtra(
                FocusNotificationManager.EXTRA_NOTIFICATION_ID,
                Math.abs(validTaskId.hashCode())
            )
            val repeatType = intent.getStringExtra(FocusNotificationManager.EXTRA_REPEAT) ?: "Once"
            val dayOfWeek = intent.getIntExtra(FocusNotificationManager.EXTRA_DAY_OF_WEEK, -1)
            val dueTime = intent.getStringExtra(FocusNotificationManager.EXTRA_DUE_TIME) ?: ""

            // Show the normal local notification
            FocusNotificationManager.showTaskReminderNotification(
                context = context,
                taskId = validTaskId,
                title = title,
                category = category,
                notificationId = notificationId
            )

            // Reschedule next occurrence for repeating alarms
            if (dueTime.isNotBlank()) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                if (alarmManager != null) {
                    val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    val pendingIntent = PendingIntent.getBroadcast(context, notificationId, intent, flags)

                    val nextTrigger = when (repeatType) {
                        "Daily" -> {
                            val cal = Calendar.getInstance()
                            val (hour, minute) = FocusNotificationManager.parseTimeHourMinute(dueTime) ?: return
                            cal.set(Calendar.HOUR_OF_DAY, hour)
                            cal.set(Calendar.MINUTE, minute)
                            cal.set(Calendar.SECOND, 0)
                            cal.set(Calendar.MILLISECOND, 0)
                            cal.add(Calendar.DAY_OF_YEAR, 1)
                            cal.timeInMillis
                        }
                        "Custom Days" -> {
                            if (dayOfWeek in 1..7) {
                                val cal = Calendar.getInstance()
                                val (hour, minute) = FocusNotificationManager.parseTimeHourMinute(dueTime) ?: return
                                cal.set(Calendar.HOUR_OF_DAY, hour)
                                cal.set(Calendar.MINUTE, minute)
                                cal.set(Calendar.SECOND, 0)
                                cal.set(Calendar.MILLISECOND, 0)
                                cal.add(Calendar.DAY_OF_YEAR, 7) // Next week same day
                                cal.timeInMillis
                            } else null
                        }
                        else -> null
                    }

                    if (nextTrigger != null) {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                if (alarmManager.canScheduleExactAlarms()) {
                                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTrigger, pendingIntent)
                                } else {
                                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTrigger, pendingIntent)
                                }
                            } else {
                                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTrigger, pendingIntent)
                            }
                        } catch (e: Exception) {
                            try {
                                alarmManager.set(AlarmManager.RTC_WAKEUP, nextTrigger, pendingIntent)
                            } catch (ex: Exception) {
                                // Ignore
                            }
                        }
                    }
                }
            }
            return
        }

        // Handle Scheduled Focus Session Reminder
        val sessionId = intent.getStringExtra(FocusNotificationManager.EXTRA_SESSION_ID) ?: return
        val title = intent.getStringExtra(FocusNotificationManager.EXTRA_TITLE) ?: "Focus Session"
        val category = intent.getStringExtra(FocusNotificationManager.EXTRA_CATEGORY) ?: "Study"
        val minutesBefore = intent.getIntExtra(FocusNotificationManager.EXTRA_MINUTES_BEFORE, 10)
        val durationMinutes = intent.getIntExtra(FocusNotificationManager.EXTRA_DURATION, 25)

        FocusNotificationManager.showReminderNotification(
            context = context,
            sessionId = sessionId,
            title = title,
            category = category,
            minutesBefore = minutesBefore,
            durationMinutes = durationMinutes
        )
    }
}
