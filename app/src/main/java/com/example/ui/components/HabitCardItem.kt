package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Habit
import com.example.ui.theme.*
import com.example.util.PlantUtils

@Composable
fun HabitCardItem(
    habit: Habit,
    weekDays: List<PlantUtils.DayItem>,
    todayStr: String,
    onToggleToday: () -> Unit,
    onToggleDate: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompletedToday = habit.completionHistory.contains(todayStr)
    val streak = habit.currentStreak
    val isDark = PlantTheme.isDark
    val orangePillBg = if (isDark) Color(0x33F79009) else PlantOrangeSoft
    val trackBg = PlantTheme.colors.progressTrack
    val borderColor = PlantTheme.colors.cardBorder

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 2.dp else 4.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = if (isDark) Color(0x33000000) else Color(0x10111614)
            )
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .testTag("habit_item_${habit.id}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PlantTheme.colors.cardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header: Checkbox, Title, Category & Frequency, Streak badge, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Today Toggle Checkbox
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (isCompletedToday) PlantPrimary else Color.Transparent)
                        .border(
                            2.dp,
                            if (isCompletedToday) PlantPrimary else PlantTheme.colors.border,
                            CircleShape
                        )
                        .clickable(onClick = onToggleToday)
                        .testTag("btn_toggle_habit_today_${habit.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompletedToday) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Completed Today",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PlantText,
                            fontSize = 17.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${habit.category} • ${habit.frequency}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = PlantTextSecondary,
                            fontSize = 13.sp
                        )
                    )
                }

                // Streak counter pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(orangePillBg)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalFireDepartment,
                        contentDescription = "Streak",
                        tint = PlantOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$streak d",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PlantOrange,
                            fontSize = 13.5.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete Habit",
                        tint = PlantTextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Weekly Day Bubble Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekDays.forEach { day ->
                    val done = habit.completionHistory.contains(day.dateString)
                    val isToday = day.dateString == todayStr

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onToggleDate(day.dateString) }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = day.dayName.take(1),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                color = if (isToday) PlantPrimary else PlantTextSecondary,
                                fontSize = 12.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (done) PlantPrimary else trackBg)
                                .border(
                                    width = if (isToday) 1.5.dp else 0.dp,
                                    color = if (isToday) PlantPrimary else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (done) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Text(
                                    text = day.dayNumber.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isToday) PlantPrimary else PlantTextTertiary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
