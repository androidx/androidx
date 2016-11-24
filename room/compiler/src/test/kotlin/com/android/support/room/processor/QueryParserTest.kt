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
import com.android.support.room.Query
import com.android.support.room.testing.TestProcessor
import com.android.support.room.vo.QueryMethod
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.google.common.truth.Truth.assertAbout
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourceSubjectFactory
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeVariableName
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
@RunWith(JUnit4::class)
class QueryParserTest {
    companion object {
        const val DAO_PREFIX = """
                package foo.bar;
                import com.android.support.room.*;
                @Dao
                abstract class MyClass {
                """
        const val DAO_SUFFIX = "}"

    }

    @Test
    fun testReadNoParams() {
        singleQueryMethod(
                """
                @Query("SELECT * from users")
                abstract public void foo();
                """) { parsedQuery ->
            assertThat(parsedQuery.name, `is`("foo"))
            assertThat(parsedQuery.parameters.size, `is`(0))
            assertThat(parsedQuery.returnType, `is`(TypeName.VOID))
        }.compilesWithoutError()
    }

    @Test
    fun testSingleParam() {
        singleQueryMethod(
                """
                @Query("SELECT * from users")
                abstract public long foo(int x);
                """) { parsedQuery ->
            assertThat(parsedQuery.name, `is`("foo"))
            assertThat(parsedQuery.returnType, `is`(TypeName.LONG))
            assertThat(parsedQuery.parameters.size, `is`(1))
            val param = parsedQuery.parameters.first()
            assertThat(param.name, `is`("x"))
            assertThat(param.type, `is`(TypeName.INT))
        }.compilesWithoutError()
    }

    @Test
    fun testGenericReturnType() {
        singleQueryMethod(
                """
                @Query("select * from users")
                abstract public <T> java.util.List<T> foo(int x);
                """) { parsedQuery ->
            val expected: TypeName = ParameterizedTypeName.get(ClassName.get(List::class.java),
                    TypeVariableName.get("T"))
            assertThat(parsedQuery.returnType, `is`(expected))
        }.failsToCompile()
                .withErrorContaining(ProcessorErrors.CANNOT_USE_UNBOUND_GENERICS_IN_QUERY_METHODS)
    }

    @Test
    fun testBadQuery() {
        singleQueryMethod(
                """
                @Query("select * from :1 :2")
                abstract public long foo(int x);
                """) { parsedQuery ->
            // do nothing
        }.failsToCompile()
                .withErrorContaining("mismatched input")
    }

    @Test
    fun testBoundGeneric() {
        singleQueryMethod(
                """
                static abstract class BaseModel<T> {
                    @Query("select COUNT(*) from users")
                    abstract public T getT();
                }
                @Dao
                static abstract class ExtendingModel extends BaseModel<Integer> {
                }
                """) { parsedQuery ->
            assertThat(parsedQuery.returnType, `is`(ClassName.get(Integer::class.java) as TypeName))
        }.compilesWithoutError()
    }

    @Test
    fun testBoundGenericParameter() {
        singleQueryMethod(
                """
                static abstract class BaseModel<T> {
                    @Query("select COUNT(*) from users where :t")
                    abstract public int getT(T t);
                }
                @Dao
                static abstract class ExtendingModel extends BaseModel<Integer> {
                }
                """) { parsedQuery ->
            assertThat(parsedQuery.parameters.first().type,
                    `is`(ClassName.get(Integer::class.java) as TypeName))
        }.compilesWithoutError()
    }


    fun singleQueryMethod(vararg input: String,
                          handler: (QueryMethod) -> Unit):
            CompileTester {
        return assertAbout(JavaSourceSubjectFactory.javaSource())
                .that(JavaFileObjects.forSourceString("foo.bar.MyClass",
                        DAO_PREFIX + input.joinToString("\n") + DAO_SUFFIX
                ))
                .processedWith(TestProcessor.builder()
                        .forAnnotations(Query::class, Dao::class)
                        .nextRunHandler { invocation ->
                            val (owner, methods) = invocation.roundEnv
                                    .getElementsAnnotatedWith(Dao::class.java)
                                    .map {
                                        Pair(it,
                                                invocation.processingEnv.elementUtils
                                                        .getAllMembers(MoreElements.asType(it))
                                                        .filter {
                                                            MoreElements.isAnnotationPresent(it,
                                                                    Query::class.java)
                                                        }
                                        )
                                    }.filter { it.second.isNotEmpty() }.first()
                            val parser = QueryParser(invocation.roundEnv, invocation.processingEnv)
                            val parsedQuery = parser.parse(MoreTypes.asDeclared(owner.asType()),
                                    MoreElements.asExecutable(methods.first()))
                            handler(parsedQuery)
                            true
                        }
                        .build())
    }
}