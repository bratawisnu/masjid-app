package com.masjid.display.prayer

import com.batoulapps.adhan2.CalculationMethod as AdhanCalculationMethod
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.Madhab as AdhanMadhab
import com.batoulapps.adhan2.PrayerAdjustments
import com.batoulapps.adhan2.PrayerTimes as AdhanPrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import com.masjid.display.data.local.entity.CalculationMethod
import com.masjid.display.data.local.entity.MadhabMethod
import com.masjid.display.data.local.entity.MosqueProfile
import com.masjid.display.data.local.entity.PrayerConfig
import com.masjid.display.data.local.entity.PrayerSchedule
import java.time.LocalDate

/**
 * Wraps Adhan-Kotlin. Kemenag RI has no dedicated preset in the library, so
 * it is approximated with MUSLIM_WORLD_LEAGUE (Fajr 20°/Isha 18°) and the
 * ihtiyat (safety margin) minutes in [PrayerConfig] make up the difference —
 * those correction fields are user-adjustable in Admin Settings regardless.
 */
@OptIn(kotlin.time.ExperimentalTime::class)
class PrayerCalculator {

    fun calculate(
        mosqueProfile: MosqueProfile,
        config: PrayerConfig,
        date: LocalDate
    ): PrayerSchedule {
        val coordinates = Coordinates(mosqueProfile.latitude, mosqueProfile.longitude)
        val dateComponents = DateComponents(date.year, date.monthValue, date.dayOfMonth)

        val adhanMethod = when (config.calculationMethod) {
            CalculationMethod.MUSLIM_WORLD_LEAGUE, CalculationMethod.KEMENAG_RI ->
                AdhanCalculationMethod.MUSLIM_WORLD_LEAGUE
            CalculationMethod.ISNA -> AdhanCalculationMethod.NORTH_AMERICA
            CalculationMethod.EGYPTIAN -> AdhanCalculationMethod.EGYPTIAN
            CalculationMethod.KARACHI -> AdhanCalculationMethod.KARACHI
            CalculationMethod.UMM_AL_QURA -> AdhanCalculationMethod.UMM_AL_QURA
        }

        val madhab = when (config.madhab) {
            MadhabMethod.SHAFI -> AdhanMadhab.SHAFI
            MadhabMethod.HANAFI -> AdhanMadhab.HANAFI
        }

        val parameters = adhanMethod.parameters.copy(
            madhab = madhab,
            prayerAdjustments = PrayerAdjustments(
                fajr = config.subuhCorrectionMinutes,
                sunrise = config.sunriseCorrectionMinutes,
                dhuhr = config.dhuhrCorrectionMinutes,
                asr = config.asrCorrectionMinutes,
                maghrib = config.maghribCorrectionMinutes,
                isha = config.ishaCorrectionMinutes
            )
        )

        val prayerTimes = AdhanPrayerTimes(coordinates, dateComponents, parameters)

        val subuhMillis = prayerTimes.fajr.toEpochMilliseconds()
        val imsakMillis = subuhMillis + config.imsakCorrectionMinutes * 60_000L

        return PrayerSchedule(
            date = date.toString(),
            imsak = imsakMillis,
            subuh = subuhMillis,
            sunrise = prayerTimes.sunrise.toEpochMilliseconds(),
            dhuhr = prayerTimes.dhuhr.toEpochMilliseconds(),
            asr = prayerTimes.asr.toEpochMilliseconds(),
            maghrib = prayerTimes.maghrib.toEpochMilliseconds(),
            isha = prayerTimes.isha.toEpochMilliseconds()
        )
    }
}
