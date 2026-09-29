package com.example.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.ScheduledFocusSession
import com.example.data.model.Task
import com.example.util.PlantUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object FocusNotificationManager {
    const val CHANNEL_ID = "focus_reminders_channel"
    const val CHANNEL_NAME = "Plant Reminders"

    const val EXTRA_ACTION_TYPE = "extra_action_type"
    const val ACTION_TYPE_FOCUS = "focus_session"
    const val ACTION_TYPE_TASK = "task_reminder"

    const val EXTRA_SESSION_ID = "extra_session_id"
    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_CATEGORY = "extra_category"
    const val EXTRA_MINUTES_BEFORE = "extra_minutes_before"
    const val EXTRA_DURATION = "extra_duration"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_REPEAT = "extra_repeat"
    const val EXTRA_DAY_OF_WEEK = "extra_day_of_week"
    const val EXTRA_DUE_TIME = "extra_due_time"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminds you of your scheduled tasks and focus study sessions."
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // --- Time Parsing Helper ---
    fun parseTimeHourMinute(dueTime: String): Pair<Int, Int>? {
        if (dueTime.isBlank()) return null
        val trimmed = dueTime.trim()
        val formats = listOf(
            SimpleDateFormat("h:mm a", Locale.getDefault()),
            SimpleDateFormat("hh:mm a", Locale.getDefault()),
            SimpleDateFormat("H:mm", Locale.getDefault()),
            SimpleDateFormat("HH:mm", Locale.getDefault())
        )
        for (fmt in formats) {
            try {
                val parsedDate = fmt.parse(trimmed)
                if (parsedDate != null) {
                    val cal = Calendar.getInstance().apply { time = parsedDate }
                    return Pair(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
                }
            } catch (e: Exception) {
                // Try next format
            }
        }
        val match = Regex("""(\d{1,2}):(\d{2})\s*(AM|PM|am|pm)?""").find(trimmed)
        if (match != null) {
            var hour = match.groupValues[1].toInt()
            val min = match.groupValues[2].toInt()
            val ampm = match.groupValues.getOrNull(3)?.uppercase(Locale.getDefault())
            if (ampm == "PM" && hour < 12) hour += 12
            if (ampm == "AM" && hour == 12) hour = 0
            return Pair(hour, min)
        }
        return null
    }

    // --- Task Reminder Scheduling ---

    /**
     * Calculates the trigger time for a single "Once" task reminder.
     */
    fun calculateTaskTriggerTime(dueDate: String, dueTime: String): Long? {
        val (hour, minute) = parseTimeHourMinute(dueTime) ?: return null
        val calendar = Calendar.getInstance() // Device local timezone
        val targetDateStr = dueDate.ifBlank { PlantUtils.getTodayDateString() }

        val dateParts = targetDateStr.split("-")
        if (dateParts.size == 3) {
            val year = dateParts[0].toIntOrNull() ?: calendar.get(Calendar.YEAR)
            val month = (dateParts[1].toIntOrNull() ?: (calendar.get(Calendar.MONTH) + 1)) - 1
            val day = dateParts[2].toIntOrNull() ?: calendar.get(Calendar.DAY_OF_MONTH)
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, day)
        }

        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        var triggerTime = calendar.timeInMillis
        // If dueDate was blank/today and the time is already passed today, advance to tomorrow
        if (dueDate.isBlank() && triggerTime <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            triggerTime = calendar.timeInMillis
        }
        return triggerTime
    }

    /**
     * Calculates next trigger time for a daily reminder at [dueTime].
     */
    fun calculateDailyTriggerTime(dueTime: String): Long? {
        val (hour, minute) = parseTimeHourMinute(dueTime) ?: return null
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }

    /**
     * Calculates next trigger time for a specific day of week (Calendar.MONDAY..Calendar.SUNDAY) at [dueTime].
     */
    fun calculateCustomDayTriggerTime(dayOfWeek: Int, dueTime: String): Long? {
        val (hour, minute) = parseTimeHourMinute(dueTime) ?: return null
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
        var daysDiff = (dayOfWeek - currentDay) % 7
        if (daysDiff < 0) {
            daysDiff += 7
        }
        if (daysDiff == 0 && calendar.timeInMillis <= System.currentTimeMillis()) {
            daysDiff = 7
        }
        calendar.add(Calendar.DAY_OF_YEAR, daysDiff)
        return calendar.timeInMillis
    }

    fun scheduleTaskReminder(context: Context, task: Task) {
        if (!task.reminderEnabled || task.dueTime.isBlank()) {
            cancelTaskReminder(context, task)
            return
        }

        createNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val baseNotifId = task.notificationId ?: (Math.abs((task.id + task.title).hashCode()) % 900000 + 1000)

        // Cancel previous alarms first to prevent duplicates
        cancelTaskReminder(context, task)

        when (task.reminderRepeat) {
            "Daily" -> {
                val triggerTime = calculateDailyTriggerTime(task.dueTime) ?: return
                scheduleAlarmIntent(
                    context = context,
                    alarmManager = alarmManager,
                    notifId = baseNotifId,
                    triggerTime = triggerTime,
                    task = task,
                    repeatType = "Daily",
                    dayOfWeek = -1
                )
            }
            "Custom Days" -> {
                val days = if (task.reminderDays.isNotEmpty()) task.reminderDays else listOf(Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY)
                days.forEach { dayOfWeek ->
                    val triggerTime = calculateCustomDayTriggerTime(dayOfWeek, task.dueTime) ?: return@forEach
                    val dayNotifId = baseNotifId + (dayOfWeek * 1000)
                    scheduleAlarmIntent(
                        context = context,
                        alarmManager = alarmManager,
                        notifId = dayNotifId,
                        triggerTime = triggerTime,
                        task = task,
                        repeatType = "Custom Days",
                        dayOfWeek = dayOfWeek
                    )
                }
            }
            else -> {
                // "Once"
                val triggerTime = calculateTaskTriggerTime(task.dueDate, task.dueTime) ?: return
                if (triggerTime <= System.currentTimeMillis()) {
                    return // Past time
                }
                scheduleAlarmIntent(
                    context = context,
                    alarmManager = alarmManager,
                    notifId = baseNotifId,
                    triggerTime = triggerTime,
                    task = task,
                    repeatType = "Once",
                    dayOfWeek = -1
                )
            }
        }
    }

    private fun scheduleAlarmIntent(
        context: Context,
        alarmManager: AlarmManager,
        notifId: Int,
        triggerTime: Long,
        task: Task,
        repeatType: String,
        dayOfWeek: Int
    ) {
        val intent = Intent(context, FocusReminderReceiver::class.java).apply {
            putExtra(EXTRA_ACTION_TYPE, ACTION_TYPE_TASK)
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_TITLE, task.title)
            putExtra(EXTRA_CATEGORY, task.category)
            putExtra(EXTRA_NOTIFICATION_ID, notifId)
            putExtra(EXTRA_REPEAT, repeatType)
            putExtra(EXTRA_DAY_OF_WEEK, dayOfWeek)
            putExtra(EXTRA_DUE_TIME, task.dueTime)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, notifId, intent, flags)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: Exception) {
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } catch (ex: Exception) {
                // Ignore restricted OS issues
            }
        }
    }

    fun cancelTaskReminder(context: Context, task: Task) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val baseNotifId = task.notificationId ?: (Math.abs((task.id + task.title).hashCode()) % 900000 + 1000)

        // Cancel base notification ID
        val intent = Intent(context, FocusReminderReceiver::class.java).apply {
            putExtra(EXTRA_ACTION_TYPE, ACTION_TYPE_TASK)
            putExtra(EXTRA_TASK_ID, task.id)
        }
        val flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        val basePendingIntent = PendingIntent.getBroadcast(context, baseNotifId, intent, flags)
        if (basePendingIntent != null) {
            alarmManager.cancel(basePendingIntent)
            basePendingIntent.cancel()
        }

        // Cancel any day-specific custom days alarms (days 1..7: Sunday through Saturday)
        for (day in 1..7) {
            val dayId = baseNotifId + (day * 1000)
            val dayPendingIntent = PendingIntent.getBroadcast(context, dayId, intent, flags)
            if (dayPendingIntent != null) {
                alarmManager.cancel(dayPendingIntent)
                dayPendingIntent.cancel()
            }
        }

        // Also cancel stored notificationIds list if any
        task.notificationIds.forEach { storedId ->
            val storedPendingIntent = PendingIntent.getBroadcast(context, storedId, intent, flags)
            if (storedPendingIntent != null) {
                alarmManager.cancel(storedPendingIntent)
                storedPendingIntent.cancel()
            }
        }
    }

    fun showTaskReminderNotification(
        context: Context,
        taskId: String,
        title: String,
        category: String,
        notificationId: Int
    ) {
        createNotificationChannel(context)

        // Launch app into Tasks screen when notification is tapped
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "TASKS")
            putExtra("task_id", taskId)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val contentPendingIntent = PendingIntent.getActivity(context, notificationId, openAppIntent, flags)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🌱 Task Reminder: $title")
            .setContentText("It's time for your $category task: $title")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("It's time to work on '$title' ($category).\nTake a moment to make meaningful progress on your goals today!")
            )
            .setColor(0xFF12B76A.toInt())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Open Tasks",
                contentPendingIntent
            )
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    // --- Scheduled Focus Session Reminders ---
    fun scheduleReminder(context: Context, session: ScheduledFocusSession) {
        createNotificationChannel(context)
        val triggerTime = session.reminderTriggerTimeMillis
        if (triggerTime <= System.currentTimeMillis()) {
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, FocusReminderReceiver::class.java).apply {
            putExtra(EXTRA_ACTION_TYPE, ACTION_TYPE_FOCUS)
            putExtra(EXTRA_SESSION_ID, session.id)
            putExtra(EXTRA_TITLE, session.title)
            putExtra(EXTRA_CATEGORY, session.category)
            putExtra(EXTRA_MINUTES_BEFORE, session.reminderMinutesBefore)
            putExtra(EXTRA_DURATION, session.durationMinutes)
        }

        val requestCode = session.id.hashCode()
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: Exception) {
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } catch (ex: Exception) {
                // Ignore if restricted
            }
        }
    }

    fun cancelReminder(context: Context, session: ScheduledFocusSession) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, FocusReminderReceiver::class.java).apply {
            putExtra(EXTRA_ACTION_TYPE, ACTION_TYPE_FOCUS)
            putExtra(EXTRA_SESSION_ID, session.id)
        }
        val requestCode = session.id.hashCode()
        val flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun showReminderNotification(
        context: Context,
        sessionId: String,
        title: String,
        category: String,
        minutesBefore: Int,
        durationMinutes: Int
    ) {
        createNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "FOCUS")
            putExtra("focus_category", category)
            putExtra("focus_duration", durationMinutes)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val contentPendingIntent = PendingIntent.getActivity(context, sessionId.hashCode(), openAppIntent, flags)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🌱 Focus Session Approaching: $title")
            .setContentText("Your $category session starts in $minutesBefore minutes ($durationMinutes min session). Get ready to grow!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Your $category session '$title' is starting in $minutesBefore minutes ($durationMinutes min duration).\nTake a breath, gather your materials, and prepare to bloom!")
            )
            .setColor(0xFF12B76A.toInt())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Start Focus Now",
                contentPendingIntent
            )
            .build()

        try {
            NotificationManagerCompat.from(context).notify(sessionId.hashCode(), notification)
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    fun showFocusCompleteNotification(
        context: Context,
        category: String,
        durationMinutes: Int
    ) {
        createNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "FOCUS")
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val notifId = ((System.currentTimeMillis() % 100000) + 300000).toInt()
        val contentPendingIntent = PendingIntent.getActivity(context, notifId, openAppIntent, flags)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🌱 Focus Session Complete")
            .setContentText("Your $durationMinutes-minute $category focus session is complete.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Your $durationMinutes-minute $category focus session is complete.\nGreat job staying focused and growing your virtual plant!")
            )
            .setColor(0xFF12B76A.toInt())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "Open Plant",
                contentPendingIntent
            )
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }
}
