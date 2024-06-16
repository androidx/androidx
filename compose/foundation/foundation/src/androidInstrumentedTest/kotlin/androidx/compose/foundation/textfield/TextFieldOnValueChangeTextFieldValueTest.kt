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

package androidx.compose.foundation.textfield

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.InputMethodInterceptor
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@MediumTest
@RunWith(AndroidJUnit4::class)
class TextFieldOnValueChangeTextFieldValueTest {
    @get:Rule val rule = createComposeRule()

    private val inputMethodInterceptor = InputMethodInterceptor(rule)
    private val onValueChange: (TextFieldValue) -> Unit = mock()

    @Before
    fun setUp() {
        inputMethodInterceptor.setContent {
            val state = remember { mutableStateOf(TextFieldValue("abcde", TextRange.Zero)) }
            BasicTextField(
                value = state.value,
                onValueChange = {
                    state.value = it
                    onValueChange(it)
                }
            )
        }

        // Perform click to focus in.
        rule.onNode(hasSetTextAction()).performTouchInput { click(Offset(1f, 1f)) }
    }

    @Test
    fun commitText_onValueChange_call_once() {
        // Committing text should be reported as value change
        inputMethodInterceptor.withInputConnection { commitText("ABCDE", 1) }

        rule.runOnIdle {
            verify(onValueChange, times(1)).invoke(eq(TextFieldValue("ABCDEabcde", TextRange(5))))
        }
    }

    @Test
    fun setComposingRegion_onValueChange_call_once() {
        val textFieldValueCaptor = argumentCaptor<TextFieldValue>()
        // Composition change will be reported as a change
        inputMethodInterceptor.withInputConnection { setComposingRegion(0, 5) }
        inputMethodInterceptor.withInputConnection { setComposingRegion(0, 5) }

        rule.runOnIdle {
            verify(onValueChange, times(1)).invoke(textFieldValueCaptor.capture())
            assertThat(textFieldValueCaptor.firstValue.text).isEqualTo("abcde")
            assertThat(textFieldValueCaptor.firstValue.selection).isEqualTo(TextRange.Zero)
            assertThat(textFieldValueCaptor.firstValue.composition).isEqualTo(TextRange(0, 5))
        }
    }

    @Test
    fun setComposingText_onValueChange_call_once() {
        val textFieldValueCaptor = argumentCaptor<TextFieldValue>()
        val composingText = "ABCDE"

        inputMethodInterceptor.withInputConnection { setComposingText(composingText, 1) }

        rule.runOnIdle {
            verify(onValueChange, times(1)).invoke(textFieldValueCaptor.capture())
            assertThat(textFieldValueCaptor.firstValue.text).isEqualTo("ABCDEabcde")
            assertThat(textFieldValueCaptor.firstValue.selection).isEqualTo(TextRange(5))
            assertThat(textFieldValueCaptor.firstValue.composition).isEqualTo(TextRange(0, 5))
        }
    }

    @Test
    fun setSelection_onValueChange_call_once() {
        // Selection change is a part of value-change in EditorModel text field
        inputMethodInterceptor.withInputConnection { setSelection(1, 1) }

        rule.runOnIdle {
            verify(onValueChange, times(1)).invoke(eq(TextFieldValue("abcde", TextRange(1))))
        }
    }

    @Test
    fun clearComposition_onValueChange_call_once() {
        val textFieldValueCaptor = argumentCaptor<TextFieldValue>()
        val composingText = "ABCDE"

        inputMethodInterceptor.withInputConnection { setComposingText(composingText, 1) }

        rule.runOnIdle {
            verify(onValueChange, times(1)).invoke(textFieldValueCaptor.capture())
            assertThat(textFieldValueCaptor.firstValue.text).isEqualTo("ABCDEabcde")
            assertThat(textFieldValueCaptor.firstValue.selection).isEqualTo(TextRange(5))
            assertThat(textFieldValueCaptor.firstValue.composition)
                .isEqualTo(TextRange(0, composingText.length))
        }

        // Composition change will be reported as a change
        clearInvocations(onValueChange)
        val compositionClearCaptor = argumentCaptor<TextFieldValue>()
        inputMethodInterceptor.withInputConnection { finishComposingText() }
        rule.runOnIdle {
            verify(onValueChange, times(1)).invoke(compositionClearCaptor.capture())
            assertThat(compositionClearCaptor.firstValue.text).isEqualTo("ABCDEabcde")
            assertThat(compositionClearCaptor.firstValue.selection).isEqualTo(TextRange(5))
            assertThat(compositionClearCaptor.firstValue.composition).isNull()
        }
    }

    @Test
    fun deleteSurroundingText_onValueChange_call_once() {
        inputMethodInterceptor.withInputConnection { deleteSurroundingText(0, 1) }

        rule.runOnIdle {
            verify(onValueChange, times(1)).invoke(eq(TextFieldValue("bcde", TextRange.Zero)))
        }
    }
}
