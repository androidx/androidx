/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp

@Sampled
fun touchInputClick() {
    composeTestRule.onNodeWithTag("myComponent").performTouchInput { click() }
}

@Sampled
fun touchInputSwipeUp() {
    composeTestRule.onNodeWithTag("myComponent").performTouchInput { swipeUp() }
}

@Sampled
fun touchInputClickOffCenter() {
    composeTestRule.onNodeWithTag("myComponent").performTouchInput {
        click(percentOffset(.2f, .5f))
    }
}

@Sampled
fun touchInputAssertDuringClick() {
    composeTestRule
        .onNodeWithTag("myComponent")
        .performTouchInput { down(topLeft) }
        .assertHasClickAction()
        .performTouchInput { up() }
}

@Sampled
fun touchInputClickAndDrag() {
    composeTestRule.onNodeWithTag("myComponent").performTouchInput {
        click()
        advanceEventTime(100)
        swipeUp()
    }
}

@Sampled
fun touchInputLShapedGesture() {
    composeTestRule.onNodeWithTag("myComponent").performTouchInput {
        down(topLeft)
        moveTo(topLeft + percentOffset(0f, .1f))
        moveTo(topLeft + percentOffset(0f, .2f))
        moveTo(topLeft + percentOffset(0f, .3f))
        moveTo(topLeft + percentOffset(0f, .4f))
        moveTo(centerLeft)
        moveTo(centerLeft + percentOffset(.1f, 0f))
        moveTo(centerLeft + percentOffset(.2f, 0f))
        moveTo(centerLeft + percentOffset(.3f, 0f))
        moveTo(centerLeft + percentOffset(.4f, 0f))
        moveTo(center)
        up()
    }
}

@Sampled
fun touchInputMultiTouchWithHistory() {
    // Move two fingers in a horizontal line, one on y=100 and one on y=500
    composeTestRule.onNodeWithTag("myComponent").performTouchInput {
        // First, make contact with the screen with both pointers:
        down(0, Offset(300f, 100f))
        down(1, Offset(300f, 500f))
        // Update the pointer locations for the next event
        updatePointerTo(0, Offset(400f, 100f))
        updatePointerTo(1, Offset(400f, 500f))
        // And send the move event with historical data
        @OptIn(ExperimentalTestApi::class)
        moveWithHistoryMultiPointer(
            // Let's add 3 historical events
            relativeHistoricalTimes = listOf(-12, -8, -4),
            // Now, for each pointer we supply the historical coordinates
            historicalCoordinates =
                listOf(
                    // Pointer 0 moves along y=100
                    listOf(Offset(325f, 100f), Offset(350f, 100f), Offset(375f, 100f)),
                    // Pointer 1 moves along y=500
                    listOf(Offset(325f, 500f), Offset(350f, 500f), Offset(375f, 500f)),
                ),
            // The actual move event will be sent 16ms after the previous event
            delayMillis = 16
        )
        // And finish the gesture by lifting both fingers. Can be done in any order
        up(1)
        up(0)
    }
}
