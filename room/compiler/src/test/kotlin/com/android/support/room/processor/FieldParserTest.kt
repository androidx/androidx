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
import javax.lang.model.element.ElementKind

@RunWith(JUnit4::class)
class FieldParserTest {
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
            singleEntity("$primitive x;") { field ->
                assertThat(field, `is`(
                        Field(name = "x", type = primitive, primaryKey = false)))
            }.compilesWithoutError()
        }
    }

    @Test
    fun boxed() {
        ALL_PRIMITIVES.forEach { primitive ->
            singleEntity("${primitive.box()} y;") { field ->
                assertThat(field, `is`(
                        Field(name = "y", type = primitive.box(), primaryKey = false)))
            }.compilesWithoutError()
        }
    }

    @Test
    fun primaryKey() {
        singleEntity("""
            @PrimaryKey
            int x;
            """) { field ->
            assertThat(field, `is`(
                    Field(name = "x", type = TypeName.INT, primaryKey = true)
            ))
        }.compilesWithoutError()
    }

    @Test
    fun primitiveArray() {
        ALL_PRIMITIVES.forEach { primitive ->
            singleEntity("$primitive[] arr;") { field ->
                assertThat(field, `is`(
                        Field(name = "arr", type = ArrayTypeName.of(primitive), primaryKey = false)
                ))
            }.compilesWithoutError()
        }
    }

    @Test
    fun boxedArray() {
        ALL_PRIMITIVES.forEach { primitive ->
            singleEntity("${primitive.box()}[] arr;") { field ->
                assertThat(field, `is`(
                        Field(name = "arr", type = ArrayTypeName.of(primitive.box()),
                                primaryKey = false)
                ))
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
                """) { field ->
            assertThat(field, `is`(Field(name = "item",
                    type = TypeName.INT.box(),
                    primaryKey = false)))
        }.compilesWithoutError()
    }

    @Test
    fun unboundGeneric() {
        singleEntity("""
                @Entity
                static class BaseClass<T> {
                    T item;
                }
                """) {}.failsToCompile()
                .withErrorContaining(ProcessorErrors.CANNOT_USE_UNBOUND_GENERICS_IN_ENTITY_FIELDS)
    }

    fun singleEntity(vararg input: String, handler: (Field) -> Unit): CompileTester {
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
                            val parser = FieldParser(invocation.roundEnv, invocation.processingEnv)
                            handler(parser.parse(
                                    MoreTypes.asDeclared(owner.asType()), field!!))
                            true
                        }
                        .build())
    }
}