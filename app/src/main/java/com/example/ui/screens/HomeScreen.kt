package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.PlantViewModel
import com.example.ui.viewmodel.ScreenTab
import com.example.ui.viewmodel.TasksSubTab
import com.example.util.PlantUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: PlantViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val completedCount by viewModel.todayCompletedCount.collectAsState()
    val totalCount by viewModel.todayTotalCount.collectAsState()
    val completionPct by viewModel.todayCompletionPercentage.collectAsState()
    val tasksRemaining by viewModel.todayTasksRemaining.collectAsState()

    val activeProductivityDates by viewModel.activeProductivityDates.collectAsState()
    val dynamicStreak by viewModel.dynamicStreak.collectAsState()

    val todayFocusMin by viewModel.todayFocusMinutes.collectAsState()
    val todayFocusSessionsCount by viewModel.todayFocusSessionCount.collectAsState()

    val todayHabitsDone by viewModel.todayHabitsCompleted.collectAsState()
    val todayHabitsTotal by viewModel.todayHabitsTotal.collectAsState()
    val habitsBestStreak by viewModel.habitsBestStreak.collectAsState()

    val upcomingScheduled by viewModel.upcomingScheduledSessions.collectAsState()

    val (greetingTitle, _) = androidx.compose.runtime.remember { PlantUtils.getDynamicGreeting() }
    val userName = currentUser?.name?.ifBlank { "Botanist" } ?: "Botanist"
    val userXp = currentUser?.xp ?: 0

    val animatedTaskProgress by animateFloatAsState(
        targetValue = if (totalCount == 0) 0f else (completedCount.toFloat() / totalCount).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "todayProgress"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PlantBackground)
    ) {
        // Subtle ambient botanical background glow
        val ambientSecondaryGlow = PlantTheme.colors.accentSoftBorder
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PlantPrimary.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.1f, height * 0.12f),
                    radius = width * 0.55f
                )
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ambientSecondaryGlow.copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.9f, height * 0.22f),
                    radius = width * 0.5f
                )
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Header / Greeting
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = greetingTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = PlantTextSecondary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = PlantText,
                                fontSize = 32.sp,
                                letterSpacing = (-0.5).sp
                            )
                        )
                    }

                    // Glass icon buttons: Reminders shortcut & Profile shortcut
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .shadow(elevation = 4.dp, shape = CircleShape, spotColor = if (PlantTheme.isDark) Color(0x33000000) else Color(0x12000000))
                                .clip(CircleShape)
                                .background(PlantTheme.colors.cardBackground)
                                .border(1.dp, PlantTheme.colors.cardBorder, CircleShape)
                                .clickable { viewModel.toggleScheduleDialog(true) }
                                .testTag("btn_home_schedule_alert"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsNone,
                                contentDescription = "Reminders",
                                tint = PlantText,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .shadow(elevation = 4.dp, shape = CircleShape, spotColor = if (PlantTheme.isDark) Color(0x33000000) else Color(0x12000000))
                                .clip(CircleShape)
                                .background(PlantTheme.colors.cardBackground)
                                .border(1.dp, PlantTheme.colors.cardBorder, CircleShape)
                                .clickable { viewModel.setTab(ScreenTab.PROFILE) }
                                .testTag("btn_home_profile"),
                            contentAlignment = Alignment.Center
                        ) {
                            val photoUrl = currentUser?.photoURL
                            if (!photoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = photoUrl,
                                    contentDescription = "Profile",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = "Profile",
                                    tint = PlantTheme.colors.accent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Date Selector
            item {
                DateStrip(
                    selectedDate = selectedDate,
                    onDateSelected = { viewModel.setSelectedDate(it) },
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }

            // 3. TODAY'S PROGRESS — LARGE HERO CARD
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(28.dp),
                            spotColor = if (PlantTheme.isDark) Color(0x33000000) else Color(0x2212B76A),
                            ambientColor = Color(0x0C12B76A)
                        )
                        .border(
                            1.2.dp,
                            PlantTheme.colors.cardBorder,
                            RoundedCornerShape(28.dp)
                        )
                        .testTag("card_today_progress_hero"),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = PlantTheme.colors.cardBackground)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Top specular edge highlight
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.5.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color.Transparent,
                                            if (PlantTheme.isDark) Color(0x26FFFFFF) else Color.White,
                                            Color.Transparent
                                        )
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp)
                        ) {
                            // Header row inside hero card
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(PlantPrimary)
                                    )
                                    Text(
                                        text = "Today's Progress",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = PlantTextSecondary,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp,
                                            letterSpacing = (-0.2).sp
                                        )
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(PlantSoftGreen)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (totalCount == 0) "Planning" else if (completedCount == totalCount) "Completed" else "In Progress",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = PlantPrimaryDark,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Main Progress Content: Left info + Right radial progress ring
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    // Large dynamic hero percentage
                                    Text(
                                        text = "$completionPct%",
                                        style = MaterialTheme.typography.displayMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = PlantPrimary,
                                            fontSize = 40.sp,
                                            letterSpacing = (-1.0).sp
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Task completion text
                                    Text(
                                        text = if (totalCount == 0) "No tasks planned today" else "$completedCount of $totalCount tasks completed",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = PlantText,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Smooth animated horizontal progress bar
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(9.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(PlantTheme.colors.progressTrack)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(animatedTaskProgress)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(
                                                            PlantPrimaryDark,
                                                            PlantPrimary,
                                                            Color(0xFF34D399)
                                                        )
                                                    )
                                                )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(18.dp))

                                // Circular radial progress ring with centered plant visual
                                val ringTrackColor = PlantTheme.colors.progressTrack
                                Box(
                                    modifier = Modifier
                                        .size(96.dp)
                                        .shadow(elevation = 4.dp, shape = CircleShape, spotColor = PlantPrimary.copy(alpha = 0.2f))
                                        .clip(CircleShape)
                                        .background(PlantTheme.colors.surfaceElevated)
                                        .border(1.dp, PlantTheme.colors.border, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.size(96.dp)) {
                                        val strokeWidthPx = 8.dp.toPx()
                                        val pad = strokeWidthPx / 2f
                                        val arcSize = Size(size.width - strokeWidthPx, size.height - strokeWidthPx)

                                        // Background Track Ring
                                        drawArc(
                                            color = ringTrackColor,
                                            startAngle = -90f,
                                            sweepAngle = 360f,
                                            useCenter = false,
                                            topLeft = Offset(pad, pad),
                                            size = arcSize,
                                            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                                        )

                                        // Animated Progress Sweep Ring
                                        if (animatedTaskProgress > 0f) {
                                            drawArc(
                                                brush = Brush.sweepGradient(
                                                    listOf(
                                                        PlantPrimary,
                                                        Color(0xFF34D399),
                                                        PlantPrimaryDark
                                                    )
                                                ),
                                                startAngle = -90f,
                                                sweepAngle = (animatedTaskProgress * 360f).coerceIn(0f, 360f),
                                                useCenter = false,
                                                topLeft = Offset(pad, pad),
                                                size = arcSize,
                                                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                                            )
                                        }
                                    }

                                    // Sprout Graphic in center
                                    SeedlingPlant(
                                        size = 60.dp,
                                        progress = if (totalCount == 0) 0.35f else (completedCount.toFloat() / totalCount).coerceIn(0.15f, 1f),
                                        isDark = PlantTheme.isDark
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Compact Hero Stats Row inside Today's Progress Card
                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(PlantTheme.colors.surfaceElevated)
                                    .border(1.dp, PlantTheme.colors.border, RoundedCornerShape(18.dp))
                                    .padding(vertical = 12.dp, horizontal = 16.dp)
                            ) {
                                val density = androidx.compose.ui.platform.LocalDensity.current
                                val fontScale = density.fontScale
                                val minRequiredWidthFor3Columns = 280.dp * fontScale

                                if (maxWidth >= minRequiredWidthFor3Columns) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        HeroStatItem(
                                            modifier = Modifier.weight(1f),
                                            label = "Focus Today",
                                            value = PlantUtils.formatMinutes(todayFocusMin),
                                            icon = Icons.Filled.Timer,
                                            iconTint = PlantPrimaryDark
                                        )

                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .height(28.dp)
                                                .background(PlantTheme.colors.divider)
                                        )

                                        HeroStatItem(
                                            modifier = Modifier.weight(1f),
                                            label = "Streak",
                                            value = "${dynamicStreak.first} days",
                                            icon = Icons.Filled.LocalFireDepartment,
                                            iconTint = PlantOrange
                                        )

                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .height(28.dp)
                                                .background(PlantTheme.colors.divider)
                                        )

                                        HeroStatItem(
                                            modifier = Modifier.weight(1f),
                                            label = "XP Earned",
                                            value = "$userXp XP",
                                            icon = Icons.Filled.Eco,
                                            iconTint = PlantPrimary
                                        )
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        HeroStatItem(
                                            modifier = Modifier.fillMaxWidth(),
                                            label = "Focus Today",
                                            value = PlantUtils.formatMinutes(todayFocusMin),
                                            icon = Icons.Filled.Timer,
                                            iconTint = PlantPrimaryDark
                                        )

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .background(PlantTheme.colors.divider)
                                        )

                                        HeroStatItem(
                                            modifier = Modifier.fillMaxWidth(),
                                            label = "Streak",
                                            value = "${dynamicStreak.first} days",
                                            icon = Icons.Filled.LocalFireDepartment,
                                            iconTint = PlantOrange
                                        )

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .background(PlantTheme.colors.divider)
                                        )

                                        HeroStatItem(
                                            modifier = Modifier.fillMaxWidth(),
                                            label = "XP Earned",
                                            value = "$userXp XP",
                                            icon = Icons.Filled.Eco,
                                            iconTint = PlantPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. YOUR STREAK — SMALLER THAN TODAY'S PROGRESS
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(24.dp),
                            spotColor = if (PlantTheme.isDark) Color(0x33000000) else Color(0x14111614)
                        )
                        .border(
                            1.dp,
                            PlantTheme.colors.cardBorder,
                            RoundedCornerShape(24.dp)
                        )
                        .testTag("card_prominent_streak"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = PlantTheme.colors.cardBackground)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "🔥",
                                    fontSize = 20.sp
                                )
                                Text(
                                    text = "Your Streak",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = PlantText,
                                        fontSize = 18.sp
                                    )
                                )
                            }

                            Text(
                                text = "Longest streak: ${dynamicStreak.second} days",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = PlantTextSecondary,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${dynamicStreak.first}",
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PlantOrange,
                                    fontSize = 36.sp,
                                    letterSpacing = (-0.5).sp
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (dynamicStreak.first == 1) "day streak" else "days streak",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = PlantText,
                                    fontSize = 17.sp
                                ),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Weekly streak visualization (7 days: Mon through Sun)
                        val weekDays = androidx.compose.runtime.remember { PlantUtils.getCurrentWeekDays() }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(PlantTheme.colors.surfaceElevated)
                                .border(1.dp, PlantTheme.colors.border, RoundedCornerShape(18.dp))
                                .padding(horizontal = 8.dp, vertical = 12.dp)
                        ) {
                            StreakCalendarRow(
                                weekDays = weekDays,
                                activeDates = activeProductivityDates,
                                currentStreak = dynamicStreak.first,
                                selectedDate = selectedDate,
                                onDateSelected = { viewModel.setSelectedDate(it) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // 5. FOCUS / PRODUCTIVITY SUMMARY
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SummaryGridTile(
                            title = "Current Streak",
                            value = "${dynamicStreak.first}d",
                            subtitle = "Best: ${dynamicStreak.second}d",
                            icon = Icons.Filled.LocalFireDepartment,
                            accentColor = PlantOrange,
                            testTag = "tile_streak_summary",
                            modifier = Modifier.weight(1f),
                            onClick = { }
                        )

                        SummaryGridTile(
                            title = "Focus Time",
                            value = PlantUtils.formatMinutes(todayFocusMin),
                            subtitle = "$todayFocusSessionsCount sessions",
                            icon = Icons.Filled.Timer,
                            accentColor = PlantPrimaryDark,
                            testTag = "tile_focus_summary",
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.setTab(ScreenTab.FOCUS) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SummaryGridTile(
                            title = "Tasks Done",
                            value = "$completedCount / $totalCount",
                            subtitle = if (tasksRemaining > 0) "$tasksRemaining remaining" else "All completed",
                            icon = Icons.Filled.CheckCircle,
                            accentColor = PlantPrimary,
                            testTag = "tile_tasks_summary",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                viewModel.setTasksSubTab(TasksSubTab.TASKS)
                                viewModel.setTab(ScreenTab.TASKS)
                            }
                        )

                        SummaryGridTile(
                            title = "Habits Done",
                            value = "$todayHabitsDone / $todayHabitsTotal",
                            subtitle = if (habitsBestStreak > 0) "Streak: ${habitsBestStreak}d" else "Track daily",
                            icon = Icons.Filled.Eco,
                            accentColor = Color(0xFF059669),
                            testTag = "tile_habits_summary",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                viewModel.setTasksSubTab(TasksSubTab.HABITS)
                                viewModel.setTab(ScreenTab.TASKS)
                            }
                        )
                    }
                }
            }

            // Upcoming Scheduled Reminder Banner (if any)
            if (upcomingScheduled.isNotEmpty()) {
                val nextSession = upcomingScheduled.first()
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                val sessionTime = timeFormat.format(Date(nextSession.scheduledTimeMillis))

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setTab(ScreenTab.FOCUS) }
                            .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = PlantPrimary),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (PlantTheme.isDark) Color(0x3312B76A) else Color(0xF2EAF8F0)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PlantSoftGreenBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(PlantPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.NotificationsActive,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Approaching: ${nextSession.title}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = PlantPrimaryDark,
                                        fontSize = 15.sp
                                    )
                                )
                                Text(
                                    text = "Starts at $sessionTime (${nextSession.durationMinutes}m) • ${nextSession.reminderMinutesBefore}m reminder",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = PlantText,
                                        fontSize = 13.sp
                                    )
                                )
                            }

                            Button(
                                onClick = { viewModel.startScheduledFocusNow(nextSession) },
                                colors = ButtonDefaults.buttonColors(containerColor = PlantPrimaryDark),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Start", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            // 6. Optional Quick Actions (4 Clean Glass Tiles)
            item {
                QuickActionButtons(
                    onFocusClick = { viewModel.setTab(ScreenTab.FOCUS) },
                    onAddTaskClick = { viewModel.toggleAddTaskDialog(true) },
                    onHabitClick = { viewModel.toggleAddHabitDialog(true) },
                    onGoalClick = { viewModel.toggleAddGoalDialog(true) }
                )
            }
        }
    }
}

@Composable
private fun HeroStatItem(
    label: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = PlantText,
                    fontSize = 14.sp
                ),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = PlantTextSecondary,
                    fontSize = 11.sp
                ),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SummaryGridTile(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = if (PlantTheme.isDark) Color(0x33000000) else Color(0x0C111614))
            .border(1.dp, PlantTheme.colors.cardBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PlantTheme.colors.cardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(17.dp)
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = PlantTextSecondary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = PlantText,
                    fontSize = 20.sp,
                    letterSpacing = (-0.3).sp
                )
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = PlantTextSecondary,
                    fontSize = 12.sp
                )
            )
        }
    }
}
