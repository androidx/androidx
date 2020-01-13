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
import androidx.ui.geometry.Rect
import androidx.ui.unit.PxPosition
import androidx.ui.unit.px
import androidx.ui.unit.toPx

/**
 * Represent a node in the semantics tree together with information about its parent and children.
 *
 * @param parent Parent of this node or null if none
 * @param semanticsComponentNode The actual semantics data of this node
 */
class SemanticsTreeNodeImpl(
    override val parent: SemanticsTreeNode?,
    private val semanticsComponentNode: SemanticsComponentNode
) : SemanticsTreeNode {
    private val _children = mutableSetOf<SemanticsTreeNode>()
    override val children: Set<SemanticsTreeNode>
        get() = _children

    fun addChild(child: SemanticsTreeNode) {
        _children.add(child)
    }
    override val globalRect: Rect?
        get() {
            // TODO(ryanmentley): Handle multiple children better
            val layoutNode = semanticsComponentNode.findLastLayoutChild { true } ?: return null
            val position = layoutNode.localToGlobal(PxPosition(0.px, 0.px))

            return Rect.fromLTWH(
                position.x.value,
                position.y.value,
                layoutNode.width.toPx().value,
                layoutNode.height.toPx().value
            )
        }

    override val data: SemanticsConfiguration
        get() = semanticsComponentNode.semanticsConfiguration

    override fun findClosestParentNode(selector: (ComponentNode) -> Boolean): ComponentNode? {
        return semanticsComponentNode.findClosestParentNode(selector)
    }
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
            semanticsComponentNode = currentNode
        )
        parent?.addChild(wrapper)
        nodes.add(wrapper)
        currentParent = wrapper
    }

    currentNode.visitChildren {
        findAllSemanticNodesInternal(currentParent, it, nodes)
    }
}
