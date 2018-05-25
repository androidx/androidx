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
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.LifecyclesTypeNames
import androidx.room.ext.PagingTypeNames
import androidx.room.ext.hasAnnotation
import androidx.room.ext.typeName
import androidx.room.parser.Table
import androidx.room.processor.ProcessorErrors.CANNOT_FIND_QUERY_RESULT_ADAPTER
import androidx.room.solver.query.result.DataSourceFactoryQueryResultBinder
import androidx.room.solver.query.result.ListQueryResultAdapter
import androidx.room.solver.query.result.LiveDataQueryResultBinder
import androidx.room.solver.query.result.PojoRowAdapter
import androidx.room.solver.query.result.SingleEntityQueryResultAdapter
import androidx.room.testing.TestInvocation
import androidx.room.testing.TestProcessor
import androidx.room.vo.Field
import androidx.room.vo.QueryMethod
import androidx.room.vo.Warning
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
import com.squareup.javapoet.TypeVariableName
import createVerifierFromEntities
import mockElementAndType
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.hasItem
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.not
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.Mockito
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind.INT
import javax.lang.model.type.TypeMirror

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
@RunWith(Parameterized::class)
class QueryMethodProcessorTest(val enableVerification: Boolean) {
    companion object {
        const val DAO_PREFIX = """
                package foo.bar;
                import androidx.annotation.NonNull;
                import androidx.room.*;
                @Dao
                abstract class MyClass {
                """
        const val DAO_SUFFIX = "}"
        val POJO: ClassName = ClassName.get("foo.bar", "MyClass.Pojo")
        @Parameterized.Parameters(name = "enableDbVerification={0}")
        @JvmStatic
        fun getParams() = arrayOf(true, false)

        fun createField(name: String, columnName: String? = null): Field {
            val (element, type) = mockElementAndType()
            return Field(
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
        singleQueryMethod(
                """
                @Query("SELECT * from User")
                abstract public int[] foo();
                """) { parsedQuery, _ ->
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
                @Query("SELECT * from User where uid = :x")
                abstract public long foo(int x);
                """) { parsedQuery, invocation ->
            assertThat(parsedQuery.name, `is`("foo"))
            assertThat(parsedQuery.returnType.typeName(), `is`(TypeName.LONG))
            assertThat(parsedQuery.parameters.size, `is`(1))
            val param = parsedQuery.parameters.first()
            assertThat(param.name, `is`("x"))
            assertThat(param.sqlName, `is`("x"))
            assertThat(param.type,
                    `is`(invocation.processingEnv.typeUtils.getPrimitiveType(INT) as TypeMirror))
        }.compilesWithoutError()
    }

    @Test
    fun testVarArgs() {
        singleQueryMethod(
                """
                @Query("SELECT * from User where uid in (:ids)")
                abstract public long foo(int... ids);
                """) { parsedQuery, invocation ->
            assertThat(parsedQuery.name, `is`("foo"))
            assertThat(parsedQuery.returnType.typeName(), `is`(TypeName.LONG))
            assertThat(parsedQuery.parameters.size, `is`(1))
            val param = parsedQuery.parameters.first()
            assertThat(param.name, `is`("ids"))
            assertThat(param.sqlName, `is`("ids"))
            val types = invocation.processingEnv.typeUtils
            assertThat(param.type,
                    `is`(types.getArrayType(types.getPrimitiveType(INT)) as TypeMirror))
        }.compilesWithoutError()
    }

    @Test
    fun testParamBindingMatchingNoName() {
        singleQueryMethod(
                """
                @Query("SELECT uid from User where uid = :id")
                abstract public long getIdById(int id);
                """) { parsedQuery, _ ->
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
                @Query("SELECT uid from User where uid = :id")
                abstract public long getIdById(int id);
                """) { parsedQuery, _ ->
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
                @Query("SELECT uid from User where uid = :id OR uid = :id")
                abstract public long getIdById(int id);
                """) { parsedQuery, _ ->
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
                @Query("SELECT uid from User where uid = :id OR uid = :uid")
                abstract public long getIdById(int id);
                """) { parsedQuery, _ ->
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
                @Query("SELECT uid from User where name = :bar AND uid = :id OR uid = :uid")
                abstract public long getIdById(int id);
                """) { parsedQuery, _ ->
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
                @Query("SELECT uid from User where name = :bar")
                abstract public long getIdById(int bar, int whyNotUseMe);
                """) { parsedQuery, _ ->
            val bar = parsedQuery.query.bindSections[0]
            val barParam = parsedQuery.parameters.firstOrNull()
            assertThat(bar, notNullValue())
            assertThat(barParam, notNullValue())
            assertThat(parsedQuery.sectionToParamMapping,
                    `is`(listOf(Pair(bar, barParam))))
        }.failsToCompile().withErrorContaining(
                ProcessorErrors.unusedQueryMethodParameter(listOf("whyNotUseMe")))
    }

    @Test
    fun testNameWithUnderscore() {
        singleQueryMethod(
                """
                @Query("select * from User where uid = :_blah")
                abstract public long getSth(int _blah);
                """
        ) { _, _ -> }
                .failsToCompile()
                .withErrorContaining(ProcessorErrors.QUERY_PARAMETERS_CANNOT_START_WITH_UNDERSCORE)
    }

    @Test
    fun testGenericReturnType() {
        singleQueryMethod(
                """
                @Query("select * from User")
                abstract public <T> ${CommonTypeNames.LIST}<T> foo(int x);
                """) { parsedQuery, _ ->
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
                """) { _, _ ->
            // do nothing
        }.failsToCompile()
                .withErrorContaining("UNEXPECTED_CHAR=:")
    }

    @Test
    fun testLiveDataWithWithClause() {
        singleQueryMethod(
                """
                @Query("WITH RECURSIVE tempTable(n, fact) AS (SELECT 0, 1 UNION ALL SELECT n+1,"
                + " (n+1)*fact FROM tempTable WHERE n < 9) SELECT fact FROM tempTable, User")
                abstract public ${LifecyclesTypeNames.LIVE_DATA}<${CommonTypeNames.LIST}<Integer>>
                getFactorialLiveData();
                """) { parsedQuery, _ ->
            assertThat(parsedQuery.query.tables, hasItem(Table("User", "User")))
            assertThat(parsedQuery.query.tables,
                    not(hasItem(Table("tempTable", "tempTable"))))
            assertThat(parsedQuery.query.tables.size, `is`(1))
        }.compilesWithoutError()
    }

    @Test
    fun testLiveDataWithNothingToObserve() {
        singleQueryMethod(
                """
                @Query("SELECT 1")
                abstract public ${LifecyclesTypeNames.LIVE_DATA}<Integer> getOne();
                """) { _, _ ->
            // do nothing
        }.failsToCompile()
                .withErrorContaining(ProcessorErrors.OBSERVABLE_QUERY_NOTHING_TO_OBSERVE)
    }

    @Test
    fun testLiveDataWithWithClauseAndNothingToObserve() {
        singleQueryMethod(
                """
                @Query("WITH RECURSIVE tempTable(n, fact) AS (SELECT 0, 1 UNION ALL SELECT n+1,"
                + " (n+1)*fact FROM tempTable WHERE n < 9) SELECT fact FROM tempTable")
                abstract public ${LifecyclesTypeNames.LIVE_DATA}<${CommonTypeNames.LIST}<Integer>>
                getFactorialLiveData();
                """) { _, _ ->
            // do nothing
        }.failsToCompile()
                .withErrorContaining(ProcessorErrors.OBSERVABLE_QUERY_NOTHING_TO_OBSERVE)
    }

    @Test
    fun testBoundGeneric() {
        singleQueryMethod(
                """
                static abstract class BaseModel<T> {
                    @Query("select COUNT(*) from User")
                    abstract public T getT();
                }
                @Dao
                static abstract class ExtendingModel extends BaseModel<Integer> {
                }
                """) { parsedQuery, _ ->
            assertThat(parsedQuery.returnType.typeName(),
                    `is`(ClassName.get(Integer::class.java) as TypeName))
        }.compilesWithoutError()
    }

    @Test
    fun testBoundGenericParameter() {
        singleQueryMethod(
                """
                static abstract class BaseModel<T> {
                    @Query("select COUNT(*) from User where :t")
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
                @Query("DELETE from User where uid = :id")
                abstract public float foo(int id);
                """) { _, _ ->
        }.failsToCompile().withErrorContaining(
                ProcessorErrors.DELETION_METHODS_MUST_RETURN_VOID_OR_INT
        )
    }

    @Test
    fun testSimpleDelete() {
        singleQueryMethod(
                """
                @Query("DELETE from User where uid = :id")
                abstract public int foo(int id);
                """) { parsedQuery, _ ->
            assertThat(parsedQuery.name, `is`("foo"))
            assertThat(parsedQuery.parameters.size, `is`(1))
            assertThat(parsedQuery.returnType.typeName(), `is`(TypeName.INT))
        }.compilesWithoutError()
    }

    @Test
    fun testVoidDeleteQuery() {
        singleQueryMethod(
                """
                @Query("DELETE from User where uid = :id")
                abstract public void foo(int id);
                """) { parsedQuery, _ ->
            assertThat(parsedQuery.name, `is`("foo"))
            assertThat(parsedQuery.parameters.size, `is`(1))
            assertThat(parsedQuery.returnType.typeName(), `is`(TypeName.VOID))
        }.compilesWithoutError()
    }

    @Test
    fun testVoidUpdateQuery() {
        singleQueryMethod(
                """
                @Query("update user set name = :name")
                abstract public void updateAllNames(String name);
                """) { parsedQuery, invocation ->
            assertThat(parsedQuery.name, `is`("updateAllNames"))
            assertThat(parsedQuery.parameters.size, `is`(1))
            assertThat(parsedQuery.returnType.typeName(), `is`(TypeName.VOID))
            assertThat(parsedQuery.parameters.first().type.typeName(),
                    `is`(invocation.context.COMMON_TYPES.STRING.typeName()))
        }.compilesWithoutError()
    }

    @Test
    fun testLiveDataQuery() {
        singleQueryMethod(
                """
                @Query("select name from user where uid = :id")
                abstract ${LifecyclesTypeNames.LIVE_DATA}<String> nameLiveData(String id);
                """
        ) { parsedQuery, _ ->
            assertThat(parsedQuery.returnType.typeName(),
                    `is`(ParameterizedTypeName.get(LifecyclesTypeNames.LIVE_DATA,
                            String::class.typeName()) as TypeName))
            assertThat(parsedQuery.queryResultBinder,
                    instanceOf(LiveDataQueryResultBinder::class.java))
        }.compilesWithoutError()
    }

    @Test
    fun testNonSelectLiveData() {
        singleQueryMethod(
                """
                @Query("delete from user where uid = :id")
                abstract ${LifecyclesTypeNames.LIVE_DATA}<Integer> deleteLiveData(String id);
                """
        ) { _, _ ->
        }.failsToCompile()
                .withErrorContaining(ProcessorErrors.DELETION_METHODS_MUST_RETURN_VOID_OR_INT)
    }

    @Test
    fun testDataSourceFactoryQuery() {
        singleQueryMethod(
                """
                @Query("select name from user")
                abstract ${PagingTypeNames.DATA_SOURCE_FACTORY}<Integer, String>
                nameDataSourceFactory();
                """
        ) { parsedQuery, _ ->
            assertThat(parsedQuery.returnType.typeName(),
                    `is`(ParameterizedTypeName.get(PagingTypeNames.DATA_SOURCE_FACTORY,
                            Integer::class.typeName(), String::class.typeName()) as TypeName))
            assertThat(parsedQuery.queryResultBinder,
                    instanceOf(DataSourceFactoryQueryResultBinder::class.java))
            val tableNames =
                    (parsedQuery.queryResultBinder as DataSourceFactoryQueryResultBinder)
                            .positionalDataSourceQueryResultBinder.tableNames
            assertEquals(setOf("user"), tableNames)
        }.compilesWithoutError()
    }

    @Test
    fun testMultiTableDataSourceFactoryQuery() {
        singleQueryMethod(
                """
                @Query("select name from User u LEFT OUTER JOIN Book b ON u.uid == b.uid")
                abstract ${PagingTypeNames.DATA_SOURCE_FACTORY}<Integer, String>
                nameDataSourceFactory();
                """
        ) { parsedQuery, _ ->
            assertThat(parsedQuery.returnType.typeName(),
                    `is`(ParameterizedTypeName.get(PagingTypeNames.DATA_SOURCE_FACTORY,
                            Integer::class.typeName(), String::class.typeName()) as TypeName))
            assertThat(parsedQuery.queryResultBinder,
                    instanceOf(DataSourceFactoryQueryResultBinder::class.java))
            val tableNames =
                    (parsedQuery.queryResultBinder as DataSourceFactoryQueryResultBinder)
                            .positionalDataSourceQueryResultBinder.tableNames
            assertEquals(setOf("User", "Book"), tableNames)
        }.compilesWithoutError()
    }

    @Test
    fun query_detectTransaction_delete() {
        singleQueryMethod(
                """
                @Query("delete from user where uid = :id")
                abstract int deleteUser(String id);
                """
        ) { method, _ ->
            assertThat(method.inTransaction, `is`(true))
        }.compilesWithoutError()
    }

    @Test
    fun query_detectTransaction_update() {
        singleQueryMethod(
                """
                @Query("UPDATE user set uid = :id + 1 where uid = :id")
                abstract int incrementId(String id);
                """
        ) { method, _ ->
            assertThat(method.inTransaction, `is`(true))
        }.compilesWithoutError()
    }

    @Test
    fun query_detectTransaction_select() {
        singleQueryMethod(
                """
                @Query("select * from user")
                abstract int loadUsers();
                """
        ) { method, _ ->
            assertThat(method.inTransaction, `is`(false))
        }.compilesWithoutError()
    }

    @Test
    fun query_detectTransaction_selectInTransaction() {
        singleQueryMethod(
                """
                @Transaction
                @Query("select * from user")
                abstract int loadUsers();
                """
        ) { method, _ ->
            assertThat(method.inTransaction, `is`(true))
        }.compilesWithoutError()
    }

    @Test
    fun skipVerification() {
        singleQueryMethod(
                """
                @SkipQueryVerification
                @Query("SELECT foo from User")
                abstract public int[] foo();
                """) { parsedQuery, _ ->
            assertThat(parsedQuery.name, `is`("foo"))
            assertThat(parsedQuery.parameters.size, `is`(0))
            assertThat(parsedQuery.returnType.typeName(),
                    `is`(ArrayTypeName.of(TypeName.INT) as TypeName))
        }.compilesWithoutError()
    }

    @Test
    fun suppressWarnings() {
        singleQueryMethod("""
                @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
                @Query("SELECT uid from User")
                abstract public int[] foo();
                """) { method, invocation ->
            assertThat(QueryMethodProcessor(
                    baseContext = invocation.context,
                    containing = Mockito.mock(DeclaredType::class.java),
                    executableElement = method.element,
                    dbVerifier = null).context.logger.suppressedWarnings
                    , `is`(setOf(Warning.CURSOR_MISMATCH)))
        }.compilesWithoutError()
    }

    @Test
    fun relationWithExtendsBounds() {
        if (!enableVerification) {
            return
        }
        singleQueryMethod(
            """
                static class Merged extends User {
                   @Relation(parentColumn = "name", entityColumn = "lastName",
                             entity = User.class)
                   java.util.List<? extends User> users;
                }
                @Transaction
                @Query("select * from user")
                abstract java.util.List<Merged> loadUsers();
            """) { method, _ ->
            assertThat(method.queryResultBinder.adapter,
                instanceOf(ListQueryResultAdapter::class.java))
            val listAdapter = method.queryResultBinder.adapter as ListQueryResultAdapter
            assertThat(listAdapter.rowAdapter, instanceOf(PojoRowAdapter::class.java))
            val pojoRowAdapter = listAdapter.rowAdapter as PojoRowAdapter
            assertThat(pojoRowAdapter.relationCollectors.size, `is`(1))
            assertThat(pojoRowAdapter.relationCollectors[0].collectionTypeName, `is`(
                ParameterizedTypeName.get(ClassName.get(ArrayList::class.java),
                    COMMON.USER_TYPE_NAME)
            ))
        }.compilesWithoutError()
            .withWarningCount(0)
    }

    @Test
    fun pojo_renamedColumn() {
        pojoTest("""
                String name;
                String lName;
                """, listOf("name", "lastName as lName")) { adapter, _, _ ->
            assertThat(adapter?.mapping?.unusedColumns, `is`(emptyList()))
            assertThat(adapter?.mapping?.unusedFields, `is`(emptyList()))
        }?.compilesWithoutError()?.withWarningCount(0)
    }

    @Test
    fun pojo_exactMatch() {
        pojoTest("""
                String name;
                String lastName;
                """, listOf("name", "lastName")) { adapter, _, _ ->
            assertThat(adapter?.mapping?.unusedColumns, `is`(emptyList()))
            assertThat(adapter?.mapping?.unusedFields, `is`(emptyList()))
        }?.compilesWithoutError()?.withWarningCount(0)
    }

    @Test
    fun pojo_exactMatchWithStar() {
        pojoTest("""
            String name;
            String lastName;
            int uid;
            @ColumnInfo(name = "ageColumn")
            int age;
        """, listOf("*")) { adapter, _, _ ->
            assertThat(adapter?.mapping?.unusedColumns, `is`(emptyList()))
            assertThat(adapter?.mapping?.unusedFields, `is`(emptyList()))
        }?.compilesWithoutError()?.withWarningCount(0)
    }

    @Test
    fun pojo_nonJavaName() {
        pojoTest("""
            @ColumnInfo(name = "MAX(ageColumn)")
            int maxAge;
            String name;
            """, listOf("MAX(ageColumn)", "name")) { adapter, _, _ ->
            assertThat(adapter?.mapping?.unusedColumns, `is`(emptyList()))
            assertThat(adapter?.mapping?.unusedFields, `is`(emptyList()))
        }?.compilesWithoutError()?.withWarningCount(0)
    }

    @Test
    fun pojo_noMatchingFields() {
        pojoTest("""
                String nameX;
                String lastNameX;
                """, listOf("name", "lastName")) { adapter, _, _ ->
            assertThat(adapter?.mapping?.unusedColumns, `is`(listOf("name", "lastName")))
            assertThat(adapter?.mapping?.unusedFields, `is`(adapter?.pojo?.fields))
        }?.failsToCompile()
                ?.withErrorContaining(CANNOT_FIND_QUERY_RESULT_ADAPTER)
                ?.and()
                ?.withWarningContaining(
                        ProcessorErrors.cursorPojoMismatch(
                                pojoTypeName = POJO,
                                unusedColumns = listOf("name", "lastName"),
                                unusedFields = listOf(createField("nameX"),
                                        createField("lastNameX")),
                                allColumns = listOf("name", "lastName"),
                                allFields = listOf(
                                        createField("nameX"),
                                        createField("lastNameX")
                                )
                        )
                )
    }

    @Test
    fun pojo_badQuery() {
        // do not report mismatch if query is broken
        pojoTest("""
            @ColumnInfo(name = "MAX(ageColumn)")
            int maxAge;
            String name;
            """, listOf("MAX(age)", "name")) { _, _, _ ->
        }?.failsToCompile()
                ?.withErrorContaining("no such column: age")
                ?.and()
                ?.withErrorCount(1)
                ?.withWarningCount(0)
    }

    @Test
    fun pojo_tooManyColumns() {
        pojoTest("""
            String name;
            String lastName;
            """, listOf("uid", "name", "lastName")) { adapter, _, _ ->
            assertThat(adapter?.mapping?.unusedColumns, `is`(listOf("uid")))
            assertThat(adapter?.mapping?.unusedFields, `is`(emptyList()))
        }?.compilesWithoutError()?.withWarningContaining(
                ProcessorErrors.cursorPojoMismatch(
                        pojoTypeName = POJO,
                        unusedColumns = listOf("uid"),
                        unusedFields = emptyList(),
                        allColumns = listOf("uid", "name", "lastName"),
                        allFields = listOf(createField("name"), createField("lastName"))
                ))
    }

    @Test
    fun pojo_tooManyFields() {
        pojoTest("""
            String name;
            String lastName;
            """, listOf("lastName")) { adapter, _, _ ->
            assertThat(adapter?.mapping?.unusedColumns, `is`(emptyList()))
            assertThat(adapter?.mapping?.unusedFields, `is`(
                    adapter?.pojo?.fields?.filter { it.name == "name" }
            ))
        }?.compilesWithoutError()?.withWarningContaining(
                ProcessorErrors.cursorPojoMismatch(
                        pojoTypeName = POJO,
                        unusedColumns = emptyList(),
                        unusedFields = listOf(createField("name")),
                        allColumns = listOf("lastName"),
                        allFields = listOf(createField("name"), createField("lastName"))
                ))
    }

    @Test
    fun pojo_missingNonNull() {
        pojoTest("""
            @NonNull
            String name;
            String lastName;
            """, listOf("lastName")) { adapter, _, _ ->
            assertThat(adapter?.mapping?.unusedColumns, `is`(emptyList()))
            assertThat(adapter?.mapping?.unusedFields, `is`(
                    adapter?.pojo?.fields?.filter { it.name == "name" }
            ))
        }?.failsToCompile()?.withWarningContaining(
                ProcessorErrors.cursorPojoMismatch(
                        pojoTypeName = POJO,
                        unusedColumns = emptyList(),
                        unusedFields = listOf(createField("name")),
                        allColumns = listOf("lastName"),
                        allFields = listOf(createField("name"), createField("lastName"))
                ))?.and()?.withErrorContaining(
                ProcessorErrors.pojoMissingNonNull(pojoTypeName = POJO,
                        missingPojoFields = listOf("name"),
                        allQueryColumns = listOf("lastName")))
    }

    @Test
    fun pojo_tooManyFieldsAndColumns() {
        pojoTest("""
            String name;
            String lastName;
            """, listOf("uid", "name")) { adapter, _, _ ->
            assertThat(adapter?.mapping?.unusedColumns, `is`(listOf("uid")))
            assertThat(adapter?.mapping?.unusedFields, `is`(
                    adapter?.pojo?.fields?.filter { it.name == "lastName" }
            ))
        }?.compilesWithoutError()?.withWarningContaining(
                ProcessorErrors.cursorPojoMismatch(
                        pojoTypeName = POJO,
                        unusedColumns = listOf("uid"),
                        unusedFields = listOf(createField("lastName")),
                        allColumns = listOf("uid", "name"),
                        allFields = listOf(createField("name"), createField("lastName"))
                ))
    }

    fun pojoTest(pojoFields: String, queryColumns: List<String>,
                 handler: (PojoRowAdapter?, QueryMethod, TestInvocation) -> Unit): CompileTester? {
        val assertion = singleQueryMethod(
                """
                static class Pojo {
                    $pojoFields
                }
                @Query("SELECT ${queryColumns.joinToString(", ")} from User LIMIT 1")
                abstract MyClass.Pojo getNameAndLastNames();
                """
        ) { parsedQuery, invocation ->
            val adapter = parsedQuery.queryResultBinder.adapter
            if (enableVerification) {
                if (adapter is SingleEntityQueryResultAdapter) {
                    handler(adapter.rowAdapter as? PojoRowAdapter, parsedQuery, invocation)
                } else {
                    handler(null, parsedQuery, invocation)
                }
            } else {
                assertThat(adapter, nullValue())
            }
        }
        if (enableVerification) {
            return assertion
        } else {
            assertion.failsToCompile().withErrorContaining(CANNOT_FIND_QUERY_RESULT_ADAPTER)
            return null
        }
    }

    private fun singleQueryMethod(
        vararg input: String,
        handler: (QueryMethod, TestInvocation) -> Unit
    ): CompileTester {
        return assertAbout(JavaSourcesSubjectFactory.javaSources())
            .that(
                listOf(
                    JavaFileObjects.forSourceString(
                        "foo.bar.MyClass",
                        DAO_PREFIX + input.joinToString("\n") + DAO_SUFFIX
                    ), COMMON.LIVE_DATA, COMMON.COMPUTABLE_LIVE_DATA, COMMON.USER, COMMON.BOOK
                )
            )
            .processedWith(TestProcessor.builder()
                .forAnnotations(
                    Query::class, Dao::class, ColumnInfo::class,
                    Entity::class, PrimaryKey::class, Relation::class,
                    Transaction::class
                )
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
                        }.first { it.second.isNotEmpty() }
                    val verifier = if (enableVerification) {
                        createVerifierFromEntities(invocation)
                    } else {
                        null
                    }
                    val parser = QueryMethodProcessor(
                        baseContext = invocation.context,
                        containing = MoreTypes.asDeclared(owner.asType()),
                        executableElement = MoreElements.asExecutable(methods.first()),
                        dbVerifier = verifier
                    )
                    val parsedQuery = parser.process()
                    handler(parsedQuery, invocation)
                    true
                }
                .build())
    }
}
