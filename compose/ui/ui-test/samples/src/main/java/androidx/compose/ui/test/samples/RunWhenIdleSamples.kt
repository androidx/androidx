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
fun runWhenIdleSample() = runComposeUiTest {
    // ExpandingCard has a 500ms expansion animation
    setContent { ExpandingCard() }

    mainClock.autoAdvance = false

    // Trigger the interaction that starts the animation
    onNodeWithTag("ExpandButton").performClick()

    // Manually advance the clock to the halfway point (250ms)
    mainClock.advanceTimeBy(250L)

    // Inspect the intermediate UI state.
    // The framework waits for the layout pass triggered by advanceTimeBy()
    // to settle, then executes these checks.
    runWhenIdle {
        val midAnimationBounds = onNodeWithTag("CardContent").fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Card height should be greater than collapsed state",
            midAnimationBounds.height > COLLAPSED_HEIGHT,
        )
        onNodeWithTag("ExpandedTextDetails").assertExists()
    }
}

@OptIn(ExperimentalTestApi::class)
@Sampled
fun awaitAndRunWhenIdleSample() = runComposeUiTest {
    setContent { ExpandingCard() }

    mainClock.autoAdvance = false

    // Trigger the interaction that starts the animation
    onNodeWithTag("ExpandButton").performClick()

    // Manually advance the clock to the halfway point (250ms)
    mainClock.advanceTimeBy(250L)

    // Inspect the intermediate UI state.
    // The framework waits for the layout pass triggered by advanceTimeBy()
    // to settle, then executes these checks.
    awaitAndRunWhenIdle {
        val midAnimationBounds = onNodeWithTag("CardContent").fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Card height should be greater than collapsed state",
            midAnimationBounds.height > COLLAPSED_HEIGHT,
        )
        onNodeWithTag("ExpandedTextDetails").assertExists()
    }
}
