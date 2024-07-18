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
import androidx.room.compiler.processing.SyntheticKspProcessor
import androidx.room.compiler.processing.util.CompilationResult
import androidx.room.compiler.processing.util.KotlinCompilationResult
import androidx.room.compiler.processing.util.compiler.TestCompilationArguments
import androidx.room.compiler.processing.util.compiler.compile
import androidx.room.compiler.processing.util.compiler.withAtLeastOneKotlinSource
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import java.io.File

@ExperimentalProcessingApi
internal class KspCompilationTestRunner(
    private val testProcessorProviders: List<SymbolProcessorProvider> = emptyList()
) : CompilationTestRunner {
    override val name: String = "ksp"

    override fun canRun(params: TestCompilationParameters): Boolean {
        return true
    }

    override fun compile(workingDir: File, params: TestCompilationParameters): CompilationResult {
        val processorProvider = object : SymbolProcessorProvider {
            lateinit var processor: SyntheticKspProcessor

            override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
                return SyntheticKspProcessor(
                    symbolProcessorEnvironment = environment,
                    handlers = params.handlers,
                    config = params.config
                ).also { processor = it }
            }
        }
        val args = TestCompilationArguments(
            sources = params.sources,
            classpath = params.classpath,
            symbolProcessorProviders = testProcessorProviders + processorProvider,
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
            processor = processorProvider.processor,
            delegate = result
        )
    }
}
