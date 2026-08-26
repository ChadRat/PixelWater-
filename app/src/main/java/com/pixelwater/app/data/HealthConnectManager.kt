package com.pixelwater.app.data

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Volume
import java.time.Instant
import java.time.ZoneOffset
import java.util.TimeZone

object HealthConnectManager {
    private const val TAG = "HealthConnectManager"

    val permissions: Set<String>
        get() = try {
            setOf(
                HealthPermission.getWritePermission(HydrationRecord::class),
                HealthPermission.getReadPermission(HydrationRecord::class),
                HealthPermission.getReadPermission(androidx.health.connect.client.records.ExerciseSessionRecord::class),
                HealthPermission.getReadPermission(androidx.health.connect.client.records.SleepSessionRecord::class)
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to resolve Health Connect permissions: ${e.message}", e)
            emptySet()
        }

    fun isSdkAvailable(context: Context): Boolean {
        return try {
            val status = HealthConnectClient.getSdkStatus(context)
            status == HealthConnectClient.SDK_AVAILABLE
        } catch (e: Throwable) {
            Log.e(TAG, "Health Connect SDK availability check failed: ${e.message}")
            false
        }
    }

    suspend fun hasAllPermissions(context: Context): Boolean {
        if (!isSdkAvailable(context)) return false
        val currentPermissions = permissions
        if (currentPermissions.isEmpty()) return false
        return try {
            val client = HealthConnectClient.getOrCreate(context)
            val granted = client.permissionController.getGrantedPermissions()
            granted.containsAll(currentPermissions)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed checking Health Connect permissions: ${e.message}")
            false
        }
    }

        fun getExerciseTypeName(type: Int): String {
        return try {
            androidx.health.connect.client.records.ExerciseSessionRecord::class.java.declaredFields
                .firstOrNull { it.name.startsWith("EXERCISE_TYPE_") && it.type == Int::class.java && it.getInt(null) == type }
                ?.name
                ?.removePrefix("EXERCISE_TYPE_")
                ?.replace("_", " ")
                ?.lowercase(java.util.Locale.US)
                ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.US) else it.toString() }
                ?: "Workout (Type: $type)"
        } catch (e: Exception) {
            "Workout (Type: $type)"
        }
    }

    suspend fun writeHydration(context: Context, amountMl: Double, timestamp: Long): Boolean {
        if (!isSdkAvailable(context)) {
            Log.w(TAG, "Health Connect SDK is not available.")
            return false
        }
        return try {
            val client = HealthConnectClient.getOrCreate(context)
            val offsetSeconds = TimeZone.getDefault().getOffset(timestamp) / 1000
            val zoneOffset = ZoneOffset.ofTotalSeconds(offsetSeconds)
            
            val hydrationRecord = HydrationRecord(
                startTime = Instant.ofEpochMilli(timestamp),
                endTime = Instant.ofEpochMilli(timestamp + 1000),
                startZoneOffset = zoneOffset,
                endZoneOffset = zoneOffset,
                volume = Volume.milliliters(amountMl)
            )
            
            client.insertRecords(listOf(hydrationRecord))
            Log.i(TAG, "Successfully wrote hydration record to Health Connect: $amountMl ml")
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Error writing hydration to Health Connect: ${e.message}")
            false
        }
    }

    suspend fun readHydration(context: Context, startTime: Instant, endTime: Instant): List<HydrationRecord> {
        if (!isSdkAvailable(context)) {
            Log.w(TAG, "Health Connect SDK is not available.")
            return emptyList()
        }
        return try {
            val client = HealthConnectClient.getOrCreate(context)
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = HydrationRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            response.records.filter { it.metadata.dataOrigin.packageName != context.packageName }
        } catch (e: Throwable) {
            Log.e(TAG, "Error reading hydration from Health Connect: ${e.message}")
            emptyList()
        }
    }

    suspend fun readExerciseSessions(context: Context, startTime: java.time.Instant, endTime: java.time.Instant): List<androidx.health.connect.client.records.ExerciseSessionRecord> {
        if (!isSdkAvailable(context)) {
            Log.w(TAG, "Health Connect SDK is not available.")
            return emptyList()
        }
        return try {
            val client = HealthConnectClient.getOrCreate(context)
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = androidx.health.connect.client.records.ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            response.records
        } catch (e: Throwable) {
            Log.e(TAG, "Error reading exercise sessions from Health Connect: ${e.message}")
            emptyList()
        }
    }

    suspend fun readSleepSessions(context: Context, startTime: java.time.Instant, endTime: java.time.Instant): List<androidx.health.connect.client.records.SleepSessionRecord> {
        if (!isSdkAvailable(context)) {
            Log.w(TAG, "Health Connect SDK is not available.")
            return emptyList()
        }
        return try {
            val client = HealthConnectClient.getOrCreate(context)
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = androidx.health.connect.client.records.SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            response.records
        } catch (e: Throwable) {
            Log.e(TAG, "Error reading sleep sessions from Health Connect: ${e.message}")
            emptyList()
        }
    }
}
