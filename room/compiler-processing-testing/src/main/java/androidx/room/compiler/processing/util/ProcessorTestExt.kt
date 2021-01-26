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

import androidx.room.compiler.processing.util.runner.CompilationTestRunner
import androidx.room.compiler.processing.util.runner.JavacCompilationTestRunner
import androidx.room.compiler.processing.util.runner.KaptCompilationTestRunner
import androidx.room.compiler.processing.util.runner.KspCompilationTestRunner
import androidx.room.compiler.processing.util.runner.TestCompilationParameters
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.tschuchort.compiletesting.KotlinCompilation
import java.io.File

private fun runTests(
    params: TestCompilationParameters,
    vararg runners: CompilationTestRunner
) {
    val runCount = runners.count { runner ->
        if (runner.canRun(params)) {
            val compilationResult = runner.compile(params)
            val subject = CompilationResultSubject.assertThat(compilationResult)
            // if any assertion failed, throw first those.
            subject.assertNoProcessorAssertionErrors()
            compilationResult.processor.invocationInstances.forEach {
                it.runPostCompilationChecks(subject)
            }
            assertWithMessage(
                "compilation should've run the processor callback at least once"
            ).that(
                compilationResult.processor.invocationInstances
            ).isNotEmpty()

            subject.assertCompilationResult()
            true
        } else {
            false
        }
    }
    // make sure some tests did run
    assertThat(runCount).isGreaterThan(0)
}

fun runProcessorTestWithoutKsp(
    sources: List<Source> = emptyList(),
    classpath: List<File> = emptyList(),
    handler: (XTestInvocation) -> Unit
) {
    runTests(
        params = TestCompilationParameters(
            sources = sources,
            classpath = classpath,
            handler = handler
        ),
        JavacCompilationTestRunner,
        KaptCompilationTestRunner
    )
}

/**
 * Runs the compilation test with all 3 backends (javac, kapt, ksp) if possible (e.g. javac
 * cannot test kotlin sources).
 *
 * The [handler] will be invoked for each compilation hence it should be repeatable.
 *
 * To assert on the compilation results, [handler] can call
 * [XTestInvocation.assertCompilationResult] where it will receive a subject for post compilation
 * assertions.
 *
 * By default, the compilation is expected to succeed. If it should fail, there must be an
 * assertion on [XTestInvocation.assertCompilationResult] which expects a failure (e.g. checking
 * errors).
 */
fun runProcessorTest(
    sources: List<Source> = emptyList(),
    classpath: List<File> = emptyList(),
    handler: (XTestInvocation) -> Unit
) {
    runTests(
        params = TestCompilationParameters(
            sources = sources,
            classpath = classpath,
            handler = handler
        ),
        JavacCompilationTestRunner,
        KaptCompilationTestRunner,
        KspCompilationTestRunner
    )
}

/**
 * Runs the test only with javac compilation backend.
 *
 * @see runProcessorTest
 */
fun runJavaProcessorTest(
    sources: List<Source>,
    classpath: List<File> = emptyList(),
    handler: (XTestInvocation) -> Unit
) {
    runTests(
        params = TestCompilationParameters(
            sources = sources,
            classpath = classpath,
            handler = handler
        ),
        JavacCompilationTestRunner
    )
}

/**
 * Runs the test only with kapt compilation backend
 */
fun runKaptTest(
    sources: List<Source>,
    classpath: List<File> = emptyList(),
    handler: (XTestInvocation) -> Unit
) {
    runTests(
        params = TestCompilationParameters(
            sources = sources,
            classpath = classpath,
            handler = handler
        ),
        KaptCompilationTestRunner
    )
}

/**
 * Runs the test only with ksp compilation backend
 */
fun runKspTest(
    sources: List<Source>,
    classpath: List<File> = emptyList(),
    handler: (XTestInvocation) -> Unit
) {
    runTests(
        params = TestCompilationParameters(
            sources = sources,
            classpath = classpath,
            handler = handler
        ),
        KspCompilationTestRunner
    )
}

/**
 * Compiles the given set of sources into a temporary folder and returns the output classes
 * directory.
 */
fun compileFiles(
    sources: List<Source>
): File {
    val compilation = KotlinCompilationUtil.prepareCompilation(sources = sources)
    val result = compilation.compile()
    check(result.exitCode == KotlinCompilation.ExitCode.OK) {
        "compilation failed: ${result.messages}"
    }
    return compilation.classesDir
}
