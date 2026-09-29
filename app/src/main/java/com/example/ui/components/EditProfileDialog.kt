package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.UserProfile
import com.example.ui.theme.*
import com.example.util.AvatarOptimizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun EditProfileDialog(
    user: UserProfile,
    onDismiss: () -> Unit,
    onSave: (newName: String, newPhotoUrl: String?) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf(user.name) }
    var photoUrl by remember { mutableStateOf(user.photoURL ?: "") }
    var selectedPreset by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var isProcessingImage by remember { mutableStateOf(false) }
    var showUrlField by remember { mutableStateOf(false) }

    // Botanical avatar presets
    val avatarPresets = listOf(
        "🌱 Seedling" to "https://images.unsplash.com/photo-1530595467537-0b5996c41f2d?w=200&auto=format&fit=crop&q=80",
        "🌿 Fern" to "https://images.unsplash.com/photo-1518531933037-91b2f5f229cc?w=200&auto=format&fit=crop&q=80",
        "🌸 Blossom" to "https://images.unsplash.com/photo-1522383225653-ed111181a951?w=200&auto=format&fit=crop&q=80",
        "🌲 Pine" to "https://images.unsplash.com/photo-1542273917363-3b1817f69a2d?w=200&auto=format&fit=crop&q=80",
        "🍃 Monstera" to "https://images.unsplash.com/photo-1614594975525-e45190c55d0b?w=200&auto=format&fit=crop&q=80",
        "🌵 Succulent" to "https://images.unsplash.com/photo-1509423350716-97f9360b4e09?w=200&auto=format&fit=crop&q=80"
    )

    // Image Picker Launcher for selecting photo from device gallery
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                isProcessingImage = true
                try {
                    // Compress, resize to max 512x512, correct EXIF orientation, and save to internal storage
                    val optimizedFile = AvatarOptimizer.compressAndSaveAvatar(
                        context = context,
                        sourceUri = uri,
                        userId = user.uid
                    )

                    if (optimizedFile != null && optimizedFile.exists()) {
                        val persistedUri = Uri.fromFile(optimizedFile).toString()
                        withContext(Dispatchers.Main) {
                            photoUrl = persistedUri
                            selectedPreset = null
                            isProcessingImage = false
                        }
                    } else {
                        // Fallback to direct URI if optimization returned null
                        withContext(Dispatchers.Main) {
                            photoUrl = uri.toString()
                            selectedPreset = null
                            isProcessingImage = false
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        photoUrl = uri.toString()
                        selectedPreset = null
                        isProcessingImage = false
                    }
                }
            }
        }
    }

    val isDark = PlantTheme.isDark
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(22.dp)
            ) {
                // Header: Title & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Profile",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 1. PROFILE PICTURE / AVATAR SECTION AT THE TOP
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar Container with dual-ring halo and Camera Edit Badge
                    Box(
                        modifier = Modifier.padding(vertical = 4.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        // Outer Halo
                        Box(
                            modifier = Modifier
                                .size(104.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            PlantPrimary.copy(alpha = if (isDark) 0.35f else 0.22f),
                                            Color.Transparent
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            // Avatar Core
                            Box(
                                modifier = Modifier
                                    .size(92.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF1E2A23) else PlantSoftGreen)
                                    .border(
                                        width = 2.5.dp,
                                        brush = Brush.sweepGradient(
                                            colors = listOf(
                                                PlantPrimary,
                                                PlantSoftGreen,
                                                PlantPrimaryDark,
                                                PlantPrimary
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                    .testTag("avatar_preview_box"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isProcessingImage) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        color = PlantPrimary,
                                        strokeWidth = 2.5.dp
                                    )
                                } else if (photoUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(photoUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Profile Photo Preview",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                } else {
                                    val initials = name.split(" ")
                                        .filter { it.isNotBlank() }
                                        .take(2)
                                        .map { it.first().uppercase() }
                                        .joinToString("")
                                        .ifBlank { user.name.take(2).uppercase().ifBlank { "BO" } }
                                    Text(
                                        text = initials,
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = PlantPrimaryDark,
                                            fontSize = 28.sp,
                                            letterSpacing = 1.sp
                                        )
                                    )
                                }
                            }
                        }

                        // Floating Camera Badge
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .offset(x = (-2).dp, y = (-2).dp)
                                .shadow(elevation = 6.dp, shape = CircleShape, spotColor = PlantPrimaryDark)
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            PlantPrimary,
                                            PlantPrimaryDark
                                        )
                                    )
                                )
                                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .testTag("btn_change_photo_badge"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = "Change Photo",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // "Change Photo" / Gallery button & Remove photo actions
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PlantPrimary),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = PlantPrimaryDark
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("btn_change_photo")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = PlantPrimaryDark
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Change Photo",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        if (photoUrl.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    photoUrl = ""
                                    selectedPreset = null
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Remove",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. BOTANICAL AVATAR PRESETS
                Text(
                    text = "Botanical Avatar Presets",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(avatarPresets) { (label, url) ->
                        val isSelected = photoUrl == url
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) PlantSoftGreen else MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    1.dp,
                                    if (isSelected) PlantPrimary else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    photoUrl = url
                                    selectedPreset = label
                                }
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) PlantPrimaryDark else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = PlantPrimaryDark,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Optional Custom URL toggle
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(
                    onClick = { showUrlField = !showUrlField },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (showUrlField) "Hide Image URL option" else "+ Enter custom Image URL",
                        style = MaterialTheme.typography.labelSmall.copy(color = PlantPrimaryDark)
                    )
                }

                if (showUrlField) {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = photoUrl,
                        onValueChange = { photoUrl = it },
                        label = { Text("Photo URL") },
                        placeholder = { Text("https://example.com/avatar.jpg") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PlantPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. FULL NAME FIELD
                Text(
                    text = "Name",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Your Name") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = PlantPrimary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_edit_name"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PlantPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 4. EMAIL FIELD (READ-ONLY)
                Text(
                    text = "Email (Linked Account)",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = user.email,
                    onValueChange = {},
                    readOnly = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Locked",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 5. ACTION BUTTONS (Cancel & Save)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                isSaving = true
                                onSave(name.trim(), photoUrl.trim().ifBlank { null })
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PlantPrimary),
                        shape = RoundedCornerShape(14.dp),
                        enabled = !isSaving && name.isNotBlank(),
                        modifier = Modifier.testTag("btn_save_profile")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Save Changes",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
