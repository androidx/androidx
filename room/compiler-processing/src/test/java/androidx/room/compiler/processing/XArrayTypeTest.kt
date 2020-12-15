/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.room.compiler.processing

import androidx.room.compiler.processing.ksp.KspProcessingEnv
import androidx.room.compiler.processing.ksp.createTypeReference
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.getField
import androidx.room.compiler.processing.util.kspResolver
import androidx.room.compiler.processing.util.runKspTest
import androidx.room.compiler.processing.util.runProcessorTest
import androidx.room.compiler.processing.util.typeName
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.TypeName
import org.junit.Test

class XArrayTypeTest {
    @Test
    fun java() {
        val source = Source.java(
            "foo.bar.Baz",
            """
            package foo.bar;
            class Baz {
                String[] param;
            }
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(source)
        ) { invocation ->
            val type = invocation.processingEnv
                .requireTypeElement("foo.bar.Baz")
                .getField("param")
                .type
            assertThat(type.isArray()).isTrue()
            assertThat(type.typeName).isEqualTo(
                ArrayTypeName.of(String::class.java)
            )
            check(type.isArray())
            type.componentType.let { component ->
                assertThat(component.typeName).isEqualTo(String::class.typeName())
                assertThat(component.nullability).isEqualTo(XNullability.UNKNOWN)
            }
        }
    }

    @Test
    fun synthetic() {
        runProcessorTest {
            val objArray = it.processingEnv.getArrayType(
                TypeName.OBJECT
            )
            check(objArray.isArray())
            assertThat(objArray.componentType.typeName).isEqualTo(
                TypeName.OBJECT
            )
            assertThat(objArray.typeName).isEqualTo(
                ArrayTypeName.of(TypeName.OBJECT)
            )
        }
    }

    @Test
    fun kotlin() {
        val source = Source.kotlin(
            "Foo.kt",
            """
            package foo.bar
            class Baz {
                val nonNull:Array<String> = TODO()
                val nullable:Array<String?> = TODO()
            }
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(source)
        ) { invocation ->
            val element = invocation.processingEnv.requireTypeElement("foo.bar.Baz")
            val nonNull = element.getField("nonNull").type
            val nullable = element.getField("nullable").type
            listOf(nonNull, nullable).forEach {
                assertThat(it.isArray()).isTrue()
                assertThat(it.typeName).isEqualTo(
                    ArrayTypeName.of(String::class.java)
                )
            }
            check(nonNull.isArray())
            nonNull.componentType.let { component ->
                assertThat(component.typeName).isEqualTo(
                    String::class.typeName()
                )
                assertThat(component.nullability).isEqualTo(XNullability.NONNULL)
            }
            check(nullable.isArray())
            nullable.componentType.let { component ->
                assertThat(component.typeName).isEqualTo(String::class.typeName())
                assertThat(component.nullability).isEqualTo(XNullability.NULLABLE)
            }
        }
    }

    @Test
    fun kotlinPrimitiveArray() {
        val src = Source.kotlin(
            "Foo.kt",
            """
            class Subject {
                val primitiveBooleanArray : BooleanArray = TODO()
                val primitiveByteArray : ByteArray = TODO()
                val primitiveShortArray : ShortArray = TODO()
                val primitiveIntArray : IntArray = TODO()
                val primitiveLongArray : LongArray = TODO()
                val primitiveCharArray : CharArray = TODO()
                val primitiveFloatArray : FloatArray = TODO()
                val primitiveDoubleArray : DoubleArray = TODO()
                val boxedBooleanArray : Array<Boolean> = TODO()
                val boxedByteArray : Array<Byte> = TODO()
                val boxedShortArray : Array<Short> = TODO()
                val boxedIntArray : Array<Int> = TODO()
                val boxedLongArray : Array<Long> = TODO()
                val boxedCharArray : Array<Char> = TODO()
                val boxedFloatArray : Array<Float> = TODO()
                val boxedDoubleArray : Array<Double> = TODO()
            }
            """.trimIndent()
        )
        runProcessorTest(listOf(src)) {
            val subject = it.processingEnv.requireTypeElement("Subject")
            val types = subject.getAllFieldsIncludingPrivateSupers().map {
                assertWithMessage(it.name).that(it.type.isArray()).isTrue()
                it.name to it.type.typeName
            }
            assertThat(types).containsExactly(
                "primitiveBooleanArray" to ArrayTypeName.of(TypeName.BOOLEAN),
                "primitiveByteArray" to ArrayTypeName.of(TypeName.BYTE),
                "primitiveShortArray" to ArrayTypeName.of(TypeName.SHORT),
                "primitiveIntArray" to ArrayTypeName.of(TypeName.INT),
                "primitiveLongArray" to ArrayTypeName.of(TypeName.LONG),
                "primitiveCharArray" to ArrayTypeName.of(TypeName.CHAR),
                "primitiveFloatArray" to ArrayTypeName.of(TypeName.FLOAT),
                "primitiveDoubleArray" to ArrayTypeName.of(TypeName.DOUBLE),
                "boxedBooleanArray" to ArrayTypeName.of(TypeName.BOOLEAN.box()),
                "boxedByteArray" to ArrayTypeName.of(TypeName.BYTE.box()),
                "boxedShortArray" to ArrayTypeName.of(TypeName.SHORT.box()),
                "boxedIntArray" to ArrayTypeName.of(TypeName.INT.box()),
                "boxedLongArray" to ArrayTypeName.of(TypeName.LONG.box()),
                "boxedCharArray" to ArrayTypeName.of(TypeName.CHAR.box()),
                "boxedFloatArray" to ArrayTypeName.of(TypeName.FLOAT.box()),
                "boxedDoubleArray" to ArrayTypeName.of(TypeName.DOUBLE.box())
            )
        }
    }

    @Test
    fun createArray() {
        runKspTest(
            sources = emptyList()
        ) { invocation ->
            val intType = invocation.processingEnv.requireType("kotlin.Int")
            invocation.processingEnv.getArrayType(intType).let {
                assertThat(it.isArray()).isTrue()
                assertThat(it.componentType).isEqualTo(intType)
                assertThat(it.typeName).isEqualTo(
                    ArrayTypeName.of(TypeName.INT)
                )
            }
            val nullableInt = (invocation.processingEnv as KspProcessingEnv).wrap(
                invocation.kspResolver.builtIns.intType.makeNullable().createTypeReference()
            )

            invocation.processingEnv.getArrayType(nullableInt).let {
                assertThat(it.isArray()).isTrue()
                assertThat(it.componentType).isEqualTo(nullableInt)
                assertThat(it.typeName).isEqualTo(
                    ArrayTypeName.of(TypeName.INT.box())
                )
            }
        }
    }

    @Test
    fun notAnArray() {
        runProcessorTest {
            val list = it.processingEnv.requireType("java.util.List")
            assertThat(list.isArray()).isFalse()
        }
    }
}
