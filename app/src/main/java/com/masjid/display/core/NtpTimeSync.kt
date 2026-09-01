package com.masjid.display.core

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Minimal SNTP client. This app is offline-first — prayer times and the
 * display never depend on network access — but RTC-less TV boxes can drift
 * or reset their clock on power loss, so we opportunistically correct it
 * whenever connectivity happens to be available. Not used for anything
 * time-critical; a failed/timed-out sync is silently ignored.
 */
object NtpTimeSync {

    private const val NTP_SERVER = "pool.ntp.org"
    private const val NTP_PORT = 123
    private const val NTP_PACKET_SIZE = 48
    private const val NTP_MODE_CLIENT = 3
    private const val NTP_VERSION = 3
    private const val TIMEOUT_MS = 5000
    private const val SEVENTY_YEARS_SECONDS = 2208988800L

    /** Returns the network time in epoch millis, or null if the request failed/timed out. */
    fun requestTime(): Long? {
        return try {
            DatagramSocket().use { socket ->
                socket.soTimeout = TIMEOUT_MS
                val address = InetAddress.getByName(NTP_SERVER)
                val buffer = ByteArray(NTP_PACKET_SIZE)
                buffer[0] = ((NTP_MODE_CLIENT or (NTP_VERSION shl 3)).toByte())

                val requestPacket = DatagramPacket(buffer, buffer.size, address, NTP_PORT)
                socket.send(requestPacket)

                val responsePacket = DatagramPacket(buffer, buffer.size)
                socket.receive(responsePacket)

                val responseTicks = readTimestamp(buffer, 40)
                responseTicks
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun readTimestamp(buffer: ByteArray, offset: Int): Long {
        val seconds = readUInt32(buffer, offset)
        val fraction = readUInt32(buffer, offset + 4)
        val millisSinceNtpEpoch = seconds * 1000L + (fraction * 1000L) / 0x100000000L
        return millisSinceNtpEpoch - SEVENTY_YEARS_SECONDS * 1000L
    }

    private fun readUInt32(buffer: ByteArray, offset: Int): Long {
        var value = 0L
        for (i in 0 until 4) {
            value = (value shl 8) or (buffer[offset + i].toLong() and 0xFF)
        }
        return value
    }
}
