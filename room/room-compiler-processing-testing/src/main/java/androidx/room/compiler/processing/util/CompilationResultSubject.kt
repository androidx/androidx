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

package androidx.room.compiler.processing.util

import androidx.room.compiler.processing.ExperimentalProcessingApi
import androidx.room.compiler.processing.SyntheticJavacProcessor
import androidx.room.compiler.processing.SyntheticProcessor
import androidx.room.compiler.processing.util.runner.CompilationTestRunner
import com.google.common.truth.Fact.fact
import com.google.common.truth.Fact.simpleFact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth
import com.google.testing.compile.Compilation
import com.tschuchort.compiletesting.KotlinCompilation
import java.io.File
import javax.tools.Diagnostic

/**
 * Holds the information about a test compilation result.
 */
@ExperimentalProcessingApi
abstract class CompilationResult internal constructor(
    /**
     * The test infra which run this test
     */
    internal val testRunnerName: String,
    /**
     * The [SyntheticProcessor] used in this compilation.
     */
    internal val processor: SyntheticProcessor,
    /**
     * True if compilation result was success.
     */
    internal val successfulCompilation: Boolean,
) {
    internal abstract val generatedSources: List<Source>

    private val diagnostics = processor.messageWatcher.diagnostics()

    fun diagnosticsOfKind(kind: Diagnostic.Kind) = diagnostics[kind].orEmpty()

    /**
     * We report only errors reported via room diagnostics which means we might miss other
     * compiler errors, warnings etc. This output is free-text for compilers to print whatever is
     * relevant to them. Note that not including non-room diagnostic errors do not impact
     * correctness as we always assert compilation result.
     */
    abstract fun rawOutput(): String

    override fun toString(): String {
        return buildString {
            appendLine("CompilationResult (with $testRunnerName)")
            Diagnostic.Kind.values().forEach { kind ->
                val messages = diagnosticsOfKind(kind)
                appendLine("${kind.name}: ${messages.size}")
                messages.forEach {
                    appendLine(it)
                }
                appendLine()
            }
            appendLine("Generated files:")
            generatedSources.forEach {
                appendLine(it.relativePath)
            }
            appendLine("RAW OUTPUT:")
            appendLine(rawOutput())
        }
    }
}

/**
 * Truth subject that can run assertions on the [CompilationResult].
 * see: [XTestInvocation.assertCompilationResult]
 */
@ExperimentalProcessingApi
class CompilationResultSubject internal constructor(
    failureMetadata: FailureMetadata,
    val compilationResult: CompilationResult,
) : Subject<CompilationResultSubject, CompilationResult>(
    failureMetadata, compilationResult
) {
    /**
     * set to true if any assertion on the subject requires it to fail (e.g. looking for errors)
     */
    internal var shouldSucceed: Boolean = true

    /**
     * Asserts that compilation did fail. This covers the cases where the processor won't print
     * any diagnostics but compilation will still fail (e.g. bad generated code).
     *
     * @see hasError
     */
    fun compilationDidFail() = apply {
        shouldSucceed = false
    }

    /**
     * Asserts free form output from the compilation output.
     */
    fun hasRawOutputContaining(expected: String) = apply {
        val found = compilationResult.rawOutput().contains(expected)
        if (!found) {
            failWithActual(
                simpleFact("Did not find $expected in the output.")
            )
        }
    }

    /**
     * Checks the compilation didn't have any warnings.
     */
    fun hasNoWarnings() = hasDiagnosticCount(Diagnostic.Kind.WARNING, 0)

    /**
     * Check the compilation had [expected] number of error messages.
     */
    fun hasErrorCount(expected: Int) = hasDiagnosticCount(Diagnostic.Kind.ERROR, expected)

    /**
     * Check the compilation had [expected] number of warning messages.
     */
    fun hasWarningCount(expected: Int) = hasDiagnosticCount(Diagnostic.Kind.WARNING, expected)

    private fun hasDiagnosticCount(kind: Diagnostic.Kind, expected: Int) = apply {
        val actual = compilationResult.diagnosticsOfKind(kind).size
        if (actual != expected) {
            failWithActual(
                simpleFact("expected $expected $kind messages, found $actual")
            )
        }
    }
    /**
     * Asserts that compilation has a warning with the given text.
     *
     * @see hasError
     * @see hasNote
     */
    fun hasWarning(expected: String) = apply {
        hasDiagnosticWithMessage(
            kind = Diagnostic.Kind.WARNING,
            expected = expected,
            acceptPartialMatch = false
        ) {
            "expected warning: $expected"
        }
    }

    /**
     * Asserts that compilation has a warning that contains the given text.
     *
     * @see hasErrorContaining
     * @see hasNoteContaining
     */
    fun hasWarningContaining(expected: String) = apply {
        hasDiagnosticWithMessage(
            kind = Diagnostic.Kind.WARNING,
            expected = expected,
            acceptPartialMatch = true
        ) {
            "expected warning: $expected"
        }
    }

    /**
     * Asserts that compilation has a note with the given text.
     *
     * @see hasError
     * @see hasWarning
     */
    fun hasNote(expected: String) = apply {
        hasDiagnosticWithMessage(
            kind = Diagnostic.Kind.NOTE,
            expected = expected,
            acceptPartialMatch = false
        ) {
            "expected note: $expected"
        }
    }

    /**
     * Asserts that compilation has a note that contains the given text.
     *
     * @see hasErrorContaining
     * @see hasWarningContaining
     */
    fun hasNoteContaining(expected: String) = apply {
        hasDiagnosticWithMessage(
            kind = Diagnostic.Kind.NOTE,
            expected = expected,
            acceptPartialMatch = true
        ) {
            "expected note: $expected"
        }
    }

    /**
     * Asserts that compilation has an error with the given text.
     *
     * @see hasWarning
     * @see hasNote
     */
    fun hasError(expected: String) = apply {
        shouldSucceed = false
        hasDiagnosticWithMessage(
            kind = Diagnostic.Kind.ERROR,
            expected = expected,
            acceptPartialMatch = false
        ) {
            "expected error: $expected"
        }
    }

    /**
     * Asserts that compilation has an error that contains the given text.
     *
     * @see hasWarningContaining
     * @see hasNoteContaining
     */
    fun hasErrorContaining(expected: String) = apply {
        shouldSucceed = false
        hasDiagnosticWithMessage(
            kind = Diagnostic.Kind.ERROR,
            expected = expected,
            acceptPartialMatch = true
        ) {
            "expected error: $expected"
        }
    }

    /**
     * Asserts that compilation has at least one diagnostics message with kind error.
     *
     * @see compilationDidFail
     * @see hasWarning
     */
    fun hasError() = apply {
        shouldSucceed = false
        if (compilationResult.diagnosticsOfKind(Diagnostic.Kind.ERROR).isEmpty()) {
            failWithActual(
                simpleFact("expected at least one failure message")
            )
        }
    }

    /**
     * Asserts that a file with the given [relativePath] was generated.
     *
     * @see generatedSource
     */
    fun generatedSourceFileWithPath(relativePath: String) = apply {
        val match = compilationResult.generatedSources.firstOrNull {
            it.relativePath == relativePath
        }
        if (match == null) {
            failWithActual(
                simpleFact("Didn't generate file with path: $relativePath")
            )
        }
    }
    /**
     * Asserts that the given source file is generated.
     *
     * Unlike Java compile testing, which does structural comparison, this method executes a line
     * by line comparison and is only able to ignore spaces and empty lines.
     *
     * @see generatedSourceFileWithPath
     */
    fun generatedSource(source: Source) = apply {
        val match = compilationResult.generatedSources.firstOrNull {
            it.relativePath == source.relativePath
        }
        if (match == null) {
            failWithActual(
                simpleFact("Didn't generate $source")
            )
            return@apply
        }
        val mismatch = source.findMismatch(match)
        if (mismatch != null) {
            failWithActual(
                simpleFact("Generated code does not match expected"),
                fact("mismatch", mismatch),
                fact("expected", source.contents),
                fact("actual", match.contents),
            )
        }
    }

    /**
     * Called after handler is invoked to check its compilation failure assertion against the
     * compilation result.
     */
    internal fun assertCompilationResult() {
        if (compilationResult.successfulCompilation != shouldSucceed) {
            failWithActual(
                simpleFact(
                    "expected compilation result to be: $shouldSucceed but was " +
                        "${compilationResult.successfulCompilation}"
                )
            )
        }
    }

    /**
     * Checks if the processor has any remaining rounds that did not run which would possibly
     * mean it didn't run assertions it wanted to run.
     */
    internal fun assertAllExpectedRoundsAreCompleted() {
        if (compilationResult.processor.expectsAnotherRound()) {
            failWithActual(
                simpleFact("Test runner requested another round but that didn't happen")
            )
        }
    }

    internal fun assertNoProcessorAssertionErrors() {
        val processingException = compilationResult.processor.getProcessingException()
        if (processingException != null) {
            // processor has an error which we want to throw but we also want the subject, hence
            // we wrap it
            throw createProcessorAssertionError(
                compilationResult = compilationResult,
                realError = processingException
            )
        }
    }

    private fun hasDiagnosticWithMessage(
        kind: Diagnostic.Kind,
        expected: String,
        acceptPartialMatch: Boolean,
        buildErrorMessage: () -> String
    ) {
        val diagnostics = compilationResult.diagnosticsOfKind(kind)
        if (diagnostics.any { it.msg == expected }) {
            return
        }
        if (acceptPartialMatch && diagnostics.any { it.msg.contains(expected) }) {
            return
        }
        failWithActual(simpleFact(buildErrorMessage()))
    }

    /**
     * Helper method to create an exception that does not include the stack trace from the test
     * infra, instead, it just reports the stack trace of the actual error with added log.
     */
    private fun createProcessorAssertionError(
        compilationResult: CompilationResult,
        realError: Throwable
    ) = object : AssertionError("processor did throw an error\n$compilationResult", realError) {
        override fun fillInStackTrace(): Throwable {
            return realError
        }
    }

    companion object {
        private val FACTORY =
            Factory<CompilationResultSubject, CompilationResult> { metadata, actual ->
                CompilationResultSubject(metadata, actual)
            }

        fun assertThat(
            compilationResult: CompilationResult
        ): CompilationResultSubject {
            return Truth.assertAbout(FACTORY).that(
                compilationResult
            )
        }
    }
}

@ExperimentalProcessingApi
internal class JavaCompileTestingCompilationResult(
    testRunner: CompilationTestRunner,
    @Suppress("unused")
    private val delegate: Compilation,
    processor: SyntheticJavacProcessor
) : CompilationResult(
    testRunnerName = testRunner.name,
    processor = processor,
    successfulCompilation = delegate.status() == Compilation.Status.SUCCESS
) {
    override val generatedSources: List<Source> by lazy {
        if (successfulCompilation) {
            delegate.generatedSourceFiles().map(Source::fromJavaFileObject)
        } else {
            // java compile testing does not provide access to generated files when compilation
            // fails
            emptyList()
        }
    }

    override fun rawOutput(): String {
        return delegate.diagnostics().joinToString {
            it.toString()
        }
    }
}
@ExperimentalProcessingApi
internal class KotlinCompileTestingCompilationResult(
    testRunner: CompilationTestRunner,
    @Suppress("unused")
    private val delegate: KotlinCompilation.Result,
    processor: SyntheticProcessor,
    successfulCompilation: Boolean,
    outputSourceDirs: List<File>,
    private val rawOutput: String,
) : CompilationResult(
    testRunnerName = testRunner.name,
    processor = processor,
    successfulCompilation = successfulCompilation
) {
    override val generatedSources: List<Source> by lazy {
        outputSourceDirs.flatMap { srcRoot ->
            srcRoot.walkTopDown().mapNotNull { sourceFile ->
                when {
                    sourceFile.name.endsWith(".java") -> {
                        val qName = sourceFile.absolutePath.substringAfter(
                            srcRoot.absolutePath
                        ).dropWhile { it == '/' }
                            .replace('/', '.')
                            .dropLast(".java".length)
                        Source.loadJavaSource(sourceFile, qName)
                    }
                    sourceFile.name.endsWith(".kt") -> {
                        val relativePath = sourceFile.absolutePath.substringAfter(
                            srcRoot.absolutePath
                        ).dropWhile { it == '/' }
                        Source.loadKotlinSource(sourceFile, relativePath)
                    }
                    else -> null
                }
            }
        }
    }

    override fun rawOutput() = rawOutput
}
