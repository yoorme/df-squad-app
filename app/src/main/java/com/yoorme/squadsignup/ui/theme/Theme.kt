package com.yoorme.squadsignup.ui.theme

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

private val LightColors = lightColorScheme(
    primary = Color(0xFF1A73E8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E7FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF575E71),
    surface = Color(0xFFFCFCFF),
    background = Color(0xFFF5F6FA),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA0C9FF),
    onPrimary = Color(0xFF00315C),
    primaryContainer = Color(0xFF004787),
    onPrimaryContainer = Color(0xFFD6E7FF),
    secondary = Color(0xFFBFC6DC),
    surface = Color(0xFF1B1B1F),
    background = Color(0xFF121214),
)

@Composable
fun SquadTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
