package com.chargeanim.pro.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

object CyberpunkColors {
    val VoidBlack = Color(0xFF050508)
    val Surface = Color(0xFF0C0C12)
    val SurfaceContainer = Color(0xFF12121A)
    val SurfaceContainerHigh = Color(0xFF1A1A24)
    val SurfaceContainerHighest = Color(0xFF22222E)
    val Cyan = Color(0xFF00E5FF)
    val CyanDim = Color(0xFF4FC3FF)
    val Magenta = Color(0xFFFF2EC4)
    val Violet = Color(0xFF7C4DFF)
    val ElectricPurple = Color(0xFFB84FFF)
    val MatrixGreen = Color(0xFF00E676)
    val HotPink = Color(0xFFFF006E)
    val WarningYellow = Color(0xFFEEFF00)
    val OnSurface = Color(0xFFEAF4FF)
    val OnSurfaceVariant = Color(0xFF8B9AB0)
    val Muted = Color(0xFF6B7788)
    val Outline = Color(0xFF2A2A36)
    val OutlineVariant = Color(0xFF1C1C28)
    val DarkScheme = darkColorScheme(
        primary=Cyan,onPrimary=Color(0xFF003640),primaryContainer=Color(0xFF004D5C),onPrimaryContainer=Color(0xFF9EFFFF),
        secondary=Violet,onSecondary=Color(0xFF1A0060),secondaryContainer=Color(0xFF2D1B6E),onSecondaryContainer=Color(0xFFE8D4FF),
        tertiary=Magenta,onTertiary=Color(0xFF3D0028),background=VoidBlack,onBackground=OnSurface,
        surface=Surface,onSurface=OnSurface,surfaceContainer=SurfaceContainer,surfaceContainerHigh=SurfaceContainerHigh,
        surfaceContainerHighest=SurfaceContainerHighest,onSurfaceVariant=OnSurfaceVariant,outline=Outline,outlineVariant=OutlineVariant
    )
}
