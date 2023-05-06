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

import androidx.appactions.interaction.capabilities.core.impl.BuilderOf
import androidx.appactions.interaction.capabilities.core.impl.exceptions.StructConversionException
import androidx.appactions.interaction.capabilities.core.impl.utils.ImmutableCollectors
import androidx.appactions.interaction.capabilities.core.properties.Property
import androidx.appactions.interaction.proto.AppActionsContext
import androidx.appactions.interaction.proto.FulfillmentResponse
import androidx.appactions.interaction.proto.ParamValue
import java.util.function.Function
import java.util.function.Supplier

/** The implementation of `ActionSpec` interface.  */
internal class ActionSpecImpl<ArgumentsT, ArgumentsBuilderT : BuilderOf<ArgumentsT>, OutputT>(
    private val capabilityName: String,
    private val argumentBuilderSupplier: Supplier<ArgumentsBuilderT>,
    private val paramBindingList: List<ParamBinding<ArgumentsT, ArgumentsBuilderT>>,
    private val outputBindings: Map<String, Function<OutputT, List<ParamValue>>>
) : ActionSpec<ArgumentsT, OutputT> {

    override fun convertPropertyToProto(
        property: Map<String, Property<*>>
    ): AppActionsContext.AppAction {
        return AppActionsContext.AppAction.newBuilder()
            .setName(capabilityName)
            .addAllParams(
                paramBindingList.stream()
                    .map { binding -> binding.paramGetter.apply(property) }
                    .filter { optional -> optional.isPresent }
                    .map { optional -> optional.get() }
                    .collect(ImmutableCollectors.toImmutableList())
            )
            .build()
    }

    @Throws(StructConversionException::class)
    override fun buildArguments(args: Map<String, List<ParamValue>>): ArgumentsT {
        val argumentBuilder = argumentBuilderSupplier.get()
        paramBindingList.forEach { binding ->
            val paramValues = args[binding.name] ?: return@forEach
            try {
                binding.argumentSetter.setArguments(argumentBuilder, paramValues)
            } catch (e: StructConversionException) {
                // Wrap the exception with a more meaningful error message.
                throw StructConversionException(
                    "Failed to parse parameter '${binding.name}' from assistant because of" +
                        " failure: ${e.message}"
                )
            }
        }
        return argumentBuilder.build()
    }

    override fun convertOutputToProto(output: OutputT): FulfillmentResponse.StructuredOutput {
        val outputBuilder = FulfillmentResponse.StructuredOutput.newBuilder()
        outputBindings.entries.forEach { entry ->
            val paramValues = entry.value.apply(output)
            if (paramValues.isNotEmpty()) {
                outputBuilder.addOutputValues(
                    FulfillmentResponse.StructuredOutput.OutputValue.newBuilder()
                        .setName(entry.key)
                        .addAllValues(paramValues)
                        .build()
                )
            }
        }
        return outputBuilder.build()
    }
}