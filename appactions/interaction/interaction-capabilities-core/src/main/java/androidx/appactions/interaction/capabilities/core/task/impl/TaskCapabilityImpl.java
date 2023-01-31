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

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appactions.interaction.capabilities.core.ActionCapability;
import androidx.appactions.interaction.capabilities.core.impl.ArgumentsWrapper;
import androidx.appactions.interaction.capabilities.core.impl.CallbackInternal;
import androidx.appactions.interaction.capabilities.core.impl.ErrorStatusInternal;
import androidx.appactions.interaction.capabilities.core.impl.TouchEventCallback;
import androidx.appactions.interaction.capabilities.core.impl.concurrent.FutureCallback;
import androidx.appactions.interaction.capabilities.core.impl.concurrent.Futures;
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpec;
import androidx.appactions.interaction.capabilities.core.task.OnDialogFinishListener;
import androidx.appactions.interaction.capabilities.core.task.OnInitListener;
import androidx.appactions.interaction.proto.AppActionsContext.AppAction;
import androidx.appactions.interaction.proto.ParamValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Stateful horizontal task orchestrator to manage business logic for the task.
 *
 * @param <PropertyT>
 * @param <ArgumentT>
 * @param <OutputT>
 * @param <ConfirmationT>
 * @param <TaskUpdaterT>
 */
public final class TaskCapabilityImpl<
                PropertyT,
                ArgumentT,
                OutputT,
                ConfirmationT,
                TaskUpdaterT extends AbstractTaskUpdater>
        implements ActionCapability, TaskUpdateHandler {

    private final String mIdentifier;
    private final Executor mExecutor;
    private final TaskOrchestrator<PropertyT, ArgumentT, OutputT, ConfirmationT, TaskUpdaterT>
            mTaskOrchestrator;

    private final Object mAssistantUpdateLock = new Object();

    @GuardedBy("mAssistantUpdateLock")
    @Nullable
    private AssistantUpdateRequest mPendingAssistantRequest = null;

    @GuardedBy("mAssistantUpdateLock")
    @Nullable
    private TouchEventUpdateRequest mPendingTouchEventRequest = null;

    public TaskCapabilityImpl(
            @NonNull String identifier,
            @NonNull ActionSpec<PropertyT, ArgumentT, OutputT> actionSpec,
            PropertyT property,
            @NonNull TaskParamRegistry paramRegistry,
            @NonNull Optional<OnInitListener<TaskUpdaterT>> onInitListener,
            @NonNull
                    Optional<OnReadyToConfirmListenerInternal<ConfirmationT>>
                            onReadyToConfirmListener,
            @NonNull OnDialogFinishListener<ArgumentT, OutputT> onFinishListener,
            @NonNull
                    Map<String, Function<ConfirmationT, List<ParamValue>>>
                            confirmationOutputBindings,
            @NonNull Map<String, Function<OutputT, List<ParamValue>>> executionOutputBindings,
            @NonNull Executor executor) {
        this.mIdentifier = identifier;
        this.mExecutor = executor;
        this.mTaskOrchestrator =
                new TaskOrchestrator<>(
                        identifier,
                        actionSpec,
                        property,
                        paramRegistry,
                        onInitListener,
                        onReadyToConfirmListener,
                        onFinishListener,
                        confirmationOutputBindings,
                        executionOutputBindings,
                        executor);
    }

    @NonNull
    @Override
    public Optional<String> getId() {
        return Optional.of(mIdentifier);
    }

    public void setTaskUpdaterSupplier(@NonNull Supplier<TaskUpdaterT> taskUpdaterSupplier) {
        this.mTaskOrchestrator.setTaskUpdaterSupplier(
                () -> {
                    TaskUpdaterT taskUpdater = taskUpdaterSupplier.get();
                    taskUpdater.init(this);
                    return taskUpdater;
                });
    }

    @Override
    public void setTouchEventCallback(@NonNull TouchEventCallback touchEventCallback) {
        mTaskOrchestrator.setTouchEventCallback(touchEventCallback);
    }

    @NonNull
    @Override
    public AppAction getAppAction() {
        return this.mTaskOrchestrator.getAppAction();
    }

    /**
     * If there is a pendingAssistantRequest, we will overwrite that request (and send CANCELLED
     * response to that request).
     *
     * <p>This is done because assistant requests contain the full state, so we can safely ignore
     * existing requests if a new one arrives.
     */
    private void enqueueAssistantRequest(@NonNull AssistantUpdateRequest request) {
        synchronized (mAssistantUpdateLock) {
            if (mPendingAssistantRequest != null) {
                mPendingAssistantRequest.callbackInternal().onError(ErrorStatusInternal.CANCELLED);
            }
            mPendingAssistantRequest = request;
            dispatchPendingRequestIfIdle();
        }
    }

    private void enqueueTouchEventRequest(@NonNull TouchEventUpdateRequest request) {
        synchronized (mAssistantUpdateLock) {
            if (mPendingTouchEventRequest == null) {
                mPendingTouchEventRequest = request;
            } else {
                mPendingTouchEventRequest =
                        TouchEventUpdateRequest.merge(mPendingTouchEventRequest, request);
            }
            dispatchPendingRequestIfIdle();
        }
    }

    /**
     * If taskOrchestrator is idle, select the next request to dispatch to taskOrchestrator (if
     * there are any pending requests).
     *
     * <p>If taskOrchestrator is not idle, do nothing, since this method will automatically be
     * called when the current request finishes.
     */
    void dispatchPendingRequestIfIdle() {
        synchronized (mAssistantUpdateLock) {
            if (!mTaskOrchestrator.isIdle()) {
                return;
            }
            UpdateRequest nextRequest = null;
            if (mPendingAssistantRequest != null) {
                nextRequest = UpdateRequest.of(mPendingAssistantRequest);
                mPendingAssistantRequest = null;
            } else if (mPendingTouchEventRequest != null) {
                nextRequest = UpdateRequest.of(mPendingTouchEventRequest);
                mPendingTouchEventRequest = null;
            }
            if (nextRequest != null) {
                Futures.addCallback(
                        mTaskOrchestrator.processUpdateRequest(nextRequest),
                        new FutureCallback<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                dispatchPendingRequestIfIdle();
                            }

                            /**
                             * A fatal exception has occurred, cause by one of the following:
                             *
                             * <ul>
                             *   <li>1. The developer listener threw some runtime exception
                             *   <li>2. The SDK encountered some uncaught internal exception
                             * </ul>
                             *
                             * <p>In both cases, this exception will be rethrown which will crash
                             * the app.
                             */
                            @Override
                            public void onFailure(@NonNull Throwable t) {
                                throw new IllegalStateException(
                                        "unhandled exception in request processing", t);
                            }
                        },
                        mExecutor);
            }
        }
    }

    @Override
    public void execute(
            @NonNull ArgumentsWrapper argumentsWrapper, @NonNull CallbackInternal callback) {
        enqueueAssistantRequest(AssistantUpdateRequest.create(argumentsWrapper, callback));
    }

    /** Method for attempting to manually update the param values. */
    @Override
    public void updateParamValues(@NonNull Map<String, List<ParamValue>> paramValuesMap) {
        enqueueTouchEventRequest(TouchEventUpdateRequest.create(paramValuesMap));
    }
}
