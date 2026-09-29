package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.util.PlantUtils

/**
 * Visual 7-day streak calendar row under the main hero card on the Home dashboard.
 * - Shows M, T, W, T, F, S, S
 * - Completed days: Green filled glass circles with checkmark/accent
 * - Incomplete days: Soft translucent circles
 * - Selected/current day: Subtle green glow
 * - Derived from real user streak & activity data
 */
@Composable
fun StreakCalendarRow(
    weekDays: List<PlantUtils.DayItem>,
    activeDates: Set<String>,
    currentStreak: Int,
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("streak_calendar_row"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        weekDays.forEach { day ->
            val isCompleted = PlantUtils.isDateInStreak(
                dateStr = day.dateString,
                streakCount = currentStreak,
                activeDates = activeDates
            )
            val isToday = day.isToday
            val isSelected = day.dateString == selectedDate

            StreakDayItem(
                day = day,
                isCompleted = isCompleted,
                isToday = isToday,
                isSelected = isSelected,
                onClick = { onDateSelected(day.dateString) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StreakDayItem(
    day: PlantUtils.DayItem,
    isCompleted: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val highlightGlow = isToday || isSelected
    val accessibilityLabel = buildString {
        append(day.dayName)
        append(", ")
        append(day.dayNumber)
        if (isToday) append(" (Today)")
        if (isCompleted) append(" - Streak Completed") else append(" - Incomplete")
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .semantics {
                contentDescription = accessibilityLabel
                role = Role.Button
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp)
            .testTag("streak_day_item_${day.dayName}")
    ) {
        // Day of Week Letter: M, T, W, T, F, S, S
        Text(
            text = day.dayLetter,
            style = MaterialTheme.typography.labelSmall.copy(
                color = when {
                    isToday -> PlantPrimary
                    isSelected -> PlantPrimary
                    isCompleted -> PlantText
                    else -> PlantTextSecondary
                },
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 13.sp
            )
        )

        Spacer(modifier = Modifier.height(7.dp))

        // Visual Circle (Filled green glass vs Soft translucent)
        val isDark = PlantTheme.isDark
        val completedBorderColor = if (isDark) Color(0x40FFFFFF) else Color(0xBFFFFFFF)
        val incompleteBorderColor = if (isDark) Color(0x2612B76A) else Color(0x3812B76A)

        Box(
            modifier = Modifier
                .size(34.dp)
                .then(
                    if (highlightGlow) {
                        Modifier.shadow(
                            elevation = 8.dp,
                            shape = CircleShape,
                            spotColor = PlantPrimary,
                            ambientColor = PlantPrimary.copy(alpha = 0.5f)
                        )
                    } else if (isCompleted) {
                        Modifier.shadow(
                            elevation = 3.dp,
                            shape = CircleShape,
                            spotColor = PlantPrimary.copy(alpha = 0.35f)
                        )
                    } else {
                        Modifier
                    }
                )
                .clip(CircleShape)
                .background(
                    if (isCompleted) {
                        // Green filled glass circle
                        Brush.linearGradient(
                            listOf(
                                PlantPrimary,
                                Color(0xFF10B981)
                            )
                        )
                    } else if (isToday || isSelected) {
                        // Subtle green glowing translucent fill
                        Brush.linearGradient(
                            listOf(
                                Color(0x3312B76A),
                                Color(0x1F12B76A)
                            )
                        )
                    } else {
                        // Soft translucent circle
                        Brush.linearGradient(
                            listOf(
                                if (isDark) Color(0x1F162A20) else Color(0x1812B76A),
                                if (isDark) Color(0x12162A20) else Color(0x1012B76A)
                            )
                        )
                    }
                )
                .border(
                    width = when {
                        isToday || isSelected -> 2.dp
                        isCompleted -> 1.dp
                        else -> 1.dp
                    },
                    color = when {
                        isToday || isSelected -> PlantPrimary
                        isCompleted -> completedBorderColor
                        else -> incompleteBorderColor
                    },
                    shape = CircleShape
                )
                .testTag("streak_day_circle_${day.dayName}"),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Completed",
                    tint = Color.White,
                    modifier = Modifier
                        .size(18.dp)
                        .testTag("streak_day_check_${day.dayName}")
                )
            } else if (isToday) {
                // Subtle green center dot for current day
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(PlantPrimary)
                )
            } else {
                // Subtle soft translucent dot
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0x3312B76A))
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Day Number (e.g. 21, 22...)
        Text(
            text = "${day.dayNumber}",
            style = MaterialTheme.typography.labelSmall.copy(
                color = when {
                    isToday -> PlantPrimary
                    isSelected -> PlantPrimary
                    isCompleted -> PlantText
                    else -> PlantTextSecondary.copy(alpha = 0.75f)
                },
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.sp
            )
        )
    }
}
