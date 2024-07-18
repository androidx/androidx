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

/**
 * Class that represents the response after all slots are filled and accepted and the task is ready
 * to enter the confirmation turn.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ConfirmationOutput<ConfirmationT> internal constructor(val confirmation: ConfirmationT?) {
    override fun toString() =
        "ConfirmationOutput(confirmation=$confirmation)"

    override fun equals(other: Any?): Boolean {
        return other is ConfirmationOutput<*> && confirmation == other.confirmation
    }

    override fun hashCode() = Objects.hash(confirmation)

    /**
     * Builder for ConfirmationOutput.
     */
    class Builder<ConfirmationT> {
        private var confirmation: ConfirmationT? = null

        /** Sets the confirmation output. */
        fun setConfirmation(confirmation: ConfirmationT) = apply {
            this.confirmation = confirmation
        }

        /** Builds and returns the ConfirmationOutput instance. */
        fun build() = ConfirmationOutput(confirmation)
    }
}
