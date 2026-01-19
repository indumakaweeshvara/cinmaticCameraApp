package com.cinecam.cinematiccamera.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = CineCamColors.Primary,
    secondary = CineCamColors.Secondary,
    tertiary = CineCamColors.PrimaryVariant,
    background = CineCamColors.Background,
    surface = CineCamColors.Surface,
    onPrimary = CineCamColors.OnPrimary,
    onSecondary = CineCamColors.OnPrimary,
    onTertiary = CineCamColors.OnPrimary,
    onBackground = CineCamColors.OnBackground,
    onSurface = CineCamColors.OnSurface,
    error = CineCamColors.Error,
    onError = Color.White
)

@Composable
fun CinematicCameraTheme(
    darkTheme: Boolean = true, // Always dark for cinematic look
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
