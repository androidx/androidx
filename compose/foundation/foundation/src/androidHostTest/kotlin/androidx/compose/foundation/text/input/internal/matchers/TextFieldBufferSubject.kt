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

package androidx.compose.foundation.text.input.internal.matchers

import androidx.compose.foundation.text.InternalFoundationTextApi
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.internal.PartialGapBuffer
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth.assertAbout
import com.google.common.truth.Truth.assertThat

internal fun assertThat(buffer: PartialGapBuffer): TextFieldBufferSubject {
    return assertAbout(TextFieldBufferSubject.SUBJECT_FACTORY).that(GapBufferWrapper(buffer))!!
}

internal fun assertThat(buffer: TextFieldBuffer): TextFieldBufferSubject {
    return assertAbout(TextFieldBufferSubject.SUBJECT_FACTORY)
        .that(TextFieldBufferWrapper(buffer))!!
}

internal abstract class GetOperatorWrapper(val buffer: Any) {
    abstract operator fun get(index: Int): Char

    override fun toString(): String = buffer.toString()
}

private class TextFieldBufferWrapper(buffer: TextFieldBuffer) : GetOperatorWrapper(buffer) {
    override fun get(index: Int): Char = (buffer as TextFieldBuffer).asCharSequence()[index]
}

@OptIn(InternalFoundationTextApi::class)
private class GapBufferWrapper(buffer: PartialGapBuffer) : GetOperatorWrapper(buffer) {
    override fun get(index: Int): Char = (buffer as PartialGapBuffer)[index]
}

/** Truth extension for TextField Buffers. */
internal class TextFieldBufferSubject
private constructor(failureMetadata: FailureMetadata?, private val subject: GetOperatorWrapper) :
    Subject(failureMetadata, subject) {

    companion object {
        internal val SUBJECT_FACTORY: Factory<TextFieldBufferSubject, GetOperatorWrapper> =
            Factory { failureMetadata, subject ->
                TextFieldBufferSubject(failureMetadata, subject)
            }
    }

    fun hasChars(expected: String) {
        assertThat(subject.buffer.toString()).isEqualTo(expected)
        for (i in expected.indices) {
            assertThat(subject[i]).isEqualTo(expected[i])
        }
    }
}
