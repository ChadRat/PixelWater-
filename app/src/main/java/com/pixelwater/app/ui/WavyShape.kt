package com.pixelwater.app.ui

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import kotlin.math.cos
import kotlin.math.sin

class WavyShape(private val periods: Int = 10, private val amplitude: Float = 0.08f) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val baseRadius = (size.width / 2f) * (1f - amplitude)
        
        val steps = 150
        for (i in 0..steps) {
            val theta = (i * 2.0 * Math.PI / steps).toFloat()
            val r = baseRadius + baseRadius * amplitude * cos(periods * theta.toDouble()).toFloat()
            val x = centerX + r * cos(theta.toDouble()).toFloat()
            val y = centerY + r * sin(theta.toDouble()).toFloat()
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        return Outline.Generic(path)
    }
}
