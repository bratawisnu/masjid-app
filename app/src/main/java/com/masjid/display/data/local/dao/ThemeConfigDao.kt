package com.masjid.display.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.masjid.display.data.local.entity.ThemeConfig

@Dao
interface ThemeConfigDao {

    @Query("SELECT * FROM theme_config ORDER BY themeId ASC")
    fun observeAll(): LiveData<List<ThemeConfig>>

    @Query("SELECT * FROM theme_config WHERE themeId = :themeId")
    suspend fun getById(themeId: Int): ThemeConfig?

    @Query("SELECT * FROM theme_config WHERE themeId = :themeId")
    fun observeById(themeId: Int): LiveData<ThemeConfig?>

    @Query("SELECT COUNT(*) FROM theme_config")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(themes: List<ThemeConfig>)
}
