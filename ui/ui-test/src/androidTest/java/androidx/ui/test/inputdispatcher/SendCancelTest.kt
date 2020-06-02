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
import android.view.MotionEvent.ACTION_POINTER_DOWN
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
 * Tests if [AndroidInputDispatcher.sendCancel] works
 */
@SmallTest
class SendCancelTest {
    companion object {
        // pointerIds
        private const val pointer1 = 11
        private const val pointer2 = 22

        // positions (used with corresponding pointerId: pointerX with positionX_Y)
        private val position1_1 = PxPosition(11f, 11f)
        private val position2_1 = PxPosition(21f, 21f)
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
    }

    @Test
    fun onePointer() {
        subject.sendDownAndCheck(pointer1, position1_1)
        subject.sendCancelAndCheckPointers()
        subject.verifyNoGestureInProgress()
        recorder.assertHasValidEventTimes()

        recorder.events.apply {
            var t = 0L
            assertThat(this).hasSize(2)
            this[0].verifyEvent(1, ACTION_DOWN, 0, t) // pointer1
            this[0].verifyPointer(pointer1, position1_1)

            t += eventPeriod
            this[1].verifyEvent(1, ACTION_CANCEL, 0, t)
            this[1].verifyPointer(pointer1, position1_1)
        }
    }

    @Test
    fun multiplePointers() {
        subject.sendDownAndCheck(pointer1, position1_1)
        subject.sendDownAndCheck(pointer2, position2_1)
        subject.sendCancelAndCheckPointers()
        subject.verifyNoGestureInProgress()
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
            this[2].verifyPointer(pointer1, position1_1)
            this[2].verifyPointer(pointer2, position2_1)
        }
    }

    @Test
    fun cancelWithoutDown() {
        expectError<IllegalStateException> {
            subject.sendCancel()
        }
    }

    @Test
    fun cancelAfterUp() {
        subject.sendDown(pointer1, position1_1)
        subject.sendUp(pointer1)
        expectError<IllegalStateException> {
            subject.sendCancel()
        }
    }

    @Test
    fun cancelAfterCancel() {
        subject.sendDown(pointer1, position1_1)
        subject.sendCancel()
        expectError<IllegalStateException> {
            subject.sendCancel()
        }
    }
}
