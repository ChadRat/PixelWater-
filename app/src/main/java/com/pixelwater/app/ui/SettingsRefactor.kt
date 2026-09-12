package com.pixelwater.app.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

@Composable
fun SettingsCategoryHeader(title: String, icon: ImageVector? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun SegmentedThemeMode(
    themeMode: String,
    appLanguage: String,
    autoThemeScheduleEnabled: Boolean = false,
    autoThemeScheduleMode: String = "SUNSET_SUNRISE",
    autoThemeLightStartHour: Int = 7,
    autoThemeLightStartMin: Int = 0,
    autoThemeDarkStartHour: Int = 20,
    autoThemeDarkStartMin: Int = 0,
    locationCity: String = "",
    onThemeSelect: (String) -> Unit,
    onToggleAutoSchedule: (Boolean) -> Unit = {},
    onUpdateScheduleMode: (String) -> Unit = {},
    onUpdateLightStart: (Int, Int) -> Unit = { _, _ -> },
    onUpdateDarkStart: (Int, Int) -> Unit = { _, _ -> },
    triggerHaptic: () -> Unit
) {
    val steps = listOf(
        "LIGHT" to (Icons.Rounded.LightMode to (if (appLanguage == "el") "Φωτεινό" else "Light")),
        "DARK" to (Icons.Rounded.DarkMode to (if (appLanguage == "el") "Σκοτεινό" else "Dark")),
        "SYSTEM" to (Icons.Rounded.AutoAwesome to (if (appLanguage == "el") "Σύστημα" else "System"))
    )

    val view = androidx.compose.ui.platform.LocalView.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { index, item ->
                val isSelected = item.first == themeMode

                val targetTopStart = if (isSelected || index == 0) 28.dp else 12.dp
                val targetBottomStart = if (isSelected || index == 0) 28.dp else 12.dp
                val targetTopEnd = if (isSelected || index == steps.size - 1) 28.dp else 12.dp
                val targetBottomEnd = if (isSelected || index == steps.size - 1) 28.dp else 12.dp

                val topStart by animateDpAsState(
                    targetValue = targetTopStart,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                    label = "theme_topStart_$index"
                )
                val bottomStart by animateDpAsState(
                    targetValue = targetBottomStart,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                    label = "theme_bottomStart_$index"
                )
                val topEnd by animateDpAsState(
                    targetValue = targetTopEnd,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                    label = "theme_topEnd_$index"
                )
                val bottomEnd by animateDpAsState(
                    targetValue = targetBottomEnd,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                    label = "theme_bottomEnd_$index"
                )

                val animatedShape = RoundedCornerShape(
                    topStart = topStart,
                    bottomStart = bottomStart,
                    topEnd = topEnd,
                    bottomEnd = bottomEnd
                )

                val isFrostedGlass = LocalFrostedGlassEnabled.current
                val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

                val targetBgColor = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    if (isFrostedGlass) GlassTheme.getSubCardColor(isDark) else MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.25f else 0.18f)
                }

                val targetContentColor = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    if (isFrostedGlass) (if (isDark) Color.White.copy(alpha = 0.95f) else MaterialTheme.colorScheme.onSurface) else MaterialTheme.colorScheme.primary
                }

                val targetBorderColor = if (isSelected) {
                    if (isFrostedGlass) Color.White.copy(alpha = 0.55f) else if (!isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else Color.Transparent
                } else {
                    if (isFrostedGlass) Color.White.copy(alpha = if (isDark) 0.18f else 0.40f) else Color.Transparent
                }

                val animatedBgColor by animateColorAsState(
                    targetValue = targetBgColor,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                    label = "theme_bgColor_$index"
                )
                val animatedContentColor by animateColorAsState(
                    targetValue = targetContentColor,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                    label = "theme_contentColor_$index"
                )
                val animatedBorderColor by animateColorAsState(
                    targetValue = targetBorderColor,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                    label = "theme_borderColor_$index"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(animatedShape)
                        .background(animatedBgColor)
                        .then(
                            if (animatedBorderColor != Color.Transparent) {
                                Modifier.border(1.dp, animatedBorderColor, animatedShape)
                            } else {
                                Modifier
                            }
                        )
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
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
                                    onThemeSelect(item.first)
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = item.second.first,
                            contentDescription = item.second.second,
                            tint = animatedContentColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = item.second.second,
                            color = animatedContentColor,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        if (themeMode == "SYSTEM") {
            var isScheduleExpanded by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        isScheduleExpanded = !isScheduleExpanded
                        triggerHaptic()
                    }
                    .padding(vertical = 6.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isScheduleExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Expand Schedule Options",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (appLanguage == "el") "Προγραμματισμός Θέματος" else "Automatic Dark/Light Schedule",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (autoThemeScheduleEnabled) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text(
                            text = if (autoThemeScheduleMode == "SUNSET_SUNRISE") {
                                if (appLanguage == "el") "Ηλιοβασίλεμα" else "Sunset"
                            } else {
                                if (appLanguage == "el") "Προσαρμοσμένο" else "Custom"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isScheduleExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, start = 4.dp, end = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (appLanguage == "el") "Ενεργοποίηση Χρονοδιαγράμματος" else "Enable Auto Theme Schedule",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (appLanguage == "el") 
                                    "Αλλάζει αυτόματα φωτεινό/σκοτεινό θέμα ανεξάρτητα από το σύστημα." 
                                else 
                                    "Overrides system dark mode based on location or custom times.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        ChunkySettingSwitch(
                            checked = autoThemeScheduleEnabled,
                            onCheckedChange = {
                                onToggleAutoSchedule(it)
                                triggerHaptic()
                            }
                        )
                    }

                    if (autoThemeScheduleEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val isSunset = autoThemeScheduleMode == "SUNSET_SUNRISE"
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onUpdateScheduleMode("SUNSET_SUNRISE")
                                        triggerHaptic()
                                    },
                                color = if (isSunset) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = if (isSunset) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.WbSunny,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isSunset) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (appLanguage == "el") "Ηλιοβασίλεμα/Ανατολή" else "Sunset / Sunrise",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSunset) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSunset) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            val isCustom = autoThemeScheduleMode == "CUSTOM"
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onUpdateScheduleMode("CUSTOM")
                                        triggerHaptic()
                                    },
                                color = if (isCustom) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = if (isCustom) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Schedule,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isCustom) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (appLanguage == "el") "Προσαρμοσμένες Ώρες" else "Custom Times",
                                        fontSize = 11.sp,
                                        fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCustom) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (autoThemeScheduleMode == "SUNSET_SUNRISE") {
                            Text(
                                text = if (appLanguage == "el")
                                    "Το θέμα αλλάζει στις ~06:00 (Φωτεινό) και ~19:00 (Σκοτεινό) με βάση την τοποθεσία (${locationCity.ifEmpty { "Αθήνα" }})."
                                else
                                    "Switches to Light at ~06:00 and Dark at ~19:00 based on location (${locationCity.ifEmpty { "Detected City" }}).",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Rounded.LightMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = if (appLanguage == "el") "Έναρξη Φωτεινού:" else "Light Mode Start:",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                val newHour = (autoThemeLightStartHour + 23) % 24
                                                onUpdateLightStart(newHour, autoThemeLightStartMin)
                                                triggerHaptic()
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Rounded.Remove, contentDescription = "Decrease hour", modifier = Modifier.size(16.dp))
                                        }
                                        Text(
                                            text = String.format("%02d:%02d", autoThemeLightStartHour, autoThemeLightStartMin),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
                                        IconButton(
                                            onClick = {
                                                val newHour = (autoThemeLightStartHour + 1) % 24
                                                onUpdateLightStart(newHour, autoThemeLightStartMin)
                                                triggerHaptic()
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Rounded.Add, contentDescription = "Increase hour", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Rounded.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = if (appLanguage == "el") "Έναρξη Σκοτεινού:" else "Dark Mode Start:",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                val newHour = (autoThemeDarkStartHour + 23) % 24
                                                onUpdateDarkStart(newHour, autoThemeDarkStartMin)
                                                triggerHaptic()
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Rounded.Remove, contentDescription = "Decrease hour", modifier = Modifier.size(16.dp))
                                        }
                                        Text(
                                            text = String.format("%02d:%02d", autoThemeDarkStartHour, autoThemeDarkStartMin),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
                                        IconButton(
                                            onClick = {
                                                val newHour = (autoThemeDarkStartHour + 1) % 24
                                                onUpdateDarkStart(newHour, autoThemeDarkStartMin)
                                                triggerHaptic()
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Rounded.Add, contentDescription = "Increase hour", modifier = Modifier.size(16.dp))
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

@Composable
fun ThemeColorRow(
    currentAppTheme: String, // "DYNAMIC" or "STATIC"
    currentStaticSeed: Int,
    isFrostedGlassEnabled: Boolean,
    appLanguage: String,
    isNerdMode: Boolean = false,
    onAppThemeChange: (String) -> Unit,
    onStaticSeedChange: (Int) -> Unit,
    onFrostedGlassChange: (Boolean) -> Unit
) {
    val oceanSeed = 0xFF1D5AAB.toInt()
    val purpleSeed = 0xFF8E24AA.toInt()
    val forestSeed = 0xFF2E7D32.toInt()
    val slateSeed = 0xFF455A64.toInt()
    val crimsonSeed = 0xFFC2185B.toInt()
    val amberSeed = 0xFFE65100.toInt()
    val mintSeed = 0xFF00897B.toInt()
    val lavenderSeed = 0xFF5E35B1.toInt()
    val charcoalSeed = 0xFF212121.toInt()
    val coralSeed = 0xFFF4511E.toInt()
    
    val colors = listOf(
        (if (appLanguage == "el") "Ωκεανός" else "Ocean") to oceanSeed,
        (if (appLanguage == "el") "Μοβ" else "Purple") to purpleSeed,
        (if (appLanguage == "el") "Δάσος" else "Forest") to forestSeed,
        (if (appLanguage == "el") "Σχιστόλιθος" else "Slate") to slateSeed,
        (if (appLanguage == "el") "Πορφυρό" else "Crimson") to crimsonSeed,
        (if (appLanguage == "el") "Κεχριμπάρι" else "Amber") to amberSeed,
        (if (appLanguage == "el") "Μέντα" else "Mint") to mintSeed,
        (if (appLanguage == "el") "Λεβάντα" else "Lavender") to lavenderSeed,
        (if (appLanguage == "el") "Ανθρακί" else "Charcoal") to charcoalSeed,
        (if (appLanguage == "el") "Κοράλλι" else "Coral") to coralSeed
    )

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (appLanguage == "el") "Χρώμα Θέματος" else "Theme Color",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Dynamic item
            val isDynamicSelected = currentAppTheme == "DYNAMIC"
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GearRotatableShapeButton(
                    isSelected = isDynamicSelected,
                    onClick = { onAppThemeChange("DYNAMIC") },
                    modifier = Modifier.size(56.dp),
                    shape = WavyShape(),
                    backgroundBrush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                            if (isDynamicSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            if (isDynamicSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                        )
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = "Dynamic",
                        tint = if (isDynamicSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(if (isDynamicSelected) 28.dp else 24.dp)
                    )
                }
                Text(
                    text = if (appLanguage == "el") "Δυναμικό" else "Dynamic",
                    fontSize = 12.sp,
                    fontWeight = if (isDynamicSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isDynamicSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Glass item
            if (isNerdMode || isFrostedGlassEnabled) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GearRotatableShapeButton(
                        isSelected = isFrostedGlassEnabled,
                        onClick = { onFrostedGlassChange(!isFrostedGlassEnabled) },
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        borderWidth = 1.dp,
                        borderColor = if (isFrostedGlassEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha=0.3f),
                        backgroundColor = if (isFrostedGlassEnabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        if (isFrostedGlassEnabled) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(28.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.BlurOn,
                                contentDescription = "Glass",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Text(
                        text = if (appLanguage == "el") "Γυαλί" else "Glass",
                        fontSize = 12.sp,
                        fontWeight = if (isFrostedGlassEnabled) FontWeight.Bold else FontWeight.Medium,
                        color = if (isFrostedGlassEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Static color items
            colors.forEach { (name, seed) ->
                val isSelected = currentAppTheme == "STATIC" && currentStaticSeed == seed
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GearRotatableShapeButton(
                        isSelected = isSelected,
                        onClick = {
                            onAppThemeChange("STATIC")
                            onStaticSeedChange(seed)
                        },
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        backgroundColor = Color(seed)
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Selected",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Text(
                        text = name,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun WearOsConnectionStatusCard(
    isWatchConnected: Boolean,
    connectedWatchModel: String,
    watchBatteryLevel: Int?,
    watchCompanionDetected: Boolean,
    appLanguage: String,
    isFrostedGlassEnabled: Boolean,
    frostedTransparency: Float
) {
    val isDark = isSystemInDarkTheme() || MaterialTheme.colorScheme.background.red < 0.3f
    val watchCardBgColor = if (isFrostedGlassEnabled) {
        MaterialTheme.colorScheme.surface.copy(alpha = frostedTransparency * 0.35f)
    } else {
        if (isDark) Color(0xFF1B1C20) else MaterialTheme.colorScheme.surface
    }
    val watchCardBorderColor = if (isFrostedGlassEnabled) {
        Color.White.copy(alpha = 0.15f)
    } else {
        if (isWatchConnected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(watchCardBgColor)
            .border(1.dp, watchCardBorderColor, RoundedCornerShape(28.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Watch status badge box with connection dot
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (isWatchConnected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) 
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), 
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Watch,
                            contentDescription = null,
                            tint = if (isWatchConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    // Pulsating Signal Dot Badge in corner of the icon
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 0.8f,
                        targetValue = 1.4f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )
                    
                    val dotColor = if (isWatchConnected) Color(0xFF4CAF50) else Color(0xFFE57373)
                    
                    Box(
                        modifier = Modifier
                            .padding(end = 2.dp, bottom = 2.dp)
                            .size(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .graphicsLayer {
                                    scaleX = if (isWatchConnected) scale else 1.0f
                                    scaleY = if (isWatchConnected) scale else 1.0f
                                }
                                .background(dotColor.copy(alpha = 0.4f), shape = CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(dotColor, shape = CircleShape)
                                .border(1.dp, watchCardBgColor, CircleShape)
                        )
                    }
                }
                
                Column {
                    val watchName = remember(isWatchConnected, connectedWatchModel) {
                        if (isWatchConnected) {
                            val name = connectedWatchModel.trim()
                            if (name.isEmpty()) {
                                if (appLanguage == "el") "Έξυπνο Ρολόι" else "Smartwatch"
                            } else {
                                val isPixelWatch = name.lowercase().contains("pixel watch")
                                if (isPixelWatch) "Google Pixel Watch" else name
                            }
                        } else {
                            ""
                        }
                    }
                    
                    val titleText = if (isWatchConnected) {
                        if (appLanguage == "el") "$watchName Συνδεδεμένο" else "$watchName Connected"
                    } else {
                        if (appLanguage == "el") "Δεν βρέθηκε Ρολόι" else "No Watch Connected"
                    }
                    
                    val subtitleText = if (isWatchConnected) {
                        if (watchCompanionDetected) {
                            if (appLanguage == "el") "Εφαρμογή συνοδός ενεργή" else "Companion app active"
                        } else {
                            if (appLanguage == "el") "Δεν εκτελείται η εφαρμογή" else "Companion app not running"
                        }
                    } else {
                        if (appLanguage == "el") "Ενεργοποιήστε το Bluetooth" else "Enable Bluetooth or start watch app"
                    }
                    
                    Text(
                        text = titleText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitleText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
            
            if (isWatchConnected) {
                val batteryText = if (watchBatteryLevel != null) "$watchBatteryLevel%" else "--%"
                val batteryColor = if (watchBatteryLevel != null && watchBatteryLevel!! <= 20) Color(0xFFE57373) else Color(0xFF4CAF50)
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .background(
                            if (isFrostedGlassEnabled) Color.White.copy(alpha = 0.08f)
                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (watchBatteryLevel != null && watchBatteryLevel!! <= 20) Icons.Rounded.BatteryAlert else Icons.Rounded.BatteryChargingFull,
                        contentDescription = null,
                        tint = batteryColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = batteryText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = batteryColor
                    )
                }
            }
        }
    }
}

@Composable
fun WearOsThemeColorCard(
    wearThemeColor: String,
    appLanguage: String,
    isFrostedGlassEnabled: Boolean,
    strokeColor: Color,
    wallpaperThemeColors: List<Int>,
    wearDynamicPaletteRole: String,
    getSwatchColors: (Int) -> Triple<Color, Color, Color>,
    updateWearThemeColor: (String) -> Unit,
    updateWearDynamicPaletteRole: (String) -> Unit,
    triggerSliderHaptic: () -> Unit
) {
    ChunkySettingCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header title with Palette icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = if (appLanguage == "el") "Θέμα Χρώματος Ρολογιού" else "Watch Face Theme Color",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Follow App item (expressive tilted oval Material 3 style)
                    val isFollowAppSelected = wearThemeColor == "FOLLOW_APP" || wearThemeColor == "DYNAMIC"
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        GearRotatableShapeButton(
                            isSelected = isFollowAppSelected,
                            onClick = { updateWearThemeColor("FOLLOW_APP") },
                            modifier = Modifier
                                .width(62.dp)
                                .height(46.dp)
                                .padding(2.dp),
                            shape = RoundedCornerShape(50),
                            borderWidth = if (isFollowAppSelected) 3.dp else 0.dp,
                            borderColor = if (isFollowAppSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            backgroundBrush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(
                                    if (isFollowAppSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    if (isFollowAppSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                )
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Smartphone,
                                contentDescription = "Follow App Theme",
                                tint = if (isFollowAppSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = if (appLanguage == "el") "Θέμα Εφαρμ." else "Follow App",
                            fontSize = 11.sp,
                            fontWeight = if (isFollowAppSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isFollowAppSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Extracted wallpaper colors
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        wallpaperThemeColors.forEachIndexed { idx, colorInt ->
                            val name = "PALETTE_$idx"
                            val isSelected = wearThemeColor == name
                            val (topColor, bottomLeftColor, bottomRightColor) = remember(colorInt) { getSwatchColors(colorInt) }

                            GearRotatableShapeButton(
                                isSelected = isSelected,
                                onClick = { updateWearThemeColor(name) },
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
                }

                // Dynamic/Palette Role options
                val isDynamicOrPalette = wearThemeColor == "DYNAMIC" || wearThemeColor == "FOLLOW_APP" || wearThemeColor.startsWith("PALETTE_")
                if (isDynamicOrPalette) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (appLanguage == "el") "Ρόλος Χρωματικής Παλέτας (Wear OS)" else "Palette Color Role (Wear OS)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isFrostedGlassEnabled) Color.White.copy(alpha = 0.08f) 
                                else MaterialTheme.colorScheme.surfaceVariant, 
                                RoundedCornerShape(12.dp)
                            )
                            .border(1.dp, strokeColor, RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val roles = listOf(
                            "PRIMARY" to (if (appLanguage == "el") "Πρωτεύον" else "Primary"),
                            "SECONDARY" to (if (appLanguage == "el") "Δευτερεύον" else "Secondary"),
                            "TERTIARY" to (if (appLanguage == "el") "Τριτεύον" else "Tertiary")
                        )
                        roles.forEach { (roleKey, roleLabel) ->
                            val isRoleSelected = wearDynamicPaletteRole == roleKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isRoleSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { updateWearDynamicPaletteRole(roleKey) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = roleLabel,
                                    fontSize = 11.sp,
                                    fontWeight = if (isRoleSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isRoleSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Scrollable Row of standard colors
                Text(
                    text = if (appLanguage == "el") "Σταθερά Χρώματα" else "Standard Colors",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val isCustomRgbSelected = wearThemeColor.startsWith("CUSTOM_RGB_")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Custom RGB Color trigger dot
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isCustomRgbSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent)
                            .border(
                                width = if (isCustomRgbSelected) 2.5.dp else 1.dp,
                                color = if (isCustomRgbSelected) MaterialTheme.colorScheme.primary else strokeColor,
                                shape = CircleShape
                            )
                            .clickable { 
                                val currentHex = if (wearThemeColor.startsWith("CUSTOM_RGB_")) wearThemeColor.removePrefix("CUSTOM_RGB_") else "00BFA5"
                                updateWearThemeColor("CUSTOM_RGB_$currentHex")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    androidx.compose.ui.graphics.Brush.sweepGradient(
                                        colors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                                    )
                                )
                        )
                    }

                    val standardColors = listOf(
                        Triple("MINT", Color(0xFF00BFA5), if (appLanguage == "el") "Παγετώδης Μέντα" else "Glacial Mint"),
                        Triple("SKY", Color(0xFF29B6F6), if (appLanguage == "el") "Αέριος Ουρανός" else "Aero Sky"),
                        Triple("LAVENDER", Color(0xFFAB47BC), if (appLanguage == "el") "Πρωινή Λεβάντα" else "Lavender Dew"),
                        Triple("CORAL", Color(0xFFFF7043), if (appLanguage == "el") "Κοράλλι Ήλιου" else "Sunset Coral"),
                        Triple("OCEAN", Color(0xFF1D5AAB), if (appLanguage == "el") "Βαθύς Ωκεανός" else "Deep Ocean"),
                        Triple("PURPLE", Color(0xFF8E24AA), if (appLanguage == "el") "Μωβ Ορχιδέα" else "Orchid Purple"),
                        Triple("FOREST", Color(0xFF2E7D32), if (appLanguage == "el") "Δάσος" else "Eldergrove"),
                        Triple("SLATE", Color(0xFF455A64), if (appLanguage == "el") "Σχιστόλιθος" else "Basalt Slate"),
                        Triple("ORANGE", Color(0xFFFF9100), if (appLanguage == "el") "Ηλιακή Έκλαμψη" else "Solar Flare"),
                        Triple("CRIMSON", Color(0xFFD81B60), if (appLanguage == "el") "Ρουμπίνι" else "Ruby Crimson"),
                        Triple("INDIGO", Color(0xFF3F51B5), if (appLanguage == "el") "Ινδικό Μεσάνυχτα" else "Midnight Indigo"),
                        Triple("ROSE", Color(0xFFFF80AB), if (appLanguage == "el") "Ροζ Ανθού" else "Blossom Rose"),
                        Triple("GOLD", Color(0xFFFFD700), if (appLanguage == "el") "Χρυσός Ήλιος" else "Aurum Gold"),
                        Triple("AQUA", Color(0xFF00E5FF), if (appLanguage == "el") "Aqua Ποσειδώνα" else "Neptune Aqua"),
                        Triple("TEAL", Color(0xFF008080), if (appLanguage == "el") "Κιρκίρι" else "Abyssal Teal"),
                        Triple("PLUM", Color(0xFF4A148C), if (appLanguage == "el") "Δαμάσκηνο" else "Imperial Plum"),
                        Triple("PEACH", Color(0xFFFFCC80), if (appLanguage == "el") "Ροδάκινο" else "Sherbet Peach"),
                        Triple("BLACK", Color(0xFF000000), if (appLanguage == "el") "Μαύρος Όνυχας" else "Onyx Black"),
                        Triple("WHITE", Color(0xFFFFFFFF), if (appLanguage == "el") "Κιμωλία" else "Chalk White")
                    )
                    
                    standardColors.forEach { (name, color, label) ->
                        val isSelected = wearThemeColor == name
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.width(72.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else strokeColor,
                                        shape = CircleShape
                                    )
                                    .clickable { updateWearThemeColor(name) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Selected",
                                        tint = if (color == Color.White) Color.Black else Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // Custom RGB Slider Controls
                if (isCustomRgbSelected) {
                    val currentHex = wearThemeColor.removePrefix("CUSTOM_RGB_")
                    val rVal = remember(currentHex) {
                        try { currentHex.substring(0, 2).toInt(16) } catch (e: Exception) { 0 }
                    }
                    val gVal = remember(currentHex) {
                        try { currentHex.substring(2, 4).toInt(16) } catch (e: Exception) { 191 }
                    }
                    val bVal = remember(currentHex) {
                        try { currentHex.substring(4, 6).toInt(16) } catch (e: Exception) { 165 }
                    }

                    var isSlidersExpanded by remember { mutableStateOf(false) }

                    Spacer(modifier = Modifier.height(6.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isFrostedGlassEnabled) Color.White.copy(alpha = 0.05f) 
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(1.dp, strokeColor, RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isSlidersExpanded = !isSlidersExpanded }
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowUp,
                                    contentDescription = "Toggle RGB Sliders",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (appLanguage == "el") "Προσαρμοσμένο Χρώμα RGB" else "Custom RGB Color",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            val previewColor = remember(currentHex) {
                                try { Color(android.graphics.Color.parseColor("#$currentHex")) } catch (e: Exception) { Color(0xFF00BFA5) }
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(previewColor)
                                        .border(1.dp, strokeColor, CircleShape)
                                )
                                Text(
                                    text = "#$currentHex",
                                    fontSize = 13.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        if (isSlidersExpanded) {
                            // Red Slider
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = if (appLanguage == "el") "Κόκκινο" else "Red", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = rVal.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                CapsulePatternSlider(
                                    value = rVal.toFloat(),
                                    onValueChange = { 
                                        val hex = String.format("%02X%02X%02X", it.toInt(), gVal, bVal)
                                        updateWearThemeColor("CUSTOM_RGB_$hex")
                                    },
                                    valueRange = 0f..255f,
                                    stepsCount = 256,
                                    triggerHaptic = triggerSliderHaptic,
                                    activeColor = Color.Red,
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
                                    Text(text = gVal.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                CapsulePatternSlider(
                                    value = gVal.toFloat(),
                                    onValueChange = { 
                                        val hex = String.format("%02X%02X%02X", rVal, it.toInt(), bVal)
                                        updateWearThemeColor("CUSTOM_RGB_$hex")
                                    },
                                    valueRange = 0f..255f,
                                    stepsCount = 256,
                                    triggerHaptic = triggerSliderHaptic,
                                    activeColor = Color.Green,
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
                                    Text(text = bVal.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                CapsulePatternSlider(
                                    value = bVal.toFloat(),
                                    onValueChange = { 
                                        val hex = String.format("%02X%02X%02X", rVal, gVal, it.toInt())
                                        updateWearThemeColor("CUSTOM_RGB_$hex")
                                    },
                                    valueRange = 0f..255f,
                                    stepsCount = 256,
                                    triggerHaptic = triggerSliderHaptic,
                                    activeColor = Color.Blue,
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
