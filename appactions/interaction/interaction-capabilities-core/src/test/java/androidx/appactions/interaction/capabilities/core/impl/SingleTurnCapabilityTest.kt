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

package androidx.appactions.interaction.capabilities.core.impl

import android.util.SizeF
import androidx.appactions.interaction.capabilities.core.ExecutionCallback
import androidx.appactions.interaction.capabilities.core.ExecutionCallbackAsync
import androidx.appactions.interaction.capabilities.core.toExecutionCallback
import androidx.appactions.interaction.capabilities.core.ExecutionResult
import androidx.appactions.interaction.capabilities.core.HostProperties
import androidx.appactions.interaction.capabilities.core.impl.concurrent.Futures
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpec
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecBuilder
import androidx.appactions.interaction.capabilities.core.properties.Property
import androidx.appactions.interaction.capabilities.core.properties.StringValue
import androidx.appactions.interaction.capabilities.testing.internal.ArgumentUtils
import androidx.appactions.interaction.capabilities.testing.internal.FakeCallbackInternal
import androidx.appactions.interaction.capabilities.testing.internal.TestingUtils.CB_TIMEOUT
import androidx.appactions.interaction.capabilities.testing.internal.TestingUtils.BLOCKING_TIMEOUT
import androidx.appactions.interaction.capabilities.core.testing.spec.Arguments
import androidx.appactions.interaction.capabilities.core.testing.spec.Output
import androidx.appactions.interaction.capabilities.core.testing.spec.Properties
import androidx.appactions.interaction.proto.AppActionsContext.AppAction
import androidx.appactions.interaction.proto.AppActionsContext.IntentParameter
import androidx.appactions.interaction.proto.FulfillmentResponse
import androidx.appactions.interaction.proto.FulfillmentResponse.StructuredOutput
import androidx.appactions.interaction.proto.FulfillmentResponse.StructuredOutput.OutputValue
import androidx.appactions.interaction.proto.ParamValue
import androidx.appactions.interaction.proto.TaskInfo
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SingleTurnCapabilityTest {
    private val hostProperties =
        HostProperties.Builder().setMaxHostSizeDp(SizeF(300f, 500f)).build()
    private val fakeSessionId = "fakeSessionId"

    @Test
    fun appAction_computedProperty() {
        val mutableEntityList = mutableListOf<StringValue>()
        val capability = SingleTurnCapabilityImpl(
            id = "capabilityId",
            actionSpec = ACTION_SPEC,
            property = Properties.newBuilder()
                .setRequiredStringField(
                    Property.Builder<StringValue>().setPossibleValueSupplier(
                        mutableEntityList::toList
                    ).build()
                )
                .build(),
            executionCallback = ExecutionCallback<Arguments, Output> {
                ExecutionResult.Builder<Output>().build()
            }
        )
        mutableEntityList.add(StringValue.of("entity1"))

        assertThat(capability.appAction).isEqualTo(
            AppAction.newBuilder()
                .setIdentifier("capabilityId")
                .setName("actions.intent.TEST")
                .addParams(
                    IntentParameter.newBuilder()
                        .setName("requiredString")
                        .addPossibleEntities(
                            androidx.appactions.interaction.proto.Entity.newBuilder()
                                .setIdentifier("entity1")
                                .setName("entity1")
                        )
                )
                .setTaskInfo(TaskInfo.newBuilder().setSupportsPartialFulfillment(false))
                .build()
        )

        mutableEntityList.add(StringValue.of("entity2"))
        assertThat(capability.appAction).isEqualTo(
            AppAction.newBuilder()
                .setIdentifier("capabilityId")
                .setName("actions.intent.TEST")
                .addParams(
                    IntentParameter.newBuilder()
                        .setName("requiredString")
                        .addPossibleEntities(
                            androidx.appactions.interaction.proto.Entity.newBuilder()
                                .setIdentifier("entity1")
                                .setName("entity1")
                        )
                        .addPossibleEntities(
                            androidx.appactions.interaction.proto.Entity.newBuilder()
                                .setIdentifier("entity2")
                                .setName("entity2")
                        )
                )
                .setTaskInfo(TaskInfo.newBuilder().setSupportsPartialFulfillment(false))
                .build()
        )
    }

    @Test
    fun oneShotCapability_successWithOutput() {
        val executionCallback =
            ExecutionCallback<Arguments, Output> {
                ExecutionResult.Builder<Output>()
                    .setOutput(
                        Output.builder().setOptionalStringField("stringOutput").build()
                    )
                    .build()
            }
        val capability =
            SingleTurnCapabilityImpl(
                id = "capabilityId",
                actionSpec = ACTION_SPEC,
                property =
                Properties.newBuilder()
                    .setRequiredStringField(
                        Property.Builder<StringValue>().build()
                    )
                    .setOptionalStringField(Property.prohibited())
                    .build(),
                executionCallback = executionCallback
            )

        val capabilitySession = capability.createSession(fakeSessionId, hostProperties)
        assertThat(capabilitySession.sessionId).isEqualTo(fakeSessionId)
        assertThat(capabilitySession.state).isNull()
        assertThat(capabilitySession.isActive).isTrue()

        val callbackInternal = FakeCallbackInternal(CB_TIMEOUT)
        capabilitySession.execute(
            ArgumentUtils.buildArgs(
                mapOf(
                    "optionalString" to
                        ParamValue.newBuilder().setStringValue("string argument value").build()
                )
            ),
            callbackInternal
        )

        assertThat(capabilitySession.isActive).isFalse()
        val response = callbackInternal.receiveResponse()
        assertThat(response.fulfillmentResponse).isNotNull()
        assertThat(response.fulfillmentResponse)
            .isEqualTo(
                FulfillmentResponse.newBuilder()
                    .setExecutionOutput(
                        StructuredOutput.newBuilder()
                            .addOutputValues(
                                OutputValue.newBuilder()
                                    .setName("optionalStringOutput")
                                    .addValues(
                                        ParamValue.newBuilder()
                                            .setStringValue("stringOutput")
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
    }

    @Test
    fun oneShotCapability_failure() {
        val executionCallback =
            ExecutionCallback<Arguments, Output> { throw IllegalStateException("") }
        val capability =
            SingleTurnCapabilityImpl(
                id = "capabilityId",
                actionSpec = ACTION_SPEC,
                property =
                Properties.newBuilder()
                    .setRequiredStringField(
                        Property.Builder<StringValue>().build()
                    )
                    .setOptionalStringField(Property.prohibited())
                    .build(),
                executionCallback = executionCallback
            )

        val capabilitySession = capability.createSession(fakeSessionId, hostProperties)
        val callbackInternal = FakeCallbackInternal(CB_TIMEOUT)
        capabilitySession.execute(
            ArgumentUtils.buildArgs(
                mapOf(
                    "optionalString" to
                        ParamValue.newBuilder().setStringValue("string argument value").build()
                )
            ),
            callbackInternal
        )

        val response = callbackInternal.receiveResponse()
        assertThat(response.errorStatus).isNotNull()
        assertThat(response.errorStatus).isEqualTo(ErrorStatusInternal.CANCELLED)
    }

    @Test
    fun oneShotSession_uiHandle_withExecutionCallback() {
        val executionCallback =
            ExecutionCallback<Arguments, Output> { ExecutionResult.Builder<Output>().build() }
        val capability =
            SingleTurnCapabilityImpl(
                id = "capabilityId",
                actionSpec = ACTION_SPEC,
                property =
                Properties.newBuilder()
                    .setRequiredStringField(
                        Property.Builder<StringValue>().build()
                    )
                    .build(),
                executionCallback = executionCallback
            )
        val session = capability.createSession(fakeSessionId, hostProperties)
        assertThat(session.uiHandle).isSameInstanceAs(executionCallback)
    }

    @Test
    fun oneShotSession_uiHandle_withExecutionCallbackAsync() {
        val executionCallbackAsync =
            ExecutionCallbackAsync<Arguments, Output> {
                Futures.immediateFuture(ExecutionResult.Builder<Output>().build())
            }
        val capability =
            SingleTurnCapabilityImpl(
                id = "capabilityId",
                actionSpec = ACTION_SPEC,
                property =
                Properties.newBuilder()
                    .setRequiredStringField(
                        Property.Builder<StringValue>().build()
                    )
                    .build(),
                executionCallback = executionCallbackAsync.toExecutionCallback()
            )
        val session = capability.createSession(fakeSessionId, hostProperties)
        assertThat(session.uiHandle).isSameInstanceAs(executionCallbackAsync)
    }

    @Test
    fun multipleSessions_sequentialExecution(): Unit = runBlocking {
        val executionResultChannel = Channel<ExecutionResult<Output>>()
        val argumentChannel = Channel<Arguments>()

        val executionCallback = ExecutionCallback<Arguments, Output> {
            argumentChannel.send(it)
            executionResultChannel.receive()
        }
        val capability = SingleTurnCapabilityImpl(
            id = "capabilityId",
            actionSpec = ACTION_SPEC,
            property = Properties.newBuilder().setRequiredStringField(
                Property.Builder<StringValue>().build()
            ).build(),
            executionCallback = executionCallback
        )
        val session1 = capability.createSession("session1", hostProperties)
        val session2 = capability.createSession("session2", hostProperties)

        val callbackInternal1 = FakeCallbackInternal(CB_TIMEOUT)
        val callbackInternal2 = FakeCallbackInternal(CB_TIMEOUT)

        session1.execute(
            ArgumentUtils.buildArgs(
                mapOf(
                    "optionalString" to
                        ParamValue.newBuilder().setStringValue("string value 1").build()
                )
            ),
            callbackInternal1
        )
        session2.execute(
            ArgumentUtils.buildArgs(
                mapOf(
                    "optionalString" to
                        ParamValue.newBuilder().setStringValue("string value 2").build()
                )
            ),
            callbackInternal2
        )

        // verify ExecutionCallback receives 1st request.
        assertThat(argumentChannel.receive()).isEqualTo(
            Arguments.newBuilder().setOptionalStringField("string value 1").build()
        )
        // verify the 2nd request cannot be received due to mutex.
        assertThat(withTimeoutOrNull(BLOCKING_TIMEOUT) { argumentChannel.receive() }).isNull()

        // unblock first request handling.
        executionResultChannel.send(ExecutionResult.Builder<Output>().build())
        assertThat(callbackInternal1.receiveResponse().fulfillmentResponse).isEqualTo(
            FulfillmentResponse.getDefaultInstance()
        )

        assertThat(argumentChannel.receive()).isEqualTo(
            Arguments.newBuilder().setOptionalStringField("string value 2").build()
        )
        executionResultChannel.send(ExecutionResult.Builder<Output>().build())
        assertThat(callbackInternal2.receiveResponse().fulfillmentResponse).isEqualTo(
            FulfillmentResponse.getDefaultInstance()
        )
    }

    companion object {
        val ACTION_SPEC: ActionSpec<Properties, Arguments, Output> =
            ActionSpecBuilder.ofCapabilityNamed(
                "actions.intent.TEST"
            )
                .setDescriptor(Properties::class.java)
                .setArguments(Arguments::class.java, Arguments::newBuilder)
                .setOutput(Output::class.java)
                .bindParameter(
                    "requiredString",
                    Properties::requiredStringField,
                    Arguments.Builder::setRequiredStringField,
                    TypeConverters.STRING_PARAM_VALUE_CONVERTER,
                    TypeConverters.STRING_VALUE_ENTITY_CONVERTER
                )
                .bindOptionalParameter(
                    "optionalString",
                    Properties::optionalStringField,
                    Arguments.Builder::setOptionalStringField,
                    TypeConverters.STRING_PARAM_VALUE_CONVERTER,
                    TypeConverters.STRING_VALUE_ENTITY_CONVERTER
                )
                .bindOptionalOutput(
                    "optionalStringOutput",
                    Output::optionalStringField,
                    TypeConverters.STRING_PARAM_VALUE_CONVERTER::toParamValue
                )
                .build()
    }
}
