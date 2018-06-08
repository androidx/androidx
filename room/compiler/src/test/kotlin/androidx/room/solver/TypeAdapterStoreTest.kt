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
import androidx.paging.PositionalDataSource
import androidx.room.Entity
import androidx.room.ext.L
import androidx.room.ext.LifecyclesTypeNames
import androidx.room.ext.PagingTypeNames
import androidx.room.ext.ReactiveStreamsTypeNames
import androidx.room.ext.RoomTypeNames.STRING_UTIL
import androidx.room.ext.RxJava2TypeNames
import androidx.room.ext.T
import androidx.room.ext.typeName
import androidx.room.parser.SQLTypeAffinity
import androidx.room.processor.Context
import androidx.room.processor.ProcessorErrors
import androidx.room.solver.binderprovider.DataSourceFactoryQueryResultBinderProvider
import androidx.room.solver.binderprovider.DataSourceQueryResultBinderProvider
import androidx.room.solver.binderprovider.FlowableQueryResultBinderProvider
import androidx.room.solver.binderprovider.LiveDataQueryResultBinderProvider
import androidx.room.solver.types.CompositeAdapter
import androidx.room.solver.types.TypeConverter
import androidx.room.testing.TestInvocation
import androidx.room.testing.TestProcessor
import com.google.auto.common.MoreTypes
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
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.type.TypeKind

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
            val primitiveType = invocation.processingEnv.typeUtils.getPrimitiveType(TypeKind.INT)
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
                    .elementUtils
                    .getTypeElement("java.lang.Boolean")
                    .asType()
            val adapter = store.findColumnTypeAdapter(boolean, null)
            assertThat(adapter, notNullValue())
            assertThat(adapter, instanceOf(CompositeAdapter::class.java))
            val composite = adapter as CompositeAdapter
            assertThat(composite.intoStatementConverter?.from?.typeName(),
                    `is`(TypeName.BOOLEAN.box()))
            assertThat(composite.columnTypeAdapter.out.typeName(),
                    `is`(TypeName.INT.box()))
        }.compilesWithoutError()
    }

    @Test
    fun testVia1TypeAdapter() {
        singleRun { invocation ->
            val store = TypeAdapterStore.create(Context(invocation.processingEnv))
            val booleanType = invocation.processingEnv.typeUtils
                    .getPrimitiveType(TypeKind.BOOLEAN)
            val adapter = store.findColumnTypeAdapter(booleanType, null)
            assertThat(adapter, notNullValue())
            assertThat(adapter, instanceOf(CompositeAdapter::class.java))
            val bindScope = testCodeGenScope()
            adapter!!.bindToStmt("stmt", "41", "fooVar", bindScope)
            assertThat(bindScope.generate().trim(), `is`("""
                    final int ${tmp(0)};
                    ${tmp(0)} = fooVar ? 1 : 0;
                    stmt.bindLong(41, ${tmp(0)});
                    """.trimIndent()))

            val cursorScope = testCodeGenScope()
            adapter.readFromCursor("res", "curs", "7", cursorScope)
            assertThat(cursorScope.generate().trim(), `is`("""
                    final int ${tmp(0)};
                    ${tmp(0)} = curs.getInt(7);
                    res = ${tmp(0)} != 0;
                    """.trimIndent()))
        }.compilesWithoutError()
    }

    @Test
    fun testVia2TypeAdapters() {
        singleRun { invocation ->
            val store = TypeAdapterStore.create(Context(invocation.processingEnv),
                    pointTypeConverters(invocation.processingEnv))
            val pointType = invocation.processingEnv.elementUtils
                    .getTypeElement("foo.bar.Point").asType()
            val adapter = store.findColumnTypeAdapter(pointType, null)
            assertThat(adapter, notNullValue())
            assertThat(adapter, instanceOf(CompositeAdapter::class.java))

            val bindScope = testCodeGenScope()
            adapter!!.bindToStmt("stmt", "41", "fooVar", bindScope)
            assertThat(bindScope.generate().trim(), `is`("""
                    final int ${tmp(0)};
                    final boolean ${tmp(1)};
                    ${tmp(1)} = foo.bar.Point.toBoolean(fooVar);
                    ${tmp(0)} = ${tmp(1)} ? 1 : 0;
                    stmt.bindLong(41, ${tmp(0)});
                    """.trimIndent()))

            val cursorScope = testCodeGenScope()
            adapter.readFromCursor("res", "curs", "11", cursorScope).toString()
            assertThat(cursorScope.generate().trim(), `is`("""
                    final int ${tmp(0)};
                    ${tmp(0)} = curs.getInt(11);
                    final boolean ${tmp(1)};
                    ${tmp(1)} = ${tmp(0)} != 0;
                    res = foo.bar.Point.fromBoolean(${tmp(1)});
                    """.trimIndent()))
        }.compilesWithoutError()
    }

    @Test
    fun testDate() {
        singleRun { (processingEnv) ->
            val store = TypeAdapterStore.create(Context(processingEnv),
                    dateTypeConverters(processingEnv))
            val tDate = processingEnv.elementUtils.getTypeElement("java.util.Date").asType()
            val adapter = store.findCursorValueReader(tDate, SQLTypeAffinity.INTEGER)
            assertThat(adapter, notNullValue())
            assertThat(adapter?.typeMirror(), `is`(tDate))
            val bindScope = testCodeGenScope()
            adapter!!.readFromCursor("outDate", "curs", "0", bindScope)
            assertThat(bindScope.generate().trim(), `is`("""
                final java.lang.Long _tmp;
                if (curs.isNull(0)) {
                  _tmp = null;
                } else {
                  _tmp = curs.getLong(0);
                }
                // convert Long to Date;
            """.trimIndent()))
        }.compilesWithoutError()
    }

    @Test
    fun testIntList() {
        singleRun { invocation ->
            val binders = createIntListToStringBinders(invocation)
            val store = TypeAdapterStore.create(Context(invocation.processingEnv), binders[0],
                    binders[1])

            val adapter = store.findColumnTypeAdapter(binders[0].from, null)
            assertThat(adapter, notNullValue())

            val bindScope = testCodeGenScope()
            adapter!!.bindToStmt("stmt", "41", "fooVar", bindScope)
            assertThat(bindScope.generate().trim(), `is`("""
                final java.lang.String ${tmp(0)};
                ${tmp(0)} = androidx.room.util.StringUtil.joinIntoString(fooVar);
                if (${tmp(0)} == null) {
                  stmt.bindNull(41);
                } else {
                  stmt.bindString(41, ${tmp(0)});
                }
                """.trimIndent()))

            val converter = store.findTypeConverter(binders[0].from,
                    invocation.context.COMMON_TYPES.STRING)
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

            val converter = store.findTypeConverter(binders[0].from,
                    invocation.context.COMMON_TYPES.STRING)
            assertThat(converter, notNullValue())
            assertThat(store.reverse(converter!!), nullValue())
        }
    }

    @Test
    fun testMissingRxRoom() {
        simpleRun(jfos = *arrayOf(COMMON.PUBLISHER, COMMON.FLOWABLE)) { invocation ->
            val publisherElement = invocation.processingEnv.elementUtils
                    .getTypeElement(ReactiveStreamsTypeNames.PUBLISHER.toString())
            assertThat(publisherElement, notNullValue())
            assertThat(FlowableQueryResultBinderProvider(invocation.context).matches(
                    MoreTypes.asDeclared(publisherElement.asType())), `is`(true))
        }.failsToCompile().withErrorContaining(ProcessorErrors.MISSING_ROOM_RXJAVA2_ARTIFACT)
    }

    @Test
    fun testFindPublisher() {
        simpleRun(jfos = *arrayOf(COMMON.PUBLISHER, COMMON.FLOWABLE, COMMON.RX2_ROOM)) {
            invocation ->
            val publisher = invocation.processingEnv.elementUtils
                    .getTypeElement(ReactiveStreamsTypeNames.PUBLISHER.toString())
            assertThat(publisher, notNullValue())
            assertThat(FlowableQueryResultBinderProvider(invocation.context).matches(
                    MoreTypes.asDeclared(publisher.asType())), `is`(true))
        }.compilesWithoutError()
    }

    @Test
    fun testFindFlowable() {
        simpleRun(jfos = *arrayOf(COMMON.PUBLISHER, COMMON.FLOWABLE, COMMON.RX2_ROOM)) {
            invocation ->
            val flowable = invocation.processingEnv.elementUtils
                    .getTypeElement(RxJava2TypeNames.FLOWABLE.toString())
            assertThat(flowable, notNullValue())
            assertThat(FlowableQueryResultBinderProvider(invocation.context).matches(
                    MoreTypes.asDeclared(flowable.asType())), `is`(true))
        }.compilesWithoutError()
    }

    @Test
    fun testFindLiveData() {
        simpleRun(jfos = *arrayOf(COMMON.COMPUTABLE_LIVE_DATA, COMMON.LIVE_DATA)) {
            invocation ->
            val liveData = invocation.processingEnv.elementUtils
                    .getTypeElement(LifecyclesTypeNames.LIVE_DATA.toString())
            assertThat(liveData, notNullValue())
            assertThat(LiveDataQueryResultBinderProvider(invocation.context).matches(
                    MoreTypes.asDeclared(liveData.asType())), `is`(true))
        }.compilesWithoutError()
    }

    @Test
    fun findDataSource() {
        simpleRun {
            invocation ->
            val dataSource = invocation.processingEnv.elementUtils
                    .getTypeElement(DataSource::class.java.canonicalName)
            assertThat(dataSource, notNullValue())
            assertThat(DataSourceQueryResultBinderProvider(invocation.context).matches(
                    MoreTypes.asDeclared(dataSource.asType())), `is`(true))
        }.failsToCompile().withErrorContaining(ProcessorErrors.PAGING_SPECIFY_DATA_SOURCE_TYPE)
    }

    @Test
    fun findPositionalDataSource() {
        simpleRun {
            invocation ->
            val dataSource = invocation.processingEnv.elementUtils
                    .getTypeElement(PositionalDataSource::class.java.canonicalName)
            assertThat(dataSource, notNullValue())
            assertThat(DataSourceQueryResultBinderProvider(invocation.context).matches(
                    MoreTypes.asDeclared(dataSource.asType())), `is`(true))
        }.compilesWithoutError()
    }

    @Test
    fun findDataSourceFactory() {
        simpleRun(jfos = *arrayOf(COMMON.DATA_SOURCE_FACTORY)) {
            invocation ->
            val pagedListProvider = invocation.processingEnv.elementUtils
                    .getTypeElement(PagingTypeNames.DATA_SOURCE_FACTORY.toString())
            assertThat(pagedListProvider, notNullValue())
            assertThat(DataSourceFactoryQueryResultBinderProvider(invocation.context).matches(
                    MoreTypes.asDeclared(pagedListProvider.asType())), `is`(true))
        }.compilesWithoutError()
    }

    private fun createIntListToStringBinders(invocation: TestInvocation): List<TypeConverter> {
        val intType = invocation.processingEnv.elementUtils
                .getTypeElement(Integer::class.java.canonicalName)
                .asType()
        val listType = invocation.processingEnv.elementUtils
                .getTypeElement(java.util.List::class.java.canonicalName)
        val listOfInts = invocation.processingEnv.typeUtils.getDeclaredType(listType, intType)

        val intListConverter = object : TypeConverter(listOfInts,
                invocation.context.COMMON_TYPES.STRING) {
            override fun convert(inputVarName: String, outputVarName: String,
                                 scope: CodeGenScope) {
                scope.builder().apply {
                    addStatement("$L = $T.joinIntoString($L)", outputVarName, STRING_UTIL,
                            inputVarName)
                }
            }
        }

        val stringToIntListConverter = object : TypeConverter(
                invocation.context.COMMON_TYPES.STRING, listOfInts) {
            override fun convert(inputVarName: String, outputVarName: String,
                                 scope: CodeGenScope) {
                scope.builder().apply {
                    addStatement("$L = $T.splitToIntList($L)", outputVarName, STRING_UTIL,
                            inputVarName)
                }
            }
        }
        return listOf(intListConverter, stringToIntListConverter)
    }

    fun singleRun(handler: (TestInvocation) -> Unit): CompileTester {
        return Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
                .that(listOf(JavaFileObjects.forSourceString("foo.bar.DummyClass",
                        """
                        package foo.bar;
                        import androidx.room.*;
                        @Entity
                        public class DummyClass {}
                        """
                ), JavaFileObjects.forSourceString("foo.bar.Point",
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
                )))
                .processedWith(TestProcessor.builder()
                        .forAnnotations(Entity::class)
                        .nextRunHandler { invocation ->
                            handler(invocation)
                            true
                        }
                        .build())
    }

    fun pointTypeConverters(env: ProcessingEnvironment): List<TypeConverter> {
        val tPoint = env.elementUtils.getTypeElement("foo.bar.Point").asType()
        val tBoolean = env.typeUtils.getPrimitiveType(TypeKind.BOOLEAN)
        return listOf(
                object : TypeConverter(tPoint, tBoolean) {
                    override fun convert(inputVarName: String, outputVarName: String,
                                         scope: CodeGenScope) {
                        scope.builder().apply {
                            addStatement("$L = $T.toBoolean($L)", outputVarName, from, inputVarName)
                        }
                    }
                },
                object : TypeConverter(tBoolean, tPoint) {
                    override fun convert(inputVarName: String, outputVarName: String,
                                         scope: CodeGenScope) {
                        scope.builder().apply {
                            addStatement("$L = $T.fromBoolean($L)", outputVarName, tPoint,
                                    inputVarName)
                        }
                    }
                }
        )
    }

    fun dateTypeConverters(env: ProcessingEnvironment): List<TypeConverter> {
        val tDate = env.elementUtils.getTypeElement("java.util.Date").asType()
        val tLong = env.elementUtils.getTypeElement("java.lang.Long").asType()
        return listOf(
                object : TypeConverter(tDate, tLong) {
                    override fun convert(inputVarName: String, outputVarName: String,
                                         scope: CodeGenScope) {
                        scope.builder().apply {
                            addStatement("// convert Date to Long")
                        }
                    }
                },
                object : TypeConverter(tLong, tDate) {
                    override fun convert(inputVarName: String, outputVarName: String,
                                         scope: CodeGenScope) {
                        scope.builder().apply {
                            addStatement("// convert Long to Date")
                        }
                    }
                }
        )
    }
}
