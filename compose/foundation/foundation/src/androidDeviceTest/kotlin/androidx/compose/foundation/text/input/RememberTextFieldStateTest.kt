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

import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class RememberTextFieldStateTest {

    @get:Rule val rule = createComposeRule()

    private val restorationTester = StateRestorationTester(rule)

    @Test
    fun rememberTextFieldState_withInitialTextAndSelection() {
        lateinit var state: TextFieldState
        rule.setContent {
            state = rememberTextFieldState(initialText = "hello", initialSelection = TextRange(2))
        }

        rule.runOnIdle {
            assertThat(state.text.toString()).isEqualTo("hello")
            assertThat(state.selection).isEqualTo(TextRange(2))
        }
    }

    @Test
    fun rememberTextFieldState_restoresTextAndSelection() {
        lateinit var originalState: TextFieldState
        lateinit var restoredState: TextFieldState
        var rememberCount = 0
        restorationTester.setContent {
            val state = rememberTextFieldState()
            if (remember { rememberCount++ } == 0) {
                originalState = state
            } else {
                restoredState = state
            }
        }
        rule.runOnIdle {
            originalState.edit {
                append("hello, world")
                selectAll()
            }
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            assertThat(restoredState.text.toString()).isEqualTo("hello, world")
            assertThat(restoredState.selection).isEqualTo(TextRange(0, 12))
        }
    }

    @Test
    fun rememberTextFieldState_withInitialTextAndSelection_restoresTextAndSelection() {
        lateinit var originalState: TextFieldState
        lateinit var restoredState: TextFieldState
        var rememberCount = 0
        restorationTester.setContent {
            val state =
                rememberTextFieldState(
                    initialText = "this should be ignored",
                    initialSelection = TextRange.Zero,
                )
            if (remember { rememberCount++ } == 0) {
                originalState = state
            } else {
                restoredState = state
            }
        }
        rule.runOnIdle {
            originalState.edit {
                replace(0, length, "hello, world")
                selectAll()
            }
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            assertThat(restoredState.text.toString()).isEqualTo("hello, world")
            assertThat(restoredState.selection).isEqualTo(TextRange(0, 12))
        }
    }

    @Test
    fun rememberTextFieldState_restoresStyles() {
        lateinit var originalState: TextFieldState
        lateinit var restoredState: TextFieldState
        var rememberCount = 0
        restorationTester.setContent {
            val state = rememberTextFieldState()
            if (remember { rememberCount++ } == 0) {
                originalState = state
            } else {
                restoredState = state
            }
        }
        rule.runOnIdle {
            originalState.edit {
                replace(length, length, "hello, world")
                addStyle(SpanStyle(color = Color.Red), TextRange(0, 5), ExpandPolicy.InsideOnly)
                addStyle(
                    SpanStyle(textDecoration = TextDecoration.Underline),
                    TextRange(7, 12),
                    ExpandPolicy.AtBoth,
                )
                addStyle(
                    ParagraphStyle(textAlign = TextAlign.Center),
                    TextRange(0, 5),
                    ExpandPolicy.InsideOnly,
                )
                addStyle(
                    ParagraphStyle(textAlign = TextAlign.Right),
                    TextRange(7, 12),
                    ExpandPolicy.AtBoth,
                )
            }
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            assertThat(restoredState.text.toString()).isEqualTo("hello, world")

            restoredState.edit {
                val spanStyles = getSpanStyles(TextRange(0, 12))
                assertThat(spanStyles).hasSize(2)
                assertThat(spanStyles[0].spanStyle).isEqualTo(SpanStyle(color = Color.Red))
                assertThat(spanStyles[0].textRange).isEqualTo(TextRange(0, 5))
                assertThat(spanStyles[0].expandPolicy).isEqualTo(ExpandPolicy.InsideOnly)

                assertThat(spanStyles[1].spanStyle)
                    .isEqualTo(SpanStyle(textDecoration = TextDecoration.Underline))
                assertThat(spanStyles[1].textRange).isEqualTo(TextRange(7, 12))
                assertThat(spanStyles[1].expandPolicy).isEqualTo(ExpandPolicy.AtBoth)

                val paragraphStyles = getParagraphStyles(TextRange(0, 12))
                assertThat(paragraphStyles).hasSize(2)
                assertThat(paragraphStyles[0].paragraphStyle)
                    .isEqualTo(ParagraphStyle(textAlign = TextAlign.Center))
                assertThat(paragraphStyles[0].textRange).isEqualTo(TextRange(0, 5))
                assertThat(paragraphStyles[0].expandPolicy).isEqualTo(ExpandPolicy.InsideOnly)

                assertThat(paragraphStyles[1].paragraphStyle)
                    .isEqualTo(ParagraphStyle(textAlign = TextAlign.Right))
                assertThat(paragraphStyles[1].textRange).isEqualTo(TextRange(7, 12))
                assertThat(paragraphStyles[1].expandPolicy).isEqualTo(ExpandPolicy.AtBoth)
            }
        }
    }
}
