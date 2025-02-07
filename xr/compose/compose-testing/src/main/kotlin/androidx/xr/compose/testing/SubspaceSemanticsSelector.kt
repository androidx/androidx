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

package androidx.xr.compose.testing

import androidx.xr.compose.subspace.node.SubspaceSemanticsInfo

/**
 * Projects the given set of nodes to a new set of nodes.
 *
 * @param description Description that is displayed to the developer in error outputs.
 * @param requiresExactlyOneNode Whether this selector should expect to receive exactly 1 node.
 * @param chainedInputSelector Optional selector to apply before this selector gets applied.
 * @param selector The lambda that implements the projection.
 */
internal class SubspaceSemanticsSelector(
    internal val description: String,
    private val requiresExactlyOneNode: Boolean,
    private val chainedInputSelector: SubspaceSemanticsSelector? = null,
    private val selector: (Iterable<SubspaceSemanticsInfo>) -> SubspaceSelectionResult,
) {

    /**
     * Maps the given list of nodes to a new list of nodes.
     *
     * @throws AssertionError if required prerequisites to perform the selection were not satisfied.
     */
    internal fun map(
        nodes: Iterable<SubspaceSemanticsInfo>,
        errorOnFail: String,
    ): SubspaceSelectionResult {
        val chainedResult = chainedInputSelector?.map(nodes, errorOnFail)
        val inputNodes = chainedResult?.selectedNodes ?: nodes
        if (requiresExactlyOneNode && inputNodes.count() != 1) {
            throw AssertionError(
                chainedResult?.customErrorOnNoMatch
                    ?: "Required exactly one node but found ${inputNodes.count()} nodes."
            )
        }
        return selector(inputNodes)
    }
}

/** Creates a new [SubspaceSemanticsSelector] based on the given [SubspaceSemanticsMatcher]. */
internal fun SubspaceSemanticsSelector(
    matcher: SubspaceSemanticsMatcher
): SubspaceSemanticsSelector {
    return SubspaceSemanticsSelector(
        matcher.description,
        requiresExactlyOneNode = false,
        chainedInputSelector = null,
    ) { nodes ->
        SubspaceSelectionResult(nodes.filter { matcher.matches(it) })
    }
}

/**
 * Result of [SubspaceSemanticsSelector] projection.
 *
 * @param selectedNodes The result nodes found.
 * @param customErrorOnNoMatch If the projection failed to map nodes due to wrong input (e.g.
 *   selector expected only 1 node but got multiple) it will provide a custom error exactly
 *   explaining what selection was performed and what nodes it received.
 */
internal class SubspaceSelectionResult(
    internal val selectedNodes: List<SubspaceSemanticsInfo>,
    internal val customErrorOnNoMatch: String? = null,
)

/**
 * Chains the given selector to be performed after this one.
 *
 * The new selector will expect to receive exactly one node (otherwise will fail).
 */
internal fun SubspaceSemanticsSelector.addSelectionFromSingleNode(
    description: String,
    selector: (SubspaceSemanticsInfo) -> List<SubspaceSemanticsInfo>,
): SubspaceSemanticsSelector {
    return SubspaceSemanticsSelector(
        "(${this.description}).$description",
        requiresExactlyOneNode = true,
        chainedInputSelector = this,
    ) { nodes ->
        SubspaceSelectionResult(selector(nodes.first()))
    }
}

/** Chains a new selector that retrieves node from this selector at the given [index]. */
internal fun SubspaceSemanticsSelector.addIndexSelector(index: Int): SubspaceSemanticsSelector {
    return SubspaceSemanticsSelector(
        "(${this.description})[$index]",
        requiresExactlyOneNode = false,
        chainedInputSelector = this,
    ) { nodes ->
        val nodesList = nodes.toList()
        if (index >= 0 && index < nodesList.size) {
            SubspaceSelectionResult(listOf(nodesList[index]))
        } else {
            val errorMessage = "Index out of bounds: $index"
            SubspaceSelectionResult(emptyList(), errorMessage)
        }
    }
}

/** Chains a new selector that retrieves the last node returned from this selector. */
internal fun SubspaceSemanticsSelector.addLastNodeSelector(): SubspaceSemanticsSelector {
    return SubspaceSemanticsSelector(
        "(${this.description}).last",
        requiresExactlyOneNode = false,
        chainedInputSelector = this,
    ) { nodes ->
        SubspaceSelectionResult(nodes.toList().takeLast(1))
    }
}

/**
 * Chains a new selector that selects all the nodes matching the given [matcher] from the nodes
 * returned by this selector.
 */
internal fun SubspaceSemanticsSelector.addSelectorViaMatcher(
    selectorName: String,
    matcher: SubspaceSemanticsMatcher,
): SubspaceSemanticsSelector {
    return SubspaceSemanticsSelector(
        "(${this.description}).$selectorName(${matcher.description})",
        requiresExactlyOneNode = false,
        chainedInputSelector = this,
    ) { nodes ->
        SubspaceSelectionResult(nodes.filter { matcher.matches(it) })
    }
}
