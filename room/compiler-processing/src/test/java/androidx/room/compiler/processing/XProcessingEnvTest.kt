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
import androidx.room.compiler.processing.util.TestInvocation
import androidx.room.compiler.processing.util.runProcessorTest
import androidx.room.compiler.processing.util.runProcessorTestIncludingKsp
import com.google.common.truth.Truth.assertThat
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import javax.lang.model.element.Modifier

@RunWith(JUnit4::class)
class XProcessingEnvTest {
    @Test
    fun getElement() {
        runProcessorTestIncludingKsp(
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
            assertThat(element.packageName).isEqualTo("java.util")
            assertThat(element.name).isEqualTo("List")

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
        runProcessorTestIncludingKsp(
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
            assertThat(element.asDeclaredType().typeName)
                .isEqualTo(ClassName.get("foo.bar", "Baz"))
            assertThat(element.findPrimaryConstructor()).isNull()
            assertThat(element.getConstructors()).hasSize(1)
            assertThat(element.getDeclaredMethods()).hasSize(2)
            assertThat(element.kindName()).isEqualTo("class")
            assertThat(element.isInterface()).isFalse()
            assertThat(element.superType?.typeName).isEqualTo(it.types.objectOrAny)
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
            PRIMITIVE_TYPES.forEach {
                val targetType = invocation.processingEnv.findType(it.key)
                assertThat(targetType?.typeName).isEqualTo(it.value)
                assertThat(targetType?.boxed()?.typeName).isEqualTo(it.value.box())
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
        runProcessorTestIncludingKsp(sources = listOf(src)) {
            it.processingEnv.requireTypeElement("foo.bar.Outer.Inner").let {
                val className = it.className
                assertThat(className.packageName()).isEqualTo("foo.bar")
                assertThat(className.simpleNames()).containsExactly("Outer", "Inner")
                assertThat(className.simpleName()).isEqualTo("Inner")
            }
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
            fun runTest(block: (TestInvocation) -> Unit) {
                // KSP does not support generated code access in java sources yet
                // TODO remove this check once the bug is fixed.
                //  https://github.com/google/ksp/issues/119
                if (src === javaSrc) {
                    runProcessorTest(sources = listOf(src), block)
                } else {
                    runProcessorTestIncludingKsp(sources = listOf(src), block)
                }
            }
            runTest { invocation ->
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

    companion object {
        val PRIMITIVE_TYPES = mapOf(
            TypeName.BOOLEAN.toString() to TypeName.BOOLEAN,
            TypeName.BYTE.toString() to TypeName.BYTE,
            TypeName.SHORT.toString() to TypeName.SHORT,
            TypeName.INT.toString() to TypeName.INT,
            TypeName.LONG.toString() to TypeName.LONG,
            TypeName.CHAR.toString() to TypeName.CHAR,
            TypeName.FLOAT.toString() to TypeName.FLOAT,
            TypeName.DOUBLE.toString() to TypeName.DOUBLE
        )
    }
}
