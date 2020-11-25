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

import androidx.room.compiler.processing.javac.JavacProcessingEnv
import androidx.room.compiler.processing.safeTypeName
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.className
import androidx.room.compiler.processing.util.kspResolver
import androidx.room.compiler.processing.util.runKaptTest
import androidx.room.compiler.processing.util.runKspTest
import com.google.common.truth.Truth.assertThat
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import javax.lang.model.util.ElementFilter

@RunWith(JUnit4::class)
class KSTypeExtTest {
    @Test
    fun kotlinTypeName() {
        val subjectSrc = Source.kotlin(
            "Foo.kt",
            """
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
            assertThat(subject.propertyType("intField").typeName(resolver))
                .isEqualTo(TypeName.INT)
            assertThat(subject.propertyType("listOfInts").typeName(resolver))
                .isEqualTo(
                    ParameterizedTypeName.get(
                        List::class.className(),
                        TypeName.INT.box()
                    )
                )
            assertThat(subject.propertyType("mutableMapOfAny").typeName(resolver))
                .isEqualTo(
                    ParameterizedTypeName.get(
                        Map::class.className(),
                        String::class.className(),
                        TypeName.OBJECT,
                    )
                )
            val typeName = subject.propertyType("nested").typeName(resolver)
            check(typeName is ClassName)
            assertThat(typeName.packageName()).isEqualTo("foo.bar")
            assertThat(typeName.simpleNames()).containsExactly("Baz", "Nested")
        }
    }

    @Test
    fun javaTypeName() {
        val subjectSrc = Source.java(
            "Baz",
            """
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
            assertThat(subject.propertyType("intField").typeName(resolver))
                .isEqualTo(TypeName.INT)
            assertThat(subject.propertyType("listOfInts").typeName(resolver))
                .isEqualTo(
                    ParameterizedTypeName.get(
                        List::class.className(),
                        TypeName.INT.box()
                    )
                )
            assertThat(subject.propertyType("incompleteGeneric").typeName(resolver))
                .isEqualTo(
                    List::class.className()
                )
            assertThat(subject.propertyType("nested").typeName(resolver))
                .isEqualTo(
                    ClassName.get("", "Baz", "Nested")
                )
        }
    }

    @Test
    fun kotlinErrorType() {
        val subjectSrc = Source.kotlin(
            "Foo.kt",
            """
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
            assertThat(subject.propertyType("errorField").typeName(resolver))
                .isEqualTo(ERROR_TYPE_NAME)
            assertThat(subject.propertyType("listOfError").typeName(resolver))
                .isEqualTo(
                    ParameterizedTypeName.get(
                        List::class.className(),
                        ERROR_TYPE_NAME
                    )
                )
            assertThat(subject.propertyType("mutableMapOfDontExist").typeName(resolver))
                .isEqualTo(
                    ParameterizedTypeName.get(
                        Map::class.className(),
                        String::class.className(),
                        ERROR_TYPE_NAME
                    )
                )
        }
    }

    /**
     * Compare against what KAPT returns.
     */
    @Test
    fun kaptGoldenTest() {
        val src = Source.kotlin(
            "Foo.kt",
            """
            class MyType
            class MyGeneric<T>
            class Subject {
                fun method1():MyGeneric<MyType> = TODO()
                fun method2(items: MyGeneric<in MyType>): MyType = TODO()
                fun method3(items: MyGeneric<out MyType>): MyType = TODO()
                fun method4(items: MyGeneric<MyType>): MyType = TODO()
                fun method5(): MyGeneric<out MyType> = TODO()
                fun method6(): MyGeneric<in MyType> = TODO()
                fun method7(): MyGeneric<MyType> = TODO()
                fun method8(): MyGeneric<*> = TODO()
                fun method9(args : Array<Int>):Array<Array<String>> = TODO()
                fun method10(args : Array<Array<Int>>):Array<String> = TODO()
            }
            """.trimIndent()
        )
        // methodName -> returnType, ...paramTypes
        val golden = mutableMapOf<String, List<TypeName>>()
        runKaptTest(
            sources = listOf(src),
            succeed = true
        ) { invocation ->
            val env = (invocation.processingEnv as JavacProcessingEnv)
            val subject = env.delegate.elementUtils.getTypeElement("Subject")
            ElementFilter.methodsIn(subject.enclosedElements).map { method ->
                val types = listOf(method.returnType.safeTypeName()) +
                    method.parameters.map {
                        it.asType().safeTypeName()
                    }
                golden[method.simpleName.toString()] = types
            }
        }
        val kspResults = mutableMapOf<String, List<TypeName>>()
        runKspTest(
            sources = listOf(src),
            succeed = true
        ) { invocation ->
            val env = (invocation.processingEnv as KspProcessingEnv)
            val subject = env.resolver.requireClass("Subject")
            subject.getDeclaredFunctions().forEach { method ->
                val types = listOf(
                    method.returnType.typeName(
                        invocation.kspResolver
                    )
                ) +
                    method.parameters.map {
                        it.type.typeName(
                            invocation.kspResolver
                        )
                    }
                kspResults[method.simpleName.asString()] = types
            }
        }
        // make sure we grabbed some values to ensure test is working
        assertThat(golden).isNotEmpty()
        assertThat(kspResults).containsExactlyEntriesIn(golden)
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
}
