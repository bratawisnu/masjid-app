package com.masjid.display.data.repository

import androidx.lifecycle.LiveData
import com.masjid.display.data.local.dao.MosqueProfileDao
import com.masjid.display.data.local.entity.MosqueProfile

class MosqueProfileRepository(private val dao: MosqueProfileDao) {

    fun observe(): LiveData<MosqueProfile?> = dao.observe()

    suspend fun get(): MosqueProfile? = dao.get()

    suspend fun save(profile: MosqueProfile) = dao.upsert(profile)
}
