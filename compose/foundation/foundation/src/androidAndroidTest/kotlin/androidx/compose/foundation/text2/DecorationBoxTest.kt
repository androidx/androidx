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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text2.input.TextFieldState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class DecorationBoxTest {

    @get:Rule
    val rule = createComposeRule()

    private val Tag = "BasicTextField2"
    private val DecorationTag = "DecorationBox"

    @Test
    fun focusIsAppliedOnDecoratedComposable() {
        val state = TextFieldState()
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                            .padding(16.dp)
                            .testTag(DecorationTag)
                    ) {
                        innerTextField()
                    }
                }
            )
        }

        // requestFocus on node
        rule.onNodeWithTag(Tag).performClick()

        // assertThat decoration modifier has a focused parent.
        rule.onNodeWithTag(DecorationTag, useUnmergedTree = true).assert(hasParent(isFocused()))
    }

    @Test
    fun semanticsAreAppliedOnDecoratedComposable() {
        val state = TextFieldState("hello")
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                            .padding(16.dp)
                            .testTag(DecorationTag)
                    ) {
                        innerTextField()
                    }
                }
            )
        }

        // assertThat decoration modifier has a focused parent.
        with(rule.onNodeWithTag(DecorationTag, useUnmergedTree = true)) {
            assert(hasParent(hasText("hello")))
            assert(hasParent(hasSetTextAction()))
        }
    }

    @Test
    fun clickGestureIsAppliedOnDecoratedComposable() {
        val state = TextFieldState("hello")
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                            .padding(16.dp)
                            .testTag(DecorationTag)
                    ) {
                        innerTextField()
                    }
                }
            )
        }

        // click on decoration box
        rule.onNodeWithTag(DecorationTag, useUnmergedTree = true).performTouchInput {
            // should be on the box not on inner text field since there is a padding
            click(Offset(1f, 1f))
        }

        // assertThat textfield has focus
        rule.onNodeWithTag(Tag).assertIsFocused()
    }

    @Test
    fun nonPlacedInnerTextField_stillAcceptsTextInput() {
        val state = TextFieldState()
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag),
                decorationBox = {
                    Box(
                        modifier = Modifier
                            .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                            .padding(16.dp)
                    )
                }
            )
        }

        // requestFocus on node
        with(rule.onNodeWithTag(Tag)) {
            performClick()
            performTextInput("hello")
        }

        rule.runOnIdle {
            assertThat(state.text.toString()).isEqualTo("hello")
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun nonPlacedInnerTextField_stillAcceptsKeyInput() {
        val state = TextFieldState()
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag),
                decorationBox = {
                    Box(
                        modifier = Modifier
                            .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                            .padding(16.dp)
                    )
                }
            )
        }

        // requestFocus on node
        with(rule.onNodeWithTag(Tag)) {
            performClick()
            performKeyInput {
                pressKey(Key.H)
                pressKey(Key.E)
                pressKey(Key.L)
                pressKey(Key.L)
                pressKey(Key.O)
            }
        }

        rule.runOnIdle {
            assertThat(state.text.toString()).isEqualTo("hello")
        }
    }

    @Ignore // TODO(halilibo): enable when pointerInput gestures are enabled
    @Test
    fun longClickGestureIsAppliedOnDecoratedComposable() {
        // create a decorated BasicTextField2
        val state = TextFieldState("hello")
        rule.setContent {
            BasicTextField2(
                state = state,
                modifier = Modifier.testTag(Tag),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .border(BorderStroke(2.dp, SolidColor(Color.Red)))
                            .padding(16.dp)
                            .testTag(DecorationTag)
                    ) {
                        innerTextField()
                    }
                }
            )
        }

        // click on decoration box
        rule.onNodeWithTag(DecorationTag, useUnmergedTree = true).performTouchInput {
            // should be on the box not on inner text field since there is a padding
            longClick(Offset(1f, 1f))
        }

        // assertThat selection happened
        rule.runOnIdle {
            assertThat(state.text.selectionInChars).isEqualTo(TextRange(0, 5))
        }
    }
}