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

package androidx.appactions.interaction.capabilities.core.impl.spec

import androidx.appactions.interaction.capabilities.core.impl.exceptions.StructConversionException
import androidx.appactions.interaction.proto.AppActionsContext
import androidx.appactions.interaction.proto.FulfillmentResponse
import androidx.appactions.interaction.proto.ParamValue

/**
 * A specification for an action, describing it from the app's point of view.
 *
 * @param ArgumentsT typed representation of action's arguments.
 * @param OutputT    typed action's execution output.
 */
interface ActionSpec<ArgumentsT, OutputT> {

    /**
     * The BII capability name this ActionSpec is for.
     */
    val capabilityName: String

    /**
     * Converts the input parameters to the `AppAction` proto.
     * @param identifier                    the capability identifier
     * @param boundProperties               the list of BoundProperty instances.
     * @param supportsPartialFulfillment    whether or not this capability supports partial
     * fulfillment.
     */
    fun createAppAction(
        identifier: String,
        boundProperties: List<BoundProperty<*>>,
        supportsPartialFulfillment: Boolean
    ): AppActionsContext.AppAction

    /** Builds this action's arguments from a map of slot name to param values.  */
    @Throws(StructConversionException::class)
    fun buildArguments(args: Map<String, List<ParamValue>>): ArgumentsT

    /**
     * Converts an [ArgumentsT] instance to a Fulfillment proto
     */
    fun serializeArguments(args: ArgumentsT): Map<String, List<ParamValue>>

    /** Converts the output to the `StructuredOutput` proto.  */
    fun convertOutputToProto(output: OutputT): FulfillmentResponse.StructuredOutput
}
