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

import androidx.appactions.interaction.capabilities.core.impl.converters.ParamValueConverter
import androidx.appactions.interaction.capabilities.core.impl.converters.SlotTypeConverter
import androidx.appactions.interaction.capabilities.core.impl.spec.ParamBinding.ArgumentSetter
import androidx.appactions.interaction.capabilities.core.impl.utils.ImmutableCollectors
import androidx.appactions.interaction.proto.ParamValue
import java.util.function.BiConsumer
import java.util.function.Function
import java.util.function.Supplier

/**
 * A builder for the `ActionSpec`.
 */
class ActionSpecBuilder<ArgumentsT, ArgumentsBuilderT, OutputT>
private constructor(
    private val capabilityName: String,
    private val argumentBuilderSupplier: Supplier<ArgumentsBuilderT>,
    private val builderFinalizer: Function<ArgumentsBuilderT, ArgumentsT>
) {
    private val paramBindingList: MutableList<ParamBinding<ArgumentsT, ArgumentsBuilderT>> =
        ArrayList()
    private val outputBindings: MutableMap<String, Function<OutputT, List<ParamValue>>> = HashMap()

    /** Sets the argument type and its builder and returns a new `ActionSpecBuilder`.  */
    @Suppress("UNUSED_PARAMETER")
    fun <NewArgumentsT, NewArgumentsBuilderT> setArguments(
        unused: Class<NewArgumentsT>,
        argumentBuilderSupplier: Supplier<NewArgumentsBuilderT>,
        builderFinalizer: Function<NewArgumentsBuilderT, NewArgumentsT>
    ): ActionSpecBuilder<NewArgumentsT, NewArgumentsBuilderT, OutputT> {
        return ActionSpecBuilder(this.capabilityName, argumentBuilderSupplier, builderFinalizer)
    }

    @Suppress("UNUSED_PARAMETER")
    fun <NewOutputT> setOutput(
        unused: Class<NewOutputT>
    ): ActionSpecBuilder<ArgumentsT, ArgumentsBuilderT, NewOutputT> {
        return ActionSpecBuilder(this.capabilityName,
            this.argumentBuilderSupplier,
            this.builderFinalizer
        )
    }

    /**
     * Binds the parameter name, getter and setter.
     *
     * @param paramName             the name of this action' parameter.
     * @param paramGetter           a getter of the param-specific info from the property.
     * @param argumentSetter        a setter to the argument with the input from `ParamValue`.
     * @param argumentSerializer    a function that serializes this slot on an argument instance.
     * @return the builder itself.
     */
    private fun bindParameterInternal(
        paramName: String,
        argumentSetter: ArgumentSetter<ArgumentsBuilderT>,
        argumentSerializer: (ArgumentsT) -> List<ParamValue>
    ): ActionSpecBuilder<ArgumentsT, ArgumentsBuilderT, OutputT> {
        paramBindingList.add(ParamBinding(paramName, argumentSetter, argumentSerializer))
        return this
    }

    /**
     * Binds the parameter name, and corresponding method references for setting Argument value.
     *
     * If the Property getter returns a null value, this parameter will not exist in the parameter
     * definition of the capability.
     *
     * @param paramName the name of this action' parameter.
     * @param paramGetter the function reference that returns the parameter given [Arguments].
     * @param paramConsumer a setter to set the string value in the argument builder.
     * @param paramValueConverter converter FROM assistant ParamValue proto
     * @return the builder itself.
     */
    fun <T> bindParameter(
        paramName: String,
        paramGetter: (ArgumentsT) -> T?,
        paramConsumer: BiConsumer<in ArgumentsBuilderT, T>,
        paramValueConverter: ParamValueConverter<T>,
    ): ActionSpecBuilder<ArgumentsT, ArgumentsBuilderT, OutputT> {
        return bindParameterInternal(
            paramName,
            { argBuilder: ArgumentsBuilderT, paramList: List<ParamValue> ->
                if (paramList.isNotEmpty()) {
                    paramConsumer.accept(
                        argBuilder,
                        SlotTypeConverter.ofSingular(paramValueConverter).convert(paramList)
                    )
                }
            },
            {
                arguments ->
                listOf(
                    paramGetter(arguments)?.let(paramValueConverter::toParamValue)
                ).filterNotNull()
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
    fun <T> bindRepeatedParameter(
        paramName: String,
        paramGetter: (ArgumentsT) -> List<T>,
        paramConsumer: BiConsumer<in ArgumentsBuilderT, List<T>>,
        paramValueConverter: ParamValueConverter<T>
    ): ActionSpecBuilder<ArgumentsT, ArgumentsBuilderT, OutputT> {
        return bindParameterInternal(
            paramName,
            { argBuilder: ArgumentsBuilderT, paramList: List<ParamValue?>? ->
                paramConsumer.accept(
                    argBuilder,
                    SlotTypeConverter.ofRepeated(paramValueConverter).convert(paramList!!)
                )
            },
            {
                arguments -> paramGetter(arguments).filterNotNull().map(
                    paramValueConverter::toParamValue
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
    // TODO(dennistwo) after all usages of this overload is removed, remove this method
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

    fun <T> bindOutput(
        name: String,
        outputFieldGetter: Function<OutputT, T?>,
        converter: ParamValueConverter<T>
    ) = bindOutput(name, outputFieldGetter, converter::toParamValue)

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

    fun <T> bindRepeatedOutput(
        name: String,
        outputGetter: Function<OutputT, List<T>>,
        converter: ParamValueConverter<T>
    ) = bindRepeatedOutput(name, outputGetter, converter::toParamValue)

    /** Builds an `ActionSpec` from this builder.  */
    fun build(): ActionSpec<ArgumentsT, OutputT> {
        return ActionSpecImpl(
            capabilityName,
            argumentBuilderSupplier,
            paramBindingList.toList(),
            outputBindings.toMap(),
            builderFinalizer
        )
    }

    companion object {
        /**
         * Creates an empty `ActionSpecBuilder` with the given capability name. ArgumentsT is set
         * to Object as a placeholder, which must be replaced by calling setArgument.
         */
        fun ofCapabilityNamed(
            capabilityName: String
        ): ActionSpecBuilder<Any, Any, Any> {
            return ActionSpecBuilder(capabilityName, { Supplier { Object() } }) {
                Function<Any, Any> { Object() }
            }
        }
    }
}
