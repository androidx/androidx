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
import androidx.appactions.interaction.capabilities.core.CapabilityExecutor
import androidx.appactions.interaction.capabilities.core.CapabilityExecutorAsync
import androidx.appactions.interaction.capabilities.core.toCapabilityExecutor
import androidx.appactions.interaction.capabilities.core.ExecutionResult
import androidx.appactions.interaction.capabilities.core.HostProperties
import androidx.appactions.interaction.capabilities.core.impl.concurrent.Futures
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpec
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecBuilder
import androidx.appactions.interaction.capabilities.core.properties.Entity
import androidx.appactions.interaction.capabilities.core.properties.Property
import androidx.appactions.interaction.capabilities.testing.internal.ArgumentUtils
import androidx.appactions.interaction.capabilities.testing.internal.FakeCallbackInternal
import androidx.appactions.interaction.capabilities.testing.internal.TestingUtils.CB_TIMEOUT
import androidx.appactions.interaction.capabilities.testing.internal.TestingUtils.BLOCKING_TIMEOUT
import androidx.appactions.interaction.capabilities.core.testing.spec.Arguments
import androidx.appactions.interaction.capabilities.core.testing.spec.Output
import androidx.appactions.interaction.capabilities.core.testing.spec.Properties
import androidx.appactions.interaction.proto.FulfillmentResponse
import androidx.appactions.interaction.proto.FulfillmentResponse.StructuredOutput
import androidx.appactions.interaction.proto.FulfillmentResponse.StructuredOutput.OutputValue
import androidx.appactions.interaction.proto.ParamValue
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SingleTurnCapabilityTest {
    private val hostProperties =
        HostProperties.Builder().setMaxHostSizeDp(SizeF(300f, 500f)).build()
    private val fakeSessionId = "fakeSessionId"

    @Test
    fun oneShotCapability_successWithOutput() {
        val capabilityExecutor =
            CapabilityExecutor<Arguments, Output> {
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
                    .setRequiredEntityField(
                        Property.Builder<Entity>().build()
                    )
                    .setOptionalStringField(Property.prohibited())
                    .build(),
                capabilityExecutor = capabilityExecutor
            )

        val capabilitySession = capability.createSession(fakeSessionId, hostProperties)
        assertThat(capabilitySession.sessionId).isEqualTo(fakeSessionId)

        val callbackInternal = FakeCallbackInternal(CB_TIMEOUT)
        capabilitySession.execute(
            ArgumentUtils.buildArgs(
                mapOf(
                    "optionalString" to
                        ParamValue.newBuilder().setIdentifier("string argument value").build()
                )
            ),
            callbackInternal
        )

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
        val capabilityExecutor =
            CapabilityExecutor<Arguments, Output> { throw IllegalStateException("") }
        val capability =
            SingleTurnCapabilityImpl(
                id = "capabilityId",
                actionSpec = ACTION_SPEC,
                property =
                Properties.newBuilder()
                    .setRequiredEntityField(
                        Property.Builder<Entity>().build()
                    )
                    .setOptionalStringField(Property.prohibited())
                    .build(),
                capabilityExecutor = capabilityExecutor
            )

        val capabilitySession = capability.createSession(fakeSessionId, hostProperties)
        val callbackInternal = FakeCallbackInternal(CB_TIMEOUT)
        capabilitySession.execute(
            ArgumentUtils.buildArgs(
                mapOf(
                    "optionalString" to
                        ParamValue.newBuilder().setIdentifier("string argument value").build()
                )
            ),
            callbackInternal
        )

        val response = callbackInternal.receiveResponse()
        assertThat(response.errorStatus).isNotNull()
        assertThat(response.errorStatus).isEqualTo(ErrorStatusInternal.CANCELLED)
    }

    @Test
    fun oneShotSession_uiHandle_withCapabilityExecutor() {
        val capabilityExecutor =
            CapabilityExecutor<Arguments, Output> { ExecutionResult.Builder<Output>().build() }
        val capability =
            SingleTurnCapabilityImpl(
                id = "capabilityId",
                actionSpec = ACTION_SPEC,
                property =
                Properties.newBuilder()
                    .setRequiredEntityField(
                        Property.Builder<Entity>().build()
                    )
                    .build(),
                capabilityExecutor = capabilityExecutor
            )
        val session = capability.createSession(fakeSessionId, hostProperties)
        assertThat(session.uiHandle).isSameInstanceAs(capabilityExecutor)
    }

    @Test
    fun oneShotSession_uiHandle_withCapabilityExecutorAsync() {
        val capabilityExecutorAsync =
            CapabilityExecutorAsync<Arguments, Output> {
                Futures.immediateFuture(ExecutionResult.Builder<Output>().build())
            }
        val capability =
            SingleTurnCapabilityImpl(
                id = "capabilityId",
                actionSpec = ACTION_SPEC,
                property =
                Properties.newBuilder()
                    .setRequiredEntityField(
                        Property.Builder<Entity>().build()
                    )
                    .build(),
                capabilityExecutor = capabilityExecutorAsync.toCapabilityExecutor()
            )
        val session = capability.createSession(fakeSessionId, hostProperties)
        assertThat(session.uiHandle).isSameInstanceAs(capabilityExecutorAsync)
    }

    @Ignore // b/277121577
    @Test
    fun multipleSessions_sequentialExecution(): Unit = runBlocking {
        val executionResultChannel = Channel<ExecutionResult<Output>>()
        val argumentChannel = Channel<Arguments>()

        val capabilityExecutor = CapabilityExecutor<Arguments, Output> {
            argumentChannel.send(it)
            executionResultChannel.receive()
        }
        val capability = SingleTurnCapabilityImpl(
            id = "capabilityId",
            actionSpec = ACTION_SPEC,
            property = Properties.newBuilder().setRequiredEntityField(
                Property.Builder<Entity>().build()
            ).build(),
            capabilityExecutor = capabilityExecutor
        )
        val session1 = capability.createSession("session1", hostProperties)
        val session2 = capability.createSession("session2", hostProperties)

        val callbackInternal1 = FakeCallbackInternal(CB_TIMEOUT)
        val callbackInternal2 = FakeCallbackInternal(CB_TIMEOUT)

        session1.execute(
            ArgumentUtils.buildArgs(
                mapOf(
                    "optionalString" to
                        ParamValue.newBuilder().setIdentifier("string value 1").build()
                )
            ),
            callbackInternal1
        )
        session2.execute(
            ArgumentUtils.buildArgs(
                mapOf(
                    "optionalString" to
                        ParamValue.newBuilder().setIdentifier("string value 2").build()
                )
            ),
            callbackInternal2
        )

        // verify CapabilityExecutor receives 1st request.
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
