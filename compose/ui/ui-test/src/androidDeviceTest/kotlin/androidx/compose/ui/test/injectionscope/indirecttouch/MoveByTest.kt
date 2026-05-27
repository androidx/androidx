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

package androidx.compose.ui.test.injectionscope.indirecttouch

import android.os.SystemClock.sleep
import androidx.compose.testutils.expectError
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.indirect.IndirectPointerEventPrimaryDirectionalMotionAxis
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Move
import androidx.compose.ui.input.pointer.PointerType.Companion.Touch
import androidx.compose.ui.test.IndirectPointerInjectionScope
import androidx.compose.ui.test.InputDispatcher.Companion.eventPeriodMillis
import androidx.compose.ui.test.injectionscope.indirecttouch.Common.performIndirectPointerInput
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.util.ClickableTestBox
import androidx.compose.ui.test.util.MultiPointerInputRecorder
import androidx.compose.ui.test.util.assertTimestampsAreIncreasing
import androidx.compose.ui.test.util.verify
import androidx.compose.ui.unit.IntSize
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests if [IndirectPointerInjectionScope.moveBy] and
 * [IndirectPointerInjectionScope.updatePointerBy] work
 */
@MediumTest
class MoveByTest {
    companion object {
        private val downPosition1 = Offset(10f, 10f)
        private val downPosition2 = Offset(20f, 20f)
        private val delta1 = Offset(11f, 11f)
        private val delta2 = Offset(21f, 21f)

        // Horizontal external indirect pointer input device
        private val inputDeviceSize = IntSize(3082, 616)
    }

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    private val recorder = MultiPointerInputRecorder()

    @Before
    fun setUp() {
        // Given some content
        rule.setContent { ClickableTestBox(recorder) }
        rule.onNodeWithTag(ClickableTestBox.defaultTag).requestFocus()
    }

    @Test
    fun onePointerSameInputBlock() {
        // When we inject a down event followed by a move event
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            down(downPosition1)
            // Sleep done within input block
            sleep(20)
            moveBy(delta1)
        }

        rule.runOnIdle {
            recorder.run {
                // Then we have recorded 1 down event and 1 move event
                assertTimestampsAreIncreasing()

                assertThat(events).hasSize(2)

                var t = events[0].getPointer(0).timestamp
                val pointerId = events[0].getPointer(0).id

                t += eventPeriodMillis
                assertThat(events[1].pointerCount).isEqualTo(1)
                events[1]
                    .getPointer(0)
                    .verify(t, pointerId, true, downPosition1 + delta1, Touch, Move)
            }
        }
    }

    @Test
    fun onePointerDifferentInputBlocks() {
        // When we inject a down event followed by a move event
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            down(downPosition1)
        }

        sleep(20) // (with some time in between)

        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            advanceEventTime(0L)
            moveBy(delta1)
        }

        rule.runOnIdle {
            recorder.run {
                // Then we have recorded 1 down event and 1 move event
                assertTimestampsAreIncreasing()
                assertThat(events).hasSize(2)

                var t = events[0].getPointer(0).timestamp
                val pointerId = events[0].getPointer(0).id

                t += eventPeriodMillis
                assertThat(events[1].pointerCount).isEqualTo(1)
                events[1]
                    .getPointer(0)
                    .verify(t, pointerId, true, downPosition1 + delta1, Touch, Move)
            }
        }
    }

    @Test
    fun twoPointers() {
        // When we inject two down events followed by two move events
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            down(1, downPosition1)
        }
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            down(2, downPosition2)
        }
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            moveBy(1, delta1)
        }
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            moveBy(2, delta2)
        }

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
                events[2]
                    .getPointer(0)
                    .verify(t, pointerId1, true, downPosition1 + delta1, Touch, Move)
                events[2].getPointer(1).verify(t, pointerId2, true, downPosition2, Touch, Move)

                t += eventPeriodMillis
                assertThat(events[3].pointerCount).isEqualTo(2)
                events[3]
                    .getPointer(0)
                    .verify(t, pointerId1, true, downPosition1 + delta1, Touch, Move)
                events[3]
                    .getPointer(1)
                    .verify(t, pointerId2, true, downPosition2 + delta2, Touch, Move)
            }
        }
    }

    @Test
    fun onePointer_oneMoveEvent() {
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            down(downPosition1)
        }
        sleep(20) // (with some time in between)
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            updatePointerBy(delta1)
        }
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            move()
        }

        rule.runOnIdle {
            recorder.run {
                // Then we have recorded two down events and one move events
                assertTimestampsAreIncreasing()
                assertThat(events).hasSize(2)

                var t = events[0].getPointer(0).timestamp
                val pointerId1 = events[0].getPointer(0).id

                assertThat(pointerId1.value).isEqualTo(0)

                t += eventPeriodMillis
                assertThat(events[1].pointerCount).isEqualTo(1)
                events[1]
                    .getPointer(0)
                    .verify(t, pointerId1, true, downPosition1 + delta1, Touch, Move)
            }
        }
    }

    @Test
    fun twoPointers_oneMoveEvent() {
        // When we inject two down events followed by one move events
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            down(1, downPosition1)
        }
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            down(2, downPosition2)
        }
        sleep(20) // (with some time in between)
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            updatePointerBy(1, delta1)
        }
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            updatePointerBy(2, delta2)
        }
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            move()
        }

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
                events[2]
                    .getPointer(0)
                    .verify(t, pointerId1, true, downPosition1 + delta1, Touch, Move)
                events[2]
                    .getPointer(1)
                    .verify(t, pointerId2, true, downPosition2 + delta2, Touch, Move)
            }
        }
    }

    @Test
    fun moveBy_withoutDown() {
        expectError<IllegalStateException> {
            rule.performIndirectPointerInput(
                IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                inputDeviceSize,
            ) {
                moveBy(delta1)
            }
        }
    }

    @Test
    fun moveBy_wrongPointerId() {
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            down(1, downPosition1)
        }
        expectError<IllegalArgumentException> {
            rule.performIndirectPointerInput(
                IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                inputDeviceSize,
            ) {
                moveBy(2, delta1)
            }
        }
    }

    @Test
    fun moveBy_afterUp() {
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            down(downPosition1)
        }
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            up()
        }
        expectError<IllegalStateException> {
            rule.performIndirectPointerInput(
                IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                inputDeviceSize,
            ) {
                moveBy(delta1)
            }
        }
    }

    @Test
    fun moveBy_afterCancel() {
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            down(downPosition1)
        }
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            cancel()
        }
        expectError<IllegalStateException> {
            rule.performIndirectPointerInput(
                IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                inputDeviceSize,
            ) {
                moveBy(delta1)
            }
        }
    }

    @Test
    fun updatePointerBy_withoutDown() {
        expectError<IllegalStateException> {
            rule.performIndirectPointerInput(
                IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                inputDeviceSize,
            ) {
                updatePointerBy(1, delta1)
            }
        }
    }

    @Test
    fun updatePointerBy_wrongPointerId() {
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            down(1, downPosition1)
        }
        expectError<IllegalArgumentException> {
            rule.performIndirectPointerInput(
                IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                inputDeviceSize,
            ) {
                updatePointerBy(2, delta1)
            }
        }
    }

    @Test
    fun updatePointerBy_afterUp() {
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            down(1, downPosition1)
        }
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            up(1)
        }
        expectError<IllegalStateException> {
            rule.performIndirectPointerInput(
                IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                inputDeviceSize,
            ) {
                updatePointerBy(1, delta1)
            }
        }
    }

    @Test
    fun updatePointerBy_afterCancel() {
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            down(1, downPosition1)
        }
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            cancel()
        }
        expectError<IllegalStateException> {
            rule.performIndirectPointerInput(
                IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                inputDeviceSize,
            ) {
                updatePointerBy(1, delta1)
            }
        }
    }
}
