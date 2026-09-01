package com.masjid.display.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AreaPosition {
    TOP, BOTTOM, LEFT, RIGHT, CENTER
}

enum class MainContentType {
    IMAGE_SLIDER, PRAYER_SCHEDULE, ANNOUNCEMENT
}

@Entity(tableName = "theme_config")
data class ThemeConfig(
    @PrimaryKey val themeId: Int,
    val name: String,

    val backgroundColor: Int,
    val primaryColor: Int,
    val secondaryColor: Int,
    val accentColor: Int,

    val headerPosition: AreaPosition = AreaPosition.TOP,
    val clockPosition: AreaPosition = AreaPosition.TOP,
    val prayerPanelPosition: AreaPosition = AreaPosition.BOTTOM,
    val mainContentType: MainContentType = MainContentType.IMAGE_SLIDER,
    val runningTextPosition: AreaPosition = AreaPosition.BOTTOM
)
