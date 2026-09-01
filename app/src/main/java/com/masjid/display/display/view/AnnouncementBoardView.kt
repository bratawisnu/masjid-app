package com.masjid.display.display.view

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.masjid.display.display.design.Palette
import com.masjid.display.display.design.Scale
import com.masjid.display.display.design.Typography

/**
 * One notice at a time, held large, for themes whose stage is ANNOUNCEMENT.
 *
 * The ticker at the foot of the screen already carries the same text, but a
 * marquee is only readable if you catch it mid-pass — you can't re-read a line
 * that has already left. On the stage the notice stays put for twelve seconds,
 * so someone glancing up at any moment gets the whole thing.
 *
 * The eyebrow and the position counter are the only chrome. A notice about a
 * funeral or a burst pipe shouldn't arrive inside a decorated card.
 */
class AnnouncementBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private companion object {
        /** Long enough to read three lines unhurried, short enough to cycle. */
        const val HOLD_MILLIS = 12_000L
        const val FADE_MILLIS = 500L
    }

    private val eyebrowText = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, Scale.text(context, Scale.ANNOUNCEMENT_EYEBROW))
        typeface = Typography.label(context)
        includeFontPadding = false
        letterSpacing = 0.22f
        text = "PENGUMUMAN"
    }

    private val rule = View(context)

    private val bodyText = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, Scale.text(context, Scale.ANNOUNCEMENT_BODY))
        typeface = Typography.heading(context)
        includeFontPadding = false
        // Four lines at this size fills the stage; anything longer belongs in
        // two notices, and ellipsis is a clearer failure than silent clipping.
        maxLines = 4
        ellipsize = android.text.TextUtils.TruncateAt.END
        // Notices run to full lines at this size; the default leading packs
        // them too tightly to scan from across the hall.
        setLineSpacing(0f, 1.12f)
    }

    private val counterText = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, Scale.text(context, Scale.ANNOUNCEMENT_EYEBROW))
        typeface = Typography.numeric(context)
        includeFontPadding = false
        letterSpacing = 0.08f
    }

    private val handler = Handler(Looper.getMainLooper())
    private val advanceRunnable = Runnable { advance() }

    private var items: List<String> = emptyList()
    private var index = 0

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_VERTICAL

        addView(eyebrowText)
        addView(
            rule,
            LayoutParams(LayoutParams.MATCH_PARENT, Scale.px(context, 2f)).apply {
                topMargin = Scale.px(context, 10f)
                bottomMargin = Scale.dim(context, Scale.GUTTER)
            }
        )
        addView(bodyText)
        addView(
            counterText,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = Scale.dim(context, Scale.GUTTER)
            }
        )
    }

    fun applyPalette(palette: Palette) {
        eyebrowText.setTextColor(palette.accent)
        rule.setBackgroundColor(palette.hairline)
        bodyText.setTextColor(palette.textPrimary)
        counterText.setTextColor(palette.textMuted)
    }

    fun setItems(newItems: List<String>) {
        stop()
        val unchanged = newItems == items
        items = newItems
        if (items.isEmpty()) {
            // An empty stage is worse than an honest one — a caretaker seeing
            // nothing can't tell the difference between "no notices" and
            // "the display is broken".
            bodyText.text = "Belum ada pengumuman."
            bodyText.alpha = 1f
            counterText.text = ""
            return
        }
        // Toggling an unrelated switch re-emits the whole list; restarting the
        // cycle would visibly jump the notice on screen.
        if (!unchanged) index = 0
        show(animate = false)
    }

    private fun show(animate: Boolean) {
        val text = items.getOrNull(index) ?: return
        bodyText.animate().cancel()

        if (animate) {
            bodyText.animate()
                .alpha(0f)
                .setDuration(FADE_MILLIS / 2)
                .withEndAction {
                    bodyText.text = text
                    updateCounter()
                    bodyText.animate().alpha(1f).setDuration(FADE_MILLIS / 2).start()
                }
                .start()
        } else {
            bodyText.text = text
            bodyText.alpha = 1f
            updateCounter()
        }

        // A single notice has nothing to cycle to; leave it up permanently.
        if (items.size > 1) handler.postDelayed(advanceRunnable, HOLD_MILLIS)
    }

    private fun updateCounter() {
        counterText.text = if (items.size > 1) "${index + 1} / ${items.size}" else ""
    }

    private fun advance() {
        if (items.isEmpty()) return
        index = (index + 1) % items.size
        show(animate = true)
    }

    fun stop() {
        handler.removeCallbacks(advanceRunnable)
    }

    /**
     * Restarts the cycle from the notice already shown. Without this a board
     * that was hidden and shown again would hold one notice permanently, and
     * the rest would never be seen.
     */
    fun resume() {
        if (items.size <= 1) return
        stop()
        handler.postDelayed(advanceRunnable, HOLD_MILLIS)
    }
}
