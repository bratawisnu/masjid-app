package com.masjid.display.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.masjid.display.data.local.dao.AnnouncementDao
import com.masjid.display.data.local.dao.DisplayConfigDao
import com.masjid.display.data.local.dao.MosqueProfileDao
import com.masjid.display.data.local.dao.PrayerConfigDao
import com.masjid.display.data.local.dao.PrayerScheduleDao
import com.masjid.display.data.local.dao.SlideDao
import com.masjid.display.data.local.dao.ThemeConfigDao
import com.masjid.display.data.local.entity.Announcement
import com.masjid.display.data.local.entity.DisplayConfig
import com.masjid.display.data.local.entity.MosqueProfile
import com.masjid.display.data.local.entity.PrayerConfig
import com.masjid.display.data.local.entity.PrayerSchedule
import com.masjid.display.data.local.entity.Slide
import com.masjid.display.data.local.entity.ThemeConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Database(
    entities = [
        MosqueProfile::class,
        PrayerConfig::class,
        PrayerSchedule::class,
        ThemeConfig::class,
        DisplayConfig::class,
        Announcement::class,
        Slide::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun mosqueProfileDao(): MosqueProfileDao
    abstract fun prayerConfigDao(): PrayerConfigDao
    abstract fun prayerScheduleDao(): PrayerScheduleDao
    abstract fun themeConfigDao(): ThemeConfigDao
    abstract fun displayConfigDao(): DisplayConfigDao
    abstract fun announcementDao(): AnnouncementDao
    abstract fun slideDao(): SlideDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        private val seedMutex = Mutex()
        private var themesSeeded = false

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }
        }

        /**
         * v3 -> v4 adds [DisplayConfig.backgroundImagePath].
         *
         * Written out rather than left to destructive fallback because this
         * database holds everything the caretaker typed by hand — mosque name,
         * coordinates, prayer corrections, announcements, the admin PIN. An
         * app update that silently emptied all of it and dropped the panel back
         * into the setup wizard is not a migration, it's data loss.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE display_config ADD COLUMN backgroundImagePath TEXT")
            }
        }

        /**
         * v4 -> v5 turns the image-only slideshow into a typed one: `slide_image`
         * becomes `slide`, with a [com.masjid.display.data.local.entity.SlideType]
         * and a nullable payload per type.
         *
         * This is a table rebuild rather than a sequence of ALTERs because
         * `path` has to lose its NOT NULL — a text slide has no file — and
         * SQLite cannot drop a column constraint in place. Every existing row
         * carries over as an IMAGE slide with its order, duration and enabled
         * flag intact, so a panel that has been running in a mosque keeps
         * showing exactly what it showed before the update.
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `slide` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `type` TEXT NOT NULL,
                        `path` TEXT,
                        `url` TEXT,
                        `body` TEXT,
                        `order` INTEGER NOT NULL,
                        `durationSeconds` INTEGER NOT NULL,
                        `enabled` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `slide` (`id`, `type`, `path`, `url`, `body`, `order`, `durationSeconds`, `enabled`)
                    SELECT `id`, 'IMAGE', `path`, NULL, NULL, `order`, `durationSeconds`, `enabled`
                    FROM `slide_image`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `slide_image`")
            }
        }

        private fun build(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "masjid_display.db"
            )
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                // Deliberately NOT fallbackToDestructiveMigration: a missing
                // migration should fail loudly in development rather than wipe
                // a mosque's configuration in the field.
                .build()
        }

        /**
         * Seeds the 15 themes if missing, and suspends until done. Callers
         * that read the theme list (SetupWizardActivity, AdminSettingsActivity,
         * DisplayActivity) must await this first — Room's Callback.onCreate
         * runs asynchronously and racing it produced a blank theme picker.
         */
        suspend fun ensureThemesSeeded(context: Context) {
            if (themesSeeded) return
            seedMutex.withLock {
                if (themesSeeded) return
                val db = getInstance(context)
                if (db.themeConfigDao().count() == 0) {
                    db.themeConfigDao().insertAll(ThemeSeedData.seed())
                }
                themesSeeded = true
            }
        }
    }
}
