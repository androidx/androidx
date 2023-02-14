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

import androidx.appactions.interaction.capabilities.core.impl.exceptions.StructConversionException;
import androidx.appactions.interaction.capabilities.core.testing.spec.TestEntity;

import com.google.protobuf.Struct;
import com.google.protobuf.Value;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@RunWith(JUnit4.class)
public final class TypeSpecImplTest {

    @Test
    public void bindEnumField_convertsSuccessfully() throws Exception {
        TypeSpec<TestEntity> entityTypeSpec =
                TypeSpecBuilder.newBuilder("TestEntity", TestEntity::newBuilder)
                        .bindEnumField(
                                "enum", TestEntity::getEnum, TestEntity.Builder::setEnum,
                                TestEntity.TestEnum.class)
                        .build();
        TestEntity entity = TestEntity.newBuilder().setEnum(TestEntity.TestEnum.VALUE_1).build();
        Struct entityStruct =
                Struct.newBuilder()
                        .putFields("@type", Value.newBuilder().setStringValue("TestEntity").build())
                        .putFields("enum", Value.newBuilder().setStringValue("value_1").build())
                        .build();

        Struct convertedStruct = entityTypeSpec.toStruct(entity);
        assertThat(convertedStruct).isEqualTo(entityStruct);

        TestEntity convertedEntity = entityTypeSpec.fromStruct(entityStruct);
        assertThat(convertedEntity).isEqualTo(entity);
    }

    @Test
    public void bindEnumField_throwsException() throws Exception {
        TypeSpec<TestEntity> entityTypeSpec =
                TypeSpecBuilder.newBuilder("TestEntity", TestEntity::newBuilder)
                        .bindEnumField(
                                "enum", TestEntity::getEnum, TestEntity.Builder::setEnum,
                                TestEntity.TestEnum.class)
                        .build();
        Struct malformedStruct =
                Struct.newBuilder()
                        .putFields("@type", Value.newBuilder().setStringValue("TestEntity").build())
                        .putFields("enum", Value.newBuilder().setStringValue("invalid").build())
                        .build();

        assertThrows(StructConversionException.class,
                () -> entityTypeSpec.fromStruct(malformedStruct));
    }

    @Test
    public void bindDurationField_convertsSuccessfully() throws Exception {
        TypeSpec<TestEntity> entityTypeSpec =
                TypeSpecBuilder.newBuilder("TestEntity", TestEntity::newBuilder)
                        .bindDurationField("duration", TestEntity::getDuration,
                                TestEntity.Builder::setDuration)
                        .build();
        TestEntity entity = TestEntity.newBuilder().setDuration(Duration.ofMinutes(5)).build();
        Struct entityStruct =
                Struct.newBuilder()
                        .putFields("@type", Value.newBuilder().setStringValue("TestEntity").build())
                        .putFields("duration", Value.newBuilder().setStringValue("PT5M").build())
                        .build();

        Struct convertedStruct = entityTypeSpec.toStruct(entity);
        assertThat(convertedStruct).isEqualTo(entityStruct);

        TestEntity convertedEntity = entityTypeSpec.fromStruct(entityStruct);
        assertThat(convertedEntity).isEqualTo(entity);
    }

    @Test
    public void bindZonedDateTimeField_convertsSuccessfully() throws Exception {
        TypeSpec<TestEntity> entityTypeSpec =
                TypeSpecBuilder.newBuilder("TestEntity", TestEntity::newBuilder)
                        .bindZonedDateTimeField(
                                "date", TestEntity::getZonedDateTime,
                                TestEntity.Builder::setZonedDateTime)
                        .build();
        TestEntity entity =
                TestEntity.newBuilder()
                        .setZonedDateTime(ZonedDateTime.of(2022, 1, 1, 8, 0, 0, 0, ZoneOffset.UTC))
                        .build();
        Struct entityStruct =
                Struct.newBuilder()
                        .putFields("@type", Value.newBuilder().setStringValue("TestEntity").build())
                        .putFields("date",
                                Value.newBuilder().setStringValue("2022-01-01T08:00Z").build())
                        .build();

        Struct convertedStruct = entityTypeSpec.toStruct(entity);
        assertThat(convertedStruct).isEqualTo(entityStruct);

        TestEntity convertedEntity = entityTypeSpec.fromStruct(entityStruct);
        assertThat(convertedEntity).isEqualTo(entity);
    }

    @Test
    public void bindZonedDateTimeField_zoneId_convertsSuccessfully() throws Exception {
        TypeSpec<TestEntity> entityTypeSpec =
                TypeSpecBuilder.newBuilder("TestEntity", TestEntity::newBuilder)
                        .bindZonedDateTimeField(
                                "date", TestEntity::getZonedDateTime,
                                TestEntity.Builder::setZonedDateTime)
                        .build();
        TestEntity entity =
                TestEntity.newBuilder()
                        .setZonedDateTime(
                                ZonedDateTime.of(2022, 1, 1, 8, 0, 0, 0, ZoneId.of("UTC+01:00")))
                        .build();
        Struct entityStruct =
                Struct.newBuilder()
                        .putFields("@type", Value.newBuilder().setStringValue("TestEntity").build())
                        .putFields("date",
                                Value.newBuilder().setStringValue("2022-01-01T08:00+01:00").build())
                        .build();
        TestEntity expectedEntity =
                TestEntity.newBuilder()
                        .setZonedDateTime(
                                ZonedDateTime.of(2022, 1, 1, 8, 0, 0, 0, ZoneOffset.of("+01:00")))
                        .build();

        Struct convertedStruct = entityTypeSpec.toStruct(entity);
        assertThat(convertedStruct).isEqualTo(entityStruct);

        TestEntity convertedEntity = entityTypeSpec.fromStruct(entityStruct);
        assertThat(convertedEntity).isEqualTo(expectedEntity);
    }

    @Test
    public void bindZonedDateTimeField_throwsException() throws Exception {
        TypeSpec<TestEntity> entityTypeSpec =
                TypeSpecBuilder.newBuilder("TestEntity", TestEntity::newBuilder)
                        .bindZonedDateTimeField(
                                "date", TestEntity::getZonedDateTime,
                                TestEntity.Builder::setZonedDateTime)
                        .build();
        Struct malformedStruct =
                Struct.newBuilder()
                        .putFields("@type", Value.newBuilder().setStringValue("TestEntity").build())
                        .putFields("date",
                                Value.newBuilder().setStringValue("2022-01-01T08").build())
                        .build();

        assertThrows(StructConversionException.class,
                () -> entityTypeSpec.fromStruct(malformedStruct));
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
                                        .bindStringField("name", TestEntity::getName,
                                                TestEntity.Builder::setName)
                                        .build())
                        .build();
        TestEntity entity =
                TestEntity.newBuilder()
                        .setEntity(TestEntity.newBuilder().setName("entity name").build())
                        .build();
        Struct entityStruct =
                Struct.newBuilder()
                        .putFields("@type", Value.newBuilder().setStringValue("TestEntity").build())
                        .putFields(
                                "entity",
                                Value.newBuilder()
                                        .setStructValue(
                                                Struct.newBuilder()
                                                        .putFields(
                                                                "@type",
                                                                Value.newBuilder().setStringValue(
                                                                        "TestEntity").build())
                                                        .putFields(
                                                                "name",
                                                                Value.newBuilder().setStringValue(
                                                                        "entity name").build())
                                                        .build())
                                        .build())
                        .build();

        Struct convertedStruct = entityTypeSpec.toStruct(entity);
        assertThat(convertedStruct).isEqualTo(entityStruct);

        TestEntity convertedEntity = entityTypeSpec.fromStruct(entityStruct);
        assertThat(convertedEntity).isEqualTo(entity);
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
                                        .bindStringField("name", TestEntity::getName,
                                                TestEntity.Builder::setName)
                                        .build())
                        .build();
        Struct malformedStruct =
                Struct.newBuilder()
                        .putFields("@type", Value.newBuilder().setStringValue("TestEntity").build())
                        .putFields("entity",
                                Value.newBuilder().setStringValue("wrong value").build())
                        .build();

        assertThrows(StructConversionException.class,
                () -> entityTypeSpec.fromStruct(malformedStruct));
    }
}
