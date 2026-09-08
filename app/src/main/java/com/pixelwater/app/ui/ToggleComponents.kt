package com.pixelwater.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import com.pixelwater.app.ui.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun MultiWayToggle(
    selectedIndex: Int,
    options: List<String>,
    onModeSelect: (Int) -> Unit,
    triggerLightHaptic: () -> Unit,
    triggerSnapHaptic: () -> Unit,
    enabled: Boolean = true
) {
    val isTransparent = LocalTransparentComponentsEnabled.current
    val transLimit = LocalComponentsTransparency.current
    val tc = if (isTransparent) transLimit else 1f
    val alpha = if (enabled) 1.0f else 0.5f

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha * 0.35f * tc),
                shape = CircleShape
            )
            .padding(4.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.CenterStart
    ) {
        val totalWidth = maxWidth
        val segmentCount = options.size
        val segmentWidth = totalWidth / segmentCount

        // Animated sliding background capsule indicator (Google Pixel style)
        val indicatorOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = spring(
                stiffness = Spring.StiffnessMedium,
                dampingRatio = Spring.DampingRatioLowBouncy
            ),
            label = "multi_way_indicator_offset"
        )

        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = alpha * tc),
                    shape = CircleShape
                )
        )

        // Labels Row
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEachIndexed { index, text ->
                val isSelected = selectedIndex == index
                val contentColor = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha * tc)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(enabled = enabled) {
                            if (selectedIndex != index) {
                                triggerSnapHaptic()
                                onModeSelect(index)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = text,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Modern Google Pixel Material 3 Expressive Multi-Point Toggle for Title Faces.
 * Redesigned strictly as the multi-point toggle itself:
 * - Pixel M3 Expressive pill chassis with bouncy spring physics & stretch dynamics
 * - Elevated active indicator capsule with expressive ambient/spot shadow and tonal colors
 * - Dynamic text bounce & scale pop animations on selection
 * - Segment boundary divider notches
 * - Comprehensive haptics (boundary detents, continuous drag ticks, unlatch snap, and selection snap)
 */
@Composable
fun TitleFaceToggle(
    currentMode: Int,
    appLanguage: String,
    cornerRadius: Int,
    onModeSelect: (Int) -> Unit,
    triggerLightHaptic: () -> Unit,
    triggerSnapHaptic: () -> Unit,
    triggerUnlatchHaptic: () -> Unit = {},
    triggerTensionHaptic: () -> Unit = {}
) {
    val options = if (appLanguage == "el") {
        listOf(
            "(っ◕‿◕)っ",
            "(つ≧▽≦)つ",
            "૮(˶ᵔ ᵕ ᵔ˶)っ",
            "(⊃＾▽＾)⊃",
            "<(◠‿◠)ﾉ",
            "—",
            "✦"
        )
    } else {
        listOf(
            "(っ◕‿◕)っ",
            "(つ≧▽≦)つ",
            "૮(˶ᵔ ᵕ ᵔ˶)っ",
            "(⊃＾▽＾)⊃",
            "<(◠‿◠)ﾉ",
            "—",
            "✦"
        )
    }

    val isTransparent = LocalTransparentComponentsEnabled.current
    val transLimit = LocalComponentsTransparency.current
    val tc = if (isTransparent) transLimit else 1f

    val segmentCount = options.size
    val safeIndex = currentMode.coerceIn(0, segmentCount - 1)

    val coroutineScope = rememberCoroutineScope()
    val pressScale = remember { Animatable(1f) }

    val currentModeState by rememberUpdatedState(currentMode)
    val onModeSelectState by rememberUpdatedState(onModeSelect)
    val triggerLightHapticState by rememberUpdatedState(triggerLightHaptic)
    val triggerSnapHapticState by rememberUpdatedState(triggerSnapHaptic)
    val triggerUnlatchHapticState by rememberUpdatedState(triggerUnlatchHaptic)
    val triggerTensionHapticState by rememberUpdatedState(triggerTensionHaptic)

    val chassisShape = RoundedCornerShape(maxOf(18, cornerRadius).dp)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .scale(pressScale.value)
            .shadow(
                elevation = 1.dp,
                shape = chassisShape,
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f * tc),
                shape = chassisShape
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f),
                shape = chassisShape
            )
            .padding(4.dp)
            .clip(RoundedCornerShape(maxOf(14, cornerRadius - 4).dp)),
        contentAlignment = Alignment.CenterStart
    ) {
        val totalWidthPx = constraints.maxWidth.toFloat()
        val density = androidx.compose.ui.platform.LocalDensity.current.density
        val segmentWidthPx = totalWidthPx / segmentCount

        var isDragging by remember { mutableStateOf(false) }
        var dragOffsetPx by remember { mutableFloatStateOf(safeIndex * segmentWidthPx) }
        var accumulatedDragPx by remember { mutableFloatStateOf(safeIndex * segmentWidthPx) }
        var isUnlatched by remember { mutableStateOf(false) }
        var lastTensionTickIndex by remember { mutableIntStateOf(0) }
        var lastSnappedMode by remember { mutableIntStateOf(safeIndex) }

        val targetIndicatorOffsetPx = safeIndex * segmentWidthPx

        // Fluid spring physics for the sliding active indicator
        val animatedOffsetPx by animateFloatAsState(
            targetValue = if (isDragging) dragOffsetPx else targetIndicatorOffsetPx,
            animationSpec = spring(
                dampingRatio = if (isDragging) 0.88f else Spring.DampingRatioLowBouncy,
                stiffness = if (isDragging) 1400f else Spring.StiffnessMediumLow
            ),
            label = "face_toggle_sliding_offset"
        )

        // Subtle stretch factor when actively moving
        val stretchFactor by animateFloatAsState(
            targetValue = if (isDragging) 1.06f else 1.0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "face_toggle_stretch"
        )

        // Segment divider notches between inactive options
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until segmentCount - 1) {
                Spacer(modifier = Modifier.weight(1f))
                val isNearActive = abs(i - safeIndex) <= 0 || (isDragging && abs((dragOffsetPx / segmentWidthPx) - (i + 0.5f)) < 0.75f)
                val dividerAlpha by animateFloatAsState(
                    targetValue = if (isNearActive) 0f else 0.25f,
                    animationSpec = spring(stiffness = Spring.StiffnessHigh),
                    label = "divider_alpha_$i"
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(14.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = dividerAlpha * tc),
                            shape = CircleShape
                        )
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        }

        // Active Floating Pill Indicator (Google Pixel M3 Expressive container)
        val pillShape = RoundedCornerShape(maxOf(12, cornerRadius - 5).dp)
        Box(
            modifier = Modifier
                .offset { androidx.compose.ui.unit.IntOffset(animatedOffsetPx.roundToInt(), 0) }
                .width(androidx.compose.ui.unit.Dp((segmentWidthPx * stretchFactor) / density))
                .fillMaxHeight()
                .shadow(
                    elevation = if (isDragging) 5.dp else 2.dp,
                    shape = pillShape,
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                )
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = tc),
                            MaterialTheme.colorScheme.primary.copy(alpha = tc * 0.92f)
                        )
                    ),
                    shape = pillShape
                )
        )

        // Interactive Tappable & Draggable Items Row
        Row(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(segmentWidthPx) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            val startIndex = (offset.x / segmentWidthPx).toInt().coerceIn(0, segmentCount - 1)
                            dragOffsetPx = startIndex * segmentWidthPx
                            accumulatedDragPx = startIndex * segmentWidthPx
                            isUnlatched = false
                            lastTensionTickIndex = 0
                            lastSnappedMode = startIndex

                            coroutineScope.launch {
                                pressScale.animateTo(
                                    0.98f,
                                    spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
                                )
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            coroutineScope.launch {
                                pressScale.animateTo(
                                    1f,
                                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                                )
                            }
                            val finalMode = (dragOffsetPx / segmentWidthPx).roundToInt().coerceIn(0, segmentCount - 1)
                            triggerSnapHapticState()
                            onModeSelectState(finalMode)
                        },
                        onDragCancel = {
                            isDragging = false
                            coroutineScope.launch {
                                pressScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val maxOffset = (segmentCount - 1) * segmentWidthPx
                            val unlatchThreshPx = 18f * density

                            if (!isUnlatched) {
                                val rawNewDrag = accumulatedDragPx + dragAmount
                                accumulatedDragPx = rawNewDrag.coerceIn(-segmentWidthPx, maxOffset + segmentWidthPx)
                                val pullDistance = accumulatedDragPx - (currentModeState * segmentWidthPx)
                                dragOffsetPx = (currentModeState * segmentWidthPx) + pullDistance * 0.25f

                                val numTicks = (abs(pullDistance) / (4f * density)).toInt()
                                if (numTicks > lastTensionTickIndex) {
                                    triggerTensionHapticState()
                                    lastTensionTickIndex = numTicks
                                }

                                if (abs(pullDistance) >= unlatchThreshPx) {
                                    isUnlatched = true
                                    triggerUnlatchHapticState()
                                    dragOffsetPx = accumulatedDragPx.coerceIn(0f, maxOffset)
                                }
                            } else {
                                accumulatedDragPx = (accumulatedDragPx + dragAmount).coerceIn(0f, maxOffset)
                                dragOffsetPx = accumulatedDragPx

                                val candidateMode = (dragOffsetPx / segmentWidthPx).roundToInt().coerceIn(0, segmentCount - 1)
                                if (candidateMode != lastSnappedMode) {
                                    triggerLightHapticState()
                                    lastSnappedMode = candidateMode
                                }
                            }
                        }
                    )
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEachIndexed { index, itemText ->
                val isSelected = safeIndex == index

                // Spring scale pop for selected face
                val textScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.06f else 0.94f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "face_scale_$index"
                )

                val textColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f * tc)
                    },
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "text_color_$index"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(pillShape)
                        .pointerInput(index) {
                            detectTapGestures(
                                onPress = {
                                    triggerLightHapticState()
                                    coroutineScope.launch {
                                        pressScale.animateTo(0.97f, spring(stiffness = Spring.StiffnessHigh))
                                    }
                                    tryAwaitRelease()
                                    coroutineScope.launch {
                                        pressScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                                    }
                                },
                                onTap = {
                                    if (currentModeState != index) {
                                        triggerSnapHapticState()
                                        onModeSelectState(index)
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .scale(textScale)
                            .padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = itemText,
                            fontSize = if (index < 5) 8.5.sp else 10.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = textColor,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
