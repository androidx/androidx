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
import androidx.appactions.interaction.capabilities.core.ActionCapability
import androidx.appactions.interaction.capabilities.core.ActionExecutor
import androidx.appactions.interaction.capabilities.core.ActionExecutorAsync
import androidx.appactions.interaction.capabilities.core.ActionExecutorAsync.Companion.toActionExecutorAsync
import androidx.appactions.interaction.capabilities.core.ExecutionResult
import androidx.appactions.interaction.capabilities.core.HostProperties
import androidx.appactions.interaction.capabilities.core.impl.concurrent.Futures
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpec
import androidx.appactions.interaction.capabilities.core.impl.spec.ActionSpecBuilder
import androidx.appactions.interaction.capabilities.core.properties.EntityProperty
import androidx.appactions.interaction.capabilities.core.properties.StringProperty
import androidx.appactions.interaction.capabilities.core.testing.ArgumentUtils
import androidx.appactions.interaction.capabilities.core.testing.spec.Argument
import androidx.appactions.interaction.capabilities.core.testing.spec.Output
import androidx.appactions.interaction.capabilities.core.testing.spec.Property
import androidx.appactions.interaction.proto.FulfillmentResponse
import androidx.appactions.interaction.proto.FulfillmentResponse.StructuredOutput
import androidx.appactions.interaction.proto.FulfillmentResponse.StructuredOutput.OutputValue
import androidx.appactions.interaction.proto.ParamValue
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.verify
import org.mockito.kotlin.mock

@RunWith(JUnit4::class)
class SingleTurnCapabilityTest {
    val hostProperties = HostProperties.Builder().setMaxHostSizeDp(SizeF(300f, 500f)).build()
    val mockCalback: CallbackInternal = mock()

    @Ignore // b/271033076
    @Test
    fun oneShotCapability_successWithOutput() {
        val actionExecutor = object : ActionExecutor<Argument, Output> {
            override suspend fun execute(argument: Argument): ExecutionResult<Output> =
                ExecutionResult.Builder<Output>().setOutput(
                    Output.builder().setOptionalStringField("stringOutput")
                        .build(),
                ).build()
        }
        val capability: ActionCapability =
            SingleTurnCapabilityImpl<Property, Argument, Output>(
                "capabilityId",
                ACTION_SPEC,
                Property.newBuilder().setRequiredEntityField(
                    EntityProperty.Builder().build(),
                ).setOptionalStringField(
                    StringProperty.Builder().setProhibited(true).build(),
                ).build(),
                actionExecutor.toActionExecutorAsync(),
            )
        val expectedFulfillmentResponse: FulfillmentResponse =
            FulfillmentResponse.newBuilder().setExecutionOutput(
                StructuredOutput.newBuilder()
                    .addOutputValues(
                        OutputValue.newBuilder()
                            .setName("optionalStringOutput")
                            .addValues(
                                ParamValue.newBuilder()
                                    .setStringValue("stringOutput")
                                    .build(),
                            )
                            .build(),
                    )
                    .build(),
            ).build()

        val capabilitySession = capability.createSession(hostProperties)
        capabilitySession.execute(
            ArgumentUtils.buildArgs(
                mapOf(
                    "optionalString" to ParamValue.newBuilder().setIdentifier(
                        "string argument value",
                    ).build(),
                ),
            ),
            mockCalback,
        )

        verify(mockCalback).onSuccess(expectedFulfillmentResponse)
    }

    @Test
    fun oneShotSession_uiHandle_withActionExecutor() {
        val actionExecutor =
            ActionExecutor<Argument, Output> {
                ExecutionResult.getDefaultInstance()
            }
        val capability: ActionCapability =
            SingleTurnCapabilityImpl<Property, Argument, Output>(
                "capabilityId",
                ACTION_SPEC,
                Property.newBuilder().setRequiredEntityField(
                    EntityProperty.Builder().build(),
                ).build(),
                actionExecutor.toActionExecutorAsync(),
            )
        val session = capability.createSession(hostProperties)
        assertThat(session.uiHandle).isSameInstanceAs(actionExecutor)
    }

    @Test
    fun oneShotSession_uiHandle_withActionExecutorAsync() {
        val actionExecutorAsync = ActionExecutorAsync<Argument, Output> {
            Futures.immediateFuture(ExecutionResult.getDefaultInstance())
        }
        val capability: ActionCapability =
            SingleTurnCapabilityImpl<Property, Argument, Output>(
                "capabilityId",
                ACTION_SPEC,
                Property.newBuilder().setRequiredEntityField(
                    EntityProperty.Builder().build(),
                ).build(),
                actionExecutorAsync,
            )
        val session = capability.createSession(hostProperties)
        assertThat(session.uiHandle).isSameInstanceAs(actionExecutorAsync)
    }

    companion object {
        val ACTION_SPEC: ActionSpec<Property, Argument, Output> =
            ActionSpecBuilder.ofCapabilityNamed(
                "actions.intent.TEST",
            )
                .setDescriptor(Property::class.java)
                .setArgument(Argument::class.java, Argument::newBuilder)
                .setOutput(Output::class.java)
                .bindOptionalStringParameter(
                    "optionalString",
                    Property::optionalStringField,
                    Argument.Builder::setOptionalStringField,
                )
                .bindOptionalOutput(
                    "optionalStringOutput",
                    Output::optionalStringField,
                    TypeConverters::toParamValue,
                ).build()
    }
}
