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

import com.android.support.room.Entity
import com.android.support.room.testing.TestInvocation
import com.android.support.room.testing.TestProcessor
import com.android.support.room.vo.Field
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.google.common.truth.Truth
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourceSubjectFactory
import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.TypeName
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.mock
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind

@RunWith(JUnit4::class)
class FieldProcessorTest {
    companion object {
        const val ENTITY_PREFIX = """
                package foo.bar;
                import com.android.support.room.*;
                @Entity
                abstract class MyEntity {
                """
        const val ENTITY_SUFFIX = "}"
        val ALL_PRIMITIVES = arrayListOf(
                TypeName.BOOLEAN,
                TypeName.BYTE,
                TypeName.SHORT,
                TypeName.INT,
                TypeName.LONG,
                TypeName.CHAR,
                TypeName.FLOAT,
                TypeName.DOUBLE)
    }

    @Test
    fun primitives() {
        ALL_PRIMITIVES.forEach { primitive ->
            singleEntity("$primitive x;") { field, invocation ->
                assertThat(field, `is`(
                        Field(name = "x", type = primitive, primaryKey = false,
                                element = field.element
                                )))
            }.compilesWithoutError()
        }
    }

    @Test
    fun boxed() {
        ALL_PRIMITIVES.forEach { primitive ->
            singleEntity("${primitive.box()} y;") { field, invocation ->
                assertThat(field, `is`(
                        Field(name = "y", type = primitive.box(), primaryKey = false,
                                element = field.element)))
            }.compilesWithoutError()
        }
    }

    @Test
    fun primaryKey() {
        singleEntity("""
            @PrimaryKey
            int x;
            """) { field, invocation ->
            assertThat(field, `is`(
                    Field(name = "x", type = TypeName.INT, primaryKey = true,
                            element = field.element)))
        }.compilesWithoutError()
    }

    @Test
    fun columnName() {
        singleEntity("""
            @ColumnName("foo")
            @PrimaryKey
            int x;
            """) { field, invocation ->
            assertThat(field, `is`(
                    Field(name = "x", type = TypeName.INT, primaryKey = true,
                            element = field.element, columnName = "foo")))
        }.compilesWithoutError()
    }

    @Test
    fun emptyColumnName() {
        singleEntity("""
            @ColumnName("")
            int x;
            """) { field, invocation ->
        }.failsToCompile().withErrorContaining(ProcessorErrors.COLUMN_NAME_CANNOT_BE_EMPTY)
    }

    @Test
    fun primitiveArray() {
        ALL_PRIMITIVES.forEach { primitive ->
            singleEntity("$primitive[] arr;") { field, invocation ->
                assertThat(field, `is`(
                        Field(name = "arr", type = ArrayTypeName.of(primitive), primaryKey = false,
                                element = field.element)))
            }.compilesWithoutError()
        }
    }

    @Test
    fun boxedArray() {
        ALL_PRIMITIVES.forEach { primitive ->
            singleEntity("${primitive.box()}[] arr;") { field, invocation ->
                assertThat(field, `is`(
                        Field(name = "arr", type = ArrayTypeName.of(primitive.box()),
                                primaryKey = false,
                                element = field.element)))
            }.compilesWithoutError()
        }
    }

    @Test
    fun generic() {
        singleEntity("""
                static class BaseClass<T> {
                    T item;
                }
                @Entity
                static class Extending extends BaseClass<java.lang.Integer> {
                }
                """) { field, invocation ->
            assertThat(field, `is`(Field(name = "item",
                    type = TypeName.INT.box(),
                    primaryKey = false,
                    element = field.element)))
        }.compilesWithoutError()
    }

    @Test
    fun unboundGeneric() {
        singleEntity("""
                @Entity
                static class BaseClass<T> {
                    T item;
                }
                """) {field, invocation -> }.failsToCompile()
                .withErrorContaining(ProcessorErrors.CANNOT_USE_UNBOUND_GENERICS_IN_ENTITY_FIELDS)
    }

    @Test
    fun nameVariations() {
        assertThat(Field(mock(Element::class.java), "x", TypeName.INT, false)
                .nameWithVariations, `is`(arrayListOf("x")))
        assertThat(Field(mock(Element::class.java), "x", TypeName.BOOLEAN, false)
                .nameWithVariations, `is`(arrayListOf("x")))
        assertThat(Field(mock(Element::class.java), "xAll", TypeName.BOOLEAN, false)
                .nameWithVariations, `is`(arrayListOf("xAll")))
    }

    @Test
    fun nameVariations_is() {
        assertThat(Field(mock(Element::class.java), "isX", TypeName.BOOLEAN, false)
                .nameWithVariations, `is`(arrayListOf("isX", "x")))
        assertThat(Field(mock(Element::class.java), "isX", TypeName.INT, false)
                .nameWithVariations, `is`(arrayListOf("isX")))
        assertThat(Field(mock(Element::class.java), "is", TypeName.BOOLEAN, false)
                .nameWithVariations, `is`(arrayListOf("is")))
        assertThat(Field(mock(Element::class.java), "isAllItems", TypeName.BOOLEAN, false)
                .nameWithVariations, `is`(arrayListOf("isAllItems", "allItems")))
    }

    @Test
    fun nameVariations_has() {
        assertThat(Field(mock(Element::class.java), "hasX", TypeName.BOOLEAN, false)
                .nameWithVariations, `is`(arrayListOf("hasX", "x")))
        assertThat(Field(mock(Element::class.java), "hasX", TypeName.INT, false)
                .nameWithVariations, `is`(arrayListOf("hasX")))
        assertThat(Field(mock(Element::class.java), "has", TypeName.BOOLEAN, false)
                .nameWithVariations, `is`(arrayListOf("has")))
        assertThat(Field(mock(Element::class.java), "hasAllItems", TypeName.BOOLEAN, false)
                .nameWithVariations, `is`(arrayListOf("hasAllItems", "allItems")))
    }

    @Test
    fun nameVariations_m() {
        assertThat(Field(mock(Element::class.java), "mall", TypeName.BOOLEAN, false)
                .nameWithVariations, `is`(arrayListOf("mall")))
        assertThat(Field(mock(Element::class.java), "mallVars", TypeName.BOOLEAN, false)
                .nameWithVariations, `is`(arrayListOf("mallVars")))
        assertThat(Field(mock(Element::class.java), "mAll", TypeName.BOOLEAN, false)
                .nameWithVariations, `is`(arrayListOf("mAll", "all")))
        assertThat(Field(mock(Element::class.java), "m", TypeName.INT, false)
                .nameWithVariations, `is`(arrayListOf("m")))
        assertThat(Field(mock(Element::class.java), "mallItems", TypeName.BOOLEAN, false)
                .nameWithVariations, `is`(arrayListOf("mallItems")))
        assertThat(Field(mock(Element::class.java), "mAllItems", TypeName.BOOLEAN, false)
                .nameWithVariations, `is`(arrayListOf("mAllItems", "allItems")))
    }

    @Test
    fun nameVariations_underscore() {
        assertThat(Field(mock(Element::class.java), "_all", TypeName.BOOLEAN, false)
                .nameWithVariations, `is`(arrayListOf("_all", "all")))
        assertThat(Field(mock(Element::class.java), "_", TypeName.INT, false)
                .nameWithVariations, `is`(arrayListOf("_")))
        assertThat(Field(mock(Element::class.java), "_allItems", TypeName.BOOLEAN, false)
                .nameWithVariations, `is`(arrayListOf("_allItems", "allItems")))
    }

    fun singleEntity(vararg input: String, handler: (Field, invocation : TestInvocation) -> Unit):
            CompileTester {
        return Truth.assertAbout(JavaSourceSubjectFactory.javaSource())
                .that(JavaFileObjects.forSourceString("foo.bar.MyEntity",
                        ENTITY_PREFIX + input.joinToString("\n") + ENTITY_SUFFIX
                ))
                .processedWith(TestProcessor.builder()
                        .forAnnotations(com.android.support.room.Entity::class)
                        .nextRunHandler { invocation ->
                            val (owner, field) = invocation.roundEnv
                                    .getElementsAnnotatedWith(Entity::class.java)
                                    .map {
                                        Pair(it, invocation.processingEnv.elementUtils
                                                .getAllMembers(MoreElements.asType(it))
                                                .firstOrNull { it.kind == ElementKind.FIELD })
                                    }
                                    .first { it.second != null }
                            val parser = FieldProcessor(invocation.context)
                            handler(parser.parse(
                                    MoreTypes.asDeclared(owner.asType()), field!!), invocation)
                            true
                        }
                        .build())
    }
}
