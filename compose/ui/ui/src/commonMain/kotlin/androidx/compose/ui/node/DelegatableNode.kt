/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Represents a [Modifier.Node] which can be a delegate of another [Modifier.Node]. Since
 * [Modifier.Node] implements this interface, in practice any [Modifier.Node] can be delegated.
 *
 * @see DelegatingNode
 * @see DelegatingNode.delegated
 */
// TODO(lmr): this interface needs a better name
interface DelegatableNode {
    /**
     * A reference of the [Modifier.Node] that holds this node's position in the node hierarchy. If
     * the node is a delegate of another node, this will point to that node. Otherwise, this will
     * point to itself.
     */
    val node: Modifier.Node
}

// TREE TRAVERSAL APIS
// For now, traversing the node tree and layout node tree will be kept out of public API.
// Some internal modifiers, such as Focus, PointerInput, etc. will all need to utilize this
// a bit, but I think we want to avoid giving this power to public API just yet. We can
// introduce this as valid cases arise
internal fun DelegatableNode.localChild(mask: Int): Modifier.Node? {
    val child = node.child ?: return null
    if (child.aggregateChildKindSet and mask == 0) return null
    var next: Modifier.Node? = child
    while (next != null) {
        if (next.kindSet and mask != 0) {
            return next
        }
        next = next.child
    }
    return null
}

internal fun DelegatableNode.localParent(mask: Int): Modifier.Node? {
    var next = node.parent
    while (next != null) {
        if (next.kindSet and mask != 0) {
            return next
        }
        next = next.parent
    }
    return null
}

internal inline fun DelegatableNode.visitAncestors(mask: Int, block: (Modifier.Node) -> Unit) {
    // TODO(lmr): we might want to add some safety wheels to prevent this from being called
    //  while one of the chains is being diffed / updated. Although that might only be
    //  necessary for visiting subtree.
    check(node.isAttached)
    var node: Modifier.Node? = node.parent
    var layout: LayoutNode? = requireLayoutNode()
    while (layout != null) {
        val head = layout.nodes.head
        if (head.aggregateChildKindSet and mask != 0) {
            while (node != null) {
                if (node.kindSet and mask != 0) {
                    block(node)
                }
                node = node.parent
            }
        }
        layout = layout.parent
        node = layout?.nodes?.tail
    }
}

internal fun DelegatableNode.ancestors(mask: Int): List<Modifier.Node>? {
    check(node.isAttached)
    var ancestors: MutableList<Modifier.Node>? = null
    var node: Modifier.Node? = node.parent
    var layout: LayoutNode? = requireLayoutNode()
    while (layout != null) {
        val head = layout.nodes.head
        if (head.aggregateChildKindSet and mask != 0) {
            while (node != null) {
                if (node.kindSet and mask != 0) {
                    if (ancestors == null) ancestors = mutableListOf()
                    ancestors += node
                }
                node = node.parent
            }
        }
        layout = layout.parent
        node = layout?.nodes?.tail
    }
    return ancestors
}

internal fun DelegatableNode.nearestAncestor(mask: Int): Modifier.Node? {
    check(node.isAttached)
    var node: Modifier.Node? = node.parent
    var layout: LayoutNode? = requireLayoutNode()
    while (layout != null) {
        val head = layout.nodes.head
        if (head.aggregateChildKindSet and mask != 0) {
            while (node != null) {
                if (node.kindSet and mask != 0) {
                    return node
                }
                node = node.parent
            }
        }
        layout = layout.parent
        node = layout?.nodes?.tail
    }
    return null
}

internal fun DelegatableNode.firstChild(mask: Int): Modifier.Node? {
    check(node.isAttached)
    val branches = mutableVectorOf<Modifier.Node>()
    val child = node.child
    if (child == null)
        branches.addLayoutNodeChildren(node)
    else
        branches.add(child)
    while (branches.isNotEmpty()) {
        val branch = branches.removeAt(branches.lastIndex)
        if (branch.aggregateChildKindSet and mask == 0) {
            branches.addLayoutNodeChildren(branch)
            // none of these nodes match the mask, so don't bother traversing them
            continue
        }
        var node: Modifier.Node? = branch
        while (node != null) {
            if (node.kindSet and mask != 0) {
                return node
            }
            node = node.child
        }
    }
    return null
}

internal inline fun DelegatableNode.visitSubtree(mask: Int, block: (Modifier.Node) -> Unit) {
    // TODO(lmr): we might want to add some safety wheels to prevent this from being called
    //  while one of the chains is being diffed / updated.
    check(node.isAttached)
    var node: Modifier.Node? = node.child
    var layout: LayoutNode? = requireLayoutNode()
    // we use this bespoke data structure here specifically for traversing children. In the
    // depth first traversal you would typically do a `stack.addAll(node.children)` type
    // call, but to avoid enumerating the vector and moving into our stack, we simply keep
    // a stack of vectors and keep track of where we are in each
    val nodes = NestedVectorStack<LayoutNode>()
    while (layout != null) {
        // NOTE: the ?: is important here for the starting condition, since we are starting
        // at THIS node, and not the head of this node chain.
        node = node ?: layout.nodes.head
        if (node.aggregateChildKindSet and mask != 0) {
            while (node != null) {
                if (node.kindSet and mask != 0) {
                    block(node)
                }
                node = node.child
            }
            node = null
        }
        nodes.push(layout._children)
        layout = if (nodes.isNotEmpty()) nodes.pop() else null
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun MutableVector<Modifier.Node>.addLayoutNodeChildren(node: Modifier.Node) {
    node.requireLayoutNode()._children.forEachReversed {
        add(it.nodes.head)
    }
}

internal inline fun DelegatableNode.visitChildren(mask: Int, block: (Modifier.Node) -> Unit) {
    check(node.isAttached)
    val branches = mutableVectorOf<Modifier.Node>()
    val child = node.child
    if (child == null)
        branches.addLayoutNodeChildren(node)
    else
        branches.add(child)
    while (branches.isNotEmpty()) {
        val branch = branches.removeAt(branches.lastIndex)
        if (branch.aggregateChildKindSet and mask == 0) {
            branches.addLayoutNodeChildren(branch)
            // none of these nodes match the mask, so don't bother traversing them
            continue
        }
        var node: Modifier.Node? = branch
        while (node != null) {
            if (node.kindSet and mask != 0) {
                block(node)
                break
            }
            node = node.child
        }
    }
}

/**
 * visit the shallow tree of children of a given mask, but if block returns true, we will continue
 * traversing below it
 */
internal inline fun DelegatableNode.visitSubtreeIf(mask: Int, block: (Modifier.Node) -> Boolean) {
    check(node.isAttached)
    val branches = mutableVectorOf<Modifier.Node>()
    val child = node.child
    if (child == null)
        branches.addLayoutNodeChildren(node)
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
        branches.addLayoutNodeChildren(branch)
    }
}

internal inline fun DelegatableNode.visitLocalChildren(mask: Int, block: (Modifier.Node) -> Unit) {
    check(node.isAttached)
    val self = node
    if (self.aggregateChildKindSet and mask == 0) return
    var next = self.child
    while (next != null) {
        if (next.kindSet and mask != 0) {
            block(next)
        }
        next = next.child
    }
}

internal inline fun DelegatableNode.visitLocalParents(mask: Int, block: (Modifier.Node) -> Unit) {
    check(node.isAttached)
    var next = node.parent
    while (next != null) {
        if (next.kindSet and mask != 0) {
            block(next)
        }
        next = next.parent
    }
}

internal inline fun <reified T> DelegatableNode.visitLocalChildren(
    type: NodeKind<T>,
    block: (T) -> Unit
) = visitLocalChildren(type.mask) {
    if (it is T) block(it)
}

internal inline fun <reified T> DelegatableNode.visitLocalParents(
    type: NodeKind<T>,
    block: (T) -> Unit
) = visitLocalParents(type.mask) {
    if (it is T) block(it)
}

internal inline fun <reified T> DelegatableNode.localParent(type: NodeKind<T>): T? =
    localParent(type.mask) as? T

internal inline fun <reified T> DelegatableNode.localChild(type: NodeKind<T>): T? =
    localChild(type.mask) as? T

internal inline fun <reified T> DelegatableNode.visitAncestors(
    type: NodeKind<T>,
    block: (T) -> Unit
) = visitAncestors(type.mask) { if (it is T) block(it) }

@Suppress("UNCHECKED_CAST") // Type info lost due to erasure.
internal inline fun <reified T> DelegatableNode.ancestors(
    type: NodeKind<T>
): List<T>? = ancestors(type.mask) as? List<T>

internal inline fun <reified T : Any> DelegatableNode.nearestAncestor(type: NodeKind<T>): T? =
    nearestAncestor(type.mask) as? T

internal inline fun <reified T : Any> DelegatableNode.firstChild(type: NodeKind<T>): T? =
    firstChild(type.mask) as? T

internal inline fun <reified T> DelegatableNode.visitSubtree(
    type: NodeKind<T>,
    block: (T) -> Unit
) = visitSubtree(type.mask) { if (it is T) block(it) }

internal inline fun <reified T> DelegatableNode.visitChildren(
    type: NodeKind<T>,
    block: (T) -> Unit
) = visitChildren(type.mask) { if (it is T) block(it) }

internal inline fun <reified T> DelegatableNode.visitSubtreeIf(
    type: NodeKind<T>,
    block: (T) -> Boolean
) = visitSubtreeIf(type.mask) { if (it is T) block(it) else true }

internal fun DelegatableNode.has(type: NodeKind<*>): Boolean =
    node.aggregateChildKindSet and type.mask != 0

internal fun DelegatableNode.requireCoordinator(kind: NodeKind<*>): NodeCoordinator {
    val coordinator = node.coordinator!!
    return if (coordinator.tail !== this)
        coordinator
    else if (kind.includeSelfInTraversal)
        coordinator.wrapped!!
    else
        coordinator
}

internal fun DelegatableNode.requireLayoutNode(): LayoutNode =
    checkNotNull(node.coordinator) {
        "Cannot obtain node coordinator. Is the Modifier.Node attached?"
    }.layoutNode

internal fun DelegatableNode.requireOwner(): Owner = checkNotNull(requireLayoutNode().owner)

/**
 * Returns the current [Density] of the LayoutNode that this [DelegatableNode] is attached to.
 * If the node is not attached, this function will throw an [IllegalStateException].
 */
fun DelegatableNode.requireDensity(): Density = requireLayoutNode().density

/**
 * Returns the current [LayoutDirection] of the LayoutNode that this [DelegatableNode] is attached
 * to. If the node is not attached, this function will throw an [IllegalStateException].
 */
fun DelegatableNode.requireLayoutDirection(): LayoutDirection = requireLayoutNode().layoutDirection

/**
 * Invalidates the subtree of this layout, including layout, drawing, parent data, etc.
 *
 * Calling this method can be a relatively expensive operation as it will cause the
 * entire subtree to relayout and redraw instead of just parts that
 * are otherwise invalidated. Its use should be limited to structural changes.
 */
fun DelegatableNode.invalidateSubtree() {
    if (node.isAttached) {
        requireLayoutNode().invalidateSubtree()
    }
}