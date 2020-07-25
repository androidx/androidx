/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.test

import androidx.compose.runtime.state
import androidx.test.filters.FlakyTest
import androidx.test.filters.MediumTest
import androidx.ui.core.Modifier
import androidx.ui.core.testTag
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.ui.test.util.expectAssertionError
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class AssertExistsTest {

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    @Test
    @FlakyTest
    fun toggleTextInHierarchy_assertExistsAndNotExists() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface {
                    val (showText, toggle) = state { true }
                    Column {
                        Button(
                            modifier = Modifier.testTag("MyButton"),
                            onClick = { toggle(!showText) }
                        ) {
                            Text("Toggle")
                        }
                        if (showText) {
                            Text("Hello")
                        }
                    }
                }
            }
        }

        onNodeWithText("Hello")
            .assertExists()

        expectAssertionError(true) {
            onNodeWithText("Hello")
                .assertDoesNotExist()
        }

        val cachedResult = onNodeWithText("Hello")

        // Hide
        onNodeWithTag("MyButton")
            .performClick()

        onNodeWithText("Hello")
            .assertDoesNotExist()

        cachedResult
            .assertDoesNotExist()

        expectAssertionError(true) {
            onNodeWithText("Hello")
                .assertExists()
        }

        expectAssertionError(true) {
            cachedResult.assertExists()
        }

        // Show
        onNodeWithTag("MyButton")
            .performClick()

        onNodeWithText("Hello")
            .assertExists()

        expectAssertionError(true) {
            onNodeWithText("Hello")
                .assertDoesNotExist()
        }
    }
}