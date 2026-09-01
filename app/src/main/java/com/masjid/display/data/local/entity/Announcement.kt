package com.masjid.display.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "announcement")
data class Announcement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val enabled: Boolean = true,
    val priority: Int = 0,
    val startDate: String? = null, // "yyyy-MM-dd", null = no start bound
    val endDate: String? = null,   // "yyyy-MM-dd", null = no end bound
    val speed: Int = 8 // px per frame, higher = faster
)
