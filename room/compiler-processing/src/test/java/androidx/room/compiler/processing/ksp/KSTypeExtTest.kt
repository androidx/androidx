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

package androidx.room.compiler.processing.ksp

import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.runKspTest
import com.google.common.truth.Truth.assertThat
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import org.jetbrains.kotlin.ksp.getDeclaredProperties
import org.jetbrains.kotlin.ksp.processing.Resolver
import org.jetbrains.kotlin.ksp.symbol.KSClassDeclaration
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class KSTypeExtTest {
    @Test
    fun kotlinTypeName() {
        val subjectSrc = Source.kotlin(
            "Foo.kt", """
            package foo.bar;
            import kotlin.collections.*
            class Baz {
                val intField : Int = TODO()
                val listOfInts : List<Int> = TODO()
                val mutableMapOfAny = mutableMapOf<String, Any?>()
                val nested:Nested = TODO()
                class Nested {
                }
            }
        """.trimIndent()
        )
        runTest(subjectSrc) { resolver ->
            val subject = resolver.requireClass("foo.bar.Baz")
            assertThat(subject.propertyType("intField").typeName())
                .isEqualTo(INT_TYPE_NAME)
            assertThat(subject.propertyType("listOfInts").typeName())
                .isEqualTo(
                    ParameterizedTypeName.get(
                        LIST_TYPE_NAME,
                        INT_TYPE_NAME
                    )
                )
            assertThat(subject.propertyType("mutableMapOfAny").typeName())
                .isEqualTo(
                    ParameterizedTypeName.get(
                        MUTABLE_MAP_TYPE_NAME,
                        STRING_TYPE_NAME,
                        ANY_TYPE_NAME
                    )
                )
            val typeName = subject.propertyType("nested").typeName()
            check(typeName is ClassName)
            assertThat(typeName.packageName()).isEqualTo("foo.bar")
            assertThat(typeName.simpleNames()).containsExactly("Baz", "Nested")
        }
    }

    @Test
    fun javaTypeName() {
        val subjectSrc = Source.java(
            "Baz", """
            import java.util.List;
            class Baz {
                int intField;
                List<Integer> listOfInts;
                List incompleteGeneric;
                Nested nested;
                static class Nested {
                }
            }
        """.trimIndent()
        )
        runTest(subjectSrc) { resolver ->
            val subject = resolver.requireClass("Baz")
            assertThat(subject.propertyType("intField").typeName())
                .isEqualTo(INT_TYPE_NAME)
            assertThat(subject.propertyType("listOfInts").typeName())
                .isEqualTo(
                    ParameterizedTypeName.get(
                        MUTABLE_LIST_TYPE_NAME,
                        INT_TYPE_NAME
                    )
                )
            assertThat(subject.propertyType("incompleteGeneric").typeName())
                .isEqualTo(MUTABLE_LIST_TYPE_NAME)
            assertThat(subject.propertyType("nested").typeName())
                .isEqualTo(
                    ClassName.get("", "Baz", "Nested")
                )
        }
    }

    @Test
    fun kotlinErrorType() {
        val subjectSrc = Source.kotlin(
            "Foo.kt", """
            import kotlin.collections.*
            class Foo {
                val errorField : DoesNotExist = TODO()
                val listOfError : List<DoesNotExist> = TODO()
                val mutableMapOfDontExist : MutableMap<String, DoesNotExist> = TODO()
            }
        """.trimIndent()
        )
        runTest(subjectSrc, succeed = false) { resolver ->
            val subject = resolver.requireClass("Foo")
            assertThat(subject.propertyType("errorField").typeName())
                .isEqualTo(ERROR_TYPE_NAME)
            assertThat(subject.propertyType("listOfError").typeName())
                .isEqualTo(
                    ParameterizedTypeName.get(
                        LIST_TYPE_NAME,
                        ERROR_TYPE_NAME
                    )
                )
            assertThat(subject.propertyType("mutableMapOfDontExist").typeName())
                .isEqualTo(
                    ParameterizedTypeName.get(
                        MUTABLE_MAP_TYPE_NAME,
                        STRING_TYPE_NAME,
                        ERROR_TYPE_NAME
                    )
                )
        }
    }

    private fun runTest(
        vararg sources: Source,
        succeed: Boolean = true,
        handler: (Resolver) -> Unit
    ) {
        runKspTest(
            sources = sources.toList(),
            succeed = succeed
        ) {
            handler(
                (it.processingEnv as KspProcessingEnv).resolver
            )
        }
    }

    private fun KSClassDeclaration.requireProperty(name: String) = getDeclaredProperties().first {
        it.simpleName.asString() == name
    }

    private fun KSClassDeclaration.propertyType(name: String) = checkNotNull(
        requireProperty(name).type
    )

    companion object {
        val INT_TYPE_NAME: ClassName = ClassName.get("kotlin", "Int")
        val STRING_TYPE_NAME: ClassName = ClassName.get("kotlin", "String")
        val ANY_TYPE_NAME: ClassName = ClassName.get("kotlin", "Any")
        val LIST_TYPE_NAME: ClassName = ClassName.get("kotlin.collections", "List")
        val MUTABLE_LIST_TYPE_NAME: ClassName = ClassName.get("kotlin.collections", "MutableList")
        val MUTABLE_MAP_TYPE_NAME: ClassName = ClassName.get("kotlin.collections", "MutableMap")
    }
}
