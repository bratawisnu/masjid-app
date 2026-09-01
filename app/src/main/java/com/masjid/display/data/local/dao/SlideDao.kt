package com.masjid.display.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.masjid.display.data.local.entity.Slide

@Dao
interface SlideDao {

    @Query("SELECT * FROM slide WHERE enabled = 1 ORDER BY `order` ASC")
    fun observeEnabled(): LiveData<List<Slide>>

    @Query("SELECT * FROM slide ORDER BY `order` ASC")
    fun observeAll(): LiveData<List<Slide>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(slide: Slide): Long

    @Update
    suspend fun update(slide: Slide)

    @Delete
    suspend fun delete(slide: Slide)
}
