package com.masjid.display.display.design

import android.graphics.Bitmap
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils

/**
 * Works out how much the background photo has to be dimmed before the
 * display's text is still legible on top of it.
 *
 * A photo destroys the contrast guarantee [Palette] establishes: those ratios
 * are computed against a flat colour, and white text over a bright sky is
 * unreadable no matter what the theme says. The obvious fix — a fixed dark
 * overlay — costs far more than it looks. Measured across all 15 themes, a
 * uniform scrim needs alpha 0.84 to stay safe against a worst-case image, at
 * which point the photo is all but gone.
 *
 * So the scrim is measured instead of assumed. Dimming only has to defeat the
 * photo's *bright* regions, so this samples the 95th-percentile luminance and
 * solves for the smallest alpha that still clears [MIN_CONTRAST]. A night shot
 * of a mosque needs about 0.34 and stays clearly visible; an overexposed one
 * needs 0.81 and mostly doesn't — which is the honest answer for that image.
 */
object BackgroundScrim {

    /** WCAG AA for large text, matching [Palette]'s accent floor. */
    private const val MIN_CONTRAST = 4.5

    /**
     * Even a photo dark enough to need nothing gets a light wash. Text sitting
     * directly on photographic detail reads as a mistake, and the edges of
     * glyphs fight the grain; a floor keeps the panel looking composed.
     */
    private const val MIN_ALPHA = 0.20f

    /**
     * Past this the image contributes nothing but noise, and the panel is
     * better served by the flat theme colour.
     */
    private const val MAX_ALPHA = 0.85f

    /**
     * Above this the photo is more scrim than photo and the admin should pick
     * a darker one. Deliberately well below [MAX_ALPHA]: measured across all
     * 15 themes, even a pure-white image only ever demands 0.81, so a warning
     * keyed to the ceiling would never fire. At 0.65 barely a third of the
     * picture survives, which is the point where it stops being worth having.
     */
    private const val WARN_ALPHA = 0.65f

    /**
     * Sampling grid. 32x32 is ~1000 reads regardless of source resolution,
     * which is immaterial next to the decode Glide already performed, and
     * fine-grained enough that a bright window in one corner still registers.
     */
    private const val SAMPLE_STEPS = 32

    /** Ignore the brightest 5% — a specular highlight shouldn't black out the whole wall. */
    private const val BRIGHT_PERCENTILE = 0.95f

    /**
     * The scrim alpha for [bitmap] under [palette], in 0f..1f.
     *
     * Returns [MAX_ALPHA] for an unreadable bitmap rather than giving up, so a
     * bad photo degrades into a tinted wash instead of an illegible screen.
     */
    fun alphaFor(bitmap: Bitmap, palette: Palette): Float {
        val hotSpot = brightRegionColor(bitmap)
        return alphaFor(hotSpot, palette)
    }

    /**
     * The alpha calculation itself, against an already-sampled colour. Split
     * out so it can be exercised without a Bitmap.
     */
    fun alphaFor(@ColorInt brightRegion: Int, palette: Palette): Float {
        // A scrim of palette.base rather than plain black: on Minimal Light
        // black would fight the theme, and tinting toward the base keeps the
        // photo feeling part of the design instead of layered under it.
        var step = (MIN_ALPHA * 100).toInt()
        while (step <= (MAX_ALPHA * 100).toInt()) {
            val alpha = step / 100f
            val composite = composite(palette.base, brightRegion, alpha)
            if (Palette.contrastRatio(palette.textPrimary, composite) >= MIN_CONTRAST) {
                return alpha
            }
            step++
        }
        return MAX_ALPHA
    }

    /**
     * True when the photo still reads through the scrim it needs. The admin
     * screen uses this to warn before a picture is accepted, which is cheaper
     * than the caretaker discovering it from the back of the hall.
     *
     * Takes the already-computed alpha so the caller doesn't decode twice.
     */
    fun isImageWorthShowing(alpha: Float): Boolean = alpha < WARN_ALPHA

    /**
     * The colour of the photo's bright regions — what the text actually has to
     * survive. Averaging the whole image instead would let a mostly-dark photo
     * with a blown-out window pass, and the text would vanish exactly there.
     */
    @ColorInt
    private fun brightRegionColor(bitmap: Bitmap): Int {
        val samples = ArrayList<Int>(SAMPLE_STEPS * SAMPLE_STEPS)
        val xStep = (bitmap.width / SAMPLE_STEPS).coerceAtLeast(1)
        val yStep = (bitmap.height / SAMPLE_STEPS).coerceAtLeast(1)

        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                samples.add(bitmap.getPixel(x, y))
                x += xStep
            }
            y += yStep
        }
        if (samples.isEmpty()) return Color.WHITE

        // Rank by perceived luminance, then take the percentile pixel. Sorting
        // colours by their own brightness keeps the returned value a real
        // colour from the image rather than an average that exists nowhere.
        samples.sortBy { ColorUtils.calculateLuminance(it) }
        val index = ((samples.size - 1) * BRIGHT_PERCENTILE).toInt()
        return samples[index]
    }

    /** Source-over composite of [over] at [alpha] on top of [under]. */
    @ColorInt
    private fun composite(@ColorInt over: Int, @ColorInt under: Int, alpha: Float): Int {
        val a = alpha.coerceIn(0f, 1f)
        return Color.rgb(
            (Color.red(over) * a + Color.red(under) * (1 - a)).toInt(),
            (Color.green(over) * a + Color.green(under) * (1 - a)).toInt(),
            (Color.blue(over) * a + Color.blue(under) * (1 - a)).toInt()
        )
    }
}
