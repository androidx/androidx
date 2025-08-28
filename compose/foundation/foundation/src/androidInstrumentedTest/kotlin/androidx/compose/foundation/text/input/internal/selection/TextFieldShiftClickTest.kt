/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.foundation.text.input.internal.selection

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.FocusedWindowTest
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMultiModalInput
import androidx.compose.ui.test.withKeysDown
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

@LargeTest
@OptIn(ExperimentalTestApi::class)
class TextFieldShiftClickTest : FocusedWindowTest {

    @get:Rule val rule = createComposeRule()

    private lateinit var state: TextFieldState

    private val TAG = "BasicTextField"

    private val fontSize = 10.sp

    private val defaultTextStyle = TextStyle(fontFamily = TEST_FONT_FAMILY, fontSize = fontSize)

    @Test
    fun shiftClickOnText_fromLeft() {
        state = TextFieldState("abc")
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG).width(50.dp).height(15.dp),
            )
        }

        with(rule.onNodeWithTag(TAG)) {
            performMultiModalInput {
                mouse { click(centerLeft) }
                key { withKeysDown(listOf(Key.ShiftLeft)) { mouse { click(centerRight) } } }
            }
        }
        assertThat(state.selection).isEqualTo(TextRange(0, state.value.length))
    }

    @Test
    fun shiftClickOnText_fromRight() {
        state = TextFieldState("abc")
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG).width(50.dp).height(15.dp),
            )
        }

        with(rule.onNodeWithTag(TAG)) {
            performMultiModalInput {
                mouse { click(centerRight) }
                key { withKeysDown(listOf(Key.ShiftLeft)) { mouse { click(centerLeft) } } }
            }
        }
        assertThat(state.selection).isEqualTo(TextRange(0, state.value.length))
    }

    @Test
    fun shiftClickOnText_rtl_fromLeft() {
        state = TextFieldState("\u05D0\u05D1\u05D2")
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG).width(50.dp).height(15.dp),
            )
        }

        with(rule.onNodeWithTag(TAG)) {
            performMultiModalInput {
                mouse { click(centerLeft) }
                key { withKeysDown(listOf(Key.ShiftLeft)) { mouse { click(centerRight) } } }
            }
        }
        assertThat(state.selection).isEqualTo(TextRange(0, state.value.length))
    }

    @Test
    fun shiftClickOnText_rtl_fromRight() {
        state = TextFieldState("\u05D0\u05D1\u05D2")
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG).width(50.dp).height(15.dp),
            )
        }

        with(rule.onNodeWithTag(TAG)) {
            performMultiModalInput {
                mouse { click(centerRight) }
                key { withKeysDown(listOf(Key.ShiftLeft)) { mouse { click(centerLeft) } } }
            }
        }
        assertThat(state.selection).isEqualTo(TextRange(0, state.value.length))
    }

    @Test
    fun shiftClickOnText_ltr_in_rtlLayout() {
        state = TextFieldState("abc")
        rule.setTextFieldTestContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BasicTextField(
                    state = state,
                    textStyle = defaultTextStyle,
                    modifier = Modifier.testTag(TAG).width(50.dp).height(15.dp),
                )
            }
        }

        with(rule.onNodeWithTag(TAG)) {
            performMultiModalInput {
                mouse { click(centerLeft) }
                key { withKeysDown(listOf(Key.ShiftLeft)) { mouse { click(centerRight) } } }
            }
        }
        assertThat(state.selection).isEqualTo(TextRange(0, state.value.length))
    }

    @Test
    fun shiftClickOnText_rtl_in_ltrLayout() {
        state = TextFieldState("\u05D0\u05D1\u05D2")
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG).width(50.dp).height(15.dp),
            )
        }

        with(rule.onNodeWithTag(TAG)) {
            performMultiModalInput {
                mouse { click(centerLeft) }
                key { withKeysDown(listOf(Key.ShiftLeft)) { mouse { click(centerRight) } } }
            }
        }
        assertThat(state.selection).isEqualTo(TextRange(0, state.value.length))
    }

    @Test
    fun shiftClickOnEmptyRegion_ltr() {
        state = TextFieldState("abc")
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                modifier = Modifier.testTag(TAG).width(50.dp).height(15.dp),
            )
        }

        with(rule.onNodeWithTag(TAG)) {
            performMultiModalInput {
                mouse { click(centerRight) }
                key {
                    withKeysDown(listOf(Key.ShiftLeft)) {
                        mouse { click(Offset((fontSize * 4).toPx(), height / 2f)) }
                    }
                }
            }
        }
        assertThat(state.selection).isEqualTo(TextRange(3))
    }

    @Test
    fun shiftClickOnEmptyRegion_rtl() {
        state = TextFieldState("\u05D0\u05D1\u05D2")
        rule.setTextFieldTestContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                BasicTextField(
                    state = state,
                    textStyle = defaultTextStyle,
                    modifier = Modifier.testTag(TAG).width(50.dp).height(15.dp),
                )
            }
        }

        with(rule.onNodeWithTag(TAG)) {
            performMultiModalInput {
                mouse { click(centerLeft) }
                key {
                    withKeysDown(listOf(Key.ShiftLeft)) {
                        mouse { click(Offset(fontSize.toPx(), height / 2f)) }
                    }
                }
            }
        }
        assertThat(state.selection).isEqualTo(TextRange(3))
    }

    @Test
    fun shiftClickOnText_readOnly() {
        state = TextFieldState("abc")
        rule.setTextFieldTestContent {
            BasicTextField(
                state = state,
                textStyle = defaultTextStyle,
                readOnly = true,
                modifier = Modifier.testTag(TAG).width(50.dp).height(15.dp),
            )
        }

        with(rule.onNodeWithTag(TAG)) {
            performMultiModalInput {
                mouse { click(centerLeft) }
                key { withKeysDown(listOf(Key.ShiftLeft)) { mouse { click(centerRight) } } }
            }
        }
        assertThat(state.selection).isEqualTo(TextRange(0, state.value.length))
    }
}
