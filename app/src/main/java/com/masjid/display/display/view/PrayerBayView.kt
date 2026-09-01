package com.masjid.display.display.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.masjid.display.display.design.Palette
import com.masjid.display.display.design.Scale
import com.masjid.display.display.design.Typography

/**
 * One bay of the prayer arcade: label, time, Arabic name, and — when this is
 * the next prayer — a live countdown.
 *
 * The active bay is the screen's signature element. It rises out of the row
 * onto a raised surface with a soft radial glow behind it, so the next prayer
 * reads from across the hall without anyone parsing seven times. Everything
 * else in the row stays flat and quiet.
 *
 * [riseFraction] drives both the surface and the content offset, so the whole
 * bay moves as one object when the active prayer changes.
 */
class PrayerBayView(context: Context) : LinearLayout(context) {

    /**
     * Which way the active bay leaves the row.
     *
     * In the horizontal arcade it rises [UP] out of the band. When a theme
     * puts the panel in a side rail the same gesture has to travel sideways,
     * always *into* the content rather than off the screen edge — so a rail on
     * the left pushes [RIGHT] and one on the right pushes [LEFT].
     */
    enum class RiseAxis { UP, RIGHT, LEFT }

    private val surfacePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val surfaceRect = RectF()
    private val glowMatrix = Matrix()
    private var glowRadius = 0f

    private val cornerRadius = Scale.px(context, 14f).toFloat()

    private val riseDistance = Scale.dim(context, Scale.BAY_RISE)

    private var palette: Palette? = null

    /** Null until the first apply, so a new palette always repaints. */
    private var colorsAppliedActive: Boolean? = null

    var riseAxis: RiseAxis = RiseAxis.UP
        set(value) {
            if (field == value) return
            field = value
            // The previous axis left a translation on the children; re-running
            // the rise clears it and re-applies along the new one.
            applyRise()
            (parent as? View)?.invalidate() ?: invalidate()
        }

    /** 0f = flat in the row, 1f = fully raised as the mihrab bay. */
    var riseFraction: Float = 0f
        set(value) {
            val clamped = value.coerceIn(0f, 1f)
            if (field == clamped) return
            field = clamped
            applyRise()
            // This view paints its glow well outside its own bounds, so
            // invalidating only itself leaves the spill area stale. Redrawing
            // through the parent covers the full affected region.
            (parent as? View)?.invalidate() ?: invalidate()
        }

    private val labelText = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, Scale.text(context, Scale.PRAYER_LABEL))
        typeface = Typography.label(context)
        gravity = Gravity.CENTER
        includeFontPadding = false
        letterSpacing = 0.12f
    }

    private val timeText = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, Scale.text(context, Scale.PRAYER_TIME))
        typeface = Typography.numeric(context)
        gravity = Gravity.CENTER
        includeFontPadding = false
        text = "--:--"
    }

    private val arabicText = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, Scale.text(context, Scale.PRAYER_ARABIC))
        typeface = Typography.arabic(context)
        gravity = Gravity.CENTER
        includeFontPadding = false
    }

    /**
     * Held at INVISIBLE rather than GONE on inactive bays so every bay keeps
     * the same intrinsic height — the rise is then purely the animated offset
     * and the row never reflows.
     */
    private val countdownText = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, Scale.text(context, Scale.COUNTDOWN))
        typeface = Typography.numeric(context)
        gravity = Gravity.CENTER
        includeFontPadding = false
        visibility = INVISIBLE
        text = "00:00:00"
    }

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        setWillNotDraw(false)
        // The glow and the raised surface both paint outside these bounds;
        // without this the parent clips the invalidation region to the bay
        // and leaves smears behind as the rise animates.
        clipChildren = false
        clipToPadding = false
        // Symmetric top and bottom: the content is centred in the raised
        // surface, and any difference here would show up as the bay sitting
        // high or low inside its own block.
        setPadding(
            Scale.px(context, 10f),
            Scale.px(context, 14f),
            Scale.px(context, 10f),
            Scale.px(context, 14f)
        )
        addView(labelText)
        addView(timeText)
        addView(arabicText)
        addView(countdownText)
    }

    fun applyPalette(palette: Palette) {
        this.palette = palette
        surfacePaint.color = palette.surface
        buildGlowShader()
        // Force the colour pass — the active state hasn't changed, but every
        // colour behind it has.
        colorsAppliedActive = null
        applyTextColors()
        invalidate()
    }

    fun bind(label: String, arabic: String, time: String) {
        labelText.text = label
        arabicText.text = arabic
        timeText.text = time
    }

    fun setCountdown(text: String?) {
        if (text == null) {
            countdownText.visibility = INVISIBLE
        } else {
            countdownText.visibility = VISIBLE
            countdownText.text = text
        }
    }

    private fun applyRise() {
        val offset = riseDistance * riseFraction
        val offsetX = when (riseAxis) {
            RiseAxis.UP -> 0f
            RiseAxis.RIGHT -> offset
            RiseAxis.LEFT -> -offset
        }
        val offsetY = if (riseAxis == RiseAxis.UP) -offset else 0f
        for (i in 0 until childCount) {
            getChildAt(i).translationX = offsetX
            getChildAt(i).translationY = offsetY
        }
        // The active time grows slightly along with the rise, so the emphasis
        // comes from the bay as a whole rather than a separate text swap.
        val growth = 1f + (Scale.PRAYER_TIME_ACTIVE / Scale.PRAYER_TIME - 1f) * riseFraction
        timeText.scaleX = growth
        timeText.scaleY = growth
        applyTextColors()
    }

    /**
     * Colours snap at the halfway point of the rise rather than interpolating,
     * and only when that threshold is actually crossed — [applyRise] runs on
     * every animation frame for all seven bays, and setTextColor invalidates
     * the TextView even when handed the colour it already has.
     */
    private fun applyTextColors() {
        val palette = palette ?: return
        val active = riseFraction > 0.5f
        if (active == colorsAppliedActive) return
        colorsAppliedActive = active

        labelText.setTextColor(if (active) palette.accent else palette.textMuted)
        timeText.setTextColor(if (active) palette.accent else palette.textPrimary)
        arabicText.setTextColor(palette.textMuted)
        countdownText.setTextColor(palette.accent)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        buildGlowShader()
    }

    /**
     * The gradient is built once per size/palette rather than per frame —
     * this view redraws every frame of the 600 ms rise, and allocating a
     * shader in onDraw is exactly the GC pressure that shows up as jank on
     * low-end TV boxes. Position is applied later via a local matrix.
     */
    private fun buildGlowShader() {
        val palette = palette ?: return
        if (width == 0) return
        // Keyed to the bay's longer edge. In the horizontal arcade that is the
        // width; in a vertical rail a bay is wide and short, and sizing off
        // width alone would leave the glow barely wider than the surface.
        glowRadius = maxOf(width, height) * 0.9f
        glowPaint.shader = RadialGradient(
            0f,
            0f,
            glowRadius,
            palette.accentGlow,
            Palette.withAlpha(palette.accent, 0f),
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val palette = palette ?: return
        if (width == 0 || height == 0) return
        // Only the raised bay carries a surface. The other six are the plain
        // row it stands out from — giving them a surface too flattens the
        // contrast the whole panel is built on.
        if (riseFraction <= 0.01f) return

        // The surface keeps its full size and slides by the same offset the
        // content does, so the two stay locked together — holding the trailing
        // edge would stretch the block instead and leave the content sitting
        // high inside it.
        val offset = riseDistance * riseFraction
        when (riseAxis) {
            RiseAxis.UP -> surfaceRect.set(0f, -offset, width.toFloat(), height - offset)
            RiseAxis.RIGHT -> surfaceRect.set(offset, 0f, width + offset, height.toFloat())
            RiseAxis.LEFT -> surfaceRect.set(-offset, 0f, width - offset, height.toFloat())
        }
        val alpha = (255 * riseFraction).toInt()

        // Glow first and deliberately larger than the surface, so the bay
        // reads as lit from behind rather than outlined. The parent panel
        // disables child clipping, which lets this spill past the bay.
        glowPaint.shader?.let { shader ->
            glowMatrix.setTranslate(surfaceRect.centerX(), surfaceRect.centerY())
            shader.setLocalMatrix(glowMatrix)
            glowPaint.alpha = alpha
            canvas.drawRect(
                surfaceRect.left - glowRadius,
                surfaceRect.top - glowRadius,
                surfaceRect.right + glowRadius,
                surfaceRect.bottom + glowRadius,
                glowPaint
            )
        }

        surfacePaint.color = palette.surface
        surfacePaint.alpha = alpha
        canvas.drawRoundRect(surfaceRect, cornerRadius, cornerRadius, surfacePaint)
    }
}
