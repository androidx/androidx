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

import androidx.ui.core.AlignmentLine
import androidx.ui.core.ExperimentalLayoutNodeApi
import androidx.ui.core.LayoutNode
import androidx.ui.core.LayoutNodeWrapper
import androidx.ui.core.boundsInRoot
import androidx.ui.core.findClosestParentNode
import androidx.ui.core.globalBounds
import androidx.ui.core.globalPosition
import androidx.ui.semantics.AccessibilityAction
import androidx.ui.semantics.SemanticsPropertyKey
import androidx.ui.unit.IntSize
import androidx.ui.unit.PxBounds
import androidx.ui.geometry.Offset
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
@OptIn(ExperimentalLayoutNodeApi::class)
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
    internal constructor(unmergedConfig: SemanticsConfiguration, layoutNode: LayoutNode) :
            this(generateNewId(), unmergedConfig, layoutNode)

    // GEOMETRY

    /** The size of the bounding box for this node */
    val size: IntSize
        get() {
            return componentNode.coordinates.size
        }

    /** The bounding box for this node relative to the root of this Compose hierarchy */
    val boundsInRoot: PxBounds
        get() {
            return componentNode.coordinates.boundsInRoot
        }

    val globalBounds: PxBounds
        get() {
            return componentNode.coordinates.globalBounds
        }

    val globalPosition: Offset
        get() {
            return componentNode.coordinates.globalPosition
        }

    /**
     * The merged configuration of this node
     */
    // TODO(aelias): This is too expensive for a val (full subtree recreation every call);
    //               optimize this when the merging algorithm is improved.
    val config: SemanticsConfiguration
        get() {
            if (isMergingSemanticsOfDescendants) {
                return buildMergedConfig(SemanticsConfiguration())
            } else {
                return unmergedConfig
            }
        }

    /**
     * Returns the position of an [alignment line][AlignmentLine], or [AlignmentLine.Unspecified]
     * if the line is not provided.
     */
    fun getAlignmentLinePosition(line: AlignmentLine): Int {
        return componentNode.coordinates[line]
    }

    private fun buildMergedConfig(
        mergedConfig: SemanticsConfiguration
    ): SemanticsConfiguration {
        mergedConfig.absorb(unmergedConfig, ignoreAlreadySet = true)

        unmergedChildren().fastForEach { child ->
            if (child.isMergingSemanticsOfDescendants == false) {
                child.buildMergedConfig(mergedConfig)
            }
        }

        return mergedConfig
    }

    /** Whether this node and all of its descendants should be treated as one logical entity. */
    private val isMergingSemanticsOfDescendants: Boolean
        get() = unmergedConfig.isMergingSemanticsOfDescendants

    // CHILDREN

    private fun unmergedChildren(): List<SemanticsNode> {
        val unmergedChildren: MutableList<SemanticsNode> = mutableListOf()

        val semanticsChildren = componentNode.findOneLayerOfSemanticsWrappers()
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
            if (isMergingSemanticsOfDescendants) {
                // In most common merging scenarios like Buttons, this will return nothing.
                // In cases like a clickable Drawer itself containing a Button, this will
                // return the Button as a child.
                return findOneLayerOfMergingSemanticsNodes()
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
     * Visit all the descendants of this node.  *
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
    val parent: SemanticsNode?
        get() {
            var node: LayoutNode?
            node = componentNode.findClosestParentNode {
                it.outerSemantics
                    ?.collapsedSemanticsConfiguration()
                    ?.isMergingSemanticsOfDescendants == true
            }

            if (node == null) {
                node = componentNode.findClosestParentNode { it.outerSemantics != null }
            }

            return node?.outerSemantics?.semanticsNode()
        }

    internal fun <T : Function<Boolean>> canPerformAction(
        action: SemanticsPropertyKey<AccessibilityAction<T>>
    ) =
        this.config.contains(action)

    private fun findOneLayerOfMergingSemanticsNodes(
        list: MutableList<SemanticsNode> = mutableListOf<SemanticsNode>()
    ): List<SemanticsNode> {
        unmergedChildren().fastForEach { child ->
            if (child.isMergingSemanticsOfDescendants == true) {
                list.add(child)
            } else {
                child.findOneLayerOfMergingSemanticsNodes(list)
            }
        }
        return list
    }
}

/**
 * Returns the outermost semantics node on a LayoutNode.
 */
@OptIn(ExperimentalLayoutNodeApi::class)
internal val LayoutNode.outerSemantics: SemanticsWrapper?
    get() {
        return (this as? LayoutNode)?.outerLayoutNodeWrapper?.nearestSemantics
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

@OptIn(ExperimentalLayoutNodeApi::class)
private fun LayoutNode.findOneLayerOfSemanticsWrappers(
    list: MutableList<SemanticsWrapper> = mutableListOf<SemanticsWrapper>()
): List<SemanticsWrapper> {
    children.fastForEach { child ->
        val outerSemantics = child.outerSemantics
        if (outerSemantics != null) {
            list.add(outerSemantics)
        } else {
            child.findOneLayerOfSemanticsWrappers(list)
        }
    }
    return list
}
