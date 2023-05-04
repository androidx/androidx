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

package androidx.appactions.interaction.capabilities.core.impl.task

import androidx.annotation.RestrictTo
import androidx.appactions.interaction.capabilities.core.AppEntityListListener
import androidx.appactions.interaction.capabilities.core.AppEntityListener
import androidx.appactions.interaction.capabilities.core.InventoryListListener
import androidx.appactions.interaction.capabilities.core.InventoryListener
import androidx.appactions.interaction.capabilities.core.ValueListener
import androidx.appactions.interaction.capabilities.core.impl.converters.EntityConverter
import androidx.appactions.interaction.capabilities.core.impl.converters.ParamValueConverter
import androidx.appactions.interaction.capabilities.core.impl.converters.SearchActionConverter
import androidx.appactions.interaction.proto.ParamValue

/** Container of multi-turn Task related function references. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class TaskHandler<ConfirmationT>
internal constructor(
    internal val taskParamMap: Map<String, TaskParamBinding<*>>,
    internal val confirmationDataBindings: Map<String, (ConfirmationT) -> List<ParamValue>>,
    internal val onReadyToConfirmListener: OnReadyToConfirmListenerInternal<ConfirmationT>?,
) {
    class Builder<ConfirmationT>() {
        private val mutableTaskParamMap = mutableMapOf<String, TaskParamBinding<*>>()
        private val confirmationDataBindings =
            mutableMapOf<String, (ConfirmationT) -> List<ParamValue>>()
        private var onReadyToConfirmListener: OnReadyToConfirmListenerInternal<ConfirmationT>? =
            null

        fun <ValueTypeT> registerInventoryTaskParam(
            paramName: String,
            listener: InventoryListener<ValueTypeT>,
            converter: ParamValueConverter<ValueTypeT>,
        ): Builder<ConfirmationT> = apply {
            mutableTaskParamMap[paramName] =
                TaskParamBinding(
                    paramName,
                    GROUND_NEVER,
                    GenericResolverInternal.fromInventoryListener(listener),
                    converter,
                    null,
                    null,
                )
        }

        fun <ValueTypeT> registerInventoryListTaskParam(
            paramName: String,
            listener: InventoryListListener<ValueTypeT>,
            converter: ParamValueConverter<ValueTypeT>,
        ): Builder<ConfirmationT> = apply {
            mutableTaskParamMap[paramName] =
                TaskParamBinding(
                    paramName,
                    GROUND_NEVER,
                    GenericResolverInternal.fromInventoryListListener(listener),
                    converter,
                    null,
                    null,
                )
        }

        fun <ValueTypeT> registerAppEntityTaskParam(
            paramName: String,
            listener: AppEntityListener<ValueTypeT>,
            converter: ParamValueConverter<ValueTypeT>,
            entityConverter: EntityConverter<ValueTypeT>,
            searchActionConverter: SearchActionConverter<ValueTypeT>,
        ): Builder<ConfirmationT> = apply {
            mutableTaskParamMap[paramName] =
                TaskParamBinding(
                    paramName,
                    GROUND_IF_NO_IDENTIFIER,
                    GenericResolverInternal.fromAppEntityListener(listener),
                    converter,
                    entityConverter,
                    searchActionConverter,
                )
        }

        fun <ValueTypeT> registerAppEntityListTaskParam(
            paramName: String,
            listener: AppEntityListListener<ValueTypeT>,
            converter: ParamValueConverter<ValueTypeT>,
            entityConverter: EntityConverter<ValueTypeT>,
            searchActionConverter: SearchActionConverter<ValueTypeT>,
        ): Builder<ConfirmationT> = apply {
            mutableTaskParamMap[paramName] =
                TaskParamBinding(
                    paramName,
                    GROUND_IF_NO_IDENTIFIER,
                    GenericResolverInternal.fromAppEntityListListener(listener),
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
