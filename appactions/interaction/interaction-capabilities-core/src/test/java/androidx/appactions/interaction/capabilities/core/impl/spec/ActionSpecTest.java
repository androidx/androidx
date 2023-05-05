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

import androidx.annotation.NonNull;
import androidx.appactions.interaction.capabilities.core.impl.converters.EntityConverter;
import androidx.appactions.interaction.capabilities.core.impl.converters.ParamValueConverter;
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters;
import androidx.appactions.interaction.capabilities.core.properties.Property;
import androidx.appactions.interaction.capabilities.core.properties.StringValue;
import androidx.appactions.interaction.capabilities.core.testing.spec.Arguments;
import androidx.appactions.interaction.capabilities.core.testing.spec.GenericEntityArguments;
import androidx.appactions.interaction.capabilities.core.testing.spec.Output;
import androidx.appactions.interaction.capabilities.core.testing.spec.TestEntity;
import androidx.appactions.interaction.proto.AppActionsContext.AppAction;
import androidx.appactions.interaction.proto.AppActionsContext.IntentParameter;
import androidx.appactions.interaction.proto.FulfillmentResponse.StructuredOutput;
import androidx.appactions.interaction.proto.FulfillmentResponse.StructuredOutput.OutputValue;
import androidx.appactions.interaction.proto.ParamValue;
import androidx.appactions.interaction.protobuf.Struct;
import androidx.appactions.interaction.protobuf.Value;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@RunWith(JUnit4.class)
@SuppressWarnings("unchecked")
public final class ActionSpecTest {
    private static final ActionSpec<Arguments, Output> ACTION_SPEC =
            ActionSpecBuilder.ofCapabilityNamed("actions.intent.TEST")
                    .setArguments(Arguments.class, Arguments.Builder::new)
                    .setOutput(Output.class)
                    .bindParameter(
                            "requiredString",
                            properties ->
                            {
                                return (Property<StringValue>) (properties.get(
                                        "requiredString"));
                            },
                            Arguments.Builder::setRequiredStringField,
                            TypeConverters.STRING_PARAM_VALUE_CONVERTER,
                            TypeConverters.STRING_VALUE_ENTITY_CONVERTER)
                    .bindOptionalParameter(
                            "optionalString",
                            properties ->
                            {
                                return Optional.ofNullable((Property<StringValue>) (properties.get(
                                        "optionalString")));
                            },
                            Arguments.Builder::setOptionalStringField,
                            TypeConverters.STRING_PARAM_VALUE_CONVERTER,
                            TypeConverters.STRING_VALUE_ENTITY_CONVERTER)
                    .bindRepeatedParameter(
                            "repeatedString",
                            properties ->
                            {
                                return Optional.ofNullable((Property<StringValue>) (properties.get(
                                        "repeatedString")));
                            },
                            Arguments.Builder::setRepeatedStringField,
                            TypeConverters.STRING_PARAM_VALUE_CONVERTER,
                            TypeConverters.STRING_VALUE_ENTITY_CONVERTER)
                    .bindOptionalOutput(
                            "optionalStringOutput",
                            output -> Optional.ofNullable(output.getOptionalStringField()),
                            TypeConverters.STRING_PARAM_VALUE_CONVERTER::toParamValue)
                    .bindRepeatedOutput(
                            "repeatedStringOutput",
                            Output::getRepeatedStringField,
                            TypeConverters.STRING_PARAM_VALUE_CONVERTER::toParamValue)
                    .build();
    private static final ParamValueConverter<TestEntity> TEST_ENTITY_PARAM_VALUE_CONVERTER =
            new ParamValueConverter<TestEntity>() {
                @NonNull
                @Override
                public ParamValue toParamValue(TestEntity type) {
                    return ParamValue.newBuilder()
                            .setStructValue(
                                    Struct.newBuilder()
                                            .putFields(
                                                    "name",
                                                    Value.newBuilder()
                                                            .setStringValue(type.getName())
                                                            .build())
                                            .build())
                            .build();
                }

                @Override
                public TestEntity fromParamValue(@NonNull ParamValue paramValue) {
                    String name =
                            paramValue.getStructValue().getFieldsOrThrow("name").getStringValue();
                    return new TestEntity.Builder().setName(name).build();
                }
            };
    private static final EntityConverter<TestEntity> TEST_ENTITY_CONVERTER =
            (testEntity) ->
                    androidx.appactions.interaction.proto.Entity.newBuilder()
                            .setIdentifier(testEntity.getId())
                            .setName(testEntity.getName())
                            .build();

    private static final ActionSpec<GenericEntityArguments, Output> GENERIC_TYPES_ACTION_SPEC =
            ActionSpecBuilder.ofCapabilityNamed("actions.intent.TEST")
                    .setArguments(GenericEntityArguments.class, GenericEntityArguments.Builder::new)
                    .setOutput(Output.class)
                    .bindParameter(
                            "requiredEntity",
                            properties -> {
                                return (Property<TestEntity>) (properties.get("requiredEntity"));
                            },
                            GenericEntityArguments.Builder::setSingularField,
                            TEST_ENTITY_PARAM_VALUE_CONVERTER,
                            TEST_ENTITY_CONVERTER)
                    .bindOptionalParameter(
                            "optionalEntity",
                            properties -> {
                                return Optional.of(
                                        (Property<TestEntity>) (properties.get("optionalEntity")));
                            },
                            GenericEntityArguments.Builder::setOptionalField,
                            TEST_ENTITY_PARAM_VALUE_CONVERTER,
                            TEST_ENTITY_CONVERTER)
                    .bindRepeatedParameter(
                            "repeatedEntities",
                            properties -> {
                                return Optional.of(
                                        (Property<TestEntity>)
                                                (properties.get("repeatedEntities")));
                            },
                            GenericEntityArguments.Builder::setRepeatedField,
                            TEST_ENTITY_PARAM_VALUE_CONVERTER,
                            TEST_ENTITY_CONVERTER)
                    .build();

    @Test
    public void getAppAction_genericParameters() {
        Map<String, Property<?>> property = new HashMap<>();
        property.put(
                "requiredEntity",
                new Property.Builder<TestEntity>()
                        .setRequired(true)
                        .setPossibleValues(
                                new TestEntity.Builder().setId("one").setName("one").build())
                        .build());
        property.put(
                "optionalEntity",
                new Property.Builder<TestEntity>()
                        .setRequired(true)
                        .setPossibleValues(
                                new TestEntity.Builder().setId("two").setName("two").build())
                        .build());
        property.put(
                "repeatedEntities",
                new Property.Builder<TestEntity>()
                        .setRequired(true)
                        .setPossibleValues(
                                new TestEntity.Builder().setId("three").setName("three").build())
                        .build());

        assertThat(GENERIC_TYPES_ACTION_SPEC.convertPropertyToProto(property))
                .isEqualTo(
                        AppAction.newBuilder()
                                .setName("actions.intent.TEST")
                                .addParams(
                                        IntentParameter.newBuilder()
                                                .setName("requiredEntity")
                                                .setIsRequired(true)
                                                .addPossibleEntities(
                                                        androidx.appactions.interaction.proto.Entity
                                                                .newBuilder()
                                                                .setIdentifier("one")
                                                                .setName("one")))
                                .addParams(
                                        IntentParameter.newBuilder()
                                                .setName("optionalEntity")
                                                .setIsRequired(true)
                                                .addPossibleEntities(
                                                        androidx.appactions.interaction.proto.Entity
                                                                .newBuilder()
                                                                .setIdentifier("two")
                                                                .setName("two")))
                                .addParams(
                                        IntentParameter.newBuilder()
                                                .setName("repeatedEntities")
                                                .setIsRequired(true)
                                                .addPossibleEntities(
                                                        androidx.appactions.interaction.proto.Entity
                                                                .newBuilder()
                                                                .setIdentifier("three")
                                                                .setName("three")))
                                .build());
    }

    @Test
    public void getAppAction_onlyRequiredProperty() {
        Map<String, Property<?>> property = new HashMap<>();
        property.put("requiredString",
                new Property.Builder<StringValue>()
                        .setPossibleValues(StringValue.of("Donald"))
                        .setValueMatchRequired(true)
                        .build());

        assertThat(ACTION_SPEC.convertPropertyToProto(property))
                .isEqualTo(
                        AppAction.newBuilder()
                                .setName("actions.intent.TEST")
                                .addParams(
                                        IntentParameter.newBuilder()
                                                .setName("requiredString")
                                                .setEntityMatchRequired(true)
                                                .addPossibleEntities(
                                                        androidx.appactions.interaction.proto.Entity
                                                                .newBuilder()
                                                                .setIdentifier("Donald")
                                                                .setName("Donald")))
                                .build());
    }

    @Test
    public void getAppAction_allProperties() {
        Map<String, Property<?>> property = new HashMap<>();
        property.put("requiredString",
                new Property.Builder<StringValue>().build());
        property.put("optionalString",
                new Property.Builder<StringValue>()
                        .setPossibleValues(StringValue.of("value1"))
                        .setValueMatchRequired(true)
                        .setRequired(true)
                        .build());
        property.put("repeatedString", Property.prohibited());

        assertThat(ACTION_SPEC.convertPropertyToProto(property))
                .isEqualTo(
                        AppAction.newBuilder()
                                .setName("actions.intent.TEST")
                                .addParams(IntentParameter.newBuilder().setName("requiredString"))
                                .addParams(
                                        IntentParameter.newBuilder()
                                                .setName("optionalString")
                                                .addPossibleEntities(
                                                        androidx.appactions.interaction.proto.Entity
                                                                .newBuilder()
                                                                .setIdentifier("value1")
                                                                .setName("value1")
                                                                .build())
                                                .setIsRequired(true)
                                                .setEntityMatchRequired(true))
                                .addParams(
                                        IntentParameter.newBuilder()
                                                .setName("repeatedString")
                                                .setIsProhibited(true))
                                .build());
    }

    @Test
    public void convertOutputToProto_string() {
        Output output =
                new Output.Builder()
                        .setOptionalStringField("test2")
                        .setRepeatedStringField(List.of("test3", "test4"))
                        .build();

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

        StructuredOutput executionOutput = ACTION_SPEC.convertOutputToProto(output);

        assertThat(executionOutput.getOutputValuesList())
                .containsExactlyElementsIn(expectedExecutionOutput.getOutputValuesList());
    }

    @Test
    public void convertOutputToProto_emptyOutput() {
        Output output =
                new Output.Builder().setRepeatedStringField(List.of("test3", "test4")).build();
        // No optionalStringOutput since it is not in the output above.
        StructuredOutput expectedExecutionOutput =
                StructuredOutput.newBuilder()
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

        StructuredOutput executionOutput = ACTION_SPEC.convertOutputToProto(output);

        assertThat(executionOutput.getOutputValuesList())
                .containsExactlyElementsIn(expectedExecutionOutput.getOutputValuesList());
    }
}
