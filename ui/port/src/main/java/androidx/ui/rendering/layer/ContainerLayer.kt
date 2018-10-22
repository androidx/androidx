package androidx.ui.rendering.layer

import androidx.annotation.CallSuper
import androidx.ui.assert
import androidx.ui.compositing.SceneBuilder
import androidx.ui.engine.geometry.Offset
import androidx.ui.foundation.diagnostics.DiagnosticsNode
import androidx.ui.vectormath64.Matrix4

/**
 * A composited layer that has a list of children.
 *
 * A [ContainerLayer] instance merely takes a list of children and inserts them
 * into the composited rendering in order. There are subclasses of
 * [ContainerLayer] which apply more elaborate effects in the process.
 */
open class ContainerLayer : Layer() {

    /** The first composited layer in this layer's child list. */
    internal var firstChild: Layer? = null
    /** The last composited layer in this layer's child list. */
    internal var lastChild: Layer? = null

    private fun _debugUltimatePreviousSiblingOf(child: Layer, equals: Layer? = null): Boolean {
        var curChild = child
        assert(curChild.attached == attached)
        while (curChild.previousSibling != null) {
            assert(curChild.previousSibling != curChild)
            curChild = curChild.previousSibling!!
            assert(curChild.attached == attached)
        }
        return curChild == equals
    }

    private fun _debugUltimateNextSiblingOf(child: Layer, equals: Layer? = null): Boolean {
        var curChild = child
        assert(curChild.attached == attached)
        while (curChild.nextSibling != null) {
            assert(curChild.nextSibling != curChild)
            curChild = curChild.nextSibling!!
            assert(curChild.attached == attached)
        }
        return curChild == equals
    }

    override fun attach(owner: Any) {
        super.attach(owner)
        var child = firstChild
        while (child != null) {
            child.attach(owner)
            child = child.nextSibling
        }
    }

    override fun detach() {
        super.detach()
        var child = firstChild
        while (child != null) {
            child.detach()
            child = child.nextSibling
        }
    }

    /** Adds the given layer to the end of this layer's child list. */
    fun append(child: Layer) {
        assert(child != this)
        assert(child != firstChild)
        assert(child != lastChild)
        assert(child.parent == null)
        assert(!child.attached)
        assert(child.nextSibling == null)
        assert(child.previousSibling == null)
        assert {
            var node = this
            while (node.parent != null)
                node = node.parentContainer!!
            assert(node != child) // indicates we are about to create a cycle
            true
        }
        adoptChild(child)
        child.previousSibling = lastChild
        lastChild?.nextSibling = child
        lastChild = child
        firstChild = firstChild ?: child
        assert(child.attached == attached)
    }

    // Implementation of [Layer.remove].
    internal fun _removeChild(child: Layer) {
        assert(child.parent == this)
        assert(child.attached == attached)
        assert(_debugUltimatePreviousSiblingOf(child, equals = firstChild))
        assert(_debugUltimateNextSiblingOf(child, equals = lastChild))
        if (child.previousSibling == null) {
            assert(firstChild == child)
            firstChild = child.nextSibling
        } else {
            child.previousSibling!!.nextSibling = child.nextSibling
        }
        if (child.nextSibling == null) {
            assert(lastChild == child)
            lastChild = child.previousSibling
        } else {
            child.nextSibling!!.previousSibling = child.previousSibling
        }
        assert((firstChild == null) == (lastChild == null))
        assert(firstChild == null || firstChild!!.attached == attached)
        assert(lastChild == null || lastChild!!.attached == attached)
        assert(firstChild == null || _debugUltimateNextSiblingOf(firstChild!!, lastChild))
        assert(lastChild == null || _debugUltimatePreviousSiblingOf(lastChild!!, firstChild))
        child.previousSibling = null
        child.nextSibling = null
        dropChild(child)
        assert(!child.attached)
    }

    /** Removes all of this layer's children from its child list. */
    fun removeAllChildren() {
        var child = firstChild
        while (child != null) {
            val next = child.nextSibling
            child.previousSibling = null
            child.nextSibling = null
            assert(child.attached == attached)
            dropChild(child)
            child = next
        }
        firstChild = null
        lastChild = null
    }

    override fun addToScene(builder: SceneBuilder, layerOffset: Offset) {
        addChildrenToScene(builder, layerOffset)
    }

    /**
     * Uploads all of this layer's children to the engine.
     *
     * This method is typically used by [addToScene] to insert the children into
     * the scene. Subclasses of [ContainerLayer] typically override [addToScene]
     * to apply effects to the scene using the [SceneBuilder] API, then insert
     * their children using [addChildrenToScene], then reverse the aforementioned
     * effects before returning from [addToScene].
     */
    fun addChildrenToScene(builder: SceneBuilder, childOffset: Offset) {
        var child = firstChild
        while (child != null) {
            child.addToScene(builder, childOffset)
            child = child.nextSibling
        }
    }

    /**
     * Applies the transform that would be applied when compositing the given
     * child to the given matrix.
     *
     * Specifically, this should apply the transform that is applied to child's
     * _origin_. When using [applyTransform] with a chain of layers, results will
     * be unreliable unless the deepest layer in the chain collapses the
     * `layerOffset` in [addToScene] to zero, meaning that it passes
     * [Offset.zero] to its children, and bakes any incoming `layerOffset` into
     * the [SceneBuilder] as (for instance) a transform (which is then also
     * included in the transformation applied by [applyTransform]).
     *
     * For example, if [addToScene] applies the `layerOffset` and then
     * passes [Offset.zero] to the children, then it should be included in the
     * transform applied here, whereas if [addToScene] just passes the
     * `layerOffset` to the child, then it should not be included in the
     * transform applied here.
     *
     * This method is only valid immediately after [addToScene] has been called,
     * before any of the properties have been changed.
     *
     * The default implementation does nothing, since [ContainerLayer], by
     * default, composites its children at the origin of the [ContainerLayer]
     * itself.
     *
     * The `child` argument should generally not be null, since in principle a
     * layer could transform each child independently. However, certain layers
     * may explicitly allow null as a value, for example if they know that they
     * transform all their children identically.
     *
     * The `transform` argument must not be null.
     *
     * Used by [FollowerLayer] to transform its child to a [LeaderLayer]'s
     * position.
     */
    open fun applyTransform(child: Layer, transform: Matrix4) {
    }

    @CallSuper
    override fun debugDescribeChildren(): List<DiagnosticsNode> {
        val children = mutableListOf<DiagnosticsNode>()
        if (firstChild == null)
            return children
        var child: Layer = firstChild!!
        var count = 1
        while (true) {
            children.add(child.toDiagnosticsNode(name = "child $count"))
            if (child == lastChild) {
                break
            }
            count += 1
            child = child.nextSibling!!
        }
        return children
    }
}