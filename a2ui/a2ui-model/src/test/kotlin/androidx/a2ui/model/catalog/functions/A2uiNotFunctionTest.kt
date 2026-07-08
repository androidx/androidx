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

class A2uiNotFunctionTest {

    @Test
    fun execute_trueBoolean_returnsFalse() {
        assertThat(A2uiNotFunction.INSTANCE.execute(mapOf(ARG_VALUE to true))).isEqualTo(false)
    }

    @Test
    fun execute_falseBoolean_returnsTrue() {
        assertThat(A2uiNotFunction.INSTANCE.execute(mapOf(ARG_VALUE to false))).isEqualTo(true)
    }

    @Test
    fun execute_trueString_returnsFalse() {
        assertThat(A2uiNotFunction.INSTANCE.execute(mapOf(ARG_VALUE to "true"))).isEqualTo(false)
    }

    @Test
    fun execute_falseString_returnsTrue() {
        assertThat(A2uiNotFunction.INSTANCE.execute(mapOf(ARG_VALUE to "false"))).isEqualTo(true)
    }

    @Test
    fun execute_mixedCaseTrueString_returnsFalse() {
        assertThat(A2uiNotFunction.INSTANCE.execute(mapOf(ARG_VALUE to "True"))).isEqualTo(false)
    }

    @Test
    fun execute_mixedCaseFalseString_returnsTrue() {
        assertThat(A2uiNotFunction.INSTANCE.execute(mapOf(ARG_VALUE to "False"))).isEqualTo(true)
    }

    @Test
    fun execute_missingValue_throwsValidationException() {
        assertThrows(A2uiException.A2uiValidationException::class.java) {
            A2uiNotFunction.INSTANCE.execute(emptyMap())
        }
    }

    @Test
    fun execute_invalidValueType_throwsValidationException() {
        assertThrows(A2uiException.A2uiValidationException::class.java) {
            A2uiNotFunction.INSTANCE.execute(mapOf(ARG_VALUE to "not-a-boolean"))
        }
    }

    private companion object {
        private const val ARG_VALUE = "value"
    }
}
