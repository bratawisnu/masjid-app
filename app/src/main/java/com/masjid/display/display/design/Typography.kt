package com.masjid.display.display.design

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat

/**
 * Resolves the display's five type roles.
 *
 * Faces are looked up by resource name at runtime rather than via `R.font.*`
 * so the app still builds and runs before the TTFs are dropped into
 * `res/font/`, falling back to the closest stock Android family. Mosque
 * devices are frequently offline, so nothing is fetched at runtime — the
 * files are bundled or they are absent.
 *
 * To activate the intended faces, add to `app/src/main/res/font/`:
 *   archivo_expanded_bold.ttf   (Archivo Expanded 700)
 *   archivo_condensed_semibold.ttf (Archivo SemiCondensed 600)
 *   archivo_bold.ttf            (Archivo 700)
 *   archivo_medium.ttf          (Archivo 500)
 *   amiri_regular.ttf           (Amiri 400)
 */
object Typography {

    private val cache = mutableMapOf<String, Typeface>()

    /** Clock and prayer times. Wide face so digits hold weight at distance. */
    fun numeric(context: Context): Typeface =
        resolve(context, "archivo_expanded_bold") ?: Typeface.create("sans-serif", Typeface.BOLD)

    /** Prayer name labels — condensed, so seven bays fit without crowding. */
    fun label(context: Context): Typeface =
        resolve(context, "archivo_condensed_semibold") ?: Typeface.create("sans-serif-condensed", Typeface.BOLD)

    /** Mosque name and other display headings. */
    fun heading(context: Context): Typeface =
        resolve(context, "archivo_bold") ?: Typeface.create("sans-serif", Typeface.BOLD)

    /** Body copy: city, dates, running text. */
    fun body(context: Context): Typeface =
        resolve(context, "archivo_medium") ?: Typeface.create("sans-serif-medium", Typeface.NORMAL)

    /**
     * Arabic prayer names. When Amiri is absent the platform's Arabic
     * fallback (Noto Naskh on most builds) renders the script correctly —
     * less characterful, but never tofu.
     */
    fun arabic(context: Context): Typeface =
        resolve(context, "amiri_regular") ?: Typeface.SERIF

    /** True once the intended faces are bundled — useful for a settings hint. */
    fun bundledFacesPresent(context: Context): Boolean =
        resolve(context, "archivo_expanded_bold") != null

    private fun resolve(context: Context, name: String): Typeface? = cache[name] ?: run {
        val id = context.resources.getIdentifier(name, "font", context.packageName)
        if (id == 0) return null
        val typeface = runCatching { ResourcesCompat.getFont(context, id) }.getOrNull() ?: return null
        cache[name] = typeface
        typeface
    }
}
