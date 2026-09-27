package com.chargeanim.pro.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

@Composable
fun ChargeFlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme=CyberpunkColors.DarkScheme,typography=CyberpunkTypography,shapes=Shapes(small=RoundedCornerShape(12.dp),medium=RoundedCornerShape(20.dp),large=RoundedCornerShape(24.dp),extraLarge=RoundedCornerShape(28.dp)),content=content)
}
