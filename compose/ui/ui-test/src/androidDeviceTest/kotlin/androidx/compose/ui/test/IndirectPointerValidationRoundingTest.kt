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

package androidx.compose.ui.test

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.indirect.IndirectPointerEventPrimaryDirectionalMotionAxis
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IndirectPointerValidationRoundingTest {

    private val UiSizeNotRelatedToInputDeviceSizeTesting = 10.dp

    @get:Rule val rule = createComposeRule()

    @Test
    fun validatePosition_allowsTinyNegativeXCoordinate() {
        rule.setContent {
            Box(Modifier.testTag("box").size(UiSizeNotRelatedToInputDeviceSizeTesting))
        }

        // This should not throw if we have a small epsilon
        rule.onNodeWithTag("box").performIndirectPointerInput(
            indirectPointerEventPrimaryDirectionalMotionAxis =
                IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize = IntSize(100, 100),
        ) {
            down(0, Offset(-0.0000001f, 0f))
        }
    }

    @Test
    fun validatePosition_allowsTinyPositiveXCoordinate() {
        rule.setContent {
            Box(Modifier.testTag("box").size(UiSizeNotRelatedToInputDeviceSizeTesting))
        }

        // This should not throw if we have a small epsilon
        rule.onNodeWithTag("box").performIndirectPointerInput(
            indirectPointerEventPrimaryDirectionalMotionAxis =
                IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize = IntSize(100, 100),
        ) {
            down(0, Offset(100.001f, 0f))
        }
    }

    @Test
    fun validatePosition_allowsTinyNegativeYCoordinate() {
        rule.setContent {
            Box(Modifier.testTag("box").size(UiSizeNotRelatedToInputDeviceSizeTesting))
        }

        // This should not throw if we have a small epsilon
        rule.onNodeWithTag("box").performIndirectPointerInput(
            indirectPointerEventPrimaryDirectionalMotionAxis =
                IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize = IntSize(100, 100),
        ) {
            down(0, Offset(0f, -0.0000001f))
        }
    }

    @Test
    fun validatePosition_allowsTinyPositiveYCoordinate() {
        rule.setContent {
            Box(Modifier.testTag("box").size(UiSizeNotRelatedToInputDeviceSizeTesting))
        }

        // This should not throw if we have a small epsilon
        rule.onNodeWithTag("box").performIndirectPointerInput(
            indirectPointerEventPrimaryDirectionalMotionAxis =
                IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            inputDeviceSize = IntSize(100, 100),
        ) {
            down(0, Offset(0f, 100.001f))
        }
    }

    @Test
    fun validatePosition_throwsWhenXCoordinateTooNegative() {
        rule.setContent {
            Box(Modifier.testTag("box").size(UiSizeNotRelatedToInputDeviceSizeTesting))
        }
        assertThrows(IllegalArgumentException::class.java) {
            rule.onNodeWithTag("box").performIndirectPointerInput(
                indirectPointerEventPrimaryDirectionalMotionAxis =
                    IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                inputDeviceSize = IntSize(100, 100),
            ) {
                down(0, Offset(-1f, 0f))
            }
        }
    }

    @Test
    fun validatePosition_throwsWhenXCoordinateTooPositive() {
        rule.setContent {
            Box(Modifier.testTag("box").size(UiSizeNotRelatedToInputDeviceSizeTesting))
        }
        assertThrows(IllegalArgumentException::class.java) {
            rule.onNodeWithTag("box").performIndirectPointerInput(
                indirectPointerEventPrimaryDirectionalMotionAxis =
                    IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                inputDeviceSize = IntSize(100, 100),
            ) {
                down(0, Offset(101f, 0f))
            }
        }
    }

    @Test
    fun validatePosition_throwsWhenYCoordinateTooNegative() {
        rule.setContent {
            Box(Modifier.testTag("box").size(UiSizeNotRelatedToInputDeviceSizeTesting))
        }
        assertThrows(IllegalArgumentException::class.java) {
            rule.onNodeWithTag("box").performIndirectPointerInput(
                indirectPointerEventPrimaryDirectionalMotionAxis =
                    IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                inputDeviceSize = IntSize(100, 100),
            ) {
                down(0, Offset(0f, -1f))
            }
        }
    }

    @Test
    fun validatePosition_throwsWhenYCoordinateTooPositive() {
        rule.setContent {
            Box(Modifier.testTag("box").size(UiSizeNotRelatedToInputDeviceSizeTesting))
        }
        assertThrows(IllegalArgumentException::class.java) {
            rule.onNodeWithTag("box").performIndirectPointerInput(
                indirectPointerEventPrimaryDirectionalMotionAxis =
                    IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                inputDeviceSize = IntSize(100, 100),
            ) {
                down(0, Offset(0f, 101f))
            }
        }
    }
}
