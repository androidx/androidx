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

package androidx.compose.foundation.text.input

import androidx.compose.foundation.text.input.internal.TextStyleBuffer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle

/**
 * Provides access to the styles applied to the text within a [TextFieldState].
 *
 * This interface provides a style query API similar to [TextFieldBuffer], but returns immutable
 * data.
 *
 * Use this interface when you only need to read the current text styles (e.g., to update a
 * formatting toolbar UI). If you need to mutate the text styles, use [TextFieldState.edit] and the
 * corresponding methods on [TextFieldBuffer].
 *
 * Do not use this interface to query styles within a [TextFieldBuffer] edit block. The data
 * returned will not reflect any ongoing changes made to the buffer, as it only returns the
 * unupdated state from before the edit block began.
 *
 * @sample androidx.compose.foundation.samples.BasicTextFieldTrackedRangeToggleBoldSample
 * @see TextFieldBuffer
 * @see TextFieldState.textStyles
 */
interface TextFieldTextStyles {
    /**
     * Returns a list of [AnnotatedString.Range]s representing the [SpanStyle]s that intersect with
     * the given range defined by [start] (inclusive) and [end] (exclusive).
     *
     * Styles are returned in the same order they were originally added to the buffer.
     *
     * A style intersects with the range if it overlaps with it at any point. For non-empty ranges,
     * this means `style.start < end` and `start < style.end`.
     *
     * Example Query Range: `[5, 15)`
     *
     * ```
     * 0    5    10   15   20   25
     * |----|----|----|----|----|
     *      [---------)              Query Range [5, 15)
     *
     * [-------------------)         Style [0, 20) (Contains query) -> Returned
     *           [----)              Style [8, 12) (Inside query)   -> Returned
     * [----------)                  Style [0, 10) (Overlap start)  -> Returned
     * [----)                        Style [0, 5)  (Touching start) -> NOT Returned
     *                [----------)   Style [15, 25)(Touching end)   -> NOT Returned
     * ```
     *
     * Example Collapsed Query: `[10, 10)`
     *
     * ```
     * 0    5    10   15   20   25
     * |----|----|----|----|----|
     *           |                   Query Range [10, 10)
     *
     * [-------------------)         Style [0, 20) (Contains query) -> Returned
     * [---------)                   Style [0, 10) (Touching end)   -> NOT Returned
     *           [----------)        Style [10, 20)(Touching start) -> Returned
     * ```
     *
     * @param start The start index of the range to query, inclusive.
     * @param end The end index of the range to query, exclusive.
     * @return A list of [AnnotatedString.Range]s representing the [SpanStyle]s overlapping with the
     *   queried range.
     */
    fun getSpanStyles(start: Int, end: Int): List<AnnotatedString.Range<SpanStyle>>

    /**
     * Returns a list of [AnnotatedString.Range]s representing the [ParagraphStyle]s that intersect
     * with the given range defined by [start] (inclusive) and [end] (exclusive).
     *
     * Styles are returned in the same order they were originally added to the buffer.
     *
     * A style intersects with the range if it overlaps with it at any point. For non-empty ranges,
     * this means `style.start < end` and `start < style.end`.
     *
     * Example Query Range: `[5, 15)`
     *
     * ```
     * 0    5    10   15   20   25
     * |----|----|----|----|----|
     *      [---------)              Query Range [5, 15)
     *
     * [-------------------)         Style [0, 20) (Contains query) -> Returned
     *           [----)              Style [8, 12) (Inside query)   -> Returned
     * [----------)                  Style [0, 10) (Overlap start)  -> Returned
     * [----)                        Style [0, 5)  (Touching start) -> NOT Returned
     *                [----------)   Style [15, 25)(Touching end)   -> NOT Returned
     * ```
     *
     * Example Collapsed Query: `[10, 10)`
     *
     * ```
     * 0    5    10   15   20   25
     * |----|----|----|----|----|
     *           |                   Query Range [10, 10)
     *
     * [-------------------)         Style [0, 20) (Contains query) -> Returned
     * [---------)                   Style [0, 10) (Touching end)   -> NOT Returned
     *           [----------)        Style [10, 20)(Touching start) -> Returned
     * ```
     *
     * @param start The start index of the range to query, inclusive.
     * @param end The end index of the range to query, exclusive.
     * @return A list of [AnnotatedString.Range]s representing the [ParagraphStyle]s overlapping
     *   with the queried range.
     */
    fun getParagraphStyles(start: Int, end: Int): List<AnnotatedString.Range<ParagraphStyle>>
}

internal class TextFieldTextStylesImpl(
    internal val textStyleBuffer: TextStyleBuffer<AnnotatedString.Annotation>,
    private val length: Int,
) : TextFieldTextStyles {
    override fun getSpanStyles(start: Int, end: Int): List<AnnotatedString.Range<SpanStyle>> {
        validateRange(start, end, length)
        return textStyleBuffer.getImmutableStyles(start, end)
    }

    override fun getParagraphStyles(
        start: Int,
        end: Int,
    ): List<AnnotatedString.Range<ParagraphStyle>> {
        validateRange(start, end, length)
        return textStyleBuffer.getImmutableStyles(start, end)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TextFieldTextStylesImpl) return false
        if (length != other.length) return false
        if (textStyleBuffer != other.textStyleBuffer) return false
        return true
    }

    override fun hashCode(): Int = 31 * textStyleBuffer.hashCode() + length
}

internal object EmptyTextFieldTextStyles : TextFieldTextStyles {
    override fun getSpanStyles(start: Int, end: Int): List<AnnotatedString.Range<SpanStyle>> =
        emptyList()

    override fun getParagraphStyles(
        start: Int,
        end: Int,
    ): List<AnnotatedString.Range<ParagraphStyle>> = emptyList()
}

private fun validateRange(start: Int, end: Int, length: Int) {
    require(end in start..length && start >= 0) {
        "Expected start to be at least 0, and end to be at least start and no greater than " +
            "the text length (length=$length, start=$start, end=$end)"
    }
}
