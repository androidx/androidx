/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.support.room.processor

import com.android.support.room.vo.CallType
import com.android.support.room.vo.Field
import com.android.support.room.vo.FieldGetter
import com.android.support.room.vo.FieldSetter
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import javax.lang.model.type.TypeKind.INT

@RunWith(JUnit4::class)
class EntityProcessorTest : BaseEntityParserTest() {
    @Test
    fun simple() {
        singleEntity("""
                @PrimaryKey
                private int id;
                public int getId() { return id; }
                public void setId(int id) { this.id = id; }
            """) { entity, invocation ->
            assertThat(entity.type.toString(), `is`("foo.bar.MyEntity"))
            assertThat(entity.fields.size, `is`(1))
            val field = entity.fields.first()
            val intType = invocation.processingEnv.typeUtils.getPrimitiveType(INT)
            assertThat(field, `is`(Field(
                    element = field.element,
                    name = "id",
                    type = intType,
                    primaryKey = true,
                    columnName = "id")))
            assertThat(field.setter, `is`(FieldSetter("setId", intType, CallType.METHOD,
                    field.setter.columnAdapter)))
            assertThat(field.getter, `is`(FieldGetter("getId", intType, CallType.METHOD,
                    field.getter.columnAdapter)))
            assertThat(entity.primaryKeys, `is`(listOf(field)))
        }.compilesWithoutError()
    }

    @Test
    fun noGetter() {
        singleEntity("""
                @PrimaryKey
                private int id;
                public void setId(int id) {this.id = id;}
                """) { entity, invocation -> }
                .failsToCompile()
                .withErrorContaining(ProcessorErrors.CANNOT_FIND_GETTER_FOR_FIELD)
    }

    @Test
    fun noSetter() {
        singleEntity("""
                @PrimaryKey
                private int id;
                public int getId(){ return id; }
                """) { entity, invocation -> }
                .failsToCompile()
                .withErrorContaining(ProcessorErrors.CANNOT_FIND_SETTER_FOR_FIELD)
    }

    @Test
    fun tooManyGetters() {
        singleEntity("""
                @PrimaryKey
                private int id;
                public void setId(int id) {}
                public int getId(){ return id; }
                public int id(){ return id; }
                """) { entity, invocation -> }
                .failsToCompile()
                .withErrorContaining("getId, id")
    }

    @Test
    fun tooManyGettersWithIgnore() {
        singleEntity("""
                @PrimaryKey
                private int id;
                public void setId(int id) {}
                public int getId(){ return id; }
                @Ignore public int id(){ return id; }
                """) { entity, invocation ->
            assertThat(entity.fields.first().getter.name, `is`("getId"))
        }.compilesWithoutError()
    }

    @Test
    fun tooManySetters() {
        singleEntity("""
                @PrimaryKey
                private int id;
                public void setId(int id) {}
                public void id(int id) {}
                public int getId(){ return id; }
                """) { entity, invocation -> }
                .failsToCompile()
                .withErrorContaining("setId, id")
    }

    @Test
    fun tooManySettersWithIgnore() {
        singleEntity("""
                @PrimaryKey
                private int id;
                public void setId(int id) {}
                @Ignore public void id(int id) {}
                public int getId(){ return id; }
                """) { entity, invocation ->
            assertThat(entity.fields.first().setter.name, `is`("setId"))
        }.compilesWithoutError()
    }

    @Test
    fun multiplePrimaryKeys() {
        singleEntity("""
                @PrimaryKey
                int x;
                @PrimaryKey
                int y;
                """) { entity , invocation ->
            assertThat(entity.primaryKeys.size, `is`(2))
        }.compilesWithoutError()
    }

    @Test
    fun customName() {
        singleEntity("""
                @PrimaryKey
                int x;
                """, hashMapOf(Pair("tableName", "\"foo_table\""))) { entity , invocation ->
            assertThat(entity.tableName, `is`("foo_table"))
        }.compilesWithoutError()
    }

    @Test
    fun emptyCustomName() {
        singleEntity("""
                @PrimaryKey
                int x;
                """, hashMapOf(Pair("tableName", "\" \""))) { entity , invocation ->
        }.failsToCompile().withErrorContaining(ProcessorErrors.ENTITY_TABLE_NAME_CANNOT_BE_EMPTY)
    }

    @Test
    fun missingPrimaryKey() {
        singleEntity("""
                """) { entity, invocation ->
        }.failsToCompile()
                .withErrorContaining(ProcessorErrors.MISSING_PRIMARY_KEY)
    }
}
