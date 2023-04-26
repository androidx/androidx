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

import androidx.appactions.interaction.capabilities.core.AppEntityListListener
import androidx.appactions.interaction.capabilities.core.AppEntityListener
import androidx.appactions.interaction.capabilities.core.EntitySearchResult
import androidx.appactions.interaction.capabilities.core.InventoryListListener
import androidx.appactions.interaction.capabilities.core.InventoryListener
import androidx.appactions.interaction.capabilities.core.ValidationResult
import androidx.appactions.interaction.capabilities.core.ValueListener
import androidx.appactions.interaction.capabilities.core.impl.converters.ParamValueConverter
import androidx.appactions.interaction.capabilities.core.impl.converters.SlotTypeConverter
import androidx.appactions.interaction.capabilities.core.impl.exceptions.StructConversionException
import androidx.appactions.interaction.capabilities.core.impl.task.exceptions.InvalidResolverException
import androidx.appactions.interaction.capabilities.core.SearchAction
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
    val appEntity: AppEntityListener<ValueTypeT>? = null,
    val appEntityList: AppEntityListListener<ValueTypeT>? = null,
    val inventory: InventoryListener<ValueTypeT>? = null,
    val inventoryList: InventoryListListener<ValueTypeT>? = null,
) {

    /** Wrapper which should invoke the `lookupAndRender` provided by the developer. */
    @Throws(InvalidResolverException::class)
    suspend fun invokeLookup(
        searchAction: SearchAction<ValueTypeT>,
    ): EntitySearchResult<ValueTypeT> {
        return if (appEntity != null) {
            appEntity.lookupAndRender(searchAction)
        } else if (appEntityList != null) {
            appEntityList.lookupAndRender(searchAction)
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
        if (inventory != null) {
            inventory.renderChoices(entityIds)
        } else if (inventoryList != null) {
            inventoryList.renderChoices(entityIds)
        } else {
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
        converter: ParamValueConverter<ValueTypeT>,
    ): ValidationResult {
        val singularConverter = SlotTypeConverter.ofSingular(converter)
        val repeatedConverter = SlotTypeConverter.ofRepeated(converter)
        return when {
            value != null -> value.onReceived(singularConverter.convert(paramValues))
            valueList != null -> valueList.onReceived(repeatedConverter.convert(paramValues))
            appEntity != null ->
                appEntity.onReceived(singularConverter.convert(paramValues))
            appEntityList != null ->
                appEntityList.onReceived(repeatedConverter.convert(paramValues))
            inventory != null ->
                inventory.onReceived(singularConverter.convert(paramValues))
            inventoryList != null ->
                inventoryList.onReceived(repeatedConverter.convert(paramValues))
            else -> throw IllegalStateException("unreachable")
        }
    }

    companion object {
        fun <ValueTypeT> fromValueListener(valueListener: ValueListener<ValueTypeT>) =
            GenericResolverInternal(value = valueListener)

        fun <ValueTypeT> fromValueListListener(valueListListener: ValueListener<List<ValueTypeT>>) =
            GenericResolverInternal(valueList = valueListListener)

        fun <ValueTypeT> fromAppEntityListener(appEntity: AppEntityListener<ValueTypeT>) =
            GenericResolverInternal(appEntity = appEntity)

        fun <ValueTypeT> fromAppEntityListListener(
            appEntityList: AppEntityListListener<ValueTypeT>,
        ) = GenericResolverInternal(appEntityList = appEntityList)

        fun <ValueTypeT> fromInventoryListener(inventory: InventoryListener<ValueTypeT>) =
            GenericResolverInternal(inventory = inventory)

        fun <ValueTypeT> fromInventoryListListener(
            inventoryList: InventoryListListener<ValueTypeT>,
        ) = GenericResolverInternal(inventoryList = inventoryList)
    }
}
