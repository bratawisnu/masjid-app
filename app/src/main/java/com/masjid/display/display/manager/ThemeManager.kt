package com.masjid.display.display.manager

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.masjid.display.R
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.masjid.display.data.local.entity.AreaPosition
import com.masjid.display.data.local.entity.DisplayConfig
import com.masjid.display.data.local.entity.MainContentType
import com.masjid.display.data.local.entity.ThemeConfig
import com.masjid.display.display.design.BackgroundScrim
import com.masjid.display.display.design.Palette
import com.masjid.display.display.design.Scale
import java.io.File
import com.masjid.display.display.view.AnnouncementBoardView
import com.masjid.display.display.view.ClockView
import com.masjid.display.display.view.HeaderView
import com.masjid.display.display.view.SlideShowView
import com.masjid.display.display.view.PrayerPanelView
import com.masjid.display.display.view.RunningTextView
import com.masjid.display.display.view.ScheduleBoardView

/**
 * Applies a [ThemeConfig] + [DisplayConfig] pair to the five area containers
 * declared in activity_display.xml. Themes only ever parameterize these
 * shared views — there is no per-theme layout/Activity.
 *
 * The theme's four colours are expanded into a [Palette] of seven roles here,
 * once per apply, and handed to each view.
 */
class ThemeManager(
    private val rootView: ConstraintLayout,
    private val headerContainer: FrameLayout,
    private val mainContainer: FrameLayout,
    private val clockContainer: FrameLayout,
    private val prayerContainer: FrameLayout,
    private val footerContainer: FrameLayout,
    private val backgroundImage: ImageView,
    private val backgroundScrim: View,
    /** Receives the theme's base colour so the Activity can tint its window. */
    private val onWindowBackground: ((Int) -> Unit)? = null
) {

    private val context = headerContainer.context

    val headerView = HeaderView(context)
    val clockView = ClockView(context)
    val prayerPanelView = PrayerPanelView(context)
    val runningTextView = RunningTextView(context)
    val slideShowView = SlideShowView(context)
    val scheduleBoardView = ScheduleBoardView(context)
    val announcementBoardView = AnnouncementBoardView(context)

    var palette: Palette? = null
        private set

    /** Guards against a slow decode landing after the admin cleared the image. */
    private var backgroundRequestPath: String? = null

    /**
     * Null until the first theme applies. Re-running the ConstraintSet is a
     * full re-layout of the display, and applyTheme fires on every config
     * save — so it only runs when the arrangement actually differs.
     */
    private var appliedPanelPosition: AreaPosition? = null

    init {
        headerContainer.addView(headerView, matchParent())
        clockContainer.addView(clockView, wrapContent().apply { gravity = Gravity.CENTER })
        // Height is set by setArrangement — a band wraps its bays, a rail
        // fills the column.
        prayerContainer.addView(
            prayerPanelView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        )
        footerContainer.addView(runningTextView, matchParent())

        // All three stage views are added once and swapped by visibility.
        // Adding and removing them per theme change would re-run the slider's
        // Glide loads and restart the announcement cycle on every unrelated
        // config save.
        mainContainer.addView(slideShowView, matchParent())
        mainContainer.addView(scheduleBoardView, matchParent())
        mainContainer.addView(announcementBoardView, matchParent())

        applySafeArea()
    }

    /**
     * Insets content from the panel edges. Older TVs overscan by 3–5% per
     * edge and crop whatever sits there — without this the mosque name and
     * the ticker are the first things lost.
     */
    private fun applySafeArea() {
        val horizontal = Scale.safeInsetHorizontal(context)
        val vertical = Scale.safeInsetVertical(context)
        val gutter = Scale.dim(context, Scale.GUTTER)
        headerContainer.setPadding(horizontal, vertical, horizontal, gutter)
        mainContainer.setPadding(horizontal, 0, gutter, gutter)
        clockContainer.setPadding(0, 0, horizontal, gutter)
        // The arcade is now sized by its own content, so this bottom inset is
        // the whole gap between the bays and the ticker — without it the two
        // touch and the arcade stops reading as a separate band.
        prayerContainer.setPadding(horizontal, 0, horizontal, gutter)
        footerContainer.setPadding(horizontal, 0, horizontal, vertical)
    }

    /**
     * Re-runs the constraint graph for the theme's prayer-panel position.
     *
     * BOTTOM is the authored arrangement: a full-width band under the stage.
     * LEFT/RIGHT turn that band into a full-height rail beside the stage, and
     * the stage and clock give up the width. Everything is expressed against
     * the guidelines already in the layout, so no view is re-parented and the
     * background/overlay layers are untouched.
     */
    private fun applyPanelArrangement(position: AreaPosition) {
        if (position == appliedPanelPosition) return
        appliedPanelPosition = position

        val set = ConstraintSet().apply { clone(rootView) }
        val horizontal = Scale.safeInsetHorizontal(context)
        val vertical = Scale.safeInsetVertical(context)

        when (position) {
            AreaPosition.LEFT, AreaPosition.RIGHT -> {
                val onLeft = position == AreaPosition.LEFT

                // The rail runs from under the header to the ticker: a
                // half-height rail would leave the seven bays crushed and the
                // remaining space empty. Unlike the band, it takes its height
                // from the constraints rather than its content.
                set.constrainHeight(R.id.container_prayer, 0)
                set.connect(R.id.container_prayer, ConstraintSet.TOP, R.id.guideline_header_bottom, ConstraintSet.BOTTOM)
                set.connect(R.id.container_prayer, ConstraintSet.BOTTOM, R.id.guideline_prayer_bottom, ConstraintSet.TOP)
                if (onLeft) {
                    set.connect(R.id.container_prayer, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                    set.connect(R.id.container_prayer, ConstraintSet.END, R.id.guideline_rail_end, ConstraintSet.START)
                } else {
                    set.connect(R.id.container_prayer, ConstraintSet.START, R.id.guideline_rail_start, ConstraintSet.END)
                    set.connect(R.id.container_prayer, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                }

                // Stage and clock drop to the band the rail doesn't occupy,
                // and both grow downward to the ticker now that the panel no
                // longer needs a band of its own.
                set.connect(R.id.container_main, ConstraintSet.BOTTOM, R.id.guideline_prayer_bottom, ConstraintSet.TOP)
                set.connect(R.id.container_clock, ConstraintSet.BOTTOM, R.id.guideline_prayer_bottom, ConstraintSet.TOP)

                if (onLeft) {
                    set.connect(R.id.container_main, ConstraintSet.START, R.id.guideline_rail_end, ConstraintSet.END)
                    set.connect(R.id.container_main, ConstraintSet.END, R.id.guideline_clock_start, ConstraintSet.START)
                    set.connect(R.id.container_clock, ConstraintSet.START, R.id.guideline_clock_start, ConstraintSet.END)
                    set.connect(R.id.container_clock, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                } else {
                    // Clock moves to the inside edge so it isn't sandwiched
                    // between the stage and the rail.
                    set.connect(R.id.container_clock, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                    set.connect(R.id.container_clock, ConstraintSet.END, R.id.guideline_clock_end, ConstraintSet.START)
                    set.connect(R.id.container_main, ConstraintSet.START, R.id.guideline_clock_end, ConstraintSet.END)
                    set.connect(R.id.container_main, ConstraintSet.END, R.id.guideline_rail_start, ConstraintSet.START)
                }
            }

            else -> {
                // Back to the authored bands. The arcade hangs off the ticker
                // and is only as tall as its bays — so its top edge is left
                // unconstrained, and the stage and clock come down to meet it
                // wherever that lands.
                set.clear(R.id.container_prayer, ConstraintSet.TOP)
                set.constrainHeight(R.id.container_prayer, ConstraintSet.WRAP_CONTENT)
                set.connect(R.id.container_prayer, ConstraintSet.BOTTOM, R.id.guideline_prayer_bottom, ConstraintSet.TOP)
                set.connect(R.id.container_prayer, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                set.connect(R.id.container_prayer, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

                set.connect(R.id.container_main, ConstraintSet.BOTTOM, R.id.container_prayer, ConstraintSet.TOP)
                set.connect(R.id.container_main, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                set.connect(R.id.container_main, ConstraintSet.END, R.id.guideline_clock_start, ConstraintSet.START)

                set.connect(R.id.container_clock, ConstraintSet.BOTTOM, R.id.container_prayer, ConstraintSet.TOP)
                set.connect(R.id.container_clock, ConstraintSet.START, R.id.guideline_clock_start, ConstraintSet.END)
                set.connect(R.id.container_clock, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            }
        }

        set.applyTo(rootView)
        prayerPanelView.setArrangement(position)

        // applyTo() rebuilds LayoutParams, which does not touch padding — but
        // the rail needs a different inset than the band did, so re-run it.
        applySafeArea()
        when (position) {
            AreaPosition.LEFT -> prayerContainer.setPadding(horizontal, vertical, 0, vertical)
            AreaPosition.RIGHT -> prayerContainer.setPadding(0, vertical, horizontal, vertical)
            else -> Unit
        }
    }

    fun applyTheme(theme: ThemeConfig, config: DisplayConfig) {
        val palette = Palette.from(theme)
        this.palette = palette

        rootView.setBackgroundColor(palette.base)
        // The root is nudged a few pixels for burn-in protection, which would
        // otherwise expose the window's own background along one edge — on a
        // light theme that reads as a black sliver.
        onWindowBackground?.invoke(palette.base)

        applyBackgroundImage(config.backgroundImagePath, palette)

        headerView.applyPalette(palette)
        clockView.applyPalette(palette)
        prayerPanelView.applyPalette(palette)
        runningTextView.applyPalette(palette)
        slideShowView.applyPalette(palette)
        scheduleBoardView.applyPalette(palette)
        announcementBoardView.applyPalette(palette)

        applyPanelArrangement(theme.prayerPanelPosition)

        (clockView.layoutParams as? FrameLayout.LayoutParams)?.let { params ->
            params.gravity = theme.clockPosition.toGravity()
            clockView.layoutParams = params
        }

        clockView.isVisible = config.clockVisible
        prayerPanelView.isVisible = config.prayerPanelVisible
        runningTextView.isVisible = config.runningTextVisible

        applyMainContent(theme.mainContentType, config)
    }

    /**
     * Shows exactly one stage view.
     *
     * Ten of the fifteen themes ask for PRAYER_SCHEDULE or ANNOUNCEMENT, and
     * until now only the slider existed — so those themes rendered the largest
     * area of the screen empty. The hidden views are stopped rather than left
     * running: an invisible slider still holds decoded bitmaps and still posts
     * its advance every few seconds.
     */
    private fun applyMainContent(type: MainContentType, config: DisplayConfig) {
        val showSlider = type == MainContentType.IMAGE_SLIDER && config.sliderVisible
        val showSchedule = type == MainContentType.PRAYER_SCHEDULE
        val showAnnouncement = type == MainContentType.ANNOUNCEMENT

        slideShowView.isVisible = showSlider
        scheduleBoardView.isVisible = showSchedule
        announcementBoardView.isVisible = showAnnouncement

        if (showSlider) slideShowView.resume() else slideShowView.stop()
        if (showAnnouncement) announcementBoardView.resume() else announcementBoardView.stop()
    }

    /** Restarts whichever stage view is currently showing, after an onStop. */
    fun restartMainContent() {
        if (slideShowView.isVisible) slideShowView.resume()
        if (announcementBoardView.isVisible) announcementBoardView.resume()
    }

    /**
     * Shows [path] behind the display, dimmed by however much that particular
     * photo needs. Falls back to the theme's flat colour when there is no
     * image, when the file has gone missing, or when the decode fails —
     * the panel must never end up showing nothing.
     */
    private fun applyBackgroundImage(path: String?, palette: Palette) {
        backgroundRequestPath = path

        if (path == null || !File(path).exists()) {
            clearBackgroundImage()
            return
        }

        // Capped at panel size: the source may be a 12 MP phone photo, and a
        // TV box has no headroom to hold that decoded for the lifetime of the
        // display. centerCrop in the layout handles the aspect difference.
        Glide.with(rootView)
            .asBitmap()
            .load(File(path))
            .override(Scale.widthPx(context), Scale.heightPx(context))
            .centerCrop()
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    // The admin may have cleared or replaced the image while
                    // this decode was in flight; a stale bitmap would paint
                    // over the newer choice.
                    if (backgroundRequestPath != path) return
                    showBackground(resource, palette)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    backgroundImage.setImageDrawable(null)
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    // Corrupt or unreadable file — the theme colour still works.
                    if (backgroundRequestPath == path) clearBackgroundImage()
                }
            })
    }

    private fun showBackground(bitmap: Bitmap, palette: Palette) {
        backgroundImage.setImageDrawable(BitmapDrawable(rootView.resources, bitmap))
        backgroundImage.isVisible = true

        // Measured per image rather than fixed: a flat scrim strong enough for
        // a bright photo would erase a dark one, and vice versa.
        backgroundScrim.setBackgroundColor(palette.base)
        backgroundScrim.alpha = BackgroundScrim.alphaFor(bitmap, palette)
        backgroundScrim.isVisible = true
    }

    private fun clearBackgroundImage() {
        backgroundImage.setImageDrawable(null)
        backgroundImage.isVisible = false
        backgroundScrim.isVisible = false
    }

    private fun AreaPosition.toGravity(): Int = when (this) {
        AreaPosition.TOP -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
        AreaPosition.BOTTOM -> Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        AreaPosition.LEFT -> Gravity.START or Gravity.CENTER_VERTICAL
        AreaPosition.RIGHT -> Gravity.END or Gravity.CENTER_VERTICAL
        AreaPosition.CENTER -> Gravity.CENTER
    }

    private fun matchParent() = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT
    )

    private fun wrapContent() = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.WRAP_CONTENT,
        FrameLayout.LayoutParams.WRAP_CONTENT
    )
}
