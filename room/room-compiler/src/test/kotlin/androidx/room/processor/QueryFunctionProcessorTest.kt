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

package androidx.room.processor

import COMMON
import androidx.kruth.assertThat
import androidx.room.Dao
import androidx.room.Query
import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.XClassName
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.runProcessorTest
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.CommonTypeNames.LIST
import androidx.room.ext.CommonTypeNames.MUTABLE_LIST
import androidx.room.ext.CommonTypeNames.STRING
import androidx.room.ext.GuavaUtilConcurrentTypeNames
import androidx.room.ext.KotlinTypeNames
import androidx.room.ext.LifecyclesTypeNames
import androidx.room.ext.PagingTypeNames
import androidx.room.ext.ReactiveStreamsTypeNames
import androidx.room.ext.RxJava2TypeNames
import androidx.room.ext.RxJava3TypeNames
import androidx.room.parser.QueryType
import androidx.room.parser.Table
import androidx.room.processor.ProcessorErrors.CANNOT_USE_MAP_COLUMN_AND_MAP_INFO_SIMULTANEOUSLY
import androidx.room.processor.ProcessorErrors.DO_NOT_USE_GENERIC_IMMUTABLE_MULTIMAP
import androidx.room.processor.ProcessorErrors.MAP_INFO_MUST_HAVE_AT_LEAST_ONE_COLUMN_PROVIDED
import androidx.room.processor.ProcessorErrors.cannotFindQueryResultAdapter
import androidx.room.processor.ProcessorErrors.mayNeedMapColumn
import androidx.room.solver.query.result.DataClassRowAdapter
import androidx.room.solver.query.result.DataSourceFactoryQueryResultBinder
import androidx.room.solver.query.result.ListQueryResultAdapter
import androidx.room.solver.query.result.LiveDataQueryResultBinder
import androidx.room.solver.query.result.SingleColumnRowAdapter
import androidx.room.solver.query.result.SingleItemQueryResultAdapter
import androidx.room.testing.context
import androidx.room.vo.Property
import androidx.room.vo.QueryFunction
import androidx.room.vo.ReadQueryFunction
import androidx.room.vo.Warning
import androidx.room.vo.WriteQueryFunction
import createVerifierFromEntitiesAndViews
import mockElementAndType
import org.junit.AssumptionViolatedException
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.Mockito

@RunWith(Parameterized::class)
class QueryFunctionProcessorTest(private val enableVerification: Boolean) {
    companion object {
        const val DAO_PREFIX =
            """
                package foo.bar;
                import androidx.annotation.NonNull;
                import androidx.room.*;
                import java.util.*;
                import com.google.common.collect.*;
                @Dao
                abstract class MyClass {
                """
        const val DAO_PREFIX_KT =
            """
                package foo.bar
                import androidx.room.*
                import java.util.*
                import io.reactivex.*         
                import io.reactivex.rxjava3.core.*
                import androidx.lifecycle.*
                import com.google.common.util.concurrent.*
                import org.reactivestreams.*
                import kotlinx.coroutines.flow.*
            
                @Dao
                abstract class MyClass {
                """
        const val DAO_SUFFIX = "}"
        val DATA_CLASS = XClassName.get("foo.bar", "MyClass.DataClass")

        @Parameterized.Parameters(name = "enableDbVerification={0}")
        @JvmStatic
        fun getParams() = arrayOf(true, false)

        fun createField(name: String, columnName: String? = null): Property {
            val (element, type) = mockElementAndType()
            return Property(
                element = element,
                name = name,
                type = type,
                columnName = columnName ?: name,
                affinity = null
            )
        }
    }

    @Test
    fun testReadNoParams() {
        singleQueryMethod<ReadQueryFunction>(
            """
                @Query("SELECT uid from User")
                abstract public int[] foo();
            """
        ) { parsedQuery, _ ->
            assertThat(parsedQuery.element.jvmName).isEqualTo("foo")
            assertThat(parsedQuery.parameters.size).isEqualTo(0)
            assertThat(parsedQuery.returnType.asTypeName())
                .isEqualTo(XTypeName.getArrayName(XTypeName.PRIMITIVE_INT).copy(nullable = true))
        }
    }

    @Test
    fun testSingleParam() {
        singleQueryMethod<ReadQueryFunction>(
            """
                @Query("SELECT * from User where uid = :x")
                abstract public long foo(int x);
                """
        ) { parsedQuery, invocation ->
            assertThat(parsedQuery.element.jvmName).isEqualTo("foo")
            assertThat(parsedQuery.returnType.asTypeName()).isEqualTo(XTypeName.PRIMITIVE_LONG)
            assertThat(parsedQuery.parameters.size).isEqualTo(1)
            val param = parsedQuery.parameters.first()
            assertThat(param.name).isEqualTo("x")
            assertThat(param.sqlName).isEqualTo("x")
            assertThat(param.type)
                .isEqualTo(invocation.processingEnv.requireType(XTypeName.PRIMITIVE_INT))
        }
    }

    @Test
    fun testVarArgs() {
        singleQueryMethod<ReadQueryFunction>(
            """
                @Query("SELECT * from User where uid in (:ids)")
                abstract public long foo(int... ids);
                """
        ) { parsedQuery, _ ->
            assertThat(parsedQuery.element.jvmName).isEqualTo("foo")
            assertThat(parsedQuery.returnType.asTypeName()).isEqualTo(XTypeName.PRIMITIVE_LONG)
            assertThat(parsedQuery.parameters.size).isEqualTo(1)
            val param = parsedQuery.parameters.first()
            assertThat(param.name).isEqualTo("ids")
            assertThat(param.sqlName).isEqualTo("ids")
            assertThat(param.type.asTypeName())
                .isEqualTo(XTypeName.getArrayName(XTypeName.PRIMITIVE_INT).copy(nullable = true))
        }
    }

    @Test
    fun testParamBindingMatchingNoName() {
        singleQueryMethod<ReadQueryFunction>(
            """
                @Query("SELECT uid from User where uid = :id")
                abstract public long getIdById(int id);
                """
        ) { parsedQuery, _ ->
            val section = parsedQuery.query.bindSections.first()
            val param = parsedQuery.parameters.firstOrNull()
            assertThat(section).isNotNull()
            assertThat(param).isNotNull()
            assertThat(parsedQuery.sectionToParamMapping).isEqualTo(listOf(Pair(section, param)))
        }
    }

    @Test
    fun testParamBindingMatchingSimpleBind() {
        singleQueryMethod<ReadQueryFunction>(
            """
                @Query("SELECT uid from User where uid = :id")
                abstract public long getIdById(int id);
                """
        ) { parsedQuery, _ ->
            val section = parsedQuery.query.bindSections.first()
            val param = parsedQuery.parameters.firstOrNull()
            assertThat(section).isNotNull()
            assertThat(param).isNotNull()
            assertThat(parsedQuery.sectionToParamMapping).isEqualTo(listOf(Pair(section, param)))
        }
    }

    @Test
    fun testParamBindingTwoBindVarsIntoTheSameParameter() {
        singleQueryMethod<ReadQueryFunction>(
            """
                @Query("SELECT uid from User where uid = :id OR uid = :id")
                abstract public long getIdById(int id);
                """
        ) { parsedQuery, _ ->
            val section = parsedQuery.query.bindSections[0]
            val section2 = parsedQuery.query.bindSections[1]
            val param = parsedQuery.parameters.firstOrNull()
            assertThat(section).isNotNull()
            assertThat(section2).isNotNull()
            assertThat(param).isNotNull()
            assertThat(parsedQuery.sectionToParamMapping)
                .containsExactly(section to param, section2 to param)
        }
    }

    @Test
    fun testMissingParameterForBinding() {
        singleQueryMethod<ReadQueryFunction>(
            """
                @Query("SELECT uid from User where uid = :id OR uid = :uid")
                abstract public long getIdById(int id);
                """
        ) { parsedQuery, invocation ->
            val section = parsedQuery.query.bindSections[0]
            val section2 = parsedQuery.query.bindSections[1]
            val param = parsedQuery.parameters.firstOrNull()
            assertThat(section).isNotNull()
            assertThat(section2).isNotNull()
            assertThat(param).isNotNull()
            assertThat(parsedQuery.sectionToParamMapping)
                .containsExactly(section to param, section2 to null)
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.missingParameterForBindVariable(listOf(":uid")))
            }
        }
    }

    @Test
    fun test2MissingParameterForBinding() {
        singleQueryMethod<ReadQueryFunction>(
            """
                @Query("SELECT uid from User where name = :bar AND uid = :id OR uid = :uid")
                abstract public long getIdById(int id);
                """
        ) { parsedQuery, invocation ->
            val bar = parsedQuery.query.bindSections[0]
            val id = parsedQuery.query.bindSections[1]
            val uid = parsedQuery.query.bindSections[2]
            val param = parsedQuery.parameters.firstOrNull()
            assertThat(bar).isNotNull()
            assertThat(id).isNotNull()
            assertThat(uid).isNotNull()
            assertThat(param).isNotNull()
            assertThat(parsedQuery.sectionToParamMapping)
                .isEqualTo(listOf(Pair(bar, null), Pair(id, param), Pair(uid, null)))

            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.missingParameterForBindVariable(listOf(":bar", ":uid"))
                )
            }
        }
    }

    @Test
    fun testUnusedParameters() {
        singleQueryMethod<ReadQueryFunction>(
            """
                @Query("SELECT uid from User where name = :bar")
                abstract public long getIdById(int bar, int whyNotUseMe);
                """
        ) { parsedQuery, invocation ->
            val bar = parsedQuery.query.bindSections[0]
            val barParam = parsedQuery.parameters.firstOrNull()
            assertThat(bar).isNotNull()
            assertThat(barParam).isNotNull()
            assertThat(parsedQuery.sectionToParamMapping).containsExactly(bar to barParam)
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.unusedQueryFunctionParameter(listOf("whyNotUseMe"))
                )
            }
        }
    }

    @Test
    fun testNameWithUnderscore() {
        singleQueryMethod<ReadQueryFunction>(
            """
                @Query("select * from User where uid = :_blah")
                abstract public long getSth(int _blah);
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.QUERY_PARAMETERS_CANNOT_START_WITH_UNDERSCORE)
            }
        }
    }

    @Test
    fun testGenericReturnType() {
        singleQueryMethod<ReadQueryFunction>(
            """
                @Query("select * from User")
                abstract public <T> ${LIST.canonicalName}<T> foo(int x);
                """
        ) { parsedQuery, invocation ->
            val expected =
                MUTABLE_LIST.parametrizedBy(
                        XTypeName.getTypeVariableName(
                                name = "T",
                                bounds = listOf(XTypeName.ANY_OBJECT.copy(nullable = true))
                            )
                            .copy(nullable = true)
                    )
                    .copy(nullable = true)
            assertThat(parsedQuery.returnType.asTypeName()).isEqualTo(expected)
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.CANNOT_USE_UNBOUND_GENERICS_IN_QUERY_FUNCTIONS)
            }
        }
    }

    @Test
    fun testBadQuery() {
        singleQueryMethod<ReadQueryFunction>(
            """
                @Query("select * from :1 :2")
                abstract public long foo(int x);
                """
        ) { _, invocation ->
            // do nothing
            invocation.assertCompilationResult { hasErrorContaining("mismatched input ':'") }
        }
    }

    @Test
    fun testLiveDataWithWithClause() {
        singleQueryMethod<ReadQueryFunction>(
            """
                @Query("WITH RECURSIVE tempTable(n, fact) AS (SELECT 0, 1 UNION ALL SELECT n+1,"
                + " (n+1)*fact FROM tempTable WHERE n < 9) SELECT fact FROM tempTable, User")
                abstract public ${LifecyclesTypeNames.LIVE_DATA.canonicalName}<${LIST.canonicalName}<Integer>>
                getFactorialLiveData();
                """
        ) { parsedQuery, _ ->
            assertThat(parsedQuery.query.tables).contains(Table("User", "User"))
            assertThat(parsedQuery.query.tables).doesNotContain(Table("tempTable", "tempTable"))
            assertThat(parsedQuery.query.tables.size).isEqualTo(1)
        }
    }

    @Test
    fun testLiveDataWithNothingToObserve() {
        singleQueryMethod<ReadQueryFunction>(
            """
                @Query("SELECT 1")
                abstract public ${LifecyclesTypeNames.LIVE_DATA.canonicalName}<Integer> getOne();
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.OBSERVABLE_QUERY_NOTHING_TO_OBSERVE)
            }
        }
    }

    @Test
    fun testLiveDataWithWithClauseAndNothingToObserve() {
        singleQueryMethod<ReadQueryFunction>(
            """
                @Query("WITH RECURSIVE tempTable(n, fact) AS (SELECT 0, 1 UNION ALL SELECT n+1,"
                + " (n+1)*fact FROM tempTable WHERE n < 9) SELECT fact FROM tempTable")
                abstract public ${LifecyclesTypeNames.LIVE_DATA.canonicalName}<${LIST.canonicalName}<Integer>>
                getFactorialLiveData();
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.OBSERVABLE_QUERY_NOTHING_TO_OBSERVE)
            }
        }
    }

    @Test
    fun testBoundGeneric() {
        singleQueryMethod<ReadQueryFunction>(
            """
                static abstract class BaseModel<T> {
                    @Query("select COUNT(*) from User")
                    abstract public T getT();
                }
                @Dao
                static abstract class ExtendingModel extends BaseModel<Integer> {
                }
                """
        ) { parsedQuery, _ ->
            assertThat(parsedQuery.returnType.asTypeName())
                .isEqualTo(XTypeName.BOXED_INT.copy(nullable = true))
        }
    }

    @Test
    fun testBoundGenericParameter() {
        singleQueryMethod<ReadQueryFunction>(
            """
                static abstract class BaseModel<T> {
                    @Query("select COUNT(*) from User where :t")
                    abstract public int getT(T t);
                }
                @Dao
                static abstract class ExtendingModel extends BaseModel<Integer> {
                }
                """
        ) { parsedQuery, _ ->
            assertThat(parsedQuery.parameters.first().type.asTypeName())
                .isEqualTo(XTypeName.BOXED_INT.copy(nullable = true))
        }
    }

    @Test
    fun testReadDeleteWithBadReturnType() {
        singleQueryMethod<WriteQueryFunction>(
            """
                @Query("DELETE from User where uid = :id")
                abstract public float foo(int id);
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.cannotFindPreparedQueryResultAdapter("float", QueryType.DELETE)
                )
            }
        }
    }

    @Test
    fun testSimpleDelete() {
        singleQueryMethod<WriteQueryFunction>(
            """
                @Query("DELETE from User where uid = :id")
                abstract public int foo(int id);
                """
        ) { parsedQuery, _ ->
            assertThat(parsedQuery.element.jvmName).isEqualTo("foo")
            assertThat(parsedQuery.parameters.size).isEqualTo(1)
            assertThat(parsedQuery.returnType.asTypeName()).isEqualTo(XTypeName.PRIMITIVE_INT)
        }
    }

    @Test
    fun testVoidDeleteQuery() {
        singleQueryMethod<WriteQueryFunction>(
            """
                @Query("DELETE from User where uid = :id")
                abstract public void foo(int id);
                """
        ) { parsedQuery, _ ->
            assertThat(parsedQuery.element.jvmName).isEqualTo("foo")
            assertThat(parsedQuery.parameters.size).isEqualTo(1)
            assertThat(parsedQuery.returnType.asTypeName()).isEqualTo(XTypeName.UNIT_VOID)
        }
    }

    @Test
    fun testVoidUpdateQuery() {
        singleQueryMethod<WriteQueryFunction>(
            """
                @Query("update user set name = :name")
                abstract public void updateAllNames(String name);
                """
        ) { parsedQuery, _ ->
            assertThat(parsedQuery.element.jvmName).isEqualTo("updateAllNames")
            assertThat(parsedQuery.parameters.size).isEqualTo(1)
            assertThat(parsedQuery.returnType.asTypeName()).isEqualTo(XTypeName.UNIT_VOID)
            assertThat(parsedQuery.parameters.first().type.asTypeName())
                .isEqualTo(STRING.copy(nullable = true))
        }
    }

    @Test
    fun testVoidInsertQuery() {
        singleQueryMethod<WriteQueryFunction>(
            """
                @Query("insert into user (name) values (:name)")
                abstract public void insertUsername(String name);
                """
        ) { parsedQuery, _ ->
            assertThat(parsedQuery.element.jvmName).isEqualTo("insertUsername")
            assertThat(parsedQuery.parameters.size).isEqualTo(1)
            assertThat(parsedQuery.returnType.asTypeName()).isEqualTo(XTypeName.UNIT_VOID)
            assertThat(parsedQuery.parameters.first().type.asTypeName())
                .isEqualTo(STRING.copy(nullable = true))
        }
    }

    @Test
    fun testLongInsertQuery() {
        singleQueryMethod<WriteQueryFunction>(
            """
                @Query("insert into user (name) values (:name)")
                abstract public long insertUsername(String name);
                """
        ) { parsedQuery, _ ->
            assertThat(parsedQuery.element.jvmName).isEqualTo("insertUsername")
            assertThat(parsedQuery.parameters.size).isEqualTo(1)
            assertThat(parsedQuery.returnType.asTypeName()).isEqualTo(XTypeName.PRIMITIVE_LONG)
            assertThat(parsedQuery.parameters.first().type.asTypeName())
                .isEqualTo(STRING.copy(nullable = true))
        }
    }

    @Test
    fun testInsertQueryWithBadReturnType() {
        singleQueryMethod<WriteQueryFunction>(
            """
                @Query("insert into user (name) values (:name)")
                abstract public int insert(String name);
                """
        ) { parsedQuery, invocation ->
            assertThat(parsedQuery.returnType.asTypeName()).isEqualTo(XTypeName.PRIMITIVE_INT)
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.cannotFindPreparedQueryResultAdapter("int", QueryType.INSERT)
                )
            }
        }
    }

    @Test
    fun testLiveDataQuery() {
        singleQueryMethod<ReadQueryFunction>(
            """
                @Query("select name from user where uid = :id")
                abstract ${LifecyclesTypeNames.LIVE_DATA.canonicalName}<String> nameLiveData(String id);
                """
        ) { parsedQuery, _ ->
            assertThat(parsedQuery.returnType.asTypeName())
                .isEqualTo(
                    LifecyclesTypeNames.LIVE_DATA.parametrizedBy(STRING.copy(nullable = true))
                        .copy(nullable = true)
                )

            assertThat(parsedQuery.queryResultBinder).isInstanceOf<LiveDataQueryResultBinder>()
        }
    }

    @Test
    fun testBadReturnForDeleteQuery() {
        singleQueryMethod<WriteQueryFunction>(
            """
                @Query("delete from user where uid = :id")
                abstract ${LifecyclesTypeNames.LIVE_DATA.canonicalName}<Integer> deleteLiveData(String id);
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.cannotFindPreparedQueryResultAdapter(
                        "androidx.lifecycle.LiveData<java.lang.Integer>",
                        QueryType.DELETE
                    )
                )
            }
        }
    }

    @Test
    fun testBadReturnForUpdateQuery() {
        singleQueryMethod<WriteQueryFunction>(
            """
                @Query("update user set name = :name")
                abstract ${LifecyclesTypeNames.LIVE_DATA.canonicalName}<Integer> updateNameLiveData(String name);
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.cannotFindPreparedQueryResultAdapter(
                        "androidx.lifecycle.LiveData<java.lang.Integer>",
                        QueryType.UPDATE
                    )
                )
            }
        }
    }

    @Test
    fun testDataSourceFactoryQuery() {
        singleQueryMethod<ReadQueryFunction>(
            """
                @Query("select name from user")
                abstract ${PagingTypeNames.DATA_SOURCE_FACTORY.canonicalName}<Integer, String>
                nameDataSourceFactory();
                """
        ) { parsedQuery, _ ->
            assertThat(parsedQuery.returnType.asTypeName())
                .isEqualTo(
                    PagingTypeNames.DATA_SOURCE_FACTORY.parametrizedBy(
                            XTypeName.BOXED_INT.copy(nullable = true),
                            STRING.copy(nullable = true),
                        )
                        .copy(nullable = true)
                )

            assertThat(parsedQuery.queryResultBinder)
                .isInstanceOf<DataSourceFactoryQueryResultBinder>()
            val tableNames =
                (parsedQuery.queryResultBinder as DataSourceFactoryQueryResultBinder)
                    .positionalDataSourceQueryResultBinder
                    .tableNames
            assertThat(tableNames).containsExactly("user")
        }
    }

    @Test
    fun testMultiTableDataSourceFactoryQuery() {
        singleQueryMethod<ReadQueryFunction>(
            """
                @Query("select name from User u LEFT OUTER JOIN Book b ON u.uid == b.uid")
                abstract ${PagingTypeNames.DATA_SOURCE_FACTORY.canonicalName}<Integer, String>
                nameDataSourceFactory();
                """
        ) { parsedQuery, _ ->
            assertThat(parsedQuery.returnType.asTypeName())
                .isEqualTo(
                    PagingTypeNames.DATA_SOURCE_FACTORY.parametrizedBy(
                            XTypeName.BOXED_INT.copy(nullable = true),
                            STRING.copy(nullable = true),
                        )
                        .copy(nullable = true),
                )
            assertThat(parsedQuery.queryResultBinder)
                .isInstanceOf<DataSourceFactoryQueryResultBinder>()
            val tableNames =
                (parsedQuery.queryResultBinder as DataSourceFactoryQueryResultBinder)
                    .positionalDataSourceQueryResultBinder
                    .tableNames
            assertThat(tableNames).containsExactly("User", "Book")
        }
    }

    @Test
    fun testBadChannelReturnForQuery() {
        singleQueryMethod<QueryFunction>(
            """
                @Query("select * from user")
                abstract ${KotlinTypeNames.CHANNEL.canonicalName}<User> getUsersChannel();
                """,
            additionalSources = listOf(COMMON.CHANNEL)
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.invalidChannelType(KotlinTypeNames.CHANNEL.canonicalName)
                )
            }
        }
    }

    @Test
    fun testBadSendChannelReturnForQuery() {
        singleQueryMethod<QueryFunction>(
            """
                @Query("select * from user")
                abstract ${KotlinTypeNames.SEND_CHANNEL.canonicalName}<User> getUsersChannel();
                """,
            additionalSources = listOf(COMMON.SEND_CHANNEL)
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.invalidChannelType(KotlinTypeNames.SEND_CHANNEL.canonicalName)
                )
            }
        }
    }

    @Test
    fun testBadReceiveChannelReturnForQuery() {
        singleQueryMethod<QueryFunction>(
            """
                @Query("select * from user")
                abstract ${KotlinTypeNames.RECEIVE_CHANNEL.canonicalName}<User> getUsersChannel();
                """,
            additionalSources = listOf(COMMON.RECEIVE_CHANNEL)
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.invalidChannelType(
                        KotlinTypeNames.RECEIVE_CHANNEL.toString(CodeLanguage.JAVA)
                    )
                )
            }
        }
    }

    @Test
    fun query_detectTransaction_select() {
        singleQueryMethod<ReadQueryFunction>(
            """
                @Query("select * from user")
                abstract int loadUsers();
                """
        ) { method, _ ->
            assertThat(method.inTransaction).isEqualTo(false)
        }
    }

    @Test
    fun query_detectTransaction_selectInTransaction() {
        singleQueryMethod<ReadQueryFunction>(
            """
                @Transaction
                @Query("select * from user")
                abstract int loadUsers();
                """
        ) { method, _ ->
            assertThat(method.inTransaction).isEqualTo(true)
        }
    }

    @Test
    fun skipVerification() {
        singleQueryMethod<ReadQueryFunction>(
            """
                @SkipQueryVerification
                @Query("SELECT foo from User")
                abstract public int[] foo();
                """
        ) { parsedQuery, _ ->
            assertThat(parsedQuery.element.jvmName).isEqualTo("foo")
            assertThat(parsedQuery.parameters.size).isEqualTo(0)
            assertThat(parsedQuery.returnType.asTypeName())
                .isEqualTo(XTypeName.getArrayName(XTypeName.PRIMITIVE_INT).copy(nullable = true))
        }
    }

    @Test
    fun skipVerificationDataClass() {
        singleQueryMethod<ReadQueryFunction>(
            """
                @SkipQueryVerification
                @Query("SELECT bookId, uid  FROM User")
                abstract NotAnEntity getDataClass();
                """
        ) { parsedQuery, _ ->
            assertThat(parsedQuery.element.jvmName).isEqualTo("getDataClass")
            assertThat(parsedQuery.parameters.size).isEqualTo(0)
            assertThat(parsedQuery.returnType.asTypeName())
                .isEqualTo(COMMON.NOT_AN_ENTITY_TYPE_NAME.copy(nullable = true))

            val adapter = parsedQuery.queryResultBinder.adapter
            assertThat(adapter).isNotNull()
            assertThat(adapter).isInstanceOf<SingleItemQueryResultAdapter>()
            val rowAdapter = adapter!!.rowAdapters.single()
            assertThat(rowAdapter).isInstanceOf<DataClassRowAdapter>()
        }
    }

    @Test
    fun suppressWarnings() {
        singleQueryMethod<ReadQueryFunction>(
            """
                @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
                @Query("SELECT uid from User")
                abstract public int[] foo();
                """
        ) { method, invocation ->
            assertThat(
                    QueryFunctionProcessor(
                            baseContext = invocation.context,
                            containing = Mockito.mock(XType::class.java),
                            executableElement = method.element,
                            dbVerifier = null
                        )
                        .context
                        .logger
                        .suppressedWarnings
                )
                .isEqualTo(setOf(Warning.QUERY_MISMATCH))
        }
    }

    @Test
    fun relationWithExtendsBounds() {
        if (!enableVerification) {
            return
        }
        singleQueryMethod<ReadQueryFunction>(
            """
                static class Merged extends User {
                   @Relation(parentColumn = "name", entityColumn = "lastName",
                             entity = User.class)
                   java.util.List<? extends User> users;
                }
                @Transaction
                @Query("select * from user")
                abstract java.util.List<Merged> loadUsers();
            """
        ) { method, invocation ->
            assertThat(method.queryResultBinder.adapter).isInstanceOf<ListQueryResultAdapter>()
            val listAdapter = method.queryResultBinder.adapter as ListQueryResultAdapter
            assertThat(listAdapter.rowAdapters.single()).isInstanceOf<DataClassRowAdapter>()
            val dataClassRowAdapter = listAdapter.rowAdapters.single() as DataClassRowAdapter
            assertThat(dataClassRowAdapter.relationCollectors.size).isEqualTo(1)
            assertThat(dataClassRowAdapter.relationCollectors[0].relationTypeName)
                .isEqualTo(
                    CommonTypeNames.ARRAY_LIST.parametrizedBy(
                        COMMON.USER_TYPE_NAME.copy(nullable = true)
                    )
                )
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    @Test
    fun dataClass_renamedColumn() {
        dataClassTest(
            """
                String name;
                String lName;
                """,
            listOf("name", "lastName as lName")
        ) { adapter, _, invocation ->
            assertThat(adapter?.mapping?.unusedColumns).isEmpty()
            assertThat(adapter?.mapping?.unusedFields).isEmpty()
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    @Test
    fun dataClass_exactMatch() {
        dataClassTest(
            """
                String name;
                String lastName;
                """,
            listOf("name", "lastName")
        ) { adapter, _, invocation ->
            assertThat(adapter?.mapping?.unusedColumns).isEmpty()
            assertThat(adapter?.mapping?.unusedFields).isEmpty()
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    @Test
    fun dataClass_exactMatchWithStar() {
        dataClassTest(
            """
            String name;
            String lastName;
            int uid;
            @ColumnInfo(name = "ageColumn")
            int age;
        """,
            listOf("*")
        ) { adapter, _, invocation ->
            assertThat(adapter?.mapping?.unusedColumns).isEmpty()
            assertThat(adapter?.mapping?.unusedFields).isEmpty()
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    @Test
    fun primitive_removeUnusedColumns() {
        if (!enableVerification) {
            throw AssumptionViolatedException("nothing to test w/o db verification")
        }
        singleQueryMethod<ReadQueryFunction>(
            """
                @RewriteQueriesToDropUnusedColumns
                @Query("select 1 from user")
                abstract int getOne();
                """
        ) { method, invocation ->
            val adapter = method.queryResultBinder.adapter?.rowAdapters?.single()
            check(adapter is SingleColumnRowAdapter)
            assertThat(method.query.original).isEqualTo("select 1 from user")
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    @Test
    fun dataClass_removeUnusedColumns() {
        if (!enableVerification) {
            throw AssumptionViolatedException("nothing to test w/o db verification")
        }
        singleQueryMethod<ReadQueryFunction>(
            """
                public static class DataClass {
                    public String name;
                    public String lastName;
                }
                @RewriteQueriesToDropUnusedColumns
                @Query("select * from user LIMIT 1")
                abstract DataClass loadUsers();
                """
        ) { method, invocation ->
            val adapter = method.queryResultBinder.adapter?.rowAdapters?.single()
            check(adapter is DataClassRowAdapter)
            assertThat(method.query.original)
                .isEqualTo("SELECT `name`, `lastName` FROM (select * from user LIMIT 1)")
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    @Test
    fun dataClass_multimapQuery_removeUnusedColumns() {
        if (!enableVerification) {
            throw AssumptionViolatedException("nothing to test w/o db verification")
        }
        val relatingEntity =
            Source.java(
                "foo.bar.Relation",
                """
            package foo.bar;
            import androidx.room.*;
            @Entity
            public class Relation {
              @PrimaryKey
              long relationId;
              long userId;
            }
            """
                    .trimIndent()
            )
        singleQueryMethod<ReadQueryFunction>(
            """
                public static class Username {
                    public String name;
                    @Override
                    public boolean equals(Object o) {
                        if (this == o) return true;
                        if (o == null || getClass() != o.getClass()) return false;
                        Username username = (Username) o;
                        if (name != username.name) return false;
                        return true;
                    }
                    @Override
                    public int hashCode() {
                        return name.hashCode();
                    }
                }
                @RewriteQueriesToDropUnusedColumns
                @Query("SELECT * FROM User JOIN Relation ON (User.uid = Relation.userId)")
                abstract Map<Username, List<Relation>> loadUserRelations();
                """,
            additionalSources = listOf(relatingEntity)
        ) { method, invocation ->
            assertThat(method.query.original)
                .isEqualTo(
                    "SELECT `name`, `relationId`, `userId` FROM " +
                        "(SELECT * FROM User JOIN Relation ON (User.uid = Relation.userId))"
                )
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    @Test
    fun dataClass_dontRemoveUnusedColumnsWhenColumnNamesConflict() {
        if (!enableVerification) {
            throw AssumptionViolatedException("nothing to test w/o db verification")
        }
        singleQueryMethod<ReadQueryFunction>(
            """
                public static class DataClass {
                    public String name;
                    public String lastName;
                }
                @RewriteQueriesToDropUnusedColumns
                @Query("select * from user u, user u2 LIMIT 1")
                abstract DataClass loadUsers();
                """
        ) { method, invocation ->
            val adapter = method.queryResultBinder.adapter?.rowAdapters?.single()
            check(adapter is DataClassRowAdapter)
            assertThat(method.query.original).isEqualTo("select * from user u, user u2 LIMIT 1")
            invocation.assertCompilationResult {
                hasWarningContaining("The query returns some columns [uid")
            }
        }
    }

    @Test
    fun dataClass_nonJavaName() {
        dataClassTest(
            """
            @ColumnInfo(name = "MAX(ageColumn)")
            int maxAge;
            String name;
            """,
            listOf("MAX(ageColumn)", "name")
        ) { adapter, _, invocation ->
            assertThat(adapter?.mapping?.unusedColumns).isEmpty()
            assertThat(adapter?.mapping?.unusedFields).isEmpty()
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    @Test
    fun dataClass_noMatchingFields() {
        dataClassTest(
            """
                String nameX;
                String lastNameX;
                """,
            listOf("name", "lastName")
        ) { adapter, _, invocation ->
            assertThat(adapter?.mapping?.unusedColumns).containsExactly("name", "lastName")
            assertThat(adapter?.mapping?.unusedFields)
                .containsExactlyElementsIn(adapter?.dataClass?.properties)
            invocation.assertCompilationResult {
                hasErrorContaining(
                    cannotFindQueryResultAdapter(
                        XClassName.get("foo.bar", "MyClass", "DataClass").canonicalName
                    )
                )
                hasWarningContaining(
                    ProcessorErrors.queryPropertyDataClassMismatch(
                        dataClassTypeNames = listOf(DATA_CLASS.canonicalName),
                        unusedColumns = listOf("name", "lastName"),
                        dataClassUnusedProperties =
                            mapOf(
                                DATA_CLASS.canonicalName to
                                    listOf(createField("nameX"), createField("lastNameX"))
                            ),
                        allColumns = listOf("name", "lastName"),
                    )
                )
            }
        }
    }

    @Test
    fun dataClass_badQuery() {
        // do not report mismatch if query is broken
        dataClassTest(
            """
            @ColumnInfo(name = "MAX(ageColumn)")
            int maxAge;
            String name;
            """,
            listOf("MAX(age)", "name")
        ) { _, _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining("no such column: age")
                hasErrorContaining(
                    cannotFindQueryResultAdapter(
                        XClassName.get("foo.bar", "MyClass", "DataClass").canonicalName
                    )
                )
                hasErrorCount(2)
                hasNoWarnings()
            }
        }
    }

    @Test
    fun dataClass_tooManyColumns() {
        dataClassTest(
            """
            String name;
            String lastName;
            """,
            listOf("uid", "name", "lastName")
        ) { adapter, _, invocation ->
            assertThat(adapter?.mapping?.unusedColumns).containsExactly("uid")
            assertThat(adapter?.mapping?.unusedFields).isEmpty()
            invocation.assertCompilationResult {
                hasWarningContaining(
                    ProcessorErrors.queryPropertyDataClassMismatch(
                        dataClassTypeNames = listOf(DATA_CLASS.canonicalName),
                        unusedColumns = listOf("uid"),
                        dataClassUnusedProperties = emptyMap(),
                        allColumns = listOf("uid", "name", "lastName"),
                    )
                )
            }
        }
    }

    @Test
    fun dataClass_tooManyFields() {
        dataClassTest(
            """
            String name;
            String lastName;
            """,
            listOf("lastName")
        ) { adapter, _, invocation ->
            assertThat(adapter?.mapping?.unusedColumns).isEmpty()
            assertThat(adapter?.mapping?.unusedFields)
                .containsExactlyElementsIn(
                    adapter?.dataClass?.properties?.filter { it.name == "name" }
                )

            invocation.assertCompilationResult {
                hasWarningContaining(
                    ProcessorErrors.queryPropertyDataClassMismatch(
                        dataClassTypeNames = listOf(DATA_CLASS.canonicalName),
                        unusedColumns = emptyList(),
                        allColumns = listOf("lastName"),
                        dataClassUnusedProperties =
                            mapOf(DATA_CLASS.canonicalName to listOf(createField("name"))),
                    )
                )
            }
        }
    }

    @Test
    fun dataClass_missingNonNull() {
        dataClassTest(
            """
            @NonNull
            String name;
            String lastName;
            """,
            listOf("lastName")
        ) { adapter, _, invocation ->
            assertThat(adapter?.mapping?.unusedColumns).isEmpty()
            assertThat(adapter?.mapping?.unusedFields)
                .containsExactlyElementsIn(
                    adapter?.dataClass?.properties?.filter { it.name == "name" }
                )

            invocation.assertCompilationResult {
                hasWarningContaining(
                    ProcessorErrors.queryPropertyDataClassMismatch(
                        dataClassTypeNames = listOf(DATA_CLASS.canonicalName),
                        unusedColumns = emptyList(),
                        dataClassUnusedProperties =
                            mapOf(DATA_CLASS.canonicalName to listOf(createField("name"))),
                        allColumns = listOf("lastName"),
                    )
                )
                hasErrorContaining(
                    ProcessorErrors.dataClassMissingNonNull(
                        dataClassTypeName = DATA_CLASS.canonicalName,
                        missingDataClassProperties = listOf("name"),
                        allQueryColumns = listOf("lastName")
                    )
                )
            }
        }
    }

    @Test
    fun dataClass_tooManyFieldsAndColumns() {
        dataClassTest(
            """
            String name;
            String lastName;
            """,
            listOf("uid", "name")
        ) { adapter, _, invocation ->
            assertThat(adapter?.mapping?.unusedColumns).containsExactly("uid")
            assertThat(adapter?.mapping?.unusedFields)
                .containsExactlyElementsIn(
                    adapter?.dataClass?.properties?.filter { it.name == "lastName" }
                )
            invocation.assertCompilationResult {
                hasWarningContaining(
                    ProcessorErrors.queryPropertyDataClassMismatch(
                        dataClassTypeNames = listOf(DATA_CLASS.canonicalName),
                        unusedColumns = listOf("uid"),
                        allColumns = listOf("uid", "name"),
                        dataClassUnusedProperties =
                            mapOf(DATA_CLASS.canonicalName to listOf(createField("lastName")))
                    )
                )
            }
        }
    }

    @Test
    fun dataClass_expandProjection() {
        if (!enableVerification) return
        dataClassTest(
            """
                String uid;
                String name;
            """,
            listOf("*"),
            options = mapOf("room.expandProjection" to "true")
        ) { adapter, _, invocation ->
            adapter!!
            assertThat(adapter.mapping.unusedColumns).isEmpty()
            assertThat(adapter.mapping.unusedFields).isEmpty()
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    private fun dataClassTest(
        dataClassFields: String,
        queryColumns: List<String>,
        options: Map<String, String> = emptyMap(),
        handler: (DataClassRowAdapter?, QueryFunction, XTestInvocation) -> Unit
    ) {
        singleQueryMethod<ReadQueryFunction>(
            """
                static class DataClass {
                    $dataClassFields
                }
                @Query("SELECT ${queryColumns.joinToString(", ")} from User LIMIT 1")
                abstract MyClass.DataClass getNameAndLastNames();
            """,
            options = options
        ) { parsedQuery, invocation ->
            val adapter = parsedQuery.queryResultBinder.adapter
            if (enableVerification) {
                if (adapter is SingleItemQueryResultAdapter) {
                    handler(
                        adapter.rowAdapters.single() as? DataClassRowAdapter,
                        parsedQuery,
                        invocation
                    )
                } else {
                    handler(null, parsedQuery, invocation)
                }
            } else {
                assertThat(adapter).isNotNull()
            }
        }
    }

    private fun <T : QueryFunction> singleQueryMethod(
        vararg input: String,
        additionalSources: Iterable<Source> = emptyList(),
        options: Map<String, String> = emptyMap(),
        handler: (T, XTestInvocation) -> Unit
    ) {
        val inputSource =
            Source.java("foo.bar.MyClass", DAO_PREFIX + input.joinToString("\n") + DAO_SUFFIX)
        val commonSources =
            listOf(
                COMMON.LIVE_DATA,
                COMMON.COMPUTABLE_LIVE_DATA,
                COMMON.USER,
                COMMON.BOOK,
                COMMON.PAGE,
                COMMON.NOT_AN_ENTITY,
                COMMON.ARTIST,
                COMMON.SONG,
                COMMON.IMAGE,
                COMMON.IMAGE_FORMAT,
                COMMON.CONVERTER
            )
        val allOptions =
            mapOf(Context.BooleanProcessorOptions.GENERATE_KOTLIN.argName to "false") + options
        runProcessorTest(
            sources = additionalSources + commonSources + inputSource,
            options = allOptions
        ) { invocation ->
            val (owner, methods) =
                invocation.roundEnv
                    .getElementsAnnotatedWith(Dao::class.qualifiedName!!)
                    .filterIsInstance<XTypeElement>()
                    .map { typeElement ->
                        Pair(
                            typeElement,
                            typeElement
                                .getAllMethods()
                                .filter { method -> method.hasAnnotation(Query::class) }
                                .toList()
                        )
                    }
                    .first { it.second.isNotEmpty() }
            val verifier =
                if (enableVerification) {
                    createVerifierFromEntitiesAndViews(invocation)
                        .also(invocation.context::attachDatabaseVerifier)
                } else {
                    null
                }
            val parser =
                QueryFunctionProcessor(
                    baseContext = invocation.context,
                    containing = owner.type,
                    executableElement = methods.first(),
                    dbVerifier = verifier
                )
            val parsedQuery = parser.process()
            @Suppress("UNCHECKED_CAST") handler(parsedQuery as T, invocation)
        }
    }

    private fun <T : QueryFunction> singleQueryFunction(
        vararg input: String,
        additionalSources: Iterable<Source> = emptyList(),
        options: Map<String, String> = emptyMap(),
        handler: (T, XTestInvocation) -> Unit
    ) {
        val inputSource =
            Source.kotlin("MyClass.kt", DAO_PREFIX_KT + input.joinToString("\n") + DAO_SUFFIX)
        val commonSources =
            listOf(
                COMMON.USER,
                COMMON.BOOK,
                COMMON.NOT_AN_ENTITY,
                COMMON.RX2_COMPLETABLE,
                COMMON.RX2_MAYBE,
                COMMON.RX2_SINGLE,
                COMMON.RX2_FLOWABLE,
                COMMON.RX2_OBSERVABLE,
                COMMON.RX3_COMPLETABLE,
                COMMON.RX3_MAYBE,
                COMMON.RX3_SINGLE,
                COMMON.RX3_FLOWABLE,
                COMMON.RX3_OBSERVABLE,
                COMMON.LISTENABLE_FUTURE,
                COMMON.LIVE_DATA,
                COMMON.COMPUTABLE_LIVE_DATA,
                COMMON.PUBLISHER,
                COMMON.FLOW,
                COMMON.GUAVA_ROOM,
                COMMON.RX2_ROOM,
                COMMON.RX2_EMPTY_RESULT_SET_EXCEPTION
            )

        runProcessorTest(
            sources = additionalSources + commonSources + inputSource,
            options = options
        ) { invocation ->
            val (owner, methods) =
                invocation.roundEnv
                    .getElementsAnnotatedWith(Dao::class.qualifiedName!!)
                    .filterIsInstance<XTypeElement>()
                    .map { typeElement ->
                        Pair(
                            typeElement,
                            typeElement
                                .getAllMethods()
                                .filter { method -> method.hasAnnotation(Query::class) }
                                .toList()
                        )
                    }
                    .first { it.second.isNotEmpty() }
            val verifier =
                if (enableVerification) {
                    createVerifierFromEntitiesAndViews(invocation)
                        .also(invocation.context::attachDatabaseVerifier)
                } else {
                    null
                }
            val parser =
                QueryFunctionProcessor(
                    baseContext = invocation.context,
                    containing = owner.type,
                    executableElement = methods.first(),
                    dbVerifier = verifier
                )
            val parsedQuery = parser.process()
            @Suppress("UNCHECKED_CAST") handler(parsedQuery as T, invocation)
        }
    }

    @Test
    fun testInvalidLinkedListCollectionInMultimapJoin() {
        singleQueryMethod<ReadQueryFunction>(
            """
                @Query("select * from User u JOIN Book b ON u.uid == b.uid")
                abstract Map<User, LinkedList<Book>> getInvalidCollectionMultimap();
            """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorCount(2)
                hasErrorContaining("Multimap 'value' collection type must be a List, Set or Map.")
                hasErrorContaining(
                    "Not sure how to convert the query result to this function's return type"
                )
            }
        }
    }

    @Test
    fun testInvalidGenericMultimapJoin() {
        singleQueryMethod<ReadQueryFunction>(
            """
                @Query("select * from User u JOIN Book b ON u.uid == b.uid")
                abstract com.google.common.collect.ImmutableMultimap<User, Book>
                getInvalidCollectionMultimap();
            """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorCount(2)
                hasErrorContaining(DO_NOT_USE_GENERIC_IMMUTABLE_MULTIMAP)
                hasErrorContaining(
                    "Not sure how to convert the query result to this function's return type"
                )
            }
        }
    }

    @Test
    fun testUseMapInfoWithBothEmptyColumnsProvided() {
        if (!enableVerification) {
            return
        }
        singleQueryMethod<ReadQueryFunction>(
            """
                @MapInfo
                @Query("select * from User u JOIN Book b ON u.uid == b.uid")
                abstract Map<User, Book> getMultimap();
            """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorCount(1)
                hasErrorContaining(MAP_INFO_MUST_HAVE_AT_LEAST_ONE_COLUMN_PROVIDED)
            }
        }
    }

    @Test
    fun testUseMapInfoWithTableAndColumnName() {
        if (!enableVerification) {
            return
        }
        singleQueryMethod<ReadQueryFunction>(
            """
                @SuppressWarnings(
                    {RoomWarnings.QUERY_MISMATCH, RoomWarnings.AMBIGUOUS_COLUMN_IN_RESULT}
                )
                @MapInfo(keyColumn = "uid", keyTable = "u")
                @Query("SELECT * FROM User u JOIN Book b ON u.uid == b.uid")
                abstract Map<Integer, Book> getMultimap();
            """
        ) { _, invocation ->
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    @Test
    fun testUseMapInfoWithOriginalTableAndColumnName() {
        if (!enableVerification) {
            return
        }
        singleQueryMethod<ReadQueryFunction>(
            """
                @SuppressWarnings(
                    {RoomWarnings.QUERY_MISMATCH, RoomWarnings.AMBIGUOUS_COLUMN_IN_RESULT}
                )
                @MapInfo(keyColumn = "uid", keyTable = "User")
                @Query("SELECT * FROM User u JOIN Book b ON u.uid == b.uid")
                abstract Map<Integer, Book> getMultimap();
            """
        ) { _, invocation ->
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    @Test
    fun testUseMapInfoWithColumnAlias() {
        if (!enableVerification) {
            return
        }
        singleQueryMethod<ReadQueryFunction>(
            """
                @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
                @MapInfo(keyColumn = "name", valueColumn = "bookCount")
                @Query("SELECT name, (SELECT count(*) FROM User u JOIN Book b ON u.uid == b.uid) "
                    + "AS bookCount FROM User")
                abstract Map<String, Integer> getMultimap();
            """
        ) { _, invocation ->
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    @Test
    fun testUseMapColumnWithColumnName() {
        if (!enableVerification) {
            return
        }
        singleQueryMethod<ReadQueryFunction>(
            """
                @SuppressWarnings(
                    {RoomWarnings.QUERY_MISMATCH, RoomWarnings.AMBIGUOUS_COLUMN_IN_RESULT}
                )
                @Query("SELECT * FROM User u JOIN Book b ON u.uid == b.uid")
                abstract Map<@MapColumn(columnName = "uid") Integer, Book> getMultimap();
            """
        ) { _, invocation ->
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    @Test
    fun testUseMapColumnWithColumnNameWrongTableName() {
        if (!enableVerification) {
            return
        }
        singleQueryMethod<ReadQueryFunction>(
            """
                @SuppressWarnings(
                    {RoomWarnings.QUERY_MISMATCH, RoomWarnings.AMBIGUOUS_COLUMN_IN_RESULT}
                )
                @Query("SELECT * FROM User u JOIN Book b ON u.uid == b.uid")
                abstract Map<@MapColumn(columnName = "uid", tableName = "NoName") Integer, Book> getMultimap();
            """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    "Column specified in the provided @MapColumn " +
                        "annotation must be present in the query."
                )
            }
        }
    }

    @Test
    fun testUseNestedMapColumnWithColumnName() {
        if (!enableVerification) {
            return
        }
        singleQueryMethod<ReadQueryFunction>(
            """
                @SuppressWarnings(
                    {RoomWarnings.QUERY_MISMATCH, RoomWarnings.AMBIGUOUS_COLUMN_IN_RESULT}
                )
                @Query("SELECT * FROM User u JOIN Book b ON u.uid == b.uid JOIN Page on b.uid == pBid")
                abstract Map<@MapColumn(columnName = "uid") Integer, Map<Book, @MapColumn(columnName = "pBid") Integer>> getMultimap();
            """
        ) { _, invocation ->
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    @Test
    fun testUseNestedMapColumnWithNestedKeyColumnName() {
        if (!enableVerification) {
            return
        }
        singleQueryMethod<ReadQueryFunction>(
            """
                @SuppressWarnings(
                    {RoomWarnings.QUERY_MISMATCH, RoomWarnings.AMBIGUOUS_COLUMN_IN_RESULT}
                )
                @Query("SELECT * FROM User u JOIN Book b ON u.uid == b.uid JOIN Page on b.uid == pBid")
                abstract Map<@MapColumn(columnName = "uid") Integer, Map<@MapColumn(columnName = "bookId") Integer, @MapColumn(columnName = "pBid") Integer>> getMultimap();
            """
        ) { _, invocation ->
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    @Test
    fun testUseMapColumnWithColumnAlias() {
        if (!enableVerification) {
            return
        }
        singleQueryMethod<ReadQueryFunction>(
            """
                @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
                @Query("SELECT name, (SELECT count(*) FROM User u JOIN Book b ON u.uid == b.uid) "
                    + "AS bookCount FROM User")
                abstract Map<@MapColumn(columnName = "name") String, @MapColumn(columnName = "bookCount") Integer> getMultimap();
            """
        ) { _, invocation ->
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    @Test
    fun testCannotHaveMapInfoAndMapColumn() {
        if (!enableVerification) {
            return
        }
        singleQueryMethod<ReadQueryFunction>(
            """
                @SuppressWarnings(
                    {RoomWarnings.QUERY_MISMATCH, RoomWarnings.AMBIGUOUS_COLUMN_IN_RESULT}
                )
                @MapInfo(keyColumn = "uid", keyTable = "u")
                @Query("SELECT * FROM User u JOIN Book b ON u.uid == b.uid")
                abstract Map<@MapColumn(columnName = "uid") Integer, Book> getMultimap();
            """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(CANNOT_USE_MAP_COLUMN_AND_MAP_INFO_SIMULTANEOUSLY)
            }
        }
    }

    @Test
    fun testDoesNotImplementEqualsAndHashcodeQuery() {
        singleQueryMethod<ReadQueryFunction>(
            """
                @Query("select * from User u JOIN Book b ON u.uid == b.uid")
                abstract Map<User, Book> getMultimap();
            """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasWarningCount(1)
                hasWarningContaining(
                    ProcessorErrors.classMustImplementEqualsAndHashCode("foo.bar.User")
                )
            }
        }
    }

    @Test
    fun testMissingMapInfoOneToOneString() {
        singleQueryMethod<ReadQueryFunction>(
            """
                @Query("select * from Artist JOIN Song ON Artist.mArtistName == Song.mArtist")
                abstract Map<Artist, String> getAllArtistsWithAlbumCoverYear();
            """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(mayNeedMapColumn(STRING.canonicalName))
            }
        }
    }

    @Test
    fun testOneToOneStringMapInfoForKeyInsteadOfColumn() {
        singleQueryMethod<ReadQueryFunction>(
            """
                @MapInfo(keyColumn = "mArtistName")
                @Query("select * from Artist JOIN Song ON Artist.mArtistName == Song.mArtist")
                abstract Map<Artist, String> getAllArtistsWithAlbumCoverYear();
            """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(mayNeedMapColumn(STRING.canonicalName))
            }
        }
    }

    @Test
    fun testMissingMapInfoOneToManyString() {
        singleQueryMethod<ReadQueryFunction>(
            """
                @Query("select * from Artist JOIN Song ON Artist.mArtistName == Song.mArtist")
                abstract Map<Artist, List<String>> getAllArtistsWithAlbumCoverYear();
            """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(mayNeedMapColumn(STRING.canonicalName))
            }
        }
    }

    @Test
    fun testMissingMapInfoImmutableListMultimapOneToOneString() {
        singleQueryMethod<ReadQueryFunction>(
            """
                @Query("select * from Artist JOIN Song ON Artist.mArtistName == Song.mArtist")
                abstract ImmutableListMultimap<Artist, String> getAllArtistsWithAlbumCoverYear();
            """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(mayNeedMapColumn(STRING.canonicalName))
            }
        }
    }

    @Test
    fun testMissingMapInfoOneToOneLong() {
        singleQueryMethod<ReadQueryFunction>(
            """
                @Query("SELECT * FROM Artist JOIN Image ON Artist.mArtistName = Image.mArtistInImage")
                Map<Artist, Long> getAllArtistsWithAlbumCoverYear();
            """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(mayNeedMapColumn(XTypeName.BOXED_LONG.canonicalName))
            }
        }
    }

    @Test
    fun testMissingMapInfoOneToManyLong() {
        singleQueryMethod<ReadQueryFunction>(
            """
                @Query("SELECT * FROM Artist JOIN Image ON Artist.mArtistName = Image.mArtistInImage")
                Map<Artist, Set<Long>> getAllArtistsWithAlbumCoverYear();
            """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(mayNeedMapColumn(XTypeName.BOXED_LONG.canonicalName))
            }
        }
    }

    @Test
    fun testMissingMapInfoImmutableListMultimapOneToOneLong() {
        singleQueryMethod<ReadQueryFunction>(
            """
                @Query("SELECT * FROM Artist JOIN Image ON Artist.mArtistName = Image.mArtistInImage")
                ImmutableListMultimap<Artist, Long> getAllArtistsWithAlbumCoverYear();
            """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(mayNeedMapColumn(XTypeName.BOXED_LONG.canonicalName))
            }
        }
    }

    @Test
    fun testMissingMapInfoImmutableListMultimapOneToOneTypeConverterKey() {
        singleQueryMethod<ReadQueryFunction>(
            """
                @TypeConverters(DateConverter.class)
                @Query("SELECT * FROM Image JOIN Artist ON Artist.mArtistName = Image.mArtistInImage")
                ImmutableMap<java.util.Date, Artist> getAlbumDateWithBandActivity();
            """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(mayNeedMapColumn("java.util.Date"))
            }
        }
    }

    @Test
    fun testMissingMapInfoImmutableListMultimapOneToOneTypeConverterValue() {
        singleQueryMethod<ReadQueryFunction>(
            """
                @TypeConverters(DateConverter.class)
                @Query("SELECT * FROM Artist JOIN Image ON Artist.mArtistName = Image.mArtistInImage")
                ImmutableMap<Artist, java.util.Date> getAlbumDateWithBandActivity();
            """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(mayNeedMapColumn("java.util.Date"))
            }
        }
    }

    @Test
    fun testUseMapInfoWithColumnsNotInQuery() {
        if (!enableVerification) {
            return
        }
        singleQueryMethod<ReadQueryFunction>(
            """
                @MapInfo(keyColumn="cat", valueColumn="dog")
                @Query("select * from User u JOIN Book b ON u.uid == b.uid")
                abstract Map<User, Book> getMultimap();
            """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasWarningCount(1)
                hasWarningContaining(
                    ProcessorErrors.classMustImplementEqualsAndHashCode("foo.bar.User")
                )
                hasErrorCount(2)
                hasErrorContaining(
                    "Column specified in the provided @MapInfo annotation must " +
                        "be present in the query. Provided: cat."
                )
                hasErrorContaining(
                    "Column specified in the provided @MapInfo annotation must " +
                        "be present in the query. Provided: dog."
                )
            }
        }
    }

    @Test
    fun testAmbiguousColumnInMapInfo() {
        if (!enableVerification) {
            // No warning without verification, avoiding false positives
            return
        }
        singleQueryMethod<ReadQueryFunction>(
            """
                @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
                @MapInfo(keyColumn = "uid")
                @Query("SELECT * FROM User u JOIN Book b ON u.uid == b.uid")
                abstract Map<Integer, Book> getMultimap();
            """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasWarning(
                    ProcessorErrors.ambiguousColumn(
                        "uid",
                        ProcessorErrors.AmbiguousColumnLocation.MAP_INFO,
                        null
                    )
                )
            }
        }
    }

    @Test
    fun testAmbiguousColumnInMapDataClass() {
        if (!enableVerification) {
            // No warning without verification, avoiding false positives
            return
        }
        val extraDataClass =
            Source.java(
                "foo.bar.Id",
                """
                package foo.bar;
                public class Id {
                    public int uid;

                    @Override
                    public boolean equals(Object o) {
                        return true;
                    }

                    @Override
                    public int hashCode() {
                        return 0;
                    }
                }
            """
                    .trimIndent()
            )
        singleQueryMethod<ReadQueryFunction>(
            """
                @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
                @Query("SELECT * FROM User u JOIN Book b ON u.uid == b.uid")
                abstract Map<Id, Book> getMultimap();
            """,
            additionalSources = listOf(extraDataClass)
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasWarning(
                    ProcessorErrors.ambiguousColumn(
                        "uid",
                        ProcessorErrors.AmbiguousColumnLocation.DATA_CLASS,
                        "foo.bar.Id"
                    )
                )
            }
        }
    }

    @Test
    fun suspendReturnsDeferredType() {
        listOf(
                "${RxJava2TypeNames.FLOWABLE.canonicalName}<Int>",
                "${RxJava2TypeNames.OBSERVABLE.canonicalName}<Int>",
                "${RxJava2TypeNames.MAYBE.canonicalName}<Int>",
                "${RxJava2TypeNames.SINGLE.canonicalName}<Int>",
                "${RxJava2TypeNames.COMPLETABLE.canonicalName}",
                "${RxJava3TypeNames.FLOWABLE.canonicalName}<Int>",
                "${RxJava3TypeNames.OBSERVABLE.canonicalName}<Int>",
                "${RxJava3TypeNames.MAYBE.canonicalName}<Int>",
                "${RxJava3TypeNames.SINGLE.canonicalName}<Int>",
                "${RxJava3TypeNames.COMPLETABLE.canonicalName}",
                "${LifecyclesTypeNames.LIVE_DATA.canonicalName}<Int>",
                "${LifecyclesTypeNames.COMPUTABLE_LIVE_DATA.canonicalName}<Int>",
                "${GuavaUtilConcurrentTypeNames.LISTENABLE_FUTURE.canonicalName}<Int>",
                "${ReactiveStreamsTypeNames.PUBLISHER.canonicalName}<Int>",
                "${KotlinTypeNames.FLOW.canonicalName}<Int>"
            )
            .forEach { type ->
                singleQueryFunction<WriteQueryFunction>(
                    """
                @Query("DELETE from User where uid = :id")
                abstract suspend fun foo(id: Int): $type
                """
                ) { _, invocation ->
                    invocation.assertCompilationResult {
                        val rawTypeName = type.substringBefore("<")
                        hasErrorContaining(ProcessorErrors.suspendReturnsDeferredType(rawTypeName))
                    }
                }
            }
    }

    @Test
    fun nonNullVoidGuava() {
        singleQueryFunction<WriteQueryFunction>(
            """
                @Query("DELETE from User where uid = :id")
                abstract fun foo(id: Int): ListenableFuture<Void>
                """
        ) { _, invocation ->
            invocation.assertCompilationResult { hasErrorContaining(ProcessorErrors.NONNULL_VOID) }
        }
    }

    @Test
    fun maybe() {
        singleQueryFunction<ReadQueryFunction>(
            """
                @Query("SELECT * FROM book WHERE bookId = :bookId")
                abstract fun getBookMaybe(bookId: String): io.reactivex.Maybe<Book>
                """
        ) { _, invocation ->
            invocation.assertCompilationResult { hasErrorCount(0) }
        }
    }

    @Test
    fun single() {
        singleQueryFunction<ReadQueryFunction>(
            """
                @Query("SELECT * FROM book WHERE bookId = :bookId")
                abstract fun getBookSingle(bookId: String): io.reactivex.Single<Book>
                """
        ) { _, invocation ->
            invocation.assertCompilationResult { hasErrorCount(0) }
        }
    }

    @Test
    fun testStringArraySingleColumnQuery() {
        if (!enableVerification) {
            return
        }
        singleQueryMethod<ReadQueryFunction>(
            """
                @Query("select * from User")
                abstract String[] stringArray();
            """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.invalidQueryForSingleColumnArray("java.lang.String[]")
                )
            }
        }
    }

    @Test
    fun testLongArraySingleColumnQuery() {
        if (!enableVerification) {
            return
        }
        singleQueryMethod<ReadQueryFunction>(
            """
                @Query("select * from User")
                abstract long[] longArray();
            """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.invalidQueryForSingleColumnArray("long[]"))
            }
        }
    }
}
