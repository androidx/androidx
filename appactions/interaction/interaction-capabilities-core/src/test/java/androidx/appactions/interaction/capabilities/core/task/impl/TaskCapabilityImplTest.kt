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
package androidx.appactions.interaction.capabilities.core.impl.task

import android.util.SizeF
import androidx.appactions.builtintypes.experimental.types.ListItem
import androidx.appactions.interaction.capabilities.core.AppEntityListener
import androidx.appactions.interaction.capabilities.core.Capability
import androidx.appactions.interaction.capabilities.core.EntitySearchResult
import androidx.appactions.interaction.capabilities.core.ExecutionResult
import androidx.appactions.interaction.capabilities.core.ExecutionSessionFactory
import androidx.appactions.interaction.capabilities.core.HostProperties
import androidx.appactions.interaction.capabilities.core.SessionContext
import androidx.appactions.interaction.capabilities.core.ValidationResult
import androidx.appactions.interaction.capabilities.core.ValueListener
import androidx.appactions.interaction.capabilities.core.impl.ErrorStatusInternal
import androidx.appactions.interaction.capabilities.core.impl.UiHandleRegistry
import androidx.appactions.interaction.capabilities.core.impl.concurrent.Futures
import androidx.appactions.interaction.capabilities.core.impl.converters.EntityConverter
import androidx.appactions.interaction.capabilities.core.impl.converters.ParamValueConverter
import androidx.appactions.interaction.capabilities.core.impl.converters.SearchActionConverter
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters.LIST_ITEM_TYPE_SPEC
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpec
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecBuilder
import androidx.appactions.interaction.capabilities.core.properties.StringValue
import androidx.appactions.interaction.capabilities.core.properties.Property
import androidx.appactions.interaction.capabilities.core.testing.spec.Arguments
import androidx.appactions.interaction.capabilities.core.testing.spec.CapabilityStructFill
import androidx.appactions.interaction.capabilities.core.testing.spec.CapabilityTwoEntityValues
import androidx.appactions.interaction.capabilities.core.testing.spec.Confirmation
import androidx.appactions.interaction.capabilities.core.testing.spec.ExecutionSession
import androidx.appactions.interaction.capabilities.core.testing.spec.Output
import androidx.appactions.interaction.capabilities.core.testing.spec.TestEnum
import androidx.appactions.interaction.capabilities.core.testing.spec.Properties
import androidx.appactions.interaction.capabilities.core.values.EntityValue
import androidx.appactions.interaction.capabilities.core.values.SearchAction
import androidx.appactions.interaction.capabilities.testing.internal.ArgumentUtils.buildRequestArgs
import androidx.appactions.interaction.capabilities.testing.internal.ArgumentUtils.buildSearchActionParamValue
import androidx.appactions.interaction.capabilities.testing.internal.FakeCallbackInternal
import androidx.appactions.interaction.capabilities.testing.internal.TestingUtils.awaitSync
import androidx.appactions.interaction.proto.AppActionsContext.AppAction
import androidx.appactions.interaction.proto.AppActionsContext.AppDialogState
import androidx.appactions.interaction.proto.AppActionsContext.DialogParameter
import androidx.appactions.interaction.proto.AppActionsContext.IntentParameter
import androidx.appactions.interaction.proto.CurrentValue
import androidx.appactions.interaction.proto.DisambiguationData
import androidx.appactions.interaction.proto.Entity
import androidx.appactions.interaction.proto.FulfillmentRequest.Fulfillment.Type.CANCEL
import androidx.appactions.interaction.proto.FulfillmentRequest.Fulfillment.Type.SYNC
import androidx.appactions.interaction.proto.FulfillmentRequest.Fulfillment.Type.UNKNOWN_TYPE
import androidx.appactions.interaction.proto.FulfillmentResponse.StructuredOutput
import androidx.appactions.interaction.proto.FulfillmentResponse.StructuredOutput.OutputValue
import androidx.appactions.interaction.proto.ParamValue
import androidx.appactions.interaction.proto.TaskInfo
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.concurrent.futures.CallbackToFutureAdapter.Completer
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Supplier

@RunWith(JUnit4::class)
class TaskCapabilityImplTest {
    private val capability: Capability =
        createCapability<EmptyTaskUpdater>(
            SINGLE_REQUIRED_FIELD_PROPERTY,
            sessionFactory =
            {
                object : ExecutionSession {
                    override fun onExecuteAsync(arguments: Arguments) =
                        Futures.immediateFuture(ExecutionResult.Builder<Output>().build())
                }
            },
            sessionBridge = { TaskHandler.Builder<Confirmation>().build() },
            sessionUpdaterSupplier = ::EmptyTaskUpdater,
        )
    private val hostProperties: HostProperties =
        HostProperties.Builder()
            .setMaxHostSizeDp(
                SizeF(300f, 500f),
            )
            .build()
    private val fakeSessionId = "fakeSessionId"

    @Test
    fun appAction_smokeTest() {
        assertThat(capability.appAction)
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
    fun appAction_computedProperty() {
        val mutableEntityList = mutableListOf<
            androidx.appactions.interaction.capabilities.core.properties.Entity
        >()
        val capability = createCapability<EmptyTaskUpdater>(
            Properties.newBuilder()
                .setRequiredEntityField(
                    Property.Builder<
                        androidx.appactions.interaction.capabilities.core.properties.Entity
                    >().setPossibleValueSupplier(
                        mutableEntityList::toList
                    ).build()
                )
                .build(),
            sessionFactory =
            {
                object : ExecutionSession {
                    override fun onExecuteAsync(arguments: Arguments) =
                        Futures.immediateFuture(ExecutionResult.Builder<Output>().build())
                }
            },
            sessionBridge = { TaskHandler.Builder<Confirmation>().build() },
            sessionUpdaterSupplier = ::EmptyTaskUpdater,
        )
        mutableEntityList.add(
            androidx.appactions.interaction.capabilities.core.properties.Entity.Builder()
                .setName("entity1").build()
        )

        assertThat(capability.appAction).isEqualTo(
            AppAction.newBuilder()
                .setIdentifier("id")
                .setName("actions.intent.TEST")
                .addParams(
                    IntentParameter.newBuilder()
                        .setName("required")
                        .addPossibleEntities(Entity.newBuilder().setName("entity1"))
                )
                .setTaskInfo(TaskInfo.newBuilder().setSupportsPartialFulfillment(true))
                .build()
        )

        mutableEntityList.add(
            androidx.appactions.interaction.capabilities.core.properties.Entity.Builder()
                .setName("entity2").build()
        )
        assertThat(capability.appAction).isEqualTo(
            AppAction.newBuilder()
                .setIdentifier("id")
                .setName("actions.intent.TEST")
                .addParams(
                    IntentParameter.newBuilder()
                        .setName("required")
                        .addPossibleEntities(Entity.newBuilder().setName("entity1"))
                        .addPossibleEntities(Entity.newBuilder().setName("entity2"))
                )
                .setTaskInfo(TaskInfo.newBuilder().setSupportsPartialFulfillment(true))
                .build()
        )
    }

    @Test
    fun capabilitySession_getUiHandle() {
        val externalSession = object : ExecutionSession {}
        val capability =
            createCapability(
                SINGLE_REQUIRED_FIELD_PROPERTY,
                { externalSession },
                { TaskHandler.Builder<Confirmation>().build() },
                ::EmptyTaskUpdater,
            )
        val session = capability.createSession(fakeSessionId, hostProperties)
        assertThat(session.uiHandle).isSameInstanceAs(externalSession)
    }

    @Test
    @kotlin.Throws(Exception::class)
    fun onCreateInvoked_invokedOnce() {
        val onCreateInvocationCount = AtomicInteger(0)
        val capability: Capability =
            createCapability(
                SINGLE_REQUIRED_FIELD_PROPERTY,
                sessionFactory =
                ExecutionSessionFactory {
                    object : ExecutionSession {
                        override fun onCreate(sessionContext: SessionContext) {
                            onCreateInvocationCount.incrementAndGet()
                        }

                        override fun onExecuteAsync(arguments: Arguments) =
                            Futures.immediateFuture(
                                ExecutionResult.Builder<Output>().build(),
                            )
                    }
                },
                sessionBridge = SessionBridge { TaskHandler.Builder<Confirmation>().build() },
                sessionUpdaterSupplier = ::EmptyTaskUpdater,
            )
        val session = capability.createSession(fakeSessionId, hostProperties)

        // TURN 1.
        val callback = FakeCallbackInternal()
        session.execute(
            buildRequestArgs(SYNC, "unknownArgName", "foo"),
            callback,
        )
        assertThat(callback.receiveResponse().fulfillmentResponse).isNotNull()
        assertThat(onCreateInvocationCount.get()).isEqualTo(1)

        // TURN 2.
        val callback2 = FakeCallbackInternal()
        session.execute(
            buildRequestArgs(
                SYNC,
                "required",
                ParamValue.newBuilder().setIdentifier("foo").setStringValue("foo").build(),
            ),
            callback2,
        )
        assertThat(callback2.receiveResponse().fulfillmentResponse).isNotNull()
        assertThat(onCreateInvocationCount.get()).isEqualTo(1)
    }

    class RequiredTaskUpdater : AbstractTaskUpdater() {
        fun setRequiredEntityValue(entityValue: EntityValue) {
            super.updateParamValues(
                mapOf(
                    "required" to
                        listOf(
                            TypeConverters.ENTITY_PARAM_VALUE_CONVERTER.toParamValue(entityValue),
                        ),
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
                oldCompleter?.setCancelled()
                "waiting for setValidationResult"
            }
        }
    }

    @Test
    fun duringExecution_uiHandleRegistered(): Unit = runBlocking {
        val onExecuteReached = CompletableDeferred<Unit>()
        val onExecuteResult = CompletableDeferred<ExecutionResult<Output>>()
        val externalSession = object : ExecutionSession {
            override suspend fun onExecute(arguments: Arguments): ExecutionResult<Output> {
                onExecuteReached.complete(Unit)
                return onExecuteResult.await()
            }
        }
        val capability: Capability = createCapability(
            SINGLE_REQUIRED_FIELD_PROPERTY,
            sessionFactory = ExecutionSessionFactory { externalSession },
            sessionBridge = SessionBridge { TaskHandler.Builder<Confirmation>().build() },
            sessionUpdaterSupplier = ::RequiredTaskUpdater,
        )
        val session = capability.createSession("mySessionId", hostProperties)
        val callback = FakeCallbackInternal()
        session.execute(
            buildRequestArgs(
                SYNC,
                "required",
                "hello",
            ),
            callback,
        )
        onExecuteReached.await()
        assertThat(UiHandleRegistry.getSessionIdFromUiHandle(externalSession)).isEqualTo(
            "mySessionId",
        )

        onExecuteResult.complete(ExecutionResult.Builder<Output>().build())
        assertThat(callback.receiveResponse().fulfillmentResponse).isNotNull()
        assertThat(UiHandleRegistry.getSessionIdFromUiHandle(externalSession)).isNull()
    }

    @Test
    @kotlin.Throws(Exception::class)
    fun fulfillmentType_unknown_errorReported() {
        val capability: Capability =
            createCapability(
                SINGLE_REQUIRED_FIELD_PROPERTY,
                sessionFactory =
                ExecutionSessionFactory {
                    object : ExecutionSession {
                        override fun onExecuteAsync(arguments: Arguments) =
                            Futures.immediateFuture(
                                ExecutionResult.Builder<Output>().build(),
                            )
                    }
                },
                sessionBridge = SessionBridge { TaskHandler.Builder<Confirmation>().build() },
                sessionUpdaterSupplier = ::RequiredTaskUpdater,
            )
        val session = capability.createSession(fakeSessionId, hostProperties)

        assertThat(capability.appAction)
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
        val callback = FakeCallbackInternal()
        session.execute(buildRequestArgs(UNKNOWN_TYPE), callback)
        assertThat(callback.receiveResponse().errorStatus)
            .isEqualTo(ErrorStatusInternal.INVALID_REQUEST_TYPE)
    }

    @Test
    fun slotFilling_isActive_smokeTest() {
        val property: CapabilityTwoEntityValues.Properties =
            CapabilityTwoEntityValues.Properties.newBuilder()
                .setSlotA(
                    Property.Builder<
                        androidx.appactions.interaction.capabilities.core.properties.Entity,
                        >()
                        .setRequired(true)
                        .build(),
                )
                .setSlotB(
                    Property.Builder<
                        androidx.appactions.interaction.capabilities.core.properties.Entity,
                        >()
                        .setRequired(true)
                        .build(),
                )
                .build()
        val sessionFactory =
            ExecutionSessionFactory<CapabilityTwoEntityValues.ExecutionSession> {
                object : CapabilityTwoEntityValues.ExecutionSession {
                    override suspend fun onExecute(
                        arguments: CapabilityTwoEntityValues.Arguments,
                    ): ExecutionResult<Void> = ExecutionResult.Builder<Void>().build()
                }
            }
        val sessionBridge =
            SessionBridge<CapabilityTwoEntityValues.ExecutionSession, Void> {
                TaskHandler.Builder<Void>()
                    .registerValueTaskParam(
                        "slotA",
                        AUTO_ACCEPT_ENTITY_VALUE,
                        TypeConverters.ENTITY_PARAM_VALUE_CONVERTER,
                    )
                    .registerValueTaskParam(
                        "slotB",
                        AUTO_ACCEPT_ENTITY_VALUE,
                        TypeConverters.ENTITY_PARAM_VALUE_CONVERTER,
                    )
                    .build()
            }
        val capability: Capability =
            TaskCapabilityImpl(
                "fakeId",
                CapabilityTwoEntityValues.ACTION_SPEC,
                property,
                sessionFactory,
                sessionBridge,
                ::EmptyTaskUpdater,
            )

        val session = capability.createSession(fakeSessionId, hostProperties)
        assertThat(session.isActive).isTrue()

        // turn 1
        val callback = FakeCallbackInternal()
        session.execute(
            buildRequestArgs(
                SYNC,
                "slotA",
                ParamValue.newBuilder().setIdentifier("foo").setStringValue("foo").build(),
            ),
            callback,
        )
        assertThat(callback.receiveResponse().fulfillmentResponse).isNotNull()
        assertThat(session.isActive).isTrue()

        // turn 2
        val callback2 = FakeCallbackInternal()
        session.execute(
            buildRequestArgs(
                SYNC,
                "slotA",
                ParamValue.newBuilder().setIdentifier("foo").setStringValue("foo").build(),
                "slotB",
                ParamValue.newBuilder().setIdentifier("bar").setStringValue("bar").build(),
            ),
            callback2,
        )
        assertThat(callback2.receiveResponse().fulfillmentResponse).isNotNull()
        assertThat(session.isActive).isFalse()

        // turn 3
        val callback3 = FakeCallbackInternal()
        session.execute(
            buildRequestArgs(CANCEL),
            callback3,
        )
        assertThat(callback3.receiveResponse().fulfillmentResponse).isNotNull()
        assertThat(session.isActive).isFalse()
    }

    @Test
    @kotlin.Throws(Exception::class)
    fun slotFilling_optionalButRejectedParam_onFinishNotInvoked() {
        val onExecuteInvocationCount = AtomicInteger(0)
        val property: CapabilityTwoEntityValues.Properties =
            CapabilityTwoEntityValues.Properties.newBuilder()
                .setSlotA(
                    Property.Builder<
                        androidx.appactions.interaction.capabilities.core.properties.Entity,
                        >()
                        .setRequired(true)
                        .build(),
                )
                .setSlotB(
                    Property.Builder<
                        androidx.appactions.interaction.capabilities.core.properties.Entity,
                        >()
                        .setRequired(false)
                        .build(),
                )
                .build()
        val sessionFactory =
            ExecutionSessionFactory<CapabilityTwoEntityValues.ExecutionSession> {
                object : CapabilityTwoEntityValues.ExecutionSession {
                    override suspend fun onExecute(
                        arguments: CapabilityTwoEntityValues.Arguments,
                    ): ExecutionResult<Void> {
                        onExecuteInvocationCount.incrementAndGet()
                        return ExecutionResult.Builder<Void>().build()
                    }
                }
            }
        val sessionBridge =
            SessionBridge<CapabilityTwoEntityValues.ExecutionSession, Void> {
                TaskHandler.Builder<Void>()
                    .registerValueTaskParam(
                        "slotA",
                        AUTO_ACCEPT_ENTITY_VALUE,
                        TypeConverters.ENTITY_PARAM_VALUE_CONVERTER,
                    )
                    .registerValueTaskParam(
                        "slotB",
                        AUTO_REJECT_ENTITY_VALUE,
                        TypeConverters.ENTITY_PARAM_VALUE_CONVERTER,
                    )
                    .build()
            }
        val capability: Capability =
            TaskCapabilityImpl(
                "fakeId",
                CapabilityTwoEntityValues.ACTION_SPEC,
                property,
                sessionFactory,
                sessionBridge,
                ::EmptyTaskUpdater,
            )
        val session = capability.createSession(fakeSessionId, hostProperties)

        // TURN 1.
        val callback = FakeCallbackInternal()
        session.execute(
            buildRequestArgs(
                SYNC,
                "slotA",
                ParamValue.newBuilder().setIdentifier("foo").setStringValue("foo").build(),
                "slotB",
                ParamValue.newBuilder().setIdentifier("bar").setStringValue("bar").build(),
            ),
            callback,
        )
        assertThat(callback.receiveResponse().fulfillmentResponse).isNotNull()
        assertThat(onExecuteInvocationCount.get()).isEqualTo(0)
        assertThat(getCurrentValues("slotA", session.state!!))
            .containsExactly(
                CurrentValue.newBuilder()
                    .setValue(
                        ParamValue.newBuilder().setIdentifier("foo").setStringValue("foo"),
                    )
                    .setStatus(CurrentValue.Status.ACCEPTED)
                    .build(),
            )
        assertThat(getCurrentValues("slotB", session.state!!))
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
        val property: Properties =
            Properties.newBuilder()
                .setRequiredEntityField(
                    Property.Builder<
                        androidx.appactions.interaction.capabilities.core.properties.Entity,
                        >()
                        .setRequired(true)
                        .build(),
                )
                .setEnumField(
                    Property.Builder<TestEnum>()
                        .setPossibleValues(TestEnum.VALUE_1, TestEnum.VALUE_2)
                        .setRequired(true)
                        .build(),
                )
                .build()
        val capability: Capability =
            createCapability(
                property,
                sessionFactory = ExecutionSessionFactory { ExecutionSession.DEFAULT },
                sessionBridge = SessionBridge { TaskHandler.Builder<Confirmation>().build() },
                sessionUpdaterSupplier = ::EmptyTaskUpdater,
            )
        val session = capability.createSession(fakeSessionId, hostProperties)

        // TURN 1.
        val callback = FakeCallbackInternal()
        session.execute(
            buildRequestArgs(
                SYNC,
                "required",
                ParamValue.newBuilder().setIdentifier("foo").setStringValue("foo").build(),
            ),
            callback,
        )
        assertThat(callback.receiveResponse()).isNotNull()
        assertThat(getCurrentValues("required", session.state!!))
            .containsExactly(
                CurrentValue.newBuilder()
                    .setValue(
                        ParamValue.newBuilder().setIdentifier("foo").setStringValue("foo"),
                    )
                    .setStatus(CurrentValue.Status.ACCEPTED)
                    .build(),
            )
        assertThat(getCurrentValues("optionalEnum", session.state!!)).isEmpty()

        // TURN 2.
        val callback2 = FakeCallbackInternal()
        session.execute(
            buildRequestArgs(SYNC, "optionalEnum", TestEnum.VALUE_2),
            callback2,
        )
        assertThat(callback2.receiveResponse().fulfillmentResponse).isNotNull()
        assertThat(getCurrentValues("required", session.state!!)).isEmpty()
        assertThat(getCurrentValues("optionalEnum", session.state!!))
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
        val entityConverter: EntityConverter<EntityValue> = EntityConverter { entityValue ->
            Entity.newBuilder()
                .setIdentifier(entityValue.id.get())
                .setName(entityValue.value)
                .build()
        }
        val capability: Capability =
            createCapability(
                SINGLE_REQUIRED_FIELD_PROPERTY,
                sessionFactory = {
                    object : ExecutionSession {
                        override suspend fun onExecute(arguments: Arguments) =
                            ExecutionResult.Builder<Output>().build()

                        override fun getRequiredEntityListener() =
                            object : AppEntityListener<EntityValue> {
                                override fun lookupAndRenderAsync(
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
                SessionBridge<ExecutionSession, Confirmation> { session ->
                    val builder = TaskHandler.Builder<Confirmation>()
                    session.getRequiredEntityListener()
                        ?.let { listener: AppEntityListener<EntityValue> ->
                            builder.registerAppEntityTaskParam(
                                "required",
                                listener,
                                TypeConverters.ENTITY_PARAM_VALUE_CONVERTER,
                                entityConverter,
                                getTrivialSearchActionConverter(),
                            )
                        }
                    builder.build()
                },
                sessionUpdaterSupplier = ::EmptyTaskUpdater,
            )
        val session = capability.createSession(fakeSessionId, hostProperties)

        // TURN 1.
        val callback = FakeCallbackInternal()
        session.execute(
            buildRequestArgs(SYNC, "required", buildSearchActionParamValue("invalid")),
            callback,
        )
        assertThat(callback.receiveResponse().fulfillmentResponse).isNotNull()
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
                                                    ),
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
                    ).build(),
            )

        // TURN 2.
        val callback2 = FakeCallbackInternal()
        session.execute(
            buildRequestArgs(
                SYNC,
                "required",
                ParamValue.newBuilder().setIdentifier("valid2").setStringValue("valid2").build(),
            ),
            callback2,
        )
        assertThat(callback2.receiveResponse().fulfillmentResponse).isNotNull()
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
                    .build(),
            )
    }

    /**
     * Assistant sends grounded objects as identifier only, but we need to mark the entire value
     * struct as accepted.
     */
    @Test
    @kotlin.Throws(Exception::class)
    @Suppress("DEPRECATION") // TODO(b/269638788) migrate session state to AppDialogState message
    fun identifierOnly_refillsStruct() = runBlocking<Unit> {
        val property: CapabilityStructFill.Properties =
            CapabilityStructFill.Properties.newBuilder()
                .setListItem(Property.Builder<ListItem>().setRequired(true).build())
                .setAnyString(Property.Builder<StringValue>().setRequired(true).build())
                .build()
        val item1: ListItem = ListItem.Builder().setName("red apple").setIdentifier("item1").build()
        val item2: ListItem =
            ListItem.Builder().setName("green apple").setIdentifier("item2").build()
        val onReceivedDeferred = CompletableDeferred<ListItem>()
        val onExecuteListItemDeferred = CompletableDeferred<ListItem>()
        val onExecuteStringDeferred = CompletableDeferred<String>()

        val sessionFactory =
            ExecutionSessionFactory<CapabilityStructFill.ExecutionSession> {
                object : CapabilityStructFill.ExecutionSession {
                    override suspend fun onExecute(
                        arguments: CapabilityStructFill.Arguments,
                    ): ExecutionResult<Void> {
                        val listItem: ListItem = arguments.listItem().orElse(null)
                        val string: String = arguments.anyString().orElse(null)
                        onExecuteListItemDeferred.complete(listItem)
                        onExecuteStringDeferred.complete(string)
                        return ExecutionResult.Builder<Void>().build()
                    }

                    override fun getListItemListener() =
                        object : AppEntityListener<ListItem> {
                            override fun onReceivedAsync(
                                value: ListItem,
                            ): ListenableFuture<ValidationResult> {
                                onReceivedDeferred.complete(value)
                                return Futures.immediateFuture(ValidationResult.newAccepted())
                            }

                            override fun lookupAndRenderAsync(
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
            SessionBridge<CapabilityStructFill.ExecutionSession, Void> { session ->
                TaskHandler.Builder<Void>()
                    .registerAppEntityTaskParam(
                        "listItem",
                        session.getListItemListener(),
                        ParamValueConverter.of(LIST_ITEM_TYPE_SPEC),
                        EntityConverter.of(LIST_ITEM_TYPE_SPEC)::convert,
                        getTrivialSearchActionConverter(),
                    )
                    .build()
            }

        val capability: Capability =
            TaskCapabilityImpl(
                "selectListItem",
                CapabilityStructFill.ACTION_SPEC,
                property,
                sessionFactory,
                sessionBridge,
                ::EmptyTaskUpdater,
            )
        val session = capability.createSession(fakeSessionId, hostProperties)

        // first sync request
        val callback = FakeCallbackInternal()
        session.execute(
            buildRequestArgs(SYNC, "listItem", buildSearchActionParamValue("apple")),
            callback,
        )
        assertThat(callback.receiveResponse().fulfillmentResponse).isNotNull()
        assertThat(onReceivedDeferred.isCompleted).isFalse()
        assertThat(onExecuteListItemDeferred.isCompleted).isFalse()
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
                                                EntityConverter.of(LIST_ITEM_TYPE_SPEC)
                                                    .convert(item1),
                                            )
                                            .addEntities(
                                                EntityConverter.of(LIST_ITEM_TYPE_SPEC)
                                                    .convert(item2),
                                            )
                                            .build(),
                                    )
                                    .build(),
                            )
                            .build(),
                    )
                    .addParams(DialogParameter.newBuilder().setName("string").build())
                    .build(),
            )

        // second sync request, sending grounded ParamValue with identifier only
        val callback2 = FakeCallbackInternal()
        session.execute(
            buildRequestArgs(
                SYNC,
                "listItem",
                ParamValue.newBuilder().setIdentifier("item2").build(),
            ),
            callback2,
        )
        assertThat(callback2.receiveResponse().fulfillmentResponse).isNotNull()
        assertThat(onReceivedDeferred.awaitSync()).isEqualTo(item2)
        assertThat(onExecuteListItemDeferred.isCompleted).isFalse()

        // third sync request, sending grounded ParamValue with identifier only, completes task
        val callback3 = FakeCallbackInternal()
        session.execute(
            buildRequestArgs(
                SYNC,
                "listItem",
                ParamValue.newBuilder().setIdentifier("item2").build(),
                "string",
                "unused",
            ),
            callback3,
        )
        assertThat(callback3.receiveResponse().fulfillmentResponse).isNotNull()
        assertThat(onExecuteListItemDeferred.awaitSync()).isEqualTo(item2)
        assertThat(onExecuteStringDeferred.awaitSync()).isEqualTo("unused")
    }

    @Test
    @kotlin.Throws(Exception::class)
    fun executionResult_resultReturned() {
        val sessionFactory =
            ExecutionSessionFactory<ExecutionSession> {
                object : ExecutionSession {
                    override suspend fun onExecute(arguments: Arguments) =
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
            CapabilityBuilder().setId("fakeId").setExecutionSessionFactory(sessionFactory).build()
        val session = capability.createSession(fakeSessionId, hostProperties)
        val callback = FakeCallbackInternal()
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
            callback,
        )
        assertThat(
            callback.receiveResponse()
                .fulfillmentResponse!!
                .getExecutionOutput()
                .getOutputValuesList(),
        )
            .containsExactlyElementsIn(expectedOutput.getOutputValuesList())
    }

    @Test
    @kotlin.Throws(Exception::class)
    fun executionResult_shouldStartDictation_resultReturned() {
        val sessionFactory =
            ExecutionSessionFactory<ExecutionSession> {
                object : ExecutionSession {
                    override suspend fun onExecute(arguments: Arguments) =
                        ExecutionResult.Builder<Output>()
                            .setStartDictation(true)
                            .build()
                }
            }
        val capability =
            CapabilityBuilder().setId("fakeId").setExecutionSessionFactory(sessionFactory).build()
        val session = capability.createSession(fakeSessionId, hostProperties)
        val callback = FakeCallbackInternal()

        session.execute(
            buildRequestArgs(
                SYNC, /* args...= */
                "required",
                ParamValue.newBuilder().setIdentifier("foo").setStringValue("foo").build(),
            ),
            callback,
        )

        assertThat(callback.receiveResponse().fulfillmentResponse!!.startDictation).isTrue()
    }

    /**
     * an implementation of Capability.Builder using Argument. Output, etc. defined under
     * testing/spec
     */
    class CapabilityBuilder :
        Capability.Builder<
            CapabilityBuilder,
            Properties,
            Arguments,
            Output,
            Confirmation,
            ExecutionSession,
            >(ACTION_SPEC) {

        init {
            setProperty(SINGLE_REQUIRED_FIELD_PROPERTY)
        }

        override val sessionBridge: SessionBridge<ExecutionSession, Confirmation> = SessionBridge {
            TaskHandler.Builder<Confirmation>().build()
        }

        public override fun setExecutionSessionFactory(
            sessionFactory: ExecutionSessionFactory<ExecutionSession>,
        ): CapabilityBuilder = super.setExecutionSessionFactory(sessionFactory)
    }

    companion object {

        private val AUTO_ACCEPT_ENTITY_VALUE: AppEntityListener<EntityValue> =
            object : AppEntityListener<EntityValue> {
                override fun lookupAndRenderAsync(
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
        private val AUTO_REJECT_ENTITY_VALUE: AppEntityListener<EntityValue> =
            object : AppEntityListener<EntityValue> {
                override fun lookupAndRenderAsync(
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
        private val ENUM_CONVERTER: ParamValueConverter<TestEnum> =
            object : ParamValueConverter<TestEnum> {
                override fun fromParamValue(paramValue: ParamValue): TestEnum {
                    return TestEnum.VALUE_1
                }

                override fun toParamValue(obj: TestEnum): ParamValue {
                    return ParamValue.newBuilder().build()
                }
            }
        private val ACTION_SPEC: ActionSpec<Properties, Arguments, Output> =
            ActionSpecBuilder.ofCapabilityNamed(
                CAPABILITY_NAME,
            )
                .setDescriptor(Properties::class.java)
                .setArguments(Arguments::class.java, Arguments::newBuilder)
                .setOutput(Output::class.java)
                .bindParameter(
                    "required",
                    Properties::requiredEntityField,
                    Arguments.Builder::setRequiredEntityField,
                    TypeConverters.ENTITY_PARAM_VALUE_CONVERTER,
                    TypeConverters.ENTITY_ENTITY_CONVERTER,
                )
                .bindOptionalParameter(
                    "optional",
                    Properties::optionalStringField,
                    Arguments.Builder::setOptionalStringField,
                    TypeConverters.STRING_PARAM_VALUE_CONVERTER,
                    TypeConverters.STRING_VALUE_ENTITY_CONVERTER,
                )
                .bindOptionalParameter(
                    "optionalEnum",
                    Properties::enumField,
                    Arguments.Builder::setEnumField,
                    ENUM_CONVERTER,
                    { Entity.newBuilder().setIdentifier(it.toString()).build() },
                )
                .bindRepeatedParameter(
                    "repeated",
                    Properties::repeatedStringField,
                    Arguments.Builder::setRepeatedStringField,
                    TypeConverters.STRING_PARAM_VALUE_CONVERTER,
                    TypeConverters.STRING_VALUE_ENTITY_CONVERTER,
                )
                .bindOptionalOutput(
                    "optionalStringOutput",
                    Output::optionalStringField,
                    TypeConverters.STRING_PARAM_VALUE_CONVERTER::toParamValue,
                )
                .bindRepeatedOutput(
                    "repeatedStringOutput",
                    Output::repeatedStringField,
                    TypeConverters.STRING_PARAM_VALUE_CONVERTER::toParamValue,
                )
                .build()

        private val SINGLE_REQUIRED_FIELD_PROPERTY: Properties =
            Properties.newBuilder()
                .setRequiredEntityField(
                    Property.Builder<
                        androidx.appactions.interaction.capabilities.core.properties.Entity,
                        >()
                        .setRequired(true)
                        .build(),
                )
                .build()

        private fun getCurrentValues(
            argName: String,
            appDialogState: AppDialogState,
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
            property: Properties,
            sessionFactory: ExecutionSessionFactory<ExecutionSession>,
            sessionBridge: SessionBridge<ExecutionSession, Confirmation>,
            sessionUpdaterSupplier: Supplier<SessionUpdaterT>,
        ): TaskCapabilityImpl<
            Properties,
            Arguments,
            Output,
            ExecutionSession,
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
