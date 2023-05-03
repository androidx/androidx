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
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import javax.tools.Diagnostic

@RunWith(JUnit4::class)
class XMessagerTest {
    @Test
    fun errorLogTest() {
        runProcessorTest(
            sources = listOf(
                Source.java(
                    "Foo",
                    """
                    class Foo {}
                    """.trimIndent()
                )
            )
        ) {
            it.processingEnv.messager.printMessage(
                Diagnostic.Kind.ERROR,
                "intentional failure"
            )
            it.assertCompilationResult {
                compilationDidFail()
                hasErrorCount(1)
                hasWarningCount(0)
                hasError("intentional failure")
            }
        }
    }

    @Test
    fun warningLogTest() {
        runProcessorTest(
            sources = listOf(
                Source.java(
                    "Foo",
                    """
                    class Foo {}
                    """.trimIndent()
                )
            )
        ) {
            it.processingEnv.messager.printMessage(
                Diagnostic.Kind.WARNING,
                "intentional warning"
            )
            it.assertCompilationResult {
                hasErrorCount(0)
                hasWarningCount(1)
                hasWarning("intentional warning")
            }
        }
    }

    @Test
    fun noteLogTest() {
        runProcessorTest(
            sources = listOf(
                Source.java(
                    "Foo",
                    """
                    class Foo {}
                    """.trimIndent()
                )
            )
        ) {
            it.processingEnv.messager.printMessage(
                Diagnostic.Kind.NOTE,
                "intentional note"
            )
            it.assertCompilationResult {
                hasErrorCount(0)
                hasWarningCount(0)
                hasNote("intentional note")
            }
        }
    }

    @Test
    fun errorOnElementTest() {
        runProcessorTest(
            sources = listOf(
                Source.java(
                    "Foo",
                    """
                    class Foo {}
                    """.trimIndent()
                )
            )
        ) {
            val fooElement = it.processingEnv.requireTypeElement("Foo")
            it.processingEnv.messager.printMessage(
                Diagnostic.Kind.ERROR,
                "intentional failure",
                fooElement
            )
            it.assertCompilationResult {
                compilationDidFail()
                hasErrorCount(1)
                hasWarningCount(0)
                hasErrorContaining("intentional failure")
                    .onLineContaining("class Foo")
            }
        }
    }

    @Test
    fun errorOnAnnotationTest() {
        runProcessorTest(
            sources = listOf(
                Source.java(
                    "test.FooAnnotation",
                    """
                    package test;
                    @interface FooAnnotation {}
                    """.trimIndent()
                ),
                Source.java(
                    "test.Foo",
                    """
                    package test;
                    @FooAnnotation
                    class Foo {}
                    """.trimIndent()
                )
            )
        ) {
            val fooElement = it.processingEnv.requireTypeElement("test.Foo")
            val fooAnnotations = fooElement.getAllAnnotations().filter {
                it.qualifiedName == "test.FooAnnotation"
            }
            assertThat(fooAnnotations).hasSize(1)
            val fooAnnotation = fooAnnotations.get(0)
            it.processingEnv.messager.printMessage(
                Diagnostic.Kind.ERROR,
                "intentional failure",
                fooElement,
                fooAnnotation
            )
            it.assertCompilationResult {
                compilationDidFail()
                hasErrorCount(1)
                hasWarningCount(0)
                hasErrorContaining("intentional failure")
                    .onLineContaining("@FooAnnotation")
            }
        }
    }

    @Test
    fun errorOnAnnotationValueTest() {
        runProcessorTest(
            sources = listOf(
                Source.java(
                    "test.FooAnnotation",
                    """
                    package test;
                    @interface FooAnnotation {
                      String value();
                    }
                    """.trimIndent()
                ),
                Source.java(
                    "test.Foo",
                    """
                    package test;
                    @FooAnnotation("fooValue")
                    class Foo {}
                    """.trimIndent()
                )
            )
        ) {
            val fooElement = it.processingEnv.requireTypeElement("test.Foo")
            val fooAnnotations = fooElement.getAllAnnotations().filter {
                it.qualifiedName == "test.FooAnnotation"
            }
            assertThat(fooAnnotations).hasSize(1)
            val fooAnnotation = fooAnnotations.get(0)
            val fooAnnotationValue = fooAnnotation.annotationValues.first {
                it.name == "value"
            }
            assertThat(fooAnnotationValue).isNotNull()
            it.processingEnv.messager.printMessage(
                Diagnostic.Kind.ERROR,
                "intentional failure",
                fooElement,
                fooAnnotation,
                fooAnnotationValue
            )
            it.assertCompilationResult {
                compilationDidFail()
                hasErrorCount(1)
                hasWarningCount(0)
                hasErrorContaining("intentional failure")
                    .onLineContaining("@FooAnnotation")
            }
        }
    }
}
