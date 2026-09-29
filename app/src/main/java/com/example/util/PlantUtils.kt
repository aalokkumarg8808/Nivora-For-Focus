package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object PlantUtils {
    const val TASK_COMPLETION_XP = 20
    const val HABIT_COMPLETION_XP = 15
    const val FOCUS_SESSION_XP = 25
    const val GOAL_COMPLETION_XP = 50

    fun calculateLevel(xp: Int): Int {
        return (xp / 100) + 1
    }

    fun xpProgressInLevel(xp: Int): Float {
        return (xp % 100).toFloat() / 100f
    }

    fun xpRemainingToNextLevel(xp: Int): Int {
        return 100 - (xp % 100)
    }

    fun getDynamicGreeting(): Pair<String, String> {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Good Morning ☀️" to "Let's make today productive!"
            in 12..16 -> "Good Afternoon 🌤️" to "Keep up your great focus!"
            in 17..20 -> "Good Evening 🌇" to "Review today's growth!"
            else -> "Good Night 🌙" to "Rest well for tomorrow's blooms!"
        }
    }

    fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    fun formatDateForDisplay(dateStr: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(dateStr) ?: return dateStr
            val displayFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            displayFormat.format(date)
        } catch (e: Exception) {
            dateStr
        }
    }

    data class DayItem(
        val dayName: String, // Mon, Tue, etc.
        val dayNumber: Int,  // 22, 23, etc.
        val dateString: String, // YYYY-MM-DD
        val isToday: Boolean,
        val dayLetter: String = dayName.take(1)
    )

    fun getCurrentWeekDays(): List<DayItem> {
        val todayStr = getTodayDateString()
        val calendar = Calendar.getInstance()
        // Reset time
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // Find Monday of the current week deterministically
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = when (currentDayOfWeek) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
        calendar.add(Calendar.DAY_OF_YEAR, -daysFromMonday)

        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val dayLetters = listOf("M", "T", "W", "T", "F", "S", "S")
        val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val list = mutableListOf<DayItem>()
        for (i in 0 until 7) {
            val dateStr = isoFormat.format(calendar.time)
            list.add(
                DayItem(
                    dayName = dayNames[i],
                    dayNumber = calendar.get(Calendar.DAY_OF_MONTH),
                    dateString = dateStr,
                    isToday = dateStr == todayStr,
                    dayLetter = dayLetters[i]
                )
            )
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        return list
    }

    fun isDateInStreak(
        dateStr: String,
        streakCount: Int,
        activeDates: Set<String>
    ): Boolean {
        if (activeDates.contains(dateStr)) return true
        if (streakCount <= 0) return false

        val todayStr = getTodayDateString()
        val isTodayActive = activeDates.contains(todayStr)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val cal = Calendar.getInstance()
        // If today is active, streak extends from today backwards streakCount days
        // If today is not active yet, streak extends from yesterday backwards streakCount days
        if (!isTodayActive) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }

        for (i in 0 until streakCount) {
            val sDate = sdf.format(cal.time)
            if (sDate == dateStr) return true
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return false
    }

    fun getDateStringFromMillis(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun formatMinutes(minutes: Int): String {
        if (minutes <= 0) return "0m"
        val hours = minutes / 60
        val mins = minutes % 60
        return if (hours > 0) {
            if (mins > 0) "${hours}h ${mins}m" else "${hours}h"
        } else {
            "${mins}m"
        }
    }

    fun getLevelTitle(level: Int): String {
        return when (level) {
            1 -> "Seedling Explorer"
            2 -> "Sprout Tender"
            3 -> "Budding Botanist"
            4 -> "Flourishing Gardener"
            5 -> "Canopy Arborist"
            6 -> "Master Botanist"
            else -> "Flora Guardian Lv.$level"
        }
    }

    fun calculateStreak(completedDates: List<String>): Pair<Int, Int> {
        if (completedDates.isEmpty()) return 0 to 0

        val uniqueDates = completedDates.toSet().sortedDescending()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = getTodayDateString()

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = sdf.format(calendar.time)

        var currentStreak = 0
        var checkCal = Calendar.getInstance()

        // Check if user completed something today or yesterday
        if (uniqueDates.contains(todayStr)) {
            currentStreak++
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
        } else if (uniqueDates.contains(yesterdayStr)) {
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            // Broken streak
            return 0 to calculateLongestStreak(uniqueDates)
        }

        while (true) {
            val dateStr = sdf.format(checkCal.time)
            if (uniqueDates.contains(dateStr)) {
                currentStreak++
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }

        val longest = maxOf(currentStreak, calculateLongestStreak(uniqueDates))
        return currentStreak to longest
    }

    private fun calculateLongestStreak(sortedDescDates: List<String>): Int {
        if (sortedDescDates.isEmpty()) return 0
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dates = sortedDescDates.mapNotNull {
            try { sdf.parse(it) } catch (e: Exception) { null }
        }.sorted()

        if (dates.isEmpty()) return 0

        var maxStreak = 1
        var tempStreak = 1

        for (i in 1 until dates.size) {
            val diff = (dates[i].time - dates[i - 1].time) / (1000 * 60 * 60 * 24)
            if (diff == 1L) {
                tempStreak++
                if (tempStreak > maxStreak) maxStreak = tempStreak
            } else if (diff > 1L) {
                tempStreak = 1
            }
        }
        return maxStreak
    }
}
