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
import androidx.compose.ui.geometry.Offset
import androidx.ui.test.AndroidBaseInputDispatcher.InputDispatcherTestRule
import androidx.ui.test.createComposeRule
import androidx.ui.test.inputdispatcher.verifyNoGestureInProgress
import androidx.ui.test.partialgesturescope.Common.partialGesture
import androidx.ui.test.runOnIdle
import androidx.ui.test.cancel
import androidx.ui.test.down
import androidx.ui.test.up
import androidx.ui.test.util.ClickableTestBox
import androidx.ui.test.util.MultiPointerInputRecorder
import androidx.ui.test.util.assertTimestampsAreIncreasing
import androidx.ui.test.util.expectError
import androidx.ui.test.util.verify
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

/**
 * Tests if [up] works
 */
@MediumTest
class SendUpTest {
    companion object {
        private val downPosition1 = Offset(10f, 10f)
        private val downPosition2 = Offset(20f, 20f)
    }

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val inputDispatcherRule: TestRule = InputDispatcherTestRule(disableDispatchInRealTime = true)

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
        partialGesture { down(downPosition1) }
        sleep(20) // (with some time in between)
        partialGesture { up() }

        runOnIdle {
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
        partialGesture { down(1, downPosition1) }
        partialGesture { down(2, downPosition2) }
        partialGesture { up(1) }
        partialGesture { up(2) }

        runOnIdle {
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
            partialGesture { up() }
        }
    }

    @Test
    fun upWrongPointerId() {
        partialGesture { down(1, downPosition1) }
        expectError<IllegalArgumentException> {
            partialGesture { up(2) }
        }
    }

    @Test
    fun upAfterUp() {
        partialGesture { down(downPosition1) }
        partialGesture { up() }
        expectError<IllegalStateException> {
            partialGesture { up() }
        }
    }

    @Test
    fun upAfterCancel() {
        partialGesture { down(downPosition1) }
        partialGesture { cancel() }
        expectError<IllegalStateException> {
            partialGesture { up() }
        }
    }
}
