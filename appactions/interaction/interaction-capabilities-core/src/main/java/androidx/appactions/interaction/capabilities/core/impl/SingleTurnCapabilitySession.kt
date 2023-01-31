
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

package androidx.appactions.interaction.capabilities.core.impl

import androidx.annotation.NonNull
import androidx.appactions.interaction.capabilities.core.ExecutionResult
import androidx.appactions.interaction.capabilities.core.BaseSession
import androidx.appactions.interaction.capabilities.core.impl.concurrent.FutureCallback
import androidx.appactions.interaction.capabilities.core.impl.concurrent.Futures
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpec
import androidx.appactions.interaction.proto.FulfillmentRequest.Fulfillment.FulfillmentValue
import androidx.appactions.interaction.proto.FulfillmentResponse
import androidx.appactions.interaction.proto.ParamValue

import androidx.annotation.RestrictTo

/**
 * ActionCapabilitySession implementation for executing single-turn fulfillment requests.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class SingleTurnCapabilitySession<
    ArgumentT,
    OutputT,
>(
    val actionSpec: ActionSpec<*, ArgumentT, OutputT>,
    val externalSession: BaseSession<ArgumentT, OutputT>,
) : ActionCapabilitySession {
    override fun execute(
        @NonNull argumentsWrapper: ArgumentsWrapper,
        @NonNull callback: CallbackInternal,
    ) {
        val paramValuesMap: Map<String, List<ParamValue>> = argumentsWrapper.paramValues().entries
            .associate {
                    entry: Map.Entry<String, List<FulfillmentValue>> ->
                Pair(
                    entry.key,
                    entry.value.mapNotNull {
                            fulfillmentValue: FulfillmentValue ->
                        fulfillmentValue.getValue()
                    },
                )
            }
        val argument = actionSpec.buildArgument(paramValuesMap)
        Futures.addCallback(
            externalSession.onFinishAsync(argument),
            object : FutureCallback<ExecutionResult<OutputT>> {
                override fun onSuccess(executionResult: ExecutionResult<OutputT>) {
                    callback.onSuccess(convertToFulfillmentResponse(executionResult))
                }

                override fun onFailure(t: Throwable) {
                    callback.onError(ErrorStatusInternal.CANCELLED)
                }
            },
            Runnable::run,
        )
    }

    /** Converts typed {@link ExecutionResult} to {@link FulfillmentResponse} proto. */
    private fun convertToFulfillmentResponse(
        executionResult: ExecutionResult<OutputT>,
    ): FulfillmentResponse {
        val fulfillmentResponseBuilder =
            FulfillmentResponse.newBuilder()
                .setStartDictation(executionResult.startDictation)
        executionResult.output?.let { it ->
            fulfillmentResponseBuilder.setExecutionOutput(
                actionSpec.convertOutputToProto(it),
            )
        }
        return fulfillmentResponseBuilder.build()
    }
}