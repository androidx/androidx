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

import androidx.ui.core.LayoutNode
import androidx.ui.semantics.AccessibilityAction
import androidx.ui.semantics.SemanticsPropertyKey
import androidx.ui.util.fastForEach

// TODO(b/142821673): Clean up and integrate this (probably with AndroidComposeView)

/**
 * Owns [SemanticsNode] objects and notifies listeners of changes to the
 * semantics tree
 */
class SemanticsOwner(val rootNode: LayoutNode) {
    /**
     * The root node of the semantics tree.  Does not contain any unmerged data.
     * May contain merged data.
     */
    val rootSemanticsNode: SemanticsNode
        get() {
            return rootNode.outerSemantics!!.semanticsNode()
        }

    private fun <T : Function<Boolean>> getSemanticsActionHandlerForId(
        id: Int,
        action: SemanticsPropertyKey<AccessibilityAction<T>>
    ): AccessibilityAction<*>? {
        var result: SemanticsNode? = rootSemanticsNode.findChildById(id)
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
}

/**
 * Finds all [SemanticsNode]s in the tree owned by this [SemanticsOwner]. Return the results in a
 * list.
 */
fun SemanticsOwner.getAllSemanticsNodes(): List<SemanticsNode> {
    return getAllSemanticsNodesToMap().values.toList()
}

/**
 * Finds all [SemanticsNode]s in the tree owned by this [SemanticsOwner]. Return the results in a
 * map.
 */
internal fun SemanticsOwner.getAllSemanticsNodesToMap(): Map<Int, SemanticsNode> {
    val nodes = mutableMapOf<Int, SemanticsNode>()

    fun findAllSemanticNodesRecursive(currentNode: SemanticsNode) {
        nodes[currentNode.id] = currentNode
        currentNode.children.fastForEach { child ->
            findAllSemanticNodesRecursive(child)
        }
    }

    findAllSemanticNodesRecursive(rootSemanticsNode)
    return nodes
}
