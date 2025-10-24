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
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.compose.ui.ExperimentalIndirectPointerApi
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.IndirectPointerNavigationGestureDetector
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalIndirectPointerApi::class)
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
            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, startX, startY, 0)
        val downEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(
                    downEvent,
                    primaryDirectionalMotionAxis =
                        IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                ),
                isConsumed = false,
            )
        assertTrue(downEventResult)
        assertEquals(null, currentFocusDirection)

        // 2. ACTION_MOVE events (simulating rapid movement)
        val moveTime1 = downTime + timeBetweenEvents
        val move1X = startX + flingTriggeringDistanceBetweenEvents
        val moveEvent1 =
            MotionEvent.obtain(downTime, moveTime1, MotionEvent.ACTION_MOVE, move1X, startY, 0)
        val moveEventResult1 =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(moveEvent1),
                isConsumed = false,
            )
        assertTrue(moveEventResult1)
        assertEquals(null, currentFocusDirection)

        val moveTime2 = moveTime1 + timeBetweenEvents
        val move2X = move1X + flingTriggeringDistanceBetweenEvents
        val moveEvent2 =
            MotionEvent.obtain(downTime, moveTime2, MotionEvent.ACTION_MOVE, move2X, startY, 0)
        val moveEventResult2 =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(moveEvent2),
                isConsumed = false,
            )
        assertTrue(moveEventResult2)
        assertEquals(null, currentFocusDirection)

        // Simulate an up event
        val upTime = moveTime2 + timeBetweenEvents
        val upX = move2X + flingTriggeringDistanceBetweenEvents
        val upEvent = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, upX, startY, 0)
        val upEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(upEvent),
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
            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, startX, startY, 0)
        val downEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(
                    downEvent,
                    primaryDirectionalMotionAxis =
                        IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                ),
                isConsumed = false,
            )
        assertTrue(downEventResult)
        assertEquals(null, currentFocusDirection)

        // ACTION_MOVE events
        val moveTime1 = downTime + timeBetweenEvents
        val move1X = startX + flingTriggeringDistanceBetweenEvents
        val moveEvent1 =
            MotionEvent.obtain(downTime, moveTime1, MotionEvent.ACTION_MOVE, move1X, startY, 0)
        val moveEvent1Result =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(moveEvent1),
                isConsumed = false,
            )
        assertTrue(moveEvent1Result)
        assertEquals(null, currentFocusDirection)

        val moveTime2 = moveTime1 + timeBetweenEvents
        val move2X = move1X + flingTriggeringDistanceBetweenEvents
        val moveEvent2 =
            MotionEvent.obtain(downTime, moveTime2, MotionEvent.ACTION_MOVE, move2X, startY, 0)
        val moveEvent2Result =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(moveEvent2),
                isConsumed = false,
            )
        assertTrue(moveEvent2Result)
        assertEquals(null, currentFocusDirection)

        // Injects an extra down that should restart the event stream, so the on fling should NOT be
        // triggered.
        val down2Time = moveTime2 + timeBetweenEvents
        val down2X = move2X
        val downEvent2 =
            MotionEvent.obtain(downTime, down2Time, MotionEvent.ACTION_DOWN, down2X, startY, 0)
        val downEvent2Result =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(
                    downEvent2,
                    primaryDirectionalMotionAxis =
                        IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                ),
                isConsumed = false,
            )
        assertTrue(downEvent2Result)
        assertEquals(null, currentFocusDirection)

        // Simulate an up event
        val upTime = down2Time + timeBetweenEvents
        val upX = down2X + flingTriggeringDistanceBetweenEvents
        val upEvent = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, upX, startY, 0)
        val upEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(upEvent),
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
            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, startX, startY, 0)
        val downEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(
                    downEvent,
                    primaryDirectionalMotionAxis =
                        IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                ),
                isConsumed = false,
            )
        assertTrue(downEventResult)
        assertEquals(null, currentFocusDirection)

        // 2. ACTION_MOVE events (simulating rapid movement)
        val moveTime1 = downTime + timeBetweenEvents
        val move1X = startX - flingTriggeringDistanceBetweenEvents
        val moveEvent1 =
            MotionEvent.obtain(downTime, moveTime1, MotionEvent.ACTION_MOVE, move1X, startY, 0)
        val moveEventResult1 =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(moveEvent1),
                isConsumed = false,
            )
        assertTrue(moveEventResult1)
        assertEquals(null, currentFocusDirection)

        val moveTime2 = moveTime1 + timeBetweenEvents
        val move2X = move1X - flingTriggeringDistanceBetweenEvents
        val moveEvent2 =
            MotionEvent.obtain(downTime, moveTime2, MotionEvent.ACTION_MOVE, move2X, startY, 0)
        val moveEventResult2 =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(moveEvent2),
                isConsumed = false,
            )
        assertTrue(moveEventResult2)
        assertEquals(null, currentFocusDirection)

        // Simulate an up event
        val upTime = moveTime2 + timeBetweenEvents
        val upX = move2X - flingTriggeringDistanceBetweenEvents
        val upEvent = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, upX, startY, 0)
        val upEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(upEvent),
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
            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, startX, startY, 0)
        val downEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(
                    downEvent,
                    primaryDirectionalMotionAxis =
                        IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                ),
                isConsumed = false,
            )
        assertTrue(downEventResult)
        assertEquals(null, currentFocusDirection)

        // ACTION_MOVE events
        val moveTime1 = downTime + timeBetweenEvents
        val move1Y = startY + flingTriggeringDistanceBetweenEvents
        val moveEvent1 =
            MotionEvent.obtain(downTime, moveTime1, MotionEvent.ACTION_MOVE, startX, move1Y, 0)
        val moveEvent1Result =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(moveEvent1),
                isConsumed = false,
            )
        assertTrue(moveEvent1Result)
        assertEquals(null, currentFocusDirection)

        val moveTime2 = moveTime1 + timeBetweenEvents
        val move2Y = move1Y + flingTriggeringDistanceBetweenEvents
        val moveEvent2 =
            MotionEvent.obtain(downTime, moveTime2, MotionEvent.ACTION_MOVE, startX, move2Y, 0)
        val moveEvent2Result =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(moveEvent2),
                isConsumed = false,
            )
        assertTrue(moveEvent2Result)
        assertEquals(null, currentFocusDirection)

        // Simulate an up event
        val upTime = moveTime2 + timeBetweenEvents
        val upY = move2Y + flingTriggeringDistanceBetweenEvents
        val upEvent = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, startX, upY, 0)
        val upEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(upEvent),
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
            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, startX, startY, 0)
        val downEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(
                    downEvent,
                    primaryDirectionalMotionAxis =
                        IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                ),
                isConsumed = false,
            )
        assertTrue(downEventResult)
        assertEquals(null, currentFocusDirection)

        // ACTION_MOVE events
        val moveTime1 = downTime + timeBetweenEvents
        val move1Y = startY - flingTriggeringDistanceBetweenEvents
        val moveEvent1 =
            MotionEvent.obtain(downTime, moveTime1, MotionEvent.ACTION_MOVE, startX, move1Y, 0)
        val moveEvent1Result =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(moveEvent1),
                isConsumed = false,
            )
        assertTrue(moveEvent1Result)
        assertEquals(null, currentFocusDirection)

        val moveTime2 = moveTime1 + timeBetweenEvents
        val move2Y = move1Y - flingTriggeringDistanceBetweenEvents
        val moveEvent2 =
            MotionEvent.obtain(downTime, moveTime2, MotionEvent.ACTION_MOVE, startX, move2Y, 0)
        val moveEvent2Result =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(moveEvent2),
                isConsumed = false,
            )
        assertTrue(moveEvent2Result)
        assertEquals(null, currentFocusDirection)

        // Simulate an up event
        val upTime = moveTime2 + timeBetweenEvents
        val upY = move2Y - flingTriggeringDistanceBetweenEvents
        val upEvent = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, startX, upY, 0)
        val upEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(upEvent),
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
            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, startX, startY, 0)
        val downEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(
                    downEvent,
                    primaryDirectionalMotionAxis =
                        IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                ),
                isConsumed = false,
            )
        assertTrue(downEventResult)
        assertEquals(null, currentFocusDirection)

        // 2. ACTION_MOVE events (simulating rapid movement)
        val moveTime1 = downTime + timeBetweenEvents
        val move1X = startX + flingTriggeringDistanceBetweenEvents
        val move1Y = startY + flingTriggeringDistanceBetweenEvents
        val moveEvent1 =
            MotionEvent.obtain(downTime, moveTime1, MotionEvent.ACTION_MOVE, move1X, move1Y, 0)
        val moveEvent1Result =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(moveEvent1),
                isConsumed = false,
            )
        assertTrue(moveEvent1Result)
        assertEquals(null, currentFocusDirection)

        val moveTime2 = moveTime1 + timeBetweenEvents
        val move2X = move1X + flingTriggeringDistanceBetweenEvents
        val move2Y = move1Y + flingTriggeringDistanceBetweenEvents
        val moveEvent2 =
            MotionEvent.obtain(downTime, moveTime2, MotionEvent.ACTION_MOVE, move2X, move2Y, 0)
        val moveEvent2Result =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(moveEvent2),
                isConsumed = false,
            )
        assertTrue(moveEvent2Result)
        assertEquals(null, currentFocusDirection)

        // Simulate an up event
        val upTime = moveTime2 + timeBetweenEvents
        val upX = move2X + flingTriggeringDistanceBetweenEvents
        val upY = move2Y + flingTriggeringDistanceBetweenEvents
        val upEvent = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, upX, upY, 0)
        val upEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(upEvent),
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
            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, startX, startY, 0)
        val downEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(
                    downEvent,
                    primaryDirectionalMotionAxis =
                        IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                ),
                isConsumed = false,
            )
        assertTrue(downEventResult)
        assertEquals(null, currentFocusDirection)

        // 2. ACTION_MOVE events (simulating rapid movement)
        val moveTime1 = downTime + timeBetweenEvents
        val move1X = startX - flingTriggeringDistanceBetweenEvents
        val move1Y = startY - flingTriggeringDistanceBetweenEvents
        val moveEvent1 =
            MotionEvent.obtain(downTime, moveTime1, MotionEvent.ACTION_MOVE, move1X, move1Y, 0)
        val moveEvent1Result =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(moveEvent1),
                isConsumed = false,
            )
        assertTrue(moveEvent1Result)
        assertEquals(null, currentFocusDirection)

        val moveTime2 = moveTime1 + timeBetweenEvents
        val move2X = move1X - flingTriggeringDistanceBetweenEvents
        val move2Y = move1Y - flingTriggeringDistanceBetweenEvents
        val moveEvent2 =
            MotionEvent.obtain(downTime, moveTime2, MotionEvent.ACTION_MOVE, move2X, move2Y, 0)
        val moveEventResult2 =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(moveEvent2),
                isConsumed = false,
            )
        assertTrue(moveEventResult2)
        assertEquals(null, currentFocusDirection)

        // Simulate an up event
        val upTime = moveTime2 + timeBetweenEvents
        val upX = move2X - flingTriggeringDistanceBetweenEvents
        val upY = move2Y - flingTriggeringDistanceBetweenEvents
        val upEvent = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, upX, upY, 0)
        val upEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(upEvent),
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
            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, startX, startY, 0)
        val downEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(
                    downEvent,
                    primaryDirectionalMotionAxis =
                        IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                ),
                isConsumed = false,
            )
        assertTrue(downEventResult)
        assertEquals(null, currentFocusDirection)

        // 2. ACTION_MOVE events (simulating rapid movement)
        val moveTime1 = downTime + timeBetweenEvents
        val move1X = startX + flingTriggeringDistanceBetweenEvents
        val move1Y = startY + nonFlingTriggeringDistanceBetweenEvents
        val moveEvent1 =
            MotionEvent.obtain(downTime, moveTime1, MotionEvent.ACTION_MOVE, move1X, move1Y, 0)
        val moveEvent1Result =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(moveEvent1),
                isConsumed = false,
            )
        assertTrue(moveEvent1Result)
        assertEquals(null, currentFocusDirection)

        val moveTime2 = moveTime1 + timeBetweenEvents
        val move2X = move1X + flingTriggeringDistanceBetweenEvents
        val move2Y = move1Y + nonFlingTriggeringDistanceBetweenEvents
        val moveEvent2 =
            MotionEvent.obtain(downTime, moveTime2, MotionEvent.ACTION_MOVE, move2X, move2Y, 0)
        val moveEvent2Result =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(moveEvent2),
                isConsumed = false,
            )
        assertTrue(moveEvent2Result)
        assertEquals(null, currentFocusDirection)

        // Simulate an up event
        val upTime = moveTime2 + timeBetweenEvents
        val upX = move2X + flingTriggeringDistanceBetweenEvents
        val upY = move2Y + nonFlingTriggeringDistanceBetweenEvents
        val upEvent = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, upX, upY, 0)
        val upEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(upEvent),
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
            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, startX, startY, 0)
        val downEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(
                    downEvent,
                    primaryDirectionalMotionAxis =
                        IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                ),
                isConsumed = false,
            )
        assertTrue(downEventResult)
        assertEquals(null, currentFocusDirection)

        // 2. ACTION_MOVE events (simulating rapid movement)
        val moveTime1 = downTime + timeBetweenEvents
        val move1X = startX - flingTriggeringDistanceBetweenEvents
        val move1Y = startY - nonFlingTriggeringDistanceBetweenEvents
        val moveEvent1 =
            MotionEvent.obtain(downTime, moveTime1, MotionEvent.ACTION_MOVE, move1X, move1Y, 0)
        val moveEvent1Result =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(moveEvent1),
                isConsumed = false,
            )
        assertTrue(moveEvent1Result)
        assertEquals(null, currentFocusDirection)

        val moveTime2 = moveTime1 + timeBetweenEvents
        val move2X = move1X - flingTriggeringDistanceBetweenEvents
        val move2Y = move1Y - nonFlingTriggeringDistanceBetweenEvents
        val moveEvent2 =
            MotionEvent.obtain(downTime, moveTime2, MotionEvent.ACTION_MOVE, move2X, move2Y, 0)
        val moveEventResult2 =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(moveEvent2),
                isConsumed = false,
            )
        assertTrue(moveEventResult2)
        assertEquals(null, currentFocusDirection)

        // Simulate an up event
        val upTime = moveTime2 + timeBetweenEvents
        val upX = move2X - flingTriggeringDistanceBetweenEvents
        val upY = move2Y - nonFlingTriggeringDistanceBetweenEvents
        val upEvent = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, upX, upY, 0)
        val upEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(upEvent),
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
            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, startX, startY, 0)
        val downEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(
                    downEvent,
                    primaryDirectionalMotionAxis =
                        IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                ),
                isConsumed = false,
            )
        assertTrue(downEventResult)
        assertEquals(null, currentFocusDirection)

        // 2. ACTION_MOVE events (simulating rapid movement)
        val moveTime1 = downTime + timeBetweenEvents
        val move1X = startX + nonFlingTriggeringDistanceBetweenEvents
        val move1Y = startY + flingTriggeringDistanceBetweenEvents
        val moveEvent1 =
            MotionEvent.obtain(downTime, moveTime1, MotionEvent.ACTION_MOVE, move1X, move1Y, 0)
        val moveEvent1Result =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(moveEvent1),
                isConsumed = false,
            )
        assertTrue(moveEvent1Result)
        assertEquals(null, currentFocusDirection)

        val moveTime2 = moveTime1 + timeBetweenEvents
        val move2X = move1X + nonFlingTriggeringDistanceBetweenEvents
        val move2Y = move1Y + flingTriggeringDistanceBetweenEvents
        val moveEvent2 =
            MotionEvent.obtain(downTime, moveTime2, MotionEvent.ACTION_MOVE, move2X, move2Y, 0)
        val moveEventResult2 =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(moveEvent2),
                isConsumed = false,
            )
        assertTrue(moveEventResult2)
        assertEquals(null, currentFocusDirection)

        // Simulate an up event
        val upTime = moveTime2 + timeBetweenEvents
        val upX = move2X + nonFlingTriggeringDistanceBetweenEvents
        val upY = move2Y + flingTriggeringDistanceBetweenEvents
        val upEvent = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, upX, upY, 0)
        val upEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(upEvent),
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
            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, startX, startY, 0)
        val downEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(
                    downEvent,
                    primaryDirectionalMotionAxis =
                        IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                ),
                isConsumed = false,
            )
        assertTrue(downEventResult)
        assertEquals(null, currentFocusDirection)

        // 2. ACTION_MOVE events (simulating rapid movement)
        val moveTime1 = downTime + timeBetweenEvents
        val move1X = startX - nonFlingTriggeringDistanceBetweenEvents
        val move1Y = startY - flingTriggeringDistanceBetweenEvents
        val moveEvent1 =
            MotionEvent.obtain(downTime, moveTime1, MotionEvent.ACTION_MOVE, move1X, move1Y, 0)
        val moveEvent1Result =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(moveEvent1),
                isConsumed = false,
            )
        assertTrue(moveEvent1Result)
        assertEquals(null, currentFocusDirection)

        val moveTime2 = moveTime1 + timeBetweenEvents
        val move2X = move1X - nonFlingTriggeringDistanceBetweenEvents
        val move2Y = move1Y - flingTriggeringDistanceBetweenEvents
        val moveEvent2 =
            MotionEvent.obtain(downTime, moveTime2, MotionEvent.ACTION_MOVE, move2X, move2Y, 0)
        val moveEvent2Result =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(moveEvent2),
                isConsumed = false,
            )
        assertTrue(moveEvent2Result)
        assertEquals(null, currentFocusDirection)

        // Simulate an up event
        val upTime = moveTime1 + timeBetweenEvents
        val upX = move1X - nonFlingTriggeringDistanceBetweenEvents
        val upY = move1Y - flingTriggeringDistanceBetweenEvents
        val upEvent = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, upX, upY, 0)
        val upEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(upEvent),
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
            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, startX, startY, 0)
        val downEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(
                    downEvent,
                    primaryDirectionalMotionAxis =
                        IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                ),
                isConsumed = true, // The event is consumed.
            )
        assertTrue(downEventResult)
        assertEquals(null, currentFocusDirection)

        // Simulate a move event that is not consumed.
        val moveTime = downTime + timeBetweenEvents
        val moveX = startX + flingTriggeringDistanceBetweenEvents
        val moveEvent =
            MotionEvent.obtain(downTime, moveTime, MotionEvent.ACTION_MOVE, moveX, startY, 0)
        val moveEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(moveEvent),
                isConsumed = false,
            )
        assertTrue(moveEventResult)
        assertEquals(null, currentFocusDirection)

        // Simulate an up event that is not consumed.
        val upTime = moveTime + timeBetweenEvents
        val upX = moveX + flingTriggeringDistanceBetweenEvents
        val upEvent = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, upX, startY, 0)
        val upEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(upEvent),
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
            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, startX, startY, 0)
        val downEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(
                    downEvent,
                    primaryDirectionalMotionAxis =
                        IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                ),
                isConsumed = false,
            )
        assertTrue(downEventResult)
        assertEquals(null, currentFocusDirection)

        // Simulate a move event that is consumed. This should set the ignore flag.
        val moveTime = downTime + timeBetweenEvents
        val moveX = startX + flingTriggeringDistanceBetweenEvents
        val moveEvent =
            MotionEvent.obtain(downTime, moveTime, MotionEvent.ACTION_MOVE, moveX, startY, 0)
        val moveEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(moveEvent),
                isConsumed = true, // The event is consumed.
            )
        assertTrue(moveEventResult)
        assertEquals(null, currentFocusDirection)

        // Simulate an up event, which would normally trigger the fling.
        val upTime = moveTime + timeBetweenEvents
        val upX = moveX + flingTriggeringDistanceBetweenEvents
        val upEvent = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, upX, startY, 0)
        val upEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(upEvent),
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
            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, startX, startY, 0)
        val downEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(
                    downEvent,
                    primaryDirectionalMotionAxis =
                        IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                ),
                isConsumed = false,
            )
        assertTrue(downEventResult)
        assertEquals(null, currentFocusDirection)

        // Simulate a move event that is not consumed that would trigger the fling.
        val moveTime = downTime + timeBetweenEvents
        val moveX = startX + flingTriggeringDistanceBetweenEvents
        val moveEvent =
            MotionEvent.obtain(downTime, moveTime, MotionEvent.ACTION_MOVE, moveX, startY, 0)
        val moveEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(moveEvent),
                isConsumed = false, // The event is consumed.
            )
        assertTrue(moveEventResult)
        assertEquals(null, currentFocusDirection)

        // Simulate an up event that is consumed, which would normally trigger the fling.
        // This should set the ignore flag.
        val upTime = moveTime + timeBetweenEvents
        val upX = moveX + flingTriggeringDistanceBetweenEvents
        val upEvent = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, upX, startY, 0)
        val upEventResult =
            indirectPointerNavigationGestureDetector.onIndirectPointerEvent(
                IndirectPointerEvent(upEvent),
                isConsumed = true,
            )

        // Assert that the fling was ignored because an up event was consumed.
        assertTrue(upEventResult)
        assertEquals(null, currentFocusDirection)
    }
}
