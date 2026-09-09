package com.pixelwater.app.notifications

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.pixelwater.app.MainActivity
import com.pixelwater.app.receiver.WaterReminderReceiver
import java.util.Calendar

object NotificationHelper {
    private const val CHANNEL_ID = "Πιές Νερό"
    private const val CHANNEL_NAME = "Πιές Νερό"
    private const val CHANNEL_DESC = "ειδοποιήσεις για να σου θυμήσουν να πιείς νερό"
    const val NOTIFICATION_ID = 1001
    const val ALARM_REQUEST_CODE = 2002

    fun getActiveChannelId(context: Context): String {
        val prefs = context.getSharedPreferences("water_tracker_prefs", Context.MODE_PRIVATE)
        val bannerEnabled = prefs.getBoolean("notification_banner_enabled", false)
        val bypassDnd = prefs.getBoolean("notification_bypass_dnd", false)
        val soundMode = prefs.getString("notification_sound_mode", "device") ?: "device"
        val isSilent = prefs.getString("notification_default_silent", "DEFAULT") == "SILENT"
        
        return "water_reminder_ch_b${bannerEnabled}_d${bypassDnd}_s${soundMode}_silent${isSilent}"
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val prefs = context.getSharedPreferences("water_tracker_prefs", Context.MODE_PRIVATE)
            val bannerEnabled = prefs.getBoolean("notification_banner_enabled", false)
            val bypassDnd = prefs.getBoolean("notification_bypass_dnd", false)
            val soundMode = prefs.getString("notification_sound_mode", "device") ?: "device"
            val isSilent = prefs.getString("notification_default_silent", "DEFAULT") == "SILENT"
            val vibrationEnabled = prefs.getBoolean("notification_vibration_enabled", true)
            
            val channelId = getActiveChannelId(context)
            val channelName = if (prefs.getString("app_language", "en") == "el") "Πιές Νερό" else "Drink Water"
            
            // Delete other water reminder channels to avoid clutter in Android Settings
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            try {
                notificationManager.notificationChannels.forEach { existingChannel ->
                    if (existingChannel.id.startsWith("water_reminder_ch_") && existingChannel.id != channelId) {
                        notificationManager.deleteNotificationChannel(existingChannel.id)
                    }
                }
            } catch (e: Exception) {
                Log.e("NotificationHelper", "Error tidying up channels: ${e.message}")
            }
            
            val importance = when {
                bannerEnabled -> NotificationManager.IMPORTANCE_HIGH
                else -> NotificationManager.IMPORTANCE_DEFAULT
            }
            
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = CHANNEL_DESC
                
                // Do Not Disturb Bypass
                setBypassDnd(bypassDnd)
                
                // Vibration
                enableVibration(vibrationEnabled)
                if (!vibrationEnabled) {
                    vibrationPattern = null
                }
                
                // Sound configuration
                if (isSilent) {
                    setSound(null, null)
                } else {
                    when (soundMode) {
                        "droplet" -> {
                            val soundFile = SoundGenerator.generateWaterDropletWav(context)
                            try {
                                val soundUri = androidx.core.content.FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    soundFile
                                )
                                // Grant Uri permissions so system_server can play it
                                context.grantUriPermission("com.android.systemui", soundUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                context.grantUriPermission("android", soundUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                
                                val audioAttributes = android.media.AudioAttributes.Builder()
                                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .build()
                                setSound(soundUri, audioAttributes)
                            } catch (e: Exception) {
                                Log.e("NotificationHelper", "Failed to set custom droplet sound: ${e.message}")
                            }
                        }
                        "ai" -> {
                            val soundFile = java.io.File(context.filesDir, "ai_notification.wav")
                            if (soundFile.exists()) {
                                try {
                                    val soundUri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.provider",
                                        soundFile
                                    )
                                    context.grantUriPermission("com.android.systemui", soundUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    context.grantUriPermission("android", soundUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    
                                    val audioAttributes = android.media.AudioAttributes.Builder()
                                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                        .build()
                                    setSound(soundUri, audioAttributes)
                                } catch (e: Exception) {
                                    Log.e("NotificationHelper", "Failed to set custom AI sound: ${e.message}")
                                }
                            }
                        }
                        else -> {
                            if (soundMode.startsWith("ai_notification_saved_")) {
                                val soundFile = java.io.File(context.filesDir, soundMode)
                                if (soundFile.exists()) {
                                    try {
                                        val soundUri = androidx.core.content.FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.provider",
                                            soundFile
                                        )
                                        context.grantUriPermission("com.android.systemui", soundUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        context.grantUriPermission("android", soundUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        
                                        val audioAttributes = android.media.AudioAttributes.Builder()
                                            .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                            .build()
                                        setSound(soundUri, audioAttributes)
                                    } catch (e: Exception) {
                                        Log.e("NotificationHelper", "Failed to set custom saved AI sound: ${e.message}")
                                    }
                                }
                            }
                            // Default Device notification sound (leave default as-is)
                        }
                    }
                }
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    fun showReminderNotification(context: Context) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prefs = context.getSharedPreferences("water_tracker_prefs", Context.MODE_PRIVATE)
        val appLanguage = prefs.getString("app_language", "en") ?: "en"
        val notificationSourceMode = prefs.getString("notification_source_mode", "DEFAULT") ?: "DEFAULT"
        val mixDefaultEnabled = prefs.getBoolean("mix_default_enabled", true)
        val mixUserEnabled = prefs.getBoolean("mix_user_enabled", true)
        val mixAiEnabled = prefs.getBoolean("mix_ai_enabled", true)

        val userManualStr = prefs.getString("user_manual_reminders", null)
        val aiCoachStr = prefs.getString("ai_coach_reminders", null)

        val userList = mutableListOf<String>()
        if (!userManualStr.isNullOrEmpty()) {
            try {
                val array = org.json.JSONArray(userManualStr)
                for (i in 0 until array.length()) {
                    val m = array.getString(i).trim()
                    if (m.isNotEmpty()) userList.add(m)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        val aiList = mutableListOf<String>()
        if (!aiCoachStr.isNullOrEmpty()) {
            try {
                val array = org.json.JSONArray(aiCoachStr)
                for (i in 0 until array.length()) {
                    val m = array.getString(i).trim()
                    if (m.isNotEmpty()) aiList.add(m)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }

        val defaultEn = listOf(
            "drink water ^_^",
            "time for water ◉‿◉",
            "Waterrrrr ᕙ( • ‿ • )ᕗ",
            "Water? ( ◜‿◝ )♡",
            "Drink water NOW <(￣︶￣)>",
            "WATER WATER WATER WATER (＾∇＾)ﾉ♪(‾▿‾)"
        )
        val defaultEl = listOf(
            "πιες νερό ^_^",
            "ώρα για νερό ◉‿◉",
            "Νεροοοοοο ᕙ( • ‿ • )ᕗ",
            "Νεράκι? ( ◜‿◝ )♡",
            "Πιες νερό ΤΩΡΑ <(￣︶￣)>",
            "ΝΕΡΟ ΝΕΡΟ ΝΕΡΟ ΝΕΡΟ (＾∇＾)ﾉ♪(‾▿‾)"
        )
        val defaultList = if (appLanguage == "el") defaultEl else defaultEn

        val possibleMessages = mutableListOf<String>()

        when (notificationSourceMode) {
            "DEFAULT" -> {
                possibleMessages.addAll(defaultList)
            }
            "USER" -> {
                if (userList.isNotEmpty()) {
                    possibleMessages.addAll(userList)
                } else {
                    possibleMessages.addAll(defaultList)
                }
            }
            "AI" -> {
                if (aiList.isNotEmpty()) {
                    possibleMessages.addAll(aiList)
                } else {
                    possibleMessages.addAll(defaultList)
                }
            }
            "MIX" -> {
                if (mixDefaultEnabled) {
                    possibleMessages.addAll(defaultList)
                }
                if (mixUserEnabled && userList.isNotEmpty()) {
                    possibleMessages.addAll(userList)
                }
                if (mixAiEnabled && aiList.isNotEmpty()) {
                    possibleMessages.addAll(aiList)
                }
                if (possibleMessages.isEmpty()) {
                    possibleMessages.addAll(defaultList)
                }
            }
            else -> possibleMessages.addAll(defaultList)
        }

        var message = if (possibleMessages.isNotEmpty()) possibleMessages.random() else ""
        if (message.isEmpty()) {
            message = if (appLanguage == "el") "Ώρα να πιεις λίγο νερό!" else "Time to drink some water!"
        }
        
        // Remove water droplets emoji from the message if any exist
        message = message.replace("💧", "").trim()

        val title = "Pixel Water"

        val quickAmount = prefs.getInt("quick_add_amount", 250)
        val quickLabel = if (appLanguage == "el") "Προσθήκη ${quickAmount}ml" else "Add ${quickAmount}ml"

        val quickAddIntent = Intent(context, com.pixelwater.app.receiver.WaterQuickAddReceiver::class.java)
        val quickAddPendingIntent = PendingIntent.getBroadcast(
            context,
            54321,
            quickAddIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = getActiveChannelId(context)
        val isSilent = prefs.getString("notification_default_silent", "DEFAULT") == "SILENT"
        val bannerEnabled = prefs.getBoolean("notification_banner_enabled", false)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(com.pixelwater.app.R.drawable.ic_notification_water_drop)
            .setContentText(message)
            .setPriority(when {
                isSilent -> NotificationCompat.PRIORITY_LOW
                bannerEnabled -> NotificationCompat.PRIORITY_HIGH
                else -> NotificationCompat.PRIORITY_DEFAULT
            })
            .setContentIntent(pendingIntent)
            .addAction(com.pixelwater.app.R.drawable.ic_notification_water_drop, quickLabel, quickAddPendingIntent)
            .setAutoCancel(true)
            .setLocalOnly(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val dndNoSound = prefs.getBoolean("notification_dnd_no_sound", false)
        if (dndNoSound) {
            val isDndActive = notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
            if (isDndActive) {
                builder.setSilent(true)
            }
        }

        val destination = prefs.getString("reminders_destination", "BOTH") ?: "BOTH"

        if (destination == "WATCH" || destination == "BOTH") {
            Thread {
                try {
                    val nodes = com.google.android.gms.tasks.Tasks.await(com.google.android.gms.wearable.Wearable.getNodeClient(context).connectedNodes)
                    for (node in nodes) {
                        com.google.android.gms.wearable.Wearable.getMessageClient(context).sendMessage(node.id, "/show-water-reminder", message.toByteArray())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        }

        if (destination == "PHONE" || destination == "BOTH") {
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        }
    }

    fun scheduleNextReminder(context: Context) {
        val prefs = context.getSharedPreferences("water_tracker_prefs", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("reminders_enabled", true)
        if (!enabled) {
            cancelAlarms(context)
            return
        }

        val intervalHours = prefs.getInt("reminder_interval", 2)
        val startHour = prefs.getInt("reminder_start_hour", 8)
        val endHour = prefs.getInt("reminder_end_hour", 22)

        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

        val triggerTimeMs: Long
        if (currentHour < startHour) {
            // Schedule for start hour today
            calendar.set(Calendar.HOUR_OF_DAY, startHour)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            triggerTimeMs = calendar.timeInMillis
        } else if (currentHour >= endHour) {
            // Schedule for start hour tomorrow
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            calendar.set(Calendar.HOUR_OF_DAY, startHour)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            triggerTimeMs = calendar.timeInMillis
        } else {
            // Schedule for current_time + interval_hours
            calendar.add(Calendar.HOUR_OF_DAY, intervalHours)
            val scheduledHour = calendar.get(Calendar.HOUR_OF_DAY)
            if (scheduledHour >= endHour) {
                // If interval slips into sleep hours, schedule for tomorrow start hour
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, startHour)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
            }
            triggerTimeMs = calendar.timeInMillis
        }

        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            if (alarmManager == null) {
                Log.e("NotificationHelper", "AlarmManager service is null")
                return
            }
            val intent = Intent(context, WaterReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Cancel previous one first to avoid duplicates
            alarmManager.cancel(pendingIntent)

            // Schedule inexact repeating or just a single alarm that will self-reschedule on receive.
            // Single schedules are much more precise and resilient to various versions of Android background limits.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            }
            Log.d("NotificationHelper", "Reminder scheduled at: ${calendar.time}")
        } catch (e: Throwable) {
            Log.e("NotificationHelper", "Failed to schedule or cancel alarm", e)
        }
    }

    fun cancelAlarms(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            if (alarmManager == null) {
                Log.e("NotificationHelper", "AlarmManager service is null during cancel")
                return
            }
            val intent = Intent(context, WaterReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            Log.d("NotificationHelper", "Reminders cancelled")
        } catch (e: Throwable) {
            Log.e("NotificationHelper", "Failed to cancel alarms", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun showCustomNotification(context: Context, title: String, content: String) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1234,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = getActiveChannelId(context)
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(com.pixelwater.app.R.drawable.ic_notification_water_drop)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val prefs = context.getSharedPreferences("water_tracker_prefs", Context.MODE_PRIVATE)
        val dndNoSound = prefs.getBoolean("notification_dnd_no_sound", false)
        if (dndNoSound) {
            val isDndActive = notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
            if (isDndActive) {
                builder.setSilent(true)
            }
        }

        notificationManager.notify(NOTIFICATION_ID + 10, builder.build())
    }
}
