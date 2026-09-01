package com.masjid.display.display.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.masjid.display.data.local.AppDatabase
import com.masjid.display.data.local.entity.DisplayConfig
import com.masjid.display.data.local.entity.MosqueProfile
import com.masjid.display.data.local.entity.PrayerConfig
import com.masjid.display.data.local.entity.ThemeConfig
import com.masjid.display.data.repository.AnnouncementRepository
import com.masjid.display.data.repository.DisplayConfigRepository
import com.masjid.display.data.repository.MosqueProfileRepository
import com.masjid.display.data.repository.PrayerConfigRepository
import com.masjid.display.data.repository.PrayerScheduleRepository
import com.masjid.display.data.repository.SlideRepository
import com.masjid.display.data.repository.ThemeConfigRepository
import com.masjid.display.prayer.PrayerManager
import com.masjid.display.prayer.PrayerScheduleGenerator
import com.masjid.display.prayer.PrayerState
import kotlinx.coroutines.launch

class DisplayViewModel(
    private val mosqueProfileRepository: MosqueProfileRepository,
    private val prayerConfigRepository: PrayerConfigRepository,
    private val prayerScheduleRepository: PrayerScheduleRepository,
    private val themeConfigRepository: ThemeConfigRepository,
    private val displayConfigRepository: DisplayConfigRepository,
    val announcementRepository: AnnouncementRepository,
    val slideRepository: SlideRepository
) : ViewModel() {

    private val scheduleGenerator = PrayerScheduleGenerator(prayerScheduleRepository)
    val prayerManager = PrayerManager(prayerScheduleRepository, viewModelScope)

    val mosqueProfile: LiveData<MosqueProfile?> = mosqueProfileRepository.observe()
    val displayConfig: LiveData<DisplayConfig?> = displayConfigRepository.observe()
    val allThemes: LiveData<List<ThemeConfig>> = themeConfigRepository.observeAll()

    val activeTheme: LiveData<ThemeConfig?> = MediatorLiveData<ThemeConfig?>().apply {
        var lastThemeId: Int? = null
        addSource(displayConfig) { config ->
            val themeId = config?.activeThemeId ?: return@addSource
            if (themeId == lastThemeId) return@addSource
            lastThemeId = themeId
            viewModelScope.launch {
                postValue(themeConfigRepository.getById(themeId))
            }
        }
    }

    val prayerState: LiveData<PrayerState> = prayerManager.state

    init {
        viewModelScope.launch {
            val profile = mosqueProfileRepository.get() ?: MosqueProfile()
            val config = prayerConfigRepository.get()
            if (profile.latitude != 0.0 || profile.longitude != 0.0) {
                scheduleGenerator.ensureWindow(profile, config)
            }
        }
    }

    fun onClockTick(nowMillis: Long) {
        prayerManager.tick(nowMillis)
    }

    class Factory(private val database: AppDatabase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DisplayViewModel(
                MosqueProfileRepository(database.mosqueProfileDao()),
                PrayerConfigRepository(database.prayerConfigDao()),
                PrayerScheduleRepository(database.prayerScheduleDao()),
                ThemeConfigRepository(database.themeConfigDao()),
                DisplayConfigRepository(database.displayConfigDao()),
                AnnouncementRepository(database.announcementDao()),
                SlideRepository(database.slideDao())
            ) as T
        }
    }
}
