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
import androidx.room.compiler.processing.XProcessingStep
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.util.runner.CompilationTestRunner
import androidx.room.compiler.processing.util.runner.JavacCompilationTestRunner
import androidx.room.compiler.processing.util.runner.KaptCompilationTestRunner
import androidx.room.compiler.processing.util.runner.KspCompilationTestRunner
import androidx.room.compiler.processing.util.runner.TestCompilationParameters
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.kspArgs
import com.tschuchort.compiletesting.symbolProcessorProviders
import java.io.ByteArrayOutputStream
import java.io.File
import javax.annotation.processing.Processor

@ExperimentalProcessingApi
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
            subject.assertAllExpectedRoundsAreCompleted()
            true
        } else {
            false
        }
    }
    // make sure some tests did run. Ksp tests might be disabled so if it is the only test given,
    // ignore the check
    val minTestCount = when {
        CompilationTestCapabilities.canTestWithKsp ||
            (runners.toList() - KspCompilationTestRunner).isNotEmpty() -> {
            1
        }
        else -> {
            // is ok if we don't run any tests if ksp is disabled and it is the only test
            0
        }
    }
    assertThat(runCount).isAtLeast(minTestCount)
}

@ExperimentalProcessingApi
fun runProcessorTestWithoutKsp(
    sources: List<Source> = emptyList(),
    classpath: List<File> = emptyList(),
    options: Map<String, String> = emptyMap(),
    handler: (XTestInvocation) -> Unit
) {
    runTests(
        params = TestCompilationParameters(
            sources = sources,
            classpath = classpath,
            options = options,
            handlers = listOf(handler)
        ),
        JavacCompilationTestRunner,
        KaptCompilationTestRunner
    )
}

/**
 * Runs the compilation test with ksp and one of javac or kapt, depending on whether input has
 * kotlin sources.
 *
 * The [handler] will be invoked only for the first round. If you need to test multi round
 * processing, use `handlers = listOf(..., ...)`.
 *
 * To assert on the compilation results, [handler] can call
 * [XTestInvocation.assertCompilationResult] where it will receive a subject for post compilation
 * assertions.
 *
 * By default, the compilation is expected to succeed. If it should fail, there must be an
 * assertion on [XTestInvocation.assertCompilationResult] which expects a failure (e.g. checking
 * errors).
 */
@ExperimentalProcessingApi
fun runProcessorTest(
    sources: List<Source> = emptyList(),
    classpath: List<File> = emptyList(),
    options: Map<String, String> = emptyMap(),
    handler: (XTestInvocation) -> Unit
) = runProcessorTest(
    sources = sources,
    classpath = classpath,
    options = options,
    handlers = listOf(handler)
)

/**
 * Runs the step created by [createProcessingStep] with ksp and one of javac or kapt, depending
 * on whether input has kotlin sources.
 *
 * The step created by [createProcessingStep] will be invoked only for the first round.
 *
 * [onCompilationResult] will be called with a [CompilationResultSubject] after each compilation to
 * assert the compilation result.
 *
 * By default, the compilation is expected to succeed. If it should fail, there must be an
 * assertion on [onCompilationResult] which expects a failure (e.g. checking errors).
 */
@Suppress("VisibleForTests") // this is a test library
@ExperimentalProcessingApi
fun runProcessorTest(
    sources: List<Source> = emptyList(),
    classpath: List<File> = emptyList(),
    options: Map<String, String> = emptyMap(),
    createProcessingStep: () -> XProcessingStep,
    onCompilationResult: (CompilationResultSubject) -> Unit
) {
    runProcessorTest(
        sources = sources,
        classpath = classpath,
        options = options
    ) { invocation ->
        val step = createProcessingStep()
        val elements =
            step.annotations()
                .associateWith { annotation ->
                    invocation.roundEnv.getElementsAnnotatedWith(annotation)
                        .filterIsInstance<XTypeElement>()
                        .toSet()
                }
        step.process(
            env = invocation.processingEnv,
            elementsByAnnotation = elements
        )
        invocation.assertCompilationResult(onCompilationResult)
    }
}

/**
 * @see runProcessorTest
 */
@ExperimentalProcessingApi
fun runProcessorTest(
    sources: List<Source> = emptyList(),
    classpath: List<File> = emptyList(),
    options: Map<String, String> = emptyMap(),
    handlers: List<(XTestInvocation) -> Unit>
) {
    val javaApRunner = if (sources.any { it is Source.KotlinSource }) {
        KaptCompilationTestRunner
    } else {
        JavacCompilationTestRunner
    }
    runTests(
        params = TestCompilationParameters(
            sources = sources,
            classpath = classpath,
            options = options,
            handlers = handlers
        ),
        javaApRunner,
        KspCompilationTestRunner
    )
}

/**
 * Runs the test only with javac compilation backend.
 *
 * @see runProcessorTest
 */
@ExperimentalProcessingApi
fun runJavaProcessorTest(
    sources: List<Source>,
    classpath: List<File> = emptyList(),
    options: Map<String, String> = emptyMap(),
    handler: (XTestInvocation) -> Unit
) = runJavaProcessorTest(
    sources = sources,
    classpath = classpath,
    options = options,
    handlers = listOf(handler)
)

/**
 * @see runJavaProcessorTest
 */
@ExperimentalProcessingApi
fun runJavaProcessorTest(
    sources: List<Source>,
    classpath: List<File> = emptyList(),
    options: Map<String, String> = emptyMap(),
    handlers: List<(XTestInvocation) -> Unit>
) {
    runTests(
        params = TestCompilationParameters(
            sources = sources,
            classpath = classpath,
            options = options,
            handlers = handlers
        ),
        JavacCompilationTestRunner
    )
}

/**
 * Runs the test only with kapt compilation backend
 */
@ExperimentalProcessingApi
fun runKaptTest(
    sources: List<Source>,
    classpath: List<File> = emptyList(),
    options: Map<String, String> = emptyMap(),
    handler: (XTestInvocation) -> Unit
) = runKaptTest(
    sources = sources,
    classpath = classpath,
    options = options,
    handlers = listOf(handler)
)

/**
 * @see runKaptTest
 */
@ExperimentalProcessingApi
fun runKaptTest(
    sources: List<Source>,
    classpath: List<File> = emptyList(),
    options: Map<String, String> = emptyMap(),
    handlers: List<(XTestInvocation) -> Unit>
) {
    runTests(
        params = TestCompilationParameters(
            sources = sources,
            classpath = classpath,
            options = options,
            handlers = handlers
        ),
        KaptCompilationTestRunner
    )
}

/**
 * Runs the test only with ksp compilation backend
 */
@ExperimentalProcessingApi
fun runKspTest(
    sources: List<Source>,
    classpath: List<File> = emptyList(),
    options: Map<String, String> = emptyMap(),
    handler: (XTestInvocation) -> Unit
) = runKspTest(
    sources = sources,
    classpath = classpath,
    options = options,
    handlers = listOf(handler)
)

/**
 * @see runKspTest
 */
@ExperimentalProcessingApi
fun runKspTest(
    sources: List<Source>,
    classpath: List<File> = emptyList(),
    options: Map<String, String> = emptyMap(),
    handlers: List<(XTestInvocation) -> Unit>
) {
    runTests(
        params = TestCompilationParameters(
            sources = sources,
            classpath = classpath,
            options = options,
            handlers = handlers
        ),
        KspCompilationTestRunner
    )
}

/**
 * Compiles the given set of sources into a temporary folder and returns the output classes
 * directory.
 * @param sources The list of source files to compile
 * @param options The annotation processor arguments
 * @param annotationProcessors The list of Java annotation processors to run with compilation
 * @param symbolProcessorProviders The list of Kotlin symbol processor providers to run with
 * compilation
 * @param javacArguments The command line arguments that will be passed into javac
 */
fun compileFiles(
    sources: List<Source>,
    options: Map<String, String> = emptyMap(),
    annotationProcessors: List<Processor> = emptyList(),
    symbolProcessorProviders: List<SymbolProcessorProvider> = emptyList(),
    javacArguments: List<String> = emptyList()
): File {
    val outputStream = ByteArrayOutputStream()
    val compilation = KotlinCompilationUtil.prepareCompilation(
        sources = sources,
        outputStream = outputStream
    )
    if (annotationProcessors.isNotEmpty()) {
        compilation.kaptArgs.putAll(options)
    }
    if (symbolProcessorProviders.isNotEmpty()) {
        compilation.kspArgs.putAll(options)
    }
    compilation.javacArguments.addAll(javacArguments)
    compilation.annotationProcessors = annotationProcessors
    compilation.symbolProcessorProviders = symbolProcessorProviders
    val result = compilation.compile()
    check(result.exitCode == KotlinCompilation.ExitCode.OK) {
        "compilation failed: ${outputStream.toString(Charsets.UTF_8)}"
    }
    return compilation.classesDir
}
