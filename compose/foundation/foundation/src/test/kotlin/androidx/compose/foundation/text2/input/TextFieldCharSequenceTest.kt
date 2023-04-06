/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.foundation.text2.input

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.text.TextRange
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalFoundationApi::class)
@RunWith(JUnit4::class)
class TextFieldCharSequenceTest {
    private val defaultSaverScope = SaverScope { true }

    @Test(expected = IllegalArgumentException::class)
    fun throws_exception_for_negative_selection() {
        TextFieldCharSequence(text = "", selection = TextRange(-1))
    }

    @Test
    fun aligns_selection_to_the_text_length() {
        val text = "a"
        val textFieldValue =
            TextFieldCharSequence(text = text, selection = TextRange(text.length + 1))
        assertThat(textFieldValue.selectionInChars.collapsed).isTrue()
        assertThat(textFieldValue.selectionInChars.max).isEqualTo(textFieldValue.length)
    }

    @Test
    fun keep_selection_that_is_less_than_text_length() {
        val text = "a bc"
        val selection = TextRange(0, "a".length)

        val textFieldValue = TextFieldCharSequence(text = text, selection = selection)

        assertThat(textFieldValue.toString()).isEqualTo(text)
        assertThat(textFieldValue.selectionInChars).isEqualTo(selection)
    }

    @Test(expected = IllegalArgumentException::class)
    fun throws_exception_for_negative_composition() {
        TextEditState(text = "", composition = TextRange(-1))
    }

    @Test
    fun aligns_composition_to_text_length() {
        val text = "a"
        val textFieldValue = TextEditState(text = text, composition = TextRange(text.length + 1))
        assertThat(textFieldValue.compositionInChars?.collapsed).isTrue()
        assertThat(textFieldValue.compositionInChars?.max).isEqualTo(textFieldValue.length)
    }

    @Test
    fun keep_composition_that_is_less_than_text_length() {
        val text = "a bc"
        val composition = TextRange(0, "a".length)

        val textFieldValue = TextEditState(text = text, composition = composition)

        assertThat(textFieldValue.toString()).isEqualTo(text)
        assertThat(textFieldValue.compositionInChars).isEqualTo(composition)
    }

    @Test
    fun equals_returns_true_for_same_instance() {
        val textFieldValue = TextFieldCharSequence(
            text = "a",
            selection = TextRange(1),
            composition = TextRange(2)
        )

        assertThat(textFieldValue).isEqualTo(textFieldValue)
    }

    @Test
    fun equals_returns_true_for_equivalent_object() {
        val textFieldValue = TextFieldCharSequence(
            text = "a",
            selection = TextRange(1),
            composition = TextRange(2)
        )

        assertThat(
            TextFieldCharSequence(
                textFieldValue,
                textFieldValue.selectionInChars,
                textFieldValue.compositionInChars
            )
        ).isEqualTo(textFieldValue)
    }

    @Test
    fun text_and_selection_parameter_constructor_has_null_composition() {
        val textFieldValue = TextFieldCharSequence(
            text = "a",
            selection = TextRange(1)
        )

        assertThat(textFieldValue.compositionInChars).isNull()
    }

    private fun TextEditState(text: String, composition: TextRange) =
        TextFieldCharSequence(text, selection = TextRange.Zero, composition = composition)
}