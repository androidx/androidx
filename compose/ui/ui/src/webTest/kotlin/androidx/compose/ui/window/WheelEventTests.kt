/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.window

import androidx.compose.foundation.InternalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.resetWheelEventTrackingForTests
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.OnCanvasTests
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.browser.window
import kotlinx.coroutines.test.runTest
import org.w3c.dom.events.WheelEvent
import org.w3c.dom.events.WheelEventInit

@OptIn(InternalFoundationApi::class)
class WheelEventTests : OnCanvasTests {

    @OptIn(InternalComposeApi::class)
    @BeforeTest
    fun resetWheelTracking() {
        // JsConfig keeps wheel-event state in a page-global singleton; clear it so a previous
        // test's last event can't be mistaken for part of the current test's gesture.
        resetWheelEventTrackingForTests()
    }

    @Test
    fun verticalScroll() = runTest {
        val verticalScrollState = ScrollState(initial = 0)

        createComposeWindow {
            CompositionLocalProvider(LocalDensity provides Density(2f)) {
                Box(
                    modifier = Modifier.size(100.dp).verticalScroll(verticalScrollState)
                ) {
                    Column(modifier = Modifier.size(400.dp)) { }
                }
            }
        }

        assertEquals(0, verticalScrollState.value)

        // do horizontal scroll, and check that scroll state didn't change
        getCanvas().dispatchEvent(WheelEvent("wheel", WheelEventInit(deltaX = 5.0)))
        assertEquals(0, verticalScrollState.value, "vertical scroll was not expected to change")

        // vertical scroll
        getCanvas().dispatchEvent(WheelEvent("wheel", WheelEventInit(deltaY = 5.0)))
        assertEquals(10, verticalScrollState.value, "vertical scroll was expected to change")
    }

    @Test
    fun trackpadWheelScrollIsAppliedImmediately() = runTest {
        val verticalScrollState = ScrollState(initial = 0)

        createComposeWindow {
            CompositionLocalProvider(LocalDensity provides Density(2f)) {
                Box(
                    modifier = Modifier.size(100.dp).verticalScroll(verticalScrollState)
                ) {
                    Column(modifier = Modifier.size(400.dp)) { }
                }
            }
        }

        assertEquals(0, verticalScrollState.value)

        // A delta that is not divisible by 120 looks like high-resolution input (a trackpad
        // or a freely rotating wheel), so the whole delta is applied immediately:
        // 100 * density(2f) = 200px.
        //
        // This trackpad/stepping-wheel disambiguation relies on the legacy wheelDelta* fields,
        // which Firefox does not expose. There a pixel-mode event can't be recognized as
        // high-resolution, so it falls back to the animated stepping-wheel path and only the
        // animation threshold (6.dp * density(2f) = 12px) is applied immediately.
        val isFirefox = window.navigator.userAgent.contains("firefox", ignoreCase = true)
        val expected = if (isFirefox) 12 else 200
        getCanvas().dispatchEvent(WheelEvent("wheel", WheelEventInit(deltaY = 100.0)))

        assertEquals(
            expected,
            verticalScrollState.value,
            "high-resolution wheel scroll should apply immediately"
        )
    }

    @Test
    fun mouseWheelScrollIsAnimated() = runTest {
        val verticalScrollState = ScrollState(initial = 0)

        createComposeWindow {
            CompositionLocalProvider(LocalDensity provides Density(2f)) {
                Box(
                    modifier = Modifier.size(100.dp).verticalScroll(verticalScrollState)
                ) {
                    Column(modifier = Modifier.size(400.dp)) { }
                }
            }
        }

        assertEquals(0, verticalScrollState.value)

        // A delta divisible by 120 looks like a regular stepping mouse wheel tick, so the
        // scroll is animated: only the animation threshold (6.dp * density(2f) = 12px) is
        // applied immediately, not the full 120 * density(2f) = 240px.
        getCanvas().dispatchEvent(WheelEvent("wheel", WheelEventInit(deltaY = 120.0)))

        assertEquals(
            12,
            verticalScrollState.value,
            "stepping mouse wheel scroll should be animated, not applied immediately"
        )
    }

    @Test
    fun lineModeWheelScrollIsConvertedToPixels() = runTest {
        val verticalScrollState = ScrollState(initial = 0)

        createComposeWindow {
            CompositionLocalProvider(LocalDensity provides Density(2f)) {
                Box(
                    modifier = Modifier.size(100.dp).verticalScroll(verticalScrollState)
                ) {
                    Column(modifier = Modifier.size(400.dp)) { }
                }
            }
        }

        assertEquals(0, verticalScrollState.value)

        // A single line-mode delta must scroll by a whole line, not a single pixel.
        // The default browser font size is 16px, so 1 line * 16px * density(2f) = 32px.
        getCanvas().dispatchEvent(
            WheelEvent(
                "wheel",
                WheelEventInit(deltaY = 1.0, deltaMode = WheelEvent.DOM_DELTA_LINE)
            )
        )

        assertEquals(
            32,
            verticalScrollState.value,
            "line-mode wheel scroll should be normalized to pixels"
        )
    }

    @Test
    fun pageModeWheelScrollUsesViewportSize() = runTest {
        val verticalScrollState = ScrollState(initial = 0)

        createComposeWindow {
            CompositionLocalProvider(LocalDensity provides Density(2f)) {
                // requiredSize (not size) so the viewport keeps its 100.dp even when the test
                // canvas is smaller than 200px. The canvas is only 30% of the karma iframe (see
                // compose_context.html), which on CI can be < 200px; a plain size() would then be
                // coerced down to the canvas height and page-mode scroll (= bounds.height) would
                // no longer equal the expected viewport size.
                Box(
                    modifier = Modifier.requiredSize(100.dp).verticalScroll(verticalScrollState)
                ) {
                    Column(modifier = Modifier.size(400.dp)) { }
                }
            }
        }

        assertEquals(0, verticalScrollState.value)

        // A single page-mode delta must scroll by a whole viewport.
        // The viewport is 100.dp, so 1 page * 100.dp * density(2f) = 200px.
        getCanvas().dispatchEvent(
            WheelEvent(
                "wheel",
                WheelEventInit(deltaY = 1.0, deltaMode = WheelEvent.DOM_DELTA_PAGE)
            )
        )

        assertEquals(
            200,
            verticalScrollState.value,
            "page-mode wheel scroll should scroll by the viewport size"
        )
    }

    @Test
    fun horizontalScroll() = runTest {
        val horizontalScrollState = ScrollState(initial = 0)

        createComposeWindow {
            CompositionLocalProvider(LocalDensity provides Density(2f)) {
                Box(
                    modifier = Modifier.size(100.dp).horizontalScroll(horizontalScrollState)
                ) {
                    Column(modifier = Modifier.size(400.dp)) { }
                }
            }
        }

        assertEquals(0, horizontalScrollState.value)

        // do vertical scroll, and check that scroll state didn't change
        getCanvas().dispatchEvent(WheelEvent("wheel", WheelEventInit(deltaY = 5.0)))
        assertEquals(0, horizontalScrollState.value, "horizontal scroll was not expected to change")

        // horizontal scroll
        getCanvas().dispatchEvent(WheelEvent("wheel", WheelEventInit(deltaX = 5.0)))
        assertEquals(10, horizontalScrollState.value, "horizontal scroll was expected to change")
    }


    @Test
    fun horizontalScrollWithShift() = runTest {
        val horizontalScrollState = ScrollState(initial = 0)

        createComposeWindow {
            CompositionLocalProvider(LocalDensity provides Density(2f)) {
                Box(
                    modifier = Modifier.size(100.dp).horizontalScroll(horizontalScrollState)
                ) {
                    Column(modifier = Modifier.size(400.dp)) { }
                }
            }
        }

        assertEquals(0, horizontalScrollState.value)

        // do vertical scroll w/o Shift, and check that scroll state didn't change
        getCanvas().dispatchEvent(WheelEvent("wheel", WheelEventInit(deltaY = 5.0)))
        assertEquals(0, horizontalScrollState.value, "horizontal scroll was not expected to change")

        // do vertical scroll with Shift
        getCanvas().dispatchEvent(WheelEvent("wheel", WheelEventInit(deltaY = 5.0, shiftKey = true)))
        assertEquals(10, horizontalScrollState.value, "horizontal scroll was expected to change")
    }

    @Test
    fun verticalScrollWithShift() = runTest {
        val verticalScrollState = ScrollState(initial = 0)

        createComposeWindow {
            CompositionLocalProvider(LocalDensity provides Density(2f)) {
                Box(
                    modifier = Modifier.size(100.dp).verticalScroll(verticalScrollState)
                ) {
                    Column(modifier = Modifier.size(400.dp)) { }
                }
            }
        }

        assertEquals(0, verticalScrollState.value)

        // press shift and do horizontal scroll (X-axis)
        getCanvas().dispatchEvent(WheelEvent("wheel", WheelEventInit(deltaX = 5.0, shiftKey = true)))
        assertEquals(0, verticalScrollState.value, "horizontal scroll was not expected to change")

        // press shift and do vertical scroll (Y-axis) - verticalScrollState won't change because Shift is pressed
        getCanvas().dispatchEvent(WheelEvent("wheel", WheelEventInit(deltaY = 5.0, shiftKey = true)))
        assertEquals(0, verticalScrollState.value, "horizontal scroll was not expected to change")

        // no Shift, do vertical scroll (Y-axis)
        getCanvas().dispatchEvent(WheelEvent("wheel", WheelEventInit(deltaY = 5.0, shiftKey = false)))
        assertEquals(10, verticalScrollState.value, "horizontal scroll expected to change")
    }
}