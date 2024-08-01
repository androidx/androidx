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
 * Propositions for [String] subjects.
 *
 * @constructor Constructor for use by subclasses. If you want to create an instance of this class
 *   itself, call [check(...)][Subject.check].[that(actual)][StandardSubjectBuilder.that].
 */
expect open class StringSubject protected constructor(metadata: FailureMetadata, actual: String?) :
    ComparableSubject<String> {

    internal constructor(actual: String?, metadata: FailureMetadata)

    /** Fails if the string does not contain the given sequence. */
    open fun contains(charSequence: CharSequence)

    /** Fails if the string does not have the given length. */
    open fun hasLength(expectedLength: Int)

    /** Fails if the string is not equal to the zero-length "empty string." */
    open fun isEmpty()

    /** Fails if the string is equal to the zero-length "empty string." */
    open fun isNotEmpty()

    /** Fails if the string contains the given sequence. */
    open fun doesNotContain(charSequence: CharSequence)

    /** Fails if the string does not start with the given string. */
    open fun startsWith(string: String)

    /** Fails if the string does not end with the given string. */
    open fun endsWith(string: String)

    /** Fails if the string does not match the given [regex]. */
    open fun matches(regex: String)

    /** Fails if the string does not match the given [regex]. */
    fun matches(regex: Regex)

    /** Fails if the string matches the given regex. */
    open fun doesNotMatch(regex: String)

    /** Fails if the string matches the given regex. */
    fun doesNotMatch(regex: Regex)

    /** Fails if the string does not contain a match on the given regex. */
    open fun containsMatch(regex: String)

    /** Fails if the string does not contain a match on the given regex. */
    fun containsMatch(regex: Regex)

    /** Fails if the string contains a match on the given regex. */
    open fun doesNotContainMatch(regex: String)

    /** Fails if the string contains a match on the given regex. */
    fun doesNotContainMatch(regex: Regex)

    /**
     * Returns a [StringSubject]-like instance that will ignore the case of the characters.
     *
     * Character equality ignoring case is defined as follows: Characters must be equal either after
     * calling [Char.lowercaseChar] or after calling [Char.uppercaseChar]. Note that this is
     * independent of any locale.
     */
    open fun ignoringCase(): CaseInsensitiveStringComparison

    inner class CaseInsensitiveStringComparison internal constructor() {
        /**
         * Fails if the subject is not equal to the given sequence (while ignoring case). For the
         * purposes of this comparison, two strings are equal if any of the following is true:
         * * they are equal according to [String.equals] with `ignoreCase = true`
         * * they are both null
         *
         * Example: "abc" is equal to "ABC", but not to "abcd".
         */
        fun isEqualTo(expected: String?)

        /**
         * Fails if the subject is equal to the given string (while ignoring case). The meaning of
         * equality is the same as for the [isEqualTo] method.
         */
        fun isNotEqualTo(unexpected: String?)

        /** Fails if the string does not contain the given sequence (while ignoring case). */
        fun contains(expected: CharSequence?)

        /** Fails if the string contains the given sequence (while ignoring case). */
        fun doesNotContain(expected: CharSequence)
    }
}

/** Fails if the string does not contain the given sequence. */
internal fun StringSubject.commonContains(charSequence: CharSequence) {
    if (actual == null) {
        failWithActualInternal("expected a string that contains", charSequence)
    } else if (!actual.contains(charSequence)) {
        failWithActualInternal("expected to contain", charSequence)
    }
}

/** Fails if the string does not have the given length. */
internal fun StringSubject.commonHasLength(expectedLength: Int) {
    require(expectedLength >= 0) { "expectedLength($expectedLength) must be >= 0" }
    checkInternal("length").that(requireNonNull(actual).length).isEqualTo(expectedLength)
}

/** Fails if the string is not equal to the zero-length "empty string." */
internal fun StringSubject.commonIsEmpty() {
    if (actual == null) {
        failWithActualInternal(simpleFact("expected an empty string"))
    } else if (actual.isNotEmpty()) {
        failWithActualInternal(simpleFact("expected to be string"))
    }
}

/** Fails if the string is equal to the zero-length "empty string." */
internal fun StringSubject.commonIsNotEmpty() {
    if (actual == null) {
        failWithActualInternal(simpleFact("expected a non-empty string"))
    } else if (actual.isEmpty()) {
        failWithoutActualInternal(simpleFact("expected not to be empty"))
    }
}

/** Fails if the string contains the given sequence. */
internal fun StringSubject.commonDoesNotContain(charSequence: CharSequence) {
    if (actual == null) {
        failWithActualInternal("expected a string that does not contain", charSequence)
    } else if (actual.contains(charSequence)) {
        failWithActualInternal("expected not to contain", charSequence)
    }
}

/** Fails if the string does not start with the given string. */
internal fun StringSubject.commonStartsWith(string: String) {
    if (actual == null) {
        failWithActualInternal("expected a string that starts with", string)
    } else if (!actual.startsWith(string)) {
        failWithActualInternal("expected to start with", string)
    }
}

/** Fails if the string does not end with the given string. */
internal fun StringSubject.commonEndsWith(string: String) {
    if (actual == null) {
        failWithActualInternal("expected a string that ends with", string)
    } else if (!actual.endsWith(string)) {
        failWithActualInternal("expected to end with", string)
    }
}

/** Fails if the string does not match the given [regex]. */
internal fun StringSubject.commonMatches(regex: String) {
    matchesImpl(regex.toRegex()) {
        "Looks like you want to use .isEqualTo() for an exact equality assertion."
    }
}

/** Fails if the string does not match the given [regex]. */
internal fun StringSubject.commonMatches(regex: Regex) {
    matchesImpl(regex) {
        "If you want an exact equality assertion you can escape your regex with Regex.escape()."
    }
}

/** Fails if the string matches the given regex. */
internal fun StringSubject.commonDoesNotMatch(regex: String) {
    doesNotMatchImpl(regex.toRegex())
}

/** Fails if the string matches the given regex. */
internal fun StringSubject.commonDoesNotMatch(regex: Regex) {
    doesNotMatchImpl(regex)
}

/** Fails if the string does not contain a match on the given regex. */
internal fun StringSubject.commonContainsMatch(regex: String) {
    containsMatchImpl(regex.toRegex())
}

/** Fails if the string does not contain a match on the given regex. */
internal fun StringSubject.commonContainsMatch(regex: Regex) {
    containsMatchImpl(regex)
}

/** Fails if the string contains a match on the given regex. */
internal fun StringSubject.commonDoesNotContainMatch(regex: String) {
    if (actual == null) {
        failWithActualInternal("expected a string that does not contain a match for", regex)
    } else if (regex.toRegex().containsMatchIn(actual)) {
        failWithActualInternal("expected not to contain a match for", regex)
    }
}

/** Fails if the string contains a match on the given regex. */
internal fun StringSubject.commonDoesNotContainMatch(regex: Regex) {
    doesNotContainMatchImpl(regex)
}

internal fun StringSubject.commonIgnoringCase(): StringSubject.CaseInsensitiveStringComparison =
    CaseInsensitiveStringComparison()

internal fun StringSubject.commonCaseInsensitiveStringComparisonIsEqualTo(expected: String?) {
    if ((actual == null) && (expected != null)) {
        failWithoutActualInternal(
            fact("expected a string that is equal to", expected),
            fact("but was", actual),
            simpleFact("(case is ignored)")
        )
    } else if ((expected == null) && (actual != null)) {
        failWithoutActualInternal(
            fact("expected", "null (null reference)"),
            fact("but was", actual),
            simpleFact("(case is ignored)")
        )
    } else if (!actual.equals(expected, ignoreCase = true)) {
        failWithoutActualInternal(
            fact("expected", expected),
            fact("but was", actual),
            simpleFact("(case is ignored)")
        )
    }
}

internal fun StringSubject.commonCaseInsensitiveStringComparisonIsNotEqualTo(unexpected: String?) {
    if ((actual == null) && (unexpected == null)) {
        failWithoutActualInternal(
            fact("expected a string that is not equal to", "null (null reference)"),
            simpleFact("(case is ignored)")
        )
    } else if (actual.equals(unexpected, ignoreCase = true)) {
        failWithoutActualInternal(
            fact("expected not to be", unexpected),
            fact("but was", actual),
            simpleFact("(case is ignored)")
        )
    }
}

/** Fails if the string does not contain the given sequence (while ignoring case). */
internal fun StringSubject.commonCaseInsensitiveStringComparisonContains(expected: CharSequence?) {
    requireNonNull(expected)

    if (actual == null) {
        failWithoutActualInternal(
            fact("expected a string that contains", expected),
            fact("but was", actual),
            simpleFact("(case is ignored)")
        )
    } else if (!actual.contains(expected, ignoreCase = true)) {
        failWithoutActualInternal(
            fact("expected to contain", expected),
            fact("but was", actual),
            simpleFact("(case is ignored)")
        )
    }
}

/** Fails if the string contains the given sequence (while ignoring case). */
internal fun StringSubject.commonCaseInsensitiveStringComparisonDoesNotContain(
    expected: CharSequence
) {
    if (actual == null) {
        failWithoutActualInternal(
            fact("expected a string that does not contain", expected),
            fact("but was", actual),
            simpleFact("(case is ignored)")
        )
    } else if (actual.contains(expected, ignoreCase = true)) {
        failWithoutActualInternal(
            fact("expected not to contain", expected),
            fact("but was", actual),
            simpleFact("(case is ignored)")
        )
    }
}

internal inline fun Subject<String>.matchesImpl(regex: Regex, equalToStringErrorMsg: () -> String) {
    if (actual == null) {
        failWithActualInternal("Expected a string that matches", regex)
    } else if (actual.matches(regex)) {
        return
    }

    if (regex.toString() == actual) {
        failWithoutActualInternal(
            fact("Expected to match", regex),
            fact("but was", actual),
            simpleFact(equalToStringErrorMsg()),
        )
    } else {
        failWithActualInternal("Expected to match", regex)
    }
}

internal fun Subject<String>.doesNotMatchImpl(regex: Regex) {
    if (actual == null) {
        failWithActualInternal("Expected a string that does not match", regex)
    } else if (actual.matches(regex)) {
        failWithActualInternal("Expected not to match", regex)
    }
}

internal fun Subject<String>.containsMatchImpl(regex: Regex) {
    if (actual == null) {
        failWithActualInternal("Expected a string that contains a match for", regex)
    } else if (!regex.containsMatchIn(actual)) {
        failWithActualInternal("Expected to contain a match for", regex)
    }
}

internal fun Subject<String>.doesNotContainMatchImpl(regex: Regex) {
    if (actual == null) {
        failWithActualInternal("expected a string that does not contain a match for", regex)
        return
    }

    val result = regex.find(actual)
    if (result != null) {
        failWithoutActualInternal(
            fact("expected not to contain a match for", regex),
            fact("but contained", result.value),
            fact("full string", actual)
        )
    }
}
