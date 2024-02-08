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
 */
open class ThrowableSubject<out T : Throwable> internal constructor(
    actual: T?,
    metadata: FailureMetadata = FailureMetadata(),
) : Subject<T>(actual = actual, metadata = metadata) {

    /**
     * Returns a [StringSubject] to make assertions about the throwable's message.
     */
    fun hasMessageThat(): StringSubject {
        return StringSubject(actual = actual?.message, metadata = metadata)
    }

    /**
     * Returns a new [ThrowableSubject] that supports assertions on this [Throwable]'s direct
     * cause. This method can be invoked repeatedly (e.g.
     * `assertThat(e).hasCauseThat().hasCauseThat()....` to assert on a particular indirect cause.
     */
    fun hasCauseThat(): ThrowableSubject<Throwable> {
        // provides a more helpful error message if hasCauseThat() methods are chained too deep
        // e.g. assertThat(new Exception()).hCT().hCT()....
        // TODO(diamondm) in keeping with other subjects' behavior this should still NPE if the subject
        //  *itself* is null, since there's no context to lose. See also b/37645583
        if (actual == null) {
            check("cause")
                .withMessage("Causal chain is not deep enough - add a .isNotNull() check?")
                .fail()
        }

        return check("cause").that(actual.cause)
    }
}
