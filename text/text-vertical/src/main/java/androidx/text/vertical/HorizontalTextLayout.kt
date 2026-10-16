/*
 * Copyright 2026 The Android Open Source Project
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
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristic
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.annotation.ColorInt
import androidx.annotation.Px
import androidx.annotation.RestrictTo
import kotlin.math.ceil
import kotlin.math.max

/**
 * Builds a [StaticLayout] sized to the tightest visible line width within `[minWidth, maxWidth]`.
 *
 * Preserves [TextPaint.bgColor] across [StaticLayout.Builder.build], which clears `paint.bgColor`
 * inside the [Layout] constructor.
 *
 * @param text text to lay out
 * @param paint [TextPaint] used for measuring and laying out [text]
 * @param alignment horizontal line alignment
 * @param textDir paragraph text direction heuristic
 * @param minWidth minimum layout width in pixels
 * @param maxWidth maximum layout width in pixels
 * @return measured [StaticLayout] shrunk to the widest visible line within `[minWidth, maxWidth]`
 */
internal fun buildHorizontalLayout(
    text: CharSequence,
    paint: TextPaint,
    alignment: Layout.Alignment,
    textDir: TextDirectionHeuristic,
    @Px minWidth: Int,
    @Px maxWidth: Int,
): StaticLayout {
    fun obtain(w: Int): StaticLayout {
        val bgColor = paint.bgColor
        val layout =
            StaticLayout.Builder.obtain(text, 0, text.length, paint, w)
                .setAlignment(alignment)
                .setTextDirection(textDir)
                .build()
        // The Layout constructor zeroes paint.bgColor. Restore it so RubySpan, EmphasisSpan, and
        // plainTextBackgroundRects receive the style background color.
        paint.bgColor = bgColor
        return layout
    }
    val width =
        if (minWidth == maxWidth) {
            maxWidth
        } else {
            ceil(Layout.getDesiredWidth(text, 0, text.length, paint))
                .toInt()
                .coerceIn(minWidth, maxWidth)
        }
    val layout = obtain(width)
    if (layout.lineCount <= 1 || minWidth == width) return layout
    var maxLineWidth = 0f
    for (i in 0 until layout.lineCount) {
        maxLineWidth = max(maxLineWidth, layout.getLineMax(i))
    }
    val wrappedWidth = ceil(maxLineWidth).toInt().coerceAtLeast(minWidth)
    return if (wrappedWidth < width) obtain(wrappedWidth) else layout
}

/**
 * Measured horizontal text layout with precomputed plain text background boxes.
 *
 * Queries line metrics and draws the layout and plain text backgrounds onto a [Canvas].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
// TODO(b/571750156): Remove @RestrictTo in 1.1.0-alpha01
public class HorizontalTextLayout
/**
 * @param text text to lay out
 * @param start inclusive start offset of the target text range in [text]
 * @param end exclusive end offset of the target text range in [text]
 * @param paint [TextPaint] used for measuring and laying out [text]
 * @param minWidth minimum layout width in pixels
 * @param maxWidth maximum layout width in pixels
 * @param alignment horizontal line alignment
 * @param textDir paragraph text direction heuristic
 */
@JvmOverloads
constructor(
    internal val text: CharSequence = "",
    internal val start: Int = 0,
    internal val end: Int = text.length,
    internal val paint: TextPaint = TextPaint(),
    @Px internal val minWidth: Int = 0,
    @Px internal val maxWidth: Int = Int.MAX_VALUE,
    internal val alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
    internal val textDir: TextDirectionHeuristic = TextDirectionHeuristics.FIRSTSTRONG_LTR,
) {
    /** The computed width of the horizontal text layout in pixels. */
    @get:Px
    public val width: Int
        get() = layout.width

    /** The computed height of the horizontal text layout in pixels. */
    @get:Px
    public val height: Int
        get() = layout.height

    /** The number of lines in this horizontal text layout. */
    public val lineCount: Int
        get() = layout.lineCount

    /**
     * Returns the left horizontal coordinate in pixels of the line at [line].
     *
     * @param line 0-based line index (`0 <= line <` [lineCount])
     * @return left coordinate in pixels
     */
    @Px public fun getLineLeft(line: Int): Float = layout.getLineLeft(line)

    /**
     * Returns the right horizontal coordinate in pixels of the line at [line].
     *
     * @param line 0-based line index (`0 <= line <` [lineCount])
     * @return right coordinate in pixels
     */
    @Px public fun getLineRight(line: Int): Float = layout.getLineRight(line)

    /**
     * Returns the top vertical coordinate in pixels of the line at [line].
     *
     * @param line 0-based line index (`0 <= line <` [lineCount])
     * @return top coordinate in pixels
     */
    @Px public fun getLineTop(line: Int): Int = layout.getLineTop(line)

    /**
     * Returns the bottom vertical coordinate in pixels of the line at [line].
     *
     * @param line 0-based line index (`0 <= line <` [lineCount])
     * @return bottom coordinate in pixels
     */
    @Px public fun getLineBottom(line: Int): Int = layout.getLineBottom(line)

    /**
     * Returns the baseline vertical coordinate in pixels of the line at [line].
     *
     * @param line 0-based line index (`0 <= line <` [lineCount])
     * @return baseline coordinate in pixels
     */
    @Px public fun getLineBaseline(line: Int): Int = layout.getLineBaseline(line)

    /**
     * Returns the visible width in pixels (excluding trailing whitespace) of the line at [line].
     *
     * @param line 0-based line index (`0 <= line <` [lineCount])
     * @return visible line width in pixels
     */
    @Px public fun getLineMax(line: Int): Float = layout.getLineMax(line)

    /**
     * Returns the inclusive start character offset of the line at [line].
     *
     * @param line 0-based line index (`0 <= line <` [lineCount])
     * @return inclusive start character offset in [text]
     */
    public fun getLineStart(line: Int): Int = start + layout.getLineStart(line)

    /**
     * Returns the exclusive end character offset of the line at [line].
     *
     * @param line 0-based line index (`0 <= line <` [lineCount])
     * @return exclusive end character offset in [text]
     */
    public fun getLineEnd(line: Int): Int = start + layout.getLineEnd(line)

    internal val layout: StaticLayout
    internal val plainBackgroundRects: List<RectF>
    @ColorInt internal val backgroundColor: Int

    init {
        require(start in 0..end && end <= text.length)
        require(minWidth >= 0 && minWidth <= maxWidth)

        val targetText =
            if (start == 0 && end == text.length) text else text.subSequence(start, end)
        val preparedText = prepareHorizontalText(targetText)
        layout =
            buildHorizontalLayout(
                text = preparedText,
                paint = paint,
                alignment = alignment,
                textDir = textDir,
                minWidth = minWidth,
                maxWidth = maxWidth,
            )
        backgroundColor = paint.bgColor
        plainBackgroundRects =
            if (backgroundColor != 0) {
                plainTextBackgroundRects(layout, preparedText, paint)
            } else {
                emptyList()
            }
    }

    /**
     * Draws this text layout onto [canvas] with its top-left corner at ([x], [y]).
     *
     * @param canvas [Canvas] to draw onto
     * @param x horizontal offset in pixels of the top-left drawing origin
     * @param y vertical offset in pixels of the top-left drawing origin
     */
    public fun draw(canvas: Canvas, @Px x: Float, @Px y: Float) {
        canvas.withSave {
            if (x != 0f || y != 0f) {
                translate(x, y)
            }
            if (plainBackgroundRects.isNotEmpty()) {
                tempPaint { bgPaint ->
                    bgPaint.reset()
                    bgPaint.color = backgroundColor
                    for (i in plainBackgroundRects.indices) {
                        drawRect(plainBackgroundRects[i], bgPaint)
                    }
                }
            }
            layout.draw(this)
        }
    }
}
