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
import com.squareup.javapoet.TypeName
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class EntityParserTest : BaseEntityParserTest() {
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
            assertThat(field, `is`(Field(
                    element = field.element,
                    name = "id",
                    type = TypeName.INT,
                    primaryKey = true)))
            assertThat(field.setter, `is`(FieldSetter("setId", CallType.METHOD)))
            assertThat(field.getter, `is`(FieldGetter("getId", CallType.METHOD)))
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
}
