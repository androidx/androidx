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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.internal.checkPrecondition
import androidx.compose.ui.internal.checkPreconditionNotNull
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.semantics.SemanticsInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Represents a [Modifier.Node] which can be a delegate of another [Modifier.Node]. Since
 * [Modifier.Node] implements this interface, in practice any [Modifier.Node] can be delegated.
 *
 * @see DelegatingNode
 * @see DelegatingNode.delegate
 */
// TODO(lmr): this interface needs a better name
interface DelegatableNode {
    /**
     * A reference of the [Modifier.Node] that holds this node's position in the node hierarchy. If
     * the node is a delegate of another node, this will point to the root delegating node that is
     * actually part of the node tree. Otherwise, this will point to itself.
     */
    val node: Modifier.Node

    /**
     * Invoked when the density changes for this node. This affects Dp to pixel conversions, and can
     * cause coordinates / other values to change.
     *
     * Changes to density will automatically invalidate layout / draw modifiers, as layout,
     * measurement, and draw depend on density. This callback can be used to update any other node
     * state that depends on density, outside of these phases. Density can be retrieved inside a
     * node by using [androidx.compose.ui.node.requireDensity].
     */
    fun onDensityChange() {}

    /**
     * Invoked when the layout direction changes for this node. This can affect the layout and
     * drawing of nodes.
     *
     * Changes to layout direction will automatically invalidate layout / draw modifiers, as layout,
     * measurement, and draw depend on layout direction. This callback can be used to update any
     * other node state that depends on layout direction, outside of these phases. Layout direction
     * can be retrieved inside a node by using [androidx.compose.ui.node.requireLayoutDirection].
     */
    fun onLayoutDirectionChange() {}

    fun interface RegistrationHandle {
        fun unregister()
    }
}

internal val DelegatableNode.isDelegationRoot: Boolean
    get() = node === this

// TREE TRAVERSAL APIS
// For now, traversing the node tree and layout node tree will be kept out of public API.
// However, when we add APIs here, we should add corresponding tests.
// Some internal modifiers, such as Focus, PointerInput, etc. will all need to utilize this
// a bit, but I think we want to avoid giving this power to public API just yet. We can
// introduce this as valid cases arise
internal inline fun DelegatableNode.visitAncestors(
    mask: Int,
    includeSelf: Boolean = false,
    block: (Modifier.Node) -> Unit,
) {
    // TODO(lmr): we might want to add some safety wheels to prevent this from being called
    //  while one of the chains is being diffed / updated. Although that might only be
    //  necessary for visiting subtree.
    checkPrecondition(node.isAttached) { "visitAncestors called on an unattached node" }
    var node: Modifier.Node? = if (includeSelf) node else node.parent
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

@Suppress("unused")
internal fun DelegatableNode.nearestAncestor(mask: Int): Modifier.Node? {
    checkPrecondition(node.isAttached) { "nearestAncestor called on an unattached node" }
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
    node.requireLayoutNode().getChildren(zOrder).forEachReversed { add(it.nodes.head) }
}

internal inline fun DelegatableNode.visitChildren(
    mask: Int,
    zOrder: Boolean,
    block: (Modifier.Node) -> Unit,
) {
    checkPrecondition(node.isAttached) { "visitChildren called on an unattached node" }
    val branches = mutableVectorOf<Modifier.Node>()
    val child = node.child
    if (child == null) branches.addLayoutNodeChildren(node, zOrder) else branches.add(child)
    while (branches.isNotEmpty()) {
        val branch = branches.removeAt(branches.lastIndex)
        if (branch.aggregateChildKindSet and mask == 0) {
            branches.addLayoutNodeChildren(branch, zOrder)
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
internal inline fun DelegatableNode.visitSubtreeIf(
    mask: Int,
    zOrder: Boolean,
    block: (Modifier.Node) -> Boolean,
) {
    checkPrecondition(node.isAttached) { "visitSubtreeIf called on an unattached node" }
    val branches = mutableVectorOf<Modifier.Node>()
    val child = node.child
    if (child == null) branches.addLayoutNodeChildren(node, zOrder) else branches.add(child)
    outer@ while (branches.isNotEmpty()) {
        val branch = branches.removeAt(branches.size - 1)
        if (branch.aggregateChildKindSet and mask != 0) {
            var node: Modifier.Node? = branch
            while (node != null && node.isAttached) {
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

internal inline fun DelegatableNode.visitLocalDescendants(
    mask: Int,
    block: (Modifier.Node) -> Unit,
) = visitLocalDescendants(mask = mask, includeSelf = false, block = block)

internal inline fun DelegatableNode.visitLocalDescendants(
    mask: Int,
    includeSelf: Boolean = false,
    block: (Modifier.Node) -> Unit,
) {
    checkPrecondition(node.isAttached) { "visitLocalDescendants called on an unattached node" }
    val self = node
    if (self.aggregateChildKindSet and mask == 0) return
    var next = if (includeSelf) self else self.child
    while (next != null) {
        if (next.kindSet and mask != 0) {
            block(next)
        }
        next = next.child
    }
}

internal inline fun DelegatableNode.visitLocalAncestors(mask: Int, block: (Modifier.Node) -> Unit) {
    checkPrecondition(node.isAttached) { "visitLocalAncestors called on an unattached node" }
    var next = node.parent
    while (next != null) {
        if (next.kindSet and mask != 0) {
            block(next)
        }
        next = next.parent
    }
}

internal inline fun <reified T> DelegatableNode.visitSelfAndLocalDescendants(
    type: NodeKind<T>,
    block: (T) -> Unit,
) = visitLocalDescendants(mask = type.mask, includeSelf = true) { it.dispatchForKind(type, block) }

internal inline fun <reified T> DelegatableNode.visitLocalDescendants(
    type: NodeKind<T>,
    block: (T) -> Unit,
) = visitLocalDescendants(type.mask) { it.dispatchForKind(type, block) }

internal inline fun <reified T> DelegatableNode.visitLocalAncestors(
    type: NodeKind<T>,
    block: (T) -> Unit,
) = visitLocalAncestors(type.mask) { it.dispatchForKind(type, block) }

internal inline fun <reified T> DelegatableNode.visitAncestors(
    type: NodeKind<T>,
    includeSelf: Boolean = false,
    block: (T) -> Unit,
) = visitAncestors(type.mask, includeSelf) { it.dispatchForKind(type, block) }

internal inline fun <reified T> DelegatableNode.visitSelfAndAncestors(
    type: NodeKind<T>,
    untilType: NodeKind<*>,
    block: (T) -> Unit,
) {
    val self = node
    visitAncestors(type.mask or untilType.mask, true) {
        if (it !== self && it.isKind(untilType)) return
        if (it.isKind(type)) {
            it.dispatchForKind(type, block)
        }
    }
}

internal inline fun <reified T> DelegatableNode.ancestors(type: NodeKind<T>): List<T>? {
    var result: MutableList<T>? = null
    visitAncestors(type) {
        if (result == null) result = mutableListOf()
        result?.add(it)
    }
    return result
}

internal inline fun <reified T : Any> DelegatableNode.nearestAncestor(type: NodeKind<T>): T? {
    visitAncestors(type) {
        return it
    }
    return null
}

internal inline fun <reified T> DelegatableNode.visitChildren(
    type: NodeKind<T>,
    zOrder: Boolean = false,
    block: (T) -> Unit,
) = visitChildren(type.mask, zOrder) { it.dispatchForKind(type, block) }

internal inline fun <reified T> DelegatableNode.visitSelfAndChildren(
    type: NodeKind<T>,
    zOrder: Boolean = false,
    block: (T) -> Unit,
) {
    node.dispatchForKind(type, block)
    visitChildren(type.mask, zOrder) { it.dispatchForKind(type, block) }
}

internal inline fun <reified T> DelegatableNode.visitSubtreeIf(
    type: NodeKind<T>,
    zOrder: Boolean = false,
    block: (T) -> Boolean,
) =
    visitSubtreeIf(type.mask, zOrder) foo@{ node ->
        node.dispatchForKind(type) { if (!block(it)) return@foo false }
        true
    }

internal inline fun <reified T> DelegatableNode.visitSubtree(
    type: NodeKind<T>,
    zOrder: Boolean = false,
    block: (T) -> Unit,
) =
    visitSubtreeIf(type.mask, zOrder) {
        it.dispatchForKind(type, block)
        true
    }

internal fun DelegatableNode.has(type: NodeKind<*>): Boolean =
    node.aggregateChildKindSet and type.mask != 0

internal fun DelegatableNode.requireCoordinator(kind: NodeKind<*>): NodeCoordinator {
    val coordinator = node.coordinator!!
    return if (coordinator.tail !== this) coordinator
    else if (kind.includeSelfInTraversal) coordinator.wrapped!! else coordinator
}

internal fun DelegatableNode.requireLayoutNode(): LayoutNode =
    checkPreconditionNotNull(node.coordinator) {
            "Cannot obtain node coordinator. Is the Modifier.Node attached?"
        }
        .layoutNode

internal fun DelegatableNode.requireSemanticsInfo(): SemanticsInfo = requireLayoutNode()

internal fun DelegatableNode.requireOwner(): Owner =
    checkPreconditionNotNull(requireLayoutNode().owner) { "This node does not have an owner." }

/**
 * Requests autofill for the LayoutNode that this [DelegatableNode] is attached to. If the node does
 * not have any autofill semantic properties set, then the request still may be sent to the Autofill
 * service, but no response is expected.
 */
fun DelegatableNode.requestAutofill() = requireLayoutNode().requestAutofill()

/**
 * Returns the current [Density] of the LayoutNode that this [DelegatableNode] is attached to. If
 * the node is not attached, this function will throw an [IllegalStateException].
 */
fun DelegatableNode.requireDensity(): Density = requireLayoutNode().density

/** Returns the current [GraphicsContext] of the [Owner] */
fun DelegatableNode.requireGraphicsContext(): GraphicsContext = requireOwner().graphicsContext

/**
 * Returns the current [LayoutDirection] of the LayoutNode that this [DelegatableNode] is attached
 * to. If the node is not attached, this function will throw an [IllegalStateException].
 */
fun DelegatableNode.requireLayoutDirection(): LayoutDirection = requireLayoutNode().layoutDirection

/**
 * Returns the [LayoutCoordinates] of this node.
 *
 * To get a signal when the [LayoutCoordinates] become available, or when its parent places it,
 * implement [LayoutAwareModifierNode].
 *
 * @throws IllegalStateException When either this node is not attached, or the [LayoutCoordinates]
 *   object is not attached.
 */
fun DelegatableNode.requireLayoutCoordinates(): LayoutCoordinates {
    checkPrecondition(node.isAttached) {
        "Cannot get LayoutCoordinates, Modifier.Node is not attached."
    }
    val coordinates = requireCoordinator(Nodes.Layout).coordinates
    checkPrecondition(coordinates.isAttached) { "LayoutCoordinates is not attached." }
    return coordinates
}

/**
 * Invalidates the subtree of this layout, including layout, drawing, parent data, etc.
 *
 * Calling this method can be a relatively expensive operation as it will cause the entire subtree
 * to relayout and redraw instead of just parts that are otherwise invalidated. Its use should be
 * limited to structural changes. This might be necessary in certain situations where you are
 * updating some data which you know descendant nodes use, but you are not relaying on automatic
 * snapshot observation through [androidx.compose.runtime.MutableState].
 */
fun DelegatableNode.invalidateSubtree() {
    if (node.isAttached) {
        requireLayoutNode().invalidateSubtree()
    }
}

/**
 * Invalidates layout for the entire subtree of this node.
 *
 * Note that [invalidateMeasurement] is preferable in most cases, however it is only guaranteed to
 * invalidate measurement for that specific node, and it is possible that layout nodes that are
 * underneath it could be cached and thus their measure policies will not get re-executed. Use this
 * API if you need to ensure that measure is called for all layout nodes below this one. This might
 * be necessary in certain situations where you are updating some data which you know descendant
 * nodes use, but you are not relaying on automatic snapshot observation through
 * [androidx.compose.runtime.MutableState].
 *
 * Calling this method can be a relatively expensive operation as it will cause the entire subtree
 * to relayout instead of just parts that are otherwise invalidated. [invalidateMeasurement] is
 * preferable in most cases, and this should only be used when absolutely necessary.
 */
fun DelegatableNode.invalidateLayoutForSubtree() {
    if (node.isAttached) {
        requireLayoutNode().invalidateLayoutForSubtree()
    }
}

/**
 * Invalidates draw for the entire subtree of this node.
 *
 * Note that [invalidateDraw] is preferable in most cases, however it is only guaranteed to
 * invalidate draw for that specific node, and it is possible that draw nodes that are underneath it
 * could be cached and thus their draw methods will not get re-executed. Use this API if you need to
 * ensure that draw is called for all draw nodes below this one. This might be necessary in certain
 * situations where you are updating some data which you know descendant nodes use, but you are not
 * relaying on automatic snapshot observation through [androidx.compose.runtime.MutableState].
 *
 * Calling this method can be a relatively expensive operation as it will cause the entire subtree
 * to redraw instead of just parts that are otherwise invalidated. [invalidateDraw] is preferable in
 * most cases, and this should only be used when absolutely necessary.
 */
fun DelegatableNode.invalidateDrawForSubtree() {
    if (node.isAttached) {
        requireLayoutNode().invalidateDrawForSubtree()
    }
}

/**
 * Call this function whenever a scroll chang happened in the LayoutNode that this [DelegatableNode]
 * is attached to to let the underlying platform know that a scroll event happened in this
 * [LayoutNode].
 *
 * On Android this will trigger a ViewTreeObserver onScrollChanged callback.
 *
 * @param delta The scroll delta that was consumed by this node.
 */
fun DelegatableNode.dispatchOnScrollChanged(delta: Offset) =
    requireOwner().dispatchOnScrollChanged(delta)

// It is safe to do this for LayoutModifierNode because we enforce only a single delegate is
// a LayoutModifierNode, however for other NodeKinds that is not true. As a result, this function
// is not generic and instead is made specific to LayoutModifierNode.
internal fun Modifier.Node.asLayoutModifierNode(): LayoutModifierNode? {
    if (!isKind(Nodes.Layout)) return null
    if (this is LayoutModifierNode) return this
    if (this is DelegatingNode) {
        var node: Modifier.Node? = delegate
        while (node != null) {
            if (node is LayoutModifierNode) return node
            node =
                if (node is DelegatingNode && node.isKind(Nodes.Layout)) {
                    // NOTE: we can only do this here because we are enforcing that a delegating
                    // node
                    // only behaves as a single LayoutModifierNode, not multiple, so we know that if
                    // the node is of kind "Layout", then one of its delegates has to be a
                    // LayoutModifierNode and *none of the other delegates of its parent can be*. As
                    // a
                    // result, we can avoid allocating a collection here and instead just dive down
                    // into
                    // this delegate directly.
                    node.delegate
                } else {
                    node.child
                }
        }
    }
    return null
}

/**
 * Since Modifier.Nodes can have multiple delegates of the same type, generally we should use this
 * method in lieu of casting a modifier.node to a particular NodeKind's interface type. This will
 * allow us to properly perform operations on the right delegates for a given node instance.
 *
 * If a Node implements T, then this will just be called once. if it does NOT implement T, it will
 * effectively dispatch recursively (although this is implemented iteratively) to all of its direct
 * delegates where delegate.isKind(kind) is true.
 *
 * In the common case of the node implementing the type directly, this method will not allocate,
 * however it allocates a vector if it dispatches to delegates.
 */
internal inline fun <reified T> Modifier.Node.dispatchForKind(
    kind: NodeKind<T>,
    block: (T) -> Unit,
) {
    var stack: MutableVector<Modifier.Node>? = null
    var node: Modifier.Node? = this
    while (node != null) {
        if (node is T) {
            block(node)
        } else if (node.isKind(kind) && node is DelegatingNode) {
            // We jump through a few extra hoops here to avoid the vector allocation in the
            // case where there is only one delegate node that implements this particular kind.
            // It is very likely that a delegating node will have one or zero delegates of a
            // particular kind, so this seems like a worthwhile optimization to make.
            var count = 0
            node.forEachImmediateDelegate { next ->
                if (next.isKind(kind)) {
                    count++
                    if (count == 1) {
                        node = next
                    } else {
                        // turns out there are multiple delegates that implement this kind, so we
                        // have to allocate in this case.
                        stack = stack ?: mutableVectorOf()
                        val theNode = node
                        if (theNode != null) {
                            stack?.add(theNode)
                            node = null
                        }
                        stack?.add(next)
                    }
                }
            }
            if (count == 1) {
                // if count == 1 then `node` is pointing to the "next" node we need to look at
                continue
            }
        }
        node = stack.pop()
    }
}

private fun MutableVector<Modifier.Node>?.pop(): Modifier.Node? {
    return if (this == null || isEmpty()) null else removeAt(size - 1)
}
