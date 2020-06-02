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
 * Tests if [AndroidInputDispatcher.sendDown] works
 */
@SmallTest
class SendDownTest {
    companion object {
        // Pointer ids
        private const val pointer1 = 11
        private const val pointer2 = 22
        private const val pointer3 = 33
        private const val pointer4 = 44

        // Positions (mostly used with corresponding pointerId: pointerX with positionX)
        private val position1 = PxPosition(1f, 1f)
        private val position2 = PxPosition(2f, 2f)
        private val position3 = PxPosition(3f, 3f)
        private val position4 = PxPosition(4f, 4f)

        // Single alternative for pointer1
        private val position1_2 = PxPosition(12f, 12f)
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

    @Test
    fun onePointer() {
        subject.sendDownAndCheck(pointer1, position1)

        val t = 0L
        recorder.assertHasValidEventTimes()
        assertThat(recorder.events).hasSize(1)
        recorder.events[0].verifyEvent(1, ACTION_DOWN, 0, t) // pointer1
        recorder.events[0].verifyPointer(pointer1, position1)
    }

    @Test
    fun twoPointers_ascending() {
        // 2 fingers, sent in ascending order of pointerId (matters for actionIndex)
        subject.sendDownAndCheck(pointer1, position1)
        subject.sendDownAndCheck(pointer2, position2)

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            val t = 0L
            assertThat(this).hasSize(2)

            this[0].verifyEvent(1, ACTION_DOWN, 0, t) // pointer1
            this[0].verifyPointer(pointer1, position1)

            this[1].verifyEvent(2, ACTION_POINTER_DOWN, 1, t) // pointer2
            this[1].verifyPointer(pointer1, position1)
            this[1].verifyPointer(pointer2, position2)
        }
    }

    @Test
    fun twoPointers_descending() {
        // 2 fingers, sent in descending order of pointerId (matters for actionIndex)
        subject.sendDownAndCheck(pointer2, position2)
        subject.sendDownAndCheck(pointer1, position1)

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            val t = 0L
            assertThat(this).hasSize(2)

            this[0].verifyEvent(1, ACTION_DOWN, 0, t) // pointer2
            this[0].verifyPointer(pointer2, position2)

            this[1].verifyEvent(2, ACTION_POINTER_DOWN, 0, t) // pointer1
            this[1].verifyPointer(pointer1, position1)
            this[1].verifyPointer(pointer2, position2)
        }
    }

    @Test
    fun fourPointers() {
        // 4 fingers, sent in non-trivial order of pointerId (matters for actionIndex)

        subject.sendDownAndCheck(pointer3, position3)
        subject.sendDownAndCheck(pointer1, position1)
        subject.sendDownAndCheck(pointer4, position4)
        subject.sendDownAndCheck(pointer2, position2)

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            val t = 0L
            assertThat(this).hasSize(4)

            this[0].verifyEvent(1, ACTION_DOWN, 0, t) // pointer3
            this[0].verifyPointer(pointer3, position3)

            this[1].verifyEvent(2, ACTION_POINTER_DOWN, 0, t) // pointer1
            this[1].verifyPointer(pointer1, position1)
            this[1].verifyPointer(pointer3, position3)

            this[2].verifyEvent(3, ACTION_POINTER_DOWN, 2, t) // pointer4
            this[2].verifyPointer(pointer1, position1)
            this[2].verifyPointer(pointer3, position3)
            this[2].verifyPointer(pointer4, position4)

            this[3].verifyEvent(4, ACTION_POINTER_DOWN, 1, t) // pointer2
            this[3].verifyPointer(pointer1, position1)
            this[3].verifyPointer(pointer2, position2)
            this[3].verifyPointer(pointer3, position3)
            this[3].verifyPointer(pointer4, position4)
        }
    }

    @Test
    fun staggeredDown() {
        // 4 fingers, going down at different times
        // Each [sendMove] increases the time by 10 milliseconds

        subject.sendDownAndCheck(pointer3, position3)
        subject.sendMove()
        subject.sendDownAndCheck(pointer1, position1)
        subject.sendDownAndCheck(pointer2, position2)
        subject.sendMove()
        subject.sendMove()
        subject.sendMove()
        subject.sendDownAndCheck(pointer4, position4)

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(8)

            this[0].verifyEvent(1, ACTION_DOWN, 0, t) // pointer3
            this[0].verifyPointer(pointer3, position3)

            t += eventPeriod
            this[1].verifyEvent(1, ACTION_MOVE, 0, t)
            this[1].verifyPointer(pointer3, position3)

            this[2].verifyEvent(2, ACTION_POINTER_DOWN, 0, t) // pointer1
            this[2].verifyPointer(pointer1, position1)
            this[2].verifyPointer(pointer3, position3)

            this[3].verifyEvent(3, ACTION_POINTER_DOWN, 1, t) // pointer2
            this[3].verifyPointer(pointer1, position1)
            this[3].verifyPointer(pointer2, position2)
            this[3].verifyPointer(pointer3, position3)

            for (i in 4..6) {
                t += eventPeriod
                this[i].verifyEvent(3, ACTION_MOVE, 0, t)
                this[i].verifyPointer(pointer1, position1)
                this[i].verifyPointer(pointer2, position2)
                this[i].verifyPointer(pointer3, position3)
            }

            this[7].verifyEvent(4, ACTION_POINTER_DOWN, 3, t) // pointer4
            this[7].verifyPointer(pointer1, position1)
            this[7].verifyPointer(pointer2, position2)
            this[7].verifyPointer(pointer3, position3)
            this[7].verifyPointer(pointer4, position4)
        }
    }

    @Test
    fun nonOverlappingPointers() {
        // 3 fingers, where the 1st finger goes up before the 3rd finger goes down (no overlap)
        // Each [sendMove] increases the time by 10 milliseconds

        subject.sendDownAndCheck(pointer1, position1)
        subject.sendDownAndCheck(pointer2, position2)
        subject.sendMove()
        subject.sendUpAndCheck(pointer1)
        subject.sendMove()
        subject.sendDownAndCheck(pointer3, position3)

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(6)

            this[0].verifyEvent(1, ACTION_DOWN, 0, t) // pointer1
            this[0].verifyPointer(pointer1, position1)

            this[1].verifyEvent(2, ACTION_POINTER_DOWN, 1, t) // pointer2
            this[1].verifyPointer(pointer1, position1)
            this[1].verifyPointer(pointer2, position2)

            t += eventPeriod
            this[2].verifyEvent(2, ACTION_MOVE, 0, t)
            this[2].verifyPointer(pointer1, position1)
            this[2].verifyPointer(pointer2, position2)

            this[3].verifyEvent(2, ACTION_POINTER_UP, 0, t) // pointer1
            this[3].verifyPointer(pointer1, position1)
            this[3].verifyPointer(pointer2, position2)

            t += eventPeriod
            this[4].verifyEvent(1, ACTION_MOVE, 0, t)
            this[4].verifyPointer(pointer2, position2)

            this[5].verifyEvent(2, ACTION_POINTER_DOWN, 1, t) // pointer3
            this[5].verifyPointer(pointer2, position2)
            this[5].verifyPointer(pointer3, position3)
        }
    }

    @Test
    fun pointerIdReuse() {
        // 3 fingers, where the 1st finger goes up before the 3rd finger goes down, and the 3rd
        // fingers reuses the pointerId of finger 1
        // Each [sendMove] increases the time by 10 milliseconds

        subject.sendDownAndCheck(pointer1, position1)
        subject.sendDownAndCheck(pointer2, position2)
        subject.sendMove()
        subject.sendUpAndCheck(pointer1)
        subject.sendMove()
        subject.sendDownAndCheck(pointer1, position1_2)

        recorder.assertHasValidEventTimes()
        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(6)

            this[0].verifyEvent(1, ACTION_DOWN, 0, t) // pointer1
            this[0].verifyPointer(pointer1, position1)

            this[1].verifyEvent(2, ACTION_POINTER_DOWN, 1, t) // pointer2
            this[1].verifyPointer(pointer1, position1)
            this[1].verifyPointer(pointer2, position2)

            t += eventPeriod
            this[2].verifyEvent(2, ACTION_MOVE, 0, t)
            this[2].verifyPointer(pointer1, position1)
            this[2].verifyPointer(pointer2, position2)

            this[3].verifyEvent(2, ACTION_POINTER_UP, 0, t) // pointer1
            this[3].verifyPointer(pointer1, position1)
            this[3].verifyPointer(pointer2, position2)

            t += eventPeriod
            this[4].verifyEvent(1, ACTION_MOVE, 0, t)
            this[4].verifyPointer(pointer2, position2)

            this[5].verifyEvent(2, ACTION_POINTER_DOWN, 0, t) // pointer1
            this[5].verifyPointer(pointer1, position1_2)
            this[5].verifyPointer(pointer2, position2)
        }
    }

    @Test
    fun downAfterDown() {
        subject.sendDown(pointer1, position1)
        expectError<IllegalArgumentException> {
            subject.sendDown(pointer1, position2)
        }
    }
}
