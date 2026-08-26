package com.pixelwater.app.service

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.google.android.gms.wearable.*
import com.pixelwater.app.data.WaterDatabase
import com.pixelwater.app.data.WaterLog
import com.pixelwater.app.data.WearActionDeduplicator
import com.pixelwater.app.data.WearDataSyncManager
import com.pixelwater.app.widget.WaterTrackerWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PhoneWearableListenerService : WearableListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        Log.d(TAG, "onMessageReceived: ${messageEvent.path}")
        when (messageEvent.path) {
            "/add-water", "/complication-add", "/complication-tap", "/complication_add", "/complication_tap" -> {
                val payload = String(messageEvent.data)
                val parts = payload.split(",")
                val amount = parts.getOrNull(0)?.toIntOrNull() ?: 250
                val offset = parts.getOrNull(1)?.toIntOrNull() ?: 0
                val actionId = parts.getOrNull(2) ?: ""

                if (actionId.isNotEmpty() && WearActionDeduplicator.isProcessedAndMark(applicationContext, actionId)) {
                    Log.d(TAG, "Message ${messageEvent.path} with actionId $actionId already processed, skipping")
                    return
                }

                handleAddWater(amount, offset)
            }
            "/delete-water" -> {
                val payload = String(messageEvent.data)
                val parts = payload.split(",")
                val offset = parts.getOrNull(0)?.toIntOrNull() ?: 0
                val actionId = parts.getOrNull(1) ?: ""

                if (actionId.isNotEmpty() && WearActionDeduplicator.isProcessedAndMark(applicationContext, actionId)) {
                    Log.d(TAG, "Message /delete-water with actionId $actionId already processed, skipping")
                    return
                }

                handleDeleteWater(offset)
            }
            "/request_graph_sync", "/request_wear_sync" -> {
                scope.launch {
                    syncBackToWear(applicationContext)
                }
            }
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/wear_action") {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val actionId = dataMap.getString("action_id", "")
                if (actionId.isNotEmpty() && WearActionDeduplicator.isProcessedAndMark(applicationContext, actionId)) {
                    Log.d(TAG, "DataEvent /wear_action with actionId $actionId already processed, skipping")
                    continue
                }

                val action = dataMap.getString("action", "")
                val offset = dataMap.getInt("offset", 0)
                when (action) {
                    "ADD" -> {
                        val amount = dataMap.getInt("amount", 250)
                        handleAddWater(amount, offset)
                    }
                    "DELETE" -> {
                        handleDeleteWater(offset)
                    }
                }
            }
        }
    }

    private fun handleAddWater(amount: Int, offset: Int) {
        triggerTactileClickHaptic(applicationContext)
        sendHapticToWearNodes(applicationContext)

        scope.launch {
            try {
                val db = WaterDatabase.getDatabase(applicationContext)
                val dao = db.waterLogDao()
                val targetDateStr = getDateStringForOffset(applicationContext, offset)
                val todayStr = getDateStringForOffset(applicationContext, 0)
                val timestamp = if (targetDateStr == todayStr) {
                    System.currentTimeMillis()
                } else {
                    try {
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(targetDateStr)?.time ?: System.currentTimeMillis()
                    } catch (e: Exception) {
                        System.currentTimeMillis()
                    }
                }

                val log = WaterLog(
                    amountMl = amount,
                    beverageType = "Water",
                    waterEquivalency = 1.0f,
                    waterEquivalentMl = amount,
                    dateString = targetDateStr,
                    timestamp = timestamp,
                    sourceDevice = "Wear OS Smartwatch"
                )
                dao.insertLog(log)
                Log.d(TAG, "Successfully inserted log from Wear OS: $amount ml for $targetDateStr")

                try {
                    WaterTrackerWidget().updateAll(applicationContext)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed updating widget: ${e.message}")
                }

                syncBackToWear(applicationContext)
            } catch (e: Exception) {
                Log.e(TAG, "Error handling add water: ${e.message}", e)
            }
        }
    }

    private fun handleDeleteWater(offset: Int) {
        scope.launch {
            try {
                val db = WaterDatabase.getDatabase(applicationContext)
                val dao = db.waterLogDao()
                val targetDateStr = getDateStringForOffset(applicationContext, offset)
                val prefs = applicationContext.getSharedPreferences("water_tracker_prefs", Context.MODE_PRIVATE)
                val quickAddAmount = prefs.getInt("quick_add_amount", 250)

                val logs = dao.getLogsForDateSync(targetDateStr).sortedByDescending { it.timestamp }
                var remainingToSubtract = quickAddAmount
                for (log in logs) {
                    if (remainingToSubtract <= 0) break
                    if (log.amountMl <= remainingToSubtract) {
                        remainingToSubtract -= log.amountMl
                        dao.deleteLog(log)
                    } else {
                        val updated = log.copy(amountMl = log.amountMl - remainingToSubtract)
                        remainingToSubtract = 0
                        dao.insertLog(updated)
                    }
                }

                try {
                    WaterTrackerWidget().updateAll(applicationContext)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed updating widget: ${e.message}")
                }

                syncBackToWear(applicationContext)
            } catch (e: Exception) {
                Log.e(TAG, "Error handling delete water: ${e.message}", e)
            }
        }
    }

    private suspend fun syncBackToWear(context: Context) {
        try {
            val db = WaterDatabase.getDatabase(context)
            val dao = db.waterLogDao()
            val todayStr = getDateStringForOffset(context, 0)
            val totalIntakeToday = dao.getTotalIntakeForDateSync(todayStr) ?: 0

            val prefs = context.getSharedPreferences("water_tracker_prefs", Context.MODE_PRIVATE)
            val dailyGoal = prefs.getInt("daily_goal", 2000)
            val quickAdd = prefs.getInt("quick_add_amount", 250)
            val showRem = prefs.getBoolean("wear_show_remaining", false)
            val haptic = prefs.getString("wear_haptic_strength", "MEDIUM") ?: "MEDIUM"
            val theme = prefs.getString("theme_color", "MINT") ?: "MINT"
            val cSize = prefs.getInt("wear_circle_size", -1)
            val resolvedColor = prefs.getInt("theme_color_resolved", 0xFF00BFA5.toInt())
            val longPressEnabled = prefs.getBoolean("wear_long_press_delete_enabled", true)
            val longPressDuration = prefs.getInt("wear_long_press_delete_duration", 1000)
            val progressCircleEnabled = prefs.getBoolean("wear_progress_circle_enabled", true)
            val textColorType = prefs.getString("wear_text_color_type", "SAME_AS_CIRCLE") ?: "SAME_AS_CIRCLE"
            val textColorResolved = if (textColorType == "SAME_AS_CIRCLE") {
                resolvedColor
            } else {
                val saved = prefs.getInt("text_color_resolved", 0)
                if (saved != 0) saved else resolvedColor
            }
            val crownRotationEnabled = prefs.getBoolean("wear_crown_rotation_enabled", true)
            val crownRotationReverse = prefs.getBoolean("wear_crown_rotation_reverse", false)
            val crownClicksPerMl = prefs.getInt("wear_crown_clicks_per_ml", 10)
            val crownSyncDelay = prefs.getInt("wear_crown_sync_delay", 200)
            val devOverlayEnabled = prefs.getBoolean("wear_dev_overlay_enabled", false)
            val devOverlayR = prefs.getInt("wear_dev_overlay_r", 0)
            val devOverlayG = prefs.getInt("wear_dev_overlay_g", 255)
            val devOverlayB = prefs.getInt("wear_dev_overlay_b", 0)
            val insideCircleRemoved = prefs.getBoolean("wear_inside_circle_removed", false)
            val progressCircleThickness = prefs.getString("wear_progress_circle_thickness", "THIN") ?: "THIN"
            val customThickness = prefs.getFloat("wear_custom_thickness", 13f)
            val graphPillarThickness = prefs.getFloat("wear_graph_pillar_thickness", 24f)
            val graphHorizontalPadding = prefs.getFloat("wear_graph_horizontal_padding", 18f)
            val customTextSize = prefs.getFloat("wear_custom_text_size", 32f)
            val wearShowTomorrowDay = prefs.getBoolean("wear_show_tomorrow_day", true)
            val appLanguage = prefs.getString("app_language", "en") ?: "en"
            val goalLineSquiggly = prefs.getBoolean("goal_line_squiggly", false)
            val ramOptimization = prefs.getBoolean("wear_ram_optimization", false)
            val wearSwipeMode = prefs.getString("wear_swipe_mode", "VERTICAL") ?: "VERTICAL"
            val wearGraphSwipeDir = prefs.getString("wear_graph_swipe_dir", "LEFT") ?: "LEFT"

            val wearGraphWeeksPrior = prefs.getInt("wear_graph_weeks_prior", 1)
            val wearGraphDaysPrior = prefs.getInt("wear_graph_days_prior", 5)
            val wearPastDaysToShow = prefs.getInt("wear_past_days_to_show", 3)
            val days = maxOf(wearPastDaysToShow, wearGraphDaysPrior)

            val intakeM3 = if (days >= 3) (dao.getTotalIntakeForDateSync(getDateStringForOffset(context, -3)) ?: 0) else 0
            val intakeM2 = if (days >= 2) (dao.getTotalIntakeForDateSync(getDateStringForOffset(context, -2)) ?: 0) else 0
            val intakeM1 = if (days >= 1) (dao.getTotalIntakeForDateSync(getDateStringForOffset(context, -1)) ?: 0) else 0
            val intakeP1 = dao.getTotalIntakeForDateSync(getDateStringForOffset(context, 1)) ?: 0
            val intakeP2 = dao.getTotalIntakeForDateSync(getDateStringForOffset(context, 2)) ?: 0

            val goalM3 = if (days >= 3) getDailyGoalForDate(context, prefs, getDateStringForOffset(context, -3)) else 2000
            val goalM2 = if (days >= 2) getDailyGoalForDate(context, prefs, getDateStringForOffset(context, -2)) else 2000
            val goalM1 = if (days >= 1) getDailyGoalForDate(context, prefs, getDateStringForOffset(context, -1)) else 2000
            val goalP1 = getDailyGoalForDate(context, prefs, getDateStringForOffset(context, 1))
            val goalP2 = getDailyGoalForDate(context, prefs, getDateStringForOffset(context, 2))

            val startOffset = -days
            val intakesList = (startOffset..2).map { offset ->
                dao.getTotalIntakeForDateSync(getDateStringForOffset(context, offset)) ?: 0
            }
            val goalsList = (startOffset..2).map { offset ->
                getDailyGoalForDate(context, prefs, getDateStringForOffset(context, offset))
            }
            val pastIntakesCsv = intakesList.joinToString(",")
            val pastGoalsCsv = goalsList.joinToString(",")

            WearDataSyncManager.syncStatus(
                context = context,
                totalIntakeMl = totalIntakeToday,
                goalMl = dailyGoal,
                quickAddAmount = quickAdd,
                showRemaining = showRem,
                hapticStrength = haptic,
                themeColor = theme,
                circleSize = cSize,
                resolvedColor = resolvedColor,
                longPressDeleteEnabled = longPressEnabled,
                longPressDeleteDuration = longPressDuration,
                progressCircleEnabled = progressCircleEnabled,
                textColorResolved = textColorResolved,
                crownRotationEnabled = crownRotationEnabled,
                crownRotationReverse = crownRotationReverse,
                crownClicksPerMl = crownClicksPerMl,
                crownSyncDelay = crownSyncDelay,
                devOverlayEnabled = devOverlayEnabled,
                devOverlayR = devOverlayR,
                devOverlayG = devOverlayG,
                devOverlayB = devOverlayB,
                wearInsideCircleRemoved = insideCircleRemoved,
                wearProgressCircleThickness = progressCircleThickness,
                wearCustomThickness = customThickness,
                wearGraphPillarThickness = graphPillarThickness,
                wearGraphHorizontalPadding = graphHorizontalPadding,
                wearCustomTextSize = customTextSize,
                wearShowTomorrowDay = wearShowTomorrowDay,
                intakeMinus3 = intakeM3,
                intakeMinus2 = intakeM2,
                intakeMinus1 = intakeM1,
                intakePlus1 = intakeP1,
                intakePlus2 = intakeP2,
                goalMinus3 = goalM3,
                goalMinus2 = goalM2,
                goalMinus1 = goalM1,
                goalPlus1 = goalP1,
                goalPlus2 = goalP2,
                appLanguage = appLanguage,
                goalLineSquiggly = goalLineSquiggly,
                pastIntakesCsv = pastIntakesCsv,
                pastGoalsCsv = pastGoalsCsv,
                weeksPrior = wearGraphWeeksPrior,
                daysPrior = days,
                pastDaysToShow = wearPastDaysToShow,
                ramOptimizationEnabled = ramOptimization,
                wearSwipeMode = wearSwipeMode,
                wearGraphSwipeDir = wearGraphSwipeDir
            )
        } catch (e: Exception) {
            if (e.message?.contains("API_UNAVAILABLE") == true || e.message?.contains("17: API:") == true) {
                Log.d(TAG, "Wearable API not available, skipping sync back to wear.")
            } else {
                Log.e(TAG, "Error syncing back to wear: ${e.message}", e)
            }
        }
    }

    private fun getDailyGoalForDate(context: Context, prefs: android.content.SharedPreferences, dateStr: String): Int {
        val base = prefs.getInt("daily_goal", 2000)
        var bonus = 0
        if (prefs.getBoolean("workout_water_adjustment_enabled", false)) {
            bonus += prefs.getInt("workout_bonus_$dateStr", 0)
        }
        if (prefs.getBoolean("sleep_water_adjustment_enabled", false)) {
            bonus += prefs.getInt("sleep_bonus_$dateStr", 0)
        }
        if (prefs.getBoolean("heat_water_adjustment_enabled", false)) {
            bonus += prefs.getInt("heat_bonus_$dateStr", 0)
        }
        return base + bonus
    }

    private fun isActionProcessed(actionId: String): Boolean {
        val prefs = applicationContext.getSharedPreferences("wear_processed_actions", Context.MODE_PRIVATE)
        return prefs.contains(actionId)
    }

    private fun markActionProcessed(actionId: String) {
        val prefs = applicationContext.getSharedPreferences("wear_processed_actions", Context.MODE_PRIVATE)
        val allKeys = prefs.all.keys.toList()
        val editor = prefs.edit().putBoolean(actionId, true)
        if (allKeys.size > 100) {
            allKeys.take(50).forEach { editor.remove(it) }
        }
        editor.apply()
    }

    private fun getDateStringForOffset(context: Context, offset: Int): String {
        val prefs = context.getSharedPreferences("water_tracker_prefs", Context.MODE_PRIVATE)
        val lateNightEnabled = prefs.getBoolean("late_night_logging_enabled", true)
        val rolloverHour = prefs.getInt("late_night_rollover_hour", 3)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        if (lateNightEnabled) {
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            if (hour < rolloverHour) {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            }
        }
        calendar.add(Calendar.DAY_OF_YEAR, offset)
        return sdf.format(calendar.time)
    }

    private fun triggerTactileClickHaptic(context: Context) {
        try {
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            }
            if (vibrator != null && vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    vibrator.vibrate(android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_CLICK))
                } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(12L, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(12L)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering tactile click haptic: ${e.message}")
        }
    }

    private fun sendHapticToWearNodes(context: Context) {
        scope.launch {
            try {
                val nodes = com.google.android.gms.tasks.Tasks.await(Wearable.getNodeClient(context).connectedNodes)
                for (node in nodes) {
                    Wearable.getMessageClient(context).sendMessage(node.id, "/trigger-haptic", "CLICK".toByteArray())
                }
            } catch (e: Exception) {
                if (e.message?.contains("API_UNAVAILABLE") == true || e.message?.contains("17: API:") == true) {
                    Log.d(TAG, "Wearable API not available, skipping haptic message to Wear OS.")
                } else {
                    Log.e(TAG, "Error sending haptic message to Wear OS nodes: ${e.message}")
                }
            }
        }
    }

    companion object {
        private const val TAG = "PhoneWearListener"
    }
}
