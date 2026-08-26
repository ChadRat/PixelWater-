package com.pixelwater.app.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.content.Context
import android.widget.Toast
import com.pixelwater.app.data.WaterDatabase
import com.pixelwater.app.data.WaterLog
import com.pixelwater.app.widget.WaterTrackerWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.drawable.Icon
import com.pixelwater.app.R

class WaterQuickAddTileService : TileService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onStopListening() {
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val context = applicationContext
        val prefs = context.getSharedPreferences("water_tracker_prefs", Context.MODE_PRIVATE)
        val amount = prefs.getInt("tile_quick_add_amount", 250)
        val appLanguage = prefs.getString("app_language", "en") ?: "en"

        // Calculate precise date matching late rollover
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        if (prefs.getBoolean("late_night_logging_enabled", true)) {
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val rolloverHour = prefs.getInt("late_night_rollover_hour", 4)
            if (hour < rolloverHour) {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            }
        }
        val dateString = sdf.format(calendar.time)

        val db = WaterDatabase.getDatabase(context)
        val waterLogDao = db.waterLogDao()

        val log = WaterLog(
            amountMl = amount,
            timestamp = System.currentTimeMillis(),
            dateString = dateString,
            beverageType = "Water",
            waterEquivalency = 1.0f,
            waterEquivalentMl = amount
        )

        serviceScope.launch {
            try {
                waterLogDao.insertLog(log)

                // Force update on home widget
                try {
                    WaterTrackerWidget().updateAll(context)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Show toast
                val msg = if (appLanguage == "el") {
                    "Προστέθηκαν ${amount}ml!"
                } else {
                    "Added ${amount}ml!"
                }
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

                // Instantly update tile
                updateTileState()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val context = applicationContext
        val prefs = context.getSharedPreferences("water_tracker_prefs", Context.MODE_PRIVATE)

        // Read preferences
        val appLanguage = prefs.getString("app_language", "en") ?: "en"
        val displayMode = prefs.getString("tile_display_mode", "remaining") ?: "remaining"
        val iconType = prefs.getString("tile_icon_type", "stars") ?: "stars"
        val quickAddAmount = prefs.getInt("tile_quick_add_amount", 250)

        // Calculate date string
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        if (prefs.getBoolean("late_night_logging_enabled", true)) {
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val rolloverHour = prefs.getInt("late_night_rollover_hour", 4)
            if (hour < rolloverHour) {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            }
        }
        val dateString = sdf.format(calendar.time)

        serviceScope.launch {
            try {
                val db = WaterDatabase.getDatabase(context)
                val totalIntake = db.waterLogDao().getTotalIntakeForDateSync(dateString) ?: 0

                // Get base goal
                val baseGoal = prefs.getInt("daily_goal", 2000)
                var bonus = 0
                if (prefs.getBoolean("workout_water_adjustment_enabled", false)) {
                    bonus += prefs.getInt("workout_bonus_$dateString", 0)
                }
                if (prefs.getBoolean("sleep_water_adjustment_enabled", true)) {
                    bonus += prefs.getInt("sleep_bonus_$dateString", 0)
                }
                if (prefs.getBoolean("heat_water_adjustment_enabled", true)) {
                    bonus += prefs.getInt("heat_bonus_$dateString", 0)
                }
                val goal = baseGoal + bonus

                // Format text for remaining/added
                val label = "+${quickAddAmount}"

                val subtitle = if (displayMode == "remaining") {
                    val remaining = (goal - totalIntake).coerceAtLeast(0)
                    "${remaining}"
                } else {
                    "${totalIntake}"
                }

                // Set icon
                val iconRes = when (iconType) {
                    "droplet" -> R.drawable.ic_tile_app_icon
                    "actual_app_icon" -> R.drawable.ic_tile_actual_app_icon
                    else -> R.drawable.ic_tile_stars
                }
                
                tile.label = label
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    tile.subtitle = subtitle
                } else {
                    tile.label = "$label ($subtitle)"
                }
                tile.icon = Icon.createWithResource(context, iconRes)
                tile.state = Tile.STATE_ACTIVE
                tile.updateTile()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
