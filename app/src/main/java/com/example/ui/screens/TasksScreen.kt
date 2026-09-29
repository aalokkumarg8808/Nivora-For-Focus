package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.HabitCardItem
import com.example.ui.components.SeedlingPlant
import com.example.ui.components.TaskCardItem
import com.example.ui.theme.*
import com.example.ui.viewmodel.PlantViewModel
import com.example.ui.viewmodel.TasksSubTab
import com.example.util.PlantUtils

@Composable
fun TasksScreen(
    viewModel: PlantViewModel,
    modifier: Modifier = Modifier
) {
    val currentSubTab by viewModel.tasksSubTab.collectAsState()
    val tasks by viewModel.filteredTasks.collectAsState()
    val habits by viewModel.allHabits.collectAsState()
    val selectedCategory by viewModel.selectedTaskCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val todayHabitsDone by viewModel.todayHabitsCompleted.collectAsState()
    val todayHabitsTotal by viewModel.todayHabitsTotal.collectAsState()

    var showSearchField by remember { mutableStateOf(false) }

    val categories = remember { listOf("All", "Study", "Personal", "Health", "Work") }
    val todayStr = remember { PlantUtils.getTodayDateString() }
    val weekDays = remember { PlantUtils.getCurrentWeekDays() }

    val (todayTasks, upcomingTasks) = remember(tasks, todayStr) {
        val today = tasks.filter { it.dueDate.isBlank() || it.dueDate == todayStr }
        val upcoming = tasks.filter { it.dueDate.isNotBlank() && it.dueDate > todayStr }
        today to upcoming
    }

    val todayCompleted = remember(todayTasks) { todayTasks.count { it.completed } }
    val todayTotal = todayTasks.size

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
            // 1. Screen Title with Search Toggle & Quick Add
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
                            text = "Productivity",
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = PlantText,
                                fontSize = 32.sp,
                                letterSpacing = (-0.5).sp
                            )
                        )
                        Text(
                            text = if (currentSubTab == TasksSubTab.TASKS) "Organize daily priorities" else "Cultivate lasting habits",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = PlantTextSecondary,
                                fontSize = 14.sp
                            )
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (currentSubTab == TasksSubTab.TASKS) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .shadow(elevation = 4.dp, shape = CircleShape, spotColor = if (PlantTheme.isDark) Color(0x33000000) else Color(0x12000000))
                                    .clip(CircleShape)
                                    .background(PlantTheme.colors.cardBackground)
                                    .border(1.dp, PlantTheme.colors.cardBorder, CircleShape)
                                    .clickable { showSearchField = !showSearchField }
                                    .testTag("btn_toggle_search"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (showSearchField) Icons.Filled.Close else Icons.Filled.Search,
                                    contentDescription = "Search",
                                    tint = PlantText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
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
                                .clickable {
                                    if (currentSubTab == TasksSubTab.TASKS) {
                                        viewModel.toggleAddTaskDialog(true)
                                    } else {
                                        viewModel.toggleAddHabitDialog(true)
                                    }
                                }
                                .testTag("btn_add_item_top"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = if (currentSubTab == TasksSubTab.TASKS) "Add Task" else "Add Habit",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // 2. UNIFIED SEGMENTED CONTROL: [ Tasks ] [ Habits ]
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(26.dp), spotColor = if (PlantTheme.isDark) Color(0x33000000) else Color(0x10111614)),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = PlantTheme.colors.cardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PlantTheme.colors.cardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Tasks Segment
                        val isTasks = currentSubTab == TasksSubTab.TASKS
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(if (isTasks) PlantPrimary else Color.Transparent)
                                .clickable { viewModel.setTasksSubTab(TasksSubTab.TASKS) }
                                .testTag("tab_tasks_selector"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.TaskAlt,
                                    contentDescription = null,
                                    tint = if (isTasks) Color.White else PlantTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Tasks",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = if (isTasks) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isTasks) Color.White else PlantTextSecondary,
                                        fontSize = 15.sp
                                    )
                                )
                                if (todayTotal > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(if (isTasks) Color.White.copy(alpha = 0.25f) else PlantTheme.colors.surfaceElevated)
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "$todayCompleted/$todayTotal",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isTasks) Color.White else PlantText
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Habits Segment
                        val isHabits = currentSubTab == TasksSubTab.HABITS
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(if (isHabits) PlantPrimary else Color.Transparent)
                                .clickable { viewModel.setTasksSubTab(TasksSubTab.HABITS) }
                                .testTag("tab_habits_selector"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Eco,
                                    contentDescription = null,
                                    tint = if (isHabits) Color.White else PlantTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Habits",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = if (isHabits) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isHabits) Color.White else PlantTextSecondary,
                                        fontSize = 15.sp
                                    )
                                )
                                if (todayHabitsTotal > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(if (isHabits) Color.White.copy(alpha = 0.25f) else PlantTheme.colors.surfaceElevated)
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "$todayHabitsDone/$todayHabitsTotal",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isHabits) Color.White else PlantText
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // SUB-TAB CONTENT: TASKS
            // ==========================================
            if (currentSubTab == TasksSubTab.TASKS) {
                // Search Input if active
                if (showSearchField) {
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search your tasks...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("input_search_tasks"),
                            shape = RoundedCornerShape(18.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PlantPrimary,
                                unfocusedBorderColor = PlantTheme.colors.border,
                                focusedContainerColor = PlantTheme.colors.inputBackground,
                                unfocusedContainerColor = PlantTheme.colors.inputBackground,
                                focusedTextColor = PlantText,
                                unfocusedTextColor = PlantText
                            )
                        )
                    }
                }

                // Filter Chips Row
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { cat ->
                            val isSelected = selectedCategory == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSelected) PlantPrimary else PlantTheme.colors.surfaceElevated
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) PlantPrimary else PlantTheme.colors.border,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable { viewModel.setSelectedCategory(cat) }
                                    .padding(horizontal = 16.dp, vertical = 9.dp)
                                    .testTag("filter_chip_$cat"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else PlantTextSecondary,
                                        fontSize = 13.5.sp
                                    )
                                )
                            }
                        }
                    }
                }

                // Add New Task Action Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(22.dp),
                                spotColor = if (PlantTheme.isDark) Color(0x33000000) else Color(0x12111614)
                            )
                            .clickable { viewModel.toggleAddTaskDialog(true) }
                            .testTag("card_add_new_task"),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = PlantTheme.colors.cardBackground),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PlantTheme.colors.cardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                PlantPrimary,
                                                PlantPrimaryDark
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Add New Task",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Add New Task",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = PlantText,
                                        fontSize = 17.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Turn plans into progress",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = PlantTextSecondary,
                                        fontSize = 13.sp
                                    )
                                )
                            }

                            Icon(
                                imageVector = Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = PlantTextTertiary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // "Today X of Y" Section Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today ",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = PlantText,
                                fontSize = 20.sp,
                                letterSpacing = (-0.2).sp
                            )
                        )
                        Text(
                            text = "$todayCompleted of $todayTotal",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = PlantPrimaryDark,
                                fontSize = 16.sp
                            )
                        )
                    }
                }

                // Today Tasks List or Empty State
                if (todayTasks.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = PlantTheme.colors.cardBackground),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PlantTheme.colors.cardBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                SeedlingPlant(size = 64.dp, progress = 0.3f)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No tasks found",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = PlantText,
                                        fontSize = 17.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Create your first task and start growing.",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = PlantTextSecondary)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.toggleAddTaskDialog(true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = PlantPrimary),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Task", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                } else {
                    items(todayTasks, key = { it.id }) { task ->
                        TaskCardItem(
                            task = task,
                            onToggleComplete = { viewModel.toggleTask(task.id) },
                            onDelete = { viewModel.deleteTask(task.id) },
                            onEdit = { viewModel.setEditingTask(task) }
                        )
                    }
                }

                // "Upcoming" Section Header
                if (upcomingTasks.isNotEmpty()) {
                    item {
                        Text(
                            text = "Upcoming",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = PlantText,
                                fontSize = 20.sp,
                                letterSpacing = (-0.2).sp
                            ),
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }

                    items(upcomingTasks, key = { it.id }) { task ->
                        TaskCardItem(
                            task = task,
                            onToggleComplete = { viewModel.toggleTask(task.id) },
                            onDelete = { viewModel.deleteTask(task.id) },
                            onEdit = { viewModel.setEditingTask(task) }
                        )
                    }
                }
            } else {
                // ==========================================
                // SUB-TAB CONTENT: HABITS
                // ==========================================
                // Add New Habit Action Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(22.dp),
                                spotColor = if (PlantTheme.isDark) Color(0x33000000) else Color(0x12111614)
                            )
                            .clickable { viewModel.toggleAddHabitDialog(true) }
                            .testTag("card_add_new_habit"),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = PlantTheme.colors.cardBackground),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PlantTheme.colors.cardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                PlantPrimary,
                                                PlantPrimaryDark
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Add New Habit",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Add New Habit",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = PlantText,
                                        fontSize = 17.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Small daily habits yield great harvests",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = PlantTextSecondary,
                                        fontSize = 13.sp
                                    )
                                )
                            }

                            Icon(
                                imageVector = Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = PlantTextTertiary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Section Header: Active Habits
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily Habits (${habits.size})",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = PlantText,
                                fontSize = 20.sp,
                                letterSpacing = (-0.2).sp
                            )
                        )
                        Text(
                            text = "$todayHabitsDone completed today",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = PlantPrimaryDark,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        )
                    }
                }

                // Habits List or Empty State
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
}
