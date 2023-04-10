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
import androidx.room.compiler.processing.util.getSystemClasspaths
import java.io.File
import java.net.URLClassLoader
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.config.JvmTarget

/**
 * Utility object to run kotlin compiler via its CLI API.
 */
internal object KotlinCliRunner {
    private val compiler = K2JVMCompiler()
    private fun List<SourceSet>.existingRootPaths() = this.asSequence()
        .map { it.root }
        .filter { it.exists() }
        .map { it.canonicalPath }
        .distinct()

    private fun CompilationStepArguments.copyToCliArguments(cliArguments: K2JVMCompilerArguments) {
        // stdlib is in the classpath so no need to specify it here.
        cliArguments.noStdlib = true
        cliArguments.noReflect = true
        cliArguments.jvmTarget = JvmTarget.JVM_1_8.description
        cliArguments.noOptimize = true
        // useJavac & compileJava are experimental so lets not use it for now.
        cliArguments.useJavac = false
        cliArguments.compileJava = false
        cliArguments.jvmDefault = JvmDefaultMode.ALL_COMPATIBILITY.description
        cliArguments.allowNoSourceFiles = true
        cliArguments.javacArguments = javacArguments.toTypedArray()
        val inherited = if (inheritClasspaths) {
            inheritedClasspath
        } else {
            emptyList()
        }
        cliArguments.classpath = (additionalClasspaths + inherited)
            .filter { it.exists() }
            .distinct()
            .joinToString(
                separator = File.pathSeparator
            ) {
                it.canonicalPath
            }
        cliArguments.javaSourceRoots = this.sourceSets.filter {
            it.hasJavaSource
        }.existingRootPaths()
            .toList()
            .toTypedArray()
        cliArguments.freeArgs += this.sourceSets.filter {
            it.hasKotlinSource
        }.existingRootPaths()
    }

    /**
     * Runs the kotlin cli API with the given arguments.
     */
    @OptIn(ExperimentalCompilerApi::class)
    fun runKotlinCli(
        /**
         * Compilation arguments (sources, classpaths etc)
         */
        arguments: CompilationStepArguments,
        /**
         * Destination directory where generated class files will be written to
         */
        destinationDir: File,
        /**
         * List of component registrars for the compilation.
         */
        @Suppress("DEPRECATION")
        pluginRegistrars: List<org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar>
    ): KotlinCliResult {
        val cliArguments = compiler.createArguments()
        destinationDir.mkdirs()
        cliArguments.destination = destinationDir.absolutePath
        arguments.copyToCliArguments(cliArguments)
        compiler.parseArguments(arguments.kotlincArguments.toTypedArray(), cliArguments)

        val diagnosticsMessageCollector = DiagnosticsMessageCollector("kotlinc")
        val exitCode = DelegatingTestRegistrar.runCompilation(
            compiler = compiler,
            messageCollector = diagnosticsMessageCollector,
            arguments = cliArguments,
            pluginRegistrars = pluginRegistrars
        )

        return KotlinCliResult(
            exitCode = exitCode,
            diagnostics = diagnosticsMessageCollector.getDiagnostics(),
            compiledClasspath = destinationDir,
            kotlinCliArguments = cliArguments
        )
    }

    /**
     * Result of a kotlin compilation request
     */
    internal class KotlinCliResult(
        /**
         * The exit code reported by the compiler
         */
        val exitCode: ExitCode,
        /**
         * List of diagnostic messages reported by the compiler
         */
        val diagnostics: List<RawDiagnosticMessage>,
        /**
         * The output classpath for the compiled files.
         */
        val compiledClasspath: File,
        /**
         * Compiler arguments that were passed into Kotlin CLI
         */
        val kotlinCliArguments: K2JVMCompilerArguments
    )

    private val inheritedClasspath by lazy(LazyThreadSafetyMode.NONE) {
        getClasspathFromClassloader(KotlinCliRunner::class.java.classLoader)
    }

    // ported from https://github.com/google/compile-testing/blob/master/src/main/java/com
    // /google/testing/compile/Compiler.java#L231
    private fun getClasspathFromClassloader(referenceClassLoader: ClassLoader): List<File> {
        val platformClassLoader: ClassLoader = ClassLoader.getPlatformClassLoader()
        var currentClassloader = referenceClassLoader
        val systemClassLoader = ClassLoader.getSystemClassLoader()

        // Concatenate search paths from all classloaders in the hierarchy
        // 'till the system classloader.
        val classpaths: MutableSet<String> = LinkedHashSet()
        while (true) {
            if (currentClassloader === systemClassLoader) {
                classpaths.addAll(getSystemClasspaths())
                break
            }
            if (currentClassloader === platformClassLoader) {
                break
            }
            check(currentClassloader is URLClassLoader) {
                """Classpath for compilation could not be extracted
                since $currentClassloader is not an instance of URLClassloader
                """.trimIndent()
            }
            // We only know how to extract classpaths from URLClassloaders.
            currentClassloader.urLs.forEach { url ->
                check(url.protocol == "file") {
                    """Given classloader consists of classpaths which are unsupported for
                    compilation.
                    """.trimIndent()
                }
                classpaths.add(url.path)
            }
            currentClassloader = currentClassloader.parent
        }
        return classpaths.map { File(it) }.filter { it.exists() }
    }
}