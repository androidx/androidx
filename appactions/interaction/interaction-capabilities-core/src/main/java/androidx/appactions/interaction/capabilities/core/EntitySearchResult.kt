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
 * Represents results from searching for ungrounded value(s).
 *
 * Returning exactly 1 result means the value will be immediately accepted by the session.
 * Returning multiple values will leave the argument in disambiguation state, and Assistant should
 * ask for clarification from the user.
 *
 * @property possibleValues The possible values for grounding.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class EntitySearchResult<V> internal constructor(
    val possibleValues: List<V>,
) {
    override fun toString() =
        "EntitySearchResult(possibleValues=$possibleValues)"

    override fun equals(other: Any?): Boolean {
        return other is EntitySearchResult<*> && possibleValues == other.possibleValues
    }

    override fun hashCode() = Objects.hash(possibleValues)

    /**
     * Builder for the EntitySearchResult.
     */
    class Builder<V> {
        private val possibleValues = mutableListOf<V>()

        /** Sets the search result values.  */
        fun setPossibleValues(possibleValues: List<V>) = apply {
            this.possibleValues.clear()
            this.possibleValues.addAll(possibleValues)
        }

        /**
         * Add one or more search result values.
         */
        fun addPossibleValue(vararg value: V) = apply {
            possibleValues.addAll(value)
        }

        fun build() = EntitySearchResult(possibleValues.toList())
    }
}
