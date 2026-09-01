package com.masjid.display.data.repository

import com.masjid.display.data.local.dao.PrayerScheduleDao
import com.masjid.display.data.local.entity.PrayerSchedule

class PrayerScheduleRepository(private val dao: PrayerScheduleDao) {

    suspend fun getByDate(date: String): PrayerSchedule? = dao.getByDate(date)

    suspend fun getLastCachedDate(): String? = dao.getLastCachedDate()

    suspend fun countFrom(fromDate: String): Int = dao.countFrom(fromDate)

    suspend fun insertAll(schedules: List<PrayerSchedule>) = dao.insertAll(schedules)

    suspend fun pruneOlderThan(beforeDate: String) = dao.deleteOlderThan(beforeDate)

    suspend fun clearAll() = dao.deleteAll()
}
