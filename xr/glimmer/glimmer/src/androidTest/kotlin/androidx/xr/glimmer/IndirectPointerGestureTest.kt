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
import android.os.SystemClock
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_POINTER_DOWN
import android.view.MotionEvent.ACTION_POINTER_INDEX_SHIFT
import android.view.MotionEvent.ACTION_POINTER_UP
import android.view.MotionEvent.ACTION_UP
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ExperimentalIndirectPointerApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.input.indirect.IndirectPointerEvent
import androidx.compose.ui.input.indirect.IndirectPointerEventPrimaryDirectionalMotionAxis
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.core.view.InputDeviceCompat.SOURCE_TOUCH_NAVIGATION
import androidx.test.core.view.MotionEventBuilder
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
// The expected min sdk is 35, but we test on 33 for wider device coverage (some APIs are not
// available below 33)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalIndirectPointerApi::class, ExperimentalComposeUiApi::class)
class IndirectPointerGestureTest {

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @get:Rule val inputModeRule = nonTouchInputModeRule()

    val savedInitialFocusAvailabilityFlag = ComposeUiFlags.isInitialFocusOnFocusableAvailable

    @Before
    fun setup() {
        ComposeUiFlags.isInitialFocusOnFocusableAvailable = true
    }

    @After
    fun tearDown() {
        ComposeUiFlags.isInitialFocusOnFocusableAvailable = savedInitialFocusAvailabilityFlag
    }

    @Test
    fun gestures_areIgnored_whenDisabled() {
        var touchSlop = 0f
        var gestureCount = 0

        rule.setContent {
            touchSlop = LocalViewConfiguration.current.touchSlop
            Box(
                modifier =
                    Modifier.testTag(ROOT_TEST_TAG)
                        .size(10.dp)
                        .onIndirectPointerGesture(
                            enabled = false,
                            onClick = { gestureCount++ },
                            onSwipeForward = { gestureCount++ },
                            onSwipeBackward = { gestureCount++ },
                        )
                        .focusTarget()
            )
        }

        // Perform all known gestures
        rule.onNodeWithTag(ROOT_TEST_TAG).performIndirectClick(rule)
        rule
            .onNodeWithTag(ROOT_TEST_TAG)
            .performIndirectSwipe(rule = rule, distance = touchSlop * 2, moveDuration = 10L)
        rule
            .onNodeWithTag(ROOT_TEST_TAG)
            .performIndirectSwipe(rule = rule, distance = -touchSlop * 2, moveDuration = 10L)

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

        rule.onNodeWithTag(ROOT_TEST_TAG).performIndirectClick(rule)

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
                            onClick = { onClickCount++ },
                            onSwipeForward = { onSwipeForwardCount++ },
                            onSwipeBackward = { onSwipeBackwardCount++ },
                        )
                        .focusTarget()
            )
        }

        // Perform a click that lasts longer than the long press timeout
        rule.onNodeWithTag(ROOT_TEST_TAG).performIndirectClick(rule, longPressTimeoutMillis + 50L)

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

        var touchSlop = 0f

        rule.setContent {
            touchSlop = LocalViewConfiguration.current.touchSlop
            Box(
                modifier =
                    Modifier.testTag(ROOT_TEST_TAG)
                        .size(10.dp)
                        .onIndirectPointerGesture(
                            enabled = true,
                            onClick = { onClickCount++ },
                            onSwipeForward = { onSwipeForwardCount++ },
                            onSwipeBackward = { onSwipeBackwardCount++ },
                        )
                        .focusTarget()
            )
        }

        rule
            .onNodeWithTag(ROOT_TEST_TAG)
            .performIndirectSwipe(rule = rule, distance = touchSlop * 2, moveDuration = 10L)

        rule.runOnIdle {
            assertThat(onClickCount).isEqualTo(0)
            assertThat(onSwipeForwardCount).isEqualTo(1)
            assertThat(onSwipeBackwardCount).isEqualTo(0)
        }
    }

    @Test
    fun swipeBackward_triggersOnSwipeBackward() {
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
                            onClick = { onClickCount++ },
                            onSwipeForward = { onSwipeForwardCount++ },
                            onSwipeBackward = { onSwipeBackwardCount++ },
                        )
                        .focusTarget()
            )
        }

        rule
            .onNodeWithTag(ROOT_TEST_TAG)
            .performIndirectSwipe(rule = rule, moveDuration = 10L, distance = -touchSlop * 2)

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

        var touchSlop = 0f

        rule.setContent {
            touchSlop = LocalViewConfiguration.current.touchSlop
            Box(
                modifier =
                    Modifier.testTag(ROOT_TEST_TAG)
                        .size(10.dp)
                        .onIndirectPointerGesture(
                            enabled = true,
                            onClick = { onClickCount++ },
                            onSwipeForward = { onSwipeForwardCount++ },
                            onSwipeBackward = { onSwipeBackwardCount++ },
                        )
                        .focusTarget()
            )
        }

        val downTime = SystemClock.uptimeMillis()
        var eventTime = downTime

        val p0 = getPointerProperties(0)
        val p1 = getPointerProperties(1)
        val p0Coords = getPointerCoords(x = 0f, y = 0f)
        val p1Coords = getPointerCoords(x = 0f, y = 0f)

        val p0Down =
            buildMotionEvent(
                action = ACTION_DOWN,
                pointers = listOf(p0),
                pointerCoords = listOf(p0Coords),
                downTime = downTime,
                eventTime = eventTime,
            )
        dispatchIndirectPointerEvent(p0Down)

        eventTime += 10
        val p1Down =
            buildMotionEvent(
                action = ACTION_POINTER_DOWN or (1 shl ACTION_POINTER_INDEX_SHIFT),
                pointers = listOf(p0, p1),
                pointerCoords = listOf(p0Coords, p1Coords),
                downTime = downTime,
                eventTime = eventTime,
            )
        dispatchIndirectPointerEvent(p1Down, p0Down)

        eventTime += 10
        p0Coords.x = -(touchSlop * 4)
        val p0Move =
            buildMotionEvent(
                action = ACTION_MOVE,
                pointers = listOf(p0, p1),
                pointerCoords = listOf(p0Coords, p1Coords),
                downTime = downTime,
                eventTime = eventTime,
            )
        dispatchIndirectPointerEvent(p0Move, p1Down)

        eventTime += 10
        val p1Up =
            buildMotionEvent(
                action = ACTION_POINTER_UP or (1 shl ACTION_POINTER_INDEX_SHIFT),
                pointers = listOf(p0, p1),
                pointerCoords = listOf(p0Coords, p1Coords),
                downTime = downTime,
                eventTime = eventTime,
            )
        dispatchIndirectPointerEvent(p1Up, p0Move)

        eventTime += 10
        val p0Up =
            buildMotionEvent(
                action = ACTION_UP,
                pointers = listOf(p0),
                pointerCoords = listOf(p0Coords),
                downTime = downTime,
                eventTime = eventTime,
            )
        dispatchIndirectPointerEvent(p0Up, p0Move)

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

        var touchSlop = 0f

        rule.setContent {
            touchSlop = LocalViewConfiguration.current.touchSlop
            Box(
                modifier =
                    Modifier.testTag(ROOT_TEST_TAG)
                        .size(10.dp)
                        .onIndirectPointerGesture(
                            enabled = true,
                            onClick = { onClickCount++ },
                            onSwipeForward = { onSwipeForwardCount++ },
                            onSwipeBackward = { onSwipeBackwardCount++ },
                        )
                        .focusTarget()
            )
        }

        val downTime = SystemClock.uptimeMillis()
        var eventTime = downTime

        val p0 = getPointerProperties(0)
        val p1 = getPointerProperties(1)
        val p0Coords = getPointerCoords(x = 0f, y = 0f)
        val p1Coords = getPointerCoords(x = 0f, y = 0f)

        val p0Down =
            buildMotionEvent(
                action = ACTION_DOWN,
                pointers = listOf(p0),
                pointerCoords = listOf(p0Coords),
                downTime = downTime,
                eventTime = eventTime,
            )
        dispatchIndirectPointerEvent(p0Down)

        eventTime += 10
        val p1Down =
            buildMotionEvent(
                action = ACTION_POINTER_DOWN or (1 shl ACTION_POINTER_INDEX_SHIFT),
                pointers = listOf(p0, p1),
                pointerCoords = listOf(p0Coords, p1Coords),
                downTime = downTime,
                eventTime = eventTime,
            )
        dispatchIndirectPointerEvent(p1Down, p0Down)

        eventTime += 10
        p0Coords.x = -touchSlop * 4f // p0 (primary) moves backward
        p1Coords.x = touchSlop * 4f // p1 moves forward
        val pointersMove =
            buildMotionEvent(
                action = ACTION_MOVE,
                pointers = listOf(p0, p1),
                pointerCoords = listOf(p0Coords, p1Coords),
                downTime = downTime,
                eventTime = eventTime,
            )
        dispatchIndirectPointerEvent(pointersMove, p1Down)

        eventTime += 10
        val p1Up =
            buildMotionEvent(
                action = ACTION_POINTER_UP or (1 shl ACTION_POINTER_INDEX_SHIFT),
                pointers = listOf(p0, p1),
                pointerCoords = listOf(p0Coords, p1Coords),
                downTime = downTime,
                eventTime = eventTime,
            )
        dispatchIndirectPointerEvent(p1Up, pointersMove)

        eventTime += 10
        val p0Up =
            buildMotionEvent(
                action = ACTION_UP,
                pointers = listOf(p0),
                pointerCoords = listOf(p0Coords),
                downTime = downTime,
                eventTime = eventTime,
            )
        dispatchIndirectPointerEvent(p0Up, pointersMove)

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
                            onClick = { onClickCount++ },
                            onSwipeForward = { onSwipeForwardCount++ },
                            onSwipeBackward = { onSwipeBackwardCount++ },
                        )
                        .focusTarget()
            )
        }

        val downTime = System.currentTimeMillis()
        var eventTime = downTime

        val p0 = getPointerProperties(0)
        val p0Coords = getPointerCoords(x = 0f, y = 0f)

        val p0Down =
            buildMotionEvent(
                action = ACTION_DOWN,
                pointers = listOf(p0),
                pointerCoords = listOf(p0Coords),
                downTime = downTime,
                eventTime = eventTime,
            )

        dispatchIndirectPointerEvent(p0Down)

        eventTime += 10
        p0Coords.x = touchSlop + 1f

        val p0MoveInitial =
            buildMotionEvent(
                action = ACTION_MOVE,
                pointers = listOf(p0),
                pointerCoords = listOf(p0Coords),
                downTime = downTime,
                eventTime = eventTime,
            )
        dispatchIndirectPointerEvent(p0MoveInitial, p0Down)

        eventTime += 10
        p0Coords.x = 0f

        val p0MoveSecond =
            buildMotionEvent(
                action = ACTION_MOVE,
                pointers = listOf(p0),
                pointerCoords = listOf(p0Coords),
                downTime = downTime,
                eventTime = eventTime,
            )
        dispatchIndirectPointerEvent(p0MoveSecond, p0MoveInitial)

        eventTime += 10
        val p0Up =
            buildMotionEvent(
                action = ACTION_MOVE,
                pointers = listOf(p0),
                pointerCoords = listOf(p0Coords),
                downTime = downTime,
                eventTime = eventTime,
            )
        dispatchIndirectPointerEvent(p0Up, p0MoveSecond)

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

        var touchSlop = 0f

        rule.setContent {
            touchSlop = LocalViewConfiguration.current.touchSlop
            Box(
                modifier =
                    Modifier.testTag(ROOT_TEST_TAG)
                        .size(10.dp)
                        .onIndirectPointerGesture(
                            enabled = true,
                            onClick = { onClickCount++ },
                            onSwipeForward = { onSwipeForwardCount++ },
                            onSwipeBackward = { onSwipeBackwardCount++ },
                        )
                        .focusTarget()
            )
        }

        val downTime = System.currentTimeMillis()
        var eventTime = downTime

        val p0 = getPointerProperties(0)
        val p1 = getPointerProperties(1)

        val p0Coords = getPointerCoords(x = 0f, y = 0f)
        val p1Coords = getPointerCoords(0f, y = 0f)

        val p0Down =
            buildMotionEvent(
                action = ACTION_DOWN,
                pointers = listOf(p0),
                pointerCoords = listOf(p0Coords),
                downTime = downTime,
                eventTime = eventTime,
            )

        dispatchIndirectPointerEvent(p0Down)

        eventTime += 10

        val p1Down =
            buildMotionEvent(
                action = ACTION_POINTER_DOWN or (1 shl ACTION_POINTER_INDEX_SHIFT),
                pointers = listOf(p0, p1),
                pointerCoords = listOf(p0Coords, p1Coords),
                downTime = downTime,
                eventTime = eventTime,
            )

        dispatchIndirectPointerEvent(p1Down)

        eventTime += 10

        p1Coords.x = touchSlop * 4
        // Only move p1
        val p1Move =
            buildMotionEvent(
                action = ACTION_MOVE,
                pointers = listOf(p0, p1),
                pointerCoords = listOf(p0Coords, p1Coords),
                downTime = downTime,
                eventTime = eventTime,
            )

        dispatchIndirectPointerEvent(p1Move, p1Down)

        eventTime += 10

        val p1Up =
            buildMotionEvent(
                action = ACTION_POINTER_UP or (1 shl ACTION_POINTER_INDEX_SHIFT),
                pointers = listOf(p0, p1),
                pointerCoords = listOf(p0Coords, p1Coords),
                downTime = downTime,
                eventTime = eventTime,
            )

        dispatchIndirectPointerEvent(p1Up, p1Move)

        val p0Up =
            buildMotionEvent(
                action = ACTION_UP,
                pointers = listOf(p0),
                pointerCoords = listOf(p0Coords),
                downTime = downTime,
                eventTime = eventTime,
            )

        dispatchIndirectPointerEvent(p0Up, p1Move)

        // The gesture is processed based on p0, which was a click.
        rule.runOnIdle {
            assertThat(onClickCount).isEqualTo(1)
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
                            onClick = { onClickCount++ },
                            onSwipeForward = { onSwipeForwardCount++ },
                            onSwipeBackward = { onSwipeBackwardCount++ },
                        )
                        .focusTarget()
            )
        }

        // Perform a swipe that is fast, but the distance is just *under* the threshold
        val swipeDistanceThreshold = touchSlop * 1.3f
        val swipeDistance = swipeDistanceThreshold * 0.98f

        rule
            .onNodeWithTag(ROOT_TEST_TAG)
            .performIndirectSwipe(rule = rule, distance = swipeDistance, moveDuration = 10L)

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
                            onClick = { onClickCount++ },
                            onSwipeForward = { onSwipeForwardCount++ },
                            onSwipeBackward = { onSwipeBackwardCount++ },
                        )
                        .focusTarget()
            )
        }

        val downTime = System.currentTimeMillis()
        var eventTime = downTime + 10L

        val p0Coords = getPointerCoords(x = 0f, y = 0f)
        val properties = listOf(getPointerProperties(0))
        val coords = listOf(p0Coords)

        val p0Down =
            buildMotionEvent(
                action = ACTION_DOWN,
                pointers = properties,
                pointerCoords = coords,
                downTime = downTime,
                eventTime = eventTime,
            )

        dispatchIndirectPointerEvent(p0Down)

        eventTime += 10L

        val repeatCount = 30
        val eachDragMovement = (touchSlop * 1.4f) / repeatCount
        var lastEvent = p0Down
        // The resulting X velocity is 76, which is below the 100f threshold.
        repeat(repeatCount) {
            eventTime += 10
            p0Coords.x += eachDragMovement
            val p0MoveSlow =
                buildMotionEvent(
                    action = ACTION_MOVE,
                    pointers = properties,
                    pointerCoords = coords,
                    downTime = downTime,
                    eventTime = eventTime,
                )
            dispatchIndirectPointerEvent(p0MoveSlow, lastEvent)
            lastEvent = p0MoveSlow
        }

        eventTime += 10
        val p0Up =
            buildMotionEvent(
                action = ACTION_UP,
                pointers = properties,
                pointerCoords = coords,
                downTime = downTime,
                eventTime = eventTime,
            )

        dispatchIndirectPointerEvent(p0Up, lastEvent)

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
                            onClick = { onClickCount++ },
                            onSwipeForward = { onSwipeForwardCount++ },
                            onSwipeBackward = { onSwipeBackwardCount++ },
                        )
                        .focusTarget()
            )
        }

        val downTime = SystemClock.uptimeMillis()
        var eventTime = downTime
        val p0 = getPointerProperties(0)
        val p0Coords = getPointerCoords(x = 0f, y = 0f)

        val p0Down =
            buildMotionEvent(
                action = ACTION_DOWN,
                pointers = listOf(p0),
                pointerCoords = listOf(p0Coords),
                downTime = downTime,
                eventTime = eventTime,
            )
        dispatchIndirectPointerEvent(p0Down)

        eventTime += 10
        p0Coords.x = touchSlop * 4 // Move forward
        val p0MoveForward =
            buildMotionEvent(
                action = ACTION_MOVE,
                pointers = listOf(p0),
                pointerCoords = listOf(p0Coords),
                downTime = downTime,
                eventTime = eventTime,
            )
        dispatchIndirectPointerEvent(p0MoveForward, p0Down)

        eventTime += 10
        p0Coords.x = touchSlop * 2 // Move backward
        val p0MoveBackward =
            buildMotionEvent(
                action = ACTION_MOVE,
                pointers = listOf(p0),
                pointerCoords = listOf(p0Coords),
                downTime = downTime,
                eventTime = eventTime,
            )
        dispatchIndirectPointerEvent(p0MoveBackward, p0MoveForward)

        eventTime += 20L
        val p0Up =
            buildMotionEvent(
                action = ACTION_UP,
                pointers = listOf(p0),
                pointerCoords = listOf(p0Coords),
                downTime = downTime,
                eventTime = eventTime,
            )
        dispatchIndirectPointerEvent(p0Up, p0MoveBackward)

        rule.runOnIdle {
            assertThat(onClickCount).isEqualTo(0)
            assertThat(onSwipeForwardCount).isEqualTo(0)
            assertThat(onSwipeBackwardCount).isEqualTo(0)
        }
    }

    @Test
    fun consumedGesture_isIgnored_andDoesNotBlockSubsequentGestures() {
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
                            onClick = { onClickCount++ },
                            onSwipeForward = { onSwipeForwardCount++ },
                            onSwipeBackward = { onSwipeBackwardCount++ },
                        )
                        .focusTarget()
            )
        }

        val downTime = SystemClock.uptimeMillis()
        var eventTime = downTime
        val p0 = getPointerProperties(0)
        val p0Coords = getPointerCoords(x = 0f, y = 0f)

        val p0Down =
            buildMotionEvent(
                action = ACTION_DOWN,
                pointers = listOf(p0),
                pointerCoords = listOf(p0Coords),
                downTime = downTime,
                eventTime = eventTime,
            )
        dispatchIndirectPointerEvent(p0Down)

        eventTime += 10
        p0Coords.x = touchSlop * 4
        val p0Move =
            buildMotionEvent(
                action = ACTION_MOVE,
                pointers = listOf(p0),
                pointerCoords = listOf(p0Coords),
                downTime = downTime,
                eventTime = eventTime,
            )
        dispatchIndirectPointerEvent(p0Move, p0Down, consumeEvents = true)

        eventTime += 20L
        val p0Up =
            buildMotionEvent(
                action = ACTION_UP,
                pointers = listOf(p0),
                pointerCoords = listOf(p0Coords),
                downTime = downTime,
                eventTime = eventTime,
            )
        dispatchIndirectPointerEvent(p0Up, p0Move)

        // The consumed 'move' in gesture should reset so no callback is called.
        rule.runOnIdle {
            assertThat(onClickCount).isEqualTo(0)
            assertThat(onSwipeForwardCount).isEqualTo(0)
            assertThat(onSwipeBackwardCount).isEqualTo(0)
        }

        // A new, valid gesture should be processed correctly
        rule.onNodeWithTag(ROOT_TEST_TAG).performIndirectClick(rule)

        rule.runOnIdle {
            assertThat(onClickCount).isEqualTo(1)
            assertThat(onSwipeForwardCount).isEqualTo(0)
            assertThat(onSwipeBackwardCount).isEqualTo(0)
        }
    }

    @Test
    fun gestureEventsIgnored_whenDisabledDuringGesture() {
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
                            onClick = { onClickCount++ },
                            onSwipeForward = { onSwipeForwardCount++ },
                            onSwipeBackward = { onSwipeBackwardCount++ },
                        )
                        .focusTarget()
            )
        }

        val downTime = SystemClock.uptimeMillis()
        var eventTime = downTime
        val p0 = getPointerProperties(0)
        val p0Coords = getPointerCoords(x = 0f, y = 0f)

        // Start gesture while enabled
        val p0Down =
            buildMotionEvent(
                action = ACTION_DOWN,
                pointers = listOf(p0),
                pointerCoords = listOf(p0Coords),
                downTime = downTime,
                eventTime = eventTime,
            )
        dispatchIndirectPointerEvent(p0Down)

        // Disable mid-gesture
        enabled.value = false

        // Finish gesture
        eventTime += 10L
        val p0Up =
            buildMotionEvent(
                action = ACTION_UP,
                pointers = listOf(p0),
                pointerCoords = listOf(p0Coords),
                downTime = downTime,
                eventTime = eventTime,
            )
        dispatchIndirectPointerEvent(p0Up, p0Down)

        rule.runOnIdle {
            assertThat(onClickCount).isEqualTo(0)
            assertThat(onSwipeForwardCount).isEqualTo(0)
            assertThat(onSwipeBackwardCount).isEqualTo(0)
        }
    }

    @Test
    fun gestureEventsRegistered_whenModifierIsReEnabled() {
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
                            onClick = { onClickCount++ },
                            onSwipeForward = { onSwipeForwardCount++ },
                            onSwipeBackward = { onSwipeBackwardCount++ },
                        )
                        .focusTarget()
            )
        }

        rule.onNodeWithTag(ROOT_TEST_TAG).performIndirectClick(rule)

        rule.runOnIdle {
            assertThat(onClickCount).isEqualTo(0)
            assertThat(onSwipeForwardCount).isEqualTo(0)
            assertThat(onSwipeBackwardCount).isEqualTo(0)
        }

        enabled.value = true

        rule.onNodeWithTag(ROOT_TEST_TAG).performIndirectClick(rule)

        rule.runOnIdle {
            assertThat(onClickCount).isEqualTo(1)
            assertThat(onSwipeForwardCount).isEqualTo(0)
            assertThat(onSwipeBackwardCount).isEqualTo(0)
        }
    }

    private fun buildMotionEvent(
        action: Int,
        pointers: List<MotionEvent.PointerProperties>,
        pointerCoords: List<MotionEvent.PointerCoords>,
        downTime: Long,
        eventTime: Long,
    ): MotionEvent {
        val builder =
            MotionEventBuilder.newBuilder()
                .setAction(action)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setDownTime(downTime)
                .setEventTime(eventTime)

        pointers.zip(pointerCoords).forEach { (props, coords) -> builder.setPointer(props, coords) }

        return builder.build()
    }

    private fun dispatchIndirectPointerEvent(
        motionEvent: MotionEvent,
        previousMotionEvent: MotionEvent? = null,
        consumeEvents: Boolean = false,
    ) {
        rule
            .onNodeWithTag(ROOT_TEST_TAG)
            .performIndirectPointerEvent(
                rule,
                IndirectPointerEvent(
                        motionEvent = motionEvent,
                        primaryDirectionalMotionAxis =
                            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                        previousMotionEvent = previousMotionEvent,
                    )
                    .apply { if (consumeEvents) changes.forEach { it.consume() } },
            )
    }

    private fun getPointerProperties(id: Int) =
        MotionEvent.PointerProperties().apply {
            this.id = id
            this.toolType = MotionEvent.TOOL_TYPE_FINGER
        }

    private fun getPointerCoords(x: Float, y: Float) =
        MotionEvent.PointerCoords().apply {
            this.x = x
            this.y = y
        }

    companion object {
        private const val ROOT_TEST_TAG = "boxWithIndirectPointerGesture"
    }
}
