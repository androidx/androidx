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

package androidx.compose.ui.input.indirect

import android.content.Context
import android.view.InputDevice.SOURCE_TOUCH_NAVIGATION
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.platform.IndirectPointerNavigationGestureDetector
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class IndirectPointerNavigationGestureDetectorTest {
    private lateinit var context: Context
    private lateinit var indirectPointerNavigationGestureDetector:
        IndirectPointerNavigationGestureDetector

    private val timeBetweenEvents = 20L

    // Triggers GestureDetector's onFling().
    // Note: Simple calculated distance of speed (viewConfiguration.scaledMinimumFlingVelocity) *
    // time will NOT trigger a fling, as it needs to take into account other factors
    // (VelocityTracker and GestureDetector internal fling detection algorithms, touch slop,
    // initial movement, and even possible synthetic input limits). This does that.
    private val flingTriggeringDistanceBetweenEvents = 50
    private val nonFlingTriggeringDistanceBetweenEvents = 10
    private var currentFocusDirection: FocusDirection? = null

    @get:Rule val rule = ActivityScenarioRule(ComponentActivity::class.java)

    @Before
    fun setup() {
        currentFocusDirection = null
        rule.scenario.onActivity { activity ->
            context = activity
            indirectPointerNavigationGestureDetector =
                IndirectPointerNavigationGestureDetector(
                    context,
                    { focusDirection: FocusDirection -> currentFocusDirection = focusDirection },
                )
            // All tests in file require the primary axis to be X:
            indirectPointerNavigationGestureDetector.primaryDirectionalMotionAxis =
                IndirectPointerEventPrimaryDirectionalMotionAxis.X
        }
    }

    @Test
    fun indirectPointerNavigationGesture_swipeForwardHorizontally_triggersNext() {
        val downTime = System.currentTimeMillis()
        val startX = 100f
        val startY = 100f

        // Simulate a down event
        val downEvent =
            createDownIndirectPointerEvent(downTime = downTime, position = Offset(startX, startY))
        val downEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                downEvent,
                isConsumed = false,
            )
        assertTrue(downEventResult)
        assertEquals(null, currentFocusDirection)

        // 2. ACTION_MOVE events (simulating rapid movement)
        val moveTime1 = downTime + timeBetweenEvents
        val move1X = startX + flingTriggeringDistanceBetweenEvents
        val moveChange1 =
            IndirectPointerInputChange(
                id = PointerId(0L),
                uptimeMillis = moveTime1,
                position = Offset(move1X, startY),
                pressed = true,
                pressure = 1.0f,
                previousUptimeMillis = downTime,
                previousPosition = Offset(startX, startY),
                previousPressed = true,
            )
        val moveEvent1 =
            IndirectPointerEvent(
                changes = listOf(moveChange1),
                type = IndirectPointerEventType.Move,
                primaryDirectionalMotionAxis = IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                motionEvent =
                    obtainIndirectMotionEvent(
                        downTime,
                        moveTime1,
                        MotionEvent.ACTION_MOVE,
                        Offset(move1X, startY),
                    ),
            )
        val moveEventResult1 =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                moveEvent1,
                isConsumed = false,
            )
        assertTrue(moveEventResult1)
        assertEquals(null, currentFocusDirection)

        val moveTime2 = moveTime1 + timeBetweenEvents
        val move2X = move1X + flingTriggeringDistanceBetweenEvents
        val moveChange2 =
            IndirectPointerInputChange(
                id = PointerId(0L),
                uptimeMillis = moveTime2,
                position = Offset(move2X, startY),
                pressed = true,
                pressure = 1.0f,
                previousUptimeMillis = moveTime1,
                previousPosition = Offset(move1X, startY),
                previousPressed = true,
            )
        val moveEvent2 =
            IndirectPointerEvent(
                changes = listOf(moveChange2),
                type = IndirectPointerEventType.Move,
                primaryDirectionalMotionAxis = IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                motionEvent =
                    obtainIndirectMotionEvent(
                        downTime,
                        moveTime2,
                        MotionEvent.ACTION_MOVE,
                        Offset(move2X, startY),
                    ),
            )
        val moveEventResult2 =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                moveEvent2,
                isConsumed = false,
            )
        assertTrue(moveEventResult2)
        assertEquals(null, currentFocusDirection)

        // Simulate an up event
        val upTime = moveTime2 + timeBetweenEvents
        val upX = move2X + flingTriggeringDistanceBetweenEvents
        val upEvent =
            createUpIndirectPointerEvent(
                downTime = downTime,
                uptimeMillis = upTime,
                position = Offset(upX, startY),
                previousUptimeMillis = moveTime2,
                previousPosition = Offset(move2X, startY),
            )
        val upEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                upEvent,
                isConsumed = false,
            )
        assertTrue(upEventResult)
        assertEquals(FocusDirection.Companion.Next, currentFocusDirection!!)
    }

    @Test
    fun indirectPointerNavigationGesture_swipeForwardHorizontallyWithExtraDown_doesNotTrigger() {
        val downTime = System.currentTimeMillis()
        val startX = 100f
        val startY = 100f

        // Simulate a down event
        val downEvent =
            createDownIndirectPointerEvent(downTime = downTime, position = Offset(startX, startY))
        val downEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                downEvent,
                isConsumed = false,
            )
        assertTrue(downEventResult)
        assertEquals(null, currentFocusDirection)

        // ACTION_MOVE events
        val moveTime1 = downTime + timeBetweenEvents
        val move1X = startX + flingTriggeringDistanceBetweenEvents
        val moveChange1 =
            IndirectPointerInputChange(
                id = PointerId(0L),
                uptimeMillis = moveTime1,
                position = Offset(move1X, startY),
                pressed = true,
                pressure = 1.0f,
                previousUptimeMillis = downTime,
                previousPosition = Offset(startX, startY),
                previousPressed = true,
            )
        val moveEvent1 =
            IndirectPointerEvent(
                changes = listOf(moveChange1),
                type = IndirectPointerEventType.Move,
                primaryDirectionalMotionAxis = IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                motionEvent =
                    obtainIndirectMotionEvent(
                        downTime,
                        moveTime1,
                        MotionEvent.ACTION_MOVE,
                        Offset(move1X, startY),
                    ),
            )
        val moveEvent1Result =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                moveEvent1,
                isConsumed = false,
            )
        assertTrue(moveEvent1Result)
        assertEquals(null, currentFocusDirection)

        val moveTime2 = moveTime1 + timeBetweenEvents
        val move2X = move1X + flingTriggeringDistanceBetweenEvents
        val moveChange2 =
            IndirectPointerInputChange(
                id = PointerId(0L),
                uptimeMillis = moveTime2,
                position = Offset(move2X, startY),
                pressed = true,
                pressure = 1.0f,
                previousUptimeMillis = moveTime1,
                previousPosition = Offset(move1X, startY),
                previousPressed = true,
            )
        val moveEvent2 =
            IndirectPointerEvent(
                changes = listOf(moveChange2),
                type = IndirectPointerEventType.Move,
                primaryDirectionalMotionAxis = IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                motionEvent =
                    obtainIndirectMotionEvent(
                        downTime,
                        moveTime2,
                        MotionEvent.ACTION_MOVE,
                        Offset(move2X, startY),
                    ),
            )
        val moveEvent2Result =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                moveEvent2,
                isConsumed = false,
            )
        assertTrue(moveEvent2Result)
        assertEquals(null, currentFocusDirection)

        // Injects an extra down that should restart the event stream, so the on fling should NOT be
        // triggered.
        val down2Time = moveTime2 + timeBetweenEvents
        val down2X = move2X
        val down2Change =
            IndirectPointerInputChange(
                id = PointerId(0L),
                uptimeMillis = down2Time,
                position = Offset(down2X, startY),
                pressed = true,
                pressure = 1.0f,
                previousUptimeMillis = moveTime2,
                previousPosition = Offset(move2X, startY),
                previousPressed = true,
            )
        val downEvent2 =
            IndirectPointerEvent(
                changes = listOf(down2Change),
                type = IndirectPointerEventType.Press,
                primaryDirectionalMotionAxis = IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                motionEvent =
                    obtainIndirectMotionEvent(
                        downTime,
                        down2Time,
                        MotionEvent.ACTION_DOWN,
                        Offset(down2X, startY),
                    ),
            )
        val downEvent2Result =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                downEvent2,
                isConsumed = false,
            )
        assertTrue(downEvent2Result)
        assertEquals(null, currentFocusDirection)

        // Simulate an up event
        val upTime = down2Time + timeBetweenEvents
        val upX = down2X + flingTriggeringDistanceBetweenEvents
        val upEvent =
            createUpIndirectPointerEvent(
                downTime = downTime,
                uptimeMillis = upTime,
                position = Offset(upX, startY),
                previousUptimeMillis = down2Time,
                previousPosition = Offset(down2X, startY),
            )
        val upEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                upEvent,
                isConsumed = false,
            )
        assertTrue(upEventResult)
        assertEquals(null, currentFocusDirection)
    }

    @Test
    fun indirectPointerNavigationGesture_swipeBackwardHorizontally_triggersPrevious() {
        val downTime = System.currentTimeMillis()
        val startX = 100f + (3 * flingTriggeringDistanceBetweenEvents)
        val startY = 100f

        // Simulate a down event
        val downEvent =
            createDownIndirectPointerEvent(downTime = downTime, position = Offset(startX, startY))
        val downEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                downEvent,
                isConsumed = false,
            )
        assertTrue(downEventResult)
        assertEquals(null, currentFocusDirection)

        // 2. ACTION_MOVE events (simulating rapid movement)
        val moveTime1 = downTime + timeBetweenEvents
        val move1X = startX - flingTriggeringDistanceBetweenEvents
        val moveChange1 =
            IndirectPointerInputChange(
                id = PointerId(0L),
                uptimeMillis = moveTime1,
                position = Offset(move1X, startY),
                pressed = true,
                pressure = 1.0f,
                previousUptimeMillis = downTime,
                previousPosition = Offset(startX, startY),
                previousPressed = true,
            )
        val moveEvent1 =
            IndirectPointerEvent(
                changes = listOf(moveChange1),
                type = IndirectPointerEventType.Move,
                primaryDirectionalMotionAxis = IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                motionEvent =
                    obtainIndirectMotionEvent(
                        downTime,
                        moveTime1,
                        MotionEvent.ACTION_MOVE,
                        Offset(move1X, startY),
                    ),
            )
        val moveEventResult1 =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                moveEvent1,
                isConsumed = false,
            )
        assertTrue(moveEventResult1)
        assertEquals(null, currentFocusDirection)

        val moveTime2 = moveTime1 + timeBetweenEvents
        val move2X = move1X - flingTriggeringDistanceBetweenEvents
        val moveChange2 =
            IndirectPointerInputChange(
                id = PointerId(0L),
                uptimeMillis = moveTime2,
                position = Offset(move2X, startY),
                pressed = true,
                pressure = 1.0f,
                previousUptimeMillis = moveTime1,
                previousPosition = Offset(move1X, startY),
                previousPressed = true,
            )
        val moveEvent2 =
            IndirectPointerEvent(
                changes = listOf(moveChange2),
                type = IndirectPointerEventType.Move,
                primaryDirectionalMotionAxis = IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                motionEvent =
                    obtainIndirectMotionEvent(
                        downTime,
                        moveTime2,
                        MotionEvent.ACTION_MOVE,
                        Offset(move2X, startY),
                    ),
            )
        val moveEventResult2 =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                moveEvent2,
                isConsumed = false,
            )
        assertTrue(moveEventResult2)
        assertEquals(null, currentFocusDirection)

        // Simulate an up event
        val upTime = moveTime2 + timeBetweenEvents
        val upX = move2X - flingTriggeringDistanceBetweenEvents
        val upEvent =
            createUpIndirectPointerEvent(
                downTime = downTime,
                uptimeMillis = upTime,
                position = Offset(upX, startY),
                previousUptimeMillis = moveTime2,
                previousPosition = Offset(move2X, startY),
            )
        val upEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                upEvent,
                isConsumed = false,
            )
        assertTrue(upEventResult)
        assertEquals(FocusDirection.Companion.Previous, currentFocusDirection!!)
    }

    @Test
    fun indirectPointerNavigationGesture_swipeForwardVertically_doesNotTrigger() {
        val downTime = System.currentTimeMillis()
        val startX = 100f
        val startY = 100f

        // Simulate a down event
        val downEvent =
            createDownIndirectPointerEvent(downTime = downTime, position = Offset(startX, startY))
        val downEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                downEvent,
                isConsumed = false,
            )
        assertTrue(downEventResult)
        assertEquals(null, currentFocusDirection)

        // ACTION_MOVE events
        val moveTime1 = downTime + timeBetweenEvents
        val move1Y = startY + flingTriggeringDistanceBetweenEvents
        val moveChange1 =
            IndirectPointerInputChange(
                id = PointerId(0L),
                uptimeMillis = moveTime1,
                position = Offset(startX, move1Y),
                pressed = true,
                pressure = 1.0f,
                previousUptimeMillis = downTime,
                previousPosition = Offset(startX, startY),
                previousPressed = true,
            )
        val moveEvent1 =
            IndirectPointerEvent(
                changes = listOf(moveChange1),
                type = IndirectPointerEventType.Move,
                primaryDirectionalMotionAxis = IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                motionEvent =
                    obtainIndirectMotionEvent(
                        downTime,
                        moveTime1,
                        MotionEvent.ACTION_MOVE,
                        Offset(startX, move1Y),
                    ),
            )
        val moveEvent1Result =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                moveEvent1,
                isConsumed = false,
            )
        assertTrue(moveEvent1Result)
        assertEquals(null, currentFocusDirection)

        val moveTime2 = moveTime1 + timeBetweenEvents
        val move2Y = move1Y + flingTriggeringDistanceBetweenEvents
        val moveChange2 =
            IndirectPointerInputChange(
                id = PointerId(0L),
                uptimeMillis = moveTime2,
                position = Offset(startX, move2Y),
                pressed = true,
                pressure = 1.0f,
                previousUptimeMillis = moveTime1,
                previousPosition = Offset(startX, move1Y),
                previousPressed = true,
            )
        val moveEvent2 =
            IndirectPointerEvent(
                changes = listOf(moveChange2),
                type = IndirectPointerEventType.Move,
                primaryDirectionalMotionAxis = IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                motionEvent =
                    obtainIndirectMotionEvent(
                        downTime,
                        moveTime2,
                        MotionEvent.ACTION_MOVE,
                        Offset(startX, move2Y),
                    ),
            )
        val moveEvent2Result =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                moveEvent2,
                isConsumed = false,
            )
        assertTrue(moveEvent2Result)
        assertEquals(null, currentFocusDirection)

        // Simulate an up event
        val upTime = moveTime2 + timeBetweenEvents
        val upY = move2Y + flingTriggeringDistanceBetweenEvents
        val upEvent =
            createUpIndirectPointerEvent(
                downTime = downTime,
                uptimeMillis = upTime,
                position = Offset(startX, upY),
                previousUptimeMillis = moveTime2,
                previousPosition = Offset(startX, move2Y),
            )
        val upEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                upEvent,
                isConsumed = false,
            )
        assertTrue(upEventResult)
        assertEquals(null, currentFocusDirection)
    }

    @Test
    fun indirectPointerNavigationGesture_swipeBackwardVertically_doesNotTrigger() {
        val downTime = System.currentTimeMillis()
        val startX = 100f
        val startY = 100f + (3 * flingTriggeringDistanceBetweenEvents)

        // Simulate a down event
        val downEvent =
            createDownIndirectPointerEvent(downTime = downTime, position = Offset(startX, startY))
        val downEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                downEvent,
                isConsumed = false,
            )
        assertTrue(downEventResult)
        assertEquals(null, currentFocusDirection)

        // ACTION_MOVE events
        val moveTime1 = downTime + timeBetweenEvents
        val move1Y = startY - flingTriggeringDistanceBetweenEvents
        val moveChange1 =
            IndirectPointerInputChange(
                id = PointerId(0L),
                uptimeMillis = moveTime1,
                position = Offset(startX, move1Y),
                pressed = true,
                pressure = 1.0f,
                previousUptimeMillis = downTime,
                previousPosition = Offset(startX, startY),
                previousPressed = true,
            )
        val moveEvent1 =
            IndirectPointerEvent(
                changes = listOf(moveChange1),
                type = IndirectPointerEventType.Move,
                primaryDirectionalMotionAxis = IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                motionEvent =
                    obtainIndirectMotionEvent(
                        downTime,
                        moveTime1,
                        MotionEvent.ACTION_MOVE,
                        Offset(startX, move1Y),
                    ),
            )
        val moveEvent1Result =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                moveEvent1,
                isConsumed = false,
            )
        assertTrue(moveEvent1Result)
        assertEquals(null, currentFocusDirection)

        val moveTime2 = moveTime1 + timeBetweenEvents
        val move2Y = move1Y - flingTriggeringDistanceBetweenEvents
        val moveChange2 =
            IndirectPointerInputChange(
                id = PointerId(0L),
                uptimeMillis = moveTime2,
                position = Offset(startX, move2Y),
                pressed = true,
                pressure = 1.0f,
                previousUptimeMillis = moveTime1,
                previousPosition = Offset(startX, move1Y),
                previousPressed = true,
            )
        val moveEvent2 =
            IndirectPointerEvent(
                changes = listOf(moveChange2),
                type = IndirectPointerEventType.Move,
                primaryDirectionalMotionAxis = IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                motionEvent =
                    obtainIndirectMotionEvent(
                        downTime,
                        moveTime2,
                        MotionEvent.ACTION_MOVE,
                        Offset(startX, move2Y),
                    ),
            )
        val moveEvent2Result =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                moveEvent2,
                isConsumed = false,
            )
        assertTrue(moveEvent2Result)
        assertEquals(null, currentFocusDirection)

        // Simulate an up event
        val upTime = moveTime2 + timeBetweenEvents
        val upY = move2Y - flingTriggeringDistanceBetweenEvents
        val upEvent =
            createUpIndirectPointerEvent(
                downTime = downTime,
                uptimeMillis = upTime,
                position = Offset(startX, upY),
                previousUptimeMillis = moveTime2,
                previousPosition = Offset(startX, move2Y),
            )
        val upEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                upEvent,
                isConsumed = false,
            )
        assertTrue(upEventResult)
        assertEquals(null, currentFocusDirection)
    }

    @Test
    fun indirectPointerNavigationGesture_swipeForwardDiagonalSameLargeXAndYDeltas_doesNotTrigger() {
        val downTime = System.currentTimeMillis()
        val startX = 100f
        val startY = 100f

        // Simulate a down event
        val downEvent =
            createDownIndirectPointerEvent(downTime = downTime, position = Offset(startX, startY))
        val downEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                downEvent,
                isConsumed = false,
            )
        assertTrue(downEventResult)
        assertEquals(null, currentFocusDirection)

        // 2. ACTION_MOVE events (simulating rapid movement)
        val moveTime1 = downTime + timeBetweenEvents
        val move1X = startX + flingTriggeringDistanceBetweenEvents
        val move1Y = startY + flingTriggeringDistanceBetweenEvents
        val moveChange1 =
            IndirectPointerInputChange(
                id = PointerId(0L),
                uptimeMillis = moveTime1,
                position = Offset(move1X, move1Y),
                pressed = true,
                pressure = 1.0f,
                previousUptimeMillis = downTime,
                previousPosition = Offset(startX, startY),
                previousPressed = true,
            )
        val moveEvent1 =
            IndirectPointerEvent(
                changes = listOf(moveChange1),
                type = IndirectPointerEventType.Move,
                primaryDirectionalMotionAxis = IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                motionEvent =
                    obtainIndirectMotionEvent(
                        downTime,
                        moveTime1,
                        MotionEvent.ACTION_MOVE,
                        Offset(move1X, move1Y),
                    ),
            )
        val moveEvent1Result =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                moveEvent1,
                isConsumed = false,
            )
        assertTrue(moveEvent1Result)
        assertEquals(null, currentFocusDirection)

        val moveTime2 = moveTime1 + timeBetweenEvents
        val move2X = move1X + flingTriggeringDistanceBetweenEvents
        val move2Y = move1Y + flingTriggeringDistanceBetweenEvents
        val moveChange2 =
            IndirectPointerInputChange(
                id = PointerId(0L),
                uptimeMillis = moveTime2,
                position = Offset(move2X, move2Y),
                pressed = true,
                pressure = 1.0f,
                previousUptimeMillis = moveTime1,
                previousPosition = Offset(move1X, move1Y),
                previousPressed = true,
            )
        val moveEvent2 =
            IndirectPointerEvent(
                changes = listOf(moveChange2),
                type = IndirectPointerEventType.Move,
                primaryDirectionalMotionAxis = IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                motionEvent =
                    obtainIndirectMotionEvent(
                        downTime,
                        moveTime2,
                        MotionEvent.ACTION_MOVE,
                        Offset(move2X, move2Y),
                    ),
            )
        val moveEvent2Result =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                moveEvent2,
                isConsumed = false,
            )
        assertTrue(moveEvent2Result)
        assertEquals(null, currentFocusDirection)

        // Simulate an up event
        val upTime = moveTime2 + timeBetweenEvents
        val upX = move2X + flingTriggeringDistanceBetweenEvents
        val upY = move2Y + flingTriggeringDistanceBetweenEvents
        val upEvent =
            createUpIndirectPointerEvent(
                downTime = downTime,
                uptimeMillis = upTime,
                position = Offset(upX, upY),
                previousUptimeMillis = moveTime2,
                previousPosition = Offset(move2X, move2Y),
            )
        val upEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                upEvent,
                isConsumed = false,
            )
        assertTrue(upEventResult)
        assertEquals(null, currentFocusDirection)
    }

    @Test
    fun indirectPointerNavigationGesture_swipeBackwardDiagonalSameLargeXAndYDeltas_doesNotTrigger() {
        val downTime = System.currentTimeMillis()
        val startX = 100f + (3 * flingTriggeringDistanceBetweenEvents)
        val startY = 100f + (3 * flingTriggeringDistanceBetweenEvents)

        // Simulate a down event
        val downEvent =
            createDownIndirectPointerEvent(downTime = downTime, position = Offset(startX, startY))
        val downEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                downEvent,
                isConsumed = false,
            )
        assertTrue(downEventResult)
        assertEquals(null, currentFocusDirection)

        // 2. ACTION_MOVE events (simulating rapid movement)
        val moveTime1 = downTime + timeBetweenEvents
        val move1X = startX - flingTriggeringDistanceBetweenEvents
        val move1Y = startY - flingTriggeringDistanceBetweenEvents
        val moveChange1 =
            IndirectPointerInputChange(
                id = PointerId(0L),
                uptimeMillis = moveTime1,
                position = Offset(move1X, move1Y),
                pressed = true,
                pressure = 1.0f,
                previousUptimeMillis = downTime,
                previousPosition = Offset(startX, startY),
                previousPressed = true,
            )
        val moveEvent1 =
            IndirectPointerEvent(
                changes = listOf(moveChange1),
                type = IndirectPointerEventType.Move,
                primaryDirectionalMotionAxis = IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                motionEvent =
                    obtainIndirectMotionEvent(
                        downTime,
                        moveTime1,
                        MotionEvent.ACTION_MOVE,
                        Offset(move1X, move1Y),
                    ),
            )
        val moveEvent1Result =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                moveEvent1,
                isConsumed = false,
            )
        assertTrue(moveEvent1Result)
        assertEquals(null, currentFocusDirection)

        val moveTime2 = moveTime1 + timeBetweenEvents
        val move2X = move1X - flingTriggeringDistanceBetweenEvents
        val move2Y = move1Y - flingTriggeringDistanceBetweenEvents
        val moveChange2 =
            IndirectPointerInputChange(
                id = PointerId(0L),
                uptimeMillis = moveTime2,
                position = Offset(move2X, move2Y),
                pressed = true,
                pressure = 1.0f,
                previousUptimeMillis = moveTime1,
                previousPosition = Offset(move1X, move1Y),
                previousPressed = true,
            )
        val moveEvent2 =
            IndirectPointerEvent(
                changes = listOf(moveChange2),
                type = IndirectPointerEventType.Move,
                primaryDirectionalMotionAxis = IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                motionEvent =
                    obtainIndirectMotionEvent(
                        downTime,
                        moveTime2,
                        MotionEvent.ACTION_MOVE,
                        Offset(move2X, move2Y),
                    ),
            )
        val moveEventResult2 =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                moveEvent2,
                isConsumed = false,
            )
        assertTrue(moveEventResult2)
        assertEquals(null, currentFocusDirection)

        // Simulate an up event
        val upTime = moveTime2 + timeBetweenEvents
        val upX = move2X - flingTriggeringDistanceBetweenEvents
        val upY = move2Y - flingTriggeringDistanceBetweenEvents
        val upEvent =
            createUpIndirectPointerEvent(
                downTime = downTime,
                uptimeMillis = upTime,
                position = Offset(upX, upY),
                previousUptimeMillis = moveTime2,
                previousPosition = Offset(move2X, move2Y),
            )
        val upEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                upEvent,
                isConsumed = false,
            )
        assertTrue(upEventResult)
        assertEquals(null, currentFocusDirection)
    }

    @Test
    fun indirectPointerNavigationGesture_swipeForwardDiagonalLargeXDeltaSmallYDelta_triggersNext() {
        val downTime = System.currentTimeMillis()
        val startX = 100f
        val startY = 100f

        // Simulate a down event
        val downEvent =
            createDownIndirectPointerEvent(downTime = downTime, position = Offset(startX, startY))
        val downEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                downEvent,
                isConsumed = false,
            )
        assertTrue(downEventResult)
        assertEquals(null, currentFocusDirection)

        // 2. ACTION_MOVE events (simulating rapid movement)
        val moveTime1 = downTime + timeBetweenEvents
        val move1X = startX + flingTriggeringDistanceBetweenEvents
        val move1Y = startY + nonFlingTriggeringDistanceBetweenEvents
        val moveChange1 =
            IndirectPointerInputChange(
                id = PointerId(0L),
                uptimeMillis = moveTime1,
                position = Offset(move1X, move1Y),
                pressed = true,
                pressure = 1.0f,
                previousUptimeMillis = downTime,
                previousPosition = Offset(startX, startY),
                previousPressed = true,
            )
        val moveEvent1 =
            IndirectPointerEvent(
                changes = listOf(moveChange1),
                type = IndirectPointerEventType.Move,
                primaryDirectionalMotionAxis = IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                motionEvent =
                    obtainIndirectMotionEvent(
                        downTime,
                        moveTime1,
                        MotionEvent.ACTION_MOVE,
                        Offset(move1X, move1Y),
                    ),
            )
        val moveEvent1Result =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                moveEvent1,
                isConsumed = false,
            )
        assertTrue(moveEvent1Result)
        assertEquals(null, currentFocusDirection)

        val moveTime2 = moveTime1 + timeBetweenEvents
        val move2X = move1X + flingTriggeringDistanceBetweenEvents
        val move2Y = move1Y + nonFlingTriggeringDistanceBetweenEvents
        val moveChange2 =
            IndirectPointerInputChange(
                id = PointerId(0L),
                uptimeMillis = moveTime2,
                position = Offset(move2X, move2Y),
                pressed = true,
                pressure = 1.0f,
                previousUptimeMillis = moveTime1,
                previousPosition = Offset(move1X, move1Y),
                previousPressed = true,
            )
        val moveEvent2 =
            IndirectPointerEvent(
                changes = listOf(moveChange2),
                type = IndirectPointerEventType.Move,
                primaryDirectionalMotionAxis = IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                motionEvent =
                    obtainIndirectMotionEvent(
                        downTime,
                        moveTime2,
                        MotionEvent.ACTION_MOVE,
                        Offset(move2X, move2Y),
                    ),
            )
        val moveEvent2Result =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                moveEvent2,
                isConsumed = false,
            )
        assertTrue(moveEvent2Result)
        assertEquals(null, currentFocusDirection)

        // Simulate an up event
        val upTime = moveTime2 + timeBetweenEvents
        val upX = move2X + flingTriggeringDistanceBetweenEvents
        val upY = move2Y + nonFlingTriggeringDistanceBetweenEvents
        val upEvent =
            createUpIndirectPointerEvent(
                downTime = downTime,
                uptimeMillis = upTime,
                position = Offset(upX, upY),
                previousUptimeMillis = moveTime2,
                previousPosition = Offset(move2X, move2Y),
            )
        val upEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                upEvent,
                isConsumed = false,
            )
        assertTrue(upEventResult)
        assertEquals(FocusDirection.Companion.Next, currentFocusDirection!!)
    }

    @Test
    fun indirectPointerNavigationGesture_swipeBackwardDiagonalLargeXDeltaSmallYDelta_triggersPrevious() {
        val downTime = System.currentTimeMillis()
        val startX = 100f + (3 * flingTriggeringDistanceBetweenEvents)
        val startY = 100f + (3 * nonFlingTriggeringDistanceBetweenEvents)

        // Simulate a down event
        val downEvent =
            createDownIndirectPointerEvent(downTime = downTime, position = Offset(startX, startY))
        val downEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                downEvent,
                isConsumed = false,
            )
        assertTrue(downEventResult)
        assertEquals(null, currentFocusDirection)

        // 2. ACTION_MOVE events (simulating rapid movement)
        val moveTime1 = downTime + timeBetweenEvents
        val move1X = startX - flingTriggeringDistanceBetweenEvents
        val move1Y = startY - nonFlingTriggeringDistanceBetweenEvents
        val moveChange1 =
            IndirectPointerInputChange(
                id = PointerId(0L),
                uptimeMillis = moveTime1,
                position = Offset(move1X, move1Y),
                pressed = true,
                pressure = 1.0f,
                previousUptimeMillis = downTime,
                previousPosition = Offset(startX, startY),
                previousPressed = true,
            )
        val moveEvent1 =
            IndirectPointerEvent(
                changes = listOf(moveChange1),
                type = IndirectPointerEventType.Move,
                primaryDirectionalMotionAxis = IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                motionEvent =
                    obtainIndirectMotionEvent(
                        downTime,
                        moveTime1,
                        MotionEvent.ACTION_MOVE,
                        Offset(move1X, move1Y),
                    ),
            )
        val moveEvent1Result =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                moveEvent1,
                isConsumed = false,
            )
        assertTrue(moveEvent1Result)
        assertEquals(null, currentFocusDirection)

        val moveTime2 = moveTime1 + timeBetweenEvents
        val move2X = move1X - flingTriggeringDistanceBetweenEvents
        val move2Y = move1Y - nonFlingTriggeringDistanceBetweenEvents
        val moveChange2 =
            IndirectPointerInputChange(
                id = PointerId(0L),
                uptimeMillis = moveTime2,
                position = Offset(move2X, move2Y),
                pressed = true,
                pressure = 1.0f,
                previousUptimeMillis = moveTime1,
                previousPosition = Offset(move1X, move1Y),
                previousPressed = true,
            )
        val moveEvent2 =
            IndirectPointerEvent(
                changes = listOf(moveChange2),
                type = IndirectPointerEventType.Move,
                primaryDirectionalMotionAxis = IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                motionEvent =
                    obtainIndirectMotionEvent(
                        downTime,
                        moveTime2,
                        MotionEvent.ACTION_MOVE,
                        Offset(move2X, move2Y),
                    ),
            )
        val moveEventResult2 =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                moveEvent2,
                isConsumed = false,
            )
        assertTrue(moveEventResult2)
        assertEquals(null, currentFocusDirection)

        // Simulate an up event
        val upTime = moveTime2 + timeBetweenEvents
        val upX = move2X - flingTriggeringDistanceBetweenEvents
        val upY = move2Y - nonFlingTriggeringDistanceBetweenEvents
        val upEvent =
            createUpIndirectPointerEvent(
                downTime = downTime,
                uptimeMillis = upTime,
                position = Offset(upX, upY),
                previousUptimeMillis = moveTime2,
                previousPosition = Offset(move2X, move2Y),
            )
        val upEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                upEvent,
                isConsumed = false,
            )
        assertTrue(upEventResult)
        assertEquals(FocusDirection.Companion.Previous, currentFocusDirection!!)
    }

    @Test
    fun indirectPointerNavigationGesture_swipeForwardDiagonalSmallXDeltaLargeYDelta_doesNotTrigger() {
        val downTime = System.currentTimeMillis()
        val startX = 100f
        val startY = 100f

        // Simulate a down event
        val downEvent =
            createDownIndirectPointerEvent(downTime = downTime, position = Offset(startX, startY))
        val downEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                downEvent,
                isConsumed = false,
            )
        assertTrue(downEventResult)
        assertEquals(null, currentFocusDirection)

        // 2. ACTION_MOVE events (simulating rapid movement)
        val moveTime1 = downTime + timeBetweenEvents
        val move1X = startX + nonFlingTriggeringDistanceBetweenEvents
        val move1Y = startY + flingTriggeringDistanceBetweenEvents
        val moveChange1 =
            IndirectPointerInputChange(
                id = PointerId(0L),
                uptimeMillis = moveTime1,
                position = Offset(move1X, move1Y),
                pressed = true,
                pressure = 1.0f,
                previousUptimeMillis = downTime,
                previousPosition = Offset(startX, startY),
                previousPressed = true,
            )
        val moveEvent1 =
            IndirectPointerEvent(
                changes = listOf(moveChange1),
                type = IndirectPointerEventType.Move,
                primaryDirectionalMotionAxis = IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                motionEvent =
                    obtainIndirectMotionEvent(
                        downTime,
                        moveTime1,
                        MotionEvent.ACTION_MOVE,
                        Offset(move1X, move1Y),
                    ),
            )
        val moveEvent1Result =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                moveEvent1,
                isConsumed = false,
            )
        assertTrue(moveEvent1Result)
        assertEquals(null, currentFocusDirection)

        val moveTime2 = moveTime1 + timeBetweenEvents
        val move2X = move1X + nonFlingTriggeringDistanceBetweenEvents
        val move2Y = move1Y + flingTriggeringDistanceBetweenEvents
        val moveChange2 =
            IndirectPointerInputChange(
                id = PointerId(0L),
                uptimeMillis = moveTime2,
                position = Offset(move2X, move2Y),
                pressed = true,
                pressure = 1.0f,
                previousUptimeMillis = moveTime1,
                previousPosition = Offset(move1X, move1Y),
                previousPressed = true,
            )
        val moveEvent2 =
            IndirectPointerEvent(
                changes = listOf(moveChange2),
                type = IndirectPointerEventType.Move,
                primaryDirectionalMotionAxis = IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                motionEvent =
                    obtainIndirectMotionEvent(
                        downTime,
                        moveTime2,
                        MotionEvent.ACTION_MOVE,
                        Offset(move2X, move2Y),
                    ),
            )
        val moveEventResult2 =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                moveEvent2,
                isConsumed = false,
            )
        assertTrue(moveEventResult2)
        assertEquals(null, currentFocusDirection)

        // Simulate an up event
        val upTime = moveTime2 + timeBetweenEvents
        val upX = move2X + nonFlingTriggeringDistanceBetweenEvents
        val upY = move2Y + flingTriggeringDistanceBetweenEvents
        val upEvent =
            createUpIndirectPointerEvent(
                downTime = downTime,
                uptimeMillis = upTime,
                position = Offset(upX, upY),
                previousUptimeMillis = moveTime2,
                previousPosition = Offset(move2X, move2Y),
            )
        val upEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                upEvent,
                isConsumed = false,
            )
        assertTrue(upEventResult)
        assertEquals(null, currentFocusDirection)
    }

    @Test
    fun indirectPointerNavigationGesture_swipeBackwardDiagonalSmallXDeltaLargeYDelta_doesNotTrigger() {
        val downTime = System.currentTimeMillis()
        val startX = 100f + (3 * flingTriggeringDistanceBetweenEvents)
        val startY = 100f + (3 * flingTriggeringDistanceBetweenEvents)

        // Simulate a down event
        val downEvent =
            createDownIndirectPointerEvent(downTime = downTime, position = Offset(startX, startY))
        val downEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                downEvent,
                isConsumed = false,
            )
        assertTrue(downEventResult)
        assertEquals(null, currentFocusDirection)

        // 2. ACTION_MOVE events (simulating rapid movement)
        val moveTime1 = downTime + timeBetweenEvents
        val move1X = startX - nonFlingTriggeringDistanceBetweenEvents
        val move1Y = startY - flingTriggeringDistanceBetweenEvents
        val moveChange1 =
            IndirectPointerInputChange(
                id = PointerId(0L),
                uptimeMillis = moveTime1,
                position = Offset(move1X, move1Y),
                pressed = true,
                pressure = 1.0f,
                previousUptimeMillis = downTime,
                previousPosition = Offset(startX, startY),
                previousPressed = true,
            )
        val moveEvent1 =
            IndirectPointerEvent(
                changes = listOf(moveChange1),
                type = IndirectPointerEventType.Move,
                primaryDirectionalMotionAxis = IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                motionEvent =
                    obtainIndirectMotionEvent(
                        downTime,
                        moveTime1,
                        MotionEvent.ACTION_MOVE,
                        Offset(move1X, move1Y),
                    ),
            )
        val moveEvent1Result =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                moveEvent1,
                isConsumed = false,
            )
        assertTrue(moveEvent1Result)
        assertEquals(null, currentFocusDirection)

        val moveTime2 = moveTime1 + timeBetweenEvents
        val move2X = move1X - nonFlingTriggeringDistanceBetweenEvents
        val move2Y = move1Y - flingTriggeringDistanceBetweenEvents
        val moveChange2 =
            IndirectPointerInputChange(
                id = PointerId(0L),
                uptimeMillis = moveTime2,
                position = Offset(move2X, move2Y),
                pressed = true,
                pressure = 1.0f,
                previousUptimeMillis = moveTime1,
                previousPosition = Offset(move1X, move1Y),
                previousPressed = true,
            )
        val moveEvent2 =
            IndirectPointerEvent(
                changes = listOf(moveChange2),
                type = IndirectPointerEventType.Move,
                primaryDirectionalMotionAxis = IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                motionEvent =
                    obtainIndirectMotionEvent(
                        downTime,
                        moveTime2,
                        MotionEvent.ACTION_MOVE,
                        Offset(move2X, move2Y),
                    ),
            )
        val moveEvent2Result =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                moveEvent2,
                isConsumed = false,
            )
        assertTrue(moveEvent2Result)
        assertEquals(null, currentFocusDirection)

        // Simulate an up event
        val upTime = moveTime2 + timeBetweenEvents
        val upX = move2X - nonFlingTriggeringDistanceBetweenEvents
        val upY = move2Y - flingTriggeringDistanceBetweenEvents
        val upEvent =
            createUpIndirectPointerEvent(
                downTime = downTime,
                uptimeMillis = upTime,
                position = Offset(upX, upY),
                previousUptimeMillis = moveTime2,
                previousPosition = Offset(move2X, move2Y),
            )
        val upEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                upEvent,
                isConsumed = false,
            )
        assertTrue(upEventResult)
        assertEquals(null, currentFocusDirection)
    }

    @Test
    fun indirectPointerNavigationGesture_swipeForwardHorizontally_whenDownIsConsumed_doesTrigger() {
        val downTime = System.currentTimeMillis()
        val startX = 100f
        val startY = 100f

        // Simulate a down event that is consumed.
        val downEvent =
            createDownIndirectPointerEvent(downTime = downTime, position = Offset(startX, startY))
        val downEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                downEvent,
                isConsumed = true, // The event is consumed.
            )
        assertTrue(downEventResult)
        assertEquals(null, currentFocusDirection)

        // Simulate a move event that is not consumed.
        val moveTime = downTime + timeBetweenEvents
        val moveX = startX + flingTriggeringDistanceBetweenEvents
        val moveEvent =
            createMoveIndirectPointerEvent(
                downTime = downTime,
                uptimeMillis = moveTime,
                position = Offset(moveX, startY),
                previousUptimeMillis = downTime,
                previousPosition = Offset(startX, startY),
            )
        val moveEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                moveEvent,
                isConsumed = false,
            )
        assertTrue(moveEventResult)
        assertEquals(null, currentFocusDirection)

        // Simulate an up event that is not consumed.
        val upTime = moveTime + timeBetweenEvents
        val upX = moveX + flingTriggeringDistanceBetweenEvents
        val upEvent =
            createUpIndirectPointerEvent(
                downTime = downTime,
                uptimeMillis = upTime,
                position = Offset(upX, startY),
                previousUptimeMillis = moveTime,
                previousPosition = Offset(moveX, startY),
            )
        val upEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                upEvent,
                isConsumed = false,
            )

        // Assert that the fling was not ignored even though a down event was consumed.
        assertTrue(upEventResult)
        assertEquals(FocusDirection.Companion.Next, currentFocusDirection!!)
    }

    @Test
    fun indirectPointerNavigationGesture_swipeForwardHorizontally_whenMoveIsConsumed_doesNotTrigger() {
        val downTime = System.currentTimeMillis()
        val startX = 100f
        val startY = 100f

        // Simulate a down event
        val downEvent =
            createDownIndirectPointerEvent(downTime = downTime, position = Offset(startX, startY))
        val downEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                downEvent,
                isConsumed = false,
            )
        assertTrue(downEventResult)
        assertEquals(null, currentFocusDirection)

        // Simulate a move event that is consumed. This should set the ignore flag.
        val moveTime = downTime + timeBetweenEvents
        val moveX = startX + flingTriggeringDistanceBetweenEvents
        val moveEvent =
            createMoveIndirectPointerEvent(
                downTime = downTime,
                uptimeMillis = moveTime,
                position = Offset(moveX, startY),
                previousUptimeMillis = downTime,
                previousPosition = Offset(startX, startY),
            )
        val moveEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                moveEvent,
                isConsumed = true, // The event is consumed.
            )
        assertTrue(moveEventResult)
        assertEquals(null, currentFocusDirection)

        // Simulate an up event, which would normally trigger the fling.
        val upTime = moveTime + timeBetweenEvents
        val upX = moveX + flingTriggeringDistanceBetweenEvents
        val upEvent =
            createUpIndirectPointerEvent(
                downTime = downTime,
                uptimeMillis = upTime,
                position = Offset(upX, startY),
                previousUptimeMillis = moveTime,
                previousPosition = Offset(moveX, startY),
            )
        val upEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                upEvent,
                isConsumed = false,
            )

        // Assert that the fling was ignored because a move event was consumed.
        assertTrue(upEventResult)
        assertEquals(null, currentFocusDirection)
    }

    @Test
    fun indirectPointerNavigationGesture_swipeForwardHorizontally_whenUpIsConsumed_doesNotTrigger() {
        val downTime = System.currentTimeMillis()
        val startX = 100f
        val startY = 100f

        // Simulate a down event
        val downEvent =
            createDownIndirectPointerEvent(downTime = downTime, position = Offset(startX, startY))
        val downEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                downEvent,
                isConsumed = false,
            )
        assertTrue(downEventResult)
        assertEquals(null, currentFocusDirection)

        // Simulate a move event that is not consumed that would trigger the fling.
        val moveTime = downTime + timeBetweenEvents
        val moveX = startX + flingTriggeringDistanceBetweenEvents
        val moveEvent =
            createMoveIndirectPointerEvent(
                downTime = downTime,
                uptimeMillis = moveTime,
                position = Offset(moveX, startY),
                previousUptimeMillis = downTime,
                previousPosition = Offset(startX, startY),
            )
        val moveEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                moveEvent,
                isConsumed = false,
            )
        assertTrue(moveEventResult)
        assertEquals(null, currentFocusDirection)

        // Simulate an up event that is consumed, which would normally trigger the fling.
        // This should set the ignore flag.
        val upTime = moveTime + timeBetweenEvents
        val upX = moveX + flingTriggeringDistanceBetweenEvents
        val upEvent =
            createUpIndirectPointerEvent(
                downTime = downTime,
                uptimeMillis = upTime,
                position = Offset(upX, startY),
                previousUptimeMillis = moveTime,
                previousPosition = Offset(moveX, startY),
            )
        val upEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                upEvent,
                isConsumed = true,
            )

        // Assert that the fling was ignored because an up event was consumed.
        assertTrue(upEventResult)
        assertEquals(null, currentFocusDirection)
    }

    private fun createDownIndirectPointerEvent(
        downTime: Long,
        position: Offset,
        uptimeMillis: Long = downTime,
        previousUptimeMillis: Long = downTime,
        previousPosition: Offset = position,
        previousPressed: Boolean = false,
    ): IndirectPointerEvent =
        IndirectPointerEvent(
            changes =
                listOf(
                    IndirectPointerInputChange(
                        id = PointerId(0L),
                        uptimeMillis = uptimeMillis,
                        position = position,
                        pressed = true,
                        pressure = 1.0f,
                        previousUptimeMillis = previousUptimeMillis,
                        previousPosition = previousPosition,
                        previousPressed = previousPressed,
                    )
                ),
            type = IndirectPointerEventType.Press,
            primaryDirectionalMotionAxis = IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            motionEvent =
                obtainIndirectMotionEvent(
                    downTime = downTime,
                    eventTime = uptimeMillis,
                    action = MotionEvent.ACTION_DOWN,
                    coordinates = position,
                ),
        )

    private fun createMoveIndirectPointerEvent(
        downTime: Long,
        uptimeMillis: Long,
        position: Offset,
        previousUptimeMillis: Long,
        previousPosition: Offset,
    ): IndirectPointerEvent =
        IndirectPointerEvent(
            changes =
                listOf(
                    IndirectPointerInputChange(
                        id = PointerId(0L),
                        uptimeMillis = uptimeMillis,
                        position = position,
                        pressed = true,
                        pressure = 1.0f,
                        previousUptimeMillis = previousUptimeMillis,
                        previousPosition = previousPosition,
                        previousPressed = true,
                    )
                ),
            type = IndirectPointerEventType.Move,
            primaryDirectionalMotionAxis = IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            motionEvent =
                obtainIndirectMotionEvent(
                    downTime = downTime,
                    eventTime = uptimeMillis,
                    action = MotionEvent.ACTION_MOVE,
                    coordinates = position,
                ),
        )

    private fun createUpIndirectPointerEvent(
        downTime: Long,
        uptimeMillis: Long,
        position: Offset,
        previousUptimeMillis: Long,
        previousPosition: Offset,
    ): IndirectPointerEvent =
        IndirectPointerEvent(
            changes =
                listOf(
                    IndirectPointerInputChange(
                        id = PointerId(0L),
                        uptimeMillis = uptimeMillis,
                        position = position,
                        pressed = false,
                        pressure = 1.0f,
                        previousUptimeMillis = previousUptimeMillis,
                        previousPosition = previousPosition,
                        previousPressed = true,
                    )
                ),
            type = IndirectPointerEventType.Release,
            primaryDirectionalMotionAxis = IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            motionEvent =
                obtainIndirectMotionEvent(
                    downTime = downTime,
                    eventTime = uptimeMillis,
                    action = MotionEvent.ACTION_UP,
                    coordinates = position,
                ),
        )

    private fun obtainIndirectMotionEvent(
        downTime: Long,
        eventTime: Long,
        action: Int,
        coordinates: Offset,
    ): MotionEvent {
        return MotionEvent.obtain(
                /* downTime = */ downTime,
                /* eventTime = */ eventTime,
                /* action = */ action,
                /* x = */ coordinates.x,
                /* y = */ coordinates.y,
                /* metaState = */ 0,
            )
            .apply { source = SOURCE_TOUCH_NAVIGATION }
    }
}
