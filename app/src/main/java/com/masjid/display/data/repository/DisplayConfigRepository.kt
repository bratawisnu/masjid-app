package com.masjid.display.data.repository

import androidx.lifecycle.LiveData
import com.masjid.display.data.local.dao.DisplayConfigDao
import com.masjid.display.data.local.entity.DisplayConfig

class DisplayConfigRepository(private val dao: DisplayConfigDao) {

    fun observe(): LiveData<DisplayConfig?> = dao.observe()

    suspend fun get(): DisplayConfig = dao.get() ?: DisplayConfig()

    suspend fun save(config: DisplayConfig) = dao.upsert(config)
}
