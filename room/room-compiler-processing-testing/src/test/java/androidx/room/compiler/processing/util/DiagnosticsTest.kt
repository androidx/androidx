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
                assertThat(shouldSucceed).isTrue()
                hasNoteContaining("ote")
                assertThat(shouldSucceed).isTrue()
                hasNoteContainingMatch("ote")
                hasNoteContainingMatch("^note \\d$")
                assertThat(shouldSucceed).isTrue()

                hasWarning("warn 1")
                assertThat(shouldSucceed).isTrue()
                hasWarningContaining("arn")
                assertThat(shouldSucceed).isTrue()
                hasWarningContainingMatch("arn")
                hasWarningContainingMatch("^warn \\d$")
                assertThat(shouldSucceed).isTrue()

                hasError("error 1")
                assertThat(shouldSucceed).isFalse()
                hasErrorContaining("rror")
                assertThat(shouldSucceed).isFalse()
                hasErrorContainingMatch("rror")
                hasErrorContainingMatch("^error \\d$")
                assertThat(shouldSucceed).isFalse()

                hasNote("note 1")
                assertThat(shouldSucceed).isFalse()
                hasNoteContaining("ote")
                assertThat(shouldSucceed).isFalse()
                hasNoteContainingMatch("ote")
                hasNoteContainingMatch("^note \\d$")
                assertThat(shouldSucceed).isFalse()

                hasWarning("warn 1")
                assertThat(shouldSucceed).isFalse()
                hasWarningContaining("arn")
                assertThat(shouldSucceed).isFalse()
                hasWarningContainingMatch("arn")
                hasWarningContainingMatch("^warn \\d$")
                assertThat(shouldSucceed).isFalse()

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
                assertThat(
                    runCatching { hasNoteContainingMatch("error %d") }.isFailure
                ).isTrue()
                assertThat(
                    runCatching { hasWarningContainingMatch("note %d") }.isFailure
                ).isTrue()
                assertThat(
                    runCatching { hasErrorContainingMatch("warning %d") }.isFailure
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
            invocation.assertCompilationResult {
                hasWarningContaining("on field")
                    .onLine(3)
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
            "Subject.kt",
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

    @Test
    fun diagnoticMessageCompareTrimmedLines() {
        runTest { invocation ->
            invocation.processingEnv.messager.run {
                printMessage(Diagnostic.Kind.ERROR, "error: This is the first line\n" +
                    "    This is the second line\n" +
                    "    This is the third line")
            }
            invocation.assertCompilationResult {
                hasError("error: This is the first line\n" +
                    "      This is the second line\n" +
                    "      This is the third line")

                hasErrorContaining("   This is the second line  \n This is the third  ")

                assertThat(
                    runCatching { hasError("error: This is the \nfirst line" +
                        "This is the \nsecond line" +
                        "This is the third line") }.isFailure
                ).isTrue()
            }
        }
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