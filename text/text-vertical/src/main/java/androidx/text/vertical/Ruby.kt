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
import android.text.Spanned
import android.text.TextPaint
import kotlin.math.max

/**
 * A span used to specify ruby text for a portion of the text.
 *
 * Ruby text cannot be nested (i.e., ruby text cannot contain further ruby text). Ruby spans also
 * cannot overlap each other.
 *
 * This span is designed for use with [VerticalTextLayout].
 *
 * @property text The ruby text to be displayed adjacent to the base text.
 * @property orientation The text orientation of the ruby text. Defaults to [TextOrientation.MIXED].
 * @property textScale The text scale ratio of the ruby text relative to the base text. Defaults to
 *   0.5f.
 */
public class RubySpan
private constructor(
    public val text: CharSequence,
    @OrientationMode public val orientation: Int,
    public val textScale: Float,
) {
    /**
     * Builder class for creating [RubySpan] instances.
     *
     * @param text The ruby text to be displayed adjacent to the base text.
     */
    public class Builder(private val text: CharSequence) {
        private var _orientation: Int = TextOrientation.MIXED
        private var _textScale: Float = 0.5f

        /**
         * Sets the text orientation for the ruby text.
         *
         * By default, [TextOrientation.MIXED] is used.
         *
         * @param orientation The text orientation to set.
         * @return This [Builder] instance for method chaining.
         */
        public fun setOrientation(@OrientationMode orientation: Int): Builder = apply {
            _orientation = orientation
        }

        /**
         * Sets the text scale for the ruby text.
         *
         * By default, 0.5f is used, meaning the ruby text will be half the size of the base text.
         *
         * @param textScale The text scale to set.
         * @return This [Builder] instance for method chaining.
         */
        public fun setTextScale(textScale: Float): Builder = apply { _textScale = textScale }

        /**
         * Builds and returns a new [RubySpan] instance.
         *
         * @return A new [RubySpan] instance.
         */
        public fun build(): RubySpan = RubySpan(text, _orientation, _textScale)
    }
}

/**
 * Iterates through each RubySpan within a specified range of a CharSequence.
 *
 * @param text The CharSequence
 * @param start The inclusive starting index
 * @param end The exclusive ending index
 * @param consumer A callback function that is called for each RubySpan transition. It receives
 *   three parameters:
 *     - The inclusive start index of the RubySpan.
 *     - The exclusive end index of the RubySpan.
 *     - The `RubySpan` object itself, or `null` if no RubySpan is found.
 */
internal inline fun forEachRubySpanTransition(
    text: CharSequence,
    start: Int,
    end: Int,
    crossinline consumer: (Int, Int, RubySpan?) -> Unit,
) =
    forEachSpan(text, start, end) { rStart, rEnd, rubySpans ->
        require(rubySpans.size <= 1) { "RubySpan cannot be overlapped" }
        consumer(rStart, rEnd, rubySpans.getOrNull(0))
    }

/**
 * A special LayoutRun specialized for a Ruby text.
 *
 * @param text The text this layout represents.
 * @param start The starting inclusive index of the text.
 * @param end The ending exclusive index of the text.
 * @param textOrientation The text orientation mode.
 * @param paint The paint used for text rendering.
 * @param rubySpan The rubySpan attached to the range.
 */
internal class RubyLayoutRun(
    text: CharSequence,
    start: Int,
    end: Int,
    @OrientationMode textOrientation: Int,
    paint: TextPaint,
    rubySpan: RubySpan,
) : LayoutRun(text, start, end) {

    init {
        val rubyText = rubySpan.text
        if (rubyText is Spanned) {
            require(rubyText.getSpans(0, rubyText.length, RubySpan::class.java).isEmpty()) {
                "Ruby text cannot have RubySpan. (Ruby cannot be nested.)"
            }
        }
    }

    override val height: Float
        get() = max(bodyLayoutRuns.height, rubyLayoutRuns.height)

    override val leftSideOffset: Float
        get() = bodyLayoutRuns.leftSide

    override val rightSideOffset: Float
        get() = bodyLayoutRuns.rightSide + rubyLayoutRuns.width

    private val rubyScale = rubySpan.textScale
    private val rubyLayoutRuns: LineLayout =
        withTempScale(paint, rubyScale) {
            createLineLayout(rubySpan.text, 0, rubySpan.text.length, paint, rubySpan.orientation)
        }
    private val bodyLayoutRuns: LineLayout =
        createLineLayout(text, start, end, paint, textOrientation)

    override fun draw(canvas: Canvas, originX: Float, originY: Float, paint: TextPaint) {
        val bodyHeight = bodyLayoutRuns.height
        val rubyHeight = rubyLayoutRuns.height

        // Vertical centering the body text and ruby text.
        var bodyY = originY
        var rubyY = originY
        val heightDiffHalf = (bodyHeight - rubyHeight) / 2
        if (heightDiffHalf > 0) {
            // The body text is taller than the ruby text. Push the ruby text for centering.
            rubyY += heightDiffHalf
        } else {
            // The body text is shorter than the ruby text. Push the body text for centering.
            bodyY -= heightDiffHalf
        }

        bodyLayoutRuns.draw(canvas, originX, bodyY, paint)

        val rubyX = originX + bodyLayoutRuns.rightSide - rubyLayoutRuns.leftSide
        withTempScale(paint, rubyScale) { rubyLayoutRuns.draw(canvas, rubyX, rubyY, paint) }
    }

    override fun getCharAdvances(out: FloatArray, paint: TextPaint) {
        // We don't support line break inside Ruby. Just assigning all height into the first
        // character for preventing line break.
        out[0] = height
        if (out.size > 1) {
            out.fill(0f, 1, out.size)
        }
    }
}
