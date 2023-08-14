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

package androidx.compose.foundation.text2

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.selection.fetchTextLayoutResult
import androidx.compose.foundation.text2.input.CodepointTransformation
import androidx.compose.foundation.text2.input.TextFieldLineLimits
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.foundation.text2.input.mask
import androidx.compose.foundation.text2.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.text.TextRange
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.test.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class, ExperimentalTestApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class TextFieldCodepointTransformationTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val sessionHandler = InputMethodInterceptorRule(rule)

    private val Tag = "BasicTextField2"

    @Test
    fun textField_rendersTheResultOf_codepointTransformation() {
        val state = TextFieldState()
        state.setTextAndPlaceCursorAtEnd("Hello")
        rule.setContent {
            BasicTextField2(
                state = state,
                codepointTransformation = { _, codepoint -> codepoint + 1 },
                modifier = Modifier.testTag(Tag)
            )
        }

        assertLayoutText("Ifmmp") // one character after in lexical order
    }

    @Test
    fun textField_rendersTheResultOf_codepointTransformation_codepointIndex() {
        val state = TextFieldState()
        state.setTextAndPlaceCursorAtEnd("Hello")
        rule.setContent {
            BasicTextField2(
                state = state,
                codepointTransformation = { index, codepoint ->
                    if (index % 2 == 0) codepoint + 1 else codepoint - 1
                },
                modifier = Modifier.testTag(Tag)
            )
        }

        assertLayoutText("Idmkp") // one character after and before in lexical order
    }

    @Test
    fun textField_toggleCodepointTransformation_affectsNextFrame() {
        rule.mainClock.autoAdvance = false
        val state = TextFieldState()
        state.setTextAndPlaceCursorAtEnd("Hello")
        var codepointTransformation: CodepointTransformation? by mutableStateOf(null)
        rule.setContent {
            BasicTextField2(
                state = state,
                codepointTransformation = codepointTransformation,
                modifier = Modifier.testTag(Tag)
            )
        }

        assertLayoutText("Hello") // no change
        codepointTransformation = CodepointTransformation.mask('c')

        rule.mainClock.advanceTimeByFrame()
        assertLayoutText("ccccc") // all characters turn to c
    }

    @Test
    fun textField_statefulCodepointTransformation_reactsToStateChange() {
        val state = TextFieldState()
        state.setTextAndPlaceCursorAtEnd("Hello")
        var mask by mutableStateOf('-')
        rule.setContent {
            BasicTextField2(
                state = state,
                codepointTransformation = CodepointTransformation.mask(mask),
                modifier = Modifier.testTag(Tag)
            )
        }

        assertLayoutText("-----")
        mask = '@'

        rule.waitForIdle()
        assertLayoutText("@@@@@")
    }

    @Test
    fun textField_removingCodepointTransformation_rendersTextNormally() {
        val state = TextFieldState()
        state.setTextAndPlaceCursorAtEnd("Hello")
        var codepointTransformation by mutableStateOf<CodepointTransformation?>(
            CodepointTransformation.mask('*')
        )
        rule.setContent {
            BasicTextField2(
                state = state,
                codepointTransformation = codepointTransformation,
                modifier = Modifier.testTag(Tag)
            )
        }

        assertLayoutText("*****")
        codepointTransformation = null

        rule.waitForIdle()
        assertLayoutText("Hello")
    }

    @Test
    fun textField_codepointTransformation_continuesToRenderUpdatedText() {
        val state = TextFieldState()
        state.setTextAndPlaceCursorAtEnd("Hello")
        rule.setContent {
            BasicTextField2(
                state = state,
                codepointTransformation = CodepointTransformation.mask('*'),
                modifier = Modifier.testTag(Tag)
            )
        }

        assertLayoutText("*****")
        rule.waitForIdle()
        rule.onNodeWithTag(Tag).performTextInput(", World!")
        assertLayoutText("*".repeat("Hello, World!".length))
    }

    @Test
    fun textField_singleLine_removesLineFeedViaCodepointTransformation() {
        val state = TextFieldState()
        state.setTextAndPlaceCursorAtEnd("Hello\nWorld")
        rule.setContent {
            BasicTextField2(
                state = state,
                lineLimits = TextFieldLineLimits.SingleLine,
                modifier = Modifier.testTag(Tag)
            )
        }

        assertLayoutText("Hello World")
        rule.onNodeWithTag(Tag).performTextInput("\n")
        assertLayoutText("Hello World ")
    }

    @Test
    fun textField_singleLine_removesCarriageReturnViaCodepointTransformation() {
        val state = TextFieldState()
        state.setTextAndPlaceCursorAtEnd("Hello\rWorld")
        rule.setContent {
            BasicTextField2(
                state = state,
                lineLimits = TextFieldLineLimits.SingleLine,
                modifier = Modifier.testTag(Tag)
            )
        }

        assertLayoutText("Hello\uFEFFWorld")
    }

    @Test
    fun textField_singleLine_doesNotOverrideGivenCodepointTransformation() {
        val state = TextFieldState()
        state.setTextAndPlaceCursorAtEnd("Hello\nWorld")
        rule.setContent {
            BasicTextField2(
                state = state,
                lineLimits = TextFieldLineLimits.SingleLine,
                codepointTransformation = { _, codepoint -> codepoint },
                modifier = Modifier.testTag(Tag)
            )
        }

        assertLayoutText("Hello\nWorld")
    }

    @Test
    fun surrogateToNonSurrogate_singleCodepoint_isTransformed() {
        val state = TextFieldState(SingleSurrogateCodepointString)
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag),
                codepointTransformation = MaskWithNonSurrogate
            )
        }

        assertLayoutText(".")
    }

    @Test
    fun surrogateToNonSurrogate_multipleCodepoints_areTransformed() {
        val state = TextFieldState(SingleSurrogateCodepointString + SingleSurrogateCodepointString)
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag),
                codepointTransformation = MaskWithNonSurrogate
            )
        }

        assertLayoutText("..")
    }

    @Test
    fun surrogateToNonSurrogate_withNonSurrogates_areTransformed() {
        val state = TextFieldState("a${SingleSurrogateCodepointString}b")
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag),
                codepointTransformation = MaskWithNonSurrogate
            )
        }

        assertLayoutText("...")
    }

    @Test
    fun nonSurrogateToSurrogate_singleCodepoint_isTransformed() {
        val state = TextFieldState("a")
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag),
                codepointTransformation = MaskWithSurrogate
            )
        }

        assertLayoutText(SingleSurrogateCodepointString)
    }

    @Test
    fun nonSurrogateToSurrogate_multipleCodepoints_areTransformed() {
        val state = TextFieldState("ab")
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag),
                codepointTransformation = MaskWithSurrogate
            )
        }

        assertLayoutText(SingleSurrogateCodepointString + SingleSurrogateCodepointString)
    }

    @Test
    fun nonSurrogateToSurrogate_withNonSurrogates_areTransformed() {
        val state = TextFieldState("abc")
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag),
                codepointTransformation = { i, codepoint ->
                    if (i == 1) SurrogateCodepoint else codepoint
                }
            )
        }

        assertLayoutText("a${SingleSurrogateCodepointString}c")
    }

    @Test
    fun surrogateToNonSurrogate_singleCodepoint_selectionIsMappedAroundCodepoint() {
        val state = TextFieldState(SingleSurrogateCodepointString)
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag),
                codepointTransformation = MaskWithNonSurrogate
            )
        }

        assertVisualTextLength(1)
        state.assertSelectionMappings(
            TextRange(0) to TextRange(0),
            TextRange(0, 1) to TextRange(0, 2),
            TextRange(1, 0) to TextRange(2, 0),
            TextRange(1) to TextRange(2),
        )
    }

    @Test
    fun nonSurrogateToSurrogate_singleCodepoint_selectionIsMappedAroundCodepoint() {
        val state = TextFieldState("a")
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag),
                codepointTransformation = MaskWithSurrogate
            )
        }

        assertVisualTextLength(2)
        state.assertSelectionMappings(
            TextRange(0) to TextRange(0),
            TextRange(0, 1) to TextRange(0, 1),
            TextRange(0, 2) to TextRange(0, 1),
            TextRange(1, 0) to TextRange(1, 0),
            TextRange(1) to TextRange(0, 1),
            TextRange(1, 2) to TextRange(0, 1),
            TextRange(2, 0) to TextRange(1, 0),
            TextRange(2, 1) to TextRange(1, 0),
            TextRange(2) to TextRange(1),
        )
    }

    @Test
    fun multipleCodepoints_selectionIsMappedAroundCodepoints() {
        val state = TextFieldState("a${SingleSurrogateCodepointString}c")
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag),
                codepointTransformation = { i, codepoint ->
                    when (codepoint) {
                        'a'.code, 'c'.code -> SurrogateCodepoint
                        SurrogateCodepoint -> 'b'.code
                        else -> fail(
                            "unrecognized codepoint at index $i: " +
                                String(intArrayOf(codepoint), 0, 1)
                        )
                    }
                }
            )
        }

        assertVisualTextLength(5)
        state.assertSelectionMappings(
            TextRange(0) to TextRange(0),
            TextRange(0, 1) to TextRange(0, 1),
            TextRange(0, 2) to TextRange(0, 1),
            TextRange(0, 3) to TextRange(0, 3),
            TextRange(0, 4) to TextRange(0, 4),
            TextRange(0, 5) to TextRange(0, 4),
            TextRange(1, 0) to TextRange(1, 0),
            TextRange(1) to TextRange(0, 1),
            TextRange(1, 2) to TextRange(0, 1),
            TextRange(1, 3) to TextRange(0, 3),
            TextRange(1, 4) to TextRange(0, 4),
            TextRange(1, 5) to TextRange(0, 4),
            TextRange(2, 0) to TextRange(1, 0),
            TextRange(2, 1) to TextRange(1, 0),
            TextRange(2) to TextRange(1),
            TextRange(2, 3) to TextRange(1, 3),
            TextRange(2, 4) to TextRange(1, 4),
            TextRange(2, 5) to TextRange(1, 4),
            TextRange(3, 0) to TextRange(3, 0),
            TextRange(3, 1) to TextRange(3, 0),
            TextRange(3, 2) to TextRange(3, 1),
            TextRange(3) to TextRange(3),
            TextRange(3, 4) to TextRange(3, 4),
            TextRange(3, 5) to TextRange(3, 4),
            TextRange(4, 0) to TextRange(4, 0),
            TextRange(4, 1) to TextRange(4, 0),
            TextRange(4, 2) to TextRange(4, 1),
            TextRange(4, 3) to TextRange(4, 3),
            TextRange(4) to TextRange(3, 4),
            TextRange(4, 5) to TextRange(3, 4),
        )
    }

    @Test
    fun cursorTraversal_withArrowKeys() {
        val state = TextFieldState("a${SingleSurrogateCodepointString}c")
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag),
                codepointTransformation = { i, codepoint ->
                    when (codepoint) {
                        'a'.code -> SurrogateCodepoint
                        SurrogateCodepoint -> 'b'.code
                        'c'.code -> SurrogateCodepoint
                        else -> fail(
                            "unrecognized codepoint at index $i: " +
                                String(intArrayOf(codepoint), 0, 1)
                        )
                    }
                }
            )
        }

        rule.onNodeWithTag(Tag).requestFocus()
        rule.onNodeWithTag(Tag).performTextInputSelection(TextRange(0))

        listOf(0, 1, 3, 4).forEachIndexed { i, expectedCursor ->
            rule.runOnIdle {
                assertWithMessage("After pressing right arrow $i times")
                    .that(state.text.selectionInChars).isEqualTo(TextRange(expectedCursor))
            }
            rule.onNodeWithTag(Tag).performKeyInput {
                pressKey(Key.DirectionRight)
            }
        }
    }

    @Test
    fun expandSelectionForward_withArrowKeys() {
        val state = TextFieldState("a${SingleSurrogateCodepointString}c")
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag),
                codepointTransformation = { i, codepoint ->
                    when (codepoint) {
                        'a'.code -> SurrogateCodepoint
                        SurrogateCodepoint -> 'b'.code
                        'c'.code -> SurrogateCodepoint
                        else -> fail(
                            "unrecognized codepoint at index $i: " +
                                String(intArrayOf(codepoint), 0, 1)
                        )
                    }
                }
            )
        }

        rule.onNodeWithTag(Tag).requestFocus()
        rule.onNodeWithTag(Tag).performTextInputSelection(TextRange(0))

        listOf(
            TextRange(0),
            TextRange(0, 1),
            TextRange(0, 3),
            TextRange(0, 4)
        ).forEachIndexed { i, expectedSelection ->
            rule.runOnIdle {
                assertWithMessage("After pressing shift+right arrow $i times")
                    .that(state.text.selectionInChars).isEqualTo(expectedSelection)
            }
            rule.onNodeWithTag(Tag).performKeyInput {
                withKeyDown(Key.ShiftLeft) {
                    pressKey(Key.DirectionRight)
                }
            }
        }
    }

    @Test
    fun expandSelectionBackward_withArrowKeys() {
        val state = TextFieldState("a${SingleSurrogateCodepointString}c")
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag),
                codepointTransformation = { i, codepoint ->
                    when (codepoint) {
                        'a'.code -> SurrogateCodepoint
                        SurrogateCodepoint -> 'b'.code
                        'c'.code -> SurrogateCodepoint
                        else -> fail(
                            "unrecognized codepoint at index $i: " +
                                String(intArrayOf(codepoint), 0, 1)
                        )
                    }
                }
            )
        }

        rule.onNodeWithTag(Tag).requestFocus()
        rule.onNodeWithTag(Tag).performTextInputSelection(TextRange(4))

        listOf(
            TextRange(4),
            TextRange(4, 3),
            TextRange(4, 1),
            TextRange(4, 0)
        ).forEachIndexed { i, expectedSelection ->
            rule.runOnIdle {
                assertWithMessage("After pressing shift+left arrow $i times")
                    .that(state.text.selectionInChars).isEqualTo(expectedSelection)
            }
            rule.onNodeWithTag(Tag).performKeyInput {
                withKeyDown(Key.ShiftLeft) {
                    pressKey(Key.DirectionLeft)
                }
            }
        }
    }

    @Test
    fun insertNonSurrogates_intoSurrogateMask_fromKeyEvents() {
        val state = TextFieldState("a$SingleSurrogateCodepointString")
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag),
                codepointTransformation = MaskWithSurrogate
            )
        }
        rule.onNodeWithTag(Tag).requestFocus()
        rule.onNodeWithTag(Tag).performTextInputSelection(TextRange(0))

        rule.onNodeWithTag(Tag).performKeyInput {
            pressKey(Key.X)
            pressKey(Key.DirectionRight)
            pressKey(Key.Y)
            pressKey(Key.DirectionRight)
            pressKey(Key.Z)
        }

        rule.runOnIdle {
            assertThat(state.text.toString()).isEqualTo("xay${SingleSurrogateCodepointString}z")
        }
        assertVisualTextLength(10)
    }

    @Test
    fun insertNonSurrogates_intoNonSurrogateMask_fromKeyEvents() {
        val state = TextFieldState("a$SingleSurrogateCodepointString")
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag),
                codepointTransformation = MaskWithNonSurrogate
            )
        }
        rule.onNodeWithTag(Tag).requestFocus()
        rule.onNodeWithTag(Tag).performTextInputSelection(TextRange(0))

        rule.onNodeWithTag(Tag).performKeyInput {
            pressKey(Key.X)
            pressKey(Key.DirectionRight)
            pressKey(Key.Y)
            pressKey(Key.DirectionRight)
            pressKey(Key.Z)
        }

        rule.runOnIdle {
            assertThat(state.text.toString()).isEqualTo("xay${SingleSurrogateCodepointString}z")
        }
        assertVisualTextLength(5)
    }

    @Test
    fun insertText_intoSurrogateMask_fromSemantics() {
        val state = TextFieldState("a$SingleSurrogateCodepointString")
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag),
                codepointTransformation = MaskWithSurrogate
            )
        }
        rule.onNodeWithTag(Tag).requestFocus()
        rule.onNodeWithTag(Tag).performTextInputSelection(TextRange(0))

        // Use semantics to actually input the text, just use key events to move the cursor.
        rule.onNodeWithTag(Tag).performTextInput("x")
        pressKey(Key.DirectionRight)
        rule.onNodeWithTag(Tag).performTextInput("y")
        pressKey(Key.DirectionRight)
        rule.onNodeWithTag(Tag).performTextInput("z")

        rule.runOnIdle {
            assertThat(state.text.toString()).isEqualTo("xay${SingleSurrogateCodepointString}z")
        }
        assertVisualTextLength(10)
    }

    @Test
    fun insertNonSurrogates_intoNonSurrogateMask_fromSemantics() {
        val state = TextFieldState("a$SingleSurrogateCodepointString")
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag),
                codepointTransformation = MaskWithNonSurrogate
            )
        }
        rule.onNodeWithTag(Tag).requestFocus()
        rule.onNodeWithTag(Tag).performTextInputSelection(TextRange(0))

        // Use semantics to actually input the text, just use key events to move the cursor.
        rule.onNodeWithTag(Tag).performTextInput("x")
        pressKey(Key.DirectionRight)
        rule.onNodeWithTag(Tag).performTextInput("y")
        pressKey(Key.DirectionRight)
        rule.onNodeWithTag(Tag).performTextInput("z")

        rule.runOnIdle {
            assertThat(state.text.toString()).isEqualTo("xay${SingleSurrogateCodepointString}z")
        }
        assertVisualTextLength(5)
    }

    @Test
    fun insertText_intoSurrogateMask_fromIme() {
        val state = TextFieldState("a$SingleSurrogateCodepointString")
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag),
                codepointTransformation = MaskWithSurrogate
            )
        }
        rule.onNodeWithTag(Tag).requestFocus()
        sessionHandler.withInputConnection {
            beginBatchEdit()
            finishComposingText()
            setSelection(0, 0)
            endBatchEdit()
        }

        sessionHandler.withInputConnection { commitText("x", 1) }
        pressKey(Key.DirectionRight)
        sessionHandler.withInputConnection { commitText("y", 1) }
        pressKey(Key.DirectionRight)
        sessionHandler.withInputConnection { commitText("z", 1) }
        pressKey(Key.DirectionRight)

        rule.runOnIdle {
            assertThat(state.text.toString()).isEqualTo("xay${SingleSurrogateCodepointString}z")
        }
        assertVisualTextLength(10)
    }

    @Test
    fun insertText_intoNonSurrogateMask_fromIme() {
        val state = TextFieldState("a$SingleSurrogateCodepointString")
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag),
                codepointTransformation = MaskWithNonSurrogate
            )
        }
        rule.onNodeWithTag(Tag).requestFocus()
        sessionHandler.withInputConnection {
            beginBatchEdit()
            finishComposingText()
            setSelection(0, 0)
            endBatchEdit()
        }

        sessionHandler.withInputConnection { commitText("x", 1) }
        pressKey(Key.DirectionRight)
        sessionHandler.withInputConnection { commitText("y", 1) }
        pressKey(Key.DirectionRight)
        sessionHandler.withInputConnection { commitText("z", 1) }
        pressKey(Key.DirectionRight)

        rule.runOnIdle {
            assertThat(state.text.toString()).isEqualTo("xay${SingleSurrogateCodepointString}z")
        }
        assertVisualTextLength(5)
    }

    private fun assertLayoutText(text: String) {
        assertThat(rule.onNodeWithTag(Tag).fetchTextLayoutResult().layoutInput.text.text)
            .isEqualTo(text)
    }

    private fun assertVisualTextLength(expectedLength: Int) {
        assertThat(rule.onNodeWithTag(Tag).fetchTextLayoutResult().layoutInput.text.text)
            .hasLength(expectedLength)
    }

    private fun TextFieldState.assertSelectionMappings(
        vararg mappings: Pair<TextRange, TextRange>
    ) {
        mappings.forEach { (write, expected) ->
            val existingSelection = rule.onNodeWithTag(Tag)
                .fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange]
            // Setting the selection to the current selection will return false.
            if (existingSelection != write) {
                assertWithMessage("Expected to be able to select $write")
                    .that(performSelectionOnVisualText(write)).isTrue()
                rule.runOnIdle {
                    assertWithMessage("Visual selection $write to mapped")
                        .that(text.selectionInChars).isEqualTo(expected)
                }
            }
        }
    }

    private fun performSelectionOnVisualText(selection: TextRange): Boolean {
        rule.onNodeWithTag(Tag).requestFocus()
        var actionSucceeded = false
        rule.onNodeWithTag(Tag).performSemanticsAction(SemanticsActions.SetSelection) {
            actionSucceeded = it(selection.start, selection.end, /* relativeToOriginal= */ false)
        }
        return actionSucceeded
    }

    private fun pressKey(key: Key) {
        rule.onNodeWithTag(Tag).performKeyInput { pressKey(key) }
    }

    private companion object {
        /** This is "𐐷", a surrogate codepoint. */
        val SurrogateCodepoint = Character.toCodePoint('\uD801', '\uDC37')
        const val SingleSurrogateCodepointString = "\uD801\uDC37"

        val MaskWithSurrogate = CodepointTransformation { _, _ -> SurrogateCodepoint }
        val MaskWithNonSurrogate = CodepointTransformation { _, _ -> '.'.code }
    }
}
