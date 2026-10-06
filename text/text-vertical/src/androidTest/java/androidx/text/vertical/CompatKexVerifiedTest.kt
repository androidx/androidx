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
import android.text.TextPaint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests branches in `CanvasCompat.kt` and `PaintCompat.kt` derived from Kex symbolic execution
 * paths.
 *
 * Covers [drawTextVertical], `MetricCacheKey`, `getStaticAscentRatio`, [getFontMetricsIntCompat],
 * [getRunCharacterAdvanceCompat], `sumAdvances`, [measureTextVertical], and
 * [getRunCharacterAdvanceVertical].
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class CompatKexVerifiedTest {

    private val paint = TextPaint().apply { textSize = 20f }

    // =========================================================================================
    // Target 1: Canvas.drawTextVertical bounds checks and empty-range early return
    // =========================================================================================

    // Branch: start < 0 in drawTextVertical throws IndexOutOfBoundsException.
    @Test
    fun kex_drawTextVertical_whenStartIsNegative_throwsIndexOutOfBoundsException() {
        val canvas = Canvas()

        assertThrows(IndexOutOfBoundsException::class.java) {
            canvas.drawTextVertical("あいう", start = -1, end = 2, x = 0f, y = 0f, paint = paint)
        }
    }

    // Branch: end < 0 in drawTextVertical throws IndexOutOfBoundsException.
    @Test
    fun kex_drawTextVertical_whenEndIsNegative_throwsIndexOutOfBoundsException() {
        val canvas = Canvas()

        assertThrows(IndexOutOfBoundsException::class.java) {
            canvas.drawTextVertical("あいう", start = 0, end = -1, x = 0f, y = 0f, paint = paint)
        }
    }

    // Branch: start > end in drawTextVertical throws IndexOutOfBoundsException.
    @Test
    fun kex_drawTextVertical_whenStartGreaterThanEnd_throwsIndexOutOfBoundsException() {
        val canvas = Canvas()

        assertThrows(IndexOutOfBoundsException::class.java) {
            canvas.drawTextVertical("あいう", start = 2, end = 1, x = 0f, y = 0f, paint = paint)
        }
    }

    // Branch: end > text.length in drawTextVertical throws IndexOutOfBoundsException.
    @Test
    fun kex_drawTextVertical_whenEndExceedsTextLength_throwsIndexOutOfBoundsException() {
        val canvas = Canvas()

        assertThrows(IndexOutOfBoundsException::class.java) {
            canvas.drawTextVertical("あいう", start = 0, end = 4, x = 0f, y = 0f, paint = paint)
        }
    }

    // Branch: start == end in drawTextVertical returns early without invoking Canvas.drawText.
    @Test
    fun kex_drawTextVertical_whenStartEqualsEnd_returnsEarlyWithoutDrawing() {
        var drawCallCount = 0
        val canvas =
            object : Canvas() {
                override fun drawText(
                    text: CharSequence,
                    start: Int,
                    end: Int,
                    x: Float,
                    y: Float,
                    paint: Paint,
                ) {
                    drawCallCount++
                }

                override fun drawText(text: String, x: Float, y: Float, paint: Paint) {
                    drawCallCount++
                }
            }

        canvas.drawTextVertical("あいう", start = 2, end = 2, x = 10f, y = 20f, paint = paint)
        assertThat(drawCallCount).isEqualTo(0)

        canvas.drawTextVertical("", x = 10f, y = 20f, paint = paint)

        // On API < 36, empty String delegates to start == 0, end == 0 and returns early without
        // calling drawText. On API >= 36, String overload calls drawText("", ...) directly.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) {
            assertThat(drawCallCount).isEqualTo(0)
        } else {
            assertThat(drawCallCount).isEqualTo(1)
        }
    }

    // =========================================================================================
    // Target 2: Canvas.drawTextVertical cluster loop, MetricCacheKey caching, and finally restore
    // =========================================================================================

    // Branch: API < 36 backport groups base character with zero-advance combining mark
    // (advances[clusterEndIndex] == 0f) into a single cluster and separates distinct clusters
    // (advances[clusterEndIndex] != 0f).
    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun kex_drawTextVertical_backport_groupsCombiningMarkWithBaseClusterAndRestoresPaint() {
        val drawnClusters = mutableListOf<Pair<String, Float>>()
        val canvas =
            object : Canvas() {
                override fun drawText(
                    text: CharSequence,
                    start: Int,
                    end: Int,
                    x: Float,
                    y: Float,
                    paint: Paint,
                ) {
                    drawnClusters.add(text.subSequence(start, end).toString() to y)
                }
            }
        val testPaint =
            TextPaint().apply {
                textSize = 20f
                fontFeatureSettings = "\"liga\" 0"
                typeface = Typeface.SERIF
            }

        // "か\u3099き" has 3 code units: 'か' (index 0), combining dakuten U+3099 (index 1,
        // 0 advance), and 'き' (index 2, non-zero advance).
        canvas.drawTextVertical("か\u3099き", start = 0, end = 3, x = 30f, y = 10f, paint = testPaint)
        // Second call with the same Typeface exercises the metricCache.get(key) != null branch.
        canvas.drawTextVertical("か\u3099き", start = 0, end = 3, x = 30f, y = 10f, paint = testPaint)

        assertThat(testPaint.fontFeatureSettings).isEqualTo("\"liga\" 0")
        assertThat(testPaint.textSize).isEqualTo(20f)
        assertThat(drawnClusters).hasSize(4)
        assertThat(drawnClusters[0].first).isEqualTo("か\u3099")
        assertThat(drawnClusters[1].first).isEqualTo("き")
        assertThat(drawnClusters[1].second - drawnClusters[0].second).isWithin(0.01f).of(20f)
        assertThat(drawnClusters[2]).isEqualTo(drawnClusters[0])
        assertThat(drawnClusters[3]).isEqualTo(drawnClusters[1])
    }

    // Branch: Both CharSequence and String overloads of drawTextVertical apply vertical paint
    // state during drawing and restore paint flags and fontFeatureSettings afterward.
    @Test
    fun kex_drawTextVertical_stringOverload_drawsTextAndRestoresPaintState() {
        val recordedFlags = mutableListOf<Int>()
        val recordedFeatures = mutableListOf<String?>()
        val canvas =
            object : Canvas() {
                override fun drawText(
                    text: CharSequence,
                    start: Int,
                    end: Int,
                    x: Float,
                    y: Float,
                    paint: Paint,
                ) {
                    recordedFlags.add(paint.flags)
                    recordedFeatures.add(paint.fontFeatureSettings)
                }

                override fun drawText(text: String, x: Float, y: Float, paint: Paint) {
                    recordedFlags.add(paint.flags)
                    recordedFeatures.add(paint.fontFeatureSettings)
                }
            }
        val testPaint =
            TextPaint().apply {
                textSize = 18f
                flags = Paint.ANTI_ALIAS_FLAG
                fontFeatureSettings = "\"kern\" 1"
            }

        canvas.drawTextVertical(
            "あいう" as CharSequence,
            start = 0,
            end = 2,
            x = 15f,
            y = 25f,
            paint = testPaint,
        )
        assertThat(testPaint.flags).isEqualTo(Paint.ANTI_ALIAS_FLAG)
        assertThat(testPaint.fontFeatureSettings).isEqualTo("\"kern\" 1")

        canvas.drawTextVertical("あい", x = 15f, y = 25f, paint = testPaint)
        assertThat(testPaint.flags).isEqualTo(Paint.ANTI_ALIAS_FLAG)
        assertThat(testPaint.fontFeatureSettings).isEqualTo("\"kern\" 1")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            assertThat(recordedFlags).hasSize(2)
            recordedFlags.forEach { flags ->
                assertThat(flags and Paint.VERTICAL_TEXT_FLAG).isEqualTo(Paint.VERTICAL_TEXT_FLAG)
            }
        } else {
            assertThat(recordedFeatures)
                .containsExactly("\"vert\" on", "\"vert\" on", "\"vert\" on", "\"vert\" on")
        }
    }

    // =========================================================================================
    // Target 3: PaintCompat (getFontMetricsIntCompat, getRunCharacterAdvanceCompat,
    // measureTextVertical, getRunCharacterAdvanceVertical)
    // =========================================================================================

    // Branch: getFontMetricsIntCompat populates FontMetricsInt across all API tiers.
    @Test
    fun kex_getFontMetricsIntCompat_populatesFontMetricsInt() {
        val fm = Paint.FontMetricsInt()

        paint.getFontMetricsIntCompat(
            cs = "あいう",
            start = 1,
            count = 1,
            contextStart = 0,
            contextCount = 3,
            isRtl = false,
            out = fm,
        )

        assertThat(fm.ascent).isLessThan(0)
        assertThat(fm.descent).isGreaterThan(0)
    }

    // Branch: getRunCharacterAdvanceCompat with non-zero contextStart, start > contextStart,
    // outOffset > 0, and offset == start (sumAdvances count == 0) vs offset == end.
    @Test
    fun kex_getRunCharacterAdvanceCompat_withSubstringContextAndOutOffset_computesPrefixAdvances() {
        val text = "prefix_abc_suffix"
        val contextStart = 7
        val start = 8
        val end = 10
        val out = FloatArray(6)

        val zeroOffsetAdvance =
            paint.getRunCharacterAdvanceCompat(
                text = text,
                start = start,
                end = end,
                contextStart = contextStart,
                contextEnd = 10,
                isRtl = false,
                offset = start,
                out = out,
                outOffset = 2,
            )
        val fullOffsetAdvance =
            paint.getRunCharacterAdvanceCompat(
                text = text,
                start = start,
                end = end,
                contextStart = contextStart,
                contextEnd = 10,
                isRtl = false,
                offset = end,
                out = out,
                outOffset = 2,
            )

        assertThat(zeroOffsetAdvance).isEqualTo(0f)
        assertThat(out[0]).isEqualTo(0f)
        assertThat(out[1]).isEqualTo(0f)
        assertThat(out[2]).isGreaterThan(0f)
        assertThat(out[3]).isGreaterThan(0f)
        assertThat(fullOffsetAdvance).isWithin(0.01f).of(out[2] + out[3])
    }

    // Branch: measureTextVertical (1-arg and 3-arg) with empty range and non-empty text.
    @Test
    fun kex_measureTextVertical_whenEmptyAndNonEmptyRange_returnsExpectedVerticalHeight() {
        val originalFlags = paint.flags

        assertThat(paint.measureTextVertical("")).isEqualTo(0f)
        assertThat(paint.measureTextVertical("あいう", start = 2, end = 2)).isEqualTo(0f)

        val twoCharAdvance = paint.measureTextVertical("あい")
        val subRangeAdvance = paint.measureTextVertical("あいう", start = 0, end = 2)

        assertThat(twoCharAdvance).isGreaterThan(0f)
        assertThat(subRangeAdvance).isWithin(0.01f).of(twoCharAdvance)
        assertThat(paint.flags).isEqualTo(originalFlags)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            val preflaggedPaint =
                TextPaint().apply {
                    textSize = 20f
                    flags = Paint.ANTI_ALIAS_FLAG or Paint.VERTICAL_TEXT_FLAG
                }
            assertThat(preflaggedPaint.measureTextVertical("あい")).isWithin(0.01f).of(twoCharAdvance)
            assertThat(preflaggedPaint.flags)
                .isEqualTo(Paint.ANTI_ALIAS_FLAG or Paint.VERTICAL_TEXT_FLAG)
        }
    }

    // Branch: measureTextVertical and getRunCharacterAdvanceVertical on API < 36 backport assign
    // textSize to base characters (widths[i] > 0f) and 0f to combining marks (widths[i] <= 0f).
    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun kex_measureAndGetRunCharacterAdvanceVertical_backport_handlesZeroWidthCombiningMark() {
        // "か\u3099き" has two base kana ('か', 'き') and one zero-width combining dakuten ('\u3099').
        val text = "か\u3099き"
        val out = FloatArray(5)

        val measuredHeight = paint.measureTextVertical(text, start = 0, end = text.length)
        val runAdvance =
            paint.getRunCharacterAdvanceVertical(
                text = text,
                start = 0,
                end = text.length,
                contextStart = 0,
                contextEnd = text.length,
                isRtl = false,
                offset = text.length,
                out = out,
                outOffset = 1,
            )

        assertThat(measuredHeight).isWithin(0.01f).of(2f * paint.textSize)
        assertThat(runAdvance).isWithin(0.01f).of(2f * paint.textSize)
        assertThat(out[0]).isEqualTo(0f)
        assertThat(out[1]).isEqualTo(paint.textSize)
        assertThat(out[2]).isEqualTo(0f)
        assertThat(out[3]).isEqualTo(paint.textSize)
        assertThat(out[4]).isEqualTo(0f)
    }

    // Branch: getRunCharacterAdvanceVertical on all API tiers populates out array at outOffset and
    // returns 0f for an empty range.
    @Test
    fun kex_getRunCharacterAdvanceVertical_whenEmptyAndNonEmptyRange_populatesOutArray() {
        val emptyOut = FloatArray(2)
        val emptyAdvance =
            paint.getRunCharacterAdvanceVertical(
                text = "あい",
                start = 1,
                end = 1,
                contextStart = 0,
                contextEnd = 2,
                isRtl = false,
                offset = 1,
                out = emptyOut,
                outOffset = 0,
            )
        assertThat(emptyAdvance).isEqualTo(0f)
        assertThat(emptyOut.toList()).containsExactly(0f, 0f)

        val out = FloatArray(4)
        val advance =
            paint.getRunCharacterAdvanceVertical(
                text = "あい",
                start = 0,
                end = 2,
                contextStart = 0,
                contextEnd = 2,
                isRtl = false,
                offset = 2,
                out = out,
                outOffset = 1,
            )

        assertThat(advance).isGreaterThan(0f)
        assertThat(out[0]).isEqualTo(0f)
        assertThat(out[1]).isGreaterThan(0f)
        assertThat(out[2]).isGreaterThan(0f)
        assertThat(out[3]).isEqualTo(0f)
    }
}
