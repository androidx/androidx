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

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appactions.interaction.capabilities.core.BaseSession;
import androidx.appactions.interaction.capabilities.core.ConfirmationOutput;
import androidx.appactions.interaction.capabilities.core.ExecutionResult;
import androidx.appactions.interaction.capabilities.core.InitArg;
import androidx.appactions.interaction.capabilities.core.impl.ActionCapabilitySession;
import androidx.appactions.interaction.capabilities.core.impl.ArgumentsWrapper;
import androidx.appactions.interaction.capabilities.core.impl.CallbackInternal;
import androidx.appactions.interaction.capabilities.core.impl.ErrorStatusInternal;
import androidx.appactions.interaction.capabilities.core.impl.TouchEventCallback;
import androidx.appactions.interaction.capabilities.core.impl.concurrent.FutureCallback;
import androidx.appactions.interaction.capabilities.core.impl.concurrent.Futures;
import androidx.appactions.interaction.capabilities.core.impl.exceptions.StructConversionException;
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpec;
import androidx.appactions.interaction.capabilities.core.impl.utils.CapabilityLogger;
import androidx.appactions.interaction.capabilities.core.impl.utils.LoggerInternal;
import androidx.appactions.interaction.capabilities.core.task.impl.exceptions.MissingRequiredArgException;
import androidx.appactions.interaction.proto.AppActionsContext.AppAction;
import androidx.appactions.interaction.proto.AppActionsContext.IntentParameter;
import androidx.appactions.interaction.proto.CurrentValue;
import androidx.appactions.interaction.proto.CurrentValue.Status;
import androidx.appactions.interaction.proto.FulfillmentRequest.Fulfillment;
import androidx.appactions.interaction.proto.FulfillmentRequest.Fulfillment.FulfillmentValue;
import androidx.appactions.interaction.proto.FulfillmentResponse;
import androidx.appactions.interaction.proto.FulfillmentResponse.StructuredOutput;
import androidx.appactions.interaction.proto.ParamValue;
import androidx.appactions.interaction.proto.TaskInfo;
import androidx.appactions.interaction.proto.TouchEventMetadata;
import androidx.concurrent.futures.CallbackToFutureAdapter;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Function;

/**
 * TaskOrchestrator is responsible for holding session state, and processing assistant / manual
 * input updates to update session state.
 *
 * <p>TaskOrchestrator is also responsible to communicating state updates to developer provided
 * listeners.
 *
 * <p>Only one request can be processed at a time.
 */
final class TaskOrchestrator<ArgumentT, OutputT, ConfirmationT> {

    private static final String LOG_TAG = "TaskOrchestrator";
    private final ActionSpec<?, ArgumentT, OutputT> mActionSpec;
    private final AppAction mAppAction;
    private final TaskHandler<ConfirmationT> mTaskHandler;
    private final Executor mExecutor;
    private final BaseSession<ArgumentT, OutputT> mExternalSession;

    /**
     * Map of argument name to the {@link CurrentValue} which wraps the argument name and status .
     */
    private final Map<String, List<CurrentValue>> mCurrentValuesMap;
    /**
     * Internal lock to enable synchronization while processing update requests. Also used for
     * synchronization of Task orchestrator state. ie indicate whether it is idle or not
     */
    private final Object mTaskOrchestratorLock = new Object();
    /**
     * The callback that should be invoked when manual input processing finishes. This sends the
     * processing results to the AppInteraction SDKs. Note, this field is not provided on
     * construction because the callback is not available at the time when the developer creates the
     * capability.
     */
    @Nullable TouchEventCallback mTouchEventCallback;
    /** Current status of the overall task (i.e. status of the task). */
    private ActionCapabilitySession.Status mTaskStatus;

    /** True if an UpdateRequest is currently being processed, false otherwise. */
    @GuardedBy("mTaskOrchestratorLock")
    private boolean mIsIdle = true;

    TaskOrchestrator(
            ActionSpec<?, ArgumentT, OutputT> actionSpec,
            AppAction appAction,
            TaskHandler<ConfirmationT> taskHandler,
            BaseSession<ArgumentT, OutputT> externalSession,
            Executor executor) {
        this.mActionSpec = actionSpec;
        this.mAppAction = appAction;
        this.mTaskHandler = taskHandler;
        this.mExternalSession = externalSession;
        this.mExecutor = executor;

        this.mCurrentValuesMap = Collections.synchronizedMap(new HashMap<>());
        this.mTaskStatus = ActionCapabilitySession.Status.UNINITIATED;
    }
    // Set a TouchEventCallback instance. This callback is invoked when state changes from manual
    // input.
    void setTouchEventCallback(@Nullable TouchEventCallback touchEventCallback) {
        this.mTouchEventCallback = touchEventCallback;
    }

    /** Returns whether or not a request is currently being processed */
    boolean isIdle() {
        synchronized (mTaskOrchestratorLock) {
            return mIsIdle;
        }
    }

    ActionCapabilitySession.Status getStatus() {
        return mTaskStatus;
    }

    /**
     * processes the provided UpdateRequest asynchronously.
     *
     * <p>Returns a {@code ListenableFuture<Void>} that is completed when the request handling is
     * completed.
     *
     * <p>An unhandled exception when handling an UpdateRequest will cause all future update
     * requests to fail.
     *
     * <p>This method should never be called when isIdle() returns false.
     */
    ListenableFuture<Void> processUpdateRequest(UpdateRequest updateRequest) {
        synchronized (mTaskOrchestratorLock) {
            if (!mIsIdle) {
                throw new IllegalStateException(
                        "processUpdateRequest should never be called when isIdle is false.");
            }
            mIsIdle = false;
            ListenableFuture<Void> requestProcessingFuture;
            switch (updateRequest.getKind()) {
                case ASSISTANT:
                    requestProcessingFuture =
                            processAssistantUpdateRequest(updateRequest.assistant());
                    break;
                case TOUCH_EVENT:
                    requestProcessingFuture =
                            processTouchEventUpdateRequest(updateRequest.touchEvent());
                    break;
                default:
                    throw new IllegalArgumentException("unknown UpdateRequest type");
            }
            return Futures.transform(
                    requestProcessingFuture,
                    unused -> {
                        synchronized (mTaskOrchestratorLock) {
                            mIsIdle = true;
                            return null;
                        }
                    },
                    mExecutor,
                    "set isIdle");
        }
    }

    /** Processes an assistant update request. */
    private ListenableFuture<Void> processAssistantUpdateRequest(
            AssistantUpdateRequest assistantUpdateRequest) {
        ArgumentsWrapper argumentsWrapper = assistantUpdateRequest.argumentsWrapper();
        CallbackInternal callback = assistantUpdateRequest.callbackInternal();

        if (!argumentsWrapper.requestMetadata().isPresent()) {
            callback.onError(ErrorStatusInternal.INVALID_REQUEST_TYPE);
            return Futures.immediateVoidFuture();
        }
        Fulfillment.Type requestType = argumentsWrapper.requestMetadata().get().requestType();
        switch (requestType) {
            case UNRECOGNIZED:
            case UNKNOWN_TYPE:
                callback.onError(ErrorStatusInternal.INVALID_REQUEST_TYPE);
                break;
            case SYNC:
                return handleSync(argumentsWrapper, callback);
            case CONFIRM:
                return handleConfirm(callback);
            case CANCEL:
            case TERMINATE:
                terminate();
                callback.onSuccess(FulfillmentResponse.getDefaultInstance());
                break;
        }
        return Futures.immediateVoidFuture();
    }

    public ListenableFuture<Void> processTouchEventUpdateRequest(
            TouchEventUpdateRequest touchEventUpdateRequest) {
        Map<String, List<ParamValue>> paramValuesMap = touchEventUpdateRequest.getParamValuesMap();
        if (mTouchEventCallback == null
                || paramValuesMap.isEmpty()
                || mTaskStatus != ActionCapabilitySession.Status.IN_PROGRESS) {
            return Futures.immediateVoidFuture();
        }
        for (Map.Entry<String, List<ParamValue>> entry : paramValuesMap.entrySet()) {
            String argName = entry.getKey();
            mCurrentValuesMap.put(
                    argName,
                    entry.getValue().stream()
                            .map(
                                    paramValue ->
                                            TaskCapabilityUtils.toCurrentValue(
                                                    paramValue, Status.ACCEPTED))
                            .collect(toImmutableList()));
        }
        ListenableFuture<Void> argumentsProcessingFuture;
        if (anyParamsOfStatus(Status.DISAMBIG)) {
            argumentsProcessingFuture = Futures.immediateVoidFuture();
        } else {
            Map<String, List<FulfillmentValue>> fulfillmentValuesMap =
                    TaskCapabilityUtils.paramValuesMapToFulfillmentValuesMap(
                            getCurrentPendingArguments());
            argumentsProcessingFuture = processFulfillmentValues(fulfillmentValuesMap);
        }

        ListenableFuture<FulfillmentResponse> fulfillmentResponseFuture =
                Futures.transformAsync(
                        argumentsProcessingFuture,
                        (unused) -> maybeConfirmOrFinish(),
                        mExecutor,
                        "maybeConfirmOrFinish");

        return invokeTouchEventCallback(fulfillmentResponseFuture);
    }

    private ListenableFuture<Void> invokeTouchEventCallback(
            ListenableFuture<FulfillmentResponse> fulfillmentResponseFuture) {
        return CallbackToFutureAdapter.getFuture(
                completer -> {
                    Futures.addCallback(
                            fulfillmentResponseFuture,
                            new FutureCallback<FulfillmentResponse>() {
                                @Override
                                public void onSuccess(FulfillmentResponse fulfillmentResponse) {
                                    LoggerInternal.log(
                                            CapabilityLogger.LogLevel.INFO,
                                            LOG_TAG,
                                            "Manual input success");
                                    if (mTouchEventCallback != null) {
                                        mTouchEventCallback.onSuccess(
                                                fulfillmentResponse,
                                                TouchEventMetadata.getDefaultInstance());
                                    } else {
                                        LoggerInternal.log(
                                                CapabilityLogger.LogLevel.ERROR,
                                                LOG_TAG,
                                                "Manual input null callback");
                                    }
                                    completer.set(null);
                                }

                                @Override
                                public void onFailure(@NonNull Throwable t) {
                                    LoggerInternal.log(
                                            CapabilityLogger.LogLevel.ERROR,
                                            LOG_TAG,
                                            "Manual input fail");
                                    if (mTouchEventCallback != null) {
                                        mTouchEventCallback.onError(
                                                ErrorStatusInternal.TOUCH_EVENT_REQUEST_FAILURE);
                                    } else {
                                        LoggerInternal.log(
                                                CapabilityLogger.LogLevel.ERROR,
                                                LOG_TAG,
                                                "Manual input null callback");
                                    }
                                    completer.set(null);
                                }
                            },
                            mExecutor);
                    return "handle fulfillmentResponseFuture for manual input";
                });
    }

    // TODO: add cleanup logic if any
    private void terminate() {
        this.mTaskStatus = ActionCapabilitySession.Status.DESTROYED;
    }

    /**
     * If slot filling is incomplete, the future contains default FulfillmentResponse.
     *
     * <p>Otherwise, the future contains a FulfillmentResponse containing BIC or BIO data.
     */
    private ListenableFuture<FulfillmentResponse> maybeConfirmOrFinish() {
        Map<String, List<ParamValue>> finalArguments = getCurrentAcceptedArguments();
        if (anyParamsOfStatus(Status.REJECTED)
                || !TaskCapabilityUtils.isSlotFillingComplete(
                        finalArguments, mAppAction.getParamsList())) {
            return Futures.immediateFuture(FulfillmentResponse.getDefaultInstance());
        }
        if (mTaskHandler.getOnReadyToConfirmListener() != null) {
            return getFulfillmentResponseForConfirmation(finalArguments);
        }
        return getFulfillmentResponseForExecution(finalArguments);
    }

    private ListenableFuture<Void> maybeInitializeTask() {
        if (this.mTaskStatus == ActionCapabilitySession.Status.UNINITIATED) {
            mExternalSession.onInit(new InitArg());
        }
        this.mTaskStatus = ActionCapabilitySession.Status.IN_PROGRESS;
        return Futures.immediateVoidFuture();
    }

    /**
     * Handles a SYNC request from assistant.
     *
     * <p>Control-flow logic for a single task turn. Note, a task may start and finish in the same
     * turn, so the logic should include onEnter, arg validation, and onExit.
     */
    private ListenableFuture<Void> handleSync(
            ArgumentsWrapper argumentsWrapper, CallbackInternal callback) {
        ListenableFuture<Void> onInitFuture = maybeInitializeTask();

        clearMissingArgs(argumentsWrapper);
        ListenableFuture<Void> argResolutionFuture =
                Futures.transformAsync(
                        onInitFuture,
                        unused -> processFulfillmentValues(argumentsWrapper.paramValues()),
                        mExecutor,
                        "processFulfillmentValues");

        ListenableFuture<FulfillmentResponse> fulfillmentResponseFuture =
                Futures.transformAsync(
                        argResolutionFuture,
                        unused -> maybeConfirmOrFinish(),
                        mExecutor,
                        "maybeConfirmOrFinish");
        return CallbackToFutureAdapter.getFuture(
                completer -> {
                    Futures.addCallback(
                            fulfillmentResponseFuture,
                            new FutureCallback<FulfillmentResponse>() {
                                @Override
                                public void onSuccess(FulfillmentResponse fulfillmentResponse) {
                                    LoggerInternal.log(
                                            CapabilityLogger.LogLevel.INFO,
                                            LOG_TAG,
                                            "Task sync success");
                                    callback.onSuccess(fulfillmentResponse);
                                    completer.set(null);
                                }

                                @Override
                                public void onFailure(@NonNull Throwable t) {
                                    LoggerInternal.log(
                                            CapabilityLogger.LogLevel.ERROR,
                                            LOG_TAG,
                                            "Task sync fail",
                                            t);
                                    callback.onError(ErrorStatusInternal.SYNC_REQUEST_FAILURE);
                                    completer.set(null);
                                }
                            },
                            mExecutor);
                    return "handle fulfillmentResponseFuture for SYNC";
                });
    }

    /**
     * Control-flow logic for a single task turn in which the user has confirmed in the previous
     * turn.
     */
    private ListenableFuture<Void> handleConfirm(CallbackInternal callback) {
        Map<String, List<ParamValue>> finalArguments = getCurrentAcceptedArguments();
        return CallbackToFutureAdapter.getFuture(
                completer -> {
                    Futures.addCallback(
                            getFulfillmentResponseForExecution(finalArguments),
                            new FutureCallback<FulfillmentResponse>() {
                                @Override
                                public void onSuccess(FulfillmentResponse fulfillmentResponse) {
                                    LoggerInternal.log(
                                            CapabilityLogger.LogLevel.INFO,
                                            LOG_TAG,
                                            "Task confirm success");
                                    callback.onSuccess(fulfillmentResponse);
                                    completer.set(null);
                                }

                                @Override
                                public void onFailure(@NonNull Throwable t) {
                                    LoggerInternal.log(
                                            CapabilityLogger.LogLevel.ERROR,
                                            LOG_TAG,
                                            "Task confirm fail");
                                    callback.onError(
                                            ErrorStatusInternal.CONFIRMATION_REQUEST_FAILURE);
                                    completer.set(null);
                                }
                            },
                            mExecutor);
                    return "handle fulfillmentResponseFuture for CONFIRM";
                });
    }

    private void clearMissingArgs(ArgumentsWrapper assistantArgs) {
        Set<String> argsCleared =
                mCurrentValuesMap.keySet().stream()
                        .filter(argName -> !assistantArgs.paramValues().containsKey(argName))
                        .collect(toImmutableSet());
        for (String arg : argsCleared) {
            mCurrentValuesMap.remove(arg);
            // TODO(b/234170829): notify listener#onReceived of the cleared arguments
        }
    }

    /**
     * Main processing chain for both assistant requests and manual input requests. All pending
     * parameters contained in fulfillmentValuesMap are chained together in a serial fassion. We use
     * Futures here to make sure long running app processing (such as argument grounding or argument
     * validation) are executed asynchronously.
     */
    private ListenableFuture<Void> processFulfillmentValues(
            Map<String, List<FulfillmentValue>> fulfillmentValuesMap) {
        ListenableFuture<SlotProcessingResult> currentFuture =
                Futures.immediateFuture(SlotProcessingResult.create(true, Collections.emptyList()));
        for (Map.Entry<String, List<FulfillmentValue>> entry : fulfillmentValuesMap.entrySet()) {
            String name = entry.getKey();
            List<FulfillmentValue> fulfillmentValues = entry.getValue();
            currentFuture =
                    Futures.transformAsync(
                            currentFuture,
                            (previousResult) ->
                                    maybeProcessSlotAndUpdateCurrentValues(
                                            previousResult, name, fulfillmentValues),
                            mExecutor,
                            "maybeProcessSlotAndUpdateCurrentValues");
        }
        // Transform the final Boolean future to a void one.
        return Futures.transform(currentFuture, (unused) -> null, mExecutor, "return null");
    }

    private ListenableFuture<SlotProcessingResult> maybeProcessSlotAndUpdateCurrentValues(
            SlotProcessingResult previousResult,
            String slotKey,
            List<FulfillmentValue> newSlotValues) {
        List<CurrentValue> currentSlotValues =
                mCurrentValuesMap.getOrDefault(slotKey, Collections.emptyList());
        List<FulfillmentValue> modifiedSlotValues =
                TaskCapabilityUtils.getMaybeModifiedSlotValues(currentSlotValues, newSlotValues);
        if (TaskCapabilityUtils.canSkipSlotProcessing(currentSlotValues, modifiedSlotValues)) {
            return Futures.immediateFuture(previousResult);
        }
        List<CurrentValue> pendingArgs =
                TaskCapabilityUtils.fulfillmentValuesToCurrentValues(
                        modifiedSlotValues, Status.PENDING);
        return Futures.transform(
                processSlot(slotKey, previousResult, pendingArgs),
                currentResult -> {
                    mCurrentValuesMap.put(slotKey, currentResult.processedValues());
                    return currentResult;
                },
                mExecutor,
                "update currentValuesMap");
    }

    /**
     * Process pending param values for a slot.
     *
     * <p>If the previous slot was accepted, go through grounding/validation with TaskSlotProcessor,
     * otherwise just return the pending values as is.
     */
    private ListenableFuture<SlotProcessingResult> processSlot(
            String name, SlotProcessingResult previousResult, List<CurrentValue> pendingArgs) {
        if (!previousResult.isSuccessful()) {
            return Futures.immediateFuture(SlotProcessingResult.create(false, pendingArgs));
        }
        return TaskSlotProcessor.processSlot(
                name, pendingArgs, mTaskHandler.getTaskParamRegistry(), mExecutor);
    }

    /**
     * Retrieve all ParamValue from accepted slots in currentValuesMap.
     *
     * <p>A slot is considered accepted if all CurrentValues in the slot has ACCEPTED status.
     */
    private Map<String, List<ParamValue>> getCurrentAcceptedArguments() {
        return mCurrentValuesMap.entrySet().stream()
                .filter(
                        entry ->
                                entry.getValue().stream()
                                        .allMatch(
                                                currentValue ->
                                                        currentValue.getStatus()
                                                                == Status.ACCEPTED))
                .collect(
                        toImmutableMap(
                                Map.Entry::getKey,
                                entry ->
                                        entry.getValue().stream()
                                                .map(CurrentValue::getValue)
                                                .collect(toImmutableList())));
    }

    /**
     * Retrieve all ParamValue from pending slots in currentValuesMap.
     *
     * <p>A slot is considered pending if any CurrentValues in the slot has PENDING status.
     */
    private Map<String, List<ParamValue>> getCurrentPendingArguments() {
        return mCurrentValuesMap.entrySet().stream()
                .filter(
                        entry ->
                                entry.getValue().stream()
                                        .anyMatch(
                                                currentValue ->
                                                        currentValue.getStatus() == Status.PENDING))
                .collect(
                        toImmutableMap(
                                Map.Entry::getKey,
                                entry ->
                                        entry.getValue().stream()
                                                .map(CurrentValue::getValue)
                                                .collect(toImmutableList())));
    }

    /** Returns true if any CurrentValue in currentValuesMap has the given Status. */
    private boolean anyParamsOfStatus(Status status) {
        return mCurrentValuesMap.entrySet().stream()
                .anyMatch(
                        entry ->
                                entry.getValue().stream()
                                        .anyMatch(
                                                currentValue ->
                                                        currentValue.getStatus() == status));
    }

    private ListenableFuture<ConfirmationOutput<ConfirmationT>> executeOnTaskReadyToConfirm(
            Map<String, List<ParamValue>> finalArguments) {
        try {
            return Objects.requireNonNull(mTaskHandler.getOnReadyToConfirmListener())
                    .onReadyToConfirm(finalArguments);
        } catch (StructConversionException | MissingRequiredArgException e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    private ListenableFuture<ExecutionResult<OutputT>> executeOnTaskFinish(
            Map<String, List<ParamValue>> finalArguments) {
        ListenableFuture<ExecutionResult<OutputT>> executionResultFuture;
        try {
            executionResultFuture =
                    mExternalSession.onFinishAsync(mActionSpec.buildArgument(finalArguments));
        } catch (StructConversionException e) {
            return Futures.immediateFailedFuture(e);
        }
        return Futures.transform(
                executionResultFuture,
                executionResult -> {
                    this.mTaskStatus = ActionCapabilitySession.Status.COMPLETED;
                    return executionResult;
                },
                mExecutor,
                "set taskStatus to COMPLETED");
    }

    private ListenableFuture<FulfillmentResponse> getFulfillmentResponseForConfirmation(
            Map<String, List<ParamValue>> finalArguments) {
        return Futures.transform(
                executeOnTaskReadyToConfirm(finalArguments),
                result -> {
                    FulfillmentResponse.Builder fulfillmentResponse =
                            FulfillmentResponse.newBuilder();
                    convertToConfirmationOutput(result)
                            .ifPresent(fulfillmentResponse::setConfirmationData);
                    return fulfillmentResponse.build();
                },
                mExecutor,
                "create FulfillmentResponse from ConfirmationOutput");
    }

    private ListenableFuture<FulfillmentResponse> getFulfillmentResponseForExecution(
            Map<String, List<ParamValue>> finalArguments) {
        return Futures.transform(
                executeOnTaskFinish(finalArguments),
                result -> {
                    FulfillmentResponse.Builder fulfillmentResponse =
                            FulfillmentResponse.newBuilder();
                    if (mTaskStatus == ActionCapabilitySession.Status.COMPLETED) {
                        convertToExecutionOutput(result)
                                .ifPresent(fulfillmentResponse::setExecutionOutput);
                    }
                    return fulfillmentResponse.build();
                },
                mExecutor,
                "create FulfillmentResponse from ExecutionResult");
    }

    private List<IntentParameter> addStateToParamsContext(List<IntentParameter> params) {
        List<IntentParameter> updatedList = new ArrayList<>();
        params.stream()
                .forEach(
                        param -> {
                            List<CurrentValue> vals = mCurrentValuesMap.get(param.getName());
                            if (vals != null) {
                                updatedList.add(
                                        param.toBuilder()
                                                .clearCurrentValue()
                                                .addAllCurrentValue(vals)
                                                .build());
                            } else {
                                updatedList.add(param);
                            }
                        });
        return updatedList;
    }

    AppAction getAppAction() {
        return mAppAction.toBuilder()
                .clearParams()
                .addAllParams(addStateToParamsContext(mAppAction.getParamsList()))
                .setTaskInfo(TaskInfo.newBuilder().setSupportsPartialFulfillment(true))
                .build();
    }

    /** Convert from java capabilities {@link ExecutionResult} to {@link StructuredOutput} proto. */
    private Optional<StructuredOutput> convertToExecutionOutput(
            ExecutionResult<OutputT> executionResult) {
        OutputT output = executionResult.getOutput();
        if (output == null) {
            return Optional.empty();
        }

        return Optional.of(mActionSpec.convertOutputToProto(output));
    }

    /**
     * Convert from java capabilities {@link ConfirmationOutput} to {@link StructuredOutput} proto.
     */
    private Optional<StructuredOutput> convertToConfirmationOutput(
            ConfirmationOutput<ConfirmationT> confirmationOutput) {
        ConfirmationT confirmation = confirmationOutput.getConfirmation();
        if (confirmation == null || confirmation instanceof Void) {
            return Optional.empty();
        }

        StructuredOutput.Builder confirmationOutputBuilder = StructuredOutput.newBuilder();
        for (Map.Entry<String, Function<ConfirmationT, List<ParamValue>>> entry :
                mTaskHandler.getConfirmationDataBindings().entrySet()) {
            confirmationOutputBuilder.addOutputValues(
                    StructuredOutput.OutputValue.newBuilder()
                            .setName(entry.getKey())
                            .addAllValues(entry.getValue().apply(confirmation))
                            .build());
        }
        return Optional.of(confirmationOutputBuilder.build());
    }
}
