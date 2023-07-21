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
import androidx.appactions.interaction.capabilities.core.properties.Property
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

    /** Converts the property to the `AppAction` proto.  */
    fun convertPropertyToProto(property: Map<String, Property<*>>): AppActionsContext.AppAction

    /** Builds this action's arguments from an ArgumentsWrapper instance.  */
    @Throws(StructConversionException::class)
    fun buildArguments(args: Map<String, List<ParamValue>>): ArgumentsT

    /** Converts the output to the `StructuredOutput` proto.  */
    fun convertOutputToProto(output: OutputT): FulfillmentResponse.StructuredOutput
}