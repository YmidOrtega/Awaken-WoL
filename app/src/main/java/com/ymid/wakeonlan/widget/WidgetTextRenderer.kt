package com.ymid.wakeonlan.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.TypedValue
import androidx.annotation.ColorRes
import androidx.annotation.FontRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import kotlin.math.ceil

/**
 * Draws widget text into a bitmap. Launchers inflate widgets with their own fonts and ignore the
 * app's font resources (and system font themes such as Samsung's replace every family), so text
 * rendered here is the only way to keep the console typefaces on the home screen.
 */
object WidgetTextRenderer {

    fun render(context: Context, text: String, @FontRes font: Int, sizeSp: Float, @ColorRes color: Int): Bitmap {
        val metrics = context.resources.displayMetrics
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = ResourcesCompat.getFont(context, font)
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sizeSp, metrics)
            this.color = ContextCompat.getColor(context, color)
        }
        val fontMetrics = paint.fontMetrics
        val width = ceil(paint.measureText(text)).toInt().coerceAtLeast(1)
        val height = ceil(fontMetrics.descent - fontMetrics.ascent).toInt().coerceAtLeast(1)

        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            density = metrics.densityDpi
            Canvas(this).drawText(text, 0f, -fontMetrics.ascent, paint)
        }
    }
}
