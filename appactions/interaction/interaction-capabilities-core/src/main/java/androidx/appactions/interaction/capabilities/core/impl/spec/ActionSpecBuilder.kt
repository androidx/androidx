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
import androidx.appactions.interaction.capabilities.core.impl.converters.EntityConverter
import androidx.appactions.interaction.capabilities.core.impl.converters.ParamValueConverter
import androidx.appactions.interaction.capabilities.core.impl.converters.SlotTypeConverter
import androidx.appactions.interaction.capabilities.core.impl.spec.ParamBinding.ArgumentSetter
import androidx.appactions.interaction.capabilities.core.impl.spec.ParamBinding.Companion.create
import androidx.appactions.interaction.capabilities.core.impl.utils.ImmutableCollectors
import androidx.appactions.interaction.capabilities.core.properties.Property
import androidx.appactions.interaction.proto.AppActionsContext
import androidx.appactions.interaction.proto.ParamValue
import java.util.function.BiConsumer
import java.util.function.Function
import java.util.function.Supplier

/**
 * A builder for the `ActionSpec`.
 */
class ActionSpecBuilder<ArgumentsT, ArgumentsBuilderT : BuilderOf<ArgumentsT>, OutputT>
private constructor(
    private val capabilityName: String,
    private val argumentBuilderSupplier: Supplier<ArgumentsBuilderT>
) {
    private val paramBindingList: MutableList<ParamBinding<ArgumentsT, ArgumentsBuilderT>> =
        ArrayList()
    private val outputBindings: MutableMap<String, Function<OutputT, List<ParamValue>>> = HashMap()

    /** Sets the property type and returns a new `ActionSpecBuilder`.  */
    /** Sets the argument type and its builder and returns a new `ActionSpecBuilder`.  */
    @Suppress("UNUSED_PARAMETER")
    fun <NewArgumentsT, NewArgumentsBuilderT : BuilderOf<NewArgumentsT>> setArguments(
        unused: Class<NewArgumentsT>,
        argumentBuilderSupplier: Supplier<NewArgumentsBuilderT>
    ): ActionSpecBuilder<NewArgumentsT, NewArgumentsBuilderT, OutputT> {
        return ActionSpecBuilder(this.capabilityName, argumentBuilderSupplier)
    }

    @Suppress("UNUSED_PARAMETER")
    fun <NewOutputT> setOutput(
        unused: Class<NewOutputT>
    ): ActionSpecBuilder<ArgumentsT, ArgumentsBuilderT, NewOutputT> {
        return ActionSpecBuilder(this.capabilityName, this.argumentBuilderSupplier)
    }

    /**
     * Binds the parameter name, getter and setter.
     *
     * @param paramName      the name of this action' parameter.
     * @param paramGetter    a getter of the param-specific info from the property.
     * @param argumentSetter a setter to the argument with the input from `ParamValue`.
     * @return the builder itself.
     */
    private fun bindParameterInternal(
        paramName: String,
        paramGetter: Function<Map<String, Property<*>>, AppActionsContext.IntentParameter?>,
        argumentSetter: ArgumentSetter<ArgumentsBuilderT>
    ): ActionSpecBuilder<ArgumentsT, ArgumentsBuilderT, OutputT> {
        paramBindingList.add(create(paramName, paramGetter, argumentSetter))
        return this
    }

    /**
     * Binds the parameter name, getter, and setter for a [Property].
     *
     * If the Property getter returns a null value, this parameter will not exist in the parameter
     * definition of the capability.
     *
     * @param paramName the name of this action' parameter.
     * @param propertyGetter a getter of the Property from the property, which must be able to
     * fetch a non-null `Property` from `PropertyT`.
     * @param paramConsumer a setter to set the string value in the argument builder.
     * @param paramValueConverter converter FROM assistant ParamValue proto
     * @param entityConverter converter TO assistant Entity proto
     * @return the builder itself.
     */
    fun <T, PossibleValueT> bindParameter(
        paramName: String,
        propertyGetter: Function<Map<String, Property<*>>, Property<PossibleValueT>?>,
        paramConsumer: BiConsumer<in ArgumentsBuilderT, T>,
        paramValueConverter: ParamValueConverter<T>,
        entityConverter: EntityConverter<PossibleValueT>
    ): ActionSpecBuilder<ArgumentsT, ArgumentsBuilderT, OutputT> {
        return bindParameterInternal(
            paramName,
            { propertyMap ->
                propertyGetter.apply(propertyMap)?.let {
                    buildIntentParameter(paramName, it, entityConverter)
                }
            },
            { argBuilder: ArgumentsBuilderT, paramList: List<ParamValue> ->
                if (paramList.isNotEmpty()) {
                    paramConsumer.accept(
                        argBuilder,
                        SlotTypeConverter.ofSingular(paramValueConverter).convert(paramList)
                    )
                }
            }
        )
    }

    /**
     * This is similar to [ActionSpecBuilder.bindParameter] but for setting a list of
     * entities instead.
     *
     * If the Property getter returns a null value, this parameter will not exist in the parameter
     * definition of the capability.
     */
    fun <T, PossibleValueT> bindRepeatedParameter(
        paramName: String,
        propertyGetter: Function<Map<String, Property<*>>, Property<PossibleValueT>?>,
        paramConsumer: BiConsumer<in ArgumentsBuilderT, List<T>>,
        paramValueConverter: ParamValueConverter<T>,
        entityConverter: EntityConverter<PossibleValueT>
    ): ActionSpecBuilder<ArgumentsT, ArgumentsBuilderT, OutputT> {
        return bindParameterInternal(
            paramName,
            { propertyMap ->
                propertyGetter.apply(propertyMap)?.let {
                    buildIntentParameter(paramName, it, entityConverter)
                }
            },
            { argBuilder: ArgumentsBuilderT, paramList: List<ParamValue?>? ->
                paramConsumer.accept(
                    argBuilder,
                    SlotTypeConverter.ofRepeated(paramValueConverter).convert(paramList!!)
                )
            }
        )
    }

    /**
     * Binds an optional output.
     *
     * @param name         the BII output slot name of this parameter.
     * @param outputFieldGetter a getter of the output from the `OutputT` instance.
     * @param converter    a converter from an output object to a ParamValue.
     */
    fun <T> bindOutput(
        name: String,
        outputFieldGetter: Function<OutputT, T?>,
        converter: Function<T, ParamValue>
    ): ActionSpecBuilder<ArgumentsT, ArgumentsBuilderT, OutputT> {
        outputBindings[name] = Function { output: OutputT ->
            val outputField: T? = outputFieldGetter.apply(output)
            val paramValues: MutableList<ParamValue> =
                ArrayList()
            if (outputField != null) {
                paramValues.add(converter.apply(outputField))
            }
            paramValues.toList()
        }
        return this
    }

    /**
     * Binds a repeated output.
     *
     * @param name         the BII output slot name of this parameter.
     * @param outputGetter a getter of the output from the `OutputT` instance.
     * @param converter    a converter from an output object to a ParamValue.
     */
    fun <T> bindRepeatedOutput(
        name: String,
        outputGetter: Function<OutputT, List<T>>,
        converter: Function<T, ParamValue>
    ): ActionSpecBuilder<ArgumentsT, ArgumentsBuilderT, OutputT> {
        outputBindings[name] = Function { output: OutputT ->
            outputGetter.apply(output).stream()
                .map(converter)
                .collect(ImmutableCollectors.toImmutableList())
        }
        return this
    }

    /** Builds an `ActionSpec` from this builder.  */
    fun build(): ActionSpec<ArgumentsT, OutputT> {
        return ActionSpecImpl(
            capabilityName,
            argumentBuilderSupplier,
            paramBindingList.toList(),
            outputBindings.toMap()
        )
    }

    companion object {
        /**
         * Creates an empty `ActionSpecBuilder` with the given capability name. ArgumentsT is set
         * to Object as a placeholder, which must be replaced by calling setArgument.
         */
        fun ofCapabilityNamed(
            capabilityName: String
        ): ActionSpecBuilder<Any, BuilderOf<Any>, Any> {
            return ActionSpecBuilder(capabilityName) { BuilderOf { Object() } }
        }

        /** Create IntentParameter proto from a Property.  */
        internal fun <T> buildIntentParameter(
            paramName: String,
            property: Property<T>,
            entityConverter: EntityConverter<T>
        ): AppActionsContext.IntentParameter {
            val builder = AppActionsContext.IntentParameter.newBuilder()
                .setName(paramName)
                .setIsRequired(property.isRequired)
                .setEntityMatchRequired(property.isValueMatchRequired)
                .setIsProhibited(property.isProhibited)
            property.possibleValues.stream()
                .map { possibleValue ->
                    entityConverter.convert(possibleValue)
                }
                .forEach { entityProto ->
                    builder.addPossibleEntities(entityProto)
                }
            return builder.build()
        }
    }
}