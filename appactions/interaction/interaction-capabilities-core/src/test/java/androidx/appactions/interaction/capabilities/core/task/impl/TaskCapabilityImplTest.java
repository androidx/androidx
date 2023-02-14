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

import static androidx.appactions.interaction.capabilities.core.testing.ArgumentUtils.buildRequestArgs;
import static androidx.appactions.interaction.capabilities.core.testing.ArgumentUtils.buildSearchActionParamValue;
import static androidx.appactions.interaction.capabilities.core.testing.TestingUtils.CB_TIMEOUT;
import static androidx.appactions.interaction.capabilities.core.testing.TestingUtils.buildActionCallback;
import static androidx.appactions.interaction.capabilities.core.testing.TestingUtils.buildActionCallbackWithFulfillmentResponse;
import static androidx.appactions.interaction.capabilities.core.testing.TestingUtils.buildErrorActionCallback;
import static androidx.appactions.interaction.capabilities.core.testing.TestingUtils.buildTouchEventCallback;
import static androidx.appactions.interaction.proto.FulfillmentRequest.Fulfillment.Type.CANCEL;
import static androidx.appactions.interaction.proto.FulfillmentRequest.Fulfillment.Type.CONFIRM;
import static androidx.appactions.interaction.proto.FulfillmentRequest.Fulfillment.Type.SYNC;
import static androidx.appactions.interaction.proto.FulfillmentRequest.Fulfillment.Type.TERMINATE;
import static androidx.appactions.interaction.proto.FulfillmentRequest.Fulfillment.Type.UNKNOWN_TYPE;
import static androidx.appactions.interaction.proto.FulfillmentRequest.Fulfillment.Type.UNRECOGNIZED;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import androidx.annotation.NonNull;
import androidx.appactions.interaction.capabilities.core.AbstractCapabilityBuilder;
import androidx.appactions.interaction.capabilities.core.AbstractTaskHandlerBuilder;
import androidx.appactions.interaction.capabilities.core.ActionCapability;
import androidx.appactions.interaction.capabilities.core.ConfirmationOutput;
import androidx.appactions.interaction.capabilities.core.ExecutionResult;
import androidx.appactions.interaction.capabilities.core.impl.ErrorStatusInternal;
import androidx.appactions.interaction.capabilities.core.impl.TouchEventCallback;
import androidx.appactions.interaction.capabilities.core.impl.concurrent.Futures;
import androidx.appactions.interaction.capabilities.core.impl.converters.DisambigEntityConverter;
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters;
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpec;
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecBuilder;
import androidx.appactions.interaction.capabilities.core.properties.EntityProperty;
import androidx.appactions.interaction.capabilities.core.properties.EnumProperty;
import androidx.appactions.interaction.capabilities.core.properties.SimpleProperty;
import androidx.appactions.interaction.capabilities.core.properties.StringProperty;
import androidx.appactions.interaction.capabilities.core.task.AppEntityResolver;
import androidx.appactions.interaction.capabilities.core.task.EntitySearchResult;
import androidx.appactions.interaction.capabilities.core.task.InvalidTaskException;
import androidx.appactions.interaction.capabilities.core.task.OnDialogFinishListener;
import androidx.appactions.interaction.capabilities.core.task.OnInitListener;
import androidx.appactions.interaction.capabilities.core.task.OnReadyToConfirmListener;
import androidx.appactions.interaction.capabilities.core.task.ValidationResult;
import androidx.appactions.interaction.capabilities.core.task.ValueListener;
import androidx.appactions.interaction.capabilities.core.testing.TestingUtils;
import androidx.appactions.interaction.capabilities.core.testing.TestingUtils.ReusableTouchEventCallback;
import androidx.appactions.interaction.capabilities.core.testing.TestingUtils.TouchEventResult;
import androidx.appactions.interaction.capabilities.core.testing.spec.Argument;
import androidx.appactions.interaction.capabilities.core.testing.spec.CapabilityStructFill;
import androidx.appactions.interaction.capabilities.core.testing.spec.CapabilityTwoEntityValues;
import androidx.appactions.interaction.capabilities.core.testing.spec.CapabilityTwoStrings;
import androidx.appactions.interaction.capabilities.core.testing.spec.Confirmation;
import androidx.appactions.interaction.capabilities.core.testing.spec.Output;
import androidx.appactions.interaction.capabilities.core.testing.spec.Property;
import androidx.appactions.interaction.capabilities.core.testing.spec.SettableFutureWrapper;
import androidx.appactions.interaction.capabilities.core.testing.spec.TestEnum;
import androidx.appactions.interaction.capabilities.core.values.EntityValue;
import androidx.appactions.interaction.capabilities.core.values.ListItem;
import androidx.appactions.interaction.capabilities.core.values.SearchAction;
import androidx.appactions.interaction.proto.AppActionsContext.AppAction;
import androidx.appactions.interaction.proto.AppActionsContext.IntentParameter;
import androidx.appactions.interaction.proto.CurrentValue;
import androidx.appactions.interaction.proto.DisambiguationData;
import androidx.appactions.interaction.proto.Entity;
import androidx.appactions.interaction.proto.FulfillmentResponse;
import androidx.appactions.interaction.proto.FulfillmentResponse.StructuredOutput;
import androidx.appactions.interaction.proto.FulfillmentResponse.StructuredOutput.OutputValue;
import androidx.appactions.interaction.proto.ParamValue;
import androidx.appactions.interaction.proto.TaskInfo;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.concurrent.futures.CallbackToFutureAdapter.Completer;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@RunWith(JUnit4.class)
public final class TaskCapabilityImplTest {

    private static final Optional<DisambigEntityConverter<EntityValue>> DISAMBIG_ENTITY_CONVERTER =
            Optional.of(TypeConverters::toEntity);
    private static final GenericResolverInternal<EntityValue> AUTO_ACCEPT_ENTITY_VALUE =
            GenericResolverInternal.fromAppEntityResolver(
                    new AppEntityResolver<EntityValue>() {
                        @Override
                        public ListenableFuture<EntitySearchResult<EntityValue>> lookupAndRender(
                                SearchAction<EntityValue> searchAction) {
                            EntitySearchResult.Builder<EntityValue> result =
                                    EntitySearchResult.newBuilder();
                            return Futures.immediateFuture(
                                    result.addPossibleValue(EntityValue.ofId("valid1")).build());
                        }

                        @NonNull
                        @Override
                        public ListenableFuture<ValidationResult> onReceived(EntityValue newValue) {
                            return Futures.immediateFuture(ValidationResult.newAccepted());
                        }
                    });
    private static final GenericResolverInternal<EntityValue> AUTO_REJECT_ENTITY_VALUE =
            GenericResolverInternal.fromAppEntityResolver(
                    new AppEntityResolver<EntityValue>() {
                        @Override
                        public ListenableFuture<EntitySearchResult<EntityValue>> lookupAndRender(
                                SearchAction<EntityValue> searchAction) {
                            EntitySearchResult.Builder<EntityValue> result =
                                    EntitySearchResult.newBuilder();
                            return Futures.immediateFuture(
                                    result.addPossibleValue(EntityValue.ofId("valid1")).build());
                        }

                        @NonNull
                        @Override
                        public ListenableFuture<ValidationResult> onReceived(EntityValue newValue) {
                            return Futures.immediateFuture(ValidationResult.newRejected());
                        }
                    });
    private static final String CAPABILITY_NAME = "actions.intent.TEST";
    private static final ActionSpec<Property, Argument, Output> ACTION_SPEC =
            ActionSpecBuilder.ofCapabilityNamed(CAPABILITY_NAME)
                    .setDescriptor(Property.class)
                    .setArgument(Argument.class, Argument::newBuilder)
                    .setOutput(Output.class)
                    .bindRequiredEntityParameter(
                            "required",
                            Property::requiredEntityField,
                            Argument.Builder::setRequiredEntityField)
                    .bindOptionalStringParameter(
                            "optional",
                            Property::optionalStringField,
                            Argument.Builder::setOptionalStringField)
                    .bindOptionalEnumParameter(
                            "optionalEnum",
                            TestEnum.class,
                            Property::enumField,
                            Argument.Builder::setEnumField)
                    .bindRepeatedStringParameter(
                            "repeated",
                            Property::repeatedStringField,
                            Argument.Builder::setRepeatedStringField)
                    .build();
    private static final Property SINGLE_REQUIRED_FIELD_PROPERTY =
            Property.newBuilder()
                    .setRequiredEntityField(EntityProperty.newBuilder().setIsRequired(true).build())
                    .build();
    private static final Optional<OnReadyToConfirmListener<Argument, Confirmation>>
            EMPTY_CONFIRM_LISTENER = Optional.empty();
    private static final OnDialogFinishListener<Argument, Output> EMPTY_FINISH_LISTENER =
            (finalArgs) ->
                    Futures.immediateFuture(ExecutionResult.<Output>getDefaultInstance());

    private static boolean groundingPredicate(ParamValue paramValue) {
        return !paramValue.hasIdentifier();
    }

    private static List<CurrentValue> getCurrentValues(String argName, AppAction appAction) {
        return appAction.getParamsList().stream()
                .filter(intentParam -> intentParam.getName().equals(argName))
                .findFirst()
                .orElse(IntentParameter.getDefaultInstance())
                .getCurrentValueList();
    }

    private static <TaskUpdaterT extends AbstractTaskUpdater>
            TaskCapabilityImpl<Property, Argument, Output, Confirmation, TaskUpdaterT>
                    createTaskCapability(
                            Property property,
                            TaskParamRegistry paramRegistry,
                            Supplier<TaskUpdaterT> taskUpdaterSupplier,
                            Optional<OnInitListener<TaskUpdaterT>> onInitListener,
                            Optional<OnReadyToConfirmListener<Argument, Confirmation>>
                                    optionalOnReadyToConfirmListener,
                            OnDialogFinishListener<Argument, Output> onTaskFinishListener) {

        Optional<OnReadyToConfirmListenerInternal<Confirmation>> onReadyToConfirmListenerInternal =
                optionalOnReadyToConfirmListener.isPresent()
                        ? Optional.of(
                                (args) ->
                                        optionalOnReadyToConfirmListener
                                                .get()
                                                .onReadyToConfirm(ACTION_SPEC.buildArgument(args)))
                        : Optional.empty();

        TaskCapabilityImpl<Property, Argument, Output, Confirmation, TaskUpdaterT> taskCapability =
                new TaskCapabilityImpl<>(
                        "id",
                        ACTION_SPEC,
                        property,
                        paramRegistry,
                        onInitListener,
                        onReadyToConfirmListenerInternal,
                        onTaskFinishListener,
                        /* confirmationOutputBindings= */ new HashMap<>(),
                        /* executionOutputBindings= */ new HashMap<>(),
                        Executors.newFixedThreadPool(5));
        taskCapability.setTaskUpdaterSupplier(taskUpdaterSupplier);
        return taskCapability;
    }

    @Test
    public void getAppAction_executeNeverCalled_taskIsUninitialized() {
        ActionCapability capability =
                createTaskCapability(
                        SINGLE_REQUIRED_FIELD_PROPERTY,
                        TaskParamRegistry.builder().build(),
                        RequiredTaskUpdater::new,
                        Optional.empty(),
                        EMPTY_CONFIRM_LISTENER,
                        EMPTY_FINISH_LISTENER);

        assertThat(capability.getAppAction())
                .isEqualTo(
                        AppAction.newBuilder()
                                .setName("actions.intent.TEST")
                                .setIdentifier("id")
                                .addParams(
                                        IntentParameter.newBuilder()
                                                .setName("required")
                                                .setIsRequired(true))
                                .setTaskInfo(
                                        TaskInfo.newBuilder().setSupportsPartialFulfillment(true))
                                .build());
    }

    @Test
    public void onInitInvoked_invokedOnce() throws Exception {
        AtomicInteger onSuccessInvocationCount = new AtomicInteger(0);
        ActionCapability capability =
                createTaskCapability(
                        SINGLE_REQUIRED_FIELD_PROPERTY,
                        TaskParamRegistry.builder().build(),
                        RequiredTaskUpdater::new,
                        Optional.of(
                                (unused) -> {
                                    onSuccessInvocationCount.incrementAndGet();
                                    return Futures.immediateVoidFuture();
                                }),
                        EMPTY_CONFIRM_LISTENER,
                        EMPTY_FINISH_LISTENER);

        // TURN 1.
        SettableFutureWrapper<Boolean> onSuccessInvoked = new SettableFutureWrapper<>();

        capability.execute(
                buildRequestArgs(SYNC, "unknownArgName", "foo"),
                buildActionCallback(onSuccessInvoked));

        assertThat(onSuccessInvoked.getFuture().get(CB_TIMEOUT, MILLISECONDS)).isTrue();
        assertThat(onSuccessInvocationCount.get()).isEqualTo(1);

        // TURN 2.
        SettableFutureWrapper<Boolean> onSuccessInvoked2 = new SettableFutureWrapper<>();

        capability.execute(
                buildRequestArgs(
                        SYNC,
                        "required",
                        ParamValue.newBuilder().setIdentifier("foo").setStringValue("foo").build()),
                buildActionCallback(onSuccessInvoked2));

        assertThat(onSuccessInvoked2.getFuture().get(CB_TIMEOUT, MILLISECONDS)).isTrue();
        assertThat(onSuccessInvocationCount.get()).isEqualTo(1);
    }

    @Test
    public void fulfillmentType_terminate_taskStateCleared() throws Exception {
        ActionCapability capability =
                createTaskCapability(
                        SINGLE_REQUIRED_FIELD_PROPERTY,
                        TaskParamRegistry.builder().build(),
                        RequiredTaskUpdater::new,
                        Optional.empty(),
                        EMPTY_CONFIRM_LISTENER,
                        EMPTY_FINISH_LISTENER);

        // TURN 1.
        SettableFutureWrapper<Boolean> onSuccessInvoked = new SettableFutureWrapper<>();

        capability.execute(
                buildRequestArgs(
                        SYNC,
                        "required",
                        ParamValue.newBuilder().setIdentifier("foo").setStringValue("foo").build()),
                buildActionCallback(onSuccessInvoked));

        assertThat(onSuccessInvoked.getFuture().get(CB_TIMEOUT, MILLISECONDS)).isTrue();

        // TURN 2.
        SettableFutureWrapper<Boolean> onSuccessInvoked2 = new SettableFutureWrapper<>();

        capability.execute(buildRequestArgs(TERMINATE), buildActionCallback(onSuccessInvoked2));

        assertThat(onSuccessInvoked2.getFuture().get(CB_TIMEOUT, MILLISECONDS)).isTrue();
        assertThat(capability.getAppAction())
                .isEqualTo(
                        AppAction.newBuilder()
                                .setName("actions.intent.TEST")
                                .setIdentifier("id")
                                .addParams(
                                        IntentParameter.newBuilder()
                                                .setName("required")
                                                .setIsRequired(true))
                                .setTaskInfo(
                                        TaskInfo.newBuilder().setSupportsPartialFulfillment(true))
                                .build());
    }

    @Test
    public void fulfillmentType_cancel_taskStateCleared() throws Exception {
        SettableFutureWrapper<RequiredTaskUpdater> taskUpdaterCb = new SettableFutureWrapper<>();
        ActionCapability capability =
                createTaskCapability(
                        SINGLE_REQUIRED_FIELD_PROPERTY,
                        TaskParamRegistry.builder().build(),
                        RequiredTaskUpdater::new,
                        TestingUtils.buildOnInitListener(taskUpdaterCb),
                        EMPTY_CONFIRM_LISTENER,
                        EMPTY_FINISH_LISTENER);

        // TURN 1.
        SettableFutureWrapper<Boolean> onSuccessInvoked = new SettableFutureWrapper<>();

        capability.execute(buildRequestArgs(SYNC), buildActionCallback(onSuccessInvoked));

        assertThat(onSuccessInvoked.getFuture().get(CB_TIMEOUT, MILLISECONDS)).isTrue();
        assertThat(taskUpdaterCb.getFuture().get(CB_TIMEOUT, MILLISECONDS).isDestroyed()).isFalse();

        // TURN 2.
        SettableFutureWrapper<Boolean> onSuccessInvoked2 = new SettableFutureWrapper<>();

        capability.execute(buildRequestArgs(CANCEL), buildActionCallback(onSuccessInvoked2));

        assertThat(onSuccessInvoked2.getFuture().get(CB_TIMEOUT, MILLISECONDS)).isTrue();
        assertThat(capability.getAppAction())
                .isEqualTo(
                        AppAction.newBuilder()
                                .setName("actions.intent.TEST")
                                .setIdentifier("id")
                                .addParams(
                                        IntentParameter.newBuilder()
                                                .setName("required")
                                                .setIsRequired(true))
                                .setTaskInfo(
                                        TaskInfo.newBuilder().setSupportsPartialFulfillment(true))
                                .build());
        assertThat(taskUpdaterCb.getFuture().get(CB_TIMEOUT, MILLISECONDS).isDestroyed()).isTrue();
    }

    @Test
    public void fulfillmentType_unknown_errorReported() throws Exception {
        ActionCapability capability =
                createTaskCapability(
                        SINGLE_REQUIRED_FIELD_PROPERTY,
                        TaskParamRegistry.builder().build(),
                        RequiredTaskUpdater::new,
                        Optional.empty(),
                        EMPTY_CONFIRM_LISTENER,
                        EMPTY_FINISH_LISTENER);

        // TURN 1 (UNKNOWN).
        SettableFutureWrapper<ErrorStatusInternal> errorCb = new SettableFutureWrapper<>();

        capability.execute(buildRequestArgs(UNKNOWN_TYPE), buildErrorActionCallback(errorCb));

        assertThat(errorCb.getFuture().get(CB_TIMEOUT, MILLISECONDS))
                .isEqualTo(ErrorStatusInternal.INVALID_REQUEST_TYPE);
        assertThat(capability.getAppAction())
                .isEqualTo(
                        AppAction.newBuilder()
                                .setName("actions.intent.TEST")
                                .setIdentifier("id")
                                .addParams(
                                        IntentParameter.newBuilder()
                                                .setName("required")
                                                .setIsRequired(true))
                                .setTaskInfo(
                                        TaskInfo.newBuilder().setSupportsPartialFulfillment(true))
                                .build());

        // TURN 2 (UNRECOGNIZED).
        SettableFutureWrapper<ErrorStatusInternal> errorCb2 = new SettableFutureWrapper<>();

        capability.execute(buildRequestArgs(UNRECOGNIZED), buildErrorActionCallback(errorCb2));

        assertThat(errorCb2.getFuture().get(CB_TIMEOUT, MILLISECONDS))
                .isEqualTo(ErrorStatusInternal.INVALID_REQUEST_TYPE);
        assertThat(capability.getAppAction())
                .isEqualTo(
                        AppAction.newBuilder()
                                .setName("actions.intent.TEST")
                                .setIdentifier("id")
                                .addParams(
                                        IntentParameter.newBuilder()
                                                .setName("required")
                                                .setIsRequired(true))
                                .setTaskInfo(
                                        TaskInfo.newBuilder().setSupportsPartialFulfillment(true))
                                .build());
    }

    @Test
    public void slotFilling_optionalButRejectedParam_onFinishNotInvoked() throws Exception {
        AtomicInteger onFinishInvocationCount = new AtomicInteger(0);
        CapabilityTwoEntityValues.Property property =
                CapabilityTwoEntityValues.Property.newBuilder()
                        .setSlotA(EntityProperty.newBuilder().setIsRequired(true).build())
                        .setSlotB(EntityProperty.newBuilder().setIsRequired(false).build())
                        .build();
        TaskParamRegistry paramRegistry =
                TaskParamRegistry.builder()
                        .addTaskParameter(
                                "slotA",
                                TaskCapabilityImplTest::groundingPredicate,
                                AUTO_ACCEPT_ENTITY_VALUE,
                                DISAMBIG_ENTITY_CONVERTER,
                                Optional.of(
                                        unused -> SearchAction.<EntityValue>newBuilder().build()),
                                TypeConverters::toEntityValue)
                        .addTaskParameter(
                                "slotB",
                                TaskCapabilityImplTest::groundingPredicate,
                                AUTO_REJECT_ENTITY_VALUE,
                                DISAMBIG_ENTITY_CONVERTER,
                                Optional.of(
                                        unused -> SearchAction.<EntityValue>newBuilder().build()),
                                TypeConverters::toEntityValue)
                        .build();
        TaskCapabilityImpl<
                        CapabilityTwoEntityValues.Property,
                        CapabilityTwoEntityValues.Argument,
                        Void,
                        Void,
                        EmptyTaskUpdater>
                capability =
                        new TaskCapabilityImpl<>(
                                "fakeId",
                                CapabilityTwoEntityValues.ACTION_SPEC,
                                property,
                                paramRegistry,
                                /* onInitListener= */ Optional.empty(),
                                /* onReadyToConfirmListener= */ Optional.empty(),
                                (finalArgs) -> {
                                    onFinishInvocationCount.incrementAndGet();
                                    return Futures.immediateFuture(
                                            ExecutionResult.getDefaultInstance());
                                },
                                /* confirmationOutputBindings= */ Collections.emptyMap(),
                                /* executionOutputBindings= */ Collections.emptyMap(),
                                Runnable::run);
        capability.setTaskUpdaterSupplier(EmptyTaskUpdater::new);

        // TURN 1.
        SettableFutureWrapper<Boolean> onSuccessInvoked = new SettableFutureWrapper<>();
        capability.execute(
                buildRequestArgs(
                        SYNC,
                        "slotA",
                        ParamValue.newBuilder().setIdentifier("foo").setStringValue("foo").build(),
                        "slotB",
                        ParamValue.newBuilder().setIdentifier("bar").setStringValue("bar").build()),
                buildActionCallback(onSuccessInvoked));

        assertThat(onSuccessInvoked.getFuture().get(CB_TIMEOUT, MILLISECONDS)).isTrue();
        assertThat(onFinishInvocationCount.get()).isEqualTo(0);
        assertThat(getCurrentValues("slotA", capability.getAppAction()))
                .containsExactly(
                        CurrentValue.newBuilder()
                                .setValue(
                                        ParamValue.newBuilder()
                                                .setIdentifier("foo")
                                                .setStringValue("foo"))
                                .setStatus(CurrentValue.Status.ACCEPTED)
                                .build());
        assertThat(getCurrentValues("slotB", capability.getAppAction()))
                .containsExactly(
                        CurrentValue.newBuilder()
                                .setValue(
                                        ParamValue.newBuilder()
                                                .setIdentifier("bar")
                                                .setStringValue("bar"))
                                .setStatus(CurrentValue.Status.REJECTED)
                                .build());
    }

    @Test
    public void slotFilling_assistantRemovedParam_clearInSdkState() throws Exception {
        Property property =
                Property.newBuilder()
                        .setRequiredEntityField(
                                EntityProperty.newBuilder().setIsRequired(true).build())
                        .setEnumField(
                                EnumProperty.newBuilder(TestEnum.class)
                                        .addSupportedEnumValues(TestEnum.VALUE_1, TestEnum.VALUE_2)
                                        .setIsRequired(true)
                                        .build())
                        .build();
        ActionCapability capability =
                createTaskCapability(
                        property,
                        TaskParamRegistry.builder().build(),
                        RequiredTaskUpdater::new,
                        Optional.empty(),
                        EMPTY_CONFIRM_LISTENER,
                        EMPTY_FINISH_LISTENER);

        // TURN 1.
        SettableFutureWrapper<Boolean> onSuccessInvoked = new SettableFutureWrapper<>();

        capability.execute(
                buildRequestArgs(
                        SYNC,
                        "required",
                        ParamValue.newBuilder().setIdentifier("foo").setStringValue("foo").build()),
                buildActionCallback(onSuccessInvoked));

        assertThat(onSuccessInvoked.getFuture().get(CB_TIMEOUT, MILLISECONDS)).isTrue();
        assertThat(getCurrentValues("required", capability.getAppAction()))
                .containsExactly(
                        CurrentValue.newBuilder()
                                .setValue(
                                        ParamValue.newBuilder()
                                                .setIdentifier("foo")
                                                .setStringValue("foo"))
                                .setStatus(CurrentValue.Status.ACCEPTED)
                                .build());
        assertThat(getCurrentValues("optionalEnum", capability.getAppAction())).isEmpty();

        // TURN 2.
        SettableFutureWrapper<Boolean> onSuccessInvoked2 = new SettableFutureWrapper<>();

        capability.execute(
                buildRequestArgs(SYNC, "optionalEnum", TestEnum.VALUE_2),
                buildActionCallback(onSuccessInvoked2));

        assertThat(onSuccessInvoked2.getFuture().get(CB_TIMEOUT, MILLISECONDS)).isTrue();
        assertThat(getCurrentValues("required", capability.getAppAction())).isEmpty();
        assertThat(getCurrentValues("optionalEnum", capability.getAppAction()))
                .containsExactly(
                        CurrentValue.newBuilder()
                                .setValue(ParamValue.newBuilder().setIdentifier("VALUE_2"))
                                .setStatus(CurrentValue.Status.ACCEPTED)
                                .build());
    }

    @Test
    public void disambig_singleParam_disambigEntitiesInContext() throws Exception {
        TaskParamRegistry.Builder paramRegistry = TaskParamRegistry.builder();
        paramRegistry.addTaskParameter(
                "required",
                TaskCapabilityImplTest::groundingPredicate,
                GenericResolverInternal.fromAppEntityResolver(
                        new AppEntityResolver<EntityValue>() {
                            @Override
                            public ListenableFuture<EntitySearchResult<EntityValue>>
                                    lookupAndRender(SearchAction<EntityValue> searchAction) {
                                EntitySearchResult.Builder<EntityValue> result =
                                        EntitySearchResult.newBuilder();
                                return Futures.immediateFuture(
                                        result.addPossibleValue(EntityValue.ofId("valid1"))
                                                .addPossibleValue(EntityValue.ofId("valid2"))
                                                .build());
                            }

                            @NonNull
                            @Override
                            public ListenableFuture<ValidationResult> onReceived(
                                    EntityValue newValue) {
                                return Futures.immediateFuture(ValidationResult.newAccepted());
                            }
                        }),
                DISAMBIG_ENTITY_CONVERTER,
                Optional.of(unused -> SearchAction.<EntityValue>newBuilder().build()),
                TypeConverters::toEntityValue);
        ActionCapability capability =
                createTaskCapability(
                        SINGLE_REQUIRED_FIELD_PROPERTY,
                        paramRegistry.build(),
                        RequiredTaskUpdater::new,
                        Optional.empty(),
                        EMPTY_CONFIRM_LISTENER,
                        EMPTY_FINISH_LISTENER);

        // TURN 1.
        SettableFutureWrapper<Boolean> onSuccessInvoked = new SettableFutureWrapper<>();

        capability.execute(
                buildRequestArgs(SYNC, "required", buildSearchActionParamValue("invalid")),
                buildActionCallback(onSuccessInvoked));

        assertThat(onSuccessInvoked.getFuture().get(CB_TIMEOUT, MILLISECONDS)).isTrue();
        assertThat(capability.getAppAction())
                .isEqualTo(
                        AppAction.newBuilder()
                                .setName("actions.intent.TEST")
                                .setIdentifier("id")
                                .addParams(
                                        IntentParameter.newBuilder()
                                                .setName("required")
                                                .setIsRequired(true)
                                                .addCurrentValue(
                                                        CurrentValue.newBuilder()
                                                                .setValue(
                                                                        buildSearchActionParamValue(
                                                                                "invalid"))
                                                                .setStatus(
                                                                        CurrentValue.Status
                                                                                .DISAMBIG)
                                                                .setDisambiguationData(
                                                                        DisambiguationData
                                                                                .newBuilder()
                                                                                .addEntities(
                                                                                        Entity
                                                                                                .newBuilder()
                                                                                                .setIdentifier(
                                                                                                        "valid1")
                                                                                                .setName(
                                                                                                        "valid1"))
                                                                                .addEntities(
                                                                                        Entity
                                                                                                .newBuilder()
                                                                                                .setIdentifier(
                                                                                                        "valid2")
                                                                                                .setName(
                                                                                                        "valid2")))))
                                .setTaskInfo(
                                        TaskInfo.newBuilder().setSupportsPartialFulfillment(true))
                                .build());

        // TURN 2.
        SettableFutureWrapper<Boolean> turn2SuccessInvoked = new SettableFutureWrapper<>();

        capability.execute(
                buildRequestArgs(
                        SYNC,
                        "required",
                        ParamValue.newBuilder()
                                .setIdentifier("valid2")
                                .setStringValue("valid2")
                                .build()),
                buildActionCallback(turn2SuccessInvoked));

        assertThat(turn2SuccessInvoked.getFuture().get(CB_TIMEOUT, MILLISECONDS)).isTrue();
        assertThat(capability.getAppAction())
                .isEqualTo(
                        AppAction.newBuilder()
                                .setName("actions.intent.TEST")
                                .setIdentifier("id")
                                .addParams(
                                        IntentParameter.newBuilder()
                                                .setName("required")
                                                .setIsRequired(true)
                                                .addCurrentValue(
                                                        CurrentValue.newBuilder()
                                                                .setValue(
                                                                        ParamValue.newBuilder()
                                                                                .setIdentifier(
                                                                                        "valid2")
                                                                                .setStringValue(
                                                                                        "valid2"))
                                                                .setStatus(
                                                                        CurrentValue.Status
                                                                                .ACCEPTED)))
                                .setTaskInfo(
                                        TaskInfo.newBuilder().setSupportsPartialFulfillment(true))
                                .build());
    }

    /**
     * Assistant sends grounded objects as identifier only, but we need to mark the entire value
     * struct as accepted.
     */
    @Test
    public void identifierOnly_refillsStruct() throws Exception {
        ListItem item1 = ListItem.newBuilder().setName("red apple").setId("item1").build();
        ListItem item2 = ListItem.newBuilder().setName("green apple").setId("item2").build();
        SettableFutureWrapper<ListItem> onReceivedCb = new SettableFutureWrapper<>();
        CapabilityStructFill.Property property =
                CapabilityStructFill.Property.newBuilder()
                        .setItemList(SimpleProperty.REQUIRED)
                        .setAnyString(StringProperty.newBuilder().setIsRequired(true).build())
                        .build();
        TaskParamRegistry.Builder paramRegistryBuilder = TaskParamRegistry.builder();
        paramRegistryBuilder.addTaskParameter(
                "listItem",
                TaskCapabilityImplTest::groundingPredicate,
                GenericResolverInternal.fromAppEntityResolver(
                        new AppEntityResolver<ListItem>() {
                            @NonNull
                            @Override
                            public ListenableFuture<ValidationResult> onReceived(
                                    ListItem listItem) {
                                onReceivedCb.set(listItem);
                                return Futures.immediateFuture(ValidationResult.newAccepted());
                            }

                            @Override
                            public ListenableFuture<EntitySearchResult<ListItem>> lookupAndRender(
                                    SearchAction<ListItem> searchAction) {
                                return Futures.immediateFuture(
                                        EntitySearchResult.<ListItem>newBuilder()
                                                .addPossibleValue(item1)
                                                .addPossibleValue(item2)
                                                .build());
                            }
                        }),
                Optional.of((DisambigEntityConverter<ListItem>) TypeConverters::toEntity),
                Optional.of(unused -> SearchAction.<ListItem>newBuilder().build()),
                TypeConverters::toListItem);
        SettableFutureWrapper<ListItem> onFinishListItemCb = new SettableFutureWrapper<>();
        SettableFutureWrapper<String> onFinishStringCb = new SettableFutureWrapper<>();
        TaskCapabilityImpl<
                        CapabilityStructFill.Property,
                        CapabilityStructFill.Argument,
                        Void,
                        Void,
                        EmptyTaskUpdater>
                capability =
                        new TaskCapabilityImpl<>(
                                "selectListItem",
                                CapabilityStructFill.ACTION_SPEC,
                                property,
                                paramRegistryBuilder.build(),
                                /* onInitListener= */ Optional.empty(),
                                /* onReadyToConfirmListener= */ Optional.empty(),
                                (argument) -> {
                                    ListItem listItem = argument.listItem().orElse(null);
                                    String string = argument.anyString().orElse(null);
                                    onFinishListItemCb.set(listItem);
                                    onFinishStringCb.set(string);
                                    return Futures.immediateFuture(
                                            ExecutionResult.getDefaultInstance());
                                },
                                /* confirmationOutputBindings= */ Collections.emptyMap(),
                                /* executionOutputBindings= */ Collections.emptyMap(),
                                Runnable::run);
        capability.setTaskUpdaterSupplier(EmptyTaskUpdater::new);

        // first sync request
        SettableFutureWrapper<Boolean> firstTurnSuccess = new SettableFutureWrapper<>();
        capability.execute(
                buildRequestArgs(SYNC, "listItem", buildSearchActionParamValue("apple")),
                buildActionCallback(firstTurnSuccess));
        assertThat(firstTurnSuccess.getFuture().get(CB_TIMEOUT, MILLISECONDS)).isTrue();
        assertThat(onReceivedCb.getFuture().isDone()).isFalse();
        assertThat(onFinishListItemCb.getFuture().isDone()).isFalse();
        assertThat(capability.getAppAction())
                .isEqualTo(
                        AppAction.newBuilder()
                                .setName("actions.intent.TEST")
                                .setIdentifier("selectListItem")
                                .addParams(
                                        IntentParameter.newBuilder()
                                                .setName("listItem")
                                                .setIsRequired(true)
                                                .addCurrentValue(
                                                        CurrentValue.newBuilder()
                                                                .setValue(
                                                                        buildSearchActionParamValue(
                                                                                "apple"))
                                                                .setStatus(
                                                                        CurrentValue.Status
                                                                                .DISAMBIG)
                                                                .setDisambiguationData(
                                                                        DisambiguationData
                                                                                .newBuilder()
                                                                                .addEntities(
                                                                                        TypeConverters
                                                                                                .toEntity(
                                                                                                        item1))
                                                                                .addEntities(
                                                                                        TypeConverters
                                                                                                .toEntity(
                                                                                                        item2))
                                                                                .build())
                                                                .build())
                                                .build())
                                .addParams(
                                        IntentParameter.newBuilder()
                                                .setName("string")
                                                .setIsRequired(true)
                                                .build())
                                .setTaskInfo(
                                        TaskInfo.newBuilder().setSupportsPartialFulfillment(true))
                                .build());

        // second sync request, sending grounded ParamValue with identifier only
        SettableFutureWrapper<Boolean> secondTurnSuccess = new SettableFutureWrapper<>();
        capability.execute(
                buildRequestArgs(
                        SYNC, "listItem", ParamValue.newBuilder().setIdentifier("item2").build()),
                buildActionCallback(secondTurnSuccess));
        assertThat(secondTurnSuccess.getFuture().get(CB_TIMEOUT, MILLISECONDS)).isTrue();
        assertThat(onReceivedCb.getFuture().get(CB_TIMEOUT, MILLISECONDS)).isEqualTo(item2);
        assertThat(onFinishListItemCb.getFuture().isDone()).isFalse();

        // third sync request, sending grounded ParamValue with identifier only, completes task
        SettableFutureWrapper<Boolean> thirdTurnSuccess = new SettableFutureWrapper<>();
        capability.execute(
                buildRequestArgs(
                        SYNC,
                        "listItem",
                        ParamValue.newBuilder().setIdentifier("item2").build(),
                        "string",
                        "unused"),
                buildActionCallback(thirdTurnSuccess));
        assertThat(thirdTurnSuccess.getFuture().get(CB_TIMEOUT, MILLISECONDS)).isTrue();
        assertThat(onFinishListItemCb.getFuture().get(CB_TIMEOUT, MILLISECONDS)).isEqualTo(item2);
        assertThat(onFinishStringCb.getFuture().get(CB_TIMEOUT, MILLISECONDS)).isEqualTo("unused");
    }

    @Test
    public void executionResult_resultReturned() throws Exception {
        OnDialogFinishListener<Argument, Output> finishListener =
                (argument) ->
                        Futures.immediateFuture(
                                new ExecutionResult.Builder<Output>()
                                        .setOutput(
                                                Output.builder()
                                                        .setOptionalStringField("bar")
                                                        .setRepeatedStringField(
                                                                Arrays.asList("bar1", "bar2"))
                                                        .build())
                                        .build());
        ActionCapability capability =
                (ActionCapability)
                        new CapabilityBuilder()
                                .setTaskHandlerBuilder(
                                        new TaskHandlerBuilder()
                                                .setOnFinishListener(finishListener))
                                .build();
        SettableFutureWrapper<FulfillmentResponse> onSuccessInvoked = new SettableFutureWrapper<>();
        StructuredOutput expectedOutput =
                StructuredOutput.newBuilder()
                        .addOutputValues(
                                OutputValue.newBuilder()
                                        .setName("optionalStringOutput")
                                        .addValues(
                                                ParamValue.newBuilder()
                                                        .setStringValue("bar")
                                                        .build())
                                        .build())
                        .addOutputValues(
                                OutputValue.newBuilder()
                                        .setName("repeatedStringOutput")
                                        .addValues(
                                                ParamValue.newBuilder()
                                                        .setStringValue("bar1")
                                                        .build())
                                        .addValues(
                                                ParamValue.newBuilder()
                                                        .setStringValue("bar2")
                                                        .build())
                                        .build())
                        .build();

        capability.execute(
                buildRequestArgs(
                        SYNC,
                        /* args...= */ "required",
                        ParamValue.newBuilder().setIdentifier("foo").setStringValue("foo").build()),
                buildActionCallbackWithFulfillmentResponse(onSuccessInvoked));

        assertThat(
                        onSuccessInvoked
                                .getFuture()
                                .get(CB_TIMEOUT, MILLISECONDS)
                                .getExecutionOutput()
                                .getOutputValuesList())
                .containsExactlyElementsIn(expectedOutput.getOutputValuesList());
    }

    @Test
    public void touchEvent_fillOnlySlot_onFinishInvoked() throws Exception {
        EntityValue slotValue = EntityValue.newBuilder().setId("id1").setValue("value").build();
        SettableFutureWrapper<RequiredTaskUpdater> updaterFuture = new SettableFutureWrapper<>();
        SettableFutureWrapper<Argument> onFinishFuture = new SettableFutureWrapper<>();
        SettableFutureWrapper<FulfillmentResponse> touchEventResponse =
                new SettableFutureWrapper<>();

        ActionCapability capability =
                createTaskCapability(
                        SINGLE_REQUIRED_FIELD_PROPERTY,
                        TaskParamRegistry.builder().build(),
                        RequiredTaskUpdater::new,
                        TestingUtils.buildOnInitListener(updaterFuture),
                        EMPTY_CONFIRM_LISTENER,
                        TestingUtils.<Argument, Output>buildOnFinishListener(onFinishFuture));
        capability.setTouchEventCallback(buildTouchEventCallback(touchEventResponse));

        // Turn 1. No args but capability triggered (value updater should be set now).
        SettableFutureWrapper<Boolean> onSuccessInvoked = new SettableFutureWrapper<>();
        capability.execute(buildRequestArgs(SYNC), buildActionCallback(onSuccessInvoked));

        assertThat(onSuccessInvoked.getFuture().get(CB_TIMEOUT, MILLISECONDS)).isTrue();
        assertThat(onFinishFuture.getFuture().isDone()).isFalse();
        assertThat(touchEventResponse.getFuture().isDone()).isFalse();

        // Turn 2. Invoke the TaskCapability via the updater.
        // TaskUpdater should be usable after onFinishListener from the first turn.
        RequiredTaskUpdater taskUpdater = updaterFuture.getFuture().get(CB_TIMEOUT, MILLISECONDS);
        taskUpdater.setRequiredEntityValue(slotValue);

        assertThat(
                        onFinishFuture
                                .getFuture()
                                .get(CB_TIMEOUT, MILLISECONDS)
                                .requiredEntityField()
                                .get())
                .isNotNull();
        assertThat(
                        onFinishFuture
                                .getFuture()
                                .get(CB_TIMEOUT, MILLISECONDS)
                                .requiredEntityField()
                                .get())
                .isEqualTo(slotValue);
        assertThat(touchEventResponse.getFuture().get(CB_TIMEOUT, MILLISECONDS))
                .isEqualTo(FulfillmentResponse.getDefaultInstance());
    }

    @Test
    public void touchEvent_callbackNotSet_onFinishNotInvoked() throws Exception {
        EntityValue slotValue = EntityValue.newBuilder().setId("id1").setValue("value").build();
        SettableFutureWrapper<RequiredTaskUpdater> updaterFuture = new SettableFutureWrapper<>();
        SettableFutureWrapper<Argument> onFinishFuture = new SettableFutureWrapper<>();
        ActionCapability capability =
                createTaskCapability(
                        SINGLE_REQUIRED_FIELD_PROPERTY,
                        TaskParamRegistry.builder().build(),
                        RequiredTaskUpdater::new,
                        TestingUtils.buildOnInitListener(updaterFuture),
                        EMPTY_CONFIRM_LISTENER,
                        TestingUtils.<Argument, Output>buildOnFinishListener(onFinishFuture));
        // Explicitly set to null for testing.
        capability.setTouchEventCallback(null);

        // Turn 1. No args but capability triggered (value updater should be set now).
        SettableFutureWrapper<Boolean> onSuccessInvoked = new SettableFutureWrapper<>();
        capability.execute(buildRequestArgs(SYNC), buildActionCallback(onSuccessInvoked));

        assertThat(onSuccessInvoked.getFuture().get(CB_TIMEOUT, MILLISECONDS)).isTrue();
        assertThat(onFinishFuture.getFuture().isDone()).isFalse();

        // Turn 2. Invoke the TaskCapability via the updater.
        RequiredTaskUpdater taskUpdater = updaterFuture.getFuture().get(CB_TIMEOUT, MILLISECONDS);
        taskUpdater.setRequiredEntityValue(slotValue);

        assertThat(onFinishFuture.getFuture().isDone()).isFalse();
    }

    @Test
    public void touchEvent_emptyValues_onFinishNotInvoked() throws Exception {
        SettableFutureWrapper<RequiredTaskUpdater> updaterFuture = new SettableFutureWrapper<>();
        SettableFutureWrapper<Argument> onFinishFuture = new SettableFutureWrapper<>();
        SettableFutureWrapper<FulfillmentResponse> touchEventResponse =
                new SettableFutureWrapper<>();
        TaskCapabilityImpl<Property, Argument, Output, Confirmation, RequiredTaskUpdater>
                capability =
                        createTaskCapability(
                                SINGLE_REQUIRED_FIELD_PROPERTY,
                                TaskParamRegistry.builder().build(),
                                RequiredTaskUpdater::new,
                                TestingUtils.buildOnInitListener(updaterFuture),
                                EMPTY_CONFIRM_LISTENER,
                                TestingUtils.<Argument, Output>buildOnFinishListener(
                                        onFinishFuture));
        capability.setTouchEventCallback(buildTouchEventCallback(touchEventResponse));

        // Turn 1. No args but capability triggered (value updater should be set now).
        SettableFutureWrapper<Boolean> onSuccessInvoked = new SettableFutureWrapper<>();
        capability.execute(buildRequestArgs(SYNC), buildActionCallback(onSuccessInvoked));

        assertThat(onSuccessInvoked.getFuture().get(CB_TIMEOUT, MILLISECONDS)).isTrue();
        assertThat(onFinishFuture.getFuture().isDone()).isFalse();
        assertThat(touchEventResponse.getFuture().isDone()).isFalse();

        // Turn 2. Invoke the TaskCapability via the updater.
        // TaskUpdater should be usable after onFinishListener from the first turn.
        capability.updateParamValues(Collections.emptyMap());

        assertThat(onFinishFuture.getFuture().isDone()).isFalse();
        assertThat(touchEventResponse.getFuture().isDone()).isFalse();
    }

    @Test
    public void touchEvent_fillOnlySlot_confirmationRequired_onReadyToConfirmInvoked()
            throws Exception {
        EntityValue slotValue = EntityValue.newBuilder().setId("id1").setValue("value").build();
        SettableFutureWrapper<RequiredTaskUpdater> updaterFuture = new SettableFutureWrapper<>();
        SettableFutureWrapper<Argument> onReadyToConfirmFuture = new SettableFutureWrapper<>();
        SettableFutureWrapper<Argument> onFinishFuture = new SettableFutureWrapper<>();
        SettableFutureWrapper<FulfillmentResponse> touchEventResponse =
                new SettableFutureWrapper<>();

        TaskCapabilityImpl<Property, Argument, Output, Confirmation, RequiredTaskUpdater>
                capability =
                        createTaskCapability(
                                SINGLE_REQUIRED_FIELD_PROPERTY,
                                TaskParamRegistry.builder().build(),
                                RequiredTaskUpdater::new,
                                TestingUtils.buildOnInitListener(updaterFuture),
                                TestingUtils.<Argument, Confirmation>buildOnReadyToConfirmListener(
                                        onReadyToConfirmFuture),
                                TestingUtils.<Argument, Output>buildOnFinishListener(
                                        onFinishFuture));
        capability.setTouchEventCallback(buildTouchEventCallback(touchEventResponse));

        // Turn 1. No args but capability triggered (value updater should be set now).
        SettableFutureWrapper<Boolean> onSuccessInvoked = new SettableFutureWrapper<>();
        capability.execute(buildRequestArgs(SYNC), buildActionCallback(onSuccessInvoked));

        assertThat(onSuccessInvoked.getFuture().get(CB_TIMEOUT, MILLISECONDS)).isTrue();
        assertThat(onReadyToConfirmFuture.getFuture().isDone()).isFalse();
        assertThat(onFinishFuture.getFuture().isDone()).isFalse();
        assertThat(touchEventResponse.getFuture().isDone()).isFalse();

        // Turn 2. Invoke the TaskCapability via the updater.
        // TaskUpdater should be usable after onReadyToConfirmListener from the first turn.
        RequiredTaskUpdater taskUpdater = updaterFuture.getFuture().get(CB_TIMEOUT, MILLISECONDS);
        taskUpdater.setRequiredEntityValue(slotValue);

        assertThat(onFinishFuture.getFuture().isDone()).isFalse();
        assertThat(
                        onReadyToConfirmFuture
                                .getFuture()
                                .get(CB_TIMEOUT, MILLISECONDS)
                                .requiredEntityField()
                                .get())
                .isNotNull();
        assertThat(
                        onReadyToConfirmFuture
                                .getFuture()
                                .get(CB_TIMEOUT, MILLISECONDS)
                                .requiredEntityField()
                                .get())
                .isEqualTo(slotValue);
        assertThat(touchEventResponse.getFuture().get(CB_TIMEOUT, MILLISECONDS))
                .isEqualTo(FulfillmentResponse.getDefaultInstance());
    }

    @Test
    public void requiredConfirmation_throwsExecptionWhenConfirmationListenerIsNotSet()
            throws Exception {
        InvalidTaskException exception =
                assertThrows(
                        InvalidTaskException.class,
                        () ->
                                new CapabilityBuilderWithRequiredConfirmation()
                                        .setTaskHandlerBuilder(
                                                new TaskHandlerBuilderWithRequiredConfirmation())
                                        .build());

        assertThat(exception)
                .hasMessageThat()
                .contains("ConfirmationType is REQUIRED, but onReadyToConfirmListener is not set.");
    }

    @Test
    public void confirmationNotSupported_throwsExecptionWhenConfirmationListenerIsSet()
            throws Exception {

        OnReadyToConfirmListenerInternal<Confirmation> onReadyToConfirmListener =
                (args) ->
                        Futures.immediateFuture(
                                new ConfirmationOutput.Builder<Confirmation>()
                                        .setConfirmation(
                                                Confirmation.builder()
                                                        .setOptionalStringField("bar")
                                                        .build())
                                        .build());

        InvalidTaskException exception =
                assertThrows(
                        InvalidTaskException.class,
                        () ->
                                new CapabilityBuilder()
                                        .setTaskHandlerBuilder(
                                                new TaskHandlerBuilder()
                                                        .setOnReadyToConfirmListener(
                                                                onReadyToConfirmListener))
                                        .build());

        assertThat(exception)
                .hasMessageThat()
                .contains(
                        "ConfirmationType is NOT_SUPPORTED, but onReadyToConfirmListener is set.");
    }

    @Test
    public void confirmationOutput_resultReturned() throws Exception {
        OnReadyToConfirmListenerInternal<Confirmation> onReadyToConfirmListener =
                (args) ->
                        Futures.immediateFuture(
                                new ConfirmationOutput.Builder<Confirmation>()
                                        .setConfirmation(
                                                Confirmation.builder()
                                                        .setOptionalStringField("bar")
                                                        .build())
                                        .build());
        OnDialogFinishListener<Argument, Output> finishListener =
                (argument) ->
                        Futures.immediateFuture(
                                new ExecutionResult.Builder<Output>()
                                        .setOutput(
                                                Output.builder()
                                                        .setOptionalStringField("baz")
                                                        .setRepeatedStringField(
                                                                Arrays.asList("baz1", "baz2"))
                                                        .build())
                                        .build());
        ActionCapability capability =
                (ActionCapability)
                        new CapabilityBuilderWithRequiredConfirmation()
                                .setTaskHandlerBuilder(
                                        new TaskHandlerBuilderWithRequiredConfirmation()
                                                .setOnReadyToConfirmListener(
                                                        onReadyToConfirmListener)
                                                .setOnFinishListener(finishListener))
                                .build();
        SettableFutureWrapper<FulfillmentResponse> onSuccessInvoked = new SettableFutureWrapper<>();
        StructuredOutput expectedOutput =
                StructuredOutput.newBuilder()
                        .addOutputValues(
                                OutputValue.newBuilder()
                                        .setName("optionalStringOutput")
                                        .addValues(
                                                ParamValue.newBuilder()
                                                        .setStringValue("bar")
                                                        .build())
                                        .build())
                        .build();

        capability.execute(
                buildRequestArgs(
                        SYNC,
                        /* args...= */ "required",
                        ParamValue.newBuilder().setIdentifier("foo").setStringValue("foo").build()),
                buildActionCallbackWithFulfillmentResponse(onSuccessInvoked));

        assertThat(
                        onSuccessInvoked
                                .getFuture()
                                .get(CB_TIMEOUT, MILLISECONDS)
                                .getConfirmationData()
                                .getOutputValuesList())
                .containsExactlyElementsIn(expectedOutput.getOutputValuesList());
    }

    @Test
    public void executionResult_resultReturnedAfterConfirm() throws Exception {
        // Build the capability
        OnReadyToConfirmListenerInternal<Confirmation> onReadyToConfirmListener =
                (args) ->
                        Futures.immediateFuture(
                                new ConfirmationOutput.Builder<Confirmation>()
                                        .setConfirmation(
                                                Confirmation.builder()
                                                        .setOptionalStringField("bar")
                                                        .build())
                                        .build());
        OnDialogFinishListener<Argument, Output> finishListener =
                (argument) ->
                        Futures.immediateFuture(
                                new ExecutionResult.Builder<Output>()
                                        .setOutput(
                                                Output.builder()
                                                        .setOptionalStringField("baz")
                                                        .setRepeatedStringField(
                                                                Arrays.asList("baz1", "baz2"))
                                                        .build())
                                        .build());
        ActionCapability capability =
                (ActionCapability)
                        new CapabilityBuilderWithRequiredConfirmation()
                                .setTaskHandlerBuilder(
                                        new TaskHandlerBuilderWithRequiredConfirmation()
                                                .setOnReadyToConfirmListener(
                                                        onReadyToConfirmListener)
                                                .setOnFinishListener(finishListener))
                                .build();
        SettableFutureWrapper<FulfillmentResponse> onSuccessInvokedFirstTurn =
                new SettableFutureWrapper<>();

        // Send a sync request that triggers confirmation
        capability.execute(
                buildRequestArgs(
                        SYNC,
                        /* args...= */ "required",
                        ParamValue.newBuilder().setIdentifier("foo").setStringValue("foo").build()),
                buildActionCallbackWithFulfillmentResponse(onSuccessInvokedFirstTurn));

        // Confirm the BIC
        StructuredOutput expectedConfirmationOutput =
                StructuredOutput.newBuilder()
                        .addOutputValues(
                                OutputValue.newBuilder()
                                        .setName("optionalStringOutput")
                                        .addValues(
                                                ParamValue.newBuilder()
                                                        .setStringValue("bar")
                                                        .build())
                                        .build())
                        .build();
        assertThat(
                        onSuccessInvokedFirstTurn
                                .getFuture()
                                .get(CB_TIMEOUT, MILLISECONDS)
                                .getConfirmationData()
                                .getOutputValuesList())
                .containsExactlyElementsIn(expectedConfirmationOutput.getOutputValuesList());

        // Send a CONFIRM request which indicates the user confirmed the BIC. This triggers
        // onFinish.
        SettableFutureWrapper<FulfillmentResponse> onSuccessInvokedSecondTurn =
                new SettableFutureWrapper<>();
        capability.execute(
                buildRequestArgs(CONFIRM),
                buildActionCallbackWithFulfillmentResponse(onSuccessInvokedSecondTurn));

        // Confirm the BIO
        StructuredOutput expectedOutput =
                StructuredOutput.newBuilder()
                        .addOutputValues(
                                OutputValue.newBuilder()
                                        .setName("optionalStringOutput")
                                        .addValues(
                                                ParamValue.newBuilder()
                                                        .setStringValue("baz")
                                                        .build())
                                        .build())
                        .addOutputValues(
                                OutputValue.newBuilder()
                                        .setName("repeatedStringOutput")
                                        .addValues(
                                                ParamValue.newBuilder()
                                                        .setStringValue("baz1")
                                                        .build())
                                        .addValues(
                                                ParamValue.newBuilder()
                                                        .setStringValue("baz2")
                                                        .build())
                                        .build())
                        .build();
        assertThat(
                        onSuccessInvokedSecondTurn
                                .getFuture()
                                .get(CB_TIMEOUT, MILLISECONDS)
                                .getExecutionOutput()
                                .getOutputValuesList())
                .containsExactlyElementsIn(expectedOutput.getOutputValuesList());

        // send TERMINATE request after CONFIRM
        SettableFutureWrapper<Boolean> terminateCb = new SettableFutureWrapper<>();
        capability.execute(buildRequestArgs(TERMINATE), buildActionCallback(terminateCb));

        assertThat(terminateCb.getFuture().get(CB_TIMEOUT, MILLISECONDS)).isTrue();
    }

    @Test
    public void concurrentRequests_prioritizeAssistantRequest() throws Exception {
        CapabilityTwoStrings.Property property =
                CapabilityTwoStrings.Property.newBuilder()
                        .setStringSlotA(StringProperty.newBuilder().setIsRequired(true).build())
                        .setStringSlotB(StringProperty.newBuilder().setIsRequired(true).build())
                        .build();
        DeferredValueListener<String> slotAListener = new DeferredValueListener<>();
        DeferredValueListener<String> slotBListener = new DeferredValueListener<>();
        TaskParamRegistry.Builder paramRegistryBuilder = TaskParamRegistry.builder();
        paramRegistryBuilder.addTaskParameter(
                "stringSlotA",
                unused -> false,
                GenericResolverInternal.fromValueListener(slotAListener),
                Optional.empty(),
                Optional.empty(),
                TypeConverters::toStringValue);
        paramRegistryBuilder.addTaskParameter(
                "stringSlotB",
                unused -> false,
                GenericResolverInternal.fromValueListener(slotBListener),
                Optional.empty(),
                Optional.empty(),
                TypeConverters::toStringValue);
        SettableFutureWrapper<String> onFinishCb = new SettableFutureWrapper<>();
        TaskCapabilityImpl<
                        CapabilityTwoStrings.Property,
                        CapabilityTwoStrings.Argument,
                        Void,
                        Void,
                        EmptyTaskUpdater>
                taskCapability =
                        new TaskCapabilityImpl<>(
                                "myTestCapability",
                                CapabilityTwoStrings.ACTION_SPEC,
                                property,
                                paramRegistryBuilder.build(),
                                /* onInitListener= */ Optional.empty(),
                                /* onReadyToConfirmListener= */ Optional.empty(),
                                (argument) -> {
                                    String slotA = argument.stringSlotA().orElse(null);
                                    String slotB = argument.stringSlotB().orElse(null);
                                    onFinishCb.set(String.format("%s %s", slotA, slotB));
                                    return Futures.immediateFuture(
                                            ExecutionResult.getDefaultInstance());
                                },
                                new HashMap<>(),
                                new HashMap<>(),
                                Runnable::run);
        taskCapability.setTaskUpdaterSupplier(EmptyTaskUpdater::new);
        ReusableTouchEventCallback touchEventCallback = new ReusableTouchEventCallback();
        taskCapability.setTouchEventCallback(touchEventCallback);

        // first assistant request
        SettableFutureWrapper<FulfillmentResponse> firstTurnResult = new SettableFutureWrapper<>();
        taskCapability.execute(
                buildRequestArgs(SYNC, "stringSlotA", "apple"),
                buildActionCallbackWithFulfillmentResponse(firstTurnResult));

        // manual input request
        Map<String, List<ParamValue>> touchEventParamValues = new HashMap<>();
        touchEventParamValues.put(
                "stringSlotA",
                Collections.singletonList(ParamValue.newBuilder().setIdentifier("banana").build()));
        taskCapability.updateParamValues(touchEventParamValues);

        // second assistant request
        SettableFutureWrapper<FulfillmentResponse> secondTurnResult = new SettableFutureWrapper<>();
        taskCapability.execute(
                buildRequestArgs(SYNC, "stringSlotA", "apple", "stringSlotB", "smoothie"),
                buildActionCallbackWithFulfillmentResponse(secondTurnResult));

        assertThat(firstTurnResult.getFuture().isDone()).isFalse();
        assertThat(touchEventCallback.getLastResult()).isEqualTo(null);

        // unblock first assistant request
        slotAListener.setValidationResult(ValidationResult.newAccepted());
        assertThat(firstTurnResult.getFuture().get(CB_TIMEOUT, MILLISECONDS))
                .isEqualTo(FulfillmentResponse.getDefaultInstance());
        assertThat(touchEventCallback.getLastResult()).isEqualTo(null);

        // unblock second assistant request
        slotBListener.setValidationResult(ValidationResult.newAccepted());
        assertThat(secondTurnResult.getFuture().get(CB_TIMEOUT, MILLISECONDS))
                .isEqualTo(FulfillmentResponse.getDefaultInstance());

        assertThat(onFinishCb.getFuture().get(CB_TIMEOUT, MILLISECONDS))
                .isEqualTo("apple smoothie");

        // since task already finished, the manual input update was ignored.
        assertThat(touchEventCallback.getLastResult()).isNull();
    }

    @Test
    public void concurrentRequests_touchEventFinishesTask() throws Exception {
        CapabilityTwoStrings.Property property =
                CapabilityTwoStrings.Property.newBuilder()
                        .setStringSlotA(StringProperty.newBuilder().setIsRequired(true).build())
                        .setStringSlotB(StringProperty.newBuilder().setIsRequired(true).build())
                        .build();
        DeferredValueListener<String> slotAListener = new DeferredValueListener<>();
        DeferredValueListener<String> slotBListener = new DeferredValueListener<>();
        TaskParamRegistry.Builder paramRegistryBuilder = TaskParamRegistry.builder();
        paramRegistryBuilder.addTaskParameter(
                "stringSlotA",
                unused -> false,
                GenericResolverInternal.fromValueListener(slotAListener),
                Optional.empty(),
                Optional.empty(),
                TypeConverters::toStringValue);
        paramRegistryBuilder.addTaskParameter(
                "stringSlotB",
                unused -> false,
                GenericResolverInternal.fromValueListener(slotBListener),
                Optional.empty(),
                Optional.empty(),
                TypeConverters::toStringValue);
        SettableFutureWrapper<String> onFinishCb = new SettableFutureWrapper<>();
        TaskCapabilityImpl<
                        CapabilityTwoStrings.Property,
                        CapabilityTwoStrings.Argument,
                        Void,
                        Void,
                        EmptyTaskUpdater>
                taskCapability =
                        new TaskCapabilityImpl<>(
                                "myTestCapability",
                                CapabilityTwoStrings.ACTION_SPEC,
                                property,
                                paramRegistryBuilder.build(),
                                /* onInitListener= */ Optional.empty(),
                                /* onReadyToConfirmListener= */ Optional.empty(),
                                (argument) -> {
                                    String slotA = argument.stringSlotA().orElse(null);
                                    String slotB = argument.stringSlotB().orElse(null);
                                    onFinishCb.set(String.format("%s %s", slotA, slotB));
                                    return Futures.immediateFuture(
                                            ExecutionResult.getDefaultInstance());
                                },
                                new HashMap<>(),
                                new HashMap<>(),
                                Runnable::run);
        taskCapability.setTaskUpdaterSupplier(EmptyTaskUpdater::new);
        ReusableTouchEventCallback touchEventCallback = new ReusableTouchEventCallback();
        taskCapability.setTouchEventCallback(touchEventCallback);

        // first assistant request
        SettableFutureWrapper<FulfillmentResponse> firstTurnResult = new SettableFutureWrapper<>();
        taskCapability.execute(
                buildRequestArgs(SYNC, "stringSlotA", "apple"),
                buildActionCallbackWithFulfillmentResponse(firstTurnResult));

        // manual input request
        Map<String, List<ParamValue>> touchEventParamValues = new HashMap<>();
        touchEventParamValues.put(
                "stringSlotB",
                Collections.singletonList(
                        ParamValue.newBuilder().setIdentifier("smoothie").build()));
        taskCapability.updateParamValues(touchEventParamValues);

        // second assistant request
        SettableFutureWrapper<FulfillmentResponse> secondTurnResult = new SettableFutureWrapper<>();
        taskCapability.execute(
                buildRequestArgs(SYNC, "stringSlotA", "banana"),
                buildActionCallbackWithFulfillmentResponse(secondTurnResult));

        assertThat(firstTurnResult.getFuture().isDone()).isFalse();
        assertThat(touchEventCallback.getLastResult()).isEqualTo(null);

        // unblock first assistant request
        slotAListener.setValidationResult(ValidationResult.newAccepted());
        assertThat(firstTurnResult.getFuture().get(CB_TIMEOUT, MILLISECONDS))
                .isEqualTo(FulfillmentResponse.getDefaultInstance());
        assertThat(touchEventCallback.getLastResult()).isEqualTo(null);

        // unblock second assistant request
        slotAListener.setValidationResult(ValidationResult.newAccepted());
        assertThat(secondTurnResult.getFuture().get(CB_TIMEOUT, MILLISECONDS))
                .isEqualTo(FulfillmentResponse.getDefaultInstance());
        assertThat(onFinishCb.getFuture().get(CB_TIMEOUT, MILLISECONDS))
                .isEqualTo("banana smoothie");
        assertThat(touchEventCallback.getLastResult().getKind())
                .isEqualTo(TouchEventResult.Kind.SUCCESS);
    }

    @Test
    public void touchEvent_noDisambig_continuesProcessing() throws Exception {
        TaskParamRegistry.Builder paramRegistryBuilder = TaskParamRegistry.builder();
        SettableFutureWrapper<RequiredTaskUpdater> taskUpdaterCb = new SettableFutureWrapper<>();
        SettableFutureWrapper<Boolean> onFinishCb = new SettableFutureWrapper<>();
        CapabilityTwoEntityValues.Property property =
                CapabilityTwoEntityValues.Property.newBuilder()
                        .setSlotA(EntityProperty.newBuilder().setIsRequired(true).build())
                        .setSlotB(EntityProperty.newBuilder().setIsRequired(true).build())
                        .build();
        paramRegistryBuilder.addTaskParameter(
                "slotA",
                paramValue -> !paramValue.hasIdentifier(),
                GenericResolverInternal.fromAppEntityResolver(
                        new AppEntityResolver<EntityValue>() {
                            @NonNull
                            @Override
                            public ListenableFuture<ValidationResult> onReceived(
                                    EntityValue unused) {
                                return Futures.immediateFuture(ValidationResult.newAccepted());
                            }

                            @Override
                            public ListenableFuture<EntitySearchResult<EntityValue>>
                                    lookupAndRender(SearchAction<EntityValue> unused) {
                                return Futures.immediateFuture(
                                        EntitySearchResult.<EntityValue>newBuilder()
                                                .addPossibleValue(EntityValue.ofId("entityValue1"))
                                                .addPossibleValue(EntityValue.ofId("entityValue2"))
                                                .build());
                            }
                        }),
                Optional.of(TypeConverters::toEntity),
                Optional.of(paramValue -> SearchAction.<EntityValue>newBuilder().build()),
                TypeConverters::toEntityValue);
        TaskCapabilityImpl<
                        CapabilityTwoEntityValues.Property,
                        CapabilityTwoEntityValues.Argument,
                        Void,
                        Void,
                        RequiredTaskUpdater>
                taskCapability =
                        new TaskCapabilityImpl<>(
                                "fakeId",
                                CapabilityTwoEntityValues.ACTION_SPEC,
                                property,
                                paramRegistryBuilder.build(),
                                /* onInitListener= */ Optional.of(
                                        taskUpdater -> {
                                            taskUpdaterCb.set(taskUpdater);
                                            return Futures.immediateVoidFuture();
                                        }),
                                /* onReadyToConfirmListener= */ Optional.empty(),
                                /* onFinishListener= */ (paramValuesMap) -> {
                            onFinishCb.set(true);
                            return Futures.immediateFuture(
                                            ExecutionResult.getDefaultInstance());
                        },
                                /* confirmationOutputBindings= */ new HashMap<>(),
                                /* executionOutputBindings= */ new HashMap<>(),
                                Runnable::run);
        taskCapability.setTaskUpdaterSupplier(RequiredTaskUpdater::new);
        SettableFutureWrapper<FulfillmentResponse> touchEventCb = new SettableFutureWrapper<>();
        TouchEventCallback touchEventCallback = buildTouchEventCallback(touchEventCb);
        taskCapability.setTouchEventCallback(touchEventCallback);

        // turn 1
        SettableFutureWrapper<Boolean> turn1Finished = new SettableFutureWrapper<>();
        taskCapability.execute(
                buildRequestArgs(
                        SYNC, "slotA", buildSearchActionParamValue("query"), "slotB", "anything"),
                buildActionCallback(turn1Finished));

        assertThat(turn1Finished.getFuture().get(CB_TIMEOUT, MILLISECONDS)).isTrue();
        assertThat(taskCapability.getAppAction())
                .isEqualTo(
                        AppAction.newBuilder()
                                .setName("actions.intent.TEST")
                                .setIdentifier("fakeId")
                                .addParams(
                                        IntentParameter.newBuilder()
                                                .setName("slotA")
                                                .setIsRequired(true)
                                                .addCurrentValue(
                                                        CurrentValue.newBuilder()
                                                                .setValue(
                                                                        buildSearchActionParamValue(
                                                                                "query"))
                                                                .setStatus(
                                                                        CurrentValue.Status
                                                                                .DISAMBIG)
                                                                .setDisambiguationData(
                                                                        DisambiguationData
                                                                                .newBuilder()
                                                                                .addEntities(
                                                                                        Entity
                                                                                                .newBuilder()
                                                                                                .setIdentifier(
                                                                                                        "entityValue1")
                                                                                                .setName(
                                                                                                        "entityValue1")
                                                                                                .build())
                                                                                .addEntities(
                                                                                        Entity
                                                                                                .newBuilder()
                                                                                                .setIdentifier(
                                                                                                        "entityValue2")
                                                                                                .setName(
                                                                                                        "entityValue2")
                                                                                                .build())
                                                                                .build())
                                                                .build())
                                                .build())
                                .addParams(
                                        IntentParameter.newBuilder()
                                                .setName("slotB")
                                                .setIsRequired(true)
                                                .addCurrentValue(
                                                        CurrentValue.newBuilder()
                                                                .setValue(
                                                                        ParamValue.newBuilder()
                                                                                .setStringValue(
                                                                                        "anything")
                                                                                .build())
                                                                .setStatus(
                                                                        CurrentValue.Status.PENDING)
                                                                .build())
                                                .build())
                                .setTaskInfo(
                                        TaskInfo.newBuilder()
                                                .setSupportsPartialFulfillment(true)
                                                .build())
                                .build());

        // turn 2
        taskCapability.updateParamValues(
                Collections.singletonMap(
                        "slotA",
                        Collections.singletonList(
                                ParamValue.newBuilder()
                                        .setIdentifier("entityValue1")
                                        .setStringValue("entityValue1")
                                        .build())));

        assertThat(touchEventCb.getFuture().get(CB_TIMEOUT, MILLISECONDS))
                .isEqualTo(FulfillmentResponse.getDefaultInstance());
        assertThat(onFinishCb.getFuture().get()).isTrue();
    }

    private static class RequiredTaskUpdater extends AbstractTaskUpdater {
        void setRequiredEntityValue(EntityValue entityValue) {
            super.updateParamValues(
                    Collections.singletonMap(
                            "required",
                            Collections.singletonList(TypeConverters.toParamValue(entityValue))));
        }
    }

    private static class EmptyTaskUpdater extends AbstractTaskUpdater {}

    private static class DeferredValueListener<T> implements ValueListener<T> {

        final AtomicReference<Completer<ValidationResult>> mCompleterRef = new AtomicReference<>();

        void setValidationResult(ValidationResult t) {
            Completer<ValidationResult> completer = mCompleterRef.getAndSet(null);
            if (completer == null) {
                throw new IllegalStateException("no onReceived is waiting");
            }
            completer.set(t);
        }

        @NonNull
        @Override
        public ListenableFuture<ValidationResult> onReceived(T value) {
            return CallbackToFutureAdapter.getFuture(
                    newCompleter -> {
                        Completer<ValidationResult> oldCompleter =
                                mCompleterRef.getAndSet(newCompleter);
                        if (oldCompleter != null) {
                            oldCompleter.setCancelled();
                        }
                        return "waiting for setValidationResult";
                    });
        }
    }

    private static class CapabilityBuilder
            extends AbstractCapabilityBuilder<
                    CapabilityBuilder,
                    Property,
                    Argument,
                    Output,
                    Confirmation,
                    RequiredTaskUpdater> {
        @SuppressWarnings("CheckReturnValue")
        private CapabilityBuilder() {
            super(ACTION_SPEC);
            setId("id");
            setProperty(SINGLE_REQUIRED_FIELD_PROPERTY);
        }

        @NonNull
        public final CapabilityBuilder setTaskHandlerBuilder(
                TaskHandlerBuilder taskHandlerBuilder) {
            return setTaskHandler(taskHandlerBuilder.build());
        }
    }

    private static class TaskHandlerBuilder
            extends AbstractTaskHandlerBuilder<
                    TaskHandlerBuilder, Argument, Output, Confirmation, RequiredTaskUpdater> {

        private TaskHandlerBuilder() {
            super.registerExecutionOutput(
                    "optionalStringOutput",
                    Output::optionalStringField,
                    TypeConverters::toParamValue);
            super.registerRepeatedExecutionOutput(
                    "repeatedStringOutput",
                    Output::repeatedStringField,
                    TypeConverters::toParamValue);
        }

        @Override
        protected Supplier<RequiredTaskUpdater> getTaskUpdaterSupplier() {
            return RequiredTaskUpdater::new;
        }

        public TaskHandlerBuilder setOnReadyToConfirmListener(
                OnReadyToConfirmListenerInternal<Confirmation> listener) {
            return super.setOnReadyToConfirmListenerInternal(listener);
        }
    }

    private static class CapabilityBuilderWithRequiredConfirmation
            extends AbstractCapabilityBuilder<
                    CapabilityBuilderWithRequiredConfirmation,
                    Property,
                    Argument,
                    Output,
                    Confirmation,
                    RequiredTaskUpdater> {
        @SuppressWarnings("CheckReturnValue")
        private CapabilityBuilderWithRequiredConfirmation() {
            super(ACTION_SPEC);
            setProperty(SINGLE_REQUIRED_FIELD_PROPERTY);
            setId("id");
        }

        @NonNull
        public final CapabilityBuilderWithRequiredConfirmation setTaskHandlerBuilder(
                TaskHandlerBuilderWithRequiredConfirmation taskHandlerBuilder) {
            return setTaskHandler(taskHandlerBuilder.build());
        }
    }

    private static class TaskHandlerBuilderWithRequiredConfirmation
            extends AbstractTaskHandlerBuilder<
                    TaskHandlerBuilderWithRequiredConfirmation,
                    Argument,
                    Output,
                    Confirmation,
                    RequiredTaskUpdater> {

        private TaskHandlerBuilderWithRequiredConfirmation() {
            super(ConfirmationType.REQUIRED);
            super.registerExecutionOutput(
                    "optionalStringOutput",
                    Output::optionalStringField,
                    TypeConverters::toParamValue);
            super.registerRepeatedExecutionOutput(
                    "repeatedStringOutput",
                    Output::repeatedStringField,
                    TypeConverters::toParamValue);
            super.registerConfirmationOutput(
                    "optionalStringOutput",
                    Confirmation::optionalStringField,
                    TypeConverters::toParamValue);
        }

        @Override
        protected Supplier<RequiredTaskUpdater> getTaskUpdaterSupplier() {
            return RequiredTaskUpdater::new;
        }

        public TaskHandlerBuilderWithRequiredConfirmation setOnReadyToConfirmListener(
                OnReadyToConfirmListenerInternal<Confirmation> listener) {
            return super.setOnReadyToConfirmListenerInternal(listener);
        }
    }
}
