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
import android.graphics.Paint.FontMetrics
import android.graphics.Paint.FontMetricsInt
import android.icu.lang.UCharacter
import android.icu.lang.UCharacterCategory
import android.text.Spanned
import android.text.TextPaint
import android.text.style.CharacterStyle
import java.util.LinkedList
import kotlin.concurrent.getOrSet
import kotlin.math.max
import kotlin.math.min

// Constants for better readability.
private const val HORIZONTAL = false
private const val VERTICAL = true

/**
 * Creates a new LayoutRun instance.
 *
 * @param text The text to be laid out.
 * @param start The inclusive starting index.
 * @param end The exclusive ending index.
 * @param paint The TextPaint object used to measure and draw the text.
 * @param orientation The resolved orientation mode.
 */
internal fun createLayoutRun(
    text: CharSequence,
    start: Int,
    end: Int,
    paint: TextPaint,
    orientation: ResolvedOrientation,
): LayoutRun =
    when (orientation) {
        ResolvedOrientation.Rotate -> RotateLayoutRun(text, start, end, paint)
        ResolvedOrientation.Upright -> UprightLayoutRun(text, start, end, paint)
        ResolvedOrientation.TateChuYoko -> TateChuYokoLayoutRun(text, start, end, paint)
    }

/**
 * Represents a segment of text laid out with specific orientation and styling. This is an internal
 * class used for managing text layout variations.
 */
internal sealed class LayoutRun(val text: CharSequence, val start: Int, val end: Int) {
    /**
     * Distance from left most position from the baseline in pixels.
     *
     * This is usually negative value.
     *
     * To get the next drawing horizontal coordinate, add this amount to the baseline. To get the
     * width of this run, subtract this amount from the [rightSideOffset] value, i.e. width =
     * right - left.
     */
    abstract val leftSideOffset: Float

    /**
     * Distance from right most position from the baseline in pixels.
     *
     * This is usually positive value.
     *
     * To get baseline of this run, subtract this amount from the drawing offset. To get the width
     * of this run, subtract [leftSideOffset] from this amount, i.e. width = right - left.
     */
    abstract val rightSideOffset: Float

    /**
     * Distance from the top to bottom in pixels.
     *
     * This is always positive value.
     */
    abstract val height: Float

    /**
     * Distance from the right to left in pixels.
     *
     * This is always positive value.
     */
    val width
        get() = rightSideOffset - leftSideOffset

    /**
     * Calculates the character advances and stores them in the `out` array.
     *
     * [out] must have at least [end - start] elements.
     *
     * @param out The array to store the character advances.
     * @param paint The paint used for text rendering.
     */
    abstract fun getCharAdvances(out: FloatArray, paint: TextPaint)

    /**
     * Draws the laid out text on the canvas.
     *
     * @param canvas The canvas to draw on.
     * @param originX The x-coordinate of the top-right corner of the text on the canvas.
     * @param originY The y-coordinate of the top-right corner of the text on the canvas.
     * @param paint The paint used for text rendering.
     */
    abstract fun draw(canvas: Canvas, originX: Float, originY: Float, paint: TextPaint)

    /** Draws the background rectangle on the Canvas. */
    protected fun drawBackground(
        canvas: Canvas,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        bgColor: Int,
    ) {
        if (bgColor == 0) {
            return
        }
        tempPaint { bgPaint ->
            bgPaint.color = bgColor
            canvas.drawRect(left, top, left + width, top + height, bgPaint)
        }
    }
}

/**
 * Represents a "Tate-chu-yoko" (horizontal in vertical) text layout run.
 *
 * @param text The text this layout represents.
 * @param start The starting inclusive index of the text.
 * @param end The ending exclusive index of the text.
 * @param paint The paint used for text rendering.
 */
internal class TateChuYokoLayoutRun(text: CharSequence, start: Int, end: Int, paint: TextPaint) :
    LayoutRun(text, start, end) {
    override val height: Float
        get() = descent - ascent

    override val leftSideOffset: Float
    override val rightSideOffset: Float

    /** The ascent of the tate-chu-yoko text. */
    private val ascent: Float

    /** The descent of the tate-chu-yoko text. */
    private val descent: Float

    /**
     * The horizontal scaling factor applied to the text.
     *
     * If the text is too long to fit in the surrounding width, this factor will be used to shrink
     * the text to fit in the given width.
     */
    private val scaleX: Float

    /** The actual width occupied by the text. This is used for centering. */
    private val textWidth: Float

    init {
        var leftSide = 0f
        var rightSide = 0f
        var maxAscent = 0
        var maxDescent = 0

        val fontMetrics = FontMetricsInt()

        var textWidth = 0f
        text.forStyleRuns(start, end, paint, HORIZONTAL) {
            rStart,
            rEnd,
            rPaint,
            _,
            _,
            _,
            emphasisScale ->
            val emphasisWidth = rPaint.textSize * emphasisScale

            val rCount = rEnd - rStart

            leftSide = min(leftSide, -rPaint.textSize * 0.5f)
            rightSide = max(rightSide, rPaint.textSize * 0.5f + emphasisWidth)

            rPaint.getFontMetricsInt(text, rStart, rCount, rStart, rCount, false, fontMetrics)
            maxAscent = min(maxAscent, fontMetrics.ascent)
            maxDescent = max(maxDescent, fontMetrics.descent)

            textWidth += rPaint.measureText(text, rStart, rEnd) + emphasisWidth
        }

        val w = rightSide - leftSide
        var scaleX = 1f
        // Adjust the width and scaling if necessary to fit the text within the desired bounds.
        if (textWidth > w) {
            if (textWidth <= w * 1.1f) {
                // If the text exceeds the width by up to 10%, expand the width by 10%.
                leftSide *= 1.1f
                rightSide *= 1.1f
            } else {
                // If the text exceeds the width significantly, shrink the text.
                leftSide *= 1.1f
                rightSide *= 1.1f
                scaleX = 1.1f * w / textWidth
                textWidth = 1.1f * w
            }
        }

        this.descent = maxDescent.toFloat()
        this.ascent = maxAscent.toFloat()
        this.leftSideOffset = leftSide
        this.rightSideOffset = rightSide
        this.scaleX = scaleX
        this.textWidth = textWidth
    }

    override fun draw(canvas: Canvas, originX: Float, originY: Float, paint: TextPaint) {
        var x = originX + leftSideOffset + (width - textWidth) / 2 // centering
        var y = originY - ascent
        text.forStyleRuns(start, end, paint, HORIZONTAL) {
            rStart,
            rEnd,
            rPaint,
            bgColor,
            fontShear,
            _,
            _ ->
            withTempScaleX(rPaint, scaleX) {
                val w = rPaint.measureText(text, rStart, rEnd)
                val h = descent - ascent

                // Draw a background rectangle if a background color is specified.
                drawBackground(canvas, x, y + ascent, w, h, bgColor)

                if (fontShear == 0f) {
                    canvas.drawText(text, rStart, rEnd, x, y, rPaint)
                } else {
                    canvas.save()
                    try {
                        canvas.translate(x, y)
                        canvas.skew(-fontShear, 0f)
                        canvas.drawText(text, rStart, rEnd, 0f, 0f, rPaint)
                    } finally {
                        canvas.restore()
                    }
                }

                // Advance the draw offset for the next style.
                x += w
            }
        }

        val emphasisSpans = text.getSpans<EmphasisSpan>(start, end)
        if (emphasisSpans.isNotEmpty()) {
            val span = emphasisSpans.last() // The last span wins
            val xOffset = paint.textSize * (1f + span.scale) / 2
            withTempScale(paint, span.scale) {
                applyVerticalFlag(paint, VERTICAL) {
                    val letterWidth = paint.measureText(span.letter)
                    val yOffset = (letterWidth - height) / 2f
                    canvas.drawText(span.letter, originX + xOffset, originY - yOffset, paint)
                }
            }
        }
    }

    override fun getCharAdvances(out: FloatArray, paint: TextPaint) {
        // The line break won't happen inside Tate-chu-yoko span.
        out[0] = height
        if (out.size > 1) {
            out.fill(0f, 1, out.size)
        }
    }
}

/**
 * Represents a layout run that rotates the text by 90 degrees.
 *
 * @param text The text this layout represents.
 * @param start The starting inclusive index of the text.
 * @param end The ending exclusive index of the text.
 * @param paint The paint used for text rendering.
 */
internal class RotateLayoutRun(text: CharSequence, start: Int, end: Int, paint: TextPaint) :
    LayoutRun(text, start, end) {
    override val height: Float
    override val leftSideOffset: Float
    override val rightSideOffset: Float
    private val ascent: Float
    private val descent: Float

    init {
        var height = 0f
        var leftSide = 0f
        var rightSide = 0f
        var ascent = 0f
        var descent = 0f

        val metrics = FontMetrics()
        text.forStyleRuns(start, end, paint, HORIZONTAL) { rStart, rEnd, rPaint, _, _, _, _ ->
            height += rPaint.measureText(text, rStart, rEnd)
            leftSide = min(leftSide, -rPaint.textSize * 0.5f)
            rightSide = max(rightSide, rPaint.textSize * 0.5f)
            rPaint.getFontMetrics(metrics)
            ascent = min(ascent, metrics.ascent)
            descent = max(descent, metrics.descent)
        }

        this.leftSideOffset = leftSide
        this.rightSideOffset = rightSide
        this.height = height
        this.ascent = ascent
        this.descent = descent
    }

    override fun draw(canvas: Canvas, originX: Float, originY: Float, paint: TextPaint) {
        canvas.save()
        try {
            // To horizontal centering the rotated string, adjust the baseline offset.
            canvas.translate(originX + (ascent + descent) * 0.5f, originY)
            canvas.rotate(90f, 0f, 0f)

            var x = 0f
            text.forStyleRuns(start, end, paint, HORIZONTAL) {
                rStart,
                rEnd,
                rPaint,
                bgColor,
                fontShear,
                _,
                _ ->
                val width = rPaint.measureText(text, rStart, rEnd)
                drawBackground(canvas, x, ascent, width, descent - ascent, bgColor)

                if (fontShear == 0f) {
                    canvas.drawText(text, rStart, rEnd, x, 0f, rPaint)
                } else {
                    canvas.save()
                    try {
                        canvas.translate(x, 0f)
                        canvas.skew(-fontShear, 0f)
                        canvas.drawText(text, rStart, rEnd, 0f, 0f, rPaint)
                    } finally {
                        canvas.restore()
                    }
                }

                // TODO: Draw Emphasis for rotated text.

                x += width
            }
        } finally {
            canvas.restore()
        }
    }

    override fun getCharAdvances(out: FloatArray, paint: TextPaint) {
        text.forStyleRuns(start, end, paint, HORIZONTAL) { rStart, rEnd, rPaint, _, _, _, _ ->
            rPaint.getRunCharacterAdvance(
                text,
                rStart,
                rEnd, // target range
                rStart,
                rEnd, // context range
                false, // RTL // TODO: support RTL
                rEnd, // offset,
                out,
                rStart - start,
            )
        }
    }
}

/**
 * Represents an upright text layout run, where text is laid out vertically.
 *
 * @param text The text this layout represents.
 * @param start The starting inclusive index of the text.
 * @param end The ending exclusive index of the text.
 * @param paint The paint used for text rendering.
 */
internal class UprightLayoutRun(text: CharSequence, start: Int, end: Int, paint: TextPaint) :
    LayoutRun(text, start, end) {

    override val height: Float
    override val leftSideOffset: Float
    override val rightSideOffset: Float

    init {
        var height = 0f
        var left = 0f
        var right = 0f

        text.forStyleRuns(start, end, paint, VERTICAL) {
            rStart,
            rEnd,
            rPaint,
            _,
            _,
            _,
            emphasisScale ->
            val emphasisWidth = rPaint.textSize * emphasisScale

            height += rPaint.measureText(text, rStart, rEnd)
            left = min(left, -rPaint.textSize * 0.5f)
            right = max(right, rPaint.textSize * 0.5f + emphasisWidth)
        }
        this.height = height
        this.leftSideOffset = left
        this.rightSideOffset = right
    }

    override fun draw(canvas: Canvas, originX: Float, originY: Float, paint: TextPaint) {
        var y = originY
        text.forStyleRuns(start, end, paint, VERTICAL) {
            rStart,
            rEnd,
            rPaint,
            bgColor,
            fontShear,
            emphasisLetter,
            emphasisScale ->
            if (bgColor != 0) {
                tempPaint { bgWorkPaint ->
                    bgWorkPaint.color = bgColor
                    canvas.drawRect(
                        originX + leftSideOffset,
                        y,
                        originX + rightSideOffset,
                        y + height,
                        bgWorkPaint,
                    )
                }
            }

            if (fontShear == 0f) {
                canvas.drawText(text, rStart, rEnd, originX, y, rPaint)
            } else {
                canvas.save()
                try {
                    canvas.translate(originX, y)
                    canvas.skew(0f, -fontShear)
                    canvas.drawText(text, rStart, rEnd, 0f, 0f, rPaint)
                } finally {
                    canvas.restore()
                }
            }

            if (emphasisLetter != null) {
                var eY = y
                val xOffset = rPaint.textSize * (1f + emphasisScale) / 2

                val letterWidth =
                    withTempScale(rPaint, emphasisScale) { rPaint.measureText(emphasisLetter) }

                text.forEachGrapheme(rStart, rEnd, rPaint.textLocale) { gStart, gEnd ->
                    val positions = FloatArray(gEnd - gStart)
                    rPaint.getRunCharacterAdvance(
                        text,
                        gStart,
                        gEnd,
                        gStart,
                        gEnd,
                        false,
                        gEnd,
                        positions,
                        0,
                    )
                    withTempScale(rPaint, emphasisScale) {
                        positions.forEach {
                            if (it == 0f) {
                                return@forEach // Skip the surrogate pair or combining character.
                            }
                            val yOffset = (letterWidth - it) / 2f
                            if (isEmphasisTarget(Character.codePointAt(text, gStart))) {
                                canvas.drawText(
                                    emphasisLetter,
                                    originX + xOffset,
                                    eY - yOffset,
                                    rPaint,
                                )
                            }
                            eY += it
                        }
                    }
                }
            }

            y += rPaint.measureText(text, rStart, rEnd)
        }
    }

    override fun getCharAdvances(out: FloatArray, paint: TextPaint) {
        text.forStyleRuns(start, end, paint, VERTICAL) { rStart, rEnd, rPaint, _, _, _, _ ->
            rPaint.getRunCharacterAdvance(
                text,
                rStart,
                rEnd, // target range
                rStart,
                rEnd, // context range
                false, // RTL
                rEnd, // offset,
                out,
                rStart - start,
            ) // out array and its index
        }
    }
}

/**
 * Extension function to iterate over style runs within a CharSequence, applying specific styles and
 * vertical orientation.
 *
 * @param start The inclusive start index of the range to process.
 * @param end The exclusive end index of the range to process.
 * @param basePaint The base paint to use for drawing.
 * @param isVertical If true, sets the vertical text flag; otherwise, clears it.
 */
private inline fun CharSequence.forStyleRuns(
    start: Int,
    end: Int,
    basePaint: TextPaint,
    isVertical: Boolean,
    crossinline block: (Int, Int, Paint, Int, Float, String?, Float) -> Unit,
) {
    // Easy case: if the text is a non-styled text, just call back entire text with applying
    // vertical flag.
    if (this !is Spanned) {
        applyVerticalFlag(basePaint, isVertical) {
            block(
                start,
                end,
                it,
                0 /* bgColor */,
                0f /* fontShear */,
                null /* letter */,
                0f, /* scale */
            )
        }
        return
    }

    tempPaint { workPaint ->
        var current = start
        while (current < end) {
            val rEnd = nextSpanTransition(current, end, CharacterStyle::class.java)
            val styles = getSpans(current, rEnd, CharacterStyle::class.java)

            var fontShear = 0f
            var emphasisLetter: String? = null
            var emphasisScale = 0f
            workPaint.set(basePaint)
            styles.forEach {
                it.updateDrawState(workPaint)
                if (it is FontShearSpan) {
                    fontShear = it.fontShear // last wins
                } else if (it is EmphasisSpan) {
                    emphasisLetter = it.letter
                    emphasisScale = it.scale
                }
            }
            applyVerticalFlag(workPaint, isVertical) {
                block(
                    current,
                    rEnd,
                    workPaint,
                    workPaint.bgColor,
                    fontShear,
                    emphasisLetter,
                    emphasisScale,
                )
            }
            current = rEnd
        }
    }
}

/**
 * Applies or removes the vertical text flag from the given Paint.
 *
 * @param paint The paint to modify.
 * @param isVertical True to add the flag, false to remove it.
 * @param block A lambda to execute with the modified paint.
 */
internal inline fun applyVerticalFlag(
    paint: Paint,
    isVertical: Boolean,
    crossinline block: (Paint) -> Unit,
) {
    val originalFlags = paint.flags
    paint.flags =
        if (isVertical) {
            paint.flags or Paint.VERTICAL_TEXT_FLAG
        } else {
            paint.flags and Paint.VERTICAL_TEXT_FLAG.inv()
        }

    try {
        block(paint)
    } finally {
        paint.flags = originalFlags
    }
}

private val paintPool = ThreadLocal<LinkedList<TextPaint>>()

private inline fun tempPaint(crossinline block: (TextPaint) -> Unit) {
    val pool = paintPool.getOrSet { LinkedList<TextPaint>() }
    var paint = if (pool.isNotEmpty()) pool.remove() else TextPaint()
    try {
        block(paint)
    } finally {
        if (pool.size <= 2) { // Pool up to two paints.
            pool.push(paint)
        }
    }
}

/** Executes a block of code with a temporary scaling X of the text size of a given [TextPaint]. */
private inline fun <T : Paint, R> withTempScaleX(
    textPaint: T,
    scaleX: Float,
    crossinline block: () -> R,
): R {
    val originalScaleX = textPaint.textScaleX
    textPaint.textScaleX = scaleX
    try {
        return block()
    } finally {
        textPaint.textScaleX = originalScaleX
    }
}

private inline fun <reified T> CharSequence.getSpans(start: Int, end: Int): Array<T> =
    if (this is Spanned) {
        getSpans(start, end, T::class.java)
    } else {
        emptyArray()
    }

private fun isEmphasisTarget(cp: Int): Boolean {
    val type = UCharacter.getType(cp).toByte()
    if (
        type == UCharacterCategory.CONTROL ||
            type == UCharacterCategory.FORMAT ||
            type == UCharacterCategory.UNASSIGNED ||
            type == UCharacterCategory.LINE_SEPARATOR ||
            type == UCharacterCategory.PARAGRAPH_SEPARATOR ||
            type == UCharacterCategory.SPACE_SEPARATOR ||
            type == UCharacterCategory.CONNECTOR_PUNCTUATION ||
            type == UCharacterCategory.DASH_PUNCTUATION ||
            type == UCharacterCategory.FINAL_PUNCTUATION ||
            type == UCharacterCategory.INITIAL_PUNCTUATION ||
            type == UCharacterCategory.OTHER_PUNCTUATION
    ) {
        return false
    }

    return true
}
