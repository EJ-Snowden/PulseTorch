package com.denysshulhin.pulsetorch.core.design.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ColorScheme = darkColorScheme(
    primary = PTColor.Primary,
    secondary = PTColor.AccentBlue,
    background = PTColor.Background,
    surface = PTColor.SurfaceDark,
    onPrimary = PTColor.Background,
    onSecondary = PTColor.White,
    onBackground = PTColor.White,
    onSurface = PTColor.White
)

@Composable
fun PulseTorchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        typography = PTTypography,
        shapes = PTShapes,
        content = content
    )
}
