/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.kruth

import androidx.kruth.TestPhase.AFTER
import androidx.kruth.TestPhase.BEFORE
import androidx.kruth.TestPhase.DURING
import com.google.common.base.Preconditions.checkState
import com.google.common.base.Throwables.getStackTraceAsString
import org.junit.AssumptionViolatedException
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A [TestRule] that batches up all failures encountered during a test, and reports them all
 * together at the end (similar to [ErrorCollector][org.junit.rules.ErrorCollector]). It is also
 * useful for making assertions from other threads or from within callbacks whose exceptions would
 * be swallowed or logged, rather than propagated out to fail the test.
 * ([AssertJ](https://joel-costigliola.github.io/assertj) has a similar feature called
 * "soft assertions"; however, soft assertions are not safe for concurrent use.)
 *
 * Usage:
 *
 * ```
 * @get:Rule
 * val expect: Expect = Expect.create()
 *
 * ...
 *
 *    expect.that(results).containsExactly(...)
 *    expect.that(errors).isEmpty()
 * ```
 *
 * If both of the assertions above fail, the test will fail with an exception that contains
 * information about both.
 *
 * `Expect` may be used concurrently from multiple threads. However, multithreaded tests still
 * require care:
 *
 * * `Expect` has no way of knowing when all your other test threads are done. It simply
 *       checks for failures when the main thread finishes executing the test method. Thus, you must
 *       ensure that any background threads complete their assertions before then, or your test may
 *       ignore their results.
 * * Assertion failures are not the only exceptions that may occur in other threads. For maximum
 *       safety, multithreaded tests should check for such exceptions regardless of whether they use
 *       `Expect`. (Typically, this means calling `get()` on any `Future` returned
 *       by a method like `executor.submit(...)`. It might also include checking for
 *       unexpected log messages or reading metrics that count failures.) If your tests already
 *       check for exceptions from a thread, then that will any cover exception from plain
 *       `assertThat`.
 *
 * To record failures for the purpose of testing that an assertion fails when it should, see
 * [ExpectFailure].
 *
 * For more on this class, see [the documentation page](https://truth.dev/expect).
 */
// TODO(dustinlam): This class needs to be made thread-safe as Truth's version is synchronized.
class Expect private constructor(
    private val gatherer: ExpectationGatherer,
) : StandardSubjectBuilder(FailureMetadata(failureStrategy = gatherer)), TestRule {

    companion object {
        /** Creates a new instance. */
        @JvmStatic
        fun create(): Expect = Expect(ExpectationGatherer())
    }

    fun hasFailures(): Boolean {
        return gatherer.hasFailures()
    }

    override fun checkStatePreconditions() {
        gatherer.checkInRuleContext()
    }

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                gatherer.enterRuleContext()
                var caught: Throwable? = null
                try {
                    base.evaluate()
                } catch (t: Throwable) {
                    caught = t
                } finally {
                    gatherer.leaveRuleContext(caught)
                }
            }
        }
    }
}

private class ExpectationGatherer : FailureStrategy {
    private val failures: MutableList<AssertionError> = ArrayList()
    private var inRuleContext: TestPhase = BEFORE

    override fun fail(failure: AssertionError) {
        record(failure)
    }

    fun enterRuleContext() {
        checkState(inRuleContext == BEFORE)
        inRuleContext = DURING
    }

    fun leaveRuleContext(caught: Throwable?) {
        try {
            if (caught == null) {
                doLeaveRuleContext()
            } else {
                doLeaveRuleContext(caught)
            }
            /*
             * We'd like to check this even if an exception was thrown, but we don't want to override
             * the "real" failure. TODO(cpovirk): Maybe attach as a suppressed exception once we require
             * a newer version of Android.
             */
            checkState(inRuleContext == DURING)
        } finally {
            inRuleContext = AFTER
        }
    }

    fun checkInRuleContext() {
        doCheckInRuleContext(null)
    }

    fun hasFailures(): Boolean {
        return failures.isNotEmpty()
    }

    override fun toString(): String {
        if (failures.isEmpty()) {
            return "No expectation failed."
        }
        val numFailures = failures.size
        val message = buildString {
            append(numFailures)
            append(if (numFailures > 1) " expectations" else " expectation")
            append(" failed:\n")
            val countLength = (failures.size + 1).toString().length
            var count = 0
            for (failure: AssertionError in failures) {
                count++
                append("  ")
                append(count.toString().padStart(length = countLength, padChar = ' '))
                append(". ")
                if (count == 1) {
                    appendIndented(countLength, getStackTraceAsString(failure))
                } else {
                    appendIndented(
                        countLength,
                        printSubsequentFailure(failures[0].getStackTrace(), failure)
                    )
                }
                append("\n")
            }
        }
        return message
    }

    private fun printSubsequentFailure(
        baseTraceFrames: Array<StackTraceElement>,
        toPrint: AssertionError
    ): String {
        val e = RuntimeException("__EXCEPTION_MARKER__", toPrint)
        e.setStackTrace(baseTraceFrames)
        val s = getStackTraceAsString(e)
        // Force single line reluctant matching
        return s.replaceFirst(Regex("(?s)^.*?__EXCEPTION_MARKER__.*?Caused by:\\s+"), "")
    }

    private fun doCheckInRuleContext(failure: AssertionError?) {
        when (inRuleContext) {
            BEFORE -> throw IllegalStateException(
                "assertion made on Expect instance, but it's not enabled as a @Rule.", failure
            )

            DURING -> return
            AFTER -> throw IllegalStateException(
                "assertion made on Expect instance, but its @Rule has already completed. Maybe " +
                    "you're making assertions from a background thread and not waiting for them " +
                    "to complete, or maybe you've shared an Expect instance across multiple" +
                    " tests? We're throwing this exception to warn you that your assertion would " +
                    "have been ignored. However, this exception might not cause any test to " +
                    "fail, or it might cause some subsequent test to fail rather than the test " +
                    "that caused the problem.",
                failure
            )
        }
    }

    private fun doLeaveRuleContext() {
        if (hasFailures()) {
            throw AssertionErrorWithFacts.createWithNoStack(toString())
        }
    }

    /**
     * @throws Throwable
     */
    private fun doLeaveRuleContext(caught: Throwable) {
        if (hasFailures()) {
            val message = when (caught) {
                is AssumptionViolatedException -> {
                    "Also, after those failures, an assumption was violated:"
                }

                else -> "Also, after those failures, an exception was thrown:"
            }
            caught.stackTrace = emptyArray()
            record(AssertionErrorWithFacts.createWithNoStack(message, caught))
            throw AssertionErrorWithFacts.createWithNoStack(toString())
        } else {
            throw caught
        }
    }

    private fun record(failure: AssertionError) {
        doCheckInRuleContext(failure)
        failures.add(failure)
    }
}

private enum class TestPhase {
    BEFORE, DURING, AFTER
}

private fun StringBuilder.appendIndented(countLength: Int, toAppend: String) {
    val indent = countLength + 4 // "  " and ". "
    append(toAppend.replace("\n", "\n" + " ".repeat(indent)))
}
