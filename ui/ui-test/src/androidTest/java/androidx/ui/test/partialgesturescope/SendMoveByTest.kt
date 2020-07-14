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
import androidx.ui.geometry.Offset
import androidx.ui.test.InputDispatcher.InputDispatcherTestRule
import androidx.ui.test.createComposeRule
import androidx.ui.test.movePointerBy
import androidx.ui.test.partialgesturescope.Common.partialGesture
import androidx.ui.test.runOnIdle
import androidx.ui.test.cancel
import androidx.ui.test.down
import androidx.ui.test.move
import androidx.ui.test.moveBy
import androidx.ui.test.up
import androidx.ui.test.util.ClickableTestBox
import androidx.ui.test.util.MultiPointerInputRecorder
import androidx.ui.test.util.assertTimestampsAreIncreasing
import androidx.ui.test.util.expectError
import androidx.ui.test.util.verify
import androidx.ui.unit.milliseconds
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

/**
 * Tests if [moveBy] and [movePointerBy] work
 */
@MediumTest
class SendMoveByTest {
    companion object {
        private val downPosition1 = Offset(10f, 10f)
        private val downPosition2 = Offset(20f, 20f)
        private val delta1 = Offset(11f, 11f)
        private val delta2 = Offset(21f, 21f)
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
        // When we inject a down event followed by a move event
        partialGesture { down(downPosition1) }
        sleep(20) // (with some time in between)
        partialGesture { moveBy(delta1) }

        runOnIdle {
            recorder.run {
                // Then we have recorded 1 down event and 1 move event
                assertTimestampsAreIncreasing()
                assertThat(events).hasSize(2)

                var t = events[0].getPointer(0).timestamp
                val pointerId = events[0].getPointer(0).id

                t += 10.milliseconds
                assertThat(events[1].pointerCount).isEqualTo(1)
                events[1].getPointer(0).verify(t, pointerId, true, downPosition1 + delta1)
            }
        }
    }

    @Test
    fun twoPointers() {
        // When we inject two down events followed by two move events
        partialGesture { down(1, downPosition1) }
        partialGesture { down(2, downPosition2) }
        partialGesture { moveBy(1, delta1) }
        partialGesture { moveBy(2, delta2) }

        runOnIdle {
            recorder.run {
                // Then we have recorded two down events and two move events
                assertTimestampsAreIncreasing()
                assertThat(events).hasSize(4)

                var t = events[0].getPointer(0).timestamp
                val pointerId1 = events[0].getPointer(0).id
                val pointerId2 = events[1].getPointer(1).id

                t += 10.milliseconds
                assertThat(events[2].pointerCount).isEqualTo(2)
                events[2].getPointer(0).verify(t, pointerId1, true, downPosition1 + delta1)
                events[2].getPointer(1).verify(t, pointerId2, true, downPosition2)

                t += 10.milliseconds
                assertThat(events[3].pointerCount).isEqualTo(2)
                events[3].getPointer(0).verify(t, pointerId1, true, downPosition1 + delta1)
                events[3].getPointer(1).verify(t, pointerId2, true, downPosition2 + delta2)
            }
        }
    }

    @Test
    fun twoPointers_oneMoveEvent() {
        // When we inject two down events followed by one move events
        partialGesture { down(1, downPosition1) }
        partialGesture { down(2, downPosition2) }
        sleep(20) // (with some time in between)
        partialGesture { movePointerBy(1, delta1) }
        partialGesture { movePointerBy(2, delta2) }
        partialGesture { move() }

        runOnIdle {
            recorder.run {
                // Then we have recorded two down events and one move events
                assertTimestampsAreIncreasing()
                assertThat(events).hasSize(3)

                var t = events[0].getPointer(0).timestamp
                val pointerId1 = events[0].getPointer(0).id
                val pointerId2 = events[1].getPointer(1).id

                t += 10.milliseconds
                assertThat(events[2].pointerCount).isEqualTo(2)
                events[2].getPointer(0).verify(t, pointerId1, true, downPosition1 + delta1)
                events[2].getPointer(1).verify(t, pointerId2, true, downPosition2 + delta2)
            }
        }
    }

    @Test
    fun moveByWithoutDown() {
        expectError<IllegalStateException> {
            partialGesture { moveBy(delta1) }
        }
    }

    @Test
    fun moveByWrongPointerId() {
        partialGesture { down(1, downPosition1) }
        expectError<IllegalArgumentException> {
            partialGesture { moveBy(2, delta1) }
        }
    }

    @Test
    fun moveByAfterUp() {
        partialGesture { down(downPosition1) }
        partialGesture { up() }
        expectError<IllegalStateException> {
            partialGesture { moveBy(delta1) }
        }
    }

    @Test
    fun moveByAfterCancel() {
        partialGesture { down(downPosition1) }
        partialGesture { cancel() }
        expectError<IllegalStateException> {
            partialGesture { moveBy(delta1) }
        }
    }

    @Test
    fun movePointerByWithoutDown() {
        expectError<IllegalStateException> {
            partialGesture { movePointerBy(1, delta1) }
        }
    }

    @Test
    fun movePointerByWrongPointerId() {
        partialGesture { down(1, downPosition1) }
        expectError<IllegalArgumentException> {
            partialGesture { movePointerBy(2, delta1) }
        }
    }

    @Test
    fun movePointerByAfterUp() {
        partialGesture { down(1, downPosition1) }
        partialGesture { up(1) }
        expectError<IllegalStateException> {
            partialGesture { movePointerBy(1, delta1) }
        }
    }

    @Test
    fun movePointerByAfterCancel() {
        partialGesture { down(1, downPosition1) }
        partialGesture { cancel() }
        expectError<IllegalStateException> {
            partialGesture { movePointerBy(1, delta1) }
        }
    }
}
