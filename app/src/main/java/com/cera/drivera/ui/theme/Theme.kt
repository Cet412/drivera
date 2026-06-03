package com.cera.drivera.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DriveraDarkColorScheme = darkColorScheme(
    primary = DmsSuccess,
    secondary = DriveraAccentBlue,
    background = DriveraBackground,
    surface = DriveraSurface,
    error = DmsError,
    onPrimary = TextOnBrand,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun DriveraTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 2. Blend the top Status Bar with the application Background
            window.statusBarColor = DriveraDarkColorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = DriveraDarkColorScheme,
        typography = Typography, // Ensure the Typography variable exists in Type.kt
        content = content
    )
}