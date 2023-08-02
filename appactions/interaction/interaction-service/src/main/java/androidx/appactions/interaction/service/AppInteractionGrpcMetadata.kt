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

package androidx.appactions.interaction.service

import androidx.appactions.interaction.capabilities.core.impl.ErrorStatusInternal
import androidx.appactions.interaction.proto.AppInteractionMetadata
import androidx.appactions.interaction.proto.AppInteractionMetadata.ErrorStatus
import androidx.appactions.interaction.protobuf.InvalidProtocolBufferException
import io.grpc.Metadata

/**
 * Utilities to attach App Interaction specific metadata to any gRPC response
 * (via StatusRuntimeException).
 */
internal object AppInteractionGrpcMetadata {

    /** [Metadata.Key] used on gRPC trailers to pass [AppInteractionMetadata].  */
    val INTERACTION_SERVICE_STATUS_KEY: Metadata.Key<AppInteractionMetadata> = Metadata.Key.of(
        "interaction-service-metadata-bin",
        object : Metadata.BinaryMarshaller<AppInteractionMetadata> {

            override fun toBytes(value: AppInteractionMetadata): ByteArray {
                return value.toByteArray()
            }

            override fun parseBytes(serialized: ByteArray): AppInteractionMetadata {
                return try {
                    AppInteractionMetadata.parseFrom(serialized)
                } catch (e: InvalidProtocolBufferException) {
                    throw IllegalArgumentException(e)
                }
            }
        })

    private fun buildAppInteractionMetadata(
        errorStatusInternal: ErrorStatusInternal
    ): AppInteractionMetadata {
        val errorStatus = when (errorStatusInternal) {
            ErrorStatusInternal.UNKNOWN_ERROR_STATUS,
            -> ErrorStatus.UNKNOWN_ERROR_STATUS
            ErrorStatusInternal.INTERNAL -> ErrorStatus.INTERNAL
            ErrorStatusInternal.CANCELED -> ErrorStatus.CANCELED
            ErrorStatusInternal.TIMEOUT -> ErrorStatus.TIMEOUT
            ErrorStatusInternal.INVALID_REQUEST -> ErrorStatus.INVALID_REQUEST
            ErrorStatusInternal.SESSION_NOT_FOUND,
            -> ErrorStatus.SESSION_NOT_FOUND
            ErrorStatusInternal.EXTERNAL_EXCEPTION -> ErrorStatus.EXTERNAL_EXCEPTION
        }
        return AppInteractionMetadata.newBuilder()
            .setErrorStatus(errorStatus)
            .build()
    }

    /**
     * Supply errors directly from the service library. For example, sometimes we need to return an
     * error before we invoked any capability.
     */
    fun metadataOf(errorStatus: ErrorStatus): Metadata {
        val metadata = Metadata()
        metadata.put(
            INTERACTION_SERVICE_STATUS_KEY,
            AppInteractionMetadata.newBuilder()
                .setErrorStatus(errorStatus)
                .build()
        )
        return metadata
    }

    /** Supply errors by converting from errors in the Capability library. */
    fun metadataOf(errorStatusInternal: ErrorStatusInternal): Metadata {
        val metadata = Metadata()
        metadata.put(
            INTERACTION_SERVICE_STATUS_KEY,
            buildAppInteractionMetadata(errorStatusInternal)
        )
        return metadata
    }
}
