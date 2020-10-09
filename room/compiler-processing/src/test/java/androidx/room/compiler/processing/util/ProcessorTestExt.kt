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
import com.tschuchort.compiletesting.symbolProcessors

private fun compileSources(
    sources: List<Source>,
    handler: (TestInvocation) -> Unit
): Pair<SyntheticJavacProcessor, CompileTester> {
    val syntheticJavacProcessor = SyntheticJavacProcessor(handler)
    return syntheticJavacProcessor to Truth.assertAbout(
        JavaSourcesSubjectFactory.javaSources()
    ).that(
        sources.map {
            it.toJFO()
        }
    ).processedWith(
        syntheticJavacProcessor
    )
}

private fun compileWithKapt(
    sources: List<Source>,
    handler: (TestInvocation) -> Unit
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

    return syntheticJavacProcessor to compilation
}

private fun compileWithKsp(
    sources: List<Source>,
    handler: (TestInvocation) -> Unit
): Pair<SyntheticKspProcessor, KotlinCompilation> {
    @Suppress("NAME_SHADOWING")
    val sources = if (sources.none { it is Source.KotlinSource }) {
        // looks like this requires a kotlin source file
        // see: https://github.com/tschuchortdev/kotlin-compile-testing/issues/57
        sources + Source.kotlin("placeholder.kt", "")
    } else {
        sources
    }
    val syntheticKspProcessor = SyntheticKspProcessor(handler)
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
    compilation.symbolProcessors = listOf(syntheticKspProcessor)
    compilation.jvmDefault = "enable"
    compilation.jvmTarget = "1.8"
    compilation.inheritClassPath = true
    compilation.verbose = false

    return syntheticKspProcessor to compilation
}

fun runProcessorTest(
    sources: List<Source> = emptyList(),
    handler: (TestInvocation) -> Unit
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
    if (sources.all { it is Source.JavaSource }) {
        runJavaProcessorTest(sources = sources, handler = handler, succeed = true)
    }
    runKaptTest(sources = sources, handler = handler, succeed = true)
}

/**
 * This method is oddly named instead of being an overload on runProcessorTest to easily track
 * which tests started to support KSP.
 *
 * Eventually, it will be merged with runProcessorTest when all tests pass with KSP.
 */
fun runProcessorTestIncludingKsp(
    sources: List<Source> = emptyList(),
    handler: (TestInvocation) -> Unit
) {
    runProcessorTest(sources = sources, handler = handler)
    runKspTest(sources = sources, succeed = true, handler = handler)
}

fun runProcessorTestForFailedCompilation(
    sources: List<Source>,
    handler: (TestInvocation) -> Unit
) {
    // run with java processor
    runJavaProcessorTest(sources = sources, handler = handler, succeed = false)
    // now run with kapt
    runKaptTest(sources = sources, handler = handler, succeed = false)
}

fun runJavaProcessorTest(
    sources: List<Source>,
    succeed: Boolean,
    handler: (TestInvocation) -> Unit
) {
    val (syntheticJavacProcessor, compileTester) = compileSources(sources, handler)
    if (succeed) {
        compileTester.compilesWithoutError()
    } else {
        compileTester.failsToCompile()
    }
    syntheticJavacProcessor.throwIfFailed()
}

fun runKaptTest(
    sources: List<Source>,
    succeed: Boolean,
    handler: (TestInvocation) -> Unit
) {
    // now run with kapt
    val (kaptProcessor, kotlinCompilation) = compileWithKapt(sources, handler)
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
    succeed: Boolean,
    handler: (TestInvocation) -> Unit
) {
    val (kspProcessor, kotlinCompilation) = compileWithKsp(sources, handler)
    val compilationResult = kotlinCompilation.compile()
    if (succeed) {
        assertThat(compilationResult.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    } else {
        assertThat(compilationResult.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
    }
    kspProcessor.throwIfFailed()
}
