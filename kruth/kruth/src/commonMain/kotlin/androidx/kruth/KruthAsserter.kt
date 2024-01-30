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

package androidx.kruth

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
internal class KruthAsserter(
    private val formatMessage: (String?) -> String?,
) {

    /**
     * Fails the current test with the specified message and cause exception.
     *
     * @param message the message to report.
     * @param cause the exception to set as the root cause of the reported failure.
     */
    fun fail(message: String? = null, cause: Throwable? = null): Nothing {
        kotlin.test.fail(message = formatMessage(message), cause = cause)
    }

    /**
     * Asserts that the specified value is `true`.
     *
     * @param message the message to report if the assertion fails.
     */
    fun assertTrue(actual: Boolean, message: String? = null) {
        contract { returns() implies actual }

        if (!actual) {
            fail(message)
        }
    }

    /**
     * Asserts that the specified value is `false`.
     *
     * @param message the message to report if the assertion fails.
     */
    fun assertFalse(actual: Boolean, message: String? = null) {
        contract { returns() implies !actual }

        if (actual) {
            fail(message)
        }
    }

    /**
     * Asserts that the specified values are equal.
     *
     * @param message the message to report if the assertion fails.
     */
    fun assertEquals(expected: Any?, actual: Any?, message: String? = null) {
        assertTrue(expected == actual, message)
    }

    /**
     * Asserts that the specified values are not equal.
     *
     * @param message the message to report if the assertion fails.
     */
    fun assertNotEquals(illegal: Any?, actual: Any?, message: String? = null) {
        assertFalse(illegal == actual, message)
    }

    /**
     * Asserts that the specified value is `null`.
     *
     * @param message the message to report if the assertion fails.
     */
    fun assertNull(actual: Any?, message: String? = null) {
        contract { returns() implies (actual == null) }
        assertTrue(actual == null, message)
    }

    /**
     * Asserts that the specified value is not `null`.
     *
     * @param message the message to report if the assertion fails.
     */
    fun <T : Any> assertNotNull(actual: T?, message: String? = null): T {
        contract { returns() implies (actual != null) }
        assertFalse(actual == null, message)

        return actual
    }
}
