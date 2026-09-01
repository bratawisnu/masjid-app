package com.masjid.display.display.view

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.masjid.display.display.design.HijriDate
import com.masjid.display.display.design.Palette
import com.masjid.display.display.design.Scale
import com.masjid.display.display.design.Typography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Identity on the left, date on the right, separated by a hairline that runs
 * the full width — the datum line the rest of the screen hangs from.
 *
 * The Hijri date leads and the Gregorian date supports it, since the Hijri
 * date is what the congregation reads here.
 */
class HeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val logoView = ImageView(context).apply {
        val size = Scale.dim(context, 0.075f)
        layoutParams = LinearLayout.LayoutParams(size, size).apply {
            marginEnd = Scale.dim(context, Scale.GUTTER)
        }
        scaleType = ImageView.ScaleType.FIT_CENTER
    }

    private val nameText = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, Scale.text(context, Scale.MOSQUE_NAME))
        typeface = Typography.heading(context)
        includeFontPadding = false
        maxLines = 1
    }

    private val cityText = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, Scale.text(context, Scale.MOSQUE_CITY))
        typeface = Typography.body(context)
        includeFontPadding = false
        maxLines = 1
        letterSpacing = 0.04f
    }

    private val hijriText = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, Scale.text(context, Scale.HIJRI))
        typeface = Typography.arabic(context)
        gravity = Gravity.END
        includeFontPadding = false
    }

    private val gregorianText = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, Scale.text(context, Scale.GREGORIAN))
        typeface = Typography.body(context)
        gravity = Gravity.END
        includeFontPadding = false
        letterSpacing = 0.04f
    }

    private val identityColumn = LinearLayout(context).apply {
        orientation = VERTICAL
        gravity = Gravity.CENTER_VERTICAL
        addView(nameText)
        addView(cityText)
    }

    private val dateColumn = LinearLayout(context).apply {
        orientation = VERTICAL
        gravity = Gravity.CENTER_VERTICAL or Gravity.END
        addView(hijriText)
        addView(gregorianText)
    }

    private val hairline = View(context)

    /** Identity and date on one row, hairline beneath spanning the full width. */
    private val topRow = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(logoView)
        addView(identityColumn, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        addView(dateColumn, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
    }

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_VERTICAL
        addView(topRow, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        addView(
            hairline,
            LayoutParams(LayoutParams.MATCH_PARENT, Scale.px(context, 2f))
        )
    }

    fun applyPalette(palette: Palette) {
        nameText.setTextColor(palette.textPrimary)
        cityText.setTextColor(palette.textMuted)
        hijriText.setTextColor(palette.accent)
        gregorianText.setTextColor(palette.textMuted)
        hairline.setBackgroundColor(palette.hairline)
    }

    fun bind(mosqueName: String, city: String, logoPath: String?, logoVisible: Boolean) {
        nameText.text = mosqueName
        cityText.text = city
        logoView.isVisible = logoVisible && logoPath != null
        if (logoVisible && logoPath != null) {
            Glide.with(this).load(logoPath).into(logoView)
        }
    }

    /** Called from the clock tick; both dates roll over at local midnight. */
    fun updateDate(nowMillis: Long, timeZoneId: String?, hijriDayOffset: Int) {
        hijriText.text = HijriDate.formatArabic(nowMillis, timeZoneId, hijriDayOffset)
        gregorianText.text = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID"))
            .apply { timeZone = timeZoneId?.let { TimeZone.getTimeZone(it) } ?: TimeZone.getDefault() }
            .format(Date(nowMillis))
    }
}
