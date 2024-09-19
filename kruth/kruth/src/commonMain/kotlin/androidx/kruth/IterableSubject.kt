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

package androidx.kruth

import androidx.kruth.Fact.Companion.fact
import androidx.kruth.Fact.Companion.simpleFact

/**
 * Propositions for [Iterable] subjects.
 *
 * **Note:**
 * - Assertions may iterate through the given [Iterable] more than once. If you have an unusual
 *   implementation of [Iterable] which does not support multiple iterations (sometimes known as a
 *   "one-shot iterable"), you must copy your iterable into a collection which does (e.g.
 *   `iterable.toList()`). If you don't, you may see surprising failures.
 * - Assertions may also require that the elements in the given [Iterable] implement [Any.hashCode]
 *   correctly.
 *
 * @constructor Constructor for use by subclasses. If you want to create an instance of this class
 *   itself, call [check(...)][Subject.check].[that(actual)][StandardSubjectBuilder.that].
 */
// Can't be final since MultisetSubject and SortedSetSubject extend it
open class IterableSubject<T>
protected constructor(
    metadata: FailureMetadata,
    actual: Iterable<T>?,
) : Subject<Iterable<T>>(actual, metadata = metadata, typeDescriptionOverride = null) {

    internal constructor(actual: Iterable<T>?, metadata: FailureMetadata) : this(metadata, actual)

    override fun isEqualTo(expected: Any?) {
        // method contract requires testing iterables for equality
        if (expected == actual) {
            return
        }

        // Fail but with a more descriptive message:
        when {
            (actual is List<*>) && (expected is List<*>) ->
                containsExactlyElementsIn(expected).inOrder()
            (actual is Set<*>) && (expected is Set<*>) -> containsExactlyElementsIn(expected)

            // TODO(b/18430105): Consider a special message if comparing incompatible collection
            // types
            else -> super.isEqualTo(expected)
        }
    }

    /** Fails if the subject is not empty. */
    fun isEmpty() {
        requireNonNull(actual)

        if (!actual.isEmpty()) {
            failWithoutActual(simpleFact("expected to be empty"))
        }
    }

    /** Fails if the subject is empty. */
    fun isNotEmpty() {
        requireNonNull(actual)

        if (actual.isEmpty()) {
            failWithoutActual(simpleFact("expected to be not empty"))
        }
    }

    /** Fails if the subject does not have the given size. */
    fun hasSize(expectedSize: Int) {
        require(expectedSize >= 0) { "expectedSize($expectedSize) must be >= 0" }
        requireNonNull(actual)

        check("count()").that(actual.count()).isEqualTo(expectedSize)
    }

    /** Checks (with a side-effect failure) that the subject contains the supplied item. */
    fun contains(element: Any?) {
        requireNonNull(actual)

        if (element !in actual) {
            val matchingItems = actual.retainMatchingToString(listOf(element))
            if (matchingItems.isNotEmpty()) {
                failWithoutActual(
                    fact("expected to contain", element),
                    fact("an instance of", element.typeName()),
                    simpleFact("but did not"),
                    fact("though it did contain", matchingItems)
                )
            } else {
                failWithActual("expected to contain", element)
            }
        }
    }

    /** Checks (with a side-effect failure) that the subject does not contain the supplied item. */
    fun doesNotContain(element: Any?) {
        requireNonNull(actual)

        if (element in actual) {
            failWithActual("expected not to contain", element)
        }
    }

    /** Checks that the subject does not contain duplicate elements. */
    fun containsNoDuplicates() {
        requireNonNull(actual)

        val duplicates = actual.groupBy { it }.values.filter { it.size > 1 }
        if (duplicates.isNotEmpty()) {
            failWithoutActual(
                simpleFact("expected not to contain duplicates"),
                fact("but contained", duplicates),
                fact("full contents", actualCustomStringRepresentation())
            )
        }
    }

    /** Checks that the subject contains at least one of the provided objects or fails. */
    fun containsAnyOf(first: Any?, second: Any?, vararg rest: Any?) {
        containsAnyIn(listOf(first, second, *rest))
    }

    /**
     * Checks that the subject contains at least one of the objects contained in the provided
     * collection or fails.
     */
    fun containsAnyIn(expected: Iterable<*>?) {
        requireNonNull(expected)
        val actual = requireNonNull(actual).toList()

        if (expected.any { it in actual }) {
            return
        }

        val matchingItems = actual.retainMatchingToString(expected)
        if (matchingItems.isNotEmpty()) {
            failWithoutActual(
                fact("expected to contain any of", expected),
                simpleFact("but did not"),
                fact("though it did contain", matchingItems)
            )
        } else {
            failWithActual("expected to contain any of", expected)
        }
    }

    /**
     * Checks that the subject contains at least one of the objects contained in the provided array
     * or fails.
     */
    fun containsAnyIn(expected: Array<out Any?>?) {
        containsAnyIn(requireNonNull(expected).asList())
    }

    /**
     * Checks that the actual iterable contains at least all of the expected elements or fails. If
     * an element appears more than once in the expected elements to this call then it must appear
     * at least that number of times in the actual elements.
     *
     * To also test that the contents appear in the given order, make a call to `inOrder()` on the
     * object returned by this method. The expected elements must appear in the given order within
     * the actual elements, but they are not required to be consecutive.
     */
    fun containsAtLeast(
        firstExpected: Any?,
        secondExpected: Any?,
        vararg restOfExpected: Any?,
    ): Ordered = containsAtLeastElementsIn(listOf(firstExpected, secondExpected, *restOfExpected))

    /**
     * Checks that the actual iterable contains at least all of the expected elements or fails. If
     * an element appears more than once in the expected elements then it must appear at least that
     * number of times in the actual elements.
     *
     * To also test that the contents appear in the given order, make a call to `inOrder()` on the
     * object returned by this method. The expected elements must appear in the given order within
     * the actual elements, but they are not required to be consecutive.
     */
    fun containsAtLeastElementsIn(expected: Iterable<*>?): Ordered {
        requireNonNull(expected)
        val actualList = requireNonNull(actual).toMutableList()

        val missing = ArrayList<Any?>()
        val actualNotInOrder = ArrayList<Any?>()

        var ordered = true
        // step through the expected elements...
        for (e in expected) {
            val index = actualList.indexOf(e)
            if (index != -1) { // if we find the element in the actual list...
                // drain all the elements that come before that element into actualNotInOrder
                repeat(index) { actualNotInOrder += actualList.removeAt(0) }

                // and remove the element from the actual list
                actualList.removeAt(0)
            } else { // otherwise try removing it from actualNotInOrder...
                if (actualNotInOrder.remove(e)) {
                    // if it was in actualNotInOrder, we're not in order
                    ordered = false
                } else {
                    // if it's not in actualNotInOrder, we're missing an expected element
                    missing.add(e)
                }
            }
        }

        // if we have any missing expected elements, fail
        if (missing.isNotEmpty()) {
            val nearMissing = actualList.retainMatchingToString(missing)

            // TODO(dustinlam): Message is still a bit different from Truth.
            failWithActual(
                fact("missing", missing),
                simpleFact(""),
                fact("though it did contain", nearMissing),
                simpleFact("---"),
                fact("expected to contain at least", expected)
            )
        }

        if (ordered) {
            return NoopOrdered
        }

        return FailingOrdered(metadata) {
            buildString {
                append("required elements were all found, but order was wrong")
                append("expected order for required elements $expected.")

                if (actualList.any { it !in expected }) {
                    append("Actual order: $actualList.")
                }
            }
        }
    }

    /**
     * Checks that the actual iterable contains at least all of the expected elements or fails. If
     * an element appears more than once in the expected elements then it must appear at least that
     * number of times in the actual elements.
     *
     * To also test that the contents appear in the given order, make a call to `inOrder()` on the
     * object returned by this method. The expected elements must appear in the given order within
     * the actual elements, but they are not required to be consecutive.
     */
    fun containsAtLeastElementsIn(expected: Array<out Any?>?): Ordered =
        containsAtLeastElementsIn(expected?.asList())

    /**
     * Checks that a subject contains exactly the provided objects or fails.
     *
     * Multiplicity is respected. For example, an object duplicated exactly 3 times in the
     * parameters asserts that the object must likewise be duplicated exactly 3 times in the
     * subject.
     *
     * To also test that the contents appear in the given order, make a call to [Ordered.inOrder] on
     * the object returned by this method.
     *
     * To test that the iterable contains the same elements as an array, prefer
     * [containsExactlyElementsIn]. It makes clear that the given array is a list of elements, not
     * an element itself. This helps human readers and avoids a compiler warning.
     */
    fun containsExactly(vararg expected: Any?): Ordered =
        containsExactlyElementsIn(expected.asList())

    /**
     * Checks that the subject contains exactly the provided objects or fails.
     *
     * Multiplicity is respected. For example, an object duplicated exactly 3 times in the
     * [Iterable] parameter asserts that the object must likewise be duplicated exactly 3 times in
     * the subject.
     *
     * To also test that the contents appear in the given order, make a call to [Ordered.inOrder] on
     * the object returned by this method.
     */
    fun containsExactlyElementsIn(required: Iterable<*>?): Ordered {
        val actualIter = requireNonNull(actual).iterator()
        val requiredIter = requireNonNull(required).iterator()

        if (!requiredIter.hasNext()) {
            if (actualIter.hasNext()) {
                isEmpty() // fails
            }

            return NoopOrdered
        }

        // Step through both iterators comparing elements pairwise.
        var isFirst = true
        while (actualIter.hasNext() && requiredIter.hasNext()) {
            val actualElement = actualIter.next()
            val requiredElement = requiredIter.next()

            // As soon as we encounter a pair of elements that differ, we know that inOrder()
            // cannot succeed, so we can check the rest of the elements more normally.
            // Since any previous pairs of elements we iterated over were equal, they have no
            // effect on the result now.
            if (actualElement != requiredElement) {
                if (isFirst && !actualIter.hasNext() && !requiredIter.hasNext()) {
                    /*
                     * There's exactly one actual element and exactly one expected element, and they don't
                     * match, so throw a ComparisonFailure. The logical way to do that would be
                     * `check(...).that(actualElement).isEqualTo(requiredElement)`. But isEqualTo has magic
                     * behavior for arrays and primitives, behavior that's inconsistent with how this method
                     * otherwise behaves. For consistency, we want to rely only on the equal() call we've
                     * already made. So we expose a special method for this and call it from here.
                     *
                     * TODO(b/135918662): Consider always throwing ComparisonFailure if there is exactly one
                     *  missing and exactly one extra element, even if there were additional (matching)
                     *  elements. However, this will probably be useful less often, and it will be tricky to
                     *  explain. First, what would we say, "value of: iterable.onlyElementThatDidNotMatch()?"
                     *  And second, it feels weirder to call out a single element when the expected and actual
                     *  values had multiple elements. Granted, Fuzzy Truth already does this, so maybe it's OK?
                     *  But Fuzzy Truth doesn't (yet) make the mismatched value so prominent.
                     */
                    failWithoutActual(
                        simpleFact(
                            "Expected $actualElement to be equal to $requiredElement, but was not"
                        )
                    )
                }

                // Missing elements; elements that are not missing will be removed as we iterate.
                // Missing elements; elements that are not missing will be removed as we iterate.
                val missing = ArrayList<Any?>()
                missing.add(requiredElement)
                missing.addAll(requiredIter.asSequence())

                // Extra elements that the subject had but shouldn't have.
                // Extra elements that the subject had but shouldn't have.
                val extra = ArrayList<T>()

                // Remove all actual elements from missing, and add any that weren't in missing
                // to extra.
                // Remove all actual elements from missing, and add any that weren't in missing
                // to extra.
                if (!missing.remove(actualElement)) {
                    extra.add(actualElement)
                }
                while (actualIter.hasNext()) {
                    val item = actualIter.next()
                    if (!missing.remove(item)) {
                        extra.add(item)
                    }
                }

                if (missing.isEmpty() && extra.isEmpty()) {
                    /*
                     * This containsExactly() call is a success. But the iterables were not in the same order,
                     * so return an object that will fail the test if the user calls inOrder().
                     */
                    return FailingOrdered(metadata) {
                        """
                             Contents match. Expected the order to also match, but was not.
                             Expected: $required.
                             Actual: $actual.
                        """
                            .trimIndent()
                    }
                }

                failWithActual(
                    fact("missing", missing),
                    simpleFact(""),
                    fact("unexpected", extra),
                    simpleFact("---"),
                    fact("expected", required),
                    fact("but was", actual)
                )
            }

            isFirst = false
        }

        // Here, we must have reached the end of one of the iterators without finding any
        // pairs of elements that differ. If the actual iterator still has elements, they're
        // extras. If the required iterator has elements, they're missing elements.
        if (actualIter.hasNext()) {
            failWithoutActual(
                fact("unexpected", actualIter.asSequence().toList()),
                simpleFact("---"),
                fact("expected", required),
                fact("but was", actual)
            )
        }

        if (requiredIter.hasNext()) {
            failWithoutActual(
                fact("missing", requiredIter.asSequence().toList()),
                simpleFact("---"),
                fact("expected", required),
                fact("but was", actual)
            )
        }

        // If neither iterator has elements, we reached the end and the elements were in
        // order, so inOrder() can just succeed.
        return NoopOrdered
    }

    /**
     * Checks that a subject contains exactly the [expected] objects or fails.
     *
     * Multiplicity is respected. For example, an object duplicated exactly 3 times in the array
     * parameter asserts that the object must likewise be duplicated exactly 3 times in the subject.
     *
     * To also test that the contents appear in the given order, make a call to `inOrder()` on the
     * object returned by this method.
     */
    fun containsExactlyElementsIn(expected: Array<out Any?>?): Ordered =
        containsExactlyElementsIn(expected?.asList())

    /**
     * Checks that a actual iterable contains none of the excluded objects or fails. (Duplicates are
     * irrelevant to this test, which fails if any of the actual elements equal any of the excluded)
     */
    fun containsNoneOf(
        firstExcluded: Any?,
        secondExcluded: Any?,
        vararg restOfExcluded: Any?,
    ) {
        containsNoneIn(listOf(firstExcluded, secondExcluded, *restOfExcluded))
    }

    /**
     * Checks that the actual iterable contains none of the elements contained in the [excluded]
     * iterable or fails. (Duplicates are irrelevant to this test, which fails if any of the actual
     * elements equal any of the excluded)
     */
    fun containsNoneIn(excluded: Iterable<*>?) {
        requireNonNull(excluded)
        val actual = requireNonNull(actual).toSet()
        val present = excluded.intersect(actual)

        if (present.isNotEmpty()) {
            failWithActual(
                simpleFact("Expected not to contain any of $excluded but contained $present.")
            )
        }
    }

    /**
     * Checks that the actual iterable contains none of the elements contained in the [excluded]
     * array or fails. (Duplicates are irrelevant to this test, which fails if any of the actual
     * elements equal any of the excluded)
     */
    fun containsNoneIn(excluded: Array<Any?>?) {
        containsNoneIn(excluded?.asList())
    }

    /**
     * Fails if the iterable is not strictly ordered, according to the natural ordering of its
     * elements. Strictly ordered means that each element in the iterable is <i>strictly</i> greater
     * than the element that preceded it.
     *
     * @throws ClassCastException if any pair of elements is not mutually Comparable
     * @throws NullPointerException if any element is null
     */
    open fun isInStrictOrder() {
        isInStrictOrder(compareBy<Comparable<Any>> { it })
    }

    /**
     * Fails if the iterable is not strictly ordered, according to the given [comparator]. Strictly
     * ordered means that each element in the iterable is *strictly* greater than the element that
     * preceded it.
     *
     * Note: star-projection in `Comparator<*>` is for compatibility with Truth.
     *
     * @throws ClassCastException if any pair of elements is not mutually Comparable
     */
    fun isInStrictOrder(comparator: Comparator<*>?) {
        @Suppress("UNCHECKED_CAST") val cmp = requireNonNull(comparator) as Comparator<in Any?>

        verifyInOrder(
            predicate = { a, b -> cmp.compare(a, b) < 0 },
            message = { a, b -> "Expected to be in strict order but contained $a followed by $b." }
        )
    }

    /**
     * Fails if the iterable is not ordered, according to the natural ordering of its elements.
     * Ordered means that each element in the iterable is greater than or equal to the element that
     * preceded it.
     *
     * @throws ClassCastException if any pair of elements is not mutually Comparable
     * @throws NullPointerException if any element is null
     */
    open fun isInOrder() {
        isInOrder(compareBy<Comparable<Any>> { it })
    }

    /**
     * Fails if the iterable is not ordered, according to the given [comparator]. Ordered means that
     * each element in the iterable is greater than or equal to the element that preceded it.
     *
     * @throws ClassCastException if any pair of elements is not mutually Comparable
     */
    fun isInOrder(comparator: Comparator<*>?) {
        @Suppress("UNCHECKED_CAST") val cmp = requireNonNull(comparator) as Comparator<in Any?>

        verifyInOrder(
            predicate = { a, b -> cmp.compare(a, b) <= 0 },
            message = { a, b -> "Expected to be in order but contained $a followed by $b." }
        )
    }

    private inline fun verifyInOrder(
        predicate: (a: Any?, b: Any?) -> Boolean,
        message: (a: Any?, b: Any?) -> String,
    ) {
        requireNonNull(actual).asSequence().zipWithNext(::Pair).forEach { (a, b) ->
            if (!predicate(a, b)) {
                failWithActual(simpleFact(message(a, b)))
            }
        }
    }

    @Deprecated(
        message = "You probably meant to call containsNoneOf instead.",
        replaceWith = ReplaceWith(expression = "containsNoneOf"),
    )
    override fun isNoneOf(first: Any?, second: Any?, vararg rest: Any?) {
        super.isNoneOf(first, second, *rest)
    }

    @Deprecated(
        message = "You probably meant to call containsNoneIn instead.",
        replaceWith = ReplaceWith(expression = "containsNoneIn"),
    )
    override fun isNotIn(iterable: Iterable<*>?) {
        super.isNotIn(iterable)

        requireNonNull(iterable)

        val nonIterables = iterable.filterNot { it is Iterable<*> }
        if (nonIterables.isNotEmpty()) {
            failWithoutActual(
                simpleFact(
                    "The actual value is an Iterable, and you've written a test that compares it " +
                        "to some objects that are not Iterables. Did you instead mean to check " +
                        "whether its *contents* match any of the *contents* of the given values? " +
                        "If so, call containsNoneOf(...)/containsNoneIn(...) instead. " +
                        "Non-iterables: $nonIterables"
                )
            )
        }
    }
}
