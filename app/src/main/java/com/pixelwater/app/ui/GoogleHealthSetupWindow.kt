package com.pixelwater.app.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.pixelwater.app.ui.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.ui.input.pointer.pointerInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleHealthSetupWindow(
    viewModel: WaterViewModel,
    isDismissible: Boolean,
    onDismissRequest: () -> Unit
) {
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val isGreek = appLanguage == "el"
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    val setupWeightFlow by viewModel.setupWeight.collectAsStateWithLifecycle()
    val setupHeightFlow by viewModel.setupHeight.collectAsStateWithLifecycle()
    val creatineEnabledFlow by viewModel.creatineEnabled.collectAsStateWithLifecycle()
    val creatineOptionFlow by viewModel.creatineOption.collectAsStateWithLifecycle()
    val creatineGramsFlow by viewModel.creatineGrams.collectAsStateWithLifecycle()
    val proteinEnabledFlow by viewModel.proteinEnabled.collectAsStateWithLifecycle()
    val proteinFixedFlow by viewModel.proteinFixed.collectAsStateWithLifecycle()
    val proteinMinFlow by viewModel.proteinMin.collectAsStateWithLifecycle()
    val proteinMaxFlow by viewModel.proteinMax.collectAsStateWithLifecycle()

    val materialShapesEnabled by viewModel.materialShapesEnabled.collectAsStateWithLifecycle()
    val auraGlowEnabled by viewModel.auraGlowEnabled.collectAsStateWithLifecycle()
    val shapeRotationEnabled by viewModel.shapeRotationEnabled.collectAsStateWithLifecycle()
    val materialShapesRotationSpeed by viewModel.materialShapesRotationSpeed.collectAsStateWithLifecycle()
    val auraGlowRotationSpeed by viewModel.auraGlowRotationSpeed.collectAsStateWithLifecycle()
    val shapeMonochromeEnabled by viewModel.shapeMonochromeEnabled.collectAsStateWithLifecycle()
    val shapeMonochromeSource by viewModel.shapeMonochromeSource.collectAsStateWithLifecycle()
    val shapeCustomR by viewModel.shapeCustomR.collectAsStateWithLifecycle()
    val shapeCustomG by viewModel.shapeCustomG.collectAsStateWithLifecycle()
    val shapeCustomB by viewModel.shapeCustomB.collectAsStateWithLifecycle()
    val shapeContrastMode by viewModel.shapeContrastMode.collectAsStateWithLifecycle()
    val shapeVariabilityEnabled by viewModel.shapeVariabilityEnabled.collectAsStateWithLifecycle()
    val shapeVariabilityMode by viewModel.shapeVariabilityMode.collectAsStateWithLifecycle()
    val shapeRotMultA by viewModel.shapeRotMultA.collectAsStateWithLifecycle()
    val shapeRotMultB by viewModel.shapeRotMultB.collectAsStateWithLifecycle()
    val shapeRotMultC by viewModel.shapeRotMultC.collectAsStateWithLifecycle()
    val shapeRotMultD by viewModel.shapeRotMultD.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val autoThemeScheduleEnabled by viewModel.autoThemeScheduleEnabled.collectAsStateWithLifecycle()
    val autoThemeScheduleMode by viewModel.autoThemeScheduleMode.collectAsStateWithLifecycle()
    val autoThemeLightStartHour by viewModel.autoThemeLightStartHour.collectAsStateWithLifecycle()
    val autoThemeLightStartMin by viewModel.autoThemeLightStartMin.collectAsStateWithLifecycle()
    val autoThemeDarkStartHour by viewModel.autoThemeDarkStartHour.collectAsStateWithLifecycle()
    val autoThemeDarkStartMin by viewModel.autoThemeDarkStartMin.collectAsStateWithLifecycle()

    val isDark = when (themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> {
            if (autoThemeScheduleEnabled) {
                viewModel.isDarkBySchedule(
                    mode = autoThemeScheduleMode,
                    lightStartHour = autoThemeLightStartHour,
                    lightStartMin = autoThemeLightStartMin,
                    darkStartHour = autoThemeDarkStartHour,
                    darkStartMin = autoThemeDarkStartMin
                )
            } else {
                androidx.compose.foundation.isSystemInDarkTheme()
            }
        }
    }

    // Set interactive state using weight and height flows
    var weight by remember { mutableStateOf(if (setupWeightFlow > 0f) setupWeightFlow else 70f) }
    var height by remember { mutableStateOf(if (setupHeightFlow > 0f) setupHeightFlow else 175f) }
    var creatineEnabled by remember { mutableStateOf(creatineEnabledFlow) }
    var creatineOption by remember { mutableStateOf(creatineOptionFlow) }
    var creatineGrams by remember { mutableStateOf(creatineGramsFlow) }
    var creatineTextExpanded by remember { mutableStateOf(false) }
    var proteinTextExpanded by remember { mutableStateOf(false) }

    var isWeightKg by remember { mutableStateOf(true) }
    var isHeightCm by remember { mutableStateOf(true) }
    val weightInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isWeightPressed by weightInteractionSource.collectIsPressedAsState()
    val heightInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isHeightPressed by heightInteractionSource.collectIsPressedAsState()

    var proteinEnabled by remember { mutableStateOf(proteinEnabledFlow) }
    var proteinFixed by remember { mutableStateOf(proteinFixedFlow) }
    var proteinMin by remember { mutableStateOf(proteinMinFlow) }
    var proteinMax by remember { mutableStateOf(proteinMaxFlow) }

    // Dynamic computations taking into account weight, height, creatine, and protein
    val heightAdjustment = (height - 170f) * 8f
    val baseGoal = ((weight * 33f) + heightAdjustment).toInt().coerceAtLeast(1000)
    val upperGoal = ((weight * 38f) + heightAdjustment).toInt().coerceAtLeast(1200)

    val creatineWaterBoost = if (creatineEnabled) {
        when (creatineOption) {
            "3G" -> 300
            "5G" -> 500
            "10G" -> 1000
            "CUSTOM" -> creatineGrams * 100
            else -> 500
        }
    } else 0

    val proteinWaterBoost = if (proteinEnabled) {
        val averageProtein = if (proteinFixed) {
            proteinMin.toFloat()
        } else {
            (proteinMin + proteinMax) / 2f
        }
        val excessProtein = (averageProtein - 80f).coerceAtLeast(0f)
        (excessProtein * 6f).toInt()
    } else {
        0
    }

    val finalBaseGoal = baseGoal + creatineWaterBoost + proteinWaterBoost
    val finalUpperGoal = upperGoal + creatineWaterBoost + proteinWaterBoost

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = { if (isDismissible) onDismissRequest() },
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            // Google Pixel Styled Setup Window Surface
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title and Header Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Calculate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (isGreek) "Υπολογισμός Στόχου" else "Daily Target Calculator",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isGreek) "Εξατομικευμένη ενυδάτωση" else "Personalized hydration metrics",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (isDismissible) {
                        IconButton(
                            onClick = onDismissRequest,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close setup dialog",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                    androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // 1. Body Weight Card with interactive Sliders and Precise Plus / Minus buttons
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (isGreek) "Βάρος Σώματος" else "Body Weight",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isGreek) "Ρυθμίζει τον βασικό όγκο νερού" else "Determines critical base rate",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                val weightBgColor by animateColorAsState(targetValue = if (isWeightPressed) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                val weightTextColor by animateColorAsState(targetValue = if (isWeightPressed) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary)
                                
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(weightBgColor)
                                        .clickable(
                                            interactionSource = weightInteractionSource,
                                            indication = null
                                        ) {
                                            isWeightKg = !isWeightKg
                                            viewModel.triggerButtonHaptic()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val displayWeightStr = if (isWeightKg) {
                                        "${weight.toInt()} kg"
                                    } else {
                                        "${(weight * 2.20462f).toInt()} lb"
                                    }
                                    Text(
                                        text = displayWeightStr,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Black,
                                        color = weightTextColor
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledIconButton(
                                    onClick = {
                                        if (weight > 30f) {
                                            weight--
                                            viewModel.triggerButtonHaptic()
                                        }
                                    },
                                    modifier = Modifier.size(36.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    )
                                ) {
                                    Icon(Icons.Rounded.Remove, contentDescription = "Decrease weight", modifier = Modifier.size(18.dp))
                                }

                                Slider(
                                    value = weight,
                                    onValueChange = { weight = it },
                                    valueRange = 30f..150f,
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                    )
                                )

                                FilledIconButton(
                                    onClick = {
                                        if (weight < 150f) {
                                            weight++
                                            viewModel.triggerButtonHaptic()
                                        }
                                    },
                                    modifier = Modifier.size(36.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    )
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Increase weight", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    // 2. Body Height Card with interactive Sliders and Precise Plus / Minus buttons
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (isGreek) "Ύψος Σώματος" else "Body Height",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isGreek) "Για ακριβή υπολογισμό προτύπων" else "Helps model biological standards",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                val heightBgColor by animateColorAsState(targetValue = if (isHeightPressed) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                val heightTextColor by animateColorAsState(targetValue = if (isHeightPressed) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary)
                                
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(heightBgColor)
                                        .clickable(
                                            interactionSource = heightInteractionSource,
                                            indication = null
                                        ) {
                                            isHeightCm = !isHeightCm
                                            viewModel.triggerButtonHaptic()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val displayHeightStr = if (isHeightCm) {
                                        "${height.toInt()} cm"
                                    } else {
                                        val totalInches = (height / 2.54f).toInt()
                                        val ft = totalInches / 12
                                        val inches = totalInches % 12
                                        "${ft}'${inches}\""
                                    }
                                    Text(
                                        text = displayHeightStr,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Black,
                                        color = heightTextColor
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledIconButton(
                                    onClick = {
                                        if (height > 100f) {
                                            height--
                                            viewModel.triggerButtonHaptic()
                                        }
                                    },
                                    modifier = Modifier.size(36.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    )
                                ) {
                                    Icon(Icons.Rounded.Remove, contentDescription = "Decrease height", modifier = Modifier.size(18.dp))
                                }

                                Slider(
                                    value = height,
                                    onValueChange = { height = it },
                                    valueRange = 100f..220f,
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                    )
                                )

                                FilledIconButton(
                                    onClick = {
                                        if (height < 220f) {
                                            height++
                                            viewModel.triggerButtonHaptic()
                                        }
                                    },
                                    modifier = Modifier.size(36.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    )
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Increase height", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    // 3. Dynamic Calculation Output Card
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.WaterDrop,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Text(
                                text = if (isGreek) "Προτεινόμενος Ημερήσιος Στόχος" else "Recommended Daily Target",
                                modifier = Modifier.fillMaxWidth(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "$finalBaseGoal - $finalUpperGoal ml",
                                modifier = Modifier.fillMaxWidth(),
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                letterSpacing = (-0.5).sp,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = if (isGreek) {
                                    "Βασίζεται στο βάρος και το ύψος σας.${if (creatineEnabled) " Περιλαμβάνει συμπλήρωμα κρεατίνης (+$creatineWaterBoost ml)." else ""}"
                                } else {
                                    "Based on optimal guidelines for your weight and height.${if (creatineEnabled) " Includes creatine supplementation hydration booster (+$creatineWaterBoost ml)." else ""}"
                                },
                                modifier = Modifier.fillMaxWidth(),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // 4. Creatine & Health Connect Sync Settings Card
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Creatine Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            creatineTextExpanded = !creatineTextExpanded
                                            viewModel.triggerButtonHaptic()
                                        },
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.FitnessCenter,
                                            contentDescription = null,
                                            tint = if (creatineEnabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (isGreek) "Συμπλήρωμα Κρεατίνης" else "Creatine Supplement",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = if (creatineTextExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                                contentDescription = "Expand description",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Text(
                                            text = if (creatineEnabled) {
                                                if (isGreek) "Αύξηση ημερήσιου στόχου (+$creatineWaterBoost ml)" else "Boost daily target (+$creatineWaterBoost ml)"
                                            } else {
                                                if (isGreek) "Ενεργοποίηση για υπολογισμό δόσης" else "Enable for custom dosage water boost"
                                            },
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = creatineEnabled,
                                    onCheckedChange = {
                                        creatineEnabled = it
                                        viewModel.triggerToggleHaptic(reversed = !it)
                                    }
                                )
                            }

                            AnimatedVisibility(
                                visible = creatineTextExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Text(
                                    text = if (isGreek) {
                                        "Η συμπλήρωση με κρεατίνη αυξάνει την ενδοκυτταρική κατακράτηση νερού στους μύες, καθιστώντας απαραίτητη την πρόσθετη κατανάλωση υγρών για την αποφυγή αφυδάτωσης και τη βέλτιστη απόδοση."
                                    } else {
                                        "Creatine supplementation increases intracellular water retention in muscles, making additional fluid intake essential to prevent dehydration and ensure optimal performance."
                                    },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 46.dp, top = 2.dp, bottom = 4.dp)
                                )
                            }

                            // Creatine Dosage Preset Selector & Custom Wheel Slider
                            AnimatedVisibility(
                                visible = creatineEnabled,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 2.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = if (isGreek) "Ημερήσια Δόση Κρεατίνης" else "Daily Creatine Dosage",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(38.dp)
                                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), shape = CircleShape)
                                            .padding(2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val dosageOptions = listOf(
                                            "3G" to "3g",
                                            "5G" to "5g",
                                            "10G" to "10g",
                                            "CUSTOM" to if (isGreek) "Προσαρμογή" else "Custom"
                                        )

                                        dosageOptions.forEach { (key, label) ->
                                            val selected = creatineOption == key
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .background(
                                                        if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                        shape = CircleShape
                                                    )
                                                    .clickable {
                                                        creatineOption = key
                                                        if (key == "3G") creatineGrams = 3
                                                        if (key == "5G") creatineGrams = 5
                                                        if (key == "10G") creatineGrams = 10
                                                        viewModel.triggerButtonHaptic()
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    if (creatineOption == "CUSTOM") {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (isGreek) "Ποσότητα Κρεατίνης (g)" else "Creatine Amount (g)",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                
                                                Box(
                                                    modifier = Modifier
                                                        .background(Color(0xFFE2F3C0), shape = CircleShape)
                                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = "$creatineGrams g (+${creatineGrams * 100} ml)",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color(0xFF131512)
                                                    )
                                                }
                                            }

                                            PixelSliderWheel(
                                                value = creatineGrams,
                                                onValueChange = {
                                                    creatineGrams = it
                                                    viewModel.triggerButtonHaptic()
                                                },
                                                valueRange = 1..25,
                                                pxPerUnit = 36f,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }

                            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                            // Google Health Connect Sync Row
                            val workoutWaterAdjustmentEnabledFlow by viewModel.workoutWaterAdjustmentEnabled.collectAsStateWithLifecycle()
                            var workoutAutoEnabled by remember { mutableStateOf(workoutWaterAdjustmentEnabledFlow) }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Sync,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = if (isGreek) "Συγχρονισμός Google Health Connect" else "Sync Google Health Connect",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isGreek) "Αυτόματη προσαρμογή βάσει προπονήσεων" else "Auto-adjust targets based on activity",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = workoutAutoEnabled,
                                    onCheckedChange = {
                                        workoutAutoEnabled = it
                                        viewModel.updateWorkoutWaterAdjustmentEnabled(it)
                                        viewModel.triggerToggleHaptic(reversed = !it)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 5. High Protein Intake Settings Card
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Protein Toggle Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            proteinTextExpanded = !proteinTextExpanded
                                            viewModel.triggerButtonHaptic()
                                        },
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Restaurant,
                                            contentDescription = null,
                                            tint = if (proteinEnabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (isGreek) "Υψηλή Πρωτεϊνική Δίαιτα" else "High Protein Diet",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = if (proteinTextExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                                contentDescription = "Expand description",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Text(
                                            text = if (isGreek) "Αυξάνει τις ανάγκες ενυδάτωσης" else "Elevates hydration needs scientifically",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = proteinEnabled,
                                    onCheckedChange = {
                                        proteinEnabled = it
                                        viewModel.triggerToggleHaptic(reversed = !it)
                                    }
                                )
                            }

                            AnimatedVisibility(
                                visible = proteinTextExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Text(
                                    text = if (isGreek) {
                                        "Η αυξημένη πρόσληψη πρωτεΐνης αυξάνει τις ανάγκες του οργανισμού σε νερό, καθώς οι νεφροί χρειάζονται περισσότερα υγρά για να μεταβολίσουν και να αποβάλουν τα υποπροϊόντα της "
                                    } else {
                                        "Increased protein intake increases the body's water needs, as the kidneys require more fluids to metabolize and excrete its byproducts."
                                    },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 46.dp, top = 2.dp, bottom = 4.dp)
                                )
                            }

                            // Only show if protein option is toggled on
                            AnimatedVisibility(
                                visible = proteinEnabled,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Custom Segmented Control to choose between Fixed or Range
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(38.dp)
                                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), shape = CircleShape)
                                            .padding(2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .background(
                                                    if (proteinFixed) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                    shape = CircleShape
                                                )
                                                .clickable {
                                                    proteinFixed = true
                                                    viewModel.triggerButtonHaptic()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (isGreek) "Σταθερός Στόχος" else "Fixed Target",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (proteinFixed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .background(
                                                    if (!proteinFixed) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                    shape = CircleShape
                                                )
                                                .clickable {
                                                    proteinFixed = false
                                                    viewModel.triggerButtonHaptic()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (isGreek) "Εύρος Στόχου" else "Range Target",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (!proteinFixed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (proteinFixed) {
                                        // Fixed Option
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (isGreek) "Ημερήσια Πρωτεΐνη" else "Daily Protein intake",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                
                                                Box(
                                                    modifier = Modifier
                                                        .background(Color(0xFFE2F3C0), shape = CircleShape)
                                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = "$proteinMin g",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color(0xFF131512)
                                                    )
                                                }
                                            }

                                            PixelSliderWheel(
                                                value = proteinMin,
                                                onValueChange = {
                                                    proteinMin = it
                                                    viewModel.triggerButtonHaptic()
                                                },
                                                valueRange = 50..300,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    } else {
                                        // Range Option with From & To Wheels
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            // Minimum protein wheel
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = if (isGreek) "Ελάχιστη Πρωτεΐνη (Από)" else "Minimum Protein Intake (From)",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .background(Color(0xFFE2F3C0), shape = CircleShape)
                                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                                    ) {
                                                        Text(
                                                            text = "$proteinMin g",
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = Color(0xFF131512)
                                                        )
                                                    }
                                                }

                                                PixelSliderWheel(
                                                    value = proteinMin,
                                                    onValueChange = {
                                                        proteinMin = it
                                                        if (proteinMin > proteinMax) {
                                                            proteinMax = proteinMin
                                                        }
                                                        viewModel.triggerButtonHaptic()
                                                    },
                                                    valueRange = 50..300,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }

                                            // Maximum protein wheel
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = if (isGreek) "Μέγιστη Πρωτεΐνη (Έως)" else "Maximum Protein Intake (To)",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .background(Color(0xFFE2F3C0), shape = CircleShape)
                                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                                    ) {
                                                        Text(
                                                            text = "$proteinMax g",
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = Color(0xFF131512)
                                                        )
                                                    }
                                                }

                                                PixelSliderWheel(
                                                    value = proteinMax,
                                                    onValueChange = {
                                                        proteinMax = it
                                                        if (proteinMax < proteinMin) {
                                                            proteinMin = proteinMax
                                                        }
                                                        viewModel.triggerButtonHaptic()
                                                    },
                                                    valueRange = 50..300,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 6. Action Button Controls Row - Google Pixel Tablet Design Pattern
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isDismissible) {
                            OutlinedButton(
                                onClick = onDismissRequest,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = if (isGreek) "Ακύρωση" else "Cancel",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (weight > 0f) {
                                    viewModel.triggerButtonHaptic()
                                    viewModel.saveSetupData(
                                        weight = weight,
                                        height = height,
                                        baseGoal = finalBaseGoal,
                                        upperGoal = finalUpperGoal,
                                        creatine = creatineEnabled,
                                        proteinEnabled = proteinEnabled,
                                        proteinFixed = proteinFixed,
                                        proteinMin = proteinMin,
                                        proteinMax = proteinMax,
                                        creatineOption = creatineOption,
                                        creatineGrams = creatineGrams
                                    )
                                    onDismissRequest()
                                }
                            },
                            enabled = weight > 0f,
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(
                                text = if (isGreek) "Εφαρμογή Στόχου" else "Apply Profile Goal",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
}

@androidx.compose.runtime.Composable
fun FirebaseLogoIcon(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val orangePath = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.12f, h * 0.75f)
            lineTo(w * 0.50f, h * 0.05f)
            lineTo(w * 0.62f, h * 0.18f)
            close()
        }
        drawPath(path = orangePath, color = Color(0xFFFFCA28)) // Amber

        val redPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.12f, h * 0.75f)
            lineTo(w * 0.62f, h * 0.18f)
            lineTo(w * 0.88f, h * 0.75f)
            lineTo(w * 0.50f, h * 0.95f)
            close()
        }
        drawPath(path = redPath, color = Color(0xFFF57C00)) // Orange

        val yellowPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.50f, h * 0.95f)
            lineTo(w * 0.88f, h * 0.75f)
            lineTo(w * 0.50f, h * 0.45f)
            close()
        }
        drawPath(path = yellowPath, color = Color(0xFFDD2C00)) // Crimson Red
    }
}

@Composable
fun PixelSliderWheel(
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: IntRange,
    modifier: Modifier = Modifier,
    pxPerUnit: Float = 8f
) {
    val minVal = valueRange.first
    val maxVal = valueRange.last

    var internalValue by remember { mutableStateOf(value.toFloat()) }
    
    // Sync external changes safely (only if they are completely out of sync)
    LaunchedEffect(value) {
        if (java.lang.Math.abs(internalValue - value) > 1.5f) {
            internalValue = value.toFloat()
        }
    }

    val draggableState = rememberDraggableState { delta ->
        val newValue = (internalValue - delta / pxPerUnit).coerceIn(minVal.toFloat(), maxVal.toFloat())
        internalValue = newValue
        
        val newIntValue = java.lang.Math.round(newValue)
        if (newIntValue != value) {
            onValueChange(newIntValue)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF131512)) // Dark sleek Pixel tone
            .border(
                width = 1.dp,
                color = Color.LightGray.copy(alpha = 0.15f),
                shape = RoundedCornerShape(24.dp)
            )
            .draggable(
                state = draggableState,
                orientation = Orientation.Horizontal
            )
    ) {
        // Draw the wheel ticks inside Canvas
        androidx.compose.foundation.Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f
            val centerScreenX = width / 2f

            val centerTickValue = internalValue.toInt()
            val fractionalOffset = (internalValue - centerTickValue) * pxPerUnit

            // Draw ticks in range
            val maxUnitsOnHalf = (centerScreenX / pxPerUnit).toInt() + 2

            for (diff in -maxUnitsOnHalf..maxUnitsOnHalf) {
                val tickValue = centerTickValue + diff
                if (tickValue in minVal..maxVal) {
                    val x = centerScreenX + (diff * pxPerUnit) - fractionalOffset
                    
                    val isMajor = if (maxVal <= 30) (tickValue % 5 == 0) else (tickValue % 10 == 0)
                    val isIntermediate = if (maxVal <= 30) (tickValue % 1 == 0 && !isMajor) else (tickValue % 5 == 0 && !isMajor)
                    
                    val tickHeight = when {
                        isMajor -> 34f
                        isIntermediate -> 22f
                        else -> 12f
                    }
                    
                    val tickColor = when {
                        isMajor -> Color.LightGray.copy(alpha = 0.9f)
                        isIntermediate -> Color.Gray.copy(alpha = 0.6f)
                        else -> Color.DarkGray.copy(alpha = 0.35f)
                    }

                    val tickWidth = if (isMajor) 2.5f else 1.5f

                    drawLine(
                        color = tickColor,
                        start = androidx.compose.ui.geometry.Offset(x, centerY - tickHeight / 2f),
                        end = androidx.compose.ui.geometry.Offset(x, centerY + tickHeight / 2f),
                        strokeWidth = tickWidth
                    )
                }
            }

            // Draw Central highlighted indicator
            val centerColor = Color(0xFFE2F3C0) 
            drawLine(
                color = centerColor,
                start = androidx.compose.ui.geometry.Offset(centerScreenX, centerY - 24f),
                end = androidx.compose.ui.geometry.Offset(centerScreenX, centerY + 24f),
                strokeWidth = 4f
            )
        }

        // Dark gradient overlays at left and right edges (gives "darkening in the edges" exactly like the screenshot cylinder lens)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(64.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(Color(0xFF131512), Color.Transparent)
                    )
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(64.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, Color(0xFF131512))
                    )
                )
        )
    }
}
