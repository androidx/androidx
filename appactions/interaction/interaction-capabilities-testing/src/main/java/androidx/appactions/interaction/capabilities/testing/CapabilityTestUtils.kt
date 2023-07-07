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

package androidx.appactions.interaction.capabilities.testing

import androidx.annotation.VisibleForTesting
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecRegistry
import androidx.appactions.interaction.proto.FulfillmentRequest
import androidx.appactions.interaction.proto.FulfillmentRequest.Fulfillment
import androidx.appactions.interaction.proto.FulfillmentRequest.Fulfillment.FulfillmentParam
import androidx.appactions.interaction.proto.FulfillmentRequest.Fulfillment.FulfillmentValue

/**
 * Contains utility methods and classes for testing any BII capability built with the
 * AppInteraction Capabilities library.
 */
@VisibleForTesting
object CapabilityTestUtils {
  /**
   * Create a CapabilityRequest for executing a capability with some Argument instance.
   * @param args an Arguments instance for some AppInteraction capability.
   * @throws [IllegalArgumentException] if [args] is not an instance of a supported Arguments
   * class.
   * Example Usage:
   * ```
   * val arguments = CreateMessage.Arguments.Builder().setMessageText("Hello").build()
   * val capabilityRequest = createCapabilityRequest(arguments)
   * ```
   */
  fun createCapabilityRequest(args: Any): CapabilityRequest {
    val actionSpec = ActionSpecRegistry.getActionSpecForArguments(args)
    if (actionSpec == null) {
      throw IllegalArgumentException(
        "Failed to find associated capability class for arguments of class ${args.javaClass}."
      )
    }
    val fulfillmentParams = actionSpec.serializeArguments(args).map {
      (slotName, paramValues) ->
      FulfillmentParam.newBuilder().setName(slotName).addAllFulfillmentValues(
        paramValues.map { FulfillmentValue.newBuilder().setValue(it).build() }
      ).build()
    }
    return CapabilityRequest(
      FulfillmentRequest.newBuilder().addFulfillments(
        Fulfillment.newBuilder()
          .setName(actionSpec.capabilityName)
          .addAllParams(fulfillmentParams)
          .build()
      ).build()
    )
  }
}
