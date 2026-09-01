package com.masjid.display.display.design

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import com.masjid.display.data.local.entity.ThemeConfig
import kotlin.math.max
import kotlin.math.min

/**
 * The seven surface/text roles the display actually needs, derived from the
 * four colours a [ThemeConfig] stores.
 *
 * Deriving rather than storing means all 15 seeded themes get consistent
 * contrast for free, and a 16th theme only has to supply four colours to look
 * finished. It also gives `secondaryColor` a job — it was previously written
 * to the database and never read.
 *
 * Every derivation is luminance-aware, so light themes (e.g. "Minimal Light")
 * lift toward white where dark themes lift toward black, instead of a fixed
 * blend that would wash one of the two out.
 */
data class Palette(
    @ColorInt val base: Int,
    @ColorInt val surface: Int,
    @ColorInt val textPrimary: Int,
    @ColorInt val textMuted: Int,
    @ColorInt val accent: Int,
    @ColorInt val accentGlow: Int,
    @ColorInt val hairline: Int
) {
    companion object {

        /** WCAG AA for large text; the accent carries the countdown and active time. */
        private const val MIN_ACCENT_CONTRAST = 4.5

        fun from(theme: ThemeConfig): Palette {
            val base = theme.backgroundColor
            val isDarkBase = luminance(base) < 0.5f
            val surface = if (isDarkBase) lighten(base, 0.085f) else darken(base, 0.055f)
            val accent = ensureContrast(theme.accentColor, surface)

            return Palette(
                base = base,
                // The mihrab bay reads as raised, so it separates from the
                // field without a border. Dark themes lift, light themes sink.
                surface = surface,
                textPrimary = theme.primaryColor,
                // secondaryColor is the intended muted tone; fall back to a
                // faded primary when a theme leaves it too close to the base
                // to be readable.
                textMuted = pickMuted(theme.secondaryColor, theme.primaryColor, base),
                accent = accent,
                accentGlow = withAlpha(accent, 0.18f),
                hairline = withAlpha(theme.primaryColor, 0.12f)
            )
        }

        /**
         * A muted tone must sit clearly above the background but below the
         * primary. If the theme's secondary fails that, blend the primary
         * toward the background instead.
         */
        @ColorInt
        private fun pickMuted(
            @ColorInt secondary: Int,
            @ColorInt primary: Int,
            @ColorInt base: Int
        ): Int {
            val separation = kotlin.math.abs(luminance(secondary) - luminance(base))
            return if (separation >= 0.20f) secondary else blend(primary, base, 0.38f)
        }

        /**
         * Walks the colour's HSL lightness until it clears
         * [MIN_ACCENT_CONTRAST] against [against], away from that surface.
         *
         * Four of the seeded themes ship an accent that fails against their
         * own raised surface — Desert Sand's brown lands at 1.4:1, effectively
         * invisible. Moving lightness only (rather than blending toward white
         * or black) keeps the theme's hue and saturation intact, so the
         * correction stays faithful to the designer's colour.
         */
        @ColorInt
        private fun ensureContrast(@ColorInt color: Int, @ColorInt against: Int): Int {
            if (contrastRatio(color, against) >= MIN_ACCENT_CONTRAST) return color

            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(color, hsl)
            val lighter = luminance(against) < 0.5f
            val originalLightness = hsl[2]

            for (step in 1..100) {
                val delta = step / 100f
                hsl[2] = if (lighter) {
                    (originalLightness + delta).coerceAtMost(1f)
                } else {
                    (originalLightness - delta).coerceAtLeast(0f)
                }
                val candidate = ColorUtils.HSLToColor(hsl)
                if (contrastRatio(candidate, against) >= MIN_ACCENT_CONTRAST) return candidate
            }
            // Fully white/black still failing means the surface is mid-grey;
            // take the strongest available rather than the original.
            return ColorUtils.HSLToColor(hsl)
        }

        /** WCAG relative-luminance contrast ratio, 1.0 to 21.0. */
        fun contrastRatio(@ColorInt a: Int, @ColorInt b: Int): Double {
            val la = ColorUtils.calculateLuminance(a)
            val lb = ColorUtils.calculateLuminance(b)
            val lighter = maxOf(la, lb)
            val darker = minOf(la, lb)
            return (lighter + 0.05) / (darker + 0.05)
        }

        /** Perceived brightness, 0f (black) to 1f (white). */
        fun luminance(@ColorInt color: Int): Float =
            (0.299f * Color.red(color) + 0.587f * Color.green(color) + 0.114f * Color.blue(color)) / 255f

        @ColorInt
        fun blend(@ColorInt from: Int, @ColorInt to: Int, ratio: Float): Int {
            val r = ratio.coerceIn(0f, 1f)
            return Color.rgb(
                (Color.red(from) * (1 - r) + Color.red(to) * r).toInt(),
                (Color.green(from) * (1 - r) + Color.green(to) * r).toInt(),
                (Color.blue(from) * (1 - r) + Color.blue(to) * r).toInt()
            )
        }

        @ColorInt
        fun lighten(@ColorInt color: Int, amount: Float): Int = Color.rgb(
            min(255, (Color.red(color) + 255 * amount).toInt()),
            min(255, (Color.green(color) + 255 * amount).toInt()),
            min(255, (Color.blue(color) + 255 * amount).toInt())
        )

        @ColorInt
        fun darken(@ColorInt color: Int, amount: Float): Int = Color.rgb(
            max(0, (Color.red(color) - 255 * amount).toInt()),
            max(0, (Color.green(color) - 255 * amount).toInt()),
            max(0, (Color.blue(color) - 255 * amount).toInt())
        )

        @ColorInt
        fun withAlpha(@ColorInt color: Int, alpha: Float): Int =
            Color.argb((255 * alpha.coerceIn(0f, 1f)).toInt(), Color.red(color), Color.green(color), Color.blue(color))
    }
}
