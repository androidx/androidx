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
package androidx.appactions.interaction.capabilities.core.impl

import androidx.annotation.RestrictTo
import androidx.appactions.interaction.proto.FulfillmentRequest.Fulfillment
import androidx.appactions.interaction.proto.FulfillmentRequest.Fulfillment.FulfillmentValue

/**
 * Represents Fulfillment request sent from assistant, including arguments.
 *
 * @param paramValues A map of BII parameter names to a task param value, where each
 *   `FulfillmentValue` can have a value and `DisambigData` sent from Assistant.
 * @param requestMetadata Metadata from the FulfillmentRequest on the current Assistant turn. This
 *   field should be null for one-shot capabilities.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class ArgumentsWrapper
internal constructor(
    val paramValues: Map<String, List<FulfillmentValue>>,
    val requestMetadata: RequestMetadata?,
) {
    companion object {
        /**
         * Creates an instance of ArgumentsWrapper based on the Fulfillment send from Assistant.
         *
         * @param fulfillment for a single BII sent from Assistant.
         */
        @JvmStatic
        fun create(fulfillment: Fulfillment): ArgumentsWrapper {
            return ArgumentsWrapper(
                convertToArgumentMap(fulfillment),
                createRequestMetadata(fulfillment),
            )
        }

        internal fun createRequestMetadata(fulfillment: Fulfillment): RequestMetadata? {
            return if (
                fulfillment.type == Fulfillment.Type.UNKNOWN_TYPE ||
                fulfillment.type == Fulfillment.Type.UNRECOGNIZED
            ) {
                null
            } else {
                RequestMetadata(fulfillment.type, fulfillment.syncStatus)
            }
        }

        internal fun convertToArgumentMap(
            fulfillment: Fulfillment,
        ): Map<String, List<FulfillmentValue>> {
            val result = mutableMapOf<String, List<FulfillmentValue>>()
            for (fp in fulfillment.paramsList) {
                result[fp.name] = fp.fulfillmentValuesList
            }
            return result.toMap()
        }
    }
}
