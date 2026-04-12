/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.foundation.benchmark

import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.runBlocking

/**
 * Test case that puts a large number of boxes in a column in a vertical scroller to force
 * scrolling.
 *
 * [toggleState] calls [ScrollState.scrollTo] between oscillating values
 */
class ScrollerTestCase : LayeredComposeTestCase(), ToggleableTestCase {
    private lateinit var scrollState: ScrollState

    @Composable
    override fun MeasuredContent() {
        scrollState = rememberScrollState()
        Column(Modifier.verticalScroll(scrollState)) {
            ColorStripes(step = 1, Modifier.fillMaxHeight())
        }
    }

    override fun toggleState() {
        runBlocking { scrollState.scrollTo(if (scrollState.value == 0) 10 else 0) }
    }
}

/**
 * Test case that puts a large number of boxes in a column in a vertical scroller to force
 * scrolling.
 *
 * [toggleState] injects mouse wheel events to scroll forward and backwards repeatedly.
 */
class MouseWheelScrollerTestCase() : LayeredComposeTestCase(), ToggleableTestCase {
    private lateinit var scrollState: ScrollState
    private var view: View? = null
    private var currentEventTime: Long = 0
    private var lastScrollUp: Boolean = true

    @Composable
    override fun MeasuredContent() {
        view = LocalView.current
        scrollState = rememberScrollState()
        Column(Modifier.verticalScroll(scrollState)) {
            // A lower step causes benchmark issues due to the resulting size / number of nodes
            ColorStripes(step = 5, Modifier.fillMaxHeight())
        }
    }

    override fun toggleState() {
        // For mouse wheel scroll, negative values scroll down
        // Note: these aren't the actual values that will be used to scroll, depending on
        // Android version these values will get converted into a different (larger) value.
        // This also unfortunately includes an animation that cannot be disabled, so this
        // is a best effort at some repeated scrolling
        val scrollAmount =
            if (lastScrollUp) {
                lastScrollUp = false
                -10
            } else {
                lastScrollUp = true
                10
            }
        dispatchMouseWheelScroll(scrollAmount = scrollAmount, eventTime = currentEventTime, view!!)
        currentEventTime += 10
    }
}

@Composable
private fun ColorStripes(step: Int, modifier: Modifier) {
    Column(modifier) {
        for (green in 0..0xFF step step) {
            ColorStripe(0xFF, green, 0)
        }
        for (red in 0xFF downTo 0 step step) {
            ColorStripe(red, 0xFF, 0)
        }
        for (blue in 0..0xFF step step) {
            ColorStripe(0, 0xFF, blue)
        }
        for (green in 0xFF downTo 0 step step) {
            ColorStripe(0, green, 0xFF)
        }
        for (red in 0..0xFF step step) {
            ColorStripe(red, 0, 0xFF)
        }
        for (blue in 0xFF downTo 0 step step) {
            ColorStripe(0xFF, 0, blue)
        }
    }
}

@Composable
private fun ColorStripe(red: Int, green: Int, blue: Int) {
    Canvas(Modifier.size(45.dp, 5.dp)) { drawRect(Color(red = red, green = green, blue = blue)) }
}

private fun dispatchMouseWheelScroll(scrollAmount: Int, eventTime: Long, view: View) {
    val properties =
        MotionEvent.PointerProperties().apply { toolType = MotionEvent.TOOL_TYPE_MOUSE }

    val coords =
        MotionEvent.PointerCoords().apply {
            x = view.measuredWidth / 2f
            y = view.measuredHeight / 2f
            setAxisValue(MotionEvent.AXIS_VSCROLL, scrollAmount.toFloat())
        }

    val event =
        MotionEvent.obtain(
            0, /* downTime */
            eventTime, /* eventTime */
            MotionEvent.ACTION_SCROLL, /* action */
            1, /* pointerCount */
            arrayOf(properties),
            arrayOf(coords),
            0, /* metaState */
            0, /* buttonState */
            0f, /* xPrecision */
            0f, /* yPrecision */
            0, /* deviceId */
            0, /* edgeFlags */
            InputDevice.SOURCE_MOUSE, /* source */
            0, /* flags */
        )

    view.dispatchGenericMotionEvent(event)
}
