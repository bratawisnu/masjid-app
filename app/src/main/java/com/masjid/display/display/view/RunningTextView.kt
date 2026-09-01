package com.masjid.display.display.view

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.masjid.display.display.design.Palette
import com.masjid.display.display.design.Scale
import com.masjid.display.display.design.Typography

/**
 * Marquee driven by a single TranslationX ObjectAnimator per the doc's own
 * recommendation against heavier custom animation on TV hardware.
 *
 * A hairline above the text ties the ticker to the header's datum line, so
 * the announcement band reads as the base of the composition rather than a
 * strip floating at the bottom.
 */
class RunningTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val textView = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, Scale.text(context, Scale.RUNNING_TEXT))
        typeface = Typography.body(context)
        maxLines = 1
        includeFontPadding = false
        gravity = Gravity.CENTER_VERTICAL
    }

    private val hairline = View(context)

    private val track = FrameLayout(context).apply {
        clipChildren = true
        addView(textView)
    }

    private var animator: Animator? = null
    private var pendingItems: List<String> = emptyList()
    private var currentIndex = 0

    init {
        orientation = VERTICAL
        addView(hairline, LayoutParams(LayoutParams.MATCH_PARENT, Scale.px(context, 2f)))
        addView(track, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
    }

    fun applyPalette(palette: Palette) {
        textView.setTextColor(palette.textPrimary)
        hairline.setBackgroundColor(palette.hairline)
    }

    /**
     * [speedReferencePxPerSec] is authored against a 1080p panel and scaled to
     * the actual one, so the text crosses the screen at the same apparent
     * speed — and stays readable — on 720p through 4K.
     */
    fun setItems(items: List<String>, speedReferencePxPerSec: Int = 150) {
        animator?.cancel()
        pendingItems = items
        currentIndex = 0
        if (items.isEmpty()) {
            textView.text = ""
            return
        }
        val speed = Scale.px(context, speedReferencePxPerSec.toFloat()).coerceAtLeast(1)
        post { playNext(speed) }
    }

    private fun playNext(speedPxPerSec: Int) {
        val trackWidth = track.width
        if (pendingItems.isEmpty() || trackWidth == 0) return
        val text = pendingItems[currentIndex % pendingItems.size]
        currentIndex++

        textView.text = text
        textView.measure(
            View.MeasureSpec.UNSPECIFIED,
            View.MeasureSpec.UNSPECIFIED
        )
        val textWidth = textView.measuredWidth

        textView.translationX = trackWidth.toFloat()
        val distance = trackWidth + textWidth
        val durationMs = (distance.toFloat() / speedPxPerSec * 1000).toLong().coerceAtLeast(1000L)

        animator = ObjectAnimator.ofFloat(textView, "translationX", trackWidth.toFloat(), -textWidth.toFloat()).apply {
            duration = durationMs
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationEnd(animation: Animator) {
                    playNext(speedPxPerSec)
                }
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
            start()
        }
    }

    fun stop() {
        animator?.cancel()
        animator = null
    }

    /**
     * Restarts the marquee after a [stop]. The item list is unchanged, so this
     * picks up where the cycle left off rather than jumping back to the first
     * announcement.
     */
    fun resume(speedReferencePxPerSec: Int = 150) {
        if (pendingItems.isEmpty() || animator?.isRunning == true) return
        val speed = Scale.px(context, speedReferencePxPerSec.toFloat()).coerceAtLeast(1)
        post { playNext(speed) }
    }
}
