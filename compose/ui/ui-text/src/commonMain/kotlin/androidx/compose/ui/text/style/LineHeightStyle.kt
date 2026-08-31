/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.text.style

import androidx.compose.ui.text.PlatformParagraphStyle
import androidx.compose.ui.text.internal.checkPrecondition
import kotlin.jvm.JvmInline

/**
 * Configures line height behavior, including alignment and trimming of extra space.
 *
 * Applies only when a line height is defined on the text.
 *
 * @param alignment alignment of the line within the allocated line height.
 * @param trim trimming behavior for the top of the first line and bottom of the last line. Requires
 *   [PlatformParagraphStyle.includeFontPadding] to be false.
 * @param mode behavior when the specified line height is smaller than the system default (see
 *   [Mode]).
 */
public class LineHeightStyle(
    public val alignment: Alignment,
    public val trim: Trim,
    public val mode: Mode,
) {

    public constructor(alignment: Alignment, trim: Trim) : this(alignment, trim, Mode.Fixed)

    public companion object {
        /**
         * The default configuration for [LineHeightStyle]:
         * - alignment = [Alignment.Proportional]
         * - trim = [Trim.Both]
         * - mode = [Mode.Fixed]
         */
        public val Default: LineHeightStyle =
            LineHeightStyle(alignment = Alignment.Proportional, trim = Trim.Both, mode = Mode.Fixed)
    }

    /** Returns a copy of this [LineHeightStyle], optionally overriding some of the values. */
    public fun copy(
        alignment: Alignment = this.alignment,
        trim: Trim = this.trim,
        mode: Mode = this.mode,
    ): LineHeightStyle = LineHeightStyle(alignment, trim, mode)

    public override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LineHeightStyle) return false

        if (alignment != other.alignment) return false
        if (trim != other.trim) return false
        if (mode != other.mode) return false

        return true
    }

    public override fun hashCode(): Int {
        var result = alignment.hashCode()
        result = 31 * result + trim.hashCode()
        result = 31 * result + mode.hashCode()
        return result
    }

    public override fun toString(): String {
        return "LineHeightStyle(" + "alignment=$alignment, " + "trim=$trim," + "mode=$mode" + ")"
    }

    /**
     * Controls trimming of extra space from the first line top and last line bottom.
     *
     * Requires [PlatformParagraphStyle.includeFontPadding] to be false to take effect.
     *
     * Trimming behavior depends on the selected [Mode]:
     * - [Mode.Fixed] / [Mode.Minimum]: Trims extra space only when the configured line height is
     *   taller than the font default (prevents clipping).
     * - [Mode.Tight]: Trims space even when the configured line height is shorter than the font
     *   default.
     *
     * **Warning:** Use [Mode.Tight] with caution to avoid cutting off tall characters or accents.
     * Test your text layout with tall scripts (e.g., Arabic "العَرَبِيَّةُ", Tibetan "དབུ་ཅན་", or
     * Burmese "မြန်မာဘာသာ") before using in production.
     */
    @JvmInline
    public value class Trim internal constructor(internal val value: Int) {

        public override fun toString(): String {
            return when (value) {
                FirstLineTop.value -> "LineHeightStyle.Trim.FirstLineTop"
                LastLineBottom.value -> "LineHeightStyle.Trim.LastLineBottom"
                Both.value -> "LineHeightStyle.Trim.Both"
                None.value -> "LineHeightStyle.Trim.None"
                else -> "Invalid"
            }
        }

        public companion object {
            private const val FlagTrimTop = 0x00000001
            private const val FlagTrimBottom = 0x00000010

            /**
             * Trim the space that would be added to the top of the first line as a result of the
             * line height. Single line text is both the first and last line. This feature is
             * available only when [PlatformParagraphStyle.includeFontPadding] is false.
             *
             * For example, when line height is 3.em, and [Alignment] is [Alignment.Center], the
             * first line has 2.em height and the height from first line baseline to second line
             * baseline is still 3.em:
             * <pre>
             * +--------+
             * | Line1  |
             * |        |
             * |--------|
             * |        |
             * | Line2  |
             * |        |
             * +--------+
             * </pre>
             */
            public val FirstLineTop: Trim
                get() = Trim(FlagTrimTop)

            /**
             * Trim the space that would be added to the bottom of the last line as a result of the
             * line height. Single line text is both the first and last line. This feature is
             * available only when [PlatformParagraphStyle.includeFontPadding] is false.
             *
             * For example, when line height is 3.em, and [Alignment] is [Alignment.Center], the
             * last line has 2.em height and the height from first line baseline to second line
             * baseline is still 3.em:
             * <pre>
             * +--------+
             * |        |
             * | Line1  |
             * |        |
             * |--------|
             * |        |
             * | Line2  |
             * +--------+
             * </pre>
             */
            public val LastLineBottom: Trim
                get() = Trim(FlagTrimBottom)

            /**
             * Trim the space that would be added to the top of the first line and bottom of the
             * last line as a result of the line height. This feature is available only when
             * [PlatformParagraphStyle.includeFontPadding] is false.
             *
             * For example, when line height is 3.em, and [Alignment] is [Alignment.Center], the
             * first and last line has 2.em height and the height from first line baseline to second
             * line baseline is still 3.em:
             * <pre>
             * +--------+
             * | Line1  |
             * |        |
             * |--------|
             * |        |
             * | Line2  |
             * +--------+
             * </pre>
             */
            public val Both: Trim
                get() = Trim(FlagTrimTop or FlagTrimBottom)

            /**
             * Do not trim first line top or last line bottom.
             *
             * For example, when line height is 3.em, and [Alignment] is [Alignment.Center], the
             * first line height, last line height and the height from first line baseline to second
             * line baseline are 3.em:
             * <pre>
             * +--------+
             * |        |
             * | Line1  |
             * |        |
             * |--------|
             * |        |
             * | Line2  |
             * |        |
             * +--------+
             * </pre>
             */
            public val None: Trim
                get() = Trim(0)
        }

        /**
         * Returns true if this [Trim] configuration trims the space at the top of the first line.
         */
        public val trimsFirstLineTop: Boolean
            get() {
                return value and FlagTrimTop != 0
            }

        /**
         * Returns true if this [Trim] configuration trims the space at the bottom of the last line.
         */
        public val trimsLastLineBottom: Boolean
            get() {
                return value and FlagTrimBottom != 0
            }
    }

    /**
     * Aligns the line within the space provided by the line height.
     *
     * @property topRatio the alignment ratio of the text. 0f aligns to top, 0.5f to center, 1f to
     *   bottom, and -1f aligns proportionally based on font metrics (ascent/descent ratio).
     */
    @JvmInline
    public value class Alignment(public val topRatio: Float) {

        init {
            checkPrecondition(topRatio in 0f..1f || topRatio == -1f) {
                "topRatio should be in [0..1] range or -1"
            }
        }

        public override fun toString(): String {
            return when (topRatio) {
                Top.topRatio -> "LineHeightStyle.Alignment.Top"
                Center.topRatio -> "LineHeightStyle.Alignment.Center"
                Proportional.topRatio -> "LineHeightStyle.Alignment.Proportional"
                Bottom.topRatio -> "LineHeightStyle.Alignment.Bottom"
                else -> "LineHeightStyle.Alignment(topPercentage = $topRatio)"
            }
        }

        public companion object {
            /**
             * Align the line to the top of the space reserved for that line. This means that all
             * extra space as a result of line height is applied to the bottom of the line. When the
             * provided line height value is smaller than the actual line height, the line will
             * still be aligned to the top, therefore the required difference will be subtracted
             * from the bottom of the line.
             *
             * For example, when line height is 3.em, the lines are aligned to the top of 3.em
             * height:
             * <pre>
             * +--------+
             * | Line1  |
             * |        |
             * |        |
             * |--------|
             * | Line2  |
             * |        |
             * |        |
             * +--------+
             * </pre>
             */
            public val Top: Alignment
                get() = Alignment(topRatio = 0f)

            /**
             * Align the line to the center of the space reserved for the line. This configuration
             * distributes additional space evenly between top and bottom of the line.
             *
             * For example, when line height is 3.em, the lines are aligned to the center of 3.em
             * height:
             * <pre>
             * +--------+
             * |        |
             * | Line1  |
             * |        |
             * |--------|
             * |        |
             * | Line2  |
             * |        |
             * +--------+
             * </pre>
             */
            public val Center: Alignment
                get() = Alignment(topRatio = 0.5f)

            /**
             * Align the line proportional to the ascent and descent values of the line. For example
             * if ascent is 8 units of length, and descent is 2 units; an additional space of 10
             * units will be distributed as 8 units to top, and 2 units to the bottom of the line.
             * This is the default behavior.
             */
            public val Proportional: Alignment
                get() = Alignment(topRatio = -1f)

            /**
             * Align the line to the bottom of the space reserved for that line. This means that all
             * extra space as a result of line height is applied to the top of the line. When the
             * provided line height value is smaller than the actual line height, the line will
             * still be aligned to the bottom, therefore the required difference will be subtracted
             * from the top of the line.
             *
             * For example, when line height is 3.em, the lines are aligned to the bottom of 3.em
             * height:
             * <pre>
             * +--------+
             * |        |
             * |        |
             * | Line1  |
             * |--------|
             * |        |
             * |        |
             * | Line2  |
             * +--------+
             * </pre>
             */
            public val Bottom: Alignment
                get() = Alignment(topRatio = 1f)
        }
    }

    /**
     * Determines how line height is calculated across lines in a paragraph.
     *
     * [Fixed], [Minimum], and [Tight] measure font metrics on the **first line only** and apply
     * that single calculated height to every line in the paragraph. If subsequent lines are taller
     * than the first line (such as lines with larger font sizes or taller scripts), they will still
     * use the first line's height and may clip.
     *
     * Text with mixed font sizes or mixed scripts should use [PerLine], which measures each line
     * independently.
     */
    @JvmInline
    public value class Mode internal constructor(internal val value: Int) {

        public override fun toString(): String {
            return when (this) {
                Fixed -> "LineHeightStyle.Mode.Fixed"
                Minimum -> "LineHeightStyle.Mode.Minimum"
                Tight -> "LineHeightStyle.Mode.Tight"
                PerLine -> "LineHeightStyle.Mode.PerLine"
                else -> "Invalid"
            }
        }

        public companion object {
            /**
             * Sets the same line height for each line, calculated from the font metrics of the
             * **first line only**.
             *
             * If the requested line height is smaller than the first line's font metrics, adds
             * padding outside the first line top and last line bottom to prevent clipping at the
             * paragraph boundaries. Middle lines are not padded and will clip if they overflow.
             *
             * If later lines are taller than the first line, they do not expand and may clip.
             *
             * This is the default mode in [LineHeightStyle.Default].
             */
            public val Fixed: Mode
                get() = Mode(0)

            /**
             * Sets the same line height for each line, calculated from the font metrics of the
             * **first line only**.
             *
             * If the requested line height is smaller than the first line's font metrics, uses the
             * first line's natural height for all lines instead of shrinking them. If the requested
             * line height is larger, behaves the same as [Fixed].
             *
             * If later lines are taller than the first line, they do not expand and may clip.
             */
            public val Minimum: Mode
                get() = Mode(1)

            /**
             * Sets the requested line height on every line based on the **first line only**,
             * without adding top or bottom padding.
             *
             * If the requested line height is smaller than the font metrics of any line, that line
             * will clip.
             */
            public val Tight: Mode
                get() = Mode(2)

            /**
             * Calculates line height independently for each line.
             *
             * Unlike [Fixed], [Minimum], and [Tight] (which only measure the first line), [PerLine]
             * measures every line. If a line's natural font height exceeds the requested line
             * height, that line expands to its natural height.
             *
             * Use [PerLine] when text contains mixed font sizes or mixed scripts so that tall lines
             * do not clip and short lines do not leave extra gaps:
             * ```
             * Mode.Fixed / Mode.Minimum (Only measures Line 1; Line 2 may clip):
             * +----------------------------------------------+
             * | Line 1 (Small font, measures line height)    |
             * +----------------------------------------------+
             * | Line 2 (TALL SCRIPT - clips or overlaps!)    |
             * +----------------------------------------------+
             * | Line 3 (Small font)                          |
             * +----------------------------------------------+
             *
             * Mode.PerLine (Measures each line independently):
             * +----------------------------------------------+
             * | Line 1 (Small font, compact line height)     |
             * +----------------------------------------------+
             * | Line 2 (TALL SCRIPT - expands to fit)        |
             * +----------------------------------------------+
             * | Line 3 (Small font, compact line height)     |
             * +----------------------------------------------+
             * ```
             *
             * @sample androidx.compose.ui.text.samples.LineHeightStylePerLineSample
             */
            public val PerLine: Mode
                get() = Mode(3)
        }
    }
}
