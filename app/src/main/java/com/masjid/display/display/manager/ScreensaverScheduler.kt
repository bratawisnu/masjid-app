package com.masjid.display.display.manager

import com.masjid.display.data.local.entity.DisplayConfig
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * Pure time-window check, no AlarmManager: evaluated on every ClockManager
 * tick alongside prayer-state detection. Handles the overnight case (e.g.
 * 23:00-04:00) where start is later in the day than end.
 */
object ScreensaverScheduler {

    fun isActive(config: DisplayConfig, nowMillis: Long, zoneId: String?): Boolean {
        if (!config.screensaverEnabled) return false

        val zone = zoneId?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: ZoneId.systemDefault()
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalTime()

        val start = LocalTime.of(config.screensaverStartHour, config.screensaverStartMinute)
        val end = LocalTime.of(config.screensaverEndHour, config.screensaverEndMinute)

        return if (start <= end) {
            now >= start && now < end
        } else {
            now >= start || now < end
        }
    }
}
