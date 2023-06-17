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
import androidx.appactions.interaction.capabilities.core.ConfirmationOutput
import androidx.appactions.interaction.capabilities.core.EntitySearchResult
import androidx.appactions.interaction.capabilities.core.ExecutionResult
import androidx.appactions.interaction.capabilities.core.HostProperties
import androidx.appactions.interaction.capabilities.core.SearchAction
import androidx.appactions.interaction.capabilities.core.SessionConfig
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
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeSpec
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpec
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecBuilder
import androidx.appactions.interaction.capabilities.core.impl.spec.BoundProperty
import androidx.appactions.interaction.capabilities.core.impl.utils.EXTERNAL_TIMEOUT_MILLIS
import androidx.appactions.interaction.capabilities.core.properties.Property
import androidx.appactions.interaction.capabilities.core.properties.StringValue
import androidx.appactions.interaction.capabilities.core.testing.spec.Arguments
import androidx.appactions.interaction.capabilities.core.testing.spec.CapabilityStructFill
import androidx.appactions.interaction.capabilities.core.testing.spec.CapabilityTwoStrings
import androidx.appactions.interaction.capabilities.core.testing.spec.Confirmation
import androidx.appactions.interaction.capabilities.core.testing.spec.ExecutionSession
import androidx.appactions.interaction.capabilities.core.testing.spec.Output
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
import androidx.appactions.interaction.proto.FulfillmentRequest.Fulfillment.SyncStatus
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
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Supplier
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TaskCapabilityImplTest {
    private val capability: Capability =
        createCapability<EmptyTaskUpdater>(
            SINGLE_REQUIRED_FIELD_BOUND_PROPERTIES,
            sessionFactory =
            { _ ->
                object : ExecutionSession {
                    override fun onExecuteAsync(arguments: Arguments) =
                        Futures.immediateFuture(ExecutionResult.Builder<Output>().build())
                }
            },
            sessionBridge = { TaskHandler.Builder<Arguments, Confirmation>().build() },
            sessionUpdaterSupplier = ::EmptyTaskUpdater
        )
    private val hostProperties: HostProperties =
        HostProperties.Builder()
            .setMaxHostSizeDp(
                SizeF(300f, 500f)
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
                        IntentParameter.newBuilder().setName("required").setIsRequired(true)
                    )
                    .setTaskInfo(
                        TaskInfo.newBuilder().setSupportsPartialFulfillment(true)
                    )
                    .build()
            )
    }

    @Test
    fun appAction_computedProperty() {
        val mutableEntityList = mutableListOf<StringValue>()
        val capability = createCapability<EmptyTaskUpdater>(
            listOf(
                BoundProperty(
                    "required",
                    Property<StringValue>(possibleValueSupplier = mutableEntityList::toList),
                    TypeConverters.STRING_VALUE_ENTITY_CONVERTER
                )
            ),
            sessionFactory =
            {
                object : ExecutionSession {
                    override fun onExecuteAsync(arguments: Arguments) =
                        Futures.immediateFuture(ExecutionResult.Builder<Output>().build())
                }
            },
            sessionBridge = { TaskHandler.Builder<Arguments, Confirmation>().build() },
            sessionUpdaterSupplier = ::EmptyTaskUpdater
        )
        mutableEntityList.add(StringValue("entity1"))

        assertThat(capability.appAction).isEqualTo(
            AppAction.newBuilder()
                .setIdentifier("id")
                .setName("actions.intent.TEST")
                .addParams(
                    IntentParameter.newBuilder()
                        .setName("required")
                        .addPossibleEntities(
                            Entity.newBuilder().setIdentifier("entity1").setName("entity1")
                        )
                )
                .setTaskInfo(TaskInfo.newBuilder().setSupportsPartialFulfillment(true))
                .build()
        )

        mutableEntityList.add(StringValue("entity2"))
        assertThat(capability.appAction).isEqualTo(
            AppAction.newBuilder()
                .setIdentifier("id")
                .setName("actions.intent.TEST")
                .addParams(
                    IntentParameter.newBuilder()
                        .setName("required")
                        .addPossibleEntities(
                            Entity.newBuilder().setIdentifier("entity1").setName("entity1")
                        )
                        .addPossibleEntities(
                            Entity.newBuilder().setIdentifier("entity2").setName("entity2")
                        )
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
                SINGLE_REQUIRED_FIELD_BOUND_PROPERTIES,
                { externalSession },
                { TaskHandler.Builder<Arguments, Confirmation>().build() },
                ::EmptyTaskUpdater
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
                SINGLE_REQUIRED_FIELD_BOUND_PROPERTIES,
                sessionFactory =
                { _ ->
                    object : ExecutionSession {
                        override fun onCreate(sessionConfig: SessionConfig) {
                            onCreateInvocationCount.incrementAndGet()
                        }

                        override fun onExecuteAsync(arguments: Arguments) =
                            Futures.immediateFuture(
                                ExecutionResult.Builder<Output>().build()
                            )
                    }
                },
                sessionBridge = SessionBridge {
                    TaskHandler.Builder<Arguments, Confirmation>().build()
                },
                sessionUpdaterSupplier = ::EmptyTaskUpdater
            )
        val session = capability.createSession(fakeSessionId, hostProperties)

        // TURN 1.
        val callback = FakeCallbackInternal()
        session.execute(
            buildRequestArgs(SYNC, "unknownArgName", "foo"),
            callback
        )
        assertThat(callback.receiveResponse().fulfillmentResponse).isNotNull()
        assertThat(onCreateInvocationCount.get()).isEqualTo(1)

        // TURN 2.
        val callback2 = FakeCallbackInternal()
        session.execute(
            buildRequestArgs(
                SYNC,
                "required",
                ParamValue.newBuilder().setIdentifier("foo").setStringValue("foo").build()
            ),
            callback2
        )
        assertThat(callback2.receiveResponse().fulfillmentResponse).isNotNull()
        assertThat(onCreateInvocationCount.get()).isEqualTo(1)
    }

    class RequiredTaskUpdater : AbstractTaskUpdater() {
        fun setRequiredForExecutionStringValue(value: String) {
            super.updateParamValues(
                mapOf(
                    "required" to
                        listOf(
                            TypeConverters.STRING_PARAM_VALUE_CONVERTER.toParamValue(value)
                        )
                )
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
                        newCompleter
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
            SINGLE_REQUIRED_FIELD_BOUND_PROPERTIES,
            sessionFactory = { _ -> externalSession },
            sessionBridge = SessionBridge {
                TaskHandler.Builder<Arguments, Confirmation>().build()
            },
            sessionUpdaterSupplier = ::RequiredTaskUpdater
        )
        val session = capability.createSession("mySessionId", hostProperties)
        val callback = FakeCallbackInternal()
        session.execute(
            buildRequestArgs(
                SYNC,
                "required",
                "hello"
            ),
            callback
        )
        onExecuteReached.await()
        assertThat(UiHandleRegistry.getSessionIdFromUiHandle(externalSession)).isEqualTo(
            "mySessionId"
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
                SINGLE_REQUIRED_FIELD_BOUND_PROPERTIES,
                sessionFactory =
                { _ ->
                    object : ExecutionSession {
                        override fun onExecuteAsync(arguments: Arguments) =
                            Futures.immediateFuture(
                                ExecutionResult.Builder<Output>().build()
                            )
                    }
                },
                sessionBridge = SessionBridge {
                    TaskHandler.Builder<Arguments, Confirmation>().build()
                },
                sessionUpdaterSupplier = ::RequiredTaskUpdater
            )
        val session = capability.createSession(fakeSessionId, hostProperties)

        assertThat(capability.appAction)
            .isEqualTo(
                AppAction.newBuilder()
                    .setName("actions.intent.TEST")
                    .setIdentifier("id")
                    .addParams(
                        IntentParameter.newBuilder().setName("required").setIsRequired(true)
                    )
                    .setTaskInfo(
                        TaskInfo.newBuilder().setSupportsPartialFulfillment(true)
                    )
                    .build()
            )

        // TURN 1 (UNKNOWN).
        val callback = FakeCallbackInternal()
        session.execute(buildRequestArgs(UNKNOWN_TYPE), callback)
        assertThat(callback.receiveResponse().errorStatus)
            .isEqualTo(ErrorStatusInternal.INVALID_REQUEST)
    }

    @Test
    fun slotFilling_isActive_smokeTest() {
        val boundProperties = listOf(
            BoundProperty(
                "stringSlotA",
                Property<StringValue>(isRequiredForExecution = true),
                TypeConverters.STRING_VALUE_ENTITY_CONVERTER
            ),
            BoundProperty(
                "stringSlotB",
                Property<StringValue>(isRequiredForExecution = true),
                TypeConverters.STRING_VALUE_ENTITY_CONVERTER
            )
        )
        val sessionFactory:
            (hostProperties: HostProperties?) -> CapabilityTwoStrings.ExecutionSession =
            { _ ->
                object : CapabilityTwoStrings.ExecutionSession {
                    override suspend fun onExecute(
                        arguments: CapabilityTwoStrings.Arguments
                    ): ExecutionResult<CapabilityTwoStrings.Output> =
                        ExecutionResult.Builder<CapabilityTwoStrings.Output>().build()
                }
            }
        val sessionBridge =
            SessionBridge<
                CapabilityTwoStrings.ExecutionSession,
                CapabilityTwoStrings.Arguments,
                CapabilityTwoStrings.Confirmation> {
                TaskHandler.Builder<
                    CapabilityTwoStrings.Arguments,
                    CapabilityTwoStrings.Confirmation
                >()
                    .registerValueTaskParam(
                        "stringSlotA",
                        AUTO_ACCEPT_STRING_VALUE,
                        TypeConverters.STRING_PARAM_VALUE_CONVERTER
                    )
                    .registerValueTaskParam(
                        "stringSlotB",
                        AUTO_ACCEPT_STRING_VALUE,
                        TypeConverters.STRING_PARAM_VALUE_CONVERTER
                    )
                    .build()
            }
        val capability: Capability =
            TaskCapabilityImpl(
                "fakeId",
                CapabilityTwoStrings.ACTION_SPEC,
                boundProperties,
                sessionFactory,
                sessionBridge,
                ::EmptyTaskUpdater
            )

        val session = capability.createSession(fakeSessionId, hostProperties)
        assertThat(session.isActive).isTrue()

        // turn 1
        val callback = FakeCallbackInternal()
        session.execute(
            buildRequestArgs(
                SYNC,
                "stringSlotA",
                ParamValue.newBuilder().setIdentifier("foo").setStringValue("foo").build()
            ),
            callback
        )
        assertThat(callback.receiveResponse().fulfillmentResponse).isNotNull()
        assertThat(session.isActive).isTrue()

        // turn 2
        val callback2 = FakeCallbackInternal()
        session.execute(
            buildRequestArgs(
                SYNC,
                "stringSlotA",
                ParamValue.newBuilder().setStringValue("foo").build(),
                "stringSlotB",
                ParamValue.newBuilder().setStringValue("bar").build()
            ),
            callback2
        )
        assertThat(callback2.receiveResponse().fulfillmentResponse).isNotNull()
        assertThat(session.isActive).isFalse()

        // turn 3
        val callback3 = FakeCallbackInternal()
        session.execute(
            buildRequestArgs(CANCEL),
            callback3
        )
        assertThat(session.isActive).isFalse()
    }

    @Test
    @kotlin.Throws(Exception::class)
    fun slotFilling_optionalButRejectedParam_onFinishNotInvoked() {
        val onExecuteInvocationCount = AtomicInteger(0)
        val boundProperties = listOf(
            BoundProperty(
                "stringSlotA",
                Property<StringValue>(isRequiredForExecution = true),
                TypeConverters.STRING_VALUE_ENTITY_CONVERTER
            ),
            BoundProperty(
                "stringSlotB",
                Property<StringValue>(isRequiredForExecution = false),
                TypeConverters.STRING_VALUE_ENTITY_CONVERTER
            )
        )
        val sessionFactory:
            (hostProperties: HostProperties?) -> CapabilityTwoStrings.ExecutionSession =
            { _ ->
                object : CapabilityTwoStrings.ExecutionSession {
                    override suspend fun onExecute(
                        arguments: CapabilityTwoStrings.Arguments
                    ): ExecutionResult<CapabilityTwoStrings.Output> {
                        onExecuteInvocationCount.incrementAndGet()
                        return ExecutionResult.Builder<CapabilityTwoStrings.Output>().build()
                    }
                }
            }
        val sessionBridge =
            SessionBridge<
                CapabilityTwoStrings.ExecutionSession,
                CapabilityTwoStrings.Arguments,
                CapabilityTwoStrings.Confirmation> {
                TaskHandler.Builder<
                    CapabilityTwoStrings.Arguments,
                    CapabilityTwoStrings.Confirmation
                >()
                    .registerValueTaskParam(
                        "stringSlotA",
                        AUTO_ACCEPT_STRING_VALUE,
                        TypeConverters.STRING_PARAM_VALUE_CONVERTER
                    )
                    .registerValueTaskParam(
                        "stringSlotB",
                        AUTO_REJECT_STRING_VALUE,
                        TypeConverters.STRING_PARAM_VALUE_CONVERTER
                    )
                    .build()
            }
        val capability: Capability =
            TaskCapabilityImpl(
                "fakeId",
                CapabilityTwoStrings.ACTION_SPEC,
                boundProperties,
                sessionFactory,
                sessionBridge,
                ::EmptyTaskUpdater
            )
        val session = capability.createSession(fakeSessionId, hostProperties)

        // TURN 1.
        val callback = FakeCallbackInternal()
        session.execute(
            buildRequestArgs(
                SYNC,
                "stringSlotA",
                ParamValue.newBuilder().setIdentifier("foo").setStringValue("foo").build(),
                "stringSlotB",
                ParamValue.newBuilder().setIdentifier("bar").setStringValue("bar").build()
            ),
            callback
        )
        assertThat(callback.receiveResponse().fulfillmentResponse).isNotNull()
        assertThat(onExecuteInvocationCount.get()).isEqualTo(0)
        assertThat(getCurrentValues("stringSlotA", session.state!!))
            .containsExactly(
                CurrentValue.newBuilder()
                    .setValue(
                        ParamValue.newBuilder().setIdentifier("foo").setStringValue("foo")
                    )
                    .setStatus(CurrentValue.Status.ACCEPTED)
                    .build()
            )
        assertThat(getCurrentValues("stringSlotB", session.state!!))
            .containsExactly(
                CurrentValue.newBuilder()
                    .setValue(
                        ParamValue.newBuilder().setIdentifier("bar").setStringValue("bar")
                    )
                    .setStatus(CurrentValue.Status.REJECTED)
                    .build()
            )
    }

    @Test
    @kotlin.Throws(Exception::class)
    fun slotFilling_assistantRemovedParam_clearInSdkState() {
        val boundProperties = listOf(
            BoundProperty(
                "required",
                Property<StringValue>(isRequiredForExecution = true),
                TypeConverters.STRING_VALUE_ENTITY_CONVERTER
            ),
            BoundProperty(
                "optional",
                Property<StringValue>(
                    possibleValues = listOf(StringValue("VALUE_1"), StringValue("VALUE_2")),
                    isRequiredForExecution = true,
                ),
                TypeConverters.STRING_VALUE_ENTITY_CONVERTER
            )
        )
        val capability: Capability =
            createCapability(
                boundProperties,
                sessionFactory = { _ -> ExecutionSession.DEFAULT },
                sessionBridge = SessionBridge {
                    TaskHandler.Builder<Arguments, Confirmation>().build()
                },
                sessionUpdaterSupplier = ::EmptyTaskUpdater
            )
        val session = capability.createSession(fakeSessionId, hostProperties)

        // TURN 1.
        val callback = FakeCallbackInternal()
        session.execute(
            buildRequestArgs(
                SYNC,
                "required",
                ParamValue.newBuilder().setIdentifier("foo").setStringValue("foo").build()
            ),
            callback
        )
        assertThat(callback.receiveResponse()).isNotNull()
        assertThat(getCurrentValues("required", session.state!!))
            .containsExactly(
                CurrentValue.newBuilder()
                    .setValue(
                        ParamValue.newBuilder().setIdentifier("foo").setStringValue("foo")
                    )
                    .setStatus(CurrentValue.Status.ACCEPTED)
                    .build()
            )
        assertThat(getCurrentValues("optional", session.state!!)).isEmpty()

        // TURN 2.
        val callback2 = FakeCallbackInternal()
        session.execute(
            buildRequestArgs(SYNC, "optional", "VALUE_2"),
            callback2
        )
        assertThat(callback2.receiveResponse().fulfillmentResponse).isNotNull()
        assertThat(getCurrentValues("required", session.state!!)).isEmpty()
        assertThat(getCurrentValues("optional", session.state!!))
            .containsExactly(
                CurrentValue.newBuilder()
                    .setValue(ParamValue.newBuilder().setStringValue("VALUE_2"))
                    .setStatus(CurrentValue.Status.ACCEPTED)
                    .build()
            )
    }

    @Test
    @kotlin.Throws(Exception::class)
    fun disambig_singleParam_disambigEntitiesInContext() {
        val capability: Capability =
            createCapability(
                SINGLE_REQUIRED_FIELD_BOUND_PROPERTIES,
                sessionFactory = {
                    object : ExecutionSession {
                        override suspend fun onExecute(arguments: Arguments) =
                            ExecutionResult.Builder<Output>().build()

                        override val requiredStringListener =
                            object : AppEntityListener<String> {
                                override fun lookupAndRenderAsync(
                                    searchAction: SearchAction<String>
                                ): ListenableFuture<EntitySearchResult<String>> {
                                    return Futures.immediateFuture(
                                        EntitySearchResult.Builder<String>()
                                            .addPossibleValue("valid1")
                                            .addPossibleValue("valid2")
                                            .build()
                                    )
                                }

                                override fun onReceivedAsync(
                                    value: String
                                ): ListenableFuture<ValidationResult> {
                                    return Futures.immediateFuture(ValidationResult.newAccepted())
                                }
                            }
                    }
                },
                sessionBridge =
                SessionBridge<ExecutionSession, Arguments, Confirmation> { session ->
                    val builder = TaskHandler.Builder<Arguments, Confirmation>()
                    session.requiredStringListener?.let {
                            listener: AppEntityListener<String> ->
                        builder.registerAppEntityTaskParam(
                            "required",
                            listener,
                            TypeConverters.STRING_PARAM_VALUE_CONVERTER,
                            EntityConverter.of(TypeSpec.STRING_TYPE_SPEC),
                            getTrivialSearchActionConverter()
                        )
                    }
                    builder.build()
                },
                sessionUpdaterSupplier = ::EmptyTaskUpdater
            )
        val session = capability.createSession(fakeSessionId, hostProperties)

        // TURN 1.
        val callback = FakeCallbackInternal()
        session.execute(
            buildRequestArgs(SYNC, "required", buildSearchActionParamValue("invalid")),
            callback
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
                                        buildSearchActionParamValue("invalid")
                                    )
                                    .setStatus(
                                        CurrentValue.Status.DISAMBIG
                                    )
                                    .setDisambiguationData(
                                        DisambiguationData.newBuilder()
                                            .addEntities(
                                                Entity.newBuilder()
                                                    .setStringValue(
                                                        "valid1"
                                                    )
                                            )
                                            .addEntities(
                                                Entity.newBuilder()
                                                    .setStringValue(
                                                        "valid2"
                                                    )
                                            )
                                    )
                            )
                    ).build()
            )

        // TURN 2.
        val callback2 = FakeCallbackInternal()
        session.execute(
            buildRequestArgs(
                SYNC,
                "required",
                ParamValue.newBuilder().setIdentifier("valid2").setStringValue("valid2").build()
            ),
            callback2
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
                                            .setIdentifier("valid2")
                                            .setStringValue(
                                                "valid2"
                                            )
                                    )
                                    .setStatus(
                                        CurrentValue.Status.ACCEPTED
                                    )
                            )
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
    fun identifierOnly_refillsStruct() = runBlocking<Unit> {
        val boundProperties = listOf(
            BoundProperty(
                "listItem",
                Property<ListItem>(isRequiredForExecution = true),
                EntityConverter.of(TypeConverters.LIST_ITEM_TYPE_SPEC)
            ),
            BoundProperty(
                "string",
                Property<StringValue>(isRequiredForExecution = true),
                TypeConverters.STRING_VALUE_ENTITY_CONVERTER
            )
        )
        val item1: ListItem = ListItem.Builder().setName("red apple").setIdentifier("item1").build()
        val item2: ListItem =
            ListItem.Builder().setName("green apple").setIdentifier("item2").build()
        val onReceivedDeferred = CompletableDeferred<ListItem>()
        val onExecuteListItemDeferred = CompletableDeferred<ListItem>()
        val onExecuteStringDeferred = CompletableDeferred<String>()

        val sessionFactory:
            (hostProperties: HostProperties?) -> CapabilityStructFill.ExecutionSession =
            { _ ->
                object : CapabilityStructFill.ExecutionSession {
                    override suspend fun onExecute(
                        arguments: CapabilityStructFill.Arguments
                    ): ExecutionResult<CapabilityStructFill.Output> {
                        arguments.listItem?.let { onExecuteListItemDeferred.complete(it) }
                        arguments.string?.let { onExecuteStringDeferred.complete(it) }
                        return ExecutionResult.Builder<CapabilityStructFill.Output>().build()
                    }

                    override val listItemListener =
                        object : AppEntityListener<ListItem> {
                            override fun onReceivedAsync(
                                value: ListItem
                            ): ListenableFuture<ValidationResult> {
                                onReceivedDeferred.complete(value)
                                return Futures.immediateFuture(ValidationResult.newAccepted())
                            }

                            override fun lookupAndRenderAsync(
                                searchAction: SearchAction<ListItem>
                            ): ListenableFuture<EntitySearchResult<ListItem>> =
                                Futures.immediateFuture(
                                    EntitySearchResult.Builder<ListItem>()
                                        .addPossibleValue(item1)
                                        .addPossibleValue(item2)
                                        .build()
                                )
                        }
                }
            }
        val sessionBridge =
            SessionBridge<
                CapabilityStructFill.ExecutionSession,
                CapabilityStructFill.Arguments,
                CapabilityStructFill.Confirmation> { session ->
                TaskHandler.Builder<
                    CapabilityStructFill.Arguments,
                    CapabilityStructFill.Confirmation
                >()
                    .registerAppEntityTaskParam(
                        "listItem",
                        session.listItemListener,
                        ParamValueConverter.of(LIST_ITEM_TYPE_SPEC),
                        EntityConverter.of(LIST_ITEM_TYPE_SPEC)::convert,
                        getTrivialSearchActionConverter()
                    )
                    .build()
            }

        val capability: Capability =
            TaskCapabilityImpl(
                "selectListItem",
                CapabilityStructFill.ACTION_SPEC,
                boundProperties,
                sessionFactory,
                sessionBridge,
                ::EmptyTaskUpdater
            )
        val session = capability.createSession(fakeSessionId, hostProperties)

        // first sync request
        val callback = FakeCallbackInternal()
        session.execute(
            buildRequestArgs(SYNC, "listItem", buildSearchActionParamValue("apple")),
            callback
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
                                            "apple"
                                        )
                                    )
                                    .setStatus(CurrentValue.Status.DISAMBIG)
                                    .setDisambiguationData(
                                        DisambiguationData.newBuilder()
                                            .addEntities(
                                                EntityConverter.of(LIST_ITEM_TYPE_SPEC)
                                                    .convert(item1)
                                            )
                                            .addEntities(
                                                EntityConverter.of(LIST_ITEM_TYPE_SPEC)
                                                    .convert(item2)
                                            )
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .addParams(DialogParameter.newBuilder().setName("string").build())
                    .build()
            )

        // second sync request, sending grounded ParamValue with identifier only
        val callback2 = FakeCallbackInternal()
        session.execute(
            buildRequestArgs(
                SYNC,
                "listItem",
                ParamValue.newBuilder().setIdentifier("item2").build()
            ),
            callback2
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
                "unused"
            ),
            callback3
        )
        assertThat(callback3.receiveResponse().fulfillmentResponse).isNotNull()
        assertThat(onExecuteListItemDeferred.awaitSync()).isEqualTo(item2)
        assertThat(onExecuteStringDeferred.awaitSync()).isEqualTo("unused")
    }

    @Test
    @kotlin.Throws(Exception::class)
    fun executionResult_resultReturned() {
        val sessionFactory: (hostProperties: HostProperties?) -> ExecutionSession =
            { _ ->
                object : ExecutionSession {
                    override suspend fun onExecute(arguments: Arguments) =
                        ExecutionResult.Builder<Output>()
                            .setOutput(
                                Output.Builder()
                                    .setOptionalStringField("bar")
                                    .setRepeatedStringField(
                                        listOf("bar1", "bar2")
                                    )
                                    .build()
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
                            ParamValue.newBuilder().setStringValue("bar").build()
                        )
                        .build()
                )
                .addOutputValues(
                    OutputValue.newBuilder()
                        .setName("repeatedStringOutput")
                        .addValues(
                            ParamValue.newBuilder().setStringValue("bar1").build()
                        )
                        .addValues(
                            ParamValue.newBuilder().setStringValue("bar2").build()
                        )
                        .build()
                )
                .build()
        session.execute(
            buildRequestArgs(
                SYNC, /* args...= */
                "required",
                ParamValue.newBuilder().setIdentifier("foo").setStringValue("foo").build()
            ),
            callback
        )
        assertThat(
            callback.receiveResponse()
                .fulfillmentResponse!!
                .executionOutput
                .outputValuesList
        )
            .containsExactlyElementsIn(expectedOutput.outputValuesList)
    }

    @Test
    @kotlin.Throws(Exception::class)
    fun executionResult_shouldStartDictation_resultReturned() {
        val sessionFactory: (hostProperties: HostProperties?) -> ExecutionSession =
            { _ ->
                object : ExecutionSession {
                    override suspend fun onExecute(arguments: Arguments) =
                        ExecutionResult.Builder<Output>()
                            .setShouldStartDictation(true)
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
                ParamValue.newBuilder().setIdentifier("foo").setStringValue("foo").build()
            ),
            callback
        )

        assertThat(callback.receiveResponse().fulfillmentResponse!!.startDictation).isTrue()
    }

    @Test
    @kotlin.Throws(Exception::class)
    fun fulfillmentType_finalSync_stateCleared() {
        val sessionFactory: (hostProperties: HostProperties?) -> ExecutionSession =
            { _ ->
                object : ExecutionSession {
                    override suspend fun onExecute(arguments: Arguments) =
                        ExecutionResult.Builder<Output>().build()
                }
            }
        val capability: Capability =
            createCapability(
                SINGLE_REQUIRED_FIELD_BOUND_PROPERTIES,
                sessionFactory = sessionFactory,
                sessionBridge = SessionBridge {
                    TaskHandler.Builder<Arguments, Confirmation>().build()
                },
                sessionUpdaterSupplier = ::EmptyTaskUpdater
            )
        val session = capability.createSession(fakeSessionId, hostProperties)

        // TURN 1. Not providing all the required slots in the SYNC Request
        val callback = FakeCallbackInternal()
        session.execute(
            buildRequestArgs(SYNC),
            callback
        )
        assertThat(callback.receiveResponse()).isNotNull()
        assertThat(getCurrentValues("required", session.state!!)).isEmpty()
        assertThat(session.isActive).isEqualTo(true)

        // TURN 2. Providing the required slots so that the task completes and the state gets cleared
        val callback2 = FakeCallbackInternal()
        session.execute(
            buildRequestArgs(
                SYNC,
                "required",
                ParamValue.newBuilder().setIdentifier("foo").setStringValue("foo").build()
            ),
            callback2
        )
        assertThat(callback2.receiveResponse().fulfillmentResponse).isNotNull()
        assertThat(session.isActive).isEqualTo(false)
    }

    @Test
    @kotlin.Throws(Exception::class)
    fun fulfillmentType_syncWithConfirmation_stateClearedAfterConfirmation() {
        val sessionFactory: (hostProperties: HostProperties?) -> ExecutionSession =
            { _ ->
                object : ExecutionSession {
                    override suspend fun onExecute(arguments: Arguments) =
                        ExecutionResult.Builder<Output>().build()
                }
            }
        var onReadyToConfirm =
            object : OnReadyToConfirmListener<Arguments, Confirmation> {
                override suspend fun onReadyToConfirm(arguments: Arguments):
                    ConfirmationOutput<Confirmation> {
                    return ConfirmationOutput.Builder<Confirmation>()
                        .setConfirmation(
                            Confirmation.Builder().setOptionalStringField("bar")
                                .build()
                        )
                        .build()
                }
            }
        val capability: Capability =
            createCapability(
                SINGLE_REQUIRED_FIELD_BOUND_PROPERTIES,
                sessionFactory = sessionFactory,
                sessionBridge = SessionBridge {
                    TaskHandler.Builder<Arguments, Confirmation>()
                        .setOnReadyToConfirmListener(onReadyToConfirm)
                        .build()
                },
                sessionUpdaterSupplier = ::EmptyTaskUpdater
            )
        val session = capability.createSession(fakeSessionId, hostProperties)

        // TURN 1. Providing all the required slots in the SYNC Request
        val callback = FakeCallbackInternal()
        session.execute(
            buildRequestArgs(
                SYNC,
                "required",
                ParamValue.newBuilder().setIdentifier("foo").setStringValue("foo").build()
            ),
            callback
        )
        assertThat(callback.receiveResponse()).isNotNull()
        assertThat(session.isActive).isEqualTo(true)

        // Sending the confirmation request. After the confirm request, the session should not be
        // active
        val callback2 = FakeCallbackInternal()
        session.execute(
            buildRequestArgs(SYNC, SyncStatus.INTENT_CONFIRMED),
            callback2
        )

        assertThat(callback2.receiveResponse().fulfillmentResponse).isNotNull()
        assertThat(session.isActive).isEqualTo(false)
    }

    @Test
    fun fulfillmentRequest_whenStatusDestroyed_errorReported() {
        val sessionFactory: (hostProperties: HostProperties?) -> ExecutionSession =
            { _ ->
                object : ExecutionSession {
                    override suspend fun onExecute(arguments: Arguments) =
                        ExecutionResult.Builder<Output>().build()
                }
            }
        val capability: Capability =
            createCapability(
                SINGLE_REQUIRED_FIELD_BOUND_PROPERTIES,
                sessionFactory = sessionFactory,
                sessionBridge = SessionBridge {
                    TaskHandler.Builder<Arguments, Confirmation>().build()
                },
                sessionUpdaterSupplier = ::EmptyTaskUpdater
            )
        val session = capability.createSession(fakeSessionId, hostProperties)

        // TURN 1. Providing the required slots so that the task completes and the state gets cleared
        val callback = FakeCallbackInternal()
        session.execute(
            buildRequestArgs(
                SYNC,
                "required",
                ParamValue.newBuilder().setIdentifier("foo").setStringValue("foo").build()
            ),
            callback
        )
        assertThat(callback.receiveResponse().fulfillmentResponse).isNotNull()
        assertThat(session.isActive).isEqualTo(false)

        // TURN 2. Trying to sync after the session is destroyed
        val callback2 = FakeCallbackInternal()
        session.execute(
            buildRequestArgs(
                SYNC,
                "required",
                ParamValue.newBuilder().setIdentifier("foo").setStringValue("foo").build()
            ),
            callback2
        )
        assertThat(session.isActive).isEqualTo(false)
        assertThat(callback2.receiveResponse().errorStatus)
            .isEqualTo(ErrorStatusInternal.SESSION_NOT_FOUND)
    }
    @Test
    @kotlin.Throws(Exception::class)
    fun syncStatus_unknown_errorReported() {
        val capability: Capability =
            createCapability(
                SINGLE_REQUIRED_FIELD_BOUND_PROPERTIES,
                sessionFactory =
                    { _ ->
                        object : ExecutionSession {
                            override suspend fun onExecute(arguments: Arguments) =
                                ExecutionResult.Builder<Output>().build()
                        }
                    },
                sessionBridge = SessionBridge { TaskHandler.Builder<Arguments, Confirmation>()
                    .build() },
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
        val errorCallback = FakeCallbackInternal()
        session.execute(buildRequestArgs(SYNC, SyncStatus.UNKNOWN_SYNC_STATUS), errorCallback)
        assertThat(errorCallback.receiveResponse().errorStatus)
            .isEqualTo(ErrorStatusInternal.INVALID_REQUEST)

        // TURN 2 (UNRECOGNIZED)
        val errorCallback2 = FakeCallbackInternal()
        session.execute(buildRequestArgs(SYNC, SyncStatus.UNRECOGNIZED), errorCallback2)
        assertThat(errorCallback2.receiveResponse().errorStatus)
            .isEqualTo(ErrorStatusInternal.INVALID_REQUEST)
    }

    @Test
    @kotlin.Throws(Exception::class)
    fun syncStatus_slotsIncomplete_taskNotExecuted() {
        val onExecuteInvocationCount = AtomicInteger(0)
        val sessionFactory: (hostProperties: HostProperties?) -> ExecutionSession =
            { _ ->
                object : ExecutionSession {
                    override suspend fun onExecute(arguments: Arguments): ExecutionResult<Output> {
                        onExecuteInvocationCount.incrementAndGet()
                        return ExecutionResult.Builder<Output>().build()
                    }
                }
            }
        val capability: Capability =
            createCapability(
                SINGLE_REQUIRED_FIELD_BOUND_PROPERTIES,
                sessionFactory = sessionFactory,
                sessionBridge = SessionBridge { TaskHandler.Builder<Arguments, Confirmation>()
                    .build() },
                sessionUpdaterSupplier = ::EmptyTaskUpdater,
            )
        val session = capability.createSession(fakeSessionId, hostProperties)

        /** TURN 1. Not providing all the required slots and passing SyncStatus as SLOTS_INCOMPLETE
         *  Execution should not happen as sync status is SLOTS_INCOMPLETE
         */
        val callback = FakeCallbackInternal()
        session.execute(buildRequestArgs(SYNC, SyncStatus.SLOTS_INCOMPLETE), callback)
        assertThat(callback.receiveResponse()).isNotNull()
        assertThat(getCurrentValues("required", session.state!!)).isEmpty()
        assertThat(onExecuteInvocationCount.get()).isEqualTo(0)

        /** TURN 2. Providing all the required slots but still passing SyncStatus as
         * SLOTS_INCOMPLETE. Execution should not happen as sync status is SLOTS_INCOMPLETE
         */
        val callback2 = FakeCallbackInternal()
        session.execute(
            buildRequestArgs(
                SYNC,
                SyncStatus.SLOTS_INCOMPLETE,
                "required",
                ParamValue.newBuilder().setIdentifier("foo").setStringValue("foo").build(),
            ),
            callback2,
        )
        assertThat(callback2.receiveResponse()).isNotNull()
        assertThat(getCurrentValues("required", session.state!!))
            .containsExactly(
                CurrentValue.newBuilder()
                    .setValue(
                        ParamValue.newBuilder().setIdentifier("foo").setStringValue("foo"),
                    )
                    .setStatus(CurrentValue.Status.ACCEPTED)
                    .build(),
            )
        assertThat(onExecuteInvocationCount.get()).isEqualTo(0)

        /**
         *  TURN 3. Providing all the required slots and passing SyncStatus as SLOTS_COMPLETE
         *  Execution should happen
         */
        val callback3 = FakeCallbackInternal()
        session.execute(
            buildRequestArgs(
                SYNC,
                SyncStatus.SLOTS_COMPLETE,
                "required",
                ParamValue.newBuilder().setIdentifier("foo").setStringValue("foo").build(),
            ),
            callback3,
        )
        assertThat(callback3.receiveResponse()).isNotNull()
        assertThat(onExecuteInvocationCount.get()).isEqualTo(1)
    }

    @Test
    @kotlin.Throws(Exception::class)
    fun syncStatus_intentConfirmed_taskExecuted() {
        val onConfirmationInvocationCount = AtomicInteger(0)
        var onReadyConfirm =
            object : OnReadyToConfirmListener<Arguments, Confirmation> {
                override suspend fun onReadyToConfirm(arguments: Arguments):
                    ConfirmationOutput<Confirmation> {
                    onConfirmationInvocationCount.incrementAndGet()
                    return ConfirmationOutput.Builder<Confirmation>()
                        .setConfirmation(Confirmation.Builder().setOptionalStringField("bar")
                            .build())
                        .build()
                }
            }
        val onExecuteInvocationCount = AtomicInteger(0)
        val sessionFactory: (hostProperties: HostProperties?) -> ExecutionSession =
            { _ ->
                object : ExecutionSession {
                    override suspend fun onExecute(arguments: Arguments): ExecutionResult<Output> {
                        onExecuteInvocationCount.incrementAndGet()
                        return ExecutionResult.Builder<Output>()
                            .setOutput(
                                Output.Builder()
                                    .setOptionalStringField("baz")
                                    .setRepeatedStringField(listOf("baz1", "baz2"))
                                    .build())
                            .build()
                    }
                }
            }
        val capability: Capability =
            createCapability(
                SINGLE_REQUIRED_FIELD_BOUND_PROPERTIES,
                sessionFactory = sessionFactory,
                sessionBridge = {
                                    TaskHandler.Builder<Arguments, Confirmation>()
                                        .setOnReadyToConfirmListener(onReadyConfirm)
                                        .registerConfirmationOutput(
                                            "optionalStringOutput",
                                            Confirmation::optionalStringField,
                                            TypeConverters.STRING_PARAM_VALUE_CONVERTER
                                            ::toParamValue)
                                        .build() },
                sessionUpdaterSupplier = ::EmptyTaskUpdater,
            )

        val session = capability.createSession(fakeSessionId, hostProperties)

        /** TURN 1. Providing all the required slots and passing SyncStatus as SLOTS_COMPLETE
         *  This should trigger the confirmation. Execution should not happen
         */
        val callback = FakeCallbackInternal()
        session.execute(buildRequestArgs(
            SYNC,
            SyncStatus.SLOTS_COMPLETE,
            "required",
            ParamValue.newBuilder().setIdentifier("foo").setStringValue("foo").build(),
        ), callback)
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
        assertThat(onExecuteInvocationCount.get()).isEqualTo(0)
        assertThat(onConfirmationInvocationCount.get()).isEqualTo(1)

        // Confirm the BIC
        val expectedConfirmationOutput: StructuredOutput =
            StructuredOutput.newBuilder()
                .addOutputValues(
                    OutputValue.newBuilder()
                        .setName("optionalStringOutput")
                        .addValues(ParamValue.newBuilder().setStringValue("bar").build())
                        .build())
                .build()
        assertThat(callback.receiveResponse()
            .fulfillmentResponse!!
            .confirmationData
            .outputValuesList
        )
            .containsExactlyElementsIn(expectedConfirmationOutput.outputValuesList)

        /** TURN 2. Send CONFIRM request using INTENT_CONFIRMED sync status.
         *  Execution should happen
         */
        val callback2 = FakeCallbackInternal()
        session.execute(
            buildRequestArgs(
                SYNC,
                SyncStatus.INTENT_CONFIRMED,
            ),
            callback2,
        )
        assertThat(callback2.receiveResponse()).isNotNull()
        assertThat(onExecuteInvocationCount.get()).isEqualTo(1)

        // Confirm the BIO
        val expectedOutput: StructuredOutput =
            StructuredOutput.newBuilder()
                .addOutputValues(
                    OutputValue.newBuilder()
                        .setName("optionalStringOutput")
                        .addValues(ParamValue.newBuilder().setStringValue("baz"))
                        .build())
                .addOutputValues(
                    OutputValue.newBuilder()
                        .setName("repeatedStringOutput")
                        .addValues(ParamValue.newBuilder().setStringValue("baz1").build())
                        .addValues(ParamValue.newBuilder().setStringValue("baz2").build())
                        .build()
                )
                .build()
        assertThat(callback2.receiveResponse()
            .fulfillmentResponse!!
            .getExecutionOutput()
            .getOutputValuesList())
            .containsExactlyElementsIn(expectedOutput.getOutputValuesList())
    }

    @Test
    fun structConversionException_shouldReportInternalFailure() {
        val sessionFactory: (hostProperties: HostProperties?) -> ExecutionSession =
            { _ ->
                object : ExecutionSession {
                    override val requiredStringListener = object : AppEntityListener<String> {
                        override fun lookupAndRenderAsync(
                            searchAction: SearchAction<String>
                        ): ListenableFuture<EntitySearchResult<String>> {
                            return Futures.immediateFuture(
                                EntitySearchResult.Builder<String>().build()
                            )
                        }

                        override fun onReceivedAsync(
                            value: String
                        ): ListenableFuture<ValidationResult> {
                            return Futures.immediateFuture(ValidationResult.newAccepted())
                        }
                    }
                    override suspend fun onExecute(arguments: Arguments) =
                        ExecutionResult.Builder<Output>()
                            .setShouldStartDictation(true)
                            .build()
                }
            }
        val capability = CapabilityBuilder()
            .setId("fakeId")
            .setExecutionSessionFactory(sessionFactory)
            .build()
        val session = capability.createSession(fakeSessionId, hostProperties)
        val callback = FakeCallbackInternal()

        session.execute(
            buildRequestArgs(
                SYNC,
                "required",
                // this ParamValue can't be parsed as String, and causes StructConversionException
                ParamValue.newBuilder().setIdentifier("foo").setBoolValue(false).build()
            ),
            callback
        )

        assertThat(
            callback.receiveResponse().errorStatus
        ).isEqualTo(ErrorStatusInternal.INTERNAL)
    }

    @Test
    fun slotListenerException_shouldReportExternalException() {
        val sessionFactory: (hostProperties: HostProperties?) -> ExecutionSession =
            { _ ->
                object : ExecutionSession {
                    override val requiredStringListener = object : AppEntityListener<String> {
                        override fun lookupAndRenderAsync(
                            searchAction: SearchAction<String>
                        ): ListenableFuture<EntitySearchResult<String>> {
                            throw IllegalStateException("error in lookupAndRender")
                        }

                        override fun onReceivedAsync(
                            value: String
                        ): ListenableFuture<ValidationResult> {
                            throw IllegalStateException("error in onReceivedAsync")
                        }
                    }
                    override suspend fun onExecute(arguments: Arguments) =
                        ExecutionResult.Builder<Output>()
                            .setShouldStartDictation(true)
                            .build()
                }
            }
        val capability = CapabilityBuilder()
            .setId("fakeId")
            .setExecutionSessionFactory(sessionFactory)
            .build()
        val session = capability.createSession(fakeSessionId, hostProperties)
        val callback = FakeCallbackInternal()

        session.execute(
            buildRequestArgs(
                SYNC, /* args...= */
                "required",
                ParamValue.newBuilder().setIdentifier("foo").setStringValue("foo").build()
            ),
            callback
        )

        assertThat(
            callback.receiveResponse().errorStatus
        ).isEqualTo(ErrorStatusInternal.EXTERNAL_EXCEPTION)
    }

    @kotlinx.coroutines.ExperimentalCoroutinesApi
    @Test
    fun slotListenerTimeout_returnsCorrectErrorStatus() = runTest {
        val externalSession = object : ExecutionSession {
            override val requiredStringListener = object : AppEntityListener<String> {
                override suspend fun lookupAndRender(
                    searchAction: SearchAction<String>
                ): EntitySearchResult<String> {
                    return EntitySearchResult.Builder<String>().build()
                }

                override suspend fun onReceived(
                    value: String
                ): ValidationResult {
                    delay(EXTERNAL_TIMEOUT_MILLIS + 1000L)
                    return ValidationResult.newAccepted()
                }
            }
        }
        val session = TaskCapabilitySession(
            "sessionId",
            ACTION_SPEC,
            ACTION_SPEC.createAppAction(
                "fakeCapabilityId",
                SINGLE_REQUIRED_FIELD_BOUND_PROPERTIES,
                supportsPartialFulfillment = true
            ),
            TaskHandler.Builder<Arguments, Confirmation>()
            .registerAppEntityTaskParam(
                "required",
                externalSession.requiredStringListener,
                TypeConverters.STRING_PARAM_VALUE_CONVERTER,
                EntityConverter.of(TypeSpec.STRING_TYPE_SPEC),
                getTrivialSearchActionConverter()
            ).build(),
            externalSession,
            scope = this,
        )
        val callback = FakeCallbackInternal(timeoutMs = EXTERNAL_TIMEOUT_MILLIS + 2000L)

        session.execute(
            buildRequestArgs(
                SYNC, /* args...= */
                "required",
                ParamValue.newBuilder().setIdentifier("foo").setStringValue("foo").build()
            ),
            callback
        )

        advanceTimeBy(EXTERNAL_TIMEOUT_MILLIS + 2000L)
        val response = callback.receiveResponse()
        assertThat(
            response.errorStatus
        ).isEqualTo(ErrorStatusInternal.EXTERNAL_EXCEPTION)
    }

    /**
     * an implementation of Capability.Builder using Argument. Output, etc. defined under
     * testing/spec
     */
    class CapabilityBuilder :
        Capability.Builder<
            CapabilityBuilder,
            Arguments,
            Output,
            Confirmation,
            ExecutionSession
            >(ACTION_SPEC) {

        init {
            setProperty(
                "required",
                Property<StringValue>(isRequiredForExecution = true),
                TypeConverters.STRING_VALUE_ENTITY_CONVERTER
            )
        }

        public override fun setExecutionSessionFactory(
            sessionFactory: (hostProperties: HostProperties?) -> ExecutionSession
        ) = super.setExecutionSessionFactory(sessionFactory)

        override val sessionBridge: SessionBridge<
            ExecutionSession,
            Arguments,
            Confirmation
        > = SessionBridge {
                session ->
            val builder = TaskHandler.Builder<Arguments, Confirmation>()
            session.requiredStringListener?.let {
                    listener: AppEntityListener<String> ->
                builder.registerAppEntityTaskParam(
                    "required",
                    listener,
                    TypeConverters.STRING_PARAM_VALUE_CONVERTER,
                    EntityConverter.of(TypeSpec.STRING_TYPE_SPEC),
                    getTrivialSearchActionConverter()
                )
            }
            builder.build()
        }
    }

    companion object {

        private val AUTO_ACCEPT_STRING_VALUE: AppEntityListener<String> =
            object : AppEntityListener<String> {
                override fun lookupAndRenderAsync(
                    searchAction: SearchAction<String>
                ): ListenableFuture<EntitySearchResult<String>> {
                    val result: EntitySearchResult.Builder<String> =
                        EntitySearchResult.Builder()
                    return Futures.immediateFuture(
                        result.addPossibleValue("valid1").build()
                    )
                }

                override fun onReceivedAsync(
                    value: String
                ): ListenableFuture<ValidationResult> {
                    return Futures.immediateFuture(ValidationResult.newAccepted())
                }
            }
        private val AUTO_REJECT_STRING_VALUE: AppEntityListener<String> =
            object : AppEntityListener<String> {
                override fun lookupAndRenderAsync(
                    searchAction: SearchAction<String>
                ): ListenableFuture<EntitySearchResult<String>> {
                    val result: EntitySearchResult.Builder<String> =
                        EntitySearchResult.Builder()
                    return Futures.immediateFuture(
                        result.addPossibleValue("valid1").build()
                    )
                }

                override fun onReceivedAsync(
                    value: String
                ): ListenableFuture<ValidationResult> {
                    return Futures.immediateFuture(ValidationResult.newRejected())
                }
            }

        private fun <T> getTrivialSearchActionConverter() = SearchActionConverter {
            SearchAction.Builder<T>().build()
        }

        private const val CAPABILITY_NAME = "actions.intent.TEST"
        private val ACTION_SPEC: ActionSpec<Arguments, Output> =
            ActionSpecBuilder.ofCapabilityNamed(
                CAPABILITY_NAME
            )
                .setArguments(Arguments::class.java, Arguments::Builder, Arguments.Builder::build)
                .setOutput(Output::class.java)
                .bindParameter(
                    "required",
                    Arguments::requiredStringField,
                    Arguments.Builder::setRequiredStringField,
                    TypeConverters.STRING_PARAM_VALUE_CONVERTER
                )
                .bindParameter(
                    "optional",
                    Arguments::optionalStringField,
                    Arguments.Builder::setOptionalStringField,
                    TypeConverters.STRING_PARAM_VALUE_CONVERTER
                )
                .bindRepeatedParameter(
                    "repeated",
                    Arguments::repeatedStringField,
                    Arguments.Builder::setRepeatedStringField,
                    TypeConverters.STRING_PARAM_VALUE_CONVERTER
                )
                .bindOutput(
                    "optionalStringOutput",
                    Output::optionalStringField,
                    TypeConverters.STRING_PARAM_VALUE_CONVERTER::toParamValue
                )
                .bindRepeatedOutput(
                    "repeatedStringOutput",
                    Output::repeatedStringField,
                    TypeConverters.STRING_PARAM_VALUE_CONVERTER::toParamValue
                )
                .build()

        private val SINGLE_REQUIRED_FIELD_BOUND_PROPERTIES = listOf(
            BoundProperty(
                "required",
                Property<StringValue>(isRequiredForExecution = true),
                TypeConverters.STRING_VALUE_ENTITY_CONVERTER)
        )

        private fun getCurrentValues(
            argName: String,
            appDialogState: AppDialogState
        ): List<CurrentValue> {
            return appDialogState
                .paramsList
                .stream()
                .filter { dialogParam -> dialogParam.name.equals(argName) }
                .findFirst()
                .orElse(DialogParameter.getDefaultInstance())
                .currentValueList
        }

        /**
         * Create a capability instance templated with Property, Argument, Output, Confirmation
         * etc., defined under ../../testing/spec
         */
        private fun <SessionUpdaterT : AbstractTaskUpdater> createCapability(
            boundProperties: List<BoundProperty<*>>,
            sessionFactory: (hostProperties: HostProperties?) -> ExecutionSession,
            sessionBridge: SessionBridge<ExecutionSession, Arguments, Confirmation>,
            sessionUpdaterSupplier: Supplier<SessionUpdaterT>
        ): TaskCapabilityImpl<
            Arguments,
            Output,
            ExecutionSession,
            Confirmation,
            SessionUpdaterT
            > {
            return TaskCapabilityImpl(
                "id",
                ACTION_SPEC,
                boundProperties,
                sessionFactory,
                sessionBridge,
                sessionUpdaterSupplier
            )
        }
    }
}
