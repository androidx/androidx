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
import androidx.room.parser.Collate
import androidx.room.parser.SQLTypeAffinity
import androidx.room.solver.types.ColumnTypeAdapter
import androidx.room.testing.TestInvocation
import androidx.room.testing.TestProcessor
import androidx.room.vo.Field
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.google.common.truth.Truth
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubjectFactory
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.mock
import simpleRun
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

@Suppress("HasPlatformType")
@RunWith(JUnit4::class)
class FieldProcessorTest {
    companion object {
        const val ENTITY_PREFIX = """
                package foo.bar;
                import androidx.room.*;
                @Entity
                abstract class MyEntity {
                """
        const val ENTITY_SUFFIX = "}"
        val ALL_PRIMITIVES = arrayListOf(
                TypeKind.INT,
                TypeKind.BYTE,
                TypeKind.SHORT,
                TypeKind.LONG,
                TypeKind.CHAR,
                TypeKind.FLOAT,
                TypeKind.DOUBLE)
        val ARRAY_CONVERTER = JavaFileObjects.forSourceLines("foo.bar.MyConverter",
                """
                package foo.bar;
                import androidx.room.*;
                public class MyConverter {
                ${ALL_PRIMITIVES.joinToString("\n") {
                    val arrayDef = "${it.name.toLowerCase()}[]"
                    "@TypeConverter public static String" +
                            " arrayIntoString($arrayDef input) { return null;}" +
                            "@TypeConverter public static $arrayDef" +
                            " stringIntoArray${it.name}(String input) { return null;}"
                }}
                ${ALL_PRIMITIVES.joinToString("\n") {
                    val arrayDef = "${it.box()}[]"
                    "@TypeConverter public static String" +
                            " arrayIntoString($arrayDef input) { return null;}" +
                            "@TypeConverter public static $arrayDef" +
                            " stringIntoArray${it.name}Boxed(String input) { return null;}"
                }}
                }
                """)

        private fun TypeKind.box(): String {
            return "java.lang." + when (this) {
                TypeKind.INT -> "Integer"
                TypeKind.CHAR -> "Character"
                else -> this.name.toLowerCase().capitalize()
            }
        }

        // these 2 box methods are ugly but makes tests nicer and they are private
        private fun TypeKind.typeMirror(invocation: TestInvocation): TypeMirror {
            return invocation.processingEnv.typeUtils.getPrimitiveType(this)
        }

        private fun TypeKind.affinity(): SQLTypeAffinity {
            return when (this) {
                TypeKind.FLOAT, TypeKind.DOUBLE -> SQLTypeAffinity.REAL
                else -> SQLTypeAffinity.INTEGER
            }
        }

        private fun TypeKind.box(invocation: TestInvocation): TypeMirror {
            return invocation.processingEnv.elementUtils.getTypeElement(box()).asType()
        }
    }

    @Test
    fun primitives() {
        ALL_PRIMITIVES.forEach { primitive ->
            singleEntity("${primitive.name.toLowerCase()} x;") { field, invocation ->
                assertThat(field, `is`(
                        Field(name = "x",
                                type = primitive.typeMirror(invocation),
                                element = field.element,
                                affinity = primitive.affinity()
                        )))
            }.compilesWithoutError()
        }
    }

    @Test
    fun boxed() {
        ALL_PRIMITIVES.forEach { primitive ->
            singleEntity("${primitive.box()} y;") { field, invocation ->
                assertThat(field, `is`(
                        Field(name = "y",
                                type = primitive.box(invocation),
                                element = field.element,
                                affinity = primitive.affinity())))
            }.compilesWithoutError()
        }
    }

    @Test
    fun columnName() {
        singleEntity("""
            @ColumnInfo(name = "foo")
            @PrimaryKey
            int x;
            """) { field, invocation ->
            assertThat(field, `is`(
                    Field(name = "x",
                            type = TypeKind.INT.typeMirror(invocation),
                            element = field.element,
                            columnName = "foo",
                            affinity = SQLTypeAffinity.INTEGER)))
        }.compilesWithoutError()
    }

    @Test
    fun indexed() {
        singleEntity("""
            @ColumnInfo(name = "foo", index = true)
            int x;
            """) { field, invocation ->
            assertThat(field, `is`(
                    Field(name = "x",
                            type = TypeKind.INT.typeMirror(invocation),
                            element = field.element,
                            columnName = "foo",
                            affinity = SQLTypeAffinity.INTEGER,
                            indexed = true)))
        }.compilesWithoutError()
    }

    @Test
    fun emptyColumnName() {
        singleEntity("""
            @ColumnInfo(name = "")
            int x;
            """) { _, _ ->
        }.failsToCompile().withErrorContaining(ProcessorErrors.COLUMN_NAME_CANNOT_BE_EMPTY)
    }

    @Test
    fun byteArrayWithEnforcedType() {
        singleEntity("@TypeConverters(foo.bar.MyConverter.class)" +
                "@ColumnInfo(typeAffinity = ColumnInfo.TEXT) byte[] arr;") { field, invocation ->
            assertThat(field, `is`(Field(name = "arr",
                    type = invocation.processingEnv.typeUtils.getArrayType(
                            TypeKind.BYTE.typeMirror(invocation)),
                    element = field.element,
                    affinity = SQLTypeAffinity.TEXT)))
            assertThat((field.cursorValueReader as? ColumnTypeAdapter)?.typeAffinity,
                    `is`(SQLTypeAffinity.TEXT))
        }.compilesWithoutError()
    }

    @Test
    fun primitiveArray() {
        ALL_PRIMITIVES.forEach { primitive ->
            singleEntity("@TypeConverters(foo.bar.MyConverter.class) " +
                    "${primitive.toString().toLowerCase()}[] arr;") { field, invocation ->
                assertThat(field, `is`(
                        Field(name = "arr",
                                type = invocation.processingEnv.typeUtils.getArrayType(
                                        primitive.typeMirror(invocation)),
                                element = field.element,
                                affinity = if (primitive == TypeKind.BYTE) {
                                    SQLTypeAffinity.BLOB
                                } else {
                                    SQLTypeAffinity.TEXT
                                })))
            }.compilesWithoutError()
        }
    }

    @Test
    fun boxedArray() {
        ALL_PRIMITIVES.forEach { primitive ->
            singleEntity("@TypeConverters(foo.bar.MyConverter.class) " +
                    "${primitive.box()}[] arr;") { field, invocation ->
                assertThat(field, `is`(
                        Field(name = "arr",
                                type = invocation.processingEnv.typeUtils.getArrayType(
                                        primitive.box(invocation)),
                                element = field.element,
                                affinity = SQLTypeAffinity.TEXT)))
            }.compilesWithoutError()
        }
    }

    @Test
    fun generic() {
        singleEntity("""
                static class BaseClass<T> {
                    T item;
                }
                @Entity
                static class Extending extends BaseClass<java.lang.Integer> {
                }
                """) { field, invocation ->
            assertThat(field, `is`(Field(name = "item",
                    type = TypeKind.INT.box(invocation),
                    element = field.element,
                    affinity = SQLTypeAffinity.INTEGER)))
        }.compilesWithoutError()
    }

    @Test
    fun unboundGeneric() {
        singleEntity("""
                @Entity
                static class BaseClass<T> {
                    T item;
                }
                """) { _, _ -> }.failsToCompile()
                .withErrorContaining(ProcessorErrors.CANNOT_USE_UNBOUND_GENERICS_IN_ENTITY_FIELDS)
    }

    @Test
    fun nameVariations() {
        simpleRun {
            assertThat(Field(mock(Element::class.java), "x", TypeKind.INT.typeMirror(it),
                    SQLTypeAffinity.INTEGER).nameWithVariations, `is`(arrayListOf("x")))
            assertThat(Field(mock(Element::class.java), "x", TypeKind.BOOLEAN.typeMirror(it),
                    SQLTypeAffinity.INTEGER).nameWithVariations, `is`(arrayListOf("x")))
            assertThat(Field(mock(Element::class.java), "xAll",
                    TypeKind.BOOLEAN.typeMirror(it), SQLTypeAffinity.INTEGER)
                    .nameWithVariations, `is`(arrayListOf("xAll")))
        }
    }

    @Test
    fun nameVariations_is() {
        val elm = mock(Element::class.java)
        simpleRun {
            assertThat(Field(elm, "isX", TypeKind.BOOLEAN.typeMirror(it),
                    SQLTypeAffinity.INTEGER).nameWithVariations, `is`(arrayListOf("isX", "x")))
            assertThat(Field(elm, "isX", TypeKind.INT.typeMirror(it),
                    SQLTypeAffinity.INTEGER).nameWithVariations, `is`(arrayListOf("isX")))
            assertThat(Field(elm, "is", TypeKind.BOOLEAN.typeMirror(it),
                    SQLTypeAffinity.INTEGER).nameWithVariations, `is`(arrayListOf("is")))
            assertThat(Field(elm, "isAllItems", TypeKind.BOOLEAN.typeMirror(it),
                    SQLTypeAffinity.INTEGER).nameWithVariations,
                    `is`(arrayListOf("isAllItems", "allItems")))
        }
    }

    @Test
    fun nameVariations_has() {
        val elm = mock(Element::class.java)
        simpleRun {
            assertThat(Field(elm, "hasX", TypeKind.BOOLEAN.typeMirror(it),
                    SQLTypeAffinity.INTEGER).nameWithVariations, `is`(arrayListOf("hasX", "x")))
            assertThat(Field(elm, "hasX", TypeKind.INT.typeMirror(it),
                    SQLTypeAffinity.INTEGER).nameWithVariations, `is`(arrayListOf("hasX")))
            assertThat(Field(elm, "has", TypeKind.BOOLEAN.typeMirror(it),
                    SQLTypeAffinity.INTEGER).nameWithVariations, `is`(arrayListOf("has")))
            assertThat(Field(elm, "hasAllItems", TypeKind.BOOLEAN.typeMirror(it),
                    SQLTypeAffinity.INTEGER).nameWithVariations,
                    `is`(arrayListOf("hasAllItems", "allItems")))
        }
    }

    @Test
    fun nameVariations_m() {
        val elm = mock(Element::class.java)
        simpleRun {
            assertThat(Field(elm, "mall", TypeKind.BOOLEAN.typeMirror(it),
                    SQLTypeAffinity.INTEGER).nameWithVariations, `is`(arrayListOf("mall")))
            assertThat(Field(elm, "mallVars", TypeKind.BOOLEAN.typeMirror(it),
                    SQLTypeAffinity.INTEGER).nameWithVariations, `is`(arrayListOf("mallVars")))
            assertThat(Field(elm, "mAll", TypeKind.BOOLEAN.typeMirror(it),
                    SQLTypeAffinity.INTEGER).nameWithVariations, `is`(arrayListOf("mAll", "all")))
            assertThat(Field(elm, "m", TypeKind.INT.typeMirror(it),
                    SQLTypeAffinity.INTEGER).nameWithVariations, `is`(arrayListOf("m")))
            assertThat(Field(elm, "mallItems", TypeKind.BOOLEAN.typeMirror(it),
                    SQLTypeAffinity.INTEGER).nameWithVariations,
                    `is`(arrayListOf("mallItems")))
            assertThat(Field(elm, "mAllItems", TypeKind.BOOLEAN.typeMirror(it),
                    SQLTypeAffinity.INTEGER).nameWithVariations,
                    `is`(arrayListOf("mAllItems", "allItems")))
        }
    }

    @Test
    fun nameVariations_underscore() {
        val elm = mock(Element::class.java)
        simpleRun {
            assertThat(Field(elm, "_all", TypeKind.BOOLEAN.typeMirror(it),
                    SQLTypeAffinity.INTEGER).nameWithVariations, `is`(arrayListOf("_all", "all")))
            assertThat(Field(elm, "_", TypeKind.INT.typeMirror(it),
                    SQLTypeAffinity.INTEGER).nameWithVariations, `is`(arrayListOf("_")))
            assertThat(Field(elm, "_allItems", TypeKind.BOOLEAN.typeMirror(it),
                    SQLTypeAffinity.INTEGER).nameWithVariations,
                    `is`(arrayListOf("_allItems", "allItems")))
        }
    }

    @Test
    fun collate() {
        Collate.values().forEach { collate ->
            singleEntity("""
            @PrimaryKey
            @ColumnInfo(collate = ColumnInfo.${collate.name})
            String code;
            """) { field, invocation ->
                assertThat(field, `is`(
                        Field(name = "code",
                                type = invocation.context.COMMON_TYPES.STRING,
                                element = field.element,
                                columnName = "code",
                                collate = collate,
                                affinity = SQLTypeAffinity.TEXT)))
            }.compilesWithoutError()
        }
    }

    fun singleEntity(vararg input: String, handler: (Field, invocation: TestInvocation) -> Unit):
            CompileTester {
        return Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
                .that(listOf(JavaFileObjects.forSourceString("foo.bar.MyEntity",
                        ENTITY_PREFIX + input.joinToString("\n") + ENTITY_SUFFIX
                ), ARRAY_CONVERTER))
                .processedWith(TestProcessor.builder()
                        .forAnnotations(androidx.room.Entity::class)
                        .nextRunHandler { invocation ->
                            val (owner, field) = invocation.roundEnv
                                    .getElementsAnnotatedWith(Entity::class.java)
                                    .map {
                                        Pair(it, invocation.processingEnv.elementUtils
                                                .getAllMembers(MoreElements.asType(it))
                                                .firstOrNull { it.kind == ElementKind.FIELD })
                                    }
                                    .first { it.second != null }
                            val entityContext =
                                    EntityProcessor(invocation.context, MoreElements.asType(owner))
                                            .context
                            val parser = FieldProcessor(
                                    baseContext = entityContext,
                                    containing = MoreTypes.asDeclared(owner.asType()),
                                    element = field!!,
                                    bindingScope = FieldProcessor.BindingScope.TWO_WAY,
                                    fieldParent = null)
                            handler(parser.process(), invocation)
                            true
                        }
                        .build())
    }
}
