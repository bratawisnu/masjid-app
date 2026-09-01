package com.masjid.display.display.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import com.masjid.display.data.local.entity.PrayerSchedule
import com.masjid.display.display.design.Palette
import com.masjid.display.display.design.PrayerNaming
import com.masjid.display.display.design.Scale
import com.masjid.display.display.design.Typography
import com.masjid.display.prayer.Prayer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * The day's seven times as a vertical board, for themes whose stage is
 * PRAYER_SCHEDULE.
 *
 * The arcade along the bottom already carries these times, so a board that
 * merely repeated them would be dead weight in the largest area of the screen.
 * This one answers a different question: not "when is the next prayer" — the
 * mihrab bay does that — but "where are we in the day". Elapsed prayers recede
 * to the muted tone, the current one takes the accent and a marker in the left
 * margin, and the rest wait at full strength.
 *
 * Reading down a column also suits the stage's proportions, where the arcade's
 * seven-across row would leave each entry too small to matter.
 */
class ScheduleBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    /** One row: marker rail, name, Arabic, time. */
    private inner class Row(val prayer: Prayer) : LinearLayout(context) {

        private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val markerWidth = Scale.px(context, 4f).toFloat()

        var isCurrent: Boolean = false
        var isElapsed: Boolean = false

        val labelText = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, Scale.text(context, Scale.SCHEDULE_LABEL))
            typeface = Typography.label(context)
            includeFontPadding = false
            letterSpacing = 0.10f
        }

        val arabicText = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, Scale.text(context, Scale.SCHEDULE_ARABIC))
            typeface = Typography.arabic(context)
            includeFontPadding = false
            gravity = Gravity.CENTER_VERTICAL
        }

        val timeText = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, Scale.text(context, Scale.SCHEDULE_TIME))
            typeface = Typography.numeric(context)
            includeFontPadding = false
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            text = "--:--"
        }

        init {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setWillNotDraw(false)
            // Left inset clears the marker rail; the rail is painted, not laid
            // out, so rows keep identical heights whether marked or not.
            setPadding(Scale.px(context, 22f), 0, 0, 0)

            addView(labelText, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
            addView(
                arabicText,
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = Scale.dim(context, Scale.GUTTER)
                }
            )
            addView(timeText, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        }

        fun applyPalette(palette: Palette) {
            markerPaint.color = palette.accent
            rulePaint.color = palette.hairline
            applyState(palette)
        }

        /**
         * Three states, three weights. Elapsed rows drop to muted so the eye
         * skips them; the current row takes the accent on both name and time
         * so it reads as one object rather than a highlighted number.
         */
        fun applyState(palette: Palette) {
            when {
                isCurrent -> {
                    labelText.setTextColor(palette.accent)
                    timeText.setTextColor(palette.accent)
                    arabicText.setTextColor(palette.accent)
                }
                isElapsed -> {
                    labelText.setTextColor(palette.textMuted)
                    timeText.setTextColor(palette.textMuted)
                    arabicText.setTextColor(palette.textMuted)
                }
                else -> {
                    labelText.setTextColor(palette.textPrimary)
                    timeText.setTextColor(palette.textPrimary)
                    arabicText.setTextColor(palette.textMuted)
                }
            }
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            // Hairline between rows rather than around them: a full grid would
            // fight the arcade below, which has no boxes either.
            canvas.drawRect(0f, height - 1f, width.toFloat(), height.toFloat(), rulePaint)
            if (isCurrent) {
                val inset = height * 0.18f
                canvas.drawRect(0f, inset, markerWidth, height - inset, markerPaint)
            }
        }
    }

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val rows: List<Row> = PrayerNaming.ordered.map { Row(it) }

    private var palette: Palette? = null
    private var current: Prayer? = null

    init {
        orientation = VERTICAL
        rows.forEach { row ->
            row.labelText.text = PrayerNaming.label(row.prayer)
            row.arabicText.text = PrayerNaming.arabic(row.prayer)
            // Equal weight: the board fills the stage exactly, so it never
            // leaves a gap under the last row on a tall panel.
            addView(row, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        }
    }

    fun applyPalette(palette: Palette) {
        this.palette = palette
        rows.forEach { it.applyPalette(palette) }
    }

    /**
     * [highlight] is the next prayer due; everything before it has passed.
     * Null (during adzan/iqamah) leaves every row at full strength rather than
     * guessing, since the overlay is carrying the moment anyway.
     */
    fun bind(schedule: PrayerSchedule, timeZoneId: String?, highlight: Prayer?) {
        timeFormat.timeZone = timeZoneId?.let { TimeZone.getTimeZone(it) } ?: TimeZone.getDefault()
        current = highlight
        val highlightIndex = highlight?.let { PrayerNaming.ordered.indexOf(it) } ?: -1
        val palette = palette

        rows.forEachIndexed { index, row ->
            row.timeText.text =
                timeFormat.format(Date(PrayerNaming.timeOf(schedule, row.prayer)))
            row.isCurrent = row.prayer == highlight
            row.isElapsed = highlightIndex >= 0 && index < highlightIndex
            if (palette != null) row.applyState(palette)
        }
    }
}
