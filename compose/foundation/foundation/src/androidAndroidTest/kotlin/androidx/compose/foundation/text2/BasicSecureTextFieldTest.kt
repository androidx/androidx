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
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.fetchTextLayoutResult
import androidx.compose.foundation.text2.input.TextObfuscationMode
import androidx.compose.foundation.text2.input.rememberTextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class BasicSecureTextFieldTest {

    @get:Rule
    val rule = createComposeRule().apply {
        mainClock.autoAdvance = false
    }

    private val Tag = "BasicSecureTextField"

    @Test
    fun passwordSemanticsAreSet() {
        rule.setContent {
            BasicSecureTextField(
                state = rememberTextFieldState(),
                modifier = Modifier.testTag(Tag)
            )
        }

        rule.onNodeWithTag(Tag).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
    }

    @Test
    fun lastTypedCharacterIsRevealedTemporarily() {
        rule.setContent {
            BasicSecureTextField(
                state = rememberTextFieldState(),
                modifier = Modifier.testTag(Tag)
            )
        }

        with(rule.onNodeWithTag(Tag)) {
            performTextInput("a")
            rule.mainClock.advanceTimeBy(200)
            assertThat(fetchTextLayoutResult().layoutInput.text.text).isEqualTo("a")
            rule.mainClock.advanceTimeBy(1500)
            assertThat(fetchTextLayoutResult().layoutInput.text.text).isEqualTo("\u2022")
        }
    }

    @Test
    fun lastTypedCharacterIsRevealed_hidesAfterAnotherCharacterIsTyped() {
        rule.setContent {
            BasicSecureTextField(
                state = rememberTextFieldState(),
                modifier = Modifier.testTag(Tag)
            )
        }

        with(rule.onNodeWithTag(Tag)) {
            performTextInput("a")
            rule.mainClock.advanceTimeBy(200)
            assertThat(fetchTextLayoutResult().layoutInput.text.text).isEqualTo("a")
            performTextInput("b")
            rule.mainClock.advanceTimeBy(50)
            assertThat(fetchTextLayoutResult().layoutInput.text.text).isEqualTo("\u2022b")
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun lastTypedCharacterIsRevealed_whenInsertedInMiddle() {
        rule.setContent {
            BasicSecureTextField(
                state = rememberTextFieldState(),
                modifier = Modifier.testTag(Tag)
            )
        }

        with(rule.onNodeWithTag(Tag)) {
            performTextInput("abc")
            rule.mainClock.advanceTimeBy(200)
            assertThat(fetchTextLayoutResult().layoutInput.text.text)
                .isEqualTo("\u2022\u2022\u2022")
            performTextInputSelection(TextRange(1))
            performTextInput("d")
            rule.mainClock.advanceTimeBy(50)
            assertThat(fetchTextLayoutResult().layoutInput.text.text)
                .isEqualTo("\u2022d\u2022\u2022")
        }
    }

    @Test
    fun lastTypedCharacterIsRevealed_hidesAfterFocusIsLost() {
        val focusRequester = FocusRequester()
        rule.setContent {
            Column {
                BasicSecureTextField(
                    state = rememberTextFieldState(),
                    modifier = Modifier.testTag(Tag)
                )
                Box(modifier = Modifier
                    .size(1.dp)
                    .focusRequester(focusRequester)
                    .focusable()
                )
            }
        }

        with(rule.onNodeWithTag(Tag)) {
            performTextInput("a")
            rule.mainClock.advanceTimeBy(200)
            assertThat(fetchTextLayoutResult().layoutInput.text.text).isEqualTo("a")
            focusRequester.requestFocus()
            rule.mainClock.advanceTimeBy(50)
            assertThat(fetchTextLayoutResult().layoutInput.text.text).isEqualTo("\u2022")
        }
    }

    @Test
    fun lastTypedCharacterIsRevealed_hidesAfterAnotherCharacterRemoved() {
        rule.setContent {
            BasicSecureTextField(
                state = rememberTextFieldState(),
                modifier = Modifier.testTag(Tag)
            )
        }

        with(rule.onNodeWithTag(Tag)) {
            performTextInput("abc")
            rule.mainClock.advanceTimeBy(200)
            performTextInput("d")
            rule.mainClock.advanceTimeBy(50)
            assertThat(fetchTextLayoutResult().layoutInput.text.text)
                .isEqualTo("\u2022\u2022\u2022d")
            performTextReplacement("bcd")
            assertThat(fetchTextLayoutResult().layoutInput.text.text)
                .isEqualTo("\u2022\u2022\u2022")
        }
    }

    @Test
    fun obfuscationMethodVisible_doesNotHideAnything() {
        rule.setContent {
            BasicSecureTextField(
                state = rememberTextFieldState(),
                textObfuscationMode = TextObfuscationMode.Visible,
                modifier = Modifier.testTag(Tag)
            )
        }

        with(rule.onNodeWithTag(Tag)) {
            performTextInput("abc")
            rule.mainClock.advanceTimeBy(200)
            assertThat(fetchTextLayoutResult().layoutInput.text.text)
                .isEqualTo("abc")
            rule.mainClock.advanceTimeBy(1500)
            assertThat(fetchTextLayoutResult().layoutInput.text.text)
                .isEqualTo("abc")
        }
    }

    @Test
    fun obfuscationMethodVisible_revealsEverythingWhenSwitchedTo() {
        var obfuscationMode by mutableStateOf(TextObfuscationMode.Hidden)
        rule.setContent {
            BasicSecureTextField(
                state = rememberTextFieldState(),
                textObfuscationMode = obfuscationMode,
                modifier = Modifier.testTag(Tag)
            )
        }

        with(rule.onNodeWithTag(Tag)) {
            performTextInput("abc")
            rule.mainClock.advanceTimeBy(200)
            assertThat(fetchTextLayoutResult().layoutInput.text.text)
                .isEqualTo("\u2022\u2022\u2022")
            obfuscationMode = TextObfuscationMode.Visible
            rule.mainClock.advanceTimeByFrame()
            assertThat(fetchTextLayoutResult().layoutInput.text.text)
                .isEqualTo("abc")
        }
    }

    @Test
    fun obfuscationMethodHidden_hidesEverything() {
        rule.setContent {
            BasicSecureTextField(
                state = rememberTextFieldState(),
                textObfuscationMode = TextObfuscationMode.Hidden,
                modifier = Modifier.testTag(Tag)
            )
        }

        with(rule.onNodeWithTag(Tag)) {
            performTextInput("abc")
            rule.mainClock.advanceTimeByFrame()
            assertThat(fetchTextLayoutResult().layoutInput.text.text)
                .isEqualTo("\u2022\u2022\u2022")
            performTextInput("d")
            rule.mainClock.advanceTimeByFrame()
            assertThat(fetchTextLayoutResult().layoutInput.text.text)
                .isEqualTo("\u2022\u2022\u2022\u2022")
        }
    }

    @Test
    fun obfuscationMethodHidden_hidesEverythingWhenSwitchedTo() {
        var obfuscationMode by mutableStateOf(TextObfuscationMode.Visible)
        rule.setContent {
            BasicSecureTextField(
                state = rememberTextFieldState(),
                textObfuscationMode = obfuscationMode,
                modifier = Modifier.testTag(Tag)
            )
        }

        with(rule.onNodeWithTag(Tag)) {
            performTextInput("abc")
            rule.mainClock.advanceTimeByFrame()
            assertThat(fetchTextLayoutResult().layoutInput.text.text)
                .isEqualTo("abc")
            obfuscationMode = TextObfuscationMode.Hidden
            rule.mainClock.advanceTimeByFrame()
            assertThat(fetchTextLayoutResult().layoutInput.text.text)
                .isEqualTo("\u2022\u2022\u2022")
        }
    }
}