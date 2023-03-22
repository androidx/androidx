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

package androidx.appactions.interaction.capabilities.core.task.impl

import androidx.annotation.RestrictTo
import androidx.appactions.interaction.capabilities.core.impl.converters.EntityConverter
import androidx.appactions.interaction.capabilities.core.impl.converters.ParamValueConverter
import androidx.appactions.interaction.capabilities.core.impl.converters.SearchActionConverter
import androidx.appactions.interaction.capabilities.core.task.AppEntityListResolver
import androidx.appactions.interaction.capabilities.core.task.AppEntityResolver
import androidx.appactions.interaction.capabilities.core.task.InventoryListResolver
import androidx.appactions.interaction.capabilities.core.task.InventoryResolver
import androidx.appactions.interaction.capabilities.core.task.ValueListener
import androidx.appactions.interaction.proto.ParamValue

/** Container of multi-turn Task related function references. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class TaskHandler<ConfirmationT>
internal constructor(
    internal val taskParamMap: Map<String, TaskParamBinding<*>>,
    internal val confirmationType: ConfirmationType,
    internal val confirmationDataBindings: Map<String, (ConfirmationT) -> List<ParamValue>>,
    internal val onReadyToConfirmListener: OnReadyToConfirmListenerInternal<ConfirmationT>?,
) {
    class Builder<ConfirmationT>(
        private val confirmationType: ConfirmationType = ConfirmationType.NOT_SUPPORTED,
    ) {
        private val mutableTaskParamMap = mutableMapOf<String, TaskParamBinding<*>>()
        private val confirmationDataBindings =
            mutableMapOf<String, (ConfirmationT) -> List<ParamValue>>()
        private var onReadyToConfirmListener: OnReadyToConfirmListenerInternal<ConfirmationT>? =
            null

        fun <ValueTypeT> registerInventoryTaskParam(
            paramName: String,
            listener: InventoryResolver<ValueTypeT>,
            converter: ParamValueConverter<ValueTypeT>,
        ): Builder<ConfirmationT> = apply {
            mutableTaskParamMap[paramName] =
                TaskParamBinding(
                    paramName,
                    GROUND_IF_NO_IDENTIFIER,
                    GenericResolverInternal.fromInventoryResolver(listener),
                    converter,
                    null,
                    null,
                )
        }

        fun <ValueTypeT> registerInventoryListTaskParam(
            paramName: String,
            listener: InventoryListResolver<ValueTypeT>,
            converter: ParamValueConverter<ValueTypeT>,
        ): Builder<ConfirmationT> = apply {
            mutableTaskParamMap[paramName] =
                TaskParamBinding(
                    paramName,
                    GROUND_IF_NO_IDENTIFIER,
                    GenericResolverInternal.fromInventoryListResolver(listener),
                    converter,
                    null,
                    null,
                )
        }

        fun <ValueTypeT> registerAppEntityTaskParam(
            paramName: String,
            listener: AppEntityResolver<ValueTypeT>,
            converter: ParamValueConverter<ValueTypeT>,
            entityConverter: EntityConverter<ValueTypeT>,
            searchActionConverter: SearchActionConverter<ValueTypeT>,
        ): Builder<ConfirmationT> = apply {
            mutableTaskParamMap[paramName] =
                TaskParamBinding(
                    paramName,
                    GROUND_IF_NO_IDENTIFIER,
                    GenericResolverInternal.fromAppEntityResolver(listener),
                    converter,
                    entityConverter,
                    searchActionConverter,
                )
        }

        fun <ValueTypeT> registerAppEntityListTaskParam(
            paramName: String,
            listener: AppEntityListResolver<ValueTypeT>,
            converter: ParamValueConverter<ValueTypeT>,
            entityConverter: EntityConverter<ValueTypeT>,
            searchActionConverter: SearchActionConverter<ValueTypeT>,
        ): Builder<ConfirmationT> = apply {
            mutableTaskParamMap[paramName] =
                TaskParamBinding(
                    paramName,
                    GROUND_IF_NO_IDENTIFIER,
                    GenericResolverInternal.fromAppEntityListResolver(listener),
                    converter,
                    entityConverter,
                    searchActionConverter,
                )
        }

        fun <ValueTypeT> registerValueTaskParam(
            paramName: String,
            listener: ValueListener<ValueTypeT>,
            converter: ParamValueConverter<ValueTypeT>,
        ): Builder<ConfirmationT> = apply {
            mutableTaskParamMap[paramName] =
                TaskParamBinding(
                    paramName,
                    GROUND_NEVER,
                    GenericResolverInternal.fromValueListener(listener),
                    converter,
                    null,
                    null,
                )
        }

        fun <ValueTypeT> registerValueListTaskParam(
            paramName: String,
            listener: ValueListener<List<ValueTypeT>>,
            converter: ParamValueConverter<ValueTypeT>,
        ): Builder<ConfirmationT> = apply {
            mutableTaskParamMap[paramName] =
                TaskParamBinding(
                    paramName,
                    GROUND_NEVER,
                    GenericResolverInternal.fromValueListListener(listener),
                    converter,
                    null,
                    null,
                )
        }

        /**
         * Registers an optional, non-repeated confirmation data.
         *
         * @param paramName the BIC confirmation data slot name of this parameter.
         * @param confirmationGetter a getter of the confirmation data from the {@code
         *   ConfirmationT} instance.
         * @param converter a converter from confirmation data to a ParamValue.
         */
        fun <T> registerConfirmationOutput(
            paramName: String,
            confirmationGetter: (ConfirmationT) -> T?,
            converter: (T) -> ParamValue,
        ): Builder<ConfirmationT> = apply {
            confirmationDataBindings[paramName] = { output: ConfirmationT ->
                listOfNotNull(confirmationGetter(output)).map(converter)
            }
        }

        /** Sets the onReadyToConfirmListener for this capability. */
        fun setOnReadyToConfirmListenerInternal(
            onReadyToConfirmListener: OnReadyToConfirmListenerInternal<ConfirmationT>,
        ): Builder<ConfirmationT> = apply {
            this.onReadyToConfirmListener = onReadyToConfirmListener
        }

        fun build(): TaskHandler<ConfirmationT> {
            return TaskHandler(
                mutableTaskParamMap.toMap(),
                confirmationType,
                confirmationDataBindings,
                onReadyToConfirmListener,
            )
        }
        companion object {
            val GROUND_IF_NO_IDENTIFIER = { paramValue: ParamValue -> !paramValue.hasIdentifier() }
            val GROUND_NEVER = { _: ParamValue -> false }
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
