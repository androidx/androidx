/*
 * Copyright 2020 The Android Open Source Project
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
import androidx.room.compiler.processing.util.KotlinCompilationUtil
import androidx.room.compiler.processing.util.KotlinCompileTestingCompilationResult
import androidx.room.compiler.processing.util.CompilationTestCapabilities
import androidx.room.compiler.processing.util.Source
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspArgs
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.symbolProcessorProviders
import java.io.ByteArrayOutputStream
import java.io.File
import javax.tools.Diagnostic

@ExperimentalProcessingApi
internal object KspCompilationTestRunner : CompilationTestRunner {

    override val name: String = "ksp"

    override fun canRun(params: TestCompilationParameters): Boolean {
        return CompilationTestCapabilities.canTestWithKsp
    }

    override fun compile(params: TestCompilationParameters): CompilationResult {
        @Suppress("NAME_SHADOWING")
        val sources = if (params.sources.none { it is Source.KotlinSource }) {
            // looks like this requires a kotlin source file
            // see: https://github.com/tschuchortdev/kotlin-compile-testing/issues/57
            params.sources + Source.kotlin("placeholder.kt", "")
        } else {
            params.sources
        }
        val syntheticKspProcessor = SyntheticKspProcessor(params.handlers)

        val combinedOutputStream = ByteArrayOutputStream()
        val kspCompilation = KotlinCompilationUtil.prepareCompilation(
            sources = sources,
            outputStream = combinedOutputStream,
            classpaths = params.classpath
        )
        kspCompilation.kspArgs.putAll(params.options)
        kspCompilation.symbolProcessorProviders = listOf(syntheticKspProcessor.asProvider())
        kspCompilation.compile()
        // ignore KSP result for now because KSP stops compilation, which might create false
        // negatives when java code accesses kotlin code.
        // TODO:  fix once https://github.com/tschuchortdev/kotlin-compile-testing/issues/72 is
        //  fixed

        // after ksp, compile without ksp with KSP's output as input
        val finalCompilation = KotlinCompilationUtil.prepareCompilation(
            sources = sources,
            outputStream = combinedOutputStream,
            classpaths = params.classpath,
        )
        // build source files from generated code
        finalCompilation.sources += kspCompilation.kspJavaSourceDir.collectSourceFiles() +
            kspCompilation.kspKotlinSourceDir.collectSourceFiles()
        val result = finalCompilation.compile()
        // workaround for: https://github.com/google/ksp/issues/122
        // KSP does not fail compilation for error diagnostics hence we do it here.
        val hasErrorDiagnostics = syntheticKspProcessor.messageWatcher
            .diagnostics()[Diagnostic.Kind.ERROR].orEmpty().isNotEmpty()
        return KotlinCompileTestingCompilationResult(
            testRunner = this,
            delegate = result,
            processor = syntheticKspProcessor,
            successfulCompilation = result.exitCode == KotlinCompilation.ExitCode.OK &&
                !hasErrorDiagnostics,
            outputSourceDirs = listOf(
                kspCompilation.kspJavaSourceDir,
                kspCompilation.kspKotlinSourceDir
            ),
            rawOutput = combinedOutputStream.toString(Charsets.UTF_8),
        )
    }

    // TODO get rid of these once kotlin compile testing supports two step compilation for KSP.
    //  https://github.com/tschuchortdev/kotlin-compile-testing/issues/72
    private val KotlinCompilation.kspJavaSourceDir: File
        get() = kspSourcesDir.resolve("java")

    private val KotlinCompilation.kspKotlinSourceDir: File
        get() = kspSourcesDir.resolve("kotlin")

    private fun File.collectSourceFiles(): List<SourceFile> {
        return walkTopDown().filter {
            it.isFile
        }.map { file ->
            SourceFile.fromPath(file)
        }.toList()
    }
}