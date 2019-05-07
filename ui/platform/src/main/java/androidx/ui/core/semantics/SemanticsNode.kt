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

package androidx.ui.core.semantics

import androidx.ui.core.ifDebug
import androidx.ui.engine.geometry.Rect

/**
 * Signature for a function that is called for each [SemanticsNode].
 *
 * Return false to stop visiting nodes.
 *
 * Used by [SemanticsNode.visitChildren].
 */
internal typealias SemanticsNodeVisitor = (node: SemanticsNode) -> Boolean

/**
 * A node that represents some semantic data.
 *
 * The semantics tree is maintained during the semantics phase of the pipeline
 * (i.e., during [PipelineOwner.flushSemantics]), which happens after
 * compositing. The semantics tree is then uploaded into the engine for use
 * by assistive technology.
 */
class SemanticsNode private constructor(
    /**
     * The unique identifier for this node.
     *
     * The root node has an id of zero. Other nodes are given a unique id when
     * they are created.
     */
    val id: Int
) {

    companion object {
        private var lastIdentifier: Int = 0
        private fun generateNewId(): Int {
            lastIdentifier += 1
            return lastIdentifier
        }

        fun root(
            owner: SemanticsOwner
        ): SemanticsNode {
            val node = SemanticsNode(0)
            node.attach(owner)
            return node
        }

        /**
         * In tests use this function to reset the counter used to generate
         * [SemanticsNode.id].
         */
        internal fun debugResetSemanticsIdCounter() {
            lastIdentifier = 0
        }
    }

    /**
     * Creates a semantic node.
     *
     * Each semantic node has a unique identifier that is assigned when the node
     * is created.
     */
    constructor() : this(generateNewId())

    // GEOMETRY

    /** The bounding box for this node in its coordinate system. */
    var rect: Rect = Rect.zero
        set(value) {
            if (field != value) {
                field = value
                markDirty()
            }
        }

    /**
     * The semantic clip from an ancestor that was applied to this node.
     *
     * Expressed in the coordinate system of the node. May be null if no clip has
     * been applied.
     *
     * Descendant [SemanticsNode]s that are positioned outside of this rect will
     * be excluded from the semantics tree. Descendant [SemanticsNode]s that are
     * overlapping with this rect, but are outside of [parentPaintClipRect] will
     * be included in the tree, but they will be marked as hidden because they
     * are assumed to be not visible on screen.
     *
     * If this rect is null, all descendant [SemanticsNode]s outside of
     * [parentPaintClipRect] will be excluded from the tree.
     *
     * If this rect is non-null it has to completely enclose
     * [parentPaintClipRect]. If [parentPaintClipRect] is null this property is
     * also null.
     */
    var parentSemanticsClipRect: Rect? = null

    /**
     * The paint clip from an ancestor that was applied to this node.
     *
     * Expressed in the coordinate system of the node. May be null if no clip has
     * been applied.
     *
     * Descendant [SemanticsNode]s that are positioned outside of this rect will
     * either be excluded from the semantics tree (if they have no overlap with
     * [parentSemanticsClipRect]) or they will be included and marked as hidden
     * (if they are overlapping with [parentSemanticsClipRect]).
     *
     * This rect is completely enclosed by [parentSemanticsClipRect].
     *
     * If this rect is null [parentSemanticsClipRect] also has to be null.
     */
    var parentPaintClipRect: Rect? = null

    /**
     * Whether the node is invisible.
     *
     * A node whose [rect] is outside of the bounds of the screen and hence not
     * reachable for users is considered invisible if its semantic information
     * is not merged into a (partially) visible parent as indicated by
     * [isMergedIntoParent].
     *
     * An invisible node can be safely dropped from the semantic tree without
     * loosing semantic information that is relevant for describing the content
     * currently shown on screen.
     */
    val isInvisible: Boolean
        get() = !isMergedIntoParent && rect.isEmpty()

    // MERGING

    /** Whether this node merges its semantic information into an ancestor node. */
    var isMergedIntoParent: Boolean = false
        set(value) {
            if (field == value)
                return
            field = value
            markDirty()
        }

    /**
     * Whether this node is taking part in a merge of semantic information.
     *
     * This returns true if the node is either merged into an ancestor node or if
     * decedent nodes are merged into this node.
     *
     * See also:
     *
     *  * [isMergedIntoParent]
     *  * [mergeAllDescendantsIntoThisNode]
     */
    val isPartOfNodeMerging
        get() = mergeAllDescendantsIntoThisNode || isMergedIntoParent

    /** Whether this node and all of its descendants should be treated as one logical entity. */
    var mergeAllDescendantsIntoThisNode = false
        private set

    // CHILDREN

    /** Contains the children in inverse hit test order (i.e. paint order). */
    var children: List<SemanticsNode> = emptyList()

    /** Whether this node has a non-zero number of children. */
    val hasChildren
        get() = children.isNotEmpty()

    /** The number of children this node has. */
    val childrenCount
        get() = children.size

    /**
     * Mark the given node as being a child of this node.
     */
    private fun adoptChild(child: SemanticsNode) {
        assert(child.parent == null)
        ifDebug {
            var node: SemanticsNode? = this
            while (node!!.parent != null)
                node = node.parent
            assert(node != child) // indicates we are about to create a cycle
        }
        child.parent = this
        if (attached)
            child.attach(owner!!)
        redepthChild(child)
    }

    /**
     * Disconnect the given node from this node.
     */
    private fun dropChild(child: SemanticsNode) {
        assert(child.parent == this)
        assert(child.attached == attached)
        child.parent = null
        if (attached)
            child.detach()
    }

    /**
     * Visits the immediate children of this node.
     *
     * This function calls visitor for each immediate child until visitor returns
     * false.
     */
    private fun visitChildren(visitor: SemanticsNodeVisitor) {
        children.forEach {
            if (!visitor(it)) {
                return
            }
        }
    }

    /**
     * Visit all the descendants of this node.
     *
     * This function calls visitor for each descendant in a pre-order traversal
     * until visitor returns false. Returns true if all the visitor calls
     * returned true, otherwise returns false.
     */
    internal fun visitDescendants(visitor: SemanticsNodeVisitor): Boolean {
        children.forEach {
            if (!visitor(it) || !it.visitDescendants(visitor))
                return false
        }
        return true
    }

    /**
     * The owner for this node (null if unattached).
     *
     * The entire subtree that this node belongs to will have the same owner.
     */
    var owner: SemanticsOwner? = null
        private set

    /**
     * Whether this node is in a tree whose root is attached to something.
     *
     * This becomes true during the call to [attach].
     *
     * This becomes false during the call to [detach].
     */
    val attached get() = owner != null

    /** The parent of this node in the tree. */
    var parent: SemanticsNode? = null

    /**
     * The depth of this node in the tree.
     *
     * The depth of nodes in a tree monotonically increases as you traverse down
     * the tree.
     */
    var depth = 0
        private set

    val config = SemanticsConfiguration()

    /**
     * Adjust the [depth] of the given [child] to be greater than this node's own
     * [depth].
     */
    fun redepthChild(child: SemanticsNode) {
        assert(child.owner == owner)
        if (child.depth <= depth) {
            child.depth = depth + 1
            child.redepthChildren()
        }
    }

    fun redepthChildren() {
        children.forEach(::redepthChild)
    }

    // TODO(ryanmentley): Document the proper usage of attach/detach once they're more solidified
    private fun attach(owner: SemanticsOwner) {
        assert(this.owner == null)
        this.owner = owner
        assert(!owner.nodes.containsKey(id))
        owner.nodes[id] = this
        owner.detachedNodes.remove(this)
        if (dirty) {
            dirty = false
            markDirty()
        }
        children.let {
            for (child in it) {
                child.attach(owner)
            }
        }
    }

    // TODO(ryanmentley): Should we make this API idempotent so that it works if detached
    // more than once?
    private fun detach() {
        assert(owner != null)

        owner!!.let {
            assert(it.nodes.containsKey(id))
            assert(!it.detachedNodes.contains(this))
            it.nodes.remove(id)
            it.detachedNodes.add(this)
        }
        owner = null
        assert(parent == null || attached == parent!!.attached)

        for (child in children) {
            // The list of children may be stale and may contain nodes that have
            // been assigned to a different parent.
            if (child.parent == this) {
                child.detach()
            }
        }

        // The other side will have forgotten this node if we ever send
        // it again, so make sure to mark it dirty so that it'll get
        // sent if it is resurrected.
        markDirty()
    }

    private var dirty: Boolean = false

    private fun markDirty() {
        if (dirty) {
            return
        }
        dirty = true
        if (attached) {
            owner!!.let {
                assert(!it.detachedNodes.contains(this))
                it.dirtyNodes.add(this)
            }
        }
    }

    fun isDifferentFromCurrentSemanticAnnotation(config: SemanticsConfiguration): Boolean {
        return this.config.isSemanticallyDifferentFrom(config) ||
                mergeAllDescendantsIntoThisNode != config.isMergingSemanticsOfDescendants
    }

    internal fun canPerformAction(action: SemanticsActionType<*>) =
        this.config._actions.containsKey(action)
}
