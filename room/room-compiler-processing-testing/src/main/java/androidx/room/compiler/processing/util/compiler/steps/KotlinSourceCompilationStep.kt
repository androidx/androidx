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

import androidx.room.compiler.processing.util.compiler.KotlinCliRunner
import java.io.File
import org.jetbrains.kotlin.cli.common.ExitCode

/**
 * Compiles Kotlin sources.
 *
 * Note that annotation / symbol processors are not run by this step.
 */
internal object KotlinSourceCompilationStep : KotlinCompilationStep {
    override val name = "kotlinSourceCompilation"

    override fun execute(
        workingDir: File,
        arguments: CompilationStepArguments
    ): CompilationStepResult {
        if (arguments.sourceSets.none { it.hasKotlinSource }) {
            return CompilationStepResult.skip(arguments)
        }
        val result =
            KotlinCliRunner.runKotlinCli(
                arguments = arguments,
                destinationDir = workingDir.resolve(CLASS_OUT_FOLDER_NAME),
            )
        val diagnostics =
            resolveDiagnostics(diagnostics = result.diagnostics, sourceSets = arguments.sourceSets)
        return CompilationStepResult(
            success = result.exitCode == ExitCode.OK,
            generatedSourceRoots = emptyList(),
            diagnostics = diagnostics,
            nextCompilerArguments =
                arguments.copy(
                    additionalClasspaths =
                        listOf(workingDir.resolve(CLASS_OUT_FOLDER_NAME)) +
                            arguments.additionalClasspaths,
                    // NOTE: ideally, we should remove kotlin sources but we know that there are no
                    // more
                    // kotlin steps so we skip unnecessary work
                    sourceSets = arguments.sourceSets
                ),
            outputClasspath = listOf(result.compiledClasspath),
            generatedResources = emptyList()
        )
    }

    private const val CLASS_OUT_FOLDER_NAME = "class-out"
}
