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

import com.android.support.room.Dao
import com.android.support.room.Delete
import com.android.support.room.ext.typeName
import com.android.support.room.testing.TestInvocation
import com.android.support.room.testing.TestProcessor
import com.android.support.room.vo.DeletionMethod
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.google.common.truth.Truth
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubjectFactory
import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
@RunWith(JUnit4::class)
class DeletionMethodProcessorTest {
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
    fun deleteNoParams() {
        singleDeletion(
                """
                @Delete
                abstract public void foo();
                """) { deletion, invocation ->
            MatcherAssert.assertThat(deletion.name, `is`("foo"))
            MatcherAssert.assertThat(deletion.parameters.size, `is`(0))
            MatcherAssert.assertThat(deletion.returnCount, `is`(false))
        }.failsToCompile().withErrorContaining(
                ProcessorErrors.DELETION_DOES_NOT_HAVE_ANY_PARAMETERS_TO_DELETE)
    }

    @Test
    fun deleteSingle() {
        singleDeletion(
                """
                @Delete
                abstract public int foo(User user);
                """) { deletion, invocation ->
            MatcherAssert.assertThat(deletion.name, `is`("foo"))
            MatcherAssert.assertThat(deletion.parameters.size, `is`(1))
            val param = deletion.parameters.first()
            MatcherAssert.assertThat(param.type.typeName(), `is`(USER_TYPE_NAME))
            MatcherAssert.assertThat(param.entityType?.typeName(), `is`(USER_TYPE_NAME))
            MatcherAssert.assertThat(deletion.entity?.typeName,
                    `is`(ClassName.get("foo.bar", "User") as TypeName))
            MatcherAssert.assertThat(deletion.returnCount, `is`(true))
        }.compilesWithoutError()
    }

    @Test
    fun deleteNotAnEntity() {
        singleDeletion(
                """
                @Delete
                abstract public void foo(NotAnEntity notValid);
                """) { deletion, invocation ->
            MatcherAssert.assertThat(deletion.name, `is`("foo"))
            MatcherAssert.assertThat(deletion.parameters.size, `is`(1))
            val param = deletion.parameters.first()
            MatcherAssert.assertThat(param.entityType, `is`(nullValue()))
            MatcherAssert.assertThat(deletion.entity, `is`(nullValue()))
        }.failsToCompile().withErrorContaining(
                ProcessorErrors.CANNOT_FIND_ENTITY_FOR_SHORTCUT_QUERY_PARAMETER
        )
    }

    @Test
    fun deleteTwo() {
        singleDeletion(
                """
                @Delete
                abstract public void foo(User u1, User u2);
                """) { deletion, invocation ->
            MatcherAssert.assertThat(deletion.name, `is`("foo"))

            MatcherAssert.assertThat(deletion.parameters.size, `is`(2))
            deletion.parameters.forEach {
                MatcherAssert.assertThat(it.type.typeName(), `is`(USER_TYPE_NAME))
                MatcherAssert.assertThat(it.entityType?.typeName(), `is`(USER_TYPE_NAME))
            }
            MatcherAssert.assertThat(deletion.entity?.typeName, `is`(USER_TYPE_NAME))
            MatcherAssert.assertThat(deletion.parameters.map { it.name }, `is`(listOf("u1", "u2")))
            MatcherAssert.assertThat(deletion.returnCount, `is`(false))
        }.compilesWithoutError()
    }

    @Test
    fun deleteList() {
        singleDeletion(
                """
                @Delete
                abstract public int deleteUsers(List<User> users);
                """) { deletion, invocation ->
            MatcherAssert.assertThat(deletion.name, `is`("deleteUsers"))
            MatcherAssert.assertThat(deletion.parameters.size, `is`(1))
            val param = deletion.parameters.first()
            MatcherAssert.assertThat(param.type.typeName(), `is`(
                    ParameterizedTypeName.get(
                            ClassName.get("java.util", "List"), USER_TYPE_NAME) as TypeName))
            MatcherAssert.assertThat(param.entityType?.typeName(), `is`(USER_TYPE_NAME))
            MatcherAssert.assertThat(deletion.entity?.typeName, `is`(USER_TYPE_NAME))
            MatcherAssert.assertThat(deletion.returnCount, `is`(true))
        }.compilesWithoutError()
    }

    @Test
    fun deleteArray() {
        singleDeletion(
                """
                @Delete
                abstract public void deleteUsers(User[] users);
                """) { deletion, invocation ->
            MatcherAssert.assertThat(deletion.name, `is`("deleteUsers"))
            MatcherAssert.assertThat(deletion.parameters.size, `is`(1))
            val param = deletion.parameters.first()
            MatcherAssert.assertThat(param.type.typeName(), `is`(
                    ArrayTypeName.of(COMMON.USER_TYPE_NAME) as TypeName))
            MatcherAssert.assertThat(deletion.entity?.typeName,
                    `is`(ClassName.get("foo.bar", "User") as TypeName))
            MatcherAssert.assertThat(deletion.returnCount, `is`(false))
        }.compilesWithoutError()
    }

    @Test
    fun deleteSet() {
        singleDeletion(
                """
                @Delete
                abstract public void deleteUsers(Set<User> users);
                """) { deletion, invocation ->
            MatcherAssert.assertThat(deletion.name, `is`("deleteUsers"))
            MatcherAssert.assertThat(deletion.parameters.size, `is`(1))
            val param = deletion.parameters.first()
            MatcherAssert.assertThat(param.type.typeName(), `is`(
                    ParameterizedTypeName.get(ClassName.get("java.util", "Set")
                            , COMMON.USER_TYPE_NAME) as TypeName))
            MatcherAssert.assertThat(deletion.entity?.typeName,
                    `is`(ClassName.get("foo.bar", "User") as TypeName))
            MatcherAssert.assertThat(deletion.returnCount, `is`(false))
        }.compilesWithoutError()
    }

    @Test
    fun deleteDifferentTypes() {
        singleDeletion(
                """
                @Delete
                abstract public void foo(User u1, Book b1);
                """) { deletion, invocation ->
            MatcherAssert.assertThat(deletion.parameters.size, `is`(2))
            MatcherAssert.assertThat(deletion.parameters[0].type.typeName().toString(),
                    `is`("foo.bar.User"))
            MatcherAssert.assertThat(deletion.parameters[1].type.typeName().toString(),
                    `is`("foo.bar.Book"))
            MatcherAssert.assertThat(deletion.parameters.map { it.name }, `is`(listOf("u1", "b1")))
            MatcherAssert.assertThat(deletion.returnCount, `is`(false))
        }.failsToCompile().withErrorContaining(
                ProcessorErrors.DELETION_METHOD_PARAMETERS_MUST_HAVE_THE_SAME_ENTITY_TYPE
        )
    }

    @Test
    fun invalidReturnType() {
        singleDeletion(
                """
                @Delete
                abstract public long foo(User user);
                """) { deletion, invocation ->
        }.failsToCompile().withErrorContaining(
                ProcessorErrors.DELETION_METHODS_MUST_RETURN_VOID_OR_INT)
    }

    fun singleDeletion(vararg input: String,
                       handler: (DeletionMethod, TestInvocation) -> Unit):
            CompileTester {
        return Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
                .that(listOf(JavaFileObjects.forSourceString("foo.bar.MyClass",
                        DAO_PREFIX + input.joinToString("\n") + DAO_SUFFIX
                ), COMMON.USER, COMMON.BOOK, COMMON.NOT_AN_ENTITY))
                .processedWith(TestProcessor.builder()
                        .forAnnotations(Delete::class, Dao::class)
                        .nextRunHandler { invocation ->
                            val (owner, methods) = invocation.roundEnv
                                    .getElementsAnnotatedWith(Dao::class.java)
                                    .map {
                                        Pair(it,
                                                invocation.processingEnv.elementUtils
                                                        .getAllMembers(MoreElements.asType(it))
                                                        .filter {
                                                            MoreElements.isAnnotationPresent(it,
                                                                    Delete::class.java)
                                                        }
                                        )
                                    }.filter { it.second.isNotEmpty() }.first()
                            val processor = DeletionMethodProcessor(invocation.context)
                            val processed = processor.parse(MoreTypes.asDeclared(owner.asType()),
                                    MoreElements.asExecutable(methods.first()))
                            handler(processed, invocation)
                            true
                        }
                        .build())
    }
}
