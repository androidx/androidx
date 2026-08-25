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
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.TextRange
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalFoundationApi::class)
@RunWith(JUnit4::class)
class MaxLengthTransformationTest {

    // region maxLengthTrim

    @Test
    fun maxLengthTrim_definesMaxTextLengthSemantics() {
        val transformation = InputTransformation.maxLengthTrim(10)
        val config = SemanticsConfiguration()
        with(transformation) { config.applySemantics() }
        assertThat(config[SemanticsProperties.MaxTextLength]).isEqualTo(10)
    }

    @Test
    fun maxLengthTrim_throwsException_whenNegativeMaxLength() {
        assertThrows(IllegalArgumentException::class.java) { InputTransformation.maxLengthTrim(-1) }
    }

    @Test
    fun maxLengthTrim_doesNotTruncate_whenLengthIsLessThanOrEqualToMaxLength() {
        val transformation = InputTransformation.maxLengthTrim(5)

        val originalValue = TextFieldCharSequence("")
        val buffer = TextFieldBuffer(originalValue).apply { insert(0, "abc") }

        with(transformation) { buffer.transformInput() }

        assertThat(buffer.toString()).isEqualTo("abc")
    }

    @Test
    fun maxLengthTrim_truncatesInsertedText_atEnd() {
        val transformation = InputTransformation.maxLengthTrim(5)

        val originalValue = TextFieldCharSequence("abc", TextRange(3))
        val buffer = TextFieldBuffer(originalValue).apply { insert(3, "12345") }

        with(transformation) { buffer.transformInput() }

        assertThat(buffer.toString()).isEqualTo("abc12")
        assertThat(buffer.selection).isEqualTo(TextRange(5))
    }

    @Test
    fun maxLengthTrim_truncatesInsertedText_inMiddle() {
        val transformation = InputTransformation.maxLengthTrim(3)

        val originalValue = TextFieldCharSequence("ac", TextRange(1))
        val buffer = TextFieldBuffer(originalValue).apply { insert(1, "1234") }

        with(transformation) { buffer.transformInput() }

        assertThat(buffer.toString()).isEqualTo("a1c")
        assertThat(buffer.selection).isEqualTo(TextRange(2))
    }

    @Test
    fun maxLengthTrim_truncatesReplacedText() {
        val transformation = InputTransformation.maxLengthTrim(10)

        val originalValue = TextFieldCharSequence("hello world", TextRange(11))
        val buffer = TextFieldBuffer(originalValue).apply { replace(6, 11, "everyone!") }

        with(transformation) { buffer.transformInput() }

        assertThat(buffer.toString()).isEqualTo("hello ever")
    }

    @Test
    fun maxLengthTrim_handlesZeroMaxLength() {
        val transformation = InputTransformation.maxLengthTrim(0)

        val originalValue = TextFieldCharSequence("")
        val buffer = TextFieldBuffer(originalValue).apply { insert(0, "abc") }

        with(transformation) { buffer.transformInput() }

        assertThat(buffer.toString()).isEqualTo("")
    }

    @Test
    fun maxLengthTrim_truncatesExistingLongText_whenFurtherEditsMade() {
        val transformation = InputTransformation.maxLengthTrim(5)

        val originalValue = TextFieldCharSequence("abcdef", TextRange(6))
        val buffer = TextFieldBuffer(originalValue).apply { insert(6, "g") }

        with(transformation) { buffer.transformInput() }

        assertThat(buffer.toString()).isEqualTo("abcde")
    }

    @Test
    fun maxLengthTrim_multipleEdits() {
        val transformation = InputTransformation.maxLengthTrim(3)

        var originalValue = TextFieldCharSequence("a", TextRange(1))
        var buffer = TextFieldBuffer(originalValue).apply { insert(1, "bcde") }

        with(transformation) { buffer.transformInput() }
        assertThat(buffer.toString()).isEqualTo("abc")

        originalValue = buffer.toTextFieldCharSequence()
        buffer = TextFieldBuffer(originalValue).apply { delete(1, 3) }

        with(transformation) { buffer.transformInput() }
        assertThat(buffer.toString()).isEqualTo("a")

        originalValue = buffer.toTextFieldCharSequence()
        buffer = TextFieldBuffer(originalValue).apply { insert(1, "xyz") }

        with(transformation) { buffer.transformInput() }
        assertThat(buffer.toString()).isEqualTo("axy")
    }

    @Test
    fun maxLengthTrim_multipleEditsInSingleBuffer_commitDeleteCommit() {
        val transformation = InputTransformation.maxLengthTrim(5)

        val originalValue = TextFieldCharSequence("")
        val buffer =
            TextFieldBuffer(originalValue).apply {
                insert(0, "1234567890")
                delete(0, 7)
                insert(3, "123")
            }

        with(transformation) { buffer.transformInput() }

        assertThat(buffer.toString()).isEqualTo("89012")
    }

    // endregion

    // region maxLengthReject

    @Test
    fun maxLengthReject_definesMaxTextLengthSemantics() {
        val transformation = InputTransformation.maxLengthReject(10)
        val config = SemanticsConfiguration()
        with(transformation) { config.applySemantics() }
        assertThat(config[SemanticsProperties.MaxTextLength]).isEqualTo(10)
    }

    @Test
    fun maxLengthReject_throwsException_whenNegativeMaxLength() {
        assertThrows(IllegalArgumentException::class.java) {
            InputTransformation.maxLengthReject(-1)
        }
    }

    @Test
    fun maxLengthReject_allowsInput_whenLengthIsLessThanOrEqualToMaxLength() {
        val transformation = InputTransformation.maxLengthReject(5)

        val originalValue = TextFieldCharSequence("")
        val buffer = TextFieldBuffer(originalValue).apply { insert(0, "abc") }

        with(transformation) { buffer.transformInput() }

        assertThat(buffer.toString()).isEqualTo("abc")
    }

    @Test
    fun maxLengthReject_revertsAllChanges_whenLengthExceedsMaxLength() {
        val transformation = InputTransformation.maxLengthReject(5)

        val originalValue = TextFieldCharSequence("abc", TextRange(3))
        val buffer = TextFieldBuffer(originalValue).apply { insert(3, "12345") }

        with(transformation) { buffer.transformInput() }

        assertThat(buffer.toString()).isEqualTo("abc")
        assertThat(buffer.selection).isEqualTo(TextRange(3))
    }

    @Test
    fun maxLengthReject_handlesZeroMaxLength() {
        val transformation = InputTransformation.maxLengthReject(0)

        val originalValue = TextFieldCharSequence("")
        val buffer = TextFieldBuffer(originalValue).apply { insert(0, "abc") }

        with(transformation) { buffer.transformInput() }

        assertThat(buffer.toString()).isEqualTo("")
    }

    // endregion

    // region maxLength (deprecated)

    @Suppress("DEPRECATION")
    @Test
    fun maxLength_definesMaxTextLengthSemantics() {
        val transformation = InputTransformation.maxLength(10)
        val config = SemanticsConfiguration()
        with(transformation) { config.applySemantics() }
        assertThat(config[SemanticsProperties.MaxTextLength]).isEqualTo(10)
    }

    @Suppress("DEPRECATION")
    @Test
    fun maxLength_revertsAllChanges_whenLengthExceedsMaxLength() {
        val transformation = InputTransformation.maxLength(5)

        val originalValue = TextFieldCharSequence("abc", TextRange(3))
        val buffer = TextFieldBuffer(originalValue).apply { insert(3, "12345") }

        with(transformation) { buffer.transformInput() }

        assertThat(buffer.toString()).isEqualTo("abc")
        assertThat(buffer.selection).isEqualTo(TextRange(3))
    }

    // endregion
}
