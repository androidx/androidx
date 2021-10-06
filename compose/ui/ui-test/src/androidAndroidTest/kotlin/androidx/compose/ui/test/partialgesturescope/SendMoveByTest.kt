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

package androidx.compose.ui.test.partialgesturescope

import android.os.SystemClock.sleep
import androidx.compose.testutils.expectError
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.InputDispatcher.Companion.eventPeriodMillis
import androidx.compose.ui.test.cancel
import androidx.compose.ui.test.down
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.move
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.movePointerBy
import androidx.compose.ui.test.partialgesturescope.Common.partialGesture
import androidx.compose.ui.test.up
import androidx.compose.ui.test.util.ClickableTestBox
import androidx.compose.ui.test.util.MultiPointerInputRecorder
import androidx.compose.ui.test.util.assertTimestampsAreIncreasing
import androidx.compose.ui.test.util.verify
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test

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
    val rule = createComposeRule()

    private val recorder = MultiPointerInputRecorder()

    @Before
    fun setUp() {
        // Given some content
        rule.setContent {
            ClickableTestBox(recorder)
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun onePointer() {
        // When we inject a down event followed by a move event
        rule.partialGesture { down(downPosition1) }
        sleep(20) // (with some time in between)
        rule.partialGesture { moveBy(delta1) }

        rule.runOnIdle {
            recorder.run {
                // Then we have recorded 1 down event and 1 move event
                assertTimestampsAreIncreasing()
                assertThat(events).hasSize(2)

                var t = events[0].getPointer(0).timestamp
                val pointerId = events[0].getPointer(0).id

                t += eventPeriodMillis
                assertThat(events[1].pointerCount).isEqualTo(1)
                events[1].getPointer(0).verify(t, pointerId, true, downPosition1 + delta1)
            }
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun twoPointers() {
        // When we inject two down events followed by two move events
        rule.partialGesture { down(1, downPosition1) }
        rule.partialGesture { down(2, downPosition2) }
        rule.partialGesture { moveBy(1, delta1) }
        rule.partialGesture { moveBy(2, delta2) }

        rule.runOnIdle {
            recorder.run {
                // Then we have recorded two down events and two move events
                assertTimestampsAreIncreasing()
                assertThat(events).hasSize(4)

                var t = events[0].getPointer(0).timestamp
                val pointerId1 = events[0].getPointer(0).id
                val pointerId2 = events[1].getPointer(1).id

                t += eventPeriodMillis
                assertThat(events[2].pointerCount).isEqualTo(2)
                events[2].getPointer(0).verify(t, pointerId1, true, downPosition1 + delta1)
                events[2].getPointer(1).verify(t, pointerId2, true, downPosition2)

                t += eventPeriodMillis
                assertThat(events[3].pointerCount).isEqualTo(2)
                events[3].getPointer(0).verify(t, pointerId1, true, downPosition1 + delta1)
                events[3].getPointer(1).verify(t, pointerId2, true, downPosition2 + delta2)
            }
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun twoPointers_oneMoveEvent() {
        // When we inject two down events followed by one move events
        rule.partialGesture { down(1, downPosition1) }
        rule.partialGesture { down(2, downPosition2) }
        sleep(20) // (with some time in between)
        rule.partialGesture { movePointerBy(1, delta1) }
        rule.partialGesture { movePointerBy(2, delta2) }
        rule.partialGesture { move() }

        rule.runOnIdle {
            recorder.run {
                // Then we have recorded two down events and one move events
                assertTimestampsAreIncreasing()
                assertThat(events).hasSize(3)

                var t = events[0].getPointer(0).timestamp
                val pointerId1 = events[0].getPointer(0).id
                val pointerId2 = events[1].getPointer(1).id

                t += eventPeriodMillis
                assertThat(events[2].pointerCount).isEqualTo(2)
                events[2].getPointer(0).verify(t, pointerId1, true, downPosition1 + delta1)
                events[2].getPointer(1).verify(t, pointerId2, true, downPosition2 + delta2)
            }
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun moveByWithoutDown() {
        expectError<IllegalStateException> {
            rule.partialGesture { moveBy(delta1) }
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun moveByWrongPointerId() {
        rule.partialGesture { down(1, downPosition1) }
        expectError<IllegalArgumentException> {
            rule.partialGesture { moveBy(2, delta1) }
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun moveByAfterUp() {
        rule.partialGesture { down(downPosition1) }
        rule.partialGesture { up() }
        expectError<IllegalStateException> {
            rule.partialGesture { moveBy(delta1) }
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun moveByAfterCancel() {
        rule.partialGesture { down(downPosition1) }
        rule.partialGesture { cancel() }
        expectError<IllegalStateException> {
            rule.partialGesture { moveBy(delta1) }
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun movePointerByWithoutDown() {
        expectError<IllegalStateException> {
            rule.partialGesture { movePointerBy(1, delta1) }
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun movePointerByWrongPointerId() {
        rule.partialGesture { down(1, downPosition1) }
        expectError<IllegalArgumentException> {
            rule.partialGesture { movePointerBy(2, delta1) }
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun movePointerByAfterUp() {
        rule.partialGesture { down(1, downPosition1) }
        rule.partialGesture { up(1) }
        expectError<IllegalStateException> {
            rule.partialGesture { movePointerBy(1, delta1) }
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun movePointerByAfterCancel() {
        rule.partialGesture { down(1, downPosition1) }
        rule.partialGesture { cancel() }
        expectError<IllegalStateException> {
            rule.partialGesture { movePointerBy(1, delta1) }
        }
    }
}
