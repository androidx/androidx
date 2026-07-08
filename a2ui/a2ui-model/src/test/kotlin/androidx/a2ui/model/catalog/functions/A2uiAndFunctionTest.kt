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

class A2uiAndFunctionTest {

    @Test
    fun execute_allTrueBooleans_returnsTrue() {
        assertThat(A2uiAndFunction.INSTANCE.execute(mapOf(ARG_VALUES to listOf(true, true))))
            .isEqualTo(true)
    }

    @Test
    fun execute_mixedBooleans_returnsFalse() {
        assertThat(A2uiAndFunction.INSTANCE.execute(mapOf(ARG_VALUES to listOf(true, false))))
            .isEqualTo(false)
    }

    @Test
    fun execute_allTrueStrings_returnsTrue() {
        assertThat(A2uiAndFunction.INSTANCE.execute(mapOf(ARG_VALUES to listOf("true", "true"))))
            .isEqualTo(true)
    }

    @Test
    fun execute_mixedStrings_returnsFalse() {
        assertThat(A2uiAndFunction.INSTANCE.execute(mapOf(ARG_VALUES to listOf("true", "false"))))
            .isEqualTo(false)
    }

    @Test
    fun execute_mixedCaseTrueStrings_returnsTrue() {
        assertThat(A2uiAndFunction.INSTANCE.execute(mapOf(ARG_VALUES to listOf("True", "TRUE"))))
            .isEqualTo(true)
    }

    @Test
    fun execute_mixedCaseFalseStrings_returnsFalse() {
        assertThat(A2uiAndFunction.INSTANCE.execute(mapOf(ARG_VALUES to listOf("True", "False"))))
            .isEqualTo(false)
    }

    @Test
    fun execute_missingArguments_throwsValidationException() {
        assertThrows(A2uiException.A2uiValidationException::class.java) {
            A2uiAndFunction.INSTANCE.execute(emptyMap())
        }
    }

    @Test
    fun execute_invalidArgumentsNotAList_throwsValidationException() {
        assertThrows(A2uiException.A2uiValidationException::class.java) {
            A2uiAndFunction.INSTANCE.execute(mapOf(ARG_VALUES to "not-a-list"))
        }
    }

    @Test
    fun execute_invalidArgumentsListWithNull_throwsValidationException() {
        assertThrows(A2uiException.A2uiValidationException::class.java) {
            A2uiAndFunction.INSTANCE.execute(mapOf(ARG_VALUES to listOf(true, null)))
        }
    }

    @Test
    fun execute_invalidArgumentsListWithNonBoolean_throwsValidationException() {
        assertThrows(A2uiException.A2uiValidationException::class.java) {
            A2uiAndFunction.INSTANCE.execute(mapOf(ARG_VALUES to listOf(true, 123)))
        }
    }

    private companion object {
        private const val ARG_VALUES = "values"
    }
}
