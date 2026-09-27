package com.chargeflow.theme

import androidx.compose.ui.graphics.Color

enum class ThemeId {
    FUTURISTIC, SPACE, ELECTRIC, FIRE, WATER, ICE,
    NEON, MATRIX, NATURE, MINIMAL, ANIME, ABSTRACT,
    MIDNIGHT_GARDEN, CELESTIAL_SPARKLE, ENCHANTED_FOREST, OCEAN_ABYSS
}

data class FlagshipTheme(
    val id: ThemeId,
    val displayName: String,
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val background: Color,
    val particleColors: List<Color>,
    val ringColors: List<Color>,
    val glowIntensity: Float = 0.7f,
    val particleCount: Int = 70,
    val particleSpeed: ClosedFloatingPointRange<Float> = 0.6f..2.2f,
    val particleSize: ClosedFloatingPointRange<Float> = 2.5f..7f,
    val upwardBias: Boolean = true,
    val gravity: Float = -0.015f,
    val ringSegments: Int = 36,
    val ringStroke: Float = 3.5f,
    val pulseSpeed: Float = 1.0f,
    val hasLightning: Boolean = false,
    val hasRipple: Boolean = false,
    val hasMatrix: Boolean = false,
    val hasRibbon: Boolean = false,
    val hasCrystals: Boolean = false
)
