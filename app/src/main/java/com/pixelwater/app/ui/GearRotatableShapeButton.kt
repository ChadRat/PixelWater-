package com.pixelwater.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A shaped button component that rotates its outer background shape 180° (half a full turn)
 * with ultra-smooth gear rotation and deceleration coming to a pristine stop.
 * When unselected, it rotates back to 0° counter-clockwise in the exact same smooth manner.
 * Haptic feedback triggers strictly while the rotation animation is actively playing.
 * Touch indication ("ghost box") is removed, and inner icon content remains completely stationary at 0°.
 */
@Composable
fun GearRotatableShapeButton(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = WavyShape(),
    backgroundBrush: Brush? = null,
    backgroundColor: Color? = null,
    borderWidth: Dp = 0.dp,
    borderColor: Color = Color.Transparent,
    content: @Composable BoxScope.() -> Unit
) {
    val view = LocalView.current

    val animAngle = remember { Animatable(if (isSelected && shape != CircleShape) 90f else 0f) }
    var isInitialComposition by remember { mutableStateOf(true) }

    LaunchedEffect(isSelected, shape) {
        if (shape == CircleShape) {
            animAngle.snapTo(0f)
            return@LaunchedEffect
        }

        val target = if (isSelected) 90f else 0f

        if (isInitialComposition) {
            isInitialComposition = false
            animAngle.snapTo(target)
            return@LaunchedEffect
        }

        if (kotlin.math.abs(animAngle.value - target) > 0.5f) {
            val stepSize = 15f
            var lastStep = (animAngle.value / stepSize).toInt()
            animAngle.animateTo(
                targetValue = target,
                animationSpec = tween(
                    durationMillis = 2000,
                    easing = CubicBezierEasing(0.22f, 1.25f, 0.36f, 1.0f) // Organic gear motion: slower mechanical sweep with a subtle gear tooth overshoot & recoil lock
                )
            ) {
                if (animAngle.isRunning) {
                    val currentStep = (value / stepSize).toInt()
                    if (currentStep != lastStep) {
                        lastStep = currentStep
                        try {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                        } catch (_: Exception) {}
                    }
                }
            }
            // Crisp final detent lock haptic right as the gear settles into its final position
            try {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            } catch (_: Exception) {}
        } else {
            animAngle.snapTo(target)
        }
    }

    val angle = animAngle.value
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null // Removes ghost box / square touch highlight
            ) {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        // Rotated background shape container
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    rotationZ = angle
                }
                .then(
                    if (borderWidth > 0.dp) Modifier.border(borderWidth, borderColor, shape) else Modifier
                )
                .clip(shape)
                .then(
                    when {
                        backgroundBrush != null -> Modifier.background(backgroundBrush)
                        backgroundColor != null -> Modifier.background(backgroundColor)
                        else -> Modifier
                    }
                )
        )

        // Inner content/icon kept strictly stationary at 0° rotation
        content()
    }
}
