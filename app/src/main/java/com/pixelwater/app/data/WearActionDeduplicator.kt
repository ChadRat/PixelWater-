package com.pixelwater.app.data

import android.content.Context
import android.util.Log
import java.util.Collections

object WearActionDeduplicator {
    private const val TAG = "WearDeduplicator"
    private const val PREFS_NAME = "wear_action_dedup_prefs"
    private val processedSet = Collections.synchronizedSet(HashSet<String>())

    @Synchronized
    fun isProcessedAndMark(context: Context, actionId: String): Boolean {
        if (actionId.isBlank()) return false

        if (processedSet.contains(actionId)) {
            Log.d(TAG, "Action $actionId found in memory cache - skipping duplicate")
            return true
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.contains(actionId)) {
            processedSet.add(actionId)
            Log.d(TAG, "Action $actionId found in prefs - skipping duplicate")
            return true
        }

        processedSet.add(actionId)
        val allKeys = prefs.all.keys.toList()
        val editor = prefs.edit().putBoolean(actionId, true)
        if (allKeys.size > 200) {
            allKeys.take(100).forEach { editor.remove(it) }
        }
        editor.apply()
        return false
    }
}
