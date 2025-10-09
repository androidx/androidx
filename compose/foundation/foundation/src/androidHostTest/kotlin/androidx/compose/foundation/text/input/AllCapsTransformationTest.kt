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

import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.intl.Locale
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AllCapsTransformationTest {

    @Test
    fun allCapsTransformation_definesCharacterCapitalizationKeyboardOption() {
        val transformation = InputTransformation.allCaps(Locale.current)
        assertThat(transformation.keyboardOptions?.capitalization)
            .isEqualTo(KeyboardCapitalization.Characters)
    }

    @Test
    fun allNewTypedCharacters_convertedToUppercase() {
        val transformation = InputTransformation.allCaps(Locale("en-US"))

        val originalValue = TextFieldCharSequence("")
        val buffer = TextFieldBuffer(originalValue).apply { append("hello") }

        with(transformation) { buffer.transformInput() }

        assertThat(buffer.toString()).isEqualTo("HELLO")
    }

    @Test
    fun oldCharacters_areNotConverted() {
        val transformation = InputTransformation.allCaps(Locale("en-US"))

        val originalValue = TextFieldCharSequence("hello")
        val buffer = TextFieldBuffer(originalValue).apply { append(" world") }

        with(transformation) { buffer.transformInput() }

        assertThat(buffer.toString()).isEqualTo("hello WORLD")
    }

    @Test
    fun localeDifference_turkishI() {
        val transformation = InputTransformation.allCaps(Locale("tr"))

        val originalValue = TextFieldCharSequence("")
        val buffer = TextFieldBuffer(originalValue).apply { append("i") }

        with(transformation) { buffer.transformInput() }

        assertThat(buffer.toString()).isEqualTo("\u0130") // Turkish dotted capital i
    }

    @Test
    fun multipleEdits() {
        val transformation = InputTransformation.allCaps(Locale("en-US"))

        var originalValue = TextFieldCharSequence("hello")
        var buffer = TextFieldBuffer(originalValue)

        with(buffer) {
            delete(0, 3) // lo
            replace(1, 1, "abc") // lABCo
        }

        with(transformation) { buffer.transformInput() }

        originalValue = buffer.toTextFieldCharSequence()
        buffer = TextFieldBuffer(originalValue)

        with(buffer) {
            delete(2, 3) // lACo
            append("xyz") // lACoXYZ
        }

        with(transformation) { buffer.transformInput() }

        assertThat(buffer.toString()).isEqualTo("lACoXYZ")
    }
}
