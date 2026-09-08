package com.pixelwater.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import kotlinx.coroutines.isActive
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.random.Random
import com.google.android.gms.wearable.*
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity(), DataClient.OnDataChangedListener {
    private val TAG = "WearMainActivity"
    
    // Synced reactive states
    private val _totalIntake = mutableStateOf(0)
    private val _goal = mutableStateOf(2000)
    private val _quickAdd = mutableStateOf(250)
    private val _showRemainingButton = mutableStateOf(true)
    private val _showRemaining = mutableStateOf(false)
    private val _circleSize = mutableStateOf(-1)
    private val _themeColorResolved = mutableStateOf(0xFF00BFA5.toInt())
    private val _longPressDeleteEnabled = mutableStateOf(true)
    private val _longPressDeleteDuration = mutableStateOf(1000)
    private val _progressCircleEnabled = mutableStateOf(true)
    private val _textColorResolved = mutableStateOf(0xFF00BFA5.toInt())
    private val _wearInsideCircleRemoved = mutableStateOf(false)
    private val _progressCircleThickness = mutableStateOf("THIN")
    private val _customThickness = mutableStateOf(13f)
    private val _wearGraphPillarThickness = mutableStateOf(24f)
    private val _wearGraphHorizontalPadding = mutableStateOf(18f)
    private val _customTextSize = mutableStateOf(32f)
    private val _wearShowTomorrowDay = mutableStateOf(true)
    private val _crownRotationEnabled = mutableStateOf(true)
    private val _crownRotationReverse = mutableStateOf(false)
    private val _crownClicksPerMl = mutableStateOf(10)
    private val _crownSyncDelay = mutableStateOf(250)
    private val _devOverlayEnabled = mutableStateOf(false)
    private val _devOverlayR = mutableStateOf(0)
    private val _devOverlayG = mutableStateOf(255)
    private val _devOverlayB = mutableStateOf(0)
    private val _appLanguage = mutableStateOf("en")
    private val _goalLineSquiggly = mutableStateOf(false)
    private val _ramOptimizationEnabled = mutableStateOf(false)
    private val _swipeMode = mutableStateOf("VERTICAL")
    private val _graphSwipeDir = mutableStateOf("LEFT")
    private val _wearHapticStrength = mutableStateOf("MEDIUM")

    private var deleteSyncJob: kotlinx.coroutines.Job? = null
    private var pendingDeleteCount = 0
    private var pendingDeleteOffset = 0

    // Daily offsets synced states
    private val _intakeMinus3 = mutableStateOf(0)
    private val _intakeMinus2 = mutableStateOf(0)
    private val _intakeMinus1 = mutableStateOf(0)
    private val _intakePlus1 = mutableStateOf(0)
    private val _intakePlus2 = mutableStateOf(0)
    private val _goalMinus3 = mutableStateOf(2000)
    private val _goalMinus2 = mutableStateOf(2000)
    private val _goalMinus1 = mutableStateOf(2000)
    private val _goalPlus1 = mutableStateOf(2000)
    private val _goalPlus2 = mutableStateOf(2000)
    private val _pastIntakesCsv = mutableStateOf("")
    private val _pastGoalsCsv = mutableStateOf("")
    private val _daysPrior = mutableStateOf(5)
    private val _pastDaysToShow = mutableStateOf(3)

    private val GRAPH_SYNC_INTERVAL_MS = 3600000L // 1 hour
    private var graphSyncCheckJob: kotlinx.coroutines.Job? = null

    private fun shouldSyncGraph(): Boolean {
        val wearPrefs = getSharedPreferences("wear_prefs", MODE_PRIVATE)
        val lastSync = wearPrefs.getLong("last_graph_sync_time", 0L)
        val now = System.currentTimeMillis()
        return (lastSync == 0L || (now - lastSync) >= GRAPH_SYNC_INTERVAL_MS)
    }

    private fun requestGraphSyncFromPhone() {
        try {
            Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
                for (node in nodes) {
                    Wearable.getMessageClient(this).sendMessage(node.id, "/request_graph_sync", ByteArray(0))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting graph sync: ${e.message}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        loadLocalPrefs()
        
        loadInitialData()

        setContent {
            val resolvedColorVal by _themeColorResolved
            WearAppTheme(primaryColor = Color(resolvedColorVal)) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    WearWaterTrackerScreen(
                        totalIntakeState = _totalIntake,
                        goalState = _goal,
                        quickAddState = _quickAdd,
                        showRemainingButtonState = _showRemainingButton,
                        showRemainingState = _showRemaining,
                        circleSizeState = _circleSize,
                        themeColorResolvedState = _themeColorResolved,
                        wearHapticStrengthState = _wearHapticStrength,
                        longPressDeleteEnabledState = _longPressDeleteEnabled,
                        longPressDeleteDurationState = _longPressDeleteDuration,
                        progressCircleEnabledState = _progressCircleEnabled,
                        textColorResolvedState = _textColorResolved,
                        wearInsideCircleRemovedState = _wearInsideCircleRemoved,
                        progressCircleThicknessState = _progressCircleThickness,
                        customThicknessState = _customThickness,
                        wearGraphPillarThicknessState = _wearGraphPillarThickness,
                        wearGraphHorizontalPaddingState = _wearGraphHorizontalPadding,
                        customTextSizeState = _customTextSize,
                        wearShowTomorrowDayState = _wearShowTomorrowDay,
                        ramOptimizationEnabledState = _ramOptimizationEnabled,
                        crownRotationEnabledState = _crownRotationEnabled,
                        crownRotationReverseState = _crownRotationReverse,
                        crownClicksPerMlState = _crownClicksPerMl,
                        crownSyncDelayState = _crownSyncDelay,
                        devOverlayEnabledState = _devOverlayEnabled,
                        devOverlayRState = _devOverlayR,
                        devOverlayGState = _devOverlayG,
                        devOverlayBState = _devOverlayB,
                        intakeMinus3State = _intakeMinus3,
                        intakeMinus2State = _intakeMinus2,
                        intakeMinus1State = _intakeMinus1,
                        intakePlus1State = _intakePlus1,
                        intakePlus2State = _intakePlus2,
                        goalMinus3State = _goalMinus3,
                        goalMinus2State = _goalMinus2,
                        goalMinus1State = _goalMinus1,
                        goalPlus1State = _goalPlus1,
                        goalPlus2State = _goalPlus2,
                        appLanguageState = _appLanguage,
                        goalLineSquigglyState = _goalLineSquiggly,
                        pastIntakesCsvState = _pastIntakesCsv,
                        pastGoalsCsvState = _pastGoalsCsv,
                        daysPriorState = _daysPrior,
                        pastDaysToShowState = _pastDaysToShow,
                        swipeModeState = _swipeMode,
                        graphSwipeDirState = _graphSwipeDir,
                        onQuickAddClick = { amount, offset ->
                            sendAddWaterMessage(amount, offset)
                            syncDeviceDetailsToPhone()
                        },
                        onDeleteLastEntry = { offset ->
                            sendDeleteWaterMessage(offset)
                            syncDeviceDetailsToPhone()
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Wearable.getDataClient(this).addListener(this)
        loadInitialData()
        syncDeviceDetailsToPhone()

        if (shouldSyncGraph()) {
            requestGraphSyncFromPhone()
        }

        graphSyncCheckJob?.cancel()
        graphSyncCheckJob = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            while (isActive) {
                kotlinx.coroutines.delay(60_000L)
                if (shouldSyncGraph()) {
                    requestGraphSyncFromPhone()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        graphSyncCheckJob?.cancel()
        Wearable.getDataClient(this).removeListener(this)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        try {
            for (event in dataEvents) {
                if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/water_status") {
                    try {
                        val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                        val intake = dataMap.getInt("total_intake", 0)
                        val goal = dataMap.getInt("goal", 2000)
                        val quickAdd = dataMap.getInt("quick_add", 250)
                        val showRemainingButton = false // Always false (hard deleted setting)
                        val showRemaining = dataMap.getBoolean("show_remaining", false)
                        val circleSize = dataMap.getInt("circle_size", -1)
                        val themeColorResolved = dataMap.getInt("theme_color_resolved", 0xFF00BFA5.toInt())
                        val longPressDeleteEnabled = dataMap.getBoolean("long_press_delete_enabled", true)
                        val longPressDeleteDuration = dataMap.getInt("long_press_delete_duration", 1000)
                        val progressCircleEnabled = dataMap.getBoolean("progress_circle_enabled", true)
                        val textColorResolved = dataMap.getInt("text_color_resolved", themeColorResolved)
                        val ramOptimizationEnabled = dataMap.getBoolean("ram_optimization_enabled", false)
                        val removeInsideCircle = dataMap.getBoolean("remove_inside_circle", false)
                        val progressCircleThickness = dataMap.getString("progress_circle_thickness", "THIN") ?: "THIN"
                        val customThickness = dataMap.getFloat("custom_thickness", 13f)
                        val graphPillarThickness = dataMap.getFloat("graph_pillar_thickness", 24f)
                        val graphHorizontalPadding = dataMap.getFloat("graph_horizontal_padding", 18f)
                        val customTextSize = dataMap.getFloat("custom_text_size", 32f)
                        val wearShowTomorrowDay = dataMap.getBoolean("wear_show_tomorrow_day", true)
                        val crownRotationEnabled = dataMap.getBoolean("crown_rotation_enabled", true)
                        val crownRotationReverse = dataMap.getBoolean("crown_rotation_reverse", false)
                        val crownClicksPerMl = dataMap.getInt("crown_clicks_per_ml", 10)
                        val crownSyncDelay = dataMap.getInt("crown_sync_delay", 200)
                        val devOverlayEnabled = dataMap.getBoolean("dev_overlay_enabled", false)
                        val devOverlayR = dataMap.getInt("dev_overlay_r", 0)
                        val devOverlayG = dataMap.getInt("dev_overlay_g", 255)
                        val devOverlayB = dataMap.getInt("dev_overlay_b", 0)
                        val appLang = dataMap.getString("app_language", "en") ?: "en"
                        val goalLineSquiggly = dataMap.getBoolean("goal_line_squiggly", false)
                        val hapticStrength = dataMap.getString("haptic_strength", "MEDIUM") ?: "MEDIUM"
                        
                        val wearPrefs = getSharedPreferences("wear_prefs", MODE_PRIVATE)
                        val swipeMode = dataMap.getString("swipe_mode", "VERTICAL") ?: "VERTICAL"
                        val graphSwipeDir = dataMap.getString("graph_swipe_dir", "LEFT") ?: "LEFT"
                        val isGraphSyncDue = shouldSyncGraph()
                        saveLocalPrefs(dataMap, isGraphSyncDue)
                        
                        val intakeM3 = dataMap.getInt("intake_minus_3", 0)
                        val intakeM2 = dataMap.getInt("intake_minus_2", 0)
                        val intakeM1 = dataMap.getInt("intake_minus_1", 0)
                        val intakeP1 = dataMap.getInt("intake_plus_1", 0)
                        val intakeP2 = dataMap.getInt("intake_plus_2", 0)
                        
                        val goalM3 = dataMap.getInt("goal_minus_3", 2000)
                        val goalM2 = dataMap.getInt("goal_minus_2", 2000)
                        val goalM1 = dataMap.getInt("goal_minus_1", 2000)
                        val goalP1 = dataMap.getInt("goal_plus_1", 2000)
                        val goalP2 = dataMap.getInt("goal_plus_2", 2000)
                        
                        val pastIntakesCsv = dataMap.getString("past_intakes_csv", "") ?: ""
                        val pastGoalsCsv = dataMap.getString("past_goals_csv", "") ?: ""
                        val daysPrior = if (dataMap.containsKey("days_prior")) {
                            dataMap.getInt("days_prior", 5)
                        } else {
                            dataMap.getInt("weeks_prior", 1) * 7
                        }
                        val pastDaysToShow = dataMap.getInt("past_days_to_show", 3)
                        
                        _totalIntake.value = intake
                        _goal.value = goal
                        _quickAdd.value = quickAdd
                        _showRemainingButton.value = false // Always false
                        _showRemaining.value = showRemaining
                        _circleSize.value = circleSize
                        _themeColorResolved.value = themeColorResolved
                        _longPressDeleteEnabled.value = longPressDeleteEnabled
                        _longPressDeleteDuration.value = longPressDeleteDuration
                        _progressCircleEnabled.value = progressCircleEnabled
                        _textColorResolved.value = textColorResolved
                        _ramOptimizationEnabled.value = ramOptimizationEnabled
                        _wearInsideCircleRemoved.value = removeInsideCircle
                        _progressCircleThickness.value = progressCircleThickness
                        _customThickness.value = customThickness
                        _wearGraphPillarThickness.value = graphPillarThickness
                        _wearGraphHorizontalPadding.value = graphHorizontalPadding
                        _customTextSize.value = customTextSize
                        _wearShowTomorrowDay.value = wearShowTomorrowDay
                        _crownRotationEnabled.value = crownRotationEnabled
                        _crownRotationReverse.value = crownRotationReverse
                        _crownClicksPerMl.value = crownClicksPerMl
                        _crownSyncDelay.value = crownSyncDelay
                        _devOverlayEnabled.value = devOverlayEnabled
                        _devOverlayR.value = devOverlayR
                        _devOverlayG.value = devOverlayG
                        _devOverlayB.value = devOverlayB
                        _appLanguage.value = appLang
                        _goalLineSquiggly.value = goalLineSquiggly
                        _swipeMode.value = swipeMode
                        _graphSwipeDir.value = graphSwipeDir
                        _wearHapticStrength.value = hapticStrength
                        
                        if (pastIntakesCsv.isNotEmpty()) {
                            _pastIntakesCsv.value = pastIntakesCsv
                        }
                        if (pastGoalsCsv.isNotEmpty()) {
                            _pastGoalsCsv.value = pastGoalsCsv
                        }
                        if (isGraphSyncDue) {
                            _intakeMinus3.value = intakeM3
                            _intakeMinus2.value = intakeM2
                            _intakeMinus1.value = intakeM1
                            _intakePlus1.value = intakeP1
                            _intakePlus2.value = intakeP2
                            
                            _goalMinus3.value = goalM3
                            _goalMinus2.value = goalM2
                            _goalMinus1.value = goalM1
                            _goalPlus1.value = goalP1
                            _goalPlus2.value = goalP2
        
                            _daysPrior.value = daysPrior
                            _pastDaysToShow.value = pastDaysToShow
                        }

                        try {
                            com.pixelwater.app.complication.QuickAddComplicationReceiver.updateComplicationsAndTiles(this)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error updating tiles and complications in onDataChanged: ${e.message}")
                        }
    
                        Log.d(TAG, "Data changed from phone: intake=$intake, goal=$goal, quickAdd=$quickAdd, showRemainingButton=$showRemainingButton, circleSize=$circleSize, themeColorResolved=$themeColorResolved, longPressDeleteEnabled=$longPressDeleteEnabled, longPressDeleteDuration=$longPressDeleteDuration, pastIntakes=$pastIntakesCsv")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in onDataChanged loop: ${e.message}")
                    }
                }
            }
        } finally {
            try {
                dataEvents.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing dataEvents: ${e.message}")
            }
        }
    }

    private fun syncDeviceDetailsToPhone() {
        try {
            val batteryManager = getSystemService(android.content.Context.BATTERY_SERVICE) as android.os.BatteryManager
            val batteryLevel = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val model = android.os.Build.MODEL ?: "Unknown Smartwatch"
            
            val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
            scope.launch {
                try {
                    val dataMapRequest = PutDataMapRequest.create("/watch_details").apply {
                        dataMap.putString("model", model)
                        dataMap.putInt("battery", batteryLevel)
                        dataMap.putBoolean("companion_detected", true)
                        dataMap.putLong("timestamp", System.currentTimeMillis())
                    }
                    val putDataReq = dataMapRequest.asPutDataRequest()
                    putDataReq.setUrgent()
                    Wearable.getDataClient(this@MainActivity).putDataItem(putDataReq)
                        .addOnSuccessListener {
                            Log.d(TAG, "Successfully synced watch details to phone: model=$model, battery=$batteryLevel%")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed to send watch details data item: ${e.message}")
                        }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to put watch details data item: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync device details: ${e.message}")
        }
    }

    private fun loadInitialData() {
        Wearable.getDataClient(this).getDataItems(android.net.Uri.parse("wear://*/water_status"))
            .addOnSuccessListener { dataItems ->
                try {
                    for (item in dataItems) {
                        if (item.uri.path == "/water_status") {
                            val dataMap = DataMapItem.fromDataItem(item).dataMap
                            _totalIntake.value = dataMap.getInt("total_intake", _totalIntake.value)
                            _goal.value = dataMap.getInt("goal", _goal.value)
                            _quickAdd.value = dataMap.getInt("quick_add", _quickAdd.value)
                            _showRemainingButton.value = false // Always false (hard deleted option)
                            _showRemaining.value = dataMap.getBoolean("show_remaining", _showRemaining.value)
                            _circleSize.value = dataMap.getInt("circle_size", _circleSize.value)
                            _themeColorResolved.value = dataMap.getInt("theme_color_resolved", _themeColorResolved.value)
                            _longPressDeleteEnabled.value = dataMap.getBoolean("long_press_delete_enabled", _longPressDeleteEnabled.value)
                            _longPressDeleteDuration.value = dataMap.getInt("long_press_delete_duration", _longPressDeleteDuration.value)
                            _progressCircleEnabled.value = dataMap.getBoolean("progress_circle_enabled", _progressCircleEnabled.value)
                            _textColorResolved.value = dataMap.getInt("text_color_resolved", _textColorResolved.value)
                            _ramOptimizationEnabled.value = dataMap.getBoolean("ram_optimization_enabled", _ramOptimizationEnabled.value)
                            _wearInsideCircleRemoved.value = dataMap.getBoolean("remove_inside_circle", _wearInsideCircleRemoved.value)
                            _progressCircleThickness.value = dataMap.getString("progress_circle_thickness", "THIN") ?: "THIN"
                            _customThickness.value = dataMap.getFloat("custom_thickness", _customThickness.value)
                            _wearGraphPillarThickness.value = dataMap.getFloat("graph_pillar_thickness", _wearGraphPillarThickness.value)
                            _wearGraphHorizontalPadding.value = dataMap.getFloat("graph_horizontal_padding", _wearGraphHorizontalPadding.value)
                            _customTextSize.value = dataMap.getFloat("custom_text_size", _customTextSize.value)
                            _crownRotationEnabled.value = dataMap.getBoolean("crown_rotation_enabled", _crownRotationEnabled.value)
                            _crownRotationReverse.value = dataMap.getBoolean("crown_rotation_reverse", _crownRotationReverse.value)
                            _crownClicksPerMl.value = dataMap.getInt("crown_clicks_per_ml", _crownClicksPerMl.value)
                            _crownSyncDelay.value = dataMap.getInt("crown_sync_delay", _crownSyncDelay.value)
                            _devOverlayEnabled.value = dataMap.getBoolean("dev_overlay_enabled", _devOverlayEnabled.value)
                            _devOverlayR.value = dataMap.getInt("dev_overlay_r", _devOverlayR.value)
                            _devOverlayG.value = dataMap.getInt("dev_overlay_g", _devOverlayG.value)
                            _devOverlayB.value = dataMap.getInt("dev_overlay_b", _devOverlayB.value)
                            _appLanguage.value = dataMap.getString("app_language", _appLanguage.value) ?: "en"
                            _goalLineSquiggly.value = dataMap.getBoolean("goal_line_squiggly", _goalLineSquiggly.value)
                            _swipeMode.value = dataMap.getString("swipe_mode", "VERTICAL") ?: "VERTICAL"
                            _graphSwipeDir.value = dataMap.getString("graph_swipe_dir", "LEFT") ?: "LEFT"
                            _wearHapticStrength.value = dataMap.getString("haptic_strength", _wearHapticStrength.value) ?: "MEDIUM"
                            
                            val isGraphSyncDue = shouldSyncGraph()
                            saveLocalPrefs(dataMap, isGraphSyncDue)
                            
                            val initialPastIntakes = dataMap.getString("past_intakes_csv", "") ?: ""
                            val initialPastGoals = dataMap.getString("past_goals_csv", "") ?: ""
                            if (initialPastIntakes.isNotEmpty()) {
                                _pastIntakesCsv.value = initialPastIntakes
                            }
                            if (initialPastGoals.isNotEmpty()) {
                                _pastGoalsCsv.value = initialPastGoals
                            }
                            if (isGraphSyncDue) {
                                _intakeMinus3.value = dataMap.getInt("intake_minus_3", _intakeMinus3.value)
                                _intakeMinus2.value = dataMap.getInt("intake_minus_2", _intakeMinus2.value)
                                _intakeMinus1.value = dataMap.getInt("intake_minus_1", _intakeMinus1.value)
                                _intakePlus1.value = dataMap.getInt("intake_plus_1", _intakePlus1.value)
                                _intakePlus2.value = dataMap.getInt("intake_plus_2", _intakePlus2.value)
                                
                                _goalMinus3.value = dataMap.getInt("goal_minus_3", _goalMinus3.value)
                                _goalMinus2.value = dataMap.getInt("goal_minus_2", _goalMinus2.value)
                                _goalMinus1.value = dataMap.getInt("goal_minus_1", _goalMinus1.value)
                                _goalPlus1.value = dataMap.getInt("goal_plus_1", _goalPlus1.value)
                                _goalPlus2.value = dataMap.getInt("goal_plus_2", _goalPlus2.value)
                                
                                _pastDaysToShow.value = dataMap.getInt("past_days_to_show", 3)
                                _daysPrior.value = if (dataMap.containsKey("days_prior")) {
                                    dataMap.getInt("days_prior", 5)
                                } else {
                                    dataMap.getInt("weeks_prior", _daysPrior.value / 7) * 7
                                }
                            }
 
                            Log.d(TAG, "Initial data loaded: intake=${_totalIntake.value}, goal=${_goal.value}, quickAdd=${_quickAdd.value}, showRemainingButton=${_showRemainingButton.value}, circleSize=${_circleSize.value}, themeColorResolved=${_themeColorResolved.value}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in loadInitialData success: ${e.message}")
                } finally {
                    try {
                        dataItems.release()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error releasing dataItems: ${e.message}")
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to load initial data: ${e.message}")
            }
    }

    private fun sendAddWaterMessage(amount: Int, offset: Int) {
        val actionId = java.util.UUID.randomUUID().toString()
        val payload = "$amount,$offset,$actionId".toByteArray()
        Wearable.getNodeClient(this).connectedNodes
            .addOnSuccessListener { nodes ->
                for (node in nodes) {
                    Wearable.getMessageClient(this).sendMessage(node.id, "/add-water", payload)
                        .addOnSuccessListener {
                            Log.d(TAG, "Sent add-water message to node ${node.displayName}: $amount ml offset $offset")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed to send message to node ${node.displayName}: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to get connected nodes: ${e.message}")
            }

        try {
            val dataMapRequest = PutDataMapRequest.create("/wear_action").apply {
                dataMap.putString("action", "ADD")
                dataMap.putInt("amount", amount)
                dataMap.putInt("offset", offset)
                dataMap.putLong("timestamp", System.currentTimeMillis())
                dataMap.putString("action_id", actionId)
            }
            Wearable.getDataClient(this).putDataItem(dataMapRequest.asPutDataRequest().setUrgent())
        } catch (e: Exception) {
            Log.e(TAG, "Failed putting wear_action item: ${e.message}")
        }
        com.pixelwater.app.complication.QuickAddComplicationReceiver.updateComplicationsAndTiles(this)
    }

    private fun sendDeleteWaterMessage(offset: Int) {
        synchronized(this) {
            pendingDeleteCount++
            pendingDeleteOffset = offset
        }
        deleteSyncJob?.cancel()
        deleteSyncJob = CoroutineScope(Dispatchers.IO).launch {
            delay(1000L)
            val countToSync: Int
            val offsetToSync: Int
            synchronized(this@MainActivity) {
                countToSync = pendingDeleteCount
                offsetToSync = pendingDeleteOffset
                pendingDeleteCount = 0
            }

            for (i in 0 until countToSync) {
                val actionId = java.util.UUID.randomUUID().toString()
                val payload = "$offsetToSync,$actionId".toByteArray()
                Wearable.getNodeClient(this@MainActivity).connectedNodes
                    .addOnSuccessListener { nodes ->
                        for (node in nodes) {
                            Wearable.getMessageClient(this@MainActivity).sendMessage(node.id, "/delete-water", payload)
                                .addOnSuccessListener {
                                    Log.d(TAG, "Sent delete-water message ($i) to node ${node.displayName} offset $offsetToSync")
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Failed to send delete-water message to node ${node.displayName}: ${e.message}")
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to get connected nodes for delete: ${e.message}")
                    }

                try {
                    val dataMapRequest = PutDataMapRequest.create("/wear_action").apply {
                        dataMap.putString("action", "DELETE")
                        dataMap.putInt("offset", offsetToSync)
                        dataMap.putLong("timestamp", System.currentTimeMillis() + i)
                        dataMap.putString("action_id", actionId)
                    }
                    Wearable.getDataClient(this@MainActivity).putDataItem(dataMapRequest.asPutDataRequest().setUrgent())
                } catch (e: Exception) {
                    Log.e(TAG, "Failed putting wear_action item for delete: ${e.message}")
                }
                if (countToSync > 1) {
                    delay(50L)
                }
            }
        }
    }

    private fun loadLocalPrefs() {
        val wearPrefs = getSharedPreferences("wear_prefs", MODE_PRIVATE)
        _swipeMode.value = wearPrefs.getString("swipe_mode", "VERTICAL") ?: "VERTICAL"
        _graphSwipeDir.value = wearPrefs.getString("graph_swipe_dir", "LEFT") ?: "LEFT"
        _wearHapticStrength.value = wearPrefs.getString("wear_haptic_strength", "MEDIUM") ?: "MEDIUM"

        _totalIntake.value = wearPrefs.getInt("total_intake", _totalIntake.value)
        _goal.value = wearPrefs.getInt("goal", _goal.value)
        _quickAdd.value = wearPrefs.getInt("quick_add", _quickAdd.value)
        _showRemaining.value = wearPrefs.getBoolean("show_remaining", _showRemaining.value)
        _circleSize.value = wearPrefs.getInt("circle_size", _circleSize.value)
        _themeColorResolved.value = wearPrefs.getInt("theme_color_resolved", _themeColorResolved.value)
        _longPressDeleteEnabled.value = wearPrefs.getBoolean("long_press_delete_enabled", _longPressDeleteEnabled.value)
        _longPressDeleteDuration.value = wearPrefs.getInt("long_press_delete_duration", _longPressDeleteDuration.value)
        _progressCircleEnabled.value = wearPrefs.getBoolean("progress_circle_enabled", _progressCircleEnabled.value)
        _textColorResolved.value = wearPrefs.getInt("text_color_resolved", _textColorResolved.value)
        _ramOptimizationEnabled.value = wearPrefs.getBoolean("ram_optimization_enabled", _ramOptimizationEnabled.value)
        _wearInsideCircleRemoved.value = wearPrefs.getBoolean("remove_inside_circle", _wearInsideCircleRemoved.value)
        _progressCircleThickness.value = wearPrefs.getString("progress_circle_thickness", "THIN") ?: "THIN"
        _customThickness.value = wearPrefs.getFloat("custom_thickness", _customThickness.value)
        _wearGraphPillarThickness.value = wearPrefs.getFloat("graph_pillar_thickness", _wearGraphPillarThickness.value)
        _wearGraphHorizontalPadding.value = wearPrefs.getFloat("graph_horizontal_padding", _wearGraphHorizontalPadding.value)
        _customTextSize.value = wearPrefs.getFloat("custom_text_size", _customTextSize.value)
        _wearShowTomorrowDay.value = wearPrefs.getBoolean("wear_show_tomorrow_day", _wearShowTomorrowDay.value)
        _crownRotationEnabled.value = wearPrefs.getBoolean("crown_rotation_enabled", _crownRotationEnabled.value)
        _crownRotationReverse.value = wearPrefs.getBoolean("crown_rotation_reverse", _crownRotationReverse.value)
        _crownClicksPerMl.value = wearPrefs.getInt("crown_clicks_per_ml", _crownClicksPerMl.value)
        _crownSyncDelay.value = wearPrefs.getInt("crown_sync_delay", _crownSyncDelay.value)
        _devOverlayEnabled.value = wearPrefs.getBoolean("dev_overlay_enabled", _devOverlayEnabled.value)
        _devOverlayR.value = wearPrefs.getInt("dev_overlay_r", _devOverlayR.value)
        _devOverlayG.value = wearPrefs.getInt("dev_overlay_g", _devOverlayG.value)
        _devOverlayB.value = wearPrefs.getInt("dev_overlay_b", _devOverlayB.value)
        _appLanguage.value = wearPrefs.getString("app_language", _appLanguage.value) ?: "en"
        _goalLineSquiggly.value = wearPrefs.getBoolean("goal_line_squiggly", _goalLineSquiggly.value)

        _intakeMinus3.value = wearPrefs.getInt("intake_minus_3", _intakeMinus3.value)
        _intakeMinus2.value = wearPrefs.getInt("intake_minus_2", _intakeMinus2.value)
        _intakeMinus1.value = wearPrefs.getInt("intake_minus_1", _intakeMinus1.value)
        _intakePlus1.value = wearPrefs.getInt("intake_plus_1", _intakePlus1.value)
        _intakePlus2.value = wearPrefs.getInt("intake_plus_2", _intakePlus2.value)

        _goalMinus3.value = wearPrefs.getInt("goal_minus_3", _goalMinus3.value)
        _goalMinus2.value = wearPrefs.getInt("goal_minus_2", _goalMinus2.value)
        _goalMinus1.value = wearPrefs.getInt("goal_minus_1", _goalMinus1.value)
        _goalPlus1.value = wearPrefs.getInt("goal_plus_1", _goalPlus1.value)
        _goalPlus2.value = wearPrefs.getInt("goal_plus_2", _goalPlus2.value)

        _pastIntakesCsv.value = wearPrefs.getString("past_intakes_csv", _pastIntakesCsv.value) ?: ""
        _pastGoalsCsv.value = wearPrefs.getString("past_goals_csv", _pastGoalsCsv.value) ?: ""
        _daysPrior.value = wearPrefs.getInt("days_prior", _daysPrior.value)
        _pastDaysToShow.value = wearPrefs.getInt("past_days_to_show", _pastDaysToShow.value)
    }

    private fun saveLocalPrefs(dataMap: DataMap, syncGraph: Boolean = false) {
        val wearPrefs = getSharedPreferences("wear_prefs", MODE_PRIVATE)
        val editor = wearPrefs.edit()

        if (dataMap.containsKey("swipe_mode")) editor.putString("swipe_mode", dataMap.getString("swipe_mode"))
        if (dataMap.containsKey("graph_swipe_dir")) editor.putString("graph_swipe_dir", dataMap.getString("graph_swipe_dir"))
        if (dataMap.containsKey("complication_icon_style")) editor.putString("complication_icon_style", dataMap.getString("complication_icon_style"))
        if (dataMap.containsKey("haptic_strength")) editor.putString("wear_haptic_strength", dataMap.getString("haptic_strength"))
        if (dataMap.containsKey("total_intake")) editor.putInt("total_intake", dataMap.getInt("total_intake"))
        if (dataMap.containsKey("goal")) editor.putInt("goal", dataMap.getInt("goal"))
        if (dataMap.containsKey("quick_add")) editor.putInt("quick_add", dataMap.getInt("quick_add"))
        if (dataMap.containsKey("show_remaining")) editor.putBoolean("show_remaining", dataMap.getBoolean("show_remaining"))
        if (dataMap.containsKey("circle_size")) editor.putInt("circle_size", dataMap.getInt("circle_size"))
        if (dataMap.containsKey("theme_color_resolved")) editor.putInt("theme_color_resolved", dataMap.getInt("theme_color_resolved"))
        if (dataMap.containsKey("long_press_delete_enabled")) editor.putBoolean("long_press_delete_enabled", dataMap.getBoolean("long_press_delete_enabled"))
        if (dataMap.containsKey("long_press_delete_duration")) editor.putInt("long_press_delete_duration", dataMap.getInt("long_press_delete_duration"))
        if (dataMap.containsKey("progress_circle_enabled")) editor.putBoolean("progress_circle_enabled", dataMap.getBoolean("progress_circle_enabled"))
        if (dataMap.containsKey("text_color_resolved")) editor.putInt("text_color_resolved", dataMap.getInt("text_color_resolved"))
        if (dataMap.containsKey("ram_optimization_enabled")) editor.putBoolean("ram_optimization_enabled", dataMap.getBoolean("ram_optimization_enabled"))
        if (dataMap.containsKey("remove_inside_circle")) editor.putBoolean("remove_inside_circle", dataMap.getBoolean("remove_inside_circle"))
        if (dataMap.containsKey("progress_circle_thickness")) editor.putString("progress_circle_thickness", dataMap.getString("progress_circle_thickness"))
        if (dataMap.containsKey("custom_thickness")) editor.putFloat("custom_thickness", dataMap.getFloat("custom_thickness"))
        if (dataMap.containsKey("graph_pillar_thickness")) editor.putFloat("graph_pillar_thickness", dataMap.getFloat("graph_pillar_thickness"))
        if (dataMap.containsKey("graph_horizontal_padding")) editor.putFloat("graph_horizontal_padding", dataMap.getFloat("graph_horizontal_padding"))
        if (dataMap.containsKey("custom_text_size")) editor.putFloat("custom_text_size", dataMap.getFloat("custom_text_size"))
        if (dataMap.containsKey("wear_show_tomorrow_day")) editor.putBoolean("wear_show_tomorrow_day", dataMap.getBoolean("wear_show_tomorrow_day"))
        if (dataMap.containsKey("crown_rotation_enabled")) editor.putBoolean("crown_rotation_enabled", dataMap.getBoolean("crown_rotation_enabled"))
        if (dataMap.containsKey("crown_rotation_reverse")) editor.putBoolean("crown_rotation_reverse", dataMap.getBoolean("crown_rotation_reverse"))
        if (dataMap.containsKey("crown_clicks_per_ml")) editor.putInt("crown_clicks_per_ml", dataMap.getInt("crown_clicks_per_ml"))
        if (dataMap.containsKey("crown_sync_delay")) editor.putInt("crown_sync_delay", dataMap.getInt("crown_sync_delay"))
        if (dataMap.containsKey("dev_overlay_enabled")) editor.putBoolean("dev_overlay_enabled", dataMap.getBoolean("dev_overlay_enabled"))
        if (dataMap.containsKey("dev_overlay_r")) editor.putInt("dev_overlay_r", dataMap.getInt("dev_overlay_r"))
        if (dataMap.containsKey("dev_overlay_g")) editor.putInt("dev_overlay_g", dataMap.getInt("dev_overlay_g"))
        if (dataMap.containsKey("dev_overlay_b")) editor.putInt("dev_overlay_b", dataMap.getInt("dev_overlay_b"))
        if (dataMap.containsKey("app_language")) editor.putString("app_language", dataMap.getString("app_language"))
        if (dataMap.containsKey("goal_line_squiggly")) editor.putBoolean("goal_line_squiggly", dataMap.getBoolean("goal_line_squiggly"))

        if (dataMap.containsKey("past_intakes_csv")) {
            val pIntakes = dataMap.getString("past_intakes_csv") ?: ""
            if (pIntakes.isNotEmpty()) editor.putString("past_intakes_csv", pIntakes)
        }
        if (dataMap.containsKey("past_goals_csv")) {
            val pGoals = dataMap.getString("past_goals_csv") ?: ""
            if (pGoals.isNotEmpty()) editor.putString("past_goals_csv", pGoals)
        }
        if (syncGraph) {
            if (dataMap.containsKey("intake_minus_3")) editor.putInt("intake_minus_3", dataMap.getInt("intake_minus_3"))
            if (dataMap.containsKey("intake_minus_2")) editor.putInt("intake_minus_2", dataMap.getInt("intake_minus_2"))
            if (dataMap.containsKey("intake_minus_1")) editor.putInt("intake_minus_1", dataMap.getInt("intake_minus_1"))
            if (dataMap.containsKey("intake_plus_1")) editor.putInt("intake_plus_1", dataMap.getInt("intake_plus_1"))
            if (dataMap.containsKey("intake_plus_2")) editor.putInt("intake_plus_2", dataMap.getInt("intake_plus_2"))

            if (dataMap.containsKey("goal_minus_3")) editor.putInt("goal_minus_3", dataMap.getInt("goal_minus_3"))
            if (dataMap.containsKey("goal_minus_2")) editor.putInt("goal_minus_2", dataMap.getInt("goal_minus_2"))
            if (dataMap.containsKey("goal_minus_1")) editor.putInt("goal_minus_1", dataMap.getInt("goal_minus_1"))
            if (dataMap.containsKey("goal_plus_1")) editor.putInt("goal_plus_1", dataMap.getInt("goal_plus_1"))
            if (dataMap.containsKey("goal_plus_2")) editor.putInt("goal_plus_2", dataMap.getInt("goal_plus_2"))

            if (dataMap.containsKey("days_prior")) editor.putInt("days_prior", dataMap.getInt("days_prior"))
            if (dataMap.containsKey("past_days_to_show")) editor.putInt("past_days_to_show", dataMap.getInt("past_days_to_show"))

            editor.putLong("last_graph_sync_time", System.currentTimeMillis())
        }

        editor.apply()
        com.pixelwater.app.complication.QuickAddComplicationReceiver.updateComplicationsAndTiles(this)
    }
}

@Composable
fun WearWaterTrackerScreen(
    totalIntakeState: State<Int>,
    goalState: State<Int>,
    quickAddState: State<Int>,
    showRemainingButtonState: State<Boolean>,
    showRemainingState: State<Boolean>,
    circleSizeState: State<Int>,
    themeColorResolvedState: State<Int>,
    longPressDeleteEnabledState: State<Boolean>,
    longPressDeleteDurationState: State<Int>,
    progressCircleEnabledState: State<Boolean>,
    textColorResolvedState: State<Int>,
    crownRotationEnabledState: State<Boolean>,
    crownRotationReverseState: State<Boolean>,
    crownClicksPerMlState: State<Int>,
    crownSyncDelayState: State<Int>,
    devOverlayEnabledState: State<Boolean>,
    devOverlayRState: State<Int>,
    devOverlayGState: State<Int>,
    devOverlayBState: State<Int>,
    wearInsideCircleRemovedState: State<Boolean>,
    progressCircleThicknessState: State<String>,
    customThicknessState: State<Float>,
    wearGraphPillarThicknessState: State<Float> = mutableStateOf(24f),
    wearGraphHorizontalPaddingState: State<Float> = mutableStateOf(18f),
    customTextSizeState: State<Float>,
    wearShowTomorrowDayState: State<Boolean> = mutableStateOf(true),
    ramOptimizationEnabledState: State<Boolean>,
    
    intakeMinus3State: State<Int>,
    intakeMinus2State: State<Int>,
    intakeMinus1State: State<Int>,
    intakePlus1State: State<Int>,
    intakePlus2State: State<Int>,
    goalMinus3State: State<Int>,
    goalMinus2State: State<Int>,
    goalMinus1State: State<Int>,
    goalPlus1State: State<Int>,
    goalPlus2State: State<Int>,
    appLanguageState: State<String>,
    goalLineSquigglyState: State<Boolean>,
    pastIntakesCsvState: State<String>,
    pastGoalsCsvState: State<String>,
    daysPriorState: State<Int>,
    pastDaysToShowState: State<Int>,
    swipeModeState: State<String>,
    graphSwipeDirState: State<String>,
    wearHapticStrengthState: State<String>,
    
    onQuickAddClick: (Int, Int) -> Unit,
    onDeleteLastEntry: (Int) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val wearHapticStrength = wearHapticStrengthState.value
    val baseHaptic = LocalHapticFeedback.current
    val haptic = remember(context, wearHapticStrength) {
        object : androidx.compose.ui.hapticfeedback.HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: androidx.compose.ui.hapticfeedback.HapticFeedbackType) {
                val type = when (hapticFeedbackType) {
                    androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove -> "ROTARY"
                    else -> "CLICK"
                }
                performWearHaptic(context, wearHapticStrength, type)
            }
        }
    }
    val appLanguage = appLanguageState.value
    val goalLineSquiggly = goalLineSquigglyState.value
    val totalIntakeMlFromPhone = totalIntakeState.value
    val goalMl = goalState.value
    val quickAddAmount = quickAddState.value
    val showRemainingButton = showRemainingButtonState.value
    val circleSize = circleSizeState.value
    val longPressEnabled = longPressDeleteEnabledState.value
    val longPressDuration = longPressDeleteDurationState.value.toLong()
    val progressCircleEnabled = progressCircleEnabledState.value
    val textColorResolved = textColorResolvedState.value
    val wearInsideCircleRemoved = wearInsideCircleRemovedState.value
    val progressCircleThickness = progressCircleThicknessState.value
    val customThickness = customThicknessState.value
    val customTextSize = customTextSizeState.value
    val wearRamOptimization = ramOptimizationEnabledState.value
    val crownRotationEnabled = crownRotationEnabledState.value
    val crownRotationReverse = crownRotationReverseState.value
    val crownClicksPerMl = crownClicksPerMlState.value
    val crownSyncDelay = crownSyncDelayState.value
    val devOverlayEnabled = devOverlayEnabledState.value
    val devOverlayR = devOverlayRState.value
    val devOverlayG = devOverlayGState.value
    val devOverlayB = devOverlayBState.value

    val wearPrefs = remember { context.getSharedPreferences("wear_prefs", android.content.Context.MODE_PRIVATE) }
    val swipeMode = swipeModeState.value
    val graphSwipeDir = graphSwipeDirState.value
    var currentLayer by remember { mutableStateOf(1) } // 1 = Main, 2 = Graph
    var currentOffset by remember { mutableStateOf(0) }

    var swipeOffsetX by remember { mutableStateOf(0f) }
    var swipeOffsetY by remember { mutableStateOf(0f) }
    var hasSnappedHaptic by remember { mutableStateOf(false) }

    var graphSwipeOffsetX by remember { mutableStateOf(0f) }
    var graphSwipeOffsetY by remember { mutableStateOf(0f) }
    var graphHasSnappedHaptic by remember { mutableStateOf(false) }

    var autoHideHeaderEnabled by remember { mutableStateOf(wearPrefs.getBoolean("auto_hide_header_enabled", false)) }
    var autoHideDurationSeconds by remember { mutableStateOf(wearPrefs.getInt("auto_hide_duration_seconds", 2)) }
    var isHeaderVisible by remember { mutableStateOf(true) }

    val headerAlpha by if (wearRamOptimization) {
        remember(isHeaderVisible) { mutableStateOf(if (isHeaderVisible) 1f else 0f) }
    } else {
        animateFloatAsState(
            targetValue = if (isHeaderVisible) 1f else 0f,
            animationSpec = tween(durationMillis = 300)
        )
    }

    LaunchedEffect(currentOffset, autoHideHeaderEnabled, autoHideDurationSeconds) {
        if (autoHideHeaderEnabled) {
            isHeaderVisible = true
            delay(autoHideDurationSeconds * 1000L)
            isHeaderVisible = false
        } else {
            isHeaderVisible = true
        }
    }

    val pastIntakesCsv = pastIntakesCsvState.value
    val pastGoalsCsv = pastGoalsCsvState.value
    val daysPrior = daysPriorState.value
    val pastDaysToShow = pastDaysToShowState.value
    val fallbackListSize = maxOf(daysPrior, pastDaysToShow) + 3

    val intakesList = remember(pastIntakesCsv, totalIntakeMlFromPhone, intakeMinus3State.value, intakeMinus2State.value, intakeMinus1State.value, intakePlus1State.value, intakePlus2State.value, daysPrior, pastDaysToShow) {
        if (pastIntakesCsv.isNotEmpty()) {
            val list = pastIntakesCsv.split(",").mapNotNull { it.toIntOrNull() }.toMutableList()
            if (daysPrior in list.indices) {
                list[daysPrior] = totalIntakeMlFromPhone
            }
            list
        } else {
            List(fallbackListSize) { idx ->
                val offset = idx - daysPrior
                when (offset) {
                    -3 -> intakeMinus3State.value
                    -2 -> intakeMinus2State.value
                    -1 -> intakeMinus1State.value
                    0 -> totalIntakeMlFromPhone
                    1 -> intakePlus1State.value
                    2 -> intakePlus2State.value
                    else -> 0
                }
            }
        }
    }

    val maxForward = remember(intakesList, daysPrior, wearShowTomorrowDayState.value) {
        if (wearShowTomorrowDayState.value) 1 else 0
    }

    val goalsList = remember(pastGoalsCsv, goalMl, goalMinus3State.value, goalMinus2State.value, goalMinus1State.value, goalPlus1State.value, goalPlus2State.value, daysPrior, pastDaysToShow) {
        if (pastGoalsCsv.isNotEmpty()) {
            val list = pastGoalsCsv.split(",").mapNotNull { it.toIntOrNull() }.toMutableList()
            if (daysPrior in list.indices) {
                list[daysPrior] = goalMl
            }
            list
        } else {
            List(fallbackListSize) { idx ->
                val offset = idx - daysPrior
                when (offset) {
                    -3 -> goalMinus3State.value
                    -2 -> goalMinus2State.value
                    -1 -> goalMinus1State.value
                    0 -> goalMl
                    1 -> goalPlus1State.value
                    2 -> goalPlus2State.value
                    else -> 2000
                }
            }
        }
    }

    val activeIntakeFromPhone = remember(intakesList, currentOffset, totalIntakeMlFromPhone, daysPrior) {
        if (currentOffset == 0) {
            totalIntakeMlFromPhone
        } else {
            val idx = currentOffset + daysPrior
            if (idx in intakesList.indices) {
                intakesList[idx]
            } else {
                0
            }
        }
    }

    val activeGoalMl = remember(goalsList, currentOffset, goalMl, daysPrior) {
        if (currentOffset == 0) {
            goalMl
        } else {
            val idx = currentOffset + daysPrior
            if (idx in goalsList.indices) {
                goalsList[idx]
            } else {
                2000
            }
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val primaryColor = MaterialTheme.colorScheme.primary
    val surpassedColor = remember(primaryColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(primaryColor.toArgb(), hsv)
        hsv[0] = (hsv[0] + 120f) % 360f
        hsv[1] = hsv[1].coerceAtLeast(0.85f)
        hsv[2] = hsv[2].coerceAtLeast(0.85f)
        Color(android.graphics.Color.HSVToColor(hsv))
    }
    val surpassedColor2 = remember(primaryColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(primaryColor.toArgb(), hsv)
        hsv[0] = (hsv[0] + 240f) % 360f
        hsv[1] = hsv[1].coerceAtLeast(0.85f)
        hsv[2] = hsv[2].coerceAtLeast(0.85f)
        Color(android.graphics.Color.HSVToColor(hsv))
    }
    val displayTextColor = Color(textColorResolved)

    val localIntakesMap = remember { mutableStateMapOf<Int, Int?>() }
    var accumulatedScroll by remember { mutableStateOf(0f) }
    var lastScrollTime by remember { mutableStateOf(0L) }
    val focusRequester = remember { FocusRequester() }

    var crownAccumulatedChange by remember { mutableStateOf(0) }
    var crownSyncJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) {}
    
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    
    LaunchedEffect(activeIntakeFromPhone, currentOffset) {
        if (crownAccumulatedChange == 0) {
            localIntakesMap[currentOffset] = activeIntakeFromPhone
        }
    }

    val totalIntakeMl = (localIntakesMap[currentOffset] ?: activeIntakeFromPhone).coerceAtLeast(0)
    val percentage = if (activeGoalMl > 0) totalIntakeMl.toFloat() / activeGoalMl.toFloat() else 0f

    var showWaterRemaining by remember { mutableStateOf(showRemainingState.value) }
    LaunchedEffect(showRemainingState.value) {
        showWaterRemaining = showRemainingState.value
    }

    val animatedPercentage by if (wearRamOptimization) {
        remember(percentage) { mutableStateOf(percentage) }
    } else {
        animateFloatAsState(
            targetValue = percentage,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "wear_progress"
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "wear_breathing")
    val breatheScale by if (wearRamOptimization) {
        remember { mutableStateOf(1.0f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0.88f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(2400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "wear_breathe"
        )
    }

    var hasCelebratedCurrentGoal by remember { mutableStateOf(false) }

    LaunchedEffect(totalIntakeMl, activeGoalMl, currentOffset) {
        if (activeGoalMl > 0 && totalIntakeMl >= activeGoalMl && currentOffset == 0) {
            if (!hasCelebratedCurrentGoal) {
                hasCelebratedCurrentGoal = true
                performWearHaptic(context, wearHapticStrength, "GOAL_CELEBRATION")
            }
        } else if (totalIntakeMl < activeGoalMl) {
            hasCelebratedCurrentGoal = false
        }
    }

    val centerTextScale = remember { Animatable(1f) }
    LaunchedEffect(totalIntakeMl) {
        try { focusRequester.requestFocus() } catch (e: Exception) {}
        if (!wearRamOptimization) {
            centerTextScale.animateTo(
                targetValue = 1.14f,
                animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing)
            )
            centerTextScale.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        } else {
            centerTextScale.snapTo(1.0f)
        }
    }

    LaunchedEffect(Unit) {
        delay(100)
        try { focusRequester.requestFocus() } catch (e: Exception) {}
        delay(250)
        try { focusRequester.requestFocus() } catch (e: Exception) {}
    }

    LaunchedEffect(crownRotationEnabled) {
        if (crownRotationEnabled) {
            delay(50)
            try { focusRequester.requestFocus() } catch (e: Exception) {}
        }
    }

    val rotaryModifier = if (crownRotationEnabled) {
        Modifier.onRotaryScrollEvent { event ->
            val scrollAmount = event.verticalScrollPixels
            val currentTime = System.currentTimeMillis()
            
            if (currentTime - lastScrollTime > 300) {
                accumulatedScroll = 0f
            }
            lastScrollTime = currentTime

            if ((scrollAmount > 0f && accumulatedScroll < 0f) || (scrollAmount < 0f && accumulatedScroll > 0f)) {
                accumulatedScroll = 0f
            }

            accumulatedScroll += scrollAmount
            val absScroll = kotlin.math.abs(accumulatedScroll)
            val scrollThreshold = 18f
            
            if (absScroll >= scrollThreshold) {
                val times = (absScroll / scrollThreshold).toInt().coerceAtMost(2)
                val direction = if (accumulatedScroll > 0f) 1 else -1
                accumulatedScroll %= scrollThreshold
                
                val realDirection = if (crownRotationReverse) -direction else direction
                val mlPerClick = if (crownClicksPerMl > 0) crownClicksPerMl else 10
                val amountToAdd = times * realDirection * mlPerClick
                
                val currentTotal = localIntakesMap[currentOffset] ?: activeIntakeFromPhone
                val newTotal = (currentTotal + amountToAdd).coerceAtLeast(0)
                
                if (newTotal != currentTotal) {
                    val actualDelta = newTotal - currentTotal
                    if (actualDelta != 0) {
                        localIntakesMap[currentOffset] = newTotal
                        crownAccumulatedChange += actualDelta
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        
                        crownSyncJob?.cancel()
                        crownSyncJob = coroutineScope.launch {
                            delay(1500L)
                            val changeToSync = crownAccumulatedChange
                            if (changeToSync != 0) {
                                onQuickAddClick(changeToSync, currentOffset)
                                crownAccumulatedChange = 0
                            }
                        }
                    }
                }
            }
            true
        }
    } else {
        Modifier
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (devOverlayEnabled) Color(devOverlayR, devOverlayG, devOverlayB) else Color.Black)
            .then(rotaryModifier)
            .focusRequester(focusRequester)
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current

        // Reusable Native Objects for progress circle drawing
        val paintBottom = remember {
            android.graphics.Paint().apply {
                isAntiAlias = true
                typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
                textAlign = android.graphics.Paint.Align.CENTER
                letterSpacing = 0.08f
            }
        }
        val paintTop = remember {
            android.graphics.Paint().apply {
                isAntiAlias = true
                typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
                textAlign = android.graphics.Paint.Align.CENTER
                letterSpacing = 0.1f
            }
        }
        val rectBottom = remember { android.graphics.RectF() }
        val rectTop = remember { android.graphics.RectF() }
        val pathBottom = remember { android.graphics.Path() }
        val pathTop = remember { android.graphics.Path() }

        // Reusable Native Path for squiggly/smooth graph line
        val graphGoalPath = remember { androidx.compose.ui.graphics.Path() }
        val graphPoints = remember { mutableListOf<androidx.compose.ui.geometry.Offset>() }

        AnimatedContent(
            targetState = currentLayer,
            transitionSpec = {
                val isUpOrDown = graphSwipeDir == "UP" || graphSwipeDir == "DOWN"
                if (isUpOrDown) {
                    if (targetState > initialState) {
                        slideInVertically { height -> height } + fadeIn() togetherWith
                                slideOutVertically { height -> -height } + fadeOut()
                    } else {
                        slideInVertically { height -> -height } + fadeIn() togetherWith
                                slideOutVertically { height -> height } + fadeOut()
                    }
                } else {
                    if (targetState > initialState) {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> width } + fadeOut()
                    }
                }
            },
            label = "layer_transition"
        ) { layer ->
            if (layer == 1) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (showRemainingButton) Modifier.padding(bottom = 12.dp, top = 8.dp) else Modifier
                            )
                    ) {
            val defaultSize = if (showRemainingButton) 170 else 200
            val sizeDp = if (circleSize > 0) circleSize.dp else defaultSize.dp

            Box(
                modifier = Modifier
                    .then(
                        if (circleSize > 0) {
                            Modifier.size(circleSize.dp)
                        } else {
                            if (showRemainingButton) {
                                Modifier.weight(1f).aspectRatio(1f).padding(bottom = 4.dp)
                            } else {
                                Modifier.size(defaultSize.dp)
                            }
                        }
                    )
                    .clip(CircleShape)
                    .pointerInput(quickAddAmount, longPressDuration, longPressEnabled, swipeMode, graphSwipeDir, currentOffset, daysPrior, pastDaysToShow, maxForward, intakesList, activeIntakeFromPhone, localIntakesMap) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                try { focusRequester.requestFocus() } catch (e: Exception) {}

                                var isLongPress = false
                                var isDrag = false
                                var accumulatedX = 0f
                                var accumulatedY = 0f
                                val touchSlop = 15f

                                val longPressJob = coroutineScope.launch {
                                    delay(longPressDuration)
                                    if (longPressEnabled && !isDrag) {
                                        isLongPress = true
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        crownSyncJob?.cancel()
                                        crownAccumulatedChange = 0
                                        
                                        val currentTotal = localIntakesMap[currentOffset] ?: activeIntakeFromPhone
                                        val newTotal = (currentTotal - quickAddAmount).coerceAtLeast(0)
                                        localIntakesMap[currentOffset] = newTotal
                                        onDeleteLastEntry(currentOffset)
                                    }
                                }

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val anyDown = event.changes.any { it.pressed }
                                    if (!anyDown) break

                                    val change = event.changes.firstOrNull()
                                    if (change != null) {
                                        val dragAmount = change.position - change.previousPosition
                                        accumulatedX += dragAmount.x
                                        accumulatedY += dragAmount.y

                                        val absX = kotlin.math.abs(accumulatedX)
                                        val absY = kotlin.math.abs(accumulatedY)

                                        if (absX > touchSlop || absY > touchSlop) {
                                            if (!isDrag) {
                                                isDrag = true
                                                longPressJob.cancel()
                                            }
                                        }

                                        if (isDrag) {
                                            change.consume()
                                            
                                            val isGraphVertical = graphSwipeDir == "UP" || graphSwipeDir == "DOWN"
                                            val layerTargetOffset = if (isGraphVertical) accumulatedY else accumulatedX
                                            val absLayerDrag = kotlin.math.abs(layerTargetOffset)

                                            val isDaySwipeVertical = swipeMode == "VERTICAL"
                                            val dayTargetOffset = if (isDaySwipeVertical) accumulatedY else accumulatedX
                                            val absDayDrag = kotlin.math.abs(dayTargetOffset)

                                            val isDaySwipeAllowed = swipeMode != "OFF"
                                            val threshold = 60f
                                            if (absLayerDrag > threshold || (isDaySwipeAllowed && absDayDrag > threshold)) {
                                                if (!hasSnappedHaptic) {
                                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                    hasSnappedHaptic = true
                                                }
                                            } else {
                                                hasSnappedHaptic = false
                                            }
                                        }
                                    }
                                }

                                longPressJob.cancel()

                                if (isDrag) {
                                    val threshold = 60f
                                    var handledLayerSwitch = false
                                    when (graphSwipeDir) {
                                        "LEFT" -> {
                                            if (accumulatedX < -threshold) {
                                                currentLayer = 2
                                                handledLayerSwitch = true
                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            }
                                        }
                                        "RIGHT" -> {
                                            if (accumulatedX > threshold) {
                                                currentLayer = 2
                                                handledLayerSwitch = true
                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            }
                                        }
                                        "UP" -> {
                                            if (accumulatedY < -threshold) {
                                                currentLayer = 2
                                                handledLayerSwitch = true
                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            }
                                        }
                                        "DOWN" -> {
                                            if (accumulatedY > threshold) {
                                                currentLayer = 2
                                                handledLayerSwitch = true
                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            }
                                        }
                                    }

                                    if (!handledLayerSwitch && swipeMode != "OFF") {
                                        if (swipeMode == "HORIZONTAL") {
                                            if (accumulatedX > threshold) {
                                                if (currentOffset > -pastDaysToShow) {
                                                    currentOffset--
                                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                }
                                            } else if (accumulatedX < -threshold) {
                                                if (currentOffset < maxForward) {
                                                    currentOffset++
                                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                }
                                            }
                                        } else if (swipeMode == "VERTICAL") {
                                            if (accumulatedY > threshold) {
                                                if (currentOffset > -pastDaysToShow) {
                                                    currentOffset--
                                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                }
                                            } else if (accumulatedY < -threshold) {
                                                if (currentOffset < maxForward) {
                                                    currentOffset++
                                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                }
                                            }
                                        }
                                    }
                                } else if (!isLongPress) {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    val currentTotal = localIntakesMap[currentOffset] ?: activeIntakeFromPhone
                                    val newTotal = currentTotal + quickAddAmount
                                    localIntakesMap[currentOffset] = newTotal
                                    
                                    crownAccumulatedChange += quickAddAmount
                                    crownSyncJob?.cancel()
                                    crownSyncJob = coroutineScope.launch {
                                        delay(1500L)
                                        val changeToSync = crownAccumulatedChange
                                        if (changeToSync != 0) {
                                            onQuickAddClick(changeToSync, currentOffset)
                                            crownAccumulatedChange = 0
                                        }
                                    }
                                }
                                hasSnappedHaptic = false
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (progressCircleEnabled) {
                    Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val strokeWidthPx = when (progressCircleThickness) {
                            "THICK" -> 24.dp.toPx()
                            "CUSTOM" -> customThickness.dp.toPx()
                            else -> 14.4.dp.toPx()
                        }
                        val diameter = size.minDimension - strokeWidthPx
                        val radius = diameter / 2f
                        val centerOffset = Offset(size.width / 2f, size.height / 2f)

                        val centerCircleRadius = radius - strokeWidthPx / 2f
                        if (!wearInsideCircleRemoved) {
                            drawCircle(
                                color = Color(0xFF111213),
                                radius = centerCircleRadius,
                                center = centerOffset
                            )

                            drawCircle(
                                color = primaryColor.copy(alpha = 0.05f * breatheScale),
                                radius = centerCircleRadius * breatheScale,
                                center = centerOffset
                            )
                        }

                        drawCircle(
                            color = Color(0xFF2C2D2F),
                            radius = radius,
                            center = centerOffset,
                            style = Stroke(width = strokeWidthPx)
                        )

                        val basePercentage = animatedPercentage.coerceAtMost(1f)
                        drawArc(
                            color = primaryColor,
                            startAngle = -90f,
                            sweepAngle = basePercentage * 360f,
                            useCenter = false,
                            topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                            size = Size(diameter, diameter),
                            style = Stroke(
                                width = strokeWidthPx,
                                cap = StrokeCap.Round
                            )
                        )

                        if (animatedPercentage > 1f) {
                            val overflow1 = (animatedPercentage - 1f).coerceAtMost(1f)
                            drawArc(
                                color = surpassedColor,
                                startAngle = -90f,
                                sweepAngle = overflow1 * 360f,
                                useCenter = false,
                                topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                                size = Size(diameter, diameter),
                                style = Stroke(
                                    width = strokeWidthPx,
                                    cap = StrokeCap.Round
                                )
                            )
                        }

                        if (animatedPercentage > 2f) {
                            val overflow2 = (animatedPercentage - 2f).coerceAtMost(1f)
                            drawArc(
                                color = surpassedColor2,
                                startAngle = -90f,
                                sweepAngle = overflow2 * 360f,
                                useCenter = false,
                                topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                                size = Size(diameter, diameter),
                                style = Stroke(
                                    width = strokeWidthPx,
                                    cap = StrokeCap.Round
                                )
                            )
                        }
                        
                        val textRadiusOffset = when (progressCircleThickness) {
                            "THICK" -> (24f * 0.35f + 2.5f).coerceIn(4f, 15f).dp.toPx()
                            "CUSTOM" -> (customThickness * 0.35f + 2.5f).coerceIn(4f, 15f).dp.toPx()
                            else -> (14.4f * 0.35f + 2.5f).coerceIn(4f, 15f).dp.toPx()
                        }
                        val textRadius = centerCircleRadius - textRadiusOffset
                        
                        paintBottom.color = displayTextColor.toArgb()
                        paintBottom.textSize = when (progressCircleThickness) {
                            "THICK" -> (47f * 0.2f).coerceIn(4f, 16f).dp.toPx()
                            "CUSTOM" -> (customTextSize * 0.2f).coerceIn(4f, 16f).dp.toPx()
                            else -> (28.2f * 0.2f).coerceIn(4f, 16f).dp.toPx()
                        }

                        rectBottom.set(
                            centerOffset.x - textRadius,
                            centerOffset.y - textRadius,
                            centerOffset.x + textRadius,
                            centerOffset.y + textRadius
                        )
                        pathBottom.reset()
                        pathBottom.addArc(rectBottom, 180f, -180f)

                        val bottomText = if (showWaterRemaining) {
                            if (appLanguage == "el") "ml απομένουν" else "ml remaining"
                        } else {
                            "ml / $activeGoalMl"
                        }

                        drawContext.canvas.nativeCanvas.drawTextOnPath(
                            bottomText,
                            pathBottom,
                            0f,
                            0f,
                            paintBottom
                        )

                        // Top curved Day Label Header inside the circle
                        val dayHeaderColor = if (currentOffset == 0) primaryColor else Color(0xFFFFB74D)
                        paintTop.color = dayHeaderColor.toArgb()
                        paintTop.alpha = (headerAlpha * 255f).toInt().coerceIn(0, 255)
                        paintTop.textSize = when (progressCircleThickness) {
                            "THICK" -> (47f * 0.22f).coerceIn(4f, 18f).dp.toPx()
                            "CUSTOM" -> (customTextSize * 0.22f).coerceIn(4f, 18f).dp.toPx()
                            else -> (28.2f * 0.22f).coerceIn(4f, 18f).dp.toPx()
                        }

                        rectTop.set(
                            centerOffset.x - textRadius,
                            centerOffset.y - textRadius,
                            centerOffset.x + textRadius,
                            centerOffset.y + textRadius
                        )
                        pathTop.reset()
                        pathTop.addArc(rectTop, 180f, 180f)

                        val topText = when (currentOffset) {
                            -3 -> if (appLanguage == "el") "ΠΡΙΝ 3 ΜΕΡΕΣ" else "3 DAYS AGO"
                            -2 -> if (appLanguage == "el") "ΠΡΙΝ 2 ΜΕΡΕΣ" else "2 DAYS AGO"
                            -1 -> if (appLanguage == "el") "ΧΘΕΣ" else "YESTERDAY"
                            0 -> if (appLanguage == "el") "ΣΗΜΕΡΑ" else "TODAY"
                            1 -> if (appLanguage == "el") "ΑΥΡΙΟ" else "TOMORROW"
                            2 -> if (appLanguage == "el") "ΣΕ 2 ΜΕΡΕΣ" else "IN 2 DAYS"
                            else -> {
                                if (currentOffset < 0) {
                                    if (appLanguage == "el") "ΠΡΙΝ ${-currentOffset} ΜΕΡΕΣ" else "${-currentOffset} DAYS AGO"
                                } else {
                                    if (appLanguage == "el") "ΣΕ $currentOffset ΜΕΡΕΣ" else "IN $currentOffset DAYS"
                                }
                            }
                        }

                        if (headerAlpha > 0f) {
                            drawContext.canvas.nativeCanvas.drawTextOnPath(
                                topText,
                                pathTop,
                                0f,
                                0f,
                                paintTop
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .scale(centerTextScale.value)
                        .align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val intakeToFormat = if (showWaterRemaining) (activeGoalMl - totalIntakeMl).coerceAtLeast(0) else totalIntakeMl
                    val formattedIntakeStr = if (intakeToFormat >= 1000) {
                        val liters = intakeToFormat / 1000
                        val remaining = intakeToFormat % 1000
                        String.format(Locale.US, "%d.%03d", liters, remaining)
                    } else {
                        intakeToFormat.toString()
                    }

                    Text(
                        text = formattedIntakeStr,
                        fontSize = when (progressCircleThickness) {
                            "THICK" -> 47.sp
                            "CUSTOM" -> customTextSize.sp
                            else -> 28.2.sp
                        },
                        fontWeight = FontWeight.ExtraBold,
                        color = displayTextColor,
                        letterSpacing = (-0.5).sp
                    )
                }
            }
        }
    }
    } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    var statsDragX by remember { mutableStateOf(0f) }
                    val statsDragThreshold = 60f
                    val maxVisualOffsetPx = with(LocalDensity.current) { 12.dp.toPx() }
                    val statsDragDistance = kotlin.math.abs(statsDragX)
                    val statsDragFraction = (statsDragDistance / statsDragThreshold).coerceIn(0f, 1f)
                    val statsBaseCorner = 24.dp
                    val statsActiveCorner = (24f * (1f - statsDragFraction) + 4f * statsDragFraction).dp

                    val statsMorphedShape = when {
                        statsDragX > 1f -> RoundedCornerShape(
                            topStart = statsBaseCorner,
                            bottomStart = statsBaseCorner,
                            topEnd = statsActiveCorner,
                            bottomEnd = statsActiveCorner
                        )
                        statsDragX < -1f -> RoundedCornerShape(
                            topStart = statsActiveCorner,
                            bottomStart = statsActiveCorner,
                            topEnd = statsBaseCorner,
                            bottomEnd = statsBaseCorner
                        )
                        else -> RoundedCornerShape(statsBaseCorner)
                    }

                    val visualOffsetPx = if (statsDragX > 0) {
                        (statsDragX * 0.2f).coerceAtMost(maxVisualOffsetPx)
                    } else {
                        (statsDragX * 0.2f).coerceAtLeast(-maxVisualOffsetPx)
                    }

                    val scrollState = rememberScrollState()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 0.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .offset {
                                    androidx.compose.ui.unit.IntOffset(
                                        x = visualOffsetPx.toInt(),
                                        y = 0
                                    )
                                }
                                .padding(bottom = 6.dp)
                                .background(
                                    color = primaryColor.copy(alpha = 0.15f),
                                    shape = statsMorphedShape
                                )
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val down = awaitFirstDown(requireUnconsumed = false)
                                            var isDrag = false
                                            var accumulatedX = 0f
                                            val touchSlop = 15f
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val anyDown = event.changes.any { it.pressed }
                                                if (!anyDown) break
                                                val change = event.changes.firstOrNull()
                                                if (change != null) {
                                                    if (change.isConsumed) continue
                                                    val dragAmount = change.position - change.previousPosition
                                                    accumulatedX += dragAmount.x
                                                    if (kotlin.math.abs(accumulatedX) > touchSlop) {
                                                        isDrag = true
                                                    }
                                                    if (isDrag) {
                                                        change.consume()
                                                        statsDragX += dragAmount.x
                                                    }
                                                }
                                            }
                                            if (isDrag) {
                                                if (kotlin.math.abs(statsDragX) > statsDragThreshold) {
                                                    currentLayer = 1
                                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                }
                                            }
                                            statsDragX = 0f
                                        }
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (appLanguage == "el") "ΣΤΑΤΙΣΤΙΚΑ" else "STATS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                        }

                    val wearChartData = remember(intakesList, goalsList, appLanguage, daysPrior, maxForward) {
                        (-daysPrior..maxForward).map { offset ->
                            val label = when (offset) {
                                0 -> if (appLanguage == "el") "ΣΗΜ" else "TOD"
                                -1 -> if (appLanguage == "el") "ΧΘΕ" else "YEST"
                                1 -> if (appLanguage == "el") "ΑΥΡ" else "TOM"
                                else -> {
                                    if (offset < 0) {
                                        if (appLanguage == "el") "ΠΡ${-offset}" else "${offset}D"
                                    } else {
                                        if (appLanguage == "el") "ΣΕ$offset" else "+${offset}D"
                                    }
                                }
                            }
                            val originalIdx = offset + daysPrior
                            val intake = intakesList.getOrNull(originalIdx) ?: 0
                            val goal = goalsList.getOrNull(originalIdx) ?: 2000
                            Triple(label, intake, goal)
                        }
                    }

                    val maxVal = remember(wearChartData) {
                        val maxAmount = wearChartData.maxOfOrNull { it.second } ?: 1000
                        val maxGoal = wearChartData.maxOfOrNull { it.third } ?: 1000
                        val refMax = maxOf(maxAmount, maxGoal)
                        if (refMax <= 0) 1000 else refMax
                    }

                    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
                    val wearProgressCircleThickness = progressCircleThicknessState.value
                    val customThickness = customThicknessState.value
                    val wearGraphPillarThickness = wearGraphPillarThicknessState.value
                    val wearGraphHorizontalPadding = wearGraphHorizontalPaddingState.value.dp

                    val effectiveBarWidth = when (wearProgressCircleThickness) {
                        "THIN" -> (wearGraphPillarThickness * (14.4f / 24f)).coerceIn(8f, 40f).dp
                        "THICK" -> wearGraphPillarThickness.dp
                        "CUSTOM" -> (wearGraphPillarThickness * (customThickness / 24f)).coerceIn(6f, 48f).dp
                        else -> wearGraphPillarThickness.dp
                    }
                    val barWidth = effectiveBarWidth
                    val colWidth = barWidth + 6.dp

                    val wearThemePalette = remember(primaryColor) {
                        val hsl = FloatArray(3)
                        androidx.core.graphics.ColorUtils.colorToHSL(primaryColor.toArgb(), hsl)
                        val h = hsl[0]
                        val s = hsl[1]
                        val l = hsl[2]

                        // Active Pillar Body (goal achieved): Deep rich theme color (fully opaque)
                        val activePillar = Color(
                            androidx.core.graphics.ColorUtils.HSLToColor(
                                floatArrayOf(h, (s * 1.1f).coerceIn(0.6f, 1f), (l * 0.95f).coerceIn(0.40f, 0.65f))
                            )
                        )

                        // In-Progress Pillar Body (valMl > 0 but < valGoal): Soft vibrant theme tint (fully opaque)
                        val inProgressPillar = Color(
                            androidx.core.graphics.ColorUtils.HSLToColor(
                                floatArrayOf(h, (s * 0.85f).coerceIn(0.4f, 0.9f), (l * 1.25f).coerceIn(0.60f, 0.85f))
                            )
                        )

                        // Rosette Badge Background: Bright luminous analogous tint (+25deg hue shift)
                        val badgeBg = Color(
                            androidx.core.graphics.ColorUtils.HSLToColor(
                                floatArrayOf((h + 25f) % 360f, (s * 1.1f).coerceIn(0.7f, 1f), 0.82f)
                            )
                        )

                        // Check Mark Tick inside Rosette Badge: Rich contrasting theme accent (-20deg hue shift)
                        val checkMark = Color(
                            androidx.core.graphics.ColorUtils.HSLToColor(
                                floatArrayOf((h - 20f + 360f) % 360f, (s * 1.2f).coerceIn(0.8f, 1f), 0.28f)
                            )
                        )

                        val zeroBar = activePillar.copy(alpha = 0.22f)

                        listOf(activePillar, inProgressPillar, badgeBg, checkMark, zeroBar)
                    }

                    val graphActivePillarColor = wearThemePalette[0]
                    val graphInProgressPillarColor = wearThemePalette[1]
                    val graphBadgeBgColor = wearThemePalette[2]
                    val graphCheckMarkColor = wearThemePalette[3]
                    val graphZeroBarColor = wearThemePalette[4]

                    LaunchedEffect(currentOffset, daysPrior) {
                        val activeIndex = currentOffset + daysPrior
                        val density = context.resources.displayMetrics.density
                        val colWidthPx = colWidth.value * density
                        val targetScroll = colWidthPx * (activeIndex - 2.1f).coerceAtLeast(0f)
                        scrollState.animateScrollTo(targetScroll.toInt())
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(124.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .horizontalScroll(scrollState)
                        ) {
                            val totalScrollWidth = (colWidth * wearChartData.size) + (wearGraphHorizontalPadding * 2)

                            Box(
                                modifier = Modifier
                                    .width(totalScrollWidth)
                                    .fillMaxHeight()
                            ) {
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(bottom = 4.dp)
                                ) {
                                    val numSegments = wearChartData.size
                                    if (numSegments > 0) {
                                        val padPx = wearGraphHorizontalPadding.toPx()
                                        val segmentWidth = (size.width - padPx * 2f).coerceAtLeast(1f) / numSegments
                                        graphGoalPath.reset()
                                        graphPoints.clear()

                                        val invMaxVal = if (maxVal > 0) 1f / maxVal.toFloat() else 0.001f
                                        for (idx in 0 until numSegments) {
                                            val itemGoal = wearChartData[idx].third
                                            val goalRatio = (itemGoal.toFloat() * invMaxVal).coerceIn(0f, 1f) * 0.72f + 0.12f
                                            val y = size.height * (1f - goalRatio)
                                            val x = padPx + idx * segmentWidth + segmentWidth / 2f
                                            graphPoints.add(androidx.compose.ui.geometry.Offset(x, y))
                                        }

                                        if (graphPoints.size > 1) {
                                            val strokeWidth = (barWidth.value * 0.13f).coerceIn(2.2f, 5.0f).dp.toPx()
                                            if (goalLineSquiggly) {
                                                val amplitude = (barWidth.value * 0.20f).coerceIn(2.5f, 6.5f).dp.toPx()
                                                val period = (barWidth.value * 0.85f).coerceIn(14f, 28f).dp.toPx()
                                                val twoPiOverPeriod = (2 * Math.PI / period).toFloat()
                                                
                                                val firstP = graphPoints.first()
                                                val startSquigglyY = firstP.y + amplitude * kotlin.math.sin(0.0).toFloat()
                                                graphGoalPath.moveTo(0f, startSquigglyY)
                                                
                                                val step = 3f
                                                var currentX = 0f
                                                while (currentX <= size.width) {
                                                    val baseY = when {
                                                        currentX <= graphPoints.first().x -> graphPoints.first().y
                                                        currentX >= graphPoints.last().x -> graphPoints.last().y
                                                        else -> {
                                                            var interpolatedY = graphPoints.first().y
                                                            for (i in 0 until graphPoints.size - 1) {
                                                                val p1 = graphPoints[i]
                                                                val p2 = graphPoints[i + 1]
                                                                if (currentX >= p1.x && currentX <= p2.x) {
                                                                    val deltaX = p2.x - p1.x
                                                                    val t = if (deltaX != 0f) (currentX - p1.x) / deltaX else 0f
                                                                    interpolatedY = p1.y + t * (p2.y - p1.y)
                                                                    break
                                                                }
                                                            }
                                                            interpolatedY
                                                        }
                                                    }
                                                    val squigglyY = baseY + amplitude * kotlin.math.sin(twoPiOverPeriod * currentX)
                                                    graphGoalPath.lineTo(currentX, squigglyY)
                                                    currentX += step
                                                }
                                                val lastBaseY = graphPoints.last().y
                                                val endSquigglyY = lastBaseY + amplitude * kotlin.math.sin(twoPiOverPeriod * size.width)
                                                graphGoalPath.lineTo(size.width, endSquigglyY)
                                            } else {
                                                graphGoalPath.moveTo(0f, graphPoints[0].y)
                                                graphGoalPath.lineTo(graphPoints[0].x, graphPoints[0].y)
                                                for (i in 0 until graphPoints.size - 1) {
                                                    val p1 = graphPoints[i]
                                                    val p2 = graphPoints[i + 1]
                                                    val dx = (p2.x - p1.x) / 2f
                                                    graphGoalPath.cubicTo(
                                                        p1.x + dx, p1.y,
                                                        p2.x - dx, p2.y,
                                                        p2.x, p2.y
                                                    )
                                                }
                                                graphGoalPath.lineTo(size.width, graphPoints.last().y)
                                            }
                                            drawPath(
                                                path = graphGoalPath,
                                                color = primaryColor.copy(alpha = 0.5f),
                                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                                    width = strokeWidth,
                                                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                                                )
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = wearGraphHorizontalPadding),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    wearChartData.forEachIndexed { idx, (label, valMl, valGoal) ->
                                        val activeBar = valMl >= valGoal
                                        val isActiveDay = idx == (currentOffset + daysPrior)

                                        Box(
                                            modifier = Modifier
                                                .width(colWidth)
                                                .fillMaxHeight()
                                                .padding(bottom = 4.dp),
                                            contentAlignment = Alignment.BottomCenter
                                        ) {
                                            val barHeightRatio = (valMl.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f) * 0.72f + 0.12f

                                            if (valMl > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxHeight(barHeightRatio)
                                                        .width(barWidth),
                                                    contentAlignment = Alignment.TopCenter
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxHeight()
                                                            .width(barWidth)
                                                            .clip(RoundedCornerShape(percent = 50))
                                                            .background(
                                                                color = if (activeBar) graphActivePillarColor else graphInProgressPillarColor
                                                            )
                                                    )

                                                    if (activeBar) {
                                                        val rosetteSize = (barWidth.value * 0.82f).coerceIn(12f, 32f).dp
                                                        val rosetteOffsetY = (barWidth.value * 0.08f).coerceIn(1.5f, 4f).dp
                                                        VerifiedRosetteBadge(
                                                            modifier = Modifier
                                                                .size(rosetteSize)
                                                                .offset(y = rosetteOffsetY),
                                                            tint = graphBadgeBgColor,
                                                            checkMarkColor = graphCheckMarkColor
                                                        )
                                                    }
                                                }
                                            } else {
                                                val zeroBarHeight = (barWidth.value * 0.12f).coerceIn(2f, 5f).dp
                                                Box(
                                                    modifier = Modifier
                                                        .height(zeroBarHeight)
                                                        .width(barWidth)
                                                        .clip(RoundedCornerShape(percent = 50))
                                                        .background(
                                                            color = graphZeroBarColor
                                                        )
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
}
}

@Composable
private fun VerifiedRosetteBadge(
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFF4FE3D6),
    checkMarkColor: Color? = null
) {
    val finalCheckMarkColor = checkMarkColor ?: remember(tint) {
        val hsl = FloatArray(3)
        androidx.core.graphics.ColorUtils.colorToHSL(tint.toArgb(), hsl)
        val h = (hsl[0] - 20f + 360f) % 360f
        val s = (hsl[1] * 1.2f).coerceIn(0.7f, 1.0f)
        val l = if (tint.luminance() > 0.5f) 0.22f else 0.92f
        Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(h, s, l)))
    }

    val rosettePath = remember { androidx.compose.ui.graphics.Path() }
    val tickPath = remember { androidx.compose.ui.graphics.Path() }

    Canvas(
        modifier = modifier
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxR = size.minDimension / 2f
        val baseRadius = maxR * 0.82f
        val amplitude = maxR * 0.18f
        
        rosettePath.reset()
        val steps = 36 // 36 steps is visually smooth for a 13dp badge with 12 petals
        for (i in 0 until steps) {
            val angle = (i * 2 * Math.PI / steps).toFloat()
            val r = baseRadius + amplitude * kotlin.math.cos(12f * angle)
            val x = center.x + r * kotlin.math.cos(angle)
            val y = center.y + r * kotlin.math.sin(angle)
            if (i == 0) {
                rosettePath.moveTo(x, y)
            } else {
                rosettePath.lineTo(x, y)
            }
        }
        rosettePath.close()
        drawPath(rosettePath, color = tint)

        val w = size.width
        val h = size.height
        
        tickPath.reset()
        tickPath.moveTo(w * 0.32f, h * 0.52f)
        tickPath.lineTo(w * 0.45f, h * 0.65f)
        tickPath.lineTo(w * 0.68f, h * 0.38f)
        
        drawPath(
            path = tickPath,
            color = finalCheckMarkColor,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = (size.minDimension * 0.16f).coerceIn(2.4.dp.toPx(), 4.5.dp.toPx()),
                cap = StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
    }
}

@Composable
fun WearAppTheme(primaryColor: Color = Color(0xFF00BFA5), content: @Composable () -> Unit) {
    val darkColorScheme = darkColorScheme(
        primary = primaryColor,
        secondary = primaryColor.copy(alpha = 0.8f),
        background = Color.Black,
        surface = Color(0xFF1C1E1E),
        onSurface = Color.White
    )
    MaterialTheme(
        colorScheme = darkColorScheme,
        content = content
    )
}

fun performWearHaptic(context: android.content.Context, hapticStrength: String, type: String = "CLICK") {
    if (hapticStrength == "NONE") return
    
    val vibrator = try {
        @Suppress("DEPRECATION")
        context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
    } catch (e: Exception) {
        null
    } ?: return

    if (type == "GOAL_CELEBRATION" || type == "GOAL") {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                if (vibrator.areAllPrimitivesSupported(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK)) {
                    val composition = android.os.VibrationEffect.startComposition()
                    val hasSlowRise = vibrator.areAllPrimitivesSupported(android.os.VibrationEffect.Composition.PRIMITIVE_SLOW_RISE)
                    val hasThud = vibrator.areAllPrimitivesSupported(android.os.VibrationEffect.Composition.PRIMITIVE_THUD)
                    val hasTick = vibrator.areAllPrimitivesSupported(android.os.VibrationEffect.Composition.PRIMITIVE_TICK)

                    if (hasSlowRise) {
                        composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_SLOW_RISE, 1.0f, 0)
                    }
                    if (hasThud) {
                        composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_THUD, 1.0f, 15)
                    }
                    composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 30)
                    composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 70)
                    if (hasTick) {
                        composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_TICK, 1.0f, 100)
                        composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_TICK, 1.0f, 130)
                    }
                    composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 160)
                    if (hasThud) {
                        composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_THUD, 1.0f, 220)
                    }
                    composition.addPrimitive(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 250)
                    vibrator.vibrate(composition.compose())
                    return
                }
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                // Maximum strength multi-pulse rhythm (amplitude 255 = 100% max intensity)
                val timings = longArrayOf(0, 120, 60, 140, 60, 180, 80, 260)
                val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255, 0, 255)
                vibrator.vibrate(android.os.VibrationEffect.createWaveform(timings, amplitudes, -1))
                return
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 120, 60, 140, 60, 180, 80, 260), -1)
                return
            }
        } catch (e: Exception) {
            Log.e("WearHaptic", "Error playing goal celebration haptic: ${e.message}")
        }
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

    try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(android.os.VibrationEffect.createOneShot(durationMs, amplitude))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    } catch (e: Exception) {
        // Fallback
    }
}

