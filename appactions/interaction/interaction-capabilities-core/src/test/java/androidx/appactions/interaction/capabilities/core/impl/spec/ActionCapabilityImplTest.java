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

package androidx.appactions.interaction.capabilities.core.impl.spec;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;

import androidx.appactions.interaction.capabilities.core.ExecutionResult;
import androidx.appactions.interaction.capabilities.core.impl.ArgumentsWrapper;
import androidx.appactions.interaction.capabilities.core.impl.CallbackInternal;
import androidx.appactions.interaction.capabilities.core.impl.concurrent.Futures;
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters;
import androidx.appactions.interaction.capabilities.core.properties.EntityProperty;
import androidx.appactions.interaction.capabilities.core.testing.spec.Argument;
import androidx.appactions.interaction.capabilities.core.testing.spec.Output;
import androidx.appactions.interaction.capabilities.core.testing.spec.Property;
import androidx.appactions.interaction.proto.FulfillmentRequest.Fulfillment;
import androidx.appactions.interaction.proto.FulfillmentResponse;
import androidx.appactions.interaction.proto.FulfillmentResponse.StructuredOutput;
import androidx.appactions.interaction.proto.FulfillmentResponse.StructuredOutput.OutputValue;
import androidx.appactions.interaction.proto.ParamValue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.List;
import java.util.Optional;

@RunWith(JUnit4.class)
public final class ActionCapabilityImplTest {

    private static final ActionSpec<Property, Argument, Output> ACTION_SPEC =
            ActionSpecBuilder.ofCapabilityNamed("actions.intent.TEST")
                    .setDescriptor(Property.class)
                    .setArgument(Argument.class, Argument::newBuilder)
                    .setOutput(Output.class)
                    .bindRequiredEntityParameter(
                            "requiredEntity",
                            Property::requiredEntityField,
                            Argument.Builder::setRequiredEntityField)
                    .bindOptionalOutput(
                            "optionalStringOutput",
                            Output::optionalStringField,
                            TypeConverters::toParamValue)
                    .bindRepeatedOutput(
                            "repeatedStringOutput",
                            Output::repeatedStringField,
                            TypeConverters::toParamValue)
                    .build();
    @Rule public final MockitoRule mockito = MockitoJUnit.rule();
    @Captor ArgumentCaptor<FulfillmentResponse> mCaptor;
    @Mock private CallbackInternal mCallbackInternal;

    @Test
    @SuppressWarnings("JdkImmutableCollections")
    public void execute_convertExecutionResult() {
        Property property =
                Property.newBuilder()
                        .setRequiredEntityField(EntityProperty.newBuilder().build())
                        .build();

        ExecutionResult<Output> executionResult =
                ExecutionResult.<Output>newBuilderWithOutput()
                        .setOutput(
                                Output.builder()
                                        .setOptionalStringField("test2")
                                        .setRepeatedStringField(List.of("test3", "test4"))
                                        .build())
                        .build();
        ActionCapabilityImpl<Property, Argument, Output> capability =
                new ActionCapabilityImpl<>(
                        ACTION_SPEC,
                        Optional.of("id"),
                        property,
                        (argument) -> Futures.immediateFuture(executionResult));
        StructuredOutput expectedExecutionOutput =
                StructuredOutput.newBuilder()
                        .addOutputValues(
                                OutputValue.newBuilder()
                                        .setName("optionalStringOutput")
                                        .addValues(
                                                ParamValue.newBuilder()
                                                        .setStringValue("test2")
                                                        .build())
                                        .build())
                        .addOutputValues(
                                OutputValue.newBuilder()
                                        .setName("repeatedStringOutput")
                                        .addValues(
                                                ParamValue.newBuilder()
                                                        .setStringValue("test3")
                                                        .build())
                                        .addValues(
                                                ParamValue.newBuilder()
                                                        .setStringValue("test4")
                                                        .build())
                                        .build())
                        .build();

        capability.execute(
                ArgumentsWrapper.create(Fulfillment.getDefaultInstance()), mCallbackInternal);

        verify(mCallbackInternal).onSuccess(mCaptor.capture());
        assertThat(mCaptor.getValue().getExecutionOutput().getOutputValuesList())
                .containsExactlyElementsIn(expectedExecutionOutput.getOutputValuesList());
    }
}
