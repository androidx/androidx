/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.test.injectionscope.touch

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Move
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Press
import androidx.compose.ui.input.pointer.PointerEventType.Companion.Release
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerType.Companion.Touch
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.multiTouchSwipe
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.util.ClickableTestBox
import androidx.compose.ui.test.util.SinglePointerInputRecorder
import androidx.compose.ui.test.util.verify
import androidx.compose.ui.test.util.verifyEvents
import androidx.test.filters.MediumTest
import org.junit.Test

@MediumTest
@OptIn(ExperimentalTestApi::class)
class SwipeMultiTouchTest {
    companion object {
        private const val TAG = "widget"
        // Duration is 4 * eventPeriod to get easily predictable results
        private const val DURATION = 64L
    }

    private val recorder = SinglePointerInputRecorder()

    @Test
    fun test() = runComposeUiTest {
        setContent {
            Box(Modifier.fillMaxSize()) { ClickableTestBox(modifier = recorder, tag = TAG) }
        }

        // Move three fingers over the box from left to right simultaneously
        // With a duration that is exactly 4 times the eventPeriod, each pointer will be sampled
        // at t = 0, 16, 32, 48 and 64. That corresponds to x values of 10, 30, 50, 70 and 90.

        val curve1 = line(fromX = 10f, toX = 90f, y = 20f, DURATION)
        val curve2 = line(fromX = 10f, toX = 90f, y = 50f, DURATION)
        val curve3 = line(fromX = 10f, toX = 90f, y = 80f, DURATION)

        onNodeWithTag(TAG).performTouchInput {
            multiTouchSwipe(curves = listOf(curve1, curve2, curve3), durationMillis = DURATION)
        }

        val pointer1 = PointerId(0)
        val pointer2 = PointerId(1)
        val pointer3 = PointerId(2)

        runOnIdle {
            recorder.apply {
                verifyEvents(
                    // pointer1 down
                    { verify(0L, pointer1, true, Offset(10f, 20f), Touch, Press) },
                    // pointer2 down
                    { verify(0L, pointer1, true, Offset(10f, 20f), Touch, Press) },
                    { verify(0L, pointer2, true, Offset(10f, 50f), Touch, Press) },
                    // pointer3 down
                    { verify(0L, pointer1, true, Offset(10f, 20f), Touch, Press) },
                    { verify(0L, pointer2, true, Offset(10f, 50f), Touch, Press) },
                    { verify(0L, pointer3, true, Offset(10f, 80f), Touch, Press) },
                    // first move
                    { verify(16L, pointer1, true, Offset(30f, 20f), Touch, Move) },
                    { verify(16L, pointer2, true, Offset(30f, 50f), Touch, Move) },
                    { verify(16L, pointer3, true, Offset(30f, 80f), Touch, Move) },
                    // second move
                    { verify(32L, pointer1, true, Offset(50f, 20f), Touch, Move) },
                    { verify(32L, pointer2, true, Offset(50f, 50f), Touch, Move) },
                    { verify(32L, pointer3, true, Offset(50f, 80f), Touch, Move) },
                    // third move
                    { verify(48L, pointer1, true, Offset(70f, 20f), Touch, Move) },
                    { verify(48L, pointer2, true, Offset(70f, 50f), Touch, Move) },
                    { verify(48L, pointer3, true, Offset(70f, 80f), Touch, Move) },
                    // last move
                    { verify(64L, pointer1, true, Offset(90f, 20f), Touch, Move) },
                    { verify(64L, pointer2, true, Offset(90f, 50f), Touch, Move) },
                    { verify(64L, pointer3, true, Offset(90f, 80f), Touch, Move) },
                    // pointer1 up
                    { verify(64L, pointer1, false, Offset(90f, 20f), Touch, Release) },
                    { verify(64L, pointer2, true, Offset(90f, 50f), Touch, Release) },
                    { verify(64L, pointer3, true, Offset(90f, 80f), Touch, Release) },
                    // pointer2 up
                    { verify(64L, pointer2, false, Offset(90f, 50f), Touch, Release) },
                    { verify(64L, pointer3, true, Offset(90f, 80f), Touch, Release) },
                    // pointer3 up
                    { verify(64L, pointer3, false, Offset(90f, 80f), Touch, Release) },
                )
            }
        }
    }

    @Suppress("SameParameterValue")
    private fun line(fromX: Float, toX: Float, y: Float, durationMillis: Long): (Long) -> Offset {
        return { Offset(fromX + (toX - fromX) * it / durationMillis, y) }
    }
}
