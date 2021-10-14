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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.SwipeDismissTarget
import androidx.wear.compose.material.SwipeToDismissBox
import androidx.wear.compose.material.TEST_TAG
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleButton
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
                Text("Testing")
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
            ) { isBackground ->
                if (isBackground) Text(BACKGROUND_MESSAGE) else messageContent()
            }
        }

        rule.onNodeWithText(BACKGROUND_MESSAGE).assertDoesNotExist()
    }

    @Test
    fun does_not_dismiss_if_has_background_is_false() {
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
                hasBackground = false,
            ) {
                Text(CONTENT_MESSAGE, color = MaterialTheme.colors.onPrimary)
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput({ swipeRight() })

        rule.runOnIdle {
            assertEquals(false, dismissed)
        }
    }

    @Test
    fun remembers_saved_state() {
        val showCounterForContent = mutableStateOf(true)
        rule.setContentWithTheme {
            val state = rememberSwipeToDismissBoxState()
            val holder = rememberSaveableStateHolder()
            LaunchedEffect(state.currentValue) {
                if (state.currentValue == SwipeDismissTarget.Dismissal) {
                    showCounterForContent.value = !showCounterForContent.value
                    state.snapTo(SwipeDismissTarget.Original)
                }
            }
            SwipeToDismissBox(
                state = state,
                modifier = Modifier.testTag(TEST_TAG),
                backgroundKey = if (showCounterForContent.value) TOGGLE_SCREEN else COUNTER_SCREEN,
                contentKey = if (showCounterForContent.value) COUNTER_SCREEN else TOGGLE_SCREEN,
                content = { isBackground ->
                    if (showCounterForContent.value xor isBackground)
                        counterScreen(holder)
                    else
                        toggleScreen(holder)
                }
            )
        }

        // Start with foreground showing Counter screen.
        rule.onNodeWithTag(COUNTER_SCREEN).assertTextContains("0")
        rule.onNodeWithTag(COUNTER_SCREEN).performClick()
        rule.waitForIdle()
        rule.onNodeWithTag(COUNTER_SCREEN).assertTextContains("1")

        // Swipe to switch to Toggle screen
        rule.onNodeWithTag(TEST_TAG).performTouchInput({ swipeRight() })
        rule.waitForIdle()
        rule.onNodeWithTag(TOGGLE_SCREEN).assertIsOff()
        rule.onNodeWithTag(TOGGLE_SCREEN).performClick()
        rule.waitForIdle()
        rule.onNodeWithTag(TOGGLE_SCREEN).assertIsOn()

        // Swipe back to Counter screen
        rule.onNodeWithTag(TEST_TAG).performTouchInput({ swipeRight() })
        rule.waitForIdle()
        rule.onNodeWithTag(COUNTER_SCREEN).assertTextContains("1")

        // Swipe back to Toggle screen
        rule.onNodeWithTag(TEST_TAG).performTouchInput({ swipeRight() })
        rule.waitForIdle()
        rule.onNodeWithTag(TOGGLE_SCREEN).assertIsOn()
    }

    @Test
    fun gives_top_swipe_box_gestures_when_nested() {
        var outerDismissed = false
        var innerDismissed = false
        rule.setContentWithTheme {
            val outerState = rememberSwipeToDismissBoxState()
            LaunchedEffect(outerState.currentValue) {
                outerDismissed = outerState.currentValue == SwipeDismissTarget.Dismissal
            }
            SwipeToDismissBox(
                state = outerState,
                modifier = Modifier.testTag("OUTER"),
                hasBackground = true,
            ) {
                Text("Outer", color = MaterialTheme.colors.onPrimary)
                val innerState = rememberSwipeToDismissBoxState()
                LaunchedEffect(innerState.currentValue) {
                    innerDismissed = innerState.currentValue == SwipeDismissTarget.Dismissal
                }
                SwipeToDismissBox(
                    state = innerState,
                    modifier = Modifier.testTag("INNER"),
                    hasBackground = true,
                ) {
                    Text(
                        text = "Inner",
                        color = MaterialTheme.colors.onPrimary,
                        modifier = Modifier.testTag(TEST_TAG)
                    )
                }
            }
        }

        rule.onNodeWithTag(TEST_TAG).performTouchInput({ swipeRight() })

        rule.runOnIdle {
            assertEquals(true, innerDismissed)
            assertEquals(false, outerDismissed)
        }
    }

    @Composable
    fun toggleScreen(saveableStateHolder: SaveableStateHolder) {
        saveableStateHolder.SaveableStateProvider(TOGGLE_SCREEN) {
            var toggle by rememberSaveable { mutableStateOf(false) }
            ToggleButton(
                checked = toggle,
                onCheckedChange = { toggle = !toggle },
                content = { Text(text = if (toggle) TOGGLE_ON else TOGGLE_OFF) },
                modifier = Modifier.testTag(TOGGLE_SCREEN)
            )
        }
    }

    @Composable
    fun counterScreen(saveableStateHolder: SaveableStateHolder) {
        saveableStateHolder.SaveableStateProvider(COUNTER_SCREEN) {
            var counter by rememberSaveable { mutableStateOf(0) }
            Button(
                onClick = { ++counter },
                modifier = Modifier.testTag(COUNTER_SCREEN)
            ) {
                Text(text = "" + counter)
            }
        }
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
            ) { isBackground ->
                if (isBackground) Text(BACKGROUND_MESSAGE) else messageContent()
            }
        }

        // Click down and drag across 1/4 of the screen to start a swipe,
        // but don't release the finger, so that the screen can be inspected
        // (note that swipeRight would release the finger and does not pause time midway).
        rule.onNodeWithTag(TEST_TAG).performTouchInput(
            {
                down(Offset(x = 0f, y = height / 2f))
                moveTo(Offset(x = width / 4f, y = height / 2f))
            }
        )

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
internal const val TOGGLE_SCREEN = "Toggle"
internal const val COUNTER_SCREEN = "Counter"
internal const val TOGGLE_ON = "On"
internal const val TOGGLE_OFF = "Off"