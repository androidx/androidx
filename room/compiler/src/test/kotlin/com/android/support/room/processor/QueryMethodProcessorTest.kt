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
import com.android.support.room.ext.hasAnnotation
import com.android.support.room.ext.typeName
import com.android.support.room.testing.TestInvocation
import com.android.support.room.testing.TestProcessor
import com.android.support.room.vo.QueryMethod
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.google.common.truth.Truth.assertAbout
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourceSubjectFactory
import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeVariableName
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import javax.lang.model.type.TypeKind.INT
import javax.lang.model.type.TypeMirror

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
@RunWith(JUnit4::class)
class QueryMethodProcessorTest {
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
                abstract public int[] foo();
                """) { parsedQuery, invocation ->
            assertThat(parsedQuery.name, `is`("foo"))
            assertThat(parsedQuery.parameters.size, `is`(0))
            assertThat(parsedQuery.returnType.typeName(),
                    `is`(ArrayTypeName.of(TypeName.INT) as TypeName))
        }.compilesWithoutError()
    }

    @Test
    fun testSingleParam() {
        singleQueryMethod(
                """
                @Query("SELECT * from users")
                abstract public long foo(int x);
                """) { parsedQuery, invocation ->
            assertThat(parsedQuery.name, `is`("foo"))
            assertThat(parsedQuery.returnType.typeName(), `is`(TypeName.LONG))
            assertThat(parsedQuery.parameters.size, `is`(1))
            val param = parsedQuery.parameters.first()
            assertThat(param.name, `is`("x"))
            assertThat(param.type,
                    `is`(invocation.processingEnv.typeUtils.getPrimitiveType(INT) as TypeMirror))
        }.compilesWithoutError()
    }

    @Test
    fun testVarArgs() {
        singleQueryMethod(
                """
                @Query("SELECT * from users where id in (?)")
                abstract public long foo(int... ids);
                """) { parsedQuery, invocation ->
            assertThat(parsedQuery.name, `is`("foo"))
            assertThat(parsedQuery.returnType.typeName(), `is`(TypeName.LONG))
            assertThat(parsedQuery.parameters.size, `is`(1))
            val param = parsedQuery.parameters.first()
            assertThat(param.name, `is`("ids"))
            val types = invocation.processingEnv.typeUtils
            assertThat(param.type,
                    `is`(types.getArrayType(types.getPrimitiveType(INT)) as TypeMirror))
        }.compilesWithoutError()
    }

    @Test
    fun testParamBindingMatchingNoName() {
        singleQueryMethod(
                """
                @Query("SELECT id from users where id = ?")
                abstract public long getIdById(int id);
                """) { parsedQuery, invocation ->
            val section = parsedQuery.query.bindSections.first()
            val param = parsedQuery.parameters.firstOrNull()
            assertThat(section, notNullValue())
            assertThat(param, notNullValue())
            assertThat(parsedQuery.sectionToParamMapping, `is`(listOf(Pair(section, param))))
        }.compilesWithoutError()
    }

    @Test
    fun testParamBindingMatchingSimpleBind() {
        singleQueryMethod(
                """
                @Query("SELECT id from users where id = :id")
                abstract public long getIdById(int id);
                """) { parsedQuery, invocation ->
            val section = parsedQuery.query.bindSections.first()
            val param = parsedQuery.parameters.firstOrNull()
            assertThat(section, notNullValue())
            assertThat(param, notNullValue())
            assertThat(parsedQuery.sectionToParamMapping,
                    `is`(listOf(Pair(section, param))))
        }.compilesWithoutError()
    }

    @Test
    fun testParamBindingTwoBindVarsIntoTheSameParameter() {
        singleQueryMethod(
                """
                @Query("SELECT id from users where id = :id OR uid = :id")
                abstract public long getIdById(int id);
                """) { parsedQuery, invocation ->
            val section = parsedQuery.query.bindSections[0]
            val section2 = parsedQuery.query.bindSections[1]
            val param = parsedQuery.parameters.firstOrNull()
            assertThat(section, notNullValue())
            assertThat(section2, notNullValue())
            assertThat(param, notNullValue())
            assertThat(parsedQuery.sectionToParamMapping,
                    `is`(listOf(Pair(section, param), Pair(section2, param))))
        }.compilesWithoutError()
    }

    @Test
    fun testMissingParameterForBinding() {
        singleQueryMethod(
                """
                @Query("SELECT id from users where id = :id OR uid = :uid")
                abstract public long getIdById(int id);
                """) { parsedQuery, invocation ->
            val section = parsedQuery.query.bindSections[0]
            val section2 = parsedQuery.query.bindSections[1]
            val param = parsedQuery.parameters.firstOrNull()
            assertThat(section, notNullValue())
            assertThat(section2, notNullValue())
            assertThat(param, notNullValue())
            assertThat(parsedQuery.sectionToParamMapping,
                    `is`(listOf(Pair(section, param), Pair(section2, null))))
        }
                .failsToCompile()
                .withErrorContaining(
                        ProcessorErrors.missingParameterForBindVariable(listOf(":uid")))
    }

    @Test
    fun test2MissingParameterForBinding() {
        singleQueryMethod(
                """
                @Query("SELECT id from users where foo = :bar AND id = :id OR uid = :uid")
                abstract public long getIdById(int id);
                """) { parsedQuery, invocation ->
            val bar = parsedQuery.query.bindSections[0]
            val id = parsedQuery.query.bindSections[1]
            val uid = parsedQuery.query.bindSections[2]
            val param = parsedQuery.parameters.firstOrNull()
            assertThat(bar, notNullValue())
            assertThat(id, notNullValue())
            assertThat(uid, notNullValue())
            assertThat(param, notNullValue())
            assertThat(parsedQuery.sectionToParamMapping,
                    `is`(listOf(Pair(bar, null), Pair(id, param), Pair(uid, null))))
        }
                .failsToCompile()
                .withErrorContaining(
                        ProcessorErrors.missingParameterForBindVariable(listOf(":bar", ":uid")))
    }

    @Test
    fun testUnusedParameters() {
        singleQueryMethod(
                """
                @Query("SELECT id from users where foo = :bar")
                abstract public long getIdById(int bar, int whyNotUseMe);
                """) { parsedQuery, invocation ->
            val bar = parsedQuery.query.bindSections[0]
            val barParam = parsedQuery.parameters.firstOrNull()
            assertThat(bar, notNullValue())
            assertThat(barParam, notNullValue())
            assertThat(parsedQuery.sectionToParamMapping,
                    `is`(listOf(Pair(bar, barParam))))
        }.compilesWithoutError().withWarningContaining(
                ProcessorErrors.unusedQueryMethodParameter(listOf("whyNotUseMe")))
    }

    @Test
    fun testNameWithUnderscore() {
        singleQueryMethod(
                """
                @Query("select * from users where id = :_blah")
                abstract public long getSth(int _blah);
                """
        ){parsedQuery, invocation -> }
                .failsToCompile()
                .withErrorContaining(ProcessorErrors.QUERY_PARAMETERS_CANNOT_START_WITH_UNDERSCORE)
    }

    @Test
    fun testGenericReturnType() {
        singleQueryMethod(
                """
                @Query("select * from users")
                abstract public <T> java.util.List<T> foo(int x);
                """) { parsedQuery, invocation ->
            val expected: TypeName = ParameterizedTypeName.get(ClassName.get(List::class.java),
                    TypeVariableName.get("T"))
            assertThat(parsedQuery.returnType.typeName(), `is`(expected))
        }.failsToCompile()
                .withErrorContaining(ProcessorErrors.CANNOT_USE_UNBOUND_GENERICS_IN_QUERY_METHODS)
    }

    @Test
    fun testBadQuery() {
        singleQueryMethod(
                """
                @Query("select * from :1 :2")
                abstract public long foo(int x);
                """) { parsedQuery, invocation ->
            // do nothing
        }.failsToCompile()
                .withErrorContaining("UNEXPECTED_CHAR=:")
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
                """) { parsedQuery, invocation ->
            assertThat(parsedQuery.returnType.typeName(),
                    `is`(ClassName.get(Integer::class.java) as TypeName))
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
                """) { parsedQuery, invocation ->
            assertThat(parsedQuery.parameters.first().type,
                    `is`(invocation.processingEnv.elementUtils
                            .getTypeElement("java.lang.Integer").asType()))
        }.compilesWithoutError()
    }

    @Test
    fun testReadDeleteWithBadReturnType() {
        singleQueryMethod(
                """
                @Query("DELETE FROM users where id = ?")
                abstract public float foo(int id);
                """) { parsedQuery, invocation ->
        }.failsToCompile().withErrorContaining(
                ProcessorErrors.DELETION_METHODS_MUST_RETURN_VOID_OR_INT
        )
    }

    @Test
    fun testSimpleDelete() {
        singleQueryMethod(
                """
                @Query("DELETE FROM users where id = ?")
                abstract public int foo(int id);
                """) { parsedQuery, invocation ->
            assertThat(parsedQuery.name, `is`("foo"))
            assertThat(parsedQuery.parameters.size, `is`(1))
            assertThat(parsedQuery.returnType.typeName(), `is`(TypeName.INT))
        }.compilesWithoutError()
    }

    fun singleQueryMethod(vararg input: String,
                          handler: (QueryMethod, TestInvocation) -> Unit):
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
                                                            it.hasAnnotation(Query::class)
                                                        }
                                        )
                                    }.filter { it.second.isNotEmpty() }.first()
                            val parser = QueryMethodProcessor(invocation.context)
                            val parsedQuery = parser.parse(MoreTypes.asDeclared(owner.asType()),
                                    MoreElements.asExecutable(methods.first()))
                            handler(parsedQuery, invocation)
                            true
                        }
                        .build())
    }
}
