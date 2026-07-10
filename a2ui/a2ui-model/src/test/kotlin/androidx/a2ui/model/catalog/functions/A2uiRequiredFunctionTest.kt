/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.a2ui.model.catalog.functions

import androidx.a2ui.model.protocol.A2uiException
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class A2uiRequiredFunctionTest {

    @Test
    fun execute_emptyString_returnsFalse() {
        assertThat(A2uiRequiredFunction.INSTANCE.execute(mapOf(ARG_VALUE to ""))).isEqualTo(false)
    }

    @Test
    fun execute_nonEmptyString_returnsTrue() {
        assertThat(A2uiRequiredFunction.INSTANCE.execute(mapOf(ARG_VALUE to "hello")))
            .isEqualTo(true)
    }

    @Test
    fun execute_emptyCharSequence_returnsFalse() {
        val charSequence = StringBuilder()
        assertThat(A2uiRequiredFunction.INSTANCE.execute(mapOf(ARG_VALUE to charSequence)))
            .isEqualTo(false)
    }

    @Test
    fun execute_nonEmptyCharSequence_returnsTrue() {
        val charSequence = StringBuilder("hello")
        assertThat(A2uiRequiredFunction.INSTANCE.execute(mapOf(ARG_VALUE to charSequence)))
            .isEqualTo(true)
    }

    @Test
    fun execute_emptyCollection_returnsFalse() {
        assertThat(A2uiRequiredFunction.INSTANCE.execute(mapOf(ARG_VALUE to emptyList<Any>())))
            .isEqualTo(false)
    }

    @Test
    fun execute_nonEmptyCollection_returnsTrue() {
        assertThat(A2uiRequiredFunction.INSTANCE.execute(mapOf(ARG_VALUE to listOf(1))))
            .isEqualTo(true)
    }

    @Test
    fun execute_emptyMap_returnsFalse() {
        assertThat(A2uiRequiredFunction.INSTANCE.execute(mapOf(ARG_VALUE to emptyMap<Any, Any>())))
            .isEqualTo(false)
    }

    @Test
    fun execute_nonEmptyMap_returnsTrue() {
        assertThat(A2uiRequiredFunction.INSTANCE.execute(mapOf(ARG_VALUE to mapOf(1 to 2))))
            .isEqualTo(true)
    }

    @Test
    fun execute_emptyArray_returnsFalse() {
        assertThat(A2uiRequiredFunction.INSTANCE.execute(mapOf(ARG_VALUE to emptyArray<Any>())))
            .isEqualTo(false)
    }

    @Test
    fun execute_nonEmptyArray_returnsTrue() {
        assertThat(A2uiRequiredFunction.INSTANCE.execute(mapOf(ARG_VALUE to arrayOf("a"))))
            .isEqualTo(true)
    }

    @Test
    fun execute_emptyIntArray_returnsFalse() {
        assertThat(A2uiRequiredFunction.INSTANCE.execute(mapOf(ARG_VALUE to intArrayOf())))
            .isEqualTo(false)
    }

    @Test
    fun execute_nonEmptyIntArray_returnsTrue() {
        assertThat(A2uiRequiredFunction.INSTANCE.execute(mapOf(ARG_VALUE to intArrayOf(1))))
            .isEqualTo(true)
    }

    @Test
    fun execute_emptyLongArray_returnsFalse() {
        assertThat(A2uiRequiredFunction.INSTANCE.execute(mapOf(ARG_VALUE to longArrayOf())))
            .isEqualTo(false)
    }

    @Test
    fun execute_nonEmptyLongArray_returnsTrue() {
        assertThat(A2uiRequiredFunction.INSTANCE.execute(mapOf(ARG_VALUE to longArrayOf(1L))))
            .isEqualTo(true)
    }

    @Test
    fun execute_emptyDoubleArray_returnsFalse() {
        assertThat(A2uiRequiredFunction.INSTANCE.execute(mapOf(ARG_VALUE to doubleArrayOf())))
            .isEqualTo(false)
    }

    @Test
    fun execute_nonEmptyDoubleArray_returnsTrue() {
        assertThat(A2uiRequiredFunction.INSTANCE.execute(mapOf(ARG_VALUE to doubleArrayOf(1.0))))
            .isEqualTo(true)
    }

    @Test
    fun execute_emptyBooleanArray_returnsFalse() {
        assertThat(A2uiRequiredFunction.INSTANCE.execute(mapOf(ARG_VALUE to booleanArrayOf())))
            .isEqualTo(false)
    }

    @Test
    fun execute_nonEmptyBooleanArray_returnsTrue() {
        assertThat(A2uiRequiredFunction.INSTANCE.execute(mapOf(ARG_VALUE to booleanArrayOf(true))))
            .isEqualTo(true)
    }

    @Test
    fun execute_emptyByteArray_returnsFalse() {
        assertThat(A2uiRequiredFunction.INSTANCE.execute(mapOf(ARG_VALUE to byteArrayOf())))
            .isEqualTo(false)
    }

    @Test
    fun execute_nonEmptyByteArray_returnsTrue() {
        assertThat(A2uiRequiredFunction.INSTANCE.execute(mapOf(ARG_VALUE to byteArrayOf(1))))
            .isEqualTo(true)
    }

    @Test
    fun execute_emptyCharArray_returnsFalse() {
        assertThat(A2uiRequiredFunction.INSTANCE.execute(mapOf(ARG_VALUE to charArrayOf())))
            .isEqualTo(false)
    }

    @Test
    fun execute_nonEmptyCharArray_returnsTrue() {
        assertThat(A2uiRequiredFunction.INSTANCE.execute(mapOf(ARG_VALUE to charArrayOf('a'))))
            .isEqualTo(true)
    }

    @Test
    fun execute_emptyFloatArray_returnsFalse() {
        assertThat(A2uiRequiredFunction.INSTANCE.execute(mapOf(ARG_VALUE to floatArrayOf())))
            .isEqualTo(false)
    }

    @Test
    fun execute_nonEmptyFloatArray_returnsTrue() {
        assertThat(A2uiRequiredFunction.INSTANCE.execute(mapOf(ARG_VALUE to floatArrayOf(1.0f))))
            .isEqualTo(true)
    }

    @Test
    fun execute_emptyShortArray_returnsFalse() {
        assertThat(A2uiRequiredFunction.INSTANCE.execute(mapOf(ARG_VALUE to shortArrayOf())))
            .isEqualTo(false)
    }

    @Test
    fun execute_nonEmptyShortArray_returnsTrue() {
        assertThat(A2uiRequiredFunction.INSTANCE.execute(mapOf(ARG_VALUE to shortArrayOf(1))))
            .isEqualTo(true)
    }

    @Test
    fun execute_nonNullBoolean_returnsTrue() {
        assertThat(A2uiRequiredFunction.INSTANCE.execute(mapOf(ARG_VALUE to true))).isEqualTo(true)
    }

    @Test
    fun execute_nonNullInteger_returnsTrue() {
        assertThat(A2uiRequiredFunction.INSTANCE.execute(mapOf(ARG_VALUE to 123))).isEqualTo(true)
    }

    @Test
    fun execute_missingValue_throwsValidationException() {
        assertThrows(A2uiException.A2uiValidationException::class.java) {
            A2uiRequiredFunction.INSTANCE.execute(emptyMap())
        }
    }

    private companion object {
        private const val ARG_VALUE = "value"
    }
}
