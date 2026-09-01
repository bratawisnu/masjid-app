package com.masjid.display.prayer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.masjid.display.data.local.entity.PrayerSchedule
import com.masjid.display.data.repository.PrayerScheduleRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Drives prayer-time state purely off wall-clock comparisons against cached
 * [PrayerSchedule] rows. Intentionally has no AlarmManager/WorkManager
 * involvement: this is a foreground kiosk app, so [tick] is called once a
 * second by the display's clock loop (see ClockManager), which is already
 * running for the on-screen clock.
 */
class PrayerManager(
    private val scheduleRepository: PrayerScheduleRepository,
    private val scope: CoroutineScope
) {

    companion object {
        private val ADZAN_PRAYERS = listOf(Prayer.SUBUH, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA)
        private const val ADZAN_DURATION_MILLIS = 3 * 60_000L
        private const val IQAMAH_DURATION_MILLIS = 10 * 60_000L
    }

    private val _state = MutableLiveData<PrayerState>(PrayerState.Loading)
    val state: LiveData<PrayerState> = _state

    private val _todaySchedule = MutableLiveData<PrayerSchedule?>(null)
    val todaySchedule: LiveData<PrayerSchedule?> = _todaySchedule

    private var loadedDate: String? = null
    private var tomorrowSchedule: PrayerSchedule? = null
    private var loading = false

    fun tick(nowMillis: Long) {
        val todayDate = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val todayDateString = todayDate.toString()

        if (loadedDate != todayDateString) {
            if (!loading) loadSchedules(todayDate)
            return
        }

        val schedule = _todaySchedule.value ?: return
        evaluate(nowMillis, schedule)
    }

    /** Forces the next [tick] to reload from the repository, e.g. after Admin Settings regenerates the schedule. */
    fun invalidate() {
        loadedDate = null
        tomorrowSchedule = null
        _todaySchedule.value = null
    }

    private fun loadSchedules(today: LocalDate) {
        loading = true
        scope.launch {
            val schedule = scheduleRepository.getByDate(today.toString())
            _todaySchedule.value = schedule
            tomorrowSchedule = scheduleRepository.getByDate(today.plusDays(1).toString())
            // Only mark the date as loaded once the schedule actually exists —
            // PrayerScheduleGenerator may still be generating it (e.g. right
            // after first launch), so a null result here must retry on the
            // next tick rather than being treated as final.
            if (schedule != null) loadedDate = today.toString()
            loading = false
        }
    }

    private fun evaluate(nowMillis: Long, schedule: PrayerSchedule) {
        for (prayer in ADZAN_PRAYERS) {
            val prayerTime = schedule.timeFor(prayer)
            val elapsed = nowMillis - prayerTime
            when {
                elapsed in 0 until ADZAN_DURATION_MILLIS -> {
                    _state.postValue(PrayerState.AdzanTime(prayer))
                    return
                }
                elapsed in ADZAN_DURATION_MILLIS until (ADZAN_DURATION_MILLIS + IQAMAH_DURATION_MILLIS) -> {
                    val remaining = ADZAN_DURATION_MILLIS + IQAMAH_DURATION_MILLIS - elapsed
                    _state.postValue(PrayerState.IqamahCountdown(prayer, remaining))
                    return
                }
            }
        }

        val (next, nextTime) = findNextPrayer(nowMillis, schedule)
        _state.postValue(PrayerState.Idle(next, nextTime, nextTime - nowMillis))
    }

    private fun findNextPrayer(nowMillis: Long, schedule: PrayerSchedule): Pair<Prayer, Long> {
        val upcomingToday = ADZAN_PRAYERS
            .map { it to schedule.timeFor(it) }
            .firstOrNull { it.second > nowMillis }
        if (upcomingToday != null) return upcomingToday

        val tomorrow = tomorrowSchedule
        return if (tomorrow != null) {
            Prayer.SUBUH to tomorrow.subuh
        } else {
            Prayer.SUBUH to schedule.subuh
        }
    }

    private fun PrayerSchedule.timeFor(prayer: Prayer): Long = when (prayer) {
        Prayer.IMSAK -> imsak
        Prayer.SUBUH -> subuh
        Prayer.SUNRISE -> sunrise
        Prayer.DHUHR -> dhuhr
        Prayer.ASR -> asr
        Prayer.MAGHRIB -> maghrib
        Prayer.ISHA -> isha
    }
}
