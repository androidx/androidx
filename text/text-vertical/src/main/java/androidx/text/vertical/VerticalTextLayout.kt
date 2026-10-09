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
import androidx.annotation.RestrictTo

/**
 * Represents the result of laying out text vertically.
 *
 * This class encapsulates the result of a vertical text layout process. It stores the layout's
 * properties and provides methods to draw the layout on a [Canvas].
 */
public class VerticalTextLayout
/**
 * @param text The text to be laid out.
 * @param start The inclusive start offset of the target text range.
 * @param end The exclusive end offset of the target text range.
 * @param paint The [TextPaint] instance used for laying out the text.
 * @param height The height constraint in pixels.
 * @param orientation The text orientation used for building this vertical layout.
 */
@JvmOverloads
constructor(
    internal val text: CharSequence = "",
    internal val start: Int = 0,
    internal val end: Int = text.length,
    internal val paint: TextPaint = TextPaint(),
    @Px internal val height: Float = 0f,
    internal val orientation: TextOrientation = TextOrientation.Mixed,
) {
    /** The computed width of the vertical text layout in pixels. */
    @get:Px
    public val width: Float
        get() = result.width

    /**
     * Vertical content height in pixels.
     *
     * Equals the maximum [getLineHeight] across all [lineCount] lines (columns).
     */
    @get:Px
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    // TODO(b/571750156): Remove @RestrictTo in 1.1.0-alpha01
    public val contentHeight: Float
        get() = result.height

    /** Uniform width in pixels of a single vertical line (column). */
    @get:Px
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    // TODO(b/571750156): Remove @RestrictTo in 1.1.0-alpha01
    public val columnWidth: Float
        get() = result.lineWidth

    /** The number of lines (columns) in this vertical text layout. */
    public val lineCount: Int
        get() = result.lineCount

    /**
     * Returns the laid-out height in pixels of the vertical line (column) at [line].
     *
     * @param line 0-based line (column) index (`0 <= line <` [lineCount])
     * @return line (column) height in pixels
     * @throws IndexOutOfBoundsException if [line] is outside `0 until` [lineCount]
     */
    @Px
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    // TODO(b/571750156): Remove @RestrictTo in 1.1.0-alpha01
    public fun getLineHeight(line: Int): Float = result.getLineHeight(line)

    /**
     * Returns the inclusive start character offset of the vertical line (column) at [line].
     *
     * @param line 0-based line (column) index (`0 <= line <` [lineCount])
     * @return inclusive start character offset in [text]
     * @throws IndexOutOfBoundsException if [line] is outside `0 until` [lineCount]
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    // TODO(b/571750156): Remove @RestrictTo in 1.1.0-alpha01
    public fun getLineStart(line: Int): Int = result.getLineStart(line)

    /**
     * Returns the exclusive end character offset of the vertical line (column) at [line].
     *
     * @param line 0-based line (column) index (`0 <= line <` [lineCount])
     * @return exclusive end character offset in [text]
     * @throws IndexOutOfBoundsException if [line] is outside `0 until` [lineCount]
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    // TODO(b/571750156): Remove @RestrictTo in 1.1.0-alpha01
    public fun getLineEnd(line: Int): Int = result.getLineEnd(line)

    private val result: LineBreaker.Result

    init {
        require(start <= end && end <= text.length && height >= 0)

        result = LineBreaker.breakTextIntoLines(text, start, end, paint, height, orientation)
    }

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
     * Capability query to determine whether [VerticalTextLayout] supports vertical text painting.
     * If this returns false, [draw] will have no effect.
     */
    public fun isVerticalTextSupported(): Boolean {
        return true
    }
}
