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

package androidx.room.compiler.processing.util.runner

import androidx.room.compiler.processing.ExperimentalProcessingApi
import androidx.room.compiler.processing.SyntheticJavacProcessor
import androidx.room.compiler.processing.util.CompilationResult
import androidx.room.compiler.processing.util.KotlinCompilationResult
import androidx.room.compiler.processing.util.compiler.TestCompilationArguments
import androidx.room.compiler.processing.util.compiler.compile
import androidx.room.compiler.processing.util.compiler.withAtLeastOneKotlinSource
import java.io.File

@ExperimentalProcessingApi
internal object KaptCompilationTestRunner : CompilationTestRunner {
    override val name: String = "kapt"

    override fun canRun(params: TestCompilationParameters): Boolean {
        return true
    }

    override fun compile(workingDir: File, params: TestCompilationParameters): CompilationResult {
        val syntheticJavacProcessor = SyntheticJavacProcessor(params.handlers)
        val args = TestCompilationArguments(
            sources = params.sources,
            classpath = params.classpath,
            kaptProcessors = listOf(syntheticJavacProcessor),
            processorOptions = params.options,
            javacArguments = params.javacArguments,
            kotlincArguments = params.kotlincArguments,
        ).withAtLeastOneKotlinSource()
        val result = compile(
            workingDir = workingDir,
            arguments = args
        )
        return KotlinCompilationResult(
            testRunner = this,
            processor = syntheticJavacProcessor,
            delegate = result
        )
    }
}