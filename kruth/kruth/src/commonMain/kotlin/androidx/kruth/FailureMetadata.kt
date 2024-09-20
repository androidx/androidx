/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.kruth.Fact.Companion.fact
import androidx.kruth.Fact.Companion.simpleFact
import androidx.kruth.Step.CheckStep
import androidx.kruth.Step.SubjectStep
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * An opaque, immutable object containing state from the previous calls in the fluent assertion
 * chain. It appears primarily as a parameter to [Subject] constructors (and [Subject.Factory]
 * methods), which should pass it to the superclass constructor and not otherwise use or store it.
 * In particular, users should not attempt to call [Subject] constructors or [Subject.Factory]
 * methods directly. Instead, they should use the appropriate factory method:
 * * If you're writing a test: Truth#assertAbout(Subject.Factory)}`.that(...)`
 * * If you're creating a derived subject from within another subject:
 *   `check(...).about(...).that(...)`
 * * If you're testing your subject to verify that assertions fail when they should: [ExpectFailure]
 *
 * (One exception: Implementations of [CustomSubjectBuilder] do directly call constructors, using
 * their [CustomSubjectBuilder.metadata] method to get an instance to pass to the constructor.)
 */
@OptIn(ExperimentalContracts::class)
class FailureMetadata
internal constructor(
    private val failureStrategy: FailureStrategy = FailureStrategy { failure -> throw failure },
    // TODO(dustinlam): In Google Truth, messages are lazily evaluated.
    private val messagesToPrepend: List<String> = emptyList(),
    private val steps: List<Step> = emptyList()
) {

    /**
     * Returns a new instance that includes the given subject in its chain of values. Truth users do
     * not need to call this method directly; Truth automatically accumulates context, starting from
     * the initial that(...) call and continuing into any chained calls, like
     * [ThrowableSubject.hasMessageThat].
     */
    internal fun updateForSubject(subject: Subject<*>): FailureMetadata {
        return FailureMetadata(
            failureStrategy = failureStrategy,
            messagesToPrepend = messagesToPrepend,
            steps = steps + SubjectStep(subject)
        )
    }

    internal fun updateForCheckCall(): FailureMetadata {
        return FailureMetadata(
            failureStrategy = failureStrategy,
            messagesToPrepend = messagesToPrepend,
            steps = steps + CheckStep(null, null)
        )
    }

    internal fun updateForCheckCall(
        valuesAreSimilar: OldAndNewValuesAreSimilar,
        descriptionUpdate: (String?) -> String
    ): FailureMetadata {
        return FailureMetadata(
            failureStrategy = failureStrategy,
            messagesToPrepend = messagesToPrepend,
            steps = steps + CheckStep(valuesAreSimilar, descriptionUpdate)
        )
    }

    internal fun fail(vararg facts: Fact) {
        fail(facts.asList())
    }

    // TODO: change to AssertionError that takes in a cause when upgraded to 1.9.20.
    //  Take care to also update the places we check for AssertionErrorWithFacts for error message
    //  formatting.
    internal fun fail(facts: List<Fact> = emptyList()) {
        failureStrategy.fail(
            AssertionErrorWithFacts(
                    messagesToPrepend = messagesToPrepend,
                    // TODO(dustinlam): We should sometimes be calling into failEqualityCheck, which
                    // has
                    //  different formatting for ComparisonFailures.
                    facts = description() + facts + rootUnlessThrowable(),
                    cause = rootCause()
                )
                .also(AssertionErrorWithFacts::cleanStackTrace)
        )
    }

    /**
     * @param message A message to append to a list of messages stored in this [FailureMetadata],
     *   which are prepended to the list of [Fact] when reporting a failure via [fail].
     */
    internal fun withMessage(message: String?): FailureMetadata =
        FailureMetadata(
            failureStrategy = failureStrategy,
            messagesToPrepend = messagesToPrepend + (message ?: "null"),
            steps = steps
        )

    /**
     * Returns a description of the final actual value, if it appears "interesting" enough to show.
     * The description is considered interesting if the chain of derived subjects ends with at least
     * one derivation that we have a name for. It's also considered interesting in the absence of
     * derived subjects if we inferred a name for the root actual value from the bytecode.
     *
     * We don't want to say: "value of string: expected \[foo\] but was \[bar\]" (OK, we might still
     * decide to say this, but for now, we don't.)
     *
     * We do want to say: "value of throwable.getMessage(): expected \[foo\] but was \[bar\]"
     *
     * We also want to say: "value of getLogMessages(): expected not to be empty"
     *
     * To support that, `descriptionIsInteresting` tracks whether we've been given context through
     * `check` calls _that include names_ or, initially, whether we inferred a name for the root
     * actual value from the bytecode.
     *
     * If we're missing a naming function halfway through, we have to reset: We don't want to claim
     * that the value is "foo.bar.baz" when it's "foo.bar.somethingelse.baz." We have to go back to
     * "object.baz." (But note that [rootUnlessThrowable] will still provide the value of the root
     * foo to the user as long as we had at least one naming function: We might not know the root's
     * exact relationship to the final object, but we know it's some object "different enough" to be
     * worth displaying.)
     */
    private fun description(): List<Fact> {
        var description: String? = null
        var descriptionIsInteresting = false
        for (step in steps) {
            if (step is CheckStep) {
                if (step.descriptionUpdate == null) {
                    description = null
                    descriptionIsInteresting = false
                } else {
                    description = step.descriptionUpdate.invoke(description)
                    descriptionIsInteresting = true
                }
                continue
            }

            if (description == null) {
                require(step is SubjectStep)
                description = step.subject.typeDescription()
            }
        }

        return if (descriptionIsInteresting) {
            listOf(fact("value of", description))
        } else {
            emptyList()
        }
    }

    /**
     * Returns the root actual value, if we know it's "different enough" from the final actual
     * value.
     *
     * We don't want to say: "expected \[foo\] but was \[bar\]. string: \[bar\]"
     *
     * We do want to say: "expected \[foo\] but was \[bar\]. myObject: MyObject\[string=bar, i=0\]"
     *
     * To support that, `seenDerivation` tracks whether we've seen multiple actual values, which is
     * equivalent to whether we've seen multiple Subject instances or, more informally, whether the
     * user is making a chained assertion.
     *
     * There's one wrinkle: Sometimes chaining doesn't add information. This is often true with
     * "internal" chaining, like when StreamSubject internally creates an IterableSubject to
     * delegate to. The two subjects' string representations will be identical (or, in some cases,
     * _almost_ identical), so there is no value in showing both. In such cases, implementations can
     * call the no-arg `checkNoNeedToDisplayBothValues()`, which sets `valuesAreSimilar`,
     * instructing this method that that particular chain link "doesn't count." (Note also that
     * there are some edge cases that we're not sure how to handle yet, for which we might introduce
     * additional `check`-like methods someday.)
     */
    // TODO(b/134505914): Consider returning multiple facts in some cases.
    private fun rootUnlessThrowable(): List<Fact> {
        var rootSubject: Step? = null
        var seenDerivation = false
        for (step in steps) {
            if (step is CheckStep) {
                // If we don't have a description update, don't trigger display of a root object.
                // (If we did, we'd change the messages of a bunch of existing subjects, and we
                // don't want to bite that off yet.)
                //
                // If we do have a description update, then trigger display of a root object but
                // only if the old and new values are "different enough" to be worth both
                // displaying.
                seenDerivation =
                    seenDerivation ||
                        (step.descriptionUpdate != null &&
                            step.valuesAreSimilar == OldAndNewValuesAreSimilar.DIFFERENT)
                continue
            }

            if (rootSubject == null) {
                require(step is SubjectStep)
                if (step.subject.actual is Throwable) {
                    // We'll already include the Throwable as a cause of the AssertionError
                    // (see rootCause()), so we don't need to include it again in the message.
                    return emptyList()
                }
                rootSubject = step
            }
        }

        /*
         * TODO(cpovirk): Maybe say "root foo was: ..." instead of just "foo was: ..." if there's
         *  more than one foo in the chain, if the description string doesn't start with "foo,"
         *  and/or if the name we have is just "object?"
         */
        return if (seenDerivation) {
            requireNonNull(rootSubject)
            require(rootSubject is SubjectStep)
            listOf(
                fact(
                    // TODO(dustinlam): Value should be .actualCustomStringRepresentation()
                    "${rootSubject.subject.typeDescription()} was",
                    rootSubject.subject.actual
                )
            )
        } else {
            emptyList()
        }
    }

    /**
     * Asserts that the specified value is `true`.
     *
     * @param message the message to report if the assertion fails.
     */
    internal inline fun assertTrue(actual: Boolean, message: () -> String) {
        contract { returns() implies actual }

        if (!actual) {
            fail(simpleFact(message()))
        }
    }

    /**
     * Asserts that the specified value is `false`.
     *
     * @param message the message to report if the assertion fails.
     */
    internal inline fun assertFalse(actual: Boolean, message: () -> Fact) {
        contract { returns() implies !actual }

        if (actual) {
            fail(message())
        }
    }

    /**
     * Asserts that the specified values are equal.
     *
     * @param message the message to report if the assertion fails.
     */
    internal inline fun assertEquals(
        expected: Any?,
        actual: Any?,
        message: () -> String,
    ) {
        assertTrue(expected == actual, message)
    }

    /**
     * Asserts that the specified values are not equal.
     *
     * @param message the message to report if the assertion fails.
     */
    internal fun assertNotEquals(illegal: Any?, actual: Any?, message: () -> Fact) {
        assertFalse(illegal == actual, message)
    }

    /**
     * Asserts that the specified value is `null`.
     *
     * @param message the message to report if the assertion fails.
     */
    internal fun assertNull(actual: Any?, message: () -> String) {
        contract { returns() implies (actual == null) }
        assertTrue(actual == null, message)
    }

    /**
     * Asserts that the specified value is not `null`.
     *
     * @param message the message to report if the assertion fails.
     */
    internal fun <T : Any> assertNotNull(actual: T?, message: () -> Fact): T {
        contract { returns() implies (actual != null) }
        assertFalse(actual == null, message)

        return actual
    }

    /**
     * Returns the first [Throwable] in the chain of actual values. Typically, we'll have a root
     * cause only if the assertion chain contains a [ThrowableSubject].
     */
    private fun rootCause(): Throwable? {
        return steps.firstNotNullOfOrNull { step ->
            (step as? SubjectStep)?.subject?.actual as? Throwable
        }
    }
}

/**
 * Whether the value of the original subject and the value of the derived subject are "similar
 * enough" that we don't need to display both. For example, if we're printing a message about the
 * value of optional.get(), there's no need to print the optional itself because it adds no
 * information. Similarly, if we're printing a message about the asList() view of an array, there's
 * no need to also print the array.
 */
internal enum class OldAndNewValuesAreSimilar {
    SIMILAR,
    DIFFERENT
}

/** The data from a call to either (a) a [Subject] constructor or (b) [Subject.check]. */
internal sealed class Step {

    internal class SubjectStep(
        /**
         * We store Subject, rather than the actual value itself, so that we can call
         * actualCustomStringRepresentation(). Why not call actualCustomStringRepresentation()
         * immediately? First, it might be expensive, and second, the Subject isn't initialized at
         * the time we receive it. We *might* be able to make it safe to call if it looks only at
         * actual(), but it might try to look at facts initialized by a subclass, which aren't ready
         * yet.
         */
        val subject: Subject<*>
    ) : Step()

    internal class CheckStep(
        val valuesAreSimilar: OldAndNewValuesAreSimilar?,
        val descriptionUpdate: ((String?) -> String)?
    ) : Step()
}
