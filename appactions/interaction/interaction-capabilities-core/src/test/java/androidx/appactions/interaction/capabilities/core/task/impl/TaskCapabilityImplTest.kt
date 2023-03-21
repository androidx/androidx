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
package androidx.appactions.interaction.capabilities.core.task.impl

import android.util.SizeF
import androidx.appactions.interaction.capabilities.core.ActionCapability
import androidx.appactions.interaction.capabilities.core.CapabilityBuilderBase
import androidx.appactions.interaction.capabilities.core.ExecutionResult
import androidx.appactions.interaction.capabilities.core.HostProperties
import androidx.appactions.interaction.capabilities.core.InitArg
import androidx.appactions.interaction.capabilities.core.SessionFactory
import androidx.appactions.interaction.capabilities.core.impl.ActionCapabilitySession
import androidx.appactions.interaction.capabilities.core.impl.ErrorStatusInternal
import androidx.appactions.interaction.capabilities.core.impl.concurrent.Futures
import androidx.appactions.interaction.capabilities.core.impl.converters.DisambigEntityConverter
import androidx.appactions.interaction.capabilities.core.impl.converters.SearchActionConverter
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpec
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecBuilder
import androidx.appactions.interaction.capabilities.core.properties.SimpleProperty
import androidx.appactions.interaction.capabilities.core.properties.StringValue
import androidx.appactions.interaction.capabilities.core.properties.TypeProperty
import androidx.appactions.interaction.capabilities.core.task.AppEntityResolver
import androidx.appactions.interaction.capabilities.core.task.EntitySearchResult
import androidx.appactions.interaction.capabilities.core.task.ValidationResult
import androidx.appactions.interaction.capabilities.core.task.ValueListener
import androidx.appactions.interaction.capabilities.core.testing.ArgumentUtils.buildRequestArgs
import androidx.appactions.interaction.capabilities.core.testing.ArgumentUtils.buildSearchActionParamValue
import androidx.appactions.interaction.capabilities.core.testing.TestingUtils.CB_TIMEOUT
import androidx.appactions.interaction.capabilities.core.testing.TestingUtils.buildActionCallback
import androidx.appactions.interaction.capabilities.core.testing.TestingUtils.buildActionCallbackWithFulfillmentResponse
import androidx.appactions.interaction.capabilities.core.testing.TestingUtils.buildErrorActionCallback
import androidx.appactions.interaction.capabilities.core.testing.spec.Argument
import androidx.appactions.interaction.capabilities.core.testing.spec.CapabilityStructFill
import androidx.appactions.interaction.capabilities.core.testing.spec.CapabilityTwoEntityValues
import androidx.appactions.interaction.capabilities.core.testing.spec.Confirmation
import androidx.appactions.interaction.capabilities.core.testing.spec.Output
import androidx.appactions.interaction.capabilities.core.testing.spec.Property
import androidx.appactions.interaction.capabilities.core.testing.spec.Session
import androidx.appactions.interaction.capabilities.core.testing.spec.SettableFutureWrapper
import androidx.appactions.interaction.capabilities.core.testing.spec.TestEnum
import androidx.appactions.interaction.capabilities.core.values.EntityValue
import androidx.appactions.interaction.capabilities.core.values.ListItem
import androidx.appactions.interaction.capabilities.core.values.SearchAction
import androidx.appactions.interaction.proto.AppActionsContext.AppAction
import androidx.appactions.interaction.proto.AppActionsContext.AppDialogState
import androidx.appactions.interaction.proto.AppActionsContext.DialogParameter
import androidx.appactions.interaction.proto.AppActionsContext.IntentParameter
import androidx.appactions.interaction.proto.CurrentValue
import androidx.appactions.interaction.proto.DisambiguationData
import androidx.appactions.interaction.proto.Entity
import androidx.appactions.interaction.proto.FulfillmentRequest.Fulfillment.Type.SYNC
import androidx.appactions.interaction.proto.FulfillmentRequest.Fulfillment.Type.TERMINATE
import androidx.appactions.interaction.proto.FulfillmentRequest.Fulfillment.Type.UNKNOWN_TYPE
import androidx.appactions.interaction.proto.FulfillmentResponse
import androidx.appactions.interaction.proto.FulfillmentResponse.StructuredOutput
import androidx.appactions.interaction.proto.FulfillmentResponse.StructuredOutput.OutputValue
import androidx.appactions.interaction.proto.ParamValue
import androidx.appactions.interaction.proto.TaskInfo
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.concurrent.futures.CallbackToFutureAdapter.Completer
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Supplier
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TaskCapabilityImplTest {
    val capability: ActionCapability =
        createCapability<EmptyTaskUpdater>(
            SINGLE_REQUIRED_FIELD_PROPERTY,
            sessionFactory =
                SessionFactory {
                    object : Session {
                        override fun onFinishAsync(argument: Argument) =
                            Futures.immediateFuture(ExecutionResult.getDefaultInstance<Output>())
                    }
                },
            sessionBridge = SessionBridge { TaskHandler.Builder<Confirmation>().build() },
            sessionUpdaterSupplier = ::EmptyTaskUpdater,
        )
    val hostProperties: HostProperties =
        HostProperties.Builder()
            .setMaxHostSizeDp(
                SizeF(300f, 500f),
            )
            .build()

    @Test
    fun getAppAction_smokeTest() {
        assertThat(capability.getAppAction())
            .isEqualTo(
                AppAction.newBuilder()
                    .setName("actions.intent.TEST")
                    .setIdentifier("id")
                    .addParams(
                        IntentParameter.newBuilder().setName("required").setIsRequired(true),
                    )
                    .setTaskInfo(
                        TaskInfo.newBuilder().setSupportsPartialFulfillment(true),
                    )
                    .build(),
            )
    }

    @Test
    fun actionCapabilitySession_getUiHandle() {
        val externalSession = object : Session {}
        val capability =
            createCapability(
                SINGLE_REQUIRED_FIELD_PROPERTY,
                SessionFactory { externalSession },
                SessionBridge { TaskHandler.Builder<Confirmation>().build() },
                ::EmptyTaskUpdater,
            )
        val session = capability.createSession(hostProperties)
        assertThat(session.uiHandle).isSameInstanceAs(externalSession)
    }

    @Test
    @kotlin.Throws(Exception::class)
    fun onInitInvoked_invokedOnce() {
        val onSuccessInvocationCount = AtomicInteger(0)
        val capability: ActionCapability =
            createCapability(
                SINGLE_REQUIRED_FIELD_PROPERTY,
                sessionFactory =
                    SessionFactory {
                        object : Session {
                            override fun onInit(initArg: InitArg) {
                                onSuccessInvocationCount.incrementAndGet()
                            }
                            override fun onFinishAsync(argument: Argument) =
                                Futures.immediateFuture(
                                    ExecutionResult.getDefaultInstance<Output>(),
                                )
                        }
                    },
                sessionBridge = SessionBridge { TaskHandler.Builder<Confirmation>().build() },
                sessionUpdaterSupplier = ::EmptyTaskUpdater,
            )
        val session = capability.createSession(hostProperties)

        // TURN 1.
        val onSuccessInvoked: SettableFutureWrapper<Boolean> = SettableFutureWrapper()
        session.execute(
            buildRequestArgs(SYNC, "unknownArgName", "foo"),
            buildActionCallback(onSuccessInvoked),
        )
        assertThat(onSuccessInvoked.getFuture().get(CB_TIMEOUT, MILLISECONDS)).isTrue()
        assertThat(onSuccessInvocationCount.get()).isEqualTo(1)

        // TURN 2.
        val onSuccessInvoked2: SettableFutureWrapper<Boolean> = SettableFutureWrapper()
        session.execute(
            buildRequestArgs(
                SYNC,
                "required",
                ParamValue.newBuilder().setIdentifier("foo").setStringValue("foo").build(),
            ),
            buildActionCallback(onSuccessInvoked2),
        )
        assertThat(onSuccessInvoked2.getFuture().get(CB_TIMEOUT, MILLISECONDS)).isTrue()
        assertThat(onSuccessInvocationCount.get()).isEqualTo(1)
    }

    class RequiredTaskUpdater : AbstractTaskUpdater() {
        fun setRequiredEntityValue(entityValue: EntityValue) {
            super.updateParamValues(
                mapOf(
                    "required" to listOf(TypeConverters.toParamValue(entityValue)),
                ),
            )
        }
    }

    private class DeferredValueListener<T> : ValueListener<T> {
        val mCompleterRef: AtomicReference<Completer<ValidationResult>> =
            AtomicReference<Completer<ValidationResult>>()

        fun setValidationResult(t: ValidationResult) {
            val completer: Completer<ValidationResult> =
                mCompleterRef.getAndSet(null)
                    ?: throw IllegalStateException("no onReceived is waiting")
            completer.set(t)
        }

        override fun onReceivedAsync(value: T): ListenableFuture<ValidationResult> {
            return CallbackToFutureAdapter.getFuture { newCompleter ->
                val oldCompleter: Completer<ValidationResult>? =
                    mCompleterRef.getAndSet(
                        newCompleter,
                    )
                if (oldCompleter != null) {
                    oldCompleter.setCancelled()
                }
                "waiting for setValidationResult"
            }
        }
    }

    @Test
    @kotlin.Throws(Exception::class)
    fun fulfillmentType_unknown_errorReported() {
        val capability: ActionCapability =
            createCapability(
                SINGLE_REQUIRED_FIELD_PROPERTY,
                sessionFactory =
                    SessionFactory {
                        object : Session {
                            override fun onFinishAsync(argument: Argument) =
                                Futures.immediateFuture(
                                    ExecutionResult.getDefaultInstance<Output>(),
                                )
                        }
                    },
                sessionBridge = SessionBridge { TaskHandler.Builder<Confirmation>().build() },
                sessionUpdaterSupplier = ::RequiredTaskUpdater,
            )
        val session = capability.createSession(hostProperties)

        assertThat(capability.getAppAction())
            .isEqualTo(
                AppAction.newBuilder()
                    .setName("actions.intent.TEST")
                    .setIdentifier("id")
                    .addParams(
                        IntentParameter.newBuilder().setName("required").setIsRequired(true),
                    )
                    .setTaskInfo(
                        TaskInfo.newBuilder().setSupportsPartialFulfillment(true),
                    )
                    .build(),
            )

        // TURN 1 (UNKNOWN).
        val errorCb: SettableFutureWrapper<ErrorStatusInternal> = SettableFutureWrapper()
        session.execute(buildRequestArgs(UNKNOWN_TYPE), buildErrorActionCallback(errorCb))
        assertThat(errorCb.getFuture().get(CB_TIMEOUT, MILLISECONDS))
            .isEqualTo(ErrorStatusInternal.INVALID_REQUEST_TYPE)
    }

    @Test
    fun slotFilling_getStatus_smokeTest() {
        val property: CapabilityTwoEntityValues.Property =
            CapabilityTwoEntityValues.Property.newBuilder()
                .setSlotA(
                    TypeProperty.Builder<
                            androidx.appactions.interaction.capabilities.core.properties.Entity
                        >()
                        .setRequired(true)
                        .build()
                )
                .setSlotB(
                    TypeProperty.Builder<
                            androidx.appactions.interaction.capabilities.core.properties.Entity
                        >()
                        .setRequired(true)
                        .build()
                )
                .build()
        val sessionFactory =
            SessionFactory<CapabilityTwoEntityValues.Session> {
                object : CapabilityTwoEntityValues.Session {
                    override suspend fun onFinish(
                        argument: CapabilityTwoEntityValues.Argument,
                    ): ExecutionResult<Void> = ExecutionResult.getDefaultInstance()
                }
            }
        val sessionBridge =
            SessionBridge<CapabilityTwoEntityValues.Session, Void> {
                TaskHandler.Builder<Void>()
                    .registerValueTaskParam(
                        "slotA",
                        AUTO_ACCEPT_ENTITY_VALUE,
                        TypeConverters::toEntityValue,
                    )
                    .registerValueTaskParam(
                        "slotB",
                        AUTO_ACCEPT_ENTITY_VALUE,
                        TypeConverters::toEntityValue,
                    )
                    .build()
            }
        val capability: ActionCapability =
            TaskCapabilityImpl(
                "fakeId",
                CapabilityTwoEntityValues.ACTION_SPEC,
                property,
                sessionFactory,
                sessionBridge,
                ::EmptyTaskUpdater,
            )

        val session = capability.createSession(hostProperties)
        assertThat(session.status).isEqualTo(ActionCapabilitySession.Status.UNINITIATED)

        // turn 1
        val turn1Success: SettableFutureWrapper<Boolean> = SettableFutureWrapper()
        session.execute(
            buildRequestArgs(
                SYNC,
                "slotA",
                ParamValue.newBuilder().setIdentifier("foo").setStringValue("foo").build(),
            ),
            buildActionCallback(turn1Success),
        )
        assertThat(turn1Success.getFuture().get(CB_TIMEOUT, MILLISECONDS)).isTrue()
        assertThat(session.status).isEqualTo(ActionCapabilitySession.Status.IN_PROGRESS)

        // turn 2
        val turn2Success: SettableFutureWrapper<Boolean> = SettableFutureWrapper()
        session.execute(
            buildRequestArgs(
                SYNC,
                "slotA",
                ParamValue.newBuilder().setIdentifier("foo").setStringValue("foo").build(),
                "slotB",
                ParamValue.newBuilder().setIdentifier("bar").setStringValue("bar").build(),
            ),
            buildActionCallback(turn2Success),
        )
        assertThat(turn2Success.getFuture().get(CB_TIMEOUT, MILLISECONDS)).isTrue()
        assertThat(session.status).isEqualTo(ActionCapabilitySession.Status.COMPLETED)

        // turn 3
        val turn3Success: SettableFutureWrapper<Boolean> = SettableFutureWrapper()
        session.execute(
            buildRequestArgs(TERMINATE),
            buildActionCallback(turn3Success),
        )
        assertThat(turn3Success.getFuture().get(CB_TIMEOUT, MILLISECONDS)).isTrue()
        assertThat(session.status).isEqualTo(ActionCapabilitySession.Status.DESTROYED)
    }

    @Test
    @kotlin.Throws(Exception::class)
    fun slotFilling_optionalButRejectedParam_onFinishNotInvoked() {
        val onFinishInvocationCount = AtomicInteger(0)
        val property: CapabilityTwoEntityValues.Property =
            CapabilityTwoEntityValues.Property.newBuilder()
                .setSlotA(
                    TypeProperty.Builder<
                            androidx.appactions.interaction.capabilities.core.properties.Entity
                        >()
                        .setRequired(true)
                        .build()
                )
                .setSlotB(
                    TypeProperty.Builder<
                            androidx.appactions.interaction.capabilities.core.properties.Entity
                        >()
                        .setRequired(false)
                        .build()
                )
                .build()
        val sessionFactory =
            SessionFactory<CapabilityTwoEntityValues.Session> {
                object : CapabilityTwoEntityValues.Session {
                    override suspend fun onFinish(
                        argument: CapabilityTwoEntityValues.Argument,
                    ): ExecutionResult<Void> {
                        onFinishInvocationCount.incrementAndGet()
                        return ExecutionResult.getDefaultInstance()
                    }
                }
            }
        val sessionBridge =
            SessionBridge<CapabilityTwoEntityValues.Session, Void> {
                TaskHandler.Builder<Void>()
                    .registerValueTaskParam(
                        "slotA",
                        AUTO_ACCEPT_ENTITY_VALUE,
                        TypeConverters::toEntityValue,
                    )
                    .registerValueTaskParam(
                        "slotB",
                        AUTO_REJECT_ENTITY_VALUE,
                        TypeConverters::toEntityValue,
                    )
                    .build()
            }
        val capability: ActionCapability =
            TaskCapabilityImpl(
                "fakeId",
                CapabilityTwoEntityValues.ACTION_SPEC,
                property,
                sessionFactory,
                sessionBridge,
                ::EmptyTaskUpdater,
            )
        val session = capability.createSession(hostProperties)

        // TURN 1.
        val onSuccessInvoked: SettableFutureWrapper<Boolean> = SettableFutureWrapper()
        session.execute(
            buildRequestArgs(
                SYNC,
                "slotA",
                ParamValue.newBuilder().setIdentifier("foo").setStringValue("foo").build(),
                "slotB",
                ParamValue.newBuilder().setIdentifier("bar").setStringValue("bar").build(),
            ),
            buildActionCallback(onSuccessInvoked),
        )
        assertThat(onSuccessInvoked.getFuture().get(CB_TIMEOUT, MILLISECONDS)).isTrue()
        assertThat(onFinishInvocationCount.get()).isEqualTo(0)
        assertThat(getCurrentValues("slotA", session.state))
            .containsExactly(
                CurrentValue.newBuilder()
                    .setValue(
                        ParamValue.newBuilder().setIdentifier("foo").setStringValue("foo"),
                    )
                    .setStatus(CurrentValue.Status.ACCEPTED)
                    .build(),
            )
        assertThat(getCurrentValues("slotB", session.state))
            .containsExactly(
                CurrentValue.newBuilder()
                    .setValue(
                        ParamValue.newBuilder().setIdentifier("bar").setStringValue("bar"),
                    )
                    .setStatus(CurrentValue.Status.REJECTED)
                    .build(),
            )
    }

    @Test
    @kotlin.Throws(Exception::class)
    fun slotFilling_assistantRemovedParam_clearInSdkState() {
        val property: Property =
            Property.newBuilder()
                .setRequiredEntityField(
                    TypeProperty.Builder<
                            androidx.appactions.interaction.capabilities.core.properties.Entity
                        >()
                        .setRequired(true)
                        .build(),
                )
                .setEnumField(
                    TypeProperty.Builder<TestEnum>()
                        .setPossibleValues(TestEnum.VALUE_1, TestEnum.VALUE_2)
                        .setRequired(true)
                        .build(),
                )
                .build()
        val capability: ActionCapability =
            createCapability(
                property,
                sessionFactory = SessionFactory { Session.DEFAULT },
                sessionBridge = SessionBridge { TaskHandler.Builder<Confirmation>().build() },
                sessionUpdaterSupplier = ::EmptyTaskUpdater,
            )
        val session = capability.createSession(hostProperties)

        // TURN 1.
        val onSuccessInvoked: SettableFutureWrapper<Boolean> = SettableFutureWrapper()
        session.execute(
            buildRequestArgs(
                SYNC,
                "required",
                ParamValue.newBuilder().setIdentifier("foo").setStringValue("foo").build(),
            ),
            buildActionCallback(onSuccessInvoked),
        )
        assertThat(onSuccessInvoked.getFuture().get(CB_TIMEOUT, MILLISECONDS)).isTrue()
        assertThat(getCurrentValues("required", session.state))
            .containsExactly(
                CurrentValue.newBuilder()
                    .setValue(
                        ParamValue.newBuilder().setIdentifier("foo").setStringValue("foo"),
                    )
                    .setStatus(CurrentValue.Status.ACCEPTED)
                    .build(),
            )
        assertThat(getCurrentValues("optionalEnum", session.state)).isEmpty()

        // TURN 2.
        val onSuccessInvoked2: SettableFutureWrapper<Boolean> = SettableFutureWrapper()
        session.execute(
            buildRequestArgs(SYNC, "optionalEnum", TestEnum.VALUE_2),
            buildActionCallback(onSuccessInvoked2),
        )
        assertThat(onSuccessInvoked2.getFuture().get(CB_TIMEOUT, MILLISECONDS)).isTrue()
        assertThat(getCurrentValues("required", session.state)).isEmpty()
        assertThat(getCurrentValues("optionalEnum", session.state))
            .containsExactly(
                CurrentValue.newBuilder()
                    .setValue(ParamValue.newBuilder().setIdentifier("VALUE_2"))
                    .setStatus(CurrentValue.Status.ACCEPTED)
                    .build(),
            )
    }

    @Test
    @kotlin.Throws(Exception::class)
    @Suppress("DEPRECATION") // TODO(b/269638788) migrate session state to AppDialogState message
    fun disambig_singleParam_disambigEntitiesInContext() {
        val capability: ActionCapability =
            createCapability(
                SINGLE_REQUIRED_FIELD_PROPERTY,
                sessionFactory = {
                    object : Session {
                        override suspend fun onFinish(argument: Argument) =
                            ExecutionResult.getDefaultInstance<Output>()
                        override fun getRequiredEntityListener() =
                            object : AppEntityResolver<EntityValue> {
                                override fun lookupAndRender(
                                    searchAction: SearchAction<EntityValue>,
                                ): ListenableFuture<EntitySearchResult<EntityValue>> {
                                    val result = EntitySearchResult.Builder<EntityValue>()
                                    return Futures.immediateFuture(
                                        result
                                            .addPossibleValue(EntityValue.ofId("valid1"))
                                            .addPossibleValue(EntityValue.ofId("valid2"))
                                            .build(),
                                    )
                                }

                                override fun onReceivedAsync(
                                    value: EntityValue,
                                ): ListenableFuture<ValidationResult> {
                                    return Futures.immediateFuture(ValidationResult.newAccepted())
                                }
                            }
                    }
                },
                sessionBridge =
                    SessionBridge<Session, Confirmation> { session ->
                        val builder = TaskHandler.Builder<Confirmation>()
                        session.getRequiredEntityListener()?.let {
                            listener: AppEntityResolver<EntityValue> ->
                            builder.registerAppEntityTaskParam(
                                "required",
                                listener,
                                TypeConverters::toEntityValue,
                                TypeConverters::toEntity,
                                getTrivialSearchActionConverter(),
                            )
                        }
                        builder.build()
                    },
                sessionUpdaterSupplier = ::EmptyTaskUpdater,
            )
        val session = capability.createSession(hostProperties)

        // TURN 1.
        val onSuccessInvoked: SettableFutureWrapper<Boolean> = SettableFutureWrapper()
        session.execute(
            buildRequestArgs(SYNC, "required", buildSearchActionParamValue("invalid")),
            buildActionCallback(onSuccessInvoked),
        )
        assertThat(onSuccessInvoked.getFuture().get(CB_TIMEOUT, MILLISECONDS)).isTrue()
        assertThat(session.state)
            .isEqualTo(
                AppDialogState.newBuilder()
                    .setFulfillmentIdentifier("id")
                    .addParams(
                        DialogParameter.newBuilder()
                            .setName("required")
                            .addCurrentValue(
                                CurrentValue.newBuilder()
                                    .setValue(
                                        buildSearchActionParamValue("invalid"),
                                    )
                                    .setStatus(
                                        CurrentValue.Status.DISAMBIG,
                                    )
                                    .setDisambiguationData(
                                        DisambiguationData.newBuilder()
                                            .addEntities(
                                                Entity.newBuilder()
                                                    .setIdentifier(
                                                        "valid1",
                                                    )
                                                    .setName(
                                                        "valid1",
                                                    )
                                            )
                                            .addEntities(
                                                Entity.newBuilder()
                                                    .setIdentifier(
                                                        "valid2",
                                                    )
                                                    .setName(
                                                        "valid2",
                                                    ),
                                            ),
                                    ),
                            ),
                    )
                    .build()
            )

        // TURN 2.
        val turn2SuccessInvoked: SettableFutureWrapper<Boolean> = SettableFutureWrapper()
        session.execute(
            buildRequestArgs(
                SYNC,
                "required",
                ParamValue.newBuilder().setIdentifier("valid2").setStringValue("valid2").build(),
            ),
            buildActionCallback(turn2SuccessInvoked),
        )
        assertThat(turn2SuccessInvoked.getFuture().get(CB_TIMEOUT, MILLISECONDS)).isTrue()
        assertThat(session.state)
            .isEqualTo(
                AppDialogState.newBuilder()
                    .setFulfillmentIdentifier("id")
                    .addParams(
                        DialogParameter.newBuilder()
                            .setName("required")
                            .addCurrentValue(
                                CurrentValue.newBuilder()
                                    .setValue(
                                        ParamValue.newBuilder()
                                            .setIdentifier(
                                                "valid2",
                                            )
                                            .setStringValue(
                                                "valid2",
                                            ),
                                    )
                                    .setStatus(
                                        CurrentValue.Status.ACCEPTED,
                                    ),
                            ),
                    )
                    .build()
            )
    }

    /**
     * Assistant sends grounded objects as identifier only, but we need to mark the entire value
     * struct as accepted.
     */
    @Test
    @kotlin.Throws(Exception::class)
    @Suppress("DEPRECATION") // TODO(b/269638788) migrate session state to AppDialogState message
    fun identifierOnly_refillsStruct() {
        val property: CapabilityStructFill.Property =
            CapabilityStructFill.Property.newBuilder()
                .setListItem(SimpleProperty.Builder().setRequired(true).build())
                .setAnyString(TypeProperty.Builder<StringValue>().setRequired(true).build())
                .build()
        val item1: ListItem = ListItem.newBuilder().setName("red apple").setId("item1").build()
        val item2: ListItem = ListItem.newBuilder().setName("green apple").setId("item2").build()
        val onReceivedCb: SettableFutureWrapper<ListItem> = SettableFutureWrapper()
        val onFinishListItemCb: SettableFutureWrapper<ListItem> = SettableFutureWrapper()
        val onFinishStringCb: SettableFutureWrapper<String> = SettableFutureWrapper()

        val sessionFactory =
            SessionFactory<CapabilityStructFill.Session> {
                object : CapabilityStructFill.Session {
                    override suspend fun onFinish(
                        argument: CapabilityStructFill.Argument,
                    ): ExecutionResult<Void> {
                        val listItem: ListItem = argument.listItem().orElse(null)
                        val string: String = argument.anyString().orElse(null)
                        onFinishListItemCb.set(listItem)
                        onFinishStringCb.set(string)
                        return ExecutionResult.getDefaultInstance<Void>()
                    }

                    override fun getListItemListener() =
                        object : AppEntityResolver<ListItem> {
                            override fun onReceivedAsync(
                                value: ListItem,
                            ): ListenableFuture<ValidationResult> {
                                onReceivedCb.set(value)
                                return Futures.immediateFuture(ValidationResult.newAccepted())
                            }

                            override fun lookupAndRender(
                                searchAction: SearchAction<ListItem>,
                            ): ListenableFuture<EntitySearchResult<ListItem>> =
                                Futures.immediateFuture(
                                    EntitySearchResult.Builder<ListItem>()
                                        .addPossibleValue(item1)
                                        .addPossibleValue(item2)
                                        .build(),
                                )
                        }
                }
            }
        val sessionBridge =
            SessionBridge<CapabilityStructFill.Session, Void> { session ->
                TaskHandler.Builder<Void>()
                    .registerAppEntityTaskParam(
                        "listItem",
                        session.getListItemListener(),
                        TypeConverters::toListItem,
                        TypeConverters::toEntity,
                        getTrivialSearchActionConverter(),
                    )
                    .build()
            }

        val capability: ActionCapability =
            TaskCapabilityImpl(
                "selectListItem",
                CapabilityStructFill.ACTION_SPEC,
                property,
                sessionFactory,
                sessionBridge,
                ::EmptyTaskUpdater,
            )
        val session = capability.createSession(hostProperties)

        // first sync request
        val firstTurnSuccess: SettableFutureWrapper<Boolean> = SettableFutureWrapper()
        session.execute(
            buildRequestArgs(SYNC, "listItem", buildSearchActionParamValue("apple")),
            buildActionCallback(firstTurnSuccess),
        )
        assertThat(firstTurnSuccess.getFuture().get(CB_TIMEOUT, MILLISECONDS)).isTrue()
        assertThat(onReceivedCb.getFuture().isDone()).isFalse()
        assertThat(onFinishListItemCb.getFuture().isDone()).isFalse()
        assertThat(session.state)
            .isEqualTo(
                AppDialogState.newBuilder()
                    .setFulfillmentIdentifier("selectListItem")
                    .addParams(
                        DialogParameter.newBuilder()
                            .setName("listItem")
                            .addCurrentValue(
                                CurrentValue.newBuilder()
                                    .setValue(
                                        buildSearchActionParamValue(
                                            "apple",
                                        ),
                                    )
                                    .setStatus(CurrentValue.Status.DISAMBIG)
                                    .setDisambiguationData(
                                        DisambiguationData.newBuilder()
                                            .addEntities(
                                                TypeConverters.toEntity(
                                                    item1,
                                                ),
                                            )
                                            .addEntities(
                                                TypeConverters.toEntity(
                                                    item2,
                                                ),
                                            )
                                            .build(),
                                    )
                                    .build(),
                            )
                            .build(),
                    )
                    .addParams(DialogParameter.newBuilder().setName("string").build())
                    .build()
            )

        // second sync request, sending grounded ParamValue with identifier only
        val secondTurnSuccess: SettableFutureWrapper<Boolean> = SettableFutureWrapper()
        session.execute(
            buildRequestArgs(
                SYNC,
                "listItem",
                ParamValue.newBuilder().setIdentifier("item2").build(),
            ),
            buildActionCallback(secondTurnSuccess),
        )
        assertThat(secondTurnSuccess.getFuture().get(CB_TIMEOUT, MILLISECONDS)).isTrue()
        assertThat(onReceivedCb.getFuture().get(CB_TIMEOUT, MILLISECONDS)).isEqualTo(item2)
        assertThat(onFinishListItemCb.getFuture().isDone()).isFalse()

        // third sync request, sending grounded ParamValue with identifier only, completes task
        val thirdTurnSuccess: SettableFutureWrapper<Boolean> = SettableFutureWrapper()
        session.execute(
            buildRequestArgs(
                SYNC,
                "listItem",
                ParamValue.newBuilder().setIdentifier("item2").build(),
                "string",
                "unused",
            ),
            buildActionCallback(thirdTurnSuccess),
        )
        assertThat(thirdTurnSuccess.getFuture().get(CB_TIMEOUT, MILLISECONDS)).isTrue()
        assertThat(onFinishListItemCb.getFuture().get(CB_TIMEOUT, MILLISECONDS))
            .isEqualTo(
                item2,
            )
        assertThat(
                onFinishStringCb.getFuture().get(CB_TIMEOUT, MILLISECONDS),
            )
            .isEqualTo("unused")
    }

    @Test
    @kotlin.Throws(Exception::class)
    fun executionResult_resultReturned() {
        val sessionFactory =
            SessionFactory<Session> {
                object : Session {
                    override suspend fun onFinish(argument: Argument) =
                        ExecutionResult.Builder<Output>()
                            .setOutput(
                                Output.builder()
                                    .setOptionalStringField("bar")
                                    .setRepeatedStringField(
                                        listOf("bar1", "bar2"),
                                    )
                                    .build(),
                            )
                            .build()
                }
            }
        val capability =
            CapabilityBuilder().setId("fakeId").setSessionFactory(sessionFactory).build()
        val session = capability.createSession(hostProperties)
        val onSuccessInvoked: SettableFutureWrapper<FulfillmentResponse> = SettableFutureWrapper()
        val expectedOutput: StructuredOutput =
            StructuredOutput.newBuilder()
                .addOutputValues(
                    OutputValue.newBuilder()
                        .setName("optionalStringOutput")
                        .addValues(
                            ParamValue.newBuilder().setStringValue("bar").build(),
                        )
                        .build(),
                )
                .addOutputValues(
                    OutputValue.newBuilder()
                        .setName("repeatedStringOutput")
                        .addValues(
                            ParamValue.newBuilder().setStringValue("bar1").build(),
                        )
                        .addValues(
                            ParamValue.newBuilder().setStringValue("bar2").build(),
                        )
                        .build(),
                )
                .build()
        session.execute(
            buildRequestArgs(
                SYNC, /* args...= */
                "required",
                ParamValue.newBuilder().setIdentifier("foo").setStringValue("foo").build(),
            ),
            buildActionCallbackWithFulfillmentResponse(onSuccessInvoked),
        )
        assertThat(
                onSuccessInvoked
                    .getFuture()
                    .get(CB_TIMEOUT, MILLISECONDS)
                    .getExecutionOutput()
                    .getOutputValuesList(),
            )
            .containsExactlyElementsIn(expectedOutput.getOutputValuesList())
    }

    /**
     * an implementation of CapabilityBuilderBase using Argument. Output, etc. defined under
     * testing/spec
     */
    class CapabilityBuilder :
        CapabilityBuilderBase<
            CapabilityBuilder,
            Property,
            Argument,
            Output,
            Confirmation,
            RequiredTaskUpdater,
            Session,
        >(ACTION_SPEC) {

        init {
            setProperty(SINGLE_REQUIRED_FIELD_PROPERTY)
        }
        override val sessionBridge: SessionBridge<Session, Confirmation> = SessionBridge {
            TaskHandler.Builder<Confirmation>().build()
        }
        override val sessionUpdaterSupplier: Supplier<RequiredTaskUpdater> = Supplier {
            RequiredTaskUpdater()
        }

        public override fun setSessionFactory(
            sessionFactory: SessionFactory<Session>,
        ): CapabilityBuilder = super.setSessionFactory(sessionFactory)
    }

    companion object {
        private val DISAMBIG_ENTITY_CONVERTER: DisambigEntityConverter<EntityValue> =
            DisambigEntityConverter {
                TypeConverters.toEntity(it)
            }

        private val AUTO_ACCEPT_ENTITY_VALUE: AppEntityResolver<EntityValue> =
            object : AppEntityResolver<EntityValue> {
                override fun lookupAndRender(
                    searchAction: SearchAction<EntityValue>,
                ): ListenableFuture<EntitySearchResult<EntityValue>> {
                    val result: EntitySearchResult.Builder<EntityValue> =
                        EntitySearchResult.Builder()
                    return Futures.immediateFuture(
                        result.addPossibleValue(EntityValue.ofId("valid1")).build(),
                    )
                }

                override fun onReceivedAsync(
                    value: EntityValue,
                ): ListenableFuture<ValidationResult> {
                    return Futures.immediateFuture(ValidationResult.newAccepted())
                }
            }
        private val AUTO_REJECT_ENTITY_VALUE: AppEntityResolver<EntityValue> =
            object : AppEntityResolver<EntityValue> {
                override fun lookupAndRender(
                    searchAction: SearchAction<EntityValue>,
                ): ListenableFuture<EntitySearchResult<EntityValue>> {
                    val result: EntitySearchResult.Builder<EntityValue> =
                        EntitySearchResult.Builder()
                    return Futures.immediateFuture(
                        result.addPossibleValue(EntityValue.ofId("valid1")).build(),
                    )
                }

                override fun onReceivedAsync(
                    value: EntityValue,
                ): ListenableFuture<ValidationResult> {
                    return Futures.immediateFuture(ValidationResult.newRejected())
                }
            }

        private fun <T> getTrivialSearchActionConverter() = SearchActionConverter {
            SearchAction.newBuilder<T>().build()
        }

        private const val CAPABILITY_NAME = "actions.intent.TEST"
        private val ACTION_SPEC: ActionSpec<Property, Argument, Output> =
            ActionSpecBuilder.ofCapabilityNamed(
                    CAPABILITY_NAME,
                )
                .setDescriptor(Property::class.java)
                .setArgument(Argument::class.java, Argument::newBuilder)
                .setOutput(Output::class.java)
                .bindRequiredEntityParameter(
                    "required",
                    Property::requiredEntityField,
                    Argument.Builder::setRequiredEntityField,
                )
                .bindOptionalStringParameter(
                    "optional",
                    Property::optionalStringField,
                    Argument.Builder::setOptionalStringField,
                )
                .bindOptionalGenericParameter(
                    "optionalEnum",
                    Property::enumField,
                    Argument.Builder::setEnumField,
                    { TestEnum.VALUE_1 },
                    { Entity.newBuilder().setIdentifier(it.toString()).build() }
                )
                .bindRepeatedStringParameter(
                    "repeated",
                    Property::repeatedStringField,
                    Argument.Builder::setRepeatedStringField,
                )
                .bindOptionalOutput(
                    "optionalStringOutput",
                    Output::optionalStringField,
                    TypeConverters::toParamValue,
                )
                .bindRepeatedOutput(
                    "repeatedStringOutput",
                    Output::repeatedStringField,
                    TypeConverters::toParamValue,
                )
                .build()

        private val SINGLE_REQUIRED_FIELD_PROPERTY: Property =
            Property.newBuilder()
                .setRequiredEntityField(
                    TypeProperty.Builder<
                            androidx.appactions.interaction.capabilities.core.properties.Entity
                        >()
                        .setRequired(true)
                        .build()
                )
                .build()

        private fun groundingPredicate(paramValue: ParamValue): Boolean {
            return !paramValue.hasIdentifier()
        }

        private fun getCurrentValues(
            argName: String,
            appDialogState: AppDialogState
        ): List<CurrentValue> {
            return appDialogState
                .getParamsList()
                .stream()
                .filter { dialogParam -> dialogParam.getName().equals(argName) }
                .findFirst()
                .orElse(DialogParameter.getDefaultInstance())
                .getCurrentValueList()
        }

        /**
         * Create a capability instance templated with Property, Argument, Output, Confirmation
         * etc., defined under ../../testing/spec
         */
        private fun <SessionUpdaterT : AbstractTaskUpdater> createCapability(
            property: Property,
            sessionFactory: SessionFactory<Session>,
            sessionBridge: SessionBridge<Session, Confirmation>,
            sessionUpdaterSupplier: Supplier<SessionUpdaterT>,
        ): TaskCapabilityImpl<
            Property,
            Argument,
            Output,
            Session,
            Confirmation,
            SessionUpdaterT,
        > {
            return TaskCapabilityImpl(
                "id",
                ACTION_SPEC,
                property,
                sessionFactory,
                sessionBridge,
                sessionUpdaterSupplier,
            )
        }
    }
}
