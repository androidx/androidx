/*
 * Copyright 2023 The Android Open Source Project
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
import androidx.collection.intObjectMapOf
import androidx.collection.mutableIntObjectMapOf
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.ExperimentalComposeUiHostApi
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEach
import kotlin.math.max
import kotlin.math.min

/**
 * This function prepares a subtree for `sortByGeometryGroupings` by retrieving all non-container
 * nodes and adding them to the list to be geometrically sorted. We recurse on containers (if they
 * exist) and add their sorted children to an optional mapping. The list to be sorted and child
 * mapping is passed into `sortByGeometryGroupings`.
 *
 * [inClippedMergingSubtree] is true when [listToSort] holds the descendants of a merging container
 * that is clipped by a viewport; such subtrees are sorted by unclipped window bounds (see
 * [sortingBoundsInWindow]).
 */
internal fun SemanticsNode.subtreeSortedByGeometryGrouping(
    isVisible: (SemanticsNode) -> Boolean,
    isFocusableContainer: (SemanticsNode) -> Boolean,
    listToSort: List<SemanticsNode>,
    inClippedMergingSubtree: Boolean = false,
): List<SemanticsNode> {
    if (listToSort.isEmpty()) return emptyList()

    // This should be mapping of [containerID: listOfSortedChildren], only populated if there
    // are container nodes in this level. If there are container nodes, `containerMapToChildren`
    // would look like {containerId: [sortedChild, sortedChild], containerId: [sortedChild]}
    val containerMapToChildren = mutableIntObjectMapOf<List<SemanticsNode>>()
    val geometryList = ArrayList<SemanticsNode>()

    listToSort.fastForEach { node ->
        node.geometryDepthFirstSearch(
            geometryList,
            isVisible,
            isFocusableContainer,
            containerMapToChildren,
            inClippedMergingSubtree,
        )
    }

    if (geometryList.isEmpty()) return emptyList()

    return sortByGeometryGroupings(
        geometryList,
        isFocusableContainer,
        containerMapToChildren,
        useUnclippedBounds = inClippedMergingSubtree,
    )
}

@OptIn(ExperimentalComposeUiHostApi::class)
private fun SemanticsNode.geometryDepthFirstSearch(
    geometryList: ArrayList<SemanticsNode>,
    isVisible: (SemanticsNode) -> Boolean,
    isFocusableContainer: (SemanticsNode) -> Boolean,
    containerMapToChildren: MutableIntObjectMap<List<SemanticsNode>>,
    inClippedMergingSubtree: Boolean,
) {
    // We only want to add children that are either traversalGroups or are
    // screen reader focusable. The child must also be in the current pruned semantics tree.
    val explicitTraversalGroup = unmergedConfig.getOrNull(SemanticsProperties.IsTraversalGroup)
    val nodeChildren = children
    val isClippedMergingNode =
        if (explicitTraversalGroup != null && inClippedMergingSubtree) {
            false
        } else {
            isClippedMergingContainer(
                explicitTraversalGroup = explicitTraversalGroup,
                hasChildren = nodeChildren.isNotEmpty(),
                isVisible = isVisible,
                isFocusableContainer = isFocusableContainer,
            )
        }
    val isTraversalGroup = explicitTraversalGroup ?: isClippedMergingNode

    if ((isTraversalGroup || isFocusableContainer(this)) && isVisible(this)) {
        geometryList.add(this)
    }
    if (isTraversalGroup) {
        // Recurse and record the container's children, sorted
        val sortedChildren =
            subtreeSortedByGeometryGrouping(
                isVisible,
                isFocusableContainer,
                nodeChildren,
                inClippedMergingSubtree = inClippedMergingSubtree || isClippedMergingNode,
            )
        // A focusable container without sortable children (e.g. a clipped merging node whose
        // children are all merged non-focusable text) needs no mapping entry: it is only kept in
        // `geometryList`.
        if (sortedChildren.isNotEmpty() || !isFocusableContainer(this)) {
            containerMapToChildren[id] = sortedChildren
        }
    } else {
        // Otherwise, continue adding children to the list that'll be sorted regardless of hierarchy
        nodeChildren.fastForEach { child ->
            child.geometryDepthFirstSearch(
                geometryList,
                isVisible,
                isFocusableContainer,
                containerMapToChildren,
                inClippedMergingSubtree,
            )
        }
    }
}

/**
 * Returns true when this node is a visible, non-empty merging container that is clipped by a
 * viewport and did not explicitly opt out via `isTraversalGroup = false`.
 *
 * When `isTraversalGroup` is unset, only screen-reader-focusable containers (e.g. a clickable row
 * or card containing secondary controls) become implicit traversal groups so their clipped children
 * stay grouped with them; non-focusable merging wrappers only enter unclipped subtree sorting if
 * `isTraversalGroup = true` was set explicitly.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalComposeUiHostApi::class)
private fun SemanticsNode.isClippedMergingContainer(
    explicitTraversalGroup: Boolean?,
    hasChildren: Boolean,
    isVisible: (SemanticsNode) -> Boolean,
    isFocusableContainer: (SemanticsNode) -> Boolean,
): Boolean {
    if (!ComposeUiFlags.isSemanticsSortUnclippedFallbackEnabled) return false
    if (!unmergedConfig.isMergingSemanticsOfDescendants) return false
    if (explicitTraversalGroup == false || !hasChildren || !isVisible(this)) return false
    if (explicitTraversalGroup == null && !isFocusableContainer(this)) return false
    return isClipped
}

private val DefaultIsFocusableContainer: (SemanticsNode) -> Boolean = { false }

/**
 * Returns the results of geometry groupings, which is determined from 1) grouping nodes into
 * distinct, non-overlapping rows based on their top/bottom coordinates, then 2) sorting nodes
 * within each row with the semantics comparator.
 *
 * This method approaches traversal order with more nuance than an approach considering only just
 * hierarchy or only just an individual node's bounds.
 *
 * If [containerChildrenMapping] exists, there are additional children to add, as well as the sorted
 * parent itself
 *
 * If [useUnclippedBounds] is true, [parentListToSort] holds the descendants of a merging container
 * that is clipped by a viewport, and nodes are grouped and compared by their unclipped window
 * bounds so partially and fully clipped siblings share one coordinate space.
 */
internal fun SemanticsNode.sortByGeometryGroupings(
    parentListToSort: List<SemanticsNode>,
    isFocusableContainer: (SemanticsNode) -> Boolean = DefaultIsFocusableContainer,
    containerChildrenMapping: IntObjectMap<List<SemanticsNode>> = intObjectMapOf(),
    useUnclippedBounds: Boolean = false,
): List<SemanticsNode> {
    val layoutIsRtl = layoutInfo.layoutDirection == LayoutDirection.Rtl

    // RowGroupings list consists of pairs, first = a rectangle of the bounds of the row
    // and second = the list of nodes in that row
    val rowGroupings = ArrayList<Pair<Rect, MutableList<SemanticsNode>>>(parentListToSort.size / 2)

    for (entryIndex in 0..parentListToSort.lastIndex) {
        val currEntry = parentListToSort[entryIndex]
        val currRect = currEntry.sortingBoundsInWindow(useUnclippedBounds)
        // If this is the first entry, or vertical groups don't overlap
        if (entryIndex == 0 || !placedEntryRowOverlaps(rowGroupings, currEntry, currRect)) {
            rowGroupings.add(Pair(currRect, mutableListOf(currEntry)))
        } // otherwise, we've already iterated through, found and placed it in a matching group
    }

    // Sort the rows from top to bottom
    rowGroupings.sortWith(TopBottomBoundsComparator)

    val returnList = ArrayList<SemanticsNode>()
    val comparator = semanticComparator(layoutIsRtl, useUnclippedBounds)
    rowGroupings.fastForEach { row ->
        // Sort each individual row's parent nodes
        row.second.sortWith(comparator)
        returnList.addAll(row.second)
    }

    returnList.sortWith(UnmergedConfigComparator)

    var i = 0
    // Afterwards, go in and add the containers' children.
    while (i <= returnList.lastIndex) {
        val currNodeId = returnList[i].id
        // If a parent node is a container, then add its children.
        // Add all container's children after the container itself.
        // Because we've already recursed on the containers children, the children should
        // also be sorted by their traversal index
        val containersChildrenList = containerChildrenMapping[currNodeId]
        if (containersChildrenList != null) {
            val containerIsScreenReaderFocusable = isFocusableContainer(returnList[i])
            if (!containerIsScreenReaderFocusable) {
                // Container is removed if it is not screenreader-focusable
                returnList.removeAt(i)
            } else {
                // Increase counter if the container was not removed
                i += 1
            }
            // Add all the container's children and increase counter by the number of children
            returnList.addAll(i, containersChildrenList)
            i += containersChildrenList.size
        } else {
            // Advance to the next item
            i += 1
        }
    }
    return returnList
}

/**
 * Returns the node's window bounds for geometric traversal sorting.
 *
 * Within a clipped merging subtree ([useUnclippedBounds] == true), all sibling controls use
 * unclipped window bounds (`boundsInWindow(clipBounds = false)`) so partially clipped and fully
 * clipped children of an edge-straddling merging container remain in a consistent coordinate space.
 * Outside clipped merging subtrees, clipped [boundsInWindow] are used so clipped containers cannot
 * overlap unrelated outer headers, falling back to [unclippedBoundsInWindow] only when
 * [boundsInWindow] collapses to [Rect.Zero].
 */
@OptIn(ExperimentalComposeUiApi::class)
private fun SemanticsNode.sortingBoundsInWindow(useUnclippedBounds: Boolean): Rect {
    if (useUnclippedBounds) {
        return unclippedBoundsInWindow
    }
    val clipped = boundsInWindow
    if (!clipped.isEmpty || !ComposeUiFlags.isSemanticsSortUnclippedFallbackEnabled) {
        return clipped
    }
    return unclippedBoundsInWindow
}

private val SemanticsNode.isClipped: Boolean
    get() {
        val coordinator = findCoordinatorToGetBounds()?.takeIf { it.isAttached } ?: return false
        return coordinator.boundsInWindow(clipBounds = true) !=
            coordinator.boundsInWindow(clipBounds = false)
    }

private val SemanticsNode.unclippedBoundsInWindow: Rect
    get() =
        findCoordinatorToGetBounds()?.takeIf { it.isAttached }?.boundsInWindow(clipBounds = false)
            ?: Rect.Zero

// check to see if this entry overlaps with any groupings in rowGroupings
private fun placedEntryRowOverlaps(
    rowGroupings: ArrayList<Pair<Rect, MutableList<SemanticsNode>>>,
    node: SemanticsNode,
    entryBounds: Rect,
): Boolean {
    // Conversion to long is needed in order to utilize `until`, which has no float ver
    val entryTopCoord = entryBounds.top
    val entryBottomCoord = entryBounds.bottom
    val entryIsEmpty = entryTopCoord >= entryBottomCoord

    for (currIndex in 0..rowGroupings.lastIndex) {
        val currRect = rowGroupings[currIndex].first
        val groupIsEmpty = currRect.top >= currRect.bottom
        val groupOverlapsEntry =
            !entryIsEmpty &&
                !groupIsEmpty &&
                max(entryTopCoord, currRect.top) < min(entryBottomCoord, currRect.bottom)

        // If it overlaps with this row group, update cover and add node
        if (groupOverlapsEntry) {
            val newRect =
                currRect.intersect(0f, entryTopCoord, Float.POSITIVE_INFINITY, entryBottomCoord)
            // Replace the cover rectangle, copying over the old list of nodes
            rowGroupings[currIndex] = Pair(newRect, rowGroupings[currIndex].second)
            // Add current node
            rowGroupings[currIndex].second.add(node)
            // We've found an overlapping group, return true
            return true
        }
    }

    // If we've made it here, then there are no groups our entry overlaps with
    return false
}

@OptIn(ExperimentalComposeUiHostApi::class)
private object ZAndPlaceOrderComparator : Comparator<SemanticsNode> {
    override fun compare(a: SemanticsNode, b: SemanticsNode): Int {
        // compare by layoutInfo's zIndex and placement order, then by semanticsId
        val zCmp = a.layoutInfo.zIndex.compareTo(b.layoutInfo.zIndex)
        if (zCmp != 0) return zCmp
        val placeCmp = a.layoutInfo.placeOrder.compareTo(b.layoutInfo.placeOrder)
        if (placeCmp != 0) return placeCmp
        return a.id.compareTo(b.id)
    }
}

private fun semanticComparator(
    isRtl: Boolean,
    useUnclippedBounds: Boolean,
): Comparator<SemanticsNode> =
    when {
        isRtl && useUnclippedBounds -> RtlUnclippedComparator
        isRtl -> RtlClippedComparator
        useUnclippedBounds -> LtrUnclippedComparator
        else -> LtrClippedComparator
    }

private val LtrClippedComparator =
    LtrBoundsComparator(useUnclippedBounds = false).then(ZAndPlaceOrderComparator)
private val RtlClippedComparator =
    RtlBoundsComparator(useUnclippedBounds = false).then(ZAndPlaceOrderComparator)
private val LtrUnclippedComparator =
    LtrBoundsComparator(useUnclippedBounds = true).then(ZAndPlaceOrderComparator)
private val RtlUnclippedComparator =
    RtlBoundsComparator(useUnclippedBounds = true).then(ZAndPlaceOrderComparator)

private class LtrBoundsComparator(private val useUnclippedBounds: Boolean) :
    Comparator<SemanticsNode> {
    override fun compare(a: SemanticsNode, b: SemanticsNode): Int {
        // TODO: boundsInWindow is quite expensive and allocates several objects,
        // we need to fix this since this is called during sorting
        val ab = a.sortingBoundsInWindow(useUnclippedBounds)
        val bb = b.sortingBoundsInWindow(useUnclippedBounds)
        var r = ab.left.compareTo(bb.left)
        if (r != 0) return r
        r = ab.top.compareTo(bb.top)
        if (r != 0) return r
        r = ab.bottom.compareTo(bb.bottom)
        if (r != 0) return r
        return ab.right.compareTo(bb.right)
    }
}

private class RtlBoundsComparator(private val useUnclippedBounds: Boolean) :
    Comparator<SemanticsNode> {
    override fun compare(a: SemanticsNode, b: SemanticsNode): Int {
        // TODO: boundsInWindow is quite expensive and allocates several objects,
        // we need to fix this since this is called during sorting
        val ab = a.sortingBoundsInWindow(useUnclippedBounds)
        val bb = b.sortingBoundsInWindow(useUnclippedBounds)
        // We want to compare the right-most bounds, with the largest values first — that way
        // the nodes will be sorted from right to left. Since `compareTo` returns a positive
        // number if the first object is greater than the second, we want to call
        // `b.compareTo(a)`, since we want our values in descending order, rather than
        // ascending order.
        var r = bb.right.compareTo(ab.right)
        if (r != 0) return r
        // Since in RTL layouts we still read from top to bottom, we compare the top and
        // bottom bounds as usual.
        r = ab.top.compareTo(bb.top)
        if (r != 0) return r
        r = ab.bottom.compareTo(bb.bottom)
        if (r != 0) return r
        // We also want to sort the left bounds in descending order, so call `b.compareTo(a)`
        // here too.
        return bb.left.compareTo(ab.left)
    }
}

private object TopBottomBoundsComparator : Comparator<Pair<Rect, MutableList<SemanticsNode>>> {
    override fun compare(
        a: Pair<Rect, MutableList<SemanticsNode>>,
        b: Pair<Rect, MutableList<SemanticsNode>>,
    ): Int {
        val r = a.first.top.compareTo(b.first.top)
        if (r != 0) return r
        return a.first.bottom.compareTo(b.first.bottom)
    }
}

// Kotlin `sortWith` should just pull out the highest traversal indices, but keep everything
// else in place. If the element does not have a `traversalIndex` then `0f` will be used.
@OptIn(ExperimentalComposeUiHostApi::class)
internal object UnmergedConfigComparator : Comparator<SemanticsNode> {
    override fun compare(a: SemanticsNode, b: SemanticsNode): Int =
        (a.unmergedConfig.getOrNull(SemanticsProperties.TraversalIndex) ?: 0f).compareTo(
            b.unmergedConfig.getOrNull(SemanticsProperties.TraversalIndex) ?: 0f
        )
}
