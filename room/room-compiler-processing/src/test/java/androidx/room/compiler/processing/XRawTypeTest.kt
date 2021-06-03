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
import androidx.room.compiler.processing.util.runProcessorTest
import com.google.common.truth.Truth.assertThat
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeVariableName
import org.junit.Test

class XRawTypeTest {
    @Test
    fun simple() {
        val javaSrc = Source.java(
            "foo.bar.JavaClass",
            """
            package foo.bar;
            class JavaClass<T, R> {
            }
            """.trimIndent()
        )
        val kotlinSrc = Source.kotlin(
            "KotlinClass.kt",
            """
            package foo.bar
            class KotlinClass<T, R>
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(javaSrc, kotlinSrc)
        ) { invocation ->
            val intType = invocation.processingEnv.requireType("java.lang.Integer")
            val stringType = invocation.processingEnv.requireType("java.lang.String")
            invocation.processingEnv.requireTypeElement("foo.bar.JavaClass").let { javaElm ->
                assertThat(javaElm.type.typeName).isEqualTo(
                    ParameterizedTypeName.get(
                        ClassName.get("foo.bar", "JavaClass"),
                        TypeVariableName.get("T"),
                        TypeVariableName.get("R")
                    )
                )
                val rawType = javaElm.type.rawType
                assertThat(rawType.toString()).isEqualTo("foo.bar.JavaClass")
                assertThat(rawType.typeName).isEqualTo(
                    ClassName.get("foo.bar", "JavaClass")
                )
                invocation.processingEnv.getDeclaredType(
                    type = javaElm,
                    intType,
                    stringType
                ).let { withTypes ->
                    assertThat(rawType.isAssignableFrom(withTypes)).isTrue()
                    assertThat(rawType.isAssignableFrom(withTypes.makeNullable())).isTrue()
                    assertThat(rawType.isAssignableFrom(withTypes.makeNonNullable())).isTrue()
                }
            }

            invocation.processingEnv.requireTypeElement("foo.bar.KotlinClass").let { kotlinElm ->
                assertThat(kotlinElm.type.typeName).isEqualTo(
                    ParameterizedTypeName.get(
                        ClassName.get("foo.bar", "KotlinClass"),
                        TypeVariableName.get("T"),
                        TypeVariableName.get("R")
                    )
                )
                val rawType = kotlinElm.type.rawType
                assertThat(rawType.toString()).isEqualTo("foo.bar.KotlinClass")
                assertThat(rawType.typeName).isEqualTo(
                    ClassName.get("foo.bar", "KotlinClass")
                )
                invocation.processingEnv.getDeclaredType(
                    type = kotlinElm,
                    intType,
                    stringType
                ).let { withTypes ->
                    assertThat(rawType.isAssignableFrom(withTypes)).isTrue()
                    assertThat(rawType.isAssignableFrom(withTypes.makeNullable())).isTrue()
                    assertThat(rawType.isAssignableFrom(withTypes.makeNonNullable())).isTrue()
                }
            }
        }
    }

    @Test
    fun iterables() {
        runProcessorTest { invocation ->
            val iterable = invocation.processingEnv.requireType("java.lang.Iterable")
            val arrayList = invocation.processingEnv.requireType("java.util.ArrayList")
            assertThat(iterable.rawType.isAssignableFrom(arrayList)).isTrue()
            if (invocation.isKsp) {
                val kotlinList = invocation.processingEnv.requireType("kotlin.collections.List")
                val kotlinMutableList = invocation.processingEnv
                    .requireType("kotlin.collections.MutableList")
                assertThat(iterable.rawType.isAssignableFrom(kotlinList)).isTrue()
                assertThat(iterable.rawType.isAssignableFrom(kotlinMutableList)).isTrue()
            }
        }
    }
}