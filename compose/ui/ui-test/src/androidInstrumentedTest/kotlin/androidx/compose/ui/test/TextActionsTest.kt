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

package androidx.compose.ui.test

import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.testutils.expectError
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.insertTextAtCursor
import androidx.compose.ui.semantics.isEditable
import androidx.compose.ui.semantics.onImeAction
import androidx.compose.ui.semantics.requestFocus
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.util.BoundaryNode
import androidx.compose.ui.test.util.expectErrorMessageStartsWith
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class TextActionsTest {

    private val fieldTag = "Field"

    @get:Rule val rule = createComposeRule()

    @Composable
    fun TextFieldUi(
        imeAction: ImeAction = ImeAction.Default,
        keyboardActions: KeyboardActions = KeyboardActions.Default,
        enabled: Boolean = true,
        readOnly: Boolean = false,
        textCallback: (String) -> Unit = {}
    ) {
        val state = remember { mutableStateOf("") }
        BasicTextField(
            modifier = Modifier.testTag(fieldTag).border(0.dp, Color.Black),
            value = state.value,
            keyboardOptions = KeyboardOptions(imeAction = imeAction),
            keyboardActions = keyboardActions,
            enabled = enabled,
            readOnly = readOnly,
            onValueChange = {
                state.value = it
                textCallback(it)
            }
        )
    }

    @Test
    fun sendText_requestFocusNotSupported_shouldFail() {
        rule.setContent {
            BoundaryNode(
                testTag = "node",
                Modifier.semantics {
                    isEditable = true
                    setText { true }
                }
            )
        }

        expectErrorMessageStartsWith(
            "Failed to perform text input.\n" +
                "Failed to assert the following: (RequestFocus is defined)\n" +
                "Semantics of the node:"
        ) {
            rule.onNodeWithTag("node").performTextInput("hello")
        }
    }

    @Test
    fun performTextInput_setTextNotSupported_shouldFail() {
        rule.setContent {
            BoundaryNode(
                fieldTag,
                Modifier.semantics {
                    isEditable = true
                    insertTextAtCursor { true }
                    requestFocus { true }
                }
            )
        }

        expectErrorMessageStartsWith(
            "Failed to perform text input.\n" +
                "Failed to assert the following: (SetText is defined)\n" +
                "Semantics of the node:"
        ) {
            rule.onNodeWithTag(fieldTag).performTextInput("")
        }
    }

    @Test
    fun performTextInput_insertTextAtCursorNotSupported_shouldFail() {
        rule.setContent {
            BoundaryNode(
                fieldTag,
                Modifier.semantics {
                    isEditable = true
                    setText { true }
                    requestFocus { true }
                }
            )
        }

        expectErrorMessageStartsWith(
            "Failed to perform text input.\n" +
                "Failed to assert the following: (InsertTextAtCursor is defined)\n" +
                "Semantics of the node:"
        ) {
            rule.onNodeWithTag(fieldTag).performTextInput("")
        }
    }

    @Test
    fun sendText_clearText() {
        var lastSeenText = ""
        rule.setContent { TextFieldUi { lastSeenText = it } }

        rule.onNodeWithTag(fieldTag).performTextInput("Hello!")

        rule.runOnIdle { assertThat(lastSeenText).isEqualTo("Hello!") }

        rule.onNodeWithTag(fieldTag).performTextClearance()

        rule.runOnIdle { assertThat(lastSeenText).isEqualTo("") }
    }

    @Test
    fun sendTextRepeatedly_shouldAppend() {
        var lastSeenText = ""
        rule.setContent { TextFieldUi { lastSeenText = it } }

        rule.onNodeWithTag(fieldTag).performTextInput("Hello")

        // "Type" one character at a time.
        " world!".forEach { rule.onNodeWithTag(fieldTag).performTextInput(it.toString()) }

        rule.runOnIdle { assertThat(lastSeenText).isEqualTo("Hello world!") }
    }

    @Test
    fun sendText_whenDisabled_shouldFail() {
        rule.setContent { TextFieldUi(enabled = false) }

        expectErrorMessageStartsWith(
            "Failed to perform text input.\n" +
                "Failed to assert the following: (is enabled)\n" +
                "Semantics of the node:"
        ) {
            rule.onNodeWithTag(fieldTag).performTextInput("hi")
        }
    }

    @Test
    fun sendText_whenReadOnly_isNotAllowed() {
        var lastSeenText = ""
        rule.setContent { TextFieldUi(readOnly = true) }

        expectError<AssertionError> { rule.onNodeWithTag(fieldTag).performTextInput("hi") }
        rule.runOnIdle { assertThat(lastSeenText).isEqualTo("") }
    }

    @Test
    fun replaceText() {
        var lastSeenText = ""
        rule.setContent { TextFieldUi { lastSeenText = it } }

        rule.onNodeWithTag(fieldTag).performTextInput("Hello")

        rule.runOnIdle { assertThat(lastSeenText).isEqualTo("Hello") }

        rule.onNodeWithTag(fieldTag).performTextReplacement("world")

        rule.runOnIdle { assertThat(lastSeenText).isEqualTo("world") }
    }

    @Test
    fun performImeAction_search() {
        var actionPerformed = false
        rule.setContent {
            TextFieldUi(
                imeAction = ImeAction.Search,
                keyboardActions = KeyboardActions(onSearch = { actionPerformed = true })
            )
        }
        assertThat(actionPerformed).isFalse()

        rule.onNodeWithTag(fieldTag).performImeAction()

        rule.runOnIdle { assertThat(actionPerformed).isTrue() }
    }

    @Test
    fun performImeAction_actionNotDefined_shouldFail() {
        var actionPerformed = false
        rule.setContent {
            TextFieldUi(
                imeAction = ImeAction.Default,
                keyboardActions = KeyboardActions { actionPerformed = true }
            )
        }
        assertThat(actionPerformed).isFalse()

        expectErrorMessageStartsWith(
            "Failed to perform IME action.\n" +
                "Failed to assert the following: (NOT (ImeAction = 'Default'))\n" +
                "Semantics of the node:"
        ) {
            rule.onNodeWithTag(fieldTag).performImeAction()
        }
    }

    @Test
    fun performImeAction_actionReturnsFalse_shouldFail() {
        rule.setContent {
            BoundaryNode(
                fieldTag,
                Modifier.semantics {
                    isEditable = true
                    setText { true }
                    requestFocus { true }
                    insertTextAtCursor { true }
                    onImeAction(ImeAction.Done) { false }
                }
            )
        }

        expectErrorMessageStartsWith(
            "Failed to perform IME action, handler returned false.\n" + "Semantics of the node:"
        ) {
            rule.onNodeWithTag(fieldTag).performImeAction()
        }
    }

    @Test
    fun performImeAction_inputNotSupported_shouldFail() {
        rule.setContent { BoundaryNode(fieldTag) }

        expectErrorMessageStartsWith(
            "Failed to perform IME action.\n" +
                "Failed to assert the following: (PerformImeAction is defined)\n" +
                "Semantics of the node:"
        ) {
            rule.onNodeWithTag(fieldTag).performImeAction()
        }
    }

    @Test
    fun performImeAction_focusNotSupported_shouldFail() {
        rule.setContent {
            BoundaryNode(
                testTag = "node",
                Modifier.semantics {
                    isEditable = true
                    setText { true }
                    onImeAction(ImeAction.Done) { true }
                }
            )
        }

        expectErrorMessageStartsWith(
            "Failed to perform IME action.\n" +
                "Failed to assert the following: (RequestFocus is defined)\n" +
                "Semantics of the node:"
        ) {
            rule.onNodeWithTag("node").performImeAction()
        }
    }

    @Test
    fun performImeAction_whenDisabled_shouldFail() {
        rule.setContent { TextFieldUi(imeAction = ImeAction.Done, enabled = false) }

        expectErrorMessageStartsWith(
            "Failed to perform IME action.\n" +
                "Failed to assert the following: (is enabled)\n" +
                "Semantics of the node:"
        ) {
            rule.onNodeWithTag(fieldTag).performImeAction()
        }
    }

    @Test
    fun performImeAction_whenReadOnly_isAllowed() {
        var actionPerformed = false
        rule.setContent {
            TextFieldUi(
                imeAction = ImeAction.Done,
                readOnly = true,
                keyboardActions = KeyboardActions { actionPerformed = true }
            )
        }

        rule.onNodeWithTag(fieldTag).performImeAction()
        rule.runOnIdle { assertThat(actionPerformed).isTrue() }
    }

    @Composable
    fun Btf1Selection(
        enabled: Boolean = true,
        readOnly: Boolean = false,
        visualTransformation: VisualTransformation = VisualTransformation.None,
        textCallback: (TextRange) -> Unit = {}
    ) {
        val tfv = remember { mutableStateOf(TextFieldValue("text text text")) }
        val focusRequester = remember { FocusRequester() }
        BasicTextField(
            modifier =
                Modifier.testTag(fieldTag).border(0.dp, Color.Black).focusRequester(focusRequester),
            value = tfv.value,
            enabled = enabled,
            readOnly = readOnly,
            visualTransformation = visualTransformation,
            onValueChange = {
                tfv.value = it
                textCallback(it.selection)
            }
        )

        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }

    @Test
    fun btf1_performTextInputSelection_whenEditable_shouldCorrectlySetSelection() {
        var actualSelectionRange = TextRange.Zero
        rule.setContent { Btf1Selection { actualSelectionRange = it } }

        var expectedRange = TextRange(start = 5, end = 9)
        rule.onNodeWithTag(fieldTag).performTextInputSelection(expectedRange)

        rule.runOnIdle { assertThat(actualSelectionRange).isEqualTo(expectedRange) }
    }

    @Test
    fun btf1_performTextInputSelection_whenEditableAndReversed_shouldCorrectlySetSelection() {
        var actualSelectionRange = TextRange.Zero
        rule.setContent { Btf1Selection { actualSelectionRange = it } }

        rule.onNodeWithTag(fieldTag).performTextInputSelection(TextRange(start = 9, end = 5))

        var expectedRange = TextRange(start = 5, end = 9)
        rule.runOnIdle { assertThat(actualSelectionRange).isEqualTo(expectedRange) }
    }

    @Test
    fun btf1_performTextInputSelection_whenReadOnly_shouldCorrectlySetSelection() {
        var actualSelectionRange = TextRange.Zero
        rule.setContent { Btf1Selection(readOnly = true) { actualSelectionRange = it } }

        var expectedRange = TextRange(start = 5, end = 9)
        rule.onNodeWithTag(fieldTag).performTextInputSelection(expectedRange)

        rule.runOnIdle { assertThat(actualSelectionRange).isEqualTo(expectedRange) }
    }

    @Test
    fun btf1_performTextInputSelection_whenDisabled_shouldFail() {
        rule.setContent { Btf1Selection(enabled = false) }

        expectErrorMessageStartsWith(
            "Failed to perform text input selection.\n" +
                "Failed to assert the following: (is enabled)\n" +
                "Semantics of the node:"
        ) {
            rule.onNodeWithTag(fieldTag).performTextInputSelection(TextRange(start = 5, end = 9))
        }
    }

    @Test
    fun btf1_performTextInputSelection_invalidSelection_noChange() {
        var actualSelectionRange = TextRange.Zero
        rule.setContent { Btf1Selection { actualSelectionRange = it } }

        // BTF1 puts the cursor at the beginning of the text
        val expectedRange = TextRange.Zero
        rule.onNodeWithTag(fieldTag).performTextInputSelection(TextRange(0, Int.MAX_VALUE))
        rule.runOnIdle { assertThat(actualSelectionRange).isEqualTo(expectedRange) }
    }

    @Test
    fun btf1_performTextInputSelection_whenRelativeToTransformedText_shouldCorrectlySetSelection() {
        var actualSelectionRange = TextRange.Zero
        rule.setContent {
            Btf1Selection(visualTransformation = IncreasedVisualTransformation) {
                actualSelectionRange = it
            }
        }

        val transformedRange = TextRange(start = 10, end = 18)
        rule
            .onNodeWithTag(fieldTag)
            .performTextInputSelection(transformedRange, relativeToOriginalText = false)

        val expectedRange = TextRange(start = 5, end = 9)
        rule.runOnIdle { assertThat(actualSelectionRange).isEqualTo(expectedRange) }
    }

    @Composable
    fun Btf2Selection(
        enabled: Boolean = true,
        readOnly: Boolean = false,
        outputTransformation: OutputTransformation? = null,
        textCallback: (TextRange) -> Unit = {}
    ) {
        val tfs = rememberTextFieldState("text text text")
        BasicTextField(
            modifier = Modifier.testTag(fieldTag).border(0.dp, Color.Black),
            state = tfs,
            enabled = enabled,
            readOnly = readOnly,
            outputTransformation = outputTransformation,
        )

        LaunchedEffect(Unit) { snapshotFlow { tfs.selection }.collect { textCallback(it) } }
    }

    @Test
    fun btf2_performTextInputSelection_whenEditable_shouldCorrectlySetSelection() {
        var actualSelectionRange = TextRange.Zero
        rule.setContent { Btf2Selection { actualSelectionRange = it } }

        var expectedRange = TextRange(start = 5, end = 9)
        rule.onNodeWithTag(fieldTag).performTextInputSelection(expectedRange)

        rule.runOnIdle { assertThat(actualSelectionRange).isEqualTo(expectedRange) }
    }

    @Test
    fun btf2_performTextInputSelection_whenEditableAndReversed_shouldCorrectlySetSelection() {
        var actualSelectionRange = TextRange.Zero
        rule.setContent { Btf2Selection { actualSelectionRange = it } }

        rule.onNodeWithTag(fieldTag).performTextInputSelection(TextRange(start = 9, end = 5))

        var expectedRange = TextRange(start = 5, end = 9)
        rule.runOnIdle { assertThat(actualSelectionRange).isEqualTo(expectedRange) }
    }

    @Test
    fun btf2_performTextInputSelection_whenReadOnly_shouldCorrectlySetSelection() {
        var actualSelectionRange = TextRange.Zero
        rule.setContent { Btf2Selection(readOnly = true) { actualSelectionRange = it } }

        var expectedRange = TextRange(start = 5, end = 9)
        rule.onNodeWithTag(fieldTag).performTextInputSelection(expectedRange)

        rule.runOnIdle { assertThat(actualSelectionRange).isEqualTo(expectedRange) }
    }

    @Test
    fun btf2_performTextInputSelection_whenDisabled_shouldFail() {
        rule.setContent { Btf2Selection(enabled = false) }

        expectErrorMessageStartsWith(
            "Failed to perform text input selection.\n" +
                "Failed to assert the following: (is enabled)\n" +
                "Semantics of the node:"
        ) {
            rule.onNodeWithTag(fieldTag).performTextInputSelection(TextRange(start = 5, end = 9))
        }
    }

    @Test
    fun btf2_performTextInputSelection_invalidSelection_noChange() {
        var actualSelectionRange = TextRange.Zero
        rule.setContent { Btf2Selection { actualSelectionRange = it } }

        // BTF2 puts the cursor at the end of the text.
        val expectedRange = TextRange(14)
        rule.onNodeWithTag(fieldTag).performTextInputSelection(TextRange(0, Int.MAX_VALUE))
        rule.runOnIdle { assertThat(actualSelectionRange).isEqualTo(expectedRange) }
    }

    @Test
    fun btf2_performTextInputSelection_whenRelativeToTransformedText_shouldCorrectlySetSelection() {
        var actualSelectionRange = TextRange.Zero
        rule.setContent {
            Btf2Selection(outputTransformation = IncreasedOutputTransformation) {
                actualSelectionRange = it
            }
        }

        val transformedRange = TextRange(start = 10, end = 18)
        rule
            .onNodeWithTag(fieldTag)
            .performTextInputSelection(transformedRange, relativeToOriginalText = false)

        val expectedRange = TextRange(start = 5, end = 9)
        rule.runOnIdle { assertThat(actualSelectionRange).isEqualTo(expectedRange) }
    }
}

/** Adds a `-` after every single character in the original text */
private object IncreasedVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            AnnotatedString(text.text.map { "${it}-" }.joinToString("")),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int) = 2 * offset

                override fun transformedToOriginal(offset: Int) = offset / 2
            }
        )
    }
}

/** Adds a `-` between every single character in the original text */
private object IncreasedOutputTransformation : OutputTransformation {
    override fun TextFieldBuffer.transformOutput() {
        val endLength = length * 2
        for (i in 1..endLength step 2) {
            insert(i, "-")
        }
    }
}
