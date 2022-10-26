/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.privacysandbox.tools.testing

import androidx.room.compiler.processing.util.DiagnosticLocation
import androidx.room.compiler.processing.util.DiagnosticMessage
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.compiler.TestCompilationArguments
import androidx.room.compiler.processing.util.compiler.TestCompilationResult
import androidx.room.compiler.processing.util.compiler.compile
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import java.io.File
import java.nio.file.Files
import javax.tools.Diagnostic

object CompilationTestHelper {
    fun assertCompiles(sources: List<Source>): TestCompilationResult {
        val result = compileAll(sources)
        assertThat(result).succeeds()
        return result
    }

    fun compileAll(
        sources: List<Source>,
        extraClasspath: List<File> = emptyList(),
        includePrivacySandboxPlatformSources: Boolean = true,
        symbolProcessorProviders: List<SymbolProcessorProvider> = emptyList(),
        processorOptions: Map<String, String> = emptyMap()
    ): TestCompilationResult {
        val tempDir = Files.createTempDirectory("compile").toFile().also { it.deleteOnExit() }
        val targetSources = if (includePrivacySandboxPlatformSources) {
            sources + syntheticPrivacySandboxSources
        } else sources
        return compile(
            tempDir,
            TestCompilationArguments(
                sources = targetSources,
                classpath = extraClasspath,
                symbolProcessorProviders = symbolProcessorProviders,
                processorOptions = processorOptions,
            )
        )
    }

    fun assertThat(result: TestCompilationResult) = CompilationResultSubject(result)
}

val TestCompilationResult.resourceOutputDir: File
    get() = outputClasspath.first().parentFile.resolve("ksp-compiler/resourceOutputDir")

class CompilationResultSubject(private val result: TestCompilationResult) {
    fun succeeds() {
        assertWithMessage(
            "Unexpected errors:\n${getFullErrorMessages().joinToString("\n")}"
        ).that(
            result.success && getRawErrorMessages().isEmpty()
        ).isTrue()
    }

    fun generatesExactlySources(vararg sourcePaths: String) {
        succeeds()
        assertThat(result.generatedSources.map(Source::relativePath))
            .containsExactlyElementsIn(sourcePaths)
    }

    fun generatesSourcesWithContents(sources: List<Source>) {
        succeeds()
        val contentsByFile = result.generatedSources.associate { it.relativePath to it.contents }
        for (source in sources) {
            assertWithMessage("File ${source.relativePath} was not generated")
                .that(contentsByFile).containsKey(source.relativePath)
            assertWithMessage("Contents of file ${source.relativePath} don't match.")
                .that(contentsByFile[source.relativePath]).isEqualTo(source.contents)
        }
    }

    fun fails() {
        assertThat(result.success).isFalse()
    }

    fun containsError(error: String) {
        assertThat(getShortErrorMessages()).contains(error)
    }

    fun containsExactlyErrors(vararg errors: String) {
        assertThat(getShortErrorMessages()).containsExactly(*errors)
    }

    private fun getRawErrorMessages() =
        (result.diagnostics[Diagnostic.Kind.ERROR] ?: emptyList()) +
            (result.diagnostics[Diagnostic.Kind.WARNING] ?: emptyList()) +
            (result.diagnostics[Diagnostic.Kind.MANDATORY_WARNING] ?: emptyList())

    private fun getShortErrorMessages() =
        result.diagnostics[Diagnostic.Kind.ERROR]?.map(DiagnosticMessage::msg)

    private fun getFullErrorMessages() =
        getRawErrorMessages().map { it.toFormattedMessage() }

    private fun DiagnosticMessage.toFormattedMessage() = """
            |$kind: $msg
            |${location?.toFormattedLocation()}$
        """.trimMargin()

    private fun DiagnosticLocation.toFormattedLocation(): String {
        if (source == null) return "Location information missing"
        return """
            |Location: ${source!!.relativePath}:$line
            |File:
            |${contentsHighlightingLine(source!!.contents, line)}
        """.trimMargin()
    }

    private fun contentsHighlightingLine(contents: String, line: Int): String {
        var lineCountdown = line
        // Insert a "->" in the beginning of the highlighted line.
        return contents.split("\n").joinToString("\n") {
            val lineHeader = if (--lineCountdown == 0) "->" else "  "
            "$lineHeader$it"
        }
    }
}

// PrivacySandbox platform APIs are not available in AndroidX prebuilts nor are they stable, so
// while that's the case we use fake stubs to run our compilation tests.
val syntheticPrivacySandboxSources = listOf(
    Source.kotlin(
        "androidx/privacysandbox/sdkruntime/core/SandboxedSdkCompat.kt", """
        |package androidx.privacysandbox.sdkruntime.core
        |
        |import android.os.IBinder
        |
        |@Suppress("UNUSED_PARAMETER")
        |sealed class SandboxedSdkCompat {
        |    abstract fun getInterface(): IBinder?
        |
        |    companion object {
        |        fun create(binder: IBinder): SandboxedSdkCompat = throw RuntimeException("Stub!")
        |    }
        |}
        |""".trimMargin()
    ),
    Source.kotlin(
        "androidx/privacysandbox/sdkruntime/client/SdkSandboxManagerCompat.kt", """
        |package androidx.privacysandbox.sdkruntime.client
        |
        |import android.content.Context
        |import android.os.Bundle
        |import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat
        |
        |@Suppress("UNUSED_PARAMETER")
        |class SdkSandboxManagerCompat private constructor() {
        |    suspend fun loadSdk(
        |        sdkName: String,
        |        params: Bundle,
        |    ): SandboxedSdkCompat = throw RuntimeException("Stub!")
        |
        |    companion object {
        |        fun obtain(context: Context): SdkSandboxManagerCompat =
        |            throw RuntimeException("Stub!")
        |    }
        |}
        |""".trimMargin()
    ),
    Source.kotlin(
        "androidx/privacysandbox/sdkruntime/core/SandboxedSdkProviderCompat.kt", """
        |package androidx.privacysandbox.sdkruntime.core
        |
        |import android.content.Context
        |import android.os.Bundle
        |import android.view.View
        |
        |@Suppress("UNUSED_PARAMETER")
        |abstract class SandboxedSdkProviderCompat {
        |   var context: Context? = null
        |       private set
        |   fun attachContext(context: Context): Unit = throw RuntimeException("Stub!")
        |
        |   abstract fun onLoadSdk(params: Bundle): SandboxedSdkCompat
        |
        |   open fun beforeUnloadSdk() {}
        |
        |   abstract fun getView(
        |       windowContext: Context,
        |       params: Bundle,
        |       width: Int,
        |       height: Int
        |   ): View
        |}
        |""".trimMargin()
    )
)
