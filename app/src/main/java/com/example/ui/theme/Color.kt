package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Plant: Focus & Study Centralized Semantic Theme Tokens.
 * Supports Light, Dark, and System modes with strict contrast and design fidelity.
 */

// Accent colors (Identical across themes for brand identity)
val PlantPrimary = Color(0xFF12B76A)
val PlantPrimaryDark = Color(0xFF079455)

// Accents for Priority & Categories
val PlantOrange = Color(0xFFF79009)
val PlantOrangeSoft = Color(0xFFFEF0C7)
val PlantRed = Color(0xFFF04438)
val PlantRedSoft = Color(0xFFFEE4E2)
val PlantBlue = Color(0xFF2E90FA)
val PlantBlueSoft = Color(0xFFD1E9FF)
val PlantPurple = Color(0xFF7A5AF8)
val PlantPurpleSoft = Color(0xFFECE9FE)

// Static Light Design Tokens
val PlantLightBackground = Color(0xFFF7F9F8)
val PlantLightSurface = Color(0xFFFFFFFF)
val PlantLightSurfaceElevated = Color(0xFFFFFFFF)
val PlantLightGlass = Color(0x94FFFFFF) // rgba(255,255,255,.58)
val PlantLightGlassBorder = Color(0xBFFFFFFF) // rgba(255,255,255,.75)
val PlantLightText = Color(0xFF111614)
val PlantLightTextSecondary = Color(0xFF6C7772)
val PlantLightTextTertiary = Color(0xFF9AA5A0)
val PlantLightBorder = Color(0xFFE7ECE9)
val PlantLightDivider = Color(0xFFE7ECE9)
val PlantLightSoftGreen = Color(0xFFE6F8ED)
val PlantLightSoftGreenBorder = Color(0xFFBCE8D0)
val PlantLightCardBackground = Color(0xF7FFFFFF)
val PlantLightCardBorder = Color(0xDDE7ECE9)
val PlantLightInputBackground = Color(0xF2FFFFFF)
val PlantLightInputBorder = Color(0xFFE7ECE9)
val PlantLightProgressTrack = Color(0xFFEAF5EE)

// Static Dark Design Tokens
val PlantDarkBackground = Color(0xFF0B0F0D)
val PlantDarkSurface = Color(0xFF111714)
val PlantDarkSurfaceElevated = Color(0xFF151D18)
val PlantDarkGlass = Color(0xAD161E19) // rgba(22,30,25,.68)
val PlantDarkGlassBorder = Color(0x14FFFFFF) // rgba(255,255,255,.08)
val PlantDarkText = Color(0xFFF2F7F4)
val PlantDarkTextSecondary = Color(0xFFA7B5AE)
val PlantDarkTextTertiary = Color(0xFF75857E)
val PlantDarkBorder = Color(0xFF26342C)
val PlantDarkDivider = Color(0xFF26342C)
val PlantDarkSoftGreen = Color(0xFF123B28)
val PlantDarkSoftGreenBorder = Color(0xFF1C4D35)
val PlantDarkCardBackground = Color(0xF7151D18)
val PlantDarkCardBorder = Color(0xFF26342C)
val PlantDarkInputBackground = Color(0xF018221E)
val PlantDarkInputBorder = Color(0xFF26342C)
val PlantDarkProgressTrack = Color(0xFF1F2B26)

@Immutable
data class PlantColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val glass: Color,
    val glassBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val border: Color,
    val divider: Color,
    val accent: Color,
    val accentDark: Color,
    val accentSoft: Color,
    val accentSoftBorder: Color,
    val cardBackground: Color,
    val cardBorder: Color,
    val inputBackground: Color,
    val inputBorder: Color,
    val progressTrack: Color,
    val isDark: Boolean
)

val LightPlantColors = PlantColors(
    background = PlantLightBackground,
    surface = PlantLightSurface,
    surfaceElevated = PlantLightSurfaceElevated,
    glass = PlantLightGlass,
    glassBorder = PlantLightGlassBorder,
    textPrimary = PlantLightText,
    textSecondary = PlantLightTextSecondary,
    textTertiary = PlantLightTextTertiary,
    border = PlantLightBorder,
    divider = PlantLightDivider,
    accent = PlantPrimary,
    accentDark = PlantPrimaryDark,
    accentSoft = PlantLightSoftGreen,
    accentSoftBorder = PlantLightSoftGreenBorder,
    cardBackground = PlantLightCardBackground,
    cardBorder = PlantLightCardBorder,
    inputBackground = PlantLightInputBackground,
    inputBorder = PlantLightInputBorder,
    progressTrack = PlantLightProgressTrack,
    isDark = false
)

val DarkPlantColors = PlantColors(
    background = PlantDarkBackground,
    surface = PlantDarkSurface,
    surfaceElevated = PlantDarkSurfaceElevated,
    glass = PlantDarkGlass,
    glassBorder = PlantDarkGlassBorder,
    textPrimary = PlantDarkText,
    textSecondary = PlantDarkTextSecondary,
    textTertiary = PlantDarkTextTertiary,
    border = PlantDarkBorder,
    divider = PlantDarkDivider,
    accent = PlantPrimary,
    accentDark = PlantPrimaryDark,
    accentSoft = PlantDarkSoftGreen,
    accentSoftBorder = PlantDarkSoftGreenBorder,
    cardBackground = PlantDarkCardBackground,
    cardBorder = PlantDarkCardBorder,
    inputBackground = PlantDarkInputBackground,
    inputBorder = PlantDarkInputBorder,
    progressTrack = PlantDarkProgressTrack,
    isDark = true
)

// Dynamic Composable Tokens that adapt to the active PlantTheme everywhere
val PlantBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = PlantTheme.colors.background

val PlantSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = PlantTheme.colors.surface

val PlantSurfaceElevated: Color
    @Composable
    @ReadOnlyComposable
    get() = PlantTheme.colors.surfaceElevated

val PlantSurfaceVariant: Color
    @Composable
    @ReadOnlyComposable
    get() = PlantTheme.colors.surfaceElevated

val PlantGlassSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = PlantTheme.colors.glass

val PlantGlassBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = PlantTheme.colors.glassBorder

val PlantText: Color
    @Composable
    @ReadOnlyComposable
    get() = PlantTheme.colors.textPrimary

val PlantTextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = PlantTheme.colors.textSecondary

val PlantTextTertiary: Color
    @Composable
    @ReadOnlyComposable
    get() = PlantTheme.colors.textTertiary

val PlantBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = PlantTheme.colors.border

val PlantDivider: Color
    @Composable
    @ReadOnlyComposable
    get() = PlantTheme.colors.divider

val PlantSoftGreen: Color
    @Composable
    @ReadOnlyComposable
    get() = PlantTheme.colors.accentSoft

val PlantSoftGreenBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = PlantTheme.colors.accentSoftBorder

val PlantCardBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = PlantTheme.colors.cardBackground

val PlantCardBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = PlantTheme.colors.cardBorder

val PlantInputBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = PlantTheme.colors.inputBackground

val PlantInputBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = PlantTheme.colors.inputBorder

val PlantProgressTrack: Color
    @Composable
    @ReadOnlyComposable
    get() = PlantTheme.colors.progressTrack
