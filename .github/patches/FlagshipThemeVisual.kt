package com.chargeanim.pro.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.chargeflow.theme.FlagshipThemes
import com.chargeflow.theme.ThemeId
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun FlagshipThemeVisual(
    themeId: ThemeId,
    modifier: Modifier = Modifier,
    active: Boolean = true,
    animated: Boolean = true,
    content: @Composable () -> Unit = {}
) {
    val theme = FlagshipThemes.get(themeId)
    val particles = remember(theme.id) {
        val r = Random(theme.id.ordinal * 7919 + 17)
        List(theme.particleCount.coerceAtMost(150)) {
            ParticleSeed(
                r.nextFloat(), r.nextFloat(), r.nextFloat(), r.nextFloat(),
                r.nextInt(0, theme.particleColors.size)
            )
        }
    }

    val phase: Float
    val pulse: Float
    if (animated) {
        val transition = rememberInfiniteTransition(label = "flagship-" + theme.id.name)
        val p by transition.animateFloat(
            0f, 1f,
            infiniteRepeatable(
                tween((9000f / theme.pulseSpeed.coerceAtLeast(0.1f)).toInt(), easing = LinearEasing)
            ),
            label = "phase"
        )
        val q by transition.animateFloat(
            0.78f, 1f,
            infiniteRepeatable(
                tween((1400f / theme.pulseSpeed.coerceAtLeast(0.1f)).toInt(), easing = LinearEasing),
                RepeatMode.Reverse
            ),
            label = "pulse"
        )
        phase = p
        pulse = q
    } else {
        phase = 0.5f
        pulse = 0.9f
    }

    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension * 0.45f
            val glow = theme.glowIntensity.coerceIn(0f, 1f) * if (active) pulse else 0.32f

            drawRect(theme.background)
            drawCircle(
                Brush.radialGradient(
                    listOf(
                        theme.primary.copy(alpha = 0.20f * glow),
                        theme.secondary.copy(alpha = 0.06f * glow),
                        Color.Transparent
                    ),
                    center,
                    radius * 1.18f
                ),
                radius * 1.18f,
                center
            )

            when (theme.id) {
                ThemeId.FUTURISTIC -> drawFuturistic(center, radius, theme.primary, theme.secondary, theme.ringColors, theme.ringSegments, theme.ringStroke, phase, glow)
                ThemeId.SPACE -> drawSpace(center, radius, theme, particles, phase, active)
                ThemeId.ELECTRIC -> drawElectric(center, radius, theme.primary, theme.secondary, theme.accent, phase, glow)
                ThemeId.FIRE -> drawFire(center, radius, theme, particles, phase, active)
                ThemeId.WATER -> drawWater(center, radius, theme.primary, theme.secondary, theme.accent, phase, glow)
                ThemeId.ICE -> drawIce(center, radius, theme.primary, theme.secondary, theme.accent, phase, glow)
                ThemeId.NEON -> drawNeon(center, radius, theme, phase, glow)
                ThemeId.MATRIX -> drawMatrix(center, radius, theme, particles, phase, active)
                ThemeId.NATURE -> drawNature(center, radius, theme, particles, phase, active)
                ThemeId.MINIMAL -> drawMinimal(center, radius, theme, phase, glow)
                ThemeId.ANIME -> drawAnime(center, radius, theme, particles, phase, active)
                ThemeId.ABSTRACT -> drawAbstract(center, radius, theme.primary, theme.secondary, theme.accent, phase, glow)
                ThemeId.MIDNIGHT_GARDEN -> drawMidnightGarden(center, radius, theme, particles, phase, active)
                ThemeId.CELESTIAL_SPARKLE -> drawCelestial(center, radius, theme, particles, phase, active)
                ThemeId.ENCHANTED_FOREST -> drawEnchantedForest(center, radius, theme, particles, phase, active)
                ThemeId.OCEAN_ABYSS -> drawOceanAbyss(center, radius, theme, particles, phase, glow)
            }
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
    }
}

private fun DrawScope.drawFuturistic(c: Offset, r: Float, p: Color, s: Color, rings: List<Color>, segments: Int, stroke: Float, phase: Float, glow: Float) {
    drawCircle(p.copy(alpha=.08f*glow), r*.74f, c)
    repeat(3) { layer ->
        val rr = r * (0.46f + layer*.17f)
        val count = (segments / 3).coerceIn(10, 32)
        for (i in 0 until count) if ((i + layer) % 2 == 0) {
            drawArc(rings[layer % rings.size].copy(alpha=.75f*glow), phase*360f*(if(layer%2==0)1f else -1f)+i*360f/count, 18f, false,
                Offset(c.x-rr,c.y-rr), Size(rr*2,rr*2), style=Stroke(stroke*(1f-layer*.15f)))
        }
    }
    drawCircle(s.copy(alpha=.8f*glow), r*.12f, c, style=Stroke(stroke*.65f))
    repeat(8) { i ->
        val a = i*45f + phase*30f
        val pt = polar(c,r*.76f,a)
        drawCircle(p.copy(alpha=.9f*glow), if(i%2==0) 2.4f else 1.4f, pt)
    }
}

private fun DrawScope.drawSpace(c: Offset, r: Float, t: com.chargeflow.theme.FlagshipTheme, ps: List<ParticleSeed>, phase: Float, active: Boolean) {
    ps.forEachIndexed { i, p ->
        val depth = .25f + p.size*.75f
        val x = ((p.x + sin(phase*6.28f+p.phase)*.025f) % 1f) * size.width
        val y = ((p.y + phase*(.02f + p.size*.03f)) % 1f) * size.height
        val col=t.particleColors[p.colorIndex]
        drawCircle(col.copy(alpha=(.25f+.7f*p.size)*(if(active)1f:.45f)), (1f+p.size*3f)*depth, Offset(x,y))
        if (i%17==0) drawCircle(Color.White.copy(alpha=.55f), 1f+p.size*2f, Offset(x,y))
    }
    drawArc(t.secondary.copy(alpha=.35f),phase*360f,r*.22f,false,Offset(c.x-r*.55f,c.y-r*.55f),Size(r*1.1f,r*1.1f),style=Stroke(1.5f))
}

private fun DrawScope.drawElectric(c: Offset, r: Float, p: Color, s: Color, a: Color, phase: Float, glow: Float) {
    val bolt = Path()
    bolt.moveTo(c.x-r*.28f,c.y-r*.86f)
    bolt.lineTo(c.x-r*.03f,c.y-r*.28f)
    bolt.lineTo(c.x-r*.22f,c.y-r*.28f)
    bolt.lineTo(c.x+r*.34f,c.y+.82f*r)
    drawPath(bolt,s.copy(alpha=.22f*glow),style=Stroke(r*.055f))
    drawPath(bolt,a.copy(alpha=.92f*glow),style=Stroke(2.2f))
    val branches=listOf(
        arrayOf(Offset(c.x-r*.03f,c.y-r*.28f),Offset(c.x+r*.36f,c.y-r*.56f),Offset(c.x+r*.60f,c.y-r*.42f)),
        arrayOf(Offset(c.x-r*.18f,c.y-r*.05f),Offset(c.x-r*.56f,c.y+r*.12f),Offset(c.x-r*.70f,c.y+.02f*r)),
        arrayOf(Offset(c.x+r*.08f,c.y+r*.22f),Offset(c.x-r*.20f,c.y+r*.50f),Offset(c.x-r*.42f,c.y+r*.44f))
    )
    branches.forEachIndexed { i, pts ->
        val path=Path().apply{moveTo(pts[0].x,pts[0].y);lineTo(pts[1].x,pts[1].y);lineTo(pts[2].x,pts[2].y)}
        drawPath(path,p.copy(alpha=.72f*glow),style=Stroke(1.3f))
        if (i==0) drawCircle(Color.White.copy(alpha=.65f*glow),2f,pts[1])
    }
    drawCircle(s.copy(alpha=.16f*glow),r*.38f,c)
    drawCircle(Color.White.copy(alpha=.7f*glow),2.5f,Offset(c.x+r*.06f*sin(phase*6.28f),c.y+r*.12f*cos(phase*6.28f)))
}

private fun DrawScope.drawFire(c: Offset, r: Float, t: com.chargeflow.theme.FlagshipTheme, ps: List<ParticleSeed>, phase: Float, active: Boolean) {
    ps.forEach { p ->
        val rise=(phase*(.45f+p.size*1.4f)+p.phase)%1f
        val x=c.x+(p.x-.5f)*r*1.6f+sin(rise*10f+p.phase)*r*.08f
        val y=c.y+r*.72f-rise*r*1.55f
        val col=t.particleColors[p.colorIndex]
        val alpha=(.2f+.8f*(1f-p.size))*(if(active)1f:.45f)
        drawCircle(col.copy(alpha=alpha),1.5f+p.size*4f,Offset(x,y))
        if(p.size>.55f) drawLine(col.copy(alpha=alpha*.55f),Offset(x,y+5f),Offset(x,y+15f+p.size*10f),1.2f)
    }
    val flame=Path().apply{moveTo(c.x,c.y+r*.45f);quadraticTo(c.x-r*.42f,c.y+r*.05f,c.x-r*.12f,c.y-r*.25f);quadraticTo(c.x,c.y-.02f*r,c.x+.04f*r,c.y-r*.50f);quadraticTo(c.x+r*.34f,c.y+.02f*r,c.x+r*.12f,c.y+r*.40f);close()}
    drawPath(flame,t.secondary.copy(alpha=.18f),style=Stroke(2f))
    drawCircle(t.accent.copy(alpha=.32f),r*.16f,c)
}

private fun DrawScope.drawWater(c: Offset, r: Float, p: Color, s: Color, a: Color, phase: Float, glow: Float) {
    repeat(5) { i ->
        val rr=r*(.16f+((phase+i*.2f)%1f)*.76f)
        drawCircle(p.copy(alpha=(.42f*(1f-((phase+i*.2f)%1f)))*glow),rr,c,style=Stroke(2.2f))
    }
    for(i in 0..5){
        val y=c.y-r*.45f+i*r*.18f+sin(phase*6.28f+i)*5f
        drawArc(s.copy(alpha=.22f*glow),phase*90f+i*38f,75f,false,Offset(c.x-r*.68f,y-r*.04f),Size(r*1.36f,r*.08f),style=Stroke(1.5f))
    }
    drawCircle(a.copy(alpha=.16f*glow),r*.23f,c)
}

private fun DrawScope.drawIce(c: Offset, r: Float, p: Color, s: Color, a: Color, phase: Float, glow: Float) {
    repeat(2){i->drawArc(s.copy(alpha=.45f*glow),phase*30f+i*180f,130f,false,Offset(c.x-r*.72f,c.y-r*.72f),Size(r*1.44f,r*1.44f),style=Stroke(1.5f))}
    val tri=Path().apply{moveTo(c.x,c.y-r*.46f);lineTo(c.x+r*.42f,c.y+r*.25f);lineTo(c.x-r*.42f,c.y+r*.25f);close()}
    drawPath(tri,p.copy(alpha=.16f*glow))
    drawPath(tri,p.copy(alpha=.85f*glow),style=Stroke(2.4f))
    drawLine(p.copy(alpha=.45f),Offset(c.x,c.y-r*.46f),Offset(c.x,c.y+r*.25f),1f)
    drawLine(s.copy(alpha=.4f),Offset(c.x-r*.42f,c.y+r*.25f),Offset(c.x+r*.42f,c.y+r*.25f),1f)
    repeat(6){i->val pt=polar(c,r*.75f,i*60f+phase*25f);drawCircle(a.copy(alpha=.7f*glow),2f,pt)}
}

private fun DrawScope.drawNeon(c: Offset, r: Float, t: com.chargeflow.theme.FlagshipTheme, phase: Float, glow: Float) {
    repeat(3){i->
        val rr=r*(.35f+i*.18f)
        drawArc(t.ringColors[i%2].copy(alpha=.32f*glow),phase*120f+i*120f,240f,false,Offset(c.x-rr,c.y-rr),Size(rr*2,rr*2),style=Stroke(6f))
        drawArc(t.particleColors[i%t.particleColors.size].copy(alpha=.85f*glow),phase*120f+i*120f,110f,false,Offset(c.x-rr,c.y-rr),Size(rr*2,rr*2),style=Stroke(1.8f))
    }
    drawLine(t.accent.copy(alpha=.65f*glow),Offset(c.x-r*.8f,c.y),Offset(c.x+r*.8f,c.y),1.2f)
    drawLine(t.secondary.copy(alpha=.5f*glow),Offset(c.x,c.y-r*.8f),Offset(c.x,c.y+r*.8f),1.2f)
}

private fun DrawScope.drawMatrix(c: Offset, r: Float, t: com.chargeflow.theme.FlagshipTheme, ps: List<ParticleSeed>, phase: Float, active: Boolean) {
    ps.forEach { p ->
        val x=(p.x*size.width)
        val y=((p.y+phase*(.25f+p.size*.55f))%1f)*size.height
        val col=t.particleColors[p.colorIndex]
        drawRect(col.copy(alpha=(.25f+.7f*p.size)*(if(active)1f:.4f)),Offset(x,y),Size(1.2f+p.size*2f,4f+p.size*7f))
    }
    drawCircle(t.primary.copy(alpha=.12f),r*.48f,c)
}

private fun DrawScope.drawNature(c: Offset, r: Float, t: com.chargeflow.theme.FlagshipTheme, ps: List<ParticleSeed>, phase: Float, active: Boolean) {
    ps.forEach { p ->
        val a=p.phase*360f+phase*40f
        val rr=r*(.22f+p.y*.58f)
        val pt=polar(c,rr,a)
        val col=t.particleColors[p.colorIndex]
        drawCircle(col.copy(alpha=(.28f+.55f*p.size)*(if(active)1f:.45f)),1.5f+p.size*3f,pt)
    }
    repeat(4){i->
        val y=c.y-r*.35f+i*r*.23f
        val path=Path().apply{moveTo(c.x-r*.55f,y);quadraticTo(c.x,y+sin(phase*6.28f+i)*r*.12f,c.x+r*.55f,y)}
        drawPath(path,t.secondary.copy(alpha=.18f),style=Stroke(2f))
    }
}

private fun DrawScope.drawMinimal(c: Offset, r: Float, t: com.chargeflow.theme.FlagshipTheme, phase: Float, glow: Float) {
    repeat(3){i->
        val rr=r*(.42f+i*.16f)
        drawArc(t.ringColors[i%2].copy(alpha=(.55f-i*.12f)*glow),phase*20f+i*120f,70f,false,Offset(c.x-rr,c.y-rr),Size(rr*2,rr*2),style=Stroke(t.ringStroke))
    }
    drawCircle(t.primary.copy(alpha=.45f*glow),2f,c)
}

private fun DrawScope.drawAnime(c: Offset, r: Float, t: com.chargeflow.theme.FlagshipTheme, ps: List<ParticleSeed>, phase: Float, active: Boolean) {
    ps.forEachIndexed { i,p->
        val ang=p.phase*360f+phase*55f
        val rr=r*(.15f+p.y*.7f)
        val pt=polar(c,rr,ang)
        val col=t.particleColors[p.colorIndex]
        drawCircle(col.copy(alpha=(.3f+.65f*p.size)*(if(active)1f:.4f)),1.5f+p.size*3f,pt)
        if(i%9==0){
            drawLine(col.copy(alpha=.55f),Offset(pt.x-5f,pt.y),Offset(pt.x+5f,pt.y),1f)
            drawLine(col.copy(alpha=.55f),Offset(pt.x,pt.y-5f),Offset(pt.x,pt.y+5f),1f)
        }
    }
    drawArc(t.primary.copy(alpha=.45f),phase*80f,140f,false,Offset(c.x-r*.58f,c.y-r*.58f),Size(r*1.16f,r*1.16f),style=Stroke(1.5f))
}

private fun DrawScope.drawAbstract(c: Offset, r: Float, p: Color, s: Color, a: Color, phase: Float, glow: Float) {
    val colors=listOf(p,s,a)
    repeat(3){band->
        val path=Path()
        for(i in 0..48){
            val x=i/48f*size.width
            val y=c.y+(band-1)*r*.28f+sin(i/48f*6.28f+phase*6.28f+band*1.7f)*r*.12f
            if(i==0)path.moveTo(x,y)else path.lineTo(x,y)
        }
        drawPath(path,colors[band].copy(alpha=.65f*glow),style=Stroke(4f-band))
    }
}

private fun DrawScope.drawMidnightGarden(c: Offset, r: Float, t: com.chargeflow.theme.FlagshipTheme, ps: List<ParticleSeed>, phase: Float, active: Boolean) {
    repeat(5){i->
        val ang=i*72f+phase*18f
        val end=polar(c,r*.78f,ang)
        val path=Path().apply{moveTo(c.x,c.y);quadraticTo((c.x+end.x)/2f,(c.y+end.y)/2f+r*.18f,end.x,end.y)}
        drawPath(path,t.ringColors[i%2].copy(alpha=.28f),style=Stroke(2f))
    }
    ps.forEach{p->
        val pt=polar(c,r*(.18f+p.y*.72f),p.phase*360f+phase*24f)
        drawCircle(t.particleColors[p.colorIndex].copy(alpha=.55f*(if(active)1f:.45f)),1.5f+p.size*2.5f,pt)
    }
}

private fun DrawScope.drawCelestial(c: Offset, r: Float, t: com.chargeflow.theme.FlagshipTheme, ps: List<ParticleSeed>, phase: Float, active: Boolean) {
    ps.forEachIndexed{i,p->
        val pt=polar(c,r*(.25f+p.y*.65f),p.phase*360f+phase*12f)
        val alpha=.25f+.7f*p.size
        drawCircle(t.particleColors[p.colorIndex].copy(alpha=alpha*(if(active)1f:.5f)),1f+p.size*2.8f,pt)
        if(i%8==0){
            drawLine(Color.White.copy(alpha=.65f),Offset(pt.x-5,pt.y),Offset(pt.x+5,pt.y),1f)
            drawLine(Color.White.copy(alpha=.65f),Offset(pt.x,pt.y-5),Offset(pt.x,pt.y+5),1f)
        }
    }
    drawArc(t.primary.copy(alpha=.42f),phase*45f,110f,false,Offset(c.x-r*.65f,c.y-r*.65f),Size(r*1.3f,r*1.3f),style=Stroke(1.5f))
}

private fun DrawScope.drawEnchantedForest(c: Offset, r: Float, t: com.chargeflow.theme.FlagshipTheme, ps: List<ParticleSeed>, phase: Float, active: Boolean) {
    repeat(7){i->
        val x=c.x-r*.72f+i*r*.24f
        val top=c.y+r*.30f-sin(i*.9f+phase*2f)*r*.12f
        drawLine(t.secondary.copy(alpha=.28f),Offset(x,c.y+r*.62f),Offset(x,top),2f)
        drawCircle(t.accent.copy(alpha=.3f),4f,Offset(x,top))
    }
    ps.forEach{p->
        val pt=polar(c,r*(.2f+p.y*.65f),p.phase*360f+phase*30f)
        drawCircle(t.particleColors[p.colorIndex].copy(alpha=.5f*(if(active)1f:.45f)),1.5f+p.size*2f,pt)
    }
}

private fun DrawScope.drawOceanAbyss(c: Offset, r: Float, t: com.chargeflow.theme.FlagshipTheme, ps: List<ParticleSeed>, phase: Float, glow: Float) {
    repeat(5){i->
        val rr=r*(.22f+((phase+i*.2f)%1f)*.72f)
        drawOval(t.ringColors[i%2].copy(alpha=.28f*glow),Offset(c.x-rr,c.y-rr*.55f),Size(rr*2,rr*1.1f),style=Stroke(2f))
    }
    ps.forEach{p->
        val x=c.x+(p.x-.5f)*r*1.5f
        val y=((p.y+phase*(.04f+p.size*.04f))%1f)*size.height
        drawCircle(t.particleColors[p.colorIndex].copy(alpha=.35f),1f+p.size*2f,Offset(x,y))
    }
}

private fun polar(center: Offset, radius: Float, degrees: Float): Offset {
    val a=Math.toRadians(degrees.toDouble())
    return Offset(center.x+cos(a).toFloat()*radius,center.y+sin(a).toFloat()*radius)
}

private data class ParticleSeed(
    val x: Float,
    val y: Float,
    val size: Float,
    val phase: Float,
    val colorIndex: Int
)
