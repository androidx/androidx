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

import androidx.kruth.ExpectFailure.Companion.expectFailure
import androidx.kruth.TruthFailureSubject.Companion.truthFailures
import com.google.common.base.Throwables.getStackTraceAsString
import com.google.errorprone.annotations.CanIgnoreReturnValue
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A utility for testing that assertions against a custom [Subject] fail when they should, plus a
 * utility to assert about parts of the resulting failure messages.
 *
 * Usage:
 * ```
 * val failure: AssertionError = expectFailure {
 *   whenTesting -> whenTesting.that(cancelButton).isVisible()
 * }
 * assertThat(failure).factKeys().containsExactly("expected to be visible")
 *
 * ...
 *
 * private fun expectFailure(
 *   assertionCallback: ExpectFailure.SimpleSubjectBuilderCallback<UiElementSubject, UiElement>
 * ): AssertionError {
 *   return ExpectFailure.expectFailureAbout(uiElements(), assertionCallback)
 * }
 * ```
 *
 * Or, if you can't use lambdas:
 * ```
 * @get:Rule val expectFailure: ExpectFailure = ExpectFailure()
 *
 * ...
 *
 *     expectFailure.whenTesting().about(uiElements()).that(cancelButton).isVisible()
 *     assertThat(failure).factKeys().containsExactly("expected to be visible")
 * ```
 *
 * [ExpectFailure] is similar to JUnit's `assertThrows` (
 * [JUnit4](https://junit.org/junit4/javadoc/latest/org/junit/Assert.html#assertThrows%28java.lang.Class,%20org.junit.function.ThrowingRunnable%29)
 * ,
 * [JUnit5](https://junit.org/junit5/docs/current/api/org/junit/jupiter/api/Assertions.html#assertThrows%28java.lang.Class,org.junit.jupiter.api.function.Executable%29)
 * ). We recommend it over `assertThrows` when you're testing a Truth subject because it also checks
 * that the assertion you're testing uses the supplied [FailureStrategy] and calls
 * [FailureStrategy.fail] only once.
 *
 * @constructor Creates a new instance for use as a `Rule`. See the class documentation for details,
 *   and consider using [the lambda version][expectFailure] instead.
 */
class ExpectFailure : TestRule {
    private var inRuleContext = false
    private var failureExpected = false
    private var failure: AssertionError? = null

    /**
     * Returns a test verb that expects the chained assertion to fail, and makes the failure
     * available via [getFailure].
     *
     * An instance of [ExpectFailure] supports only one [whenTesting] call per test method. The
     * static [expectFailure] method, by contrast, does not have this limitation.
     */
    fun whenTesting(): StandardSubjectBuilder {
        require(inRuleContext) { "ExpectFailure must be used as a JUnit @Rule" }
        if (failure != null) {
            throw AssertionErrorWithFacts("ExpectFailure already captured a failure", failure)
        }
        if (failureExpected) {
            throw AssertionError(
                "ExpectFailure.whenTesting() called previously, but did not capture a failure."
            )
        }
        failureExpected = true
        return StandardSubjectBuilder.forCustomFailureStrategy(::captureFailure)
    }

    /**
     * Enters rule context to be ready to capture failures.
     *
     * This should be rarely used directly, except if this class is as a long living object but not
     * as a JUnit rule, like truth subject tests where for GWT compatible reasons.
     */
    internal fun enterRuleContext() {
        inRuleContext = true
    }

    /** Leaves rule context and verify if a failure has been caught if it's expected. */
    internal fun leaveRuleContext() {
        inRuleContext = false
    }

    /**
     * Ensures a failure is caught if it's expected (i.e., [whenTesting] is called) and throws error
     * if not.
     */
    internal fun ensureFailureCaught() {
        if (failureExpected && failure == null) {
            throw AssertionError("ExpectFailure.whenTesting() invoked, but no failure was caught.")
        }
    }

    /** Returns the captured failure, if one occurred. */
    fun getFailure(): AssertionError {
        return failure ?: throw AssertionError("ExpectFailure did not capture a failure.")
    }

    /**
     * Captures the provided failure, or throws an [AssertionError] if a failure had previously been
     * captured.
     */
    private fun captureFailure(captured: AssertionError) {
        failure?.let { failure ->
            // TODO(diamondm) is it worthwhile to add the failures as suppressed exceptions?
            throw AssertionError(
                lenientFormat(
                    "ExpectFailure.whenTesting() caught multiple failures:\n\n%s\n\n%s\n",
                    arrayOf(getStackTraceAsString(failure), getStackTraceAsString(captured))
                )
            )
        }
        failure = captured
    }

    companion object {
        /**
         * Static alternative that directly returns the triggered failure. This is intended to be
         * used in Java 8+ tests similar to `expectThrows`:
         * ```
         * val failure: AssertionError = expectFailure { whenTesting ->
         *   whenTesting.that(4).isNotEqualTo(4)
         * }
         * ```
         */
        @JvmStatic
        fun expectFailure(assertionCallback: StandardSubjectBuilderCallback): AssertionError {
            val expectFailure = ExpectFailure()
            expectFailure.enterRuleContext() // safe since this instance doesn't leave this method
            assertionCallback.invokeAssertion(expectFailure.whenTesting())
            return expectFailure.getFailure()
        }

        /**
         * Static alternative that directly returns the triggered failure. This is intended to be
         * used in Java 8+ tests similar to `expectThrows`:
         * ```
         * val failure: AssertionError = expectFailureAbout(myTypes()) { whenTesting ->
         *   whenTesting.that(myType).hasProperty()
         * }
         * ```
         */
        @JvmStatic
        @CanIgnoreReturnValue
        fun <S : Subject<A>, A> expectFailureAbout(
            factory: Subject.Factory<S, A>,
            assertionCallback: SimpleSubjectBuilderCallback<S, A>
        ): AssertionError {
            return expectFailure { whenTesting ->
                assertionCallback.invokeAssertion(whenTesting.about(factory))
            }
        }

        /**
         * Creates a subject for asserting about the given [AssertionError], usually one produced by
         * Truth.
         */
        @JvmStatic
        fun <T : AssertionError> assertThat(actual: T?): TruthFailureSubject<T> {
            return assertAbout(truthFailures<T>()).that(actual)
        }
    }

    override fun apply(base: Statement, description: Description): Statement {
        requireNonNull(base)
        requireNonNull(description)
        return object : Statement() {
            override fun evaluate() {
                enterRuleContext()
                try {
                    base.evaluate()
                } finally {
                    leaveRuleContext()
                }
                ensureFailureCaught()
            }
        }
    }

    /**
     * A functional interface for [expectFailure] to invoke and capture failures.
     *
     * Java 8+ users should pass a lambda to [expectFailure] rather than directly implement this
     * interface. Java 7+ users can define an `@get:Rule ExpectFailure` instance instead, however if
     * you prefer the [expectFailure] pattern you can use this interface to pass in an anonymous
     * class.
     */
    fun interface StandardSubjectBuilderCallback {
        fun invokeAssertion(whenTesting: StandardSubjectBuilder)
    }

    /**
     * A functional interface for [expectFailureAbout] to invoke and capture failures.
     *
     * Java 8+ users should pass a lambda to [expectFailureAbout] rather than directly implement
     * this interface. Java 7+ users can define an `@get:Rule ExpectFailure` instance instead,
     * however if you prefer the `expectFailureAbout` pattern you can use this interface to pass in
     * an anonymous class.
     */
    fun interface SimpleSubjectBuilderCallback<S : Subject<A>, A> {
        fun invokeAssertion(whenTesting: SimpleSubjectBuilder<S, A>)
    }
}
