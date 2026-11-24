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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue

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

@Sampled
fun runOnEachSnapshotApplySample() {
    var itemCount by mutableIntStateOf(0)
    // itemCount is snapshot state that the screen under test reads and updates. The block runs on
    // each snapshot applied from now on, so register it before the actions to check.
    composeTestRule.runOnEachSnapshotApply {
        // Asserts on the state behind the UI, as blocks can't look up nodes
        assertTrue("Item count must never be negative", itemCount >= 0)
    }

    composeTestRule.onNodeWithTag("add").performClick()
    composeTestRule.onNodeWithTag("remove").performClick()
    // Checks what the UI shows at a checkpoint
    composeTestRule.onNodeWithTag("itemCount").assertTextEquals("Count: 0")
}

@Sampled
fun runOnEachSnapshotApplyScopedSample() {
    fun <R> ComposeTestRule.withSnapshotInvariant(assertion: () -> Unit, block: () -> R): R {
        val handle = runOnEachSnapshotApply(assertion)
        try {
            return block()
        } finally {
            handle.dispose()
        }
    }

    var itemCount by mutableIntStateOf(0)
    composeTestRule.withSnapshotInvariant(assertion = { assertTrue(itemCount >= 0) }) {
        composeTestRule.onNodeWithTag("add").performClick()
    }
}
