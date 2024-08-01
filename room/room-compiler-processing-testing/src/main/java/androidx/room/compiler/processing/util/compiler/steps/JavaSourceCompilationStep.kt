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

import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.getSystemClasspathFiles
import androidx.room.compiler.processing.util.toDiagnosticMessages
import com.google.testing.compile.Compilation
import com.google.testing.compile.Compiler
import java.io.File
import javax.tools.JavaFileObject

/**
 * Compiles Java sources.
 *
 * Note that this does not run Java annotation processors. They are run in the KAPT step for
 * consistency. When a test is run with purely Java sources, it uses google-compile-testing library
 * directly instead of the Kotlin compilation pipeline.
 */
internal object JavaSourceCompilationStep : KotlinCompilationStep {
    override val name = "javaSourceCompilation"

    override fun execute(
        workingDir: File,
        arguments: CompilationStepArguments
    ): CompilationStepResult {
        val javaSources: Map<JavaFileObject, Source> =
            arguments.sourceSets.asSequence().flatMap { it.javaSources }.associateBy { it.toJFO() }
        if (javaSources.isEmpty()) {
            return CompilationStepResult.skip(arguments)
        }
        val classpaths =
            if (arguments.inheritClasspaths) {
                    arguments.additionalClasspaths + getSystemClasspathFiles()
                } else {
                    arguments.additionalClasspaths
                }
                .filter { it.exists() }

        val compiler =
            Compiler.javac()
                .withOptions(arguments.javacArguments + "-Xlint")
                .withClasspath(classpaths)

        val result = compiler.compile(javaSources.keys)

        val generatedClasses =
            if (result.status() == Compilation.Status.SUCCESS) {
                val classpathOut = workingDir.resolve(GEN_CLASS_OUT)
                result.generatedFiles().map {
                    val targetFile =
                        classpathOut
                            .resolve(it.toUri().path.substringAfter("CLASS_OUTPUT/"))
                            .also { file -> file.parentFile.mkdirs() }
                    it.openInputStream().use { inputStream ->
                        targetFile.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                }
                listOf(classpathOut)
            } else {
                emptyList()
            }

        return CompilationStepResult(
            success = result.status() == Compilation.Status.SUCCESS,
            generatedSourceRoots = emptyList(),
            diagnostics = result.diagnostics().toDiagnosticMessages(javaSources),
            nextCompilerArguments =
                arguments.copy(
                    // NOTE: ideally, we should remove java sources but we know that there are no
                    // next
                    // steps so we skip unnecessary work
                    sourceSets = arguments.sourceSets
                ),
            outputClasspath = generatedClasses,
            generatedResources = emptyList()
        )
    }

    private const val GEN_CLASS_OUT = "classOut"
}
