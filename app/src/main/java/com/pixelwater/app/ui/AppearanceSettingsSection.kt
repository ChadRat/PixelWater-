package com.pixelwater.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.lazy.LazyRow
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.animation.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.*
import kotlinx.coroutines.launch





fun LazyListScope.renderRedesignedAppearanceSettingsSection(
    viewModel: WaterViewModel,
    themeMode: String,
    appTheme: String,
    isFrostedGlassEnabled: Boolean,
    frostedGlassTransparency: Float,
    transparentComponentsEnabled: Boolean,
    componentsTransparency: Float,
    oledModeEnabled: Boolean,
    lightModeDarkTextEnabled: Boolean,
    tabTransitionMode: Int,
    isSwipeTabNavEnabled: Boolean,
    bounceStiffness: Float,
    bounceDamping: Float,
    fadeStiffness: Float,
    fadeDamping: Float,
    slideStiffness: Float,
    slideDamping: Float,
    appLanguage: String,
    isDeveloper: Boolean,
    isEmailDeveloper: Boolean,
    onNavigateToCornerRadius: () -> Unit,
    onScrollLockChange: (Boolean) -> Unit = {},
    onTriggerConfetti: () -> Unit = {},
    isScrollInProgress: Boolean = false
) {
    // 1. THEME & COLORS
    item {
        val configProfile by viewModel.configProfile.collectAsStateWithLifecycle()
        val isNerdMode = configProfile == "NERD_MODE"
        val autoThemeScheduleEnabled by viewModel.autoThemeScheduleEnabled.collectAsStateWithLifecycle()
        val autoThemeScheduleMode by viewModel.autoThemeScheduleMode.collectAsStateWithLifecycle()
        val autoThemeLightStartHour by viewModel.autoThemeLightStartHour.collectAsStateWithLifecycle()
        val autoThemeLightStartMin by viewModel.autoThemeLightStartMin.collectAsStateWithLifecycle()
        val autoThemeDarkStartHour by viewModel.autoThemeDarkStartHour.collectAsStateWithLifecycle()
        val autoThemeDarkStartMin by viewModel.autoThemeDarkStartMin.collectAsStateWithLifecycle()
        val locationCity by viewModel.locationCity.collectAsStateWithLifecycle()

        SettingsCategoryHeader(if (appLanguage == "el") "ΧΡΩΜΑΤΑ & ΘΕΜΑΤΑ" else "THEME & COLORS", icon = Icons.Rounded.Palette)
        ChunkySettingCard {
            SegmentedThemeMode(
                themeMode = themeMode,
                appLanguage = appLanguage,
                autoThemeScheduleEnabled = autoThemeScheduleEnabled,
                autoThemeScheduleMode = autoThemeScheduleMode,
                autoThemeLightStartHour = autoThemeLightStartHour,
                autoThemeLightStartMin = autoThemeLightStartMin,
                autoThemeDarkStartHour = autoThemeDarkStartHour,
                autoThemeDarkStartMin = autoThemeDarkStartMin,
                locationCity = locationCity,
                onThemeSelect = { viewModel.updateThemeMode(it) },
                onToggleAutoSchedule = { viewModel.updateAutoThemeScheduleEnabled(it) },
                onUpdateScheduleMode = { viewModel.updateAutoThemeScheduleMode(it) },
                onUpdateLightStart = { h, m -> viewModel.updateAutoThemeLightStart(h, m) },
                onUpdateDarkStart = { h, m -> viewModel.updateAutoThemeDarkStart(h, m) },
                triggerHaptic = { viewModel.triggerButtonHaptic() }
            )

            HorizontalDivider(paddingVertical = 16.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            val currentStaticSeed by viewModel.staticThemeSeed.collectAsStateWithLifecycle(initialValue = 0)
            ThemeColorRow(
                currentAppTheme = appTheme,
                currentStaticSeed = currentStaticSeed,
                isFrostedGlassEnabled = isFrostedGlassEnabled,
                appLanguage = appLanguage,
                isNerdMode = isNerdMode,
                onAppThemeChange = { viewModel.updateAppTheme(it) },
                onStaticSeedChange = { viewModel.updateStaticThemeSeed(it) },
                onFrostedGlassChange = { viewModel.updateFrostedGlassEnabled(it); viewModel.triggerButtonHaptic() }
            )

            if (appTheme == "STATIC") {
                HorizontalDivider(paddingVertical = 16.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                    var isRgbSlidersExpanded by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isRgbSlidersExpanded = !isRgbSlidersExpanded }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isRgbSlidersExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = "Toggle RGB Sliders",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = if (appLanguage == "el") "Προσαρμοσμένο Χρώμα (RGB)" else "Custom Color (RGB)",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        val colorHex = String.format("#%06X", 0xFFFFFF and currentStaticSeed)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(currentStaticSeed))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                            )
                            Text(
                                text = colorHex,
                                fontSize = 14.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isRgbSlidersExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))

                        val rVal = android.graphics.Color.red(currentStaticSeed)
                        val gVal = android.graphics.Color.green(currentStaticSeed)
                        val bVal = android.graphics.Color.blue(currentStaticSeed)

                        // Red Slider
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = if (appLanguage == "el") "Κόκκινο" else "Red", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = rVal.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Slider(
                                value = rVal.toFloat(),
                                onValueChange = { 
                                    val newColor = android.graphics.Color.rgb(it.toInt(), gVal, bVal)
                                    viewModel.updateStaticThemeSeed(newColor)
                                },
                                valueRange = 0f..255f,
                                colors = SliderDefaults.colors(thumbColor = Color.Red, activeTrackColor = Color.Red.copy(alpha = 0.7f)),
                                modifier = Modifier.height(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Green Slider
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = if (appLanguage == "el") "Πράσινο" else "Green", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = gVal.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Slider(
                                value = gVal.toFloat(),
                                onValueChange = { 
                                    val newColor = android.graphics.Color.rgb(rVal, it.toInt(), bVal)
                                    viewModel.updateStaticThemeSeed(newColor)
                                },
                                valueRange = 0f..255f,
                                colors = SliderDefaults.colors(thumbColor = Color.Green, activeTrackColor = Color.Green.copy(alpha = 0.7f)),
                                modifier = Modifier.height(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Blue Slider
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = if (appLanguage == "el") "Μπλε" else "Blue", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = bVal.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Slider(
                                value = bVal.toFloat(),
                                onValueChange = { 
                                    val newColor = android.graphics.Color.rgb(rVal, gVal, it.toInt())
                                    viewModel.updateStaticThemeSeed(newColor)
                                },
                                valueRange = 0f..255f,
                                colors = SliderDefaults.colors(thumbColor = Color.Blue, activeTrackColor = Color.Blue.copy(alpha = 0.7f)),
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }
                }
            }

            if (appTheme == "DYNAMIC") {
                HorizontalDivider(paddingVertical = 16.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Column {
                    Text(
                        text = if (appLanguage == "el") "Αρμονικά Χρώματα Έμφασης" else "Harmonious Accent Seeds",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val wallpaperColors by viewModel.wallpaperThemeColors.collectAsStateWithLifecycle()
                        val selectedPaletteIdx by viewModel.appThemePaletteIndex.collectAsStateWithLifecycle()

                        wallpaperColors.forEachIndexed { idx, colorInt ->
                            val isSelected = selectedPaletteIdx == idx
                            val (topColor, bottomLeftColor, bottomRightColor) = remember(colorInt) { viewModel.getSwatchColors(colorInt) }

                            GearRotatableShapeButton(
                                isSelected = isSelected,
                                onClick = {
                                    viewModel.updateAppThemePaletteIndex(idx)
                                },
                                modifier = Modifier
                                    .size(56.dp)
                                    .padding(2.dp),
                                shape = CircleShape,
                                borderWidth = if (isSelected) 3.dp else 0.dp,
                                borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawArc(color = topColor, startAngle = 180f, sweepAngle = 180f, useCenter = true)
                                    drawArc(color = bottomLeftColor, startAngle = 90f, sweepAngle = 90f, useCenter = true)
                                    drawArc(color = bottomRightColor, startAngle = 0f, sweepAngle = 90f, useCenter = true)
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val dynamicPaletteShapesOverride by viewModel.dynamicPaletteShapesOverride.collectAsStateWithLifecycle()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { viewModel.updateDynamicPaletteShapesOverride(!dynamicPaletteShapesOverride) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (appLanguage == "el") "Εφαρμογή και στα Σχήματα" else "Apply to Background Shapes",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (appLanguage == "el") "Αλλάζει τα χρώματα των βίντεο" else "Themes will automatically style the animated background layers",
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = dynamicPaletteShapesOverride,
                            onCheckedChange = { viewModel.updateDynamicPaletteShapesOverride(it) },
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                }

                HorizontalDivider(paddingVertical = 16.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Column {
                    val isMonochrome by viewModel.monochromeEnabled.collectAsStateWithLifecycle()
                    val monochromeTarget by viewModel.monochromeColorTarget.collectAsStateWithLifecycle()

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (appLanguage == "el") "Μονόχρωμο Θέμα" else "Monochrome Theme",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        ChunkySettingSwitch(
                            checked = isMonochrome,
                            onCheckedChange = { 
                                viewModel.updateMonochromeEnabled(it)
                                viewModel.triggerButtonHaptic()
                            }
                        )
                    }

                    if (isMonochrome) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (appLanguage == "el") "Επιλέξτε συγκεκριμένο χρώμα παλέτας" else "Target Palette Color",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        ThreeWayColorToggle(
                            currentMode = monochromeTarget,
                            appLanguage = appLanguage,
                            onModeSelect = { viewModel.updateMonochromeColorTarget(it) },
                            triggerLightHaptic = { viewModel.triggerToggleLightHaptic() },
                            triggerSnapHaptic = { viewModel.triggerToggleSnapHaptic() }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    // 1.2 PREMADE STYLES & COLORS
    item {
        Spacer(modifier = Modifier.height(12.dp))
        var arePremadeOptionsExpanded by remember { mutableStateOf(false) }
        ChunkySettingCard {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.triggerButtonHaptic()
                            arePremadeOptionsExpanded = !arePremadeOptionsExpanded
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Style,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = if (appLanguage == "el") "ΠΡΟΚΑΘΟΡΙΣΜΕΝΑ ΣΤΥΛ & ΧΡΩΜΑΤΑ" else "PREMADE STYLES & COLORS",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (appLanguage == "el") "Εφαρμόστε έτοιμους συνδυασμούς εμφάνισης" else "Apply ready-to-go visual presets instantly",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val rotationAngle by animateFloatAsState(
                        targetValue = if (arePremadeOptionsExpanded) 180f else 0f,
                        label = "premade_arrow_rotation"
                    )
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Expand premade options",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(rotationAngle)
                    )
                }

                AnimatedVisibility(
                    visible = arePremadeOptionsExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        val aiCoachShapesOverride by viewModel.aiCoachShapesOverride.collectAsStateWithLifecycle()
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (appLanguage == "el") "Αλλαγή Χρωμάτων Σχημάτων με AI" else "Theme Background Shapes via AI",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (appLanguage == "el") "Ο AI Coach και τα έτοιμα στυλ προσαρμόζουν το φόντο" else "AI Coach & presets will also style the animated shapes",
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = aiCoachShapesOverride,
                                onCheckedChange = { viewModel.updateAiCoachShapesOverride(it) },
                                modifier = Modifier.scale(0.8f)
                            )
                        }

                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                        data class VisualPreset(
                            val title: String,
                            val icon: androidx.compose.ui.graphics.vector.ImageVector,
                            val action: () -> Unit
                        )

                        val optionPreviews = if (appLanguage == "el") {
                            listOf(
                                VisualPreset("Ταίριασμα Ταπετσαρίας", Icons.Rounded.Wallpaper) {
                                    viewModel.updateThemeMode("System")
                                    viewModel.updateOledMode(false)
                                    viewModel.updateBoxBgOledEnabled(false)
                                    viewModel.updateAppTheme("DYNAMIC")
                                    viewModel.updateAppThemePaletteIndex(0)
                                    viewModel.updateBoxBgSource("THEME")
                                    viewModel.updateShapeMonochromeEnabled(false)
                                    viewModel.updateShapeUseIndependentDynamicPalette(false)
                                    viewModel.updateProgressCircleColorSource("THEME")
                                    viewModel.updateProgressCircleThemeColor(0)
                                    viewModel.updateMaterialShapesRotationSpeed(1.0f)
                                    viewModel.updateAuraGlowRotationSpeed(1.0f)
                                    viewModel.updateShapeRotMultA(0.8f)
                                    viewModel.updateShapeRotMultB(-1.2f)
                                    viewModel.updateShapeRotMultC(0.5f)
                                    viewModel.updateShapeRotMultD(-0.7f)
                                },
                                VisualPreset("Μεσάνυχτα OLED", Icons.Rounded.DarkMode) { viewModel.analyzeAndApplyThemeWithAi("Midnight OLED Theme with pitch black background") },
                                VisualPreset("Σμαραγδένιο Δάσος", Icons.Rounded.Eco) { viewModel.analyzeAndApplyThemeWithAi("Emerald Forest dark theme") },
                                VisualPreset("Νέον Γαλαξίας", Icons.Rounded.Bolt) { viewModel.analyzeAndApplyThemeWithAi("Neon Galaxy vibrant cyberpunk dark theme") },
                                VisualPreset("Ροδακινί Cozy", Icons.Rounded.WbSunny) { viewModel.analyzeAndApplyThemeWithAi("Minimalist cozy peach light theme") },
                                VisualPreset("Καθαρός Παγετώνας", Icons.Rounded.AcUnit) { viewModel.analyzeAndApplyThemeWithAi("Glacier breeze light icy blue theme") },
                                VisualPreset("Τσιχλόφουσκα Pop", Icons.Rounded.BubbleChart) { viewModel.analyzeAndApplyThemeWithAi("Bubble gum pop bright pink vibrant light theme") },
                                VisualPreset("Ωκεανός & Ηλιοβασίλεμα", Icons.Rounded.WbTwilight) { viewModel.analyzeAndApplyThemeWithAi("Ocean sunset dark theme with fiery orange and deep blue") },
                                VisualPreset("Λιβάδι Λεβάντας", Icons.Rounded.LocalFlorist) { viewModel.analyzeAndApplyThemeWithAi("Lavender meadow soft purple light theme") },
                                VisualPreset("Άνθη Κερασιάς", Icons.Rounded.Park) { viewModel.analyzeAndApplyThemeWithAi("Cherry blossom pink spring light theme") },
                                VisualPreset("Τροπική Όαση", Icons.Rounded.BeachAccess) { viewModel.analyzeAndApplyThemeWithAi("Tropical oasis dark cyan and teal theme") },
                                VisualPreset("Φθινοπωρινά Φύλλα", Icons.Rounded.Nature) { viewModel.analyzeAndApplyThemeWithAi("Autumn leaves dark theme with warm orange and brown") }
                            )
                        } else {
                            listOf(
                                VisualPreset("Wallpaper Match", Icons.Rounded.Wallpaper) {
                                    viewModel.updateThemeMode("System")
                                    viewModel.updateOledMode(false)
                                    viewModel.updateBoxBgOledEnabled(false)
                                    viewModel.updateAppTheme("DYNAMIC")
                                    viewModel.updateAppThemePaletteIndex(0)
                                    viewModel.updateBoxBgSource("THEME")
                                    viewModel.updateShapeMonochromeEnabled(false)
                                    viewModel.updateShapeUseIndependentDynamicPalette(false)
                                    viewModel.updateProgressCircleColorSource("THEME")
                                    viewModel.updateProgressCircleThemeColor(0)
                                    viewModel.updateMaterialShapesRotationSpeed(1.0f)
                                    viewModel.updateAuraGlowRotationSpeed(1.0f)
                                    viewModel.updateShapeRotMultA(0.8f)
                                    viewModel.updateShapeRotMultB(-1.2f)
                                    viewModel.updateShapeRotMultC(0.5f)
                                    viewModel.updateShapeRotMultD(-0.7f)
                                },
                                VisualPreset("Midnight OLED", Icons.Rounded.DarkMode) { viewModel.analyzeAndApplyThemeWithAi("Midnight OLED Theme with pitch black background") },
                                VisualPreset("Forest Emerald", Icons.Rounded.Eco) { viewModel.analyzeAndApplyThemeWithAi("Emerald Forest dark theme") },
                                VisualPreset("Neon Galaxy", Icons.Rounded.Bolt) { viewModel.analyzeAndApplyThemeWithAi("Neon Galaxy vibrant cyberpunk dark theme") },
                                VisualPreset("Minimalist Peach", Icons.Rounded.WbSunny) { viewModel.analyzeAndApplyThemeWithAi("Minimalist cozy peach light theme") },
                                VisualPreset("Glacier Breeze", Icons.Rounded.AcUnit) { viewModel.analyzeAndApplyThemeWithAi("Glacier breeze light icy blue theme") },
                                VisualPreset("Bubble Gum Pop", Icons.Rounded.BubbleChart) { viewModel.analyzeAndApplyThemeWithAi("Bubble gum pop bright pink vibrant light theme") },
                                VisualPreset("Ocean Sunset", Icons.Rounded.WbTwilight) { viewModel.analyzeAndApplyThemeWithAi("Ocean sunset dark theme with fiery orange and deep blue") },
                                VisualPreset("Lavender Meadow", Icons.Rounded.LocalFlorist) { viewModel.analyzeAndApplyThemeWithAi("Lavender meadow soft purple light theme") },
                                VisualPreset("Cherry Blossom", Icons.Rounded.Park) { viewModel.analyzeAndApplyThemeWithAi("Cherry blossom pink spring light theme") },
                                VisualPreset("Tropical Oasis", Icons.Rounded.BeachAccess) { viewModel.analyzeAndApplyThemeWithAi("Tropical oasis dark cyan and teal theme") },
                                VisualPreset("Autumn Leaves", Icons.Rounded.Nature) { viewModel.analyzeAndApplyThemeWithAi("Autumn leaves dark theme with warm orange and brown") }
                            )
                        }

                        optionPreviews.forEach { preset ->
                            item {
                                val isDark = isSystemInDarkTheme() || MaterialTheme.colorScheme.background.red < 0.3f
                                var isActive by remember { mutableStateOf(false) }
                                val scope = rememberCoroutineScope()
                                var resetJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
                                val scaleAnim = remember { Animatable(1f) }

                                val cornerRadius by animateDpAsState(
                                    targetValue = if (isActive) 100.dp else 14.dp,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    ),
                                    label = "premade_radius_settings"
                                )

                                val containerColor by animateColorAsState(
                                    targetValue = if (isActive) {
                                        if (isDark) Color(0xFFE2E2E2) else MaterialTheme.colorScheme.primary
                                    } else if (isFrostedGlassEnabled) {
                                        Color.Transparent
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    },
                                    animationSpec = tween(250),
                                    label = "premade_container_color_settings"
                                )

                                val contentColor by animateColorAsState(
                                    targetValue = if (isActive) {
                                        if (isDark) Color(0xFF191C1C) else MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    animationSpec = tween(250),
                                    label = "premade_content_color_settings"
                                )

                                val iconTint by animateColorAsState(
                                    targetValue = if (isActive) {
                                        if (isDark) Color(0xFF191C1C) else MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                    animationSpec = tween(250),
                                    label = "premade_icon_color_settings"
                                )

                                val iconBg by animateColorAsState(
                                    targetValue = if (isActive) {
                                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                                    } else {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    },
                                    animationSpec = tween(250),
                                    label = "premade_icon_bg_settings"
                                )

                                Column(
                                    modifier = Modifier
                                        .width(120.dp)
                                        .scale(scaleAnim.value)
                                        .then(
                                            if (!isFrostedGlassEnabled) {
                                                Modifier.shadow(elevation = 2.dp, shape = RoundedCornerShape(maxOf(0f, cornerRadius.value).dp))
                                            } else Modifier
                                        )
                                        .background(
                                            color = containerColor,
                                            shape = RoundedCornerShape(maxOf(0f, cornerRadius.value).dp)
                                        )
                                        .clip(RoundedCornerShape(maxOf(0f, cornerRadius.value).dp))
                                        .clickable {
                                            viewModel.triggerButtonHaptic()
                                            preset.action()
                                            isActive = true
                                            scope.launch {
                                                scaleAnim.animateTo(0.85f, animationSpec = tween(50))
                                                scaleAnim.animateTo(1.08f, animationSpec = spring(dampingRatio = 0.45f, stiffness = 300f))
                                                scaleAnim.animateTo(1.0f, animationSpec = spring())
                                            }
                                            resetJob?.cancel()
                                            resetJob = scope.launch {
                                                delay(1500)
                                                isActive = false
                                            }
                                        }
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(iconBg, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = preset.icon,
                                            contentDescription = preset.title,
                                            tint = iconTint,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Text(
                                        text = preset.title,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = contentColor,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Custom Theme Creator header
                    Text(
                        text = if (appLanguage == "el") "Δημιουργός Προσαρμοσμένου Θέματος AI" else "AI Custom Theme Creator",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    
                    var customThemePrompt by remember { mutableStateOf("") }
                    var strictSelection by remember { mutableStateOf(false) } // False = Imaginary, True = Strict
                    
                    OutlinedTextField(
                        value = customThemePrompt,
                        onValueChange = { customThemePrompt = it },
                        label = { Text(if (appLanguage == "el") "Περιγράψτε το θέμα (π.χ. 'Ηφαιστειακό Νέον')" else "Describe theme (e.g. 'Volcanic Neon')") },
                        modifier = Modifier.fillMaxWidth().testTag("custom_theme_prompt_input"),
                        maxLines = 2,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            if (customThemePrompt.isNotEmpty()) {
                                IconButton(onClick = { customThemePrompt = "" }) {
                                    Icon(Icons.Rounded.Clear, contentDescription = "Clear text")
                                }
                            }
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Pop up mode options for Strict / Imaginary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { strictSelection = true },
                            modifier = Modifier.weight(1f).testTag("strict_mode_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (strictSelection) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (strictSelection) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Rule, 
                                contentDescription = null, 
                                modifier = Modifier.size(16.dp).padding(end = 4.dp)
                            )
                            Text(
                                text = if (appLanguage == "el") "Αυστηρό" else "Strict",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Button(
                            onClick = { strictSelection = false },
                            modifier = Modifier.weight(1f).testTag("imaginary_mode_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!strictSelection) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (!strictSelection) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Brush, 
                                contentDescription = null, 
                                modifier = Modifier.size(16.dp).padding(end = 4.dp)
                            )
                            Text(
                                text = if (appLanguage == "el") "Φανταστικό" else "Imaginary",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    val themeAiLoading by viewModel.themeAiLoading.collectAsStateWithLifecycle()
                    val themeAiError by viewModel.themeAiError.collectAsStateWithLifecycle()
                    
                    if (themeAiLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Text(
                                    text = if (appLanguage == "el") "Ο AI Coach συνθέτει το θέμα σας..." else "AI Coach is crafting your theme...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                if (customThemePrompt.isNotBlank()) {
                                    viewModel.analyzeAndApplyThemeWithAi(
                                        presetName = customThemePrompt,
                                        extraContext = if (strictSelection) "Strict matching request" else "Imaginary detail expansion approved",
                                        isStrict = strictSelection,
                                        isWallpaperBased = false,
                                        saveToCustom = true
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("ai_coach_theme_generate_button"),
                            shape = RoundedCornerShape(12.dp),
                            enabled = customThemePrompt.isNotBlank()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome, 
                                contentDescription = null, 
                                modifier = Modifier.size(18.dp).padding(end = 6.dp)
                            )
                            Text(if (appLanguage == "el") "Δημιουργία με AI Coach" else "Generate with AI Coach", fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    if (themeAiError != null) {
                        Text(
                            text = themeAiError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    
                    val customThemes by viewModel.customThemes.collectAsStateWithLifecycle()
                    
                    if (customThemes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (appLanguage == "el") "Αποθηκευμένα Προσαρμοσμένα Θέματα" else "Saved Custom Themes",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(customThemes.size) { index ->
                                val theme = customThemes[index]
                                val isSelected = viewModel.staticThemeSeed.collectAsStateWithLifecycle().value == theme.staticThemeSeed
                                Card(
                                    modifier = Modifier
                                        .width(140.dp)
                                        .testTag("custom_theme_card_${theme.id}")
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { viewModel.applyCustomTheme(theme) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer 
                                        } else if (isFrostedGlassEnabled) {
                                            Color.Transparent
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        }
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Box(modifier = Modifier.padding(8.dp)) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .background(Color(theme.staticThemeSeed), CircleShape)
                                                )
                                                IconButton(
                                                    onClick = { viewModel.deleteCustomTheme(theme.id) },
                                                    modifier = Modifier.size(20.dp).testTag("delete_theme_button_${theme.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Delete, 
                                                        contentDescription = "Delete theme",
                                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = theme.name,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (theme.isStrict) (if (appLanguage == "el") "Αυστηρό" else "Strict") else (if (appLanguage == "el") "Φανταστικό" else "Imaginary"),
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    }
                }
            }
        }
    }

    // 1.5 PROGRESS CIRCLE STYLE
    item {
        Spacer(modifier = Modifier.height(12.dp))
        SettingsCategoryHeader(if (appLanguage == "el") "ΣΤΥΛ ΚΥΚΛΟΥ ΠΡΟΟΔΟΥ" else "PROGRESS CIRCLE STYLE", icon = Icons.Rounded.DataUsage)
        ChunkySettingCard {
            val configProfile by viewModel.configProfile.collectAsStateWithLifecycle()
            val isNerdMode = configProfile == "NERD_MODE"
            val progressCircleColorSource by viewModel.progressCircleColorSource.collectAsStateWithLifecycle()
            val progressCircleThemeColor by viewModel.progressCircleThemeColor.collectAsStateWithLifecycle()
            val progressCircleStandardColorIndex by viewModel.progressCircleStandardColorIndex.collectAsStateWithLifecycle()
            val progressGlowEnabled by viewModel.progressGlowEnabled.collectAsStateWithLifecycle()
            val showRemaining by viewModel.showWaterRemaining.collectAsStateWithLifecycle()
            val isProgressCircleCardBgRemoved by viewModel.isProgressCircleCardBgRemoved.collectAsStateWithLifecycle()
            val keepCircleInsideOpaque by viewModel.keepCircleInsideOpaque.collectAsStateWithLifecycle()
            val mainLongPressDeleteEnabled by viewModel.mainLongPressDeleteEnabled.collectAsStateWithLifecycle()
            val mainLongPressDeleteDuration by viewModel.mainLongPressDeleteDuration.collectAsStateWithLifecycle()

            // 1. Color Source (Theme vs Standard)
            Text(
                text = if (appLanguage == "el") "Πηγή Χρώματος Κύκλου" else "Circle Color Source",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            MultiWayToggle(
                selectedIndex = if (progressCircleColorSource == "THEME") 0 else 1,
                options = if (appLanguage == "el") listOf("Παλέτα Θέματος", "Σταθερό Χρώμα") else listOf("Theme Palette", "Standard Color"),
                onModeSelect = { idx ->
                    val newSource = if (idx == 0) "THEME" else "STANDARD"
                    viewModel.updateProgressCircleColorSource(newSource)
                },
                triggerLightHaptic = { viewModel.triggerToggleLightHaptic() },
                triggerSnapHaptic = { viewModel.triggerToggleSnapHaptic() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Selection dependant Controls
            if (progressCircleColorSource == "THEME") {
                Text(
                    text = if (appLanguage == "el") "Χρώμα Παλέτας Θέματος" else "Theme Palette Color Accent",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                ThreeWayColorToggle(
                    currentMode = progressCircleThemeColor,
                    appLanguage = appLanguage,
                    onModeSelect = { viewModel.updateProgressCircleThemeColor(it) },
                    triggerLightHaptic = { viewModel.triggerToggleLightHaptic() },
                    triggerSnapHaptic = { viewModel.triggerToggleSnapHaptic() },
                    usePaletteColors = true
                )
            } else {
                Text(
                    text = if (appLanguage == "el") "Επιλέξτε Σταθερό Χρώμα" else "Select Standard Color",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                val standardColors = listOf(
                    Triple(if (appLanguage == "el") "Ωκεανός" else "Ocean", Color(0xFF1D5AAB), Color(0xFF1D5AAB)),
                    Triple(if (appLanguage == "el") "Μοβ" else "Purple", Color(0xFFAB47BC), Color(0xFF8E24AA)),
                    Triple(if (appLanguage == "el") "Δάσος" else "Forest", Color(0xFF66BB6A), Color(0xFF2E7D32)),
                    Triple(if (appLanguage == "el") "Σχιστόλιθος" else "Slate", Color(0xFF78909C), Color(0xFF455A64)),
                    Triple(if (appLanguage == "el") "Πετρόλ" else "Teal", Color(0xFF00BFA5), Color(0xFF0C9B8B)),
                    Triple(if (appLanguage == "el") "Πορτοκαλί" else "Orange", Color(0xFFFF9100), Color(0xFFFF6D00)),
                    Triple(if (appLanguage == "el") "Πορφυρό" else "Crimson", Color(0xFFEC407A), Color(0xFFD81B60)),
                    Triple(if (appLanguage == "el") "Ινδικό" else "Indigo", Color(0xFF536DFE), Color(0xFF3F51B5))
                )

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    standardColors.forEachIndexed { idx, (name, darkColor, lightColor) ->
                        val isSelected = progressCircleStandardColorIndex == idx
                        val activeColor = if (androidx.compose.foundation.isSystemInDarkTheme()) darkColor else lightColor
                        
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(activeColor)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    viewModel.updateProgressCircleStandardColorIndex(idx)
                                    viewModel.triggerButtonHaptic()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Selected",
                                    tint = if (activeColor.red > 0.8f && activeColor.green > 0.8f && activeColor.blue < 0.5f) Color.Black else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(paddingVertical = 16.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            // 3. Companion Switch Progress Glow Halo
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (appLanguage == "el") "Λάμψη Προόδου" else "Progress Glow Halo",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                ChunkySettingSwitch(
                    checked = progressGlowEnabled,
                    onCheckedChange = { 
                        viewModel.updateProgressGlowEnabled(it)
                        viewModel.triggerButtonHaptic()
                    }
                )
            }

            HorizontalDivider(paddingVertical = 16.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            // 4. Companion Switch Show Water Remaining
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (appLanguage == "el") "Εμφάνιση Υπολειπόμενου Νερού" else "Show Water Remaining",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                ChunkySettingSwitch(
                    checked = showRemaining,
                    onCheckedChange = { 
                        viewModel.updateShowWaterRemaining(it)
                        viewModel.triggerButtonHaptic()
                    }
                )
            }

            if (isNerdMode) {
                HorizontalDivider(paddingVertical = 16.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // 4.5 Tap & Hold to Delete last entry
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (appLanguage == "el") "Παρατεταμένο Πάτημα για Διαγραφή" else "Tap & Hold to Delete Last Log",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (appLanguage == "el") "Κρατήστε πατημένο τον κύκλο προόδου για γρήγορη αφαίρεση της τελευταίας καταχώρησης." else "Hold down the progress circle to quickly delete your most recent water log entry.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    ChunkySettingSwitch(
                        checked = mainLongPressDeleteEnabled,
                        onCheckedChange = { 
                            viewModel.updateMainLongPressDeleteEnabled(it)
                            viewModel.triggerButtonHaptic()
                        }
                    )
                }

                if (mainLongPressDeleteEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (appLanguage == "el") "Διάρκεια Παρατεταμένου Πατήματος" else "Hold Duration",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${mainLongPressDeleteDuration} ms",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        CapsulePatternSlider(
                            value = mainLongPressDeleteDuration.toFloat(),
                            onValueChange = { viewModel.updateMainLongPressDeleteDuration(it.toInt()) },
                            valueRange = 100f..3000f,
                            stepsCount = 30, // Increment of 100 ms
                            triggerHaptic = { viewModel.triggerSliderHaptic() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                HorizontalDivider(paddingVertical = 16.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // 5. Companion Switch Remove Progress Circle Card Background
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (appLanguage == "el") "Αφαίρεση Φόντου Κύκλου" else "Remove Box Behind Circle",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (appLanguage == "el") "Αφαιρεί πλήρως το πλαίσιο/κάρτα πίσω από τον κύκλο προόδου" else "Completely removes the card background box behind the progress circle",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    ChunkySettingSwitch(
                        checked = isProgressCircleCardBgRemoved,
                        onCheckedChange = { 
                            viewModel.updateProgressCircleCardBgRemoved(it)
                            viewModel.triggerButtonHaptic()
                        }
                    )
                }

                if (isProgressCircleCardBgRemoved) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (appLanguage == "el") "Διατήρηση Αδιαφανούς Εσωτερικού Κύκλου" else "Keep Pulsing Circle Inside Opaque",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (appLanguage == "el") "Κρατά το εσωτερικό του κύκλου αδιαφανές με το εφέ παλμού" else "Keeps the center of the progress circle opaque while retaining the breathing pulse",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        ChunkySettingSwitch(
                            checked = keepCircleInsideOpaque,
                            onCheckedChange = { 
                                viewModel.updateKeepCircleInsideOpaque(it)
                            }
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (appLanguage == "el") "Χρώμα Κειμένου Κύκλου Προόδου" else "Progress Circle Text Color",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (appLanguage == "el") "Επιλέξτε αν το κείμενο θα ακολουθεί το χρώμα του κύκλου προόδου ή τα χρώματα της παλέτας." else "Choose if the text follows the progress circle color or the palette colors.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        val mainCircleTextColorEnabled by viewModel.mainCircleTextColorEnabled.collectAsStateWithLifecycle()
                        ChunkySettingSwitch(
                            checked = mainCircleTextColorEnabled,
                            onCheckedChange = { 
                                viewModel.updateMainCircleTextColorEnabled(it)
                                viewModel.triggerButtonHaptic()
                            }
                        )
                    }

                    val mainCircleTextColorEnabled by viewModel.mainCircleTextColorEnabled.collectAsStateWithLifecycle()
                    if (mainCircleTextColorEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val mainCircleTextColorType by viewModel.mainCircleTextColorType.collectAsStateWithLifecycle()
                            val textColorsOptions = if (appLanguage == "el") {
                                listOf("Κύκλος", "Πρωτεύον", "Δευτερεύον", "Τριτεύον", "RGB")
                            } else {
                                listOf("Circle", "Primary", "Secondary", "Tertiary", "RGB")
                            }

                            val selectedTextColorsIndex = when {
                                mainCircleTextColorType == "PRIMARY" -> 1
                                mainCircleTextColorType == "SECONDARY" -> 2
                                mainCircleTextColorType == "TERTIARY" -> 3
                                mainCircleTextColorType.startsWith("CUSTOM_RGB") -> 4
                                else -> 0
                            }

                            MultiWayToggle(
                                selectedIndex = selectedTextColorsIndex,
                                options = textColorsOptions,
                                onModeSelect = { idx ->
                                    val newType = when (idx) {
                                        1 -> "PRIMARY"
                                        2 -> "SECONDARY"
                                        3 -> "TERTIARY"
                                        4 -> "CUSTOM_RGB_FFFFFF"
                                        else -> "PROGRESS_COLOR"
                                    }
                                    viewModel.updateMainCircleTextColorType(newType)
                                },
                                triggerLightHaptic = { viewModel.triggerToggleLightHaptic() },
                                triggerSnapHaptic = { viewModel.triggerToggleSnapHaptic() }
                            )
                            
                            if (mainCircleTextColorType.startsWith("CUSTOM_RGB")) {
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                val currentTextHex = if (mainCircleTextColorType.startsWith("CUSTOM_RGB_")) {
                                    mainCircleTextColorType.removePrefix("CUSTOM_RGB_")
                                } else "FFFFFF"

                                val rTextVal = try { currentTextHex.substring(0, 2).toInt(16) } catch (e: Exception) { 255 }
                                val gTextVal = try { currentTextHex.substring(2, 4).toInt(16) } catch (e: Exception) { 255 }
                                val bTextVal = try { currentTextHex.substring(4, 6).toInt(16) } catch (e: Exception) { 255 }

                                val previewColor = remember(currentTextHex) {
                                    try { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor("#$currentTextHex")) } catch (e: Exception) { androidx.compose.ui.graphics.Color.White }
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (appLanguage == "el") "Χρώμα RGB" else "RGB Color",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(androidx.compose.foundation.shape.CircleShape)
                                                .background(previewColor)
                                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, androidx.compose.foundation.shape.CircleShape)
                                        )
                                        Text(
                                            text = "#$currentTextHex",
                                            fontSize = 13.sp,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Red Slider
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = if (appLanguage == "el") "Κόκκινο" else "Red", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = rTextVal.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    CapsulePatternSlider(
                                        value = rTextVal.toFloat(),
                                        onValueChange = { 
                                            val hex = String.format("%02X%02X%02X", it.toInt(), gTextVal, bTextVal)
                                            viewModel.updateMainCircleTextColorType("CUSTOM_RGB_$hex")
                                        },
                                        valueRange = 0f..255f,
                                        stepsCount = 256,
                                        triggerHaptic = { viewModel.triggerSliderHaptic() },
                                        activeColor = androidx.compose.ui.graphics.Color.Red,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                // Green Slider
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = if (appLanguage == "el") "Πράσινο" else "Green", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = gTextVal.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    CapsulePatternSlider(
                                        value = gTextVal.toFloat(),
                                        onValueChange = { 
                                            val hex = String.format("%02X%02X%02X", rTextVal, it.toInt(), bTextVal)
                                            viewModel.updateMainCircleTextColorType("CUSTOM_RGB_$hex")
                                        },
                                        valueRange = 0f..255f,
                                        stepsCount = 256,
                                        triggerHaptic = { viewModel.triggerSliderHaptic() },
                                        activeColor = androidx.compose.ui.graphics.Color.Green,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                // Blue Slider
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = if (appLanguage == "el") "Μπλε" else "Blue", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = bTextVal.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    CapsulePatternSlider(
                                        value = bTextVal.toFloat(),
                                        onValueChange = { 
                                            val hex = String.format("%02X%02X%02X", rTextVal, gTextVal, it.toInt())
                                            viewModel.updateMainCircleTextColorType("CUSTOM_RGB_$hex")
                                        },
                                        valueRange = 0f..255f,
                                        stepsCount = 256,
                                        triggerHaptic = { viewModel.triggerSliderHaptic() },
                                        activeColor = androidx.compose.ui.graphics.Color.Blue,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 1.15 SETTINGS DIVIDER STYLE
    item {
        val configProfile by viewModel.configProfile.collectAsStateWithLifecycle()
        val isNerdMode = configProfile == "NERD_MODE"
        if (isNerdMode) {
            Spacer(modifier = Modifier.height(12.dp))
            SettingsCategoryHeader(
                if (appLanguage == "el") "ΣΤΥΛ ΔΙΑΧΩΡΙΣΤΙΚΟΥ ΡΥΘΜΙΣΕΩΝ" else "SETTINGS DIVIDER STYLE",
                icon = Icons.Rounded.LineStyle
            )
            ChunkySettingCard {
                val currentDividerStyle by viewModel.settingsDividerStyle.collectAsStateWithLifecycle()
            
            Text(
                text = if (appLanguage == "el") "Επιλέξτε Στυλ Γραμμής" else "Select Line Style",
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (appLanguage == "el") "Αλλαγή του στυλ των διαχωριστικών γραμμών στις ρυθμίσεις." else "Change the style of the divider lines across settings.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val dividerOptions = listOf("STRAIGHT", "SQUIGGLY", "GAPS")
            val dividerLabels = if (appLanguage == "el") {
                listOf("Ευθεία Γραμμή", "Κυματιστή", "Κενά (Χωρίς Γραμμή)")
            } else {
                listOf("Straight", "Squiggly", "Gaps (No Line)")
            }

            val selectedIndex = dividerOptions.indexOf(currentDividerStyle).coerceAtLeast(0)

            MultiWayToggle(
                selectedIndex = selectedIndex,
                options = dividerLabels,
                onModeSelect = { index ->
                    viewModel.updateSettingsDividerStyle(dividerOptions[index])
                },
                triggerLightHaptic = { viewModel.triggerToggleLightHaptic() },
                triggerSnapHaptic = { viewModel.triggerToggleSnapHaptic() }
            )

            if (currentDividerStyle == "GAPS") {
                val gapScale by viewModel.settingsGapScale.collectAsStateWithLifecycle()
                HorizontalDivider(paddingVertical = 12.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Text(
                    text = if (appLanguage == "el") "Μέγεθος Κενού" else "Gap Size / Distance",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (appLanguage == "el") "Μικρό" else "Tiny",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = gapScale,
                        onValueChange = { viewModel.updateSettingsGapScale(it) },
                        valueRange = 0.05f..2.5f,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                    Text(
                        text = if (appLanguage == "el") "Μέγιστο" else "Huge",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

                val adaptiveGrouping by viewModel.settingsAdaptiveGrouping.collectAsStateWithLifecycle()
                HorizontalDivider(paddingVertical = 12.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (appLanguage == "el") "Προσαρμοστική Ομαδοποίηση Κουτιών" else "Adaptive Box Grouping",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (appLanguage == "el") "Μειώνει τις στρογγυλεμένες γωνίες στις πλευρές που τα κουτιά είναι κοντά, όπως στις ρυθμίσεις συστήματος." else "Reduces rounded corners on contiguous sides of adjacent boxes, matching system settings.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    ChunkySettingSwitch(
                        checked = adaptiveGrouping,
                        onCheckedChange = { 
                            viewModel.updateSettingsAdaptiveGrouping(it)
                        }
                    )
                }

                if (adaptiveGrouping) {
                    val adaptiveCornerRadius by viewModel.settingsAdaptiveCornerRadius.collectAsStateWithLifecycle()
                    HorizontalDivider(paddingVertical = 12.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                    Text(
                        text = if (appLanguage == "el") "Γωνίες Ομαδοποίησης" else "Contiguous Corner Radius",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "0",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        CapsulePatternSlider(
                            value = adaptiveCornerRadius.toFloat(),
                            onValueChange = { viewModel.updateSettingsAdaptiveCornerRadius(it.toInt()) },
                            valueRange = 0f..24f,
                            stepsCount = 25, // 0 to 24 (25 dots)
                            triggerHaptic = { viewModel.triggerSliderHaptic() },
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "24",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }

    // 1.16 GOAL LINE STYLE
    item {
        val configProfile by viewModel.configProfile.collectAsStateWithLifecycle()
        val isNerdMode = configProfile == "NERD_MODE"
        if (isNerdMode) {
            Spacer(modifier = Modifier.height(12.dp))
            SettingsCategoryHeader(
                if (appLanguage == "el") "ΣΤΥΛ ΓΡΑΜΜΗΣ ΣΤΟΧΟΥ" else "GOAL LINE STYLE",
                icon = Icons.Rounded.ShowChart
            )
            ChunkySettingCard {
                val goalLineSquiggly by viewModel.goalLineSquiggly.collectAsStateWithLifecycle()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (appLanguage == "el") "Κυματιστή Γραμμή Στόχου" else "Squiggly Goal Line",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (appLanguage == "el") "Κάνει τη γραμμή στόχου στο γράφημα κυματιστή." else "Makes the goal line on the tracking chart squiggly instead of smooth.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    ChunkySettingSwitch(
                        checked = goalLineSquiggly,
                        onCheckedChange = { 
                            viewModel.updateGoalLineSquiggly(it)
                        }
                    )
                }
            }
        }
    }

    // 2.5. NAVIGATION
    item {
        val configProfile by viewModel.configProfile.collectAsStateWithLifecycle()
        val isNerdMode = configProfile == "NERD_MODE"
        Spacer(modifier = Modifier.height(12.dp))
        SettingsCategoryHeader(if (appLanguage == "el") "ΠΛΟΗΓΗΣΗ" else "NAVIGATION", icon = Icons.Rounded.Swipe)
        ChunkySettingCard {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Navigation Bar Style (Thin vs Full)
                val navBarStyle by viewModel.navBarStyle.collectAsStateWithLifecycle()
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Rounded.Navigation,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = if (appLanguage == "el") "Στυλ Μπάρας Πλοήγησης" else "Navigation Bar Style",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (appLanguage == "el") {
                                        if (navBarStyle == "THIN") "Λεπτή αιωρούμενη νησίδα" else "Παχιά αιωρούμενη νησίδα"
                                    } else {
                                        if (navBarStyle == "THIN") "Thin floating island" else "Thick floating island"
                                    },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    MultiWayToggle(
                        selectedIndex = if (navBarStyle == "THIN") 0 else 1,
                        options = if (appLanguage == "el") listOf("Λεπτή", "Παχιά") else listOf("Thin", "Thick"),
                        onModeSelect = { index ->
                            val selected = if (index == 0) "THIN" else "THICK"
                            viewModel.updateNavBarStyle(selected)
                        },
                        triggerLightHaptic = { viewModel.triggerToggleLightHaptic() },
                        triggerSnapHaptic = { viewModel.triggerToggleSnapHaptic() }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // Horizontal Swipe Navigation at the very top!
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(imageVector = Icons.Rounded.SwapHoriz, contentDescription = null, tint = if (isSwipeTabNavEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(if (appLanguage == "el") "Οριζόντια Σάρωση" else "Horizontal Swipe Navigation", fontWeight = FontWeight.Bold)
                            Text(if (appLanguage == "el") "Σύρετε οριζόντια για να πλοηγηθείτε μεταξύ των καρτελών." else "Swipe horizontally to navigate between screens/tabs.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    ChunkySettingSwitch(
                        checked = isSwipeTabNavEnabled,
                        onCheckedChange = { viewModel.updateIsSwipeTabNavEnabled(it) }
                    )
                }

                if (isNerdMode) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    val separateSettingsTabEnabled by viewModel.separateSettingsTabEnabled.collectAsStateWithLifecycle()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(imageVector = Icons.Rounded.Settings, contentDescription = null, tint = if (separateSettingsTabEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(if (appLanguage == "el") "Ξεχωριστό Tab Ρυθμίσεων" else "Separate Settings Tab", fontWeight = FontWeight.Bold)
                                Text(if (appLanguage == "el") "Αποσπά την καρτέλα ρυθμίσεων σε ξεχωριστό κουμπί δίπλα από τη μπάρα πλοήγησης." else "Detaches the settings tab into a separate circular button next to the navigation bar.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        ChunkySettingSwitch(
                            checked = separateSettingsTabEnabled,
                            onCheckedChange = { 
                                viewModel.updateSeparateSettingsTabEnabled(it)
                                viewModel.triggerButtonHaptic()
                            }
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                val remindersInSettings by viewModel.remindersInSettings.collectAsStateWithLifecycle()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Rounded.Notifications,
                            contentDescription = null,
                            tint = if (remindersInSettings) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = if (appLanguage == "el") "Υπενθυμίσεις στις Ρυθμίσεις" else "Reminders in Settings",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (appLanguage == "el") {
                                    if (remindersInSettings) "Οι υπενθυμίσεις βρίσκονται ως παράθυρο στις ρυθμίσεις αντί για την κάτω μπάρα." else "Οι υπενθυμίσεις εμφανίζονται στην κάτω μπάρα πλοήγησης."
                                } else {
                                    if (remindersInSettings) "Reminders tab is located in Settings instead of the bottom navigation bar." else "Reminders tab is displayed in the bottom navigation bar."
                                },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    ChunkySettingSwitch(
                        checked = remindersInSettings,
                        onCheckedChange = { 
                            viewModel.updateRemindersInSettings(it)
                            viewModel.triggerButtonHaptic()
                        }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                val daySwipeNavigationEnabled by viewModel.daySwipeNavigationEnabled.collectAsStateWithLifecycle()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(imageVector = Icons.Rounded.Swipe, contentDescription = null, tint = if (daySwipeNavigationEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(if (appLanguage == "el") "Χειρονομίες Σάρωσης" else "Swipe Gestures", fontWeight = FontWeight.Bold)
                            Text(if (appLanguage == "el") "Χρήση σαρώσεων αντί για κουμπιά για πλοήγηση ημερών." else "Use swipe gestures instead of arrows for day navigation.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    ChunkySettingSwitch(
                        checked = daySwipeNavigationEnabled,
                        onCheckedChange = { viewModel.updateDaySwipeNavigationEnabled(it) }
                    )
                }
                
                if (isNerdMode) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Column {
                        val currentPadding = viewModel.dayNavBarPadding.collectAsStateWithLifecycle().value
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(imageVector = Icons.Rounded.LinearScale, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(if (appLanguage == "el") "Μήκος Μπάρας Ημερομηνίας" else "Date Bar Length", fontWeight = FontWeight.Bold)
                                    Text(if (appLanguage == "el") "Ορίστε το οριζόντιο μήκος (padding) της μπάρας πλοήγησης." else "Adjust the horizontal length (padding) of the date bar.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        CapsulePatternSlider(
                            value = currentPadding.toFloat(),
                            onValueChange = { viewModel.updateDayNavBarPadding(it.toInt()) },
                            valueRange = 0f..80f,
                            stepsCount = 17, // 0 to 80 (17 dots)
                            triggerHaptic = { viewModel.triggerSliderHaptic() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val colorModeOption = viewModel.dayNavBarColorMode.collectAsStateWithLifecycle().value
                        val colorModes = listOf("OFF_COLOR", "PRIMARY", "TERTIARY")
                        val colorLabels = if (appLanguage == "el") {
                            listOf("Απαλό / Off-Color", "Κύριο Θέμα", "Θέμα Τόνου")
                        } else {
                            listOf("Off-Color (Subtle)", "Primary Theme", "Accent Theme")
                        }
                        val selectedColorIndex = colorModes.indexOf(colorModeOption).coerceAtLeast(0)

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(imageVector = Icons.Rounded.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(if (appLanguage == "el") "Χρώμα Πλαισίων Ημερομηνίας" else "Date Box Coloring", fontWeight = FontWeight.Bold)
                                    Text(if (appLanguage == "el") "Επιλέξτε αν τα πλαίσια της ημερομηνίας & των βελών θα είναι απαλά ή θα ακολουθούν το κύριο χρώμα του θέματος." else "Select if date & arrow boxes are subtle (off-color) or follow the primary app/palette color.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        MultiWayToggle(
                            selectedIndex = selectedColorIndex,
                            options = colorLabels,
                            onModeSelect = { index ->
                                viewModel.updateDayNavBarColorMode(colorModes[index])
                            },
                            triggerLightHaptic = { viewModel.triggerToggleLightHaptic() },
                            triggerSnapHaptic = { viewModel.triggerToggleSnapHaptic() }
                        )
                    }
                }
            }
        }
    }

    // DAY ROLL TIMING
    item {
        val lateNightLoggingEnabled by viewModel.lateNightLoggingEnabled.collectAsStateWithLifecycle()
        val lateNightRolloverHour by viewModel.lateNightRolloverHour.collectAsStateWithLifecycle()

        Spacer(modifier = Modifier.height(12.dp))
        SettingsCategoryHeader(
            title = if (appLanguage == "el") "ΜΕΤΑΒΑΣΗ ΗΜΕΡΑΣ" else "DAY ROLL TIMING",
            icon = Icons.Rounded.Nightlight
        )
        ChunkySettingCard {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Rounded.Nightlight,
                            contentDescription = null,
                            tint = Color(0xFF673AB7)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                if (appLanguage == "el") "Παράταση μετά τα Μεσάνυχτα" else "Late-Night Grace Period",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (appLanguage == "el") {
                                    "Διατηρεί την προηγούμενη ημέρα ενεργή μετά τα μεσάνυχτα για σωστή καταγραφή."
                                } else {
                                    "Keep tracking on yesterday's date after midnight so late logs count properly."
                                },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    ChunkySettingSwitch(
                        checked = lateNightLoggingEnabled,
                        onCheckedChange = { 
                            viewModel.updateLateNightLoggingEnabled(it) 
                            viewModel.triggerButtonHaptic()
                        }
                    )
                }

                if (lateNightLoggingEnabled) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (appLanguage == "el") "Ώρα Μετάβασης" else "Rollover Hour",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                if (appLanguage == "el") {
                                    "Η νέα ημέρα θα ξεκινήσει στις $lateNightRolloverHour:00 π.μ."
                                } else {
                                    "Today's cycle will begin at $lateNightRolloverHour:00 AM"
                                },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Decrement button
                            androidx.compose.material3.IconButton(
                                onClick = {
                                    if (lateNightRolloverHour > 1) {
                                        viewModel.updateLateNightRolloverHour(lateNightRolloverHour - 1)
                                        viewModel.triggerToggleHaptic()
                                    }
                                },
                                enabled = lateNightRolloverHour > 1
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Remove,
                                    contentDescription = "Decrease",
                                    tint = if (lateNightRolloverHour > 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            }

                            Text(
                                text = "$lateNightRolloverHour:00",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Increment button
                            androidx.compose.material3.IconButton(
                                onClick = {
                                    if (lateNightRolloverHour < 8) {
                                        viewModel.updateLateNightRolloverHour(lateNightRolloverHour + 1)
                                        viewModel.triggerToggleHaptic()
                                    }
                                },
                                enabled = lateNightRolloverHour < 8
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Increase",
                                    tint = if (lateNightRolloverHour < 8) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 3. BLUR & TRANSPARENCY EFFECTS
    item {
        val configProfile by viewModel.configProfile.collectAsStateWithLifecycle()
        val isNerdMode = configProfile == "NERD_MODE"
        if (isNerdMode || isFrostedGlassEnabled) {

        Spacer(modifier = Modifier.height(12.dp))
        SettingsCategoryHeader(if (appLanguage == "el") "ΘΟΛΩΣΗ & ΔΙΑΦΑΝΕΙΑ" else "BLUR & TRANSPARENCY EFFECTS", icon = Icons.Rounded.BlurOn)
        ChunkySettingCard {
            if (isFrostedGlassEnabled) {
                Column {
                    Text(
                        text = if (appLanguage == "el") "Διαφάνεια Θέματος Glass" else "Glass Theme Transparency",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Slider(
                        value = frostedGlassTransparency,
                        onValueChange = { viewModel.updateFrostedGlassTransparency(it) },
                        valueRange = 0f..1f,
                        modifier = Modifier.padding(horizontal = 8.dp).let { if (transparentComponentsEnabled) it.alpha(componentsTransparency) else it }
                    )
                }
                HorizontalDivider(paddingVertical = 16.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                
                if (isNerdMode) {
                    val isBlurEffectEnabled by viewModel.isBlurEffectEnabled.collectAsStateWithLifecycle()
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (appLanguage == "el") "Εφέ Θολώματος (Gaussian Blur)" else "Gaussian Blur",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (appLanguage == "el") "Ομοιόμορφη θόλωση στα διάφανα κουτιά" else "Even secondary blur on transparent element boxes",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            ChunkySettingSwitch(
                                checked = isBlurEffectEnabled,
                                onCheckedChange = {
                                    viewModel.updateBlurEffectEnabled(it)
                                    viewModel.triggerButtonHaptic()
                                }
                            )
                        }
                    }
                    HorizontalDivider(paddingVertical = 16.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                }
            }

            if (isNerdMode) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (appLanguage == "el") "Διαφανή Στοιχεία" else "Transparent Components",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        ChunkySettingSwitch(
                            checked = transparentComponentsEnabled,
                            onCheckedChange = {
                                viewModel.updateTransparentComponentsEnabled(it)
                                viewModel.triggerButtonHaptic()
                            }
                        )
                    }

                    if (transparentComponentsEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (appLanguage == "el") "Ένταση Διαφάνειας Στοιχείων" else "Components Transparency",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Slider(
                            value = componentsTransparency,
                            onValueChange = { viewModel.updateComponentsTransparency(it) },
                            valueRange = 0.1f..1f,
                            modifier = Modifier.padding(horizontal = 8.dp).alpha(componentsTransparency)
                        )
                    }
                }
            }
        }
        }
    }

    // 4. COMPONENT MODIFIERS
    item {
        val isNerdMode = viewModel.configProfile.collectAsStateWithLifecycle().value == "NERD_MODE"
        if (isNerdMode) {
            Spacer(modifier = Modifier.height(12.dp))
            SettingsCategoryHeader(if (appLanguage == "el") "ΤΡΟΠΟΠΟΙΗΣΕΙΣ" else "COMPONENT MODIFIERS", icon = Icons.Rounded.Build)
            ChunkySettingCard {

            Row(
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToCornerRadius() }.padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Adjust Corner Radius",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        }
    }

    // 5. HIGH CONTRAST
    item {
        val isNerdMode = viewModel.configProfile.collectAsStateWithLifecycle().value == "NERD_MODE"
        if (isNerdMode) {
        Spacer(modifier = Modifier.height(12.dp))
        SettingsCategoryHeader(if (appLanguage == "el") "ΥΨΗΛΗ ΑΝΤΙΘΕΣΗ" else "HIGH CONTRAST MODE", icon = Icons.Rounded.Contrast)
        ChunkySettingCard {
            val isSystemDark = isSystemInDarkTheme()
            val autoThemeScheduleEnabled by viewModel.autoThemeScheduleEnabled.collectAsStateWithLifecycle()
            val autoThemeScheduleMode by viewModel.autoThemeScheduleMode.collectAsStateWithLifecycle()
            val autoThemeLightStartHour by viewModel.autoThemeLightStartHour.collectAsStateWithLifecycle()
            val autoThemeLightStartMin by viewModel.autoThemeLightStartMin.collectAsStateWithLifecycle()
            val autoThemeDarkStartHour by viewModel.autoThemeDarkStartHour.collectAsStateWithLifecycle()
            val autoThemeDarkStartMin by viewModel.autoThemeDarkStartMin.collectAsStateWithLifecycle()

            val isActiveDark = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
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
                        isSystemDark
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "OLED Pure Black Background", fontWeight = FontWeight.Bold, color = if (isActiveDark) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha=0.4f))
                }
                ChunkySettingSwitch(
                    checked = oledModeEnabled && isActiveDark,
                    onCheckedChange = { viewModel.updateOledMode(it) },
                    enabled = isActiveDark
                )
            }

            HorizontalDivider(paddingVertical = 12.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Black Text (Light Mode)", fontWeight = FontWeight.Bold, color = if (!isActiveDark) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha=0.4f))
                }
                ChunkySettingSwitch(
                    checked = lightModeDarkTextEnabled && !isActiveDark,
                    onCheckedChange = { viewModel.updateLightModeDarkTextEnabled(it) },
                    enabled = !isActiveDark
                )
            }

            HorizontalDivider(paddingVertical = 12.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            val vividLightBoxesEnabled by viewModel.vividLightBoxesEnabled.collectAsStateWithLifecycle()
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (appLanguage == "el") "Έγχρωμα Πλαίσια στο Φωτεινό Θέμα" else "Colored Boxes in Light Mode",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (appLanguage == "el") "Χρήση ζωντανών χρωμάτων στα πλαίσια καρτών" else "Use vivid accent container colors for cards in light mode",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ChunkySettingSwitch(
                    checked = vividLightBoxesEnabled,
                    onCheckedChange = { viewModel.updateVividLightBoxesEnabled(it) }
                )
            }

            HorizontalDivider(paddingVertical = 12.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            val keepLightBackgroundWhite by viewModel.keepLightBackgroundWhite.collectAsStateWithLifecycle()
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (appLanguage == "el") "Καθαρό Λευκό Φόντο (Φωτεινό Θέμα)" else "White Background (Light Mode)",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (appLanguage == "el") "Διατήρηση του φόντου πίσω από τα σχήματα σε καθαρό λευκό" else "Keep canvas background behind animated shapes pure white instead of accent tinted",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ChunkySettingSwitch(
                    checked = keepLightBackgroundWhite,
                    onCheckedChange = { viewModel.updateKeepLightBackgroundWhite(it) }
                )
            }
        }
        }
    }

        item {

            val configProfile by viewModel.configProfile.collectAsStateWithLifecycle()
        val isNerdMode = configProfile == "NERD_MODE"

        Spacer(modifier = Modifier.height(12.dp))
        SettingsCategoryHeader(if (appLanguage == "el") "ΑΥΡΑ & ΣΧΗΜΑΤΑ" else "MATERIAL SHAPES & AURA GLOW", icon = Icons.Rounded.Category)
        ChunkySettingCard {
            val materialShapesEnabled by viewModel.materialShapesEnabled.collectAsStateWithLifecycle()
            val auraGlowEnabled by viewModel.auraGlowEnabled.collectAsStateWithLifecycle()
            val shapeRotationEnabled by viewModel.shapeRotationEnabled.collectAsStateWithLifecycle()
            val backgroundContrast by viewModel.backgroundContrast.collectAsStateWithLifecycle()

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Background Shapes", fontWeight = FontWeight.Bold)
                ChunkySettingSwitch(checked = materialShapesEnabled, onCheckedChange = { viewModel.updateMaterialShapesEnabled(it) })
            }
            HorizontalDivider(paddingVertical = 12.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Aura Glow Background", fontWeight = FontWeight.Bold)
                ChunkySettingSwitch(checked = auraGlowEnabled, onCheckedChange = { viewModel.updateAuraGlowEnabled(it) })
            }

            if (isNerdMode) {
                // Background Contrast Slider
                HorizontalDivider(paddingVertical = 12.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (appLanguage == "el") "Ένταση Αντίθεσης Υποβάθρου" else "Background Contrast Intensity", fontWeight = FontWeight.Bold)
                        Text("${String.format("%.2f", backgroundContrast)}x", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    }
                    CapsulePatternSlider(
                        value = backgroundContrast,
                        onValueChange = { viewModel.updateBackgroundContrast(it) },
                        valueRange = 0.0f..3.0f,
                        triggerHaptic = { viewModel.triggerSliderHaptic() }
                    )

                    // Fast Quick Presets (Middle Points) Row
                    Text(
                        text = if (appLanguage == "el") "Γρήγορες Επιλογές / Επίπεδα" else "Quick Preset Intensities (Middle Points)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val presets = remember { (0..30).map { it * 0.1f } }
                        val nearestPreset = remember(backgroundContrast) {
                            presets.minByOrNull { kotlin.math.abs(backgroundContrast - it) } ?: 1.0f
                        }
                        for (presetVal in presets) {
                            val isSelected = kotlin.math.abs(nearestPreset - presetVal) < 0.01f
                            val label = "${String.format("%.2f", presetVal)}x"

                            Box(
                                modifier = Modifier
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .clickable {
                                        viewModel.updateBackgroundContrast(presetVal)
                                        viewModel.triggerSliderHaptic()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // --- COLOR SATURATION SLIDER ---
                HorizontalDivider(paddingVertical = 12.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val shapeSaturation by viewModel.shapeSaturation.collectAsStateWithLifecycle()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (appLanguage == "el") "Κορεσμός Χρωμάτων" else "Shape & Aura Color Saturation", fontWeight = FontWeight.Bold)
                        Text("${(shapeSaturation * 100).toInt()}%", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    }
                    Text(
                        text = if (appLanguage == "el") "Από 0% (Ασπρόμαυρο) έως 100% (Πλήρως Έντονο)" else "From 0% (Grayscale) to 100% (Fully Vivid)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    CapsulePatternSlider(
                        value = shapeSaturation,
                        onValueChange = { viewModel.updateShapeSaturation(it) },
                        valueRange = 0.0f..1.0f,
                        triggerHaptic = { viewModel.triggerSliderHaptic() }
                    )
                }

                // --- INDEPENDENT MONOCHROME / MULTICOLOR ROLE TOGGLE ---
                HorizontalDivider(paddingVertical = 12.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                val shapeMonochromeEnabled by viewModel.shapeMonochromeEnabled.collectAsStateWithLifecycle()
                val shapeMonochromeSource by viewModel.shapeMonochromeSource.collectAsStateWithLifecycle()

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (appLanguage == "el") "Ανεξάρτητος Ρόλος Χρώματος" else "Independent Color Palette Role",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val options = listOf(
                            Triple(false, 0, if (appLanguage == "el") "Πολύχρωμο" else "Multicolor"),
                            Triple(true, 0, if (appLanguage == "el") "Πρωτεύον" else "Primary"),
                            Triple(true, 1, if (appLanguage == "el") "Δευτερεύον" else "Secondary"),
                            Triple(true, 2, if (appLanguage == "el") "Τριτεύον" else "Tertiary")
                        )
                        
                        options.forEach { (mono, src, label) ->
                            val isSelected = if (mono) {
                                shapeMonochromeEnabled && shapeMonochromeSource == src
                            } else {
                                !shapeMonochromeEnabled
                            }
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        viewModel.updateShapeMonochromeEnabled(mono)
                                        if (mono) {
                                            viewModel.updateShapeMonochromeSource(src)
                                        }
                                        viewModel.triggerButtonHaptic()
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // --- INDEPENDENT WALLPAPER PALETTE SELECTION ---
                HorizontalDivider(paddingVertical = 12.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val shapeUseIndependentDynamicPalette by viewModel.shapeUseIndependentDynamicPalette.collectAsStateWithLifecycle()
                    val shapeDynamicPaletteIndex by viewModel.shapeDynamicPaletteIndex.collectAsStateWithLifecycle()
                    val wallpaperColors by viewModel.wallpaperThemeColors.collectAsStateWithLifecycle()
                    val shapePaletteShiftEnabled by viewModel.shapePaletteShiftEnabled.collectAsStateWithLifecycle()

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (appLanguage == "el") "Διαφοροποίηση Φάσματος Σχημάτων" else "Shape Palette Spectrum Shift", fontWeight = FontWeight.Bold)
                            Text(
                                text = if (appLanguage == "el") "Εφαρμόζει ελαφρώς διαφορετικές αποχρώσεις μεταξύ κάθε σχήματος για πλουσιότερη διαστρωμάτωση" else "Apply slightly varied hues and tones between background shapes & aura elements",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        ChunkySettingSwitch(checked = shapePaletteShiftEnabled, onCheckedChange = { viewModel.updateShapePaletteShiftEnabled(it); viewModel.triggerButtonHaptic() })
                    }

                    HorizontalDivider(paddingVertical = 4.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (appLanguage == "el") "Ανεξάρτητη Παλέτα Ταπετσαρίας" else "Independent Wallpaper Palette", fontWeight = FontWeight.Bold)
                            Text(
                                text = if (appLanguage == "el") "Επιτρέπει στα σχήματα να έχουν διαφορετική παλέτα από την εφαρμογή" else "Allows shapes to display a different palette than the app UI",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        ChunkySettingSwitch(checked = shapeUseIndependentDynamicPalette, onCheckedChange = { viewModel.updateShapeUseIndependentDynamicPalette(it); viewModel.triggerButtonHaptic() })
                    }

                    if (shapeUseIndependentDynamicPalette) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (appLanguage == "el") "Επιλέξτε Παλέτα για τα Σχήματα & την Αύρα:" else "Select Shape & Aura Color Palette Seed:",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            wallpaperColors.forEachIndexed { idx, colorInt ->
                                val isSelected = shapeDynamicPaletteIndex == idx
                                val (topColor, bottomLeftColor, bottomRightColor) = remember(colorInt) { viewModel.getSwatchColors(colorInt) }

                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .padding(2.dp)
                                        .then(
                                            if (isSelected) {
                                                Modifier.border(
                                                    width = 3.dp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = CircleShape
                                                )
                                            } else Modifier
                                        )
                                        .padding(if (isSelected) 4.dp else 0.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            viewModel.updateShapeDynamicPaletteIndex(idx)
                                            viewModel.triggerButtonHaptic()
                                        }
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawArc(color = topColor, startAngle = 180f, sweepAngle = 180f, useCenter = true)
                                        drawArc(color = bottomLeftColor, startAngle = 90f, sweepAngle = 90f, useCenter = true)
                                        drawArc(color = bottomRightColor, startAngle = 0f, sweepAngle = 90f, useCenter = true)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (isNerdMode) {
            Spacer(modifier = Modifier.height(16.dp))
            IndependentShapeSpeedsCard(viewModel = viewModel, appLanguage = appLanguage)
            
            Spacer(modifier = Modifier.height(16.dp))
            InteractiveSpinCanvasCard(viewModel = viewModel, appLanguage = appLanguage, onScrollLockChange = onScrollLockChange)
        }
    }


    // 7. APP ANIMATION PHYSICS
    item {
        val configProfileAnimPhysics by viewModel.configProfile.collectAsStateWithLifecycle()
        if (configProfileAnimPhysics == "NERD_MODE") {
            val confettiStyle by viewModel.confettiStyle.collectAsStateWithLifecycle()
            Spacer(modifier = Modifier.height(12.dp))
            SettingsCategoryHeader(if (appLanguage == "el") "ΦΥΣΙΚΗ & ΚΙΝΗΣΗ" else "APP ANIMATION PHYSICS", icon = Icons.Rounded.Animation)
            ChunkySettingCard {
            
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (appLanguage == "el") "Κίνηση Μετάβασης" else "Tab Transition Animation",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                ThreeWayAnimToggle(
                    currentMode = tabTransitionMode,
                    appLanguage = appLanguage,
                    onModeSelect = { viewModel.updateTabTransitionMode(it) },
                    triggerLightHaptic = { viewModel.triggerToggleLightHaptic() },
                    triggerSnapHaptic = { viewModel.triggerToggleSnapHaptic() }
                )
            }

            HorizontalDivider(paddingVertical = 16.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Rounded.Celebration, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (appLanguage == "el") "Στυλ Εφέ & Κίνησης" else "Confetti & Effect Style",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                val labels = if (appLanguage == "el") {
                    listOf("Μικτό", "Confetti", "Καρδιές", "Αστέρια", "Σταγόνες", "Πλάτανος", "Φύλλο", "Υδατική Έκρηξη")
                } else {
                    listOf("Mixed", "Confetti", "Hearts", "Stars", "Drops", "Plane Leaf", "Regular Leaf", "Hydration Splash")
                }
                ConfettiStyleToggle(
                    currentMode = confettiStyle,
                    labels = labels,
                    onModeSelect = { viewModel.updateConfettiStyle(it) },
                    triggerLightHaptic = { viewModel.triggerScrollHaptic() },
                    triggerSnapHaptic = { viewModel.triggerSliderHaptic() }
                )
                if (confettiStyle == 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (appLanguage == "el") "Επιλέξτε σχήματα για το Μικτό στυλ:" else "Select shapes for Mixed style:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                    )
                    
                    val customMixRect by viewModel.customMixRectangles.collectAsStateWithLifecycle()
                    val customMixHearts by viewModel.customMixHearts.collectAsStateWithLifecycle()
                    val customMixStars by viewModel.customMixStars.collectAsStateWithLifecycle()
                    val customMixDrops by viewModel.customMixDrops.collectAsStateWithLifecycle()
                    val customMixPlaneTree by viewModel.customMixPlaneTreeLeaves.collectAsStateWithLifecycle()
                    val customMixRegularLeaf by viewModel.customMixRegularLeaves.collectAsStateWithLifecycle()

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MixFilterChip(
                                label = if (appLanguage == "el") "Confetti" else "Confetti",
                                selected = customMixRect,
                                icon = Icons.Rounded.Category,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    viewModel.triggerButtonHaptic()
                                    viewModel.updateCustomMix(!customMixRect, customMixHearts, customMixStars, customMixDrops, customMixPlaneTree, customMixRegularLeaf)
                                }
                            )
                            MixFilterChip(
                                label = if (appLanguage == "el") "Καρδιές" else "Hearts",
                                selected = customMixHearts,
                                icon = Icons.Rounded.Favorite,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    viewModel.triggerButtonHaptic()
                                    viewModel.updateCustomMix(customMixRect, !customMixHearts, customMixStars, customMixDrops, customMixPlaneTree, customMixRegularLeaf)
                                }
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MixFilterChip(
                                label = if (appLanguage == "el") "Αστέρια" else "Stars",
                                selected = customMixStars,
                                icon = Icons.Rounded.Star,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    viewModel.triggerButtonHaptic()
                                    viewModel.updateCustomMix(customMixRect, customMixHearts, !customMixStars, customMixDrops, customMixPlaneTree, customMixRegularLeaf)
                                }
                            )
                            MixFilterChip(
                                label = if (appLanguage == "el") "Σταγόνες" else "Drops",
                                selected = customMixDrops,
                                icon = Icons.Rounded.Opacity,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    viewModel.triggerButtonHaptic()
                                    viewModel.updateCustomMix(customMixRect, customMixHearts, customMixStars, !customMixDrops, customMixPlaneTree, customMixRegularLeaf)
                                }
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MixFilterChip(
                                label = if (appLanguage == "el") "Πλάτανος" else "Plane Leaf",
                                selected = customMixPlaneTree,
                                icon = Icons.Rounded.Eco,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    viewModel.triggerButtonHaptic()
                                    viewModel.updateCustomMix(customMixRect, customMixHearts, customMixStars, customMixDrops, !customMixPlaneTree, customMixRegularLeaf)
                                }
                            )
                            MixFilterChip(
                                label = if (appLanguage == "el") "Φύλλο" else "Regular Leaf",
                                selected = customMixRegularLeaf,
                                icon = Icons.Rounded.Spa,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    viewModel.triggerButtonHaptic()
                                    viewModel.updateCustomMix(customMixRect, customMixHearts, customMixStars, customMixDrops, customMixPlaneTree, !customMixRegularLeaf)
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(paddingVertical = 16.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Rounded.Animation, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (appLanguage == "el") "Κίνηση Ροής Confetti" else "Confetti Flow Style",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                val confettiFlowStyle by viewModel.confettiFlowStyle.collectAsStateWithLifecycle()
                val flowLabels = if (appLanguage == "el") {
                    listOf("Κλασικός", "Σιντριβάνι", "Zen Ροή", "Δίνη", "Άνοδος", "Άνεμος")
                } else {
                    listOf("Classic", "Fountain", "Zen Drift", "Vortex", "Ascending", "Crosswind")
                }
                ConfettiStyleToggle(
                    currentMode = confettiFlowStyle,
                    labels = flowLabels,
                    onModeSelect = { viewModel.updateConfettiFlowStyle(it) },
                    triggerLightHaptic = { viewModel.triggerScrollHaptic() },
                    triggerSnapHaptic = { viewModel.triggerSliderHaptic() }
                )
            }

            HorizontalDivider(paddingVertical = 16.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Rounded.Grain, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (appLanguage == "el") "Ποσότητα Σωματιδίων" else "High Performance Particles",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                val particleCount by viewModel.particleCount.collectAsStateWithLifecycle()
                ParticleCountToggle(
                    currentValue = particleCount,
                    options = listOf(400, 600, 800, 1000, 1200, 1500, 2000, 3000, 5000),
                    onValueSelect = { viewModel.updateParticleCount(it) },
                    triggerLightHaptic = { viewModel.triggerScrollHaptic() },
                    triggerSnapHaptic = { viewModel.triggerSliderHaptic() }
                )
                Spacer(modifier = Modifier.height(12.dp))
                androidx.compose.material3.Button(
                    onClick = {
                        viewModel.triggerButtonHaptic()
                        onTriggerConfetti()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("test_confetti_button"),
                    shape = RoundedCornerShape(24.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Celebration,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (appLanguage == "el") "Δοκιμή Εφέ" else "Test Animation",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(paddingVertical = 16.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            var isPhysicsExpanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { 
                        viewModel.triggerToggleLightHaptic()
                        isPhysicsExpanded = !isPhysicsExpanded 
                    }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Animation,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (appLanguage == "el") "Προσαρμοσμένες Κινήσεις Μετάβασης" else "Custom Transmission Animations",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = if (isPhysicsExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = if (isPhysicsExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = isPhysicsExpanded,
                enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    when (tabTransitionMode) {
                        0 -> {
                            // SHOW ONLY SLIDE SLIDERS IN SLIDING MODE
                            Column {
                                PhysicsSliderRow(
                                    title = if (appLanguage == "el") "Σκληρότητα Ολίσθησης" else "Slide Stiffness",
                                    value = slideStiffness,
                                    defaultValue = 300f,
                                    valueRange = 10f..800f,
                                    onValueChange = { viewModel.updateSlideStiffness(it) },
                                    triggerHaptic = { viewModel.triggerButtonHaptic() }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                PhysicsSliderRow(
                                    title = if (appLanguage == "el") "Απόσβεση Ολίσθησης" else "Slide Damping",
                                    value = slideDamping,
                                    defaultValue = 1.0f,
                                    valueRange = 0.1f..2f,
                                    onValueChange = { viewModel.updateSlideDamping(it) },
                                    triggerHaptic = { viewModel.triggerButtonHaptic() }
                                )
                            }
                        }
                        1 -> {
                            // SHOW ONLY BOUNCE SLIDERS IN BOUNCING MODE
                            Column {
                                PhysicsSliderRow(
                                    title = if (appLanguage == "el") "Σκληρότητα Αναπήδησης" else "Bounce Stiffness",
                                    value = bounceStiffness,
                                    defaultValue = 300f,
                                    valueRange = 10f..800f,
                                    onValueChange = { viewModel.updateBounceStiffness(it) },
                                    triggerHaptic = { viewModel.triggerButtonHaptic() }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                PhysicsSliderRow(
                                    title = if (appLanguage == "el") "Απόσβεση Αναπήδησης" else "Bounce Damping",
                                    value = bounceDamping,
                                    defaultValue = 0.55f,
                                    valueRange = 0.1f..2f,
                                    onValueChange = { viewModel.updateBounceDamping(it) },
                                    triggerHaptic = { viewModel.triggerButtonHaptic() }
                                )
                            }
                        }
                        else -> {
                            // SHOW ONLY FADE SLIDERS IN FADING MODE
                            Column {
                                PhysicsSliderRow(
                                    title = if (appLanguage == "el") "Σκληρότητα Σβησίματος" else "Fade Stiffness",
                                    value = fadeStiffness,
                                    defaultValue = 300f,
                                    valueRange = 10f..800f,
                                    onValueChange = { viewModel.updateFadeStiffness(it) },
                                    triggerHaptic = { viewModel.triggerButtonHaptic() }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                PhysicsSliderRow(
                                    title = if (appLanguage == "el") "Απόσβεση Σβησίματος" else "Fade Damping",
                                    value = fadeDamping,
                                    defaultValue = 1.0f,
                                    valueRange = 0.1f..2f,
                                    onValueChange = { viewModel.updateFadeDamping(it) },
                                    triggerHaptic = { viewModel.triggerButtonHaptic() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
fun PhysicsSliderRow(
    title: String,
    value: Float,
    defaultValue: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    triggerHaptic: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()
    // Show reset button if not currently dragging and value differs from default
    val showReset = !isDragged && kotlin.math.abs(value - defaultValue) > 0.001f

    Column {
        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                interactionSource = interactionSource,
                modifier = Modifier.weight(1f)
            )
            AnimatedVisibility(
                visible = showReset,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                IconButton(
                    onClick = { 
                        onValueChange(defaultValue)
                        triggerHaptic()
                    },
                    modifier = Modifier.padding(start = 8.dp).size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Restore, 
                        contentDescription = "Reset",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun AnimationPhysicsSandbox(
    tabTransitionMode: Int,
    bounceStiffness: Float,
    bounceDamping: Float,
    fadeStiffness: Float,
    fadeDamping: Float,
    slideStiffness: Float,
    slideDamping: Float,
    appLanguage: String,
    triggerHaptic: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    
    // States for animations
    val bounceX = remember { androidx.compose.animation.core.Animatable(0f) }
    val fadeAlpha = remember { androidx.compose.animation.core.Animatable(1f) }
    val slideX = remember { androidx.compose.animation.core.Animatable(0f) }

    // Jobs to cancel previous runs to prevent fighting coroutines (fixes jank/flicker)
    val bounceJob = remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val fadeJob = remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val slideJob = remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    fun playBounce() {
        bounceJob.value?.cancel()
        bounceJob.value = coroutineScope.launch {
            triggerHaptic()
            try {
                // Bounce horizontally with spring bounce
                bounceX.animateTo(
                    targetValue = 60f,
                    animationSpec = androidx.compose.animation.core.spring(
                        stiffness = bounceStiffness,
                        dampingRatio = bounceDamping
                    )
                )
                bounceX.animateTo(
                    targetValue = 0f,
                    animationSpec = androidx.compose.animation.core.spring(
                        stiffness = bounceStiffness,
                        dampingRatio = bounceDamping
                    )
                )
            } catch (e: Exception) {
                bounceX.snapTo(0f)
            }
        }
    }

    fun playFade() {
        fadeJob.value?.cancel()
        fadeJob.value = coroutineScope.launch {
            triggerHaptic()
            try {
                val duration = (120000f / fadeStiffness).coerceIn(100f, 1200f).toInt()
                val fadeOutDuration = (duration * 0.45f).toInt().coerceAtLeast(50)
                val fadeInDuration = (duration * 0.55f).toInt().coerceAtLeast(50)
                val fadeInDelay = fadeOutDuration + 30
                
                // Pure vertical alpha fade-out
                fadeAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = fadeOutDuration,
                        easing = androidx.compose.animation.core.FastOutLinearInEasing
                    )
                )
                kotlinx.coroutines.delay(fadeInDelay.toLong())
                // Smooth fade-in
                fadeAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = fadeInDuration,
                        easing = androidx.compose.animation.core.LinearOutSlowInEasing
                    )
                )
            } catch (e: Exception) {
                fadeAlpha.snapTo(1f)
            }
        }
    }

    fun playSlide() {
        slideJob.value?.cancel()
        slideJob.value = coroutineScope.launch {
            triggerHaptic()
            try {
                // Slide horizontally, then spring back to 0
                slideX.animateTo(
                    targetValue = 60f,
                    animationSpec = androidx.compose.animation.core.spring(
                        stiffness = slideStiffness,
                        dampingRatio = slideDamping
                    )
                )
                slideX.animateTo(
                    targetValue = 0f,
                    animationSpec = androidx.compose.animation.core.spring(
                        stiffness = slideStiffness,
                        dampingRatio = slideDamping
                    )
                )
            } catch (e: Exception) {
                slideX.snapTo(0f)
            }
        }
    }

    // State trackers to trigger live previews on parameter changes
    var prevBounceStiffness by remember { mutableStateOf(bounceStiffness) }
    var prevBounceDamping by remember { mutableStateOf(bounceDamping) }
    var prevFadeStiffness by remember { mutableStateOf(fadeStiffness) }
    var prevFadeDamping by remember { mutableStateOf(fadeDamping) }
    var prevSlideStiffness by remember { mutableStateOf(slideStiffness) }
    var prevSlideDamping by remember { mutableStateOf(slideDamping) }

    LaunchedEffect(bounceStiffness, bounceDamping) {
        if (tabTransitionMode == 1 && (bounceStiffness != prevBounceStiffness || bounceDamping != prevBounceDamping)) {
            prevBounceStiffness = bounceStiffness
            prevBounceDamping = bounceDamping
            kotlinx.coroutines.delay(180)
            playBounce()
        }
    }

    LaunchedEffect(fadeStiffness, fadeDamping) {
        if (tabTransitionMode == 2 && (fadeStiffness != prevFadeStiffness || fadeDamping != prevFadeDamping)) {
            prevFadeStiffness = fadeStiffness
            prevFadeDamping = fadeDamping
            kotlinx.coroutines.delay(180)
            playFade()
        }
    }

    LaunchedEffect(slideStiffness, slideDamping) {
        if (tabTransitionMode == 0 && (slideStiffness != prevSlideStiffness || slideDamping != prevSlideDamping)) {
            prevSlideStiffness = slideStiffness
            prevSlideDamping = slideDamping
            kotlinx.coroutines.delay(180)
            playSlide()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (appLanguage == "el") "Δοκιμαστήριο Κίνησης" else "Animation Sandbox",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (appLanguage == "el") "Αγγίξτε το στοιχείο για δοκιμή" else "Tap elements to preview custom physics",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            TextButton(
                onClick = { 
                    when (tabTransitionMode) {
                        0 -> playSlide()
                        1 -> playBounce()
                        else -> playFade()
                    }
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (appLanguage == "el") "Δοκιμή Κίνησης" else "Trigger Play",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Playground Box (Shows only the selected preview)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.45f))
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when (tabTransitionMode) {
                    0 -> {
                        // 3. Slide Sandbox Item
                        Column(
                            modifier = Modifier
                                .width(120.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { playSlide() }
                                .padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .graphicsLayer {
                                            translationX = slideX.value.dp.toPx()
                                        }
                                        .size(40.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    tonalElevation = 4.dp
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.PlayArrow,
                                            contentDescription = "Slide Item",
                                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (appLanguage == "el") "Ολίσθηση" else "Slide",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    1 -> {
                        // 1. Bounce Sandbox Item
                        Column(
                            modifier = Modifier
                                .width(120.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { playBounce() }
                                .padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .graphicsLayer {
                                            translationX = bounceX.value.dp.toPx()
                                        }
                                        .size(40.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    tonalElevation = 4.dp
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.WaterDrop,
                                            contentDescription = "Bounce Item",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (appLanguage == "el") "Αναπήδηση" else "Bounce",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> {
                        // 2. Fade Sandbox Item (Pure Alpha Fade)
                        Column(
                            modifier = Modifier
                                .width(120.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { playFade() }
                                .padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .graphicsLayer {
                                            // Smooth Alpha fade with no horizontal slide/jitter
                                            alpha = fadeAlpha.value.coerceIn(0f, 1f)
                                        }
                                        .size(40.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    tonalElevation = 4.dp
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Star,
                                            contentDescription = "Fade Item",
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (appLanguage == "el") "Σβήσιμο" else "Fade",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConfettiStyleToggle(
    currentMode: Int,
    labels: List<String>,
    onModeSelect: (Int) -> Unit,
    triggerLightHaptic: () -> Unit,
    triggerSnapHaptic: () -> Unit = {}
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(25.dp))
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val densityVal = androidx.compose.ui.platform.LocalDensity.current.density
        val trackPaddingPx = 5f * densityVal
        val numSteps = labels.size
        
        // Single segment width in Px
        val segmentWidthPx = widthPx / numSteps
        
        val currentIndex = currentMode.coerceIn(0, numSteps - 1)
        val targetOffsetPx = segmentWidthPx * currentIndex
        
        var isDragging by remember { mutableStateOf(false) }
        var dragOffsetPx by remember { mutableFloatStateOf(0f) }
        
        val animatedOffsetPx by androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (isDragging) dragOffsetPx else targetOffsetPx,
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = if (isDragging) 0.8f else 0.8f,
                stiffness = if (isDragging) 1000f else 300f
            ),
            label = "confetti_thumb_offset"
        )
        
        // Background labels click areas
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            labels.forEachIndexed { index, text ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {
                            if (currentMode != index) {
                                onModeSelect(index)
                                triggerLightHaptic()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val isSelected = currentMode == index
                    val textAlpha by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (isSelected) 0f else 0.6f,
                        label = "confetti_text_alpha"
                    )
                    Text(
                        text = text,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = textAlpha),
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
        
        val currentModeState by rememberUpdatedState(currentMode)
        val onModeSelectState by rememberUpdatedState(onModeSelect)
        val triggerLightHapticState by rememberUpdatedState(triggerLightHaptic)
        val triggerSnapHapticState by rememberUpdatedState(triggerSnapHaptic)

        // The thumb itself containing the selected item text
        val maxDragOffsetPx = widthPx - segmentWidthPx
        val centerOffsets = List(numSteps) { it * segmentWidthPx }

        Box(
            modifier = Modifier
                .offset {
                    androidx.compose.ui.unit.IntOffset(
                        x = (trackPaddingPx + animatedOffsetPx).toInt(),
                        y = trackPaddingPx.toInt()
                    )
                }
                .size(
                    width = androidx.compose.ui.unit.Dp((segmentWidthPx / densityVal) - trackPaddingPx * 2 / densityVal),
                    height = 40.dp
                )
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset: androidx.compose.ui.geometry.Offset ->
                            isDragging = true
                            dragOffsetPx = segmentWidthPx * currentModeState
                        },
                        onDragEnd = {
                            isDragging = false
                            // Snap to closest
                            val closestIndex = centerOffsets.withIndex().minByOrNull { Math.abs(it.value - dragOffsetPx) }?.index ?: 0
                            if (closestIndex != currentModeState) {
                                triggerSnapHapticState()
                                onModeSelectState(closestIndex)
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onHorizontalDrag = { change: androidx.compose.ui.input.pointer.PointerInputChange, dragAmount: Float ->
                            change.consume()
                            dragOffsetPx = (dragOffsetPx + dragAmount).coerceIn(0f, maxDragOffsetPx)
                            triggerLightHapticState()
                            
                            val closestIndex = centerOffsets.withIndex().minByOrNull { Math.abs(it.value - dragOffsetPx) }?.index ?: 0
                            if (closestIndex != currentModeState) {
                                isDragging = false
                                triggerSnapHapticState()
                                onModeSelectState(closestIndex)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            val labelText = labels.getOrElse(currentMode) { "" }
            val icon = when (currentMode) {
                0 -> Icons.Rounded.Celebration
                1 -> Icons.Rounded.Star
                else -> Icons.Rounded.Favorite
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 2.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = labelText,
                    fontSize = 11.sp, // slightly smaller font to prevent truncation
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun DotsIcon(dotCount: Int, tint: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(20.dp)) {
        val width = size.width
        val height = size.height
        val radius = 2.dp.toPx()
        
        when (dotCount) {
            4 -> {
                // 2x2 grid
                val cols = 2
                val rows = 2
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val cx = width * (c + 1) / (cols + 1)
                        val cy = height * (r + 1) / (rows + 1)
                        drawCircle(color = tint, radius = radius, center = androidx.compose.ui.geometry.Offset(cx, cy))
                    }
                }
            }
            6 -> {
                // 2x3 grid (3 cols, 2 rows)
                val cols = 3
                val rows = 2
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val cx = width * (c + 1) / (cols + 1)
                        val cy = height * (r + 1) / (rows + 1)
                        drawCircle(color = tint, radius = radius, center = androidx.compose.ui.geometry.Offset(cx, cy))
                    }
                }
            }
            8 -> {
                // Circular layout (8 dots on a circle)
                val centerX = width / 2
                val centerY = height / 2
                val rx = width * 0.35f
                for (i in 0 until 8) {
                    val angle = i * (2 * Math.PI / 8)
                    val cx = (centerX + rx * Math.cos(angle)).toFloat()
                    val cy = (centerY + rx * Math.sin(angle)).toFloat()
                    drawCircle(color = tint, radius = radius, center = androidx.compose.ui.geometry.Offset(cx, cy))
                }
            }
            10 -> {
                // 2x5 grid (5 cols, 2 rows)
                val cols = 5
                val rows = 2
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val cx = width * (c + 1) / (cols + 1)
                        val cy = height * (r + 1) / (rows + 1)
                        drawCircle(color = tint, radius = radius - 0.2f.dp.toPx(), center = androidx.compose.ui.geometry.Offset(cx, cy))
                    }
                }
            }
            12 -> {
                // 3x4 grid (4 cols, 3 rows)
                val cols = 4
                val rows = 3
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val cx = width * (c + 1) / (cols + 1)
                        val cy = height * (r + 1) / (rows + 1)
                        drawCircle(color = tint, radius = radius - 0.4f.dp.toPx(), center = androidx.compose.ui.geometry.Offset(cx, cy))
                    }
                }
            }
            15 -> {
                // 3x5 grid (5 cols, 3 rows)
                val cols = 5
                val rows = 3
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val cx = width * (c + 1) / (cols + 1)
                        val cy = height * (r + 1) / (rows + 1)
                        drawCircle(color = tint, radius = radius - 0.5f.dp.toPx(), center = androidx.compose.ui.geometry.Offset(cx, cy))
                    }
                }
            }
            20 -> {
                // 4x5 grid (5 cols, 4 rows)
                val cols = 5
                val rows = 4
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val cx = width * (c + 1) / (cols + 1)
                        val cy = height * (r + 1) / (rows + 1)
                        drawCircle(color = tint, radius = radius - 0.6f.dp.toPx(), center = androidx.compose.ui.geometry.Offset(cx, cy))
                    }
                }
            }
            30 -> {
                // 5x6 grid (6 cols, 5 rows)
                val cols = 6
                val rows = 5
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val cx = width * (c + 1) / (cols + 1)
                        val cy = height * (r + 1) / (rows + 1)
                        drawCircle(color = tint, radius = radius - 0.8f.dp.toPx(), center = androidx.compose.ui.geometry.Offset(cx, cy))
                    }
                }
            }
            50 -> {
                // 5x10 grid (10 cols, 5 rows)
                val cols = 10
                val rows = 5
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val cx = width * (c + 1) / (cols + 1)
                        val cy = height * (r + 1) / (rows + 1)
                        drawCircle(color = tint, radius = radius - 1.1f.dp.toPx(), center = androidx.compose.ui.geometry.Offset(cx, cy))
                    }
                }
            }
            else -> {
                // Fallback
                val centerX = width / 2
                val centerY = height / 2
                val rx = width * 0.3f
                for (i in 0 until dotCount.coerceAtMost(50)) {
                    val angle = i * (2 * Math.PI / dotCount)
                    val cx = (centerX + rx * Math.cos(angle)).toFloat()
                    val cy = (centerY + rx * Math.sin(angle)).toFloat()
                    drawCircle(color = tint, radius = radius, center = androidx.compose.ui.geometry.Offset(cx, cy))
                }
            }
        }
    }
}

@Composable
fun ParticleCountToggle(
    currentValue: Int,
    options: List<Int>,
    onValueSelect: (Int) -> Unit,
    triggerLightHaptic: () -> Unit,
    triggerSnapHaptic: () -> Unit = {}
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(25.dp))
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val densityVal = androidx.compose.ui.platform.LocalDensity.current.density
        val trackPaddingPx = 5f * densityVal
        val numSteps = options.size
        
        // Single segment width
        val segmentWidthPx = widthPx / numSteps
        
        val currentIndex = options.indexOf(currentValue).coerceAtLeast(0)
        val targetOffsetPx = segmentWidthPx * currentIndex
        
        var isDragging by remember { mutableStateOf(false) }
        var dragOffsetPx by remember { mutableFloatStateOf(0f) }
        
        val animatedOffsetPx by androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (isDragging) dragOffsetPx else targetOffsetPx,
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = if (isDragging) 0.82f else 0.82f,
                stiffness = if (isDragging) 1000f else 300f
            ),
            label = "particles_thumb_offset"
        )
        
        // Background labels click areas
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEachIndexed { index, valOption ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {
                            if (currentValue != valOption) {
                                onValueSelect(valOption)
                                triggerLightHaptic()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val isSelected = currentValue == valOption
                    val textAlpha by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (isSelected) 0f else 0.65f,
                        label = "particles_text_alpha"
                    )
                    Text(
                        text = "$valOption",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = textAlpha),
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
        
        val currentValueState by rememberUpdatedState(currentValue)
        val currentOnValueSelect by rememberUpdatedState(onValueSelect)
        val currentTriggerLightHapticState by rememberUpdatedState(triggerLightHaptic)
        val currentTriggerSnapHapticState by rememberUpdatedState(triggerSnapHaptic)

        val maxDragOffsetPx = widthPx - segmentWidthPx
        val centerOffsets = List(numSteps) { it * segmentWidthPx }

        // The thumb itself containing the selected item with custom DotsIcon and count text
        Box(
            modifier = Modifier
                .offset {
                    androidx.compose.ui.unit.IntOffset(
                        x = (animatedOffsetPx + trackPaddingPx).toInt(),
                        y = trackPaddingPx.toInt()
                    )
                }
                .width(
                    androidx.compose.ui.unit.Dp((segmentWidthPx / densityVal) - (trackPaddingPx * 2 / densityVal))
                )
                .height(40.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset: androidx.compose.ui.geometry.Offset ->
                            isDragging = true
                            val activeIndex = options.indexOf(currentValueState).coerceAtLeast(0)
                            dragOffsetPx = segmentWidthPx * activeIndex
                        },
                        onDragEnd = {
                            isDragging = false
                            val closestIndex = centerOffsets.withIndex().minByOrNull { Math.abs(it.value - dragOffsetPx) }?.index ?: 0
                            val activeIndex = options.indexOf(currentValueState).coerceAtLeast(0)
                            if (closestIndex != activeIndex) {
                                currentTriggerSnapHapticState()
                                currentOnValueSelect(options[closestIndex])
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onHorizontalDrag = { change: androidx.compose.ui.input.pointer.PointerInputChange, dragAmount: Float ->
                            change.consume()
                            dragOffsetPx = (dragOffsetPx + dragAmount).coerceIn(0f, maxDragOffsetPx)
                            currentTriggerLightHapticState()
                            
                            val closestIndex = centerOffsets.withIndex().minByOrNull { Math.abs(it.value - dragOffsetPx) }?.index ?: 0
                            val activeIndex = options.indexOf(currentValueState).coerceAtLeast(0)
                            if (closestIndex != activeIndex) {
                                isDragging = false
                                currentTriggerSnapHapticState()
                                currentOnValueSelect(options[closestIndex])
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            val dotCount = when (currentValue) {
                1500 -> 15
                2000 -> 20
                3000 -> 30
                5000 -> 50
                else -> currentValue / 100
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 0.dp)
            ) {
                DotsIcon(
                    dotCount = dotCount,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "$currentValue",
                    fontSize = 10.sp, // scaled down so 4 digit numbers fit with the icon
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun MixFilterChip(
    label: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    androidx.compose.material3.Surface(
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        ),
        modifier = modifier.height(40.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}