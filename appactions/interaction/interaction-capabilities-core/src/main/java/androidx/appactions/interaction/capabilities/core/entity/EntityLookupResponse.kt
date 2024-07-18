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

import androidx.annotation.IntDef
import androidx.appactions.interaction.protobuf.ByteString

/** The class for the response of the entity lookup. */
class EntityLookupResponse<T> internal constructor(
    val candidateList: List<EntityLookupCandidate<T>>,
    @property:EntityLookupStatus val status: Int,
    val nextPageToken: ByteString?,
) {
    override fun toString(): String {
        return "EntityLookupResponse(" +
            "candidateList=$candidateList, " +
            "status=$status, " +
            "nextPageToken=$nextPageToken)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EntityLookupResponse<*>

        if (candidateList != other.candidateList) return false
        if (status != other.status) return false
        if (nextPageToken != other.nextPageToken) return false
        return true
    }

    override fun hashCode(): Int {
        var result = candidateList.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + nextPageToken.hashCode()
        return result
    }

    /** Builder class for [EntityLookupResponse]. */
    class Builder<T> {
        private var candidateList: List<EntityLookupCandidate<T>> = listOf()

        @property:EntityLookupStatus
        private var status: Int = SUCCESS
        private var nextPageToken: ByteString? = null
        fun setCandidateList(candidateList: List<EntityLookupCandidate<T>>): Builder<T> = apply {
            this.candidateList = candidateList
        }

        fun setStatus(status: @EntityLookupStatus Int): Builder<T> =
            apply {
                this.status = status
            }

        fun setNextPageToken(nextPageToken: ByteString): Builder<T> = apply {
            this.nextPageToken = nextPageToken
        }

        fun build() = EntityLookupResponse(candidateList, status, nextPageToken)
    }

    companion object {

        const val SUCCESS: Int = 0
        const val CANCELED: Int = 1
        const val INVALID_PAGE_TOKEN: Int = 2
        const val TIMEOUT: Int = 3
        const val UNKNOWN_ERROR: Int = 4
    }

    // IntDef enum for lookup status.
    @Target(
        AnnotationTarget.PROPERTY,
        AnnotationTarget.LOCAL_VARIABLE,
        AnnotationTarget.VALUE_PARAMETER,
        AnnotationTarget.TYPE
    )
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        value = [
            SUCCESS,
            CANCELED,
            INVALID_PAGE_TOKEN,
            TIMEOUT,
            UNKNOWN_ERROR,
        ]
    )
    annotation class EntityLookupStatus
}
