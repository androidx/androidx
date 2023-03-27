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

package androidx.appactions.interaction.capabilities.core.testing;

import static java.util.stream.Collectors.toMap;

import androidx.appactions.interaction.capabilities.core.impl.ArgumentsWrapper;
import androidx.appactions.interaction.proto.FulfillmentRequest.Fulfillment;
import androidx.appactions.interaction.proto.FulfillmentRequest.Fulfillment.FulfillmentParam;
import androidx.appactions.interaction.proto.ParamValue;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Utilities for creating objects to make testing classes less verbose. */
public final class ArgumentUtils {

    private ArgumentUtils() {
    }

    /**
     * Useful for one-shot BIIs where the task data is not needed and the ParamValues are singular.
     */
    public static ArgumentsWrapper buildArgs(Map<String, ParamValue> args) {
        return ArgumentUtils.buildListArgs(
                args.entrySet().stream()
                        .collect(toMap(e -> e.getKey(),
                                e -> Collections.singletonList(e.getValue()))));
    }

    /** Useful for one-shot BIIs where the task data is not needed. */
    public static ArgumentsWrapper buildListArgs(Map<String, List<ParamValue>> args) {
        Fulfillment.Builder builder = Fulfillment.newBuilder();
        for (Map.Entry<String, List<ParamValue>> entry : args.entrySet()) {
            builder.addParams(
                    FulfillmentParam.newBuilder().setName(entry.getKey()).addAllValues(
                            entry.getValue()));
        }
        return ArgumentsWrapper.create(builder.build());
    }

    private static ParamValue toParamValue(Object argVal) {
        if (argVal instanceof Integer) {
            return ParamValue.newBuilder().setNumberValue(((Integer) argVal).intValue()).build();
        } else if (argVal instanceof Double) {
            return ParamValue.newBuilder().setNumberValue(((Double) argVal).doubleValue()).build();
        } else if (argVal instanceof String) {
            return ParamValue.newBuilder().setStringValue((String) argVal).build();
        } else if (argVal instanceof Enum) {
            return ParamValue.newBuilder().setIdentifier(argVal.toString()).build();
        } else if (argVal instanceof ParamValue) {
            return (ParamValue) argVal;
        }
        throw new IllegalArgumentException("invalid argument type.");
    }

    public static ParamValue buildSearchActionParamValue(String query) {
        return ParamValue.newBuilder()
                .setStructValue(
                        Struct.newBuilder()
                                .putFields("@type",
                                        Value.newBuilder().setStringValue("SearchAction").build())
                                .putFields("query",
                                        Value.newBuilder().setStringValue(query).build())
                                .build())
                .build();
    }

    /**
     * Convenience method to build ArgumentsWrapper based on plain java types. Input args should be
     * even in length, where each String argName is followed by any type of argVal.
     */
    public static ArgumentsWrapper buildRequestArgs(Fulfillment.Type type, Object... args) {
        Fulfillment.Builder builder = Fulfillment.newBuilder();
        if (type != Fulfillment.Type.UNRECOGNIZED) {
            builder.setType(type);
        }
        if (args.length == 0) {
            return ArgumentsWrapper.create(builder.build());
        }
        if (args.length % 2 != 0) {
            throw new IllegalArgumentException("Must call function with even number of args");
        }
        Map<String, List<ParamValue>> argsMap = new LinkedHashMap<>();
        for (int argNamePos = 0, argValPos = 1; argValPos < args.length; ) {
            if (!(args[argNamePos] instanceof String)) {
                throw new IllegalArgumentException(
                        "Argument must be instance of String but got: "
                                + args[argNamePos].getClass());
            }
            String argName = (String) args[argNamePos];
            ParamValue paramValue = toParamValue(args[argValPos]);
            argsMap.computeIfAbsent(argName, (unused) -> new ArrayList<>());
            Objects.requireNonNull(argsMap.get(argName)).add(paramValue);
            argNamePos += 2;
            argValPos += 2;
        }
        argsMap.entrySet().stream()
                .forEach(
                        entry ->
                                builder.addParams(
                                        FulfillmentParam.newBuilder()
                                                .setName(entry.getKey())
                                                .addAllValues(entry.getValue())));
        return ArgumentsWrapper.create(builder.build());
    }
}
