package com.masjid.display.display.view

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.masjid.display.display.design.Palette
import com.masjid.display.display.design.Scale
import com.masjid.display.display.design.Typography

/**
 * A text slide: one message, set large, holding the stage for its turn.
 *
 * Deliberately built from the same eyebrow/rule/body parts as
 * [AnnouncementBoardView] rather than given a look of its own. Both are the
 * mosque speaking in words on the same surface, and two different treatments
 * would read as two different kinds of notice when the only real difference is
 * where the caretaker typed it.
 *
 * Unlike the announcement board this holds no cycle of its own — the slideshow
 * owns the timing, because a text slide takes its turn among images and video.
 */
class SlideTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val eyebrowText = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, Scale.text(context, Scale.ANNOUNCEMENT_EYEBROW))
        typeface = Typography.label(context)
        includeFontPadding = false
        letterSpacing = 0.22f
        text = "INFORMASI"
    }

    private val rule = View(context)

    private val bodyText = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, Scale.text(context, Scale.ANNOUNCEMENT_BODY))
        typeface = Typography.heading(context)
        includeFontPadding = false
        maxLines = 4
        ellipsize = TextUtils.TruncateAt.END
        setLineSpacing(0f, 1.12f)
    }

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_VERTICAL

        addView(eyebrowText)
        addView(
            rule,
            LayoutParams(LayoutParams.MATCH_PARENT, Scale.px(context, 2f)).apply {
                topMargin = Scale.px(context, 10f)
                bottomMargin = Scale.dim(context, Scale.GUTTER)
            }
        )
        addView(bodyText)
    }

    fun applyPalette(palette: Palette) {
        eyebrowText.setTextColor(palette.accent)
        rule.setBackgroundColor(palette.hairline)
        bodyText.setTextColor(palette.textPrimary)
    }

    fun setBody(text: String?) {
        bodyText.text = text.orEmpty()
    }
}
