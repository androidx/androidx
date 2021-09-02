/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.SwipeDismissTarget
import androidx.wear.compose.material.SwipeToDismissBox
import androidx.wear.compose.material.TEST_TAG
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.rememberSwipeToDismissBoxState
import androidx.wear.compose.material.setContentWithTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@ExperimentalWearMaterialApi
class SwipeToDismissBoxTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun supports_testtag() {
        rule.setContentWithTheme {
            val state = rememberSwipeToDismissBoxState()
            SwipeToDismissBox(
                state = state,
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                Text("Test")
            }
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun dismisses_when_swiped_right() =
        verifySwipe(gesture = { swipeRight() }, expectedToDismiss = true)

    @Test
    fun does_not_dismiss_when_swiped_left() =
        // Swipe left is met with resistance and is not a swipe-to-dismiss.
        verifySwipe(gesture = { swipeLeft() }, expectedToDismiss = false)

    @Test
    fun does_not_dismiss_when_swipe_right_incomplete() =
        // Execute a partial swipe over a longer-than-default duration so that there
        // is insufficient velocity to perform a 'fling'.
        verifySwipe(
            gesture = { swipeRight(startX = 0f, endX = width / 4f, durationMillis = LONG_SWIPE) },
            expectedToDismiss = false
        )

    @Test
    fun does_not_display_background_without_swipe() {
        rule.setContentWithTheme {
            val state = rememberSwipeToDismissBoxState()
            SwipeToDismissBox(
                state = state,
                modifier = Modifier.testTag(TEST_TAG),
                background = {
                    Text(BACKGROUND_MESSAGE)
                },
            ) {
                messageContent()
            }
        }

        rule.onNodeWithText(BACKGROUND_MESSAGE).assertDoesNotExist()
    }

    @Test
    fun displays_background_during_swipe() =
        verifyPartialSwipe(expectedMessage = BACKGROUND_MESSAGE)

    @Test
    fun displays_content_during_swipe() =
        verifyPartialSwipe(expectedMessage = CONTENT_MESSAGE)

    private fun verifySwipe(gesture: TouchInjectionScope.() -> Unit, expectedToDismiss: Boolean) {
        var dismissed = false
        rule.setContentWithTheme {
            val state = rememberSwipeToDismissBoxState()
            LaunchedEffect(state.currentValue) {
                dismissed =
                    state.currentValue == SwipeDismissTarget.Dismissal
            }
            SwipeToDismissBox(
                state = state,
                modifier = Modifier.testTag(TEST_TAG),
            ) {
                messageContent()
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput(gesture)

        rule.runOnIdle {
            assertEquals(expectedToDismiss, dismissed)
        }
    }

    private fun verifyPartialSwipe(expectedMessage: String) {
        rule.setContentWithTheme {
            val state = rememberSwipeToDismissBoxState()
            SwipeToDismissBox(
                state = state,
                modifier = Modifier.testTag(TEST_TAG),
                background = {
                    Text(BACKGROUND_MESSAGE)
                },
            ) {
                messageContent()
            }
        }

        // Advance the clock by half the length of time configured for the swipe gesture,
        // so that the background ought to be revealed.
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag(TEST_TAG).performTouchInput { swipeRight(durationMillis = LONG_SWIPE) }
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(milliseconds = LONG_SWIPE / 2)

        rule.onNodeWithText(expectedMessage).assertExists()
    }

    @Composable
    fun messageContent() {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(CONTENT_MESSAGE, color = MaterialTheme.colors.onPrimary)
        }
    }
}

internal const val BACKGROUND_MESSAGE = "The Background"
internal const val CONTENT_MESSAGE = "The Content"
internal const val LONG_SWIPE = 1000L
