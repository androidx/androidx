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
import androidx.compose.ui.text.TextRange

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
     * the given [range].
     *
     * Styles are returned in the same order they were originally added to the buffer.
     *
     * A style intersects with the range if it overlaps with it at any point. For non-empty ranges,
     * this means `style.start < range.max` and `range.min < style.end`.
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
     * @param range The range to query.
     * @return A list of [AnnotatedString.Range]s representing the [SpanStyle]s overlapping with the
     *   queried range.
     */
    fun getSpanStyles(range: TextRange): List<AnnotatedString.Range<SpanStyle>>

    /**
     * Returns a list of [AnnotatedString.Range]s representing the [ParagraphStyle]s that intersect
     * with the given [range].
     *
     * Styles are returned in the same order they were originally added to the buffer.
     *
     * A style intersects with the range if it overlaps with it at any point. For non-empty ranges,
     * this means `style.start < range.max` and `range.min < style.end`.
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
     * @param range The range to query.
     * @return A list of [AnnotatedString.Range]s representing the [ParagraphStyle]s overlapping
     *   with the queried range.
     */
    fun getParagraphStyles(range: TextRange): List<AnnotatedString.Range<ParagraphStyle>>
}

internal class TextFieldTextStylesImpl(
    internal val textStyleBuffer: TextStyleBuffer<AnnotatedString.Annotation>,
    private val length: Int,
) : TextFieldTextStyles {
    override fun getSpanStyles(range: TextRange): List<AnnotatedString.Range<SpanStyle>> {
        val start = range.min.coerceIn(0, length)
        val end = range.max.coerceIn(0, length)
        return textStyleBuffer.getImmutableStyles(start, end)
    }

    override fun getParagraphStyles(range: TextRange): List<AnnotatedString.Range<ParagraphStyle>> {
        val start = range.min.coerceIn(0, length)
        val end = range.max.coerceIn(0, length)
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
    override fun getSpanStyles(range: TextRange): List<AnnotatedString.Range<SpanStyle>> =
        emptyList()

    override fun getParagraphStyles(range: TextRange): List<AnnotatedString.Range<ParagraphStyle>> =
        emptyList()
}
