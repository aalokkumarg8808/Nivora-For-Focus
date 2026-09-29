package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.SeedlingPlant
import com.example.ui.theme.*
import com.example.ui.viewmodel.PlantViewModel
import com.example.ui.viewmodel.ScreenTab
import com.example.ui.viewmodel.TimerMode
import com.example.util.PlantUtils
import com.example.util.SoundscapeManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusSessionScreen(
    viewModel: PlantViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.setTab(ScreenTab.HOME)
    }

    val currentMode by viewModel.timerMode.collectAsState()
    val remainingSecs by viewModel.timerRemainingSeconds.collectAsState()
    val totalSecs by viewModel.timerDurationSeconds.collectAsState()
    val isRunning by viewModel.isTimerRunning.collectAsState()
    val category by viewModel.focusCategory.collectAsState()
    val focusTitle by viewModel.focusTitle.collectAsState()
    val scene by viewModel.focusScene.collectAsState()
    val notifySessionEnd by viewModel.notifySessionEnd.collectAsState()
    val showEditDialog by viewModel.showEditFocusDialog.collectAsState()

    // Soundscape state
    val isSoundscapePlaying by viewModel.soundscapePlaying.collectAsState()
    val soundscapeVol by viewModel.soundscapeVolume.collectAsState()

    // Statistics derived from real focus time
    val todayFocusMin by viewModel.todayFocusMinutes.collectAsState()
    val todayAvgSessionMin by viewModel.todayAvgSessionMinutes.collectAsState()
    val totalFocusMinAllTime by viewModel.totalFocusMinutesAllTime.collectAsState()
    val weeklyFocusMin by viewModel.weeklyFocusMinutes.collectAsState()
    val allFocusSessions by viewModel.allFocusSessions.collectAsState()

    var showCategoryDialog by remember { mutableStateOf(false) }
    var showSceneDialog by remember { mutableStateOf(false) }

    val minutes = remainingSecs / 60
    val seconds = remainingSecs % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)
    val progress = if (totalSecs == 0) 1f else (totalSecs - remainingSecs).toFloat() / totalSecs

    if (showEditDialog) {
        EditFocusSessionDialog(
            currentTitle = focusTitle,
            currentDurationMinutes = totalSecs / 60,
            currentCategory = category,
            currentScene = scene,
            notifyWhenEnds = notifySessionEnd,
            onDismiss = { viewModel.toggleEditFocusDialog(false) },
            onSave = { newTitle, newDuration, newCategory, newScene, notifyEnd ->
                viewModel.updateFocusSessionConfig(newTitle, newDuration, newCategory, newScene, notifyEnd)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Focus Timer",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = PlantText,
                            fontSize = 20.sp,
                            letterSpacing = (-0.2).sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.setTab(ScreenTab.HOME) },
                        modifier = Modifier.testTag("btn_focus_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Home",
                            tint = PlantText
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleEditFocusDialog(true) },
                        modifier = Modifier.testTag("btn_edit_focus_session")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = "Edit Session",
                            tint = PlantPrimary
                        )
                    }
                    IconButton(
                        onClick = { viewModel.resetTimer() },
                        modifier = Modifier.testTag("btn_reset_focus")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Reset Timer",
                            tint = PlantTextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PlantBackground)
            )
        },
        containerColor = PlantBackground
    ) { padding ->
        val isDark = PlantTheme.isDark
        val progressTrackColor = PlantTheme.colors.progressTrack
        val dialBg1 = PlantTheme.colors.surfaceElevated
        val dialBg2 = PlantTheme.colors.cardBackground
        val cardBg = PlantTheme.colors.cardBackground
        val cardBorderColor = PlantTheme.colors.cardBorder
        val iconBg = PlantTheme.colors.accentSoft

        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Ambient botanical glow
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            PlantPrimary.copy(alpha = if (isDark) 0.12f else 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.5f, height * 0.35f),
                        radius = width * 0.6f
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Segment Pills (Focus / Short Break / Long Break)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .shadow(elevation = 2.dp, shape = RoundedCornerShape(24.dp))
                        .background(cardBg, RoundedCornerShape(24.dp))
                        .border(1.dp, cardBorderColor, RoundedCornerShape(24.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TimerMode.values().forEach { mode ->
                        val isSelected = currentMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) PlantPrimary else Color.Transparent)
                                .clickable { viewModel.setTimerMode(mode) }
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.title,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else PlantTextSecondary,
                                    fontSize = 13.5.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 2. Large Circular Dial with Sprouting Plant in Center
                Box(
                    modifier = Modifier
                        .size(268.dp)
                        .shadow(elevation = if (isDark) 12.dp else 6.dp, shape = CircleShape, spotColor = Color(0x33000000))
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(dialBg1, dialBg2)
                            )
                        )
                        .border(1.5.dp, cardBorderColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Circular progress ring
                    Canvas(modifier = Modifier.size(244.dp)) {
                        val stroke = 5.5.dp.toPx()
                        drawCircle(
                            color = progressTrackColor,
                            radius = size.minDimension / 2f - stroke / 2f,
                            style = Stroke(stroke)
                        )
                        drawArc(
                            color = PlantPrimary,
                            startAngle = -90f,
                            sweepAngle = progress * 360f,
                            useCenter = false,
                            style = Stroke(stroke, cap = StrokeCap.Round)
                        )
                    }

                    // Center Content: Plant + Digits + Label
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        SeedlingPlant(
                            size = 72.dp,
                            progress = progress,
                            isDark = isDark
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = timeFormatted,
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 40.sp,
                                letterSpacing = (-0.5).sp,
                                color = PlantText
                            )
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.clickable { viewModel.toggleEditFocusDialog(true) }
                        ) {
                            Text(
                                text = if (currentMode == TimerMode.FOCUS) focusTitle else "${currentMode.title} Session",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = PlantTextSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit",
                                tint = PlantPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // 3. Main Start/Pause Action Button
                Button(
                    onClick = {
                        if (isRunning) viewModel.pauseTimer() else viewModel.startTimer()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(18.dp), spotColor = PlantPrimary)
                        .testTag("btn_toggle_timer"),
                    colors = ButtonDefaults.buttonColors(containerColor = PlantPrimary),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isRunning) "Pause" else "Start",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRunning) "Pause Focus" else "Start Focus",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4. Category & Ambient Soundscape Glass Tiles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(78.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, cardBorderColor, RoundedCornerShape(20.dp))
                            .clickable { showCategoryDialog = true },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(iconBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Tag, contentDescription = null, tint = PlantPrimary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Category", style = MaterialTheme.typography.bodySmall.copy(color = PlantTextSecondary, fontSize = 12.sp))
                                Text(category, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = PlantText, fontSize = 15.sp))
                            }
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(78.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, cardBorderColor, RoundedCornerShape(20.dp))
                            .clickable { showSceneDialog = true },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isSoundscapePlaying) PlantSoftGreen else iconBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isSoundscapePlaying) Icons.Filled.GraphicEq else Icons.Filled.Park,
                                    contentDescription = null,
                                    tint = PlantPrimaryDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Soundscape", style = MaterialTheme.typography.bodySmall.copy(color = PlantTextSecondary, fontSize = 12.sp))
                                Text(scene, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = PlantText, fontSize = 15.sp))
                            }
                            IconButton(
                                onClick = { viewModel.toggleSoundscapePlayback() },
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("btn_toggle_soundscape")
                            ) {
                                Icon(
                                    imageVector = if (isSoundscapePlaying) Icons.Filled.PauseCircle else Icons.Filled.PlayCircle,
                                    contentDescription = "Play Soundscape",
                                    tint = PlantPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                // 5. Soundscape Volume Slider Control Strip
                if (scene != "None") {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSoundscapePlaying) PlantSoftGreen.copy(alpha = 0.5f) else cardBg
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSoundscapePlaying) PlantPrimary.copy(alpha = 0.35f) else cardBorderColor
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (soundscapeVol <= 0.01f) Icons.Filled.VolumeMute else if (soundscapeVol < 0.5f) Icons.Filled.VolumeDown else Icons.Filled.VolumeUp,
                                contentDescription = "Soundscape Volume",
                                tint = PlantPrimaryDark,
                                modifier = Modifier.size(20.dp)
                            )
                            Slider(
                                value = soundscapeVol,
                                onValueChange = { newVol ->
                                    viewModel.setSoundscapeVolume(newVol)
                                },
                                valueRange = 0f..1f,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("slider_soundscape_volume"),
                                colors = SliderDefaults.colors(
                                    thumbColor = PlantPrimary,
                                    activeTrackColor = PlantPrimary,
                                    inactiveTrackColor = PlantBorder
                                )
                            )
                            Text(
                                text = "${(soundscapeVol * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PlantPrimaryDark,
                                    fontSize = 12.sp
                                ),
                                modifier = Modifier.width(36.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 6. Completion Notification Control Strip
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (notifySessionEnd) PlantSoftGreen.copy(alpha = 0.4f) else cardBg
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (notifySessionEnd) PlantPrimary.copy(alpha = 0.3f) else cardBorderColor
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (notifySessionEnd) PlantPrimary.copy(alpha = 0.15f) else iconBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (notifySessionEnd) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsOff,
                                    contentDescription = "Notify on completion",
                                    tint = if (notifySessionEnd) PlantPrimaryDark else PlantTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Notify when session ends",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = PlantText,
                                        fontSize = 14.sp
                                    )
                                )
                                Text(
                                    text = if (notifySessionEnd) "Alert when timer reaches 0:00" else "Silent completion",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = PlantTextSecondary,
                                        fontSize = 11.5.sp
                                    )
                                )
                            }
                        }

                        Switch(
                            checked = notifySessionEnd,
                            onCheckedChange = { viewModel.setNotifySessionEnd(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PlantPrimary,
                                uncheckedThumbColor = PlantTextSecondary,
                                uncheckedTrackColor = PlantTheme.colors.surfaceElevated
                            ),
                            modifier = Modifier.testTag("switch_notify_session_end")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 7. FOCUS STATISTICS CARD (FOCUSED DYNAMIC METRICS)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(22.dp), spotColor = if (isDark) Color(0x33000000) else Color(0x10111614))
                        .border(1.dp, cardBorderColor, RoundedCornerShape(22.dp))
                        .testTag("card_focus_statistics"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(PlantSoftGreen),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Timer,
                                        contentDescription = null,
                                        tint = PlantPrimaryDark,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "Focus Statistics",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = PlantText,
                                        fontSize = 17.sp
                                    )
                                )
                            }

                            if (allFocusSessions.isNotEmpty()) {
                                Text(
                                    text = "All-time: ${PlantUtils.formatMinutes(totalFocusMinAllTime)}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = PlantPrimaryDark,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.5.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (todayFocusMin > 0) PlantUtils.formatMinutes(todayFocusMin) else "0m",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = PlantText,
                                        fontSize = 24.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Focus Today",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = PlantTextSecondary,
                                        fontSize = 12.sp
                                    )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(38.dp)
                                    .background(PlantTheme.colors.divider)
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (todayAvgSessionMin > 0) "${todayAvgSessionMin}m" else "—",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = PlantText,
                                        fontSize = 24.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Avg Duration",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = PlantTextSecondary,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }

                        if (weeklyFocusMin > 0) {
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = PlantBorder.copy(alpha = 0.5f), thickness = 0.8.dp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "This Week's Focus:",
                                    style = MaterialTheme.typography.bodySmall.copy(color = PlantTextSecondary, fontSize = 12.sp)
                                )
                                Text(
                                    text = PlantUtils.formatMinutes(weeklyFocusMin),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = PlantPrimaryDark,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Category Dialog
    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            containerColor = PlantTheme.colors.surfaceElevated,
            title = { Text("Choose Category", fontWeight = FontWeight.Bold, color = PlantText) },
            text = {
                Column {
                    listOf("Study", "Code", "Reading", "Project", "Writing", "Health").forEach { cat ->
                        Text(
                            text = cat,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setFocusCategory(cat)
                                    showCategoryDialog = false
                                }
                                .padding(vertical = 12.dp),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (category == cat) FontWeight.Bold else FontWeight.Normal,
                                color = if (category == cat) PlantPrimary else PlantText
                            )
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCategoryDialog = false }) { Text("Close", color = PlantPrimary) }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Soundscape Scene Dialog
    if (showSceneDialog) {
        AlertDialog(
            onDismissRequest = { showSceneDialog = false },
            containerColor = PlantTheme.colors.surfaceElevated,
            title = { Text("Ambient Soundscape", fontWeight = FontWeight.Bold, color = PlantText) },
            text = {
                Column {
                    SoundscapeManager.availableSoundscapes.forEach { sc ->
                        val isSel = scene == sc
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setFocusScene(sc)
                                    showSceneDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = sc,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) PlantPrimary else PlantText
                                )
                            )
                            if (isSel) {
                                Icon(Icons.Filled.Check, contentDescription = "Selected", tint = PlantPrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSceneDialog = false }) { Text("Close", color = PlantPrimary) }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFocusSessionDialog(
    currentTitle: String,
    currentDurationMinutes: Int,
    currentCategory: String,
    currentScene: String,
    notifyWhenEnds: Boolean,
    onDismiss: () -> Unit,
    onSave: (title: String, durationMinutes: Int, category: String, scene: String, notifyEnd: Boolean) -> Unit
) {
    var title by remember { mutableStateOf(currentTitle) }
    var category by remember { mutableStateOf(currentCategory) }
    var scene by remember { mutableStateOf(currentScene) }
    var notifyEnd by remember { mutableStateOf(notifyWhenEnds) }
    var customDurationText by remember { mutableStateOf(currentDurationMinutes.toString()) }
    var customError by remember { mutableStateOf<String?>(null) }

    val categories = listOf("Study", "Code", "Reading", "Project", "Writing", "Health")
    val soundscapes = SoundscapeManager.availableSoundscapes

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
                .heightIn(max = 680.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = PlantSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, PlantTheme.colors.cardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    text = "Edit Focus Session",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = PlantText
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Session Name
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Session Name / Focus Goal") },
                    placeholder = { Text("e.g. Deep Study, Chapter 4 Review") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_focus_session_title"),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PlantPrimary,
                        unfocusedBorderColor = PlantBorder,
                        focusedTextColor = PlantText,
                        unfocusedTextColor = PlantText,
                        focusedContainerColor = PlantTheme.colors.inputBackground,
                        unfocusedContainerColor = PlantTheme.colors.inputBackground
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Manual Custom Duration Input (Direct, no fixed dedicated presets)
                val parsedDuration = customDurationText.toIntOrNull()
                Text(
                    text = "Focus Duration (Minutes)",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = PlantTextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = customDurationText,
                    onValueChange = { txt ->
                        val clean = txt.filter { it.isDigit() }
                        customDurationText = clean
                        val parsed = clean.toIntOrNull()
                        if (parsed == null || parsed <= 0) {
                            customError = "Enter a duration between 1 and 720 minutes"
                        } else if (parsed > 720) {
                            customError = "Maximum duration is 720 minutes (12 hours)"
                        } else {
                            customError = null
                        }
                    },
                    label = { Text("Duration") },
                    placeholder = { Text("Enter minutes (1 - 720)") },
                    suffix = { Text("min", fontWeight = FontWeight.Bold, color = PlantPrimaryDark) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = customError != null,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_edit_custom_duration"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PlantPrimary,
                        unfocusedBorderColor = PlantBorder,
                        focusedTextColor = PlantText,
                        unfocusedTextColor = PlantText,
                        focusedContainerColor = PlantTheme.colors.inputBackground,
                        unfocusedContainerColor = PlantTheme.colors.inputBackground
                    )
                )

                if (customError != null) {
                    Text(
                        text = customError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        modifier = Modifier.padding(start = 6.dp, top = 4.dp)
                    )
                }

                // Quick Increment / Decrement Steppers for convenience
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(-15, -5, +5, +15).forEach { step ->
                        OutlinedButton(
                            onClick = {
                                val cur = customDurationText.toIntOrNull() ?: 25
                                val next = (cur + step).coerceIn(1, 720)
                                customDurationText = next.toString()
                                customError = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = PlantTheme.colors.surfaceElevated
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PlantBorder)
                        ) {
                            Text(
                                text = if (step > 0) "+$step" else "$step",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (step > 0) PlantPrimary else PlantTextSecondary,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Category Selector
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = PlantTextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.take(4).forEach { cat ->
                        val isSelected = category == cat
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) PlantSoftGreen else PlantSurfaceVariant)
                                .border(
                                    1.dp,
                                    if (isSelected) PlantPrimary else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { category = cat }
                                .padding(vertical = 8.dp)
                                .testTag("chip_focus_cat_$cat"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) PlantPrimaryDark else PlantTextSecondary,
                                    fontSize = 11.5.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Soundscape Selector
                Text(
                    text = "Ambient Soundscape",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = PlantTextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    soundscapes.take(4).forEach { sc ->
                        val isSelected = scene == sc
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) PlantSoftGreen else PlantSurfaceVariant)
                                .border(
                                    1.dp,
                                    if (isSelected) PlantPrimary else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { scene = sc }
                                .padding(vertical = 8.dp)
                                .testTag("chip_focus_scene_$sc"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = sc,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) PlantPrimaryDark else PlantTextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Completion Notification Switch
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (notifyEnd) PlantSoftGreen.copy(alpha = 0.5f) else PlantSurfaceVariant
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (notifyEnd) PlantPrimary.copy(alpha = 0.4f) else Color.Transparent
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Notify when session ends",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = PlantText,
                                    fontSize = 13.5.sp
                                )
                            )
                            Text(
                                text = "Local notification at 0:00",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = PlantTextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        Switch(
                            checked = notifyEnd,
                            onCheckedChange = { notifyEnd = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PlantPrimary,
                                uncheckedThumbColor = PlantTextSecondary,
                                uncheckedTrackColor = PlantSurfaceVariant
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = PlantTextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    val isDurationValid = parsedDuration != null && parsedDuration in 1..720

                    Button(
                        onClick = {
                            val finalDuration = customDurationText.toIntOrNull()?.coerceIn(1, 720) ?: 25
                            onSave(title, finalDuration, category, scene, notifyEnd)
                        },
                        enabled = isDurationValid,
                        colors = ButtonDefaults.buttonColors(containerColor = PlantPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("btn_save_focus_edit")
                    ) {
                        Text("Save Configuration", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
