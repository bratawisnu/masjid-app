package com.masjid.display.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** What a slide carries, and therefore which fields below are filled. */
enum class SlideType {
    /** [Slide.body] holds the message; nothing is loaded from disk. */
    TEXT,

    /** [Slide.path] points at a copied image file. */
    IMAGE,

    /** [Slide.path] for a copied file, or [Slide.url] for a network stream. */
    VIDEO
}

/**
 * One frame of the slideshow.
 *
 * The three payload columns are all nullable and only one of them is populated
 * for any given [type] — a slide table with a `contentType` and a single
 * overloaded `value` column would be smaller, but it puts a URL, a filesystem
 * path and a paragraph of Indonesian prose in one place and loses the ability
 * to tell them apart when a file goes missing.
 */
@Entity(tableName = "slide")
data class Slide(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: SlideType = SlideType.IMAGE,
    /** Absolute path under app-scoped external storage; IMAGE and local VIDEO. */
    val path: String? = null,
    /** Network address for a streamed VIDEO. */
    val url: String? = null,
    /** The message shown by a TEXT slide. */
    val body: String? = null,
    val order: Int,
    /** Ignored by VIDEO slides, which run for as long as the video does. */
    val durationSeconds: Int = 10,
    val enabled: Boolean = true
) {
    /**
     * Where the player should read this slide from, or null if it has no
     * source to read — a TEXT slide, or a row saved without its payload.
     */
    val source: String?
        get() = when (type) {
            SlideType.TEXT -> null
            SlideType.IMAGE -> path
            SlideType.VIDEO -> path ?: url
        }

    /** True when this slide owns a file that deleting it should also remove. */
    val ownsFile: Boolean
        get() = path != null
}
