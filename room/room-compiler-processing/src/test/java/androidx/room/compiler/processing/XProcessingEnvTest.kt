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
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import javax.lang.model.element.Modifier
import javax.tools.Diagnostic

@RunWith(JUnit4::class)
class XProcessingEnvTest {
    @Test
    fun getElement() {
        runProcessorTest(
            listOf(
                Source.java(
                    "foo.bar.Baz",
                    """
                package foo.bar;
                public class Baz {
                }
                    """.trimIndent()
                )
            )
        ) {
            val qName = "java.util.List"
            val className = ClassName.get("java.util", "List")
            val klass = List::class
            val element = it.processingEnv.requireTypeElement(qName)
            assertThat(element).isNotNull()
            assertThat(element.className).isEqualTo(
                className
            )

            val type = element.type

            assertThat(
                it.processingEnv.findTypeElement(qName)
            ).isEqualTo(element)
            assertThat(
                it.processingEnv.findTypeElement(className)
            ).isEqualTo(element)
            assertThat(
                it.processingEnv.findTypeElement(klass)
            ).isEqualTo(element)

            assertThat(
                it.processingEnv.requireTypeElement(className)
            ).isEqualTo(element)
            assertThat(
                it.processingEnv.requireTypeElement(klass)
            ).isEqualTo(element)

            assertThat(
                it.processingEnv.findType(qName)
            ).isEqualTo(type)
            assertThat(
                it.processingEnv.findType(className)
            ).isEqualTo(type)
            assertThat(
                it.processingEnv.findType(klass)
            ).isEqualTo(type)

            assertThat(
                it.processingEnv.requireType(className)
            ).isEqualTo(type)
            assertThat(
                it.processingEnv.requireType(klass)
            ).isEqualTo(type)
            assertThat(
                it.processingEnv.requireType(qName)
            ).isEqualTo(type)
        }
    }

    @Test
    fun basic() {
        runProcessorTest(
            listOf(
                Source.java(
                    "foo.bar.Baz",
                    """
                package foo.bar;
                public class Baz {
                    private void foo() {}
                    public int bar(int param1) {
                        return 3;
                    }
                }
                    """.trimIndent()
                )
            )
        ) {
            val element = it.processingEnv.requireTypeElement("foo.bar.Baz")
            assertThat(element.packageName).isEqualTo("foo.bar")
            assertThat(element.name).isEqualTo("Baz")
            assertThat(element.className)
                .isEqualTo(ClassName.get("foo.bar", "Baz"))
            assertThat(element.findPrimaryConstructor()).isNull()
            assertThat(element.getConstructors()).hasSize(1)
            assertThat(element.getDeclaredMethods()).hasSize(2)
            assertThat(element.kindName()).isEqualTo("class")
            assertThat(element.isInterface()).isFalse()
            assertThat(element.superType?.typeName).isEqualTo(TypeName.OBJECT)
        }
    }

    @Test
    fun getPrimitives() {
        val source = Source.java(
            "foo.bar.Baz",
            """
            package foo.bar;
            class Baz {
            }
            """.trimIndent()
        )
        runProcessorTest(
            listOf(source)
        ) { invocation ->
            PRIMITIVE_TYPES.flatMap {
                listOf(it, it.box())
            }.forEach {
                val targetType = invocation.processingEnv.findType(it.toString())
                assertThat(targetType?.typeName).isEqualTo(it)
                assertThat(targetType?.boxed()?.typeName).isEqualTo(it.box())
            }
        }
    }

    @Test
    fun nestedType() {
        val src = Source.java(
            "foo.bar.Outer",
            """
            package foo.bar;
            public class Outer {
                public static class Inner {
                }
            }
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(src)) {
            it.processingEnv.requireTypeElement("foo.bar.Outer.Inner").let {
                val className = it.className
                assertThat(className.packageName()).isEqualTo("foo.bar")
                assertThat(className.simpleNames()).containsExactly("Outer", "Inner")
                assertThat(className.simpleName()).isEqualTo("Inner")
            }
        }
    }

    @Test
    fun findGeneratedAnnotation() {
        runProcessorTest(sources = emptyList(), classpath = emptyList()) { invocation ->
            val generatedAnnotation = invocation.processingEnv.findGeneratedAnnotation()
            assertThat(generatedAnnotation?.name).isEqualTo("Generated")
        }
    }

    @Test
    fun generateCode() {
        val javaSrc = Source.java(
            "foo.bar.AccessGenerated",
            """
            package foo.bar;
            public class AccessGenerated {
                ToBeGenerated x;
            }
            """.trimIndent()
        )
        val kotlinSrc = Source.kotlin(
            "AccessGenerated.kt",
            """
            package foo.bar;
            public class AccessGenerated(x: ToBeGenerated)
            """.trimIndent()
        )
        listOf(javaSrc, kotlinSrc).forEach { src ->
            runProcessorTest(sources = listOf(src)) { invocation ->
                val className = ClassName.get("foo.bar", "ToBeGenerated")
                if (invocation.processingEnv.findTypeElement(className) == null) {
                    // generate only if it doesn't exist to handle multi-round
                    val spec = TypeSpec.classBuilder(className)
                        .addModifiers(Modifier.PUBLIC)
                        .build()
                    JavaFile.builder(className.packageName(), spec)
                        .build()
                        .writeTo(invocation.processingEnv.filer)
                }
            }
        }
    }

    @Test
    fun errorLogFailsCompilation() {
        val src = Source.java(
            "Foo.java",
            """
            class Foo {}
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(src)
        ) {
            it.processingEnv.messager.printMessage(
                Diagnostic.Kind.ERROR,
                "intentional failure"
            )
            it.assertCompilationResult {
                compilationDidFail()
                hasError("intentional failure")
            }
        }
    }

    @Test
    fun typeElementsAreCached() {
        val src = Source.java(
            "JavaSubject",
            """
            class JavaSubject {
                NestedClass nestedClass;
                class NestedClass {
                    int x;
                }
            }
            """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(src)
        ) { invocation ->
            val parent = invocation.processingEnv.requireTypeElement("JavaSubject")
            val nested = invocation.processingEnv.requireTypeElement("JavaSubject.NestedClass")
            assertThat(nested.enclosingTypeElement).isSameInstanceAs(parent)
        }
    }

    companion object {
        val PRIMITIVE_TYPES = listOf(
            TypeName.BOOLEAN,
            TypeName.BYTE,
            TypeName.SHORT,
            TypeName.INT,
            TypeName.LONG,
            TypeName.CHAR,
            TypeName.FLOAT,
            TypeName.DOUBLE,
        )
    }
}
