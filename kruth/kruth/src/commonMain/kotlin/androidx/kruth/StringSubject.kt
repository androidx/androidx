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

import androidx.kruth.Fact.Companion.fact
import androidx.kruth.Fact.Companion.simpleFact

/**
 * Propositions for string subjects.
 */
class StringSubject internal constructor(
    actual: String?,
    metadata: FailureMetadata = FailureMetadata(),
) : ComparableSubject<String>(actual = actual, metadata = metadata),
    PlatformStringSubject by PlatformStringSubjectImpl(actual, metadata) {

    /**
     * Fails if the string does not contain the given sequence.
     */
    fun contains(charSequence: CharSequence) {
        metadata.assertNotNull(actual)

        metadata.assertTrue(actual.contains(charSequence)) {
            "Expected to contain \"$charSequence\", but was: \"$actual\""
        }
    }

    /** Fails if the string does not have the given length.  */
    fun hasLength(expectedLength: Int) {
        metadata.assertNotNull(actual)

        metadata.assertTrue(actual.length == expectedLength) {
            "Expected to have length $expectedLength, but was: \"$actual\""
        }
    }

    /** Fails if the string is not equal to the zero-length "empty string."  */
    fun isEmpty() {
        metadata.assertNotNull(actual)
        if (actual.isNotEmpty()) {
            metadata.fail(
                """
                    expected to be empty
                    | but was $actual
                """.trimMargin()
            )
        }
    }

    /** Fails if the string is equal to the zero-length "empty string."  */
    fun isNotEmpty() {
        metadata.assertNotNull(actual)
        if (actual.isEmpty()) {
            metadata.fail("expected not to be empty")
        }
    }

    /** Fails if the string contains the given sequence.  */
    fun doesNotContain(string: CharSequence) {
        metadata.assertNotNull(actual) { "expected a string that does not contain $string" }

        if (actual.contains(string)) {
            metadata.fail(
                """
                    expected not to contain $string
                    | but was $actual
                """.trimMargin()
            )
        }
    }

    /** Fails if the string does not start with the given string.  */
    fun startsWith(string: String) {
        metadata.assertNotNull(actual) { "expected a string that starts with $string" }

        if (!actual.startsWith(string)) {
            metadata.fail(
                """
                    expected to start with $string
                    | but was $actual
                """.trimMargin()
            )
        }
    }

    /** Fails if the string does not end with the given string.  */
    fun endsWith(string: String) {
        metadata.assertNotNull(actual) { "expected a string that ends with $string" }

        if (!actual.endsWith(string)) {
            metadata.fail(
                """
                    expected to end with $string
                    | but was $actual
                """.trimMargin()
            )
        }
    }

    /** Fails if the string does not match the given [regex]. */
    fun matches(regex: String) {
        matchesImpl(regex.toRegex()) {
            "Looks like you want to use .isEqualTo() for an exact equality assertion."
        }
    }

    /** Fails if the string does not match the given [regex]. */
    fun matches(regex: Regex) {
        matchesImpl(regex) {
            "If you want an exact equality assertion you can escape your regex with Regex.escape()."
        }
    }

    /** Fails if the string matches the given regex.  */
    fun doesNotMatch(regex: String) {
        doesNotMatchImpl(regex.toRegex())
    }

    /** Fails if the string matches the given regex.  */
    fun doesNotMatch(regex: Regex) {
        doesNotMatchImpl(regex)
    }

    /** Fails if the string does not contain a match on the given regex.  */
    fun containsMatch(regex: Regex) {
        containsMatchImpl(regex)
    }

    /** Fails if the string does not contain a match on the given regex.  */
    fun containsMatch(regex: String) {
        containsMatchImpl(regex.toRegex())
    }

    /** Fails if the string contains a match on the given regex.  */
    fun doesNotContainMatch(regex: Regex) {
        doesNotContainMatchImpl(regex)
    }

    /** Fails if the string contains a match on the given regex.  */
    fun doesNotContainMatch(regex: String) {
        if (actual == null) {
            failWithActual("expected a string that does not contain a match for", regex)
        }

        if (regex.toRegex().containsMatchIn(actual)) {
            failWithActual("expected not to contain a match for", regex)
        }
    }

    /**
     * Returns a [StringSubject]-like instance that will ignore the case of the characters.
     *
     * Character equality ignoring case is defined as follows: Characters must be equal either
     * after calling [Char.lowercaseChar] or after calling [Char.uppercaseChar].
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
                    metadata.fail(
                        "Expected a string equal to \"$expected\" (case is ignored), but was null"
                    )

                (expected == null) && (actual != null) ->
                    metadata.fail(
                        "Expected a string that is null (null reference), but was \"$actual\""
                    )

                !actual.equals(expected, ignoreCase = true) ->
                    metadata.fail(
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
                    metadata.fail(
                        "Expected a string not equal to null (null reference), but it was null"
                    )

                actual.equals(unexpected, ignoreCase = true) ->
                    metadata.fail(
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
                    metadata.fail(
                        "Expected a string that contains \"$expected\" (case is ignored), " +
                            "but was null"
                    )

                !actual.contains(expected, ignoreCase = true) ->
                    metadata.fail(
                        "Expected to contain \"$expected\" (case is ignored), but was \"$actual\""
                    )
            }
        }

        /** Fails if the string contains the given sequence (while ignoring case).  */
        fun doesNotContain(expected: CharSequence?) {
            requireNonNull(expected)

            when {
                actual == null ->
                    metadata.fail(
                        "Expected a string that does not contain \"$expected\" " +
                            "(case is ignored), but was null"
                    )

                actual.contains(expected, ignoreCase = true) ->
                    metadata.fail(
                        "Expected a string that does not contain \"$expected\" " +
                            "(case is ignored), but it was. Actual string: \"$actual\"."
                    )
            }
        }
    }
}

internal inline fun Subject<String>.matchesImpl(regex: Regex, equalToStringErrorMsg: () -> String) {
    if (actual == null) {
        failWithActualInternal("Expected a string that matches", regex)
    }

    if (actual.matches(regex)) {
        return
    }

    if (regex.toString() == actual) {
        failWithoutActualInternal(
            fact("Expected to match", regex),
            fact("but was", actual),
            simpleFact(equalToStringErrorMsg()),
        )
    } else {
        failWithActualInternal("Expected to match", regex);
    }
}

internal fun Subject<String>.doesNotMatchImpl(regex: Regex) {
    if (actual == null) {
        failWithActualInternal("Expected a string that does not match", regex)
    }

    if (actual.matches(regex)) {
        failWithActualInternal("Expected not to match", regex)
    }
}

internal fun Subject<String>.containsMatchImpl(regex: Regex) {
    if (actual == null) {
        failWithActualInternal("Expected a string that contains a match for", regex)
    }

    if (!regex.containsMatchIn(actual)) {
        failWithActualInternal("Expected to contain a match for", regex)
    }
}

internal fun Subject<String>.doesNotContainMatchImpl(regex: Regex) {
    if (actual == null) {
        failWithActualInternal("expected a string that does not contain a match for", regex)
    }

    val result = regex.find(actual)
    if (result != null) {
        failWithoutActualInternal(
            fact("Expected not to contain a match for", regex),
            fact("but contained", result.value),
            fact("Full string", actual)
        )
    }
}

internal expect interface PlatformStringSubject

internal expect class PlatformStringSubjectImpl(
    actual: String?,
    metadata: FailureMetadata,
) : Subject<String>, PlatformStringSubject
