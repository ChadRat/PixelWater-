package com.pixelwater.app.ui

import android.service.wallpaper.WallpaperService
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Color as AndroidColor
import android.os.Handler
import android.os.Looper
import android.view.SurfaceHolder
import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class GlowShapesWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return GlowEngine()
    }

    inner class GlowEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private val drawRunnable = Runnable { drawFrame() }
        private var visible = false

        // Load preferences
        private val prefs: SharedPreferences by lazy {
            this@GlowShapesWallpaperService.getSharedPreferences("water_tracker_prefs", Context.MODE_PRIVATE)
        }

        private var lastTime = System.nanoTime()
        private var phaseA = 0f
        private var phaseB = 45f
        private var phaseC = 90f
        private var phaseD = 120f

        // Spaceship automated physics state
        private var shipInitialized = false
        private var shipX = 0f
        private var shipY = 0f
        private var shipVx = 0f
        private var shipVy = 0f
        private var shipAngle = 0f
        private var shipTargetX = 0f
        private var shipTargetY = 0f
        private var shipState = "FLYING" // "FLYING" or "LANDED"
        private var shipLandedTimer = 0f
        private var shipTargetIndex = -1

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                lastTime = System.nanoTime()
                handler.post(drawRunnable)
            } else {
                handler.removeCallbacks(drawRunnable)
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            super.onSurfaceDestroyed(holder)
            visible = false
            handler.removeCallbacks(drawRunnable)
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    renderWallpaper(canvas)
                }
            } catch (e: Exception) {
                Log.e("WallpaperService", "Error drawing frame", e)
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        Log.e("WallpaperService", "Error unlocking canvas", e)
                    }
                }
            }

            handler.removeCallbacks(drawRunnable)
            if (visible) {
                // Read rotation from SharedPreferences
                val rotationEnabled = prefs.getBoolean("gimmick_rotation_enabled", true)
                if (rotationEnabled) {
                    handler.postDelayed(drawRunnable, 16) // ~60fps
                } else {
                    handler.postDelayed(drawRunnable, 100) // Lower refresh rates when static
                }
            }
        }

        private fun renderWallpaper(canvas: Canvas) {
            val now = System.nanoTime()
            val deltaSec = (now - lastTime) / 1_000_000_000f
            lastTime = now

            // Retrieve current gimmick configurations
            val wallpaperType = prefs.getString("gimmick_wallpaper_type", "MATERIAL_SHAPES") ?: "MATERIAL_SHAPES"
            val speed = (prefs.getFloat("gimmick_speed", 1.0f) * 0.6f).coerceIn(0.01f, 0.6f)
            val oledEnabled = prefs.getBoolean("gimmick_oled_enabled", false)
            val rotationEnabled = prefs.getBoolean("gimmick_rotation_enabled", true)
            val rgbLoop = prefs.getBoolean("gimmick_rgb_loop", false)
            var hueShift = prefs.getFloat("gimmick_hue_shift", 0f)

            // Dynamic/RGB color shift if RGB loop is enabled
            if (rgbLoop && rotationEnabled && visible) {
                hueShift = (hueShift + 25f * speed * deltaSec) % 360f
                prefs.edit().putFloat("gimmick_hue_shift", hueShift).apply()
            }

            // Retrieve independent rotation multipliers and speeds
            val rotMultA = prefs.getFloat("gimmick_rot_mult_a", 1.0f)
            val rotMultB = prefs.getFloat("gimmick_rot_mult_b", -0.727f)
            val rotMultC = prefs.getFloat("gimmick_rot_mult_c", 0.533f)
            val rotMultD = prefs.getFloat("gimmick_rot_mult_d", -0.615f)

            val indSpeedA = prefs.getFloat("gimmick_ind_speed_a", 1.0f)
            val indSpeedB = prefs.getFloat("gimmick_ind_speed_b", 1.0f)
            val indSpeedC = prefs.getFloat("gimmick_ind_speed_c", 1.0f)
            val indSpeedD = prefs.getFloat("gimmick_ind_speed_d", 1.0f)

            // Update phases for rotating angles
            if (rotationEnabled && visible) {
                phaseA = (phaseA + 0.15f * speed * indSpeedA * rotMultA * deltaSec) % (2f * Math.PI.toFloat())
                phaseB = (phaseB + 0.15f * speed * indSpeedB * rotMultB * deltaSec) % (2f * Math.PI.toFloat())
                phaseC = (phaseC + 0.15f * speed * indSpeedC * rotMultC * deltaSec) % (2f * Math.PI.toFloat())
                phaseD = (phaseD + 0.15f * speed * indSpeedD * rotMultD * deltaSec) % (2f * Math.PI.toFloat())
            }

            // Background drawing
            canvas.drawColor(if (oledEnabled) AndroidColor.BLACK else AndroidColor.parseColor("#121318"))

            val width = canvas.width.toFloat()
            val height = canvas.height.toFloat()

            // Resolve 4 colors to feed gradients and lobed shapes
            val colors = resolveColors(hueShift)

            // Calculate shape/glow centers for spaceship targetting
            val centerAX: Float
            val centerAY: Float
            val centerBX: Float
            val centerBY: Float
            val centerCX: Float
            val centerCY: Float
            val centerDX: Float
            val centerDY: Float

            if (wallpaperType == "AURA_GLOW") {
                centerAX = width * 0.15f + kotlin.math.sin(phaseA) * (width * 0.06f)
                centerAY = height * 0.15f + kotlin.math.cos(phaseA) * (height * 0.05f)

                centerBX = width * 0.85f + kotlin.math.cos(phaseB) * (width * 0.07f)
                centerBY = height * 0.85f + kotlin.math.sin(phaseB) * (height * 0.06f)

                centerCX = width * 0.10f + kotlin.math.sin(phaseC) * (width * 0.05f)
                centerCY = height * 0.50f + kotlin.math.cos(phaseC) * (height * 0.08f)

                centerDX = width * 0.90f + kotlin.math.cos(phaseD) * (width * 0.06f)
                centerDY = height * 0.35f + kotlin.math.sin(phaseD) * (height * 0.05f)

                renderAuraGlow(canvas, width, height, colors, centerAX, centerAY, centerBX, centerBY, centerCX, centerCY, centerDX, centerDY)
            } else {
                val dpScale = width / 360f
                centerAX = 90f * dpScale
                centerAY = 130f * dpScale

                centerBX = width - 30f * dpScale
                centerBY = height - 35f * dpScale

                centerCX = 5f * dpScale
                centerCY = height / 2f + 130f * dpScale

                centerDX = width - 20f * dpScale
                centerDY = 340f * dpScale

                renderMaterialShapes(canvas, width, height, colors, centerAX, centerAY, centerBX, centerBY, centerCX, centerCY, centerDX, centerDY)
            }

            // Spaceship automated movement physics
            val spaceshipEnabled = prefs.getBoolean("gimmick_spaceship_enabled", true)
            if (spaceshipEnabled) {
                if (!shipInitialized && width > 0f && height > 0f) {
                    shipX = width / 2f
                    shipY = height / 2f
                    shipInitialized = true
                }

                if (shipInitialized) {
                    val dpScale = width / 360f
                    val s1 = prefs.getFloat("gimmick_shape1_size", 0.95f)
                    val s2 = prefs.getFloat("gimmick_shape2_size", 1.41f)
                    val s3 = prefs.getFloat("gimmick_shape3_size", 1.35f)
                    val s4 = prefs.getFloat("gimmick_shape4_size", 0.99f)
                    val shipSpeedMult = prefs.getFloat("gimmick_spaceship_speed", 1.0f)
                    val shipLandingTimePref = prefs.getFloat("gimmick_spaceship_landing_time", 3.0f)
                    val shipLandingEnabled = prefs.getBoolean("gimmick_spaceship_landing_enabled", true)
                    val shipAntiCrush = prefs.getBoolean("gimmick_spaceship_anti_crush", false)
                    val shipCautiousness = prefs.getFloat("gimmick_spaceship_cautiousness", 0.5f)
                    val shipSizePref = prefs.getFloat("gimmick_spaceship_size", 12f)
                    val shipRadius = shipSizePref * dpScale
 
                     val targetsX = floatArrayOf(centerAX, centerBX, centerCX, centerDX)
                     val targetsY = floatArrayOf(centerAY, centerBY, centerCY, centerDY)
                     // Precalculate approximate shape bounds (using base radius of the lobed shapes)
                     val targetsR = floatArrayOf(
                         340f * 0.45f * dpScale * s1,
                         340f * 0.33f * dpScale * s2,
                         280f * 0.32f * dpScale * s3,
                         280f * 0.44f * dpScale * s4
                     )
 
                     if (shipState == "CRUMBLED" || shipState == "CRASHED") {
                         shipLandedTimer -= deltaSec
                         
                         // Drift broken pieces beautifully on momentum
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
                                 // Collision!
                                 if (i != shipTargetIndex || !shipLandingEnabled) {
                                     hitCount++
                                 }
                             }
                             
                             // Apply repulsive plus slide force
                             if (odist < safeDist * 1.8f && odist > 0f) {
                                 val force = 1.0f - (odist / (safeDist * 1.8f))
                                 val fdx = odx / odist
                                 val fdy = ody / odist
                                 
                                 // Normal repulsive force
                                 avoidX += fdx * force * 550f * dpScale * avoidForceMult
                                 avoidY += fdy * force * 550f * dpScale * avoidForceMult
                                 
                                 // Tangential slide force (obstacle hugging) to slide gracefully
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
                             shipLandedTimer = 3f // Time dead
                         } else {
                             // Apply acceleration
                             val accel = 250f * dpScale * shipSpeedMult
                             shipVx += (ux * accel + avoidX) * deltaSec
                             shipVy += (uy * accel + avoidY) * deltaSec
 
                             val drag = 1.2f
                             shipVx *= (1f - drag * deltaSec)
                             shipVy *= (1f - drag * deltaSec)
 
                             val currentSpeed = kotlin.math.sqrt(shipVx * shipVx + shipVy * shipVy)
                             val maxSpeed = 160f * dpScale * shipSpeedMult
                             if (currentSpeed > maxSpeed) {
                                 shipVx = (shipVx / currentSpeed) * maxSpeed
                                 shipVy = (shipVy / currentSpeed) * maxSpeed
                             }
 
                             shipX += shipVx * deltaSec
                             shipY += shipVy * deltaSec
 
                             if (currentSpeed > 5f) {
                                 val targetAngle = Math.toDegrees(kotlin.math.atan2(shipVy.toDouble(), shipVx.toDouble())).toFloat()
                                 // Smooth rotation
                                 var angleDiff = targetAngle - shipAngle
                                 while (angleDiff > 180f) angleDiff -= 360f
                                 while (angleDiff < -180f) angleDiff += 360f
                                 shipAngle += angleDiff * 0.15f
                             }
                             
                             if (dist < shipRadius * 1.5f) {
                                 if (shipLandingEnabled) {
                                     shipState = "LANDED"
                                     shipLandedTimer = shipLandingTimePref
                                     shipVx = 0f
                                     shipVy = 0f
                                     // Make sure we are attached relative to the center
                                     val finalDx = shipX - targetsX[shipTargetIndex]
                                     val finalDy = shipY - targetsY[shipTargetIndex]
                                     val finalAngle = Math.toDegrees(kotlin.math.atan2(finalDy.toDouble(), finalDx.toDouble())).toFloat()
                                     shipAngle = finalAngle // face outward when landed
                                 } else {
                                     // Just pick next target
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
                         
                         // Keep on edge even if shapes move
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
                             val launchSpeed = 160f * dpScale * shipSpeedMult
                             shipVx = (kotlin.math.cos(launchAngle) * launchSpeed).toFloat()
                             shipVy = (kotlin.math.sin(launchAngle) * launchSpeed).toFloat()
                             shipState = "FLYING"
                         }
                     }

                    val shipContrastPref = prefs.getFloat("gimmick_spaceship_contrast_scale", 1.0f)
                    val shipRPref = prefs.getFloat("gimmick_ship_r", 255f).toInt()
                    val shipGPref = prefs.getFloat("gimmick_ship_g", 255f).toInt()
                    val shipBPref = prefs.getFloat("gimmick_ship_b", 255f).toInt()

                    if (shipState == "CRUMBLED" || shipState == "CRASHED") {
                        // Drawing broken pieces instead of ship
                        canvas.save()
                        canvas.translate(shipX, shipY)
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = AndroidColor.rgb(shipRPref, shipGPref, shipBPref)
                            alpha = (255 * shipContrastPref * (shipLandedTimer / 3f)).toInt().coerceIn(0, 255)
                            style = Paint.Style.STROKE
                            strokeWidth = 2f * dpScale
                        }
                        val pieces = if (shipState == "CRASHED") 8 else 4
                        for (i in 0 until pieces) {
                            val a = Math.PI * 2 * i / pieces + (3f - shipLandedTimer) * 2f
                            val ox = kotlin.math.cos(a).toFloat() * shipRadius * (3f - shipLandedTimer) * 2f
                            val oy = kotlin.math.sin(a).toFloat() * shipRadius * (3f - shipLandedTimer) * 2f
                            canvas.drawLine(ox, oy, ox + 5f*dpScale, oy + 5f*dpScale, paint)
                        }
                        canvas.restore()
                    } else {
                        // Render Spaceship
                        drawSpaceship(canvas, shipX, shipY, shipAngle, shipSizePref * dpScale, dpScale, shipContrastPref, shipRPref, shipGPref, shipBPref)
                    }
                }
            }
        }

        private fun drawSpaceship(canvas: Canvas, x: Float, y: Float, angleDeg: Float, size: Float, dpScale: Float, contrast: Float, shipR: Int, shipG: Int, shipB: Int) {
            val path = Path()
            val S = size
            
            // Outer arc of 'C' (facing right). Arc from 45 to 315 degrees (sweep = 270)
            path.arcTo(android.graphics.RectF(-S, -S, S, S), 45f, 270f, false)
            
            // Draw a rounded cap at the top tip (from outer ring at 315 to inner ring at 315)
            // Or just a straight line to inner ring, arcTo takes care of it natively
            // Inner arc of 'C' (going backwards from 315 down to 45) -> sweep = -270
            path.arcTo(android.graphics.RectF(-S * 0.6f, -S * 0.6f, S * 0.6f, S * 0.6f), 315f, -270f, false)
            path.close()

            canvas.save()
            canvas.translate(x, y)
            canvas.rotate(angleDeg)

            // Fill solid dark center
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = AndroidColor.BLACK
                this.alpha = (215 * contrast).toInt().coerceIn(0, 255)
                style = Paint.Style.FILL
            }
            canvas.drawPath(path, fillPaint)

            // Stroke outline white (or user selected color)
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = AndroidColor.rgb(shipR, shipG, shipB)
                this.alpha = (255 * contrast).toInt().coerceIn(0, 255)
                style = Paint.Style.STROKE
                strokeWidth = 2.5f * dpScale
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            canvas.drawPath(path, strokePaint)

            canvas.restore()
        }

        private val isLockScreen: Boolean by lazy {
            try {
                val flags = if (android.os.Build.VERSION.SDK_INT >= 34) {
                    getWallpaperFlags()
                } else {
                    val method = this.javaClass.getMethod("getWallpaperFlags")
                    method.invoke(this) as Int
                }
                flags == 2
            } catch (e: Exception) {
                false
            }
        }

        private fun resolveColors(hueShift: Float): List<Int> {
            val lockscreenEnabled = prefs.getBoolean("gimmick_lockscreen_colors_enabled", false)
            val useLockscreenColors = isLockScreen && lockscreenEnabled

            val globalEnabled = if (useLockscreenColors) {
                prefs.getBoolean("gimmick_lockscreen_global_color_enabled", false)
            } else {
                prefs.getBoolean("gimmick_global_color_enabled", false)
            }

            val baseColors = if (globalEnabled) {
                val gr = if (useLockscreenColors) {
                    prefs.getFloat("gimmick_lockscreen_global_r", 152f).toInt()
                } else {
                    prefs.getFloat("gimmick_global_r", 66f).toInt()
                }
                val gg = if (useLockscreenColors) {
                    prefs.getFloat("gimmick_lockscreen_global_g", 57f).toInt()
                } else {
                    prefs.getFloat("gimmick_global_g", 133f).toInt()
                }
                val gb = if (useLockscreenColors) {
                    prefs.getFloat("gimmick_lockscreen_global_b", 235f).toInt()
                } else {
                    prefs.getFloat("gimmick_global_b", 244f).toInt()
                }
                val c = AndroidColor.rgb(gr, gg, gb)
                listOf(c, c, c, c)
            } else {
                // Default color hex parameters
                val customR1 = if (useLockscreenColors) prefs.getFloat("gimmick_lockscreen_c1_r", 0f).toInt() else prefs.getFloat("gimmick_c1_r", 66f).toInt()
                val customG1 = if (useLockscreenColors) prefs.getFloat("gimmick_lockscreen_c1_g", 229f).toInt() else prefs.getFloat("gimmick_c1_g", 133f).toInt()
                val customB1 = if (useLockscreenColors) prefs.getFloat("gimmick_lockscreen_c1_b", 255f).toInt() else prefs.getFloat("gimmick_c1_b", 244f).toInt()

                val customR2 = if (useLockscreenColors) prefs.getFloat("gimmick_lockscreen_c2_r", 98f).toInt() else prefs.getFloat("gimmick_c2_r", 219f).toInt()
                val customG2 = if (useLockscreenColors) prefs.getFloat("gimmick_lockscreen_c2_g", 0f).toInt() else prefs.getFloat("gimmick_c2_g", 68f).toInt()
                val customB2 = if (useLockscreenColors) prefs.getFloat("gimmick_lockscreen_c2_b", 234f).toInt() else prefs.getFloat("gimmick_c2_b", 85f).toInt()

                val customR3 = if (useLockscreenColors) prefs.getFloat("gimmick_lockscreen_c3_r", 255f).toInt() else prefs.getFloat("gimmick_c3_r", 244f).toInt()
                val customG3 = if (useLockscreenColors) prefs.getFloat("gimmick_lockscreen_c3_g", 64f).toInt() else prefs.getFloat("gimmick_c3_g", 180f).toInt()
                val customB3 = if (useLockscreenColors) prefs.getFloat("gimmick_lockscreen_c3_b", 129f).toInt() else prefs.getFloat("gimmick_c3_b", 0f).toInt()

                val customR4 = if (useLockscreenColors) prefs.getFloat("gimmick_lockscreen_c4_r", 224f).toInt() else prefs.getFloat("gimmick_c4_r", 52f).toInt()
                val customG4 = if (useLockscreenColors) prefs.getFloat("gimmick_lockscreen_c4_g", 64f).toInt() else prefs.getFloat("gimmick_c4_g", 168f).toInt()
                val customB4 = if (useLockscreenColors) prefs.getFloat("gimmick_lockscreen_c4_b", 251f).toInt() else prefs.getFloat("gimmick_c4_b", 83f).toInt()

                listOf(
                    AndroidColor.rgb(customR1, customG1, customB1),
                    AndroidColor.rgb(customR2, customG2, customB2),
                    AndroidColor.rgb(customR3, customG3, customB3),
                    AndroidColor.rgb(customR4, customG4, customB4)
                )
            }

            val shapesSaturation = prefs.getFloat("gimmick_shapes_saturation", 1.0f)

            val indSatA = prefs.getFloat("gimmick_ind_sat_a", 1.0f)
            val indSatB = prefs.getFloat("gimmick_ind_sat_b", 1.0f)
            val indSatC = prefs.getFloat("gimmick_ind_sat_c", 1.0f)
            val indSatD = prefs.getFloat("gimmick_ind_sat_d", 1.0f)
            val sats = listOf(indSatA, indSatB, indSatC, indSatD)

            return baseColors.mapIndexed { index, color ->
                val rValue = AndroidColor.red(color)
                val gValue = AndroidColor.green(color)
                val bValue = AndroidColor.blue(color)
                if (rValue >= 253 && gValue >= 253 && bValue >= 253) {
                    color
                } else {
                    val hsv = FloatArray(3)
                    AndroidColor.colorToHSV(color, hsv)
                    if (hueShift != 0f) {
                        hsv[0] = (hsv[0] + hueShift) % 360f
                    }
                    val shapeSat = sats.getOrElse(index) { 1.0f }
                    hsv[1] = (hsv[1] * shapesSaturation * shapeSat).coerceIn(0f, 1f)
                    AndroidColor.HSVToColor(hsv)
                }
            }
        }

        private fun renderAuraGlow(
            canvas: Canvas,
            width: Float,
            height: Float,
            colors: List<Int>,
            centerAX: Float, centerAY: Float,
            centerBX: Float, centerBY: Float,
            centerCX: Float, centerCY: Float,
            centerDX: Float, centerDY: Float
        ) {
            val contrastScale = prefs.getFloat("gimmick_contrast_scale", 1.0f)

            val radiusA = width * 0.55f
            drawGlowCircle(canvas, centerAX, centerAY, radiusA, colors[0], 0.28f * contrastScale)

            val radiusB = width * 0.65f
            drawGlowCircle(canvas, centerBX, centerBY, radiusB, colors[1], 0.24f * contrastScale)

            val radiusC = width * 0.50f
            drawGlowCircle(canvas, centerCX, centerCY, radiusC, colors[2], 0.30f * contrastScale)

            val radiusD = width * 0.45f
            drawGlowCircle(canvas, centerDX, centerDY, radiusD, colors[3], 0.26f * contrastScale)
        }

        private fun drawGlowCircle(canvas: Canvas, cx: Float, cy: Float, radius: Float, color: Int, alpha: Float) {
            val rValue = AndroidColor.red(color)
            val gValue = AndroidColor.green(color)
            val bValue = AndroidColor.blue(color)
            val isPureWhite = rValue >= 253 && gValue >= 253 && bValue >= 253
            val resolvedAlpha = if (isPureWhite) 1.0f else alpha

            val transparentColor = color and 0x00FFFFFF // alpha 0
            val opaqueColor = (color and 0x00FFFFFF) or ((resolvedAlpha.coerceIn(0f, 1f) * 255).toInt() shl 24)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(cx, cy, radius, opaqueColor, transparentColor, Shader.TileMode.CLAMP)
            }
            canvas.drawCircle(cx, cy, radius, paint)
        }

        private fun renderMaterialShapes(
            canvas: Canvas,
            width: Float,
            height: Float,
            colors: List<Int>,
            centerAX: Float, centerAY: Float,
            centerBX: Float, centerBY: Float,
            centerCX: Float, centerCY: Float,
            centerDX: Float, centerDY: Float
        ) {
            val dpScale = width / 360f
            val contrastScale = prefs.getFloat("gimmick_contrast_scale", 1.0f)
            
            val s1 = prefs.getFloat("gimmick_shape1_size", 0.95f)
            val s2 = prefs.getFloat("gimmick_shape2_size", 1.41f)
            val s3 = prefs.getFloat("gimmick_shape3_size", 1.35f)
            val s4 = prefs.getFloat("gimmick_shape4_size", 0.99f)

            val indVividA = prefs.getFloat("gimmick_ind_vivid_a", 1.0f)
            val indVividB = prefs.getFloat("gimmick_ind_vivid_b", 1.0f)
            val indVividC = prefs.getFloat("gimmick_ind_vivid_c", 1.0f)
            val indVividD = prefs.getFloat("gimmick_ind_vivid_d", 1.0f)

            // Shape C (Center-Left)
            val angleC = Math.toDegrees(phaseC.toDouble()).toFloat()
            drawLobedShape(canvas, centerCX, centerCY, 280f * 0.32f * dpScale * s3, 3, 280f * 0.07f * dpScale * s3, colors[2], 0.28f * contrastScale * indVividC, angleC, false, 2)

            // Shape A (Top-Left)
            val angleA = Math.toDegrees(phaseA.toDouble()).toFloat()
            drawLobedShape(canvas, centerAX, centerAY, 340f * 0.45f * dpScale * s1, 4, 340f * 0.08f * dpScale * s1, colors[0], 0.26f * contrastScale * indVividA, angleA, false, 0)

            // Shape B (Bottom-Right)
            val angleB = Math.toDegrees(phaseB.toDouble()).toFloat()
            drawLobedShape(canvas, centerBX, centerBY, 340f * 0.33f * dpScale * s2, 5, 340f * 0.06f * dpScale * s2, colors[1], 0.24f * contrastScale * indVividB, angleB, true, 1)

            // Shape D (Top-Right)
            val angleD = Math.toDegrees(phaseD.toDouble()).toFloat()
            drawLobedShape(canvas, centerDX, centerDY, 280f * 0.44f * dpScale * s4, 6, 280f * 0.05f * dpScale * s4, colors[3], 0.22f * contrastScale * indVividD, angleD, true, 3)
        }

        private fun drawLobedShape(
            canvas: Canvas,
            cx: Float,
            cy: Float,
            baseRadius: Float,
            lobes: Int,
            amplitude: Float,
            color: Int,
            alpha: Float,
            rotationAngle: Float,
            isSin: Boolean,
            shapeIndex: Int
        ) {
            val path = Path()
            val pointsCount = 180

            canvas.save()
            canvas.rotate(rotationAngle, cx, cy)

            for (i in 0 until pointsCount) {
                val angleRad = (i * 2 * Math.PI / pointsCount).toFloat()
                val wave = if (isSin) {
                    kotlin.math.sin(lobes * angleRad)
                } else {
                    kotlin.math.cos(lobes * angleRad)
                }
                val r = baseRadius + amplitude * wave
                val x = cx + r * kotlin.math.cos(angleRad)
                val y = cy + r * kotlin.math.sin(angleRad)
                if (i == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            path.close()

            val rValue = AndroidColor.red(color)
            val gValue = AndroidColor.green(color)
            val bValue = AndroidColor.blue(color)
            val isPureWhite = rValue >= 253 && gValue >= 253 && bValue >= 253
            val resolvedAlpha = if (isPureWhite) 1.0f else alpha

            val rawBlurRadius = prefs.getFloat("gimmick_shape_blur_radius", 0f)
            val blurTarget = prefs.getString("gimmick_shape_blur_target", "BOTH") ?: "BOTH"
            val lockIndependent = prefs.getBoolean("gimmick_shape_blur_lock_independent", false)
            val rawBlurRadiusLock = prefs.getFloat("gimmick_shape_blur_radius_lock", 15.0f)

            val baseShapeBlurRadius = when {
                isLockScreen -> {
                    if (lockIndependent) rawBlurRadiusLock else rawBlurRadius
                }
                else -> { // Home screen
                    if (blurTarget == "LOCK_ONLY") 0f else rawBlurRadius
                }
            }

            // Retrieve independent shape-only blurs
            val indBlurA = prefs.getFloat("gimmick_ind_blur_a", 0.0f)
            val indBlurB = prefs.getFloat("gimmick_ind_blur_b", 0.0f)
            val indBlurC = prefs.getFloat("gimmick_ind_blur_c", 0.0f)
            val indBlurD = prefs.getFloat("gimmick_ind_blur_d", 0.0f)
            val indBlurs = listOf(indBlurA, indBlurB, indBlurC, indBlurD)
            val shapeIndBlur = indBlurs.getOrElse(shapeIndex) { 0.0f }

            val totalShapeBlurRadius = baseShapeBlurRadius + shapeIndBlur
            val fullDpScale = canvas.width.toFloat() / 360f

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                this.alpha = (resolvedAlpha.coerceIn(0f, 1f) * 255).toInt()
                style = Paint.Style.FILL
                if (totalShapeBlurRadius > 0f) {
                    maskFilter = android.graphics.BlurMaskFilter(totalShapeBlurRadius * fullDpScale, android.graphics.BlurMaskFilter.Blur.NORMAL)
                }
            }
            canvas.drawPath(path, paint)
            canvas.restore()
        }
    }
}
