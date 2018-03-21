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

package androidx.room.processor

import COMMON
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.ext.PagingTypeNames
import androidx.room.ext.SupportDbTypeNames
import androidx.room.ext.hasAnnotation
import androidx.room.ext.typeName
import androidx.room.processor.ProcessorErrors.RAW_QUERY_STRING_PARAMETER_REMOVED
import androidx.room.testing.TestInvocation
import androidx.room.testing.TestProcessor
import androidx.room.vo.RawQueryMethod
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
                """) { _, _ ->
        }.failsToCompile().withErrorContaining(RAW_QUERY_STRING_PARAMETER_REMOVED)
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
            assertThat(query.observedTableNames.size, `is`(1))
            assertThat(query.observedTableNames, `is`(setOf("User")))
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
            assertThat(query.observedTableNames, `is`(emptySet()))
        }.failsToCompile()
                .withErrorContaining(ProcessorErrors.OBSERVABLE_QUERY_NOTHING_TO_OBSERVE)
    }

    @Test
    fun observableWithoutEntities_dataSourceFactory() {
        singleQueryMethod(
                """
                @RawQuery
                abstract public ${PagingTypeNames.DATA_SOURCE_FACTORY}<Integer, User> getOne();
                """) { _, _ ->
            // do nothing
        }.failsToCompile()
                .withErrorContaining(ProcessorErrors.OBSERVABLE_QUERY_NOTHING_TO_OBSERVE)
    }

    @Test
    fun observableWithoutEntities_positionalDataSource() {
        singleQueryMethod(
                """
                @RawQuery
                abstract public ${PagingTypeNames.POSITIONAL_DATA_SOURCE}<User> getOne();
                """) { _, _ ->
            // do nothing
        }.failsToCompile()
                .withErrorContaining(ProcessorErrors.OBSERVABLE_QUERY_NOTHING_TO_OBSERVE)
    }

    @Test
    fun positionalDataSource() {
        singleQueryMethod(
                """
                @RawQuery(observedEntities = {User.class})
                abstract public ${PagingTypeNames.POSITIONAL_DATA_SOURCE}<User> getOne(
                        SupportSQLiteQuery query);
                """) { _, _ ->
            // do nothing
        }.compilesWithoutError()
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
            assertThat(query.observedTableNames, `is`(emptySet()))
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
                abstract public int[] foo(SupportSQLiteQuery query,
                                          SupportSQLiteQuery query2);
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
                abstract public int[] foo(SupportSQLiteQuery... query);
                """) { _, _ ->
        }.failsToCompile().withErrorContaining(
                ProcessorErrors.RAW_QUERY_BAD_PARAMS
        )
    }

    @Test
    fun observed_notAnEntity() {
        singleQueryMethod(
                """
                @RawQuery(observedEntities = {${COMMON.NOT_AN_ENTITY_TYPE_NAME}.class})
                abstract public int[] foo(SupportSQLiteQuery query);
                """) { _, _ ->
        }.failsToCompile().withErrorContaining(
                ProcessorErrors.rawQueryBadEntity(COMMON.NOT_AN_ENTITY_TYPE_NAME)
        )
    }

    @Test
    fun observed_relationPojo() {
        singleQueryMethod(
                """
                public static class MyPojo {
                    public String foo;
                    @Relation(
                        parentColumn = "foo",
                        entityColumn = "name"
                    )
                    public java.util.List<User> users;
                }
                @RawQuery(observedEntities = MyPojo.class)
                abstract public int[] foo(SupportSQLiteQuery query);
                """) { method, _ ->
            assertThat(method.observedTableNames, `is`(setOf("User")))
        }.compilesWithoutError()
    }

    @Test
    fun observed_embedded() {
        singleQueryMethod(
                """
                public static class MyPojo {
                    public String foo;
                    @Embedded
                    public User users;
                }
                @RawQuery(observedEntities = MyPojo.class)
                abstract public int[] foo(SupportSQLiteQuery query);
                """) { method, _ ->
            assertThat(method.observedTableNames, `is`(setOf("User")))
        }.compilesWithoutError()
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
                ), COMMON.LIVE_DATA, COMMON.COMPUTABLE_LIVE_DATA, COMMON.USER,
                        COMMON.DATA_SOURCE_FACTORY, COMMON.POSITIONAL_DATA_SOURCE,
                        COMMON.NOT_AN_ENTITY))
                .processedWith(TestProcessor.builder()
                        .forAnnotations(Query::class, Dao::class, ColumnInfo::class,
                                Entity::class, PrimaryKey::class, RawQuery::class)
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
                import androidx.annotation.NonNull;
                import androidx.room.*;
                import androidx.sqlite.db.SupportSQLiteQuery;
                import androidx.lifecycle.LiveData;
                @Dao
                abstract class MyClass {
                """
        private const val DAO_SUFFIX = "}"
    }
}