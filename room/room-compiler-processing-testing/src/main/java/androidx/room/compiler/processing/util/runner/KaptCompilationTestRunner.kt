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

package androidx.room.compiler.processing.util.runner

import androidx.room.compiler.processing.ExperimentalProcessingApi
import androidx.room.compiler.processing.SyntheticJavacProcessor
import androidx.room.compiler.processing.util.CompilationResult
import androidx.room.compiler.processing.util.KotlinCompilationUtil
import androidx.room.compiler.processing.util.KotlinCompileTestingCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import java.io.ByteArrayOutputStream

@ExperimentalProcessingApi
internal object KaptCompilationTestRunner : CompilationTestRunner {

    override val name: String = "kapt"

    override fun canRun(params: TestCompilationParameters): Boolean {
        return true
    }

    override fun compile(params: TestCompilationParameters): CompilationResult {
        val syntheticJavacProcessor = SyntheticJavacProcessor(params.handlers)
        val outputStream = ByteArrayOutputStream()
        val compilation = KotlinCompilationUtil.prepareCompilation(
            sources = params.sources,
            outputStream = outputStream,
            classpaths = params.classpath
        )
        compilation.kaptArgs.putAll(params.options)
        compilation.annotationProcessors = listOf(syntheticJavacProcessor)
        val result = compilation.compile()
        return KotlinCompileTestingCompilationResult(
            testRunner = this,
            delegate = result,
            processor = syntheticJavacProcessor,
            successfulCompilation = result.exitCode == KotlinCompilation.ExitCode.OK,
            outputSourceDirs = listOf(
                compilation.kaptSourceDir, compilation.kaptKotlinGeneratedDir
            ),
            rawOutput = outputStream.toString(Charsets.UTF_8),
        )
    }
}