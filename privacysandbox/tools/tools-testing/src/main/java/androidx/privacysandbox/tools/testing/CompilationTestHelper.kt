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
import androidx.room.compiler.processing.util.compiler.TestCompilationResult
import androidx.room.compiler.processing.util.DiagnosticMessage
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.compiler.TestCompilationArguments
import androidx.room.compiler.processing.util.compiler.compile
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.devtools.ksp.processing.SymbolProcessorProvider
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
        symbolProcessorProviders: List<SymbolProcessorProvider> = emptyList(),
        processorOptions: Map<String, String> = emptyMap()
    ): TestCompilationResult {
        val tempDir = Files.createTempDirectory("compile").toFile().also { it.deleteOnExit() }
        return compile(
            tempDir,
            TestCompilationArguments(
                sources = sources + syntheticPrivacySandboxSources,
                symbolProcessorProviders = symbolProcessorProviders,
                processorOptions = processorOptions,
            )
        )
    }

    fun assertThat(result: TestCompilationResult) = CompilationResultSubject(result)
}

class CompilationResultSubject(private val result: TestCompilationResult) {
    fun succeeds() {
        assertWithMessage(
            "UnexpectedErrors:\n${getFullErrorMessages()?.joinToString("\n")}"
        ).that(
            result.success
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

    private fun getShortErrorMessages() =
        result.diagnostics[Diagnostic.Kind.ERROR]?.map(DiagnosticMessage::msg)

    private fun getFullErrorMessages() =
        result.diagnostics[Diagnostic.Kind.ERROR]?.map { it.toFormattedMessage() }

    private fun DiagnosticMessage.toFormattedMessage() = """
            |Error: $msg
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
        return contents.split("\n").map {
            val lineHeader = if (--lineCountdown == 0) "->" else "  "
            "$lineHeader$it"
        }.joinToString("\n")
    }
}

// PrivacySandbox platform APIs are not available in AndroidX prebuilts nor are they stable, so
// while that's the case we use fake stubs to run our compilation tests.
val syntheticPrivacySandboxSources = listOf(
    Source.java(
        "android.app.sdksandbox.SdkSandboxManager", """
        |package android.app.sdksandbox;
        |
        |import android.os.Bundle;
        |import android.os.OutcomeReceiver;
        |import java.util.concurrent.Executor;
        |
        |public final class SdkSandboxManager {
        |    public void loadSdk(
        |        String sdkName,
        |        Bundle params,
        |        Executor executor,
        |        OutcomeReceiver<SandboxedSdk, LoadSdkException> receiver) {}
        |}
        |""".trimMargin()
    ),
    Source.java(
        "android.app.sdksandbox.SandboxedSdk", """
        |package android.app.sdksandbox;
        |
        |import android.os.IBinder;
        |
        |public final class SandboxedSdk {
        |    public SandboxedSdk(IBinder sdkInterface) {}
        |    public IBinder getInterface() { return null; }
        |}
        |""".trimMargin()
    ),
    Source.java(
        "android.app.sdksandbox.SandboxedSdkProvider", """
        |package android.app.sdksandbox;
        |
        |import android.content.Context;
        |import android.os.Bundle;
        |import android.view.View;
        |
        |public abstract class SandboxedSdkProvider {
        |    public abstract SandboxedSdk onLoadSdk(Bundle params) throws LoadSdkException;
        |    public abstract View getView(
        |            Context windowContext, Bundle params, int width, int height);
        |    public final Context getContext() {
        |        return null;
        |    }
        |    public abstract void onDataReceived(
        |            Bundle data, DataReceivedCallback callback);
        |    public interface DataReceivedCallback {
        |        void onDataReceivedSuccess(Bundle params);
        |        void onDataReceivedError(String errorMessage);
        |    }
        |}
        |""".trimMargin()
    ),
    Source.java(
        "android.app.sdksandbox.LoadSdkException", """
        |package android.app.sdksandbox;
        |
        |public final class LoadSdkException extends Exception {}
        |""".trimMargin()
    ),
    Source.java(
        "android.app.sdksandbox.SandboxedSdkContext", """
        |package android.app.sdksandbox;
        |
        |public final class SandboxedSdkContext {}
        |""".trimMargin()
    ),
)
