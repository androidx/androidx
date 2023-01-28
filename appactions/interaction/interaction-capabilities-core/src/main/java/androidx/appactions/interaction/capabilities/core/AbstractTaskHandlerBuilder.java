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

import static androidx.appactions.interaction.capabilities.core.impl.utils.ImmutableCollectors.toImmutableList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appactions.interaction.capabilities.core.impl.converters.DisambigEntityConverter;
import androidx.appactions.interaction.capabilities.core.impl.converters.ParamValueConverter;
import androidx.appactions.interaction.capabilities.core.impl.converters.SearchActionConverter;
import androidx.appactions.interaction.capabilities.core.task.AppEntityListResolver;
import androidx.appactions.interaction.capabilities.core.task.AppEntityResolver;
import androidx.appactions.interaction.capabilities.core.task.InvalidTaskException;
import androidx.appactions.interaction.capabilities.core.task.InventoryListResolver;
import androidx.appactions.interaction.capabilities.core.task.InventoryResolver;
import androidx.appactions.interaction.capabilities.core.task.OnDialogFinishListener;
import androidx.appactions.interaction.capabilities.core.task.OnInitListener;
import androidx.appactions.interaction.capabilities.core.task.ValueListListener;
import androidx.appactions.interaction.capabilities.core.task.ValueListener;
import androidx.appactions.interaction.capabilities.core.task.impl.AbstractTaskUpdater;
import androidx.appactions.interaction.capabilities.core.task.impl.GenericResolverInternal;
import androidx.appactions.interaction.capabilities.core.task.impl.OnReadyToConfirmListenerInternal;
import androidx.appactions.interaction.capabilities.core.task.impl.TaskParamRegistry;
import androidx.appactions.interaction.proto.ParamValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * An abstract Builder class for an ActionCapability that supports task.
 *
 * @param <BuilderT>
 * @param <ArgumentT>
 * @param <OutputT>
 * @param <ConfirmationT>
 * @param <TaskUpdaterT>
 */
public abstract class AbstractTaskHandlerBuilder<
        BuilderT extends
                AbstractTaskHandlerBuilder<BuilderT, ArgumentT, OutputT, ConfirmationT,
                        TaskUpdaterT>,
        ArgumentT,
        OutputT,
        ConfirmationT,
        TaskUpdaterT extends AbstractTaskUpdater> {

    private final ConfirmationType mConfirmationType;
    private final TaskParamRegistry.Builder mParamsRegistry;
    private final Map<String, Function<OutputT, List<ParamValue>>> mExecutionOutputBindings =
            new HashMap<>();
    private final Map<String, Function<ConfirmationT, List<ParamValue>>> mConfirmationDataBindings =
            new HashMap<>();
    @Nullable
    private OnInitListener<TaskUpdaterT> mOnInitListener;
    @Nullable
    private OnReadyToConfirmListenerInternal<ConfirmationT> mOnReadyToConfirmListener;
    @Nullable
    private OnDialogFinishListener<ArgumentT, OutputT> mOnFinishListener;

    protected AbstractTaskHandlerBuilder() {
        this(ConfirmationType.NOT_SUPPORTED);
    }

    protected AbstractTaskHandlerBuilder(@NonNull ConfirmationType confirmationType) {
        this.mConfirmationType = confirmationType;
        this.mParamsRegistry = TaskParamRegistry.builder();
    }

    @SuppressWarnings("unchecked") // cast to child class
    protected BuilderT asBuilder() {
        return (BuilderT) this;
    }

    /** Sets the OnInitListener for this capability. */
    public final BuilderT setOnInitListener(@NonNull OnInitListener<TaskUpdaterT> onInitListener) {
        this.mOnInitListener = onInitListener;
        return asBuilder();
    }

    /** Sets the onReadyToConfirmListener for this capability. */
    protected final BuilderT setOnReadyToConfirmListenerInternal(
            @NonNull OnReadyToConfirmListenerInternal<ConfirmationT> onReadyToConfirm) {
        this.mOnReadyToConfirmListener = onReadyToConfirm;
        return asBuilder();
    }

    /** Sets the onFinishListener for this capability. */
    public final BuilderT setOnFinishListener(
            @NonNull OnDialogFinishListener<ArgumentT, OutputT> onFinishListener) {
        this.mOnFinishListener = onFinishListener;
        return asBuilder();
    }

    protected <ValueTypeT> void registerInventoryTaskParam(
            @NonNull String paramName,
            @NonNull InventoryResolver<ValueTypeT> listener,
            @NonNull ParamValueConverter<ValueTypeT> converter) {
        mParamsRegistry.addTaskParameter(
                paramName,
                (paramValue) -> !paramValue.hasIdentifier(),
                GenericResolverInternal.fromInventoryResolver(listener),
                Optional.empty(),
                Optional.empty(),
                converter);
    }

    protected <ValueTypeT> void registerInventoryListTaskParam(
            @NonNull String paramName,
            @NonNull InventoryListResolver<ValueTypeT> listener,
            @NonNull ParamValueConverter<ValueTypeT> converter) {
        mParamsRegistry.addTaskParameter(
                paramName,
                (paramValue) -> !paramValue.hasIdentifier(),
                GenericResolverInternal.fromInventoryListResolver(listener),
                Optional.empty(),
                Optional.empty(),
                converter);
    }

    protected <ValueTypeT> void registerAppEntityTaskParam(
            @NonNull String paramName,
            @NonNull AppEntityResolver<ValueTypeT> listener,
            @NonNull ParamValueConverter<ValueTypeT> converter,
            @NonNull DisambigEntityConverter<ValueTypeT> entityConverter,
            @NonNull SearchActionConverter<ValueTypeT> searchActionConverter) {
        mParamsRegistry.addTaskParameter(
                paramName,
                (paramValue) -> !paramValue.hasIdentifier(),
                GenericResolverInternal.fromAppEntityResolver(listener),
                Optional.of(entityConverter),
                Optional.of(searchActionConverter),
                converter);
    }

    protected <ValueTypeT> void registerAppEntityListTaskParam(
            @NonNull String paramName,
            @NonNull AppEntityListResolver<ValueTypeT> listener,
            @NonNull ParamValueConverter<ValueTypeT> converter,
            @NonNull DisambigEntityConverter<ValueTypeT> entityConverter,
            @NonNull SearchActionConverter<ValueTypeT> searchActionConverter) {
        mParamsRegistry.addTaskParameter(
                paramName,
                (paramValue) -> !paramValue.hasIdentifier(),
                GenericResolverInternal.fromAppEntityListResolver(listener),
                Optional.of(entityConverter),
                Optional.of(searchActionConverter),
                converter);
    }

    protected <ValueTypeT> void registerValueTaskParam(
            @NonNull String paramName,
            @NonNull ValueListener<ValueTypeT> listener,
            @NonNull ParamValueConverter<ValueTypeT> converter) {
        mParamsRegistry.addTaskParameter(
                paramName,
                unused -> false,
                GenericResolverInternal.fromValueListener(listener),
                Optional.empty(),
                Optional.empty(),
                converter);
    }

    protected <ValueTypeT> void registerValueListTaskParam(
            @NonNull String paramName,
            @NonNull ValueListListener<ValueTypeT> listener,
            @NonNull ParamValueConverter<ValueTypeT> converter) {
        mParamsRegistry.addTaskParameter(
                paramName,
                unused -> false,
                GenericResolverInternal.fromValueListListener(listener),
                Optional.empty(),
                Optional.empty(),
                converter);
    }

    /**
     * Registers an optional execution output.
     *
     * @param paramName    the BII output slot name of this parameter.
     * @param outputGetter a getter of the output from the {@code OutputT} instance.
     * @param converter    a converter from an output object to a ParamValue.
     */
    protected <T> void registerExecutionOutput(
            @NonNull String paramName,
            @NonNull Function<OutputT, Optional<T>> outputGetter,
            @NonNull Function<T, ParamValue> converter) {
        mExecutionOutputBindings.put(
                paramName,
                output -> outputGetter.apply(output).stream().map(converter).collect(
                        toImmutableList()));
    }

    /**
     * Registers a repeated execution output.
     *
     * @param paramName    the BII output slot name of this parameter.
     * @param outputGetter a getter of the output from the {@code OutputT} instance.
     * @param converter    a converter from an output object to a ParamValue.
     */
    protected <T> void registerRepeatedExecutionOutput(
            @NonNull String paramName,
            @NonNull Function<OutputT, List<T>> outputGetter,
            @NonNull Function<T, ParamValue> converter) {
        mExecutionOutputBindings.put(
                paramName,
                output -> outputGetter.apply(output).stream().map(converter).collect(
                        toImmutableList()));
    }

    /**
     * Registers an optional confirmation data.
     *
     * @param paramName          the BIC confirmation data slot name of this parameter.
     * @param confirmationGetter a getter of the confirmation data from the {@code ConfirmationT}
     *                           instance.
     * @param converter          a converter from confirmation data to a ParamValue.
     */
    protected <T> void registerConfirmationOutput(
            @NonNull String paramName,
            @NonNull Function<ConfirmationT, Optional<T>> confirmationGetter,
            @NonNull Function<T, ParamValue> converter) {
        mConfirmationDataBindings.put(
                paramName,
                output ->
                        confirmationGetter.apply(output).stream().map(converter).collect(
                                toImmutableList()));
    }

    /** Specific capability builders override this to support BII-specific TaskUpdaters. */
    @NonNull
    protected abstract Supplier<TaskUpdaterT> getTaskUpdaterSupplier();

    /**
     * Build a TaskHandler.
     */
    @NonNull
    public TaskHandler<ArgumentT, OutputT, ConfirmationT, TaskUpdaterT> build() {
        if (this.mConfirmationType == ConfirmationType.REQUIRED
                && mOnReadyToConfirmListener == null) {
            throw new InvalidTaskException(
                    "ConfirmationType is REQUIRED, but onReadyToConfirmListener is not set.");
        }
        if (this.mConfirmationType == ConfirmationType.NOT_SUPPORTED
                && mOnReadyToConfirmListener != null) {
            throw new InvalidTaskException(
                    "ConfirmationType is NOT_SUPPORTED, but onReadyToConfirmListener is set.");
        }
        return new TaskHandler<>(
                mConfirmationType,
                mParamsRegistry.build(),
                Optional.ofNullable(mOnInitListener),
                Optional.ofNullable(mOnReadyToConfirmListener),
                Objects.requireNonNull(mOnFinishListener, "onTaskFinishListener must not be null."),
                mConfirmationDataBindings,
                mExecutionOutputBindings,
                getTaskUpdaterSupplier());
    }

    /** Confirmation types for a Capability. */
    protected enum ConfirmationType {
        // Confirmation is not supported for this Capability.
        NOT_SUPPORTED,
        // This Capability requires confirmation.
        REQUIRED,
        // Confirmation is optional for this Capability.
        OPTIONAL
    }
}
