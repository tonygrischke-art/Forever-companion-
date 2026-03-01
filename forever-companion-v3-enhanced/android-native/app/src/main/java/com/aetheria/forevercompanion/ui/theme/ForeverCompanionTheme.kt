package com.aetheria.forevercompanion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AetherPurple = Color(0xFF7B2FBE)
private val StarGold = Color(0xFFFFD700)
private val NebulaBlue = Color(0xFF1A237E)
private val DarkBackground = Color(0xFF0D0D1A)

private val AetherColorScheme = darkColorScheme(
    primary = AetherPurple,
    secondary = StarGold,
    background = DarkBackground,
    surface = Color(0xFF1C1C2E),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun ForeverCompanionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AetherColorScheme,
        content = content
    )
}
