package com.chargeanim.pro.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val CyberpunkTypography = Typography(
    displayLarge=TextStyle(fontFamily=CyberpunkFonts.Orbitron,fontWeight=FontWeight.Bold,fontSize=57.sp,letterSpacing=(-0.5).sp),
    displayMedium=TextStyle(fontFamily=CyberpunkFonts.Orbitron,fontWeight=FontWeight.Bold,fontSize=44.sp),
    headlineMedium=TextStyle(fontFamily=CyberpunkFonts.Orbitron,fontWeight=FontWeight.Bold,fontSize=28.sp),
    titleLarge=TextStyle(fontFamily=CyberpunkFonts.Exo2,fontWeight=FontWeight.Medium,fontSize=22.sp),
    titleMedium=TextStyle(fontFamily=CyberpunkFonts.Exo2,fontWeight=FontWeight.Medium,fontSize=16.sp),
    bodyLarge=TextStyle(fontFamily=CyberpunkFonts.Exo2,fontWeight=FontWeight.Normal,fontSize=16.sp),
    bodyMedium=TextStyle(fontFamily=CyberpunkFonts.Rajdhani,fontWeight=FontWeight.Normal,fontSize=14.sp),
    labelLarge=TextStyle(fontFamily=CyberpunkFonts.Rajdhani,fontWeight=FontWeight.Medium,fontSize=14.sp),
    labelSmall=TextStyle(fontFamily=CyberpunkFonts.ShareTechMono,fontWeight=FontWeight.Normal,fontSize=11.sp)
)
val MetricNumberStyle=TextStyle(fontFamily=CyberpunkFonts.ShareTechMono,fontWeight=FontWeight.Normal,fontSize=18.sp,color=CyberpunkColors.OnSurface)
val MetricLabelStyle=TextStyle(fontFamily=CyberpunkFonts.Rajdhani,fontWeight=FontWeight.Medium,fontSize=11.sp,color=CyberpunkColors.Muted)
