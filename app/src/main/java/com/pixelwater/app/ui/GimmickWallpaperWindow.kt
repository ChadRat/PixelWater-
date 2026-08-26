package com.pixelwater.app.ui

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalContext
import com.pixelwater.app.ui.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelwater.app.data.Content
import com.pixelwater.app.data.GeminiRequest
import com.pixelwater.app.data.GeminiRetrofitClient
import com.pixelwater.app.data.Part
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GimmickWallpaperWindow(
    viewModel: WaterViewModel,
    appLanguage: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = true // Gimmick window uses a beautiful dark slate theme, Google Pixel 10 style!

    val density = androidx.compose.ui.platform.LocalDensity.current
    val widthPx = with(density) { 170.dp.toPx() }
    val heightPx = with(density) { 360.dp.toPx() }

    // Initialize SharedPreferences
    val prefs: SharedPreferences = remember {
        context.getSharedPreferences("water_tracker_prefs", Context.MODE_PRIVATE)
    }

    // Interactive states synced with SHARED PREFERENCES
    var wallpaperType by remember { mutableStateOf(prefs.getString("gimmick_wallpaper_type", "MATERIAL_SHAPES") ?: "MATERIAL_SHAPES") }
    var speed by remember { mutableStateOf(prefs.getFloat("gimmick_speed", 1.0f)) }
    var oledEnabled by remember { mutableStateOf(prefs.getBoolean("gimmick_oled_enabled", false)) }
    var rotationEnabled by remember { mutableStateOf(prefs.getBoolean("gimmick_rotation_enabled", true)) }
    var rgbLoop by remember { mutableStateOf(prefs.getBoolean("gimmick_rgb_loop", false)) }

    // Independent rotation multipliers as requested
    var rotMultA by remember { mutableStateOf(prefs.getFloat("gimmick_rot_mult_a", 1.0f)) }
    var rotMultB by remember { mutableStateOf(prefs.getFloat("gimmick_rot_mult_b", -0.727f)) }
    var rotMultC by remember { mutableStateOf(prefs.getFloat("gimmick_rot_mult_c", 0.533f)) }
    var rotMultD by remember { mutableStateOf(prefs.getFloat("gimmick_rot_mult_d", -0.615f)) }

    // Floating/Animation offsets for PREVIEW
    var previewPhaseA by remember { mutableStateOf(0f) }
    var previewPhaseB by remember { mutableStateOf(45f) }
    var previewPhaseC by remember { mutableStateOf(90f) }
    var previewPhaseD by remember { mutableStateOf(120f) }

    // Individual color sliders config
    var c1R by remember { mutableStateOf(prefs.getFloat("gimmick_c1_r", 66f)) }
    var c1G by remember { mutableStateOf(prefs.getFloat("gimmick_c1_g", 133f)) }
    var c1B by remember { mutableStateOf(prefs.getFloat("gimmick_c1_b", 244f)) }

    var c2R by remember { mutableStateOf(prefs.getFloat("gimmick_c2_r", 219f)) }
    var c2G by remember { mutableStateOf(prefs.getFloat("gimmick_c2_g", 68f)) }
    var c2B by remember { mutableStateOf(prefs.getFloat("gimmick_c2_b", 85f)) }

    var c3R by remember { mutableStateOf(prefs.getFloat("gimmick_c3_r", 244f)) }
    var c3G by remember { mutableStateOf(prefs.getFloat("gimmick_c3_g", 180f)) }
    var c3B by remember { mutableStateOf(prefs.getFloat("gimmick_c3_b", 0f)) }

    var c4R by remember { mutableStateOf(prefs.getFloat("gimmick_c4_r", 52f)) }
    var c4G by remember { mutableStateOf(prefs.getFloat("gimmick_c4_g", 168f)) }
    var c4B by remember { mutableStateOf(prefs.getFloat("gimmick_c4_b", 83f)) }

    var hueShift by remember { mutableStateOf(prefs.getFloat("gimmick_hue_shift", 0f)) }
    var shapesSaturation by remember { mutableStateOf(prefs.getFloat("gimmick_shapes_saturation", 1.0f)) }

    // Global vs Independent coloring
    var globalColorEnabled by remember { mutableStateOf(prefs.getBoolean("gimmick_global_color_enabled", false)) }
    var globalR by remember { mutableStateOf(prefs.getFloat("gimmick_global_r", 66f)) }
    var globalG by remember { mutableStateOf(prefs.getFloat("gimmick_global_g", 133f)) }
    var globalB by remember { mutableStateOf(prefs.getFloat("gimmick_global_b", 244f)) }

    // Shapes Contrast slider
    var contrastScale by remember { mutableStateOf(prefs.getFloat("gimmick_contrast_scale", 1.0f)) }

    // Spaceship animated state variables
    var pShipEnabled by remember { mutableStateOf(prefs.getBoolean("gimmick_spaceship_enabled", true)) }
    var shipX by remember { mutableStateOf(0f) }
    var shipY by remember { mutableStateOf(0f) }
    var shipVx by remember { mutableStateOf(0f) }
    var shipVy by remember { mutableStateOf(0f) }
    var shipAngle by remember { mutableStateOf(0f) }
    var shipTargetX by remember { mutableStateOf(0f) }
    var shipTargetY by remember { mutableStateOf(0f) }
    var shipState by remember { mutableStateOf("FLYING") }
    var shipLandedTimer by remember { mutableStateOf(0f) }
    var shipTargetIndex by remember { mutableStateOf(-1) }
    var shipInitialized by remember { mutableStateOf(false) }

    // Spaceship advanced design parameters
    var shipSize by remember { mutableStateOf(prefs.getFloat("gimmick_spaceship_size", 12.0f)) }
    var shipContrast by remember { mutableStateOf(prefs.getFloat("gimmick_spaceship_contrast_scale", 1.0f)) }
    var shipR by remember { mutableStateOf(prefs.getFloat("gimmick_ship_r", 255f)) }
    var shipG by remember { mutableStateOf(prefs.getFloat("gimmick_ship_g", 255f)) }
    var shipB by remember { mutableStateOf(prefs.getFloat("gimmick_ship_b", 255f)) }
    var shipSpeedMultiplier by remember { mutableStateOf(prefs.getFloat("gimmick_spaceship_speed", 1.0f)) }
    var shipLandingTime by remember { mutableStateOf(prefs.getFloat("gimmick_spaceship_landing_time", 3.0f)) }
    var shipLandingEnabled by remember { mutableStateOf(prefs.getBoolean("gimmick_spaceship_landing_enabled", true)) }
    var shipAntiCrush by remember { mutableStateOf(prefs.getBoolean("gimmick_spaceship_anti_crush", false)) }
    var shipCautiousness by remember { mutableStateOf(prefs.getFloat("gimmick_spaceship_cautiousness", 0.5f)) }
    var shape1Size by remember { mutableStateOf(prefs.getFloat("gimmick_shape1_size", 0.95f)) }
    var shape2Size by remember { mutableStateOf(prefs.getFloat("gimmick_shape2_size", 1.41f)) }
    var shape3Size by remember { mutableStateOf(prefs.getFloat("gimmick_shape3_size", 1.35f)) }
    var shape4Size by remember { mutableStateOf(prefs.getFloat("gimmick_shape4_size", 0.99f)) }

    // Lock screen customization states
    var isPreviewingLockScreen by remember { mutableStateOf(false) }
    var lockscreenColorsEnabled by remember { mutableStateOf(prefs.getBoolean("gimmick_lockscreen_colors_enabled", false)) }
    var lockscreenGlobalColorEnabled by remember { mutableStateOf(prefs.getBoolean("gimmick_lockscreen_global_color_enabled", false)) }

    var lockscreenGlobalR by remember { mutableStateOf(prefs.getFloat("gimmick_lockscreen_global_r", 152f)) }
    var lockscreenGlobalG by remember { mutableStateOf(prefs.getFloat("gimmick_lockscreen_global_g", 57f)) }
    var lockscreenGlobalB by remember { mutableStateOf(prefs.getFloat("gimmick_lockscreen_global_b", 235f)) }

    var lockscreenC1R by remember { mutableStateOf(prefs.getFloat("gimmick_lockscreen_c1_r", 0f)) }
    var lockscreenC1G by remember { mutableStateOf(prefs.getFloat("gimmick_lockscreen_c1_g", 229f)) }
    var lockscreenC1B by remember { mutableStateOf(prefs.getFloat("gimmick_lockscreen_c1_b", 255f)) }

    var lockscreenC2R by remember { mutableStateOf(prefs.getFloat("gimmick_lockscreen_c2_r", 98f)) }
    var lockscreenC2G by remember { mutableStateOf(prefs.getFloat("gimmick_lockscreen_c2_g", 0f)) }
    var lockscreenC2B by remember { mutableStateOf(prefs.getFloat("gimmick_lockscreen_c2_b", 234f)) }

    var lockscreenC3R by remember { mutableStateOf(prefs.getFloat("gimmick_lockscreen_c3_r", 255f)) }
    var lockscreenC3G by remember { mutableStateOf(prefs.getFloat("gimmick_lockscreen_c3_g", 64f)) }
    var lockscreenC3B by remember { mutableStateOf(prefs.getFloat("gimmick_lockscreen_c3_b", 129f)) }

    var lockscreenC4R by remember { mutableStateOf(prefs.getFloat("gimmick_lockscreen_c4_r", 224f)) }
    var lockscreenC4G by remember { mutableStateOf(prefs.getFloat("gimmick_lockscreen_c4_g", 64f)) }
    var lockscreenC4B by remember { mutableStateOf(prefs.getFloat("gimmick_lockscreen_c4_b", 251f)) }

    var shapeBlurRadius by remember { mutableStateOf(prefs.getFloat("gimmick_shape_blur_radius", 0f)) }
    var shapeBlurTarget by remember { mutableStateOf(prefs.getString("gimmick_shape_blur_target", "BOTH") ?: "BOTH") }
    var shapeBlurLockIndependent by remember { mutableStateOf(prefs.getBoolean("gimmick_shape_blur_lock_independent", false)) }
    var shapeBlurRadiusLock by remember { mutableStateOf(prefs.getFloat("gimmick_shape_blur_radius_lock", 15.0f)) }

    var selectedShapeIndex by remember { mutableStateOf(-1) }

    // Shape-specific properties (A, B, C, D)
    var indSpeedA by remember { mutableStateOf(prefs.getFloat("gimmick_ind_speed_a", 1.0f)) }
    var indSpeedB by remember { mutableStateOf(prefs.getFloat("gimmick_ind_speed_b", 1.0f)) }
    var indSpeedC by remember { mutableStateOf(prefs.getFloat("gimmick_ind_speed_c", 1.0f)) }
    var indSpeedD by remember { mutableStateOf(prefs.getFloat("gimmick_ind_speed_d", 1.0f)) }

    var indSatA by remember { mutableStateOf(prefs.getFloat("gimmick_ind_sat_a", 1.0f)) }
    var indSatB by remember { mutableStateOf(prefs.getFloat("gimmick_ind_sat_b", 1.0f)) }
    var indSatC by remember { mutableStateOf(prefs.getFloat("gimmick_ind_sat_c", 1.0f)) }
    var indSatD by remember { mutableStateOf(prefs.getFloat("gimmick_ind_sat_d", 1.0f)) }

    var indVividA by remember { mutableStateOf(prefs.getFloat("gimmick_ind_vivid_a", 1.0f)) }
    var indVividB by remember { mutableStateOf(prefs.getFloat("gimmick_ind_vivid_b", 1.0f)) }
    var indVividC by remember { mutableStateOf(prefs.getFloat("gimmick_ind_vivid_c", 1.0f)) }
    var indVividD by remember { mutableStateOf(prefs.getFloat("gimmick_ind_vivid_d", 1.0f)) }

    var indBlurA by remember { mutableStateOf(prefs.getFloat("gimmick_ind_blur_a", 0.0f)) }
    var indBlurB by remember { mutableStateOf(prefs.getFloat("gimmick_ind_blur_b", 0.0f)) }
    var indBlurC by remember { mutableStateOf(prefs.getFloat("gimmick_ind_blur_c", 0.0f)) }
    var indBlurD by remember { mutableStateOf(prefs.getFloat("gimmick_ind_blur_d", 0.0f)) }

    // Gestural swipe states for flipping directions
    var dragStartOffset by remember { mutableStateOf(Offset.Zero) }
    var dragAmountTotal by remember { mutableStateOf(Offset.Zero) }

    // AI Coach prompt variables
    var aiPrompt by remember { mutableStateOf("") }
    var isAiGenerating by remember { mutableStateOf(false) }
    var aiCoachFeedback by remember { mutableStateOf("") }

    // Tick preview rotations and automated spaceship physics
    LaunchedEffect(rotationEnabled, speed, rotMultA, rotMultB, rotMultC, rotMultD, pShipEnabled, shipSize, shipLandingEnabled, shipAntiCrush, shipCautiousness, indSpeedA, indSpeedB, indSpeedC, indSpeedD) {
        var lastTime = System.currentTimeMillis()
        while (true) {
            val now = System.currentTimeMillis()
            val deltaSec = (now - lastTime) / 1000f
            lastTime = now
            if (deltaSec > 0f) {
                if (rotationEnabled) {
                    previewPhaseA = (previewPhaseA + 0.15f * speed * indSpeedA * rotMultA * deltaSec) % (2f * Math.PI.toFloat())
                    previewPhaseB = (previewPhaseB + 0.15f * speed * indSpeedB * rotMultB * deltaSec) % (2f * Math.PI.toFloat())
                    previewPhaseC = (previewPhaseC + 0.15f * speed * indSpeedC * rotMultC * deltaSec) % (2f * Math.PI.toFloat())
                    previewPhaseD = (previewPhaseD + 0.15f * speed * indSpeedD * rotMultD * deltaSec) % (2f * Math.PI.toFloat())

                    if (rgbLoop) {
                        hueShift = (hueShift + 25f * speed * deltaSec) % 360f
                        prefs.edit().putFloat("gimmick_hue_shift", hueShift).apply()
                    }
                }

                // Run automated retro spaceship movement
                if (pShipEnabled) {
                    val w = widthPx
                    val h = heightPx
                    val dpScale = w / 360f

                    if (!shipInitialized) {
                        shipX = w / 2f
                        shipY = h / 2f
                        shipInitialized = true
                    }

                    if (shipInitialized) {
                        val centerAX: Float
                        val centerAY: Float
                        val centerBX: Float
                        val centerBY: Float
                        val centerCX: Float
                        val centerCY: Float
                        val centerDX: Float
                        val centerDY: Float

                        if (wallpaperType == "AURA_GLOW") {
                            centerAX = w * 0.15f + kotlin.math.sin(previewPhaseA) * (w * 0.06f)
                            centerAY = h * 0.15f + kotlin.math.cos(previewPhaseA) * (h * 0.05f)

                            centerBX = w * 0.85f + kotlin.math.cos(previewPhaseB) * (w * 0.07f)
                            centerBY = h * 0.85f + kotlin.math.sin(previewPhaseB) * (h * 0.06f)

                            centerCX = w * 0.10f + kotlin.math.sin(previewPhaseC) * (w * 0.05f)
                            centerCY = h * 0.50f + kotlin.math.cos(previewPhaseC) * (h * 0.08f)

                            centerDX = w * 0.90f + kotlin.math.cos(previewPhaseD) * (w * 0.06f)
                            centerDY = h * 0.35f + kotlin.math.sin(previewPhaseD) * (h * 0.05f)
                        } else {
                            centerAX = 90f * dpScale
                            centerAY = 130f * dpScale

                            centerBX = w - 30f * dpScale
                            centerBY = h - 35f * dpScale

                            centerCX = 5f * dpScale
                            centerCY = h / 2f + 130f * dpScale

                            centerDX = w - 20f * dpScale
                            centerDY = 340f * dpScale
                        }

                        val targetsX = floatArrayOf(centerAX, centerBX, centerCX, centerDX)
                        val targetsY = floatArrayOf(centerAY, centerBY, centerCY, centerDY)

                        val s1 = shape1Size
                        val s2 = shape2Size
                        val s3 = shape3Size
                        val s4 = shape4Size
                        val shipRadius = shipSize * dpScale
                        val targetsR = floatArrayOf(
                            340f * 0.45f * dpScale * s1,
                            340f * 0.33f * dpScale * s2,
                            280f * 0.32f * dpScale * s3,
                            280f * 0.44f * dpScale * s4
                        )

                        if (shipState == "CRUMBLED" || shipState == "CRASHED") {
                            shipLandedTimer -= deltaSec
                            
                            // Drift broken pieces beautifully
                            shipX += shipVx * deltaSec
                            shipY += shipVy * deltaSec
                            val crashDrag = 0.8f
                            shipVx *= (1f - crashDrag * deltaSec)
                            shipVy *= (1f - crashDrag * deltaSec)
                            
                            if (shipLandedTimer <= 0f) {
                                shipInitialized = false // Respawn
                            }
                        } else if (shipTargetIndex == -1) {
                            shipTargetIndex = 0
                            val angle = Math.random() * 2 * Math.PI
                            shipTargetX = targetsX[0] + (targetsR[0] + shipRadius + 2f*dpScale) * kotlin.math.cos(angle).toFloat()
                            shipTargetY = targetsY[0] + (targetsR[0] + shipRadius + 2f*dpScale) * kotlin.math.sin(angle).toFloat()
                            shipState = "FLYING"
                        }

                        if (shipState == "FLYING") {
                            // 1. Steering: Seek target
                            var ux = shipTargetX - shipX
                            var uy = shipTargetY - shipY
                            var dist = kotlin.math.sqrt(ux * ux + uy * uy)
                            if (dist > 0f) {
                                ux /= dist
                                uy /= dist
                            }

                            // 2. Obstacle avoidance with cautiousness scaling
                            var avoidX = 0f
                            var avoidY = 0f
                            var hitCount = 0
                            
                            val multiplier = if (shipAntiCrush) 1.5f else (0.5f + shipCautiousness * 1.5f)
                            val avoidForceMult = if (shipAntiCrush) 1.0f else (shipCautiousness * 1.5f)

                            for (i in 0..3) {
                                val odx = shipX - targetsX[i]
                                val ody = shipY - targetsY[i]
                                val odist = kotlin.math.sqrt(odx * odx + ody * ody)
                                val safeDist = targetsR[i] + shipRadius * multiplier

                                if (odist < targetsR[i] + shipRadius * 0.8f) {
                                    if (i != shipTargetIndex || !shipLandingEnabled) {
                                        hitCount++
                                    }
                                }

                                if (odist < safeDist * 1.8f && odist > 0f) {
                                    val force = 1.0f - (odist / (safeDist * 1.8f))
                                    val fdx = odx / odist
                                    val fdy = ody / odist
                                    
                                    // Repulsive normal force
                                    avoidX += fdx * force * 550f * dpScale * avoidForceMult
                                    avoidY += fdy * force * 550f * dpScale * avoidForceMult
                                    
                                    // Tangential slide force (obstacle hugging) to prevent deadlock
                                    val tdx = -fdy
                                    val tdy = fdx
                                    val dot = tdx * ux + tdy * uy
                                    val sign = if (dot >= 0f) 1f else -1f
                                    val slideStrength = (1.0f - shipCautiousness) * 550f * dpScale * force
                                    avoidX += tdx * sign * slideStrength
                                    avoidY += tdy * sign * slideStrength
                                }
                            }

                            if (!shipAntiCrush && hitCount > 0) {
                                shipState = if (hitCount == 1) "CRUMBLED" else "CRASHED"
                                shipLandedTimer = 3f
                            } else {
                                val accel = 250f * dpScale * shipSpeedMultiplier
                                shipVx += (ux * accel + avoidX) * deltaSec
                                shipVy += (uy * accel + avoidY) * deltaSec

                                val drag = 1.2f
                                shipVx *= (1f - drag * deltaSec)
                                shipVy *= (1f - drag * deltaSec)

                                val currentSpeed = kotlin.math.sqrt(shipVx * shipVx + shipVy * shipVy)
                                val maxSpeed = 160f * dpScale * shipSpeedMultiplier
                                if (currentSpeed > maxSpeed) {
                                    shipVx = (shipVx / currentSpeed) * maxSpeed
                                    shipVy = (shipVy / currentSpeed) * maxSpeed
                                }

                                shipX += shipVx * deltaSec
                                shipY += shipVy * deltaSec

                                if (currentSpeed > 5f) {
                                    val targetAngle = Math.toDegrees(kotlin.math.atan2(shipVy.toDouble(), shipVx.toDouble())).toFloat()
                                    var angleDiff = targetAngle - shipAngle
                                    while (angleDiff > 180f) angleDiff -= 360f
                                    while (angleDiff < -180f) angleDiff += 360f
                                    shipAngle += angleDiff * 0.15f
                                }

                                if (dist < shipRadius * 1.5f) {
                                    if (shipLandingEnabled) {
                                        shipState = "LANDED"
                                        shipLandedTimer = shipLandingTime
                                        shipVx = 0f
                                        shipVy = 0f
                                        val finalDx = shipX - targetsX[shipTargetIndex]
                                        val finalDy = shipY - targetsY[shipTargetIndex]
                                        val finalAngle = Math.toDegrees(kotlin.math.atan2(finalDy.toDouble(), finalDx.toDouble())).toFloat()
                                        shipAngle = finalAngle
                                    } else {
                                        var nextIndex = shipTargetIndex
                                        var attempts = 0
                                        while (nextIndex == shipTargetIndex && attempts < 10) {
                                            nextIndex = (0..3).random()
                                            attempts++
                                        }
                                        shipTargetIndex = nextIndex
                                        val angle = Math.random() * 2 * Math.PI
                                        shipTargetX = targetsX[nextIndex] + (targetsR[nextIndex] + shipRadius * 1.5f) * kotlin.math.cos(angle).toFloat()
                                        shipTargetY = targetsY[nextIndex] + (targetsR[nextIndex] + shipRadius * 1.5f) * kotlin.math.sin(angle).toFloat()
                                    }
                                }
                            }
                        } else if (shipState == "LANDED") {
                            shipLandedTimer -= deltaSec

                            val dx = shipTargetX - targetsX[shipTargetIndex]
                            val dy = shipTargetY - targetsY[shipTargetIndex]
                            val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                            val R = targetsR[shipTargetIndex] + shipRadius * 1.1f
                            if (dist > 0) {
                                shipX = targetsX[shipTargetIndex] + (dx / dist) * R
                                shipY = targetsY[shipTargetIndex] + (dy / dist) * R
                            }

                            if (shipLandedTimer <= 0f) {
                                var nextIndex = shipTargetIndex
                                var attempts = 0
                                while (nextIndex == shipTargetIndex && attempts < 10) {
                                    nextIndex = (0..3).random()
                                    attempts++
                                }
                                shipTargetIndex = nextIndex
                                val angle = Math.random() * 2 * Math.PI
                                shipTargetX = targetsX[nextIndex] + (targetsR[nextIndex] + shipRadius * 1.5f) * kotlin.math.cos(angle).toFloat()
                                shipTargetY = targetsY[nextIndex] + (targetsR[nextIndex] + shipRadius * 1.5f) * kotlin.math.sin(angle).toFloat()

                                val launchAngle = Math.atan2((shipTargetY - shipY).toDouble(), (shipTargetX - shipX).toDouble())
                                val launchSpeed = 160f * dpScale * shipSpeedMultiplier
                                shipVx = (kotlin.math.cos(launchAngle) * launchSpeed).toFloat()
                                shipVy = (kotlin.math.sin(launchAngle) * launchSpeed).toFloat()
                                shipState = "FLYING"
                            }
                        }
                    }
                }
            }
            androidx.compose.runtime.withFrameNanos { it }
        }
    }

    // Preset options
    fun applyPreset(presetColors: List<Color>) {
        if (presetColors.size < 4) return
        c1R = presetColors[0].red * 255f
        c1G = presetColors[0].green * 255f
        c1B = presetColors[0].blue * 255f

        c2R = presetColors[1].red * 255f
        c2G = presetColors[1].green * 255f
        c2B = presetColors[1].blue * 255f

        c3R = presetColors[2].red * 255f
        c3G = presetColors[2].green * 255f
        c3B = presetColors[2].blue * 255f

        c4R = presetColors[3].red * 255f
        c4G = presetColors[3].green * 255f
        c4B = presetColors[3].blue * 255f

        prefs.edit().apply {
            putFloat("gimmick_c1_r", c1R)
            putFloat("gimmick_c1_g", c1G)
            putFloat("gimmick_c1_b", c1B)

            putFloat("gimmick_c2_r", c2R)
            putFloat("gimmick_c2_g", c2G)
            putFloat("gimmick_c2_b", c2B)

            putFloat("gimmick_c3_r", c3R)
            putFloat("gimmick_c3_g", c3G)
            putFloat("gimmick_c3_b", c3B)

            putFloat("gimmick_c4_r", c4R)
            putFloat("gimmick_c4_g", c4G)
            putFloat("gimmick_c4_b", c4B)
        }.apply()
        viewModel.triggerButtonHaptic()
    }

    // Resolves current preview colors (applying HSV hueShift if dynamic RGB loop)
    fun resolveColor(r: Float, g: Float, b: Float, currentHueShift: Float, shapeSat: Float = 1.0f): Color {
        if (r >= 253f && g >= 253f && b >= 253f) return Color.White
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(android.graphics.Color.rgb(r.toInt(), g.toInt(), b.toInt()), hsv)
        if (currentHueShift != 0f) {
            hsv[0] = (hsv[0] + currentHueShift) % 360f
        }
        hsv[1] = (hsv[1] * shapesSaturation * shapeSat).coerceIn(0f, 1f)
        val argb = android.graphics.Color.HSVToColor(hsv)
        return Color(argb)
    }

    val evaluatedC1 = if (globalColorEnabled) resolveColor(globalR, globalG, globalB, hueShift, indSatA) else resolveColor(c1R, c1G, c1B, hueShift, indSatA)
    val evaluatedC2 = if (globalColorEnabled) resolveColor(globalR, globalG, globalB, hueShift, indSatB) else resolveColor(c2R, c2G, c2B, hueShift, indSatB)
    val evaluatedC3 = if (globalColorEnabled) resolveColor(globalR, globalG, globalB, hueShift, indSatC) else resolveColor(c3R, c3G, c3B, hueShift, indSatC)
    val evaluatedC4 = if (globalColorEnabled) resolveColor(globalR, globalG, globalB, hueShift, indSatD) else resolveColor(c4R, c4G, c4B, hueShift, indSatD)

    val evalC1 = if (isPreviewingLockScreen && lockscreenColorsEnabled) {
        if (lockscreenGlobalColorEnabled) resolveColor(lockscreenGlobalR, lockscreenGlobalG, lockscreenGlobalB, hueShift, indSatA)
        else resolveColor(lockscreenC1R, lockscreenC1G, lockscreenC1B, hueShift, indSatA)
    } else {
        evaluatedC1
    }
    val evalC2 = if (isPreviewingLockScreen && lockscreenColorsEnabled) {
        if (lockscreenGlobalColorEnabled) resolveColor(lockscreenGlobalR, lockscreenGlobalG, lockscreenGlobalB, hueShift, indSatB)
        else resolveColor(lockscreenC2R, lockscreenC2G, lockscreenC2B, hueShift, indSatB)
    } else {
        evaluatedC2
    }
    val evalC3 = if (isPreviewingLockScreen && lockscreenColorsEnabled) {
        if (lockscreenGlobalColorEnabled) resolveColor(lockscreenGlobalR, lockscreenGlobalG, lockscreenGlobalB, hueShift, indSatC)
        else resolveColor(lockscreenC3R, lockscreenC3G, lockscreenC3B, hueShift, indSatC)
    } else {
        evaluatedC3
    }
    val evalC4 = if (isPreviewingLockScreen && lockscreenColorsEnabled) {
        if (lockscreenGlobalColorEnabled) resolveColor(lockscreenGlobalR, lockscreenGlobalG, lockscreenGlobalB, hueShift, indSatD)
        else resolveColor(lockscreenC4R, lockscreenC4G, lockscreenC4B, hueShift, indSatD)
    } else {
        evaluatedC4
    }

    // Helper functions for updating single preferences
    fun updateWallpaperType(type: String) {
        wallpaperType = type
        prefs.edit().putString("gimmick_wallpaper_type", type).apply()
        viewModel.triggerButtonHaptic()
    }

    fun updateSpeed(newSpeed: Float) {
        speed = newSpeed
        prefs.edit().putFloat("gimmick_speed", newSpeed).apply()
    }

    fun updateOled(enabled: Boolean) {
        oledEnabled = enabled
        prefs.edit().putBoolean("gimmick_oled_enabled", enabled).apply()
        viewModel.triggerButtonHaptic()
    }

    fun updateRotation(enabled: Boolean) {
        rotationEnabled = enabled
        prefs.edit().putBoolean("gimmick_rotation_enabled", enabled).apply()
        viewModel.triggerButtonHaptic()
    }

    fun updateRgbLoop(enabled: Boolean) {
        rgbLoop = enabled
        prefs.edit().putBoolean("gimmick_rgb_loop", enabled).apply()
        viewModel.triggerButtonHaptic()
        if (!enabled) {
            hueShift = 0f
            prefs.edit().putFloat("gimmick_hue_shift", 0f).apply()
        }
    }

    fun updateSpaceship(enabled: Boolean) {
        pShipEnabled = enabled
        prefs.edit().putBoolean("gimmick_spaceship_enabled", enabled).apply()
        viewModel.triggerButtonHaptic()
    }

    // Set LIVE Wallpaper chooser
    fun setAsLiveWallpaper() {
        viewModel.triggerFireworksHaptic()
        try {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(context, GlowShapesWallpaperService::class.java)
                )
            }
            context.startActivity(intent)
            Toast.makeText(context, "Opening Live Wallpaper Chooser...", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to launch chooser: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Render current layout into Bitmap & set as Static Wallpaper
    fun setAsStaticWallpaper() {
        viewModel.triggerFireworksHaptic()
        Toast.makeText(context, "Processing static wallpaper...", Toast.LENGTH_SHORT).show()

        scope.launch {
            try {
                // Generate a full screen size high-fidelity bitmap
                val displayMetrics = context.resources.displayMetrics
                val width = displayMetrics.widthPixels
                val height = displayMetrics.heightPixels

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)

                // Background
                canvas.drawColor(if (oledEnabled) android.graphics.Color.BLACK else android.graphics.Color.parseColor("#121318"))

                // Paints & shaders
                val colors = listOf(
                    android.graphics.Color.rgb((evalC1.red * 255).toInt(), (evalC1.green * 255).toInt(), (evalC1.blue * 255).toInt()),
                    android.graphics.Color.rgb((evalC2.red * 255).toInt(), (evalC2.green * 255).toInt(), (evalC2.blue * 255).toInt()),
                    android.graphics.Color.rgb((evalC3.red * 255).toInt(), (evalC3.green * 255).toInt(), (evalC3.blue * 255).toInt()),
                    android.graphics.Color.rgb((evalC4.red * 255).toInt(), (evalC4.green * 255).toInt(), (evalC4.blue * 255).toInt())
                )

                if (wallpaperType == "AURA_GLOW") {
                    // Draw Aura Glow
                    val drawAuraGlowHelper = { cx: Float, cy: Float, radius: Float, colorInt: Int, alpha: Float ->
                        val transparentColor = colorInt and 0x00FFFFFF
                        val opaqueColor = (colorInt and 0x00FFFFFF) or ((alpha * 255).toInt() shl 24)
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            shader = RadialGradient(cx, cy, radius, opaqueColor, transparentColor, Shader.TileMode.CLAMP)
                        }
                        canvas.drawCircle(cx, cy, radius, paint)
                    }

                    val w = width.toFloat()
                    val h = height.toFloat()

                    // Match AuraGlowBackground positions and radii precisely
                    val centerAX = w * 0.15f + kotlin.math.sin(previewPhaseA) * (w * 0.06f)
                    val centerAY = h * 0.15f + kotlin.math.cos(previewPhaseA) * (h * 0.05f)
                    val radiusA = w * 0.55f

                    val centerBX = w * 0.85f + kotlin.math.cos(previewPhaseB) * (w * 0.07f)
                    val centerBY = h * 0.85f + kotlin.math.sin(previewPhaseB) * (h * 0.06f)
                    val radiusB = w * 0.65f

                    val centerCX = w * 0.10f + kotlin.math.sin(previewPhaseC) * (w * 0.05f)
                    val centerCY = h * 0.50f + kotlin.math.cos(previewPhaseC) * (h * 0.08f)
                    val radiusC = w * 0.50f

                    val centerDX = w * 0.90f + kotlin.math.cos(previewPhaseD) * (w * 0.06f)
                    val centerDY = h * 0.35f + kotlin.math.sin(previewPhaseD) * (h * 0.05f)
                    val radiusD = w * 0.45f

                    drawAuraGlowHelper(centerAX, centerAY, radiusA, colors[0], (0.28f * contrastScale).coerceIn(0f, 1f))
                    drawAuraGlowHelper(centerBX, centerBY, radiusB, colors[1], (0.24f * contrastScale).coerceIn(0f, 1f))
                    drawAuraGlowHelper(centerCX, centerCY, radiusC, colors[2], (0.30f * contrastScale).coerceIn(0f, 1f))
                    drawAuraGlowHelper(centerDX, centerDY, radiusD, colors[3], (0.26f * contrastScale).coerceIn(0f, 1f))
                } else {
                    // Draw Material Shapes (Static / Stopped rotations)
                    val drawLobedShapeHelper = { cx: Float, cy: Float, baseR: Float, lobesCount: Int, amp: Float, colorInt: Int, alpha: Float, rotationAngle: Float, isSin: Boolean ->
                        val path = Path()
                        val pointsCount = 180
                        
                        canvas.save()
                        canvas.rotate(rotationAngle, cx, cy)
                        
                        for (i in 0 until pointsCount) {
                            val angleRad = (i * 2 * Math.PI / pointsCount).toFloat()
                            val wave = if (isSin) kotlin.math.sin(lobesCount * angleRad) else kotlin.math.cos(lobesCount * angleRad)
                            val r = baseR + amp * wave
                            val x = cx + r * kotlin.math.cos(angleRad)
                            val y = cy + r * kotlin.math.sin(angleRad)
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        path.close()
                        val fullDpScale = width.toFloat() / 360f
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = colorInt
                            this.alpha = (alpha * 255).toInt()
                            style = Paint.Style.FILL
                            if (shapeBlurRadius > 0f) {
                                maskFilter = android.graphics.BlurMaskFilter(shapeBlurRadius * fullDpScale, android.graphics.BlurMaskFilter.Blur.NORMAL)
                            }
                        }
                        canvas.drawPath(path, paint)
                        canvas.restore()
                    }

                    val w = width.toFloat()
                    val h = height.toFloat()
                    val dpScale = w / 360f

                    // Color indices maps: colors[0]->Shape A, colors[1]->Shape B, colors[2]->Shape C, colors[3]->Shape D
                    val angleC = Math.toDegrees(previewPhaseC.toDouble()).toFloat()
                    drawLobedShapeHelper(5f * dpScale, h / 2f + 130f * dpScale, 280f * 0.32f * dpScale * shape3Size, 3, 280f * 0.07f * dpScale * shape3Size, colors[2], (0.28f * contrastScale).coerceIn(0f, 1f), angleC, false)

                    val angleA = Math.toDegrees(previewPhaseA.toDouble()).toFloat()
                    drawLobedShapeHelper(90f * dpScale, 130f * dpScale, 340f * 0.45f * dpScale * shape1Size, 4, 340f * 0.08f * dpScale * shape1Size, colors[0], (0.26f * contrastScale).coerceIn(0f, 1f), angleA, false)

                    val angleB = Math.toDegrees(previewPhaseB.toDouble()).toFloat()
                    drawLobedShapeHelper(w - 30f * dpScale, h - 35f * dpScale, 340f * 0.33f * dpScale * shape2Size, 5, 340f * 0.06f * dpScale * shape2Size, colors[1], (0.24f * contrastScale).coerceIn(0f, 1f), angleB, true)

                    val angleD = Math.toDegrees(previewPhaseD.toDouble()).toFloat()
                    drawLobedShapeHelper(w - 20f * dpScale, 340f * dpScale, 280f * 0.44f * dpScale * shape4Size, 6, 280f * 0.05f * dpScale * shape4Size, colors[3], (0.22f * contrastScale).coerceIn(0f, 1f), angleD, true)
                }

                // Draw spaceship if enabled
                val spaceshipEnabled = prefs.getBoolean("gimmick_spaceship_enabled", true)
                if (spaceshipEnabled && shipInitialized) {
                    val rx = shipX / widthPx
                    val ry = shipY / heightPx

                    val fullShipX = rx * width
                    val fullShipY = ry * height
                    val fullDpScale = width.toFloat() / 360f
                    val shipS = shipSize * fullDpScale

                    val path = Path()
                    path.moveTo(shipS, 0f)
                    path.cubicTo(shipS * 0.8f, -shipS * 0.3f, shipS * 0.2f, -shipS * 0.35f, -shipS * 0.3f, -shipS * 0.4f)
                    path.lineTo(-shipS * 0.7f, -shipS * 0.7f)
                    path.lineTo(-shipS * 0.8f, -shipS * 0.35f)
                    path.lineTo(-shipS * 0.6f, -shipS * 0.25f)
                    path.lineTo(-shipS * 0.8f, 0f)
                    path.lineTo(-shipS * 0.6f, shipS * 0.25f)
                    path.lineTo(-shipS * 0.8f, shipS * 0.35f)
                    path.lineTo(-shipS * 0.7f, shipS * 0.7f)
                    path.lineTo(-shipS * 0.3f, shipS * 0.4f)
                    path.cubicTo(shipS * 0.2f, shipS * 0.35f, shipS * 0.8f, shipS * 0.3f, shipS, 0f)
                    path.close()

                    canvas.save()
                    canvas.translate(fullShipX, fullShipY)
                    canvas.rotate(shipAngle)

                    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.BLACK
                        alpha = (215 * shipContrast).toInt().coerceIn(0, 255)
                        style = Paint.Style.FILL
                    }
                    canvas.drawPath(path, fillPaint)

                    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.rgb(shipR.toInt(), shipG.toInt(), shipB.toInt())
                        alpha = (255 * shipContrast).toInt().coerceIn(0, 255)
                        style = Paint.Style.STROKE
                        strokeWidth = 1.8f * fullDpScale
                    }
                    canvas.drawPath(path, strokePaint)

                    canvas.restore()
                }

                // Apply to device wallpaper
                withContext(Dispatchers.IO) {
                    val wm = WallpaperManager.getInstance(context)
                    wm.setBitmap(bitmap)
                }

                Toast.makeText(context, "Static wallpaper applied successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error setting wallpaper: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Call Gemini API to parse prompt
    fun askAiCoach() {
        if (aiPrompt.isBlank()) return
        isAiGenerating = true
        aiCoachFeedback = "Consulting AI Coach..."
        viewModel.triggerButtonHaptic()

        viewModel.generateGimmickWallpaperAiTheme(
            promptText = aiPrompt,
            onSuccess = { obj ->
                try {
                    if (obj.has("wallpaperType")) wallpaperType = obj.getString("wallpaperType")
                    if (obj.has("oledEnabled")) oledEnabled = obj.getBoolean("oledEnabled")
                    if (obj.has("rotationEnabled")) rotationEnabled = obj.getBoolean("rotationEnabled")
                    if (obj.has("speed")) speed = obj.getDouble("speed").toFloat()
                    if (obj.has("rgbLoop")) rgbLoop = obj.getBoolean("rgbLoop")
                    
                    if (obj.has("c1R")) c1R = obj.getDouble("c1R").toFloat()
                    if (obj.has("c1G")) c1G = obj.getDouble("c1G").toFloat()
                    if (obj.has("c1B")) c1B = obj.getDouble("c1B").toFloat()
                    
                    if (obj.has("c2R")) c2R = obj.getDouble("c2R").toFloat()
                    if (obj.has("c2G")) c2G = obj.getDouble("c2G").toFloat()
                    if (obj.has("c2B")) c2B = obj.getDouble("c2B").toFloat()
                    
                    if (obj.has("c3R")) c3R = obj.getDouble("c3R").toFloat()
                    if (obj.has("c3G")) c3G = obj.getDouble("c3G").toFloat()
                    if (obj.has("c3B")) c3B = obj.getDouble("c3B").toFloat()
                    
                    if (obj.has("c4R")) c4R = obj.getDouble("c4R").toFloat()
                    if (obj.has("c4G")) c4G = obj.getDouble("c4G").toFloat()
                    if (obj.has("c4B")) c4B = obj.getDouble("c4B").toFloat()

                    if (obj.has("globalColorEnabled")) globalColorEnabled = obj.getBoolean("globalColorEnabled")
                    if (obj.has("globalR")) globalR = obj.getDouble("globalR").toFloat()
                    if (obj.has("globalG")) globalG = obj.getDouble("globalG").toFloat()
                    if (obj.has("globalB")) globalB = obj.getDouble("globalB").toFloat()
                    
                    if (obj.has("rotA")) rotMultA = obj.getDouble("rotA").toFloat()
                    if (obj.has("rotB")) rotMultB = obj.getDouble("rotB").toFloat()
                    if (obj.has("rotC")) rotMultC = obj.getDouble("rotC").toFloat()
                    if (obj.has("rotD")) rotMultD = obj.getDouble("rotD").toFloat()
                    
                    if (obj.has("size1")) shape1Size = obj.getDouble("size1").toFloat()
                    if (obj.has("size2")) shape2Size = obj.getDouble("size2").toFloat()
                    if (obj.has("size3")) shape3Size = obj.getDouble("size3").toFloat()
                    if (obj.has("size4")) shape4Size = obj.getDouble("size4").toFloat()
                    
                    if (obj.has("thought")) aiCoachFeedback = obj.getString("thought")
                    else aiCoachFeedback = "Theme successfully generated!"

                    prefs.edit().apply {
                        putString("gimmick_wallpaper_type", wallpaperType)
                        putBoolean("gimmick_oled_enabled", oledEnabled)
                        putBoolean("gimmick_rotation_enabled", rotationEnabled)
                        putFloat("gimmick_speed", speed)
                        putBoolean("gimmick_rgb_loop", rgbLoop)
                        putFloat("gimmick_c1_r", c1R); putFloat("gimmick_c1_g", c1G); putFloat("gimmick_c1_b", c1B)
                        putFloat("gimmick_c2_r", c2R); putFloat("gimmick_c2_g", c2G); putFloat("gimmick_c2_b", c2B)
                        putFloat("gimmick_c3_r", c3R); putFloat("gimmick_c3_g", c3G); putFloat("gimmick_c3_b", c3B)
                        putFloat("gimmick_c4_r", c4R); putFloat("gimmick_c4_g", c4G); putFloat("gimmick_c4_b", c4B)
                        putBoolean("gimmick_global_color_enabled", globalColorEnabled)
                        putFloat("gimmick_global_r", globalR); putFloat("gimmick_global_g", globalG); putFloat("gimmick_global_b", globalB)
                        putFloat("gimmick_rot_mult_a", rotMultA); putFloat("gimmick_rot_mult_b", rotMultB); putFloat("gimmick_rot_mult_c", rotMultC); putFloat("gimmick_rot_mult_d", rotMultD)
                        putFloat("gimmick_shape1_size", shape1Size); putFloat("gimmick_shape2_size", shape2Size); putFloat("gimmick_shape3_size", shape3Size); putFloat("gimmick_shape4_size", shape4Size)
                    }.apply()

                    viewModel.triggerFireworksHaptic()
                    Toast.makeText(context, "Wallpaper style dynamically generated!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    aiCoachFeedback = "Could not parse AI formatting. ${e.localizedMessage}"
                } finally {
                    isAiGenerating = false
                }
            },
            onError = { errmsg ->
                aiCoachFeedback = "AI Coach failed: $errmsg"
                isAiGenerating = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (appLanguage == "el") "Κόπλο" else "Gimmick",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.triggerButtonHaptic()
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F1115),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0F1115) // Pure pixel slate black
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header description
                Text(
                    text = if (appLanguage == "el")
                        "Σχεδιάστε custom Aura Glow ή Material Σχήματα και ορίστε τα ως Live ή Στατική ταπετσαρία στη συσκευή σας!"
                        else "Configure dynamic Aura Glow or Material Shapes, fine-tune colors, spin speeds, or let the AI Coach design one for your device's home screen!",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    lineHeight = 18.sp
                )

                // 1. DYNAMIC PREVIEW BOX (High-Fidelity Handheld Phone Frame Mockup)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer Phone Bezel
                    Box(
                        modifier = Modifier
                            .width(178.dp)
                            .height(368.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .border(4.5.dp, Color(0xFF2C3036), RoundedCornerShape(32.dp))
                            .border(6.dp, Color(0xFF16181C), RoundedCornerShape(32.dp))
                            .background(if (oledEnabled) Color.Black else Color(0xFF121318))
                            .pointerInput(wallpaperType) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        dragStartOffset = offset
                                        dragAmountTotal = Offset.Zero
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragAmountTotal += dragAmount
                                    },
                                    onDragEnd = {
                                        val dist = kotlin.math.sqrt(dragAmountTotal.x * dragAmountTotal.x + dragAmountTotal.y * dragAmountTotal.y)
                                        if (dist > 30f) {
                                            // Solve shape centers to determine swipe hit target
                                            val w = widthPx
                                            val h = heightPx
                                            val dpScale = w / 360f

                                            val centerAX: Float
                                            val centerAY: Float
                                            val centerBX: Float
                                            val centerBY: Float
                                            val centerCX: Float
                                            val centerCY: Float
                                            val centerDX: Float
                                            val centerDY: Float

                                            if (wallpaperType == "AURA_GLOW") {
                                                centerAX = w * 0.15f + kotlin.math.sin(previewPhaseA) * (w * 0.06f)
                                                centerAY = h * 0.15f + kotlin.math.cos(previewPhaseA) * (h * 0.05f)

                                                centerBX = w * 0.85f + kotlin.math.cos(previewPhaseB) * (w * 0.07f)
                                                centerBY = h * 0.85f + kotlin.math.sin(previewPhaseB) * (h * 0.06f)

                                                centerCX = w * 0.10f + kotlin.math.sin(previewPhaseC) * (w * 0.05f)
                                                centerCY = h * 0.50f + kotlin.math.cos(previewPhaseC) * (h * 0.08f)

                                                centerDX = w * 0.90f + kotlin.math.cos(previewPhaseD) * (w * 0.06f)
                                                centerDY = h * 0.35f + kotlin.math.sin(previewPhaseD) * (h * 0.05f)
                                            } else {
                                                centerAX = 90f * dpScale
                                                centerAY = 130f * dpScale

                                                centerBX = w - 30f * dpScale
                                                centerBY = h - 35f * dpScale

                                                centerCX = 5f * dpScale
                                                centerCY = h / 2f + 130f * dpScale

                                                centerDX = w - 20f * dpScale
                                                centerDY = 340f * dpScale
                                            }

                                            val centers = listOf(
                                                Offset(centerAX, centerAY),
                                                Offset(centerBX, centerBY),
                                                Offset(centerCX, centerCY),
                                                Offset(centerDX, centerDY)
                                            )

                                            var closestIndex = 0
                                            var minDist = Float.MAX_VALUE
                                            for (i in centers.indices) {
                                                val dx = dragStartOffset.x - centers[i].x
                                                val dy = dragStartOffset.y - centers[i].y
                                                val d = kotlin.math.sqrt(dx * dx + dy * dy)
                                                if (d < minDist) {
                                                    minDist = d
                                                    closestIndex = i
                                                }
                                            }

                                            viewModel.triggerButtonHaptic()
                                            when (closestIndex) {
                                                0 -> {
                                                    rotMultA = -rotMultA
                                                    prefs.edit().putFloat("gimmick_rot_mult_a", rotMultA).apply()
                                                    Toast.makeText(context, if (appLanguage == "el") "Αντιστροφή φοράς πάνω-αριστερά σχήματος!" else "Flipped Top-Left Shape direction!", Toast.LENGTH_SHORT).show()
                                                }
                                                1 -> {
                                                    rotMultB = -rotMultB
                                                    prefs.edit().putFloat("gimmick_rot_mult_b", rotMultB).apply()
                                                    Toast.makeText(context, if (appLanguage == "el") "Αντιστροφή φοράς κάτω-δεξιά σχήματος!" else "Flipped Bottom-Right Shape direction!", Toast.LENGTH_SHORT).show()
                                                }
                                                2 -> {
                                                    rotMultC = -rotMultC
                                                    prefs.edit().putFloat("gimmick_rot_mult_c", rotMultC).apply()
                                                    Toast.makeText(context, if (appLanguage == "el") "Αντιστροφή φοράς κάτω-αριστερά σχήματος!" else "Flipped Bottom-Left Shape direction!", Toast.LENGTH_SHORT).show()
                                                }
                                                3 -> {
                                                    rotMultD = -rotMultD
                                                    prefs.edit().putFloat("gimmick_rot_mult_d", rotMultD).apply()
                                                    Toast.makeText(context, if (appLanguage == "el") "Αντιστροφή φοράς πάνω-δεξιά σχήματος!" else "Flipped Top-Right Shape direction!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Wallpaper Display Layer (Modeled as a physical device frame constraint)
                        Box(
                            modifier = Modifier
                                .width(184.dp)
                                .height(384.dp)
                                .background(Color(0xFF0F0F0F), RoundedCornerShape(24.dp))
                                .border(4.dp, Color(0xFF2C2C2C), RoundedCornerShape(24.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(172.dp)
                                    .height(372.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .pointerInput(wallpaperType) {
                                        detectTapGestures(
                                            onTap = { offset ->
                                                val w = size.width.toFloat()
                                                val h = size.height.toFloat()
                                                val dpScale = w / 360f

                                                val centerAX: Float
                                                val centerAY: Float
                                                val centerBX: Float
                                                val centerBY: Float
                                                val centerCX: Float
                                                val centerCY: Float
                                                val centerDX: Float
                                                val centerDY: Float

                                                if (wallpaperType == "AURA_GLOW") {
                                                    centerAX = w * 0.15f + kotlin.math.sin(previewPhaseA) * (w * 0.06f)
                                                    centerAY = h * 0.15f + kotlin.math.cos(previewPhaseA) * (h * 0.05f)

                                                    centerBX = w * 0.85f + kotlin.math.cos(previewPhaseB) * (w * 0.07f)
                                                    centerBY = h * 0.85f + kotlin.math.sin(previewPhaseB) * (h * 0.06f)

                                                    centerCX = w * 0.10f + kotlin.math.sin(previewPhaseC) * (w * 0.05f)
                                                    centerCY = h * 0.50f + kotlin.math.cos(previewPhaseC) * (h * 0.08f)

                                                    centerDX = w * 0.90f + kotlin.math.cos(previewPhaseD) * (w * 0.06f)
                                                    centerDY = h * 0.35f + kotlin.math.sin(previewPhaseD) * (h * 0.05f)
                                                } else {
                                                    centerAX = 90f * dpScale
                                                    centerAY = 130f * dpScale

                                                    centerBX = w - 30f * dpScale
                                                    centerBY = h - 35f * dpScale

                                                    centerCX = 5f * dpScale
                                                    centerCY = h / 2f + 130f * dpScale

                                                    centerDX = w - 20f * dpScale
                                                    centerDY = 340f * dpScale
                                                }

                                                val centers = listOf(
                                                    Offset(centerAX, centerAY),
                                                    Offset(centerBX, centerBY),
                                                    Offset(centerCX, centerCY),
                                                    Offset(centerDX, centerDY)
                                                )

                                                var closestIndex = 0
                                                var minDist = Float.MAX_VALUE
                                                for (i in centers.indices) {
                                                    val dx = offset.x - centers[i].x
                                                    val dy = offset.y - centers[i].y
                                                    val d = kotlin.math.sqrt(dx * dx + dy * dy)
                                                    if (d < minDist) {
                                                        minDist = d
                                                        closestIndex = i
                                                    }
                                                }

                                                rotationEnabled = !rotationEnabled
                                                prefs.edit().putBoolean("gimmick_rotation_enabled", rotationEnabled).apply()
                                                viewModel.triggerButtonHaptic()
                                                val shapeLabel = when (closestIndex) {
                                                    0 -> if (appLanguage == "el") "Πάνω-Αριστερά (Σχήμα A)" else "Top-Left (Shape A)"
                                                    1 -> if (appLanguage == "el") "Κάτω-Δεξιά (Σχήμα B)" else "Bottom-Right (Shape B)"
                                                    2 -> if (appLanguage == "el") "Κάτω-Αριστερά (Σχήμα C)" else "Bottom-Left (Shape C)"
                                                    3 -> if (appLanguage == "el") "Πάνω-Δεξιά (Σχήμα D)" else "Top-Right (Shape D)"
                                                    else -> "Shape"
                                                }
                                                Toast.makeText(
                                                    context,
                                                    if (appLanguage == "el") "Η περιστροφή άλλαξε κατάσταση για το $shapeLabel!" else "Rotation toggled (now ${if(rotationEnabled) "running" else "stopped"}) for $shapeLabel!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            onDoubleTap = {
                                                isPreviewingLockScreen = !isPreviewingLockScreen
                                                viewModel.triggerButtonHaptic()
                                                Toast.makeText(
                                                    context,
                                                    if (appLanguage == "el") "Εναλλαγή προεπισκόπησης: ${if (isPreviewingLockScreen) "Οθόνη Κλειδώματος" else "Αρχική Οθόνη"}" else "Preview Switched to: ${if (isPreviewingLockScreen) "Lock Screen" else "Home Screen"}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            onLongPress = { offset ->
                                                val w = size.width.toFloat()
                                                val h = size.height.toFloat()
                                                val dpScale = w / 360f

                                                val centerAX: Float
                                                val centerAY: Float
                                                val centerBX: Float
                                                val centerBY: Float
                                                val centerCX: Float
                                                val centerCY: Float
                                                val centerDX: Float
                                                val centerDY: Float

                                                if (wallpaperType == "AURA_GLOW") {
                                                    centerAX = w * 0.15f + kotlin.math.sin(previewPhaseA) * (w * 0.06f)
                                                    centerAY = h * 0.15f + kotlin.math.cos(previewPhaseA) * (h * 0.05f)

                                                    centerBX = w * 0.85f + kotlin.math.cos(previewPhaseB) * (w * 0.07f)
                                                    centerBY = h * 0.85f + kotlin.math.sin(previewPhaseB) * (h * 0.06f)

                                                    centerCX = w * 0.10f + kotlin.math.sin(previewPhaseC) * (w * 0.05f)
                                                    centerCY = h * 0.50f + kotlin.math.cos(previewPhaseC) * (h * 0.08f)

                                                    centerDX = w * 0.90f + kotlin.math.cos(previewPhaseD) * (w * 0.06f)
                                                    centerDY = h * 0.35f + kotlin.math.sin(previewPhaseD) * (h * 0.05f)
                                                } else {
                                                    centerAX = 90f * dpScale
                                                    centerAY = 130f * dpScale

                                                    centerBX = w - 30f * dpScale
                                                    centerBY = h - 35f * dpScale

                                                    centerCX = 5f * dpScale
                                                    centerCY = h / 2f + 130f * dpScale

                                                    centerDX = w - 20f * dpScale
                                                    centerDY = 340f * dpScale
                                                }

                                                val centers = listOf(
                                                    Offset(centerAX, centerAY),
                                                    Offset(centerBX, centerBY),
                                                    Offset(centerCX, centerCY),
                                                    Offset(centerDX, centerDY)
                                                )

                                                var closestIndex = 0
                                                var minDist = Float.MAX_VALUE
                                                for (i in centers.indices) {
                                                    val dx = offset.x - centers[i].x
                                                    val dy = offset.y - centers[i].y
                                                    val d = kotlin.math.sqrt(dx * dx + dy * dy)
                                                    if (d < minDist) {
                                                        minDist = d
                                                        closestIndex = i
                                                    }
                                                }

                                                selectedShapeIndex = closestIndex
                                                viewModel.triggerToggleSnapHaptic()
                                                val shapeLabel = when (closestIndex) {
                                                    0 -> if (appLanguage == "el") "Πάνω-Αριστερά (Σχήμα A)" else "Top-Left (Shape A)"
                                                    1 -> if (appLanguage == "el") "Κάτω-Δεξιά (Σχήμα B)" else "Bottom-Right (Shape B)"
                                                    2 -> if (appLanguage == "el") "Κάτω-Αριστερά (Σχήμα C)" else "Bottom-Left (Shape C)"
                                                    3 -> if (appLanguage == "el") "Πάνω-Δεξιά (Σχήμα D)" else "Top-Right (Shape D)"
                                                    else -> "Shape"
                                                }
                                                Toast.makeText(
                                                    context,
                                                    if (appLanguage == "el") "$shapeLabel επιλέχθηκε!" else "$shapeLabel selected for independent configuration!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        )
                                    }
                            ) {
                            ComposeCanvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                val dpScale = w / 360f

                                val resolvedPreviewBlurRadius = when {
                                    isPreviewingLockScreen -> {
                                        if (shapeBlurLockIndependent) shapeBlurRadiusLock else shapeBlurRadius
                                    }
                                    else -> { // Home screen
                                        if (shapeBlurTarget == "LOCK_ONLY") 0f else shapeBlurRadius
                                    }
                                }

                                if (wallpaperType == "AURA_GLOW") {
                                    val centerAX = w * 0.15f + kotlin.math.sin(previewPhaseA) * (w * 0.06f)
                                    val centerAY = h * 0.15f + kotlin.math.cos(previewPhaseA) * (h * 0.05f)
                                    val radiusA = w * 0.55f
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(evalC1.copy(alpha = (0.28f * contrastScale).coerceIn(0f, 1f)), Color.Transparent),
                                            center = Offset(centerAX, centerAY),
                                            radius = radiusA
                                        ),
                                        radius = radiusA,
                                        center = Offset(centerAX, centerAY)
                                    )

                                    val centerBX = w * 0.85f + kotlin.math.cos(previewPhaseB) * (w * 0.07f)
                                    val centerBY = h * 0.85f + kotlin.math.sin(previewPhaseB) * (h * 0.06f)
                                    val radiusB = w * 0.65f
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(evalC2.copy(alpha = (0.24f * contrastScale).coerceIn(0f, 1f)), Color.Transparent),
                                            center = Offset(centerBX, centerBY),
                                            radius = radiusB
                                        ),
                                        radius = radiusB,
                                        center = Offset(centerBX, centerBY)
                                    )

                                    val centerCX = w * 0.10f + kotlin.math.sin(previewPhaseC) * (w * 0.05f)
                                    val centerCY = h * 0.50f + kotlin.math.cos(previewPhaseC) * (h * 0.08f)
                                    val radiusC = w * 0.50f
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(evalC3.copy(alpha = (0.30f * contrastScale).coerceIn(0f, 1f)), Color.Transparent),
                                            center = Offset(centerCX, centerCY),
                                            radius = radiusC
                                        ),
                                        radius = radiusC,
                                        center = Offset(centerCX, centerCY)
                                    )

                                    val centerDX = w * 0.90f + kotlin.math.cos(previewPhaseD) * (w * 0.06f)
                                    val centerDY = h * 0.35f + kotlin.math.sin(previewPhaseD) * (h * 0.05f)
                                    val radiusD = w * 0.45f
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(evalC4.copy(alpha = (0.26f * contrastScale).coerceIn(0f, 1f)), Color.Transparent),
                                            center = Offset(centerDX, centerDY),
                                            radius = radiusD
                                        ),
                                        radius = radiusD,
                                        center = Offset(centerDX, centerDY)
                                    )
                                } else {
                                    val drawLobedShape: androidx.compose.ui.graphics.drawscope.DrawScope.(Float, Float, Float, Int, Float, Color, Float, Boolean, Float) -> Unit = { cx, cy, baseRadius, lobes, amplitude, colorInt, rotationAngle, isSin, shapeSpecificBlur ->
                                        val path = androidx.compose.ui.graphics.Path()
                                        val pointsCount = 180
                                        val cosR = kotlin.math.cos(Math.toRadians(rotationAngle.toDouble())).toFloat()
                                        val sinR = kotlin.math.sin(Math.toRadians(rotationAngle.toDouble())).toFloat()
                                            
                                        for (i in 0 until pointsCount) {
                                            val angleRad = (i * 2 * Math.PI / pointsCount).toFloat()
                                            val wave = if (isSin) kotlin.math.sin(lobes * angleRad) else kotlin.math.cos(lobes * angleRad)
                                            val r = baseRadius + amplitude * wave
                                            val rawX = cx + r * kotlin.math.cos(angleRad)
                                            val rawY = cy + r * kotlin.math.sin(angleRad)
                                            
                                            val dx = rawX - cx
                                            val dy = rawY - cy
                                            val rotatedX = cx + dx * cosR - dy * sinR
                                            val rotatedY = cy + dx * sinR + dy * cosR
                                            
                                            if (i == 0) path.moveTo(rotatedX, rotatedY) else path.lineTo(rotatedX, rotatedY)
                                        }
                                        path.close()
                                        val totalBlurRadius = resolvedPreviewBlurRadius + shapeSpecificBlur
                                        if (totalBlurRadius > 0f) {
                                            drawIntoCanvas { canvas ->
                                                val paint = androidx.compose.ui.graphics.Paint()
                                                val frameworkPaint = paint.asFrameworkPaint()
                                                frameworkPaint.isAntiAlias = true
                                                frameworkPaint.maskFilter = android.graphics.BlurMaskFilter(totalBlurRadius * dpScale, android.graphics.BlurMaskFilter.Blur.NORMAL)
                                                paint.color = colorInt
                                                canvas.drawPath(path, paint)
                                            }
                                        } else {
                                            drawPath(path = path, color = colorInt)
                                        }
                                    }

                                    val angleC = Math.toDegrees(previewPhaseC.toDouble()).toFloat()
                                    drawLobedShape(5f * dpScale, h / 2f + 130f * dpScale, 280f * 0.32f * dpScale * shape3Size, 3, 280f * 0.07f * dpScale * shape3Size, evalC3.copy(alpha = (0.28f * contrastScale * indVividC).coerceIn(0f, 1f)), angleC, false, indBlurC)

                                    val angleA = Math.toDegrees(previewPhaseA.toDouble()).toFloat()
                                    drawLobedShape(90f * dpScale, 130f * dpScale, 340f * 0.45f * dpScale * shape1Size, 4, 340f * 0.08f * dpScale * shape1Size, evalC1.copy(alpha = (0.26f * contrastScale * indVividA).coerceIn(0f, 1f)), angleA, false, indBlurA)

                                    val angleB = Math.toDegrees(previewPhaseB.toDouble()).toFloat()
                                    drawLobedShape(w - 30f * dpScale, h - 35f * dpScale, 340f * 0.33f * dpScale * shape2Size, 5, 340f * 0.06f * dpScale * shape2Size, evalC2.copy(alpha = (0.24f * contrastScale * indVividB).coerceIn(0f, 1f)), angleB, true, indBlurB)

                                    val angleD = Math.toDegrees(previewPhaseD.toDouble()).toFloat()
                                    drawLobedShape(w - 20f * dpScale, 340f * dpScale, 280f * 0.44f * dpScale * shape4Size, 6, 280f * 0.05f * dpScale * shape4Size, evalC4.copy(alpha = (0.22f * contrastScale * indVividD).coerceIn(0f, 1f)), angleD, true, indBlurD)
                                }

                                // Automatic Easter Egg Spaceship
                                if (pShipEnabled && shipInitialized) {
                                    val pathShip = androidx.compose.ui.graphics.Path()
                                    val S = shipSize * dpScale
                                    pathShip.moveTo(S, 0f)
                                    pathShip.cubicTo(S * 0.8f, -S * 0.3f, S * 0.2f, -S * 0.35f, -S * 0.3f, -S * 0.4f)
                                    pathShip.lineTo(-S * 0.7f, -S * 0.7f)
                                    pathShip.lineTo(-S * 0.8f, -S * 0.35f)
                                    pathShip.lineTo(-S * 0.6f, -S * 0.25f)
                                    pathShip.lineTo(-S * 0.8f, 0f)
                                    pathShip.lineTo(-S * 0.6f, S * 0.25f)
                                    pathShip.lineTo(-S * 0.8f, S * 0.35f)
                                    pathShip.lineTo(-S * 0.7f, S * 0.7f)
                                    pathShip.lineTo(-S * 0.3f, S * 0.4f)
                                    pathShip.cubicTo(S * 0.2f, S * 0.35f, S * 0.8f, S * 0.3f, S, 0f)
                                    pathShip.close()

                                    drawContext.canvas.save()
                                    drawContext.canvas.translate(shipX, shipY)
                                    drawContext.canvas.rotate(shipAngle)

                                    // Draw background masking field
                                    drawPath(
                                        path = pathShip,
                                        color = Color.Black.copy(alpha = (0.85f * shipContrast).coerceIn(0f, 1f)),
                                        style = androidx.compose.ui.graphics.drawscope.Fill
                                    )

                                    // Draw white profile line
                                    drawPath(
                                        path = pathShip,
                                        color = Color(shipR / 255f, shipG / 255f, shipB / 255f).copy(alpha = (1.0f * shipContrast).coerceIn(0f, 1f)),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.8f)
                                    )

                                    // Thruster Orange Jet Stream
                                    if (System.currentTimeMillis() % 200 < 100) {
                                        val firePath = androidx.compose.ui.graphics.Path()
                                        firePath.moveTo(-S * 0.8f, 0f)
                                        firePath.lineTo(-S * 1.2f, -S * 0.15f)
                                        firePath.lineTo(-S * 1.5f, 0f)
                                        firePath.lineTo(-S * 1.8f, 0f)
                                        firePath.lineTo(-S * 1.2f, S * 0.15f)
                                        firePath.close()
                                        drawPath(
                                            path = firePath,
                                            color = Color(0xFFFF5722).copy(alpha = (1.0f * shipContrast).coerceIn(0f, 1f)),
                                            style = androidx.compose.ui.graphics.drawscope.Fill
                                        )
                                    }

                                    drawContext.canvas.restore()
                                }
                            }

                            // Dynamic themed home screen widgets overlay (Clock, Sunny Weather, search Pill, app Dock)
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp, vertical = 14.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Top clock widget
                                Column {
                                    Text(
                                        text = "08:14",
                                        color = Color.White.copy(alpha = 0.95f),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontStyle = FontStyle.Normal
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = if (appLanguage == "el") "Κυρ 14 Ιουν" else "Sun, Jun 14",
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Icon(
                                            imageVector = Icons.Rounded.WbSunny,
                                            contentDescription = null,
                                            tint = Color(0xFFFFB300),
                                            modifier = Modifier.size(9.dp)
                                        )
                                        Text(
                                            text = "26°C",
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                // Bottom apps dock + search bar
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // App icons dock (circular themed glass widgets)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        val dockIcons = listOf(
                                            Icons.Rounded.Sms,
                                            Icons.Rounded.Forum,
                                            Icons.Rounded.Call,
                                            Icons.Rounded.PhotoLibrary,
                                            Icons.Rounded.Email
                                        )
                                        dockIcons.forEach { icon ->
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                                                    .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = null,
                                                    tint = Color.White.copy(alpha = 0.9f),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Search Bar Pill
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(26.dp)
                                            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(13.dp))
                                            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(13.dp))
                                            .padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            androidx.compose.foundation.Canvas(modifier = Modifier.size(11.dp)) {
                                                val scale = size.width / 24f
                                                val path = androidx.compose.ui.graphics.Path().apply {
                                                    moveTo(21.35f * scale, 11.1f * scale)
                                                    lineTo(12f * scale, 11.1f * scale)
                                                    lineTo(12f * scale, 14.9f * scale)
                                                    lineTo(17.38f * scale, 14.9f * scale)
                                                    cubicTo(
                                                        16.59f * scale, 15.82f * scale,
                                                        14.82f * scale, 18.2f * scale,
                                                        12f * scale, 18.2f * scale
                                                    )
                                                    cubicTo(
                                                        9.51f * scale, 18.2f * scale,
                                                        7.49f * scale, 16.15f * scale,
                                                        7.49f * scale, 13.5f * scale
                                                    )
                                                    cubicTo(
                                                        7.49f * scale, 10.85f * scale,
                                                        9.51f * scale, 8.8f * scale,
                                                        12f * scale, 8.8f * scale
                                                    )
                                                    cubicTo(
                                                        13.43f * scale, 8.8f * scale,
                                                        14.39f * scale, 9.42f * scale,
                                                        14.94f * scale, 9.94f * scale
                                                    )
                                                    lineTo(17.9f * scale, 7f * scale)
                                                    cubicTo(
                                                        16f * scale, 5.23f * scale,
                                                        13.57f * scale, 4.14f * scale,
                                                        12f * scale, 4.14f * scale
                                                    )
                                                    cubicTo(
                                                        6.8f * scale, 4.14f * scale,
                                                        2.57f * scale, 8.35f * scale,
                                                        2.57f * scale, 13.5f * scale
                                                    )
                                                    cubicTo(
                                                        2.57f * scale, 18.65f * scale,
                                                        6.8f * scale, 22.86f * scale,
                                                        12f * scale, 22.86f * scale
                                                    )
                                                    cubicTo(
                                                        17.43f * scale, 22.86f * scale,
                                                        22f * scale, 18.96f * scale,
                                                        22f * scale, 13.5f * scale
                                                    )
                                                    cubicTo(
                                                        22f * scale, 12.65f * scale,
                                                        21.89f * scale, 11.83f * scale,
                                                        21.35f * scale, 11.1f * scale
                                                    )
                                                    close()
                                                }
                                                drawPath(path = path, color = Color.White.copy(alpha = 0.6f))
                                            }
                                            Text(
                                                text = if (appLanguage == "el") "Αναζήτηση..." else "Search...",
                                                color = Color.White.copy(alpha = 0.5f),
                                                fontSize = 8.sp
                                            )
                                        }
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Mic,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.6f),
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Icon(
                                                imageVector = Icons.Rounded.CenterFocusWeak,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.6f),
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            } // Close nested Box
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Interactive Guide Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E212A).copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.TouchApp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (appLanguage == "el") "Δραστικές Δυνατότητες Προεπισκόπησης" else "Interactive Preview Gestures",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }
                        
                        Text(
                            text = if (appLanguage == "el") {
                                "• Αγγίξτε οποιοδήποτε σχήμα για να ξεκινήσει/σταματήσει η περιστροφή.\n" +
                                "• Πατήστε παρατεταμένα (Long Press) ένα σχήμα για να εμφανίσετε τις ανεξάρτητες επιλογές του (RGB, ταχύτητα, κορεσμός, ζωντάνια και θόλωμα).\n" +
                                "• Κάντε διπλό πάτημα (Double Tap) στην προεπισκόπηση για να αλλάξετε μεταξύ Αρχικής Οθόνης και Οθόνης Κλειδώματος."
                            } else {
                                "• Tap any shape to pause or resume its rotation animation.\n" +
                                "• Tap & Hold (Long Press) a shape to open independent RGB controls, rotation speed, saturation, vividness, and shape-only blur sliders.\n" +
                                "• Double Tap on the preview to easily toggle configure/preview focus between Home Screen and Lock Screen."
                            },
                            fontSize = 11.sp,
                            color = Color.LightGray,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Independent Shape Configuration Card
                androidx.compose.animation.AnimatedVisibility(visible = selectedShapeIndex != -1) {
                    val shapeLabel = when (selectedShapeIndex) {
                        0 -> if (appLanguage == "el") "Πάνω-Αριστερά (Σχήμα A)" else "Top-Left (Shape A)"
                        1 -> if (appLanguage == "el") "Κάτω-Δεξιά (Σχήμα B)" else "Bottom-Right (Shape B)"
                        2 -> if (appLanguage == "el") "Κάτω-Αριστερά (Σχήμα C)" else "Bottom-Left (Shape C)"
                        3 -> if (appLanguage == "el") "Πάνω-Δεξιά (Σχήμα D)" else "Top-Right (Shape D)"
                        else -> "Shape"
                    }
                    
                    val currentIndSpeed = when (selectedShapeIndex) {
                        0 -> indSpeedA; 1 -> indSpeedB; 2 -> indSpeedC; else -> indSpeedD
                    }
                    val currentIndSat = when (selectedShapeIndex) {
                        0 -> indSatA; 1 -> indSatB; 2 -> indSatC; else -> indSatD
                    }
                    val currentIndVivid = when (selectedShapeIndex) {
                        0 -> indVividA; 1 -> indVividB; 2 -> indVividC; else -> indVividD
                    }
                    val currentIndBlur = when (selectedShapeIndex) {
                        0 -> indBlurA; 1 -> indBlurB; 2 -> indBlurC; else -> indBlurD
                    }
                    val currentR = when (selectedShapeIndex) {
                        0 -> c1R; 1 -> c2R; 2 -> c3R; else -> c4R
                    }
                    val currentG = when (selectedShapeIndex) {
                        0 -> c1G; 1 -> c2G; 2 -> c3G; else -> c4G
                    }
                    val currentB = when (selectedShapeIndex) {
                        0 -> c1B; 1 -> c2B; 2 -> c3B; else -> c4B
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E212A)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.ColorLens, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (appLanguage == "el") "Ρυθμίσεις του $shapeLabel" else "$shapeLabel Custon Features",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            Color(currentR / 255f, currentG / 255f, currentB / 255f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                )
                            }

                            Text(
                                text = if (appLanguage == "el") {
                                    "Προσαρμόστε ανεξάρτητα τα χρώματα, την ταχύτητα περιστροφής, τον κορεσμό, τη ζωντάνια και το θόλωμα μόνο αυτού του σχήματος!"
                                } else {
                                    "Independently design the unique colors, spin speed, saturation, vividness level, and shape-only blur of this shape!"
                                },
                                fontSize = 11.sp,
                                color = Color.Gray,
                                lineHeight = 14.sp
                            )

                            // 1. Independent RGB Sliders
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (appLanguage == "el") "Χρώμα Σχήματος (RGB)" else "Shape Color (RGB Sliders)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.LightGray
                                )
                                
                                // Red
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Text("R", fontSize = 11.sp, color = Color.Red, modifier = Modifier.width(16.dp), fontWeight = FontWeight.Bold)
                                    Slider(
                                        value = currentR,
                                        onValueChange = { newVal ->
                                            when (selectedShapeIndex) {
                                                0 -> { c1R = newVal; prefs.edit().putFloat("gimmick_c1_r", newVal).apply() }
                                                1 -> { c2R = newVal; prefs.edit().putFloat("gimmick_c2_r", newVal).apply() }
                                                2 -> { c3R = newVal; prefs.edit().putFloat("gimmick_c3_r", newVal).apply() }
                                                3 -> { c4R = newVal; prefs.edit().putFloat("gimmick_c4_r", newVal).apply() }
                                            }
                                        },
                                        valueRange = 0f..255f,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text("${currentR.toInt()}", fontSize = 11.sp, color = Color.White, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                                }

                                // Green
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Text("G", fontSize = 11.sp, color = Color.Green, modifier = Modifier.width(16.dp), fontWeight = FontWeight.Bold)
                                    Slider(
                                        value = currentG,
                                        onValueChange = { newVal ->
                                            when (selectedShapeIndex) {
                                                0 -> { c1G = newVal; prefs.edit().putFloat("gimmick_c1_g", newVal).apply() }
                                                1 -> { c2G = newVal; prefs.edit().putFloat("gimmick_c2_g", newVal).apply() }
                                                2 -> { c3G = newVal; prefs.edit().putFloat("gimmick_c3_g", newVal).apply() }
                                                3 -> { c4G = newVal; prefs.edit().putFloat("gimmick_c4_g", newVal).apply() }
                                            }
                                        },
                                        valueRange = 0f..255f,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text("${currentG.toInt()}", fontSize = 11.sp, color = Color.White, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                                }

                                // Blue
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Text("B", fontSize = 11.sp, color = Color.Blue, modifier = Modifier.width(16.dp), fontWeight = FontWeight.Bold)
                                    Slider(
                                        value = currentB,
                                        onValueChange = { newVal ->
                                            when (selectedShapeIndex) {
                                                0 -> { c1B = newVal; prefs.edit().putFloat("gimmick_c1_b", newVal).apply() }
                                                1 -> { c2B = newVal; prefs.edit().putFloat("gimmick_c2_b", newVal).apply() }
                                                2 -> { c3B = newVal; prefs.edit().putFloat("gimmick_c3_b", newVal).apply() }
                                                3 -> { c4B = newVal; prefs.edit().putFloat("gimmick_c4_b", newVal).apply() }
                                            }
                                        },
                                        valueRange = 0f..255f,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text("${currentB.toInt()}", fontSize = 11.sp, color = Color.White, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                                }
                            }

                            Divider(color = Color.White.copy(alpha = 0.08f))

                            // 2. Independent Speed Slider
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (appLanguage == "el") "Ανεξάρτητη Ταχύτητα" else "Independent Speed",
                                        fontSize = 12.sp,
                                        color = Color.LightGray
                                    )
                                    Text(
                                        text = "${String.format("%.2f", currentIndSpeed)}x",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Slider(
                                    value = currentIndSpeed,
                                    onValueChange = { newVal ->
                                        when (selectedShapeIndex) {
                                            0 -> { indSpeedA = newVal; prefs.edit().putFloat("gimmick_ind_speed_a", newVal).apply() }
                                            1 -> { indSpeedB = newVal; prefs.edit().putFloat("gimmick_ind_speed_b", newVal).apply() }
                                            2 -> { indSpeedC = newVal; prefs.edit().putFloat("gimmick_ind_speed_c", newVal).apply() }
                                            3 -> { indSpeedD = newVal; prefs.edit().putFloat("gimmick_ind_speed_d", newVal).apply() }
                                        }
                                    },
                                    valueRange = 0.0f..3.0f
                                )
                            }

                            Divider(color = Color.White.copy(alpha = 0.08f))

                            // 3. Independent Saturation Slider
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (appLanguage == "el") "Ανεξάρτητος Κορεσμός" else "Independent Saturation",
                                        fontSize = 12.sp,
                                        color = Color.LightGray
                                    )
                                    Text(
                                        text = "${String.format("%.1f", currentIndSat * 100)}%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Slider(
                                    value = currentIndSat,
                                    onValueChange = { newVal ->
                                        when (selectedShapeIndex) {
                                            0 -> { indSatA = newVal; prefs.edit().putFloat("gimmick_ind_sat_a", newVal).apply() }
                                            1 -> { indSatB = newVal; prefs.edit().putFloat("gimmick_ind_sat_b", newVal).apply() }
                                            2 -> { indSatC = newVal; prefs.edit().putFloat("gimmick_ind_sat_c", newVal).apply() }
                                            3 -> { indSatD = newVal; prefs.edit().putFloat("gimmick_ind_sat_d", newVal).apply() }
                                        }
                                    },
                                    valueRange = 0.0f..2.0f
                                )
                            }

                            Divider(color = Color.White.copy(alpha = 0.08f))

                            // 4. Independent Vividness Slider
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (appLanguage == "el") "Ανεξάρτητη Ζωντάνια (Opacity/Alpha)" else "Independent Vividness (Alpha)",
                                        fontSize = 12.sp,
                                        color = Color.LightGray
                                    )
                                    Text(
                                        text = "${String.format("%.1f", currentIndVivid * 100)}%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Slider(
                                    value = currentIndVivid,
                                    onValueChange = { newVal ->
                                        when (selectedShapeIndex) {
                                            0 -> { indVividA = newVal; prefs.edit().putFloat("gimmick_ind_vivid_a", newVal).apply() }
                                            1 -> { indVividB = newVal; prefs.edit().putFloat("gimmick_ind_vivid_b", newVal).apply() }
                                            2 -> { indVividC = newVal; prefs.edit().putFloat("gimmick_ind_vivid_c", newVal).apply() }
                                            3 -> { indVividD = newVal; prefs.edit().putFloat("gimmick_ind_vivid_d", newVal).apply() }
                                        }
                                    },
                                    valueRange = 0.0f..3.0f
                                )
                            }

                            Divider(color = Color.White.copy(alpha = 0.08f))

                            // 5. Shape Only (not Background) Independent Shape Blur
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (appLanguage == "el") "Ανεξάρτητο Θόλωμα Σχήματος (Μόνο Σχήμα)" else "Shape-Only Independent Blur",
                                        fontSize = 12.sp,
                                        color = Color.LightGray
                                    )
                                    Text(
                                        text = if (currentIndBlur == 0f) {
                                            if (appLanguage == "el") "Αιχμηρό (Κανένα)" else "Sharp Edges (None)"
                                        } else {
                                            "${String.format("%.1f", currentIndBlur)} dp"
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Slider(
                                    value = currentIndBlur,
                                    onValueChange = { newVal ->
                                        when (selectedShapeIndex) {
                                            0 -> { indBlurA = newVal; prefs.edit().putFloat("gimmick_ind_blur_a", newVal).apply() }
                                            1 -> { indBlurB = newVal; prefs.edit().putFloat("gimmick_ind_blur_b", newVal).apply() }
                                            2 -> { indBlurC = newVal; prefs.edit().putFloat("gimmick_ind_blur_c", newVal).apply() }
                                            3 -> { indBlurD = newVal; prefs.edit().putFloat("gimmick_ind_blur_d", newVal).apply() }
                                        }
                                    },
                                    valueRange = 0.0f..30.0f
                                )
                            }

                            // Dismiss button
                            Button(
                                onClick = { selectedShapeIndex = -1 },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF23262D)),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(if (appLanguage == "el") "Κλείσιμο" else "Dismiss Configuration", fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. WALLPAPER TYPE & QUICK PRESET ACTIONS
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16181C)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = if (appLanguage == "el") "Επιλογή Σχεδιασμού" else "Design Mode Selection",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { updateWallpaperType("AURA_GLOW") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (wallpaperType == "AURA_GLOW") MaterialTheme.colorScheme.primary else Color(0xFF23262D)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.FilterDrama, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Aura Glow", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { updateWallpaperType("MATERIAL_SHAPES") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (wallpaperType == "MATERIAL_SHAPES") MaterialTheme.colorScheme.primary else Color(0xFF23262D)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.SquareFoot, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Shapes", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 3. AI COACH INTEGRATION
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C22)),
                    shape = RoundedCornerShape(16.dp),
                    border = borderStrokeWhiteLow()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color(0xFF91B1F9), modifier = Modifier.size(20.dp))
                            Text("AI Wallpaper Coach", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color.White)
                        }

                        Text(
                            text = if (appLanguage == "el")
                                "Περιγράψτε την ιδέα σας (π.χ. 'δροσερή θάλασσα' ή 'cyberpunk σκοτάδι') και το AI Coach θα ρυθμίσει τα χρώματα & την ταχύτητα!"
                                else "Describe your layout idea (e.g., 'calm ocean breeze with slow rotation' or 'cyberpunk neon matrix on oled screen') and let the AI formulate the design!",
                            fontSize = 11.sp,
                            color = Color.LightGray,
                            lineHeight = 15.sp
                        )

                        OutlinedTextField(
                            value = aiPrompt,
                            onValueChange = { aiPrompt = it },
                            placeholder = { Text("E.g., Dark pastel sunset violet and orange dream, extremely slow...", fontSize = 12.sp, color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF101115),
                                unfocusedContainerColor = Color(0xFF0C0D10),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            maxLines = 3
                        )

                        Button(
                            onClick = { askAiCoach() },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isAiGenerating,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B68DF))
                        ) {
                            if (isAiGenerating) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                            } else {
                                Icon(Icons.Rounded.DesignServices, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Let AI Coach Generate", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        if (aiCoachFeedback.isNotBlank()) {
                            Surface(
                                color = Color.White.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = aiCoachFeedback,
                                    fontSize = 11.sp,
                                    color = Color(0xFF91B1F9),
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(10.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // 4. PRESETS & CUSTOM COLOR RGB CONTROLS
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16181C)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = if (appLanguage == "el") "Παλέτες Χρωμάτων" else "Fixed Preset Palettes",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Preset 1: Pixel 10 Classic
                            PresetCircle(
                                colors = listOf(Color(0xFF4285F4), Color(0xFFEA4335), Color(0xFFFBBC05), Color(0xFF34A853)),
                                onClick = { applyPreset(listOf(Color(0xFF4285F4), Color(0xFFEA4335), Color(0xFFFBBC05), Color(0xFF34A853))) }
                            )
                            // Preset 2: Cyber Neon
                            PresetCircle(
                                colors = listOf(Color(0xFF00F5FF), Color(0xFFFF007F), Color(0xFF8A2BE2), Color(0xFF4B0082)),
                                onClick = { applyPreset(listOf(Color(0xFF00F5FF), Color(0xFFFF007F), Color(0xFF8A2BE2), Color(0xFF4B0082))) }
                            )
                            // Preset 3: Cosmic Amber Sunset
                            PresetCircle(
                                colors = listOf(Color(0xFFFFBF00), Color(0xFFFF4500), Color(0xFFFF007F), Color(0xFF4A00E0)),
                                onClick = { applyPreset(listOf(Color(0xFFFFBF00), Color(0xFFFF4500), Color(0xFFFF007F), Color(0xFF4A00E0))) }
                            )
                            // Preset 4: Emerald Sage
                            PresetCircle(
                                colors = listOf(Color(0xFF50C878), Color(0xFF00A86B), Color(0xFF98FF98), Color(0xFF004B49)),
                                onClick = { applyPreset(listOf(Color(0xFF50C878), Color(0xFF00A86B), Color(0xFF98FF98), Color(0xFF004B49))) }
                            )
                        }

                        Divider(color = Color.White.copy(alpha = 0.08f))

                        Text(
                            text = if (appLanguage == "el") "Λεπτομερής Ρύθμιση Χρωμάτων RGB" else "Detailed Fine Tuning RGB Sliders",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )

                        // Shape/Aura Glow Color Contrast Slider
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (appLanguage == "el") "Αντίθεση Σχημάτων & Αύρας" else "Shapes & Aura Contrast",
                                    fontSize = 13.sp,
                                    color = Color.LightGray
                                )
                                Text(
                                    text = when {
                                        contrastScale < 0.4f -> if (appLanguage == "el") "Εξαιρετικά Αχνό" else "Extremely Fade"
                                        contrastScale > 1.6f -> if (appLanguage == "el") "Εξαιρετικά Έντονο" else "Extremely Vivid"
                                        else -> "${String.format("%.1f", contrastScale * 100f)}%"
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = contrastScale,
                                onValueChange = {
                                    contrastScale = it
                                    prefs.edit().putFloat("gimmick_contrast_scale", it).apply()
                                },
                                valueRange = 0.1f..2.0f
                            )

                            // Shapes Saturation Slider
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (appLanguage == "el") "Κορεσμός Χρωμάτων Σχημάτων" else "Shapes Color Saturation",
                                        fontSize = 13.sp,
                                        color = Color.LightGray
                                    )
                                    Text(
                                        text = "${String.format("%.0f", shapesSaturation * 100f)}%",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Slider(
                                    value = shapesSaturation,
                                    onValueChange = {
                                        shapesSaturation = it
                                        prefs.edit().putFloat("gimmick_shapes_saturation", it).apply()
                                    },
                                    valueRange = 0.0f..2.0f
                                )
                            }
                        }

                        Divider(color = Color.White.copy(alpha = 0.08f))

                        // Global sync toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (appLanguage == "el") "Καθολικό Χρώμα Σχημάτων" else "Global Shape Color Override",
                                    fontSize = 13.sp,
                                    color = Color.LightGray
                                )
                                Text(
                                    text = if (appLanguage == "el") "Μονόχρωμος συγχρονισμός όλων των σχημάτων" else "Lock all shapes to a single fixed color",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                            Switch(
                                checked = globalColorEnabled,
                                onCheckedChange = {
                                    globalColorEnabled = it
                                    prefs.edit().putBoolean("gimmick_global_color_enabled", it).apply()
                                }
                            )
                        }

                        AnimatedVisibility(visible = globalColorEnabled) {
                            ColorSliderGroup(
                                label = if (appLanguage == "el") "Καθολικό Χρώμα (RGB)" else "Global Synced RGB Color",
                                color = resolveColor(globalR, globalG, globalB, hueShift),
                                onValueChange = { r, g, b ->
                                    globalR = r; globalG = g; globalB = b
                                    prefs.edit().putFloat("gimmick_global_r", r).putFloat("gimmick_global_g", g).putFloat("gimmick_global_b", b).apply()
                                }
                            )
                        }

                        AnimatedVisibility(visible = !globalColorEnabled) {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                // Color 1 slider
                                ColorSliderGroup(
                                    label = "Aura Segment A / Lobe 1",
                                    color = evaluatedC1,
                                    onValueChange = { r, g, b ->
                                        c1R = r; c1G = g; c1B = b
                                        prefs.edit().putFloat("gimmick_c1_r", r).putFloat("gimmick_c1_g", g).putFloat("gimmick_c1_b", b).apply()
                                    }
                                )

                                // Color 2 slider
                                ColorSliderGroup(
                                    label = "Aura Segment B / Lobe 2",
                                    color = evaluatedC2,
                                    onValueChange = { r, g, b ->
                                        c2R = r; c2G = g; c2B = b
                                        prefs.edit().putFloat("gimmick_c2_r", r).putFloat("gimmick_c2_g", g).putFloat("gimmick_c2_b", b).apply()
                                    }
                                )

                                // Color 3 slider
                                ColorSliderGroup(
                                    label = "Aura Segment C / Lobe 3",
                                    color = evaluatedC3,
                                    onValueChange = { r, g, b ->
                                        c3R = r; c3G = g; c3B = b
                                        prefs.edit().putFloat("gimmick_c3_r", r).putFloat("gimmick_c3_g", g).putFloat("gimmick_c3_b", b).apply()
                                    }
                                )

                                // Color 4 slider
                                ColorSliderGroup(
                                    label = "Aura Segment D / Lobe 4",
                                    color = evaluatedC4,
                                    onValueChange = { r, g, b ->
                                        c4R = r; c4G = g; c4B = b
                                        prefs.edit().putFloat("gimmick_c4_r", r).putFloat("gimmick_c4_g", g).putFloat("gimmick_c4_b", b).apply()
                                    }
                                )
                            }
                        }
                    }
                }

                // 5. MOTION ADJUSTMENTS & OLED TOGGLES
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16181C)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (appLanguage == "el") "Προσαρμογή Κίνησης & OLED" else "Motion Controls & OLED Preferences",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )

                        // Spin Rotation enabled
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Continuous Custom Spin and Rotations", fontSize = 13.sp, color = Color.LightGray)
                            Switch(checked = rotationEnabled, onCheckedChange = { updateRotation(it) })
                        }

                        // OLED screen enabled
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Pitch Black OLED Background", fontSize = 13.sp, color = Color.LightGray)
                            Switch(checked = oledEnabled, onCheckedChange = { updateOled(it) })
                        }

                        // RGB Loop pulsing
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("RGB Color Cycle Loop (Dynamic Hue)", fontSize = 13.sp, color = Color.LightGray)
                            Switch(checked = rgbLoop, onCheckedChange = { updateRgbLoop(it) })
                        }

                        // Android Easter Egg Spaceship
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (appLanguage == "el") "Διαστημόπλοιο Android Easter Egg" else "Android Easter Egg Spaceship",
                                    fontSize = 13.sp,
                                    color = Color.LightGray
                                )
                                Text(
                                    text = if (appLanguage == "el") "Αυτόματη πτήση, προσγείωση και απογείωση" else "Automatic flight, landing & launch cycle",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                            Switch(checked = pShipEnabled, onCheckedChange = { updateSpaceship(it) })
                        }

                        // Rotation speed slider
                        AnimatedVisibility(visible = rotationEnabled) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Spin Speed Multiplier", fontSize = 13.sp, color = Color.LightGray)
                                        Text("${String.format("%.2f", speed)}x", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Slider(
                                        value = speed,
                                        onValueChange = { updateSpeed(it) },
                                        valueRange = 0.1f..4.0f
                                    )
                                }

                                // Custom Independent Shape/Aura Speed Multipliers as requested
                                var showIndependentSpeeds by remember { mutableStateOf(false) }
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showIndependentSpeeds = !showIndependentSpeeds }
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (appLanguage == "el") "Ανεξάρτητη Ταχύτητα Σχημάτων" else "Independent Shape Speeds",
                                            fontSize = 13.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            imageVector = if (showIndependentSpeeds) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                            contentDescription = null,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    AnimatedVisibility(visible = showIndependentSpeeds) {
                                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            // Shape A Speed Multiplier
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("Top Left Shape / Lobe 1 Speed", fontSize = 11.sp, color = Color.Gray)
                                                    Text("${String.format("%.2f", rotMultA)}x", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                }
                                                Slider(
                                                    value = rotMultA,
                                                    onValueChange = {
                                                        rotMultA = it
                                                        prefs.edit().putFloat("gimmick_rot_mult_a", it).apply()
                                                    },
                                                    valueRange = -2.0f..2.0f
                                                )
                                            }

                                            // Shape B Speed Multiplier
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("Bottom Right Shape / Lobe 2 Speed", fontSize = 11.sp, color = Color.Gray)
                                                    Text("${String.format("%.2f", rotMultB)}x", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                }
                                                Slider(
                                                    value = rotMultB,
                                                    onValueChange = {
                                                        rotMultB = it
                                                        prefs.edit().putFloat("gimmick_rot_mult_b", it).apply()
                                                    },
                                                    valueRange = -2.0f..2.0f
                                                )
                                            }

                                            // Shape C Speed Multiplier
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("Middle Left Shape / Lobe 3 Speed", fontSize = 11.sp, color = Color.Gray)
                                                    Text("${String.format("%.2f", rotMultC)}x", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                }
                                                Slider(
                                                    value = rotMultC,
                                                    onValueChange = {
                                                        rotMultC = it
                                                        prefs.edit().putFloat("gimmick_rot_mult_c", it).apply()
                                                    },
                                                    valueRange = -2.0f..2.0f
                                                )
                                            }

                                            // Shape D Speed Multiplier
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("Middle Right Shape / Lobe 4 Speed", fontSize = 11.sp, color = Color.Gray)
                                                    Text("${String.format("%.2f", rotMultD)}x", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                }
                                                Slider(
                                                    value = rotMultD,
                                                    onValueChange = {
                                                        rotMultD = it
                                                        prefs.edit().putFloat("gimmick_rot_mult_d", it).apply()
                                                    },
                                                    valueRange = -2.0f..2.0f
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 5.5 SPACESHIP CUSTOMIZATION & INDEPENDENT DESIGN
                AnimatedVisibility(visible = pShipEnabled) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF16181C)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = if (appLanguage == "el") "Προσαρμογή Διαστημοπλοίου" else "Spaceship Parameters & Design",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )

                            // Spaceship Size Slider
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (appLanguage == "el") "Μέγεθος Διαστημοπλοίου" else "Spaceship Size",
                                        fontSize = 13.sp,
                                        color = Color.LightGray
                                    )
                                    Text(
                                        text = "${shipSize.toInt()} dp",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Slider(
                                    value = shipSize,
                                    onValueChange = {
                                        shipSize = it
                                        prefs.edit().putFloat("gimmick_spaceship_size", it).apply()
                                    },
                                    valueRange = 6.0f..30.0f
                                )
                            }

                            // Spaceship Contrast Slider
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (appLanguage == "el") "Αντίθεση Διαστημοπλοίου" else "Spaceship Contrast / Opacity",
                                        fontSize = 13.sp,
                                        color = Color.LightGray
                                    )
                                    Text(
                                        text = when {
                                            shipContrast < 0.4f -> if (appLanguage == "el") "Αχνό" else "Fade"
                                            shipContrast > 1.2f -> if (appLanguage == "el") "Έντονο" else "Vivid"
                                            else -> "${String.format("%.1f", shipContrast * 100f)}%"
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Slider(
                                    value = shipContrast,
                                    onValueChange = {
                                        shipContrast = it
                                        prefs.edit().putFloat("gimmick_spaceship_contrast_scale", it).apply()
                                    },
                                    valueRange = 0.1f..1.5f
                                )
                            }

                            Divider(color = Color.White.copy(alpha = 0.08f))

                            // Spaceship Speed Slider
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (appLanguage == "el") "Ταχύτητα Διαστημοπλοίου" else "Spaceship Speed",
                                        fontSize = 13.sp,
                                        color = Color.LightGray
                                    )
                                    Text(
                                        text = String.format("%.1fx", shipSpeedMultiplier),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Slider(
                                    value = shipSpeedMultiplier,
                                    onValueChange = {
                                        shipSpeedMultiplier = it
                                        prefs.edit().putFloat("gimmick_spaceship_speed", it).apply()
                                    },
                                    valueRange = 0.1f..5.0f
                                )
                            }

                            // Spaceship Landing Duration Slider
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (appLanguage == "el") "Χρόνος Προσγείωσης" else "Landing Time (sec)",
                                        fontSize = 13.sp,
                                        color = Color.LightGray
                                    )
                                    Text(
                                        text = String.format("%.1fs", shipLandingTime),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Slider(
                                    value = shipLandingTime,
                                    onValueChange = {
                                        shipLandingTime = it
                                        prefs.edit().putFloat("gimmick_spaceship_landing_time", it).apply()
                                    },
                                    valueRange = 0.1f..10.0f
                                )
                            }
                            
                            // Spaceship Landing Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (appLanguage == "el") "Προσγείωση Σκαφών" else "Enable Landing",
                                    fontSize = 13.sp,
                                    color = Color.LightGray
                                )
                                Switch(
                                    checked = shipLandingEnabled,
                                    onCheckedChange = {
                                        shipLandingEnabled = it
                                        prefs.edit().putBoolean("gimmick_spaceship_landing_enabled", it).apply()
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFAECBFA), checkedTrackColor = Color(0xFF1967D2))
                                )
                            }

                            // Spaceship Anti Crush Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (appLanguage == "el") "Αντι-σύνθλιψη" else "Anti-Crush Mode",
                                    fontSize = 13.sp,
                                    color = Color.LightGray
                                )
                                Switch(
                                    checked = shipAntiCrush,
                                    onCheckedChange = {
                                        shipAntiCrush = it
                                        prefs.edit().putBoolean("gimmick_spaceship_anti_crush", it).apply()
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFAECBFA), checkedTrackColor = Color(0xFF1967D2))
                                )
                            }

                            // Spaceship Cautiousness Slider
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (appLanguage == "el") "Προσοχή Αποφυγής Διαστημοπλοίου" else "Spaceship Avoidance Cautiousness",
                                        fontSize = 13.sp,
                                        color = Color.LightGray
                                    )
                                    Text(
                                        text = String.format("%.2f", shipCautiousness),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Slider(
                                    value = shipCautiousness,
                                    onValueChange = {
                                        shipCautiousness = it
                                        prefs.edit().putFloat("gimmick_spaceship_cautiousness", it).apply()
                                    },
                                    valueRange = 0.0f..1.0f
                                )
                            }
                            
                            Divider(color = Color.White.copy(alpha = 0.08f))

                            // Spaceship RGB Profile Sliders
                            ColorSliderGroup(
                                label = if (appLanguage == "el") "Χρώμα Διαστημοπλοίου (RGB)" else "Spaceship Custom RGB Color",
                                color = Color(shipR / 255f, shipG / 255f, shipB / 255f),
                                onValueChange = { r, g, b ->
                                    shipR = r; shipG = g; shipB = b
                                    prefs.edit().putFloat("gimmick_ship_r", r).putFloat("gimmick_ship_g", g).putFloat("gimmick_ship_b", b).apply()
                                }
                            )
                        }
                    }
                }

                // 5.6 SHAPE SIZES
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16181C)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (appLanguage == "el") "Μεγέθη Σχημάτων" else "Shape Sizes",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            val isAnyNotDefault = kotlin.math.abs(shape1Size - 0.95f) > 0.01f ||
                                                  kotlin.math.abs(shape2Size - 1.41f) > 0.01f ||
                                                  kotlin.math.abs(shape3Size - 1.35f) > 0.01f ||
                                                  kotlin.math.abs(shape4Size - 0.99f) > 0.01f
                            AnimatedVisibility(
                                visible = isAnyNotDefault,
                                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandHorizontally(expandFrom = Alignment.End),
                                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkHorizontally(shrinkTowards = Alignment.End)
                            ) {
                                IconButton(
                                    onClick = {
                                        shape1Size = 0.95f
                                        shape2Size = 1.41f
                                        shape3Size = 1.35f
                                        shape4Size = 0.99f
                                        prefs.edit()
                                            .putFloat("gimmick_shape1_size", 0.95f)
                                            .putFloat("gimmick_shape2_size", 1.41f)
                                            .putFloat("gimmick_shape3_size", 1.35f)
                                            .putFloat("gimmick_shape4_size", 0.99f)
                                            .apply()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = "Reset to Defaults",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Shape 1 Size Slider
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Top Left Shape Size", fontSize = 13.sp, color = Color.LightGray)
                                Text(text = String.format("%.2fx", shape1Size), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = shape1Size,
                                onValueChange = { shape1Size = it; prefs.edit().putFloat("gimmick_shape1_size", it).apply() },
                                valueRange = 0.2f..3.0f
                            )
                        }
                        
                        // Shape 2 Size Slider
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Bottom Right Shape Size", fontSize = 13.sp, color = Color.LightGray)
                                Text(text = String.format("%.2fx", shape2Size), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = shape2Size,
                                onValueChange = { shape2Size = it; prefs.edit().putFloat("gimmick_shape2_size", it).apply() },
                                valueRange = 0.2f..3.0f
                            )
                        }

                        // Shape 3 Size Slider
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Middle Left Shape Size", fontSize = 13.sp, color = Color.LightGray)
                                Text(text = String.format("%.2fx", shape3Size), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = shape3Size,
                                onValueChange = { shape3Size = it; prefs.edit().putFloat("gimmick_shape3_size", it).apply() },
                                valueRange = 0.2f..3.0f
                            )
                        }

                        // Shape 4 Size Slider
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Middle Right Shape Size", fontSize = 13.sp, color = Color.LightGray)
                                Text(text = String.format("%.2fx", shape4Size), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = shape4Size,
                                onValueChange = { shape4Size = it; prefs.edit().putFloat("gimmick_shape4_size", it).apply() },
                                valueRange = 0.2f..3.0f
                            )
                        }
                    }
                }

                // 2. SHAPE BLUR AND ORGANIC GLOW EFFECTS
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16181C)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = if (appLanguage == "el") "Οργανικό Θόλωμα Σχημάτων" else "Shape Blur & Organic Glow Effects",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (appLanguage == "el") "Εφαρμόστε ένα φίλτρο θολώματος στα σχήματα για ατμοσφαιρικό, νεφελώδη φωτισμό." else "Apply an organic blur mask to shapes. High values result in a diffused, glowing, cloud-like ambient backdrop.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            lineHeight = 16.sp
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (appLanguage == "el") "Ακτίνα Θολώματος" else "Shape Blur Radius",
                                    fontSize = 13.sp,
                                    color = Color.LightGray
                                )
                                Text(
                                    text = if (shapeBlurRadius == 0f) {
                                        if (appLanguage == "el") "Σκληρές Γωνίες (Κανένα)" else "Sharp Edges (None)"
                                    } else {
                                        "${String.format("%.1f", shapeBlurRadius)} dp"
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = shapeBlurRadius,
                                onValueChange = {
                                    shapeBlurRadius = it
                                    prefs.edit().putFloat("gimmick_shape_blur_radius", it).apply()
                                },
                                valueRange = 0.0f..30.0f
                            )
                        }

                        // Target Screen Selector (Both vs Lock Screen Only)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = if (appLanguage == "el") "Εμφάνιση Θολώματος σε" else "Apply Shape Blur To",
                                fontSize = 13.sp,
                                color = Color.LightGray
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val targets = listOf("BOTH", "LOCK_ONLY")
                                val labels = if (appLanguage == "el") {
                                    listOf("Αρχική & Κλείδωμα", "Μόνο Κλείδωμα")
                                } else {
                                    listOf("Both Home & Lock", "Lock Screen Only")
                                }
                                targets.forEachIndexed { index, targetVal ->
                                    val isSelected = shapeBlurTarget == targetVal
                                    Button(
                                        onClick = {
                                            shapeBlurTarget = targetVal
                                            prefs.edit().putString("gimmick_shape_blur_target", targetVal).apply()
                                        },
                                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF232730),
                                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.LightGray
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        Text(text = labels[index], fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Divider(color = Color.White.copy(alpha = 0.08f))

                        // Independent Lock Screen Blur Switch Group
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (appLanguage == "el") "Ανεξάρτητο Θόλωμα στην Οθόνη Κλειδώματος" else "Independent Lock Screen Blur",
                                    fontSize = 13.sp,
                                    color = Color.LightGray
                                )
                                Text(
                                    text = if (appLanguage == "el") "Ορίστε διαφορετική ένταση θολώματος για την οθόνη κλειδώματος." else "Configure a different blur intensity specifically for the lock screen.",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    lineHeight = 14.sp
                                )
                            }
                            Switch(
                                checked = shapeBlurLockIndependent,
                                onCheckedChange = {
                                    shapeBlurLockIndependent = it
                                    prefs.edit().putBoolean("gimmick_shape_blur_lock_independent", it).apply()
                                }
                            )
                        }

                        if (shapeBlurLockIndependent) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (appLanguage == "el") "Ακτίνα Θολώματος Οθόνης Κλειδώματος" else "Lock Screen Shape Blur Radius",
                                        fontSize = 13.sp,
                                        color = Color.LightGray
                                    )
                                    Text(
                                        text = if (shapeBlurRadiusLock == 0f) {
                                            if (appLanguage == "el") "Σκληρές Γωνίες (Κανένα)" else "Sharp Edges (None)"
                                        } else {
                                            "${String.format("%.1f", shapeBlurRadiusLock)} dp"
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Slider(
                                    value = shapeBlurRadiusLock,
                                    onValueChange = {
                                        shapeBlurRadiusLock = it
                                        prefs.edit().putFloat("gimmick_shape_blur_radius_lock", it).apply()
                                    },
                                    valueRange = 0.0f..30.0f
                                )
                            }
                        }
                    }
                }

                // 3. LOCK SCREEN SPECIFIC CUSTOMIZATION
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16181C)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (appLanguage == "el") "Διαφορετικά Χρώματα Lock Screen" else "Lock Screen Custom Colors",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (isPreviewingLockScreen) "Preview State: Lock" else "Preview State: Home",
                                    fontSize = 11.sp,
                                    color = if (isPreviewingLockScreen) MaterialTheme.colorScheme.primary else Color.Gray,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Switch(
                                    checked = isPreviewingLockScreen,
                                    onCheckedChange = {
                                        isPreviewingLockScreen = it
                                        viewModel.triggerButtonHaptic()
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }
                        }

                        Text(
                            text = if (appLanguage == "el") "Ορίστε ξεχωριστά χρώματα για την οθόνη κλειδώματος, διατηρώντας όλες τις άλλες ρυθμίσεις ίδιες (ταχύτητα, μέγεθος, κατεύθυνση)." else "Define distinct custom colors specifically for your Lock Screen while maintaining identical spin speed, shape sizes, and directions.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            lineHeight = 16.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (appLanguage == "el") "Ενεργοποίηση Χρωμάτων Lock Screen" else "Enable Lock Screen Colors",
                                fontSize = 13.sp,
                                color = Color.LightGray
                            )
                            Switch(
                                checked = lockscreenColorsEnabled,
                                onCheckedChange = {
                                    lockscreenColorsEnabled = it
                                    prefs.edit().putBoolean("gimmick_lockscreen_colors_enabled", it).apply()
                                    viewModel.triggerButtonHaptic()
                                }
                            )
                        }

                        AnimatedVisibility(visible = lockscreenColorsEnabled) {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Divider(color = Color.White.copy(alpha = 0.08f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (appLanguage == "el") "Καθολικό Χρώμα Lock Screen" else "Lock Screen Global Override",
                                            fontSize = 13.sp,
                                            color = Color.LightGray
                                        )
                                        Text(
                                            text = if (appLanguage == "el") "Κλειδώστε όλα τα σχήματα σε ένα μόνο χρώμα" else "Lock all shapes on Lock Screen to a single color",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    Switch(
                                        checked = lockscreenGlobalColorEnabled,
                                        onCheckedChange = {
                                            lockscreenGlobalColorEnabled = it
                                            prefs.edit().putBoolean("gimmick_lockscreen_global_color_enabled", it).apply()
                                            viewModel.triggerButtonHaptic()
                                        }
                                    )
                                }

                                AnimatedVisibility(visible = lockscreenGlobalColorEnabled) {
                                    ColorSliderGroup(
                                        label = if (appLanguage == "el") "Καθολικό Χρώμα (RGB)" else "Global Synced Lock Screen Color",
                                        color = Color(lockscreenGlobalR / 255f, lockscreenGlobalG / 255f, lockscreenGlobalB / 255f),
                                        onValueChange = { r, g, b ->
                                            lockscreenGlobalR = r; lockscreenGlobalG = g; lockscreenGlobalB = b
                                            prefs.edit()
                                                .putFloat("gimmick_lockscreen_global_r", r)
                                                .putFloat("gimmick_lockscreen_global_g", g)
                                                .putFloat("gimmick_lockscreen_global_b", b)
                                                .apply()
                                        }
                                    )
                                }

                                AnimatedVisibility(visible = !lockscreenGlobalColorEnabled) {
                                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                        ColorSliderGroup(
                                            label = "Lock Screen Aura A / Lobe 1",
                                            color = Color(lockscreenC1R / 255f, lockscreenC1G / 255f, lockscreenC1B / 255f),
                                            onValueChange = { r, g, b ->
                                                lockscreenC1R = r; lockscreenC1G = g; lockscreenC1B = b
                                                prefs.edit()
                                                    .putFloat("gimmick_lockscreen_c1_r", r)
                                                    .putFloat("gimmick_lockscreen_c1_g", g)
                                                    .putFloat("gimmick_lockscreen_c1_b", b)
                                                    .apply()
                                            }
                                        )

                                        ColorSliderGroup(
                                            label = "Lock Screen Aura B / Lobe 2",
                                            color = Color(lockscreenC2R / 255f, lockscreenC2G / 255f, lockscreenC2B / 255f),
                                            onValueChange = { r, g, b ->
                                                lockscreenC2R = r; lockscreenC2G = g; lockscreenC2B = b
                                                prefs.edit()
                                                    .putFloat("gimmick_lockscreen_c2_r", r)
                                                    .putFloat("gimmick_lockscreen_c2_g", g)
                                                    .putFloat("gimmick_lockscreen_c2_b", b)
                                                    .apply()
                                            }
                                        )

                                        ColorSliderGroup(
                                            label = "Lock Screen Aura C / Lobe 3",
                                            color = Color(lockscreenC3R / 255f, lockscreenC3G / 255f, lockscreenC3B / 255f),
                                            onValueChange = { r, g, b ->
                                                lockscreenC3R = r; lockscreenC3G = g; lockscreenC3B = b
                                                prefs.edit()
                                                    .putFloat("gimmick_lockscreen_c3_r", r)
                                                    .putFloat("gimmick_lockscreen_c3_g", g)
                                                    .putFloat("gimmick_lockscreen_c3_b", b)
                                                    .apply()
                                            }
                                        )

                                        ColorSliderGroup(
                                            label = "Lock Screen Aura D / Lobe 4",
                                            color = Color(lockscreenC4R / 255f, lockscreenC4G / 255f, lockscreenC4B / 255f),
                                            onValueChange = { r, g, b ->
                                                lockscreenC4R = r; lockscreenC4G = g; lockscreenC4B = b
                                                prefs.edit()
                                                    .putFloat("gimmick_lockscreen_c4_r", r)
                                                    .putFloat("gimmick_lockscreen_c4_g", g)
                                                    .putFloat("gimmick_lockscreen_c4_b", b)
                                                    .apply()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. ACTION APPLY BUTTONS
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Button(
                        onClick = { setAsLiveWallpaper() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Rounded.Wallpaper, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (appLanguage == "el") "Ορισμός ως LIVE Wallpaper" else "Apply as LIVE Wallpaper",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    OutlinedButton(
                        onClick = { setAsStaticWallpaper() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = borderStrokeWhiteLow()
                    ) {
                        Icon(Icons.Rounded.PhotoSizeSelectActual, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (appLanguage == "el") "Ορισμός ως ΣΤΑΤΙΚΗ Wallpaper" else "Apply as STATIC Wallpaper",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetCircle(colors: List<Color>, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(45.dp)
            .clip(CircleShape)
            .clickable { onClick() }
            .background(
                Brush.sweepGradient(
                    colors = colors
                )
            )
            .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape)
    )
}

@Composable
private fun ColorSliderGroup(
    label: String,
    color: Color,
    onValueChange: (Float, Float, Float) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                )
                Text(label, fontSize = 12.sp, color = Color.White)
            }
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // R Red
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("R: ${(color.red * 255).toInt()}", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.width(36.dp))
                    Slider(
                        value = color.red * 255f,
                        onValueChange = { onValueChange(it, color.green * 255f, color.blue * 255f) },
                        valueRange = 0f..255f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = Color.Red, activeTrackColor = Color.Red.copy(alpha = 0.5f))
                    )
                }

                // G Green
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("G: ${(color.green * 255).toInt()}", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.width(36.dp))
                    Slider(
                        value = color.green * 255f,
                        onValueChange = { onValueChange(color.red * 255f, it, color.blue * 255f) },
                        valueRange = 0f..255f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = Color.Green, activeTrackColor = Color.Green.copy(alpha = 0.5f))
                    )
                }

                // B Blue
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("B: ${(color.blue * 255).toInt()}", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.width(36.dp))
                    Slider(
                        value = color.blue * 255f,
                        onValueChange = { onValueChange(color.red * 255f, color.green * 255f, it) },
                        valueRange = 0f..255f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = Color.Blue, activeTrackColor = Color.Blue.copy(alpha = 0.5f))
                    )
                }
            }
        }
    }
}

@Composable
private fun borderStrokeWhiteLow() = androidx.compose.foundation.BorderStroke(
    width = 1.dp,
    color = Color.White.copy(alpha = 0.10f)
)
