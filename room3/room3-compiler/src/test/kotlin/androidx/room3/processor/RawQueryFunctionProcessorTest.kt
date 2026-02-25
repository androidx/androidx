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

package androidx.room3.processor

import COMMON
import androidx.room3.Dao
import androidx.room3.RawQuery
import androidx.room3.compiler.codegen.XClassName
import androidx.room3.compiler.codegen.XTypeName
import androidx.room3.compiler.processing.XTypeElement
import androidx.room3.compiler.processing.util.Source
import androidx.room3.compiler.processing.util.XTestInvocation
import androidx.room3.compiler.processing.util.runKspTest
import androidx.room3.ext.GuavaUtilConcurrentTypeNames
import androidx.room3.ext.KotlinTypeNames
import androidx.room3.ext.LifecyclesTypeNames
import androidx.room3.ext.PagingTypeNames
import androidx.room3.ext.ReactiveStreamsTypeNames
import androidx.room3.ext.RxJava3TypeNames
import androidx.room3.ext.SupportDbTypeNames
import androidx.room3.processor.ProcessorErrors.RAW_QUERY_STRING_PARAMETER_REMOVED
import androidx.room3.testing.context
import androidx.room3.vo.RawQueryFunction
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class RawQueryFunctionProcessorTest {
    @Test
    fun supportRawQuery() {
        singleQueryMethod(
            """
                @RawQuery
                abstract public int[] foo(SupportSQLiteQuery query);
                """
        ) { query, _ ->
            assertThat(query.element.name, `is`("foo"))
            assertThat(
                query.runtimeQueryParam,
                `is`(
                    RawQueryFunction.RuntimeQueryParameter(
                        paramName = "query",
                        typeName = SupportDbTypeNames.QUERY,
                        isNonNull = false,
                    )
                ),
            )
            assertThat(
                query.returnType.asTypeName(),
                `is`(XTypeName.getArrayName(XTypeName.PRIMITIVE_INT).copy(nullable = true)),
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
            assertThat(query.element.name, `is`("foo"))
            assertThat(
                query.runtimeQueryParam,
                `is`(
                    RawQueryFunction.RuntimeQueryParameter(
                        paramName = "query",
                        typeName = SupportDbTypeNames.QUERY,
                        isNonNull = false,
                    )
                ),
            )
            assertThat(query.observedTableNames.size, `is`(1))
            assertThat(query.observedTableNames, `is`(setOf("User")))
        }
    }

    @Test
    fun positionalDataSource() {
        singleQueryMethod(
            """
                @RawQuery(observedEntities = {User.class})
                abstract public ${PagingTypeNames.POSITIONAL_DATA_SOURCE.canonicalName}<User> getOne(
                        SupportSQLiteQuery query);
                """
        ) { _, _ ->
            // do nothing
        }
    }

    @Test
    fun dataClass() {
        val dataClass = XClassName.get("foo.bar", "MyClass", "MyDataClass")
        singleQueryMethod(
            """
                public class MyDataClass {
                    public String foo;
                    public String bar;
                }

                @RawQuery
                abstract public MyDataClass foo(SupportSQLiteQuery query);
                """
        ) { query, _ ->
            assertThat(query.element.name, `is`("foo"))
            assertThat(
                query.runtimeQueryParam,
                `is`(
                    RawQueryFunction.RuntimeQueryParameter(
                        paramName = "query",
                        typeName = SupportDbTypeNames.QUERY,
                        isNonNull = false,
                    )
                ),
            )
            assertThat(query.returnType.asTypeName(), `is`(dataClass.copy(nullable = true)))
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
                hasErrorContaining(ProcessorErrors.RAW_QUERY_BAD_RETURN_TYPE)
            }
        }
    }

    @Test
    fun suspendUnit() {
        runKspTest(
            sources =
                listOf(
                    Source.kotlin(
                        "RawQuerySuspendUnitDao.kt",
                        """
                        import androidx.room3.RawQuery
                        import androidx.sqlite.db.SupportSQLiteQuery
                        interface RawQuerySuspendUnitDao {
                            @RawQuery suspend fun foo(query: SupportSQLiteQuery)
                        }
                        """
                            .trimIndent(),
                    )
                )
        ) { invocation ->
            val daoElement = invocation.processingEnv.requireTypeElement("RawQuerySuspendUnitDao")
            val daoFunctionElement = daoElement.getDeclaredMethods().first()
            RawQueryFunctionProcessor(
                    baseContext = invocation.context,
                    containing = daoElement.type,
                    executableElement = daoFunctionElement,
                )
                .process()
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
                hasErrorContaining(ProcessorErrors.RAW_QUERY_BAD_PARAMS)
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
                hasErrorContaining(ProcessorErrors.RAW_QUERY_BAD_PARAMS)
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
                hasErrorContaining(ProcessorErrors.RAW_QUERY_BAD_PARAMS)
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
                    ProcessorErrors.parameterCannotBeNullable(parameterName = "query")
                )
            }
        }
    }

    @Test
    fun observed_notAnEntity() {
        singleQueryMethod(
            """
                @RawQuery(observedEntities = {${COMMON.NOT_AN_ENTITY_TYPE_NAME.canonicalName}.class})
                abstract public int[] foo(SupportSQLiteQuery query);
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.rawQueryBadEntity(COMMON.NOT_AN_ENTITY_TYPE_NAME.canonicalName)
                )
            }
        }
    }

    @Test
    fun observed_relationDataClass() {
        singleQueryMethod(
            """
                public static class MyDataClass {
                    public String foo;
                    @Relation(
                        parentColumn = "foo",
                        entityColumn = "name"
                    )
                    public java.util.List<User> users;
                }
                @RawQuery(observedEntities = MyDataClass.class)
                abstract public int[] foo(SupportSQLiteQuery query);
                """
        ) { function, _ ->
            assertThat(function.observedTableNames, `is`(setOf("User")))
        }
    }

    @Test
    fun observed_embedded() {
        singleQueryMethod(
            """
                public static class MyDataClass {
                    public String foo;
                    @Embedded
                    public User users;
                }
                @RawQuery(observedEntities = MyDataClass.class)
                abstract public int[] foo(SupportSQLiteQuery query);
                """
        ) { function, _ ->
            assertThat(function.observedTableNames, `is`(setOf("User")))
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
                    ProcessorErrors.classMustImplementEqualsAndHashCode("foo.bar.User?")
                )
            }
        }
    }

    @Test
    fun testMissingMapColumnOneToOneString() {
        singleQueryMethod(
            """
                @RawQuery
                abstract Map<Artist, String> getAllArtistsWithAlbumCoverYear(SupportSQLiteQuery query);
            """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.mayNeedMapColumn("kotlin.String?"))
            }
        }
    }

    @Test
    fun testMissingMapColumnOneToManyString() {
        singleQueryMethod(
            """
                @RawQuery
                abstract Map<Artist, List<String>> getAllArtistsWithAlbumCoverYear(SupportSQLiteQuery query);
            """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.mayNeedMapColumn("kotlin.String?"))
            }
        }
    }

    @Test
    fun testMissingMapColumnImmutableListMultimapOneToOneString() {
        singleQueryMethod(
            """
                @RawQuery
                abstract ImmutableListMultimap<Artist, String> getAllArtistsWithAlbumCoverYear(SupportSQLiteQuery query);
            """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.mayNeedMapColumn("kotlin.String?"))
            }
        }
    }

    @Test
    fun testMissingMapColumnOneToOneLong() {
        singleQueryMethod(
            """
                @RawQuery
                Map<Artist, Long> getAllArtistsWithAlbumCoverYear(SupportSQLiteQuery query);
            """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.mayNeedMapColumn("kotlin.Long?"))
            }
        }
    }

    @Test
    fun testMissingMapColumnOneToManyLong() {
        singleQueryMethod(
            """
                @RawQuery
                Map<Artist, Set<Long>> getAllArtistsWithAlbumCoverYear(SupportSQLiteQuery query);
            """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.mayNeedMapColumn("kotlin.Long?"))
            }
        }
    }

    @Test
    fun testMissingMapColumnImmutableListMultimapOneToOneLong() {
        singleQueryMethod(
            """
                @RawQuery
                ImmutableListMultimap<Artist, Long> getAllArtistsWithAlbumCoverYear(SupportSQLiteQuery query);
            """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.mayNeedMapColumn("kotlin.Long?"))
            }
        }
    }

    @Test
    fun testMissingMapColumnImmutableListMultimapOneToOneTypeConverterKey() {
        singleQueryMethod(
            """
                @TypeConverters(DateConverter.class)
                @RawQuery
                ImmutableMap<java.util.Date, Artist> getAlbumDateWithBandActivity(SupportSQLiteQuery query);
            """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.mayNeedMapColumn("java.util.Date"))
            }
        }
    }

    @Test
    fun testMissingMapColumnImmutableListMultimapOneToOneTypeConverterValue() {
        singleQueryMethod(
            """
                @TypeConverters(DateConverter.class)
                @RawQuery
                ImmutableMap<Artist, java.util.Date> getAlbumDateWithBandActivity(SupportSQLiteQuery query);
            """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.mayNeedMapColumn("java.util.Date"))
            }
        }
    }

    @Test
    fun testOneToOneStringMapColumnForKeyInsteadOfColumn() {
        singleQueryMethod(
            """
                @RawQuery
                abstract Map<@MapColumn(columnName="mArtistName") Artist, String> getAllArtistsWithAlbumCoverYear(SupportSQLiteQuery query);
            """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.mayNeedMapColumn("kotlin.String?"))
            }
        }
    }

    @Test
    fun testUseMapColumnWithColumnName() {
        singleQueryMethod(
            """
                @SuppressWarnings(
                    {RoomWarnings.QUERY_MISMATCH, RoomWarnings.AMBIGUOUS_COLUMN_IN_RESULT}
                )
                @RawQuery
                abstract Map<@MapColumn(columnName = "uid") Integer, Book> getMultimap(
                    SupportSQLiteQuery query
                );
            """
        ) { _, invocation ->
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    @Test
    fun suspendReturnsDeferredType() {
        listOf(
                "${RxJava3TypeNames.FLOWABLE.canonicalName}<Int>",
                "${RxJava3TypeNames.OBSERVABLE.canonicalName}<Int>",
                "${RxJava3TypeNames.MAYBE.canonicalName}<Int>",
                "${RxJava3TypeNames.SINGLE.canonicalName}<Int>",
                "${RxJava3TypeNames.COMPLETABLE.canonicalName}",
                "${LifecyclesTypeNames.LIVE_DATA.canonicalName}<Int>",
                "${LifecyclesTypeNames.COMPUTABLE_LIVE_DATA.canonicalName}<Int>",
                "${GuavaUtilConcurrentTypeNames.LISTENABLE_FUTURE.canonicalName}<Int>",
                "${ReactiveStreamsTypeNames.PUBLISHER.canonicalName}<Int>",
                "${KotlinTypeNames.FLOW.canonicalName}<Int>",
            )
            .forEach { type ->
                singleQueryFunction(
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

    @Test
    fun nonNullVoidGuava() {
        singleQueryFunction(
            """
                @RawQuery
                abstract fun foo(query: SupportSQLiteQuery): ListenableFuture<Void>
                """
        ) { _, invocation ->
            invocation.assertCompilationResult { hasErrorContaining(ProcessorErrors.NONNULL_VOID) }
        }
    }

    private fun singleQueryMethod(
        vararg input: String,
        handler: (RawQueryFunction, XTestInvocation) -> Unit,
    ) {
        val inputSource =
            Source.java("foo.bar.MyClass", DAO_PREFIX + input.joinToString("\n") + DAO_SUFFIX)
        val commonSources =
            listOf(
                COMMON.LIVE_DATA,
                COMMON.COMPUTABLE_LIVE_DATA,
                COMMON.USER,
                COMMON.DATA_SOURCE_FACTORY,
                COMMON.POSITIONAL_DATA_SOURCE,
                COMMON.NOT_AN_ENTITY,
                COMMON.BOOK,
                COMMON.ARTIST,
                COMMON.SONG,
                COMMON.IMAGE,
                COMMON.IMAGE_FORMAT,
                COMMON.CONVERTER,
                COMMON.RX3_COMPLETABLE,
                COMMON.RX3_MAYBE,
                COMMON.RX3_SINGLE,
                COMMON.RX3_FLOWABLE,
                COMMON.RX3_OBSERVABLE,
                COMMON.PUBLISHER,
            )
        runKspTest(sources = commonSources + inputSource) { invocation ->
            val (owner, functions) =
                invocation.roundEnv
                    .getElementsAnnotatedWith(Dao::class.qualifiedName!!)
                    .filterIsInstance<XTypeElement>()
                    .map {
                        Pair(
                            it,
                            it.getAllMethods().filter { it.hasAnnotation(RawQuery::class) }.toList(),
                        )
                    }
                    .first { it.second.isNotEmpty() }
            val forkedContext = invocation.context.fork(owner)
            val parser =
                RawQueryFunctionProcessor(
                    baseContext = forkedContext,
                    containing = owner.type,
                    executableElement = functions.first(),
                )
            val parsedQuery = parser.process()
            handler(parsedQuery, invocation)
        }
    }

    private fun singleQueryFunction(
        vararg input: String,
        handler: (RawQueryFunction, XTestInvocation) -> Unit,
    ) {
        val inputSource =
            Source.kotlin("MyClass.kt", DAO_PREFIX_KT + input.joinToString("\n") + DAO_SUFFIX)
        val commonSources =
            listOf(
                COMMON.USER,
                COMMON.BOOK,
                COMMON.NOT_AN_ENTITY,
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
            )
        runKspTest(sources = commonSources + inputSource) { invocation ->
            val (owner, functions) =
                invocation.roundEnv
                    .getElementsAnnotatedWith(Dao::class.qualifiedName!!)
                    .filterIsInstance<XTypeElement>()
                    .map {
                        Pair(
                            it,
                            it.getAllMethods().filter { it.hasAnnotation(RawQuery::class) }.toList(),
                        )
                    }
                    .first { it.second.isNotEmpty() }
            val forkedContext = invocation.context.fork(owner)
            val parser =
                RawQueryFunctionProcessor(
                    baseContext = forkedContext,
                    containing = owner.type,
                    executableElement = functions.first(),
                )
            val parsedQuery = parser.process()
            handler(parsedQuery, invocation)
        }
    }

    companion object {
        private const val DAO_PREFIX =
            """
                package foo.bar;
                import androidx.annotation.NonNull;
                import androidx.room3.*;
                import androidx.sqlite.db.SupportSQLiteQuery;
                import androidx.lifecycle.LiveData;
                import java.util.*;
                import com.google.common.collect.*;
                @Dao
                abstract class MyClass {
                """
        const val DAO_PREFIX_KT =
            """
                package foo.bar
                import androidx.room3.*
                import androidx.room3.guava.GuavaDaoReturnTypeConverter
                import androidx.sqlite.db.SupportSQLiteQuery
                import java.util.*
                import io.reactivex.*         
                import io.reactivex.rxjava3.core.*
                import androidx.lifecycle.*
                import com.google.common.util.concurrent.*
                import org.reactivestreams.*
                import kotlinx.coroutines.flow.*
                @DaoReturnTypeConverters(GuavaDaoReturnTypeConverter::class)
                @Dao
                abstract class MyClass {
                """
        private const val DAO_SUFFIX = "}"
    }
}
