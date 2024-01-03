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

package androidx.appactions.interaction.capabilities.core.entity

/** The candidate of the lookup results, including the entity object. */
class EntityLookupCandidate<T> internal constructor(
    val candidate: T
) {

    override fun toString(): String {
        return "EntityLookupCandidate(candidate=$candidate)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EntityLookupCandidate<*>

        if (candidate != other.candidate) return false
        return true
    }

    override fun hashCode(): Int {
        return candidate.hashCode()
    }

    /** Builder class for [EntityLookupCandidate]. */
    class Builder<T> {
        private var candidate: T? = null
        fun setCandidate(candidate: T): Builder<T> = apply { this.candidate = candidate }
        fun build(): EntityLookupCandidate<T> = EntityLookupCandidate(
            requireNotNull(candidate) { "Entity lookup candidate must be set." })
    }
}
