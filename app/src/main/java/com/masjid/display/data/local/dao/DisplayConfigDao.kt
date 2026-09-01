package com.masjid.display.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.masjid.display.data.local.entity.DisplayConfig

@Dao
interface DisplayConfigDao {

    @Query("SELECT * FROM display_config WHERE id = ${DisplayConfig.SINGLETON_ID}")
    fun observe(): LiveData<DisplayConfig?>

    @Query("SELECT * FROM display_config WHERE id = ${DisplayConfig.SINGLETON_ID}")
    suspend fun get(): DisplayConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(config: DisplayConfig)
}
