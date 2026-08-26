package com.pixelwater.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun DevEnhancedHapticsTesterCard(
    viewModel: WaterViewModel,
    appLanguage: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val enhancedHapticStrength by viewModel.enhancedHapticStrength.collectAsStateWithLifecycle()
    val devHapticTestGapMs by viewModel.devHapticTestGapMs.collectAsStateWithLifecycle()

    var isLoopRunning by remember { mutableStateOf(false) }
    var selectedPattern by remember { mutableStateOf("BUTTON") }

    val isLinear = remember(context) { HapticManager.isLinearMotor(context) }

    fun triggerPattern(patternKey: String) {
        when (patternKey) {
            "BUTTON" -> viewModel.triggerCustomEnhancedHaptic(CustomHapticType.BUTTON)
            "LIGHT_TICK" -> viewModel.triggerCustomEnhancedHaptic(CustomHapticType.LIGHT_TICK)
            "TOGGLE_SNAP" -> viewModel.triggerCustomEnhancedHaptic(CustomHapticType.TOGGLE_SNAP)
            "TOGGLE_LIGHT" -> viewModel.triggerCustomEnhancedHaptic(CustomHapticType.TOGGLE_LIGHT)
            "SLIDER" -> viewModel.triggerCustomEnhancedHaptic(CustomHapticType.SLIDER)
            "WOOP" -> viewModel.triggerCustomEnhancedHaptic(CustomHapticType.WOOP)
            "GOAL" -> viewModel.triggerCustomEnhancedHaptic(CustomHapticType.GOAL)
            "CONFETTI" -> viewModel.triggerCustomEnhancedHaptic(CustomHapticType.CONFETTI_BURST)
            "SIDE_BURST" -> viewModel.triggerCustomEnhancedHaptic(CustomHapticType.SIDE_BURST)
            "RAINFALL" -> viewModel.triggerCustomEnhancedHaptic(CustomHapticType.RAINFALL)
            "FIREWORKS" -> viewModel.triggerCustomEnhancedHaptic(CustomHapticType.FIREWORKS)
            "UNLATCH" -> viewModel.triggerDevUnlatchHaptic()
            "TENSION" -> viewModel.triggerDevTensionHaptic()
            "TAB_TRACK" -> viewModel.triggerCustomEnhancedHaptic(CustomHapticType.TAB_TRACK)
            "TAB_STATS" -> viewModel.triggerCustomEnhancedHaptic(CustomHapticType.TAB_STATS)
            "TAB_AI" -> viewModel.triggerCustomEnhancedHaptic(CustomHapticType.TAB_AI)
            "TAB_REMINDERS" -> viewModel.triggerCustomEnhancedHaptic(CustomHapticType.TAB_REMINDERS)
            "TAB_SETTINGS" -> viewModel.triggerCustomEnhancedHaptic(CustomHapticType.TAB_SETTINGS)
            else -> viewModel.triggerCustomEnhancedHaptic(CustomHapticType.BUTTON)
        }
    }

    // Live continuous loop that adjusts delay live as user drags slider
    LaunchedEffect(isLoopRunning, selectedPattern) {
        if (isLoopRunning) {
            while (isActive) {
                triggerPattern(selectedPattern)
                val currentGap = devHapticTestGapMs
                val waitTime = if (currentGap <= 0f) 15L else currentGap.toLong().coerceAtLeast(15L)
                delay(waitTime)
            }
        }
    }

    // Stop loop if user navigates away or unmounts
    DisposableEffect(Unit) {
        onDispose {
            isLoopRunning = false
        }
    }

    ChunkySettingCard {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Vibration,
                        contentDescription = "Haptics",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (appLanguage == "el") "Δοκιμή Ενισχυμένης Απτικής Ανάδρασης" else "Enhanced Haptics Live Tester",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isLinear) {
                            if (appLanguage == "el") "Γραμμικό Μοτέρ (Linear Motor): Ενεργό & Υποστηρίζεται" else "Linear Motor: Active & Supported"
                        } else {
                            if (appLanguage == "el") "Τυπικό Μοτέρ (ERM / PWM Fallback)" else "Standard Motor (PWM Fallback)"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isLinear) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = if (appLanguage == "el")
                    "Δοκιμάστε ζωντανά όλα τα προηγμένα μοτίβα δόνησης (Enhanced Haptics) και αλλάξτε το διάκενο (gap) μεταξύ των διαδοχικών παλμών σε πραγματικό χρόνο."
                    else "Test all advanced haptic patterns and change the interval gap between consecutive pulses live in real time.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                lineHeight = 16.sp
            )

            // Gap Slider Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (appLanguage == "el") "Διάκενο Παλμών (Gap):" else "Pulse Interval Gap:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (devHapticTestGapMs <= 0f) {
                                if (appLanguage == "el") "0 ms (Χωρίς κενό)" else "0 ms (Zero gap)"
                            } else {
                                "${devHapticTestGapMs.toInt()} ms"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Interactive Live Slider with continuous fine-grained steps
                DevOptionsSliderRow(
                    titlePrefix = if (appLanguage == "el") "Τιμή διακένου: " else "Gap value: ",
                    value = devHapticTestGapMs,
                    defaultValue = 120f,
                    valueRange = 0f..2000f,
                    stepsCount = 0,
                    onValueChange = { viewModel.updateDevHapticTestGapMs(it) },
                    triggerHaptic = { viewModel.triggerSliderHaptic() },
                    formatValue = { v ->
                        if (v <= 0f) (if (appLanguage == "el") "0 ms (Χωρίς κενό)" else "0 ms (No gap)")
                        else "${v.toInt()} ms"
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Preset Chips for rapid testing
                Text(
                    text = if (appLanguage == "el") "Γρήγορες Προεπιλογές (Presets):" else "Quick Gap Presets:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val presets = listOf(
                    0f to if (appLanguage == "el") "0 ms (Χωρίς κενό)" else "0 ms (Zero)",
                    15f to "15 ms",
                    40f to "40 ms",
                    80f to "80 ms",
                    120f to "120 ms",
                    250f to "250 ms",
                    500f to "500 ms",
                    1000f to "1000 ms"
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presets.size) { idx ->
                        val (presetVal, presetLabel) = presets[idx]
                        val isSelected = kotlin.math.abs(devHapticTestGapMs - presetVal) < 1f
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.updateDevHapticTestGapMs(presetVal)
                                viewModel.triggerSliderHaptic()
                            },
                            label = {
                                Text(
                                    text = presetLabel,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // Live Test Controls & Loop Player
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (appLanguage == "el") "Έλεγχος Δοκιμής & Αναπαραγωγή" else "Live Playback & Trigger Tests",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Loop playback button
                Button(
                    onClick = {
                        isLoopRunning = !isLoopRunning
                        viewModel.triggerButtonHaptic()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLoopRunning) Color(0xFFE53935) else MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = if (isLoopRunning) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isLoopRunning) {
                            if (appLanguage == "el") "Διακοπή Συνεχούς Δοκιμής (Gap: ${devHapticTestGapMs.toInt()}ms)" else "Stop Live Loop (Gap: ${devHapticTestGapMs.toInt()}ms)"
                        } else {
                            if (appLanguage == "el") "Έναρξη Συνεχούς Δοκιμής (Live Loop)" else "Start Live Continuous Loop"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Multi-pulse burst buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Double Pulse Button (2x)
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                triggerPattern(selectedPattern)
                                val gap = devHapticTestGapMs.toLong()
                                if (gap > 0L) {
                                    delay(gap)
                                }
                                triggerPattern(selectedPattern)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (appLanguage == "el") "Διπλός Παλμός (2x)" else "Double Pulse (2x)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Triple Burst Button (3x)
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                triggerPattern(selectedPattern)
                                val gap = devHapticTestGapMs.toLong()
                                if (gap > 0L) delay(gap)
                                triggerPattern(selectedPattern)
                                if (gap > 0L) delay(gap)
                                triggerPattern(selectedPattern)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (appLanguage == "el") "Τριπλός Παλμός (3x)" else "Triple Burst (3x)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Single Pulse Button (1x)
                OutlinedButton(
                    onClick = {
                        triggerPattern(selectedPattern)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.TouchApp,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (appLanguage == "el") "Μονός Παλμός (1x)" else "Single Pulse (1x)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Haptic Strength Selector
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (appLanguage == "el") "Ένταση Ενισχυμένης Απτικής:" else "Enhanced Strength:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = enhancedHapticStrength.displayName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    EnhancedHapticStrength.values().forEach { strength ->
                        val isSelected = enhancedHapticStrength == strength
                        Surface(
                            onClick = {
                                viewModel.updateEnhancedHapticStrength(strength)
                                viewModel.triggerButtonHaptic()
                            },
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = strength.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            // Pattern Selection Grid
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (appLanguage == "el") "Επιλογή Εφέ Απτικής Ανάδρασης:" else "Select Enhanced Tactile Effect:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    letterSpacing = 0.5.sp
                )

                val patterns = listOf(
                    "BUTTON" to (if (appLanguage == "el") "Κουμπί (Button Click)" else "Button Click"),
                    "LIGHT_TICK" to (if (appLanguage == "el") "Ελαφρύ Τικ (Light Tick)" else "Light Tick"),
                    "TOGGLE_SNAP" to (if (appLanguage == "el") "Μηχανικό Snap (Toggle Snap)" else "Toggle Snap"),
                    "TOGGLE_LIGHT" to (if (appLanguage == "el") "Απαλό Toggle (Toggle Light)" else "Toggle Light"),
                    "SLIDER" to (if (appLanguage == "el") "Ολισθητήρας (Slider Detent)" else "Slider Detent"),
                    "WOOP" to (if (appLanguage == "el") "Woop Pop (Ευχάριστο)" else "Woop Pop"),
                    "GOAL" to (if (appLanguage == "el") "Επίτευξη Στόχου (Goal)" else "Goal Celebration"),
                    "CONFETTI" to (if (appLanguage == "el") "Έκρηξη Κομφετί (Confetti)" else "Confetti Burst"),
                    "SIDE_BURST" to (if (appLanguage == "el") "Πλευρική Έκρηξη (Side Burst)" else "Side Burst"),
                    "RAINFALL" to (if (appLanguage == "el") "Σταγόνες Βροχής (Rainfall)" else "Rainfall Drops"),
                    "FIREWORKS" to (if (appLanguage == "el") "Πυροτέχνημα (Fireworks)" else "Fireworks Shock"),
                    "UNLATCH" to (if (appLanguage == "el") "Μηχανικό Unlatch" else "Mechanical Unlatch"),
                    "TENSION" to (if (appLanguage == "el") "Τάνυση Ελατηρίου (Tension)" else "Spring Tension"),
                    "TAB_TRACK" to (if (appLanguage == "el") "Καρτέλα: Καταγραφή" else "Tab: Track"),
                    "TAB_STATS" to (if (appLanguage == "el") "Καρτέλα: Στατιστικά" else "Tab: Stats"),
                    "TAB_AI" to (if (appLanguage == "el") "Καρτέλα: AI Coach" else "Tab: AI Coach"),
                    "TAB_REMINDERS" to (if (appLanguage == "el") "Καρτέλα: Υπενθυμίσεις" else "Tab: Reminders"),
                    "TAB_SETTINGS" to (if (appLanguage == "el") "Καρτέλα: Ρυθμίσεις" else "Tab: Settings")
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    patterns.chunked(2).forEach { rowPair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowPair.forEach { (key, label) ->
                                val isSelected = selectedPattern == key
                                Surface(
                                    onClick = {
                                        selectedPattern = key
                                        triggerPattern(key)
                                    },
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    shape = RoundedCornerShape(10.dp),
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                                    CircleShape
                                                )
                                        )
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            if (rowPair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}
