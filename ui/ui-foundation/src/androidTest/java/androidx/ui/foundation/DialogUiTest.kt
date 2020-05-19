/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.ui.foundation

import androidx.compose.state
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.UiDevice
import androidx.ui.core.Modifier
import androidx.ui.test.assertIsDisplayed
import androidx.ui.test.createComposeRule
import androidx.ui.test.doClick
import androidx.ui.test.findByText
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class DialogUiTest {
    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    private val defaultText = "dialogText"

    @Test
    fun dialogTest_isShowingContent() {
        composeTestRule.setContent {
            val showDialog = state { true }

            if (showDialog.value) {
                Dialog(onCloseRequest = {}) {
                    Text(defaultText)
                }
            }
        }

        findByText(defaultText).assertIsDisplayed()
    }

    @Test
    fun dialogTest_isNotDismissed_whenClicked() {
        val textBeforeClick = "textBeforeClick"
        val textAfterClick = "textAfterClick"

        composeTestRule.setContent {
            val showDialog = state { true }
            val text = state { textBeforeClick }

            if (showDialog.value) {
                Dialog(onCloseRequest = {
                    showDialog.value = false
                }) {
                    Text(
                        text = text.value,
                        modifier = Modifier.clickable {
                            text.value = textAfterClick
                        }
                    )
                }
            }
        }

        findByText(textBeforeClick)
            .assertIsDisplayed()
            // Click inside the dialog
            .doClick()

        // Check that the Clickable was pressed and that the Dialog is still visible, but with
        // the new text
        findByText(textBeforeClick).assertDoesNotExist()
        findByText(textAfterClick).assertIsDisplayed()
    }

    @Test
    fun dialogTest_isDismissed_whenSpecified() {
        composeTestRule.setContent {
            val showDialog = state { true }

            if (showDialog.value) {
                Dialog(onCloseRequest = { showDialog.value = false }) {
                    Text(defaultText)
                }
            }
        }

        findByText(defaultText).assertIsDisplayed()

        // Click outside the dialog to dismiss it
        val outsideX = 0
        val outsideY = composeTestRule.displayMetrics.heightPixels / 2
        UiDevice.getInstance(getInstrumentation()).click(outsideX, outsideY)

        findByText(defaultText).assertDoesNotExist()
    }

    @Test
    fun dialogTest_isNotDismissed_whenNotSpecified() {
        composeTestRule.setContent {
            val showDialog = state { true }

            if (showDialog.value) {
                Dialog(onCloseRequest = {}) {
                    Text(defaultText)
                }
            }
        }

        findByText(defaultText).assertIsDisplayed()

        // Click outside the dialog to try to dismiss it
        val outsideX = 0
        val outsideY = composeTestRule.displayMetrics.heightPixels / 2
        UiDevice.getInstance(getInstrumentation()).click(outsideX, outsideY)

        // The Dialog should still be visible
        findByText(defaultText).assertIsDisplayed()
    }

    @Test
    fun dialogTest_isDismissed_whenSpecified_backButtonPressed() {
        composeTestRule.setContent {
            val showDialog = state { true }

            if (showDialog.value) {
                Dialog(onCloseRequest = { showDialog.value = false }) {
                    Text(defaultText)
                }
            }
        }

        findByText(defaultText).assertIsDisplayed()

        // Click the back button to dismiss the Dialog
        UiDevice.getInstance(getInstrumentation()).pressBack()

        findByText(defaultText).assertDoesNotExist()
    }

    // TODO(pavlis): Espresso loses focus on the dialog after back press. That makes the
    // subsequent query to fails.
    @Ignore
    @Test
    fun dialogTest_isNotDismissed_whenNotSpecified_backButtonPressed() {
        composeTestRule.setContent {
            val showDialog = state { true }

            if (showDialog.value) {
                Dialog(onCloseRequest = {}) {
                    Text(defaultText)
                }
            }
        }

        findByText(defaultText).assertIsDisplayed()

        // Click the back button to try to dismiss the dialog
        UiDevice.getInstance(getInstrumentation()).pressBack()

        // The Dialog should still be visible
        findByText(defaultText).assertIsDisplayed()
    }
}