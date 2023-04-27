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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class TextFieldCodepointTransformationTest {

    @get:Rule
    val rule = createComposeRule()

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
        var codepointTransformation by mutableStateOf(CodepointTransformation.None)
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
                codepointTransformation = CodepointTransformation.None,
                modifier = Modifier.testTag(Tag)
            )
        }

        assertLayoutText("Hello\nWorld")
    }

    // TODO: add more tests when selection is added

    private fun assertLayoutText(text: String) {
        assertThat(rule.onNodeWithTag(Tag).fetchTextLayoutResult().layoutInput.text.text)
            .isEqualTo(text)
    }
}