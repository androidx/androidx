/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.text.android

import android.text.BoringLayout
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.MetricAffectingSpan
import androidx.compose.ui.text.android.style.LetterSpacingSpanEm
import androidx.compose.ui.text.android.style.LetterSpacingSpanPx
import java.text.BreakIterator
import java.util.PriorityQueue
import kotlin.math.ceil

/**
 * Flag for applying the fix for [b/346918500](https://issuetracker.google.com/346918500).
 *
 * If true, this will allocate a new [SpannableString] if there are spans that must be removed
 * before measuring any intrinsic width.
 */
@Suppress("MayBeConstant") // Don't inline so folks can R8 assumevalues it
private val stripNonMetricAffectingCharSpans: Boolean = true

/** Computes and caches the text layout intrinsic values such as min/max width. */
internal class LayoutIntrinsics(
    private val charSequence: CharSequence,
    private val textPaint: TextPaint,
    @LayoutCompat.TextDirection private val textDirectionHeuristic: Int
) {

    private var _maxIntrinsicWidth: Float = Float.NaN
    private var _minIntrinsicWidth: Float = Float.NaN
    private var _boringMetrics: BoringLayout.Metrics? = null
    private var boringMetricsIsInit: Boolean = false

    private var _charSequenceForIntrinsicWidth: CharSequence? = null
    private val charSequenceForIntrinsicWidth: CharSequence
        get() =
            if (_charSequenceForIntrinsicWidth == null) {
                if (stripNonMetricAffectingCharSpans) {
                    stripNonMetricAffectingCharacterStyleSpans(charSequence).also {
                        _charSequenceForIntrinsicWidth = it
                    }
                } else {
                    charSequence
                }
            } else {
                _charSequenceForIntrinsicWidth!!
            }

    /**
     * Compute Android platform BoringLayout metrics. A null value means the provided CharSequence
     * cannot be laid out using a BoringLayout.
     */
    val boringMetrics: BoringLayout.Metrics?
        get() {
            if (!boringMetricsIsInit) {
                val frameworkTextDir = getTextDirectionHeuristic(textDirectionHeuristic)
                _boringMetrics =
                    BoringLayoutFactory.measure(charSequence, textPaint, frameworkTextDir)
                boringMetricsIsInit = true
            }
            return _boringMetrics
        }

    /**
     * Calculate minimum intrinsic width of the CharSequence.
     *
     * @see androidx.compose.ui.text.android.minIntrinsicWidth
     */
    val minIntrinsicWidth: Float
        get() =
            if (!_minIntrinsicWidth.isNaN()) {
                _minIntrinsicWidth
            } else {
                _minIntrinsicWidth = computeMinIntrinsicWidth()
                _minIntrinsicWidth
            }

    /**
     * Returns the word with the longest length. To calculate it in a performant way, it applies a
     * heuristics where
     * - it first finds a set of words with the longest length
     * - finds the word with maximum width in that set
     */
    private fun computeMinIntrinsicWidth(): Float {
        val iterator = BreakIterator.getLineInstance(textPaint.textLocale)
        iterator.text = CharSequenceCharacterIterator(charSequence, 0, charSequence.length)

        // 10 is just a random number that limits the size of the candidate list
        val heapSize = 10
        // min heap that will hold [heapSize] many words with max length
        val longestWordCandidates =
            PriorityQueue(
                heapSize,
                Comparator<Pair<Int, Int>> { left, right ->
                    (left.second - left.first) - (right.second - right.first)
                }
            )

        var start = 0
        var end = iterator.next()
        while (end != BreakIterator.DONE) {
            if (longestWordCandidates.size < heapSize) {
                longestWordCandidates.add(Pair(start, end))
            } else {
                longestWordCandidates.peek()?.let { minPair ->
                    if ((minPair.second - minPair.first) < (end - start)) {
                        longestWordCandidates.poll()
                        longestWordCandidates.add(Pair(start, end))
                    }
                }
            }

            start = end
            end = iterator.next()
        }

        return if (longestWordCandidates.isEmpty()) 0f
        else longestWordCandidates.maxOf { (start, end) -> getDesiredWidth(start, end) }
    }

    /**
     * Calculate maximum intrinsic width for the CharSequence. Maximum intrinsic width is the width
     * of text where no soft line breaks are applied.
     */
    val maxIntrinsicWidth: Float
        get() =
            if (!_maxIntrinsicWidth.isNaN()) {
                _maxIntrinsicWidth
            } else {
                _maxIntrinsicWidth = computeMaxIntrinsicWidth()
                _maxIntrinsicWidth
            }

    private fun computeMaxIntrinsicWidth(): Float {
        var desiredWidth = (boringMetrics?.width ?: -1).toFloat()

        // boring metrics doesn't cover RTL text so we fallback to different calculation
        // when boring metrics can't be calculated
        if (desiredWidth < 0) {
            // b/233856978, apply `ceil` function here to be consistent with the boring
            // metrics width calculation that does it under the hood, too
            desiredWidth = ceil(getDesiredWidth())
        }

        if (shouldIncreaseMaxIntrinsic(desiredWidth, charSequence, textPaint)) {
            // b/173574230, increase maxIntrinsicWidth, so that StaticLayout won't form 2
            // lines for the given maxIntrinsicWidth
            desiredWidth += 0.5f
        }
        return desiredWidth
    }

    private fun getDesiredWidth(
        start: Int = 0,
        end: Int = charSequenceForIntrinsicWidth.length
    ): Float = Layout.getDesiredWidth(charSequenceForIntrinsicWidth, start, end, textPaint)
}

/**
 * See [b/346918500#comment7](https://issuetracker.google.com/346918500#comment7).
 *
 * Remove all character styling spans for measuring intrinsic width. [CharacterStyle] spans may
 * affect the intrinsic width, even though they aren't supposed to, resulting in a width that
 * doesn't actually fit the text. This can cause the line to unexpectedly wrap, even if `maxLines`
 * was set to `1` or `softWrap` was `false`.
 *
 * [MetricAffectingSpan] extends [CharacterStyle], but [MetricAffectingSpan]s are allowed to affect
 * the width, so only remove spans that **do** extend [CharacterStyle] but **don't** extend
 * [MetricAffectingSpan].
 */
private fun stripNonMetricAffectingCharacterStyleSpans(charSequence: CharSequence): CharSequence {
    if (charSequence !is Spanned || !charSequence.hasSpan(CharacterStyle::class.java)) {
        return charSequence
    }

    val spans = charSequence.getSpans(0, charSequence.length, CharacterStyle::class.java)
    if (spans.isNullOrEmpty()) return charSequence

    // Don't allocate a new SpannableString unless we are certain we will be modifying it.
    var spannableString: SpannableString? = null
    for (span in spans) {
        if (span is MetricAffectingSpan) continue
        if (spannableString == null) spannableString = SpannableString(charSequence)
        spannableString.removeSpan(span)
    }
    return spannableString ?: charSequence
}

/**
 * b/173574230 on Android 11 and above, creating a StaticLayout when
 * - desiredWidth is an Integer,
 * - letterSpacing is set
 * - lineHeight is set StaticLayout forms 2 lines for the given desiredWidth.
 *
 * This function checks if those conditions are met.
 */
private fun shouldIncreaseMaxIntrinsic(
    desiredWidth: Float,
    charSequence: CharSequence,
    textPaint: TextPaint
): Boolean {
    return desiredWidth != 0f &&
        (charSequence is Spanned &&
            (charSequence.hasSpan(LetterSpacingSpanPx::class.java) ||
                charSequence.hasSpan(LetterSpacingSpanEm::class.java)) ||
            textPaint.letterSpacing != 0f)
}
