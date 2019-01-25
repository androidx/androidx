/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.ui.foundation

import androidx.ui.assert
import androidx.ui.engine.geometry.Size
import androidx.ui.painting.Canvas
import androidx.ui.rendering.box.BoxConstraints
import androidx.ui.rendering.obj.Constraints
import kotlin.math.roundToInt

/**
 * PortOwner implements the connection to the underlying view system. On Android, this connects
 * to Android [android.view.View]s and all layout, draw, input, and accessibility is hooked
 * through them.
 */
internal interface PortOwner {
    /**
     * Called from a [DrawNodePort], this registers with the underlying view system that a
     * redraw of the given [drawNode] is required. It may cause other nodes to redraw, if
     * necessary.
     */
    fun onInvalidate(drawNode: DrawNodePort)

    /**
     * Called from a [LayoutNodePort], this registers with the underlying view system that the
     * given node needs a relayout. The given [layoutNode] will have [LayoutNodePort.layout] called
     * on it at a later time determined by the view system.
     */
    fun onRequestLayout(layoutNode: LayoutNodePort)

    /**
     * Called by [LayoutNodePort] to indicate the new size of [layoutNode].
     * The owner may need to track updated layouts.
     */
    fun onSizeChange(layoutNode: LayoutNodePort)

    /**
     * Called by [LayoutNodePort] to indicate the new position of [layoutNode].
     * The owner may need to track updated layouts.
     */
    fun onPositionChange(layoutNode: LayoutNodePort)

    /**
     * Called by [PortComponentNode] when it is attached to the view system and now has an owner.
     * This is used by [PortOwner] to update [PortComponentNode.ownerData] and track which nodes are
     * associated with it. It will only be called when [node] is not already attached to an
     * owner.
     */
    fun onAttach(node: PortComponentNode)

    /**
     * Called by [PortComponentNode] when it is detached from the view system, such as during
     * [PortComponentNode.dropChild]. This will only be called for [node]s that are already
     * [PortComponentNode.attach]ed.
     */
    fun onDetach(node: PortComponentNode)
}

/**
 * The base type for all nodes from the tree generated from a component hierarchy.
 *
 * Specific components are backed by a tree of nodes: Draw, Layout, Semantics, GestureDetector.
 * All other components are not represented in the backing hierarchy.
 */
internal sealed class PortComponentNode {
    /**
     * The parent node in the PortComponentNode hierarchy. This is `null` when the
     * `PortComponentNode` is attached (has an [owner]) and is the root of the tree or has not had
     * [add] called for it.
     */
    var parent: PortComponentNode? = null
        private set

    /**
     * The view system [PortOwner]. This `null` until [attach] is called
     */
    var owner: PortOwner? = null
        private set

    /**
     * The tree depth of the PortComponentNode. This is valid only when [isAttached] is true.
     */
    var depth: Int = 0

    /**
     * An opaque value set by the [PortOwner]. It is `null` when [isAttached] is false, but
     * may also be `null` when [isAttached] is true, depending on the needs of the PortOwner.
     */
    var ownerData: Any? = null

    /**
     * Returns the number of children in this PortComponentNode.
     */
    abstract val size: Int

    /**
     * Execute [block] on all children of this PortComponentNode. There is no single concept for
     * children in PortComponentNode, so this method allows executing a method on all children.
     */
    abstract fun visitChildren(block: (PortComponentNode) -> Unit)

    /**
     * Inserts a child [PortComponentNode] at a particular index. If this PortComponentNode
     * [isAttached] then [child] will become [attach]ed also. [child] must have a `null` [parent].
     */
    open fun add(index: Int, child: PortComponentNode) {
        assert(child.parent == null)
        child.parent = this
        val owner = this.owner
        if (owner != null) {
            child.attach(owner)
        }
    }

    /**
     * Removes one or more children, starting at [index].
     */
    open fun remove(index: Int, count: Int = 1) {
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
     * Moves children from one index to another. The children's lifecycle will not change.
     */
    abstract fun move(from: Int, to: Int, count: Int)

    /**
     * Returns the child PortComponentNode at the given index. An exception will be thrown if there
     * is no child at the given index.
     */
    abstract operator fun get(index: Int): PortComponentNode

    /**
     * Set the [PortOwner] of this PortComponentNode. This PortComponentNode must not already be
     * attached. [owner] must match its [parent].[owner].
     */
    open fun attach(owner: PortOwner) {
        assert(this.owner == null)
        assert {
            val parent = parent
            if (parent != null) {
                parent.owner == owner
            } else {
                true
            }
        }
        this.owner = owner
        this.depth = (parent?.depth ?: -1) + 1
        owner.onAttach(this)
        visitChildren { child ->
            child.attach(owner)
        }
    }

    /**
     * Remove the PortComponentNode from the [PortOwner]. The [owner] must not be `null` before this
     * call and its [parent]'s [owner] must be `null` before calling this. This will also [detach]
     * all children. After executing, the [owner] will be `null`.
     */
    open fun detach() {
        assert(owner != null)
        owner!!.onDetach(this)
        visitChildren { child ->
            child.detach()
        }
        owner = null
        depth = 0
    }
}

/**
 * Base class for [PortComponentNode]s that have zero or one child
 */
internal open class SingleChildPortComponentNode() : PortComponentNode() {
    /**
     * The child that this PortComponentNode has. This will be `null` if it has no child.
     */
    var child: PortComponentNode? = null

    override val size: Int
        get() = if (child == null) 0 else 1

    override fun add(index: Int, child: PortComponentNode) {
        assert(this.child == null)
        assert(index == 0)
        super.add(index, child)
        this.child = child
    }

    override fun remove(index: Int, count: Int) {
        assert(count == 1)
        assert(index == 0)
        assert(child != null)
        super.remove(index, count)
        this.child = null
    }

    override fun get(index: Int): PortComponentNode {
        assert(index == 0)
        return child ?: throw IllegalArgumentException("There is no child of this node")
    }

    override fun move(from: Int, to: Int, count: Int) {
        throw UnsupportedOperationException("Can't move children when there is a maximum of 1")
    }

    override fun visitChildren(block: (PortComponentNode) -> Unit) {
        val child = this.child
        if (child != null) {
            block(child)
        }
    }
}

/**
 * Backing node for Gesture
 */
internal class GestureNodePort() : SingleChildPortComponentNode()

/**
 * Backing node for the [Draw] component.
 */
internal class DrawNodePort(val onPaint: (Canvas) -> Unit) : PortComponentNode() {
    override val size: Int
        get() = 0

    var needsPaint = true

    override fun visitChildren(block: (PortComponentNode) -> Unit) {
        // no children
    }

    override fun move(from: Int, to: Int, count: Int) {
        throw UnsupportedOperationException("Children are not supported")
    }

    override fun get(index: Int): PortComponentNode {
        throw IllegalArgumentException("Children are not supported")
    }

    override fun add(index: Int, child: PortComponentNode) {
        throw UnsupportedOperationException("Children are not supported")
    }

    override fun remove(index: Int, count: Int) {
        throw java.lang.IllegalArgumentException("Children are not supported")
    }

    override fun attach(owner: PortOwner) {
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
internal class LayoutNodePort(
    /** This is only needed now because we don't have reconciliation */
    val onLayout: LayoutNodePort.(constraints: Constraints, parentUsesSize: Boolean) -> Size
) : PortComponentNode() {
    /**
     * The list of child ComponentNodes that this PortComponentNode has. It can contain zero or
     * more entries.
     */
    val children = mutableListOf<PortComponentNode>()

    /**
     * `true` when [dirtyLayout] has been called on this Node and `false` after [layout] has
     * been called.
     */
    var needsLayout = true

    /**
     * Flutter hint that the parent node controls the size of this layout instead.
     */
    var sizedByParent = true

    /**
     * The constraints used the last time [layout] was called.
     */
    var constraints: Constraints = BoxConstraints.tight(Size(0.0f, 0.0f))

    /**
     * The `parentUsesSize` from the last time [layout] was called.
     */
    var parentUsesSize: Boolean = false

    /**
     * The [Size.width] from the return value of [onLayout]. This is set during [layout].
     */
    var width = 0
        private set

    /**
     * The [Size.height] from the return value of [onLayout]. This is set during [layout].
     */
    var height = 0
        private set

    /**
     * The x position of this LayoutNodePort as set by its [parentLayoutNode]. Only the
     * [parentLayoutNode] will set this value during its [layout].
     */
    var x = 0
        private set

    /**
     * The y position of this LayoutNodePort as set by its [parentLayoutNode]. Only the
     * [parentLayoutNode] will set this value during its [layout].
     */
    var y = 0
        private set

    /**
     * Map from a child to the first [LayoutNodePort] within that child. The [parentLayoutNode] of
     * that [LayoutNodePort] will be this. There will be no entry for a child if there is no
     * [LayoutNodePort] within the child's hierarchy. This is only valid between attach() and
     * detach()
     */
    val layoutChildren = mutableMapOf<PortComponentNode, LayoutNodePort?>()

    /**
     * This is the LayoutNodePort ancestor that contains this LayoutNodePort. This will be `null`
     * for the root [LayoutNodePort]. It is only valid between attach() and detach().
     */
    var parentLayoutNode: LayoutNodePort? = null

    override val size: Int
        get() = children.size

    override fun get(index: Int): PortComponentNode = children[index]

    override fun add(index: Int, child: PortComponentNode) {
        children.add(index, child)
        super.add(index, child)
        if (owner != null) {
            dirtyLayout()
        }
    }

    override fun remove(index: Int, count: Int) {
        super.remove(index, count)
        for (i in index + count - 1 downTo index) {
            children.removeAt(i)
        }
    }

    override fun move(from: Int, to: Int, count: Int) {
        assert(from >= 0)
        assert(to >= 0)
        assert(count > 0)
        // Do the simple thing for now. We can improve efficiency later if we need to
        val removed = ArrayList<PortComponentNode>(count)
        for (i in from until from + count) {
            removed += children[i]
        }
        children.removeAll(removed)

        children.addAll(to, removed)
        dirtyLayout()
    }

    override fun attach(owner: PortOwner) {
        super.attach(owner)

        // Find the LayoutNodePort ancestor and its direct child
        var nodeParent: PortComponentNode? = this
        var node: PortComponentNode
        do {
            node = nodeParent!!
            nodeParent = node.parent
        } while (nodeParent != null && nodeParent !is LayoutNodePort)

        if (nodeParent is LayoutNodePort) {
            // Change the layoutChildren of the ancestor LayoutNodePort
            nodeParent.layoutChildren[node] = this
            parentLayoutNode = nodeParent
        }
        owner.onRequestLayout(this)
    }

    override fun detach() {
        needsLayout = false
        val parentLayoutNode = this.parentLayoutNode
        if (parentLayoutNode != null) {
            val entry = parentLayoutNode.layoutChildren.entries.find { it.value == this }
                ?: throw IllegalStateException()
            parentLayoutNode.layoutChildren.remove(entry.key)
            this.parentLayoutNode = null
        }
        super.detach()
    }

    override fun visitChildren(block: (PortComponentNode) -> Unit) {
        children.forEach(block)
    }

    /**
     * Indicate that the LayoutNodePort's layout is dirty and should be updated.
     * The [layout] call will be scheduled to execute at a later point.
     * This call is necessary because we don't have a reconciliation step yet. Normally
     * the layout would automatically be updated during reconciliation.
     */
    fun dirtyLayout() {
        val owner = this.owner
        if (!needsLayout && owner != null) {
            owner.onRequestLayout(this)
        }
        needsLayout = true
    }

    /**
     * Changes the position of a LayoutNodePort within its LayoutNodePort [parent].
     */
    fun position(child: PortComponentNode, x: Int, y: Int) {
        assert(children.contains(child))
        val layoutNode = layoutChildren[child] ?: return // non-layout child has no position

        if (x != layoutNode.x || y != layoutNode.y) {
            layoutNode.x = x
            layoutNode.y = y
            owner?.onPositionChange(layoutNode)
        }
    }

    companion object {
        /**
         * This does the measurement of a layout. Consider:
         *
         *   <MeasureBox> constraints, parentUsesSize ->
         *       val measuredText = measure { <Text text="Hello World"/> }
         *       <Arrange size=measuredText.size>
         *           <measuredText x=0 y=0/>
         *       </Arrange>
         *   </MeasureBox>
         *
         * This returns the result of the measure call. This code will likely disappear when
         * R4A completes its layout reconciliation.
         */
        fun measure(
            node: PortComponentNode,
            constraints: Constraints,
            parentUsesSize: Boolean = false
        ): Size {
            System.out.println("Measuring $node")
            var child = node
            while (child !is LayoutNodePort) {
                if (child.size == 0) {
                    return Size.zero
                }
                child = child[0]
            }
            child.parentUsesSize = parentUsesSize
            if (constraints != child.constraints || child.needsLayout) {
                child.constraints = constraints
                val measuredSize = child.onLayout(child, constraints, parentUsesSize)
                System.out.println("size of $node = $measuredSize")
                child.needsLayout = false
                val width = measuredSize.width.roundToInt()
                val height = measuredSize.height.roundToInt()
                if (width != child.width || height != child.height) {
                    child.width = width
                    child.height = height
                    child.owner?.onSizeChange(child)
                }
            }

            return Size(child.width.toFloat(), child.height.toFloat())
        }
    }
}

/**
 * Removes all the children within the hierarchy
 */
internal fun PortComponentNode.removeChildren() {
    for (i in size - 1 downTo 0) {
        get(i).removeChildren()
        remove(i)
    }
}

/**
 * Inserts a child [PortComponentNode] at a last index. If this PortComponentNode [isAttached]
 * then [child] will become [attach]ed also. [child] must have a `null` [parent].
 */
internal fun PortComponentNode.add(child: PortComponentNode) {
    add(size, child)
}