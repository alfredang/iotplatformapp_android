package com.tertiaryinfotech.iotflow.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Teal = Color(0xFF14B8A6)
val TealLight = Color(0xFF2DD4BF)
val Navy = Color(0xFF0D1226)
val Navy2 = Color(0xFF1A2147)

private val DarkColors = darkColorScheme(
    primary = TealLight,
    onPrimary = Color(0xFF06231F),
    secondary = Teal,
    background = Color(0xFF0B0F1E),
    surface = Color(0xFF141A2E),
    surfaceVariant = Color(0xFF1C2336),
)

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    secondary = TealLight,
    background = Color(0xFFF6F7FB),
    surface = Color.White,
    surfaceVariant = Color(0xFFEDEFF5),
)

@Composable
fun IoTFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
