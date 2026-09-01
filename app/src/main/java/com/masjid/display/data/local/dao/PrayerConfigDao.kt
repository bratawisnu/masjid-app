package com.masjid.display.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.masjid.display.data.local.entity.PrayerConfig

@Dao
interface PrayerConfigDao {

    @Query("SELECT * FROM prayer_config WHERE id = ${PrayerConfig.SINGLETON_ID}")
    fun observe(): LiveData<PrayerConfig?>

    @Query("SELECT * FROM prayer_config WHERE id = ${PrayerConfig.SINGLETON_ID}")
    suspend fun get(): PrayerConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(config: PrayerConfig)
}
