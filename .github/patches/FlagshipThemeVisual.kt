package com.chargeanim.pro.ui.theme

import androidx.compose.animation.core.*
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
import import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun FlagshipThemeVisual(themeId: ThemeId, modifier: Modifier = Modifier, active: Boolean = true, animated: Boolean = true, content: @Composable () -> Unit = {}) {
    val t=FlagshipThemes.get(themeId)
    val seeds=remember(themeId){val r=Random(themeId.ordinal*7919+17);List(100){S(r.nextFloat(),r.nextFloat(),r.nextFloat(),r.nextFloat())}}
    val phase:Float
    if(animated){val tr=rememberInfiniteTransition(label="theme");val p by tr.animateFloat(0f,1f,infiniteRepeatable(tween(7000,easing=LinearEasing)),label="phase");phase=p}else phase=.5f
    Box(modifier){
        Canvas(Modifier.fillMaxSize()){
            val c=Offset(size.width/2f,size.height/2f);val r=size.minDimension*.46f
            drawRect(t.background)
            when(themeId){
                ThemeId.FUTURISTIC->hud(c,r,t,phase)
                ThemeId.SPACE->stars(c,r,t,seeds,phase,active)
                ThemeId.ELECTRIC->electric(c,r,t,phase)
                ThemeId.FIRE->flames(c,r,t,seeds,phase,active)
                ThemeId.WATER->waves(c,r,t,phase)
                ThemeId.ICE->crystal(c,r,t)
                ThemeId.NEON->synth(c,r,t,phase)
                ThemeId.MATRIX->rain(c,r,t,seeds,phase,active)
                ThemeId.NATURE->vines(c,r,t,phase)
                ThemeId.MINIMAL->precision(c,r,t,phase)
                ThemeId.ANIME->petals(c,r,t,phase)
                ThemeId.ABSTRACT->ribbons(c,r,t,phase)
                ThemeId.MIDNIGHT_GARDEN->garden(c,r,t,seeds,phase)
                ThemeId.CELESTIAL_SPARKLE->constellation(c,r,t,seeds)
                ThemeId.ENCHANTED_FOREST->forest(c,r,t,seeds)
                ThemeId.OCEAN_ABYSS->abyss(c,r,t,seeds,phase)
            }
        }
        Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){content()}
    }
}

private fun DrawScope.hud(c:Offset,r:Float,t:com.chargeflow.theme.FlagshipTheme,phase:Float){
    val w=r*1.5f;val h=r*1.1f
    drawRect(t.primary.copy(alpha=.08f),Offset(c.x-w,c.y-h),Size(w*2,h*2))
    drawRect(t.primary.copy(alpha=.6f),Offset(c.x-w,c.y-h),Size(w*2,h*2),style=Stroke(1.5f))
    repeat(8){i->val y=c.y-h+i*h*2/7;drawLine(t.secondary.copy(alpha=.08f),Offset(c.x-w,y),Offset(c.x+w,y),1f)}
    val y=c.y-h+phase*h*2;drawLine(t.primary.copy(alpha=.75f),Offset(c.x-w,y),Offset(c.x+w,y),2f)
    listOf(Offset(c.x-w,c.y-h),Offset(c.x+w,c.y-h),Offset(c.x-w,c.y+h),Offset(c.x+w,c.y+h)).forEach{p->drawLine(t.primary,p,Offset(p.x+if(p.x<c.x)30f else -30f,p.y),3f);drawLine(t.primary,p,Offset(p.x,p.y+if(p.y<c.y)30f else -30f),3f)}
    repeat(12){i->drawRect(t.accent.copy(alpha=.55f),Offset(c.x-w*.82f+i*w*.15f,c.y+h*.72f),Size(10f,3f))}
}
private fun DrawScope.stars(c:Offset,r:Float,t:com.chargeflow.theme.FlagshipTheme,s:List<S>,phase:Float,active:Boolean){
    val neb=Path();for(i in 0..50){val u=i/50f;val x=c.x-r*1.5f+u*r*3f;val y=c.y+r*.25f+sin(u*7f+phase*6.28f)*r*.3f;if(i==0)neb.moveTo(x,y)else neb.lineTo(x,y)};drawPath(neb,t.secondary.copy(alpha=.11f),style=Stroke(r*.35f))
    s.forEachIndexed{i,q->{val x=(q.x*size.width+phase*(10+q.z*25))%size.width;val y=q.y*size.height+sin((phase*4+q.w*7).toDouble()).toFloat()*10;val col=t.particleColors[i%t.particleColors.size];drawCircle(col.copy(alpha=.35f+.5f*q.z),.8f+q.z*2.4f,Offset(x,y));if(i%12==0){drawLine(Color.White.copy(alpha=.7f),Offset(x-5,y),Offset(x+5,y),1f);drawLine(Color.White.copy(alpha=.7f),Offset(x,y-5),Offset(x,y+5),1f)}}}
}
private fun DrawScope.electric(c:Offset,r:Float,t:com.chargeflow.theme.FlagshipTheme,phase:Float){
    repeat(4){b->val p=Path();var x=c.x+(b-1.5f)*r*.22f;var y=c.y-r*.9f;p.moveTo(x,y);repeat(8){i->x+=sin((i*2.1+b+phase*18).toDouble()).toFloat()*r*.15f;y+=r*.23f;p.lineTo(x,y)};drawPath(p,t.primary.copy(alpha=.18f),style=Stroke(8f));drawPath(p,t.accent.copy(alpha=.9f),style=Stroke(1.8f))}
}
private fun DrawScope.flames(c:Offset,r:Float,t:com.chargeflow.theme.FlagshipTheme,s:List<S>,phase:Float,active:Boolean){
    val p=Path().apply{moveTo(c.x-r*.6f,c.y+r*.75f);cubicTo(c.x-r*.75f,c.y,c.x-r*.15f,c.y,c.x-r*.25f,c.y-r*.5f);cubicTo(c.x,c.y-r*.25f,c.x+.05f*r,c.y-r*.8f,c.x+.22f*r,c.y-r*.9f);cubicTo(c.x+.2f*r,c.y-r*.25f,c.x+.65f*r,c.y,c.x+.58f*r,c.y+r*.75f);close()};drawPath(p,t.secondary.copy(alpha=.18f));drawPath(p,t.primary.copy(alpha=.7f),style=Stroke(2.5f))
    s.take(55).forEachIndexed{i,q->{val z=(phase*(.5f+q.z*1.4f)+q.w)%1f;val x=c.x+(q.x-.5f)*r*1.3f+sin((z*9).toDouble()).toFloat()*r*.08f;val y=c.y+r*.7f-z*r*1.6f;val col=t.particleColors[i%t.particleColors.size];drawLine(col.copy(alpha=.65f),Offset(x,y+12),Offset(x,y-5-q.z*8),1.7f)}}
}
private fun DrawScope.waves(c:Offset,r:Float,t:com.chargeflow.theme.FlagshipTheme,phase:Float){
    repeat(7){i->val y=c.y-r*.65f+i*r*.22f;val p=Path();for(xi in 0..50){val x=c.x-r*1.2f+xi/50f*r*2.4f;val yy=y+sin((xi*.42+phase*6.28+i).toDouble()).toFloat()*r*.07f;if(xi==0)p.moveTo(x,yy)else p.lineTo(x,yy)};drawPath(p,t.particleColors[i%t.particleColors.size].copy(alpha=.3f),style=Stroke(2f))}
    repeat(8){i->val x=c.x-r*.85f+i*r*.24f;drawLine(t.secondary.copy(alpha=.12f),Offset(x,c.y-r*.8f),Offset(x+sin((phase*5+i).toDouble()).toFloat()*14,c.y+r*.7f),2f)}
}
private fun DrawScope.crystal(c:Offset,r:Float,t:com.chargeflow.theme.FlagshipTheme){
    val a=listOf(Offset(c.x,c.y-r*.9f),Offset(c.x+r*.5f,c.y-r*.2f),Offset(c.x+r*.32f,c.y+r*.7f),Offset(c.x-r*.42f,c.y+r*.72f),Offset(c.x-r*.55f,c.y-r*.2f));val p=Path().apply{moveTo(a[0].x,a[0].y);a.drop(1).forEach{lineTo(it.x,it.y)};close()};drawPath(p,t.secondary.copy(alpha=.13f));drawPath(p,t.primary.copy(alpha=.8f),style=Stroke(2.2f));a.drop(1).forEach{drawLine(t.accent.copy(alpha=.5f),a[0],it,1f)};drawLine(Color.White.copy(alpha=.5f),Offset(c.x-r*.3f,c.y+r*.3f),Offset(c.x+r*.3f,c.y-r*.3f),1.5f)
}
private fun DrawScope.synth(c:Offset,r:Float,t:com.chargeflow.theme.FlagshipTheme,phase:Float){
    val p=Path();repeat(6){i->{val a=(-Math.PI.toFloat()/2f)+i*Math.PI.toFloat()/3f+phase*.25f;val q=Offset(c.x+cos(a.toDouble()).toFloat()*r*.72f,c.y+sin(a.toDouble()).toFloat()*r*.72f);if(i==0)p.moveTo(q.x,q.y)else p.lineTo(q.x,q.y)}};p.close();drawPath(p,t.primary.copy(alpha=.2f),style=Stroke(9f));drawPath(p,t.primary.copy(alpha=.85f),style=Stroke(2f));repeat(5){i->val y=c.y-r*.55f+i*r*.27f;val x=c.x-r+((phase+i*.2)%1)*r*2;drawLine(t.particleColors[i%t.particleColors.size],Offset(x,y),Offset(x+r*.3f,y),3f)}
}
private fun DrawScope.rain(c:Offset,r:Float,t:com.chargeflow.theme.FlagshipTheme,s:List<S>,phase:Float,active:Boolean){
    s.take(75).forEachIndexed{i,q->{val x=c.x-r*.95f+q.x*r*1.9f;val head=(q.y+phase*(.2f+q.z*.7f))%1f;repeat(7){j->val y=c.y-r*.9f+((head+j*.045f)%1f)*r*1.8f;drawRect(t.particleColors[i%t.particleColors.size].copy(alpha=(.12f+(6-j)*.1f)),Offset(x,y),Size(1.5f+q.z*2f,5f))}}}
}
private fun DrawScope.vines(c:Offset,r:Float,t:com.chargeflow.theme.FlagshipTheme,phase:Float){
    repeat(4){v->val p=Path();val x0=c.x-r*.8f+v*r*.53f;p.moveTo(x0,c.y+r*.8f);for(i in 1..8)p.lineTo(x0+sin((i*.8+v+phase).toDouble()).toFloat()*r*.15f,c.y+r*.8f-i*r*.19f);drawPath(p,t.secondary.copy(alpha=.6f),style=Stroke(2f));repeat(3){j->val y=c.y+r*.48f-j*r*.34f;val x=x0+sin((j+v+phase).toDouble()).toFloat()*r*.1f;drawLine(t.particleColors[(v+j)%t.particleColors.size].copy(alpha=.6f),Offset(x,y),Offset(x+r*.18f,y-r*.08f),4f)}}
}
private fun DrawScope.precision(c:Offset,r:Float,t:com.chargeflow.theme.FlagshipTheme,phase:Float){
    drawLine(t.primary.copy(alpha=.7f),Offset(c.x-r*.75f,c.y),Offset(c.x+r*.75f,c.y),1f);repeat(8){i->val x=c.x-r*.65f+i*r*.19f;val h=r*(.06f+((i+1)%3)*.07f);drawLine(t.secondary.copy(alpha=.45f),Offset(x,c.y-h),Offset(x,c.y+h),1f)};val x=c.x-r*.72f+((phase*1.4)%1)*r*1.44f;drawLine(t.accent,Offset(x,c.y-r*.3f),Offset(x,c.y+r*.3f),2f)
}
private fun DrawScope.petals(c:Offset,r:Float,t:com.chargeflow.theme.FlagshipTheme,phase:Float){
    repeat(9){i->val a=i*Math.PI.toFloat()*2f/9f+phase*.8f;val x=c.x+cos(a).toFloat()*r*.42f;val y=c.y+sin(a).toFloat()*r*.42f;drawLine(t.particleColors[i%t.particleColors.size],Offset(x,y),Offset(x+cos(a).toFloat()*r*.22f,y+sin(a).toFloat()*r*.22f),5f)};drawLine(t.secondary.copy(alpha=.6f),Offset(c.x-r*.9f,c.y+r*.7f),Offset(c.x+r*.9f,c.y-r*.55f),2f)
}
private fun DrawScope.ribbons(c:Offset,r:Float,t:com.chargeflow.theme.FlagshipTheme,phase:Float){
    listOf(t.primary,t.secondary,t.accent).forEachIndexed{b,col->val p=Path();for(i in 0..70){val u=i/70f;val x=c.x-r*1.35f+u*r*2.7f;val y=c.y+(b-1)*r*.34f+sin((u*Math.PI.toFloat()*2.1f+phase*6.28f+b).toDouble()).toFloat()*r*.18f;if(i==0)p.moveTo(x,y)else p.lineTo(x,y)};drawPath(p,col.copy(alpha=.7f),style=Stroke(5f-b))}
}
private fun DrawScope.garden(c:Offset,r:Float,t:com.chargeflow.theme.FlagshipTheme,s:List<S>,phase:Float){
    repeat(5){i->val x=c.x-r*.8f+i*r*.4f;drawLine(t.primary.copy(alpha=.55f),Offset(x,c.y+r*.8f),Offset(x+r*.1f,c.y-r*.35f),2f);repeat(5){j->val a=j*Math.PI.toFloat()*2f/5f;drawLine(t.secondary.copy(alpha=.55f),Offset(x+r*.1f,c.y-r*.38f),Offset(x+r*.1f+cos(a).toFloat()*14,c.y-r*.38f+sin(a).toFloat()*14),3f)}};s.take(25).forEach{q->drawCircle(t.accent.copy(alpha=.4f),1.5f+q.z*2,Offset(c.x+(q.x-.5f)*r*1.8f,c.y+(q.y-.5f)*r*1.5f))}
}
private fun DrawScope.constellation(c:Offset,r:Float,t:com.chargeflow.theme.FlagshipTheme,s:List<S>){
    val pts=s.take(18).map{Offset(c.x+(it.x-.5f)*r*1.8f,c.y+(it.y-.5f)*r*1.5f)};for(i in 0 until pts.size-1 step 2)drawLine(t.secondary.copy(alpha=.22f),pts[i],pts[i+1],1f);pts.forEachIndexed{i,p->{val col=t.particleColors[i%t.particleColors.size];drawLine(col,Offset(p.x-6,p.y),Offset(p.x+6,p.y),1f);drawLine(col,Offset(p.x,p.y-6),Offset(p.x,p.y+6),1f)}}
}
private fun DrawScope.forest(c:Offset,r:Float,t:com.chargeflow.theme.FlagshipTheme,s:List<S>){
    for(layer in 0..2){val p=Path();val base=c.y+r*(.78f-layer*.16f);p.moveTo(c.x-r,base);for(i in 0..12){val x=c.x-r+i/12f*r*2;val h=r*(.16f+((i*5+layer)%4)*.09f);p.lineTo(x,base-h);p.lineTo(x+r*.08f,base)};p.close();drawPath(p,t.particleColors[layer].copy(alpha=.16f+.07f*layer))}
    s.take(30).forEach{q->val x=c.x+(q.x-.5f)*r*1.8f;val y=c.y+(q.y-.55f)*r*1.3f;drawLine(t.accent.copy(alpha=.7f),Offset(x-4,y),Offset(x+4,y),1.5f);drawLine(t.accent.copy(alpha=.7f),Offset(x,y-4),Offset(x,y+4),1.5f)}
}
private fun DrawScope.abyss(c:Offset,r:Float,t:com.chargeflow.theme.FlagshipTheme,s:List<S>,phase:Float){
    repeat(8){i->val x=c.x-r*.9f+i*r*.26f;drawLine(t.secondary.copy(alpha=.1f),Offset(x,c.y-r*.9f),Offset(x+r*.12f,c.y+r*.65f),12f)}
    val floor=Path().apply{moveTo(c.x-r,c.y+r*.58f);cubicTo(c.x-r*.75f,c.y+r*.48f,c.x-r*.55f,c.y+r*.36f,c.x+.1f*r,c.y+r*.58f);cubicTo(c.x+r*.45f,c.y+r*.36f,c.x+r*.75f,c.y+r*.48f,c.x+r,c.y+r*.58f);lineTo(c.x+r,c.y+r);lineTo(c.x-r,c.y+r);close()};drawPath(floor,t.primary.copy(alpha=.18f))
    s.take(25).forEach{q->val y=c.y+r*.65f-((phase*(.12f+q.z*.3f)+q.y)%1)*r*1.4f;val x=c.x+(q.x-.5f)*r*1.5f;drawOval(t.accent.copy(alpha=.45f),Offset(x-4,y-2),Size(8f,4f),style=Stroke(1.2f))}
}
private data class S(val x:Float,val y:Float,val z:Float,val w:Float)
