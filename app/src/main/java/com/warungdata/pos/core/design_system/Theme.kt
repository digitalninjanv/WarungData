package com.warungdata.pos.core.design_system

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
    primary = Color(0xFF1B6B3E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA7F5C3),
    secondary = Color(0xFF4E6354),
    tertiary = Color(0xFF3C6472),
    background = Color(0xFFF8FDF7),
    surface = Color(0xFFF8FDF7),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8BD8A8),
    onPrimary = Color(0xFF003919),
    primaryContainer = Color(0xFF00522B),
    secondary = Color(0xFFB8CCBB),
    tertiary = Color(0xFFA1CEDD),
    background = Color(0xFF101A13),
    surface = Color(0xFF101A13),
    error = Color(0xFFFFB4AB),
)

@Composable
fun WarungDataTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
