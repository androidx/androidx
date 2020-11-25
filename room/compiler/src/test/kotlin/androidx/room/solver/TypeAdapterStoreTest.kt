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

package androidx.room.solver

import COMMON
import androidx.paging.DataSource
import androidx.paging.PagingSource
import androidx.room.Entity
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.asDeclaredType
import androidx.room.ext.GuavaUtilConcurrentTypeNames
import androidx.room.ext.L
import androidx.room.ext.LifecyclesTypeNames
import androidx.room.ext.PagingTypeNames
import androidx.room.ext.ReactiveStreamsTypeNames
import androidx.room.ext.RoomTypeNames.STRING_UTIL
import androidx.room.ext.RxJava2TypeNames
import androidx.room.ext.RxJava3TypeNames
import androidx.room.ext.T
import androidx.room.parser.SQLTypeAffinity
import androidx.room.processor.Context
import androidx.room.processor.ProcessorErrors
import androidx.room.solver.binderprovider.DataSourceFactoryQueryResultBinderProvider
import androidx.room.solver.binderprovider.DataSourceQueryResultBinderProvider
import androidx.room.solver.binderprovider.LiveDataQueryResultBinderProvider
import androidx.room.solver.binderprovider.PagingSourceQueryResultBinderProvider
import androidx.room.solver.binderprovider.RxQueryResultBinderProvider
import androidx.room.solver.shortcut.binderprovider.GuavaListenableFutureDeleteOrUpdateMethodBinderProvider
import androidx.room.solver.shortcut.binderprovider.GuavaListenableFutureInsertMethodBinderProvider
import androidx.room.solver.shortcut.binderprovider.RxCallableDeleteOrUpdateMethodBinderProvider
import androidx.room.solver.shortcut.binderprovider.RxCallableInsertMethodBinderProvider
import androidx.room.solver.types.CompositeAdapter
import androidx.room.solver.types.EnumColumnTypeAdapter
import androidx.room.solver.types.TypeConverter
import androidx.room.testing.TestInvocation
import androidx.room.testing.TestProcessor
import com.google.common.truth.Truth
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubjectFactory
import com.squareup.javapoet.TypeName
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import simpleRun
import testCodeGenScope

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
@RunWith(JUnit4::class)
class TypeAdapterStoreTest {
    companion object {
        fun tmp(index: Int) = CodeGenScope._tmpVar(index)
    }

    @Test
    fun testDirect() {
        singleRun { invocation ->
            val store = TypeAdapterStore.create(Context(invocation.processingEnv))
            val primitiveType = invocation.processingEnv.requireType(TypeName.INT)
            val adapter = store.findColumnTypeAdapter(primitiveType, null)
            assertThat(adapter, notNullValue())
        }.compilesWithoutError()
    }

    @Test
    fun testJavaLangBoolean() {
        singleRun { invocation ->
            val store = TypeAdapterStore.create(Context(invocation.processingEnv))
            val boolean = invocation
                .processingEnv
                .requireType("java.lang.Boolean")
            val adapter = store.findColumnTypeAdapter(boolean, null)
            assertThat(adapter, notNullValue())
            assertThat(adapter, instanceOf(CompositeAdapter::class.java))
            val composite = adapter as CompositeAdapter
            assertThat(
                composite.intoStatementConverter?.from?.typeName,
                `is`(TypeName.BOOLEAN.box())
            )
            assertThat(
                composite.columnTypeAdapter.out.typeName,
                `is`(TypeName.INT.box())
            )
        }.compilesWithoutError()
    }

    @Test
    fun testJavaLangEnumCompilesWithoutError() {
        simpleRun(
            JavaFileObjects.forSourceString(
                "foo.bar.Fruit",
                """ package foo.bar;
                import androidx.room.*;
                enum Fruit {
                    APPLE,
                    BANANA,
                    STRAWBERRY}
                """.trimMargin()
            )
        ) { invocation ->
            val store = TypeAdapterStore.create(Context(invocation.processingEnv))
            val enum = invocation
                .processingEnv
                .requireType("foo.bar.Fruit")
            val adapter = store.findColumnTypeAdapter(enum, null)
            assertThat(adapter, notNullValue())
            assertThat(adapter, instanceOf(EnumColumnTypeAdapter::class.java))
        }.compilesWithoutError()
    }

    @Test
    fun testVia1TypeAdapter() {
        singleRun { invocation ->
            val store = TypeAdapterStore.create(Context(invocation.processingEnv))
            val booleanType = invocation.processingEnv.requireType(TypeName.BOOLEAN)
            val adapter = store.findColumnTypeAdapter(booleanType, null)
            assertThat(adapter, notNullValue())
            assertThat(adapter, instanceOf(CompositeAdapter::class.java))
            val bindScope = testCodeGenScope()
            adapter!!.bindToStmt("stmt", "41", "fooVar", bindScope)
            assertThat(
                bindScope.generate().toString().trim(),
                `is`(
                    """
                    final int ${tmp(0)};
                    ${tmp(0)} = fooVar ? 1 : 0;
                    stmt.bindLong(41, ${tmp(0)});
                    """.trimIndent()
                )
            )

            val cursorScope = testCodeGenScope()
            adapter.readFromCursor("res", "curs", "7", cursorScope)
            assertThat(
                cursorScope.generate().toString().trim(),
                `is`(
                    """
                    final int ${tmp(0)};
                    ${tmp(0)} = curs.getInt(7);
                    res = ${tmp(0)} != 0;
                    """.trimIndent()
                )
            )
        }.compilesWithoutError()
    }

    @Test
    fun testVia2TypeAdapters() {
        singleRun { invocation ->
            val store = TypeAdapterStore.create(
                Context(invocation.processingEnv),
                pointTypeConverters(invocation.processingEnv)
            )
            val pointType = invocation.processingEnv.requireType("foo.bar.Point")
            val adapter = store.findColumnTypeAdapter(pointType, null)
            assertThat(adapter, notNullValue())
            assertThat(adapter, instanceOf(CompositeAdapter::class.java))

            val bindScope = testCodeGenScope()
            adapter!!.bindToStmt("stmt", "41", "fooVar", bindScope)
            assertThat(
                bindScope.generate().toString().trim(),
                `is`(
                    """
                    final int ${tmp(0)};
                    final boolean ${tmp(1)};
                    ${tmp(1)} = foo.bar.Point.toBoolean(fooVar);
                    ${tmp(0)} = ${tmp(1)} ? 1 : 0;
                    stmt.bindLong(41, ${tmp(0)});
                    """.trimIndent()
                )
            )

            val cursorScope = testCodeGenScope()
            adapter.readFromCursor("res", "curs", "11", cursorScope).toString()
            assertThat(
                cursorScope.generate().toString().trim(),
                `is`(
                    """
                    final int ${tmp(0)};
                    ${tmp(0)} = curs.getInt(11);
                    final boolean ${tmp(1)};
                    ${tmp(1)} = ${tmp(0)} != 0;
                    res = foo.bar.Point.fromBoolean(${tmp(1)});
                    """.trimIndent()
                )
            )
        }.compilesWithoutError()
    }

    @Test
    fun testDate() {
        singleRun { (processingEnv) ->
            val store = TypeAdapterStore.create(
                Context(processingEnv),
                dateTypeConverters(processingEnv)
            )
            val tDate = processingEnv.requireType("java.util.Date")
            val adapter = store.findCursorValueReader(tDate, SQLTypeAffinity.INTEGER)
            assertThat(adapter, notNullValue())
            assertThat(adapter?.typeMirror(), `is`(tDate))
            val bindScope = testCodeGenScope()
            adapter!!.readFromCursor("outDate", "curs", "0", bindScope)
            assertThat(
                bindScope.generate().toString().trim(),
                `is`(
                    """
                final java.lang.Long _tmp;
                if (curs.isNull(0)) {
                  _tmp = null;
                } else {
                  _tmp = curs.getLong(0);
                }
                // convert Long to Date;
                    """.trimIndent()
                )
            )
        }.compilesWithoutError()
    }

    @Test
    fun testIntList() {
        singleRun { invocation ->
            val binders = createIntListToStringBinders(invocation)
            val store = TypeAdapterStore.create(
                Context(invocation.processingEnv), binders[0],
                binders[1]
            )

            val adapter = store.findColumnTypeAdapter(binders[0].from, null)
            assertThat(adapter, notNullValue())

            val bindScope = testCodeGenScope()
            adapter!!.bindToStmt("stmt", "41", "fooVar", bindScope)
            assertThat(
                bindScope.generate().toString().trim(),
                `is`(
                    """
                final java.lang.String ${tmp(0)};
                ${tmp(0)} = androidx.room.util.StringUtil.joinIntoString(fooVar);
                if (${tmp(0)} == null) {
                  stmt.bindNull(41);
                } else {
                  stmt.bindString(41, ${tmp(0)});
                }
                    """.trimIndent()
                )
            )

            val converter = store.findTypeConverter(
                binders[0].from,
                invocation.context.COMMON_TYPES.STRING
            )
            assertThat(converter, notNullValue())
            assertThat(store.reverse(converter!!), `is`(binders[1]))
        }.compilesWithoutError()
    }

    @Test
    fun testOneWayConversion() {
        singleRun { invocation ->
            val binders = createIntListToStringBinders(invocation)
            val store = TypeAdapterStore.create(Context(invocation.processingEnv), binders[0])
            val adapter = store.findColumnTypeAdapter(binders[0].from, null)
            assertThat(adapter, nullValue())

            val stmtBinder = store.findStatementValueBinder(binders[0].from, null)
            assertThat(stmtBinder, notNullValue())

            val converter = store.findTypeConverter(
                binders[0].from,
                invocation.context.COMMON_TYPES.STRING
            )
            assertThat(converter, notNullValue())
            assertThat(store.reverse(converter!!), nullValue())
        }
    }

    @Test
    fun testMissingRx2Room() {
        @Suppress("CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS")
        simpleRun(jfos = arrayOf(COMMON.PUBLISHER, COMMON.RX2_FLOWABLE)) { invocation ->
            val publisherElement = invocation.processingEnv
                .requireTypeElement(ReactiveStreamsTypeNames.PUBLISHER)
            assertThat(publisherElement, notNullValue())
            assertThat(
                RxQueryResultBinderProvider.getAll(invocation.context).any {
                    it.matches(publisherElement.asDeclaredType())
                },
                `is`(true)
            )
        }.failsToCompile().withErrorContaining(ProcessorErrors.MISSING_ROOM_RXJAVA2_ARTIFACT)
    }

    @Test
    fun testMissingRx3Room() {
        @Suppress("CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS")
        simpleRun(jfos = arrayOf(COMMON.PUBLISHER, COMMON.RX3_FLOWABLE)) { invocation ->
            val publisherElement = invocation.processingEnv
                .requireTypeElement(ReactiveStreamsTypeNames.PUBLISHER)
            assertThat(publisherElement, notNullValue())
            assertThat(
                RxQueryResultBinderProvider.getAll(invocation.context).any {
                    it.matches(publisherElement.asDeclaredType())
                },
                `is`(true)
            )
        }.failsToCompile().withErrorContaining(ProcessorErrors.MISSING_ROOM_RXJAVA3_ARTIFACT)
    }

    @Test
    fun testFindPublisher() {
        listOf(
            COMMON.RX2_FLOWABLE to COMMON.RX2_ROOM,
            COMMON.RX3_FLOWABLE to COMMON.RX3_ROOM
        ).forEach { (rxTypeSrc, rxRoomSrc) ->
            @Suppress("CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS")
            simpleRun(jfos = arrayOf(COMMON.PUBLISHER, rxTypeSrc, rxRoomSrc)) {
                invocation ->
                val publisher = invocation.processingEnv
                    .requireTypeElement(ReactiveStreamsTypeNames.PUBLISHER)
                assertThat(publisher, notNullValue())
                assertThat(
                    RxQueryResultBinderProvider.getAll(invocation.context).any {
                        it.matches(publisher.asDeclaredType())
                    },
                    `is`(true)
                )
            }.compilesWithoutError()
        }
    }

    @Test
    fun testFindFlowable() {
        listOf(
            Triple(COMMON.RX2_FLOWABLE, COMMON.RX2_ROOM, RxJava2TypeNames.FLOWABLE),
            Triple(COMMON.RX3_FLOWABLE, COMMON.RX3_ROOM, RxJava3TypeNames.FLOWABLE)
        ).forEach { (rxTypeSrc, rxRoomSrc, rxTypeClassName) ->
            @Suppress("CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS")
            simpleRun(jfos = arrayOf(COMMON.PUBLISHER, rxTypeSrc, rxRoomSrc)) {
                invocation ->
                val flowable = invocation.processingEnv.requireTypeElement(rxTypeClassName)
                assertThat(
                    RxQueryResultBinderProvider.getAll(invocation.context).any {
                        it.matches(flowable.asDeclaredType())
                    },
                    `is`(true)
                )
            }.compilesWithoutError()
        }
    }

    @Test
    fun testFindObservable() {
        listOf(
            Triple(COMMON.RX2_OBSERVABLE, COMMON.RX2_ROOM, RxJava2TypeNames.OBSERVABLE),
            Triple(COMMON.RX3_OBSERVABLE, COMMON.RX3_ROOM, RxJava3TypeNames.OBSERVABLE)
        ).forEach { (rxTypeSrc, rxRoomSrc, rxTypeClassName) ->
            @Suppress("CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS")
            simpleRun(jfos = arrayOf(rxTypeSrc, rxRoomSrc)) {
                invocation ->
                val observable = invocation.processingEnv.requireTypeElement(rxTypeClassName)
                assertThat(observable, notNullValue())
                assertThat(
                    RxQueryResultBinderProvider.getAll(invocation.context).any {
                        it.matches(observable.asDeclaredType())
                    },
                    `is`(true)
                )
            }.compilesWithoutError()
        }
    }

    @Test
    fun testFindInsertSingle() {
        listOf(
            Triple(COMMON.RX2_SINGLE, COMMON.RX2_ROOM, RxJava2TypeNames.SINGLE),
            Triple(COMMON.RX3_SINGLE, COMMON.RX3_ROOM, RxJava3TypeNames.SINGLE)
        ).forEach { (rxTypeSrc, _, rxTypeClassName) ->
            @Suppress("CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS")
            simpleRun(jfos = arrayOf(rxTypeSrc)) {
                invocation ->
                val single = invocation.processingEnv.requireTypeElement(rxTypeClassName)
                assertThat(single, notNullValue())
                assertThat(
                    RxCallableInsertMethodBinderProvider.getAll(invocation.context).any {
                        it.matches(single.asDeclaredType())
                    },
                    `is`(true)
                )
            }.compilesWithoutError()
        }
    }

    @Test
    fun testFindInsertMaybe() {
        listOf(
            Triple(COMMON.RX2_MAYBE, COMMON.RX2_ROOM, RxJava2TypeNames.MAYBE),
            Triple(COMMON.RX3_MAYBE, COMMON.RX3_ROOM, RxJava3TypeNames.MAYBE)
        ).forEach { (rxTypeSrc, _, rxTypeClassName) ->
            @Suppress("CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS")
            simpleRun(jfos = arrayOf(rxTypeSrc)) {
                invocation ->
                val maybe = invocation.processingEnv.requireTypeElement(rxTypeClassName)
                assertThat(
                    RxCallableInsertMethodBinderProvider.getAll(invocation.context).any {
                        it.matches(maybe.asDeclaredType())
                    },
                    `is`(true)
                )
            }.compilesWithoutError()
        }
    }

    @Test
    fun testFindInsertCompletable() {
        listOf(
            Triple(COMMON.RX2_COMPLETABLE, COMMON.RX2_ROOM, RxJava2TypeNames.COMPLETABLE),
            Triple(COMMON.RX3_COMPLETABLE, COMMON.RX3_ROOM, RxJava3TypeNames.COMPLETABLE)
        ).forEach { (rxTypeSrc, _, rxTypeClassName) ->
            @Suppress("CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS")
            simpleRun(jfos = arrayOf(rxTypeSrc)) {
                invocation ->
                val completable = invocation.processingEnv.requireTypeElement(rxTypeClassName)
                assertThat(
                    RxCallableInsertMethodBinderProvider.getAll(invocation.context).any {
                        it.matches(completable.asDeclaredType())
                    },
                    `is`(true)
                )
            }.compilesWithoutError()
        }
    }

    @Test
    fun testFindInsertListenableFuture() {
        @Suppress("CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS")
        simpleRun(jfos = arrayOf(COMMON.LISTENABLE_FUTURE)) {
            invocation ->
            val future = invocation.processingEnv
                .requireTypeElement(GuavaUtilConcurrentTypeNames.LISTENABLE_FUTURE)
            assertThat(
                GuavaListenableFutureInsertMethodBinderProvider(invocation.context).matches(
                    future.asDeclaredType()
                ),
                `is`(true)
            )
        }.compilesWithoutError()
    }

    @Test
    fun testFindDeleteOrUpdateSingle() {
        @Suppress("CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS")
        simpleRun(jfos = arrayOf(COMMON.RX2_SINGLE)) {
            invocation ->
            val single = invocation.processingEnv.requireTypeElement(RxJava2TypeNames.SINGLE)
            assertThat(single, notNullValue())
            assertThat(
                RxCallableDeleteOrUpdateMethodBinderProvider.getAll(invocation.context).any {
                    it.matches(single.asDeclaredType())
                },
                `is`(true)
            )
        }.compilesWithoutError()
    }

    @Test
    fun testFindDeleteOrUpdateMaybe() {
        @Suppress("CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS")
        simpleRun(jfos = arrayOf(COMMON.RX2_MAYBE)) {
            invocation ->
            val maybe = invocation.processingEnv.requireTypeElement(RxJava2TypeNames.MAYBE)
            assertThat(maybe, notNullValue())
            assertThat(
                RxCallableDeleteOrUpdateMethodBinderProvider.getAll(invocation.context).any {
                    it.matches(maybe.asDeclaredType())
                },
                `is`(true)
            )
        }.compilesWithoutError()
    }

    @Test
    fun testFindDeleteOrUpdateCompletable() {
        @Suppress("CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS")
        simpleRun(jfos = arrayOf(COMMON.RX2_COMPLETABLE)) {
            invocation ->
            val completable = invocation.processingEnv
                .requireTypeElement(RxJava2TypeNames.COMPLETABLE)
            assertThat(completable, notNullValue())
            assertThat(
                RxCallableDeleteOrUpdateMethodBinderProvider.getAll(invocation.context).any {
                    it.matches(completable.asDeclaredType())
                },
                `is`(true)
            )
        }.compilesWithoutError()
    }

    @Test
    fun testFindDeleteOrUpdateListenableFuture() {
        @Suppress("CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS")
        simpleRun(jfos = arrayOf(COMMON.LISTENABLE_FUTURE)) {
            invocation ->
            val future = invocation.processingEnv
                .requireTypeElement(GuavaUtilConcurrentTypeNames.LISTENABLE_FUTURE)
            assertThat(future, notNullValue())
            assertThat(
                GuavaListenableFutureDeleteOrUpdateMethodBinderProvider(invocation.context)
                    .matches(future.asDeclaredType()),
                `is`(true)
            )
        }.compilesWithoutError()
    }

    @Test
    fun testFindLiveData() {
        @Suppress("CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS")
        simpleRun(jfos = arrayOf(COMMON.COMPUTABLE_LIVE_DATA, COMMON.LIVE_DATA)) {
            invocation ->
            val liveData = invocation.processingEnv
                .requireTypeElement(LifecyclesTypeNames.LIVE_DATA)
            assertThat(liveData, notNullValue())
            assertThat(
                LiveDataQueryResultBinderProvider(invocation.context).matches(
                    liveData.asDeclaredType()
                ),
                `is`(true)
            )
        }.compilesWithoutError()
    }

    @Test
    fun findPagingSourceIntKey() {
        simpleRun { invocation ->
            val pagingSourceElement = invocation.processingEnv
                .requireTypeElement(PagingSource::class)
            val intType = invocation.processingEnv.requireType(Integer::class)
            val pagingSourceIntIntType = invocation.processingEnv
                .getDeclaredType(pagingSourceElement, intType, intType)

            assertThat(pagingSourceIntIntType, notNullValue())
            assertThat(
                PagingSourceQueryResultBinderProvider(invocation.context)
                    .matches(pagingSourceIntIntType.asDeclaredType()),
                `is`(true)
            )
        }
    }

    @Test
    fun findPagingSourceStringKey() {
        simpleRun { invocation ->
            val pagingSourceElement = invocation.processingEnv
                .requireTypeElement(PagingSource::class)
            val stringType = invocation.processingEnv.requireType(String::class)
            val pagingSourceIntIntType = invocation.processingEnv
                .getDeclaredType(pagingSourceElement, stringType, stringType)

            assertThat(pagingSourceIntIntType, notNullValue())
            assertThat(
                PagingSourceQueryResultBinderProvider(invocation.context)
                    .matches(pagingSourceIntIntType.asDeclaredType()),
                `is`(true)
            )
        }.failsToCompile().withErrorContaining(ProcessorErrors.PAGING_SPECIFY_PAGING_SOURCE_TYPE)
    }

    @Test
    fun findDataSource() {
        simpleRun {
            invocation ->
            val dataSource = invocation.processingEnv.requireTypeElement(DataSource::class)
            assertThat(dataSource, notNullValue())
            assertThat(
                DataSourceQueryResultBinderProvider(invocation.context).matches(
                    dataSource.asDeclaredType()
                ),
                `is`(true)
            )
        }.failsToCompile().withErrorContaining(ProcessorErrors.PAGING_SPECIFY_DATA_SOURCE_TYPE)
    }

    @Test
    fun findPositionalDataSource() {
        simpleRun {
            invocation ->
            @Suppress("DEPRECATION")
            val dataSource = invocation.processingEnv
                .requireTypeElement(androidx.paging.PositionalDataSource::class)
            assertThat(dataSource, notNullValue())
            assertThat(
                DataSourceQueryResultBinderProvider(invocation.context).matches(
                    dataSource.asDeclaredType()
                ),
                `is`(true)
            )
        }.compilesWithoutError()
    }

    @Test
    fun findDataSourceFactory() {
        @Suppress("CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS")
        simpleRun(jfos = arrayOf(COMMON.DATA_SOURCE_FACTORY)) {
            invocation ->
            val pagedListProvider = invocation.processingEnv
                .requireTypeElement(PagingTypeNames.DATA_SOURCE_FACTORY)
            assertThat(pagedListProvider, notNullValue())
            assertThat(
                DataSourceFactoryQueryResultBinderProvider(invocation.context).matches(
                    pagedListProvider.asDeclaredType()
                ),
                `is`(true)
            )
        }.compilesWithoutError()
    }

    private fun createIntListToStringBinders(invocation: TestInvocation): List<TypeConverter> {
        val intType = invocation.processingEnv.requireType(Integer::class)
        val listElement = invocation.processingEnv.requireTypeElement(java.util.List::class)
        val listOfInts = invocation.processingEnv.getDeclaredType(listElement, intType)

        val intListConverter = object : TypeConverter(
            listOfInts,
            invocation.context.COMMON_TYPES.STRING
        ) {
            override fun convert(
                inputVarName: String,
                outputVarName: String,
                scope: CodeGenScope
            ) {
                scope.builder().apply {
                    addStatement(
                        "$L = $T.joinIntoString($L)", outputVarName, STRING_UTIL,
                        inputVarName
                    )
                }
            }
        }

        val stringToIntListConverter = object : TypeConverter(
            invocation.context.COMMON_TYPES.STRING, listOfInts
        ) {
            override fun convert(
                inputVarName: String,
                outputVarName: String,
                scope: CodeGenScope
            ) {
                scope.builder().apply {
                    addStatement(
                        "$L = $T.splitToIntList($L)", outputVarName, STRING_UTIL,
                        inputVarName
                    )
                }
            }
        }
        return listOf(intListConverter, stringToIntListConverter)
    }

    fun singleRun(handler: (TestInvocation) -> Unit): CompileTester {
        return Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
            .that(
                listOf(
                    JavaFileObjects.forSourceString(
                        "foo.bar.DummyClass",
                        """
                        package foo.bar;
                        import androidx.room.*;
                        @Entity
                        public class DummyClass {}
                        """
                    ),
                    JavaFileObjects.forSourceString(
                        "foo.bar.Point",
                        """
                        package foo.bar;
                        import androidx.room.*;
                        @Entity
                        public class Point {
                            public int x, y;
                            public Point(int x, int y) {
                                this.x = x;
                                this.y = y;
                            }
                            public static Point fromBoolean(boolean val) {
                                return val ? new Point(1, 1) : new Point(0, 0);
                            }
                            public static boolean toBoolean(Point point) {
                                return point.x > 0;
                            }
                        }
                        """
                    )
                )
            )
            .processedWith(
                TestProcessor.builder()
                    .forAnnotations(Entity::class)
                    .nextRunHandler { invocation ->
                        handler(invocation)
                        true
                    }
                    .build()
            )
    }

    fun pointTypeConverters(env: XProcessingEnv): List<TypeConverter> {
        val tPoint = env.requireType("foo.bar.Point")
        val tBoolean = env.requireType(TypeName.BOOLEAN)
        return listOf(
            object : TypeConverter(tPoint, tBoolean) {
                override fun convert(
                    inputVarName: String,
                    outputVarName: String,
                    scope: CodeGenScope
                ) {
                    scope.builder().apply {
                        addStatement(
                            "$L = $T.toBoolean($L)", outputVarName, from.typeName,
                            inputVarName
                        )
                    }
                }
            },
            object : TypeConverter(tBoolean, tPoint) {
                override fun convert(
                    inputVarName: String,
                    outputVarName: String,
                    scope: CodeGenScope
                ) {
                    scope.builder().apply {
                        addStatement(
                            "$L = $T.fromBoolean($L)", outputVarName, tPoint.typeName,
                            inputVarName
                        )
                    }
                }
            }
        )
    }

    fun dateTypeConverters(env: XProcessingEnv): List<TypeConverter> {
        val tDate = env.requireType("java.util.Date")
        val tLong = env.requireType("java.lang.Long")
        return listOf(
            object : TypeConverter(tDate, tLong) {
                override fun convert(
                    inputVarName: String,
                    outputVarName: String,
                    scope: CodeGenScope
                ) {
                    scope.builder().apply {
                        addStatement("// convert Date to Long")
                    }
                }
            },
            object : TypeConverter(tLong, tDate) {
                override fun convert(
                    inputVarName: String,
                    outputVarName: String,
                    scope: CodeGenScope
                ) {
                    scope.builder().apply {
                        addStatement("// convert Long to Date")
                    }
                }
            }
        )
    }
}
