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

class A2uiEmailFunctionTest {

    @Test
    fun execute_validStandardEmail_returnsTrue() {
        assertThat(A2uiEmailFunction.INSTANCE.execute(mapOf(ARG_VALUE to "test@example.com")))
            .isEqualTo(true)
    }

    @Test
    fun execute_validEmailWithSubdomain_returnsTrue() {
        assertThat(A2uiEmailFunction.INSTANCE.execute(mapOf(ARG_VALUE to "test@sub.example.com")))
            .isEqualTo(true)
    }

    @Test
    fun execute_validEmailWithPlus_returnsTrue() {
        assertThat(A2uiEmailFunction.INSTANCE.execute(mapOf(ARG_VALUE to "user+tag@example.com")))
            .isEqualTo(true)
    }

    @Test
    fun execute_validEmailWithPeriodInLocalPart_returnsTrue() {
        assertThat(A2uiEmailFunction.INSTANCE.execute(mapOf(ARG_VALUE to "first.last@example.com")))
            .isEqualTo(true)
    }

    @Test
    fun execute_invalidEmailMissingDomain_returnsFalse() {
        assertThat(A2uiEmailFunction.INSTANCE.execute(mapOf(ARG_VALUE to "test@"))).isEqualTo(false)
    }

    @Test
    fun execute_invalidEmailMissingLocalPart_returnsFalse() {
        assertThat(A2uiEmailFunction.INSTANCE.execute(mapOf(ARG_VALUE to "@example.com")))
            .isEqualTo(false)
    }

    @Test
    fun execute_invalidEmailMissingAtSymbol_returnsFalse() {
        assertThat(A2uiEmailFunction.INSTANCE.execute(mapOf(ARG_VALUE to "testexample.com")))
            .isEqualTo(false)
    }

    @Test
    fun execute_invalidEmailWithSpaces_returnsFalse() {
        assertThat(A2uiEmailFunction.INSTANCE.execute(mapOf(ARG_VALUE to "test @example.com")))
            .isEqualTo(false)
    }

    @Test
    fun execute_invalidEmailWithMultipleAtSymbols_returnsFalse() {
        assertThat(A2uiEmailFunction.INSTANCE.execute(mapOf(ARG_VALUE to "test@sub@example.com")))
            .isEqualTo(false)
    }

    @Test
    fun execute_missingArguments_throwsValidationException() {
        assertThrows(A2uiException.A2uiValidationException::class.java) {
            A2uiEmailFunction.INSTANCE.execute(emptyMap())
        }
    }

    private companion object {
        private const val ARG_VALUE = "value"
    }
}
