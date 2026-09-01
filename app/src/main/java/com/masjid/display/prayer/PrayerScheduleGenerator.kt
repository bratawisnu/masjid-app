package com.masjid.display.prayer

import com.masjid.display.data.local.entity.MosqueProfile
import com.masjid.display.data.local.entity.PrayerConfig
import com.masjid.display.data.repository.PrayerScheduleRepository
import java.time.LocalDate

/**
 * Keeps a rolling window of cached [com.masjid.display.data.local.entity.PrayerSchedule]
 * rows so the app never depends on a hardcoded multi-year table or a live
 * network call for its core function.
 */
class PrayerScheduleGenerator(
    private val repository: PrayerScheduleRepository,
    private val calculator: PrayerCalculator = PrayerCalculator()
) {

    companion object {
        private const val WINDOW_DAYS = 30
        private const val RETENTION_DAYS = 7
    }

    suspend fun ensureWindow(mosqueProfile: MosqueProfile, config: PrayerConfig) {
        // Defense-in-depth: UI forms validate lat/long range before saving,
        // but a corrupt/pre-existing row shouldn't crash the whole app.
        if (mosqueProfile.latitude !in -90.0..90.0 || mosqueProfile.longitude !in -180.0..180.0) return

        val today = LocalDate.now()
        val targetLastDate = today.plusDays(WINDOW_DAYS.toLong())

        val cachedCount = repository.countFrom(today.toString())
        if (cachedCount >= WINDOW_DAYS) return

        val startDate = repository.getLastCachedDate()
            ?.let { LocalDate.parse(it).plusDays(1) }
            ?.takeIf { !it.isBefore(today) }
            ?: today

        if (startDate.isAfter(targetLastDate)) return

        val schedules = generateSequence(startDate) { it.plusDays(1) }
            .takeWhile { !it.isAfter(targetLastDate) }
            .map { calculator.calculate(mosqueProfile, config, it) }
            .toList()

        repository.insertAll(schedules)
        repository.pruneOlderThan(today.minusDays(RETENTION_DAYS.toLong()).toString())
    }

    /**
     * Wipes the cached window and recomputes it from scratch. Call this
     * whenever location or calculation settings change in Admin Settings —
     * otherwise stale schedules calculated under the old settings would
     * keep serving until they naturally roll off the window.
     */
    suspend fun regenerate(mosqueProfile: MosqueProfile, config: PrayerConfig) {
        repository.clearAll()
        ensureWindow(mosqueProfile, config)
    }
}
