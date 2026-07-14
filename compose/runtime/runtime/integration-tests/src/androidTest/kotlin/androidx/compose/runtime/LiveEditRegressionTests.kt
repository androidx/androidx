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

package androidx.compose.runtime

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(InternalComposeApi::class)
@RunWith(AndroidJUnit4::class)
class LiveEditRegressionTests {
    @get:Rule val composeTestRule = createComposeRule(effectContext = StandardTestDispatcher())

    @Before
    fun setUp() {
        // ensures recomposer knows that hot reload is on
        invalidateGroupsWithKey(-1)
    }

    @After
    fun tearDown() {
        clearCompositionErrors()
        disableHotReloadMode()
    }

    @Test
    @MediumTest
    fun errorInAnimatedVisibility() {
        val shouldThrow = mutableStateOf(true)
        var errorState = false
        composeTestRule.setContent {
            var showContent by remember { mutableStateOf(false) }
            Column {
                Button(
                    modifier = Modifier.testTag("button"),
                    onClick = { showContent = !showContent },
                ) {}
                AnimatedVisibility(showContent) {
                    errorState = shouldThrow.value
                    if (errorState) {
                        error("")
                    }
                }
            }
        }

        assertFalse("Initial error state should be false", errorState)

        val button = composeTestRule.onNodeWithTag("button")
        button.performClick()
        composeTestRule.waitForIdle()

        assertTrue("should come to error state after clicking button", errorState)

        // we add pending work here (button hover effects) that should be resumed on recover
        button.performMouseInput { enter(center) }

        shouldThrow.value = false
        composeTestRule.runOnUiThread {
            // try to recover from error
            invalidateGroupsWithKey(-1)
        }

        // as we invalidated the whole state, click the button again to toggle animation
        button.performClick()

        composeTestRule.waitForIdle()
        assertFalse("should recover from error state", errorState)
    }

    /**
     * An error thrown while recomposing the content of a subcomposition (here a
     * [BoxWithConstraints], which composes its content in a child composition) must not permanently
     * wedge the *parent* (root) composition once the error is fixed and reloaded.
     */
    @Test
    @MediumTest
    fun errorInBoxWithConstraints() {
        val shouldThrow = mutableStateOf(false)
        val reloadTick = mutableStateOf("iteration=0")
        var boxErrored = false
        var observedButtonClicked = false

        composeTestRule.setContent {
            // The root observes 'reloadTick' so it recomposes on every simulated reload.
            reloadTick.value
            var buttonClicked by remember { mutableStateOf(false) }
            // The root observes 'buttonClicked'. If this observation survives, the button toggles
            // it.
            observedButtonClicked = buttonClicked
            Column {
                Button(
                    modifier = Modifier.testTag("button"),
                    onClick = { buttonClicked = !buttonClicked },
                ) {}
                BoxWithConstraints {
                    boxErrored = shouldThrow.value
                    if (boxErrored) error("boom in BoxWithConstraints subcomposition")
                }
            }
        }
        composeTestRule.waitForIdle()
        assertFalse("no error initially", boxErrored)

        // Bump the tick the root observes so it recomposes in the same frame as the reload.
        composeTestRule.runOnUiThread {
            shouldThrow.value = true
            reloadTick.value = "iteration=1"
            invalidateGroupsWithKey(-1)
        }
        composeTestRule.waitForIdle()
        assertTrue("subcomposition should have thrown", boxErrored)

        // "Fix the error and reload".
        composeTestRule.runOnUiThread {
            shouldThrow.value = false
            reloadTick.value = "iteration=2"
            invalidateGroupsWithKey(-1)
        }
        composeTestRule.waitForIdle()

        // The UI must be interactive again: clicking the button toggles 'buttonClicked', which must
        // recompose the root scope observing it.
        composeTestRule.onNodeWithTag("button").performClick()
        composeTestRule.waitForIdle()
        assertTrue(
            "root must still observe 'buttonClicked' after recovering from a subcomposition error " +
                "(the click should have toggled it)",
            observedButtonClicked,
        )
    }

    /**
     * Once the subcomposition has failed, the recomposer keeps it in its failed list. On the next
     * reload [invalidateGroupsWithKey] runs `retryFailedCompositions`, which re-invokes the failed
     * composition's *stored* content lambda. After the block is commented out, the hot-swap has
     * removed that lambda's backing method, so re-invoking it throws a [LinkageError] (a
     * [NoSuchMethodError]) rather than the original error. This test simulates exactly that: the
     * stored content lambda throws [NoSuchMethodError] once [commentedOut] is set, while the
     * current content stops emitting the block at all.
     *
     * If we retry that failed composition, the recomposer wedges and the parent never recomposes
     * again, so the final click does not toggle 'buttonClicked' and this assertion fails.
     */
    @Test
    @MediumTest
    fun commentingOutErroringBoxWithConstraints() {
        val commentedOut = mutableStateOf(false)
        val reloadTick = mutableStateOf("iteration=0")
        var observedButtonClicked = false

        composeTestRule.setContent {
            // The root observes 'reloadTick' so it recomposes on every simulated reload.
            reloadTick.value
            var buttonClicked by remember { mutableStateOf(false) }
            // The root observes 'buttonClicked'. If this observation survives, the button toggles
            // it.
            observedButtonClicked = buttonClicked
            Column {
                Button(
                    modifier = Modifier.testTag("button"),
                    onClick = { buttonClicked = !buttonClicked },
                ) {}
                if (!commentedOut.value) {
                    BoxWithConstraints {
                        BoxWithConstraints {
                            // Re-invoking this stored lambda after the block is commented out
                            // simulates calling a composable whose method the hot-swap removed.
                            // This is only reached via retryFailedCompositions; the current content
                            // stops emitting the block once 'commentedOut' is set.
                            if (commentedOut.value) {
                                throw NoSuchMethodError("simulated: composable removed by hot-swap")
                            }
                            error("boom in nested BoxWithConstraints subcomposition")
                        }
                    }
                }
            }
        }
        // The block errored during its initial subcomposition, so it is now a failed composition.
        composeTestRule.waitForIdle()

        // "Comment out the whole BoxWithConstraints block and reload": the current content no
        // longer emits it
        composeTestRule.runOnUiThread {
            commentedOut.value = true
            reloadTick.value = "iteration=1"
            invalidateGroupsWithKey(-1)
        }
        composeTestRule.waitForIdle()

        // The parent must stay interactive: clicking toggles 'buttonClicked', which must recompose.
        composeTestRule.onNodeWithTag("button").performClick()
        composeTestRule.waitForIdle()
        assertTrue(
            "root must stay interactive after commenting out an erroring BoxWithConstraints block " +
                "(retrying the removed subcomposition's stale lambda throws a LinkageError)",
            observedButtonClicked,
        )
    }
}
