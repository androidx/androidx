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

package androidx.a2ui.core.protocol

import com.google.common.testing.EqualsTester
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class A2uiExceptionTest {

    @Test
    fun validationFailed_argumentsProvided_propertiesAreStoredCorrectly() {
        val valErr = A2uiException.A2uiValidationException(TEST_MESSAGE, TEST_PATH)
        assertThat(valErr.code).isEqualTo("VALIDATION_FAILED")
        assertThat(valErr.message).isEqualTo(TEST_MESSAGE)
        assertThat(valErr.context).isEqualTo(mapOf("path" to TEST_PATH))
    }

    @Test
    fun validationException_toString_returnsExpectedFormat() {
        val ex = A2uiException.A2uiValidationException(TEST_MESSAGE, TEST_PATH)
        val str = (ex as Any).toString()
        assertThat(str)
            .isEqualTo(
                "A2uiValidationException(code=VALIDATION_FAILED, message=$TEST_MESSAGE, path=$TEST_PATH)"
            )
    }

    @Test
    fun runtimeError_allArgumentsProvided_allPropertiesAreStored() {
        val runErr = A2uiException.A2uiRuntimeException(TEST_MESSAGE, TEST_CONTEXT)
        assertThat(runErr.code).isEqualTo("RUNTIME_ERROR")
        assertThat(runErr.message).isEqualTo(TEST_MESSAGE)
        assertThat(runErr.context).isEqualTo(TEST_CONTEXT)
    }

    @Test
    fun runtimeError_onlyRequiredArgumentsProvided_contextIsDefault() {
        val minErr = A2uiException.A2uiRuntimeException(TEST_MESSAGE)
        assertThat(minErr.context).isEmpty()
    }

    @Test
    fun runtimeException_toString_returnsExpectedFormat() {
        val ex = A2uiException.A2uiRuntimeException(TEST_MESSAGE, TEST_CONTEXT)
        val str = (ex as Any).toString()
        assertThat(str)
            .isEqualTo(
                "A2uiRuntimeException(code=RUNTIME_ERROR, message=$TEST_MESSAGE, $TEST_KEY=$TEST_VALUE)"
            )
    }

    @Test
    fun runtimeException_toString_emptyContext_returnsExpectedFormat() {
        val ex = A2uiException.A2uiRuntimeException(TEST_MESSAGE)
        val str = (ex as Any).toString()
        assertThat(str).isEqualTo("A2uiRuntimeException(code=RUNTIME_ERROR, message=$TEST_MESSAGE)")
    }

    @Test
    fun a2uiException_equalsAndHashCode_contracts() {
        EqualsTester()
            .addEqualityGroup(
                A2uiException.A2uiValidationException(TEST_MESSAGE, TEST_PATH),
                A2uiException.A2uiValidationException(TEST_MESSAGE, TEST_PATH),
            )
            .addEqualityGroup(A2uiException.A2uiValidationException(TEST_MESSAGE_2, TEST_PATH))
            .addEqualityGroup(A2uiException.A2uiValidationException(TEST_MESSAGE, TEST_PATH_2))
            .addEqualityGroup(
                A2uiException.A2uiRuntimeException(TEST_MESSAGE, TEST_CONTEXT),
                A2uiException.A2uiRuntimeException(TEST_MESSAGE, TEST_CONTEXT),
            )
            .addEqualityGroup(A2uiException.A2uiRuntimeException(TEST_MESSAGE_2, TEST_CONTEXT))
            .addEqualityGroup(A2uiException.A2uiRuntimeException(TEST_MESSAGE, TEST_CONTEXT_2))
            .addEqualityGroup(
                A2uiException.A2uiRuntimeException(TEST_MESSAGE, emptyMap()),
                A2uiException.A2uiRuntimeException(TEST_MESSAGE),
            )
            .testEquals()
    }

    companion object {
        private const val TEST_PATH = "/test/path"
        private const val TEST_PATH_2 = "/test/path-2"
        private const val TEST_MESSAGE = "test-message"
        private const val TEST_MESSAGE_2 = "test-message-2"

        private const val TEST_KEY = "key"
        private const val TEST_KEY_2 = "key-2"
        private const val TEST_VALUE = "value"
        private const val TEST_VALUE_2 = "value-2"

        private val TEST_CONTEXT = mapOf(TEST_KEY to TEST_VALUE)
        private val TEST_CONTEXT_2 = mapOf(TEST_KEY_2 to TEST_VALUE_2)
    }
}
