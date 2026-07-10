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

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.indirect.IndirectPointerEventPrimaryDirectionalMotionAxis
import androidx.compose.ui.test.click
import androidx.compose.ui.test.injectionscope.indirecttouch.Common.performIndirectPointerInput
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.util.ClickableTestBox
import androidx.compose.ui.test.util.ClickableTestBox.defaultTag
import androidx.compose.ui.test.util.SinglePointerInputRecorder
import androidx.compose.ui.unit.IntSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests if the current time of gestures is aligned with the main test clock */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SynchronizedWithMainClockTest {
    companion object {
        private val inputDeviceSize = IntSize(3082, 616)
    }

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    private val recorder = SinglePointerInputRecorder()

    @Before
    fun setUp() {
        // Given some content
        rule.setContent { ClickableTestBox(recorder) }
        rule.onNodeWithTag(defaultTag).requestFocus()
    }

    @Test
    fun zeroTimeBetween_performTouchInput() {
        testWithTwoGestures(expectedDifference = 0, betweenGesturesBlock = {})
    }

    @Test
    fun someTimeBetween_performTouchInput() {
        testWithTwoGestures(
            expectedDifference = 1273,
            betweenGesturesBlock = {
                rule.mainClock.advanceTimeBy(1273, ignoreFrameDuration = true)
            },
        )
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun testWithTwoGestures(expectedDifference: Long, betweenGesturesBlock: () -> Unit) {
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            click()
        }
        betweenGesturesBlock.invoke()
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            click()
        }

        rule.runOnIdle {
            recorder.run {
                val hasExtraMove = true
                assertThat(events).hasSize(if (hasExtraMove) 6 else 4)
                val t1 = if (hasExtraMove) events[2].timestamp else events[1].timestamp
                val t2 = if (hasExtraMove) events[3].timestamp else events[2].timestamp
                assertThat(t2 - t1).isEqualTo(expectedDifference)
            }
        }
    }
}
