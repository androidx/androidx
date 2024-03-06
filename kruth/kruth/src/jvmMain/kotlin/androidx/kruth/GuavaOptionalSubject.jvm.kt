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

import androidx.kruth.Fact.Companion.fact
import androidx.kruth.Fact.Companion.simpleFact
import com.google.common.base.Optional

/**
 * Propositions for Guava [Optional] subjects.
 */
class GuavaOptionalSubject<T : Any> internal constructor(
    actual: Optional<out T>?,
    metadata: FailureMetadata = FailureMetadata(),
) : Subject<Optional<out T>>(actual, metadata = metadata, typeDescriptionOverride = "optional") {

    /** Fails if the [Optional]`<T>` is absent or the subject is null. */
    fun isPresent() {
        if (actual == null) {
            failWithActual(simpleFact("expected present optional"))
        } else if (!actual.isPresent) {
            failWithoutActual(simpleFact("expected to be present"))
        }
    }

    /** Fails if the [Optional]`<T>` is present or the subject is null. */
    fun isAbsent() {
        if (actual == null) {
            failWithActual(simpleFact("expected absent optional"))
        } else if (actual.isPresent) {
            failWithoutActual(
                simpleFact("expected to be absent"),
                fact("but was present with value", actual.get()),
            )
        }
    }

    /**
     * Fails if the [Optional]`<T>` does not have the given value or the subject is null.
     *
     * To make more complex assertions on the optional's value split your assertion in two:
     *
     * ```
     * assertThat(myOptional).isPresent()
     * assertThat(myOptional.get()).contains("foo")
     * ```
     */
    fun hasValue(expected: Any?) {
        requireNonNull(expected) { "Optional cannot have a null value" }

        if (actual == null) {
            failWithActual("expected an optional with value", expected)
        } else if (!actual.isPresent) {
            failWithoutActual(
                fact("expected to have value", expected),
                simpleFact("but was absent")
            )
        } else {
            checkNoNeedToDisplayBothValues("get()").that(actual.get()).isEqualTo(expected)
        }
    }
}
