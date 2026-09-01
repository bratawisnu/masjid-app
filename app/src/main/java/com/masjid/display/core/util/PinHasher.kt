package com.masjid.display.core.util

import java.security.MessageDigest

object PinHasher {

    fun hash(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun matches(pin: String, hash: String?): Boolean {
        if (hash == null) return false
        return hash(pin) == hash
    }
}
