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
import androidx.room.compiler.processing.XFieldElement
import androidx.room.parser.Collate
import androidx.room.parser.SQLTypeAffinity
import androidx.room.solver.types.ColumnTypeAdapter
import androidx.room.testing.TestInvocation
import androidx.room.testing.TestProcessor
import androidx.room.vo.Field
import com.google.common.truth.Truth
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubjectFactory
import com.squareup.javapoet.TypeName
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.mock
import simpleRun
import java.util.Locale

@Suppress("HasPlatformType")
@RunWith(JUnit4::class)
class FieldProcessorTest {
    companion object {
        const val ENTITY_PREFIX = """
                package foo.bar;
                import androidx.room.*;
                import androidx.annotation.NonNull;
                @Entity
                abstract class MyEntity {
                """
        const val ENTITY_SUFFIX = "}"
        val ALL_PRIMITIVES = arrayListOf(
            TypeName.INT,
            TypeName.BYTE,
            TypeName.SHORT,
            TypeName.LONG,
            TypeName.CHAR,
            TypeName.FLOAT,
            TypeName.DOUBLE
        )
        val ARRAY_CONVERTER = JavaFileObjects.forSourceLines(
            "foo.bar.MyConverter",
            """
                package foo.bar;
                import androidx.room.*;
                public class MyConverter {
                ${ALL_PRIMITIVES.joinToString("\n") {
                val arrayDef = "$it[]"
                "@TypeConverter public static String" +
                    " arrayIntoString($arrayDef input) { return null;}" +
                    "@TypeConverter public static $arrayDef" +
                    " stringIntoArray$it(String input) { return null;}"
            }}
                ${ALL_PRIMITIVES.joinToString("\n") {
                val arrayDef = "${it.box()}[]"
                "@TypeConverter public static String" +
                    " arrayIntoString($arrayDef input) { return null;}" +
                    "@TypeConverter public static $arrayDef" +
                    " stringIntoArray${it}Boxed(String input) { return null;}"
            }}
                }
                """
        )

        private fun TypeName.affinity(): SQLTypeAffinity {
            return when (this) {
                TypeName.FLOAT, TypeName.DOUBLE -> SQLTypeAffinity.REAL
                else -> SQLTypeAffinity.INTEGER
            }
        }

        private fun TypeName.box(invocation: TestInvocation) =
            typeMirror(invocation).boxed()

        private fun TypeName.typeMirror(invocation: TestInvocation) =
            invocation.processingEnv.requireType(this)
    }

    @Test
    fun primitives() {
        ALL_PRIMITIVES.forEach { primitive ->
            singleEntity("$primitive x;") { field, invocation ->
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
            }.compilesWithoutError()
        }
    }

    @Test
    fun boxed() {
        ALL_PRIMITIVES.forEach { primitive ->
            singleEntity("${primitive.box()} y;") { field, invocation ->
                assertThat(
                    field,
                    `is`(
                        Field(
                            name = "y",
                            type = primitive.box(invocation),
                            element = field.element,
                            affinity = primitive.affinity()
                        )
                    )
                )
            }.compilesWithoutError()
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
                        type = TypeName.INT.typeMirror(invocation),
                        element = field.element,
                        columnName = "foo",
                        affinity = SQLTypeAffinity.INTEGER
                    )
                )
            )
        }.compilesWithoutError()
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
                        type = TypeName.INT.typeMirror(invocation),
                        element = field.element,
                        columnName = "foo",
                        affinity = SQLTypeAffinity.INTEGER,
                        indexed = true
                    )
                )
            )
        }.compilesWithoutError()
    }

    @Test
    fun emptyColumnName() {
        singleEntity(
            """
            @ColumnInfo(name = "")
            int x;
            """
        ) { _, _ ->
        }.failsToCompile().withErrorContaining(ProcessorErrors.COLUMN_NAME_CANNOT_BE_EMPTY)
    }

    @Test
    fun byteArrayWithEnforcedType() {
        singleEntity(
            "@TypeConverters(foo.bar.MyConverter.class)" +
                "@ColumnInfo(typeAffinity = ColumnInfo.TEXT) byte[] arr;"
        ) { field, invocation ->
            assertThat(
                field,
                `is`(
                    Field(
                        name = "arr",
                        type = invocation.processingEnv.getArrayType(TypeName.BYTE),
                        element = field.element,
                        affinity = SQLTypeAffinity.TEXT
                    )
                )
            )
            assertThat(
                (field.cursorValueReader as? ColumnTypeAdapter)?.typeAffinity,
                `is`(SQLTypeAffinity.TEXT)
            )
        }.compilesWithoutError()
    }

    @Test
    fun primitiveArray() {
        ALL_PRIMITIVES.forEach { primitive ->
            singleEntity(
                "@TypeConverters(foo.bar.MyConverter.class) " +
                    "${primitive.toString().toLowerCase(Locale.US)}[] arr;"
            ) { field, invocation ->
                assertThat(
                    field,
                    `is`(
                        Field(
                            name = "arr",
                            type = invocation.processingEnv.getArrayType(primitive),
                            element = field.element,
                            affinity = if (primitive == TypeName.BYTE) {
                                SQLTypeAffinity.BLOB
                            } else {
                                SQLTypeAffinity.TEXT
                            }
                        )
                    )
                )
            }.compilesWithoutError()
        }
    }

    @Test
    fun boxedArray() {
        ALL_PRIMITIVES.forEach { primitive ->
            singleEntity(
                "@TypeConverters(foo.bar.MyConverter.class) " +
                    "${primitive.box()}[] arr;"
            ) { field, invocation ->
                assertThat(
                    field,
                    `is`(
                        Field(
                            name = "arr",
                            type = invocation.processingEnv.getArrayType(
                                primitive.box()
                            ),
                            element = field.element,
                            affinity = SQLTypeAffinity.TEXT
                        )
                    )
                )
            }.compilesWithoutError()
        }
    }

    @Test
    fun generic() {
        singleEntity(
            """
                static class BaseClass<T> {
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
                        type = TypeName.INT.box(invocation),
                        element = field.element,
                        affinity = SQLTypeAffinity.INTEGER
                    )
                )
            )
        }.compilesWithoutError()
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
        ) { _, _ -> }.failsToCompile()
            .withErrorContaining(ProcessorErrors.CANNOT_USE_UNBOUND_GENERICS_IN_ENTITY_FIELDS)
    }

    @Test
    fun nameVariations() {
        simpleRun {
            val fieldElement = mock(XFieldElement::class.java)
            assertThat(
                Field(
                    fieldElement, "x", TypeName.INT.typeMirror(it),
                    SQLTypeAffinity.INTEGER
                ).nameWithVariations,
                `is`(arrayListOf("x"))
            )
            assertThat(
                Field(
                    fieldElement, "x", TypeName.BOOLEAN.typeMirror(it),
                    SQLTypeAffinity.INTEGER
                ).nameWithVariations,
                `is`(arrayListOf("x"))
            )
            assertThat(
                Field(
                    fieldElement, "xAll",
                    TypeName.BOOLEAN.typeMirror(it), SQLTypeAffinity.INTEGER
                )
                    .nameWithVariations,
                `is`(arrayListOf("xAll"))
            )
        }
    }

    @Test
    fun nameVariations_is() {
        val elm = mock(XFieldElement::class.java)
        simpleRun {
            assertThat(
                Field(
                    elm, "isX", TypeName.BOOLEAN.typeMirror(it),
                    SQLTypeAffinity.INTEGER
                ).nameWithVariations,
                `is`(arrayListOf("isX", "x"))
            )
            assertThat(
                Field(
                    elm, "isX", TypeName.INT.typeMirror(it),
                    SQLTypeAffinity.INTEGER
                ).nameWithVariations,
                `is`(arrayListOf("isX"))
            )
            assertThat(
                Field(
                    elm, "is", TypeName.BOOLEAN.typeMirror(it),
                    SQLTypeAffinity.INTEGER
                ).nameWithVariations,
                `is`(arrayListOf("is"))
            )
            assertThat(
                Field(
                    elm, "isAllItems", TypeName.BOOLEAN.typeMirror(it),
                    SQLTypeAffinity.INTEGER
                ).nameWithVariations,
                `is`(arrayListOf("isAllItems", "allItems"))
            )
        }
    }

    @Test
    fun nameVariations_has() {
        val elm = mock(XFieldElement::class.java)
        simpleRun {
            assertThat(
                Field(
                    elm, "hasX", TypeName.BOOLEAN.typeMirror(it),
                    SQLTypeAffinity.INTEGER
                ).nameWithVariations,
                `is`(arrayListOf("hasX", "x"))
            )
            assertThat(
                Field(
                    elm, "hasX", TypeName.INT.typeMirror(it),
                    SQLTypeAffinity.INTEGER
                ).nameWithVariations,
                `is`(arrayListOf("hasX"))
            )
            assertThat(
                Field(
                    elm, "has", TypeName.BOOLEAN.typeMirror(it),
                    SQLTypeAffinity.INTEGER
                ).nameWithVariations,
                `is`(arrayListOf("has"))
            )
            assertThat(
                Field(
                    elm, "hasAllItems", TypeName.BOOLEAN.typeMirror(it),
                    SQLTypeAffinity.INTEGER
                ).nameWithVariations,
                `is`(arrayListOf("hasAllItems", "allItems"))
            )
        }
    }

    @Test
    fun nameVariations_m() {
        val elm = mock(XFieldElement::class.java)
        simpleRun {
            assertThat(
                Field(
                    elm, "mall", TypeName.BOOLEAN.typeMirror(it),
                    SQLTypeAffinity.INTEGER
                ).nameWithVariations,
                `is`(arrayListOf("mall"))
            )
            assertThat(
                Field(
                    elm, "mallVars", TypeName.BOOLEAN.typeMirror(it),
                    SQLTypeAffinity.INTEGER
                ).nameWithVariations,
                `is`(arrayListOf("mallVars"))
            )
            assertThat(
                Field(
                    elm, "mAll", TypeName.BOOLEAN.typeMirror(it),
                    SQLTypeAffinity.INTEGER
                ).nameWithVariations,
                `is`(arrayListOf("mAll", "all"))
            )
            assertThat(
                Field(
                    elm, "m", TypeName.INT.typeMirror(it),
                    SQLTypeAffinity.INTEGER
                ).nameWithVariations,
                `is`(arrayListOf("m"))
            )
            assertThat(
                Field(
                    elm, "mallItems", TypeName.BOOLEAN.typeMirror(it),
                    SQLTypeAffinity.INTEGER
                ).nameWithVariations,
                `is`(arrayListOf("mallItems"))
            )
            assertThat(
                Field(
                    elm, "mAllItems", TypeName.BOOLEAN.typeMirror(it),
                    SQLTypeAffinity.INTEGER
                ).nameWithVariations,
                `is`(arrayListOf("mAllItems", "allItems"))
            )
        }
    }

    @Test
    fun nameVariations_underscore() {
        val elm = mock(XFieldElement::class.java)
        simpleRun {
            assertThat(
                Field(
                    elm, "_all", TypeName.BOOLEAN.typeMirror(it),
                    SQLTypeAffinity.INTEGER
                ).nameWithVariations,
                `is`(arrayListOf("_all", "all"))
            )
            assertThat(
                Field(
                    elm, "_", TypeName.INT.typeMirror(it),
                    SQLTypeAffinity.INTEGER
                ).nameWithVariations,
                `is`(arrayListOf("_"))
            )
            assertThat(
                Field(
                    elm, "_allItems", TypeName.BOOLEAN.typeMirror(it),
                    SQLTypeAffinity.INTEGER
                ).nameWithVariations,
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
            String code;
            """
            ) { field, invocation ->
                assertThat(
                    field,
                    `is`(
                        Field(
                            name = "code",
                            type = invocation.context.COMMON_TYPES.STRING,
                            element = field.element,
                            columnName = "code",
                            collate = collate,
                            affinity = SQLTypeAffinity.TEXT
                        )
                    )
                )
            }.compilesWithoutError()
        }
    }

    @Test
    fun defaultValues_number() {
        testDefaultValue("\"1\"", "int") { defaultValue ->
            assertThat(defaultValue, `is`(equalTo("1")))
        }.compilesWithoutError()
        testDefaultValue("\"\"", "int") { defaultValue ->
            assertThat(defaultValue, `is`(nullValue()))
        }.compilesWithoutError()
        testDefaultValue("\"null\"", "Integer") { defaultValue ->
            assertThat(defaultValue, `is`(equalTo("null")))
        }.compilesWithoutError()
        testDefaultValue("ColumnInfo.VALUE_UNSPECIFIED", "int") { defaultValue ->
            assertThat(defaultValue, `is`(nullValue()))
        }.compilesWithoutError()
        testDefaultValue("\"CURRENT_TIMESTAMP\"", "long") { defaultValue ->
            assertThat(defaultValue, `is`(equalTo("CURRENT_TIMESTAMP")))
        }.compilesWithoutError()
        testDefaultValue("\"true\"", "boolean") { defaultValue ->
            assertThat(defaultValue, `is`(equalTo("true")))
        }.compilesWithoutError()
        testDefaultValue("\"false\"", "boolean") { defaultValue ->
            assertThat(defaultValue, `is`(equalTo("false")))
        }.compilesWithoutError()
    }

    @Test
    fun defaultValues_nonNull() {
        testDefaultValue("\"null\"", "int") {
        }.failsToCompile().withErrorContaining(ProcessorErrors.DEFAULT_VALUE_NULLABILITY)
        testDefaultValue("\"null\"", "@NonNull String") {
        }.failsToCompile().withErrorContaining(ProcessorErrors.DEFAULT_VALUE_NULLABILITY)
    }

    @Test
    fun defaultValues_text() {
        testDefaultValue("\"a\"", "String") { defaultValue ->
            assertThat(defaultValue, `is`(equalTo("'a'")))
        }.compilesWithoutError()
        testDefaultValue("\"'a'\"", "String") { defaultValue ->
            assertThat(defaultValue, `is`(equalTo("'a'")))
        }.compilesWithoutError()
        testDefaultValue("\"\"", "String") { defaultValue ->
            assertThat(defaultValue, `is`(equalTo("''")))
        }.compilesWithoutError()
        testDefaultValue("\"null\"", "String") { defaultValue ->
            assertThat(defaultValue, `is`(equalTo("null")))
        }.compilesWithoutError()
        testDefaultValue("ColumnInfo.VALUE_UNSPECIFIED", "String") { defaultValue ->
            assertThat(defaultValue, `is`(nullValue()))
        }.compilesWithoutError()
        testDefaultValue("\"CURRENT_TIMESTAMP\"", "String") { defaultValue ->
            assertThat(defaultValue, `is`(equalTo("CURRENT_TIMESTAMP")))
        }.compilesWithoutError()
        testDefaultValue("\"('Created at ' || CURRENT_TIMESTAMP)\"", "String") { defaultValue ->
            assertThat(defaultValue, `is`(equalTo("('Created at ' || CURRENT_TIMESTAMP)")))
        }.compilesWithoutError()
    }

    private fun testDefaultValue(
        defaultValue: String,
        fieldType: String,
        body: (String?) -> Unit
    ): CompileTester {
        return singleEntity(
            """
                @ColumnInfo(defaultValue = $defaultValue)
                $fieldType name;
            """
        ) { field, _ ->
            body(field.defaultValue)
        }
    }

    fun singleEntity(vararg input: String, handler: (Field, invocation: TestInvocation) -> Unit):
        CompileTester {
            return Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
                .that(
                    listOf(
                        JavaFileObjects.forSourceString(
                            "foo.bar.MyEntity",
                            ENTITY_PREFIX + input.joinToString("\n") + ENTITY_SUFFIX
                        ),
                        ARRAY_CONVERTER
                    )
                )
                .processedWith(
                    TestProcessor.builder()
                        .forAnnotations(androidx.room.Entity::class)
                        .nextRunHandler { invocation ->
                            val (owner, fieldElement) = invocation.roundEnv
                                .getElementsAnnotatedWith(Entity::class.java)
                                .map {
                                    Pair(
                                        it,
                                        it.asTypeElement()
                                            .getAllFieldsIncludingPrivateSupers().firstOrNull()
                                    )
                                }
                                .first { it.second != null }
                            val entityContext =
                                TableEntityProcessor(
                                    baseContext = invocation.context,
                                    element = owner.asTypeElement()
                                ).context
                            val parser = FieldProcessor(
                                baseContext = entityContext,
                                containing = owner.asDeclaredType(),
                                element = fieldElement!!,
                                bindingScope = FieldProcessor.BindingScope.TWO_WAY,
                                fieldParent = null,
                                onBindingError = { field, errorMsg ->
                                    invocation.context.logger.e(field.element, errorMsg)
                                }
                            )
                            handler(parser.process(), invocation)
                            true
                        }
                        .build()
                )
        }
}
