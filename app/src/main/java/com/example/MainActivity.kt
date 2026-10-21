package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.notification.FocusNotificationManager
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.PlantViewModel
import com.example.ui.viewmodel.ScreenTab

class MainActivity : ComponentActivity() {
    private val viewModel: PlantViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create the notification channel for approaching focus session reminders
        FocusNotificationManager.createNotificationChannel(this)
        handleNotificationIntent(intent)

        setContent {
            val themePreference by viewModel.themePreference.collectAsState()
            PlantThemeProvider(themePreference = themePreference) {
                PlantApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent?.getStringExtra("navigate_to") == "FOCUS") {
            viewModel.setTab(ScreenTab.FOCUS)
            intent.getStringExtra("focus_category")?.let {
                viewModel.setFocusCategory(it)
            }
        }
    }
}

@Composable
fun PlantApp(viewModel: PlantViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isAuthLoading by viewModel.isAuthLoading.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()

    // Dialog states
    val showCreateSheet by viewModel.showCreateSheet.collectAsState()
    val showAddTaskDialog by viewModel.showAddTaskDialog.collectAsState()
    val editingTask by viewModel.editingTask.collectAsState()
    val showAddHabitDialog by viewModel.showAddHabitDialog.collectAsState()
    val showAddGoalDialog by viewModel.showAddGoalDialog.collectAsState()
    val showScheduleDialog by viewModel.showScheduleDialog.collectAsState()
    val unlockedAchievementPopup by viewModel.unlockedAchievementPopup.collectAsState()

    if (isAuthLoading) {
        // Splash / Auth loading screen while checking the session
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PlantBackground),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                SeedlingPlant(size = 90.dp, progress = 0.8f)
                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    Text(
                        text = "Nivora",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = PlantText
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "For Focus",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = PlantPrimary
                        )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = PlantPrimary,
                    strokeWidth = 2.dp
                )
            }
        }
    } else if (currentUser == null) {
        // Protected App: unauthenticated users only access Login / Create Account
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            AuthScreen(
                initialIsRegister = false,
                viewModel = viewModel,
                onAuthSuccess = {}
            )
        }
    } else {
        // Authenticated Application
        Scaffold(
            containerColor = PlantBackground,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                PlantBottomNavBar(
                    currentTab = currentTab,
                    onTabSelected = { viewModel.setTab(it) },
                    onPlusClicked = { viewModel.toggleCreateSheet(true) }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    ScreenTab.HOME -> HomeScreen(viewModel = viewModel)
                    ScreenTab.TASKS -> TasksScreen(viewModel = viewModel)
                    ScreenTab.FOCUS -> FocusSessionScreen(viewModel = viewModel)
                    ScreenTab.HABITS -> HabitsScreen(viewModel = viewModel)
                    ScreenTab.GOALS -> GoalsScreen(viewModel = viewModel)
                    ScreenTab.PROFILE -> ProfileScreen(viewModel = viewModel, onLogout = { viewModel.logout() })
                    ScreenTab.INSIGHTS -> ProfileScreen(viewModel = viewModel, onLogout = { viewModel.logout() })
                }
            }
        }

        // Global Bottom Sheet for Quick Creation (+)
        if (showCreateSheet) {
            CreateChooserSheet(
                onDismiss = { viewModel.toggleCreateSheet(false) },
                onChooseTask = { viewModel.toggleAddTaskDialog(true) },
                onChooseHabit = { viewModel.toggleAddHabitDialog(true) },
                onChooseGoal = { viewModel.toggleAddGoalDialog(true) },
                onChooseScheduleFocus = { viewModel.toggleScheduleDialog(true) }
            )
        }

        // Add Task Dialog
        if (showAddTaskDialog) {
            AddTaskDialog(
                onDismiss = { viewModel.toggleAddTaskDialog(false) },
                onConfirm = { title, category, dueDate, dueTime, reminderEnabled, reminderRepeat, reminderDays ->
                    viewModel.addTask(
                        title = title,
                        category = category,
                        dueDate = dueDate,
                        dueTime = dueTime,
                        reminderEnabled = reminderEnabled,
                        reminderRepeat = reminderRepeat,
                        reminderDays = reminderDays
                    )
                }
            )
        }

        // Edit Task Dialog
        editingTask?.let { task ->
            AddTaskDialog(
                initialTask = task,
                onDismiss = { viewModel.setEditingTask(null) },
                onConfirm = { title, category, dueDate, dueTime, reminderEnabled, reminderRepeat, reminderDays ->
                    viewModel.updateTask(
                        task.copy(
                            title = title,
                            category = category,
                            dueDate = dueDate,
                            dueTime = dueTime,
                            reminderEnabled = reminderEnabled,
                            reminderRepeat = reminderRepeat,
                            reminderDays = reminderDays
                        )
                    )
                }
            )
        }

        // Schedule Focus Dialog (Approaching Reminders)
        if (showScheduleDialog) {
            ScheduleFocusDialog(
                onDismiss = { viewModel.toggleScheduleDialog(false) },
                onConfirm = { title, category, duration, targetTime, reminderMinutes ->
                    viewModel.scheduleFocusSession(title, category, duration, targetTime, reminderMinutes)
                }
            )
        }

        // Add Habit Dialog
        if (showAddHabitDialog) {
            AddHabitDialog(
                onDismiss = { viewModel.toggleAddHabitDialog(false) },
                onConfirm = { name, frequency, category ->
                    viewModel.addHabit(name, frequency, category)
                }
            )
        }

        // Add Goal Dialog
        if (showAddGoalDialog) {
            AddGoalDialog(
                onDismiss = { viewModel.toggleAddGoalDialog(false) },
                onConfirm = { title, deadline, milestones ->
                    viewModel.addGoal(title, deadline, milestones)
                }
            )
        }

        // Achievement Unlock Celebration Popup
        unlockedAchievementPopup?.let { achievement ->
            AchievementUnlockDialog(
                achievement = achievement,
                onDismiss = { viewModel.dismissAchievementPopup() }
            )
        }
    }
}
