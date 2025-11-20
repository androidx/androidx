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
         * This value represents a shear angle of 15 degree (tan(15 deg)).
         */
        // This value is derived from Chrome's vertical writing implementation.
        // https://source.chromium.org/chromium/chromium/src/+/main:third_party/blink/renderer/platform/fonts/shaping/shape_result_bloberizer.cc;drc=c58cc9d7cce70b7f52b985e48aa126a4ba705cf6;l=676
        public const val DEFAULT_FONT_SHEAR: Float = 0.2679492f
    }
}

/**
 * A span that applies emphasis marks to text in a vertical layout.
 *
 * This span is designed for use with [VerticalTextLayout] and will not have an effect in other
 * contexts. It allows for the application of various emphasis styles, such as dots, circles, or
 * triangles, above or next to characters in vertical text.
 *
 * The `EmphasisSpan` takes a style, a boolean indicating whether the mark should be filled, and a
 * scale factor for the size of the mark.
 *
 * @param style The style of the emphasis mark. This value determines the size of the emphasis mark
 *   relative to the font size. A scale of 0.5f means the emphasis mark will be half the size of the
 *   text.
 * @param isFilled Whether the mark should be filled or outlined.
 * @param scale The scale factor for the size of the mark. When `true`, the emphasis mark will be
 *   drawn as a solid shape. When `false`, it will be drawn as an outline.
 */
public class EmphasisSpan(
    @EmphasisStyleType public val style: Int = DEFAULT_EMPHASIS_STYLE,
    public val isFilled: Boolean = DEFAULT_EMPHASIS_FILL,
    public val scale: Float = DEFAULT_SCALE,
) : MetricAffectingSpan() {

    internal val letter =
        when (style) {
            STYLE_DOT -> if (isFilled) "\u2022" else "\u25E6"
            STYLE_CIRCLE -> if (isFilled) "\u25CF" else "\u25CB"
            STYLE_DOUBLE_CIRCLE -> if (isFilled) "\u25C9" else "\u25CE"
            STYLE_TRIANGLE -> if (isFilled) "\u25B2" else "\u25B3"
            STYLE_SESAME -> if (isFilled) "\uFE45" else "\uFE46"
            else -> throw RuntimeException("Unknown emphasis style: $style")
        }

    override fun updateMeasureState(p0: TextPaint) {}

    override fun updateDrawState(p0: TextPaint?) {}

    public companion object {
        /** Emphasis mark is a small circle. The filled dot is U+2022, open dot is U+25E6. */
        public const val STYLE_DOT: Int = 0
        /** Emphasis mark is a large circle. The filled dot is U+25CF, open dot is U+25CB. */
        public const val STYLE_CIRCLE: Int = 1
        /** Emphasis mark is a double circle. The filled dot is U+25C9, open dot is U+25CE. */
        public const val STYLE_DOUBLE_CIRCLE: Int = 2
        /** Emphasis mark is a triangle. The filled dot is U+25B2, open dot is U+25B3. */
        public const val STYLE_TRIANGLE: Int = 3
        /** Emphasis mark is a sesame. The filled dot is U+FE45, open dot is U+FE46. */
        public const val STYLE_SESAME: Int = 4

        @IntDef(
            value = [STYLE_DOT, STYLE_CIRCLE, STYLE_DOUBLE_CIRCLE, STYLE_TRIANGLE, STYLE_SESAME]
        )
        internal annotation class EmphasisStyleType

        /** The default scale factor for emphasis marks. */
        public const val DEFAULT_SCALE: Float = 0.5f
        /** The default style used for emphasis marks, typically a dot. */
        public const val DEFAULT_EMPHASIS_STYLE: Int = STYLE_DOT
        /** The default value for whether the emphasis mark should be filled. */
        public const val DEFAULT_EMPHASIS_FILL: Boolean = true
    }
}
