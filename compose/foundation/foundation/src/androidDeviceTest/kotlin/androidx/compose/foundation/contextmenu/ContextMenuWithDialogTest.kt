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

package androidx.compose.foundation.contextmenu

import androidx.compose.foundation.WindowTestActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuDropdownProvider
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuToolbarProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuDataProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuProvider
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.selection.containsInclusive
import androidx.compose.foundation.text.selection.gestures.util.doubleTap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Dialog
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

/**
 * A test case where the TextField is not in the same LayoutNode tree as the context menu. This can
 * only happen when user provides a customized context menu.
 */
class ContextMenuWithTextFieldInDialogTest {

    @get:Rule val rule = createAndroidComposeRule<WindowTestActivity>()

    private val TAG = "textField"

    @Test
    fun textField1_showToolbar_doesNotCrash() {
        rule.setContent { TextField1TestCase() }
        // Trigger the Context Menu (Selection Toolbar) inside the Dialog.
        rule.onNodeWithTag(TAG).performTouchInput { doubleTap(center) }

        // Wait for the UI to settle. If the app crashes, the test fails here.
        rule.waitForIdle()
    }

    @Test
    fun textField1_showContextMenu_doesNotCrash() {
        rule.setContent { TextField1TestCase() }
        // Trigger the Context Menu (Selection Toolbar) inside the Dialog.
        rule.onNodeWithTag(TAG).performMouseInput { rightClick(center) }

        // Wait for the UI to settle. If the app crashes, the test fails here.
        rule.waitForIdle()
    }

    @Test
    fun textField2_showToolbar_doesNotCrash() {
        rule.setContent { TextField2TestCase() }
        // Trigger the Context Menu (Selection Toolbar) inside the Dialog.
        rule.onNodeWithTag(TAG).performTouchInput { doubleTap(center) }

        // Wait for the UI to settle. If the app crashes, the test fails here.
        rule.waitForIdle()
    }

    @Test
    fun textField2_showContextMenu_doesNotCrash() {
        rule.setContent { TextField2TestCase() }
        // Trigger the Context Menu (Selection Toolbar) inside the Dialog.
        rule.onNodeWithTag(TAG).performMouseInput { rightClick(center) }

        // Wait for the UI to settle. If the app crashes, the test fails here.
        rule.waitForIdle()
    }

    @Composable
    fun TextField1TestCase() {
        var dialogText by remember { mutableStateOf(TextFieldValue("Dialog TextField")) }
        var textFieldCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

        CustomizedContextMenu(getTextFieldCoordinates = { textFieldCoordinates }) {
            Dialog(onDismissRequest = {}) {
                BasicTextField(
                    value = dialogText,
                    onValueChange = { dialogText = it },
                    modifier =
                        Modifier.testTag(TAG).onGloballyPositioned { textFieldCoordinates = it },
                )
            }
        }
    }

    @Composable
    fun TextField2TestCase() {
        val dialogText = rememberTextFieldState("Dialog TextField")
        var textFieldCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

        CustomizedContextMenu(getTextFieldCoordinates = { textFieldCoordinates }) {
            Dialog(onDismissRequest = {}) {
                BasicTextField(
                    state = dialogText,
                    modifier =
                        Modifier.testTag(TAG).onGloballyPositioned { textFieldCoordinates = it },
                )
            }
        }
    }

    @Composable
    fun CustomizedContextMenu(
        getTextFieldCoordinates: () -> LayoutCoordinates?,
        content: @Composable () -> Unit,
    ) {
        var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
        val textContextMenuProvider =
            object : TextContextMenuProvider {
                override suspend fun showTextContextMenu(
                    dataProvider: TextContextMenuDataProvider
                ) {
                    val coordinates = coordinates ?: return
                    val menuPosition = dataProvider.position(coordinates)
                    val contentBounds = dataProvider.contentBounds(coordinates)
                    val menuPositionOnScreen = coordinates.localToScreen(menuPosition)
                    val contentBoundsOnScreen =
                        Rect(
                            coordinates.localToScreen(contentBounds.topLeft),
                            coordinates.size.toSize(),
                        )

                    val textFieldCoordinates = getTextFieldCoordinates() ?: return
                    val textFieldScreenBounds =
                        Rect(
                            textFieldCoordinates.localToScreen(Offset.Zero),
                            textFieldCoordinates.size.toSize(),
                        )

                    assertThat(textFieldScreenBounds.contains(menuPositionOnScreen)).isTrue()
                    assertThat(
                            textFieldScreenBounds.containsInclusive(contentBoundsOnScreen.topLeft)
                        )
                        .isTrue()
                    assertThat(
                            textFieldScreenBounds.containsInclusive(
                                contentBoundsOnScreen.bottomRight
                            )
                        )
                        .isTrue()
                }
            }

        CompositionLocalProvider(
            LocalTextContextMenuDropdownProvider provides textContextMenuProvider,
            LocalTextContextMenuToolbarProvider provides textContextMenuProvider,
        ) {
            Box(modifier = Modifier.onGloballyPositioned { coordinates = it }) { content() }
        }
    }
}
