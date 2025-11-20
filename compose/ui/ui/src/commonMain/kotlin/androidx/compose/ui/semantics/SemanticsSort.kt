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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEach
import kotlin.math.max
import kotlin.math.min

/**
 * This function prepares a subtree for `sortByGeometryGroupings` by retrieving all non-container
 * nodes and adding them to the list to be geometrically sorted. We recurse on containers (if they
 * exist) and add their sorted children to an optional mapping. The list to be sorted and child
 * mapping is passed into `sortByGeometryGroupings`.
 */
internal fun SemanticsNode.subtreeSortedByGeometryGrouping(
    isVisible: (SemanticsNode) -> Boolean,
    isFocusableContainer: (SemanticsNode) -> Boolean,
    listToSort: List<SemanticsNode>,
): List<SemanticsNode> {
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
        )
    }

    return sortByGeometryGroupings(geometryList, isFocusableContainer, containerMapToChildren)
}

private fun SemanticsNode.geometryDepthFirstSearch(
    geometryList: ArrayList<SemanticsNode>,
    isVisible: (SemanticsNode) -> Boolean,
    isFocusableContainer: (SemanticsNode) -> Boolean,
    containerMapToChildren: MutableIntObjectMap<List<SemanticsNode>>,
) {
    // We only want to add children that are either traversalGroups or are
    // screen reader focusable. The child must also be in the current pruned semantics tree.
    val isTraversalGroup = unmergedConfig.getOrElse(SemanticsProperties.IsTraversalGroup) { false }

    if ((isTraversalGroup || isFocusableContainer(this)) && isVisible(this)) {
        geometryList.add(this)
    }
    if (isTraversalGroup) {
        // Recurse and record the container's children, sorted
        containerMapToChildren[id] =
            subtreeSortedByGeometryGrouping(isVisible, isFocusableContainer, children)
    } else {
        // Otherwise, continue adding children to the list that'll be sorted regardless of hierarchy
        children.fastForEach { child ->
            child.geometryDepthFirstSearch(
                geometryList,
                isVisible,
                isFocusableContainer,
                containerMapToChildren,
            )
        }
    }
}

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
 */
internal fun SemanticsNode.sortByGeometryGroupings(
    parentListToSort: List<SemanticsNode>,
    isFocusableContainer: (SemanticsNode) -> Boolean = { false },
    containerChildrenMapping: IntObjectMap<List<SemanticsNode>> = intObjectMapOf(),
): List<SemanticsNode> {
    val layoutIsRtl = layoutInfo.layoutDirection == LayoutDirection.Rtl

    // RowGroupings list consists of pairs, first = a rectangle of the bounds of the row
    // and second = the list of nodes in that row
    val rowGroupings = ArrayList<Pair<Rect, MutableList<SemanticsNode>>>(parentListToSort.size / 2)

    for (entryIndex in 0..parentListToSort.lastIndex) {
        val currEntry = parentListToSort[entryIndex]
        // If this is the first entry, or vertical groups don't overlap
        if (entryIndex == 0 || !placedEntryRowOverlaps(rowGroupings, currEntry)) {
            val newRect = currEntry.boundsInWindow
            rowGroupings.add(Pair(newRect, mutableListOf(currEntry)))
        } // otherwise, we've already iterated through, found and placed it in a matching group
    }

    // Sort the rows from top to bottom
    rowGroupings.sortWith(TopBottomBoundsComparator)

    val returnList = ArrayList<SemanticsNode>()
    val comparator = semanticComparators[if (layoutIsRtl) 0 else 1]
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

// check to see if this entry overlaps with any groupings in rowGroupings
private fun placedEntryRowOverlaps(
    rowGroupings: ArrayList<Pair<Rect, MutableList<SemanticsNode>>>,
    node: SemanticsNode,
): Boolean {
    // Conversion to long is needed in order to utilize `until`, which has no float ver
    val entryTopCoord = node.boundsInWindow.top
    val entryBottomCoord = node.boundsInWindow.bottom
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

private val semanticComparators: Array<Comparator<SemanticsNode>> =
    Array(2) { index ->
        val comparator =
            when (index) {
                0 -> RtlBoundsComparator
                else -> LtrBoundsComparator
            }
        comparator
            // then compare by layoutNode's zIndex and placement order
            .thenBy(LayoutNode.ZComparator) { it.layoutNode }
            // then compare by semanticsId to break the tie somehow
            .thenBy { it.id }
    }

private object LtrBoundsComparator : Comparator<SemanticsNode> {
    override fun compare(a: SemanticsNode, b: SemanticsNode): Int {
        // TODO: boundsInWindow is quite expensive and allocates several objects,
        // we need to fix this since this is called during sorting
        val ab = a.boundsInWindow
        val bb = b.boundsInWindow
        var r = ab.left.compareTo(bb.left)
        if (r != 0) return r
        r = ab.top.compareTo(bb.top)
        if (r != 0) return r
        r = ab.bottom.compareTo(bb.bottom)
        if (r != 0) return r
        return ab.right.compareTo(bb.right)
    }
}

private object RtlBoundsComparator : Comparator<SemanticsNode> {
    override fun compare(a: SemanticsNode, b: SemanticsNode): Int {
        // TODO: boundsInWindow is quite expensive and allocates several objects,
        // we need to fix this since this is called during sorting
        val ab = a.boundsInWindow
        val bb = b.boundsInWindow
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
private val UnmergedConfigComparator: (SemanticsNode, SemanticsNode) -> Int = { a, b ->
    a.unmergedConfig
        .getOrElse(SemanticsProperties.TraversalIndex) { 0f }
        .compareTo(b.unmergedConfig.getOrElse(SemanticsProperties.TraversalIndex) { 0f })
}
