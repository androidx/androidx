/*
 * Copyright 2024 The Android Open Source Project
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

/**
 * Propositions for [String] subjects.
 *
 * @constructor Constructor for use by subclasses. If you want to create an instance of this class
 *   itself, call [check(...)][Subject.check].[that(actual)][StandardSubjectBuilder.that].
 */
public actual open class StringSubject
protected actual constructor(metadata: FailureMetadata, actual: String?) :
    ComparableSubject<String>(actual, metadata) {

    internal actual constructor(actual: String?, metadata: FailureMetadata) : this(metadata, actual)

    /** Fails if the string does not contain the given sequence. */
    public actual open fun contains(charSequence: CharSequence?): Unit =
        commonContains(charSequence)

    /** Fails if the string does not have the given length. */
    public actual open fun hasLength(expectedLength: Int): Unit = commonHasLength(expectedLength)

    /** Fails if the string is not equal to the zero-length "empty string." */
    public actual open fun isEmpty(): Unit = commonIsEmpty()

    /** Fails if the string is equal to the zero-length "empty string." */
    public actual open fun isNotEmpty(): Unit = commonIsNotEmpty()

    /** Fails if the string contains the given sequence. */
    public actual open fun doesNotContain(charSequence: CharSequence?): Unit =
        commonDoesNotContain(charSequence)

    /** Fails if the string does not start with the given string. */
    public actual open fun startsWith(string: String?): Unit = commonStartsWith(string)

    /** Fails if the string does not end with the given string. */
    public actual open fun endsWith(string: String?): Unit = commonEndsWith(string)

    /** Fails if the string does not match the given [regex]. */
    public actual open fun matches(regex: String?): Unit = commonMatches(regex)

    /** Fails if the string does not match the given [regex]. */
    public actual fun matches(regex: Regex?): Unit = commonMatches(regex)

    /** Fails if the string matches the given regex. */
    public actual open fun doesNotMatch(regex: String?): Unit = commonDoesNotMatch(regex)

    /** Fails if the string matches the given regex. */
    public actual fun doesNotMatch(regex: Regex?): Unit = commonDoesNotMatch(regex)

    /** Fails if the string does not contain a match on the given regex. */
    public actual open fun containsMatch(regex: String?): Unit = commonContainsMatch(regex)

    /** Fails if the string does not contain a match on the given regex. */
    public actual fun containsMatch(regex: Regex?): Unit = commonContainsMatch(regex)

    /** Fails if the string contains a match on the given regex. */
    public actual open fun doesNotContainMatch(regex: String?): Unit =
        commonDoesNotContainMatch(regex)

    /** Fails if the string contains a match on the given regex. */
    public actual fun doesNotContainMatch(regex: Regex?): Unit = commonDoesNotContainMatch(regex)

    /**
     * Returns a [StringSubject]-like instance that will ignore the case of the characters.
     *
     * Character equality ignoring case is defined as follows: Characters must be equal either after
     * calling [Char.lowercaseChar] or after calling [Char.uppercaseChar]. Note that this is
     * independent of any locale.
     */
    public actual open fun ignoringCase(): CaseInsensitiveStringComparison = commonIgnoringCase()

    public actual inner class CaseInsensitiveStringComparison internal actual constructor() {
        /**
         * Fails if the subject is not equal to the given sequence (while ignoring case). For the
         * purposes of this comparison, two strings are equal if any of the following is true:
         * * they are equal according to [String.equals] with `ignoreCase = true`
         * * they are both null
         *
         * Example: "abc" is equal to "ABC", but not to "abcd".
         */
        public actual fun isEqualTo(expected: String?): Unit =
            commonCaseInsensitiveStringComparisonIsEqualTo(expected)

        /**
         * Fails if the subject is equal to the given string (while ignoring case). The meaning of
         * equality is the same as for the [isEqualTo] method.
         */
        public actual fun isNotEqualTo(unexpected: String?): Unit =
            commonCaseInsensitiveStringComparisonIsNotEqualTo(unexpected)

        /** Fails if the string does not contain the given sequence (while ignoring case). */
        public actual fun contains(expected: CharSequence?): Unit =
            commonCaseInsensitiveStringComparisonContains(expected)

        /** Fails if the string contains the given sequence (while ignoring case). */
        public actual fun doesNotContain(expected: CharSequence?): Unit =
            commonCaseInsensitiveStringComparisonDoesNotContain(expected)
    }
}
