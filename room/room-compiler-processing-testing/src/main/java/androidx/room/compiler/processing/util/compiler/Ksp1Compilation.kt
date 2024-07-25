/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.room.compiler.processing.util.compiler

import androidx.room.compiler.processing.util.FileResource
import androidx.room.compiler.processing.util.compiler.steps.CompilationStepArguments
import androidx.room.compiler.processing.util.compiler.steps.CompilationStepResult
import androidx.room.compiler.processing.util.compiler.steps.resolveDiagnostics
import com.google.devtools.ksp.KspOptions
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import java.io.File
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

internal class Ksp1Compilation(
    private val name: String,
    private val symbolProcessorProviders: List<SymbolProcessorProvider>,
    private val processorOptions: Map<String, String>
) {
    private fun createKspOptions(workingDir: File): KspOptions.Builder {
        return KspOptions.Builder().apply {
            this.javaOutputDir = workingDir.resolve(JAVA_OUT_DIR)
            this.kotlinOutputDir = workingDir.resolve(KOTLIN_OUT_DIR)
            this.processingOptions.putAll(processorOptions)
        }
    }

    @OptIn(ExperimentalCompilerApi::class)
    fun execute(workingDir: File, arguments: CompilationStepArguments): CompilationStepResult {
        if (symbolProcessorProviders.isEmpty()) {
            return CompilationStepResult.skip(arguments)
        }
        val kspMessages = DiagnosticsMessageCollector(name)
        val result =
            KotlinCliRunner.runKotlinCli(
                arguments = arguments,
                destinationDir = workingDir.resolve(CLASS_OUT_FOLDER_NAME),
                pluginRegistrars =
                    PluginRegistrarArguments(
                        listOf(
                            TestKspRegistrar(
                                kspWorkingDir = workingDir.resolve("ksp-compiler"),
                                baseOptions = createKspOptions(workingDir),
                                processorProviders = symbolProcessorProviders,
                                messageCollector = kspMessages
                            )
                        ),
                        emptyList()
                    ),
            )
        // workaround for https://github.com/google/ksp/issues/623
        val failureDueToWarnings =
            result.kotlinCliArguments.allWarningsAsErrors && kspMessages.hasWarnings()

        val generatedSources =
            listOfNotNull(
                workingDir.resolve(KOTLIN_OUT_DIR).toSourceSet(),
                workingDir.resolve(JAVA_OUT_DIR).toSourceSet(),
            )
        val diagnostics =
            resolveDiagnostics(
                diagnostics = result.diagnostics + kspMessages.getDiagnostics(),
                sourceSets = arguments.sourceSets + generatedSources
            )
        val outputResources = workingDir.resolve(RESOURCES_OUT_FOLDER_NAME)
        val outputClasspath = listOf(result.compiledClasspath) + outputResources
        val generatedResources =
            outputResources
                .walkTopDown()
                .filter { it.isFile }
                .map { FileResource(it.relativeTo(outputResources).path, it) }
                .toList()
        return CompilationStepResult(
            success = result.exitCode == ExitCode.OK && !failureDueToWarnings,
            generatedSourceRoots = generatedSources,
            diagnostics = diagnostics,
            nextCompilerArguments =
                arguments.copy(sourceSets = arguments.sourceSets + generatedSources),
            outputClasspath = outputClasspath,
            generatedResources = generatedResources
        )
    }

    companion object {
        private const val JAVA_OUT_DIR = "generatedJava"
        private const val KOTLIN_OUT_DIR = "generatedKotlin"
        private const val CLASS_OUT_FOLDER_NAME = "class-out"
        private const val RESOURCES_OUT_FOLDER_NAME = "ksp-compiler/resourceOutputDir"
    }
}
