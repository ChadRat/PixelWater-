package com.pixelwater.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.pixelwater.app.data.WaterDatabase
import com.pixelwater.app.data.WaterLog
import com.pixelwater.app.data.WaterRepository
import com.pixelwater.app.notifications.NotificationHelper
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DevNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        Log.d("DevNotificationReceiver", "Action received: $action")

        val prefs = context.getSharedPreferences("water_tracker_prefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("dev_quick_add_notification_enabled", false)
        if (!isEnabled) {
            NotificationHelper.cancelDevQuickAddNotification(context)
            return
        }

        when (action) {
            "COM_PIXELWATER_APP_DEV_QUICK_ADD_250" -> {
                addWaterInBackground(context, 250)
            }
            "COM_PIXELWATER_APP_DEV_QUICK_ADD_500" -> {
                addWaterInBackground(context, 500)
            }
            "COM_PIXELWATER_APP_DEV_NOTIFICATION_DISMISSED" -> {
                Log.d("DevNotificationReceiver", "Notification swiped/dismissed, instantly reappearing...")
                NotificationHelper.showDevQuickAddNotification(context)
            }
        }
    }

    private fun addWaterInBackground(context: Context, amountMl: Int) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = WaterDatabase.getDatabase(context)
                val repository = WaterRepository(db.waterLogDao())
                val prefs = context.getSharedPreferences("water_tracker_prefs", Context.MODE_PRIVATE)

                val lateNightLoggingEnabled = prefs.getBoolean("late_night_logging_enabled", true)
                val lateNightRolloverHour = prefs.getInt("late_night_rollover_hour", 4)
                
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val calendar = Calendar.getInstance()
                if (lateNightLoggingEnabled) {
                    val hour = calendar.get(Calendar.HOUR_OF_DAY)
                    if (hour < lateNightRolloverHour) {
                        calendar.add(Calendar.DAY_OF_YEAR, -1)
                    }
                }
                val dateString = sdf.format(calendar.time)

                val newLog = WaterLog(
                    amountMl = amountMl,
                    beverageType = "Water",
                    waterEquivalency = 1.0f,
                    waterEquivalentMl = amountMl,
                    dateString = dateString,
                    timestamp = System.currentTimeMillis()
                )
                repository.insertLog(newLog)

                // Sync with Google Health Connect if authorized
                try {
                    if (com.pixelwater.app.data.HealthConnectManager.hasAllPermissions(context)) {
                        com.pixelwater.app.data.HealthConnectManager.writeHydration(context, amountMl.toDouble(), newLog.timestamp)
                    }
                } catch (e: Exception) {
                    Log.e("DevNotificationReceiver", "Error syncing with Health Connect from background", e)
                }

                // Update widgets via helper
                try {
                    com.pixelwater.app.widget.WaterTrackerWidget().updateAll(context)
                } catch (e: Exception) {
                    Log.e("DevNotificationReceiver", "Error updating widget from background", e)
                }

                Log.d("DevNotificationReceiver", "Successfully logged $amountMl ml water from notification!")
            } catch (e: Exception) {
                Log.e("DevNotificationReceiver", "Failed to insert log from notification: ${e.message}", e)
            } finally {
                // Re-show / refresh the notification so it instantly reappears!
                NotificationHelper.showDevQuickAddNotification(context)
                pendingResult.finish()
            }
        }
    }
}
