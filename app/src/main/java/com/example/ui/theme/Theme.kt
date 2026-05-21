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
    onPrimary = Color(0xFF121813),
    onSecondary = Color(0xFF1E261F),
    onTertiary = Color.White,
    onBackground = Color(0xFFE8F5E9),
    onSurface = Color(0xFFE8F5E9),
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = Color(0xFFC8E6C9)
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
    onBackground = Color(0xFF112015),
    onSurface = Color(0xFF112015),
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = Color(0xFF2E7D32)
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
