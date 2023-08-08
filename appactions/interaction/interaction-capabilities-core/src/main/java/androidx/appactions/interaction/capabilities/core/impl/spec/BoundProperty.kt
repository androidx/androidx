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

import androidx.appactions.interaction.capabilities.core.impl.converters.EntityConverter
import androidx.appactions.interaction.capabilities.core.impl.utils.invokeExternalBlock
import androidx.appactions.interaction.capabilities.core.properties.Property
import androidx.appactions.interaction.proto.AppActionsContext.IntentParameter

/** Wrapper around [Property] and a type-specific EntityConverter. */
data class BoundProperty<T> internal constructor(
    val slotName: String,
    val property: Property<T>,
    val entityConverter: EntityConverter<T>
) {
    /** * Convert this wrapped Property into [IntentParameter] proto representation with
     * current inventory.
     */
    fun convertToProto(): IntentParameter {
        val builder = IntentParameter.newBuilder()
            .setName(slotName)
            .setIsRequired(property.isRequiredForExecution)
            .setEntityMatchRequired(property.shouldMatchPossibleValues)
            .setIsProhibited(!property.isSupported)
        invokeExternalBlock("retrieving possibleValues for $slotName property") {
            property.possibleValues
        }.map {
            entityConverter.convert(it)
        }.forEach {
            builder.addPossibleEntities(it)
        }
        return builder.build()
    }
}
