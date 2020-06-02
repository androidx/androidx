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

package androidx.ui.test.partialgesturescope

import android.os.SystemClock.sleep
import androidx.test.filters.MediumTest
import androidx.ui.test.android.AndroidInputDispatcher
import androidx.ui.test.createComposeRule
import androidx.ui.test.inputdispatcher.verifyNoGestureInProgress
import androidx.ui.test.partialgesturescope.Common.partialGesture
import androidx.ui.test.runOnIdleCompose
import androidx.ui.test.sendCancel
import androidx.ui.test.sendDown
import androidx.ui.test.sendUp
import androidx.ui.test.util.ClickableTestBox
import androidx.ui.test.util.MultiPointerInputRecorder
import androidx.ui.test.util.assertTimestampsAreIncreasing
import androidx.ui.test.util.expectError
import androidx.ui.test.util.verify
import androidx.ui.unit.PxPosition
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

/**
 * Tests if [sendUp] works
 */
@MediumTest
class SendUpTest {
    companion object {
        private val downPosition1 = PxPosition(10f, 10f)
        private val downPosition2 = PxPosition(20f, 20f)
    }

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val inputDispatcherRule: TestRule =
        AndroidInputDispatcher.TestRule(disableDispatchInRealTime = true)

    private val recorder = MultiPointerInputRecorder()

    @Before
    fun setUp() {
        // Given some content
        composeTestRule.setContent {
            ClickableTestBox(recorder)
        }
    }

    @Test
    fun onePointer() {
        // When we inject a down event followed by an up event
        partialGesture { sendDown(downPosition1) }
        sleep(20) // (with some time in between)
        partialGesture { sendUp() }

        runOnIdleCompose {
            recorder.run {
                // Then we have recorded 1 down event and 1 up event
                assertTimestampsAreIncreasing()
                assertThat(events).hasSize(2)

                val t = events[0].getPointer(0).timestamp
                val pointerId = events[0].getPointer(0).id

                assertThat(events[1].pointerCount).isEqualTo(1)
                events[1].getPointer(0).verify(t, pointerId, false, downPosition1)
            }
        }

        // And no gesture is in progress
        partialGesture { inputDispatcher.verifyNoGestureInProgress() }
    }

    @Test
    fun twoPointers() {
        // When we inject two down events followed by two up events
        partialGesture { sendDown(1, downPosition1) }
        partialGesture { sendDown(2, downPosition2) }
        partialGesture { sendUp(1) }
        partialGesture { sendUp(2) }

        runOnIdleCompose {
            recorder.run {
                // Then we have recorded two down events and two up events
                assertTimestampsAreIncreasing()
                assertThat(events).hasSize(4)

                val t = events[0].getPointer(0).timestamp
                val pointerId1 = events[0].getPointer(0).id
                val pointerId2 = events[1].getPointer(1).id

                assertThat(events[2].pointerCount).isEqualTo(2)
                events[2].getPointer(0).verify(t, pointerId1, false, downPosition1)
                events[2].getPointer(1).verify(t, pointerId2, true, downPosition2)

                assertThat(events[3].pointerCount).isEqualTo(1)
                events[3].getPointer(0).verify(t, pointerId2, false, downPosition2)
            }
        }

        // And no gesture is in progress
        partialGesture { inputDispatcher.verifyNoGestureInProgress() }
    }

    @Test
    fun upWithoutDown() {
        expectError<IllegalStateException> {
            partialGesture { sendUp() }
        }
    }

    @Test
    fun upWrongPointerId() {
        partialGesture { sendDown(1, downPosition1) }
        expectError<IllegalArgumentException> {
            partialGesture { sendUp(2) }
        }
    }

    @Test
    fun upAfterUp() {
        partialGesture { sendDown(downPosition1) }
        partialGesture { sendUp() }
        expectError<IllegalStateException> {
            partialGesture { sendUp() }
        }
    }

    @Test
    fun upAfterCancel() {
        partialGesture { sendDown(downPosition1) }
        partialGesture { sendCancel() }
        expectError<IllegalStateException> {
            partialGesture { sendUp() }
        }
    }
}
