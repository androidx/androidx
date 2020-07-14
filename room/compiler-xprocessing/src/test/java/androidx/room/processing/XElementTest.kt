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
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class XElementTest {
    @Test
    fun modifiers() {
        runProcessorTest(
            listOf(
                Source.java(
                    "foo.bar.Baz", """
                package foo.bar;
                public abstract class Baz {
                }
            """.trimIndent()
                )
            )
        ) {
            val element = it.processingEnv.requireTypeElement("foo.bar.Baz")
            fun XElement.readModifiers(): Set<String> {
                val result = mutableSetOf<String>()
                if (isPrivate()) result.add("private")
                if (isPublic()) result.add("public")
                if (isTransient()) result.add("transient")
                if (isStatic()) result.add("static")
                if (isFinal()) result.add("final")
                if (isAbstract()) result.add("abstract")
                if (isProtected()) result.add("protected")
                return result
            }

            fun XElement.assertModifiers(vararg expected: String) {
                assertThat(readModifiers()).containsExactlyElementsIn(expected)
            }
            element.assertModifiers("abstract", "public")
        }
    }

    @Test
    fun annotationAvailability() {
        val source = Source.java(
            "foo.bar.Baz", """
            package foo.bar;
            import org.junit.*;
            import org.junit.runner.*;
            import org.junit.runners.*;
            import androidx.room.processing.testcode.OtherAnnotation;

            @RunWith(JUnit4.class)
            class Baz {
            }
        """.trimIndent()
        )
        runProcessorTest(
            listOf(source)
        ) {
            val element = it.processingEnv.requireTypeElement("foo.bar.Baz")
            assertThat(element.hasAnnotation(RunWith::class)).isTrue()
            assertThat(element.hasAnnotation(Test::class)).isFalse()
            assertThat(
                element.hasAnnotationInPackage(
                    "org.junit.runner"
                )
            ).isTrue()
            assertThat(
                element.hasAnnotationInPackage(
                    "org.junit"
                )
            ).isFalse()
            assertThat(
                element.hasAnnotationInPackage(
                    "foo.bar"
                )
            ).isFalse()
        }
    }

    @Test
    fun nonType() {
        val source = Source.java(
            "foo.bar.Baz", """
            package foo.bar;
            class Baz {
            }
        """.trimIndent()
        )
        runProcessorTest(
            listOf(source)
        ) {
            val element = it.processingEnv.requireTypeElement("java.lang.Object")
            // make sure we return null for not existing types
            assertThat(element.superType).isNull()
        }
    }

    @Test
    fun isSomething() {
        val subject = Source.java(
            "foo.bar.Baz", """
            package foo.bar;
            class Baz {
                static interface Inner {}
            }
        """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(subject)
        ) {
            val inner = ClassName.get("foo.bar", "Baz.Inner")
            assertThat(
                it.processingEnv.requireTypeElement(inner).isInterface()
            ).isTrue()
            val element = it.processingEnv.requireTypeElement("foo.bar.Baz")
            assertThat(element.isInterface()).isFalse()
            assertThat(element.isAbstract()).isFalse()
            assertThat(element.isType()).isTrue()
        }
    }

    @Test
    fun toStringMatchesUnderlyingElement() {
        runProcessorTest {
            it.processingEnv.findTypeElement("java.util.List").let { list ->
                assertThat(list.toString()).isEqualTo("java.util.List")
            }
        }
    }
}
