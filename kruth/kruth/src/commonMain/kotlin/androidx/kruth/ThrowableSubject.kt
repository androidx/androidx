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

/**
 * Propositions for [Throwable] subjects.
 *
 * @constructor Constructor for use by subclasses. If you want to create an instance of this class
 * itself, call [check(...)][Subject.check].[that(actual)][StandardSubjectBuilder.that].
 */
open class ThrowableSubject<out T : Throwable> internal constructor(
    actual: T?,
    metadata: FailureMetadata,
    typeDescriptionOverride: String?
) : Subject<T>(actual, metadata, typeDescriptionOverride) {

    /**
     * Constructor for use by subclasses. If you want to create an instance of this class
     *  * itself, call [check(...)][Subject.check].[that(actual)][StandardSubjectBuilder.that].
     */
    protected constructor(metadata: FailureMetadata, actual: T?) : this(actual, metadata, null)

    /*
     * TODO(cpovirk): consider a special case for isEqualTo and isSameInstanceAs that adds |expected|
     *  as a suppressed exception
     */

    /**
     * Returns a [StringSubject] to make assertions about the throwable's message.
     */
    fun hasMessageThat(): StringSubject {
        var check: StandardSubjectBuilder = check("message")
        if (actual is AssertionErrorWithFacts && actual.facts.isNotEmpty()) {
            check = check.withMessage(
                "(Note from Truth: When possible, instead of asserting on the full message, " +
                    "assert about individual facts by using ExpectFailure.assertThat.)"
            )
        }
        return check.that(requireNonNull(actual).message)
    }

    /**
     * Returns a new [ThrowableSubject] that supports assertions on this [Throwable]'s direct
     * cause. This method can be invoked repeatedly (e.g.
     * `assertThat(e).hasCauseThat().hasCauseThat()....` to assert on a particular indirect cause.
     */
    // Any Throwable is fine, and we use plain Throwable to emphasize that it's not used "for real."
    fun hasCauseThat(): ThrowableSubject<Throwable> {
        // provides a more helpful error message if hasCauseThat() methods are chained too deep
        // e.g. assertThat(new Exception()).hCT().hCT()....
        // TODO(diamondm) in keeping with other subjects' behavior this should still NPE if the subject
        //  *itself* is null, since there's no context to lose. See also b/37645583
        if (actual == null) {
            check("cause")
                .withMessage("Causal chain is not deep enough - add a .isNotNull() check?")
                .fail()

            return ignoreCheck().that(Throwable())
        }

        return check("cause").that(actual.cause)
    }
}
