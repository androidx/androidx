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

import java.util.regex.Pattern

internal actual interface PlatformStringSubject {

    /** Fails if the string does not match the given regex. */
    fun matches(regex: Pattern)

    /** Fails if the string matches the given regex. */
    fun doesNotMatch(regex: Pattern)

    /** Fails if the string does not contain a match on the given regex. */
    fun containsMatch(regex: Pattern)

    /** Fails if the string contains a match on the given regex. */
    fun doesNotContainMatch(regex: Pattern)
}

internal actual class PlatformStringSubjectImpl actual constructor(
    actual: String?,
    metadata: FailureMetadata,
) : Subject<String>(actual, metadata, typeDescriptionOverride = null), PlatformStringSubject {

    override fun matches(regex: Pattern) {
        matchesImpl(regex.toRegex()) {
            "If you want an exact equality assertion you can escape your regex with " +
                "Pattern.quote()."
        }
    }

    override fun doesNotMatch(regex: Pattern) {
        doesNotMatchImpl(regex.toRegex())
    }

    override fun containsMatch(regex: Pattern) {
        containsMatchImpl(regex.toRegex())
    }

    override fun doesNotContainMatch(regex: Pattern) {
        doesNotContainMatchImpl(regex.toRegex())
    }
}
