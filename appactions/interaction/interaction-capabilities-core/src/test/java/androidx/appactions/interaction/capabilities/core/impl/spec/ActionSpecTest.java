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
import androidx.appactions.interaction.capabilities.core.impl.BuilderOf;
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters;
import androidx.appactions.interaction.capabilities.core.properties.Entity;
import androidx.appactions.interaction.capabilities.core.properties.EntityProperty;
import androidx.appactions.interaction.capabilities.core.properties.EnumProperty;
import androidx.appactions.interaction.capabilities.core.properties.StringProperty;
import androidx.appactions.interaction.capabilities.core.testing.spec.Output;
import androidx.appactions.interaction.capabilities.core.values.EntityValue;
import androidx.appactions.interaction.proto.AppActionsContext.AppAction;
import androidx.appactions.interaction.proto.AppActionsContext.IntentParameter;
import androidx.appactions.interaction.proto.FulfillmentResponse.StructuredOutput;
import androidx.appactions.interaction.proto.FulfillmentResponse.StructuredOutput.OutputValue;
import androidx.appactions.interaction.proto.ParamValue;

import com.google.auto.value.AutoValue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;
import java.util.Optional;

@RunWith(JUnit4.class)
public final class ActionSpecTest {

    private static final ActionSpec<Property, Argument, Output> ACTION_SPEC =
            ActionSpecBuilder.ofCapabilityNamed("actions.intent.TEST")
                    .setDescriptor(Property.class)
                    .setArgument(Argument.class, Argument::newBuilder)
                    .setOutput(Output.class)
                    .bindRequiredEntityParameter(
                            "requiredEntity",
                            Property::requiredEntityField,
                            Argument.Builder::setRequiredEntityField)
                    .bindOptionalEntityParameter(
                            "optionalEntity",
                            Property::optionalEntityField,
                            Argument.Builder::setOptionalEntityField)
                    .bindOptionalEnumParameter(
                            "optionalEnum",
                            TestEnum.class,
                            Property::optionalEnumField,
                            Argument.Builder::setOptionalEnumField)
                    .bindRepeatedEntityParameter(
                            "repeatedEntity",
                            Property::repeatedEntityField,
                            Argument.Builder::setRepeatedEntityField)
                    .bindRequiredStringParameter(
                            "requiredString",
                            Property::requiredStringField,
                            Argument.Builder::setRequiredStringField)
                    .bindOptionalStringParameter(
                            "optionalString",
                            Property::optionalStringField,
                            Argument.Builder::setOptionalStringField)
                    .bindRepeatedStringParameter(
                            "repeatedString",
                            Property::repeatedStringField,
                            Argument.Builder::setRepeatedStringField)
                    .bindOptionalOutput(
                            "optionalStringOutput",
                            Output::optionalStringField,
                            TypeConverters::toParamValue)
                    .bindRepeatedOutput(
                            "repeatedStringOutput",
                            Output::repeatedStringField,
                            TypeConverters::toParamValue)
                    .build();

    @Test
    public void getAppAction_onlyRequiredProperty() {
        Property property =
                Property.create(
                        EntityProperty.newBuilder()
                                .addPossibleEntity(
                                        new Entity.Builder()
                                                .setId("contact_2")
                                                .setName("Donald")
                                                .setAlternateNames("Duck")
                                                .build())
                                .setValueMatchRequired(true)
                                .build(),
                        StringProperty.EMPTY);

        assertThat(ACTION_SPEC.convertPropertyToProto(property))
                .isEqualTo(
                        AppAction.newBuilder()
                                .setName("actions.intent.TEST")
                                .addParams(
                                        IntentParameter.newBuilder()
                                                .setName("requiredEntity")
                                                .addPossibleEntities(
                                                        androidx.appactions.interaction.proto.Entity
                                                                .newBuilder()
                                                                .setIdentifier("contact_2")
                                                                .setName("Donald")
                                                                .addAlternateNames("Duck")
                                                                .build())
                                                .setIsRequired(false)
                                                .setEntityMatchRequired(true))
                                .addParams(IntentParameter.newBuilder().setName("requiredString"))
                                .build());
    }

    @Test
    public void getAppAction_allProperties() {
        Property property =
                Property.create(
                        EntityProperty.newBuilder()
                                .addPossibleEntity(
                                        new Entity.Builder()
                                                .setId("contact_2")
                                                .setName("Donald")
                                                .setAlternateNames("Duck")
                                                .build())
                                .build(),
                        Optional.of(
                                EntityProperty.newBuilder()
                                        .addPossibleEntity(
                                                new Entity.Builder()
                                                        .setId("entity1")
                                                        .setName("optional possible entity")
                                                        .build())
                                        .setIsRequired(true)
                                        .build()),
                        Optional.of(
                                EnumProperty.newBuilder(TestEnum.class)
                                        .addSupportedEnumValues(TestEnum.VALUE_1)
                                        .setIsRequired(true)
                                        .build()),
                        Optional.of(
                                EntityProperty.newBuilder()
                                        .addPossibleEntity(
                                                new Entity.Builder()
                                                        .setId("entity1")
                                                        .setName("repeated entity1")
                                                        .build())
                                        .addPossibleEntity(
                                                new Entity.Builder()
                                                        .setId("entity2")
                                                        .setName("repeated entity2")
                                                        .build())
                                        .setIsRequired(true)
                                        .build()),
                        StringProperty.EMPTY,
                        Optional.of(
                                StringProperty.newBuilder()
                                        .addPossibleValue("value1")
                                        .setValueMatchRequired(true)
                                        .setIsRequired(true)
                                        .build()),
                        Optional.of(StringProperty.PROHIBITED));

        assertThat(ACTION_SPEC.convertPropertyToProto(property))
                .isEqualTo(
                        AppAction.newBuilder()
                                .setName("actions.intent.TEST")
                                .addParams(
                                        IntentParameter.newBuilder()
                                                .setName("requiredEntity")
                                                .addPossibleEntities(
                                                        androidx.appactions.interaction.proto.Entity
                                                                .newBuilder()
                                                                .setIdentifier("contact_2")
                                                                .setName("Donald")
                                                                .addAlternateNames("Duck")
                                                                .build())
                                                .setIsRequired(false)
                                                .setEntityMatchRequired(false))
                                .addParams(
                                        IntentParameter.newBuilder()
                                                .setName("optionalEntity")
                                                .addPossibleEntities(
                                                        androidx.appactions.interaction.proto.Entity
                                                                .newBuilder()
                                                                .setIdentifier("entity1")
                                                                .setName("optional possible entity")
                                                                .build())
                                                .setIsRequired(true)
                                                .setEntityMatchRequired(false))
                                .addParams(
                                        IntentParameter.newBuilder()
                                                .setName("optionalEnum")
                                                .addPossibleEntities(
                                                        androidx.appactions.interaction.proto.Entity
                                                                .newBuilder()
                                                                .setIdentifier(
                                                                        TestEnum.VALUE_1.toString())
                                                                .build())
                                                .setIsRequired(true)
                                                .setEntityMatchRequired(true))
                                .addParams(
                                        IntentParameter.newBuilder()
                                                .setName("repeatedEntity")
                                                .addPossibleEntities(
                                                        androidx.appactions.interaction.proto.Entity
                                                                .newBuilder()
                                                                .setIdentifier("entity1")
                                                                .setName("repeated entity1")
                                                                .build())
                                                .addPossibleEntities(
                                                        androidx.appactions.interaction.proto.Entity
                                                                .newBuilder()
                                                                .setIdentifier("entity2")
                                                                .setName("repeated entity2")
                                                                .build())
                                                .setIsRequired(true)
                                                .setEntityMatchRequired(false))
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
    @SuppressWarnings("JdkImmutableCollections")
    public void convertOutputToProto_string() {
        Output output =
                Output.builder()
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

    enum TestEnum {
        VALUE_1,
        VALUE_2,
    }

    @AutoValue
    abstract static class Argument {

        static Builder newBuilder() {
            return new AutoValue_ActionSpecTest_Argument.Builder();
        }

        abstract EntityValue requiredEntityField();

        abstract EntityValue optionalEntityField();

        abstract TestEnum optionalEnumField();

        abstract List<EntityValue> repeatedEntityField();

        abstract String requiredStringField();

        abstract String optionalStringField();

        abstract List<String> repeatedStringField();

        @AutoValue.Builder
        abstract static class Builder implements BuilderOf<Argument> {

            abstract Builder setRequiredEntityField(EntityValue value);

            abstract Builder setOptionalEntityField(EntityValue value);

            abstract Builder setOptionalEnumField(TestEnum value);

            abstract Builder setRepeatedEntityField(List<EntityValue> repeated);

            abstract Builder setRequiredStringField(String value);

            abstract Builder setOptionalStringField(String value);

            abstract Builder setRepeatedStringField(List<String> repeated);

            @NonNull
            @Override
            public abstract Argument build();
        }
    }

    @AutoValue
    abstract static class Property {

        static Property create(
                EntityProperty requiredEntityField,
                Optional<EntityProperty> optionalEntityField,
                Optional<EnumProperty<TestEnum>> optionalEnumField,
                Optional<EntityProperty> repeatedEntityField,
                StringProperty requiredStringField,
                Optional<StringProperty> optionalStringField,
                Optional<StringProperty> repeatedStringField) {
            return new AutoValue_ActionSpecTest_Property(
                    requiredEntityField,
                    optionalEntityField,
                    optionalEnumField,
                    repeatedEntityField,
                    requiredStringField,
                    optionalStringField,
                    repeatedStringField);
        }

        static Property create(
                EntityProperty requiredEntityField, StringProperty requiredStringField) {
            return create(
                    requiredEntityField,
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    requiredStringField,
                    Optional.empty(),
                    Optional.empty());
        }

        abstract EntityProperty requiredEntityField();

        abstract Optional<EntityProperty> optionalEntityField();

        abstract Optional<EnumProperty<TestEnum>> optionalEnumField();

        abstract Optional<EntityProperty> repeatedEntityField();

        abstract StringProperty requiredStringField();

        abstract Optional<StringProperty> optionalStringField();

        abstract Optional<StringProperty> repeatedStringField();
    }
}
