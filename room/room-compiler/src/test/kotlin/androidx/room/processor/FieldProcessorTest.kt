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

import androidx.room.Entity
import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.processing.XFieldElement
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.runProcessorTest
import androidx.room.ext.CommonTypeNames
import androidx.room.parser.Collate
import androidx.room.parser.SQLTypeAffinity
import androidx.room.runProcessorTestWithK1
import androidx.room.solver.types.ColumnTypeAdapter
import androidx.room.testing.context
import androidx.room.vo.Field
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.mock

@RunWith(JUnit4::class)
class FieldProcessorTest {
    companion object {
        const val ENTITY_PREFIX =
            """
                package foo.bar;
                import androidx.room.*;
                import androidx.annotation.NonNull;
                import androidx.annotation.Nullable;
                @Entity
                abstract class MyEntity {
                """
        const val ENTITY_SUFFIX = "}"
        val ALL_PRIMITIVES =
            arrayListOf(
                XTypeName.PRIMITIVE_INT,
                XTypeName.PRIMITIVE_BYTE,
                XTypeName.PRIMITIVE_SHORT,
                XTypeName.PRIMITIVE_LONG,
                XTypeName.PRIMITIVE_CHAR,
                XTypeName.PRIMITIVE_FLOAT,
                XTypeName.PRIMITIVE_DOUBLE
            )
        val ALL_BOXED_PRIMITIVES =
            arrayListOf(
                XTypeName.BOXED_INT,
                XTypeName.BOXED_BYTE,
                XTypeName.BOXED_SHORT,
                XTypeName.BOXED_LONG,
                XTypeName.BOXED_CHAR,
                XTypeName.BOXED_FLOAT,
                XTypeName.BOXED_DOUBLE
            )
        val ARRAY_CONVERTER =
            Source.java(
                "foo.bar.MyConverter",
                """
            |package foo.bar;
            |import androidx.room.*;
            |public class MyConverter {
            |${
                ALL_PRIMITIVES.joinToString("|\n") {
                    val arrayName = it.toString(CodeLanguage.JAVA)
                    val arrayDef = "$arrayName[]"
                    """
                    |  @TypeConverter
                    |  public static String arrayIntoString($arrayDef input) { return null; }
                    |  @TypeConverter
                    |  public static $arrayDef stringIntoArray$arrayName(String input) { return null; }
                    """
                }
            }
            |${
                ALL_BOXED_PRIMITIVES.joinToString("|\n") {
                    val arrayName = it.simpleNames.single()
                    val arrayDef = "$arrayName[]"
                    """
                    |  @TypeConverter
                    |  public static String arrayIntoString($arrayDef input) { return null; }
                    |  @TypeConverter
                    |  public static $arrayDef stringIntoArray$arrayName(String input) { return null; }
                    """
                }
            }
            |}
            """
                    .trimMargin()
            )

        private fun XTypeName.affinity(): SQLTypeAffinity {
            return when (this) {
                XTypeName.PRIMITIVE_FLOAT,
                XTypeName.BOXED_FLOAT,
                XTypeName.PRIMITIVE_DOUBLE,
                XTypeName.BOXED_DOUBLE -> SQLTypeAffinity.REAL
                else -> SQLTypeAffinity.INTEGER
            }
        }

        private fun XTypeName.box(invocation: XTestInvocation) = typeMirror(invocation).boxed()

        private fun XTypeName.typeMirror(invocation: XTestInvocation) =
            invocation.processingEnv.requireType(this)
    }

    @Test
    fun primitives() {
        ALL_PRIMITIVES.forEach { primitive ->
            val fieldName = primitive.toString(CodeLanguage.JAVA)
            singleEntity("$fieldName x;") { field, invocation ->
                assertThat(
                    field,
                    `is`(
                        Field(
                            name = "x",
                            type = primitive.typeMirror(invocation),
                            element = field.element,
                            affinity = primitive.affinity()
                        )
                    )
                )
            }
        }
    }

    @Test
    fun boxed() {
        ALL_BOXED_PRIMITIVES.forEach { boxedPrimitive ->
            val fieldName = boxedPrimitive.simpleNames.single()
            singleEntity("@Nullable $fieldName y;") { field, invocation ->
                assertThat(
                    field,
                    `is`(
                        Field(
                            name = "y",
                            type = boxedPrimitive.typeMirror(invocation).makeNullable(),
                            element = field.element,
                            affinity = boxedPrimitive.affinity()
                        )
                    )
                )
            }
        }
    }

    @Test
    fun columnName() {
        singleEntity(
            """
            @ColumnInfo(name = "foo")
            @PrimaryKey
            int x;
            """
        ) { field, invocation ->
            assertThat(
                field,
                `is`(
                    Field(
                        name = "x",
                        type = XTypeName.PRIMITIVE_INT.typeMirror(invocation),
                        element = field.element,
                        columnName = "foo",
                        affinity = SQLTypeAffinity.INTEGER
                    )
                )
            )
        }
    }

    @Test
    fun indexed() {
        singleEntity(
            """
            @ColumnInfo(name = "foo", index = true)
            int x;
            """
        ) { field, invocation ->
            assertThat(
                field,
                `is`(
                    Field(
                        name = "x",
                        type = XTypeName.PRIMITIVE_INT.typeMirror(invocation),
                        element = field.element,
                        columnName = "foo",
                        affinity = SQLTypeAffinity.INTEGER,
                        indexed = true
                    )
                )
            )
        }
    }

    @Test
    fun emptyColumnName() {
        singleEntity(
            """
            @ColumnInfo(name = "")
            int x;
            """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.COLUMN_NAME_CANNOT_BE_EMPTY)
            }
        }
    }

    @Test
    fun byteArrayWithEnforcedType() {
        singleEntity(
            "@TypeConverters(foo.bar.MyConverter.class)" +
                "@ColumnInfo(typeAffinity = ColumnInfo.TEXT) @NonNull byte[] arr;"
        ) { field, invocation ->
            assertThat(
                field,
                `is`(
                    Field(
                        name = "arr",
                        type =
                            invocation.processingEnv
                                .getArrayType(XTypeName.PRIMITIVE_BYTE)
                                .makeNonNullable(),
                        element = field.element,
                        affinity = SQLTypeAffinity.TEXT
                    )
                )
            )
            assertThat(
                (field.cursorValueReader as? ColumnTypeAdapter)?.typeAffinity,
                `is`(SQLTypeAffinity.TEXT)
            )
        }
    }

    @Test
    fun primitiveArray() {
        ALL_PRIMITIVES.forEach { primitive ->
            singleEntity(
                "@TypeConverters(foo.bar.MyConverter.class) @NonNull " +
                    "${primitive.toString(CodeLanguage.JAVA)}[] arr;"
            ) { field, invocation ->
                assertThat(
                    field,
                    `is`(
                        Field(
                            name = "arr",
                            type =
                                invocation.processingEnv.getArrayType(primitive).makeNonNullable(),
                            element = field.element,
                            affinity =
                                if (primitive == XTypeName.PRIMITIVE_BYTE) {
                                    SQLTypeAffinity.BLOB
                                } else {
                                    SQLTypeAffinity.TEXT
                                }
                        )
                    )
                )
            }
        }
    }

    @Test
    fun boxedArray() {
        ALL_BOXED_PRIMITIVES.forEach { boxedPrimitive ->
            singleEntity(
                "@TypeConverters(foo.bar.MyConverter.class) " +
                    "${boxedPrimitive.toString(CodeLanguage.JAVA)}[] arr;"
            ) { field, invocation ->
                val expected =
                    Field(
                        name = "arr",
                        type = invocation.processingEnv.getArrayType(boxedPrimitive),
                        element = field.element,
                        affinity = SQLTypeAffinity.TEXT,
                        nonNull = false // no annotation
                    )
                // When source is parsed, it will have a flexible type in KSP and we have no way of
                // obtaining a flexible type of:
                // (Array<(kotlin.Int..kotlin.Int?)>..Array<out (kotlin.Int..kotlin.Int?)>?)
                // as a workaround in test,
                assertThat(
                    field,
                    `is`(
                        expected.copy(
                            // don't compare type
                            type = field.type
                        )
                    )
                )
                assertThat(
                    field.type.asTypeName(),
                    `is`(
                        XTypeName.getArrayName(boxedPrimitive.copy(nullable = true))
                            .copy(nullable = true)
                    )
                )
            }
        }
    }

    @Test
    fun generic() {
        singleEntity(
            """
                static class BaseClass<T> {
                    @NonNull
                    T item;
                }
                @Entity
                static class Extending extends BaseClass<java.lang.Integer> {
                }
                """
        ) { field, invocation ->
            assertThat(
                field,
                `is`(
                    Field(
                        name = "item",
                        type = XTypeName.PRIMITIVE_INT.box(invocation).makeNonNullable(),
                        element = field.element,
                        affinity = SQLTypeAffinity.INTEGER
                    )
                )
            )
        }
    }

    @Test
    fun unboundGeneric() {
        singleEntity(
            """
                @Entity
                static class BaseClass<T> {
                    T item;
                }
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    // unbounded generics do not exist in KSP so the error is different
                    if (invocation.isKsp) {
                        ProcessorErrors.CANNOT_FIND_COLUMN_TYPE_ADAPTER
                    } else {
                        ProcessorErrors.CANNOT_USE_UNBOUND_GENERICS_IN_ENTITY_FIELDS
                    }
                )
            }
        }
    }

    @Test
    fun nameVariations() {
        runProcessorTest {
            val fieldElement = mock(XFieldElement::class.java)
            assertThat(
                Field(
                        fieldElement,
                        "x",
                        XTypeName.PRIMITIVE_INT.typeMirror(it),
                        SQLTypeAffinity.INTEGER
                    )
                    .nameWithVariations,
                `is`(arrayListOf("x"))
            )
            assertThat(
                Field(
                        fieldElement,
                        "x",
                        XTypeName.PRIMITIVE_BOOLEAN.typeMirror(it),
                        SQLTypeAffinity.INTEGER
                    )
                    .nameWithVariations,
                `is`(arrayListOf("x"))
            )
            assertThat(
                Field(
                        fieldElement,
                        "xAll",
                        XTypeName.PRIMITIVE_BOOLEAN.typeMirror(it),
                        SQLTypeAffinity.INTEGER
                    )
                    .nameWithVariations,
                `is`(arrayListOf("xAll"))
            )
        }
    }

    @Test
    fun nameVariations_is() {
        val elm = mock(XFieldElement::class.java)
        runProcessorTest {
            assertThat(
                Field(
                        elm,
                        "isX",
                        XTypeName.PRIMITIVE_BOOLEAN.typeMirror(it),
                        SQLTypeAffinity.INTEGER
                    )
                    .nameWithVariations,
                `is`(arrayListOf("isX", "x"))
            )
            assertThat(
                Field(elm, "isX", XTypeName.PRIMITIVE_INT.typeMirror(it), SQLTypeAffinity.INTEGER)
                    .nameWithVariations,
                `is`(arrayListOf("isX"))
            )
            assertThat(
                Field(
                        elm,
                        "is",
                        XTypeName.PRIMITIVE_BOOLEAN.typeMirror(it),
                        SQLTypeAffinity.INTEGER
                    )
                    .nameWithVariations,
                `is`(arrayListOf("is"))
            )
            assertThat(
                Field(
                        elm,
                        "isAllItems",
                        XTypeName.PRIMITIVE_BOOLEAN.typeMirror(it),
                        SQLTypeAffinity.INTEGER
                    )
                    .nameWithVariations,
                `is`(arrayListOf("isAllItems", "allItems"))
            )
        }
    }

    @Test
    fun nameVariations_has() {
        val elm = mock(XFieldElement::class.java)
        runProcessorTest {
            assertThat(
                Field(
                        elm,
                        "hasX",
                        XTypeName.PRIMITIVE_BOOLEAN.typeMirror(it),
                        SQLTypeAffinity.INTEGER
                    )
                    .nameWithVariations,
                `is`(arrayListOf("hasX", "x"))
            )
            assertThat(
                Field(elm, "hasX", XTypeName.PRIMITIVE_INT.typeMirror(it), SQLTypeAffinity.INTEGER)
                    .nameWithVariations,
                `is`(arrayListOf("hasX"))
            )
            assertThat(
                Field(
                        elm,
                        "has",
                        XTypeName.PRIMITIVE_BOOLEAN.typeMirror(it),
                        SQLTypeAffinity.INTEGER
                    )
                    .nameWithVariations,
                `is`(arrayListOf("has"))
            )
            assertThat(
                Field(
                        elm,
                        "hasAllItems",
                        XTypeName.PRIMITIVE_BOOLEAN.typeMirror(it),
                        SQLTypeAffinity.INTEGER
                    )
                    .nameWithVariations,
                `is`(arrayListOf("hasAllItems", "allItems"))
            )
        }
    }

    @Test
    fun nameVariations_m() {
        val elm = mock(XFieldElement::class.java)
        runProcessorTest {
            assertThat(
                Field(
                        elm,
                        "mall",
                        XTypeName.PRIMITIVE_BOOLEAN.typeMirror(it),
                        SQLTypeAffinity.INTEGER
                    )
                    .nameWithVariations,
                `is`(arrayListOf("mall"))
            )
            assertThat(
                Field(
                        elm,
                        "mallVars",
                        XTypeName.PRIMITIVE_BOOLEAN.typeMirror(it),
                        SQLTypeAffinity.INTEGER
                    )
                    .nameWithVariations,
                `is`(arrayListOf("mallVars"))
            )
            assertThat(
                Field(
                        elm,
                        "mAll",
                        XTypeName.PRIMITIVE_BOOLEAN.typeMirror(it),
                        SQLTypeAffinity.INTEGER
                    )
                    .nameWithVariations,
                `is`(arrayListOf("mAll", "all"))
            )
            assertThat(
                Field(elm, "m", XTypeName.PRIMITIVE_INT.typeMirror(it), SQLTypeAffinity.INTEGER)
                    .nameWithVariations,
                `is`(arrayListOf("m"))
            )
            assertThat(
                Field(
                        elm,
                        "mallItems",
                        XTypeName.PRIMITIVE_BOOLEAN.typeMirror(it),
                        SQLTypeAffinity.INTEGER
                    )
                    .nameWithVariations,
                `is`(arrayListOf("mallItems"))
            )
            assertThat(
                Field(
                        elm,
                        "mAllItems",
                        XTypeName.PRIMITIVE_BOOLEAN.typeMirror(it),
                        SQLTypeAffinity.INTEGER
                    )
                    .nameWithVariations,
                `is`(arrayListOf("mAllItems", "allItems"))
            )
        }
    }

    @Test
    fun nameVariations_underscore() {
        val elm = mock(XFieldElement::class.java)
        runProcessorTest {
            assertThat(
                Field(
                        elm,
                        "_all",
                        XTypeName.PRIMITIVE_BOOLEAN.typeMirror(it),
                        SQLTypeAffinity.INTEGER
                    )
                    .nameWithVariations,
                `is`(arrayListOf("_all", "all"))
            )
            assertThat(
                Field(elm, "_", XTypeName.PRIMITIVE_INT.typeMirror(it), SQLTypeAffinity.INTEGER)
                    .nameWithVariations,
                `is`(arrayListOf("_"))
            )
            assertThat(
                Field(
                        elm,
                        "_allItems",
                        XTypeName.PRIMITIVE_BOOLEAN.typeMirror(it),
                        SQLTypeAffinity.INTEGER
                    )
                    .nameWithVariations,
                `is`(arrayListOf("_allItems", "allItems"))
            )
        }
    }

    @Test
    fun collate() {
        Collate.values().forEach { collate ->
            singleEntity(
                """
            @PrimaryKey
            @ColumnInfo(collate = ColumnInfo.${collate.name})
            @Nullable
            String code;
            """
            ) { field, invocation ->
                assertThat(
                    field,
                    `is`(
                        Field(
                            name = "code",
                            type =
                                invocation.context.processingEnv
                                    .requireType(CommonTypeNames.STRING)
                                    .makeNullable(),
                            element = field.element,
                            columnName = "code",
                            collate = collate,
                            affinity = SQLTypeAffinity.TEXT
                        )
                    )
                )
            }
        }
    }

    @Test
    fun defaultValues_number() {
        testDefaultValue("\"1\"", "int") { defaultValue, _ ->
            assertThat(defaultValue, `is`(equalTo("1")))
        }
        testDefaultValue("\"\"", "int") { defaultValue, _ ->
            assertThat(defaultValue, `is`(nullValue()))
        }
        testDefaultValue("\"null\"", "Integer") { defaultValue, _ ->
            assertThat(defaultValue, `is`(equalTo("null")))
        }
        testDefaultValue("ColumnInfo.VALUE_UNSPECIFIED", "int") { defaultValue, _ ->
            assertThat(defaultValue, `is`(nullValue()))
        }
        testDefaultValue("\"CURRENT_TIMESTAMP\"", "long") { defaultValue, _ ->
            assertThat(defaultValue, `is`(equalTo("CURRENT_TIMESTAMP")))
        }
        testDefaultValue("\"true\"", "boolean") { defaultValue, _ ->
            assertThat(defaultValue, `is`(equalTo("true")))
        }
        testDefaultValue("\"false\"", "boolean") { defaultValue, _ ->
            assertThat(defaultValue, `is`(equalTo("false")))
        }
    }

    @Test
    fun defaultValues_nonNull() {
        testDefaultValue("\"null\"", "int") { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.DEFAULT_VALUE_NULLABILITY)
            }
        }
        testDefaultValue("\"null\"", "@NonNull String") { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.DEFAULT_VALUE_NULLABILITY)
            }
        }
    }

    @Test
    fun defaultValues_text() {
        testDefaultValue("\"a\"", "String") { defaultValue, _ ->
            assertThat(defaultValue, `is`(equalTo("'a'")))
        }
        testDefaultValue("\"'a'\"", "String") { defaultValue, _ ->
            assertThat(defaultValue, `is`(equalTo("'a'")))
        }
        testDefaultValue("\"\"", "String") { defaultValue, _ ->
            assertThat(defaultValue, `is`(equalTo("''")))
        }
        testDefaultValue("\"null\"", "String") { defaultValue, _ ->
            assertThat(defaultValue, `is`(equalTo("null")))
        }
        testDefaultValue("ColumnInfo.VALUE_UNSPECIFIED", "String") { defaultValue, _ ->
            assertThat(defaultValue, `is`(nullValue()))
        }
        testDefaultValue("\"CURRENT_TIMESTAMP\"", "String") { defaultValue, _ ->
            assertThat(defaultValue, `is`(equalTo("CURRENT_TIMESTAMP")))
        }
        testDefaultValue(defaultValue = "\"('Created at ' || CURRENT_TIMESTAMP)\"", "String") {
            defaultValue,
            _ ->
            assertThat(defaultValue, `is`(equalTo("('Created at ' || CURRENT_TIMESTAMP)")))
        }
    }

    private fun testDefaultValue(
        defaultValue: String,
        fieldType: String,
        body: (String?, XTestInvocation) -> Unit
    ) {
        singleEntity(
            """
                @ColumnInfo(defaultValue = $defaultValue)
                $fieldType name;
            """
        ) { field, invocation ->
            body(field.defaultValue, invocation)
        }
    }

    fun singleEntity(vararg input: String, handler: (Field, invocation: XTestInvocation) -> Unit) {
        val sources =
            listOf(
                Source.java(
                    "foo.bar.MyEntity",
                    ENTITY_PREFIX + input.joinToString("\n") + ENTITY_SUFFIX
                ),
                ARRAY_CONVERTER
            )
        runProcessorTestWithK1(sources = sources) { invocation ->
            val (owner, fieldElement) =
                invocation.roundEnv
                    .getElementsAnnotatedWith(Entity::class.qualifiedName!!)
                    .filterIsInstance<XTypeElement>()
                    .map { Pair(it, it.getAllFieldsIncludingPrivateSupers().firstOrNull()) }
                    .first { it.second != null }
            val entityContext =
                TableEntityProcessor(baseContext = invocation.context, element = owner).context
            val parser =
                FieldProcessor(
                    baseContext = entityContext,
                    containing = owner.type,
                    element = fieldElement!!,
                    bindingScope = FieldProcessor.BindingScope.TWO_WAY,
                    fieldParent = null,
                    onBindingError = { field, errorMsg ->
                        invocation.context.logger.e(field.element, errorMsg)
                    }
                )
            handler(parser.process(), invocation)
        }
    }
}
