package com.masjid.display.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Regular (non-system) apps cannot call Android's setTime() — that requires
 * a signature/system-level permission this app will never have. So instead
 * of changing the device clock, this computes the drift between NTP truth
 * and the device clock and exposes it as an additive correction. ClockManager
 * applies it on top of System.currentTimeMillis() the same way the manual
 * "Time Offset" admin setting does, just automatically instead of by hand.
 */
class NtpDriftManager(private val scope: CoroutineScope) {

    companion object {
        private const val SYNC_INTERVAL_MS = 6 * 60 * 60 * 1000L // 6 hours
    }

    @Volatile
    var driftMillis: Long = 0
        private set

    private var lastSyncAttempt = 0L

    /** Call periodically (e.g. from a clock tick); internally rate-limits itself. */
    fun syncIfDue(nowMillis: Long) {
        if (nowMillis - lastSyncAttempt < SYNC_INTERVAL_MS) return
        lastSyncAttempt = nowMillis

        scope.launch(Dispatchers.IO) {
            val ntpMillis = NtpTimeSync.requestTime() ?: return@launch
            val deviceMillisAtResponse = System.currentTimeMillis()
            // Ignore round-trip time: on a kiosk device the offset only needs
            // to be accurate to the second, not the millisecond.
            driftMillis = ntpMillis - deviceMillisAtResponse
        }
    }
}
