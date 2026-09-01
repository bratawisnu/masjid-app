package com.masjid.display.setup

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.masjid.display.core.util.PinHasher
import com.masjid.display.data.local.AppDatabase
import com.masjid.display.data.local.entity.CalculationMethod
import com.masjid.display.data.local.entity.DisplayConfig
import com.masjid.display.data.local.entity.MadhabMethod
import com.masjid.display.data.local.entity.MosqueProfile
import com.masjid.display.data.local.entity.PrayerConfig
import com.masjid.display.data.local.entity.ThemeConfig
import com.masjid.display.data.repository.DisplayConfigRepository
import com.masjid.display.data.repository.MosqueProfileRepository
import com.masjid.display.data.repository.PrayerConfigRepository
import com.masjid.display.data.repository.ThemeConfigRepository
import com.masjid.display.databinding.ActivitySetupWizardBinding
import com.masjid.display.databinding.ItemThemeChoiceBinding
import com.masjid.display.display.DisplayActivity
import com.masjid.display.prayer.PrayerScheduleGenerator
import kotlinx.coroutines.launch

class SetupWizardActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupWizardBinding

    private lateinit var mosqueProfileRepository: MosqueProfileRepository
    private lateinit var prayerConfigRepository: PrayerConfigRepository
    private lateinit var displayConfigRepository: DisplayConfigRepository
    private lateinit var themeConfigRepository: ThemeConfigRepository
    private lateinit var scheduleGenerator: PrayerScheduleGenerator

    private var themes: List<ThemeConfig> = emptyList()
    private var selectedThemeId: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupWizardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = AppDatabase.getInstance(applicationContext)
        mosqueProfileRepository = MosqueProfileRepository(database.mosqueProfileDao())
        prayerConfigRepository = PrayerConfigRepository(database.prayerConfigDao())
        displayConfigRepository = DisplayConfigRepository(database.displayConfigDao())
        themeConfigRepository = ThemeConfigRepository(database.themeConfigDao())
        scheduleGenerator = PrayerScheduleGenerator(com.masjid.display.data.repository.PrayerScheduleRepository(database.prayerScheduleDao()))

        setupStep1()
        setupStep2()
        setupStep3()
        setupStep4()
        setupStep5()
    }

    private fun setupStep1() {
        binding.btnStep1Next.setOnClickListener {
            if (binding.inputMosqueName.text.isNullOrBlank()) {
                binding.inputMosqueName.error = "Nama masjid wajib diisi"
                return@setOnClickListener
            }
            binding.viewFlipper.showNext()
        }
    }

    private fun setupStep2() {
        binding.btnStep2Back.setOnClickListener { binding.viewFlipper.showPrevious() }
        binding.btnStep2Next.setOnClickListener {
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
            binding.viewFlipper.showNext()
        }
    }

    private fun setupStep3() {
        val methods = CalculationMethod.entries.toTypedArray()
        binding.spinnerCalculationMethod.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            methods.map { it.name }
        )
        binding.spinnerCalculationMethod.setSelection(methods.indexOf(CalculationMethod.KEMENAG_RI))

        val madhabs = MadhabMethod.entries.toTypedArray()
        binding.spinnerMadhab.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            madhabs.map { it.name }
        )

        binding.btnStep3Back.setOnClickListener { binding.viewFlipper.showPrevious() }
        binding.btnStep3Next.setOnClickListener { binding.viewFlipper.showNext() }
    }

    private fun setupStep4() {
        lifecycleScope.launch {
            AppDatabase.ensureThemesSeeded(applicationContext)
            themes = (1..15).mapNotNull { themeConfigRepository.getById(it) }
            renderThemeChoices()
        }

        binding.btnStep4Back.setOnClickListener { binding.viewFlipper.showPrevious() }
        binding.btnStep4Next.setOnClickListener {
            binding.textSummary.text = buildSummary()
            binding.viewFlipper.showNext()
        }
    }

    private fun renderThemeChoices() {
        binding.themeListContainer.removeAllViews()
        themes.forEach { theme ->
            val itemBinding = ItemThemeChoiceBinding.inflate(
                LayoutInflater.from(this),
                binding.themeListContainer,
                false
            )
            itemBinding.textThemeName.text = theme.name
            itemBinding.colorSwatch.setBackgroundColor(theme.backgroundColor)
            itemBinding.radioSelected.isChecked = theme.themeId == selectedThemeId
            itemBinding.itemRoot.setOnClickListener {
                selectedThemeId = theme.themeId
                refreshThemeSelectionUi()
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

    private fun buildSummary(): String {
        val name = binding.inputMosqueName.text.toString()
        val city = binding.inputCity.text.toString()
        val method = binding.spinnerCalculationMethod.selectedItem?.toString().orEmpty()
        val themeName = themes.firstOrNull { it.themeId == selectedThemeId }?.name.orEmpty()
        return "Masjid: $name\nKota: $city\nMetode: $method\nTema: $themeName"
    }

    private fun setupStep5() {
        binding.btnStep5Back.setOnClickListener { binding.viewFlipper.showPrevious() }
        binding.btnStep5Finish.setOnClickListener { finishSetup() }
    }

    private fun finishSetup() {
        val pin = binding.inputAdminPin.text.toString()
        if (pin.length < 4) {
            binding.inputAdminPin.error = "PIN minimal 4 digit"
            return
        }

        val profile = MosqueProfile(
            name = binding.inputMosqueName.text.toString(),
            address = binding.inputAddress.text.toString(),
            city = binding.inputCity.text.toString(),
            latitude = binding.inputLatitude.text.toString().toDoubleOrNull() ?: 0.0,
            longitude = binding.inputLongitude.text.toString().toDoubleOrNull() ?: 0.0,
            timezone = binding.inputTimezone.text.toString().ifBlank { "Asia/Jakarta" }
        )

        val methods = CalculationMethod.entries.toTypedArray()
        val selectedMethod = methods.getOrElse(binding.spinnerCalculationMethod.selectedItemPosition) {
            CalculationMethod.KEMENAG_RI
        }
        val madhabs = MadhabMethod.entries.toTypedArray()
        val selectedMadhab = madhabs.getOrElse(binding.spinnerMadhab.selectedItemPosition) {
            MadhabMethod.SHAFI
        }
        val prayerConfig = PrayerConfig(
            calculationMethod = selectedMethod,
            madhab = selectedMadhab
        )

        lifecycleScope.launch {
            mosqueProfileRepository.save(profile)
            prayerConfigRepository.save(prayerConfig)
            displayConfigRepository.save(
                DisplayConfig(activeThemeId = selectedThemeId, adminPinHash = PinHasher.hash(pin))
            )
            scheduleGenerator.regenerate(profile, prayerConfig)

            startActivity(Intent(this@SetupWizardActivity, DisplayActivity::class.java))
            finish()
        }
    }
}
