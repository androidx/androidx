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

import androidx.room.compiler.processing.ksp.ERROR_TYPE_NAME
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.className
import androidx.room.compiler.processing.util.getDeclaredMethod
import androidx.room.compiler.processing.util.getField
import androidx.room.compiler.processing.util.getMethod
import androidx.room.compiler.processing.util.javaElementUtils
import androidx.room.compiler.processing.util.kspResolver
import androidx.room.compiler.processing.util.runKspTest
import androidx.room.compiler.processing.util.runProcessorTest
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
    fun typeArguments() {
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
        runProcessorTest(
            sources = listOf(parent)
        ) {
            val type = it.processingEnv.requireType("foo.bar.Parent")
            val className = ClassName.get("foo.bar", "Parent")
            assertThat(type.typeName).isEqualTo(
                ParameterizedTypeName.get(
                    className,
                    ClassName.get("", "InputStreamType")
                )
            )

            val typeArguments = type.typeArguments
            assertThat(typeArguments).hasSize(1)
            val inputStreamClassName = ClassName.get("java.io", "InputStream")
            typeArguments.first().let { firstType ->
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

            type.typeElement!!.getMethod("wildcardParam").let { method ->
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

        runProcessorTest(
            sources = listOf(missingTypeRef)
        ) {
            val errorTypeName = if (it.isKsp) {
                // in ksp, we lose the name when resolving the type.
                // b/175246617
                ERROR_TYPE_NAME
            } else {
                ClassName.get("", "NotExistingType")
            }
            val element = it.processingEnv.requireTypeElement("foo.bar.Baz")
            element.getField("badField").let { field ->
                assertThat(field.type.isError()).isTrue()
                assertThat(field.type.typeName).isEqualTo(errorTypeName)
            }
            element.getDeclaredMethod("badMethod").let { method ->
                assertThat(method.returnType.isError()).isTrue()
                assertThat(method.returnType.typeName).isEqualTo(errorTypeName)
            }
            it.assertCompilationResult {
                compilationDidFail()
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
        runProcessorTest(
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
    fun sameType_kotlinJava() {
        val javaSrc = Source.java(
            "JavaClass",
            """
            class JavaClass {
                int intField;
                Integer integerField;
            }
            """.trimIndent()
        )
        val kotlinSrc = Source.kotlin(
            "Foo.kt",
            """
            class KotlinClass {
                val intProp: Int = 0
                val integerProp : Int? = null
            }
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(javaSrc, kotlinSrc)
        ) { invocation ->
            val javaElm = invocation.processingEnv.requireTypeElement("JavaClass")
            val kotlinElm = invocation.processingEnv.requireTypeElement("KotlinClass")
            fun XFieldElement.isSameType(other: XFieldElement): Boolean {
                return type.isSameType(other.type)
            }
            val fields = javaElm.getAllFieldsIncludingPrivateSupers() +
                kotlinElm.getAllFieldsIncludingPrivateSupers()
            val results = fields.flatMap { f1 ->
                fields.map { f2 ->
                    f1 to f2
                }.filter { (first, second) ->
                    first.isSameType(second)
                }
            }.map { (first, second) ->
                first.name to second.name
            }

            val expected = setOf(
                "intField" to "intProp",
                "intProp" to "intField",
                "integerField" to "integerProp",
                "integerProp" to "integerField"
            ) + fields.map { it.name to it.name }.toSet()
            assertThat(results).containsExactlyElementsIn(expected)
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
        runKspTest(sources = emptyList()) { invocation ->
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
        runProcessorTest {
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
        runProcessorTest(
            sources = listOf(missingTypeRef)
        ) {
            val element = it.processingEnv.requireTypeElement("foo.bar.Baz")
            assertThat(element.superType?.isError()).isTrue()
            it.assertCompilationResult {
                compilationDidFail()
            }
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
        runProcessorTest {
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

    @Test
    fun isKotlinUnit() {
        val kotlinSubject = Source.kotlin(
            "Subject.kt",
            """
            class KotlinSubject {
                suspend fun unitSuspend() {}
            }
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(kotlinSubject)) { invocation ->
            invocation.processingEnv.requireTypeElement("KotlinSubject").let {
                val continuationParam = it.getMethod("unitSuspend").parameters.last()
                val typeArg = continuationParam.type.typeArguments.first()
                assertThat(
                    typeArg.extendsBound()?.isKotlinUnit()
                ).isTrue()
                assertThat(
                    typeArg.extendsBound()?.extendsBound()
                ).isNull()
            }
        }
    }

    @Test
    fun isVoidObject() {
        val javaBase = Source.java(
            "JavaInterface.java",
            """
            import java.lang.Void;
            interface JavaInterface {
                Void getVoid();
                Void anotherVoid();
            }
            """.trimIndent()
        )
        val kotlinSubject = Source.kotlin(
            "Subject.kt",
            """
            abstract class KotlinSubject: JavaInterface {
                fun voidMethod() {}
            }
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(javaBase, kotlinSubject)) { invocation ->
            invocation.processingEnv.requireTypeElement("KotlinSubject").let {
                it.getMethod("voidMethod").returnType.let {
                    assertThat(it.isVoidObject()).isFalse()
                    assertThat(it.isVoid()).isTrue()
                    assertThat(it.isKotlinUnit()).isFalse()
                }
                val method = it.getMethod("getVoid")
                method.returnType.let {
                    assertThat(it.isVoidObject()).isTrue()
                    assertThat(it.isVoid()).isFalse()
                    assertThat(it.isKotlinUnit()).isFalse()
                }
                it.getMethod("anotherVoid").returnType.let {
                    assertThat(it.isVoidObject()).isTrue()
                    assertThat(it.isVoid()).isFalse()
                    assertThat(it.isKotlinUnit()).isFalse()
                }
            }
        }
    }
}
