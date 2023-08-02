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

import androidx.kruth.assertThat
import androidx.kruth.assertWithMessage
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.className
import androidx.room.compiler.processing.util.compileFiles
import androidx.room.compiler.processing.util.kspResolver
import androidx.room.compiler.processing.util.runKspTest
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class KSTypeExtTest {
    @Test
    fun kotlinTypeName() {
        fun createSubject(pkg: String): Source {
            return Source.kotlin(
                "Foo.kt",
                """
                package $pkg
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
        }

        val subjectSrc = createSubject(pkg = "main")
        val classpath = compileFiles(listOf(createSubject(pkg = "lib")))
        runKspTest(sources = listOf(subjectSrc), classpath = classpath) { invocation ->
            listOf("main", "lib").map {
                it to invocation.kspResolver.requireClass("$it.Baz")
            }.forEach { (pkg, subject) ->
                assertThat(subject.propertyType("intField").asJTypeName(invocation.kspResolver))
                    .isEqualTo(TypeName.INT)
                assertThat(subject.propertyType("listOfInts").asJTypeName(invocation.kspResolver))
                    .isEqualTo(
                        ParameterizedTypeName.get(
                            List::class.className(),
                            TypeName.INT.box()
                        )
                    )
                assertThat(
                    subject.propertyType("mutableMapOfAny").asJTypeName(invocation.kspResolver)
                )
                    .isEqualTo(
                        ParameterizedTypeName.get(
                            Map::class.className(),
                            String::class.className(),
                            TypeName.OBJECT,
                        )
                    )
                val typeName = subject.propertyType("nested").asJTypeName(invocation.kspResolver)
                check(typeName is ClassName)
                assertThat(typeName.packageName()).isEqualTo(pkg)
                assertThat(typeName.simpleNames()).containsExactly("Baz", "Nested")
            }
        }
    }

    @Test
    fun javaTypeName() {
        fun createSubject(pkg: String): Source {
            return Source.java(
                "$pkg.Baz",
                """
                package $pkg;

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
        }

        val subjectSrc = createSubject(pkg = "main")
        val classpath = compileFiles(listOf(createSubject(pkg = "lib")))
        runKspTest(sources = listOf(subjectSrc), classpath = classpath) { invocation ->
            listOf("main.Baz", "lib.Baz").map {
                invocation.kspResolver.requireClass(it)
            }.forEach { subject ->
                assertWithMessage(subject.qualifiedName!!.asString())
                    .that(
                        subject.propertyType("intField").asJTypeName(invocation.kspResolver)
                    ).isEqualTo(TypeName.INT)
                assertWithMessage(subject.qualifiedName!!.asString())
                    .that(
                        subject.propertyType("listOfInts").asJTypeName(invocation.kspResolver)
                    ).isEqualTo(
                        ParameterizedTypeName.get(
                            List::class.className(),
                            TypeName.INT.box()
                        )
                    )
                val propertyType = subject.propertyType("incompleteGeneric")
                val typeName = propertyType.asJTypeName(invocation.kspResolver)
                assertWithMessage(subject.qualifiedName!!.asString())
                    .that(
                        typeName
                    ).isEqualTo(
                        ClassName.get(List::class.java)
                    )
                val nestedTypeName =
                    subject.propertyType("nested").asJTypeName(invocation.kspResolver)
                assertWithMessage(subject.qualifiedName!!.asString())
                    .that(nestedTypeName)
                    .isEqualTo(
                        ClassName.get(subject.packageName.asString(), "Baz", "Nested")
                    )
            }
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
        runKspTest(sources = listOf(subjectSrc)) { invocation ->
            val subject = invocation.kspResolver.requireClass("Foo")
            assertThat(
                subject.propertyType("errorField").asJTypeName(invocation.kspResolver)
            ).isEqualTo(ERROR_JTYPE_NAME)
            assertThat(
                subject.propertyType("listOfError").asJTypeName(invocation.kspResolver)
            ).isEqualTo(
                ParameterizedTypeName.get(
                    List::class.className(),
                    ERROR_JTYPE_NAME
                )
            )
            assertThat(
                subject.propertyType("mutableMapOfDontExist").asJTypeName(invocation.kspResolver)
            ).isEqualTo(
                ParameterizedTypeName.get(
                    Map::class.className(),
                    String::class.className(),
                    ERROR_JTYPE_NAME
                )
            )
            invocation.assertCompilationResult {
                compilationDidFail()
            }
        }
    }

    private fun KSClassDeclaration.requireProperty(name: String) = getDeclaredProperties().first {
        it.simpleName.asString() == name
    }

    private fun KSClassDeclaration.propertyType(name: String) = checkNotNull(
        requireProperty(name).type
    )
}
