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

package androidx.appactions.interaction.capabilities.core.task.impl;

import static androidx.appactions.interaction.capabilities.core.impl.utils.ImmutableCollectors.toImmutableList;
import static androidx.appactions.interaction.capabilities.core.impl.utils.ImmutableCollectors.toImmutableMap;
import static androidx.appactions.interaction.capabilities.core.impl.utils.ImmutableCollectors.toImmutableSet;

import androidx.annotation.NonNull;
import androidx.appactions.interaction.capabilities.core.task.impl.exceptions.MissingRequiredArgException;
import androidx.appactions.interaction.proto.AppActionsContext.IntentParameter;
import androidx.appactions.interaction.proto.CurrentValue;
import androidx.appactions.interaction.proto.CurrentValue.Status;
import androidx.appactions.interaction.proto.DisambiguationData;
import androidx.appactions.interaction.proto.Entity;
import androidx.appactions.interaction.proto.FulfillmentRequest.Fulfillment.FulfillmentValue;
import androidx.appactions.interaction.proto.ParamValue;

import com.google.protobuf.Struct;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

/** Utility methods used for implementing Task Capabilities. */
public final class TaskCapabilityUtils {
    private TaskCapabilityUtils() {
    }

    /** Uses Property to detect if all required arguments are present. */
    static boolean isSlotFillingComplete(
            Map<String, List<ParamValue>> finalArguments, List<IntentParameter> paramsList) {
        Set<String> requiredParams =
                paramsList.stream()
                        .filter(IntentParameter::getIsRequired)
                        .map(IntentParameter::getName)
                        .collect(toImmutableSet());
        for (String paramName : requiredParams) {
            if (!finalArguments.containsKey(paramName)) {
                return false;
            }
        }
        return true;
    }

    static List<CurrentValue> paramValuesToCurrentValue(
            List<ParamValue> paramValueList, Status status) {
        return paramValueList.stream()
                .map(paramValue -> toCurrentValue(paramValue, status))
                .collect(toImmutableList());
    }

    static List<FulfillmentValue> paramValuesToFulfillmentValues(List<ParamValue> paramValueList) {
        return paramValueList.stream()
                .map(paramValue -> FulfillmentValue.newBuilder().setValue(paramValue).build())
                .collect(toImmutableList());
    }

    static Map<String, List<FulfillmentValue>> paramValuesMapToFulfillmentValuesMap(
            Map<String, List<ParamValue>> paramValueMap) {
        return paramValueMap.entrySet().stream()
                .collect(
                        toImmutableMap(
                                Map.Entry::getKey,
                                (entry) -> paramValuesToFulfillmentValues(entry.getValue())));
    }

    static List<CurrentValue> fulfillmentValuesToCurrentValues(
            List<FulfillmentValue> fulfillmentValueList, Status status) {
        return fulfillmentValueList.stream()
                .map(fulfillmentValue -> toCurrentValue(fulfillmentValue, status))
                .collect(toImmutableList());
    }

    static CurrentValue toCurrentValue(ParamValue paramValue, Status status) {
        return CurrentValue.newBuilder().setValue(paramValue).setStatus(status).build();
    }

    static CurrentValue toCurrentValue(FulfillmentValue fulfillmentValue, Status status) {
        CurrentValue.Builder result = CurrentValue.newBuilder();
        if (fulfillmentValue.hasValue()) {
            result.setValue(fulfillmentValue.getValue());
        }
        if (fulfillmentValue.hasDisambigData()) {
            result.setDisambiguationData(fulfillmentValue.getDisambigData());
        }
        return result.setStatus(status).build();
    }

    static ParamValue groundedValueToParamValue(Entity groundedEntity) {
        if (groundedEntity.hasValue()) {
            return ParamValue.newBuilder()
                    .setIdentifier(groundedEntity.getIdentifier())
                    .setStructValue(groundedEntity.getValue())
                    .build();
        } else {
            return ParamValue.newBuilder()
                    .setIdentifier(groundedEntity.getIdentifier())
                    .setStringValue(groundedEntity.getName())
                    .build();
        }
    }

    /** Create a CurrentValue based on Disambugation result for a ParamValue. */
    static CurrentValue getCurrentValueForDisambiguation(
            ParamValue paramValue, List<Entity> disambiguationEntities) {
        return CurrentValue.newBuilder()
                .setValue(paramValue)
                .setStatus(Status.DISAMBIG)
                .setDisambiguationData(
                        DisambiguationData.newBuilder().addAllEntities(disambiguationEntities))
                .build();
    }

    /** Convenience method to be used in onFinishListeners. */
    @NonNull
    public static List<ParamValue> checkRequiredArg(
            @NonNull Map<String, List<ParamValue>> args, @NonNull String argName)
            throws MissingRequiredArgException {
        List<ParamValue> result = args.get(argName);
        if (result == null) {
            throw new MissingRequiredArgException(
                    String.format(
                            "'%s' is a required argument but is missing from the final arguments "
                                    + "map.",
                            argName));
        }
        return result;
    }

    /** Compares two ParamValue, returns false if they are equivalent, true otherwise. */
    private static boolean hasParamValueDiff(ParamValue oldArg, ParamValue newArg) {
        if (oldArg.getValueCase().getNumber() != newArg.getValueCase().getNumber()) {
            return true;
        }
        if (!oldArg.getIdentifier().equals(newArg.getIdentifier())) {
            return true;
        }
        switch (oldArg.getValueCase()) {
            case VALUE_NOT_SET:
                return false;
            case STRING_VALUE:
                return !oldArg.getStringValue().equals(newArg.getStringValue());
            case BOOL_VALUE:
                return oldArg.getBoolValue() != newArg.getBoolValue();
            case NUMBER_VALUE:
                return oldArg.getNumberValue() != newArg.getNumberValue();
            case STRUCT_VALUE:
                return !Arrays.equals(
                        oldArg.getStructValue().toByteArray(),
                        newArg.getStructValue().toByteArray());
        }
        return true;
    }

    /**
     * Returns true if we can skip processing of new FulfillmentValues for a slot.
     *
     * <p>There are two required conditions for skipping processing:
     *
     * <ul>
     *   <li>1. currentValues are all ACCEPTED.
     *   <li>2. there are no differences between the ParamValues in currentValues and
     *       fulfillmentValues.
     * </ul>
     */
    static boolean canSkipSlotProcessing(
            List<CurrentValue> currentValues, List<FulfillmentValue> fulfillmentValues) {
        if (currentValues.stream()
                .allMatch(currentValue -> currentValue.getStatus().equals(Status.ACCEPTED))) {
            if (currentValues.size() == fulfillmentValues.size()) {
                return IntStream.range(0, fulfillmentValues.size())
                        .allMatch(
                                i ->
                                        !TaskCapabilityUtils.hasParamValueDiff(
                                                currentValues.get(i).getValue(),
                                                fulfillmentValues.get(i).getValue()));
            }
        }
        return false;
    }

    /** Given a {@code List<CurrentValue>} find all the Struct in them as a Map. */
    private static Map<String, Struct> getStructsFromCurrentValues(
            List<CurrentValue> currentValues) {
        Map<String, Struct> candidates = new HashMap<>();
        for (CurrentValue currentValue : currentValues) {
            if (currentValue.getStatus() == CurrentValue.Status.ACCEPTED
                    && currentValue.getValue().hasStructValue()) {
                candidates.put(
                        currentValue.getValue().getIdentifier(),
                        currentValue.getValue().getStructValue());
            } else if (currentValue.getStatus() == CurrentValue.Status.DISAMBIG) {
                for (Entity entity : currentValue.getDisambiguationData().getEntitiesList()) {
                    if (entity.hasValue()) {
                        candidates.put(entity.getIdentifier(), entity.getValue());
                    }
                }
            }
        }
        return Collections.unmodifiableMap(candidates);
    }

    /**
     * Grounded values for donated inventory slots are sent as identifier only, so find matching
     * Struct from previous turn and add them to the fulfillment values.
     */
    static List<FulfillmentValue> getMaybeModifiedSlotValues(
            List<CurrentValue> currentValues, List<FulfillmentValue> newSlotValues) {
        Map<String, Struct> candidates = getStructsFromCurrentValues(currentValues);
        if (candidates.isEmpty()) {
            return newSlotValues;
        }
        return newSlotValues.stream()
                .map(
                        fulfillmentValue -> {
                            ParamValue paramValue = fulfillmentValue.getValue();
                            if (paramValue.hasIdentifier()
                                    && !paramValue.hasStructValue()
                                    && candidates.containsKey(paramValue.getIdentifier())) {
                                // TODO(b/243944366) throw error if struct filling fails for an
                                //  inventory slot.
                                return fulfillmentValue.toBuilder()
                                        .setValue(
                                                paramValue.toBuilder()
                                                        .setStructValue(candidates.get(
                                                                paramValue.getIdentifier())))
                                        .build();
                            }
                            return fulfillmentValue;
                        })
                .collect(toImmutableList());
    }
}
