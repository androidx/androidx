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

package androidx.xr.testutils

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class SpatialSceneScreenshotUtilsTest {

    private class FakeTimeSource : TimeSource {
        var elapsed: Duration = Duration.ZERO

        fun advanceBy(duration: Duration) {
            elapsed += duration
        }

        override fun markNow(): TimeMark =
            object : TimeMark {
                private val start = elapsed

                override fun elapsedNow(): Duration = elapsed - start
            }
    }

    private fun solidBitmap(color: Int = Color.BLACK, size: Int = 64): Bitmap =
        Bitmap.createBitmap(IntArray(size * size) { color }, size, size, Bitmap.Config.ARGB_8888)

    private fun foregroundBitmap(
        foregroundColor: Int,
        bgColor: Int = Color.BLACK,
        size: Int = 64,
    ): Bitmap {
        val pixels = IntArray(size * size) { bgColor }
        val start = size / 4
        val end = (size * 3) / 4
        for (y in start until end) {
            for (x in start until end) {
                pixels[y * size + x] = foregroundColor
            }
        }
        return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    }

    @Test
    fun skipsConsecutiveBlankFramesUntilForegroundContentSettles() {
        val timeSource = FakeTimeSource()
        val produced = mutableListOf<Bitmap>()
        val sequence =
            listOf(
                { solidBitmap(Color.BLACK) },
                { solidBitmap(Color.BLACK) },
                { solidBitmap(Color.BLACK) },
                { foregroundBitmap(Color.BLUE) },
                { foregroundBitmap(Color.BLUE) },
                { foregroundBitmap(Color.BLUE) },
            )
        var index = 0

        val settled =
            waitForSettledSoftwareScreenshot(
                screenshotProvider = {
                    sequence[index++].invoke().also { produced.add(it) }
                },
                timeout = 5000.milliseconds,
                pollInterval = 100.milliseconds,
                timeSource = timeSource,
                sleepMillis = { timeSource.advanceBy(it.milliseconds) },
            )

        assertEquals(6, index)
        assertEquals(Color.BLUE, settled.getPixel(32, 32))
        assertFalse(settled.isRecycled)
        assertTrue(produced.dropLast(1).all { it.isRecycled })
        settled.recycle()
    }

    @Test
    fun resetsMatchCountWhenStaleFrameTransitionsToNewContent() {
        val timeSource = FakeTimeSource()
        val produced = mutableListOf<Bitmap>()
        val sequence =
            listOf(
                { foregroundBitmap(Color.BLUE) },
                { foregroundBitmap(Color.BLUE) },
                { foregroundBitmap(Color.RED) },
                { foregroundBitmap(Color.RED) },
                { foregroundBitmap(Color.RED) },
            )
        var index = 0

        val settled =
            waitForSettledSoftwareScreenshot(
                screenshotProvider = {
                    sequence[index++].invoke().also { produced.add(it) }
                },
                timeout = 5000.milliseconds,
                pollInterval = 100.milliseconds,
                timeSource = timeSource,
                sleepMillis = { timeSource.advanceBy(it.milliseconds) },
            )

        assertEquals(5, index)
        assertEquals(Color.RED, settled.getPixel(32, 32))
        assertFalse(settled.isRecycled)
        assertTrue(produced.dropLast(1).all { it.isRecycled })
        settled.recycle()
    }

    @Test
    fun resetsMatchCountWhenIntermediateFrameDiffers() {
        val timeSource = FakeTimeSource()
        val sequence =
            listOf(
                { foregroundBitmap(Color.BLUE) },
                { foregroundBitmap(Color.BLUE) },
                { foregroundBitmap(Color.GREEN) },
                { foregroundBitmap(Color.BLUE) },
                { foregroundBitmap(Color.BLUE) },
                { foregroundBitmap(Color.BLUE) },
            )
        var index = 0

        val settled =
            waitForSettledSoftwareScreenshot(
                screenshotProvider = { sequence[index++].invoke() },
                timeout = 5000.milliseconds,
                pollInterval = 100.milliseconds,
                timeSource = timeSource,
                sleepMillis = { timeSource.advanceBy(it.milliseconds) },
            )

        assertEquals(6, index)
        assertEquals(Color.BLUE, settled.getPixel(32, 32))
        assertFalse(settled.isRecycled)
        settled.recycle()
    }

    @Test
    fun returnsLatestBlankFrameWhenTimeoutElapsesWithoutForeground() {
        val timeSource = FakeTimeSource()
        var calls = 0

        val result =
            waitForSettledSoftwareScreenshot(
                screenshotProvider = {
                    calls++
                    solidBitmap(Color.BLACK)
                },
                timeout = 250.milliseconds,
                pollInterval = 100.milliseconds,
                timeSource = timeSource,
                sleepMillis = { timeSource.advanceBy(it.milliseconds) },
            )

        assertEquals(4, calls)
        assertEquals(Color.BLACK, result.getPixel(32, 32))
        assertFalse(result.isRecycled)
        result.recycle()
    }

    @Test
    fun returnsLatestFrameWhenFramesKeepChangingUntilTimeout() {
        val timeSource = FakeTimeSource()
        var calls = 0

        val result =
            waitForSettledSoftwareScreenshot(
                screenshotProvider = {
                    val color = if (calls % 2 == 0) Color.BLUE else Color.RED
                    calls++
                    foregroundBitmap(color)
                },
                timeout = 250.milliseconds,
                pollInterval = 100.milliseconds,
                timeSource = timeSource,
                sleepMillis = { timeSource.advanceBy(it.milliseconds) },
            )

        assertEquals(4, calls)
        assertEquals(Color.RED, result.getPixel(32, 32))
        assertFalse(result.isRecycled)
        result.recycle()
    }

    @Test
    fun throwsAssertionErrorWhenAllCapturesReturnNull() {
        val timeSource = FakeTimeSource()

        assertFailsWith<AssertionError> {
            waitForSettledSoftwareScreenshot(
                screenshotProvider = { null },
                timeout = 200.milliseconds,
                pollInterval = 100.milliseconds,
                timeSource = timeSource,
                sleepMillis = { timeSource.advanceBy(it.milliseconds) },
            )
        }
    }
}
