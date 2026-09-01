package com.masjid.display.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per calendar date. Time fields are epoch millis (UTC) for the
 * exact prayer instant on that date, already including [PrayerConfig]
 * correction minutes.
 */
@Entity(tableName = "prayer_schedule")
data class PrayerSchedule(
    @PrimaryKey val date: String, // "yyyy-MM-dd"
    val imsak: Long,
    val subuh: Long,
    val sunrise: Long,
    val dhuhr: Long,
    val asr: Long,
    val maghrib: Long,
    val isha: Long
)
