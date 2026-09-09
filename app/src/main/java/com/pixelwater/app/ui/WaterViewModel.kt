package com.pixelwater.app.ui

import android.app.Application
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pixelwater.app.data.WaterDatabase
import com.pixelwater.app.data.WaterLog
import com.pixelwater.app.data.WaterRepository
import com.pixelwater.app.data.HealthConnectManager
import com.pixelwater.app.notifications.NotificationHelper
import com.pixelwater.app.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.Build
import android.view.View
import android.app.WallpaperManager
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.Spring
import java.util.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import org.json.JSONArray
import org.json.JSONObject
import java.text.DateFormat

sealed interface WearUpdateStatus {
    object Idle : WearUpdateStatus
    object Checking : WearUpdateStatus
    data class UpdateAvailable(
        val tagName: String,
        val name: String,
        val body: String,
        val downloadUrl: String,
        val sizeBytes: Long,
        val publishedAt: String,
        val hasWearApk: Boolean = true,
        val zipBackupUrl: String = ""
    ) : WearUpdateStatus
    object UpToDate : WearUpdateStatus
    data class Error(val message: String) : WearUpdateStatus
    data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : WearUpdateStatus
    data class Downloaded(val localPath: String, val publishedAt: String) : WearUpdateStatus
}

sealed interface UpdateStatus {
    object Idle : UpdateStatus
    object Checking : UpdateStatus
    data class UpdateAvailable(
        val tagName: String,
        val name: String,
        val body: String,
        val downloadUrl: String,
        val sizeBytes: Long,
        val publishedAt: String,
        val hasApk: Boolean = true,
        val zipBackupUrl: String = ""
    ) : UpdateStatus
    object UpToDate : UpdateStatus
    data class Error(val message: String) : UpdateStatus
    data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : UpdateStatus
    data class Downloaded(val localPath: String, val publishedAt: String) : UpdateStatus
}

enum class VibrationPatternPreset(val displayName: String) {
    DOUBLE_PULSE("Double Pulse"),
    SINGLE_PULSE("Single Crisp"),
    TRIPLE_PULSE("Triple Celebration"),
    HEARTBEAT("Heartbeat Rhythm"),
    VIBRANT_WAVE("Vibrant Wave"),
    CUSTOM("Custom Adjustment")
}

data class RapidOverhydrationState(
    val isOverhydrated: Boolean = false,
    val recent60MinIntakeMl: Int = 0,
    val excessVolumeMl: Int = 0,
    val renalClearanceRateMlMin: Double = 15.0,
    val estimatedClearanceMinutes: Int = 0,
    val safeDrinkTimestampMillis: Long? = null,
    val remainingSeconds: Long = 0L
)

class WaterViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs: SharedPreferences = application.getSharedPreferences("water_tracker_prefs", Context.MODE_PRIVATE)
    private val database = WaterDatabase.getDatabase(application)
    private val repository = WaterRepository(database.waterLogDao())
    private val externalSyncRepository = com.pixelwater.app.data.ExternalHydrationSyncRepository(application, database)

    private val _lateNightLoggingEnabled = MutableStateFlow(prefs.getBoolean("late_night_logging_enabled", true))
    val lateNightLoggingEnabled: StateFlow<Boolean> = _lateNightLoggingEnabled.asStateFlow()

    private val _lateNightRolloverHour = MutableStateFlow(prefs.getInt("late_night_rollover_hour", 4))
    val lateNightRolloverHour: StateFlow<Int> = _lateNightRolloverHour.asStateFlow()

    private val _currentDate = MutableStateFlow(getCurrentDateString())
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    // Health Connect Authorization Status State
    val isHealthConnectAvailable = HealthConnectManager.isSdkAvailable(application)
    private val _isHealthConnectAuthorized = MutableStateFlow(false)
    val isHealthConnectAuthorized: StateFlow<Boolean> = _isHealthConnectAuthorized.asStateFlow()

    // Preferences exposed as Flow state
    private val _appTheme = MutableStateFlow(prefs.getString("app_theme", "STATIC") ?: "STATIC")
    val appTheme: StateFlow<String> = _appTheme.asStateFlow()

    private val _showWolfRain = MutableStateFlow(false)
    val showWolfRain: StateFlow<Boolean> = _showWolfRain.asStateFlow()

    fun triggerWolfEasterEgg(context: android.content.Context) {
        android.widget.Toast.makeText(context, "Γεια σου Λύκε", android.widget.Toast.LENGTH_LONG).show()
        kotlinx.coroutines.GlobalScope.launch {
            kotlinx.coroutines.delay(5000)
            _showWolfRain.value = true
            kotlinx.coroutines.delay(9000)
            _showWolfRain.value = false
        }
    }

    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _autoThemeScheduleEnabled = MutableStateFlow(prefs.getBoolean("auto_theme_schedule_enabled", false))
    val autoThemeScheduleEnabled: StateFlow<Boolean> = _autoThemeScheduleEnabled.asStateFlow()

    private val _autoThemeScheduleMode = MutableStateFlow(prefs.getString("auto_theme_schedule_mode", "SUNSET_SUNRISE") ?: "SUNSET_SUNRISE")
    val autoThemeScheduleMode: StateFlow<String> = _autoThemeScheduleMode.asStateFlow()

    private val _autoThemeLightStartHour = MutableStateFlow(prefs.getInt("auto_theme_light_start_hour", 7))
    val autoThemeLightStartHour: StateFlow<Int> = _autoThemeLightStartHour.asStateFlow()

    private val _autoThemeLightStartMin = MutableStateFlow(prefs.getInt("auto_theme_light_start_min", 0))
    val autoThemeLightStartMin: StateFlow<Int> = _autoThemeLightStartMin.asStateFlow()

    private val _autoThemeDarkStartHour = MutableStateFlow(prefs.getInt("auto_theme_dark_start_hour", 20))
    val autoThemeDarkStartHour: StateFlow<Int> = _autoThemeDarkStartHour.asStateFlow()

    private val _autoThemeDarkStartMin = MutableStateFlow(prefs.getInt("auto_theme_dark_start_min", 0))
    val autoThemeDarkStartMin: StateFlow<Int> = _autoThemeDarkStartMin.asStateFlow()

    private val _appThemePaletteIndex = MutableStateFlow(prefs.getInt("app_theme_palette_index", 0))
    val appThemePaletteIndex: StateFlow<Int> = _appThemePaletteIndex.asStateFlow()

    private val _staticThemeSeed = MutableStateFlow(prefs.getInt("static_theme_seed", 0xFF1D5AAB.toInt()))
    val staticThemeSeed: StateFlow<Int> = _staticThemeSeed.asStateFlow()

    private val _monochromeEnabled = MutableStateFlow(prefs.getBoolean("monochrome_enabled", false))
    val monochromeEnabled: StateFlow<Boolean> = _monochromeEnabled.asStateFlow()

    private val _progressCircleColorSource = MutableStateFlow(prefs.getString("progress_circle_color_source", "THEME") ?: "THEME")
    val progressCircleColorSource: StateFlow<String> = _progressCircleColorSource.asStateFlow()

    private val _progressCircleThemeColor = MutableStateFlow(prefs.getInt("progress_circle_theme_color", 0))
    val progressCircleThemeColor: StateFlow<Int> = _progressCircleThemeColor.asStateFlow()

    private val _progressCircleStandardColorIndex = MutableStateFlow(prefs.getInt("progress_circle_standard_color_index", 0))
    val progressCircleStandardColorIndex: StateFlow<Int> = _progressCircleStandardColorIndex.asStateFlow()

    private val _monochromeColorTarget = MutableStateFlow(prefs.getInt("monochrome_color_target", 0))
    val monochromeColorTarget: StateFlow<Int> = _monochromeColorTarget.asStateFlow()

    private val _secretLennyCount = MutableStateFlow(prefs.getInt("secret_lenny_count", 0))
    val secretLennyCount: StateFlow<Int> = _secretLennyCount.asStateFlow()

    private val _isDeveloper = MutableStateFlow(prefs.getBoolean("is_developer", false))
    val isDeveloper: StateFlow<Boolean> = _isDeveloper.asStateFlow()

    private val _isFartModeEnabled = MutableStateFlow(prefs.getBoolean("is_fart_mode_enabled", false))
    val isFartModeEnabled: StateFlow<Boolean> = _isFartModeEnabled.asStateFlow()

    private val _isBlurEffectEnabled = MutableStateFlow(prefs.getBoolean("is_blur_effect_enabled", false))
    val isBlurEffectEnabled: StateFlow<Boolean> = _isBlurEffectEnabled.asStateFlow()

    private val _experimentalBlurRadius = MutableStateFlow(prefs.getFloat("experimental_blur_radius", 20f))
    val experimentalBlurRadius: StateFlow<Float> = _experimentalBlurRadius.asStateFlow()

    private val _isRainbowBorderEnabled = MutableStateFlow(prefs.getBoolean("is_rainbow_border_enabled", false))
    val isRainbowBorderEnabled: StateFlow<Boolean> = _isRainbowBorderEnabled.asStateFlow()

    private val _experimentalBorderWidth = MutableStateFlow(prefs.getFloat("experimental_border_width", 1.0f))
    val experimentalBorderWidth: StateFlow<Float> = _experimentalBorderWidth.asStateFlow()

    private val _experimentalCardScale = MutableStateFlow(prefs.getFloat("experimental_card_scale", 1.0f))
    val experimentalCardScale: StateFlow<Float> = _experimentalCardScale.asStateFlow()

    private val _experimentalHueShiftDuration = MutableStateFlow(prefs.getFloat("experimental_hue_shift_duration", 5f))
    val experimentalHueShiftDuration: StateFlow<Float> = _experimentalHueShiftDuration.asStateFlow()

    private val _devFontScale = MutableStateFlow(prefs.getFloat("dev_font_scale", 1.0f))
    val devFontScale: StateFlow<Float> = _devFontScale.asStateFlow()

    private val _devFontWeight = MutableStateFlow(prefs.getFloat("dev_font_weight", 500f))
    val devFontWeight: StateFlow<Float> = _devFontWeight.asStateFlow()

    private val _devSmallestWidth = MutableStateFlow(prefs.getFloat("dev_smallest_width", 411f))
    val devSmallestWidth: StateFlow<Float> = _devSmallestWidth.asStateFlow()

    private val _devAnimatorDurationScale = MutableStateFlow(prefs.getFloat("dev_animator_duration_scale", 1.0f))
    val devAnimatorDurationScale: StateFlow<Float> = _devAnimatorDurationScale.asStateFlow()

    private val _devTransitionAnimationScale = MutableStateFlow(prefs.getFloat("dev_transition_animation_scale", 1.0f))
    val devTransitionAnimationScale: StateFlow<Float> = _devTransitionAnimationScale.asStateFlow()

    private val _devWindowAnimationScale = MutableStateFlow(prefs.getFloat("dev_window_animation_scale", 1.0f))
    val devWindowAnimationScale: StateFlow<Float> = _devWindowAnimationScale.asStateFlow()

    private val _isVoiceModeEnabled = MutableStateFlow(prefs.getBoolean("is_voice_mode_enabled", false))
    val isVoiceModeEnabled: StateFlow<Boolean> = _isVoiceModeEnabled.asStateFlow()

    private val _legalDisclaimerAccepted = MutableStateFlow(prefs.getBoolean("legal_disclaimer_accepted_v2", false))
    val legalDisclaimerAccepted: StateFlow<Boolean> = _legalDisclaimerAccepted.asStateFlow()

    private val _forceShowDisclaimerDialog = MutableStateFlow(false)
    val forceShowDisclaimerDialog: StateFlow<Boolean> = _forceShowDisclaimerDialog.asStateFlow()

    fun acceptLegalDisclaimer() {
        _legalDisclaimerAccepted.value = true
        prefs.edit().putBoolean("legal_disclaimer_accepted_v2", true).apply()
    }

    fun resetLegalDisclaimer() {
        _legalDisclaimerAccepted.value = false
        prefs.edit().putBoolean("legal_disclaimer_accepted_v2", false).apply()
    }

    fun setForceShowDisclaimerDialog(show: Boolean) {
        _forceShowDisclaimerDialog.value = show
    }

    private val _firstUseSetupCompleted = MutableStateFlow(prefs.getBoolean("first_use_setup_completed", true))
    val firstUseSetupCompleted: StateFlow<Boolean> = _firstUseSetupCompleted.asStateFlow()

    fun completeFirstUseSetup() {
        _firstUseSetupCompleted.value = true
        prefs.edit().putBoolean("first_use_setup_completed", true).apply()
    }

    val hapticMode = HapticManager.hapticMode
    val scrollHapticsEnabled = HapticManager.scrollHapticsEnabled
    val scrollHapticsPixels = HapticManager.scrollHapticsPixels
    val enhancedScrollHapticStrength = HapticManager.enhancedScrollHapticStrength
    val enhancedHapticStrength = HapticManager.enhancedHapticStrength

    fun updateHapticMode(mode: HapticMode) {
        HapticManager.setHapticMode(getApplication(), mode)
    }

    fun updateScrollHapticsEnabled(enabled: Boolean) {
        HapticManager.setScrollHapticsEnabled(getApplication(), enabled)
    }

    fun updateScrollHapticsPixels(pixels: Float) {
        HapticManager.setScrollHapticsPixels(getApplication(), pixels)
    }

    fun updateEnhancedHapticStrength(strength: EnhancedHapticStrength) {
        HapticManager.setEnhancedHapticStrength(getApplication(), strength)
    }

    fun updateEnhancedScrollHapticStrength(strength: Float) {
        HapticManager.setEnhancedScrollHapticStrength(getApplication(), strength)
    }

    fun updateSettingsSubPage(page: String?) {
        // This is a placeholder as the property seems to be managed locally in the Composable
    }

    fun updateDeveloperMode(enabled: Boolean) {
        _isDeveloper.value = enabled
        prefs.edit().putBoolean("is_developer", enabled).apply()
    }

    fun updateFartModeEnabled(enabled: Boolean) {
        _isFartModeEnabled.value = enabled
        prefs.edit().putBoolean("is_fart_mode_enabled", enabled).apply()
        if (enabled) {
            _isVoiceModeEnabled.value = false
            prefs.edit().putBoolean("is_voice_mode_enabled", false).apply()
        }
    }

    fun updateBlurEffectEnabled(enabled: Boolean) {
        _isBlurEffectEnabled.value = enabled
        prefs.edit().putBoolean("is_blur_effect_enabled", enabled).apply()
    }

    fun updateExperimentalBlurRadius(radius: Float) {
        _experimentalBlurRadius.value = radius
        prefs.edit().putFloat("experimental_blur_radius", radius).apply()
    }

    fun updateRainbowBorderEnabled(enabled: Boolean) {
        _isRainbowBorderEnabled.value = enabled
        prefs.edit().putBoolean("is_rainbow_border_enabled", enabled).apply()
    }

    fun updateExperimentalBorderWidth(width: Float) {
        _experimentalBorderWidth.value = width
        prefs.edit().putFloat("experimental_border_width", width).apply()
    }

    fun updateExperimentalCardScale(scale: Float) {
        _experimentalCardScale.value = scale
        prefs.edit().putFloat("experimental_card_scale", scale).apply()
    }

    fun updateExperimentalHueShiftDuration(duration: Float) {
        _experimentalHueShiftDuration.value = duration
        prefs.edit().putFloat("experimental_hue_shift_duration", duration).apply()
    }

    fun updateDevFontScale(scale: Float) {
        _devFontScale.value = scale
        prefs.edit().putFloat("dev_font_scale", scale).apply()
    }

    fun updateDevFontWeight(weight: Float) {
        _devFontWeight.value = weight
        prefs.edit().putFloat("dev_font_weight", weight).apply()
        FontWeight.globalDevFontWeight = weight
    }

    fun updateDevSmallestWidth(width: Float) {
        _devSmallestWidth.value = width
        prefs.edit().putFloat("dev_smallest_width", width).apply()
    }

    fun updateDevAnimatorDurationScale(scale: Float) {
        _devAnimatorDurationScale.value = scale
        prefs.edit().putFloat("dev_animator_duration_scale", scale).apply()
    }

    fun updateDevTransitionAnimationScale(scale: Float) {
        _devTransitionAnimationScale.value = scale
        prefs.edit().putFloat("dev_transition_animation_scale", scale).apply()
    }

    fun updateDevWindowAnimationScale(scale: Float) {
        _devWindowAnimationScale.value = scale
        prefs.edit().putFloat("dev_window_animation_scale", scale).apply()
    }

    fun updateVoiceModeEnabled(enabled: Boolean) {
        _isVoiceModeEnabled.value = enabled
        prefs.edit().putBoolean("is_voice_mode_enabled", enabled).apply()
        if (enabled) {
            _isFartModeEnabled.value = false
            prefs.edit().putBoolean("is_fart_mode_enabled", false).apply()
        }
    }

    fun playSound(context: Context) {
        if (_isFartModeEnabled.value) {
            playFartSound(context)
        } else if (_isVoiceModeEnabled.value) {
            playVoiceSound(context)
        }
    }

    private fun playFartSound(context: Context) {
        try {
            val fartSounds = listOf(
                R.raw.fart_1,
                R.raw.fart_2,
                R.raw.fart_3,
                R.raw.fart_4,
                R.raw.fart_5
            )
            val sound = fartSounds.random()
            val mediaPlayer = android.media.MediaPlayer.create(context, sound)
            mediaPlayer.setOnCompletionListener { it.release() }
            mediaPlayer.start()
        } catch (e: Exception) {
            Log.e("WaterViewModel", "Error playing fart sound", e)
        }
    }

    private fun playVoiceSound(context: Context) {
        try {
            val voiceSounds = listOf(
                R.raw.voice_1,
                R.raw.voice_2,
                R.raw.voice_3
            )
            val sound = voiceSounds.random()
            val mediaPlayer = android.media.MediaPlayer.create(context, sound)
            mediaPlayer.setOnCompletionListener { it.release() }
            mediaPlayer.start()
        } catch (e: Exception) {
            Log.e("WaterViewModel", "Error playing voice sound", e)
        }
    }

    fun generateMockData() {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            for (i in 0 until 365) {
                val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                val amount = (1000..3000).random()
                repository.insertLog(WaterLog(
                    waterEquivalentMl = amount,
                    amountMl = amount,
                    timestamp = calendar.timeInMillis,
                    dateString = dateString,
                    beverageType = "Test"
                ))
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            }
        }
    }

    fun generateTestPastData60Days(onComplete: () -> Unit = {}) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            for (i in 35..50) {
                val pastCal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -i)
                }
                val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(pastCal.time)
                repository.insertLog(WaterLog(
                    waterEquivalentMl = 2200,
                    amountMl = 2200,
                    timestamp = pastCal.timeInMillis,
                    dateString = dateString,
                    beverageType = "Water"
                ))
            }
            calculateStreak()
            refreshCurrentUserDataSize()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun clearMockData() {
        viewModelScope.launch {
            repository.deleteAllLogs()
        }
    }

    fun resetTestLogsOnly() {
        viewModelScope.launch {
            repository.deleteLogsByType("Test")
        }
    }

    fun restoreRealData() {
        viewModelScope.launch {
            repository.deleteAllLogs()
        }
    }

    fun incrementSecretLennyCount() {
        val next = _secretLennyCount.value + 1
        _secretLennyCount.value = next
        prefs.edit().putInt("secret_lenny_count", next).apply()
    }

    fun generateTestHistoryData() {
        generateMockData()
    }

    val wallpaperThemeColors: StateFlow<List<Int>> = flow {
        emit(getWallpaperSeedColors(getApplication()))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, listOf(
        0xFF1D5AAB.toInt(), // Sky Blue
        0xFF4B5563.toInt(), // Slate Gray
        0xFFD97706.toInt(), // Warm Amber / Orange
        0xFF3B82F6.toInt(), // Deep Royal Blue
        0xFFEC4899.toInt()  // Hot Pink
    ))

    private val _settingsDividerStyle = MutableStateFlow(prefs.getString("settings_divider_style", "SQUIGGLY") ?: "SQUIGGLY")
    val settingsDividerStyle: StateFlow<String> = _settingsDividerStyle.asStateFlow()

    private val _goalLineSquiggly = MutableStateFlow(prefs.getBoolean("goal_line_squiggly", false))
    val goalLineSquiggly: StateFlow<Boolean> = _goalLineSquiggly.asStateFlow()

    private val _settingsDividerContrast = MutableStateFlow(prefs.getString("settings_divider_contrast", "MEDIUM") ?: "MEDIUM")
    val settingsDividerContrast: StateFlow<String> = _settingsDividerContrast.asStateFlow()

    private val _settingsGapScale = MutableStateFlow(prefs.getFloat("settings_gap_scale", 1.0f))
    val settingsGapScale: StateFlow<Float> = _settingsGapScale.asStateFlow()

    private val _settingsAdaptiveGrouping = MutableStateFlow(prefs.getBoolean("settings_adaptive_grouping", true))
    val settingsAdaptiveGrouping: StateFlow<Boolean> = _settingsAdaptiveGrouping.asStateFlow()

    private val _settingsAdaptiveCornerRadius = MutableStateFlow(prefs.getInt("settings_adaptive_corner_radius", 5))
    val settingsAdaptiveCornerRadius: StateFlow<Int> = _settingsAdaptiveCornerRadius.asStateFlow()

    private val _oledModeEnabled = MutableStateFlow(prefs.getBoolean("oled_mode_enabled", false))
    val oledModeEnabled: StateFlow<Boolean> = _oledModeEnabled.asStateFlow()

    private val _vividLightBoxesEnabled = MutableStateFlow(prefs.getBoolean("vivid_light_boxes_enabled", true))
    val vividLightBoxesEnabled: StateFlow<Boolean> = _vividLightBoxesEnabled.asStateFlow()

    private val _keepLightBackgroundWhite = MutableStateFlow(prefs.getBoolean("keep_light_background_white", false))
    val keepLightBackgroundWhite: StateFlow<Boolean> = _keepLightBackgroundWhite.asStateFlow()

    private val _keepCircleInsideOpaque = MutableStateFlow(prefs.getBoolean("keep_circle_inside_opaque", true))
    val keepCircleInsideOpaque: StateFlow<Boolean> = _keepCircleInsideOpaque.asStateFlow()

    private val _mainLongPressDeleteEnabled = MutableStateFlow(prefs.getBoolean("main_long_press_delete_enabled", true))
    val mainLongPressDeleteEnabled: StateFlow<Boolean> = _mainLongPressDeleteEnabled.asStateFlow()

    private val _mainLongPressDeleteDuration = MutableStateFlow(prefs.getInt("main_long_press_delete_duration", 500))
    val mainLongPressDeleteDuration: StateFlow<Int> = _mainLongPressDeleteDuration.asStateFlow()

    private val _wearInsideCircleRemoved = MutableStateFlow(prefs.getBoolean("wear_inside_circle_removed", false))
    val wearInsideCircleRemoved: StateFlow<Boolean> = _wearInsideCircleRemoved.asStateFlow()

    private val _boxBgOledEnabled = MutableStateFlow(prefs.getBoolean("box_bg_oled_enabled", false))
    val boxBgOledEnabled: StateFlow<Boolean> = _boxBgOledEnabled.asStateFlow()

    private val _boxBgSource = MutableStateFlow(prefs.getString("box_bg_source", "THEME") ?: "THEME")
    val boxBgSource: StateFlow<String> = _boxBgSource.asStateFlow()

    private val _boxBgFixedIndex = MutableStateFlow(prefs.getInt("box_bg_fixed_index", 0))
    val boxBgFixedIndex: StateFlow<Int> = _boxBgFixedIndex.asStateFlow()

    private val _boxBgR = MutableStateFlow(prefs.getFloat("box_bg_r", 128f))
    val boxBgR: StateFlow<Float> = _boxBgR.asStateFlow()

    private val _boxBgG = MutableStateFlow(prefs.getFloat("box_bg_g", 128f))
    val boxBgG: StateFlow<Float> = _boxBgG.asStateFlow()

    private val _boxBgB = MutableStateFlow(prefs.getFloat("box_bg_b", 128f))
    val boxBgB: StateFlow<Float> = _boxBgB.asStateFlow()

    private val _boxBgPaletteChoice = MutableStateFlow(prefs.getInt("box_bg_palette_choice", 0))
    val boxBgPaletteChoice: StateFlow<Int> = _boxBgPaletteChoice.asStateFlow()

    private val _isFrostedGlassEnabled = MutableStateFlow(prefs.getBoolean("frosted_glass_enabled", false))
    val isFrostedGlassEnabled: StateFlow<Boolean> = _isFrostedGlassEnabled.asStateFlow()

    private val _frostedGlassTransparency = MutableStateFlow(prefs.getFloat("frosted_glass_transparency", 0.55f))
    val frostedGlassTransparency: StateFlow<Float> = _frostedGlassTransparency.asStateFlow()

    private val _transparentComponentsEnabled = MutableStateFlow(prefs.getBoolean("transparent_components_enabled", false))
    val transparentComponentsEnabled: StateFlow<Boolean> = _transparentComponentsEnabled.asStateFlow()

    private val _componentsTransparency = MutableStateFlow(prefs.getFloat("components_transparency", 0.35f))
    val componentsTransparency: StateFlow<Float> = _componentsTransparency.asStateFlow()

    private val _lightModeDarkTextEnabled = MutableStateFlow(prefs.getBoolean("light_mode_dark_text_enabled", false))
    val lightModeDarkTextEnabled: StateFlow<Boolean> = _lightModeDarkTextEnabled.asStateFlow()

    private val _baseDailyGoalMl = MutableStateFlow(prefs.getInt("daily_goal", 2000))
    
    private val _workoutWaterAdjustmentEnabled = MutableStateFlow(prefs.getBoolean("workout_water_adjustment_enabled", false))
    val workoutWaterAdjustmentEnabled: StateFlow<Boolean> = _workoutWaterAdjustmentEnabled.asStateFlow()

    private val _bypassWorkoutConfirmation = MutableStateFlow(prefs.getBoolean("bypass_workout_confirmation", false))
    val bypassWorkoutConfirmation: StateFlow<Boolean> = _bypassWorkoutConfirmation.asStateFlow()

    private val _sleepWaterAdjustmentEnabled = MutableStateFlow(prefs.getBoolean("sleep_water_adjustment_enabled", true))
    val sleepWaterAdjustmentEnabled: StateFlow<Boolean> = _sleepWaterAdjustmentEnabled.asStateFlow()

    private val _bypassSleepConfirmation = MutableStateFlow(prefs.getBoolean("bypass_sleep_confirmation", false))
    val bypassSleepConfirmation: StateFlow<Boolean> = _bypassSleepConfirmation.asStateFlow()

    private val _workoutWalkingCounts = MutableStateFlow(prefs.getBoolean("workout_walking_counts", true))
    val workoutWalkingCounts: StateFlow<Boolean> = _workoutWalkingCounts.asStateFlow()

    fun updateWorkoutWalkingCounts(enabled: Boolean) {
        _workoutWalkingCounts.value = enabled
        prefs.edit().putBoolean("workout_walking_counts", enabled).apply()
    }

    fun updateLateNightLoggingEnabled(enabled: Boolean) {
        _lateNightLoggingEnabled.value = enabled
        prefs.edit().putBoolean("late_night_logging_enabled", enabled).apply()
        _currentDate.value = getCurrentDateString()
    }

    fun updateLateNightRolloverHour(hour: Int) {
        _lateNightRolloverHour.value = hour
        prefs.edit().putInt("late_night_rollover_hour", hour).apply()
        _currentDate.value = getCurrentDateString()
    }

    private val _badSleepThreshold = MutableStateFlow(prefs.getFloat("bad_sleep_threshold", 6.5f))
    val badSleepThreshold: StateFlow<Float> = _badSleepThreshold.asStateFlow()

    fun updateBadSleepThreshold(hours: Float) {
        _badSleepThreshold.value = hours
        prefs.edit().putFloat("bad_sleep_threshold", hours).apply()
    }

    private val _aiCoachSleepDetectionEnabled = MutableStateFlow(prefs.getBoolean("ai_coach_sleep_detection_enabled", true))
    val aiCoachSleepDetectionEnabled: StateFlow<Boolean> = _aiCoachSleepDetectionEnabled.asStateFlow()

    fun updateAiCoachSleepDetectionEnabled(enabled: Boolean) {
        _aiCoachSleepDetectionEnabled.value = enabled
        prefs.edit().putBoolean("ai_coach_sleep_detection_enabled", enabled).apply()
    }

    private val _showHydrationInsight = MutableStateFlow(prefs.getBoolean("show_hydration_insight", true))
    val showHydrationInsight: StateFlow<Boolean> = _showHydrationInsight.asStateFlow()

    fun updateShowHydrationInsight(show: Boolean) {
        _showHydrationInsight.value = show
        prefs.edit().putBoolean("show_hydration_insight", show).apply()
    }

    private val _insightTapRefreshGesture = MutableStateFlow(prefs.getString("insight_tap_refresh_gesture", "triple") ?: "triple")
    val insightTapRefreshGesture: StateFlow<String> = _insightTapRefreshGesture.asStateFlow()

    fun updateInsightTapRefreshGesture(gesture: String) {
        _insightTapRefreshGesture.value = gesture
        prefs.edit().putString("insight_tap_refresh_gesture", gesture).apply()
    }

    fun updateShowInsightGreeting(show: Boolean) {
        _showInsightGreeting.value = show
        prefs.edit().putBoolean("show_insight_greeting", show).apply()
    }

    private val _showInsightFaces = MutableStateFlow(prefs.getBoolean("show_insight_faces", true))
    val showInsightFaces: StateFlow<Boolean> = _showInsightFaces.asStateFlow()

    fun updateShowInsightFaces(show: Boolean) {
        _showInsightFaces.value = show
        prefs.edit().putBoolean("show_insight_faces", show).apply()
    }

    private val _collapseLongInsightsEnabled = MutableStateFlow(prefs.getBoolean("collapse_long_insights_enabled", true))
    val collapseLongInsightsEnabled: StateFlow<Boolean> = _collapseLongInsightsEnabled.asStateFlow()

    fun updateCollapseLongInsightsEnabled(enabled: Boolean) {
        _collapseLongInsightsEnabled.value = enabled
        prefs.edit().putBoolean("collapse_long_insights_enabled", enabled).apply()
    }

    private val _collapseInsightThresholdLines = MutableStateFlow(prefs.getInt("collapse_insight_threshold_lines", 10))
    val collapseInsightThresholdLines: StateFlow<Int> = _collapseInsightThresholdLines.asStateFlow()

    fun updateCollapseInsightThresholdLines(lines: Int) {
        _collapseInsightThresholdLines.value = lines
        prefs.edit().putInt("collapse_insight_threshold_lines", lines).apply()
    }

    private val _showDeviceOriginText = MutableStateFlow(prefs.getBoolean("show_device_origin_text", false))
    val showDeviceOriginText: StateFlow<Boolean> = _showDeviceOriginText.asStateFlow()

    fun updateShowDeviceOriginText(enabled: Boolean) {
        _showDeviceOriginText.value = enabled
        prefs.edit().putBoolean("show_device_origin_text", enabled).apply()
    }

    private val _boldDeviceOriginText = MutableStateFlow(prefs.getBoolean("bold_device_origin_text", true))
    val boldDeviceOriginText: StateFlow<Boolean> = _boldDeviceOriginText.asStateFlow()

    fun updateBoldDeviceOriginText(enabled: Boolean) {
        _boldDeviceOriginText.value = enabled
        prefs.edit().putBoolean("bold_device_origin_text", enabled).apply()
    }

    private val _showWaterRemaining = MutableStateFlow(prefs.getBoolean("show_water_remaining", false))
    val showWaterRemaining: StateFlow<Boolean> = _showWaterRemaining.asStateFlow()

    private val _wearSwipeMode = MutableStateFlow(prefs.getString("wear_swipe_mode", "VERTICAL") ?: "VERTICAL")
    val wearSwipeMode: StateFlow<String> = _wearSwipeMode.asStateFlow()

    private val _wearGraphSwipeDir = MutableStateFlow(prefs.getString("wear_graph_swipe_dir", "LEFT") ?: "LEFT")
    val wearGraphSwipeDir: StateFlow<String> = _wearGraphSwipeDir.asStateFlow()

    private val _wearComplicationIconStyle = MutableStateFlow(prefs.getString("wear_complication_icon_style", "DROPLET") ?: "DROPLET")
    val wearComplicationIconStyle: StateFlow<String> = _wearComplicationIconStyle.asStateFlow()

    private val _wearShowRemaining = MutableStateFlow(prefs.getBoolean("wear_show_remaining", false))
    val wearShowRemaining: StateFlow<Boolean> = _wearShowRemaining.asStateFlow()

    private val _wearShowTomorrowDay = MutableStateFlow(prefs.getBoolean("wear_show_tomorrow_day", true))
    val wearShowTomorrowDay: StateFlow<Boolean> = _wearShowTomorrowDay.asStateFlow()

    private val _wearHapticStrength = MutableStateFlow(prefs.getString("wear_haptic_strength", "MEDIUM") ?: "MEDIUM")
    val wearHapticStrength: StateFlow<String> = _wearHapticStrength.asStateFlow()

    private val _wearThemeColor = MutableStateFlow(prefs.getString("wear_theme_color", "MINT") ?: "MINT")
    val wearThemeColor: StateFlow<String> = _wearThemeColor.asStateFlow()

    private val _wearGraphWeeksPrior = MutableStateFlow(prefs.getInt("wear_graph_weeks_prior", 1))
    val wearGraphWeeksPrior: StateFlow<Int> = _wearGraphWeeksPrior.asStateFlow()

    private val _wearGraphDaysPrior = MutableStateFlow(prefs.getInt("wear_graph_days_prior", 5))
    val wearGraphDaysPrior: StateFlow<Int> = _wearGraphDaysPrior.asStateFlow()

    private val _wearPastDaysToShow = MutableStateFlow(prefs.getInt("wear_past_days_to_show", 3))
    val wearPastDaysToShow: StateFlow<Int> = _wearPastDaysToShow.asStateFlow()

    private val _mainCircleTextColorType = MutableStateFlow(prefs.getString("main_circle_text_color_type", "PROGRESS_COLOR") ?: "PROGRESS_COLOR")
    val mainCircleTextColorType: StateFlow<String> = _mainCircleTextColorType.asStateFlow()

    private val _mainCircleTextColorEnabled = MutableStateFlow(prefs.getBoolean("main_circle_text_color_enabled", false))
    val mainCircleTextColorEnabled: StateFlow<Boolean> = _mainCircleTextColorEnabled.asStateFlow()

    private val _separateSettingsTabEnabled = MutableStateFlow(prefs.getBoolean("separate_settings_tab_enabled", true))
    val separateSettingsTabEnabled: StateFlow<Boolean> = _separateSettingsTabEnabled.asStateFlow()

    private val _wearProgressCircleEnabled = MutableStateFlow(prefs.getBoolean("wear_progress_circle_enabled", true))
    val wearProgressCircleEnabled: StateFlow<Boolean> = _wearProgressCircleEnabled.asStateFlow()

    private val _wearProgressCircleThickness = MutableStateFlow(prefs.getString("wear_progress_circle_thickness", "THIN") ?: "THIN")
    val wearProgressCircleThickness: StateFlow<String> = _wearProgressCircleThickness.asStateFlow()

    private val _wearCustomThickness = MutableStateFlow(prefs.getFloat("wear_custom_thickness", 13f))
    val wearCustomThickness: StateFlow<Float> = _wearCustomThickness.asStateFlow()

    private val _wearGraphPillarThickness = MutableStateFlow(prefs.getFloat("wear_graph_pillar_thickness", 24f))
    val wearGraphPillarThickness: StateFlow<Float> = _wearGraphPillarThickness.asStateFlow()

    private val _wearGraphHorizontalPadding = MutableStateFlow(prefs.getFloat("wear_graph_horizontal_padding", 18f))
    val wearGraphHorizontalPadding: StateFlow<Float> = _wearGraphHorizontalPadding.asStateFlow()

    private val _wearCustomTextSize = MutableStateFlow(prefs.getFloat("wear_custom_text_size", 32f))
    val wearCustomTextSize: StateFlow<Float> = _wearCustomTextSize.asStateFlow()

    private val _wearTextColorType = MutableStateFlow(prefs.getString("wear_text_color_type", "SAME_AS_CIRCLE") ?: "SAME_AS_CIRCLE")
    val wearTextColorType: StateFlow<String> = _wearTextColorType.asStateFlow()

    private val _wearTextColorStatic = MutableStateFlow(prefs.getString("wear_text_color_static", "WHITE") ?: "WHITE")
    val wearTextColorStatic: StateFlow<String> = _wearTextColorStatic.asStateFlow()

    private val _wearCrownRotationEnabled = MutableStateFlow(prefs.getBoolean("wear_crown_rotation_enabled", true))
    val wearCrownRotationEnabled: StateFlow<Boolean> = _wearCrownRotationEnabled.asStateFlow()

    private val _wearCrownRotationReverse = MutableStateFlow(prefs.getBoolean("wear_crown_rotation_reverse", false))
    val wearCrownRotationReverse: StateFlow<Boolean> = _wearCrownRotationReverse.asStateFlow()

    private val _wearCrownClicksPerMl = MutableStateFlow(prefs.getInt("wear_crown_clicks_per_ml", 10))
    val wearCrownClicksPerMl: StateFlow<Int> = _wearCrownClicksPerMl.asStateFlow()

    private val _devCrownVibratePhone = MutableStateFlow(prefs.getBoolean("dev_crown_vibrate_phone", false))
    val devCrownVibratePhone: StateFlow<Boolean> = _devCrownVibratePhone.asStateFlow()
    
    private val _oledBackgroundAfterMidnight = MutableStateFlow(prefs.getBoolean("oled_background_after_midnight", false))
    val oledBackgroundAfterMidnight: StateFlow<Boolean> = _oledBackgroundAfterMidnight.asStateFlow()
    
    private val _oledBackgroundTime = MutableStateFlow(prefs.getInt("oled_background_time", 0))
    val oledBackgroundTime: StateFlow<Int> = _oledBackgroundTime.asStateFlow()

    private val _wearDynamicPaletteRole = MutableStateFlow(prefs.getString("wear_dynamic_palette_role", "PRIMARY") ?: "PRIMARY")
    val wearDynamicPaletteRole: StateFlow<String> = _wearDynamicPaletteRole.asStateFlow()

    private val _wearQuickAddMultiplier = MutableStateFlow(prefs.getFloat("wear_quick_add_multiplier", 1.0f))
    val wearQuickAddMultiplier: StateFlow<Float> = _wearQuickAddMultiplier.asStateFlow()

    private val _wearCircleSize = MutableStateFlow(prefs.getInt("wear_circle_size", -1))
    val wearCircleSize: StateFlow<Int> = _wearCircleSize.asStateFlow()

    private val _wearLongPressDeleteEnabled = MutableStateFlow(prefs.getBoolean("wear_long_press_delete_enabled", true))
    val wearLongPressDeleteEnabled: StateFlow<Boolean> = _wearLongPressDeleteEnabled.asStateFlow()

    private val _wearLongPressDeleteDuration = MutableStateFlow(prefs.getInt("wear_long_press_delete_duration", 500))
    val wearLongPressDeleteDuration: StateFlow<Int> = _wearLongPressDeleteDuration.asStateFlow()

    private val _wearRamOptimization = MutableStateFlow(prefs.getBoolean("wear_ram_optimization", false))
    val wearRamOptimization: StateFlow<Boolean> = _wearRamOptimization.asStateFlow()

    private val _resolvedProgressCircleColor = MutableStateFlow(prefs.getInt("resolved_progress_circle_color", 0xFF00BFA5.toInt()))
    val resolvedProgressCircleColor: StateFlow<Int> = _resolvedProgressCircleColor.asStateFlow()

    private val _wearCrownSyncDelay = MutableStateFlow(prefs.getInt("wear_crown_sync_delay", 200))
    val wearCrownSyncDelay: StateFlow<Int> = _wearCrownSyncDelay.asStateFlow()

    private val _wearDevOverlayEnabled = MutableStateFlow(prefs.getBoolean("wear_dev_overlay_enabled", false))
    val wearDevOverlayEnabled: StateFlow<Boolean> = _wearDevOverlayEnabled.asStateFlow()

    private val _wearDevOverlayR = MutableStateFlow(prefs.getInt("wear_dev_overlay_r", 0))
    val wearDevOverlayR: StateFlow<Int> = _wearDevOverlayR.asStateFlow()

    private val _wearDevOverlayG = MutableStateFlow(prefs.getInt("wear_dev_overlay_g", 255))
    val wearDevOverlayG: StateFlow<Int> = _wearDevOverlayG.asStateFlow()

    private val _wearDevOverlayB = MutableStateFlow(prefs.getInt("wear_dev_overlay_b", 0))
    val wearDevOverlayB: StateFlow<Int> = _wearDevOverlayB.asStateFlow()

    private val _isWatchConnected = MutableStateFlow(false)
    val isWatchConnected: StateFlow<Boolean> = _isWatchConnected.asStateFlow()

    private val _connectedWatchModel = MutableStateFlow("")
    val connectedWatchModel: StateFlow<String> = _connectedWatchModel.asStateFlow()

    private val _watchBatteryLevel = MutableStateFlow<Int?>(null)
    val watchBatteryLevel: StateFlow<Int?> = _watchBatteryLevel.asStateFlow()

    private val _watchCompanionDetected = MutableStateFlow(false)
    val watchCompanionDetected: StateFlow<Boolean> = _watchCompanionDetected.asStateFlow()

    private val watchDetailsListener = com.google.android.gms.wearable.DataClient.OnDataChangedListener { dataEvents ->
        for (event in dataEvents) {
            if (event.type == com.google.android.gms.wearable.DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/watch_details") {
                val dataMap = com.google.android.gms.wearable.DataMapItem.fromDataItem(event.dataItem).dataMap
                val model = dataMap.getString("model", "")
                val battery = dataMap.getInt("battery", -1)
                val companionDetected = dataMap.getBoolean("companion_detected", false)
                
                if (model.isNotEmpty()) {
                    _connectedWatchModel.value = model
                }
                if (battery != -1) {
                    _watchBatteryLevel.value = battery
                }
                _watchCompanionDetected.value = companionDetected
                _isWatchConnected.value = true
                Log.d("WaterViewModel", "Received watch details update: model=$model, battery=$battery%, companion=$companionDetected")
            }
        }
    }

    private fun isWearableUnavailable(e: Throwable?): Boolean {
        if (e == null) return false
        if (e is com.google.android.gms.common.api.ApiException) {
            val sc = e.statusCode
            if (sc == 17 || sc == 16) {
                return true
            }
        }
        val msg = e.message ?: ""
        return msg.startsWith("17") ||
               msg.contains("17:") ||
               msg.contains("API_UNAVAILABLE", ignoreCase = true) ||
               msg.contains("API_NOT_AVAILABLE", ignoreCase = true) ||
               msg.contains("API_NOT_CONNECTED", ignoreCase = true) ||
               msg.contains("API_DISABLED", ignoreCase = true)
    }

    private fun loadInitialWatchDetails() {
        try {
            com.google.android.gms.wearable.Wearable.getDataClient(getApplication())
                .getDataItems(android.net.Uri.parse("wear://*/watch_details"))
                .addOnSuccessListener { dataItems ->
                    try {
                        for (item in dataItems) {
                            if (item.uri.path == "/watch_details") {
                                val dataMap = com.google.android.gms.wearable.DataMapItem.fromDataItem(item).dataMap
                                val model = dataMap.getString("model", "")
                                val battery = dataMap.getInt("battery", -1)
                                val companionDetected = dataMap.getBoolean("companion_detected", false)
                                
                                if (model.isNotEmpty()) {
                                    _connectedWatchModel.value = model
                                }
                                if (battery != -1) {
                                    _watchBatteryLevel.value = battery
                                }
                                _watchCompanionDetected.value = companionDetected
                                _isWatchConnected.value = true
                                Log.d("WaterViewModel", "Loaded initial watch details: model=$model, battery=$battery%, companion=$companionDetected")
                            }
                        }
                    } finally {
                        try {
                            dataItems.release()
                        } catch (e: Exception) {
                            Log.e("WaterViewModel", "Error releasing dataItems: ${e.message}")
                        }
                    }
                }
                .addOnFailureListener { e ->
                    if (isWearableUnavailable(e)) {
                        Log.d("WaterViewModel", "Wearable API not available, skipping initial watch details.")
                    } else {
                        Log.e("WaterViewModel", "Failed to load initial watch details: ${e.message}")
                    }
                }
        } catch (e: Exception) {
            if (isWearableUnavailable(e)) {
                Log.d("WaterViewModel", "Wearable API not available, skipping initial watch details.")
            } else {
                Log.e("WaterViewModel", "Error loading initial watch details: ${e.message}")
            }
        }
    }

    private fun checkConnectedWatchNodes() {
        try {
            com.google.android.gms.wearable.Wearable.getNodeClient(getApplication()).connectedNodes
                .addOnSuccessListener { nodes ->
                    if (nodes.isNotEmpty()) {
                        _isWatchConnected.value = true
                        val firstNode = nodes[0]
                        val name = firstNode.displayName ?: "Smartwatch"
                        if (_connectedWatchModel.value.isEmpty()) {
                            _connectedWatchModel.value = name
                        }
                    } else {
                        _isWatchConnected.value = false
                        _watchCompanionDetected.value = false
                        _watchBatteryLevel.value = null
                        _connectedWatchModel.value = ""
                    }
                }
                .addOnFailureListener { e ->
                    if (isWearableUnavailable(e)) {
                        Log.d("WaterViewModel", "Wearable API not available, skipping watch connection check.")
                    } else {
                        Log.e("WaterViewModel", "Failed to get connected nodes: ${e.message}")
                    }
                }
        } catch (e: Exception) {
            if (isWearableUnavailable(e)) {
                Log.d("WaterViewModel", "Wearable API not available, skipping watch connection check.")
            } else {
                Log.e("WaterViewModel", "Error getting connected nodes: ${e.message}")
            }
        }
    }

    private val _todayWorkoutWaterBonus = MutableStateFlow(0)
    val todayWorkoutWaterBonus: StateFlow<Int> = _todayWorkoutWaterBonus.asStateFlow()

    private val _todayWorkoutAiCoachResponse = MutableStateFlow("")
    val todayWorkoutAiCoachResponse: StateFlow<String> = _todayWorkoutAiCoachResponse.asStateFlow()

    private val _todaySleepWaterBonus = MutableStateFlow(0)
    val todaySleepWaterBonus: StateFlow<Int> = _todaySleepWaterBonus.asStateFlow()

    private val _todaySleepAiCoachResponse = MutableStateFlow("")
    val todaySleepAiCoachResponse: StateFlow<String> = _todaySleepAiCoachResponse.asStateFlow()

    private val _todaySleepHours = MutableStateFlow<Double?>(null)
    val todaySleepHours: StateFlow<Double?> = _todaySleepHours.asStateFlow()

    // Extreme Heat Tracker States
    private val _todayHeatWaterBonus = MutableStateFlow(0)
    val todayHeatWaterBonus: StateFlow<Int> = _todayHeatWaterBonus.asStateFlow()

    private val _todayHeatAiCoachResponse = MutableStateFlow("")
    val todayHeatAiCoachResponse: StateFlow<String> = _todayHeatAiCoachResponse.asStateFlow()

    private val _pendingHeatProposal = MutableStateFlow<HeatProposal?>(null)
    val pendingHeatProposal: StateFlow<HeatProposal?> = _pendingHeatProposal.asStateFlow()

    private val _heatWaterAdjustmentEnabled = MutableStateFlow(prefs.getBoolean("heat_water_adjustment_enabled", true))
    val heatWaterAdjustmentEnabled: StateFlow<Boolean> = _heatWaterAdjustmentEnabled.asStateFlow()

    private val _approxLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val approxLocation: StateFlow<Pair<Double, Double>?> = _approxLocation.asStateFlow()

    private val _locationCity = MutableStateFlow(prefs.getString("weather_city", "") ?: "")
    val locationCity: StateFlow<String> = _locationCity.asStateFlow()

    private val _weatherTemperature = MutableStateFlow(
        if (prefs.contains("weather_temp")) prefs.getFloat("weather_temp", 0f).toDouble() else null
    )
    val weatherTemperature: StateFlow<Double?> = _weatherTemperature.asStateFlow()

    private val _isExtremeHeat = MutableStateFlow(prefs.getBoolean("weather_extreme_heat", false))
    val isExtremeHeat: StateFlow<Boolean> = _isExtremeHeat.asStateFlow()

    private val _weatherRelativeHumidity = MutableStateFlow<Double?>(
        if (prefs.contains("weather_humidity")) prefs.getFloat("weather_humidity", 0f).toDouble() else null
    )
    val weatherRelativeHumidity: StateFlow<Double?> = _weatherRelativeHumidity.asStateFlow()

    private val _weatherApparentTemperature = MutableStateFlow<Double?>(
        if (prefs.contains("weather_apparent_temp")) prefs.getFloat("weather_apparent_temp", 0f).toDouble() else null
    )
    val weatherApparentTemperature: StateFlow<Double?> = _weatherApparentTemperature.asStateFlow()

    val heatDangerLevel: StateFlow<String> = combine(_weatherTemperature, _weatherApparentTemperature) { temp, apparentTemp ->
        val t = apparentTemp ?: temp
        if (t != null) {
            when {
                t >= 51.0 -> "Extreme Danger"
                t >= 39.0 -> "Danger"
                t >= 32.0 -> "Extreme Caution"
                t >= 27.0 -> "Caution"
                else -> "Normal"
            }
        } else {
            "Normal"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Normal")

    private val _isRaining = MutableStateFlow<Boolean>(prefs.getBoolean("weather_is_raining", false))
    val isRaining: StateFlow<Boolean> = _isRaining.asStateFlow()

    private val _weatherUpdateIntervalMinutes = MutableStateFlow<String>(prefs.getString("weather_update_interval", "DEFAULT") ?: "DEFAULT")
    val weatherUpdateIntervalMinutes: StateFlow<String> = _weatherUpdateIntervalMinutes.asStateFlow()

    fun updateWeatherUpdateInterval(interval: String) {
        _weatherUpdateIntervalMinutes.value = interval
        prefs.edit().putString("weather_update_interval", interval).apply()
        triggerButtonHaptic()
    }

    fun toggleRainSimulation() {
        val next = !_isRaining.value
        _isRaining.value = next
        prefs.edit().putBoolean("weather_is_raining", next).apply()
        triggerToggleHaptic()
        refreshWeeklyProgressSummary(forceGemini = false)
    }

    // Proposal overlays/pop-ups
    private val _pendingWorkoutProposal = MutableStateFlow<WorkoutProposal?>(null)
    val pendingWorkoutProposal: StateFlow<WorkoutProposal?> = _pendingWorkoutProposal.asStateFlow()

    private val _pendingSleepProposal = MutableStateFlow<SleepProposal?>(null)
    val pendingSleepProposal: StateFlow<SleepProposal?> = _pendingSleepProposal.asStateFlow()

    val dailyGoalMl: StateFlow<Int> = combine(
        combine(_baseDailyGoalMl, _currentDate, _workoutWaterAdjustmentEnabled) { base, date, workoutEnabled ->
            Triple(base, date, workoutEnabled)
        },
        combine(_todayWorkoutWaterBonus, _sleepWaterAdjustmentEnabled, _todaySleepWaterBonus) { workoutBonus, sleepEnabled, sleepBonus ->
            Triple(workoutBonus, sleepEnabled, sleepBonus)
        },
        combine(_heatWaterAdjustmentEnabled, _todayHeatWaterBonus) { heatEnabled, heatBonus ->
            Pair(heatEnabled, heatBonus)
        }
    ) { part1, part2, part3 ->
        val base = part1.first
        val date = part1.second
        val isWorkoutEnabled = part1.third
        
        val workoutBonus = part2.first
        val isSleepEnabled = part2.second
        val sleepBonus = part2.third

        val isHeatEnabled = part3.first
        val heatBonus = part3.second

        var total = base
        if (date == getCurrentDateString()) {
            if (isWorkoutEnabled) {
                total += workoutBonus
            }
            if (isSleepEnabled) {
                total += sleepBonus
            }
            if (isHeatEnabled) {
                total += heatBonus
            }
        } else {
            var bonus = 0
            if (isWorkoutEnabled) {
                bonus += prefs.getInt("workout_bonus_$date", 0)
            }
            if (isSleepEnabled) {
                bonus += prefs.getInt("sleep_bonus_$date", 0)
            }
            if (isHeatEnabled) {
                bonus += prefs.getInt("heat_bonus_$date", 0)
            }
            total += bonus
        }
        total
    }.stateIn(viewModelScope, SharingStarted.Eagerly, prefs.getInt("daily_goal", 2000))

    fun updateWorkoutWaterAdjustmentEnabled(enabled: Boolean) {
        _workoutWaterAdjustmentEnabled.value = enabled
        prefs.edit().putBoolean("workout_water_adjustment_enabled", enabled).apply()
        if (!enabled) {
            clearTodayWorkoutAdjustment()
        } else {
            detectWorkoutsFromHealthConnect()
        }
    }

    fun updateBypassWorkoutConfirmation(enabled: Boolean) {
        _bypassWorkoutConfirmation.value = enabled
        prefs.edit().putBoolean("bypass_workout_confirmation", enabled).apply()
    }

    fun updateBypassSleepConfirmation(enabled: Boolean) {
        _bypassSleepConfirmation.value = enabled
        prefs.edit().putBoolean("bypass_sleep_confirmation", enabled).apply()
    }

    fun updateShowWaterRemaining(enabled: Boolean) {
        _showWaterRemaining.value = enabled
        prefs.edit().putBoolean("show_water_remaining", enabled).apply()
    }

    fun updateWearShowRemaining(enabled: Boolean) {
        _wearShowRemaining.value = enabled
        prefs.edit().putBoolean("wear_show_remaining", enabled).apply()
        triggerWearOsSync()
    }

    fun updateWearShowTomorrowDay(show: Boolean) {
        _wearShowTomorrowDay.value = show
        prefs.edit().putBoolean("wear_show_tomorrow_day", show).apply()
        triggerWearOsSync()
    }

    fun updateWearSwipeMode(mode: String) {
        _wearSwipeMode.value = mode
        prefs.edit().putString("wear_swipe_mode", mode).apply()
        triggerWearOsSync()
    }

    fun updateWearGraphSwipeDir(dir: String) {
        _wearGraphSwipeDir.value = dir
        prefs.edit().putString("wear_graph_swipe_dir", dir).apply()
        triggerWearOsSync()
    }

    fun updateWearComplicationIconStyle(style: String) {
        _wearComplicationIconStyle.value = style
        prefs.edit().putString("wear_complication_icon_style", style).apply()
        triggerWearOsSync()
    }

    fun updateWearHapticStrength(strength: String) {
        _wearHapticStrength.value = strength
        prefs.edit().putString("wear_haptic_strength", strength).apply()
        triggerWearOsSync()
    }

    fun updateWearThemeColor(color: String) {
        _wearThemeColor.value = color
        prefs.edit().putString("wear_theme_color", color).apply()
        triggerWearOsSync()
    }

    fun updateWearGraphWeeksPrior(weeks: Int) {
        _wearGraphWeeksPrior.value = weeks.coerceIn(1, 8)
        prefs.edit().putInt("wear_graph_weeks_prior", _wearGraphWeeksPrior.value).apply()
        triggerWearOsSync()
    }

    fun updateWearGraphDaysPrior(days: Int) {
        _wearGraphDaysPrior.value = days.coerceIn(1, 30)
        prefs.edit().putInt("wear_graph_days_prior", _wearGraphDaysPrior.value).apply()
        triggerWearOsSync()
    }

    fun updateWearProgressCircleEnabled(enabled: Boolean) {
        _wearProgressCircleEnabled.value = enabled
        prefs.edit().putBoolean("wear_progress_circle_enabled", enabled).apply()
        triggerWearOsSync()
    }

    fun updateWearProgressCircleThickness(thickness: String) {
        _wearProgressCircleThickness.value = thickness
        prefs.edit().putString("wear_progress_circle_thickness", thickness).apply()
        triggerWearOsSync()
    }

    fun updateWearCustomThickness(thickness: Float) {
        _wearCustomThickness.value = thickness
        prefs.edit().putFloat("wear_custom_thickness", thickness).apply()
        triggerWearOsSync()
    }

    fun updateWearGraphPillarThickness(thickness: Float) {
        _wearGraphPillarThickness.value = thickness
        prefs.edit().putFloat("wear_graph_pillar_thickness", thickness).apply()
        triggerWearOsSync()
    }

    fun updateWearGraphHorizontalPadding(padding: Float) {
        _wearGraphHorizontalPadding.value = padding
        prefs.edit().putFloat("wear_graph_horizontal_padding", padding).apply()
        triggerWearOsSync()
    }

    fun updateWearCustomTextSize(textSize: Float) {
        _wearCustomTextSize.value = textSize
        prefs.edit().putFloat("wear_custom_text_size", textSize).apply()
        triggerWearOsSync()
    }

    fun updateWearRamOptimization(enabled: Boolean) {
        _wearRamOptimization.value = enabled
        prefs.edit().putBoolean("wear_ram_optimization", enabled).apply()
        triggerWearOsSync()
    }

    fun updateWearTextColorType(type: String) {
        _wearTextColorType.value = type
        prefs.edit().putString("wear_text_color_type", type).apply()
        triggerWearOsSync()
    }

    fun updateWearTextColorStatic(color: String) {
        _wearTextColorStatic.value = color
        prefs.edit().putString("wear_text_color_static", color).apply()
        triggerWearOsSync()
    }

    fun updateWearCrownRotationEnabled(enabled: Boolean) {
        _wearCrownRotationEnabled.value = enabled
        prefs.edit().putBoolean("wear_crown_rotation_enabled", enabled).apply()
        triggerWearOsSync()
    }

    fun updateWearCrownRotationReverse(enabled: Boolean) {
        _wearCrownRotationReverse.value = enabled
        prefs.edit().putBoolean("wear_crown_rotation_reverse", enabled).apply()
        triggerWearOsSync()
    }

    fun updateWearCrownClicksPerMl(clicks: Int) {
        _wearCrownClicksPerMl.value = clicks
        prefs.edit().putInt("wear_crown_clicks_per_ml", clicks).apply()
        triggerWearOsSync()
    }

    fun updateDevCrownVibratePhone(enabled: Boolean) {
        _devCrownVibratePhone.value = enabled
        prefs.edit().putBoolean("dev_crown_vibrate_phone", enabled).apply()
    }
    
    fun updateOledBackgroundAfterMidnight(enabled: Boolean) {
        _oledBackgroundAfterMidnight.value = enabled
        prefs.edit().putBoolean("oled_background_after_midnight", enabled).apply()
    }
    
    fun updateOledBackgroundTime(hour: Int) {
        _oledBackgroundTime.value = hour
        prefs.edit().putInt("oled_background_time", hour).apply()
    }

    fun updateWearDynamicPaletteRole(role: String) {
        _wearDynamicPaletteRole.value = role
        prefs.edit().putString("wear_dynamic_palette_role", role).apply()
        triggerWearOsSync()
    }

    fun updateWearQuickAddMultiplier(multiplier: Float) {
        _wearQuickAddMultiplier.value = multiplier
        prefs.edit().putFloat("wear_quick_add_multiplier", multiplier).apply()
        triggerWearOsSync()
    }

    fun updateWearCircleSize(size: Int) {
        _wearCircleSize.value = size
        prefs.edit().putInt("wear_circle_size", size).apply()
        triggerWearOsSync()
    }

    fun resetWearCircleSizeToDefault() {
        _wearCircleSize.value = -1
        prefs.edit().remove("wear_circle_size").apply()
        triggerWearOsSync()
    }

    fun updateWearCrownSyncDelay(delayMs: Int) {
        _wearCrownSyncDelay.value = delayMs
        prefs.edit().putInt("wear_crown_sync_delay", delayMs).apply()
        triggerWearOsSync()
    }

    fun updateWearDevOverlayEnabled(enabled: Boolean) {
        _wearDevOverlayEnabled.value = enabled
        prefs.edit().putBoolean("wear_dev_overlay_enabled", enabled).apply()
        triggerWearOsSync()
    }

    fun updateWearDevOverlayR(r: Int) {
        _wearDevOverlayR.value = r
        prefs.edit().putInt("wear_dev_overlay_r", r).apply()
        triggerWearOsSync()
    }

    fun updateWearDevOverlayG(g: Int) {
        _wearDevOverlayG.value = g
        prefs.edit().putInt("wear_dev_overlay_g", g).apply()
        triggerWearOsSync()
    }

    fun updateWearDevOverlayB(b: Int) {
        _wearDevOverlayB.value = b
        prefs.edit().putInt("wear_dev_overlay_b", b).apply()
        triggerWearOsSync()
    }

    fun updateWearLongPressDeleteEnabled(enabled: Boolean) {
        _wearLongPressDeleteEnabled.value = enabled
        prefs.edit().putBoolean("wear_long_press_delete_enabled", enabled).apply()
        triggerWearOsSync()
    }

    fun updateWearInsideCircleRemoved(removed: Boolean) {
        _wearInsideCircleRemoved.value = removed
        prefs.edit().putBoolean("wear_inside_circle_removed", removed).apply()
        triggerWearOsSync()
    }

    fun updateMainLongPressDeleteEnabled(enabled: Boolean) {
        _mainLongPressDeleteEnabled.value = enabled
        prefs.edit().putBoolean("main_long_press_delete_enabled", enabled).apply()
    }

    fun updateMainLongPressDeleteDuration(durationMs: Int) {
        _mainLongPressDeleteDuration.value = durationMs
        prefs.edit().putInt("main_long_press_delete_duration", durationMs).apply()
    }

    fun updateWearPastDaysToShow(days: Int) {
        _wearPastDaysToShow.value = days.coerceIn(1, 7)
        prefs.edit().putInt("wear_past_days_to_show", _wearPastDaysToShow.value).apply()
        triggerWearOsSync()
    }

    fun updateMainCircleTextColorType(type: String) {
        _mainCircleTextColorType.value = type
        prefs.edit().putString("main_circle_text_color_type", type).apply()
    }

    fun updateMainCircleTextColorEnabled(enabled: Boolean) {
        _mainCircleTextColorEnabled.value = enabled
        prefs.edit().putBoolean("main_circle_text_color_enabled", enabled).apply()
    }

    fun updateSeparateSettingsTabEnabled(enabled: Boolean) {
        _separateSettingsTabEnabled.value = enabled
        prefs.edit().putBoolean("separate_settings_tab_enabled", enabled).apply()
    }

    fun updateWearLongPressDeleteDuration(durationMs: Int) {
        _wearLongPressDeleteDuration.value = durationMs
        prefs.edit().putInt("wear_long_press_delete_duration", durationMs).apply()
        triggerWearOsSync()
    }

    fun updateResolvedProgressCircleColor(color: Int) {
        if (_resolvedProgressCircleColor.value != color) {
            _resolvedProgressCircleColor.value = color
            prefs.edit().putInt("resolved_progress_circle_color", color).apply()
            triggerWearOsSync()
        }
    }

    fun deleteMostRecentWaterLog() {
        subtractWaterLogAmountForOffset(0, quickAddAmount.value)
    }

    private fun resolveRoleColorFromSeed(seedColorInt: Int, role: String): Int {
        val hsl = FloatArray(3)
        androidx.core.graphics.ColorUtils.colorToHSL(seedColorInt, hsl)
        val hue = hsl[0]
        val sat = hsl[1]
        return when (role) {
            "SECONDARY" -> {
                val secHue = (hue + 30f) % 360f
                androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(secHue, sat.coerceIn(0.3f, 0.6f), 0.6f))
            }
            "TERTIARY" -> {
                val tertHue = (hue + 120f) % 360f
                androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(tertHue, sat.coerceIn(0.3f, 0.6f), 0.65f))
            }
            else -> { // PRIMARY
                androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(hue, sat.coerceIn(0.5f, 0.85f), 0.65f))
            }
        }
    }

    fun resolveWearThemeColorValue(themeColor: String): Int {
        val role = _wearDynamicPaletteRole.value
        return when (themeColor) {
            "DYNAMIC" -> _resolvedProgressCircleColor.value
            "FOLLOW_APP" -> _resolvedProgressCircleColor.value
            "MINT" -> 0xFF00BFA5.toInt()
            "SKY" -> 0xFF29B6F6.toInt()
            "LAVENDER" -> 0xFFAB47BC.toInt()
            "CORAL" -> 0xFFFF7043.toInt()
            "OCEAN" -> 0xFF1D5AAB.toInt()
            "PURPLE" -> 0xFF8E24AA.toInt()
            "FOREST" -> 0xFF2E7D32.toInt()
            "SLATE" -> 0xFF455A64.toInt()
            "ORANGE" -> 0xFFFF9100.toInt()
            "CRIMSON" -> 0xFFD81B60.toInt()
            "INDIGO" -> 0xFF3F51B5.toInt()
            "ROSE" -> 0xFFFF80AB.toInt()
            "GOLD" -> 0xFFFFD700.toInt()
            "AQUA" -> 0xFF00E5FF.toInt()
            "TEAL" -> 0xFF008080.toInt()
            "PLUM" -> 0xFF4A148C.toInt()
            "PEACH" -> 0xFFFFCC80.toInt()
            "BLACK" -> 0xFF000000.toInt()
            "WHITE" -> 0xFFFFFFFF.toInt()
            else -> {
                if (themeColor.startsWith("PALETTE_")) {
                    val idx = themeColor.removePrefix("PALETTE_").toIntOrNull() ?: 0
                    val list = wallpaperThemeColors.value
                    if (idx in list.indices) {
                        resolveRoleColorFromSeed(list[idx], role)
                    } else {
                        0xFF00BFA5.toInt()
                    }
                } else if (themeColor.startsWith("CUSTOM_RGB_")) {
                    val hex = themeColor.removePrefix("CUSTOM_RGB_")
                    try {
                        android.graphics.Color.parseColor("#$hex")
                    } catch (e: Exception) {
                        0xFF00BFA5.toInt()
                    }
                } else {
                    try {
                        if (themeColor.length == 6 && themeColor.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
                            android.graphics.Color.parseColor("#$themeColor")
                        } else {
                            0xFF00BFA5.toInt() // default to Mint
                        }
                    } catch (e: Exception) {
                        0xFF00BFA5.toInt()
                    }
                }
            }
        }
    }

    fun resolveWearTextColorValue(): Int {
        val type = _wearTextColorType.value
        if (type == "SAME_AS_CIRCLE") {
            return resolveWearThemeColorValue(_wearThemeColor.value)
        } else if (type == "STATIC") {
            return resolveWearThemeColorValue(_wearTextColorStatic.value)
        } else if (type.startsWith("PALETTE_")) {
            val idx = type.removePrefix("PALETTE_").toIntOrNull() ?: 0
            val list = wallpaperThemeColors.value
            if (idx in list.indices) {
                return resolveRoleColorFromSeed(list[idx], _wearDynamicPaletteRole.value)
            }
            return 0xFFFFFFFF.toInt()
        } else if (type == "CUSTOM_RGB") {
            return resolveWearThemeColorValue(_wearTextColorStatic.value)
        }
        return 0xFFFFFFFF.toInt()
    }

    fun triggerWearOsSync() {
        try {
            val themeColor = _wearThemeColor.value
            val resolvedColor = resolveWearThemeColorValue(themeColor)
            val resolvedTextColor = resolveWearTextColorValue()

            prefs.edit()
                .putInt("theme_color_resolved", resolvedColor)
                .putInt("text_color_resolved", resolvedTextColor)
                .apply()
            viewModelScope.launch {
                val days = maxOf(_wearPastDaysToShow.value, _wearGraphDaysPrior.value)
                val intakeM3 = if (days >= 3) (repository.getTotalIntakeForDateSync(getDateStringForSelectedOffset(-3)) ?: 0) else 0
                val intakeM2 = if (days >= 2) (repository.getTotalIntakeForDateSync(getDateStringForSelectedOffset(-2)) ?: 0) else 0
                val intakeM1 = if (days >= 1) (repository.getTotalIntakeForDateSync(getDateStringForSelectedOffset(-1)) ?: 0) else 0
                val intakeP1 = repository.getTotalIntakeForDateSync(getDateStringForSelectedOffset(1)) ?: 0
                val intakeP2 = repository.getTotalIntakeForDateSync(getDateStringForSelectedOffset(2)) ?: 0
                
                val weeks = _wearGraphWeeksPrior.value
                val startOffset = -days
                val intakesList = (startOffset..2).map { offset ->
                    repository.getTotalIntakeForDateSync(getDateStringForSelectedOffset(offset)) ?: 0
                }
                val goalsList = (startOffset..2).map { offset ->
                    getDailyGoalForDate(getDateStringForSelectedOffset(offset))
                }
                val pastIntakesCsv = intakesList.joinToString(",")
                val pastGoalsCsv = goalsList.joinToString(",")
                
                com.pixelwater.app.data.WearDataSyncManager.syncStatus(
                    getApplication(),
                    totalIntakeToday.value,
                    dailyGoalMl.value,
                    quickAddAmount.value,
                    _wearShowRemaining.value,
                    _wearHapticStrength.value,
                    themeColor,
                    _wearCircleSize.value,
                    resolvedColor,
                    _wearLongPressDeleteEnabled.value,
                    _wearLongPressDeleteDuration.value,
                    _wearProgressCircleEnabled.value,
                    resolvedTextColor,
                    _wearCrownRotationEnabled.value,
                    _wearCrownRotationReverse.value,
                    _wearCrownClicksPerMl.value,
                    _wearCrownSyncDelay.value,
                    _wearDevOverlayEnabled.value,
                    _wearDevOverlayR.value,
                    _wearDevOverlayG.value,
                    _wearDevOverlayB.value,
                    _wearInsideCircleRemoved.value,
                    _wearProgressCircleThickness.value,
                    _wearCustomThickness.value,
                    _wearGraphPillarThickness.value,
                    _wearGraphHorizontalPadding.value,
                    _wearCustomTextSize.value,
                    wearShowTomorrowDay = _wearShowTomorrowDay.value,
                    intakeMinus3 = intakeM3,
                    intakeMinus2 = intakeM2,
                    intakeMinus1 = intakeM1,
                    intakePlus1 = intakeP1,
                    intakePlus2 = intakeP2,
                    goalMinus3 = if (days >= 3) getDailyGoalForDate(getDateStringForSelectedOffset(-3)) else 2000,
                    goalMinus2 = if (days >= 2) getDailyGoalForDate(getDateStringForSelectedOffset(-2)) else 2000,
                    goalMinus1 = if (days >= 1) getDailyGoalForDate(getDateStringForSelectedOffset(-1)) else 2000,
                    goalPlus1 = getDailyGoalForDate(getDateStringForSelectedOffset(1)),
                    goalPlus2 = getDailyGoalForDate(getDateStringForSelectedOffset(2)),
                    appLanguage = appLanguage.value,
                    goalLineSquiggly = _goalLineSquiggly.value,
                    pastIntakesCsv = pastIntakesCsv,
                    pastGoalsCsv = pastGoalsCsv,
                    weeksPrior = weeks,
                    daysPrior = days,
                    pastDaysToShow = _wearPastDaysToShow.value,
                    ramOptimizationEnabled = _wearRamOptimization.value,
                    wearSwipeMode = _wearSwipeMode.value,
                    wearGraphSwipeDir = _wearGraphSwipeDir.value,
                    complicationIconStyle = _wearComplicationIconStyle.value
                )
            }
        } catch (e: Exception) {
            if (e.message?.contains("API_UNAVAILABLE") == true || e.message?.contains("17: API:") == true) {
                // Ignore as wear might not be connected
            } else {
                Log.e("WaterViewModel", "Manual Wear OS sync failed: ${e.message}")
            }
        }
    }

    private val _hasCompletedSetup = MutableStateFlow(prefs.getBoolean("has_completed_setup", false))
    val hasCompletedSetup: StateFlow<Boolean> = _hasCompletedSetup.asStateFlow()

    private val _setupWeight = MutableStateFlow(prefs.getFloat("setup_weight", 0f))
    val setupWeight: StateFlow<Float> = _setupWeight.asStateFlow()

    private val _setupHeight = MutableStateFlow(prefs.getFloat("setup_height", 0f))
    val setupHeight: StateFlow<Float> = _setupHeight.asStateFlow()

    private val _creatineEnabled = MutableStateFlow(prefs.getBoolean("creatine_enabled", false))
    val creatineEnabled: StateFlow<Boolean> = _creatineEnabled.asStateFlow()

    private val _creatineOption = MutableStateFlow(prefs.getString("creatine_option", "5G") ?: "5G")
    val creatineOption: StateFlow<String> = _creatineOption.asStateFlow()

    private val _creatineGrams = MutableStateFlow(prefs.getInt("creatine_grams", 5))
    val creatineGrams: StateFlow<Int> = _creatineGrams.asStateFlow()

    private val _showRecentlyAddedBubble = MutableStateFlow(
        prefs.getBoolean("creatine_enabled", false) &&
        prefs.getBoolean("has_completed_setup", false) &&
        !prefs.getBoolean("creatine_upgrade_prompt_shown", false)
    )
    val showRecentlyAddedBubble: StateFlow<Boolean> = _showRecentlyAddedBubble.asStateFlow()

    fun dismissRecentlyAddedBubble() {
        prefs.edit().putBoolean("creatine_upgrade_prompt_shown", true).apply()
        _showRecentlyAddedBubble.value = false
    }

    fun triggerRecentlyAddedBubble() {
        _showRecentlyAddedBubble.value = true
    }

    private val _proteinEnabled = MutableStateFlow(prefs.getBoolean("protein_enabled", false))
    val proteinEnabled: StateFlow<Boolean> = _proteinEnabled.asStateFlow()

    private val _proteinFixed = MutableStateFlow(prefs.getBoolean("protein_fixed", true))
    val proteinFixed: StateFlow<Boolean> = _proteinFixed.asStateFlow()

    private val _proteinMin = MutableStateFlow(prefs.getInt("protein_min", 180))
    val proteinMin: StateFlow<Int> = _proteinMin.asStateFlow()

    private val _proteinMax = MutableStateFlow(prefs.getInt("protein_max", 200))
    val proteinMax: StateFlow<Int> = _proteinMax.asStateFlow()

    private val _remindersEnabled = MutableStateFlow(prefs.getBoolean("reminders_enabled", true))
    val remindersEnabled: StateFlow<Boolean> = _remindersEnabled.asStateFlow()

    private val _remindersDestination = MutableStateFlow(prefs.getString("reminders_destination", "BOTH") ?: "BOTH")
    val remindersDestination: StateFlow<String> = _remindersDestination.asStateFlow()

    private val _reminderInterval = MutableStateFlow(prefs.getInt("reminder_interval", 2))
    val reminderInterval: StateFlow<Int> = _reminderInterval.asStateFlow()

    private val _startHour = MutableStateFlow(prefs.getInt("reminder_start_hour", 8))
    val startHour: StateFlow<Int> = _startHour.asStateFlow()

    private val _endHour = MutableStateFlow(prefs.getInt("reminder_end_hour", 22))
    val endHour: StateFlow<Int> = _endHour.asStateFlow()

    // Haptics customization preferences
    private val _hapticsSlidersEnabled = MutableStateFlow(prefs.getBoolean("haptics_sliders_enabled", true))
    val hapticsSlidersEnabled: StateFlow<Boolean> = _hapticsSlidersEnabled.asStateFlow()

    private val _hapticsButtonsEnabled = MutableStateFlow(prefs.getBoolean("haptics_buttons_enabled", true))
    val hapticsButtonsEnabled: StateFlow<Boolean> = _hapticsButtonsEnabled.asStateFlow()

    private val _hapticsGoalEnabled = MutableStateFlow(prefs.getBoolean("haptics_goal_enabled", true))
    val hapticsGoalEnabled: StateFlow<Boolean> = _hapticsGoalEnabled.asStateFlow()

    private val _hapticsTogglesEnabled = MutableStateFlow(prefs.getBoolean("haptics_toggles_enabled", true))
    val hapticsTogglesEnabled: StateFlow<Boolean> = _hapticsTogglesEnabled.asStateFlow()

    private val _overrideSwipeHapticsEnabled = MutableStateFlow(prefs.getBoolean("override_swipe_haptics_enabled", false))
    val overrideSwipeHapticsEnabled: StateFlow<Boolean> = _overrideSwipeHapticsEnabled.asStateFlow()

    private val _customFireworkHapticsEnabled = MutableStateFlow(prefs.getBoolean("custom_firework_haptics_enabled", false))
    val customFireworkHapticsEnabled: StateFlow<Boolean> = _customFireworkHapticsEnabled.asStateFlow()

    private val _fireworkDurationMs = MutableStateFlow(prefs.getInt("firework_duration_ms", 40))
    val fireworkDurationMs: StateFlow<Int> = _fireworkDurationMs.asStateFlow()

    private val _fireworkGapMs = MutableStateFlow(prefs.getInt("firework_gap_ms", 30))
    val fireworkGapMs: StateFlow<Int> = _fireworkGapMs.asStateFlow()

    private val _fireworkStrength = MutableStateFlow(prefs.getInt("firework_strength", 60))
    val fireworkStrength: StateFlow<Int> = _fireworkStrength.asStateFlow()

    private val _fireworkPatternPreset = MutableStateFlow(
        run {
            val savedName = prefs.getString("firework_pattern_preset", null) ?: VibrationPatternPreset.TRIPLE_PULSE.name
            try {
                VibrationPatternPreset.valueOf(savedName)
            } catch (e: Exception) {
                VibrationPatternPreset.TRIPLE_PULSE
            }
        }
    )
    val fireworkPatternPreset: StateFlow<VibrationPatternPreset> = _fireworkPatternPreset.asStateFlow()


    private fun loadUserManualReminders(): List<String> {
        val jsonStr = prefs.getString("user_manual_reminders", null)
        if (jsonStr.isNullOrEmpty()) return emptyList()
        val list = mutableListOf<String>()
        try {
            val array = org.json.JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun loadAiCoachReminders(): List<String> {
        val jsonStr = prefs.getString("ai_coach_reminders", null)
        if (jsonStr.isNullOrEmpty()) return emptyList()
        val list = mutableListOf<String>()
        try {
            val array = org.json.JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private val _userManualReminders = MutableStateFlow<List<String>>(loadUserManualReminders())
    val userManualReminders: StateFlow<List<String>> = _userManualReminders.asStateFlow()

    private val _aiCoachReminders = MutableStateFlow<List<String>>(loadAiCoachReminders())
    val aiCoachReminders: StateFlow<List<String>> = _aiCoachReminders.asStateFlow()

    private val _notificationSourceMode = MutableStateFlow<String>(prefs.getString("notification_source_mode", "DEFAULT") ?: "DEFAULT")
    val notificationSourceMode: StateFlow<String> = _notificationSourceMode.asStateFlow()

    private val _mixDefaultEnabled = MutableStateFlow<Boolean>(prefs.getBoolean("mix_default_enabled", true))
    val mixDefaultEnabled: StateFlow<Boolean> = _mixDefaultEnabled.asStateFlow()

    private val _mixUserEnabled = MutableStateFlow<Boolean>(prefs.getBoolean("mix_user_enabled", true))
    val mixUserEnabled: StateFlow<Boolean> = _mixUserEnabled.asStateFlow()

    private val _mixAiEnabled = MutableStateFlow<Boolean>(prefs.getBoolean("mix_ai_enabled", true))
    val mixAiEnabled: StateFlow<Boolean> = _mixAiEnabled.asStateFlow()

    fun addUserManualReminder(message: String) {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) return
        val current = _userManualReminders.value.toMutableList()
        if (!current.contains(trimmed)) {
            current.add(trimmed)
            _userManualReminders.value = current
            saveUserManualReminders(current)
        }
    }

    fun removeUserManualReminder(message: String) {
        val current = _userManualReminders.value.toMutableList()
        if (current.remove(message)) {
            _userManualReminders.value = current
            saveUserManualReminders(current)
        }
    }

    private fun saveUserManualReminders(list: List<String>) {
        val array = org.json.JSONArray()
        list.forEach { array.put(it) }
        prefs.edit().putString("user_manual_reminders", array.toString()).apply()
    }

    fun addAiCoachReminder(message: String) {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) return
        val current = _aiCoachReminders.value.toMutableList()
        if (!current.contains(trimmed)) {
            current.add(trimmed)
            _aiCoachReminders.value = current
            saveAiCoachReminders(current)
        }
    }

    fun removeAiCoachReminder(message: String) {
        val current = _aiCoachReminders.value.toMutableList()
        if (current.remove(message)) {
            _aiCoachReminders.value = current
            saveAiCoachReminders(current)
        }
    }

    fun clearAiCoachReminders() {
        _aiCoachReminders.value = emptyList()
        prefs.edit().remove("ai_coach_reminders").apply()
    }

    private fun saveAiCoachReminders(list: List<String>) {
        val array = org.json.JSONArray()
        list.forEach { array.put(it) }
        prefs.edit().putString("ai_coach_reminders", array.toString()).apply()
    }

    fun updateNotificationSourceMode(mode: String) {
        _notificationSourceMode.value = mode
        prefs.edit().putString("notification_source_mode", mode).apply()
        triggerButtonHaptic()
    }

    fun updateMixDefaultEnabled(enabled: Boolean) {
        _mixDefaultEnabled.value = enabled
        prefs.edit().putBoolean("mix_default_enabled", enabled).apply()
        triggerButtonHaptic()
    }

    fun updateMixUserEnabled(enabled: Boolean) {
        _mixUserEnabled.value = enabled
        prefs.edit().putBoolean("mix_user_enabled", enabled).apply()
        triggerButtonHaptic()
    }

    fun updateMixAiEnabled(enabled: Boolean) {
        _mixAiEnabled.value = enabled
        prefs.edit().putBoolean("mix_ai_enabled", enabled).apply()
        triggerButtonHaptic()
    }

    private val _notificationCustomPrompt = MutableStateFlow<String>(prefs.getString("notification_custom_prompt", "") ?: "")
    val notificationCustomPrompt: StateFlow<String> = _notificationCustomPrompt.asStateFlow()

    fun updateNotificationCustomPrompt(prompt: String) {
        _notificationCustomPrompt.value = prompt
        prefs.edit().putString("notification_custom_prompt", prompt).apply()
    }

    private val _fireworksFeedbackEnabled = MutableStateFlow(prefs.getBoolean("fireworks_feedback_enabled", true))
    val fireworksFeedbackEnabled: StateFlow<Boolean> = _fireworksFeedbackEnabled.asStateFlow()

    private val _geminiApiKey = MutableStateFlow(prefs.getString("gemini_api_key", "") ?: "")
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _deepseekApiKey = MutableStateFlow(prefs.getString("deepseek_api_key", "") ?: "")
    val deepseekApiKey: StateFlow<String> = _deepseekApiKey.asStateFlow()

    private val _openaiApiKey = MutableStateFlow(prefs.getString("openai_api_key", "") ?: "")
    val openaiApiKey: StateFlow<String> = _openaiApiKey.asStateFlow()

    private val _quickAddAmount = MutableStateFlow(prefs.getInt("quick_add_amount", 250))
    val quickAddAmount: StateFlow<Int> = _quickAddAmount.asStateFlow()

    private val _tabTransitionMode = MutableStateFlow(prefs.getInt("tab_transition_mode", 1)) // 0: Sliding, 1: Bouncing, 2: Fading
    val tabTransitionMode: StateFlow<Int> = _tabTransitionMode.asStateFlow()

    private val _confettiStyle = MutableStateFlow(prefs.getInt("confetti_style", 0)) // 0: Mixed, 1: Confetti Only, 2: Hearts Only
    val confettiStyle: StateFlow<Int> = _confettiStyle.asStateFlow()

    private val _customMixRectangles = MutableStateFlow(prefs.getBoolean("custom_mix_rect", true))
    val customMixRectangles = _customMixRectangles.asStateFlow()

    private val _customMixHearts = MutableStateFlow(prefs.getBoolean("custom_mix_hearts", true))
    val customMixHearts = _customMixHearts.asStateFlow()

    private val _customMixStars = MutableStateFlow(prefs.getBoolean("custom_mix_stars", true))
    val customMixStars = _customMixStars.asStateFlow()

    private val _customMixDrops = MutableStateFlow(prefs.getBoolean("custom_mix_drops", true))
    val customMixDrops = _customMixDrops.asStateFlow()

    private val _customMixPlaneTreeLeaves = MutableStateFlow(prefs.getBoolean("custom_mix_plane_tree", true))
    val customMixPlaneTreeLeaves = _customMixPlaneTreeLeaves.asStateFlow()

    private val _customMixRegularLeaves = MutableStateFlow(prefs.getBoolean("custom_mix_regular_leaf", true))
    val customMixRegularLeaves = _customMixRegularLeaves.asStateFlow()

    private val _confettiFlowStyle = MutableStateFlow(prefs.getInt("confetti_flow_style", 0)) // 0: Flow & Represent, 1: Sides & Top Burst, 2: Side Burst, 3: Rainfall
    val confettiFlowStyle: StateFlow<Int> = _confettiFlowStyle.asStateFlow()

    private val _particleCount = MutableStateFlow(prefs.getInt("particle_count", 800))
    val particleCount: StateFlow<Int> = _particleCount.asStateFlow()

    private val _isSwipeTabNavEnabled = MutableStateFlow(prefs.getBoolean("is_swipe_tab_nav_enabled", true))
    val isSwipeTabNavEnabled: StateFlow<Boolean> = _isSwipeTabNavEnabled.asStateFlow()

    private val _materialShapesEnabled = MutableStateFlow(prefs.getBoolean("material_shapes_enabled", true))
    val materialShapesEnabled: StateFlow<Boolean> = _materialShapesEnabled.asStateFlow()

    private val _auraGlowEnabled = MutableStateFlow(prefs.getBoolean("aura_glow_enabled", false))
    val auraGlowEnabled: StateFlow<Boolean> = _auraGlowEnabled.asStateFlow()

    private val _shapeRotationEnabled = MutableStateFlow(prefs.getBoolean("shape_rotation_enabled", true))
    val shapeRotationEnabled: StateFlow<Boolean> = _shapeRotationEnabled.asStateFlow()

    private val _materialShapesRotationSpeed = MutableStateFlow(prefs.getFloat("material_shapes_rotation_speed", 1.0f))
    val materialShapesRotationSpeed: StateFlow<Float> = _materialShapesRotationSpeed.asStateFlow()

    private val _auraGlowRotationSpeed = MutableStateFlow(prefs.getFloat("aura_glow_rotation_speed", 1.0f))
    val auraGlowRotationSpeed: StateFlow<Float> = _auraGlowRotationSpeed.asStateFlow()

    private val _shapeMonochromeEnabled = MutableStateFlow(prefs.getBoolean("shape_monochrome_enabled", false))
    val shapeMonochromeEnabled: StateFlow<Boolean> = _shapeMonochromeEnabled.asStateFlow()

    private val _shapeMonochromeSource = MutableStateFlow(prefs.getInt("shape_monochrome_source", 0)) // 0: Primary, 1: Secondary, 2: Tertiary, 3: Custom RGB
    val shapeMonochromeSource: StateFlow<Int> = _shapeMonochromeSource.asStateFlow()

    private val _shapeCustomR = MutableStateFlow(prefs.getFloat("shape_custom_r", 0f))
    val shapeCustomR: StateFlow<Float> = _shapeCustomR.asStateFlow()

    private val _shapeCustomG = MutableStateFlow(prefs.getFloat("shape_custom_g", 191f))
    val shapeCustomG: StateFlow<Float> = _shapeCustomG.asStateFlow()

    private val _shapeCustomB = MutableStateFlow(prefs.getFloat("shape_custom_b", 165f))
    val shapeCustomB: StateFlow<Float> = _shapeCustomB.asStateFlow()

    private val _shapeContrastMode = MutableStateFlow(prefs.getInt("shape_contrast_mode", 1))
    val shapeContrastMode: StateFlow<Int> = _shapeContrastMode.asStateFlow() // 0: Low, 1: Medium, 2: High, 3: Very High, 4: Extreme

    private val _backgroundContrast = MutableStateFlow(prefs.getFloat("background_contrast", 2.30f))
    val backgroundContrast: StateFlow<Float> = _backgroundContrast.asStateFlow()

    private val _configProfile = MutableStateFlow(prefs.getString("config_profile", "PLUG_AND_PLAY") ?: "PLUG_AND_PLAY")
    val configProfile: StateFlow<String> = _configProfile.asStateFlow()

    private val _daySwipeNavigationEnabled = MutableStateFlow(prefs.getBoolean("day_swipe_navigation_enabled", true))
    val daySwipeNavigationEnabled: StateFlow<Boolean> = _daySwipeNavigationEnabled.asStateFlow()

    private val _daySwipeNavigationArrowsVisible = MutableStateFlow(prefs.getBoolean("day_swipe_navigation_arrows_visible", true))
    val daySwipeNavigationArrowsVisible: StateFlow<Boolean> = _daySwipeNavigationArrowsVisible.asStateFlow()

    private val _dayNavBarPadding = MutableStateFlow(prefs.getInt("day_nav_bar_padding", 5))
    val dayNavBarPadding: StateFlow<Int> = _dayNavBarPadding.asStateFlow()

    private val _dayNavBarColorMode = MutableStateFlow(prefs.getString("day_nav_bar_color_mode", "OFF_COLOR") ?: "OFF_COLOR")
    val dayNavBarColorMode: StateFlow<String> = _dayNavBarColorMode.asStateFlow()

    private val _shapeSaturation = MutableStateFlow(prefs.getFloat("shape_saturation", 1.0f))
    val shapeSaturation: StateFlow<Float> = _shapeSaturation.asStateFlow()

    private val _shapeUseIndependentDynamicPalette = MutableStateFlow(prefs.getBoolean("shape_use_independent_dynamic_palette", false))
    val shapeUseIndependentDynamicPalette: StateFlow<Boolean> = _shapeUseIndependentDynamicPalette.asStateFlow()

    private val _shapePaletteShiftEnabled = MutableStateFlow(prefs.getBoolean("shape_palette_shift_enabled", true))
    val shapePaletteShiftEnabled: StateFlow<Boolean> = _shapePaletteShiftEnabled.asStateFlow()

    private val _shapeDynamicPaletteIndex = MutableStateFlow(prefs.getInt("shape_dynamic_palette_index", 0))
    val shapeDynamicPaletteIndex: StateFlow<Int> = _shapeDynamicPaletteIndex.asStateFlow()

    private val _shapeVariabilityEnabled = MutableStateFlow(prefs.getBoolean("shape_variability_enabled", false))
    val shapeVariabilityEnabled: StateFlow<Boolean> = _shapeVariabilityEnabled.asStateFlow()

    private val _shapeVariabilityMode = MutableStateFlow(prefs.getInt("shape_variability_mode", 1))
    val shapeVariabilityMode: StateFlow<Int> = _shapeVariabilityMode.asStateFlow() // 0: Low, 1: Medium, 2: High

    private val _shapeRotMultA = MutableStateFlow(prefs.getFloat("shape_rot_mult_a", 1.0f))
    val shapeRotMultA: StateFlow<Float> = _shapeRotMultA.asStateFlow()

    private val _shapeRotMultB = MutableStateFlow(prefs.getFloat("shape_rot_mult_b", -0.727f))
    val shapeRotMultB: StateFlow<Float> = _shapeRotMultB.asStateFlow()

    private val _shapeRotMultC = MutableStateFlow(prefs.getFloat("shape_rot_mult_c", 0.533f))
    val shapeRotMultC: StateFlow<Float> = _shapeRotMultC.asStateFlow()

    private val _shapeRotMultD = MutableStateFlow(prefs.getFloat("shape_rot_mult_d", -0.615f))
    val shapeRotMultD: StateFlow<Float> = _shapeRotMultD.asStateFlow()

    // Individual Shape Color Overrides
    // Mode: 0 = Primary, 1 = Secondary, 2 = Tertiary, 3 = RGB
    private val _shapeAColorMode = MutableStateFlow(prefs.getInt("shape_a_color_mode", 0))
    val shapeAColorMode: StateFlow<Int> = _shapeAColorMode.asStateFlow()
    private val _shapeAColorR = MutableStateFlow(prefs.getFloat("shape_a_color_r", 255f))
    val shapeAColorR: StateFlow<Float> = _shapeAColorR.asStateFlow()
    private val _shapeAColorG = MutableStateFlow(prefs.getFloat("shape_a_color_g", 0f))
    val shapeAColorG: StateFlow<Float> = _shapeAColorG.asStateFlow()
    private val _shapeAColorB = MutableStateFlow(prefs.getFloat("shape_a_color_b", 0f))
    val shapeAColorB: StateFlow<Float> = _shapeAColorB.asStateFlow()

    private val _shapeBColorMode = MutableStateFlow(prefs.getInt("shape_b_color_mode", 1))
    val shapeBColorMode: StateFlow<Int> = _shapeBColorMode.asStateFlow()
    private val _shapeBColorR = MutableStateFlow(prefs.getFloat("shape_b_color_r", 0f))
    val shapeBColorR: StateFlow<Float> = _shapeBColorR.asStateFlow()
    private val _shapeBColorG = MutableStateFlow(prefs.getFloat("shape_b_color_g", 255f))
    val shapeBColorG: StateFlow<Float> = _shapeBColorG.asStateFlow()
    private val _shapeBColorB = MutableStateFlow(prefs.getFloat("shape_b_color_b", 0f))
    val shapeBColorB: StateFlow<Float> = _shapeBColorB.asStateFlow()

    private val _shapeCColorMode = MutableStateFlow(prefs.getInt("shape_c_color_mode", 2))
    val shapeCColorMode: StateFlow<Int> = _shapeCColorMode.asStateFlow()
    private val _shapeCColorR = MutableStateFlow(prefs.getFloat("shape_c_color_r", 0f))
    val shapeCColorR: StateFlow<Float> = _shapeCColorR.asStateFlow()
    private val _shapeCColorG = MutableStateFlow(prefs.getFloat("shape_c_color_g", 0f))
    val shapeCColorG: StateFlow<Float> = _shapeCColorG.asStateFlow()
    private val _shapeCColorB = MutableStateFlow(prefs.getFloat("shape_c_color_b", 255f))
    val shapeCColorB: StateFlow<Float> = _shapeCColorB.asStateFlow()

    private val _shapeDColorMode = MutableStateFlow(prefs.getInt("shape_d_color_mode", 0))
    val shapeDColorMode: StateFlow<Int> = _shapeDColorMode.asStateFlow()
    private val _shapeDColorR = MutableStateFlow(prefs.getFloat("shape_d_color_r", 255f))
    val shapeDColorR: StateFlow<Float> = _shapeDColorR.asStateFlow()
    private val _shapeDColorG = MutableStateFlow(prefs.getFloat("shape_d_color_g", 255f))
    val shapeDColorG: StateFlow<Float> = _shapeDColorG.asStateFlow()
    private val _shapeDColorB = MutableStateFlow(prefs.getFloat("shape_d_color_b", 255f))
    val shapeDColorB: StateFlow<Float> = _shapeDColorB.asStateFlow()

    private val _progressGlowEnabled = MutableStateFlow(prefs.getBoolean("progress_glow_enabled", true))
    val progressGlowEnabled: StateFlow<Boolean> = _progressGlowEnabled.asStateFlow()

    private val _isProgressCircleCardBgRemoved = MutableStateFlow(prefs.getBoolean("remove_progress_circle_card_bg", true))
    val isProgressCircleCardBgRemoved: StateFlow<Boolean> = _isProgressCircleCardBgRemoved.asStateFlow()

    private val _appLanguage: MutableStateFlow<String> = MutableStateFlow(
        run {
            if (prefs.contains("app_language")) {
                prefs.getString("app_language", "en") ?: "en"
            } else {
                val sysLanguage = java.util.Locale.getDefault().language
                val detected = if (sysLanguage == "el") "el" else "en"
                prefs.edit().putString("app_language", detected).apply()
                detected
            }
        }
    )
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    private val _isFluidOuncesEnabled = MutableStateFlow(prefs.getBoolean("fluid_ounces_enabled", false))
    val isFluidOuncesEnabled: StateFlow<Boolean> = _isFluidOuncesEnabled.asStateFlow()

    fun updateFluidOuncesEnabled(enabled: Boolean) {
        _isFluidOuncesEnabled.value = enabled
        prefs.edit().putBoolean("fluid_ounces_enabled", enabled).apply()
    }

    private val _titleFaceStyle = MutableStateFlow(prefs.getInt("title_face_style", 1))
    val titleFaceStyle: StateFlow<Int> = _titleFaceStyle.asStateFlow()

    private val _titleFacePosition = MutableStateFlow(prefs.getInt("title_face_position", 0))
    val titleFacePosition: StateFlow<Int> = _titleFacePosition.asStateFlow()

    private val _titleFaceVividness = MutableStateFlow(prefs.getInt("title_face_vividness", 5))
    val titleFaceVividness: StateFlow<Int> = _titleFaceVividness.asStateFlow()

    private val _titleFacePaletteRole = MutableStateFlow(prefs.getString("title_face_palette_role", "PRIMARY") ?: "PRIMARY")
    val titleFacePaletteRole: StateFlow<String> = _titleFacePaletteRole.asStateFlow()

    private val _aiProvider = MutableStateFlow(prefs.getString("ai_provider", "Gemini") ?: "Gemini")
    val aiProvider: StateFlow<String> = _aiProvider.asStateFlow()

    private val _geminiModel = MutableStateFlow(
        prefs.getString("gemini_model", "gemini-auto") ?: "gemini-auto"
    )
    val geminiModel: StateFlow<String> = _geminiModel.asStateFlow()

    private val _insightGeminiModel = MutableStateFlow(
        prefs.getString("insight_gemini_model", "gemini-auto") ?: "gemini-auto"
    )
    val insightGeminiModel: StateFlow<String> = _insightGeminiModel.asStateFlow()

    private val _audioGeminiModel = MutableStateFlow(
        prefs.getString("audio_gemini_model", "lyria-3-clip-preview") ?: "lyria-3-clip-preview"
    )
    val audioGeminiModel: StateFlow<String> = _audioGeminiModel.asStateFlow()

    private val _tileQuickAddAmount = MutableStateFlow(prefs.getInt("tile_quick_add_amount", 250))
    val tileQuickAddAmount: StateFlow<Int> = _tileQuickAddAmount.asStateFlow()

    private val _tileDisplayMode = MutableStateFlow(prefs.getString("tile_display_mode", "remaining") ?: "remaining")
    val tileDisplayMode: StateFlow<String> = _tileDisplayMode.asStateFlow()

    private val _tileIconType = MutableStateFlow(prefs.getString("tile_icon_type", "stars") ?: "stars")
    val tileIconType: StateFlow<String> = _tileIconType.asStateFlow()

    private val _tileLongPressAppLaunch = MutableStateFlow(prefs.getBoolean("tile_long_press_app_launch", true))
    val tileLongPressAppLaunch: StateFlow<Boolean> = _tileLongPressAppLaunch.asStateFlow()

    private val _lastUsedCoachModel = MutableStateFlow<String?>(null)
    val lastUsedCoachModel: StateFlow<String?> = _lastUsedCoachModel.asStateFlow()

    private val _lastUsedInsightModel = MutableStateFlow<String?>(null)
    val lastUsedInsightModel: StateFlow<String?> = _lastUsedInsightModel.asStateFlow()

    private val _systemPrompt = MutableStateFlow(prefs.getString("system_prompt", "You are a helpful AI assistant integrated into a hydration tracking app. You help users reach their daily goals with concise, encouraging, and science-backed advice on staying hydrated.") ?: "")
    val systemPrompt: StateFlow<String> = _systemPrompt.asStateFlow()

    private val _textContrastMode = MutableStateFlow(
        if (prefs.contains("text_contrast_mode")) {
            prefs.getInt("text_contrast_mode", 0)
        } else {
            if (prefs.getBoolean("text_contrast_enabled", false)) 2 else 0
        }
    )
    val textContrastMode: StateFlow<Int> = _textContrastMode.asStateFlow()

    private val _fontSizeMode = MutableStateFlow(prefs.getInt("font_size_mode", 0)) // 0: Normal, 1: Medium, 2: Big, 3: Enormous
    val fontSizeMode: StateFlow<Int> = _fontSizeMode.asStateFlow()

    private val _textFontMode = MutableStateFlow(prefs.getInt("text_font_mode", 0)) // 0: Follow Device, 1: Monospace, 2: Serif, 3: Sans-Serif
    val textFontMode: StateFlow<Int> = _textFontMode.asStateFlow()

    private val _colorContrastMode = MutableStateFlow(
        if (prefs.contains("color_contrast_mode")) {
            prefs.getInt("color_contrast_mode", 0)
        } else {
            if (prefs.getBoolean("color_contrast_enabled", false)) 2 else 0
        }
    )
    val colorContrastMode: StateFlow<Int> = _colorContrastMode.asStateFlow()

    private val _vibrationDurationMs = MutableStateFlow(prefs.getInt("vibration_duration_ms", 27))
    val vibrationDurationMs: StateFlow<Int> = _vibrationDurationMs.asStateFlow()

    private val _vibrationGapMs = MutableStateFlow(prefs.getInt("vibration_gap_ms", 10))
    val vibrationGapMs: StateFlow<Int> = _vibrationGapMs.asStateFlow()

    private val _vibrationStrength = MutableStateFlow(prefs.getInt("vibration_strength", 80))
    val vibrationStrength: StateFlow<Int> = _vibrationStrength.asStateFlow()

    private val _animationHapticStrength = MutableStateFlow(prefs.getInt("animation_haptic_strength", 100))
    val animationHapticStrength: StateFlow<Int> = _animationHapticStrength.asStateFlow()

    fun updateAnimationHapticStrength(strength: Int) {
        _animationHapticStrength.value = strength
        prefs.edit().putInt("animation_haptic_strength", strength).apply()
    }

    // --- Advanced Notifications Settings Prefs ---
    private val _notificationDefaultSilent = MutableStateFlow(prefs.getString("notification_default_silent", "DEFAULT") ?: "DEFAULT")
    val notificationDefaultSilent: StateFlow<String> = _notificationDefaultSilent.asStateFlow()

    private val _notificationBannerEnabled = MutableStateFlow(prefs.getBoolean("notification_banner_enabled", false))
    val notificationBannerEnabled: StateFlow<Boolean> = _notificationBannerEnabled.asStateFlow()

    private val _notificationSoundMode = MutableStateFlow(prefs.getString("notification_sound_mode", "device") ?: "device")
    val notificationSoundMode: StateFlow<String> = _notificationSoundMode.asStateFlow()

    private val _notificationVibrationEnabled = MutableStateFlow(prefs.getBoolean("notification_vibration_enabled", true))
    val notificationVibrationEnabled: StateFlow<Boolean> = _notificationVibrationEnabled.asStateFlow()

    private val _notificationBypassDnd = MutableStateFlow(prefs.getBoolean("notification_bypass_dnd", false))
    val notificationBypassDnd: StateFlow<Boolean> = _notificationBypassDnd.asStateFlow()

    private val _notificationDndNoSound = MutableStateFlow(prefs.getBoolean("notification_dnd_no_sound", false))
    val notificationDndNoSound: StateFlow<Boolean> = _notificationDndNoSound.asStateFlow()

    private val _notificationLockScreenMode = MutableStateFlow(prefs.getString("notification_lock_screen_mode", "SHOW_ALL") ?: "SHOW_ALL")
    val notificationLockScreenMode: StateFlow<String> = _notificationLockScreenMode.asStateFlow()

    private val _isGeneratingAiSound = MutableStateFlow(false)
    val isGeneratingAiSound: StateFlow<Boolean> = _isGeneratingAiSound.asStateFlow()

    fun updateNotificationDefaultSilent(mode: String) {
        _notificationDefaultSilent.value = mode
        prefs.edit().putString("notification_default_silent", mode).apply()
        com.pixelwater.app.notifications.NotificationHelper.createNotificationChannel(getApplication())
    }

    fun updateNotificationBannerEnabled(enabled: Boolean) {
        _notificationBannerEnabled.value = enabled
        prefs.edit().putBoolean("notification_banner_enabled", enabled).apply()
        com.pixelwater.app.notifications.NotificationHelper.createNotificationChannel(getApplication())
    }

    fun updateNotificationSoundMode(mode: String) {
        _notificationSoundMode.value = mode
        prefs.edit().putString("notification_sound_mode", mode).apply()
        com.pixelwater.app.notifications.NotificationHelper.createNotificationChannel(getApplication())
    }

    fun updateNotificationVibrationEnabled(enabled: Boolean) {
        _notificationVibrationEnabled.value = enabled
        prefs.edit().putBoolean("notification_vibration_enabled", enabled).apply()
        com.pixelwater.app.notifications.NotificationHelper.createNotificationChannel(getApplication())
    }

    fun updateNotificationBypassDnd(enabled: Boolean) {
        _notificationBypassDnd.value = enabled
        prefs.edit().putBoolean("notification_bypass_dnd", enabled).apply()
        com.pixelwater.app.notifications.NotificationHelper.createNotificationChannel(getApplication())
    }

    fun updateNotificationDndNoSound(enabled: Boolean) {
        _notificationDndNoSound.value = enabled
        prefs.edit().putBoolean("notification_dnd_no_sound", enabled).apply()
        com.pixelwater.app.notifications.NotificationHelper.createNotificationChannel(getApplication())
    }

    fun updateNotificationLockScreenMode(mode: String) {
        _notificationLockScreenMode.value = mode
        prefs.edit().putString("notification_lock_screen_mode", mode).apply()
        com.pixelwater.app.notifications.NotificationHelper.createNotificationChannel(getApplication())
    }

    fun generateAiSoundTrack(
        customInstruction: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isGeneratingAiSound.value = true
            try {
                val apiKey = if (_geminiApiKey.value.isNotBlank()) _geminiApiKey.value.trim() else com.pixelwater.app.BuildConfig.GEMINI_API_KEY.trim()
                if (apiKey == "MY_GEMINI_API_KEY" || apiKey.isBlank()) {
                    throw Exception("Gemini API Key is unconfigured. Please configure your API key under AI Integration Settings.")
                }

                val model = _audioGeminiModel.value
                val isLyria = model.contains("lyria")
                val extraInstruct = if (!customInstruction.isNullOrBlank()) {
                    if (isLyria) {
                        "Generate a very short, realistic, and highly precise music notification sound/jingle matching this description exactly: '$customInstruction'. Ensure it is highly musical, polished, clean, and optimized as an elegant short notification tone. Keep it short (2-5 seconds)."
                    } else {
                        "The user specifically requested a custom notification sound: '$customInstruction'. Create a short, elegant notification sound that matches this description exactly. Maximum duration 3 seconds."
                    }
                } else {
                    if (isLyria) {
                        "Generate a beautiful, short, realistic water-themed musical jingle or chime, perfect for a notification sound. Maximum 3 seconds."
                    } else {
                        "Create a beautiful, elegant, short notification sound. It should be abstract, water-themed like a splash, droplet or a harmonic glass rising arpeggio. Maximum duration 2 seconds."
                    }
                }
                
                val prompt = extraInstruct

                val request = com.pixelwater.app.data.GeminiRequest(
                    contents = listOf(
                        com.pixelwater.app.data.Content(parts = listOf(com.pixelwater.app.data.Part(text = prompt)))
                    ),
                    generationConfig = com.pixelwater.app.data.GenerationConfig(
                        temperature = 0.7f,
                        maxOutputTokens = 300,
                        responseModalities = listOf("AUDIO")
                    )
                )

                val response = safeGenerateContent(
                    model = model,
                    apiKey = apiKey,
                    request = request
                )

                val responsePart = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()
                val audioData = responsePart?.inlineData
                
                if (audioData != null && audioData.mimeType.startsWith("audio/")) {
                    val decodedBytes = android.util.Base64.decode(audioData.data, android.util.Base64.DEFAULT)
                    val file = java.io.File(getApplication<android.app.Application>().filesDir, "ai_notification.wav")
                    java.io.FileOutputStream(file).use { it.write(decodedBytes) }
                    
                    // Re-register channel to bind the new audio file
                    com.pixelwater.app.notifications.NotificationHelper.createNotificationChannel(getApplication())
                } else {
                    throw Exception("AI model did not return valid audio data.")
                }

                _isGeneratingAiSound.value = false
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e("WaterViewModel", "Gemini sound generation failed (error: ${e.message}), falling back to offline procedural synthesis...", e)
                try {
                    val notes = com.pixelwater.app.notifications.SoundGenerator.generateProceduralNotes(customInstruction)
                    com.pixelwater.app.notifications.SoundGenerator.generateAiJingleWav(getApplication(), notes)
                    
                    // Re-register channel to bind the new audio file
                    com.pixelwater.app.notifications.NotificationHelper.createNotificationChannel(getApplication())
                    
                    _isGeneratingAiSound.value = false
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                        onSuccess()
                    }
                } catch (fallbackEx: Exception) {
                    _isGeneratingAiSound.value = false
                    Log.e("WaterViewModel", "Fallback sound generation also failed", fallbackEx)
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                        onError(fallbackEx.message ?: "Failed to generate sound")
                    }
                }
            }
        }
    }

    private val _fakeDeviceBrand = MutableStateFlow(prefs.getString("fake_device_brand", "") ?: "")
    val fakeDeviceBrand: StateFlow<String> = _fakeDeviceBrand.asStateFlow()

    private val _fakeDeviceModel = MutableStateFlow(prefs.getString("fake_device_model", "") ?: "")
    val fakeDeviceModel: StateFlow<String> = _fakeDeviceModel.asStateFlow()

    private val _fakeDeviceEnabled = MutableStateFlow(prefs.getBoolean("fake_device_enabled", false))
    val fakeDeviceEnabled: StateFlow<Boolean> = _fakeDeviceEnabled.asStateFlow()

    fun updateFakeDevice(brand: String, model: String, enabled: Boolean) {
        _fakeDeviceBrand.value = brand
        _fakeDeviceModel.value = model
        _fakeDeviceEnabled.value = enabled
        prefs.edit()
            .putString("fake_device_brand", brand)
            .putString("fake_device_model", model)
            .putBoolean("fake_device_enabled", enabled)
            .apply()
    }

    fun getActiveBrand(): String {
        return if (_fakeDeviceEnabled.value && _fakeDeviceBrand.value.isNotBlank()) {
            _fakeDeviceBrand.value
        } else {
            android.os.Build.MANUFACTURER
        }
    }

    fun getActiveModel(): String {
        return if (_fakeDeviceEnabled.value && _fakeDeviceModel.value.isNotBlank()) {
            _fakeDeviceModel.value
        } else {
            android.os.Build.MODEL
        }
    }

    fun getSavedAiSounds(): List<String> {
        val files = getApplication<android.app.Application>().filesDir.listFiles() ?: return emptyList()
        return files.filter { it.name.startsWith("ai_notification_saved_") && it.name.endsWith(".wav") }
            .map { it.name }
            .sortedByDescending { it }
    }

    fun saveCurrentAiSound(customName: String, onSuccess: () -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val sourceFile = java.io.File(getApplication<android.app.Application>().filesDir, "ai_notification.wav")
                if (!sourceFile.exists()) return@launch
                val safeName = customName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                val timestamp = System.currentTimeMillis()
                val destFile = java.io.File(getApplication<android.app.Application>().filesDir, "ai_notification_saved_\${safeName}_\${timestamp}.wav")
                sourceFile.copyTo(destFile, overwrite = true)
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e("WaterViewModel", "Failed to save AI sound: \${e.message}")
            }
        }
    }

    private val _isGeneratingGimmickTheme = MutableStateFlow(false)
    val isGeneratingGimmickTheme: StateFlow<Boolean> = _isGeneratingGimmickTheme.asStateFlow()

    fun generateGimmickWallpaperAiTheme(
        promptText: String,
        onSuccess: (org.json.JSONObject) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isGeneratingGimmickTheme.value = true
            try {
                val apiKey = if (_geminiApiKey.value.isNotBlank()) _geminiApiKey.value.trim() else com.pixelwater.app.BuildConfig.GEMINI_API_KEY.trim()
                if (apiKey == "MY_GEMINI_API_KEY" || apiKey.isBlank()) {
                    throw Exception("Gemini API Key is unconfigured.")
                }

                val prompt = """
                    You are designing a custom theme for an abstract animated shapes wallpaper.
                    The user requested: "$promptText"
                    
                    Return a JSON object containing the exact configuration for the following keys. No markdown backticks, just the valid JSON object.
                    {
                      "globalColorEnabled": false,
                      "globalR": 0.0, "globalG": 0.0, "globalB": 0.0,
                      "c1R": 0.0, "c1G": 0.0, "c1B": 0.0,
                      "c2R": 0.0, "c2G": 0.0, "c2B": 0.0,
                      "c3R": 0.0, "c3G": 0.0, "c3B": 0.0,
                      "c4R": 0.0, "c4G": 0.0, "c4B": 0.0,
                      "rotA": 1.0, "rotB": -0.727, "rotC": 0.533, "rotD": -0.615,
                      "size1": 1.0, "size2": 1.0, "size3": 1.0, "size4": 1.0,
                      "bgR": 0.0, "bgG": 0.0, "bgB": 0.0
                    }
                    Color ranges are 0.0 to 255.0. Size ranges are 0.2 to 3.0. Rotation ranges are -2.0 to 2.0.
                    c1 = Top Left Shape, c2 = Bottom Right Shape, c3 = Middle Left Shape, c4 = Middle Right Shape.
                    globalColorEnabled handles monochrome modes. If true, set global RGB and ignore individual C1-C4.
                """.trimIndent()

                val request = com.pixelwater.app.data.GeminiRequest(
                    contents = listOf(
                        com.pixelwater.app.data.Content(parts = listOf(com.pixelwater.app.data.Part(text = prompt)))
                    ),
                    generationConfig = com.pixelwater.app.data.GenerationConfig(
                        temperature = 0.7f,
                        maxOutputTokens = 800
                    )
                )

                val response = safeGenerateContent(
                    model = _geminiModel.value,
                    apiKey = apiKey,
                    request = request
                )

                val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                val cleanJson = rawText.replace("```json", "").replace("```", "").trim()
                val jsonObject = org.json.JSONObject(cleanJson)
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onSuccess(jsonObject)
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onError(e.localizedMessage ?: "Unknown error")
                }
            } finally {
                _isGeneratingGimmickTheme.value = false
            }
        }
    }

    fun updateCustomFireworkHapticsEnabled(enabled: Boolean) {
        _customFireworkHapticsEnabled.value = enabled
        prefs.edit().putBoolean("custom_firework_haptics_enabled", enabled).apply()
    }

    fun updateFireworkDurationMs(ms: Int, isFromPreset: Boolean = false) {
        _fireworkDurationMs.value = ms
        if (!isFromPreset) {
            checkAndSetCustomFireworkPreset()
        }
        prefs.edit().putInt("firework_duration_ms", ms).apply()
    }

    fun updateFireworkGapMs(ms: Int, isFromPreset: Boolean = false) {
        _fireworkGapMs.value = ms
        if (!isFromPreset) {
            checkAndSetCustomFireworkPreset()
        }
        prefs.edit().putInt("firework_gap_ms", ms).apply()
    }

    fun updateFireworkStrength(strength: Int, isFromPreset: Boolean = false) {
        _fireworkStrength.value = strength
        if (!isFromPreset) {
            checkAndSetCustomFireworkPreset()
        }
        prefs.edit().putInt("firework_strength", strength).apply()
    }

    private fun checkAndSetCustomFireworkPreset() {
        if (_fireworkPatternPreset.value != VibrationPatternPreset.CUSTOM) {
            _fireworkPatternPreset.value = VibrationPatternPreset.CUSTOM
            prefs.edit()
                .putInt("firework_duration_ms", _fireworkDurationMs.value)
                .putInt("firework_gap_ms", _fireworkGapMs.value)
                .putInt("firework_strength", _fireworkStrength.value)
                .putString("firework_pattern_preset", _fireworkPatternPreset.value.name)
                .apply()
        }
    }

    fun updateFireworkPatternPreset(preset: VibrationPatternPreset) {
        _fireworkPatternPreset.value = preset
        prefs.edit().putString("firework_pattern_preset", preset.name).apply()
        
        when (preset) {
            VibrationPatternPreset.SINGLE_PULSE -> {
                updateFireworkDurationMs(60, true)
                updateFireworkGapMs(0, true)
                updateFireworkStrength(100, true)
            }
            VibrationPatternPreset.DOUBLE_PULSE -> {
                updateFireworkDurationMs(35, true)
                updateFireworkGapMs(45, true)
                updateFireworkStrength(100, true)
            }
            VibrationPatternPreset.TRIPLE_PULSE -> {
                updateFireworkDurationMs(25, true)
                updateFireworkGapMs(35, true)
                updateFireworkStrength(100, true)
            }
            VibrationPatternPreset.HEARTBEAT -> {
                updateFireworkDurationMs(20, true)
                updateFireworkGapMs(30, true)
                updateFireworkStrength(90, true)
            }
            VibrationPatternPreset.VIBRANT_WAVE -> {
                updateFireworkDurationMs(45, true)
                updateFireworkGapMs(15, true)
                updateFireworkStrength(100, true)
            }
            VibrationPatternPreset.CUSTOM -> {
                // Do nothing
            }
        }
    }

    private val _navbarCornerRadius = MutableStateFlow(prefs.getInt("navbar_corner_radius", 32).coerceIn(0, 32))
    val navbarCornerRadius: StateFlow<Int> = _navbarCornerRadius.asStateFlow()

    private val _generalCornerRadius = MutableStateFlow(prefs.getInt("general_corner_radius", 40).coerceIn(0, 40))
    val generalCornerRadius: StateFlow<Int> = _generalCornerRadius.asStateFlow()

    private val _showOutlines = MutableStateFlow(prefs.getBoolean("show_outlines", true))
    val showOutlines: StateFlow<Boolean> = _showOutlines.asStateFlow()

    private val _outlineWidth = MutableStateFlow(prefs.getInt("outline_width", 1))
    val outlineWidth: StateFlow<Int> = _outlineWidth.asStateFlow()

    private val _vibrationPatternPreset = MutableStateFlow(
        run {
            val savedName = prefs.getString("vibration_pattern_preset", null) ?: VibrationPatternPreset.DOUBLE_PULSE.name
            try {
                VibrationPatternPreset.valueOf(savedName)
            } catch (e: Exception) {
                VibrationPatternPreset.DOUBLE_PULSE
            }
        }
    )
    val vibrationPatternPreset: StateFlow<VibrationPatternPreset> = _vibrationPatternPreset.asStateFlow()

    private val _googleAccountEmail = MutableStateFlow(prefs.getString("google_account_email", "") ?: "")
    val googleAccountEmail: StateFlow<String> = _googleAccountEmail.asStateFlow()

    private val _userMbti = MutableStateFlow(prefs.getString("user_mbti", "") ?: "")
    val userMbti: StateFlow<String> = _userMbti.asStateFlow()

    fun updateUserMbti(mbti: String) {
        _userMbti.value = mbti.uppercase().trim()
        prefs.edit().putString("user_mbti", _userMbti.value).apply()
        // Refresh summary to immediately see MBTI tailored output
        refreshWeeklyProgressSummary()
    }

    private val _userBigFiveO = MutableStateFlow(prefs.getInt("user_bigfive_o", -1))
    val userBigFiveO: StateFlow<Int> = _userBigFiveO.asStateFlow()

    private val _userBigFiveC = MutableStateFlow(prefs.getInt("user_bigfive_c", -1))
    val userBigFiveC: StateFlow<Int> = _userBigFiveC.asStateFlow()

    private val _userBigFiveE = MutableStateFlow(prefs.getInt("user_bigfive_e", -1))
    val userBigFiveE: StateFlow<Int> = _userBigFiveE.asStateFlow()

    private val _userBigFiveA = MutableStateFlow(prefs.getInt("user_bigfive_a", -1))
    val userBigFiveA: StateFlow<Int> = _userBigFiveA.asStateFlow()

    private val _userBigFiveN = MutableStateFlow(prefs.getInt("user_bigfive_n", -1))
    val userBigFiveN: StateFlow<Int> = _userBigFiveN.asStateFlow()

    fun updateBigFive(o: Int, c: Int, e: Int, a: Int, n: Int) {
        _userBigFiveO.value = o
        _userBigFiveC.value = c
        _userBigFiveE.value = e
        _userBigFiveA.value = a
        _userBigFiveN.value = n
        prefs.edit()
            .putInt("user_bigfive_o", o)
            .putInt("user_bigfive_c", c)
            .putInt("user_bigfive_e", e)
            .putInt("user_bigfive_a", a)
            .putInt("user_bigfive_n", n)
            .apply()
        refreshWeeklyProgressSummary()
    }

    private val _googleAccountName = MutableStateFlow(prefs.getString("google_account_name", "") ?: "")
    val googleAccountName: StateFlow<String> = _googleAccountName.asStateFlow()

    private val _isGoogleLoggedIn = MutableStateFlow(prefs.getBoolean("is_google_logged_in", false))
    val isGoogleLoggedIn: StateFlow<Boolean> = _isGoogleLoggedIn.asStateFlow()

    private val _isGoogleDriveBackupEnabled = MutableStateFlow(prefs.getBoolean("google_drive_backup_enabled", true))
    val isGoogleDriveBackupEnabled: StateFlow<Boolean> = _isGoogleDriveBackupEnabled.asStateFlow()

    private val _autoLocalBackupEnabled = MutableStateFlow(prefs.getBoolean("auto_local_backup_enabled", false))
    val autoLocalBackupEnabled: StateFlow<Boolean> = _autoLocalBackupEnabled.asStateFlow()

    private val _autoGoogleBackupEnabled = MutableStateFlow(prefs.getBoolean("auto_google_backup_enabled", false))
    val autoGoogleBackupEnabled: StateFlow<Boolean> = _autoGoogleBackupEnabled.asStateFlow()

    fun updateAutoLocalBackupEnabled(enabled: Boolean) {
        _autoLocalBackupEnabled.value = enabled
        prefs.edit().putBoolean("auto_local_backup_enabled", enabled).apply()
        triggerButtonHaptic()
        if (enabled) {
            triggerAutoBackup()
        }
    }

    fun updateAutoGoogleBackupEnabled(enabled: Boolean) {
        _autoGoogleBackupEnabled.value = enabled
        prefs.edit().putBoolean("auto_google_backup_enabled", enabled).apply()
        triggerButtonHaptic()
        if (enabled) {
            triggerAutoBackup()
        }
    }

    // App User Data Limitation & Storage Management
    data class UserDataStats(
        val totalBytes: Long = 0L,
        val databaseBytes: Long = 0L,
        val filesAndBackupsBytes: Long = 0L,
        val preferencesBytes: Long = 0L,
        val formattedTotal: String = "0 B",
        val formattedDatabase: String = "0 B",
        val formattedFilesAndBackups: String = "0 B",
        val formattedPreferences: String = "0 B"
    )

    private val _appUserDataLimitMb = MutableStateFlow(
        prefs.getInt("app_user_data_limit_mb", prefs.getInt("app_cache_limit_mb", 1024))
    )
    val appUserDataLimitMb: StateFlow<Int> = _appUserDataLimitMb.asStateFlow()

    private val _currentUserDataStats = MutableStateFlow(UserDataStats())
    val currentUserDataStats: StateFlow<UserDataStats> = _currentUserDataStats.asStateFlow()

    fun updateAppUserDataLimitMb(limitMb: Int) {
        val clamped = limitMb.coerceIn(10, 5120)
        _appUserDataLimitMb.value = clamped
        prefs.edit().putInt("app_user_data_limit_mb", clamped).apply()
        triggerSliderHaptic()
    }

    fun refreshCurrentUserDataSize() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val stats = calculateUserDataStats()
            _currentUserDataStats.value = stats
            _currentCacheSizeBytes.value = stats.totalBytes
        }
    }

    fun optimizeUserDataStorage(onComplete: (freedBytes: Long, message: String) -> Unit = { _, _ -> }) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val beforeStats = calculateUserDataStats()

                // Safe SQLite Compaction: Checkpoint WAL & compact pages
                // STRICT GUARANTEE: Water logs, day streaks, graphs, and settings are 100% preserved.
                try {
                    database.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL)")
                    database.openHelper.writableDatabase.execSQL("VACUUM")
                } catch (e: Exception) {
                    Log.e("WaterViewModel", "Error running database checkpoint/vacuum", e)
                }

                // Clean temporary cache files in cache directories
                val context = getApplication<Application>()
                val cacheDirs = listOfNotNull(context.cacheDir, context.externalCacheDir)
                for (dir in cacheDirs) {
                    deleteDirectoryContents(dir)
                }

                val afterStats = calculateUserDataStats()
                _currentUserDataStats.value = afterStats
                _currentCacheSizeBytes.value = afterStats.totalBytes
                val freed = (beforeStats.totalBytes - afterStats.totalBytes).coerceAtLeast(0L)

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    triggerButtonHaptic()
                    val freedStr = formatBytesToDisplay(freed)
                    val msg = if (appLanguage.value == "el") {
                        "Βελτιστοποιήθηκε! Απελευθερώθηκαν $freedStr χώρου. Τα δεδομένα & σερί σας διατηρήθηκαν ακέραια."
                    } else {
                        "Optimized! Reclaimed $freedStr. Your logs and streaks were kept intact."
                    }
                    onComplete(freed, msg)
                }
            } catch (e: Exception) {
                Log.e("WaterViewModel", "Error optimizing user data storage", e)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete(0L, "Optimization completed.")
                }
            }
        }
    }

    /**
     * Actively deletes gigabytes of historical user data, old backups (.json), temporary audio clips,
     * and caches, strictly preserving only the active goal streak.
     */
    fun deleteUserDataAndMaintainStreak(onComplete: (freedBytes: Long, message: String) -> Unit = { _, _ -> }) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val beforeStats = calculateUserDataStats()
                val context = getApplication<Application>()

                // 1. Capture and preserve current goal streak
                val currentStreak = _streak.value
                val persistedStreak = prefs.getInt("persisted_goal_streak", currentStreak)
                val streakToMaintain = maxOf(currentStreak, persistedStreak)
                prefs.edit().putInt("persisted_goal_streak", streakToMaintain).apply()

                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val todayStr = getCurrentDateString()
                val todayGoal = getDailyGoalForDate(todayStr)

                // Streak dates to preserve
                val streakDatesToKeep = mutableSetOf<String>()
                streakDatesToKeep.add(todayStr)

                val allCurrentLogs = repository.getAllLogs().first()
                val dailyTotals = allCurrentLogs.groupBy { it.dateString }
                    .mapValues { entry -> entry.value.sumOf { it.waterEquivalentMl } }

                val todayTotal = dailyTotals[todayStr] ?: 0
                val streakCal = java.util.Calendar.getInstance()
                if (todayTotal >= todayGoal) {
                    streakCal.add(java.util.Calendar.DAY_OF_YEAR, -1)
                } else {
                    streakCal.add(java.util.Calendar.DAY_OF_YEAR, -1)
                }

                for (i in 0 until streakToMaintain) {
                    streakDatesToKeep.add(sdf.format(streakCal.time))
                    streakCal.add(java.util.Calendar.DAY_OF_YEAR, -1)
                }

                // 2. Ensure each kept streak day has a compact single log row meeting the goal
                streakDatesToKeep.forEach { dateKey ->
                    val dayLogs = allCurrentLogs.filter { it.dateString == dateKey }
                    val dayGoal = getDailyGoalForDate(dateKey)
                    val dayIntake = dayLogs.sumOf { it.waterEquivalentMl }
                    prefs.edit().putInt("goal_history_$dateKey", dayGoal).apply()

                    if (dateKey != todayStr && dayLogs.size > 1) {
                        repository.deleteLogsForDate(dateKey)
                        val repTimestamp = dayLogs.firstOrNull()?.timestamp ?: System.currentTimeMillis()
                        val consolidatedAmount = maxOf(dayIntake, dayGoal)
                        repository.insertLog(
                            com.pixelwater.app.data.WaterLog(
                                amountMl = consolidatedAmount,
                                timestamp = repTimestamp,
                                dateString = dateKey,
                                beverageType = "Water",
                                waterEquivalency = 1.0f,
                                waterEquivalentMl = consolidatedAmount,
                                sourceDevice = null
                            )
                        )
                    }
                }

                // 3. Delete ALL logs outside active streak
                val logsToDelete = allCurrentLogs.filterNot { it.dateString in streakDatesToKeep }
                val deletedLogsCount = logsToDelete.size
                if (streakDatesToKeep.isNotEmpty()) {
                    repository.deleteLogsNotInDates(streakDatesToKeep.toList())
                }

                // 4. Actively delete gigabytes of files across all backup and storage directories
                var deletedFilesCount = 0
                val candidateDirs = mutableListOf<java.io.File>()
                try {
                    val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                    candidateDirs.add(java.io.File(downloadDir, "PixelWaterBackup"))
                } catch (e: Exception) {}
                try {
                    val extDir = android.os.Environment.getExternalStorageDirectory()
                    candidateDirs.add(java.io.File(extDir, "PixelWaterBackup"))
                } catch (e: Exception) {}
                try {
                    context.getExternalFilesDir(null)?.let {
                        candidateDirs.add(java.io.File(it, "PixelWaterBackup"))
                        candidateDirs.add(it)
                    }
                } catch (e: Exception) {}
                try {
                    context.getExternalFilesDirs(null)?.filterNotNull()?.forEach {
                        candidateDirs.add(java.io.File(it, "PixelWaterBackup"))
                        candidateDirs.add(it)
                    }
                } catch (e: Exception) {}
                try {
                    candidateDirs.add(getBackupDirectory())
                } catch (e: Exception) {}

                candidateDirs.distinctBy { it.absolutePath }.forEach { bDir ->
                    try {
                        if (bDir.exists()) {
                            bDir.listFiles()?.forEach { f ->
                                val name = f.name.lowercase()
                                if (f.isFile && (name.endsWith(".json") || name.endsWith(".bak") || name.endsWith(".zip") || name.endsWith(".csv") || name.endsWith(".tmp") || name.startsWith("backup"))) {
                                    if (f.delete()) deletedFilesCount++
                                } else if (f.isDirectory && f.name == "PixelWaterBackup") {
                                    deleteDirectoryContents(f)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("WaterViewModel", "Error cleaning backup dir", e)
                    }
                }

                // 4b. Clean internal files directory
                try {
                    context.filesDir?.listFiles()?.forEach { f ->
                        if (f.isFile) {
                            val name = f.name.lowercase()
                            if (name.startsWith("ai_notification_saved") || name.endsWith(".json") || name.endsWith(".tmp") || name.endsWith(".bak")) {
                                if (f.delete()) deletedFilesCount++
                            }
                        }
                    }
                } catch (e: Exception) {}

                // 4c. Clean noBackupFilesDir
                try {
                    context.noBackupFilesDir?.let { deleteDirectoryContents(it) }
                } catch (e: Exception) {}

                // 4d. Clean cache directories
                try {
                    val cacheDirs = listOfNotNull(
                        context.cacheDir,
                        context.externalCacheDir,
                        context.codeCacheDir
                    )
                    for (dir in cacheDirs) {
                        deleteDirectoryContents(dir)
                    }
                    context.externalCacheDirs?.filterNotNull()?.forEach {
                        deleteDirectoryContents(it)
                    }
                } catch (e: Exception) {}

                // 4e. SQLite WAL truncate checkpoint and VACUUM
                try {
                    database.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(TRUNCATE)")
                    database.openHelper.writableDatabase.execSQL("VACUUM")
                    database.openHelper.writableDatabase.execSQL("PRAGMA shrink_memory")
                } catch (e: Exception) {
                    Log.e("WaterViewModel", "Database vacuum error", e)
                }

                // 5. Recalculate streak & preserve it
                calculateStreak()
                updateHomeScreenWidget()
                triggerWearOsSync()

                // 6. Recalculate storage stats
                val afterStats = calculateUserDataStats()
                _currentUserDataStats.value = afterStats
                _currentCacheSizeBytes.value = afterStats.totalBytes
                val freed = (beforeStats.totalBytes - afterStats.totalBytes).coerceAtLeast(0L)

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    triggerButtonHaptic()
                    val freedStr = formatBytesToDisplay(freed)
                    val streakText = if (appLanguage.value == "el") "$streakToMaintain ημερών" else "$streakToMaintain days"
                    val msg = if (appLanguage.value == "el") {
                        "Εκκαθαρίστηκαν επιτυχώς $deletedLogsCount καταγραφές και $deletedFilesCount αρχεία/αντίγραφα. Απελευθερώθηκαν $freedStr! Το σερί στόχου ($streakText) διατηρήθηκε άθικτο."
                    } else {
                        "Successfully deleted $deletedLogsCount logs and $deletedFilesCount backup files. Freed $freedStr! Your goal streak ($streakText) is preserved."
                    }
                    onComplete(freed, msg)
                }
            } catch (e: Exception) {
                Log.e("WaterViewModel", "Error deleting user data maintaining streak", e)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete(0L, if (appLanguage.value == "el") "Η διαγραφή απέτυχε." else "Deletion failed.")
                }
            }
        }
    }

    fun purgeUserDataOlderThanMonth(onComplete: (freedBytes: Long, message: String) -> Unit = { _, _ -> }) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val beforeStats = calculateUserDataStats()

                // Cutoff date is 30 days ago (1 month)
                val cal = java.util.Calendar.getInstance().apply {
                    add(java.util.Calendar.DAY_OF_YEAR, -30)
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                val cutoffTimestamp = cal.timeInMillis
                val cutoffDateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.time)

                // Retrieve all logs
                val currentLogs = if (allLogs.value.isNotEmpty()) {
                    allLogs.value
                } else {
                    repository.getAllLogs().first()
                }

                // Filter logs older than 30 days
                val olderLogs = currentLogs.filter { it.dateString < cutoffDateStr || it.timestamp < cutoffTimestamp }
                var deletedGranularCount = 0

                if (olderLogs.isNotEmpty()) {
                    val olderByDate = olderLogs.groupBy { it.dateString }
                    olderByDate.forEach { (dateStr, dayLogs) ->
                        val dayTotalAmount = dayLogs.sumOf { it.amountMl }
                        val dayTotalEquivalent = dayLogs.sumOf { it.waterEquivalentMl }
                        val repTimestamp = dayLogs.firstOrNull()?.timestamp ?: cal.timeInMillis

                        // Save the goal to preferences to preserve it
                        val pastGoal = getDailyGoalForDate(dateStr)
                        prefs.edit().putInt("goal_history_$dateStr", pastGoal).apply()

                        // Delete all granular logs for this past date
                        repository.deleteLogsForDate(dateStr)

                        // Insert 1 consolidated summary entry
                        if (dayTotalAmount > 0) {
                            val consolidatedLog = com.pixelwater.app.data.WaterLog(
                                amountMl = dayTotalAmount,
                                timestamp = repTimestamp,
                                dateString = dateStr,
                                beverageType = "Daily Consolidated",
                                waterEquivalency = 1.0f,
                                waterEquivalentMl = dayTotalEquivalent,
                                sourceDevice = null
                            )
                            repository.insertLog(consolidatedLog)
                        }
                        deletedGranularCount += dayLogs.size
                    }
                }

                // Clean backup files older than 30 days
                var deletedBackupsCount = 0
                try {
                    val backupDir = getBackupDirectory()
                    if (backupDir.exists()) {
                        backupDir.listFiles()?.forEach { f ->
                            if (f.isFile && (f.lastModified() < cutoffTimestamp || f.name.contains(cutoffDateStr))) {
                                if (f.delete()) {
                                    deletedBackupsCount++
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WaterViewModel", "Error deleting old backup files", e)
                }

                // Clean temporary cache files in cache directories
                val context = getApplication<Application>()
                val cacheDirs = listOfNotNull(context.cacheDir, context.externalCacheDir)
                for (dir in cacheDirs) {
                    deleteDirectoryContents(dir)
                }

                // Safely checkpoint SQLite WAL to commit freed space without transaction collisions
                try {
                    database.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(PASSIVE)")
                    database.openHelper.writableDatabase.execSQL("VACUUM")
                } catch (e: Exception) {
                    Log.e("WaterViewModel", "Error checkpointing database after purge", e)
                }

                // Recalculate streak and update widgets
                calculateStreak()
                updateHomeScreenWidget()
                triggerWearOsSync()

                val afterStats = calculateUserDataStats()
                _currentUserDataStats.value = afterStats
                _currentCacheSizeBytes.value = afterStats.totalBytes
                val freed = (beforeStats.totalBytes - afterStats.totalBytes).coerceAtLeast(0L)
                val totalDeletedItems = deletedGranularCount + deletedBackupsCount

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    triggerButtonHaptic()
                    val freedStr = formatBytesToDisplay(freed)
                    val msg = if (appLanguage.value == "el") {
                        if (totalDeletedItems > 0) {
                            "Εκκαθαρίστηκαν $totalDeletedItems στοιχεία (> 30 ημερών). Απελευθερώθηκαν $freedStr. Το σερί, οι στήλες και οι στόχοι διατηρήθηκαν!"
                        } else {
                            "Δεν βρέθηκαν δεδομένα άνω των 30 ημερών. Όλες οι καταγραφές και τα αντίγραφα είναι εντός των τελευταίων 30 ημερών!"
                        }
                    } else {
                        if (totalDeletedItems > 0) {
                            "Purged $totalDeletedItems items older than 30 days. Freed $freedStr. Streaks, graph pillars, and daily goals preserved!"
                        } else {
                            "No data older than 30 days found. All your records and backups are within the last 30 days!"
                        }
                    }
                    onComplete(freed, msg)
                }
            } catch (e: Exception) {
                Log.e("WaterViewModel", "Error purging user data older than month", e)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete(0L, if (appLanguage.value == "el") "Η διαγραφή απέτυχε." else "Deletion failed.")
                }
            }
        }
    }

    fun deleteUserDataRecent30Days(onComplete: (freedBytes: Long, message: String) -> Unit = { _, _ -> }) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val beforeStats = calculateUserDataStats()

                val cal = java.util.Calendar.getInstance().apply {
                    add(java.util.Calendar.DAY_OF_YEAR, -30)
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                val cutoffTimestamp = cal.timeInMillis
                val cutoffDateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.time)

                val deletedLogsCount = repository.deleteLogsRecent30Days(cutoffDateStr, cutoffTimestamp)

                val context = getApplication<Application>()
                val cacheDirs = listOfNotNull(context.cacheDir, context.externalCacheDir)
                for (dir in cacheDirs) {
                    deleteDirectoryContents(dir)
                }

                try {
                    database.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(PASSIVE)")
                    database.openHelper.writableDatabase.execSQL("VACUUM")
                } catch (e: Exception) {
                    Log.e("WaterViewModel", "Error checkpointing database", e)
                }

                calculateStreak()
                updateHomeScreenWidget()
                triggerWearOsSync()

                val afterStats = calculateUserDataStats()
                _currentUserDataStats.value = afterStats
                _currentCacheSizeBytes.value = afterStats.totalBytes
                val freed = (beforeStats.totalBytes - afterStats.totalBytes).coerceAtLeast(0L)

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    triggerButtonHaptic()
                    val freedStr = formatBytesToDisplay(freed)
                    val msg = if (appLanguage.value == "el") {
                        if (deletedLogsCount > 0) {
                            "Διαγράφηκαν επιτυχώς $deletedLogsCount καταγραφές των τελευταίων 30 ημερών. Απελευθερώθηκαν $freedStr!"
                        } else {
                            "Δεν βρέθηκαν καταγραφές των τελευταίων 30 ημερών."
                        }
                    } else {
                        if (deletedLogsCount > 0) {
                            "Successfully deleted $deletedLogsCount logs from the past 30 days. Freed $freedStr!"
                        } else {
                            "No logs from the past 30 days found."
                        }
                    }
                    onComplete(freed, msg)
                }
            } catch (e: Exception) {
                Log.e("WaterViewModel", "Error deleting recent logs", e)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete(0L, if (appLanguage.value == "el") "Η διαγραφή απέτυχε." else "Deletion failed.")
                }
            }
        }
    }

    fun clearAllHydrationData(onComplete: (freedBytes: Long, message: String) -> Unit = { _, _ -> }) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val beforeStats = calculateUserDataStats()

                repository.deleteAllLogs()

                val context = getApplication<Application>()
                val cacheDirs = listOfNotNull(context.cacheDir, context.externalCacheDir)
                for (dir in cacheDirs) {
                    deleteDirectoryContents(dir)
                }

                try {
                    database.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(PASSIVE)")
                    database.openHelper.writableDatabase.execSQL("VACUUM")
                } catch (e: Exception) {
                    Log.e("WaterViewModel", "Error checkpointing database", e)
                }

                calculateStreak()
                updateHomeScreenWidget()
                triggerWearOsSync()

                val afterStats = calculateUserDataStats()
                _currentUserDataStats.value = afterStats
                _currentCacheSizeBytes.value = afterStats.totalBytes
                val freed = (beforeStats.totalBytes - afterStats.totalBytes).coerceAtLeast(0L)

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    triggerButtonHaptic()
                    val freedStr = formatBytesToDisplay(freed)
                    val msg = if (appLanguage.value == "el") {
                        "Όλες οι καταγραφές νερού διαγράφηκαν επιτυχώς! Απελευθερώθηκαν $freedStr."
                    } else {
                        "All hydration logs were deleted successfully! Freed $freedStr."
                    }
                    onComplete(freed, msg)
                }
            } catch (e: Exception) {
                Log.e("WaterViewModel", "Error clearing all hydration data", e)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete(0L, if (appLanguage.value == "el") "Η διαγραφή απέτυχε." else "Deletion failed.")
                }
            }
        }
    }

    private fun calculateUserDataStats(): UserDataStats {
        val context = getApplication<Application>()
        var databaseBytes = 0L
        var filesBytes = 0L
        var prefsBytes = 0L
        var otherInternalDataBytes = 0L
        var externalFilesBytes = 0L
        var backupsFolderBytes = 0L

        try {
            // 1. Room SQLite Database directory
            val dbFile = context.getDatabasePath("water_tracker_database")
            dbFile.parentFile?.let { dbDir ->
                if (dbDir.exists() && dbDir.isDirectory) {
                    dbDir.listFiles()?.forEach { f ->
                        databaseBytes += if (f.isDirectory) getDirectorySize(f) else f.length()
                    }
                }
            }

            // 2. Internal files directory
            context.filesDir?.let { fDir ->
                if (fDir.exists()) {
                    filesBytes += getDirectorySize(fDir)
                }
            }

            // 3. No backup files directory
            context.noBackupFilesDir?.let { nbDir ->
                if (nbDir.exists()) {
                    filesBytes += getDirectorySize(nbDir)
                }
            }

            // 4. SharedPreferences directory and other internal data folders
            val dataDir = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                context.dataDir
            } else {
                context.filesDir?.parentFile
            }
            dataDir?.let { root ->
                val spDir = java.io.File(root, "shared_prefs")
                if (spDir.exists() && spDir.isDirectory) {
                    prefsBytes += getDirectorySize(spDir)
                }
                root.listFiles()?.forEach { sub ->
                    val name = sub.name.lowercase()
                    // Exclude transient cache directories (cache, code_cache)
                    if (name != "cache" && name != "code_cache" && name != "databases" && name != "files" && name != "no_backup" && name != "shared_prefs") {
                        otherInternalDataBytes += if (sub.isDirectory) getDirectorySize(sub) else sub.length()
                    }
                }
            }

            // 5. External files directory (app-specific)
            try {
                context.getExternalFilesDirs(null)?.filterNotNull()?.forEach { extDir ->
                    if (extDir.exists()) {
                        externalFilesBytes += getDirectorySize(extDir)
                    }
                }
            } catch (e: Exception) {
                context.getExternalFilesDir(null)?.let { extDir ->
                    if (extDir.exists()) {
                        externalFilesBytes += getDirectorySize(extDir)
                    }
                }
            }

            // 6. User device backups directory (scan public Download, external root & app backups)
            try {
                val candidateDirs = mutableListOf<java.io.File>()
                try {
                    val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                    candidateDirs.add(java.io.File(downloadDir, "PixelWaterBackup"))
                } catch (e: Exception) {}
                try {
                    val extDir = android.os.Environment.getExternalStorageDirectory()
                    candidateDirs.add(java.io.File(extDir, "PixelWaterBackup"))
                } catch (e: Exception) {}
                try {
                    candidateDirs.add(getBackupDirectory())
                } catch (e: Exception) {}

                candidateDirs.distinctBy { it.absolutePath }.forEach { bDir ->
                    if (bDir.exists()) {
                        backupsFolderBytes += getDirectorySize(bDir)
                    }
                }
            } catch (e: Exception) {
                // Ignore if storage access issue
            }
        } catch (e: Exception) {
            Log.e("WaterViewModel", "Error calculating user data size", e)
        }

        val totalScanned = databaseBytes + filesBytes + prefsBytes + otherInternalDataBytes + externalFilesBytes + backupsFolderBytes
        val finalTotal = totalScanned

        val totalFilesAndBackups = filesBytes + otherInternalDataBytes + externalFilesBytes + backupsFolderBytes

        return UserDataStats(
            totalBytes = finalTotal,
            databaseBytes = databaseBytes,
            filesAndBackupsBytes = totalFilesAndBackups,
            preferencesBytes = prefsBytes,
            formattedTotal = formatBytesToDisplay(finalTotal),
            formattedDatabase = formatBytesToDisplay(databaseBytes),
            formattedFilesAndBackups = formatBytesToDisplay(totalFilesAndBackups),
            formattedPreferences = formatBytesToDisplay(prefsBytes)
        )
    }

    // Backward-compatibility aliases for existing calls
    val appCacheLimitMb: StateFlow<Int> = _appUserDataLimitMb.asStateFlow()
    private val _currentCacheSizeBytes = MutableStateFlow(0L)
    val currentCacheSizeBytes: StateFlow<Long> = _currentCacheSizeBytes.asStateFlow()
    fun updateAppCacheLimitMb(limitMb: Int) = updateAppUserDataLimitMb(limitMb)
    fun refreshCurrentCacheSize() = refreshCurrentUserDataSize()
    fun clearAppCache(onComplete: (freedBytes: Long) -> Unit = {}) {
        optimizeUserDataStorage { freed, _ -> onComplete(freed) }
    }

    private fun getDirectorySize(dir: java.io.File): Long {
        if (!dir.exists()) return 0L
        if (dir.isFile) return dir.length()
        var size = 0L
        dir.listFiles()?.forEach { child ->
            size += if (child.isDirectory) getDirectorySize(child) else child.length()
        }
        return size
    }

    private fun collectCacheFiles(dir: java.io.File, list: MutableList<java.io.File>) {
        if (!dir.exists()) return
        dir.listFiles()?.forEach { file ->
            if (file.isFile) {
                list.add(file)
            } else if (file.isDirectory) {
                collectCacheFiles(file, list)
            }
        }
    }

    private fun deleteDirectoryContents(dir: java.io.File) {
        if (!dir.exists()) return
        dir.listFiles()?.forEach { child ->
            if (child.isDirectory) {
                deleteDirectoryContents(child)
                child.delete()
            } else {
                child.delete()
            }
        }
    }

    fun formatBytesToDisplay(bytes: Long): String {
        return when {
            bytes >= 1024L * 1024L * 1024L -> String.format(java.util.Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
            bytes >= 1024L * 1024L -> String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024L -> String.format(java.util.Locale.US, "%.0f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    private val _wearUpdateStatus = MutableStateFlow<WearUpdateStatus>(WearUpdateStatus.Idle)
    val wearUpdateStatus: StateFlow<WearUpdateStatus> = _wearUpdateStatus.asStateFlow()

    // Logs for selected/current date
    @OptIn(ExperimentalCoroutinesApi::class)
    val todayLogs: StateFlow<List<WaterLog>> = _currentDate
        .flatMapLatest { date -> repository.getLogsForDate(date).map { logs -> logs.sortedByDescending { it.timestamp } } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Aggregated total intake for the day
    @OptIn(ExperimentalCoroutinesApi::class)
    val totalIntakeToday: StateFlow<Int> = _currentDate
        .flatMapLatest { date -> repository.getTotalIntakeForDate(date) }
        .map { (it ?: 0).coerceAtLeast(0) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalIntakeMinus3: Flow<Int> = _currentDate
        .flatMapLatest { date -> repository.getTotalIntakeForDate(getDateStringForSelectedOffset(-3)) }
        .map { (it ?: 0).coerceAtLeast(0) }

    val totalIntakeMinus2: Flow<Int> = _currentDate
        .flatMapLatest { date -> repository.getTotalIntakeForDate(getDateStringForSelectedOffset(-2)) }
        .map { (it ?: 0).coerceAtLeast(0) }

    val totalIntakeMinus1: Flow<Int> = _currentDate
        .flatMapLatest { date -> repository.getTotalIntakeForDate(getDateStringForSelectedOffset(-1)) }
        .map { (it ?: 0).coerceAtLeast(0) }

    val totalIntakePlus1: Flow<Int> = _currentDate
        .flatMapLatest { date -> repository.getTotalIntakeForDate(getDateStringForSelectedOffset(1)) }
        .map { (it ?: 0).coerceAtLeast(0) }

    val totalIntakePlus2: Flow<Int> = _currentDate
        .flatMapLatest { date -> repository.getTotalIntakeForDate(getDateStringForSelectedOffset(2)) }
        .map { (it ?: 0).coerceAtLeast(0) }

    // All logs for historical visualization
    val allLogs: StateFlow<List<WaterLog>> = repository.getAllLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dynamic overhydration calculation based on scientific renal clearance mechanics (~15 ml/min) and timestamped intake logs
    val rapidOverhydrationState: StateFlow<RapidOverhydrationState> = kotlinx.coroutines.flow.combine(
        allLogs,
        kotlinx.coroutines.flow.flow {
            while (true) {
                emit(System.currentTimeMillis())
                kotlinx.coroutines.delay(1000L) // ticker every second for precise real-time countdown
            }
        }
    ) { logs, currentTime ->
        val oneHourAgo = currentTime - 3600 * 1000L
        val recentLogs = logs.filter { it.timestamp >= oneHourAgo }.sortedBy { it.timestamp }
        val recentIntake = recentLogs.sumOf { it.waterEquivalentMl }
        if (recentIntake <= 1000) {
            RapidOverhydrationState(
                isOverhydrated = false,
                recent60MinIntakeMl = recentIntake,
                excessVolumeMl = 0,
                renalClearanceRateMlMin = 14.5,
                estimatedClearanceMinutes = 0,
                safeDrinkTimestampMillis = null,
                remainingSeconds = 0L
            )
        } else {
            val excessMl = recentIntake - 1000
            
            // SCIENTIFIC RENAL CLEARANCE MODEL FOR ACUTE OVERHYDRATION:
            // Maximal renal free water excretion rate under vasopressin suppression is ~14.5 mL/min (~870 mL/hr).
            // When an acute fluid volume V (e.g. 1750 mL) is ingested within 60 minutes, total renal excretion
            // requires T_clearance = Total_Intake / Clearance_Rate (e.g. 1750 / 14.5 = ~121 minutes) from the onset
            // of heavy intake to safely eliminate fluid load and prevent acute hyponatremia.
            val firstLogTimestamp = recentLogs.firstOrNull()?.timestamp ?: (currentTime - 3600 * 1000L)
            val totalClearanceMs = (recentIntake / 14.5 * 60_000.0).toLong()
            val targetSafeTimestamp = maxOf(currentTime + 60_000L, firstLogTimestamp + totalClearanceMs)
            
            val remainingSec = maxOf(0L, (targetSafeTimestamp - currentTime) / 1000L)
            val remainingMinutes = kotlin.math.ceil(remainingSec / 60.0).toInt()

            RapidOverhydrationState(
                isOverhydrated = true,
                recent60MinIntakeMl = recentIntake,
                excessVolumeMl = excessMl,
                renalClearanceRateMlMin = 14.5,
                estimatedClearanceMinutes = remainingMinutes,
                safeDrinkTimestampMillis = targetSafeTimestamp,
                remainingSeconds = remainingSec
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RapidOverhydrationState())

    val rapidIntakeWarning: StateFlow<Boolean> = rapidOverhydrationState
        .map { it.isOverhydrated }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val nextSafeDrinkTimeMillis: StateFlow<Long?> = rapidOverhydrationState
        .map { it.safeDrinkTimestampMillis }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Streak count
    private val _streak = MutableStateFlow(0)
    val streak: StateFlow<Int> = _streak.asStateFlow()

    // Streak Milestone Celebration
    private val _activeStreakMilestoneToCelebrate = MutableStateFlow<Int?>(null)
    val activeStreakMilestoneToCelebrate: StateFlow<Int?> = _activeStreakMilestoneToCelebrate.asStateFlow()

    private val _streakCelebrationMessage = MutableStateFlow<String>("")
    val streakCelebrationMessage: StateFlow<String> = _streakCelebrationMessage.asStateFlow()

    private val _isStreakCelebrationMessageLoading = MutableStateFlow<Boolean>(false)
    val isStreakCelebrationMessageLoading: StateFlow<Boolean> = _isStreakCelebrationMessageLoading.asStateFlow()

    fun dismissStreakCelebration() {
        _activeStreakMilestoneToCelebrate.value = null
        _streakCelebrationMessage.value = ""
    }

    fun triggerStreakMilestoneCelebration(streakValue: Int) {
        _activeStreakMilestoneToCelebrate.value = streakValue
        _streakCelebrationMessage.value = ""
        _isStreakCelebrationMessageLoading.value = true
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val apiKey = if (_geminiApiKey.value.isNotBlank()) _geminiApiKey.value.trim() else com.pixelwater.app.BuildConfig.GEMINI_API_KEY.trim()
                if (apiKey == "MY_GEMINI_API_KEY" || apiKey.isBlank()) {
                    throw Exception("Gemini API Key is unconfigured.")
                }

                // Get personality adaptation
                val mbtiTypeFull = _userMbti.value.uppercase().trim()
                val baseMbti = mbtiTypeFull.take(4)
                val mbtiTone = when {
                    baseMbti in listOf("INTJ", "INTP", "ENTJ", "ENTP") -> {
                        "Style: Clinical, intellectual, analytical, and highly structured with precise metrics. Focus strictly on biological optimization, efficiency ratios, chemical/physiological processes, and precise scientific reasoning suited for a rational $baseMbti. Avoid generic emotional cheerleading."
                    }
                    baseMbti in listOf("INFJ", "INFP", "ENFJ", "ENFP") -> {
                        "Style: Deeply personal, highly warm, exceptionally empathetic, holistic, and encouraging. Focus on self-love, mindfulness, emotional well-being, active somatic listening, and aligning hydration with mental state suited for a diplomatic $baseMbti."
                    }
                    baseMbti in listOf("ISTJ", "ISFJ", "ESTJ", "ESFJ") -> {
                        "Style: Structured, practical, standard habit-oriented, highly organized, and duty-focused. Focus strictly on steady schedule compliance, milestone completion percentages, exact target numbers, and building solid daily habits suited for a sentinel $baseMbti."
                    }
                    baseMbti in listOf("ISTP", "ISFP", "ESTP", "ESFP") -> {
                        "Style: Full of high energy, active, highly snappy, playful, and fun. Use punchy phrasing, lighthearted humor, active verbs, and immediate casual prompts suited for an explorer $baseMbti."
                    }
                    else -> "Style: Energetic, encouraging, fun and motivational coach style."
                }

                val systemPrompt = """
                    You are "Pixel Water AI", the user's hydration motivation coach.
                    The user has reached a major streak milestone: $streakValue days of continuous hydration compliance!
                    This is an outstanding achievement! Please reward bigger numbers (like 20, 30, 50, etc.) with extra wow factor, calling them unstoppable or legendary.
                    
                    Write a short, highly engaging, and personalized congratulatory message for this milestone. 
                    - It must be relatively short (around 2 to 4 sentences).
                    - It must take into account the user's set coach personality/style:
                      $mbtiTone
                    - Highlight the number $streakValue days as a proof of their dedication.
                    - If the user language is Greek ('el'), write it in Greek, otherwise write in English.
                    - IMPORTANT FOR GREEK ('el'): Do NOT use awkward phrases like "αγαπητέ φίλε- φίλη", "αγαπητέ/ή φίλε/η", "αγαπητέ μου φίλε", or gender-split words. Write natural, direct, gender-neutral Greek using standard motivational verbs and expressions (e.g., "Συγχαρητήρια!", "Μπράβο!", "Είσαι απίστευτος/η!"). Keep the phrasing natural and direct for any gender.
                    - Ensure you do NOT mention personality terms like "MBTI", "Big Five", or personality codes in your text. Just adopt the tone.
                    - Make it incredibly rewarding, supportive and memorable!
                """.trimIndent()

                val prompt = "Create a congratulations message for a $streakValue days hydration streak in ${if (appLanguage.value == "el") "Greek" else "English"} language."

                val request = com.pixelwater.app.data.GeminiRequest(
                    contents = listOf(
                        com.pixelwater.app.data.Content(
                            parts = listOf(com.pixelwater.app.data.Part(text = prompt))
                        )
                    ),
                    systemInstruction = com.pixelwater.app.data.Content(
                        parts = listOf(com.pixelwater.app.data.Part(text = systemPrompt))
                    ),
                    generationConfig = com.pixelwater.app.data.GenerationConfig(
                        temperature = 0.85f,
                        maxOutputTokens = 300
                    )
                )

                val response = safeGenerateContent(
                    model = "gemini-3.5-flash",
                    apiKey = apiKey,
                    request = request
                )

                val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!rawText.isNullOrBlank()) {
                    _streakCelebrationMessage.value = cleanMetaCommentsAndInstructions(rawText)
                } else {
                    _streakCelebrationMessage.value = if (appLanguage.value == "el") 
                        "Είσαι απίστευτος! $streakValue ημέρες συνεχόμενου σερί! Συνέχισε έτσι τη φανταστική δουλειά! 🔥" 
                        else "You are unstoppable! $streakValue days of continuous hydration streak! Keep up the legendary work! 🔥"
                }
            } catch (e: Exception) {
                Log.e("WaterViewModel", "Failed to generate streak milestone message: ${e.message}")
                _streakCelebrationMessage.value = if (appLanguage.value == "el") 
                    "Συγχαρητήρια! Φτάσατε τις $streakValue ημέρες σερί! Είμαστε εξαιρετικά περήφανοι για την αφοσίωσή σας! 🔥💧" 
                    else "Incredible achievement! You have reached a $streakValue days streak! We are extremely proud of your dedication! 🔥💧"
            } finally {
                _isStreakCelebrationMessageLoading.value = false
            }
        }
    }

    fun testTriggerStreakMilestone(milestone: Int) {
        triggerStreakMilestoneCelebration(milestone)
    }

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (key == "widget_selected_date") {
            val newDateSelection = sharedPreferences.getString("widget_selected_date", null)
            if (newDateSelection != null && newDateSelection != _currentDate.value) {
                _currentDate.value = newDateSelection
            }
        } else if (key == "daily_goal") {
            val newGoal = sharedPreferences.getInt("daily_goal", 2000)
            if (newGoal != _baseDailyGoalMl.value) {
                _baseDailyGoalMl.value = newGoal
                calculateStreak()
            }
        }
    }

    private val _customDrinks = MutableStateFlow<List<CustomDrink>>(emptyList())
    val customDrinks: StateFlow<List<CustomDrink>> = _customDrinks.asStateFlow()

    private val _customThemes = MutableStateFlow<List<CustomTheme>>(emptyList())
    val customThemes: StateFlow<List<CustomTheme>> = _customThemes.asStateFlow()

    private val _showInsightGreeting = MutableStateFlow(prefs.getBoolean("show_insight_greeting", true))
    val showInsightGreeting: StateFlow<Boolean> = _showInsightGreeting.asStateFlow()

    private val _aiWeeklyProgressSummaryRaw = MutableStateFlow(prefs.getString("ai_weekly_progress_summary_raw", "") ?: prefs.getString("ai_weekly_progress_summary", "") ?: "")
    
    fun cleanMetaCommentsAndInstructions(text: String): String {
        val lines = text.split("\n")
        val cleanedLines = mutableListOf<String>()
        
        val metaWords = listOf(
            "length", "presentation", "style", "emojis", "tone", "mbti", "big five", "ocean",
            "openness", "conscientiousness", "extraversion", "agreeableness", "neuroticism",
            "myers-briggs", "assertive", "turbulent", "oscillating", "harmony", "introversion",
            "introverted", "intuitive", "infj", "intj", "infp", "enfj", "enfp", "entj", "entp",
            "istj", "isfj", "estj", "esfj", "istp", "isfp", "estp", "esfp", "fits", "suited",
            "sentence", "perfectly", "cozy", "empathetic", "adaptation", "personality", "traits"
        )
        
        for (line in lines) {
            var trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue
            
            // Remove bullet points/stars
            while (trimmedLine.startsWith("*") || trimmedLine.startsWith("-") || trimmedLine.startsWith("•") || trimmedLine.startsWith("#")) {
                trimmedLine = trimmedLine.substring(1).trim()
            }
            
            val lowerLine = trimmedLine.lowercase()
            
            val isWholeLineMeta = lowerLine.startsWith("total:") ||
                    lowerLine.startsWith("sentence count") ||
                    lowerLine.startsWith("length &") ||
                    lowerLine.startsWith("style:") ||
                    lowerLine.startsWith("tone:") ||
                    lowerLine.startsWith("emojis:") ||
                    lowerLine.startsWith("mbti") ||
                    lowerLine.startsWith("big five") ||
                    lowerLine.startsWith("note:") ||
                    lowerLine.startsWith("constraint") ||
                    (lowerLine.contains("fits ") && lowerLine.contains("perfectly")) ||
                    (lowerLine.contains("suited for") && lowerLine.contains("personality")) ||
                    (lowerLine.contains("total:") && lowerLine.contains("sentence"))
                    
            if (isWholeLineMeta) {
                continue
            }
            
            val sentencePrefixRegex = Regex("^(?i)(sentence|paragraph|πρόταση|παράγραφος)\\s*\\d*\\s*[:\\- ]\\s*")
            var lineContent = sentencePrefixRegex.replace(trimmedLine, "").trim()
            
            if (lineContent.isEmpty()) continue
            
            // Clean up parenthetical metadata annotations
            val parenRegex = Regex("\\(([^)]+)\\)")
            lineContent = parenRegex.replace(lineContent) { matchResult ->
                val innerContent = matchResult.groupValues[1].lowercase()
                val isMeta = metaWords.any { innerContent.contains(it) }
                if (isMeta) "" else matchResult.value
            }.trim()
            
            lineContent = lineContent.replace(Regex("\\s+"), " ")
            
            // Strip leading/trailing ellipses or punctuation remaining after removal
            while (lineContent.startsWith("...") || lineContent.startsWith("…")) {
                lineContent = lineContent.substring(if (lineContent.startsWith("...")) 3 else 1).trim()
            }
            while (lineContent.endsWith("...") || lineContent.endsWith("…")) {
                lineContent = lineContent.substring(0, lineContent.length - (if (lineContent.endsWith("...")) 3 else 1)).trim()
            }
            
            if (lineContent.endsWith("()") || lineContent.endsWith("[]")) {
                lineContent = lineContent.substring(0, lineContent.length - 2).trim()
            }
            
            // If the line is empty or just punctuation now, skip it
            if (lineContent.isEmpty() || lineContent == "." || lineContent == ",") {
                continue
            }
            
            cleanedLines.add(lineContent)
        }
        
        var resultText = cleanedLines.joinToString("\n").trim()
        if (resultText.endsWith("* *")) {
            resultText = resultText.substring(0, resultText.length - 3).trim()
        }
        if (resultText.endsWith("*")) {
            resultText = resultText.substring(0, resultText.length - 1).trim()
        }
        
        return resultText
    }

    fun getFormattedInsight(rawSummary: String, showGreeting: Boolean, lang: String): String {
        if (rawSummary.isBlank()) return ""
        val cleanedRaw = cleanMetaCommentsAndInstructions(rawSummary)
        if (cleanedRaw.isBlank()) return ""
        val isGreek = lang == "el"
        val possibleGreetings = listOf(
            "Good morning", "Good afternoon", "Good evening", "Goodnight", "Sweet dreams", "Wishing you",
            "looks like", "be sure take", "Pitter-patter", "The clouds", "Dancing in", "Water is",
            "Καλημέρα", "Καλό μεσημέρι", "Καλό απόγευμα", "Καλό βράδυ", "Καληνύχτα", "Όνειρα", "Σου εύχομαι",
            "Φαίνεται", "Σιγουρέψου", "Άκου την", "Τα σύννεφα", "Ο χορός", "Το νερό"
        )
        val hasOriginalGreeting = possibleGreetings.any { cleanedRaw.trim().startsWith(it) }

        val greetingPrefix = if (showGreeting && !hasOriginalGreeting) {
            val calendar = java.util.Calendar.getInstance(getUserTimeZone())
            getContextualGreeting(calendar, lang)
        } else ""

        val actualTextToSanitize = if (!showGreeting) {
            if (hasOriginalGreeting && cleanedRaw.contains("\n\n")) {
                cleanedRaw.substringAfter("\n\n").trim()
            } else {
                cleanedRaw.trim()
            }
        } else {
            cleanedRaw.trim()
        }

        val sanitized = sanitizeIncompleteSentences(actualTextToSanitize)
        return greetingPrefix + sanitized
    }

    val aiWeeklyProgressSummary: StateFlow<String> = kotlinx.coroutines.flow.combine(
        _aiWeeklyProgressSummaryRaw,
        _showInsightGreeting,
        _showInsightFaces,
        appLanguage
    ) { raw, showGreeting, showFaces, lang ->
        getFormattedInsight(raw, showGreeting, lang)
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), "")
    
    private val _isGeneratingWeeklySummary = MutableStateFlow(false)
    val isGeneratingWeeklySummary: StateFlow<Boolean> = _isGeneratingWeeklySummary.asStateFlow()

    init {
        // Synchronize application locales with the saved/detected language on startup
        updateAppLanguage(_appLanguage.value)

        // Enforce user preferred data limit and calculate initial accurate user data usage
        refreshCurrentUserDataSize()

        // Sync tile long press app launch setting to PackageManager component state on startup
        try {
            val enabled = prefs.getBoolean("tile_long_press_app_launch", true)
            val componentName = android.content.ComponentName(
                application,
                "com.pixelwater.app.service.WaterQuickAddTileSettingsActivity"
            )
            val newState = if (enabled) {
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            application.packageManager.setComponentEnabledSetting(
                componentName,
                newState,
                android.content.pm.PackageManager.DONT_KILL_APP
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Initialize Wear OS sync listener and reactive states broadcaster
        try {
            com.pixelwater.app.data.WearDataSyncManager.registerMessageListener(
                application,
                onAddWater = { amount, offset ->
                    viewModelScope.launch {
                        val name = _connectedWatchModel.value.trim()
                        val watchName = if (name.isNotEmpty()) {
                            val isPixelWatch = name.lowercase().contains("pixel watch")
                            if (isPixelWatch) "Google Pixel Watch" else name
                        } else {
                            "Wear OS Smartwatch"
                        }
                        if (offset == 0) {
                            addWaterLog(amount, sourceDevice = watchName)
                        } else {
                            val targetDateStr = getDateStringForSelectedOffset(offset)
                            addWaterLogForDate(amount, targetDateStr, sourceDevice = watchName)
                        }
                    }
                },
                onDeleteWater = { offset ->
                    subtractWaterLogAmountForOffset(offset, quickAddAmount.value)
                }
            )
            viewModelScope.launch {
                class WearSyncPayload(
                    val intake: Int,
                    val goal: Int,
                    val quickAdd: Int,
                    val showRem: Boolean,
                    val haptic: String,
                    val theme: String,
                    val cSize: Int,
                    val resolvedColor: Int,
                    val longPressEnabled: Boolean,
                    val longPressDuration: Int,
                    val insideCircleRemoved: Boolean,
                    val thickness: String,
                    val intakeMinus3: Int,
                    val intakeMinus2: Int,
                    val intakeMinus1: Int,
                    val intakePlus1: Int,
                    val intakePlus2: Int
                )
                kotlinx.coroutines.flow.combine(
                    totalIntakeToday,
                    dailyGoalMl,
                    quickAddAmount,
                    wearShowRemaining,
                    wearHapticStrength,
                    wearThemeColor,
                    wearCircleSize,
                    resolvedProgressCircleColor,
                    wearLongPressDeleteEnabled,
                    wearLongPressDeleteDuration,
                    wearInsideCircleRemoved,
                    wearProgressCircleThickness,
                    totalIntakeMinus3,
                    totalIntakeMinus2,
                    totalIntakeMinus1,
                    totalIntakePlus1,
                    totalIntakePlus2
                ) { flows ->
                    val themeColor = flows[5] as String
                    val resolvedColor = resolveWearThemeColorValue(themeColor)
                    WearSyncPayload(
                        intake = flows[0] as Int,
                        goal = flows[1] as Int,
                        quickAdd = flows[2] as Int,
                        showRem = flows[3] as Boolean,
                        haptic = flows[4] as String,
                        theme = themeColor,
                        cSize = flows[6] as Int,
                        resolvedColor = resolvedColor,
                        longPressEnabled = flows[8] as Boolean,
                        longPressDuration = flows[9] as Int,
                        insideCircleRemoved = flows[10] as Boolean,
                        thickness = flows[11] as String,
                        intakeMinus3 = flows[12] as Int,
                        intakeMinus2 = flows[13] as Int,
                        intakeMinus1 = flows[14] as Int,
                        intakePlus1 = flows[15] as Int,
                        intakePlus2 = flows[16] as Int
                    )
                }.collect { payload ->
                    val weeks = _wearGraphWeeksPrior.value
                    val days = maxOf(_wearPastDaysToShow.value, _wearGraphDaysPrior.value)
                    val startOffset = -days
                    val intakesList = (startOffset..2).map { offset ->
                        repository.getTotalIntakeForDateSync(getDateStringForSelectedOffset(offset)) ?: 0
                    }
                    val goalsList = (startOffset..2).map { offset ->
                        getDailyGoalForDate(getDateStringForSelectedOffset(offset))
                    }
                    val pastIntakesCsv = intakesList.joinToString(",")
                    val pastGoalsCsv = goalsList.joinToString(",")

                    com.pixelwater.app.data.WearDataSyncManager.syncStatus(
                        application,
                        payload.intake,
                        payload.goal,
                        payload.quickAdd,
                        payload.showRem,
                        payload.haptic,
                        payload.theme,
                        payload.cSize,
                        payload.resolvedColor,
                        payload.longPressEnabled,
                        payload.longPressDuration,
                        _wearProgressCircleEnabled.value,
                        resolveWearTextColorValue(),
                        _wearCrownRotationEnabled.value,
                        _wearCrownRotationReverse.value,
                        _wearCrownClicksPerMl.value,
                        _wearCrownSyncDelay.value,
                        _wearDevOverlayEnabled.value,
                        _wearDevOverlayR.value,
                        _wearDevOverlayG.value,
                        _wearDevOverlayB.value,
                        payload.insideCircleRemoved,
                        payload.thickness,
                        _wearCustomThickness.value,
                        _wearGraphPillarThickness.value,
                        _wearGraphHorizontalPadding.value,
                        _wearCustomTextSize.value,
                        intakeMinus3 = if (days >= 3) payload.intakeMinus3 else 0,
                        intakeMinus2 = if (days >= 2) payload.intakeMinus2 else 0,
                        intakeMinus1 = if (days >= 1) payload.intakeMinus1 else 0,
                        intakePlus1 = payload.intakePlus1,
                        intakePlus2 = repository.getTotalIntakeForDateSync(getDateStringForSelectedOffset(2)) ?: 0,
                        goalMinus3 = if (days >= 3) getDailyGoalForDate(getDateStringForSelectedOffset(-3)) else 2000,
                        goalMinus2 = if (days >= 2) getDailyGoalForDate(getDateStringForSelectedOffset(-2)) else 2000,
                        goalMinus1 = if (days >= 1) getDailyGoalForDate(getDateStringForSelectedOffset(-1)) else 2000,
                        goalPlus1 = getDailyGoalForDate(getDateStringForSelectedOffset(1)),
                        goalPlus2 = getDailyGoalForDate(getDateStringForSelectedOffset(2)),
                        appLanguage = appLanguage.value,
                        goalLineSquiggly = _goalLineSquiggly.value,
                        pastIntakesCsv = pastIntakesCsv,
                        pastGoalsCsv = pastGoalsCsv,
                        weeksPrior = weeks,
                        daysPrior = days,
                        pastDaysToShow = _wearPastDaysToShow.value,
                        ramOptimizationEnabled = _wearRamOptimization.value,
                        wearSwipeMode = _wearSwipeMode.value,
                        wearGraphSwipeDir = _wearGraphSwipeDir.value,
                        complicationIconStyle = _wearComplicationIconStyle.value
                    )
                }
            }
        } catch (e: Exception) {
            if (e.message?.contains("API_UNAVAILABLE") == true || e.message?.contains("17: API:") == true) {
                Log.d("WaterViewModel", "Wearable API not available, skipping Wear OS sync setup.")
            } else {
                Log.e("WaterViewModel", "Wear OS sync setup failed: ${e.message}")
            }
        }

        FontWeight.globalDevFontWeight = prefs.getFloat("dev_font_weight", 500f)
        ensureFirebaseInitialized()
        viewModelScope.launch {
            try {
                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                if (auth.currentUser != null) {
                    restoreFromFirestore {
                        syncToFirestore()
                        Log.d("WaterViewModel", "Successfully merged settings and logs from Firestore at startup.")
                    }
                }
            } catch (e: Exception) {
                Log.e("WaterViewModel", "Firebase sync on startup skipped/failed: ${e.message}")
            }
        }
        HapticManager.initialize(application)
        loadCustomDrinks()
        loadCustomThemes()
        
        viewModelScope.launch {
            var lastLogsHash = 0
            var lastGoal = 0
            var isFirst = true
            kotlinx.coroutines.flow.combine(allLogs, dailyGoalMl) { logs, goal -> Pair(logs, goal) }.collectLatest { (logs, goal) ->
                val currentHash = logs.hashCode()
                if (isFirst || currentHash != lastLogsHash || goal != lastGoal) {
                    isFirst = false
                    lastLogsHash = currentHash
                    lastGoal = goal
                    kotlinx.coroutines.delay(1200) // Debounce: wait 1.2 seconds after the last change for a snappier response
                    refreshWeeklyProgressSummary(forceGemini = false, logsOverride = logs)
                }
            }
        }
        
        // Periodic hourly update for hydration insight
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(3600000L) // 1 hour in ms
                Log.d("WaterViewModel", "Periodic hourly hydration insight refresh.")
                refreshWeeklyProgressSummary(forceGemini = false)
            }
        }
        // Setup Auto Crash Reporting is disabled locally for better logcat captures.
        
        // Migrate legacy "OLED" and "DEFAULT" theme selections to DYNAMIC (with OLED mode enabled if they were on OLED)
        if (_appTheme.value == "OLED") {
            _appTheme.value = "DYNAMIC"
            prefs.edit().putString("app_theme", "DYNAMIC").apply()
            _oledModeEnabled.value = true
            prefs.edit().putBoolean("oled_mode_enabled", true).apply()
        } else if (_appTheme.value == "DEFAULT") {
            _appTheme.value = "DYNAMIC"
            prefs.edit().putString("app_theme", "DYNAMIC").apply()
        }
        // Initial setup for alarms if enabled
        val todayStr = getCurrentDateString()
        _todayWorkoutWaterBonus.value = prefs.getInt("workout_bonus_$todayStr", 0)
        _todayWorkoutAiCoachResponse.value = prefs.getString("workout_coach_$todayStr", "") ?: ""
        _todaySleepWaterBonus.value = prefs.getInt("sleep_bonus_$todayStr", 0)
        _todaySleepAiCoachResponse.value = prefs.getString("sleep_coach_$todayStr", "") ?: ""
        
        val savedSleepVal = prefs.getFloat("sleep_hours_$todayStr", -1f)
        _todaySleepHours.value = if (savedSleepVal >= 0f) savedSleepVal.toDouble() else null

        _todayHeatWaterBonus.value = prefs.getInt("heat_bonus_$todayStr", 0)
        _todayHeatAiCoachResponse.value = prefs.getString("heat_coach_$todayStr", "") ?: ""
        
        syncReminders()
        calculateStreak()
        checkHealthConnectPermissions()
        checkAutoUpdateDaily()

        if (_workoutWaterAdjustmentEnabled.value) {
            detectWorkoutsFromHealthConnect()
        }
        if (_sleepWaterAdjustmentEnabled.value) {
            detectSleepFromHealthConnect()
        }
        if (_heatWaterAdjustmentEnabled.value) {
            refreshLocationAndWeather()
        }

        // Periodic weather update loop based on interval setting
        viewModelScope.launch {
            while (true) {
                val intervalMins = when (_weatherUpdateIntervalMinutes.value) {
                    "5" -> 5
                    "10" -> 10
                    "20" -> 20
                    "30" -> 30
                    "45" -> 45
                    "60" -> 60
                    "120" -> 120
                    else -> 0 // DEFAULT (startup/manual only)
                }
                if (intervalMins > 0 && _heatWaterAdjustmentEnabled.value) {
                    kotlinx.coroutines.delay(intervalMins * 60 * 1000L)
                    Log.d("WaterViewModel", "Periodic weather check: triggered every $intervalMins minutes.")
                    refreshLocationAndWeather()
                } else {
                    kotlinx.coroutines.delay(30000L) // check settings periodically
                }
            }
        }

        // Register Shared Preference Listener for reactive widget-to-app changes
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)

        // Reactive widget synchronizations
        viewModelScope.launch {
            _currentDate.collect { date ->
                if (prefs.getString("widget_selected_date", null) != date) {
                    prefs.edit().putString("widget_selected_date", date).apply()
                }
                updateHomeScreenWidget()
            }
        }
        viewModelScope.launch {
            totalIntakeToday.collect {
                updateHomeScreenWidget()
            }
        }
        // Recalculate streak reactively whenever any log entry is inserted, updated, or removed
        viewModelScope.launch {
            allLogs.collect {
                calculateStreak()
            }
        }

        // Initialize watch details detection and periodic polling
        try {
            com.google.android.gms.wearable.Wearable.getDataClient(getApplication()).addListener(watchDetailsListener)
            loadInitialWatchDetails()
            viewModelScope.launch {
                while (isActive) {
                    checkConnectedWatchNodes()
                    delay(10000)
                }
            }
        } catch (e: Exception) {
            if (isWearableUnavailable(e)) {
                Log.d("WaterViewModel", "Wearable API not available, skipping watch details receiver setup.")
            } else {
                Log.e("WaterViewModel", "Failed to setup watch details receiver: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            com.google.android.gms.wearable.Wearable.getDataClient(getApplication()).removeListener(watchDetailsListener)
        } catch (e: Exception) {
            if (isWearableUnavailable(e)) {
                // Ignore
            } else {
                Log.e("WaterViewModel", "Failed to remove watch details listener: ${e.message}")
            }
        }
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    private fun checkAutoUpdateDaily() {
        val lastCheck = prefs.getLong("last_update_check_time", 0L)
        val currentTime = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L
        if (currentTime - lastCheck >= oneDayMs) {
            checkForUpdates(silent = true)
        }
    }

    fun checkHealthConnectPermissions() {
        viewModelScope.launch {
            val authorized = HealthConnectManager.hasAllPermissions(getApplication())
            _isHealthConnectAuthorized.value = authorized
            if (authorized) {
                // Periodically check and import logs from last 2 days on permission checks/tab changes
                val startTime = System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000L
                val endTime = System.currentTimeMillis() + 60 * 60 * 1000L
                externalSyncRepository.manuallyFetchHydrationData(startTime, endTime)
            }
        }
    }

    fun syncToHealthConnect(amountMl: Double, timestamp: Long) {
        viewModelScope.launch {
            if (HealthConnectManager.hasAllPermissions(getApplication())) {
                HealthConnectManager.writeHydration(getApplication(), amountMl, timestamp)
            }
        }
    }

    fun syncAllTodayToHealthConnect() {
        viewModelScope.launch {
            if (HealthConnectManager.hasAllPermissions(getApplication())) {
                // Export today's logs to Google Health Connect
                todayLogs.value.forEach { log ->
                    HealthConnectManager.writeHydration(
                        getApplication(),
                        log.waterEquivalentMl.toDouble(),
                        log.timestamp
                    )
                }
                // Import the last 3 days of hydration entries from Google Health Connect
                val startTime = System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000L
                val endTime = System.currentTimeMillis() + 60 * 60 * 1000L
                externalSyncRepository.manuallyFetchHydrationData(startTime, endTime)
            }
        }
    }

    fun setDate(dateStr: String) {
        _currentDate.value = dateStr
    }

    fun addWaterLog(
        amountMl: Int,
        beverageType: String = "Water",
        waterEquivalency: Float = 1.0f,
        sourceDevice: String? = null
    ) {
        viewModelScope.launch {
            val beforeIntake = totalIntakeToday.value
            val selectedDateStr = _currentDate.value
            val goal = getDailyGoalForDate(selectedDateStr)
            
            // Adjust to prevent the total daily intake from going under zero
            val potentialEquivalent = (amountMl * waterEquivalency).toInt()
            val adjustedAmountMl = if (beforeIntake + potentialEquivalent < 0) {
                if (waterEquivalency != 0f) {
                    (-beforeIntake / waterEquivalency).toInt()
                } else {
                    0
                }
            } else {
                amountMl
            }
            
            if (adjustedAmountMl == 0 && amountMl != 0) {
                return@launch
            }
            
            val equivalentMl = (adjustedAmountMl * waterEquivalency).toInt()
            val nextTotal = beforeIntake + equivalentMl

            val newLog = WaterLog(
                amountMl = adjustedAmountMl,
                beverageType = beverageType,
                waterEquivalency = waterEquivalency,
                waterEquivalentMl = equivalentMl,
                dateString = selectedDateStr,
                timestamp = if (selectedDateStr == getCurrentDateString()) {
                    System.currentTimeMillis()
                } else {
                    getTimestampForDateString(selectedDateStr)
                },
                sourceDevice = sourceDevice ?: android.os.Build.MODEL
            )
            repository.insertLog(newLog)
            // Recalculate streak in case a day's goal has been reached/restored
            calculateStreak()
            // Auto-sync to Health Connect
            syncToHealthConnect(equivalentMl.toDouble(), newLog.timestamp)
            triggerAutoBackup()

            // Trigger corresponding haptics
            if (beforeIntake < goal && nextTotal >= goal) {
                triggerGoalHaptic()
            } else {
                triggerButtonHaptic()
            }
            updateHomeScreenWidget()
            triggerWearOsSync()
        }
    }

    fun addWaterLogForDate(amountMl: Int, dateStr: String, sourceDevice: String?) {
        viewModelScope.launch {
            val equivalentMl = amountMl
            val timestamp = if (dateStr == getCurrentDateString()) {
                System.currentTimeMillis()
            } else {
                getTimestampForDateString(dateStr)
            }
            val newLog = WaterLog(
                amountMl = amountMl,
                beverageType = "Water",
                waterEquivalency = 1.0f,
                waterEquivalentMl = equivalentMl,
                dateString = dateStr,
                timestamp = timestamp,
                sourceDevice = sourceDevice ?: android.os.Build.MODEL
            )
            repository.insertLog(newLog)
            calculateStreak()
            triggerAutoBackup()
            updateHomeScreenWidget()
            triggerWearOsSync()
        }
    }

    fun subtractWaterLogAmountForOffset(offset: Int, amountMl: Int) {
        viewModelScope.launch {
            val targetDateStr = getDateStringForSelectedOffset(offset)
            val logs = repository.getLogsForDate(targetDateStr).first().sortedByDescending { it.timestamp }
            var remainingToSubtract = amountMl
            for (log in logs) {
                if (remainingToSubtract <= 0) break
                if (log.amountMl <= remainingToSubtract) {
                    remainingToSubtract -= log.amountMl
                    repository.deleteLog(log)
                } else {
                    val updated = log.copy(amountMl = log.amountMl - remainingToSubtract)
                    remainingToSubtract = 0
                    repository.insertLog(updated)
                }
            }
            calculateStreak()
            triggerAutoBackup()
            updateHomeScreenWidget()
            triggerWearOsSync()
        }
    }

    fun deleteMostRecentWaterLogForOffset(offset: Int) {
        subtractWaterLogAmountForOffset(offset, quickAddAmount.value)
    }

    fun addWaterLogWithCustomDate(amountMl: Int, beverageType: String, waterEquivalency: Float, dateStr: String, daysAgoOffset: Int) {
        viewModelScope.launch {
            val calendar = java.util.Calendar.getInstance()
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -daysAgoOffset)
            val timestamp = calendar.timeInMillis
            val equivalentMl = (amountMl * waterEquivalency).toInt()
            val newLog = WaterLog(
                amountMl = amountMl,
                beverageType = beverageType,
                waterEquivalency = waterEquivalency,
                waterEquivalentMl = equivalentMl,
                dateString = dateStr,
                timestamp = timestamp,
                sourceDevice = android.os.Build.MODEL
            )
            repository.insertLog(newLog)
            calculateStreak()
            triggerAutoBackup()
            triggerButtonHaptic()
            updateHomeScreenWidget()
            triggerWearOsSync()
        }
    }

    fun updateLogTime(log: WaterLog, hour: Int, minute: Int) {
        viewModelScope.launch {
            val calendar = java.util.Calendar.getInstance()
            calendar.timeInMillis = log.timestamp
            calendar.set(java.util.Calendar.HOUR_OF_DAY, hour)
            calendar.set(java.util.Calendar.MINUTE, minute)
            
            val updatedLog = log.copy(timestamp = calendar.timeInMillis)
            repository.insertLog(updatedLog)
            updateHomeScreenWidget()
            triggerWearOsSync()
        }
    }

    fun deleteWaterLog(log: WaterLog) {
        viewModelScope.launch {
            repository.deleteLog(log)
            calculateStreak()
            triggerAutoBackup()
            updateHomeScreenWidget()
            triggerWearOsSync()
        }
    }

    fun deleteWaterLogsForDate(dateString: String) {
        viewModelScope.launch {
            repository.deleteLogsForDate(dateString)
            calculateStreak()
            triggerAutoBackup()
            updateHomeScreenWidget()
            triggerWearOsSync()
        }
    }

    fun updateDailyGoal(goalMl: Int) {
        _baseDailyGoalMl.value = goalMl
        prefs.edit().putInt("daily_goal", goalMl).apply()
        calculateStreak() // Goal change can affect streak
        updateHomeScreenWidget()
        triggerAutoBackup()
        triggerWearOsSync()
    }

    fun getDailyGoalForDate(dateStr: String): Int {
        val base = _baseDailyGoalMl.value
        var bonus = 0
        if (_workoutWaterAdjustmentEnabled.value) {
            bonus += prefs.getInt("workout_bonus_$dateStr", 0)
        }
        if (_sleepWaterAdjustmentEnabled.value) {
            bonus += prefs.getInt("sleep_bonus_$dateStr", 0)
        }
        if (_heatWaterAdjustmentEnabled.value) {
            bonus += prefs.getInt("heat_bonus_$dateStr", 0)
        }
        return base + bonus
    }

    private fun updateHomeScreenWidget() {
        viewModelScope.launch {
            try {
                com.pixelwater.app.widget.WaterTrackerWidget().updateAll(getApplication())
            } catch (e: Exception) {
                Log.e("WaterViewModel", "Error updating widgets", e)
            }
        }
    }

    fun updateReminders(enabled: Boolean) {
        _remindersEnabled.value = enabled
        prefs.edit().putBoolean("reminders_enabled", enabled).apply()
        syncReminders()
    }

    fun updateRemindersDestination(destination: String) {
        _remindersDestination.value = destination
        prefs.edit().putString("reminders_destination", destination).apply()
    }

    fun updateReminderInterval(intervalHours: Int) {
        _reminderInterval.value = intervalHours
        prefs.edit().putInt("reminder_interval", intervalHours).apply()
        syncReminders()
    }

    fun updateSleepWindow(start: Int, end: Int) {
        _startHour.value = start
        _endHour.value = end
        prefs.edit()
            .putInt("reminder_start_hour", start)
            .putInt("reminder_end_hour", end)
            .apply()
        syncReminders()
    }

    fun updateAppTheme(theme: String) {
        _appTheme.value = theme
        prefs.edit().putString("app_theme", theme).apply()
    }

    fun updateAppThemePaletteIndex(index: Int) {
        _appThemePaletteIndex.value = index
        prefs.edit().putInt("app_theme_palette_index", index).apply()
        
        val colors = wallpaperThemeColors.value
        if (colors.isNotEmpty() && index in colors.indices) {
            syncThemeColorToShapes(colors[index])
        }
        
        triggerButtonHaptic()
    }

    fun updateStaticThemeSeed(seed: Int) {
        _staticThemeSeed.value = seed
        prefs.edit().putInt("static_theme_seed", seed).apply()
        
        syncThemeColorToShapes(seed)
        
        triggerButtonHaptic()
    }

    private fun syncThemeColorToShapes(colorInt: Int) {
        if (!_dynamicPaletteShapesOverride.value) return
        
        _shapeMonochromeEnabled.value = true
        prefs.edit().putBoolean("shape_monochrome_enabled", true).apply()
        
        _shapeMonochromeSource.value = 0
        prefs.edit().putInt("shape_monochrome_source", 0).apply()

        val r = android.graphics.Color.red(colorInt).toFloat()
        val g = android.graphics.Color.green(colorInt).toFloat()
        val b = android.graphics.Color.blue(colorInt).toFloat()
        
        prefs.edit().putBoolean("gimmick_global_color_enabled", true)
            .putFloat("gimmick_global_r", r)
            .putFloat("gimmick_global_g", g)
            .putFloat("gimmick_global_b", b)
            .apply()
            
        // Reset shape modes so they actually follow the theme/monochrome settings
        updateShapeColorMode("A", 0)
        updateShapeColorMode("B", 1)
        updateShapeColorMode("C", 2)
        updateShapeColorMode("D", 0)
    }

    fun updateMonochromeEnabled(enabled: Boolean) {
        _monochromeEnabled.value = enabled
        prefs.edit().putBoolean("monochrome_enabled", enabled).apply()
    }

    fun updateMonochromeColorTarget(target: Int) {
        _monochromeColorTarget.value = target
        prefs.edit().putInt("monochrome_color_target", target).apply()
        triggerButtonHaptic()
    }

    fun updateProgressCircleColorSource(source: String) {
        _progressCircleColorSource.value = source
        prefs.edit().putString("progress_circle_color_source", source).apply()
        triggerButtonHaptic()
    }

    fun updateProgressCircleThemeColor(target: Int) {
        _progressCircleThemeColor.value = target
        prefs.edit().putInt("progress_circle_theme_color", target).apply()
        triggerButtonHaptic()
    }

    fun updateProgressCircleStandardColorIndex(index: Int) {
        _progressCircleStandardColorIndex.value = index
        prefs.edit().putInt("progress_circle_standard_color_index", index).apply()
        triggerButtonHaptic()
    }

    fun getSwatchColors(seedColorInt: Int): Triple<Color, Color, Color> {
        val wallpaperList = wallpaperThemeColors.value
        val idx = wallpaperList.indexOf(seedColorInt)
        if (wallpaperList.isNotEmpty() && idx != -1) {
            val p = wallpaperList[idx % wallpaperList.size]
            val s = wallpaperList[(idx + 1) % wallpaperList.size]
            val t = wallpaperList[(idx + 2) % wallpaperList.size]
            return Triple(
                Color(p or (0xFF.toInt() shl 24)),
                Color(s or (0xFF.toInt() shl 24)),
                Color(t or (0xFF.toInt() shl 24))
            )
        }
        val hsl = FloatArray(3)
        androidx.core.graphics.ColorUtils.colorToHSL(seedColorInt, hsl)
        val hue = hsl[0]
        val sat = hsl[1]
        
        val topColor = Color(seedColorInt or (0xFF.toInt() shl 24))
        val botLeftColor = Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf((hue + 30f) % 360f, sat.coerceIn(0.3f, 0.8f), 0.55f)))
        val botRightColor = Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf((hue + 120f) % 360f, sat.coerceIn(0.3f, 0.8f), 0.65f)))
        
        return Triple(topColor, botLeftColor, botRightColor)
    }

    private val _themeAiLoading = MutableStateFlow(false)
    val themeAiLoading: StateFlow<Boolean> = _themeAiLoading.asStateFlow()

    private val _themeAiError = MutableStateFlow<String?>(null)
    val themeAiError: StateFlow<String?> = _themeAiError.asStateFlow()

    private val _aiCoachShapesOverride = MutableStateFlow(prefs.getBoolean("ai_coach_shapes_override", true))
    val aiCoachShapesOverride: StateFlow<Boolean> = _aiCoachShapesOverride.asStateFlow()

    fun updateAiCoachShapesOverride(value: Boolean) {
        _aiCoachShapesOverride.value = value
        prefs.edit().putBoolean("ai_coach_shapes_override", value).apply()
    }

    private val _aiCoachDisableEmojis = MutableStateFlow(prefs.getBoolean("ai_coach_disable_emojis", false))
    val aiCoachDisableEmojis: StateFlow<Boolean> = _aiCoachDisableEmojis.asStateFlow()

    fun updateAiCoachDisableEmojis(disabled: Boolean) {
        _aiCoachDisableEmojis.value = disabled
        prefs.edit().putBoolean("ai_coach_disable_emojis", disabled).apply()
        triggerButtonHaptic()
    }

    private val _dynamicPaletteShapesOverride = MutableStateFlow(prefs.getBoolean("dynamic_palette_shapes_override", true))
    val dynamicPaletteShapesOverride: StateFlow<Boolean> = _dynamicPaletteShapesOverride.asStateFlow()

    fun updateDynamicPaletteShapesOverride(value: Boolean) {
        _dynamicPaletteShapesOverride.value = value
        prefs.edit().putBoolean("dynamic_palette_shapes_override", value).apply()
        if (value) {
            updateShapeColorMode("A", 0)
            updateShapeColorMode("B", 1)
            updateShapeColorMode("C", 2)
            updateShapeColorMode("D", 0)
        }
    }

    fun loadCustomThemes() {
        val jsonStr = prefs.getString("custom_themes_list", "[]") ?: "[]"
        try {
            val jsonArray = JSONArray(jsonStr)
            val themes = mutableListOf<CustomTheme>()
            for (i in 0 until jsonArray.length()) {
                val s = jsonArray.getJSONObject(i)
                themes.add(CustomTheme(
                    id = s.optString("id", java.util.UUID.randomUUID().toString()),
                    name = s.optString("name", "Unnamed Theme"),
                    prompt = s.optString("prompt", ""),
                    isStrict = s.optBoolean("isStrict", false),
                    dateCreated = s.optString("dateCreated", ""),
                    themeMode = s.optString("themeMode", "Dark"),
                    oledMode = s.optBoolean("oledMode", false),
                    boxBgOledEnabled = s.optBoolean("boxBgOledEnabled", false),
                    appTheme = s.optString("appTheme", "STATIC"),
                    staticThemeSeed = s.optInt("staticThemeSeed", 0xFF1D5AAB.toInt()),
                    boxBgSource = s.optString("boxBgSource", "THEME"),
                    boxBgPaletteChoice = s.optInt("boxBgPaletteChoice", 0),
                    shapeMonochromeEnabled = s.optBoolean("shapeMonochromeEnabled", false),
                    shapeMonochromeSource = s.optInt("shapeMonochromeSource", 0),
                    shapeUseIndependentDynamicPalette = s.optBoolean("shapeUseIndependentDynamicPalette", false),
                    progressCircleColorSource = s.optString("progressCircleColorSource", "THEME"),
                    progressCircleThemeColor = s.optInt("progressCircleThemeColor", 0),
                    progressCircleStandardColorIndex = s.optInt("progressCircleStandardColorIndex", 0),
                    materialShapesRotationSpeed = s.optDouble("materialShapesRotationSpeed", 1.0).toFloat(),
                    auraGlowRotationSpeed = s.optDouble("auraGlowRotationSpeed", 1.0).toFloat(),
                    shapeRotMultA = s.optDouble("shapeRotMultA", 1.0).toFloat(),
                    shapeRotMultB = s.optDouble("shapeRotMultB", -1.0).toFloat(),
                    shapeRotMultC = s.optDouble("shapeRotMultC", 1.0).toFloat(),
                    shapeRotMultD = s.optDouble("shapeRotMultD", -1.0).toFloat()
                ))
            }
            _customThemes.value = themes
        } catch (e: Exception) {
            Log.e("WaterViewModel", "Failed to load custom themes", e)
        }
    }

    fun saveCustomTheme(theme: CustomTheme) {
        val currentThemes = _customThemes.value.toMutableList()
        currentThemes.removeAll { it.id == theme.id }
        currentThemes.add(0, theme)
        _customThemes.value = currentThemes
        try {
            val jsonArray = JSONArray()
            for (t in currentThemes) {
                val s = JSONObject()
                s.put("id", t.id)
                s.put("name", t.name)
                s.put("prompt", t.prompt)
                s.put("isStrict", t.isStrict)
                s.put("dateCreated", t.dateCreated)
                s.put("themeMode", t.themeMode)
                s.put("oledMode", t.oledMode)
                s.put("boxBgOledEnabled", t.boxBgOledEnabled)
                s.put("appTheme", t.appTheme)
                s.put("staticThemeSeed", t.staticThemeSeed)
                s.put("boxBgSource", t.boxBgSource)
                s.put("boxBgPaletteChoice", t.boxBgPaletteChoice)
                s.put("shapeMonochromeEnabled", t.shapeMonochromeEnabled)
                s.put("shapeMonochromeSource", t.shapeMonochromeSource)
                s.put("shapeUseIndependentDynamicPalette", t.shapeUseIndependentDynamicPalette)
                s.put("progressCircleColorSource", t.progressCircleColorSource)
                s.put("progressCircleThemeColor", t.progressCircleThemeColor)
                s.put("progressCircleStandardColorIndex", t.progressCircleStandardColorIndex)
                s.put("materialShapesRotationSpeed", t.materialShapesRotationSpeed.toDouble())
                s.put("auraGlowRotationSpeed", t.auraGlowRotationSpeed.toDouble())
                s.put("shapeRotMultA", t.shapeRotMultA.toDouble())
                s.put("shapeRotMultB", t.shapeRotMultB.toDouble())
                s.put("shapeRotMultC", t.shapeRotMultC.toDouble())
                s.put("shapeRotMultD", t.shapeRotMultD.toDouble())
                jsonArray.put(s)
            }
            prefs.edit().putString("custom_themes_list", jsonArray.toString()).apply()
            triggerButtonHaptic()
        } catch (e: Exception) {
            Log.e("WaterViewModel", "Failed to save custom themes", e)
        }
    }

    fun deleteCustomTheme(id: String) {
        val currentThemes = _customThemes.value.toMutableList()
        currentThemes.removeAll { it.id == id }
        _customThemes.value = currentThemes
        try {
            val jsonArray = JSONArray()
            for (t in currentThemes) {
                val s = JSONObject()
                s.put("id", t.id)
                s.put("name", t.name)
                s.put("prompt", t.prompt)
                s.put("isStrict", t.isStrict)
                s.put("dateCreated", t.dateCreated)
                s.put("themeMode", t.themeMode)
                s.put("oledMode", t.oledMode)
                s.put("boxBgOledEnabled", t.boxBgOledEnabled)
                s.put("appTheme", t.appTheme)
                s.put("staticThemeSeed", t.staticThemeSeed)
                s.put("boxBgSource", t.boxBgSource)
                s.put("boxBgPaletteChoice", t.boxBgPaletteChoice)
                s.put("shapeMonochromeEnabled", t.shapeMonochromeEnabled)
                s.put("shapeMonochromeSource", t.shapeMonochromeSource)
                s.put("shapeUseIndependentDynamicPalette", t.shapeUseIndependentDynamicPalette)
                s.put("progressCircleColorSource", t.progressCircleColorSource)
                s.put("progressCircleThemeColor", t.progressCircleThemeColor)
                s.put("progressCircleStandardColorIndex", t.progressCircleStandardColorIndex)
                s.put("materialShapesRotationSpeed", t.materialShapesRotationSpeed.toDouble())
                s.put("auraGlowRotationSpeed", t.auraGlowRotationSpeed.toDouble())
                s.put("shapeRotMultA", t.shapeRotMultA.toDouble())
                s.put("shapeRotMultB", t.shapeRotMultB.toDouble())
                s.put("shapeRotMultC", t.shapeRotMultC.toDouble())
                s.put("shapeRotMultD", t.shapeRotMultD.toDouble())
                jsonArray.put(s)
            }
            prefs.edit().putString("custom_themes_list", jsonArray.toString()).apply()
            triggerButtonHaptic()
        } catch (e: Exception) {
            Log.e("WaterViewModel", "Failed to delete theme", e)
        }
    }

    fun applyCustomTheme(theme: CustomTheme) {
        updateThemeMode(theme.themeMode)
        updateOledMode(theme.oledMode)
        updateBoxBgOledEnabled(theme.boxBgOledEnabled)
        updateAppTheme(theme.appTheme)
        updateStaticThemeSeed(theme.staticThemeSeed)
        updateBoxBgSource(theme.boxBgSource)
        updateBoxBgPaletteChoice(theme.boxBgPaletteChoice)
        updateShapeMonochromeEnabled(theme.shapeMonochromeEnabled)
        updateShapeMonochromeSource(theme.shapeMonochromeSource)
        updateShapeUseIndependentDynamicPalette(theme.shapeUseIndependentDynamicPalette)
        updateProgressCircleColorSource(theme.progressCircleColorSource)
        updateProgressCircleThemeColor(theme.progressCircleThemeColor)
        updateProgressCircleStandardColorIndex(theme.progressCircleStandardColorIndex)
        updateMaterialShapesRotationSpeed(theme.materialShapesRotationSpeed)
        updateAuraGlowRotationSpeed(theme.auraGlowRotationSpeed)
        updateShapeRotMultA(theme.shapeRotMultA)
        updateShapeRotMultB(theme.shapeRotMultB)
        updateShapeRotMultC(theme.shapeRotMultC)
        updateShapeRotMultD(theme.shapeRotMultD)
        triggerButtonHaptic()
    }

    fun parseAndApplyThemeJson(jsonString: String, promptText: String = "", isStrict: Boolean = false, saveToCustom: Boolean = false): CustomTheme? {
        try {
            var cleaned = jsonString.trim()
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.substringBeforeLast("```")
                if (cleaned.startsWith("```json")) {
                    cleaned = cleaned.removePrefix("```json")
                } else {
                    cleaned = cleaned.removePrefix("```")
                }
            }
            cleaned = cleaned.trim()
            
            val s = JSONObject(cleaned)
            val seedRaw = s.opt("staticThemeSeed")
            val seedInt = when (seedRaw) {
                is Number -> seedRaw.toInt()
                is String -> {
                    val str = seedRaw.trim()
                    if (str.startsWith("0x") || str.startsWith("0X")) {
                        java.lang.Long.decode(str).toInt()
                    } else if (str.startsWith("#")) {
                        android.graphics.Color.parseColor(str)
                    } else {
                        str.toIntOrNull() ?: 0xFF1D5AAB.toInt()
                    }
                }
                else -> 0xFF1D5AAB.toInt()
            }

            val themeName = s.optString("name", "AI Theme Choice")
            val parsedTheme = CustomTheme(
                id = java.util.UUID.randomUUID().toString(),
                name = themeName,
                prompt = promptText,
                isStrict = isStrict,
                dateCreated = java.text.DateFormat.getDateTimeInstance().format(java.util.Date()),
                themeMode = s.optString("themeMode", "Dark"),
                oledMode = s.optBoolean("oledMode", false),
                boxBgOledEnabled = s.optBoolean("boxBgOledEnabled", false),
                appTheme = s.optString("appTheme", "STATIC"),
                staticThemeSeed = seedInt,
                boxBgSource = s.optString("boxBgSource", "THEME"),
                boxBgPaletteChoice = s.optInt("boxBgPaletteChoice", 0),
                shapeMonochromeEnabled = s.optBoolean("shapeMonochromeEnabled", false),
                shapeMonochromeSource = s.optInt("shapeMonochromeSource", 0),
                shapeUseIndependentDynamicPalette = s.optBoolean("shapeUseIndependentDynamicPalette", false),
                progressCircleColorSource = s.optString("progressCircleColorSource", "THEME"),
                progressCircleThemeColor = s.optInt("progressCircleThemeColor", 0),
                progressCircleStandardColorIndex = s.optInt("progressCircleStandardColorIndex", 0),
                materialShapesRotationSpeed = s.optDouble("materialShapesRotationSpeed", 1.0).toFloat(),
                auraGlowRotationSpeed = s.optDouble("auraGlowRotationSpeed", 1.0).toFloat(),
                shapeRotMultA = s.optDouble("shapeRotMultA", 1.0).toFloat(),
                shapeRotMultB = s.optDouble("shapeRotMultB", -1.0).toFloat(),
                shapeRotMultC = s.optDouble("shapeRotMultC", 1.0).toFloat(),
                shapeRotMultD = s.optDouble("shapeRotMultD", -1.0).toFloat()
            )
            
            applyCustomTheme(parsedTheme)
            if (saveToCustom) {
                saveCustomTheme(parsedTheme)
            }
            return parsedTheme
        } catch (e: Exception) {
            Log.e("WaterViewModel", "Failed to parse theme JSON: $jsonString", e)
        }
        return null
    }

    fun analyzeAndApplyThemeWithAi(
        presetName: String,
        extraContext: String = "",
        isStrict: Boolean = false,
        isWallpaperBased: Boolean = false,
        saveToCustom: Boolean = false,
        onComplete: (String) -> Unit = {}
    ) {
        val localPresetJson = getLocalPresetThemeJson(presetName)
        if (localPresetJson != null) {
            val parsed = parseAndApplyThemeJson(localPresetJson, promptText = presetName, isStrict = isStrict, saveToCustom = saveToCustom)
            if (parsed != null) {
                onComplete("Theme successfully created: ${parsed.name}!")
            } else {
                onComplete("Failed to parse local preset theme.")
            }
            return
        }

        viewModelScope.launch {
            _themeAiLoading.value = true
            _themeAiError.value = null
            try {
                val apiKey = if (_geminiApiKey.value.isNotBlank()) _geminiApiKey.value.trim() else com.pixelwater.app.BuildConfig.GEMINI_API_KEY.trim()
                if (apiKey == "MY_GEMINI_API_KEY" || apiKey.isBlank()) {
                    _themeAiError.value = "Unconfigured API Key"
                    _themeAiLoading.value = false
                    onComplete("API Key is not configured. Please add your key in Settings.")
                    return@launch
                }

                val currentWallpapers = wallpaperThemeColors.value.map { String.format("0x%08X", it) }.joinToString(", ")

                val systemPrompt = """
                    You are "Pixel Theme Coach", an expert material design color and aesthetic theme coordinator.
                    Your goal is to output a single, raw, perfectly validated JSON block representing the color and behavior settings requested.
                    Do NOT wrap your response in markdown backticks like ```json.
                    Only output raw, unformatted JSON. Do NOT include comments. Do NOT include any accompanying conversational text.

                    Fields and expected value constraints:
                    - "name": String (Beautiful descriptive name of the theme)
                    - "themeMode": String ("Dark" or "Light")
                    - "oledMode": Boolean (True if the background of the app is OLED Pitch Black)
                    - "boxBgOledEnabled": Boolean (True if cards should be OLED black on OLED dark backgrounds)
                    - "appTheme": String ("STATIC" or "DYNAMIC")
                    - "staticThemeSeed": String (Hex value of primary theme seed color, in 0xXXXXXXXX form, e.g., "0xFF1D5AAB")
                    - "boxBgSource": String ("THEME" for standard themes or "PALETTE" for independent theme contrast accents)
                    - "boxBgPaletteChoice": Integer (0 for primary accent container, 1 for secondary, 2 for tertiary)
                    - "shapeMonochromeEnabled": Boolean (True if background shapes/glow should be unified monochrome, False for tri-color gradients)
                    - "shapeMonochromeSource": Integer (0 for primary base, 1 for secondary base, 2 for tertiary base)
                    - "shapeUseIndependentDynamicPalette": Boolean (True if shapes should have complementary accents out of wallpaper)
                    - "progressCircleColorSource": String ("THEME" or "STANDARD" color index)
                    - "progressCircleThemeColor": Integer (0 for primary, 1 for secondary, 2 for tertiary)
                    - "progressCircleStandardColorIndex": Integer (0 to 5 for general colors)
                    - "materialShapesRotationSpeed": Float (0.1 to 3.0: Speed of rotating backing shapes)
                    - "auraGlowRotationSpeed": Float (0.1 to 5.0: Speed of animated background glow/shimmer)
                    - "shapeRotMultA": Float (-2.0 to 2.0: Speed multiplier of shape A)
                    - "shapeRotMultB": Float (-2.0 to 2.0: Speed multiplier of shape B)
                    - "shapeRotMultC": Float (-2.0 to 2.0: Speed multiplier of shape C)
                    - "shapeRotMultD": Float (-2.0 to 2.0: Speed multiplier of shape D)

                    CRITICAL CONSTRAINTS:
                    1. When analyzing a theme/preset: You must NOT make the background shapes (rotating background shapes) or aura glow pitch black (oled black). They must have beautiful, visible colors (saturated, contrasting) to ensure seamless aesthetics.
                    2. If isWallpaperBased=true: Choose beautiful complements among the current extracted wallpaper hex colors ($currentWallpapers).
                    3. If custom request: If isStrict=true, loyal/strict to requested colors. If isStrict=false (imaginary mode), expand creatively with vibrant complementary aura speeds and colors in your selection.
                    4. USER TOGGLE FOR SHAPES: The user has set 'Allow AI to modify shape colors' to: ${_aiCoachShapesOverride.value}. 
                       If TRUE, you are encouraged to creatively modify shapeMonochromeEnabled, shapeUseIndependentDynamicPalette, and rotation speeds to fit the theme. 
                       If FALSE, you MUST keep shape-related features conservative (shapeMonochromeEnabled=false, shapeUseIndependentDynamicPalette=false) so shapes perfectly match the raw staticThemeSeed.
                """.trimIndent()

                val userPrompt = if (isWallpaperBased) {
                    "System Wallpaper Match requested. Wallpaper Seed Colors: [$currentWallpapers]. Choose the best matching harmonious color palette."
                } else if (saveToCustom) {
                    "Custom theme requested. User instruction: \"$presetName\". Strict mode: $isStrict. Prompt Details: \"$extraContext\"."
                } else {
                    "Premade Theme Preset requested: \"$presetName\". Optimize current values for maximum visual beauty."
                }

                val request = com.pixelwater.app.data.GeminiRequest(
                    contents = listOf(
                        com.pixelwater.app.data.Content(parts = listOf(com.pixelwater.app.data.Part(text = userPrompt)))
                    ),
                    systemInstruction = com.pixelwater.app.data.Content(parts = listOf(com.pixelwater.app.data.Part(text = systemPrompt))),
                    generationConfig = com.pixelwater.app.data.GenerationConfig(temperature = 0.8f, maxOutputTokens = 800)
                )

                val response = safeGenerateContent(
                    model = _geminiModel.value,
                    apiKey = apiKey,
                    request = request
                )

                val replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (replyText != null) {
                    val parsed = parseAndApplyThemeJson(replyText, promptText = presetName, isStrict = isStrict, saveToCustom = saveToCustom)
                    if (parsed != null) {
                        onComplete("Theme successfully created: ${parsed.name}!")
                    } else {
                        _themeAiError.value = "Failed to parse AI response"
                        onComplete("Could not parse the AI's theme layout.")
                    }
                } else {
                    _themeAiError.value = "Empty AI response"
                    onComplete("AI returned an empty response. Please retry dev build.")
                }
            } catch (e: Exception) {
                Log.e("WaterViewModel", "Theme AI failed", e)
                _themeAiError.value = e.localizedMessage
                onComplete("Failed to reach AI Coach: ${e.localizedMessage}")
            } finally {
                _themeAiLoading.value = false
            }
        }
    }

    private fun getLocalPresetThemeJson(presetName: String): String? {
        val nameLower = presetName.lowercase().trim()
        return when {
            nameLower.contains("midnight oled") || nameLower.contains("μεσάνυχτα oled") -> """
                {
                  "name": "Midnight OLED",
                  "themeMode": "Dark",
                  "oledMode": true,
                  "boxBgOledEnabled": true,
                  "appTheme": "STATIC",
                  "staticThemeSeed": "0xFF7C4DFF",
                  "boxBgSource": "PALETTE",
                  "boxBgPaletteChoice": 1,
                  "shapeMonochromeEnabled": false,
                  "shapeMonochromeSource": 0,
                  "shapeUseIndependentDynamicPalette": false,
                  "progressCircleColorSource": "THEME",
                  "progressCircleThemeColor": 0,
                  "materialShapesRotationSpeed": 1.2,
                  "auraGlowRotationSpeed": 1.4,
                  "shapeRotMultA": 0.8,
                  "shapeRotMultB": -1.1,
                  "shapeRotMultC": 0.6,
                  "shapeRotMultD": -0.7
                }
            """.trimIndent()
            
            nameLower.contains("forest emerald") || nameLower.contains("emerald forest") || nameLower.contains("σμαραγδένιο δάσος") -> """
                {
                  "name": "Forest Emerald",
                  "themeMode": "Dark",
                  "oledMode": false,
                  "boxBgOledEnabled": false,
                  "appTheme": "STATIC",
                  "staticThemeSeed": "0xFF00C853",
                  "boxBgSource": "THEME",
                  "boxBgPaletteChoice": 0,
                  "shapeMonochromeEnabled": false,
                  "shapeMonochromeSource": 0,
                  "shapeUseIndependentDynamicPalette": false,
                  "progressCircleColorSource": "THEME",
                  "progressCircleThemeColor": 0,
                  "materialShapesRotationSpeed": 0.6,
                  "auraGlowRotationSpeed": 0.8,
                  "shapeRotMultA": 0.5,
                  "shapeRotMultB": -0.6,
                  "shapeRotMultC": 0.4,
                  "shapeRotMultD": -0.5
                }
            """.trimIndent()
            
            nameLower.contains("neon galaxy") || nameLower.contains("νέον γαλαξίας") -> """
                {
                  "name": "Neon Galaxy",
                  "themeMode": "Dark",
                  "oledMode": false,
                  "boxBgOledEnabled": false,
                  "appTheme": "STATIC",
                  "staticThemeSeed": "0xFFE040FB",
                  "boxBgSource": "THEME",
                  "boxBgPaletteChoice": 0,
                  "shapeMonochromeEnabled": false,
                  "shapeMonochromeSource": 0,
                  "shapeUseIndependentDynamicPalette": false,
                  "progressCircleColorSource": "THEME",
                  "progressCircleThemeColor": 1,
                  "materialShapesRotationSpeed": 2.0,
                  "auraGlowRotationSpeed": 2.2,
                  "shapeRotMultA": 1.5,
                  "shapeRotMultB": -1.8,
                  "shapeRotMultC": 1.2,
                  "shapeRotMultD": -1.4
                }
            """.trimIndent()
            
            nameLower.contains("minimalist peach") || nameLower.contains("cozy peach") || nameLower.contains("ροδακινί cozy") || nameLower.contains("peach") -> """
                {
                  "name": "Minimalist Peach",
                  "themeMode": "Light",
                  "oledMode": false,
                  "boxBgOledEnabled": false,
                  "appTheme": "STATIC",
                  "staticThemeSeed": "0xFFFF9100",
                  "boxBgSource": "THEME",
                  "boxBgPaletteChoice": 0,
                  "shapeMonochromeEnabled": false,
                  "shapeMonochromeSource": 0,
                  "shapeUseIndependentDynamicPalette": false,
                  "progressCircleColorSource": "THEME",
                  "progressCircleThemeColor": 1,
                  "materialShapesRotationSpeed": 0.8,
                  "auraGlowRotationSpeed": 0.7,
                  "shapeRotMultA": 0.6,
                  "shapeRotMultB": -0.7,
                  "shapeRotMultC": 0.5,
                  "shapeRotMultD": -0.6
                }
            """.trimIndent()
            
            nameLower.contains("glacier breeze") || nameLower.contains("καθαρός παγετώνας") || nameLower.contains("glacier") -> """
                {
                  "name": "Glacier Breeze",
                  "themeMode": "Light",
                  "oledMode": false,
                  "boxBgOledEnabled": false,
                  "appTheme": "STATIC",
                  "staticThemeSeed": "0xFF00E5FF",
                  "boxBgSource": "THEME",
                  "boxBgPaletteChoice": 0,
                  "shapeMonochromeEnabled": false,
                  "shapeMonochromeSource": 0,
                  "shapeUseIndependentDynamicPalette": false,
                  "progressCircleColorSource": "THEME",
                  "progressCircleThemeColor": 0,
                  "materialShapesRotationSpeed": 0.5,
                  "auraGlowRotationSpeed": 0.6,
                  "shapeRotMultA": 0.4,
                  "shapeRotMultB": -0.5,
                  "shapeRotMultC": 0.3,
                  "shapeRotMultD": -0.4
                }
            """.trimIndent()
            
            nameLower.contains("bubble gum pop") || nameLower.contains("bubble gum") || nameLower.contains("τσιχλόφουσκα pop") -> """
                {
                  "name": "Bubble Gum Pop",
                  "themeMode": "Light",
                  "oledMode": false,
                  "boxBgOledEnabled": false,
                  "appTheme": "STATIC",
                  "staticThemeSeed": "0xFFFF4081",
                  "boxBgSource": "THEME",
                  "boxBgPaletteChoice": 0,
                  "shapeMonochromeEnabled": false,
                  "shapeMonochromeSource": 0,
                  "shapeUseIndependentDynamicPalette": false,
                  "progressCircleColorSource": "THEME",
                  "progressCircleThemeColor": 0,
                  "materialShapesRotationSpeed": 1.5,
                  "auraGlowRotationSpeed": 1.6,
                  "shapeRotMultA": 1.2,
                  "shapeRotMultB": -1.4,
                  "shapeRotMultC": 1.0,
                  "shapeRotMultD": -1.1
                }
            """.trimIndent()
            
            nameLower.contains("ocean sunset") || nameLower.contains("ωκεανός & ηλιοβασίλεμα") || nameLower.contains("sunset") -> """
                {
                  "name": "Ocean Sunset",
                  "themeMode": "Dark",
                  "oledMode": false,
                  "boxBgOledEnabled": false,
                  "appTheme": "STATIC",
                  "staticThemeSeed": "0xFFFF6E40",
                  "boxBgSource": "THEME",
                  "boxBgPaletteChoice": 0,
                  "shapeMonochromeEnabled": false,
                  "shapeMonochromeSource": 0,
                  "shapeUseIndependentDynamicPalette": false,
                  "progressCircleColorSource": "THEME",
                  "progressCircleThemeColor": 0,
                  "materialShapesRotationSpeed": 1.1,
                  "auraGlowRotationSpeed": 1.2,
                  "shapeRotMultA": 0.9,
                  "shapeRotMultB": -1.0,
                  "shapeRotMultC": 0.7,
                  "shapeRotMultD": -0.8
                }
            """.trimIndent()
            
            nameLower.contains("lavender meadow") || nameLower.contains("λιβάδι λεβάντας") || nameLower.contains("lavender") -> """
                {
                  "name": "Lavender Meadow",
                  "themeMode": "Light",
                  "oledMode": false,
                  "boxBgOledEnabled": false,
                  "appTheme": "STATIC",
                  "staticThemeSeed": "0xFFB388FF",
                  "boxBgSource": "THEME",
                  "boxBgPaletteChoice": 0,
                  "shapeMonochromeEnabled": false,
                  "shapeMonochromeSource": 0,
                  "shapeUseIndependentDynamicPalette": false,
                  "progressCircleColorSource": "THEME",
                  "progressCircleThemeColor": 0,
                  "materialShapesRotationSpeed": 0.7,
                  "auraGlowRotationSpeed": 0.8,
                  "shapeRotMultA": 0.5,
                  "shapeRotMultB": -0.6,
                  "shapeRotMultC": 0.4,
                  "shapeRotMultD": -0.5
                }
            """.trimIndent()
            
            nameLower.contains("cherry blossom") || nameLower.contains("άνθη κερασιάς") || nameLower.contains("cherry") -> """
                {
                  "name": "Cherry Blossom",
                  "themeMode": "Light",
                  "oledMode": false,
                  "boxBgOledEnabled": false,
                  "appTheme": "STATIC",
                  "staticThemeSeed": "0xFFFF80AB",
                  "boxBgSource": "THEME",
                  "boxBgPaletteChoice": 0,
                  "shapeMonochromeEnabled": false,
                  "shapeMonochromeSource": 0,
                  "shapeUseIndependentDynamicPalette": false,
                  "progressCircleColorSource": "THEME",
                  "progressCircleThemeColor": 0,
                  "materialShapesRotationSpeed": 0.6,
                  "auraGlowRotationSpeed": 0.7,
                  "shapeRotMultA": 0.4,
                  "shapeRotMultB": -0.5,
                  "shapeRotMultC": 0.3,
                  "shapeRotMultD": -0.4
                }
            """.trimIndent()
            
            nameLower.contains("tropical oasis") || nameLower.contains("τροπική όαση") || nameLower.contains("tropical") || nameLower.contains("oasis") -> """
                {
                  "name": "Tropical Oasis",
                  "themeMode": "Dark",
                  "oledMode": false,
                  "boxBgOledEnabled": false,
                  "appTheme": "STATIC",
                  "staticThemeSeed": "0xFF00BFA5",
                  "boxBgSource": "THEME",
                  "boxBgPaletteChoice": 0,
                  "shapeMonochromeEnabled": false,
                  "shapeMonochromeSource": 0,
                  "shapeUseIndependentDynamicPalette": false,
                  "progressCircleColorSource": "THEME",
                  "progressCircleThemeColor": 0,
                  "materialShapesRotationSpeed": 1.0,
                  "auraGlowRotationSpeed": 1.1,
                  "shapeRotMultA": 0.8,
                  "shapeRotMultB": -0.9,
                  "shapeRotMultC": 0.6,
                  "shapeRotMultD": -0.7
                }
            """.trimIndent()
            
            nameLower.contains("autumn leaves") || nameLower.contains("φθινοπωρινά φύλλα") || nameLower.contains("autumn") -> """
                {
                  "name": "Autumn Leaves",
                  "themeMode": "Dark",
                  "oledMode": false,
                  "boxBgOledEnabled": false,
                  "appTheme": "STATIC",
                  "staticThemeSeed": "0xFFFFAB40",
                  "boxBgSource": "THEME",
                  "boxBgPaletteChoice": 0,
                  "shapeMonochromeEnabled": false,
                  "shapeMonochromeSource": 0,
                  "shapeUseIndependentDynamicPalette": false,
                  "progressCircleColorSource": "THEME",
                  "progressCircleThemeColor": 0,
                  "materialShapesRotationSpeed": 0.9,
                  "auraGlowRotationSpeed": 1.0,
                  "shapeRotMultA": 0.7,
                  "shapeRotMultB": -0.8,
                  "shapeRotMultC": 0.5,
                  "shapeRotMultD": -0.6
                }
            """.trimIndent()
            
            else -> null
        }
    }

    private fun getWallpaperSeedColors(context: Context): List<Int> {
        val defaultSeeds = listOf(
            0xFF1D5AAB.toInt(), // Ice Blue / Sky Blue
            0xFF4B5563.toInt(), // Slate Gray
            0xFFD97706.toInt(), // Warm Amber / Orange
            0xFF3B82F6.toInt(), // Deep Royal Blue
            0xFFEC4899.toInt()  // Hot Pink
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val colors = Api27Helper.getWallpaperColors(context)
            if (colors != null) {
                return colors
            }
        }
        return defaultSeeds
    }

    private object Api27Helper {
        @androidx.annotation.RequiresApi(Build.VERSION_CODES.O_MR1)
        fun getWallpaperColors(context: Context): List<Int>? {
            return try {
                val wallpaperManager = WallpaperManager.getInstance(context)
                val colors = wallpaperManager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM) ?: return null
                val list = mutableListOf<Int>()
                list.add(colors.primaryColor.toArgb())
                colors.secondaryColor?.let { list.add(it.toArgb()) }
                colors.tertiaryColor?.let { list.add(it.toArgb()) }
                
                val primaryArgb = colors.primaryColor.toArgb()
                val hsl = FloatArray(3)
                androidx.core.graphics.ColorUtils.colorToHSL(primaryArgb, hsl)
                
                while (list.size < 5) {
                    val shiftRatio = list.size * 72f
                    val newHsl = floatArrayOf((hsl[0] + shiftRatio) % 360f, hsl[1], hsl[2])
                    val generatedColor = androidx.core.graphics.ColorUtils.HSLToColor(newHsl)
                    list.add(generatedColor)
                }
                list.take(5)
            } catch (e: Throwable) {
                Log.e("WaterViewModel", "Error in Api27Helper.getWallpaperColors", e)
                null
            }
        }
    }

    fun updateThemeMode(mode: String) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode).apply()
        triggerButtonHaptic()
    }

    fun updateAutoThemeScheduleEnabled(enabled: Boolean) {
        _autoThemeScheduleEnabled.value = enabled
        prefs.edit().putBoolean("auto_theme_schedule_enabled", enabled).apply()
        triggerButtonHaptic()
    }

    fun updateAutoThemeScheduleMode(mode: String) {
        _autoThemeScheduleMode.value = mode
        prefs.edit().putString("auto_theme_schedule_mode", mode).apply()
        triggerButtonHaptic()
    }

    fun updateAutoThemeLightStart(hour: Int, min: Int) {
        _autoThemeLightStartHour.value = hour
        _autoThemeLightStartMin.value = min
        prefs.edit().putInt("auto_theme_light_start_hour", hour).putInt("auto_theme_light_start_min", min).apply()
        triggerButtonHaptic()
    }

    fun updateAutoThemeDarkStart(hour: Int, min: Int) {
        _autoThemeDarkStartHour.value = hour
        _autoThemeDarkStartMin.value = min
        prefs.edit().putInt("auto_theme_dark_start_hour", hour).putInt("auto_theme_dark_start_min", min).apply()
        triggerButtonHaptic()
    }

    fun isDarkBySchedule(
        mode: String = _autoThemeScheduleMode.value,
        lightStartHour: Int = _autoThemeLightStartHour.value,
        lightStartMin: Int = _autoThemeLightStartMin.value,
        darkStartHour: Int = _autoThemeDarkStartHour.value,
        darkStartMin: Int = _autoThemeDarkStartMin.value
    ): Boolean {
        val cal = java.util.Calendar.getInstance()
        val nowMins = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)

        if (mode == "SUNSET_SUNRISE") {
            val dateStr = getCurrentDateString()
            val sunsetStr = prefs.getString("weather_sunset_$dateStr", null)
            val sunriseStr = prefs.getString("weather_sunrise_$dateStr", null)

            var sunriseMins = 6 * 60 + 30 // Default 06:30
            var sunsetMins = 20 * 60     // Default 20:00

            if (sunriseStr != null && sunriseStr.contains("T")) {
                val timePart = sunriseStr.substringAfter("T")
                val parts = timePart.split(":")
                if (parts.size >= 2) {
                    val h = parts[0].toIntOrNull() ?: 6
                    val m = parts[1].toIntOrNull() ?: 30
                    sunriseMins = h * 60 + m
                }
            }
            if (sunsetStr != null && sunsetStr.contains("T")) {
                val timePart = sunsetStr.substringAfter("T")
                val parts = timePart.split(":")
                if (parts.size >= 2) {
                    val h = parts[0].toIntOrNull() ?: 20
                    val m = parts[1].toIntOrNull() ?: 0
                    sunsetMins = h * 60 + m
                }
            }

            return if (sunsetMins > sunriseMins) {
                nowMins < sunriseMins || nowMins >= sunsetMins
            } else {
                nowMins in sunsetMins..<sunriseMins
            }
        } else {
            val lightMins = lightStartHour * 60 + lightStartMin
            val darkMins = darkStartHour * 60 + darkStartMin

            return if (darkMins > lightMins) {
                nowMins < lightMins || nowMins >= darkMins
            } else {
                nowMins in darkMins..<lightMins
            }
        }
    }

    fun updateSettingsDividerStyle(style: String) {
        _settingsDividerStyle.value = style
        prefs.edit().putString("settings_divider_style", style).apply()
        triggerButtonHaptic()
    }

    fun updateGoalLineSquiggly(enabled: Boolean) {
        _goalLineSquiggly.value = enabled
        prefs.edit().putBoolean("goal_line_squiggly", enabled).apply()
        triggerButtonHaptic()
    }

    fun updateSettingsDividerContrast(contrast: String) {
        _settingsDividerContrast.value = contrast
        prefs.edit().putString("settings_divider_contrast", contrast).apply()
        triggerButtonHaptic()
    }

    fun updateSettingsGapScale(scale: Float) {
        _settingsGapScale.value = scale
        prefs.edit().putFloat("settings_gap_scale", scale).apply()
    }

    fun updateSettingsAdaptiveGrouping(enabled: Boolean) {
        _settingsAdaptiveGrouping.value = enabled
        prefs.edit().putBoolean("settings_adaptive_grouping", enabled).apply()
        triggerButtonHaptic()
    }

    fun updateSettingsAdaptiveCornerRadius(radius: Int) {
        _settingsAdaptiveCornerRadius.value = radius
        prefs.edit().putInt("settings_adaptive_corner_radius", radius).apply()
    }

    fun updateOledMode(enabled: Boolean) {
        _oledModeEnabled.value = enabled
        prefs.edit().putBoolean("oled_mode_enabled", enabled).apply()
    }

    fun updateVividLightBoxesEnabled(enabled: Boolean) {
        _vividLightBoxesEnabled.value = enabled
        prefs.edit().putBoolean("vivid_light_boxes_enabled", enabled).apply()
        triggerButtonHaptic()
    }

    fun updateKeepLightBackgroundWhite(enabled: Boolean) {
        _keepLightBackgroundWhite.value = enabled
        prefs.edit().putBoolean("keep_light_background_white", enabled).apply()
        triggerButtonHaptic()
    }

    fun updateKeepCircleInsideOpaque(enabled: Boolean) {
        _keepCircleInsideOpaque.value = enabled
        prefs.edit().putBoolean("keep_circle_inside_opaque", enabled).apply()
        triggerButtonHaptic()
    }

    fun updateBoxBgOledEnabled(enabled: Boolean) {
        _boxBgOledEnabled.value = enabled
        prefs.edit().putBoolean("box_bg_oled_enabled", enabled).apply()
        triggerButtonHaptic()
    }

    fun updateBoxBgSource(source: String) {
        _boxBgSource.value = source
        prefs.edit().putString("box_bg_source", source).apply()
        triggerButtonHaptic()
    }

    fun updateBoxBgFixedIndex(index: Int) {
        _boxBgFixedIndex.value = index
        prefs.edit().putInt("box_bg_fixed_index", index).apply()
        triggerButtonHaptic()
    }

    fun updateBoxBgR(r: Float) {
        _boxBgR.value = r
        prefs.edit().putFloat("box_bg_r", r).apply()
    }

    fun updateBoxBgG(g: Float) {
        _boxBgG.value = g
        prefs.edit().putFloat("box_bg_g", g).apply()
    }

    fun updateBoxBgB(b: Float) {
        _boxBgB.value = b
        prefs.edit().putFloat("box_bg_b", b).apply()
    }

    fun updateBoxBgPaletteChoice(choice: Int) {
        _boxBgPaletteChoice.value = choice
        prefs.edit().putInt("box_bg_palette_choice", choice).apply()
        triggerButtonHaptic()
    }

    fun updateFrostedGlassEnabled(enabled: Boolean) {
        _isFrostedGlassEnabled.value = enabled
        prefs.edit().putBoolean("frosted_glass_enabled", enabled).apply()
    }

    fun updateFrostedGlassTransparency(alpha: Float) {
        _frostedGlassTransparency.value = alpha
        prefs.edit().putFloat("frosted_glass_transparency", alpha).apply()
    }

    fun updateTransparentComponentsEnabled(enabled: Boolean) {
        _transparentComponentsEnabled.value = enabled
        prefs.edit().putBoolean("transparent_components_enabled", enabled).apply()
    }

    fun updateComponentsTransparency(alpha: Float) {
        _componentsTransparency.value = alpha
        prefs.edit().putFloat("components_transparency", alpha).apply()
    }

    fun updateLightModeDarkTextEnabled(enabled: Boolean) {
        _lightModeDarkTextEnabled.value = enabled
        prefs.edit().putBoolean("light_mode_dark_text_enabled", enabled).apply()
    }

    fun updateMaterialShapesEnabled(enabled: Boolean) {
        _materialShapesEnabled.value = enabled
        prefs.edit().putBoolean("material_shapes_enabled", enabled).apply()
        if (enabled) {
            _auraGlowEnabled.value = false
            prefs.edit().putBoolean("aura_glow_enabled", false).apply()
        }
        triggerButtonHaptic()
    }

    fun updateAuraGlowEnabled(enabled: Boolean) {
        _auraGlowEnabled.value = enabled
        prefs.edit().putBoolean("aura_glow_enabled", enabled).apply()
        if (enabled) {
            _materialShapesEnabled.value = false
            prefs.edit().putBoolean("material_shapes_enabled", false).apply()
        }
        triggerButtonHaptic()
    }

    fun updateShapeRotationEnabled(enabled: Boolean) {
        _shapeRotationEnabled.value = enabled
        prefs.edit().putBoolean("shape_rotation_enabled", enabled).apply()
        triggerButtonHaptic()
    }

    fun updateMaterialShapesRotationSpeed(speed: Float) {
        _materialShapesRotationSpeed.value = speed
        prefs.edit().putFloat("material_shapes_rotation_speed", speed).apply()
    }

    fun updateAuraGlowRotationSpeed(speed: Float) {
        _auraGlowRotationSpeed.value = speed
        prefs.edit().putFloat("aura_glow_rotation_speed", speed).apply()
    }

    fun updateShapeMonochromeEnabled(enabled: Boolean) {
        _shapeMonochromeEnabled.value = enabled
        prefs.edit().putBoolean("shape_monochrome_enabled", enabled).apply()
        if (enabled && _shapeVariabilityEnabled.value) {
            _shapeVariabilityEnabled.value = false
            prefs.edit().putBoolean("shape_variability_enabled", false).apply()
        }
        triggerButtonHaptic()
    }

    fun updateShapeMonochromeSource(source: Int) {
        _shapeMonochromeSource.value = source
        prefs.edit().putInt("shape_monochrome_source", source).apply()
        triggerButtonHaptic()
    }

    fun updateShapeContrastMode(mode: Int) {
        _shapeContrastMode.value = mode
        prefs.edit().putInt("shape_contrast_mode", mode).apply()
    }

    fun updateBackgroundContrast(contrast: Float) {
        _backgroundContrast.value = contrast
        prefs.edit().putFloat("background_contrast", contrast).apply()
    }

    fun updateConfigProfile(profile: String) {
        _configProfile.value = profile
        prefs.edit().putString("config_profile", profile).apply()
        triggerButtonHaptic()
    }

    fun updateDaySwipeNavigationEnabled(enabled: Boolean) {
        _daySwipeNavigationEnabled.value = enabled
        prefs.edit().putBoolean("day_swipe_navigation_enabled", enabled).apply()
        triggerButtonHaptic()
    }

    fun updateDaySwipeNavigationArrowsVisible(visible: Boolean) {
        _daySwipeNavigationArrowsVisible.value = visible
        prefs.edit().putBoolean("day_swipe_navigation_arrows_visible", visible).apply()
        triggerButtonHaptic()
    }

    fun updateDayNavBarPadding(padding: Int) {
        _dayNavBarPadding.value = padding
        prefs.edit().putInt("day_nav_bar_padding", padding).apply()
    }

    fun updateDayNavBarColorMode(mode: String) {
        _dayNavBarColorMode.value = mode
        prefs.edit().putString("day_nav_bar_color_mode", mode).apply()
        triggerButtonHaptic()
    }

    fun updateShapeSaturation(valSat: Float) {
        _shapeSaturation.value = valSat
        prefs.edit().putFloat("shape_saturation", valSat).apply()
    }

    fun updateShapeUseIndependentDynamicPalette(useInd: Boolean) {
        _shapeUseIndependentDynamicPalette.value = useInd
        prefs.edit().putBoolean("shape_use_independent_dynamic_palette", useInd).apply()
    }

    fun updateShapeDynamicPaletteIndex(idx: Int) {
        _shapeDynamicPaletteIndex.value = idx
        prefs.edit().putInt("shape_dynamic_palette_index", idx).apply()
    }

    fun updateShapePaletteShiftEnabled(enabled: Boolean) {
        _shapePaletteShiftEnabled.value = enabled
        prefs.edit().putBoolean("shape_palette_shift_enabled", enabled).apply()
    }

    fun updateShapeVariabilityEnabled(enabled: Boolean) {
        _shapeVariabilityEnabled.value = enabled
        prefs.edit().putBoolean("shape_variability_enabled", enabled).apply()
        if (enabled && _shapeMonochromeEnabled.value) {
            _shapeMonochromeEnabled.value = false
            prefs.edit().putBoolean("shape_monochrome_enabled", false).apply()
        }
        triggerButtonHaptic()
    }

    fun updateShapeVariabilityMode(mode: Int) {
        _shapeVariabilityMode.value = mode
        prefs.edit().putInt("shape_variability_mode", mode).apply()
    }

    fun updateShapeRotMultA(value: Float) {
        _shapeRotMultA.value = value
        prefs.edit().putFloat("shape_rot_mult_a", value).apply()
    }

    fun updateShapeRotMultB(value: Float) {
        _shapeRotMultB.value = value
        prefs.edit().putFloat("shape_rot_mult_b", value).apply()
    }

    fun updateShapeRotMultC(value: Float) {
        _shapeRotMultC.value = value
        prefs.edit().putFloat("shape_rot_mult_c", value).apply()
    }

    fun updateShapeRotMultD(value: Float) {
        _shapeRotMultD.value = value
        prefs.edit().putFloat("shape_rot_mult_d", value).apply()
    }

    fun randomizeShapeRotations() {
        val options = listOf(-1.5f, -1.0f, -0.6f, 0f, 0.6f, 1.0f, 1.5f)
        updateShapeRotMultA(options.random())
        updateShapeRotMultB(options.random())
        updateShapeRotMultC(options.random())
        updateShapeRotMultD(options.random())
        triggerButtonHaptic()
    }

    fun toggleFreezeAllShapes() {
        val currA = _shapeRotMultA.value
        val currB = _shapeRotMultB.value
        val currC = _shapeRotMultC.value
        val currD = _shapeRotMultD.value

        val isAllFrozen = currA == 0f && currB == 0f && currC == 0f && currD == 0f

        if (isAllFrozen) {
            // Restore previous non-zero values
            var restA = prefs.getFloat("last_non_zero_rot_a", 1.0f)
            var restB = prefs.getFloat("last_non_zero_rot_b", -0.727f)
            var restC = prefs.getFloat("last_non_zero_rot_c", 0.533f)
            var restD = prefs.getFloat("last_non_zero_rot_d", -0.615f)

            // Fallback to standard defaults if values are invalid/0
            if (restA == 0f && restB == 0f && restC == 0f && restD == 0f) {
                restA = 1.0f
                restB = -0.727f
                restC = 0.533f
                restD = -0.615f
            }

            updateShapeRotMultA(restA)
            updateShapeRotMultB(restB)
            updateShapeRotMultC(restC)
            updateShapeRotMultD(restD)
            triggerButtonHaptic()
        } else {
            // Save the non-zero state before freezing
            prefs.edit()
                .putFloat("last_non_zero_rot_a", if (currA != 0f) currA else prefs.getFloat("last_non_zero_rot_a", 1.0f))
                .putFloat("last_non_zero_rot_b", if (currB != 0f) currB else prefs.getFloat("last_non_zero_rot_b", -0.727f))
                .putFloat("last_non_zero_rot_c", if (currC != 0f) currC else prefs.getFloat("last_non_zero_rot_c", 0.533f))
                .putFloat("last_non_zero_rot_d", if (currD != 0f) currD else prefs.getFloat("last_non_zero_rot_d", -0.615f))
                .apply()

            updateShapeRotMultA(0f)
            updateShapeRotMultB(0f)
            updateShapeRotMultC(0f)
            updateShapeRotMultD(0f)
            triggerWoopHaptic()
        }
    }

    fun updateShapeColorMode(shape: String, mode: Int) {
        when (shape) {
            "A" -> {
                _shapeAColorMode.value = mode
                prefs.edit().putInt("shape_a_color_mode", mode).apply()
            }
            "B" -> {
                _shapeBColorMode.value = mode
                prefs.edit().putInt("shape_b_color_mode", mode).apply()
            }
            "C" -> {
                _shapeCColorMode.value = mode
                prefs.edit().putInt("shape_c_color_mode", mode).apply()
            }
            "D" -> {
                _shapeDColorMode.value = mode
                prefs.edit().putInt("shape_d_color_mode", mode).apply()
            }
        }
    }

    fun updateShapeRGB(shape: String, r: Float, g: Float, b: Float) {
        when (shape) {
            "A" -> {
                _shapeAColorR.value = r
                _shapeAColorG.value = g
                _shapeAColorB.value = b
                prefs.edit().putFloat("shape_a_color_r", r).putFloat("shape_a_color_g", g).putFloat("shape_a_color_b", b).apply()
                updateShapeColorMode("A", 3)
            }
            "B" -> {
                _shapeBColorR.value = r
                _shapeBColorG.value = g
                _shapeBColorB.value = b
                prefs.edit().putFloat("shape_b_color_r", r).putFloat("shape_b_color_g", g).putFloat("shape_b_color_b", b).apply()
                updateShapeColorMode("B", 3)
            }
            "C" -> {
                _shapeCColorR.value = r
                _shapeCColorG.value = g
                _shapeCColorB.value = b
                prefs.edit().putFloat("shape_c_color_r", r).putFloat("shape_c_color_g", g).putFloat("shape_c_color_b", b).apply()
                updateShapeColorMode("C", 3)
            }
            "D" -> {
                _shapeDColorR.value = r
                _shapeDColorG.value = g
                _shapeDColorB.value = b
                prefs.edit().putFloat("shape_d_color_r", r).putFloat("shape_d_color_g", g).putFloat("shape_d_color_b", b).apply()
                updateShapeColorMode("D", 3)
            }
        }
    }

    fun randomizeShapeColors() {
        val random = java.util.Random()
        
        val rA = random.nextFloat() * 255f
        val gA = random.nextFloat() * 255f
        val bA = random.nextFloat() * 255f
        _shapeAColorMode.value = 3
        _shapeAColorR.value = rA
        _shapeAColorG.value = gA
        _shapeAColorB.value = bA
        
        val rB = random.nextFloat() * 255f
        val gB = random.nextFloat() * 255f
        val bB = random.nextFloat() * 255f
        _shapeBColorMode.value = 3
        _shapeBColorR.value = rB
        _shapeBColorG.value = gB
        _shapeBColorB.value = bB

        val rC = random.nextFloat() * 255f
        val gC = random.nextFloat() * 255f
        val bC = random.nextFloat() * 255f
        _shapeCColorMode.value = 3
        _shapeCColorR.value = rC
        _shapeCColorG.value = gC
        _shapeCColorB.value = bC

        val rD = random.nextFloat() * 255f
        val gD = random.nextFloat() * 255f
        val bD = random.nextFloat() * 255f
        _shapeDColorMode.value = 3
        _shapeDColorR.value = rD
        _shapeDColorG.value = gD
        _shapeDColorB.value = bD

        prefs.edit()
            .putInt("shape_a_color_mode", 3)
            .putFloat("shape_a_color_r", rA).putFloat("shape_a_color_g", gA).putFloat("shape_a_color_b", bA)
            .putInt("shape_b_color_mode", 3)
            .putFloat("shape_b_color_r", rB).putFloat("shape_b_color_g", gB).putFloat("shape_b_color_b", bB)
            .putInt("shape_c_color_mode", 3)
            .putFloat("shape_c_color_r", rC).putFloat("shape_c_color_g", gC).putFloat("shape_c_color_b", bC)
            .putInt("shape_d_color_mode", 3)
            .putFloat("shape_d_color_r", rD).putFloat("shape_d_color_g", gD).putFloat("shape_d_color_b", bD)
            .apply()

        triggerButtonHaptic()
    }

    fun resetShapeColors() {
        _shapeAColorMode.value = 0
        _shapeBColorMode.value = 1
        _shapeCColorMode.value = 2
        _shapeDColorMode.value = 0
        
        prefs.edit()
            .putInt("shape_a_color_mode", 0)
            .putInt("shape_b_color_mode", 1)
            .putInt("shape_c_color_mode", 2)
            .putInt("shape_d_color_mode", 0)
            .apply()
            
        triggerButtonHaptic()
    }

    fun updateShapeCustomR(r: Float) {
        _shapeCustomR.value = r
        prefs.edit().putFloat("shape_custom_r", r).apply()
    }

    fun updateShapeCustomG(g: Float) {
        _shapeCustomG.value = g
        prefs.edit().putFloat("shape_custom_g", g).apply()
    }

    fun updateShapeCustomB(b: Float) {
        _shapeCustomB.value = b
        prefs.edit().putFloat("shape_custom_b", b).apply()
    }

    fun updateProgressGlowEnabled(enabled: Boolean) {
        _progressGlowEnabled.value = enabled
        prefs.edit().putBoolean("progress_glow_enabled", enabled).apply()
        triggerButtonHaptic()
    }

    fun updateProgressCircleCardBgRemoved(removed: Boolean) {
        _isProgressCircleCardBgRemoved.value = removed
        prefs.edit().putBoolean("remove_progress_circle_card_bg", removed).apply()
        triggerButtonHaptic()
    }

    fun updateGeminiApiKey(key: String) {
        val trimmed = key.trim()
        _knownInvalidGeminiKeys.remove(trimmed)
        _geminiApiKey.value = trimmed
        prefs.edit().putString("gemini_api_key", trimmed).apply()
    }

    fun updateDeepseekApiKey(key: String) {
        _deepseekApiKey.value = key
        prefs.edit().putString("deepseek_api_key", key).apply()
    }

    fun updateOpenaiApiKey(key: String) {
        _openaiApiKey.value = key
        prefs.edit().putString("openai_api_key", key).apply()
    }

    fun updateQuickAddAmount(amount: Int) {
        _quickAddAmount.value = amount
        prefs.edit().putInt("quick_add_amount", amount).apply()
        triggerWearOsSync()
    }

    private val _bounceStiffness = MutableStateFlow(prefs.getFloat("bounce_stiffness", 300f))
    val bounceStiffness: StateFlow<Float> = _bounceStiffness.asStateFlow()
    private val _bounceDamping = MutableStateFlow(prefs.getFloat("bounce_damping", 0.55f))
    val bounceDamping: StateFlow<Float> = _bounceDamping.asStateFlow()

    private val _fadeStiffness = MutableStateFlow(prefs.getFloat("fade_stiffness", 300f))
    val fadeStiffness: StateFlow<Float> = _fadeStiffness.asStateFlow()
    private val _fadeDamping = MutableStateFlow(prefs.getFloat("fade_damping", 1.0f))
    val fadeDamping: StateFlow<Float> = _fadeDamping.asStateFlow()

    private val _slideStiffness = MutableStateFlow(prefs.getFloat("slide_stiffness", 300f))
    val slideStiffness: StateFlow<Float> = _slideStiffness.asStateFlow()
    private val _slideDamping = MutableStateFlow(prefs.getFloat("slide_damping", 1.0f))
    val slideDamping: StateFlow<Float> = _slideDamping.asStateFlow()

    fun updateBounceStiffness(stiffness: Float) { _bounceStiffness.value = stiffness; prefs.edit().putFloat("bounce_stiffness", stiffness).apply() }
    fun updateBounceDamping(damping: Float) { _bounceDamping.value = damping; prefs.edit().putFloat("bounce_damping", damping).apply() }
    fun updateFadeStiffness(stiffness: Float) { _fadeStiffness.value = stiffness; prefs.edit().putFloat("fade_stiffness", stiffness).apply() }
    fun updateFadeDamping(damping: Float) { _fadeDamping.value = damping; prefs.edit().putFloat("fade_damping", damping).apply() }
    fun updateSlideStiffness(stiffness: Float) { _slideStiffness.value = stiffness; prefs.edit().putFloat("slide_stiffness", stiffness).apply() }
    fun updateSlideDamping(damping: Float) { _slideDamping.value = damping; prefs.edit().putFloat("slide_damping", damping).apply() }

    fun resetAnimationsToDefault() {
        updateBounceStiffness(300f)
        updateBounceDamping(0.55f)
        updateFadeStiffness(300f)
        updateFadeDamping(1.0f)
        updateSlideStiffness(300f)
        updateSlideDamping(1.0f)
        updateParticleCount(800)
    }

    fun updateTabTransitionMode(mode: Int) {
        _tabTransitionMode.value = mode
        prefs.edit().putInt("tab_transition_mode", mode).apply()
    }

    fun updateConfettiStyle(mode: Int) {
        _confettiStyle.value = mode
        prefs.edit().putInt("confetti_style", mode).apply()
    }

    fun updateCustomMix(rect: Boolean, hearts: Boolean, stars: Boolean, drops: Boolean, planeTree: Boolean, regularLeaf: Boolean) {
        _customMixRectangles.value = rect
        _customMixHearts.value = hearts
        _customMixStars.value = stars
        _customMixDrops.value = drops
        _customMixPlaneTreeLeaves.value = planeTree
        _customMixRegularLeaves.value = regularLeaf
        prefs.edit()
            .putBoolean("custom_mix_rect", rect)
            .putBoolean("custom_mix_hearts", hearts)
            .putBoolean("custom_mix_stars", stars)
            .putBoolean("custom_mix_drops", drops)
            .putBoolean("custom_mix_plane_tree", planeTree)
            .putBoolean("custom_mix_regular_leaf", regularLeaf)
            .apply()
    }

    fun updateConfettiFlowStyle(mode: Int) {
        _confettiFlowStyle.value = mode
        prefs.edit().putInt("confetti_flow_style", mode).apply()
    }

    fun updateParticleCount(count: Int) {
        _particleCount.value = count
        prefs.edit().putInt("particle_count", count).apply()
    }

    fun updateIsSwipeTabNavEnabled(enabled: Boolean) {
        _isSwipeTabNavEnabled.value = enabled
        prefs.edit().putBoolean("is_swipe_tab_nav_enabled", enabled).apply()
    }

    fun updateAiProvider(provider: String) {
        _aiProvider.value = provider
        prefs.edit().putString("ai_provider", provider).apply()
    }

    fun updateTitleFaceStyle(style: Int) {
        _titleFaceStyle.value = style
        prefs.edit().putInt("title_face_style", style).apply()
    }

    fun updateTitleFacePosition(position: Int) {
        _titleFacePosition.value = position
        prefs.edit().putInt("title_face_position", position).apply()
    }

    fun updateTitleFaceVividness(vividness: Int) {
        _titleFaceVividness.value = vividness
        prefs.edit().putInt("title_face_vividness", vividness).apply()
    }

    fun updateTitleFacePaletteRole(role: String) {
        _titleFacePaletteRole.value = role
        prefs.edit().putString("title_face_palette_role", role).apply()
    }

    fun updateAppLanguage(languageCode: String) {
        _appLanguage.value = languageCode
        prefs.edit().putString("app_language", languageCode).apply()
        
        try {
            if (languageCode == "el") {
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags("el")
                )
            } else {
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags("en")
                )
            }
        } catch (e: Throwable) {
            Log.e("WaterViewModel", "Failed to update layout locales via AppCompatDelegate: ${e.message}", e)
        }
    }

    fun updateGeminiModel(model: String) {
        _geminiModel.value = model
        prefs.edit().putString("gemini_model", model).apply()
        triggerButtonHaptic()
    }

    fun updateInsightGeminiModel(model: String) {
        _insightGeminiModel.value = model
        prefs.edit().putString("insight_gemini_model", model).apply()
        triggerButtonHaptic()
    }

    fun updateAudioGeminiModel(model: String) {
        _audioGeminiModel.value = model
        prefs.edit().putString("audio_gemini_model", model).apply()
        triggerButtonHaptic()
    }

    fun updateTileQuickAddAmount(amount: Int) {
        _tileQuickAddAmount.value = amount
        prefs.edit().putInt("tile_quick_add_amount", amount).apply()
        triggerButtonHaptic()
        try {
            android.service.quicksettings.TileService.requestListeningState(
                getApplication(),
                android.content.ComponentName(getApplication(), com.pixelwater.app.service.WaterQuickAddTileService::class.java)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateTileDisplayMode(mode: String) {
        _tileDisplayMode.value = mode
        prefs.edit().putString("tile_display_mode", mode).apply()
        triggerButtonHaptic()
        try {
            android.service.quicksettings.TileService.requestListeningState(
                getApplication(),
                android.content.ComponentName(getApplication(), com.pixelwater.app.service.WaterQuickAddTileService::class.java)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateTileIconType(type: String) {
        _tileIconType.value = type
        prefs.edit().putString("tile_icon_type", type).apply()
        triggerButtonHaptic()
        try {
            android.service.quicksettings.TileService.requestListeningState(
                getApplication(),
                android.content.ComponentName(getApplication(), com.pixelwater.app.service.WaterQuickAddTileService::class.java)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateTileLongPressAppLaunch(enabled: Boolean) {
        _tileLongPressAppLaunch.value = enabled
        prefs.edit().putBoolean("tile_long_press_app_launch", enabled).apply()
        triggerButtonHaptic()
        try {
            val context = getApplication<android.app.Application>()
            val pm = context.packageManager
            val componentName = android.content.ComponentName(
                context,
                "com.pixelwater.app.service.WaterQuickAddTileSettingsActivity"
            )
            val newState = if (enabled) {
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            pm.setComponentEnabledSetting(
                componentName,
                newState,
                android.content.pm.PackageManager.DONT_KILL_APP
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateSystemPrompt(prompt: String) {
        _systemPrompt.value = prompt
        prefs.edit().putString("system_prompt", prompt).apply()
    }

    private val _isHapticTestActive = MutableStateFlow(false)
    val isHapticTestActive: StateFlow<Boolean> = _isHapticTestActive.asStateFlow()

    fun updateHapticsSlidersEnabled(enabled: Boolean) {
        _hapticsSlidersEnabled.value = enabled
        prefs.edit().putBoolean("haptics_sliders_enabled", enabled).apply()
    }

    fun updateHapticsButtonsEnabled(enabled: Boolean) {
        _hapticsButtonsEnabled.value = enabled
        prefs.edit().putBoolean("haptics_buttons_enabled", enabled).apply()
    }

    fun updateHapticsGoalEnabled(enabled: Boolean) {
        _hapticsGoalEnabled.value = enabled
        prefs.edit().putBoolean("haptics_goal_enabled", enabled).apply()
    }

    fun updateHapticsTogglesEnabled(enabled: Boolean) {
        _hapticsTogglesEnabled.value = enabled
        prefs.edit().putBoolean("haptics_toggles_enabled", enabled).apply()
    }

    fun updateOverrideSwipeHapticsEnabled(enabled: Boolean) {
        _overrideSwipeHapticsEnabled.value = enabled
        prefs.edit().putBoolean("override_swipe_haptics_enabled", enabled).apply()
    }

    fun updateFireworksFeedbackEnabled(enabled: Boolean) {
        _fireworksFeedbackEnabled.value = enabled
        prefs.edit().putBoolean("fireworks_feedback_enabled", enabled).apply()
    }

    private var hapticUpdateJob: kotlinx.coroutines.Job? = null

    fun updateVibrationDurationMs(ms: Int, isFromPreset: Boolean = false) {
        _vibrationDurationMs.value = ms
        scheduleHapticUpdate(isFromPreset)
    }

    fun updateVibrationGapMs(ms: Int, isFromPreset: Boolean = false) {
        _vibrationGapMs.value = ms
        scheduleHapticUpdate(isFromPreset)
    }

    fun updateVibrationStrength(strength: Int, isFromPreset: Boolean = false) {
        _vibrationStrength.value = strength
        scheduleHapticUpdate(isFromPreset)
    }

    private fun scheduleHapticUpdate(isFromPreset: Boolean) {
        hapticUpdateJob?.cancel()
        if (!isFromPreset) {
            if (_vibrationPatternPreset.value != VibrationPatternPreset.CUSTOM) {
                _vibrationPatternPreset.value = VibrationPatternPreset.CUSTOM
            }
        }
        hapticUpdateJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            kotlinx.coroutines.delay(100)
            prefs.edit()
                .putInt("vibration_duration_ms", _vibrationDurationMs.value)
                .putInt("vibration_gap_ms", _vibrationGapMs.value)
                .putInt("vibration_strength", _vibrationStrength.value)
                .putString("vibration_pattern_preset", _vibrationPatternPreset.value.name)
                .apply()
            
            if (_isHapticTestActive.value) {
                startContinuousHapticTest()
            }
        }
    }

    fun updateVibrationPatternPreset(preset: VibrationPatternPreset) {
        _vibrationPatternPreset.value = preset
        prefs.edit().putString("vibration_pattern_preset", preset.name).apply()
        
        // Sync sliders with preset values
        when (preset) {
            VibrationPatternPreset.SINGLE_PULSE -> {
                updateVibrationDurationMs(60, true)
                updateVibrationGapMs(0, true)
                updateVibrationStrength(100, true)
            }
            VibrationPatternPreset.DOUBLE_PULSE -> {
                updateVibrationDurationMs(35, true)
                updateVibrationGapMs(45, true)
                updateVibrationStrength(100, true)
            }
            VibrationPatternPreset.TRIPLE_PULSE -> {
                updateVibrationDurationMs(25, true)
                updateVibrationGapMs(35, true)
                updateVibrationStrength(100, true)
            }
            VibrationPatternPreset.HEARTBEAT -> {
                updateVibrationDurationMs(20, true)
                updateVibrationGapMs(30, true)
                updateVibrationStrength(90, true)
            }
            VibrationPatternPreset.VIBRANT_WAVE -> {
                updateVibrationDurationMs(45, true)
                updateVibrationGapMs(15, true)
                updateVibrationStrength(100, true)
            }
            VibrationPatternPreset.CUSTOM -> {
                // Do nothing, let values persist as they are
            }
        }

        if (_isHapticTestActive.value) {
            startContinuousHapticTest()
        } else {
            triggerPresetPreview(preset)
        }
    }

    fun updateNavbarCornerRadius(radius: Int) {
        _navbarCornerRadius.value = radius
        prefs.edit().putInt("navbar_corner_radius", radius).apply()
    }

    fun updateGeneralCornerRadius(radius: Int) {
        _generalCornerRadius.value = radius
        prefs.edit().putInt("general_corner_radius", radius).apply()
    }

    fun updateShowOutlines(show: Boolean) {
        _showOutlines.value = show
        prefs.edit().putBoolean("show_outlines", show).apply()
    }

    fun updateOutlineWidth(width: Int) {
        val w = width.coerceIn(1, 10)
        _outlineWidth.value = w
        prefs.edit().putInt("outline_width", w).apply()
    }

    fun updateTextContrastMode(mode: Int) {
        _textContrastMode.value = mode
        prefs.edit().putInt("text_contrast_mode", mode).apply()
    }

    fun updateFontSizeMode(mode: Int) {
        _fontSizeMode.value = mode
        prefs.edit().putInt("font_size_mode", mode).apply()
    }

    fun updateTextFontMode(mode: Int) {
        _textFontMode.value = mode
        prefs.edit().putInt("text_font_mode", mode).apply()
    }

    fun updateColorContrastMode(mode: Int) {
        _colorContrastMode.value = mode
        prefs.edit().putInt("color_contrast_mode", mode).apply()
    }

    private fun triggerPresetPreview(preset: VibrationPatternPreset) {
        val duration = _vibrationDurationMs.value
        val gap = _vibrationGapMs.value
        val strength = _vibrationStrength.value
        val (timings, strengths) = getPatternData(preset, duration, gap, strength)
        vibratePattern(timings, strengths)
    }

    private var fireworkTestJob: Job? = null
    private val _isFireworkHapticTestActive = MutableStateFlow(false)
    val isFireworkHapticTestActive: StateFlow<Boolean> = _isFireworkHapticTestActive.asStateFlow()

    fun startContinuousFireworkHapticTest() {
        if (_isFireworkHapticTestActive.value) return
        _isFireworkHapticTestActive.value = true
        fireworkTestJob?.cancel()
        fireworkTestJob = viewModelScope.launch {
            while (isActive) {
                try {
                    triggerFireworksHaptic()
                    // Fireworks pattern naturally takes around 1500-2000ms. Wait 2500ms before looping.
                    delay(2500)
                } catch (e: CancellationException) {
                    break
                }
            }
            _isFireworkHapticTestActive.value = false
        }
    }

    fun stopContinuousFireworkHapticTest() {
        _isFireworkHapticTestActive.value = false
        fireworkTestJob?.cancel()
        try {
            getVibrator()?.cancel()
        } catch (e: Throwable) {
            Log.e("WaterViewModel", "Error canceling firework test", e)
        }
    }

    fun resetFireworkHapticsToDefault() {
        updateFireworkPatternPreset(VibrationPatternPreset.TRIPLE_PULSE)
        _fireworkDurationMs.value = 100
        _fireworkGapMs.value = 50
        _fireworkStrength.value = 50
        prefs.edit()
            .putInt("firework_duration", 100)
            .putInt("firework_gap", 50)
            .putInt("firework_strength", 50)
            .apply()
    }

    fun getPatternData(preset: VibrationPatternPreset, duration: Int, gap: Int, strength: Int): Pair<LongArray, IntArray> {
        val d = duration.toLong()
        val g = gap.toLong()
        val s = strength
        return when (preset) {
            VibrationPatternPreset.SINGLE_PULSE -> Pair(
                longArrayOf(0, d),
                intArrayOf(0, s)
            )
            VibrationPatternPreset.DOUBLE_PULSE -> Pair(
                longArrayOf(0, d, g, d),
                intArrayOf(0, s, 0, s)
            )
            VibrationPatternPreset.TRIPLE_PULSE -> Pair(
                longArrayOf(0, d, g, d, g, d),
                intArrayOf(0, s, 0, s, 0, s)
            )
            VibrationPatternPreset.HEARTBEAT -> Pair(
                longArrayOf(0, d / 2, g, d, g * 2, d / 2, g, d),
                intArrayOf(0, s / 2, 0, s, 0, s / 2, 0, s)
            )
            VibrationPatternPreset.VIBRANT_WAVE -> Pair(
                longArrayOf(0, d, g, d, g, d),
                intArrayOf(0, (s * 0.4f).toInt().coerceIn(1, 100), 0, (s * 0.7f).toInt().coerceIn(1, 100), 0, s)
            )
            VibrationPatternPreset.CUSTOM -> Pair(
                longArrayOf(0, d, g, d),
                intArrayOf(0, s, 0, s)
            )
        }
    }

    fun resetHapticsToDefault() {
        updateVibrationDurationMs(27)
        updateVibrationGapMs(10)
        updateVibrationStrength(80)
        updateVibrationPatternPreset(VibrationPatternPreset.DOUBLE_PULSE)
    }

    fun triggerButtonHaptic(view: View? = null) {
        if (_hapticsButtonsEnabled.value) {
            HapticManager.triggerButtonHaptic(getApplication(), view, _vibrationDurationMs.value, _vibrationStrength.value)
        }
    }

    fun triggerTabClickHaptic(index: Int, view: View? = null) {
        if (_hapticsButtonsEnabled.value) {
            HapticManager.triggerTabClickHaptic(getApplication(), view, index, _vibrationDurationMs.value, _vibrationStrength.value)
        }
    }

    fun triggerWoopHaptic(view: View? = null) {
        if (_hapticsButtonsEnabled.value) {
            HapticManager.triggerWoopHaptic(getApplication(), view, _vibrationDurationMs.value, _vibrationStrength.value)
        }
    }

    fun triggerContinuousLightHaptic(view: View? = null) {
        if (_hapticsSlidersEnabled.value) {
            HapticManager.triggerContinuousLightHaptic(getApplication(), view, _vibrationStrength.value)
        }
    }

    fun triggerScrollHaptic(view: View? = null) {
        HapticManager.triggerScrollHaptic(getApplication(), view)
    }

    fun triggerWearHaptic(type: String = "CLICK") {
        val hapticStrength = _wearHapticStrength.value
        if (hapticStrength == "NONE") return

        val vibrator = getVibrator() ?: return

        try {
            if ((type == "CLICK" || type == "COMPLICATION_CLICK") && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                vibrator.vibrate(android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_CLICK))
                return
            }

            val durationMs = when (hapticStrength) {
                "LIGHT" -> when (type) {
                    "ROTARY" -> 4L
                    "LONG_PRESS" -> 15L
                    "SWIPE" -> 8L
                    else -> 6L // CLICK
                }
                "MEDIUM" -> when (type) {
                    "ROTARY" -> 10L
                    "LONG_PRESS" -> 40L
                    "SWIPE" -> 20L
                    else -> 15L // CLICK
                }
                "STRONG" -> when (type) {
                    "ROTARY" -> 20L
                    "LONG_PRESS" -> 80L
                    "SWIPE" -> 35L
                    else -> 30L // CLICK
                }
                else -> 10L // Default MEDIUM
            }

            val amplitude = when (hapticStrength) {
                "LIGHT" -> 60
                "MEDIUM" -> 150
                "STRONG" -> 255
                else -> 150
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(durationMs, amplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun triggerSliderHaptic(view: View? = null) {
        if (_hapticsSlidersEnabled.value) {
            val duration = _vibrationDurationMs.value
            val gap = _vibrationGapMs.value
            val strength = _vibrationStrength.value
            val preset = _vibrationPatternPreset.value
            val (timings, strengths) = getPatternData(preset, duration, gap, strength)
            HapticManager.triggerSliderHaptic(getApplication(), view, timings, strengths)
        }
    }

    fun triggerToggleSnapHaptic(view: View? = null, reversed: Boolean = false) {
        if (_hapticsTogglesEnabled.value) {
            HapticManager.triggerToggleSnapHaptic(getApplication(), view, _vibrationDurationMs.value, _vibrationStrength.value, reversed)
        }
    }

    fun triggerToggleLightHaptic(view: View? = null, reversed: Boolean = false) {
        if (_hapticsTogglesEnabled.value) {
            HapticManager.triggerToggleLightHaptic(getApplication(), view, _vibrationStrength.value, reversed)
        }
    }

    fun triggerToggleHaptic(view: View? = null, reversed: Boolean = false) {
        if (_hapticsTogglesEnabled.value) {
            val duration = _vibrationDurationMs.value
            val gap = _vibrationGapMs.value
            val strength = _vibrationStrength.value
            val preset = _vibrationPatternPreset.value
            var (timings, strengths) = getPatternData(preset, duration, gap, strength)
            if (reversed) {
                val activeIndices = strengths.indices.filter { it % 2 == 1 }
                val activeStrengths = activeIndices.map { strengths[it] }
                val reversedActiveStrengths = activeStrengths.reversed()
                val newStrengths = strengths.clone()
                for (i in activeIndices.indices) {
                    newStrengths[activeIndices[i]] = reversedActiveStrengths[i]
                }
                strengths = newStrengths

                val activeTimingsIndices = timings.indices.filter { it % 2 == 1 }
                val activeTimings = activeTimingsIndices.map { timings[it] }
                val reversedActiveTimings = activeTimings.reversed()
                val newTimings = timings.clone()
                for (i in activeTimingsIndices.indices) {
                    newTimings[activeTimingsIndices[i]] = reversedActiveTimings[i]
                }
                timings = newTimings
            }
            HapticManager.triggerToggleHaptic(getApplication(), view, timings, strengths, reversed)
        }
    }

    fun triggerTitleFaceUnlatchHaptic(view: View? = null) {
        if (_hapticsTogglesEnabled.value) {
            HapticManager.triggerUnlatchHaptic(getApplication(), view, _vibrationStrength.value)
        }
    }

    fun triggerTitleFaceTensionHaptic(view: View? = null) {
        if (_hapticsTogglesEnabled.value) {
            HapticManager.triggerTensionHaptic(getApplication(), view, _vibrationStrength.value)
        }
    }

    fun triggerGoalHaptic(view: View? = null) {
        if (_hapticsGoalEnabled.value) {
            val duration = _vibrationDurationMs.value
            val gap = _vibrationGapMs.value
            val maxStrength = _vibrationStrength.value
            
            val numRepeats = 5
            val allTimings = mutableListOf<Long>()
            val allStrengths = mutableListOf<Int>()
            
            val gapBetweenRepeats = (gap * 3).toLong().coerceAtLeast(80L)
            
            for (i in 1..numRepeats) {
                val currentStrength = (maxStrength * (i.toFloat() / numRepeats)).toInt().coerceIn(1, 100)
                val (timings, strengths) = getPatternData(_vibrationPatternPreset.value, duration, gap, currentStrength)
                
                for (j in timings.indices) {
                    if (j == 0) {
                        if (i == 1) {
                            allTimings.add(timings[0])
                            allStrengths.add(strengths[0])
                        } else {
                            allTimings.add(gapBetweenRepeats)
                            allStrengths.add(0)
                        }
                    } else {
                        allTimings.add(timings[j])
                        allStrengths.add(strengths[j])
                    }
                }
            }
            HapticManager.triggerGoalHaptic(getApplication(), view, allTimings.toLongArray(), allStrengths.toIntArray())
        }
    }

    fun triggerFireworksHaptic(view: View? = null) {
        if (!_fireworksFeedbackEnabled.value) return
        val scale = _animationHapticStrength.value / 100f
        val allTimings = mutableListOf<Long>()
        val allStrengths = mutableListOf<Int>()
        
        if (_customFireworkHapticsEnabled.value) {
            val duration = _fireworkDurationMs.value
            val gap = _fireworkGapMs.value
            val baseStrength = _fireworkStrength.value
            val preset = _fireworkPatternPreset.value
            val (baseTimings, basePatternStrengths) = getPatternData(preset, duration, gap, baseStrength)
            
            // Stage 1: Rise (Shrill ascending hum)
            allTimings.add(0); allStrengths.add(0)
            allTimings.add(10); allStrengths.add((15 * scale).toInt())
            allTimings.add(20); allStrengths.add((30 * scale).toInt())
            allTimings.add(40); allStrengths.add((50 * scale).toInt())
            allTimings.add(80); allStrengths.add((75 * scale).toInt())
            allTimings.add(300); allStrengths.add(0) // Silent rise
            
            // Stage 2 & 3: Secondary pops (popping effect using user's pattern)
            for (burst in 0..2) {
                val burstScale = scale * (1.0f - burst * 0.25f)
                for (p in 0 until baseTimings.size step 2) {
                    val pDuration = baseTimings[p]
                    val pGap = if (p + 1 < baseTimings.size) baseTimings[p + 1] else 0L
                    allTimings.add(pDuration)
                    allStrengths.add((basePatternStrengths[p] * burstScale).toInt().coerceAtMost(100))
                    if (pGap > 0) {
                        allTimings.add(pGap)
                        allStrengths.add(0)
                    }
                }
                // gap between pops
                allTimings.add((60..100).random().toLong()); allStrengths.add(0)
            }
            
            // Stage 4: Crackling Fizzle
            for (i in 0..6) {
                val randomDuration = (10..30).random().toLong()
                val randomGap = (30..70).random().toLong()
                val randomStrength = (35..65).random()
                val fizzleScale = scale * 0.7f
                allTimings.add(randomDuration); allStrengths.add((randomStrength * fizzleScale).toInt().coerceAtMost(100))
                allTimings.add(randomGap); allStrengths.add(0)
            }
            
            // Stage 5: Final Spike using preset pattern
            allTimings.add(100); allStrengths.add(0)
            for (p in 0 until baseTimings.size step 2) {
                val pDuration = baseTimings[p]
                val pGap = if (p + 1 < baseTimings.size) baseTimings[p + 1] else 0L
                allTimings.add(pDuration)
                allStrengths.add((basePatternStrengths[p] * scale * 1.5f).toInt().coerceAtMost(100))
                if (pGap > 0) {
                    allTimings.add(pGap)
                    allStrengths.add(0)
                }
            }
        } else {
            // Stage 1: The Rising Launch (450ms)
            allTimings.add(0); allStrengths.add(0)
            allTimings.add(10); allStrengths.add((15 * scale).toInt())
            allTimings.add(20); allStrengths.add((30 * scale).toInt())
            allTimings.add(40); allStrengths.add((50 * scale).toInt())
            allTimings.add(80); allStrengths.add((75 * scale).toInt())
            allTimings.add(300); allStrengths.add(0) // Silent rise
            
            // Stage 2: Primary Boom (100ms)
            allTimings.add(40); allStrengths.add((100 * scale).toInt())
            allTimings.add(60); allStrengths.add(0)
            
            // Stage 3: Secondary Bursts / Expansion (600ms)
            for (i in 0..3) {
                allTimings.add(20); allStrengths.add((85 * scale).toInt())
                allTimings.add(80); allStrengths.add(0)
                allTimings.add(15); allStrengths.add((70 * scale).toInt())
                allTimings.add(45); allStrengths.add(0)
            }
            
            // Stage 4: Crackling Fizzle (500ms)
            for (i in 0..6) {
                val randomDuration = (10..30).random().toLong()
                val randomGap = (30..70).random().toLong()
                val randomStrength = (35..65).random()
                allTimings.add(randomDuration); allStrengths.add((randomStrength * scale).toInt())
                allTimings.add(randomGap); allStrengths.add(0)
            }
            
            // Stage 5: THE FINAL SPIKE (350ms)
            allTimings.add(100); allStrengths.add(0)
            allTimings.add(30); allStrengths.add((40 * scale).toInt())
            allTimings.add(30); allStrengths.add((60 * scale).toInt())
            allTimings.add(30); allStrengths.add((85 * scale).toInt())
            allTimings.add(160); allStrengths.add((100 * scale).toInt())
        }
        
        HapticManager.triggerFireworksHaptic(getApplication(), view, allTimings.toLongArray(), allStrengths.toIntArray())
    }

    fun triggerConfettiBurstHaptic(view: View? = null) {
        if (!_fireworksFeedbackEnabled.value) return
        val scale = _animationHapticStrength.value / 100f
        val allTimings = mutableListOf<Long>()
        val allStrengths = mutableListOf<Int>()
        allTimings.add(0); allStrengths.add((100 * scale).toInt().coerceIn(1, 100))
        allTimings.add(50); allStrengths.add(0)
        allTimings.add(30); allStrengths.add((65 * scale).toInt().coerceIn(1, 100))
        allTimings.add(50); allStrengths.add(0)
        allTimings.add(35); allStrengths.add((100 * scale).toInt().coerceIn(1, 100))
        allTimings.add(100); allStrengths.add(0)
        val delays = intArrayOf(20, 40, 20, 60, 20, 80, 20, 100, 25, 120, 30, 140, 30, 160)
        val strengths = intArrayOf(80, 0, 70, 0, 60, 0, 50, 0, 40, 0, 30, 0, 20, 0)
        for (idx in delays.indices) {
            allTimings.add(delays[idx].toLong())
            allStrengths.add((strengths[idx] * scale).toInt().coerceIn(0, 100))
        }
        HapticManager.triggerConfettiBurstHaptic(getApplication(), view, allTimings.toLongArray(), allStrengths.toIntArray())
    }

    fun triggerSideBurstHaptic(view: View? = null) {
        if (!_fireworksFeedbackEnabled.value) return
        val scale = _animationHapticStrength.value / 100f
        val allTimings = mutableListOf<Long>()
        val allStrengths = mutableListOf<Int>()
        allTimings.add(0); allStrengths.add((100 * scale).toInt().coerceIn(1, 100))
        allTimings.add(60); allStrengths.add(0)
        allTimings.add(40); allStrengths.add((100 * scale).toInt().coerceIn(1, 100))
        allTimings.add(120); allStrengths.add(0)
        val delays = intArrayOf(30, 50, 30, 70, 30, 90, 35, 110, 40, 130)
        val strengths = intArrayOf(75, 0, 60, 0, 45, 0, 30, 0, 15, 0)
        for (idx in delays.indices) {
            allTimings.add(delays[idx].toLong())
            allStrengths.add((strengths[idx] * scale).toInt().coerceIn(0, 100))
        }
        HapticManager.triggerSideBurstHaptic(getApplication(), view, allTimings.toLongArray(), allStrengths.toIntArray())
    }

    fun triggerRainfallHaptic(view: View? = null) {
        if (!_fireworksFeedbackEnabled.value) return
        val scale = _animationHapticStrength.value / 100f
        val allTimings = mutableListOf<Long>()
        val allStrengths = mutableListOf<Int>()
        val delays = intArrayOf(10, 80, 10, 80, 10, 80, 10, 80, 15, 90, 15, 90, 15, 90, 15, 100, 20, 100, 20, 100)
        val strengths = intArrayOf(45, 0, 50, 0, 45, 0, 55, 0, 40, 0, 45, 0, 40, 0, 35, 0, 30, 0, 25, 0)
        for (idx in delays.indices) {
            allTimings.add(delays[idx].toLong())
            allStrengths.add((strengths[idx] * scale).toInt().coerceIn(0, 100))
        }
        HapticManager.triggerRainfallHaptic(getApplication(), view, allTimings.toLongArray(), allStrengths.toIntArray())
    }

    fun startContinuousHapticTest() {
        _isHapticTestActive.value = true
        val vibrator = getVibrator() ?: return
        val duration = _vibrationDurationMs.value
        val gap = _vibrationGapMs.value
        val strength = _vibrationStrength.value

        val (timings, strengths) = getPatternData(_vibrationPatternPreset.value, duration, gap, strength)

        try {
            vibrator.cancel()
            
            // Ensure array has no 0 timings except maybe the first index
            val safeTimings = timings.mapIndexed { index, t ->
                if (index == 0) t else maxOf(1L, t)
            }.toMutableList()
            val safeStrengths = strengths.toMutableList()
            
            // Always ensure there is a gap (OFF period) if we are repeating the waveform. 
            // Repeating a waveform with no OFF time or size 1 can crash the Android Vibrator HAL.
            val safeGap = maxOf(10, gap)
            safeTimings.add(safeGap.toLong())
            safeStrengths.add(0)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amplitudes = safeStrengths.map { (it / 100f * 255).toInt().coerceIn(0, 255) }.toIntArray()
                if (vibrator.hasAmplitudeControl()) {
                    vibrator.vibrate(VibrationEffect.createWaveform(safeTimings.toLongArray(), amplitudes, 0))
                } else {
                    vibrator.vibrate(VibrationEffect.createWaveform(safeTimings.toLongArray(), 0))
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(safeTimings.toLongArray(), 0)
            }
        } catch (e: Throwable) {
            Log.e("WaterViewModel", "Error in startContinuousHapticTest: ${e.message}", e)
        }
    }

    fun stopContinuousHapticTest() {
        _isHapticTestActive.value = false
        try {
            getVibrator()?.cancel()
        } catch (e: Throwable) {
            Log.e("WaterViewModel", "Error in stopContinuousHapticTest: ${e.message}", e)
        }
    }

    private fun getVibrator(): Vibrator? {
        return try {
            val context = getApplication<Application>()
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } catch (e: Throwable) {
            Log.e("WaterViewModel", "Failed to access Vibrator service: ${e.message}", e)
            null
        }
    }

    private fun vibrateSinglePulse(durationMs: Int, strengthPercent: Int) {
        if (durationMs <= 0) return
        val vibrator = getVibrator() ?: return
        val amplitude = (strengthPercent / 100f * 255).toInt().coerceIn(1, 255)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs.toLong(), amplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs.toLong())
            }
        } catch (e: Throwable) {
            Log.e("WaterViewModel", "Error in vibrateSinglePulse: ${e.message}", e)
        }
    }

    private fun vibratePattern(timings: LongArray, strengths: IntArray) {
        if (timings.isEmpty() || strengths.isEmpty() || timings.size != strengths.size) return
        
        // Android's VibrationEffect throws IllegalArgumentException for any timings == 0 (except index 0)
        val finalTimings = timings.mapIndexed { index, t ->
            if (index == 0) t else maxOf(1L, t)
        }.toLongArray()
        
        val vibrator = getVibrator() ?: return
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
            Log.e("WaterViewModel", "Error in vibratePattern: ${e.message}", e)
        }
    }

    private fun syncReminders() {
        if (_remindersEnabled.value) {
            NotificationHelper.createNotificationChannel(getApplication())
            NotificationHelper.scheduleNextReminder(getApplication())
        } else {
            NotificationHelper.cancelAlarms(getApplication())
        }
    }

    fun triggerTestNotification() {
        NotificationHelper.showReminderNotification(getApplication())
    }

    // Smart logic to calculate hydration streaks
    fun calculateStreak() {
        viewModelScope.launch {
            repository.getAllLogs().take(1).collect { allLogs ->
                if (allLogs.isEmpty()) {
                    _streak.value = 0
                    return@collect
                }

                // Group history by date
                val dailyTotals = allLogs.groupBy { it.dateString }
                    .mapValues { entry -> entry.value.sumOf { it.waterEquivalentMl } }

                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val todayStr = getCurrentDateString()
                val todayGoal = getDailyGoalForDate(todayStr)
                
                var currentStreak = 0
                val calendar = Calendar.getInstance()

                // If today's objective is completed, start streak check from today
                // Otherwise start check from yesterday
                val todayTotal = dailyTotals[todayStr] ?: 0
                if (todayTotal >= todayGoal) {
                    currentStreak++
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                } else {
                    // Check if they had logs today. If today is still partially logged, 
                    // check yesterday for a streak continuation
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                }

                // Keep counting backward as long as days met the goal
                val oldestTimestamp = allLogs.minOfOrNull { it.timestamp } ?: 0L
                val oldestCalendar = Calendar.getInstance().apply {
                    timeInMillis = oldestTimestamp
                    add(Calendar.DAY_OF_YEAR, -2) // 2 days buffer safety margin
                }
                val baseGoal = _baseDailyGoalMl.value
                var iterations = 0
                while (baseGoal > 0 && iterations < 10000 && !calendar.before(oldestCalendar)) {
                    iterations++
                    val dateKey = sdf.format(calendar.time)
                    val daysIntake = dailyTotals[dateKey] ?: 0
                    val daySpecificGoal = getDailyGoalForDate(dateKey)
                    if (daysIntake >= daySpecificGoal) {
                        currentStreak++
                        calendar.add(Calendar.DAY_OF_YEAR, -1)
                    } else {
                        break
                    }
                }

                val persistedGoalStreak = prefs.getInt("persisted_goal_streak", 0)
                val finalStreak = maxOf(currentStreak, persistedGoalStreak)
                if (currentStreak > persistedGoalStreak) {
                    prefs.edit().putInt("persisted_goal_streak", currentStreak).apply()
                }

                val oldStreak = _streak.value
                _streak.value = finalStreak

                // Calculate the latest 10-day streak milestone reached
                val earnedMilestone = (currentStreak / 10) * 10
                val lastCelebrated = prefs.getInt("last_celebrated_streak_milestone", 0)

                if (earnedMilestone >= 10 && earnedMilestone > lastCelebrated) {
                    prefs.edit().putInt("last_celebrated_streak_milestone", earnedMilestone).apply()
                } else if (earnedMilestone < lastCelebrated) {
                    // Reset tracked milestone if current streak drops or resets
                    prefs.edit().putInt("last_celebrated_streak_milestone", earnedMilestone).apply()
                }
            }
        }
    }

    fun getCurrentDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        if (_lateNightLoggingEnabled.value) {
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            if (hour < _lateNightRolloverHour.value) {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            }
        }
        return sdf.format(calendar.time)
    }

    fun getDateStringForOffset(offset: Int): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        if (_lateNightLoggingEnabled.value) {
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            if (hour < _lateNightRolloverHour.value) {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            }
        }
        calendar.add(Calendar.DAY_OF_YEAR, offset)
        return sdf.format(calendar.time)
    }

    fun getDateStringForSelectedOffset(offset: Int): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        try {
            val parsedDate = sdf.parse(_currentDate.value) ?: Date()
            calendar.time = parsedDate
        } catch (e: Exception) {
            if (_lateNightLoggingEnabled.value) {
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                if (hour < _lateNightRolloverHour.value) {
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                }
            }
        }
        calendar.add(Calendar.DAY_OF_YEAR, offset)
        return sdf.format(calendar.time)
    }

    fun getTimestampForDateString(dateStr: String): Long {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return try {
            val parsedDate = sdf.parse(dateStr) ?: Date()
            val calendar = Calendar.getInstance()
            calendar.time = parsedDate
            val now = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY))
            calendar.set(Calendar.MINUTE, now.get(Calendar.MINUTE))
            calendar.set(Calendar.SECOND, now.get(Calendar.SECOND))
            calendar.set(Calendar.MILLISECOND, now.get(Calendar.MILLISECOND))
            calendar.timeInMillis
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    fun changeDateOffset(days: Int) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        try {
            val parsedDate = sdf.parse(_currentDate.value) ?: Date()
            val calendar = Calendar.getInstance()
            calendar.time = parsedDate
            calendar.add(Calendar.DAY_OF_YEAR, days)
            _currentDate.value = sdf.format(calendar.time)
        } catch (e: Exception) {
            _currentDate.value = getCurrentDateString()
        }
    }

    fun seedMockData() {
        viewModelScope.launch {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val calendar = Calendar.getInstance()
            
            // Generate mock records for the past 30 days
            for (i in 0 until 30) {
                val dateStr = sdf.format(calendar.time)
                // Delete existing logs for this mock day first to prevent duplication / pile-up
                repository.getLogsForDate(dateStr).take(1).collect { existing ->
                    existing.forEach { repository.deleteLog(it) }
                }

                // Generates randomized target completion
                val shouldMeetGoal = (0..5).random() > 1 // 66% chance to exceed or meet goal
                val goal = dailyGoalMl.value
                val dayTarget = if (shouldMeetGoal) {
                    goal + (100..600).random()
                } else {
                    goal - (200..800).random()
                }

                var currentTotal = 0
                val logCount = (2..5).random()
                for (j in 0 until logCount) {
                    val remaining = dayTarget - currentTotal
                    if (remaining <= 0) break
                    
                    val segmentAmount = if (j == logCount - 1) {
                        remaining
                    } else {
                        val base = (remaining / (logCount - j)).coerceAtLeast(100)
                        ((base + (-50..50).random()) / 50 * 50).coerceIn(100, 1000)
                    }
                    
                    currentTotal += segmentAmount
                    // Distribute throughout the awake hours
                    val logTime = calendar.timeInMillis + (3600 * 1000 * 9) + (j * 3 * 3600 * 1000)
                    val beverages = listOf(
                        Triple("Water", 1.00f, "Water"),
                        Triple("Tea", 0.90f, "Tea"),
                        Triple("Juice", 0.85f, "Juice"),
                        Triple("Milk", 0.88f, "Milk")
                    )
                    val bev = beverages.random()
                    val log = WaterLog(
                        amountMl = segmentAmount,
                        dateString = dateStr,
                        timestamp = logTime,
                        beverageType = bev.first,
                        waterEquivalency = bev.second,
                        waterEquivalentMl = (segmentAmount * bev.second).toInt()
                    )
                    repository.insertLog(log)
                }
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            }
            calculateStreak()
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.deleteAllLogs()
            calculateStreak()
        }
    }

    private fun parseAndRegisterAiReminders(replyText: String): String {
        var processedText = replyText
        val proposals = mutableListOf<ActionProposal>()

        // 1. change_setting
        val settingPattern = java.util.regex.Pattern.compile("<change_setting>(.*?)</change_setting>", java.util.regex.Pattern.DOTALL)
        val settingMatcher = settingPattern.matcher(processedText)
        while (settingMatcher.find()) {
            val settingRaw = settingMatcher.group(1)?.trim()
            if (!settingRaw.isNullOrEmpty()) {
                try {
                    val parts = settingRaw.split("=", limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0].trim().lowercase()
                        val value = parts[1].trim()
                        proposals.add(ActionProposal("setting", mapOf("key" to key, "value" to value)))
                    }
                } catch (e: Exception) {
                    Log.e("WaterViewModel", "AI Setting Change parsing failed", e)
                }
            }
        }
        processedText = processedText.replace(Regex("<change_setting>.*?</change_setting>", RegexOption.DOT_MATCHES_ALL), "").trim()

        // 2. add_water
        val addWaterPattern = java.util.regex.Pattern.compile("<add_water>(.*?)</add_water>", java.util.regex.Pattern.DOTALL)
        val addWaterMatcher = addWaterPattern.matcher(processedText)
        while (addWaterMatcher.find()) {
            val raw = addWaterMatcher.group(1)?.trim() ?: ""
            val params = mutableMapOf<String, String>()
            if (raw.toIntOrNull() != null) {
                params["amount"] = raw
            } else {
                raw.split(",").forEach { pair ->
                    val p = pair.split("=", limit = 2)
                    if (p.size == 2) {
                        params[p[0].trim().lowercase()] = p[1].trim()
                    }
                }
            }
            proposals.add(ActionProposal("add_water", params))
        }
        processedText = processedText.replace(Regex("<add_water>.*?</add_water>", RegexOption.DOT_MATCHES_ALL), "").trim()

        // 3. subtract_water
        val subWaterPattern = java.util.regex.Pattern.compile("<subtract_water>(.*?)</subtract_water>", java.util.regex.Pattern.DOTALL)
        val subWaterMatcher = subWaterPattern.matcher(processedText)
        while (subWaterMatcher.find()) {
            val raw = subWaterMatcher.group(1)?.trim() ?: ""
            val params = mutableMapOf<String, String>()
            if (raw.toIntOrNull() != null) {
                params["amount"] = raw
            } else {
                raw.split(",").forEach { pair ->
                    val p = pair.split("=", limit = 2)
                    if (p.size == 2) {
                        params[p[0].trim().lowercase()] = p[1].trim()
                    }
                }
            }
            proposals.add(ActionProposal("subtract_water", params))
        }
        processedText = processedText.replace(Regex("<subtract_water>.*?</subtract_water>", RegexOption.DOT_MATCHES_ALL), "").trim()

        // 4. create_custom_drink
        val createDrinkPattern = java.util.regex.Pattern.compile("<create_custom_drink>(.*?)</create_custom_drink>", java.util.regex.Pattern.DOTALL)
        val createDrinkMatcher = createDrinkPattern.matcher(processedText)
        while (createDrinkMatcher.find()) {
            val raw = createDrinkMatcher.group(1)?.trim() ?: ""
            val params = mutableMapOf<String, String>()
            raw.split(",").forEach { pair ->
                val p = pair.split("=", limit = 2)
                if (p.size == 2) {
                    params[p[0].trim().lowercase()] = p[1].trim()
                }
            }
            proposals.add(ActionProposal("create_drink", params))
        }
        processedText = processedText.replace(Regex("<create_custom_drink>.*?</create_custom_drink>", RegexOption.DOT_MATCHES_ALL), "").trim()

        // 5. change_color
        val changeColorPattern = java.util.regex.Pattern.compile("<change_color>(.*?)</change_color>", java.util.regex.Pattern.DOTALL)
        val changeColorMatcher = changeColorPattern.matcher(processedText)
        while (changeColorMatcher.find()) {
            val raw = changeColorMatcher.group(1)?.trim() ?: ""
            val params = mutableMapOf<String, String>()
            raw.split(",").forEach { pair ->
                val p = pair.split("=", limit = 2)
                if (p.size == 2) {
                    params[p[0].trim().lowercase()] = p[1].trim()
                }
            }
            proposals.add(ActionProposal("change_color", params))
        }
        processedText = processedText.replace(Regex("<change_color>.*?</change_color>", RegexOption.DOT_MATCHES_ALL), "").trim()

        // Set pending proposal state!
        if (proposals.isNotEmpty()) {
            _pendingProposal.value = proposals
        }

        // Reminders standard
        val pattern = java.util.regex.Pattern.compile("<add_reminder>(.*?)</add_reminder>", java.util.regex.Pattern.DOTALL)
        val matcher = pattern.matcher(processedText)
        val messagesToAdd = mutableListOf<String>()
        while (matcher.find()) {
            val msg = matcher.group(1)?.trim()
            if (!msg.isNullOrEmpty()) {
                messagesToAdd.add(msg)
            }
        }
        
        messagesToAdd.forEach { addAiCoachReminder(it) }
        
        return processedText.replace("<add_reminder>", "").replace("</add_reminder>", "").trim().replace("</add_reminder>", "").trim()
    }

    fun subtractWaterLogAmount(amountMl: Int) {
        viewModelScope.launch {
            val todayKey = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            val todayLogs = allLogs.value.filter { it.dateString == todayKey }.sortedByDescending { it.timestamp }
            var remainingToSubtract = amountMl
            for (log in todayLogs) {
                if (remainingToSubtract <= 0) break
                if (log.amountMl <= remainingToSubtract) {
                    remainingToSubtract -= log.amountMl
                    repository.deleteLog(log)
                } else {
                    val updated = log.copy(amountMl = log.amountMl - remainingToSubtract)
                    remainingToSubtract = 0
                    repository.insertLog(updated)
                }
            }
            calculateStreak()
            triggerAutoBackup()
            updateHomeScreenWidget()
        }
    }

    private fun parseHexColor(hex: String): Int? {
        val cleanHex = hex.trim().removePrefix("#")
        return try {
            if (cleanHex.length == 6) {
                java.lang.Long.parseLong("FF$cleanHex", 16).toInt()
            } else if (cleanHex.length == 8) {
                java.lang.Long.parseLong(cleanHex, 16).toInt()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseRgbColor(r: Int, g: Int, b: Int): Int {
        return (255 shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun intToRgbFloats(colorInt: Int): Triple<Float, Float, Float> {
        val r = ((colorInt shr 16) and 0xFF).toFloat()
        val g = ((colorInt shr 8) and 0xFF).toFloat()
        val b = (colorInt and 0xFF).toFloat()
        return Triple(r, g, b)
    }

    fun acceptPendingProposal() {
        val proposals = _pendingProposal.value ?: return
        viewModelScope.launch {
            proposals.forEach { proposal ->
                try {
                    when (proposal.type) {
                        "setting" -> {
                            val key = proposal.params["key"]?.lowercase() ?: ""
                            val value = proposal.params["value"] ?: ""
                            when (key) {
                                "daily_goal" -> value.toIntOrNull()?.let { updateDailyGoal(it) }
                                "quick_add" -> value.toIntOrNull()?.let { updateQuickAddAmount(it) }
                                "fluid_ounces" -> updateFluidOuncesEnabled(value.equals("true", ignoreCase = true))
                                "reminders" -> updateReminders(value.equals("true", ignoreCase = true))
                                "reminder_interval" -> value.toIntOrNull()?.let { updateReminderInterval(it) }
                                "theme_mode" -> updateThemeMode(value)
                                "oled_mode" -> updateOledMode(value.equals("true", ignoreCase = true))
                                "app_language" -> updateAppLanguage(value)
                                "fart_mode" -> updateFartModeEnabled(value.equals("true", ignoreCase = true))
                                "blur_effect" -> updateBlurEffectEnabled(value.equals("true", ignoreCase = true))
                                "rainbow_border" -> updateRainbowBorderEnabled(value.equals("true", ignoreCase = true))
                                "voice_mode" -> updateVoiceModeEnabled(value.equals("true", ignoreCase = true))
                                "water_remaining" -> updateShowWaterRemaining(value.equals("true", ignoreCase = true))
                                "app_theme" -> updateAppTheme(value.uppercase())
                                "app_theme_palette_index", "palette_index", "theme_palette_index" -> value.toIntOrNull()?.let { updateAppThemePaletteIndex(it) }
                                "box_bg_source", "bg_source" -> updateBoxBgSource(value.uppercase())
                                "box_bg_palette_choice", "bg_palette_choice", "background_palette_choice" -> value.toIntOrNull()?.let { updateBoxBgPaletteChoice(it) }
                                "box_bg_oled_enabled", "bg_oled_enabled" -> updateBoxBgOledEnabled(value.equals("true", ignoreCase = true))
                                "keep_circle_inside_opaque", "circle_inside_opaque" -> updateKeepCircleInsideOpaque(value.equals("true", ignoreCase = true))
                                "haptic_mode" -> {
                                    val mode = when (value.lowercase()) {
                                        "enhanced" -> HapticMode.ENHANCED
                                        else -> HapticMode.DEFAULT
                                    }
                                    updateHapticMode(mode)
                                }
                                "enhanced_haptic_strength" -> {
                                    val strength = when (value.lowercase()) {
                                        "very_light", "verylight" -> EnhancedHapticStrength.VERY_LIGHT
                                        "light" -> EnhancedHapticStrength.LIGHT
                                        "normal" -> EnhancedHapticStrength.NORMAL
                                        "strong" -> EnhancedHapticStrength.STRONG
                                        "very_strong", "verystrong", "maximum", "max" -> EnhancedHapticStrength.MAXIMUM
                                        else -> EnhancedHapticStrength.NORMAL
                                    }
                                    updateEnhancedHapticStrength(strength)
                                }
                            }
                        }
                        "add_water" -> {
                            val amount = proposal.params["amount"]?.toIntOrNull() ?: 250
                            val beverage = proposal.params["beverage"] ?: "Water"
                            val factor = proposal.params["factor"]?.toFloatOrNull() ?: 1.0f
                            addWaterLog(amount, beverage, factor)
                        }
                        "subtract_water" -> {
                            val amount = proposal.params["amount"]?.toIntOrNull() ?: 250
                            subtractWaterLogAmount(amount)
                        }
                        "create_drink" -> {
                            val name = proposal.params["name"] ?: "Custom Drink"
                            val factor = proposal.params["factor"]?.toFloatOrNull() ?: 1.0f
                            val icon = proposal.params["icon"] ?: "waterdrop"
                            saveCustomDrink(name, factor, icon)
                        }
                        "change_color" -> {
                            val element = proposal.params["element"]?.lowercase() ?: ""
                            val colorStr = proposal.params["color"]
                            val rStr = proposal.params["r"]
                            val gStr = proposal.params["g"]
                            val bStr = proposal.params["b"]
                            
                            var finalColorInt: Int? = null
                            if (!colorStr.isNullOrEmpty()) {
                                finalColorInt = parseHexColor(colorStr)
                            } else if (!rStr.isNullOrEmpty() && !gStr.isNullOrEmpty() && !bStr.isNullOrEmpty()) {
                                val r = rStr.toIntOrNull() ?: 255
                                val g = gStr.toIntOrNull() ?: 255
                                val b = bStr.toIntOrNull() ?: 255
                                finalColorInt = parseRgbColor(r, g, b)
                            }
                            
                            if (finalColorInt != null) {
                                when (element) {
                                    "static_theme_seed" -> {
                                        updateStaticThemeSeed(finalColorInt)
                                    }
                                    "progress_circle" -> {
                                        updateProgressCircleThemeColor(finalColorInt)
                                    }
                                    "shape_a" -> {
                                        val (r, g, b) = intToRgbFloats(finalColorInt)
                                        updateShapeRGB("A", r, g, b)
                                    }
                                    "shape_b" -> {
                                        val (r, g, b) = intToRgbFloats(finalColorInt)
                                        updateShapeRGB("B", r, g, b)
                                    }
                                    "shape_c" -> {
                                        val (r, g, b) = intToRgbFloats(finalColorInt)
                                        updateShapeRGB("C", r, g, b)
                                    }
                                    "shape_d" -> {
                                        val (r, g, b) = intToRgbFloats(finalColorInt)
                                        updateShapeRGB("D", r, g, b)
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WaterViewModel", "Execution of proposal failed", e)
                }
            }
            _pendingProposal.value = null
            triggerGoalHaptic()
        }
    }

    fun declinePendingProposal() {
        _pendingProposal.value = null
        triggerButtonHaptic()
    }

    // --- Gemini AI Coach Integration ---

    data class ActionProposal(
        val type: String, // "setting", "add_water", "subtract_water", "create_drink", "change_color"
        val params: Map<String, String>
    )

    private val _pendingProposal = MutableStateFlow<List<ActionProposal>?>(null)
    val pendingProposal: StateFlow<List<ActionProposal>?> = _pendingProposal.asStateFlow()

    private val _selectedImageBase64 = MutableStateFlow<String?>(null)
    val selectedImageBase64: StateFlow<String?> = _selectedImageBase64.asStateFlow()

    private val _selectedImageUri = MutableStateFlow<String?>(null)
    val selectedImageUri: StateFlow<String?> = _selectedImageUri.asStateFlow()

    fun setSelectedImage(uriString: String?, base64: String?) {
        _selectedImageUri.value = uriString
        _selectedImageBase64.value = base64
        triggerButtonHaptic()
    }

    fun clearSelectedImage() {
        _selectedImageUri.value = null
        _selectedImageBase64.value = null
        triggerButtonHaptic()
    }

    fun saveBase64Audio(base64Data: String, mimeType: String): String? {
        return try {
            val decoded = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
            val extension = if (mimeType.contains("mp3")) "mp3" else "wav"
            val file = java.io.File(getApplication<android.app.Application>().cacheDir, "lyria_${System.currentTimeMillis()}.$extension")
            file.outputStream().use { it.write(decoded) }
            file.absolutePath
        } catch (e: Exception) {
            android.util.Log.e("WaterViewModel", "Failed to save generated audio", e)
            null
        }
    }

    data class ChatMessage(
        val id: String = UUID.randomUUID().toString(),
        val text: String,
        val isUser: Boolean,
        val timestamp: Long = System.currentTimeMillis(),
        val audioPath: String? = null
    )

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage(
            text = application.getString(R.string.ai_greeting_message),
            isUser = false
        )
    ))
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _chatLoading = MutableStateFlow(false)
    val chatLoading: StateFlow<Boolean> = _chatLoading.asStateFlow()

    private val _chatError = MutableStateFlow<String?>(null)
    val chatError: StateFlow<String?> = _chatError.asStateFlow()

    fun sendChatMessage(text: String, imageBase64: String? = null) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() && imageBase64 == null) return
        if (_chatLoading.value) return
        
        viewModelScope.launch {
            _chatError.value = null
            
            val userMsgText = if (imageBase64 != null || _selectedImageBase64.value != null) {
                if (trimmed.isNotEmpty()) "📷 $trimmed" else "📷 [Image Attached]"
            } else {
                trimmed
            }
            val userMsg = ChatMessage(text = userMsgText, isUser = true)
            _chatMessages.value = _chatMessages.value + userMsg
            _chatLoading.value = true
            
            try {
                // Compile full logs context for the personal advisor prompt
                val logsList = allLogs.value
                val todayGoal = dailyGoalMl.value
                val todayTotal = totalIntakeToday.value
                val currentStreakDays = streak.value
                
                val contextBuilder = StringBuilder()
                contextBuilder.append("User Hydration Context:\n")
                contextBuilder.append("- Daily Hydration Goal: ${todayGoal}ml\n")
                contextBuilder.append("- Water Consumed Today: ${todayTotal}ml\n")
                contextBuilder.append("- Current Completion percentage: ${(todayTotal * 100f / todayGoal.coerceAtLeast(1)).toInt()}%\n")
                contextBuilder.append("- Hydration Streak Count: ${currentStreakDays} days\n")
                
                // Physical & Athletic Profile info (protein, creatine, weight, height, BMI, etc.)
                val weightKg = if (_setupWeight.value > 10f) _setupWeight.value else prefs.getFloat("user_weight", 0f)
                val heightCm = _setupHeight.value
                val isCreatineActive = _creatineEnabled.value
                val isProteinActive = _proteinEnabled.value
                val proteinIsFixed = _proteinFixed.value
                val pMin = _proteinMin.value
                val pMax = _proteinMax.value
                val userMbti = prefs.getString("user_mbti", null)
                val bigFiveO = prefs.getInt("user_bigfive_o", -1)
                val bigFiveC = prefs.getInt("user_bigfive_c", -1)
                val bigFiveE = prefs.getInt("user_bigfive_e", -1)
                val bigFiveA = prefs.getInt("user_bigfive_a", -1)
                val bigFiveN = prefs.getInt("user_bigfive_n", -1)

                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val workoutBonusToday = prefs.getInt("workout_bonus_$todayStr", 0)
                val sleepBonusToday = prefs.getInt("sleep_bonus_$todayStr", 0)
                val heatBonusToday = prefs.getInt("heat_bonus_$todayStr", 0)
                val isWalkingCountsEnabled = prefs.getBoolean("workout_walking_counts", true)

                contextBuilder.append("\nUser Physical & Health / Supplement Profile:\n")
                if (weightKg > 0f) {
                    contextBuilder.append("- Body Weight: ${String.format(Locale.US, "%.1f", weightKg)} kg\n")
                } else {
                    contextBuilder.append("- Body Weight: Not set (estimated baseline ~75kg)\n")
                }

                if (heightCm > 0f) {
                    contextBuilder.append("- Height: ${String.format(Locale.US, "%.1f", heightCm)} cm\n")
                    if (weightKg > 0f) {
                        val bmi = weightKg / ((heightCm / 100f) * (heightCm / 100f))
                        val bmiCategory = when {
                            bmi < 18.5 -> "Underweight"
                            bmi < 25.0 -> "Normal weight"
                            bmi < 30.0 -> "Overweight"
                            else -> "Obese"
                        }
                        contextBuilder.append("- BMI: ${String.format(Locale.US, "%.1f", bmi)} ($bmiCategory)\n")
                    }
                }

                contextBuilder.append("- Creatine Supplementation: ${if (isCreatineActive) "ENABLED / ACTIVE (Supplementing with creatine; increases muscle cell water retention requiring ~500ml extra daily hydration)" else "Not enabled"}\n")

                if (isProteinActive) {
                    val proteinTargetStr = if (proteinIsFixed) "${pMin}g / day" else "${pMin}g - ${pMax}g / day"
                    contextBuilder.append("- Protein Intake Tracking: ENABLED (Daily Target: $proteinTargetStr; high protein intake increases renal solute load requiring higher water intake for urea clearance)\n")
                } else {
                    contextBuilder.append("- Protein Intake Tracking: Not active\n")
                }

                if (!userMbti.isNullOrBlank() || bigFiveO != -1) {
                    val psychometricsStr = mutableListOf<String>()
                    if (!userMbti.isNullOrBlank()) psychometricsStr.add("MBTI: $userMbti")
                    if (bigFiveO != -1) psychometricsStr.add("Big 5 (O:$bigFiveO%, C:$bigFiveC%, E:$bigFiveE%, A:$bigFiveA%, N:$bigFiveN%)")
                    contextBuilder.append("- User Psychometric Profile: ${psychometricsStr.joinToString(", ")}\n")
                }

                contextBuilder.append("- Today's Activity & Environmental Adjustments: Workout bonus: +${workoutBonusToday}ml, Sleep bonus: +${sleepBonusToday}ml, Heat bonus: +${heatBonusToday}ml (Step-walking adjustment: ${if (isWalkingCountsEnabled) "Active" else "Off"})\n")

                // Inject real weather and rain status to the AI Coach
                val isRainingNow = _isRaining.value
                val weatherTempStr = _weatherTemperature.value?.let { String.format(Locale.US, "%.1f°C", it) } ?: "unknown"
                contextBuilder.append("- Current Local Weather: Temperature is $weatherTempStr, raining status is ${if (isRainingNow) "RAINING (wet outside, cozy vibes! If appropriate, make some enthusiastic or caring style comment about taking umbrellas or cozy cups!)" else "NOT raining (dry weather)"}\n")
                
                // Comprehensive 1.5-Year (547-Day) Historical Logs Context
                val nowMillis = System.currentTimeMillis()
                val fiveHundredFortySevenDaysMs = 547L * 24 * 3600 * 1000
                val cutoffMillis = nowMillis - fiveHundredFortySevenDaysMs
                val past15YLogs = logsList.filter { it.timestamp >= cutoffMillis }

                contextBuilder.append("\nUser Hydration Logs & History (Past 1.5 Years / 547 Days):\n")
                contextBuilder.append("- Total Logs Recorded (Past 1.5 Years): ${past15YLogs.size}\n")

                if (past15YLogs.isNotEmpty()) {
                    val totalMl15Y = past15YLogs.sumOf { it.waterEquivalentMl.toLong() }
                    val totalLiters15Y = totalMl15Y / 1000.0
                    val uniqueLoggingDays = past15YLogs.map { it.dateString }.distinct()
                    val activeDaysCount = uniqueLoggingDays.size
                    val dailyAvgMl = if (activeDaysCount > 0) (totalMl15Y / activeDaysCount).toInt() else 0
                    
                    contextBuilder.append("- Total Volume Consumed (Past 1.5 Years): ${String.format(Locale.US, "%.2f", totalLiters15Y)} Liters (${totalMl15Y} ml)\n")
                    contextBuilder.append("- Active Days Logged: $activeDaysCount days in past 547 days\n")
                    contextBuilder.append("- Daily Average Volume on Active Days: ${dailyAvgMl} ml/day\n")
                    
                    val bevDistribution = past15YLogs.groupBy { it.beverageType }
                        .mapValues { entry -> entry.value.sumOf { log -> log.waterEquivalentMl.toLong() } }
                        .entries.sortedByDescending { it.value }
                        .joinToString(", ") { "${it.key}: ${it.value / 1000}L (${String.format(Locale.US, "%.1f", it.value * 100.0 / totalMl15Y.coerceAtLeast(1))}%)" }
                    contextBuilder.append("- Beverage Distribution (1.5 Years): $bevDistribution\n")

                    val dailyTotals = past15YLogs.groupBy { it.dateString }
                        .mapValues { entry -> entry.value.sumOf { log -> log.waterEquivalentMl } }
                    val maxDay = dailyTotals.maxByOrNull { it.value }
                    if (maxDay != null) {
                        contextBuilder.append("- Peak Hydration Day (1.5 Years): ${maxDay.value}ml on ${maxDay.key}\n")
                    }

                    val monthlyTotals = past15YLogs.groupBy { if (it.dateString.length >= 7) it.dateString.substring(0, 7) else "Unknown" }
                        .mapValues { entry ->
                            val totalMonthMl = entry.value.sumOf { log -> log.waterEquivalentMl.toLong() }
                            val monthDaysCount = entry.value.map { log -> log.dateString }.distinct().size
                            val monthAvgMl = if (monthDaysCount > 0) totalMonthMl / monthDaysCount else 0
                            val bevTypesMonth = entry.value.groupBy { log -> log.beverageType }
                                .mapValues { b -> b.value.sumOf { l -> l.amountMl } }
                                .map { b -> "${b.key}: ${b.value}ml" }
                                .joinToString(", ")
                            "Total: ${totalMonthMl / 1000}L over $monthDaysCount active days (avg ${monthAvgMl}ml/day) [$bevTypesMonth]"
                        }

                    contextBuilder.append("- Monthly Summaries (Past 18 Months):\n")
                    for ((monthKey, monthSummary) in monthlyTotals.entries.sortedByDescending { it.key }) {
                        contextBuilder.append("  * ${monthKey}: ${monthSummary}\n")
                    }
                } else {
                    contextBuilder.append("- No historical logs found in the past 1.5 years.\n")
                }

                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val calendar = Calendar.getInstance()
                contextBuilder.append("- Detailed Daily Breakdown (Past 14 Days):\n")
                
                for (i in 0 until 14) {
                    val dateKey = sdf.format(calendar.time)
                    val daysLogs = logsList.filter { it.dateString == dateKey }
                    val totalMl = daysLogs.sumOf { it.waterEquivalentMl }
                    val dayGoal = getDailyGoalForDate(dateKey)
                    val status = if (totalMl >= dayGoal) "Goal Achieved" else "Goal Incomplete"
                    val beverageDistribution = daysLogs.groupBy { it.beverageType }
                        .mapValues { it.value.sumOf { log -> log.amountMl } }
                        .map { "${it.key}: ${it.value}ml" }
                        .joinToString(", ")
                    
                    if (daysLogs.isNotEmpty() || i < 5) {
                        contextBuilder.append("  * ${dateKey}: ${totalMl}ml / ${dayGoal}ml (${status}) [Beverages: ${if (beverageDistribution.isEmpty()) "None logged" else beverageDistribution}]\n")
                    }
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                }
                
                val defaultCapabilities = """
                    APP INTERACTION & AUTOMATION CAPABILITY (PROPOSALS):
                    Whenever the user asks you to log water, subtract/delete water, change any app setting, configure elements of the app, change colors, or customize background styles, you MUST output specific XML-styled tags inside your text response to propose these actions. 
                    The app will intercept these tags and present "Accept" and "Decline" buttons to the user so they can give explicit permission before proceeding.
                    Always inform the user clearly in your message that they can accept or decline this proposal using the buttons below.
                    
                    CRITICAL CONSTRAINT ON PROPOSING CHANGES (MANDATORY):
                    You are STRICTLY FORBIDDEN from suggesting, proposing, or outputting any XML tags (such as <add_water>, <change_setting>, <create_custom_drink>, or <change_color>) unless the user has explicitly requested an action, a configuration change, a settings update, a log action, or a color adjustment in their immediate input.
                    If the user's message is purely informational, asks a general question, or is one of the suggested quick topics (such as Weekly Trends, Workout Fuel, Coffee vs Water, Morning Routine, Skin Vitality, Electrolyte Balance), you MUST NOT propose or suggest any settings/color/water changes. Do NOT output any XML tags. Simply answer their question or provide the requested information with absolutely no proposals or suggestions of changes.
                    
                    IMPORTANT: You have complete and absolute control over every single setting, layout, detail, and accent color of the app. You can customize the screen theme mode, OLED styling, box backgrounds, shape colors, and core transparency values as follows:
                    
                    Allowed tags format (you can combine multiple tags if needed):
                    1. Water Management:
                       - To add water: <add_water>amount=250,beverage=Water,factor=1.0</add_water> (change beverage or factor if it is tea/coffee/etc.)
                       - To subtract/delete water: <subtract_water>amount=100</subtract_water>
                    2. App Settings:
                       - To change settings: <change_setting>KEY=VALUE</change_setting>
                         Allowed KEYs and VALUE specifications:
                         - "app_theme" (String: "DYNAMIC" to base colors on user's wallpaper, or "STATIC" for custom colors)
                         - "palette_index" (Integer: 0 to 5 to select different dynamic palette choices extracted from the user's wallpaper colors)
                         - "box_bg_source" (String: "THEME" to match card backings with active theme, or "PALETTE" for independent palette accent)
                         - "box_bg_palette_choice" (Integer: 0 for primary theme accent container, 1 for secondary, 2 for tertiary theme container colors)
                         - "box_bg_oled_enabled" (Boolean: "true" to make card/container backings pitch black on dark OLED, "false" to use standard dark)
                         - "keep_circle_inside_opaque" (Boolean: "true" to keep progress circle's center opaque when card transparency is on, "false" for completely transparent inside)
                         - "daily_goal" (Integer in ml, e.g. <change_setting>daily_goal=2500</change_setting>)
                         - "quick_add" (Integer in ml, e.g. <change_setting>quick_add=250</change_setting>)
                         - "fluid_ounces" (Boolean: "true" or "false")
                         - "reminders" (Boolean: "true" or "false")
                         - "reminder_interval" (Integer in hours)
                         - "theme_mode" (String: "Light", "Dark", or "System")
                         - "oled_mode" (Boolean: "true" or "false")
                         - "app_language" (String language code: "en" or "el")
                         - "fart_mode" (Boolean: "true" or "false")
                         - "blur_effect" (Boolean: "true" or "false")
                         - "rainbow_border" (Boolean: "true" or "false")
                         - "voice_mode" (Boolean: "true" or "false")
                         - "water_remaining" (Boolean: "true" or "false")
                         - "haptic_mode" (String: "default", "enhanced")
                         - "enhanced_haptic_strength" (String: "very_light", "light", "normal", "strong", "very_strong", "maximum")
                    3. Custom Saved Beverages:
                       - To create custom drinks that appear on the saved custom drinks window:
                         <create_custom_drink>name=NAME,factor=FACTOR,icon=ICON</create_custom_drink>
                         - ICON options: "waterdrop", "coffee", "tea", "juice", "milk", "soda", "bolt", "flash", "soup", "beer", "wine", "eco", "water", "icecream"
                         - FACTOR is the hydrational value (e.g. 1.0 for tea, 0.9 for coffee, etc.)
                    4. Element Colors & RGB Palette Customization:
                       - To change colors of screens, progress indicators, or shapes:
                         <change_color>element=KEY,color=HEX</change_color> or <change_color>element=KEY,r=RED_INT,g=GREEN_INT,b=BLUE_INT</change_color>
                         - Allowed elements (KEY):
                           - "static_theme_seed" (updates static seed, changing primary colors, e.g. color=ffffff or color=38bdf8)
                           - "progress_circle" (updates progress circle theme color)
                           - "shape_a", "shape_b", "shape_c", "shape_d" (the background decorative shapes)
                         - Format examples:
                           - <change_color>element=static_theme_seed,color=ffffff</change_color>
                           - <change_color>element=shape_a,r=255,g=127,b=0</change_color>
                    
                    DYNAMIC WALLPAPER INTELLIGENT CAPABILITY:
                    If the user wants app-wide colors, cards, and decorative background shapes to be matching or based on their lockscreen/homescreen device wallpaper, you should explain the solution and propose:
                    - <change_setting>app_theme=DYNAMIC</change_setting>
                    This will instantly command the app's dynamic style engines to extract vibrant and matching accent colors directly from the user's wallpaper.
                    
                    MULTIMODAL IMAGE ANALYSIS CAPABILITY:
                    If the user uploads an image/picture, analyze it for water-related purposes, troubleshooting, background color inspirations, or anything pixel water-related!
                    For example, you can extract prominent base/complementary colors from the uploaded picture and propose them to the user via <change_color> tags to create a custom matching color palette for their background shapes and themes!
                """.trimIndent()

                val defaultSystemPrompt = """
                    You are "Pixel Water AI", an expert supportive and personalized Hydration Coach and Water Advisor helper integrated inside the "Pixel Water" tracker app.
                    Address the user warmly and directly. Offer top-tier, friendly, encouraging, and science-backed advice.

                    TEXT FORMATTING RULE (STRICT & MANDATORY):
                    - Do NOT wrap entire sentences, whole lines, or whole paragraphs in double asterisks (**bold**). Keep body sentences in regular font weight.
                    - ONLY use bold (**text**) to highlight specific numbers, key metrics, or short 1-3 word topic headings (e.g. "**Pre-hydrate:**", "**500-600ml**", "**33.6°C**").
                """.trimIndent() + "\n\n" + defaultCapabilities
                
                val userConfiguredPrompt = _systemPrompt.value
                var finalSystemPrompt = if (userConfiguredPrompt.isNotBlank() && userConfiguredPrompt != defaultSystemPrompt) {
                    "IMPORTANT MANDATE: Your persona details are configured below, but you MUST absolutely retain full capability to control settings and colors using the XML tags parameters listed in the Capabilities section below. NEVER ignore or claim you cannot perform color/setting customizations!\n\nUser Persona System Instructions:\n$userConfiguredPrompt\n\nCapabilities Configuration (MANDATORY & UNTOUCHED):\n$defaultCapabilities" 
                } else {
                    defaultSystemPrompt
                }
                
                if (_appLanguage.value == "el") {
                    finalSystemPrompt += "\n\nIMPORTANT: You MUST respond entirely in Greek."
                }
                
                val contextPrompt = "$finalSystemPrompt\n\n$contextBuilder"
                val activeBase64 = imageBase64 ?: _selectedImageBase64.value
                
                // Reset selected image states once processed
                _selectedImageUri.value = null
                _selectedImageBase64.value = null
                
                if (_aiProvider.value == "Deepseek") {
                    val apiKey = if (_deepseekApiKey.value.isNotBlank()) _deepseekApiKey.value.trim() else ""
                    if (apiKey.isBlank()) {
                        _chatLoading.value = false
                        _chatMessages.value = _chatMessages.value + ChatMessage(
                            text = "The DeepSeek API Key is currently unconfigured. Please configure your API key in the App Settings menu.",
                            isUser = false
                        )
                        _chatError.value = "Unconfigured API Key"
                        return@launch
                    }

                    val deepseekMessages = mutableListOf<com.pixelwater.app.data.DeepseekMessage>()
                    deepseekMessages.add(com.pixelwater.app.data.DeepseekMessage(role = "system", content = contextPrompt))
                    
                    _chatMessages.value.takeLast(10).forEach { msg ->
                        deepseekMessages.add(com.pixelwater.app.data.DeepseekMessage(
                            role = if (msg.isUser) "user" else "assistant",
                            content = msg.text
                        ))
                    }

                    val request = com.pixelwater.app.data.DeepseekRequest(
                        model = "deepseek-chat", // DeepSeek-V3
                        messages = deepseekMessages,
                        temperature = 0.7f,
                        maxTokens = 1200
                    )
                    
                    val response = com.pixelwater.app.data.DeepseekRetrofitClient.service.createChatCompletion(
                        authorization = "Bearer $apiKey",
                        request = request
                    )
                    
                    val replyText = response.choices?.firstOrNull()?.message?.content
                        ?: "I apologize, I couldn't process your request this time. Let's try once more."
                    
                    val parsedReply = parseAndRegisterAiReminders(replyText)
                    _chatMessages.value = _chatMessages.value + ChatMessage(text = parsedReply, isUser = false)

                } else if (_aiProvider.value == "OpenAI") {
                    val apiKey = if (_openaiApiKey.value.isNotBlank()) _openaiApiKey.value.trim() else ""
                    if (apiKey.isBlank()) {
                        _chatLoading.value = false
                        _chatMessages.value = _chatMessages.value + ChatMessage(
                            text = "The OpenAI API Key is currently unconfigured. Please configure your API key in the App Settings menu.",
                            isUser = false
                        )
                        _chatError.value = "Unconfigured API Key"
                        return@launch
                    }

                    val openaiMessages = mutableListOf<com.pixelwater.app.data.OpenaiMessage>()
                    openaiMessages.add(com.pixelwater.app.data.OpenaiMessage(role = "system", content = contextPrompt))
                    
                    _chatMessages.value.takeLast(10).forEach { msg ->
                        openaiMessages.add(com.pixelwater.app.data.OpenaiMessage(
                            role = if (msg.isUser) "user" else "assistant",
                            content = msg.text
                        ))
                    }

                    val request = com.pixelwater.app.data.OpenaiRequest(
                        model = "gpt-4o-mini",
                        messages = openaiMessages,
                        temperature = 0.7f,
                        maxTokens = 1200
                    )
                    
                    val response = com.pixelwater.app.data.OpenaiRetrofitClient.service.createChatCompletion(
                        authorization = "Bearer $apiKey",
                        request = request
                    )
                    
                    val replyText = response.choices?.firstOrNull()?.message?.content
                        ?: "I apologize, I couldn't process your request this time. Let's try once more."
                    
                    val parsedReply = parseAndRegisterAiReminders(replyText)
                    _chatMessages.value = _chatMessages.value + ChatMessage(text = parsedReply, isUser = false)

                } else {
                    // Google Gemini
                    val hasPersonalKey = _geminiApiKey.value.isNotBlank()
                    val apiKey = if (hasPersonalKey) _geminiApiKey.value.trim() else com.pixelwater.app.BuildConfig.GEMINI_API_KEY.trim()
                    
                    if (apiKey == "MY_GEMINI_API_KEY" || apiKey.isBlank()) {
                        _chatLoading.value = false
                        _chatMessages.value = _chatMessages.value + ChatMessage(
                            text = "A Gemini API Key is currently unconfigured. Please add an API key in the App Settings menu or set it as a system secret to activate your Pixel Water AI Coach!",
                            isUser = false
                        )
                        _chatError.value = "Unconfigured API Key"
                        return@launch
                    }
                    
                    // Determine which model to run
                    val textLc = trimmed.lowercase(java.util.Locale.ROOT)
                    val isMusicRequest = textLc.contains("music") || 
                                         textLc.contains("song") || 
                                         textLc.contains("track") || 
                                         textLc.contains("lyria") ||
                                         textLc.contains("melody") ||
                                         textLc.contains("audio") || 
                                         textLc.contains("sound")
                    
                    var modelToCall = _geminiModel.value
                    if (!hasPersonalKey) {
                        // Default to low-latency responses mode using gemini-3.5-flash when key is unconfigured (optional)
                        modelToCall = "gemini-3.5-flash"
                    }
                    
                    if (!activeBase64.isNullOrBlank()) {
                        // Use gemini-3.1-pro-preview for image understanding as mandated
                        modelToCall = "gemini-3.1-pro-preview"
                    } else if (isMusicRequest) {
                        // Music & audio generation is routed to gemini-2.5-flash-native-audio-preview-12-2025 as recommended
                        modelToCall = "gemini-2.5-flash-native-audio-preview-12-2025"
                    }
                    
                    val validHistory = mutableListOf<ChatMessage>()
                    var lastRoleIsUser: Boolean? = null
                    for (msg in _chatMessages.value.takeLast(10)) {
                        if (lastRoleIsUser == null || lastRoleIsUser != msg.isUser) {
                            validHistory.add(msg)
                            lastRoleIsUser = msg.isUser
                        } else {
                            val last = validHistory.removeAt(validHistory.lastIndex)
                            validHistory.add(last.copy(text = last.text + "\n\n" + msg.text))
                        }
                    }
                    
                    if (validHistory.isNotEmpty() && !validHistory.first().isUser) {
                        validHistory.removeAt(0)
                    }
                    
                    val chatHistory = validHistory.mapIndexed { index, msg ->
                        val parts = mutableListOf<com.pixelwater.app.data.Part>()
                        parts.add(com.pixelwater.app.data.Part(text = msg.text))
                        
                        // Append Base64 image payload to the target initial input part if it exists!
                        if (index == validHistory.lastIndex && msg.isUser && !activeBase64.isNullOrBlank()) {
                            parts.add(com.pixelwater.app.data.Part(inlineData = com.pixelwater.app.data.InlineData(mimeType = "image/jpeg", data = activeBase64)))
                        }
                        
                        com.pixelwater.app.data.Content(
                            role = if (msg.isUser) "user" else "model",
                            parts = parts
                        )
                    }
                    
                    // Enable googleSearch tool for search grounding when gemini-3.5-flash is selected
                    val toolsList = if (modelToCall == "gemini-3.5-flash") {
                        listOf(com.pixelwater.app.data.Tool(googleSearch = emptyMap()))
                    } else {
                        null
                    }
                    
                    // Set modality to AUDIO if it's music request
                    val responseModalities = if (isMusicRequest) listOf("AUDIO") else null
                    
                    val request = com.pixelwater.app.data.GeminiRequest(
                        contents = chatHistory,
                        systemInstruction = com.pixelwater.app.data.Content(parts = listOf(com.pixelwater.app.data.Part(text = contextPrompt))),
                        generationConfig = com.pixelwater.app.data.GenerationConfig(
                            temperature = 0.7f, 
                            maxOutputTokens = 1200,
                            responseModalities = responseModalities
                        ),
                        tools = toolsList
                    )
                    
                    val response = safeGenerateContent(
                        model = modelToCall,
                        apiKey = apiKey,
                        request = request
                    )
                    
                    // Handle output parts (either text response, or audio stream)
                    val responsePart = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()
                    val replyText = responsePart?.text
                    val audioData = responsePart?.inlineData
                    
                    if (audioData != null && audioData.mimeType.startsWith("audio/")) {
                        // Decode audio and save to file
                        val savedPath = saveBase64Audio(audioData.data, audioData.mimeType)
                        val trackLabel = if (modelToCall == "gemini-2.5-flash-native-audio-preview-12-2025") "Gemini-2.5 Audio Engine 🎵" else "AI Audio Engine 🎵"
                        val dynamicText = if (_appLanguage.value == "el") {
                            "🎵 Δημιουργήθηκε επιτυχώς μουσικό κομμάτι με το $trackLabel!"
                        } else {
                            "🎵 Custom music track successfully generated using $trackLabel!"
                        }
                        _chatMessages.value = _chatMessages.value + ChatMessage(
                            text = dynamicText,
                            isUser = false,
                            audioPath = savedPath
                        )
                    } else {
                        val parsedReply = parseAndRegisterAiReminders(replyText ?: "I apologize, I couldn't process your request this time. Let's try once more.")
                        _chatMessages.value = _chatMessages.value + ChatMessage(text = parsedReply, isUser = false)
                    }
                }
            } catch (e: Exception) {
                Log.e("WaterViewModel", "AI API failed", e)
                _chatError.value = e.localizedMessage
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    text = "I encountered an error connecting to the AI provider: ${e.localizedMessage}. Please ensure your internet connection is active and that your API key is correctly configured.",
                    isUser = false
                )
            } finally {
                _chatLoading.value = false
            }
        }
    }

    suspend fun sendSilentAiRequest(prompt: String) {
        val customPrompt = _notificationCustomPrompt.value
        val finalSystemPrompt = """
            You are "Pixel Water AI", an expert supportive and personalized Hydration Coach and Water Advisor helper.
            Always incorporate custom notification messages within a strict "<add_reminder>...</add_reminder>" XML block in your response. 
            Do not include standard water 💧 emojis in these reminders. 
            You MUST use fun text-emojis/kaomojis such as ( ◜‿◝ )♡, (｡･ω･｡)ﾉ♡, (・o・), ಠ_ʖಠ, ಠ_ಠ, ಠ︵ಠ, (•‿•), ◉‿◉, ᕙ( • ‿ • )ᕗ and others inside the newly created reminders.
            Example:
            - "<add_reminder>Drink water right now! (｡･ω･｡)ﾉ♡</add_reminder>"
            - "<add_reminder>Hydration time! (・o・)</add_reminder>"
            ${if (customPrompt.isNotBlank()) "\nUSER INSTRUCTION SPECIFIC TO NOTIFICATION REMINDERS STYLE:\n$customPrompt\n" else ""}
        """.trimIndent()
        try {
            if (_aiProvider.value == "Deepseek") {
                val apiKey = if (_deepseekApiKey.value.isNotBlank()) _deepseekApiKey.value.trim() else ""
                if (apiKey.isNotBlank()) {
                    val messages = listOf(
                        com.pixelwater.app.data.DeepseekMessage(role = "system", content = finalSystemPrompt),
                        com.pixelwater.app.data.DeepseekMessage(role = "user", content = prompt)
                    )
                    val request = com.pixelwater.app.data.DeepseekRequest(
                        model = "deepseek-chat",
                        messages = messages,
                        temperature = 0.7f,
                        maxTokens = 800
                    )
                    val response = com.pixelwater.app.data.DeepseekRetrofitClient.service.createChatCompletion(
                        authorization = "Bearer $apiKey",
                        request = request
                    )
                    val replyText = response.choices?.firstOrNull()?.message?.content
                    if (replyText != null) {
                        parseAndRegisterAiReminders(replyText)
                    }
                } else {
                    throw Exception("DeepSeek API Key is unconfigured.")
                }
            } else if (_aiProvider.value == "OpenAI") {
                val apiKey = if (_openaiApiKey.value.isNotBlank()) _openaiApiKey.value.trim() else ""
                if (apiKey.isNotBlank()) {
                    val messages = listOf(
                        com.pixelwater.app.data.OpenaiMessage(role = "system", content = finalSystemPrompt),
                        com.pixelwater.app.data.OpenaiMessage(role = "user", content = prompt)
                    )
                    val request = com.pixelwater.app.data.OpenaiRequest(
                        model = "gpt-4o-mini",
                        messages = messages,
                        temperature = 0.7f,
                        maxTokens = 800
                    )
                    val response = com.pixelwater.app.data.OpenaiRetrofitClient.service.createChatCompletion(
                        authorization = "Bearer $apiKey",
                        request = request
                    )
                    val replyText = response.choices?.firstOrNull()?.message?.content
                    if (replyText != null) {
                        parseAndRegisterAiReminders(replyText)
                    }
                } else {
                    throw Exception("OpenAI API Key is unconfigured.")
                }
            } else {
                // Default: Google Gemini
                val apiKey = if (_geminiApiKey.value.isNotBlank()) _geminiApiKey.value.trim() else com.pixelwater.app.BuildConfig.GEMINI_API_KEY.trim()
                if (apiKey != "MY_GEMINI_API_KEY" && apiKey.isNotBlank()) {
                    val contents = listOf(
                        com.pixelwater.app.data.Content(
                            role = "user",
                            parts = listOf(com.pixelwater.app.data.Part(text = prompt))
                        )
                    )
                    val request = com.pixelwater.app.data.GeminiRequest(
                        contents = contents,
                        systemInstruction = com.pixelwater.app.data.Content(parts = listOf(com.pixelwater.app.data.Part(text = finalSystemPrompt))),
                        generationConfig = com.pixelwater.app.data.GenerationConfig(temperature = 0.7f, maxOutputTokens = 800)
                    )
                    val response = safeGenerateContent(
                        model = _geminiModel.value,
                        apiKey = apiKey,
                        request = request
                    )
                    val replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (replyText != null) {
                        parseAndRegisterAiReminders(replyText)
                    }
                } else {
                    throw Exception("Gemini API Key is unconfigured.")
                }
            }
        } catch (e: Exception) {
            Log.e("WaterViewModel", "Silent AI API failed", e)
            throw e
        }
    }

    fun clearChat() {
        val app = getApplication<Application>()
        _chatMessages.value = listOf(
            ChatMessage(
                text = app.getString(R.string.ai_greeting_message),
                isUser = false
            )
        )
        _chatError.value = null
        _chatLoading.value = false
    }

    fun ensureFirebaseInitialized() {
        try {
            if (com.google.firebase.FirebaseApp.getApps(getApplication()).isEmpty()) {
                val options = com.google.firebase.FirebaseOptions.Builder()
                    .setApiKey(com.pixelwater.app.BuildConfig.FIREBASE_API_KEY)
                    .setApplicationId(com.pixelwater.app.BuildConfig.FIREBASE_APP_ID)
                    .setProjectId(com.pixelwater.app.BuildConfig.FIREBASE_PROJECT_ID)
                    .build()
                com.google.firebase.FirebaseApp.initializeApp(getApplication(), options)
            }
        } catch (e: Exception) {
            Log.e("WaterViewModel", "Firebase initialization error: ${e.message}")
        }
    }

    fun restoreFromFirestore(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                ensureFirebaseInitialized()
                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                val user = auth.currentUser
                if (user != null) {
                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    val userDoc = db.collection("users").document(user.uid)
                    userDoc.get().addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            val logsList = document.get("logs") as? List<Map<String, Any>>
                            val settingsMap = document.get("settings") as? Map<String, Any>
                            
                            viewModelScope.launch {
                                if (logsList != null) {
                                    val currentLogs = repository.getAllLogs().first()
                                    logsList.forEach { logMap ->
                                        val amountMl = (logMap["amountMl"] as? Number)?.toInt() ?: 0
                                        val timestamp = (logMap["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                                        val dateStr = logMap["dateString"] as? String ?: getCurrentDateString()
                                        val bevType = logMap["beverageType"] as? String ?: "Water"
                                        val eq = (logMap["waterEquivalency"] as? Number)?.toFloat() ?: 1.0f
                                        val eqMl = (logMap["waterEquivalentMl"] as? Number)?.toInt() ?: amountMl
                                        val src = logMap["sourceDevice"] as? String
                                        
                                        val exists = currentLogs.any { it.timestamp == timestamp && it.amountMl == amountMl }
                                        if (!exists) {
                                            val newLog = WaterLog(
                                                amountMl = amountMl,
                                                timestamp = timestamp,
                                                dateString = dateStr,
                                                beverageType = bevType,
                                                waterEquivalency = eq,
                                                waterEquivalentMl = eqMl,
                                                sourceDevice = src
                                            )
                                            repository.insertLog(newLog)
                                        }
                                    }
                                }
                                
                                if (settingsMap != null) {
                                    settingsMap["daily_goal_ml"]?.let { val g = (it as? Number)?.toInt(); if (g != null) updateDailyGoal(g) }
                                    settingsMap["haptics_sliders_enabled"]?.let { val b = it as? Boolean; if (b != null) updateHapticsSlidersEnabled(b) }
                                    settingsMap["haptics_buttons_enabled"]?.let { val b = it as? Boolean; if (b != null) updateHapticsButtonsEnabled(b) }
                                    settingsMap["haptics_goal_enabled"]?.let { val b = it as? Boolean; if (b != null) updateHapticsGoalEnabled(b) }
                                    settingsMap["haptics_toggles_enabled"]?.let { val b = it as? Boolean; if (b != null) updateHapticsTogglesEnabled(b) }
                                    settingsMap["fireworks_feedback_enabled"]?.let { val b = it as? Boolean; if (b != null) updateFireworksFeedbackEnabled(b) }
                                    settingsMap["app_language"]?.let { val s = it as? String; if (s != null) updateAppLanguage(s) }
                                    settingsMap["vibration_duration_ms"]?.let { val v = (it as? Number)?.toInt(); if (v != null) updateVibrationDurationMs(v) }
                                    settingsMap["vibration_gap_ms"]?.let { val v = (it as? Number)?.toInt(); if (v != null) updateVibrationGapMs(v) }
                                    settingsMap["vibration_strength"]?.let { val v = (it as? Number)?.toInt(); if (v != null) updateVibrationStrength(v) }
                                    settingsMap["custom_firework_haptics_enabled"]?.let { val b = it as? Boolean; if (b != null) updateCustomFireworkHapticsEnabled(b) }
                                    settingsMap["firework_duration_ms"]?.let { val v = (it as? Number)?.toInt(); if (v != null) updateFireworkDurationMs(v) }
                                    settingsMap["firework_gap_ms"]?.let { val v = (it as? Number)?.toInt(); if (v != null) updateFireworkGapMs(v) }
                                    settingsMap["firework_strength"]?.let { val v = (it as? Number)?.toInt(); if (v != null) updateFireworkStrength(v) }
                                    settingsMap["navbar_corner_radius"]?.let { val v = (it as? Number)?.toInt(); if (v != null) updateNavbarCornerRadius(v) }
                                }
                                
                                calculateStreak()
                                updateHomeScreenWidget()
                                onComplete()
                            }
                        } else {
                            onComplete()
                        }
                    }.addOnFailureListener {
                        onComplete()
                    }
                } else {
                    onComplete()
                }
            } catch (e: Exception) {
                Log.e("WaterViewModel", "Firestore restore error: ${e.message}")
                onComplete()
            }
        }
    }

    fun signInWithFirebase(email: String, pass: String, onResult: (Boolean, String) -> Unit) {
        try {
            ensureFirebaseInitialized()
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()

            auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    signInWithGoogle(email, "Firebase User", true)
                    restoreFromFirestore {
                        syncToFirestore()
                        val msg = if (appLanguage.value == "el") "Επιτυχής σύνδεση!" else "Successfully logged in via Firebase!"
                        onResult(true, msg)
                    }
                } else {
                    val ex = task.exception
                    val isUserNotFound = ex is com.google.firebase.auth.FirebaseAuthInvalidUserException || 
                        ex?.message?.contains("no user record", ignoreCase = true) == true ||
                        ex?.message?.contains("invalid_login", ignoreCase = true) == true ||
                        ex?.message?.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) == true

                    // Modern Firebase often uses INVALID_LOGIN_CREDENTIALS for both wrong pass and wrong user.
                    // Let's attempt to create the user anyway since we want seamless sign up / sign in.
                    auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { task2 ->
                        if (task2.isSuccessful) {
                            signInWithGoogle(email, "Firebase User", true)
                            syncToFirestore()
                            val msg = if (appLanguage.value == "el") "Επιτυχής εγγραφή και σύνδεση!" else "Successfully created and logged in via Firebase!"
                            onResult(true, msg)
                        } else {
                            // If creation failed too
                            val ex2 = task2.exception
                            val isEmailInUse = ex2?.message?.contains("already in use", ignoreCase = true) == true ||
                                    ex2?.message?.contains("already exists", ignoreCase = true) == true

                            if (isEmailInUse) {
                                // Meaning the sign in failed due to wrong password!
                                val errorMsg = if (appLanguage.value == "el") "Λάθος κωδικός πρόσβασης. Δοκιμάστε ξανά." else "Incorrect password. Please try again."
                                onResult(false, errorMsg)
                            } else {
                                // Other error during account creation (e.g., weak password)
                                onResult(false, ex2?.localizedMessage ?: "Authentication failed")
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            onResult(false, "Firebase Error: ${e.message}")
        }
    }

    fun sendPasswordResetEmail(email: String, onResult: (Boolean, String) -> Unit) {
        try {
            ensureFirebaseInitialized()
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            if (email.isBlank()) {
                val errMsg = if (appLanguage.value == "el") "Παρακαλώ εισάγετε μια έγκυρη διεύθυνση email πρώτα." else "Please enter a valid email address first."
                onResult(false, errMsg)
                return
            }
            auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val msg = if (appLanguage.value == "el") 
                        "Στάλθηκε email αλλαγής κωδικού πρόσβασης στο $email!" 
                        else "Password reset email sent to $email!"
                    onResult(true, msg)
                } else {
                    onResult(false, task.exception?.localizedMessage ?: "Failed to send reset email.")
                }
            }
        } catch (e: Exception) {
            onResult(false, e.localizedMessage ?: "Unknown error occurred.")
        }
    }

    fun sendPasswordlessEmailLink(email: String, onResult: (Boolean, String) -> Unit) {
        try {
            ensureFirebaseInitialized()
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            
            // Appends URLEncoded email for passwordless activation verification
            val deepLinkUrl = "https://pixelwater.app/login?email=${java.net.URLEncoder.encode(email, "UTF-8")}"
            
            val actionCodeSettings = com.google.firebase.auth.ActionCodeSettings.newBuilder()
                .setUrl(deepLinkUrl)
                .setHandleCodeInApp(true)
                .setAndroidPackageName(
                    getApplication<android.app.Application>().packageName,
                    true, /* installIfNotAvailable */
                    null /* minimumVersion */
                )
                .build()

            auth.sendSignInLinkToEmail(email, actionCodeSettings).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    prefs.edit().putString("email_link_auth_email", email).apply()
                    onResult(true, if (appLanguage.value == "el") "Στάλθηκε σύνδεσμος εισόδου! Ελέγξτε τα εισερχόμενά σας." else "A secure, passwordless sign-in link has been sent to your email. Click it to complete sign-in!")
                } else {
                    onResult(false, task.exception?.localizedMessage ?: "Failed to send the sign-in link.")
                }
            }
        } catch (e: Exception) {
            onResult(false, e.localizedMessage ?: "Error during passwordless initiation.")
        }
    }

    fun handleIncomingEmailLink(link: String, onResult: (Boolean, String) -> Unit) {
        try {
            ensureFirebaseInitialized()
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            if (auth.isSignInWithEmailLink(link)) {
                val uri = android.net.Uri.parse(link)
                var email = uri.getQueryParameter("email")
                
                if (email.isNullOrEmpty()) {
                    val innerLink = uri.getQueryParameter("link")
                    if (!innerLink.isNullOrEmpty()) {
                        val innerUri = android.net.Uri.parse(innerLink)
                        email = innerUri.getQueryParameter("email")
                    }
                }
                
                if (email.isNullOrEmpty()) {
                    email = prefs.getString("email_link_auth_email", "")
                }
                
                if (email.isNullOrEmpty()) {
                    onResult(false, if (appLanguage.value == "el") "Απαιτείται διεύθυνση email για ολοκλήρωση." else "To complete sign-in, please open the link on the same device or sign in with your email again.")
                    return
                }
                
                auth.signInWithEmailLink(email, link).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        signInWithGoogle(email, "Firebase User", true)
                        restoreFromFirestore {
                            syncToFirestore()
                            onResult(true, if (appLanguage.value == "el") "Επιτυχής σύνδεση χωρίς κωδικό!" else "Successfully signed in with email link passwordless!")
                        }
                    } else {
                        onResult(false, task.exception?.localizedMessage ?: "Sign-in link is invalid or expired.")
                    }
                }
            } else {
                onResult(false, "Not a valid Firebase email sign-in link.")
            }
        } catch (e: Exception) {
            Log.e("WaterViewModel", "Error in handleIncomingEmailLink: ${e.message}")
            onResult(false, e.localizedMessage ?: "Error during email link sign-in.")
        }
    }

    fun signInWithGoogle(email: String, name: String, enableDriveBackup: Boolean = true) {
        prefs.edit().apply {
            putString("google_account_email", email)
            putString("google_account_name", name)
            putBoolean("is_google_logged_in", true)
            putBoolean("google_drive_backup_enabled", enableDriveBackup)
            apply()
        }
        _googleAccountEmail.value = email
        _googleAccountName.value = name
        _isGoogleLoggedIn.value = true
        _isGoogleDriveBackupEnabled.value = enableDriveBackup
        triggerButtonHaptic()
    }

    fun signOutGoogle() {
        prefs.edit().apply {
            putString("google_account_email", "")
            putString("google_account_name", "")
            putBoolean("is_google_logged_in", false)
            putBoolean("google_drive_backup_enabled", true)
            apply()
        }
        _googleAccountEmail.value = ""
        _googleAccountName.value = ""
        _isGoogleLoggedIn.value = false
        _isGoogleDriveBackupEnabled.value = true
        triggerButtonHaptic()
    }

    fun exportDataToJson(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val allLogs = repository.getAllLogs().first()
                val json = org.json.JSONObject()
                
                val logsArray = org.json.JSONArray()
                allLogs.forEach { log ->
                    val obj = org.json.JSONObject()
                    obj.put("amountMl", log.amountMl)
                    obj.put("timestamp", log.timestamp)
                    obj.put("dateString", log.dateString)
                    obj.put("beverageType", log.beverageType)
                    obj.put("waterEquivalency", log.waterEquivalency)
                    obj.put("waterEquivalentMl", log.waterEquivalentMl)
                    if (log.sourceDevice != null) {
                        obj.put("sourceDevice", log.sourceDevice)
                    }
                    logsArray.put(obj)
                }
                json.put("logs", logsArray)
                
                val settingsObj = org.json.JSONObject()
                settingsObj.put("daily_goal_ml", _baseDailyGoalMl.value)
                settingsObj.put("haptics_sliders_enabled", hapticsSlidersEnabled.value)
                settingsObj.put("haptics_buttons_enabled", hapticsButtonsEnabled.value)
                settingsObj.put("haptics_goal_enabled", hapticsGoalEnabled.value)
                settingsObj.put("haptics_toggles_enabled", hapticsTogglesEnabled.value)
                settingsObj.put("fireworks_feedback_enabled", fireworksFeedbackEnabled.value)
                settingsObj.put("app_language", appLanguage.value)
                settingsObj.put("vibration_duration_ms", vibrationDurationMs.value)
                settingsObj.put("vibration_gap_ms", vibrationGapMs.value)
                settingsObj.put("vibration_strength", vibrationStrength.value)
                settingsObj.put("custom_firework_haptics_enabled", customFireworkHapticsEnabled.value)
                settingsObj.put("firework_duration_ms", fireworkDurationMs.value)
                settingsObj.put("firework_gap_ms", fireworkGapMs.value)
                settingsObj.put("firework_strength", fireworkStrength.value)
                settingsObj.put("navbar_corner_radius", navbarCornerRadius.value)
                json.put("settings", settingsObj)
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onSuccess(json.toString())
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onError(e.localizedMessage ?: "Failed to generate backup")
                }
            }
        }
    }

    fun getBackupDirectory(): java.io.File {
        val app = getApplication<Application>()
        
        // 1. Try public Download directory
        try {
            val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val publicDir = java.io.File(downloadDir, "PixelWaterBackup")
            if (!publicDir.exists()) publicDir.mkdirs()
            if (publicDir.exists() && publicDir.canWrite()) {
                return publicDir
            }
        } catch (e: Exception) {
            Log.e("WaterViewModel", "Could not write to Download directory: ${e.localizedMessage}")
        }
        
        // 2. Try root external storage partition
        try {
            val extDir = android.os.Environment.getExternalStorageDirectory()
            val rawExtDir = java.io.File(extDir, "PixelWaterBackup")
            if (!rawExtDir.exists()) rawExtDir.mkdirs()
            if (rawExtDir.exists() && rawExtDir.canWrite()) {
                return rawExtDir
            }
        } catch (e: Exception) {
            Log.e("WaterViewModel", "Could not write to external storage root: ${e.localizedMessage}")
        }

        // 3. Fallback to app external files dir
        val appExtDir = java.io.File(app.getExternalFilesDir(null), "PixelWaterBackup")
        if (!appExtDir.exists()) {
            appExtDir.mkdirs()
        }
        return appExtDir
    }

    fun searchLocalBackups(): List<java.io.File> {
        val dir = getBackupDirectory()
        if (!dir.exists()) return emptyList()
        return dir.listFiles { _, name -> name.endsWith(".json", ignoreCase = true) }?.toList() ?: emptyList()
    }

    fun syncToFirestore() {
        viewModelScope.launch {
            try {
                ensureFirebaseInitialized()
                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                val user = auth.currentUser
                if (user != null) {
                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    val allLogs = repository.getAllLogs().first()
                    // Create a batch map
                    val logsData = allLogs.map { log ->
                        mapOf(
                            "id" to log.id,
                            "amountMl" to log.amountMl,
                            "timestamp" to log.timestamp,
                            "dateString" to log.dateString,
                            "beverageType" to log.beverageType,
                            "waterEquivalency" to log.waterEquivalency,
                            "waterEquivalentMl" to log.waterEquivalentMl
                        )
                    }
                    val settingsData = mapOf(
                        "daily_goal_ml" to _baseDailyGoalMl.value,
                        "haptics_sliders_enabled" to hapticsSlidersEnabled.value,
                        "haptics_buttons_enabled" to hapticsButtonsEnabled.value,
                        "haptics_goal_enabled" to hapticsGoalEnabled.value,
                        "haptics_toggles_enabled" to hapticsTogglesEnabled.value,
                        "fireworks_feedback_enabled" to fireworksFeedbackEnabled.value,
                        "app_language" to appLanguage.value,
                        "vibration_duration_ms" to vibrationDurationMs.value,
                        "vibration_gap_ms" to vibrationGapMs.value,
                        "vibration_strength" to vibrationStrength.value,
                        "custom_firework_haptics_enabled" to customFireworkHapticsEnabled.value,
                        "firework_duration_ms" to fireworkDurationMs.value,
                        "firework_gap_ms" to fireworkGapMs.value,
                        "firework_strength" to fireworkStrength.value,
                        "navbar_corner_radius" to navbarCornerRadius.value
                    )
                    val userDoc = db.collection("users").document(user.uid)
                    userDoc.set(mapOf("logs" to logsData, "settings" to settingsData))
                        .addOnSuccessListener { Log.d("WaterViewModel", "Successfully synced logs and settings to Firestore.") }
                        .addOnFailureListener { e -> Log.e("WaterViewModel", "Failed to sync to Firestore: ${e.message}") }
                }
            } catch (e: Exception) {
                Log.e("WaterViewModel", "Firestore sync error: ${e.message}")
            }
        }
    }

    fun triggerAutoBackup() {
        viewModelScope.launch {
            if (_autoLocalBackupEnabled.value) {
                backupToDeviceStorage(
                    fileName = "pixel_water_backup_auto.json",
                    onSuccess = { file ->
                        Log.d("WaterViewModel", "Auto local backup succeeded: ${file.absolutePath}")
                    },
                    onError = { err ->
                        Log.e("WaterViewModel", "Auto local backup failed: $err")
                    }
                )
            }
            if (_autoGoogleBackupEnabled.value && _isGoogleLoggedIn.value) {
                backupToGoogleDrive(
                    fileName = "pixel_water_backup_auto.json",
                    onSuccess = { _, _, file ->
                        Log.d("WaterViewModel", "Auto Google Drive backup succeeded: $file")
                    },
                    onError = { err ->
                        Log.e("WaterViewModel", "Auto Google Drive backup failed: $err")
                    }
                )
            }
            // Always attempt to sync to Firestore if the user is signed in with Firebase
            syncToFirestore()
        }
    }

    fun backupToDeviceStorage(
        fileName: String,
        onSuccess: (java.io.File) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val allLogs = repository.getAllLogs().first()
                val json = org.json.JSONObject()
                val logsArray = org.json.JSONArray()
                allLogs.forEach { log ->
                    val obj = org.json.JSONObject()
                    obj.put("amountMl", log.amountMl)
                    obj.put("timestamp", log.timestamp)
                    obj.put("dateString", log.dateString)
                    obj.put("beverageType", log.beverageType)
                    obj.put("waterEquivalency", log.waterEquivalency)
                    obj.put("waterEquivalentMl", log.waterEquivalentMl)
                    if (log.sourceDevice != null) {
                        obj.put("sourceDevice", log.sourceDevice)
                    }
                    logsArray.put(obj)
                }
                json.put("logs", logsArray)
                
                val settingsObj = org.json.JSONObject()
                settingsObj.put("daily_goal_ml", _baseDailyGoalMl.value)
                settingsObj.put("haptics_sliders_enabled", hapticsSlidersEnabled.value)
                settingsObj.put("haptics_buttons_enabled", hapticsButtonsEnabled.value)
                settingsObj.put("haptics_goal_enabled", hapticsGoalEnabled.value)
                settingsObj.put("haptics_toggles_enabled", hapticsTogglesEnabled.value)
                settingsObj.put("fireworks_feedback_enabled", fireworksFeedbackEnabled.value)
                settingsObj.put("app_language", appLanguage.value)
                settingsObj.put("vibration_duration_ms", vibrationDurationMs.value)
                settingsObj.put("vibration_gap_ms", vibrationGapMs.value)
                settingsObj.put("vibration_strength", vibrationStrength.value)
                settingsObj.put("custom_firework_haptics_enabled", customFireworkHapticsEnabled.value)
                settingsObj.put("firework_duration_ms", fireworkDurationMs.value)
                settingsObj.put("firework_gap_ms", fireworkGapMs.value)
                settingsObj.put("firework_strength", fireworkStrength.value)
                settingsObj.put("navbar_corner_radius", navbarCornerRadius.value)
                json.put("settings", settingsObj)
                
                val dir = getBackupDirectory()
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                val safeFileName = if (fileName.endsWith(".json")) fileName else "$fileName.json"
                val file = java.io.File(dir, safeFileName)
                file.writeText(json.toString())
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onSuccess(file)
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onError(e.localizedMessage ?: "Failed to write backup")
                }
            }
        }
    }

    fun restoreDataFromJson(jsonStr: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val json = org.json.JSONObject(jsonStr)
                if (json.has("logs")) {
                    val logsArray = json.getJSONArray("logs")
                    val existingLogs = repository.getAllLogs().first()
                    existingLogs.forEach { repository.deleteLog(it) }
                    
                    for (i in 0 until logsArray.length()) {
                        val obj = logsArray.getJSONObject(i)
                        repository.insertLog(
                            com.pixelwater.app.data.WaterLog(
                                amountMl = obj.getInt("amountMl"),
                                timestamp = obj.getLong("timestamp"),
                                dateString = obj.getString("dateString"),
                                beverageType = if (obj.has("beverageType")) obj.getString("beverageType") else "Water",
                                waterEquivalency = if (obj.has("waterEquivalency")) obj.getDouble("waterEquivalency").toFloat() else 1.0f,
                                waterEquivalentMl = if (obj.has("waterEquivalentMl")) obj.getInt("waterEquivalentMl") else obj.getInt("amountMl"),
                                sourceDevice = if (obj.has("sourceDevice")) obj.getString("sourceDevice") else null
                            )
                        )
                    }
                }
                if (json.has("settings")) {
                    val s = json.getJSONObject("settings")
                    if (s.has("daily_goal_ml")) updateDailyGoal(s.getInt("daily_goal_ml"))
                    if (s.has("haptics_sliders_enabled")) updateHapticsSlidersEnabled(s.getBoolean("haptics_sliders_enabled"))
                    if (s.has("haptics_buttons_enabled")) updateHapticsButtonsEnabled(s.getBoolean("haptics_buttons_enabled"))
                    if (s.has("haptics_goal_enabled")) updateHapticsGoalEnabled(s.getBoolean("haptics_goal_enabled"))
                    if (s.has("haptics_toggles_enabled")) updateHapticsTogglesEnabled(s.getBoolean("haptics_toggles_enabled"))
                    if (s.has("fireworks_feedback_enabled")) updateFireworksFeedbackEnabled(s.getBoolean("fireworks_feedback_enabled"))
                    if (s.has("app_language")) updateAppLanguage(s.getString("app_language"))
                    if (s.has("vibration_duration_ms")) updateVibrationDurationMs(s.getInt("vibration_duration_ms"))
                    if (s.has("vibration_gap_ms")) updateVibrationGapMs(s.getInt("vibration_gap_ms"))
                    if (s.has("vibration_strength")) updateVibrationStrength(s.getInt("vibration_strength"))
                    if (s.has("custom_firework_haptics_enabled")) updateCustomFireworkHapticsEnabled(s.getBoolean("custom_firework_haptics_enabled"))
                    if (s.has("firework_duration_ms")) updateFireworkDurationMs(s.getInt("firework_duration_ms"))
                    if (s.has("firework_gap_ms")) updateFireworkGapMs(s.getInt("firework_gap_ms"))
                    if (s.has("firework_strength")) updateFireworkStrength(s.getInt("firework_strength"))
                    if (s.has("navbar_corner_radius")) updateNavbarCornerRadius(s.getInt("navbar_corner_radius"))
                }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onError(e.localizedMessage ?: "Invalid backup file format")
                }
            }
        }
    }

    private val _tempGoogleOAuthToken = MutableStateFlow(prefs.getString("google_drive_access_token", "") ?: "")
    val tempGoogleOAuthToken: StateFlow<String> = _tempGoogleOAuthToken.asStateFlow()
    
    fun updateGoogleOAuthToken(token: String) {
        prefs.edit().putString("google_drive_access_token", token).apply()
        _tempGoogleOAuthToken.value = token
    }

    fun backupToGoogleDrive(
        fileName: String,
        onSuccess: (logCount: Int, prefCount: Int, file: String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val allLogs = repository.getAllLogs().first()
                val settingsMap = mapOf(
                    "daily_goal_ml" to _baseDailyGoalMl.value,
                    "haptics_sliders_enabled" to hapticsSlidersEnabled.value,
                    "haptics_buttons_enabled" to hapticsButtonsEnabled.value,
                    "haptics_goal_enabled" to hapticsGoalEnabled.value,
                    "haptics_toggles_enabled" to hapticsTogglesEnabled.value,
                    "fireworks_feedback_enabled" to fireworksFeedbackEnabled.value,
                    "app_language" to appLanguage.value,
                    "ai_provider" to aiProvider.value,
                    "vibration_duration_ms" to vibrationDurationMs.value,
                    "vibration_gap_ms" to vibrationGapMs.value,
                    "vibration_strength" to vibrationStrength.value,
                    "custom_firework_haptics_enabled" to customFireworkHapticsEnabled.value,
                    "firework_duration_ms" to fireworkDurationMs.value,
                    "firework_gap_ms" to fireworkGapMs.value,
                    "firework_strength" to fireworkStrength.value,
                    "navbar_corner_radius" to navbarCornerRadius.value,
                    "vibration_preset" to vibrationPatternPreset.value.name
                )
                
                val rootJson = org.json.JSONObject()
                val logsArray = org.json.JSONArray()
                allLogs.forEach { log ->
                    val obj = org.json.JSONObject()
                    obj.put("id", log.id)
                    obj.put("amountMl", log.amountMl)
                    obj.put("beverageType", log.beverageType)
                    obj.put("timestamp", log.timestamp)
                    obj.put("dateString", log.dateString)
                    obj.put("waterEquivalentMl", log.waterEquivalentMl)
                    if (log.sourceDevice != null) {
                        obj.put("sourceDevice", log.sourceDevice)
                    }
                    logsArray.put(obj)
                }
                rootJson.put("logs", logsArray)
                
                val settingsJson = org.json.JSONObject()
                settingsMap.forEach { (k, v) -> settingsJson.put(k, v) }
                rootJson.put("settings", settingsJson)
                
                val backupDataStr = rootJson.toString()
                
                var accessToken = prefs.getString("google_drive_access_token", null)
                if (accessToken.isNullOrEmpty()) {
                    accessToken = _tempGoogleOAuthToken.value.takeIf { it.isNotEmpty() }
                }
                
                if (accessToken.isNullOrEmpty()) {
                    ensureFirebaseInitialized()
                    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                    if (auth.currentUser != null) {
                        syncToFirestore()
                        onSuccess(allLogs.size, settingsMap.size, "Firestore Cloud Storage (Auto)")
                        return@launch
                    }
                    throw Exception("Google Drive Authentication required. Connect Google account first.")
                }

                val client = okhttp3.OkHttpClient()
                
                val searchUrl = "https://www.googleapis.com/drive/v3/files?q=name='$fileName'&spaces=appDataFolder"
                val searchRequest = okhttp3.Request.Builder()
                    .url(searchUrl)
                    .header("Authorization", "Bearer $accessToken")
                    .get()
                    .build()
                
                var existingFileId: String? = null
                client.newCall(searchRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (body != null) {
                            val jsonResp = org.json.JSONObject(body)
                            val files = jsonResp.getJSONArray("files")
                            if (files.length() > 0) {
                                existingFileId = files.getJSONObject(0).getString("id")
                            }
                        }
                    }
                }
                
                val boundary = "PixelWaterBoundary"
                val mediaTypeJson = "application/json; charset=UTF-8".toMediaTypeOrNull()
                
                val metadataJson = org.json.JSONObject()
                metadataJson.put("name", fileName)
                if (existingFileId == null) {
                    val parentsArr = org.json.JSONArray()
                    parentsArr.put("appDataFolder")
                    metadataJson.put("parents", parentsArr)
                }

                val requestBody = okhttp3.MultipartBody.Builder(boundary)
                    .setType(okhttp3.MultipartBody.FORM)
                    .addPart(
                        okhttp3.Headers.Builder().add("Content-Type", "application/json; charset=UTF-8").build(),
                        metadataJson.toString().toRequestBody(mediaTypeJson)
                    )
                    .addPart(
                        okhttp3.Headers.Builder().add("Content-Type", "application/json").build(),
                        backupDataStr.toRequestBody(mediaTypeJson)
                    )
                    .build()

                val uploadUrl = if (existingFileId != null) {
                    "https://www.googleapis.com/upload/drive/v3/files/$existingFileId?uploadType=multipart"
                } else {
                    "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
                }

                val uploadRequest = okhttp3.Request.Builder()
                    .url(uploadUrl)
                    .header("Authorization", "Bearer $accessToken")
                    .method(if (existingFileId != null) "PATCH" else "POST", requestBody)
                    .build()

                client.newCall(uploadRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val respBody = response.body?.string() ?: ""
                        val respObj = org.json.JSONObject(respBody)
                        val fileId = respObj.optString("id", "GDrive")
                        onSuccess(allLogs.size, settingsMap.size, "Google Drive (File ID: $fileId)")
                    } else {
                        throw Exception("Google Drive upload error: ${response.code} ${response.message}")
                    }
                }

            } catch (e: Exception) {
                try {
                    ensureFirebaseInitialized()
                    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                    if (auth.currentUser != null) {
                        syncToFirestore()
                        val allLogs = repository.getAllLogs().first()
                        onSuccess(allLogs.size, 15, "Firestore Cloud Storage (Fallback)")
                        return@launch
                    }
                } catch (fe: Exception) {}
                onError(e.localizedMessage ?: "Unknown Google Drive connection error")
            }
        }
    }

    fun restoreFromGoogleDrive(
        fileName: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                var accessToken = prefs.getString("google_drive_access_token", null)
                if (accessToken.isNullOrEmpty()) {
                    accessToken = _tempGoogleOAuthToken.value.takeIf { it.isNotEmpty() }
                }

                if (accessToken.isNullOrEmpty()) {
                    ensureFirebaseInitialized()
                    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                    if (auth.currentUser != null) {
                        restoreFromFirestore {
                            onSuccess("Successfully restored settings and logs from Firestore backup!")
                        }
                        return@launch
                    }
                    throw Exception("Google Drive Authentication required. Connect Google account first.")
                }

                val client = okhttp3.OkHttpClient()
                
                val searchUrl = "https://www.googleapis.com/drive/v3/files?q=name='$fileName'&spaces=appDataFolder"
                val searchRequest = okhttp3.Request.Builder()
                    .url(searchUrl)
                    .header("Authorization", "Bearer $accessToken")
                    .get()
                    .build()
                
                var fileId: String? = null
                client.newCall(searchRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (body != null) {
                            val jsonResp = org.json.JSONObject(body)
                            val files = jsonResp.getJSONArray("files")
                            if (files.length() > 0) {
                                fileId = files.getJSONObject(0).getString("id")
                            }
                        }
                    }
                }

                if (fileId == null) {
                    ensureFirebaseInitialized()
                    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                    if (auth.currentUser != null) {
                        restoreFromFirestore {
                            onSuccess("No Drive file found. Restored from secure Firestore automatically!")
                        }
                        return@launch
                    }
                    throw Exception("Backup file '$fileName' not found on Google Drive.")
                }

                val downloadUrl = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
                val downloadRequest = okhttp3.Request.Builder()
                    .url(downloadUrl)
                    .header("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                client.newCall(downloadRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val backupJsonStr = response.body?.string()
                        if (backupJsonStr != null) {
                            viewModelScope.launch {
                                restoreDataFromJson(
                                    backupJsonStr,
                                    onSuccess = { onSuccess("Successfully restored from Google Drive!") },
                                    onError = { onError(it) }
                                )
                            }
                        } else {
                            throw Exception("Empty backup file received.")
                        }
                    } else {
                        throw Exception("Failed to download file from Google Drive: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                try {
                    ensureFirebaseInitialized()
                    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                    if (auth.currentUser != null) {
                        restoreFromFirestore {
                            onSuccess("Drive restore failed. Restored from secure Firestore automatically!")
                        }
                        return@launch
                    }
                } catch (fe: Exception) {}
                onError(e.localizedMessage ?: "Unknown Google Drive query error")
            }
        }
    }

    fun resetUpdateStatus() {
        _updateStatus.value = UpdateStatus.Idle
    }

    fun generateRuleBasedProgressSummary(
        logs: List<WaterLog>,
        dailyGoal: Int,
        lang: String,
        targetDateStr: String
    ): String {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
        val daysToSubtract = when (dayOfWeek) {
            java.util.Calendar.SUNDAY -> 6
            java.util.Calendar.MONDAY -> 0
            else -> dayOfWeek - java.util.Calendar.MONDAY
        }
        cal.add(java.util.Calendar.DAY_OF_YEAR, -daysToSubtract)
        val currentWeekStart = cal.timeInMillis

        val isMonday = (dayOfWeek == java.util.Calendar.MONDAY)
        val logsForAnalysis = if (isMonday) {
            val prevWeekStart = currentWeekStart - 7 * 24 * 3600 * 1000L
            logs.filter { it.timestamp in prevWeekStart until currentWeekStart }
        } else {
            logs.filter { it.timestamp >= currentWeekStart }
        }
        
        val intakeByDay = logsForAnalysis.groupBy { it.dateString }
            .mapValues { it.value.sumOf { log -> log.amountMl } }

        val daysMetThisWeek = intakeByDay.count { (dateStr, totalMl) -> totalMl >= getDailyGoalForDate(dateStr) }
        val totalVolumeThisWeek = logsForAnalysis.sumOf { it.amountMl }
        val averageIntakeThisWeek = if (intakeByDay.isNotEmpty()) totalVolumeThisWeek / intakeByDay.size else 0

        var peakDayName = ""
        var peakDayVolume = 0
        if (intakeByDay.isNotEmpty()) {
            val peakDayEntry = intakeByDay.maxByOrNull { it.value }
            if (peakDayEntry != null) {
                peakDayVolume = peakDayEntry.value
                val peakCal = java.util.Calendar.getInstance().apply {
                    val firstLogForDay = logsForAnalysis.firstOrNull { it.dateString == peakDayEntry.key }
                    if (firstLogForDay != null) {
                        timeInMillis = firstLogForDay.timestamp
                    }
                }
                peakDayName = when (peakCal.get(java.util.Calendar.DAY_OF_WEEK)) {
                    java.util.Calendar.MONDAY -> if (lang == "el") "Δευτέρα" else "Monday"
                    java.util.Calendar.TUESDAY -> if (lang == "el") "Τρίτη" else "Tuesday"
                    java.util.Calendar.WEDNESDAY -> if (lang == "el") "Τετάρτη" else "Wednesday"
                    java.util.Calendar.THURSDAY -> if (lang == "el") "Πέμπτη" else "Thursday"
                    java.util.Calendar.FRIDAY -> if (lang == "el") "Παρασκευή" else "Friday"
                    java.util.Calendar.SATURDAY -> if (lang == "el") "Σάββατο" else "Saturday"
                    java.util.Calendar.SUNDAY -> if (lang == "el") "Κυριακή" else "Sunday"
                    else -> ""
                }
            }
        }

        val caffeineTerms = listOf("coffee", "tea", "caffeine", "καφέ", "τσάι")
        val caffeineRich = logsForAnalysis.any { log -> 
            caffeineTerms.any { term -> log.beverageType.lowercase().contains(term) } 
        }

        val calendarInstance = java.util.Calendar.getInstance(getUserTimeZone())
        val currentHour = calendarInstance.get(java.util.Calendar.HOUR_OF_DAY)
        val isNightOrEarlyMorning = currentHour >= 22 || currentHour < 7

        val summary = StringBuilder()
        
        val targetDateIntake = logs.filter { it.dateString == targetDateStr }.sumOf { it.waterEquivalentMl }
        val targetDateGoal = getDailyGoalForDate(targetDateStr)
        val todayPct = if (targetDateGoal > 0) ((targetDateIntake.toDouble() / targetDateGoal.toDouble()) * 100).toInt() else 0
        val currentStreakVal = _streak.value
        val weatherTemp = _weatherTemperature.value
        val city = _locationCity.value
        val fullDateFormat = java.text.SimpleDateFormat("EEEE, MMMM d, yyyy", if (lang == "el") java.util.Locale("el") else java.util.Locale.US)
        val formattedDate = fullDateFormat.format(java.util.Date())

        val oneHourAgo = System.currentTimeMillis() - 3600 * 1000L
        val recentLogs60 = logs.filter { it.timestamp >= oneHourAgo }.sortedBy { it.timestamp }
        val recent60MinIntake = recentLogs60.sumOf { it.waterEquivalentMl }
        val isRapidOverhydrated = recent60MinIntake > 1000
        val excessRapidMl = (recent60MinIntake - 1000).coerceAtLeast(0)
        val firstLogTs = recentLogs60.firstOrNull()?.timestamp ?: (System.currentTimeMillis() - 3600 * 1000L)
        val totalClearanceMs = (recent60MinIntake / 14.5 * 60_000.0).toLong()
        val targetSafeTs = maxOf(System.currentTimeMillis() + 60_000L, firstLogTs + totalClearanceMs)
        val rapidClearanceMin = maxOf(1, kotlin.math.ceil((targetSafeTs - System.currentTimeMillis()) / 60_000.0).toInt())

        val isDailyOverdone = (targetDateGoal > 0 && targetDateIntake >= (targetDateGoal * 1.25).toInt()) || targetDateIntake >= 4000
        val hasOverdone = isRapidOverhydrated || isDailyOverdone
        
        if (hasOverdone) {
            if (lang == "el") {
                val warnText = when {
                    isRapidOverhydrated && isDailyOverdone -> "⚠️ Προσοχή: Έχετε καταναλώσει ${recent60MinIntake}ml την τελευταία ώρα (+${excessRapidMl}ml υπέρβαση νεφρικής αποβολής) και συνολικά ${targetDateIntake}ml σήμερα. Σταματήστε αμέσως την κατανάλωση υγρών για ~${rapidClearanceMin} λεπτά.\n\n"
                    isRapidOverhydrated -> "⚠️ Προσοχή: Έχετε καταναλώσει ${recent60MinIntake}ml την τελευταία ώρα, υπερβαίνοντας το όριο νεφρικής αποβολής (~1.000 ml/ώρα) κατά +${excessRapidMl}ml. Συνιστάται παύση κατανάλωσης υγρών για ~${rapidClearanceMin} λεπτά.\n\n"
                    targetDateIntake >= (targetDateGoal * 1.5).coerceAtLeast(5000.0) -> "⚠️ Κάνετε υπερβολική κατανάλωση νερού (${targetDateIntake}ml)! Υπάρχει κρίσιμος κίνδυνος εισροής μεγάλων ποσοτήτων υγρών (οσμωτικό οίδημα). Σταματήστε αμέσως την κατανάλωση και παρακολουθήστε για πονοκέφαλο ή κόπωση.\n\n"
                    targetDateIntake >= (targetDateGoal * 1.35).coerceAtLeast(4000.0) -> "⚠️ Μεγάλη κατανάλωση νερού (${targetDateIntake}ml)! Τα νεφρά σας δέχονται σημαντικό φορτίο φιλτραρίσματος. Προτείνεται να κάνετε μια παύση για μερικές ώρες.\n\n"
                    else -> "⚠️ Έχετε ξεπεράσει σημαντικά τον ημερήσιο στόχο σας (${targetDateIntake}ml). Συνιστάται να ελαττώσετε τον ρυθμό κατανάλωσης, καθώς η περίσσεια νερού δεν προσφέρει επιπλέον οφέλη.\n\n"
                }
                summary.append(warnText)
            } else {
                val warnText = when {
                    isRapidOverhydrated && isDailyOverdone -> "⚠️ Rapid overhydration alert: You consumed ${recent60MinIntake}ml in the past hour (+${excessRapidMl}ml excess) with ${targetDateIntake}ml total today. Immediately pause fluid intake for ~${rapidClearanceMin} minutes.\n\n"
                    isRapidOverhydrated -> "⚠️ Rapid fluid intake warning: You consumed ${recent60MinIntake}ml in the past hour (+${excessRapidMl}ml over the ~1,000 ml/hr renal clearance limit). Pause fluid intake for ~${rapidClearanceMin} min to allow safe renal clearance.\n\n"
                    targetDateIntake >= (targetDateGoal * 1.5).coerceAtLeast(5000.0) -> "⚠️ You are consuming an excessive amount of water (${targetDateIntake}ml)! There is a critical osmotic swelling risk. Halt active water drinking immediately and observe for headaches or fatigue.\n\n"
                    targetDateIntake >= (targetDateGoal * 1.35).coerceAtLeast(4000.0) -> "⚠️ High fluid intake recorded (${targetDateIntake}ml)! Significant filtration load is being placed on your kidneys. Please pause fluid intake for a few hours.\n\n"
                    else -> "⚠️ You have exceeded your daily goal substantially (${targetDateIntake}ml). It is recommended to pace your intake slower as extra water brings no physiological benefits.\n\n"
                }
                summary.append(warnText)
            }
        }
        
        if (lang == "el") {
            summary.append("Σήμερα ($formattedDate), έχετε καλύψει το $todayPct% του ημερήσιου στόχου σας ($targetDateIntake / $targetDateGoal ml). ")
            if (currentStreakVal > 0) {
                summary.append("🔥 Διατηρείτε σερί $currentStreakVal ημερών! ")
            }
            if (isMonday) {
                summary.append("Την προηγούμενη εβδομάδα, επιτύχατε το στόχο σας $daysMetThisWeek από τις 7 ημέρες, με μέσο όρο ${averageIntakeThisWeek} ml/ημέρα. ")
            } else {
                summary.append("Στη γράφημα της εβδομάδας, έχετε επιτύχει το στόχο σας $daysMetThisWeek από τις 7 ημέρες, με μέσο όρο ${averageIntakeThisWeek} ml/ημέρα. ")
            }
            if (weatherTemp != null) {
                val cityPart = if (city.isNotBlank()) " στη $city" else ""
                summary.append("Με τη θερμοκρασία στους ${String.format(java.util.Locale.US, "%.1f", weatherTemp)}°C$cityPart, φροντίστε να διατηρείτε σταθερή ροή ενυδάτωσης. ")
            }
            if (isNightOrEarlyMorning) {
                if (daysMetThisWeek <= 2) {
                    summary.append("Πιείτε ένα ποτήρι νερό το πρωί!")
                } else if (daysMetThisWeek <= 5) {
                    if (caffeineRich) summary.append("Ισορροπήστε τον καφέ με νερό.") else summary.append("Εξαιρετική προσπάθεια!")
                } else {
                    summary.append("Είστε υπόδειγμα ενυδάτωσης!")
                }
            } else {
                if (caffeineRich) {
                    summary.append("Παρατηρήσαμε ότι καταναλώνετε ροφήματα με καφέ ή τσάι. Λόγω της διουρητικής τους δράσης, είναι σημαντικό να αναπληρώνετε την απώλεια υγρών πίνοντας ένα επιπλέον ποτήρι καθαρό νερό για κάθε φλιτζάνι.")
                } else if (peakDayName.isNotEmpty() && peakDayVolume > 0) {
                    summary.append("Η καλύτερη ημέρα σας ήταν η $peakDayName με κατανάλωση $peakDayVolume ml! Προσπαθήστε να επαναλάβετε αυτή τη συχνότητα.")
                } else {
                    summary.append("Η σταθερή πρόσληψη υγρών κρατά τα επίπεδα ενέργειάς σας υψηλά και βελτιώνει τη συγκέντρωση.")
                }
            }
        } else {
            summary.append("Today ($formattedDate), you have completed $todayPct% of your daily goal ($targetDateIntake / $targetDateGoal ml). ")
            if (currentStreakVal > 0) {
                summary.append("🔥 You are on a $currentStreakVal-day goal streak! ")
            }
            if (isMonday) {
                summary.append("Last week, you met your goal $daysMetThisWeek of 7 days, averaging ${averageIntakeThisWeek} ml/day. ")
            } else {
                summary.append("In your weekly graph, you met your goal $daysMetThisWeek of 7 days, averaging ${averageIntakeThisWeek} ml/day. ")
            }
            if (weatherTemp != null) {
                val cityPart = if (city.isNotBlank()) " in $city" else ""
                summary.append("With current temperatures around ${String.format(java.util.Locale.US, "%.1f", weatherTemp)}°C$cityPart, maintain a steady hydration pace. ")
            }
            if (isNightOrEarlyMorning) {
                if (daysMetThisWeek <= 2) {
                    summary.append("Try drinking a glass of water every morning!")
                } else if (daysMetThisWeek <= 5) {
                    if (caffeineRich) summary.append("Balance coffee with extra water.") else summary.append("Great momentum, keep up!")
                } else {
                    summary.append("Outstanding hydration consistency!")
                }
            } else {
                if (caffeineRich) {
                    summary.append("We noticed you logged caffeinated beverages like coffee or tea. Remember that caffeine has natural diuretic properties, so counterbalancing each serving with an extra cup of water is key.")
                } else if (peakDayName.isNotEmpty() && peakDayVolume > 0) {
                    summary.append("Your peak intake reached $peakDayVolume ml on $peakDayName! Replicating that frequency makes maintaining your hydration habit effortless.")
                } else {
                    summary.append("Keeping your absolute daily fluid intake balanced supports immediate cognitive speed and keeps joints nicely lubricated.")
                }
            }
        }

        return summary.toString()
    }

    fun getUserTimeZone(): java.util.TimeZone {
        val savedTz = prefs.getString("weather_timezone", "")
        if (!savedTz.isNullOrBlank()) {
            try {
                return java.util.TimeZone.getTimeZone(savedTz)
            } catch (e: Exception) {
                // fall through
            }
        }
        return java.util.TimeZone.getDefault()
    }

    fun getSunsetHourOfDay(calendar: java.util.Calendar): Double {
        val lat = prefs.getFloat("weather_latitude", 37.9838f).toDouble()
        val lon = prefs.getFloat("weather_longitude", 23.7275f).toDouble()
        
        val todayStr = getCurrentDateString()
        val savedSunsetStr = prefs.getString("weather_sunset_$todayStr", "")
        if (!savedSunsetStr.isNullOrBlank()) {
            try {
                if (savedSunsetStr.contains("T")) {
                    val timePart = savedSunsetStr.substringAfter("T")
                    val parts = timePart.split(":")
                    if (parts.size >= 2) {
                        val sunsetHour = parts[0].toDoubleOrNull()
                        val sunsetMin = parts[1].toDoubleOrNull()
                        if (sunsetHour != null && sunsetMin != null) {
                            return sunsetHour + (sunsetMin / 60.0)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("WaterViewModel", "Error parsing saved sunset: $savedSunsetStr", e)
            }
        }
        
        val dayOfYear = calendar.get(java.util.Calendar.DAY_OF_YEAR)
        val latRad = Math.toRadians(lat)
        val declination = Math.toRadians(23.45 * kotlin.math.sin(2 * Math.PI * (284 + dayOfYear) / 365.0))
        val cosH = -kotlin.math.tan(latRad) * kotlin.math.tan(declination)
        val hourAngleDeg = when {
            cosH <= -1.0 -> 180.0
            cosH >= 1.0 -> 0.0
            else -> Math.toDegrees(kotlin.math.acos(cosH))
        }
        val sunsetSolarTime = 12.0 + (hourAngleDeg / 15.0)
        val tzOffsetHours = calendar.timeZone.getOffset(calendar.timeInMillis) / 3600000.0
        val localTime = sunsetSolarTime - (lon / 15.0) + tzOffsetHours
        return localTime.coerceIn(16.5, 22.5)
    }

    fun getContextualGreeting(hour: Int, lang: String): String {
        val calendar = java.util.Calendar.getInstance(getUserTimeZone())
        return getContextualGreeting(calendar, lang)
    }

    fun getContextualGreeting(calendar: java.util.Calendar, lang: String): String {
        val isGreek = lang == "el"
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val includeFaces = _showInsightFaces.value
        
        if (_isRaining.value) {
            val rainGreetingsEng = if (includeFaces) {
                listOf(
                    "looks like it's raining ◉\u203f\u25c9\n\n",
                    "be sure take an umbrella if you are going out \u25d5\u203f\u25d5\n\n",
                    "Pitter-patter! Do you hear the raindrops? Stay cozy, keep warm, and hydrate! \ud83c\udf27\ufe0f(\uff65\u03c9\uff65)\n\n",
                    "The clouds are drinking rainwater, and you should drink your water too! (\u273f\u25e1\u203f\u25e1)\n\n",
                    "Dancing in the rain is fun, but cozying up with a glass of pure water is better! \u2602\ufe0f(^\u203f^)\n\n",
                    "Water is falling from the sky! Perfect time for a fresh sip! \ud83c\udf22(\u0e40\u25e1\u0e40)\n\n"
                )
            } else {
                listOf(
                    "looks like it's raining\n\n",
                    "be sure take an umbrella if you are going out\n\n",
                    "Pitter-patter! Do you hear the raindrops? Stay cozy, keep warm, and hydrate! \ud83c\udf27\ufe0f\n\n",
                    "The clouds are drinking rainwater, and you should drink your water too!\n\n",
                    "Dancing in the rain is fun, but cozying up with a glass of pure water is better! \u2602\ufe0f\n\n",
                    "Water is falling from the sky! Perfect time for a fresh sip! \ud83c\udf22\n\n"
                )
            }
            val rainGreetingsGreek = if (includeFaces) {
                listOf(
                    "\u03a6\u03b1\u03af\u03bd\u03b5\u03c4\u03b1\u03b9 \u03c0\u03c9\u03c2 \u03b2\u03c1\u03ad\u03c7\u03b5\u03b9 \u25c9\u203f\u25c9\n\n",
                    "\u03a3\u03b9\u03b3\u03bf\u03c5\u03c1\u03ad\u03c8\u03bf\u03c5 \u03cc\u03c4\u03b9 \u03b8\u03b1 \u03c0\u03ac\u03c1\u03b5\u03b9\u03c2 \u03bf\u03bc\u03c0\u03c1\u03ad\u03bb\u03b1 \u03b1\u03bd \u03b2\u03b3\u03b5\u03b9\u03c2 \u03ad\u03be\u03c9 \u25d5\u203f\u25d5\n\n",
                    "\u038a\u03ba\u03bf\u03c5 \u03c4\u03b7 \u03b2\u03c1\u03bf\u03c7\u03ae! \u0389\u03c1\u03b1 \u03bd\u03b1 \u03bc\u03b5\u03af\u03bd\u03b5\u03b9\u03c2 \u03b6\u03b5\u03c3\u03c4\u03cc\u03c2 \u03ba\u03b1\u03b9 \u03b5\u03bd\u03c5\u03b4\u03b1\u03c4\u03c9\u03bc\u03ad\u03bd\u03bf\u03c2 \ud83c\udf27\ufe0f(\uff65\u03c9\uff65)\n\n",
                    "\u03a4\u03b1 \u03c3\u03cd\u03bd\u03bd\u03b5\u03c6\u03b1 \u03c0\u03af\u03bd\u03bf\u03c5\u03bd \u03bd\u03b5\u03c1\u03cc \u03b2\u03c1\u03bf\u03c7\u03ae\u03c2, \u03ba\u03b9 \u03b5\u03c3\u03cd \u03c0\u03c1\u03ad\u03c0\u03b5\u03b9 \u03bd\u03b1 \u03c0\u03b9\u03be\u03b9\u03c2 \u03c4\u03bf \u03b4\u03b9\u03ba\u03cc \u03c3\u03bf\u03c5! (\u273f\u25e1\u203f\u25e1)\n\n",
                    "\u039f \u03c7\u03bf\u03c1\u03cc\u03c2 \u03c3\u03c4\u03b7 \u03b2\u03c1\u03bf\u03c7\u03ae \u03b5\u03af\u03bd\u03b1\u03b9 \u03c9\u03c1\u03b1\u03af\u03bf\u03c2, \u03b1\u03bb\u03bb\u03ac \u03ad\u03bd\u03b1 \u03c0\u03bf\u03c4\u03ae\u03c1\u03b9 \u03bd\u03b5\u03c1\u03cc \u03c3\u03c4\u03b7 \u03b6\u03b5\u03c3\u03c4\u03b1\u03c3\u03b9\u03ac \u03b5\u03af\u03bd\u03b1\u03b9 \u03b1\u03ba\u03cc\u03bc\u03b1 \u03ba\u03b1\u03bb\u03cd\u03c4\u03b5\u03c1\u03bf! \u2602\ufe0f(^\u203f^)\n\n",
                    "\u03a4\u03bf \u03bd\u03b5\u03c1\u03cc \u03c0\u03ad\u03c6\u03c4\u03b5\u03b9 \u03b1\u03c0\u03cc \u03c4\u03bf\u03bd \u03bf\u03c5\u03c1\u03b1\u03bd\u03cc! \u03a4\u03ad\u03bb\u03b5\u03b9\u03b1 \u03c3\u03c4\u03b9\u03b3\u03bc\u03ae \u03b3\u03b9\u03b1 \u03bc\u03b9\u03b1 \ud83c\udf22(\u0e40\u25e1\u0e40) \u03b3\u03bf\u03c5\u03bb\u03b9\u03ac!\n\n"
                )
            } else {
                listOf(
                    "\u03a6\u03b1\u03af\u03bd\u03b5\u03c4\u03b1\u03b9 \u03c0\u03c9\u03c2 \u03b2\u03c1\u03ad\u03c7\u03b5\u03b9\n\n",
                    "\u03a3\u03b9\u03b3\u03bf\u03c5\u03c1\u03ad\u03c8\u03bf\u03c5 \u03cc\u03c4\u03b9 \u03b8\u03b1 \u03c0\u03ac\u03c1\u03b5\u03b9\u03c2 \u03bf\u03bc\u03c0\u03c1\u03ad\u03bb\u03b1 \u03b1\u03bd \u03b2\u03b3\u03b5\u03b9\u03c2 \u03ad\u03be\u03c9\n\n",
                    "\u038a\u03ba\u03bf\u03c5 \u03c4\u03b7 \u03b2\u03c1\u03bf\u03c7\u03ae! \u0389\u03c1\u03b1 \u03bd\u03b1 \u03bc\u03b5\u03af\u03bd\u03b5\u03b9\u03c2 \u03b6\u03b5\u03c3\u03c4\u03cc\u03c2 \u03ba\u03b1\u03b9 \u03b5\u03bd\u03c5\u03b4\u03b1\u03c4\u03c9\u03bc\u03ad\u03bd\u03bf\u03c2 \ud83c\udf27\ufe0f\n\n",
                    "\u03a4\u03b1 \u03c3\u03cd\u03bd\u03bd\u03b5\u03c6\u03b1 \u03c0\u03af\u03bd\u03bf\u03c5\u03bd \u03bd\u03b5\u03c1\u03cc \u03b2\u03c1\u03bf\u03c7\u03ae\u03c2, \u03ba\u03b9 \u03b5\u03c3\u03cd \u03c0\u03c1\u03ad\u03c0\u03b5\u03b9 \u03bd\u03b1 \u03c0\u03b9\u03b5\u03b9\u03c2 \u03c4\u03bf \u03b4\u03b9\u03ba\u03cc \u03c3\u03bf\u03c5!\n\n",
                    "\u039f \u03c7\u03bf\u03c1\u03cc\u03c2 \u03c3\u03c4\u03b7 \u03b2\u03c1\u03bf\u03c7\u03ae \u03b5\u03af\u03bd\u03b1\u03b9 \u03c9\u03c1\u03b1\u03af\u03bf\u03c2, \u03b1\u03bb\u03bb\u03ac \u03ad\u03bd\u03b1 \u03c0\u03bf\u03c4\u03ae\u03c1\u03b9 \u03bd\u03b5\u03c1\u03cc \u03c3\u03c4\u03b7 \u03b6\u03b5\u03c3\u03c4\u03b1\u03c3\u03b9\u03ac \u03b5\u03af\u03bd\u03b1\u03b9 \u03b1\u03ba\u03cc\u03bc\u03b1 \u03ba\u03b1\u03bb\u03cd\u03c4\u03b5\u03c1\u03bf! \u2602\ufe0f\n\n",
                    "\u03a4\u03bf \u03bd\u03b5\u03c1\u03cc \u03c0\u03ad\u03c6\u03c4\u03b5\u03b9 \u03b1\u03c0\u03cc \u03c4\u03bf\u03bd \u03bf\u03c5\u03c1\u03b1\u03bd\u03cc! \u03a4\u03ad\u03bb\u03b5\u03b9\u03b1 \u03c3\u03c4\u03b9\u03b3\u03bc\u03ae \u03b3\u03b9\u03b1 \u03bc\u03b9\u03b1 \ud83c\udf22 \u03b3\u03bf\u03c5\u03bb\u03b9\u03ac!\n\n"
                )
            }
            val index = hour % rainGreetingsEng.size
            return if (isGreek) rainGreetingsGreek[index] else rainGreetingsEng[index]
        }

        val minute = calendar.get(java.util.Calendar.MINUTE)
        val currentDecimalHour = hour + (minute / 60.0)
        val sunsetHour = getSunsetHourOfDay(calendar)
        
        return when {
            currentDecimalHour >= 5.0 && currentDecimalHour < 12.0 -> {
                val faces = listOf("◉‿◉", "❛ ᴗ ❛")
                val face = if (includeFaces) " " + faces[hour % faces.size] else ""
                if (isGreek) "Καλημέρα$face\n\n" else "Good morning$face\n\n"
            }
            currentDecimalHour >= 12.0 && currentDecimalHour < 16.0 -> {
                val faces = listOf("◕‿◕", "╹▽╹")
                val face = if (includeFaces) " " + faces[hour % faces.size] else ""
                if (isGreek) "Καλό μεσημέρι$face\n\n" else "Good afternoon$face\n\n"
            }
            currentDecimalHour >= 16.0 && currentDecimalHour < sunsetHour -> {
                val faces = listOf("◕‿◕", "╹▽╹")
                val face = if (includeFaces) " " + faces[hour % faces.size] else ""
                if (isGreek) "Καλό απόγευμα$face\n\n" else "Good afternoon$face\n\n"
            }
            currentDecimalHour >= sunsetHour && currentDecimalHour < sunsetHour + 1.0 -> {
                val greetingsEn = if (includeFaces) listOf("Sunset 🌇", "Sunset ( ◜‿◝ )") else listOf("Sunset 🌇", "Sunset 🌇")
                val greetingsEl = if (includeFaces) listOf("Ηλιοβασίλεμα 🌇", "Ηλιοβασίλεμα ( ◜‿◝ )") else listOf("Ηλιοβασίλεμα 🌇", "Ηλιοβασίλεμα 🌇")
                val index = ((System.currentTimeMillis() / 1000) % 2).toInt()
                if (isGreek) "${greetingsEl[index]}\n\n" else "${greetingsEn[index]}\n\n"
            }
            currentDecimalHour >= sunsetHour + 1.0 && currentDecimalHour < 24.0 -> {
                val faces = listOf("ʘ‿ʘ", "￣︶￣")
                val face = if (includeFaces) " " + faces[hour % faces.size] else ""
                if (isGreek) "Καλό βράδυ$face\n\n" else "Good evening$face\n\n"
            }
            else -> {
                val wishesGreek = listOf("Καληνύχτα", "Όνειρα γλυκά", "Σου εύχομαι να έχεις όνειρα γεμάτα ταξίδια")
                val wishesEng = listOf("Goodnight", "Sweet dreams", "Wishing you travel-filled dreams")
                val icons = listOf("🌃", "🌉", "🌌")
                val index = ((System.currentTimeMillis() / 1000) % 3).toInt()
                val icon = icons[index]
                if (isGreek) {
                    "${wishesGreek[index]} $icon\n\n"
                } else {
                    "${wishesEng[index]} $icon\n\n"
                }
            }
        }
    }

    fun refreshWeeklyProgressSummary(forceGemini: Boolean = false, logsOverride: List<WaterLog>? = null) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isGeneratingWeeklySummary.value = true
            try {
                val logs = logsOverride ?: repository.getAllLogs().first()
                val targetDateStr = _currentDate.value
                val targetGoal = getDailyGoalForDate(targetDateStr)
                val lang = appLanguage.value
                
                val targetDateIntake = logs.filter { it.dateString == targetDateStr }.sumOf { it.waterEquivalentMl }
                
                val oneHourAgo = System.currentTimeMillis() - 3600 * 1000L
                val recentLogs60 = logs.filter { it.timestamp >= oneHourAgo }.sortedBy { it.timestamp }
                val recent60MinIntake = recentLogs60.sumOf { it.waterEquivalentMl }
                val isRapidOverhydrated = recent60MinIntake > 1000
                val excessRapidMl = (recent60MinIntake - 1000).coerceAtLeast(0)
                val firstLogTs = recentLogs60.firstOrNull()?.timestamp ?: (System.currentTimeMillis() - 3600 * 1000L)
                val totalClearanceMs = (recent60MinIntake / 14.5 * 60_000.0).toLong()
                val targetSafeTs = maxOf(System.currentTimeMillis() + 60_000L, firstLogTs + totalClearanceMs)
                val rapidClearanceMin = maxOf(1, kotlin.math.ceil((targetSafeTs - System.currentTimeMillis()) / 60_000.0).toInt())

                val isDailyOverdone = (targetGoal > 0 && targetDateIntake >= (targetGoal * 1.25).toInt()) || targetDateIntake >= 4000
                val hasOverdone = isRapidOverhydrated || isDailyOverdone

                val baseText = generateRuleBasedProgressSummary(logs, targetGoal, lang, targetDateStr)
                
                val apiKey = getEffectiveGeminiApiKey()
                if (apiKey != null) {
                    val cal = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    val dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
                    val daysToSubtract = when (dayOfWeek) {
                        java.util.Calendar.SUNDAY -> 6
                        java.util.Calendar.MONDAY -> 0
                        else -> dayOfWeek - java.util.Calendar.MONDAY
                    }
                    cal.add(java.util.Calendar.DAY_OF_YEAR, -daysToSubtract)
                    val currentWeekStart = cal.timeInMillis
                    
                    val isMonday = (dayOfWeek == java.util.Calendar.MONDAY)
                    val logsForAnalysis = if (isMonday) {
                        val prevWeekStart = currentWeekStart - 7 * 24 * 3600 * 1000L
                        logs.filter { it.timestamp in prevWeekStart until currentWeekStart }
                    } else {
                        logs.filter { it.timestamp >= currentWeekStart }
                    }
                    
                    val dayFormat = java.text.SimpleDateFormat("EEEE", if (lang == "el") java.util.Locale("el") else java.util.Locale.US)
                    val fullDateFormat = java.text.SimpleDateFormat("EEEE, MMMM d, yyyy", if (lang == "el") java.util.Locale("el") else java.util.Locale.US)
                    val timeFormat = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
                    val currentTimeStr = timeFormat.format(java.util.Date())
                    val currentDateStr = dayFormat.format(java.util.Date())
                    val currentDateFullStr = fullDateFormat.format(java.util.Date())
                    
                    val todayPct = if (targetGoal > 0) ((targetDateIntake.toDouble() / targetGoal.toDouble()) * 100).toInt() else 0
                    val remainingGoal = (targetGoal - targetDateIntake).coerceAtLeast(0)
                    val currentStreakVal = _streak.value
                    
                    val todayLogs = logs.filter { it.dateString == targetDateStr }
                    val todayLogCount = todayLogs.size
                    val todayBeverageBreakdown = todayLogs.groupBy { it.beverageType }
                        .map { (bev, logList) -> "${logList.sumOf { it.amountMl }}ml of $bev" }
                        .joinToString(", ")

                    // Build 7-day graph history breakdown
                    val past7DaysGraph = StringBuilder()
                    val graphCal = java.util.Calendar.getInstance().apply {
                        add(java.util.Calendar.DAY_OF_YEAR, -6)
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    val dateStrFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                    for (i in 0 until 7) {
                        val dStr = dateStrFormat.format(graphCal.time)
                        val dDayName = dayFormat.format(graphCal.time)
                        val dayGoal = getDailyGoalForDate(dStr)
                        val dayIntake = logs.filter { it.dateString == dStr }.sumOf { it.waterEquivalentMl }
                        val dayPct = if (dayGoal > 0) ((dayIntake.toDouble() / dayGoal.toDouble()) * 100).toInt() else 0
                        val status = if (dayIntake >= dayGoal && dayGoal > 0) "Goal Met" else "Goal Incomplete"
                        past7DaysGraph.append("- $dDayName ($dStr): ${dayIntake}ml logged / ${dayGoal}ml goal ($dayPct% reached) -> $status\n")
                        graphCal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                    }

                    val totalVolumeThisWeek = logsForAnalysis.sumOf { it.amountMl }
                    val intakeByDayMap = logsForAnalysis.groupBy { it.dateString }
                        .mapValues { entry -> entry.value.sumOf { log -> log.amountMl } }
                    val daysMetThisWeek = intakeByDayMap.count { (dStr, totalMl) -> totalMl >= getDailyGoalForDate(dStr) }
                    val averageIntakeThisWeek = if (intakeByDayMap.isNotEmpty()) totalVolumeThisWeek / intakeByDayMap.size else 0
                    
                    val userWeightVal = prefs.getFloat("user_weight", 0f)
                    val cityNameVal = _locationCity.value
                    val weatherTemp = _weatherTemperature.value
                    val weatherApparentTemp = _weatherApparentTemperature.value
                    val weatherHumidity = _weatherRelativeHumidity.value
                    val dangerLevel = heatDangerLevel.value
                    val isRainingNow = _isRaining.value

                    val summaryData = StringBuilder()
                    summaryData.append("=== USER PROFILE & CURRENT CONTEXT ===\n")
                    summaryData.append("Language: $lang\n")
                    summaryData.append("Current Local Date: $currentDateFullStr ($targetDateStr)\n")
                    summaryData.append("Current Local Time: $currentTimeStr\n")
                    if (cityNameVal.isNotBlank()) summaryData.append("City / Location: $cityNameVal\n")
                    if (userWeightVal > 0f) summaryData.append("User Weight: ${String.format(java.util.Locale.US, "%.1f", userWeightVal)} kg\n")
                    
                    summaryData.append("\n=== TODAY'S REAL-TIME HYDRATION STATS ===\n")
                    summaryData.append("Daily Goal Target: $targetGoal ml\n")
                    summaryData.append("Today's Total Intake: $targetDateIntake ml\n")
                    summaryData.append("Today's Goal Percentage Reached: $todayPct%\n")
                    summaryData.append("Remaining Water Needed Today: $remainingGoal ml\n")
                    summaryData.append("Current Active Goal Streak: $currentStreakVal days in a row\n")
                    summaryData.append("Today's Total Log Entries: $todayLogCount\n")
                    if (todayBeverageBreakdown.isNotBlank()) {
                        summaryData.append("Today's Beverage Intake Breakdown: $todayBeverageBreakdown\n")
                    }
                    summaryData.append("\n=== REAL-TIME OVERHYDRATION & CLINICAL SAFETY STATUS ===\n")
                    summaryData.append("Rapid Over-consumption (Last 60m Intake > 1000ml) Active: $isRapidOverhydrated\n")
                    if (isRapidOverhydrated) {
                        summaryData.append("- 60-Minute Intake Volume: ${recent60MinIntake} ml\n")
                        summaryData.append("- Excess Volume Above Renal Limit (~1000ml/hr): +${excessRapidMl} ml\n")
                        summaryData.append("- Estimated Clearance Duration Required: ~${rapidClearanceMin} minutes\n")
                    }
                    summaryData.append("Daily Goal Overhydration Active: $isDailyOverdone (Today Intake: ${targetDateIntake} ml vs Daily Goal: ${targetGoal} ml)\n")
                    
                    summaryData.append("\n=== WEEKLY GRAPH & 7-DAY HISTORY DATA ===\n")
                    summaryData.append("7-Day Graph Daily Breakdown:\n$past7DaysGraph")
                    summaryData.append("7-Day Total Hydration Volume: $totalVolumeThisWeek ml\n")
                    summaryData.append("7-Day Daily Average Intake: $averageIntakeThisWeek ml/day\n")
                    summaryData.append("Days Daily Goal Achieved: $daysMetThisWeek out of 7 days\n")
                    
                    summaryData.append("\n=== WEATHER & ENVIRONMENT FORECAST ===\n")
                    if (weatherTemp != null) summaryData.append("Current Temperature: ${String.format(java.util.Locale.US, "%.1f", weatherTemp)}°C\n")
                    if (weatherApparentTemp != null) summaryData.append("Feels-like Temperature: ${String.format(java.util.Locale.US, "%.1f", weatherApparentTemp)}°C\n")
                    if (weatherHumidity != null) summaryData.append("Relative Humidity: ${String.format(java.util.Locale.US, "%.0f", weatherHumidity)}%\n")
                    summaryData.append("Heat Danger Assessment: $dangerLevel\n")
                    summaryData.append("Rain Condition: ${if (isRainingNow) "YES (heavy/cozy rain in progress)" else "NO (dry/no rain)"}\n")

                    summaryData.append("\n=== DETAILED LOGS THIS WEEK ===\n")
                    logsForAnalysis.forEach {
                        val day = dayFormat.format(java.util.Date(it.timestamp))
                        val hr = timeFormat.format(java.util.Date(it.timestamp))
                        summaryData.append("- $day at $hr: ${it.amountMl}ml of ${it.beverageType}\n")
                    }
                    
                    val javaMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)
                    val monthName = java.text.SimpleDateFormat("MMMM", java.util.Locale.US).format(java.util.Date())
                    val season = when (javaMonth) {
                        11, 0, 1 -> "Winter/Cold"
                        2, 3, 4 -> "Spring"
                        5, 6, 7 -> "Summer/Hot"
                        8, 9, 10 -> "Autumn"
                        else -> "Unknown"
                    }
                    
                    val workoutBonus = _todayWorkoutWaterBonus.value
                    val workoutDetails = _todayWorkoutAiCoachResponse.value
                    val workoutStatusText = if (workoutBonus > 0) {
                        "The user completed a workout today (Bonus: ${workoutBonus}ml). Details: \"$workoutDetails\". (Note how intrusive/intensive this activity was on sweat levels: >500ml is highly taxing/intensive!)."
                    } else {
                        "No workout recorded today."
                    }

                    val calObj = java.util.Calendar.getInstance(getUserTimeZone())
                    val hourOfDay = calObj.get(java.util.Calendar.HOUR_OF_DAY)
                    val minuteOfHour = calObj.get(java.util.Calendar.MINUTE)
                    val currentDecHour = hourOfDay + (minuteOfHour / 60.0)
                    val isNightOrEarlyMorningForAi = hourOfDay >= 22 || hourOfDay < 7
                    
                    val isMorning = currentDecHour >= 5.0 && currentDecHour < 12.0
                    val isNoonAfternoon = currentDecHour >= 12.0 && currentDecHour < 16.0
                    
                    val sleepHoursVal = _todaySleepHours.value
                    
                    val sleepStatusText = if (sleepHoursVal == null) {
                        "No sleep data available for last night. Do NOT mention or comment on sleep or sleep quality at all in your insight."
                    } else if (!isMorning && !isNoonAfternoon) {
                        "Even though sleep data is available (${sleepHoursVal} hours), the current time is neither Morning nor Noon/Afternoon. Do NOT mention sleep or sleep quality at all in your insight."
                    } else {
                        // It is morning or noon/afternoon, and we have sleep data.
                        val isGoodSleep = sleepHoursVal >= 6.5
                        if (isGoodSleep) {
                            if (isMorning) {
                                "The user slept exceptionally well! They slept for exactly ${String.format(java.util.Locale.US, "%.1f", sleepHoursVal)} hours. Praise them warmly for this excellent good sleep but ONLY because it is still morning."
                            } else {
                                "The user slept well (${String.format(java.util.Locale.US, "%.1f", sleepHoursVal)} hours). Praise their sleep quality warmly, but do NOT mention the specific number of hours slept since it is afternoon now."
                            }
                        } else {
                            val sleepBonusMsg = _todaySleepWaterBonus.value
                            val sleepDetailsMsg = _todaySleepAiCoachResponse.value
                            if (isMorning) {
                                "The user slept poorly (${String.format(java.util.Locale.US, "%.1f", sleepHoursVal)} hours). An additional bonus of ${sleepBonusMsg}ml was added. Detail: \"$sleepDetailsMsg\". Empathize supportively, mentioning the hours slept."
                            } else {
                                "The user slept poorly under 6.5 hours (Detail: \"$sleepDetailsMsg\"). Offer gentle support, but do NOT mention the specific number of hours slept."
                            }
                        }
                    }

                    val latestLog = logs.maxByOrNull { it.timestamp }
                    val currentMilli = System.currentTimeMillis()
                    val hasNoRecentEntries = latestLog == null || (currentMilli - latestLog.timestamp) > 18 * 3600 * 1000L
                    
                    val inactivityHypePrompt = if (hasNoRecentEntries) {
                        "The user has not logged any water intake for a while. Do NOT mention or point out that they have been inactive or haven't logged recently (keep it positive). Instead, deliver a high-energy, hyping message to inspire them to drink water and get back on track right now!"
                    } else {
                        "The user is active and has recently logged their hydration."
                    }

                    val mbtiTypeFull = _userMbti.value.uppercase().trim()
                    val baseMbti = mbtiTypeFull.take(4)
                    val mbtiInstruction = if (baseMbti.isNotEmpty()) {
                        val mbtiTone = when {
                            baseMbti in listOf("INTJ", "INTP", "ENTJ", "ENTP") -> {
                                "Style: Clinical, intellectual, analytical, and highly structured with precise metrics. Avoid any generic emotional cheerleading. Focus strictly on biological optimization, efficiency ratios, chemical/physiological processes, and precise scientific reasoning suited for a rational $baseMbti.\n" +
                                "Length & Presentation: Concise, direct, and compact (1-2 sentences). Use quantitative academic language without friendly emojis or conversational fluff."
                            }
                            baseMbti in listOf("INFJ", "INFP", "ENFJ", "ENFP") -> {
                                "Style: Deeply personal, highly warm, exceptionally empathetic, holistic, and encouraging. Focus on self-love, mindfulness, emotional well-being, active somatic listening, and aligning hydration with mental state suited for a diplomatic $baseMbti.\n" +
                                "Length & Presentation: Descriptive, flowing, and comprehensive (3-5 sentences, letting the AI express deep and rich insights without feeling artificial or truncated). Use rich narrative metaphors (like a flowing stream, organic growth) and a few warm, supportive emojis (e.g., 🌸, ✨, 💧, 🧠) to give them a deep, comforting sense of reward."
                            }
                            baseMbti in listOf("ISTJ", "ISFJ", "ESTJ", "ESFJ") -> {
                                "Style: Structured, practical, standard habit-oriented, highly organized, and duty-focused. Focus strictly on steady schedule compliance, milestone completion percentages, exact target numbers, and building solid daily habits suited for a sentinel $baseMbti.\n" +
                                "Length & Presentation: Direct, clear, and structured (2-3 sentences). Discuss concrete progress ratios, habit discipline, and organized schedule compliance."
                            }
                            baseMbti in listOf("ISTP", "ISFP", "ESTP", "ESFP") -> {
                                "Style: Full of high energy, active, highly snappy, playful, and fun. Use punchy phrasing, lighthearted humor, active verbs, and immediate casual prompts suited for an explorer $baseMbti.\n" +
                                "Length & Presentation: Snappy and actionable (1-2 short, high-impact sentences). Keep it highly actionable with a very active tone and energetic emojis (e.g., ⚡, 🚀, 🏃)."
                            }
                            else -> "Style: Balanced and helpful.\nLength & Presentation: Standard 1-2 sentences."
                        }
                        
                        var extraTraitsInstruction = ""
                        if (mbtiTypeFull.length >= 7 && mbtiTypeFull[4] == '-') {
                            val axis5 = mbtiTypeFull[5]
                            val axis6 = mbtiTypeFull.getOrNull(6) ?: ' '
                            val trait5 = if (axis5 == 'A') "Assertive (adapts firmly without oscillating)" else if (axis5 == 'O') "Oscillating (fluid, changes mind dynamically)" else ""
                            val trait6 = if (axis6 == 'H') "Harmony (prioritizes deep emotional connection over detachment)" else if (axis6 == 'C') "Calm (maintains a highly detached and calm emotional core)" else ""
                            extraTraitsInstruction = "Extended Personality Traits:\n- Axis 5 (Adaptation): $trait5\n- Axis 6 (Emotional Core): $trait6\n" +
                            "Make sure your writing style adapts to match these extra traits seamlessly without ever mentioning the traits, axes, or their labels in the text.\n"
                        }

                        val extraEmojiRule = if (_aiCoachDisableEmojis.value) "\nCRITICAL: DO NOT use ANY emojis or emoticons in your response. The user has explicitly disabled emojis. Absolutely no emojis allowed.\n" else ""

                        "\nMBTI ADAPTATION & CORE CONSTRAINTS (User Type: $mbtiTypeFull):\n$mbtiTone\n$extraTraitsInstruction" +
                        "Crucial: Override any default instruction stating \"max 2 sentences\" or \"1-2 sentences\". Adhere strictly to the requested Length, Tone, and Formatting specified above for the user's type.$extraEmojiRule"
                    } else {
                        if (_aiCoachDisableEmojis.value) "\nCRITICAL: DO NOT use ANY emojis or emoticons in your response. The user has explicitly disabled emojis.\n" else ""
                    }

                    val bigFiveInstruction = if (_userBigFiveO.value != -1 && 
                                                 _userBigFiveC.value != -1 && 
                                                 _userBigFiveE.value != -1 && 
                                                 _userBigFiveA.value != -1 && 
                                                 _userBigFiveN.value != -1) {
                        val oVal = _userBigFiveO.value
                        val cVal = _userBigFiveC.value
                        val eVal = _userBigFiveE.value
                        val aVal = _userBigFiveA.value
                        val nVal = _userBigFiveN.value

                        fun getLevel(v: Int): String = when {
                            v < 35 -> "Low"
                            v <= 65 -> "Moderate"
                            else -> "High"
                        }

                        val oLvl = getLevel(oVal)
                        val cLvl = getLevel(cVal)
                        val eLvl = getLevel(eVal)
                        val aLvl = getLevel(aVal)
                        val nLvl = getLevel(nVal)

                        val traitsPrompt = """
                            BIG FIVE (OCEAN) trait adjustments of the user:
                            - Openness: $oLvl ($oVal%). ${if (oLvl == "High") "User enjoys variety, deep biological/physiological details, and intellectual/creative novelty. Introduce intellectually stimulating facts or unique metaphors where appropriate." else "User prefers simple, practical, tried-and-true tracking cues."}
                            - Conscientiousness: $cLvl ($cVal%). ${if (cLvl == "High") "User loves exact metrics, goal streak details, compliance percentages, and highly structured advice." else "User appreciates flexible, easygoing, non-rigid suggestions."}
                            - Extraversion: $eLvl ($eVal%). ${if (eLvl == "High") "User thrives on interactive, active, expressive, and socially oriented framing." else "User values calm, peaceful, introspective, and serene wording."}
                            - Agreeableness: $aLvl ($aVal%). ${if (aLvl == "High") "User is motivated by collaborative wellness, high empathetic support, warmth, and friendly care." else "User appreciates cool objective facts, performance-oriented targets, and raw directness."}
                            - Neuroticism: $nLvl ($nVal%). ${if (nLvl == "High") "User is sensitive to pressure or guilt. Be incredibly soothing, reassurance-focused, pressure-free, and positive. Never use alarming or warning language." else "User is steady, resilient, and direct. Use objective matter-of-fact styling."}
                        """.trimIndent()
                        "\nBIG FIVE ADAPTATION (Works in synergy with MBTI):\n$traitsPrompt\nCombine these Big Five modifiers seamlessly with the MBTI style rules above. Let the combination create a beautifully tailored coaching advice."
                    } else {
                        ""
                    }

                    val targetLangName = if (lang == "el") "Greek" else "English"

                    val systemPrompt = """
                        You are a motivating, expert hydration AI coach in the Pixel Water app.
                        Analyze the user's weekly hydration logs. Give a highly personalized encouraging insight.
                        Notice beverage patterns (e.g. coffee/tea limits hydration), timing patterns, or goal consistency.
                        ${if (isMonday) "CRITICAL: Today is Monday. Since the current week has just started, do NOT say or imply that the user met their goal '0 out of 7 days' or has very low stats for this week. Instead, focus your analysis on the PREVIOUS COMPLETED WEEK logs provided, analyze their week-long patterns, and offer warm encouragement and positive momentum for the brand new week ahead." else ""}
                        
                        CRUCIAL CONTEXT:
                        - Current local time: $currentTimeStr on $currentDateStr
                        - Weekday: $currentDateStr
                        - Current Month: $monthName (General Season: $season)
                        - Today's workout status: $workoutStatusText
                        - Last night's sleep status: $sleepStatusText
                        - Recent Inactivity Status: $inactivityHypePrompt
                        - Current Weather: Temperature ${_weatherTemperature.value ?: "unknown"}°C, Raining: ${if (_isRaining.value) "YES (heavy/cozy rain now!)" else "NO"}
                        $mbtiInstruction
                        $bigFiveInstruction
                        
                        Adapt your advice dynamically: 
                        - Time of day: Morning (jump-start hydration), Day (maintain pace), Evening (slow down to prevent night-time bathroom visits).
                        - Weather/Season: If it's Summer/Hot, remind them that sweating increases water needs. If Winter/Cold, mention people often forget to drink because they aren't visibly sweating.
                        - Log Insights: If they drink lots of coffee/tea, remind them to compensate with pure water. If they haven't drank much today yet, give a gentle nudge.
                        - Sleep & Exercise context: Follow the instructions in "Last night's sleep status" precisely. Only include or praise/support sleep quality if requested and allowed.
                        - Health & Overhydration Balance: The user has currently consumed $targetDateIntake ml today against a daily goal of $targetGoal ml, and in the last 60 minutes consumed $recent60MinIntake ml (Is rapid overhydration: $isRapidOverhydrated, Is daily overhydration: $isDailyOverdone, Any Overhydration: $hasOverdone). If $hasOverdone is true (or if isRapidOverhydrated or isDailyOverdone is true), you absolutely MUST analyze and address this overhydration state in your coaching insight with supreme scientific depth and clinical objectivity. In your analysis:
                          1. Explain the physiological mechanics of fluid over-consumption: rapid intake exceeding renal excretion capacity (~800-1000 ml/hr) or excessive daily volume causes acute blood volume expansion (hypervolemia), transient arterial blood pressure elevation (hypervolemic hypertension), and plasma dilution (lowering blood sodium / hyponatremia), creating an osmotic gradient that pulls water into cells (osmotic swelling).
                          2. Address their specific situation directly: ${if (isRapidOverhydrated) "The user drank ${recent60MinIntake} ml in 60 min (+${excessRapidMl} ml excess). Recommend pausing fluid intake for ~${rapidClearanceMin} minutes until renal clearance restores homeostatic fluid balance." else ""} ${if (isDailyOverdone) "Their total daily intake of ${targetDateIntake} ml has substantially exceeded their daily goal of ${targetGoal} ml. Recommend pacing intake slower." else ""}
                          3. Direct the user to look out for early symptoms (such as mild/throbbing headache, dizziness, elevated blood pressure readings, nausea, abdominal bloating, muscle cramps, or confusion/lethargy).
                          4. Explicitly advise them to contact their primary care physician, a qualified medical doctor, or visit a hospital immediately if they are concerned, feel unwell, or develop serious symptoms.
                          5. Frame this advice in an analytical, objective, and medically responsible manner that emphasizes self-care, personal water regulation limits, and the fact that this application is solely an educational tracker, not a medical device or diagnostic system. Do not sound panicked, but maintain high scientific accuracy. If $hasOverdone is false, do not mention overhydration.
                        
                        DO NOT start with any greetings like "Good morning", "Hello", etc., as our app handles greetings locally. Start directly with the hydration analysis.
                        DO NOT repeat the prompt. Provide the text strictly in $targetLangName language. Speak in a friendly, supportive tone.
                        
                        CRITICAL: DO NOT include any structural labels, metadata headers, instruction summaries, or system placeholders in your response (such as "Length & Presentation:", "Style:", "Emojis:", "MBTI Adaptation:", "Big Five Adaptation:" etc.). Do not produce bullet points outlining your output format or instructions. Start directly with the first complete sentence, and write ONLY clean, flowing sentences of coaching text.
                        CRITICAL CONSTRAINT: You must NEVER mention any of the following terms, acronyms, jargon-phrases, or concepts in your output: "MBTI", "Big Five", "OCEAN", "Openness", "Conscientiousness", "Extraversion", "Agreeableness", "Neuroticism", "Myers-Briggs", "Assertive", "Turbulent", "Oscillating", "Harmony", "Calm", "Introversion", "Introverted", "Intuitive", or any of the personality codes (like "INFJ", "INTJ", "INFP", "ENFJ", etc.). Do NOT list, name, or explain the personality traits or variables themselves. Do NOT print sentences like "Sentence 1:", "Sentence 2:", "with ellipses? No", "personality traits integrated?", or explicitly check off your rules in any format. Simply adopt the requested writing style and tone internally and output ONLY the final, polished, cohesive coaching text directly.
                        CRITICAL: Never start or end your response with ellipses "..." or leave any sentence incomplete or fragmented. Every sentence must start fully-formed with a capitalized letter and terminate cleanly with proper punctuation (like a period, exclamation mark, or emoji).
                        
                        ${if (isNightOrEarlyMorningForAi) {
                            "CRITICAL RULE FOR LENGTH (NIGHT MODE): Since it is late night or early morning, keep your advice brief, soothing, cozy, and relaxing (strictly 1 to 2 short sentences). The user should be preparing for sleep or resting, so do NOT overwhelm them with deep tables of metrics or long paragraphs."
                        } else {
                            "CRITICAL RULE FOR LENGTH (DAY MODE): You are strictly encouraged to offer a robust, highly detailed, and comprehensive deep coaching analysis (typically 3 to 5 rich, informative sentences). The layout is extremely spacious and fully scrollable, so do NOT write a brief 1-2 sentence message. Provide deep metabolic, habit-building, relative progress ratios, or environmental insights. Make it feel premium, thoughtful, and highly informative."
                        }}
                        You must always output complete, fully-formed, and grammatically correct sentences. Never cut off mid-sentence, mid-phrase, or end with incomplete words. Every single sentence you output must be fully finished with proper terminal punctuation (e.g., period, exclamation point, or emoji). No trailing half-sentences are permitted.
                        
                        GOOGLE SEARCH DRAWING RULE: You may use search grounding ONLY to lookup current local weather, high temperatures, humidity levels, or heat alerts for local weather and heat purposes. Do NOT perform web searches for any other topics.
                    """.trimIndent()
                    val prompt = "Data:\n$summaryData\nProvide ${
                        if (isNightOrEarlyMorningForAi) {
                            "a very brief, soothing, and encouraging 1-2 sentence nightly segment in $targetLangName language"
                        } else if (mbtiTypeFull.isNotEmpty() || _userBigFiveO.value != -1) {
                            "the personality-tailored, detailed, and length-compliant coaching advice according to the MBTI & Big Five rules in $targetLangName language"
                        } else {
                            "an encouraging, deep, highly detailed, and multi-sentence (3-5 sentences) layout-friendly coaching insight in $targetLangName language"
                        }
                    } about this completed week's progress ${if (isMonday) "(referring to the previous week's patterns and encouraging them for the new week)" else ""} based on these logs, today's workout, last night's sleep, current weather/heat conditions, the current time ($currentTimeStr), the weekday ($currentDateStr), the month ($monthName), and the season ($season). Ensure the entire generated text is strictly in $targetLangName language, has completely closed sentences, is never truncated, and doesn't cut off."
                    
                    val contents = listOf(
                        com.pixelwater.app.data.Content(
                            role = "user",
                            parts = listOf(com.pixelwater.app.data.Part(text = prompt))
                        )
                    )
                    val request = com.pixelwater.app.data.GeminiRequest(
                        contents = contents,
                        systemInstruction = com.pixelwater.app.data.Content(parts = listOf(com.pixelwater.app.data.Part(text = systemPrompt))),
                        generationConfig = com.pixelwater.app.data.GenerationConfig(temperature = 0.7f, maxOutputTokens = 1500),
                        tools = null
                    )
                    
                    try {
                        val response = safeGenerateContent(
                            model = _insightGeminiModel.value,
                            apiKey = apiKey,
                            request = request,
                            onModelChosen = { _lastUsedInsightModel.value = it },
                            isInsight = true
                        )
                        val replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        if (!replyText.isNullOrBlank()) {
                            val cleanReply = sanitizeIncompleteSentences(replyText.replace("\"", "").trim())
                            _aiWeeklyProgressSummaryRaw.value = cleanReply
                            prefs.edit()
                                .putString("ai_weekly_progress_summary_raw", cleanReply)
                                .putString("ai_weekly_progress_summary", cleanReply)
                                .apply()
                            return@launch
                        }
                    } catch (e: Exception) {
                        Log.d("WaterViewModel", "Gemini AI Summary Insight unavailable (${e.message}), using rule-based coach.")
                    }
                }

                _aiWeeklyProgressSummaryRaw.value = baseText
                prefs.edit()
                    .putString("ai_weekly_progress_summary_raw", baseText)
                    .putString("ai_weekly_progress_summary", baseText)
                    .apply()
            } catch (e: Exception) {
                Log.e("WaterViewModel", "Error generating weekly progress summary: ${e.message}")
            } finally {
                _isGeneratingWeeklySummary.value = false
            }
        }
    }

    fun sanitizeIncompleteSentences(text: String): String {
        var trimmed = text.trim()
        if (trimmed.isEmpty()) return trimmed
        
        // Already ends with terminal punctuation
        val lastChar = trimmed.last()
        val terminators = setOf('.', '!', '?', ';', '»', '”', '\'', '"', '…', '。', '！', '？', '）', ')')
        if (terminators.contains(lastChar)) {
            return trimmed
        }
        
        // Check if it ends with an emoji or other non-word, non-standard symbol (e.g. ✨)
        if (!lastChar.isLetterOrDigit() && lastChar != ',' && lastChar != ';' && lastChar != ':' && lastChar != '-' && lastChar != '(' && lastChar != '[' && lastChar != '{' && lastChar != '&') {
            return trimmed
        }
        
        val connectives = setOf(
            "as", "and", "but", "the", "for", "with", "particularly", "because", "so", "or", "to", "at", "by", "from", "of", "on", "in", "like", "although", "though", "while", "since", "until", "unless", "about", "against", "among", "before", "during", "under", "without", "than", "if", "that", "which", "who", "whom", "especially", "specially", "a", "an", "this", "these", "those", "such", "my", "your", "his", "her", "its", "our", "their", "any", "some", "every", "each", "either", "neither", "no", "one", "another", "other", "nor", "yet", "whereas", "whether", "how", "however", "when", "whenever", "where", "wherever", "why", "whoever", "whichever", "whatever"
        )
        val greekConnectives = setOf(
            "και", "αλλά", "όπως", "καθώς", "διότι", "επειδή", "αν", "για", "με", "σε", "από", "παρά", "ενώ", "ώστε", "ότι", "πως", "καθότι", "σχετικά", "παρόλο", "ένα", "μια", "ένας", "το", "τη", "την", "της", "του", "των", "τους", "τις", "στα", "στη", "στην", "στο", "στον", "στους", "στις", "εκείνο", "εκείνη", "εκείνα", "αυτό", "αυτή", "αυτά", "κάποιο", "κάποια", "κάποιοι", "κάποιες", "κάθε", "χωρίς", "πριν", "μετά", "κατά", "αντί", "υπέρ", "λόγω", "εξαιτίας", "να", "θα"
        )
        
        // Do up to 4 iterations of trimming trailing incomplete connectives
        for (i in 0 until 4) {
            val lastSpace = trimmed.lastIndexOf(' ')
            if (lastSpace == -1) break
            
            val lastWord = trimmed.substring(lastSpace + 1).replace(Regex("[^a-zA-Zα-ωά-ώΑ-Ωίϊΐόύϋΰήέώ]"), "").lowercase(java.util.Locale.getDefault())
            if (connectives.contains(lastWord) || greekConnectives.contains(lastWord)) {
                trimmed = trimmed.substring(0, lastSpace).trim()
                // Trim trailing punctuation like comma or semicolon
                while (trimmed.isNotEmpty() && (trimmed.last() == ',' || trimmed.last() == ';' || trimmed.last() == ':' || trimmed.last() == '-' || trimmed.last() == ' ')) {
                    trimmed = trimmed.substring(0, trimmed.length - 1).trim()
                }
                if (trimmed.isEmpty()) break
            } else {
                break
            }
        }
        
        if (trimmed.isEmpty()) return ""
        
        // Re-check trailing character
        val finalLastChar = trimmed.last()
        if (terminators.contains(finalLastChar) || (!finalLastChar.isLetterOrDigit() && finalLastChar != ',' && finalLastChar != ';' && finalLastChar != ':' && finalLastChar != '-')) {
            return trimmed
        }
        
        return trimmed + "."
    }

    private fun isNewerThan(time1: String, time2: String): Boolean {
        if (time1.isEmpty()) return false
        if (time2.isEmpty()) return true
        return try {
            val i1 = java.time.Instant.parse(time1)
            val i2 = java.time.Instant.parse(time2)
            i1.isAfter(i2)
        } catch (e: Exception) {
            time1.compareTo(time2) > 0
        }
    }

    fun checkForUpdates(silent: Boolean = false) {
        if (!silent) {
            _updateStatus.value = UpdateStatus.Checking
        }
        viewModelScope.launch {
            try {
                // Tracking update check timestamp
                val currentTime = System.currentTimeMillis()
                prefs.edit().putLong("last_update_check_time", currentTime).apply()

                val client = okhttp3.OkHttpClient()
                val request = okhttp3.Request.Builder()
                    .url("https://api.github.com/repos/ChadRat/PixelWater-/releases")
                    .header("User-Agent", "PixelWaterUpdateChecker")
                    .build()
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            if (!silent) {
                                _updateStatus.value = UpdateStatus.Error("Failed to fetch release info: ${response.code}")
                            }
                            return@use
                        }
                        val bodyString = response.body?.string()
                        if (bodyString.isNullOrBlank()) {
                            if (!silent) {
                                _updateStatus.value = UpdateStatus.Error("Empty response from updates API")
                            }
                            return@use
                        }
                        
                        val jsonArray = org.json.JSONArray(bodyString)
                        if (jsonArray.length() == 0) {
                            if (!silent) {
                                _updateStatus.value = UpdateStatus.UpToDate
                            }
                            return@use
                        }
                        
                        val releasesList = mutableListOf<org.json.JSONObject>()
                        for (i in 0 until jsonArray.length()) {
                            releasesList.add(jsonArray.getJSONObject(i))
                        }
                        
                        // Sort by published_at descending (newest first)
                        releasesList.sortByDescending { obj ->
                            val pubAt = obj.optString("published_at", "")
                            try {
                                java.time.Instant.parse(pubAt).toEpochMilli()
                            } catch (e: Exception) {
                                0L
                            }
                        }
                        
                        val releaseObj = releasesList[0]
                        val tagName = releaseObj.optString("tag_name", "")
                        val releaseName = releaseObj.optString("name", tagName)
                        val releaseBody = releaseObj.optString("body", "")
                        val publishedAt = releaseObj.optString("published_at", "")
                        
                        val assets = releaseObj.optJSONArray("assets")
                        var apkUrl = ""
                        var apkSize = 0L
                        var zipUrl = ""
                        var zipSize = 0L
                        
                        if (assets != null) {
                            for (k in 0 until assets.length()) {
                                val asset = assets.getJSONObject(k)
                                val assetName = asset.optString("name", "").lowercase()
                                if (assetName.endsWith(".apk") && !(assetName.contains("wear") || assetName.contains("pixelwaterwear") || assetName.contains("watch"))) {
                                    apkUrl = asset.optString("browser_download_url", "")
                                    apkSize = asset.optLong("size", 0L)
                                } else if (assetName.endsWith(".zip")) {
                                    zipUrl = asset.optString("browser_download_url", "")
                                    zipSize = asset.optLong("size", 0L)
                                }
                            }
                        }
                        
                        if (zipUrl.isEmpty()) {
                            zipUrl = releaseObj.optString("zipball_url", "")
                            if (zipUrl.isEmpty() && tagName.isNotEmpty()) {
                                zipUrl = "https://github.com/ChadRat/PixelWater-/archive/refs/tags/$tagName.zip"
                            }
                        }
                        
                        val hasApk = apkUrl.isNotEmpty()
                        val finalDownloadUrl = if (hasApk) apkUrl else zipUrl
                        val finalSize = if (hasApk) apkSize else zipSize
                        
                        val lastInstalledReleaseTime = prefs.getString("last_installed_release_time", "") ?: ""
                        val isNew = isNewerThan(publishedAt, lastInstalledReleaseTime)
                        
                        if (isNew) {
                            val lastNotifiedReleaseTime = prefs.getString("last_notified_release_time", "") ?: ""
                            
                            if (!silent || lastNotifiedReleaseTime != publishedAt) {
                                if (silent) {
                                    prefs.edit().putString("last_notified_release_time", publishedAt).apply()
                                }
                                triggerFireworksHaptic()
                                _updateStatus.value = UpdateStatus.UpdateAvailable(
                                    tagName = tagName,
                                    name = releaseName,
                                    body = releaseBody,
                                    downloadUrl = finalDownloadUrl,
                                    sizeBytes = finalSize,
                                    publishedAt = publishedAt,
                                    hasApk = hasApk,
                                    zipBackupUrl = zipUrl
                                )
                            }
                        } else {
                            if (!silent) {
                                _updateStatus.value = UpdateStatus.UpToDate
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (!silent) {
                    _updateStatus.value = UpdateStatus.Error("Network error: ${e.localizedMessage ?: "Unknown error"}")
                }
            }
        }
    }

    fun checkForWearOsUpdates() {
        _wearUpdateStatus.value = WearUpdateStatus.Checking
        viewModelScope.launch {
            try {
                val client = okhttp3.OkHttpClient()
                val request = okhttp3.Request.Builder()
                    .url("https://api.github.com/repos/ChadRat/PixelWater-/releases")
                    .header("User-Agent", "PixelWaterWearUpdateChecker")
                    .build()
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            _wearUpdateStatus.value = WearUpdateStatus.Error("Failed to fetch release info: ${response.code}")
                            return@use
                        }
                        val bodyString = response.body?.string()
                        if (bodyString.isNullOrBlank()) {
                            _wearUpdateStatus.value = WearUpdateStatus.Error("Empty response from updates API")
                            return@use
                        }
                        
                        val jsonArray = org.json.JSONArray(bodyString)
                        if (jsonArray.length() == 0) {
                            _wearUpdateStatus.value = WearUpdateStatus.UpToDate
                            return@use
                        }
                        
                        val releasesList = mutableListOf<org.json.JSONObject>()
                        for (i in 0 until jsonArray.length()) {
                            releasesList.add(jsonArray.getJSONObject(i))
                        }
                        
                        // Sort by published_at descending (newest first)
                        releasesList.sortByDescending { obj ->
                            val pubAt = obj.optString("published_at", "")
                            try {
                                java.time.Instant.parse(pubAt).toEpochMilli()
                            } catch (e: Exception) {
                                0L
                            }
                        }
                        
                        val releaseObj = releasesList[0]
                        val tagName = releaseObj.optString("tag_name", "")
                        val releaseName = releaseObj.optString("name", tagName)
                        val releaseBody = releaseObj.optString("body", "")
                        val publishedAt = releaseObj.optString("published_at", "")
                        
                        val assets = releaseObj.optJSONArray("assets")
                        var wearApkUrl = ""
                        var wearApkSize = 0L
                        var zipUrl = ""
                        var zipSize = 0L
                        
                        if (assets != null) {
                            for (k in 0 until assets.length()) {
                                val asset = assets.getJSONObject(k)
                                val assetName = asset.optString("name", "").lowercase()
                                if (assetName.endsWith(".apk") && (assetName.contains("wear") || assetName.contains("pixelwaterwear") || assetName.contains("watch"))) {
                                    wearApkUrl = asset.optString("browser_download_url", "")
                                    wearApkSize = asset.optLong("size", 0L)
                                } else if (assetName.endsWith(".zip")) {
                                    zipUrl = asset.optString("browser_download_url", "")
                                    zipSize = asset.optLong("size", 0L)
                                }
                            }
                        }
                        
                        if (zipUrl.isEmpty()) {
                            zipUrl = releaseObj.optString("zipball_url", "")
                            if (zipUrl.isEmpty() && tagName.isNotEmpty()) {
                                zipUrl = "https://github.com/ChadRat/PixelWater-/archive/refs/tags/$tagName.zip"
                            }
                        }
                        
                        val hasWearApk = wearApkUrl.isNotEmpty()
                        val finalDownloadUrl = if (hasWearApk) wearApkUrl else zipUrl
                        val finalSize = if (hasWearApk) wearApkSize else zipSize
                        
                        triggerFireworksHaptic()
                        _wearUpdateStatus.value = WearUpdateStatus.UpdateAvailable(
                            tagName = tagName,
                            name = releaseName,
                            body = releaseBody,
                            downloadUrl = finalDownloadUrl,
                            sizeBytes = finalSize,
                            publishedAt = publishedAt,
                            hasWearApk = hasWearApk,
                            zipBackupUrl = zipUrl
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _wearUpdateStatus.value = WearUpdateStatus.Error("Network error: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun downloadWearUpdate(context: android.content.Context, downloadUrl: String, fileName: String, publishedAt: String) {
        _wearUpdateStatus.value = WearUpdateStatus.Downloading(0f, 0L, 0L)
        viewModelScope.launch {
            try {
                val client = okhttp3.OkHttpClient()
                val request = okhttp3.Request.Builder()
                    .url(downloadUrl)
                    .build()
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            _wearUpdateStatus.value = WearUpdateStatus.Error("Download failed with response: ${response.code}")
                            return@use
                        }
                        
                        val responseBody = response.body
                        if (responseBody == null) {
                            _wearUpdateStatus.value = WearUpdateStatus.Error("Download response has no body")
                            return@use
                        }
                        
                        val totalBytes = responseBody.contentLength()
                        val updateDir = java.io.File(context.cacheDir, "wear_updates")
                        if (!updateDir.exists()) {
                            updateDir.mkdirs()
                        }
                        updateDir.listFiles()?.forEach { file ->
                            try { file.delete() } catch (e: Exception) {}
                        }
                        val destinationFile = java.io.File(updateDir, fileName)
                        
                        responseBody.byteStream().use { input ->
                            java.io.FileOutputStream(destinationFile).use { output ->
                                val buffer = ByteArray(8192)
                                var bytesRead: Int
                                var downloadedBytes = 0L
                                var lastUpdateMillis = 0L
                                
                                while (input.read(buffer).also { bytesRead = it } != -1) {
                                    output.write(buffer, 0, bytesRead)
                                    downloadedBytes += bytesRead
                                    
                                    val now = System.currentTimeMillis()
                                    if (now - lastUpdateMillis > 150L || downloadedBytes == totalBytes) {
                                        lastUpdateMillis = now
                                        if (totalBytes > 0) {
                                            val progress = downloadedBytes.toFloat() / totalBytes.toFloat()
                                            _wearUpdateStatus.value = WearUpdateStatus.Downloading(progress, downloadedBytes, totalBytes)
                                        } else {
                                            _wearUpdateStatus.value = WearUpdateStatus.Downloading(-1f, downloadedBytes, -1L)
                                        }
                                    }
                                }
                            }
                        }
                        
                        _wearUpdateStatus.value = WearUpdateStatus.Downloaded(destinationFile.absolutePath, publishedAt)
                        triggerButtonHaptic()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _wearUpdateStatus.value = WearUpdateStatus.Error("Download error: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun resetWearUpdateStatus() {
        _wearUpdateStatus.value = WearUpdateStatus.Idle
    }

    fun exportPrebundledWearApk(context: android.content.Context) {
        _wearUpdateStatus.value = WearUpdateStatus.Downloading(0f, 0L, 0L)
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val assetManager = context.assets
                val updateDir = java.io.File(context.cacheDir, "wear_updates")
                if (!updateDir.exists()) {
                    updateDir.mkdirs()
                }
                updateDir.listFiles()?.forEach { file ->
                    try { file.delete() } catch (e: Exception) {}
                }
                val destinationFile = java.io.File(updateDir, "PixelWater_WearOS_App.apk")
                
                assetManager.open("PixelWater_WearOS_App.apk").use { input ->
                    java.io.FileOutputStream(destinationFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalCopied = 0L
                        
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalCopied += bytesRead
                        }
                    }
                }
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    _wearUpdateStatus.value = WearUpdateStatus.Downloaded(destinationFile.absolutePath, "bundled")
                    triggerButtonHaptic()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    _wearUpdateStatus.value = WearUpdateStatus.Error("Failed to extract pre-bundled APK: ${e.localizedMessage ?: "File not found"}")
                }
            }
        }
    }

    fun downloadUpdateAndInstall(context: android.content.Context, downloadUrl: String, fileName: String, publishedAt: String) {
        _updateStatus.value = UpdateStatus.Downloading(0f, 0L, 0L)
        viewModelScope.launch {
            try {
                val client = okhttp3.OkHttpClient()
                val request = okhttp3.Request.Builder()
                    .url(downloadUrl)
                    .build()
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            _updateStatus.value = UpdateStatus.Error("Download failed with response: ${response.code}")
                            return@use
                        }
                        
                        val responseBody = response.body
                        if (responseBody == null) {
                            _updateStatus.value = UpdateStatus.Error("Download response has no body")
                            return@use
                        }
                        
                        val totalBytes = responseBody.contentLength()
                        val updateDir = java.io.File(context.cacheDir, "updates")
                        if (!updateDir.exists()) {
                            updateDir.mkdirs()
                        }
                        // Purge any old downloaded updates to keep cache clean
                        updateDir.listFiles()?.forEach { file ->
                            try { file.delete() } catch (e: Exception) {}
                        }
                        val destinationFile = java.io.File(updateDir, fileName)
                        
                        responseBody.byteStream().use { input ->
                            java.io.FileOutputStream(destinationFile).use { output ->
                                val buffer = ByteArray(8192)
                                var bytesRead: Int
                                var downloadedBytes = 0L
                                var lastUpdateMillis = 0L
                                
                                while (input.read(buffer).also { bytesRead = it } != -1) {
                                    output.write(buffer, 0, bytesRead)
                                    downloadedBytes += bytesRead
                                    
                                    val now = System.currentTimeMillis()
                                    // Throttle UI flow updates to at most once per 150ms to prevent recomposition storms
                                    if (now - lastUpdateMillis > 150L || downloadedBytes == totalBytes) {
                                        lastUpdateMillis = now
                                        if (totalBytes > 0) {
                                            val progress = downloadedBytes.toFloat() / totalBytes.toFloat()
                                            _updateStatus.value = UpdateStatus.Downloading(progress, downloadedBytes, totalBytes)
                                        } else {
                                            _updateStatus.value = UpdateStatus.Downloading(-1f, downloadedBytes, -1L)
                                        }
                                    }
                                }
                            }
                        }
                        
                        _updateStatus.value = UpdateStatus.Downloaded(destinationFile.absolutePath, publishedAt)
                        triggerButtonHaptic()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _updateStatus.value = UpdateStatus.Error("Download error: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun markUpdateInstalled(publishedAt: String) {
        prefs.edit().putString("last_installed_release_time", publishedAt).apply()
    }

    fun isCrashReportingEnabled(): Boolean {
        return prefs.getBoolean("crash_reporting_enabled", false)
    }

    fun setCrashReportingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("crash_reporting_enabled", enabled).apply()
    }

    fun sendManualCrashReport() {
        val app = getApplication<Application>()
        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
            data = android.net.Uri.parse("mailto:")
            putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf("kouroudesantonios@gmail.com"))
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Manual Crash Report")
            putExtra(android.content.Intent.EXTRA_TEXT, "Hello Antonis,\n\nI am experiencing the following issue:\n\n")
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            app.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getMostRecentSaturday(): String {
        val cal = java.util.Calendar.getInstance()
        while (cal.get(java.util.Calendar.DAY_OF_WEEK) != java.util.Calendar.SATURDAY) {
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
        }
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        return sdf.format(cal.time)
    }

    private val _aiSaturdayMessage = MutableStateFlow<String?>(null)
    val aiSaturdayMessage: StateFlow<String?> = _aiSaturdayMessage.asStateFlow()

    private val _aiSaturdayLoading = MutableStateFlow(false)
    val aiSaturdayLoading: StateFlow<Boolean> = _aiSaturdayLoading.asStateFlow()

    private val _aiSaturdayOriginalMessageOnly = MutableStateFlow(prefs.getBoolean("ai_saturday_original_message_only", false))
    val aiSaturdayOriginalMessageOnly: StateFlow<Boolean> = _aiSaturdayOriginalMessageOnly.asStateFlow()

    fun updateAiSaturdayOriginalMessageOnly(value: Boolean) {
        _aiSaturdayOriginalMessageOnly.value = value
        prefs.edit().putBoolean("ai_saturday_original_message_only", value).apply()
    }

    fun generateAiSaturdayPopupMessage(appLanguage: String, isFirstInstall: Boolean = false) {
        if (_aiSaturdayOriginalMessageOnly.value) return
        
        _aiSaturdayMessage.value = null
        _aiSaturdayLoading.value = true
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val apiKey = getEffectiveGeminiApiKey()
            if (apiKey == null) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    _aiSaturdayLoading.value = false
                }
                return@launch
            }

            val logs = try {
                repository.getAllLogs().first()
            } catch (e: Exception) {
                emptyList()
            }

            val cal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val todayDayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
            val daysToSubtract = when (todayDayOfWeek) {
                java.util.Calendar.SUNDAY -> 6
                java.util.Calendar.MONDAY -> 0
                else -> todayDayOfWeek - java.util.Calendar.MONDAY
            }
            val mondayCal = (cal.clone() as java.util.Calendar).apply {
                add(java.util.Calendar.DAY_OF_YEAR, -daysToSubtract)
            }
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val dayNameFormat = java.text.SimpleDateFormat("EEEE", java.util.Locale.US)

            val weekProgressInfo = StringBuilder()
            var totalDaysChecked = 0
            var daysGoalMet = 0
            var daysNoLog = 0

            for (i in 0 until daysToSubtract) {
                val checkCal = (mondayCal.clone() as java.util.Calendar).apply {
                    add(java.util.Calendar.DAY_OF_YEAR, i)
                }
                val dateStr = sdf.format(checkCal.time)
                val dayName = dayNameFormat.format(checkCal.time)
                
                val dayLogs = logs.filter { it.dateString == dateStr }
                val totalIntake = dayLogs.sumOf { it.waterEquivalentMl }
                val dayGoal = getDailyGoalForDate(dateStr)
                val percentage = if (dayGoal > 0) (totalIntake * 100) / dayGoal else 100
                
                if (dayLogs.isEmpty()) {
                    daysNoLog++
                    weekProgressInfo.append("- $dayName ($dateStr): No water logged at all (0% of goal).\n")
                } else {
                    if (totalIntake >= dayGoal) {
                        daysGoalMet++
                        weekProgressInfo.append("- $dayName ($dateStr): Goal reached! Drank $totalIntake ml / $dayGoal ml (100% or more of goal).\n")
                    } else {
                        weekProgressInfo.append("- $dayName ($dateStr): Goal missed. Drank $totalIntake ml / $dayGoal ml ($percentage% of goal reached).\n")
                    }
                }
                totalDaysChecked++
            }

            val mbtiFull = _userMbti.value.uppercase().trim()
            val mbtiInstruction = if (mbtiFull.isNotEmpty()) {
                val baseMbti = mbtiFull.take(4)
                var style = ""
                when {
                    baseMbti in listOf("INTJ", "INTP", "ENTJ", "ENTP") -> {
                        style = "Style: Clinical, intellectual, highly analytical, scientific, and direct. Focus on goal tracking efficiency."
                    }
                     baseMbti in listOf("INFJ", "INFP", "ENFJ", "ENFP") -> {
                        style = "Style: Deeply personal, exceptionally empathetic, encouraging, and warm. Focus on mindfulness and self-love."
                    }
                    baseMbti in listOf("ISTJ", "ISFJ", "ESTJ", "ESFJ") -> {
                        style = "Style: Practical, organized, clear, and structured. Focus on discipline, routines, and habit building."
                    }
                    baseMbti in listOf("ISTP", "ISFP", "ESTP", "ESFP") -> {
                        style = "Style: High-energy, direct, playful, and fun. Use punchy phrasing and actionable cheering."
                    }
                }
                "\nMBTI CONFIGURATION:\n$style\nYou must completely adapt your congratulations message to this MBTI personality."
            } else ""

            val bigFiveInstruction = if (_userBigFiveO.value != -1) {
                """
                BIG FIVE ADJUSTMENTS:
                 Openness: ${_userBigFiveO.value}%
                 Conscientiousness: ${_userBigFiveC.value}%
                 Extraversion: ${_userBigFiveE.value}%
                 Agreeableness: ${_userBigFiveA.value}%
                 Neuroticism: ${_userBigFiveN.value}%
                Adjust your tone based on these Big Five traits (e.g., if Neuroticism is high, be extra soothing; if Openness is high, be more metaphoric/deep).
                """.trimIndent()
            } else ""

            val targetLang = if (appLanguage == "el") "Greek" else "English"

            val systemPrompt = """
                You are Pixel Water AI, the user's hydration motivation coach.
                Today is Saturday, the end of the hydration tracking week, and you are displaying the "Saturday Congratulations" popup message.
                You MUST evaluate their performance for each day of the week from Monday up to yesterday (Friday) using their actual water log data.
                
                The user's actual hydration logs and performance for each day of the week till yesterday are:
                $weekProgressInfo
                
                SUMMARY STATS:
                - Days checked: $totalDaysChecked
                - Days where they met their goal: $daysGoalMet
                - Days where they did not log any water: $daysNoLog
                
                $mbtiInstruction
                $bigFiveInstruction
                
                REQUIREMENTS:
                1. Keep the message simple, short, with nice supporting energy (max 3-4 sentences).
                2. Do NOT be "plushy" (too soft, overly forgiving, or babying) on the user if they slacked off, missed logs, or failed to meet their goals. If they missed days or goals, be direct, honest, and strict about their performance, calling out where they fell short (such as missing goals or not logging at all), but remain highly encouraging, constructive, and motivating. If they did exceptionally well, celebrate their dedication!
                3. You MUST include a big kaomoji / text face title on the first line (e.g. (＾∇ηγ) or ＼(≧▽≦)／ or (︶︹︶) or ಠ_ಠ) that fits their performance (e.g., standard celebratory face for perfect score, a sweat/shrug/encouraging face if they missed days/goals, disappointed/strict face if they slacked off heavily). You can swap it with a new appropriate one.
                4. Never use emojis like 🎉 unless it suits the MBTI. 
                5. The output must be perfectly formatted for a popup dialog. The first line should be the text face, followed by a blank line, and then the text.
                 6. The text must be in $targetLang.
                7. Do NOT output markdown or conversation filler like "Here is your message:".
            """.trimIndent()

            val prompt = "Today is Saturday. The user reached the end of the hydration tracking week. Give them the AI-driven weekly congratulations popup according to the instructions."

            val contents = listOf(com.pixelwater.app.data.Content(role = "user", parts = listOf(com.pixelwater.app.data.Part(text = prompt))))
            val request = com.pixelwater.app.data.GeminiRequest(
                contents = contents,
                systemInstruction = com.pixelwater.app.data.Content(parts = listOf(com.pixelwater.app.data.Part(text = systemPrompt))),
                generationConfig = com.pixelwater.app.data.GenerationConfig(temperature = 0.8f, maxOutputTokens = 300),
                tools = null
            )

            try {
                val response = safeGenerateContent(
                    model = _insightGeminiModel.value,
                    apiKey = apiKey,
                    request = request,
                    onModelChosen = { _lastUsedInsightModel.value = it },
                    isInsight = true
                )
                val replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (!replyText.isNullOrBlank()) {
                        _aiSaturdayMessage.value = replyText
                    }
                    _aiSaturdayLoading.value = false
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    _aiSaturdayLoading.value = false
                }
            }
        }
    }

    fun shouldShowSaturdayPopup(): Boolean {
        if (!prefs.contains("last_shown_saturday")) {
            // First time ever opening the app on a fresh install.
            // Initialize the last shown Saturday so we do not show a congratulations popup immediately on first launch.
            prefs.edit().putString("last_shown_saturday", getMostRecentSaturday()).apply()
            return false
        }
        val mostRecent = getMostRecentSaturday()
        val lastShown = prefs.getString("last_shown_saturday", "") ?: ""
        return mostRecent.isNotEmpty() && mostRecent != lastShown
    }

    fun markSaturdayPopupShown() {
        val mostRecent = getMostRecentSaturday()
        prefs.edit().putString("last_shown_saturday", mostRecent).apply()
    }

    fun loadCustomDrinks() {
        val rawStr = prefs.getString("custom_drinks_list", "") ?: ""
        if (rawStr.isBlank()) {
            _customDrinks.value = emptyList()
            return
        }
        val list = mutableListOf<CustomDrink>()
        val parts = rawStr.split(";;")
        for (part in parts) {
            if (part.isBlank()) continue
            val itemParts = part.split("|")
            if (itemParts.size >= 3) {
                val name = itemParts[0]
                val factor = itemParts[1].toFloatOrNull() ?: 1.0f
                val iconName = itemParts[2]
                list.add(CustomDrink(name, factor, iconName))
            }
        }
        _customDrinks.value = list
    }

    fun saveCustomDrink(name: String, factor: Float, iconName: String) {
        val current = _customDrinks.value.toMutableList()
        current.removeAll { it.name.equals(name, ignoreCase = true) }
        current.add(CustomDrink(name, factor, iconName))
        
        val rawStr = current.joinToString(";;") { "${it.name}|${it.factor}|${it.iconName}" }
        prefs.edit().putString("custom_drinks_list", rawStr).apply()
        _customDrinks.value = current
    }

    fun deleteCustomDrink(name: String) {
        val current = _customDrinks.value.toMutableList()
        current.removeAll { it.name.equals(name, ignoreCase = true) }
        val rawStr = current.joinToString(";;") { "${it.name}|${it.factor}|${it.iconName}" }
        prefs.edit().putString("custom_drinks_list", rawStr).apply()
        _customDrinks.value = current
    }

    fun analyzeCustomLiquid(
        liquidName: String,
        appLanguage: String,
        onSuccess: (Float, String, String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val systemPrompt = """
                    You are "Pixel Water AI", an expert helpful hydration coach.
                    Analyze the hydration factor and profile of custom liquids or beverages relative to pure water (which has a factor status of 1.0 or 100%).
                    Speak facts. Respond only in the exact structured text format specified below.
                """.trimIndent()

                val prompt = """
                    Search and analyze the hydration profile of the custom drink/liquid specified below relative to pure water (where pure water is 1.00 or 100%).
                    
                    Custom Liquid Name: "$liquidName"
                    
                    Provide your analysis by strictly outputting a parseable block of text in the following format:
                    FACTOR: [decimal representation of hydration percentage, e.g. 0.85 for 85%]
                    ICON: [one of: waterdrop, coffee, tea, juice, milk, soda, bolt, flash, soup, beer, wine, eco, icecream, spark, lemon, science]
                    EXPLANATION: [a brief 1-sentence supportive coach explanation of its hydration properties in ${if (appLanguage == "el") "Greek" else "English"}]
                    
                    Do not output any other introduction, HTML, or conversational text. Speak facts.
                """.trimIndent()

                val apiKey = getEffectiveGeminiApiKey()
                if (apiKey != null) {
                    val contents = listOf(
                        com.pixelwater.app.data.Content(
                            role = "user",
                            parts = listOf(com.pixelwater.app.data.Part(text = prompt))
                        )
                    )
                    val request = com.pixelwater.app.data.GeminiRequest(
                        contents = contents,
                        systemInstruction = com.pixelwater.app.data.Content(parts = listOf(com.pixelwater.app.data.Part(text = systemPrompt))),
                        generationConfig = com.pixelwater.app.data.GenerationConfig(temperature = 0.5f, maxOutputTokens = 300)
                    )
                    val activeModel = _geminiModel.value
                    
                    val response = safeGenerateContent(
                        model = activeModel,
                        apiKey = apiKey,
                        request = request
                    )
                    val replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    
                    if (replyText != null) {
                        var parsedFactor = 1.00f
                        var parsedIconName = "localdrink"
                        var parsedExplanation = ""
                        
                        val lines = replyText.lines()
                        for (line in lines) {
                            val trimmed = line.trim()
                            if (trimmed.startsWith("FACTOR:", ignoreCase = true)) {
                                val valueStr = trimmed.substring(7).trim()
                                parsedFactor = valueStr.toFloatOrNull() ?: 1.00f
                            } else if (trimmed.startsWith("ICON:", ignoreCase = true)) {
                                parsedIconName = trimmed.substring(5).trim().lowercase()
                            } else if (trimmed.startsWith("EXPLANATION:", ignoreCase = true)) {
                                parsedExplanation = trimmed.substring(12).trim()
                            }
                        }
                        
                        parsedFactor = parsedFactor.coerceIn(0.1f, 1.5f)
                        onSuccess(parsedFactor, parsedIconName, parsedExplanation)
                    } else {
                        throw Exception("No response received from the AI Coach.")
                    }
                } else {
                    throw Exception("Gemini API Key is unconfigured. Please configure it in the developer settings first.")
                }
            } catch (e: Exception) {
                Log.e("WaterViewModel", "analyzeCustomLiquid failed", e)
                onError(e.localizedMessage ?: "AI service request failed")
            }
        }
    }

    private val _knownInvalidGeminiKeys = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    private fun markApiKeyInvalid(key: String) {
        val trimmed = key.trim()
        if (trimmed.isNotBlank()) {
            _knownInvalidGeminiKeys.add(trimmed)
        }
    }

    private fun isGeminiAuthOrKeyError(e: Throwable): Boolean {
        if (e is retrofit2.HttpException) {
            val code = e.code()
            if (code == 401 || code == 403) return true
            if (code == 400) {
                val errorBody = try {
                    e.response()?.errorBody()?.string() ?: ""
                } catch (_: Exception) {
                    ""
                }
                if (errorBody.contains("API_KEY_INVALID", ignoreCase = true) ||
                    errorBody.contains("API key not valid", ignoreCase = true) ||
                    errorBody.contains("API_KEY_EXPIRED", ignoreCase = true) ||
                    errorBody.contains("keyExpired", ignoreCase = true) ||
                    (errorBody.contains("INVALID_ARGUMENT", ignoreCase = true) && errorBody.contains("key", ignoreCase = true))
                ) {
                    return true
                }
            }
        }
        val msg = e.message ?: ""
        return msg.contains("API_KEY_INVALID", ignoreCase = true) ||
               msg.contains("API key not valid", ignoreCase = true) ||
               msg.contains("HTTP 401") ||
               msg.contains("HTTP 403")
    }

    fun getEffectiveGeminiApiKey(): String? {
        val userKey = _geminiApiKey.value.trim()
        val candidate = if (userKey.isNotBlank()) {
            userKey
        } else {
            com.pixelwater.app.BuildConfig.GEMINI_API_KEY.trim()
        }
        if (candidate.isBlank() ||
            candidate == "MY_GEMINI_API_KEY" ||
            candidate == "YOUR_GEMINI_API_KEY" ||
            _knownInvalidGeminiKeys.contains(candidate)
        ) {
            return null
        }
        return candidate
    }

    private suspend fun safeGenerateContent(
        model: String,
        apiKey: String,
        request: com.pixelwater.app.data.GeminiRequest,
        onModelChosen: ((String) -> Unit)? = null,
        isInsight: Boolean = false
    ): com.pixelwater.app.data.GeminiResponse {
        val modelsToTry = mutableListOf<String>()
        if (model == "gemini-auto") {
            if (isInsight) {
                modelsToTry.addAll(listOf(
                    "gemini-3.1-flash-lite-preview",
                    "gemini-2.5-flash-lite",
                    "gemini-3.5-flash",
                    "gemini-2.5-flash",
                    "gemini-3.1-pro-preview"
                ))
            } else {
                modelsToTry.addAll(listOf(
                    "gemini-3.5-flash",
                    "gemini-2.5-flash",
                    "gemini-3.1-flash-lite-preview",
                    "gemini-2.5-flash-lite",
                    "gemini-3.1-pro-preview"
                ))
            }
        } else {
            modelsToTry.add(model)
            val isAudioRequest = model.contains("audio") || model.contains("lyria")
            val backupModels = if (isAudioRequest) {
                listOf(
                    "lyria-3-clip-preview",
                    "gemini-2.5-flash-native-audio-preview-12-2025",
                    "lyria-3-pro-preview"
                )
            } else if (isInsight) {
                listOf(
                    "gemini-3.1-flash-lite-preview",
                    "gemini-2.5-flash-lite",
                    "gemini-3.5-flash",
                    "gemini-2.5-flash",
                    "gemini-3.1-pro-preview"
                )
            } else {
                listOf(
                    "gemini-3.5-flash",
                    "gemini-2.5-flash",
                    "gemini-3.1-flash-lite-preview",
                    "gemini-2.5-flash-lite",
                    "gemini-3.1-pro-preview"
                )
            }
            backupModels.forEach { fallback ->
                if (fallback != model && !modelsToTry.contains(fallback)) {
                    modelsToTry.add(fallback)
                }
            }
        }
        
        var lastException: Exception? = null
        for (currentModel in modelsToTry) {
            try {
                Log.d("WaterViewModel", "Attempting Gemini call with model: $currentModel")
                val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.pixelwater.app.data.GeminiRetrofitClient.service.generateContent(
                        model = currentModel,
                        apiKey = apiKey,
                        request = request
                    )
                }
                if (response.candidates != null) {
                    if (onModelChosen != null) {
                        onModelChosen.invoke(currentModel)
                    } else {
                        _lastUsedCoachModel.value = currentModel
                    }
                    return response
                }
            } catch (e: Exception) {
                lastException = e
                if (isGeminiAuthOrKeyError(e)) {
                    val keyPreview = if (apiKey.length > 8) "${apiKey.take(4)}...${apiKey.takeLast(4)}" else apiKey
                    Log.w("WaterViewModel", "Gemini API key is invalid or unauthorized ($keyPreview). Stopping fallbacks.")
                    markApiKeyInvalid(apiKey)
                    break
                }
                Log.w("WaterViewModel", "Gemini call failed with model: $currentModel (${e.message}), trying fallback...")
            }
        }
        throw lastException ?: Exception("Unknown error in safeGenerateContent")
    }

    fun saveSetupData(
        weight: Float, 
        height: Float, 
        baseGoal: Int, 
        upperGoal: Int, 
        creatine: Boolean,
        proteinEnabled: Boolean = false,
        proteinFixed: Boolean = true,
        proteinMin: Int = 180,
        proteinMax: Int = 200,
        creatineOption: String = "5G",
        creatineGrams: Int = 5
    ) {
        prefs.edit().apply {
            putFloat("setup_weight", weight)
            putFloat("setup_height", height)
            putInt("setup_goal_35", baseGoal)
            putInt("setup_goal_40", upperGoal)
            putBoolean("creatine_enabled", creatine)
            putString("creatine_option", creatineOption)
            putInt("creatine_grams", creatineGrams)
            putBoolean("creatine_upgrade_prompt_shown", true)
            putBoolean("protein_enabled", proteinEnabled)
            putBoolean("protein_fixed", proteinFixed)
            putInt("protein_min", proteinMin)
            putInt("protein_max", proteinMax)
            putBoolean("has_completed_setup", true)
            apply()
        }
        _setupWeight.value = weight
        _setupHeight.value = height
        _creatineEnabled.value = creatine
        _creatineOption.value = creatineOption
        _creatineGrams.value = creatineGrams
        _showRecentlyAddedBubble.value = false
        _proteinEnabled.value = proteinEnabled
        _proteinFixed.value = proteinFixed
        _proteinMin.value = proteinMin
        _proteinMax.value = proteinMax
        _hasCompletedSetup.value = true
        updateDailyGoal(baseGoal)
    }

    fun resetSetup() {
        prefs.edit().putBoolean("has_completed_setup", false).apply()
        _hasCompletedSetup.value = false
    }

    fun detectWorkoutsFromHealthConnect() {
        if (!_workoutWaterAdjustmentEnabled.value) return
        viewModelScope.launch {
            try {
                val context = getApplication<android.app.Application>()
                val authorized = HealthConnectManager.hasAllPermissions(context)
                if (!authorized) {
                    val fallbackBrief = if (appLanguage.value == "el") "Εκκρεμεί άδεια Health Connect" else "Health Connect authorization pending"
                    _todayWorkoutAiCoachResponse.value = fallbackBrief
                    return@launch
                }
                
                // Read exercise records for today (start of day to now)
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startTime = java.time.Instant.ofEpochMilli(calendar.timeInMillis)
                val endTime = java.time.Instant.ofEpochMilli(System.currentTimeMillis())
                
                val sessions = HealthConnectManager.readExerciseSessions(context, startTime, endTime)
                if (sessions.isNotEmpty()) {
                    // Pick the longest session or first session
                    val primarySession = sessions.maxByOrNull {
                        java.time.Duration.between(it.startTime, it.endTime).toMinutes()
                    } ?: sessions.first()
                    
                    val durationMins = java.time.Duration.between(primarySession.startTime, primarySession.endTime).toMinutes().toInt().coerceAtLeast(1)
                    val workoutTitle = primarySession.title ?: if (primarySession.exerciseType != 0) com.pixelwater.app.data.HealthConnectManager.getExerciseTypeName(primarySession.exerciseType) else "Active Workout"

                    val isWalking = workoutTitle.lowercase(Locale.US).contains("walk")
                    if (isWalking && !_workoutWalkingCounts.value) {
                         if (_todayWorkoutWaterBonus.value == 0) {
                            _todayWorkoutAiCoachResponse.value = if (appLanguage.value == "el") "Αγνοείται το περπάτημα βάσει ρυθμίσεων." else "Walking ignored by choice."
                         }
                         return@launch
                    }
                    
                    val key = "dismissed_workout_${getCurrentDateString()}_${workoutTitle}"
                    if (prefs.getBoolean(key, false)) {
                         return@launch
                    }

                    processWorkoutWithAI(workoutTitle, durationMins)
                } else {
                    // No workout detected
                    if (_todayWorkoutWaterBonus.value == 0) {
                        _todayWorkoutAiCoachResponse.value = if (appLanguage.value == "el") "Δεν εντοπίστηκε προπόνηση για σήμερα στο Google Health Connect." else "No workouts detected in Google Health Connect for today yet."
                    }
                }
            } catch (e: Throwable) {
                Log.e("WaterViewModel", "Failed to query workouts: ${e.message}")
            }
        }
    }

    private fun isWorkoutOutdoors(workoutType: String): Boolean {
        val wtLower = workoutType.lowercase(Locale.US)
        return wtLower.contains("outdoor") ||
               wtLower.contains("run") ||
               wtLower.contains("walk") ||
               wtLower.contains("hiking") ||
               wtLower.contains("trail") ||
               wtLower.contains("cycle") ||
               wtLower.contains("climb") ||
               wtLower.contains("soccer") ||
               wtLower.contains("football")
    }

    private fun processWorkoutWithAI(workoutType: String, durationMinutes: Int) {
        viewModelScope.launch {
            try {
                val apiKey = getEffectiveGeminiApiKey()
                val weightKg = if (_setupWeight.value > 10f) _setupWeight.value else 75f
                
                if (apiKey != null) {
                    var prompt = """
                        Act as an expert sports science and hydration AI coach.
                        The athlete has completed a workout:
                        - Workout Type: $workoutType
                        - Duration: $durationMinutes minutes
                        - Athlete Body Weight: $weightKg kg
                    """.trimIndent()

                    val isOutdoor = isWorkoutOutdoors(workoutType)
                    val temp = _weatherTemperature.value
                    if (isOutdoor && temp != null) {
                        prompt += "\n- Location Conditions: Outdoor workout conducted under temperature of ${String.format("%.1f", temp)}°C (${String.format("%.1f", temp * 1.8 + 32)}°F)."
                        if (temp >= 30.0) {
                            prompt += "\n- ALERT: This is an outdoor session in extreme summer heat! Perspiration rate is highly elevated. Please scale the recommended water bonus upwards (within safe constraints, e.g., typically between 400ml and 1400ml depending on intensity and body mass) to counteract heat stress and sweat loss."
                        }
                    }

                    prompt += """
                        
                        1. Analyze how much extra water they need (considering sweat rate, body weight of $weightKg kg, and general athletic guidelines).
                        2. Determine an exact amount of water in milliliters (ml) to raise their daily hydration goal by, specifically adjusted for their $weightKg kg body mass. This should typically range from 250ml to 1200ml depending on intensity (e.g. 5ml - 12ml per minute of moderate/high effort depending on body mass).
                        3. Provide a motivating 2-3 sentence feedback explanation explaining why they need this extra water.
                        
                        IMPORTANT: You MUST respond in a clean JSON format containing EXACTLY the keys: "additionalMl" (integer) and "rationale" (string). Response must contain only the valid JSON, No markdown blocks except optionally ```json ... ```.
                    """.trimIndent()
                    
                    prompt += getMbtiToneInstruction(requireScientificPracticality = true)
                    prompt += getBigFiveToneInstruction(requireScientificPracticality = true)
                    
                    if (appLanguage.value == "el") {
                        prompt += "\nSPECIAL INSTRUCTION: The value for the \"rationale\" field in the returned JSON MUST be written entirely in Greek."
                    }
                    
                    val request = com.pixelwater.app.data.GeminiRequest(
                        contents = listOf(
                            com.pixelwater.app.data.Content(
                                parts = listOf(com.pixelwater.app.data.Part(text = prompt))
                            )
                        )
                    )
                    
                    val response = safeGenerateContent(_geminiModel.value, apiKey, request)
                    val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (reply != null) {
                        val parsed = parseWorkoutAiResponse(reply)
                        proposeWorkoutWaterBonus(workoutType, durationMinutes, parsed.first, parsed.second)
                    } else {
                        proposeOfflineWorkoutFallback(workoutType, durationMinutes)
                    }
                } else {
                    proposeOfflineWorkoutFallback(workoutType, durationMinutes)
                }
            } catch (e: Throwable) {
                Log.e("WaterViewModel", "AI workout analysis failed: ${e.message}")
                proposeOfflineWorkoutFallback(workoutType, durationMinutes)
            }
        }
    }

    private fun getMbtiToneInstruction(requireScientificPracticality: Boolean = false): String {
        val mbtiFull = _userMbti.value.uppercase().trim()
        val mbti = mbtiFull.take(4)
        if (mbti.isNotEmpty()) {
            val style = when {
                mbti in listOf("INTJ", "INTP", "ENTJ", "ENTP") -> {
                    "clinical, professional, intellectual, highly analytical, scientific, and dry. Do not use hyper emotional, cheesy motivational phrases or fluffy emojis. Rely on biology, physiology, efficiency, and logical correctness suited for a rational $mbti."
                }
                mbti in listOf("INFJ", "INFP", "ENFJ", "ENFP") -> {
                    "extremely warm, supportive, deeply personal, encouraging, holistic, mindful, and values-connected. Use supportive metaphors and warm phrasing suited for a diplomatic $mbti."
                }
                mbti in listOf("ISTJ", "ISFJ", "ESTJ", "ESFJ") -> {
                    "routine-oriented, practical, clear, organized, direct, and structured. Focus on practical habit-building, routines, discipline, and milestones suited for a sentinel $mbti."
                }
                mbti in listOf("ISTP", "ISFP", "ESTP", "ESFP") -> {
                    "high energy, direct, actionable, playful, quick, and engaging. Keep it short, punchy, active, and lighthearted suited for an explorer $mbti."
                }
                else -> "balanced, supportive, and informative."
            }

            var extraTraitsInstruction = ""
            if (mbtiFull.length >= 7 && mbtiFull[4] == '-') {
                val axis5 = mbtiFull[5]
                val axis6 = mbtiFull.getOrNull(6) ?: ' '
                val trait5 = if (axis5 == 'A') "Assertive (adapts firmly without oscillating)" else if (axis5 == 'O') "Oscillating (fluid, changes mind dynamically)" else ""
                val trait6 = if (axis6 == 'H') "Harmony (prioritizes deep emotional connection over detachment)" else if (axis6 == 'C') "Calm (maintains a highly detached and calm emotional core)" else ""
                extraTraitsInstruction = "Extended Personality Traits:\n- Axis 5 (Adaptation): $trait5\n- Axis 6 (Emotional Core): $trait6\n"
            }

            val scientificOverride = if (requireScientificPracticality) {
                "\nCRITICAL: Despite these personality stylings, you MUST NOT lose your practicality and deeply scientific and reasonable approach for this technical health insight. Ensure the logic remains scientifically robust and medically sound."
            } else ""

            return "\n[MBTI INSTRUCTION FOR USER TYPE: $mbtiFull]\n- Style and Tone Requirement: Your returned \"rationale\"/feedback must be styled in a way that is $style\n$extraTraitsInstruction$scientificOverride"
        }
        return ""
    }

    private fun getBigFiveToneInstruction(requireScientificPracticality: Boolean = false): String {
        if (_userBigFiveO.value == -1) return ""
        
        val scientificOverride = if (requireScientificPracticality) {
            "NOTE: Even with this personality tailoring, strictly maintain concrete physiological logic and high-level scientific practicality."
        } else ""

        return """
            [BIG FIVE PERSONALITY INSTRUCTION]
            Blend these Big Five traits to influence the structure and content of your message:
            - Openness to Experience (${_userBigFiveO.value}/100): ${if (_userBigFiveO.value > 60) "Be more creative, use analogies and novel scientific facts." else "Be straightforward, traditional, and stick to the basics without overly abstract analogies."}
            - Conscientiousness (${_userBigFiveC.value}/100): ${if (_userBigFiveC.value > 60) "Focus intensely on goal-setting, exact precision, discipline, and milestones." else "Keep it flexible, forgiving, and less rigid about exact scheduling."}
            - Extraversion (${_userBigFiveE.value}/100): ${if (_userBigFiveE.value > 60) "Use highly engaging, energetic, enthusiastic, and socially confident language." else "Use a calm, restrained, introspective, and highly focused tone."}
            - Agreeableness (${_userBigFiveA.value}/100): ${if (_userBigFiveA.value > 60) "Communicate with immense warmth, empathy, politeness, and encouragement." else "Be extremely objective, skeptical, challenging, and strictly factual over polite."}
            - Neuroticism (${_userBigFiveN.value}/100): ${if (_userBigFiveN.value > 60) "Provide high emotional reassurance, emphasize safety, consistency, and reducing stress or anxiety." else "Remain stoic, highly resilient, and assume the user handles challenges easily without needing emotional comfort."}
            
            $scientificOverride
        """.trimIndent()
    }

    private fun parseWorkoutAiResponse(jsonText: String): Pair<Int, String> {
        return try {
            // Remove markdown codeblock qualifiers if present
            val raw = jsonText.replace("```json", "").replace("```", "").trim()
            val additionIndex = raw.indexOf("\"additionalMl\"")
            val rawValueIndex = raw.indexOf(":", additionIndex)
            val commaIndex = raw.indexOf(",", rawValueIndex)
            val endObjIndex = raw.indexOf("}", rawValueIndex)
            val targetEnd = if (commaIndex != -1 && commaIndex < endObjIndex) commaIndex else endObjIndex
            val additionStr = raw.substring(rawValueIndex + 1, targetEnd).replace("\"", "").trim()
            val additionalMl = additionStr.toIntOrNull() ?: 500
            
            val rationaleIndex = raw.indexOf("\"rationale\"")
            val ratValueStart = raw.indexOf(":", rationaleIndex)
            val firstQuoteOfStr = raw.indexOf("\"", ratValueStart)
            val lastQuoteOfStr = raw.lastIndexOf("\"")
            val rationale = if (firstQuoteOfStr != -1 && lastQuoteOfStr != -1 && lastQuoteOfStr > firstQuoteOfStr) {
                raw.substring(firstQuoteOfStr + 1, lastQuoteOfStr)
            } else {
                if (appLanguage.value == "el") {
                    "Εξαιρετική προσπάθεια! Είναι ζωτικής σημασίας να αναπληρώσετε τα υγρά σας με μια επιπλέον ώθηση ενυδάτωσης."
                } else {
                    "Great effort! It's vital to restore sweat losses with an extra intake boost."
                }
            }
            Pair(additionalMl.coerceIn(100, 2000), rationale)
        } catch (e: Throwable) {
            val fallbackMsg = if (appLanguage.value == "el") {
                "Εξαιρετική προσπάθεια! Η αποκατάσταση της ενυδάτωσης είναι καθοριστική μετά την προπόνηση."
            } else {
                "Excellent effort! Restoring hydration is crucial after a workout."
            }
            Pair(500, fallbackMsg)
        }
    }

    private fun proposeOfflineWorkoutFallback(workoutType: String, durationMinutes: Int) {
        val isOutdoor = isWorkoutOutdoors(workoutType)
        val temp = _weatherTemperature.value
        var bonus = calculateLocalWorkoutFallback(workoutType, durationMinutes)
        
        if (isOutdoor && temp != null && temp >= 30.0) {
            bonus += 250
        }
        val estimatedAddition = bonus.coerceIn(200, 1500)
        
        val explainText = if (appLanguage.value == "el") {
            if (isOutdoor && temp != null && temp >= 30.0) {
                "Υπαίθρια προπόνηση ($workoutType, $durationMinutes λεπτ.) υπό έντονη ζέστη (${String.format("%.1f", temp)}°C) αυξάνει την ανάγκη αναπλήρωσης. Προτείνεται +${estimatedAddition}ml."
            } else {
                "Εξαιρετική προπόνηση! Για $durationMinutes λεπτά $workoutType, υπολογίσαμε τοπικά μια αναγκαία αύξηση νερού κατά $estimatedAddition ml για να αναπληρώσετε τα υγρά σας."
            }
        } else {
            if (isOutdoor && temp != null && temp >= 30.0) {
                "Outdoor session ($workoutType, $durationMinutes mins) in extreme heat (${String.format("%.1f", temp)}°C) induces heavy sweating. Hydration Coach recommends extra +${estimatedAddition}ml."
            } else {
                "Great workout! For $durationMinutes minutes of $workoutType, we translated sweat losses to an offline hydration target lift of +$estimatedAddition ml."
            }
        }
        proposeWorkoutWaterBonus(workoutType, durationMinutes, estimatedAddition, explainText)
    }

    private fun calculateLocalWorkoutFallback(workoutType: String, durationMinutes: Int): Int {
        val wtLower = workoutType.lowercase(Locale.US)
        val baseFactor = when {
            wtLower.contains("run") || wtLower.contains("hiit") || wtLower.contains("cardio") -> 12f // 12ml/min
            wtLower.contains("swim") || wtLower.contains("cycle") || wtLower.contains("bicycl") -> 10f
            wtLower.contains("lift") || wtLower.contains("strength") || wtLower.contains("weights") -> 8f
            wtLower.contains("yoga") || wtLower.contains("stretching") || wtLower.contains("walk") -> 5f
            else -> 8f
        }
        val userWeight = if (_setupWeight.value > 10f) _setupWeight.value else 75f
        val weightMultiplier = (userWeight / 75f).coerceIn(0.5f, 2.0f)
        val adjustedFactor = baseFactor * weightMultiplier
        return ((durationMinutes * adjustedFactor) / 50).toInt() * 50 // round to nearest 50ml
    }

    private fun proposeWorkoutWaterBonus(workoutType: String, durationMinutes: Int, bonusMl: Int, rationaleText: String) {
        if (_bypassWorkoutConfirmation.value) {
            applyWorkoutWaterBonus(bonusMl, rationaleText)
            val context = getApplication<android.app.Application>()
            val title = if (appLanguage.value == "el") "Προσθήκη Ενυδάτωσης Προπόνησης! 🏋️‍♂️" else "Workout Hydration Added! 🏋️‍♂️"
            val content = if (appLanguage.value == "el") {
                "Προστέθηκαν αυτόματα +${bonusMl}ml για την προπόνηση $workoutType."
            } else {
                "Automatically added +${bonusMl}ml for your $workoutType session."
            }
            com.pixelwater.app.notifications.NotificationHelper.showCustomNotification(context, title, content)
            return
        }

        val recoveryTips = generateRecoveryTips(workoutType, durationMinutes)
        val proposal = WorkoutProposal(
            workoutType = workoutType,
            durationMinutes = durationMinutes,
            bonusMl = bonusMl,
            rationale = rationaleText,
            recoveryTips = recoveryTips
        )
        _pendingWorkoutProposal.value = proposal

        val context = getApplication<android.app.Application>()
        val title = if (appLanguage.value == "el") "Εντοπίστηκε Προπόνηση! 🏋️‍♂️" else "Workout Detected! 🏋️‍♂️"
        val content = if (appLanguage.value == "el") {
            "Ολοκληρώσατε $workoutType. Έξτρα στόχος: +${bonusMl}ml. Πατήστε για αποδοχή & συμβουλές ανάκαμψης."
        } else {
            "You completed $workoutType! Extra goal: +${bonusMl}ml. Tap to accept and read recovery tips."
        }
        com.pixelwater.app.notifications.NotificationHelper.showCustomNotification(context, title, content)
    }

    private fun generateRecoveryTips(workoutType: String, durationMinutes: Int): String {
        val wtLower = workoutType.lowercase(Locale.US)
        return when {
            wtLower.contains("run") || wtLower.contains("hiit") || wtLower.contains("cardio") -> {
                if (appLanguage.value == "el") {
                    "Εξαιρετική αερόβια προπόνηση! Συμβουλές: εστιάστε στην αναπλήρωση γλυκογόνου, κάντε ήπιες στατικές διατάσεις για γάμπες και μηριαίους, και καταναλώστε ηλεκτρολύτες για να επαναφέρετε την ισορροπία νατρίου-καλίου."
                } else {
                    "Excellent aerobic work! Tips: focus on glycogen replenishment, gentle static stretching for calves and hamstrings, and consume electrolytes to restore sodium-potassium balance."
                }
            }
            wtLower.contains("swim") || wtLower.contains("cycle") || wtLower.contains("bike") || wtLower.contains("bicycl") -> {
                if (appLanguage.value == "el") {
                    "Φανταστική προπόνηση αντοχής! Συμβουλές: τεντώστε τους ώμους και τους τετρακέφαλους σας, σταθείτε σε όρθια στάση για να επεκτείνετε τους πνεύμονες και ξεκουραστείτε με τα πόδια ψηλά για να βοηθήσετε την κυκλοφορία του αίματος."
                } else {
                    "Terrific endurance session! Tips: stretch your shoulders and quadriceps, stay in vertical recovery posture to expand lungs, and rest with elevated legs to assist blood circulation."
                }
            }
            wtLower.contains("lift") || wtLower.contains("strength") || wtLower.contains("weight") || wtLower.contains("push") || wtLower.contains("pull") -> {
                if (appLanguage.value == "el") {
                    "Υπέροχη προπόνηση δύναμης! Συμβουλές: η πρωτεϊνοσύνθεση είναι ενεργή – δώστε προτεραιότητα σε 20-30g πρωτεΐνης εντός 2 ωρών, κάντε foam rolling σε στοχευμένες μυϊκές ομάδες και εστιάστε σε αργές διαφραγματικές αναπνοές."
                } else {
                    "Great resistance training! Tips: protein synthesis is active – prioritize 20-30g of protein within 2 hours, perform soft foam rolling on hyper-targeted muscle groups, and focus on slow diaphragmatic breathing."
                }
            }
            wtLower.contains("yoga") || wtLower.contains("stretching") || wtLower.contains("walk") || wtLower.contains("pilated") -> {
                if (appLanguage.value == "el") {
                    "Υπέροχη αναζωογονητική κίνηση! Συμβουλές: κάντε βαθιές αναπνοές από την κοιλιά, πιείτε ζεστά ροφήματα και συνεχίστε τη χαλάρωση σε έναν ήσυχο χώρο για να διατηρήσετε την παρασυμπαθητική σας κατάσταση."
                } else {
                    "Wonderful restorative movement! Tips: practice deep belly breathing, sip warm liquids, and continue relaxation in a quiet space to sustain your parasympathetic state."
                }
            }
            else -> {
                if (appLanguage.value == "el") {
                    "Πολύ καλή προπόνηση! Συμβουλές: χαλαρώστε με ενεργητικές διατάσεις, ενυδατωθείτε σταδιακά τις επόμενες 2 ώρες και εξασφαλίστε διατροφική υποστήριξη για την αποκατάσταση των μυών."
                } else {
                    "Superb workout! Tips: cool down with active stretching, hydrate incrementally over the next 2 hours, and ensure nutritional support for repair."
                }
            }
        }
    }

    fun acceptWorkoutWaterBonus(proposal: WorkoutProposal) {
        applyWorkoutWaterBonus(proposal.bonusMl, proposal.rationale)
        _pendingWorkoutProposal.value = null
    }

    fun declineWorkoutWaterBonus() {
        _pendingWorkoutProposal.value?.let { proposal ->
            val key = "dismissed_workout_${getCurrentDateString()}_${proposal.workoutType}"
            prefs.edit().putBoolean(key, true).apply()
        }
        _pendingWorkoutProposal.value = null
    }

    private fun applyWorkoutWaterBonus(bonusMl: Int, rationaleText: String) {
        val todayStr = getCurrentDateString()
        _todayWorkoutWaterBonus.value = bonusMl
        _todayWorkoutAiCoachResponse.value = rationaleText
        prefs.edit().putInt("workout_bonus_$todayStr", bonusMl).apply()
        prefs.edit().putString("workout_coach_$todayStr", rationaleText).apply()
        calculateStreak()
    }

    fun clearTodayWorkoutAdjustment() {
        val todayStr = getCurrentDateString()
        _todayWorkoutWaterBonus.value = 0
        _todayWorkoutAiCoachResponse.value = ""
        prefs.edit().remove("workout_bonus_$todayStr").apply()
        prefs.edit().remove("workout_coach_$todayStr").apply()
        calculateStreak()
    }

    fun detectSleepFromHealthConnect() {
        if (!_sleepWaterAdjustmentEnabled.value) return
        viewModelScope.launch {
            try {
                val context = getApplication<android.app.Application>()
                val authorized = HealthConnectManager.hasAllPermissions(context)
                if (!authorized) return@launch

                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                val startTime = java.time.Instant.ofEpochMilli(calendar.timeInMillis)
                val endTime = java.time.Instant.ofEpochMilli(System.currentTimeMillis())

                val sessions = HealthConnectManager.readSleepSessions(context, startTime, endTime)
                if (sessions.isNotEmpty()) {
                    val primarySession = sessions.maxByOrNull {
                        java.time.Duration.between(it.startTime, it.endTime).toMinutes()
                    } ?: sessions.first()

                    val durationMins = java.time.Duration.between(primarySession.startTime, primarySession.endTime).toMinutes().toInt().coerceAtLeast(1)
                    val durationHours = durationMins / 60.0

                    _todaySleepHours.value = durationHours
                    prefs.edit().putFloat("sleep_hours_${getCurrentDateString()}", durationHours.toFloat()).apply()

                    val key = "dismissed_sleep_${getCurrentDateString()}"
                    if (prefs.getBoolean(key, false)) {
                         return@launch
                    }

                    if (_aiCoachSleepDetectionEnabled.value) {
                         // Default behavior: anything below ~6.5 is often considered poor by AI Coach
                         if (durationHours < 6.5 && _todaySleepWaterBonus.value == 0) {
                             processPoorSleepWithAI(durationHours)
                         }
                    } else {
                         // Use user defined threshold 
                         if (durationHours <= _badSleepThreshold.value && _todaySleepWaterBonus.value == 0) {
                             processPoorSleepWithAI(durationHours)
                         }
                    }
                }
            } catch (e: Throwable) {
                Log.e("WaterViewModel", "Failed to query sleep sessions: ${e.message}")
            }
        }
    }

    private fun processPoorSleepWithAI(sleepHours: Double, isSimulation: Boolean = false) {
        viewModelScope.launch {
            try {
                val apiKey = getEffectiveGeminiApiKey()

                if (apiKey != null) {
                    var prompt = """
                        Act as an expert sleep physiology and hydration AI coach.
                        The user slept poorly last night, getting only ${String.format("%.1f", sleepHours)} hours of sleep.

                        1. Explain the scientific link between short sleep duration and dehydration (e.g., restriction disrupted late-cycle vasopressin excretion, signaling morning fluid loss and cognitive fatigue).
                        2. Recommend a reasonable extra water amount in milliliters (ml) to add to their hydration goal for today (typically 300ml to 600ml).
                        3. Response must be in clean JSON format containing EXACTLY the keys: "additionalMl" (integer) and "rationale" (string). Response must contain only the valid JSON, No markdown blocks except optionally ```json ... ```.
                    """.trimIndent()

                    prompt += getMbtiToneInstruction(requireScientificPracticality = true)
                    prompt += getBigFiveToneInstruction(requireScientificPracticality = true)

                    if (appLanguage.value == "el") {
                        prompt += "\nSPECIAL INSTRUCTION: The value for the \"rationale\" field in the returned JSON MUST be written entirely in Greek."
                    }

                    val request = com.pixelwater.app.data.GeminiRequest(
                        contents = listOf(
                            com.pixelwater.app.data.Content(
                                parts = listOf(com.pixelwater.app.data.Part(text = prompt))
                            )
                        )
                    )

                    val response = safeGenerateContent(_geminiModel.value, apiKey, request)
                    val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (reply != null) {
                        val parsed = parseWorkoutAiResponse(reply)
                        proposeSleepWaterProposal(sleepHours, parsed.first, parsed.second, isSimulation)
                    } else {
                        proposeOfflineSleepFallback(sleepHours, isSimulation)
                    }
                } else {
                    proposeOfflineSleepFallback(sleepHours, isSimulation)
                }
            } catch (e: Throwable) {
                Log.e("WaterViewModel", "AI sleep analysis failed: ${e.message}")
                proposeOfflineSleepFallback(sleepHours, isSimulation)
            }
        }
    }

    private fun proposeOfflineSleepFallback(sleepHours: Double, isSimulation: Boolean = false) {
        val estimatedAddition = 400
        val explainText = if (appLanguage.value == "el") {
            "Λιγότερο από 6.5 ώρες ύπνου μειώνουν την έκκριση βαζοπρεσίνης, με αποτέλεσμα πρωινή αφυδάτωση. Προτείνεται αύξηση στόχου κατά +400ml."
        } else {
            "Under 6.5 hours of sleep curtails late-stage vasopressin hormone release, increasing morning dehydration. Coach recommends boosting fluid intake by +400ml."
        }
        proposeSleepWaterProposal(sleepHours, estimatedAddition, explainText, isSimulation)
    }

    private fun proposeSleepWaterProposal(sleepHours: Double, bonusMl: Int, rationaleText: String, isSimulation: Boolean = false) {
        if (_bypassSleepConfirmation.value && !isSimulation) {
            applySleepWaterBonus(bonusMl, rationaleText)
            val context = getApplication<android.app.Application>()
            val title = if (appLanguage.value == "el") "Αυτόματη Ενυδάτωση Ύπνου! 😴" else "Sleep Hydration Auto-Applied! 😴"
            val content = if (appLanguage.value == "el") {
                "Προστέθηκαν αυτόματα +${bonusMl}ml λόγω ανεπαρκούς ύπνου (${String.format("%.1f", sleepHours)} ώρες)."
            } else {
                "Automatically added +${bonusMl}ml due to short sleep (${String.format("%.1f", sleepHours)} hours)."
            }
            com.pixelwater.app.notifications.NotificationHelper.showCustomNotification(context, title, content)
            return
        }

        val proposal = SleepProposal(
            sleepHours = sleepHours,
            bonusMl = bonusMl,
            rationale = rationaleText
        )
        _pendingSleepProposal.value = proposal

        val context = getApplication<android.app.Application>()
        val title = if (appLanguage.value == "el") "Ανεπαρκής Ύπνος; 😴" else "Poor Sleep Detected 😴"
        val content = if (appLanguage.value == "el") {
            "Κοιμηθήκατε μόνο ${String.format("%.1f", sleepHours)} ώρες. Προτείνεται +${bonusMl}ml για ταχεία ενυδάτωση."
        } else {
            "Slept only ${String.format("%.1f", sleepHours)} hrs. Coach suggests +${bonusMl}ml extra water for fluid recovery today."
        }
        com.pixelwater.app.notifications.NotificationHelper.showCustomNotification(context, title, content)
    }

    fun acceptSleepWaterBonus(proposal: SleepProposal) {
        applySleepWaterBonus(proposal.bonusMl, proposal.rationale)
        _pendingSleepProposal.value = null
    }

    fun declineSleepWaterBonus() {
        val key = "dismissed_sleep_${getCurrentDateString()}"
        prefs.edit().putBoolean(key, true).apply()
        _pendingSleepProposal.value = null
    }

    private fun applySleepWaterBonus(bonusMl: Int, rationaleText: String) {
        val todayStr = getCurrentDateString()
        _todaySleepWaterBonus.value = bonusMl
        _todaySleepAiCoachResponse.value = rationaleText
        prefs.edit().putInt("sleep_bonus_$todayStr", bonusMl).apply()
        prefs.edit().putString("sleep_coach_$todayStr", rationaleText).apply()
        calculateStreak()
    }

    fun clearTodaySleepAdjustment() {
        val todayStr = getCurrentDateString()
        _todaySleepWaterBonus.value = 0
        _todaySleepAiCoachResponse.value = ""
        _todaySleepHours.value = null
        prefs.edit().remove("sleep_bonus_$todayStr").apply()
        prefs.edit().remove("sleep_coach_$todayStr").apply()
        prefs.edit().remove("sleep_hours_$todayStr").apply()
        calculateStreak()
    }

    fun updateSleepWaterAdjustmentEnabled(enabled: Boolean) {
        _sleepWaterAdjustmentEnabled.value = enabled
        prefs.edit().putBoolean("sleep_water_adjustment_enabled", enabled).apply()
        if (!enabled) {
            clearTodaySleepAdjustment()
        } else {
            detectSleepFromHealthConnect()
        }
    }

    fun injectSimulatedWorkout(workoutType: String, durationMinutes: Int) {
        viewModelScope.launch {
            processWorkoutWithAI(workoutType, durationMinutes)
        }
    }

    fun injectSimulatedSleep(sleepHours: Double) {
        viewModelScope.launch {
            processPoorSleepWithAI(sleepHours, isSimulation = true)
        }
    }

    fun getAiWaterRecommendation(
        weight: Float,
        height: Float,
        creatine: Boolean,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val apiKey = if (_geminiApiKey.value.isNotBlank()) _geminiApiKey.value.trim() else com.pixelwater.app.BuildConfig.GEMINI_API_KEY.trim()
                if (apiKey != "MY_GEMINI_API_KEY" && apiKey.isNotBlank()) {
                    val prompt = """
                        Act as an expert biology and health science coach. The user is setting up their profile in Pixel Water tracker.
                        Provide a personal water intake recommendation with:
                        - Weight: $weight kg
                        - Height: $height cm
                        - Creatine Supplementation: ${if (creatine) "Yes" else "No"}
                        
                        Give a clear recommended range of water consumption in milliliters (ml) based on 35 to 40 ml per kg daily baseline, adding exactly 500ml for creatine supplement if enabled.
                        Then provide an encouraging 2-sentence bio-health justification of why proper cellular hydration is critical for their body composition.
                    """.trimIndent()
                    
                    val request = com.pixelwater.app.data.GeminiRequest(
                        contents = listOf(
                            com.pixelwater.app.data.Content(
                                parts = listOf(com.pixelwater.app.data.Part(text = prompt))
                            )
                        )
                    )
                    val response = safeGenerateContent(_geminiModel.value, apiKey, request)
                    val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (reply != null) {
                        onSuccess(reply)
                    } else {
                        throw Exception("Empty AI response content.")
                    }
                } else {
                    throw Exception("No AI Key")
                }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "AI call failed")
            }
        }
    }

    fun querySettingsAiCoach(
        userQuery: String,
        currentLanguage: String,
        onSuccess: (explanation: String, actions: List<Pair<String, String>>) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val apiKey = if (_geminiApiKey.value.isNotBlank()) _geminiApiKey.value.trim() else com.pixelwater.app.BuildConfig.GEMINI_API_KEY.trim()
                if (apiKey == "MY_GEMINI_API_KEY" || apiKey.isBlank()) {
                    throw Exception("Gemini API Key is unconfigured.")
                }
                
                val systemPrompt = """
                    You are Pixel Water Settings Assistant and AI Coach.
                    The user is asking you for a setting modification or to locate a setting.
                    Your task is two-fold:
                    1. Formulate a short, warm, and helpful explanation/response directly to the user (supporting English or Greek depends on their instruction; prefer ${if (currentLanguage == "el") "Greek" else "English"}). Explain what change you can make or where the setting is.
                    2. Output a strictly structured change block under [ACTIONS] ... [END_ACTIONS] specifying the actual setting ID and value you propose to change, or navigation action, if applicable.
                    
                    Available Settings:
                    - Theme Mode: setting ID is 'theme_mode', value can be 'LIGHT', 'DARK', 'SYSTEM'
                    - App Art Theme Palette: setting ID is 'app_theme', value can be 'DYNAMIC', 'OUTRUN', 'RETRO', 'FOREST', 'COFFEE', 'OCEAN', 'CRIMSON', 'COBALT'
                    - Daily Goal: setting ID is 'daily_goal_ml', value must be an Integer (ml) e.g. '2000'
                    - OLED Mode: setting ID is 'oled_mode', value can be 'true' or 'false'
                    - Frosted Glass Effect: setting ID is 'frosted_glass_enabled', value can be 'true' or 'false'
                    - General Corner Radius: setting ID is 'general_corner_radius', value must be an Integer e.g. '16' or '24'
                    - Reminders Alert: setting ID is 'reminders_enabled', value can be 'true' or 'false'
                    - Reminders Interval: setting ID is 'reminder_interval', value must be an Integer in hours e.g. '1', '2', '3'
                    - Quick Add Amount: setting ID is 'quick_add_amount', value must be an Integer in ml e.g. '250', '500'
                    - App Language: setting ID is 'app_language', value can be 'en' or 'el'
                    - Navigate/Show Section on screen: setting ID is 'navigate', value can be 'appearance', 'haptics', 'widget_settings', 'ai_integration', 'contact_me', 'apk_updates', 'developer_options'
                    
                    Format your response STRICTLY as:
                    <Explanation and friendly description here>
                    ---
                    [ACTIONS]
                    setting:<setting_id>;value:<value>
                    [END_ACTIONS]
                    
                    Rules:
                    1. If the user asks where a setting is (e.g. "Where is the widget setting?"), provide instructions and put: setting:navigate;value:widget_settings inside actions.
                    2. If they describe how they want the app to look or behave (e.g., "Make my app dark and use Forest theme"), include the relevant setting action(s), e.g. setting:theme_mode;value:DARK and setting:app_theme;value:FOREST under actions.
                    3. Do not include setting actions if the request is not related to settings.
                    4. Always suggest changes but remember that the app UI will ask for permission before applying them (do not mention the permission UI in actions, only in text if desired). Keep explanations direct and positive.
                """.trimIndent()

                val request = com.pixelwater.app.data.GeminiRequest(
                    contents = listOf(
                        com.pixelwater.app.data.Content(parts = listOf(com.pixelwater.app.data.Part(text = userQuery)))
                    ),
                    systemInstruction = com.pixelwater.app.data.Content(parts = listOf(com.pixelwater.app.data.Part(text = systemPrompt))),
                    generationConfig = com.pixelwater.app.data.GenerationConfig(temperature = 0.5f, maxOutputTokens = 1000)
                )

                val response = safeGenerateContent(
                    model = _geminiModel.value,
                    apiKey = apiKey,
                    request = request
                )

                val replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (replyText != null) {
                    val splitParts = replyText.split("---")
                    val explanation = splitParts.first().trim()
                    
                    val actions = mutableListOf<Pair<String, String>>()
                    if (replyText.contains("[ACTIONS]")) {
                        val actionBlock = replyText.substringAfter("[ACTIONS]").substringBefore("[END_ACTIONS]").trim()
                        actionBlock.lines().forEach { line ->
                            if (line.isNotBlank() && line.contains(";")) {
                                val cleanLine = line.trim()
                                val idPart = cleanLine.substringBefore(";").substringAfter("setting:").trim()
                                val valPart = cleanLine.substringAfter(";").substringAfter("value:").trim()
                                if (idPart.isNotEmpty() && valPart.isNotEmpty()) {
                                    actions.add(Pair(idPart, valPart))
                                }
                            }
                        }
                    }
                    onSuccess(explanation, actions)
                } else {
                    throw Exception("Empty response from AI")
                }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed querying settings AI Coach.")
            }
        }
    }

    fun refreshLocationAndWeather() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val context = getApplication<android.app.Application>()
                var lat: Double? = null
                var lon: Double? = null
                var city: String? = null

                val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
                    context, 
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
                    context, 
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                if (hasCoarse || hasFine) {
                    val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as? android.location.LocationManager
                    if (locationManager != null) {
                        val providers = locationManager.getProviders(true)
                        var bestLocation: android.location.Location? = null
                        for (provider in providers) {
                            val l = locationManager.getLastKnownLocation(provider) ?: continue
                            if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                                bestLocation = l
                            }
                        }
                        if (bestLocation != null) {
                            lat = bestLocation.latitude
                            lon = bestLocation.longitude
                            
                            // High-Fidelity Reverse Geocode coordinates to look up physical city name
                            try {
                                if (android.location.Geocoder.isPresent()) {
                                    val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                                    val addresses = geocoder.getFromLocation(lat, lon, 1)
                                    if (!addresses.isNullOrEmpty()) {
                                        city = addresses[0].locality ?: addresses[0].subAdminArea ?: addresses[0].adminArea
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("WaterViewModel", "Geocoder failed: ${e.message}")
                            }
                        }
                    }
                }

                // Try IP geolocation if GPS/Network system location is not found
                if (lat == null || lon == null) {
                    try {
                        val client = okhttp3.OkHttpClient()
                        val req = okhttp3.Request.Builder().url("https://ipapi.co/json/").build()
                        client.newCall(req).execute().use { resp ->
                            if (resp.isSuccessful) {
                                val body = resp.body?.string()
                                if (body != null) {
                                    val obj = org.json.JSONObject(body)
                                    lat = obj.optDouble("latitude")
                                    lon = obj.optDouble("longitude")
                                    city = obj.optString("city")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("WaterViewModel", "ipapi.co failed, trying ip-api.com: ${e.message}")
                        try {
                            val client = okhttp3.OkHttpClient()
                            val req = okhttp3.Request.Builder().url("http://ip-api.com/json/").build()
                            client.newCall(req).execute().use { resp ->
                                if (resp.isSuccessful) {
                                    val body = resp.body?.string()
                                    if (body != null) {
                                        val obj = org.json.JSONObject(body)
                                        if (obj.optString("status") == "success") {
                                            lat = obj.optDouble("lat")
                                            lon = obj.optDouble("lon")
                                            city = obj.optString("city")
                                        }
                                    }
                                }
                            }
                        } catch (e2: Exception) {
                            Log.e("WaterViewModel", "IP Geolocation fallbacks failed: ${e2.message}")
                        }
                    }
                }

                // Extract city from local timezone name fallback if geolocation city has not been resolved
                if (city.isNullOrBlank()) {
                    val tz = java.util.TimeZone.getDefault().id
                    if (tz.contains("/")) {
                        val parts = tz.split("/")
                        city = parts[parts.size - 1].replace("_", " ")
                    }
                }

                // Hard fallback if completely offline or missing coordinates
                if (lat == null || lon == null) {
                    lat = 37.9838 // Athens
                    lon = 23.7275
                    if (city.isNullOrBlank()) {
                        city = "Athens"
                    }
                }

                _approxLocation.value = Pair(lat!!, lon!!)
                val finalCity = city ?: "Athens"
                _locationCity.value = finalCity
                prefs.edit().putString("weather_city", finalCity).apply()
                prefs.edit().putFloat("weather_latitude", lat!!.toFloat()).apply()
                prefs.edit().putFloat("weather_longitude", lon!!.toFloat()).apply()

                // Query Open-Meteo for live localized current weather, daily sunrise/sunset, and timezone
                val weatherClient = okhttp3.OkHttpClient()
                val weatherUrl = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code&daily=sunrise,sunset&timezone=auto"
                val weatherReq = okhttp3.Request.Builder().url(weatherUrl).build()
                weatherClient.newCall(weatherReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string()
                        if (body != null) {
                            val json = org.json.JSONObject(body)
                            
                            val tz = json.optString("timezone")
                            if (!tz.isNullOrBlank()) {
                                prefs.edit().putString("weather_timezone", tz).apply()
                            }
                            
                            val daily = json.optJSONObject("daily")
                            if (daily != null) {
                                val sunsetArray = daily.optJSONArray("sunset")
                                val sunriseArray = daily.optJSONArray("sunrise")
                                val dateStr = getCurrentDateString()
                                if (sunsetArray != null && sunsetArray.length() > 0) {
                                    val sunsetStr = sunsetArray.getString(0) // dynamic formatted e.g. "2026-06-15T20:49"
                                    prefs.edit().putString("weather_sunset_$dateStr", sunsetStr).apply()
                                }
                                if (sunriseArray != null && sunriseArray.length() > 0) {
                                    val sunriseStr = sunriseArray.getString(0) // dynamic formatted e.g. "2026-06-15T06:03"
                                    prefs.edit().putString("weather_sunrise_$dateStr", sunriseStr).apply()
                                }
                            }

                            val currentObj = json.getJSONObject("current")
                            val temp = currentObj.getDouble("temperature_2m")
                            _weatherTemperature.value = temp
                            prefs.edit().putFloat("weather_temp", temp.toFloat()).apply()

                            val humidityObj = currentObj.optDouble("relative_humidity_2m", -1.0)
                            val apparentTempObj = currentObj.optDouble("apparent_temperature", -1.0)

                            if (humidityObj >= 0.0) {
                                _weatherRelativeHumidity.value = humidityObj
                                prefs.edit().putFloat("weather_humidity", humidityObj.toFloat()).apply()
                            } else {
                                _weatherRelativeHumidity.value = null
                                prefs.edit().remove("weather_humidity").apply()
                            }

                            if (apparentTempObj != -100.0 && apparentTempObj != -1.0) {
                                _weatherApparentTemperature.value = apparentTempObj
                                prefs.edit().putFloat("weather_apparent_temp", apparentTempObj.toFloat()).apply()
                            } else {
                                _weatherApparentTemperature.value = null
                                prefs.edit().remove("weather_apparent_temp").apply()
                            }

                            val weathercode = currentObj.optInt("weather_code", -1)
                            val raining = (weathercode in 51..67) || (weathercode in 80..82) || weathercode == 95 || weathercode == 96 || weathercode == 99
                            _isRaining.value = raining
                            prefs.edit().putBoolean("weather_is_raining", raining).apply()

                            val effectiveTemp = if (apparentTempObj != -1.0) apparentTempObj else temp
                            // Scientifically, Caution heat level begins when Heat Index / Apparent temp >= 27.0
                            // Let's trigger the heat dynamic goal boost warning when effective temp >= 32.0 (Extreme Caution threshold) Or dry temperature >= 35.0
                            val isHeatNow = effectiveTemp >= 32.0 || temp >= 35.0
                            _isExtremeHeat.value = isHeatNow
                            prefs.edit().putBoolean("weather_extreme_heat", isHeatNow).apply()

                            if (isHeatNow) {
                                checkoutExtremeHeatProposal(
                                    temp = temp,
                                    city = finalCity,
                                    isSimulation = false,
                                    humidity = humidityObj,
                                    apparentTemp = apparentTempObj
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("WaterViewModel", "Failed to retrieve location weather: ${e.message}")
            }
        }
    }

    fun checkoutExtremeHeatProposal(
        temp: Double,
        city: String,
        isSimulation: Boolean = false,
        humidity: Double = -1.0,
        apparentTemp: Double = -1.0
    ) {
        viewModelScope.launch {
            val dateStr = getCurrentDateString()
            // Avoid duplicates, unless simulating
            if (!isSimulation) {
                if (prefs.getInt("heat_bonus_$dateStr", 0) > 0 || prefs.getBoolean("dismissed_heat_$dateStr", false)) {
                    return@launch
                }
            } else {
                prefs.edit().putBoolean("dismissed_heat_$dateStr", false).apply()
            }

            try {
                val apiKey = getEffectiveGeminiApiKey()
                val weightKg = if (_setupWeight.value > 10f) _setupWeight.value else 75f

                val isHumValid = humidity >= 0.0
                val isAppValid = apparentTemp > -10.0
                val displayHum = if (isHumValid) humidity else 45.0
                val displayApp = if (isAppValid) apparentTemp else (temp + 2.0)

                val dangerLevel = when {
                    displayApp >= 51.0 -> "Extreme Danger"
                    displayApp >= 39.0 -> "Danger"
                    displayApp >= 32.0 -> "Extreme Caution"
                    displayApp >= 27.0 -> "Caution"
                    else -> "Normal"
                }

                if (apiKey != null) {
                    var prompt = """
                        Act as "Pixel Water AI Coach", an expert clinical hydration scholar.
                        The user is currently experiencing high/extreme ambient heat wave conditions:
                        - Location: $city
                        - Outdoor Dry-Bulb Temperature: ${String.format("%.1f", temp)}°C (${String.format("%.1f", temp * 1.8 + 32)}°F)
                        - Relative Humidity: ${String.format("%.1f", displayHum)}%
                        - Apparent Temperature (Feels Like): ${String.format("%.1f", displayApp)}°C (${String.format("%.1f", displayApp * 1.8 + 32)}°F)
                        - Scientific Heat Index Severity Level: $dangerLevel
                        - User Body Weight: $weightKg kg
                        
                        Please deliver an extremely precise, professional, and clinical-grade response:
                        1. Explain the scientific body fluid dynamics (including sweat rate, trans-epidermal moisture loss, plasma volume contraction, increased blood viscosity, cardiac output compensation, and how high humidity diminishes evaporative heat dissipation by skin respiration) specific to their weight of $weightKg kg.
                        2. Recommend a targeted water intake increase in milliliters (ml) to maintain plasma volume and kidney filtration rate. This should range between 6ml to 14ml per kg of body weight depending on severity (e.g. Caution: 6-8ml/kg, Extreme Caution: 8-10ml/kg, Danger: 11-13ml/kg, Extreme Danger: 13-15ml/kg).
                        3. Response MUST be in clean JSON format containing EXACTLY:
                           "additionalMl" (integer) and "rationale" (string). 
                        Response must contain only the valid JSON, No markdown blocks except optionally ```json ... ```.
                    """.trimIndent()

                    prompt += getMbtiToneInstruction(requireScientificPracticality = true)
                    prompt += getBigFiveToneInstruction(requireScientificPracticality = true)

                    if (appLanguage.value == "el") {
                        prompt += "\nSPECIAL INSTRUCTION: The value for the \"rationale\" field in the JSON response MUST be written in Greek language. Explain terms like plasma depletion (απώλεια πλάσματος) and evaporative dissipation (εξατμιστική απαγωγή θερμότητας) accurately."
                    }

                    val request = com.pixelwater.app.data.GeminiRequest(
                        contents = listOf(
                            com.pixelwater.app.data.Content(
                                parts = listOf(com.pixelwater.app.data.Part(text = prompt))
                            )
                        )
                    )

                    val response = safeGenerateContent(_geminiModel.value, apiKey, request)
                    val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (reply != null) {
                        val parsed = parseWorkoutAiResponse(reply) // reuses parser since schema is identical
                        proposeHeatWaterBonus(
                            temp = temp,
                            bonusMl = parsed.first,
                            rationaleText = parsed.second,
                            city = city,
                            humidity = if (isHumValid) humidity else null,
                            apparentTemp = if (isAppValid) apparentTemp else null,
                            dangerLevel = dangerLevel
                        )
                    } else {
                        proposeOfflineHeatFallback(temp, city, humidity, apparentTemp)
                    }
                } else {
                    proposeOfflineHeatFallback(temp, city, humidity, apparentTemp)
                }
            } catch (e: Throwable) {
                Log.e("WaterViewModel", "Extreme heat AI generation failed: ${e.message}")
                proposeOfflineHeatFallback(temp, city, humidity, apparentTemp)
            }
        }
    }

    private fun proposeOfflineHeatFallback(
        temp: Double,
        city: String,
        humidity: Double = -1.0,
        apparentTemp: Double = -1.0
    ) {
        val userWeight = if (_setupWeight.value > 10f) _setupWeight.value else 75f
        val calculatedAppTemp = if (apparentTemp > -10.0) apparentTemp else (temp + 2.0)
        
        val mlPerKg = when {
            calculatedAppTemp >= 51.0 -> 14.0f
            calculatedAppTemp >= 39.0 -> 11.5f
            calculatedAppTemp >= 32.0 -> 8.5f
            calculatedAppTemp >= 27.0 -> 6.5f
            else -> 6.0f
        }
        
        val calculatedBonus = (userWeight * mlPerKg).toInt()
        val bonusMl = (((calculatedBonus + 25) / 50) * 50).coerceIn(300, 1500)

        val severityLevel = when {
            calculatedAppTemp >= 51.0 -> "Extreme Danger"
            calculatedAppTemp >= 39.0 -> "Danger"
            calculatedAppTemp >= 32.0 -> "Extreme Caution"
            calculatedAppTemp >= 27.0 -> "Caution"
            else -> "Normal"
        }

        val explain = if (appLanguage.value == "el") {
            val levelGr = when (severityLevel) {
                "Extreme Danger" -> "Ακραίος Κίνδυνος"
                "Danger" -> "Κίνδυνος"
                "Extreme Caution" -> "Αυξημένη Προσοχή"
                "Caution" -> "Προσοχή"
                else -> "Φυσιολογικό"
            }
            "Λόγω υψηλών θερμοκρασιών (${String.format("%.1f", temp)}°C, επίπεδο: $levelGr), ο ρυθμός εφίδρωσης και η απώλεια πλάσματος αυξάνονται δραστικά. Για το σωματικό σας βάρος ($userWeight kg), ο AI Hydration Coach συνιστά αύξηση κατά +${bonusMl}ml για την αποφυγή καρδιαγγειακής καταπόνησης."
        } else {
            "An ambient temperature of ${String.format("%.1f", temp)}°C with $severityLevel severity level triggers highly elevated sweating rates and plasma volume contraction. For your body weight of $userWeight kg, professional clinical guidelines suggest adding +${bonusMl}ml of clean water to maintain fluid homeostasis."
        }
        proposeHeatWaterBonus(
            temp = temp,
            bonusMl = bonusMl,
            rationaleText = explain,
            city = city,
            humidity = if (humidity >= 0.0) humidity else null,
            apparentTemp = if (apparentTemp > -10.0) apparentTemp else null,
            dangerLevel = severityLevel
        )
    }

    private fun proposeHeatWaterBonus(
        temp: Double,
        bonusMl: Int,
        rationaleText: String,
        city: String,
        humidity: Double? = null,
        apparentTemp: Double? = null,
        dangerLevel: String = "Normal"
    ) {
        val proposal = HeatProposal(
            temperature = temp,
            bonusMl = bonusMl,
            rationale = rationaleText,
            city = city,
            relativeHumidity = humidity,
            apparentTemperature = apparentTemp,
            heatDangerLevel = dangerLevel
        )
        _pendingHeatProposal.value = proposal

        // Trigger a custom push notification
        val context = getApplication<android.app.Application>()
        val title = if (appLanguage.value == "el") "⚠️ Προειδοποίηση Καύσωνα!" else "⚠️ High Temperature Alert!"
        val content = if (appLanguage.value == "el") {
            "Θερμοκρασία $temp°C στο $city. Ο AI Coach προτείνει +${bonusMl}ml."
        } else {
            "The temperature is ${String.format("%.1f", temp)}°C in $city. Coach advises adding +${bonusMl}ml water."
        }
        com.pixelwater.app.notifications.NotificationHelper.showCustomNotification(context, title, content)
    }

    fun acceptHeatWaterBonus(proposal: HeatProposal) {
        val todayStr = getCurrentDateString()
        _todayHeatWaterBonus.value = proposal.bonusMl
        _todayHeatAiCoachResponse.value = proposal.rationale
        prefs.edit().putInt("heat_bonus_$todayStr", proposal.bonusMl).apply()
        prefs.edit().putString("heat_coach_$todayStr", proposal.rationale).apply()
        _pendingHeatProposal.value = null
        calculateStreak()
    }

    fun declineHeatWaterBonus() {
        val todayStr = getCurrentDateString()
        prefs.edit().putBoolean("dismissed_heat_$todayStr", true).apply()
        _pendingHeatProposal.value = null
    }

    fun clearTodayHeatAdjustment() {
        val todayStr = getCurrentDateString()
        _todayHeatWaterBonus.value = 0
        _todayHeatAiCoachResponse.value = ""
        prefs.edit().remove("heat_bonus_$todayStr").apply()
        prefs.edit().remove("heat_coach_$todayStr").apply()
        calculateStreak()
    }

    fun updateHeatWaterAdjustmentEnabled(enabled: Boolean) {
        _heatWaterAdjustmentEnabled.value = enabled
        prefs.edit().putBoolean("heat_water_adjustment_enabled", enabled).apply()
        if (!enabled) {
            clearTodayHeatAdjustment()
        } else {
            refreshLocationAndWeather()
        }
    }
}

data class HeatProposal(
    val temperature: Double,
    val bonusMl: Int,
    val rationale: String,
    val city: String,
    val relativeHumidity: Double? = null,
    val apparentTemperature: Double? = null,
    val heatDangerLevel: String = "Normal"
)

data class CustomDrink(
    val name: String,
    val factor: Float,
    val iconName: String
)

data class WorkoutProposal(
    val workoutType: String,
    val durationMinutes: Int,
    val bonusMl: Int,
    val rationale: String,
    val recoveryTips: String
)

data class SleepProposal(
    val sleepHours: Double,
    val bonusMl: Int,
    val rationale: String
)

data class CustomTheme(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val prompt: String = "",
    val isStrict: Boolean = false,
    val dateCreated: String = "",
    val themeMode: String = "Dark",
    val oledMode: Boolean = false,
    val boxBgOledEnabled: Boolean = false,
    val appTheme: String = "STATIC",
    val staticThemeSeed: Int = 0xFF1D5AAB.toInt(),
    val boxBgSource: String = "THEME",
    val boxBgPaletteChoice: Int = 0,
    val shapeMonochromeEnabled: Boolean = false,
    val shapeMonochromeSource: Int = 0,
    val shapeUseIndependentDynamicPalette: Boolean = false,
    val progressCircleColorSource: String = "THEME",
    val progressCircleThemeColor: Int = 0,
    val progressCircleStandardColorIndex: Int = 0,
    val materialShapesRotationSpeed: Float = 1.0f,
    val auraGlowRotationSpeed: Float = 1.0f,
    val shapeRotMultA: Float = 1.0f,
    val shapeRotMultB: Float = -1.0f,
    val shapeRotMultC: Float = 1.0f,
    val shapeRotMultD: Float = -1.0f
)
