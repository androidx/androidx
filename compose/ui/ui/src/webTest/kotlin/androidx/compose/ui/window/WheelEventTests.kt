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

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.OnCanvasTests
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.w3c.dom.events.WheelEvent
import org.w3c.dom.events.WheelEventInit

class WheelEventTests : OnCanvasTests {

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