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

import androidx.ui.core.LayoutNode
import androidx.ui.core.LayoutNodeWrapper
import androidx.ui.core.boundsInRoot
import androidx.ui.core.findClosestParentNode
import androidx.ui.core.globalBounds
import androidx.ui.core.globalPosition
import androidx.ui.semantics.AccessibilityAction
import androidx.ui.semantics.SemanticsPropertyKey
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxBounds
import androidx.ui.unit.PxPosition
import androidx.ui.util.fastForEach

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
    var componentNode: LayoutNode
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
    internal constructor(unmergedConfig: SemanticsConfiguration, componentNode: LayoutNode) :
            this(generateNewId(), unmergedConfig, componentNode)

    // GEOMETRY

    /** The size of the bounding box for this node */
    val size: IntPxSize
        get() {
            return layoutNode.coordinates.size
        }

    /** The bounding box for this node relative to the root of this Compose hierarchy */
    val boundsInRoot: PxBounds
        get() {
            return layoutNode.coordinates.boundsInRoot
        }

    val globalBounds: PxBounds
        get() {
            return layoutNode.coordinates.globalBounds
        }

    val globalPosition: PxPosition
        get() {
            return layoutNode.coordinates.globalPosition
        }

    private val layoutNode: LayoutNode
        get() {
            return componentNode.requireLayoutNodeAppliedTo()
        }

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
            // Start by copying our configuration so that we can add
            // our children's configuration to it
            mergedConfig = unmergedConfig.copy()
        } else {
            // We're being merged into our parent - add our node's data
            mergedConfig = mergedConfigFromParent
            // If we are forcibly merging, we want to ignore conflicts
            mergedConfig.absorb(unmergedConfig, ignoreAlreadySet = mergeAllChildren)
        }

        if (!mergeAllChildren) {
            return mergedConfig
        }

        // If we're merging children, then collect semantic information here.
        // Order is significant here because we will attempt to merge duplicate keys.
        // This affects, for instance, the label text.
        for (child in unmergedChildren()) {
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

        var searchRoot: LayoutNode? = componentNode
        if (searchRoot?.outerSemantics?.semanticsModifier?.applyToChildLayoutNode == true) {
            searchRoot = componentNode.findLastConsecutiveSemanticsNode()
        }
        val semanticsChildren =
            searchRoot?.findOneLayerOfSemanticsWrappers() ?: emptyList()
        semanticsChildren.fastForEach { semanticsChild ->
            unmergedChildren.add(semanticsChild.semanticsNode())
        }

        return unmergedChildren
    }

    /** Contains the children in inverse hit test order (i.e. paint order). */
    // TODO(aelias): This is too expensive for a val (full subtree recreation every call);
    //               optimize this when the merging algorithm is improved.
    val children: List<SemanticsNode>
        get() {
            if (mergeAllDescendantsIntoThisNode) {
                // All of our descendants will be merged, so we have no children after merging
                return emptyList()
            }

            return unmergedChildren()
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
        children.fastForEach {
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
        children.fastForEach {
            if (!visitor(it) || !it.visitDescendants(visitor))
                return false
        }
        return true
    }

    /**
     * Whether this SemanticNode is the root of a tree or not
     */
    val isRoot: Boolean
        get() = parent == null

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

            var node = componentNode
                .findClosestParentNode { it.outerSemantics != null }
                ?.findHighestConsecutiveAncestor {
                    it.outerSemantics?.semanticsModifier?.applyToChildLayoutNode == true }

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
internal val LayoutNode.outerSemantics: SemanticsWrapper?
    get() {
        return (this as? LayoutNode)?.layoutNodeWrapper?.nearestSemantics
    }

/**
 * Returns the nearest semantics wrapper starting from a LayoutNodeWrapper.
 */
internal val LayoutNodeWrapper.nearestSemantics: SemanticsWrapper?
    get() {
        var wrapper: LayoutNodeWrapper? = this
        while (wrapper != null) {
            if (wrapper is SemanticsWrapper) return wrapper
            wrapper = wrapper.wrapped
        }
        return null
    }

/**
 * Returns the highest in a consecutive chain of this + this's parents all meeting the predicate.
*/
private fun LayoutNode.findHighestConsecutiveAncestor(
    selector: (LayoutNode) -> Boolean
): LayoutNode? {
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
    var currentParent = parent
    while (currentParent != null) {
        if (selector(currentParent)) {
            return currentParent
        } else {
            currentParent = currentParent.parent
        }
    }

    return null
}

internal fun SemanticsNode.findChildById(id: Int): SemanticsNode? {
    if (this.id == id) return this
    children.fastForEach {
        val result = it.findChildById(id)
        if (result != null) return result
    }
    return null
}

private fun LayoutNode.findOneLayerOfSemanticsWrappers(): List<SemanticsWrapper> {
    val childSemanticsComponentNodes = mutableListOf<SemanticsWrapper>()
    children.fastForEach { child ->
        findOneLayerOfSemanticsWrappersRecursive(childSemanticsComponentNodes, child)
    }
    return childSemanticsComponentNodes
}

private fun LayoutNode.findOneLayerOfSemanticsWrappersRecursive(
    list: MutableList<SemanticsWrapper>,
    node: LayoutNode
) {
    if (node.outerSemantics != null) {
        list.add(node.outerSemantics!!)
        // Stop, we're done
    } else {
        node.children.fastForEach { child ->
            findOneLayerOfSemanticsWrappersRecursive(list, child)
        }
    }
}

private fun LayoutNode.findLastConsecutiveSemanticsNode(): LayoutNode? {
    children.fastForEach { child ->
        if (child.outerSemantics != null) {
            if (child.outerSemantics?.semanticsModifier?.applyToChildLayoutNode == false)
                return child
            return child.findLastConsecutiveSemanticsNode()
        } // else, keep looking through the other children
    }

    return this
}

private fun LayoutNode.findLayoutNodeAppliedTo(): LayoutNode? {
    if ((outerSemantics == null ||
            outerSemantics?.semanticsModifier?.applyToChildLayoutNode == false)) {
        return this
    }
    children.fastForEach { child ->
            val layoutChild = child.findLayoutNodeAppliedTo()
            if (layoutChild != null) {
                return layoutChild
            }
    }

    return null
}

private fun LayoutNode.requireLayoutNodeAppliedTo(): LayoutNode {
    return findLayoutNodeAppliedTo()
        ?: throw IllegalStateException("This component has no layout children")
}
