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

import androidx.room.compiler.processing.util.compiler.steps.CompilationStepArguments
import androidx.room.compiler.processing.util.compiler.steps.RawDiagnosticMessage
import java.io.File
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.compiler.plugin.parseLegacyPluginOption
import org.jetbrains.kotlin.config.LanguageVersion

/** Utility object to run kotlin compiler via its CLI API. */
internal object KotlinCliRunner {
    private val compiler = K2JVMCompiler()

    /** Runs the Kotlin CLI API with the given arguments. */
    fun runKotlinCli(
        /** Compilation arguments (sources, classpaths etc) */
        arguments: CompilationStepArguments,
        /** Destination directory where generated class files will be written to */
        destinationDir: File,
        /** List of plugin registrars for the compilation. */
        @OptIn(ExperimentalCompilerApi::class)
        pluginRegistrars: PluginRegistrarArguments =
            PluginRegistrarArguments(emptyList(), emptyList())
    ): KotlinCliResult {
        destinationDir.mkdirs()
        val cliArguments =
            compiler.createArguments().apply {
                destination = destinationDir.absolutePath
                arguments.copyToCliArguments(this)
            }
        compiler.parseArguments(arguments.kotlincArguments.toTypedArray(), cliArguments)

        val diagnosticsMessageCollector = DiagnosticsMessageCollector("kotlinc")
        val exitCode =
            DelegatingTestRegistrar.runCompilation(
                compiler = compiler,
                messageCollector = diagnosticsMessageCollector,
                arguments = cliArguments,
                registrars = pluginRegistrars
            )

        return KotlinCliResult(
            exitCode = exitCode,
            diagnostics = diagnosticsMessageCollector.getDiagnostics(),
            compiledClasspath = destinationDir,
            kotlinCliArguments = cliArguments
        )
    }

    /** Get the language version specified with `-language-version=xxx`. */
    fun getLanguageVersion(kotlincArguments: List<String>): LanguageVersion {
        val cliArguments = compiler.createArguments()
        compiler.parseArguments(kotlincArguments.toTypedArray(), cliArguments)
        return cliArguments.languageVersion?.let { LanguageVersion.fromVersionString(it) }
            ?: TestDefaultOptions.kotlinLanguageVersion
    }

    private fun CompilationStepArguments.copyToCliArguments(cliArguments: K2JVMCompilerArguments) {
        // stdlib is in the classpath so no need to specify it here.
        cliArguments.noStdlib = true
        cliArguments.noReflect = true
        cliArguments.noOptimize = true

        // We want allow no sources to run test handlers
        cliArguments.allowNoSourceFiles = true

        cliArguments.languageVersion = TestDefaultOptions.kotlinLanguageVersion.versionString
        cliArguments.apiVersion = TestDefaultOptions.kotlinApiVersion.versionString
        cliArguments.jvmTarget = TestDefaultOptions.jvmTarget.description
        cliArguments.jvmDefault = TestDefaultOptions.jvmDefaultMode.description

        // useJavac & compileJava are experimental so lets not use it for now.
        cliArguments.useJavac = false
        cliArguments.compileJava = false

        cliArguments.javacArguments = javacArguments.toTypedArray()

        val inherited =
            if (inheritClasspaths) {
                TestClasspath.inheritedClasspath
            } else {
                emptyList()
            }
        cliArguments.classpath =
            (additionalClasspaths + inherited)
                .filter { it.exists() }
                .distinct()
                .joinToString(separator = File.pathSeparator) { it.canonicalPath }

        cliArguments.javaSourceRoots =
            this.sourceSets.filter { it.hasJavaSource }.existingRootPaths().toList().toTypedArray()

        // Sources to compile are passed as args
        cliArguments.freeArgs += this.sourceSets.filter { it.hasKotlinSource }.existingRootPaths()
    }

    /** Result of a kotlin compilation request */
    internal class KotlinCliResult(
        /** The exit code reported by the compiler */
        val exitCode: ExitCode,
        /** List of diagnostic messages reported by the compiler */
        val diagnostics: List<RawDiagnosticMessage>,
        /** The output classpath for the compiled files. */
        val compiledClasspath: File,
        /** Compiler arguments that were passed into Kotlin CLI */
        val kotlinCliArguments: K2JVMCompilerArguments
    )

    internal fun getPluginOptions(
        pluginId: String,
        kotlincArguments: List<String>
    ): Map<String, String> {
        val options =
            kotlincArguments
                .dropLast(1)
                .zip(kotlincArguments.drop(1))
                .filter { it.first == "-P" }
                .mapNotNull { parseLegacyPluginOption(it.second) }
        return options
            .filter { it.pluginId == pluginId }
            .associateBy({ it.optionName }, { it.value })
    }
}
