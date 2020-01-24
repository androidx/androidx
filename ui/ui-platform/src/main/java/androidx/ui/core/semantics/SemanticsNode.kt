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

import androidx.ui.core.ComponentNode
import androidx.ui.core.SemanticsComponentNode
import androidx.ui.core.boundsInRoot
import androidx.ui.core.findChildSemanticsComponentNodes
import androidx.ui.core.findFirstLayoutNodeInTree
import androidx.ui.core.globalBounds
import androidx.ui.core.ifDebug
import androidx.ui.core.requireFirstLayoutNodeInTree
import androidx.ui.semantics.AccessibilityAction
import androidx.ui.semantics.SemanticsPropertyKey
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxBounds

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
 */
class SemanticsNode internal constructor(
    /**
     * The unique identifier for this node.
     *
     * The root node has an id of zero. Other nodes are given a unique id when
     * they are created.
     */
    val id: Int,
    unmergedConfig: SemanticsConfiguration,
    // TODO(b/144404665): Testing currently mandates this be public - should it be?
    var componentNode: ComponentNode
) {
    var unmergedConfig: SemanticsConfiguration = unmergedConfig
        private set

    private var dirty: Boolean = false

    companion object {
        // TODO(b/145955412) maybe randomize? don't want this to be a contract
        //  (and if you're reading the source, fair warning: this may change unpredictably)
        // TODO: Might need to be atomic for multi-threaded composition
        private var lastIdentifier: Int = 2

        // TODO(b/145955062): This should be private, but needs to be accessed across modules
        //  (from framework)
        fun generateNewId(): Int {
            lastIdentifier += 1
            return lastIdentifier
        }

        internal fun root(
            owner: SemanticsOwner,
            config: SemanticsConfiguration,
            componentNode: ComponentNode
        ): SemanticsNode {
            val node = SemanticsNode(generateNewId(), config, componentNode)
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
    constructor(unmergedConfig: SemanticsConfiguration, componentNode: SemanticsComponentNode) :
            this(generateNewId(), unmergedConfig, componentNode)

    // GEOMETRY

    /** The size of the bounding box for this node */
    val size: IntPxSize
        get() {
            val layoutNode = componentNode.requireFirstLayoutNodeInTree()
            return layoutNode.coordinates.size
        }

    /** The bounding box for this node relative to the root of this Compose hierarchy */
    val boundsInRoot: PxBounds
        get() {
            val layoutNode = componentNode.requireFirstLayoutNodeInTree()
            return layoutNode.coordinates.boundsInRoot
        }

    val globalBounds: PxBounds
        get() {
            val layoutNode = componentNode.requireFirstLayoutNodeInTree()
            return layoutNode.coordinates.globalBounds
        }

    // MERGING
    val isSemanticBoundary: Boolean
        get() = unmergedConfig.isSemanticBoundary

    private var mergedParent: SemanticsNode? = null

    private var mergedConfig: SemanticsConfiguration? = null
    /**
     * The merged configuration of this node
     */
    val config: SemanticsConfiguration
        get() {
            ensureMergedParentAndConfig()
            return mergedConfig!!
        }

    /**
     * Contract: After this returns, [mergedParent] and [mergedConfig] are both non-null
     */
    private fun ensureMergedParentAndConfig() {
        var config = mergedConfig
        if (config == null) {
            config = buildMergedConfig()
            mergedConfig = config
        }

        dirty = false
    }

    /**
     * Contract: After this returns, [mergedParent] is non-null
     */
    private fun buildMergedConfig(
        parentNode: SemanticsNode? = null,
        mergedConfigFromParent: SemanticsConfiguration? = null,
        mergeAllChildren: Boolean = false
    ): SemanticsConfiguration {
        // The forced merging might not start at the top-level node,
        // so we need to check at each level
        @Suppress("NAME_SHADOWING")
        val mergeAllChildren = mergeAllChildren || mergeAllDescendantsIntoThisNode

        val mergedConfig: SemanticsConfiguration
        if (mergedConfigFromParent == null || parentNode == null) {
            check(isSemanticBoundary) {
                "Attempting to build a merged configuration " +
                        "starting from a node that is not a semantic boundary"
            }

            // We are a semantic boundary - start by copying our configuration so that we can add
            // our children's configuration to it
            mergedConfig = unmergedConfig.copy()
            mergedParent = this
        } else {
            // We're being merged into our parent - add our node's data
            mergedConfig = mergedConfigFromParent
            mergedParent = parentNode
            // If we are forcibly merging, we want to ignore conflicts
            mergedConfig.absorb(unmergedConfig, ignoreAlreadySet = mergeAllChildren)
        }

        // Collect semantic information from children.
        // Order is significant here because we will attempt to merge duplicate keys.
        // This affects, for instance, the label text.
        for (child in unmergedChildren) {
            if (child.isSemanticBoundary && !mergeAllChildren) {
                // Don't merge anything that crosses a semantic boundary. They will create
                // their own SemanticsNodes, so we ignore them here.
                // This doesn't apply if we've been explicitly told to merge all children.
                continue
            }

            // Recursively walk down the tree and collect child data
            child.buildMergedConfig(
                parentNode = this,
                mergedConfigFromParent = mergedConfig,
                mergeAllChildren = mergeAllChildren
            )
        }

        return mergedConfig
    }

    /** Whether this node and all of its descendants should be treated as one logical entity. */
    private val mergeAllDescendantsIntoThisNode: Boolean
        get() = unmergedConfig.isMergingSemanticsOfDescendants

    // CHILDREN

    private var _unmergedChildren: MutableList<SemanticsNode>? = null
    val unmergedChildren: List<SemanticsNode>
        get() {
            var unmergedChildren = _unmergedChildren
            if (unmergedChildren == null) {
                unmergedChildren = mutableListOf()
                // TODO(ryanmentley): Should this require the layout child?
                val semanticsChildren =
                    componentNode.findFirstLayoutNodeInTree()?.findChildSemanticsComponentNodes()
                        ?: emptyList()
                for (semanticsChild in semanticsChildren) {
                    // This is eager - if desired, we could make this lazier
                    unmergedChildren.add(semanticsChild.semanticsNode)
                    adoptChild(semanticsChild.semanticsNode)
                    markDirty()
                }
                _unmergedChildren = unmergedChildren
            }

            return unmergedChildren
        }

    private var _children: List<SemanticsNode>? = null
    /** Contains the children in inverse hit test order (i.e. paint order). */
    val children: List<SemanticsNode>
        get() {
            check(isSemanticBoundary) {
                "Requested merged children " +
                        "from a node that is not a semantic boundary"
            }

            var children = _children
            if (children == null) {
                children = buildMergedChildren()
                _children = children
            }

            return children
        }

    private fun buildMergedChildren(childrenFromParent: MutableList<SemanticsNode>? = null):
            List<SemanticsNode> {
        if (mergeAllDescendantsIntoThisNode) {
            // All of our descendants will be merged, so we have no children after merging
            return emptyList()
        }

        val mergedChildren = childrenFromParent ?: mutableListOf()

        // The merged children are the set of indirect children that are semantic boundaries
        for (child in unmergedChildren) {
            if (child.isSemanticBoundary) {
                // Add the child, don't recurse - we don't want to cross the semantic boundary
                mergedChildren += child
            } else {
                // Search recursively, depth-first (so that the child order matches)
                child.buildMergedChildren(mergedChildren)
            }
        }

        return mergedChildren
    }

    /** Whether this node has a non-zero number of children. */
    val hasChildren
        get() = children.isNotEmpty()

    /**
     * Mark the given node as being a child of this node.
     */
    private fun adoptChild(child: SemanticsNode) {
        check(child.parent == null)
        ifDebug {
            var node: SemanticsNode = this
            while (node.parent != null) {
                node = node.parent!!
                check(node != child) // indicates we are about to create a cycle
            }
        }
        child.parent = this
        if (attached) {
            child.attach(owner!!)
        }
        markDirty()
    }

    /**
     * Disconnect the given node from this node.
     */
    private fun dropChild(child: SemanticsNode) {
        check(child.parent == this)
        assert(child.attached == attached)
        child.parent = null
        if (child.attached) {
            child.detach()
        }
        _unmergedChildren?.remove(child)
        markDirty()
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
    internal var owner: SemanticsOwner? = null

    /**
     * Whether this node is in a tree whose root is attached to something.
     *
     * This becomes true during the call to [attach].
     *
     * This becomes false during the call to [detach].
     */
    val attached get() = owner != null

    /** The parent of this node in the tree. */
    // TODO(b/145947383): this needs to be the *merged* parent
    var parent: SemanticsNode? = null

    // TODO(ryanmentley): Document the proper usage of attach/detach once they're more solidified
    internal fun attach(owner: SemanticsOwner) {
        assert(!attached)
        this.owner = owner
        owner.onAttach(this)
        for (child in unmergedChildren) {
            child.attach(owner)
        }
    }

    // TODO(ryanmentley): Should we make this API idempotent so that it works if detached
    //  more than once?
    /**
     * Detaches the node from its owner.
     *
     * Note: this does *not* detach it from its parent, so a node's attached state should always
     * match its parent's
     */
    internal fun detach() {
        check(attached)

        owner!!.onDetach(this)
        owner = null

        check(parent == null || attached == parent!!.attached) {
            "attached: $attached, parent.attached: ${parent?.attached}"
        }

        _unmergedChildren?.let {
            for (child in it) {
                // The list of children may be stale and may contain nodes that have
                // been assigned to a different parent.
                if (child.parent == this) {
                    child.detach()
                }
            }
        }

        // The other side will have forgotten this node if we ever send
        // it again, so make sure to mark it dirty so that it'll get
        // sent if it is resurrected.
        markDirty()
    }

    internal fun invalidateChildren() {
        val localUnmergedChildren = _unmergedChildren
        // Clear unmerged children
        // TODO(ryanmentley): probably eventually needs to be smarter and invalidate things
        if (localUnmergedChildren != null) {
            // copy because it will change as we drop
            for (child in ArrayList(localUnmergedChildren)) {
                dropChild(child)
            }
        }
        // Will be regenerated from the ComponentNode
        _unmergedChildren = null

        markDirty()
        parent?.invalidateChildren()
    }

    internal fun markDirty() {
        // Mark the merged parent dirty, if we have one, as it needs to be regenerated with new
        // changes.  Note - we could be our own merged parent if we are the semantic boundary node
        if (mergedParent != this) {
            mergedParent?.markDirty()
        }
        mergedParent = null

        // Will be regenerated from unmergedChildren
        _children = null

        // Will be regenerated from unmergedConfig and unmergedChildren
        mergedConfig = null

        if (attached) {
            owner!!.onNodeMarkedDirty(this)
        }
    }

    internal fun <T : Function<Unit>> canPerformAction(
        action: SemanticsPropertyKey<AccessibilityAction<T>>
    ) =
        this.config.contains(action)
}
