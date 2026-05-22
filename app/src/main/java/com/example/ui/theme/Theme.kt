package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = TurfGreenSoft,
    secondary = WicketGold,
    tertiary = BallRedClassic,
    background = DarkBg,
    surface = DarkSurface,
    onPrimary = Color(0xFF030712),            // Dark deep navy slate ink
    onSecondary = Color(0xFF0A0F1D),          // Dark background dark text
    onTertiary = Color.White,
    onBackground = Color(0xFFF1F5F9),         // Slate white
    onSurface = Color(0xFFF1F5F9),             // Slate white
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = Color(0xFF90CAF9)       // Soft athletic blue contrast text
)

private val LightColorScheme = lightColorScheme(
    primary = TurfGreenClassic,
    secondary = WicketWood,
    tertiary = BallRedClassic,
    background = LightBg,
    surface = LightSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF0B1424),         // Rich dark sports navy
    onSurface = Color(0xFF0B1424),             // Rich dark sports navy
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = Color(0xFF1565C0)       // High contrast classic blue
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to force our custom premium cricket theme colors
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
