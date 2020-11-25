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
import androidx.room.compiler.processing.util.className
import androidx.room.compiler.processing.util.getDeclaredMethod
import androidx.room.compiler.processing.util.getField
import androidx.room.compiler.processing.util.getMethod
import androidx.room.compiler.processing.util.javaElementUtils
import androidx.room.compiler.processing.util.kspResolver
import androidx.room.compiler.processing.util.runKspTest
import androidx.room.compiler.processing.util.runProcessorTest
import androidx.room.compiler.processing.util.runProcessorTestForFailedCompilation
import androidx.room.compiler.processing.util.runProcessorTestIncludingKsp
import androidx.room.compiler.processing.util.typeName
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.google.devtools.ksp.getClassDeclarationByName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeVariableName
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class XTypeTest {
    @Test
    fun declaredTypeArguments() {
        val parent = Source.java(
            "foo.bar.Parent",
            """
            package foo.bar;
            import java.io.InputStream;
            import java.util.Set;
            class Parent<InputStreamType extends InputStream> {
                public void wildcardParam(Set<?> param1) {}
            }
            """.trimIndent()
        )
        runProcessorTestIncludingKsp(
            sources = listOf(parent)
        ) {
            val type = it.processingEnv.requireType("foo.bar.Parent") as XDeclaredType
            val className = ClassName.get("foo.bar", "Parent")
            assertThat(type.typeName).isEqualTo(
                ParameterizedTypeName.get(
                    className,
                    ClassName.get("", "InputStreamType")
                )
            )
            assertThat(type.isDeclared()).isTrue()

            val typeArguments = type.typeArguments
            assertThat(typeArguments).hasSize(1)
            val inputStreamClassName = ClassName.get("java.io", "InputStream")
            typeArguments.first().let { firstType ->
                assertThat(firstType.isDeclared()).isFalse()
                val expected = TypeVariableName.get(
                    "InputStreamType",
                    inputStreamClassName
                )
                assertThat(firstType.typeName).isEqualTo(expected)
                // equals in TypeVariableName just checks the string representation but we want
                // to assert the upper bound as well
                assertThat(
                    (firstType.typeName as TypeVariableName).bounds
                ).containsExactly(
                    inputStreamClassName
                )
            }

            type.asTypeElement().getMethod("wildcardParam").let { method ->
                val wildcardParam = method.parameters.first()
                val extendsBoundOrSelf = wildcardParam.type.extendsBoundOrSelf()
                assertThat(extendsBoundOrSelf.rawType)
                    .isEqualTo(
                        it.processingEnv.requireType(Set::class).rawType
                    )
            }
        }
    }

    @Test
    fun errorType() {
        val missingTypeRef = Source.java(
            "foo.bar.Baz",
            """
                package foo.bar;
                public class Baz {
                    NotExistingType badField;
                    NotExistingType badMethod() {
                        throw new RuntimeException("Stub");
                    }
                }
            """.trimIndent()
        )
        // TODO run with KSP as well once https://github.com/google/ksp/issues/107 is resolved
        runProcessorTestForFailedCompilation(
            sources = listOf(missingTypeRef)
        ) {
            val element = it.processingEnv.requireTypeElement("foo.bar.Baz")
            element.getField("badField").let { field ->
                assertThat(field.type.isError()).isTrue()
                assertThat(field.type.typeName).isEqualTo(
                    ClassName.get("", "NotExistingType")
                )
            }
            element.getDeclaredMethod("badMethod").let { method ->
                assertThat(method.returnType.isError()).isTrue()
                assertThat(method.returnType.typeName).isEqualTo(
                    ClassName.get("", "NotExistingType")
                )
            }
        }
    }

    @Test
    fun sameType() {
        val subject = Source.java(
            "foo.bar.Baz",
            """
            package foo.bar;
            interface Baz {
                void method(String... inputs);
            }
            """.trimIndent()
        )
        runProcessorTestIncludingKsp(
            sources = listOf(subject)
        ) {
            val type = it.processingEnv.requireType("foo.bar.Baz")
            val list = it.processingEnv.requireType("java.util.List")
            val string = it.processingEnv.requireType("java.lang.String")
            assertThat(type.isSameType(type)).isTrue()
            assertThat(type.isSameType(list)).isFalse()
            assertThat(list.isSameType(string)).isFalse()
        }
    }

    @Test
    fun isCollection() {
        runProcessorTest {
            it.processingEnv.requireType("java.util.List").let { list ->
                assertThat(list.isCollection()).isTrue()
            }
            it.processingEnv.requireType("java.util.ArrayList").let { list ->
                // isCollection is overloaded name, it is actually just checking list or set.
                assertThat(list.isCollection()).isFalse()
            }
            it.processingEnv.requireType("java.util.Set").let { list ->
                assertThat(list.isCollection()).isTrue()
            }
            it.processingEnv.requireType("java.util.Map").let { list ->
                assertThat(list.isCollection()).isFalse()
            }
        }
    }

    @Test
    fun isCollection_kotlin() {
        runKspTest(sources = emptyList(), succeed = true) { invocation ->
            val subjects = listOf("Map" to false, "List" to true, "Set" to true)
            subjects.forEach { (subject, expected) ->
                invocation.processingEnv.requireType("kotlin.collections.$subject").let { type ->
                    Truth.assertWithMessage(type.typeName.toString())
                        .that(type.isCollection()).isEqualTo(expected)
                }
            }
        }
    }

    @Test
    fun toStringMatchesUnderlyingElement() {
        runProcessorTestIncludingKsp {
            val subject = "java.lang.String"
            val expected = if (it.isKsp) {
                it.kspResolver.getClassDeclarationByName(subject)?.toString()
            } else {
                it.javaElementUtils.getTypeElement(subject)?.toString()
            }
            val actual = it.processingEnv.requireType(subject).toString()
            assertThat(actual).isEqualTo(expected)
        }
    }

    @Test
    fun errorTypeForSuper() {
        val missingTypeRef = Source.java(
            "foo.bar.Baz",
            """
                package foo.bar;
                public class Baz extends IDontExist {
                    NotExistingType foo() {
                        throw new RuntimeException("Stub");
                    }
                }
            """.trimIndent()
        )
        // TODO run with KSP as well once https://github.com/google/ksp/issues/107 is resolved
        runProcessorTestForFailedCompilation(
            sources = listOf(missingTypeRef)
        ) {
            val element = it.processingEnv.requireTypeElement("foo.bar.Baz")
            assertThat(element.superType?.isError()).isTrue()
        }
    }

    @Test
    fun defaultValues() {
        runProcessorTest {
            assertThat(
                it.processingEnv.requireType("int").defaultValue()
            ).isEqualTo("0")
            assertThat(
                it.processingEnv.requireType("java.lang.String").defaultValue()
            ).isEqualTo("null")
            assertThat(
                it.processingEnv.requireType("double").defaultValue()
            ).isEqualTo("0.0")
            assertThat(
                it.processingEnv.requireType("float").defaultValue()
            ).isEqualTo("0f")
            assertThat(
                it.processingEnv.requireType("char").defaultValue()
            ).isEqualTo("0")
        }
    }

    @Test
    fun boxed() {
        runProcessorTest {
            assertThat(
                it.processingEnv.requireType("int").boxed().typeName
            ).isEqualTo(java.lang.Integer::class.className())
            assertThat(
                it.processingEnv.requireType("java.lang.String").boxed().typeName
            ).isEqualTo(String::class.className())
        }
    }

    @Test
    fun rawType() {
        runProcessorTestIncludingKsp {
            val subject = it.processingEnv.getDeclaredType(
                it.processingEnv.requireTypeElement(List::class),
                it.processingEnv.requireType(String::class)
            )
            val listClassName = List::class.className()
            assertThat(subject.typeName).isEqualTo(
                ParameterizedTypeName.get(listClassName, String::class.typeName())
            )
            assertThat(subject.rawType.typeName).isEqualTo(listClassName)

            val listOfInts = it.processingEnv.getDeclaredType(
                it.processingEnv.requireTypeElement(List::class),
                it.processingEnv.requireType(Integer::class)
            )
            assertThat(subject.rawType).isEqualTo(listOfInts.rawType)

            val setOfStrings = it.processingEnv.getDeclaredType(
                it.processingEnv.requireTypeElement(Set::class),
                it.processingEnv.requireType(String::class)
            )
            assertThat(subject.rawType).isNotEqualTo(setOfStrings.rawType)
        }
    }
}
