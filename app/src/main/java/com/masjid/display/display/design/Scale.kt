package com.masjid.display.display.design

import android.content.Context
import kotlin.math.roundToInt

/**
 * Every dimension on the display screen derives from screen HEIGHT, not dp.
 *
 * A TV is viewed from roughly 8x its own height away, so what governs
 * legibility is the fraction of the panel a glyph covers — not its physical
 * size. dp/sp normalise for *density*, which is the wrong variable here: a
 * 48sp clock is 4.4% of a 1080p panel and 2.2% of a 4K panel, so the same
 * layout silently halves in apparent size on a better screen.
 *
 * Expressing sizes as a fraction of height makes 720p, 1080p and 4K render
 * identically at a given viewing distance.
 */
object Scale {

    /** Layout was authored against a 1080p panel; [px] converts from it. */
    private const val REFERENCE_HEIGHT = 1080f

    // ---- Type sizes, as a fraction of screen height ----
    const val CLOCK = 0.130f
    const val CLOCK_SECONDS = 0.040f
    const val CLOCK_MERIDIEM = 0.026f
    const val PRAYER_TIME = 0.045f
    const val PRAYER_TIME_ACTIVE = 0.058f
    const val PRAYER_LABEL = 0.017f
    const val PRAYER_ARABIC = 0.023f
    const val COUNTDOWN = 0.027f
    const val MOSQUE_NAME = 0.034f
    const val MOSQUE_CITY = 0.018f
    const val HIJRI = 0.028f
    const val GREGORIAN = 0.018f
    const val RUNNING_TEXT = 0.025f

    // ---- Schedule board: the arcade's data set as a vertical table ----
    const val SCHEDULE_LABEL = 0.032f
    const val SCHEDULE_ARABIC = 0.022f
    const val SCHEDULE_TIME = 0.046f

    // ---- Announcement stage: one notice held large, not a ticker ----
    const val ANNOUNCEMENT_EYEBROW = 0.018f
    const val ANNOUNCEMENT_BODY = 0.050f

    // ---- Adzan overlay: the one moment the screen speaks over everything ----
    const val ADZAN_EYEBROW = 0.026f
    const val ADZAN_TITLE = 0.115f
    const val ADZAN_COUNTDOWN = 0.072f

    // ---- Spacing, as a fraction of screen height ----
    const val GUTTER = 0.022f
    const val BAY_RISE = 0.030f

    fun heightPx(context: Context): Int = context.resources.displayMetrics.heightPixels

    fun widthPx(context: Context): Int = context.resources.displayMetrics.widthPixels

    /** Text size in raw pixels — use with [android.util.TypedValue.COMPLEX_UNIT_PX]. */
    fun text(context: Context, fractionOfHeight: Float): Float =
        heightPx(context) * fractionOfHeight

    /** A spacing/size value in raw pixels, from a fraction of screen height. */
    fun dim(context: Context, fractionOfHeight: Float): Int =
        (heightPx(context) * fractionOfHeight).roundToInt()

    /** Scales a value authored against the 1080p reference panel. */
    fun px(context: Context, referencePx: Float): Int =
        (referencePx * (heightPx(context) / REFERENCE_HEIGHT)).roundToInt()

    /**
     * Older panels overscan by 3–5% per edge, cropping whatever sits there.
     * Content is inset by this much so the mosque name and ticker survive.
     */
    fun safeInsetVertical(context: Context): Int = dim(context, 0.035f)

    fun safeInsetHorizontal(context: Context): Int = (widthPx(context) * 0.030f).roundToInt()
}
