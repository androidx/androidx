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
 * A context object that holds glance node tree being inspected as well as any state cached
 * across the chain of assertions.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class TestContext<R, T : GlanceNode<R>> {
    // e.g. RemoteViewsRoot
    var rootGlanceNode: T? = null
    private var allNodes: List<GlanceNode<R>> = emptyList()

    /**
     * Returns all nodes in single flat list (either from cache or by traversing the hierarchy from
     * root glance node).
     */
    private fun getAllNodes(): List<GlanceNode<R>> {
        val rootGlanceNode =
            checkNotNull(rootGlanceNode) { "No root GlanceNode found." }
        if (this.allNodes.isEmpty()) {
            val allNodes = mutableListOf<GlanceNode<R>>()

            fun collectAllNodesRecursive(currentNode: GlanceNode<R>) {
                allNodes.add(currentNode)
                val children = currentNode.children()
                for (index in children.indices) {
                    collectAllNodesRecursive(children[index])
                }
            }

            collectAllNodesRecursive(rootGlanceNode)
            this.allNodes = allNodes.toList()
        }

        return this.allNodes
    }

    /**
     * Finds nodes matching the given selector from the list of all nodes in the hierarchy.
     *
     * @throws AssertionError if provided selector results in an error due to no match.
     */
    fun findMatchingNodes(
        selector: GlanceNodeSelector<R>,
        errorMessageOnFail: String
    ): List<GlanceNode<R>> {
        val allNodes = getAllNodes()
        val selectionResult = selector.map(allNodes)

        if (selectionResult.errorMessageOnNoMatch != null) {
            throw AssertionError(
                buildErrorMessageWithReason(
                    errorMessageOnFail = errorMessageOnFail,
                    reason = selectionResult.errorMessageOnNoMatch
                )
            )
        }

        return selectionResult.selectedNodes
    }

    /**
     * Returns true if root has glance nodes after composition to be able to perform assertions on.
     *
     * Can be false if either composable function produced no glance elements or composable function
     * was not provided..
     */
    fun hasNodes(): Boolean {
        return rootGlanceNode?.children()?.isNotEmpty() ?: false
    }
}
