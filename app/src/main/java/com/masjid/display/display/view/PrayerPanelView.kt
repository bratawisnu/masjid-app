package com.masjid.display.display.view

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import com.masjid.display.data.local.entity.AreaPosition
import com.masjid.display.data.local.entity.PrayerSchedule
import com.masjid.display.display.design.Palette
import com.masjid.display.display.design.PrayerNaming
import com.masjid.display.display.design.Scale
import com.masjid.display.prayer.Prayer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * The arcade: seven bays in a row, one of them raised.
 *
 * A mosque facade already has this structure — a row of equal bays with the
 * mihrab marked out — so the layout encodes the content rather than
 * decorating it.
 */
class PrayerPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private data class Bay(val prayer: Prayer, val label: String, val arabic: String, val view: PrayerBayView)

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    private val bays: List<Bay> = PrayerNaming.ordered.map { prayer ->
        Bay(prayer, PrayerNaming.label(prayer), PrayerNaming.arabic(prayer), PrayerBayView(context))
    }

    private var activePrayer: Prayer? = null
    private var riseAnimator: ValueAnimator? = null

    private val riseInset = Scale.dim(context, Scale.BAY_RISE)

    init {
        // Bays rise above their own bounds; without this the raised bay and
        // its glow are clipped by the panel.
        clipChildren = false
        clipToPadding = false

        bays.forEach { bay ->
            bay.view.bind(bay.label, bay.arabic, "--:--")
            addView(bay.view)
        }
        setArrangement(AreaPosition.BOTTOM)
    }

    /**
     * Lays the arcade out as a row or a rail, per the theme's
     * `prayerPanelPosition`.
     *
     * The horizontal band is the default and the one the design was drawn for.
     * The rail exists because five of the fifteen themes ask for it, and a
     * theme setting that silently does nothing is worse than one that isn't
     * offered — the bays keep their identity either way, only the axis they
     * rise along changes.
     */
    fun setArrangement(position: AreaPosition) {
        val vertical = position == AreaPosition.LEFT || position == AreaPosition.RIGHT

        orientation = if (vertical) VERTICAL else HORIZONTAL
        gravity = if (vertical) Gravity.CENTER_VERTICAL else Gravity.BOTTOM

        // As a band the panel is measured by its bays, because its container
        // is too: asking to match a parent that is itself sizing to this view
        // makes the height depend on which of the two is measured first. As a
        // rail the container has a real height and the panel fills it.
        (layoutParams as? ViewGroup.LayoutParams)?.let { params ->
            val wanted = if (vertical) {
                ViewGroup.LayoutParams.MATCH_PARENT
            } else {
                ViewGroup.LayoutParams.WRAP_CONTENT
            }
            if (params.height != wanted) {
                params.height = wanted
                layoutParams = params
            }
        }

        // Padding on the edge the bays rise toward, so the raised one has room
        // inside the container instead of overlapping the neighbouring band.
        when (position) {
            AreaPosition.LEFT -> setPadding(0, 0, riseInset, 0)
            AreaPosition.RIGHT -> setPadding(riseInset, 0, 0, 0)
            else -> setPadding(0, riseInset, 0, 0)
        }

        val axis = when (position) {
            AreaPosition.LEFT -> PrayerBayView.RiseAxis.RIGHT
            AreaPosition.RIGHT -> PrayerBayView.RiseAxis.LEFT
            else -> PrayerBayView.RiseAxis.UP
        }

        bays.forEach { bay ->
            bay.view.riseAxis = axis
            bay.view.layoutParams = if (vertical) {
                LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
            } else {
                LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            }
        }
    }

    fun applyPalette(palette: Palette) {
        bays.forEach { it.view.applyPalette(palette) }
    }

    fun bind(schedule: PrayerSchedule, timeZoneId: String?, highlight: Prayer?) {
        timeFormat.timeZone = timeZoneId?.let { TimeZone.getTimeZone(it) } ?: TimeZone.getDefault()
        bays.forEach { bay ->
            val at = PrayerNaming.timeOf(schedule, bay.prayer)
            bay.view.bind(bay.label, bay.arabic, timeFormat.format(Date(at)))
        }
        setActivePrayer(highlight)
    }

    /**
     * Moves the raised bay. The transition is the only cue the congregation
     * gets that a prayer time has passed, so it animates rather than snaps.
     */
    fun setActivePrayer(prayer: Prayer?) {
        if (prayer == activePrayer) return
        val previous = activePrayer
        activePrayer = prayer

        // First bind after boot: place the bay without animating, so the
        // screen doesn't open on a moving element.
        if (previous == null) {
            bays.forEach { it.view.riseFraction = if (it.prayer == prayer) 1f else 0f }
            return
        }

        riseAnimator?.cancel()
        val from = bays.associate { it.prayer to it.view.riseFraction }
        riseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 600L
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                bays.forEach { bay ->
                    val target = if (bay.prayer == prayer) 1f else 0f
                    val start = from[bay.prayer] ?: 0f
                    bay.view.riseFraction = start + (target - start) * progress
                }
            }
            start()
        }
    }

    /** [remainingMillis] null clears the countdown (e.g. during adzan). */
    fun setCountdown(remainingMillis: Long?) {
        val text = remainingMillis?.let(::formatCountdown)
        bays.forEach { bay ->
            bay.view.setCountdown(if (bay.prayer == activePrayer) text else null)
        }
    }

    private fun formatCountdown(remainingMillis: Long): String {
        val totalSeconds = (remainingMillis / 1000).coerceAtLeast(0)
        return String.format(
            Locale.getDefault(),
            "%02d:%02d:%02d",
            totalSeconds / 3600,
            (totalSeconds % 3600) / 60,
            totalSeconds % 60
        )
    }

    fun stop() {
        riseAnimator?.cancel()
        riseAnimator = null
    }
}
