/*
 * Copyright 2018 The Android Open Source Project
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

package android.arch.persistence.room.processor

import COMMON
import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.arch.persistence.room.Query
import android.arch.persistence.room.RawQuery
import android.arch.persistence.room.ext.CommonTypeNames
import android.arch.persistence.room.ext.SupportDbTypeNames
import android.arch.persistence.room.ext.hasAnnotation
import android.arch.persistence.room.ext.typeName
import android.arch.persistence.room.testing.TestInvocation
import android.arch.persistence.room.testing.TestProcessor
import android.arch.persistence.room.vo.RawQueryMethod
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.google.common.truth.Truth
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubjectFactory
import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class RawQueryMethodProcessorTest {
    @Test
    fun supportRawQuery() {
        singleQueryMethod(
                """
                @RawQuery
                abstract public int[] foo(SupportSQLiteQuery query);
                """) { query, _ ->
            assertThat(query.name, `is`("foo"))
            assertThat(query.runtimeQueryParam, `is`(
                    RawQueryMethod.RuntimeQueryParameter(
                            paramName = "query",
                            type = SupportDbTypeNames.QUERY
                    )
            ))
            assertThat(query.returnType.typeName(),
                    `is`(ArrayTypeName.of(TypeName.INT) as TypeName))
        }.compilesWithoutError()
    }

    @Test
    fun stringRawQuery() {
        singleQueryMethod(
                """
                @RawQuery
                abstract public int[] foo(String query);
                """) { query, _ ->
            assertThat(query.name, `is`("foo"))
            assertThat(query.runtimeQueryParam, `is`(
                    RawQueryMethod.RuntimeQueryParameter(
                            paramName = "query",
                            type = CommonTypeNames.STRING
                    )
            ))
            assertThat(query.returnType.typeName(),
                    `is`(ArrayTypeName.of(TypeName.INT) as TypeName))
        }.compilesWithoutError()
    }

    @Test
    fun withObservedEntities() {
        singleQueryMethod(
                """
                @RawQuery(observedEntities = User.class)
                abstract public LiveData<User> foo(SupportSQLiteQuery query);
                """) { query, _ ->
            assertThat(query.name, `is`("foo"))
            assertThat(query.runtimeQueryParam, `is`(
                    RawQueryMethod.RuntimeQueryParameter(
                            paramName = "query",
                            type = SupportDbTypeNames.QUERY
                    )
            ))
            assertThat(query.observedEntities.size, `is`(1))
            assertThat(
                    query.observedEntities.first().typeName,
                    `is`(COMMON.USER_TYPE_NAME as TypeName))
        }.compilesWithoutError()
    }

    @Test
    fun observableWithoutEntities() {
        singleQueryMethod(
                """
                @RawQuery(observedEntities = {})
                abstract public LiveData<User> foo(SupportSQLiteQuery query);
                """) { query, _ ->
            assertThat(query.name, `is`("foo"))
            assertThat(query.runtimeQueryParam, `is`(
                    RawQueryMethod.RuntimeQueryParameter(
                            paramName = "query",
                            type = SupportDbTypeNames.QUERY
                    )
            ))
            assertThat(query.observedEntities, `is`(emptyList()))
        }.failsToCompile()
                .withErrorContaining(ProcessorErrors.OBSERVABLE_QUERY_NOTHING_TO_OBSERVE)
    }

    @Test
    fun pojo() {
        val pojo: TypeName = ClassName.get("foo.bar.MyClass", "MyPojo")
        singleQueryMethod(
                """
                public class MyPojo {
                    public String foo;
                    public String bar;
                }

                @RawQuery
                abstract public MyPojo foo(SupportSQLiteQuery query);
                """) { query, _ ->
            assertThat(query.name, `is`("foo"))
            assertThat(query.runtimeQueryParam, `is`(
                    RawQueryMethod.RuntimeQueryParameter(
                            paramName = "query",
                            type = SupportDbTypeNames.QUERY
                    )
            ))
            assertThat(query.returnType.typeName(), `is`(pojo))
            assertThat(query.observedEntities, `is`(emptyList()))
        }.compilesWithoutError()
    }

    @Test
    fun void() {
        singleQueryMethod(
                """
                @RawQuery
                abstract public void foo(SupportSQLiteQuery query);
                """) { _, _ ->
        }.failsToCompile().withErrorContaining(
                ProcessorErrors.RAW_QUERY_BAD_RETURN_TYPE
        )
    }

    @Test
    fun noArgs() {
        singleQueryMethod(
                """
                @RawQuery
                abstract public int[] foo();
                """) { _, _ ->
        }.failsToCompile().withErrorContaining(
                ProcessorErrors.RAW_QUERY_BAD_PARAMS
        )
    }

    @Test
    fun tooManyArgs() {
        singleQueryMethod(
                """
                @RawQuery
                abstract public int[] foo(String query, String query2);
                """) { _, _ ->
        }.failsToCompile().withErrorContaining(
                ProcessorErrors.RAW_QUERY_BAD_PARAMS
        )
    }

    @Test
    fun varargs() {
        singleQueryMethod(
                """
                @RawQuery
                abstract public int[] foo(String... query);
                """) { _, _ ->
        }.failsToCompile().withErrorContaining(
                ProcessorErrors.RAW_QUERY_BAD_PARAMS
        )
    }

    private fun singleQueryMethod(
            vararg input: String,
            handler: (RawQueryMethod, TestInvocation) -> Unit
    ): CompileTester {
        return Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
                .that(listOf(JavaFileObjects.forSourceString("foo.bar.MyClass",
                        DAO_PREFIX
                                + input.joinToString("\n")
                                + DAO_SUFFIX
                ), COMMON.LIVE_DATA, COMMON.COMPUTABLE_LIVE_DATA, COMMON.USER))
                .processedWith(TestProcessor.builder()
                        .forAnnotations(Query::class, Dao::class, ColumnInfo::class,
                                Entity::class, PrimaryKey::class, RawQueryMethod::class)
                        .nextRunHandler { invocation ->
                            val (owner, methods) = invocation.roundEnv
                                    .getElementsAnnotatedWith(Dao::class.java)
                                    .map {
                                        Pair(it,
                                                invocation.processingEnv.elementUtils
                                                        .getAllMembers(MoreElements.asType(it))
                                                        .filter {
                                                            it.hasAnnotation(RawQuery::class)
                                                        }
                                        )
                                    }.first { it.second.isNotEmpty() }
                            val parser = RawQueryMethodProcessor(
                                    baseContext = invocation.context,
                                    containing = MoreTypes.asDeclared(owner.asType()),
                                    executableElement = MoreElements.asExecutable(methods.first()))
                            val parsedQuery = parser.process()
                            handler(parsedQuery, invocation)
                            true
                        }
                        .build())
    }

    companion object {
        private const val DAO_PREFIX = """
                package foo.bar;
                import android.support.annotation.NonNull;
                import android.arch.persistence.room.*;
                import android.arch.persistence.db.SupportSQLiteQuery;
                import android.arch.lifecycle.LiveData;
                @Dao
                abstract class MyClass {
                """
        private const val DAO_SUFFIX = "}"
    }
}