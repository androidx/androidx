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

import androidx.ui.core.semantics.SemanticsProperties
import androidx.ui.painting.Canvas
import com.google.r4a.Emittable

/**
 * Owner implements the connection to the underlying view system. On Android, this connects
 * to Android [android.view.View]s and all layout, draw, input, and accessibility is hooked
 * through them.
 */
interface Owner {
    /**
     * Called from a [DrawNode], this registers with the underlying view system that a
     * redraw of the given [drawNode] is required. It may cause other nodes to redraw, if
     * necessary.
     */
    fun onInvalidate(drawNode: DrawNode)

    /**
     * Called by [LayoutNode] to indicate the new size of [layoutNode].
     * The owner may need to track updated layouts.
     */
    fun onSizeChange(layoutNode: LayoutNode)

    /**
     * Called by [LayoutNode] to indicate the new position of [layoutNode].
     * The owner may need to track updated layouts.
     */
    fun onPositionChange(layoutNode: LayoutNode)

    /**
     * Called by [ComponentNode] when it is attached to the view system and now has an owner.
     * This is used by [Owner] to update [ComponentNode.ownerData] and track which nodes are
     * associated with it. It will only be called when [node] is not already attached to an
     * owner.
     */
    fun onAttach(node: ComponentNode)

    /**
     * Called by [ComponentNode] when it is detached from the view system, such as during
     * [ComponentNode.dropChild]. This will only be called for [node]s that are already
     * [ComponentNode.attach]ed.
     */
    fun onDetach(node: ComponentNode)
}

/**
 * The base type for all nodes from the tree generated from a component hierarchy.
 *
 * Specific components are backed by a tree of nodes: Draw, Layout, Semantics, GestureDetector.
 * All other components are not represented in the backing hierarchy.
 */
sealed class ComponentNode : Emittable {
    /**
     * The parent node in the ComponentNode hierarchy. This is `null` when the `ComponentNode`
     * is attached (has an [owner]) and is the root of the tree or has not had [add] called for it.
     */
    var parent: ComponentNode? = null
        private set

    /**
     * The view system [Owner]. This `null` until [attach] is called
     */
    var owner: Owner? = null
        private set

    /**
     * The tree depth of the ComponentNode. This is valid only when [isAttached] is true.
     */
    var depth: Int = 0

    /**
     * An opaque value set by the [Owner]. It is `null` when [isAttached] is false, but
     * may also be `null` when [isAttached] is true, depending on the needs of the Owner.
     */
    var ownerData: Any? = null

    /**
     * Returns the number of children in this ComponentNode.
     */
    abstract val count: Int

    /**
     * Returns the first [LayoutNode] in the subtree. If this node is already [LayoutNode]
     * it returns this, otherwise the next [LayoutNode] or null if there is no [LayoutNode]
     * within the child's hierarchy.
     * All node types expect [LayoutNode] could have only a single child.
     */
    abstract val layoutNode: LayoutNode?

    /**
     * This is the LayoutNode ancestor that contains this LayoutNode. This will be `null` for the
     * root [LayoutNode].
     */
    open var parentLayoutNode: LayoutNode? = null

    /**
     * Execute [block] on all children of this ComponentNode. There is no single concept for
     * children in ComponentNode, so this method allows executing a method on all children.
     */
    abstract fun visitChildren(reverse: Boolean = false, block: (ComponentNode) -> Unit)

    /**
     * Inserts a child [ComponentNode] at a particular index. If this ComponentNode [isAttached]
     * then [child] will become [attach]ed also. [child] must have a `null` [parent].
     */
    override fun emitInsertAt(index: Int, instance: Emittable) {
        if (instance !is ComponentNode) {
            ErrorMessages.OnlyComponents.state()
        }
        if (instance.parent != null) {
            ErrorMessages.ComponentNodeHasParent.state()
        }
        instance.parent = this
        val owner = this.owner
        if (owner != null) {
            instance.attach(owner)
        }
    }

    /**
     * Removes one or more children, starting at [index].
     */
    override fun emitRemoveAt(index: Int, count: Int) {
        val attached = owner != null
        for (i in index until index + count) {
            val child = this[i]
            child.parent = null
            if (attached) {
                child.detach()
            }
        }
    }

    /**
     * Returns the child ComponentNode at the given index. An exception will be thrown if there
     * is no child at the given index.
     */
    abstract operator fun get(index: Int): ComponentNode

    /**
     * Set the [Owner] of this ComponentNode. This ComponentNode must not already be attached.
     * [owner] must match its [parent].[owner].
     */
    open fun attach(owner: Owner) {
        ErrorMessages.OwnerAlreadyAttached.validateState(this.owner == null)
        val parent = parent
        ErrorMessages.ParentOwnerMustMatchChild.validateState(
            parent == null ||
                    parent.owner == owner
        )
        this.owner = owner
        this.depth = (parent?.depth ?: -1) + 1
        owner.onAttach(this)
        visitChildren { child ->
            child.attach(owner)
        }
    }

    /**
     * Remove the ComponentNode from the [Owner]. The [owner] must not be `null` before this call
     * and its [parent]'s [owner] must be `null` before calling this. This will also [detach] all
     * children. After executing, the [owner] will be `null`.
     */
    open fun detach() {
        val owner = owner ?: ErrorMessages.OwnerAlreadyDetached.state()
        owner.onDetach(this)
        visitChildren { child ->
            child.detach()
        }
        this.owner = null
        depth = 0
    }
}

/**
 * Returns true if this [ComponentNode] currently has an [owner].  Semantically, this means that
 * the ComponentNode is currently a part of a component tree.
 */
fun ComponentNode.isAttached() = owner != null

/**
 * Base class for [ComponentNode]s that have zero or one child
 */
sealed class SingleChildComponentNode() : ComponentNode() {
    /**
     * The child that this ComponentNode has. This will be `null` if it has no child.
     */
    var child: ComponentNode? = null

    override var layoutNode: LayoutNode? = null

    override val count: Int
        get() = if (child == null) 0 else 1

    override var parentLayoutNode: LayoutNode?
        get() = super.parentLayoutNode
        set(value) {
            super.parentLayoutNode = value
            child?.parentLayoutNode = value
        }

    override fun emitInsertAt(index: Int, instance: Emittable) {
        ErrorMessages.IndexOutOfRange.validateArg(index == 0 && child == null, index)
        super.emitInsertAt(index, instance)
        val child = instance as ComponentNode
        this.child = child
        child.parentLayoutNode = parentLayoutNode
        layoutNode = child.layoutNode
    }

    override fun emitRemoveAt(index: Int, count: Int) {
        ErrorMessages.SingleChildOnlyOneNode.validateArg(count == 1, count)
        ErrorMessages.IndexOutOfRange.validateArg(index == 0 && child != null, index)
        super.emitRemoveAt(index, count)
        child?.parentLayoutNode = null
        this.child = null
        this.layoutNode = null
    }

    override fun get(index: Int): ComponentNode {
        ErrorMessages.IndexOutOfRange.validateArg(index == 0, index)
        return child ?: ErrorMessages.NoChild.arg()
    }

    override fun emitMove(from: Int, to: Int, count: Int) {
        ErrorMessages.NoMovingSingleElements.unsupported()
    }

    override fun visitChildren(reverse: Boolean, block: (ComponentNode) -> Unit) {
        val child = this.child
        if (child != null) {
            block(child)
        }
    }
}

/**
 * Backing node for handling pointer events.
 */
class PointerInputNode() : SingleChildComponentNode() {
    var pointerInputHandler: PointerInputHandler = { event, _ -> event }
}

/**
 * Backing node for the [Draw] component.
 */
class DrawNode() : ComponentNode() {
    var onPaint: DensityReceiver.(canvas: Canvas, parentSize: PxSize) -> Unit = { _, _ -> }
        set(value) {
            field = value
            invalidate()
        }

    override val count: Int
        get() = 0

    var needsPaint = true

    override val layoutNode: LayoutNode? get() = null // no children

    override fun visitChildren(reverse: Boolean, block: (ComponentNode) -> Unit) {
        // no children
    }

    override fun emitMove(from: Int, to: Int, count: Int) {
        ErrorMessages.ChildrenUnsupported.unsupported()
    }

    override fun get(index: Int): ComponentNode {
        ErrorMessages.ChildrenUnsupported.unsupported()
    }

    override fun emitInsertAt(index: Int, instance: Emittable) {
        ErrorMessages.ChildrenUnsupported.unsupported()
    }

    override fun emitRemoveAt(index: Int, count: Int) {
        ErrorMessages.ChildrenUnsupported.unsupported()
    }

    override fun attach(owner: Owner) {
        super.attach(owner)
        if (needsPaint) {
            owner.onInvalidate(this)
        }
    }

    fun invalidate() {
        if (!needsPaint) {
            needsPaint = true
            owner?.onInvalidate(this)
        }
    }
}

/**
 * Backing node for [Layout] component.
 */
class LayoutNode : ComponentNode() {
    /**
     * The list of child ComponentNodes that this ComponentNode has. It can contain zero or
     * more entries.
     */
    val children = mutableListOf<ComponentNode>()

    /**
     * The constraints used the last time [layout] was called.
     */
    var constraints: Constraints = Constraints.tightConstraints(IntPx.Zero, IntPx.Zero)

    var ref: Ref<LayoutNode>?
        get() = null
        set(value) {
            value?.value = this
        }

    // This is a ComplexMeasureBox, but we don't have access to that class from here.
    var measureBox: Any? = null

    /**
     * The width of this layout
     */
    var width = IntPx.Zero
        private set

    /**
     * The height of this layout
     */
    var height = IntPx.Zero
        private set

    /**
     * The horizontal position within the parent of this layout
     */
    var x = IntPx.Zero
        private set

    /**
     * The vertical position within the parent of this layout
     */
    var y = IntPx.Zero
        private set

    /**
     * Whether or not this has been placed in the hierarchy.
     */
    var visible = true

    override val layoutNode: LayoutNode get() = this

    override val count: Int
        get() = children.size

    override fun get(index: Int): ComponentNode = children.get(index)

    override fun emitInsertAt(index: Int, instance: Emittable) {
        // TODO(mount): Allow inserting Views
        if (instance !is ComponentNode) {
            ErrorMessages.OnlyComponents.state()
        }
        instance.parentLayoutNode = this
        children.add(index, instance)
        super.emitInsertAt(index, instance)
    }

    override fun emitRemoveAt(index: Int, count: Int) {
        super.emitRemoveAt(index, count)
        for (i in index + count - 1 downTo index) {
            val child = children.removeAt(i)
            child.parentLayoutNode = null
        }
    }

    override fun emitMove(from: Int, to: Int, count: Int) {
        ErrorMessages.IllegalMoveOperation.validateArgs(
            from >= 0 && to >= 0 && count > 0,
            count, from, to
        )
        // Do the simple thing for now. We can improve efficiency later if we need to
        val removed = ArrayList<ComponentNode>(count)
        for (i in from until from + count) {
            removed += children[i]
        }
        children.removeAll(removed)

        children.addAll(to, removed)
    }

    override fun visitChildren(reverse: Boolean, block: (ComponentNode) -> Unit) {
        val children = if (reverse) children.reversed() else children
        children.forEach { child ->
            block(child)
        }
    }

    fun moveTo(x: IntPx, y: IntPx) {
        if (x != this.x || y != this.y) {
            this.x = x
            this.y = y
            owner?.onPositionChange(this)
        }
    }

    fun resize(width: IntPx, height: IntPx) {
        if (width != this.width || height != this.height) {
            this.width = width
            this.height = height
            owner?.onSizeChange(this)
        }
    }
}

class SemanticsR4ANode(
    /**
     * If [container] is true, this widget will introduce a new
     * node in the semantics tree. Otherwise, the semantics will be
     * merged with the semantics of any ancestors (if the ancestor allows that).
     *
     * Whether descendants of this widget can add their semantic information to the
     * [SemanticsNode] introduced by this configuration is controlled by
     * [explicitChildNodes].
     */
    var container: Boolean = false,
    /**
     * Whether descendants of this widget are allowed to add semantic information
     * to the [SemanticsNode] annotated by this widget.
     *
     * When set to false descendants are allowed to annotate [SemanticNode]s of
     * their parent with the semantic information they want to contribute to the
     * semantic tree.
     * When set to true the only way for descendants to contribute semantic
     * information to the semantic tree is to introduce new explicit
     * [SemanticNode]s to the tree.
     *
     * If the semantics properties of this node include
     * [SemanticsProperties.scopesRoute] set to true, then [explicitChildNodes]
     * must be true also.
     *
     * This setting is often used in combination with [SemanticsConfiguration.isSemanticBoundary]
     * to create semantic boundaries that are either writable or not for children.
     */
    var explicitChildNodes: Boolean = false,
    /**
     * Contains properties used by assistive technologies to make the application
     * more accessible.
     */
    var properties: SemanticsProperties
) : SingleChildComponentNode()

/**
 * The list of child MeasureBoxes. It can contain zero or more entries.
 */
fun LayoutNode.childrenMeasureBoxes(): List<Any> {
    return children.mapNotNull { it.layoutNode?.measureBox }
}

/**
 * Inserts a child [ComponentNode] at a last index. If this ComponentNode [isAttached]
 * then [child] will become [attach]ed also. [child] must have a `null` [parent].
 */
fun ComponentNode.add(child: ComponentNode) {
    emitInsertAt(count, child)
}

class Ref<T>() {
    var value: T? = null
}

/**
 * Converts a global position into a local position within this LayoutNode.
 */
fun LayoutNode.globalToLocal(global: PxPosition): PxPosition {
    var x: Px = global.x
    var y: Px = global.y
    var node: LayoutNode? = this
    while (node != null) {
        x -= node.x.toPx()
        y -= node.y.toPx()
        node = node.parentLayoutNode
    }
    return PxPosition(x, y)
}

/**
 * Converts a local position within this LayoutNode into a global one.
 */
fun LayoutNode.localToGlobal(local: PxPosition): PxPosition {
    var x: Px = local.x
    var y: Px = local.y
    var node: LayoutNode? = this
    while (node != null) {
        x += node.x.toPx()
        y += node.y.toPx()
        node = node.parentLayoutNode
    }
    return PxPosition(x, y)
}

/**
 * Converts a child LayoutNode position into a local position within this LayoutNode.
 */
fun LayoutNode.childToLocal(child: LayoutNode, childLocal: PxPosition): PxPosition {
    if (child === this) {
        return childLocal
    }
    var x: Px = childLocal.x
    var y: Px = childLocal.y
    var node: LayoutNode? = child
    while (true) {
        if (node == null) {
            throw IllegalStateException("Current layout is not an ancestor of the provided" +
                    "child layout")
        }
        x += node.x.toPx()
        y += node.y.toPx()
        node = node.parentLayoutNode
        if (node === this) {
            // found the node
            break
        }
    }
    return PxPosition(x, y)
}
