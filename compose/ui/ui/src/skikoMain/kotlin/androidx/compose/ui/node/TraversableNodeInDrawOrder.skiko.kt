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

package androidx.compose.ui.node

import androidx.compose.runtime.collection.MutableVector
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.areObjectsOfSameType

internal fun <T> T.traverseDescendantsInDrawOrder(block: (T) -> Boolean) where T : TraversableNode {
    visitSubtreeIf(Nodes.Traversable, zOrder = true) {
        if (this.traverseKey == it.traverseKey && areObjectsOfSameType(this, it)) {
            @Suppress("UNCHECKED_CAST")
            if (!block(it as T)) return
        }
        true
    }
}

// TODO: Remove a copy once aosp/3045224 merged

private inline fun <reified T> DelegatableNode.visitSubtreeIf(
    type: NodeKind<T>,
    zOrder: Boolean = false,
    block: (T) -> Boolean
) = visitSubtreeIf(type.mask, zOrder) foo@{ node ->
    node.dispatchForKind(type) {
        if (!block(it)) return@foo false
    }
    true
}

private fun LayoutNode.getChildren(zOrder: Boolean) =
    if (zOrder) {
        zSortedChildren
    } else {
        _children
    }

private fun MutableVector<Modifier.Node>.addLayoutNodeChildren(
    node: Modifier.Node,
    zOrder: Boolean,
) {
    node.requireLayoutNode().getChildren(zOrder).forEachReversed {
        add(it.nodes.head)
    }
}

private inline fun DelegatableNode.visitSubtreeIf(
    mask: Int,
    zOrder: Boolean,
    block: (Modifier.Node) -> Boolean
) {
    check(node.isAttached) { "visitSubtreeIf called on an unattached node" }
    val branches = mutableVectorOf<Modifier.Node>()
    val child = node.child
    if (child == null)
        branches.addLayoutNodeChildren(node, zOrder)
    else
        branches.add(child)
    outer@ while (branches.isNotEmpty()) {
        val branch = branches.removeAt(branches.size - 1)
        if (branch.aggregateChildKindSet and mask != 0) {
            var node: Modifier.Node? = branch
            while (node != null) {
                if (node.kindSet and mask != 0) {
                    val diveDeeper = block(node)
                    if (!diveDeeper) continue@outer
                }
                node = node.child
            }
        }
        branches.addLayoutNodeChildren(branch, zOrder)
    }
}
