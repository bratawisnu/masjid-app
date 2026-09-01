package com.masjid.display.data.repository

import androidx.lifecycle.LiveData
import com.masjid.display.data.local.dao.AnnouncementDao
import com.masjid.display.data.local.entity.Announcement

class AnnouncementRepository(private val dao: AnnouncementDao) {

    fun observeEnabled(): LiveData<List<Announcement>> = dao.observeEnabled()

    fun observeAll(): LiveData<List<Announcement>> = dao.observeAll()

    suspend fun save(announcement: Announcement): Long = dao.upsert(announcement)

    suspend fun update(announcement: Announcement) = dao.update(announcement)

    suspend fun delete(announcement: Announcement) = dao.delete(announcement)
}
