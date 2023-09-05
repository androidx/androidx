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

package androidx.glance.testing

import androidx.annotation.RestrictTo

/**
 * Represents a collection of Glance nodes from the tree that can be asserted on.
 *
 * An instance of [GlanceNodeAssertionCollection] can be obtained from
 * [GlanceNodeAssertionsProvider.onAllNodes] and equivalent methods.
 */
// Equivalent to SemanticsNodeInteractionCollection in compose.
class GlanceNodeAssertionCollection<R, T : GlanceNode<R>>
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) constructor(
    private val testContext: TestContext<R, T>,
    private val selector: GlanceNodeSelector<R>
) {
    /**
     * Asserts that this collection of nodes is equal to the given [expectedCount].
     *
     * @throws AssertionError if the size is not equal to [expectedCount]
     */
    fun assertCountEquals(
        expectedCount: Int
    ): GlanceNodeAssertionCollection<R, T> {
        val errorMessageOnFail = "Failed to assert count of nodes"

        val actualCount = testContext.findMatchingNodes(selector, errorMessageOnFail).size
        if (actualCount != expectedCount) {
            throw AssertionError(
                buildErrorMessageWithReason(
                    errorMessageOnFail = errorMessageOnFail,
                    reason = buildErrorReasonForCountMismatch(
                        matcherDescription = selector.description,
                        expectedCount = expectedCount,
                        actualCount = actualCount
                    )
                )
            )
        }
        return this
    }

    /**
     * Asserts that all the nodes in this collection satisfy the given [matcher].
     *
     * Doesn't throw error if the collection is empty. Use [assertCountEquals] to assert on expected
     * size of the collection.
     *
     * @param matcher Matcher that has to be satisfied by all the nodes in the collection.
     * @throws AssertionError if the collection contains at least one element that does not satisfy
     * the given matcher.
     */
    fun assertAll(
        matcher: GlanceNodeMatcher<R>,
    ): GlanceNodeAssertionCollection<R, T> {
        val errorMessageOnFail = "Failed to assertAll(${matcher.description})"

        val filteredNodes = testContext.findMatchingNodes(selector, errorMessageOnFail)
        val violations = filteredNodes.filter {
            !matcher.matches(it)
        }
        if (violations.isNotEmpty()) {
            throw AssertionError(buildGeneralErrorMessage(errorMessageOnFail, violations))
        }
        return this
    }

    /**
     * Asserts that this collection contains at least one element that satisfies the given
     * [matcher].
     *
     * @param matcher Matcher that has to be satisfied by at least one of the nodes in the
     * collection.
     * @throws AssertionError if not at least one matching node was found.
     */
    fun assertAny(
        matcher: GlanceNodeMatcher<R>,
    ): GlanceNodeAssertionCollection<R, T> {
        val errorMessageOnFail = "Failed to assertAny(${matcher.description})"
        val filteredNodes = testContext.findMatchingNodes(selector, errorMessageOnFail)

        if (filteredNodes.isEmpty()) {
            throw AssertionError(
                buildErrorMessageWithReason(
                    errorMessageOnFail = errorMessageOnFail,
                    reason = buildErrorReasonForAtLeastOneNodeExpected(selector.description)
                )
            )
        }

        if (!matcher.matchesAny(filteredNodes)) {
            throw AssertionError(buildGeneralErrorMessage(errorMessageOnFail, filteredNodes))
        }
        return this
    }

    /**
     * Returns a [GlanceNodeAssertion] that can assert on the node at the given index of this
     * collection.
     *
     * Any subsequent assertion on its result will throw error if index is out of bounds of the
     * matching nodes found from previous operations.
     */
    operator fun get(index: Int): GlanceNodeAssertion<R, T> {
        return GlanceNodeAssertion(
            testContext = testContext,
            selector = selector.addIndexedSelector(index)
        )
    }

    /**
     * Returns a new collection of nodes by filtering the given nodes using the provided [matcher].
     */
    fun filter(matcher: GlanceNodeMatcher<R>): GlanceNodeAssertionCollection<R, T> {
        return GlanceNodeAssertionCollection(
            testContext,
            selector.addMatcherSelector(
                selectorName = "filter",
                matcher = matcher
            )
        )
    }
}
