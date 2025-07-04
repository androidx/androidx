/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.wear.compose.material3

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.wear.compose.material3.SwipeToRevealDefaults.SingleActionAnchorWidth
import androidx.wear.compose.materialcore.CustomTouchSlopProvider
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

class SwipeToRevealLeakTest {
    private val composeTestRule = createComposeRule()

    @get:Rule
    val ruleChain: RuleChain =
        RuleChain.outerRule(DetectLeaksAfterTestSuccess()).around(composeTestRule)

    @Test
    fun shouldNotLeak() {
        composeTestRule.setContent {
            CustomTouchSlopProvider(newTouchSlop = 0f) {
                Column {
                    SwipeToReveal(modifier = Modifier.testTag(SWIPE_TO_REVEAL_TAG))
                    SwipeToReveal(modifier = Modifier.testTag(SWIPE_TO_REVEAL_SECOND_TAG))
                }
            }
        }

        // swipe the first S2R to Revealing state
        composeTestRule.onNodeWithTag(SWIPE_TO_REVEAL_TAG).performTouchInput {
            swipeLeftToRevealing(density)
        }

        // swipe the second S2R to Revealing state
        composeTestRule.onNodeWithTag(SWIPE_TO_REVEAL_SECOND_TAG).performTouchInput {
            swipeLeftToRevealing(density)
        }
    }

    @Composable
    private fun SwipeToReveal(modifier: Modifier = Modifier) {
        SwipeToReveal(
            primaryAction = {
                PrimaryActionButton(
                    onClick = {}, /* Empty for testing */
                    { Icon(Icons.Outlined.Close, contentDescription = "Clear") },
                    { Text("Clear") },
                )
            },
            onSwipePrimaryAction = {}, /* Empty for testing */
            modifier = modifier,
            secondaryAction = {
                SecondaryActionButton({}, /* Empty for testing */ {} /* Empty for testing */)
            },
            undoPrimaryAction = {
                UndoActionButton({}, /* Empty for testing */ {} /* Empty for testing */)
            },
            undoSecondaryAction = { UndoActionButton({}, { Text("Undo Secondary") }) },
            revealState = rememberRevealState(initialValue = RevealValue.RightRevealing),
        ) {
            Button({}, Modifier.fillMaxWidth()) { Text("Swipe me!") }
        }
    }

    private fun TouchInjectionScope.swipeLeftToRevealing(density: Float) {
        val singleActionAnchorWidthPx = SingleActionAnchorWidth.value * density
        swipeLeft(startX = right, endX = right - (singleActionAnchorWidthPx * 0.75f))
    }

    companion object {
        private const val SWIPE_TO_REVEAL_TAG = TEST_TAG
        private const val SWIPE_TO_REVEAL_SECOND_TAG = "SWIPE_TO_REVEAL_SECOND_TAG"
    }
}
