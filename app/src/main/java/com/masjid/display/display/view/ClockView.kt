package com.masjid.display.display.view

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.masjid.display.display.design.Palette
import com.masjid.display.display.design.Scale
import com.masjid.display.display.design.Typography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Hours:minutes set large, with seconds as a small superscript alongside.
 *
 * Splitting them keeps the second-by-second change out of the eye's path —
 * a full-size ticking digit pulls attention continuously in a room where the
 * screen is ambient. The large block also stays readable as one shape from
 * the back of the hall.
 */
class ClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val hourMinuteText = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, Scale.text(context, Scale.CLOCK))
        typeface = Typography.numeric(context)
        gravity = Gravity.CENTER_VERTICAL
        includeFontPadding = false
        letterSpacing = -0.02f
    }

    private val secondsText = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, Scale.text(context, Scale.CLOCK_SECONDS))
        typeface = Typography.numeric(context)
        includeFontPadding = false
    }

    private val meridiemText = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, Scale.text(context, Scale.CLOCK_MERIDIEM))
        typeface = Typography.label(context)
        includeFontPadding = false
        letterSpacing = 0.10f
    }

    /** Stacks seconds above the meridiem so both align to the clock's cap height. */
    private val sideColumn = LinearLayout(context).apply {
        orientation = VERTICAL
        gravity = Gravity.START
        setPadding(Scale.px(context, 10f), Scale.px(context, 14f), 0, 0)
        addView(secondsText)
        addView(meridiemText)
    }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
        addView(hourMinuteText)
        addView(sideColumn)
    }

    fun applyPalette(palette: Palette) {
        hourMinuteText.setTextColor(palette.textPrimary)
        secondsText.setTextColor(palette.accent)
        meridiemText.setTextColor(palette.textMuted)
    }

    fun update(nowMillis: Long, use24Hour: Boolean, timeZoneId: String?) {
        val timeZone = resolveZone(timeZoneId)
        val date = Date(nowMillis)

        hourMinuteText.text = formatter(if (use24Hour) "HH:mm" else "hh:mm", timeZone).format(date)
        secondsText.text = formatter("ss", timeZone).format(date)
        meridiemText.isVisible = !use24Hour
        if (!use24Hour) {
            meridiemText.text = formatter("a", timeZone).format(date).uppercase(Locale.getDefault())
        }
    }

    private fun formatter(pattern: String, timeZone: TimeZone) =
        SimpleDateFormat(pattern, Locale.getDefault()).apply { this.timeZone = timeZone }

    private fun resolveZone(timeZoneId: String?): TimeZone =
        timeZoneId?.let { TimeZone.getTimeZone(it) } ?: TimeZone.getDefault()
}
