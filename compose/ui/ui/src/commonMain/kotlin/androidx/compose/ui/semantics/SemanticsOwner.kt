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
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InteractionBarrierNode
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.node.HitTestResult
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.requireLayoutNode
import androidx.compose.ui.platform.ExperimentalComposeUiHostApi
import androidx.compose.ui.semantics.SemanticsProperties.HideFromAccessibility
import androidx.compose.ui.semantics.SemanticsProperties.InvisibleToUser
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.trace

/** Owns [SemanticsNode] objects and notifies listeners of changes to the semantics tree */
public class SemanticsOwner
internal constructor(
    private val rootNode: LayoutNode,
    private val outerSemanticsNode: EmptySemanticsModifier,
    private val nodes: IntObjectMap<LayoutNode>,
) {
    /**
     * The root node of the semantics tree. Does not contain any unmerged data. May contain merged
     * data.
     */
    public val rootSemanticsNode: SemanticsNode
        get() {
            return SemanticsNode(rootNode, mergingEnabled = true)
        }

    /** Root node of the unmerged semantics tree. */
    public val unmergedRootSemanticsNode: SemanticsNode
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

    internal fun getSemanticsInfo(semanticsId: Int): SemanticsInfo? {
        return nodes[semanticsId]
    }

    /**
     * Returns the unmerged [SemanticsNode] with [semanticsId], or `null` if no attached layout with
     * that id has semantics.
     *
     * Allocates a [SemanticsNode]. Use [getLayoutInfo] with [LayoutInfo.semanticsConfiguration]
     * when only the layout or its unmerged configuration is needed.
     *
     * @param semanticsId The [LayoutInfo.semanticsId] of the node.
     */
    @ExperimentalComposeUiHostApi
    public operator fun get(semanticsId: Int): SemanticsNode? {
        val layoutNode = nodes[semanticsId] ?: return null
        if (!layoutNode.isAttached || !layoutNode.nodes.has(Nodes.Semantics)) return null
        return SemanticsNode(layoutNode, mergingEnabled = false)
    }

    /**
     * Returns the attached [LayoutInfo] with [semanticsId], or `null` if there is none. Doesn't
     * allocate.
     *
     * @param semanticsId The [LayoutInfo.semanticsId] of the layout.
     */
    @ExperimentalComposeUiHostApi
    public fun getLayoutInfo(semanticsId: Int): LayoutInfo? = nodes[semanticsId]

    /**
     * Performs semantics hit testing at [position] and calls [onHit] for each hit layout that has
     * semantics, top-most first, until [onHit] returns `true`. Each layout is reported once.
     *
     * Results include hidden nodes; filter them with [SemanticsNode.isHidden] or
     * [LayoutInfo.semanticsConfiguration] as needed. Inline text link test markers
     * ([SemanticsProperties.LinkTestMarker]) are skipped. Hit testing uses the most recent layout,
     * so measure and lay out pending changes first.
     *
     * @sample androidx.compose.ui.samples.SemanticsOwnerHitTestSample
     * @param position Pointer position in root coordinates.
     * @param onHit Called with each hit layout. Return `true` to stop at that layout.
     * @return the layout for which [onHit] returned `true`, or `null` if it never did.
     */
    @OptIn(androidx.compose.ui.InternalComposeUiApi::class)
    @ExperimentalComposeUiHostApi
    public fun hitTest(position: Offset, onHit: (layoutInfo: LayoutInfo) -> Boolean): LayoutInfo? {
        val hitSemanticsEntities = HitTestResult()
        rootNode.hitTestSemantics(
            pointerPosition = position,
            hitSemanticsEntities = hitSemanticsEntities,
        )
        var previous: LayoutNode? = null
        for (i in hitSemanticsEntities.lastIndex downTo 0) {
            val layoutNode = hitSemanticsEntities[i].requireLayoutNode()
            // A layout with several semantics modifiers is hit once per modifier; report it once.
            if (layoutNode === previous) continue
            previous = layoutNode
            if (!layoutNode.nodes.has(Nodes.Semantics)) continue
            val config = layoutNode.semanticsConfiguration
            if (config != null && config.contains(SemanticsProperties.LinkTestMarker)) continue
            if (onHit(layoutNode)) return layoutNode
        }
        return null
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
public fun SemanticsOwner.getAllSemanticsNodes(
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
public fun SemanticsOwner.getAllSemanticsNodes(mergingEnabled: Boolean): List<SemanticsNode> =
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

/**
 * Whether this node is meaningful to accessibility services: it isn't [hidden][isHidden] or an
 * inline text link test marker, and it merges descendants, sets a property that accessibility
 * reads, or has an interaction barrier.
 */
@ExperimentalComposeUiHostApi
public val SemanticsNode.isImportantForAccessibility: Boolean
    get() =
        when {
            isHidden -> false
            _unmergedConfig.contains(SemanticsProperties.LinkTestMarker) -> false
            _unmergedConfig.isMergingSemanticsOfDescendants -> true
            _unmergedConfig.containsImportantForAccessibility() -> true
            layoutNode.hasInteractionBarrierNode() -> true
            else -> false
        }

/** Returns whether this node is hidden from accessibility services. */
@Suppress("DEPRECATION")
@ExperimentalComposeUiHostApi
public val SemanticsNode.isHidden: Boolean
    // A node is considered hidden if it is transparent, or explicitly is hidden from accessibility.
    // This also checks if the node has been marked as `invisibleToUser`, which is what the
    // `hiddenFromAccessibility` API used to  be named.
    get() =
        when {
            isTransparent -> true
            unmergedConfig.contains(HideFromAccessibility) -> true
            unmergedConfig.contains(InvisibleToUser) -> true
            else -> false
        }

private val DefaultFakeNodeBounds = Rect(0f, 0f, 10f, 10f)

/** Semantics node with adjusted bounds for the uncovered (by siblings) part. */
internal class AdjustedSemanticsNode(
    val semanticsNode: SemanticsNode,
    val adjustedBounds: IntRect,
    val isInMergingHiddenSubtree: Boolean = false,
)

/**
 * Receives the nodes visited by [SemanticsOwner.forEachUncoveredSemanticsNode].
 *
 * Bounds are passed as primitives so that visiting doesn't allocate.
 */
@ExperimentalComposeUiHostApi
public fun interface UncoveredSemanticsNodeVisitor {
    /**
     * Called for each uncovered node.
     *
     * @param node The node, from the unmerged tree. Includes the synthetic accessibility nodes
     *   produced by [SemanticsNode.collectAccessibilityChildren].
     * @param left Left edge of the uncovered bounds, in pixels, in root coordinates.
     * @param top Top edge of the uncovered bounds, in pixels, in root coordinates.
     * @param right Right edge of the uncovered bounds, in pixels, in root coordinates.
     * @param bottom Bottom edge of the uncovered bounds, in pixels, in root coordinates.
     * @param isInMergingHiddenSubtree `true` if [node] or an enclosing merging node is hidden with
     *   [hideFromAccessibility]. Hosts report such nodes as not visible to the user.
     */
    public fun visit(
        node: SemanticsNode,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        isInMergingHiddenSubtree: Boolean,
    )
}

/**
 * Visits the semantics nodes in the unmerged tree that aren't completely covered by siblings drawn
 * on top of them.
 *
 * Starts at [SemanticsOwner.unmergedRootSemanticsNode], which is always visited (hosts usually map
 * its id to their own root). Children come from [SemanticsNode.collectAccessibilityChildren], so
 * descendants of [clearAndSetSemantics] nodes and inline text link test markers
 * ([SemanticsProperties.LinkTestMarker]) are not visited. The bounds passed to [visitor] are the
 * bounding box of the uncovered part of each node's touch bounds; synthetic nodes use the bounds of
 * the node that owns them. Nothing is visited if the root isn't attached and placed.
 *
 * @param visitor Called for each uncovered node.
 */
@ExperimentalComposeUiHostApi
public fun SemanticsOwner.forEachUncoveredSemanticsNode(visitor: UncoveredSemanticsNodeVisitor) {
    trace("forEachUncoveredSemanticsNode") {
        val root = unmergedRootSemanticsNode
        if (!root.layoutNode.isPlaced || !root.layoutNode.isAttached) {
            return
        }
        UncoveredSemanticsNodeTraversal(root, visitor).traverse()
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalComposeUiHostApi::class)
private class UncoveredSemanticsNodeTraversal(
    private val root: SemanticsNode,
    private val visitor: UncoveredSemanticsNodeVisitor,
) {
    /** Children of every node on the current path. Each level uses its own index range. */
    private val children = ArrayList<SemanticsNode>()
    private val addChild: (SemanticsNode) -> Unit = { child -> children.add(child) }

    fun traverse() {
        val unaccountedSpace = SemanticsRegion()
        unaccountedSpace.setRounded(root.boundsInRoot)
        findAllSemanticNodesRecursive(root, SemanticsRegion(), unaccountedSpace, false)
    }

    /** Appends the children of [node] to [children] and returns the index of the first one. */
    private fun collectChildren(node: SemanticsNode): Int {
        val start = children.size
        node.forEachAccessibilityChild(addChild)
        return start
    }

    private fun removeChildren(start: Int) {
        while (children.size > start) children.removeAt(children.lastIndex)
    }

    private fun visitRegion(node: SemanticsNode, region: SemanticsRegion, isHidden: Boolean) {
        region.updateBounds()
        visitor.visit(
            node,
            region.boundsLeft,
            region.boundsTop,
            region.boundsRight,
            region.boundsBottom,
            isHidden,
        )
    }

    private fun addFakeNode(node: SemanticsNode, isInMergingHiddenSubtree: Boolean) {
        val parentNode = node.parent
        // use parent bounds for fake node
        val boundsForFakeNode =
            if (parentNode?.layoutInfo?.isPlaced == true) {
                parentNode.boundsInRoot
            } else {
                DefaultFakeNodeBounds
            }
        visitor.visit(
            node,
            boundsForFakeNode.left.fastRoundToInt(),
            boundsForFakeNode.top.fastRoundToInt(),
            boundsForFakeNode.right.fastRoundToInt(),
            boundsForFakeNode.bottom.fastRoundToInt(),
            isInMergingHiddenSubtree,
        )
    }

    /**
     * Helper to add descendants of a merging node that is partially visible in its scrolling
     * container. This method is similar to [findAllSemanticNodesRecursive] but handles both clipped
     * and unclipped bounds and uses a merging parent (not a root) for unaccounted space.
     */
    private fun addDescendantsOfMergingNodePartiallyVisibleInScrollParent(
        currentNode: SemanticsNode,
        region: SemanticsRegion,
        unaccountedSpace: SemanticsRegion,
        isInMergingHiddenSubtree: Boolean,
    ) {
        if (
            !currentNode.layoutNode.isPlaced ||
                !currentNode.layoutNode.isAttached ||
                unaccountedSpace.isEmpty
        ) {
            // The node not attached because this could be a fake node, so we should add it
            if (currentNode.isFake) addFakeNode(currentNode, isInMergingHiddenSubtree)
            return
        }

        // Use unclipped bounds for intersection and reporting within this context only if the
        // node is fully off-screen. Otherwise, continue using the clipped bounds.
        val currentBounds =
            currentNode.touchBoundsInRoot.run {
                if (isEmpty) currentNode.unclippedBoundsInRoot else this
            }
        val left = currentBounds.left.fastRoundToInt()
        val top = currentBounds.top.fastRoundToInt()
        val right = currentBounds.right.fastRoundToInt()
        val bottom = currentBounds.bottom.fastRoundToInt()
        region.set(left, top, right, bottom)
        val isCurrentHidden =
            isInMergingHiddenSubtree ||
                (currentNode._unmergedConfig.isMergingSemanticsOfDescendants &&
                    currentNode._unmergedConfig.contains(HideFromAccessibility))
        if (region.intersect(unaccountedSpace)) {
            // For nodes that are partially visible in the root, we will continue reporting
            // their clipped bounds. However, if the node is *fully* off-screen, we will add
            // them with their unclipped bounds. But to send the correct signal to
            // the accessibility services, we will mark them as invisible to user
            visitRegion(currentNode, region, isCurrentHidden)

            val start = collectChildren(currentNode)
            for (i in children.lastIndex downTo start) {
                addDescendantsOfMergingNodePartiallyVisibleInScrollParent(
                    children[i],
                    region,
                    unaccountedSpace,
                    isCurrentHidden,
                )
            }
            removeChildren(start)
            if (currentNode.isImportantForAccessibility) {
                unaccountedSpace.difference(left, top, right, bottom)
            }
        }
    }

    fun findAllSemanticNodesRecursive(
        currentNode: SemanticsNode,
        region: SemanticsRegion,
        unaccountedSpace: SemanticsRegion,
        isInMergingHiddenSubtree: Boolean,
    ) {
        val isRoot = currentNode.id == root.id
        val notAttachedOrPlaced =
            !currentNode.layoutNode.isPlaced || !currentNode.layoutNode.isAttached
        if ((unaccountedSpace.isEmpty && !isRoot) || (notAttachedOrPlaced && !currentNode.isFake)) {
            return
        }
        if (currentNode.isFake) {
            addFakeNode(currentNode, isInMergingHiddenSubtree)
            return
        }
        val touchBoundsInRoot = currentNode.touchBoundsInRoot
        val touchLeft = touchBoundsInRoot.left.fastRoundToInt()
        val touchTop = touchBoundsInRoot.top.fastRoundToInt()
        val touchRight = touchBoundsInRoot.right.fastRoundToInt()
        val touchBottom = touchBoundsInRoot.bottom.fastRoundToInt()
        val clippedLeft: Int
        val clippedTop: Int
        val clippedRight: Int
        val clippedBottom: Int
        if (ComposeUiFlags.isClippedTouchBoundsOcclusionFixEnabled) {
            val clippedTouchBoundsInRoot = currentNode.clippedTouchBoundsInRoot
            clippedLeft = clippedTouchBoundsInRoot.left.fastRoundToInt()
            clippedTop = clippedTouchBoundsInRoot.top.fastRoundToInt()
            clippedRight = clippedTouchBoundsInRoot.right.fastRoundToInt()
            clippedBottom = clippedTouchBoundsInRoot.bottom.fastRoundToInt()
        } else {
            clippedLeft = touchLeft
            clippedTop = touchTop
            clippedRight = touchRight
            clippedBottom = touchBottom
        }

        val isCurrentHidden =
            isInMergingHiddenSubtree ||
                (currentNode._unmergedConfig.isMergingSemanticsOfDescendants &&
                    currentNode._unmergedConfig.contains(HideFromAccessibility))

        // Test the actual uncovered Region inside the clipping container (not region.bounds,
        // whose rectangular bounding box would include L-shaped corner overhang).
        region.set(clippedLeft, clippedTop, clippedRight, clippedBottom)

        // Note that the `intersect` call updates the region
        val isUncovered = region.intersect(unaccountedSpace)
        if (
            isUncovered &&
                (clippedLeft != touchLeft ||
                    clippedTop != touchTop ||
                    clippedRight != touchRight ||
                    clippedBottom != touchBottom)
        ) {
            region.set(touchLeft, touchTop, touchRight, touchBottom)
            region.intersect(unaccountedSpace)
        }
        if (isUncovered) {
            visitRegion(currentNode, region, isCurrentHidden)

            // Children could be drawn outside of parent, but we are using clipped bounds for
            // accessibility now, so let's put the children recursion inside of this if. If
            // later we decide to support children drawn outside of parent, we can move it out
            // of the `if` block.
            val start = collectChildren(currentNode)

            val shouldIncludeOffscreenChildren =
                ComposeUiFlags.isAccessibilityShouldIncludeOffscreenChildrenEnabled &&
                    currentNode._unmergedConfig.isMergingSemanticsOfDescendants &&
                    currentNode.isPartiallyOffscreenInScrollParent
            if (shouldIncludeOffscreenChildren) {
                // If this is a partially offscreen node inside the scrolling container,
                // and it merges its children, we want to include its children even if they are
                // completely offscreen (for screen-readers experience).
                val childrenUnaccountedRegion = SemanticsRegion()
                childrenUnaccountedRegion.setRounded(currentNode.unclippedBoundsInRoot)
                for (i in children.lastIndex downTo start) {
                    addDescendantsOfMergingNodePartiallyVisibleInScrollParent(
                        children[i],
                        SemanticsRegion(),
                        childrenUnaccountedRegion,
                        isCurrentHidden,
                    )
                }
            } else {
                for (i in children.lastIndex downTo start) {
                    findAllSemanticNodesRecursive(
                        currentNode = children[i],
                        region = region,
                        unaccountedSpace = unaccountedSpace,
                        isInMergingHiddenSubtree = isCurrentHidden,
                    )
                }
            }
            removeChildren(start)
            if (currentNode.isImportantForAccessibility) {
                unaccountedSpace.difference(clippedLeft, clippedTop, clippedRight, clippedBottom)
            }
        } else if (isRoot) {
            // Root view might have WRAP_CONTENT layout params in which case it will have zero
            // bounds if there is no other content with semantics. But we need to always send the
            // root view info as there are some other apps (e.g. Google Assistant) that depend on
            // accessibility info
            visitRegion(currentNode, region, isCurrentHidden)
        }
    }
}

private fun SemanticsRegion.setRounded(rect: Rect) {
    set(
        rect.left.fastRoundToInt(),
        rect.top.fastRoundToInt(),
        rect.right.fastRoundToInt(),
        rect.bottom.fastRoundToInt(),
    )
}

/**
 * Finds pruned [SemanticsNode]s in the tree owned by this [SemanticsOwner]. A semantics node
 * completely covered by siblings drawn on top of it will be pruned. Return the results in a map.
 */
@OptIn(ExperimentalComposeUiHostApi::class)
internal fun SemanticsOwner.getAllUncoveredSemanticsNodesToIntObjectMap(
    customRootNodeId: Int,
    shouldIgnoreNode: (SemanticsNode) -> Boolean,
): IntObjectMap<AdjustedSemanticsNode> {
    // Default capacity chosen to accommodate common scenarios
    val nodes = MutableIntObjectMap<AdjustedSemanticsNode>(48)
    val rootId = rootInfo.semanticsId
    forEachUncoveredSemanticsNode { node, left, top, right, bottom, isHidden ->
        if (!shouldIgnoreNode(node)) {
            val virtualViewId = if (node.id == rootId) customRootNodeId else node.id
            nodes[virtualViewId] =
                AdjustedSemanticsNode(node, IntRect(left, top, right, bottom), isHidden)
        }
    }
    return nodes
}

/** This is true if the node is partially within the bounds of the scrolling container. */
private val SemanticsNode.isPartiallyOffscreenInScrollParent: Boolean
    get() {
        getScrollableParent()?.let { scrollParent ->
            val nodeCoordinates =
                findCoordinatorToGetBounds()?.takeIf { it.isAttached }?.coordinates
            val parentCoordinatorForBounds =
                scrollParent.findCoordinatorToGetBounds()?.takeIf { it.isAttached }?.coordinates
            if (nodeCoordinates == null || parentCoordinatorForBounds == null) return false
            val unclippedBounds =
                parentCoordinatorForBounds.localBoundingBoxOf(nodeCoordinates, false)
            val parentBounds = Rect(Offset.Zero, parentCoordinatorForBounds.size.toSize())
            val visibleBounds = unclippedBounds.intersect(parentBounds)
            return unclippedBounds != visibleBounds
        }
        return false
    }

private fun SemanticsNode.getScrollableParent(): SemanticsNode? {
    var parent: SemanticsNode? = this.parent
    while (parent != null) {
        if (parent.isScrollNode) return parent
        parent = parent.parent
    }
    return null
}

private val SemanticsNode.isScrollNode: Boolean
    @OptIn(ExperimentalComposeUiHostApi::class)
    get() {
        return unmergedConfig.contains(SemanticsProperties.VerticalScrollAxisRange) ||
            unmergedConfig.contains(SemanticsProperties.HorizontalScrollAxisRange)
    }

internal fun LayoutNode.hasInteractionBarrierNode(): Boolean {
    val head = nodes.head(Nodes.Semantics) ?: return false
    var curr: Modifier.Node? = head.node
    while (curr != null) {
        if (curr is InteractionBarrierNode) return true
        if (curr.aggregateChildKindSet and Nodes.Semantics.mask == 0) break
        curr = curr.child
    }
    return false
}
