package com.masjid.display.display.view

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.OptIn
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.bumptech.glide.Glide
import com.masjid.display.data.local.entity.Slide
import com.masjid.display.data.local.entity.SlideType
import com.masjid.display.display.design.Palette
import java.io.File

/**
 * The stage: text, image and video slides, cross-faded into one another.
 *
 * Two content layers sit above a single video surface. A slide change fades
 * the incoming layer over the outgoing one — on an ambient screen a hard cut
 * registers as a glitch in peripheral vision, a 400 ms dissolve does not.
 *
 * ```
 *   ┌─ PlayerView   ← one, at the back
 *   ├─ layer A      (image + text)  ┐ cross-fade
 *   └─ layer B      (image + text)  ┘
 * ```
 *
 * Video is revealed by fading *both* layers away rather than by giving video
 * its own layer: two video surfaces would double the decoder load, and a cheap
 * TV box running this for twenty-four hours a day has no headroom for that.
 * Leaving a video works the same way in reverse — the next slide fades in over
 * the still-playing video and only then is the player stopped, so the seam is
 * never visible.
 */
// PlayerView and its resize modes are still @UnstableApi in media3 1.7.1.
@OptIn(markerClass = [UnstableApi::class])
class SlideShowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private companion object {
        const val CROSSFADE_MILLIS = 400L
    }

    /** One cross-fade layer: an image and a text block, one of them showing. */
    private inner class Layer {
        val root = FrameLayout(context).apply {
            layoutParams = fill()
            alpha = 0f
        }
        val image = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = fill()
            isVisible = false
        }
        val text = SlideTextView(context).apply {
            layoutParams = fill()
            isVisible = false
        }

        init {
            root.addView(image)
            root.addView(text)
        }
    }

    private fun fill() = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

    private val playerView = PlayerView(context).apply {
        layoutParams = fill()
        useController = false
        // The stage is not the video's aspect ratio, and letterboxing is the
        // honest answer — cropping a recorded khutbah cuts the speaker's head off.
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
        isVisible = false
    }

    private val layerA = Layer()
    private val layerB = Layer()

    /** The layer currently showing; the other is the one to fade in next. */
    private var frontLayer = layerA

    private val handler = Handler(Looper.getMainLooper())
    private val advanceRunnable = Runnable { advance() }

    private var slides: List<Slide> = emptyList()
    private var currentIndex = 0

    /**
     * Videos that have failed back-to-back without anything playing in
     * between. A dead network fails every URL slide in the list, and skipping
     * on each failure would spin the whole list as fast as ExoPlayer can
     * report errors; past [slides].size the show gives up until the next edit.
     */
    private var consecutiveVideoFailures = 0

    /**
     * Created on the first video slide rather than up front. A mosque showing
     * only photos should not be holding a decoder open all day.
     */
    private var player: ExoPlayer? = null

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            when (state) {
                // Something is actually on screen, so the run of failures that
                // may have led here is over.
                Player.STATE_READY -> consecutiveVideoFailures = 0
                // The video's own length is the slide's duration;
                // durationSeconds would either cut it off or leave a frozen
                // last frame on screen.
                Player.STATE_ENDED -> advance()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            // A missing file or a dead network must not strand the display on
            // a black rectangle — the rest of the slides are still fine.
            consecutiveVideoFailures++
            if (consecutiveVideoFailures > slides.size) {
                // Every slide has now failed in turn. Skipping again would
                // just spin the list at error speed; hold instead and wait for
                // the next admin edit or restart.
                stopPlayback()
                return
            }
            advance()
        }
    }

    init {
        addView(playerView)
        addView(layerA.root)
        addView(layerB.root)
    }

    fun applyPalette(palette: Palette) {
        layerA.text.applyPalette(palette)
        layerB.text.applyPalette(palette)
    }

    fun setSlides(newSlides: List<Slide>) {
        stop()
        // Admin edits re-emit the whole list; restarting from slide 1 on every
        // unrelated config save would be visible on screen.
        val unchanged = newSlides.map(::identityOf) == slides.map(::identityOf)
        slides = newSlides
        // A fresh list is a fresh chance — the caretaker may have just fixed
        // the very URL that was failing.
        consecutiveVideoFailures = 0

        if (slides.isEmpty()) {
            stopPlayback()
            layerA.root.alpha = 0f
            layerB.root.alpha = 0f
            currentIndex = 0
            return
        }

        if (!unchanged || currentIndex >= slides.size) currentIndex = 0
        showCurrent(animate = false)
    }

    private fun identityOf(slide: Slide) = "${slide.type}|${slide.source}|${slide.body}|${slide.durationSeconds}"

    private fun showCurrent(animate: Boolean) {
        val slide = slides.getOrNull(currentIndex) ?: return
        if (slide.type == SlideType.VIDEO) showVideo(slide) else showStill(slide, animate)
    }

    /** An image or a text slide: fades in over whatever is behind it. */
    private fun showStill(slide: Slide, animate: Boolean) {
        val incoming = if (frontLayer === layerA) layerB else layerA
        val outgoing = frontLayer

        incoming.root.animate().cancel()
        outgoing.root.animate().cancel()

        when (slide.type) {
            SlideType.TEXT -> {
                incoming.text.setBody(slide.body)
                incoming.text.isVisible = true
                incoming.image.isVisible = false
                Glide.with(this).clear(incoming.image)
            }
            else -> {
                Glide.with(this).load(slide.path?.let(::File)).into(incoming.image)
                incoming.image.isVisible = true
                incoming.text.isVisible = false
            }
        }

        incoming.root.bringToFront()

        // The video keeps playing under the fade and is only stopped once the
        // new slide fully covers it, so the switch never shows a blank frame.
        val coverVideo = { stopPlayback() }

        if (animate) {
            incoming.root.alpha = 0f
            incoming.root.animate()
                .alpha(1f)
                .setDuration(CROSSFADE_MILLIS)
                .withEndAction(coverVideo)
                .start()
            outgoing.root.animate().alpha(0f).setDuration(CROSSFADE_MILLIS).start()
        } else {
            incoming.root.alpha = 1f
            outgoing.root.alpha = 0f
            coverVideo()
        }

        frontLayer = incoming
        scheduleAdvance(slide.durationSeconds * 1000L)
    }

    /** A video slide: both layers fade away and the surface underneath plays. */
    private fun showVideo(slide: Slide) {
        val source = slide.source
        if (source == null) {
            // Saved without a payload — nothing to play, so don't sit on it.
            advance()
            return
        }

        val exo = player ?: ExoPlayer.Builder(context).build().also {
            // Silent by design. This screen runs all day in a prayer hall, and
            // a video soundtrack would collide with the adzan and the imam.
            it.volume = 0f
            it.addListener(playerListener)
            playerView.player = it
            player = it
        }

        val uri = if (source.startsWith("http")) Uri.parse(source) else Uri.fromFile(File(source))
        exo.setMediaItem(MediaItem.fromUri(uri))
        // A lone video has nothing to advance to, so it loops rather than
        // ending on a frozen frame.
        exo.repeatMode = if (slides.size > 1) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ONE
        exo.prepare()
        exo.playWhenReady = true
        playerView.isVisible = true

        layerA.root.animate().cancel()
        layerB.root.animate().cancel()
        layerA.root.animate().alpha(0f).setDuration(CROSSFADE_MILLIS).start()
        layerB.root.animate().alpha(0f).setDuration(CROSSFADE_MILLIS).start()

        // No timed advance: STATE_ENDED drives it.
        handler.removeCallbacks(advanceRunnable)
    }

    private fun scheduleAdvance(delayMillis: Long) {
        handler.removeCallbacks(advanceRunnable)
        // A single slide has nothing to advance to; leave it up.
        if (slides.size > 1) handler.postDelayed(advanceRunnable, delayMillis)
    }

    private fun advance() {
        if (slides.isEmpty()) return
        currentIndex = (currentIndex + 1) % slides.size
        showCurrent(animate = true)
    }

    /** Releases the surface but keeps the player, so [resume] can pick up again. */
    private fun stopPlayback() {
        player?.let {
            if (it.isPlaying || it.playWhenReady) it.stop()
        }
        playerView.isVisible = false
    }

    fun stop() {
        handler.removeCallbacks(advanceRunnable)
        player?.playWhenReady = false
    }

    /**
     * Restarts the cycle from the slide already on screen. Called when the
     * stage comes back — after [stop] the pending advance is gone, and without
     * this the display would sit frozen on one slide forever.
     */
    fun resume() {
        if (slides.isEmpty()) return
        val slide = slides.getOrNull(currentIndex) ?: return
        if (slide.type == SlideType.VIDEO) {
            val exo = player
            // A paused player still holds its position, so it picks up where
            // it left off rather than replaying the opening of a lecture. A
            // released or stopped one has nothing left to resume, so the slide
            // is set up again from scratch.
            if (exo != null && exo.playbackState != Player.STATE_IDLE) {
                exo.playWhenReady = true
                playerView.isVisible = true
            } else {
                showVideo(slide)
            }
        } else {
            scheduleAdvance(slide.durationSeconds * 1000L)
        }
    }

    /**
     * Hands back the codec and the surface. The Activity must call this from
     * onDestroy — an ExoPlayer that outlives its view leaks both.
     */
    fun release() {
        stop()
        player?.let {
            it.removeListener(playerListener)
            it.release()
        }
        player = null
        playerView.player = null
    }
}
