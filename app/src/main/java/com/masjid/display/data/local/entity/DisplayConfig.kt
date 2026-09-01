package com.masjid.display.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "display_config")
data class DisplayConfig(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val activeThemeId: Int = 1,
    val clockVisible: Boolean = true,
    val clockUse24Hour: Boolean = true,
    val prayerPanelVisible: Boolean = true,
    val countdownVisible: Boolean = true,
    val logoVisible: Boolean = true,
    val sliderVisible: Boolean = true,
    val runningTextVisible: Boolean = true,
    val timeOffsetMinutes: Int = 0,
    val timezoneOverride: String? = null,
    /** Corrects the tabular Hijri calendar to the local moon sighting, in days. */
    val hijriDayOffset: Int = 0,
    /**
     * Absolute path to a background photo under app-scoped external storage,
     * or null for the theme's solid colour. The file can vanish (factory
     * reset, manual cleanup), so every read must tolerate a dead path.
     */
    val backgroundImagePath: String? = null,
    val adminPinHash: String? = null,
    val screensaverEnabled: Boolean = false,
    val screensaverStartHour: Int = 23,
    val screensaverStartMinute: Int = 0,
    val screensaverEndHour: Int = 4,
    val screensaverEndMinute: Int = 0
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
