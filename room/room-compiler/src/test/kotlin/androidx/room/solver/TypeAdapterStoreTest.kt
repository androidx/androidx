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
import androidx.kruth.assertThat
import androidx.paging.DataSource
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.XTypeName.Companion.PRIMITIVE_INT
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.XRawType
import androidx.room.compiler.processing.isTypeElement
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.compileFiles
import androidx.room.compiler.processing.util.runProcessorTest
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.GuavaUtilConcurrentTypeNames
import androidx.room.ext.LifecyclesTypeNames
import androidx.room.ext.PagingTypeNames
import androidx.room.ext.ReactiveStreamsTypeNames
import androidx.room.ext.RoomTypeNames.ROOM_DB
import androidx.room.ext.RoomTypeNames.STRING_UTIL
import androidx.room.ext.RxJava2TypeNames
import androidx.room.ext.RxJava3TypeNames
import androidx.room.ext.implementsEqualsAndHashcode
import androidx.room.parser.SQLTypeAffinity
import androidx.room.processor.Context
import androidx.room.processor.CustomConverterProcessor
import androidx.room.processor.DaoProcessor
import androidx.room.processor.DaoProcessorTest
import androidx.room.processor.ProcessorErrors
import androidx.room.runProcessorTestWithK1
import androidx.room.solver.binderprovider.DataSourceFactoryQueryResultBinderProvider
import androidx.room.solver.binderprovider.DataSourceQueryResultBinderProvider
import androidx.room.solver.binderprovider.ListenableFuturePagingSourceQueryResultBinderProvider
import androidx.room.solver.binderprovider.LiveDataQueryResultBinderProvider
import androidx.room.solver.binderprovider.PagingSourceQueryResultBinderProvider
import androidx.room.solver.binderprovider.RxJava2PagingSourceQueryResultBinderProvider
import androidx.room.solver.binderprovider.RxJava3PagingSourceQueryResultBinderProvider
import androidx.room.solver.binderprovider.RxQueryResultBinderProvider
import androidx.room.solver.query.parameter.CollectionQueryParameterAdapter
import androidx.room.solver.query.result.MultiTypedPagingSourceQueryResultBinder
import androidx.room.solver.shortcut.binderprovider.GuavaListenableFutureDeleteOrUpdateMethodBinderProvider
import androidx.room.solver.shortcut.binderprovider.GuavaListenableFutureInsertOrUpsertMethodBinderProvider
import androidx.room.solver.shortcut.binderprovider.RxCallableDeleteOrUpdateMethodBinderProvider
import androidx.room.solver.shortcut.binderprovider.RxCallableInsertOrUpsertMethodBinderProvider
import androidx.room.solver.types.BoxedPrimitiveColumnTypeAdapter
import androidx.room.solver.types.ByteBufferColumnTypeAdapter
import androidx.room.solver.types.ColumnTypeAdapter
import androidx.room.solver.types.CompositeAdapter
import androidx.room.solver.types.CustomTypeConverterWrapper
import androidx.room.solver.types.EnumColumnTypeAdapter
import androidx.room.solver.types.PrimitiveColumnTypeAdapter
import androidx.room.solver.types.SingleStatementTypeConverter
import androidx.room.solver.types.StringColumnTypeAdapter
import androidx.room.solver.types.TypeConverter
import androidx.room.solver.types.UuidColumnTypeAdapter
import androidx.room.solver.types.ValueClassConverterWrapper
import androidx.room.testing.context
import androidx.room.vo.BuiltInConverterFlags
import androidx.room.vo.ReadQueryMethod
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import testCodeGenScope

@RunWith(JUnit4::class)
class TypeAdapterStoreTest {
    companion object {
        fun tmp(index: Int) = CodeGenScope.getTmpVarString(index)
    }

    @Test
    fun testInvalidNonStaticInnerClass() {
        val converter =
            Source.java(
                "foo.bar.EmptyClass",
                """
            package foo.bar;
            import androidx.room.*;
            public class EmptyClass {
                public enum Color {
                    RED,
                    GREEN
                }
                public class ColorTypeConverter {
                    @TypeConverter
                    public Color fromIntToColorEnum(int colorInt) {
                        if (colorInt == 1) {
                            return Color.RED;
                        } else {
                            return Color.GREEN;
                        }
                    }
                }
            }
            """
                    .trimIndent()
            )
        val entity =
            Source.java(
                "foo.bar.EntityWithOneWayEnum",
                """
            package foo.bar;
            import androidx.room.*;
            @Entity
            @TypeConverters(EmptyClass.ColorTypeConverter.class)
            public class EntityWithOneWayEnum {
                public enum Color {
                    RED,
                    GREEN
                }
                @PrimaryKey public Long id;
                public Color color;
            }
            """
                    .trimIndent()
            )
        runProcessorTestWithK1(sources = listOf(entity, converter)) { invocation ->
            val typeElement =
                invocation.processingEnv.requireTypeElement("foo.bar.EntityWithOneWayEnum")
            val context = Context(invocation.processingEnv)
            CustomConverterProcessor.Companion.findConverters(context, typeElement)
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.INNER_CLASS_TYPE_CONVERTER_MUST_BE_STATIC)
            }
        }
    }

    @Test
    fun testDirect() {
        runProcessorTest { invocation ->
            val store =
                TypeAdapterStore.create(
                    Context(invocation.processingEnv),
                    BuiltInConverterFlags.DEFAULT
                )
            val primitiveType = invocation.processingEnv.requireType(XTypeName.PRIMITIVE_INT)
            val adapter =
                store.findColumnTypeAdapter(primitiveType, null, skipDefaultConverter = false)
            assertThat(adapter, notNullValue())
        }
    }

    @Test
    fun testJavaLangBoolean() {
        runProcessorTest { invocation ->
            val store =
                TypeAdapterStore.create(
                    Context(invocation.processingEnv),
                    BuiltInConverterFlags.DEFAULT
                )
            val boolean = invocation.processingEnv.requireType("java.lang.Boolean").makeNullable()
            val adapter = store.findColumnTypeAdapter(boolean, null, skipDefaultConverter = false)
            assertThat(adapter, notNullValue())
            assertThat(adapter, instanceOf(CompositeAdapter::class.java))
            val composite = adapter as CompositeAdapter
            assertThat(
                composite.intoStatementConverter?.from?.asTypeName(),
                `is`(XTypeName.BOXED_BOOLEAN.copy(nullable = true))
            )
            assertThat(
                composite.columnTypeAdapter.out.asTypeName(),
                `is`(XTypeName.BOXED_INT.copy(nullable = true))
            )
        }
    }

    @Test
    fun testJavaLangEnumCompilesWithoutError() {
        val enumSrc =
            Source.java(
                "foo.bar.Fruit",
                """ package foo.bar;
                import androidx.room.*;
                enum Fruit {
                    APPLE,
                    BANANA,
                    STRAWBERRY}
                """
                    .trimMargin()
            )
        runProcessorTestWithK1(sources = listOf(enumSrc)) { invocation ->
            val store =
                TypeAdapterStore.create(
                    Context(invocation.processingEnv),
                    BuiltInConverterFlags.DEFAULT
                )
            val enum = invocation.processingEnv.requireType("foo.bar.Fruit")
            val adapter = store.findColumnTypeAdapter(enum, null, skipDefaultConverter = false)
            assertThat(adapter, notNullValue())
            assertThat(adapter, instanceOf(EnumColumnTypeAdapter::class.java))
        }
    }

    @Test
    fun testKotlinLangValueClassCompilesWithoutError() {
        val source =
            Source.kotlin(
                "Foo.kt",
                """
            import androidx.room.*
            @JvmInline
            value class IntValueClass(val data: Int)
            @JvmInline
            value class StringValueClass(val data: String)
            class EntityWithValueClass {
                val intData = IntValueClass(123)
                val stringData = StringValueClass("bla")
            }
            """
                    .trimIndent()
            )
        var results: Map<String, String?> = mutableMapOf()

        runProcessorTestWithK1(sources = listOf(source)) { invocation ->
            val typeAdapterStore =
                TypeAdapterStore.create(
                    context = invocation.context,
                    builtInConverterFlags = BuiltInConverterFlags.DEFAULT
                )
            val subject = invocation.processingEnv.requireTypeElement("EntityWithValueClass")
            results =
                subject.getAllFieldsIncludingPrivateSupers().associate { field ->
                    val columnAdapter =
                        typeAdapterStore.findColumnTypeAdapter(
                            out = field.type,
                            affinity = null,
                            false
                        )

                    val typeElementColumnAdapter: ColumnTypeAdapter? =
                        if (columnAdapter is ValueClassConverterWrapper) {
                            columnAdapter.valueTypeColumnAdapter
                        } else {
                            columnAdapter
                        }

                    when (typeElementColumnAdapter) {
                        is PrimitiveColumnTypeAdapter -> {
                            field.name to "primitive"
                        }
                        is StringColumnTypeAdapter -> {
                            field.name to "string"
                        }
                        else -> {
                            field.name to null
                        }
                    }
                }
        }
        assertThat(results)
            .containsExactlyEntriesIn(mapOf("intData" to "primitive", "stringData" to "string"))
    }

    @Test
    fun testValueClassWithDifferentTypeVal() {
        val source =
            Source.kotlin(
                "Foo.kt",
                """
            import androidx.room.*
            @JvmInline
            value class Foo(val value : Int) {
                val double
                    get() = value * 2
            }
            """
                    .trimIndent()
            )

        runProcessorTestWithK1(sources = listOf(source)) { invocation ->
            TypeAdapterStore.create(
                context = invocation.context,
                builtInConverterFlags = BuiltInConverterFlags.DEFAULT
            )
            val typeElement = invocation.processingEnv.requireTypeElement("Foo")
            assertThat(typeElement.getDeclaredFields()).hasSize(1)
            assertThat(typeElement.getDeclaredFields().single().type.asTypeName())
                .isEqualTo(PRIMITIVE_INT)
        }
    }

    @Test
    fun testJavaLangByteBufferCompilesWithoutError() {
        runProcessorTest { invocation ->
            val store =
                TypeAdapterStore.create(
                    Context(invocation.processingEnv),
                    BuiltInConverterFlags.DEFAULT
                )
            val byteBufferType = invocation.processingEnv.requireType(CommonTypeNames.BYTE_BUFFER)
            val adapter =
                store.findColumnTypeAdapter(byteBufferType, null, skipDefaultConverter = false)
            assertThat(adapter, notNullValue())
            assertThat(adapter, instanceOf(ByteBufferColumnTypeAdapter::class.java))
        }
    }

    @Test
    fun testJavaUtilUUIDCompilesWithoutError() {
        runProcessorTest { invocation ->
            val store =
                TypeAdapterStore.create(
                    Context(invocation.processingEnv),
                    BuiltInConverterFlags.DEFAULT
                )
            val uuid = invocation.processingEnv.requireType(CommonTypeNames.UUID)
            val adapter =
                store.findColumnTypeAdapter(
                    out = uuid,
                    affinity = null,
                    skipDefaultConverter = false
                )

            assertThat(adapter).isNotNull()
            assertThat(adapter).isInstanceOf<UuidColumnTypeAdapter>()
        }
    }

    @Test
    fun testVia1TypeAdapter() {
        runProcessorTest { invocation ->
            val store =
                TypeAdapterStore.create(
                    Context(invocation.processingEnv),
                    BuiltInConverterFlags.DEFAULT
                )
            val booleanType = invocation.processingEnv.requireType(XTypeName.PRIMITIVE_BOOLEAN)
            val adapter =
                store.findColumnTypeAdapter(booleanType, null, skipDefaultConverter = false)
            assertThat(adapter, notNullValue())
            assertThat(adapter, instanceOf(CompositeAdapter::class.java))
            val bindScope = testCodeGenScope()
            adapter!!.bindToStmt("stmt", "41", "fooVar", bindScope)
            assertThat(
                bindScope.generate().toString().trim(),
                `is`(
                    """
                    final int ${tmp(0)} = fooVar ? 1 : 0;
                    stmt.bindLong(41, ${tmp(0)});
                    """
                        .trimIndent()
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
                    """
                        .trimIndent()
                )
            )
        }
    }

    @Test
    fun testVia2TypeAdapters() {
        val point =
            Source.java(
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
                @TypeConverter
                public static Point fromBoolean(boolean val) {
                    return val ? new Point(1, 1) : new Point(0, 0);
                }
                @TypeConverter
                public static boolean toBoolean(Point point) {
                    return point.x > 0;
                }
            }
            """
            )
        runProcessorTestWithK1(sources = listOf(point)) { invocation ->
            val context = Context(invocation.processingEnv)
            val converters =
                CustomConverterProcessor(
                        context = context,
                        element = invocation.processingEnv.requireTypeElement("foo.bar.Point")
                    )
                    .process()
                    .map(::CustomTypeConverterWrapper)
            val store = TypeAdapterStore.create(context, BuiltInConverterFlags.DEFAULT, converters)
            val pointType = invocation.processingEnv.requireType("foo.bar.Point")
            val adapter = store.findColumnTypeAdapter(pointType, null, skipDefaultConverter = false)
            assertThat(adapter, notNullValue())
            assertThat(adapter, instanceOf(CompositeAdapter::class.java))

            val bindScope = testCodeGenScope()
            adapter!!.bindToStmt("stmt", "41", "fooVar", bindScope)
            assertThat(
                bindScope.generate().toString().trim(),
                `is`(
                    """
                    final boolean ${tmp(0)} = foo.bar.Point.toBoolean(fooVar);
                    final int ${tmp(1)} = ${tmp(0)} ? 1 : 0;
                    stmt.bindLong(41, ${tmp(1)});
                    """
                        .trimIndent()
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
                    final boolean ${tmp(1)} = ${tmp(0)} != 0;
                    res = foo.bar.Point.fromBoolean(${tmp(1)});
                    """
                        .trimIndent()
                )
            )
        }
    }

    @Test
    fun testDate() {
        runProcessorTest { invocation ->
            val store =
                TypeAdapterStore.create(
                    invocation.context,
                    BuiltInConverterFlags.DEFAULT,
                    dateTypeConverters(invocation.processingEnv)
                )
            val tDate = invocation.processingEnv.requireType("java.util.Date").makeNullable()
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
                outDate = new java.util.Date(_tmp);
                    """
                        .trimIndent()
                )
            )
        }
    }

    @Test
    fun testIntList() {
        runProcessorTest { invocation ->
            val binders = createIntListToStringBinders(invocation)
            val store =
                TypeAdapterStore.create(
                    Context(invocation.processingEnv),
                    BuiltInConverterFlags.DEFAULT,
                    binders[0],
                    binders[1]
                )

            val adapter =
                store.findColumnTypeAdapter(binders[0].from, null, skipDefaultConverter = false)
            assertThat(adapter).isNotNull()

            val bindScope = testCodeGenScope()
            adapter!!.bindToStmt("stmt", "41", "fooVar", bindScope)
            val expectedAdapterCode =
                if (invocation.isKsp) {
                    """
                stmt.bindString(41, ${tmp(0)});
                """
                        .trimIndent()
                } else {
                    """
                if (${tmp(0)} == null) {
                  stmt.bindNull(41);
                } else {
                  stmt.bindString(41, ${tmp(0)});
                }
                """
                        .trimIndent()
                }
            assertThat(bindScope.generate().toString().trim())
                .isEqualTo(
                    """
                |final java.lang.String ${tmp(0)} = androidx.room.util.StringUtil.joinIntoString(fooVar);
                |$expectedAdapterCode
                """
                        .trimMargin()
                )

            val converter =
                store.typeConverterStore.findTypeConverter(
                    binders[0].from,
                    invocation.context.processingEnv.requireType(CommonTypeNames.STRING)
                )
            assertThat(converter).isNotNull()
            assertThat(store.typeConverterStore.reverse(converter!!)).isEqualTo(binders[1])
        }
    }

    @Test
    fun testOneWayConversion() {
        runProcessorTest { invocation ->
            val binders = createIntListToStringBinders(invocation)
            val store =
                TypeAdapterStore.create(
                    Context(invocation.processingEnv),
                    BuiltInConverterFlags.DEFAULT,
                    binders[0]
                )
            val adapter =
                store.findColumnTypeAdapter(binders[0].from, null, skipDefaultConverter = false)
            assertThat(adapter, nullValue())

            val stmtBinder = store.findStatementValueBinder(binders[0].from, null)
            assertThat(stmtBinder, notNullValue())

            val converter =
                store.typeConverterStore.findTypeConverter(
                    binders[0].from,
                    invocation.context.processingEnv.requireType(CommonTypeNames.STRING)
                )
            assertThat(converter, notNullValue())
            assertThat(store.typeConverterStore.reverse(converter!!), nullValue())
        }
    }

    @Test
    fun testMissingRx2Room() {
        runProcessorTestWithK1(sources = listOf(COMMON.PUBLISHER, COMMON.RX2_FLOWABLE)) { invocation
            ->
            val publisherElement =
                invocation.processingEnv.requireTypeElement(ReactiveStreamsTypeNames.PUBLISHER)
            assertThat(publisherElement, notNullValue())
            assertThat(
                RxQueryResultBinderProvider.getAll(invocation.context).any {
                    it.matches(publisherElement.type)
                },
                `is`(true)
            )
            invocation.assertCompilationResult {
                hasError(ProcessorErrors.MISSING_ROOM_RXJAVA2_ARTIFACT)
            }
        }
    }

    @Test
    fun testMissingRx3Room() {
        runProcessorTestWithK1(sources = listOf(COMMON.PUBLISHER, COMMON.RX3_FLOWABLE)) { invocation
            ->
            val publisherElement =
                invocation.processingEnv.requireTypeElement(ReactiveStreamsTypeNames.PUBLISHER)
            assertThat(publisherElement, notNullValue())
            assertThat(
                RxQueryResultBinderProvider.getAll(invocation.context).any {
                    it.matches(publisherElement.type)
                },
                `is`(true)
            )
            invocation.assertCompilationResult {
                hasError(ProcessorErrors.MISSING_ROOM_RXJAVA3_ARTIFACT)
            }
        }
    }

    @Test
    fun testMissingRoomPaging() {
        runProcessorTest { invocation ->
            val pagingSourceElement =
                invocation.processingEnv.requireTypeElement(PagingSource::class)
            val intType = invocation.processingEnv.requireType(Integer::class)
            val pagingSourceIntIntType =
                invocation.processingEnv.getDeclaredType(pagingSourceElement, intType, intType)

            assertThat(pagingSourceIntIntType, notNullValue())
            assertThat(
                PagingSourceQueryResultBinderProvider(invocation.context)
                    .matches(pagingSourceIntIntType),
                `is`(true)
            )
            invocation.assertCompilationResult {
                hasError(ProcessorErrors.MISSING_ROOM_PAGING_ARTIFACT)
            }
        }
    }

    @Test
    fun testMissingRoomPagingGuava() {
        runProcessorTestWithK1(sources = listOf(COMMON.LISTENABLE_FUTURE_PAGING_SOURCE)) {
            invocation ->
            val listenableFuturePagingSourceElement =
                invocation.processingEnv.requireTypeElement(
                    PagingTypeNames.LISTENABLE_FUTURE_PAGING_SOURCE
                )
            val intType = invocation.processingEnv.requireType(Integer::class)
            val listenableFuturePagingSourceIntIntType =
                invocation.processingEnv.getDeclaredType(
                    listenableFuturePagingSourceElement,
                    intType,
                    intType
                )

            assertThat(listenableFuturePagingSourceElement, notNullValue())
            assertThat(
                ListenableFuturePagingSourceQueryResultBinderProvider(invocation.context)
                    .matches(listenableFuturePagingSourceIntIntType),
                `is`(true)
            )
            invocation.assertCompilationResult {
                hasError(ProcessorErrors.MISSING_ROOM_PAGING_GUAVA_ARTIFACT)
            }
        }
    }

    @Test
    fun testMissingRoomPagingRx2() {
        runProcessorTestWithK1(sources = listOf(COMMON.RX2_PAGING_SOURCE)) { invocation ->
            val rx2PagingSourceElement =
                invocation.processingEnv.requireTypeElement(PagingTypeNames.RX2_PAGING_SOURCE)
            val intType = invocation.processingEnv.requireType(Integer::class)
            val rx2PagingSourceIntIntType =
                invocation.processingEnv.getDeclaredType(rx2PagingSourceElement, intType, intType)

            assertThat(rx2PagingSourceElement, notNullValue())
            assertThat(
                RxJava2PagingSourceQueryResultBinderProvider(invocation.context)
                    .matches(rx2PagingSourceIntIntType),
                `is`(true)
            )
            invocation.assertCompilationResult {
                hasError(ProcessorErrors.MISSING_ROOM_PAGING_RXJAVA2_ARTIFACT)
            }
        }
    }

    @Test
    fun testMissingRoomPagingRx3() {
        runProcessorTestWithK1(sources = listOf(COMMON.RX3_PAGING_SOURCE)) { invocation ->
            val rx3PagingSourceElement =
                invocation.processingEnv.requireTypeElement(PagingTypeNames.RX3_PAGING_SOURCE)
            val intType = invocation.processingEnv.requireType(Integer::class)
            val rx3PagingSourceIntIntType =
                invocation.processingEnv.getDeclaredType(rx3PagingSourceElement, intType, intType)

            assertThat(rx3PagingSourceElement, notNullValue())
            assertThat(
                RxJava3PagingSourceQueryResultBinderProvider(invocation.context)
                    .matches(rx3PagingSourceIntIntType),
                `is`(true)
            )
            invocation.assertCompilationResult {
                hasError(ProcessorErrors.MISSING_ROOM_PAGING_RXJAVA3_ARTIFACT)
            }
        }
    }

    @Test
    fun testFindPublisher() {
        listOf(COMMON.RX2_FLOWABLE to COMMON.RX2_ROOM, COMMON.RX3_FLOWABLE to COMMON.RX3_ROOM)
            .forEach { (rxTypeSrc, rxRoomSrc) ->
                runProcessorTestWithK1(
                    sources = listOf(rxTypeSrc, rxRoomSrc),
                    classpath =
                        compileFiles(
                            listOf(
                                COMMON.RX2_SINGLE,
                                COMMON.RX2_MAYBE,
                                COMMON.RX2_COMPLETABLE,
                                COMMON.RX2_OBSERVABLE,
                                COMMON.RX3_SINGLE,
                                COMMON.RX3_MAYBE,
                                COMMON.RX3_COMPLETABLE,
                                COMMON.RX3_OBSERVABLE,
                                COMMON.PUBLISHER,
                            )
                        )
                ) { invocation ->
                    val publisher =
                        invocation.processingEnv.requireTypeElement(
                            ReactiveStreamsTypeNames.PUBLISHER
                        )
                    assertThat(publisher, notNullValue())
                    assertThat(
                        RxQueryResultBinderProvider.getAll(invocation.context).any {
                            it.matches(publisher.type)
                        },
                        `is`(true)
                    )
                }
            }
    }

    @Test
    fun testFindFlowable() {
        listOf(
                Triple(COMMON.RX2_FLOWABLE, COMMON.RX2_ROOM, RxJava2TypeNames.FLOWABLE),
                Triple(COMMON.RX3_FLOWABLE, COMMON.RX3_ROOM, RxJava3TypeNames.FLOWABLE)
            )
            .forEach { (rxTypeSrc, rxRoomSrc, rxTypeClassName) ->
                runProcessorTestWithK1(
                    sources = listOf(rxTypeSrc, rxRoomSrc),
                    classpath =
                        compileFiles(
                            listOf(
                                COMMON.RX2_SINGLE,
                                COMMON.RX2_MAYBE,
                                COMMON.RX2_COMPLETABLE,
                                COMMON.RX2_OBSERVABLE,
                                COMMON.RX3_SINGLE,
                                COMMON.RX3_MAYBE,
                                COMMON.RX3_COMPLETABLE,
                                COMMON.RX3_OBSERVABLE,
                                COMMON.PUBLISHER,
                            )
                        )
                ) { invocation ->
                    val flowable = invocation.processingEnv.requireTypeElement(rxTypeClassName)
                    assertThat(
                        RxQueryResultBinderProvider.getAll(invocation.context).any {
                            it.matches(flowable.type)
                        },
                        `is`(true)
                    )
                }
            }
    }

    @Test
    fun testFindObservable() {
        listOf(
                Triple(COMMON.RX2_OBSERVABLE, COMMON.RX2_ROOM, RxJava2TypeNames.OBSERVABLE),
                Triple(COMMON.RX3_OBSERVABLE, COMMON.RX3_ROOM, RxJava3TypeNames.OBSERVABLE)
            )
            .forEach { (rxTypeSrc, rxRoomSrc, rxTypeClassName) ->
                runProcessorTestWithK1(
                    sources = listOf(rxTypeSrc, rxRoomSrc),
                    classpath =
                        compileFiles(
                            listOf(
                                COMMON.RX2_SINGLE,
                                COMMON.RX2_MAYBE,
                                COMMON.RX2_COMPLETABLE,
                                COMMON.RX2_OBSERVABLE,
                                COMMON.RX2_FLOWABLE,
                                COMMON.RX3_SINGLE,
                                COMMON.RX3_MAYBE,
                                COMMON.RX3_COMPLETABLE,
                                COMMON.RX3_OBSERVABLE,
                                COMMON.RX3_FLOWABLE,
                                COMMON.PUBLISHER,
                            )
                        )
                ) { invocation ->
                    val observable = invocation.processingEnv.requireTypeElement(rxTypeClassName)
                    assertThat(observable, notNullValue())
                    assertThat(
                        RxQueryResultBinderProvider.getAll(invocation.context).any {
                            it.matches(observable.type)
                        },
                        `is`(true)
                    )
                }
            }
    }

    @Test
    fun testFindInsertSingle() {
        listOf(
                Triple(COMMON.RX2_SINGLE, COMMON.RX2_ROOM, RxJava2TypeNames.SINGLE),
                Triple(COMMON.RX3_SINGLE, COMMON.RX3_ROOM, RxJava3TypeNames.SINGLE)
            )
            .forEach { (rxTypeSrc, _, rxTypeClassName) ->
                runProcessorTestWithK1(sources = listOf(rxTypeSrc)) { invocation ->
                    val single = invocation.processingEnv.requireTypeElement(rxTypeClassName)
                    assertThat(single, notNullValue())
                    assertThat(
                        RxCallableInsertOrUpsertMethodBinderProvider.getAll(invocation.context)
                            .any { it.matches(single.type) },
                        `is`(true)
                    )
                }
            }
    }

    @Test
    fun testFindInsertMaybe() {
        listOf(
                Triple(COMMON.RX2_MAYBE, COMMON.RX2_ROOM, RxJava2TypeNames.MAYBE),
                Triple(COMMON.RX3_MAYBE, COMMON.RX3_ROOM, RxJava3TypeNames.MAYBE)
            )
            .forEach { (rxTypeSrc, _, rxTypeClassName) ->
                runProcessorTestWithK1(sources = listOf(rxTypeSrc)) { invocation ->
                    val maybe = invocation.processingEnv.requireTypeElement(rxTypeClassName)
                    assertThat(
                        RxCallableInsertOrUpsertMethodBinderProvider.getAll(invocation.context)
                            .any { it.matches(maybe.type) },
                        `is`(true)
                    )
                }
            }
    }

    @Test
    fun testFindInsertCompletable() {
        listOf(
                Triple(COMMON.RX2_COMPLETABLE, COMMON.RX2_ROOM, RxJava2TypeNames.COMPLETABLE),
                Triple(COMMON.RX3_COMPLETABLE, COMMON.RX3_ROOM, RxJava3TypeNames.COMPLETABLE)
            )
            .forEach { (rxTypeSrc, _, rxTypeClassName) ->
                runProcessorTestWithK1(sources = listOf(rxTypeSrc)) { invocation ->
                    val completable = invocation.processingEnv.requireTypeElement(rxTypeClassName)
                    assertThat(
                        RxCallableInsertOrUpsertMethodBinderProvider.getAll(invocation.context)
                            .any { it.matches(completable.type) },
                        `is`(true)
                    )
                }
            }
    }

    @Test
    fun testFindInsertListenableFuture() {
        runProcessorTestWithK1(sources = listOf(COMMON.LISTENABLE_FUTURE)) { invocation ->
            val future =
                invocation.processingEnv.requireTypeElement(
                    GuavaUtilConcurrentTypeNames.LISTENABLE_FUTURE
                )
            assertThat(
                GuavaListenableFutureInsertOrUpsertMethodBinderProvider(invocation.context)
                    .matches(future.type),
                `is`(true)
            )
        }
    }

    @Test
    fun testFindDeleteOrUpdateSingle() {
        runProcessorTestWithK1(sources = listOf(COMMON.RX2_SINGLE)) { invocation ->
            val single = invocation.processingEnv.requireTypeElement(RxJava2TypeNames.SINGLE)
            assertThat(single, notNullValue())
            assertThat(
                RxCallableDeleteOrUpdateMethodBinderProvider.getAll(invocation.context).any {
                    it.matches(single.type)
                },
                `is`(true)
            )
        }
    }

    @Test
    fun testFindDeleteOrUpdateMaybe() {
        runProcessorTestWithK1(sources = listOf(COMMON.RX2_MAYBE)) { invocation ->
            val maybe = invocation.processingEnv.requireTypeElement(RxJava2TypeNames.MAYBE)
            assertThat(maybe, notNullValue())
            assertThat(
                RxCallableDeleteOrUpdateMethodBinderProvider.getAll(invocation.context).any {
                    it.matches(maybe.type)
                },
                `is`(true)
            )
        }
    }

    @Test
    fun testFindDeleteOrUpdateCompletable() {
        runProcessorTestWithK1(sources = listOf(COMMON.RX2_COMPLETABLE)) { invocation ->
            val completable =
                invocation.processingEnv.requireTypeElement(RxJava2TypeNames.COMPLETABLE)
            assertThat(completable, notNullValue())
            assertThat(
                RxCallableDeleteOrUpdateMethodBinderProvider.getAll(invocation.context).any {
                    it.matches(completable.type)
                },
                `is`(true)
            )
        }
    }

    @Test
    fun testFindDeleteOrUpdateListenableFuture() {
        runProcessorTestWithK1(sources = listOf(COMMON.LISTENABLE_FUTURE)) { invocation ->
            val future =
                invocation.processingEnv.requireTypeElement(
                    GuavaUtilConcurrentTypeNames.LISTENABLE_FUTURE
                )
            assertThat(future, notNullValue())
            assertThat(
                GuavaListenableFutureDeleteOrUpdateMethodBinderProvider(invocation.context)
                    .matches(future.type),
                `is`(true)
            )
        }
    }

    @Test
    fun testFindUpsertSingle() {
        listOf(
                Triple(COMMON.RX2_SINGLE, COMMON.RX2_ROOM, RxJava2TypeNames.SINGLE),
                Triple(COMMON.RX3_SINGLE, COMMON.RX3_ROOM, RxJava3TypeNames.SINGLE)
            )
            .forEach { (rxTypeSrc, _, rxTypeClassName) ->
                runProcessorTestWithK1(sources = listOf(rxTypeSrc)) { invocation ->
                    val single = invocation.processingEnv.requireTypeElement(rxTypeClassName)
                    assertThat(single).isNotNull()
                    assertThat(
                            RxCallableInsertOrUpsertMethodBinderProvider.getAll(invocation.context)
                                .any { it.matches(single.type) }
                        )
                        .isTrue()
                }
            }
    }

    @Test
    fun testFindUpsertMaybe() {
        listOf(
                Triple(COMMON.RX2_MAYBE, COMMON.RX2_ROOM, RxJava2TypeNames.MAYBE),
                Triple(COMMON.RX3_MAYBE, COMMON.RX3_ROOM, RxJava3TypeNames.MAYBE)
            )
            .forEach { (rxTypeSrc, _, rxTypeClassName) ->
                runProcessorTestWithK1(sources = listOf(rxTypeSrc)) { invocation ->
                    val maybe = invocation.processingEnv.requireTypeElement(rxTypeClassName)
                    assertThat(
                            RxCallableInsertOrUpsertMethodBinderProvider.getAll(invocation.context)
                                .any { it.matches(maybe.type) }
                        )
                        .isTrue()
                }
            }
    }

    @Test
    fun testFindUpsertCompletable() {
        listOf(
                Triple(COMMON.RX2_COMPLETABLE, COMMON.RX2_ROOM, RxJava2TypeNames.COMPLETABLE),
                Triple(COMMON.RX3_COMPLETABLE, COMMON.RX3_ROOM, RxJava3TypeNames.COMPLETABLE)
            )
            .forEach { (rxTypeSrc, _, rxTypeClassName) ->
                runProcessorTestWithK1(sources = listOf(rxTypeSrc)) { invocation ->
                    val completable = invocation.processingEnv.requireTypeElement(rxTypeClassName)
                    assertThat(
                            RxCallableInsertOrUpsertMethodBinderProvider.getAll(invocation.context)
                                .any { it.matches(completable.type) }
                        )
                        .isTrue()
                }
            }
    }

    @Test
    fun testFindUpsertListenableFuture() {
        runProcessorTestWithK1(sources = listOf(COMMON.LISTENABLE_FUTURE)) { invocation ->
            val future =
                invocation.processingEnv.requireTypeElement(
                    GuavaUtilConcurrentTypeNames.LISTENABLE_FUTURE
                )
            assertThat(
                    GuavaListenableFutureInsertOrUpsertMethodBinderProvider(invocation.context)
                        .matches(future.type)
                )
                .isTrue()
        }
    }

    @Test
    fun testFindLiveData() {
        runProcessorTestWithK1(sources = listOf(COMMON.COMPUTABLE_LIVE_DATA, COMMON.LIVE_DATA)) {
            invocation ->
            val liveData =
                invocation.processingEnv.requireTypeElement(LifecyclesTypeNames.LIVE_DATA)
            assertThat(liveData, notNullValue())
            assertThat(
                LiveDataQueryResultBinderProvider(invocation.context).matches(liveData.type),
                `is`(true)
            )
        }
    }

    @Test
    fun findPagingSourceIntKey() {
        runProcessorTestWithK1(
            sources = listOf(COMMON.LIMIT_OFFSET_PAGING_SOURCE),
        ) { invocation ->
            val pagingSourceElement =
                invocation.processingEnv.requireTypeElement(PagingSource::class)
            val intType = invocation.processingEnv.requireType(Integer::class)
            val pagingSourceIntIntType =
                invocation.processingEnv.getDeclaredType(pagingSourceElement, intType, intType)

            assertThat(pagingSourceIntIntType, notNullValue())
            assertThat(
                PagingSourceQueryResultBinderProvider(invocation.context)
                    .matches(pagingSourceIntIntType),
                `is`(true)
            )
        }
    }

    @Test
    fun findPagingSourceStringKey() {
        runProcessorTest { invocation ->
            val pagingSourceElement =
                invocation.processingEnv.requireTypeElement(PagingSource::class)
            val stringType = invocation.processingEnv.requireType(String::class)
            val pagingSourceIntIntType =
                invocation.processingEnv.getDeclaredType(
                    pagingSourceElement,
                    stringType,
                    stringType
                )

            assertThat(pagingSourceIntIntType, notNullValue())
            assertThat(
                PagingSourceQueryResultBinderProvider(invocation.context)
                    .matches(pagingSourceIntIntType),
                `is`(true)
            )
            invocation.assertCompilationResult {
                hasError(ProcessorErrors.PAGING_SPECIFY_PAGING_SOURCE_TYPE)
            }
        }
    }

    @Test
    fun findPagingSourceJavaCollectionValue() {
        runProcessorTest { invocation ->
            val pagingSourceElement =
                invocation.processingEnv.requireTypeElement(PagingSource::class)
            val intType = invocation.processingEnv.requireType(Integer::class)
            val collectionType = invocation.processingEnv.requireType("java.util.Collection")
            val pagingSourceIntCollectionType =
                invocation.processingEnv.getDeclaredType(
                    pagingSourceElement,
                    intType,
                    collectionType
                )

            assertThat(pagingSourceIntCollectionType).isNotNull()
            assertThat(
                    PagingSourceQueryResultBinderProvider(invocation.context)
                        .matches(pagingSourceIntCollectionType)
                )
                .isTrue()
            invocation.assertCompilationResult {
                hasError(ProcessorErrors.PAGING_SPECIFY_PAGING_SOURCE_VALUE_TYPE)
            }
        }
    }

    @Test
    fun findPagingSourceKotlinCollectionValue() {
        runProcessorTest { invocation ->
            val pagingSourceElement =
                invocation.processingEnv.requireTypeElement(PagingSource::class)
            val intType = invocation.processingEnv.requireType(Integer::class)
            val kotlinCollectionType = invocation.processingEnv.requireType(Collection::class)
            val pagingSourceIntCollectionType =
                invocation.processingEnv.getDeclaredType(
                    pagingSourceElement,
                    intType,
                    kotlinCollectionType
                )

            assertThat(pagingSourceIntCollectionType).isNotNull()
            assertThat(
                    PagingSourceQueryResultBinderProvider(invocation.context)
                        .matches(pagingSourceIntCollectionType)
                )
                .isTrue()
            invocation.assertCompilationResult {
                hasError(ProcessorErrors.PAGING_SPECIFY_PAGING_SOURCE_VALUE_TYPE)
            }
        }
    }

    @Test
    fun findPagingSourceJavaListValue() {
        runProcessorTest { invocation ->
            val pagingSourceElement =
                invocation.processingEnv.requireTypeElement(PagingSource::class)
            val intType = invocation.processingEnv.requireType(Integer::class)
            val javaListType = invocation.processingEnv.requireType("java.util.List")
            val pagingSourceIntListType =
                invocation.processingEnv.getDeclaredType(pagingSourceElement, intType, javaListType)
            assertThat(pagingSourceIntListType).isNotNull()
            assertThat(
                    PagingSourceQueryResultBinderProvider(invocation.context)
                        .matches(pagingSourceIntListType)
                )
                .isTrue()
            invocation.assertCompilationResult {
                hasError(ProcessorErrors.PAGING_SPECIFY_PAGING_SOURCE_VALUE_TYPE)
            }
        }
    }

    @Test
    fun findPagingSourceKotlinMutableSetValue() {
        runProcessorTest { invocation ->
            val pagingSourceElement =
                invocation.processingEnv.requireTypeElement(PagingSource::class)
            val intType = invocation.processingEnv.requireType(Integer::class)
            val mutableSetType = invocation.processingEnv.requireType(MutableSet::class)
            val pagingSourceIntCollectionType =
                invocation.processingEnv.getDeclaredType(
                    pagingSourceElement,
                    intType,
                    mutableSetType
                )

            assertThat(pagingSourceIntCollectionType).isNotNull()
            assertThat(
                    PagingSourceQueryResultBinderProvider(invocation.context)
                        .matches(pagingSourceIntCollectionType)
                )
                .isTrue()
            invocation.assertCompilationResult {
                hasError(ProcessorErrors.PAGING_SPECIFY_PAGING_SOURCE_VALUE_TYPE)
            }
        }
    }

    @Test
    fun findListenableFuturePagingSourceJavaCollectionValue() {
        runProcessorTestWithK1(sources = listOf(COMMON.LISTENABLE_FUTURE_PAGING_SOURCE)) {
            invocation ->
            val listenableFuturePagingSourceElement =
                invocation.processingEnv.requireTypeElement(
                    PagingTypeNames.LISTENABLE_FUTURE_PAGING_SOURCE
                )
            val intType = invocation.processingEnv.requireType(Integer::class)
            val collectionType = invocation.processingEnv.requireType("java.util.Collection")
            val listenableFuturePagingSourceIntCollectionType =
                invocation.processingEnv.getDeclaredType(
                    listenableFuturePagingSourceElement,
                    intType,
                    collectionType
                )

            assertThat(listenableFuturePagingSourceIntCollectionType).isNotNull()
            assertThat(
                    ListenableFuturePagingSourceQueryResultBinderProvider(invocation.context)
                        .matches(listenableFuturePagingSourceIntCollectionType)
                )
                .isTrue()
            invocation.assertCompilationResult {
                hasError(ProcessorErrors.PAGING_SPECIFY_PAGING_SOURCE_VALUE_TYPE)
            }
        }
    }

    @Test
    fun findListenableFutureKotlinCollectionValue() {
        runProcessorTestWithK1(sources = listOf(COMMON.LISTENABLE_FUTURE_PAGING_SOURCE)) {
            invocation ->
            val listenableFuturePagingSourceElement =
                invocation.processingEnv.requireTypeElement(
                    PagingTypeNames.LISTENABLE_FUTURE_PAGING_SOURCE
                )
            val intType = invocation.processingEnv.requireType(Integer::class)
            val kotlinCollectionType = invocation.processingEnv.requireType(Collection::class)
            val listenableFuturePagingSourceIntCollectionType =
                invocation.processingEnv.getDeclaredType(
                    listenableFuturePagingSourceElement,
                    intType,
                    kotlinCollectionType
                )

            assertThat(listenableFuturePagingSourceIntCollectionType).isNotNull()
            assertThat(
                    ListenableFuturePagingSourceQueryResultBinderProvider(invocation.context)
                        .matches(listenableFuturePagingSourceIntCollectionType)
                )
                .isTrue()
            invocation.assertCompilationResult {
                hasError(ProcessorErrors.PAGING_SPECIFY_PAGING_SOURCE_VALUE_TYPE)
            }
        }
    }

    @Test
    fun findRx2PagingSourceJavaCollectionValue() {
        runProcessorTestWithK1(sources = listOf(COMMON.RX2_PAGING_SOURCE)) { invocation ->
            val rx2PagingSourceElement =
                invocation.processingEnv.requireTypeElement(PagingTypeNames.RX2_PAGING_SOURCE)
            val intType = invocation.processingEnv.requireType(Integer::class)
            val collectionType = invocation.processingEnv.requireType("java.util.Collection")
            val rx2PagingSourceIntCollectionType =
                invocation.processingEnv.getDeclaredType(
                    rx2PagingSourceElement,
                    intType,
                    collectionType
                )

            assertThat(rx2PagingSourceElement).isNotNull()
            assertThat(
                    RxJava2PagingSourceQueryResultBinderProvider(invocation.context)
                        .matches(rx2PagingSourceIntCollectionType)
                )
                .isTrue()
            invocation.assertCompilationResult {
                hasError(ProcessorErrors.PAGING_SPECIFY_PAGING_SOURCE_VALUE_TYPE)
            }
        }
    }

    @Test
    fun findRx2PagingSourceKotlinCollectionValue() {
        runProcessorTestWithK1(sources = listOf(COMMON.RX2_PAGING_SOURCE)) { invocation ->
            val rx2PagingSourceElement =
                invocation.processingEnv.requireTypeElement(PagingTypeNames.RX2_PAGING_SOURCE)
            val intType = invocation.processingEnv.requireType(Integer::class)
            val kotlinCollectionType = invocation.processingEnv.requireType(Collection::class)
            val rx2PagingSourceIntCollectionType =
                invocation.processingEnv.getDeclaredType(
                    rx2PagingSourceElement,
                    intType,
                    kotlinCollectionType
                )

            assertThat(rx2PagingSourceElement).isNotNull()
            assertThat(
                    RxJava2PagingSourceQueryResultBinderProvider(invocation.context)
                        .matches(rx2PagingSourceIntCollectionType)
                )
                .isTrue()
            invocation.assertCompilationResult {
                hasError(ProcessorErrors.PAGING_SPECIFY_PAGING_SOURCE_VALUE_TYPE)
            }
        }
    }

    @Test
    fun findRx3PagingSourceJavaCollectionValue() {
        runProcessorTestWithK1(sources = listOf(COMMON.RX3_PAGING_SOURCE)) { invocation ->
            val rx3PagingSourceElement =
                invocation.processingEnv.requireTypeElement(PagingTypeNames.RX3_PAGING_SOURCE)
            val intType = invocation.processingEnv.requireType(Integer::class)
            val collectionType = invocation.processingEnv.requireType("java.util.Collection")
            val rx3PagingSourceIntCollectionType =
                invocation.processingEnv.getDeclaredType(
                    rx3PagingSourceElement,
                    intType,
                    collectionType
                )

            assertThat(rx3PagingSourceElement).isNotNull()
            assertThat(
                    RxJava3PagingSourceQueryResultBinderProvider(invocation.context)
                        .matches(rx3PagingSourceIntCollectionType)
                )
                .isTrue()
            invocation.assertCompilationResult {
                hasError(ProcessorErrors.PAGING_SPECIFY_PAGING_SOURCE_VALUE_TYPE)
            }
        }
    }

    @Test
    fun findRx3PagingSourceKotlinCollectionValue() {
        runProcessorTestWithK1(sources = listOf(COMMON.RX3_PAGING_SOURCE)) { invocation ->
            val rx3PagingSourceElement =
                invocation.processingEnv.requireTypeElement(PagingTypeNames.RX3_PAGING_SOURCE)
            val intType = invocation.processingEnv.requireType(Integer::class)
            val kotlinCollectionType = invocation.processingEnv.requireType(Collection::class)
            val rx3PagingSourceIntCollectionType =
                invocation.processingEnv.getDeclaredType(
                    rx3PagingSourceElement,
                    intType,
                    kotlinCollectionType
                )

            assertThat(rx3PagingSourceElement).isNotNull()
            assertThat(
                    RxJava3PagingSourceQueryResultBinderProvider(invocation.context)
                        .matches(rx3PagingSourceIntCollectionType)
                )
                .isTrue()
            invocation.assertCompilationResult {
                hasError(ProcessorErrors.PAGING_SPECIFY_PAGING_SOURCE_VALUE_TYPE)
            }
        }
    }

    @Test
    fun testPagingSourceBinder() {
        val inputSource =
            Source.java(
                qName = "foo.bar.MyDao",
                code =
                    """
                ${DaoProcessorTest.DAO_PREFIX}

                @Dao abstract class MyDao {
                    @Query("SELECT uid FROM User")
                    abstract androidx.paging.PagingSource<Integer, User> getAllIds();
                }
                    """
                        .trimIndent()
            )
        runProcessorTestWithK1(
            sources =
                listOf(
                    inputSource,
                    COMMON.USER,
                    COMMON.PAGING_SOURCE,
                    COMMON.LIMIT_OFFSET_PAGING_SOURCE,
                    COMMON.LISTENABLE_FUTURE_PAGING_SOURCE,
                ),
        ) { invocation: XTestInvocation ->
            val dao =
                invocation.roundEnv.getElementsAnnotatedWith(Dao::class.qualifiedName!!).first()
            check(dao.isTypeElement())
            val dbType = invocation.context.processingEnv.requireType(ROOM_DB)
            val parser =
                DaoProcessor(
                    invocation.context,
                    dao,
                    dbType,
                    null,
                )
            val parsedDao = parser.process()
            val binder =
                parsedDao.queryMethods.filterIsInstance<ReadQueryMethod>().first().queryResultBinder
            assertThat(binder is MultiTypedPagingSourceQueryResultBinder).isTrue()

            val pagingSourceXRawType: XRawType? =
                invocation.context.processingEnv
                    .findType(PagingTypeNames.PAGING_SOURCE.canonicalName)
                    ?.rawType
            val returnedXRawType =
                parsedDao.queryMethods
                    .filterIsInstance<ReadQueryMethod>()
                    .first()
                    .returnType
                    .rawType
            // make sure returned type is the original PagingSource
            assertThat(returnedXRawType).isEqualTo(pagingSourceXRawType)

            val listenableFuturePagingSourceXRawType: XRawType? =
                invocation.context.processingEnv
                    .findType(PagingTypeNames.LISTENABLE_FUTURE_PAGING_SOURCE.canonicalName)
                    ?.rawType
            assertThat(listenableFuturePagingSourceXRawType!!.isAssignableFrom(returnedXRawType))
                .isFalse()
        }
    }

    @Test
    fun testListenableFuturePagingSourceBinder() {
        val inputSource =
            Source.java(
                qName = "foo.bar.MyDao",
                code =
                    """
                ${DaoProcessorTest.DAO_PREFIX}

                @Dao abstract class MyDao {
                    @Query("SELECT uid FROM User")
                    abstract androidx.paging.ListenableFuturePagingSource<Integer, User> getAllIds();
                }
                    """
                        .trimIndent()
            )
        runProcessorTestWithK1(
            sources =
                listOf(
                    inputSource,
                    COMMON.USER,
                    COMMON.LISTENABLE_FUTURE_PAGING_SOURCE,
                    COMMON.LIMIT_OFFSET_LISTENABLE_FUTURE_PAGING_SOURCE,
                ),
        ) { invocation: XTestInvocation ->
            val dao =
                invocation.roundEnv.getElementsAnnotatedWith(Dao::class.qualifiedName!!).first()
            check(dao.isTypeElement())
            val dbType = invocation.context.processingEnv.requireType(ROOM_DB)
            val parser =
                DaoProcessor(
                    invocation.context,
                    dao,
                    dbType,
                    null,
                )
            val parsedDao = parser.process()
            val binder =
                parsedDao.queryMethods.filterIsInstance<ReadQueryMethod>().first().queryResultBinder

            // assert that room correctly binds to ListenableFuturePagingSource instead of
            // its supertype PagingSource. ListenableFuturePagingSourceBinderProvider
            // must be added into list of binder providers in TypeAdapterStore before
            // generic PagingSource.
            assertThat(binder is MultiTypedPagingSourceQueryResultBinder).isTrue()
            val listenableFuturePagingSourceXRawType: XRawType? =
                invocation.context.processingEnv
                    .findType(PagingTypeNames.LISTENABLE_FUTURE_PAGING_SOURCE.canonicalName)
                    ?.rawType
            val returnedXRawType =
                parsedDao.queryMethods
                    .filterIsInstance<ReadQueryMethod>()
                    .first()
                    .returnType
                    .rawType
            // make sure the actual returned type from Provider is ListenableFuturePagingSource
            assertThat(returnedXRawType).isEqualTo(listenableFuturePagingSourceXRawType)
        }
    }

    @Test
    fun testRx2PagingSourceBinder() {
        val inputSource =
            Source.java(
                qName = "foo.bar.MyDao",
                code =
                    """
                ${DaoProcessorTest.DAO_PREFIX}

                @Dao abstract class MyDao {
                    @Query("SELECT uid FROM User")
                    abstract androidx.paging.rxjava2.RxPagingSource<Integer, User> getAllIds();
                }
                    """
                        .trimIndent()
            )
        runProcessorTestWithK1(
            sources =
                listOf(
                    inputSource,
                    COMMON.USER,
                    COMMON.RX2_PAGING_SOURCE,
                    COMMON.LIMIT_OFFSET_RX2_PAGING_SOURCE,
                ),
        ) { invocation: XTestInvocation ->
            val dao =
                invocation.roundEnv.getElementsAnnotatedWith(Dao::class.qualifiedName!!).first()
            check(dao.isTypeElement())
            val dbType = invocation.context.processingEnv.requireType(ROOM_DB)
            val parser =
                DaoProcessor(
                    invocation.context,
                    dao,
                    dbType,
                    null,
                )
            val parsedDao = parser.process()
            val binder =
                parsedDao.queryMethods.filterIsInstance<ReadQueryMethod>().first().queryResultBinder

            assertThat(binder is MultiTypedPagingSourceQueryResultBinder).isTrue()
            val rxPagingSourceXRawType: XRawType? =
                invocation.context.processingEnv
                    .findType(PagingTypeNames.RX2_PAGING_SOURCE.canonicalName)
                    ?.rawType
            val returnedXRawType =
                parsedDao.queryMethods
                    .filterIsInstance<ReadQueryMethod>()
                    .first()
                    .returnType
                    .rawType
            // make sure the actual returned type from Provider is a Rx2PagingSource
            assertThat(returnedXRawType).isEqualTo(rxPagingSourceXRawType)
        }
    }

    @Test
    fun testRx3PagingSourceBinder() {
        val inputSource =
            Source.java(
                qName = "foo.bar.MyDao",
                code =
                    """
                ${DaoProcessorTest.DAO_PREFIX}

                @Dao abstract class MyDao {
                    @Query("SELECT uid FROM User")
                    abstract androidx.paging.rxjava3.RxPagingSource<Integer, User> getAllIds();
                }
                    """
                        .trimIndent()
            )
        runProcessorTestWithK1(
            sources =
                listOf(
                    inputSource,
                    COMMON.USER,
                    COMMON.RX3_PAGING_SOURCE,
                    COMMON.LIMIT_OFFSET_RX3_PAGING_SOURCE,
                ),
        ) { invocation: XTestInvocation ->
            val dao =
                invocation.roundEnv.getElementsAnnotatedWith(Dao::class.qualifiedName!!).first()
            check(dao.isTypeElement())
            val dbType = invocation.context.processingEnv.requireType(ROOM_DB)
            val parser =
                DaoProcessor(
                    invocation.context,
                    dao,
                    dbType,
                    null,
                )
            val parsedDao = parser.process()
            val binder =
                parsedDao.queryMethods.filterIsInstance<ReadQueryMethod>().first().queryResultBinder

            assertThat(binder is MultiTypedPagingSourceQueryResultBinder).isTrue()
            val rxPagingSourceXRawType: XRawType? =
                invocation.context.processingEnv
                    .findType(PagingTypeNames.RX3_PAGING_SOURCE.canonicalName)
                    ?.rawType
            val returnedXRawType =
                parsedDao.queryMethods
                    .filterIsInstance<ReadQueryMethod>()
                    .first()
                    .returnType
                    .rawType
            // make sure the actual returned type from Provider is a RxPagingSource
            assertThat(returnedXRawType).isEqualTo(rxPagingSourceXRawType)
        }
    }

    @Test
    fun findDataSource() {
        runProcessorTest { invocation ->
            val dataSource = invocation.processingEnv.requireTypeElement(DataSource::class)
            assertThat(dataSource, notNullValue())
            assertThat(
                DataSourceQueryResultBinderProvider(invocation.context).matches(dataSource.type),
                `is`(true)
            )
            invocation.assertCompilationResult {
                hasError(ProcessorErrors.PAGING_SPECIFY_DATA_SOURCE_TYPE)
            }
        }
    }

    @Test
    fun findPositionalDataSource() {
        runProcessorTest { invocation ->
            @Suppress("DEPRECATION")
            val dataSource =
                invocation.processingEnv.requireTypeElement(
                    androidx.paging.PositionalDataSource::class
                )
            assertThat(dataSource, notNullValue())
            assertThat(
                DataSourceQueryResultBinderProvider(invocation.context).matches(dataSource.type),
                `is`(true)
            )
        }
    }

    @Test
    fun findDataSourceFactory() {
        runProcessorTestWithK1(sources = listOf(COMMON.DATA_SOURCE_FACTORY)) { invocation ->
            val pagedListProvider =
                invocation.processingEnv.requireTypeElement(PagingTypeNames.DATA_SOURCE_FACTORY)
            assertThat(pagedListProvider, notNullValue())
            assertThat(
                DataSourceFactoryQueryResultBinderProvider(invocation.context)
                    .matches(pagedListProvider.type),
                `is`(true)
            )
        }
    }

    @Test
    fun findQueryParameterAdapter_collections() {
        runProcessorTest { invocation ->
            val store =
                TypeAdapterStore.create(
                    context = invocation.context,
                    builtInConverterFlags = BuiltInConverterFlags.DEFAULT
                )
            val javacCollectionTypes =
                listOf("java.util.Set", "java.util.List", "java.util.ArrayList")
            val kotlinCollectionTypes =
                listOf("kotlin.collections.List", "kotlin.collections.MutableList")
            val collectionTypes =
                if (invocation.isKsp) {
                    javacCollectionTypes + kotlinCollectionTypes
                } else {
                    javacCollectionTypes
                }
            collectionTypes
                .map { collectionType ->
                    invocation.processingEnv.getDeclaredType(
                        invocation.processingEnv.requireTypeElement(collectionType),
                        invocation.processingEnv.requireType(XTypeName.PRIMITIVE_INT).boxed()
                    )
                }
                .forEach { type ->
                    val adapter =
                        store.findQueryParameterAdapter(
                            typeMirror = type,
                            isMultipleParameter = true
                        )
                    assertThat(adapter).isNotNull()
                    assertThat(adapter).isInstanceOf<CollectionQueryParameterAdapter>()
                }
        }
    }

    @Test
    fun typeAliases() {
        val source =
            Source.kotlin(
                "Foo.kt",
                """
            import androidx.room.*
            typealias MyLongAlias = Long
            typealias MyNullableLongAlias = Long?

            data class MyClass(val foo:String)
            typealias MyClassAlias = MyClass
            typealias MyClassNullableAlias = MyClass?

            object MyConverters {
                @TypeConverter
                fun myClassToString(myClass : MyClass): String = TODO()
                @TypeConverter
                fun nullableMyClassToString(myClass : MyClass?): String? = TODO()
            }
            class Subject {
                val myLongAlias : MyLongAlias = TODO()
                val myLongAlias_nullable : MyLongAlias? = TODO()
                val myNullableLongAlias : MyNullableLongAlias = TODO()
                val myNullableLongAlias_nullable : MyNullableLongAlias? = TODO()
                val myClass : MyClass = TODO()
                val myClassAlias : MyClassAlias = TODO()
                val myClassAlias_nullable : MyClassAlias? = TODO()
                val myClassNullableAlias : MyClassNullableAlias = TODO()
                val myClassNullableAlias_nullable : MyClassNullableAlias = TODO()
            }
            """
                    .trimIndent()
            )
        runProcessorTestWithK1(sources = listOf(source)) { invocation ->
            val converters =
                CustomConverterProcessor(
                        context = invocation.context,
                        element = invocation.processingEnv.requireTypeElement("MyConverters")
                    )
                    .process()
                    .map(::CustomTypeConverterWrapper)
            val typeAdapterStore =
                TypeAdapterStore.create(
                    context = invocation.context,
                    builtInConverterFlags = BuiltInConverterFlags.DEFAULT,
                    extras = converters.toTypedArray()
                )
            val subject = invocation.processingEnv.requireTypeElement("Subject")
            val results =
                subject.getAllFieldsIncludingPrivateSupers().associate { field ->
                    val binder =
                        typeAdapterStore.findStatementValueBinder(
                            input = field.type,
                            affinity = null
                        )

                    val signature =
                        when (binder) {
                            null -> null
                            is PrimitiveColumnTypeAdapter -> "primitive"
                            is BoxedPrimitiveColumnTypeAdapter -> "boxed"
                            is CompositeAdapter -> {
                                when (val converter = binder.intoStatementConverter) {
                                    null -> "composite null"
                                    is CustomTypeConverterWrapper -> converter.custom.method.name
                                    else -> "composite unknown"
                                }
                            }
                            else -> "unknown"
                        }
                    field.name to signature
                }
            // see: 187359339. We use nullability for assignments only in KSP
            val nullableClassAdapter =
                if (invocation.isKsp) {
                    "nullableMyClassToString"
                } else {
                    "myClassToString"
                }
            assertThat(results)
                .containsExactlyEntriesIn(
                    mapOf(
                        "myLongAlias" to "primitive",
                        "myLongAlias_nullable" to "boxed",
                        "myNullableLongAlias" to "boxed",
                        "myNullableLongAlias_nullable" to "boxed",
                        "myClass" to "myClassToString",
                        "myClassAlias" to "myClassToString",
                        "myClassAlias_nullable" to nullableClassAdapter,
                        "myClassNullableAlias" to nullableClassAdapter,
                        "myClassNullableAlias_nullable" to nullableClassAdapter,
                    )
                )
        }
    }

    @Test
    fun testEqualsAndHashcodeImplemented() {
        val classExtendsClassWithEqualsAndHashcodeFunctions =
            Source.java(
                "foo.bar.Human",
                """
            package foo.bar;
            public class Human extends Username {
                public String relationId;
            }
            """
                    .trimIndent()
            )
        val classWithFncs =
            Source.java(
                "foo.bar.Username",
                """
            package foo.bar;
            public class Username extends Person {
                public String name;
                @Override
                public boolean equals(Object o) {
                    return false;
                }
                @Override
                public int hashCode() {
                    return 0;
                }
            }
            """
                    .trimIndent()
            )
        val classWithoutFncs =
            Source.java(
                "foo.bar.Person",
                """
            package foo.bar;
            public class Person {
                public String userId;
            }
            """
                    .trimIndent()
            )
        val enumClass =
            Source.java(
                "foo.bar.Names",
                """
            package foo.bar;
            public enum Names {
                ELLA,
                BOB,
                JAMES
            }
            """
                    .trimIndent()
            )
        val classWithWrongFncs =
            Source.java(
                "foo.bar.UsernameWithWrongFncs",
                """
            package foo.bar;
            public class UsernameWithWrongFncs {
                public String name;
                public boolean equals() {
                    return true;
                }
                public int hashCode(int num) {
                    return num;
                }
            }
            """
                    .trimIndent()
            )
        runProcessorTestWithK1(
            sources =
                listOf(
                    classExtendsClassWithEqualsAndHashcodeFunctions,
                    classWithFncs,
                    classWithoutFncs,
                    enumClass,
                    classWithWrongFncs
                )
        ) { invocation ->
            val enumCase = invocation.processingEnv.requireTypeElement("foo.bar.Names")
            val inheritedCase = invocation.processingEnv.requireTypeElement("foo.bar.Human")
            val wrongFunctionsCase =
                invocation.processingEnv.requireTypeElement("foo.bar.UsernameWithWrongFncs")
            val noEqualsOrHashcodeCase =
                invocation.processingEnv.requireTypeElement("foo.bar.Person")
            assertThat(enumCase.type.implementsEqualsAndHashcode()).isTrue()
            assertThat(inheritedCase.type.implementsEqualsAndHashcode()).isTrue()
            assertThat(wrongFunctionsCase.type.implementsEqualsAndHashcode()).isFalse()
            assertThat(noEqualsOrHashcodeCase.type.implementsEqualsAndHashcode()).isFalse()
        }
    }

    @Test
    fun testEqualsAndHashcodeCheckWithJavaPrimitive() {
        val inputSource =
            Source.java(
                "foo.bar.Subject",
                """
            package foo.bar;
            public class Subject {
                public int primitiveInt = 0;
                public Integer boxedInt = 1;
                public boolean primitiveBool = true;
                public Boolean boxedBool = false;
                public double primitiveDouble = 2.2;
                public Double boxedDouble = 3.3;
                public long primitiveLong = 4L;
                public Long boxedLong = 5L;
            }
            """
                    .trimIndent()
            )
        runProcessorTestWithK1(
            sources =
                listOf(
                    inputSource,
                    COMMON.USER,
                    COMMON.PAGING_SOURCE,
                    COMMON.LIMIT_OFFSET_PAGING_SOURCE,
                ),
        ) { invocation ->
            val subjectTypeElement = invocation.processingEnv.requireTypeElement("foo.bar.Subject")
            subjectTypeElement.getAllFieldsIncludingPrivateSupers().forEach { field ->
                assertThat(field.type.implementsEqualsAndHashcode()).isTrue()
            }
        }
    }

    @Test
    fun testEqualsAndHashcodeCheckWithKotlinPrimitive() {
        val source =
            Source.kotlin(
                "Foo.kt",
                """
            import androidx.room.*
            class Subject {
               val anInteger = 0
               val aBoolean = true
               val aDouble = 2.2
               val aLong = 5L
            }
            """
                    .trimIndent()
            )
        runProcessorTestWithK1(sources = listOf(source)) { invocation ->
            val subjectTypeElement = invocation.processingEnv.requireTypeElement("Subject")

            subjectTypeElement.getDeclaredFields().forEach {
                assertThat(it.type.implementsEqualsAndHashcode()).isTrue()
            }
        }
    }

    private fun createIntListToStringBinders(invocation: XTestInvocation): List<TypeConverter> {
        val intType = invocation.processingEnv.requireType(Integer::class)
        val listElement = invocation.processingEnv.requireTypeElement(java.util.List::class)
        val listOfInts = invocation.processingEnv.getDeclaredType(listElement, intType)
        val intListConverter =
            object :
                SingleStatementTypeConverter(
                    listOfInts,
                    invocation.context.processingEnv.requireType(CommonTypeNames.STRING)
                ) {
                override fun buildStatement(inputVarName: String, scope: CodeGenScope): XCodeBlock {
                    return XCodeBlock.of(
                        scope.language,
                        "%T.joinIntoString(%L)",
                        STRING_UTIL,
                        inputVarName
                    )
                }
            }

        val stringToIntListConverter =
            object :
                SingleStatementTypeConverter(
                    invocation.context.processingEnv.requireType(CommonTypeNames.STRING),
                    listOfInts
                ) {
                override fun buildStatement(inputVarName: String, scope: CodeGenScope): XCodeBlock {
                    return XCodeBlock.of(
                        scope.language,
                        "%T.splitToIntList(%L)",
                        STRING_UTIL,
                        inputVarName
                    )
                }
            }
        return listOf(intListConverter, stringToIntListConverter)
    }

    private fun dateTypeConverters(env: XProcessingEnv): List<TypeConverter> {
        val tDate = env.requireType("java.util.Date").makeNullable()
        val tLong = env.requireType("java.lang.Long").makeNullable()
        return listOf(
            object : SingleStatementTypeConverter(tDate, tLong) {
                override fun buildStatement(inputVarName: String, scope: CodeGenScope): XCodeBlock {
                    return XCodeBlock.of(scope.language, "%L.time", inputVarName)
                }
            },
            object : SingleStatementTypeConverter(tLong, tDate) {
                override fun buildStatement(inputVarName: String, scope: CodeGenScope): XCodeBlock {
                    return XCodeBlock.ofNewInstance(
                        scope.language,
                        tDate.asTypeName(),
                        "%L",
                        inputVarName
                    )
                }
            }
        )
    }
}
