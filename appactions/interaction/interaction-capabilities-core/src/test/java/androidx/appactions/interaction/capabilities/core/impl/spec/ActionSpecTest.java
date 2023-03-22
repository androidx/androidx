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
import androidx.appactions.interaction.capabilities.core.impl.converters.EntityConverter;
import androidx.appactions.interaction.capabilities.core.impl.converters.ParamValueConverter;
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters;
import androidx.appactions.interaction.capabilities.core.properties.Entity;
import androidx.appactions.interaction.capabilities.core.properties.StringValue;
import androidx.appactions.interaction.capabilities.core.properties.TypeProperty;
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
    private static final ParamValueConverter<String> STRING_PARAM_VALUE_CONVERTER =
            (paramValue) -> "test";
    private static final EntityConverter<String> STRING_ENTITY_CONVERTER =
            (theString) ->
                    androidx.appactions.interaction.proto.Entity.newBuilder()
                            .setIdentifier(theString)
                            .setName(theString)
                            .build();

    private static final ActionSpec<GenericEntityProperty, GenericEntityArgument, Output>
            GENERIC_TYPES_ACTION_SPEC =
                    ActionSpecBuilder.ofCapabilityNamed("actions.intent.TEST")
                            .setDescriptor(GenericEntityProperty.class)
                            .setArgument(
                                    GenericEntityArgument.class, GenericEntityArgument::newBuilder)
                            .setOutput(Output.class)
                            .bindRequiredGenericParameter(
                                    "requiredEntity",
                                    GenericEntityProperty::singularField,
                                    GenericEntityArgument.Builder::setSingularField,
                                    STRING_PARAM_VALUE_CONVERTER,
                                    STRING_ENTITY_CONVERTER)
                            .bindOptionalGenericParameter("optionalEntity",
                                    GenericEntityProperty::optionalField,
                                    GenericEntityArgument.Builder::setOptionalField,
                                    STRING_PARAM_VALUE_CONVERTER,
                                    STRING_ENTITY_CONVERTER)
                            .bindRepeatedGenericParameter("repeatedEntities",
                                    GenericEntityProperty::repeatedField,
                                    GenericEntityArgument.Builder::setRepeatedField,
                                    STRING_PARAM_VALUE_CONVERTER,
                                    STRING_ENTITY_CONVERTER)
                            .build();

    @Test
    public void getAppAction_genericParameters() {
        GenericEntityProperty property =
                GenericEntityProperty.create(
                        new TypeProperty.Builder<String>()
                                .setRequired(true)
                                .addPossibleEntities("one")
                                .build(),
                        Optional.of(
                                new TypeProperty.Builder<String>()
                                        .setRequired(true)
                                        .addPossibleEntities("two")
                                        .build()),
                        Optional.of(
                                new TypeProperty.Builder<String>()
                                        .setRequired(true)
                                        .addPossibleEntities("three")
                                        .build()));

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
        Property property =
                Property.create(
                        new TypeProperty.Builder<Entity>()
                                .addPossibleEntities(
                                        new Entity.Builder()
                                                .setId("contact_2")
                                                .setName("Donald")
                                                .setAlternateNames("Duck")
                                                .build())
                                .setValueMatchRequired(true)
                                .build(),
                        new TypeProperty.Builder<StringValue>().build());

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
                        new TypeProperty.Builder<Entity>()
                                .addPossibleEntities(
                                        new Entity.Builder()
                                                .setId("contact_2")
                                                .setName("Donald")
                                                .setAlternateNames("Duck")
                                                .build())
                                .build(),
                        Optional.of(
                                new TypeProperty.Builder<Entity>()
                                        .addPossibleEntities(
                                                new Entity.Builder()
                                                        .setId("entity1")
                                                        .setName("optional possible entity")
                                                        .build())
                                        .setRequired(true)
                                        .build()),
                        Optional.of(
                                new TypeProperty.Builder<TestEnum>()
                                        .addPossibleEntities(TestEnum.VALUE_1)
                                        .setRequired(true)
                                        .build()),
                        Optional.of(
                                new TypeProperty.Builder<Entity>()
                                        .addPossibleEntities(
                                                new Entity.Builder()
                                                        .setId("entity1")
                                                        .setName("repeated entity1")
                                                        .build(),
                                                new Entity.Builder()
                                                        .setId("entity2")
                                                        .setName("repeated entity2")
                                                        .build())
                                        .setRequired(true)
                                        .build()),
                        new TypeProperty.Builder<StringValue>().build(),
                        Optional.of(
                                new TypeProperty.Builder<StringValue>()
                                        .addPossibleEntities(StringValue.of("value1"))
                                        .setValueMatchRequired(true)
                                        .setRequired(true)
                                        .build()),
                        Optional.of(
                                new TypeProperty.Builder<StringValue>()
                                        .setProhibited(true)
                                        .build()));

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

    @Test
    public void convertOutputToProto_emptyOutput() {
        Output output = Output.builder().setRepeatedStringField(List.of("test3", "test4")).build();
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

        abstract List<EntityValue> repeatedEntityField();

        abstract String requiredStringField();

        abstract String optionalStringField();

        abstract List<String> repeatedStringField();

        @AutoValue.Builder
        abstract static class Builder implements BuilderOf<Argument> {

            abstract Builder setRequiredEntityField(EntityValue value);

            abstract Builder setOptionalEntityField(EntityValue value);

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
                TypeProperty<Entity> requiredEntityField,
                Optional<TypeProperty<Entity>> optionalEntityField,
                Optional<TypeProperty<TestEnum>> optionalEnumField,
                Optional<TypeProperty<Entity>> repeatedEntityField,
                TypeProperty<StringValue> requiredStringField,
                Optional<TypeProperty<StringValue>> optionalStringField,
                Optional<TypeProperty<StringValue>> repeatedStringField) {
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
                TypeProperty<Entity> requiredEntityField,
                TypeProperty<StringValue> requiredStringField) {
            return create(
                    requiredEntityField,
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    requiredStringField,
                    Optional.empty(),
                    Optional.empty());
        }

        abstract TypeProperty<Entity> requiredEntityField();

        abstract Optional<TypeProperty<Entity>> optionalEntityField();

        abstract Optional<TypeProperty<TestEnum>> optionalEnumField();

        abstract Optional<TypeProperty<Entity>> repeatedEntityField();

        abstract TypeProperty<StringValue> requiredStringField();

        abstract Optional<TypeProperty<StringValue>> optionalStringField();

        abstract Optional<TypeProperty<StringValue>> repeatedStringField();
    }

    @AutoValue
    abstract static class GenericEntityArgument {

        static Builder newBuilder() {
            return new AutoValue_ActionSpecTest_GenericEntityArgument.Builder();
        }

        abstract String singularField();

        abstract String optionalField();

        abstract List<String> repeatedField();

        @AutoValue.Builder
        abstract static class Builder implements BuilderOf<GenericEntityArgument> {

            abstract Builder setSingularField(String value);

            abstract Builder setOptionalField(String value);

            abstract Builder setRepeatedField(List<String> value);

            @NonNull
            @Override
            public abstract GenericEntityArgument build();
        }
    }

    @AutoValue
    abstract static class GenericEntityProperty {

        static GenericEntityProperty create(
                TypeProperty<String> singularField,
                Optional<TypeProperty<String>> optionalField,
                Optional<TypeProperty<String>> repeatedField) {
            return new AutoValue_ActionSpecTest_GenericEntityProperty(
                    singularField, optionalField, repeatedField);
        }

        abstract TypeProperty<String> singularField();

        abstract Optional<TypeProperty<String>> optionalField();

        abstract Optional<TypeProperty<String>> repeatedField();
    }
}
