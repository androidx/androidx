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

package androidx.ui.test.inputdispatcher

import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_POINTER_DOWN
import android.view.MotionEvent.ACTION_POINTER_UP
import androidx.test.filters.SmallTest
import androidx.ui.test.android.AndroidInputDispatcher
import androidx.ui.test.util.MotionEventRecorder
import androidx.ui.test.util.assertHasValidEventTimes
import androidx.ui.test.util.expectError
import androidx.ui.test.util.verifyEvent
import androidx.ui.test.util.verifyPointer
import androidx.ui.unit.PxPosition
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

/**
 * Tests if [AndroidInputDispatcher.movePointer] and [AndroidInputDispatcher.sendMove] work
 */
@SmallTest
class SendMoveTest {
    companion object {
        // pointerIds
        private const val pointer1 = 11
        private const val pointer2 = 22
        private const val pointer3 = 33

        // positions (used with corresponding pointerId: pointerX with positionX_Y)
        private val position1_1 = PxPosition(11f, 11f)
        private val position2_1 = PxPosition(21f, 21f)
        private val position3_1 = PxPosition(31f, 31f)

        private val position1_2 = PxPosition(12f, 12f)
        private val position2_2 = PxPosition(22f, 22f)

        private val position1_3 = PxPosition(13f, 13f)
    }

    private val dispatcherRule = AndroidInputDispatcher.TestRule(disableDispatchInRealTime = true)
    private val eventPeriod get() = dispatcherRule.eventPeriod

    @get:Rule
    val inputDispatcherRule: TestRule = dispatcherRule

    private val recorder = MotionEventRecorder()
    private val subject = AndroidInputDispatcher(recorder::recordEvent)

    @After
    fun tearDown() {
        recorder.disposeEvents()
    }

    private fun AndroidInputDispatcher.sendCancelAndCheckPointers() {
        sendCancelAndCheck()
        assertThat(getCurrentPosition(pointer1)).isNull()
        assertThat(getCurrentPosition(pointer2)).isNull()
        assertThat(getCurrentPosition(pointer3)).isNull()
    }

    @Test
    fun onePointer() {
        subject.sendDownAndCheck(pointer1, position1_1)
        subject.movePointerAndCheck(pointer1, position1_2)
        subject.sendMove()

        var t = 0L
        recorder.assertHasValidEventTimes()
        assertThat(recorder.events).hasSize(2)
        recorder.events[0].verifyEvent(1, ACTION_DOWN, 0, t) // pointer1
        recorder.events[0].verifyPointer(pointer1, position1_1)

        t += eventPeriod
        recorder.events[1].verifyEvent(1, ACTION_MOVE, 0, t) // pointer1
        recorder.events[1].verifyPointer(pointer1, position1_2)
    }

    @Test
    fun twoPointers_downDownMoveMove() {
        // 2 fingers, both go down before they move
        subject.sendDownAndCheck(pointer1, position1_1)
        subject.sendDownAndCheck(pointer2, position2_1)
        subject.movePointerAndCheck(pointer1, position1_2)
        subject.sendMove()
        subject.movePointerAndCheck(pointer2, position2_2)
        subject.sendMove()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(4)

            this[0].verifyEvent(1, ACTION_DOWN, 0, t) // pointer1
            this[0].verifyPointer(pointer1, position1_1)

            this[1].verifyEvent(2, ACTION_POINTER_DOWN, 1, t) // pointer2
            this[1].verifyPointer(pointer1, position1_1)
            this[1].verifyPointer(pointer2, position2_1)

            t += eventPeriod
            this[2].verifyEvent(2, ACTION_MOVE, 0, t)
            this[2].verifyPointer(pointer1, position1_2)
            this[2].verifyPointer(pointer2, position2_1)

            t += eventPeriod
            this[3].verifyEvent(2, ACTION_MOVE, 0, t)
            this[3].verifyPointer(pointer1, position1_2)
            this[3].verifyPointer(pointer2, position2_2)
        }
    }

    @Test
    fun twoPointers_downMoveDownMove() {
        // 2 fingers, 1st finger moves before 2nd finger goes down and moves
        subject.sendDownAndCheck(pointer1, position1_1)
        subject.movePointerAndCheck(pointer1, position1_2)
        subject.sendMove()
        subject.sendDownAndCheck(pointer2, position2_1)
        subject.movePointerAndCheck(pointer2, position2_2)
        subject.sendMove()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(4)

            this[0].verifyEvent(1, ACTION_DOWN, 0, t) // pointer1
            this[0].verifyPointer(pointer1, position1_1)

            t += eventPeriod
            this[1].verifyEvent(1, ACTION_MOVE, 0, t)
            this[1].verifyPointer(pointer1, position1_2)

            this[2].verifyEvent(2, ACTION_POINTER_DOWN, 1, t) // pointer2
            this[2].verifyPointer(pointer1, position1_2)
            this[2].verifyPointer(pointer2, position2_1)

            t += eventPeriod
            this[3].verifyEvent(2, ACTION_MOVE, 0, t)
            this[3].verifyPointer(pointer1, position1_2)
            this[3].verifyPointer(pointer2, position2_2)
        }
    }

    @Test
    fun movePointer_oneMovePerPointer() {
        // 2 fingers, use [movePointer] and [sendMove]
        subject.sendDownAndCheck(pointer1, position1_1)
        subject.sendDownAndCheck(pointer2, position2_1)
        subject.movePointerAndCheck(pointer1, position1_2)
        subject.movePointerAndCheck(pointer2, position2_2)
        subject.sendMove()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(3)

            this[0].verifyEvent(1, ACTION_DOWN, 0, t) // pointer1
            this[0].verifyPointer(pointer1, position1_1)

            this[1].verifyEvent(2, ACTION_POINTER_DOWN, 1, t) // pointer2
            this[1].verifyPointer(pointer1, position1_1)
            this[1].verifyPointer(pointer2, position2_1)

            t += eventPeriod
            this[2].verifyEvent(2, ACTION_MOVE, 0, t)
            this[2].verifyPointer(pointer1, position1_2)
            this[2].verifyPointer(pointer2, position2_2)
        }
    }

    @Test
    fun movePointer_multipleMovesPerPointer() {
        // 2 fingers, do several [movePointer]s and then [sendMove]
        subject.sendDownAndCheck(pointer1, position1_1)
        subject.sendDownAndCheck(pointer2, position2_1)
        subject.movePointerAndCheck(pointer1, position1_2)
        subject.movePointerAndCheck(pointer1, position1_3)
        subject.sendMove()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(3)

            this[0].verifyEvent(1, ACTION_DOWN, 0, t) // pointer1
            this[0].verifyPointer(pointer1, position1_1)

            this[1].verifyEvent(2, ACTION_POINTER_DOWN, 1, t) // pointer2
            this[1].verifyPointer(pointer1, position1_1)
            this[1].verifyPointer(pointer2, position2_1)

            t += eventPeriod
            this[2].verifyEvent(2, ACTION_MOVE, 0, t)
            this[2].verifyPointer(pointer1, position1_3)
            this[2].verifyPointer(pointer2, position2_1)
        }
    }

    @Test
    fun sendMoveWithoutMovePointer() {
        // 2 fingers, do [sendMove] without [movePointer]
        subject.sendDownAndCheck(pointer1, position1_1)
        subject.sendDownAndCheck(pointer2, position2_1)
        subject.sendMove()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(3)

            this[0].verifyEvent(1, ACTION_DOWN, 0, t) // pointer1
            this[0].verifyPointer(pointer1, position1_1)

            this[1].verifyEvent(2, ACTION_POINTER_DOWN, 1, t) // pointer2
            this[1].verifyPointer(pointer1, position1_1)
            this[1].verifyPointer(pointer2, position2_1)

            t += eventPeriod
            this[2].verifyEvent(2, ACTION_MOVE, 0, t)
            this[2].verifyPointer(pointer1, position1_1)
            this[2].verifyPointer(pointer2, position2_1)
        }
    }

    @Test
    fun downFlushesPointerMovement() {
        // Movement from [movePointer] that hasn't been sent will be sent when sending DOWN
        subject.sendDownAndCheck(pointer1, position1_1)
        subject.sendDownAndCheck(pointer2, position2_1)
        subject.movePointerAndCheck(pointer1, position1_2)
        subject.movePointerAndCheck(pointer1, position1_3)
        subject.sendDownAndCheck(pointer3, position3_1)

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(4)

            this[0].verifyEvent(1, ACTION_DOWN, 0, t) // pointer1
            this[0].verifyPointer(pointer1, position1_1)

            this[1].verifyEvent(2, ACTION_POINTER_DOWN, 1, t) // pointer2
            this[1].verifyPointer(pointer1, position1_1)
            this[1].verifyPointer(pointer2, position2_1)

            t += eventPeriod
            this[2].verifyEvent(2, ACTION_MOVE, 0, t)
            this[2].verifyPointer(pointer1, position1_3)
            this[2].verifyPointer(pointer2, position2_1)

            this[3].verifyEvent(3, ACTION_POINTER_DOWN, 2, t) // pointer2
            this[3].verifyPointer(pointer1, position1_3)
            this[3].verifyPointer(pointer2, position2_1)
            this[3].verifyPointer(pointer3, position3_1)
        }
    }

    @Test
    fun upFlushesPointerMovement() {
        // Movement from [movePointer] that hasn't been sent will be sent when sending UP
        subject.sendDownAndCheck(pointer1, position1_1)
        subject.sendDownAndCheck(pointer2, position2_1)
        subject.movePointerAndCheck(pointer1, position1_2)
        subject.movePointerAndCheck(pointer1, position1_3)
        subject.sendUpAndCheck(pointer1)

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(4)

            this[0].verifyEvent(1, ACTION_DOWN, 0, t) // pointer1
            this[0].verifyPointer(pointer1, position1_1)

            this[1].verifyEvent(2, ACTION_POINTER_DOWN, 1, t) // pointer2
            this[1].verifyPointer(pointer1, position1_1)
            this[1].verifyPointer(pointer2, position2_1)

            t += eventPeriod
            this[2].verifyEvent(2, ACTION_MOVE, 0, t)
            this[2].verifyPointer(pointer1, position1_3)
            this[2].verifyPointer(pointer2, position2_1)

            this[3].verifyEvent(2, ACTION_POINTER_UP, 0, t) // pointer1
            this[3].verifyPointer(pointer1, position1_3)
            this[3].verifyPointer(pointer2, position2_1)
        }
    }

    @Test
    fun cancelDoesNotFlushPointerMovement() {
        // 2 fingers, both with pending movement.
        // CANCEL doesn't force a MOVE, but _does_ reflect the latest positions
        subject.sendDownAndCheck(pointer1, position1_1)
        subject.sendDownAndCheck(pointer2, position2_1)
        subject.movePointerAndCheck(pointer1, position1_2)
        subject.movePointerAndCheck(pointer2, position2_2)
        subject.sendCancelAndCheckPointers()

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(3)
            this[0].verifyEvent(1, ACTION_DOWN, 0, t) // pointer1
            this[0].verifyPointer(pointer1, position1_1)

            this[1].verifyEvent(2, ACTION_POINTER_DOWN, 1, t) // pointer2
            this[1].verifyPointer(pointer1, position1_1)
            this[1].verifyPointer(pointer2, position2_1)

            t += eventPeriod
            this[2].verifyEvent(2, ACTION_CANCEL, 0, t)
            this[2].verifyPointer(pointer1, position1_2)
            this[2].verifyPointer(pointer2, position2_2)
        }
    }

    @Test
    fun movePointerWithoutDown() {
        expectError<IllegalStateException> {
            subject.movePointer(pointer1, position1_1)
        }
    }

    @Test
    fun movePointerWrongPointerId() {
        subject.sendDown(pointer1, position1_1)
        expectError<IllegalArgumentException> {
            subject.movePointer(pointer2, position1_2)
        }
    }

    @Test
    fun movePointerAfterUp() {
        subject.sendDown(pointer1, position1_1)
        subject.sendUp(pointer1)
        expectError<IllegalStateException> {
            subject.movePointer(pointer1, position1_2)
        }
    }

    @Test
    fun movePointerAfterCancel() {
        subject.sendDown(pointer1, position1_1)
        subject.sendCancel()
        expectError<IllegalStateException> {
            subject.movePointer(pointer1, position1_2)
        }
    }

    @Test
    fun sendMoveWithoutDown() {
        expectError<IllegalStateException> {
            subject.sendMove()
        }
    }

    @Test
    fun sendMoveAfterUp() {
        subject.sendDown(pointer1, position1_1)
        subject.sendUp(pointer1)
        expectError<IllegalStateException> {
            subject.sendMove()
        }
    }

    @Test
    fun sendMoveAfterCancel() {
        subject.sendDown(pointer1, position1_1)
        subject.sendCancel()
        expectError<IllegalStateException> {
            subject.sendMove()
        }
    }
}
