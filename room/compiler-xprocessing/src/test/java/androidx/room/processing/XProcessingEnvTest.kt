/*
 * Copyright (C) 2020 The Android Open Source Project
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

package androidx.room.processing

import androidx.room.processing.util.Source
import androidx.room.processing.util.runProcessorTest
import com.google.common.truth.Truth.assertThat
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
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
                    "foo.bar.Baz", """
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
        runProcessorTest(
            listOf(
                Source.java(
                    "foo.bar.Baz", """
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
            "foo.bar.Baz", """
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
