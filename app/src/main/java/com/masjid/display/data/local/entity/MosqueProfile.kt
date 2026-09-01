package com.masjid.display.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mosque_profile")
data class MosqueProfile(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val name: String = "",
    val logoPath: String? = null,
    val address: String = "",
    val city: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timezone: String = "Asia/Jakarta"
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
