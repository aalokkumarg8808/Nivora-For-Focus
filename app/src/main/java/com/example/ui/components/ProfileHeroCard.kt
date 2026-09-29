package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.*

@Composable
fun ProfileHeroCard(
    userName: String,
    userEmail: String,
    userPhotoUrl: String?,
    userXp: Int,
    userLevel: Int,
    levelProgress: Float,
    joinDateStr: String,
    isDark: Boolean,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentXpInLevel = userXp % 100
    val xpRequiredForNext = 100
    val xpRemaining = (xpRequiredForNext - currentXpInLevel).coerceAtLeast(0)

    // Animated real-time progress for smooth XP bar filling
    val animatedProgress by animateFloatAsState(
        targetValue = levelProgress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "xpProgress"
    )

    // Layered glass background & borders
    val glassBgGradient = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xCC1F2C26),
                Color(0xB316201B)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xF2FFFFFF),
                Color(0xD9F4F8F5)
            )
        )
    }

    val glassBorderColor = if (isDark) {
        Color(0x403F5E4F)
    } else {
        Color(0xB3FFFFFF)
    }

    val innerHighlight = if (isDark) {
        Color(0x1AFFFFFF)
    } else {
        Color(0x80FFFFFF)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(28.dp),
                spotColor = if (isDark) Color(0x33000000) else Color(0x18111614),
                ambientColor = if (isDark) Color(0x22079455) else Color(0x1012B76A)
            )
            .border(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        glassBorderColor,
                        glassBorderColor.copy(alpha = 0.4f)
                    )
                ),
                shape = RoundedCornerShape(28.dp)
            )
            .testTag("profile_hero_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(glassBgGradient)
        ) {
            // Subtle top inner highlight rim for realistic glass reflection
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                innerHighlight,
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Profile Avatar with dual-ring halo and Camera Edit Overlay
                Box(
                    modifier = Modifier.padding(top = 2.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    // Outer aura halo
                    Box(
                        modifier = Modifier
                            .size(96.dp)
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
                        // Avatar core
                        Box(
                            modifier = Modifier
                                .size(86.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isDark) Color(0xFF1E2A23) else Color(0xFFEAF5EF)
                                )
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
                                .clickable(onClick = onAvatarClick)
                                .testTag("avatar_container"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!userPhotoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = userPhotoUrl,
                                    contentDescription = "Profile Photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            } else {
                                // Dynamic initial derived from user's actual name
                                val initials = androidx.compose.runtime.remember(userName) {
                                    userName.split(" ")
                                        .filter { it.isNotBlank() }
                                        .take(2)
                                        .map { it.first().uppercase() }
                                        .joinToString("")
                                        .ifBlank { "B" }
                                }
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

                    // Floating camera edit icon badge
                    Box(
                        modifier = Modifier
                            .size(30.dp)
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
                            .clickable(onClick = onAvatarClick)
                            .testTag("btn_avatar_edit"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = "Change Avatar",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. User Name
                Text(
                    text = userName,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 22.sp
                    ),
                    modifier = Modifier.testTag("profile_hero_name")
                )

                // 3. User Email
                if (userEmail.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MailOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = userEmail,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            modifier = Modifier.testTag("profile_hero_email")
                        )
                    }
                }

                // 4. Gardening Since Badge
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isDark) PlantDarkSoftGreen else PlantLightSoftGreen
                        )
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "🌱 Gardening since $joinDateStr",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isDark) PlantPrimary else PlantPrimaryDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 5. Level Badge & Real-Time XP Progress Section (Sub-glass capsule)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .border(
                            width = 1.dp,
                            color = if (isDark) PlantDarkBorder else PlantSoftGreenBorder.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0x66151D18) else Color(0x73FFFFFF)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Level Badge Header & Numeric XP Counter
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Level Pill Badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                PlantPrimary.copy(alpha = 0.18f),
                                                PlantSoftGreen.copy(alpha = 0.35f)
                                            )
                                        )
                                    )
                                    .border(
                                        1.dp,
                                        PlantPrimary.copy(alpha = 0.4f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                    .testTag("profile_level_badge")
                            ) {
                                Icon(
                                    imageVector = if (userLevel >= 5) Icons.Filled.WorkspacePremium else Icons.Filled.Eco,
                                    contentDescription = null,
                                    tint = if (isDark) PlantPrimary else PlantPrimaryDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Level $userLevel Botanist",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) PlantPrimary else PlantPrimaryDark,
                                        fontSize = 13.sp
                                    )
                                )
                            }

                            // Dynamic XP Fraction
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "$currentXpInLevel / $xpRequiredForNext XP",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = PlantText,
                                        fontSize = 13.sp
                                    ),
                                    modifier = Modifier.testTag("profile_xp_fraction")
                                )
                                Text(
                                    text = "$xpRemaining XP to Level ${userLevel + 1}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = PlantTextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Real-Time Animated Glass Progress Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    PlantTheme.colors.progressTrack
                                )
                                .testTag("profile_xp_progress_bar")
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedProgress)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                PlantPrimaryDark,
                                                PlantPrimary,
                                                Color(0xFF34D399)
                                            )
                                        )
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Bottom progress metrics row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Current Tier: ${(levelProgress * 100).toInt()}% Complete",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = PlantTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )

                            Text(
                                text = "Total Earned: $userXp XP",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isDark) PlantPrimary else PlantPrimaryDark,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
