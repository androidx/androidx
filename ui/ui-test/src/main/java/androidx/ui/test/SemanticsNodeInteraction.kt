/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.test

import androidx.ui.core.semantics.SemanticsNode

internal fun SemanticsNodeInteraction(
    node: SemanticsNode,
    semanticsTreeInteraction: SemanticsTreeInteraction
): SemanticsNodeInteraction {
    return SemanticsNodeInteraction(listOf(node), semanticsTreeInteraction)
}

/**
 * Represents a component with which one can interact with the hierarchy.
 * Examples of interactions include [findByTag], [isToggleable], [assertIsOn], [doClick]
 *
 * Example usage:
 * findByTag("myCheckbox")
 *    .doClick()
 *    .assertIsOn()
 */
class SemanticsNodeInteraction internal constructor(
    nodes: List<SemanticsNode>,
    internal val semanticsTreeInteraction: SemanticsTreeInteraction
) {
    private val nodeIds: List<Int> = nodes.map { it.id }.toList()

    /**
     * Anytime we refresh semantics we capture it here. This is then presented to the user in case
     * their tests fails deu to a missing node. This helps to see what was the last state of the
     * node before it disappeared. We dump it to string because trying to dump the node later can
     * result in failure as it gets detached from its layout.
     */
    private var lastSeenSemantics: String? = nodes.firstOrNull()?.toStringInfo()

    internal fun fetchSemanticsNodes(): List<SemanticsNode> {
        return semanticsTreeInteraction.getNodesByIds(nodeIds)
    }

    /**
     * Returns the semantics node captured by this object.
     *
     * Note: Accessing this object involves synchronization with your UI. If you are accessing this
     * multiple times in one atomic operation, it is better to cache the result instead of calling
     * this API multiple times.
     *
     * This will fail if there is 0 or multiple nodes matching.
     *
     * @throws AssertionError if 0 or multiple nodes found.
     */
    fun fetchSemanticsNode(errorMessageOnFail: String? = null): SemanticsNode {
        return fetchOneOrDie(errorMessageOnFail)
    }

    /**
     * Asserts that no item was found or that the item is no longer in the hierarchy.
     *
     * This will synchronize with the UI and fetch all the nodes again to ensure it has latest data.
     *
     * @throws [AssertionError] if the assert fails.
     */
    fun assertDoesNotExist() {
        val nodes = fetchSemanticsNodes()
        if (nodes.isNotEmpty()) {
            throw AssertionError(buildErrorMessageForCountMismatch(
                errorMessage = "Failed: assertDoesNotExist.",
                selector = semanticsTreeInteraction.selector,
                foundNodes = nodes,
                expectedCount = 0
            ))
        }
    }

    /**
     * Asserts that the component was found and is part of the component tree.
     *
     * This will synchronize with the UI and fetch all the nodes again to ensure it has latest data.
     * If you are using [fetchSemanticsNode] you don't need to call this. In fact you would just
     * introduce additional overhead.
     *
     * @param errorMessageOnFail Error message prefix to be added to the message in case this
     * asserts fails. This is typically used by operations that rely on this assert. Example prefix
     * could be: "Failed to perform doOnClick.".
     *
     * @throws [AssertionError] if the assert fails.
     */
    fun assertExists(errorMessageOnFail: String? = null): SemanticsNodeInteraction {
        fetchOneOrDie(errorMessageOnFail)
        return this
    }

    private fun fetchOneOrDie(errorMessageOnFail: String? = null): SemanticsNode {
        val nodes = fetchSemanticsNodes()

        if (nodes.size != 1) {
            val finalErrorMessage = errorMessageOnFail
                ?: "Failed: assertExists."

            if (nodes.isEmpty() && lastSeenSemantics != null) {
                // This means that node we used to have is no longer in the tree.
                throw AssertionError(buildErrorMessageForNodeMissingInTree(
                    errorMessage = finalErrorMessage,
                    selector = semanticsTreeInteraction.selector,
                    lastSeenSemantics = lastSeenSemantics!!
                ))
            }

            throw AssertionError(buildErrorMessageForCountMismatch(
                errorMessage = finalErrorMessage,
                foundNodes = nodes,
                expectedCount = 1,
                selector = semanticsTreeInteraction.selector
            ))
        }

        lastSeenSemantics = nodes.first().toStringInfo()
        return nodes.first()
    }
}