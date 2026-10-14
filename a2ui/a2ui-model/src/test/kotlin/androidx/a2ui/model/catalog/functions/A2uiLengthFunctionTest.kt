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

class A2uiLengthFunctionTest {

    @Test
    fun execute_withinBounds_returnsTrue() {
        assertThat(
                A2uiLengthFunction.INSTANCE.execute(
                    mapOf(ARG_VALUE to "hello", ARG_MIN to 1, ARG_MAX to 10)
                )
            )
            .isEqualTo(true)
    }

    @Test
    fun execute_lessThanMin_returnsFalse() {
        assertThat(A2uiLengthFunction.INSTANCE.execute(mapOf(ARG_VALUE to "hello", ARG_MIN to 6)))
            .isEqualTo(false)
    }

    @Test
    fun execute_moreThanMax_returnsFalse() {
        assertThat(A2uiLengthFunction.INSTANCE.execute(mapOf(ARG_VALUE to "hello", ARG_MAX to 1)))
            .isEqualTo(false)
    }

    @Test
    fun execute_missingValue_throwsValidationException() {
        assertThrows(A2uiException.A2uiValidationException::class.java) {
            A2uiLengthFunction.INSTANCE.execute(emptyMap())
        }
    }

    @Test
    fun execute_invalidMinType_throwsValidationException() {
        assertThrows(A2uiException.A2uiValidationException::class.java) {
            A2uiLengthFunction.INSTANCE.execute(
                mapOf(ARG_VALUE to "hello", ARG_MIN to "not-an-int")
            )
        }
    }

    @Test
    fun definition_returnsCompanionDefinition() {
        assertThat(A2uiLengthFunction.INSTANCE.definition)
            .isSameInstanceAs(A2uiLengthFunction.DEFINITION)
        assertThat(A2uiLengthFunction.DEFINITION.name).isEqualTo("length")
    }

    @Test
    fun typedExecute_checksBoundsInclusively() {
        val function = A2uiLengthFunction.INSTANCE

        assertThat(function.typedExecute(value = "hello", min = 0, max = Int.MAX_VALUE)).isTrue()
        assertThat(function.typedExecute(value = "hello", min = 5, max = 5)).isTrue()
        assertThat(function.typedExecute(value = "hello", min = 6, max = Int.MAX_VALUE)).isFalse()
        assertThat(function.typedExecute(value = "hello", min = 0, max = 4)).isFalse()
    }

    private companion object {
        private const val ARG_VALUE = "value"
        private const val ARG_MIN = "min"
        private const val ARG_MAX = "max"
    }
}
