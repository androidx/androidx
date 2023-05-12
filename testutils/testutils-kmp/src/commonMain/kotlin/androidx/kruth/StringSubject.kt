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

/**
 * Propositions for string subjects.
 */
class StringSubject(
    actual: String?,
    messageToPrepend: String? = null,
) : ComparableSubject<String>(actual, messageToPrepend) {

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

    /**
     * Returns a [StringSubject]-like instance that will ignore the case of the characters.
     *
     * Character equality ignoring case is defined as follows: Characters must be equal either
     * after calling [Character.toLowerCase] or after calling [Character.toUpperCase].
     * Note that this is independent of any locale.
     */
    fun ignoringCase(): CaseInsensitiveStringComparison =
        CaseInsensitiveStringComparison()

    inner class CaseInsensitiveStringComparison internal constructor() {
        /**
         * Fails if the subject is not equal to the given sequence (while ignoring case). For the
         * purposes of this comparison, two strings are equal if any of the following is true:
         *
         *  * they are equal according to [String.equals] with `ignoreCase = true`
         *  * they are both null
         *
         * Example: "abc" is equal to "ABC", but not to "abcd".
         */
        fun isEqualTo(expected: String?) {
            when {
                (actual == null) && (expected != null) ->
                    fail("Expected a string equal to \"$expected\" (case is ignored), but was null")

                (expected == null) && (actual != null) ->
                    fail("Expected a string that is null (null reference), but was \"$actual\"")

                !actual.equals(expected, ignoreCase = true) ->
                    fail(
                        "Expected a string equal to \"$expected\" (case is ignored), " +
                            "but was \"$actual\""
                    )
            }
        }

        /**
         * Fails if the subject is equal to the given string (while ignoring case). The meaning of
         * equality is the same as for the [isEqualTo] method.
         */
        fun isNotEqualTo(unexpected: String?) {
            when {
                (actual == null) && (unexpected == null) ->
                    fail("Expected a string not equal to null (null reference), but it was null")

                actual.equals(unexpected, ignoreCase = true) ->
                    fail(
                        "Expected a string not equal to \"$unexpected\" (case is ignored), " +
                            "but it was equal. Actual string: \"$actual\"."
                    )
            }
        }

        /** Fails if the string does not contain the given sequence (while ignoring case).  */
        fun contains(expected: CharSequence?) {
            requireNonNull(expected)

            when {
                actual == null ->
                    fail(
                        "Expected a string that contains \"$expected\" (case is ignored), " +
                            "but was null"
                    )

                !actual.contains(expected, ignoreCase = true) ->
                    fail("Expected to contain \"$expected\" (case is ignored), but was \"$actual\"")
            }
        }

        /** Fails if the string contains the given sequence (while ignoring case).  */
        fun doesNotContain(expected: CharSequence?) {
            checkNotNull(expected)

            when {
                actual == null ->
                    fail(
                        "Expected a string that does not contain \"$expected\" " +
                            "(case is ignored), but was null"
                    )

                actual.contains(expected, ignoreCase = true) ->
                    fail(
                        "Expected a string that does not contain \"$expected\" " +
                            "(case is ignored), but it was. Actual string: \"$actual\"."
                    )
            }
        }
    }
}
