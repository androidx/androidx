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

package androidx.compose.ui.test.samples

import androidx.annotation.Sampled
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.indirect.IndirectPointerEventPrimaryDirectionalMotionAxis
import androidx.compose.ui.test.click
import androidx.compose.ui.test.inputDeviceCenterX
import androidx.compose.ui.test.inputDeviceCenterY
import androidx.compose.ui.test.inputDeviceLeft
import androidx.compose.ui.test.inputDeviceRight
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performIndirectPointerInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.IntSize

// Swipe options
@Sampled
fun indirectPointerInputSwipeRight() {
    // Ensure your node is within the focus path (otherwise, you won't get the event).
    composeTestRule.onNodeWithTag("myComponent").requestFocus()
    composeTestRule.performIndirectPointerInput(
        indirectPointerEventPrimaryDirectionalMotionAxis =
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
        // Horizontal trackpad
        inputDeviceSize = IntSize(width = 5000, height = 1000),
    ) {
        swipeRight(startX = inputDeviceLeft, endX = inputDeviceRight)
    }
}

// Click options:
@Sampled
fun indirectPointerInputClick() {
    // Ensure the node is within the focus path (otherwise, you won't get the event).
    composeTestRule.onNodeWithTag("myComponent").requestFocus()
    composeTestRule.performIndirectPointerInput(
        indirectPointerEventPrimaryDirectionalMotionAxis =
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
        // Horizontal trackpad
        inputDeviceSize = IntSize(width = 5000, height = 1000),
    ) {
        click()
    }
}

@Sampled
fun indirectPointerInputAssertDuringClick() {
    // Ensure the node is within the focus path (otherwise, you won't get the event).
    composeTestRule.onNodeWithTag("myComponent").requestFocus()

    composeTestRule.performIndirectPointerInput(
        indirectPointerEventPrimaryDirectionalMotionAxis =
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
        // Horizontal trackpad
        inputDeviceSize = IntSize(width = 5000, height = 1000),
    ) {
        down(position = Offset(x = inputDeviceCenterX, y = inputDeviceCenterY))
    }

    // Assert some pressed state is visible

    composeTestRule.performIndirectPointerInput(
        indirectPointerEventPrimaryDirectionalMotionAxis =
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
        // Horizontal trackpad
        inputDeviceSize = IntSize(width = 5000, height = 1000),
    ) {
        up()
    }
}

@Sampled
fun indirectPointerInputClickAndDrag() {
    // Ensure the node is within the focus path (otherwise, you won't get the event).
    composeTestRule.onNodeWithTag("myComponent").requestFocus()
    composeTestRule.performIndirectPointerInput(
        indirectPointerEventPrimaryDirectionalMotionAxis =
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
        // Horizontal trackpad
        inputDeviceSize = IntSize(width = 5000, height = 1000),
    ) {
        click()
        advanceEventTime(durationMillis = 100)
        swipeLeft(startX = inputDeviceRight, endX = inputDeviceLeft)
    }
}
