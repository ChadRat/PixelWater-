package com.pixelwater.app.ui

import android.content.Context
import android.os.Vibrator
import android.view.View
import androidx.compose.runtime.mutableStateOf

enum class HapticFeedbackType {
    SUBTLE,
    MEDIUM,
    HEAVY
}

enum class CustomHapticType {
    BUTTON,
    WOOP,
    LIGHT_TICK,
    SCROLL,
    SLIDER,
    TOGGLE_SNAP,
    TOGGLE_LIGHT,
    GOAL,
    FIREWORKS,
    CONFETTI_BURST,
    SIDE_BURST,
    RAINFALL,
    TAB_TRACK,
    TAB_STATS,
    TAB_AI,
    TAB_REMINDERS,
    TAB_SETTINGS
}

/**
 * Centralized haptic feedback utility that can be toggled on/off app-wide.
 * Controls in-app UI haptics (not widget haptics).
 */
object HapticUtil {
    // Mutable state to track if in-app haptics are enabled
    val isAppHapticsEnabled = mutableStateOf(true)

    /**
     * Perform a UI interaction haptic feedback (light tick)
     * Only performs if app haptics are enabled
     */
    fun performUIHaptic(view: View) {
        if (!isAppHapticsEnabled.value) return
        view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
    }

    /**
     * Perform a light tick haptic feedback
     */
    fun performLightHaptic(view: View) {
        if (!isAppHapticsEnabled.value) return
        view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
    }

    /**
     * Perform an extra light micro tick haptic feedback
     */
    fun performMicroHaptic(view: View) {
        performCustomHaptic(view, 0.02f)
    }

    /**
     * Perform a medium impact haptic feedback
     */
    fun performMediumHaptic(view: View) {
        if (!isAppHapticsEnabled.value) return
        view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
    }

    /**
     * Perform a heavy/virtual key haptic feedback
     */
    fun performHeavyHaptic(view: View) {
        if (!isAppHapticsEnabled.value) return
        view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
    }

    fun performSliderHaptic(view: View) {
        if (!isAppHapticsEnabled.value) return
        view.performHapticFeedback(android.view.HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
    }

    /**
     * Perform a virtual key haptic feedback (stronger)
     * Only performs if app haptics are enabled
     */
    fun performVirtualKeyHaptic(view: View) {
        if (!isAppHapticsEnabled.value) return
        view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
    }

    /**
     * Perform haptic feedback for background services (Context-based)
     */
    fun performHapticForService(
        context: Context,
        type: HapticFeedbackType = HapticFeedbackType.SUBTLE
    ) {
        if (!isAppHapticsEnabled.value) return
        val vibrator = getVibrator(context) ?: return
        performHapticFeedback(vibrator, type)
    }

    private fun performHapticFeedback(vibrator: Vibrator, type: HapticFeedbackType) {
        val duration = when (type) {
            HapticFeedbackType.SUBTLE -> 12L
            HapticFeedbackType.MEDIUM -> 20L
            HapticFeedbackType.HEAVY -> 30L
        }
        val amplitude = when (type) {
            HapticFeedbackType.SUBTLE -> 40
            HapticFeedbackType.MEDIUM -> 120
            HapticFeedbackType.HEAVY -> 255
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(duration, amplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
        } catch (e: Exception) {
            // Fallback or ignore
        }
    }

    fun performCustomHaptic(view: View, strength: Float) {
        performTailoredHaptic(view.context, strength, CustomHapticType.BUTTON)
    }

    fun performCustomHapticWithContext(context: Context, strengthRaw: Float) {
        performTailoredHaptic(context, strengthRaw, CustomHapticType.BUTTON)
    }

    fun performTailoredHaptic(context: Context, strengthRaw: Float, type: CustomHapticType) {
        if (!isAppHapticsEnabled.value) return
        val strength = strengthRaw.coerceIn(0.01f, 1.0f)

        val vibrator = getVibrator(context) ?: return

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                val hasClick = vibrator.areAllPrimitivesSupported(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK)
                if (hasClick) {
                    val composition = android.os.VibrationEffect.startComposition()
                    
                    val hasThud = vibrator.areAllPrimitivesSupported(android.os.VibrationEffect.Composition.PRIMITIVE_THUD)
                    val hasQuickFall = vibrator.areAllPrimitivesSupported(android.os.VibrationEffect.Composition.PRIMITIVE_QUICK_FALL)
                    val hasQuickRise = vibrator.areAllPrimitivesSupported(android.os.VibrationEffect.Composition.PRIMITIVE_QUICK_RISE)
                    val hasSlowRise = vibrator.areAllPrimitivesSupported(android.os.VibrationEffect.Composition.PRIMITIVE_SLOW_RISE)
                    val hasTick = vibrator.areAllPrimitivesSupported(android.os.VibrationEffect.Composition.PRIMITIVE_TICK)

                    when (type) {
                        CustomHapticType.BUTTON -> {
                            composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, strength)
                            if (hasThud) {
                                composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_THUD, (strength * 0.8f).coerceIn(0.01f, 1.0f), 0)
                            }
                            if (hasQuickFall) {
                                composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, (strength * 0.9f).coerceIn(0.01f, 1.0f), 4)
                            }
                        }
                        CustomHapticType.WOOP -> {
                            // Upward cheerful bounce
                            if (hasQuickRise) {
                                composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, (strength * 0.7f).coerceIn(0.01f, 1.0f), 0)
                            }
                            composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, strength, 12)
                            if (hasQuickFall) {
                                composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, (strength * 0.6f).coerceIn(0.01f, 1.0f), 20)
                            }
                        }
                        CustomHapticType.LIGHT_TICK -> {
                            if (hasTick) {
                                composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_TICK, (strength * 0.5f).coerceIn(0.01f, 1.0f))
                            } else {
                                composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, (strength * 0.2f).coerceIn(0.01f, 1.0f))
                            }
                        }
                        CustomHapticType.SCROLL -> {
                            if (hasTick) {
                                composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_TICK, (strength * 0.4f).coerceIn(0.01f, 1.0f))
                            } else {
                                composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, (strength * 0.15f).coerceIn(0.01f, 1.0f))
                            }
                        }
                        CustomHapticType.SLIDER -> {
                            composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, (strength * 1.0f).coerceIn(0.01f, 1.0f))
                            if (hasTick) {
                                composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_TICK, (strength * 0.9f).coerceIn(0.01f, 1.0f), 6)
                            }
                        }
                        CustomHapticType.TOGGLE_SNAP -> {
                            // Crisp tactile clicky mechanical haptic (like Google Pixel's physical switch)
                            composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, strength)
                            if (hasTick) {
                                composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_TICK, (strength * 1.0f).coerceIn(0.01f, 1.0f), 12)
                            } else if (hasThud) {
                                composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_THUD, (strength * 0.8f).coerceIn(0.01f, 1.0f), 10)
                            }
                        }
                        CustomHapticType.TOGGLE_LIGHT -> {
                            // Perfect clicky bounce physical release tick
                            if (hasTick) {
                                composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_TICK, (strength * 0.9f).coerceIn(0.01f, 1.0f))
                                if (hasQuickFall) {
                                    composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, (strength * 0.5f).coerceIn(0.01f, 1.0f), 8)
                                }
                            } else {
                                composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, (strength * 0.4f).coerceIn(0.01f, 1.0f))
                            }
                            if (hasQuickFall) {
                                composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, (strength * 0.4f).coerceIn(0.01f, 1.0f), 8)
                            }
                        }
                        CustomHapticType.GOAL -> {
                            // Slow rising epic celebration resonance with multiple rapid triumphant clicks
                            if (hasSlowRise) {
                                composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_SLOW_RISE, (strength * 0.8f).coerceIn(0.01f, 1.0f), 0)
                            }
                            if (hasThud) {
                                composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_THUD, strength, 20)
                            }
                            composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, strength, 30)
                            
                            // High-frequency triumphant micro-clicks in the middle
                            composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, (strength * 0.9f).coerceIn(0.01f, 1.0f), 60)
                            if (hasTick) {
                                composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_TICK, (strength * 0.8f).coerceIn(0.01f, 1.0f), 80)
                                composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_TICK, (strength * 0.9f).coerceIn(0.01f, 1.0f), 100)
                            } else {
                                composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, (strength * 0.5f).coerceIn(0.01f, 1.0f), 80)
                            }
                            composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, strength, 120)
                            
                            if (hasThud) {
                                composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_THUD, (strength * 0.9f).coerceIn(0.01f, 1.0f), 180)
                            }
                            composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, strength, 190)
                        }
                        CustomHapticType.CONFETTI_BURST -> {
                            // Similar to FIREWORKS but concentrated at the start
                            composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, strength)
                            if (hasThud) {
                                composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_THUD, (strength * 0.9f).coerceIn(0.01f, 1.0f), 15)
                            }
                            if (hasTick) {
                                composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_TICK, (strength * 0.8f).coerceIn(0.01f, 1.0f), 45)
                                composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_TICK, (strength * 0.7f).coerceIn(0.01f, 1.0f), 70)
                            }
                            // Falling particles feeling
                            val delays = intArrayOf(120, 150, 190, 240, 300)
                            val strengths = floatArrayOf(0.6f, 0.5f, 0.4f, 0.3f, 0.2f)
                            for(i in delays.indices) {
                                if (hasTick) composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_TICK, (strength * strengths[i]).coerceIn(0.0f, 1.0f), delays[i])
                                else composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, (strength * strengths[i]).coerceIn(0.0f, 1.0f), delays[i])
                            }
                        }
                        CustomHapticType.SIDE_BURST -> {
                            // Two distinct bursts
                            composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, strength)
                            if (hasTick) {
                                composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_TICK, (strength * 0.8f).coerceIn(0.0f, 1.0f), 30)
                            }
                            composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, strength, 80)
                            if (hasTick) {
                                composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_TICK, (strength * 0.8f).coerceIn(0.0f, 1.0f), 110)
                            }
                            // Scattered falling
                            val delays = intArrayOf(160, 200, 250, 310)
                            val strengths = floatArrayOf(0.5f, 0.4f, 0.3f, 0.2f)
                            for(i in delays.indices) {
                                if (hasTick) composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_TICK, (strength * strengths[i]).coerceIn(0.0f, 1.0f), delays[i])
                            }
                        }
                        CustomHapticType.RAINFALL -> {
                            // Evenly spaced gentle ticks building up slightly then fading
                            val delays = intArrayOf(0, 40, 85, 130, 180, 230, 290, 350)
                            val strengths = floatArrayOf(0.4f, 0.5f, 0.6f, 0.5f, 0.4f, 0.3f, 0.2f, 0.1f)
                            for(i in delays.indices) {
                                if (hasTick) composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_TICK, (strength * strengths[i]).coerceIn(0.0f, 1.0f), delays[i])
                                else composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, (strength * strengths[i]).coerceIn(0.0f, 1.0f), delays[i])
                            }
                        }
                        CustomHapticType.FIREWORKS -> {
                            // Multiple brilliant cascading crackle pops for confetti burst
                            composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, strength, 0)
                            if (hasTick) {
                                composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_TICK, (strength * 1.0f).coerceIn(0.01f, 1.0f), 30)
                                composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_TICK, (strength * 0.9f).coerceIn(0.01f, 1.0f), 60)
                                composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_TICK, (strength * 0.8f).coerceIn(0.01f, 1.0f), 90)
                            } else {
                                composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, (strength * 0.6f).coerceIn(0.01f, 1.0f), 30)
                                composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, (strength * 0.5f).coerceIn(0.01f, 1.0f), 65)
                            }
                            composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, (strength * 1.0f).coerceIn(0.01f, 1.0f), 120)
                            if (hasQuickFall) {
                                composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, (strength * 0.8f).coerceIn(0.01f, 1.0f), 140)
                            }
                            composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, (strength * 0.9f).coerceIn(0.01f, 1.0f), 180)

                            // Cascading falling confetti particles - soft tactile clicks decaying over time
                            val individualConfettiDelays = intArrayOf(230, 290, 360, 440, 530, 630, 740, 860)
                            val individualConfettiStrengths = floatArrayOf(0.85f, 0.75f, 0.65f, 0.55f, 0.45f, 0.35f, 0.25f, 0.15f)
                            for (index in individualConfettiDelays.indices) {
                                val currentDelay = individualConfettiDelays[index]
                                val currentStrength = (strength * individualConfettiStrengths[index]).coerceIn(0.01f, 1.0f)
                                if (hasTick && index % 2 == 1) {
                                    composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_TICK, currentStrength, currentDelay)
                                } else {
                                    composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, currentStrength, currentDelay)
                                }
                            }
                        }
                        CustomHapticType.TAB_TRACK -> {
                            // Raindrop splash: drip tick, sleep, splash click, ripple thud
                            if (hasTick) {
                                composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_TICK, (strength * 0.4f).coerceIn(0.01f, 1.0f), 0)
                            }
                            composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, strength, 120)
                            if (hasThud) {
                                composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_THUD, (strength * 0.6f).coerceIn(0.01f, 1.0f), 160)
                            }
                        }
                        CustomHapticType.TAB_STATS -> {
                            // Synced with the 3 bars peaking in the 1800ms animation:
                            // Bar 1 peak @ 540ms, Bar 3 peak @ 1080ms, Bar 2 peak @ 1350ms
                            if (hasTick) {
                                composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_TICK, (strength * 0.4f).coerceIn(0.01f, 1.0f), 0)
                            }
                            composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, (strength * 0.6f).coerceIn(0.01f, 1.0f), 520)
                            composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, (strength * 0.8f).coerceIn(0.01f, 1.0f), 510)
                            if (hasThud) {
                                composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_THUD, strength, 250)
                            } else {
                                composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, strength, 250)
                            }
                        }
                        CustomHapticType.TAB_AI -> {
                            // Smart assistant rolling bubble pops
                            if (hasQuickRise) {
                                composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, (strength * 0.5f).coerceIn(0.01f, 1.0f), 0)
                            }
                            composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, (strength * 0.8f).coerceIn(0.01f, 1.0f), 80)
                            if (hasTick) {
                                composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_TICK, (strength * 0.6f).coerceIn(0.01f, 1.0f), 160)
                            }
                        }
                        CustomHapticType.TAB_REMINDERS -> {
                            composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, strength, 225)
                            composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, strength, 435)
                            composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, strength, 435)
                            composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, strength, 435)
                        }
                        CustomHapticType.TAB_SETTINGS -> {
                            // Sliding friction and firm lock gear click
                            if (hasTick) {
                                composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_TICK, (strength * 0.5f).coerceIn(0.01f, 1.0f), 0)
                            }
                            composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, strength, 180)
                            if (hasThud) {
                                composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_THUD, (strength * 0.8f).coerceIn(0.01f, 1.0f), 190)
                            }
                        }
                    }

                    val effect = composition.compose()
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        val attrs = android.os.VibrationAttributes.createForUsage(android.os.VibrationAttributes.USAGE_TOUCH)
                        vibrator.vibrate(effect, attrs)
                    } else {
                        vibrator.vibrate(effect)
                    }
                    return
                }
            } catch (e: Exception) {
                // Fallback if primitive check fails
            }
        }

        // Precise Fallback for standard vibration on older Android systems (mimic the pattern feeling)
        val amplitudeLevel = (strength * 255).toInt().coerceIn(1, 255)
        when (type) {
            CustomHapticType.BUTTON -> {
                vibrateSinglePulseWithVibrator(vibrator, 12, amplitudeLevel)
            }
            CustomHapticType.WOOP -> {
                vibratePatternWithVibrator(vibrator, longArrayOf(0, 15, 20, 25), intArrayOf(0, (amplitudeLevel * 0.6f).toInt(), 0, amplitudeLevel))
            }
            CustomHapticType.LIGHT_TICK -> {
                vibrateSinglePulseWithVibrator(vibrator, 4, (amplitudeLevel * 0.35f).toInt())
            }
            CustomHapticType.SCROLL -> {
                vibrateSinglePulseWithVibrator(vibrator, 4, (amplitudeLevel * 0.2f).toInt())
            }
            CustomHapticType.SLIDER -> {
                vibrateSinglePulseWithVibrator(vibrator, 14, (amplitudeLevel * 1.0f).toInt())
            }
            CustomHapticType.TOGGLE_SNAP -> {
                vibratePatternWithVibrator(vibrator, longArrayOf(0, 10, 10, 15), intArrayOf(0, amplitudeLevel, 0, (amplitudeLevel * 0.7f).toInt()))
            }
            CustomHapticType.TOGGLE_LIGHT -> {
                vibrateSinglePulseWithVibrator(vibrator, 6, (amplitudeLevel * 0.5f).toInt())
            }
            CustomHapticType.GOAL -> {
                vibratePatternWithVibrator(
                    vibrator, 
                    longArrayOf(0, 30, 40, 20, 15, 10, 30, 15, 15, 10, 30), 
                    intArrayOf(0, (amplitudeLevel * 0.6f).toInt(), amplitudeLevel, 0, (amplitudeLevel * 0.5f).toInt(), 0, (amplitudeLevel * 0.6f).toInt(), 0, (amplitudeLevel * 0.5f).toInt(), 0, amplitudeLevel)
                )
            }
            CustomHapticType.FIREWORKS -> {
                vibratePatternWithVibrator(
                    vibrator,
                    longArrayOf(0, 15, 30, 15, 30, 15, 30, 15, 45, 15, 50, 20, 60, 12, 70, 10, 80, 8, 90, 8, 100, 6, 110, 6, 120, 5, 130, 4),
                    intArrayOf(
                        0, amplitudeLevel,
                        0, (amplitudeLevel * 0.9f).toInt(),
                        0, (amplitudeLevel * 0.8f).toInt(),
                        0, (amplitudeLevel * 0.7f).toInt(),
                        0, (amplitudeLevel * 0.6f).toInt(),
                        0, amplitudeLevel,
                        0, (amplitudeLevel * 0.85f).toInt(),
                        0, (amplitudeLevel * 0.75f).toInt(),
                        0, (amplitudeLevel * 0.65f).toInt(),
                        0, (amplitudeLevel * 0.55f).toInt(),
                        0, (amplitudeLevel * 0.45f).toInt(),
                        0, (amplitudeLevel * 0.35f).toInt(),
                        0, (amplitudeLevel * 0.25f).toInt(),
                        0, (amplitudeLevel * 0.15f).toInt()
                    )
                )
            }
            CustomHapticType.TAB_TRACK -> {
                vibratePatternWithVibrator(vibrator, longArrayOf(0, 8, 110, 15, 40, 10), intArrayOf(0, (amplitudeLevel * 0.4f).toInt(), 0, amplitudeLevel, 0, (amplitudeLevel * 0.3f).toInt()))
            }
            CustomHapticType.TAB_STATS -> {
                vibratePatternWithVibrator(vibrator, longArrayOf(0, 10, 520, 12, 510, 15, 250, 20), intArrayOf(0, (amplitudeLevel * 0.35f).toInt(), 0, (amplitudeLevel * 0.55f).toInt(), 0, (amplitudeLevel * 0.75f).toInt(), 0, amplitudeLevel))
            }
            CustomHapticType.TAB_AI -> {
                vibratePatternWithVibrator(vibrator, longArrayOf(0, 12, 70, 8, 70, 8), intArrayOf(0, amplitudeLevel, 0, (amplitudeLevel * 0.7f).toInt(), 0, (amplitudeLevel * 0.5f).toInt()))
            }
            CustomHapticType.TAB_REMINDERS -> {
                vibratePatternWithVibrator(vibrator, longArrayOf(225, 15, 435, 15, 435, 15, 435, 15), intArrayOf(0, amplitudeLevel, 0, amplitudeLevel, 0, amplitudeLevel, 0, amplitudeLevel))
            }
            CustomHapticType.TAB_SETTINGS -> {
                vibratePatternWithVibrator(vibrator, longArrayOf(0, 10, 170, 15, 10, 10), intArrayOf(0, (amplitudeLevel * 0.3f).toInt(), 0, amplitudeLevel, 0, (amplitudeLevel * 0.8f).toInt()))
            }
            CustomHapticType.CONFETTI_BURST -> {
                vibratePatternWithVibrator(vibrator, longArrayOf(0, 10, 10, 15, 15, 15), intArrayOf(0, amplitudeLevel, 0, (amplitudeLevel * 0.8f).toInt(), 0, (amplitudeLevel * 0.6f).toInt()))
            }
            CustomHapticType.SIDE_BURST -> {
                vibratePatternWithVibrator(vibrator, longArrayOf(0, 12, 12, 18), intArrayOf(0, amplitudeLevel, 0, (amplitudeLevel * 0.7f).toInt()))
            }
            CustomHapticType.RAINFALL -> {
                vibratePatternWithVibrator(vibrator, longArrayOf(0, 10, 25, 10), intArrayOf(0, (amplitudeLevel * 0.6f).toInt(), 0, (amplitudeLevel * 0.4f).toInt()))
            }
        }
    }

    private fun vibrateSinglePulseWithVibrator(vibrator: Vibrator, durationMs: Int, amplitude: Int) {
        if (durationMs <= 0) return
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(durationMs.toLong(), amplitude.coerceIn(1, 255)))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs.toLong())
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun vibratePatternWithVibrator(vibrator: Vibrator, timings: LongArray, strengths: IntArray) {
        if (timings.isEmpty() || strengths.isEmpty() || timings.size != strengths.size) return
        val finalTimings = timings.mapIndexed { index, t ->
            if (index == 0) t else maxOf(1L, t)
        }.toLongArray()
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val amplitudes = strengths.map { it.coerceIn(0, 255) }.toIntArray()
                if (vibrator.hasAmplitudeControl()) {
                    vibrator.vibrate(android.os.VibrationEffect.createWaveform(finalTimings, amplitudes, -1))
                } else {
                    vibrator.vibrate(android.os.VibrationEffect.createWaveform(finalTimings, -1))
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(finalTimings, -1)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    /**
     * Load app haptic preference from SharedPreferences
     */
    fun loadAppHapticsEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences("water_tracker_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("app_haptics_enabled", true) // Default: enabled
    }

    /**
     * Save app haptic preference to SharedPreferences
     */
    fun saveAppHapticsEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences("water_tracker_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("app_haptics_enabled", enabled).apply()
        isAppHapticsEnabled.value = enabled
    }

    /**
     * Initialize haptic setting from SharedPreferences
     */
    fun initialize(context: Context) {
        isAppHapticsEnabled.value = loadAppHapticsEnabled(context)
    }

    private fun getVibrator(context: Context): Vibrator? {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Throwable) {
            null
        }
    }
}
