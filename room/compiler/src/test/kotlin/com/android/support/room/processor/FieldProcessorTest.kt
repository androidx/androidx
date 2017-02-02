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
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.mock
import simpleRun
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

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
                TypeKind.INT,
                TypeKind.BYTE,
                TypeKind.SHORT,
                TypeKind.INT,
                TypeKind.LONG,
                TypeKind.CHAR,
                TypeKind.FLOAT,
                TypeKind.DOUBLE)
    }

    // these 2 box methods are ugly but makes tests nicer and they are private
    private fun TypeKind.typeMirror(invocation: TestInvocation): TypeMirror {
        return invocation.processingEnv.typeUtils.getPrimitiveType(this)
    }
    private fun TypeKind.box(): String {
        return "java.lang." + when (this) {
            TypeKind.INT -> "Integer"
            TypeKind.CHAR -> "Character"
            else -> this.name.toLowerCase().capitalize()
        }
    }

    private fun TypeKind.box(invocation: TestInvocation): TypeMirror {
        return invocation.processingEnv.elementUtils.getTypeElement(box()).asType()
    }

    @Test
    fun primitives() {
        ALL_PRIMITIVES.forEach { primitive ->
            singleEntity("${primitive.name.toLowerCase()} x;") { field, invocation ->
                assertThat(field, `is`(
                        Field(name = "x",
                                type = primitive.typeMirror(invocation),
                                primaryKey = false,
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
                        Field(name = "y",
                                type = primitive.box(invocation),
                                primaryKey = false,
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
                    Field(name = "x",
                            type = TypeKind.INT.typeMirror(invocation),
                            primaryKey = true,
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
                    Field(name = "x",
                            type = TypeKind.INT.typeMirror(invocation),
                            primaryKey = true,
                            element = field.element,
                            columnName = "foo")))
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
            singleEntity("${primitive.toString().toLowerCase()}[] arr;") { field, invocation ->
                assertThat(field, `is`(
                        Field(name = "arr",
                                type = invocation.processingEnv.typeUtils.getArrayType(
                                        primitive.typeMirror(invocation)),
                                primaryKey = false,
                                element = field.element)))
            }.compilesWithoutError()
        }
    }

    @Test
    fun boxedArray() {
        ALL_PRIMITIVES.forEach { primitive ->
            singleEntity("${primitive.box()}[] arr;") { field, invocation ->
                assertThat(field, `is`(
                        Field(name = "arr",
                                type = invocation.processingEnv.typeUtils.getArrayType(
                                        primitive.box(invocation)),
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
                    type = TypeKind.INT.box(invocation),
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
                """) { field, invocation -> }.failsToCompile()
                .withErrorContaining(ProcessorErrors.CANNOT_USE_UNBOUND_GENERICS_IN_ENTITY_FIELDS)
    }

    @Test
    fun nameVariations() {
        simpleRun {
            assertThat(Field(mock(Element::class.java), "x", TypeKind.INT.typeMirror(it), false)
                    .nameWithVariations, `is`(arrayListOf("x")))
            assertThat(Field(mock(Element::class.java), "x", TypeKind.BOOLEAN.typeMirror(it), false)
                    .nameWithVariations, `is`(arrayListOf("x")))
            assertThat(Field(mock(Element::class.java), "xAll",
                            TypeKind.BOOLEAN.typeMirror(it), false).nameWithVariations,
                    `is`(arrayListOf("xAll")))
        }
    }

    @Test
    fun nameVariations_is() {
        val elm = mock(Element::class.java)
        simpleRun {
            assertThat(Field(elm, "isX", TypeKind.BOOLEAN.typeMirror(it), false)
                    .nameWithVariations, `is`(arrayListOf("isX", "x")))
            assertThat(Field(elm, "isX", TypeKind.INT.typeMirror(it), false)
                    .nameWithVariations, `is`(arrayListOf("isX")))
            assertThat(Field(elm, "is", TypeKind.BOOLEAN.typeMirror(it), false)
                    .nameWithVariations, `is`(arrayListOf("is")))
            assertThat(Field(elm, "isAllItems", TypeKind.BOOLEAN.typeMirror(it), false)
                    .nameWithVariations, `is`(arrayListOf("isAllItems", "allItems")))
        }
    }

    @Test
    fun nameVariations_has() {
        val elm = mock(Element::class.java)
        simpleRun {
            assertThat(Field(elm, "hasX", TypeKind.BOOLEAN.typeMirror(it), false)
                    .nameWithVariations, `is`(arrayListOf("hasX", "x")))
            assertThat(Field(elm, "hasX", TypeKind.INT.typeMirror(it), false)
                    .nameWithVariations, `is`(arrayListOf("hasX")))
            assertThat(Field(elm, "has", TypeKind.BOOLEAN.typeMirror(it), false)
                    .nameWithVariations, `is`(arrayListOf("has")))
            assertThat(Field(elm, "hasAllItems", TypeKind.BOOLEAN.typeMirror(it), false)
                    .nameWithVariations, `is`(arrayListOf("hasAllItems", "allItems")))
        }
    }

    @Test
    fun nameVariations_m() {
        val elm = mock(Element::class.java)
        simpleRun {
            assertThat(Field(elm, "mall", TypeKind.BOOLEAN.typeMirror(it), false)
                    .nameWithVariations, `is`(arrayListOf("mall")))
            assertThat(Field(elm, "mallVars", TypeKind.BOOLEAN.typeMirror(it), false)
                    .nameWithVariations, `is`(arrayListOf("mallVars")))
            assertThat(Field(elm, "mAll", TypeKind.BOOLEAN.typeMirror(it), false)
                    .nameWithVariations, `is`(arrayListOf("mAll", "all")))
            assertThat(Field(elm, "m", TypeKind.INT.typeMirror(it), false)
                    .nameWithVariations, `is`(arrayListOf("m")))
            assertThat(Field(elm, "mallItems", TypeKind.BOOLEAN.typeMirror(it), false)
                    .nameWithVariations, `is`(arrayListOf("mallItems")))
            assertThat(Field(elm, "mAllItems", TypeKind.BOOLEAN.typeMirror(it), false)
                    .nameWithVariations, `is`(arrayListOf("mAllItems", "allItems")))
        }
    }

    @Test
    fun nameVariations_underscore() {
        val elm = mock(Element::class.java)
        simpleRun {
            assertThat(Field(elm, "_all", TypeKind.BOOLEAN.typeMirror(it), false)
                    .nameWithVariations, `is`(arrayListOf("_all", "all")))
            assertThat(Field(elm, "_", TypeKind.INT.typeMirror(it), false)
                    .nameWithVariations, `is`(arrayListOf("_")))
            assertThat(Field(elm, "_allItems", TypeKind.BOOLEAN.typeMirror(it), false)
                    .nameWithVariations, `is`(arrayListOf("_allItems", "allItems")))
        }
    }

    fun singleEntity(vararg input: String, handler: (Field, invocation: TestInvocation) -> Unit):
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
                            val parser = FieldProcessor(
                                    baseContext = invocation.context,
                                    containing = MoreTypes.asDeclared(owner.asType()),
                                    element = field!!)
                            handler(parser.process(), invocation)
                            true
                        }
                        .build())
    }
}
