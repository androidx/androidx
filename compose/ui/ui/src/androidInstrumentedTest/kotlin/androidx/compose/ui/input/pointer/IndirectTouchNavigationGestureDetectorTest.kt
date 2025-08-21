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

import android.content.Context
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.compose.ui.ExperimentalIndirectTouchTypeApi
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.indirect.IndirectTouchEventPrimaryDirectionalMotionAxis
import androidx.compose.ui.platform.IndirectTouchNavigationGestureDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalIndirectTouchTypeApi::class)
@RunWith(JUnit4::class)
class IndirectTouchNavigationGestureDetectorTest {
    private lateinit var context: Context
    private lateinit var indirectTouchNavigationGestureDetector:
        IndirectTouchNavigationGestureDetector

    private val timeBetweenEvents = 20L

    // Triggers GestureDetector's onFling().
    // Note: Simple calculated distance of speed (viewConfiguration.scaledMinimumFlingVelocity) *
    // time will NOT trigger a fling, as it needs to take into account other factors
    // (VelocityTracker and GestureDetector internal fling detection algorithms, touch slop,
    // initial movement, and even possible synthetic input limits). This does that.
    private val flingTriggeringDistanceBetweenEvents = 50
    private val nonFlingTriggeringDistanceBetweenEvents = 10
    private var currentFocusDirection: FocusDirection? = null

    @Suppress("DEPRECATION")
    @get:Rule
    val rule = androidx.test.rule.ActivityTestRule(ComponentActivity::class.java)

    @Before
    fun setup() {
        currentFocusDirection = null
        val activity = rule.activity

        rule.runOnUiThread {
            context = activity
            indirectTouchNavigationGestureDetector =
                IndirectTouchNavigationGestureDetector(
                    context,
                    { focusDirection: FocusDirection -> currentFocusDirection = focusDirection },
                )
            // All tests in file require the primary axis to be X:
            indirectTouchNavigationGestureDetector.primaryDirectionalMotionAxis =
                IndirectTouchEventPrimaryDirectionalMotionAxis.X
        }
    }

    @Test
    fun touchNavigationGesture_swipeForwardHorizontally_triggersNext() {
        val downTime = System.currentTimeMillis()
        val startX = 100f
        val startY = 100f

        // Simulate a down event
        val downEvent =
            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, startX, startY, 0)
        val downEventResult = indirectTouchNavigationGestureDetector.onTouchEvent(downEvent)
        assertTrue(downEventResult)
        assertEquals(null, currentFocusDirection)

        // 2. ACTION_MOVE events (simulating rapid movement)
        val moveTime1 = downTime + timeBetweenEvents
        val move1X = startX + flingTriggeringDistanceBetweenEvents
        val moveEvent1 =
            MotionEvent.obtain(downTime, moveTime1, MotionEvent.ACTION_MOVE, move1X, startY, 0)
        val moveEventResult1 = indirectTouchNavigationGestureDetector.onTouchEvent(moveEvent1)
        assertTrue(moveEventResult1)
        assertEquals(null, currentFocusDirection)

        val moveTime2 = moveTime1 + timeBetweenEvents
        val move2X = move1X + flingTriggeringDistanceBetweenEvents
        val moveEvent2 =
            MotionEvent.obtain(downTime, moveTime2, MotionEvent.ACTION_MOVE, move2X, startY, 0)
        val moveEventResult2 = indirectTouchNavigationGestureDetector.onTouchEvent(moveEvent2)
        assertTrue(moveEventResult2)
        assertEquals(null, currentFocusDirection)

        // Simulate an up event
        val upTime = moveTime2 + timeBetweenEvents
        val upX = move2X + flingTriggeringDistanceBetweenEvents
        val upEvent = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, upX, startY, 0)
        val upEventResult = indirectTouchNavigationGestureDetector.onTouchEvent(upEvent)
        assertTrue(upEventResult)
        assertEquals(FocusDirection.Next, currentFocusDirection!!)
    }

    @Test
    fun touchNavigationGesture_swipeForwardHorizontallyWithExtraDown_doesNotTrigger() {
        val downTime = System.currentTimeMillis()
        val startX = 100f
        val startY = 100f

        // Simulate a down event
        val downEvent =
            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, startX, startY, 0)
        val downEventResult = indirectTouchNavigationGestureDetector.onTouchEvent(downEvent)
        assertTrue(downEventResult)
        assertEquals(null, currentFocusDirection)

        // 2. ACTION_MOVE events (simulating rapid movement)
        val moveTime1 = downTime + timeBetweenEvents
        val move1X = startX + flingTriggeringDistanceBetweenEvents
        val moveEvent1 =
            MotionEvent.obtain(downTime, moveTime1, MotionEvent.ACTION_MOVE, move1X, startY, 0)
        val moveEventResult1 = indirectTouchNavigationGestureDetector.onTouchEvent(moveEvent1)
        assertTrue(moveEventResult1)
        assertEquals(null, currentFocusDirection)

        val moveTime2 = moveTime1 + timeBetweenEvents
        val move2X = move1X + flingTriggeringDistanceBetweenEvents
        val moveEvent2 =
            MotionEvent.obtain(downTime, moveTime2, MotionEvent.ACTION_MOVE, move2X, startY, 0)
        val moveEventResult2 = indirectTouchNavigationGestureDetector.onTouchEvent(moveEvent2)
        assertTrue(moveEventResult2)
        assertEquals(null, currentFocusDirection)

        // Injects an extra down that should restart the event stream, so the on fling should NOT be
        // triggered.
        val down2Time = moveTime2 + timeBetweenEvents
        val down2X = move2X
        val downEvent2 =
            MotionEvent.obtain(downTime, down2Time, MotionEvent.ACTION_DOWN, down2X, startY, 0)
        val downEventResult2 = indirectTouchNavigationGestureDetector.onTouchEvent(downEvent2)
        assertTrue(downEventResult2)
        assertEquals(null, currentFocusDirection)

        // Simulate an up event
        val upTime = down2Time + timeBetweenEvents
        val upX = down2X + flingTriggeringDistanceBetweenEvents
        val upEvent = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, upX, startY, 0)
        val upEventResult = indirectTouchNavigationGestureDetector.onTouchEvent(upEvent)
        assertTrue(upEventResult)
        assertEquals(null, currentFocusDirection)
    }

    @Test
    fun touchNavigationGesture_swipeBackwardHorizontally_triggersPrevious() {
        val downTime = System.currentTimeMillis()
        val startX = 100f + (3 * flingTriggeringDistanceBetweenEvents)
        val startY = 100f

        // Simulate a down event
        val downEvent =
            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, startX, startY, 0)
        val downEventResult = indirectTouchNavigationGestureDetector.onTouchEvent(downEvent)
        assertTrue(downEventResult)
        assertEquals(null, currentFocusDirection)

        // 2. ACTION_MOVE events (simulating rapid movement)
        val moveTime1 = downTime + timeBetweenEvents
        val move1X = startX - flingTriggeringDistanceBetweenEvents
        val moveEvent1 =
            MotionEvent.obtain(downTime, moveTime1, MotionEvent.ACTION_MOVE, move1X, startY, 0)
        val moveEventResult1 = indirectTouchNavigationGestureDetector.onTouchEvent(moveEvent1)
        assertTrue(moveEventResult1)
        assertEquals(null, currentFocusDirection)

        val moveTime2 = moveTime1 + timeBetweenEvents
        val move2X = move1X - flingTriggeringDistanceBetweenEvents
        val moveEvent2 =
            MotionEvent.obtain(downTime, moveTime2, MotionEvent.ACTION_MOVE, move2X, startY, 0)
        val moveEventResult2 = indirectTouchNavigationGestureDetector.onTouchEvent(moveEvent2)
        assertTrue(moveEventResult2)
        assertEquals(null, currentFocusDirection)

        // Simulate an up event
        val upTime = moveTime2 + timeBetweenEvents
        val upX = move2X - flingTriggeringDistanceBetweenEvents
        val upEvent = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, upX, startY, 0)
        val upEventResult = indirectTouchNavigationGestureDetector.onTouchEvent(upEvent)
        assertTrue(upEventResult)
        assertEquals(FocusDirection.Previous, currentFocusDirection!!)
    }

    @Test
    fun touchNavigationGesture_swipeForwardVertically_doesNotTrigger() {
        val downTime = System.currentTimeMillis()
        val startX = 100f
        val startY = 100f

        // Simulate a down event
        val downEvent =
            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, startX, startY, 0)
        val downEventResult = indirectTouchNavigationGestureDetector.onTouchEvent(downEvent)
        assertTrue(downEventResult)
        assertEquals(null, currentFocusDirection)

        // 2. ACTION_MOVE events (simulating rapid movement)
        val moveTime1 = downTime + timeBetweenEvents
        val move1Y = startY + flingTriggeringDistanceBetweenEvents
        val moveEvent1 =
            MotionEvent.obtain(downTime, moveTime1, MotionEvent.ACTION_MOVE, startX, move1Y, 0)
        val moveEventResult1 = indirectTouchNavigationGestureDetector.onTouchEvent(moveEvent1)
        assertTrue(moveEventResult1)
        assertEquals(null, currentFocusDirection)

        val moveTime2 = moveTime1 + timeBetweenEvents
        val move2Y = move1Y + flingTriggeringDistanceBetweenEvents
        val moveEvent2 =
            MotionEvent.obtain(downTime, moveTime2, MotionEvent.ACTION_MOVE, startX, move2Y, 0)
        val moveEventResult2 = indirectTouchNavigationGestureDetector.onTouchEvent(moveEvent2)
        assertTrue(moveEventResult2)
        assertEquals(null, currentFocusDirection)

        // Simulate an up event
        val upTime = moveTime2 + timeBetweenEvents
        val upY = move2Y + flingTriggeringDistanceBetweenEvents
        val upEvent = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, startX, upY, 0)
        val upEventResult = indirectTouchNavigationGestureDetector.onTouchEvent(upEvent)
        assertTrue(upEventResult)
        assertEquals(null, currentFocusDirection)
    }

    @Test
    fun touchNavigationGesture_swipeBackwardVertically_doesNotTrigger() {
        val downTime = System.currentTimeMillis()
        val startX = 100f
        val startY = 100f + (3 * flingTriggeringDistanceBetweenEvents)

        // Simulate a down event
        val downEvent =
            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, startX, startY, 0)
        val downEventResult = indirectTouchNavigationGestureDetector.onTouchEvent(downEvent)
        assertTrue(downEventResult)
        assertEquals(null, currentFocusDirection)

        // 2. ACTION_MOVE events (simulating rapid movement)
        val moveTime1 = downTime + timeBetweenEvents
        val move1Y = startY - flingTriggeringDistanceBetweenEvents
        val moveEvent1 =
            MotionEvent.obtain(downTime, moveTime1, MotionEvent.ACTION_MOVE, startX, move1Y, 0)
        val moveEventResult1 = indirectTouchNavigationGestureDetector.onTouchEvent(moveEvent1)
        assertTrue(moveEventResult1)
        assertEquals(null, currentFocusDirection)

        val moveTime2 = moveTime1 + timeBetweenEvents
        val move2Y = move1Y - flingTriggeringDistanceBetweenEvents
        val moveEvent2 =
            MotionEvent.obtain(downTime, moveTime2, MotionEvent.ACTION_MOVE, startX, move2Y, 0)
        val moveEventResult2 = indirectTouchNavigationGestureDetector.onTouchEvent(moveEvent2)
        assertTrue(moveEventResult2)
        assertEquals(null, currentFocusDirection)

        // Simulate an up event
        val upTime = moveTime2 + timeBetweenEvents
        val upY = move2Y - flingTriggeringDistanceBetweenEvents
        val upEvent = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, startX, upY, 0)
        val upEventResult = indirectTouchNavigationGestureDetector.onTouchEvent(upEvent)
        assertTrue(upEventResult)
        assertEquals(null, currentFocusDirection)
    }

    @Test
    fun touchNavigationGesture_swipeForwardDiagonalSameLargeXAndYDeltas_doesNotTrigger() {
        val downTime = System.currentTimeMillis()
        val startX = 100f
        val startY = 100f

        // Simulate a down event
        val downEvent =
            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, startX, startY, 0)
        val downEventResult = indirectTouchNavigationGestureDetector.onTouchEvent(downEvent)
        assertTrue(downEventResult)
        assertEquals(null, currentFocusDirection)

        // 2. ACTION_MOVE events (simulating rapid movement)
        val moveTime1 = downTime + timeBetweenEvents
        val move1X = startX + flingTriggeringDistanceBetweenEvents
        val move1Y = startY + flingTriggeringDistanceBetweenEvents
        val moveEvent1 =
            MotionEvent.obtain(downTime, moveTime1, MotionEvent.ACTION_MOVE, move1X, move1Y, 0)
        val moveEventResult1 = indirectTouchNavigationGestureDetector.onTouchEvent(moveEvent1)
        assertTrue(moveEventResult1)
        assertEquals(null, currentFocusDirection)

        val moveTime2 = moveTime1 + timeBetweenEvents
        val move2X = move1X + flingTriggeringDistanceBetweenEvents
        val move2Y = move1Y + flingTriggeringDistanceBetweenEvents
        val moveEvent2 =
            MotionEvent.obtain(downTime, moveTime2, MotionEvent.ACTION_MOVE, move2X, move2Y, 0)
        val moveEventResult2 = indirectTouchNavigationGestureDetector.onTouchEvent(moveEvent2)
        assertTrue(moveEventResult2)
        assertEquals(null, currentFocusDirection)

        // Simulate an up event
        val upTime = moveTime2 + timeBetweenEvents
        val upX = move2X + flingTriggeringDistanceBetweenEvents
        val upY = move2Y + flingTriggeringDistanceBetweenEvents
        val upEvent = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, upX, upY, 0)
        val upEventResult = indirectTouchNavigationGestureDetector.onTouchEvent(upEvent)
        assertTrue(upEventResult)
        assertEquals(null, currentFocusDirection)
    }

    @Test
    fun touchNavigationGesture_swipeBackwardDiagonalSameLargeXAndYDeltas_doesNotTrigger() {
        val downTime = System.currentTimeMillis()
        val startX = 100f + (3 * flingTriggeringDistanceBetweenEvents)
        val startY = 100f + (3 * flingTriggeringDistanceBetweenEvents)

        // Simulate a down event
        val downEvent =
            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, startX, startY, 0)
        val downEventResult = indirectTouchNavigationGestureDetector.onTouchEvent(downEvent)
        assertTrue(downEventResult)
        assertEquals(null, currentFocusDirection)

        // 2. ACTION_MOVE events (simulating rapid movement)
        val moveTime1 = downTime + timeBetweenEvents
        val move1X = startX - flingTriggeringDistanceBetweenEvents
        val move1Y = startY - flingTriggeringDistanceBetweenEvents
        val moveEvent1 =
            MotionEvent.obtain(downTime, moveTime1, MotionEvent.ACTION_MOVE, move1X, move1Y, 0)
        val moveEventResult1 = indirectTouchNavigationGestureDetector.onTouchEvent(moveEvent1)
        assertTrue(moveEventResult1)
        assertEquals(null, currentFocusDirection)

        val moveTime2 = moveTime1 + timeBetweenEvents
        val move2X = move1X - flingTriggeringDistanceBetweenEvents
        val move2Y = move1Y - flingTriggeringDistanceBetweenEvents
        val moveEvent2 =
            MotionEvent.obtain(downTime, moveTime2, MotionEvent.ACTION_MOVE, move2X, move2Y, 0)
        val moveEventResult2 = indirectTouchNavigationGestureDetector.onTouchEvent(moveEvent2)
        assertTrue(moveEventResult2)
        assertEquals(null, currentFocusDirection)

        // Simulate an up event
        val upTime = moveTime2 + timeBetweenEvents
        val upX = move2X - flingTriggeringDistanceBetweenEvents
        val upY = move2Y - flingTriggeringDistanceBetweenEvents
        val upEvent = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, upX, upY, 0)
        val upEventResult = indirectTouchNavigationGestureDetector.onTouchEvent(upEvent)
        assertTrue(upEventResult)
        assertEquals(null, currentFocusDirection)
    }

    @Test
    fun touchNavigationGesture_swipeForwardDiagonalLargeXDeltaSmallYDelta_triggersNext() {
        val downTime = System.currentTimeMillis()
        val startX = 100f
        val startY = 100f

        // Simulate a down event
        val downEvent =
            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, startX, startY, 0)
        val downEventResult = indirectTouchNavigationGestureDetector.onTouchEvent(downEvent)
        assertTrue(downEventResult)
        assertEquals(null, currentFocusDirection)

        // 2. ACTION_MOVE events (simulating rapid movement)
        val moveTime1 = downTime + timeBetweenEvents
        val move1X = startX + flingTriggeringDistanceBetweenEvents
        val move1Y = startY + nonFlingTriggeringDistanceBetweenEvents
        val moveEvent1 =
            MotionEvent.obtain(downTime, moveTime1, MotionEvent.ACTION_MOVE, move1X, move1Y, 0)
        val moveEventResult1 = indirectTouchNavigationGestureDetector.onTouchEvent(moveEvent1)
        assertTrue(moveEventResult1)
        assertEquals(null, currentFocusDirection)

        val moveTime2 = moveTime1 + timeBetweenEvents
        val move2X = move1X + flingTriggeringDistanceBetweenEvents
        val move2Y = move1Y + nonFlingTriggeringDistanceBetweenEvents
        val moveEvent2 =
            MotionEvent.obtain(downTime, moveTime2, MotionEvent.ACTION_MOVE, move2X, move2Y, 0)
        val moveEventResult2 = indirectTouchNavigationGestureDetector.onTouchEvent(moveEvent2)
        assertTrue(moveEventResult2)
        assertEquals(null, currentFocusDirection)

        // Simulate an up event
        val upTime = moveTime2 + timeBetweenEvents
        val upX = move2X + flingTriggeringDistanceBetweenEvents
        val upY = move2Y + nonFlingTriggeringDistanceBetweenEvents
        val upEvent = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, upX, upY, 0)
        val upEventResult = indirectTouchNavigationGestureDetector.onTouchEvent(upEvent)
        assertTrue(upEventResult)
        assertEquals(FocusDirection.Next, currentFocusDirection!!)
    }

    @Test
    fun touchNavigationGesture_swipeBackwardDiagonalLargeXDeltaSmallYDelta_triggersNext() {
        val downTime = System.currentTimeMillis()
        val startX = 100f + (3 * flingTriggeringDistanceBetweenEvents)
        val startY = 100f + (3 * flingTriggeringDistanceBetweenEvents)

        // Simulate a down event
        val downEvent =
            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, startX, startY, 0)
        val downEventResult = indirectTouchNavigationGestureDetector.onTouchEvent(downEvent)
        assertTrue(downEventResult)
        assertEquals(null, currentFocusDirection)

        // 2. ACTION_MOVE events (simulating rapid movement)
        val moveTime1 = downTime + timeBetweenEvents
        val move1X = startX - flingTriggeringDistanceBetweenEvents
        val move1Y = startY - nonFlingTriggeringDistanceBetweenEvents
        val moveEvent1 =
            MotionEvent.obtain(downTime, moveTime1, MotionEvent.ACTION_MOVE, move1X, move1Y, 0)
        val moveEventResult1 = indirectTouchNavigationGestureDetector.onTouchEvent(moveEvent1)
        assertTrue(moveEventResult1)
        assertEquals(null, currentFocusDirection)

        val moveTime2 = moveTime1 + timeBetweenEvents
        val move2X = move1X - flingTriggeringDistanceBetweenEvents
        val move2Y = move1Y - nonFlingTriggeringDistanceBetweenEvents
        val moveEvent2 =
            MotionEvent.obtain(downTime, moveTime2, MotionEvent.ACTION_MOVE, move2X, move2Y, 0)
        val moveEventResult2 = indirectTouchNavigationGestureDetector.onTouchEvent(moveEvent2)
        assertTrue(moveEventResult2)
        assertEquals(null, currentFocusDirection)

        // Simulate an up event
        val upTime = moveTime2 + timeBetweenEvents
        val upX = move2X - flingTriggeringDistanceBetweenEvents
        val upY = move2Y - nonFlingTriggeringDistanceBetweenEvents
        val upEvent = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, upX, upY, 0)
        val upEventResult = indirectTouchNavigationGestureDetector.onTouchEvent(upEvent)
        assertTrue(upEventResult)
        assertEquals(FocusDirection.Previous, currentFocusDirection!!)
    }

    @Test
    fun touchNavigationGesture_swipeForwardDiagonalSmallXDeltaLargeYDelta_doesNotTrigger() {
        val downTime = System.currentTimeMillis()
        val startX = 100f
        val startY = 100f

        // Simulate a down event
        val downEvent =
            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, startX, startY, 0)
        val downEventResult = indirectTouchNavigationGestureDetector.onTouchEvent(downEvent)
        assertTrue(downEventResult)
        assertEquals(null, currentFocusDirection)

        // 2. ACTION_MOVE events (simulating rapid movement)
        val moveTime1 = downTime + timeBetweenEvents
        val move1X = startX + nonFlingTriggeringDistanceBetweenEvents
        val move1Y = startY + flingTriggeringDistanceBetweenEvents
        val moveEvent1 =
            MotionEvent.obtain(downTime, moveTime1, MotionEvent.ACTION_MOVE, move1X, move1Y, 0)
        val moveEventResult1 = indirectTouchNavigationGestureDetector.onTouchEvent(moveEvent1)
        assertTrue(moveEventResult1)
        assertEquals(null, currentFocusDirection)

        val moveTime2 = moveTime1 + timeBetweenEvents
        val move2X = move1X + nonFlingTriggeringDistanceBetweenEvents
        val move2Y = move1Y + flingTriggeringDistanceBetweenEvents
        val moveEvent2 =
            MotionEvent.obtain(downTime, moveTime2, MotionEvent.ACTION_MOVE, move2X, move2Y, 0)
        val moveEventResult2 = indirectTouchNavigationGestureDetector.onTouchEvent(moveEvent2)
        assertTrue(moveEventResult2)
        assertEquals(null, currentFocusDirection)

        // Simulate an up event
        val upTime = moveTime2 + timeBetweenEvents
        val upX = move2X + nonFlingTriggeringDistanceBetweenEvents
        val upY = move2Y + flingTriggeringDistanceBetweenEvents
        val upEvent = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, upX, upY, 0)
        val upEventResult = indirectTouchNavigationGestureDetector.onTouchEvent(upEvent)
        assertTrue(upEventResult)
        assertEquals(null, currentFocusDirection)
    }

    @Test
    fun touchNavigationGesture_swipeBackwardDiagonalSmallXDeltaLargeYDelta_doesNotTrigger() {
        val downTime = System.currentTimeMillis()
        val startX = 100f + (3 * flingTriggeringDistanceBetweenEvents)
        val startY = 100f + (3 * flingTriggeringDistanceBetweenEvents)

        // Simulate a down event
        val downEvent =
            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, startX, startY, 0)
        val downEventResult = indirectTouchNavigationGestureDetector.onTouchEvent(downEvent)
        assertTrue(downEventResult)
        assertEquals(null, currentFocusDirection)

        // 2. ACTION_MOVE events (simulating rapid movement)
        val moveTime1 = downTime + timeBetweenEvents
        val move1X = startX - nonFlingTriggeringDistanceBetweenEvents
        val move1Y = startY - flingTriggeringDistanceBetweenEvents
        val moveEvent1 =
            MotionEvent.obtain(downTime, moveTime1, MotionEvent.ACTION_MOVE, move1X, move1Y, 0)
        val moveEventResult1 = indirectTouchNavigationGestureDetector.onTouchEvent(moveEvent1)
        assertTrue(moveEventResult1)
        assertEquals(null, currentFocusDirection)

        val moveTime2 = moveTime1 + timeBetweenEvents
        val move2X = move1X - nonFlingTriggeringDistanceBetweenEvents
        val move2Y = move1Y - flingTriggeringDistanceBetweenEvents
        val moveEvent2 =
            MotionEvent.obtain(downTime, moveTime2, MotionEvent.ACTION_MOVE, move2X, move2Y, 0)
        val moveEventResult2 = indirectTouchNavigationGestureDetector.onTouchEvent(moveEvent2)
        assertTrue(moveEventResult2)
        assertEquals(null, currentFocusDirection)

        // Simulate an up event
        val upTime = moveTime2 + timeBetweenEvents
        val upX = move2X - nonFlingTriggeringDistanceBetweenEvents
        val upY = move2Y - flingTriggeringDistanceBetweenEvents
        val upEvent = MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, upX, upY, 0)
        val upEventResult = indirectTouchNavigationGestureDetector.onTouchEvent(upEvent)
        assertTrue(upEventResult)
        assertEquals(null, currentFocusDirection)
    }
}
