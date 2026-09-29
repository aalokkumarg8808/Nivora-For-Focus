package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Habit
import com.example.ui.components.SeedlingPlant
import com.example.ui.theme.*
import com.example.ui.viewmodel.PlantViewModel
import com.example.util.PlantUtils

@Composable
fun HabitsScreen(
    viewModel: PlantViewModel,
    modifier: Modifier = Modifier
) {
    val habits by viewModel.allHabits.collectAsState()
    val todayStr = PlantUtils.getTodayDateString()
    val weekDays = PlantUtils.getCurrentWeekDays()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PlantBackground)
    ) {
        // Subtle ambient botanical background glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PlantPrimary.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.15f, height * 0.15f),
                    radius = width * 0.5f
                )
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Header with Add Button
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Habits",
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = PlantText,
                                fontSize = 32.sp,
                                letterSpacing = (-0.5).sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Consistent daily roots yield fruitful blooms",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = PlantTextSecondary,
                                fontSize = 14.sp
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(elevation = 6.dp, shape = CircleShape, spotColor = PlantPrimaryDark)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        PlantPrimary,
                                        PlantPrimaryDark
                                    )
                                )
                            )
                            .clickable { viewModel.toggleAddHabitDialog(true) }
                            .testTag("btn_add_habit"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add Habit",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // 2. Habits List or Empty State
            if (habits.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = PlantTheme.colors.cardBackground),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PlantTheme.colors.cardBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            SeedlingPlant(size = 72.dp, progress = 0.4f)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No habits created yet",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PlantText,
                                    fontSize = 18.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Start building small daily routines that grow over time.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = PlantTextSecondary,
                                    fontSize = 14.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.toggleAddHabitDialog(true) },
                                colors = ButtonDefaults.buttonColors(containerColor = PlantPrimary),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("Create First Habit", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        }
                    }
                }
            } else {
                items(habits, key = { it.id }) { habit ->
                    HabitCardItem(
                        habit = habit,
                        weekDays = weekDays,
                        todayStr = todayStr,
                        onToggleToday = { viewModel.toggleHabitForDate(habit.id, todayStr) },
                        onToggleDate = { date -> viewModel.toggleHabitForDate(habit.id, date) },
                        onDelete = { viewModel.deleteHabit(habit.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HabitCardItem(
    habit: Habit,
    weekDays: List<PlantUtils.DayItem>,
    todayStr: String,
    onToggleToday: () -> Unit,
    onToggleDate: (String) -> Unit,
    onDelete: () -> Unit
) {
    val isCompletedToday = habit.completionHistory.contains(todayStr)
    val streak = habit.currentStreak
    val isDark = PlantTheme.isDark
    val orangePillBg = if (isDark) Color(0x33F79009) else PlantOrangeSoft
    val trackBg = PlantTheme.colors.progressTrack
    val borderColor = PlantTheme.colors.cardBorder

    Card(
        modifier = Modifier
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
            // Header: Title, Category, Streak badge, Checkbox & Delete
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

            // Weekly Day Bubble Strip (generous padding, not crammed)
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
