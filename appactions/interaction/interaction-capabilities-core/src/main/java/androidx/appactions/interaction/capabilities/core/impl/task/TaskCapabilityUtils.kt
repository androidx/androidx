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

import androidx.appactions.interaction.proto.AppActionsContext
import androidx.appactions.interaction.proto.CurrentValue
import androidx.appactions.interaction.proto.DisambiguationData
import androidx.appactions.interaction.proto.Entity
import androidx.appactions.interaction.proto.FulfillmentRequest
import androidx.appactions.interaction.proto.ParamValue
import androidx.appactions.interaction.protobuf.Struct
import java.util.Arrays

/** Utility methods used for implementing Task Capabilities. */
internal object TaskCapabilityUtils {
    /** Uses Property to detect if all required arguments are present. */
    fun isSlotFillingComplete(
        finalArguments: Map<String, List<ParamValue>>,
        paramsList: List<AppActionsContext.IntentParameter>
    ) = paramsList.filter { it.isRequired }.map { it.name }.all { finalArguments.containsKey(it) }

    fun paramValuesToCurrentValue(
        paramValueList: List<ParamValue>,
        status: CurrentValue.Status
    ): List<CurrentValue> = paramValueList.map { toCurrentValue(it, status) }

    private fun paramValuesToFulfillmentValues(
        paramValueList: List<ParamValue>
    ): List<FulfillmentRequest.Fulfillment.FulfillmentValue> =
        paramValueList.map {
            FulfillmentRequest.Fulfillment.FulfillmentValue.newBuilder().setValue(it).build()
        }

    fun paramValuesMapToFulfillmentValuesMap(
        paramValueMap: Map<String, List<ParamValue>>
    ): Map<String, List<FulfillmentRequest.Fulfillment.FulfillmentValue>> =
        paramValueMap.entries.associate { entry ->
            entry.key to paramValuesToFulfillmentValues(entry.value)
        }

    fun fulfillmentValuesToCurrentValues(
        fulfillmentValueList: List<FulfillmentRequest.Fulfillment.FulfillmentValue>,
        status: CurrentValue.Status
    ): List<CurrentValue> = fulfillmentValueList.map { toCurrentValue(it, status) }

    fun toCurrentValue(paramValue: ParamValue, status: CurrentValue.Status): CurrentValue =
        CurrentValue.newBuilder().setValue(paramValue).setStatus(status).build()

    private fun toCurrentValue(
        fulfillmentValue: FulfillmentRequest.Fulfillment.FulfillmentValue,
        status: CurrentValue.Status
    ): CurrentValue {
        val result = CurrentValue.newBuilder()
        if (fulfillmentValue.hasValue()) {
            result.value = fulfillmentValue.value
        }
        if (fulfillmentValue.hasDisambigData()) {
            result.disambiguationData = fulfillmentValue.disambigData
        }
        return result.setStatus(status).build()
    }

    fun groundedValueToParamValue(groundedEntity: Entity): ParamValue =
        if (groundedEntity.hasStructValue())
            ParamValue.newBuilder()
                .setIdentifier(groundedEntity.identifier)
                .setStructValue(groundedEntity.structValue)
                .build()
        else
            ParamValue.newBuilder()
                .setIdentifier(groundedEntity.identifier)
                .setStringValue(groundedEntity.name)
                .build()

    /** Create a [CurrentValue] based on Disambiguation result for a ParamValue. */
    fun getCurrentValueForDisambiguation(
        paramValue: ParamValue,
        disambiguationEntities: List<Entity>
    ): CurrentValue =
        CurrentValue.newBuilder()
            .setValue(paramValue)
            .setStatus(CurrentValue.Status.DISAMBIG)
            .setDisambiguationData(
                DisambiguationData.newBuilder().addAllEntities(disambiguationEntities)
            )
            .build()

    /** Compares two [ParamValue], returns false if they are equivalent, true otherwise. */
    private fun hasParamValueDiff(oldArg: ParamValue, newArg: ParamValue): Boolean {
        if (oldArg.valueCase.number != newArg.valueCase.number) {
            return true
        }
        return if (oldArg.identifier != newArg.identifier) {
            true
        } else
            when (oldArg.valueCase) {
                ParamValue.ValueCase.VALUE_NOT_SET -> false
                ParamValue.ValueCase.STRING_VALUE -> oldArg.stringValue != newArg.stringValue
                ParamValue.ValueCase.BOOL_VALUE -> oldArg.boolValue != newArg.boolValue
                ParamValue.ValueCase.NUMBER_VALUE -> oldArg.numberValue != newArg.numberValue
                ParamValue.ValueCase.STRUCT_VALUE ->
                    !Arrays.equals(
                        oldArg.structValue.toByteArray(),
                        newArg.structValue.toByteArray()
                    )
                else -> true
            }
    }

    /**
     * Returns true if we can skip processing of new [fulfillmentValues] for a slot.
     *
     * There are two required conditions for skipping processing:
     * * 1. [currentValues] are all ACCEPTED.
     * * 2. there are no differences between the [ParamValue]s in [currentValues] and
     *   [fulfillmentValues].
     */
    fun canSkipSlotProcessing(
        currentValues: List<CurrentValue>,
        fulfillmentValues: List<FulfillmentRequest.Fulfillment.FulfillmentValue>
    ): Boolean =
        currentValues.all { it.status == CurrentValue.Status.ACCEPTED } &&
            currentValues.size == fulfillmentValues.size &&
            fulfillmentValues.indices.all {
                !hasParamValueDiff(currentValues[it].value, fulfillmentValues[it].value)
            }

    /** Given a [List<CurrentValue>], find all the Struct in them as a Map. */
    private fun getStructsFromCurrentValues(
        currentValues: List<CurrentValue>
    ): Map<String, Struct> {
        val candidates = mutableMapOf<String, Struct>()
        for (currentValue in currentValues) {
            if (
                currentValue.status == CurrentValue.Status.ACCEPTED &&
                    currentValue.value.hasStructValue()
            ) {
                candidates[currentValue.value.identifier] = currentValue.value.structValue
            } else if (currentValue.status == CurrentValue.Status.DISAMBIG) {
                for (entity in currentValue.disambiguationData.entitiesList) {
                    if (entity.hasStructValue()) {
                        candidates[entity.identifier] = entity.structValue
                    }
                }
            }
        }
        return candidates.toMap()
    }

    /**
     * Grounded values for donated inventory slots are sent as identifier only, so find matching
     * Struct from previous turn and add them to the fulfillment values.
     */
    fun getMaybeModifiedSlotValues(
        currentValues: List<CurrentValue>,
        newSlotValues: List<FulfillmentRequest.Fulfillment.FulfillmentValue>
    ): List<FulfillmentRequest.Fulfillment.FulfillmentValue> {
        val candidates = getStructsFromCurrentValues(currentValues)
        return if (candidates.isEmpty()) {
            newSlotValues
        } else
            newSlotValues.map {
                val paramValue = it.value
                if (
                    paramValue.hasIdentifier() &&
                        !paramValue.hasStructValue() &&
                        candidates.containsKey(paramValue.identifier)
                ) {
                    // TODO(b/243944366) throw error if struct filling fails for an
                    //  inventory slot.
                    return@map it.toBuilder()
                        .setValue(
                            paramValue.toBuilder().setStructValue(candidates[paramValue.identifier])
                        )
                        .build()
                }
                it
            }
    }
}
