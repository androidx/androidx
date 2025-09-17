/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.text.input

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.internal.readText
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.FocusedWindowTest
import androidx.compose.foundation.text.Handle
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.internal.selection.FakeClipboard
import androidx.compose.foundation.text.selection.fetchTextLayoutResult
import androidx.compose.foundation.text.selection.isSelectionHandle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.expectError
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentDataType
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsMatcher.Companion.expectValue
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isEditable
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.isNotFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.intl.Locale
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
class BasicTextFieldSemanticsTest : FocusedWindowTest {
    @get:Rule val rule = createComposeRule()

    private val Tag = "TextField"

    @Test
    fun defaultSemantics() {
        rule.setContent {
            BasicTextField(
                modifier = Modifier.testTag(Tag),
                state = remember { TextFieldState() },
                decorator = {
                    Column {
                        BasicText("label")
                        it()
                    }
                },
            )
        }

        rule
            .onNodeWithTag(Tag)
            .assertInputTextEquals("")
            .assertEditableTextEquals("")
            .assertTextEquals("label", includeEditableText = false)
            .assert(isEditable())
            .assertHasClickAction()
            .assert(hasSetTextAction())
            .assert(hasImeAction(ImeAction.Default))
            .assert(isNotFocused())
            .assert(expectValue(SemanticsProperties.TextSelectionRange, TextRange.Zero))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.SetText))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.PasteText))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Password))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.SetSelection))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult))
            // All text elements should be automatically be autofillable and of "Text" data type
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDataType))
            .assert(expectValue(SemanticsProperties.ContentDataType, ContentDataType.Text))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnFillData))

        val textLayoutResults = mutableListOf<TextLayoutResult>()
        rule.onNodeWithTag(Tag).performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
            it(textLayoutResults)
        }
        assert(textLayoutResults.size == 1) { "TextLayoutResult is null" }
    }

    @Test
    fun semantics_enabledStatus() {
        var enabled by mutableStateOf(true)
        rule.setContent {
            val state = remember { TextFieldState() }
            BasicTextField(state = state, modifier = Modifier.testTag(Tag), enabled = enabled)
        }

        rule.onNodeWithTag(Tag).assert(isEnabled())

        enabled = false
        rule.waitForIdle()

        rule.onNodeWithTag(Tag).assert(isNotEnabled())
    }

    @Test
    fun semantics_setTextAction() {
        val state = TextFieldState()
        rule.setContent { BasicTextField(state = state, modifier = Modifier.testTag(Tag)) }

        rule.onNodeWithTag(Tag).assert(isNotFocused()).performTextReplacement("Hello")
        rule
            .onNodeWithTag(Tag)
            .assert(isFocused())
            .assertTextEquals("Hello")
            .assertEditableTextEquals("Hello")
            .assertInputTextEquals("Hello")

        assertThat(state.text.toString()).isEqualTo("Hello")
    }

    @Test
    fun semantics_setTextAction_inputTransformation() {
        val state = TextFieldState()
        rule.setContent {
            BasicTextField(
                state = state,
                modifier = Modifier.testTag(Tag),
                inputTransformation = InputTransformation.allCaps(Locale.current),
            )
        }

        rule.onNodeWithTag(Tag).performTextReplacement("abc")

        rule
            .onNodeWithTag(Tag)
            .assertTextEquals("ABC")
            .assertInputTextEquals("ABC")
            .assertEditableTextEquals("ABC")
    }

    @Test
    fun semantics_setTextAction_outputTransformation() {
        val state = TextFieldState()
        rule.setContent {
            BasicTextField(
                state = state,
                modifier = Modifier.testTag(Tag),
                outputTransformation = { append(" xyz") },
            )
        }

        rule.onNodeWithTag(Tag).performTextReplacement("abc")

        rule
            .onNodeWithTag(Tag)
            .assertTextEquals("abc xyz")
            .assertInputTextEquals("abc")
            .assertEditableTextEquals("abc xyz")
    }

    @Test
    fun semantics_setTextAction_inputAndOutputTransformation() {
        val state = TextFieldState()
        rule.setContent {
            BasicTextField(
                state = state,
                modifier = Modifier.testTag(Tag),
                inputTransformation = InputTransformation.allCaps(Locale.current),
                outputTransformation = { append(" XYZ") },
            )
        }

        rule.onNodeWithTag(Tag).performTextReplacement("abc")

        rule
            .onNodeWithTag(Tag)
            .assertTextEquals("ABC XYZ")
            .assertInputTextEquals("ABC")
            .assertEditableTextEquals("ABC XYZ")
    }

    @Test
    fun semantics_performSetTextAction_whenReadOnly() {
        val state = TextFieldState("", initialSelection = TextRange(1))
        rule.setContent {
            BasicTextField(state = state, modifier = Modifier.testTag(Tag), readOnly = true)
        }

        expectError<AssertionError>(expectedMessage = "Failed to perform text input.*") {
            rule.onNodeWithTag(Tag).performTextReplacement("hello")
        }

        assertThat(state.text.toString()).isEqualTo("")
    }

    @Test
    fun semantics_setTextAction_appliesFilter() {
        val state = TextFieldState()
        rule.setContent {
            BasicTextField(
                state = state,
                modifier = Modifier.testTag(Tag),
                inputTransformation = {
                    if (length > 1) {
                        val newText = asCharSequence().asSequence().joinToString("-")
                        replace(0, length, newText)
                    }
                },
            )
        }

        rule.onNodeWithTag(Tag).assert(isNotFocused()).performTextReplacement("Hello")
        rule.onNodeWithTag(Tag).assert(isFocused()).assertTextEquals("H-e-l-l-o")

        assertThat(state.text.toString()).isEqualTo("H-e-l-l-o")
    }

    @Test
    fun semantics_performTextInputAction() {
        val state = TextFieldState("Hello", initialSelection = TextRange(1))
        rule.setContent { BasicTextField(state = state, modifier = Modifier.testTag(Tag)) }

        rule.onNodeWithTag(Tag).assert(isNotFocused()).performTextInput("a")
        rule.onNodeWithTag(Tag).assert(isFocused()).assertTextEquals("Haello")

        assertThat(state.text.toString()).isEqualTo("Haello")
    }

    @Test
    fun semantics_performTextInputAction_whenReadOnly() {
        val state = TextFieldState("", initialSelection = TextRange(1))
        rule.setContent {
            BasicTextField(state = state, modifier = Modifier.testTag(Tag), readOnly = true)
        }

        expectError<AssertionError>(expectedMessage = "Failed to perform text input.*") {
            rule.onNodeWithTag(Tag).performTextInput("hello")
        }

        assertThat(state.text.toString()).isEqualTo("")
    }

    @Test
    fun semantics_performTextInputAction_appliesFilter() {
        val state = TextFieldState("Hello", initialSelection = TextRange(1))
        rule.setContent {
            BasicTextField(
                state = state,
                modifier = Modifier.testTag(Tag),
                inputTransformation = {
                    val newChange = asCharSequence().replace(Regex("a"), "")
                    replace(0, length, newChange)
                },
            )
        }

        rule.onNodeWithTag(Tag).assert(isNotFocused()).performTextInput("abc")
        rule.onNodeWithTag(Tag).assert(isFocused()).assertTextEquals("Hbcello")

        assertThat(state.text.toString()).isEqualTo("Hbcello")
    }

    @Test
    fun semantics_clickAction() {
        rule.setContent {
            val state = remember { TextFieldState() }
            BasicTextField(state = state, modifier = Modifier.testTag(Tag))
        }

        rule
            .onNodeWithTag(Tag)
            .assert(isNotFocused())
            .performSemanticsAction(SemanticsActions.OnClick)
        rule.onNodeWithTag(Tag).assert(isFocused())
    }

    @Test
    fun semantics_imeOption() {
        rule.setContent {
            val state = remember { TextFieldState() }
            BasicTextField(
                state = state,
                modifier = Modifier.testTag(Tag),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )
        }

        rule.onNodeWithTag(Tag).assert(hasImeAction(ImeAction.Search))
    }

    @Test
    fun contentSemanticsAreSet_inTheFirstComposition() {
        val state = TextFieldState("hello")
        rule.setContent { BasicTextField(state = state, modifier = Modifier.testTag(Tag)) }

        rule.onNodeWithTag(Tag).assertTextEquals("hello")
    }

    @Test
    fun contentSemanticsAreSet_afterRecomposition() {
        val state = TextFieldState("hello")
        rule.setContent { BasicTextField(state = state, modifier = Modifier.testTag(Tag)) }

        rule.onNodeWithTag(Tag).assertTextEquals("hello")

        state.setTextAndPlaceCursorAtEnd("hello2")

        rule.onNodeWithTag(Tag).assertTextEquals("hello2")
    }

    @Test
    fun selectionSemanticsAreSet_inTheFirstComposition() {
        val state = TextFieldState("hello", initialSelection = TextRange(2))
        rule.setContent { BasicTextField(state = state, modifier = Modifier.testTag(Tag)) }

        with(rule.onNodeWithTag(Tag)) {
            assertTextEquals("hello")
            assertSelection(TextRange(2))
        }
    }

    @Test
    fun selectionSemanticsAreSet_afterRecomposition() {
        val state = TextFieldState("hello", initialSelection = TextRange.Zero)
        rule.setContent { BasicTextField(state = state, modifier = Modifier.testTag(Tag)) }

        with(rule.onNodeWithTag(Tag)) {
            assertTextEquals("hello")
            assertSelection(TextRange.Zero)
        }

        state.edit { selection = TextRange(2) }

        with(rule.onNodeWithTag(Tag)) {
            assertTextEquals("hello")
            assertSelection(TextRange(2))
        }
    }

    @Test
    fun inputSelection_changesSelectionState() {
        val state = TextFieldState("hello")
        rule.setTextFieldTestContent {
            BasicTextField(state = state, modifier = Modifier.testTag(Tag))
        }

        rule.onNodeWithTag(Tag).performTextInputSelection(TextRange(2, 3))

        rule.onNode(isSelectionHandle(Handle.SelectionStart)).assertIsDisplayed()
        rule.onNode(isSelectionHandle(Handle.SelectionEnd)).assertIsDisplayed()

        rule.runOnIdle { assertThat(state.selection).isEqualTo(TextRange(2, 3)) }
    }

    @Test
    fun inputSelection_changesSelectionState_appliesFilter() {
        val state = TextFieldState("hello", initialSelection = TextRange(5))
        rule.setContent {
            BasicTextField(
                state = state,
                modifier = Modifier.testTag(Tag),
                inputTransformation = { revertAllChanges() },
            )
        }

        rule.onNodeWithTag(Tag).performTextInputSelection(TextRange(2))

        rule.runOnIdle { assertThat(state.selection).isEqualTo(TextRange(5)) }
    }

    @Test
    fun textLayoutResultSemanticsAreSet_inTheFirstComposition() {
        val state = TextFieldState("hello")
        rule.setContent { BasicTextField(state = state, modifier = Modifier.testTag(Tag)) }

        rule.onNodeWithTag(Tag).assertTextEquals("hello")
        assertThat(rule.onNodeWithTag(Tag).fetchTextLayoutResult().layoutInput.text.text)
            .isEqualTo("hello")
    }

    @Test
    fun textLayoutResultSemanticsAreUpdated_afterRecomposition() {
        val state = TextFieldState()
        rule.setContent { BasicTextField(state = state, modifier = Modifier.testTag(Tag)) }

        rule.onNodeWithTag(Tag).assertTextEquals("")
        rule.onNodeWithTag(Tag).performTextInput("hello")
        assertThat(rule.onNodeWithTag(Tag).fetchTextLayoutResult().layoutInput.text.text)
            .isEqualTo("hello")
    }

    @Test
    fun semanticsAreSet_afterStateObjectChanges() {
        val state1 = TextFieldState("hello", initialSelection = TextRange.Zero)
        val state2 = TextFieldState("world", initialSelection = TextRange(2))
        var chosenState by mutableStateOf(true)
        rule.setContent {
            BasicTextField(
                state = if (chosenState) state1 else state2,
                modifier = Modifier.testTag(Tag),
            )
        }

        with(rule.onNodeWithTag(Tag)) {
            assertTextEquals("hello")
            assertSelection(TextRange.Zero)
        }

        chosenState = false

        with(rule.onNodeWithTag(Tag)) {
            assertTextEquals("world")
            assertSelection(TextRange(2))
        }
    }

    @Test
    fun semantics_paste_notAvailable_whenDisabledOrReadOnly() {
        val state = TextFieldState("World!", initialSelection = TextRange(0))
        var enabled by mutableStateOf(false)
        var readOnly by mutableStateOf(false)
        rule.setContent {
            BasicTextField(
                state = state,
                modifier = Modifier.testTag(Tag),
                enabled = enabled,
                readOnly = readOnly,
            )
        }

        rule.onNodeWithTag(Tag).assert(SemanticsMatcher.keyNotDefined(SemanticsActions.PasteText))

        enabled = true
        readOnly = true

        rule.waitForIdle()

        rule.onNodeWithTag(Tag).assert(SemanticsMatcher.keyNotDefined(SemanticsActions.PasteText))

        enabled = true
        readOnly = false

        rule.waitForIdle()

        rule.onNodeWithTag(Tag).assert(SemanticsMatcher.keyIsDefined(SemanticsActions.PasteText))
    }

    @Test
    fun semantics_paste() {
        val state = TextFieldState("Here World!")
        val clipboard = FakeClipboard("Hello")
        rule.setContent {
            CompositionLocalProvider(LocalClipboard provides clipboard) {
                BasicTextField(state = state, modifier = Modifier.testTag(Tag))
            }
        }

        rule.onNodeWithTag(Tag).performTextInputSelection(TextRange(0, 4))
        rule.onNodeWithTag(Tag).performSemanticsAction(SemanticsActions.PasteText)

        rule.runOnIdle {
            assertThat(state.selection).isEqualTo(TextRange(5))
            assertThat(state.text.toString()).isEqualTo("Hello World!")
        }
    }

    @Test
    fun semantics_paste_appliesFilter() {
        val state = TextFieldState("Here World!")
        val clipboard = FakeClipboard("Hello")
        rule.setContent {
            CompositionLocalProvider(LocalClipboard provides clipboard) {
                BasicTextField(
                    state = state,
                    modifier = Modifier.testTag(Tag),
                    inputTransformation = {
                        // remove all 'l' characters
                        if (changes.changeCount != 0) {
                            val newChange = asCharSequence().replace(Regex("l"), "")
                            replace(0, length, newChange)
                            placeCursorAtEnd()
                        }
                    },
                )
            }
        }

        rule.onNodeWithTag(Tag).performTextInputSelection(TextRange(0, 4))
        rule.onNodeWithTag(Tag).performSemanticsAction(SemanticsActions.PasteText)

        rule.runOnIdle {
            assertThat(state.selection).isEqualTo(TextRange(9))
            assertThat(state.text.toString()).isEqualTo("Heo Word!")
        }
    }

    @Test
    fun semantics_copy() = runTest {
        val state = TextFieldState("Hello World!")
        val clipboard = FakeClipboard()
        rule.setContent {
            CompositionLocalProvider(LocalClipboard provides clipboard) {
                BasicTextField(state = state, modifier = Modifier.testTag(Tag))
            }
        }

        rule.onNodeWithTag(Tag).performTextInputSelection(TextRange(0, 5))
        rule.onNodeWithTag(Tag).performSemanticsAction(SemanticsActions.CopyText)

        rule.waitForIdle()
        assertThat(clipboard.getClipEntry()?.readText()).isEqualTo("Hello")
    }

    @Test
    fun semantics_copy_disabled_whenSelectionCollapsed() {
        val state = TextFieldState("Hello World!")
        rule.setContent { BasicTextField(state = state, modifier = Modifier.testTag(Tag)) }

        rule.onNodeWithTag(Tag).assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CopyText))
    }

    @Test
    fun semantics_copy_appliesFilter() {
        val state = TextFieldState("Hello World!", initialSelection = TextRange(0, 5))
        val clipboard = FakeClipboard()
        rule.setContent {
            CompositionLocalProvider(LocalClipboard provides clipboard) {
                BasicTextField(
                    state = state,
                    modifier = Modifier.testTag(Tag),
                    inputTransformation = {
                        // reject copy action collapsing the selection
                        if (selection != originalValue.selection) {
                            revertAllChanges()
                        }
                    },
                )
            }
        }

        rule.onNodeWithTag(Tag).performSemanticsAction(SemanticsActions.CopyText)

        rule.runOnIdle { assertThat(state.selection).isEqualTo(TextRange(0, 5)) }
    }

    @Test
    fun semantics_cut() = runTest {
        val state = TextFieldState("Hello World!", initialSelection = TextRange(0, 5))
        val clipboard = FakeClipboard()
        rule.setContent {
            CompositionLocalProvider(LocalClipboard provides clipboard) {
                BasicTextField(state = state, modifier = Modifier.testTag(Tag))
            }
        }

        rule.onNodeWithTag(Tag).performSemanticsAction(SemanticsActions.CutText)

        rule.waitForIdle()
        assertThat(state.text.toString()).isEqualTo(" World!")
        assertThat(state.selection).isEqualTo(TextRange(0))
        assertThat(clipboard.getClipEntry()?.readText()).isEqualTo("Hello")
    }

    @Test
    fun semantics_cut_appliesFilter() = runTest {
        val state = TextFieldState("Hello World!", initialSelection = TextRange(0, 5))
        val clipboard = FakeClipboard()
        rule.setContent {
            CompositionLocalProvider(LocalClipboard provides clipboard) {
                BasicTextField(
                    state = state,
                    modifier = Modifier.testTag(Tag),
                    inputTransformation = { revertAllChanges() },
                )
            }
        }

        rule.onNodeWithTag(Tag).performSemanticsAction(SemanticsActions.CutText)

        rule.waitForIdle()
        assertThat(state.text.toString()).isEqualTo("Hello World!")
        assertThat(state.selection).isEqualTo(TextRange(0, 5))
        assertThat(clipboard.getClipEntry()?.readText()).isEqualTo("Hello")
    }

    @Test
    fun semantics_cut_notAvailable_whenDisabledOrReadOnly() {
        val state = TextFieldState("World!", initialSelection = TextRange(0, 1))
        var enabled by mutableStateOf(false)
        var readOnly by mutableStateOf(false)
        rule.setContent {
            BasicTextField(
                state = state,
                modifier = Modifier.testTag(Tag),
                enabled = enabled,
                readOnly = readOnly,
            )
        }

        rule.onNodeWithTag(Tag).assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CutText))

        enabled = true
        readOnly = true

        rule.waitForIdle()

        rule.onNodeWithTag(Tag).assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CutText))

        enabled = true
        readOnly = false

        rule.waitForIdle()

        rule.onNodeWithTag(Tag).assert(SemanticsMatcher.keyIsDefined(SemanticsActions.CutText))
    }

    @Test
    fun semantics_isNotEditable_whenDisabledOrReadOnly() {
        val state = TextFieldState()
        var enabled by mutableStateOf(true)
        var readOnly by mutableStateOf(false)
        rule.setContent {
            BasicTextField(
                state = state,
                modifier = Modifier.testTag(Tag),
                enabled = enabled,
                readOnly = readOnly,
            )
        }
        rule.onNodeWithTag(Tag).assert(isEditable())

        enabled = true
        readOnly = true
        rule.onNodeWithTag(Tag).assert(expectValue(SemanticsProperties.IsEditable, false))

        enabled = false
        readOnly = false
        rule.onNodeWithTag(Tag).assert(expectValue(SemanticsProperties.IsEditable, false))

        enabled = false
        readOnly = true
        rule.onNodeWithTag(Tag).assert(expectValue(SemanticsProperties.IsEditable, false))

        // Make editable again.
        enabled = true
        readOnly = false
        rule.onNodeWithTag(Tag).assert(isEditable())
    }

    @Test
    fun inputTransformationSemantics_areApplied() {
        val state = TextFieldState()
        val semanticsPropertyKey = SemanticsPropertyKey<Int>("InputTransformation")
        val transformation =
            object : InputTransformation {
                override fun SemanticsPropertyReceiver.applySemantics() {
                    this[semanticsPropertyKey] = 2
                }

                override fun TextFieldBuffer.transformInput() = Unit
            }
        rule.setContent {
            BasicTextField(
                state = state,
                modifier = Modifier.testTag(Tag),
                inputTransformation = transformation,
            )
        }
        rule.onNodeWithTag(Tag).assertKey(2, semanticsPropertyKey)
    }

    @Test
    fun inputTransformationSemantics_areApplied_stateBacked() {
        val state = TextFieldState()
        var number by mutableIntStateOf(1)
        val semanticsPropertyKey = SemanticsPropertyKey<Int>("InputTransformation")
        val transformation =
            object : InputTransformation {
                override fun SemanticsPropertyReceiver.applySemantics() {
                    this[semanticsPropertyKey] = number
                }

                override fun TextFieldBuffer.transformInput() = Unit
            }
        rule.setContent {
            BasicTextField(
                state = state,
                modifier = Modifier.testTag(Tag),
                inputTransformation = transformation,
            )
        }
        rule.onNodeWithTag(Tag).assertKey(1, semanticsPropertyKey)
        number = 2
        rule.onNodeWithTag(Tag).assertKey(2, semanticsPropertyKey)
    }

    @Test
    fun maxLengthInputTransformationSemantics() {
        val state = TextFieldState()

        rule.setContent {
            BasicTextField(
                state = state,
                modifier = Modifier.testTag(Tag),
                inputTransformation = InputTransformation.maxLength(10),
            )
        }
        rule.onNodeWithTag(Tag).assertKey(10, SemanticsProperties.MaxTextLength)
    }

    @Test
    fun passwordSemantics_whenIsPassword_isToggled() {
        var isPassword by mutableStateOf(false)
        rule.setContent {
            BasicTextField(
                state =
                    rememberTextFieldState(
                        initialText = "Hello",
                        initialSelection = TextRange(0, 2),
                    ),
                modifier = Modifier.testTag(Tag),
                isPassword = isPassword,
            )
        }

        rule.onNodeWithTag(Tag).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Password))
        rule.onNodeWithTag(Tag).assert(SemanticsMatcher.keyIsDefined(SemanticsActions.CopyText))
        rule.onNodeWithTag(Tag).assert(SemanticsMatcher.keyIsDefined(SemanticsActions.CutText))

        isPassword = true

        rule.onNodeWithTag(Tag).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
        rule.onNodeWithTag(Tag).assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CopyText))
        rule.onNodeWithTag(Tag).assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CutText))

        isPassword = false

        rule.onNodeWithTag(Tag).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Password))
        rule.onNodeWithTag(Tag).assert(SemanticsMatcher.keyIsDefined(SemanticsActions.CopyText))
        rule.onNodeWithTag(Tag).assert(SemanticsMatcher.keyIsDefined(SemanticsActions.CutText))
    }

    private fun SemanticsNodeInteraction.assertKey(expected: Int, key: SemanticsPropertyKey<Int>) {
        assertThat(fetchSemanticsNode().config.getOrNull(key)).isEqualTo(expected)
    }

    private fun SemanticsNodeInteraction.assertEditableTextEquals(
        value: String
    ): SemanticsNodeInteraction =
        assert(
            SemanticsMatcher("${SemanticsProperties.EditableText.name} = '$value'") {
                it.config.getOrNull(SemanticsProperties.EditableText)?.text.equals(value)
            }
        )

    private fun SemanticsNodeInteraction.assertInputTextEquals(
        value: String
    ): SemanticsNodeInteraction =
        assert(
            SemanticsMatcher("${SemanticsProperties.InputText.name} = '$value'") {
                it.config.getOrNull(SemanticsProperties.InputText)?.text.equals(value)
            }
        )
}

internal fun SemanticsNodeInteraction.assertSelection(expected: TextRange) {
    val selection = fetchSemanticsNode().config.getOrNull(SemanticsProperties.TextSelectionRange)
    assertThat(selection).isEqualTo(expected)
}
