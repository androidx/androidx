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

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.ReplacementSpan

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
 * @param style The style of the emphasis mark.
 * @param isFilled Whether the mark should be filled or outlined. When `true`, the emphasis mark
 *   will be drawn as a solid shape. When `false`, it will be drawn as an outline.
 * @param position The position of the emphasis mark relative to the text. NOTE: this is reserved
 *   value, to be implemented (b/495457561).
 * @param scale The scale factor for the size of the mark. This value determines the size of the
 *   emphasis mark relative to the font size. A scale of 0.5f means the emphasis mark will be half
 *   the size of the text.
 */
public class EmphasisSpan
@JvmOverloads
constructor(
    public val style: EmphasisStyle = DEFAULT_EMPHASIS_STYLE,
    public val isFilled: Boolean = DEFAULT_EMPHASIS_FILL,
    public val position: AnnotationPosition = AnnotationPosition.Before,
    public val scale: Float = DEFAULT_SCALE,
) : ReplacementSpan() {
    private val impl by lazy {
        HorizontalSpanImpl(
            { _, text, start, end -> LayoutKey(start, end, text) },
            { paint, text, start, end ->
                HorizontalEmphasisSpanLayout(text, start, end, letter, paint, scale)
            },
        )
    }

    internal val letter =
        when (style) {
            EmphasisStyle.Dot -> if (isFilled) "\u2022" else "\u25E6"
            EmphasisStyle.Circle -> if (isFilled) "\u25CF" else "\u25CB"
            EmphasisStyle.DoubleCircle -> if (isFilled) "\u25C9" else "\u25CE"
            EmphasisStyle.Triangle -> if (isFilled) "\u25B2" else "\u25B3"
            EmphasisStyle.Sesame -> if (isFilled) "\uFE45" else "\uFE46"
            else -> throw RuntimeException("Unknown emphasis style: $style")
        }

    override fun getSize(
        paint: Paint,
        text: CharSequence?,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?,
    ): Int = impl.getSize(paint, text, start, end, fm)

    override fun draw(
        canvas: Canvas,
        text: CharSequence?,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint,
    ) {
        impl.draw(canvas, text, start, end, x, top, y, bottom, paint)
    }

    public companion object {
        /** The default scale factor for emphasis marks. */
        public const val DEFAULT_SCALE: Float = 0.5f
        /** The default style used for emphasis marks, typically a dot. */
        @JvmField public val DEFAULT_EMPHASIS_STYLE: EmphasisStyle = EmphasisStyle.Dot
        /** The default value for whether the emphasis mark should be filled. */
        public const val DEFAULT_EMPHASIS_FILL: Boolean = true
        /** The default position of the emphasis mark relative to the text. */
        @JvmField public val DEFAULT_POSITION: AnnotationPosition = AnnotationPosition.Before
    }
}
