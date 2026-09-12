package com.pixelwater.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb

fun Modifier.drawPillDropShadow(
    shape: Shape,
    isDark: Boolean,
    elevationDp: Dp = 14.dp,
    offsetYDp: Dp = 6.dp,
    shadowColor: Color? = null
): Modifier = this
    .drawBehind {
        val defaultShadowColor = if (isDark) {
            Color.Black.copy(alpha = 0.75f)
        } else {
            Color.Black.copy(alpha = 0.28f)
        }
        val effectiveColor = shadowColor ?: defaultShadowColor
        val shadowColorArgb = effectiveColor.toArgb()
        val blurPx = elevationDp.toPx()
        val offsetYPx = offsetYDp.toPx()

        if (blurPx <= 0f && offsetYPx <= 0f) return@drawBehind

        val outline = shape.createOutline(size, layoutDirection, this)
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.TRANSPARENT
                isAntiAlias = true
                setShadowLayer(
                    blurPx,
                    0f,
                    offsetYPx,
                    shadowColorArgb
                )
            }
            val nativeCanvas = canvas.nativeCanvas
            nativeCanvas.save()
            when (outline) {
                is Outline.Rectangle -> {
                    val rect = outline.rect
                    nativeCanvas.drawRect(
                        rect.left,
                        rect.top,
                        rect.right,
                        rect.bottom,
                        paint
                    )
                }
                is Outline.Rounded -> {
                    val rrect = outline.roundRect
                    val androidPath = android.graphics.Path().apply {
                        addRoundRect(
                            android.graphics.RectF(rrect.left, rrect.top, rrect.right, rrect.bottom),
                            floatArrayOf(
                                rrect.topLeftCornerRadius.x, rrect.topLeftCornerRadius.y,
                                rrect.topRightCornerRadius.x, rrect.topRightCornerRadius.y,
                                rrect.bottomRightCornerRadius.x, rrect.bottomRightCornerRadius.y,
                                rrect.bottomLeftCornerRadius.x, rrect.bottomLeftCornerRadius.y
                            ),
                            android.graphics.Path.Direction.CW
                        )
                    }
                    nativeCanvas.drawPath(androidPath, paint)
                }
                is Outline.Generic -> {
                    nativeCanvas.drawPath(outline.path.asAndroidPath(), paint)
                }
            }
            nativeCanvas.restore()
        }
    }
    .shadow(
        elevation = (elevationDp / 2.5f).coerceAtLeast(1.dp),
        shape = shape,
        clip = false,
        ambientColor = if (isDark) Color.Black.copy(alpha = 0.50f) else Color.Black.copy(alpha = 0.16f),
        spotColor = if (isDark) Color.Black.copy(alpha = 0.65f) else Color.Black.copy(alpha = 0.22f)
    )

fun parseMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            if (text.startsWith("**", i)) {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                    continue
                }
            } else if (text.startsWith("*", i)) {
                val end = text.indexOf("*", i + 1)
                if (end != -1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                    continue
                }
            }
            append(text[i])
            i++
        }
    }
}

fun cleanMetaCommentsAndBulletLabels(text: String): String {
    return text.replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "").trim()
}

@Composable
fun CircularSquigglyProgressIndicator(
    modifier: Modifier = Modifier.size(28.dp),
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 3.dp,
    numLobes: Int = 8,
    squigglyAmplitude: Float = 0.20f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "squiggly_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing)
        ),
        label = "rotation"
    )

    Canvas(modifier = modifier) {
        val strokePx = strokeWidth.toPx()
        val radius = (minOf(size.width, size.height) - strokePx * 2) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        val path = Path()
        val points = 120
        for (i in 0..points) {
            val angleDeg = (i.toFloat() / points.toFloat()) * 360f + rotation
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val wave = sin(angleRad * numLobes) * (radius * squigglyAmplitude)
            val r = radius + wave.toFloat()
            val x = center.x + r * cos(angleRad).toFloat()
            val y = center.y + r * sin(angleRad).toFloat()

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokePx)
        )
    }
}
