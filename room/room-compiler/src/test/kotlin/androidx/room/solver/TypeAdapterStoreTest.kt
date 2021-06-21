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
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.runProcessorTest
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
import androidx.room.processor.CustomConverterProcessor
import androidx.room.processor.ProcessorErrors
import androidx.room.solver.binderprovider.DataSourceFactoryQueryResultBinderProvider
import androidx.room.solver.binderprovider.DataSourceQueryResultBinderProvider
import androidx.room.solver.binderprovider.LiveDataQueryResultBinderProvider
import androidx.room.solver.binderprovider.PagingSourceQueryResultBinderProvider
import androidx.room.solver.binderprovider.RxQueryResultBinderProvider
import androidx.room.solver.query.parameter.CollectionQueryParameterAdapter
import androidx.room.solver.shortcut.binderprovider.GuavaListenableFutureDeleteOrUpdateMethodBinderProvider
import androidx.room.solver.shortcut.binderprovider.GuavaListenableFutureInsertMethodBinderProvider
import androidx.room.solver.shortcut.binderprovider.RxCallableDeleteOrUpdateMethodBinderProvider
import androidx.room.solver.shortcut.binderprovider.RxCallableInsertMethodBinderProvider
import androidx.room.solver.types.BoxedPrimitiveColumnTypeAdapter
import androidx.room.solver.types.CompositeAdapter
import androidx.room.solver.types.CompositeTypeConverter
import androidx.room.solver.types.CustomTypeConverterWrapper
import androidx.room.solver.types.EnumColumnTypeAdapter
import androidx.room.solver.types.PrimitiveColumnTypeAdapter
import androidx.room.solver.types.TypeConverter
import androidx.room.testing.context
import com.google.common.truth.Truth.assertThat
import com.squareup.javapoet.TypeName
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import testCodeGenScope

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
@RunWith(JUnit4::class)
class TypeAdapterStoreTest {
    companion object {
        fun tmp(index: Int) = CodeGenScope._tmpVar(index)
    }

    @Test
    fun testInvalidNonStaticInnerClass() {
        val converter = Source.java(
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
            """.trimIndent()
        )
        val entity = Source.java(
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
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(entity, converter)
        ) { invocation ->
            val typeElement =
                invocation.processingEnv.requireTypeElement("foo.bar.EntityWithOneWayEnum")
            val context = Context(invocation.processingEnv)
            CustomConverterProcessor.Companion.findConverters(context, typeElement)
            invocation.assertCompilationResult {
                hasError(ProcessorErrors.INNER_CLASS_TYPE_CONVERTER_MUST_BE_STATIC)
            }
        }
    }

    @Test
    fun testDirect() {
        runProcessorTest { invocation ->
            val store = TypeAdapterStore.create(Context(invocation.processingEnv))
            val primitiveType = invocation.processingEnv.requireType(TypeName.INT)
            val adapter = store.findColumnTypeAdapter(
                primitiveType,
                null,
                skipEnumConverter = false
            )
            assertThat(adapter, notNullValue())
        }
    }

    @Test
    fun testJavaLangBoolean() {
        runProcessorTest { invocation ->
            val store = TypeAdapterStore.create(
                Context(invocation.processingEnv)
            )
            val boolean = invocation
                .processingEnv
                .requireType("java.lang.Boolean")
                .makeNullable()
            val adapter = store.findColumnTypeAdapter(
                boolean,
                null,
                skipEnumConverter = false
            )
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
        }
    }

    @Test
    fun testJavaLangEnumCompilesWithoutError() {
        val enumSrc = Source.java(
            "foo.bar.Fruit",
            """ package foo.bar;
                import androidx.room.*;
                enum Fruit {
                    APPLE,
                    BANANA,
                    STRAWBERRY}
                """.trimMargin()
        )
        runProcessorTest(
            sources = listOf(enumSrc)
        ) { invocation ->
            val store = TypeAdapterStore.create(Context(invocation.processingEnv))
            val enum = invocation
                .processingEnv
                .requireType("foo.bar.Fruit")
            val adapter = store.findColumnTypeAdapter(enum, null, skipEnumConverter = false)
            assertThat(adapter, notNullValue())
            assertThat(adapter, instanceOf(EnumColumnTypeAdapter::class.java))
        }
    }

    @Test
    fun testVia1TypeAdapter() {
        runProcessorTest { invocation ->
            val store = TypeAdapterStore.create(Context(invocation.processingEnv))
            val booleanType = invocation.processingEnv.requireType(TypeName.BOOLEAN)
            val adapter = store.findColumnTypeAdapter(
                booleanType,
                null,
                skipEnumConverter = false
            )
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
        }
    }

    @Test
    fun testVia2TypeAdapters() {
        val point = Source.java(
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
        runProcessorTest(
            sources = listOf(point)
        ) { invocation ->
            val store = TypeAdapterStore.create(
                Context(invocation.processingEnv),
                pointTypeConverters(invocation.processingEnv)
            )
            val pointType = invocation.processingEnv.requireType("foo.bar.Point")
            val adapter = store.findColumnTypeAdapter(
                pointType,
                null,
                skipEnumConverter = false
            )
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
        }
    }

    @Test
    fun testDate() {
        runProcessorTest { invocation ->
            val store = TypeAdapterStore.create(
                invocation.context,
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
                // convert Long to Date;
                    """.trimIndent()
                )
            )
        }
    }

    @Test
    fun multiStepTypeConverters() {
        val source = Source.kotlin(
            "Foo.kt",
            """
            import androidx.room.*
            interface Type1_Super
            interface Type1 : Type1_Super
            interface Type1_Sub : Type1
            interface Type2_Super
            interface Type2 : Type2_Super
            interface Type2_Sub : Type2
            interface JumpType_1
            interface JumpType_2
            interface JumpType_3
            class MyConverters {
                @TypeConverter
                fun t1_jump1(inp : Type1): JumpType_1 = TODO()
                @TypeConverter
                fun jump1_t2_Sub(inp : JumpType_1): Type2_Sub = TODO()
                @TypeConverter
                fun jump1_t2(inp : JumpType_1): Type2 = TODO()
                @TypeConverter
                fun t1_super_jump2(inp : Type1_Super): JumpType_2 = TODO()
                @TypeConverter
                fun jump2_jump3(inp : JumpType_2): JumpType_3 = TODO()
                @TypeConverter
                fun jump2_Type2_Sub(inp : JumpType_3): Type2_Sub = TODO()
            }
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(source)) { invocation ->
            val convertersElm = invocation.processingEnv.requireTypeElement("MyConverters")
            val converters = CustomConverterProcessor(invocation.context, convertersElm)
                .process()
            val store = TypeAdapterStore.create(
                invocation.context,
                converters.map(::CustomTypeConverterWrapper)
            )

            fun TypeConverter.toSignature(): String {
                return when (this) {
                    is CompositeTypeConverter -> "${conv1.toSignature()} : ${conv2.toSignature()}"
                    else -> "${from.typeName} -> ${to.typeName}"
                }
            }

            fun findConverter(from: String, to: String): String? {
                val input = invocation.processingEnv.requireType(from)
                val output = invocation.processingEnv.requireType(to)
                return store.findTypeConverter(
                    input = input,
                    output = output
                )?.also {
                    // validate that it makes sense to ensure test is correct
                    assertThat(output.isAssignableFrom(it.to)).isTrue()
                    assertThat(it.from.isAssignableFrom(input)).isTrue()
                }?.toSignature()
            }
            assertThat(
                findConverter("Type1", "Type2")
            ).isEqualTo(
                "Type1 -> JumpType_1 : JumpType_1 -> Type2"
            )
            assertThat(
                findConverter("Type1", "Type2_Sub")
            ).isEqualTo(
                "Type1 -> JumpType_1 : JumpType_1 -> Type2_Sub"
            )
            assertThat(
                findConverter("Type1_Super", "Type2_Super")
            ).isEqualTo(
                "Type1_Super -> JumpType_2 : JumpType_2 -> JumpType_3 : JumpType_3 -> Type2_Sub"
            )
            assertThat(
                findConverter("Type1", "Type2_Sub")
            ).isEqualTo(
                "Type1 -> JumpType_1 : JumpType_1 -> Type2_Sub"
            )
            assertThat(
                findConverter("Type1_Sub", "Type2_Sub")
            ).isEqualTo(
                "Type1 -> JumpType_1 : JumpType_1 -> Type2_Sub"
            )
            assertThat(
                findConverter("Type2", "Type2_Sub")
            ).isNull()
            assertThat(
                findConverter("Type2", "Type1")
            ).isNull()
        }
    }

    @Test
    fun testIntList() {
        runProcessorTest { invocation ->
            val binders = createIntListToStringBinders(invocation)
            val store = TypeAdapterStore.create(
                Context(invocation.processingEnv), binders[0],
                binders[1]
            )

            val adapter = store.findColumnTypeAdapter(
                binders[0].from,
                null,
                skipEnumConverter = false
            )
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
        }
    }

    @Test
    fun testOneWayConversion() {
        runProcessorTest { invocation ->
            val binders = createIntListToStringBinders(invocation)
            val store = TypeAdapterStore.create(Context(invocation.processingEnv), binders[0])
            val adapter = store.findColumnTypeAdapter(
                binders[0].from,
                null,
                skipEnumConverter = false
            )
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
        runProcessorTest(
            sources = listOf(COMMON.PUBLISHER, COMMON.RX2_FLOWABLE)
        ) { invocation ->
            val publisherElement = invocation.processingEnv
                .requireTypeElement(ReactiveStreamsTypeNames.PUBLISHER)
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
        @Suppress("CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS")
        runProcessorTest(
            sources = listOf(COMMON.PUBLISHER, COMMON.RX3_FLOWABLE)
        ) { invocation ->
            val publisherElement = invocation.processingEnv
                .requireTypeElement(ReactiveStreamsTypeNames.PUBLISHER)
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
    fun testFindPublisher() {
        listOf(
            COMMON.RX2_FLOWABLE to COMMON.RX2_ROOM,
            COMMON.RX3_FLOWABLE to COMMON.RX3_ROOM
        ).forEach { (rxTypeSrc, rxRoomSrc) ->
            @Suppress("CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS")
            runProcessorTest(
                sources = listOf(COMMON.PUBLISHER, rxTypeSrc, rxRoomSrc)
            ) { invocation ->
                val publisher = invocation.processingEnv
                    .requireTypeElement(ReactiveStreamsTypeNames.PUBLISHER)
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
        ).forEach { (rxTypeSrc, rxRoomSrc, rxTypeClassName) ->
            @Suppress("CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS")
            runProcessorTest(
                sources = listOf(COMMON.PUBLISHER, rxTypeSrc, rxRoomSrc)
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
        ).forEach { (rxTypeSrc, rxRoomSrc, rxTypeClassName) ->
            @Suppress("CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS")
            runProcessorTest(
                sources = listOf(rxTypeSrc, rxRoomSrc)
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
        ).forEach { (rxTypeSrc, _, rxTypeClassName) ->
            @Suppress("CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS")
            runProcessorTest(sources = listOf(rxTypeSrc)) { invocation ->
                val single = invocation.processingEnv.requireTypeElement(rxTypeClassName)
                assertThat(single, notNullValue())
                assertThat(
                    RxCallableInsertMethodBinderProvider.getAll(invocation.context).any {
                        it.matches(single.type)
                    },
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
        ).forEach { (rxTypeSrc, _, rxTypeClassName) ->
            runProcessorTest(sources = listOf(rxTypeSrc)) { invocation ->
                val maybe = invocation.processingEnv.requireTypeElement(rxTypeClassName)
                assertThat(
                    RxCallableInsertMethodBinderProvider.getAll(invocation.context).any {
                        it.matches(maybe.type)
                    },
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
        ).forEach { (rxTypeSrc, _, rxTypeClassName) ->
            runProcessorTest(sources = listOf(rxTypeSrc)) { invocation ->
                val completable = invocation.processingEnv.requireTypeElement(rxTypeClassName)
                assertThat(
                    RxCallableInsertMethodBinderProvider.getAll(invocation.context).any {
                        it.matches(completable.type)
                    },
                    `is`(true)
                )
            }
        }
    }

    @Test
    fun testFindInsertListenableFuture() {
        runProcessorTest(sources = listOf(COMMON.LISTENABLE_FUTURE)) {
            invocation ->
            val future = invocation.processingEnv
                .requireTypeElement(GuavaUtilConcurrentTypeNames.LISTENABLE_FUTURE)
            assertThat(
                GuavaListenableFutureInsertMethodBinderProvider(invocation.context).matches(
                    future.type
                ),
                `is`(true)
            )
        }
    }

    @Test
    fun testFindDeleteOrUpdateSingle() {
        runProcessorTest(sources = listOf(COMMON.RX2_SINGLE)) { invocation ->
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
        runProcessorTest(sources = listOf(COMMON.RX2_MAYBE)) {
            invocation ->
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
        runProcessorTest(sources = listOf(COMMON.RX2_COMPLETABLE)) {
            invocation ->
            val completable = invocation.processingEnv
                .requireTypeElement(RxJava2TypeNames.COMPLETABLE)
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
        runProcessorTest(
            sources = listOf(COMMON.LISTENABLE_FUTURE)
        ) { invocation ->
            val future = invocation.processingEnv
                .requireTypeElement(GuavaUtilConcurrentTypeNames.LISTENABLE_FUTURE)
            assertThat(future, notNullValue())
            assertThat(
                GuavaListenableFutureDeleteOrUpdateMethodBinderProvider(invocation.context)
                    .matches(future.type),
                `is`(true)
            )
        }
    }

    @Test
    fun testFindLiveData() {
        runProcessorTest(
            sources = listOf(COMMON.COMPUTABLE_LIVE_DATA, COMMON.LIVE_DATA)
        ) { invocation ->
            val liveData = invocation.processingEnv
                .requireTypeElement(LifecyclesTypeNames.LIVE_DATA)
            assertThat(liveData, notNullValue())
            assertThat(
                LiveDataQueryResultBinderProvider(invocation.context).matches(
                    liveData.type
                ),
                `is`(true)
            )
        }
    }

    @Test
    fun findPagingSourceIntKey() {
        runProcessorTest { invocation ->
            val pagingSourceElement = invocation.processingEnv
                .requireTypeElement(PagingSource::class)
            val intType = invocation.processingEnv.requireType(Integer::class)
            val pagingSourceIntIntType = invocation.processingEnv
                .getDeclaredType(pagingSourceElement, intType, intType)

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
            val pagingSourceElement = invocation.processingEnv
                .requireTypeElement(PagingSource::class)
            val stringType = invocation.processingEnv.requireType(String::class)
            val pagingSourceIntIntType = invocation.processingEnv
                .getDeclaredType(pagingSourceElement, stringType, stringType)

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
    fun findDataSource() {
        runProcessorTest {
            invocation ->
            val dataSource = invocation.processingEnv.requireTypeElement(DataSource::class)
            assertThat(dataSource, notNullValue())
            assertThat(
                DataSourceQueryResultBinderProvider(invocation.context).matches(
                    dataSource.type
                ),
                `is`(true)
            )
            invocation.assertCompilationResult {
                hasError(ProcessorErrors.PAGING_SPECIFY_DATA_SOURCE_TYPE)
            }
        }
    }

    @Test
    fun findPositionalDataSource() {
        runProcessorTest {
            invocation ->
            @Suppress("DEPRECATION")
            val dataSource = invocation.processingEnv
                .requireTypeElement(androidx.paging.PositionalDataSource::class)
            assertThat(dataSource, notNullValue())
            assertThat(
                DataSourceQueryResultBinderProvider(invocation.context).matches(
                    dataSource.type
                ),
                `is`(true)
            )
        }
    }

    @Test
    fun findDataSourceFactory() {
        runProcessorTest(sources = listOf(COMMON.DATA_SOURCE_FACTORY)) {
            invocation ->
            val pagedListProvider = invocation.processingEnv
                .requireTypeElement(PagingTypeNames.DATA_SOURCE_FACTORY)
            assertThat(pagedListProvider, notNullValue())
            assertThat(
                DataSourceFactoryQueryResultBinderProvider(invocation.context).matches(
                    pagedListProvider.type
                ),
                `is`(true)
            )
        }
    }

    @Test
    fun findQueryParameterAdapter_collections() {
        runProcessorTest { invocation ->
            val store = TypeAdapterStore.create(
                context = invocation.context
            )
            val javacCollectionTypes = listOf(
                "java.util.Set",
                "java.util.List",
                "java.util.ArrayList"
            )
            val kotlinCollectionTypes = listOf(
                "kotlin.collections.List",
                "kotlin.collections.MutableList"
            )
            val collectionTypes = if (invocation.isKsp) {
                javacCollectionTypes + kotlinCollectionTypes
            } else {
                javacCollectionTypes
            }
            collectionTypes.map { collectionType ->
                invocation.processingEnv.getDeclaredType(
                    invocation.processingEnv.requireTypeElement(collectionType),
                    invocation.processingEnv.requireType(TypeName.INT).boxed()
                )
            }.forEach { type ->
                val adapter = store.findQueryParameterAdapter(
                    typeMirror = type,
                    isMultipleParameter = true
                )
                assertThat(adapter).isNotNull()
                assertThat(adapter).isInstanceOf(CollectionQueryParameterAdapter::class.java)
            }
        }
    }

    @Test
    fun typeAliases() {
        val source = Source.kotlin(
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
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(source)
        ) { invocation ->
            val converters = CustomConverterProcessor(
                context = invocation.context,
                element = invocation.processingEnv.requireTypeElement("MyConverters")
            ).process().map(::CustomTypeConverterWrapper)
            val typeAdapterStore = TypeAdapterStore.create(
                context = invocation.context,
                extras = converters.toTypedArray()
            )
            val subject = invocation.processingEnv.requireTypeElement("Subject")
            val results = subject.getAllFieldsIncludingPrivateSupers().associate { field ->
                val binder = typeAdapterStore.findStatementValueBinder(
                    input = field.type,
                    affinity = null
                )

                val signature = when (binder) {
                    null -> null
                    is PrimitiveColumnTypeAdapter -> "primitive"
                    is BoxedPrimitiveColumnTypeAdapter -> "boxed"
                    is CompositeAdapter -> {
                        when (val converter = binder.intoStatementConverter) {
                            null -> "composite null"
                            is CustomTypeConverterWrapper -> converter.custom.methodName
                            else -> "composite unknown"
                        }
                    }
                    else -> "unknown"
                }
                field.name to signature
            }
            // see: 187359339. We use nullability for assignments only in KSP
            val nullableClassAdapter = if (invocation.isKsp) {
                "nullableMyClassToString"
            } else {
                "myClassToString"
            }
            assertThat(results).containsExactlyEntriesIn(
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

    private fun createIntListToStringBinders(invocation: XTestInvocation): List<TypeConverter> {
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
        val tDate = env.requireType("java.util.Date").makeNullable()
        val tLong = env.requireType("java.lang.Long").makeNullable()
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
