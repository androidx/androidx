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
import androidx.room.Dao
import androidx.room.RawQuery
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.runProcessorTest
import androidx.room.ext.GuavaUtilConcurrentTypeNames
import androidx.room.ext.KotlinTypeNames
import androidx.room.ext.LifecyclesTypeNames
import androidx.room.ext.PagingTypeNames
import androidx.room.ext.ReactiveStreamsTypeNames
import androidx.room.ext.RxJava2TypeNames
import androidx.room.ext.RxJava3TypeNames
import androidx.room.ext.SupportDbTypeNames
import androidx.room.processor.ProcessorErrors.RAW_QUERY_STRING_PARAMETER_REMOVED
import androidx.room.testing.context
import androidx.room.vo.RawQueryMethod
import androidx.sqlite.db.SupportSQLiteQuery
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
                """
        ) { query, _ ->
            assertThat(query.name, `is`("foo"))
            assertThat(
                query.runtimeQueryParam,
                `is`(
                    RawQueryMethod.RuntimeQueryParameter(
                        paramName = "query",
                        type = SupportDbTypeNames.QUERY
                    )
                )
            )
            assertThat(
                query.returnType.typeName,
                `is`(ArrayTypeName.of(TypeName.INT) as TypeName)
            )
        }
    }

    @Test
    fun stringRawQuery() {
        singleQueryMethod(
            """
                @RawQuery
                abstract public int[] foo(String query);
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(RAW_QUERY_STRING_PARAMETER_REMOVED)
            }
        }
    }

    @Test
    fun withObservedEntities() {
        singleQueryMethod(
            """
                @RawQuery(observedEntities = User.class)
                abstract public LiveData<User> foo(SupportSQLiteQuery query);
                """
        ) { query, _ ->
            assertThat(query.name, `is`("foo"))
            assertThat(
                query.runtimeQueryParam,
                `is`(
                    RawQueryMethod.RuntimeQueryParameter(
                        paramName = "query",
                        type = SupportDbTypeNames.QUERY
                    )
                )
            )
            assertThat(query.observedTableNames.size, `is`(1))
            assertThat(query.observedTableNames, `is`(setOf("User")))
        }
    }

    @Test
    fun observableWithoutEntities() {
        singleQueryMethod(
            """
                @RawQuery(observedEntities = {})
                abstract public LiveData<User> foo(SupportSQLiteQuery query);
                """
        ) { query, invocation ->
            assertThat(query.name, `is`("foo"))
            assertThat(
                query.runtimeQueryParam,
                `is`(
                    RawQueryMethod.RuntimeQueryParameter(
                        paramName = "query",
                        type = SupportDbTypeNames.QUERY
                    )
                )
            )
            assertThat(query.observedTableNames, `is`(emptySet()))
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.OBSERVABLE_QUERY_NOTHING_TO_OBSERVE
                )
            }
        }
    }

    @Test
    fun observableWithoutEntities_dataSourceFactory() {
        singleQueryMethod(
            """
                @RawQuery
                abstract public ${PagingTypeNames.DATA_SOURCE_FACTORY}<Integer, User> getOne();
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.OBSERVABLE_QUERY_NOTHING_TO_OBSERVE
                )
            }
        }
    }

    @Test
    fun observableWithoutEntities_positionalDataSource() {
        singleQueryMethod(
            """
                @RawQuery
                abstract public ${PagingTypeNames.POSITIONAL_DATA_SOURCE}<User> getOne();
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.OBSERVABLE_QUERY_NOTHING_TO_OBSERVE
                )
            }
        }
    }

    @Test
    fun positionalDataSource() {
        singleQueryMethod(
            """
                @RawQuery(observedEntities = {User.class})
                abstract public ${PagingTypeNames.POSITIONAL_DATA_SOURCE}<User> getOne(
                        SupportSQLiteQuery query);
                """
        ) { _, _ ->
            // do nothing
        }
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
                """
        ) { query, _ ->
            assertThat(query.name, `is`("foo"))
            assertThat(
                query.runtimeQueryParam,
                `is`(
                    RawQueryMethod.RuntimeQueryParameter(
                        paramName = "query",
                        type = SupportDbTypeNames.QUERY
                    )
                )
            )
            assertThat(query.returnType.typeName, `is`(pojo))
            assertThat(query.observedTableNames, `is`(emptySet()))
        }
    }

    @Test
    fun void() {
        singleQueryMethod(
            """
                @RawQuery
                abstract public void foo(SupportSQLiteQuery query);
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.RAW_QUERY_BAD_RETURN_TYPE
                )
            }
        }
    }

    interface RawQuerySuspendUnitDao {
        @RawQuery
        suspend fun foo(query: SupportSQLiteQuery)
    }

    @Test
    fun suspendUnit() {
        runProcessorTest { invocation ->
            val daoElement =
                invocation.processingEnv.requireTypeElement(RawQuerySuspendUnitDao::class)
            val daoFunctionElement = daoElement.getDeclaredMethods().first()
            RawQueryMethodProcessor(
                baseContext = invocation.context,
                containing = daoElement.type,
                executableElement = daoFunctionElement
            ).process()
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.RAW_QUERY_BAD_RETURN_TYPE)
            }
        }
    }

    @Test
    fun noArgs() {
        singleQueryMethod(
            """
                @RawQuery
                abstract public int[] foo();
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.RAW_QUERY_BAD_PARAMS
                )
            }
        }
    }

    @Test
    fun tooManyArgs() {
        singleQueryMethod(
            """
                @RawQuery
                abstract public int[] foo(SupportSQLiteQuery query,
                                          SupportSQLiteQuery query2);
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.RAW_QUERY_BAD_PARAMS)
            }
        }
    }

    @Test
    fun varargs() {
        singleQueryMethod(
            """
                @RawQuery
                abstract public int[] foo(SupportSQLiteQuery... query);
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.RAW_QUERY_BAD_PARAMS
                )
            }
        }
    }

    @Test
    fun badType() {
        singleQueryMethod(
            """
                @RawQuery
                abstract public int[] foo(int query);
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.RAW_QUERY_BAD_PARAMS
                )
            }
        }
    }

    @Test
    fun badType_nullable() {
        singleQueryMethod(
            """
                @RawQuery
                abstract public int[] foo(@androidx.annotation.Nullable SupportSQLiteQuery query);
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.parameterCannotBeNullable(
                        parameterName = "query"
                    )
                )
            }
        }
    }

    @Test
    fun observed_notAnEntity() {
        singleQueryMethod(
            """
                @RawQuery(observedEntities = {${COMMON.NOT_AN_ENTITY_TYPE_NAME}.class})
                abstract public int[] foo(SupportSQLiteQuery query);
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.rawQueryBadEntity(COMMON.NOT_AN_ENTITY_TYPE_NAME)
                )
            }
        }
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
                """
        ) { method, _ ->
            assertThat(method.observedTableNames, `is`(setOf("User")))
        }
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
                """
        ) { method, _ ->
            assertThat(method.observedTableNames, `is`(setOf("User")))
        }
    }

    @Test
    fun testUseMapInfoWithBothEmptyColumnsProvided() {
        singleQueryMethod(
            """
                @MapInfo
                @RawQuery
                abstract Map<User, Book> getMultimap(SupportSQLiteQuery query);
            """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorCount(1)
                hasErrorContaining(ProcessorErrors.MAP_INFO_MUST_HAVE_AT_LEAST_ONE_COLUMN_PROVIDED)
            }
        }
    }

    @Test
    fun testDoesNotImplementEqualsAndHashcodeRawQuery() {
        singleQueryMethod(
            """
                @RawQuery
                abstract Map<User, Book> getMultimap(SupportSQLiteQuery query);
            """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasWarningCount(1)
                hasWarningContaining(
                    ProcessorErrors.classMustImplementEqualsAndHashCode(
                        "foo.bar.User"
                    )
                )
            }
        }
    }

    @Test
    fun testMissingMapInfoOneToOneString() {
        singleQueryMethod(
            """
                @RawQuery
                abstract Map<Artist, String> getAllArtistsWithAlbumCoverYear(SupportSQLiteQuery query);
            """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.valueMayNeedMapInfo(
                        ClassName.get("java.lang", "String")
                    )
                )
            }
        }
    }

    @Test
    fun testMissingMapInfoOneToManyString() {
        singleQueryMethod(
            """
                @RawQuery
                abstract Map<Artist, List<String>> getAllArtistsWithAlbumCoverYear(SupportSQLiteQuery query);
            """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.valueMayNeedMapInfo(
                        ClassName.get("java.lang", "String")
                    )
                )
            }
        }
    }

    @Test
    fun testMissingMapInfoImmutableListMultimapOneToOneString() {
        singleQueryMethod(
            """
                @RawQuery
                abstract ImmutableListMultimap<Artist, String> getAllArtistsWithAlbumCoverYear(SupportSQLiteQuery query);
            """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.valueMayNeedMapInfo(
                        ClassName.get("java.lang", "String")
                    )
                )
            }
        }
    }

    @Test
    fun testMissingMapInfoOneToOneLong() {
        singleQueryMethod(
            """
                @RawQuery
                Map<Artist, Long> getAllArtistsWithAlbumCoverYear(SupportSQLiteQuery query);
            """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.valueMayNeedMapInfo(
                        ClassName.get("java.lang", "Long")
                    )
                )
            }
        }
    }

    @Test
    fun testMissingMapInfoOneToManyLong() {
        singleQueryMethod(
            """
                @RawQuery
                Map<Artist, Set<Long>> getAllArtistsWithAlbumCoverYear(SupportSQLiteQuery query);
            """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.valueMayNeedMapInfo(
                        ClassName.get("java.lang", "Long")
                    )
                )
            }
        }
    }

    @Test
    fun testMissingMapInfoImmutableListMultimapOneToOneLong() {
        singleQueryMethod(
            """
                @RawQuery
                ImmutableListMultimap<Artist, Long> getAllArtistsWithAlbumCoverYear(SupportSQLiteQuery query);
            """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.valueMayNeedMapInfo(
                        ClassName.get("java.lang", "Long")
                    )
                )
            }
        }
    }

    @Test
    fun testMissingMapInfoImmutableListMultimapOneToOneTypeConverterKey() {
        singleQueryMethod(
            """
                @TypeConverters(DateConverter.class)
                @RawQuery
                ImmutableMap<java.util.Date, Artist> getAlbumDateWithBandActivity(SupportSQLiteQuery query);
            """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.keyMayNeedMapInfo(
                        ClassName.get("java.util", "Date")
                    )
                )
            }
        }
    }

    @Test
    fun testMissingMapInfoImmutableListMultimapOneToOneTypeConverterValue() {
        singleQueryMethod(
            """
                @TypeConverters(DateConverter.class)
                @RawQuery
                ImmutableMap<Artist, java.util.Date> getAlbumDateWithBandActivity(SupportSQLiteQuery query);
            """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.valueMayNeedMapInfo(
                        ClassName.get("java.util", "Date")
                    )
                )
            }
        }
    }

    @Test
    fun testOneToOneStringMapInfoForKeyInsteadOfColumn() {
        singleQueryMethod(
            """
                @MapInfo(keyColumn = "mArtistName")
                @RawQuery
                abstract Map<Artist, String> getAllArtistsWithAlbumCoverYear(SupportSQLiteQuery query);
            """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.valueMayNeedMapInfo(
                        ClassName.get("java.lang", "String")
                    )
                )
            }
        }
    }

    @Test
    fun suspendReturnsDeferredType() {
        listOf(
            "${RxJava2TypeNames.FLOWABLE}<Int>",
            "${RxJava2TypeNames.OBSERVABLE}<Int>",
            "${RxJava2TypeNames.MAYBE}<Int>",
            "${RxJava2TypeNames.SINGLE}<Int>",
            "${RxJava2TypeNames.COMPLETABLE}",
            "${RxJava3TypeNames.FLOWABLE}<Int>",
            "${RxJava3TypeNames.OBSERVABLE}<Int>",
            "${RxJava3TypeNames.MAYBE}<Int>",
            "${RxJava3TypeNames.SINGLE}<Int>",
            "${RxJava3TypeNames.COMPLETABLE}",
            "${LifecyclesTypeNames.LIVE_DATA}<Int>",
            "${LifecyclesTypeNames.COMPUTABLE_LIVE_DATA}<Int>",
            "${GuavaUtilConcurrentTypeNames.LISTENABLE_FUTURE}<Int>",
            "${ReactiveStreamsTypeNames.PUBLISHER}<Int>",
            "${KotlinTypeNames.FLOW}<Int>"
        ).forEach { type ->
            singleQueryMethodKotlin(
                """
                @RawQuery
                abstract suspend fun foo(query: SupportSQLiteQuery): $type
                """
            ) { _, invocation ->
                invocation.assertCompilationResult {
                    val rawTypeName = type.substringBefore("<")
                    hasErrorContaining(ProcessorErrors.suspendReturnsDeferredType(rawTypeName))
                }
            }
        }
    }

    private fun singleQueryMethod(
        vararg input: String,
        handler: (RawQueryMethod, XTestInvocation) -> Unit
    ) {
        val inputSource = Source.java(
            "foo.bar.MyClass",
            DAO_PREFIX +
                input.joinToString("\n") +
                DAO_SUFFIX
        )
        val commonSources = listOf(
            COMMON.LIVE_DATA, COMMON.COMPUTABLE_LIVE_DATA, COMMON.USER,
            COMMON.DATA_SOURCE_FACTORY, COMMON.POSITIONAL_DATA_SOURCE,
            COMMON.NOT_AN_ENTITY, COMMON.BOOK, COMMON.ARTIST, COMMON.SONG, COMMON.IMAGE,
            COMMON.IMAGE_FORMAT, COMMON.CONVERTER
        )
        runProcessorTest(
            sources = commonSources + inputSource
        ) { invocation ->
            val (owner, methods) = invocation.roundEnv
                .getElementsAnnotatedWith(Dao::class.qualifiedName!!)
                .filterIsInstance<XTypeElement>()
                .map {
                    Pair(
                        it,
                        it.getAllMethods().filter {
                            it.hasAnnotation(RawQuery::class)
                        }.toList()
                    )
                }.first { it.second.isNotEmpty() }
            val parser = RawQueryMethodProcessor(
                baseContext = invocation.context,
                containing = owner.type,
                executableElement = methods.first()
            )
            val parsedQuery = parser.process()
            handler(parsedQuery, invocation)
        }
    }

    private fun singleQueryMethodKotlin(
        vararg input: String,
        handler: (RawQueryMethod, XTestInvocation) -> Unit
    ) {
        val inputSource = Source.kotlin(
            "MyClass.kt",
            DAO_PREFIX_KT +
                input.joinToString("\n") +
                DAO_SUFFIX
        )
        val commonSources = listOf(
            COMMON.USER, COMMON.BOOK, COMMON.NOT_AN_ENTITY, COMMON.RX2_COMPLETABLE,
            COMMON.RX2_MAYBE, COMMON.RX2_SINGLE, COMMON.RX2_FLOWABLE, COMMON.RX2_OBSERVABLE,
            COMMON.RX3_COMPLETABLE, COMMON.RX3_MAYBE, COMMON.RX3_SINGLE, COMMON.RX3_FLOWABLE,
            COMMON.RX3_OBSERVABLE, COMMON.LISTENABLE_FUTURE, COMMON.LIVE_DATA,
            COMMON.COMPUTABLE_LIVE_DATA, COMMON.PUBLISHER, COMMON.FLOW, COMMON.GUAVA_ROOM
        )
        runProcessorTest(
            sources = commonSources + inputSource
        ) { invocation ->
            val (owner, methods) = invocation.roundEnv
                .getElementsAnnotatedWith(Dao::class.qualifiedName!!)
                .filterIsInstance<XTypeElement>()
                .map {
                    Pair(
                        it,
                        it.getAllMethods().filter {
                            it.hasAnnotation(RawQuery::class)
                        }.toList()
                    )
                }.first { it.second.isNotEmpty() }
            val parser = RawQueryMethodProcessor(
                baseContext = invocation.context,
                containing = owner.type,
                executableElement = methods.first()
            )
            val parsedQuery = parser.process()
            handler(parsedQuery, invocation)
        }
    }

    companion object {
        private const val DAO_PREFIX = """
                package foo.bar;
                import androidx.annotation.NonNull;
                import androidx.room.*;
                import androidx.sqlite.db.SupportSQLiteQuery;
                import androidx.lifecycle.LiveData;
                import java.util.*;
                import com.google.common.collect.*;
                @Dao
                abstract class MyClass {
                """
        const val DAO_PREFIX_KT = """
                package foo.bar
                import androidx.room.*
                import java.util.*
                import io.reactivex.*         
                io.reactivex.rxjava3.core.*
                androidx.lifecycle.*
                com.google.common.util.concurrent.*
                org.reactivestreams.*
                kotlinx.coroutines.flow.*
                
                @Dao
                abstract class MyClass {
                """
        private const val DAO_SUFFIX = "}"
    }
}