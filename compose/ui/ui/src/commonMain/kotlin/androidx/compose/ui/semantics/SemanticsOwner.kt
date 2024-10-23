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

package androidx.compose.ui.semantics

import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.util.fastForEach

/** Owns [SemanticsNode] objects and notifies listeners of changes to the semantics tree */
class SemanticsOwner
internal constructor(
    private val rootNode: LayoutNode,
    private val outerSemanticsNode: EmptySemanticsModifier
) {
    /**
     * The root node of the semantics tree. Does not contain any unmerged data. May contain merged
     * data.
     */
    val rootSemanticsNode: SemanticsNode
        get() {
            return SemanticsNode(rootNode, mergingEnabled = true)
        }

    val unmergedRootSemanticsNode: SemanticsNode
        get() {
            return SemanticsNode(
                outerSemanticsNode = outerSemanticsNode,
                layoutNode = rootNode,
                mergingEnabled = false,
                // Forcing an empty SemanticsConfiguration here since the root node will always
                // have an empty config, but if we don't pass this in explicitly here it will try
                // to call `rootNode.collapsedSemantics` which will fail because the LayoutNode
                // is not yet attached when this getter is first called.
                unmergedConfig = SemanticsConfiguration()
            )
        }
}

/**
 * Finds all [SemanticsNode]s in the tree owned by this [SemanticsOwner]. Return the results in a
 * list.
 *
 * @param mergingEnabled set to true if you want the data to be merged.
 * @param skipDeactivatedNodes set to false if you want to collect the nodes which are deactivated.
 *   For example, the children of [androidx.compose.ui.layout.SubcomposeLayout] which are retained
 *   to be reused in future are considered deactivated.
 */
fun SemanticsOwner.getAllSemanticsNodes(
    mergingEnabled: Boolean,
    skipDeactivatedNodes: Boolean = true
): List<SemanticsNode> {
    return getAllSemanticsNodesToMap(
            useUnmergedTree = !mergingEnabled,
            skipDeactivatedNodes = skipDeactivatedNodes
        )
        .values
        .toList()
}

@Deprecated(message = "Use a new overload instead", level = DeprecationLevel.HIDDEN)
fun SemanticsOwner.getAllSemanticsNodes(mergingEnabled: Boolean) =
    getAllSemanticsNodes(mergingEnabled, true)

/**
 * Finds all [SemanticsNode]s in the tree owned by this [SemanticsOwner]. Return the results in a
 * map.
 */
internal fun SemanticsOwner.getAllSemanticsNodesToMap(
    useUnmergedTree: Boolean = false,
    skipDeactivatedNodes: Boolean = true
): Map<Int, SemanticsNode> {
    val nodes = mutableMapOf<Int, SemanticsNode>()

    fun findAllSemanticNodesRecursive(currentNode: SemanticsNode) {
        nodes[currentNode.id] = currentNode
        currentNode.getChildren(includeDeactivatedNodes = !skipDeactivatedNodes).fastForEach { child
            ->
            findAllSemanticNodesRecursive(child)
        }
    }

    val root = if (useUnmergedTree) unmergedRootSemanticsNode else rootSemanticsNode
    if (!skipDeactivatedNodes || !root.layoutNode.isDeactivated) {
        findAllSemanticNodesRecursive(root)
    }
    return nodes
}
