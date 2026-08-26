package com.pixelwater.app.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color

object CircularProgressGenerator {
    fun generateCircularProgressBitmap(
        context: Context,
        totalIntake: Int,
        dailyGoal: Int,
        fillRatio: Float,
        appLanguage: String,
        isDark: Boolean = true,
        diameterDp: Float = 160f,
        displayMode: String = "left"
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        // Define dynamic scale based on standard diameter of 160dp
        val textScale = diameterDp / 160f
        
        val sizePx = (diameterDp * density).toInt()

        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Make stroke width perfectly proportional!
        // At 160dp, we use 18dp. At other sizes, we scale it proportionally.
        val strokeWidthDp = (diameterDp * 0.11f).coerceIn(6f, 18f)
        val strokeWidthPx = strokeWidthDp * density
        
        val diameter = sizePx.toFloat() - strokeWidthPx
        val radius = diameter / 2f
        val centerOffset = android.graphics.PointF(sizePx / 2f, sizePx / 2f)

        val centerBgColor = if (isDark) android.graphics.Color.parseColor("#18181A") else android.graphics.Color.WHITE
        val outerRimColor = if (isDark) android.graphics.Color.parseColor("#2A2D35") else android.graphics.Color.parseColor("#E2E8F0")
        val progressArcColor = if (isDark) android.graphics.Color.parseColor("#00BFA5") else android.graphics.Color.parseColor("#0288D1")

        // 1. Draw Inner Concentric Core
        val paintCenter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = centerBgColor
            style = Paint.Style.FILL
        }
        val centerCircleRadius = radius - strokeWidthPx / 2f
        canvas.drawCircle(centerOffset.x, centerOffset.y, centerCircleRadius, paintCenter)

        // 2. Draw Outer circular track rim
        val paintRim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = outerRimColor
            style = Paint.Style.STROKE
            strokeWidth = strokeWidthPx
        }
        canvas.drawCircle(centerOffset.x, centerOffset.y, radius, paintRim)

        // 3. Draw Active progress arc
        val paintArc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = progressArcColor
            style = Paint.Style.STROKE
            strokeWidth = strokeWidthPx
            strokeCap = Paint.Cap.ROUND
        }
        val rectF = RectF(
            centerOffset.x - radius,
            centerOffset.y - radius,
            centerOffset.x + radius,
            centerOffset.y + radius
        )
        val sweepAngle = (fillRatio * 360f).coerceIn(0f, 360f)
        canvas.drawArc(rectF, -90f, sweepAngle, false, paintArc)

        // 4. Texts inside
        // Text indent scales proportionally with diameter, with a safety buffer so it doesn't get pushed too far out
        val textsIndent = (diameterDp * 0.08f).coerceAtLeast(4f) * density
        val textRadius = (centerCircleRadius - textsIndent).coerceAtLeast(10f * density)
        val rectTop = RectF(
            centerOffset.x - textRadius,
            centerOffset.y - textRadius,
            centerOffset.x + textRadius,
            centerOffset.y + textRadius
        )

        // Get the display value: shows remaining for "left", or totalIntake for "added"
        val displayValue = if (displayMode == "left") {
            (dailyGoal - totalIntake).coerceAtLeast(0)
        } else {
            totalIntake
        }

        val formattedAmount = if (displayValue >= 1000) {
            val liters = displayValue / 1000
            val rem = displayValue % 1000
            String.format(java.util.Locale.US, "%d.%03d", liters, rem)
        } else {
            displayValue.toString()
        }
        val textLength = formattedAmount.length
        val dynamicTextSize = when {
            textLength > 5 -> 24f
            textLength == 5 -> 28f
            else -> 36f
        }

        val minimumTextSize = if (diameterDp < 70f) 8.5f else 10f
        val textPaintMiddle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDark) android.graphics.Color.WHITE else android.graphics.Color.BLACK
            textSize = (dynamicTextSize * textScale).coerceAtLeast(minimumTextSize) * density
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        
        // Center text metric label (volume count overlay underneath)
        val rectBounds = android.graphics.Rect()
        textPaintMiddle.getTextBounds(formattedAmount, 0, formattedAmount.length, rectBounds)
        val textMiddleY = centerOffset.y + (rectBounds.height() / 2f)
        canvas.drawText(formattedAmount, centerOffset.x, textMiddleY, textPaintMiddle)
        
        // Progress percentage / Daily Goal Curved at bottom (Upright, not upside down) ONLY on larger widgets
        if (diameterDp >= 90f) {
            // Make the bottom text size proportional but legible (coerce min to 7.5dp)
            val bottomTextSize = (11f * textScale).coerceAtLeast(7.5f)
            val paintBottom = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = progressArcColor
                textSize = bottomTextSize * density
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                letterSpacing = 0.02f
            }
            
            val formattedGoal = if (dailyGoal >= 1000) {
                val liters = dailyGoal / 1000
                val rem = dailyGoal % 1000
                String.format(java.util.Locale.US, "%d.%03d", liters, rem)
            } else {
                dailyGoal.toString()
            }
            val bottomLabel = if (appLanguage == "el") {
                "από $formattedGoal ml"
            } else {
                if (displayMode == "left") "out of $formattedGoal ml" else "of $formattedGoal ml"
            }
            val pathBottom = Path().apply {
                addArc(rectTop, 180f, -180f)
            }
            canvas.drawTextOnPath(bottomLabel, pathBottom, 0f, 0f, paintBottom)
        }

        return bitmap
    }
}
