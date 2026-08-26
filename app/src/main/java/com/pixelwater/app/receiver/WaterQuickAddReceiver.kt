package com.pixelwater.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import android.widget.Toast
import com.pixelwater.app.data.WaterDatabase
import com.pixelwater.app.data.WaterLog
import com.pixelwater.app.notifications.NotificationHelper
import com.pixelwater.app.widget.WaterTrackerWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WaterQuickAddReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // Dismiss the active notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NotificationHelper.NOTIFICATION_ID)

        val prefs = context.getSharedPreferences("water_tracker_prefs", Context.MODE_PRIVATE)
        val amount = prefs.getInt("quick_add_amount", 250)
        val appLanguage = prefs.getString("app_language", "en") ?: "en"

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateString = sdf.format(Date())

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

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                waterLogDao.insertLog(log)

                // Force update on home widget
                try {
                    WaterTrackerWidget().updateAll(context)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                withContext(Dispatchers.Main) {
                    val msg = if (appLanguage == "el") {
                        "Προστέθηκαν ${amount}ml!"
                    } else {
                        "Added ${amount}ml!"
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    pendingResult.finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                pendingResult.finish()
            }
        }
    }
}
