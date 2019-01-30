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

package androidx.ui.core

import androidx.ui.core.semantics.SemanticsConfiguration

/**
 * Represent a node in the semantics tree together with information about its parent and children.
 *
 * @param parent Parent of this node or null if none
 * @param data The actual semantics data of this node
 */
class SemanticsTreeNodeImpl(
    override val parent: SemanticsTreeNode?,
    override val data: SemanticsConfiguration,
    private val layoutNode: LayoutNode?
) : SemanticsTreeNode {
    private val _children = mutableSetOf<SemanticsTreeNode>()
    override val children: Set<SemanticsTreeNode>
        get() = _children

    fun addChild(child: SemanticsTreeNode) {
        _children.add(child)
    }
    override val globalPosition: PxPosition?
        get() = layoutNode?.localToGlobal(PxPosition(0.px, 0.px))
}

/**
 * Finds all [SemanticsTreeNode]s under the given [rootNode].
 */
internal fun findAllSemanticNodesIn(rootNode: ComponentNode): List<SemanticsTreeNode> {
    // TODO(pavlis): Write some unit tests for this
    var nodes = mutableListOf<SemanticsTreeNode>()
    findAllSemanticNodesInternal(parent = null, currentNode = rootNode, nodes = nodes)
    return nodes
}

private fun findAllSemanticNodesInternal(
    parent: SemanticsTreeNodeImpl?,
    currentNode: ComponentNode,
    nodes: MutableList<SemanticsTreeNode>
) {
    var currentParent = parent
    if (currentNode is SemanticsComponentNode) {
        val wrapper = SemanticsTreeNodeImpl(
            parent = parent,
            data = currentNode.semanticsConfiguration,
            layoutNode = currentNode.layoutNode
        )
        parent?.addChild(wrapper)
        nodes.add(wrapper)
        currentParent = parent
    }

    currentNode.visitChildren {
        findAllSemanticNodesInternal(currentParent, it, nodes)
    }
}