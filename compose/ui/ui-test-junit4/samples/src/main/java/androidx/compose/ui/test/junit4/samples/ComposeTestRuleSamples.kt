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

package androidx.compose.ui.test.junit4.samples

import androidx.annotation.Sampled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick

@Sampled
fun runWithoutImplicitWaitSample() {
    composeTestRule.mainClock.autoAdvance = false

    // Trigger an animation
    composeTestRule.onNodeWithText("Start Animation").performClick()

    // Step through the animation frame-by-frame
    while (composeTestRule.hasPendingWork()) {
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.waitForIdle()
        composeTestRule.runOnUiThread {
            // Suppress implicit synchronization inside this block to avoid redundant
            // waits on each node query, making the frame assertions execute much faster.
            composeTestRule.runWithoutImplicitWait {
                val box1 = composeTestRule.onNodeWithTag("Box1").fetchSemanticsNode()
                val box2 = composeTestRule.onNodeWithTag("Box2").fetchSemanticsNode()

                assert(box1.boundsInRoot.right <= box2.boundsInRoot.left)
            }
        }
    }
}

@Sampled
fun hasPendingWorkSample() {
    composeTestRule.mainClock.autoAdvance = false

    // Trigger the animation
    composeTestRule.onNodeWithTag("ExpandButton").performClick()

    while (composeTestRule.hasPendingWork()) {
        // Advance the clock by exactly one frame
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.waitForIdle()
        composeTestRule.runOnUiThread {
            composeTestRule.runWithoutImplicitWait {
                // Make intermediate assertions (e.g., check bounds or visibility)
                composeTestRule.onNodeWithTag("CardContent").assertExists()
            }
        }
    }
}
