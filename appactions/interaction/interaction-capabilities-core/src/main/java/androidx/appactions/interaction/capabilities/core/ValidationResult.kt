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

package androidx.appactions.interaction.capabilities.core

import androidx.annotation.RestrictTo
import java.util.Objects

/** Result from validating a single argument value. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ValidationResult internal constructor(
    val kind: Kind,
) {
    override fun toString() =
        "ValidationResult(kind=$kind)"

    override fun equals(other: Any?): Boolean {
        return other is ValidationResult && kind == other.kind
    }

    override fun hashCode() = Objects.hash(kind)

    /** The state of the argument value after performing validation. */
    enum class Kind {
        ACCEPTED,
        REJECTED,
    }

    companion object {
        /** Creates a new ACCEPTED ValidationResult. */
        @JvmStatic
        fun newAccepted() = ValidationResult(Kind.ACCEPTED)

        /** Creates a new REJECTED ValidationResult. */
        @JvmStatic
        fun newRejected() = ValidationResult(Kind.REJECTED)
    }
}
