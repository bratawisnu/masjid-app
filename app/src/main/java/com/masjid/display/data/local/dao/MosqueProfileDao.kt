package com.masjid.display.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.masjid.display.data.local.entity.MosqueProfile

@Dao
interface MosqueProfileDao {

    @Query("SELECT * FROM mosque_profile WHERE id = ${MosqueProfile.SINGLETON_ID}")
    fun observe(): LiveData<MosqueProfile?>

    @Query("SELECT * FROM mosque_profile WHERE id = ${MosqueProfile.SINGLETON_ID}")
    suspend fun get(): MosqueProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: MosqueProfile)
}
