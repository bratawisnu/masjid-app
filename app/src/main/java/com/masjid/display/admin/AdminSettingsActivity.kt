package com.masjid.display.admin

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.masjid.display.BuildConfig
import com.masjid.display.core.util.PinHasher
import com.masjid.display.data.local.AppDatabase
import com.masjid.display.data.local.entity.Announcement
import com.masjid.display.data.local.entity.CalculationMethod
import com.masjid.display.data.local.entity.DisplayConfig
import com.masjid.display.data.local.entity.MadhabMethod
import com.masjid.display.data.local.entity.MosqueProfile
import com.masjid.display.data.local.entity.PrayerConfig
import com.masjid.display.data.local.entity.Slide
import com.masjid.display.data.local.entity.SlideType
import com.masjid.display.data.local.entity.ThemeConfig
import com.masjid.display.data.repository.AnnouncementRepository
import com.masjid.display.data.repository.DisplayConfigRepository
import com.masjid.display.data.repository.MosqueProfileRepository
import com.masjid.display.data.repository.PrayerConfigRepository
import com.masjid.display.data.repository.PrayerScheduleRepository
import com.masjid.display.data.repository.SlideRepository
import com.masjid.display.data.repository.ThemeConfigRepository
import com.masjid.display.databinding.ActivityAdminSettingsBinding
import com.masjid.display.databinding.ItemAdminListRowBinding
import com.masjid.display.databinding.ItemThemeChoiceBinding
import com.masjid.display.display.DisplayActivity
import com.masjid.display.display.design.BackgroundScrim
import com.masjid.display.display.design.Palette
import com.masjid.display.prayer.PrayerScheduleGenerator
import java.io.File
import kotlinx.coroutines.launch

class AdminSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminSettingsBinding

    private lateinit var mosqueProfileRepository: MosqueProfileRepository
    private lateinit var prayerConfigRepository: PrayerConfigRepository
    private lateinit var displayConfigRepository: DisplayConfigRepository
    private lateinit var themeConfigRepository: ThemeConfigRepository
    private lateinit var announcementRepository: AnnouncementRepository
    private lateinit var slideRepository: SlideRepository
    private lateinit var scheduleGenerator: PrayerScheduleGenerator

    private var themes: List<ThemeConfig> = emptyList()
    private var selectedThemeId: Int = 1
    private var nextSlideOrder: Int = 0

    /** Mosque fields are read-only until Reset; see [setMosqueLocked]. */
    private var mosqueLocked: Boolean = false

    // The app browses storage itself instead of calling the system picker.
    // ACTION_OPEN_DOCUMENT needs DocumentsUI to answer it, and plenty of TV
    // boxes ship without it — on those the picker never opens and the system
    // says "You don't have an app that can do this", leaving no way to add a
    // photo or a video at all. See FilePickerActivity.
    private val pickImageLauncher = registerForActivityResult(FilePickerActivity.Contract()) { path ->
        if (path != null) copyAndSaveImage(path)
    }

    private val pickVideoLauncher = registerForActivityResult(FilePickerActivity.Contract()) { path ->
        if (path != null) copyAndSaveVideo(path)
    }

    private val pickBackgroundLauncher = registerForActivityResult(FilePickerActivity.Contract()) { path ->
        if (path != null) copyAndSaveBackground(path)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = AppDatabase.getInstance(applicationContext)
        mosqueProfileRepository = MosqueProfileRepository(database.mosqueProfileDao())
        prayerConfigRepository = PrayerConfigRepository(database.prayerConfigDao())
        displayConfigRepository = DisplayConfigRepository(database.displayConfigDao())
        themeConfigRepository = ThemeConfigRepository(database.themeConfigDao())
        announcementRepository = AnnouncementRepository(database.announcementDao())
        slideRepository = SlideRepository(database.slideDao())
        scheduleGenerator = PrayerScheduleGenerator(PrayerScheduleRepository(database.prayerScheduleDao()))

        setupMosqueSection()
        setupPrayerSection()
        setupThemeSection()
        setupDisplaySection()
        setupAnnouncementSection()
        setupBackgroundSection()
        setupImageSection()
        setupTimeSection()
        setupScreensaverSection()
        setupSystemSection()

        loadCurrentValues()
    }

    private fun loadCurrentValues() {
        lifecycleScope.launch {
            AppDatabase.ensureThemesSeeded(applicationContext)
            val profile = mosqueProfileRepository.get()
            profile?.let {
                binding.inputMosqueName.setText(it.name)
                binding.inputAddress.setText(it.address)
                binding.inputCity.setText(it.city)
                binding.inputLatitude.setText(it.latitude.toString())
                binding.inputLongitude.setText(it.longitude.toString())
                binding.inputTimezone.setText(it.timezone)
            }
            // A saved profile opens locked; an empty one has nothing to protect
            // and would otherwise strand the caretaker behind a Reset button.
            setMosqueLocked(profile != null)

            val prayerConfig = prayerConfigRepository.get()
            val methods = CalculationMethod.entries.toTypedArray()
            binding.spinnerCalculationMethod.setSelection(methods.indexOf(prayerConfig.calculationMethod))
            val madhabs = MadhabMethod.entries.toTypedArray()
            binding.spinnerMadhab.setSelection(madhabs.indexOf(prayerConfig.madhab))
            binding.inputCorrectionImsak.setText(prayerConfig.imsakCorrectionMinutes.toString())
            binding.inputCorrectionSubuh.setText(prayerConfig.subuhCorrectionMinutes.toString())
            binding.inputCorrectionDhuhr.setText(prayerConfig.dhuhrCorrectionMinutes.toString())
            binding.inputCorrectionAsr.setText(prayerConfig.asrCorrectionMinutes.toString())
            binding.inputCorrectionMaghrib.setText(prayerConfig.maghribCorrectionMinutes.toString())
            binding.inputCorrectionIsha.setText(prayerConfig.ishaCorrectionMinutes.toString())

            val displayConfig = displayConfigRepository.get()
            selectedThemeId = displayConfig.activeThemeId
            binding.switchClockVisible.isChecked = displayConfig.clockVisible
            binding.switch24hour.isChecked = displayConfig.clockUse24Hour
            binding.switchPrayerPanelVisible.isChecked = displayConfig.prayerPanelVisible
            binding.switchLogoVisible.isChecked = displayConfig.logoVisible
            binding.switchSliderVisible.isChecked = displayConfig.sliderVisible
            binding.switchRunningTextVisible.isChecked = displayConfig.runningTextVisible
            binding.inputTimeOffset.setText(displayConfig.timeOffsetMinutes.toString())

            binding.switchScreensaverEnabled.isChecked = displayConfig.screensaverEnabled
            binding.inputScreensaverStart.setText(
                "%02d:%02d".format(displayConfig.screensaverStartHour, displayConfig.screensaverStartMinute)
            )
            binding.inputScreensaverEnd.setText(
                "%02d:%02d".format(displayConfig.screensaverEndHour, displayConfig.screensaverEndMinute)
            )

            themes = (1..15).mapNotNull { themeConfigRepository.getById(it) }
            renderThemeChoices()
            // After themes load — the too-bright warning is measured against
            // the active theme's text colour, so it can't be rendered earlier.
            renderBackgroundState(displayConfig.backgroundImagePath)

            binding.textAppVersion.text = "Versi ${BuildConfig.VERSION_NAME}"
        }

        announcementRepository.observeAll().observe(this) { renderAnnouncements(it) }
        slideRepository.observeAll().observe(this) { slides ->
            nextSlideOrder = (slides.maxOfOrNull { it.order } ?: -1) + 1
            renderSlides(slides)
        }
    }

    private fun setupMosqueSection() {
        binding.btnSaveMosque.setOnClickListener {
            if (binding.inputMosqueName.text.isNullOrBlank()) {
                binding.inputMosqueName.error = "Nama masjid wajib diisi"
                return@setOnClickListener
            }
            val lat = binding.inputLatitude.text.toString().toDoubleOrNull()
            val lng = binding.inputLongitude.text.toString().toDoubleOrNull()
            if (lat == null || lng == null) {
                binding.inputLatitude.error = "Latitude/Longitude harus berupa angka"
                return@setOnClickListener
            }
            if (lat < -90.0 || lat > 90.0) {
                binding.inputLatitude.error = "Latitude harus antara -90 dan 90"
                return@setOnClickListener
            }
            if (lng < -180.0 || lng > 180.0) {
                binding.inputLongitude.error = "Longitude harus antara -180 dan 180"
                return@setOnClickListener
            }
            val profile = MosqueProfile(
                name = binding.inputMosqueName.text.toString(),
                address = binding.inputAddress.text.toString(),
                city = binding.inputCity.text.toString(),
                latitude = lat,
                longitude = lng,
                timezone = binding.inputTimezone.text.toString().ifBlank { "Asia/Jakarta" }
            )
            lifecycleScope.launch {
                mosqueProfileRepository.save(profile)
                regenerateSchedule()
                // Locking on the save that succeeded, rather than on the click,
                // means a rejected latitude leaves the fields open to fix.
                setMosqueLocked(true)
            }
        }

        binding.btnResetMosque.setOnClickListener { confirmResetMosque() }
    }

    /**
     * Reset asks first. It reopens the fields that decide when every prayer is
     * called, and on a TV remote the button beside "Simpan" is one press away
     * from the one the caretaker meant.
     */
    private fun confirmResetMosque() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Reset Data Masjid?")
            .setMessage(
                "Data masjid akan dibuka untuk diubah. Jadwal sholat dihitung " +
                    "ulang setelah Anda menekan Simpan."
            )
            .setPositiveButton("Reset") { _, _ -> setMosqueLocked(false) }
            .setNegativeButton("Batal", null)
            .show()
    }

    /**
     * Locked fields stay visible and readable — the caretaker still needs to
     * check the coordinates — but refuse focus, so the D-pad walks straight
     * past them to the Reset button.
     */
    private fun setMosqueLocked(locked: Boolean) {
        mosqueLocked = locked

        listOf(
            binding.inputMosqueName,
            binding.inputAddress,
            binding.inputCity,
            binding.inputLatitude,
            binding.inputLongitude
        ).forEach { field ->
            field.isEnabled = !locked
            field.isFocusable = !locked
            field.isFocusableInTouchMode = !locked
            field.error = null
        }

        binding.btnSaveMosque.isEnabled = !locked
        binding.btnResetMosque.isEnabled = locked
        binding.textMosqueStatus.text = if (locked) {
            "Data tersimpan dan dikunci. Tekan \"Reset & Ubah\" untuk memperbarui."
        } else {
            "Data terbuka untuk diubah. Tekan \"Simpan\" untuk mengunci kembali."
        }
    }

    private fun setupPrayerSection() {
        val methods = CalculationMethod.entries.toTypedArray()
        binding.spinnerCalculationMethod.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, methods.map { it.name }
        )
        val madhabs = MadhabMethod.entries.toTypedArray()
        binding.spinnerMadhab.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, madhabs.map { it.name }
        )

        binding.btnSavePrayer.setOnClickListener {
            val methodsArr = CalculationMethod.entries.toTypedArray()
            val madhabsArr = MadhabMethod.entries.toTypedArray()
            val config = PrayerConfig(
                calculationMethod = methodsArr.getOrElse(binding.spinnerCalculationMethod.selectedItemPosition) { CalculationMethod.KEMENAG_RI },
                madhab = madhabsArr.getOrElse(binding.spinnerMadhab.selectedItemPosition) { MadhabMethod.SHAFI },
                imsakCorrectionMinutes = binding.inputCorrectionImsak.text.toString().toIntOrNull() ?: -10,
                subuhCorrectionMinutes = binding.inputCorrectionSubuh.text.toString().toIntOrNull() ?: 0,
                dhuhrCorrectionMinutes = binding.inputCorrectionDhuhr.text.toString().toIntOrNull() ?: 0,
                asrCorrectionMinutes = binding.inputCorrectionAsr.text.toString().toIntOrNull() ?: 0,
                maghribCorrectionMinutes = binding.inputCorrectionMaghrib.text.toString().toIntOrNull() ?: 0,
                ishaCorrectionMinutes = binding.inputCorrectionIsha.text.toString().toIntOrNull() ?: 0
            )
            lifecycleScope.launch {
                prayerConfigRepository.save(config)
                regenerateSchedule()
            }
        }
    }

    private suspend fun regenerateSchedule() {
        val profile = mosqueProfileRepository.get() ?: return
        val config = prayerConfigRepository.get()
        scheduleGenerator.regenerate(profile, config)
    }

    private fun setupThemeSection() {
        // Rendered once themes/selectedThemeId are loaded in loadCurrentValues().
    }

    private fun renderThemeChoices() {
        binding.themeListContainer.removeAllViews()
        themes.forEach { theme ->
            val itemBinding = ItemThemeChoiceBinding.inflate(
                LayoutInflater.from(this), binding.themeListContainer, false
            )
            itemBinding.textThemeName.text = theme.name
            itemBinding.colorSwatch.setBackgroundColor(theme.backgroundColor)
            itemBinding.radioSelected.isChecked = theme.themeId == selectedThemeId
            itemBinding.itemRoot.setOnClickListener {
                selectedThemeId = theme.themeId
                refreshThemeSelectionUi()
                lifecycleScope.launch {
                    val current = displayConfigRepository.get()
                    displayConfigRepository.save(current.copy(activeThemeId = selectedThemeId))
                    // A photo that was fine on a dark theme can be unreadable
                    // on Minimal Light, so the warning is re-measured here.
                    renderBackgroundState(current.backgroundImagePath)
                }
            }
            binding.themeListContainer.addView(itemBinding.root)
        }
    }

    private fun refreshThemeSelectionUi() {
        for (i in 0 until binding.themeListContainer.childCount) {
            val child = binding.themeListContainer.getChildAt(i)
            val theme = themes.getOrNull(i) ?: continue
            ItemThemeChoiceBinding.bind(child).radioSelected.isChecked = theme.themeId == selectedThemeId
        }
    }

    private fun setupDisplaySection() {
        binding.btnSaveDisplay.setOnClickListener {
            lifecycleScope.launch {
                val current = displayConfigRepository.get()
                displayConfigRepository.save(
                    current.copy(
                        clockVisible = binding.switchClockVisible.isChecked,
                        clockUse24Hour = binding.switch24hour.isChecked,
                        prayerPanelVisible = binding.switchPrayerPanelVisible.isChecked,
                        logoVisible = binding.switchLogoVisible.isChecked,
                        sliderVisible = binding.switchSliderVisible.isChecked,
                        runningTextVisible = binding.switchRunningTextVisible.isChecked
                    )
                )
            }
        }
    }

    private fun setupAnnouncementSection() {
        binding.btnAddAnnouncement.setOnClickListener {
            val text = binding.inputNewAnnouncement.text.toString()
            if (text.isBlank()) return@setOnClickListener
            lifecycleScope.launch {
                announcementRepository.save(Announcement(text = text))
                binding.inputNewAnnouncement.setText("")
            }
        }
    }

    private fun renderAnnouncements(announcements: List<Announcement>) {
        binding.announcementListContainer.removeAllViews()
        announcements.forEach { announcement ->
            val row = ItemAdminListRowBinding.inflate(LayoutInflater.from(this), binding.announcementListContainer, false)
            row.textLabel.text = announcement.text
            row.switchEnabled.isChecked = announcement.enabled
            row.switchEnabled.setOnCheckedChangeListener { _, checked ->
                lifecycleScope.launch { announcementRepository.update(announcement.copy(enabled = checked)) }
            }
            row.btnDelete.setOnClickListener {
                lifecycleScope.launch { announcementRepository.delete(announcement) }
            }
            binding.announcementListContainer.addView(row.root)
        }
    }

    private fun setupBackgroundSection() {
        binding.btnPickBackground.setOnClickListener {
            pickBackgroundLauncher.launch(FilePickerActivity.Mode.IMAGE)
        }
        binding.btnClearBackground.setOnClickListener {
            lifecycleScope.launch {
                val current = displayConfigRepository.get()
                current.backgroundImagePath?.let { File(it).delete() }
                displayConfigRepository.save(current.copy(backgroundImagePath = null))
                renderBackgroundState(null)
            }
        }
    }

    private fun copyAndSaveBackground(sourcePath: String) {
        lifecycleScope.launch {
            val source = File(sourcePath)
            val backgroundsDir = File(getExternalFilesDir(null), "backgrounds").apply { mkdirs() }
            val extension = source.extension.ifBlank { "jpg" }
            val destFile = File(backgroundsDir, "background_${System.currentTimeMillis()}.$extension")
            source.inputStream().use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            }

            val current = displayConfigRepository.get()
            // Only one background is ever active, so the old file is dead
            // weight on a TV box with a few GB of storage.
            current.backgroundImagePath
                ?.takeIf { it != destFile.absolutePath }
                ?.let { File(it).delete() }

            displayConfigRepository.save(current.copy(backgroundImagePath = destFile.absolutePath))
            renderBackgroundState(destFile.absolutePath)
        }
    }

    /**
     * Shows which file is in use and, when the photo is too bright to survive
     * its own scrim, says so here — the alternative is the caretaker finding
     * out from the back of the prayer hall.
     */
    private fun renderBackgroundState(path: String?) {
        if (path == null) {
            binding.textBackgroundCurrent.text = "Belum ada gambar latar — memakai warna tema."
            binding.textBackgroundWarning.isVisible = false
            return
        }

        binding.textBackgroundCurrent.text = File(path).name

        val theme = themes.firstOrNull { it.themeId == selectedThemeId }
        // Downsampled 8x: a 12 MP phone photo would otherwise decode to ~48 MB
        // and take the admin screen down on a 1 GB TV box. Scrim alpha is
        // derived from broad brightness, which survives the downsample.
        val options = BitmapFactory.Options().apply { inSampleSize = 8 }
        val bitmap = runCatching { BitmapFactory.decodeFile(path, options) }.getOrNull()
        if (theme == null || bitmap == null) {
            binding.textBackgroundWarning.isVisible = false
            return
        }

        val palette = Palette.from(theme)
        val alpha = BackgroundScrim.alphaFor(bitmap, palette)
        bitmap.recycle()

        binding.textBackgroundWarning.isVisible = !BackgroundScrim.isImageWorthShowing(alpha)
        binding.textBackgroundWarning.text =
            "Agar teks tema \"${theme.name}\" tetap terbaca, gambar ini diredupkan " +
                "${(alpha * 100).toInt()}% dan hanya samar terlihat. " +
                "Foto yang lebih gelap akan tampil lebih jelas."
    }

    private fun setupImageSection() {
        binding.btnAddImage.setOnClickListener {
            pickImageLauncher.launch(FilePickerActivity.Mode.IMAGE)
        }

        binding.btnAddVideo.setOnClickListener {
            pickVideoLauncher.launch(FilePickerActivity.Mode.VIDEO)
        }

        binding.btnAddVideoUrl.setOnClickListener {
            val url = binding.inputSlideVideoUrl.text.toString().trim()
            if (url.isBlank()) {
                binding.inputSlideVideoUrl.error = "Alamat video wajib diisi"
                return@setOnClickListener
            }
            // Checked here rather than at playback: a typo'd address fails on
            // the display with a slide that silently skips itself, and nobody
            // is standing in front of the TV to see it happen.
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                binding.inputSlideVideoUrl.error = "Alamat harus diawali http:// atau https://"
                return@setOnClickListener
            }
            lifecycleScope.launch {
                slideRepository.save(Slide(type = SlideType.VIDEO, url = url, order = nextSlideOrder))
            }
            binding.inputSlideVideoUrl.setText("")
        }

        binding.btnAddText.setOnClickListener {
            val body = binding.inputSlideText.text.toString().trim()
            if (body.isBlank()) {
                binding.inputSlideText.error = "Isi slide teks wajib diisi"
                return@setOnClickListener
            }
            lifecycleScope.launch {
                slideRepository.save(Slide(type = SlideType.TEXT, body = body, order = nextSlideOrder))
            }
            binding.inputSlideText.setText("")
        }
    }

    private fun copyAndSaveImage(sourcePath: String) = copyAndSave(sourcePath, SlideType.IMAGE, "images")

    private fun copyAndSaveVideo(sourcePath: String) = copyAndSave(sourcePath, SlideType.VIDEO, "videos")

    /**
     * Copies the picked file into app-scoped storage and saves a slide for it.
     *
     * The copy is the point: the caretaker picks from a flash drive or the
     * Downloads folder, and a stored path would stop resolving the moment that
     * drive is pulled or the file is tidied away — leaving a slide that points
     * at nothing.
     */
    private fun copyAndSave(sourcePath: String, type: SlideType, folder: String) {
        lifecycleScope.launch {
            val source = File(sourcePath)
            val dir = File(getExternalFilesDir(null), folder).apply { mkdirs() }
            // The source extension is carried over rather than forced to
            // jpg/mp4: ExoPlayer and Glide both sniff the content, but a .mkv
            // stored as .mp4 misleads anyone reading the folder later.
            val extension = source.extension.ifBlank { if (type == SlideType.VIDEO) "mp4" else "jpg" }
            val destFile = File(dir, "slide_${System.currentTimeMillis()}.$extension")
            source.inputStream().use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            }
            slideRepository.save(Slide(type = type, path = destFile.absolutePath, order = nextSlideOrder))
        }
    }

    private fun renderSlides(slides: List<Slide>) {
        binding.imageListContainer.removeAllViews()
        slides.forEach { slide ->
            val row = ItemAdminListRowBinding.inflate(LayoutInflater.from(this), binding.imageListContainer, false)
            row.textLabel.text = labelFor(slide)
            row.switchEnabled.isChecked = slide.enabled
            row.switchEnabled.setOnCheckedChangeListener { _, checked ->
                lifecycleScope.launch { slideRepository.update(slide.copy(enabled = checked)) }
            }
            row.btnDelete.setOnClickListener {
                lifecycleScope.launch {
                    slideRepository.delete(slide)
                    // Only local files are ours to remove; a URL slide has
                    // nothing on disk, and a text slide never did.
                    if (slide.ownsFile) slide.path?.let { File(it).delete() }
                }
            }
            binding.imageListContainer.addView(row.root)
        }
    }

    /**
     * Names a slide by what it is and what it carries — the list mixes three
     * kinds now, and a column of bare filenames wouldn't say which is which.
     */
    private fun labelFor(slide: Slide): String = when (slide.type) {
        SlideType.TEXT -> "Teks · ${slide.body.orEmpty().take(40)}"
        SlideType.IMAGE -> "Gambar · ${slide.path?.let { File(it).name } ?: "berkas hilang"}"
        SlideType.VIDEO -> when {
            slide.path != null -> "Video · ${File(slide.path).name}"
            slide.url != null -> "Video URL · ${slide.url}"
            else -> "Video · belum ada sumber"
        }
    }

    private fun setupTimeSection() {
        binding.btnSaveTime.setOnClickListener {
            lifecycleScope.launch {
                val current = displayConfigRepository.get()
                displayConfigRepository.save(
                    current.copy(
                        timezoneOverride = binding.inputTimezone.text.toString().ifBlank { null },
                        timeOffsetMinutes = binding.inputTimeOffset.text.toString().toIntOrNull() ?: 0
                    )
                )
            }
        }
    }

    private fun setupScreensaverSection() {
        binding.btnSaveScreensaver.setOnClickListener {
            val start = parseHourMinute(binding.inputScreensaverStart.text.toString())
            val end = parseHourMinute(binding.inputScreensaverEnd.text.toString())
            if (start == null || end == null) {
                binding.inputScreensaverStart.error = "Format waktu harus HH:mm, contoh 23:00"
                return@setOnClickListener
            }
            lifecycleScope.launch {
                val current = displayConfigRepository.get()
                displayConfigRepository.save(
                    current.copy(
                        screensaverEnabled = binding.switchScreensaverEnabled.isChecked,
                        screensaverStartHour = start.first,
                        screensaverStartMinute = start.second,
                        screensaverEndHour = end.first,
                        screensaverEndMinute = end.second
                    )
                )
            }
        }
    }

    private fun parseHourMinute(text: String): Pair<Int, Int>? {
        val parts = text.trim().split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour to minute
    }

    private fun setupSystemSection() {
        binding.btnChangePin.setOnClickListener {
            showChangePinDialog()
        }
        binding.btnRestartDisplay.setOnClickListener {
            // Leaving while the mosque section is unlocked throws away whatever
            // was typed since Reset, and the caretaker would only find out when
            // the schedule didn't change.
            if (mosqueLocked) {
                returnToDisplay()
            } else {
                android.app.AlertDialog.Builder(this)
                    .setTitle("Data Masjid Belum Disimpan")
                    .setMessage("Perubahan pada data masjid akan hilang jika Anda keluar sekarang.")
                    .setPositiveButton("Keluar Tanpa Simpan") { _, _ -> returnToDisplay() }
                    .setNegativeButton("Kembali", null)
                    .show()
            }
        }
    }

    private fun returnToDisplay() {
        startActivity(Intent(this, DisplayActivity::class.java))
        finish()
    }

    private fun showChangePinDialog() {
        val input = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "PIN baru (4-6 digit)"
        }
        android.app.AlertDialog.Builder(this)
            .setTitle("Ubah PIN Admin")
            .setView(input)
            .setPositiveButton("Simpan") { _, _ ->
                val newPin = input.text.toString()
                if (newPin.length < 4) return@setPositiveButton
                lifecycleScope.launch {
                    val current = displayConfigRepository.get()
                    displayConfigRepository.save(current.copy(adminPinHash = PinHasher.hash(newPin)))
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
