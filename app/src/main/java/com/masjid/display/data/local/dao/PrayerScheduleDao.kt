package com.masjid.display.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.masjid.display.data.local.entity.PrayerSchedule

@Dao
interface PrayerScheduleDao {

    @Query("SELECT * FROM prayer_schedule WHERE date = :date")
    suspend fun getByDate(date: String): PrayerSchedule?

    @Query("SELECT * FROM prayer_schedule WHERE date = :date")
    fun observeByDate(date: String): LiveData<PrayerSchedule?>

    @Query("SELECT MAX(date) FROM prayer_schedule")
    suspend fun getLastCachedDate(): String?

    @Query("SELECT COUNT(*) FROM prayer_schedule WHERE date >= :fromDate")
    suspend fun countFrom(fromDate: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(schedules: List<PrayerSchedule>)

    @Query("DELETE FROM prayer_schedule WHERE date < :beforeDate")
    suspend fun deleteOlderThan(beforeDate: String)

    @Query("DELETE FROM prayer_schedule")
    suspend fun deleteAll()
}
