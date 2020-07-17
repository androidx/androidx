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

package androidx.room.processing.util

import androidx.room.processing.SyntheticJavacProcessor
import com.google.common.truth.Truth
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaSourcesSubjectFactory
import com.tschuchort.compiletesting.KotlinCompilation

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

fun runProcessorTest(
    sources: List<Source> = emptyList(),
    handler: (TestInvocation) -> Unit
) {
    @Suppress("NAME_SHADOWING")
    val sources = if (sources.isEmpty()) {
        // synthesize a source to trigger compilation
        listOf(Source.java("foo.bar.SyntheticSource", """
            package foo.bar;
            public class SyntheticSource {}
        """.trimIndent()))
    } else {
        sources
    }
    // we can compile w/ javac only if all code is in java
    if (sources.all { it is Source.JavaSource }) {
        val (syntheticJavacProcessor, compileTester) = compileSources(sources, handler)
        compileTester.compilesWithoutError()
        syntheticJavacProcessor.throwIfFailed()
    }

    // now run with kapt
    val (kaptProcessor, kotlinCompilation) = compileWithKapt(sources, handler)
    val compilationResult = kotlinCompilation.compile()
    Truth.assertThat(compilationResult.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    kaptProcessor.throwIfFailed()
}

fun runProcessorTestForFailedCompilation(
    sources: List<Source>,
    handler: (TestInvocation) -> Unit
) {
    val (syntheticJavacProcessor, compileTester) = compileSources(sources, handler)
    compileTester.failsToCompile()
    syntheticJavacProcessor.throwIfFailed()

    // now run with kapt
    val (kaptProcessor, kotlinCompilation) = compileWithKapt(sources, handler)
    val compilationResult = kotlinCompilation.compile()
    Truth.assertThat(compilationResult.exitCode).isNotEqualTo(KotlinCompilation.ExitCode.OK)
    kaptProcessor.throwIfFailed()
}
