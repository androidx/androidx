/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.room.compiler.processing.util

import androidx.room.compiler.processing.ExperimentalProcessingApi
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import javax.tools.Diagnostic

@RunWith(Parameterized::class)
@OptIn(ExperimentalProcessingApi::class)
class DiagnosticsTest internal constructor(
    private val runTest: TestRunner
) : MultiBackendTest() {

    @Test
    fun diagnosticsMessagesWithoutSource() {
        runTest { invocation ->
            invocation.processingEnv.messager.run {
                printMessage(Diagnostic.Kind.NOTE, "note 1")
                printMessage(Diagnostic.Kind.WARNING, "warn 1")
                printMessage(Diagnostic.Kind.ERROR, "error 1")
            }
            invocation.assertCompilationResult {
                hasNote("note 1")
                hasWarning("warn 1")
                hasError("error 1")
                hasNoteContaining("ote")
                hasWarningContaining("arn")
                hasErrorContaining("rror")
                // these should fail:
                assertThat(
                    runCatching { hasNote("note") }.isFailure
                ).isTrue()
                assertThat(
                    runCatching { hasWarning("warn") }.isFailure
                ).isTrue()
                assertThat(
                    runCatching { hasError("error") }.isFailure
                ).isTrue()
                assertThat(
                    runCatching { hasNoteContaining("error") }.isFailure
                ).isTrue()
                assertThat(
                    runCatching { hasWarningContaining("note") }.isFailure
                ).isTrue()
                assertThat(
                    runCatching { hasErrorContaining("warning") }.isFailure
                ).isTrue()
            }
        }
    }

    @Test
    fun diagnoticMessageOnKotlinSource() {
        runTest.assumeCanCompileKotlin()
        val source = Source.kotlin(
            "Subject.kt",
            """
            package foo.bar
            class Subject {
                val field: String = "foo"
            }
            """.trimIndent()
        )
        runTest(listOf(source)) { invocation ->
            val field = invocation.processingEnv.requireTypeElement("foo.bar.Subject")
                .getDeclaredFields().first()
            invocation.processingEnv.messager.printMessage(
                kind = Diagnostic.Kind.WARNING,
                msg = "warning on field",
                element = field
            )
            val expectedLine = if (invocation.isKsp) {
                3
            } else {
                // KAPT fails to report lines properly in certain cases
                //  (e.g. when searching for field, it uses its parent rather than itself)
                // Hopefully, once it is fixed, this test will break and we can remove this if/else
                // https://youtrack.jetbrains.com/issue/KT-47934
                2
            }
            invocation.assertCompilationResult {
                hasWarningContaining("on field")
                    .onLine(expectedLine)
                    .onSource(source)
            }
        }
    }

    @Test
    fun diagnoticMessageOnJavaSource() {
        val source = Source.java(
            "foo.bar.Subject",
            """
            package foo.bar;
            public class Subject {
                String field = "";
            }
            """.trimIndent()
        )
        runTest(listOf(source)) { invocation ->
            val field = invocation.processingEnv.requireTypeElement("foo.bar.Subject")
                .getDeclaredFields().first()
            invocation.processingEnv.messager.printMessage(
                kind = Diagnostic.Kind.WARNING,
                msg = "warning on field",
                element = field
            )
            invocation.assertCompilationResult {
                hasWarningContaining("on field")
                    .onLine(3)
                    .onSource(source)
            }
        }
    }

    @Test
    fun cleanJavaCompilationHasNoWarnings() {
        val javaSource = Source.java(
            "foo.bar.Subject",
            """
            package foo.bar;
            public class Subject {
            }
            """.trimIndent()
        )
        cleanCompilationHasNoWarnings(javaSource)
        cleanCompilationHasNoWarnings(
            options = mapOf("foo" to "bar"),
            javaSource
        )
    }

    @Test
    fun cleanKotlinCompilationHasNoWarnings() {
        val kotlinSource = Source.kotlin(
            "Subject",
            """
            package foo.bar
            class Subject {
            }
            """.trimIndent()
        )
        cleanCompilationHasNoWarnings(kotlinSource)
        cleanCompilationHasNoWarnings(
            options = mapOf("foo" to "bar"),
            kotlinSource
        )
    }

    @Test
    fun cleanJavaCompilationWithSomeAnnotationsHasNoWarnings() {
        val annotation = Source.java(
            "foo.bar.MyAnnotation",
            """
            package foo.bar;
            public @interface MyAnnotation {}
            """.trimIndent()
        )
        val source = Source.java(
            "foo.bar.Subject",
            """
            package foo.bar;
            @MyAnnotation
            public class Subject {}
            """.trimIndent()
        )
        cleanCompilationHasNoWarnings(annotation, source)
    }

    @Test
    fun cleanKotlinCompilationWithSomeAnnotationsHasNoWarnings() {
        val source = Source.kotlin(
            "Foo.kt",
            """
            annotation class MyAnnotation

            @MyAnnotation
            class Subject {}
            """.trimIndent()
        )
        cleanCompilationHasNoWarnings(source)
    }

    private fun cleanCompilationHasNoWarnings(
        vararg source: Source
    ) = cleanCompilationHasNoWarnings(options = emptyMap(), source = source)

    private fun cleanCompilationHasNoWarnings(
        options: Map<String, String>,
        vararg source: Source
    ) {
        if (source.any { it is Source.KotlinSource }) {
            runTest.assumeCanCompileKotlin()
        }
        runTest(options = options, sources = source.toList()) {
            // no report
            it.assertCompilationResult {
                hasNoWarnings()
            }
        }
    }
}