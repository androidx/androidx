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

import android.graphics.Paint
import android.os.Build
import android.text.TextUtils

/** Backport of [Paint.getFontMetricsInt] for vertical text. */
internal fun Paint.getFontMetricsIntCompat(
    cs: CharSequence,
    start: Int,
    count: Int,
    contextStart: Int,
    contextCount: Int,
    isRtl: Boolean,
    out: Paint.FontMetricsInt,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        getFontMetricsInt(cs, start, count, contextStart, contextCount, isRtl, out)
    } else {
        // Fallback to horizontal metrics for older APIs.
        getFontMetricsInt(out)
    }
}

/**
 * Backport of [Paint.getRunCharacterAdvance].
 *
 * If the API level is less than [Build.VERSION_CODES.UPSIDE_DOWN_CAKE], this function uses
 * [Paint.getTextRunAdvances] or [Paint.getTextWidths] for the advances. These two functions do not
 * give the advance of a prefix of the run. As a result, this function adds the values in [out].
 *
 * If the API level is less than [Build.VERSION_CODES.Q], this function measures each character
 * alone. As a result, it ignores [contextStart], [contextEnd] and [isRtl]. It does not apply the
 * shaping that depends on the adjacent text or on the paragraph direction.
 */
internal fun Paint.getRunCharacterAdvanceCompat(
    text: CharSequence,
    start: Int,
    end: Int,
    contextStart: Int,
    contextEnd: Int,
    isRtl: Boolean,
    offset: Int,
    out: FloatArray,
    outOffset: Int,
): Float {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        return getRunCharacterAdvance(
            text,
            start,
            end,
            contextStart,
            contextEnd,
            isRtl,
            offset,
            out,
            outOffset,
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val tmp = CharArray(contextEnd - contextStart)
        TextUtils.getChars(text, contextStart, contextEnd, tmp, 0)
        getTextRunAdvances(
            tmp,
            start - contextStart,
            end - start,
            0,
            contextEnd - contextStart,
            isRtl,
            out,
            outOffset,
        )
        return out.sumAdvances(outOffset, offset - start)
    } else {
        if (outOffset == 0) {
            getTextWidths(text, start, end, out)
        } else {
            val tmp = FloatArray(end - start)
            getTextWidths(text, start, end, tmp)
            System.arraycopy(tmp, 0, out, outOffset, end - start)
        }
        return out.sumAdvances(outOffset, offset - start)
    }
}

/** Adds [count] advances from [this]. The first advance has the index [from]. */
private fun FloatArray.sumAdvances(from: Int, count: Int): Float {
    var total = 0f
    for (i in 0 until count) {
        total += this[from + i]
    }
    return total
}

internal fun Paint.measureTextVertical(text: CharSequence): Float =
    measureTextVertical(text, 0, text.length)

/** Measures the height of text in vertical writing. */
internal fun Paint.measureTextVertical(text: CharSequence, start: Int, end: Int): Float =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
        withVerticalFlag { measureText(text, start, end) }
    } else {
        // The backport assumes that the advance of each character is 1em.
        // This assumption is correct for most CJK characters and for Upright mode.
        val widths = FloatArray(end - start)
        getTextWidths(text, start, end, widths)
        val nonZeroCount = widths.count { it > 0f }
        textSize * nonZeroCount
    }

/** Measures vertical character advances. */
internal fun Paint.getRunCharacterAdvanceVertical(
    text: CharSequence,
    start: Int,
    end: Int,
    contextStart: Int,
    contextEnd: Int,
    isRtl: Boolean,
    offset: Int,
    out: FloatArray,
    outOffset: Int,
): Float =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
        withVerticalFlag {
            getRunCharacterAdvance(
                text,
                start,
                end,
                contextStart,
                contextEnd,
                isRtl,
                offset,
                out,
                outOffset,
            )
        }
    } else {
        // The backport assumes that the advance of each character is 1em.
        // This branch must also fill the `out` array.
        val count = end - start
        val widths = FloatArray(count)
        getTextWidths(text, start, end, widths)
        var totalAdvance = 0f
        for (i in 0 until count) {
            if (widths[i] > 0f) {
                out[outOffset + i] = textSize
                totalAdvance += textSize
            } else {
                out[outOffset + i] = 0f
            }
        }
        totalAdvance
    }
