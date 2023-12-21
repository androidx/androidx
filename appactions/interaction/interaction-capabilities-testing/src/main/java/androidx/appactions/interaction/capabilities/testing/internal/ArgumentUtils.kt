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

package androidx.appactions.interaction.capabilities.testing.internal

import androidx.appactions.interaction.capabilities.core.impl.ArgumentsWrapper
import androidx.appactions.interaction.proto.FulfillmentRequest.Fulfillment
import androidx.appactions.interaction.proto.FulfillmentRequest.Fulfillment.FulfillmentParam
import androidx.appactions.interaction.proto.FulfillmentRequest.Fulfillment.FulfillmentValue
import androidx.appactions.interaction.proto.ParamValue
import androidx.appactions.interaction.protobuf.Struct
import androidx.appactions.interaction.protobuf.Value

object ArgumentUtils {
    /**
     * Useful for one-shot BIIs where the task data is not needed and the ParamValues are singular.
     */
    fun buildArgs(args: Map<String, ParamValue>): ArgumentsWrapper {
        return buildListArgs(args.mapValues { listOf(it.value) })
    }

    /** Useful for one-shot BIIs where the task data is not needed.  */
    fun buildListArgs(args: Map<String, List<ParamValue>>): ArgumentsWrapper {
        val builder = Fulfillment.newBuilder()
        for ((key, valueList) in args) {
            val paramBuilder = FulfillmentParam.newBuilder().setName(key)
            for (value in valueList) {
                builder.addParams(
                    paramBuilder.addFulfillmentValues(
                        FulfillmentValue.newBuilder().setValue(value).build()
                    )
                )
            }
        }
        return ArgumentsWrapper.create(builder.build())
    }

    private fun toParamValue(argVal: Any): ParamValue {
        when (argVal) {
            is Int -> {
                return ParamValue.newBuilder().setNumberValue(argVal.toDouble()).build()
            }

            is Double -> {
                return ParamValue.newBuilder().setNumberValue(argVal).build()
            }

            is String -> {
                return ParamValue.newBuilder().setStringValue(argVal).build()
            }

            is Enum<*> -> {
                return ParamValue.newBuilder().setIdentifier(argVal.toString()).build()
            }

            is ParamValue -> {
                return argVal
            }

            else -> throw IllegalArgumentException("invalid argument type.")
        }
    }

    fun buildSearchActionParamValue(query: String): ParamValue {
        return ParamValue.newBuilder()
            .setStructValue(
                Struct.newBuilder()
                    .putFields(
                        "@type",
                        Value.newBuilder().setStringValue("SearchAction").build()
                    )
                    .putFields(
                        "query",
                        Value.newBuilder().setStringValue(query).build()
                    )
                    .build()
            )
            .build()
    }

    /**
     * Convenience method to build ArgumentsWrapper based on plain java types. Input args should be
     * even in length, where each String argName is followed by any type of argVal. This method
     * will use SLOTS_COMPLETE as the default value for the SyncStatus
     */
    fun buildRequestArgs(
        type: Fulfillment.Type,
        vararg args: Any,
    ): ArgumentsWrapper {
        return buildRequestArgs(type, Fulfillment.SyncStatus.SLOTS_COMPLETE, *args)
    }

    /**
     * Convenience method to build ArgumentsWrapper based on plain java types and
     * also sets the SyncStatus. Input args should be even in length, where each String
     * argName is followed by any type of argVal.
     */
    fun buildRequestArgs(
        type: Fulfillment.Type,
        syncStatus: Fulfillment.SyncStatus,
        vararg args: Any,
    ): ArgumentsWrapper {
        val builder = Fulfillment.newBuilder().addAllParams(buildFulfillmentParams(*args))
        if (type != Fulfillment.Type.UNRECOGNIZED) {
            builder.type = type
            if (type == Fulfillment.Type.SYNC &&
                syncStatus != Fulfillment.SyncStatus.UNRECOGNIZED) {
                builder.syncStatus = syncStatus
            }
        }
        return ArgumentsWrapper.create(builder.build())
    }

    private fun buildFulfillmentParams(vararg args: Any): List<FulfillmentParam> {
        if (args.isEmpty()) {
            return emptyList()
        }
        val fulfillmentParams = mutableListOf<FulfillmentParam>()
        require(args.size % 2 == 0) { "Must call function with even number of args" }
        val argsMap: MutableMap<String, MutableList<ParamValue>> = LinkedHashMap()
        var argNamePos = 0
        var argValPos = 1
        while (argValPos < args.size) {
            require(args[argNamePos] is String) {
                ("Argument must be instance of String but got: ${args[argNamePos].javaClass}")
            }
            val argName = args[argNamePos] as String
            val paramValue: ParamValue = toParamValue(
                args[argValPos]
            )
            argsMap.computeIfAbsent(
                argName
            ) { ArrayList() }
            argsMap[argName]?.add(paramValue)
            argNamePos += 2
            argValPos += 2
        }
        for ((key, valueList) in argsMap.entries) {
            val paramBuilder = FulfillmentParam.newBuilder().setName(key)
            for (value in valueList) {
                fulfillmentParams.add(
                    paramBuilder.addFulfillmentValues(
                        FulfillmentValue.newBuilder().setValue(value).build()
                    ).build()
                )
            }
        }
        return fulfillmentParams.toList()
    }
}
