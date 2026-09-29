package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Task
import com.example.ui.theme.*

@Composable
fun TaskCardItem(
    task: Task,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (task.completed) 1.dp else 4.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color(0x10111614)
            )
            .border(
                1.dp,
                if (task.completed) PlantTheme.colors.border.copy(alpha = 0.5f) else PlantTheme.colors.cardBorder,
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onEdit)
            .testTag("task_item_${task.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.completed) PlantTheme.colors.surfaceElevated.copy(alpha = 0.5f) else PlantTheme.colors.cardBackground
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkmark button (28dp touch target)
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (task.completed) PlantPrimary else Color.Transparent)
                    .border(
                        2.dp,
                        if (task.completed) PlantPrimary else PlantTheme.colors.border,
                        CircleShape
                    )
                    .clickable(onClick = onToggleComplete)
                    .testTag("btn_toggle_task_${task.id}"),
                contentAlignment = Alignment.Center
            ) {
                if (task.completed) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Completed",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = if (task.completed) PlantTextSecondary else PlantText,
                        fontSize = 16.sp,
                        textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                    )
                )

                Spacer(modifier = Modifier.height(3.dp))

                val subtaskInfo = if (task.subtasks.isNotEmpty()) {
                    val done = task.subtasks.count { it.completed }
                    " • $done/${task.subtasks.size} subtasks"
                } else ""

                val dueDateInfo = if (task.dueDate.isNotBlank()) " • Due ${task.dueDate}" else ""

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${task.category}$subtaskInfo$dueDateInfo",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = PlantTextSecondary,
                            fontSize = 13.sp
                        )
                    )

                    if (task.dueTime.isNotBlank()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (task.reminderEnabled) PlantSoftGreen else PlantTheme.colors.surfaceElevated
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (task.reminderEnabled) Icons.Filled.NotificationsActive else Icons.Filled.AccessTime,
                                contentDescription = if (task.reminderEnabled) "Reminder ON" else "Time",
                                tint = if (task.reminderEnabled) PlantPrimaryDark else PlantTextSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = task.dueTime,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (task.reminderEnabled) FontWeight.Bold else FontWeight.Medium,
                                    color = if (task.reminderEnabled) PlantPrimaryDark else PlantTextSecondary
                                )
                            )
                        }
                    }
                }
            }

            // Priority badge
            val priorityInfo = when (task.priority) {
                "High" -> Icons.Filled.LocalFireDepartment to PlantRed
                "Medium" -> Icons.Filled.Flag to PlantOrange
                else -> Icons.Filled.Eco to PlantPrimaryDark
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(priorityInfo.second.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = priorityInfo.first,
                    contentDescription = task.priority,
                    tint = priorityInfo.second,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("btn_delete_task_${task.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = PlantTextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
