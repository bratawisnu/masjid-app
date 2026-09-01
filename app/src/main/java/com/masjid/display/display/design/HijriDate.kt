package com.masjid.display.display.design

import java.time.Instant
import java.time.ZoneId
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

/**
 * Hijri calendar rendering for the header.
 *
 * Month names are supplied here rather than through `DateTimeFormatter`
 * because the platform's Arabic month strings vary by ROM and locale data,
 * and mosque displays need the spelling the congregation expects — the
 * Indonesian transliteration, not a localised variant.
 *
 * Uses the tabular Umm al-Qura calendar, which can differ by a day from the
 * local moon sighting. Admins can correct that with [dayOffset], persisted as
 * DisplayConfig.hijriDayOffset.
 */
object HijriDate {

    private val MONTHS_ID = arrayOf(
        "Muharram", "Safar", "Rabiul Awal", "Rabiul Akhir",
        "Jumadil Awal", "Jumadil Akhir", "Rajab", "Sya'ban",
        "Ramadhan", "Syawal", "Dzulqa'dah", "Dzulhijjah"
    )

    private val MONTHS_AR = arrayOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر",
        "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
        "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    )

    /** e.g. "21 Rabiul Awal 1447" */
    fun format(nowMillis: Long, zoneId: String?, dayOffset: Int = 0): String {
        val (day, month, year) = parts(nowMillis, zoneId, dayOffset)
        return "$day ${MONTHS_ID[month - 1]} $year"
    }

    /** e.g. "٢١ ربيع الأول ١٤٤٧" — Arabic-Indic digits, for the header. */
    fun formatArabic(nowMillis: Long, zoneId: String?, dayOffset: Int = 0): String {
        val (day, month, year) = parts(nowMillis, zoneId, dayOffset)
        return "${toArabicDigits(day)} ${MONTHS_AR[month - 1]} ${toArabicDigits(year)}"
    }

    private fun parts(nowMillis: Long, zoneId: String?, dayOffset: Int): Triple<Int, Int, Int> {
        val zone = runCatching { zoneId?.let { ZoneId.of(it) } }.getOrNull() ?: ZoneId.systemDefault()
        val localDate = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate().plusDays(dayOffset.toLong())
        val hijri = HijrahDate.from(localDate)
        return Triple(
            hijri.get(ChronoField.DAY_OF_MONTH),
            hijri.get(ChronoField.MONTH_OF_YEAR),
            hijri.get(ChronoField.YEAR)
        )
    }

    private fun toArabicDigits(value: Int): String = buildString {
        value.toString().forEach { ch ->
            append(if (ch in '0'..'9') ('٠' + (ch - '0')) else ch)
        }
    }
}
