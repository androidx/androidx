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

import androidx.appactions.interaction.capabilities.core.impl.converters.ParamValueConverter
import androidx.appactions.interaction.capabilities.core.impl.converters.SlotTypeConverter
import androidx.appactions.interaction.capabilities.core.impl.exceptions.StructConversionException
import androidx.appactions.interaction.capabilities.core.task.AppEntityListResolver
import androidx.appactions.interaction.capabilities.core.task.AppEntityResolver
import androidx.appactions.interaction.capabilities.core.task.EntitySearchResult
import androidx.appactions.interaction.capabilities.core.task.InventoryListResolver
import androidx.appactions.interaction.capabilities.core.task.InventoryResolver
import androidx.appactions.interaction.capabilities.core.task.ValidationResult
import androidx.appactions.interaction.capabilities.core.task.ValueListener
import androidx.appactions.interaction.capabilities.core.task.impl.exceptions.InvalidResolverException
import androidx.appactions.interaction.capabilities.core.values.SearchAction
import androidx.appactions.interaction.proto.ParamValue

/**
 * A wrapper around all types of slot resolvers (value listeners + disambig resolvers).
 *
 * This allows one type of resolver to be bound for each slot, and abstracts the details of the
 * individual resolvers. It is also the place where repeated fields are handled.
 *
 * </ValueTypeT>
 */
internal class GenericResolverInternal<ValueTypeT>
private constructor(
    val value: ValueListener<ValueTypeT>? = null,
    val valueList: ValueListener<List<ValueTypeT>>? = null,
    val appEntityResolver: AppEntityResolver<ValueTypeT>? = null,
    val appEntityListResolver: AppEntityListResolver<ValueTypeT>? = null,
    val inventoryResolver: InventoryResolver<ValueTypeT>? = null,
    val inventoryListResolver: InventoryListResolver<ValueTypeT>? = null,
) {

    /** Wrapper which should invoke the `lookupAndRender` provided by the developer. */
    @Throws(InvalidResolverException::class)
    suspend fun invokeLookup(
        searchAction: SearchAction<ValueTypeT>
    ): EntitySearchResult<ValueTypeT> {
        return if (appEntityResolver != null) {
            appEntityResolver.lookupAndRender(searchAction)
        } else if (appEntityListResolver != null) {
            appEntityListResolver.lookupAndRender(searchAction)
        } else {
            throw InvalidResolverException("invokeLookup is not supported on this resolver")
        }
    }

    /**
     * Wrapper which should invoke the EntityRender#renderEntities method when the Assistant is
     * prompting for disambiguation.
     */
    @Throws(InvalidResolverException::class)
    suspend fun invokeEntityRender(entityIds: List<String>) {
        if (inventoryResolver != null) {
            inventoryResolver.renderChoices(entityIds)
        } else if (inventoryListResolver != null) inventoryListResolver.renderChoices(entityIds)
        else {
            throw InvalidResolverException("invokeEntityRender is not supported on this resolver")
        }
    }

    /**
     * Notifies the app that a new value for this argument has been set by Assistant. This method
     * should only be called with completely grounded values.
     */
    @Throws(StructConversionException::class)
    suspend fun notifyValueChange(
        paramValues: List<ParamValue>,
        converter: ParamValueConverter<ValueTypeT>
    ): ValidationResult {
        val singularConverter = SlotTypeConverter.ofSingular(converter)
        val repeatedConverter = SlotTypeConverter.ofRepeated(converter)
        return when {
            value != null -> value.onReceived(singularConverter.convert(paramValues))
            valueList != null -> valueList.onReceived(repeatedConverter.convert(paramValues))
            appEntityResolver != null ->
                appEntityResolver.onReceived(singularConverter.convert(paramValues))
            appEntityListResolver != null ->
                appEntityListResolver.onReceived(repeatedConverter.convert(paramValues))
            inventoryResolver != null ->
                inventoryResolver.onReceived(singularConverter.convert(paramValues))
            inventoryListResolver != null ->
                inventoryListResolver.onReceived(repeatedConverter.convert(paramValues))
            else -> throw IllegalStateException("unreachable")
        }
    }

    companion object {
        fun <ValueTypeT> fromValueListener(valueListener: ValueListener<ValueTypeT>) =
            GenericResolverInternal(value = valueListener)

        fun <ValueTypeT> fromValueListListener(valueListListener: ValueListener<List<ValueTypeT>>) =
            GenericResolverInternal(valueList = valueListListener)

        fun <ValueTypeT> fromAppEntityResolver(appEntityResolver: AppEntityResolver<ValueTypeT>) =
            GenericResolverInternal(appEntityResolver = appEntityResolver)

        fun <ValueTypeT> fromAppEntityListResolver(
            appEntityListResolver: AppEntityListResolver<ValueTypeT>
        ) = GenericResolverInternal(appEntityListResolver = appEntityListResolver)

        fun <ValueTypeT> fromInventoryResolver(inventoryResolver: InventoryResolver<ValueTypeT>) =
            GenericResolverInternal(inventoryResolver = inventoryResolver)

        fun <ValueTypeT> fromInventoryListResolver(
            inventoryListResolver: InventoryListResolver<ValueTypeT>
        ) = GenericResolverInternal(inventoryListResolver = inventoryListResolver)
    }
}
