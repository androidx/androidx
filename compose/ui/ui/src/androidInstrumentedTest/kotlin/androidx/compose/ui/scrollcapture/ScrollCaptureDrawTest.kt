/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.scrollcapture

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.assertColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.graphics.applyCanvas
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 31)
class ScrollCaptureDrawTest {

    @get:Rule val rule = createComposeRule()

    private val captureTester = ScrollCaptureTester(rule)

    @Test
    fun capture_drawsScrollContents_fromTop_withCaptureHeight1px() =
        captureTester.runTest {
            val scrollState = ScrollState(0)
            captureTester.setContent { TestContent(scrollState) }
            val target = captureTester.findCaptureTargets().single()
            val bitmaps = captureTester.captureBitmapsVertically(target, captureHeight = 1)
            assertThat(bitmaps).hasSize(27)
            bitmaps.joinVerticallyToBitmap().use { joined ->
                joined.assertRect(Rect(0, 0, 10, 9), Color.Red)
                joined.assertRect(Rect(0, 10, 10, 18), Color.Blue)
                joined.assertRect(Rect(0, 19, 10, 27), Color.Green)
            }
        }

    @Test
    fun capture_drawsScrollContents_fromTop_withCaptureHeightFullViewport() =
        captureTester.runTest {
            val scrollState = ScrollState(0)
            captureTester.setContent { TestContent(scrollState) }
            val target = captureTester.findCaptureTargets().single()
            val bitmaps = captureTester.captureBitmapsVertically(target, captureHeight = 10)
            assertThat(bitmaps).hasSize(3)
            bitmaps.joinVerticallyToBitmap().use { joined ->
                joined.assertRect(Rect(0, 0, 10, 9), Color.Red)
                joined.assertRect(Rect(0, 10, 10, 18), Color.Blue)
                joined.assertRect(Rect(0, 19, 10, 27), Color.Green)
            }
        }

    @Test
    fun capture_drawsScrollContents_fromMiddle_withCaptureHeight1px() =
        captureTester.runTest {
            val scrollState = ScrollState(0)
            captureTester.setContent { TestContent(scrollState) }

            scrollState.scrollTo(scrollState.maxValue / 2)

            val target = captureTester.findCaptureTargets().single()
            val bitmaps = captureTester.captureBitmapsVertically(target, captureHeight = 1)
            bitmaps.joinVerticallyToBitmap().use { joined ->
                joined.assertRect(Rect(0, 0, 10, 9), Color.Red)
                joined.assertRect(Rect(0, 10, 10, 18), Color.Blue)
                joined.assertRect(Rect(0, 19, 10, 27), Color.Green)
            }
        }

    @Test
    fun capture_drawsScrollContents_fromMiddle_withCaptureHeightFullViewport() =
        captureTester.runTest {
            val scrollState = ScrollState(0)
            captureTester.setContent { TestContent(scrollState) }

            scrollState.scrollTo(scrollState.maxValue / 2)

            val target = captureTester.findCaptureTargets().single()
            val bitmaps = captureTester.captureBitmapsVertically(target, captureHeight = 10)
            assertThat(bitmaps).hasSize(3)
            bitmaps.joinVerticallyToBitmap().use { joined ->
                joined.assertRect(Rect(0, 0, 10, 9), Color.Red)
                joined.assertRect(Rect(0, 10, 10, 18), Color.Blue)
                joined.assertRect(Rect(0, 19, 10, 27), Color.Green)
            }
        }

    @Test
    fun capture_drawsScrollContents_fromBottom_withCaptureHeight1px() =
        captureTester.runTest {
            val scrollState = ScrollState(0)
            captureTester.setContent { TestContent(scrollState) }

            scrollState.scrollTo(scrollState.maxValue)

            val target = captureTester.findCaptureTargets().single()
            val bitmaps = captureTester.captureBitmapsVertically(target, captureHeight = 1)
            bitmaps.joinVerticallyToBitmap().use { joined ->
                joined.assertRect(Rect(0, 0, 10, 9), Color.Red)
                joined.assertRect(Rect(0, 10, 10, 18), Color.Blue)
                joined.assertRect(Rect(0, 19, 10, 27), Color.Green)
            }
        }

    @Test
    fun capture_drawsScrollContents_fromBottom_withCaptureHeightFullViewport() =
        captureTester.runTest {
            val scrollState = ScrollState(0)
            captureTester.setContent { TestContent(scrollState) }

            scrollState.scrollTo(scrollState.maxValue)

            val target = captureTester.findCaptureTargets().single()
            val bitmaps = captureTester.captureBitmapsVertically(target, captureHeight = 10)
            assertThat(bitmaps).hasSize(3)
            bitmaps.joinVerticallyToBitmap().use { joined ->
                joined.assertRect(Rect(0, 0, 10, 9), Color.Red)
                joined.assertRect(Rect(0, 10, 10, 18), Color.Blue)
                joined.assertRect(Rect(0, 19, 10, 27), Color.Green)
            }
        }

    @Test
    fun capture_resetsScrollPosition_from0() =
        captureTester.runTest {
            val scrollState = ScrollState(0)
            captureTester.setContent { TestContent(scrollState) }
            val target = captureTester.findCaptureTargets().single()
            val bitmaps = captureTester.captureBitmapsVertically(target, captureHeight = 10)
            bitmaps.forEach { it.recycle() }
            rule.awaitIdle()
            assertThat(scrollState.value).isEqualTo(0)
        }

    @Test
    fun capture_resetsScrollPosition_fromNonZero() =
        captureTester.runTest {
            val scrollState = ScrollState(5)
            captureTester.setContent { TestContent(scrollState) }
            val target = captureTester.findCaptureTargets().single()
            val bitmaps = captureTester.captureBitmapsVertically(target, captureHeight = 10)
            bitmaps.forEach { it.recycle() }
            rule.awaitIdle()
            assertThat(scrollState.value).isEqualTo(5)
        }

    @Test
    fun capture_drawsScrollContents_fromTop_withCaptureHeightFullViewport_reverseScrolling() =
        captureTester.runTest {
            val scrollState = ScrollState(0)
            captureTester.setContent { TestContent(scrollState, reverseScrolling = true) }
            val target = captureTester.findCaptureTargets().single()
            val bitmaps = captureTester.captureBitmapsVertically(target, captureHeight = 10)
            assertThat(bitmaps).hasSize(3)
            bitmaps.joinVerticallyToBitmap().use { joined ->
                joined.assertRect(Rect(0, 0, 10, 9), Color.Red)
                joined.assertRect(Rect(0, 10, 10, 18), Color.Blue)
                joined.assertRect(Rect(0, 19, 10, 27), Color.Green)
            }
        }

    @Test
    fun capture_drawsScrollContents_fromMiddle_withCaptureHeightFullViewport_reverseScrolling() =
        captureTester.runTest {
            val scrollState = ScrollState(0)
            captureTester.setContent { TestContent(scrollState, reverseScrolling = true) }

            scrollState.scrollTo(scrollState.maxValue / 2)

            val target = captureTester.findCaptureTargets().single()
            val bitmaps = captureTester.captureBitmapsVertically(target, captureHeight = 10)
            assertThat(bitmaps).hasSize(3)
            bitmaps.joinVerticallyToBitmap().use { joined ->
                joined.assertRect(Rect(0, 0, 10, 9), Color.Red)
                joined.assertRect(Rect(0, 10, 10, 18), Color.Blue)
                joined.assertRect(Rect(0, 19, 10, 27), Color.Green)
            }
        }

    @Test
    fun capture_drawsScrollContents_fromBottom_withCaptureHeightFullViewport_reverseScrolling() =
        captureTester.runTest {
            val scrollState = ScrollState(0)
            captureTester.setContent { TestContent(scrollState, reverseScrolling = true) }

            scrollState.scrollTo(scrollState.maxValue)

            val target = captureTester.findCaptureTargets().single()
            val bitmaps = captureTester.captureBitmapsVertically(target, captureHeight = 10)
            assertThat(bitmaps).hasSize(3)
            bitmaps.joinVerticallyToBitmap().use { joined ->
                joined.assertRect(Rect(0, 0, 10, 9), Color.Red)
                joined.assertRect(Rect(0, 10, 10, 18), Color.Blue)
                joined.assertRect(Rect(0, 19, 10, 27), Color.Green)
            }
        }

    @Test
    fun capture_resetsScrollPosition_from0_reverseScrolling() =
        captureTester.runTest {
            val scrollState = ScrollState(0)
            captureTester.setContent { TestContent(scrollState, reverseScrolling = true) }
            val target = captureTester.findCaptureTargets().single()
            val bitmaps = captureTester.captureBitmapsVertically(target, captureHeight = 10)
            bitmaps.forEach { it.recycle() }
            rule.awaitIdle()
            assertThat(scrollState.value).isEqualTo(0)
        }

    @Test
    fun capture_resetsScrollPosition_fromNonZero_reverseScrolling() =
        captureTester.runTest {
            val scrollState = ScrollState(5)
            captureTester.setContent { TestContent(scrollState, reverseScrolling = true) }
            val target = captureTester.findCaptureTargets().single()
            val bitmaps = captureTester.captureBitmapsVertically(target, captureHeight = 10)
            bitmaps.forEach { it.recycle() }
            rule.awaitIdle()
            assertThat(scrollState.value).isEqualTo(5)
        }

    @Composable
    private fun TestContent(
        scrollState: ScrollState,
        reverseScrolling: Boolean = false,
    ) {
        with(LocalDensity.current) {
            Column(
                Modifier.size(10.toDp())
                    .verticalScroll(scrollState, reverseScrolling = reverseScrolling)
            ) {
                Box(Modifier.background(Color.Red).height(9.toDp()).fillMaxWidth())
                Box(Modifier.background(Color.Blue).height(9.toDp()).fillMaxWidth())
                Box(Modifier.background(Color.Green).height(9.toDp()).fillMaxWidth())
            }
        }
    }

    private inline fun Bitmap.use(block: (Bitmap) -> Unit) {
        try {
            block(this)
        } finally {
            recycle()
        }
    }

    private fun Iterable<Bitmap>.joinVerticallyToBitmap(): Bitmap {
        val width = maxOf { it.width }
        val height = sumOf { it.height }
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        var y = 0
        try {
            result.applyCanvas {
                forEach {
                    drawBitmap(it, 0f, y.toFloat(), null)
                    y += it.height
                }
            }
        } finally {
            forEach { it.recycle() }
        }
        return result
    }

    private fun Bitmap.assertRect(rect: Rect, color: Color) {
        for (x in rect.left until rect.right) {
            for (y in rect.top until rect.bottom) {
                assertColor(color, x, y)
            }
        }
    }
}
