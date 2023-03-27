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

package androidx.appactions.interaction.capabilities.core.impl;

import static androidx.appactions.interaction.capabilities.core.impl.utils.ImmutableCollectors.toImmutableList;

import androidx.annotation.NonNull;
import androidx.appactions.interaction.proto.FulfillmentRequest.Fulfillment;
import androidx.appactions.interaction.proto.FulfillmentRequest.Fulfillment.FulfillmentParam;
import androidx.appactions.interaction.proto.FulfillmentRequest.Fulfillment.FulfillmentValue;

import com.google.auto.value.AutoValue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Represents Fulfillment request sent from assistant, including arguments. */
@SuppressWarnings("AutoValueImmutableFields")
@AutoValue
public abstract class ArgumentsWrapper {

    /**
     * Creates an instance of ArgumentsWrapper based on the Fulfillment send from Assistant.
     *
     * @param fulfillment for a single BII sent from Assistant.
     */
    @NonNull
    public static ArgumentsWrapper create(@NonNull Fulfillment fulfillment) {
        return new AutoValue_ArgumentsWrapper(
                Collections.unmodifiableMap(convertToArgumentMap(fulfillment)),
                createRequestMetadata(fulfillment));
    }

    private static Optional<RequestMetadata> createRequestMetadata(Fulfillment fulfillment) {
        if (fulfillment.getType() == Fulfillment.Type.UNKNOWN_TYPE
                || fulfillment.getType() == Fulfillment.Type.UNRECOGNIZED) {
            return Optional.empty();
        }
        return Optional.of(
                RequestMetadata.newBuilder().setRequestType(fulfillment.getType()).build());
    }

    private static Map<String, List<FulfillmentValue>> convertToArgumentMap(
            Fulfillment fulfillment) {
        Map<String, List<FulfillmentValue>> result = new LinkedHashMap<>();
        for (FulfillmentParam fp : fulfillment.getParamsList()) {
            // Normalize deprecated param value list into new FulfillmentValue.
            if (!fp.getValuesList().isEmpty()) {
                result.put(
                        fp.getName(),
                        fp.getValuesList().stream()
                                .map(paramValue -> FulfillmentValue.newBuilder().setValue(
                                        paramValue).build())
                                .collect(toImmutableList()));
            } else {
                result.put(fp.getName(), fp.getFulfillmentValuesList());
            }
        }
        return result;
    }

    /**
     * A map of BII parameter names to a task param value, where each {@code FulfillmentValue} can
     * have a value and {@code DisambigData} sent from Assistant.
     */
    @NonNull
    public abstract Map<String, List<FulfillmentValue>> paramValues();

    /**
     * Metadata from the FulfillmentRequest on the current Assistant turn. This field should be
     * Optional.empty for one-shot capabilities.
     */
    @NonNull
    public abstract Optional<RequestMetadata> requestMetadata();
}
