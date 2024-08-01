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
import androidx.room.compiler.processing.util.KOTLINC_LANGUAGE_1_9_ARGS
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
        symbolProcessorProviders: List<SymbolProcessorProvider> = emptyList(),
        processorOptions: Map<String, String> = emptyMap(),
    ): TestCompilationResult {
        val tempDir = Files.createTempDirectory("compile").toFile().also { it.deleteOnExit() }
        return compile(
            tempDir,
            TestCompilationArguments(
                sources = sources,
                classpath = extraClasspath,
                symbolProcessorProviders = symbolProcessorProviders,
                processorOptions = processorOptions,
                kotlincArguments = KOTLINC_LANGUAGE_1_9_ARGS
            )
        )
    }

    fun assertThat(result: TestCompilationResult) = CompilationResultSubject(result)
}

val TestCompilationResult.resourceOutputDir: File
    get() = outputClasspath.first().parentFile.resolve("ksp-compiler/resourceOutputDir")

fun hasAllExpectedGeneratedSourceFilesAndContent(
    actualKotlinSources: List<Source>,
    expectedKotlinSources: List<Source>,
    expectedAidlFilepath: List<String>
) {
    val expectedRelativePaths =
        expectedKotlinSources.map(Source::relativePath) + expectedAidlFilepath
    assertThat(actualKotlinSources.map(Source::relativePath))
        .containsExactlyElementsIn(expectedRelativePaths)

    val actualRelativePathMap = actualKotlinSources.associateBy(Source::relativePath)
    for (expectedKotlinSource in expectedKotlinSources) {
        assertWithMessage(
                "Contents of generated file ${expectedKotlinSource.relativePath} don't " +
                    "match golden."
            )
            .that(actualRelativePathMap[expectedKotlinSource.relativePath]?.contents)
            .isEqualTo(expectedKotlinSource.contents)
    }
}

class CompilationResultSubject(private val result: TestCompilationResult) {
    fun succeeds() {
        assertWithMessage("Unexpected errors:\n${getFullErrorMessages().joinToString("\n")}")
            .that(result.success && getRawErrorMessages().isEmpty())
            .isTrue()
    }

    fun hasAllExpectedGeneratedSourceFilesAndContent(
        expectedKotlinSources: List<Source>,
        expectedAidlFilepath: List<String>
    ) {
        hasAllExpectedGeneratedSourceFilesAndContent(
            result.generatedSources,
            expectedKotlinSources,
            expectedAidlFilepath
        )
    }

    fun hasNoGeneratedSourceFiles() {
        assertThat(result.generatedSources).isEmpty()
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

    private fun getRawErrorMessages(): List<DiagnosticMessage> {
        return (result.diagnostics[Diagnostic.Kind.ERROR] ?: emptyList()) +
            (result.diagnostics[Diagnostic.Kind.WARNING] ?: emptyList()) +
            (result.diagnostics[Diagnostic.Kind.MANDATORY_WARNING] ?: emptyList())
    }

    private fun getShortErrorMessages() =
        result.diagnostics[Diagnostic.Kind.ERROR]?.map(DiagnosticMessage::msg)

    private fun getFullErrorMessages() = getRawErrorMessages().map { it.toFormattedMessage() }

    private fun DiagnosticMessage.toFormattedMessage() =
        """
            |$kind: $msg
            |${location?.toFormattedLocation()}$
        """
            .trimMargin()

    private fun DiagnosticLocation.toFormattedLocation(): String {
        if (source == null) return "Location information missing"
        return """
            |Location: ${source!!.relativePath}:$line
            |File:
            |${contentsHighlightingLine(source!!.contents, line)}
        """
            .trimMargin()
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
