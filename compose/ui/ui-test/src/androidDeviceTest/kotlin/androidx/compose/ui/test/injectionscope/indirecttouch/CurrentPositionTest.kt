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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.indirect.IndirectPointerEventPrimaryDirectionalMotionAxis
import androidx.compose.ui.test.injectionscope.indirecttouch.Common.performIndirectPointerInput
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.util.ClickableTestBox
import androidx.compose.ui.test.util.MultiPointerInputRecorder
import androidx.compose.ui.unit.IntSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class CurrentPositionTest {
    companion object {
        private val inputDeviceSize = IntSize(3082, 616)
        private val inputDeviceCenter = Offset(1541f, 308f)
        private val inputDeviceTopLeft = Offset(10f, 10f)
    }

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    private val recorder = MultiPointerInputRecorder()

    @Before
    fun setUp() {
        rule.setContent { ClickableTestBox(recorder) }
        rule.onNodeWithTag(ClickableTestBox.defaultTag).requestFocus()
    }

    @Test
    fun currentPosition_noPointersDown() {
        // When we have no pointers down
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            // Then the current position is null
            assertThat(currentPosition(0)).isNull()
            assertThat(currentPosition(1)).isNull()
        }
    }

    @Test
    fun currentPosition_pointer0Down() {
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            // When pointer 0 is down
            down(0, inputDeviceCenter)
            // It is at that position
            assertThat(currentPosition(0)).isEqualTo(inputDeviceCenter)
            // But pointer 1 is null
            assertThat(currentPosition(1)).isNull()
        }
        // And this remains the same in the next invocation
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            assertThat(currentPosition(0)).isEqualTo(inputDeviceCenter)
            assertThat(currentPosition(1)).isNull()
        }
    }

    @Test
    fun currentPosition_pointer1Down() {
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            // When pointer 1 is down
            down(1, inputDeviceCenter)
            // It is at that position
            assertThat(currentPosition(1)).isEqualTo(inputDeviceCenter)
            // But pointer 0 is null
            assertThat(currentPosition(0)).isNull()
        }
        // And this remains the same in the next invocation
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            assertThat(currentPosition(1)).isEqualTo(inputDeviceCenter)
            assertThat(currentPosition(0)).isNull()
        }
    }

    @Test
    fun currentPosition_pointer0And1Down() {
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            // When pointers 0 and 1 are down
            down(0, inputDeviceTopLeft)
            down(1, inputDeviceCenter)
            // They are at that position
            assertThat(currentPosition(0)).isEqualTo(inputDeviceTopLeft)
            assertThat(currentPosition(1)).isEqualTo(inputDeviceCenter)
        }
        // And this remains the same in the next invocation
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            assertThat(currentPosition(0)).isEqualTo(inputDeviceTopLeft)
            assertThat(currentPosition(1)).isEqualTo(inputDeviceCenter)
        }
    }

    @Test
    fun currentPosition_pointerMoved() {
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            // When a pointer is down and moved around
            down(2, inputDeviceTopLeft)
            moveTo(2, inputDeviceCenter)
            // It is at the new position
            assertThat(currentPosition(2)).isEqualTo(inputDeviceCenter)
        }
        // And this remains the same in the next invocation
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            assertThat(currentPosition(2)).isEqualTo(inputDeviceCenter)
        }
    }

    @Test
    fun currentPosition_pointerUp() {
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            // When a pointer is down, moved around and is up again
            down(3, inputDeviceTopLeft)
            moveTo(3, inputDeviceCenter)
            up(3)
            // Its position is null
            assertThat(currentPosition(3)).isNull()
        }
        // And this remains the same in the next invocation
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            assertThat(currentPosition(3)).isNull()
        }
    }

    @Test
    fun currentPosition_pointerCancel() {
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            // When a pointer is down, moved around and the gesture is cancelled
            down(4, inputDeviceTopLeft)
            moveTo(4, inputDeviceCenter)
            cancel()
            // Its position is null
            assertThat(currentPosition(4)).isNull()
        }
        // And this remains the same in the next invocation
        rule.performIndirectPointerInput(
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize,
        ) {
            assertThat(currentPosition(4)).isNull()
        }
    }
}
