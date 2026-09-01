package com.masjid.display.data.local

import android.graphics.Color
import com.masjid.display.data.local.entity.AreaPosition
import com.masjid.display.data.local.entity.MainContentType
import com.masjid.display.data.local.entity.ThemeConfig

/**
 * Initial 15 themes. These are deliberately simple palette/position
 * variations to exercise the layout engine end-to-end; exact visual specs
 * are refined per-theme later without touching the engine itself.
 */
object ThemeSeedData {

    fun seed(): List<ThemeConfig> = listOf(
        // The authored direction, and the one the mockups were approved
        // against (design/mockup/a-mihrab.png): petrol-blue night, chalk text,
        // and a single brass accent carried only by the next prayer. Its stage
        // is named rather than left to the variant rotation below, which would
        // have handed the one designed theme a schedule board where the
        // approved mockup shows the slideshow.
        theme(1, "Mihrab", "#0B1220", "#F2EFE6", "#7E8AA3", "#C9A227", MainContentType.IMAGE_SLIDER),
        theme(2, "Ocean Blue", "#0B3D91", "#FFFFFF", "#A9D6E5", "#2ECC71"),
        theme(3, "Emerald", "#014421", "#FFFFFF", "#B7E4C7", "#F1C40F"),
        theme(4, "Royal Purple", "#2E1A47", "#FFFFFF", "#D4B8F0", "#F39C12"),
        theme(5, "Sunset Orange", "#7A2E00", "#FFFFFF", "#FFD5B3", "#FFFFFF"),
        theme(6, "Minimal Light", "#F5F5F0", "#1A1A1A", "#555555", "#B08D57"),
        theme(7, "Crimson", "#4A0E0E", "#FFFFFF", "#F0B8B8", "#FFD700"),
        theme(8, "Teal Modern", "#003B36", "#FFFFFF", "#9FE7DE", "#FFC857"),
        theme(9, "Slate Gray", "#2B2E33", "#FFFFFF", "#C7CCD1", "#4FC3F7"),
        theme(10, "Gold Elegance", "#1A1A1A", "#D4AF37", "#FFFFFF", "#D4AF37"),
        theme(11, "Forest Night", "#0A2818", "#E8F5E9", "#A5D6A7", "#FFB300"),
        theme(12, "Desert Sand", "#5C4423", "#FFF8E1", "#E0C9A6", "#8D6E63"),
        theme(13, "Midnight Indigo", "#191970", "#FFFFFF", "#C5CAE9", "#FFD54F"),
        theme(14, "Rose Quartz", "#4A1E2C", "#FFFFFF", "#F8BBD0", "#F06292"),
        theme(15, "Monochrome", "#000000", "#FFFFFF", "#AAAAAA", "#FFFFFF")
    )

    private fun theme(
        id: Int,
        name: String,
        background: String,
        primary: String,
        secondary: String,
        accent: String,
        /** Overrides the rotation below for themes whose stage was designed. */
        stage: MainContentType? = null
    ): ThemeConfig {
        // Alternate layout arrangement every few themes so the seeded set
        // also exercises different area positions, not just palettes.
        val variant = id % 3
        return ThemeConfig(
            themeId = id,
            name = name,
            backgroundColor = Color.parseColor(background),
            primaryColor = Color.parseColor(primary),
            secondaryColor = Color.parseColor(secondary),
            accentColor = Color.parseColor(accent),
            headerPosition = AreaPosition.TOP,
            clockPosition = if (variant == 0) AreaPosition.TOP else AreaPosition.RIGHT,
            prayerPanelPosition = if (variant == 2) AreaPosition.LEFT else AreaPosition.BOTTOM,
            mainContentType = stage ?: when (variant) {
                0 -> MainContentType.IMAGE_SLIDER
                1 -> MainContentType.PRAYER_SCHEDULE
                else -> MainContentType.ANNOUNCEMENT
            },
            runningTextPosition = AreaPosition.BOTTOM
        )
    }
}
