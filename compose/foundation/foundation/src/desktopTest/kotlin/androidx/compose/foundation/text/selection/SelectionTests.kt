/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.DesktopPlatform
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyMapping
import androidx.compose.foundation.text.createPlatformDefaultKeyMapping
import androidx.compose.foundation.text.overriddenDefaultKeyMapping
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.google.common.truth.Truth
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test


class SelectionTests {

    @get:Rule
    val rule = createComposeRule()

    @After
    fun restoreRealDesktopPlatform() {
        overriddenDefaultKeyMapping = null
    }

    private fun setPlatformDefaultKeyMapping(value: KeyMapping) {
        overriddenDefaultKeyMapping = value
    }

    suspend fun SemanticsNodeInteraction.waitAndCheck(check: () -> Unit): SemanticsNodeInteraction {
        rule.awaitIdle()
        check()
        return this
    }

    @OptIn(ExperimentalTestApi::class)
    private fun DesktopPlatform.textFieldSemanticInteraction(initialValue: String = "", semanticNodeContext: suspend SemanticsNodeInteraction.(state: MutableState<TextFieldValue>) -> SemanticsNodeInteraction) =
        runBlocking {
            setPlatformDefaultKeyMapping(createPlatformDefaultKeyMapping(this@textFieldSemanticInteraction))
            val state = mutableStateOf(TextFieldValue(initialValue))

            rule.setContent {
                BasicTextField(
                    value = state.value,
                    onValueChange = { state.value = it },
                    modifier = Modifier.testTag("textField")
                )
            }
            rule.awaitIdle()
            val textField = rule.onNodeWithTag("textField")
            textField.performMouseInput {
                click(Offset(0f, 0f))
            }

            rule.awaitIdle()
            textField.assertIsFocused()

            Truth.assertThat(state.value.selection).isEqualTo(TextRange(0, 0))

            semanticNodeContext.invoke(textField, state)
        }


    @OptIn(ExperimentalTestApi::class)
    private fun DesktopPlatform.selectLineStart(keyboardInteraction: KeyInjectionScope.() -> Unit) {
        textFieldSemanticInteraction("line 1\nline 2\nline 3\nline 4\nline 5") { state ->
            performKeyInput {
                pressKey(Key.DirectionRight)
                pressKey(Key.DirectionDown)
            }
            .waitAndCheck {
                Truth.assertThat(state.value.selection).isEqualTo(TextRange(8, 8))
            }
            .performKeyInput(keyboardInteraction)
            .waitAndCheck {
                Truth.assertThat(state.value.selection).isEqualTo(TextRange(8, 7))
            }
        }
    }

    @OptIn(ExperimentalTestApi::class)
    private fun DesktopPlatform.selectTextStart(keyboardInteraction: KeyInjectionScope.() -> Unit) {
        textFieldSemanticInteraction("line 1\nline 2\nline 3\nline 4\nline 5") { state ->
            performKeyInput {
                pressKey(Key.DirectionRight)
                pressKey(Key.DirectionDown)
            }.waitAndCheck {
                Truth.assertThat(state.value.selection).isEqualTo(TextRange(8, 8))
            }
            performKeyInput(keyboardInteraction)
            .waitAndCheck { Truth.assertThat(state.value.selection).isEqualTo(TextRange(8, 0)) }
        }
    }

    @OptIn(ExperimentalTestApi::class)
    private fun DesktopPlatform.selectTextEnd(keyboardInteraction: KeyInjectionScope.() -> Unit) {
        textFieldSemanticInteraction("line 1\nline 2\nline 3\nline 4\nline 5") { state ->
            performKeyInput {
                pressKey(Key.DirectionRight)
                pressKey(Key.DirectionDown)
            }
            .waitAndCheck {
                Truth.assertThat(state.value.selection).isEqualTo(TextRange(8, 8))
            }
            .performKeyInput(keyboardInteraction)
            .waitAndCheck {
                Truth.assertThat(state.value.selection).isEqualTo(TextRange(8, 34))
             }
        }
    }
    @OptIn(ExperimentalTestApi::class)
    private fun DesktopPlatform.selectLineEnd(keyboardInteraction: KeyInjectionScope.() -> Unit) {
        textFieldSemanticInteraction("line 1\nline 2\nline 3\nline 4\nline 5") { state ->
            performKeyInput {
                pressKey(Key.DirectionRight)
                pressKey(Key.DirectionDown)
            }.waitAndCheck {
                Truth.assertThat(state.value.selection).isEqualTo(TextRange(8, 8))
            }
            .performKeyInput(keyboardInteraction)
            .waitAndCheck {
                Truth.assertThat(state.value.selection).isEqualTo(TextRange(8, 13))
            }
        }
    }

    @Test
    fun `Select till line start with DesktopPlatform-Windows`() = runBlocking {
        DesktopPlatform.Windows.selectLineStart {
            keyDown(Key.ShiftLeft)
            pressKey(Key.MoveHome)
            keyUp(Key.ShiftLeft)
        }
    }

    @Test
    fun `Select till text start with DesktopPlatform-Windows`() = runBlocking {
        DesktopPlatform.Windows.selectTextStart {
            keyDown(Key.CtrlLeft)
            keyDown(Key.ShiftLeft)
            pressKey(Key.MoveHome)
            keyUp(Key.ShiftLeft)
            keyUp(Key.CtrlLeft)
        }
    }

    @Test
    fun `Select till line end with DesktopPlatform-Windows`() = runBlocking {
        DesktopPlatform.Windows.selectLineEnd {
            keyDown(Key.ShiftLeft)
            pressKey(Key.MoveEnd)
            keyUp(Key.ShiftLeft)
        }
    }

    @Test
    fun `Select till text end with DesktopPlatform-Windows`() = runBlocking {
        DesktopPlatform.Windows.selectTextEnd {
            keyDown(Key.CtrlLeft)
            keyDown(Key.ShiftLeft)
            pressKey(Key.MoveEnd)
            keyUp(Key.ShiftLeft)
            keyUp(Key.CtrlLeft)
        }
    }


    @Test
    fun `Select till line start with DesktopPlatform-MacOs`() = runBlocking {
        DesktopPlatform.MacOS.selectLineStart() {
            keyDown(Key.ShiftLeft)
            keyDown(Key.MetaLeft)
            pressKey(Key.DirectionLeft)
            keyUp(Key.ShiftLeft)
            keyUp(Key.MetaLeft)
        }
    }

    @Test
    fun `Select till text start with DesktopPlatform-MacOs`() = runBlocking {
        DesktopPlatform.MacOS.selectTextStart {
            keyDown(Key.ShiftLeft)
            pressKey(Key.Home)
            keyUp(Key.ShiftLeft)
        }
    }

    @Test
    fun `Select till line end with DesktopPlatform-Macos`() = runBlocking {
        DesktopPlatform.MacOS.selectLineEnd {
            keyDown(Key.ShiftLeft)
            keyDown(Key.MetaLeft)
            pressKey(Key.DirectionRight)
            keyUp(Key.ShiftLeft)
            keyUp(Key.MetaLeft)
        }
    }

    @Test
    fun `Select till text end with DesktopPlatform-Macos`() = runBlocking {
        DesktopPlatform.MacOS.selectTextEnd {
            keyDown(Key.ShiftLeft)
            pressKey(Key.MoveEnd)
            keyUp(Key.ShiftLeft)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    private fun DesktopPlatform.deleteAllFromKeyBoard(
        initialText: String, deleteAllInteraction: KeyInjectionScope.() -> Unit
    ) {
        textFieldSemanticInteraction(initialText) { state ->
            performKeyInput(deleteAllInteraction).waitAndCheck { Truth.assertThat(state.value.text).isEqualTo("") }
        }
    }


    @Test
    fun `Delete backwards on an empty line with DesktopPlatform-Windows`() {
        DesktopPlatform.Windows.deleteAllFromKeyBoard("") {
            keyDown(Key.CtrlLeft)
            keyDown(Key.Backspace)
        }
    }

    @Test
    fun `Delete backwards on an empty line with DesktopPlatform-Macos`() {
        DesktopPlatform.MacOS.deleteAllFromKeyBoard("") {
            keyDown(Key.MetaLeft)
            keyDown(Key.Delete)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    private fun DesktopPlatform.selectAllTest(selectAllInteraction: KeyInjectionScope.() -> Unit) {
        textFieldSemanticInteraction("Select this text") { state ->
            performKeyInput(selectAllInteraction)
            .waitAndCheck {
                Truth.assertThat(state.value.selection).isEqualTo(TextRange(0, 16))
            }
            .performKeyInput { keyDown(Key.Delete) }
            .waitAndCheck {
                Truth.assertThat(state.value.selection).isEqualTo(TextRange(0, 0))
                Truth.assertThat(state.value.text).isEqualTo("")
            }
        }
    }

    @Test
    fun `Select all with DesktopPlatform-Windows`() = runBlocking {
        DesktopPlatform.Windows.selectAllTest {
            keyDown(Key.CtrlLeft)
            pressKey(Key.A)
            keyUp(Key.CtrlLeft)
        }
    }

    @Test
    fun `Select all with DesktopPlatform-Macos`() = runBlocking {
        DesktopPlatform.MacOS.selectAllTest {
            keyDown(Key.MetaLeft)
            pressKey(Key.A)
            keyUp(Key.MetaLeft)
        }
    }
}
