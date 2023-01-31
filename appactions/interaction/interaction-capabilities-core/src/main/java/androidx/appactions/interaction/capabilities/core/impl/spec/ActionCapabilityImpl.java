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

package androidx.appactions.interaction.capabilities.core.impl.spec;

import static androidx.appactions.interaction.capabilities.core.impl.utils.ImmutableCollectors.toImmutableList;
import static androidx.appactions.interaction.capabilities.core.impl.utils.ImmutableCollectors.toImmutableMap;

import androidx.annotation.NonNull;
import androidx.appactions.interaction.capabilities.core.ActionCapability;
import androidx.appactions.interaction.capabilities.core.ActionExecutor;
import androidx.appactions.interaction.capabilities.core.ExecutionResult;
import androidx.appactions.interaction.capabilities.core.impl.ArgumentsWrapper;
import androidx.appactions.interaction.capabilities.core.impl.CallbackInternal;
import androidx.appactions.interaction.capabilities.core.impl.ErrorStatusInternal;
import androidx.appactions.interaction.capabilities.core.impl.concurrent.FutureCallback;
import androidx.appactions.interaction.capabilities.core.impl.concurrent.Futures;
import androidx.appactions.interaction.capabilities.core.impl.exceptions.StructConversionException;
import androidx.appactions.interaction.capabilities.core.impl.utils.CapabilityLogger;
import androidx.appactions.interaction.capabilities.core.impl.utils.LoggerInternal;
import androidx.appactions.interaction.proto.AppActionsContext.AppAction;
import androidx.appactions.interaction.proto.FulfillmentRequest.Fulfillment.FulfillmentValue;
import androidx.appactions.interaction.proto.FulfillmentResponse;
import androidx.appactions.interaction.proto.ParamValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The implementation of the {@link ActionCapability} interface.
 *
 * @param <PropertyT>
 * @param <ArgumentT>
 * @param <OutputT>
 */
public final class ActionCapabilityImpl<PropertyT, ArgumentT, OutputT> implements ActionCapability {

    private static final String LOG_TAG = "ActionCapability";
    private final ActionSpec<PropertyT, ArgumentT, OutputT> mActionSpec;
    private final Optional<String> mIdentifier;
    private final PropertyT mProperty;
    private final ActionExecutor<ArgumentT, OutputT> mActionExecutor;

    public ActionCapabilityImpl(
            @NonNull ActionSpec<PropertyT, ArgumentT, OutputT> actionSpec,
            @NonNull Optional<String> identifier,
            @NonNull PropertyT property,
            @NonNull ActionExecutor<ArgumentT, OutputT> actionExecutor) {
        this.mActionSpec = actionSpec;
        this.mIdentifier = identifier;
        this.mProperty = property;
        this.mActionExecutor = actionExecutor;
    }

    @NonNull
    @Override
    public Optional<String> getId() {
        return mIdentifier;
    }

    @NonNull
    @Override
    public AppAction getAppAction() {
        AppAction appAction = mActionSpec.convertPropertyToProto(mProperty);
        if (mIdentifier.isPresent()) {
            appAction = appAction.toBuilder().setIdentifier(mIdentifier.get()).build();
        }
        return appAction;
    }

    @Override
    public void execute(
            @NonNull ArgumentsWrapper argumentsWrapper, @NonNull CallbackInternal callback) {
        // Filter out the task parts of ArgumentsWrapper. Send the raw arguments for one-shot
        // capabilities.
        Map<String, List<ParamValue>> args =
                argumentsWrapper.paramValues().entrySet().stream()
                        .collect(
                                toImmutableMap(
                                        Map.Entry::getKey,
                                        e ->
                                                e.getValue().stream()
                                                        .filter(FulfillmentValue::hasValue)
                                                        .map(FulfillmentValue::getValue)
                                                        .collect(toImmutableList())));
        try {
            Futures.addCallback(
                    mActionExecutor.execute(mActionSpec.buildArgument(args)),
                    new FutureCallback<ExecutionResult<OutputT>>() {
                        @Override
                        public void onSuccess(ExecutionResult<OutputT> executionResult) {
                            callback.onSuccess(convertToFulfillmentResponse(executionResult));
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            callback.onError(ErrorStatusInternal.CANCELLED);
                        }
                    },
                    Runnable::run);
        } catch (StructConversionException e) {
            if (e.getMessage() != null) {
                LoggerInternal.log(CapabilityLogger.LogLevel.ERROR, LOG_TAG, e.getMessage());
            }
            callback.onError(ErrorStatusInternal.STRUCT_CONVERSION_FAILURE);
        }
    }

    /** Converts typed {@link ExecutionResult} to {@link FulfillmentResponse} proto. */
    FulfillmentResponse convertToFulfillmentResponse(ExecutionResult<OutputT> executionResult) {
        FulfillmentResponse.Builder fulfillmentResponseBuilder =
                FulfillmentResponse.newBuilder()
                        .setStartDictation(executionResult.getStartDictation());
        OutputT output = executionResult.getOutput();
        if (output != null && !(output instanceof Void)) {
            fulfillmentResponseBuilder.setExecutionOutput(mActionSpec.convertOutputToProto(output));
        }
        return fulfillmentResponseBuilder.build();
    }
}
