package com.chargeanim.pro.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val CfBackground = Color(0xFF050508)
val CfSurface = Color(0xFF0C0C12)
val CfSurfaceContainer = Color(0xFF12121A)
val CfSurfaceContainerHigh = Color(0xFF1A1A24)
val CfSurfaceContainerHighest = Color(0xFF22222E)
val CfPrimary = Color(0xFF00E5FF)
val CfOnPrimary = Color(0xFF003640)
val CfPrimaryContainer = Color(0xFF004D5C)
val CfOnPrimaryContainer = Color(0xFF9EFFFF)
val CfSecondary = Color(0xFF7C4DFF)
val CfOnSecondary = Color(0xFF1A0060)
val CfTertiary = Color(0xFFFF4081)
val CfOnSurface = Color(0xFFE8E8F0)
val CfOnSurfaceVariant = Color(0xFFA0A0B0)
val CfOutline = Color(0xFF3A3A4A)
val CfOutlineVariant = Color(0xFF2A2A36)

val ChargeFlowDarkColors = darkColorScheme(
    primary = CfPrimary, onPrimary = CfOnPrimary,
    primaryContainer = CfPrimaryContainer, onPrimaryContainer = CfOnPrimaryContainer,
    secondary = CfSecondary, onSecondary = CfOnSecondary,
    tertiary = CfTertiary, background = CfBackground, surface = CfSurface,
    surfaceContainer = CfSurfaceContainer, surfaceContainerHigh = CfSurfaceContainerHigh,
    surfaceContainerHighest = CfSurfaceContainerHighest,
    onSurface = CfOnSurface, onSurfaceVariant = CfOnSurfaceVariant,
    outline = CfOutline, outlineVariant = CfOutlineVariant
)

val ChargeFlowTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Light, fontSize = 57.sp, letterSpacing = (-0.25).sp),
    displayMedium = TextStyle(fontWeight = FontWeight.Light, fontSize = 45.sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 32.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 28.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 24.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 22.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp)
)

@Composable
fun ChargeFlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ChargeFlowDarkColors, typography = ChargeFlowTypography, content = content)
}
