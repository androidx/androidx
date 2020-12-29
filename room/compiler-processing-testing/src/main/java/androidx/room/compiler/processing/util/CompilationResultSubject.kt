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

import androidx.room.compiler.processing.SyntheticJavacProcessor
import androidx.room.compiler.processing.SyntheticProcessor
import androidx.room.compiler.processing.util.runner.CompilationTestRunner
import com.google.common.truth.Fact.simpleFact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth
import com.google.testing.compile.Compilation
import com.google.testing.compile.CompileTester
import com.tschuchort.compiletesting.KotlinCompilation
import javax.tools.Diagnostic

/**
 * Holds the information about a test compilation result.
 */
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
            appendLine("RAW OUTPUT:")
            appendLine(rawOutput())
        }
    }
}

/**
 * Truth subject that can run assertions on the [CompilationResult].
 * see: [XTestInvocation.assertCompilationResult]
 */
class CompilationResultSubject(
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
    fun compilationDidFail() = chain {
        shouldSucceed = false
    }

    /**
     * Asserts that compilation has a warning with the given text.
     *
     * @see hasError
     */
    fun hasWarning(expected: String) = chain {
        hasDiagnosticWithMessage(
            kind = Diagnostic.Kind.WARNING,
            expected = expected
        ) {
            "expected warning: $expected"
        }
    }

    /**
     * Asserts that compilation has an error with the given text.
     *
     * @see hasWarning
     */
    fun hasError(expected: String) = chain {
        shouldSucceed = false
        hasDiagnosticWithMessage(
            kind = Diagnostic.Kind.ERROR,
            expected = expected
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
    fun hasError() = chain {
        shouldSucceed = false
        if (actual().diagnosticsOfKind(Diagnostic.Kind.ERROR).isEmpty()) {
            failWithActual(
                simpleFact("expected at least one failure message")
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

    internal fun assertNoProcessorAssertionErrors() {
        val processingException = compilationResult.processor.getProcessingException()
        if (processingException != null) {
            // processor has an error which we want to throw but we also want the subject, hence
            // we wrap it
            throw AssertionError(
                "Processor reported an error. See the cause for details\n" +
                    "$compilationResult",
                processingException
            )
        }
    }

    private fun hasDiagnosticWithMessage(
        kind: Diagnostic.Kind,
        expected: String,
        buildErrorMessage: () -> String
    ) {
        val diagnostics = compilationResult.diagnosticsOfKind(kind)
        if (diagnostics.any { it.msg == expected }) {
            return
        }
        failWithActual(simpleFact(buildErrorMessage()))
    }

    private fun chain(
        block: () -> Unit
    ): CompileTester.ChainingClause<CompilationResultSubject> {
        block()
        return CompileTester.ChainingClause<CompilationResultSubject> {
            this
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
    override fun rawOutput(): String {
        return delegate.diagnostics().joinToString {
            it.toString()
        }
    }
}

internal class KotlinCompileTestingCompilationResult(
    testRunner: CompilationTestRunner,
    @Suppress("unused")
    private val delegate: KotlinCompilation.Result,
    processor: SyntheticProcessor,
    successfulCompilation: Boolean
) : CompilationResult(
    testRunnerName = testRunner.name,
    processor = processor,
    successfulCompilation = successfulCompilation
) {
    override fun rawOutput(): String {
        return delegate.messages
    }
}