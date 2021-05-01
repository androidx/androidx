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

package androidx.room.compiler.processing.util

import androidx.room.compiler.processing.ExperimentalProcessingApi
import androidx.room.compiler.processing.XProcessingEnv
import com.google.common.truth.Truth.assertThat
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import org.junit.Test
import javax.tools.Diagnostic

@OptIn(ExperimentalProcessingApi::class)
class TestRunnerTest {
    @Test
    fun generatedBadCode_expected() = generatedBadCode(assertFailure = true)

    @Test(expected = AssertionError::class)
    fun generatedBadCode_unexpected() = generatedBadCode(assertFailure = false)

    @Test
    fun options() {
        val testOptions = mapOf(
            "a" to "b",
            "c" to "d"
        )
        runProcessorTest(
            options = testOptions
        ) {
            assertThat(it.processingEnv.options).containsAtLeastEntriesIn(testOptions)
        }
    }

    private fun generatedBadCode(assertFailure: Boolean) {
        runProcessorTest {
            if (it.processingEnv.findTypeElement("foo.Foo") == null) {
                val badCode = TypeSpec.classBuilder("Foo").apply {
                    addStaticBlock(
                        CodeBlock.of("bad code")
                    )
                }.build()
                val badGeneratedFile = JavaFile.builder("foo", badCode).build()
                it.processingEnv.filer.write(
                    badGeneratedFile
                )
            }
            if (assertFailure) {
                it.assertCompilationResult {
                    compilationDidFail()
                }
            }
        }
    }

    @Test
    fun reportedError_expected() = reportedError(assertFailure = true)

    @Test(expected = AssertionError::class)
    fun reportedError_unexpected() = reportedError(assertFailure = false)

    @Test
    fun diagnosticsMessages() {
        runProcessorTest { invocation ->
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

    private fun reportedError(assertFailure: Boolean) {
        runProcessorTest {
            it.processingEnv.messager.printMessage(
                kind = Diagnostic.Kind.ERROR,
                msg = "reported error"
            )
            if (assertFailure) {
                it.assertCompilationResult {
                    hasError("reported error")
                }
            }
        }
    }

    @Test
    fun syntacticErrorsAreVisibleInTheErrorMessage_java() {
        val src = Source.java(
            "test.Foo",
            """
            package test;
            // static here is invalid, causes a Java syntax error
            public static class Foo {}
            """.trimIndent()
        )
        val errorMessage = "error: modifier static not allowed here"
        val javapResult = runCatching {
            runJavaProcessorTest(
                sources = listOf(src),
                classpath = emptyList()
            ) {}
        }
        assertThat(javapResult.exceptionOrNull()).hasMessageThat()
            .contains(errorMessage)

        val kaptResult = runCatching {
            runKaptTest(
                sources = listOf(src)
            ) {}
        }
        assertThat(kaptResult.exceptionOrNull()).hasMessageThat()
            .contains(errorMessage)

        if (CompilationTestCapabilities.canTestWithKsp) {
            val kspResult = runCatching {
                runKspTest(
                    sources = listOf(src)
                ) {}
            }
            assertThat(kspResult.exceptionOrNull()).hasMessageThat()
                .contains(errorMessage)
        }
    }

    @Test
    fun syntacticErrorsAreVisibleInTheErrorMessage_kotlin() {
        val src = Source.kotlin(
            "Foo.kt",
            """
            package foo;
            bad code
            """.trimIndent()
        )
        val errorMessage = "Expecting a top level declaration"
        val kaptResult = runCatching {
            runKaptTest(
                sources = listOf(src)
            ) {}
        }
        assertThat(kaptResult.exceptionOrNull()).hasMessageThat()
            .contains(errorMessage)

        if (CompilationTestCapabilities.canTestWithKsp) {
            val kspResult = runCatching {
                runKspTest(
                    sources = listOf(src)
                ) {}
            }
            assertThat(kspResult.exceptionOrNull()).hasMessageThat()
                .contains(errorMessage)
        }
    }

    @Test
    fun targetLanguageIsPassedDown() {
        val src = Source.java(
            "test.Foo",
            """
            package test;
            public class Foo { }
            """.trimIndent()
        )
        val handler: (XTestInvocation) -> Unit = { invocation ->
            assertThat(invocation.processingEnv.targetLanguage)
                .isEqualTo(XProcessingEnv.Language.JAVA)
        }

        runProcessorTest(
            targetLanguage = XProcessingEnv.Language.JAVA,
            handler = handler
        )
        runProcessorTest(
            targetLanguage = XProcessingEnv.Language.JAVA,
            handlers = listOf(handler)
        )
        runProcessorTestWithoutKsp(
            targetLanguage = XProcessingEnv.Language.JAVA,
            handler = handler
        )
        runJavaProcessorTest(
            sources = listOf(src),
            targetLanguage = XProcessingEnv.Language.JAVA,
            handler = handler
        )
        runJavaProcessorTest(
            sources = listOf(src),
            targetLanguage = XProcessingEnv.Language.JAVA,
            handlers = listOf(handler)
        )
        runKaptTest(
            sources = listOf(src),
            targetLanguage = XProcessingEnv.Language.JAVA,
            handler = handler
        )
        runKaptTest(
            sources = listOf(src),
            targetLanguage = XProcessingEnv.Language.JAVA,
            handlers = listOf(handler)
        )
        runKspTest(
            sources = listOf(src),
            targetLanguage = XProcessingEnv.Language.JAVA,
            handler = handler
        )
        runKspTest(
            sources = listOf(src),
            targetLanguage = XProcessingEnv.Language.JAVA,
            handlers = listOf(handler)
        )
    }

    @Test
    fun targetLanguageIsPassedDown_kotlin() {
        val src = Source.kotlin(
            "Foo.kt",
            """
            package foo
            class Foo { }
            """.trimIndent()
        )
        val handler: (XTestInvocation) -> Unit = { invocation ->
            assertThat(invocation.processingEnv.targetLanguage)
                .isEqualTo(XProcessingEnv.Language.KOTLIN)
        }

        runProcessorTest(
            targetLanguage = XProcessingEnv.Language.KOTLIN,
            handler = handler
        )
        runProcessorTest(
            targetLanguage = XProcessingEnv.Language.KOTLIN,
            handlers = listOf(handler)
        )
        runProcessorTestWithoutKsp(
            targetLanguage = XProcessingEnv.Language.KOTLIN,
            handler = handler
        )
        runKaptTest(
            sources = listOf(src),
            targetLanguage = XProcessingEnv.Language.KOTLIN,
            handler = handler
        )
        runKaptTest(
            sources = listOf(src),
            targetLanguage = XProcessingEnv.Language.KOTLIN,
            handlers = listOf(handler)
        )
        runKspTest(
            sources = listOf(src),
            targetLanguage = XProcessingEnv.Language.KOTLIN,
            handler = handler
        )
        runKspTest(
            sources = listOf(src),
            targetLanguage = XProcessingEnv.Language.KOTLIN,
            handlers = listOf(handler)
        )
    }
}