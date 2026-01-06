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
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
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
}
