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
 * A chainable selector that allows specifying how to select nodes from a collection.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class GlanceNodeSelector<R>(
    val description: String,
    private val previousChainedSelector: GlanceNodeSelector<R>? = null,
    private val selector: (Iterable<GlanceNode<R>>) -> SelectionResult<R>
) {

    /**
     * Returns nodes selected by previous chained selectors followed by the current selector.
     */
    fun map(nodes: Iterable<GlanceNode<R>>): SelectionResult<R> {
        val previousSelectionResult = previousChainedSelector?.map(nodes)
        val inputNodes = previousSelectionResult?.selectedNodes ?: nodes
        return selector(inputNodes)
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class SelectionResult<R>(
    val selectedNodes: List<GlanceNode<R>>,
    val errorMessageOnNoMatch: String? = null
)

/**
 * Constructs an entry-point selector that selects nodes satisfying the matcher condition. Used at
 * the entry points such as [GlanceNodeAssertionsProvider.onNode] and
 * [GlanceNodeAssertionsProvider.onAllNodes] where there is no previous chained selector.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <R> GlanceNodeMatcher<R>.matcherToSelector(): GlanceNodeSelector<R> {
    return GlanceNodeSelector(
        description = description,
        previousChainedSelector = null
    ) { glanceNodes ->
        SelectionResult(
            selectedNodes = glanceNodes.filter { matches(it) }
        )
    }
}

/**
 * Wraps the current selector with a chained selector that selects a node at a given index from the
 * the result of current selection.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <R> GlanceNodeSelector<R>.addIndexedSelector(index: Int): GlanceNodeSelector<R> {
    return GlanceNodeSelector(
        description = "(${this.description})[$index]",
        previousChainedSelector = this
    ) { nodes ->
        val nodesList = nodes.toList()
        val minimumExpectedCount = index + 1
        if (index >= 0 && index < nodesList.size) {
            SelectionResult(
                selectedNodes = listOf(nodesList[index])
            )
        } else {
            SelectionResult(
                selectedNodes = emptyList(),
                errorMessageOnNoMatch = buildErrorReasonForIndexOutOfMatchedNodeBounds(
                    description,
                    requestedIndex = minimumExpectedCount,
                    actualCount = nodesList.size
                )
            )
        }
    }
}

/**
 * Wraps the current selector with a chained matcher-based selector that filters the list of nodes
 * to return ones matched by the matcher.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <R> GlanceNodeSelector<R>.addMatcherSelector(
    selectorName: String,
    matcher: GlanceNodeMatcher<R>
): GlanceNodeSelector<R> {
    return GlanceNodeSelector(
        description = "(${this.description}).$selectorName(${matcher.description})",
        previousChainedSelector = this
    ) { nodes ->
        SelectionResult(
            selectedNodes = nodes.filter { matcher.matches(it) }
        )
    }
}

/**
 * Wraps the current selector with a chained matcher-based selector that ensures only one node is
 * returned by current selector and selects children of that node.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <R> GlanceNodeSelector<R>.addChildrenSelector(): GlanceNodeSelector<R> {
    return GlanceNodeSelector(
        description = "($description).children()",
        previousChainedSelector = this
    ) { nodes ->
        if (nodes.count() != 1) {
            SelectionResult(
                selectedNodes = emptyList(),
                errorMessageOnNoMatch = buildErrorReasonForCountMismatch(
                    matcherDescription = description,
                    expectedCount = 1,
                    actualCount = nodes.count()
                )
            )
        } else {
            SelectionResult(
                selectedNodes = nodes.single().children()
            )
        }
    }
}
