/*
 * Copyright 2019 The Android Open Source Project
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

import android.content.Context
import android.os.Build
import android.view.InputDevice
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_BUTTON_PRESS
import android.view.MotionEvent.ACTION_BUTTON_RELEASE
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_HOVER_ENTER
import android.view.MotionEvent.ACTION_HOVER_EXIT
import android.view.MotionEvent.ACTION_HOVER_MOVE
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_OUTSIDE
import android.view.MotionEvent.ACTION_POINTER_DOWN
import android.view.MotionEvent.ACTION_POINTER_INDEX_SHIFT
import android.view.MotionEvent.ACTION_POINTER_UP
import android.view.MotionEvent.ACTION_SCROLL
import android.view.MotionEvent.ACTION_UP
import android.view.MotionEvent.PointerCoords
import android.view.MotionEvent.TOOL_TYPE_FINGER
import android.view.MotionEvent.TOOL_TYPE_MOUSE
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.OpenComposeView
import androidx.compose.ui.background
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.findAndroidComposeView
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.PointerCoords
import androidx.compose.ui.gesture.PointerProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.testutils.waitForFutureFrame
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify

@SmallTest
@RunWith(AndroidJUnit4::class)
class AndroidPointerInputTest {
    @Suppress("DEPRECATION")
    @get:Rule
    val rule = androidx.test.rule.ActivityTestRule(AndroidPointerInputTestActivity::class.java)

    private lateinit var container: OpenComposeView

    @Before
    fun setup() {
        val activity = rule.activity
        container = spy(OpenComposeView(activity))

        rule.runOnUiThread {
            activity.setContentView(
                container,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
    }

    @Test
    fun dispatchTouchEvent_invalidCoordinates() {
        countDown { latch ->
            rule.runOnUiThread {
                container.setContent {
                    FillLayout(
                        Modifier.consumeMovementGestureFilter().onGloballyPositioned {
                            latch.countDown()
                        }
                    )
                }
            }
        }

        rule.runOnUiThread {
            val motionEvent =
                MotionEvent(
                    0,
                    ACTION_DOWN,
                    1,
                    0,
                    arrayOf(PointerProperties(0)),
                    arrayOf(PointerCoords(Float.NaN, Float.NaN))
                )

            val androidComposeView = findAndroidComposeView(container)!!
            // Act
            val actual = androidComposeView.dispatchTouchEvent(motionEvent)

            // Assert
            assertThat(actual).isFalse()
        }
    }

    @Test
    fun dispatchTouchEvent_infiniteCoordinates() {
        countDown { latch ->
            rule.runOnUiThread {
                container.setContent {
                    FillLayout(
                        Modifier.consumeMovementGestureFilter().onGloballyPositioned {
                            latch.countDown()
                        }
                    )
                }
            }
        }

        rule.runOnUiThread {
            val motionEvent =
                MotionEvent(
                    0,
                    ACTION_DOWN,
                    1,
                    0,
                    arrayOf(PointerProperties(0)),
                    arrayOf(PointerCoords(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY))
                )

            val androidComposeView = findAndroidComposeView(container)!!
            // Act
            val actual = androidComposeView.dispatchTouchEvent(motionEvent)

            // Assert
            assertThat(actual).isFalse()
        }
    }

    @Test
    fun dispatchTouchEvent_noPointerInputModifiers_returnsFalse() {

        // Arrange

        countDown { latch ->
            rule.runOnUiThread {
                container.setContent {
                    FillLayout(Modifier.onGloballyPositioned { latch.countDown() })
                }
            }
        }

        rule.runOnUiThread {
            val motionEvent =
                MotionEvent(
                    0,
                    ACTION_DOWN,
                    1,
                    0,
                    arrayOf(PointerProperties(0)),
                    arrayOf(PointerCoords(0f, 0f))
                )

            // Act
            val actual = findRootView(container).dispatchTouchEvent(motionEvent)

            // Assert
            assertThat(actual).isFalse()
        }
    }

    /**
     * Recreates dispatch of non-system created cancellation [MotionEvent] (that is, developer
     * created) while system is already handling multiple [MotionEvent]s. Due to the platform not
     * allowing reentrancy while handling [MotionEvent]s, the cancellation event will be ignored.
     */
    @Test
    fun dispatchTouchEvents_eventCancelledDuringProcessing_doesNotCancel() {
        // Arrange
        var topBoxInnerCoordinates: LayoutCoordinates? = null
        var bottomBoxInnerCoordinates: LayoutCoordinates? = null

        val latch = CountDownLatch(2)

        val pointerEventsLog = mutableListOf<PointerEvent>()

        rule.runOnUiThread {
            container.setContent {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Top Box
                    Box(
                        modifier =
                            Modifier.size(50.dp)
                                .align(AbsoluteAlignment.TopLeft)
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            event.changes.forEach { it.consume() }
                                            pointerEventsLog += event

                                            // Actual dispatch of non-system created cancellation
                                            // [MotionEvent] while other [MotionEvent]s are being
                                            // handled.
                                            if (event.type == PointerEventType.Move) {
                                                dispatchTouchEvent(
                                                    ACTION_CANCEL,
                                                    topBoxInnerCoordinates!!
                                                )
                                            }
                                        }
                                    }
                                }
                                .onGloballyPositioned {
                                    topBoxInnerCoordinates = it
                                    latch.countDown()
                                }
                    )

                    // Bottom Box
                    Box(
                        modifier =
                            Modifier.size(60.dp)
                                .align(AbsoluteAlignment.BottomRight)
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            event.changes.forEach { it.consume() }
                                            pointerEventsLog += event
                                        }
                                    }
                                }
                                .onGloballyPositioned {
                                    bottomBoxInnerCoordinates = it
                                    latch.countDown()
                                }
                    )
                }
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))

        rule.runOnUiThread {
            // Arrange continued
            val root = topBoxInnerCoordinates!!.findRootCoordinates()
            val topBoxOffset = root.localPositionOf(topBoxInnerCoordinates!!, Offset.Zero)
            val bottomBoxOffset = root.localPositionOf(bottomBoxInnerCoordinates!!, Offset.Zero)

            val topBoxFingerPointerPropertiesId = 0
            val bottomBoxFingerPointerPropertiesId = 1

            val topBoxPointerProperties =
                PointerProperties(topBoxFingerPointerPropertiesId).also {
                    it.toolType = MotionEvent.TOOL_TYPE_FINGER
                }
            val bottomBoxPointerProperties =
                PointerProperties(bottomBoxFingerPointerPropertiesId).also {
                    it.toolType = MotionEvent.TOOL_TYPE_FINGER
                }

            var eventStartTime = 0

            val downTopBoxEvent =
                MotionEvent(
                    eventStartTime,
                    action = ACTION_DOWN,
                    numPointers = 1,
                    actionIndex = 0,
                    pointerProperties = arrayOf(topBoxPointerProperties),
                    pointerCoords = arrayOf(PointerCoords(topBoxOffset.x, topBoxOffset.y))
                )

            eventStartTime += 500
            val downBottomBoxEvent =
                MotionEvent(
                    eventStartTime,
                    action = ACTION_POINTER_DOWN,
                    numPointers = 2,
                    actionIndex = 1,
                    pointerProperties =
                        arrayOf(topBoxPointerProperties, bottomBoxPointerProperties),
                    pointerCoords =
                        arrayOf(
                            PointerCoords(topBoxOffset.x, topBoxOffset.y),
                            PointerCoords(bottomBoxOffset.x, bottomBoxOffset.y)
                        )
                )

            eventStartTime += 500
            val moveTopBoxEvent =
                MotionEvent(
                    eventStartTime,
                    action = ACTION_MOVE,
                    numPointers = 2,
                    actionIndex = 0,
                    pointerProperties =
                        arrayOf(topBoxPointerProperties, bottomBoxPointerProperties),
                    pointerCoords =
                        arrayOf(
                            PointerCoords(topBoxOffset.x + 10, topBoxOffset.y),
                            PointerCoords(bottomBoxOffset.x + 10, bottomBoxOffset.y)
                        )
                )

            eventStartTime += 500
            val moveBottomBoxEvent =
                MotionEvent(
                    eventStartTime,
                    action = ACTION_MOVE,
                    numPointers = 2,
                    actionIndex = 1,
                    pointerProperties =
                        arrayOf(topBoxPointerProperties, bottomBoxPointerProperties),
                    pointerCoords =
                        arrayOf(
                            PointerCoords(topBoxOffset.x + 10, topBoxOffset.y),
                            PointerCoords(bottomBoxOffset.x + 10, bottomBoxOffset.y)
                        )
                )

            eventStartTime += 500
            val upTopBoxEvent =
                MotionEvent(
                    eventStartTime,
                    action = ACTION_POINTER_UP,
                    numPointers = 2,
                    actionIndex = 0,
                    pointerProperties =
                        arrayOf(topBoxPointerProperties, bottomBoxPointerProperties),
                    pointerCoords =
                        arrayOf(
                            PointerCoords(topBoxOffset.x + 10, topBoxOffset.y),
                            PointerCoords(bottomBoxOffset.x + 10, bottomBoxOffset.y)
                        )
                )

            eventStartTime += 500
            val upBottomBoxEvent =
                MotionEvent(
                    eventStartTime,
                    action = ACTION_UP,
                    numPointers = 1,
                    actionIndex = 0,
                    pointerProperties = arrayOf(bottomBoxPointerProperties),
                    pointerCoords =
                        arrayOf(PointerCoords(bottomBoxOffset.x + 10, bottomBoxOffset.y))
                )

            // Act
            val androidComposeView = findAndroidComposeView(container) as AndroidComposeView

            androidComposeView.dispatchTouchEvent(downTopBoxEvent)
            androidComposeView.dispatchTouchEvent(downBottomBoxEvent)
            androidComposeView.dispatchTouchEvent(moveTopBoxEvent)
            androidComposeView.dispatchTouchEvent(moveBottomBoxEvent)
            androidComposeView.dispatchTouchEvent(upTopBoxEvent)
            androidComposeView.dispatchTouchEvent(upBottomBoxEvent)

            // Assert
            assertThat(pointerEventsLog).hasSize(8)

            for (pointerEvent in pointerEventsLog) {
                assertThat(pointerEvent.internalPointerEvent).isNotNull()
            }

            assertThat(pointerEventsLog[0].type).isEqualTo(PointerEventType.Press)
            assertThat(pointerEventsLog[1].type).isEqualTo(PointerEventType.Press)
            assertThat(pointerEventsLog[2].type).isEqualTo(PointerEventType.Press)

            assertThat(pointerEventsLog[3].type).isEqualTo(PointerEventType.Move)
            assertThat(pointerEventsLog[4].type).isEqualTo(PointerEventType.Move)

            assertThat(pointerEventsLog[5].type).isEqualTo(PointerEventType.Release)
            assertThat(pointerEventsLog[6].type).isEqualTo(PointerEventType.Release)
            assertThat(pointerEventsLog[7].type).isEqualTo(PointerEventType.Release)
        }
    }

    @Test
    fun dispatchTouchEvent_pointerInputModifier_returnsTrue() {

        // Arrange

        countDown { latch ->
            rule.runOnUiThread {
                container.setContent {
                    FillLayout(
                        Modifier.consumeMovementGestureFilter().onGloballyPositioned {
                            latch.countDown()
                        }
                    )
                }
            }
        }

        rule.runOnUiThread {
            val locationInWindow = IntArray(2).also { container.getLocationInWindow(it) }

            val motionEvent =
                MotionEvent(
                    0,
                    ACTION_DOWN,
                    1,
                    0,
                    arrayOf(PointerProperties(0)),
                    arrayOf(
                        PointerCoords(locationInWindow[0].toFloat(), locationInWindow[1].toFloat())
                    )
                )

            // Act
            val actual = findRootView(container).dispatchTouchEvent(motionEvent)

            // Assert
            assertThat(actual).isTrue()
        }
    }

    @Test
    fun dispatchTouchEvent_movementNotConsumed_requestDisallowInterceptTouchEventNotCalled() {
        dispatchTouchEvent_movementConsumptionInCompose(
            consumeMovement = false,
            callsRequestDisallowInterceptTouchEvent = false
        )
    }

    @Test
    fun dispatchTouchEvent_movementConsumed_requestDisallowInterceptTouchEventCalled() {
        dispatchTouchEvent_movementConsumptionInCompose(
            consumeMovement = true,
            callsRequestDisallowInterceptTouchEvent = true
        )
    }

    @Test
    fun dispatchTouchEvent_notMeasuredLayoutsAreMeasuredFirst() {
        val size = mutableStateOf(10)
        val latch = CountDownLatch(1)
        var consumedDownPosition: Offset? = null
        rule.runOnUiThread {
            container.setContent {
                Box(Modifier.fillMaxSize().wrapContentSize(align = AbsoluteAlignment.TopLeft)) {
                    Layout(
                        {},
                        Modifier.consumeDownGestureFilter { consumedDownPosition = it }
                            .onGloballyPositioned { latch.countDown() }
                    ) { _, _ ->
                        val sizePx = size.value
                        layout(sizePx, sizePx) {}
                    }
                }
            }
        }

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue()

        rule.runOnUiThread {
            // we update size from 10 to 20 pixels
            size.value = 20
            // this call will synchronously mark the LayoutNode as needs remeasure
            Snapshot.sendApplyNotifications()
            val locationInWindow = IntArray(2).also { container.getLocationInWindow(it) }

            val motionEvent =
                MotionEvent(
                    0,
                    ACTION_DOWN,
                    1,
                    0,
                    arrayOf(PointerProperties(0)),
                    arrayOf(PointerCoords(locationInWindow[0] + 15f, locationInWindow[1] + 15f))
                )

            // we expect it to first remeasure and only then process
            findRootView(container).dispatchTouchEvent(motionEvent)

            assertThat(consumedDownPosition).isEqualTo(Offset(15f, 15f))
        }
    }

    @Test
    fun dispatchTouchEvent_throughLayersOfAndroidAndCompose_hitsChildWithCorrectCoords() {

        // Arrange

        val context = rule.activity

        val log = mutableListOf<List<PointerInputChange>>()

        countDown { latch ->
            rule.runOnUiThread {
                container.setContent {
                    AndroidWithCompose(context, 1) {
                        AndroidWithCompose(context, 10) {
                            AndroidWithCompose(context, 100) {
                                Layout(
                                    {},
                                    Modifier.logEventsGestureFilter(log).onGloballyPositioned {
                                        latch.countDown()
                                    }
                                ) { _, _ ->
                                    layout(5, 5) {}
                                }
                            }
                        }
                    }
                }
            }
        }

        rule.runOnUiThread {
            val locationInWindow = IntArray(2).also { container.getLocationInWindow(it) }

            val motionEvent =
                MotionEvent(
                    0,
                    ACTION_DOWN,
                    1,
                    0,
                    arrayOf(PointerProperties(0)),
                    arrayOf(
                        PointerCoords(
                            locationInWindow[0].toFloat() + 1 + 10 + 100,
                            locationInWindow[1].toFloat() + 1 + 10 + 100
                        )
                    )
                )

            // Act
            findRootView(container).dispatchTouchEvent(motionEvent)

            // Assert
            assertThat(log).hasSize(1)
            assertThat(log[0]).hasSize(1)
            assertThat(log[0][0].position).isEqualTo(Offset(0f, 0f))
        }
    }

    private fun dispatchTouchEvent_movementConsumptionInCompose(
        consumeMovement: Boolean,
        callsRequestDisallowInterceptTouchEvent: Boolean
    ) {

        // Arrange

        countDown { latch ->
            rule.runOnUiThread {
                container.setContent {
                    FillLayout(
                        Modifier.consumeMovementGestureFilter(consumeMovement)
                            .onGloballyPositioned { latch.countDown() }
                    )
                }
            }
        }

        rule.runOnUiThread {
            val (x, y) =
                IntArray(2).let { array ->
                    container.getLocationInWindow(array)
                    array.map { item -> item.toFloat() }
                }

            val down =
                MotionEvent(
                    0,
                    ACTION_DOWN,
                    1,
                    0,
                    arrayOf(PointerProperties(0)),
                    arrayOf(PointerCoords(x, y))
                )

            val move =
                MotionEvent(
                    0,
                    ACTION_MOVE,
                    1,
                    0,
                    arrayOf(PointerProperties(0)),
                    arrayOf(PointerCoords(x + 1, y))
                )

            findRootView(container).dispatchTouchEvent(down)

            // Act
            findRootView(container).dispatchTouchEvent(move)

            // Assert
            if (callsRequestDisallowInterceptTouchEvent) {
                verify(container).requestDisallowInterceptTouchEvent(true)
            } else {
                verify(container, never()).requestDisallowInterceptTouchEvent(any())
            }
        }
    }

    /**
     * This test verifies that if the AndroidComposeView is offset directly by a call to
     * "offsetTopAndBottom(int)", that pointer locations are correct when dispatched down to a child
     * PointerInputModifier.
     */
    @Test
    fun dispatchTouchEvent_androidComposeViewOffset_positionIsCorrect() {

        // Arrange

        val offset = 50
        val log = mutableListOf<List<PointerInputChange>>()

        countDown { latch ->
            rule.runOnUiThread {
                container.setContent {
                    FillLayout(
                        Modifier.logEventsGestureFilter(log).onGloballyPositioned {
                            latch.countDown()
                        }
                    )
                }
            }
        }

        rule.runOnUiThread {
            // Get the current location in window.
            val locationInWindow = IntArray(2).also { container.getLocationInWindow(it) }

            // Offset the androidComposeView.
            container.offsetTopAndBottom(offset)

            // Create a motion event that is also offset.
            val motionEvent =
                MotionEvent(
                    0,
                    ACTION_DOWN,
                    1,
                    0,
                    arrayOf(PointerProperties(0)),
                    arrayOf(
                        PointerCoords(
                            locationInWindow[0].toFloat(),
                            locationInWindow[1].toFloat() + offset
                        )
                    )
                )

            // Act
            findRootView(container).dispatchTouchEvent(motionEvent)

            // Assert
            assertThat(log).hasSize(1)
            assertThat(log[0]).hasSize(1)
            assertThat(log[0][0].position).isEqualTo(Offset(0f, 0f))
        }
    }

    /*
     * Tests that a long press is NOT triggered when an up event (following a down event) isn't
     * executed right away because the UI thread is delayed past the long press timeout.
     *
     * Note: This test is a bit complicated because it needs to properly execute events in order
     * using multiple coroutine delay()s and Thread.sleep() in the main thread.
     *
     * Expected behavior: When the UI thread wakes up, the up event should be triggered before the
     * second delay() in the withTimeout() (which foundation's long press uses). Thus, the tap
     * triggers and NOT the long press.
     *
     * Actual steps this test uses to recreate this scenario:
     *   1. Down event is triggered
     *   2. An up event is scheduled to be triggered BEFORE the timeout for a long press (uses
     *       a coroutine sleep() that is less than the long press timeout).
     *   3. The UI thread sleeps before the sleep() awakens to fire the up event (the sleep time is
     *       LONGER than the long press timeout).
     *   4. The UI thread wakes up, executes the first sleep() for the long press timeout
     *       (in withTimeout() implementation [`SuspendingPointerInputModifierNodeImpl`])
     *   5. The up event is fired (sleep() for test coroutine finishes).
     *   6. Tap is triggered (that is, long press is NOT triggered because the second sleep() is
     *       NOT executed in withTimeout()).
     */
    @Test
    fun detectTapGestures_blockedMainThread() {
        var didLongPress = false
        var didTap = false

        val positionedLatch = CountDownLatch(1)
        var pressLatch = CountDownLatch(1)
        var clickOrLongPressLatch = CountDownLatch(1)

        lateinit var coroutineScope: CoroutineScope

        val locationInWindow = IntArray(2)

        // Less than long press timeout
        val touchUpDelay = 100
        // 400L
        val longPressTimeout = android.view.ViewConfiguration.getLongPressTimeout()
        // Goes past long press timeout (above)
        val sleepTime = longPressTimeout + 100L
        // matches first delay time in [PointerEventHandlerCoroutine.withTimeout()]
        val withTimeoutDelay = longPressTimeout - WITH_TIMEOUT_MICRO_DELAY_MILLIS
        var upEvent: MotionEvent? = null

        rule.runOnUiThread {
            container.setContent {
                coroutineScope = rememberCoroutineScope()

                FillLayout(
                    Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {
                                    didLongPress = true
                                    clickOrLongPressLatch.countDown()
                                },
                                onTap = {
                                    didTap = true
                                    clickOrLongPressLatch.countDown()
                                },
                                onPress = {
                                    // AwaitPointerEventScope.waitForLongPress() uses
                                    // PointerEventHandlerCoroutine.withTimeout() as part of
                                    // the timeout logic to see if a long press has occurred.
                                    //
                                    // Within PointerEventHandlerCoroutine.withTimeout(), there
                                    // is a coroutine with two delay() calls and we are
                                    // specifically testing that an up event that is put to
                                    // sleep (but within the timeout time), does not trigger a
                                    // long press when it comes in between those delay() calls.
                                    //
                                    // To do that, we want to get the timing of this coroutine
                                    // as close to timeout as possible. That is, executing the
                                    // up event (after the delay below) right between those
                                    // delays to avoid the test being flaky.
                                    coroutineScope.launch {
                                        // Matches first delay used with withTimeout() for long
                                        // press.
                                        delay(withTimeoutDelay)
                                        findRootView(container).dispatchTouchEvent(upEvent!!)
                                    }
                                    pressLatch.countDown()
                                }
                            )
                        }
                        .onGloballyPositioned { positionedLatch.countDown() }
                )
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))
        container.getLocationInWindow(locationInWindow)

        repeat(5) { iteration ->
            rule.runOnUiThread {
                val downEvent =
                    createPointerEventAt(
                        iteration * sleepTime.toInt(),
                        ACTION_DOWN,
                        locationInWindow
                    )

                upEvent =
                    createPointerEventAt(
                        touchUpDelay + iteration * sleepTime.toInt(),
                        ACTION_UP,
                        locationInWindow
                    )
                findRootView(container).dispatchTouchEvent(downEvent)
            }

            assertTrue(pressLatch.await(1, TimeUnit.SECONDS))

            // Blocks the UI thread from now until past the long-press
            // timeout. This tests that even in pathological situations,
            // the upEvent is still processed before the long-press
            // timeout.
            rule.runOnUiThread { Thread.sleep(sleepTime) }

            assertTrue(clickOrLongPressLatch.await(1, TimeUnit.SECONDS))

            assertFalse(didLongPress)
            assertTrue(didTap)

            didTap = false
            clickOrLongPressLatch = CountDownLatch(1)
            pressLatch = CountDownLatch(1)
        }
    }

    /**
     * When a modifier is added, it should work, even when it takes the position of a previous
     * modifier.
     */
    @Test
    fun recomposeWithNewModifier() {
        var tap2Enabled by mutableStateOf(false)
        var tapLatch = CountDownLatch(1)
        val tapLatch2 = CountDownLatch(1)
        var positionedLatch = CountDownLatch(1)

        rule.runOnUiThread {
            container.setContent {
                FillLayout(
                    Modifier.pointerInput(Unit) { detectTapGestures { tapLatch.countDown() } }
                        .then(
                            if (tap2Enabled)
                                Modifier.pointerInput(Unit) {
                                    detectTapGestures { tapLatch2.countDown() }
                                }
                            else Modifier
                        )
                        .onGloballyPositioned { positionedLatch.countDown() }
                )
            }
        }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        val locationInWindow = IntArray(2)
        rule.runOnUiThread {
            // Get the current location in window.
            container.getLocationInWindow(locationInWindow)

            val downEvent = createPointerEventAt(0, ACTION_DOWN, locationInWindow)
            findRootView(container).dispatchTouchEvent(downEvent)
        }

        rule.runOnUiThread {
            val upEvent = createPointerEventAt(200, ACTION_UP, locationInWindow)
            findRootView(container).dispatchTouchEvent(upEvent)
        }

        assertTrue(tapLatch.await(1, TimeUnit.SECONDS))
        tapLatch = CountDownLatch(1)

        positionedLatch = CountDownLatch(1)
        tap2Enabled = true
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        rule.runOnUiThread {
            val downEvent = createPointerEventAt(1000, ACTION_DOWN, locationInWindow)
            findRootView(container).dispatchTouchEvent(downEvent)
        }
        // Need to wait for long press timeout (at least)
        rule.runOnUiThread {
            val upEvent = createPointerEventAt(1030, ACTION_UP, locationInWindow)
            findRootView(container).dispatchTouchEvent(upEvent)
        }
        assertTrue(tapLatch2.await(1, TimeUnit.SECONDS))

        positionedLatch = CountDownLatch(1)
        tap2Enabled = false
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        rule.runOnUiThread {
            val downEvent = createPointerEventAt(2000, ACTION_DOWN, locationInWindow)
            findRootView(container).dispatchTouchEvent(downEvent)
        }
        rule.runOnUiThread {
            val upEvent = createPointerEventAt(2200, ACTION_UP, locationInWindow)
            findRootView(container).dispatchTouchEvent(upEvent)
        }
        assertTrue(tapLatch.await(1, TimeUnit.SECONDS))
    }

    /**
     * There are times that getLocationOnScreen() returns (0, 0). Touch input should still arrive at
     * the correct place even if getLocationOnScreen() gives a different result than the rawX, rawY
     * indicate.
     */
    @Test
    fun badGetLocationOnScreen() {
        val tapLatch = CountDownLatch(1)
        val layoutLatch = CountDownLatch(1)
        rule.runOnUiThread {
            container.setContent {
                with(LocalDensity.current) {
                    Box(
                        Modifier.size(250.toDp()).layout { measurable, constraints ->
                            val p = measurable.measure(constraints)
                            layout(p.width, p.height) {
                                p.place(0, 0)
                                layoutLatch.countDown()
                            }
                        }
                    ) {
                        Box(
                            Modifier.align(AbsoluteAlignment.TopLeft)
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        awaitFirstDown()
                                        tapLatch.countDown()
                                    }
                                }
                                .size(10.toDp())
                        )
                    }
                }
            }
        }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
        rule.runOnUiThread {}

        val down = createPointerEventAt(0, ACTION_DOWN, intArrayOf(105, 205))
        down.offsetLocation(-100f, -200f)
        val composeView = findAndroidComposeView(container) as AndroidComposeView
        composeView.dispatchTouchEvent(down)

        assertTrue(tapLatch.await(1, TimeUnit.SECONDS))
    }

    /**
     * When a scale(0, 0) is used, there is no valid inverse matrix. A touch should not reach an
     * item that is scaled to 0.
     */
    @Test
    fun badInverseMatrix() {
        val tapLatch = CountDownLatch(1)
        val layoutLatch = CountDownLatch(1)
        var insideTap = 0
        rule.runOnUiThread {
            container.setContent {
                with(LocalDensity.current) {
                    Box(
                        Modifier.layout { measurable, constraints ->
                                val p = measurable.measure(constraints)
                                layout(p.width, p.height) {
                                    layoutLatch.countDown()
                                    p.place(0, 0)
                                }
                            }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    awaitFirstDown()
                                    tapLatch.countDown()
                                }
                            }
                            .requiredSize(10.toDp())
                            .scale(0f, 0f)
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    awaitFirstDown()
                                    insideTap++
                                }
                            }
                            .requiredSize(10.toDp())
                    )
                }
            }
        }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))
        rule.runOnUiThread {}

        val down = createPointerEventAt(0, ACTION_DOWN, intArrayOf(5, 5))
        val composeView = findAndroidComposeView(container) as AndroidComposeView
        composeView.dispatchTouchEvent(down)

        assertTrue(tapLatch.await(1, TimeUnit.SECONDS))
        rule.runOnUiThread { assertEquals(0, insideTap) }
    }

    @Test
    fun dispatchNotAttached() {
        val tapLatch = CountDownLatch(1)
        val layoutLatch = CountDownLatch(1)
        rule.runOnUiThread {
            container.setContent {
                with(LocalDensity.current) {
                    Box(
                        Modifier.onPlaced { layoutLatch.countDown() }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    awaitFirstDown()
                                    tapLatch.countDown()
                                }
                            }
                            .requiredSize(10.toDp())
                    )
                }
            }
        }
        assertTrue(layoutLatch.await(1, TimeUnit.SECONDS))

        val composeView = findAndroidComposeView(container) as AndroidComposeView
        rule.runOnUiThread {
            container.removeAllViews()
            val down = createPointerEventAt(0, ACTION_DOWN, intArrayOf(5, 5))
            assertFalse(composeView.dispatchTouchEvent(down))
        }
    }

    private fun assertHoverEvent(
        event: PointerEvent,
        isEnter: Boolean = false,
        isExit: Boolean = false
    ) {
        assertThat(event.changes).hasSize(1)
        val change = event.changes[0]
        assertThat(change.pressed).isFalse()
        assertThat(change.previousPressed).isFalse()
        val expectedHoverType =
            when {
                isEnter -> PointerEventType.Enter
                isExit -> PointerEventType.Exit
                else -> PointerEventType.Move
            }
        assertThat(event.type).isEqualTo(expectedHoverType)
    }

    private fun assertScrollEvent(event: PointerEvent, scrollExpected: Offset) {
        assertThat(event.changes).hasSize(1)
        val change = event.changes[0]
        assertThat(change.pressed).isFalse()
        assertThat(event.type).isEqualTo(PointerEventType.Scroll)
        // we agreed to reverse Y in android to be in line with other platforms
        assertThat(change.scrollDelta).isEqualTo(scrollExpected.copy(y = scrollExpected.y * -1))
    }

    private fun dispatchMouseEvent(
        action: Int,
        layoutCoordinates: LayoutCoordinates,
        offset: Offset = Offset.Zero,
        scrollDelta: Offset = Offset.Zero,
        eventTime: Int = 0
    ) {
        rule.runOnUiThread {
            val root = layoutCoordinates.findRootCoordinates()
            val pos = root.localPositionOf(layoutCoordinates, offset)
            val event =
                MotionEvent(
                    eventTime,
                    action,
                    1,
                    0,
                    arrayOf(PointerProperties(0).also { it.toolType = TOOL_TYPE_MOUSE }),
                    arrayOf(PointerCoords(pos.x, pos.y, scrollDelta.x, scrollDelta.y))
                )

            val androidComposeView = findAndroidComposeView(container) as AndroidComposeView
            when (action) {
                ACTION_HOVER_ENTER,
                ACTION_HOVER_MOVE,
                ACTION_HOVER_EXIT -> androidComposeView.dispatchHoverEvent(event)
                ACTION_SCROLL -> androidComposeView.dispatchGenericMotionEvent(event)
                else -> androidComposeView.dispatchTouchEvent(event)
            }
        }
    }

    private fun dispatchStylusEvents(
        layoutCoordinates: LayoutCoordinates,
        offset: Offset,
        vararg actions: Int
    ) {
        rule.runOnUiThread {
            val root = layoutCoordinates.findRootCoordinates()
            val pos = root.localPositionOf(layoutCoordinates, offset)
            val androidComposeView = findAndroidComposeView(container) as AndroidComposeView

            for (action in actions) {
                val event =
                    MotionEvent(
                        0,
                        action,
                        1,
                        0,
                        arrayOf(
                            PointerProperties(0).also { it.toolType = MotionEvent.TOOL_TYPE_STYLUS }
                        ),
                        arrayOf(PointerCoords(pos.x, pos.y))
                    )

                when (action) {
                    ACTION_HOVER_ENTER,
                    ACTION_HOVER_MOVE,
                    ACTION_HOVER_EXIT -> androidComposeView.dispatchHoverEvent(event)
                    else -> androidComposeView.dispatchTouchEvent(event)
                }
            }
        }
    }

    private fun dispatchTouchEvent(
        action: Int,
        layoutCoordinates: LayoutCoordinates,
        offset: Offset = Offset.Zero,
        eventTime: Int = 0
    ) {
        rule.runOnUiThread {
            val root = layoutCoordinates.findRootCoordinates()
            val pos = root.localPositionOf(layoutCoordinates, offset)
            val event =
                MotionEvent(
                    eventTime,
                    action,
                    1,
                    0,
                    arrayOf(PointerProperties(0).also { it.toolType = TOOL_TYPE_FINGER }),
                    arrayOf(PointerCoords(pos.x, pos.y))
                )

            val androidComposeView = findAndroidComposeView(container) as AndroidComposeView
            androidComposeView.dispatchTouchEvent(event)
        }
    }

    @Test
    fun dispatchHoverEnter() {
        var layoutCoordinates: LayoutCoordinates? = null
        val latch = CountDownLatch(1)
        val events = mutableListOf<PointerEvent>()
        rule.runOnUiThread {
            container.setContent {
                Box(
                    Modifier.fillMaxSize()
                        .onGloballyPositioned {
                            layoutCoordinates = it
                            latch.countDown()
                        }
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    event.changes[0].consume()
                                    events += event
                                }
                            }
                        }
                )
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        dispatchMouseEvent(ACTION_HOVER_ENTER, layoutCoordinates!!)
        rule.runOnUiThread {
            assertThat(events).hasSize(1)
            assertHoverEvent(events[0], isEnter = true)
        }
    }

    @Test
    fun dispatchHoverExit() {
        var layoutCoordinates: LayoutCoordinates? = null
        val latch = CountDownLatch(1)
        val events = mutableListOf<PointerEvent>()
        rule.runOnUiThread {
            container.setContent {
                Box(
                    Modifier.fillMaxSize()
                        .onGloballyPositioned {
                            layoutCoordinates = it
                            latch.countDown()
                        }
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    event.changes[0].consume()
                                    events += event
                                }
                            }
                        }
                )
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        dispatchMouseEvent(ACTION_HOVER_ENTER, layoutCoordinates!!)
        dispatchMouseEvent(ACTION_HOVER_EXIT, layoutCoordinates!!, Offset(-1f, -1f))

        rule.runOnUiThread {
            assertThat(events).hasSize(2)
            assertHoverEvent(events[0], isEnter = true)
            assertHoverEvent(events[1], isExit = true)
        }
    }

    /*
     * Simple test that makes sure a bad ACTION_OUTSIDE MotionEvent doesn't negatively
     * impact Compose (b/299074463#comment31). (We actually ignore them in Compose.)
     * The event order of MotionEvents:
     *   1. Hover enter on box 1
     *   2. Hover move into box 2
     *   3. Hover exit on box 2
     *   4. Outside event on box 3
     *   5. Down on box 2
     *   6. Up on box 2
     */
    @Test
    fun hoverAndClickMotionEvent_badOutsideMotionEvent_outsideMotionEventIgnored() {
        // --> Arrange
        var box1LayoutCoordinates: LayoutCoordinates? = null
        var box2LayoutCoordinates: LayoutCoordinates? = null
        var box3LayoutCoordinates: LayoutCoordinates? = null

        val setUpFinishedLatch = CountDownLatch(4)
        // One less than total because outside is not sent to Compose.
        val totalEventLatch = CountDownLatch(5)

        // Events for Box 1
        var enterBox1 = false
        var exitBox1 = false

        // Events for Box 2
        var enterBox2 = false
        var exitBox2 = false
        var pressBox2 = false
        var releaseBox2 = false

        // All other events that should never be triggered in this test
        var eventsThatShouldNotTrigger = false

        var pointerEvent: PointerEvent? = null

        rule.runOnUiThread {
            container.setContent {
                Column(
                    Modifier.fillMaxSize()
                        .onGloballyPositioned { setUpFinishedLatch.countDown() }
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent()
                                    totalEventLatch.countDown()
                                }
                            }
                        }
                ) {
                    // Box 1
                    Box(
                        Modifier.size(50.dp)
                            .onGloballyPositioned {
                                box1LayoutCoordinates = it
                                setUpFinishedLatch.countDown()
                            }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        pointerEvent = awaitPointerEvent()

                                        when (pointerEvent!!.type) {
                                            PointerEventType.Enter -> {
                                                enterBox1 = true
                                            }
                                            PointerEventType.Exit -> {
                                                exitBox1 = true
                                            }
                                            else -> {
                                                eventsThatShouldNotTrigger = true
                                            }
                                        }
                                    }
                                }
                            }
                    ) {}

                    // Box 2
                    Box(
                        Modifier.size(50.dp)
                            .onGloballyPositioned {
                                box2LayoutCoordinates = it
                                setUpFinishedLatch.countDown()
                            }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        pointerEvent = awaitPointerEvent()

                                        when (pointerEvent!!.type) {
                                            PointerEventType.Enter -> {
                                                enterBox2 = true
                                            }
                                            PointerEventType.Press -> {
                                                pressBox2 = true
                                            }
                                            PointerEventType.Release -> {
                                                releaseBox2 = true
                                            }
                                            PointerEventType.Exit -> {
                                                exitBox2 = true
                                            }
                                            else -> {
                                                eventsThatShouldNotTrigger = true
                                            }
                                        }
                                    }
                                }
                            }
                    ) {}

                    // Box 3
                    Box(
                        Modifier.size(50.dp)
                            .onGloballyPositioned {
                                box3LayoutCoordinates = it
                                setUpFinishedLatch.countDown()
                            }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        pointerEvent = awaitPointerEvent()
                                        eventsThatShouldNotTrigger = true
                                    }
                                }
                            }
                    ) {}
                }
            }
        }
        // Ensure Arrange (setup) step is finished
        assertTrue(setUpFinishedLatch.await(2, TimeUnit.SECONDS))

        // --> Act + Assert (interwoven)
        // Hover Enter on Box 1
        dispatchMouseEvent(ACTION_HOVER_ENTER, box1LayoutCoordinates!!)
        rule.runOnUiThread {
            assertThat(enterBox1).isTrue()
            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
            assertHoverEvent(pointerEvent!!, isEnter = true)
        }

        // Hover Move to Box 2
        pointerEvent = null // Reset before each event
        dispatchMouseEvent(ACTION_HOVER_MOVE, box2LayoutCoordinates!!)
        rule.runOnUiThread {
            assertThat(exitBox1).isTrue()
            assertThat(enterBox2).isTrue()
            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
            assertHoverEvent(pointerEvent!!, isEnter = true)
        }

        // Hover Exit on Box 2
        pointerEvent = null // Reset before each event
        dispatchMouseEvent(ACTION_HOVER_EXIT, box2LayoutCoordinates!!)

        // Hover exit events in Compose are always delayed two frames to ensure Compose does not
        // trigger them if they are followed by a press in the next frame. This accounts for that.
        rule.waitForFutureFrame(2)

        rule.runOnUiThread {
            assertThat(exitBox2).isTrue()
            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
        }

        // Outside event with Box 3 coordinates
        pointerEvent = null // Reset before each event
        dispatchMouseEvent(ACTION_OUTSIDE, box3LayoutCoordinates!!)

        // No Compose event should be triggered (b/299074463#comment31)
        rule.runOnUiThread {
            assertThat(eventsThatShouldNotTrigger).isFalse()
            assertThat(pointerEvent).isNull()
        }

        // Press on Box 2
        pointerEvent = null // Reset before each event
        dispatchMouseEvent(ACTION_DOWN, box2LayoutCoordinates!!)
        rule.runOnUiThread {
            assertThat(pressBox2).isTrue()
            assertThat(eventsThatShouldNotTrigger).isFalse()
            assertThat(pointerEvent).isNotNull()
        }

        // Release on Box 2
        pointerEvent = null // Reset before each event
        dispatchMouseEvent(ACTION_UP, box2LayoutCoordinates!!)
        rule.runOnUiThread {
            assertThat(releaseBox2).isTrue()
            assertThat(eventsThatShouldNotTrigger).isFalse()
            assertThat(pointerEvent).isNotNull()
        }

        assertTrue(totalEventLatch.await(1, TimeUnit.SECONDS))
    }

    /*
     * Tests that a bad ACTION_HOVER_EXIT MotionEvent is ignored in Compose when it directly
     * proceeds an ACTION_SCROLL MotionEvent. This happens in some versions of Android Studio when
     * mirroring is used (b/314269723).
     *
     * The event order of MotionEvents:
     *   - Hover enter on box 1
     *   - Hover exit on box 1 (bad event)
     *   - Scroll on box 1
     */
    @Test
    fun scrollMotionEvent_proceededImmediatelyByHoverExit_shouldNotTriggerHoverExit() {
        // --> Arrange
        val scrollDelta = Offset(0.35f, 0.65f)
        var box1LayoutCoordinates: LayoutCoordinates? = null

        val setUpFinishedLatch = CountDownLatch(4)

        // Events for Box 1
        var enterBox1 = false
        var scrollBox1 = false

        // All other events that should never be triggered in this test
        var eventsThatShouldNotTrigger = false

        var pointerEvent: PointerEvent? = null

        rule.runOnUiThread {
            container.setContent {
                Column(
                    Modifier.fillMaxSize().onGloballyPositioned { setUpFinishedLatch.countDown() }
                ) {
                    // Box 1
                    Box(
                        Modifier.size(50.dp)
                            .onGloballyPositioned {
                                box1LayoutCoordinates = it
                                setUpFinishedLatch.countDown()
                            }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        pointerEvent = awaitPointerEvent()

                                        when (pointerEvent!!.type) {
                                            PointerEventType.Enter -> {
                                                enterBox1 = true
                                            }
                                            PointerEventType.Exit -> {
                                                enterBox1 = false
                                            }
                                            PointerEventType.Scroll -> {
                                                scrollBox1 = true
                                            }
                                            else -> {
                                                eventsThatShouldNotTrigger = true
                                            }
                                        }
                                    }
                                }
                            }
                    ) {}

                    // Box 2
                    Box(
                        Modifier.size(50.dp)
                            .onGloballyPositioned { setUpFinishedLatch.countDown() }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        pointerEvent = awaitPointerEvent()
                                        // Should never do anything with this UI element.
                                        eventsThatShouldNotTrigger = true
                                    }
                                }
                            }
                    ) {}

                    // Box 3
                    Box(
                        Modifier.size(50.dp)
                            .onGloballyPositioned { setUpFinishedLatch.countDown() }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        pointerEvent = awaitPointerEvent()
                                        // Should never do anything with this UI element.
                                        eventsThatShouldNotTrigger = true
                                    }
                                }
                            }
                    ) {}
                }
            }
        }
        // Ensure Arrange (setup) step is finished
        assertTrue(setUpFinishedLatch.await(2, TimeUnit.SECONDS))

        // --> Act + Assert (interwoven)
        // Hover Enter on Box 1
        dispatchMouseEvent(ACTION_HOVER_ENTER, box1LayoutCoordinates!!)
        rule.runOnUiThread {
            assertThat(enterBox1).isTrue()
            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
            assertHoverEvent(pointerEvent!!, isEnter = true)
        }

        // We do not use dispatchMouseEvent() to dispatch the following two events, because the
        // actions need to be executed in immediate succession
        rule.runOnUiThread {
            val root = box1LayoutCoordinates!!.findRootCoordinates()
            val pos = root.localPositionOf(box1LayoutCoordinates!!, Offset.Zero)

            // Bad hover exit event on Box 1
            val exitMotionEvent =
                MotionEvent(
                    0,
                    ACTION_HOVER_EXIT,
                    1,
                    0,
                    arrayOf(PointerProperties(0).also { it.toolType = TOOL_TYPE_MOUSE }),
                    arrayOf(PointerCoords(pos.x, pos.y, Offset.Zero.x, Offset.Zero.y))
                )

            // Main scroll event on Box 1
            val scrollMotionEvent =
                MotionEvent(
                    0,
                    ACTION_SCROLL,
                    1,
                    0,
                    arrayOf(PointerProperties(0).also { it.toolType = TOOL_TYPE_MOUSE }),
                    arrayOf(PointerCoords(pos.x, pos.y, scrollDelta.x, scrollDelta.y))
                )

            val androidComposeView = findAndroidComposeView(container) as AndroidComposeView
            androidComposeView.dispatchHoverEvent(exitMotionEvent)
            androidComposeView.dispatchGenericMotionEvent(scrollMotionEvent)
        }

        rule.runOnUiThread {
            assertThat(enterBox1).isTrue()
            assertThat(scrollBox1).isTrue()
            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
        }
    }

    /*
     * Tests that all valid combinations of MotionEvent.CLASSIFICATION_* are returned from
     * Compose's [PointerInput].
     * NOTE 1: We do NOT test invalid MotionEvent Classifications, because you can actually pass an
     * invalid classification value to [MotionEvent.obtain()] and it is not rejected. Therefore,
     * to maintain the same behavior, we just return whatever is set in [MotionEvent].
     * NOTE 2: The [MotionEvent.obtain()] that allows you to set classification, is only available
     * in U. (Thus, why this test request at least that version.)
     */
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun motionEventDispatch_withValidClassification_shouldMatchInPointerEvent() {
        // --> Arrange
        var boxLayoutCoordinates: LayoutCoordinates? = null
        val setUpFinishedLatch = CountDownLatch(1)
        var motionEventClassification = MotionEvent.CLASSIFICATION_NONE
        var pointerEvent: PointerEvent? = null

        rule.runOnUiThread {
            container.setContent {
                Box(
                    Modifier.fillMaxSize()
                        .onGloballyPositioned {
                            setUpFinishedLatch.countDown()
                            boxLayoutCoordinates = it
                        }
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    pointerEvent = awaitPointerEvent()
                                }
                            }
                        }
                ) {}
            }
        }

        // Ensure Arrange (setup) step is finished
        assertTrue(setUpFinishedLatch.await(2, TimeUnit.SECONDS))

        // Set up values to be used for creation of all MotionEvents.
        var position: Offset?
        var eventTime = 0
        val numPointers = 1
        val actionIndex = 0
        val pointerProperties =
            arrayOf(PointerProperties(0).also { it.toolType = MotionEvent.TOOL_TYPE_FINGER })
        var pointerCoords: Array<PointerCoords>? = null
        val buttonState = 0

        // --> Act
        rule.runOnUiThread {
            // Set up pointerCoords to be used for the rest of the events
            val root = boxLayoutCoordinates!!.findRootCoordinates()
            position = root.localPositionOf(boxLayoutCoordinates!!, Offset.Zero)
            pointerCoords =
                arrayOf(PointerCoords(position!!.x, position!!.y, Offset.Zero.x, Offset.Zero.y))

            val downEvent =
                MotionEvent(
                    eventTime = eventTime,
                    action = ACTION_DOWN,
                    numPointers = numPointers,
                    actionIndex = actionIndex,
                    pointerProperties = pointerProperties,
                    pointerCoords = pointerCoords!!,
                    buttonState = buttonState,
                    classification = motionEventClassification
                )

            val androidComposeView = findAndroidComposeView(container) as AndroidComposeView
            androidComposeView.dispatchTouchEvent(downEvent)
        }

        // --> Assert
        rule.runOnUiThread {
            assertThat(pointerEvent).isNotNull()
            // This will be MotionEvent.CLASSIFICATION_NONE (set in the beginning).
            assertThat(pointerEvent!!.classification).isEqualTo(motionEventClassification)
        }

        eventTime += 500
        motionEventClassification = MotionEvent.CLASSIFICATION_AMBIGUOUS_GESTURE

        // --> Act
        rule.runOnUiThread {
            val upEvent =
                MotionEvent(
                    eventTime = eventTime,
                    action = ACTION_UP,
                    numPointers = numPointers,
                    actionIndex = actionIndex,
                    pointerProperties = pointerProperties,
                    pointerCoords = pointerCoords!!,
                    buttonState = buttonState,
                    classification = motionEventClassification
                )

            val androidComposeView = findAndroidComposeView(container) as AndroidComposeView
            androidComposeView.dispatchTouchEvent(upEvent)
        }

        // --> Assert
        rule.runOnUiThread {
            assertThat(pointerEvent).isNotNull()
            assertThat(pointerEvent!!.classification).isEqualTo(motionEventClassification)
        }

        eventTime += 500
        motionEventClassification = MotionEvent.CLASSIFICATION_DEEP_PRESS

        // --> Act
        rule.runOnUiThread {
            val downEvent =
                MotionEvent(
                    eventTime = eventTime,
                    action = ACTION_DOWN,
                    numPointers = numPointers,
                    actionIndex = actionIndex,
                    pointerProperties = pointerProperties,
                    pointerCoords = pointerCoords!!,
                    buttonState = buttonState,
                    classification = motionEventClassification
                )

            val androidComposeView = findAndroidComposeView(container) as AndroidComposeView
            androidComposeView.dispatchTouchEvent(downEvent)
        }

        // --> Assert
        rule.runOnUiThread {
            assertThat(pointerEvent).isNotNull()
            assertThat(pointerEvent!!.classification).isEqualTo(motionEventClassification)
        }

        eventTime += 500
        motionEventClassification = MotionEvent.CLASSIFICATION_TWO_FINGER_SWIPE

        // --> Act
        rule.runOnUiThread {
            val upEvent =
                MotionEvent(
                    eventTime = eventTime,
                    action = ACTION_UP,
                    numPointers = numPointers,
                    actionIndex = actionIndex,
                    pointerProperties = pointerProperties,
                    pointerCoords = pointerCoords!!,
                    buttonState = buttonState,
                    classification = motionEventClassification
                )

            val androidComposeView = findAndroidComposeView(container) as AndroidComposeView
            androidComposeView.dispatchTouchEvent(upEvent)
        }

        // --> Assert
        rule.runOnUiThread {
            assertThat(pointerEvent).isNotNull()
            assertThat(pointerEvent!!.classification).isEqualTo(motionEventClassification)
        }

        eventTime += 500
        motionEventClassification = MotionEvent.CLASSIFICATION_PINCH

        // --> Act
        rule.runOnUiThread {
            val downEvent =
                MotionEvent(
                    eventTime = eventTime,
                    action = ACTION_DOWN,
                    numPointers = numPointers,
                    actionIndex = actionIndex,
                    pointerProperties = pointerProperties,
                    pointerCoords = pointerCoords!!,
                    buttonState = buttonState,
                    classification = motionEventClassification
                )

            val androidComposeView = findAndroidComposeView(container) as AndroidComposeView
            androidComposeView.dispatchTouchEvent(downEvent)
        }

        // --> Assert
        rule.runOnUiThread {
            assertThat(pointerEvent).isNotNull()
            assertThat(pointerEvent!!.classification).isEqualTo(motionEventClassification)
        }
    }

    /*
     * Tests that [PointerEvent] without a [MotionEvent] will return a NONE classification.
     */
    @Test
    fun pointerInput_withoutMotionEvent_classificationShouldBeNone() {
        val pointerEventWithoutMotionEvent = PointerEvent(listOf(), internalPointerEvent = null)

        rule.runOnUiThread {
            assertThat(pointerEventWithoutMotionEvent.classification)
                .isEqualTo(MotionEvent.CLASSIFICATION_NONE)
        }
    }

    /*
     * Tests alternating between hover TOUCH events and touch events across multiple UI elements.
     * Specifically, to recreate Talkback events.
     *
     * Important Note: Usually a hover MotionEvent sent from Android has the tool type set as
     * [MotionEvent.TOOL_TYPE_MOUSE]. However, Talkback sets the tool type to
     * [MotionEvent.TOOL_TYPE_FINGER]. We do that in this test by calling the
     * [dispatchTouchEvent()] instead of [dispatchMouseEvent()].
     *
     * Specific events:
     *  1. UI Element 1: ENTER (hover enter [touch])
     *  2. UI Element 1: EXIT (hover exit [touch])
     *  3. UI Element 1: PRESS (touch)
     *  4. UI Element 1: RELEASE (touch)
     *  5. UI Element 2: PRESS (touch)
     *  6. UI Element 2: RELEASE (touch)
     *
     * Should NOT trigger any additional events (like an extra press or exit)!
     */
    @Test
    fun alternatingHoverAndTouch_hoverUi1ToTouchUi1ToTouchUi2_shouldNotTriggerAdditionalEvents() {
        // --> Arrange
        var box1LayoutCoordinates: LayoutCoordinates? = null
        var box2LayoutCoordinates: LayoutCoordinates? = null

        val setUpFinishedLatch = CountDownLatch(4)

        var eventTime = 100

        // Events for Box 1
        var box1HoverEnter = 0
        var box1HoverExit = 0
        var box1Down = 0
        var box1Up = 0

        // Events for Box 2
        var box2Down = 0
        var box2Up = 0

        // All other events that should never be triggered in this test
        var eventsThatShouldNotTrigger = false

        var pointerEvent: PointerEvent? = null

        rule.runOnUiThread {
            container.setContent {
                Column(
                    Modifier.fillMaxSize().onGloballyPositioned { setUpFinishedLatch.countDown() }
                ) {
                    // Box 1
                    Box(
                        Modifier.size(50.dp)
                            .onGloballyPositioned {
                                box1LayoutCoordinates = it
                                setUpFinishedLatch.countDown()
                            }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        pointerEvent = awaitPointerEvent()

                                        when (pointerEvent!!.type) {
                                            PointerEventType.Enter -> {
                                                ++box1HoverEnter
                                            }
                                            PointerEventType.Press -> {
                                                ++box1Down
                                            }
                                            PointerEventType.Release -> {
                                                ++box1Up
                                            }
                                            PointerEventType.Exit -> {
                                                ++box1HoverExit
                                            }
                                            else -> {
                                                eventsThatShouldNotTrigger = true
                                            }
                                        }
                                    }
                                }
                            }
                    ) {}

                    // Box 2
                    Box(
                        Modifier.size(50.dp)
                            .onGloballyPositioned {
                                box2LayoutCoordinates = it
                                setUpFinishedLatch.countDown()
                            }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        pointerEvent = awaitPointerEvent()

                                        when (pointerEvent!!.type) {
                                            PointerEventType.Press -> {
                                                ++box2Down
                                            }
                                            PointerEventType.Release -> {
                                                ++box2Up
                                            }
                                            else -> {
                                                eventsThatShouldNotTrigger = true
                                            }
                                        }
                                    }
                                }
                            }
                    ) {}

                    // Box 3
                    Box(
                        Modifier.size(50.dp)
                            .onGloballyPositioned { setUpFinishedLatch.countDown() }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        pointerEvent = awaitPointerEvent()
                                        // Should never do anything with this UI element.
                                        eventsThatShouldNotTrigger = true
                                    }
                                }
                            }
                    ) {}
                }
            }
        }
        // Ensure Arrange (setup) step is finished
        assertTrue(setUpFinishedLatch.await(2, TimeUnit.SECONDS))

        // --> Act + Assert (interwoven)
        // Hover Enter on Box 1
        dispatchTouchEvent(
            action = ACTION_HOVER_ENTER,
            layoutCoordinates = box1LayoutCoordinates!!,
            eventTime = eventTime
        )
        rule.runOnUiThread {
            // Verify Box 1 events
            assertThat(box1HoverEnter).isEqualTo(1)
            assertThat(box1HoverExit).isEqualTo(0)
            assertThat(box1Down).isEqualTo(0)
            assertThat(box1Up).isEqualTo(0)

            // Verify Box 2 events
            assertThat(box2Down).isEqualTo(0)
            assertThat(box2Up).isEqualTo(0)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
            assertHoverEvent(pointerEvent!!, isEnter = true)
        }

        // Hover Exit on Box 1
        pointerEvent = null // Reset before each event
        eventTime += 100
        dispatchTouchEvent(
            action = ACTION_HOVER_EXIT,
            layoutCoordinates = box1LayoutCoordinates!!,
            eventTime = eventTime
        )

        rule.waitForFutureFrame(2)

        rule.runOnUiThread {
            // Verify Box 1 events
            assertThat(box1HoverEnter).isEqualTo(1)
            assertThat(box1HoverExit).isEqualTo(1)
            assertThat(box1Down).isEqualTo(0)
            assertThat(box1Up).isEqualTo(0)

            // Verify Box 2 events
            assertThat(box2Down).isEqualTo(0)
            assertThat(box2Up).isEqualTo(0)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
        }

        // Press on Box 1
        pointerEvent = null // Reset before each event
        eventTime += 100
        dispatchTouchEvent(
            action = ACTION_DOWN,
            layoutCoordinates = box1LayoutCoordinates!!,
            eventTime = eventTime
        )
        rule.runOnUiThread {
            // Verify Box 1 events
            assertThat(box1HoverEnter).isEqualTo(1)
            assertThat(box1HoverExit).isEqualTo(1)
            assertThat(box1Down).isEqualTo(1)
            assertThat(box1Up).isEqualTo(0)

            // Verify Box 2 events
            assertThat(box2Down).isEqualTo(0)
            assertThat(box2Up).isEqualTo(0)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
        }

        // Release on Box 1
        pointerEvent = null // Reset before each event
        eventTime += 100
        dispatchTouchEvent(
            action = ACTION_UP,
            layoutCoordinates = box1LayoutCoordinates!!,
            eventTime = eventTime
        )
        rule.runOnUiThread {
            // Verify Box 1 events
            assertThat(box1HoverEnter).isEqualTo(1)
            assertThat(box1HoverExit).isEqualTo(1)
            assertThat(box1Down).isEqualTo(1)
            assertThat(box1Up).isEqualTo(1)

            // Verify Box 2 events
            assertThat(box2Down).isEqualTo(0)
            assertThat(box2Up).isEqualTo(0)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
        }

        // Press on Box 2
        pointerEvent = null // Reset before each event
        eventTime += 100
        dispatchTouchEvent(
            action = ACTION_DOWN,
            layoutCoordinates = box2LayoutCoordinates!!,
            eventTime = eventTime
        )
        rule.runOnUiThread {
            // Verify Box 1 events
            assertThat(box1HoverEnter).isEqualTo(1)
            assertThat(box1HoverExit).isEqualTo(1)
            assertThat(box1Down).isEqualTo(1)
            assertThat(box1Up).isEqualTo(1)

            // Verify Box 2 events
            assertThat(box2Down).isEqualTo(1)
            assertThat(box2Up).isEqualTo(0)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
        }

        // Press on Box 2
        pointerEvent = null // Reset before each event
        eventTime += 100
        dispatchTouchEvent(
            action = ACTION_UP,
            layoutCoordinates = box2LayoutCoordinates!!,
            eventTime = eventTime
        )
        rule.runOnUiThread {
            // Verify Box 1 events
            assertThat(box1HoverEnter).isEqualTo(1)
            assertThat(box1HoverExit).isEqualTo(1)
            assertThat(box1Down).isEqualTo(1)
            assertThat(box1Up).isEqualTo(1)

            // Verify Box 2 events
            assertThat(box2Down).isEqualTo(1)
            assertThat(box2Up).isEqualTo(1)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
        }
    }

    /*
     * Tests alternating between hover TOUCH events and regular touch events across multiple
     * UI elements. Specifically, to recreate Talkback events.
     *
     * Important Note: Usually a hover MotionEvent sent from Android has the tool type set as
     * [MotionEvent.TOOL_TYPE_MOUSE]. However, Talkback sets the tool type to
     * [MotionEvent.TOOL_TYPE_FINGER]. We do that in this test by calling the
     * [dispatchTouchEvent()] instead of [dispatchMouseEvent()].
     *
     * Specific events:
     *  1. UI Element 1: ENTER (hover enter [touch])
     *  2. UI Element 1: EXIT (hover exit [touch])
     *  5. UI Element 2: PRESS (touch)
     *  6. UI Element 2: RELEASE (touch)
     *
     * Should NOT trigger any additional events (like an extra exit)!
     */
    @Test
    fun alternatingHoverAndTouch_hoverUi1ToTouchUi2_shouldNotTriggerAdditionalEvents() {
        // --> Arrange
        var box1LayoutCoordinates: LayoutCoordinates? = null
        var box2LayoutCoordinates: LayoutCoordinates? = null

        val setUpFinishedLatch = CountDownLatch(4)

        var eventTime = 100

        // Events for Box 1
        var box1HoverEnter = 0
        var box1HoverExit = 0

        // Events for Box 2
        var box2Down = 0
        var box2Up = 0

        // All other events that should never be triggered in this test
        var eventsThatShouldNotTrigger = false

        var pointerEvent: PointerEvent? = null

        rule.runOnUiThread {
            container.setContent {
                Column(
                    Modifier.fillMaxSize().onGloballyPositioned { setUpFinishedLatch.countDown() }
                ) {
                    // Box 1
                    Box(
                        Modifier.size(50.dp)
                            .onGloballyPositioned {
                                box1LayoutCoordinates = it
                                setUpFinishedLatch.countDown()
                            }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        pointerEvent = awaitPointerEvent()

                                        when (pointerEvent!!.type) {
                                            PointerEventType.Enter -> {
                                                ++box1HoverEnter
                                            }
                                            PointerEventType.Exit -> {
                                                ++box1HoverExit
                                            }
                                            else -> {
                                                eventsThatShouldNotTrigger = true
                                            }
                                        }
                                    }
                                }
                            }
                    ) {}

                    // Box 2
                    Box(
                        Modifier.size(50.dp)
                            .onGloballyPositioned {
                                box2LayoutCoordinates = it
                                setUpFinishedLatch.countDown()
                            }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        pointerEvent = awaitPointerEvent()

                                        when (pointerEvent!!.type) {
                                            PointerEventType.Press -> {
                                                ++box2Down
                                            }
                                            PointerEventType.Release -> {
                                                ++box2Up
                                            }
                                            else -> {
                                                eventsThatShouldNotTrigger = true
                                            }
                                        }
                                    }
                                }
                            }
                    ) {}

                    // Box 3
                    Box(
                        Modifier.size(50.dp)
                            .onGloballyPositioned { setUpFinishedLatch.countDown() }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        pointerEvent = awaitPointerEvent()
                                        // Should never do anything with this UI element.
                                        eventsThatShouldNotTrigger = true
                                    }
                                }
                            }
                    ) {}
                }
            }
        }
        // Ensure Arrange (setup) step is finished
        assertTrue(setUpFinishedLatch.await(2, TimeUnit.SECONDS))

        // --> Act + Assert (interwoven)
        // Hover Enter on Box 1
        dispatchTouchEvent(
            action = ACTION_HOVER_ENTER,
            layoutCoordinates = box1LayoutCoordinates!!,
            eventTime = eventTime
        )
        rule.runOnUiThread {
            // Verify Box 1 events
            assertThat(box1HoverEnter).isEqualTo(1)
            assertThat(box1HoverExit).isEqualTo(0)

            // Verify Box 2 events
            assertThat(box2Down).isEqualTo(0)
            assertThat(box2Up).isEqualTo(0)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
            assertHoverEvent(pointerEvent!!, isEnter = true)
        }

        // Hover Exit on Box 1
        pointerEvent = null // Reset before each event
        eventTime += 100
        dispatchTouchEvent(
            action = ACTION_HOVER_EXIT,
            layoutCoordinates = box1LayoutCoordinates!!,
            eventTime = eventTime
        )

        rule.waitForFutureFrame(2)

        rule.runOnUiThread {
            // Verify Box 1 events
            assertThat(box1HoverEnter).isEqualTo(1)
            assertThat(box1HoverExit).isEqualTo(1)
            // Verify Box 2 events
            assertThat(box2Down).isEqualTo(0)
            assertThat(box2Up).isEqualTo(0)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
        }

        // Press on Box 2
        pointerEvent = null // Reset before each event
        eventTime += 100
        dispatchTouchEvent(
            action = ACTION_DOWN,
            layoutCoordinates = box2LayoutCoordinates!!,
            eventTime = eventTime
        )
        rule.runOnUiThread {
            // Verify Box 1 events
            assertThat(box1HoverEnter).isEqualTo(1)
            assertThat(box1HoverExit).isEqualTo(1)

            // Verify Box 2 events
            assertThat(box2Down).isEqualTo(1)
            assertThat(box2Up).isEqualTo(0)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
        }

        // Press on Box 2
        pointerEvent = null // Reset before each event
        eventTime += 100
        dispatchTouchEvent(
            action = ACTION_UP,
            layoutCoordinates = box2LayoutCoordinates!!,
            eventTime = eventTime
        )
        rule.runOnUiThread {
            // Verify Box 1 events
            assertThat(box1HoverEnter).isEqualTo(1)
            assertThat(box1HoverExit).isEqualTo(1)

            // Verify Box 2 events
            assertThat(box2Down).isEqualTo(1)
            assertThat(box2Up).isEqualTo(1)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
        }
    }

    /*
     * Tests alternating hover TOUCH events across multiple UI elements. Specifically, to recreate
     * Talkback events.
     *
     * Important Note: Usually a hover MotionEvent sent from Android has the tool type set as
     * [MotionEvent.TOOL_TYPE_MOUSE]. However, Talkback sets the tool type to
     * [MotionEvent.TOOL_TYPE_FINGER]. We do that in this test by calling the
     * [dispatchTouchEvent()] instead of [dispatchMouseEvent()].
     *
     * Specific events:
     *  1. UI Element 1: ENTER (hover enter [touch])
     *  2. UI Element 1: EXIT (hover exit [touch])
     *  5. UI Element 2: ENTER (hover enter [touch])
     *  6. UI Element 2: EXIT (hover exit [touch])
     *
     * Should NOT trigger any additional events (like an extra exit)!
     */
    @Test
    fun hoverEventsBetweenUIElements_hoverUi1ToHoverUi2_shouldNotTriggerAdditionalEvents() {
        // --> Arrange
        var box1LayoutCoordinates: LayoutCoordinates? = null
        var box2LayoutCoordinates: LayoutCoordinates? = null

        val setUpFinishedLatch = CountDownLatch(4)

        // Events for Box 1
        var box1HoverEnter = 0
        var box1HoverExit = 0

        // Events for Box 2
        var box2HoverEnter = 0
        var box2HoverExit = 0

        // All other events that should never be triggered in this test
        var eventsThatShouldNotTrigger = false

        var pointerEvent: PointerEvent? = null

        rule.runOnUiThread {
            container.setContent {
                Column(
                    Modifier.fillMaxSize().onGloballyPositioned { setUpFinishedLatch.countDown() }
                ) {
                    // Box 1
                    Box(
                        Modifier.size(50.dp)
                            .onGloballyPositioned {
                                box1LayoutCoordinates = it
                                setUpFinishedLatch.countDown()
                            }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        pointerEvent = awaitPointerEvent()

                                        when (pointerEvent!!.type) {
                                            PointerEventType.Enter -> {
                                                ++box1HoverEnter
                                            }
                                            PointerEventType.Exit -> {
                                                ++box1HoverExit
                                            }
                                            else -> {
                                                eventsThatShouldNotTrigger = true
                                            }
                                        }
                                    }
                                }
                            }
                    ) {}

                    // Box 2
                    Box(
                        Modifier.size(50.dp)
                            .onGloballyPositioned {
                                box2LayoutCoordinates = it
                                setUpFinishedLatch.countDown()
                            }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        pointerEvent = awaitPointerEvent()

                                        when (pointerEvent!!.type) {
                                            PointerEventType.Enter -> {
                                                ++box2HoverEnter
                                            }
                                            PointerEventType.Exit -> {
                                                ++box2HoverExit
                                            }
                                            else -> {
                                                eventsThatShouldNotTrigger = true
                                            }
                                        }
                                    }
                                }
                            }
                    ) {}

                    // Box 3
                    Box(
                        Modifier.size(50.dp)
                            .onGloballyPositioned { setUpFinishedLatch.countDown() }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        pointerEvent = awaitPointerEvent()
                                        // Should never do anything with this UI element.
                                        eventsThatShouldNotTrigger = true
                                    }
                                }
                            }
                    ) {}
                }
            }
        }
        // Ensure Arrange (setup) step is finished
        assertTrue(setUpFinishedLatch.await(2, TimeUnit.SECONDS))

        // --> Act + Assert (interwoven)
        // Hover Enter on Box 1
        dispatchTouchEvent(ACTION_HOVER_ENTER, box1LayoutCoordinates!!)
        rule.runOnUiThread {
            // Verify Box 1 events
            assertThat(box1HoverEnter).isEqualTo(1)
            assertThat(box1HoverExit).isEqualTo(0)

            // Verify Box 2 events
            assertThat(box2HoverEnter).isEqualTo(0)
            assertThat(box2HoverExit).isEqualTo(0)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
            assertHoverEvent(pointerEvent!!, isEnter = true)
        }

        // Hover Exit on Box 1
        pointerEvent = null // Reset before each event
        dispatchTouchEvent(ACTION_HOVER_EXIT, box1LayoutCoordinates!!)

        rule.waitForFutureFrame(2)

        rule.runOnUiThread {
            // Verify Box 1 events
            assertThat(box1HoverEnter).isEqualTo(1)
            assertThat(box1HoverExit).isEqualTo(1)
            // Verify Box 2 events
            assertThat(box2HoverEnter).isEqualTo(0)
            assertThat(box2HoverExit).isEqualTo(0)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
        }

        // Press on Box 2
        pointerEvent = null // Reset before each event
        dispatchTouchEvent(ACTION_HOVER_ENTER, box2LayoutCoordinates!!)
        rule.runOnUiThread {
            // Verify Box 1 events
            assertThat(box1HoverEnter).isEqualTo(1)
            assertThat(box1HoverExit).isEqualTo(1)

            // Verify Box 2 events
            assertThat(box2HoverEnter).isEqualTo(1)
            assertThat(box2HoverExit).isEqualTo(0)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
        }

        // Press on Box 2
        pointerEvent = null // Reset before each event
        dispatchTouchEvent(ACTION_HOVER_EXIT, box2LayoutCoordinates!!)

        rule.waitForFutureFrame(2)

        rule.runOnUiThread {
            // Verify Box 1 events
            assertThat(box1HoverEnter).isEqualTo(1)
            assertThat(box1HoverExit).isEqualTo(1)

            // Verify Box 2 events
            assertThat(box2HoverEnter).isEqualTo(1)
            assertThat(box2HoverExit).isEqualTo(1)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
        }
    }

    /*
     * Tests TOUCH events are triggered correctly when dynamically adding a NON-pointer input
     * modifier above an existing pointer input modifier.
     *
     * Note: The lambda for the existing pointer input modifier is not re-executed after the
     * dynamic one is added.
     *
     * Specific events:
     *  1. UI Element (modifier 1 only): PRESS (touch)
     *  2. UI Element (modifier 1 only): MOVE (touch)
     *  3. UI Element (modifier 1 only): RELEASE (touch)
     *  4. Dynamically adds NON-pointer input modifier (between input event streams)
     *  5. UI Element (modifier 1 and 2): PRESS (touch)
     *  6. UI Element (modifier 1 and 2): MOVE (touch)
     *  7. UI Element (modifier 1 and 2): RELEASE (touch)
     */
    @Test
    fun dynamicNonInputModifier_addsAboveExistingModifier_shouldTriggerInNewModifier() {
        // --> Arrange
        val originalPointerInputModifierKey = "ORIGINAL_POINTER_INPUT_MODIFIER_KEY_123"
        var box1LayoutCoordinates: LayoutCoordinates? = null

        val setUpFinishedLatch = CountDownLatch(1)

        var enableDynamicPointerInput by mutableStateOf(false)

        // Events for the lower modifier Box 1
        var originalPointerInputScopeExecutionCount by mutableStateOf(0)
        var preexistingModifierPress by mutableStateOf(0)
        var preexistingModifierMove by mutableStateOf(0)
        var preexistingModifierRelease by mutableStateOf(0)

        // All other events that should never be triggered in this test
        var eventsThatShouldNotTrigger by mutableStateOf(false)

        var pointerEvent: PointerEvent? by mutableStateOf(null)

        // Events for the dynamic upper modifier Box 1
        var dynamicModifierExecuted by mutableStateOf(false)

        // Non-Pointer Input Modifier that is toggled on/off based on passed value.
        fun Modifier.dynamicallyToggledModifier(enable: Boolean) =
            if (enable) {
                dynamicModifierExecuted = true
                background(Color.Green)
            } else this

        // Setup UI
        rule.runOnUiThread {
            container.setContent {
                Box(
                    Modifier.size(200.dp)
                        .onGloballyPositioned {
                            box1LayoutCoordinates = it
                            setUpFinishedLatch.countDown()
                        }
                        .dynamicallyToggledModifier(enableDynamicPointerInput)
                        .pointerInput(originalPointerInputModifierKey) {
                            ++originalPointerInputScopeExecutionCount
                            // Reset pointer events when lambda is ran the first time
                            preexistingModifierPress = 0
                            preexistingModifierMove = 0
                            preexistingModifierRelease = 0

                            awaitPointerEventScope {
                                while (true) {
                                    pointerEvent = awaitPointerEvent()
                                    when (pointerEvent!!.type) {
                                        PointerEventType.Press -> {
                                            ++preexistingModifierPress
                                        }
                                        PointerEventType.Move -> {
                                            ++preexistingModifierMove
                                        }
                                        PointerEventType.Release -> {
                                            ++preexistingModifierRelease
                                        }
                                        else -> {
                                            eventsThatShouldNotTrigger = true
                                        }
                                    }
                                }
                            }
                        }
                ) {}
            }
        }
        // Ensure Arrange (setup) step is finished
        assertTrue(setUpFinishedLatch.await(2, TimeUnit.SECONDS))

        // --> Act + Assert (interwoven)
        // DOWN (original modifier only)
        dispatchTouchEvent(ACTION_DOWN, box1LayoutCoordinates!!)
        rule.runOnUiThread {
            assertThat(originalPointerInputScopeExecutionCount).isEqualTo(1)
            assertThat(dynamicModifierExecuted).isEqualTo(false)

            // Verify Box 1 existing modifier events
            assertThat(preexistingModifierPress).isEqualTo(1)
            assertThat(preexistingModifierMove).isEqualTo(0)
            assertThat(preexistingModifierRelease).isEqualTo(0)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
        }

        // MOVE (original modifier only)
        dispatchTouchEvent(
            ACTION_MOVE,
            box1LayoutCoordinates!!,
            Offset(0f, box1LayoutCoordinates!!.size.height / 2 - 1f)
        )
        rule.runOnUiThread {
            assertThat(originalPointerInputScopeExecutionCount).isEqualTo(1)
            assertThat(dynamicModifierExecuted).isEqualTo(false)

            // Verify Box 1 existing modifier events
            assertThat(preexistingModifierPress).isEqualTo(1)
            assertThat(preexistingModifierMove).isEqualTo(1)
            assertThat(preexistingModifierRelease).isEqualTo(0)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
        }

        // UP (original modifier only)
        dispatchTouchEvent(
            ACTION_UP,
            box1LayoutCoordinates!!,
            Offset(0f, box1LayoutCoordinates!!.size.height / 2 - 1f)
        )
        rule.runOnUiThread {
            assertThat(originalPointerInputScopeExecutionCount).isEqualTo(1)
            assertThat(dynamicModifierExecuted).isEqualTo(false)

            // Verify Box 1 existing modifier events
            assertThat(preexistingModifierPress).isEqualTo(1)
            assertThat(preexistingModifierMove).isEqualTo(1)
            assertThat(preexistingModifierRelease).isEqualTo(1)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
        }
        enableDynamicPointerInput = true
        rule.waitForFutureFrame(2)

        // DOWN (original + dynamically added modifiers)
        dispatchTouchEvent(ACTION_DOWN, box1LayoutCoordinates!!)

        rule.runOnUiThread {
            // There are no pointer input modifiers added above this pointer modifier, so the
            // same one is used.
            assertThat(originalPointerInputScopeExecutionCount).isEqualTo(1)
            // The dynamic one has been added, so we execute its thing as well.
            assertThat(dynamicModifierExecuted).isEqualTo(true)

            // Verify Box 1 existing modifier events
            assertThat(preexistingModifierPress).isEqualTo(2)
            assertThat(preexistingModifierMove).isEqualTo(1)
            assertThat(preexistingModifierRelease).isEqualTo(1)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
        }

        // MOVE (original + dynamically added modifiers)
        dispatchTouchEvent(
            ACTION_MOVE,
            box1LayoutCoordinates!!,
            Offset(0f, box1LayoutCoordinates!!.size.height / 2 - 1f)
        )
        rule.runOnUiThread {
            assertThat(originalPointerInputScopeExecutionCount).isEqualTo(1)
            assertThat(dynamicModifierExecuted).isEqualTo(true)

            // Verify Box 1 existing modifier events
            assertThat(preexistingModifierPress).isEqualTo(2)
            assertThat(preexistingModifierMove).isEqualTo(2)
            assertThat(preexistingModifierRelease).isEqualTo(1)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
        }

        // UP (original + dynamically added modifiers)
        dispatchTouchEvent(
            ACTION_UP,
            box1LayoutCoordinates!!,
            Offset(0f, box1LayoutCoordinates!!.size.height / 2 - 1f)
        )
        rule.runOnUiThread {
            assertThat(originalPointerInputScopeExecutionCount).isEqualTo(1)
            assertThat(dynamicModifierExecuted).isEqualTo(true)

            // Verify Box 1 existing modifier events
            assertThat(preexistingModifierPress).isEqualTo(2)
            assertThat(preexistingModifierMove).isEqualTo(2)
            assertThat(preexistingModifierRelease).isEqualTo(2)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
        }
    }

    /*
     * Tests TOUCH events are triggered correctly when dynamically adding a pointer input modifier
     * ABOVE an existing pointer input modifier.
     *
     * Note: The lambda for the existing pointer input modifier **IS** re-executed after the
     * dynamic pointer input modifier is added above it.
     *
     * Specific events:
     *  1. UI Element (modifier 1 only): PRESS (touch)
     *  2. UI Element (modifier 1 only): MOVE (touch)
     *  3. UI Element (modifier 1 only): RELEASE (touch)
     *  4. Dynamically add pointer input modifier above existing one (between input event streams)
     *  5. UI Element (modifier 1 and 2): PRESS (touch)
     *  6. UI Element (modifier 1 and 2): MOVE (touch)
     *  7. UI Element (modifier 1 and 2): RELEASE (touch)
     */
    @Test
    fun dynamicInputModifierWithKey_addsAboveExistingModifier_shouldTriggerInNewModifier() {
        // --> Arrange
        val originalPointerInputModifierKey = "ORIGINAL_POINTER_INPUT_MODIFIER_KEY_123"
        var box1LayoutCoordinates: LayoutCoordinates? = null

        val setUpFinishedLatch = CountDownLatch(1)

        var enableDynamicPointerInput by mutableStateOf(false)

        // Events for the lower modifier Box 1
        var originalPointerInputScopeExecutionCount by mutableStateOf(0)
        var preexistingModifierPress by mutableStateOf(0)
        var preexistingModifierMove by mutableStateOf(0)
        var preexistingModifierRelease by mutableStateOf(0)

        // Events for the dynamic upper modifier Box 1
        var dynamicPointerInputScopeExecutionCount by mutableStateOf(0)
        var dynamicModifierPress by mutableStateOf(0)
        var dynamicModifierMove by mutableStateOf(0)
        var dynamicModifierRelease by mutableStateOf(0)

        // All other events that should never be triggered in this test
        var eventsThatShouldNotTrigger by mutableStateOf(false)

        var pointerEvent: PointerEvent? by mutableStateOf(null)

        // Pointer Input Modifier that is toggled on/off based on passed value.
        fun Modifier.dynamicallyToggledPointerInput(
            enable: Boolean,
            pointerEventLambda: (pointerEvent: PointerEvent) -> Unit
        ) =
            if (enable) {
                pointerInput(pointerEventLambda) {
                    ++dynamicPointerInputScopeExecutionCount

                    // Reset pointer events when lambda is ran the first time
                    dynamicModifierPress = 0
                    dynamicModifierMove = 0
                    dynamicModifierRelease = 0

                    awaitPointerEventScope {
                        while (true) {
                            pointerEventLambda(awaitPointerEvent())
                        }
                    }
                }
            } else this

        // Setup UI
        rule.runOnUiThread {
            container.setContent {
                Box(
                    Modifier.size(200.dp)
                        .onGloballyPositioned {
                            box1LayoutCoordinates = it
                            setUpFinishedLatch.countDown()
                        }
                        .dynamicallyToggledPointerInput(enableDynamicPointerInput) {
                            when (it.type) {
                                PointerEventType.Press -> {
                                    ++dynamicModifierPress
                                }
                                PointerEventType.Move -> {
                                    ++dynamicModifierMove
                                }
                                PointerEventType.Release -> {
                                    ++dynamicModifierRelease
                                }
                                else -> {
                                    eventsThatShouldNotTrigger = true
                                }
                            }
                        }
                        .pointerInput(originalPointerInputModifierKey) {
                            ++originalPointerInputScopeExecutionCount
                            // Reset pointer events when lambda is ran the first time
                            preexistingModifierPress = 0
                            preexistingModifierMove = 0
                            preexistingModifierRelease = 0

                            awaitPointerEventScope {
                                while (true) {
                                    pointerEvent = awaitPointerEvent()
                                    when (pointerEvent!!.type) {
                                        PointerEventType.Press -> {
                                            ++preexistingModifierPress
                                        }
                                        PointerEventType.Move -> {
                                            ++preexistingModifierMove
                                        }
                                        PointerEventType.Release -> {
                                            ++preexistingModifierRelease
                                        }
                                        else -> {
                                            eventsThatShouldNotTrigger = true
                                        }
                                    }
                                }
                            }
                        }
                ) {}
            }
        }
        // Ensure Arrange (setup) step is finished
        assertTrue(setUpFinishedLatch.await(2, TimeUnit.SECONDS))

        // --> Act + Assert (interwoven)
        // DOWN (original modifier only)
        dispatchTouchEvent(ACTION_DOWN, box1LayoutCoordinates!!)
        rule.runOnUiThread {
            assertThat(originalPointerInputScopeExecutionCount).isEqualTo(1)
            assertThat(dynamicPointerInputScopeExecutionCount).isEqualTo(0)

            // Verify Box 1 existing modifier events
            assertThat(preexistingModifierPress).isEqualTo(1)
            assertThat(preexistingModifierMove).isEqualTo(0)
            assertThat(preexistingModifierRelease).isEqualTo(0)

            // Verify Box 1 dynamically added modifier events
            assertThat(dynamicModifierPress).isEqualTo(0)
            assertThat(dynamicModifierMove).isEqualTo(0)
            assertThat(dynamicModifierRelease).isEqualTo(0)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
        }

        // MOVE (original modifier only)
        dispatchTouchEvent(
            ACTION_MOVE,
            box1LayoutCoordinates!!,
            Offset(0f, box1LayoutCoordinates!!.size.height / 2 - 1f)
        )
        rule.runOnUiThread {
            assertThat(originalPointerInputScopeExecutionCount).isEqualTo(1)
            assertThat(dynamicPointerInputScopeExecutionCount).isEqualTo(0)

            // Verify Box 1 existing modifier events
            assertThat(preexistingModifierPress).isEqualTo(1)
            assertThat(preexistingModifierMove).isEqualTo(1)
            assertThat(preexistingModifierRelease).isEqualTo(0)

            // Verify Box 1 dynamically added modifier events
            assertThat(dynamicModifierPress).isEqualTo(0)
            assertThat(dynamicModifierMove).isEqualTo(0)
            assertThat(dynamicModifierRelease).isEqualTo(0)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
        }

        // UP (original modifier only)
        dispatchTouchEvent(
            ACTION_UP,
            box1LayoutCoordinates!!,
            Offset(0f, box1LayoutCoordinates!!.size.height / 2 - 1f)
        )
        rule.runOnUiThread {
            assertThat(originalPointerInputScopeExecutionCount).isEqualTo(1)
            assertThat(dynamicPointerInputScopeExecutionCount).isEqualTo(0)

            // Verify Box 1 existing modifier events
            assertThat(preexistingModifierPress).isEqualTo(1)
            assertThat(preexistingModifierMove).isEqualTo(1)
            assertThat(preexistingModifierRelease).isEqualTo(1)

            // Verify Box 1 dynamically added modifier events
            assertThat(dynamicModifierPress).isEqualTo(0)
            assertThat(dynamicModifierMove).isEqualTo(0)
            assertThat(dynamicModifierRelease).isEqualTo(0)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
        }

        enableDynamicPointerInput = true
        rule.waitForFutureFrame(2)

        // Important Note: Even though we reset all the pointer input blocks, the initial lambda is
        // lazily executed, meaning it won't reset the values until the first event comes in, so
        // the previously set values are still the same until an event comes in.
        rule.runOnUiThread {
            assertThat(originalPointerInputScopeExecutionCount).isEqualTo(1)
            assertThat(dynamicPointerInputScopeExecutionCount).isEqualTo(0)

            assertThat(preexistingModifierPress).isEqualTo(1)
            assertThat(preexistingModifierMove).isEqualTo(1)
            assertThat(preexistingModifierRelease).isEqualTo(1)

            assertThat(dynamicModifierPress).isEqualTo(0)
            assertThat(dynamicModifierMove).isEqualTo(0)
            assertThat(dynamicModifierRelease).isEqualTo(0)
        }

        // DOWN (original + dynamically added modifiers)
        // Now an event comes in, so the lambdas are both executed completely (dynamic one for the
        // first time and the existing one for a second time [since it was moved]).
        dispatchTouchEvent(ACTION_DOWN, box1LayoutCoordinates!!)

        rule.runOnUiThread {
            // While the original pointer input block is being reused after a new one is added, it
            // is reset (since things have changed with the Modifiers), so the entire block is
            // executed again to allow devs to reset their gesture detectors for the new Modifier
            // chain changes.
            assertThat(originalPointerInputScopeExecutionCount).isEqualTo(2)
            // The dynamic one has been added, so we execute its thing as well.
            assertThat(dynamicPointerInputScopeExecutionCount).isEqualTo(1)

            // Verify Box 1 existing modifier events
            assertThat(preexistingModifierPress).isEqualTo(1)
            assertThat(preexistingModifierMove).isEqualTo(0)
            assertThat(preexistingModifierRelease).isEqualTo(0)

            // Verify Box 1 dynamically added modifier events
            assertThat(dynamicModifierPress).isEqualTo(1)
            assertThat(dynamicModifierMove).isEqualTo(0)
            assertThat(dynamicModifierRelease).isEqualTo(0)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
        }

        // MOVE (original + dynamically added modifiers)
        dispatchTouchEvent(
            ACTION_MOVE,
            box1LayoutCoordinates!!,
            Offset(0f, box1LayoutCoordinates!!.size.height / 2 - 1f)
        )
        rule.runOnUiThread {
            assertThat(originalPointerInputScopeExecutionCount).isEqualTo(2)
            assertThat(dynamicPointerInputScopeExecutionCount).isEqualTo(1)

            // Verify Box 1 existing modifier events
            assertThat(preexistingModifierPress).isEqualTo(1)
            assertThat(preexistingModifierMove).isEqualTo(1)
            assertThat(preexistingModifierRelease).isEqualTo(0)

            // Verify Box 1 dynamically added modifier events
            assertThat(dynamicModifierPress).isEqualTo(1)
            assertThat(dynamicModifierMove).isEqualTo(1)
            assertThat(dynamicModifierRelease).isEqualTo(0)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
        }

        // UP (original + dynamically added modifiers)
        dispatchTouchEvent(
            ACTION_UP,
            box1LayoutCoordinates!!,
            Offset(0f, box1LayoutCoordinates!!.size.height / 2 - 1f)
        )
        rule.runOnUiThread {
            assertThat(originalPointerInputScopeExecutionCount).isEqualTo(2)
            assertThat(dynamicPointerInputScopeExecutionCount).isEqualTo(1)

            // Verify Box 1 existing modifier events
            assertThat(preexistingModifierPress).isEqualTo(1)
            assertThat(preexistingModifierMove).isEqualTo(1)
            assertThat(preexistingModifierRelease).isEqualTo(1)

            // Verify Box 1 dynamically added modifier events
            assertThat(dynamicModifierPress).isEqualTo(1)
            assertThat(dynamicModifierMove).isEqualTo(1)
            assertThat(dynamicModifierRelease).isEqualTo(1)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
        }
    }

    /*
     * Tests TOUCH events are triggered correctly when dynamically adding a pointer input modifier
     * (which uses Unit for its key) ABOVE an existing pointer input modifier.
     *
     * Specific events:
     *  1. UI Element (modifier 1 only): PRESS (touch)
     *  2. UI Element (modifier 1 only): MOVE (touch)
     *  3. UI Element (modifier 1 only): RELEASE (touch)
     *  4. Dynamically add pointer input modifier above existing one (between input event streams)
     *  5. UI Element (modifier 1 and 2): PRESS (touch)
     *  6. UI Element (modifier 1 and 2): MOVE (touch)
     *  7. UI Element (modifier 1 and 2): RELEASE (touch)
     */
    @Test
    fun dynamicInputModifierWithUnitKey_addsAboveExistingModifier_triggersBothModifiers() {
        // --> Arrange
        var box1LayoutCoordinates: LayoutCoordinates? = null

        val setUpFinishedLatch = CountDownLatch(1)

        var enableDynamicPointerInput by mutableStateOf(false)

        // Events for the lower modifier Box 1
        var originalPointerInputScopeExecutionCount by mutableStateOf(0)
        var preexistingModifierPress by mutableStateOf(0)
        var preexistingModifierMove by mutableStateOf(0)
        var preexistingModifierRelease by mutableStateOf(0)

        // Events for the dynamic upper modifier Box 1
        var dynamicPointerInputScopeExecutionCount by mutableStateOf(0)
        var dynamicModifierPress by mutableStateOf(0)
        var dynamicModifierMove by mutableStateOf(0)
        var dynamicModifierRelease by mutableStateOf(0)

        // All other events that should never be triggered in this test
        var eventsThatShouldNotTrigger by mutableStateOf(false)

        var pointerEvent: PointerEvent? by mutableStateOf(null)

        // Pointer Input Modifier that is toggled on/off based on passed value.
        fun Modifier.dynamicallyToggledPointerInput(
            enable: Boolean,
            pointerEventLambda: (pointerEvent: PointerEvent) -> Unit
        ) =
            if (enable) {
                pointerInput(Unit) {
                    ++dynamicPointerInputScopeExecutionCount

                    // Reset pointer events when lambda is ran the first time
                    dynamicModifierPress = 0
                    dynamicModifierMove = 0
                    dynamicModifierRelease = 0

                    awaitPointerEventScope {
                        while (true) {
                            pointerEventLambda(awaitPointerEvent())
                        }
                    }
                }
            } else this

        // Setup UI
        rule.runOnUiThread {
            container.setContent {
                Box(
                    Modifier.size(200.dp)
                        .onGloballyPositioned {
                            box1LayoutCoordinates = it
                            setUpFinishedLatch.countDown()
                        }
                        .dynamicallyToggledPointerInput(enableDynamicPointerInput) {
                            when (it.type) {
                                PointerEventType.Press -> {
                                    ++dynamicModifierPress
                                }
                                PointerEventType.Move -> {
                                    ++dynamicModifierMove
                                }
                                PointerEventType.Release -> {
                                    ++dynamicModifierRelease
                                }
                                else -> {
                                    eventsThatShouldNotTrigger = true
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            ++originalPointerInputScopeExecutionCount
                            awaitPointerEventScope {
                                while (true) {
                                    pointerEvent = awaitPointerEvent()
                                    when (pointerEvent!!.type) {
                                        PointerEventType.Press -> {
                                            ++preexistingModifierPress
                                        }
                                        PointerEventType.Move -> {
                                            ++preexistingModifierMove
                                        }
                                        PointerEventType.Release -> {
                                            ++preexistingModifierRelease
                                        }
                                        else -> {
                                            eventsThatShouldNotTrigger = true
                                        }
                                    }
                                }
                            }
                        }
                ) {}
            }
        }
        // Ensure Arrange (setup) step is finished
        assertTrue(setUpFinishedLatch.await(2, TimeUnit.SECONDS))

        // --> Act + Assert (interwoven)
        // DOWN (original modifier only)
        dispatchTouchEvent(ACTION_DOWN, box1LayoutCoordinates!!)
        rule.runOnUiThread {
            assertThat(originalPointerInputScopeExecutionCount).isEqualTo(1)
            assertThat(dynamicPointerInputScopeExecutionCount).isEqualTo(0)

            // Verify Box 1 existing modifier events
            assertThat(preexistingModifierPress).isEqualTo(1)
            assertThat(preexistingModifierMove).isEqualTo(0)
            assertThat(preexistingModifierRelease).isEqualTo(0)

            // Verify Box 1 dynamically added modifier events
            assertThat(dynamicModifierPress).isEqualTo(0)
            assertThat(dynamicModifierMove).isEqualTo(0)
            assertThat(dynamicModifierRelease).isEqualTo(0)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
        }

        // MOVE (original modifier only)
        dispatchTouchEvent(
            ACTION_MOVE,
            box1LayoutCoordinates!!,
            Offset(0f, box1LayoutCoordinates!!.size.height / 2 - 1f)
        )
        rule.runOnUiThread {
            assertThat(originalPointerInputScopeExecutionCount).isEqualTo(1)
            assertThat(dynamicPointerInputScopeExecutionCount).isEqualTo(0)

            // Verify Box 1 existing modifier events
            assertThat(preexistingModifierPress).isEqualTo(1)
            assertThat(preexistingModifierMove).isEqualTo(1)
            assertThat(preexistingModifierRelease).isEqualTo(0)

            // Verify Box 1 dynamically added modifier events
            assertThat(dynamicModifierPress).isEqualTo(0)
            assertThat(dynamicModifierMove).isEqualTo(0)
            assertThat(dynamicModifierRelease).isEqualTo(0)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
        }

        // UP (original modifier only)
        dispatchTouchEvent(
            ACTION_UP,
            box1LayoutCoordinates!!,
            Offset(0f, box1LayoutCoordinates!!.size.height / 2 - 1f)
        )
        rule.runOnUiThread {
            assertThat(originalPointerInputScopeExecutionCount).isEqualTo(1)
            assertThat(dynamicPointerInputScopeExecutionCount).isEqualTo(0)

            // Verify Box 1 existing modifier events
            assertThat(preexistingModifierPress).isEqualTo(1)
            assertThat(preexistingModifierMove).isEqualTo(1)
            assertThat(preexistingModifierRelease).isEqualTo(1)

            // Verify Box 1 dynamically added modifier events
            assertThat(dynamicModifierPress).isEqualTo(0)
            assertThat(dynamicModifierMove).isEqualTo(0)
            assertThat(dynamicModifierRelease).isEqualTo(0)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
        }

        enableDynamicPointerInput = true
        rule.waitForFutureFrame(2)

        // Important Note: I'm not resetting the variable counters in this test.

        // DOWN (original + dynamically added modifiers)
        // Now an event comes in, so the lambdas are both executed completely for the first time.
        dispatchTouchEvent(ACTION_DOWN, box1LayoutCoordinates!!)

        rule.runOnUiThread {
            // While the original pointer input block is being reused after a new one is added, it
            // is reset (since things have changed with the Modifiers), so the entire block is
            // executed again to allow devs to reset their gesture detectors for the new Modifier
            // chain changes.
            assertThat(originalPointerInputScopeExecutionCount).isEqualTo(2)
            // The dynamic one has been added, so we execute its lambda as well.
            assertThat(dynamicPointerInputScopeExecutionCount).isEqualTo(1)

            // Verify Box 1 existing modifier events
            assertThat(preexistingModifierPress).isEqualTo(2)
            assertThat(preexistingModifierMove).isEqualTo(1)
            assertThat(preexistingModifierRelease).isEqualTo(1)

            // Verify Box 1 dynamically added modifier events
            assertThat(dynamicModifierPress).isEqualTo(1)
            assertThat(dynamicModifierMove).isEqualTo(0)
            assertThat(dynamicModifierRelease).isEqualTo(0)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
        }

        // MOVE (original + dynamically added modifiers)
        dispatchTouchEvent(
            ACTION_MOVE,
            box1LayoutCoordinates!!,
            Offset(0f, box1LayoutCoordinates!!.size.height / 2 - 1f)
        )
        rule.runOnUiThread {
            assertThat(originalPointerInputScopeExecutionCount).isEqualTo(2)
            assertThat(dynamicPointerInputScopeExecutionCount).isEqualTo(1)

            // Verify Box 1 existing modifier events
            assertThat(preexistingModifierPress).isEqualTo(2)
            assertThat(preexistingModifierMove).isEqualTo(2)
            assertThat(preexistingModifierRelease).isEqualTo(1)

            // Verify Box 1 dynamically added modifier events
            assertThat(dynamicModifierPress).isEqualTo(1)
            assertThat(dynamicModifierMove).isEqualTo(1)
            assertThat(dynamicModifierRelease).isEqualTo(0)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
        }

        // UP (original + dynamically added modifiers)
        dispatchTouchEvent(
            ACTION_UP,
            box1LayoutCoordinates!!,
            Offset(0f, box1LayoutCoordinates!!.size.height / 2 - 1f)
        )
        rule.runOnUiThread {
            assertThat(originalPointerInputScopeExecutionCount).isEqualTo(2)
            assertThat(dynamicPointerInputScopeExecutionCount).isEqualTo(1)

            // Verify Box 1 existing modifier events
            assertThat(preexistingModifierPress).isEqualTo(2)
            assertThat(preexistingModifierMove).isEqualTo(2)
            assertThat(preexistingModifierRelease).isEqualTo(2)

            // Verify Box 1 dynamically added modifier events
            assertThat(dynamicModifierPress).isEqualTo(1)
            assertThat(dynamicModifierMove).isEqualTo(1)
            assertThat(dynamicModifierRelease).isEqualTo(1)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
        }
    }

    /*
     * Tests TOUCH events are triggered correctly when dynamically adding a pointer input
     * modifier BELOW an existing pointer input modifier.
     *
     * Note: The lambda for the existing pointer input modifier is NOT re-executed after the
     * dynamic one is added below it (since it doesn't impact it).
     *
     * Specific events:
     *  1. UI Element (modifier 1 only): PRESS (touch)
     *  2. UI Element (modifier 1 only): MOVE (touch)
     *  3. UI Element (modifier 1 only): RELEASE (touch)
     *  4. Dynamically add pointer input modifier below existing one (between input event streams)
     *  5. UI Element (modifier 1 and 2): PRESS (touch)
     *  6. UI Element (modifier 1 and 2): MOVE (touch)
     *  7. UI Element (modifier 1 and 2): RELEASE (touch)
     */
    @Test
    fun dynamicInputModifierWithKey_addsBelowExistingModifier_shouldTriggerInNewModifier() {
        // --> Arrange
        val originalPointerInputModifierKey = "ORIGINAL_POINTER_INPUT_MODIFIER_KEY_123"
        var box1LayoutCoordinates: LayoutCoordinates? = null

        val setUpFinishedLatch = CountDownLatch(1)

        var enableDynamicPointerInput by mutableStateOf(false)

        // Events for the lower modifier Box 1
        var originalPointerInputScopeExecutionCount by mutableStateOf(0)
        var preexistingModifierPress by mutableStateOf(0)
        var preexistingModifierMove by mutableStateOf(0)
        var preexistingModifierRelease by mutableStateOf(0)

        // Events for the dynamic upper modifier Box 1
        var dynamicPointerInputScopeExecutionCount by mutableStateOf(0)
        var dynamicModifierPress by mutableStateOf(0)
        var dynamicModifierMove by mutableStateOf(0)
        var dynamicModifierRelease by mutableStateOf(0)

        // All other events that should never be triggered in this test
        var eventsThatShouldNotTrigger by mutableStateOf(false)

        var pointerEvent: PointerEvent? by mutableStateOf(null)

        // Pointer Input Modifier that is toggled on/off based on passed value.
        fun Modifier.dynamicallyToggledPointerInput(
            enable: Boolean,
            pointerEventLambda: (pointerEvent: PointerEvent) -> Unit
        ) =
            if (enable) {
                pointerInput(pointerEventLambda) {
                    ++dynamicPointerInputScopeExecutionCount

                    // Reset pointer events when lambda is ran the first time
                    dynamicModifierPress = 0
                    dynamicModifierMove = 0
                    dynamicModifierRelease = 0

                    awaitPointerEventScope {
                        while (true) {
                            pointerEventLambda(awaitPointerEvent())
                        }
                    }
                }
            } else this

        // Setup UI
        rule.runOnUiThread {
            container.setContent {
                Box(
                    Modifier.size(200.dp)
                        .onGloballyPositioned {
                            box1LayoutCoordinates = it
                            setUpFinishedLatch.countDown()
                        }
                        .pointerInput(originalPointerInputModifierKey) {
                            ++originalPointerInputScopeExecutionCount
                            // Reset pointer events when lambda is ran the first time
                            preexistingModifierPress = 0
                            preexistingModifierMove = 0
                            preexistingModifierRelease = 0

                            awaitPointerEventScope {
                                while (true) {
                                    pointerEvent = awaitPointerEvent()
                                    when (pointerEvent!!.type) {
                                        PointerEventType.Press -> {
                                            ++preexistingModifierPress
                                        }
                                        PointerEventType.Move -> {
                                            ++preexistingModifierMove
                                        }
                                        PointerEventType.Release -> {
                                            ++preexistingModifierRelease
                                        }
                                        else -> {
                                            eventsThatShouldNotTrigger = true
                                        }
                                    }
                                }
                            }
                        }
                        .dynamicallyToggledPointerInput(enableDynamicPointerInput) {
                            when (it.type) {
                                PointerEventType.Press -> {
                                    ++dynamicModifierPress
                                }
                                PointerEventType.Move -> {
                                    ++dynamicModifierMove
                                }
                                PointerEventType.Release -> {
                                    ++dynamicModifierRelease
                                }
                                else -> {
                                    eventsThatShouldNotTrigger = true
                                }
                            }
                        }
                ) {}
            }
        }
        // Ensure Arrange (setup) step is finished
        assertTrue(setUpFinishedLatch.await(2, TimeUnit.SECONDS))

        // --> Act + Assert (interwoven)
        // DOWN (original modifier only)
        dispatchTouchEvent(ACTION_DOWN, box1LayoutCoordinates!!)
        rule.runOnUiThread {
            assertThat(originalPointerInputScopeExecutionCount).isEqualTo(1)
            assertThat(dynamicPointerInputScopeExecutionCount).isEqualTo(0)

            // Verify Box 1 existing modifier events
            assertThat(preexistingModifierPress).isEqualTo(1)
            assertThat(preexistingModifierMove).isEqualTo(0)
            assertThat(preexistingModifierRelease).isEqualTo(0)

            // Verify Box 1 dynamically added modifier events
            assertThat(dynamicModifierPress).isEqualTo(0)
            assertThat(dynamicModifierMove).isEqualTo(0)
            assertThat(dynamicModifierRelease).isEqualTo(0)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
        }

        // MOVE (original modifier only)
        dispatchTouchEvent(
            ACTION_MOVE,
            box1LayoutCoordinates!!,
            Offset(0f, box1LayoutCoordinates!!.size.height / 2 - 1f)
        )
        rule.runOnUiThread {
            assertThat(originalPointerInputScopeExecutionCount).isEqualTo(1)
            assertThat(dynamicPointerInputScopeExecutionCount).isEqualTo(0)

            // Verify Box 1 existing modifier events
            assertThat(preexistingModifierPress).isEqualTo(1)
            assertThat(preexistingModifierMove).isEqualTo(1)
            assertThat(preexistingModifierRelease).isEqualTo(0)

            // Verify Box 1 dynamically added modifier events
            assertThat(dynamicModifierPress).isEqualTo(0)
            assertThat(dynamicModifierMove).isEqualTo(0)
            assertThat(dynamicModifierRelease).isEqualTo(0)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
        }

        // UP (original modifier only)
        dispatchTouchEvent(
            ACTION_UP,
            box1LayoutCoordinates!!,
            Offset(0f, box1LayoutCoordinates!!.size.height / 2 - 1f)
        )
        rule.runOnUiThread {
            assertThat(originalPointerInputScopeExecutionCount).isEqualTo(1)
            assertThat(dynamicPointerInputScopeExecutionCount).isEqualTo(0)

            // Verify Box 1 existing modifier events
            assertThat(preexistingModifierPress).isEqualTo(1)
            assertThat(preexistingModifierMove).isEqualTo(1)
            assertThat(preexistingModifierRelease).isEqualTo(1)

            // Verify Box 1 dynamically added modifier events
            assertThat(dynamicModifierPress).isEqualTo(0)
            assertThat(dynamicModifierMove).isEqualTo(0)
            assertThat(dynamicModifierRelease).isEqualTo(0)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
        }

        enableDynamicPointerInput = true
        rule.waitForFutureFrame(2)

        // DOWN (original + dynamically added modifiers)
        dispatchTouchEvent(ACTION_DOWN, box1LayoutCoordinates!!)

        rule.runOnUiThread {
            // Because the new pointer input modifier is added below the existing one, the existing
            // one doesn't change.
            assertThat(originalPointerInputScopeExecutionCount).isEqualTo(1)
            assertThat(dynamicPointerInputScopeExecutionCount).isEqualTo(1)

            // Verify Box 1 existing modifier events
            assertThat(preexistingModifierPress).isEqualTo(2)
            assertThat(preexistingModifierMove).isEqualTo(1)
            assertThat(preexistingModifierRelease).isEqualTo(1)

            // Verify Box 1 dynamically added modifier events
            assertThat(dynamicModifierPress).isEqualTo(1)
            assertThat(dynamicModifierMove).isEqualTo(0)
            assertThat(dynamicModifierRelease).isEqualTo(0)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
        }

        // MOVE (original + dynamically added modifiers)
        dispatchTouchEvent(
            ACTION_MOVE,
            box1LayoutCoordinates!!,
            Offset(0f, box1LayoutCoordinates!!.size.height / 2 - 1f)
        )
        rule.runOnUiThread {
            assertThat(originalPointerInputScopeExecutionCount).isEqualTo(1)
            assertThat(dynamicPointerInputScopeExecutionCount).isEqualTo(1)

            // Verify Box 1 existing modifier events
            assertThat(preexistingModifierPress).isEqualTo(2)
            assertThat(preexistingModifierMove).isEqualTo(2)
            assertThat(preexistingModifierRelease).isEqualTo(1)

            // Verify Box 1 dynamically added modifier events
            assertThat(dynamicModifierPress).isEqualTo(1)
            assertThat(dynamicModifierMove).isEqualTo(1)
            assertThat(dynamicModifierRelease).isEqualTo(0)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
        }

        // UP (original + dynamically added modifiers)
        dispatchTouchEvent(
            ACTION_UP,
            box1LayoutCoordinates!!,
            Offset(0f, box1LayoutCoordinates!!.size.height / 2 - 1f)
        )
        rule.runOnUiThread {
            assertThat(originalPointerInputScopeExecutionCount).isEqualTo(1)
            assertThat(dynamicPointerInputScopeExecutionCount).isEqualTo(1)

            // Verify Box 1 existing modifier events
            assertThat(preexistingModifierPress).isEqualTo(2)
            assertThat(preexistingModifierMove).isEqualTo(2)
            assertThat(preexistingModifierRelease).isEqualTo(2)

            // Verify Box 1 dynamically added modifier events
            assertThat(dynamicModifierPress).isEqualTo(1)
            assertThat(dynamicModifierMove).isEqualTo(1)
            assertThat(dynamicModifierRelease).isEqualTo(1)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
        }
    }

    /*
     * Tests a full mouse event cycle from a press and release.
     *
     * Important Note: The pointer id should stay the same throughout all these events (part of the
     * test).
     *
     * Specific MotionEvents:
     *  1. UI Element 1: ENTER (hover enter [mouse])
     *  2. UI Element 1: EXIT (hover exit [mouse]) - Doesn't trigger Compose PointerEvent
     *  3. UI Element 1: PRESS (mouse)
     *  4. UI Element 1: ACTION_BUTTON_PRESS (mouse)
     *  5. UI Element 1: ACTION_BUTTON_RELEASE (mouse)
     *  6. UI Element 1: RELEASE (mouse)
     *  7. UI Element 1: ENTER (hover enter [mouse]) - Doesn't trigger Compose PointerEvent
     *  8. UI Element 1: EXIT (hover enter [mouse])
     *
     * Should NOT trigger any additional events (like an extra press or exit)!
     */
    @Test
    fun mouseEventsAndPointerIds_completeMouseEventCycle_pointerIdsShouldMatchAcrossAllEvents() {
        // --> Arrange
        var box1LayoutCoordinates: LayoutCoordinates? = null

        val setUpFinishedLatch = CountDownLatch(1)

        // Events for Box
        var hoverEventCount = 0
        var hoverExitCount = 0
        var downCount = 0
        // unknownCount covers both button action press and release from Android system for a
        // mouse. These events happen between the normal press and release events.
        var unknownCount = 0
        var upCount = 0

        // We want to assert that each updated pointer id matches the original pointer id that
        // starts the sequence of MotionEvents.
        var originalPointerId = -1L
        var box1PointerId = -1L

        // All other events that should never be triggered in this test
        var eventsThatShouldNotTrigger = false

        var pointerEvent: PointerEvent? = null

        rule.runOnUiThread {
            container.setContent {
                Box(
                    Modifier.size(50.dp)
                        .onGloballyPositioned {
                            box1LayoutCoordinates = it
                            setUpFinishedLatch.countDown()
                        }
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    pointerEvent = awaitPointerEvent()

                                    if (originalPointerId < 0) {
                                        originalPointerId = pointerEvent!!.changes[0].id.value
                                    }

                                    box1PointerId = pointerEvent!!.changes[0].id.value

                                    when (pointerEvent!!.type) {
                                        PointerEventType.Enter -> {
                                            ++hoverEventCount
                                        }
                                        PointerEventType.Press -> {
                                            ++downCount
                                        }
                                        PointerEventType.Release -> {
                                            ++upCount
                                        }
                                        PointerEventType.Exit -> {
                                            ++hoverExitCount
                                        }
                                        PointerEventType.Unknown -> {
                                            ++unknownCount
                                        }
                                        else -> {
                                            eventsThatShouldNotTrigger = true
                                        }
                                    }
                                }
                            }
                        }
                ) {}
            }
        }
        // Ensure Arrange (setup) step is finished
        assertTrue(setUpFinishedLatch.await(2, TimeUnit.SECONDS))

        // --> Act + Assert (interwoven)
        dispatchMouseEvent(ACTION_HOVER_ENTER, box1LayoutCoordinates!!)
        rule.runOnUiThread {
            // Verify Box 1 events and pointer id
            assertThat(originalPointerId).isEqualTo(box1PointerId)
            assertThat(hoverEventCount).isEqualTo(1)
            assertThat(hoverExitCount).isEqualTo(0)
            assertThat(downCount).isEqualTo(0)
            assertThat(unknownCount).isEqualTo(0)
            assertThat(upCount).isEqualTo(0)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
            assertHoverEvent(pointerEvent!!, isEnter = true)
        }

        pointerEvent = null // Reset before each event

        // This will be interpreted as a synthetic event triggered by an ACTION_DOWN because we
        // don't wait several frames before triggering the ACTION_DOWN. Thus, no hover exit is
        // triggered.
        dispatchMouseEvent(ACTION_HOVER_EXIT, box1LayoutCoordinates!!)
        dispatchMouseEvent(ACTION_DOWN, box1LayoutCoordinates!!)

        rule.runOnUiThread {
            assertThat(originalPointerId).isEqualTo(box1PointerId)
            assertThat(hoverEventCount).isEqualTo(1)
            assertThat(hoverExitCount).isEqualTo(0)
            assertThat(downCount).isEqualTo(1)
            assertThat(unknownCount).isEqualTo(0)
            assertThat(upCount).isEqualTo(0)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
        }

        pointerEvent = null // Reset before each event
        dispatchMouseEvent(ACTION_BUTTON_PRESS, box1LayoutCoordinates!!)
        rule.runOnUiThread {
            // Verify Box 1 events
            assertThat(originalPointerId).isEqualTo(box1PointerId)
            assertThat(hoverEventCount).isEqualTo(1)
            assertThat(hoverExitCount).isEqualTo(0)
            assertThat(downCount).isEqualTo(1)
            // unknownCount covers both button action press and release from Android system for a
            // mouse. These events happen between the normal press and release events.
            assertThat(unknownCount).isEqualTo(1)
            assertThat(upCount).isEqualTo(0)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
        }

        pointerEvent = null // Reset before each event
        dispatchMouseEvent(ACTION_BUTTON_RELEASE, box1LayoutCoordinates!!)
        rule.runOnUiThread {
            assertThat(originalPointerId).isEqualTo(box1PointerId)
            assertThat(hoverEventCount).isEqualTo(1)
            assertThat(hoverExitCount).isEqualTo(0)
            assertThat(downCount).isEqualTo(1)
            // unknownCount covers both button action press and release from Android system for a
            // mouse. These events happen between the normal press and release events.
            assertThat(unknownCount).isEqualTo(2)
            assertThat(upCount).isEqualTo(0)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
        }

        pointerEvent = null // Reset before each event
        dispatchMouseEvent(ACTION_UP, box1LayoutCoordinates!!)
        // Compose already considered us as ENTERING the UI Element, so we don't need to trigger
        // it again. (This is a synthetic hover enter anyway sent from platform with UP.)
        dispatchMouseEvent(ACTION_HOVER_ENTER, box1LayoutCoordinates!!)

        rule.runOnUiThread {
            assertThat(originalPointerId).isEqualTo(box1PointerId)
            assertThat(hoverEventCount).isEqualTo(1)
            assertThat(hoverExitCount).isEqualTo(0)
            assertThat(downCount).isEqualTo(1)
            assertThat(unknownCount).isEqualTo(2)
            assertThat(upCount).isEqualTo(1)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
        }

        pointerEvent = null // Reset before each event
        dispatchMouseEvent(ACTION_HOVER_EXIT, box1LayoutCoordinates!!)

        // Wait enough time for timeout on hover exit to trigger
        rule.waitForFutureFrame(2)

        rule.runOnUiThread {
            assertThat(originalPointerId).isEqualTo(box1PointerId)
            assertThat(hoverEventCount).isEqualTo(1)
            assertThat(hoverExitCount).isEqualTo(1)
            assertThat(downCount).isEqualTo(1)
            assertThat(unknownCount).isEqualTo(2)
            assertThat(upCount).isEqualTo(1)

            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
        }
    }

    /*
     * Tests an ACTION_HOVER_EXIT MotionEvent is ignored in Compose when it proceeds an
     * ACTION_DOWN MotionEvent (in a measure of milliseconds only).
     *
     * The event order of MotionEvents:
     *   - Hover enter on box 1
     *   - Loop 10 times:
     *     - Hover enter on box 1
     *     - Down on box 1
     *     - Up on box 1
     */
    @Test
    fun hoverExitBeforeDownMotionEvent_shortDelayBetweenMotionEvents_shouldNotTriggerHoverExit() {
        // --> Arrange
        var box1LayoutCoordinates: LayoutCoordinates? = null

        val setUpFinishedLatch = CountDownLatch(4)

        // Events for Box 1
        var enterBox1 = false
        var pressBox1 = false

        // All other events that should never be triggered in this test
        var eventsThatShouldNotTrigger = false

        var pointerEvent: PointerEvent? = null

        rule.runOnUiThread {
            container.setContent {
                Column(
                    Modifier.fillMaxSize().onGloballyPositioned { setUpFinishedLatch.countDown() }
                ) {
                    // Box 1
                    Box(
                        Modifier.size(50.dp)
                            .onGloballyPositioned {
                                box1LayoutCoordinates = it
                                setUpFinishedLatch.countDown()
                            }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        pointerEvent = awaitPointerEvent()

                                        when (pointerEvent!!.type) {
                                            PointerEventType.Enter -> {
                                                enterBox1 = true
                                            }
                                            PointerEventType.Exit -> {
                                                enterBox1 = false
                                            }
                                            PointerEventType.Press -> {
                                                pressBox1 = true
                                            }
                                            PointerEventType.Release -> {
                                                pressBox1 = false
                                            }
                                            else -> {
                                                eventsThatShouldNotTrigger = true
                                            }
                                        }
                                    }
                                }
                            }
                    ) {}

                    // Box 2
                    Box(
                        Modifier.size(50.dp)
                            .onGloballyPositioned { setUpFinishedLatch.countDown() }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        pointerEvent = awaitPointerEvent()
                                        // Should never do anything with this UI element.
                                        eventsThatShouldNotTrigger = true
                                    }
                                }
                            }
                    ) {}

                    // Box 3
                    Box(
                        Modifier.size(50.dp)
                            .onGloballyPositioned { setUpFinishedLatch.countDown() }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        pointerEvent = awaitPointerEvent()
                                        // Should never do anything with this UI element.
                                        eventsThatShouldNotTrigger = true
                                    }
                                }
                            }
                    ) {}
                }
            }
        }
        // Ensure Arrange (setup) step is finished
        assertTrue(setUpFinishedLatch.await(2, TimeUnit.SECONDS))

        // --> Act + Assert (interwoven)
        // Hover Enter on Box 1
        dispatchMouseEvent(ACTION_HOVER_ENTER, box1LayoutCoordinates!!)
        rule.runOnUiThread {
            assertThat(enterBox1).isTrue()
            assertThat(pointerEvent).isNotNull()
            assertThat(eventsThatShouldNotTrigger).isFalse()
            assertHoverEvent(pointerEvent!!, isEnter = true)
        }

        pointerEvent = null // Reset before each event

        for (index in 0 until 10) {
            // We do not use dispatchMouseEvent() to dispatch the following two events, because the
            // actions need to be executed in immediate succession.
            rule.runOnUiThread {
                val root = box1LayoutCoordinates!!.findRootCoordinates()
                val pos = root.localPositionOf(box1LayoutCoordinates!!, Offset.Zero)

                // Exit on Box 1 right before action down. This happens normally on devices, so we
                // are recreating it here. However, Compose ignores the exit if it is right before
                // a down (right before meaning within a couple milliseconds). We verify that it
                // did in fact ignore this exit.
                val exitMotionEvent =
                    MotionEvent(
                        0,
                        ACTION_HOVER_EXIT,
                        1,
                        0,
                        arrayOf(PointerProperties(0).also { it.toolType = TOOL_TYPE_MOUSE }),
                        arrayOf(PointerCoords(pos.x, pos.y, Offset.Zero.x, Offset.Zero.y))
                    )

                // Press on Box 1
                val downMotionEvent =
                    MotionEvent(
                        0,
                        ACTION_DOWN,
                        1,
                        0,
                        arrayOf(PointerProperties(0).also { it.toolType = TOOL_TYPE_MOUSE }),
                        arrayOf(PointerCoords(pos.x, pos.y, Offset.Zero.x, Offset.Zero.y))
                    )

                val androidComposeView = findAndroidComposeView(container) as AndroidComposeView

                // Execute events
                androidComposeView.dispatchHoverEvent(exitMotionEvent)
                androidComposeView.dispatchTouchEvent(downMotionEvent)
            }

            // In Compose, a hover exit MotionEvent is ignored if it is quickly followed
            // by a press.
            rule.runOnUiThread {
                assertThat(enterBox1).isTrue()
                assertThat(pressBox1).isTrue()
                assertThat(eventsThatShouldNotTrigger).isFalse()
                assertThat(pointerEvent).isNotNull()
            }

            // Release on Box 1
            pointerEvent = null // Reset before each event
            dispatchTouchEvent(ACTION_UP, box1LayoutCoordinates!!)

            rule.runOnUiThread {
                assertThat(enterBox1).isTrue()
                assertThat(pressBox1).isFalse()
                assertThat(eventsThatShouldNotTrigger).isFalse()
                assertThat(pointerEvent).isNotNull()
            }
        }

        rule.runOnUiThread { assertThat(eventsThatShouldNotTrigger).isFalse() }
    }

    @Test
    fun dispatchHoverMove() {
        var layoutCoordinates: LayoutCoordinates? = null
        var layoutCoordinates2: LayoutCoordinates? = null
        val latch = CountDownLatch(1)
        val eventLatch = CountDownLatch(1)
        var anyOtherEvent = false

        var move1 = false
        var move2 = false
        var move3 = false

        var enter: PointerEvent? = null
        var move: PointerEvent? = null
        var exit: PointerEvent? = null

        rule.runOnUiThread {
            container.setContent {
                Box(
                    Modifier.fillMaxSize()
                        .onGloballyPositioned {
                            layoutCoordinates = it
                            latch.countDown()
                        }
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                awaitPointerEvent() // enter
                                assertHoverEvent(awaitPointerEvent()) // move
                                move1 = true
                                assertHoverEvent(awaitPointerEvent()) // move
                                move2 = true
                                assertHoverEvent(awaitPointerEvent()) // move
                                move3 = true
                                awaitPointerEvent() // exit
                                eventLatch.countDown()
                            }
                        }
                ) {
                    Box(
                        Modifier.align(Alignment.Center)
                            .size(50.dp)
                            .onGloballyPositioned { layoutCoordinates2 = it }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    enter = awaitPointerEvent()
                                    move = awaitPointerEvent()
                                    exit = awaitPointerEvent()
                                    awaitPointerEvent()
                                    anyOtherEvent = true
                                }
                            }
                    )
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        // Enter outer Box
        dispatchMouseEvent(ACTION_HOVER_ENTER, layoutCoordinates!!)

        // Move to inner Box
        dispatchMouseEvent(ACTION_HOVER_MOVE, layoutCoordinates2!!)
        rule.runOnUiThread {
            assertThat(move1).isTrue()
            assertThat(enter).isNotNull()
            assertHoverEvent(enter!!, isEnter = true)
        }

        // Move within inner Box
        dispatchMouseEvent(ACTION_HOVER_MOVE, layoutCoordinates2!!, Offset(1f, 1f))
        rule.runOnUiThread {
            assertThat(move2).isTrue()
            assertThat(move).isNotNull()
            assertHoverEvent(move!!)
        }

        // Move to outer Box
        dispatchMouseEvent(ACTION_HOVER_MOVE, layoutCoordinates!!)
        rule.runOnUiThread {
            assertThat(move3).isTrue()
            assertThat(exit).isNotNull()
            assertHoverEvent(exit!!, isExit = true)
        }

        // Leave outer Box
        dispatchMouseEvent(ACTION_HOVER_EXIT, layoutCoordinates!!)

        rule.runOnUiThread { assertThat(anyOtherEvent).isFalse() }
        assertTrue(eventLatch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun dispatchScroll() {
        var layoutCoordinates: LayoutCoordinates? = null
        val latch = CountDownLatch(1)
        val events = mutableListOf<PointerEvent>()
        val scrollDelta = Offset(0.35f, 0.65f)
        rule.runOnUiThread {
            container.setContent {
                Box(
                    Modifier.fillMaxSize()
                        .onGloballyPositioned {
                            layoutCoordinates = it
                            latch.countDown()
                        }
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    event.changes[0].consume()
                                    events += event
                                }
                            }
                        }
                )
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        dispatchMouseEvent(ACTION_SCROLL, layoutCoordinates!!, scrollDelta = scrollDelta)
        rule.runOnUiThread {
            assertThat(events).hasSize(2) // synthetic enter and scroll
            assertHoverEvent(events[0], isEnter = true)
            assertScrollEvent(events[1], scrollExpected = scrollDelta)
        }
    }

    @Test
    fun dispatchScroll_whenButtonPressed() {
        var layoutCoordinates: LayoutCoordinates? = null
        val latch = CountDownLatch(1)
        val events = mutableListOf<PointerEvent>()
        val scrollDelta = Offset(0.35f, 0.65f)
        rule.runOnUiThread {
            container.setContent {
                Box(
                    Modifier.fillMaxSize()
                        .onGloballyPositioned {
                            layoutCoordinates = it
                            latch.countDown()
                        }
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    event.changes[0].consume()
                                    events += event
                                }
                            }
                        }
                )
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        // press the button first before scroll
        dispatchMouseEvent(ACTION_DOWN, layoutCoordinates!!)
        dispatchMouseEvent(ACTION_SCROLL, layoutCoordinates!!, scrollDelta = scrollDelta)
        rule.runOnUiThread {
            assertThat(events).hasSize(3) // synthetic enter, button down, scroll
            assertHoverEvent(events[0], isEnter = true)
            assert(events[1].changes.fastAll { it.changedToDownIgnoreConsumed() })
            assertScrollEvent(events[2], scrollExpected = scrollDelta)
        }
    }

    @Test
    fun dispatchScroll_batch() {
        var layoutCoordinates: LayoutCoordinates? = null
        val latch = CountDownLatch(1)
        val events = mutableListOf<PointerEvent>()
        val scrollDelta1 = Offset(0.32f, -0.75f)
        val scrollDelta2 = Offset(0.14f, 0.35f)
        val scrollDelta3 = Offset(-0.30f, -0.12f)
        val scrollDelta4 = Offset(-0.05f, 0.68f)
        rule.runOnUiThread {
            container.setContent {
                Box(
                    Modifier.fillMaxSize()
                        .onGloballyPositioned {
                            layoutCoordinates = it
                            latch.countDown()
                        }
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    event.changes[0].consume()
                                    events += event
                                }
                            }
                        }
                )
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        listOf(scrollDelta1, scrollDelta2, scrollDelta3, scrollDelta4).fastForEach {
            dispatchMouseEvent(ACTION_SCROLL, layoutCoordinates!!, scrollDelta = it)
        }
        rule.runOnUiThread {
            assertThat(events).hasSize(5) // 4 + synthetic enter
            assertHoverEvent(events[0], isEnter = true)
            assertScrollEvent(events[1], scrollExpected = scrollDelta1)
            assertScrollEvent(events[2], scrollExpected = scrollDelta2)
            assertScrollEvent(events[3], scrollExpected = scrollDelta3)
            assertScrollEvent(events[4], scrollExpected = scrollDelta4)
        }
    }

    @Test
    fun mouseScroll_ignoredAsDownEvent() {
        var layoutCoordinates: LayoutCoordinates? = null
        val latch = CountDownLatch(1)
        val events = mutableListOf<PointerEvent>()
        val scrollDelta = Offset(0.35f, 0.65f)
        rule.runOnUiThread {
            container.setContent {
                Box(
                    Modifier.fillMaxSize()
                        .onGloballyPositioned {
                            layoutCoordinates = it
                            latch.countDown()
                        }
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    event.changes[0].consume()
                                    events += event
                                }
                            }
                        }
                )
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        dispatchMouseEvent(ACTION_SCROLL, layoutCoordinates!!, scrollDelta = scrollDelta)
        rule.runOnUiThread {
            assertThat(events).hasSize(2) // hover enter + scroll
            assertThat(events[1].changes).isNotEmpty()
            assertThat(events[1].changes[0].changedToDown()).isFalse()
        }
    }

    @Test
    fun mousePress_ignoresHoverExitOnPress() {
        lateinit var layoutCoordinates: LayoutCoordinates
        val latch = CountDownLatch(1)
        val events = mutableListOf<PointerEvent>()
        rule.runOnUiThread {
            container.setContent {
                Box(
                    Modifier.fillMaxSize()
                        .onGloballyPositioned {
                            layoutCoordinates = it
                            latch.countDown()
                        }
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    event.changes[0].consume()
                                    events += event
                                }
                            }
                        }
                )
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))

        rule.runOnUiThread {
            val root = layoutCoordinates.findRootCoordinates()
            val pos = root.localPositionOf(layoutCoordinates, Offset.Zero)
            val pointerCoords = PointerCoords(pos.x, pos.y)
            val pointerProperties = PointerProperties(0).apply { toolType = TOOL_TYPE_MOUSE }
            val androidComposeView = findAndroidComposeView(container) as AndroidComposeView

            val hoverExitEvent =
                MotionEvent(
                    eventTime = 0,
                    action = ACTION_HOVER_EXIT,
                    numPointers = 1,
                    actionIndex = 0,
                    pointerProperties = arrayOf(pointerProperties),
                    pointerCoords = arrayOf(pointerCoords),
                    buttonState = 0,
                )
            androidComposeView.dispatchHoverEvent(hoverExitEvent)

            val downEvent =
                MotionEvent(
                    eventTime = 0,
                    action = ACTION_DOWN,
                    numPointers = 1,
                    actionIndex = 0,
                    pointerProperties = arrayOf(pointerProperties),
                    pointerCoords = arrayOf(pointerCoords),
                )
            androidComposeView.dispatchTouchEvent(downEvent)
        }

        rule.runOnUiThread {
            assertThat(events).hasSize(1)
            assertThat(events.single().type).isEqualTo(PointerEventType.Press)
        }
    }

    @Test
    fun hoverEnterPressExitEnterExitRelease() {
        var outerCoordinates: LayoutCoordinates? = null
        var innerCoordinates: LayoutCoordinates? = null
        val latch = CountDownLatch(1)
        val eventLog = mutableListOf<PointerEvent>()
        rule.runOnUiThread {
            container.setContent {
                Box(
                    Modifier.fillMaxSize().onGloballyPositioned {
                        outerCoordinates = it
                        latch.countDown()
                    }
                ) {
                    Box(
                        Modifier.align(Alignment.Center)
                            .size(50.dp)
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        event.changes[0].consume()
                                        eventLog += event
                                    }
                                }
                            }
                            .onGloballyPositioned { innerCoordinates = it }
                    )
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        dispatchMouseEvent(ACTION_HOVER_ENTER, outerCoordinates!!)
        dispatchMouseEvent(ACTION_HOVER_MOVE, innerCoordinates!!)
        dispatchMouseEvent(ACTION_DOWN, innerCoordinates!!)
        dispatchMouseEvent(ACTION_MOVE, outerCoordinates!!)
        dispatchMouseEvent(ACTION_MOVE, innerCoordinates!!)
        dispatchMouseEvent(ACTION_MOVE, outerCoordinates!!)
        dispatchMouseEvent(ACTION_UP, outerCoordinates!!)
        rule.runOnUiThread {
            assertThat(eventLog).hasSize(6)
            assertThat(eventLog[0].type).isEqualTo(PointerEventType.Enter)
            assertThat(eventLog[1].type).isEqualTo(PointerEventType.Press)
            assertThat(eventLog[2].type).isEqualTo(PointerEventType.Exit)
            assertThat(eventLog[3].type).isEqualTo(PointerEventType.Enter)
            assertThat(eventLog[4].type).isEqualTo(PointerEventType.Exit)
            assertThat(eventLog[5].type).isEqualTo(PointerEventType.Release)
        }
    }

    @Test
    fun hoverPressEnterRelease() {
        var missCoordinates: LayoutCoordinates? = null
        var hitCoordinates: LayoutCoordinates? = null
        val latch = CountDownLatch(1)
        val eventLog = mutableListOf<PointerEvent>()
        rule.runOnUiThread {
            container.setContent {
                Box(Modifier.fillMaxSize()) {
                    Box(
                        Modifier.align(AbsoluteAlignment.TopLeft)
                            .size(50.dp)
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        awaitPointerEvent()
                                    }
                                }
                            }
                            .onGloballyPositioned {
                                missCoordinates = it
                                latch.countDown()
                            }
                    )
                    Box(
                        Modifier.align(AbsoluteAlignment.BottomRight)
                            .size(50.dp)
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        event.changes[0].consume()
                                        eventLog += event
                                    }
                                }
                            }
                            .onGloballyPositioned { hitCoordinates = it }
                    )
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        dispatchMouseEvent(ACTION_HOVER_ENTER, missCoordinates!!)
        dispatchMouseEvent(ACTION_HOVER_EXIT, missCoordinates!!)
        dispatchMouseEvent(ACTION_DOWN, missCoordinates!!)
        dispatchMouseEvent(ACTION_MOVE, hitCoordinates!!)
        dispatchMouseEvent(ACTION_UP, hitCoordinates!!)
        dispatchMouseEvent(ACTION_HOVER_ENTER, hitCoordinates!!)
        rule.runOnUiThread {
            assertThat(eventLog).hasSize(1)
            assertThat(eventLog[0].type).isEqualTo(PointerEventType.Enter)
        }
    }

    @Test
    fun pressInsideExitWindow() {
        var innerCoordinates: LayoutCoordinates? = null
        val latch = CountDownLatch(1)
        val eventLog = mutableListOf<PointerEvent>()
        rule.runOnUiThread {
            container.setContent {
                Box(Modifier.fillMaxSize()) {
                    Box(
                        Modifier.align(Alignment.BottomCenter)
                            .size(50.dp)
                            .graphicsLayer { translationY = 25.dp.roundToPx().toFloat() }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        event.changes[0].consume()
                                        eventLog += event
                                    }
                                }
                            }
                            .onGloballyPositioned {
                                innerCoordinates = it
                                latch.countDown()
                            }
                    )
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        val coords = innerCoordinates!!
        dispatchMouseEvent(ACTION_HOVER_ENTER, coords)
        dispatchMouseEvent(ACTION_DOWN, coords)
        dispatchMouseEvent(ACTION_MOVE, coords, Offset(0f, coords.size.height / 2 - 1f))
        dispatchMouseEvent(ACTION_MOVE, coords, Offset(0f, coords.size.height - 1f))
        dispatchMouseEvent(ACTION_UP, coords, Offset(0f, coords.size.height - 1f))
        rule.runOnUiThread {
            assertThat(eventLog).hasSize(5)
            assertThat(eventLog[0].type).isEqualTo(PointerEventType.Enter)
            assertThat(eventLog[1].type).isEqualTo(PointerEventType.Press)
            assertThat(eventLog[2].type).isEqualTo(PointerEventType.Move)
            assertThat(eventLog[3].type).isEqualTo(PointerEventType.Exit)
            assertThat(eventLog[4].type).isEqualTo(PointerEventType.Release)
        }
    }

    @Test
    fun pressInsideClippedContent() {
        var innerCoordinates: LayoutCoordinates? = null
        val latch = CountDownLatch(1)
        val eventLog = mutableListOf<PointerEvent>()
        rule.runOnUiThread {
            container.setContent {
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.align(Alignment.TopCenter).requiredSize(50.dp).clipToBounds()) {
                        Box(
                            Modifier.requiredSize(50.dp)
                                .graphicsLayer { translationY = 25.dp.roundToPx().toFloat() }
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            event.changes[0].consume()
                                            eventLog += event
                                        }
                                    }
                                }
                                .onGloballyPositioned {
                                    innerCoordinates = it
                                    latch.countDown()
                                }
                        )
                    }
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        val coords = innerCoordinates!!
        dispatchMouseEvent(ACTION_HOVER_ENTER, coords)
        dispatchMouseEvent(ACTION_DOWN, coords)
        dispatchMouseEvent(ACTION_MOVE, coords, Offset(0f, coords.size.height - 1f))
        dispatchMouseEvent(ACTION_UP, coords, Offset(0f, coords.size.height - 1f))
        dispatchMouseEvent(ACTION_HOVER_ENTER, coords, Offset(0f, coords.size.height - 1f))
        rule.runOnUiThread {
            assertThat(eventLog).hasSize(5)
            assertThat(eventLog[0].type).isEqualTo(PointerEventType.Enter)
            assertThat(eventLog[1].type).isEqualTo(PointerEventType.Press)
            assertThat(eventLog[2].type).isEqualTo(PointerEventType.Move)
            assertThat(eventLog[3].type).isEqualTo(PointerEventType.Release)
            assertThat(eventLog[4].type).isEqualTo(PointerEventType.Exit)
        }
    }

    private fun setSimpleLayout(eventLog: MutableList<PointerEvent>): LayoutCoordinates {
        var innerCoordinates: LayoutCoordinates? = null
        val latch = CountDownLatch(1)
        rule.runOnUiThread {
            container.setContent {
                Box(
                    Modifier.fillMaxSize()
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    event.changes[0].consume()
                                    eventLog += event
                                }
                            }
                        }
                        .onGloballyPositioned {
                            innerCoordinates = it
                            latch.countDown()
                        }
                )
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        return innerCoordinates!!
    }

    @Test
    fun cancelOnDeviceChange() {
        // When a pointer has had a surprise removal, a "cancel" event should be sent if it was
        // pressed.
        val eventLog = mutableListOf<PointerEvent>()
        val coords = setSimpleLayout(eventLog)

        dispatchMouseEvent(ACTION_HOVER_ENTER, coords)
        dispatchMouseEvent(ACTION_DOWN, coords)
        dispatchMouseEvent(ACTION_MOVE, coords, Offset(0f, 1f))

        val motionEvent =
            MotionEvent(
                5,
                ACTION_DOWN,
                1,
                0,
                arrayOf(PointerProperties(10).also { it.toolType = TOOL_TYPE_FINGER }),
                arrayOf(PointerCoords(1f, 1f))
            )

        container.dispatchTouchEvent(motionEvent)
        rule.runOnUiThread {
            assertThat(eventLog).hasSize(5)
            assertThat(eventLog[0].type).isEqualTo(PointerEventType.Enter)
            assertThat(eventLog[1].type).isEqualTo(PointerEventType.Press)
            assertThat(eventLog[2].type).isEqualTo(PointerEventType.Move)
            assertThat(eventLog[3].type).isEqualTo(PointerEventType.Release)
            assertThat(eventLog[4].type).isEqualTo(PointerEventType.Press)
        }
    }

    @Test
    fun testSyntheticEventPosition() {
        val eventLog = mutableListOf<PointerEvent>()
        val coords = setSimpleLayout(eventLog)
        dispatchMouseEvent(ACTION_DOWN, coords)

        rule.runOnUiThread {
            assertThat(eventLog).hasSize(2)
            assertThat(eventLog[0].changes[0].position).isEqualTo(eventLog[1].changes[0].position)
        }
    }

    @Test
    fun testStylusHoverExitDueToPress() {
        val eventLog = mutableListOf<PointerEvent>()
        val coords = setSimpleLayout(eventLog)

        dispatchStylusEvents(coords, Offset.Zero, ACTION_HOVER_ENTER)
        dispatchStylusEvents(coords, Offset.Zero, ACTION_HOVER_EXIT, ACTION_DOWN)

        rule.runOnUiThread {
            assertThat(eventLog).hasSize(2)
            assertThat(eventLog[0].type).isEqualTo(PointerEventType.Enter)
            assertThat(eventLog[1].type).isEqualTo(PointerEventType.Press)
        }
    }

    @Test
    fun testStylusHoverExitNoFollowingEvent() {
        val eventLog = mutableListOf<PointerEvent>()
        val coords = setSimpleLayout(eventLog)

        // Exit followed by nothing should just send the exit
        dispatchStylusEvents(coords, Offset.Zero, ACTION_HOVER_ENTER)
        dispatchStylusEvents(coords, Offset.Zero, ACTION_HOVER_EXIT)

        // Hover exit events in Compose are always delayed two frames to ensure Compose does not
        // trigger them if they are followed by a press in the next frame. This accounts for that.
        rule.waitForFutureFrame(2)

        rule.runOnUiThread {
            assertThat(eventLog).hasSize(2)
            assertThat(eventLog[0].type).isEqualTo(PointerEventType.Enter)
            assertThat(eventLog[1].type).isEqualTo(PointerEventType.Exit)
        }
    }

    @Test
    fun testStylusHoverExitWithFollowingHoverEvent() {
        val eventLog = mutableListOf<PointerEvent>()
        val coords = setSimpleLayout(eventLog)

        // Exit immediately followed by enter with the same device should send both
        dispatchStylusEvents(coords, Offset.Zero, ACTION_HOVER_ENTER)
        dispatchStylusEvents(coords, Offset.Zero, ACTION_HOVER_EXIT, ACTION_HOVER_ENTER)

        rule.runOnUiThread {
            assertThat(eventLog).hasSize(3)
            assertThat(eventLog[0].type).isEqualTo(PointerEventType.Enter)
            assertThat(eventLog[1].type).isEqualTo(PointerEventType.Exit)
            assertThat(eventLog[2].type).isEqualTo(PointerEventType.Enter)
        }
    }

    @Test
    fun testStylusHoverExitWithFollowingTouchEvent() {
        val eventLog = mutableListOf<PointerEvent>()
        val coords = setSimpleLayout(eventLog)

        // Exit followed by cancel should send both
        dispatchStylusEvents(coords, Offset.Zero, ACTION_HOVER_ENTER)
        dispatchStylusEvents(coords, Offset.Zero, ACTION_HOVER_EXIT, ACTION_CANCEL)

        rule.runOnUiThread {
            assertThat(eventLog).hasSize(2)
            assertThat(eventLog[0].type).isEqualTo(PointerEventType.Enter)
            assertThat(eventLog[1].type).isEqualTo(PointerEventType.Exit)
        }
    }

    @Test
    fun testStylusHoverExitWithFollowingDownOnDifferentDevice() {
        val eventLog = mutableListOf<PointerEvent>()
        val coords = setSimpleLayout(eventLog)

        // Exit followed by a different device should send the exit
        dispatchStylusEvents(coords, Offset.Zero, ACTION_HOVER_ENTER)
        rule.runOnUiThread {
            val root = coords.findRootCoordinates()
            val pos = root.localPositionOf(coords, Offset.Zero)
            val androidComposeView = findAndroidComposeView(container) as AndroidComposeView
            val exit =
                MotionEvent(
                    0,
                    ACTION_HOVER_EXIT,
                    1,
                    0,
                    arrayOf(
                        PointerProperties(0).also { it.toolType = MotionEvent.TOOL_TYPE_STYLUS }
                    ),
                    arrayOf(PointerCoords(pos.x, pos.y))
                )

            androidComposeView.dispatchHoverEvent(exit)

            val down =
                MotionEvent(
                    0,
                    ACTION_DOWN,
                    1,
                    0,
                    arrayOf(PointerProperties(0).also { it.toolType = TOOL_TYPE_FINGER }),
                    arrayOf(PointerCoords(pos.x, pos.y))
                )
            androidComposeView.dispatchTouchEvent(down)
        }

        rule.runOnUiThread {
            assertThat(eventLog).hasSize(3)
            assertThat(eventLog[0].type).isEqualTo(PointerEventType.Enter)
            assertThat(eventLog[1].type).isEqualTo(PointerEventType.Exit)
            assertThat(eventLog[2].type).isEqualTo(PointerEventType.Press)
        }
    }

    @Test
    fun syntheticEventSentAfterUp() {
        val eventLog = mutableListOf<PointerEvent>()
        val coords = setSimpleLayout(eventLog)

        dispatchMouseEvent(ACTION_HOVER_ENTER, coords)
        dispatchMouseEvent(ACTION_DOWN, coords)
        dispatchMouseEvent(ACTION_UP, coords)
        dispatchStylusEvents(coords, Offset.Zero, ACTION_HOVER_ENTER)

        rule.runOnUiThread {
            assertThat(eventLog).hasSize(5)
            assertThat(eventLog[0].type).isEqualTo(PointerEventType.Enter)
            assertThat(eventLog[1].type).isEqualTo(PointerEventType.Press)
            assertThat(eventLog[2].type).isEqualTo(PointerEventType.Release)
            assertThat(eventLog[3].type).isEqualTo(PointerEventType.Exit)
            assertThat(eventLog[4].type).isEqualTo(PointerEventType.Enter)
        }
    }

    @Test
    fun clippedHasNoInputIfLargeEnough() {
        val eventLog = mutableListOf<PointerEventType>()
        var innerCoordinates: LayoutCoordinates? = null
        val latch = CountDownLatch(1)
        rule.runOnUiThread {
            container.setContent {
                Column(Modifier.fillMaxSize()) {
                    Box(
                        Modifier.size(50.dp).pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent()
                                }
                            }
                        }
                    )
                    Box(Modifier.size(50.dp).clipToBounds()) {
                        Box(
                            Modifier.size(50.dp)
                                .graphicsLayer { translationY = -25.dp.roundToPx().toFloat() }
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            event.changes[0].consume()
                                            eventLog += event.type
                                        }
                                    }
                                }
                                .onGloballyPositioned {
                                    innerCoordinates = it
                                    latch.countDown()
                                }
                        )
                    }
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))

        val coords = innerCoordinates!!

        // Hit the top Box
        dispatchMouseEvent(ACTION_HOVER_ENTER, coords, Offset(0f, -1f))

        // Hit the bottom box, but clipped
        dispatchMouseEvent(ACTION_HOVER_MOVE, coords)
        dispatchMouseEvent(
            ACTION_HOVER_MOVE,
            coords,
            Offset(0f, (coords.size.height / 2 - 1).toFloat())
        )

        rule.runOnUiThread { assertThat(eventLog).isEmpty() }

        // Now hit the box in the unclipped region
        dispatchMouseEvent(
            ACTION_HOVER_MOVE,
            coords,
            Offset(0f, (coords.size.height / 2 + 1).toFloat())
        )

        // Now hit the bottom of the clipped region
        dispatchMouseEvent(
            ACTION_HOVER_MOVE,
            coords,
            Offset(0f, (coords.size.height - 1).toFloat())
        )

        // Now leave
        dispatchMouseEvent(ACTION_HOVER_MOVE, coords, Offset(0f, coords.size.height.toFloat() + 1f))

        rule.runOnUiThread {
            assertThat(eventLog)
                .containsExactly(
                    PointerEventType.Enter,
                    PointerEventType.Move,
                    PointerEventType.Exit
                )
        }
    }

    @Test
    fun unclippedTakesPrecedenceWithMinimumTouchTarget() {
        val eventLog = mutableListOf<PointerEventType>()
        var innerCoordinates: LayoutCoordinates? = null
        val latch = CountDownLatch(1)
        rule.runOnUiThread {
            container.setContent {
                Column(Modifier.fillMaxSize()) {
                    Box(
                        Modifier.size(50.dp).pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent()
                                }
                            }
                        }
                    )
                    Box(Modifier.size(20.dp).clipToBounds()) {
                        Box(
                            Modifier.size(20.dp)
                                .graphicsLayer { translationY = -10.dp.roundToPx().toFloat() }
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            event.changes[0].consume()
                                            eventLog += event.type
                                        }
                                    }
                                }
                                .onGloballyPositioned {
                                    innerCoordinates = it
                                    latch.countDown()
                                }
                        )
                    }
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))

        val coords = innerCoordinates!!

        // Hit the top Box, but in the minimum touch target area of the bottom Box
        dispatchTouchEvent(ACTION_DOWN, coords, Offset(0f, -1f))
        dispatchTouchEvent(ACTION_UP, coords, Offset(0f, -1f))

        // Hit the top Box in the clipped region of the bottom Box
        dispatchMouseEvent(ACTION_DOWN, coords)
        dispatchMouseEvent(ACTION_UP, coords)

        rule.runOnUiThread { assertThat(eventLog).isEmpty() }

        // Hit the bottom box in the unclipped region
        val topOfUnclipped = Offset(0f, (coords.size.height / 2 + 1).toFloat())
        dispatchMouseEvent(ACTION_DOWN, coords, topOfUnclipped)
        dispatchMouseEvent(ACTION_UP, coords, topOfUnclipped)

        // Continue to the bottom of the bottom Box
        val bottomOfBox = Offset(0f, (coords.size.height - 1).toFloat())
        dispatchMouseEvent(ACTION_DOWN, coords, bottomOfBox)
        dispatchMouseEvent(ACTION_UP, coords, bottomOfBox)

        // Now exit the bottom box
        val justBelow = Offset(0f, (coords.size.height + 1).toFloat())
        dispatchMouseEvent(ACTION_DOWN, coords, justBelow)
        dispatchMouseEvent(ACTION_UP, coords, justBelow)

        rule.runOnUiThread {
            assertThat(eventLog)
                .containsExactly(
                    PointerEventType.Press,
                    PointerEventType.Release,
                    PointerEventType.Press,
                    PointerEventType.Release,
                    PointerEventType.Press,
                    PointerEventType.Release,
                )
        }
    }

    @Test
    fun stylusEnterExitPointerArea() {
        // Stylus hover enter/exit events should be sent to pointer input areas
        val eventLog = mutableListOf<PointerEvent>()
        var innerCoordinates: LayoutCoordinates? = null
        val latch = CountDownLatch(1)
        rule.runOnUiThread {
            container.setContent {
                Box(
                    Modifier.fillMaxSize().pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent()
                            }
                        }
                    }
                ) {
                    Box(
                        Modifier.size(50.dp)
                            .align(AbsoluteAlignment.BottomRight)
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        event.changes.forEach { it.consume() }
                                        eventLog += event
                                    }
                                }
                            }
                            .onGloballyPositioned {
                                innerCoordinates = it
                                latch.countDown()
                            }
                    )
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))

        val coords = innerCoordinates!!
        val outside = Offset(-100f, -100f)
        dispatchStylusEvents(coords, outside, ACTION_HOVER_ENTER)
        rule.runOnUiThread {
            // The event didn't land inside the box, so it shouldn't get the hover enter
            assertThat(eventLog).isEmpty()
        }
        dispatchStylusEvents(coords, Offset.Zero, ACTION_HOVER_MOVE)
        dispatchStylusEvents(coords, Offset.Zero, ACTION_HOVER_EXIT, ACTION_DOWN)
        dispatchStylusEvents(coords, outside, ACTION_MOVE)
        dispatchStylusEvents(coords, outside, ACTION_UP, ACTION_HOVER_ENTER)
        rule.runOnUiThread {
            assertThat(eventLog).hasSize(4)
            assertThat(eventLog[0].type).isEqualTo(PointerEventType.Enter)
            assertThat(eventLog[1].type).isEqualTo(PointerEventType.Press)
            assertThat(eventLog[2].type).isEqualTo(PointerEventType.Exit)
            assertThat(eventLog[3].type).isEqualTo(PointerEventType.Release)
        }
    }

    @Test
    fun restartStreamAfterNotProcessing() {
        // Stylus hover enter/exit events should be sent to pointer input areas
        val eventLog = mutableListOf<PointerEvent>()
        var hitCoordinates: LayoutCoordinates? = null
        var missCoordinates: LayoutCoordinates? = null
        val latch = CountDownLatch(2)
        rule.runOnUiThread {
            container.setContent {
                Box(Modifier.fillMaxSize()) {
                    Box(
                        Modifier.size(50.dp)
                            .align(AbsoluteAlignment.TopLeft)
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        event.changes.forEach { it.consume() }
                                        eventLog += event
                                    }
                                }
                            }
                            .onGloballyPositioned {
                                hitCoordinates = it
                                latch.countDown()
                            }
                    )
                    Box(
                        Modifier.size(50.dp)
                            .align(AbsoluteAlignment.BottomRight)
                            .onGloballyPositioned {
                                missCoordinates = it
                                latch.countDown()
                            }
                    )
                }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        val miss = missCoordinates!!
        val hit = hitCoordinates!!

        // This should hit
        dispatchTouchEvent(ACTION_DOWN, hit)
        dispatchTouchEvent(ACTION_UP, hit)

        // This should miss
        dispatchTouchEvent(ACTION_DOWN, miss)

        // This should hit
        dispatchTouchEvent(ACTION_DOWN, hit)

        rule.runOnUiThread {
            assertThat(eventLog).hasSize(3)
            val down1 = eventLog[0]
            val up1 = eventLog[1]
            val down2 = eventLog[2]
            assertThat(down1.changes).hasSize(1)
            assertThat(up1.changes).hasSize(1)
            assertThat(down2.changes).hasSize(1)

            assertThat(down1.type).isEqualTo(PointerEventType.Press)
            assertThat(up1.type).isEqualTo(PointerEventType.Release)
            assertThat(down2.type).isEqualTo(PointerEventType.Press)

            assertThat(up1.changes[0].id).isEqualTo(down1.changes[0].id)
            assertThat(down2.changes[0].id.value).isEqualTo(down1.changes[0].id.value + 2)
        }
    }

    private fun createPointerEventAt(eventTime: Int, action: Int, locationInWindow: IntArray) =
        MotionEvent(
            eventTime,
            action,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(locationInWindow[0].toFloat(), locationInWindow[1].toFloat()))
        )
}

@Composable
fun AndroidWithCompose(context: Context, androidPadding: Int, content: @Composable () -> Unit) {
    val anotherLayout =
        ComposeView(context).also { view ->
            view.setContent { content() }
            view.setPadding(androidPadding, androidPadding, androidPadding, androidPadding)
        }
    AndroidView({ anotherLayout })
}

fun Modifier.consumeMovementGestureFilter(consumeMovement: Boolean = false): Modifier = composed {
    val filter = remember(consumeMovement) { ConsumeMovementGestureFilter(consumeMovement) }
    PointerInputModifierImpl(filter)
}

fun Modifier.consumeDownGestureFilter(onDown: (Offset) -> Unit): Modifier = composed {
    val filter = remember { ConsumeDownChangeFilter() }
    filter.onDown = onDown
    this.then(PointerInputModifierImpl(filter))
}

fun Modifier.logEventsGestureFilter(log: MutableList<List<PointerInputChange>>): Modifier =
    composed {
        val filter = remember { LogEventsGestureFilter(log) }
        this.then(PointerInputModifierImpl(filter))
    }

private class PointerInputModifierImpl(override val pointerInputFilter: PointerInputFilter) :
    PointerInputModifier

private class ConsumeMovementGestureFilter(val consumeMovement: Boolean) : PointerInputFilter() {
    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        if (consumeMovement) {
            pointerEvent.changes.fastForEach { it.consume() }
        }
    }

    override fun onCancel() {}
}

private class ConsumeDownChangeFilter : PointerInputFilter() {
    var onDown by mutableStateOf<(Offset) -> Unit>({})

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        pointerEvent.changes.fastForEach {
            if (it.changedToDown()) {
                onDown(it.position)
                it.consume()
            }
        }
    }

    override fun onCancel() {}
}

private class LogEventsGestureFilter(val log: MutableList<List<PointerInputChange>>) :
    PointerInputFilter() {

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        if (pass == PointerEventPass.Initial) {
            log.add(pointerEvent.changes.map { it.copy() })
        }
    }

    override fun onCancel() {}
}

@Suppress("TestFunctionName")
@Composable
private fun FillLayout(modifier: Modifier = Modifier) {
    Layout({}, modifier) { _, constraints ->
        layout(constraints.maxWidth, constraints.maxHeight) {}
    }
}

private fun countDown(block: (CountDownLatch) -> Unit) {
    val countDownLatch = CountDownLatch(1)
    block(countDownLatch)
    assertThat(countDownLatch.await(1, TimeUnit.SECONDS)).isTrue()
}

class AndroidPointerInputTestActivity : ComponentActivity()

private fun MotionEvent(
    eventTime: Int,
    action: Int,
    numPointers: Int,
    actionIndex: Int,
    pointerProperties: Array<MotionEvent.PointerProperties>,
    pointerCoords: Array<MotionEvent.PointerCoords>,
    buttonState: Int =
        if (
            pointerProperties[0].toolType == TOOL_TYPE_MOUSE &&
                (action == ACTION_DOWN || action == ACTION_MOVE)
        )
            MotionEvent.BUTTON_PRIMARY
        else 0,
): MotionEvent {
    val source =
        if (pointerProperties[0].toolType == TOOL_TYPE_MOUSE) {
            InputDevice.SOURCE_MOUSE
        } else {
            InputDevice.SOURCE_TOUCHSCREEN
        }
    return MotionEvent.obtain(
        0,
        eventTime.toLong(),
        action + (actionIndex shl ACTION_POINTER_INDEX_SHIFT),
        numPointers,
        pointerProperties,
        pointerCoords,
        0,
        buttonState,
        0f,
        0f,
        0,
        0,
        source,
        0
    )
}

/*
 * Version of MotionEvent() that accepts classification.
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
private fun MotionEvent(
    eventTime: Int,
    action: Int,
    numPointers: Int,
    actionIndex: Int,
    pointerProperties: Array<MotionEvent.PointerProperties>,
    pointerCoords: Array<MotionEvent.PointerCoords>,
    buttonState: Int =
        if (
            pointerProperties[0].toolType == TOOL_TYPE_MOUSE &&
                (action == ACTION_DOWN || action == ACTION_MOVE)
        )
            MotionEvent.BUTTON_PRIMARY
        else 0,
    classification: Int
): MotionEvent {
    val source =
        if (pointerProperties[0].toolType == TOOL_TYPE_MOUSE) {
            InputDevice.SOURCE_MOUSE
        } else {
            InputDevice.SOURCE_TOUCHSCREEN
        }
    return MotionEvent.obtain(
        0,
        eventTime.toLong(),
        action + (actionIndex shl ACTION_POINTER_INDEX_SHIFT),
        numPointers,
        pointerProperties,
        pointerCoords,
        0,
        buttonState,
        0f,
        0f,
        0,
        0,
        source,
        0,
        0,
        classification
    )!!
}

internal fun findRootView(view: View): View {
    val parent = view.parent
    if (parent is View) {
        return findRootView(parent)
    }
    return view
}
