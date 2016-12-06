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

import COMMON
import com.android.support.room.Dao
import com.android.support.room.Insert
import com.android.support.room.ext.typeName
import com.android.support.room.testing.TestInvocation
import com.android.support.room.testing.TestProcessor
import com.android.support.room.vo.InsertionMethod
import com.android.support.room.vo.InsertionMethod.Type
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.google.common.truth.Truth.assertAbout
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubjectFactory
import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
@RunWith(JUnit4::class)
class InsertionMethodProcessorTest {
    companion object {
        const val DAO_PREFIX = """
                package foo.bar;
                import com.android.support.room.*;
                import java.util.*;
                @Dao
                abstract class MyClass {
                """
        const val DAO_SUFFIX = "}"
        val USER_TYPE_NAME : TypeName = COMMON.USER_TYPE_NAME
    }

    @Test
    fun readNoParams() {
        singleInsertMethod(
                """
                @Insert
                abstract public void foo();
                """) { insertion, invocation ->
            assertThat(insertion.name, `is`("foo"))
            assertThat(insertion.parameters.size, `is`(0))
            assertThat(insertion.returnType.typeName(), `is`(TypeName.VOID))
        }.failsToCompile().withErrorContaining(
                ProcessorErrors.INSERTION_DOES_NOT_HAVE_ANY_PARAMETERS_TO_INSERT)
    }

    @Test
    fun insertSingle() {
        singleInsertMethod(
                """
                @Insert
                abstract public long foo(User user);
                """) { insertion, invocation ->
            assertThat(insertion.name, `is`("foo"))
            assertThat(insertion.parameters.size, `is`(1))
            val param = insertion.parameters.first()
            assertThat(param.type.typeName(), `is`(USER_TYPE_NAME))
            assertThat(param.entityType?.typeName(), `is`(USER_TYPE_NAME))
            assertThat(insertion.entity?.typeName,
                    `is`(ClassName.get("foo.bar", "User") as TypeName))
            assertThat(insertion.returnType.typeName(), `is`(TypeName.LONG))
        }.compilesWithoutError()
    }

    @Test
    fun insertNotAnEntity() {
        singleInsertMethod(
                """
                @Insert
                abstract public void foo(NotAnEntity notValid);
                """) { insertion, invocation ->
            assertThat(insertion.name, `is`("foo"))
            assertThat(insertion.parameters.size, `is`(1))
            val param = insertion.parameters.first()
            assertThat(param.entityType, `is`(nullValue()))
            assertThat(insertion.entity, `is`(nullValue()))
        }.failsToCompile().withErrorContaining(
                ProcessorErrors.CANNOT_FIND_ENTITY_FOR_INSERT_PARAMETER
        )
    }

    @Test
    fun insertTwo() {
        singleInsertMethod(
                """
                @Insert
                abstract public long[] foo(User u1, User u2);
                """) { insertion, invocation ->
            assertThat(insertion.name, `is`("foo"))

            assertThat(insertion.parameters.size, `is`(2))
            insertion.parameters.forEach {
                assertThat(it.type.typeName(), `is`(USER_TYPE_NAME))
                assertThat(it.entityType?.typeName(), `is`(USER_TYPE_NAME))
            }
            assertThat(insertion.entity?.typeName, `is`(USER_TYPE_NAME))
            assertThat(insertion.parameters.map { it.name }, `is`(listOf("u1", "u2")))
            assertThat(insertion.returnType.typeName(),
                    `is`(ArrayTypeName.of(TypeName.LONG) as TypeName))
        }.compilesWithoutError()
    }

    @Test
    fun insertList() {
        singleInsertMethod(
                """
                @Insert
                abstract public List<Long> insertUsers(List<User> users);
                """) { insertion, invocation ->
            assertThat(insertion.name, `is`("insertUsers"))
            assertThat(insertion.parameters.size, `is`(1))
            val param = insertion.parameters.first()
            assertThat(param.type.typeName(), `is`(
                    ParameterizedTypeName.get(
                            ClassName.get("java.util", "List"), USER_TYPE_NAME) as TypeName))
            assertThat(param.entityType?.typeName(), `is`(USER_TYPE_NAME))
            assertThat(insertion.entity?.typeName, `is`(USER_TYPE_NAME))
            assertThat(insertion.returnType.typeName(), `is`(
                    ParameterizedTypeName.get(ClassName.get("java.util", "List"),
                            ClassName.get("java.lang", "Long")) as TypeName
            ))
        }.compilesWithoutError()
    }

    @Test
    fun insertArray() {
        singleInsertMethod(
                """
                @Insert
                abstract public void insertUsers(User[] users);
                """) { insertion, invocation ->
            assertThat(insertion.name, `is`("insertUsers"))
            assertThat(insertion.parameters.size, `is`(1))
            val param = insertion.parameters.first()
            assertThat(param.type.typeName(), `is`(
                    ArrayTypeName.of(COMMON.USER_TYPE_NAME) as TypeName))
            assertThat(insertion.entity?.typeName,
                    `is`(ClassName.get("foo.bar", "User") as TypeName))
            assertThat(insertion.returnType.typeName(), `is`(TypeName.VOID))
        }.compilesWithoutError()
    }

    @Test
    fun insertSet() {
        singleInsertMethod(
                """
                @Insert
                abstract public void insertUsers(Set<User> users);
                """) { insertion, invocation ->
            assertThat(insertion.name, `is`("insertUsers"))
            assertThat(insertion.parameters.size, `is`(1))
            val param = insertion.parameters.first()
            assertThat(param.type.typeName(), `is`(
                    ParameterizedTypeName.get(ClassName.get("java.util", "Set")
                            , COMMON.USER_TYPE_NAME) as TypeName))
            assertThat(insertion.entity?.typeName,
                    `is`(ClassName.get("foo.bar", "User") as TypeName))
            assertThat(insertion.returnType.typeName(), `is`(TypeName.VOID))
        }.compilesWithoutError()
    }

    @Test
    fun insertDifferentTypes() {
        singleInsertMethod(
                """
                @Insert
                abstract public void foo(User u1, Book b1);
                """) { insertion, invocation ->
            assertThat(insertion.parameters.size, `is`(2))
            assertThat(insertion.parameters[0].type.typeName().toString(),
                    `is`("foo.bar.User"))
            assertThat(insertion.parameters[1].type.typeName().toString(),
                    `is`("foo.bar.Book"))
            assertThat(insertion.parameters.map { it.name }, `is`(listOf("u1", "b1")))
            assertThat(insertion.returnType.typeName(), `is`(TypeName.VOID))
        }.failsToCompile().withErrorContaining(
                // TODO we can support this.
                ProcessorErrors.INSERTION_METHOD_PARAMETERS_MUST_HAVE_THE_SAME_ENTITY_TYPE
        )
    }

    @Test
    fun onConflict_Default() {
        singleInsertMethod(
                """
                @Insert
                abstract public void foo(User user);
                """) { insertion, invocation ->
            assertThat(insertion.onConflict, `is`(Insert.ABORT))
        }.compilesWithoutError()
    }

    @Test
    fun onConflict_Invalid() {
        singleInsertMethod(
                """
                @Insert(onConflict = -1)
                abstract public void foo(User user);
                """) { insertion, invocation ->
        }.failsToCompile().withErrorContaining(ProcessorErrors.INVALID_ON_CONFLICT_VALUE)
    }

    @Test
    fun onConflict_EachValue() {
        listOf(
                Pair("REPLACE", 1),
                Pair("ROLLBACK", 2),
                Pair("ABORT", 3),
                Pair("FAIL", 4),
                Pair("IGNORE", 5)
        ).forEach { pair ->
            singleInsertMethod(
                    """
                @Insert(onConflict=Insert.${pair.first})
                abstract public void foo(User user);
                """) { insertion, invocation ->
                assertThat(insertion.onConflict, `is`(pair.second))
            }.compilesWithoutError()
        }
    }

    @Test
    fun invalidReturnType() {
        singleInsertMethod(
                """
                @Insert
                abstract public int foo(User user);
                """) { insertion, invocation ->
            assertThat(insertion.insertionType, `is`(nullValue()))
        }.failsToCompile().withErrorContaining(
                ProcessorErrors.INVALID_INSERTION_METHOD_RETURN_TYPE)
    }

    @Test
    fun validReturnTypes() {
        listOf(
            Triple("void", Type.INSERT_VOID, Type.INSERT_VOID),
            Triple("long", Type.INSERT_SINGLE_ID, Type.INSERT_SINGLE_ID),
            Triple("long[]", Type.INSERT_SINGLE_ID, Type.INSERT_ID_ARRAY),
            Triple("List<Long>", Type.INSERT_SINGLE_ID, Type.INSERT_ID_LIST)
        ).forEach { triple ->
            singleInsertMethod(
                    """
                @Insert
                abstract public ${triple.first} foo(User user);
                """) { insertion, invocation ->
                assertThat(insertion.insertMethodTypeFor(insertion.parameters.first()),
                        `is`(triple.second))
                assertThat(insertion.insertionType, `is`(triple.third))
            }.compilesWithoutError()
        }
    }

    fun singleInsertMethod(vararg input: String,
                          handler: (InsertionMethod, TestInvocation) -> Unit):
            CompileTester {
        return assertAbout(JavaSourcesSubjectFactory.javaSources())
                .that(listOf(JavaFileObjects.forSourceString("foo.bar.MyClass",
                        DAO_PREFIX + input.joinToString("\n") + DAO_SUFFIX
                ), COMMON.USER, COMMON.BOOK, COMMON.NOT_AN_ENTITY))
                .processedWith(TestProcessor.builder()
                        .forAnnotations(Insert::class, Dao::class)
                        .nextRunHandler { invocation ->
                            val (owner, methods) = invocation.roundEnv
                                    .getElementsAnnotatedWith(Dao::class.java)
                                    .map {
                                        Pair(it,
                                                invocation.processingEnv.elementUtils
                                                        .getAllMembers(MoreElements.asType(it))
                                                        .filter {
                                                            MoreElements.isAnnotationPresent(it,
                                                                    Insert::class.java)
                                                        }
                                        )
                                    }.filter { it.second.isNotEmpty() }.first()
                            val processor = InsertionMethodProcessor(invocation.context)
                            val processed = processor.parse(MoreTypes.asDeclared(owner.asType()),
                                    MoreElements.asExecutable(methods.first()))
                            handler(processed, invocation)
                            true
                        }
                        .build())
    }
}
