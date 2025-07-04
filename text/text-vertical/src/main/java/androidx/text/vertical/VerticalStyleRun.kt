/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.text.vertical

import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import androidx.annotation.IntDef
import androidx.text.vertical.FontShearSpan.Companion.DEFAULT_FONT_SHEAR

/**
 * A span that applies a shear (skew) transformation to the vertical writing text.
 *
 * This span inherits from [MetricAffectingSpan], but it is specifically designed for use within a
 * [VerticalTextLayout] and will not have an effect in other contexts.
 *
 * It is used to achieve an italic-like effect for vertical text layout, where traditional italic
 * fonts may not render correctly.
 *
 * The shear value represents the horizontal skew factor for rotated or tate-chu-yoko text. For
 * upright text, this value is used as the vertical skew factor.
 *
 * See [DEFAULT_FONT_SHEAR] for the default value.
 *
 * Note: This span only works with VerticalTextLayout.
 *
 * @property fontShear The shear factor to apply to the text.
 */
public class FontShearSpan(public val fontShear: Float = DEFAULT_FONT_SHEAR) :
    MetricAffectingSpan() {
    override fun updateMeasureState(p0: TextPaint) {}

    override fun updateDrawState(p0: TextPaint?) {}

    public companion object {
        /**
         * Default constant for fontShear.
         *
         * This value represents a shear angle of approximately 9.53 degrees (atan(0.1678842)).
         *
         * Angles in the range of 8 to 12 degrees are widely considered to provide a visually
         * balanced emphasis while maintaining readability, aligning with the general slope observed
         * in many Roman italic and oblique typefaces.
         *
         * This specific value is adopted from the W3C Timed Text Markup Language 2 (TTML2)
         * specification for the
         * [tts:fontShear](https://www.w3.org/TR/2018/REC-ttml2-20181108/#style-attribute-fontShear)
         * attribute which aims to standardize emphasis for such scripts.
         */
        public const val DEFAULT_FONT_SHEAR: Float = 0.1678842f
    }
}

/**
 * Defines the available styles for emphasis marks in vertical text.
 *
 * These styles are used to visually highlight characters. The `EmphasisSpan` class uses these
 * constants to determine the shape of the emphasis mark.
 */
public object EmphasisStyle {
    /** Emphasis mark is a small circle. The filled dot is U+2022, open dot is U+25E6. */
    public const val DOT: Int = 0
    /** Emphasis mark is a large circle. The filled dot is U+25CF, open dot is U+25CB. */
    public const val CIRCLE: Int = 1
    /** Emphasis mark is a double circle. The filled dot is U+25C9, open dot is U+25CE. */
    public const val DOUBLE_CIRCLE: Int = 2
    /** Emphasis mark is a triangle. The filled dot is U+25B2, open dot is U+25B3. */
    public const val TRIANGLE: Int = 3
    /** Emphasis mark is a sesame. The filled dot is U+FE45, open dot is U+FE46. */
    public const val SESAME: Int = 4
}

@IntDef(
    value =
        [
            EmphasisStyle.DOT,
            EmphasisStyle.CIRCLE,
            EmphasisStyle.DOUBLE_CIRCLE,
            EmphasisStyle.TRIANGLE,
            EmphasisStyle.SESAME,
        ]
)
internal annotation class EmphasisStyleType

/**
 * A span that applies emphasis marks to text in a vertical layout.
 *
 * This span is designed for use with [VerticalTextLayout] and will not have an effect in other
 * contexts. It allows for the application of various emphasis styles, such as dots, circles, or
 * triangles, above or next to characters in vertical text.
 *
 * The `EmphasisSpan` takes a style (defined in [EmphasisStyle]), a boolean indicating whether the
 * mark should be filled, and a scale factor for the size of the mark.
 *
 * @see EmphasisStyle for available emphasis mark styles.
 */
public class EmphasisSpan private constructor(public val letter: String, public val scale: Float) :
    MetricAffectingSpan() {

    /**
     * @param style The style of the emphasis mark. This value determines the size of the emphasis
     *   mark relative to the font size. A scale of 0.5f means the emphasis mark will be half the
     *   size of the text.
     * @param filled Whether the mark should be filled or outlined.
     * @param scale The scale factor for the size of the mark. When `true`, the emphasis mark will
     *   be drawn as a solid shape. When `false`, it will be drawn as an outline.
     */
    public constructor(
        @EmphasisStyleType style: Int = DEFAULT_EMPHASIS_STYLE,
        filled: Boolean = DEFAULT_EMPHASIS_FILL,
        scale: Float = DEFAULT_SCALE,
    ) : this(
        (when (style) {
            EmphasisStyle.DOT -> if (filled) "\u2022" else "\u25E6"
            EmphasisStyle.CIRCLE -> if (filled) "\u25CF" else "\u25CB"
            EmphasisStyle.DOUBLE_CIRCLE -> if (filled) "\u25C9" else "\u25CE"
            EmphasisStyle.TRIANGLE -> if (filled) "\u25B2" else "\u25B3"
            EmphasisStyle.SESAME -> if (filled) "\uFE45" else "\uFE46"
            else -> throw RuntimeException("Unknown emphasis style: $style")
        }),
        scale,
    )

    override fun updateMeasureState(p0: TextPaint) {}

    override fun updateDrawState(p0: TextPaint?) {}

    public companion object {
        /** The default scale factor for emphasis marks. */
        public const val DEFAULT_SCALE: Float = 0.5f
        /** The default style used for emphasis marks, typically a dot. */
        public const val DEFAULT_EMPHASIS_STYLE: Int = EmphasisStyle.DOT
        /** The default value for whether the emphasis mark should be filled. */
        public const val DEFAULT_EMPHASIS_FILL: Boolean = true
    }
}
