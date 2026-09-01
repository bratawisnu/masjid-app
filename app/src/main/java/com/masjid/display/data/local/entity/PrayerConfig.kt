package com.masjid.display.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CalculationMethod {
    MUSLIM_WORLD_LEAGUE,
    ISNA,
    EGYPTIAN,
    KARACHI,
    UMM_AL_QURA,
    KEMENAG_RI
}

enum class MadhabMethod {
    SHAFI,
    HANAFI
}

@Entity(tableName = "prayer_config")
data class PrayerConfig(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val calculationMethod: CalculationMethod = CalculationMethod.KEMENAG_RI,
    val madhab: MadhabMethod = MadhabMethod.SHAFI,
    val imsakCorrectionMinutes: Int = -10,
    val subuhCorrectionMinutes: Int = 2,
    val sunriseCorrectionMinutes: Int = 0,
    val dhuhrCorrectionMinutes: Int = 2,
    val asrCorrectionMinutes: Int = 2,
    val maghribCorrectionMinutes: Int = 2,
    val ishaCorrectionMinutes: Int = 2
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
