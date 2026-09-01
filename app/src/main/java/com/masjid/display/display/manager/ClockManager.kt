package com.masjid.display.display.manager

import android.os.Handler
import android.os.Looper
import com.masjid.display.core.NtpDriftManager

/**
 * Single 1-second tick source for the whole display. Both the on-screen
 * clock and PrayerManager's real-time transition detection subscribe to
 * this instead of using AlarmManager/WorkManager, since the app is always
 * foreground. Also opportunistically applies NTP drift correction on top of
 * the manual admin "Time Offset" — see NtpDriftManager for why the device
 * clock itself is never modified.
 */
class ClockManager(
    private val ntpDriftManager: NtpDriftManager? = null,
    private val onTick: (nowMillis: Long, offsetMillis: Long) -> Unit
) {

    private val handler = Handler(Looper.getMainLooper())
    private var manualOffsetMillis: Long = 0
    private var running = false

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (!running) return
            val nowMillis = System.currentTimeMillis() + manualOffsetMillis + (ntpDriftManager?.driftMillis ?: 0)
            ntpDriftManager?.syncIfDue(nowMillis)
            onTick(nowMillis, manualOffsetMillis)
            handler.postDelayed(this, 1000L)
        }
    }

    fun setTimeOffsetMinutes(minutes: Int) {
        manualOffsetMillis = minutes * 60_000L
    }

    fun start() {
        if (running) return
        running = true
        handler.post(tickRunnable)
    }

    fun stop() {
        running = false
        handler.removeCallbacks(tickRunnable)
    }
}
