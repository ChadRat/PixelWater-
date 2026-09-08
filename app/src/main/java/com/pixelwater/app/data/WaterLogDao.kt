package com.pixelwater.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterLogDao {
    @Query("SELECT * FROM water_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<WaterLog>>

    @Query("SELECT * FROM water_logs WHERE dateString = :date ORDER BY timestamp DESC")
    fun getLogsForDate(date: String): Flow<List<WaterLog>>

    @Query("SELECT * FROM water_logs WHERE dateString = :date ORDER BY timestamp DESC")
    suspend fun getLogsForDateSync(date: String): List<WaterLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: WaterLog)

    @Delete
    suspend fun deleteLog(log: WaterLog)

    @Query("DELETE FROM water_logs WHERE id = :id")
    suspend fun deleteLogById(id: Int)

    @Query("SELECT SUM(waterEquivalentMl) FROM water_logs WHERE dateString = :dateString")
    fun getTotalIntakeForDate(dateString: String): Flow<Int?>

    @Query("SELECT SUM(waterEquivalentMl) FROM water_logs WHERE dateString = :dateString")
    suspend fun getTotalIntakeForDateSync(dateString: String): Int?

    @Query("DELETE FROM water_logs")
    suspend fun deleteAllLogs()

    @Query("DELETE FROM water_logs WHERE beverageType = :type")
    suspend fun deleteLogsByType(type: String)

    @Query("DELETE FROM water_logs WHERE dateString = :dateString")
    suspend fun deleteLogsForDate(dateString: String)

    @Query("DELETE FROM water_logs WHERE dateString < :cutoffDateStr OR timestamp < :cutoffTimestamp")
    suspend fun deleteLogsOlderThan(cutoffDateStr: String, cutoffTimestamp: Long): Int

    @Query("DELETE FROM water_logs WHERE dateString >= :cutoffDateStr OR timestamp >= :cutoffTimestamp")
    suspend fun deleteLogsRecent30Days(cutoffDateStr: String, cutoffTimestamp: Long): Int
}
