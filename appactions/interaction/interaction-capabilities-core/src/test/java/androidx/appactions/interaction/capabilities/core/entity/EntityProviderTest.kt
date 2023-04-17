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

import androidx.appactions.builtintypes.experimental.types.Alarm
import androidx.appactions.interaction.proto.Entity
import androidx.appactions.interaction.proto.GroundingRequest
import androidx.appactions.interaction.proto.GroundingResponse
import androidx.appactions.interaction.proto.ParamValue
import androidx.appactions.interaction.protobuf.ByteString
import androidx.appactions.interaction.protobuf.Struct
import androidx.appactions.interaction.protobuf.Value
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

private val VALID_GROUNDING_REQUEST = GroundingRequest.newBuilder()
    .setRequest(
        GroundingRequest.Request.newBuilder()
            .setSearchAction(
                ParamValue.newBuilder()
                    .setStructValue(
                        Struct.newBuilder()
                            .putFields(
                                "@type",
                                Value.newBuilder().setStringValue("SearchAction").build(),
                            )
                            .putFields(
                                "object",
                                Value.newBuilder()
                                    .setStructValue(
                                        Struct.newBuilder()
                                            .putFields(
                                                "@type",
                                                Value.newBuilder()
                                                    .setStringValue("Alarm")
                                                    .build(),
                                            ),
                                    )
                                    .build(),
                            ),
                    ),
            ),
    )
    .build()

@RunWith(JUnit4::class)
class EntityProviderTest {
    private fun createExternalResponse(
        candidateList: List<EntityLookupCandidate<Alarm>>,
        status: Int,
    ): EntityLookupResponse<Alarm> {
        return EntityLookupResponse.Builder<Alarm>()
            .setCandidateList(candidateList)
            .setStatus(status)
            .setNextPageToken(ByteString.EMPTY)
            .build()
    }

    private fun createInternalResponse(
        candidateList: List<GroundingResponse.Candidate>,
        status: GroundingResponse.Status,
    ): GroundingResponse? {
        return GroundingResponse.newBuilder().setResponse(
            GroundingResponse.Response.newBuilder().addAllCandidates(candidateList)
                .setStatus(status).build(),
        ).build()
    }

    private fun createExternalCandidate(id: String, name: String): EntityLookupCandidate<Alarm> {
        val candidateBuilder: EntityLookupCandidate.Builder<Alarm> =
            EntityLookupCandidate.Builder()
        candidateBuilder.setCandidate(Alarm.Builder().setName(name).setIdentifier(id).build())
        return candidateBuilder.build()
    }

    private fun createInternalCandidate(id: String, name: String): GroundingResponse.Candidate {
        return GroundingResponse.Candidate.newBuilder()
            .setGroundedEntity(
                Entity.newBuilder()
                    .setIdentifier(id)
                    .setStructValue(
                        Struct.newBuilder()
                            .putFields("@type", Value.newBuilder().setStringValue("Alarm").build())
                            .putFields("identifier", Value.newBuilder().setStringValue(id).build())
                            .putFields("name", Value.newBuilder().setStringValue(name).build()),
                    ),
            )
            .build()
    }

    @Test
    fun invalidEntity_returnError() = runBlocking<Unit> {
        val entityProvider = AlarmProvider(
            "id",
            createExternalResponse(
                listOf(),
                EntityLookupResponse.SUCCESS,
            ),
        )

        val response =
            entityProvider.lookupInternal(GroundingRequest.getDefaultInstance())

        assertThat(response)
            .isEqualTo(
                createInternalResponse(
                    listOf(),
                    GroundingResponse.Status.INVALID_ENTITY_ARGUMENT,
                ),
            )
    }

    @Test
    fun errorInExternalResponse_returnError() = runBlocking<Unit> {
        val entityProvider = AlarmProvider(
            "id",
            createExternalResponse(
                listOf(),
                EntityLookupResponse.CANCELED,
            ),
        )

        val response =
            entityProvider.lookupInternal(VALID_GROUNDING_REQUEST)

        assertThat(response)
            .isEqualTo(createInternalResponse(listOf(), GroundingResponse.Status.CANCELED))
    }

    @Test
    fun success() = runBlocking<Unit> {
        val candidateBuilder: EntityLookupCandidate.Builder<Alarm> =
            EntityLookupCandidate.Builder()
        candidateBuilder.setCandidate(Alarm.Builder().setName("testing-alarm").build())
        val entityProvider = AlarmProvider(
            "id",
            createExternalResponse(
                listOf(
                    createExternalCandidate("id-1", "name-1"),
                    createExternalCandidate("id-2", "name-2"),
                ),
                EntityLookupResponse.SUCCESS,
            ),
        )

        val response =
            entityProvider.lookupInternal(VALID_GROUNDING_REQUEST)

        assertThat(response)
            .isEqualTo(
                createInternalResponse(
                    listOf(
                        createInternalCandidate("id-1", "name-1"),
                        createInternalCandidate("id-2", "name-2"),
                    ),
                    GroundingResponse.Status.SUCCESS,
                ),
            )
    }
}
