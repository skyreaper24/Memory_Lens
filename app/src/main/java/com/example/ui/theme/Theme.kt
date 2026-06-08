package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CosmicDarkColorScheme = darkColorScheme(
    primary = NebulaViolet,
    secondary = DeepIndigo,
    tertiary = TealGlow,
    background = CosmicVoid,
    surface = DeepSlate,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = OffWhite,
    onSurface = OffWhite,
    surfaceVariant = ElevatedSlate,
    onSurfaceVariant = OffWhite,
    error = CoralGlow
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark Cosmic void atmosphere for premium memory aesthetics
    dynamicColor: Boolean = false, // Disable dynamic colors to preserve the custom luxury identity
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CosmicDarkColorScheme,
        typography = Typography,
        content = content
    )
}
