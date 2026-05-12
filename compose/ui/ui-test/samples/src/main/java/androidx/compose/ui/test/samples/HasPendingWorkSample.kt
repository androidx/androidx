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
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import junit.framework.TestCase.assertTrue

private const val COLLAPSED_HEIGHT = 100f

@Composable
fun ExpandingCard() {
    /* Not implemented */
}

@OptIn(ExperimentalTestApi::class)
@Sampled
fun hasPendingWorkSample() = runComposeUiTest {
    setContent { ExpandingCard() }

    mainClock.autoAdvance = false

    // Trigger the animation
    onNodeWithTag("ExpandButton").performClick()

    while (hasPendingWork()) {
        // Advance the clock by exactly one frame
        mainClock.advanceTimeByFrame()
        waitForIdle()
        runOnUiThread {
            runWithoutImplicitWait {
                // Make intermediate assertions (e.g., check bounds, alpha, or translation)
                val midAnimationBounds =
                    onNodeWithTag("CardContent").fetchSemanticsNode().boundsInRoot
                assertTrue(
                    "Card height should be greater than collapsed state",
                    midAnimationBounds.height > COLLAPSED_HEIGHT,
                )
                onNodeWithTag("ExpandedTextDetails").assertExists()
            }
        }
    }
    runOnIdle {
        // Assert the final state.
    }
}
