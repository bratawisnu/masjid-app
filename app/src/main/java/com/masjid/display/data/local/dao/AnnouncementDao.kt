package com.masjid.display.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.masjid.display.data.local.entity.Announcement

@Dao
interface AnnouncementDao {

    @Query("SELECT * FROM announcement WHERE enabled = 1 ORDER BY priority DESC, id ASC")
    fun observeEnabled(): LiveData<List<Announcement>>

    @Query("SELECT * FROM announcement ORDER BY priority DESC, id ASC")
    fun observeAll(): LiveData<List<Announcement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(announcement: Announcement): Long

    @Update
    suspend fun update(announcement: Announcement)

    @Delete
    suspend fun delete(announcement: Announcement)
}
