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

import androidx.kruth.Fact.Companion.makeMessage

/**
 * An [AssertionError] composed of structured [Fact] instances and other string messages.
 */
// TODO(dustinlam): Split into platform-specific implementations so we can add a
//  "createWithNoStack" constructor on JVM.
internal class AssertionErrorWithFacts(
    messagesToPrepend: List<String>,
    val facts: List<Fact> = emptyList(),
    // TODO: change to AssertionError that takes in a cause when upgraded to 1.9.20
    override val cause: Throwable? = null
) : AssertionError(
    makeMessage(messagesToPrepend, facts),
    // TODO: change to AssertionError that takes in a cause when upgraded to 1.9.20
    // cause
) {

    constructor(message: String? = null, cause: Throwable? = null) : this(
        messagesToPrepend = listOfNotNull(message),
        facts = emptyList(),
        cause = cause,
    )

    override fun toString(): String {
        // We intentionally hide the class name.
        return requireNonNull(message)
    }

    internal companion object {
        internal fun createWithNoStack(
            message: String,
            cause: Throwable? = null
        ): AssertionErrorWithFacts {
            return AssertionErrorWithFacts(message, cause)
                .also(AssertionErrorWithFacts::clearStackTrace)
        }
    }
}
