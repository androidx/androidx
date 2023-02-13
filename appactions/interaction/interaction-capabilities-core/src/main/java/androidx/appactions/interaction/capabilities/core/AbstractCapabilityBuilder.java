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

package androidx.appactions.interaction.capabilities.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appactions.interaction.capabilities.core.impl.SingleTurnCapabilityImpl;
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpec;
import androidx.appactions.interaction.capabilities.core.task.impl.AbstractTaskUpdater;
import androidx.appactions.interaction.capabilities.core.task.impl.TaskCapabilityImpl;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Objects;

/**
 * An abstract Builder class for ActionCapability.
 *
 * @param <BuilderT>
 * @param <PropertyT>
 * @param <ArgumentT>
 * @param <OutputT>
 * @param <ConfirmationT>
 * @param <TaskUpdaterT>
 */
public abstract class AbstractCapabilityBuilder<
        BuilderT extends
                AbstractCapabilityBuilder<
                                BuilderT,
                                PropertyT,
                                ArgumentT,
                                OutputT,
                                ConfirmationT,
                                TaskUpdaterT>,
        PropertyT,
        ArgumentT,
        OutputT,
        ConfirmationT,
        TaskUpdaterT extends AbstractTaskUpdater> {

    private final ActionSpec<PropertyT, ArgumentT, OutputT> mActionSpec;
    @Nullable private String mId;
    @Nullable private PropertyT mProperty;
    @Nullable private ActionExecutor<ArgumentT, OutputT> mActionExecutor;
    @Nullable private TaskHandler<ArgumentT, OutputT, ConfirmationT, TaskUpdaterT> mTaskHandler;

    /**
     * @param actionSpec
     */
    protected AbstractCapabilityBuilder(
            @NonNull ActionSpec<PropertyT, ArgumentT, OutputT> actionSpec) {
        this.mActionSpec = actionSpec;
    }

    @SuppressWarnings("unchecked") // cast to child class
    private BuilderT asBuilder() {
        return (BuilderT) this;
    }

    /**
     * Sets the Id of the capability being built. The Id should be a non-null string that is unique
     * among all ActionCapability, and should not change during/across activity lifecycles.
     */
    @NonNull
    public final BuilderT setId(@NonNull String id) {
        this.mId = id;
        return asBuilder();
    }

    /**
     * Sets the Property instance for this capability. Must be called before {@link
     * AbstractCapabilityBuilder.build}.
     */
    protected final BuilderT setProperty(@NonNull PropertyT property) {
        this.mProperty = property;
        return asBuilder();
    }

    /**
     * Sets the TaskHandler for this capability. The individual capability factory classes can
     * decide to expose their own public {@code setTaskHandler} method and invoke this parent
     * method. Setting the TaskHandler should build a capability instance that supports multi-turn
     * tasks.
     */
    protected final BuilderT setTaskHandler(
            @NonNull TaskHandler<ArgumentT, OutputT, ConfirmationT, TaskUpdaterT> taskHandler) {
        this.mTaskHandler = taskHandler;
        return asBuilder();
    }

    /** Sets the ActionExecutor for this capability. */
    @NonNull
    public final BuilderT setActionExecutor(
            @NonNull ActionExecutor<ArgumentT, OutputT> actionExecutor) {
        this.mActionExecutor = actionExecutor;
        return asBuilder();
    }

    /** Builds and returns this ActionCapability. */
    @NonNull
    public ActionCapability build() {
        Objects.requireNonNull(mProperty, "property must not be null.");
        if (mTaskHandler == null) {
            Objects.requireNonNull(mActionExecutor, "actionExecutor must not be null.");
            return new SingleTurnCapabilityImpl<PropertyT, ArgumentT, OutputT>(
                    mId,
                    mActionSpec,
                    mProperty,
                    (hostProperties)->new BaseSession<ArgumentT, OutputT>() {
                        @Override
                        public ListenableFuture<ExecutionResult<OutputT>> onFinishAsync(
                                ArgumentT argument) {
                            return mActionExecutor.execute(argument);
                        }
                    });
        }
        TaskCapabilityImpl<PropertyT, ArgumentT, OutputT, ConfirmationT, TaskUpdaterT>
                taskCapability =
                        new TaskCapabilityImpl<>(
                                Objects.requireNonNull(mId, "id field must not be null."),
                                mActionSpec,
                                mProperty,
                                mTaskHandler.getParamsRegistry(),
                                mTaskHandler.getOnInitListener(),
                                mTaskHandler.getOnReadyToConfirmListener(),
                                mTaskHandler.getOnFinishListener(),
                                mTaskHandler.getConfirmationDataBindings(),
                                mTaskHandler.getExecutionOutputBindings(),
                                Runnable::run);
        taskCapability.setTaskUpdaterSupplier(mTaskHandler.getTaskUpdaterSupplier());
        return taskCapability;
    }
}
