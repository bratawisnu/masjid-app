package com.masjid.display.data.repository

import androidx.lifecycle.LiveData
import com.masjid.display.data.local.dao.PrayerConfigDao
import com.masjid.display.data.local.entity.PrayerConfig

class PrayerConfigRepository(private val dao: PrayerConfigDao) {

    fun observe(): LiveData<PrayerConfig?> = dao.observe()

    suspend fun get(): PrayerConfig = dao.get() ?: PrayerConfig()

    suspend fun save(config: PrayerConfig) = dao.upsert(config)
}
