package com.chargeanim.pro.ui.theme

import android.view.View

object VibesThemeManager {
    fun applyTheme(view: View, themeType: ThemeType) {
        val drawableName = when (themeType) {
            ThemeType.MIDNIGHT_GARDEN -> "bg_midnight_garden"
            ThemeType.CELESTIAL_SPARKLE -> "bg_celestial_sparkle"
            ThemeType.ENCHANTED_FOREST -> "bg_enchanted_forest"
            ThemeType.OCEAN_ABYSS -> "bg_ocean_abyss"
        }
        val resourceId = view.context.resources.getIdentifier(
            drawableName, "drawable", view.context.packageName
        )
        if (resourceId != 0) view.setBackgroundResource(resourceId)
    }
}

enum class ThemeType {
    MIDNIGHT_GARDEN,
    CELESTIAL_SPARKLE,
    ENCHANTED_FOREST,
    OCEAN_ABYSS
}
