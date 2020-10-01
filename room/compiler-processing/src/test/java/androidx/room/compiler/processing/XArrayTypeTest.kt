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

import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.getField
import androidx.room.compiler.processing.util.runProcessorTest
import androidx.room.compiler.processing.util.runProcessorTestIncludingKsp
import com.google.common.truth.Truth.assertThat
import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.TypeName
import org.junit.Test

class XArrayTypeTest {
    @Test
    fun java() {
        val source = Source.java(
            "foo.bar.Baz", """
            package foo.bar;
            class Baz {
                String[] param;
            }
        """.trimIndent()
        )
        runProcessorTestIncludingKsp(
            sources = listOf(source)
        ) { invocation ->
            val type = invocation.processingEnv
                .requireTypeElement("foo.bar.Baz")
                .getField("param")
                .type
            assertThat(type.isArray()).isTrue()
            assertThat(type.typeName).isEqualTo(
                ArrayTypeName.of(invocation.types.string)
            )
            type.asArray().componentType.let { component ->
                assertThat(component.typeName).isEqualTo(invocation.types.string)
                assertThat(component.nullability).isEqualTo(XNullability.UNKNOWN)
            }
        }
    }

    @Test
    fun synthetic() {
        runProcessorTestIncludingKsp {
            val objArray = it.processingEnv.getArrayType(
                TypeName.OBJECT
            )
            assertThat(objArray.isArray()).isTrue()
            assertThat(objArray.asArray().componentType.typeName).isEqualTo(
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
            "Foo.kt", """
            package foo.bar
            class Baz {
                val nonNull:Array<String> = TODO()
                val nullable:Array<String?> = TODO()
            }
        """.trimIndent()
        )
        runProcessorTestIncludingKsp(
            sources = listOf(source)
        ) { invocation ->
            val element = invocation.processingEnv.requireTypeElement("foo.bar.Baz")
            val nonNull = element.getField("nonNull").type
            val nullable = element.getField("nullable").type
            listOf(nonNull, nullable).forEach {
                assertThat(nonNull.isArray()).isTrue()
                assertThat(nonNull.typeName).isEqualTo(
                    ArrayTypeName.of(invocation.types.string)
                )
            }

            nonNull.asArray().componentType.let { component ->
                assertThat(component.typeName).isEqualTo(invocation.types.string)
                assertThat(component.nullability).isEqualTo(XNullability.NONNULL)
            }
            nullable.asArray().componentType.let { component ->
                assertThat(component.typeName).isEqualTo(invocation.types.string)
                assertThat(component.nullability).isEqualTo(XNullability.NULLABLE)
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
