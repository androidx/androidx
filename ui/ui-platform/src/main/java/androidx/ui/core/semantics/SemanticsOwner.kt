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

package androidx.ui.core.semantics

import androidx.ui.core.ComponentNode
import androidx.ui.semantics.AccessibilityAction
import androidx.ui.semantics.SemanticsPropertyKey

// TODO(b/142821673): Clean up and integrate this (probably with AndroidComposeView)

/**
 * Owns [SemanticsNode] objects and notifies listeners of changes to the
 * semantics tree
 */
class SemanticsOwner(rootNode: ComponentNode) {
    private val dirtyNodes: MutableSet<SemanticsNode> = mutableSetOf()
    private val nodes: MutableMap<Int, SemanticsNode> = mutableMapOf()
    private val detachedNodes: MutableSet<SemanticsNode> = mutableSetOf()

    /**
     * Should *only* be called by [SemanticsNode.attach]
     */
    internal fun onAttach(node: SemanticsNode) {
        // TODO: b/150777826 - fix and re-enable assertion
        // check(node.id !in nodes)
        nodes[node.id] = node
        detachedNodes.remove(node)
    }

    /**
     * Should *only* be called by [SemanticsNode.markDirty]
     */
    internal fun onNodeMarkedDirty(node: SemanticsNode) {
        // TODO: b/150777826 - fix and re-enable assertion
        // check(node !in detachedNodes)
        dirtyNodes.add(node)
    }

    /**
     * Should *only* be called by [SemanticsNode.detach]
     */
    internal fun onDetach(node: SemanticsNode) {
        check(nodes.containsKey(node.id))
        // TODO: b/150777826 - fix and re-enable assertion
        // check(!detachedNodes.contains(node))
        nodes.remove(node.id)
        detachedNodes.add(node)
    }

    /**
     * The root node of the semantics tree.  Does not contain any unmerged data.
     * May contain merged data.
     */
    val rootSemanticsNode: SemanticsNode = SemanticsNode.root(
        this,
        SemanticsConfiguration().also { it.isSemanticBoundary = true },
        rootNode
    )

    private fun dispose() {
        dirtyNodes.clear()
        nodes.clear()
        detachedNodes.clear()
    }

    private fun <T : Function<Unit>> getSemanticsActionHandlerForId(
        id: Int,
        action: SemanticsPropertyKey<AccessibilityAction<T>>
    ): AccessibilityAction<*>? {
        var result: SemanticsNode? = nodes[id]
        if (result != null && !result.canPerformAction(action)) {
            result.visitDescendants { node: SemanticsNode ->
                if (node.canPerformAction(action)) {
                    result = node
                    return@visitDescendants false // found node, abort walk
                }
                return@visitDescendants true // continue walk
            }
        }
        if (result?.canPerformAction(action) != true) {
            return null
        }
        return result!!.unmergedConfig.getOrNull(action)
    }

    internal fun invalidateSemanticsRoot() {
        rootSemanticsNode.invalidateChildren()
    }
}