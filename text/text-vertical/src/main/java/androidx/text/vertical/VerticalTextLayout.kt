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
import android.text.TextPaint
import androidx.annotation.Px

/**
 * Represents the result of laying out text vertically.
 *
 * This class encapsulates the result of a vertical text layout process. It stores the layout's
 * properties and provides methods to draw the layout on a [Canvas].
 *
 * @property orientation The text orientation used for building this vertical layout.
 * @property paint The [TextPaint] used for building this vertical layout. Do not mutate this paint
 *   instance.
 */
public class VerticalTextLayout
private constructor(
    public val text: CharSequence,
    public val start: Int,
    public val end: Int,
    public val paint: TextPaint,
    @Px public val height: Float,
    @OrientationMode public val orientation: Int,
    private val result: LineBreaker.Result,
) {
    /** The width constraint of the vertical text in pixels. */
    @get:Px
    public val width: Float
        get() = result.width

    /**
     * Draws this text layout onto the specified [Canvas].
     *
     * @param canvas The [Canvas] to draw onto.
     * @param x The horizontal offset in pixels. The drawing origin is the top-right corner.
     * @param y The vertical offset in pixels. The drawing origin is the top-right corner.
     */
    public fun draw(canvas: Canvas, @Px x: Float, @Px y: Float) {
        result.draw(canvas, x, y, paint)
    }

    /**
     * Builder class for creating instances of [VerticalTextLayout].
     *
     * @param text The text to be laid out.
     * @param start The inclusive start offset of the target text range.
     * @param end The exclusive end offset of the target text range.
     * @param paint The [TextPaint] instance used for laying out the text.
     * @param height The height constraint in pixels.
     */
    public class Builder(
        private val text: CharSequence,
        private val start: Int,
        private val end: Int,
        private val paint: TextPaint,
        @Px private val height: Float,
    ) {
        private var _orientation: Int = TextOrientation.MIXED

        /**
         * Sets the text orientation.
         *
         * Defaults to [TextOrientation.MIXED].
         *
         * @param orientation The desired text orientation.
         * @return This [Builder] instance for chaining.
         */
        public fun setOrientation(@OrientationMode orientation: Int): Builder = apply {
            _orientation = orientation
        }

        /**
         * Builds the [VerticalTextLayout] instance.
         *
         * @return The constructed [VerticalTextLayout].
         */
        public fun build(): VerticalTextLayout {
            val lines = LineBreaker.breakTextIntoLines(text, start, end, paint, height)
            return VerticalTextLayout(text, start, end, paint, height, _orientation, lines)
        }
    }
}
