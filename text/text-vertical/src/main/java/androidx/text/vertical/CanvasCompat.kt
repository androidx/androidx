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
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.util.LruCache

/** A cache key for font metrics. */
private data class MetricCacheKey(val typeface: Typeface?) {
    constructor(paint: Paint) : this(paint.typeface)
}

private val metricCache = LruCache<MetricCacheKey, Float>(2)

/** Returns the calculated ascent ratio for vertical writing. */
private fun getStaticAscentRatio(paint: Paint): Float {
    val key = MetricCacheKey(paint)
    return metricCache.get(key) ?: createCJKMetrics(paint).also { metricCache.put(key, it) }
}

/**
 * Calculates the ascent ratio for 1px text size (i.e. 1em = 1px) so that it can be reused for
 * multiple text sizes. Scaled metrics should be used by multiplying the text size. This metrics is
 * calculated based on CJK character "あ" with the given paint (mainly for the typeface) as a
 * best-effort estimation.
 */
private fun createCJKMetrics(paint: Paint): Float {
    val oldTextSize = paint.textSize
    return try {
        paint.textSize = 1000f
        val metrics = Paint.FontMetricsInt()
        // Use a common CJK character to estimate vertical metrics.
        paint.getFontMetricsIntCompat("あ", 0, 1, 0, 1, false, metrics)
        val ascent = metrics.ascent.toFloat()
        val descent = metrics.descent.toFloat()
        ascent / (ascent - descent)
    } finally {
        paint.textSize = oldTextSize
    }
}

/**
 * Draws vertical text.
 *
 * For API levels below BAKLAVA (where native vertical text support was introduced), this performs a
 * backport by manually positioning each character cluster.
 */
internal fun Canvas.drawTextVertical(
    text: CharSequence,
    start: Int,
    end: Int,
    x: Float,
    y: Float,
    paint: Paint,
) {
    if (start < 0 || end < 0 || start > end || end > text.length) {
        throw IndexOutOfBoundsException()
    }
    if (start == end) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
        paint.withVerticalFlag() { drawText(text, start, end, x, y, paint) }
    } else {
        val ascentRatio = getStaticAscentRatio(paint)
        val oldFontFeatureSettings = paint.fontFeatureSettings
        try {
            // Enable vertical glyph alternates if available in the font.
            paint.fontFeatureSettings = "\"vert\" on"
            val advances = FloatArray(end - start)
            // Measure horizontal advances of characters to determine cluster boundaries.
            paint.getRunCharacterAdvanceCompat(
                text,
                start,
                end,
                start,
                end,
                false,
                end,
                advances,
                0,
            )

            var clusterStartIndex = 0
            // Initial yOffset positions the baseline of the first character.
            // In CJK fonts, the hhea ascender/descender can be looser than the vmtx height.
            // Using Paint.FontMetrics.ascent directly can shift the glyph down, so as a
            // workaround, we use a calculated ratio of the ascent within the 1em height.
            var yOffset = y + ascentRatio * paint.textSize

            for (clusterEndIndex in 1 until end - start) {
                // In many fonts/APIs, non-first characters in a cluster have 0 advance.
                if (advances[clusterEndIndex] != 0f) {
                    // Center the cluster horizontally at x.
                    val xOffset = x - advances[clusterStartIndex] / 2
                    drawText(
                        text,
                        start + clusterStartIndex,
                        start + clusterEndIndex,
                        xOffset,
                        yOffset,
                        paint,
                    )
                    clusterStartIndex = clusterEndIndex

                    // Each cluster is assumed to take 1em (paint.textSize) height.
                    yOffset += paint.textSize
                }
            }
            // Draw the last cluster.
            val xOffset = x - advances[clusterStartIndex] / 2
            drawText(text, start + clusterStartIndex, end, xOffset, yOffset, paint)
        } finally {
            paint.fontFeatureSettings = oldFontFeatureSettings
        }
    }
}

internal fun Canvas.drawTextVertical(text: String, x: Float, y: Float, paint: Paint) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
        paint.withVerticalFlag() { drawText(text, x, y, paint) }
    } else {
        drawTextVertical(text as CharSequence, 0, text.length, x, y, paint)
    }
}
