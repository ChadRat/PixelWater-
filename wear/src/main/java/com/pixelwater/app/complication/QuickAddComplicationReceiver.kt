package com.pixelwater.app.complication

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.pixelwater.app.tile.WaterGraphTileService
import com.pixelwater.app.tile.WaterProgressTileService
import java.util.UUID

class QuickAddComplicationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_QUICK_ADD) {
            val prefs = context.getSharedPreferences("wear_prefs", Context.MODE_PRIVATE)
            val quickAdd = prefs.getInt("quick_add", 250)
            val currentIntake = prefs.getInt("total_intake", 0)
            val newIntake = currentIntake + quickAdd

            prefs.edit().putInt("total_intake", newIntake).apply()
            Log.d(TAG, "QuickAddComplicationReceiver: Added $quickAdd ml. New total intake: $newIntake ml")

            // Haptic vibration
            try {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(50)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Vibration error: ${e.message}")
            }

            // Send add-water to phone via MessageClient & DataClient
            sendAddWaterToPhone(context, quickAdd, 0)

            // Request updates for Complications and Tiles
            updateComplicationsAndTiles(context)
        }
    }

    private fun sendAddWaterToPhone(context: Context, amount: Int, offset: Int) {
        val actionId = UUID.randomUUID().toString()
        val payload = "$amount,$offset,$actionId".toByteArray()

        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                for (node in nodes) {
                    Wearable.getMessageClient(context).sendMessage(node.id, "/add-water", payload)
                        .addOnSuccessListener {
                            Log.d(TAG, "Sent /add-water from complication to node ${node.displayName}")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed sending /add-water from complication: ${e.message}")
                        }
                }
            }

        try {
            val dataMapRequest = PutDataMapRequest.create("/wear_action").apply {
                dataMap.putString("action", "ADD")
                dataMap.putInt("amount", amount)
                dataMap.putInt("offset", offset)
                dataMap.putLong("timestamp", System.currentTimeMillis())
                dataMap.putString("action_id", actionId)
            }
            Wearable.getDataClient(context).putDataItem(dataMapRequest.asPutDataRequest().setUrgent())
        } catch (e: Exception) {
            Log.e(TAG, "Error putting wear_action data map: ${e.message}")
        }
    }

    companion object {
        const val TAG = "QuickAddCompReceiver"
        const val ACTION_QUICK_ADD = "com.pixelwater.app.ACTION_QUICK_ADD_WATER"
        const val EXTRA_AMOUNT = "extra_amount"

        fun updateComplicationsAndTiles(context: Context) {
            try {
                val quickAddComp = ComponentName(context, QuickAddComplicationService::class.java)
                ComplicationDataSourceUpdateRequester.create(context, quickAddComp).requestUpdateAll()

                val progressComp = ComponentName(context, WaterProgressComplicationService::class.java)
                ComplicationDataSourceUpdateRequester.create(context, progressComp).requestUpdateAll()
            } catch (e: Exception) {
                Log.e(TAG, "Failed updating complications: ${e.message}")
            }

            try {
                TileService.getUpdater(context).requestUpdate(WaterProgressTileService::class.java)
                TileService.getUpdater(context).requestUpdate(WaterGraphTileService::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "Failed updating tiles: ${e.message}")
            }
        }
    }
}
