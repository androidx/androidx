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

import androidx.room.compiler.processing.SyntheticJavacProcessor
import androidx.room.compiler.processing.SyntheticKspProcessor
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaSourcesSubjectFactory
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.symbolProcessors
import java.io.File

// TODO get rid of these once kotlin compile testing supports two step compilation for KSP.
//  https://github.com/tschuchortdev/kotlin-compile-testing/issues/72
private val KotlinCompilation.kspJavaSourceDir: File
    get() = kspSourcesDir.resolve("java")

private val KotlinCompilation.kspKotlinSourceDir: File
    get() = kspSourcesDir.resolve("kotlin")

private fun compileSources(
    sources: List<Source>,
    classpath: List<File>,
    handler: (XTestInvocation) -> Unit
): Pair<SyntheticJavacProcessor, CompileTester> {
    val syntheticJavacProcessor = SyntheticJavacProcessor(handler)
    return syntheticJavacProcessor to Truth.assertAbout(
        JavaSourcesSubjectFactory.javaSources()
    ).that(
        sources.map {
            it.toJFO()
        }
    ).apply {
        if (classpath.isNotEmpty()) {
            withClasspath(classpath)
        }
    }.processedWith(
        syntheticJavacProcessor
    )
}

private fun compileWithKapt(
    sources: List<Source>,
    classpath: List<File>,
    handler: (XTestInvocation) -> Unit
): Pair<SyntheticJavacProcessor, KotlinCompilation> {
    val syntheticJavacProcessor = SyntheticJavacProcessor(handler)
    val compilation = KotlinCompilation()
    sources.forEach {
        compilation.workingDir.resolve("sources")
            .resolve(it.relativePath())
            .parentFile
            .mkdirs()
    }
    compilation.sources = sources.map {
        it.toKotlinSourceFile()
    }
    compilation.annotationProcessors = listOf(syntheticJavacProcessor)
    compilation.inheritClassPath = true
    compilation.verbose = false
    compilation.classpaths += classpath

    return syntheticJavacProcessor to compilation
}

private fun compileWithKsp(
    sources: List<Source>,
    classpath: List<File>,
    handler: (XTestInvocation) -> Unit
): Pair<SyntheticKspProcessor, KotlinCompilation.Result> {
    @Suppress("NAME_SHADOWING")
    val sources = if (sources.none { it is Source.KotlinSource }) {
        // looks like this requires a kotlin source file
        // see: https://github.com/tschuchortdev/kotlin-compile-testing/issues/57
        sources + Source.kotlin("placeholder.kt", "")
    } else {
        sources
    }
    val syntheticKspProcessor = SyntheticKspProcessor(handler)
    fun prepareCompilation(): KotlinCompilation {
        val compilation = KotlinCompilation()
        sources.forEach {
            compilation.workingDir.resolve("sources")
                .resolve(it.relativePath())
                .parentFile
                .mkdirs()
        }
        compilation.sources = sources.map {
            it.toKotlinSourceFile()
        }
        compilation.jvmDefault = "enable"
        compilation.jvmTarget = "1.8"
        compilation.inheritClassPath = true
        compilation.verbose = false
        compilation.classpaths += classpath
        return compilation
    }

    val kspCompilation = prepareCompilation()
    kspCompilation.symbolProcessors = listOf(syntheticKspProcessor)
    kspCompilation.compile()
    // ignore KSP result for now because KSP stops compilation, which might create false negatives
    // when java code accesses kotlin code.
    // TODO:  fix once https://github.com/tschuchortdev/kotlin-compile-testing/issues/72 is fixed

    // after ksp, compile without ksp with KSP's output as input
    val finalCompilation = prepareCompilation()
    // build source files from generated code
    finalCompilation.sources += kspCompilation.kspJavaSourceDir.collectSourceFiles() +
        kspCompilation.kspKotlinSourceDir.collectSourceFiles()
    return syntheticKspProcessor to finalCompilation.compile()
}

private fun File.collectSourceFiles(): List<SourceFile> {
    return walkTopDown().filter {
        it.isFile
    }.map { file ->
        SourceFile.fromPath(file)
    }.toList()
}

fun runProcessorTest(
    sources: List<Source> = emptyList(),
    classpath: List<File> = emptyList(),
    handler: (XTestInvocation) -> Unit
) {
    @Suppress("NAME_SHADOWING")
    val sources = if (sources.isEmpty()) {
        // synthesize a source to trigger compilation
        listOf(
            Source.java(
                "foo.bar.SyntheticSource",
                """
            package foo.bar;
            public class SyntheticSource {}
                """.trimIndent()
            )
        )
    } else {
        sources
    }
    // we can compile w/ javac only if all code is in java
    if (sources.canCompileWithJava()) {
        runJavaProcessorTest(
            sources = sources,
            classpath = classpath,
            handler = handler,
            succeed = true
        )
    }
    runKaptTest(
        sources = sources,
        classpath = classpath,
        handler = handler,
        succeed = true
    )
}

/**
 * This method is oddly named instead of being an overload on runProcessorTest to easily track
 * which tests started to support KSP.
 *
 * Eventually, it will be merged with runProcessorTest when all tests pass with KSP.
 */
fun runProcessorTestIncludingKsp(
    sources: List<Source> = emptyList(),
    classpath: List<File> = emptyList(),
    handler: (XTestInvocation) -> Unit
) {
    runProcessorTest(
        sources = sources,
        classpath = classpath,
        handler = handler
    )
    runKspTest(
        sources = sources,
        classpath = classpath,
        succeed = true,
        handler = handler
    )
}

fun runProcessorTestForFailedCompilation(
    sources: List<Source>,
    classpath: List<File> = emptyList(),
    handler: (XTestInvocation) -> Unit
) {
    if (sources.canCompileWithJava()) {
        // run with java processor
        runJavaProcessorTest(
            sources = sources,
            classpath = classpath,
            handler = handler,
            succeed = false
        )
    }
    // now run with kapt
    runKaptTest(
        sources = sources,
        classpath = classpath,
        handler = handler,
        succeed = false
    )
}

fun runProcessorTestForFailedCompilationIncludingKsp(
    sources: List<Source>,
    classpath: List<File>,
    handler: (XTestInvocation) -> Unit
) {
    runProcessorTestForFailedCompilation(
        sources = sources,
        classpath = classpath,
        handler = handler
    )
    // now run with ksp
    runKspTest(
        sources = sources,
        classpath = classpath,
        handler = handler,
        succeed = false
    )
}

fun runJavaProcessorTest(
    sources: List<Source>,
    classpath: List<File>,
    succeed: Boolean,
    handler: (XTestInvocation) -> Unit
) {
    val (syntheticJavacProcessor, compileTester) = compileSources(sources, classpath, handler)
    if (succeed) {
        compileTester.compilesWithoutError()
    } else {
        compileTester.failsToCompile()
    }
    syntheticJavacProcessor.throwIfFailed()
}

fun runKaptTest(
    sources: List<Source>,
    classpath: List<File> = emptyList(),
    succeed: Boolean = true,
    handler: (XTestInvocation) -> Unit
) {
    // now run with kapt
    val (kaptProcessor, kotlinCompilation) = compileWithKapt(sources, classpath, handler)
    val compilationResult = kotlinCompilation.compile()
    if (succeed) {
        assertThat(compilationResult.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    } else {
        assertThat(compilationResult.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
    }
    kaptProcessor.throwIfFailed()
}

fun runKspTest(
    sources: List<Source>,
    classpath: List<File> = emptyList(),
    succeed: Boolean = true,
    handler: (XTestInvocation) -> Unit
) {
    val (kspProcessor, compilationResult) = compileWithKsp(sources, classpath, handler)
    if (succeed) {
        assertThat(compilationResult.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    } else {
        assertThat(compilationResult.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
    }
    kspProcessor.throwIfFailed()
}

/**
 * Compiles the given set of sources into a temporary folder and returns the output classes
 * directory.
 */
fun compileFiles(
    sources: List<Source>
): File {
    val compilation = KotlinCompilation()
    sources.forEach {
        compilation.workingDir.resolve("sources")
            .resolve(it.relativePath())
            .parentFile
            .mkdirs()
    }
    compilation.sources = sources.map {
        it.toKotlinSourceFile()
    }
    compilation.jvmDefault = "enable"
    compilation.jvmTarget = "1.8"
    compilation.inheritClassPath = true
    compilation.verbose = false
    val result = compilation.compile()
    check(result.exitCode == KotlinCompilation.ExitCode.OK) {
        "compilation failed: ${result.messages}"
    }
    return compilation.classesDir
}

private fun List<Source>.canCompileWithJava() = all { it is Source.JavaSource }
