package com.masjid.display.data.repository

import androidx.lifecycle.LiveData
import com.masjid.display.data.local.dao.ThemeConfigDao
import com.masjid.display.data.local.entity.ThemeConfig

class ThemeConfigRepository(private val dao: ThemeConfigDao) {

    fun observeAll(): LiveData<List<ThemeConfig>> = dao.observeAll()

    fun observeById(themeId: Int): LiveData<ThemeConfig?> = dao.observeById(themeId)

    suspend fun getById(themeId: Int): ThemeConfig? = dao.getById(themeId)
}
