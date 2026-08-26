package com.pixelwater.app.data

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object WearDataSyncManager {
    private const val TAG = "WearDataSyncManager"
    private const val PATH_STATUS = "/water_status"
    private const val PATH_ADD_WATER = "/add-water"
    private const val PATH_DELETE_WATER = "/delete-water"

    private val scope = CoroutineScope(Dispatchers.IO)

    fun syncStatus(
        context: Context,
        totalIntakeMl: Int,
        goalMl: Int,
        quickAddAmount: Int,
        showRemaining: Boolean = false,
        hapticStrength: String = "MEDIUM",
        themeColor: String = "MINT",
        circleSize: Int = -1,
        resolvedColor: Int = 0xFF00BFA5.toInt(),
        longPressDeleteEnabled: Boolean = true,
        longPressDeleteDuration: Int = 1000,
        progressCircleEnabled: Boolean = true,
        textColorResolved: Int = 0xFFFFFFFF.toInt(),
        crownRotationEnabled: Boolean = true,
        crownRotationReverse: Boolean = false,
        crownClicksPerMl: Int = 10,
        crownSyncDelay: Int = 250,
        devOverlayEnabled: Boolean = false,
        devOverlayR: Int = 0,
        devOverlayG: Int = 255,
        devOverlayB: Int = 0,
        wearInsideCircleRemoved: Boolean = false,
        wearProgressCircleThickness: String = "THIN",
        wearCustomThickness: Float = 13f,
        wearGraphPillarThickness: Float = 24f,
        wearGraphHorizontalPadding: Float = 18f,
        wearCustomTextSize: Float = 32f,
        wearShowTomorrowDay: Boolean = true,
        intakeMinus3: Int = 0,
        intakeMinus2: Int = 0,
        intakeMinus1: Int = 0,
        intakePlus1: Int = 0,
        intakePlus2: Int = 0,
        goalMinus3: Int = 2000,
        goalMinus2: Int = 2000,
        goalMinus1: Int = 2000,
        goalPlus1: Int = 2000,
        goalPlus2: Int = 2000,
        appLanguage: String = "en",
        goalLineSquiggly: Boolean = false,
        pastIntakesCsv: String = "",
        pastGoalsCsv: String = "",
        weeksPrior: Int = 1,
        daysPrior: Int = 5,
        pastDaysToShow: Int = 3,
        ramOptimizationEnabled: Boolean = false,
        wearSwipeMode: String = "VERTICAL",
        wearGraphSwipeDir: String = "LEFT",
        complicationIconStyle: String = "DROPLET"
    ) {
        scope.launch {
            try {
                val dataMapRequest = PutDataMapRequest.create(PATH_STATUS).apply {
                    dataMap.putString("complication_icon_style", complicationIconStyle)
                    dataMap.putInt("total_intake", totalIntakeMl)
                    dataMap.putInt("goal", goalMl)
                    dataMap.putInt("quick_add", quickAddAmount)
                    dataMap.putBoolean("show_remaining", showRemaining)
                    dataMap.putString("haptic_strength", hapticStrength)
                    dataMap.putString("theme_color", themeColor)
                    dataMap.putInt("circle_size", circleSize)
                    dataMap.putInt("theme_color_resolved", resolvedColor)
                    dataMap.putBoolean("long_press_delete_enabled", longPressDeleteEnabled)
                    dataMap.putInt("long_press_delete_duration", longPressDeleteDuration)
                    dataMap.putBoolean("progress_circle_enabled", progressCircleEnabled)
                    dataMap.putInt("text_color_resolved", textColorResolved)
                    dataMap.putBoolean("crown_rotation_enabled", crownRotationEnabled)
                    dataMap.putBoolean("crown_rotation_reverse", crownRotationReverse)
                    dataMap.putInt("crown_clicks_per_ml", crownClicksPerMl)
                    dataMap.putInt("crown_sync_delay", crownSyncDelay)
                    dataMap.putBoolean("dev_overlay_enabled", devOverlayEnabled)
                    dataMap.putInt("dev_overlay_r", devOverlayR)
                    dataMap.putInt("dev_overlay_g", devOverlayG)
                    dataMap.putInt("dev_overlay_b", devOverlayB)
                    dataMap.putBoolean("remove_inside_circle", wearInsideCircleRemoved)
                    dataMap.putString("progress_circle_thickness", wearProgressCircleThickness)
                    dataMap.putFloat("custom_thickness", wearCustomThickness)
                    dataMap.putFloat("graph_pillar_thickness", wearGraphPillarThickness)
                    dataMap.putFloat("graph_horizontal_padding", wearGraphHorizontalPadding)
                    dataMap.putFloat("custom_text_size", wearCustomTextSize)
                    dataMap.putBoolean("wear_show_tomorrow_day", wearShowTomorrowDay)
                    dataMap.putBoolean("ram_optimization_enabled", ramOptimizationEnabled)
                    
                    dataMap.putInt("intake_minus_3", intakeMinus3)
                    dataMap.putInt("intake_minus_2", intakeMinus2)
                    dataMap.putInt("intake_minus_1", intakeMinus1)
                    dataMap.putInt("intake_plus_1", intakePlus1)
                    dataMap.putInt("intake_plus_2", intakePlus2)
                    dataMap.putInt("goal_minus_3", goalMinus3)
                    dataMap.putInt("goal_minus_2", goalMinus2)
                    dataMap.putInt("goal_minus_1", goalMinus1)
                    dataMap.putInt("goal_plus_1", goalPlus1)
                    dataMap.putInt("goal_plus_2", goalPlus2)
                    dataMap.putString("app_language", appLanguage)
                    dataMap.putBoolean("goal_line_squiggly", goalLineSquiggly)
                    dataMap.putString("past_intakes_csv", pastIntakesCsv)
                    dataMap.putString("past_goals_csv", pastGoalsCsv)
                    dataMap.putInt("weeks_prior", weeksPrior)
                    dataMap.putInt("days_prior", daysPrior)
                    dataMap.putInt("past_days_to_show", pastDaysToShow)
                    dataMap.putString("swipe_mode", wearSwipeMode)
                    dataMap.putString("graph_swipe_dir", wearGraphSwipeDir)
                    
                    dataMap.putLong("timestamp", System.currentTimeMillis())
                }
                val putDataReq = dataMapRequest.asPutDataRequest()
                putDataReq.setUrgent()
                Wearable.getDataClient(context).putDataItem(putDataReq)
                    .addOnSuccessListener {
                        Log.d(TAG, "Successfully synced to Wear OS: intake=$totalIntakeMl, goal=$goalMl, quickAdd=$quickAddAmount, showRemaining=$showRemaining, hapticStrength=$hapticStrength, themeColor=$themeColor, resolvedColor=$resolvedColor")
                    }
                    .addOnFailureListener { e ->
                        if (e.message?.contains("API_UNAVAILABLE") == true || e.message?.contains("17: API:") == true) {
                            Log.d(TAG, "Wearable API not available, skipping sync.")
                        } else {
                            Log.e(TAG, "Failed to sync to Wear OS: ${e.message}")
                        }
                    }
            } catch (e: Exception) {
                if (e.message?.contains("API_UNAVAILABLE") == true || e.message?.contains("17: API:") == true) {
                    Log.d(TAG, "Wearable API not available, skipping sync.")
                } else {
                    Log.e(TAG, "Exception during Wear OS sync: ${e.message}")
                }
            }
        }
    }

    fun registerMessageListener(
        context: Context,
        onAddWater: (Int, Int) -> Unit,
        onDeleteWater: (Int) -> Unit
    ) {
        Wearable.getMessageClient(context).addListener { event ->
            if (event.path == PATH_ADD_WATER) {
                try {
                    val payload = String(event.data)
                    val parts = payload.split(",")
                    val amount = parts[0].toIntOrNull() ?: 250
                    val offset = if (parts.size > 1) parts[1].toIntOrNull() ?: 0 else 0
                    val actionId = if (parts.size > 2) parts[2] else ""

                    if (actionId.isNotEmpty() && WearActionDeduplicator.isProcessedAndMark(context, actionId)) {
                        Log.d(TAG, "WearDataSyncManager: actionId $actionId already processed, skipping")
                        return@addListener
                    }

                    Log.d(TAG, "Received add-water message from Wear OS: $amount ml, offset $offset, actionId $actionId")
                    onAddWater(amount, offset)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to process add-water message: ${e.message}")
                }
            } else if (event.path == PATH_DELETE_WATER) {
                try {
                    val payload = String(event.data)
                    val parts = payload.split(",")
                    val offset = parts[0].toIntOrNull() ?: 0
                    val actionId = if (parts.size > 1) parts[1] else ""

                    if (actionId.isNotEmpty() && WearActionDeduplicator.isProcessedAndMark(context, actionId)) {
                        Log.d(TAG, "WearDataSyncManager: actionId $actionId already processed, skipping")
                        return@addListener
                    }

                    Log.d(TAG, "Received delete-water message from Wear OS for offset $offset, actionId $actionId")
                    onDeleteWater(offset)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to process delete-water message: ${e.message}")
                }
            }
        }
    }
}
