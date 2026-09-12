package com.pixelwater.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider as M3HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsDivider(
    viewModel: WaterViewModel,
    modifier: Modifier = Modifier,
    color: Color = Color.Transparent,
    thickness: Dp = 1.dp,
    paddingVertical: Dp = 12.dp
) {
    val style by viewModel.settingsDividerStyle.collectAsStateWithLifecycle()
    PixelWaterDivider(
        modifier = modifier,
        style = style,
        color = color,
        thickness = thickness,
        paddingVertical = paddingVertical
    )
}

@Composable
fun PixelWaterDivider(
    modifier: Modifier = Modifier,
    style: String = "STRAIGHT", // "STRAIGHT", "SQUIGGLY", "GAPS"
    color: Color = Color.Transparent,
    thickness: Dp = 1.dp,
    paddingVertical: Dp = 12.dp
) {
    val isTransparent = LocalTransparentComponentsEnabled.current
    val isFrosted = LocalFrostedGlassEnabled.current
    // The glass theme should not make squiggly lines disappear
    if ((isTransparent || isFrosted) && style != "SQUIGGLY") {
        val gapScale = LocalSettingsGapScale.current
        val gapHeight = (if (paddingVertical > 0.dp) paddingVertical else 8.dp) * gapScale
        Spacer(
            modifier = modifier
                .height(gapHeight)
                .layoutId("divider")
        )
        return
    }

    // Divider line contrast is permanently locked to highest contrast option (2.2f)
    val contrastMultiplier = 2.2f

    val finalColor = if (color == Color.Transparent) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f * contrastMultiplier)
    } else {
        color.copy(alpha = (color.alpha * contrastMultiplier).coerceIn(0f, 1f))
    }

    when (style) {
        "SQUIGGLY" -> {
            SquigglyDivider(
                modifier = modifier.padding(vertical = paddingVertical),
                color = finalColor.copy(alpha = (finalColor.alpha * 2.5f).coerceIn(0f, 1f)),
                thickness = 2.5.dp,
                waveLength = 16.dp,
                waveHeight = 5.dp
            )
        }
        "GAPS" -> {
            val gapScale = LocalSettingsGapScale.current
            val gapHeight = paddingVertical * gapScale
            Spacer(
                modifier = modifier
                    .height(gapHeight)
                    .layoutId("divider")
            )
        }
        else -> {
            M3HorizontalDivider(
                modifier = modifier.padding(vertical = paddingVertical),
                color = finalColor,
                thickness = thickness
            )
        }
    }
}

@Composable
fun HorizontalDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp,
    paddingVertical: Dp = 0.dp,
    color: Color = Color.Transparent
) {
    val style = LocalSettingsDividerStyle.current
    val isTransparent = LocalTransparentComponentsEnabled.current
    val isFrosted = LocalFrostedGlassEnabled.current
    // The glass theme should not make squiggly lines disappear
    if ((isTransparent || isFrosted) && style != "SQUIGGLY") {
        val gapScale = LocalSettingsGapScale.current
        val basePadding = if (paddingVertical > 0.dp) paddingVertical * 2 else 8.dp
        val gapHeight = basePadding * gapScale
        Spacer(
            modifier = modifier
                .height(gapHeight)
                .layoutId("divider")
        )
        return
    }
    // Divider line contrast is permanently locked to highest contrast option (2.2f)
    val contrastMultiplier = 2.2f

    val finalColor = if (color == Color.Transparent) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f * contrastMultiplier)
    } else {
        color.copy(alpha = (color.alpha * contrastMultiplier).coerceIn(0f, 1f))
    }

    when (style) {
        "SQUIGGLY" -> {
            SquigglyDivider(
                modifier = modifier.padding(vertical = paddingVertical),
                color = finalColor.copy(alpha = (finalColor.alpha * 2.5f).coerceIn(0f, 1f)),
                thickness = 2.5.dp,
                waveLength = 16.dp,
                waveHeight = 5.dp
            )
        }
        "GAPS" -> {
            val gapScale = LocalSettingsGapScale.current
            val basePadding = if (paddingVertical > 0.dp) paddingVertical * 2 else 12.dp
            val gapHeight = basePadding * gapScale
            Spacer(
                modifier = modifier
                    .height(gapHeight)
                    .layoutId("divider")
            )
        }
        else -> {
            M3HorizontalDivider(
                modifier = modifier.padding(vertical = paddingVertical),
                thickness = thickness,
                color = finalColor
            )
        }
    }
}

@Composable
fun SquigglyDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
    thickness: Dp = 2.5.dp,
    waveLength: Dp = 16.dp,
    waveHeight: Dp = 5.dp
) {
    val density = LocalDensity.current
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(waveHeight * 2)
    ) {
        val path = Path()
        val thicknessPx = with(density) { thickness.toPx() }
        val waveLengthPx = with(density) { waveLength.toPx() }
        val waveHeightPx = with(density) { waveHeight.toPx() }
        
        val width = size.width
        val midY = size.height / 2f
        
        path.moveTo(0f, midY)
        var x = 0f
        var isUp = true
        while (x < width) {
            val nextX = x + waveLengthPx / 2f
            val controlX = x + waveLengthPx / 4f
            val targetY = if (isUp) midY - waveHeightPx else midY + waveHeightPx
            path.quadraticTo(controlX, targetY, nextX, midY)
            x = nextX
            isUp = !isUp
        }
        
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = thicknessPx,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
