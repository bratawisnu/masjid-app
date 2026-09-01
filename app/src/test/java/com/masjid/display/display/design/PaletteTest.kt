package com.masjid.display.display.design

import android.graphics.Color
import androidx.core.graphics.ColorUtils
import com.masjid.display.data.local.ThemeSeedData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Guards the contrast floor the display depends on.
 *
 * Four of the seeded themes ship an accent that fails against their own
 * raised surface — Desert Sand's brown measures 1.4:1, which is invisible at
 * viewing distance. [Palette] corrects that on the fly, and these tests keep
 * both the correction and future theme edits honest.
 */
@RunWith(RobolectricTestRunner::class)
class PaletteTest {

    private companion object {
        /** WCAG AA for large text. */
        const val MIN_ACCENT = 4.5

        /** AA for incidental/supporting text. */
        const val MIN_MUTED = 3.0
    }

    @Test
    fun `every seeded theme keeps the accent readable on the raised surface`() {
        ThemeSeedData.seed().forEach { theme ->
            val palette = Palette.from(theme)
            val ratio = Palette.contrastRatio(palette.accent, palette.surface)
            assertTrue(
                "${theme.name}: accent contrast $ratio is below $MIN_ACCENT",
                ratio >= MIN_ACCENT
            )
        }
    }

    @Test
    fun `every seeded theme keeps primary text readable on base and surface`() {
        ThemeSeedData.seed().forEach { theme ->
            val palette = Palette.from(theme)
            assertTrue(
                "${theme.name}: primary on base too low",
                Palette.contrastRatio(palette.textPrimary, palette.base) >= MIN_ACCENT
            )
            assertTrue(
                "${theme.name}: primary on surface too low",
                Palette.contrastRatio(palette.textPrimary, palette.surface) >= MIN_ACCENT
            )
        }
    }

    @Test
    fun `every seeded theme keeps muted text above the incidental floor`() {
        ThemeSeedData.seed().forEach { theme ->
            val palette = Palette.from(theme)
            assertTrue(
                "${theme.name}: muted text too close to the background",
                Palette.contrastRatio(palette.textMuted, palette.base) >= MIN_MUTED
            )
        }
    }

    @Test
    fun `the raised surface is always distinguishable from the base`() {
        ThemeSeedData.seed().forEach { theme ->
            val palette = Palette.from(theme)
            assertNotEquals(
                "${theme.name}: surface is identical to base, so the mihrab bay would be invisible",
                palette.base,
                palette.surface
            )
        }
    }

    @Test
    fun `an accent that already passes is left untouched`() {
        // Mihrab's brass measures ~6.5:1 on its own surface and needs no
        // correction — the authored direction should survive untouched.
        val mihrab = ThemeSeedData.seed().first { it.name == "Mihrab" }
        assertEquals(mihrab.accentColor, Palette.from(mihrab).accent)
    }

    @Test
    fun `a failing accent keeps its hue`() {
        // Desert Sand's accent is corrected for contrast; the correction moves
        // lightness only, so the hue must survive.
        val desertSand = ThemeSeedData.seed().first { it.name == "Desert Sand" }
        val corrected = Palette.from(desertSand).accent
        assertNotEquals(desertSand.accentColor, corrected)

        val originalHsl = FloatArray(3)
        val correctedHsl = FloatArray(3)
        ColorUtils.colorToHSL(desertSand.accentColor, originalHsl)
        ColorUtils.colorToHSL(corrected, correctedHsl)
        // The walk moves lightness only; the couple of degrees of slack covers
        // rounding through the RGB round trip, not an actual hue shift.
        assertEquals("hue drifted during contrast correction", originalHsl[0], correctedHsl[0], 3.0f)
    }

    @Test
    fun `glow and hairline stay translucent`() {
        ThemeSeedData.seed().forEach { theme ->
            val palette = Palette.from(theme)
            // A fully opaque glow would paint a solid block over the arcade.
            assertTrue("${theme.name}: glow is not translucent", Color.alpha(palette.accentGlow) in 1..128)
            assertTrue("${theme.name}: hairline is not translucent", Color.alpha(palette.hairline) in 1..128)
        }
    }
}
