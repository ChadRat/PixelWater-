package com.pixelwater.app.data

import kotlinx.coroutines.flow.Flow

class WaterRepository(private val waterLogDao: WaterLogDao) {
    fun getAllLogs(): Flow<List<WaterLog>> = waterLogDao.getAllLogs()

    fun getLogsForDate(date: String): Flow<List<WaterLog>> = waterLogDao.getLogsForDate(date)

    fun getTotalIntakeForDate(date: String): Flow<Int?> = waterLogDao.getTotalIntakeForDate(date)

    suspend fun getTotalIntakeForDateSync(date: String): Int? = waterLogDao.getTotalIntakeForDateSync(date)

    suspend fun insertLog(log: WaterLog) {
        waterLogDao.insertLog(log)
    }

    suspend fun deleteLog(log: WaterLog) {
        waterLogDao.deleteLog(log)
    }

    suspend fun deleteLogById(id: Int) {
        waterLogDao.deleteLogById(id)
    }

    suspend fun deleteAllLogs() {
        waterLogDao.deleteAllLogs()
    }

    suspend fun deleteLogsByType(type: String) {
        waterLogDao.deleteLogsByType(type)
    }

    suspend fun deleteLogsForDate(dateString: String) {
        waterLogDao.deleteLogsForDate(dateString)
    }

    suspend fun deleteLogsOlderThan(cutoffDateStr: String, cutoffTimestamp: Long): Int =
        waterLogDao.deleteLogsOlderThan(cutoffDateStr, cutoffTimestamp)

    suspend fun deleteLogsRecent30Days(cutoffDateStr: String, cutoffTimestamp: Long): Int =
        waterLogDao.deleteLogsRecent30Days(cutoffDateStr, cutoffTimestamp)

    suspend fun deleteLogsNotInDates(keepDates: List<String>): Int {
        return if (keepDates.isEmpty()) {
            deleteAllLogs()
            0
        } else {
            waterLogDao.deleteLogsNotInDates(keepDates)
        }
    }
}
