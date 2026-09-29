package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalPlantColors = staticCompositionLocalOf { LightPlantColors }
val LocalThemeMode = staticCompositionLocalOf { "System" }

object PlantTheme {
    val colors: PlantColors
        @Composable
        @ReadOnlyComposable
        get() = LocalPlantColors.current

    val isDark: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalPlantColors.current.isDark

    val themeMode: String
        @Composable
        @ReadOnlyComposable
        get() = LocalThemeMode.current
}

private val LightColorScheme = lightColorScheme(
    primary = PlantPrimary,
    onPrimary = Color.White,
    primaryContainer = PlantLightSoftGreen,
    onPrimaryContainer = PlantPrimaryDark,
    secondary = PlantPrimaryDark,
    onSecondary = Color.White,
    background = PlantLightBackground,
    onBackground = PlantLightText,
    surface = PlantLightSurface,
    onSurface = PlantLightText,
    surfaceVariant = PlantLightSurfaceElevated,
    onSurfaceVariant = PlantLightTextSecondary,
    outline = PlantLightBorder,
    outlineVariant = PlantLightDivider
)

private val DarkColorScheme = darkColorScheme(
    primary = PlantPrimary,
    onPrimary = Color.White,
    primaryContainer = PlantDarkSoftGreen,
    onPrimaryContainer = PlantPrimary,
    secondary = PlantPrimary,
    onSecondary = Color.White,
    background = PlantDarkBackground,
    onBackground = PlantDarkText,
    surface = PlantDarkSurface,
    onSurface = PlantDarkText,
    surfaceVariant = PlantDarkSurfaceElevated,
    onSurfaceVariant = PlantDarkTextSecondary,
    outline = PlantDarkBorder,
    outlineVariant = PlantDarkDivider
)

@Composable
fun PlantTheme(
    themePreference: String = "System",
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themePreference) {
        "Dark" -> true
        "Light" -> false
        else -> systemDark
    }

    val plantColors = if (isDark) DarkPlantColors else LightPlantColors
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = plantColors.background.toArgb()
                window.navigationBarColor = plantColors.background.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !isDark
                    isAppearanceLightNavigationBars = !isDark
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalPlantColors provides plantColors,
        LocalThemeMode provides themePreference
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = plantColors.background,
                content = content
            )
        }
    }
}

/**
 * Backward compatibility overload for boolean parameter
 */
@Composable
fun PlantTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    PlantTheme(
        themePreference = if (darkTheme) "Dark" else "Light",
        content = content
    )
}

/**
 * Semantic alias for global theme provider
 */
@Composable
fun PlantThemeProvider(
    themePreference: String = "System",
    content: @Composable () -> Unit
) {
    PlantTheme(themePreference = themePreference, content = content)
}
