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

package androidx.compose.ui.semantics

import androidx.collection.IntObjectMap
import androidx.collection.MutableIntObjectMap
import androidx.collection.MutableObjectList
import androidx.collection.emptyIntObjectMap
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.semantics.SemanticsProperties.HideFromAccessibility
import androidx.compose.ui.semantics.SemanticsProperties.InvisibleToUser
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.trace

/** Owns [SemanticsNode] objects and notifies listeners of changes to the semantics tree */
class SemanticsOwner
internal constructor(
    private val rootNode: LayoutNode,
    private val outerSemanticsNode: EmptySemanticsModifier,
    private val nodes: IntObjectMap<LayoutNode>,
) {
    /**
     * The root node of the semantics tree. Does not contain any unmerged data. May contain merged
     * data.
     */
    val rootSemanticsNode: SemanticsNode
        get() {
            return SemanticsNode(rootNode, mergingEnabled = true)
        }

    val unmergedRootSemanticsNode: SemanticsNode
        get() {
            return SemanticsNode(
                outerSemanticsNode = outerSemanticsNode,
                layoutNode = rootNode,
                mergingEnabled = false,
                // Forcing an empty SemanticsConfiguration here since the root node will always
                // have an empty config, but if we don't pass this in explicitly here it will try
                // to call `rootNode.collapsedSemantics` which will fail because the LayoutNode
                // is not yet attached when this getter is first called.
                unmergedConfig = SemanticsConfiguration(),
            )
        }

    internal val listeners = MutableObjectList<SemanticsListener>(2)

    internal val rootInfo: SemanticsInfo
        get() = rootNode

    internal operator fun get(semanticsId: Int): SemanticsInfo? {
        return nodes[semanticsId]
    }

    internal fun notifySemanticsChange(
        semanticsInfo: SemanticsInfo,
        previousSemanticsConfiguration: SemanticsConfiguration?,
    ) {
        listeners.forEach { it.onSemanticsChanged(semanticsInfo, previousSemanticsConfiguration) }
    }
}

/**
 * Finds all [SemanticsNode]s in the tree owned by this [SemanticsOwner]. Return the results in a
 * list.
 *
 * @param mergingEnabled set to true if you want the data to be merged.
 * @param skipDeactivatedNodes set to false if you want to collect the nodes which are deactivated.
 *   For example, the children of [androidx.compose.ui.layout.SubcomposeLayout] which are retained
 *   to be reused in future are considered deactivated.
 */
fun SemanticsOwner.getAllSemanticsNodes(
    mergingEnabled: Boolean,
    skipDeactivatedNodes: Boolean = true,
): List<SemanticsNode> {
    return getAllSemanticsNodesToMap(
            useUnmergedTree = !mergingEnabled,
            skipDeactivatedNodes = skipDeactivatedNodes,
        )
        .values
        .toList()
}

@Suppress("unused")
@Deprecated(message = "Use a new overload instead", level = DeprecationLevel.HIDDEN)
fun SemanticsOwner.getAllSemanticsNodes(mergingEnabled: Boolean) =
    getAllSemanticsNodes(mergingEnabled, true)

/**
 * Finds all [SemanticsNode]s in the tree owned by this [SemanticsOwner]. Return the results in a
 * map.
 */
internal fun SemanticsOwner.getAllSemanticsNodesToMap(
    useUnmergedTree: Boolean = false,
    skipDeactivatedNodes: Boolean = true,
): Map<Int, SemanticsNode> {
    val nodes = mutableMapOf<Int, SemanticsNode>()

    fun findAllSemanticNodesRecursive(currentNode: SemanticsNode) {
        nodes[currentNode.id] = currentNode
        currentNode.getChildren(includeDeactivatedNodes = !skipDeactivatedNodes).fastForEach { child
            ->
            findAllSemanticNodesRecursive(child)
        }
    }

    val root = if (useUnmergedTree) unmergedRootSemanticsNode else rootSemanticsNode
    if (!skipDeactivatedNodes || !root.layoutNode.isDeactivated) {
        findAllSemanticNodesRecursive(root)
    }
    return nodes
}

internal fun SemanticsNode.isImportantForAccessibility() =
    !isHidden &&
        (unmergedConfig.isMergingSemanticsOfDescendants ||
            unmergedConfig.containsImportantForAccessibility())

@Suppress("DEPRECATION")
internal val SemanticsNode.isHidden: Boolean
    // A node is considered hidden if it is transparent, or explicitly is hidden from accessibility.
    // This also checks if the node has been marked as `invisibleToUser`, which is what the
    // `hiddenFromAccessibility` API used to  be named.
    get() =
        isTransparent ||
            (unmergedConfig.contains(HideFromAccessibility) ||
                unmergedConfig.contains(InvisibleToUser))

private val DefaultFakeNodeBounds = Rect(0f, 0f, 10f, 10f)

/** Semantics node with adjusted bounds for the uncovered(by siblings) part. */
internal class SemanticsNodeWithAdjustedBounds(
    val semanticsNode: SemanticsNode,
    val adjustedBounds: IntRect,
)

/**
 * Finds pruned [SemanticsNode]s in the tree owned by this [SemanticsOwner]. A semantics node
 * completely covered by siblings drawn on top of it will be pruned. Return the results in a map.
 */
internal fun SemanticsOwner.getAllUncoveredSemanticsNodesToIntObjectMap(
    customRootNodeId: Int
): IntObjectMap<SemanticsNodeWithAdjustedBounds> {
    trace("getAllUncoveredSemanticsNodesToIntObjectMap") {
        val root = unmergedRootSemanticsNode
        if (!root.layoutNode.isPlaced || !root.layoutNode.isAttached) {
            return emptyIntObjectMap()
        }

        // Default capacity chosen to accommodate common scenarios
        val nodes = MutableIntObjectMap<SemanticsNodeWithAdjustedBounds>(48)

        val unaccountedSpace = SemanticsRegion()
        unaccountedSpace.set(root.boundsInRoot.roundToIntRect())

        fun findAllSemanticNodesRecursive(currentNode: SemanticsNode, region: SemanticsRegion) {
            val notAttachedOrPlaced =
                !currentNode.layoutNode.isPlaced || !currentNode.layoutNode.isAttached
            if (
                (unaccountedSpace.isEmpty && currentNode.id != root.id) ||
                    (notAttachedOrPlaced && !currentNode.isFake)
            ) {
                return
            }
            val touchBoundsInRoot = currentNode.touchBoundsInRoot.roundToIntRect()

            region.set(touchBoundsInRoot)

            val virtualViewId =
                if (currentNode.id == root.id) {
                    customRootNodeId
                } else {
                    currentNode.id
                }
            if (region.intersect(unaccountedSpace)) {
                nodes[virtualViewId] = SemanticsNodeWithAdjustedBounds(currentNode, region.bounds)
                // Children could be drawn outside of parent, but we are using clipped bounds for
                // accessibility now, so let's put the children recursion inside of this if. If
                // later
                // we decide to support children drawn outside of parent, we can move it out of the
                // if block.
                val children = currentNode.replacedChildren
                for (i in children.size - 1 downTo 0) {
                    // Links in text nodes are semantics children. But for Android accessibility
                    // support
                    // we don't publish them to the accessibility services because they are exposed
                    // as UrlSpan/ClickableSpan spans instead
                    if (children[i].config.contains(SemanticsProperties.LinkTestMarker)) {
                        continue
                    }
                    findAllSemanticNodesRecursive(children[i], region)
                }
                if (currentNode.isImportantForAccessibility()) {
                    unaccountedSpace.difference(touchBoundsInRoot)
                }
            } else {
                if (currentNode.isFake) {
                    val parentNode = currentNode.parent
                    // use parent bounds for fake node
                    val boundsForFakeNode =
                        if (parentNode?.layoutInfo?.isPlaced == true) {
                            parentNode.boundsInRoot
                        } else {
                            DefaultFakeNodeBounds
                        }
                    nodes[virtualViewId] =
                        SemanticsNodeWithAdjustedBounds(
                            currentNode,
                            boundsForFakeNode.roundToIntRect(),
                        )
                } else if (virtualViewId == customRootNodeId) {
                    // Root view might have WRAP_CONTENT layout params in which case it will have
                    // zero
                    // bounds if there is no other content with semantics. But we need to always
                    // send
                    // the
                    // root view info as there are some other apps (e.g. Google Assistant) that
                    // depend
                    // on accessibility info
                    nodes[virtualViewId] =
                        SemanticsNodeWithAdjustedBounds(currentNode, region.bounds)
                }
            }
        }

        findAllSemanticNodesRecursive(root, SemanticsRegion())
        return nodes
    }
}
