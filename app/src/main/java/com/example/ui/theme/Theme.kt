package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ViceDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = ViceObsidian,
    primaryContainer = NeonPurpleDeep,
    onPrimaryContainer = TextPrimary,
    secondary = NeonPink,
    onSecondary = TextPrimary,
    secondaryContainer = ViceCardElevated,
    onSecondaryContainer = NeonPink,
    tertiary = NeonPurple,
    onTertiary = TextPrimary,
    background = ViceObsidian,
    onBackground = TextPrimary,
    surface = ViceSurface,
    onSurface = TextPrimary,
    surfaceVariant = ViceCard,
    onSurfaceVariant = TextSecondary,
    outline = ViceOutline,
    error = NeonRed,
    onError = TextPrimary
)

private val ViceShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun GtaViClipAiTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ViceDarkColorScheme,
        typography = AppTypography,
        shapes = ViceShapes,
        content = content
    )
}
