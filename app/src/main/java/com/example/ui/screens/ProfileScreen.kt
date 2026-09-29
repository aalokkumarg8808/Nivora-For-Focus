package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Achievement
import com.example.data.model.Goal
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.PlantViewModel
import com.example.ui.viewmodel.ScreenTab
import com.example.util.PlantUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
    viewModel: PlantViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val achievements by viewModel.allAchievements.collectAsState()
    val tasks by viewModel.allTasks.collectAsState()
    val habits by viewModel.allHabits.collectAsState()
    val goals by viewModel.allGoals.collectAsState()
    val sessions by viewModel.allFocusSessions.collectAsState()

    val showEditProfile by viewModel.showEditProfileDialog.collectAsState()
    val showSettings by viewModel.showSettingsDialog.collectAsState()
    val showAddGoalDialog by viewModel.showAddGoalDialog.collectAsState()
    val selectedAchievement by viewModel.selectedAchievementDetail.collectAsState()

    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteAccountConfirmDialog by remember { mutableStateOf(false) }
    var updateSuccessMessage by remember { mutableStateOf<String?>(null) }

    val user = currentUser
    val userName = user?.name?.ifBlank { "Botanist" } ?: "Botanist"
    val userEmail = user?.email ?: ""
    val userPhoto = user?.photoURL
    val userXp = user?.xp ?: 0
    val userLevel = remember(userXp) { PlantUtils.calculateLevel(userXp) }
    val levelProgress = remember(userXp) { PlantUtils.xpProgressInLevel(userXp) }

    val (currentStreak, longestStreak) = remember(habits, user?.currentStreak, user?.longestStreak) {
        val allHabitDates = habits.flatMap { it.completionHistory }
        val (calculatedStreak, calculatedLongest) = PlantUtils.calculateStreak(allHabitDates)
        val current = maxOf(user?.currentStreak ?: 0, calculatedStreak)
        val longest = maxOf(user?.longestStreak ?: 0, calculatedLongest)
        current to longest
    }

    val totalCompletedTasks = remember(tasks) { tasks.count { it.completed } }
    val totalFocusMinutes = remember(sessions) { sessions.sumOf { it.durationMinutes } }
    val totalCompletedHabits = remember(habits) { habits.flatMap { it.completionHistory }.size }
    val totalCompletedGoals = remember(goals) { goals.count { it.isCompleted } }
    val totalSessionsCount = sessions.size

    val joinDateStr = remember(user?.createdAt) {
        if (user != null) {
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            sdf.format(Date(user.createdAt))
        } else "Recently"
    }

    val isDark = PlantTheme.isDark
    val ambientSecondary = PlantTheme.colors.accentSoftBorder

    // Glass style container colors from theme
    val glassCardBackground = PlantTheme.colors.glass
    val glassCardBorder = PlantTheme.colors.glassBorder

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PlantBackground)
    ) {
        // Subtle botanical ambient glow effect
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Top-right soft mint orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PlantPrimary.copy(alpha = if (isDark) 0.12f else 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(canvasWidth * 0.85f, canvasHeight * 0.15f),
                    radius = canvasWidth * 0.55f
                )
            )

            // Middle-left pale green orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ambientSecondary.copy(alpha = if (isDark) 0.08f else 0.12f),
                        Color.Transparent
                    ),
                    center = Offset(canvasWidth * 0.1f, canvasHeight * 0.45f),
                    radius = canvasWidth * 0.5f
                )
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. PROFILE HEADER
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Back button (Circular glass button)
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .shadow(elevation = 4.dp, shape = CircleShape, spotColor = Color(0x10000000))
                            .clip(CircleShape)
                            .background(glassCardBackground)
                            .border(1.dp, glassCardBorder, CircleShape)
                            .clickable { viewModel.setTab(ScreenTab.HOME) }
                            .testTag("btn_profile_back"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Center: Title
                    Text(
                        text = "Profile",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 20.sp
                        )
                    )

                    // Right: Two circular glass icon buttons (Edit Profile & Settings)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Edit Profile Button (Pencil icon)
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .shadow(elevation = 4.dp, shape = CircleShape, spotColor = Color(0x10000000))
                                .clip(CircleShape)
                                .background(glassCardBackground)
                                .border(1.dp, glassCardBorder, CircleShape)
                                .clickable { viewModel.toggleEditProfileDialog(true) }
                                .testTag("btn_profile_edit"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit Profile",
                                tint = PlantPrimaryDark,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // Settings Button (Gear icon)
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .shadow(elevation = 4.dp, shape = CircleShape, spotColor = Color(0x10000000))
                                .clip(CircleShape)
                                .background(glassCardBackground)
                                .border(1.dp, glassCardBorder, CircleShape)
                                .clickable { viewModel.toggleSettingsDialog(true) }
                                .testTag("btn_profile_settings"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Success feedback message banner if profile was just updated
            if (updateSuccessMessage != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = PlantSoftGreen),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PlantSoftGreenBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = PlantPrimaryDark, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = updateSuccessMessage ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(color = PlantPrimaryDark, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // 2. PROFILE HERO CARD (Large Glassmorphism Profile Card with Avatar & Level/XP)
            item {
                ProfileHeroCard(
                    userName = userName,
                    userEmail = userEmail,
                    userPhotoUrl = userPhoto,
                    userXp = userXp,
                    userLevel = userLevel,
                    levelProgress = levelProgress,
                    joinDateStr = joinDateStr,
                    isDark = isDark,
                    onAvatarClick = { viewModel.toggleEditProfileDialog(true) }
                )
            }

            // 3. PRODUCTIVITY STATISTICS CARD (Dedicated card containing Stats Grid + Achievements)
            item {
                val unlockedCount = achievements.count { it.isUnlocked }
                val hours = totalFocusMinutes / 60
                val mins = totalFocusMinutes % 60
                val focusFormatted = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = if (isDark) 4.dp else 6.dp,
                            shape = RoundedCornerShape(26.dp),
                            spotColor = if (isDark) Color(0x33000000) else Color(0x15111614)
                        )
                        .testTag("card_productivity_statistics"),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = glassCardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, glassCardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Card Header: Productivity Statistics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(PlantSoftGreen),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.TrendingUp,
                                        contentDescription = null,
                                        tint = PlantPrimaryDark,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Productivity Statistics",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 17.sp
                                        )
                                    )
                                    Text(
                                        text = "Growth & focus telemetry",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = PlantTextSecondary,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PlantSoftGreen)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Active",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = PlantPrimaryDark,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stats 2-Column Grid
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Row 1: Focus Time & Streak
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                GlassStatCard(
                                    icon = Icons.Filled.Timer,
                                    value = focusFormatted,
                                    label = "Focus Time",
                                    iconTint = PlantPrimaryDark,
                                    bg = PlantTheme.colors.surfaceElevated,
                                    border = glassCardBorder,
                                    modifier = Modifier.weight(1f)
                                )

                                GlassStatCard(
                                    icon = Icons.Filled.LocalFireDepartment,
                                    value = "$currentStreak days",
                                    label = "Current Streak",
                                    iconTint = PlantOrange,
                                    bg = PlantTheme.colors.surfaceElevated,
                                    border = glassCardBorder,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Row 2: Completed Tasks & Completed Habits
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                GlassStatCard(
                                    icon = Icons.Filled.CheckCircle,
                                    value = totalCompletedTasks.toString(),
                                    label = "Completed Tasks",
                                    iconTint = PlantPrimaryDark,
                                    bg = PlantTheme.colors.surfaceElevated,
                                    border = glassCardBorder,
                                    modifier = Modifier.weight(1f)
                                )

                                GlassStatCard(
                                    icon = Icons.Filled.Eco,
                                    value = totalCompletedHabits.toString(),
                                    label = "Habits Checked",
                                    iconTint = PlantPrimaryDark,
                                    bg = PlantTheme.colors.surfaceElevated,
                                    border = glassCardBorder,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Row 3: Longest Streak & Goals Finished
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                GlassStatCard(
                                    icon = Icons.Filled.EmojiEvents,
                                    value = "$longestStreak days",
                                    label = "Longest Streak",
                                    iconTint = PlantOrange,
                                    bg = PlantTheme.colors.surfaceElevated,
                                    border = glassCardBorder,
                                    modifier = Modifier.weight(1f)
                                )

                                GlassStatCard(
                                    icon = Icons.Filled.TrackChanges,
                                    value = totalCompletedGoals.toString(),
                                    label = "Goals Finished",
                                    iconTint = PlantOrange,
                                    bg = PlantTheme.colors.surfaceElevated,
                                    border = glassCardBorder,
                                    modifier = Modifier.weight(1f),
                                    onClick = { viewModel.setTab(ScreenTab.GOALS) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Divider before Achievements
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                            thickness = 1.dp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Achievements Section Inside the Productivity Statistics Card
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.EmojiEvents,
                                    contentDescription = null,
                                    tint = PlantOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Achievements",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 16.sp
                                    )
                                )
                            }

                            Text(
                                text = "$unlockedCount / ${achievements.size} Unlocked",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = PlantPrimaryDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Achievements List Inside Productivity Statistics Card
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            achievements.forEach { ach ->
                                val (statProgress, _) = calculateAchievementProgress(
                                    achievementId = ach.id,
                                    completedTasks = totalCompletedTasks,
                                    completedSessions = totalSessionsCount,
                                    totalFocusMinutes = totalFocusMinutes,
                                    completedHabitLogs = totalCompletedHabits,
                                    currentStreak = currentStreak,
                                    completedGoals = totalCompletedGoals
                                )

                                GlassAchievementCard(
                                    achievement = ach,
                                    statProgress = statProgress,
                                    onClick = {
                                        viewModel.selectAchievementDetail(ach)
                                    },
                                    bg = PlantTheme.colors.surfaceElevated,
                                    border = glassCardBorder
                                )
                            }
                        }
                    }
                }
            }

            // 4. MY GOALS CARD (Dedicated Card for User Goals)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = if (isDark) 4.dp else 6.dp,
                            shape = RoundedCornerShape(26.dp),
                            spotColor = if (isDark) Color(0x33000000) else Color(0x15111614)
                        )
                        .testTag("card_my_goals"),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = glassCardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, glassCardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Header: My Goals with Add Goal button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (isDark) Color(0x33F79009) else PlantOrangeSoft),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.TrackChanges,
                                        contentDescription = null,
                                        tint = PlantOrange,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "My Goals",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 17.sp
                                        )
                                    )
                                    Text(
                                        text = "${goals.count { it.isCompleted }} of ${goals.size} completed",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = PlantTextSecondary,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }

                            // Quick Action: Add Goal (+) or Manage
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { viewModel.toggleAddGoalDialog(true) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(PlantSoftGreen)
                                        .testTag("btn_profile_add_goal")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = "Add Goal",
                                        tint = PlantPrimaryDark,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                TextButton(
                                    onClick = { viewModel.setTab(ScreenTab.GOALS) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "View All",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = PlantPrimaryDark,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Goals List / Empty State
                        if (goals.isEmpty()) {
                            // Empty State
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(PlantTheme.colors.surfaceElevated)
                                    .border(1.dp, glassCardBorder, RoundedCornerShape(18.dp))
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    SeedlingPlant(size = 48.dp, progress = 0.4f)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "No goals created yet",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Set an ambitious learning or career target to grow your garden.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = PlantTextSecondary,
                                            fontSize = 12.sp,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { viewModel.toggleAddGoalDialog(true) },
                                        colors = ButtonDefaults.buttonColors(containerColor = PlantPrimary),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Create Goal", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        } else {
                            // Render user goals compactly inside the card
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                goals.forEach { goal ->
                                    ProfileGoalItemCard(
                                        goal = goal,
                                        onToggleMilestone = { milestoneId ->
                                            viewModel.toggleMilestone(goal.id, milestoneId)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. ACCOUNT ACTIONS (Grouped Glass Card for Sign Out & Account Management)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = glassCardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, glassCardBorder)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp)) {
                        SettingsRow(
                            title = "Log Out",
                            description = "Sign out on this device",
                            icon = Icons.AutoMirrored.Filled.Logout,
                            iconTint = PlantPrimaryDark,
                            showChevron = true,
                            testTag = "btn_profile_logout_row",
                            onClick = { showLogoutConfirmDialog = true }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )

                        SettingsRow(
                            title = "Delete Account",
                            description = "Permanently remove your account and data",
                            icon = Icons.Filled.DeleteForever,
                            iconTint = PlantRed,
                            iconBackground = PlantRedSoft,
                            isDanger = true,
                            showChevron = true,
                            testTag = "btn_profile_delete_account_row",
                            onClick = { showDeleteAccountConfirmDialog = true }
                        )
                    }
                }
            }
        }
    }

    // Edit Profile Modal / Bottom Sheet
    if (showEditProfile && user != null) {
        EditProfileDialog(
            user = user,
            onDismiss = { viewModel.toggleEditProfileDialog(false) },
            onSave = { newName, newPhotoUrl ->
                viewModel.updateUserProfile(newName, newPhotoUrl) { success ->
                    viewModel.toggleEditProfileDialog(false)
                    if (success) {
                        updateSuccessMessage = "Profile updated successfully"
                    }
                }
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

    // Settings Modal / Bottom Sheet
    if (showSettings && user != null) {
        SettingsSheet(
            user = user,
            onDismiss = { viewModel.toggleSettingsDialog(false) },
            onOpenEditProfile = { viewModel.toggleEditProfileDialog(true) },
            onChangePassword = { newPassword, onResult ->
                viewModel.changePassword(newPassword, onResult)
            },
            onUpdateSettings = { theme, reduceMotion, focusRem, taskRem, habitRem, focusSound, compSound ->
                viewModel.updateUserSettings(theme, reduceMotion, focusRem, taskRem, habitRem, focusSound, compSound)
            },
            onLogoutClick = { showLogoutConfirmDialog = true },
            onDeleteAccountClick = { showDeleteAccountConfirmDialog = true }
        )
    }

    // Achievement Detail Dialog
    selectedAchievement?.let { ach ->
        val (statProgress, progressFraction) = calculateAchievementProgress(
            achievementId = ach.id,
            completedTasks = totalCompletedTasks,
            completedSessions = totalSessionsCount,
            totalFocusMinutes = totalFocusMinutes,
            completedHabitLogs = totalCompletedHabits,
            currentStreak = currentStreak,
            completedGoals = totalCompletedGoals
        )

        AchievementDetailDialog(
            achievement = ach,
            currentStatProgress = statProgress,
            progressFraction = progressFraction,
            onDismiss = { viewModel.selectAchievementDetail(null) }
        )
    }

    // Log Out Confirmation Dialog
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = {
                Text("Log out of Plant:Focus & Study?")
            },
            text = {
                Text(
                    "You will be signed out on this device. You can sign back in anytime with your credentials.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PlantPrimary)
                ) {
                    Text("Log Out", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("Cancel", color = PlantTextSecondary)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = PlantTheme.colors.surfaceElevated
        )
    }

    // Delete Account Confirmation Dialog
    if (showDeleteAccountConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = PlantRed,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text("Delete your account?", color = PlantRed, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "This will permanently delete your account and associated productivity data. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = PlantText)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountConfirmDialog = false
                        viewModel.deleteAccount(onSuccess = onLogout)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PlantRed)
                ) {
                    Text("Delete Account", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountConfirmDialog = false }) {
                    Text("Cancel", color = PlantTextSecondary)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = PlantTheme.colors.surfaceElevated
        )
    }
}

@Composable
private fun ProfileGoalItemCard(
    goal: Goal,
    onToggleMilestone: (String) -> Unit
) {
    val totalMilestones = goal.milestones.size
    val completedMilestones = goal.milestones.count { it.completed }
    val progressFloat = if (totalMilestones == 0) (if (goal.isCompleted) 1f else 0f) else completedMilestones.toFloat() / totalMilestones
    val isDark = PlantTheme.isDark
    var isExpanded by remember { mutableStateOf(false) }

    val animatedProgress by animateFloatAsState(
        targetValue = progressFloat.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "goalProgress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(
                1.dp,
                if (goal.isCompleted) PlantPrimary.copy(alpha = 0.4f) else PlantTheme.colors.cardBorder,
                RoundedCornerShape(18.dp)
            )
            .testTag("profile_goal_item_${goal.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PlantTheme.colors.surfaceElevated)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Title & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (goal.deadline.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.CalendarToday,
                                contentDescription = null,
                                tint = PlantTextSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Target: ${goal.deadline}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = PlantTextSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }

                // Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (goal.isCompleted) PlantSoftGreen else PlantTheme.colors.cardBackground)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (goal.isCompleted) "Done ✓" else "$completedMilestones / $totalMilestones",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (goal.isCompleted) PlantPrimaryDark else PlantTextSecondary,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(PlantTheme.colors.progressTrack)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                if (goal.isCompleted) {
                                    listOf(PlantPrimaryDark, PlantPrimary)
                                } else {
                                    listOf(PlantOrange, Color(0xFFFDB022))
                                }
                            )
                        )
                )
            }

            // Milestone expandable details
            if (goal.milestones.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded }
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isExpanded) "Hide Milestones" else "Show Milestones (${goal.milestones.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = PlantPrimaryDark,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = PlantPrimaryDark,
                        modifier = Modifier.size(16.dp)
                    )
                }

                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        goal.milestones.forEach { ms ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (ms.completed) PlantSoftGreen.copy(alpha = 0.5f) else PlantTheme.colors.cardBackground)
                                    .clickable { onToggleMilestone(ms.id) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(if (ms.completed) PlantPrimary else Color.Transparent)
                                        .border(
                                            1.5.dp,
                                            if (ms.completed) PlantPrimary else PlantTextSecondary.copy(alpha = 0.5f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (ms.completed) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Completed",
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = ms.title,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (ms.completed) PlantTextSecondary else PlantText,
                                        textDecoration = if (ms.completed) TextDecoration.LineThrough else TextDecoration.None,
                                        fontSize = 12.sp
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassStatCard(
    icon: ImageVector,
    value: String,
    label: String,
    iconTint: Color,
    bg: Color,
    border: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .height(84.dp)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(18.dp), spotColor = Color(0x10000000))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        border = androidx.compose.foundation.BorderStroke(1.dp, border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(PlantSoftGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = PlantText,
                        fontSize = 15.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = PlantTextSecondary,
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun GlassAchievementCard(
    achievement: Achievement,
    statProgress: String,
    onClick: () -> Unit,
    bg: Color,
    border: Color
) {
    val isUnlocked = achievement.isUnlocked
    val isDark = PlantTheme.isDark
    val orangeIconBg = if (isDark) Color(0x33F79009) else PlantOrangeSoft
    val greenBadgeText = if (isDark) PlantPrimary else PlantPrimaryDark

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("achievement_card_${achievement.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) bg else bg.copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isUnlocked) PlantPrimary.copy(alpha = 0.35f) else border
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isUnlocked) orangeIconBg else PlantTheme.colors.cardBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isUnlocked) Icons.Filled.EmojiEvents else Icons.Filled.Lock,
                    contentDescription = achievement.title,
                    tint = if (isUnlocked) PlantOrange else PlantTextTertiary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = achievement.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isUnlocked) PlantText else PlantTextSecondary,
                            fontSize = 14.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isUnlocked) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "✓",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = greenBadgeText,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = PlantTextSecondary,
                        fontSize = 11.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Progress: $statProgress",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isUnlocked) greenBadgeText else PlantTextTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isUnlocked) PlantSoftGreen else PlantTheme.colors.cardBackground)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "+${achievement.xpReward} XP",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isUnlocked) greenBadgeText else PlantTextSecondary,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

private fun calculateAchievementProgress(
    achievementId: String,
    completedTasks: Int,
    completedSessions: Int,
    totalFocusMinutes: Int,
    completedHabitLogs: Int,
    currentStreak: Int,
    completedGoals: Int
): Pair<String, Float> {
    return when (achievementId) {
        "first_sprout" -> "$completedTasks / 1 task" to (completedTasks / 1f)
        "deep_root" -> "$completedSessions / 1 session" to (completedSessions / 1f)
        "habit_seed" -> "$completedHabitLogs / 1 habit" to (completedHabitLogs / 1f)
        "botanist_streak" -> "$currentStreak / 3 days" to (currentStreak / 3f)
        "week_of_growth" -> "$currentStreak / 7 days" to (currentStreak / 7f)
        "focus_master" -> "$totalFocusMinutes / 100 mins" to (totalFocusMinutes / 100f)
        "goal_crusher" -> "$completedGoals / 1 goal" to (completedGoals / 1f)
        "master_gardener" -> "$completedTasks / 10 tasks" to (completedTasks / 10f)
        else -> "0 / 1" to 0f
    }
}
