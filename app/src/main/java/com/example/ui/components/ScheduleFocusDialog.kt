package com.example.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
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
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScheduleFocusDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, category: String, durationMinutes: Int, scheduledTimeMillis: Long, reminderMinutesBefore: Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Study") }
    var durationMinutes by remember { mutableStateOf(25) }
    var delayMinutes by remember { mutableStateOf(30) } // In 15m, 30m, 60m, 120m
    var reminderMinutesBefore by remember { mutableStateOf(10) } // 5, 10, 15

    val categories = listOf("Study", "Code", "Reading", "Project")
    val durations = listOf(15, 25, 45, 60)
    val delays = listOf(15 to "In 15m", 30 to "In 30m", 60 to "In 1h", 120 to "In 2h")
    val reminderOptions = listOf(5, 10, 15)

    // Permission launcher for Android 13+ (POST_NOTIFICATIONS)
    var hasRequestedPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Proceed with scheduling
        val targetTime = System.currentTimeMillis() + (delayMinutes * 60 * 1000L)
        onConfirm(title, category, durationMinutes, targetTime, reminderMinutesBefore)
    }

    val calculatedTargetTime = System.currentTimeMillis() + (delayMinutes * 60 * 1000L)
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val formattedTargetTime = timeFormat.format(Date(calculatedTargetTime))

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = PlantSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PlantSoftGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            tint = if (PlantTheme.isDark) PlantPrimary else PlantPrimaryDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Schedule Focus Session",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = PlantText
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Session Title") },
                    placeholder = { Text("e.g. DSA Graphs & Trees") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_schedule_title"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PlantPrimary,
                        unfocusedBorderColor = PlantBorder,
                        focusedTextColor = PlantText,
                        unfocusedTextColor = PlantText,
                        focusedContainerColor = PlantInputBackground,
                        unfocusedContainerColor = PlantInputBackground
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category
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
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) (if (PlantTheme.isDark) PlantPrimary else PlantPrimaryDark) else PlantTextSecondary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Duration
                Text(
                    text = "Duration: $durationMinutes min",
                    style = MaterialTheme.typography.labelMedium.copy(color = PlantTextSecondary)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    durations.forEach { dur ->
                        val isSelected = durationMinutes == dur
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
                                .clickable { durationMinutes = dur }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${dur}m",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) (if (PlantTheme.isDark) PlantPrimary else PlantPrimaryDark) else PlantTextSecondary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Schedule Time
                Text(
                    text = "When: Today at $formattedTargetTime",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = if (PlantTheme.isDark) PlantPrimary else PlantPrimaryDark,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    delays.forEach { (mins, label) ->
                        val isSelected = delayMinutes == mins
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) PlantPrimary else PlantSurfaceVariant)
                                .clickable { delayMinutes = mins }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else PlantTextSecondary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Reminder Before
                Text(
                    text = "Remind me before",
                    style = MaterialTheme.typography.labelMedium.copy(color = PlantTextSecondary)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    reminderOptions.forEach { rem ->
                        val isSelected = reminderMinutesBefore == rem
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
                                .clickable { reminderMinutesBefore = rem }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${rem}m before",
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
                            if (title.isNotBlank()) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasRequestedPermission) {
                                    hasRequestedPermission = true
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    val targetTime = System.currentTimeMillis() + (delayMinutes * 60 * 1000L)
                                    onConfirm(title, category, durationMinutes, targetTime, reminderMinutesBefore)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PlantPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("btn_submit_schedule")
                    ) {
                        Text("Set Reminder", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
