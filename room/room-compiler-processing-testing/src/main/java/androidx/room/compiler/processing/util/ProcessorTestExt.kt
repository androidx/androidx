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
import androidx.room.compiler.processing.XProcessingEnvConfig
import androidx.room.compiler.processing.XProcessingEnvironmentTestConfigProvider
import androidx.room.compiler.processing.XProcessingStep
import androidx.room.compiler.processing.javac.JavacBasicAnnotationProcessor
import androidx.room.compiler.processing.ksp.KspBasicAnnotationProcessor
import androidx.room.compiler.processing.util.compiler.TestCompilationArguments
import androidx.room.compiler.processing.util.compiler.compile
import androidx.room.compiler.processing.util.runner.CompilationTestRunner
import androidx.room.compiler.processing.util.runner.JavacCompilationTestRunner
import androidx.room.compiler.processing.util.runner.KaptCompilationTestRunner
import androidx.room.compiler.processing.util.runner.KspCompilationTestRunner
import androidx.room.compiler.processing.util.runner.TestCompilationParameters
import com.google.common.io.Files
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import javax.annotation.processing.Processor
import javax.lang.model.SourceVersion

private fun defaultTestConfig(options: Map<String, String>) =
    XProcessingEnvironmentTestConfigProvider.createConfig(options)

@ExperimentalProcessingApi
private fun runTests(params: TestCompilationParameters, vararg runners: CompilationTestRunner) {
    val runCount =
        runners.count { runner ->
            if (runner.canRun(params)) {
                withTempDir { tmpDir ->
                    val compilationResult = runner.compile(tmpDir, params)
                    val subject = CompilationResultSubject.assertThat(compilationResult)
                    // if any assertion failed, throw first those.
                    subject.assertNoProcessorAssertionErrors()
                    compilationResult.processor.invocationInstances.forEach {
                        it.runPostCompilationChecks(subject)
                    }
                    assertWithMessage(
                            "compilation should've run the processor callback at least once"
                        )
                        .that(compilationResult.processor.invocationInstances)
                        .isNotEmpty()

                    subject.assertCompilationResult()
                    subject.assertAllExpectedRoundsAreCompleted()
                }
                true
            } else {
                false
            }
        }
    // make sure some tests did run. Ksp tests might be disabled so if it is the only test given,
    // ignore the check
    val minTestCount =
        when {
            CompilationTestCapabilities.canTestWithKsp ||
                (runners.count { it !is KspCompilationTestRunner } > 0) -> {
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
    javacArguments: List<String> = emptyList(),
    kotlincArguments: List<String> = emptyList(),
    config: XProcessingEnvConfig = defaultTestConfig(options),
    handler: (XTestInvocation) -> Unit
) {
    runTests(
        params =
            TestCompilationParameters(
                sources = sources,
                classpath = classpath,
                options = options,
                javacArguments = javacArguments,
                kotlincArguments = kotlincArguments,
                config = config,
                handlers = listOf(handler),
            ),
        JavacCompilationTestRunner(),
        KaptCompilationTestRunner()
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
 * By default, the compilation is expected to succeed. If it should fail, there must be an assertion
 * on [XTestInvocation.assertCompilationResult] which expects a failure (e.g. checking errors).
 */
@ExperimentalProcessingApi
fun runProcessorTest(
    sources: List<Source> = emptyList(),
    classpath: List<File> = emptyList(),
    options: Map<String, String> = emptyMap(),
    javacArguments: List<String> = emptyList(),
    kotlincArguments: List<String> = emptyList(),
    config: XProcessingEnvConfig = defaultTestConfig(options),
    handler: (XTestInvocation) -> Unit
) =
    runProcessorTest(
        sources = sources,
        classpath = classpath,
        options = options,
        javacArguments = javacArguments,
        kotlincArguments = kotlincArguments,
        config = config,
        handlers = listOf(handler)
    )

/**
 * Runs the steps created by [createProcessingSteps] with ksp and one of javac or kapt (depending on
 * whether input has kotlin sources).
 *
 * The steps will be contained in implementations of
 * [androidx.room.compiler.processing.XBasicAnnotationProcessor] and are subject to its validation
 * and element deferring behaviour.
 *
 * [onCompilationResult] will be called with a [CompilationResultSubject] after each compilation to
 * assert the compilation result.
 *
 * By default, the compilation is expected to succeed. If it should fail, there must be an assertion
 * on [onCompilationResult] which expects a failure (e.g. checking errors).
 */
@ExperimentalProcessingApi
fun runProcessorTest(
    sources: List<Source> = emptyList(),
    classpath: List<File> = emptyList(),
    options: Map<String, String> = emptyMap(),
    javacArguments: List<String> = emptyList(),
    kotlincArguments: List<String> = emptyList(),
    config: XProcessingEnvConfig = defaultTestConfig(options),
    createProcessingSteps: () -> Iterable<XProcessingStep>,
    onCompilationResult: (CompilationResultSubject) -> Unit
) {
    val javacProcessor =
        object : JavacBasicAnnotationProcessor(configureEnv = { config }) {
            override fun getSupportedSourceVersion() = SourceVersion.latestSupported()

            override fun processingSteps() = createProcessingSteps()
        }
    val ksProvider = SymbolProcessorProvider { environment ->
        object :
            KspBasicAnnotationProcessor(symbolProcessorEnvironment = environment, config = config) {
            override fun processingSteps() = createProcessingSteps()
        }
    }
    runProcessorTest(
        sources = sources,
        classpath = classpath,
        options = options,
        javacArguments = javacArguments,
        kotlincArguments = kotlincArguments,
        config = config,
        javacProcessors = listOf(javacProcessor),
        symbolProcessorProviders = listOf(ksProvider),
        onCompilationResult = onCompilationResult
    )
}

/**
 * Runs the [javacProcessors] with one of javac or kapt (depending on whether input has kotlin
 * sources) and the [symbolProcessorProviders] with ksp.
 *
 * [onCompilationResult] will be called with a [CompilationResultSubject] after each compilation to
 * assert the compilation result.
 *
 * By default, the compilation is expected to succeed. If it should fail, there must be an assertion
 * on [onCompilationResult] which expects a failure (e.g. checking errors).
 */
@ExperimentalProcessingApi
fun runProcessorTest(
    sources: List<Source> = emptyList(),
    classpath: List<File> = emptyList(),
    options: Map<String, String> = emptyMap(),
    javacArguments: List<String> = emptyList(),
    kotlincArguments: List<String> = emptyList(),
    config: XProcessingEnvConfig = defaultTestConfig(options),
    javacProcessors: List<Processor>,
    symbolProcessorProviders: List<SymbolProcessorProvider>,
    onCompilationResult: (CompilationResultSubject) -> Unit
) {
    val javaApRunner =
        if (sources.any { it is Source.KotlinSource }) {
            KaptCompilationTestRunner(javacProcessors)
        } else {
            JavacCompilationTestRunner(javacProcessors)
        }
    val handler: (XTestInvocation) -> Unit = { it.assertCompilationResult(onCompilationResult) }
    runTests(
        params =
            TestCompilationParameters(
                sources = sources,
                classpath = classpath.distinct(),
                options = options,
                handlers = listOf(handler),
                javacArguments = javacArguments,
                kotlincArguments = kotlincArguments,
                config = config
            ),
        javaApRunner,
        KspCompilationTestRunner(symbolProcessorProviders)
    )
}

/** @see runProcessorTest */
@ExperimentalProcessingApi
fun runProcessorTest(
    sources: List<Source> = emptyList(),
    classpath: List<File> = emptyList(),
    options: Map<String, String> = emptyMap(),
    javacArguments: List<String> = emptyList(),
    kotlincArguments: List<String> = emptyList(),
    config: XProcessingEnvConfig = defaultTestConfig(options),
    handlers: List<(XTestInvocation) -> Unit>
) {
    val javaApRunner =
        if (sources.any { it is Source.KotlinSource }) {
            KaptCompilationTestRunner()
        } else {
            JavacCompilationTestRunner()
        }
    runTests(
        params =
            TestCompilationParameters(
                sources = sources,
                classpath = classpath.distinct(),
                options = options,
                handlers = handlers,
                javacArguments = javacArguments,
                kotlincArguments = kotlincArguments,
                config = config
            ),
        javaApRunner,
        KspCompilationTestRunner()
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
    config: XProcessingEnvConfig = defaultTestConfig(options),
    handler: (XTestInvocation) -> Unit
) =
    runJavaProcessorTest(
        sources = sources,
        classpath = classpath,
        options = options,
        config = config,
        handlers = listOf(handler)
    )

/** @see runJavaProcessorTest */
@ExperimentalProcessingApi
fun runJavaProcessorTest(
    sources: List<Source>,
    classpath: List<File> = emptyList(),
    options: Map<String, String> = emptyMap(),
    config: XProcessingEnvConfig = defaultTestConfig(options),
    handlers: List<(XTestInvocation) -> Unit>
) {
    runTests(
        params =
            TestCompilationParameters(
                sources = sources,
                classpath = classpath,
                options = options,
                handlers = handlers,
                config = config,
            ),
        JavacCompilationTestRunner()
    )
}

/** Runs the test only with kapt compilation backend */
@ExperimentalProcessingApi
fun runKaptTest(
    sources: List<Source>,
    classpath: List<File> = emptyList(),
    options: Map<String, String> = emptyMap(),
    javacArguments: List<String> = emptyList(),
    kotlincArguments: List<String> = emptyList(),
    config: XProcessingEnvConfig = defaultTestConfig(options),
    handler: (XTestInvocation) -> Unit
) =
    runKaptTest(
        sources = sources,
        classpath = classpath,
        options = options,
        javacArguments = javacArguments,
        kotlincArguments = kotlincArguments,
        config = config,
        handlers = listOf(handler)
    )

/** @see runKaptTest */
@ExperimentalProcessingApi
fun runKaptTest(
    sources: List<Source>,
    classpath: List<File> = emptyList(),
    options: Map<String, String> = emptyMap(),
    javacArguments: List<String> = emptyList(),
    kotlincArguments: List<String> = emptyList(),
    config: XProcessingEnvConfig = defaultTestConfig(options),
    handlers: List<(XTestInvocation) -> Unit>
) {
    runTests(
        params =
            TestCompilationParameters(
                sources = sources,
                classpath = classpath,
                options = options,
                handlers = handlers,
                javacArguments = javacArguments,
                kotlincArguments = kotlincArguments,
                config = config,
            ),
        KaptCompilationTestRunner()
    )
}

/** Runs the test only with ksp compilation backend */
@ExperimentalProcessingApi
fun runKspTest(
    sources: List<Source>,
    classpath: List<File> = emptyList(),
    options: Map<String, String> = emptyMap(),
    javacArguments: List<String> = emptyList(),
    kotlincArguments: List<String> = emptyList(),
    config: XProcessingEnvConfig = defaultTestConfig(options),
    handler: (XTestInvocation) -> Unit
) =
    runKspTest(
        sources = sources,
        classpath = classpath,
        options = options,
        javacArguments = javacArguments,
        kotlincArguments = kotlincArguments,
        config = config,
        handlers = listOf(handler)
    )

/** @see runKspTest */
@ExperimentalProcessingApi
fun runKspTest(
    sources: List<Source>,
    classpath: List<File> = emptyList(),
    options: Map<String, String> = emptyMap(),
    javacArguments: List<String> = emptyList(),
    kotlincArguments: List<String> = emptyList(),
    config: XProcessingEnvConfig = defaultTestConfig(options),
    handlers: List<(XTestInvocation) -> Unit>
) {
    runTests(
        params =
            TestCompilationParameters(
                sources = sources,
                classpath = classpath,
                options = options,
                handlers = handlers,
                javacArguments = javacArguments,
                kotlincArguments = kotlincArguments,
                config = config,
            ),
        KspCompilationTestRunner()
    )
}

/**
 * Compiles the given set of sources into a temporary folder and returns the full classpath that
 * includes both the compilation output and dependencies.
 *
 * @param sources The list of source files to compile
 * @param options The annotation processor arguments
 * @param annotationProcessors The list of Java annotation processors to run with compilation
 * @param symbolProcessorProviders The list of Kotlin symbol processor providers to run with
 *   compilation
 * @param javacArguments The command line arguments that will be passed into javac
 * @param kotlincArguments The command line arguments that will be passed into kotlinc
 */
fun compileFiles(
    sources: List<Source>,
    options: Map<String, String> = emptyMap(),
    annotationProcessors: List<Processor> = emptyList(),
    symbolProcessorProviders: List<SymbolProcessorProvider> = emptyList(),
    javacArguments: List<String> = emptyList(),
    kotlincArguments: List<String> = emptyList(),
    includeSystemClasspath: Boolean = true
): List<File> {
    val workingDir = Files.createTempDir()
    val result =
        compile(
            workingDir = workingDir,
            arguments =
                TestCompilationArguments(
                    sources = sources,
                    kaptProcessors = annotationProcessors,
                    symbolProcessorProviders = symbolProcessorProviders,
                    processorOptions = options,
                    javacArguments = javacArguments,
                    kotlincArguments = kotlincArguments
                )
        )
    if (!result.success) {
        throw AssertionError(
            """
            Compilation failed:
            $result
            """
                .trimIndent()
        )
    }

    return result.outputClasspath.let {
        if (includeSystemClasspath) {
            it + getSystemClasspathFiles()
        } else {
            it
        }
    }
}

/**
 * Compiles the given set of sources into a jar located in the output directory and returns the jar
 * file.
 *
 * @param outputDirectory The directory where the jar will be created in.
 * @param sources The list of source files to compile
 * @param options The annotation processor arguments
 * @param annotationProcessors The list of Java annotation processors to run with compilation
 * @param symbolProcessorProviders The list of Kotlin symbol processor providers to run with
 *   compilation
 * @param javacArguments The command line arguments that will be passed into javac
 */
fun compileFilesIntoJar(
    outputDirectory: File,
    sources: List<Source>,
    options: Map<String, String> = emptyMap(),
    annotationProcessors: List<Processor> = emptyList(),
    symbolProcessorProviders: List<SymbolProcessorProvider> = emptyList(),
    javacArguments: List<String> = emptyList(),
): File {
    val compiledFiles =
        compileFiles(
            sources = sources,
            options = options,
            annotationProcessors = annotationProcessors,
            symbolProcessorProviders = symbolProcessorProviders,
            javacArguments = javacArguments,
            includeSystemClasspath = false,
        )
    val outputFile = File.createTempFile("compiled_", ".jar", outputDirectory)
    createJar(compiledFiles, outputFile)
    return outputFile
}

/**
 * Creates a jar with the content of the inputs. If an input is a file, it is placed a the root of
 * the jar, if it is a directory, then the contents of the directory is individually placed at the
 * root of the jar. Duplicate files are not allowed.
 */
private fun createJar(inputs: List<File>, outputFile: File) {
    JarOutputStream(outputFile.outputStream()).use {
        inputs.forEach { input ->
            addJarEntry(input, if (input.isFile) input.parent else input.absolutePath, it)
        }
    }
}

private fun addJarEntry(source: File, changeDir: String, target: JarOutputStream) {
    if (source.isDirectory) {
        var name = source.path.replace("\\", "/")
        if (name.isNotEmpty()) {
            if (!name.endsWith("/")) {
                name += "/"
            }
            val entry = JarEntry(name.substring(changeDir.length + 1))
            entry.time = source.lastModified()
            if (entry.name.isNotEmpty()) {
                target.putNextEntry(entry)
                target.closeEntry()
            }
        }
        source.listFiles()!!.forEach { nestedFile -> addJarEntry(nestedFile, changeDir, target) }
    } else if (source.isFile) {
        val entry = JarEntry(source.path.replace("\\", "/").substring(changeDir.length + 1))
        entry.time = source.lastModified()
        target.putNextEntry(entry)
        source.inputStream().use { inputStream -> inputStream.copyTo(target) }
        target.closeEntry()
    }
}

/**
 * Runs a block in a temporary directory and cleans it up afterwards.
 *
 * This method intentionally returns Unit to make it harder to return something that might reference
 * the temporary directory.
 */
private inline fun withTempDir(block: (tmpDir: File) -> Unit) {
    val tmpDir = Files.createTempDir()
    try {
        return block(tmpDir)
    } finally {
        tmpDir.deleteRecursively()
    }
}

/** Kotlin compiler arguments for K1 */
val KOTLINC_LANGUAGE_1_9_ARGS = listOf("-language-version=1.9", "-api-version=1.9")
