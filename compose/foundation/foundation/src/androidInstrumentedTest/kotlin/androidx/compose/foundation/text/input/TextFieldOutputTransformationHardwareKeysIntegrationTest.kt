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
import androidx.compose.foundation.text.BasicTextField2
import androidx.compose.foundation.text.selection.fetchTextLayoutResult
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.TextRange
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class, ExperimentalTestApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class TextFieldOutputTransformationHardwareKeysIntegrationTest {

    @get:Rule
    val rule = createComposeRule()

    private val inputMethodInterceptor = InputMethodInterceptor(rule)

    private val Tag = "BasicTextField2"

    @Test
    fun replacement_visualText() {
        val text = TextFieldState("abcd", initialSelectionInChars = TextRange(0))
        inputMethodInterceptor.setContent {
            BasicTextField2(state = text,
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
        val text = TextFieldState("abcd", initialSelectionInChars = TextRange(0))
        inputMethodInterceptor.setContent {
            BasicTextField2(state = text,
                modifier = Modifier.testTag(Tag),
                outputTransformation = {
                    replace(1, 3, "efg") // "aefgd"
                }
            )
        }
        rule.onNodeWithTag(Tag).requestFocus()

        assertThat(text.text.selectionInChars).isEqualTo(TextRange(0))
        pressKey(Key.DirectionRight)
        assertThat(text.text.selectionInChars).isEqualTo(TextRange(1))
        pressKey(Key.DirectionRight)
        assertThat(text.text.selectionInChars).isEqualTo(TextRange(3))
        pressKey(Key.DirectionRight)
        assertThat(text.text.selectionInChars).isEqualTo(TextRange(4))
    }

    @Test
    fun replacement_cursorMovement_rightToLeft_byCharacter_stateOffsets() {
        val text = TextFieldState("abcd", initialSelectionInChars = TextRange(4))
        inputMethodInterceptor.setContent {
            BasicTextField2(state = text,
                modifier = Modifier.testTag(Tag),
                outputTransformation = {
                    replace(1, 3, "efg") // "aefgd"
                }
            )
        }
        rule.onNodeWithTag(Tag).requestFocus()

        assertThat(text.text.selectionInChars).isEqualTo(TextRange(4))
        pressKey(Key.DirectionLeft)
        assertThat(text.text.selectionInChars).isEqualTo(TextRange(3))
        pressKey(Key.DirectionLeft)
        assertThat(text.text.selectionInChars).isEqualTo(TextRange(1))
        pressKey(Key.DirectionLeft)
        assertThat(text.text.selectionInChars).isEqualTo(TextRange(0))
    }

    @Test
    fun replacement_cursorMovement_leftToRight_byCharacter_semanticsOffsets() {
        val text = TextFieldState("abcd", initialSelectionInChars = TextRange(0))
        inputMethodInterceptor.setContent {
            BasicTextField2(state = text,
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
        val text = TextFieldState("abcd", initialSelectionInChars = TextRange(4))
        inputMethodInterceptor.setContent {
            BasicTextField2(state = text,
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
        val text = TextFieldState("ab", initialSelectionInChars = TextRange(0))
        inputMethodInterceptor.setContent {
            BasicTextField2(state = text,
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
        val text = TextFieldState("ab", initialSelectionInChars = TextRange(0))
        inputMethodInterceptor.setContent {
            BasicTextField2(state = text,
                modifier = Modifier.testTag(Tag),
                outputTransformation = {
                    insert(1, "efg") // "aefgb"
                }
            )
        }
        rule.onNodeWithTag(Tag).requestFocus()

        assertThat(text.text.selectionInChars).isEqualTo(TextRange(0))
        pressKey(Key.DirectionRight)
        assertThat(text.text.selectionInChars).isEqualTo(TextRange(1))
        pressKey(Key.DirectionRight)
        assertThat(text.text.selectionInChars).isEqualTo(TextRange(1))
        pressKey(Key.DirectionRight)
        assertThat(text.text.selectionInChars).isEqualTo(TextRange(2))
    }

    @Test
    fun insert_cursorMovement_rightToLeft_byCharacter_stateOffsets() {
        val text = TextFieldState("ab", initialSelectionInChars = TextRange(2))
        inputMethodInterceptor.setContent {
            BasicTextField2(state = text,
                modifier = Modifier.testTag(Tag),
                outputTransformation = {
                    insert(1, "efg") // "aefgb"
                }
            )
        }
        rule.onNodeWithTag(Tag).requestFocus()

        assertThat(text.text.selectionInChars).isEqualTo(TextRange(2))
        pressKey(Key.DirectionLeft)
        assertThat(text.text.selectionInChars).isEqualTo(TextRange(1))
        pressKey(Key.DirectionLeft)
        assertThat(text.text.selectionInChars).isEqualTo(TextRange(1))
        pressKey(Key.DirectionLeft)
        assertThat(text.text.selectionInChars).isEqualTo(TextRange(0))
    }

    @Test
    fun insert_cursorMovement_leftToRight_byCharacter_semanticsOffsets() {
        val text = TextFieldState("ab", initialSelectionInChars = TextRange(0))
        inputMethodInterceptor.setContent {
            BasicTextField2(state = text,
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
        val text = TextFieldState("ab", initialSelectionInChars = TextRange(2))
        inputMethodInterceptor.setContent {
            BasicTextField2(state = text,
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
}
