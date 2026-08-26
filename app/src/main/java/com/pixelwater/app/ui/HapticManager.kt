package com.pixelwater.app.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class HapticMode(val displayName: String) {
    DEFAULT("Default"),
    ENHANCED("Enhanced");

    companion object {
        fun fromString(value: String): HapticMode {
            return try {
                valueOf(value.uppercase())
            } catch (e: Exception) {
                DEFAULT
            }
        }
    }
}

enum class EnhancedHapticStrength(val value: Float, val displayName: String, val elDisplayName: String) {
    VERY_LIGHT(0.5f, "0.5x", "0.5x"),
    LIGHT(0.85f, "0.8x", "0.8x"),
    NORMAL(1.0f, "1.0x", "1.0x"),
    STRONG(1.5f, "1.5x", "1.5x"),
    MAXIMUM(2.0f, "2.0x", "2.0x");

    companion object {
        fun fromString(value: String): EnhancedHapticStrength {
            return try {
                valueOf(value.uppercase())
            } catch (e: Exception) {
                LIGHT
            }
        }
    }
}

object HapticManager {
    private val _hapticMode = MutableStateFlow(HapticMode.DEFAULT)
    val hapticMode: StateFlow<HapticMode> = _hapticMode.asStateFlow()

    private val _scrollHapticsEnabled = MutableStateFlow(true)
    val scrollHapticsEnabled: StateFlow<Boolean> = _scrollHapticsEnabled.asStateFlow()

    private val _scrollHapticsPixels = MutableStateFlow(200f)
    val scrollHapticsPixels: StateFlow<Float> = _scrollHapticsPixels.asStateFlow()

    private val _enhancedScrollHapticStrength = MutableStateFlow(0.85f)
    val enhancedScrollHapticStrength: StateFlow<Float> = _enhancedScrollHapticStrength.asStateFlow()

    private val _enhancedHapticStrength = MutableStateFlow(EnhancedHapticStrength.LIGHT)
    val enhancedHapticStrength: StateFlow<EnhancedHapticStrength> = _enhancedHapticStrength.asStateFlow()

    fun isLinearMotor(context: Context): Boolean {
        val vibrator = getVibrator(context) ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            vibrator.areAllPrimitivesSupported(
                VibrationEffect.Composition.PRIMITIVE_CLICK,
                VibrationEffect.Composition.PRIMITIVE_TICK
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.hasAmplitudeControl()
        } else {
            false
        }
    }

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences("water_tracker_prefs", Context.MODE_PRIVATE)
        val defaultMode = if (isLinearMotor(context)) HapticMode.ENHANCED else HapticMode.DEFAULT
        val modeStr = prefs.getString("haptic_mode", defaultMode.name) ?: defaultMode.name
        _hapticMode.value = HapticMode.fromString(modeStr)
        _scrollHapticsEnabled.value = prefs.getBoolean("scroll_haptics_enabled", true)
        _scrollHapticsPixels.value = prefs.getFloat("scroll_haptics_pixels", 200f)
        _enhancedScrollHapticStrength.value = prefs.getFloat("enhanced_scroll_haptic_strength", 0.85f)
        val strengthStr = prefs.getString("enhanced_haptic_strength", EnhancedHapticStrength.LIGHT.name) ?: EnhancedHapticStrength.LIGHT.name
        _enhancedHapticStrength.value = EnhancedHapticStrength.fromString(strengthStr)
        HapticUtil.initialize(context)
    }

    fun setHapticMode(context: Context, mode: HapticMode) {
        _hapticMode.value = mode
        val prefs = context.getSharedPreferences("water_tracker_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("haptic_mode", mode.name).apply()
    }

    fun setScrollHapticsEnabled(context: Context, enabled: Boolean) {
        _scrollHapticsEnabled.value = enabled
        val prefs = context.getSharedPreferences("water_tracker_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("scroll_haptics_enabled", enabled).apply()
    }

    fun setScrollHapticsPixels(context: Context, pixels: Float) {
        _scrollHapticsPixels.value = pixels
        val prefs = context.getSharedPreferences("water_tracker_prefs", Context.MODE_PRIVATE)
        prefs.edit().putFloat("scroll_haptics_pixels", pixels).apply()
    }

    fun setEnhancedScrollHapticStrength(context: Context, strength: Float) {
        _enhancedScrollHapticStrength.value = strength
        val prefs = context.getSharedPreferences("water_tracker_prefs", Context.MODE_PRIVATE)
        prefs.edit().putFloat("enhanced_scroll_haptic_strength", strength).apply()
    }

    fun setEnhancedHapticStrength(context: Context, strength: EnhancedHapticStrength) {
        _enhancedHapticStrength.value = strength
        val prefs = context.getSharedPreferences("water_tracker_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("enhanced_haptic_strength", strength.name).apply()
    }

    private fun getVibrator(context: Context): Vibrator? {
        return try {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } catch (e: Exception) {
            null
        }
    }

    fun triggerButtonHaptic(context: Context, view: View?, durationMs: Int, strengthPercent: Int) {
        if (_hapticMode.value == HapticMode.ENHANCED) {
            val s = _enhancedHapticStrength.value.value
            HapticUtil.performTailoredHaptic(view?.context ?: context, s, CustomHapticType.BUTTON)
        } else {
            vibrateSinglePulse(context, durationMs, strengthPercent)
        }
    }

    fun triggerWoopHaptic(context: Context, view: View?, durationMs: Int, strengthPercent: Int) {
        if (_hapticMode.value == HapticMode.ENHANCED) {
            val s = _enhancedHapticStrength.value.value * 1.5f
            HapticUtil.performTailoredHaptic(view?.context ?: context, s, CustomHapticType.WOOP)
        } else {
            vibrateSinglePulse(context, durationMs, strengthPercent)
        }
    }

    fun triggerContinuousLightHaptic(context: Context, view: View?, strengthPercent: Int) {
        if (!_scrollHapticsEnabled.value) return
        if (_hapticMode.value == HapticMode.ENHANCED) {
            val s = _enhancedHapticStrength.value.value * 0.8f
            HapticUtil.performTailoredHaptic(view?.context ?: context, s, CustomHapticType.LIGHT_TICK)
        } else {
            vibrateSinglePulse(context, 5, (strengthPercent * 0.35f).toInt())
        }
    }

    fun triggerScrollHaptic(context: Context, view: View?) {
        if (!_scrollHapticsEnabled.value) return
        if (_hapticMode.value == HapticMode.ENHANCED) {
            val s = _enhancedScrollHapticStrength.value
            HapticUtil.performTailoredHaptic(view?.context ?: context, s, CustomHapticType.SLIDER)
        } else {
            vibrateSinglePulse(context, 5, 20)
        }
    }

    fun triggerSliderHaptic(context: Context, view: View?, timings: LongArray, strengths: IntArray) {
        if (!_scrollHapticsEnabled.value) return
        if (_hapticMode.value == HapticMode.ENHANCED) {
            val s = _enhancedHapticStrength.value.value * 0.85f
            HapticUtil.performTailoredHaptic(view?.context ?: context, s, CustomHapticType.SLIDER)
        } else {
            vibratePattern(context, timings, strengths)
        }
    }

    fun triggerToggleSnapHaptic(context: Context, view: View?, durationMs: Int, strengthPercent: Int, reversed: Boolean = false) {
        if (_hapticMode.value == HapticMode.ENHANCED) {
            val s = _enhancedHapticStrength.value.value
            val type = if (reversed) CustomHapticType.TOGGLE_LIGHT else CustomHapticType.TOGGLE_SNAP
            HapticUtil.performTailoredHaptic(view?.context ?: context, s, type)
        } else {
            val dur = if (reversed) (durationMs * 0.7f).toInt().coerceAtLeast(1) else durationMs
            val str = if (reversed) (strengthPercent * 0.7f).toInt().coerceAtLeast(1) else strengthPercent
            vibrateSinglePulse(context, dur, str)
        }
    }

    fun triggerToggleLightHaptic(context: Context, view: View?, strengthPercent: Int, reversed: Boolean = false) {
        if (_hapticMode.value == HapticMode.ENHANCED) {
            val s = _enhancedHapticStrength.value.value * (if (reversed) 0.25f else 0.4f)
            val type = if (reversed) CustomHapticType.LIGHT_TICK else CustomHapticType.TOGGLE_LIGHT
            HapticUtil.performTailoredHaptic(view?.context ?: context, s, type)
        } else {
            val factor = if (reversed) 0.2f else 0.35f
            vibrateSinglePulse(context, 5, (strengthPercent * factor).toInt())
        }
    }

    fun triggerToggleHaptic(context: Context, view: View?, timings: LongArray, strengths: IntArray, reversed: Boolean = false) {
        if (_hapticMode.value == HapticMode.ENHANCED) {
            val s = _enhancedHapticStrength.value.value
            val type = if (reversed) CustomHapticType.TOGGLE_LIGHT else CustomHapticType.BUTTON
            HapticUtil.performTailoredHaptic(view?.context ?: context, s, type)
        } else {
            vibratePattern(context, timings, strengths)
        }
    }

    fun triggerUnlatchHaptic(context: Context, view: View?, strengthPercent: Int) {
        if (_hapticMode.value == HapticMode.ENHANCED) {
            val vibrator = getVibrator(view?.context ?: context)
            if (vibrator != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                try {
                    val s = _enhancedHapticStrength.value.value
                    if (vibrator.areAllPrimitivesSupported(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK) &&
                        vibrator.areAllPrimitivesSupported(android.os.VibrationEffect.Composition.PRIMITIVE_QUICK_FALL)) {
                        val composition = android.os.VibrationEffect.startComposition()
                        composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, (s * 1.2f).coerceIn(0.01f, 1.0f))
                        composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_QUICK_FALL, (s * 0.8f).coerceIn(0.01f, 1.0f), 15)
                        vibrator.vibrate(composition.compose())
                        return
                    }
                } catch (e: Exception) {}
            }
            vibratePattern(context, longArrayOf(0, 15, 20, 25), intArrayOf(0, strengthPercent, 0, (strengthPercent * 0.8f).toInt()))
        } else {
            vibratePattern(context, longArrayOf(0, 15, 20, 25), intArrayOf(0, strengthPercent, 0, (strengthPercent * 0.8f).toInt()))
        }
    }

    fun triggerTensionHaptic(context: Context, view: View?, strengthPercent: Int) {
        if (_hapticMode.value == HapticMode.ENHANCED) {
            val vibrator = getVibrator(view?.context ?: context)
            if (vibrator != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                try {
                    val s = _enhancedHapticStrength.value.value
                    if (vibrator.areAllPrimitivesSupported(android.os.VibrationEffect.Composition.PRIMITIVE_TICK)) {
                        val composition = android.os.VibrationEffect.startComposition()
                        composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_TICK, (s * 0.35f).coerceIn(0.01f, 1.0f))
                        vibrator.vibrate(composition.compose())
                        return
                    }
                } catch (e: Exception) {}
            }
            vibrateSinglePulse(context, 4, (strengthPercent * 0.2f).toInt())
        } else {
            vibrateSinglePulse(context, 4, (strengthPercent * 0.2f).toInt())
        }
    }

    fun triggerGoalHaptic(context: Context, view: View?, timings: LongArray, strengths: IntArray) {
        if (_hapticMode.value == HapticMode.ENHANCED) {
            val s = _enhancedHapticStrength.value.value * 1.5f
            HapticUtil.performTailoredHaptic(view?.context ?: context, s, CustomHapticType.GOAL)
        } else {
            vibratePattern(context, timings, strengths)
        }
    }

    fun triggerFireworksHaptic(context: Context, view: View?, timings: LongArray, strengths: IntArray) {
        if (_hapticMode.value == HapticMode.ENHANCED) {
            val s = _enhancedHapticStrength.value.value * 1.5f
            HapticUtil.performTailoredHaptic(view?.context ?: context, s, CustomHapticType.FIREWORKS)
        } else {
            vibratePattern(context, timings, strengths)
        }
    }

    fun triggerConfettiBurstHaptic(context: Context, view: View?, timings: LongArray, strengths: IntArray) {
        if (_hapticMode.value == HapticMode.ENHANCED) {
            val s = _enhancedHapticStrength.value.value * 1.5f
            HapticUtil.performTailoredHaptic(view?.context ?: context, s, CustomHapticType.CONFETTI_BURST)
        } else {
            vibratePattern(context, timings, strengths)
        }
    }

    fun triggerSideBurstHaptic(context: Context, view: View?, timings: LongArray, strengths: IntArray) {
        if (_hapticMode.value == HapticMode.ENHANCED) {
            val s = _enhancedHapticStrength.value.value * 1.5f
            HapticUtil.performTailoredHaptic(view?.context ?: context, s, CustomHapticType.SIDE_BURST)
        } else {
            vibratePattern(context, timings, strengths)
        }
    }

    fun triggerRainfallHaptic(context: Context, view: View?, timings: LongArray, strengths: IntArray) {
        if (_hapticMode.value == HapticMode.ENHANCED) {
            val s = _enhancedHapticStrength.value.value * 1.5f
            HapticUtil.performTailoredHaptic(view?.context ?: context, s, CustomHapticType.RAINFALL)
        } else {
            vibratePattern(context, timings, strengths)
        }
    }

    fun triggerTabClickHaptic(context: Context, view: View?, index: Int, durationMs: Int, strengthPercent: Int) {
        if (_hapticMode.value == HapticMode.ENHANCED) {
            val s = _enhancedHapticStrength.value.value
            val customType = when (index) {
                0 -> CustomHapticType.TAB_TRACK
                1 -> CustomHapticType.TAB_STATS
                2 -> CustomHapticType.TAB_AI
                3 -> CustomHapticType.TAB_REMINDERS
                4 -> CustomHapticType.TAB_SETTINGS
                else -> CustomHapticType.BUTTON
            }
            HapticUtil.performTailoredHaptic(view?.context ?: context, s, customType)
        } else {
            val (dur, strength) = when (index) {
                0 -> Pair(durationMs, strengthPercent)
                1 -> Pair((durationMs * 1.3f).toInt(), (strengthPercent * 1.1f).toInt().coerceAtMost(100))
                2 -> Pair((durationMs * 0.9f).toInt(), (strengthPercent * 0.95f).toInt())
                3 -> Pair((durationMs * 1.2f).toInt(), (strengthPercent * 0.9f).toInt())
                4 -> Pair((durationMs * 1.1f).toInt(), (strengthPercent * 1.05f).toInt().coerceAtMost(100))
                else -> Pair(durationMs, strengthPercent)
            }
            vibrateSinglePulse(context, dur, strength)
        }
    }

    private fun vibrateSinglePulse(context: Context, durationMs: Int, strengthPercent: Int) {
        if (durationMs <= 0) return
        val vibrator = getVibrator(context) ?: return
        val amplitude = (strengthPercent / 100f * 255).toInt().coerceIn(1, 255)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs.toLong(), amplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs.toLong())
            }
        } catch (e: Throwable) {
            // Ignore
        }
    }

    private fun vibratePattern(context: Context, timings: LongArray, strengths: IntArray) {
        if (timings.isEmpty() || strengths.isEmpty() || timings.size != strengths.size) return
        val finalTimings = timings.mapIndexed { index, t ->
            if (index == 0) t else maxOf(1L, t)
        }.toLongArray()
        val vibrator = getVibrator(context) ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amplitudes = strengths.map { (it / 100f * 255).toInt().coerceIn(0, 255) }.toIntArray()
                if (vibrator.hasAmplitudeControl()) {
                    vibrator.vibrate(VibrationEffect.createWaveform(finalTimings, amplitudes, -1))
                } else {
                    vibrator.vibrate(VibrationEffect.createWaveform(finalTimings, -1))
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(finalTimings, -1)
            }
        } catch (e: Throwable) {
            // Ignore
        }
    }
}
