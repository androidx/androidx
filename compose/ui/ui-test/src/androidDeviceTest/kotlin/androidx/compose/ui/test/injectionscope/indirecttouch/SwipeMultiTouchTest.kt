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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.indirect.IndirectPointerEventPrimaryDirectionalMotionAxis
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Move
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Press
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Release
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerType.Companion.Touch
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.multiTouchSwipe
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performIndirectPointerInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.util.ClickableTestBox
import androidx.compose.ui.test.util.MultiPointerInputRecorder
import androidx.compose.ui.test.util.assertTimestampsAreIncreasing
import androidx.compose.ui.test.util.verify
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.IntSize
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test

@MediumTest
@OptIn(ExperimentalTestApi::class)
class SwipeMultiTouchTest {
    companion object {
        private const val TAG = "widget"
        // Duration is 4 * eventPeriod to get easily predictable results
        private const val DURATION = 64L
        private val inputDeviceSize = IntSize(3082, 616)
    }

    private val recorder = MultiPointerInputRecorder()

    @Test
    fun test() = runComposeUiTest {
        setContent {
            Box(Modifier.fillMaxSize()) { ClickableTestBox(modifier = recorder, tag = TAG) }
        }
        onNodeWithTag(TAG).requestFocus()

        // Move three fingers over the box from left to right simultaneously
        // With a duration that is exactly 4 times the eventPeriod, each pointer will be sampled
        // at t = 0, 16, 32, 48 and 64. That corresponds to x values of 10, 30, 50, 70 and 90.

        val curve1 = line(fromX = 10f, toX = 90f, y = 20f, DURATION)
        val curve2 = line(fromX = 10f, toX = 90f, y = 50f, DURATION)
        val curve3 = line(fromX = 10f, toX = 90f, y = 80f, DURATION)

        onNodeWithTag(TAG).performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            multiTouchSwipe(curves = listOf(curve1, curve2, curve3), durationMillis = DURATION)
        }

        val pointer1 = PointerId(0)
        val pointer2 = PointerId(1)
        val pointer3 = PointerId(2)

        runOnIdle {
            recorder.run {
                assertTimestampsAreIncreasing()
                assertThat(events).hasSize(10)

                val t0 = events[0].getPointer(0).timestamp

                // Event 0: pointer 1 down
                assertThat(events[0].pointerCount).isEqualTo(1)
                events[0]
                    .getPointer(0)
                    .verify(t0 + 0L, pointer1, true, Offset(10f, 20f), Touch, Press)

                // Event 1: pointer 1 down, pointer 2 down
                assertThat(events[1].pointerCount).isEqualTo(2)
                events[1]
                    .getPointer(0)
                    .verify(t0 + 0L, pointer1, true, Offset(10f, 20f), Touch, Press)
                events[1]
                    .getPointer(1)
                    .verify(t0 + 0L, pointer2, true, Offset(10f, 50f), Touch, Press)

                // Event 2: pointer 1 down, pointer 2 down, pointer 3 down
                assertThat(events[2].pointerCount).isEqualTo(3)
                events[2]
                    .getPointer(0)
                    .verify(t0 + 0L, pointer1, true, Offset(10f, 20f), Touch, Press)
                events[2]
                    .getPointer(1)
                    .verify(t0 + 0L, pointer2, true, Offset(10f, 50f), Touch, Press)
                events[2]
                    .getPointer(2)
                    .verify(t0 + 0L, pointer3, true, Offset(10f, 80f), Touch, Press)

                // Event 3: first move
                assertThat(events[3].pointerCount).isEqualTo(3)
                events[3]
                    .getPointer(0)
                    .verify(t0 + 16L, pointer1, true, Offset(30f, 20f), Touch, Move)
                events[3]
                    .getPointer(1)
                    .verify(t0 + 16L, pointer2, true, Offset(30f, 50f), Touch, Move)
                events[3]
                    .getPointer(2)
                    .verify(t0 + 16L, pointer3, true, Offset(30f, 80f), Touch, Move)

                // Event 4: second move
                assertThat(events[4].pointerCount).isEqualTo(3)
                events[4]
                    .getPointer(0)
                    .verify(t0 + 32L, pointer1, true, Offset(50f, 20f), Touch, Move)
                events[4]
                    .getPointer(1)
                    .verify(t0 + 32L, pointer2, true, Offset(50f, 50f), Touch, Move)
                events[4]
                    .getPointer(2)
                    .verify(t0 + 32L, pointer3, true, Offset(50f, 80f), Touch, Move)

                // Event 5: third move
                assertThat(events[5].pointerCount).isEqualTo(3)
                events[5]
                    .getPointer(0)
                    .verify(t0 + 48L, pointer1, true, Offset(70f, 20f), Touch, Move)
                events[5]
                    .getPointer(1)
                    .verify(t0 + 48L, pointer2, true, Offset(70f, 50f), Touch, Move)
                events[5]
                    .getPointer(2)
                    .verify(t0 + 48L, pointer3, true, Offset(70f, 80f), Touch, Move)

                // Event 6: last move
                assertThat(events[6].pointerCount).isEqualTo(3)
                events[6]
                    .getPointer(0)
                    .verify(t0 + 64L, pointer1, true, Offset(90f, 20f), Touch, Move)
                events[6]
                    .getPointer(1)
                    .verify(t0 + 64L, pointer2, true, Offset(90f, 50f), Touch, Move)
                events[6]
                    .getPointer(2)
                    .verify(t0 + 64L, pointer3, true, Offset(90f, 80f), Touch, Move)

                // Event 7: pointer 1 up, pointer 2 down, pointer 3 down
                assertThat(events[7].pointerCount).isEqualTo(3)
                events[7]
                    .getPointer(0)
                    .verify(t0 + 64L, pointer1, false, Offset(90f, 20f), Touch, Release)
                events[7]
                    .getPointer(1)
                    .verify(t0 + 64L, pointer2, true, Offset(90f, 50f), Touch, Release)
                events[7]
                    .getPointer(2)
                    .verify(t0 + 64L, pointer3, true, Offset(90f, 80f), Touch, Release)

                // Event 8: pointer 2 up, pointer 3 down
                assertThat(events[8].pointerCount).isEqualTo(2)
                events[8]
                    .getPointer(0)
                    .verify(t0 + 64L, pointer2, false, Offset(90f, 50f), Touch, Release)
                events[8]
                    .getPointer(1)
                    .verify(t0 + 64L, pointer3, true, Offset(90f, 80f), Touch, Release)

                // Event 9: pointer 3 up
                assertThat(events[9].pointerCount).isEqualTo(1)
                events[9]
                    .getPointer(0)
                    .verify(t0 + 64L, pointer3, false, Offset(90f, 80f), Touch, Release)
            }
        }
    }

    @Suppress("SameParameterValue")
    private fun line(fromX: Float, toX: Float, y: Float, durationMillis: Long): (Long) -> Offset {
        return { Offset(fromX + (toX - fromX) * it / durationMillis, y) }
    }
}
