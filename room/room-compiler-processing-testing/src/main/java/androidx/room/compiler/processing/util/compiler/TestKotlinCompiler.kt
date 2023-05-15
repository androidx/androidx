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

package androidx.room.compiler.processing.util.compiler

import androidx.room.compiler.processing.util.DiagnosticMessage
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.compiler.steps.CompilationStepArguments
import androidx.room.compiler.processing.util.compiler.steps.CompilationStepResult
import androidx.room.compiler.processing.util.compiler.steps.JavaSourceCompilationStep
import androidx.room.compiler.processing.util.compiler.steps.KaptCompilationStep
import androidx.room.compiler.processing.util.compiler.steps.KotlinSourceCompilationStep
import androidx.room.compiler.processing.util.compiler.steps.KspCompilationStep
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import java.io.File
import javax.annotation.processing.Processor
import javax.tools.Diagnostic

/**
 * Compilation runner for kotlin using kotlin CLI tool
 */
data class TestCompilationArguments(
    /**
     * List of source files for the compilation
     */
    val sources: List<Source>,
    /**
     * Additional classpath for the compilation
     */
    val classpath: List<File> = emptyList(),
    /**
     * If `true` (default), the classpath of the current process will be included in the
     * classpath list.
     */
    val inheritClasspath: Boolean = true,
    /**
     * Arguments for the java compiler. This will be used when both running KAPT and also java
     * compiler.
     */
    val javacArguments: List<String> = emptyList(),
    /**
     * Arguments for the kotlin compiler. This will be used when both running KAPT and also KSP.
     */
    val kotlincArguments: List<String> = emptyList(),
    /**
     * List of annotation processors to be run by KAPT.
     */
    val kaptProcessors: List<Processor> = emptyList(),
    /**
     * List of symbol processor providers to be run by KSP.
     */
    val symbolProcessorProviders: List<SymbolProcessorProvider> = emptyList(),
    /**
     * Map of annotation/symbol processor options. Used for both KAPT and KSP.
     */
    val processorOptions: Map<String, String> = emptyMap()
)

/**
 * Result of a test compilation.
 */
data class TestCompilationResult(
    /**
     * true if the compilation succeeded, false otherwise.
     */
    val success: Boolean,
    /**
     * List of generated source files by the compilation.
     */
    val generatedSources: List<Source>,
    /**
     * Diagnostic messages that were reported during compilation.
     */
    val diagnostics: Map<Diagnostic.Kind, List<DiagnosticMessage>>,
    /**
     * List of classpath folders that contain the produced .class files.
     */
    val outputClasspath: List<File>
)

/**
 * Ensures the list of sources has at least 1 kotlin file, if not, adds one.
 */
internal fun TestCompilationArguments.withAtLeastOneKotlinSource(): TestCompilationArguments {
    val hasKotlinSource = sources.any {
        it is Source.KotlinSource
    }
    if (hasKotlinSource) return this
    return copy(
        sources = sources + Source.kotlin(
            "SyntheticSource.kt",
            code = """
                package xprocessing.generated
                class SyntheticKotlinSource
            """.trimIndent()
        )
    )
}

/**
 * Copies the [Source] file into the given root directories based on file type.
 */
private fun Source.copyTo(
    kotlinRootDir: File,
    javaRootDir: File
): File {
    val locationRoot = when (this) {
        is Source.KotlinSource -> kotlinRootDir
        is Source.JavaSource -> javaRootDir
    }
    val location = locationRoot.resolve(relativePath)
    check(!location.exists()) {
        "duplicate source file: $location ($this)"
    }
    location.parentFile.mkdirs()
    location.writeText(contents, Charsets.UTF_8)
    return location
}

/**
 * Converts [TestCompilationArguments] into the internal [CompilationStepArguments] type.
 *
 * This involves copying sources into the working directory.
 */
private fun TestCompilationArguments.toInternal(
    workingDir: File
): CompilationStepArguments {
    val (kotlinRoot, javaRoot) = workingDir.resolve("src").let {
        it.resolve("kotlin") to it.resolve("java")
    }
    // copy sources based on type.
    sources.map {
        it.copyTo(kotlinRootDir = kotlinRoot, javaRootDir = javaRoot)
    }
    return CompilationStepArguments(
        sourceSets = listOfNotNull(
            javaRoot.toSourceSet(),
            kotlinRoot.toSourceSet()
        ),
        additionalClasspaths = classpath,
        inheritClasspaths = inheritClasspath,
        javacArguments = javacArguments,
        kotlincArguments = kotlincArguments
    )
}

/**
 * Executes a build for the given [TestCompilationArguments].
 */
fun compile(
    /**
     * The temporary directory to use during compilation
     */
    workingDir: File,
    /**
     * The compilation arguments
     */
    arguments: TestCompilationArguments,
): TestCompilationResult {
    val steps = listOf(
        KaptCompilationStep(arguments.kaptProcessors, arguments.processorOptions),
        KspCompilationStep(arguments.symbolProcessorProviders, arguments.processorOptions),
        KotlinSourceCompilationStep,
        JavaSourceCompilationStep
    )
    workingDir.ensureEmptyDirectory()

    val initialArgs = arguments.toInternal(workingDir.resolve("input"))
    val initial = listOf(
        CompilationStepResult(
            success = true,
            generatedSourceRoots = emptyList(),
            diagnostics = emptyList(),
            nextCompilerArguments = initialArgs,
            outputClasspath = emptyList()
        )
    )
    val resultFromEachStep = steps.fold(initial) { prevResults, step ->
        val prev = prevResults.last()
        if (prev.success) {
            prevResults + step.execute(
                workingDir = workingDir.resolve(step.name),
                arguments = prev.nextCompilerArguments
            )
        } else {
            prevResults
        }
    }
    val combinedDiagnostics = mutableMapOf<Diagnostic.Kind, MutableList<DiagnosticMessage>>()
    resultFromEachStep.forEach { result ->
        result.diagnostics.forEach { diagnostic ->
            combinedDiagnostics.getOrPut(
                diagnostic.kind
            ) {
                mutableListOf()
            }.add(diagnostic)
        }
    }
    return TestCompilationResult(
        success = resultFromEachStep.all { it.success },
        generatedSources = resultFromEachStep.flatMap { it.generatedSources },
        diagnostics = combinedDiagnostics,
        outputClasspath = resultFromEachStep.flatMap { it.outputClasspath }
    )
}

internal fun File.ensureEmptyDirectory() {
    if (exists()) {
        check(isDirectory) {
            "$this cannot be a file"
        }
        val existingFiles = listFiles()
        check(existingFiles == null || existingFiles.isEmpty()) {
            "$this must be empty, found: ${existingFiles?.joinToString("\n")}"
        }
    } else {
        check(this.mkdirs()) {
            "failed to create working directory ($this)"
        }
    }
}