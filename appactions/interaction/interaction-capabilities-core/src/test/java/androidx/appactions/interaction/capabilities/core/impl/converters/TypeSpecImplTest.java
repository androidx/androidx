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

package androidx.appactions.interaction.capabilities.core.impl.converters;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.appactions.builtintypes.experimental.properties.Name;
import androidx.appactions.builtintypes.experimental.types.Thing;
import androidx.appactions.interaction.capabilities.core.impl.exceptions.StructConversionException;
import androidx.appactions.interaction.capabilities.core.testing.spec.TestEntity;
import androidx.appactions.interaction.protobuf.Struct;
import androidx.appactions.interaction.protobuf.Value;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@RunWith(JUnit4.class)
public final class TypeSpecImplTest {
    private static Value structToValue(Struct struct) {
        return Value.newBuilder().setStructValue(struct).build();
    }

    @Test
    public void bindIdentifier_success() {
        TypeSpec<TestEntity> entityTypeSpec =
                TypeSpecBuilder.newBuilder("TestEntity", TestEntity::newBuilder)
                        .bindIdentifier(TestEntity::getId)
                        .build();
        assertThat(
                        entityTypeSpec.getIdentifier(
                                TestEntity.newBuilder().setId("identifier1").build()))
                .isEqualTo("identifier1");
        assertThat(entityTypeSpec.getIdentifier(TestEntity.newBuilder().build())).isNull();
    }

    @Test
    public void bindEnumField_convertsSuccessfully() throws Exception {
        TypeSpec<TestEntity> entityTypeSpec =
                TypeSpecBuilder.newBuilder("TestEntity", TestEntity::newBuilder)
                        .bindEnumField(
                                "enum",
                                TestEntity::getEnum,
                                TestEntity.Builder::setEnum,
                                TestEntity.TestEnum.class)
                        .build();
        TestEntity entity = TestEntity.newBuilder().setEnum(TestEntity.TestEnum.VALUE_1).build();
        Value entityValue =
                structToValue(
                        Struct.newBuilder()
                                .putFields(
                                        "@type",
                                        Value.newBuilder().setStringValue("TestEntity").build())
                                .putFields(
                                        "enum",
                                        Value.newBuilder().setStringValue("value_1").build())
                                .build());

        assertThat(entityTypeSpec.toValue(entity)).isEqualTo(entityValue);
        assertThat(entityTypeSpec.fromValue(entityValue)).isEqualTo(entity);
    }

    @Test
    public void bindEnumField_throwsException() throws Exception {
        TypeSpec<TestEntity> entityTypeSpec =
                TypeSpecBuilder.newBuilder("TestEntity", TestEntity::newBuilder)
                        .bindEnumField(
                                "enum",
                                TestEntity::getEnum,
                                TestEntity.Builder::setEnum,
                                TestEntity.TestEnum.class)
                        .build();
        Value malformedValue =
                structToValue(
                        Struct.newBuilder()
                                .putFields(
                                        "@type",
                                        Value.newBuilder().setStringValue("TestEntity").build())
                                .putFields(
                                        "enum",
                                        Value.newBuilder().setStringValue("invalid").build())
                                .build());

        assertThrows(
                StructConversionException.class, () -> entityTypeSpec.fromValue(malformedValue));
    }

    @Test
    public void bindDurationField_convertsSuccessfully() throws Exception {
        TypeSpec<TestEntity> entityTypeSpec =
                TypeSpecBuilder.newBuilder("TestEntity", TestEntity::newBuilder)
                        .bindDurationField(
                                "duration",
                                TestEntity::getDuration,
                                TestEntity.Builder::setDuration)
                        .build();
        TestEntity entity = TestEntity.newBuilder().setDuration(Duration.ofMinutes(5)).build();
        Value entityValue =
                structToValue(
                        Struct.newBuilder()
                                .putFields(
                                        "@type",
                                        Value.newBuilder().setStringValue("TestEntity").build())
                                .putFields(
                                        "duration",
                                        Value.newBuilder().setStringValue("PT5M").build())
                                .build());

        assertThat(entityTypeSpec.toValue(entity)).isEqualTo(entityValue);
        assertThat(entityTypeSpec.fromValue(entityValue)).isEqualTo(entity);
    }

    @Test
    public void bindZonedDateTimeField_convertsSuccessfully() throws Exception {
        TypeSpec<TestEntity> entityTypeSpec =
                TypeSpecBuilder.newBuilder("TestEntity", TestEntity::newBuilder)
                        .bindZonedDateTimeField(
                                "date",
                                TestEntity::getZonedDateTime,
                                TestEntity.Builder::setZonedDateTime)
                        .build();
        TestEntity entity =
                TestEntity.newBuilder()
                        .setZonedDateTime(ZonedDateTime.of(2022, 1, 1, 8, 0, 0, 0, ZoneOffset.UTC))
                        .build();
        Value entityValue =
                structToValue(
                        Struct.newBuilder()
                                .putFields(
                                        "@type",
                                        Value.newBuilder().setStringValue("TestEntity").build())
                                .putFields(
                                        "date",
                                        Value.newBuilder()
                                                .setStringValue("2022-01-01T08:00Z")
                                                .build())
                                .build());

        assertThat(entityTypeSpec.toValue(entity)).isEqualTo(entityValue);
        assertThat(entityTypeSpec.fromValue(entityValue)).isEqualTo(entity);
    }

    @Test
    public void bindZonedDateTimeField_zoneId_convertsSuccessfully() throws Exception {
        TypeSpec<TestEntity> entityTypeSpec =
                TypeSpecBuilder.newBuilder("TestEntity", TestEntity::newBuilder)
                        .bindZonedDateTimeField(
                                "date",
                                TestEntity::getZonedDateTime,
                                TestEntity.Builder::setZonedDateTime)
                        .build();
        TestEntity entity =
                TestEntity.newBuilder()
                        .setZonedDateTime(
                                ZonedDateTime.of(2022, 1, 1, 8, 0, 0, 0, ZoneId.of("UTC+01:00")))
                        .build();
        Value entityValue =
                structToValue(
                        Struct.newBuilder()
                                .putFields(
                                        "@type",
                                        Value.newBuilder().setStringValue("TestEntity").build())
                                .putFields(
                                        "date",
                                        Value.newBuilder()
                                                .setStringValue("2022-01-01T08:00+01:00")
                                                .build())
                                .build());
        TestEntity expectedEntity =
                TestEntity.newBuilder()
                        .setZonedDateTime(
                                ZonedDateTime.of(2022, 1, 1, 8, 0, 0, 0, ZoneOffset.of("+01:00")))
                        .build();

        assertThat(entityTypeSpec.toValue(entity)).isEqualTo(entityValue);
        assertThat(entityTypeSpec.fromValue(entityValue)).isEqualTo(expectedEntity);
    }

    @Test
    public void bindZonedDateTimeField_throwsException() throws Exception {
        TypeSpec<TestEntity> entityTypeSpec =
                TypeSpecBuilder.newBuilder("TestEntity", TestEntity::newBuilder)
                        .bindZonedDateTimeField(
                                "date",
                                TestEntity::getZonedDateTime,
                                TestEntity.Builder::setZonedDateTime)
                        .build();
        Value malformedValue =
                structToValue(
                        Struct.newBuilder()
                                .putFields(
                                        "@type",
                                        Value.newBuilder().setStringValue("TestEntity").build())
                                .putFields(
                                        "date",
                                        Value.newBuilder().setStringValue("2022-01-01T08").build())
                                .build());

        assertThrows(
                StructConversionException.class, () -> entityTypeSpec.fromValue(malformedValue));
    }

    @Test
    public void bindSpecField_convertsSuccessfully() throws Exception {
        TypeSpec<TestEntity> entityTypeSpec =
                TypeSpecBuilder.newBuilder("TestEntity", TestEntity::newBuilder)
                        .bindSpecField(
                                "entity",
                                TestEntity::getEntity,
                                TestEntity.Builder::setEntity,
                                TypeSpecBuilder.newBuilder("TestEntity", TestEntity::newBuilder)
                                        .bindStringField(
                                                "name",
                                                TestEntity::getName,
                                                TestEntity.Builder::setName)
                                        .build())
                        .build();
        TestEntity entity =
                TestEntity.newBuilder()
                        .setEntity(TestEntity.newBuilder().setName("entity name").build())
                        .build();
        Value entityValue = structToValue(
                Struct.newBuilder()
                        .putFields(
                                "@type",
                                Value.newBuilder().setStringValue("TestEntity").build())
                        .putFields(
                                "entity",
                                Value.newBuilder()
                                        .setStructValue(
                                                Struct.newBuilder()
                                                        .putFields(
                                                                "@type",
                                                                Value.newBuilder()
                                                                        .setStringValue(
                                                                                "TestEntity")
                                                                        .build())
                                                        .putFields(
                                                                "name",
                                                                Value.newBuilder()
                                                                        .setStringValue(
                                                                                "entity"
                                                                                        + " name")
                                                                        .build())
                                                        .build())
                                        .build())
                        .build());

        assertThat(entityTypeSpec.toValue(entity)).isEqualTo(entityValue);
        assertThat(entityTypeSpec.fromValue(entityValue)).isEqualTo(entity);
    }

    @Test
    public void bindSpecField_throwsException() throws Exception {
        TypeSpec<TestEntity> entityTypeSpec =
                TypeSpecBuilder.newBuilder("TestEntity", TestEntity::newBuilder)
                        .bindSpecField(
                                "entity",
                                TestEntity::getEntity,
                                TestEntity.Builder::setEntity,
                                TypeSpecBuilder.newBuilder("TestEntity", TestEntity::newBuilder)
                                        .bindStringField(
                                                "name",
                                                TestEntity::getName,
                                                TestEntity.Builder::setName)
                                        .build())
                        .build();
        Value malformedValue =
                structToValue(
                        Struct.newBuilder()
                                .putFields(
                                        "@type",
                                        Value.newBuilder().setStringValue("TestEntity").build())
                                .putFields(
                                        "entity",
                                        Value.newBuilder().setStringValue("wrong value").build())
                                .build());

        assertThrows(
                StructConversionException.class, () -> entityTypeSpec.fromValue(malformedValue));
    }

    @Test
    public void newBuilderForThing_builtInTypes_smokeTest() throws Exception {
        TypeSpec<Thing> thingTypeSpec =
                TypeSpecBuilder.newBuilderForThing("Thing", Thing::Builder, Thing.Builder::build)
                        .build();

        Thing thing = Thing.Builder().setIdentifier("thing").setName(new Name("Thing One")).build();
        Value thingValue =
                structToValue(
                        Struct.newBuilder()
                                .putFields(
                                        "@type", Value.newBuilder().setStringValue("Thing").build())
                                .putFields(
                                        "identifier",
                                        Value.newBuilder().setStringValue("thing").build())
                                .putFields(
                                        "name",
                                        Value.newBuilder().setStringValue("Thing One").build())
                                .build());

        assertThat(thingTypeSpec.getIdentifier(thing)).isEqualTo("thing");
        assertThat(thingTypeSpec.toValue(thing)).isEqualTo(thingValue);
        assertThat(thingTypeSpec.fromValue(thingValue).getIdentifier())
                .isEqualTo(thing.getIdentifier());
        assertThat(thingTypeSpec.fromValue(thingValue).getName().asText())
                .isEqualTo(thing.getName().asText());
    }
}
