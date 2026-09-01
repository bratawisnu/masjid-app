package com.masjid.display.display.design

import com.masjid.display.data.local.entity.PrayerSchedule
import com.masjid.display.prayer.Prayer

/**
 * The Indonesian and Arabic name for each prayer, and the schedule lookup.
 *
 * Three views now render prayer names — the arcade, the schedule board, and
 * the adzan overlay — and the spellings have to agree across all of them. A
 * screen that says "DZUHUR" in the row and "DHUHR" in the overlay reads as two
 * different systems bolted together.
 */
object PrayerNaming {

    /** Display order, which is also chronological through the day. */
    val ordered: List<Prayer> = listOf(
        Prayer.IMSAK,
        Prayer.SUBUH,
        Prayer.SUNRISE,
        Prayer.DHUHR,
        Prayer.ASR,
        Prayer.MAGHRIB,
        Prayer.ISHA
    )

    fun label(prayer: Prayer): String = when (prayer) {
        Prayer.IMSAK -> "IMSAK"
        Prayer.SUBUH -> "SUBUH"
        Prayer.SUNRISE -> "TERBIT"
        Prayer.DHUHR -> "DZUHUR"
        Prayer.ASR -> "ASHAR"
        Prayer.MAGHRIB -> "MAGHRIB"
        Prayer.ISHA -> "ISYA"
    }

    fun arabic(prayer: Prayer): String = when (prayer) {
        Prayer.IMSAK -> "إمساك"
        Prayer.SUBUH -> "الفجر"
        Prayer.SUNRISE -> "الشروق"
        Prayer.DHUHR -> "الظهر"
        Prayer.ASR -> "العصر"
        Prayer.MAGHRIB -> "المغرب"
        Prayer.ISHA -> "العشاء"
    }

    fun timeOf(schedule: PrayerSchedule, prayer: Prayer): Long = when (prayer) {
        Prayer.IMSAK -> schedule.imsak
        Prayer.SUBUH -> schedule.subuh
        Prayer.SUNRISE -> schedule.sunrise
        Prayer.DHUHR -> schedule.dhuhr
        Prayer.ASR -> schedule.asr
        Prayer.MAGHRIB -> schedule.maghrib
        Prayer.ISHA -> schedule.isha
    }
}
