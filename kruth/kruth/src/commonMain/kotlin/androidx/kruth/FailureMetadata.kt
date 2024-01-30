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
import kotlin.jvm.JvmStatic

@OptIn(ExperimentalContracts::class)
data class FailureMetadata internal constructor(
    val failureStrategy: FailureStrategy = FailureStrategy { failure -> throw failure },
    val messagesToPrepend: List<String> = emptyList(),
) {
    companion object {
        @JvmStatic
        fun forFailureStrategy(failureStrategy: FailureStrategy): FailureMetadata {
            return FailureMetadata(
                failureStrategy
            )
        }
    }

    internal fun fail(message: String? = null): Nothing {
        // TODO: change to AssertionError that takes in a cause when upgraded to 1.9.20
        failureStrategy.fail(AssertionError(formatMessage(message)))
    }

    internal fun withMessage(messageToPrepend: String): FailureMetadata =
        copy(messagesToPrepend = messagesToPrepend + messageToPrepend)

    internal fun formatMessage(vararg messages: String?): String =
        (messagesToPrepend + messages.filterNotNull()).joinToString(separator = "\n")

    /**
     * Asserts that the specified value is `true`.
     *
     * @param message the message to report if the assertion fails.
     */
    internal fun assertTrue(actual: Boolean, message: String? = null) {
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
    internal fun assertFalse(actual: Boolean, message: String? = null) {
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
    internal fun assertEquals(expected: Any?, actual: Any?, message: String? = null) {
        assertTrue(expected == actual, message)
    }

    /**
     * Asserts that the specified values are not equal.
     *
     * @param message the message to report if the assertion fails.
     */
    internal fun assertNotEquals(illegal: Any?, actual: Any?, message: String? = null) {
        assertFalse(illegal == actual, message)
    }

    /**
     * Asserts that the specified value is `null`.
     *
     * @param message the message to report if the assertion fails.
     */
    internal fun assertNull(actual: Any?, message: String? = null) {
        contract { returns() implies (actual == null) }
        assertTrue(actual == null, message)
    }

    /**
     * Asserts that the specified value is not `null`.
     *
     * @param message the message to report if the assertion fails.
     */
    internal fun <T : Any> assertNotNull(actual: T?, message: String? = null): T {
        contract { returns() implies (actual != null) }
        assertFalse(actual == null, message)

        return actual
    }
}
