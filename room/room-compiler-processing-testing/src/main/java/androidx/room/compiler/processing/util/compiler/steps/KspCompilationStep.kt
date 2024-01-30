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

import androidx.room.compiler.processing.util.FileResource
import androidx.room.compiler.processing.util.compiler.TestClasspath
import androidx.room.compiler.processing.util.compiler.TestDefaultOptions
import androidx.room.compiler.processing.util.compiler.existingRoots
import androidx.room.compiler.processing.util.compiler.toSourceSet
import com.google.devtools.ksp.impl.KotlinSymbolProcessing
import com.google.devtools.ksp.processing.KSPJvmConfig
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.NonExistLocation
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import javax.tools.Diagnostic

/** Runs KSP to run the Symbol Processors */
internal class KspCompilationStep(
    private val symbolProcessorProviders: List<SymbolProcessorProvider>,
    private val processorOptions: Map<String, String>
) : KotlinCompilationStep {
    override val name: String = "ksp"

    override fun execute(
        workingDir: File,
        arguments: CompilationStepArguments
    ): CompilationStepResult {
        if (symbolProcessorProviders.isEmpty()) {
            return CompilationStepResult.skip(arguments)
        }

        val kspConfig = createKspConfig(workingDir, arguments)
        val kspDiagnostics = DiagnosticsCollectorKspLogger()
        val exitCode =
            KotlinSymbolProcessing(
                    kspConfig = kspConfig,
                    symbolProcessorProviders = symbolProcessorProviders,
                    logger = kspDiagnostics
                )
                .execute()
        val generatedSources =
            listOfNotNull(
                workingDir.resolve(JAVA_SRC_OUT_FOLDER_NAME).toSourceSet(),
                workingDir.resolve(KOTLIN_SRC_OUT_FOLDER_NAME).toSourceSet(),
            )
        val diagnostics =
            resolveDiagnostics(
                diagnostics = kspDiagnostics.messages,
                sourceSets = arguments.sourceSets + generatedSources
            )
        val outputResources = workingDir.resolve(RESOURCES_OUT_FOLDER_NAME)
        val outputClasspath = listOf(workingDir.resolve(CLASS_OUT_FOLDER_NAME))
        val generatedResources =
            outputResources
                .walkTopDown()
                .filter { it.isFile }
                .map { FileResource(it.relativeTo(outputResources).path, it) }
                .toList()
        return CompilationStepResult(
            success = exitCode == KotlinSymbolProcessing.ExitCode.OK,
            generatedSourceRoots = generatedSources,
            diagnostics = diagnostics,
            nextCompilerArguments =
                arguments.copy(sourceSets = arguments.sourceSets + generatedSources),
            outputClasspath = outputClasspath,
            generatedResources = generatedResources
        )
    }

    private fun createKspConfig(workingDir: File, arguments: CompilationStepArguments) =
        KSPJvmConfig.Builder()
            .apply {
                projectBaseDir = workingDir

                sourceRoots =
                    arguments.sourceSets.filter { it.hasKotlinSource }.existingRoots().toList()
                javaSourceRoots =
                    arguments.sourceSets.filter { it.hasJavaSource }.existingRoots().toList()

                libraries = buildList {
                    if (arguments.inheritClasspaths) {
                        addAll(TestClasspath.inheritedClasspath)
                    }
                    addAll(arguments.additionalClasspaths)
                }
                jdkHome = File(System.getProperty("java.home"))

                outputBaseDir = workingDir
                javaOutputDir = workingDir.resolve(JAVA_SRC_OUT_FOLDER_NAME)
                kotlinOutputDir = workingDir.resolve(KOTLIN_SRC_OUT_FOLDER_NAME)
                resourceOutputDir = workingDir.resolve(RESOURCE_OUT_FOLDER_NAME)
                classOutputDir = workingDir.resolve(CLASS_OUT_FOLDER_NAME)

                cachesDir = workingDir.resolve(CACHE_FOLDER_NAME)

                moduleName = ""

                languageVersion = TestDefaultOptions.kotlinLanguageVersion.versionString
                apiVersion = TestDefaultOptions.kotlinApiVersion.versionString
                jvmTarget = TestDefaultOptions.jvmTarget.description
                jvmDefaultMode = TestDefaultOptions.jvmDefaultMode.description

                processorOptions = this@KspCompilationStep.processorOptions
            }
            .build()

    // We purposely avoid using MessageCollectorBasedKSPLogger to reduce our dependency on impls.
    private class DiagnosticsCollectorKspLogger : KSPLogger {

        val messages = mutableListOf<RawDiagnosticMessage>()

        override fun error(message: String, symbol: KSNode?) {
            messages.add(RawDiagnosticMessage(Diagnostic.Kind.ERROR, message, symbol.toLocation()))
        }

        override fun exception(e: Throwable) {
            val writer = StringWriter()
            e.printStackTrace(PrintWriter(writer))
            messages.add(RawDiagnosticMessage(Diagnostic.Kind.ERROR, writer.toString(), null))
        }

        override fun info(message: String, symbol: KSNode?) {
            messages.add(RawDiagnosticMessage(Diagnostic.Kind.NOTE, message, symbol.toLocation()))
        }

        override fun logging(message: String, symbol: KSNode?) {
            messages.add(RawDiagnosticMessage(Diagnostic.Kind.NOTE, message, symbol.toLocation()))
        }

        override fun warn(message: String, symbol: KSNode?) {
            messages.add(
                RawDiagnosticMessage(Diagnostic.Kind.WARNING, message, symbol.toLocation())
            )
        }

        private fun KSNode?.toLocation(): RawDiagnosticMessage.Location? {
            val location = this?.location ?: return null
            return when (location) {
                is FileLocation ->
                    RawDiagnosticMessage.Location(
                        path = location.filePath,
                        line = location.lineNumber
                    )
                NonExistLocation -> null
            }
        }
    }

    companion object {
        private const val JAVA_SRC_OUT_FOLDER_NAME = "ksp-java-src-out"
        private const val KOTLIN_SRC_OUT_FOLDER_NAME = "ksp-kotlin-src-out"
        private const val RESOURCE_OUT_FOLDER_NAME = "ksp-resource-out"
        private const val CACHE_FOLDER_NAME = "ksp-cache"
        private const val CLASS_OUT_FOLDER_NAME = "class-out"
        private const val RESOURCES_OUT_FOLDER_NAME = "ksp-compiler/resourceOutputDir"
    }
}
