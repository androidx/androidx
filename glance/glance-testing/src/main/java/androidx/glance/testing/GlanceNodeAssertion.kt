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
import androidx.annotation.RestrictTo.Scope

/**
 * Represents a Glance node from the tree that can be asserted on.
 *
 * An instance of [GlanceNodeAssertion] can be obtained from {@code onNode} and equivalent methods
 * on a GlanceNodeAssertionProvider
 */
class GlanceNodeAssertion<R, T : GlanceNode<R>> @RestrictTo(Scope.LIBRARY_GROUP) constructor(
    private val matcher: GlanceNodeMatcher<R>,
    private val testContext: TestContext<R, T>,
) {
    /**
     * Asserts that the node was found.

     * @throws [AssertionError] if the assert fails.
     */
    fun assertExists(): GlanceNodeAssertion<R, T> {
        findSingleMatchingNode(finalErrorMessageOnFail = "Failed assertExists")
        return this
    }

    /**
     * Asserts that no matching node was found.

     * @throws [AssertionError] if the assert fails.
     */
    fun assertDoesNotExist(): GlanceNodeAssertion<R, T> {
        val matchedNodesCount = findMatchingNodes().size
        if (matchedNodesCount != 0) {
            throw AssertionError(
                buildErrorMessageForCountMismatch(
                    errorMessage = "Failed assertDoesNotExist",
                    matcherDescription = matcher.description,
                    expectedCount = 0,
                    actualCount = matchedNodesCount
                )
            )
        }
        return this
    }

    /**
     * Asserts that the provided [matcher] is satisfied for this node.
     *
     * <p> This function also can be used to create convenience "assert{somethingConcrete}"
     * methods as extension functions on the GlanceNodeAssertion.
     *
     * @param matcher Matcher to verify.
     * @param messagePrefixOnError Prefix to be put in front of an error that gets thrown in case
     * this assert fails. This can be helpful in situations where this assert fails as part of a
     * bigger operation that used this assert as a precondition check.
     *
     * @throws AssertionError if the matcher does not match or the node can no longer be found.
     */
    fun assert(
        matcher: GlanceNodeMatcher<R>,
        messagePrefixOnError: (() -> String)? = null
    ): GlanceNodeAssertion<R, T> {
        var errorMessageOnFail = "Failed to assert condition: (${matcher.description})"
        if (messagePrefixOnError != null) {
            errorMessageOnFail = messagePrefixOnError() + "\n" + errorMessageOnFail
        }
        val glanceNode = findSingleMatchingNode(errorMessageOnFail)

        if (!matcher.matches(glanceNode)) {
            throw AssertionError(
                buildGeneralErrorMessage(
                    errorMessageOnFail,
                    glanceNode
                )
            )
        }
        return this
    }

    private fun findSingleMatchingNode(finalErrorMessageOnFail: String): GlanceNode<R> {
        val matchingNodes = findMatchingNodes()
        val matchedNodesCount = matchingNodes.size
        if (matchedNodesCount != 1) {
            throw AssertionError(
                buildErrorMessageForCountMismatch(
                    finalErrorMessageOnFail,
                    matcher.description,
                    expectedCount = 1,
                    actualCount = matchedNodesCount
                )
            )
        }
        return matchingNodes.single()
    }

    private fun findMatchingNodes(): List<GlanceNode<R>> {
        if (testContext.cachedMatchedNodes.isEmpty()) {
            val rootGlanceNode =
                checkNotNull(testContext.rootGlanceNode) { "No root GlanceNode found." }
            testContext.cachedMatchedNodes = findMatchingNodes(rootGlanceNode)
        }
        return testContext.cachedMatchedNodes
    }

    private fun findMatchingNodes(node: GlanceNode<R>): List<GlanceNode<R>> {
        val matching = mutableListOf<GlanceNode<R>>()
        if (matcher.matches(node)) {
            matching.add(node)
        }
        for (child in node.children()) {
            matching.addAll(findMatchingNodes(child))
        }
        return matching.toList()
    }
}
