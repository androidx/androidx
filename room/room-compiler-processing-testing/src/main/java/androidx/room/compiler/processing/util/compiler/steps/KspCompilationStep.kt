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

package androidx.room.compiler.processing.util.compiler.steps

import androidx.room.compiler.processing.util.compiler.DiagnosticsMessageCollector
import androidx.room.compiler.processing.util.compiler.KotlinCliRunner
import androidx.room.compiler.processing.util.compiler.TestKspRegistrar
import androidx.room.compiler.processing.util.compiler.toSourceSet
import com.google.devtools.ksp.KspOptions
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import java.io.File
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

/**
 * Runs the Symbol Processors
 */
internal class KspCompilationStep(
    private val symbolProcessorProviders: List<SymbolProcessorProvider>,
    private val processorOptions: Map<String, String>
) : KotlinCompilationStep {
    override val name: String = "ksp"

    private fun createKspOptions(
        workingDir: File
    ): KspOptions.Builder {
        return KspOptions.Builder().apply {
            this.javaOutputDir = workingDir.resolve(JAVA_OUT_DIR)
            this.kotlinOutputDir = workingDir.resolve(KOTLIN_OUT_DIR)
            this.processingOptions.putAll(processorOptions)
        }
    }

    @OptIn(ExperimentalCompilerApi::class)
    override fun execute(
        workingDir: File,
        arguments: CompilationStepArguments
    ): CompilationStepResult {
        if (symbolProcessorProviders.isEmpty()) {
            return CompilationStepResult.skip(arguments)
        }
        val kspMessages = DiagnosticsMessageCollector(name)
        val result = KotlinCliRunner.runKotlinCli(
            arguments = arguments,
            destinationDir = workingDir.resolve(CLASS_OUT_FOLDER_NAME),
            pluginRegistrars = listOf(
                TestKspRegistrar(
                    kspWorkingDir = workingDir.resolve("ksp-compiler"),
                    baseOptions = createKspOptions(workingDir),
                    processorProviders = symbolProcessorProviders,
                    messageCollector = kspMessages
                )
            ),
        )
        // workaround for https://github.com/google/ksp/issues/623
        val failureDueToWarnings = result.kotlinCliArguments.allWarningsAsErrors &&
            kspMessages.hasWarnings()

        val generatedSources = listOfNotNull(
            workingDir.resolve(KOTLIN_OUT_DIR).toSourceSet(),
            workingDir.resolve(JAVA_OUT_DIR).toSourceSet(),
        )
        val diagnostics = resolveDiagnostics(
            diagnostics = result.diagnostics + kspMessages.getDiagnostics(),
            sourceSets = arguments.sourceSets + generatedSources
        )
        return CompilationStepResult(
            success = result.exitCode == ExitCode.OK && !failureDueToWarnings,
            generatedSourceRoots = generatedSources,
            diagnostics = diagnostics,
            nextCompilerArguments = arguments.copy(
                sourceSets = arguments.sourceSets + generatedSources
            ),
            outputClasspath = listOf(result.compiledClasspath)
        )
    }

    companion object {
        private const val JAVA_OUT_DIR = "generatedJava"
        private const val KOTLIN_OUT_DIR = "generatedKotlin"
        private const val CLASS_OUT_FOLDER_NAME = "class-out"
    }
}