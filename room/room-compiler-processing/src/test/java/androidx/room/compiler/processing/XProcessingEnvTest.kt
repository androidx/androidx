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

import androidx.room.compiler.codegen.XClassName
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.asClassName
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.runProcessorTest
import com.google.common.truth.Truth.assertThat
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.kotlinpoet.javapoet.JClassName
import javax.lang.model.element.Modifier
import javax.tools.Diagnostic
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

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
            val jClassName = JClassName.get("java.util", "List")
            val klass = List::class
            val element = it.processingEnv.requireTypeElement(qName)
            assertThat(element).isNotNull()
            assertThat(element.asClassName().java).isEqualTo(
                jClassName
            )

            val type = element.type

            assertThat(
                it.processingEnv.findTypeElement(qName)
            ).isEqualTo(element)
            assertThat(
                it.processingEnv.findTypeElement(jClassName)
            ).isEqualTo(element)
            assertThat(
                it.processingEnv.findTypeElement(klass)
            ).isEqualTo(element)

            assertThat(
                it.processingEnv.requireTypeElement(jClassName)
            ).isEqualTo(element)
            assertThat(
                it.processingEnv.requireTypeElement(klass)
            ).isEqualTo(element)

            assertThat(
                it.processingEnv.findType(qName)
            ).isEqualTo(type)
            assertThat(
                it.processingEnv.findType(jClassName)
            ).isEqualTo(type)
            assertThat(
                it.processingEnv.findType(klass)
            ).isEqualTo(type)

            assertThat(
                it.processingEnv.requireType(jClassName)
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
            assertThat(element.asClassName())
                .isEqualTo(XClassName.get("foo.bar", "Baz"))
            assertThat(element.findPrimaryConstructor()).isNull()
            assertThat(element.getConstructors()).hasSize(1)
            assertThat(element.getDeclaredMethods()).hasSize(2)
            assertThat(element.kindName()).isEqualTo("class")
            assertThat(element.isInterface()).isFalse()
            assertThat(element.superClass?.typeName).isEqualTo(TypeName.OBJECT)
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
            PRIMITIVE_TYPES.zip(BOXED_PRIMITIVE_TYPES).forEach { (primitive, boxed) ->
                val targetType = invocation.processingEnv.requireType(primitive)
                assertThat(targetType.asTypeName()).isEqualTo(primitive)
                assertThat(targetType.boxed().asTypeName()).isEqualTo(boxed)
            }
            BOXED_PRIMITIVE_TYPES.forEach { boxed ->
                val targetType = invocation.processingEnv.requireType(boxed)
                assertThat(targetType.asTypeName()).isEqualTo(boxed)
                assertThat(targetType.boxed().asTypeName()).isEqualTo(boxed)
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
                val className = it.asClassName()
                assertThat(className.packageName).isEqualTo("foo.bar")
                assertThat(className.simpleNames).containsExactly("Outer", "Inner")
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
            "Foo",
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

    @Test
    fun jvmVersion() {
        runProcessorTest(
            sources = listOf(
                Source.java(
                    "foo.bar.Baz",
                    """
                package foo.bar;
                public class Baz {
                }
                    """.trimIndent()
                )
            ),
            javacArguments = listOf("-source", "11"),
            kotlincArguments = listOf("-Xjvm-target 11")
        ) {
            if (it.processingEnv.backend == XProcessingEnv.Backend.KSP) {
                // KSP is hardcoded to 8 for now...
                assertThat(it.processingEnv.jvmVersion).isEqualTo(8)
            } else {
                assertThat(it.processingEnv.jvmVersion).isEqualTo(11)
            }
        }
    }

    @Test
    fun requireTypeWithXTypeName() {
        runProcessorTest { invocation ->
            invocation.processingEnv.requireType(String::class.asClassName()).let {
                val name = it.typeElement!!.qualifiedName
                if (invocation.isKsp) {
                    assertThat(name).isEqualTo("kotlin.String")
                } else {
                    assertThat(name).isEqualTo("java.lang.String")
                }
            }
            invocation.processingEnv.requireType(Int::class.asClassName()).let {
                val name = it.typeElement!!.qualifiedName
                if (invocation.isKsp) {
                    assertThat(name).isEqualTo("kotlin.Int")
                } else {
                    assertThat(name).isEqualTo("java.lang.Integer")
                }
            }
            invocation.processingEnv.requireType(XTypeName.PRIMITIVE_INT).let {
                assertThat(it.typeElement).isNull() // No element is an indicator of primitive type
                assertThat(it.asTypeName().java.toString()).isEqualTo("int")
                if (invocation.isKsp) {
                    assertThat(it.asTypeName().kotlin.toString()).isEqualTo("kotlin.Int")
                }
            }
        }
    }

    companion object {
        val PRIMITIVE_TYPES = listOf(
            XTypeName.PRIMITIVE_BOOLEAN,
            XTypeName.PRIMITIVE_BYTE,
            XTypeName.PRIMITIVE_SHORT,
            XTypeName.PRIMITIVE_INT,
            XTypeName.PRIMITIVE_LONG,
            XTypeName.PRIMITIVE_CHAR,
            XTypeName.PRIMITIVE_FLOAT,
            XTypeName.PRIMITIVE_DOUBLE,
        )

        val BOXED_PRIMITIVE_TYPES = listOf(
            XTypeName.BOXED_BOOLEAN,
            XTypeName.BOXED_BYTE,
            XTypeName.BOXED_SHORT,
            XTypeName.BOXED_INT,
            XTypeName.BOXED_LONG,
            XTypeName.BOXED_CHAR,
            XTypeName.BOXED_FLOAT,
            XTypeName.BOXED_DOUBLE,
        )
    }
}
