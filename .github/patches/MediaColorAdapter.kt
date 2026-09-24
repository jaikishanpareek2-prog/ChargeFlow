package com.chargeanim.pro.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import java.io.InputStream

object MediaColorAdapter {
    fun loadBitmap(context: Context, uri: Uri): Bitmap? =
        runCatching {
            context.contentResolver.openInputStream(uri).use { input: InputStream? ->
                BitmapFactory.decodeStream(input)
            }
        }.getOrNull()

    fun dominantColor(context: Context, uri: Uri, fallback: Int = android.graphics.Color.CYAN): Int {
        val bitmap = loadBitmap(context, uri) ?: return fallback
        return Palette.from(bitmap).generate().getDominantColor(fallback)
    }

    fun dominantComposeColor(context: Context, uri: Uri, fallback: Color = Color.Cyan): Color {
        val c = dominantColor(context, uri, fallback.toArgb())
        return Color(c)
    }
}

private fun Color.toArgb(): Int =
    android.graphics.Color.argb(
        (alpha * 255f).toInt().coerceIn(0, 255),
        (red * 255f).toInt().coerceIn(0, 255),
        (green * 255f).toInt().coerceIn(0, 255),
        (blue * 255f).toInt().coerceIn(0, 255)
    )
