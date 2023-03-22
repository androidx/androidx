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

import androidx.annotation.RestrictTo
import androidx.appactions.interaction.capabilities.core.impl.converters.SearchActionConverter
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeSpec
import androidx.appactions.interaction.capabilities.core.impl.exceptions.StructConversionException
import androidx.appactions.interaction.capabilities.core.values.SearchAction
import androidx.appactions.interaction.capabilities.core.values.Thing
import androidx.appactions.interaction.proto.Entity
import androidx.appactions.interaction.proto.GroundingRequest
import androidx.appactions.interaction.proto.GroundingResponse

/**
 * EntityProvider could provide candidates for assistant's search actions.
 *
 * <p>Use abstract classes within the library to create instances of the {@link EntityProvider}.
 */
abstract class EntityProvider<T : Thing> internal constructor(private val typeSpec: TypeSpec<T>) {
    /**
     * Unique identifier for this EntityFilter. Must match the shortcuts.xml declaration, which allows
     * different filters to be assigned to types on a per-BII basis.
     */
    abstract fun getId(): String

    /**
     * Executes the entity lookup.
     *
     * @param request The request includes e.g. entity, search metadata, etc.
     */
    abstract fun lookup(request: EntityLookupRequest<T>): EntityLookupResponse<T>

    /**
     * Internal method to lookup untyped entity, which will be used by service library to handle
     * {@link GroundingRequest}.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun lookupInternal(request: GroundingRequest): GroundingResponse {
        val converter: SearchActionConverter<T> =
            TypeConverters.createSearchActionConverter(this.typeSpec)
        val searchAction: SearchAction<T> = try {
            converter.toSearchAction(request.request.searchAction)
        } catch (e: StructConversionException) {
            return createResponse(GroundingResponse.Status.INVALID_ENTITY_ARGUMENT)
        }
        val lookupRequest = EntityLookupRequest.Builder<T>()
            .setSearchAction(searchAction)
            .setPageSize(request.request.pageSize)
            .setPageToken(request.request.pageToken)
            .build()
        val response = lookup(lookupRequest)
        @EntityLookupResponse.EntityLookupStatus val status: Int = response.status
        return if (status == EntityLookupResponse.SUCCESS) {
            createResponse(response)
        } else createResponse(convertStatus(status))
    }

    private fun createResponse(status: GroundingResponse.Status): GroundingResponse {
        return GroundingResponse.newBuilder()
            .setResponse(GroundingResponse.Response.newBuilder().setStatus(status))
            .build()
    }

    private fun createResponse(response: EntityLookupResponse<T>): GroundingResponse {
        val builder =
            GroundingResponse.Response.newBuilder().setStatus(GroundingResponse.Status.SUCCESS)
        for (candidate in response.candidateList) {
            builder.addCandidates(
                GroundingResponse.Candidate.newBuilder()
                    .setGroundedEntity(
                        Entity.newBuilder().setValue(typeSpec.toStruct(candidate.candidate))
                    )
                    .build()
            )
        }
        return GroundingResponse.newBuilder().setResponse(builder.build()).build()
    }

    private fun convertStatus(
        @EntityLookupResponse.EntityLookupStatus status: Int
    ): GroundingResponse.Status {
        return when (status) {
            EntityLookupResponse.CANCELED -> GroundingResponse.Status.CANCELED
            EntityLookupResponse.INVALID_PAGE_TOKEN -> GroundingResponse.Status.INVALID_PAGE_TOKEN
            EntityLookupResponse.TIMEOUT -> GroundingResponse.Status.TIMEOUT
            else -> GroundingResponse.Status.DEFAULT_UNKNOWN
        }
    }
}