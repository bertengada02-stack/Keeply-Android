package com.keeply.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val KeeplyLightColorScheme = lightColorScheme(
    primary = KeeplyTeal,
    onPrimary = KeeplySurface,
    primaryContainer = KeeplyTealContainer,
    onPrimaryContainer = KeeplyTealDark,
    secondary = KeeplyTealDark,
    onSecondary = KeeplySurface,
    background = KeeplyBackground,
    onBackground = KeeplyTextPrimary,
    surface = KeeplySurface,
    onSurface = KeeplyTextPrimary,
    onSurfaceVariant = KeeplyTextSecondary,
    outline = KeeplyOutline
)

@Composable
fun KeeplyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = KeeplyLightColorScheme,
        typography = Typography,
        content = content
    )
}
