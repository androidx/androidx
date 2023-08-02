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

import androidx.kruth.assertThat
import androidx.kruth.assertWithMessage
import androidx.room.compiler.codegen.JArrayTypeName
import androidx.room.compiler.processing.ksp.KspProcessingEnv
import androidx.room.compiler.processing.ksp.createTypeReference
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.asJTypeName
import androidx.room.compiler.processing.util.asKTypeName
import androidx.room.compiler.processing.util.getField
import androidx.room.compiler.processing.util.kspResolver
import androidx.room.compiler.processing.util.runKspTest
import androidx.room.compiler.processing.util.runProcessorTest
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.javapoet.JTypeName
import com.squareup.kotlinpoet.javapoet.KTypeName
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
            assertThat(type.asTypeName().java).isEqualTo(
                JArrayTypeName.of(String::class.java)
            )
            if (invocation.isKsp) {
                assertThat(type.asTypeName().kotlin).isEqualTo(
                    com.squareup.kotlinpoet.ARRAY.parameterizedBy(String::class.asKTypeName())
                )
            }
            check(type.isArray())
            type.componentType.let { component ->
                assertThat(component.asTypeName().java).isEqualTo(String::class.asJTypeName())
                if (invocation.isKsp) {
                    assertThat(component.asTypeName().kotlin).isEqualTo(String::class.asKTypeName())
                }
                assertThat(component.nullability).isEqualTo(XNullability.UNKNOWN)
            }
        }
    }

    @Test
    fun synthetic() {
        runProcessorTest {
            fun checkObjectArray(objArray: XArrayType) {
                check(objArray.isArray())
                assertThat(objArray.componentType.asTypeName().java)
                    .isEqualTo(JTypeName.OBJECT)
                assertThat(objArray.asTypeName().java).isEqualTo(
                    JArrayTypeName.of(JTypeName.OBJECT)
                )
                if (it.isKsp) {
                    assertThat(objArray.componentType.asTypeName().kotlin)
                        .isEqualTo(com.squareup.kotlinpoet.ANY)
                    assertThat(objArray.asTypeName().kotlin).isEqualTo(
                        com.squareup.kotlinpoet.ARRAY.parameterizedBy(com.squareup.kotlinpoet.ANY)
                    )
                }
            }
            checkObjectArray(
                it.processingEnv.getArrayType(it.processingEnv.requireType("java.lang.Object"))
            )
            if (it.isKsp) {
                // javac can't resolve Any
                checkObjectArray(
                    it.processingEnv.getArrayType(it.processingEnv.requireType("kotlin.Any"))
                )
            }
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
            element.getField("nonNull").type.let { nonNull ->
                check(nonNull.isArray())
                assertThat(nonNull.asTypeName().java).isEqualTo(
                    JArrayTypeName.of(String::class.java)
                )
                if (invocation.isKsp) {
                    assertThat(nonNull.asTypeName().kotlin).isEqualTo(
                        com.squareup.kotlinpoet.ARRAY.parameterizedBy(String::class.asKTypeName())
                    )
                }
                nonNull.componentType.let { component ->
                    assertThat(component.asTypeName().java).isEqualTo(
                        String::class.asJTypeName()
                    )
                    if (invocation.isKsp) {
                        assertThat(component.asTypeName().kotlin).isEqualTo(
                            String::class.asKTypeName()
                        )
                    }
                    assertThat(component.nullability).isEqualTo(XNullability.NONNULL)
                }
            }
            element.getField("nullable").type.let { nullable ->
                check(nullable.isArray())
                assertThat(nullable.asTypeName().java).isEqualTo(
                    JArrayTypeName.of(String::class.java)
                )
                if (invocation.isKsp) {
                    assertThat(nullable.asTypeName().kotlin).isEqualTo(
                        com.squareup.kotlinpoet.ARRAY.parameterizedBy(
                            String::class.asKTypeName().copy(nullable = true)
                        )
                    )
                }
                nullable.componentType.let { component ->
                    assertThat(component.asTypeName().java).isEqualTo(
                        String::class.asJTypeName()
                    )
                    if (invocation.isKsp) {
                        assertThat(component.asTypeName().kotlin).isEqualTo(
                            String::class.asKTypeName().copy(nullable = true)
                        )
                    }
                    assertThat(component.nullability).isEqualTo(XNullability.NULLABLE)
                }
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
        runProcessorTest(listOf(src)) { invocation ->
            class Container(
                val method: String,
                val jTypeName: JTypeName,
                val kTypeName: KTypeName
            ) {
                override fun equals(other: Any?): Boolean {
                    if (this === other) return true
                    if (javaClass != other?.javaClass) return false
                    other as Container
                    if (method != other.method) return false
                    if (jTypeName != other.jTypeName) return false
                    if (invocation.isKsp) {
                        if (kTypeName != other.kTypeName) return false
                    }
                    return true
                }
                override fun hashCode(): Int {
                    var result = method.hashCode()
                    result = 31 * result + jTypeName.hashCode()
                    if (invocation.isKsp) {
                        result = 31 * result + kTypeName.hashCode()
                    }
                    return result
                }
            }

            val subject = invocation.processingEnv.requireTypeElement("Subject")
            val types = subject.getAllFieldsIncludingPrivateSupers().map {
                assertWithMessage(it.name).that(it.type.isArray()).isTrue()
                Container(it.name, it.type.asTypeName().java, it.type.asTypeName().kotlin)
            }.toList()
            assertThat(types).containsExactly(
                Container(
                    "primitiveBooleanArray",
                    JArrayTypeName.of(JTypeName.BOOLEAN),
                    com.squareup.kotlinpoet.BOOLEAN_ARRAY
                ),
                Container(
                    "primitiveByteArray",
                    JArrayTypeName.of(JTypeName.BYTE),
                    com.squareup.kotlinpoet.BYTE_ARRAY
                ),
                Container(
                        "primitiveShortArray",
                    JArrayTypeName.of(JTypeName.SHORT),
                    com.squareup.kotlinpoet.SHORT_ARRAY
                ),
                Container(
                    "primitiveIntArray",
                    JArrayTypeName.of(JTypeName.INT),
                    com.squareup.kotlinpoet.INT_ARRAY
                ),
                Container(
                    "primitiveLongArray",
                    JArrayTypeName.of(JTypeName.LONG),
                    com.squareup.kotlinpoet.LONG_ARRAY
                ),
                Container(
                    "primitiveCharArray",
                    JArrayTypeName.of(JTypeName.CHAR),
                    com.squareup.kotlinpoet.CHAR_ARRAY
                ),
                Container(
                    "primitiveFloatArray",
                    JArrayTypeName.of(JTypeName.FLOAT),
                    com.squareup.kotlinpoet.FLOAT_ARRAY
                ),
                Container(
                    "primitiveDoubleArray",
                    JArrayTypeName.of(JTypeName.DOUBLE),
                    com.squareup.kotlinpoet.DOUBLE_ARRAY
                ),
                Container(
                    "boxedBooleanArray",
                    JArrayTypeName.of(JTypeName.BOOLEAN.box()),
                    com.squareup.kotlinpoet.ARRAY.parameterizedBy(com.squareup.kotlinpoet.BOOLEAN)
                ),
                Container(
                    "boxedByteArray",
                    JArrayTypeName.of(JTypeName.BYTE.box()),
                    com.squareup.kotlinpoet.ARRAY.parameterizedBy(com.squareup.kotlinpoet.BYTE)
                ),
                Container(
                    "boxedShortArray",
                    JArrayTypeName.of(JTypeName.SHORT.box()),
                    com.squareup.kotlinpoet.ARRAY.parameterizedBy(com.squareup.kotlinpoet.SHORT)
                ),
                Container(
                    "boxedIntArray",
                    JArrayTypeName.of(JTypeName.INT.box()),
                    com.squareup.kotlinpoet.ARRAY.parameterizedBy(com.squareup.kotlinpoet.INT)
                ),
                Container(
                    "boxedLongArray",
                    JArrayTypeName.of(JTypeName.LONG.box()),
                    com.squareup.kotlinpoet.ARRAY.parameterizedBy(com.squareup.kotlinpoet.LONG)
                ),
                Container(
                    "boxedCharArray",
                    JArrayTypeName.of(JTypeName.CHAR.box()),
                    com.squareup.kotlinpoet.ARRAY.parameterizedBy(com.squareup.kotlinpoet.CHAR)
                ),
                Container(
                    "boxedFloatArray",
                    JArrayTypeName.of(JTypeName.FLOAT.box()),
                    com.squareup.kotlinpoet.ARRAY.parameterizedBy(com.squareup.kotlinpoet.FLOAT)
                ),
                Container(
                    "boxedDoubleArray",
                    JArrayTypeName.of(JTypeName.DOUBLE.box()),
                    com.squareup.kotlinpoet.ARRAY.parameterizedBy(com.squareup.kotlinpoet.DOUBLE)
                )
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
                assertThat(it.asTypeName().java).isEqualTo(
                    JArrayTypeName.of(JTypeName.INT)
                )
                assertThat(it.asTypeName().kotlin).isEqualTo(
                    com.squareup.kotlinpoet.INT_ARRAY
                )
            }
            val nullableInt = (invocation.processingEnv as KspProcessingEnv).wrap(
                invocation.kspResolver.builtIns.intType.makeNullable().createTypeReference()
            )

            invocation.processingEnv.getArrayType(nullableInt).let {
                assertThat(it.isArray()).isTrue()
                assertThat(it.componentType).isEqualTo(nullableInt)
                assertThat(it.asTypeName().java).isEqualTo(
                    JArrayTypeName.of(JTypeName.INT.box())
                )
                assertThat(it.asTypeName().kotlin).isEqualTo(
                    com.squareup.kotlinpoet.ARRAY.parameterizedBy(
                        com.squareup.kotlinpoet.INT.copy(nullable = true)
                    )
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
