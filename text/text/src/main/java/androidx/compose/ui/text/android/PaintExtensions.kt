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

package androidx.compose.ui.text.android

import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.text.Spanned
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import kotlin.math.max
import kotlin.math.min

internal fun TextPaint.getCharSequenceBounds(
    text: CharSequence,
    startInclusive: Int,
    endExclusive: Int
): Rect {
    val metricSpanClass = MetricAffectingSpan::class.java
    if (text !is Spanned || !text.hasSpan(metricSpanClass, startInclusive, endExclusive)) {
        return getStringBounds(text, startInclusive, endExclusive)
    }

    val finalRect = Rect()
    val tmpRect = Rect()
    val tmpPaint = TextPaint()
    var tmpStart = startInclusive
    while (tmpStart < endExclusive) {
        val tmpEnd = text.nextSpanTransition(tmpStart, endExclusive, metricSpanClass)
        val spans = text.getSpans(tmpStart, tmpEnd, metricSpanClass)
        tmpPaint.set(this)
        for (span in spans) {
            val spanStart = text.getSpanStart(span)
            val spanEnd = text.getSpanEnd(span)
            // TODO handle replacement
            // skip 0 length spans
            if (spanStart != spanEnd) {
                span.updateMeasureState(tmpPaint)
            }
        }

        tmpPaint.fillStringBounds(text, tmpStart, tmpEnd, tmpRect)
        finalRect.extendWith(tmpRect)
        tmpStart = tmpEnd
    }

    return finalRect
}

/**
 * For use only in getCharSequenceBounds.
 *
 * Afaik the coordinate space of StaticLayout always start from 0,0, regardless of
 * LTR, RTL or BiDi.
 *
 * - Every string is calculated on its own, and their coordinate space will start from 0,0
 *   - assume the rect horizontal coordinates are [0, 20], [0, 40]
 *     - this should return [0, 20 + 40] = [0, 60]
 *     - left stays the same, right adds the *width*.
 *   - assume the rect vertical coordinates are [10, 20], [0, 30]
 *     - this should return [0, 30].
 *     - the min of top (0), and max of bottom (30).
 */
private fun Rect.extendWith(rect: Rect) {
    // this.left stays the same
    this.right += rect.width()
    this.top = min(this.top, rect.top)
    this.bottom = max(this.bottom, rect.bottom)
}

@VisibleForTesting
internal fun Paint.getStringBounds(text: CharSequence, start: Int, end: Int): Rect {
    val rect = Rect()
    fillStringBounds(text, start, end, rect)
    return rect
}

private fun Paint.fillStringBounds(text: CharSequence, start: Int, end: Int, rect: Rect) {
    if (Build.VERSION.SDK_INT >= 29) {
        Paint29.getTextBounds(this, text, start, end, rect)
    } else {
        this.getTextBounds(text.toString(), start, end, rect)
    }
}

@RequiresApi(29)
private object Paint29 {
    @JvmStatic
    @DoNotInline
    fun getTextBounds(paint: Paint, text: CharSequence, start: Int, end: Int, rect: Rect) {
        paint.getTextBounds(text, start, end, rect)
    }
}