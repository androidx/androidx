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
import androidx.appactions.interaction.capabilities.core.AbstractTaskHandlerBuilder.ConfirmationType;
import androidx.appactions.interaction.capabilities.core.task.OnDialogFinishListener;
import androidx.appactions.interaction.capabilities.core.task.OnInitListener;
import androidx.appactions.interaction.capabilities.core.task.impl.AbstractTaskUpdater;
import androidx.appactions.interaction.capabilities.core.task.impl.OnReadyToConfirmListenerInternal;
import androidx.appactions.interaction.capabilities.core.task.impl.TaskParamRegistry;
import androidx.appactions.interaction.proto.ParamValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Temporary holder for Task related data.
 *
 * @param <ArgumentT>
 * @param <OutputT>
 * @param <ConfirmationT>
 * @param <TaskUpdaterT>
 */
public final class TaskHandler<
        ArgumentT, OutputT, ConfirmationT, TaskUpdaterT extends AbstractTaskUpdater> {

    private final ConfirmationType mConfirmationType;
    private final TaskParamRegistry mParamsRegistry;
    private final Map<String, Function<OutputT, List<ParamValue>>> mExecutionOutputBindings;
    private final Map<String, Function<ConfirmationT, List<ParamValue>>> mConfirmationDataBindings;
    private final Optional<OnInitListener<TaskUpdaterT>> mOnInitListener;
    private final Optional<OnReadyToConfirmListenerInternal<ConfirmationT>>
            mOnReadyToConfirmListener;
    private final OnDialogFinishListener<ArgumentT, OutputT> mOnFinishListener;
    private final Supplier<TaskUpdaterT> mTaskUpdaterSupplier;

    TaskHandler(
            @NonNull ConfirmationType confirmationType,
            @NonNull TaskParamRegistry paramsRegistry,
            @NonNull Optional<OnInitListener<TaskUpdaterT>> onInitListener,
            @NonNull Optional<OnReadyToConfirmListenerInternal<ConfirmationT>> onReadyToConfirmListener,
            @NonNull OnDialogFinishListener<ArgumentT, OutputT> onFinishListener,
            @NonNull Map<String, Function<ConfirmationT, List<ParamValue>>> confirmationDataBindings,
            @NonNull Map<String, Function<OutputT, List<ParamValue>>> executionOutputBindings,
            @NonNull Supplier<TaskUpdaterT> taskUpdaterSupplier) {
        this.mConfirmationType = confirmationType;
        this.mParamsRegistry = paramsRegistry;
        this.mOnInitListener = onInitListener;
        this.mOnReadyToConfirmListener = onReadyToConfirmListener;
        this.mOnFinishListener = onFinishListener;
        this.mConfirmationDataBindings = confirmationDataBindings;
        this.mExecutionOutputBindings = executionOutputBindings;
        this.mTaskUpdaterSupplier = taskUpdaterSupplier;
    }

    ConfirmationType getConfirmationType() {
        return mConfirmationType;
    }

    TaskParamRegistry getParamsRegistry() {
        return mParamsRegistry;
    }

    Map<String, Function<OutputT, List<ParamValue>>> getExecutionOutputBindings() {
        return mExecutionOutputBindings;
    }

    Map<String, Function<ConfirmationT, List<ParamValue>>> getConfirmationDataBindings() {
        return mConfirmationDataBindings;
    }

    Optional<OnInitListener<TaskUpdaterT>> getOnInitListener() {
        return mOnInitListener;
    }

    Optional<OnReadyToConfirmListenerInternal<ConfirmationT>> getOnReadyToConfirmListener() {
        return mOnReadyToConfirmListener;
    }

    OnDialogFinishListener<ArgumentT, OutputT> getOnFinishListener() {
        return mOnFinishListener;
    }

    Supplier<TaskUpdaterT> getTaskUpdaterSupplier() {
        return mTaskUpdaterSupplier;
    }
}
