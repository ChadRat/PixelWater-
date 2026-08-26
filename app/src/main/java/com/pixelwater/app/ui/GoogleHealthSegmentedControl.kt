package com.pixelwater.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import com.pixelwater.app.ui.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset

@Composable
fun <T> GoogleHealthSegmentedControl(
    items: List<T>,
    selectedItem: T,
    onItemSelection: (T) -> Unit,
    modifier: Modifier = Modifier,
    triggerHaptic: () -> Unit = {},
    itemContent: @Composable (item: T, isSelected: Boolean) -> Unit
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val itemWidth = if (items.isNotEmpty()) containerSize.width / items.size else 0

    val selectedIndex = items.indexOf(selectedItem)
    val indicatorOffset by animateFloatAsState(
        targetValue = (selectedIndex * itemWidth).toFloat(),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "indicator_offset"
    )

    val view = androidx.compose.ui.platform.LocalView.current

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(
                if (!isDark) {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(32.dp)
                    )
                } else Modifier
            )
            .padding(6.dp)
            .onSizeChanged { containerSize = it }
    ) {
        if (itemWidth > 0 && selectedIndex >= 0) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(1f / items.size)
                    .offset { IntOffset(indicatorOffset.toInt(), 0) }
                    .clip(RoundedCornerShape(26.dp))
                    .background(MaterialTheme.colorScheme.surface)
            )
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(26.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (!isSelected) {
                                    try {
                                        view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                                        view.postDelayed({
                                            try {
                                                view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                                            } catch (e: Exception) {}
                                        }, 40)
                                    } catch (e: Exception) {
                                        triggerHaptic()
                                    }
                                    onItemSelection(item)
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    itemContent(item, isSelected)
                }
            }
        }
    }
}

@Composable
fun <T> GoogleHealthSegmentedControl(
    items: List<T>,
    selectedItem: T,
    onItemSelection: (T) -> Unit,
    itemLabel: (T) -> String,
    modifier: Modifier = Modifier,
    triggerHaptic: () -> Unit = {}
) {
    GoogleHealthSegmentedControl(
        items = items,
        selectedItem = selectedItem,
        onItemSelection = onItemSelection,
        modifier = modifier,
        triggerHaptic = triggerHaptic,
        itemContent = { item, isSelected ->
            Text(
                text = itemLabel(item),
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    )
}
