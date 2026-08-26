package com.pixelwater.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import com.pixelwater.app.ui.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pixelwater.app.data.DeviceBlueprint
import java.util.Locale

@Composable
fun PixelBlueprintRenderer(
    blueprint: DeviceBlueprint,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(100.dp, 200.dp)) {
        val dpScale = size.width / 100f
        val brandLower = (blueprint.detectedBrand.takeIf { it.isNotBlank() } ?: "generic").lowercase(Locale.US)
        val modelLower = (blueprint.detectedModel.takeIf { it.isNotBlank() } ?: "generic").lowercase(Locale.US)
        
        val isGoogle = brandLower.contains("google") || modelLower.contains("pixel")
        val isApple = brandLower.contains("apple") || modelLower.contains("iphone")
        val isSamsung = brandLower.contains("samsung") || modelLower.contains("galaxy") || modelLower.contains("sm-")
        
        // Determine phone chassis corner radius & styling
        val chassisRadius = if (isSamsung && modelLower.contains("ultra")) {
            3f * dpScale
        } else if (isApple) {
            15f * dpScale
        } else if (isGoogle) {
            13f * dpScale
        } else {
            11f * dpScale
        }
        
        // 1. Draw solid dark background matching user blueprint styling
        val phoneBgColor = Color(0xFF0F0F0F)
        drawRoundRect(
            color = phoneBgColor,
            size = Size(100f * dpScale, 200f * dpScale),
            cornerRadius = CornerRadius(chassisRadius, chassisRadius)
        )
        
        // 2. Draw outer high-contrast chassis stroke border
        drawRoundRect(
            color = Color.White,
            size = Size(100f * dpScale, 200f * dpScale),
            cornerRadius = CornerRadius(chassisRadius, chassisRadius),
            style = Stroke(width = 1.8f * dpScale)
        )
        
        // 3. Draw brand logo in the upper-middle / exact center
        if (isGoogle) {
            // Clean bold 'G' visual in center
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 14f * dpScale
                typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
            drawContext.canvas.nativeCanvas.drawText("G", 50f * dpScale, 107f * dpScale, paint)
        } else if (isApple) {
            // Iconic Apple glyph in center
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 18f * dpScale
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
            drawContext.canvas.nativeCanvas.drawText("", 50f * dpScale, 108f * dpScale, paint)
        } else if (isSamsung) {
            // Spaced SAMSUNG label at bottom
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 6f * dpScale
                typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
                textAlign = android.graphics.Paint.Align.CENTER
                letterSpacing = 0.15f
                isAntiAlias = true
            }
            drawContext.canvas.nativeCanvas.drawText("SAMSUNG", 50f * dpScale, 175f * dpScale, paint)
        } else {
            // Draw generic branding outline in the center
            drawCircle(
                color = Color.White.copy(alpha = 0.15f),
                radius = 24f * dpScale,
                center = Offset(50f * dpScale, 100f * dpScale),
                style = Stroke(width = 1f * dpScale, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f * dpScale, 4f * dpScale), 0f))
            )
        }
        
        // 4. Draw camera layout with beautiful precision
        if (isGoogle) {
            val bumpY = blueprint.bumpY.coerceAtLeast(15f).coerceAtMost(35f)
            val bumpHeight = blueprint.bumpHeight.coerceAtLeast(14f).coerceAtMost(25f)
            
            // Visor Bar Background matching high-contrast tech blueprint style
            drawRect(
                color = Color(0xFF1E1E1E),
                topLeft = Offset(0f, bumpY * dpScale),
                size = Size(100f * dpScale, bumpHeight * dpScale)
            )
            // Outer borders of visor bar
            drawLine(
                color = Color.White,
                start = Offset(0f, bumpY * dpScale),
                end = Offset(100f * dpScale, bumpY * dpScale),
                strokeWidth = 1.8f * dpScale
            )
            drawLine(
                color = Color.White,
                start = Offset(0f, (bumpY + bumpHeight) * dpScale),
                end = Offset(100f * dpScale, (bumpY + bumpHeight) * dpScale),
                strokeWidth = 1.8f * dpScale
            )
            
            val centerY = bumpY + bumpHeight / 2f
            
            if (modelLower.contains("pro") || modelLower.contains("6 pro") || modelLower.contains("7 pro") || modelLower.contains("8 pro") || modelLower.contains("9 pro")) {
                // EXACT layout from user's screenshot: 
                // 1. Circle (Left)
                drawCircle(
                    color = Color.Black,
                    radius = 5.2f * dpScale,
                    center = Offset(32f * dpScale, centerY * dpScale)
                )
                drawCircle(
                    color = Color.White,
                    radius = 5.2f * dpScale,
                    center = Offset(32f * dpScale, centerY * dpScale),
                    style = Stroke(width = 1f * dpScale)
                )
                
                // 2. Smaller Circle (Middle-left)
                drawCircle(
                    color = Color.Black,
                    radius = 3.6f * dpScale,
                    center = Offset(45f * dpScale, centerY * dpScale)
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.6f * dpScale,
                    center = Offset(45f * dpScale, centerY * dpScale),
                    style = Stroke(width = 1f * dpScale)
                )
                
                // 3. Rectangular telephoto prism (Middle-right)
                val sqSize = 7.5f * dpScale
                val sqLeft = 53.5f * dpScale - sqSize / 2f
                val sqTop = centerY * dpScale - sqSize / 2f
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(sqLeft, sqTop),
                    size = Size(sqSize, sqSize)
                )
                drawRect(
                    color = Color.White,
                    topLeft = Offset(sqLeft, sqTop),
                    size = Size(sqSize, sqSize),
                    style = Stroke(width = 1f * dpScale)
                )
                
                // 4. Matte black LED / Flash sensor circle (Far-right)
                drawCircle(
                    color = Color.Black,
                    radius = 3.2f * dpScale,
                    center = Offset(69f * dpScale, centerY * dpScale)
                )
            } else {
                // General/Standard Dual lens Pixel: Circle Circle Flash
                drawCircle(
                    color = Color.Black,
                    radius = 5.5f * dpScale,
                    center = Offset(34f * dpScale, centerY * dpScale)
                )
                drawCircle(
                    color = Color.White,
                    radius = 5.5f * dpScale,
                    center = Offset(34f * dpScale, centerY * dpScale),
                    style = Stroke(width = 1f * dpScale)
                )
                
                drawCircle(
                    color = Color.Black,
                    radius = 5.5f * dpScale,
                    center = Offset(50f * dpScale, centerY * dpScale)
                )
                drawCircle(
                    color = Color.White,
                    radius = 5.5f * dpScale,
                    center = Offset(50f * dpScale, centerY * dpScale),
                    style = Stroke(width = 1f * dpScale)
                )
                
                drawCircle(
                    color = Color.Black,
                    radius = 3.2f * dpScale,
                    center = Offset(72f * dpScale, centerY * dpScale)
                )
            }
        } else if (isApple) {
            // iPhone triple camera layout with neat outlines!
            val bX = 10f * dpScale
            val bY = 10f * dpScale
            val bS = 42f * dpScale
            val bR = 11f * dpScale
            
            // Draw background Camera island
            drawRoundRect(
                color = Color(0xFF1E1E1E),
                topLeft = Offset(bX, bY),
                size = Size(bS, bS),
                cornerRadius = CornerRadius(bR, bR)
            )
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(bX, bY),
                size = Size(bS, bS),
                cornerRadius = CornerRadius(bR, bR),
                style = Stroke(width = 1.5f * dpScale)
            )
            
            // Clean triple circular layout
            val lensData = listOf(
                Offset(21f * dpScale, 21f * dpScale),
                Offset(31f * dpScale, 38f * dpScale),
                Offset(41f * dpScale, 21f * dpScale)
            )
            lensData.forEach { center ->
                drawCircle(color = Color.Black, radius = 5.5f * dpScale, center = center)
                drawCircle(color = Color.White, radius = 5.5f * dpScale, center = center, style = Stroke(width = 1f * dpScale))
                drawCircle(color = Color.White.copy(alpha=0.6f), radius = 1.8f * dpScale, center = center)
            }
            
            // Small circles representing lidar & flash
            drawCircle(color = Color.White.copy(alpha=0.9f), radius = 2.2f * dpScale, center = Offset(31f * dpScale, 21f * dpScale))
            drawCircle(color = Color.Black, radius = 1.8f * dpScale, center = Offset(41f * dpScale, 36f * dpScale))
        } else if (isSamsung) {
            // Samsung Vertical individual elements or Vertical island
            val isUltra = modelLower.contains("ultra")
            if (isUltra) {
                // Samsung S22/23/24 Ultra linear cameras
                val yOffsets = listOf(22f, 42f, 62f)
                yOffsets.forEach { yVal ->
                    drawCircle(color = Color.Black, radius = 5f * dpScale, center = Offset(25f * dpScale, yVal * dpScale))
                    drawCircle(color = Color.White, radius = 5f * dpScale, center = Offset(25f * dpScale, yVal * dpScale), style = Stroke(width = 1.2f * dpScale))
                }
                drawCircle(color = Color.Black, radius = 3.5f * dpScale, center = Offset(50f * dpScale, 32f * dpScale))
                drawCircle(color = Color.White, radius = 3.5f * dpScale, center = Offset(50f * dpScale, 32f * dpScale), style = Stroke(width = 1f * dpScale))
                
                drawCircle(color = Color(0xFFFFFFA0), radius = 2.2f * dpScale, center = Offset(50f * dpScale, 52f * dpScale))
            } else {
                // Standard Samsung pill with white outline
                val bX = 14f * dpScale
                val bY = 14f * dpScale
                val bW = 24f * dpScale
                val bH = 65f * dpScale
                val bR = 12f * dpScale
                
                drawRoundRect(
                    color = Color(0xFF1E1E1E),
                    topLeft = Offset(bX, bY),
                    size = Size(bW, bH),
                    cornerRadius = CornerRadius(bR, bR)
                )
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(bX, bY),
                    size = Size(bW, bH),
                    cornerRadius = CornerRadius(bR, bR),
                    style = Stroke(width = 1.5f * dpScale)
                )
                
                val yOffsets = listOf(26f, 46f, 66f)
                yOffsets.forEach { yVal ->
                    drawCircle(color = Color.Black, radius = 4.5f * dpScale, center = Offset((14f + 12f) * dpScale, yVal * dpScale))
                    drawCircle(color = Color.White, radius = 4.5f * dpScale, center = Offset((14f + 12f) * dpScale, yVal * dpScale), style = Stroke(width = 1f * dpScale))
                }
            }
        } else {
            // Generic device with circular layout or custom outline based on model details
            val hasCircularIsland = modelLower.contains("oneplus") || modelLower.contains("xiaomi") || modelLower.contains("pro")
            if (hasCircularIsland) {
                // Circle center camera island
                drawCircle(
                    color = Color(0xFF1E1E1E),
                    radius = 18f * dpScale,
                    center = Offset(50f * dpScale, 38f * dpScale)
                )
                drawCircle(
                    color = Color.White,
                    radius = 18f * dpScale,
                    center = Offset(50f * dpScale, 38f * dpScale),
                    style = Stroke(width = 1.5f * dpScale)
                )
                
                val lensOffsets = listOf(
                    Offset(42f * dpScale, 38f * dpScale),
                    Offset(58f * dpScale, 38f * dpScale),
                    Offset(50f * dpScale, 46f * dpScale)
                )
                lensOffsets.forEach { center ->
                    drawCircle(color = Color.Black, radius = 4f * dpScale, center = center)
                    drawCircle(color = Color.White, radius = 4f * dpScale, center = center, style = Stroke(width = 0.8f * dpScale))
                }
            } else {
                // Standard pill style
                drawRoundRect(
                    color = Color(0xFF1E1E1E),
                    topLeft = Offset(blueprint.bumpX * dpScale, blueprint.bumpY * dpScale),
                    size = Size(blueprint.bumpWidth * dpScale, blueprint.bumpHeight * dpScale),
                    cornerRadius = CornerRadius(blueprint.bumpCornerRadius * dpScale, blueprint.bumpCornerRadius * dpScale)
                )
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(blueprint.bumpX * dpScale, blueprint.bumpY * dpScale),
                    size = Size(blueprint.bumpWidth * dpScale, blueprint.bumpHeight * dpScale),
                    cornerRadius = CornerRadius(blueprint.bumpCornerRadius * dpScale, blueprint.bumpCornerRadius * dpScale),
                    style = Stroke(width = 1.5f * dpScale)
                )
                blueprint.lenses.forEach { lens ->
                    drawCircle(color = Color.Black, radius = lens.radius * dpScale, center = Offset(lens.x * dpScale, lens.y * dpScale))
                    drawCircle(color = Color.White, radius = lens.radius * dpScale, center = Offset(lens.x * dpScale, lens.y * dpScale), style = Stroke(width = 1f * dpScale))
                }
            }
        }
    }
}

@Composable
fun DevDeviceFakerCard(
    viewModel: WaterViewModel,
    appLanguage: String,
    modifier: Modifier = Modifier
) {
    val fakeEnabled = viewModel.fakeDeviceEnabled.collectAsStateWithLifecycle().value
    val fakeBrand = viewModel.fakeDeviceBrand.collectAsStateWithLifecycle().value
    val fakeModel = viewModel.fakeDeviceModel.collectAsStateWithLifecycle().value

    ChunkySettingCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Smartphone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (appLanguage == "el") "Προσομοιωτής Συσκευής" else "Lab: Device Simulator",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Switch(
                    checked = fakeEnabled,
                    onCheckedChange = { isChecked ->
                        viewModel.triggerButtonHaptic()
                        viewModel.updateFakeDevice(fakeBrand, fakeModel, isChecked)
                    }
                )
            }

            Text(
                text = if (appLanguage == "el")
                    "Ενεργοποιήστε την παράκαμψη συσκευής για να δοκιμάσετε τα σχεδιαγράμματα, τις προδιαγραφές και το περίγραμμα διαφόρων μοντέλων Pixel ή άλλων κατασκευαστών."
                    else "Simulate any commercial phone model to stress-test custom layouts, camera visor rendering, and hardware specification tables.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (fakeEnabled) {
                androidx.compose.material3.OutlinedTextField(
                    value = fakeBrand,
                    onValueChange = { newValue ->
                        viewModel.updateFakeDevice(newValue, fakeModel, true)
                    },
                    label = { Text("Brand / Manufacturer") },
                    placeholder = { Text("e.g. Google, Apple, Samsung") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                androidx.compose.material3.OutlinedTextField(
                    value = fakeModel,
                    onValueChange = { newValue ->
                        viewModel.updateFakeDevice(fakeBrand, newValue, true)
                    },
                    label = { Text("Model Name") },
                    placeholder = { Text("e.g. Pixel 6 Pro, Galaxy S24 Ultra") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Text(
                    text = "Developer Quick Presets:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(
                        "Pixel 6 Pro" to ("Google" to "Pixel 6 Pro"),
                        "Pixel 8a" to ("Google" to "Pixel 8a"),
                        "S24 Ultra" to ("Samsung" to "Galaxy S24 Ultra"),
                        "iPhone 15 Pro" to ("Apple" to "iPhone 15 Pro Max")
                    )

                    presets.forEach { (label, pair) ->
                        val (brand, model) = pair
                        SuggestionChip(
                            onClick = {
                                viewModel.triggerButtonHaptic()
                                viewModel.updateFakeDevice(brand, model, true)
                            },
                            label = { Text(label, fontSize = 10.sp) }
                        )
                    }
                }
            }
        }
    }
}
