package com.example.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Task
import com.example.ui.theme.*
import com.example.util.PlantUtils
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChooserSheet(
    onDismiss: () -> Unit,
    onChooseTask: () -> Unit,
    onChooseHabit: () -> Unit,
    onChooseGoal: () -> Unit,
    onChooseScheduleFocus: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = PlantSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "What would you like to grow?",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = PlantText
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            ChooserOptionItem(
                title = "New Task",
                subtitle = "Turn plans into progress with subtasks",
                icon = Icons.Filled.CheckCircle,
                badgeColor = PlantSoftGreen,
                iconTint = if (PlantTheme.isDark) PlantPrimary else PlantPrimaryDark,
                testTag = "choose_task_option",
                onClick = {
                    onDismiss()
                    onChooseTask()
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            ChooserOptionItem(
                title = "Schedule Focus Session",
                subtitle = "Plan a session with approaching reminder notifications",
                icon = Icons.Filled.NotificationsActive,
                badgeColor = PlantSoftGreen,
                iconTint = if (PlantTheme.isDark) PlantPrimary else PlantPrimaryDark,
                testTag = "choose_schedule_option",
                onClick = {
                    onDismiss()
                    onChooseScheduleFocus()
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            ChooserOptionItem(
                title = "New Habit",
                subtitle = "Build daily streaks and track consistency",
                icon = Icons.Filled.Eco,
                badgeColor = PlantSoftGreen,
                iconTint = if (PlantTheme.isDark) PlantPrimary else PlantPrimaryDark,
                testTag = "choose_habit_option",
                onClick = {
                    onDismiss()
                    onChooseHabit()
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            ChooserOptionItem(
                title = "New Goal",
                subtitle = "Set milestones towards a bigger milestone",
                icon = Icons.Filled.TrackChanges,
                badgeColor = PlantOrangeSoft,
                iconTint = PlantOrange,
                testTag = "choose_goal_option",
                onClick = {
                    onDismiss()
                    onChooseGoal()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ChooserOptionItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badgeColor: Color,
    iconTint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(PlantSurfaceVariant)
            .clickable(onClick = onClick)
            .padding(16.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(badgeColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = PlantText
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = PlantTextSecondary
                )
            )
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = PlantTextTertiary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        category: String,
        dueDate: String,
        dueTime: String,
        reminderEnabled: Boolean,
        reminderRepeat: String,
        reminderDays: List<Int>
    ) -> Unit,
    initialTask: Task? = null
) {
    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var category by remember { mutableStateOf(initialTask?.category ?: "Study") }
    var dueDate by remember { mutableStateOf(initialTask?.dueDate?.ifBlank { PlantUtils.getTodayDateString() } ?: PlantUtils.getTodayDateString()) }
    var dueTime by remember { mutableStateOf(initialTask?.dueTime ?: "") }
    var reminderEnabled by remember { mutableStateOf(initialTask?.reminderEnabled ?: false) }
    var reminderRepeat by remember { mutableStateOf(initialTask?.reminderRepeat ?: "Once") }
    val reminderDays = remember {
        mutableStateListOf<Int>().apply {
            if (initialTask != null && initialTask.reminderDays.isNotEmpty()) {
                addAll(initialTask.reminderDays)
            } else {
                addAll(listOf(Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY))
            }
        }
    }

    var showTimePickerDialog by remember { mutableStateOf(false) }

    // Permission launcher for Android 13+ (POST_NOTIFICATIONS)
    var hasRequestedPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Proceed with confirm
        onConfirm(
            title.trim(),
            category,
            dueDate,
            dueTime.trim(),
            reminderEnabled,
            reminderRepeat,
            reminderDays.toList()
        )
    }

    val categories = listOf("Study", "Personal", "Health", "Work")
    val repeatOptions = listOf("Once", "Daily", "Custom Days")
    val daysOfWeek = listOf(
        Pair("Mon", Calendar.MONDAY),
        Pair("Tue", Calendar.TUESDAY),
        Pair("Wed", Calendar.WEDNESDAY),
        Pair("Thu", Calendar.THURSDAY),
        Pair("Fri", Calendar.FRIDAY),
        Pair("Sat", Calendar.SATURDAY),
        Pair("Sun", Calendar.SUNDAY)
    )
    val quickTimes = listOf("9:00 AM", "10:30 AM", "2:00 PM", "5:30 PM", "8:00 PM")

    if (showTimePickerDialog) {
        TaskTimePickerDialog(
            initialTimeStr = dueTime,
            onDismiss = { showTimePickerDialog = false },
            onConfirm = { selectedTime ->
                dueTime = selectedTime
                reminderEnabled = true // Auto-turn ON reminder when time is picked
                showTimePickerDialog = false
            }
        )
    }

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
                    text = if (initialTask != null) "Edit Task" else "Add New Task",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = PlantText
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    placeholder = { Text("e.g. Drink Water") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_task_title"),
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

                // Time Field (Optional) & Reminder Control
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Time (Optional)",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = PlantTextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    if (dueTime.isNotBlank()) {
                        Text(
                            text = "Clear",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = PlantRed,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier
                                .clickable {
                                    dueTime = ""
                                    reminderEnabled = false
                                }
                                .padding(4.dp)
                                .testTag("btn_clear_task_time")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Time display card / picker trigger
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showTimePickerDialog = true }
                        .testTag("btn_pick_task_time"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (dueTime.isNotBlank()) PlantSoftGreen.copy(alpha = 0.4f) else PlantSurfaceVariant
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (dueTime.isNotBlank()) PlantPrimary.copy(alpha = 0.5f) else PlantBorder.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AccessTime,
                                contentDescription = "Select Time",
                                tint = if (dueTime.isNotBlank()) PlantPrimary else PlantTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = if (dueTime.isNotBlank()) dueTime else "Set specific time",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (dueTime.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
                                        color = if (dueTime.isNotBlank()) PlantText else PlantTextSecondary,
                                        fontSize = 15.sp
                                    )
                                )
                                if (dueTime.isNotBlank() && reminderEnabled) {
                                    val repeatDesc = when (reminderRepeat) {
                                        "Daily" -> "Repeats daily at $dueTime"
                                        "Custom Days" -> {
                                            val dayLabels = daysOfWeek.filter { reminderDays.contains(it.second) }.map { it.first }.joinToString(", ")
                                            if (dayLabels.isNotBlank()) "Repeats on $dayLabels at $dueTime" else "Repeats at $dueTime"
                                        }
                                        else -> "Notification scheduled for $dueTime"
                                    }
                                    Text(
                                        text = repeatDesc,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = PlantPrimaryDark,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.EditCalendar,
                                contentDescription = "Pick Time",
                                tint = PlantPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Quick Time Preset Chips
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickTimes) { qTime ->
                        val isSelected = dueTime == qTime
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) PlantSoftGreen else PlantSurfaceVariant)
                                .border(
                                    1.dp,
                                    if (isSelected) PlantPrimary else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    dueTime = qTime
                                    reminderEnabled = true
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("chip_time_$qTime"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = qTime,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) PlantPrimaryDark else PlantTextSecondary,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Reminder Toggle Row: Reminder: ON / OFF
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (reminderEnabled && dueTime.isNotBlank()) PlantSoftGreen.copy(alpha = 0.5f) else PlantSurfaceVariant
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (reminderEnabled && dueTime.isNotBlank()) PlantPrimary.copy(alpha = 0.4f) else PlantBorder.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                        .background(
                                            if (reminderEnabled) PlantPrimary.copy(alpha = 0.15f) else Color.Transparent
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (reminderEnabled) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsOff,
                                        contentDescription = "Reminder",
                                        tint = if (reminderEnabled) PlantPrimaryDark else PlantTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Reminder: ",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = PlantText
                                            )
                                        )
                                        Text(
                                            text = if (reminderEnabled) "ON" else "OFF",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (reminderEnabled) PlantPrimaryDark else PlantTextSecondary
                                            )
                                        )
                                    }
                                    Text(
                                        text = if (dueTime.isBlank()) {
                                            "Select a time above to schedule notification"
                                        } else if (reminderEnabled) {
                                            "Local notification scheduled for $dueTime"
                                        } else {
                                            "No notification will be sent"
                                        },
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = PlantTextSecondary,
                                            fontSize = 11.5.sp
                                        )
                                    )
                                }
                            }

                            Switch(
                                checked = reminderEnabled,
                                onCheckedChange = { isChecked ->
                                    reminderEnabled = isChecked
                                    if (isChecked && dueTime.isBlank()) {
                                        // Default to 10:30 AM if no time set yet
                                        dueTime = "10:30 AM"
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = PlantPrimary,
                                    uncheckedThumbColor = PlantTextSecondary,
                                    uncheckedTrackColor = PlantSurfaceVariant
                                ),
                                modifier = Modifier.testTag("switch_task_reminder")
                            )
                        }

                        // Reminder Repeat / Schedule Section (visible when Reminder is ON)
                        if (reminderEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = PlantBorder.copy(alpha = 0.5f), thickness = 0.8.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Reminder Schedule / Repeat",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = PlantTextSecondary
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Repeat Options Chips (Once, Daily, Custom Days)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                repeatOptions.forEach { rep ->
                                    val isSelected = reminderRepeat == rep
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) PlantPrimary else PlantSurface)
                                            .border(
                                                1.dp,
                                                if (isSelected) PlantPrimaryDark else PlantBorder,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable { reminderRepeat = rep }
                                            .padding(vertical = 7.dp)
                                            .testTag("chip_repeat_${rep.replace(" ", "_")}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = rep,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else PlantTextSecondary,
                                                fontSize = 11.5.sp
                                            )
                                        )
                                    }
                                }
                            }

                            // Custom Days Multi-Selection Chips
                            if (reminderRepeat == "Custom Days") {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Select Days:",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = PlantTextSecondary,
                                        fontSize = 11.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    daysOfWeek.forEach { (dayLabel, calDay) ->
                                        val isDaySelected = reminderDays.contains(calDay)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isDaySelected) PlantSoftGreen else PlantSurface
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isDaySelected) PlantPrimary else PlantBorder,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    if (isDaySelected) {
                                                        if (reminderDays.size > 1) {
                                                            reminderDays.remove(calDay)
                                                        }
                                                    } else {
                                                        reminderDays.add(calDay)
                                                    }
                                                }
                                                .padding(vertical = 8.dp)
                                                .testTag("chip_day_$dayLabel"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = dayLabel,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = if (isDaySelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isDaySelected) PlantPrimaryDark else PlantTextSecondary,
                                                    fontSize = 11.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

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
                    categories.forEach { cat ->
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
                                .testTag("chip_cat_$cat"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) PlantPrimaryDark else PlantTextSecondary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = PlantTextSecondary, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                if (reminderEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasRequestedPermission) {
                                    hasRequestedPermission = true
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onConfirm(
                                        title.trim(),
                                        category,
                                        dueDate,
                                        dueTime.trim(),
                                        reminderEnabled,
                                        reminderRepeat,
                                        reminderDays.toList()
                                    )
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PlantPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("btn_submit_task")
                    ) {
                        Text(
                            text = if (initialTask != null) "Save Changes" else "Create Task",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskTimePickerDialog(
    initialTimeStr: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var initialHour = 10
    var initialMinute = 30
    if (initialTimeStr.isNotBlank()) {
        val match = Regex("""(\d{1,2}):(\d{2})\s*(AM|PM|am|pm)?""").find(initialTimeStr)
        if (match != null) {
            var h = match.groupValues[1].toInt()
            val m = match.groupValues[2].toInt()
            val ampm = match.groupValues.getOrNull(3)?.uppercase(Locale.getDefault())
            if (ampm == "PM" && h < 12) h += 12
            if (ampm == "AM" && h == 12) h = 0
            initialHour = h
            initialMinute = m
        }
    }

    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .wrapContentSize(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = PlantTheme.colors.cardBackground)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Select Task Time",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = PlantText
                    ),
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(16.dp))
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = PlantTheme.colors.surfaceElevated,
                        selectorColor = PlantPrimary,
                        periodSelectorBorderColor = PlantBorder,
                        periodSelectorSelectedContainerColor = PlantSoftGreen,
                        periodSelectorSelectedContentColor = PlantPrimaryDark,
                        timeSelectorSelectedContainerColor = PlantSoftGreen,
                        timeSelectorSelectedContentColor = PlantPrimaryDark
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = PlantTextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val hour = timePickerState.hour
                            val minute = timePickerState.minute
                            val cal = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, hour)
                                set(Calendar.MINUTE, minute)
                            }
                            val sdf = java.text.SimpleDateFormat("h:mm a", Locale.getDefault())
                            onConfirm(sdf.format(cal.time))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PlantPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Set Time", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddHabitDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, frequency: String, category: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("Daily") }
    var category by remember { mutableStateOf("Health") }

    val frequencies = listOf("Daily", "Weekdays", "Weekend")
    val categories = listOf("Health", "Study", "Mind", "Fitness")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = PlantSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Build New Habit",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = PlantText
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Habit Name") },
                    placeholder = { Text("e.g. Morning Meditation, 2L Water") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_habit_name"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PlantPrimary,
                        unfocusedBorderColor = PlantBorder
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Frequency",
                    style = MaterialTheme.typography.labelMedium.copy(color = PlantTextSecondary)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    frequencies.forEach { freq ->
                        val isSelected = frequency == freq
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
                                .clickable { frequency = freq }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = freq,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) PlantPrimaryDark else PlantTextSecondary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium.copy(color = PlantTextSecondary)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
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
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) PlantPrimaryDark else PlantTextSecondary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = PlantTextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onConfirm(name, frequency, category)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PlantPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("btn_submit_habit")
                    ) {
                        Text("Create Habit", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddGoalDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, deadline: String, milestones: List<String>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("") }
    var newMilestoneText by remember { mutableStateOf("") }
    val milestones = remember { mutableStateListOf<String>() }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = PlantSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Set Long-term Goal",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = PlantText
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Goal Title") },
                    placeholder = { Text("e.g. Master Kotlin Coroutines") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_goal_title"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PlantPrimary,
                        unfocusedBorderColor = PlantBorder
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = deadline,
                    onValueChange = { deadline = it },
                    label = { Text("Target Deadline (Optional)") },
                    placeholder = { Text("e.g. 2026-10-31") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PlantPrimary,
                        unfocusedBorderColor = PlantBorder
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Milestones (Steps to achieve)",
                    style = MaterialTheme.typography.labelMedium.copy(color = PlantTextSecondary)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newMilestoneText,
                        onValueChange = { newMilestoneText = it },
                        placeholder = { Text("Add milestone step...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PlantPrimary,
                            unfocusedBorderColor = PlantBorder
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newMilestoneText.isNotBlank()) {
                                milestones.add(newMilestoneText.trim())
                                newMilestoneText = ""
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(PlantOrange, CircleShape)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add", tint = Color.White)
                    }
                }

                if (milestones.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        milestones.forEachIndexed { index, m ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(PlantSurfaceVariant, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}. $m",
                                    style = MaterialTheme.typography.bodySmall.copy(color = PlantText)
                                )
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Remove",
                                    tint = PlantTextTertiary,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { milestones.removeAt(index) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = PlantTextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onConfirm(title, deadline, milestones.toList())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PlantOrange),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("btn_submit_goal")
                    ) {
                        Text("Create Goal", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
