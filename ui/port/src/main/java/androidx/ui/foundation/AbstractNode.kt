package androidx.ui.foundation

import androidx.annotation.CallSuper
import androidx.ui.assert

/**
 * An abstract node in a tree.
 *
 * AbstractNode has as notion of depth, attachment, and parent, but does not
 * have a model for children.
 *
 * When a subclass is changing the parent of a child, it should call either
 * `parent.adoptChild(child)` or `parent.dropChild(child)` as appropriate.
 * Subclasses can expose an API for manipulating the tree if desired (e.g. a
 * setter for a `child` property, or an `add()` method to manipulate a list).
 *
 * The current parent node is exposed by the [parent] property.
 *
 * The current attachment state is exposed by [attached]. The root of any tree
 * that is to be considered attached should be manually attached by calling
 * [attach]. Other than that, the [attach] and [detach] methods should not be
 * called directly; attachment is managed automatically by the aforementioned
 * [adoptChild] and [dropChild] methods.
 *
 * Subclasses that have children must override [attach] and [detach] as
 * described in the documentation for those methods.
 *
 * Nodes always have a [depth] greater than their ancestors'. There's no
 * guarantee regarding depth between siblings. The depth of a node is used to
 * ensure that nodes are processed in depth order. The [depth] of a child can
 * be more than one greater than the [depth] of the parent, because the [depth]
 * values are never decreased: all that matters is that it's greater than the
 * parent. Consider a tree with a root node A, a child B, and a grandchild C.
 * Initially, A will have [depth] 0, B [depth] 1, and C [depth] 2. If C is
 * moved to be a child of A, sibling of B, then the numbers won't change. C's
 * [depth] will still be 2. The [depth] is automatically maintained by the
 * [adoptChild] and [dropChild] methods.
 */
abstract class AbstractNode : Comparable<AbstractNode> {

    /**
     * The depth of this node in the tree.
     *
     * The depth of nodes in a tree monotonically increases as you traverse down
     * the tree.
     */
    var depth = 0
        private set

    override fun compareTo(other: AbstractNode): Int {
        return this.depth - other.depth
    }

    /**
     * Adjust the [depth] of the given [child] to be greater than this node's own
     * [depth].
     *
     * Only call this method from overrides of [redepthChildren].
     */
    protected fun redepthChild(child: AbstractNode) {
        assert(child.owner == owner)
        if (child.depth <= depth) {
            child.depth = depth + 1
            child.redepthChildren()
        }
    }

    /**
     * Adjust the [depth] of this node's children, if any.
     *
     * Override this method in subclasses with child nodes to call [redepthChild]
     * for each child. Do not call this method directly.
     */
    open fun redepthChildren() { }

    internal var _owner: Any? = null

    /**
     * The owner for this node (null if unattached).
     *
     * The entire subtree that this node belongs to will have the same owner.
     */
    open val owner: Any? get() = _owner

    /**
     * Whether this node is in a tree whose root is attached to something.
     *
     * This becomes true during the call to [attach].
     *
     * This becomes false during the call to [detach].
     */
    val attached get() = owner != null

    /**
     * Mark this node as attached to the given owner.
     *
     * Typically called only from the [parent]'s [attach] method, and by the
     * [owner] to mark the root of a tree as attached.
     *
     * Subclasses with children should override this method to first call their
     * inherited [attach] method, and then [attach] all their children to the
     * same [owner].
     */
    @CallSuper
    open fun attach(owner: Any) { // TODO(Migration/Filip): Removed covariant
        assert(this.owner == null)
        this._owner = owner
    }

    /**
     * Mark this node as detached.
     *
     * Typically called only from the [parent]'s [detach], and by the [owner] to
     * mark the root of a tree as detached.
     *
     * Subclasses with children should override this method to first call their
     * inherited [detach] method, and then [detach] all their children.
     */
    @CallSuper
    open fun detach() {
        assert(owner != null)
        _owner = null
        assert(parent == null || attached == parent!!.attached)
    }

    // TODO(Migration/ryanmentley): The use of types and overriding here is kind of messy.
    // It requires private backing properties to get around type requirements, which feels like
    // a hack.
    private var _parent: AbstractNode? = null
    /** The parent of this node in the tree. */
    open val parent: AbstractNode?
        get() = _parent

    /**
     * Mark the given node as being a child of this node.
     *
     * Subclasses should call this function when they acquire a new child.
     */
    @CallSuper
    open fun adoptChild(child: AbstractNode) { // TODO(Migration/Filip): Removed covariant
        assert(child.parent == null)
        assert {
            var node: AbstractNode? = this
            while (node!!.parent != null)
                node = node.parent
            assert(node != child) // indicates we are about to create a cycle
            true
        }
        child._parent = this
        if (attached)
            child.attach(owner!!)
        redepthChild(child)
    }

    /**
     * Disconnect the given node from this node.
     *
     * Subclasses should call this function when they lose a child.
     */
    @CallSuper
    protected open fun dropChild(child: AbstractNode) { // TODO(Migration/Filip): Removed covariant
        assert(child.parent == this)
        assert(child.attached == attached)
        child._parent = null
        if (attached)
            child.detach()
    }
}
