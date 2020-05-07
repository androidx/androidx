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
import androidx.ui.core.LayoutNode
import androidx.ui.core.boundsInRoot
import androidx.ui.core.findClosestParentNode
import androidx.ui.core.globalBounds
import androidx.ui.core.globalPosition
import androidx.ui.semantics.AccessibilityAction
import androidx.ui.semantics.SemanticsPropertyKey
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxBounds
import androidx.ui.unit.PxPosition

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
    val unmergedConfig: SemanticsConfiguration,
    // TODO(b/144404665): Testing currently mandates this be public - should it be?
    var componentNode: ComponentNode
) {
    private var dirty: Boolean = false

    companion object {
        // TODO(b/145955412) maybe randomize? don't want this to be a contract
        // TODO: Might need to be atomic for multi-threaded composition
        private var lastIdentifier: Int = 2

        // TODO(b/145955062): This should be private, but needs to be accessed across modules
        //                    (from framework)
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
            node.owner = owner
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
    internal constructor(unmergedConfig: SemanticsConfiguration, componentNode: ComponentNode) :
            this(generateNewId(), unmergedConfig, componentNode)

    // GEOMETRY

    /** The size of the bounding box for this node */
    val size: IntPxSize
        get() {
            val layoutNode = componentNode.requireFirstNonSemanticsNodeInTree()
            return layoutNode.coordinates.size
        }

    /** The bounding box for this node relative to the root of this Compose hierarchy */
    val boundsInRoot: PxBounds
        get() {
            val layoutNode = componentNode.requireFirstNonSemanticsNodeInTree()
            return layoutNode.coordinates.boundsInRoot
        }

    val globalBounds: PxBounds
        get() {
            val layoutNode = componentNode.requireFirstNonSemanticsNodeInTree()
            return layoutNode.coordinates.globalBounds
        }

    val globalPosition: PxPosition
        get() {
            val layoutNode = componentNode.requireFirstNonSemanticsNodeInTree()
            return layoutNode.coordinates.globalPosition
        }

    // MERGING
    val isSemanticBoundary: Boolean
        get() = unmergedConfig.isSemanticBoundary

    /**
     * The merged configuration of this node
     */
    // TODO(aelias): This is too expensive for a val (full subtree recreation every call);
    //               optimize this when the merging algorithm is improved.
    val config: SemanticsConfiguration
        get() {
            return buildMergedConfig()
        }

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
        } else {
            // We're being merged into our parent - add our node's data
            mergedConfig = mergedConfigFromParent
            // If we are forcibly merging, we want to ignore conflicts
            mergedConfig.absorb(unmergedConfig, ignoreAlreadySet = mergeAllChildren)
        }

        // Collect semantic information from children.
        // Order is significant here because we will attempt to merge duplicate keys.
        // This affects, for instance, the label text.
        for (child in unmergedChildren()) {
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

    private fun unmergedChildren(): List<SemanticsNode> {
        val unmergedChildren: MutableList<SemanticsNode> = mutableListOf()

        val firstNonSemanticsNode = componentNode.findFirstNonSemanticsNodeInTree()
        val semanticsChildren =
            firstNonSemanticsNode?.findOneLayerOfSemanticsWrappers() ?: emptyList()
        for (semanticsChild in semanticsChildren) {
            unmergedChildren.add(semanticsChild.semanticsNode())
        }

        return unmergedChildren
    }

    /** Contains the children in inverse hit test order (i.e. paint order). */
    // TODO(aelias): This is too expensive for a val (full subtree recreation every call);
    //               optimize this when the merging algorithm is improved.
    val children: List<SemanticsNode>
        get() {
            check(isSemanticBoundary) {
                "Requested merged children from a node that is not a semantic boundary"
            }

            return buildMergedChildren()
        }

    private fun buildMergedChildren(childrenFromParent: MutableList<SemanticsNode>? = null):
            List<SemanticsNode> {
        if (mergeAllDescendantsIntoThisNode) {
            // All of our descendants will be merged, so we have no children after merging
            return emptyList()
        }

        val mergedChildren = childrenFromParent ?: mutableListOf()

        // The merged children are the set of indirect children that are semantic boundaries
        for (child in unmergedChildren()) {
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
     * This is only non-null on the root node of the semantics tree.
     */
    internal var owner: SemanticsOwner? = null

    /** The parent of this node in the tree. */
    // TODO(b/145947383): this needs to be the *merged* parent
    val parent: SemanticsNode?
        get() {
            // This searches up the layout tree and takes into account
            // collapsing of adjacent SemanticsWrappers into a single
            // SemanticsNode.
            // Example: if L are normal layout node and S are semantics nodes,
            // and the ComponentNode tree is a simple list-like tree
            // "<ROOT>, S, S, L, S, S, S, L, L, S"
            //          ^        ^              ^
            //          a        b              c
            // then 'c'.parent == 'b', and 'b'.parent == 'a'

            // (This complexity is temporary -- semantics collapsing will be
            // replaced by modifier chains soon.)

            val node = componentNode
                .findClosestParentNode { it.outerSemantics != null }
                ?.findHighestConsecutiveAncestor { it.outerSemantics != null }
            return node?.outerSemantics?.semanticsNode()
        }

    internal fun <T : Function<Boolean>> canPerformAction(
        action: SemanticsPropertyKey<AccessibilityAction<T>>
    ) =
        this.config.contains(action)
}

/**
 * Returns the outermost semantics node on a LayoutNode.
 */
internal val ComponentNode.outerSemantics: SemanticsWrapper?
    get() {
        var wrapper = (this as? LayoutNode)?.layoutNodeWrapper
        while (wrapper != null) {
            if (wrapper is SemanticsWrapper) return wrapper
            wrapper = wrapper.wrapped
        }
        return null
    }

/**
 * Returns the highest in a consecutive chain of this + this's parents all meeting the predicate.
*/
private fun ComponentNode.findHighestConsecutiveAncestor(
    selector: (ComponentNode) -> Boolean
): ComponentNode? {
    if (!selector(this)) return null

    var prev = this
    var currentParent = parent
    while (currentParent != null && selector(currentParent)) {
        prev = currentParent
        currentParent = currentParent.parent
    }
    return prev
}

/**
 * Executes [selector] on every parent of this [SemanticsNode] and returns the closest
 * [SemanticsNode] to return `true` from [selector] or null if [selector] returns false
 * for all ancestors.
 */
fun SemanticsNode.findClosestParentNode(selector: (SemanticsNode) -> Boolean): SemanticsNode? {
    // TODO(b/143866294): move this to the testing side after the hierarchy isn't flattened anymore
    var currentParent = parent
    while (currentParent != null) {
        if (currentParent.isSemanticBoundary && selector(currentParent)) {
            return currentParent
        } else {
            currentParent = currentParent.parent
        }
    }

    return null
}

internal fun SemanticsNode.findChildById(id: Int): SemanticsNode? {
    if (this.id == id) return this
    children.forEach {
        val result = it.findChildById(id)
        if (result != null) return result
    }
    return null
}

private fun ComponentNode.findOneLayerOfSemanticsWrappers(): List<SemanticsWrapper> {
    val childSemanticsComponentNodes = mutableListOf<SemanticsWrapper>()
    for (child in children) {
        findOneLayerOfSemanticsWrappersRecursive(childSemanticsComponentNodes, child)
    }
    return childSemanticsComponentNodes
}

private fun ComponentNode.findOneLayerOfSemanticsWrappersRecursive(
    list: MutableList<SemanticsWrapper>,
    node: ComponentNode
) {
    if (node.outerSemantics != null) {
        list.add(node.outerSemantics!!)
        // Stop, we're done
    } else {
        for (child in node.children) {
            findOneLayerOfSemanticsWrappersRecursive(list, child)
        }
    }
}

private fun ComponentNode.findFirstNonSemanticsNodeInTree(): LayoutNode? {
    if (this is LayoutNode && outerSemantics == null) {
        return this
    }
    visitChildren { child ->
        if (child is LayoutNode && outerSemantics == null) {
            return child
        } else {
            val layoutChild = child.findFirstNonSemanticsNodeInTree()
            if (layoutChild != null) {
                return layoutChild
            } // else, keep looking through the other children
        }
    }

    return null
}

private fun ComponentNode.requireFirstNonSemanticsNodeInTree(): LayoutNode {
    return findFirstNonSemanticsNodeInTree()
        ?: throw IllegalStateException("This component has no layout children")
}
