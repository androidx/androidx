/*
 * Copyright 2022 The Android Open Source Project
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

import kotlin.test.assertContains
import kotlin.test.assertNotNull
import kotlin.test.fail

/**
 * Propositions for string subjects.
 */
class StringSubject(actual: String?) : ComparableSubject<String>(actual) {

    /**
     * Fails if the string does not contain the given sequence.
     */
    fun contains(charSequence: CharSequence) {
        assertNotNull(actual)
        assertContains(actual, charSequence)
    }

    /** Fails if the string does not have the given length.  */
    fun hasLength(expectedLength: Int) {
        assertNotNull(actual)
        assertThat(actual.length).isEqualTo(expectedLength)
    }

    /** Fails if the string is not equal to the zero-length "empty string."  */
    fun isEmpty() {
        assertNotNull(actual)
        if (actual.isNotEmpty()) {
            fail(
                """
                    expected to be empty
                    | but was $actual
                """.trimMargin()
            )
        }
    }

    /** Fails if the string is equal to the zero-length "empty string."  */
    fun isNotEmpty() {
        assertNotNull(actual)
        if (actual.isEmpty()) {
            fail("expected not to be empty")
        }
    }

    /** Fails if the string contains the given sequence.  */
    fun doesNotContain(string: CharSequence) {
        assertNotNull(actual, "expected a string that does not contain $string")

        if (actual.contains(string)) {
            fail(
                """
                    expected not to contain $string
                    | but was $actual
                """.trimMargin()
            )
        }
    }

    /** Fails if the string does not start with the given string.  */
    fun startsWith(string: String) {
        assertNotNull(actual, "expected a string that starts with $string")

        if (!actual.startsWith(string)) {
            fail(
                """
                    expected to start with $string
                    | but was $actual
                """.trimMargin()
            )
        }
    }

    /** Fails if the string does not end with the given string.  */
    fun endsWith(string: String) {
        assertNotNull(actual, "expected a string that ends with $string")

        if (!actual.endsWith(string)) {
            fail(
                """
                    expected to end with $string
                    | but was $actual
                """.trimMargin()
            )
        }
    }
}
