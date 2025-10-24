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

package androidx.compose.ui.input.pointer

import android.app.Instrumentation
import android.os.SystemClock
import android.view.Choreographer
import android.view.InputDevice
import android.view.MotionEvent
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.util.lerp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This is a test validating that unbuffered dispatch will forward motion events without additional
 * batching in the Compose input pipeline.
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class RequestUnbufferedDispatchTest {
    val dispatcher = StandardTestDispatcher()
    @get:Rule val rule = createComposeRule(dispatcher)

    @Test
    fun checkMotionEventsAreBatchedWhenBuffered() {
        lateinit var density: Density
        var framesDuringMotionEventInjection = 0
        val iterations = 1000
        val frameIndexToPointerEvents = mutableMapOf<Int, MutableList<PointerEvent>>()
        frameIndexToPointerEvents[framesDuringMotionEventInjection] = mutableListOf()

        rule.setContent {
            density = LocalDensity.current
            Spacer(
                modifier =
                    Modifier.fillMaxSize().testTag("PointerInput").pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val pointerEvent = awaitPointerEvent()
                                if (pointerEvent.type == PointerEventType.Move) {
                                    frameIndexToPointerEvents
                                        .getValue(framesDuringMotionEventInjection)
                                        .add(pointerEvent)
                                }
                            }
                        }
                    }
            )
        }

        rule.waitForIdle()

        val pointerInputCenterCoordinates: Offset =
            rule.onNodeWithTag("PointerInput").fetchSemanticsNode().let { node ->
                node.positionOnScreen + node.size.center.toOffset()
            }
        val radius = with(density) { 16.dp.toPx() }

        val frameCallback =
            object : Choreographer.FrameCallback {
                override fun doFrame(frameTimeNanos: Long) {
                    framesDuringMotionEventInjection++
                    frameIndexToPointerEvents[framesDuringMotionEventInjection] = mutableListOf()
                    dispatcher.scheduler.runCurrent()
                    Choreographer.getInstance().postFrameCallback(this)
                }
            }

        rule.runOnUiThread { Choreographer.getInstance().postFrameCallback(frameCallback) }

        rule.injectCircleTraceEvents(pointerInputCenterCoordinates, radius, iterations)

        rule.runOnUiThread { Choreographer.getInstance().removeFrameCallback(frameCallback) }

        assertEquals(
            1,
            frameIndexToPointerEvents.values.maxOf { it.size },
            "Expected to see exactly one pointer event per frame",
        )
    }

    @Test
    fun checkAllMotionEventsAreReceivedWhenUnbuffered_withPointerInteropFilter() {
        lateinit var density: Density
        var framesDuringMotionEventInjection = 0
        val iterations = 1000
        val frameIndexToPointerEvents = mutableMapOf<Int, MutableList<PointerEvent>>()
        frameIndexToPointerEvents[framesDuringMotionEventInjection] = mutableListOf()

        rule.setContent {
            density = LocalDensity.current
            val containingView = LocalView.current

            Spacer(
                modifier =
                    Modifier.fillMaxSize()
                        .testTag("PointerInput")
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val pointerEvent = awaitPointerEvent()
                                    if (pointerEvent.type == PointerEventType.Move) {
                                        frameIndexToPointerEvents
                                            .getValue(framesDuringMotionEventInjection)
                                            .add(pointerEvent)
                                    }
                                }
                            }
                        }
                        .pointerInteropFilter {
                            containingView.requestUnbufferedDispatch(it)
                            false
                        }
            )
        }

        rule.waitForIdle()

        val pointerInputCenterCoordinates: Offset =
            rule.onNodeWithTag("PointerInput").fetchSemanticsNode().let { node ->
                node.positionOnScreen + node.size.center.toOffset()
            }
        val radius = with(density) { 16.dp.toPx() }

        val frameCallback =
            object : Choreographer.FrameCallback {
                override fun doFrame(frameTimeNanos: Long) {
                    framesDuringMotionEventInjection++
                    frameIndexToPointerEvents[framesDuringMotionEventInjection] = mutableListOf()
                    dispatcher.scheduler.runCurrent()
                    Choreographer.getInstance().postFrameCallback(this)
                }
            }

        rule.runOnUiThread { Choreographer.getInstance().postFrameCallback(frameCallback) }

        rule.injectCircleTraceEvents(pointerInputCenterCoordinates, radius, iterations)

        rule.runOnUiThread { Choreographer.getInstance().removeFrameCallback(frameCallback) }

        assertTrue(
            frameIndexToPointerEvents.values.maxOf { it.size } > 1,
            "Expected to see more than one pointer event per frame at some point",
        )
        assertTrue(
            frameIndexToPointerEvents.values.maxOf { it.size } <
                (iterations * 4f / framesDuringMotionEventInjection),
            "Expected to see no more than four times the fair share of pointer events in any given frame",
        )
        assertEquals(
            iterations,
            frameIndexToPointerEvents.values.sumOf { it.size },
            "Expected to receive pointer events for the $iterations moves, " +
                "but didn't see the correct amount.",
        )
    }

    @Test
    fun checkAllMotionEventsAreReceivedWhenUnbuffered_withPointerInputAndRawMotionEventAccess() {
        lateinit var density: Density
        var framesDuringMotionEventInjection = 0
        val iterations = 1000
        val frameIndexToPointerEvents = mutableMapOf<Int, MutableList<PointerEvent>>()
        frameIndexToPointerEvents[framesDuringMotionEventInjection] = mutableListOf()

        rule.setContent {
            density = LocalDensity.current
            val containingView = LocalView.current

            Spacer(
                modifier =
                    Modifier.fillMaxSize().testTag("PointerInput").pointerInput(Unit) {
                        awaitEachGesture {
                            var isFirstPointerEvent = true
                            while (true) {
                                val pointerEvent = awaitPointerEvent()

                                if (isFirstPointerEvent) {
                                    containingView.requestUnbufferedDispatch(
                                        pointerEvent.motionEvent
                                    )
                                    isFirstPointerEvent = false
                                }

                                if (pointerEvent.type == PointerEventType.Move) {
                                    frameIndexToPointerEvents
                                        .getValue(framesDuringMotionEventInjection)
                                        .add(pointerEvent)
                                }
                            }
                        }
                    }
            )
        }

        rule.waitForIdle()

        val pointerInputCenterCoordinates: Offset =
            rule.onNodeWithTag("PointerInput").fetchSemanticsNode().let { node ->
                node.positionOnScreen + node.size.center.toOffset()
            }
        val radius = with(density) { 16.dp.toPx() }

        val frameCallback =
            object : Choreographer.FrameCallback {
                override fun doFrame(frameTimeNanos: Long) {
                    framesDuringMotionEventInjection++
                    frameIndexToPointerEvents[framesDuringMotionEventInjection] = mutableListOf()
                    dispatcher.scheduler.runCurrent()
                    Choreographer.getInstance().postFrameCallback(this)
                }
            }

        rule.runOnUiThread { Choreographer.getInstance().postFrameCallback(frameCallback) }

        rule.injectCircleTraceEvents(pointerInputCenterCoordinates, radius, iterations)

        rule.runOnUiThread { Choreographer.getInstance().removeFrameCallback(frameCallback) }

        assertTrue(
            frameIndexToPointerEvents.values.maxOf { it.size } > 1,
            "Expected to see more than one pointer event per frame at some point",
        )
        assertTrue(
            frameIndexToPointerEvents.values.maxOf { it.size } <
                (iterations * 4f / framesDuringMotionEventInjection),
            "Expected to see no more than four times the fair share of pointer events in any given frame",
        )
        assertEquals(
            iterations,
            frameIndexToPointerEvents.values.sumOf { it.size },
            "Expected to receive pointer events for the $iterations moves, " +
                "but didn't see the correct amount.",
        )
    }
}

/**
 * Uses [Instrumentation] to inject a gesture that is a down, followed by [iterations] moves around
 * in a circle, and then an up. All motion events are emitted as fast as possible so that buffering
 * would apply in normal use.
 */
private fun ComposeTestRule.injectCircleTraceEvents(
    center: Offset,
    radius: Float,
    iterations: Int,
) {
    val downTime = SystemClock.uptimeMillis()

    val instrumentation = InstrumentationRegistry.getInstrumentation()

    instrumentation.sendPointer(
        downTime = downTime,
        action = MotionEvent.ACTION_DOWN,
        screenOffset = getCoordinatesAroundCircle(center = center, radius = radius, angle = 0f),
        sync = true,
        eventTime = SystemClock.uptimeMillis(),
    )
    waitForIdle()
    repeat(iterations) { index ->
        // We're using Thread.sleep(1) to ensure that each injected move has a different event time
        // Note that this is sleeping for 1 millisecond, not 1 second.
        @Suppress("BanThreadSleep") Thread.sleep(1)
        instrumentation.sendPointer(
            downTime = downTime,
            action = MotionEvent.ACTION_MOVE,
            screenOffset =
                getCoordinatesAroundCircle(
                    center = center,
                    radius = radius,
                    angle = lerp(0f, 2f * PI.toFloat(), (index + 1).toFloat() / iterations),
                ),
            sync = false,
            eventTime = SystemClock.uptimeMillis(),
        )
    }
    waitForIdle()
    instrumentation.sendPointer(
        downTime = downTime,
        action = MotionEvent.ACTION_UP,
        screenOffset = getCoordinatesAroundCircle(center = center, radius = radius, angle = 0f),
        sync = true,
        eventTime = SystemClock.uptimeMillis(),
    )
}

private fun getCoordinatesAroundCircle(center: Offset, radius: Float, angle: Float) =
    center + Offset(cos(angle), sin(angle)) * radius

private fun Instrumentation.sendPointer(
    downTime: Long,
    action: Int,
    screenOffset: Offset,
    sync: Boolean,
    eventTime: Long,
) {
    MotionEvent.obtain(downTime, eventTime, action, screenOffset.x, screenOffset.y, 0).run {
        source = InputDevice.SOURCE_STYLUS
        uiAutomation.injectInputEvent(this, sync)
        recycle()
    }
}
