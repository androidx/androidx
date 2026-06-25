/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.xr.glimmer

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.indirect.IndirectPointerEvent
import androidx.compose.ui.input.indirect.IndirectPointerEventPrimaryDirectionalMotionAxis
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.inputDeviceCenter
import androidx.compose.ui.test.inputDeviceCenterLeft
import androidx.compose.ui.test.inputDeviceCenterRight
import androidx.compose.ui.test.inputDeviceTopLeft
import androidx.compose.ui.test.inputDeviceTopRight
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.sendIndirectPointerInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.xr.glimmer.testutils.createGlimmerRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
// The expected min sdk is 35, but we test on 33 for wider device coverage (some APIs are not
// available below 33)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalComposeUiApi::class)
class IndirectPointerGestureTest {

    @get:Rule(0) val rule = createComposeRule(StandardTestDispatcher())

    @get:Rule(1) val glimmerRule = createGlimmerRule()

    @Test
    fun gestures_areIgnored_whenDisabled() {
        var gestureCount = 0

        rule.setContent {
            Box(
                modifier =
                    Modifier.testTag(ROOT_TEST_TAG)
                        .size(10.dp)
                        .onIndirectPointerGesture(
                            enabled = false,
                            onSwipeForward = { gestureCount++ },
                            onSwipeBackward = { gestureCount++ },
                            onClick = { gestureCount++ },
                        )
                        .focusTarget()
            )
        }

        // Perform all known gestures
        rule.sendGlimmerIndirectPointerInput {
            click()
            swipeRight()
            swipeLeft()
        }

        rule.runOnIdle { assertThat(gestureCount).isEqualTo(0) }
    }

    @Test
    fun click_triggersOnClick() {
        var onClickCount = 0

        rule.setContent {
            Box(
                modifier =
                    Modifier.testTag(ROOT_TEST_TAG)
                        .size(10.dp)
                        .onIndirectPointerGesture(enabled = true, onClick = { onClickCount++ })
                        .focusTarget()
            )
        }

        rule.sendGlimmerIndirectPointerInput { click() }

        rule.runOnIdle { assertThat(onClickCount).isEqualTo(1) }
    }

    @Test
    fun longPress_withinSlop_isTreatedAsClick() {
        var onClickCount = 0
        var onSwipeForwardCount = 0
        var onSwipeBackwardCount = 0

        var longPressTimeoutMillis = 0L

        rule.setContent {
            longPressTimeoutMillis = LocalViewConfiguration.current.longPressTimeoutMillis
            Box(
                modifier =
                    Modifier.testTag(ROOT_TEST_TAG)
                        .size(10.dp)
                        .onIndirectPointerGesture(
                            enabled = true,
                            onSwipeForward = { onSwipeForwardCount++ },
                            onSwipeBackward = { onSwipeBackwardCount++ },
                            onClick = { onClickCount++ },
                        )
                        .focusTarget()
            )
        }

        // Perform a click that lasts longer than the long press timeout
        rule.sendGlimmerIndirectPointerInput {
            down(Offset.Zero)
            advanceEventTime(longPressTimeoutMillis + 50L)
            up()
        }

        // A click event *should* be fired, as the code doesn't check time.
        rule.runOnIdle {
            assertThat(onClickCount).isEqualTo(1)
            assertThat(onSwipeForwardCount).isEqualTo(0)
            assertThat(onSwipeBackwardCount).isEqualTo(0)
        }
    }

    @Test
    fun swipeForward_triggersOnSwipeForward() {
        var onClickCount = 0
        var onSwipeForwardCount = 0
        var onSwipeBackwardCount = 0

        rule.setContent {
            Box(
                modifier =
                    Modifier.testTag(ROOT_TEST_TAG)
                        .size(10.dp)
                        .onIndirectPointerGesture(
                            enabled = true,
                            onSwipeForward = { onSwipeForwardCount++ },
                            onSwipeBackward = { onSwipeBackwardCount++ },
                            onClick = { onClickCount++ },
                        )
                        .focusTarget()
            )
        }
        rule.sendGlimmerIndirectPointerInput { swipeRight() }

        rule.runOnIdle {
            assertThat(onClickCount).isEqualTo(0)
            assertThat(onSwipeForwardCount).isEqualTo(1)
            assertThat(onSwipeBackwardCount).isEqualTo(0)
        }
    }

    @Test
    fun swipeForward_withHistoricalBatch_triggersOnSwipeForward() {
        var onClickCount = 0
        var onSwipeForwardCount = 0
        var onSwipeBackwardCount = 0

        var touchSlop = 0f

        rule.setContent {
            touchSlop = LocalViewConfiguration.current.touchSlop
            Box(
                modifier =
                    Modifier.testTag(ROOT_TEST_TAG)
                        .size(10.dp)
                        .onIndirectPointerGesture(
                            enabled = true,
                            onSwipeForward = { onSwipeForwardCount++ },
                            onSwipeBackward = { onSwipeBackwardCount++ },
                            onClick = { onClickCount++ },
                        )
                        .focusTarget()
            )
        }

        val distance = touchSlop * 2f

        rule.sendIndirectPointerInput(
            indirectPointerEventPrimaryDirectionalMotionAxis =
                IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize = horizontalExternalInputDeviceSize,
        ) {
            val start = inputDeviceTopLeft
            down(start)
            updatePointerTo(0, start + Offset(distance, 0f))
            moveWithHistory(
                relativeHistoricalTimes = listOf(-80L, -60L, -40L, -20L),
                historicalCoordinates =
                    listOf(
                        start + Offset(distance * 0.2f, 0f),
                        start + Offset(distance * 0.4f, 0f),
                        start + Offset(distance * 0.6f, 0f),
                        start + Offset(distance * 0.8f, 0f),
                    ),
                delayMillis = 100L,
            )
            advanceEventTime(20L)
            up()
        }

        rule.runOnIdle {
            assertThat(onClickCount).isEqualTo(0)
            assertThat(onSwipeForwardCount).isEqualTo(1)
            assertThat(onSwipeBackwardCount).isEqualTo(0)
        }
    }

    @Test
    fun swipeBackward_withHistoricalBatch_triggersOnSwipeBackward() {
        var onClickCount = 0
        var onSwipeForwardCount = 0
        var onSwipeBackwardCount = 0

        var touchSlop = 0f

        rule.setContent {
            touchSlop = LocalViewConfiguration.current.touchSlop
            Box(
                modifier =
                    Modifier.testTag(ROOT_TEST_TAG)
                        .size(10.dp)
                        .onIndirectPointerGesture(
                            enabled = true,
                            onSwipeForward = { onSwipeForwardCount++ },
                            onSwipeBackward = { onSwipeBackwardCount++ },
                            onClick = { onClickCount++ },
                        )
                        .focusTarget()
            )
        }

        val distance = -(touchSlop * 2f)

        rule.sendIndirectPointerInput(
            indirectPointerEventPrimaryDirectionalMotionAxis =
                IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize = horizontalExternalInputDeviceSize,
        ) {
            val start = inputDeviceTopRight
            down(start)
            updatePointerTo(0, start + Offset(distance, 0f))
            moveWithHistory(
                relativeHistoricalTimes = listOf(-80L, -60L, -40L, -20L),
                historicalCoordinates =
                    listOf(
                        start + Offset(distance * 0.2f, 0f),
                        start + Offset(distance * 0.4f, 0f),
                        start + Offset(distance * 0.6f, 0f),
                        start + Offset(distance * 0.8f, 0f),
                    ),
                delayMillis = 100L,
            )
            advanceEventTime(20L)
            up()
        }

        rule.runOnIdle {
            assertThat(onClickCount).isEqualTo(0)
            assertThat(onSwipeForwardCount).isEqualTo(0)
            assertThat(onSwipeBackwardCount).isEqualTo(1)
        }
    }

    @Test
    fun swipeBackward_triggersOnSwipeBackward() {
        var onClickCount = 0
        var onSwipeForwardCount = 0
        var onSwipeBackwardCount = 0

        rule.setContent {
            Box(
                modifier =
                    Modifier.testTag(ROOT_TEST_TAG)
                        .size(10.dp)
                        .onIndirectPointerGesture(
                            enabled = true,
                            onSwipeForward = { onSwipeForwardCount++ },
                            onSwipeBackward = { onSwipeBackwardCount++ },
                            onClick = { onClickCount++ },
                        )
                        .focusTarget()
            )
        }

        rule.sendGlimmerIndirectPointerInput { swipeLeft() }

        rule.runOnIdle {
            assertThat(onClickCount).isEqualTo(0)
            assertThat(onSwipeForwardCount).isEqualTo(0)
            assertThat(onSwipeBackwardCount).isEqualTo(1)
        }
    }

    @Test
    fun swipeBackward_withStationarySecondPointer_triggersOnSwipeBackward() {
        var onClickCount = 0
        var onSwipeForwardCount = 0
        var onSwipeBackwardCount = 0

        rule.setContent {
            Box(
                modifier =
                    Modifier.testTag(ROOT_TEST_TAG)
                        .size(10.dp)
                        .onIndirectPointerGesture(
                            enabled = true,
                            onSwipeForward = { onSwipeForwardCount++ },
                            onSwipeBackward = { onSwipeBackwardCount++ },
                            onClick = { onClickCount++ },
                        )
                        .focusTarget()
            )
        }

        rule.sendGlimmerIndirectPointerInput {
            val start = inputDeviceCenterRight
            val end = inputDeviceCenterLeft

            down(0, start)
            advanceEventTime(defaultPeriodBetweenEventsMillis)
            down(1, start)
            advanceEventTime(defaultPeriodBetweenEventsMillis)

            // Building velocity for swipe
            repeat(defaultStepCountForMoveToVelocityTrigger) { i ->
                val fraction = (i + 1) / defaultStepCountForMoveToVelocityTrigger.toFloat()
                moveTo(
                    pointerId = 0,
                    position = start + (end - start) * fraction,
                    delayMillis = defaultDelayForMoveToVelocityTrigger,
                )
            }

            advanceEventTime(defaultPeriodBetweenEventsMillis)
            up(1)
            advanceEventTime(defaultPeriodBetweenEventsMillis)
            up(0)
        }

        rule.runOnIdle {
            assertThat(onClickCount).isEqualTo(0)
            assertThat(onSwipeForwardCount).isEqualTo(0)
            assertThat(onSwipeBackwardCount).isEqualTo(1)
        }
    }

    @Test
    fun swipeBackward_withOpposingSecondPointer_triggersOnSwipeBackward() {
        var onClickCount = 0
        var onSwipeForwardCount = 0
        var onSwipeBackwardCount = 0

        rule.setContent {
            Box(
                modifier =
                    Modifier.testTag(ROOT_TEST_TAG)
                        .size(10.dp)
                        .onIndirectPointerGesture(
                            enabled = true,
                            onSwipeForward = { onSwipeForwardCount++ },
                            onSwipeBackward = { onSwipeBackwardCount++ },
                            onClick = { onClickCount++ },
                        )
                        .focusTarget()
            )
        }

        rule.sendGlimmerIndirectPointerInput {
            val start = inputDeviceCenter
            val end0 = inputDeviceCenterLeft
            val end1 = inputDeviceCenterRight

            down(0, start)
            advanceEventTime(defaultPeriodBetweenEventsMillis)
            down(1, start)
            advanceEventTime(defaultPeriodBetweenEventsMillis)

            // Building velocity for swipe (both forward and back)
            repeat(defaultStepCountForMoveToVelocityTrigger) { i ->
                val fraction = (i + 1) / defaultStepCountForMoveToVelocityTrigger.toFloat()
                updatePointerTo(0, start + (end0 - start) * fraction)
                updatePointerTo(1, start + (end1 - start) * fraction)
                move(defaultDelayForMoveToVelocityTrigger)
            }

            advanceEventTime(defaultPeriodBetweenEventsMillis)
            up(1)
            advanceEventTime(defaultPeriodBetweenEventsMillis)
            up(0)
        }

        // Gesture is based on the primary pointer (p0)
        rule.runOnIdle {
            assertThat(onClickCount).isEqualTo(0)
            assertThat(onSwipeForwardCount).isEqualTo(0)
            assertThat(onSwipeBackwardCount).isEqualTo(1)
        }
    }

    @Test
    fun dragOutsideSlop_thenBackInside_cancelsClick() {
        var onClickCount = 0
        var onSwipeForwardCount = 0
        var onSwipeBackwardCount = 0

        var touchSlop = 0f

        rule.setContent {
            touchSlop = LocalViewConfiguration.current.touchSlop
            Box(
                modifier =
                    Modifier.testTag(ROOT_TEST_TAG)
                        .size(10.dp)
                        .onIndirectPointerGesture(
                            enabled = true,
                            onSwipeForward = { onSwipeForwardCount++ },
                            onSwipeBackward = { onSwipeBackwardCount++ },
                            onClick = { onClickCount++ },
                        )
                        .focusTarget()
            )
        }

        rule.sendGlimmerIndirectPointerInput {
            down(Offset.Zero)
            moveTo(Offset(touchSlop + 1f, 0f))
            moveTo(Offset.Zero)
            up()
        }

        rule.runOnIdle {
            assertThat(onClickCount).isEqualTo(0)
            assertThat(onSwipeForwardCount).isEqualTo(0)
            assertThat(onSwipeBackwardCount).isEqualTo(0)
        }
    }

    @Test
    fun gesturesFromSecondaryPointer_areIgnored() {
        var onClickCount = 0
        var onSwipeForwardCount = 0
        var onSwipeBackwardCount = 0

        rule.setContent {
            Box(
                modifier =
                    Modifier.testTag(ROOT_TEST_TAG)
                        .size(10.dp)
                        .onIndirectPointerGesture(
                            enabled = true,
                            onSwipeForward = { onSwipeForwardCount++ },
                            onSwipeBackward = { onSwipeBackwardCount++ },
                            onClick = { onClickCount++ },
                        )
                        .focusTarget()
            )
        }

        rule.sendGlimmerIndirectPointerInput {
            down(0, inputDeviceCenterLeft)
            down(1, inputDeviceCenterLeft)
            moveTo(1, inputDeviceCenterRight)
            up(1)
        }

        // The gesture should not be processed based on p1
        rule.runOnIdle {
            assertThat(onClickCount).isEqualTo(0)
            assertThat(onSwipeForwardCount).isEqualTo(0)
            assertThat(onSwipeBackwardCount).isEqualTo(0)
        }

        rule.sendGlimmerIndirectPointerInput { up(0) }

        // The gesture is processed based on p0, which was a click.
        rule.runOnIdle {
            assertThat(onClickCount).isEqualTo(1)
            assertThat(onSwipeForwardCount).isEqualTo(0)
            assertThat(onSwipeBackwardCount).isEqualTo(0)
        }
    }

    @Test
    fun gesturesFromSecondaryPointer_whenFirstPointerEventsAreConsumed_areIgnored() {
        var onClickCount = 0
        var onSwipeForwardCount = 0
        var onSwipeBackwardCount = 0

        rule.setContent {
            Box(
                modifier =
                    Modifier.testTag(ROOT_TEST_TAG)
                        .size(10.dp)
                        .onIndirectPointerGesture(
                            enabled = true,
                            onSwipeForward = { onSwipeForwardCount++ },
                            onSwipeBackward = { onSwipeBackwardCount++ },
                            onClick = { onClickCount++ },
                        )
                        // Consumes Pointer 0 press/release events.
                        .onIndirectPointerInput(
                            onEvent = { event: IndirectPointerEvent, pass: PointerEventPass ->
                                if (pass == PointerEventPass.Main) {
                                    event.consumeMatchingPress(0)
                                    event.consumeMatchingRelease(0)
                                }
                            },
                            onCancel = {},
                        )
                        .focusTarget()
            )
        }

        rule.sendGlimmerIndirectPointerInput {
            val start = inputDeviceCenterLeft
            val end = inputDeviceCenterRight

            // This down (0) is consumed (see content definition).
            down(0, start)

            advanceEventTime(defaultPeriodBetweenEventsMillis)
            down(1, start)
            advanceEventTime(defaultPeriodBetweenEventsMillis)

            // Building velocity for swipe on pointer 1.
            // Child should still be "locked" to pointer 0 (which is consumed by parent).
            repeat(defaultStepCountForMoveToVelocityTrigger) { i ->
                val fraction = (i + 1) / defaultStepCountForMoveToVelocityTrigger.toFloat()
                moveTo(
                    pointerId = 1,
                    position = start + (end - start) * fraction,
                    delayMillis = defaultDelayForMoveToVelocityTrigger,
                )
            }

            advanceEventTime(defaultPeriodBetweenEventsMillis)
            // This up (0) is consumed (see content definition).
            up(0)
            advanceEventTime(defaultPeriodBetweenEventsMillis)
            up(1)
        }

        // The gesture should still not be processed.
        rule.runOnIdle {
            assertThat(onClickCount).isEqualTo(0)
            assertThat(onSwipeForwardCount).isEqualTo(0)
            assertThat(onSwipeBackwardCount).isEqualTo(0)
        }
    }

    @Test
    fun swipe_belowDistanceThreshold_isIgnored() {
        var onClickCount = 0
        var onSwipeForwardCount = 0
        var onSwipeBackwardCount = 0

        var touchSlop = 0f

        rule.setContent {
            touchSlop = LocalViewConfiguration.current.touchSlop
            Box(
                modifier =
                    Modifier.testTag(ROOT_TEST_TAG)
                        .size(10.dp)
                        .onIndirectPointerGesture(
                            enabled = true,
                            onSwipeForward = { onSwipeForwardCount++ },
                            onSwipeBackward = { onSwipeBackwardCount++ },
                            onClick = { onClickCount++ },
                        )
                        .focusTarget()
            )
        }

        // Perform a swipe that is fast, but the distance is just *under* the threshold
        val swipeDistanceThreshold = touchSlop * 1.3f
        val swipeDistance = swipeDistanceThreshold * 0.98f

        rule.sendGlimmerIndirectPointerInput {
            swipe(start = Offset.Zero, end = Offset(swipeDistance, 0f), durationMillis = 10)
        }

        rule.runOnIdle {
            assertThat(onClickCount).isEqualTo(0)
            assertThat(onSwipeForwardCount).isEqualTo(0)
            assertThat(onSwipeBackwardCount).isEqualTo(0)
        }
    }

    @Test
    fun swipe_belowVelocityThreshold_isIgnored() {
        var onClickCount = 0
        var onSwipeForwardCount = 0
        var onSwipeBackwardCount = 0

        var touchSlop = 0f

        rule.setContent {
            touchSlop = LocalViewConfiguration.current.touchSlop
            Box(
                modifier =
                    Modifier.testTag(ROOT_TEST_TAG)
                        .size(10.dp)
                        .onIndirectPointerGesture(
                            enabled = true,
                            onSwipeForward = { onSwipeForwardCount++ },
                            onSwipeBackward = { onSwipeBackwardCount++ },
                            onClick = { onClickCount++ },
                        )
                        .focusTarget()
            )
        }

        rule.sendGlimmerIndirectPointerInput {
            down(Offset.Zero)
            // The resulting X velocity is 23, which is below the 34f threshold.
            val repeatCount = 100
            val eachDragMovement = (touchSlop * 1.4f) / repeatCount
            var currentX = 0f
            repeat(repeatCount) {
                currentX += eachDragMovement
                moveTo(Offset(currentX, 0f), 10L)
            }
            up()
        }

        rule.runOnIdle {
            assertThat(onClickCount).isEqualTo(0)
            assertThat(onSwipeForwardCount).isEqualTo(0)
            assertThat(onSwipeBackwardCount).isEqualTo(0)
        }
    }

    @Test
    fun swipe_withBacktracking_isIgnored() {
        var onClickCount = 0
        var onSwipeForwardCount = 0
        var onSwipeBackwardCount = 0

        var touchSlop = 0f

        rule.setContent {
            touchSlop = LocalViewConfiguration.current.touchSlop
            Box(
                modifier =
                    Modifier.testTag(ROOT_TEST_TAG)
                        .size(10.dp)
                        .onIndirectPointerGesture(
                            enabled = true,
                            onSwipeForward = { onSwipeForwardCount++ },
                            onSwipeBackward = { onSwipeBackwardCount++ },
                            onClick = { onClickCount++ },
                        )
                        .focusTarget()
            )
        }

        rule.sendGlimmerIndirectPointerInput {
            down(Offset.Zero)
            // Move forward
            moveBy(Offset(touchSlop * 4, 0f))
            // Move backward
            moveBy(Offset(touchSlop * -2, 0f))
            up()
        }

        rule.runOnIdle {
            assertThat(onClickCount).isEqualTo(0)
            assertThat(onSwipeForwardCount).isEqualTo(0)
            assertThat(onSwipeBackwardCount).isEqualTo(0)
        }
    }

    @Test
    fun onGesture_subsequentGesture_detectedAfterDescendantUpConsumption() {
        var onClickCount = 0
        var onSwipeForwardCount = 0
        var onSwipeBackwardCount = 0

        var childConsumedFirstRelease = false

        rule.setContent {
            Box(
                modifier =
                    Modifier.testTag(ROOT_TEST_TAG)
                        .size(10.dp)
                        .onIndirectPointerGesture(
                            enabled = true,
                            onSwipeForward = { onSwipeForwardCount++ },
                            onSwipeBackward = { onSwipeBackwardCount++ },
                            onClick = { onClickCount++ },
                        )
                        .focusTarget()
            ) {
                // Child will surgically consume first Pointer 0 release event.
                Box(
                    modifier =
                        Modifier.testTag(CHILD_TEST_TAG)
                            .size(10.dp)
                            .onIndirectPointerInput(
                                onEvent = { event: IndirectPointerEvent, pass: PointerEventPass ->
                                    if (
                                        pass == PointerEventPass.Main && !childConsumedFirstRelease
                                    ) {
                                        // Only consumes first gesture through
                                        val consumed = event.consumeMatchingRelease(0)
                                        if (consumed) {
                                            childConsumedFirstRelease = true
                                        }
                                    }
                                },
                                onCancel = {},
                            )
                            .focusable()
                )
            }
        }

        rule.onNodeWithTag(CHILD_TEST_TAG).requestFocus()

        rule.sendGlimmerIndirectPointerInput {
            down(inputDeviceCenter)
            moveTo(inputDeviceCenterRight)
            // This up (0) is consumed by child (see content definition).
            up()
        }

        // The consumed 'up' in gesture should reset so no callback is called.
        rule.runOnIdle {
            assertThat(onClickCount).isEqualTo(0)
            assertThat(onSwipeForwardCount).isEqualTo(0)
            assertThat(onSwipeBackwardCount).isEqualTo(0)
        }

        // A new, valid gesture should be processed correctly
        rule.sendGlimmerIndirectPointerInput { click() }

        rule.runOnIdle {
            assertThat(onClickCount).isEqualTo(1)
            assertThat(onSwipeForwardCount).isEqualTo(0)
            assertThat(onSwipeBackwardCount).isEqualTo(0)
        }
    }

    @Test
    fun onGesture_subsequentGesture_detectedAfterDescendantMoveConsumption() {
        var onClickCount = 0
        var onSwipeForwardCount = 0
        var onSwipeBackwardCount = 0

        var childConsumedFirstMove = false

        rule.setContent {
            Box(
                modifier =
                    Modifier.testTag(ROOT_TEST_TAG)
                        .size(10.dp)
                        .onIndirectPointerGesture(
                            enabled = true,
                            onSwipeForward = { onSwipeForwardCount++ },
                            onSwipeBackward = { onSwipeBackwardCount++ },
                            onClick = { onClickCount++ },
                        )
                        .focusTarget()
            ) {
                // Child will surgically consume first Pointer 0 release event.
                Box(
                    modifier =
                        Modifier.testTag(CHILD_TEST_TAG)
                            .size(10.dp)
                            .onIndirectPointerInput(
                                onEvent = { event: IndirectPointerEvent, pass: PointerEventPass ->
                                    if (pass == PointerEventPass.Main && !childConsumedFirstMove) {
                                        // Only consumes first gesture through
                                        val consumed = event.consumeMatchingMove(0)
                                        if (consumed) {
                                            childConsumedFirstMove = true
                                        }
                                    }
                                },
                                onCancel = {},
                            )
                            .focusable()
                )
            }
        }

        rule.onNodeWithTag(CHILD_TEST_TAG).requestFocus()

        rule.sendGlimmerIndirectPointerInput {
            down(0, inputDeviceCenter)
            // This move (0) is consumed by child (see content definition).
            moveTo(0, inputDeviceCenterRight)
            up(0)
        }

        // The consumed 'move' in gesture should reset so no callback is called.
        rule.runOnIdle {
            assertThat(onClickCount).isEqualTo(0)
            assertThat(onSwipeForwardCount).isEqualTo(0)
            assertThat(onSwipeBackwardCount).isEqualTo(0)
        }

        // A new, valid gesture should be processed correctly
        rule.sendGlimmerIndirectPointerInput { click() }

        rule.runOnIdle { assertThat(onClickCount).isEqualTo(1) }
    }

    @Test
    fun onGesture_disabledMidGesture_isIgnored() {
        var onClickCount = 0
        var onSwipeForwardCount = 0
        var onSwipeBackwardCount = 0

        val enabled = mutableStateOf(true)

        rule.setContent {
            Box(
                modifier =
                    Modifier.testTag(ROOT_TEST_TAG)
                        .size(10.dp)
                        .onIndirectPointerGesture(
                            enabled = enabled.value,
                            onSwipeForward = { onSwipeForwardCount++ },
                            onSwipeBackward = { onSwipeBackwardCount++ },
                            onClick = { onClickCount++ },
                        )
                        .focusTarget()
            )
        }

        rule.sendGlimmerIndirectPointerInput {
            // Start gesture while enabled
            down(0, Offset.Zero)
        }

        // Disable mid-gesture
        enabled.value = false

        rule.sendGlimmerIndirectPointerInput { up(0) }

        rule.runOnIdle {
            assertThat(onClickCount).isEqualTo(0)
            assertThat(onSwipeForwardCount).isEqualTo(0)
            assertThat(onSwipeBackwardCount).isEqualTo(0)
        }
    }

    @Test
    fun onGesture_whenReEnabled_isDetected() {
        var onClickCount = 0
        var onSwipeForwardCount = 0
        var onSwipeBackwardCount = 0

        val enabled = mutableStateOf(false)

        rule.setContent {
            Box(
                modifier =
                    Modifier.testTag(ROOT_TEST_TAG)
                        .size(10.dp)
                        .onIndirectPointerGesture(
                            enabled = enabled.value,
                            onSwipeForward = { onSwipeForwardCount++ },
                            onSwipeBackward = { onSwipeBackwardCount++ },
                            onClick = { onClickCount++ },
                        )
                        .focusTarget()
            )
        }

        rule.sendGlimmerIndirectPointerInput { click() }

        rule.runOnIdle {
            assertThat(onClickCount).isEqualTo(0)
            assertThat(onSwipeForwardCount).isEqualTo(0)
            assertThat(onSwipeBackwardCount).isEqualTo(0)
        }

        enabled.value = true

        rule.sendGlimmerIndirectPointerInput { click() }

        rule.runOnIdle {
            assertThat(onClickCount).isEqualTo(1)
            assertThat(onSwipeForwardCount).isEqualTo(0)
            assertThat(onSwipeBackwardCount).isEqualTo(0)
        }
    }

    @Test
    fun onClick_descendantConsumesClick_notTriggered() {
        var innerOnClickCount = 0
        var onClickCount = 0

        rule.setContent {
            Box(
                modifier =
                    Modifier.testTag(ROOT_TEST_TAG)
                        .size(10.dp)
                        .onIndirectPointerGesture(enabled = true, onClick = { onClickCount++ })
            ) {
                Box(modifier = Modifier.clickable(onClick = { innerOnClickCount++ }))
            }
        }

        rule.sendGlimmerIndirectPointerInput { click() }

        rule.runOnIdle {
            assertThat(innerOnClickCount).isEqualTo(1)
            assertThat(onClickCount).isEqualTo(0)
        }
    }

    @Test
    fun onSwipeForward_descendantConsumesClick_triggered() {
        var innerOnClickCount = 0
        var onSwipeForwardCount = 0

        rule.setContent {
            Box(
                modifier =
                    Modifier.testTag(ROOT_TEST_TAG)
                        .size(10.dp)
                        .onIndirectPointerGesture(
                            enabled = true,
                            onSwipeForward = { onSwipeForwardCount++ },
                        )
            ) {
                Box(modifier = Modifier.clickable(onClick = { innerOnClickCount++ }))
            }
        }

        rule.sendGlimmerIndirectPointerInput { swipeRight() }

        rule.runOnIdle {
            assertThat(innerOnClickCount).isEqualTo(0)
            assertThat(onSwipeForwardCount).isEqualTo(1)
        }
    }

    @Test
    fun onSwipeBackward_descendantConsumesClick_triggered() {
        var innerOnClickCount = 0
        var onSwipeBackwardCount = 0

        rule.setContent {
            Box(
                modifier =
                    Modifier.testTag(ROOT_TEST_TAG)
                        .size(10.dp)
                        .onIndirectPointerGesture(
                            enabled = true,
                            onSwipeBackward = { onSwipeBackwardCount++ },
                        )
            ) {
                Box(modifier = Modifier.clickable(onClick = { innerOnClickCount++ }))
            }
        }

        rule.sendGlimmerIndirectPointerInput { swipeLeft() }

        rule.runOnIdle {
            assertThat(innerOnClickCount).isEqualTo(0)
            assertThat(onSwipeBackwardCount).isEqualTo(1)
        }
    }

    @Test
    fun onSwipeForward_descendantConsumesDrag_notTriggered() {
        var draggableOnStartCalled = false
        var draggableOnStoppedCalled = false
        var onSwipeForwardCount = 0

        rule.setContent {
            Box(
                modifier =
                    Modifier.testTag(ROOT_TEST_TAG)
                        .size(10.dp)
                        .onIndirectPointerGesture(
                            enabled = true,
                            onSwipeForward = { onSwipeForwardCount++ },
                        )
            ) {
                Box(
                    modifier =
                        Modifier.draggable(
                                state = DraggableState {},
                                orientation = Orientation.Horizontal,
                                onDragStarted = { draggableOnStartCalled = true },
                                onDragStopped = { draggableOnStoppedCalled = true },
                            )
                            .focusable()
                )
            }
        }

        rule.sendGlimmerIndirectPointerInput { swipeRight() }

        rule.runOnIdle {
            assertThat(draggableOnStartCalled).isTrue()
            assertThat(draggableOnStoppedCalled).isTrue()
            assertThat(onSwipeForwardCount).isEqualTo(0)
        }
    }

    @Test
    fun onSwipeBackward_descendantConsumesDrag_notTriggered() {
        var draggableOnStartCalled = false
        var draggableOnStoppedCalled = false
        var onSwipeBackwardCount = 0

        rule.setContent {
            Box(
                modifier =
                    Modifier.testTag(ROOT_TEST_TAG)
                        .size(10.dp)
                        .onIndirectPointerGesture(
                            enabled = true,
                            onSwipeBackward = { onSwipeBackwardCount++ },
                        )
            ) {
                Box(
                    modifier =
                        Modifier.draggable(
                                state = DraggableState {},
                                orientation = Orientation.Horizontal,
                                onDragStarted = { draggableOnStartCalled = true },
                                onDragStopped = { draggableOnStoppedCalled = true },
                            )
                            .focusable()
                )
            }
        }

        rule.sendGlimmerIndirectPointerInput { swipeLeft() }

        rule.runOnIdle {
            assertThat(draggableOnStartCalled).isTrue()
            assertThat(draggableOnStoppedCalled).isTrue()
            assertThat(onSwipeBackwardCount).isEqualTo(0)
        }
    }

    @Test
    fun onClick_descendantConsumesDrag_triggered() {
        var onClickCount = 0

        rule.setContent {
            Box(
                modifier =
                    Modifier.testTag(ROOT_TEST_TAG)
                        .size(10.dp)
                        .onIndirectPointerGesture(enabled = true, onClick = { onClickCount++ })
            ) {
                Box(
                    modifier =
                        Modifier.draggable(
                                state = DraggableState {},
                                orientation = Orientation.Horizontal,
                            )
                            .focusable()
                )
            }
        }
        rule.sendGlimmerIndirectPointerInput { click() }

        rule.runOnIdle { assertThat(onClickCount).isEqualTo(1) }
    }

    @Test
    fun onClick_innerNullCallback_outerTriggered() {
        var outerOnClickCount = 0

        rule.setContent {
            Box(
                modifier =
                    Modifier.testTag(ROOT_TEST_TAG)
                        .size(10.dp)
                        .onIndirectPointerGesture(enabled = true, onClick = { outerOnClickCount++ })
            ) {
                Box(
                    modifier =
                        Modifier.size(10.dp)
                            .onIndirectPointerGesture(enabled = true, onClick = null)
                            .focusable()
                )
            }
        }

        rule.sendGlimmerIndirectPointerInput { click() }

        rule.runOnIdle { assertThat(outerOnClickCount).isEqualTo(1) }
    }

    @Test
    fun onSwipeForward_innerNullCallback_outerTriggered() {
        var outerOnSwipeForwardCount = 0

        rule.setContent {
            Box(
                modifier =
                    Modifier.testTag(ROOT_TEST_TAG)
                        .size(10.dp)
                        .onIndirectPointerGesture(
                            enabled = true,
                            onSwipeForward = { outerOnSwipeForwardCount++ },
                        )
            ) {
                Box(
                    modifier =
                        Modifier.size(10.dp)
                            .onIndirectPointerGesture(enabled = true, onSwipeForward = null)
                            .focusable()
                )
            }
        }

        rule.sendGlimmerIndirectPointerInput { swipeRight() }

        rule.runOnIdle { assertThat(outerOnSwipeForwardCount).isEqualTo(1) }
    }

    @Test
    fun onSwipeBackward_innerNullCallback_outerTriggered() {
        var outerOnSwipeBackwardCount = 0

        rule.setContent {
            Box(
                modifier =
                    Modifier.testTag(ROOT_TEST_TAG)
                        .size(10.dp)
                        .onIndirectPointerGesture(
                            enabled = true,
                            onSwipeBackward = { outerOnSwipeBackwardCount++ },
                        )
            ) {
                Box(
                    modifier =
                        Modifier.size(10.dp)
                            .onIndirectPointerGesture(enabled = true, onSwipeBackward = null)
                            .focusable()
                )
            }
        }

        rule.sendGlimmerIndirectPointerInput { swipeLeft() }

        rule.runOnIdle { assertThat(outerOnSwipeBackwardCount).isEqualTo(1) }
    }

    @Test
    fun outerHandlesSwipeBackward_innerHandlesSwipeForward_bothTriggered() {
        var outerOnSwipeBackwardCount = 0
        var innerOnSwipeForwardCount = 0

        rule.setContent {
            Box(
                modifier =
                    Modifier.testTag(ROOT_TEST_TAG)
                        .size(10.dp)
                        .onIndirectPointerGesture(
                            enabled = true,
                            onSwipeBackward = { outerOnSwipeBackwardCount++ },
                        )
            ) {
                Box(
                    modifier =
                        Modifier.size(10.dp)
                            .onIndirectPointerGesture(
                                enabled = true,
                                onSwipeForward = { innerOnSwipeForwardCount++ },
                            )
                            .focusable()
                )
            }
        }

        rule.sendGlimmerIndirectPointerInput { swipeLeft() }

        rule.runOnIdle { assertThat(outerOnSwipeBackwardCount).isEqualTo(1) }
        rule.runOnIdle { assertThat(innerOnSwipeForwardCount).isEqualTo(0) }

        rule.sendGlimmerIndirectPointerInput { swipeRight() }

        rule.runOnIdle { assertThat(outerOnSwipeBackwardCount).isEqualTo(1) }
        rule.runOnIdle { assertThat(innerOnSwipeForwardCount).isEqualTo(1) }
    }

    @Test
    fun outerHandlesSwipeForward_innerHandlesSwipeBackward_bothTriggered() {
        var outerOnSwipeForwardCount = 0
        var innerOnSwipeBackwardCount = 0

        rule.setContent {
            Box(
                modifier =
                    Modifier.testTag(ROOT_TEST_TAG)
                        .size(10.dp)
                        .onIndirectPointerGesture(
                            enabled = true,
                            onSwipeForward = { outerOnSwipeForwardCount++ },
                        )
            ) {
                Box(
                    modifier =
                        Modifier.size(10.dp)
                            .onIndirectPointerGesture(
                                enabled = true,
                                onSwipeBackward = { innerOnSwipeBackwardCount++ },
                            )
                            .focusable()
                )
            }
        }

        rule.sendGlimmerIndirectPointerInput { swipeLeft() }

        rule.runOnIdle { assertThat(innerOnSwipeBackwardCount).isEqualTo(1) }
        rule.runOnIdle { assertThat(outerOnSwipeForwardCount).isEqualTo(0) }

        rule.sendGlimmerIndirectPointerInput { swipeRight() }

        rule.runOnIdle { assertThat(innerOnSwipeBackwardCount).isEqualTo(1) }
        rule.runOnIdle { assertThat(outerOnSwipeForwardCount).isEqualTo(1) }
    }

    @Test
    fun outerHandlesClick_innerHandlesSwipes_outerClickNotTriggered() {
        var outerOnClickCount = 0
        var innerOnSwipeForwardCount = 0
        var innerOnSwipeBackwardCount = 0

        rule.setContent {
            Box(
                modifier =
                    Modifier.testTag(ROOT_TEST_TAG)
                        .size(10.dp)
                        .onIndirectPointerGesture(enabled = true, onClick = { outerOnClickCount++ })
            ) {
                Box(
                    modifier =
                        Modifier.size(10.dp)
                            .onIndirectPointerGesture(
                                enabled = true,
                                onSwipeBackward = { innerOnSwipeBackwardCount++ },
                                onSwipeForward = { innerOnSwipeForwardCount++ },
                            )
                            .focusable()
                )
            }
        }

        rule.sendGlimmerIndirectPointerInput { click() }

        rule.runOnIdle { assertThat(outerOnClickCount).isEqualTo(0) }
        rule.runOnIdle { assertThat(innerOnSwipeForwardCount).isEqualTo(0) }
        rule.runOnIdle { assertThat(innerOnSwipeBackwardCount).isEqualTo(0) }

        rule.sendGlimmerIndirectPointerInput { swipeLeft() }

        rule.runOnIdle { assertThat(outerOnClickCount).isEqualTo(0) }
        rule.runOnIdle { assertThat(innerOnSwipeForwardCount).isEqualTo(0) }
        rule.runOnIdle { assertThat(innerOnSwipeBackwardCount).isEqualTo(1) }

        rule.sendGlimmerIndirectPointerInput { swipeRight() }

        rule.runOnIdle { assertThat(outerOnClickCount).isEqualTo(0) }
        rule.runOnIdle { assertThat(innerOnSwipeForwardCount).isEqualTo(1) }
        rule.runOnIdle { assertThat(innerOnSwipeBackwardCount).isEqualTo(1) }
    }

    @Test
    fun outerHandlesSwipes_innerHandlesClick_bothTriggered() {
        var innerOnClickCount = 0
        var outerOnSwipeForwardCount = 0
        var outerOnSwipeBackwardCount = 0

        rule.setContent {
            Box(
                modifier =
                    Modifier.testTag(ROOT_TEST_TAG)
                        .size(10.dp)
                        .onIndirectPointerGesture(
                            enabled = true,
                            onSwipeForward = { outerOnSwipeForwardCount++ },
                            onSwipeBackward = { outerOnSwipeBackwardCount++ },
                        )
            ) {
                Box(
                    modifier =
                        Modifier.size(10.dp)
                            .onIndirectPointerGesture(
                                enabled = true,
                                onClick = { innerOnClickCount++ },
                            )
                            .focusable()
                )
            }
        }

        rule.sendGlimmerIndirectPointerInput { click() }

        rule.runOnIdle { assertThat(innerOnClickCount).isEqualTo(1) }
        rule.runOnIdle { assertThat(outerOnSwipeForwardCount).isEqualTo(0) }
        rule.runOnIdle { assertThat(outerOnSwipeBackwardCount).isEqualTo(0) }

        rule.sendGlimmerIndirectPointerInput { swipeLeft() }

        rule.runOnIdle { assertThat(innerOnClickCount).isEqualTo(1) }
        rule.runOnIdle { assertThat(outerOnSwipeForwardCount).isEqualTo(0) }
        rule.runOnIdle { assertThat(outerOnSwipeBackwardCount).isEqualTo(1) }

        rule.sendGlimmerIndirectPointerInput { swipeRight() }

        rule.runOnIdle { assertThat(innerOnClickCount).isEqualTo(1) }
        rule.runOnIdle { assertThat(outerOnSwipeForwardCount).isEqualTo(1) }
        rule.runOnIdle { assertThat(outerOnSwipeBackwardCount).isEqualTo(1) }
    }

    companion object {
        private const val ROOT_TEST_TAG = "boxWithIndirectPointerGesture"
        private const val CHILD_TEST_TAG = "childBoxWithIndirectPointerEventConsumption"
    }
}
