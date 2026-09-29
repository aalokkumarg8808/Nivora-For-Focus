package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.ui.viewmodel.ScreenTab

@Composable
fun PlantBottomNavBar(
    currentTab: ScreenTab,
    onTabSelected: (ScreenTab) -> Unit,
    onPlusClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 6.dp, start = 16.dp, end = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Floating iOS-Inspired Glass Dock
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .defaultMinSize(minHeight = 68.dp)
                    .wrapContentHeight()
                    .shadow(
                        elevation = 14.dp,
                        shape = RoundedCornerShape(30.dp),
                        spotColor = if (PlantTheme.isDark) Color(0x40000000) else Color(0x18111614),
                        ambientColor = Color(0x0C12B76A)
                    )
                    .border(
                        width = 1.2.dp,
                        brush = Brush.verticalGradient(
                            colors = if (PlantTheme.isDark) {
                                listOf(
                                    Color(0x2EFFFFFF),
                                    Color(0x10FFFFFF)
                                )
                            } else {
                                listOf(
                                    Color(0xE6FFFFFF),
                                    Color(0x80FFFFFF)
                                )
                            }
                        ),
                        shape = RoundedCornerShape(30.dp)
                    ),
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(
                    containerColor = PlantTheme.colors.cardBackground
                )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Subtle specular top highlight line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        if (PlantTheme.isDark) Color(0x26FFFFFF) else Color.White.copy(alpha = 0.9f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NavItem(
                            modifier = Modifier.weight(1f),
                            title = "Home",
                            selected = currentTab == ScreenTab.HOME,
                            selectedIcon = Icons.Filled.Home,
                            unselectedIcon = Icons.Outlined.Home,
                            testTag = "nav_home",
                            onClick = { onTabSelected(ScreenTab.HOME) }
                        )

                        NavItem(
                            modifier = Modifier.weight(1f),
                            title = "Tasks",
                            selected = currentTab == ScreenTab.TASKS,
                            selectedIcon = Icons.Filled.TaskAlt,
                            unselectedIcon = Icons.Outlined.TaskAlt,
                            testTag = "nav_tasks",
                            onClick = { onTabSelected(ScreenTab.TASKS) }
                        )

                        // Center spacer for the elevated Floating Action Button
                        Spacer(modifier = Modifier.width(52.dp))

                        NavItem(
                            modifier = Modifier.weight(1f),
                            title = "Focus",
                            selected = currentTab == ScreenTab.FOCUS,
                            selectedIcon = Icons.Filled.Timer,
                            unselectedIcon = Icons.Outlined.Timer,
                            testTag = "nav_focus",
                            onClick = { onTabSelected(ScreenTab.FOCUS) }
                        )

                        NavItem(
                            modifier = Modifier.weight(1f),
                            title = "Profile",
                            selected = currentTab == ScreenTab.PROFILE,
                            selectedIcon = Icons.Filled.Person,
                            unselectedIcon = Icons.Outlined.Person,
                            testTag = "nav_profile",
                            onClick = { onTabSelected(ScreenTab.PROFILE) }
                        )
                    }
                }
            }

            // Floating Center (+) Button: visually rises above the glass dock
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .offset(y = (-14).dp)
                    .shadow(
                        elevation = 10.dp,
                        shape = CircleShape,
                        spotColor = PlantPrimaryDark,
                        ambientColor = PlantPrimary
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                PlantPrimary,
                                PlantPrimaryDark
                            )
                        )
                    )
                    .border(2.5.dp, PlantTheme.colors.background, CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = Color.White)
                    ) {
                        onPlusClicked()
                    }
                    .testTag("nav_fab_plus"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Create",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    title: String,
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val tint = if (selected) PlantPrimary else PlantTheme.colors.textSecondary

    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = PlantPrimary.copy(alpha = 0.2f)),
                onClick = onClick
            )
            .testTag(testTag)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else unselectedIcon,
            contentDescription = title,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            softWrap = false,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = tint
            )
        )
    }
}
