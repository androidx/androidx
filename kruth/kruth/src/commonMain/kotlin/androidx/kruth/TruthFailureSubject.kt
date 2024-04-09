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

import androidx.kruth.Fact.Companion.fact
import androidx.kruth.Fact.Companion.simpleFact
import androidx.kruth.TruthFailureSubject.Companion.truthFailures
import kotlin.jvm.JvmStatic

internal val HOW_TO_TEST_KEYS_WITHOUT_VALUES: Fact = simpleFact(
    "To test that a key is present without a value, use factKeys().contains(...) or a similar " +
        "method."
)

private fun AssertionErrorWithFacts.factKeys(): List<String> {
    return facts.map { it.key }
}

private fun AssertionErrorWithFacts.factsWithName(key: String): List<Fact> {
    return facts.filter { it.key == key }
}

/**
 * Subject for [AssertionError] objects thrown by Truth. [TruthFailureSubject] contains methods for
 * asserting about the individual "facts" of those failures. This allows tests to avoid asserting
 * about the same fact more often than necessary, including avoiding asserting about facts that are
 * set by other subjects that the main subject delegates to. This keeps tests shorter and less
 * fragile.
 *
 * To create an instance, call [assertThat][androidx.kruth.ExpectFailure.Companion.assertThat]. Or,
 * if you're using a custom message or failure strategy, pass [truthFailures] to your `about(...)`
 * call.
 *
 * This class accepts any [AssertionError] value, but it will throw an exception if a caller tries
 * to access the facts of an error that wasn't produced by Truth.
 */
class TruthFailureSubject<T : AssertionError> internal constructor(
    actual: T?,
    metadata: FailureMetadata,
    typeDescription: String?
) : ThrowableSubject<T>(actual, metadata, typeDescription) {

    companion object {
        /**
         * Factory for creating [TruthFailureSubject] instances. Most users will just use
         * [ExpectFailure.assertThat][androidx.kruth.ExpectFailure.Companion.assertThat].
         */
        @JvmStatic
        fun <T : AssertionError> truthFailures(): Factory<TruthFailureSubject<T>, T> {
            return TruthFailureSubjectFactory()
        }
    }

    /** Returns a subject for the list of fact keys. */
    fun factKeys(): IterableSubject<String> {
        if (actual !is AssertionErrorWithFacts) {
            failWithActual(simpleFact("expected a failure thrown by Truth's failure API"))
            return ignoreCheck().that(emptyList())
        }

        return check("factKeys()").that(actual.factKeys())
    }

    /**
     * Returns a subject for the value with the given name.
     *
     * The value is always a string, the [toString] representation of the value passed to [fact].
     *
     * The value is never null:
     * * In the case of [facts that have no value][Fact.simpleFact], `factValue` throws an
     *   exception. To test for such facts, use [factKeys]`.contains(...)` or a similar method.
     * * In the case of facts that have a value that is rendered as "null" (such as those created
     *   with `fact("key", null)`), `factValue` considers them have a string value, the string
     *   "null."
     *
     * If the failure under test contains more than one fact with the given key, this method will
     * fail the test. To assert about such a failure, use [the other overload][factValue] of
     * `factValue`.
     */
    fun factValue(key: String): StringSubject {
        return doFactValue(key, null)
    }

    /**
     * Returns a subject for the value of the [index]-th instance of the fact with the given
     * name. Most Truth failures do not contain multiple facts with the same key, so most tests
     * should use [the other overload][factValue] of `factValue`.
     */
    fun factValue(key: String, index: Int): StringSubject {
        require(index >= 0) { "index must be nonnegative: $index" }
        return doFactValue(key, index)
    }

    private fun doFactValue(key: String, index: Int?): StringSubject {
        requireNonNull(key)
        if (actual !is AssertionErrorWithFacts) {
            failWithActual(simpleFact("expected a failure thrown by Truth's failure API"))
            return ignoreCheck().that("")
        }
        val error = actual

        /*
         * We don't care as much about including the actual AssertionError and its facts in these
         * because the AssertionError will be attached as a cause in nearly all cases.
         */
        val factsWithName = error.factsWithName(key)
        if (factsWithName.isEmpty()) {
            failWithoutActual(
                fact("expected to contain fact", key),
                fact("but contained only", error.factKeys())
            )
            return ignoreCheck().that("")
        }
        if (index == null && factsWithName.size > 1) {
            failWithoutActual(
                fact("expected to contain a single fact with key", key),
                fact("but contained multiple", factsWithName)
            )
            return ignoreCheck().that("")
        }
        if (index != null && index > factsWithName.size) {
            failWithoutActual(
                fact("for key", key),
                fact("index too high", index),
                fact("fact count was", factsWithName.size)
            )
            return ignoreCheck().that("")
        }
        val value = factsWithName.firstNotNullOf { it }.value
        if (value == null) {
            if (index == null) {
                failWithoutActual(
                    simpleFact("expected to have a value"),
                    fact("for key", key),
                    simpleFact("but the key was present with no value"),
                    HOW_TO_TEST_KEYS_WITHOUT_VALUES
                )
            } else {
                failWithoutActual(
                    simpleFact("expected to have a value"),
                    fact("for key", key),
                    fact("and index", index),
                    simpleFact("but the key was present with no value"),
                    HOW_TO_TEST_KEYS_WITHOUT_VALUES
                )
            }
            return ignoreCheck().that("")
        }
        val check: StandardSubjectBuilder = when (index) {
            null -> check("factValue($key)")
            else -> check("factValue($key, $index)")
        }
        return check.that(value)
    }
}

private class TruthFailureSubjectFactory<T : AssertionError> :
    Subject.Factory<TruthFailureSubject<T>, T> {
    override fun createSubject(metadata: FailureMetadata, actual: T?): TruthFailureSubject<T> {
        return TruthFailureSubject(actual, metadata, "failure")
    }
}
