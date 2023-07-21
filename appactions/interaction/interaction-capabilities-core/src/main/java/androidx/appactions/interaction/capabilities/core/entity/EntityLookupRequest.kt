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

import androidx.appactions.interaction.capabilities.core.SearchAction
import androidx.appactions.interaction.protobuf.ByteString

/** The class for the request of the entity lookup. */
class EntityLookupRequest<T> internal constructor(
    val searchAction: SearchAction<T>,
    val pageSize: Int?,
    val pageToken: ByteString?,
) {
    override fun toString(): String {
        return "EntityLookupRequest(" +
            "searchAction=$searchAction, " +
            "pageSize=$pageSize, " +
            "pageToken=$pageToken)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EntityLookupRequest<*>

        if (searchAction != other.searchAction) return false
        if (pageSize != other.pageSize) return false
        if (pageToken != other.pageToken) return false
        return true
    }

    override fun hashCode(): Int {
        var result = searchAction.hashCode()
        result = 31 * result + pageSize.hashCode()
        result = 31 * result + pageToken.hashCode()
        return result
    }

    /** Builder class for EntityLookupRequest. */
    class Builder<T> {
        private var searchAction: SearchAction<T>? = null
        private var pageSize: Int? = null
        private var pageToken: ByteString? = null

        fun setSearchAction(searchAction: SearchAction<T>) =
            apply { this.searchAction = searchAction }

        fun setPageSize(pageSize: Int) = apply { this.pageSize = pageSize }

        fun setPageToken(pageToken: ByteString) = apply { this.pageToken = pageToken }

        fun build() = EntityLookupRequest(
            requireNotNull(searchAction) { "Search action must be set." },
            pageSize,
            pageToken
        )
    }
}