package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun QuickActionButtons(
    onFocusClick: () -> Unit,
    onAddTaskClick: () -> Unit,
    onHabitClick: () -> Unit,
    onGoalClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentSoft = PlantTheme.colors.accentSoft
    val accent = PlantTheme.colors.accent

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        QuickActionButtonItem(
            title = "Focus",
            icon = Icons.Filled.HourglassTop,
            iconTint = accent,
            badgeBg = accentSoft,
            testTag = "quick_action_focus",
            modifier = Modifier.weight(1f),
            onClick = onFocusClick
        )
        QuickActionButtonItem(
            title = "Add Task",
            icon = Icons.Filled.Add,
            iconTint = accent,
            badgeBg = accentSoft,
            testTag = "quick_action_add_task",
            modifier = Modifier.weight(1f),
            onClick = onAddTaskClick
        )
        QuickActionButtonItem(
            title = "Add Habit",
            icon = Icons.Filled.Eco,
            iconTint = accent,
            badgeBg = accentSoft,
            testTag = "quick_action_habit",
            modifier = Modifier.weight(1f),
            onClick = onHabitClick
        )
        QuickActionButtonItem(
            title = "Add Goal",
            icon = Icons.Filled.TrackChanges,
            iconTint = PlantOrange,
            badgeBg = if (PlantTheme.isDark) Color(0x33F79009) else PlantOrangeSoft,
            testTag = "quick_action_goal",
            modifier = Modifier.weight(1f),
            onClick = onGoalClick
        )
    }
}

@Composable
private fun QuickActionButtonItem(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    badgeBg: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 92.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = if (PlantTheme.isDark) Color(0x33000000) else Color(0x10111614)
            )
            .clip(RoundedCornerShape(22.dp))
            .background(PlantTheme.colors.cardBackground)
            .border(
                1.dp,
                PlantTheme.colors.cardBorder,
                RoundedCornerShape(22.dp)
            )
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 4.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(badgeBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(7.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = PlantText
            ),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}
