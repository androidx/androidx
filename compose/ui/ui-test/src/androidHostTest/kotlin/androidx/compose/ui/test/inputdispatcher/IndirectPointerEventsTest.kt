/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.test.inputdispatcher

import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_POINTER_DOWN
import android.view.MotionEvent.ACTION_POINTER_UP
import android.view.MotionEvent.ACTION_UP
import androidx.compose.testutils.expectError
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.indirect.IndirectPointerEventPrimaryDirectionalMotionAxis
import androidx.compose.ui.test.AndroidInputDispatcher
import androidx.compose.ui.test.InputDispatcher.Companion.eventPeriodMillis
import androidx.compose.ui.test.RobolectricMinSdk
import androidx.compose.ui.test.util.assertHasValidEventTimes
import androidx.compose.ui.test.util.assertNoIndirectPointerGestureInProgress
import androidx.compose.ui.test.util.verifyIndirectPointerEvent
import androidx.compose.ui.test.util.verifyIndirectPointerEventPointers
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlin.collections.removeFirst as removeFirstKt
import kotlin.math.abs
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// the extra device info, but do we need that? I don't test it here yet.
/** Tests if [AndroidInputDispatcher.enqueueIndirectPointerDown] and friends work. */
@RunWith(AndroidJUnit4::class)
@Config(minSdk = RobolectricMinSdk)
class IndirectPointerEventsTest : InputDispatcherTest() {
    companion object {
        // Pointer ids
        private const val pointer1 = 11
        private const val pointer2 = 22
        private const val pointer3 = 33
        private const val pointer4 = 44

        // Positions, mostly used with corresponding pointerId:
        // pointerX with positionX or positionX_Y
        private val position1 = Offset(1f, 1f)
        private val position2 = Offset(2f, 2f)
        private val position3 = Offset(3f, 3f)
        private val position4 = Offset(4f, 4f)

        private val position1_1 = Offset(11f, 11f)
        private val position2_1 = Offset(21f, 21f)
        private val position3_1 = Offset(31f, 31f)

        private val position1_2 = Offset(12f, 12f)
        private val position2_2 = Offset(22f, 22f)

        private val position1_3 = Offset(13f, 13f)
    }

    @Test
    fun onePointer_down() {
        subject.generateIndirectPointerDownAndCheck(pointer1, position1)
        subject.flush()

        val t = 0L
        recorder.assertHasValidEventTimes()
        assertThat(recorder.events).hasSize(1)

        recorder.events[0].verifyIndirectPointerEvent(1, ACTION_DOWN, 0, t) // pointer1
        recorder.events[0].verifyIndirectPointerEventPointers(pointer1, position1)
    }

    @Test
    fun onePointer_downUp() {
        subject.generateIndirectPointerDownAndCheck(pointer1, position1)
        subject.generateIndirectPointerUpAndCheck(pointer1)
        subject.assertNoIndirectPointerGestureInProgress()
        subject.flush()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            val t = 0L
            assertThat(this).hasSize(2)

            this[0].verifyIndirectPointerEvent(1, ACTION_DOWN, 0, t) // pointer1
            this[0].verifyIndirectPointerEventPointers(pointer1, position1)

            this[1].verifyIndirectPointerEvent(1, ACTION_UP, 0, t) // pointer1
            this[1].verifyIndirectPointerEventPointers(pointer1, position1)
        }
    }

    @Test
    fun onePointer_downDelayUp() {
        subject.generateIndirectPointerDownAndCheck(pointer1, position1_1)
        subject.generateIndirectPointerUpAndCheck(pointer1, 2 * eventPeriodMillis)
        subject.assertNoIndirectPointerGestureInProgress()
        subject.flush()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(2)

            this[0].verifyIndirectPointerEvent(1, ACTION_DOWN, 0, t) // pointer1
            this[0].verifyIndirectPointerEventPointers(pointer1, position1_1)

            t += 2 * eventPeriodMillis
            this[1].verifyIndirectPointerEvent(1, ACTION_UP, 0, t) // pointer1
            this[1].verifyIndirectPointerEventPointers(pointer1, position1_1)
        }
    }

    @Test
    fun onePointer_downUpdateMove() {
        subject.generateIndirectPointerDownAndCheck(pointer1, position1_1)
        subject.updateIndirectPointerAndCheck(pointer1, position1_2)
        subject.advanceEventTime()
        subject.enqueueIndirectPointerMove()
        subject.flush()

        var t = 0L
        recorder.assertHasValidEventTimes()
        assertThat(recorder.events).hasSize(2)
        recorder.events[0].verifyIndirectPointerEvent(1, ACTION_DOWN, 0, t) // pointer1
        recorder.events[0].verifyIndirectPointerEventPointers(pointer1, position1_1)

        t += eventPeriodMillis
        recorder.events[1].verifyIndirectPointerEvent(1, ACTION_MOVE, 0, t) // pointer1
        recorder.events[1].verifyIndirectPointerEventPointers(pointer1, position1_2)
    }

    @Test
    fun onePointer_downCancel() {
        subject.generateIndirectPointerDownAndCheck(pointer1, position1_1)
        subject.advanceEventTime()
        subject.generateCancelAndCheckPointers()
        subject.assertNoIndirectPointerGestureInProgress()
        subject.flush()
        recorder.assertHasValidEventTimes()

        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(2)
            this[0].verifyIndirectPointerEvent(1, ACTION_DOWN, 0, t) // pointer1
            this[0].verifyIndirectPointerEventPointers(pointer1, position1_1)

            t += eventPeriodMillis
            this[1].verifyIndirectPointerEvent(1, ACTION_CANCEL, 0, t)
            this[1].verifyIndirectPointerEventPointers(pointer1, position1_1)
        }
    }

    @Test
    fun twoPointers_downDownMoveMove() {
        // 2 fingers, both go down before they move
        subject.generateIndirectPointerDownAndCheck(pointer1, position1_1)
        subject.generateIndirectPointerDownAndCheck(pointer2, position2_1)
        subject.updateIndirectPointerAndCheck(pointer1, position1_2)
        subject.advanceEventTime()
        subject.enqueueIndirectPointerMove()
        subject.updateIndirectPointerAndCheck(pointer2, position2_2)
        subject.advanceEventTime()
        subject.enqueueIndirectPointerMove()
        subject.flush()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(4)

            this[0].verifyIndirectPointerEvent(1, ACTION_DOWN, 0, t) // pointer1
            this[0].verifyIndirectPointerEventPointers(pointer1, position1_1)

            this[1].verifyIndirectPointerEvent(2, ACTION_POINTER_DOWN, 1, t) // pointer2
            this[1].verifyIndirectPointerEventPointers(pointer1, position1_1)
            this[1].verifyIndirectPointerEventPointers(pointer2, position2_1)

            t += eventPeriodMillis
            this[2].verifyIndirectPointerEvent(2, ACTION_MOVE, 0, t)
            this[2].verifyIndirectPointerEventPointers(pointer1, position1_2)
            this[2].verifyIndirectPointerEventPointers(pointer2, position2_1)

            t += eventPeriodMillis
            this[3].verifyIndirectPointerEvent(2, ACTION_MOVE, 0, t)
            this[3].verifyIndirectPointerEventPointers(pointer1, position1_2)
            this[3].verifyIndirectPointerEventPointers(pointer2, position2_2)
        }
    }

    @Test
    fun twoPointers_downMoveDownMove() {
        // 2 fingers, 1st finger moves before 2nd finger goes down and moves
        subject.generateIndirectPointerDownAndCheck(pointer1, position1_1)
        subject.updateIndirectPointerAndCheck(pointer1, position1_2)
        subject.advanceEventTime()
        subject.enqueueIndirectPointerMove()
        subject.generateIndirectPointerDownAndCheck(pointer2, position2_1)
        subject.updateIndirectPointerAndCheck(pointer2, position2_2)
        subject.advanceEventTime()
        subject.enqueueIndirectPointerMove()
        subject.flush()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(4)

            this[0].verifyIndirectPointerEvent(1, ACTION_DOWN, 0, t) // pointer1
            this[0].verifyIndirectPointerEventPointers(pointer1, position1_1)

            t += eventPeriodMillis
            this[1].verifyIndirectPointerEvent(1, ACTION_MOVE, 0, t)
            this[1].verifyIndirectPointerEventPointers(pointer1, position1_2)

            this[2].verifyIndirectPointerEvent(2, ACTION_POINTER_DOWN, 1, t) // pointer2
            this[2].verifyIndirectPointerEventPointers(pointer1, position1_2)
            this[2].verifyIndirectPointerEventPointers(pointer2, position2_1)

            t += eventPeriodMillis
            this[3].verifyIndirectPointerEvent(2, ACTION_MOVE, 0, t)
            this[3].verifyIndirectPointerEventPointers(pointer1, position1_2)
            this[3].verifyIndirectPointerEventPointers(pointer2, position2_2)
        }
    }

    @Test
    fun twoPointers_moveSimultaneously() {
        // 2 fingers, use [updateIndirectPointer] and [enqueueIndirectPointerMove]
        subject.generateIndirectPointerDownAndCheck(pointer1, position1_1)
        subject.generateIndirectPointerDownAndCheck(pointer2, position2_1)
        subject.updateIndirectPointerAndCheck(pointer1, position1_2)
        subject.updateIndirectPointerAndCheck(pointer2, position2_2)
        subject.advanceEventTime()
        subject.enqueueIndirectPointerMove()
        subject.flush()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(3)

            this[0].verifyIndirectPointerEvent(1, ACTION_DOWN, 0, t) // pointer1
            this[0].verifyIndirectPointerEventPointers(pointer1, position1_1)

            this[1].verifyIndirectPointerEvent(2, ACTION_POINTER_DOWN, 1, t) // pointer2
            this[1].verifyIndirectPointerEventPointers(pointer1, position1_1)
            this[1].verifyIndirectPointerEventPointers(pointer2, position2_1)

            t += eventPeriodMillis
            this[2].verifyIndirectPointerEvent(2, ACTION_MOVE, 0, t)
            this[2].verifyIndirectPointerEventPointers(pointer1, position1_2)
            this[2].verifyIndirectPointerEventPointers(pointer2, position2_2)
        }
    }

    @Test
    fun twoPointers_downUp_sameOrder() {
        subject.generateIndirectPointerDownAndCheck(pointer1, position1_1)
        subject.generateIndirectPointerDownAndCheck(pointer2, position2_1)
        subject.generateIndirectPointerUpAndCheck(pointer2)
        subject.generateIndirectPointerUpAndCheck(pointer1)
        subject.assertNoIndirectPointerGestureInProgress()
        subject.flush()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            val t = 0L
            assertThat(this).hasSize(4)

            this[0].verifyIndirectPointerEvent(1, ACTION_DOWN, 0, t) // pointer1
            this[0].verifyIndirectPointerEventPointers(pointer1, position1_1)

            this[1].verifyIndirectPointerEvent(2, ACTION_POINTER_DOWN, 1, t) // pointer2
            this[1].verifyIndirectPointerEventPointers(pointer1, position1_1)
            this[1].verifyIndirectPointerEventPointers(pointer2, position2_1)

            this[2].verifyIndirectPointerEvent(2, ACTION_POINTER_UP, 1, t) // pointer2
            this[2].verifyIndirectPointerEventPointers(pointer1, position1_1)
            this[2].verifyIndirectPointerEventPointers(pointer2, position2_1)

            this[3].verifyIndirectPointerEvent(1, ACTION_UP, 0, t) // pointer1
            this[3].verifyIndirectPointerEventPointers(pointer1, position1_1)
        }
    }

    @Test
    fun twoPointers_downUp_inverseOrder() {
        subject.generateIndirectPointerDownAndCheck(pointer1, position1_1)
        subject.generateIndirectPointerDownAndCheck(pointer2, position2_1)
        subject.generateIndirectPointerUpAndCheck(pointer1)
        subject.generateIndirectPointerUpAndCheck(pointer2)
        subject.assertNoIndirectPointerGestureInProgress()
        subject.flush()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            val t = 0L
            assertThat(this).hasSize(4)

            this[0].verifyIndirectPointerEvent(1, ACTION_DOWN, 0, t) // pointer1
            this[0].verifyIndirectPointerEventPointers(pointer1, position1_1)

            this[1].verifyIndirectPointerEvent(2, ACTION_POINTER_DOWN, 1, t) // pointer2
            this[1].verifyIndirectPointerEventPointers(pointer1, position1_1)
            this[1].verifyIndirectPointerEventPointers(pointer2, position2_1)

            this[2].verifyIndirectPointerEvent(2, ACTION_POINTER_UP, 0, t) // pointer1
            this[2].verifyIndirectPointerEventPointers(pointer1, position1_1)
            this[2].verifyIndirectPointerEventPointers(pointer2, position2_1)

            this[3].verifyIndirectPointerEvent(1, ACTION_UP, 0, t) // pointer2
            this[3].verifyIndirectPointerEventPointers(pointer2, position2_1)
        }
    }

    @Test
    fun twoPointers_downDownCancel() {
        subject.generateIndirectPointerDownAndCheck(pointer1, position1_1)
        subject.generateIndirectPointerDownAndCheck(pointer2, position2_1)
        subject.advanceEventTime()
        subject.generateCancelAndCheckPointers()
        subject.assertNoIndirectPointerGestureInProgress()
        subject.flush()
        recorder.assertHasValidEventTimes()

        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(3)
            this[0].verifyIndirectPointerEvent(1, ACTION_DOWN, 0, t) // pointer1
            this[0].verifyIndirectPointerEventPointers(pointer1, position1_1)

            this[1].verifyIndirectPointerEvent(2, ACTION_POINTER_DOWN, 1, t) // pointer2
            this[1].verifyIndirectPointerEventPointers(pointer1, position1_1)
            this[1].verifyIndirectPointerEventPointers(pointer2, position2_1)

            t += eventPeriodMillis
            this[2].verifyIndirectPointerEvent(2, ACTION_CANCEL, 0, t)
            this[2].verifyIndirectPointerEventPointers(pointer1, position1_1)
            this[2].verifyIndirectPointerEventPointers(pointer2, position2_1)
        }
    }

    @Test
    fun threePointers_notSimultaneously() {
        // 3 fingers, where the 1st finger goes up before the 3rd finger goes down (no overlap)

        subject.generateIndirectPointerDownAndCheck(pointer1, position1)
        subject.generateIndirectPointerDownAndCheck(pointer2, position2)
        subject.advanceEventTime()
        subject.enqueueIndirectPointerMove()
        subject.generateIndirectPointerUpAndCheck(pointer1)
        subject.advanceEventTime()
        subject.enqueueIndirectPointerMove()
        subject.generateIndirectPointerDownAndCheck(pointer3, position3)
        subject.flush()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(6)

            this[0].verifyIndirectPointerEvent(1, ACTION_DOWN, 0, t) // pointer1
            this[0].verifyIndirectPointerEventPointers(pointer1, position1)

            this[1].verifyIndirectPointerEvent(2, ACTION_POINTER_DOWN, 1, t) // pointer2
            this[1].verifyIndirectPointerEventPointers(pointer1, position1)
            this[1].verifyIndirectPointerEventPointers(pointer2, position2)

            t += eventPeriodMillis
            this[2].verifyIndirectPointerEvent(2, ACTION_MOVE, 0, t)
            this[2].verifyIndirectPointerEventPointers(pointer1, position1)
            this[2].verifyIndirectPointerEventPointers(pointer2, position2)

            this[3].verifyIndirectPointerEvent(2, ACTION_POINTER_UP, 0, t) // pointer1
            this[3].verifyIndirectPointerEventPointers(pointer1, position1)
            this[3].verifyIndirectPointerEventPointers(pointer2, position2)

            t += eventPeriodMillis
            this[4].verifyIndirectPointerEvent(1, ACTION_MOVE, 0, t)
            this[4].verifyIndirectPointerEventPointers(pointer2, position2)

            this[5].verifyIndirectPointerEvent(2, ACTION_POINTER_DOWN, 1, t) // pointer3
            this[5].verifyIndirectPointerEventPointers(pointer2, position2)
            this[5].verifyIndirectPointerEventPointers(pointer3, position3)
        }
    }

    @Test
    fun threePointers_pointerIdReuse() {
        // 3 fingers, where the 1st finger goes up before the 3rd finger goes down, and the 3rd
        // fingers reuses the pointerId of finger 1

        subject.generateIndirectPointerDownAndCheck(pointer1, position1)
        subject.generateIndirectPointerDownAndCheck(pointer2, position2)
        subject.advanceEventTime()
        subject.enqueueIndirectPointerMove()
        subject.generateIndirectPointerUpAndCheck(pointer1)
        subject.advanceEventTime()
        subject.enqueueIndirectPointerMove()
        subject.generateIndirectPointerDownAndCheck(pointer1, position1_2)
        subject.flush()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(6)

            this[0].verifyIndirectPointerEvent(1, ACTION_DOWN, 0, t) // pointer1
            this[0].verifyIndirectPointerEventPointers(pointer1, position1)

            this[1].verifyIndirectPointerEvent(2, ACTION_POINTER_DOWN, 1, t) // pointer2
            this[1].verifyIndirectPointerEventPointers(pointer1, position1)
            this[1].verifyIndirectPointerEventPointers(pointer2, position2)

            t += eventPeriodMillis
            this[2].verifyIndirectPointerEvent(2, ACTION_MOVE, 0, t)
            this[2].verifyIndirectPointerEventPointers(pointer1, position1)
            this[2].verifyIndirectPointerEventPointers(pointer2, position2)

            this[3].verifyIndirectPointerEvent(2, ACTION_POINTER_UP, 0, t) // pointer1
            this[3].verifyIndirectPointerEventPointers(pointer1, position1)
            this[3].verifyIndirectPointerEventPointers(pointer2, position2)

            t += eventPeriodMillis
            this[4].verifyIndirectPointerEvent(1, ACTION_MOVE, 0, t)
            this[4].verifyIndirectPointerEventPointers(pointer2, position2)

            this[5].verifyIndirectPointerEvent(2, ACTION_POINTER_DOWN, 0, t) // pointer1
            this[5].verifyIndirectPointerEventPointers(pointer1, position1_2)
            this[5].verifyIndirectPointerEventPointers(pointer2, position2)
        }
    }

    @Test
    fun fourPointers_downOnly() {
        subject.generateIndirectPointerDownAndCheck(pointer3, position3)
        subject.generateIndirectPointerDownAndCheck(pointer1, position1)
        subject.generateIndirectPointerDownAndCheck(pointer4, position4)
        subject.generateIndirectPointerDownAndCheck(pointer2, position2)
        subject.flush()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            val t = 0L
            assertThat(this).hasSize(4)

            this[0].verifyIndirectPointerEvent(1, ACTION_DOWN, 0, t) // pointer3
            this[0].verifyIndirectPointerEventPointers(pointer3, position3)

            this[1].verifyIndirectPointerEvent(2, ACTION_POINTER_DOWN, 0, t) // pointer1
            this[1].verifyIndirectPointerEventPointers(pointer1, position1)
            this[1].verifyIndirectPointerEventPointers(pointer3, position3)

            this[2].verifyIndirectPointerEvent(3, ACTION_POINTER_DOWN, 2, t) // pointer4
            this[2].verifyIndirectPointerEventPointers(pointer1, position1)
            this[2].verifyIndirectPointerEventPointers(pointer3, position3)
            this[2].verifyIndirectPointerEventPointers(pointer4, position4)

            this[3].verifyIndirectPointerEvent(4, ACTION_POINTER_DOWN, 1, t) // pointer2
            this[3].verifyIndirectPointerEventPointers(pointer1, position1)
            this[3].verifyIndirectPointerEventPointers(pointer2, position2)
            this[3].verifyIndirectPointerEventPointers(pointer3, position3)
            this[3].verifyIndirectPointerEventPointers(pointer4, position4)
        }
    }

    @Test
    fun fourPointers_downWithMove() {
        // 4 fingers, going down at different times

        subject.generateIndirectPointerDownAndCheck(pointer3, position3)
        subject.advanceEventTime()
        subject.enqueueIndirectPointerMove()
        subject.generateIndirectPointerDownAndCheck(pointer1, position1)
        subject.generateIndirectPointerDownAndCheck(pointer2, position2)
        subject.advanceEventTime()
        subject.enqueueIndirectPointerMove()
        subject.advanceEventTime()
        subject.enqueueIndirectPointerMove()
        subject.advanceEventTime()
        subject.enqueueIndirectPointerMove()
        subject.generateIndirectPointerDownAndCheck(pointer4, position4)
        subject.flush()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(8)

            this[0].verifyIndirectPointerEvent(1, ACTION_DOWN, 0, t) // pointer3
            this[0].verifyIndirectPointerEventPointers(pointer3, position3)

            t += eventPeriodMillis
            this[1].verifyIndirectPointerEvent(1, ACTION_MOVE, 0, t)
            this[1].verifyIndirectPointerEventPointers(pointer3, position3)

            this[2].verifyIndirectPointerEvent(2, ACTION_POINTER_DOWN, 0, t) // pointer1
            this[2].verifyIndirectPointerEventPointers(pointer1, position1)
            this[2].verifyIndirectPointerEventPointers(pointer3, position3)

            this[3].verifyIndirectPointerEvent(3, ACTION_POINTER_DOWN, 1, t) // pointer2
            this[3].verifyIndirectPointerEventPointers(pointer1, position1)
            this[3].verifyIndirectPointerEventPointers(pointer2, position2)
            this[3].verifyIndirectPointerEventPointers(pointer3, position3)

            for (i in 4..6) {
                t += eventPeriodMillis
                this[i].verifyIndirectPointerEvent(3, ACTION_MOVE, 0, t)
                this[i].verifyIndirectPointerEventPointers(pointer1, position1)
                this[i].verifyIndirectPointerEventPointers(pointer2, position2)
                this[i].verifyIndirectPointerEventPointers(pointer3, position3)
            }

            this[7].verifyIndirectPointerEvent(4, ACTION_POINTER_DOWN, 3, t) // pointer4
            this[7].verifyIndirectPointerEventPointers(pointer1, position1)
            this[7].verifyIndirectPointerEventPointers(pointer2, position2)
            this[7].verifyIndirectPointerEventPointers(pointer3, position3)
            this[7].verifyIndirectPointerEventPointers(pointer4, position4)
        }
    }

    @Test
    fun fourPointerEventsProperlyAdjustMainTestClock_downWithMove() {
        var totalTimeChangeMillis = 0L
        // There will be a slight difference from when this val is calculated and when the clock
        // sets its initial time (also System.currentTimeMillis()) on the first down event.
        val systemTimeStart = System.currentTimeMillis()

        // 4 fingers, going down at different times
        subject.generateIndirectPointerDownAndCheck(pointer3, position3)

        subject.advanceEventTime(eventPeriodMillis)
        totalTimeChangeMillis += eventPeriodMillis

        subject.enqueueIndirectPointerMove()
        subject.generateIndirectPointerDownAndCheck(pointer1, position1)
        subject.generateIndirectPointerDownAndCheck(pointer2, position2)

        subject.advanceEventTime(eventPeriodMillis)
        totalTimeChangeMillis += eventPeriodMillis

        subject.enqueueIndirectPointerMove()

        subject.advanceEventTime(eventPeriodMillis)
        totalTimeChangeMillis += eventPeriodMillis

        subject.enqueueIndirectPointerMove()

        subject.advanceEventTime(eventPeriodMillis)
        totalTimeChangeMillis += eventPeriodMillis

        subject.enqueueIndirectPointerMove()
        subject.generateIndirectPointerDownAndCheck(pointer4, position4)
        subject.flush()

        val predictedClockTime = systemTimeStart + totalTimeChangeMillis

        val diffBetweenMainTestClockAndPredictedTime = subject.currentTime - predictedClockTime

        // Most important assert in test (estimate since we grab system time slightly before the
        // test clock sets it).
        assertThat(abs(diffBetweenMainTestClockAndPredictedTime)).isLessThan(10L)
        recorder.assertHasValidEventTimes()
    }

    @Test
    fun enqueueIndirectPointerDown_flushesPointerMovement() {
        // Movement from [updateIndirectPointer] that hasn't been sent will be sent when sending
        // DOWN
        subject.generateIndirectPointerDownAndCheck(pointer1, position1_1)
        subject.generateIndirectPointerDownAndCheck(pointer2, position2_1)
        subject.updateIndirectPointerAndCheck(pointer1, position1_2)
        subject.updateIndirectPointerAndCheck(pointer1, position1_3)
        subject.advanceEventTime()
        subject.generateIndirectPointerDownAndCheck(pointer3, position3_1)
        subject.flush()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(4)

            this[0].verifyIndirectPointerEvent(1, ACTION_DOWN, 0, t) // pointer1
            this[0].verifyIndirectPointerEventPointers(pointer1, position1_1)

            this[1].verifyIndirectPointerEvent(2, ACTION_POINTER_DOWN, 1, t) // pointer2
            this[1].verifyIndirectPointerEventPointers(pointer1, position1_1)
            this[1].verifyIndirectPointerEventPointers(pointer2, position2_1)

            t += eventPeriodMillis
            this[2].verifyIndirectPointerEvent(2, ACTION_MOVE, 0, t)
            this[2].verifyIndirectPointerEventPointers(pointer1, position1_3)
            this[2].verifyIndirectPointerEventPointers(pointer2, position2_1)

            this[3].verifyIndirectPointerEvent(3, ACTION_POINTER_DOWN, 2, t) // pointer2
            this[3].verifyIndirectPointerEventPointers(pointer1, position1_3)
            this[3].verifyIndirectPointerEventPointers(pointer2, position2_1)
            this[3].verifyIndirectPointerEventPointers(pointer3, position3_1)
        }
    }

    @Test
    fun enqueueIndirectPointerUp_flushesPointerMovement() {
        // Movement from [updateIndirectPointer] that hasn't been sent will be sent when sending UP
        subject.generateIndirectPointerDownAndCheck(pointer1, position1_1)
        subject.generateIndirectPointerDownAndCheck(pointer2, position2_1)
        subject.updateIndirectPointerAndCheck(pointer1, position1_2)
        subject.updateIndirectPointerAndCheck(pointer1, position1_3)
        subject.advanceEventTime()
        subject.generateIndirectPointerUpAndCheck(pointer1)
        subject.flush()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(3)

            this[0].verifyIndirectPointerEvent(1, ACTION_DOWN, 0, t) // pointer1
            this[0].verifyIndirectPointerEventPointers(pointer1, position1_1)

            this[1].verifyIndirectPointerEvent(2, ACTION_POINTER_DOWN, 1, t) // pointer2
            this[1].verifyIndirectPointerEventPointers(pointer1, position1_1)
            this[1].verifyIndirectPointerEventPointers(pointer2, position2_1)

            t += eventPeriodMillis
            this[2].verifyIndirectPointerEvent(2, ACTION_POINTER_UP, 0, t) // pointer1
            this[2].verifyIndirectPointerEventPointers(pointer1, position1_3)
            this[2].verifyIndirectPointerEventPointers(pointer2, position2_1)
        }
    }

    @Test
    fun enqueueIndirectPointerCancel_doesNotFlushPointerMovement() {
        // 2 fingers, both with pending movement.
        // CANCEL doesn't force a MOVE, but _does_ reflect the latest positions
        subject.generateIndirectPointerDownAndCheck(pointer1, position1_1)
        subject.generateIndirectPointerDownAndCheck(pointer2, position2_1)
        subject.updateIndirectPointerAndCheck(pointer1, position1_2)
        subject.updateIndirectPointerAndCheck(pointer2, position2_2)
        subject.advanceEventTime()
        subject.generateCancelAndCheckPointers()
        subject.flush()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(3)
            this[0].verifyIndirectPointerEvent(1, ACTION_DOWN, 0, t) // pointer1
            this[0].verifyIndirectPointerEventPointers(pointer1, position1_1)

            this[1].verifyIndirectPointerEvent(2, ACTION_POINTER_DOWN, 1, t) // pointer2
            this[1].verifyIndirectPointerEventPointers(pointer1, position1_1)
            this[1].verifyIndirectPointerEventPointers(pointer2, position2_1)

            t += eventPeriodMillis
            this[2].verifyIndirectPointerEvent(2, ACTION_CANCEL, 0, t)
            this[2].verifyIndirectPointerEventPointers(pointer1, position1_2)
            this[2].verifyIndirectPointerEventPointers(pointer2, position2_2)
        }
    }

    private fun AndroidInputDispatcher.generateCancelAndCheckPointers() {
        generateIndirectPointerCancelAndCheck()
        assertThat(getCurrentIndirectPointerPosition(pointer1)).isNull()
        assertThat(getCurrentIndirectPointerPosition(pointer2)).isNull()
        assertThat(getCurrentIndirectPointerPosition(pointer3)).isNull()
    }

    @Test
    fun enqueueIndirectPointerDown_afterDown() {
        subject.enqueueIndirectPointerDown(
            pointerId = pointer1,
            position = position1,
            indirectPointerEventPrimaryDirectionalMotionAxis =
                IndirectPointerEventPrimaryDirectionalMotionAxis.None,
        )
        expectError<IllegalArgumentException> {
            subject.enqueueIndirectPointerDown(
                pointerId = pointer1,
                position = position2,
                indirectPointerEventPrimaryDirectionalMotionAxis =
                    IndirectPointerEventPrimaryDirectionalMotionAxis.None,
            )
        }
    }

    @Test
    fun updateIndirectPointerPointer_withoutDown() {
        expectError<IllegalStateException> { subject.updateIndirectPointer(pointer1, position1_1) }
    }

    @Test
    fun updateIndirectPointerPointer_wrongPointerId() {
        subject.enqueueIndirectPointerDown(
            pointerId = pointer1,
            position = position1_1,
            indirectPointerEventPrimaryDirectionalMotionAxis =
                IndirectPointerEventPrimaryDirectionalMotionAxis.None,
        )
        expectError<IllegalArgumentException> {
            subject.updateIndirectPointer(pointer2, position1_2)
        }
    }

    @Test
    fun updateIndirectPointerPointer_afterUp() {
        subject.enqueueIndirectPointerDown(
            pointerId = pointer1,
            position = position1_1,
            indirectPointerEventPrimaryDirectionalMotionAxis =
                IndirectPointerEventPrimaryDirectionalMotionAxis.None,
        )
        subject.enqueueIndirectPointerUp(pointer1)
        expectError<IllegalStateException> { subject.updateIndirectPointer(pointer1, position1_2) }
    }

    @Test
    fun updateIndirectPointerPointer_afterCancel() {
        subject.enqueueIndirectPointerDown(
            pointerId = pointer1,
            position = position1_1,
            indirectPointerEventPrimaryDirectionalMotionAxis =
                IndirectPointerEventPrimaryDirectionalMotionAxis.None,
        )
        subject.enqueueIndirectPointerCancel()
        expectError<IllegalStateException> { subject.updateIndirectPointer(pointer1, position1_2) }
    }

    @Test
    fun enqueueIndirectPointerMove_withoutDown() {
        expectError<IllegalStateException> { subject.enqueueIndirectPointerMove() }
    }

    @Test
    fun enqueueIndirectPointerMove_afterUp() {
        subject.enqueueIndirectPointerDown(
            pointerId = pointer1,
            position = position1_1,
            indirectPointerEventPrimaryDirectionalMotionAxis =
                IndirectPointerEventPrimaryDirectionalMotionAxis.None,
        )
        subject.enqueueIndirectPointerUp(pointer1)
        expectError<IllegalStateException> { subject.enqueueIndirectPointerMove() }
    }

    @Test
    fun enqueueIndirectPointerMove_afterCancel() {
        subject.enqueueIndirectPointerDown(
            pointerId = pointer1,
            position = position1_1,
            indirectPointerEventPrimaryDirectionalMotionAxis =
                IndirectPointerEventPrimaryDirectionalMotionAxis.None,
        )
        subject.enqueueIndirectPointerCancel()
        expectError<IllegalStateException> { subject.enqueueIndirectPointerMove() }
    }

    @Test
    fun enqueueIndirectPointerUp_withoutDown() {
        expectError<IllegalStateException> { subject.enqueueIndirectPointerUp(pointer1) }
    }

    @Test
    fun enqueueIndirectPointerUp_wrongPointerId() {
        subject.enqueueIndirectPointerDown(
            pointerId = pointer1,
            position = position1_1,
            indirectPointerEventPrimaryDirectionalMotionAxis =
                IndirectPointerEventPrimaryDirectionalMotionAxis.None,
        )
        expectError<IllegalArgumentException> { subject.enqueueIndirectPointerUp(pointer2) }
    }

    @Test
    fun enqueueIndirectPointerUp_afterUp() {
        subject.enqueueIndirectPointerDown(
            pointerId = pointer1,
            position = position1_1,
            indirectPointerEventPrimaryDirectionalMotionAxis =
                IndirectPointerEventPrimaryDirectionalMotionAxis.None,
        )
        subject.enqueueIndirectPointerUp(pointer1)
        expectError<IllegalStateException> { subject.enqueueIndirectPointerUp(pointer1) }
    }

    @Test
    fun enqueueIndirectPointerUp_afterCancel() {
        subject.enqueueIndirectPointerDown(
            pointerId = pointer1,
            position = position1_1,
            indirectPointerEventPrimaryDirectionalMotionAxis =
                IndirectPointerEventPrimaryDirectionalMotionAxis.None,
        )
        subject.enqueueIndirectPointerCancel()
        expectError<IllegalStateException> { subject.enqueueIndirectPointerUp(pointer1) }
    }

    @Test
    fun enqueueIndirectPointerCancel_withoutDown() {
        expectError<IllegalStateException> { subject.enqueueIndirectPointerCancel() }
    }

    @Test
    fun enqueueIndirectPointerCancel_afterUp() {
        subject.enqueueIndirectPointerDown(
            pointerId = pointer1,
            position = position1_1,
            indirectPointerEventPrimaryDirectionalMotionAxis =
                IndirectPointerEventPrimaryDirectionalMotionAxis.None,
        )
        subject.enqueueIndirectPointerUp(pointer1)
        expectError<IllegalStateException> { subject.enqueueIndirectPointerCancel() }
    }

    @Test
    fun enqueueIndirectPointerCancel_afterCancel() {
        subject.enqueueIndirectPointerDown(
            pointerId = pointer1,
            position = position1_1,
            indirectPointerEventPrimaryDirectionalMotionAxis =
                IndirectPointerEventPrimaryDirectionalMotionAxis.None,
        )
        subject.enqueueIndirectPointerCancel()
        expectError<IllegalStateException> { subject.enqueueIndirectPointerCancel() }
    }
}

private fun <E> MutableList<E>.removeFirst(n: Int): List<E> {
    return mutableListOf<E>().also { result -> repeat(n) { result.add(removeFirstKt()) } }
}
