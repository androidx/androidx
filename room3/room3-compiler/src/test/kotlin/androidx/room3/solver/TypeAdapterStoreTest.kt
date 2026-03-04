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

package androidx.room3.solver

import COMMON
import androidx.kruth.assertThat
import androidx.room3.compiler.codegen.CodeLanguage
import androidx.room3.compiler.codegen.XCodeBlock
import androidx.room3.compiler.codegen.XTypeName
import androidx.room3.compiler.codegen.compat.XConverters.toString
import androidx.room3.compiler.processing.XProcessingEnv
import androidx.room3.compiler.processing.util.Source
import androidx.room3.compiler.processing.util.XTestInvocation
import androidx.room3.compiler.processing.util.runKspTest
import androidx.room3.compiler.processing.util.runProcessorTest
import androidx.room3.ext.CommonTypeNames
import androidx.room3.ext.RoomTypeNames.STRING_UTIL
import androidx.room3.ext.implementsEqualsAndHashcode
import androidx.room3.parser.SQLTypeAffinity
import androidx.room3.processor.Context
import androidx.room3.processor.CustomConverterProcessor
import androidx.room3.processor.ProcessorErrors
import androidx.room3.solver.query.parameter.CollectionQueryParameterAdapter
import androidx.room3.solver.types.BoxedPrimitiveColumnTypeAdapter
import androidx.room3.solver.types.ByteBufferColumnTypeAdapter
import androidx.room3.solver.types.ColumnTypeAdapter
import androidx.room3.solver.types.CompositeAdapter
import androidx.room3.solver.types.CustomTypeConverterWrapper
import androidx.room3.solver.types.EnumColumnTypeAdapter
import androidx.room3.solver.types.PrimitiveColumnTypeAdapter
import androidx.room3.solver.types.SingleStatementTypeConverter
import androidx.room3.solver.types.StringColumnTypeAdapter
import androidx.room3.solver.types.TypeConverter
import androidx.room3.solver.types.UuidColumnTypeAdapter
import androidx.room3.solver.types.ValueClassConverterWrapper
import androidx.room3.testing.context
import androidx.room3.vo.BuiltInConverterFlags
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
                import androidx.room3.*;
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
                    .trimIndent(),
            )
        val entity =
            Source.java(
                "foo.bar.EntityWithOneWayEnum",
                """
                package foo.bar;
                import androidx.room3.*;
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
                    .trimIndent(),
            )
        runKspTest(sources = listOf(entity, converter)) { invocation ->
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
                    BuiltInConverterFlags.DEFAULT,
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
                    BuiltInConverterFlags.DEFAULT,
                )
            val boolean = invocation.processingEnv.requireType("java.lang.Boolean").makeNullable()
            val adapter = store.findColumnTypeAdapter(boolean, null, skipDefaultConverter = false)
            assertThat(adapter, notNullValue())
            assertThat(adapter, instanceOf(CompositeAdapter::class.java))
            val composite = adapter as CompositeAdapter
            assertThat(
                composite.intoStatementConverter?.from?.asTypeName(),
                `is`(XTypeName.BOXED_BOOLEAN.copy(nullable = true)),
            )
            assertThat(
                composite.columnTypeAdapter.out.asTypeName(),
                `is`(XTypeName.BOXED_INT.copy(nullable = true)),
            )
        }
    }

    @Test
    fun testJavaLangEnumCompilesWithoutError() {
        val enumSrc =
            Source.java(
                "foo.bar.Fruit",
                """
                | package foo.bar;
                |                enum Fruit {
                |                    APPLE,
                |                    BANANA,
                |                    STRAWBERRY}
                """
                    .trimMargin(),
            )
        runKspTest(sources = listOf(enumSrc)) { invocation ->
            val store =
                TypeAdapterStore.create(
                    Context(invocation.processingEnv),
                    BuiltInConverterFlags.DEFAULT,
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
                @JvmInline
                value class IntValueClass(val data: Int)
                @JvmInline
                value class StringValueClass(val data: String)
                class EntityWithValueClass {
                    val intData = IntValueClass(123)
                    val stringData = StringValueClass("bla")
                }
                """
                    .trimIndent(),
            )
        var results: Map<String, String?> = mutableMapOf()

        runKspTest(sources = listOf(source)) { invocation ->
            val typeAdapterStore =
                TypeAdapterStore.create(
                    context = invocation.context,
                    builtInConverterFlags = BuiltInConverterFlags.DEFAULT,
                )
            val subject = invocation.processingEnv.requireTypeElement("EntityWithValueClass")
            results =
                subject.getAllFieldsIncludingPrivateSupers().associate { field ->
                    val columnAdapter =
                        typeAdapterStore.findColumnTypeAdapter(
                            out = field.type,
                            affinity = null,
                            false,
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
                @JvmInline
                value class Foo(val value : Int) {
                    val double
                        get() = value * 2
                }
                """
                    .trimIndent(),
            )

        runKspTest(sources = listOf(source)) { invocation ->
            val store =
                TypeAdapterStore.create(
                    context = invocation.context,
                    builtInConverterFlags = BuiltInConverterFlags.DEFAULT,
                )
            val typeElement = invocation.processingEnv.requireTypeElement("Foo")
            val result =
                store.findColumnTypeAdapter(
                    out = typeElement.type,
                    affinity = null,
                    skipDefaultConverter = false,
                )
            assertThat(result).isInstanceOf<ValueClassConverterWrapper>()
        }
    }

    @Test
    fun testValueClassWithPrivateVal() {
        val source =
            Source.kotlin(
                "Foo.kt",
                """
                @JvmInline
                value class Foo(private val value : Int)
                """
                    .trimIndent(),
            )

        runKspTest(sources = listOf(source)) { invocation ->
            val store =
                TypeAdapterStore.create(
                    context = invocation.context,
                    builtInConverterFlags = BuiltInConverterFlags.DEFAULT,
                )
            val typeElement = invocation.processingEnv.requireTypeElement("Foo")
            val result =
                store.findColumnTypeAdapter(
                    out = typeElement.type,
                    affinity = null,
                    skipDefaultConverter = false,
                )
            assertThat(result).isNull()
        }
    }

    @Test
    fun testValueClassWithPrivateConstructor() {
        val source =
            Source.kotlin(
                "Foo.kt",
                """
                @JvmInline
                value class Foo private constructor(val value : Int)
                """
                    .trimIndent(),
            )

        runKspTest(sources = listOf(source)) { invocation ->
            val store =
                TypeAdapterStore.create(
                    context = invocation.context,
                    builtInConverterFlags = BuiltInConverterFlags.DEFAULT,
                )
            val typeElement = invocation.processingEnv.requireTypeElement("Foo")
            val result =
                store.findColumnTypeAdapter(
                    out = typeElement.type,
                    affinity = null,
                    skipDefaultConverter = false,
                )
            assertThat(result).isNull()
        }
    }

    @Test
    fun testJavaLangByteBufferCompilesWithoutError() {
        runProcessorTest { invocation ->
            val store =
                TypeAdapterStore.create(
                    Context(invocation.processingEnv),
                    BuiltInConverterFlags.DEFAULT,
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
                    BuiltInConverterFlags.DEFAULT,
                )
            val uuid = invocation.processingEnv.requireType(CommonTypeNames.UUID)
            val adapter =
                store.findColumnTypeAdapter(
                    out = uuid,
                    affinity = null,
                    skipDefaultConverter = false,
                )

            assertThat(adapter).isNotNull()
            assertThat(adapter).isInstanceOf<UuidColumnTypeAdapter>()
        }
    }

    @Test
    fun testVia1TypeAdapter() {
        runKspTest(sources = emptyList()) { invocation ->
            val store =
                TypeAdapterStore.create(
                    Context(invocation.processingEnv),
                    BuiltInConverterFlags.DEFAULT,
                )
            val booleanType = invocation.processingEnv.requireType(XTypeName.PRIMITIVE_BOOLEAN)
            val adapter =
                store.findColumnTypeAdapter(booleanType, null, skipDefaultConverter = false)
            assertThat(adapter, notNullValue())
            assertThat(adapter, instanceOf(CompositeAdapter::class.java))
            val bindScope = testCodeGenScope()
            adapter!!.bindToStmt("stmt", "41", "fooVar", bindScope)
            assertThat(
                bindScope.generate().toString(CodeLanguage.KOTLIN).trim(),
                `is`(
                    """
                    val ${tmp(0)}: kotlin.Int = if (fooVar) 1 else 0
                    stmt.bindLong(41, ${tmp(0)}.toLong())
                    """
                        .trimIndent()
                ),
            )

            val cursorScope = testCodeGenScope()
            adapter.readFromStatement("res", "curs", "7", cursorScope)
            assertThat(
                cursorScope.generate().toString(CodeLanguage.KOTLIN).trim(),
                `is`(
                    """
                    val ${tmp(0)}: kotlin.Int
                    ${tmp(0)} = curs.getLong(7).toInt()
                    res = ${tmp(0)} != 0
                    """
                        .trimIndent()
                ),
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
            import androidx.room3.*;
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
            """,
            )
        runKspTest(sources = listOf(point)) { invocation ->
            val context = Context(invocation.processingEnv)
            val converters =
                CustomConverterProcessor(
                        context = context,
                        element = invocation.processingEnv.requireTypeElement("foo.bar.Point"),
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
                bindScope.generate().toString(CodeLanguage.KOTLIN).trim(),
                `is`(
                    """
                    val ${tmp(0)}: kotlin.Boolean = foo.bar.Point.toBoolean(fooVar)
                    val ${tmp(1)}: kotlin.Int = if (${tmp(0)}) 1 else 0
                    stmt.bindLong(41, ${tmp(1)}.toLong())
                    """
                        .trimIndent()
                ),
            )

            val cursorScope = testCodeGenScope()
            adapter.readFromStatement("res", "curs", "11", cursorScope).toString()
            assertThat(
                cursorScope.generate().toString(CodeLanguage.KOTLIN).trim(),
                `is`(
                    """
                    val ${tmp(0)}: kotlin.Int
                    ${tmp(0)} = curs.getLong(11).toInt()
                    val ${tmp(1)}: kotlin.Boolean = ${tmp(0)} != 0
                    res = foo.bar.Point.fromBoolean(${tmp(1)})
                    """
                        .trimIndent()
                ),
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
                    dateTypeConverters(invocation.processingEnv),
                )
            val tDate = invocation.processingEnv.requireType("java.util.Date").makeNullable()
            val adapter = store.findStatementValueReader(tDate, SQLTypeAffinity.INTEGER)
            assertThat(adapter, notNullValue())
            assertThat(adapter?.typeMirror(), `is`(tDate))
            val bindScope = testCodeGenScope()
            adapter!!.readFromStatement("outDate", "curs", "0", bindScope)
            assertThat(
                bindScope.generate().toString(CodeLanguage.JAVA).trim(),
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
                ),
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
                    binders[1],
                )

            val adapter =
                store.findColumnTypeAdapter(binders[0].from, null, skipDefaultConverter = false)
            assertThat(adapter).isNotNull()

            val bindScope = testCodeGenScope()
            adapter!!.bindToStmt("stmt", "41", "fooVar", bindScope)
            val expectedAdapterCode =
                if (invocation.isKsp) {
                    """
                stmt.bindText(41, ${tmp(0)});
                """
                        .trimIndent()
                } else {
                    """
                if (${tmp(0)} == null) {
                  stmt.bindNull(41);
                } else {
                  stmt.bindText(41, ${tmp(0)});
                }
                """
                        .trimIndent()
                }
            assertThat(bindScope.generate().toString(CodeLanguage.JAVA).trim())
                .isEqualTo(
                    """
                |final java.lang.String ${tmp(0)} = androidx.room3.util.StringUtil.joinIntoString(fooVar);
                |$expectedAdapterCode
                """
                        .trimMargin()
                )

            val converter =
                store.typeConverterStore.findTypeConverter(
                    binders[0].from,
                    invocation.context.processingEnv.requireType(CommonTypeNames.STRING),
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
                    binders[0],
                )
            val adapter =
                store.findColumnTypeAdapter(binders[0].from, null, skipDefaultConverter = false)
            assertThat(adapter, nullValue())

            val stmtBinder = store.findStatementValueBinder(binders[0].from, null)
            assertThat(stmtBinder, notNullValue())

            val converter =
                store.typeConverterStore.findTypeConverter(
                    binders[0].from,
                    invocation.context.processingEnv.requireType(CommonTypeNames.STRING),
                )
            assertThat(converter, notNullValue())
            assertThat(store.typeConverterStore.reverse(converter!!), nullValue())
        }
    }

    @Test
    fun findQueryParameterAdapter_collections() {
        runProcessorTest { invocation ->
            val store =
                TypeAdapterStore.create(
                    context = invocation.context,
                    builtInConverterFlags = BuiltInConverterFlags.DEFAULT,
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
                        invocation.processingEnv.requireType(XTypeName.PRIMITIVE_INT).boxed(),
                    )
                }
                .forEach { type ->
                    val adapter =
                        store.findQueryParameterAdapter(
                            typeMirror = type,
                            isMultipleParameter = true,
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
                import androidx.room3.*
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
                    .trimIndent(),
            )
        runKspTest(sources = listOf(source)) { invocation ->
            val converters =
                CustomConverterProcessor(
                        context = invocation.context,
                        element = invocation.processingEnv.requireTypeElement("MyConverters"),
                    )
                    .process()
                    .map(::CustomTypeConverterWrapper)
            val typeAdapterStore =
                TypeAdapterStore.create(
                    context = invocation.context,
                    builtInConverterFlags = BuiltInConverterFlags.DEFAULT,
                    extras = converters.toTypedArray(),
                )
            val subject = invocation.processingEnv.requireTypeElement("Subject")
            val results =
                subject.getAllFieldsIncludingPrivateSupers().associate { field ->
                    val binder =
                        typeAdapterStore.findStatementValueBinder(
                            input = field.type,
                            affinity = null,
                        )

                    val signature =
                        when (binder) {
                            null -> null
                            is PrimitiveColumnTypeAdapter -> "primitive"
                            is BoxedPrimitiveColumnTypeAdapter -> "boxed"
                            is CompositeAdapter -> {
                                when (val converter = binder.intoStatementConverter) {
                                    null -> "composite null"
                                    is CustomTypeConverterWrapper -> converter.custom.function.name
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
                    .trimIndent(),
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
                    .trimIndent(),
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
                    .trimIndent(),
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
                    .trimIndent(),
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
                    .trimIndent(),
            )
        runKspTest(
            sources =
                listOf(
                    classExtendsClassWithEqualsAndHashcodeFunctions,
                    classWithFncs,
                    classWithoutFncs,
                    enumClass,
                    classWithWrongFncs,
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
                    .trimIndent(),
            )
        runKspTest(
            sources =
                listOf(
                    inputSource,
                    COMMON.USER,
                    COMMON.PAGING_SOURCE,
                    COMMON.LIMIT_OFFSET_PAGING_SOURCE,
                )
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
                import androidx.room3.*
                class Subject {
                   val anInteger = 0
                   val aBoolean = true
                   val aDouble = 2.2
                   val aLong = 5L
                }
                """
                    .trimIndent(),
            )
        runKspTest(sources = listOf(source)) { invocation ->
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
                    invocation.context.processingEnv.requireType(CommonTypeNames.STRING),
                ) {
                override fun buildStatement(inputVarName: String, scope: CodeGenScope): XCodeBlock {
                    return XCodeBlock.of("%T.joinIntoString(%L)", STRING_UTIL, inputVarName)
                }
            }

        val stringToIntListConverter =
            object :
                SingleStatementTypeConverter(
                    invocation.context.processingEnv.requireType(CommonTypeNames.STRING),
                    listOfInts,
                ) {
                override fun buildStatement(inputVarName: String, scope: CodeGenScope): XCodeBlock {
                    return XCodeBlock.of("%T.splitToIntList(%L)", STRING_UTIL, inputVarName)
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
                    return XCodeBlock.of("%L.time", inputVarName)
                }
            },
            object : SingleStatementTypeConverter(tLong, tDate) {
                override fun buildStatement(inputVarName: String, scope: CodeGenScope): XCodeBlock {
                    return XCodeBlock.ofNewInstance(tDate.asTypeName(), "%L", inputVarName)
                }
            },
        )
    }
}
