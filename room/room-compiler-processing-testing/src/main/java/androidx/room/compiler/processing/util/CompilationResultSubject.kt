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
import androidx.room.compiler.processing.util.compiler.TestCompilationResult
import androidx.room.compiler.processing.util.runner.CompilationTestRunner
import com.google.common.truth.Fact.fact
import com.google.common.truth.Fact.simpleFact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.StringSubject
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth
import com.google.testing.compile.Compilation
import java.util.regex.Pattern
import javax.tools.Diagnostic
import org.junit.AssumptionViolatedException

/** Holds the information about a test compilation result. */
@ExperimentalProcessingApi
abstract class CompilationResult
internal constructor(
    /** The test infra dwhich run this test */
    internal val testRunnerName: String,
    /** The [SyntheticProcessor] used in this compilation. */
    internal val processor: SyntheticProcessor,
    /** True if compilation result was success. */
    internal val successfulCompilation: Boolean,

    /** List of diagnostics that were reported during compilation */
    diagnostics: Map<Diagnostic.Kind, List<DiagnosticMessage>>
) {

    internal abstract val generatedSources: List<Source>

    internal abstract val generatedResources: List<Resource>

    val diagnostics = diagnostics.mapValues { it.value.filterNot { it.isIgnored() } }

    fun diagnosticsOfKind(kind: Diagnostic.Kind) = diagnostics[kind].orEmpty()

    /**
     * We report only errors reported via room diagnostics which means we might miss other compiler
     * errors, warnings etc. This output is free-text for compilers to print whatever is relevant to
     * them. Note that not including non-room diagnostic errors do not impact correctness as we
     * always assert compilation result.
     */
    abstract fun rawOutput(): String

    override fun toString(): String {
        return buildString {
            appendLine("CompilationResult (with $testRunnerName)")
            Diagnostic.Kind.values().forEach { kind ->
                val messages = diagnosticsOfKind(kind)
                appendLine("${kind.name}: ${messages.size}")
                messages.forEach { appendLine(it) }
                appendLine()
            }
            if (generatedSources.isEmpty()) {
                appendLine("Generated files: NONE")
            } else {
                appendLine("Generated files:")
                generatedSources.forEach { appendLine(it.relativePath) }
            }
            appendLine()
            appendLine("RAW OUTPUT:")
            appendLine(rawOutput())
        }
    }

    internal companion object {
        fun DiagnosticMessage.isIgnored() = FILTERED_MESSAGE_PREFIXES.any { msg.startsWith(it) }

        /** These messages are mostly verbose and not helpful for testing. */
        private val FILTERED_MESSAGE_PREFIXES =
            listOf(
                "No processor claimed any of these annotations:",
                "The following options were not recognized by any processor:",
                "Using Kotlin home directory",
                "Scripting plugin will not be loaded: not",
                "Using JVM IR backend",
                "Configuring the compilation environment",
                "Loading modules:",
                // TODO: Remove once we use a Kotlin 2.x version that has
                // https://github.com/JetBrains/kotlin/commit/7e9d6e601d007bc1250e1b47c6b2e55e3399145b
                "Kapt currently doesn't support language version"
            )
    }
}

/**
 * Truth subject that can run assertions on the [CompilationResult]. see:
 * [XTestInvocation.assertCompilationResult]
 */
@ExperimentalProcessingApi
class CompilationResultSubject
internal constructor(
    failureMetadata: FailureMetadata,
    val compilationResult: CompilationResult,
) : Subject<CompilationResultSubject, CompilationResult>(failureMetadata, compilationResult) {
    /** set to true if any assertion on the subject requires it to fail (e.g. looking for errors) */
    internal var shouldSucceed: Boolean = true

    /**
     * Asserts that compilation did fail. This covers the cases where the processor won't print any
     * diagnostics but compilation will still fail (e.g. bad generated code).
     *
     * @see hasError
     */
    fun compilationDidFail() = apply { shouldSucceed = false }

    /** Asserts free form output from the compilation output. */
    fun hasRawOutputContaining(expected: String) = apply {
        val found = compilationResult.rawOutput().contains(expected)
        if (!found) {
            failWithActual(simpleFact("Did not find $expected in the output."))
        }
    }

    /** Checks the compilation didn't have any warnings. */
    fun hasNoWarnings() = hasDiagnosticCount(Diagnostic.Kind.WARNING, 0)

    /** Check the compilation had [expected] number of error messages. */
    fun hasErrorCount(expected: Int) = hasDiagnosticCount(Diagnostic.Kind.ERROR, expected)

    /** Check the compilation had [expected] number of warning messages. */
    fun hasWarningCount(expected: Int) = hasDiagnosticCount(Diagnostic.Kind.WARNING, expected)

    private fun hasDiagnosticCount(kind: Diagnostic.Kind, expected: Int) = apply {
        val actual = compilationResult.diagnosticsOfKind(kind).size
        if (actual != expected) {
            failWithActual(simpleFact("expected $expected $kind messages, found $actual"))
        }
    }

    /**
     * Asserts that compilation has a warning with the given text.
     *
     * @see hasError
     * @see hasNote
     */
    fun hasWarning(expected: String) =
        hasDiagnosticWithMessage(
            kind = Diagnostic.Kind.WARNING,
            expected = expected,
            acceptPartialMatch = false
        ) {
            "expected warning: $expected"
        }

    /**
     * Asserts that compilation has a warning that contains the given text.
     *
     * @see hasErrorContaining
     * @see hasNoteContaining
     */
    fun hasWarningContaining(expected: String) =
        hasDiagnosticWithMessage(
            kind = Diagnostic.Kind.WARNING,
            expected = expected,
            acceptPartialMatch = true
        ) {
            "expected warning: $expected"
        }

    /**
     * Asserts that compilation has a warning containing text that matches the given pattern.
     *
     * @see hasErrorContainingMatch
     * @see hasNoteContainingMatch
     */
    fun hasWarningContainingMatch(expectedPattern: String): DiagnosticMessagesSubject {
        return hasDiagnosticWithPattern(
            kind = Diagnostic.Kind.WARNING,
            expectedPattern = expectedPattern,
            acceptPartialMatch = true
        ) {
            "expected warning containing pattern: $expectedPattern"
        }
    }

    /**
     * Asserts that compilation has a note with the given text.
     *
     * @see hasError
     * @see hasWarning
     */
    fun hasNote(expected: String) =
        hasDiagnosticWithMessage(
            kind = Diagnostic.Kind.NOTE,
            expected = expected,
            acceptPartialMatch = false
        ) {
            "expected note: $expected"
        }

    /**
     * Asserts that compilation has a note that contains the given text.
     *
     * @see hasErrorContaining
     * @see hasWarningContaining
     */
    fun hasNoteContaining(expected: String) =
        hasDiagnosticWithMessage(
            kind = Diagnostic.Kind.NOTE,
            expected = expected,
            acceptPartialMatch = true
        ) {
            "expected note: $expected"
        }

    /**
     * Asserts that compilation has a note containing text that matches the given pattern.
     *
     * @see hasErrorContainingMatch
     * @see hasWarningContainingMatch
     */
    fun hasNoteContainingMatch(expectedPattern: String): DiagnosticMessagesSubject {
        return hasDiagnosticWithPattern(
            kind = Diagnostic.Kind.NOTE,
            expectedPattern = expectedPattern,
            acceptPartialMatch = true
        ) {
            "expected note containing pattern: $expectedPattern"
        }
    }

    /**
     * Asserts that compilation has an error with the given text.
     *
     * @see hasWarning
     * @see hasNote
     */
    fun hasError(expected: String): DiagnosticMessagesSubject {
        shouldSucceed = false
        return hasDiagnosticWithMessage(
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
    fun hasErrorContaining(expected: String): DiagnosticMessagesSubject {
        shouldSucceed = false
        return hasDiagnosticWithMessage(
            kind = Diagnostic.Kind.ERROR,
            expected = expected,
            acceptPartialMatch = true
        ) {
            "expected error: $expected"
        }
    }

    /**
     * Asserts that compilation has an error containing text that matches the given pattern.
     *
     * @see hasWarningContainingMatch
     * @see hasNoteContainingMatch
     */
    fun hasErrorContainingMatch(expectedPattern: String): DiagnosticMessagesSubject {
        shouldSucceed = false
        return hasDiagnosticWithPattern(
            kind = Diagnostic.Kind.ERROR,
            expectedPattern = expectedPattern,
            acceptPartialMatch = true
        ) {
            "expected error containing pattern: $expectedPattern"
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
            failWithActual(simpleFact("expected at least one failure message"))
        }
    }

    /**
     * Asserts that a source file with the given [relativePath] was generated.
     *
     * @see generatedSource
     */
    fun generatedSourceFileWithPath(relativePath: String): StringSubject {
        val match = findGeneratedSource(relativePath)
        if (match == null) {
            failWithActual(simpleFact("Didn't generate file with path: $relativePath"))
        }
        return Truth.assertThat(match!!.contents)
    }

    fun findGeneratedSource(relativePath: String) =
        compilationResult.generatedSources.firstOrNull { it.relativePath == relativePath }

    /**
     * Asserts that the given source file is generated.
     *
     * Unlike Java compile testing, which does structural comparison, this method executes a line by
     * line comparison and is only able to ignore spaces and empty lines.
     *
     * @see generatedSourceFileWithPath
     */
    fun generatedSource(source: Source) = apply {
        val match =
            compilationResult.generatedSources.firstOrNull {
                it.relativePath == source.relativePath
            }
        if (match == null) {
            failWithActual(simpleFact("Didn't generate $source"))
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

    /** Asserts that a resource file with the given [relativePath] was generated. */
    fun generatedResourceFileWithPath(relativePath: String): PrimitiveByteArraySubject {
        val match =
            compilationResult.generatedResources.firstOrNull { it.relativePath == relativePath }
        if (match == null) {
            failWithActual(simpleFact("Didn't generate file with path: $relativePath"))
        }
        return Truth.assertThat(match!!.openInputStream().readAllBytes())
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
     * Checks if the processor has any remaining rounds that did not run which would possibly mean
     * it didn't run assertions it wanted to run.
     */
    internal fun assertAllExpectedRoundsAreCompleted() {
        if (compilationResult.processor.expectsAnotherRound()) {
            failWithActual(simpleFact("Test runner requested another round but that didn't happen"))
        }
    }

    internal fun assertNoProcessorAssertionErrors() {
        val processingException = compilationResult.processor.getProcessingException()
        if (processingException != null) {
            // processor has an assumption violation, re-throw so test case does not generate
            // a failure
            if (processingException is AssumptionViolatedException) {
                throw processingException
            }
            // processor has an error which we want to throw but we also want the subject, hence
            // we wrap it
            throw CompilationAssertionError(
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
    ): DiagnosticMessagesSubject {
        fun String.trimLines() = lines().joinToString(System.lineSeparator()) { it.trim() }
        val expectedTrimmed = expected.trimLines()
        val diagnostics = compilationResult.diagnosticsOfKind(kind)
        val matches =
            diagnostics.filter {
                if (acceptPartialMatch) {
                    it.msg.trimLines().contains(expectedTrimmed)
                } else {
                    it.msg.trimLines() == expectedTrimmed
                }
            }
        if (matches.isEmpty()) {
            failWithActual(simpleFact(buildErrorMessage()))
        }
        return DiagnosticMessagesSubject.assertThat(matches)
    }

    private fun hasDiagnosticWithPattern(
        kind: Diagnostic.Kind,
        expectedPattern: String,
        acceptPartialMatch: Boolean,
        buildErrorMessage: () -> String
    ): DiagnosticMessagesSubject {
        val diagnostics = compilationResult.diagnosticsOfKind(kind)
        val pattern = Pattern.compile(expectedPattern)
        val matches =
            diagnostics.filter {
                val matcher = pattern.matcher(it.msg)
                if (acceptPartialMatch) {
                    matcher.find()
                } else {
                    matcher.matches()
                }
            }
        if (matches.isEmpty()) {
            failWithActual(simpleFact(buildErrorMessage()))
        }
        return DiagnosticMessagesSubject.assertThat(matches)
    }

    /**
     * Helper error that does not include the stack trace from the test infra, instead, it just
     * reports the stack trace of the actual error with added log.
     */
    private class CompilationAssertionError(
        val compilationResult: CompilationResult,
        val realError: Throwable
    ) : AssertionError("Processor did throw an error.\n$compilationResult", realError) {
        override fun fillInStackTrace(): Throwable {
            return realError
        }
    }

    companion object {
        private val FACTORY =
            Factory<CompilationResultSubject, CompilationResult> { metadata, actual ->
                CompilationResultSubject(metadata, actual)
            }

        fun assertThat(compilationResult: CompilationResult): CompilationResultSubject {
            return Truth.assertAbout(FACTORY).that(compilationResult)
        }
    }
}

@ExperimentalProcessingApi
internal class JavaCompileTestingCompilationResult(
    testRunner: CompilationTestRunner,
    @Suppress("unused") private val delegate: Compilation,
    processor: SyntheticJavacProcessor,
    diagnostics: Map<Diagnostic.Kind, List<DiagnosticMessage>>,
    override val generatedSources: List<Source>,
    override val generatedResources: List<Resource>
) :
    CompilationResult(
        testRunnerName = testRunner.name,
        processor = processor,
        successfulCompilation = delegate.status() == Compilation.Status.SUCCESS,
        diagnostics = diagnostics
    ) {
    override fun rawOutput(): String {
        return delegate.diagnostics().joinToString(separator = System.lineSeparator()) {
            it.toString()
        }
    }
}

@ExperimentalProcessingApi
internal class KotlinCompilationResult
constructor(
    testRunner: CompilationTestRunner,
    processor: SyntheticProcessor,
    private val delegate: TestCompilationResult
) :
    CompilationResult(
        testRunnerName = testRunner.name,
        processor = processor,
        successfulCompilation = delegate.success,
        diagnostics = delegate.diagnostics
    ) {
    override val generatedSources: List<Source>
        get() = delegate.generatedSources

    override val generatedResources: List<Resource>
        get() = delegate.generatedResources

    override fun rawOutput(): String {
        return delegate.diagnostics
            .flatMap { it.value }
            .joinToString(separator = System.lineSeparator()) { it.toString() }
    }
}
