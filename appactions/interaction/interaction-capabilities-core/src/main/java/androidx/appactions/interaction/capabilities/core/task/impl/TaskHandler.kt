/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

package androidx.appactions.interaction.capabilities.core.task.impl

import androidx.appactions.interaction.capabilities.core.impl.converters.DisambigEntityConverter
import androidx.appactions.interaction.capabilities.core.impl.converters.ParamValueConverter
import androidx.appactions.interaction.capabilities.core.impl.converters.SearchActionConverter
import androidx.appactions.interaction.capabilities.core.task.AppEntityListResolver
import androidx.appactions.interaction.capabilities.core.task.AppEntityResolver
import androidx.appactions.interaction.capabilities.core.task.InventoryListResolver
import androidx.appactions.interaction.capabilities.core.task.InventoryResolver
import androidx.appactions.interaction.capabilities.core.task.ValueListListener
import androidx.appactions.interaction.capabilities.core.task.ValueListener
import androidx.appactions.interaction.proto.ParamValue
import java.util.Optional
import java.util.function.Function
import java.util.function.Predicate

/**
 * Container of multi-turn Task related function references.
 *
 */
data class TaskHandler<ConfirmationT> (
    val taskParamRegistry: TaskParamRegistry,
    val confirmationType: ConfirmationType,
    val confirmationDataBindings: Map<String, Function<ConfirmationT, List<ParamValue>>>,
    val onReadyToConfirmListener: OnReadyToConfirmListenerInternal<ConfirmationT>?,
) {
    class Builder<ConfirmationT>(
        val confirmationType: ConfirmationType = ConfirmationType.NOT_SUPPORTED,
    ) {
        val taskParamRegistryBuilder = TaskParamRegistry.builder()
        val confirmationDataBindings: MutableMap<
            String, Function<ConfirmationT, List<ParamValue>>,> = mutableMapOf()
        var onReadyToConfirmListener: OnReadyToConfirmListenerInternal<ConfirmationT>? = null

        fun <ValueTypeT> registerInventoryTaskParam(
            paramName: String,
            listener: InventoryResolver<ValueTypeT>,
            converter: ParamValueConverter<ValueTypeT>,
        ): Builder<ConfirmationT> {
            taskParamRegistryBuilder.addTaskParameter(
                paramName,
                GROUND_IF_NO_IDENTIFIER,
                GenericResolverInternal.fromInventoryResolver(listener),
                Optional.empty(),
                Optional.empty(),
                converter,
            )
            return this
        }

        fun <ValueTypeT> registerInventoryListTaskParam(
            paramName: String,
            listener: InventoryListResolver<ValueTypeT>,
            converter: ParamValueConverter<ValueTypeT>,
        ): Builder<ConfirmationT> {
            taskParamRegistryBuilder.addTaskParameter(
                paramName,
                GROUND_IF_NO_IDENTIFIER,
                GenericResolverInternal.fromInventoryListResolver(listener),
                Optional.empty(),
                Optional.empty(),
                converter,
            )
            return this
        }

        fun <ValueTypeT> registerAppEntityTaskParam(
            paramName: String,
            listener: AppEntityResolver<ValueTypeT>,
            converter: ParamValueConverter<ValueTypeT>,
            entityConverter: DisambigEntityConverter<ValueTypeT>,
            searchActionConverter: SearchActionConverter<ValueTypeT>,
        ): Builder<ConfirmationT> {
            taskParamRegistryBuilder.addTaskParameter(
                paramName,
                GROUND_IF_NO_IDENTIFIER,
                GenericResolverInternal.fromAppEntityResolver(listener),
                Optional.of(entityConverter),
                Optional.of(searchActionConverter),
                converter,
            )
            return this
        }

        fun <ValueTypeT> registerAppEntityListTaskParam(
            paramName: String,
            listener: AppEntityListResolver<ValueTypeT>,
            converter: ParamValueConverter<ValueTypeT>,
            entityConverter: DisambigEntityConverter<ValueTypeT>,
            searchActionConverter: SearchActionConverter<ValueTypeT>,
        ): Builder<ConfirmationT> {
            taskParamRegistryBuilder.addTaskParameter(
                paramName,
                GROUND_IF_NO_IDENTIFIER,
                GenericResolverInternal.fromAppEntityListResolver(listener),
                Optional.of(entityConverter),
                Optional.of(searchActionConverter),
                converter,
            )
            return this
        }

        fun <ValueTypeT> registerValueTaskParam(
            paramName: String,
            listener: ValueListener<ValueTypeT>,
            converter: ParamValueConverter<ValueTypeT>,
        ): Builder<ConfirmationT> {
            taskParamRegistryBuilder.addTaskParameter(
                paramName,
                GROUND_NEVER,
                GenericResolverInternal.fromValueListener(listener),
                Optional.empty(),
                Optional.empty(),
                converter,
            )
            return this
        }

        fun <ValueTypeT> registerValueListTaskParam(
            paramName: String,
            listener: ValueListListener<ValueTypeT>,
            converter: ParamValueConverter<ValueTypeT>,
        ): Builder<ConfirmationT> {
            taskParamRegistryBuilder.addTaskParameter(
                paramName,
                GROUND_NEVER,
                GenericResolverInternal.fromValueListListener(listener),
                Optional.empty(),
                Optional.empty(),
                converter,
            )
            return this
        }

        /**
         * Registers an optional, non-repeated confirmation data.
         *
         * @param paramName          the BIC confirmation data slot name of this parameter.
         * @param confirmationGetter a getter of the confirmation data from the {@code ConfirmationT}
         *                           instance.
         * @param converter          a converter from confirmation data to a ParamValue.
         */
        fun <T> registerConfirmationOutput(
            paramName: String,
            confirmationGetter: (ConfirmationT) -> T?,
            converter: (T) -> ParamValue,
        ): Builder<ConfirmationT> {
            confirmationDataBindings.put(
                paramName,
                { output: ConfirmationT ->
                    listOfNotNull(confirmationGetter(output)).map(converter)
                },
            )
            return this
        }

        /** Sets the onReadyToConfirmListener for this capability. */
        fun setOnReadyToConfirmListenerInternal(
            onReadyToConfirmListener: OnReadyToConfirmListenerInternal<ConfirmationT>,
        ): Builder<ConfirmationT> {
            this.onReadyToConfirmListener = onReadyToConfirmListener
            return this
        }

        fun build(): TaskHandler<ConfirmationT> {
            return TaskHandler(
                taskParamRegistryBuilder.build(),
                confirmationType,
                confirmationDataBindings,
                onReadyToConfirmListener,
            )
        }
        companion object {
            val GROUND_IF_NO_IDENTIFIER = Predicate<ParamValue> {
                    paramValue ->
                !paramValue.hasIdentifier()
            }
            val GROUND_NEVER = Predicate<ParamValue> {
                    _ ->
                false
            }
        }
    }
}

enum class ConfirmationType {
    // Confirmation is not supported for this Capability.
    NOT_SUPPORTED,

    // This Capability requires confirmation.
    REQUIRED,

    // Confirmation is optional for this Capability.
    OPTIONAL,
}
