package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.UserProfile
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    user: UserProfile,
    onDismiss: () -> Unit,
    onOpenEditProfile: () -> Unit,
    onChangePassword: (newPassword: String, onResult: (Boolean, String?) -> Unit) -> Unit,
    onUpdateSettings: (theme: String, reduceMotion: Boolean, focusReminders: Boolean, taskReminders: Boolean, habitReminders: Boolean, focusSounds: Boolean, completionSound: Boolean) -> Unit,
    onLogoutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit
) {
    BackHandler {
        onDismiss()
    }

    var selectedTheme by remember { mutableStateOf(user.themePreference) }
    var reduceMotion by remember { mutableStateOf(user.reduceMotion) }
    var focusReminders by remember { mutableStateOf(user.focusReminders) }
    var taskReminders by remember { mutableStateOf(user.taskReminders) }
    var habitReminders by remember { mutableStateOf(user.habitReminders) }
    var focusSounds by remember { mutableStateOf(user.focusSounds) }
    var completionSound by remember { mutableStateOf(user.completionSound) }

    var showPasswordDialog by remember { mutableStateOf(false) }

    val isDark = PlantTheme.isDark
    val glassCardBackground = PlantTheme.colors.glass
    val glassCardBorder = PlantTheme.colors.glassBorder

    fun commitSettings(
        newTheme: String = selectedTheme,
        newReduceMotion: Boolean = reduceMotion,
        newFocusRem: Boolean = focusReminders,
        newTaskRem: Boolean = taskReminders,
        newHabitRem: Boolean = habitReminders,
        newFocusSound: Boolean = focusSounds,
        newCompSound: Boolean = completionSound
    ) {
        onUpdateSettings(
            newTheme,
            newReduceMotion,
            newFocusRem,
            newTaskRem,
            newHabitRem,
            newFocusSound,
            newCompSound
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PlantBackground)
            .testTag("screen_full_settings")
    ) {
        // Ambient botanical glow effect
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PlantPrimary.copy(alpha = if (isDark) 0.10f else 0.06f),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.85f, height * 0.12f),
                    radius = width * 0.5f
                )
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 20.sp
                            )
                        )
                    },
                    navigationIcon = {
                        Box(
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .size(40.dp)
                                .shadow(elevation = 2.dp, shape = CircleShape)
                                .clip(CircleShape)
                                .background(glassCardBackground)
                                .border(1.dp, glassCardBorder, CircleShape)
                                .clickable { onDismiss() }
                                .testTag("btn_settings_back"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Profile",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // 1. ACCOUNT SECTION
                SettingsSectionHeader(title = "ACCOUNT")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = glassCardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, glassCardBorder)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsRow(
                            title = "Edit Profile",
                            icon = Icons.Filled.Person,
                            iconTint = PlantPrimaryDark,
                            testTag = "settings_row_edit_profile",
                            onClick = {
                                onDismiss()
                                onOpenEditProfile()
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        )
                        SettingsRow(
                            title = "Change Password",
                            icon = Icons.Filled.Lock,
                            iconTint = PlantPrimaryDark,
                            testTag = "settings_row_change_password",
                            onClick = { showPasswordDialog = true }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        )
                        SettingsRow(
                            title = "Account Provider",
                            icon = Icons.Filled.AccountCircle,
                            iconTint = PlantPrimaryDark,
                            valueText = user.authProvider,
                            showChevron = false,
                            testTag = "settings_row_provider"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 2. APPEARANCE SECTION
                SettingsSectionHeader(title = "APPEARANCE")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = glassCardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, glassCardBorder)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        // Theme Selector Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(PlantSoftGreen),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DarkMode,
                                        contentDescription = null,
                                        tint = PlantPrimaryDark,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = "Theme",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 15.sp
                                    )
                                )
                            }

                            // 3-way Theme Pills
                            Row(
                                modifier = Modifier
                                    .width(190.dp)
                                    .height(34.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                    .padding(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                listOf("System", "Light", "Dark").forEach { themeName ->
                                    val isSelected = selectedTheme == themeName
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) PlantPrimary else Color.Transparent)
                                            .clickable {
                                                selectedTheme = themeName
                                                commitSettings(newTheme = themeName)
                                            }
                                            .testTag("btn_theme_$themeName"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = themeName,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.5.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        )

                        SettingsRow(
                            title = "Reduced Motion",
                            icon = Icons.Filled.MotionPhotosOff,
                            iconTint = PlantPrimaryDark,
                            isToggle = true,
                            checked = reduceMotion,
                            testTag = "settings_toggle_reduced_motion",
                            onCheckedChange = {
                                reduceMotion = it
                                commitSettings(newReduceMotion = it)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 3. NOTIFICATIONS SECTION
                SettingsSectionHeader(title = "NOTIFICATIONS")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = glassCardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, glassCardBorder)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsRow(
                            title = "Focus Reminders",
                            icon = Icons.Filled.NotificationsActive,
                            iconTint = PlantPrimaryDark,
                            isToggle = true,
                            checked = focusReminders,
                            testTag = "settings_toggle_focus_reminders",
                            onCheckedChange = {
                                focusReminders = it
                                commitSettings(newFocusRem = it)
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        )
                        SettingsRow(
                            title = "Task Reminders",
                            icon = Icons.Filled.TaskAlt,
                            iconTint = PlantPrimaryDark,
                            isToggle = true,
                            checked = taskReminders,
                            testTag = "settings_toggle_task_reminders",
                            onCheckedChange = {
                                taskReminders = it
                                commitSettings(newTaskRem = it)
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        )
                        SettingsRow(
                            title = "Habit Reminders",
                            icon = Icons.Filled.Eco,
                            iconTint = PlantPrimaryDark,
                            isToggle = true,
                            checked = habitReminders,
                            testTag = "settings_toggle_habit_reminders",
                            onCheckedChange = {
                                habitReminders = it
                                commitSettings(newHabitRem = it)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 4. SOUND SECTION
                SettingsSectionHeader(title = "SOUND")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = glassCardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, glassCardBorder)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsRow(
                            title = "Focus Session Sounds",
                            icon = Icons.Filled.VolumeUp,
                            iconTint = PlantPrimaryDark,
                            isToggle = true,
                            checked = focusSounds,
                            testTag = "settings_toggle_focus_sounds",
                            onCheckedChange = {
                                focusSounds = it
                                commitSettings(newFocusSound = it)
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        )
                        SettingsRow(
                            title = "Completion Sound",
                            icon = Icons.Filled.MusicNote,
                            iconTint = PlantPrimaryDark,
                            isToggle = true,
                            checked = completionSound,
                            testTag = "settings_toggle_completion_sound",
                            onCheckedChange = {
                                completionSound = it
                                commitSettings(newCompSound = it)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 5. ACCOUNT ACTIONS SECTION
                SettingsSectionHeader(title = "ACCOUNT ACTIONS")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = glassCardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, glassCardBorder)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsRow(
                            title = "Log Out",
                            icon = Icons.AutoMirrored.Filled.Logout,
                            iconTint = PlantRed,
                            iconBackground = PlantRedSoft,
                            showChevron = false,
                            testTag = "settings_row_logout",
                            onClick = {
                                onDismiss()
                                onLogoutClick()
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        )
                        SettingsRow(
                            title = "Delete Account",
                            icon = Icons.Filled.DeleteForever,
                            iconTint = PlantRed,
                            iconBackground = PlantRedSoft,
                            isDanger = true,
                            showChevron = false,
                            testTag = "settings_row_delete_account",
                            onClick = {
                                onDismiss()
                                onDeleteAccountClick()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))
            }
        }
    }

    // Change Password Dialog
    if (showPasswordDialog) {
        var newPass by remember { mutableStateOf("") }
        var confirmPass by remember { mutableStateOf("") }
        var passError by remember { mutableStateOf<String?>(null) }
        var passSuccess by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showPasswordDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.padding(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Change Password",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text("New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = confirmPass,
                        onValueChange = { confirmPass = it },
                        label = { Text("Confirm New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    if (passError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = passError!!, color = PlantRed, fontSize = 12.sp)
                    }

                    if (passSuccess) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Password updated successfully!", color = PlantPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showPasswordDialog = false }) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                passError = null
                                if (newPass.length < 6) {
                                    passError = "Password must be at least 6 characters"
                                    return@Button
                                }
                                if (newPass != confirmPass) {
                                    passError = "Passwords do not match"
                                    return@Button
                                }
                                onChangePassword(newPass) { success, err ->
                                    if (success) {
                                        passSuccess = true
                                    } else {
                                        passError = err ?: "Failed to update password"
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PlantPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Compatibility wrapper for SettingsSheet alias
@Composable
fun SettingsSheet(
    user: UserProfile,
    onDismiss: () -> Unit,
    onOpenEditProfile: () -> Unit,
    onChangePassword: (newPassword: String, onResult: (Boolean, String?) -> Unit) -> Unit,
    onUpdateSettings: (theme: String, reduceMotion: Boolean, focusReminders: Boolean, taskReminders: Boolean, habitReminders: Boolean, focusSounds: Boolean, completionSound: Boolean) -> Unit,
    onLogoutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit
) {
    SettingsScreen(
        user = user,
        onDismiss = onDismiss,
        onOpenEditProfile = onOpenEditProfile,
        onChangePassword = onChangePassword,
        onUpdateSettings = onUpdateSettings,
        onLogoutClick = onLogoutClick,
        onDeleteAccountClick = onDeleteAccountClick
    )
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            fontSize = 11.sp
        ),
        modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
    )
}
