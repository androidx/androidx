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
import androidx.room.compiler.processing.util.compiler.TestKapt3Registrar
import androidx.room.compiler.processing.util.compiler.toSourceSet
import org.jetbrains.kotlin.base.kapt3.AptMode
import org.jetbrains.kotlin.base.kapt3.KaptFlag
import org.jetbrains.kotlin.base.kapt3.KaptOptions
import org.jetbrains.kotlin.cli.common.ExitCode
import java.io.File
import javax.annotation.processing.Processor

/**
 * Runs KAPT to run annotation processors.
 */
internal class KaptCompilationStep(
    private val annotationProcessors: List<Processor>,
    private val processorOptions: Map<String, String>,
) : KotlinCompilationStep {
    override val name = "kapt"
    private fun createKaptArgs(
        workingDir: File,
    ): KaptOptions.Builder {
        return KaptOptions.Builder().also {
            it.stubsOutputDir = workingDir.resolve("kapt-stubs") // IGNORED
            it.sourcesOutputDir = workingDir.resolve(JAVA_SRC_OUT_FOLDER_NAME)
            // Compiled classes don't end up here but generated resources do.
            it.classesOutputDir = workingDir.resolve(RESOURCES_OUT_FOLDER_NAME)
            it.projectBaseDir = workingDir
            it.processingOptions["kapt.kotlin.generated"] =
                workingDir.resolve(KOTLIN_SRC_OUT_FOLDER_NAME)
                    .also {
                        it.mkdirs()
                    }
                    .canonicalPath
            it.processingOptions.putAll(processorOptions)
            it.mode = AptMode.STUBS_AND_APT
            it.processors.addAll(annotationProcessors.map { it::class.java.name })
            // NOTE: this does not work very well until the following bug is fixed
            //  https://youtrack.jetbrains.com/issue/KT-47934
            it.flags.add(KaptFlag.MAP_DIAGNOSTIC_LOCATIONS)
        }
    }

    override fun execute(
        workingDir: File,
        arguments: CompilationStepArguments
    ): CompilationStepResult {
        if (annotationProcessors.isEmpty()) {
            return CompilationStepResult.skip(arguments)
        }
        val kaptMessages = DiagnosticsMessageCollector()
        val result = KotlinCliRunner.runKotlinCli(
            arguments = arguments, // output is ignored,
            destinationDir = workingDir.resolve(CLASS_OUT_FOLDER_NAME),
            pluginRegistrars = listOf(
                TestKapt3Registrar(
                    processors = annotationProcessors,
                    baseOptions = createKaptArgs(workingDir),
                    messageCollector = kaptMessages
                )
            )
        )
        val generatedSources = listOfNotNull(
            workingDir.resolve(JAVA_SRC_OUT_FOLDER_NAME).toSourceSet(),
            workingDir.resolve(KOTLIN_SRC_OUT_FOLDER_NAME).toSourceSet()
        )

        val diagnostics = resolveDiagnostics(
            diagnostics = result.diagnostics + kaptMessages.getDiagnostics(),
            sourceSets = arguments.sourceSets + generatedSources
        )
        val outputClasspath =
            listOf(result.compiledClasspath) + workingDir.resolve(RESOURCES_OUT_FOLDER_NAME)
        return CompilationStepResult(
            success = result.exitCode == ExitCode.OK,
            generatedSourceRoots = generatedSources,
            diagnostics = diagnostics,
            nextCompilerArguments = arguments.copy(
                sourceSets = arguments.sourceSets + generatedSources
            ),
            outputClasspath = outputClasspath
        )
    }

    companion object {
        private const val JAVA_SRC_OUT_FOLDER_NAME = "kapt-java-src-out"
        private const val KOTLIN_SRC_OUT_FOLDER_NAME = "kapt-kotlin-src-out"
        private const val RESOURCES_OUT_FOLDER_NAME = "kapt-classes-out"
        private const val CLASS_OUT_FOLDER_NAME = "class-out"
    }
}