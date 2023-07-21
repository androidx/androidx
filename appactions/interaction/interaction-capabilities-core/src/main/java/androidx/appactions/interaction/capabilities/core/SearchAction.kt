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

/**
 * A request to perform a search for in-app entities.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class SearchAction<FilterT> internal constructor(
    val query: String?,
    val filter: FilterT?
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SearchAction<*>) return false
        if (this.query != other.query) return false
        if (this.filter != other.filter) return false
        return true
    }

    /** Builder class for Entity. */
    class Builder<FilterT> {
        private var query: String? = null
        private var filter: FilterT? = null

        /** Sets the query keywords to search by. */
        fun setQuery(query: String) = apply {
            this.query = query
        }

        /** Sets the entity filter object to search by. */
        fun setFilter(filter: FilterT) = apply {
            this.filter = filter
        }

        /** Builds and returns a [SearchAction]. */
        fun build() = SearchAction(query, filter)
    }
}