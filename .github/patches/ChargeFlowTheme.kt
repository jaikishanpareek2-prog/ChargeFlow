package com.chargeanim.pro.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun ChargeFlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme=CyberpunkColors.DarkScheme,typography=CyberpunkTypography,content=content)
}
