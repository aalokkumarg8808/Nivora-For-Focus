package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.TrackChanges
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
import com.example.data.model.Goal
import com.example.ui.components.SeedlingPlant
import com.example.ui.theme.*
import com.example.ui.viewmodel.PlantViewModel
import com.example.ui.viewmodel.ScreenTab
import com.example.util.PlantUtils

@Composable
fun GoalsScreen(
    viewModel: PlantViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.setTab(ScreenTab.HOME)
    }

    val goals by viewModel.allGoals.collectAsState()

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
                        PlantOrange.copy(alpha = 0.07f),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.85f, height * 0.15f),
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
            // 1. Header with Back & Add Button
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .shadow(elevation = 4.dp, shape = CircleShape, spotColor = Color(0x10000000))
                                .clip(CircleShape)
                                .background(PlantTheme.colors.cardBackground)
                                .border(1.dp, PlantTheme.colors.cardBorder, CircleShape)
                                .clickable { viewModel.setTab(ScreenTab.HOME) }
                                .testTag("btn_goals_back"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = PlantText,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Goals",
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PlantText,
                                    fontSize = 30.sp,
                                    letterSpacing = (-0.5).sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Milestones on your path",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = PlantTextSecondary,
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(elevation = 6.dp, shape = CircleShape, spotColor = PlantOrange)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        PlantOrange,
                                        Color(0xFFE07A00)
                                    )
                                )
                            )
                            .clickable { viewModel.toggleAddGoalDialog(true) }
                            .testTag("btn_add_goal"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add Goal",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // 2. Goals List or Empty State
            if (goals.isEmpty()) {
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
                            SeedlingPlant(size = 72.dp, progress = 0.5f)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No goals set yet",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PlantText,
                                    fontSize = 18.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Set an ambitious learning or career target with milestone steps.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = PlantTextSecondary,
                                    fontSize = 14.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.toggleAddGoalDialog(true) },
                                colors = ButtonDefaults.buttonColors(containerColor = PlantOrange),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("Set Your First Goal", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        }
                    }
                }
            } else {
                items(goals, key = { it.id }) { goal ->
                    GoalCardItem(
                        goal = goal,
                        onToggleMilestone = { milestoneId -> viewModel.toggleMilestone(goal.id, milestoneId) },
                        onDelete = { viewModel.deleteGoal(goal.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalCardItem(
    goal: Goal,
    onToggleMilestone: (String) -> Unit,
    onDelete: () -> Unit
) {
    val totalMilestones = goal.milestones.size
    val completedMilestones = goal.milestones.count { it.completed }
    val progressFloat = if (totalMilestones == 0) (if (goal.isCompleted) 1f else 0f) else completedMilestones.toFloat() / totalMilestones
    val isDark = PlantTheme.isDark
    val orangeSoftBg = if (isDark) Color(0x33F79009) else PlantOrangeSoft
    val milestoneItemBg = PlantTheme.colors.surfaceElevated
    val cardBorderColor = PlantTheme.colors.cardBorder

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 2.dp else 4.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = if (isDark) Color(0x33000000) else Color(0x10111614)
            )
            .border(1.dp, cardBorderColor, RoundedCornerShape(24.dp))
            .testTag("goal_item_${goal.id}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PlantTheme.colors.cardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row: Title, Target icon, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(orangeSoftBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.TrackChanges,
                        contentDescription = null,
                        tint = PlantOrange,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = PlantText,
                            fontSize = 17.sp
                        )
                    )
                    if (goal.deadline.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Target: ${goal.deadline}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = PlantTextSecondary,
                                fontSize = 13.sp
                            )
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete Goal",
                        tint = PlantTextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress bar and numeric label (8px high, rounded)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$completedMilestones of $totalMilestones milestones",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = PlantOrange,
                        fontSize = 14.sp
                    )
                )
                Text(
                    text = "${(progressFloat * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = PlantText,
                        fontSize = 14.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progressFloat },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = PlantOrange,
                trackColor = orangeSoftBg
            )

            // Milestones list
            if (goal.milestones.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    goal.milestones.forEach { ms ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(milestoneItemBg)
                                .clickable { onToggleMilestone(ms.id) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (ms.completed) PlantPrimary else Color.Transparent)
                                    .border(
                                        1.5.dp,
                                        if (ms.completed) PlantPrimary else PlantTheme.colors.border,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (ms.completed) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = ms.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = if (ms.completed) PlantTextSecondary else PlantText,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
