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

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.fetchTextLayoutResult
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.text.TextRange
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class TextFieldOutputTransformationHardwareKeysIntegrationTest {

    @get:Rule val rule = createComposeRule()

    private val inputMethodInterceptor = InputMethodInterceptor(rule)

    private val Tag = "BasicTextField"

    @Test
    fun replacement_visualText() {
        val text = TextFieldState("abcd", initialSelection = TextRange(0))
        inputMethodInterceptor.setContent {
            BasicTextField(
                state = text,
                modifier = Modifier.testTag(Tag),
                outputTransformation = {
                    replace(1, 3, "efg") // "aefgd"
                }
            )
        }

        assertVisualText("aefgd")
    }

    @Test
    fun replacement_cursorMovement_leftToRight_byCharacter_stateOffsets() {
        val text = TextFieldState("abcd", initialSelection = TextRange(0))
        inputMethodInterceptor.setContent {
            BasicTextField(
                state = text,
                modifier = Modifier.testTag(Tag),
                outputTransformation = {
                    replace(1, 3, "efg") // "aefgd"
                }
            )
        }
        rule.onNodeWithTag(Tag).requestFocus()

        assertThat(text.selection).isEqualTo(TextRange(0))
        pressKey(Key.DirectionRight)
        assertThat(text.selection).isEqualTo(TextRange(1))
        pressKey(Key.DirectionRight)
        assertThat(text.selection).isEqualTo(TextRange(3))
        pressKey(Key.DirectionRight)
        assertThat(text.selection).isEqualTo(TextRange(4))
    }

    @Test
    fun replacement_cursorMovement_rightToLeft_byCharacter_stateOffsets() {
        val text = TextFieldState("abcd", initialSelection = TextRange(4))
        inputMethodInterceptor.setContent {
            BasicTextField(
                state = text,
                modifier = Modifier.testTag(Tag),
                outputTransformation = {
                    replace(1, 3, "efg") // "aefgd"
                }
            )
        }
        rule.onNodeWithTag(Tag).requestFocus()

        assertThat(text.selection).isEqualTo(TextRange(4))
        pressKey(Key.DirectionLeft)
        assertThat(text.selection).isEqualTo(TextRange(3))
        pressKey(Key.DirectionLeft)
        assertThat(text.selection).isEqualTo(TextRange(1))
        pressKey(Key.DirectionLeft)
        assertThat(text.selection).isEqualTo(TextRange(0))
    }

    @Test
    fun replacement_cursorMovement_leftToRight_byCharacter_semanticsOffsets() {
        val text = TextFieldState("abcd", initialSelection = TextRange(0))
        inputMethodInterceptor.setContent {
            BasicTextField(
                state = text,
                modifier = Modifier.testTag(Tag),
                outputTransformation = {
                    replace(1, 3, "efg") // "aefgd"
                }
            )
        }
        rule.onNodeWithTag(Tag).requestFocus()

        assertCursor(0)
        pressKey(Key.DirectionRight)
        assertCursor(1)
        pressKey(Key.DirectionRight)
        assertCursor(4)
        pressKey(Key.DirectionRight)
        assertCursor(5)
    }

    @Test
    fun replacement_cursorMovement_rightToLeft_byCharacter_semanticsOffsets() {
        val text = TextFieldState("abcd", initialSelection = TextRange(4))
        inputMethodInterceptor.setContent {
            BasicTextField(
                state = text,
                modifier = Modifier.testTag(Tag),
                outputTransformation = {
                    replace(1, 3, "efg") // "aefgd"
                }
            )
        }
        rule.onNodeWithTag(Tag).requestFocus()

        assertCursor(5)
        pressKey(Key.DirectionLeft)
        assertCursor(4)
        pressKey(Key.DirectionLeft)
        assertCursor(1)
        pressKey(Key.DirectionLeft)
        assertCursor(0)
    }

    @Test
    fun insert_visualText() {
        val text = TextFieldState("ab", initialSelection = TextRange(0))
        inputMethodInterceptor.setContent {
            BasicTextField(
                state = text,
                modifier = Modifier.testTag(Tag),
                outputTransformation = {
                    insert(1, "efg") // "aefgb"
                }
            )
        }

        assertVisualText("aefgb")
    }

    @Test
    fun insert_cursorMovement_leftToRight_byCharacter_stateOffsets() {
        val text = TextFieldState("ab", initialSelection = TextRange(0))
        inputMethodInterceptor.setContent {
            BasicTextField(
                state = text,
                modifier = Modifier.testTag(Tag),
                outputTransformation = {
                    insert(1, "efg") // "aefgb"
                }
            )
        }
        rule.onNodeWithTag(Tag).requestFocus()

        assertThat(text.selection).isEqualTo(TextRange(0))
        pressKey(Key.DirectionRight)
        assertThat(text.selection).isEqualTo(TextRange(1))
        pressKey(Key.DirectionRight)
        assertThat(text.selection).isEqualTo(TextRange(1))
        pressKey(Key.DirectionRight)
        assertThat(text.selection).isEqualTo(TextRange(2))
    }

    @Test
    fun insert_cursorMovement_rightToLeft_byCharacter_stateOffsets() {
        val text = TextFieldState("ab", initialSelection = TextRange(2))
        inputMethodInterceptor.setContent {
            BasicTextField(
                state = text,
                modifier = Modifier.testTag(Tag),
                outputTransformation = {
                    insert(1, "efg") // "aefgb"
                }
            )
        }
        rule.onNodeWithTag(Tag).requestFocus()

        assertThat(text.selection).isEqualTo(TextRange(2))
        pressKey(Key.DirectionLeft)
        assertThat(text.selection).isEqualTo(TextRange(1))
        pressKey(Key.DirectionLeft)
        assertThat(text.selection).isEqualTo(TextRange(1))
        pressKey(Key.DirectionLeft)
        assertThat(text.selection).isEqualTo(TextRange(0))
    }

    @Test
    fun insert_cursorMovement_leftToRight_byCharacter_semanticsOffsets() {
        val text = TextFieldState("ab", initialSelection = TextRange(0))
        inputMethodInterceptor.setContent {
            BasicTextField(
                state = text,
                modifier = Modifier.testTag(Tag),
                outputTransformation = {
                    insert(1, "efg") // "aefgb"
                }
            )
        }
        rule.onNodeWithTag(Tag).requestFocus()

        assertCursor(0)
        pressKey(Key.DirectionRight)
        assertCursor(1)
        pressKey(Key.DirectionRight)
        assertCursor(4)
        pressKey(Key.DirectionRight)
        assertCursor(5)
    }

    @Test
    fun insert_cursorMovement_rightToLeft_byCharacter_semanticsOffsets() {
        val text = TextFieldState("ab", initialSelection = TextRange(2))
        inputMethodInterceptor.setContent {
            BasicTextField(
                state = text,
                modifier = Modifier.testTag(Tag),
                outputTransformation = {
                    insert(1, "efg") // "aefgb"
                }
            )
        }
        rule.onNodeWithTag(Tag).requestFocus()

        assertCursor(5)
        pressKey(Key.DirectionLeft)
        assertCursor(4)
        pressKey(Key.DirectionLeft)
        assertCursor(1)
        pressKey(Key.DirectionLeft)
        assertCursor(0)
    }

    @Test
    fun insert_cursorMovement_leftToRight_byWord_stateOffsets() {
        val text = TextFieldState("ab", initialSelection = TextRange(0))
        inputMethodInterceptor.setContent {
            BasicTextField(
                state = text,
                modifier = Modifier.testTag(Tag),
                outputTransformation = {
                    insert(2, "efg hij") // "abefg hij"
                }
            )
        }
        rule.onNodeWithTag(Tag).requestFocus()

        assertThat(text.selection).isEqualTo(TextRange(0))
        moveRightByWord()
        assertThat(text.selection).isEqualTo(TextRange(2))
        moveRightByWord()
        assertThat(text.selection).isEqualTo(TextRange(2))
        moveLeftByWord()
        assertThat(text.selection).isEqualTo(TextRange(2))
    }

    @Test
    fun insert_cursorMovement_leftToRight_byWord_semanticsOffsets() {
        val text = TextFieldState("ab", initialSelection = TextRange(0))
        inputMethodInterceptor.setContent {
            BasicTextField(
                state = text,
                modifier = Modifier.testTag(Tag),
                outputTransformation = {
                    insert(2, "efg hij") // "abefg hij"
                }
            )
        }
        rule.onNodeWithTag(Tag).requestFocus()

        assertCursor(0)
        moveRightByWord()
        assertCursor(9)
        moveRightByWord()
        assertCursor(9)
        moveLeftByWord()
        assertCursor(2)
    }

    @Test
    fun insert_cursorMovement_rightToLeft_byWord_stateOffsets() {
        val text = TextFieldState("ab", initialSelection = TextRange(2))
        inputMethodInterceptor.setContent {
            BasicTextField(
                state = text,
                modifier = Modifier.testTag(Tag),
                outputTransformation = {
                    insert(0, "hij efg") // "hij efgab"
                }
            )
        }
        rule.onNodeWithTag(Tag).requestFocus()

        assertThat(text.selection).isEqualTo(TextRange(2))
        moveLeftByWord()
        assertThat(text.selection).isEqualTo(TextRange(0))
        moveLeftByWord()
        assertThat(text.selection).isEqualTo(TextRange(0))
        moveRightByWord()
        assertThat(text.selection).isEqualTo(TextRange(0))
    }

    @Test
    fun insert_cursorMovement_rightToLeft_byWord_semanticsOffsets() {
        val text = TextFieldState("ab", initialSelection = TextRange(2))
        inputMethodInterceptor.setContent {
            BasicTextField(
                state = text,
                modifier = Modifier.testTag(Tag),
                outputTransformation = {
                    insert(0, "hij efg") // "hij efgab"
                }
            )
        }
        rule.onNodeWithTag(Tag).requestFocus()

        assertThat(text.selection).isEqualTo(TextRange(2))
        moveLeftByWord()
        assertThat(text.selection).isEqualTo(TextRange(0))
        moveLeftByWord()
        assertThat(text.selection).isEqualTo(TextRange(0))
        moveRightByWord()
        assertThat(text.selection).isEqualTo(TextRange(0))
    }

    @Test
    fun insert_cursorMovement_leftToRight_byLine_stateOffsets() {
        val text = TextFieldState("abc def", initialSelection = TextRange(0))
        inputMethodInterceptor.setContent {
            BasicTextField(
                state = text,
                modifier = Modifier.testTag(Tag),
                outputTransformation = {
                    insert(4, "xyz\nxyz ") // "abc xyz\nxyz def"
                }
            )
        }
        rule.onNodeWithTag(Tag).requestFocus()

        assertThat(text.selection).isEqualTo(TextRange(0))
        moveRightByLine()
        assertThat(text.selection).isEqualTo(TextRange(4))
        moveRightByLine()
        assertThat(text.selection).isEqualTo(TextRange(7))
    }

    @Test
    fun insert_cursorMovement_leftToRight_byLine_semanticsOffsets() {
        val text = TextFieldState("abc def", initialSelection = TextRange(0))
        inputMethodInterceptor.setContent {
            BasicTextField(
                state = text,
                modifier = Modifier.testTag(Tag),
                outputTransformation = {
                    insert(4, "xyz\nxyz ") // "abc xyz\nxyz def"
                }
            )
        }
        rule.onNodeWithTag(Tag).requestFocus()

        assertCursor(0)
        moveRightByLine()
        assertCursor(12)
        moveRightByLine()
        assertCursor(15)
    }

    @Test
    fun insert_cursorMovement_rightToLeft_byLine_stateOffsets() {
        val text = TextFieldState("abc def", initialSelection = TextRange(7))
        inputMethodInterceptor.setContent {
            BasicTextField(
                state = text,
                modifier = Modifier.testTag(Tag),
                outputTransformation = {
                    insert(4, "xyz\nxyz ") // "abc xyz\nxyz def"
                }
            )
        }
        rule.onNodeWithTag(Tag).requestFocus()

        assertThat(text.selection).isEqualTo(TextRange(7))
        moveLeftByLine()
        assertThat(text.selection).isEqualTo(TextRange(4))
        moveLeftByLine()
        assertThat(text.selection).isEqualTo(TextRange(0))
    }

    @Test
    fun insert_cursorMovement_rightToLeft_byLine_semanticsOffsets() {
        val text = TextFieldState("abc def", initialSelection = TextRange(7))
        inputMethodInterceptor.setContent {
            BasicTextField(
                state = text,
                modifier = Modifier.testTag(Tag),
                outputTransformation = {
                    insert(4, "xyz\nxyz ") // "abc xyz\nxyz def"
                }
            )
        }
        rule.onNodeWithTag(Tag).requestFocus()

        assertCursor(15)
        moveLeftByLine()
        assertCursor(4)
        moveLeftByLine()
        assertCursor(0)
    }

    @Test
    fun mixed_cursorMovement_leftToRight_allOffsets() {
        val text = TextFieldState("abc def ghi", initialSelection = TextRange(0))
        inputMethodInterceptor.setContent {
            BasicTextField(
                state = text,
                modifier = Modifier.testTag(Tag),
                outputTransformation = {
                    insert(0, ">") // >abc def ghi
                    replace(1, 4, "wedge") // >wedge def ghi
                    delete(7, 10) // >wedge  ghi
                    insert(8, "insertion") // >wedge  insertionghi
                    insert(length, "<") // >wedge  insertionghi<
                }
            )
        }
        rule.onNodeWithTag(Tag).requestFocus()

        val stateOffsets = mutableListOf<TextRange>()
        val semanticsOffsets = mutableListOf<TextRange>()

        for (i in 0 until 10) {
            stateOffsets += text.selection
            semanticsOffsets +=
                rule
                    .onNodeWithTag(Tag)
                    .fetchSemanticsNode()
                    .config[SemanticsProperties.TextSelectionRange]
            pressKey(Key.DirectionRight)
        }

        assertThat(stateOffsets)
            .isEqualTo(
                listOf(
                    TextRange(0),
                    TextRange(0),
                    TextRange(3),
                    TextRange(4, 7),
                    TextRange(8),
                    TextRange(8),
                    TextRange(9),
                    TextRange(10),
                    TextRange(11),
                    TextRange(11)
                )
            )
        assertThat(semanticsOffsets)
            .isEqualTo(
                listOf(
                    TextRange(0),
                    TextRange(1),
                    TextRange(6),
                    TextRange(7),
                    TextRange(8),
                    TextRange(17),
                    TextRange(18),
                    TextRange(19),
                    TextRange(20),
                    TextRange(21)
                )
            )
    }

    @Test
    fun mixed_cursorMovement_rightToLeft_allOffsets() {
        val text = TextFieldState("abc def ghi", initialSelection = TextRange(11))
        inputMethodInterceptor.setContent {
            BasicTextField(
                state = text,
                modifier = Modifier.testTag(Tag),
                outputTransformation = {
                    insert(0, ">") // >abc def ghi
                    replace(1, 4, "wedge") // >wedge def ghi
                    delete(7, 10) // >wedge  ghi
                    insert(8, "insertion") // >wedge  insertionghi
                    insert(length, "<") // >wedge  insertionghi<
                }
            )
        }
        // guarantees selection wedge affinity.
        rule.onNodeWithTag(Tag).performTouchInput { click(centerRight) }

        val stateOffsets = mutableListOf<TextRange>()
        val semanticsOffsets = mutableListOf<TextRange>()

        for (i in 0 until 10) {
            stateOffsets += text.selection
            semanticsOffsets +=
                rule
                    .onNodeWithTag(Tag)
                    .fetchSemanticsNode()
                    .config[SemanticsProperties.TextSelectionRange]
            pressKey(Key.DirectionLeft)
        }

        assertThat(stateOffsets)
            .isEqualTo(
                listOf(
                    TextRange(11),
                    TextRange(11),
                    TextRange(10),
                    TextRange(9),
                    TextRange(8),
                    TextRange(8),
                    TextRange(4, 7),
                    TextRange(3),
                    TextRange(0),
                    TextRange(0)
                )
            )
        assertThat(semanticsOffsets)
            .isEqualTo(
                listOf(
                    TextRange(21),
                    TextRange(20),
                    TextRange(19),
                    TextRange(18),
                    TextRange(17),
                    TextRange(8),
                    TextRange(7),
                    TextRange(6),
                    TextRange(1),
                    TextRange(0)
                )
            )
    }

    private fun assertVisualText(text: String) {
        assertThat(rule.onNodeWithTag(Tag).fetchTextLayoutResult().layoutInput.text.text)
            .isEqualTo(text)
    }

    private fun assertCursor(offset: Int) {
        val node = rule.onNodeWithTag(Tag).fetchSemanticsNode()
        assertWithMessage("Selection via semantics")
            .that(node.config[SemanticsProperties.TextSelectionRange])
            .isEqualTo(TextRange(offset))
    }

    private fun pressKey(key: Key) {
        rule.onNodeWithTag(Tag).performKeyInput { pressKey(key) }
    }

    private fun moveLeftByWord() = moveByWord(false)

    private fun moveRightByWord() = moveByWord(true)

    private fun moveByWord(direction: Boolean) {
        rule.onNodeWithTag(Tag).performKeyInput {
            withKeyDown(Key.CtrlLeft) {
                if (direction) pressKey(Key.DirectionRight) else pressKey(Key.DirectionLeft)
            }
        }
    }

    private fun moveLeftByLine() = moveByLine(false)

    private fun moveRightByLine() = moveByLine(true)

    private fun moveByLine(direction: Boolean) {
        rule.onNodeWithTag(Tag).performKeyInput {
            if (direction) pressKey(Key.MoveEnd) else pressKey(Key.MoveHome)
        }
    }
}
