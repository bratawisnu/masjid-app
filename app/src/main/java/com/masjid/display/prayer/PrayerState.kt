package com.masjid.display.prayer

enum class Prayer {
    IMSAK, SUBUH, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA
}

sealed class PrayerState {

    object Loading : PrayerState()

    data class Idle(
        val next: Prayer,
        val nextTimeMillis: Long,
        val countdownMillis: Long
    ) : PrayerState()

    data class AdzanTime(val prayer: Prayer) : PrayerState()

    data class IqamahCountdown(
        val prayer: Prayer,
        val remainingMillis: Long
    ) : PrayerState()
}
