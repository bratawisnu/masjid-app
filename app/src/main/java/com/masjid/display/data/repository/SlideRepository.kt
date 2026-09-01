package com.masjid.display.data.repository

import androidx.lifecycle.LiveData
import com.masjid.display.data.local.dao.SlideDao
import com.masjid.display.data.local.entity.Slide

class SlideRepository(private val dao: SlideDao) {

    fun observeEnabled(): LiveData<List<Slide>> = dao.observeEnabled()

    fun observeAll(): LiveData<List<Slide>> = dao.observeAll()

    suspend fun save(slide: Slide): Long = dao.upsert(slide)

    suspend fun update(slide: Slide) = dao.update(slide)

    suspend fun delete(slide: Slide) = dao.delete(slide)
}
