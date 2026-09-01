package com.masjid.display.display

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.masjid.display.admin.PinEntryActivity
import com.masjid.display.core.NtpDriftManager
import com.masjid.display.data.local.AppDatabase
import com.masjid.display.data.local.entity.DisplayConfig
import com.masjid.display.data.local.entity.MosqueProfile
import com.masjid.display.data.local.entity.PrayerSchedule
import com.masjid.display.data.repository.MosqueProfileRepository
import com.masjid.display.databinding.ActivityDisplayBinding
import com.masjid.display.display.design.Scale
import com.masjid.display.display.manager.ClockManager
import com.masjid.display.display.manager.ScreensaverScheduler
import com.masjid.display.display.manager.ThemeManager
import com.masjid.display.display.view.AdzanOverlayView
import com.masjid.display.display.viewmodel.DisplayViewModel
import com.masjid.display.prayer.PrayerState
import com.masjid.display.setup.SetupWizardActivity
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch

class DisplayActivity : AppCompatActivity() {

    companion object {
        private const val LONG_PRESS_ADMIN_THRESHOLD_MS = 7000L
        private const val BURN_IN_CYCLE_MILLIS = 15 * 60_000L
    }

    private lateinit var binding: ActivityDisplayBinding
    private lateinit var themeManager: ThemeManager
    private lateinit var clockManager: ClockManager
    private lateinit var adzanOverlayView: AdzanOverlayView

    private val viewModel: DisplayViewModel by lazy {
        ViewModelProvider(
            this,
            DisplayViewModel.Factory(AppDatabase.getInstance(applicationContext))
        )[DisplayViewModel::class.java]
    }

    private var mosqueProfile: MosqueProfile? = null
    private var displayConfig: DisplayConfig? = null
    private var displayInitialized = false
    private var menuKeyDownTime: Long = 0

    /** The entry transition plays once, on the first theme applied after boot. */
    private var entryPlayed = false

    /** Burn-in drift radius, resolved once — this runs on every clock tick. */
    private val driftAmplitudePx: Float by lazy { Scale.px(this, 8f).toFloat() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            AppDatabase.ensureThemesSeeded(applicationContext)
            val hasProfile = MosqueProfileRepository(
                AppDatabase.getInstance(applicationContext).mosqueProfileDao()
            ).get() != null
            if (!hasProfile) {
                startActivity(Intent(this@DisplayActivity, SetupWizardActivity::class.java))
                finish()
                return@launch
            }
            initDisplay()
        }
    }

    private fun initDisplay() {
        binding = ActivityDisplayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        applyImmersiveMode()

        themeManager = ThemeManager(
            binding.displayRoot,
            binding.containerHeader,
            binding.containerMain,
            binding.containerClock,
            binding.containerPrayer,
            binding.containerFooter,
            binding.imageBackground,
            binding.viewBackgroundScrim
        ) { baseColor ->
            window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(baseColor))
        }

        adzanOverlayView = AdzanOverlayView(this)
        binding.containerAdzanOverlay.addView(
            adzanOverlayView,
            android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        val ntpDriftManager = NtpDriftManager(lifecycleScope)
        clockManager = ClockManager(ntpDriftManager) { nowMillis, _ ->
            val config = displayConfig
            val zone = config?.timezoneOverride ?: mosqueProfile?.timezone
            themeManager.clockView.update(
                nowMillis,
                config?.clockUse24Hour ?: true,
                zone
            )
            themeManager.headerView.updateDate(nowMillis, zone, config?.hijriDayOffset ?: 0)
            viewModel.onClockTick(nowMillis)
            updateScreensaver(nowMillis)
            applyBurnInDrift(nowMillis)
        }

        observeViewModel()
        clockManager.start()
        displayInitialized = true
    }

    /**
     * Nudges the whole layout along a slow 15-minute cycle. The panel runs
     * unattended for years with a clock in a fixed spot, which is exactly the
     * pattern that burns in; a few pixels of drift is imperceptible at viewing
     * distance but keeps any single pixel from holding one value.
     */
    private fun applyBurnInDrift(nowMillis: Long) {
        // The screensaver already blanks the panel, and drifting it would slide
        // the black overlay off the edge and expose the themed window beneath.
        if (binding.containerScreensaverOverlay.isVisible) return

        val phase = (nowMillis % BURN_IN_CYCLE_MILLIS).toDouble() / BURN_IN_CYCLE_MILLIS
        binding.displayRoot.translationX = (driftAmplitudePx * cos(2 * PI * phase)).toFloat()
        binding.displayRoot.translationY = (driftAmplitudePx * sin(2 * PI * phase)).toFloat()
    }

    private fun updateScreensaver(nowMillis: Long) {
        val config = displayConfig ?: return
        val isActive = ScreensaverScheduler.isActive(
            config,
            nowMillis,
            config.timezoneOverride ?: mosqueProfile?.timezone
        )
        if (isActive && !binding.containerScreensaverOverlay.isVisible) {
            // Recentre before blanking so the panel doesn't resume off-axis.
            binding.displayRoot.translationX = 0f
            binding.displayRoot.translationY = 0f
        }
        binding.containerScreensaverOverlay.isVisible = isActive
    }

    private fun observeViewModel() {
        viewModel.mosqueProfile.observe(this) { profile ->
            mosqueProfile = profile
            if (profile != null) {
                themeManager.headerView.bind(
                    profile.name,
                    profile.city,
                    profile.logoPath,
                    displayConfig?.logoVisible ?: true
                )
            }
        }

        viewModel.displayConfig.observe(this) { config ->
            displayConfig = config ?: DisplayConfig()
            clockManager.setTimeOffsetMinutes(config?.timeOffsetMinutes ?: 0)
            val theme = viewModel.activeTheme.value
            if (theme != null) applyTheme(theme)
        }

        viewModel.activeTheme.observe(this) { theme ->
            if (theme != null) applyTheme(theme)
        }

        viewModel.prayerState.observe(this) { state ->
            applyPrayerState(state)

            val schedule = viewModel.prayerManager.todaySchedule.value ?: return@observe
            bindSchedule(schedule, (state as? PrayerState.Idle)?.next)

            // The countdown to the next prayer is what the congregation checks
            // most; it lives inside the raised bay. Hidden during adzan and
            // iqamah, when the overlay carries the timing instead.
            val countdown = (state as? PrayerState.Idle)
                ?.countdownMillis
                ?.takeIf { displayConfig?.countdownVisible ?: true }
            themeManager.prayerPanelView.setCountdown(countdown)
        }

        viewModel.prayerManager.todaySchedule.observe(this) { schedule ->
            if (schedule == null) return@observe
            bindSchedule(schedule, (viewModel.prayerState.value as? PrayerState.Idle)?.next)
        }

        viewModel.slideRepository.observeEnabled().observe(this) { slides ->
            themeManager.slideShowView.setSlides(slides)
        }

        viewModel.announcementRepository.observeEnabled().observe(this) { announcements ->
            val texts = announcements.map { it.text }
            themeManager.runningTextView.setItems(texts)
            themeManager.announcementBoardView.setItems(texts)
        }
    }

    /**
     * Applies a theme, and plays the entry sequence the first time.
     *
     * The panel is a kiosk that boots straight into this Activity, so without
     * a transition the display appears as an abrupt full-brightness slab the
     * moment the database read returns. Only the first apply animates — later
     * ones come from an admin saving a setting, where a fade would read as a
     * glitch rather than an arrival.
     */
    private fun applyTheme(theme: com.masjid.display.data.local.entity.ThemeConfig) {
        themeManager.applyTheme(theme, displayConfig ?: DisplayConfig())
        if (!entryPlayed) {
            entryPlayed = true
            playEntryTransition()
        }
    }

    /**
     * Bands settle in from the outside: the header and ticker arrive first,
     * then the stage, then the arcade. The order matches how the screen is
     * read — identity, then content, then the times.
     */
    private fun playEntryTransition() {
        val rise = Scale.px(this, 20f).toFloat()
        val bands = listOf(
            binding.containerHeader,
            binding.containerFooter,
            binding.containerMain,
            binding.containerClock,
            binding.containerPrayer
        )
        bands.forEachIndexed { index, band ->
            band.alpha = 0f
            band.translationY = rise
            band.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(index * 90L)
                .setDuration(700L)
                .start()
        }
    }

    /**
     * The arcade and the schedule board show the same seven times, so they are
     * always bound together — a theme switch must never leave one of them
     * holding yesterday's data.
     */
    private fun bindSchedule(schedule: PrayerSchedule, highlight: com.masjid.display.prayer.Prayer?) {
        val zone = displayConfig?.timezoneOverride ?: mosqueProfile?.timezone
        themeManager.prayerPanelView.bind(schedule, zone, highlight)
        themeManager.scheduleBoardView.bind(schedule, zone, highlight)
    }

    private fun applyPrayerState(state: PrayerState) {
        when (state) {
            is PrayerState.AdzanTime -> {
                adzanOverlayView.showAdzan(state.prayer)
                showAdzanOverlay()
            }
            is PrayerState.IqamahCountdown -> {
                // Fires every second with a new countdown value; the entry only
                // plays on the transition, or the content would restart its
                // animation on each tick.
                adzanOverlayView.showIqamah(state.prayer, state.remainingMillis)
                showAdzanOverlay()
            }
            else -> {
                binding.containerAdzanOverlay.isVisible = false
            }
        }
    }

    private fun showAdzanOverlay() {
        if (binding.containerAdzanOverlay.isVisible) return
        themeManager.palette?.let { adzanOverlayView.applyPalette(it) }
        binding.containerAdzanOverlay.isVisible = true
        adzanOverlayView.playEntry()
    }

    override fun onStart() {
        super.onStart()
        if (!displayInitialized) return
        clockManager.start()
        // onStop cancelled these; the clock was already being restarted here
        // but the animated views were not, so anything that backgrounded the
        // app — a launcher overlay, an HDMI input switch — left the ticker and
        // the slider permanently frozen.
        themeManager.runningTextView.resume()
        themeManager.restartMainContent()
    }

    override fun onStop() {
        super.onStop()
        if (displayInitialized) {
            clockManager.stop()
            themeManager.runningTextView.stop()
            themeManager.slideShowView.stop()
            themeManager.announcementBoardView.stop()
            themeManager.prayerPanelView.stop()
        }
    }

    /**
     * The slideshow holds a video decoder and a surface once any video slide
     * has played. onStop only pauses it — without this it would survive the
     * Activity and leak both.
     */
    override fun onDestroy() {
        super.onDestroy()
        if (displayInitialized) {
            themeManager.slideShowView.release()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applyImmersiveMode()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (isAdminEntryKey(keyCode) && event.repeatCount == 0) {
            menuKeyDownTime = System.currentTimeMillis()
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (isAdminEntryKey(keyCode)) {
            val heldMillis = System.currentTimeMillis() - menuKeyDownTime
            if (heldMillis >= LONG_PRESS_ADMIN_THRESHOLD_MS) {
                startActivity(Intent(this, PinEntryActivity::class.java))
            }
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun isAdminEntryKey(keyCode: Int): Boolean =
        keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER

    private fun applyImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                    android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
        }
    }
}
