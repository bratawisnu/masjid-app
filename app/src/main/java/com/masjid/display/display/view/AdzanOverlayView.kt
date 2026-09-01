package com.masjid.display.display.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.masjid.display.display.design.Palette
import com.masjid.display.display.design.PrayerNaming
import com.masjid.display.display.design.Scale
import com.masjid.display.display.design.Typography
import com.masjid.display.prayer.Prayer
import java.util.Locale

/**
 * The one moment the screen stops reporting and starts announcing.
 *
 * Everything else on the display is ambient — glanced at, never demanded. This
 * takes the whole panel, and the design follows from that: the Arabic name set
 * at the largest size anywhere in the app, the Indonesian label reduced to an
 * eyebrow above it, and nothing else competing. The base colour is carried
 * over from the theme rather than a fixed dark, so a mosque running Minimal
 * Light doesn't get a black rectangle dropped on it mid-adzan.
 *
 * The glow behind the name is the same device as the mihrab bay's, at full
 * scale — the bay says "this prayer is next", this says "it is now".
 */
class AdzanOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private companion object {
        /** Long enough to register as an arrival, short enough not to delay it. */
        const val ENTER_MILLIS = 600L
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var glowRadius = 0f

    private var palette: Palette? = null

    private val eyebrowText = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, Scale.text(context, Scale.ADZAN_EYEBROW))
        typeface = Typography.label(context)
        gravity = Gravity.CENTER
        includeFontPadding = false
        letterSpacing = 0.34f
    }

    /**
     * The Arabic name carries the announcement. It is the form actually called
     * from the minaret, and at this size it reads from the back of the hall as
     * a shape rather than as text to be parsed.
     */
    private val arabicText = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, Scale.text(context, Scale.ADZAN_TITLE))
        typeface = Typography.arabic(context)
        gravity = Gravity.CENTER
        includeFontPadding = false
    }

    private val latinText = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, Scale.text(context, Scale.ADZAN_EYEBROW))
        typeface = Typography.label(context)
        gravity = Gravity.CENTER
        includeFontPadding = false
        letterSpacing = 0.20f
    }

    private val rule = View(context)

    /**
     * Only the iqamah state has a countdown. Held at GONE rather than
     * INVISIBLE so the adzan screen centres on the name itself instead of
     * around a reserved gap.
     */
    private val countdownText = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, Scale.text(context, Scale.ADZAN_COUNTDOWN))
        typeface = Typography.numeric(context)
        gravity = Gravity.CENTER
        includeFontPadding = false
        visibility = GONE
    }

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        setWillNotDraw(false)

        addView(eyebrowText, wrap())
        addView(arabicText, wrap())
        addView(latinText, wrap())
        addView(
            rule,
            LayoutParams(Scale.dim(context, 0.10f), Scale.px(context, 2f)).apply {
                topMargin = Scale.dim(context, Scale.GUTTER)
                gravity = Gravity.CENTER_HORIZONTAL
            }
        )
        addView(
            countdownText,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = Scale.dim(context, Scale.GUTTER)
                gravity = Gravity.CENTER_HORIZONTAL
            }
        )
    }

    fun applyPalette(palette: Palette) {
        this.palette = palette
        // Opaque: the display underneath must not read through, or the prayer
        // times behind the name turn the announcement into a busy screen.
        setBackgroundColor(palette.base)
        eyebrowText.setTextColor(palette.accent)
        arabicText.setTextColor(palette.textPrimary)
        latinText.setTextColor(palette.textMuted)
        rule.setBackgroundColor(palette.accent)
        countdownText.setTextColor(palette.accent)
        buildGlow()
        invalidate()
    }

    fun showAdzan(prayer: Prayer) {
        eyebrowText.text = "WAKTU ADZAN"
        arabicText.text = PrayerNaming.arabic(prayer)
        latinText.text = PrayerNaming.label(prayer)
        countdownText.isVisible = false
    }

    fun showIqamah(prayer: Prayer, remainingMillis: Long) {
        eyebrowText.text = "MENUJU IQAMAH"
        arabicText.text = PrayerNaming.arabic(prayer)
        latinText.text = PrayerNaming.label(prayer)
        countdownText.isVisible = true

        val totalSeconds = (remainingMillis / 1000).coerceAtLeast(0)
        countdownText.text = String.format(
            Locale.getDefault(),
            "%02d:%02d",
            totalSeconds / 60,
            totalSeconds % 60
        )
    }

    /**
     * Fades and lifts the content in. The overlay replaces the entire screen
     * without warning; a hard cut at the top of the hour reads as the display
     * having crashed and reloaded, which is exactly the wrong impression at
     * the moment the congregation looks up.
     */
    fun playEntry() {
        alpha = 0f
        animate().cancel()
        animate().alpha(1f).setDuration(ENTER_MILLIS).start()

        listOf(eyebrowText, arabicText, latinText, countdownText).forEachIndexed { index, view ->
            view.translationY = Scale.px(context, 24f).toFloat()
            view.animate().cancel()
            view.animate()
                .translationY(0f)
                // Staggered so the eye lands on the eyebrow, then the name —
                // simultaneous motion would arrive as one undifferentiated block.
                .setStartDelay(index * 70L)
                .setDuration(ENTER_MILLIS)
                .start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        buildGlow()
    }

    private fun buildGlow() {
        val palette = palette ?: return
        if (width == 0) return
        glowRadius = height * 0.55f
        glowPaint.shader = RadialGradient(
            width / 2f,
            height / 2f,
            glowRadius,
            // Stronger than the bay's: this glow has the whole panel to fill,
            // and at bay strength it would be invisible at this radius.
            Palette.withAlpha(palette.accent, 0.14f),
            Palette.withAlpha(palette.accent, 0f),
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (glowPaint.shader == null) return
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), glowPaint)
    }

    private fun wrap() = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
        gravity = Gravity.CENTER_HORIZONTAL
    }
}
