package com.example.ui.screens

import android.util.Patterns
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.components.SeedlingPlant
import com.example.ui.theme.*
import com.example.ui.viewmodel.PlantViewModel

@Composable
fun AuthScreen(
    initialIsRegister: Boolean = false,
    viewModel: PlantViewModel,
    onAuthSuccess: () -> Unit = {}
) {
    var isRegister by remember { mutableStateOf(initialIsRegister) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val authError by viewModel.authError.collectAsState()
    val isLoading by viewModel.isAuthLoading.collectAsState()

    // Handle back button: if on Create Account, return to Login
    BackHandler(enabled = isRegister) {
        isRegister = false
        localError = null
    }

    fun isValidEmail(input: String): Boolean {
        return input.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(input.trim()).matches()
    }

    Scaffold(
        containerColor = PlantBackground
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Brand Header with seedling icon
            SeedlingPlant(size = 68.dp, progress = 0.85f)

            Spacer(modifier = Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Plant: ",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = PlantText
                    )
                )
                Text(
                    text = "Focus & Study",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = PlantPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isRegister)
                    "Create your account to grow your focus and productivity"
                else
                    "Sign in to your account to continue",
                style = MaterialTheme.typography.bodyMedium.copy(color = PlantTextSecondary),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Tab Selector: Login | Create Account
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(PlantSurfaceVariant, RoundedCornerShape(14.dp))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            if (!isRegister) PlantSurface else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            isRegister = false
                            localError = null
                        }
                        .testTag("tab_auth_login"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Login",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (!isRegister) FontWeight.Bold else FontWeight.Normal,
                            color = if (!isRegister) PlantText else PlantTextSecondary
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            if (isRegister) PlantSurface else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            isRegister = true
                            localError = null
                        }
                        .testTag("tab_auth_create_account"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Create Account",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (isRegister) FontWeight.Bold else FontWeight.Normal,
                            color = if (isRegister) PlantText else PlantTextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val inputColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PlantPrimary,
                unfocusedBorderColor = PlantBorder,
                focusedTextColor = PlantText,
                unfocusedTextColor = PlantText,
                focusedContainerColor = PlantInputBackground,
                unfocusedContainerColor = PlantInputBackground,
                cursorColor = PlantPrimary
            )

            // Name field (Create Account only)
            if (isRegister) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        localError = null
                    },
                    label = { Text("Name") },
                    placeholder = { Text("Your full name") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = PlantTextSecondary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_input_name"),
                    singleLine = true,
                    colors = inputColors
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Email Address field
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    localError = null
                },
                label = { Text("Email Address") },
                placeholder = { Text("name@example.com") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = PlantTextSecondary
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_input_email"),
                singleLine = true,
                colors = inputColors
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Password field
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    localError = null
                },
                label = { Text("Password") },
                placeholder = { Text(if (isRegister) "Min. 6 characters" else "Enter your password") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = PlantTextSecondary
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_input_password"),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = PlantTextSecondary
                        )
                    }
                },
                colors = inputColors
            )

            // Confirm Password field (Create Account only)
            if (isRegister) {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        localError = null
                    },
                    label = { Text("Confirm Password") },
                    placeholder = { Text("Re-enter password") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = PlantTextSecondary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_input_confirm_password"),
                    singleLine = true,
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                                tint = PlantTextSecondary
                            )
                        }
                    },
                    colors = inputColors
                )
            }

            // Error Display (Local validation error or Database Authentication error)
            val currentError = localError ?: authError
            if (!currentError.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PlantRedSoft,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auth_error_message")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = PlantRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = currentError,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = PlantRed,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Action Button (Login or Create Account)
            Button(
                onClick = {
                    localError = null

                    val trimmedName = name.trim()
                    val trimmedEmail = email.trim()
                    val trimmedPassword = password.trim()
                    val trimmedConfirm = confirmPassword.trim()

                    if (isRegister) {
                        // Strict validation for Create Account
                        if (trimmedName.isBlank()) {
                            localError = "Name cannot be empty."
                            return@Button
                        }
                        if (trimmedEmail.isBlank()) {
                            localError = "Email cannot be empty."
                            return@Button
                        }
                        if (!isValidEmail(trimmedEmail)) {
                            localError = "Please enter a valid email address."
                            return@Button
                        }
                        if (trimmedPassword.isBlank()) {
                            localError = "Password cannot be empty."
                            return@Button
                        }
                        if (trimmedPassword.length < 6) {
                            localError = "Password must be at least 6 characters long."
                            return@Button
                        }
                        if (trimmedConfirm.isBlank()) {
                            localError = "Please confirm your password."
                            return@Button
                        }
                        if (trimmedPassword != trimmedConfirm) {
                            localError = "Passwords do not match."
                            return@Button
                        }

                        // Create Account via SQLite Room
                        viewModel.register(trimmedName, trimmedEmail, trimmedPassword, onSuccess = onAuthSuccess)
                    } else {
                        // Strict validation for Login
                        if (trimmedEmail.isBlank()) {
                            localError = "Email cannot be empty."
                            return@Button
                        }
                        if (!isValidEmail(trimmedEmail)) {
                            localError = "Please enter a valid email address."
                            return@Button
                        }
                        if (trimmedPassword.isBlank()) {
                            localError = "Password cannot be empty."
                            return@Button
                        }

                        // Login via SQLite Room
                        viewModel.login(trimmedEmail, trimmedPassword, onSuccess = onAuthSuccess)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("auth_submit_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = PlantPrimary),
                shape = RoundedCornerShape(16.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = if (isRegister) "Create Account" else "Login",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Switch prompt at bottom: Login <-> Create Account
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isRegister) "Already have an account? " else "Don't have an account? ",
                    style = MaterialTheme.typography.bodyMedium.copy(color = PlantTextSecondary)
                )
                Text(
                    text = if (isRegister) "Sign In" else "Create Account",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (PlantTheme.isDark) PlantPrimary else PlantPrimaryDark
                    ),
                    modifier = Modifier
                        .clickable {
                            isRegister = !isRegister
                            localError = null
                        }
                        .padding(4.dp)
                        .testTag(if (isRegister) "auth_link_login" else "auth_link_create_account")
                )
            }
        }
    }
}
}
