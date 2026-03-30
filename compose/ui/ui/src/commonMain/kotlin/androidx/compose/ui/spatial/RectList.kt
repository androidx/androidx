/*
 * Copyright 2024 The Android Open Source Project
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

@file:Suppress("NOTHING_TO_INLINE", "KotlinRedundantDiagnosticSuppress")

package androidx.compose.ui.spatial

import androidx.compose.ui.internal.checkPrecondition
import androidx.compose.ui.internal.requirePrecondition
import kotlin.jvm.JvmField
import kotlin.math.max
import kotlin.math.min

/**
 * This is a fairly straight-forward data structure. It stores an Int value and a corresponding
 * rectangle, and allows for efficient querying based on the spatial relationships between the
 * objects contained in it, but it does so just by storing all of the information packed into a
 * single LongArray. Because of the simplicity and tight loops / locality of information, this ends
 * up being faster than most other data structures in most things for the size data sets that we
 * will be using this for. For O(10**2) items, this outperforms other data structures. Each
 * meta/rect pair is stored contiguously as 3 Longs in an LongArray. This makes insert and update
 * extremely cheap. Query operations require scanning the entire array, but due to cache locality
 * and fairly efficient math, it is competitive with data structures which use mechanisms to prune
 * the size of the data set to query less.
 *
 * The nodes that use these rects ([androidx.compose.ui.node.LayoutNode] at the time of writing) are
 * expected to cache their queried index to prevent lookup overhead. Most methods are accepting the
 * index along with the id and [indexOf] has an overload that allows to check the cached index and
 * fallback to the linear search if necessary.
 *
 * This data structure comes with some assumptions:
 * 1. the "identifier" values for this data structure are positive Ints. For performance reasons, we
 *    only store 25 bits of precision here, so practically speaking the item id is limited to 25
 *    bits (~33,000,000).
 * 2. The coordinate system used for this data structure has the positive "x" axis pointing to the
 *    "right", and the positive "y" axis pointing "down". As a result, a rectangle will always have
 *    top <= bottom, and left <= right.
 * 3. The parent is always inserted in the list before a child. With that we guarantee that we need
 *    to traverse back from the child index in order to find a parent. And the parent keeps track of
 *    the offset to the last child.
 */
@Suppress("NAME_SHADOWING")
internal class RectList {
    /**
     * This is the primary data storage. We store items linearly, with each "item" taking up three
     * longs (192 bits) of space. The partitioning generally looks like:
     *
     *        Long 1 (64 bits): the "top left" long
     *          32 bits: left
     *          32 bits: top
     *        Long 2 (64 bits): the "bottom right" long
     *          32 bits: right
     *          32 bits: bottom
     *        Long 3 (64 bits): the "meta" long
     *          25 bits: item id
     *          25 bits: parent id
     *          10 bits: last child offset
     *           1 bis: updated
     *           1 bit: focusable
     *           1 bit: gesturable
     *           1 bit: hasCallbacks
     */
    @JvmField internal var items: LongArray = LongArray(LongsPerItem * InitialSize)

    /**
     * We allocate a 2nd LongArray. This is always going to be sized identical to [items], and
     * during [defragment] we will swap the two in order to have a cheap defragment algorithm that
     * preserves order.
     *
     * @see [defragment]
     */
    @JvmField internal var stack: LongArray = LongArray(LongsPerItem * InitialSize)

    /**
     * The size of the items array that is filled with actual data. This is different from
     * `items.size` since the array is larger than the data it contains so that inserts can be
     * cheap.
     */
    @JvmField internal var itemsSize: Int = 0

    /** The number of items */
    val size: Int
        get() = itemsSize / LongsPerItem

    /**
     * Returns the 0th index of which 3 contiguous Longs can be stored in the items array. If space
     * is available at the end of the array, it will use that. If not, this will grow the items
     * array.This method will return an Int index that you can use, BUT, this method has side
     * effects and may mutate the [items] and [itemsSize] fields on this class. It is important to
     * keep this in mind if you call this method and have cached any of those values in a local
     * variable, you may need to refresh them.
     */
    private inline fun allocateItemsIndex(): Int {
        val currentItems = items
        val currentSize = itemsSize
        itemsSize = currentSize + LongsPerItem
        val actualSize = currentItems.size
        if (actualSize <= currentSize + LongsPerItem) {
            resizeStorage(actualSize, currentSize, currentItems)
        }
        return currentSize
    }

    private fun resizeStorage(actualSize: Int, currentSize: Int, currentItems: LongArray) {
        val newSize = max(actualSize * 2, currentSize + LongsPerItem)
        items = currentItems.copyOf(newSize)
        stack = stack.copyOf(newSize)
    }

    /**
     * Insert a value and corresponding bounding rectangle into the RectList. This method does not
     * check to see that [value] doesn't already exist somewhere in the list.
     *
     * NOTE: -1 is NOT a valid value for this collection since it is used as a tombstone value.
     *
     * @param value The value to be stored. Intended to be a layout node id. Must be a positive
     *   integer of 28 bits or less
     * @param l the left coordinate of the rectangle
     * @param t the top coordinate of the rectangle
     * @param r the right coordinate of the rectangle
     * @param b the bottom coordinate of the rectangle
     * @param parentId The semantic parent id (e.g. scrollable container) previously added to the
     *   RectList, or -1 if parent is not present.
     * @param parentIndex Index of the element with the [parentId] in the RectList or [NotFound] if
     *   parent is not present.
     * @param focusable true if this element is focusable. This is a flag which we can use to limit
     *   the results of certain queries for
     * @param gesturable true if this element is a pointer input gesture detector. This is a flag
     *   which we can use to limit the results of certain queries for.
     * @return index of the inserted element in the RectList.
     */
    fun insert(
        value: Int,
        l: Int,
        t: Int,
        r: Int,
        b: Int,
        parentId: Int,
        parentIndex: Int,
        focusable: Boolean,
        gesturable: Boolean,
        hasCallbacks: Boolean,
    ): Int {
        val value = value and MaxSupportedId
        val index = allocateItemsIndex()
        val items = items

        items[index + 0] = packXY(l, t)
        items[index + 1] = packXY(r, b)
        items[index + 2] =
            packMeta(
                value,
                parentId,
                lastChildOffset = 0,
                // TODO: consider the fact that we will be updating every rect on insert, and that
                //  will probably impact insert times somewhat negatively. We could potentially
                //  try and check whether or not a node has a "global rect listener" on it before
                //  insert, or alternatively "mark" the updated array when we add a listener so
                //  that we could avoid the "fire" for every rect in the collection. This might not
                //  be a big deal though so let's wait until we can measure and find out if it is
                //  a problem
                updated = true,
                focusable = focusable,
                gesturable = gesturable,
                hasCallbacks = hasCallbacks,
            )

        if (parentId == -1) return index
        val parentId = parentId and MaxSupportedId
        // After inserting, find the item with id = parentId and update it's "last child offset".
        checkPrecondition(parentIndex != NotFound) {
            "Inserted child $value without valid parent index"
        }
        val meta = items[parentIndex + 2]
        checkPrecondition(unpackMetaValue(meta) == parentId) {
            "Inserted child $value without valid parent index or parent $parentId not found"
        }

        val lastChildOffset = (index - parentIndex) / LongsPerItem
        items[parentIndex + 2] = metaWithLastChildOffset(meta, lastChildOffset)
        return index
    }

    /**
     * Insert a value and corresponding bounding rectangle into the RectList, to be used when we
     * only know the relative offset of this node from its parent. This method does not check to see
     * that [value] doesn't already exist somewhere in the list.
     *
     * @param value The value to be stored. Intended to be a layout node id. Must be a positive
     *   integer of 28 bits or less
     * @param parentIndex The index of the parent in the RectList.
     * @param offsetFromParentX the x offset from the parent
     * @param offsetFromParentY the y offset from the parent
     * @param width the width of the rectangle
     * @param height the height of the rectangle
     * @param focusable true if this element is focusable. This is a flag which we can use to limit
     *   the results of certain queries for
     * @param gesturable true if this element is a pointer input gesture detector. This is a flag
     *   which we can use to limit the results of certain queries for
     */
    fun insertBasedOnParentOffset(
        value: Int,
        parentId: Int,
        parentIndex: Int,
        offsetFromParentX: Int,
        offsetFromParentY: Int,
        width: Int,
        height: Int,
        focusable: Boolean,
        gesturable: Boolean,
        hasCallbacks: Boolean,
    ): Int {
        val value = value and MaxSupportedId
        val items = items

        val parentMeta = items[parentIndex + 2]
        requirePrecondition(unpackMetaValue(parentMeta) == (parentId and MaxSupportedId)) {
            "Inserted child $value without valid parent index or parent $parentId not found"
        }

        val parentLT = items[parentIndex + 0]
        val parentX = unpackX(parentLT)
        val parentY = unpackY(parentLT)
        val l = parentX + offsetFromParentX
        val t = parentY + offsetFromParentY
        val r = l + width
        val b = t + height

        return insert(
            value = value,
            l = l,
            t = t,
            r = r,
            b = b,
            parentId = parentId,
            parentIndex = parentIndex,
            focusable = focusable,
            gesturable = gesturable,
            hasCallbacks = hasCallbacks,
        )
    }

    fun removeAt(index: Int) {
        // To "remove" an item, we make the rectangle [max, max, max, max] so that it won't
        // match any queries, and we mark meta as [TombStone] so we can detect it later
        // in the defragment method
        items[index + 0] = ULong.MAX_VALUE.toLong()
        items[index + 1] = ULong.MAX_VALUE.toLong()
        items[index + 2] = TombStone
    }

    fun updateAt(index: Int, l: Int, t: Int, r: Int, b: Int) {
        items[index + 0] = packXY(l, t)
        items[index + 1] = packXY(r, b)
        items[index + 2] = metaMarkUpdatedIfHasCallbacks(items[index + 2])
    }

    /** Updates the focusable and/or gesturable flags found on given index. */
    fun updateFlagsAt(index: Int, focusable: Boolean, gesturable: Boolean) {
        items[index + 2] = metaMarkFlags(items[index + 2], focusable, gesturable)
    }

    fun updateHasCallbacksAt(index: Int, hasCallbacks: Boolean) {
        val meta = items[index + 2]
        items[index + 2] =
            metaMarkUpdatedAndHasCallbacks(
                meta,
                updated = hasCallbacks,
                hasCallbacks = hasCallbacks,
            )
    }

    /**
     * Moves the rectangle associated with this value to the specified rectangle, and updates every
     * item that is "below" the specified rectangle by the associated offset. move() is generally
     * more efficient than calling update() for all of the rectangles included in the subhierarchy
     * of the item.
     */
    fun moveAt(id: Int, index: Int, l: Int, t: Int, r: Int, b: Int) {
        val prevLT = items[index + 0]
        items[index + 0] = packXY(l, t)
        items[index + 1] = packXY(r, b)
        val meta = items[index + 2]
        items[index + 2] = metaMarkUpdatedIfHasCallbacks(meta)
        val deltaX = l - unpackX(prevLT)
        val deltaY = t - unpackY(prevLT)

        if (deltaX != 0 || deltaY != 0) {
            updateSubhierarchy(id, index, deltaX, deltaY)
        }
    }

    fun updateSubhierarchy(id: Int, index: Int, deltaX: Int, deltaY: Int) {
        updateSubhierarchy(
            stackMeta =
                packMeta(
                    itemId = id,
                    // item index is in parent id slot for this case
                    parentId = index,
                    lastChildOffset = itemsSize / LongsPerItem,
                    updated = false,
                    focusable = false,
                    gesturable = false,
                    hasCallbacks = false,
                ),
            deltaX = deltaX,
            deltaY = deltaY,
        )
    }

    /**
     * Updates a subhierarchy of items by the specified delta. For efficiency, the [stackMeta]
     * provided is a Long encoded with the same scheme of the "meta" long of each item, where the
     * encoding has the following semantic specific to this method:
     *
     *        Long (64 bits): the "stack meta" encoding
     *          25 bits: the "parent id" that we are matching on (normally item id)
     *          25 bits: the minimum index that a child can have (normally parent index)
     *          10 bits: max offset from start index a child can have (normally last child offset)
     *          next bits are unused
     *
     * We use this essentially as a way to encode three integers into a long, which includes all of
     * the data needed to efficiently iterate through the below algorithm. It is effectively an id
     * and a range. The range isn't strictly needed, but it helps turn this O(n^2) algorithm into
     * something that is ~O(n) in the average case (still O(n^2) worst case though). By using the
     * same encoding as "meta" longs, we only need to update the start index when we
     */
    private fun updateSubhierarchy(stackMeta: Long, deltaX: Int, deltaY: Int) {
        val items = items
        val stack = stack
        val size = itemsSize

        stack[0] = stackMeta
        var stackSize = 1
        while (stackSize > 0) {
            val idAndStartAndOffset = stack[--stackSize]
            val parentId = unpackMetaValue(idAndStartAndOffset) // parent id is in the id slot
            var i = unpackMetaParentId(idAndStartAndOffset) // start index is in the parent id slot
            val offset = unpackMetaLastChildOffset(idAndStartAndOffset)
            val endIndex =
                if (offset == MaxSupportedLastChildOffset) size else i + (offset * LongsPerItem)
            if (i < 0) break
            while (i < size - 2) {
                if (i >= endIndex) break
                val meta = items[i + 2]
                if (unpackMetaParentId(meta) == parentId) {
                    val topLeft = items[i + 0]
                    val bottomRight = items[i + 1]
                    items[i + 0] = packXY(unpackX(topLeft) + deltaX, unpackY(topLeft) + deltaY)
                    items[i + 1] =
                        packXY(unpackX(bottomRight) + deltaX, unpackY(bottomRight) + deltaY)
                    items[i + 2] = metaMarkUpdatedIfHasCallbacks(meta)
                    if (unpackMetaLastChildOffset(meta) > 0) {
                        // we need to store itemId, lastChildOffset, and a "start index".
                        // For convenience, we just use `meta` which already encodes two of those
                        // values, and we add `i` into the slot for "parentId"
                        stack[stackSize++] = metaWithParentId(meta, i + LongsPerItem)
                    }
                }
                i += LongsPerItem
            }
        }
    }

    /**
     * Moves the node when we only know the relative offset of this node from its parent. This
     * method allows to both find the parent offset and return the new child offset in one pass.
     */
    fun moveBasedOnParentOffset(
        id: Int,
        index: Int,
        parentIndex: Int,
        offsetFromParentX: Int,
        offsetFromParentY: Int,
        width: Int,
        height: Int,
    ) {
        val parentLT = items[parentIndex + 0]
        val parentX = unpackX(parentLT)
        val parentY = unpackY(parentLT)
        val l = parentX + offsetFromParentX
        val t = parentY + offsetFromParentY
        val r = l + width
        val b = t + height

        val oldLT = items[index + 0]
        val oldL = unpackX(oldLT)
        val oldT = unpackY(oldLT)
        val deltaX = l - oldL
        val deltaY = t - oldT
        val meta = items[index + 2]
        items[index + 0] = packXY(l, t)
        items[index + 1] = packXY(r, b)
        items[index + 2] = metaMarkUpdatedIfHasCallbacks(meta)

        if (deltaX != 0 || deltaY != 0) {
            updateSubhierarchy(id, index, deltaX, deltaY)
        }
    }

    fun markUpdatedAt(index: Int) {
        items[index + 2] = metaMarkUpdatedIfHasCallbacks(items[index + 2])
    }

    inline fun withRectAt(index: Int, block: (Int, Int, Int, Int) -> Unit) {
        val topLeft = items[index + 0]
        val bottomRight = items[index + 1]
        block(unpackX(topLeft), unpackY(topLeft), unpackX(bottomRight), unpackY(bottomRight))
    }

    inline fun withTopLeftBottomRightAt(index: Int, block: (Long, Long) -> Unit) {
        val topLeft = items[index + 0]
        val bottomRight = items[index + 1]
        block(topLeft, bottomRight)
    }

    fun getTopLeftAt(index: Int): Long {
        return items[index + 0]
    }

    /** Returns whether RectList contains an element with metadata value matching [value]. */
    operator fun contains(value: Int): Boolean = indexOf(value) != NotFound

    /**
     * Returns the first index for the item that matches the metadata value of [value], returns
     * [NotFound] if the item wasn't found.
     *
     * Note that returned index corresponds to the Long that contains the topLeft data of the item.
     */
    fun indexOf(value: Int): Int {
        val value = value and MaxSupportedId
        val items = items
        val size = itemsSize
        var i = 0
        while (i < size - 2) {
            val meta = items[i + 2]
            if (unpackMetaValue(meta) == value) {
                return i
            }
            i += LongsPerItem
        }
        return NotFound
    }

    /**
     * Returns the first index for the item that matches the metadata value of [value], returns
     * [NotFound] if the item wasn't found.
     *
     * This overload is more efficient as it allows to provide a [cachedIndex] where the item was
     * previously found. If the metadata on the cached item matches [value], the search is skipped.
     */
    inline fun indexOf(value: Int, cachedIndex: Int): Int {
        val items = items
        if (
            cachedIndex in (0..<itemsSize - 2) &&
                unpackMetaValue(items[cachedIndex + 2]) == value and MaxSupportedId
        ) {
            return cachedIndex
        }
        return indexOf(value)
    }

    fun metaAt(index: Int): Long {
        return items[index + 2]
    }

    /**
     * For a provided rectangle, executes [block] for each value in the collection whose associated
     * rectangle intersects the provided one. The argument passed into [block] will be the value.
     */
    inline fun forEachIntersection(l: Int, t: Int, r: Int, b: Int, block: (Int) -> Unit) {
        val destTopLeft = packXY(l, t)
        val destTopRight = packXY(r, b)
        val items = items
        val size = itemsSize
        var i = 0
        while (i < size - 2) {
            if (i >= size) break
            val topLeft = items[i + 0]
            val bottomRight = items[i + 1]
            if (rectIntersectsRect(topLeft, bottomRight, destTopLeft, destTopRight)) {
                // TODO: it might make sense to include the rectangle in the block since calling
                //  code may want to filter this list using that geometry, and it would be
                //  beneficial to not have to look up the layout node in order to do so.
                block(unpackMetaValue(items[i + 2]))
            }
            i += LongsPerItem
        }
    }

    /**
     * For each value in the collection, checks first if it is gesturable. If it is and it
     * intersects with the provided rectangle, the function executes [block]. The argument passed
     * into [block] will be the value (item id).
     */
    inline fun forEachGesturableIntersection(l: Int, t: Int, r: Int, b: Int, block: (Int) -> Unit) {
        val destTopLeft = packXY(l, t)
        val destTopRight = packXY(r, b)
        val items = items
        val size = itemsSize

        var i = 0
        while (i < items.size - 2) {
            if (i >= size) break
            if (unpackMetaGesturable(items[i + 2]) != 0) { // Checks gesturable is true
                val topLeft = items[i + 0]
                val bottomRight = items[i + 1]

                if (rectIntersectsRect(topLeft, bottomRight, destTopLeft, destTopRight)) {
                    // TODO: it might make sense to include the rectangle in the block since calling
                    //  code may want to filter this list using that geometry, and it would be
                    //  beneficial to not have to look up the layout node in order to do so.
                    block(unpackMetaValue(items[i + 2]))
                }
            }
            i += LongsPerItem
        }
    }

    /**
     * For each value in the collection, checks first if it is focusable. If it is and it intersects
     * with the provided rectangle, the function executes [block]. The argument passed into [block]
     * will be the value (item id).
     */
    inline fun forEachFocusableIntersection(l: Int, t: Int, r: Int, b: Int, block: (Int) -> Unit) {
        val destTopLeft = packXY(l, t)
        val destBottomRight = packXY(r, b)
        val items = items
        val size = itemsSize

        var i = 0
        while (i < items.size - 2) {
            if (i >= size) break
            if (unpackMetaFocusable(items[i + 2]) != 0) { // Checks focusable is true
                val topLeft = items[i + 0]
                val bottomRight = items[i + 1]

                if (rectIntersectsRect(topLeft, bottomRight, destTopLeft, destBottomRight)) {
                    block(unpackMetaValue(meta = items[i + 2]))
                }
            }
            i += LongsPerItem
        }
    }

    inline fun forEachRect(block: (Int, Int, Int, Int, Int) -> Unit) {
        val items = items
        val size = itemsSize
        var i = 0
        while (i < items.size - 2) {
            if (i >= size) break
            val topLeft = items[i + 0]
            val bottomRight = items[i + 1]
            val meta = items[i + 2]
            block(
                unpackMetaValue(meta),
                unpackX(topLeft),
                unpackY(topLeft),
                unpackX(bottomRight),
                unpackY(bottomRight),
            )
            i += LongsPerItem
        }
    }

    // TODO: add ability to filter to just gesture detectors (the main use case for this function)
    /**
     * For a provided point, executes [block] for each value in the collection whose associated
     * rectangle contains the provided point. The argument passed into [block] will be the value.
     */
    inline fun forEachIntersection(x: Int, y: Int, block: (Int) -> Unit) {
        val destXY = packXY(x, y)
        val items = items
        val size = itemsSize
        var i = 0
        while (i < items.size - 2) {
            if (i >= size) break
            val topLeft = items[i + 0]
            val bottomRight = items[i + 1]
            if (rectIntersectsRect(topLeft, bottomRight, destXY, destXY)) {
                val meta = items[i + 2]
                block(unpackMetaValue(meta))
            }
            i += LongsPerItem
        }
    }

    /**
     * For the rectangle at the given [index], calls [block] for each other rectangles that
     * intersects with it. The parameters in [block] are the intersecting rect and its 'value'.
     */
    inline fun forEachIntersectingRectWithValueAt(
        index: Int,
        block: (Int, Int, Int, Int, Int) -> Unit,
    ) {
        val items = items
        val size = itemsSize

        val destTopLeft = items[index]
        val destBottomRight = items[index + 1]

        var i = 0
        while (i < items.size - 2) {
            if (i >= size) break
            if (i == index) {
                i += LongsPerItem
                continue
            }
            val topLeft = items[i + 0]
            val bottomRight = items[i + 1]
            if (rectIntersectsRect(topLeft, bottomRight, destTopLeft, destBottomRight)) {
                block(
                    unpackX(topLeft),
                    unpackY(topLeft),
                    unpackX(bottomRight),
                    unpackY(bottomRight),
                    unpackMetaValue(items[i + 2]),
                )
            }
            i += LongsPerItem
        }
    }

    internal fun neighborsScoredByDistance(
        searchAxis: Int,
        l: Int,
        t: Int,
        r: Int,
        b: Int,
    ): IntArray {
        val items = items
        val size = itemsSize / LongsPerItem
        var i = 0
        // build up an array of size N with each element being the score for the item at that index
        val results = IntArray(size)

        while (i < results.size) {
            val itemsIndex = i * LongsPerItem
            if (itemsIndex < 0 || itemsIndex >= items.size - 1) break
            val topLeft = items[itemsIndex + 0]
            val bottomRight = items[itemsIndex + 1]
            val score =
                distanceScore(
                    searchAxis,
                    l,
                    t,
                    r,
                    b,
                    unpackX(topLeft),
                    unpackY(topLeft),
                    unpackX(bottomRight),
                    unpackY(bottomRight),
                )
            results[i] = score
            i++
        }
        return results
    }

    // TODO: add ability to filter to just focusable (the main use case for this function)
    // TODO: add an overload which just takes in searchAxis, k, and item id
    inline fun findKNearestNeighbors(
        searchAxis: Int,
        k: Int,
        l: Int,
        t: Int,
        r: Int,
        b: Int,
        block: (score: Int, id: Int, l: Int, t: Int, r: Int, b: Int) -> Unit,
    ) {
        // this list is 1:1 with items and holds the score for each item
        val list = neighborsScoredByDistance(searchAxis, l, t, r, b)
        val items = items

        var sent = 0
        var min = 1
        var nextMin = Int.MAX_VALUE
        var loops = 0
        var i = 0
        while (loops <= k) {
            while (i < list.size) {
                val score = list[i]
                // update nextmin if score is smaller than nextMin but larger than min
                if (score > min) {
                    nextMin = min(nextMin, score)
                }
                if (score == min) {
                    val itemIndex = i * LongsPerItem
                    val topLeft = items[itemIndex + 0]
                    val bottomRight = items[itemIndex + 1]
                    val meta = items[itemIndex + 2]
                    block(
                        score,
                        unpackMetaValue(meta),
                        unpackX(topLeft),
                        unpackY(topLeft),
                        unpackX(bottomRight),
                        unpackY(bottomRight),
                    )
                    sent++
                    if (sent == k) return
                }
                i++
            }
            min = nextMin
            nextMin = Int.MAX_VALUE
            loops++
            i = 0
        }
    }

    inline fun findNearestNeighbor(searchAxis: Int, l: Int, t: Int, r: Int, b: Int): Int {
        val items = items
        val size = itemsSize
        var minScore = Int.MAX_VALUE
        var minIndex = -1
        var i = 0
        while (i < items.size - 2) {
            if (i >= size) break
            val topLeft = items[i + 0]
            val bottomRight = items[i + 1]
            val score =
                distanceScore(
                    searchAxis,
                    l,
                    t,
                    r,
                    b,
                    unpackX(topLeft),
                    unpackY(topLeft),
                    unpackX(bottomRight),
                    unpackY(bottomRight),
                )
            val isNewMin = (score > 0) and (score < minScore)
            minScore = if (isNewMin) score else minScore
            minIndex = if (isNewMin) i + 1 else minIndex
            i += LongsPerItem
        }
        return if (minIndex < 0 || minIndex >= items.size) {
            -1
        } else {
            unpackMetaValue(items[minIndex])
        }
    }

    /**  */
    fun defragment() {
        val from = items
        val size = itemsSize
        val to = stack
        var i = 0
        var j = 0
        while (i < from.size - 2) {
            if (j >= to.size - 2) break
            if (i >= size) break
            if (from[i + 2] != TombStone) {
                to[j + 0] = from[i + 0]
                to[j + 1] = from[i + 1]
                to[j + 2] = from[i + 2]
                j += LongsPerItem
            }
            i += LongsPerItem
        }
        itemsSize = j
        // NOTE: this could be a reasonable time to shrink items/stack to a smaller array if for
        //  some reason they have gotten very large. I'm choosing NOT to do this because I think
        //  if the arrays have gotten to a large size it is very likely that they will get to that
        //  size again, and avoiding the thrash here is probably desirable
        items = to
        stack = from
    }

    fun clearUpdated() {
        val items = items
        val size = itemsSize
        var i = 0
        while (i < items.size - 2) {
            if (i >= size) break
            items[i + 2] = metaUnMarkUpdated(items[i + 2])
            i += LongsPerItem
        }
    }

    inline fun forEachUpdatedRect(block: (Int, Long, Long) -> Unit) {
        val items = items
        val size = itemsSize
        var i = 0
        while (i < items.size - 2) {
            if (i >= size) break
            val meta = items[i + 2]
            if (unpackMetaUpdated(meta) != 0) {
                val topLeft = items[i + 0]
                val bottomRight = items[i + 1]
                block(unpackMetaValue(meta), topLeft, bottomRight)
            }
            i += LongsPerItem
        }
    }

    fun debugString(): String = buildString {
        val items = items
        val size = itemsSize
        var i = 0
        while (i < items.size - 2) {
            if (i >= size) break
            val topLeft = items[i + 0]
            val bottomRight = items[i + 1]
            val meta = items[i + 2]
            val id = unpackMetaValue(meta)
            val parentId = unpackMetaParentId(meta)
            val l = unpackX(topLeft)
            val t = unpackY(topLeft)
            val r = unpackX(bottomRight)
            val b = unpackY(bottomRight)
            val lastChildOffset = unpackMetaLastChildOffset(meta)
            val updated = unpackMetaUpdated(meta)
            val focusable = unpackMetaFocusable(meta)
            val gesturable = unpackMetaGesturable(meta)
            appendLine(
                "id=$id, rect=[$l,$t,$r,$b], parent=$parentId, lastChildOffset=$lastChildOffset, updated=$updated, focusable=$focusable, gesturable=$gesturable"
            )
            i += LongsPerItem
        }
    }
}

internal const val LongsPerItem = 3
internal const val InitialSize = 64

private const val Lower25Bits = 0b0000_0001_1111_1111_1111_1111_1111_1111
internal const val Lower10Bits = 0b0000_0000_0000_0000_0000_0011_1111_1111

private const val MaxSupportedId = Lower25Bits
internal const val MaxSupportedLastChildOffset = Lower10Bits

internal const val BitOffsetForParentId = 25
internal const val BitOffsetForLastChildOffset = 50
internal const val BitOffsetForUpdated = 60
internal const val BitOffsetForFocusable = 61
internal const val BitOffsetForGesturable = 62
internal const val BitOffsetForHasCallbacks = 63

internal val EverythingButLastChildOffset =
    (ULong.MAX_VALUE xor (MaxSupportedLastChildOffset.toULong() shl BitOffsetForLastChildOffset))
        .toLong()
internal val EverythingButParentId =
    (ULong.MAX_VALUE xor (MaxSupportedId.toULong() shl BitOffsetForParentId)).toLong()

private const val PackedIntsLowestBit = 0x000_0001_0000_0001L
private const val PackedIntsHighestBit = -0x7FFF_FFFF_8000_0000L // 0x8000_0000_8000_0000UL

/**
 * This is the "meta" value that we assign to every removed value.
 *
 * @see RectList.removeAt
 * @see packMeta
 */
internal val TombStone = packMeta(-1, -1, 0, false, false, false, false)

internal const val NotFound = -(LongsPerItem + 1)

internal const val AxisNorth: Int = 0
internal const val AxisSouth: Int = 1
internal const val AxisWest: Int = 2
internal const val AxisEast: Int = 3

internal inline fun packXY(x: Int, y: Int) = (x.toLong() shl 32) or (y.toLong() and 0xffff_ffff)

/** see docs for [RectList.items] for the description on the structure of this long */
internal inline fun packMeta(
    itemId: Int,
    parentId: Int,
    lastChildOffset: Int,
    updated: Boolean,
    focusable: Boolean,
    gesturable: Boolean,
    hasCallbacks: Boolean,
): Long =
    (hasCallbacks.toLong() shl BitOffsetForHasCallbacks) or
        (gesturable.toLong() shl BitOffsetForGesturable) or
        (focusable.toLong() shl BitOffsetForFocusable) or
        (updated.toLong() shl BitOffsetForUpdated) or
        (minOf(lastChildOffset, MaxSupportedLastChildOffset).toLong() shl
            BitOffsetForLastChildOffset) or
        ((parentId and MaxSupportedId).toLong() shl BitOffsetForParentId) or
        ((itemId and MaxSupportedId).toLong())

internal inline fun unpackMetaValue(meta: Long): Int = meta.toInt() and MaxSupportedId

internal inline fun unpackMetaParentId(meta: Long): Int =
    (meta shr BitOffsetForParentId).toInt() and MaxSupportedId

/**
 * @return value which is not larger than [MaxSupportedLastChildOffset]. If this max value is
 *   returned, it means we don't know the last child offset, and the whole array should be checked.
 */
internal inline fun unpackMetaLastChildOffset(meta: Long): Int =
    (meta shr BitOffsetForLastChildOffset).toInt() and MaxSupportedLastChildOffset

internal inline fun metaWithParentId(meta: Long, parentId: Int): Long =
    (meta and EverythingButParentId) or
        ((parentId and MaxSupportedId).toLong() shl BitOffsetForParentId)

internal inline fun metaMarkUpdated(meta: Long): Long = meta or (1L shl BitOffsetForUpdated)

internal inline fun metaUnMarkUpdated(meta: Long): Long =
    meta and (1L shl BitOffsetForUpdated).inv()

internal inline fun metaMarkFlags(meta: Long, focusable: Boolean, gesturable: Boolean): Long {
    return (meta and
        (1L shl BitOffsetForFocusable).inv() and
        (1L shl BitOffsetForGesturable).inv()) or
        ((1L shl BitOffsetForFocusable) * focusable.toInt()) or
        ((1L shl BitOffsetForGesturable) * gesturable.toInt())
}

internal inline fun unpackMetaFocusable(meta: Long): Int =
    (meta shr BitOffsetForFocusable).toInt() and 0b1

internal inline fun unpackMetaGesturable(meta: Long): Int =
    (meta shr BitOffsetForGesturable).toInt() and 0b1

internal inline fun unpackMetaUpdated(meta: Long): Int =
    (meta shr BitOffsetForUpdated).toInt() and 0b1

internal inline fun unpackMetaHasCallbacks(meta: Long): Int =
    (meta shr BitOffsetForHasCallbacks).toInt() and 0b1

internal inline fun metaMarkUpdatedIfHasCallbacks(meta: Long): Long =
    meta or ((meta shr BitOffsetForHasCallbacks and 0b1) shl BitOffsetForUpdated)

internal inline fun metaMarkUpdatedAndHasCallbacks(
    meta: Long,
    updated: Boolean,
    hasCallbacks: Boolean,
): Long {
    return (meta and
        (1L shl BitOffsetForUpdated).inv() and
        (1L shl BitOffsetForHasCallbacks).inv()) or
        ((1L shl BitOffsetForUpdated) * updated.toInt()) or
        ((1L shl BitOffsetForHasCallbacks) * hasCallbacks.toInt())
}

/**
 * @param lastChildOffset if the value is larger [MaxSupportedLastChildOffset], then
 *   [MaxSupportedLastChildOffset] will be saved instead.
 */
internal inline fun metaWithLastChildOffset(meta: Long, lastChildOffset: Int): Long =
    (meta and EverythingButLastChildOffset) or
        ((minOf(lastChildOffset, MaxSupportedLastChildOffset)).toLong() shl
            BitOffsetForLastChildOffset)

internal inline fun unpackX(xy: Long): Int = (xy shr 32).toInt()

internal inline fun unpackY(xy: Long): Int = (xy).toInt()

/**  */
internal inline fun rectIntersectsRect(
    srcLT: Long,
    srcRB: Long,
    destLT: Long,
    destRB: Long,
): Boolean {
    // destRB - srcLT = [r2 - l1, b2 - t1]
    // srcRB - destLT = [r1 - l2, b1 - t2]

    // Both of the above expressions represent two long subtractions which are effectively each two
    // int subtractions. If any of the individual subtractions would have resulted in a negative
    // value, then the rectangle has no intersection. If this is true, then there will be
    // "underflow" from one 32bit component to the next, which we can detect by isolating the top
    // bits of each component using 0x8000_0000_8000_0000UL.toLong()

    // Since an intersection happens when both right/bottom corners are **larger** than the left/top
    // of the other (only top/left edges are inclusive in a Rect), we subtract a 1 to underflow in
    // the case they are the same.
    val a = (destRB - srcLT - PackedIntsLowestBit) or (srcRB - destLT - PackedIntsLowestBit)
    return a and PackedIntsHighestBit == 0L
}

/**
 * Turns a boolean into a long of 1L/0L for true/false. It is written precisely this way as this
 * results in a single ARM instruction where as other approaches are more expensive. For example,
 * `if (this) 1L else 0L` is several instructions instead of just one. DO NOT change this without
 * looking at the corresponding arm code and verifying that it is better.
 */
internal inline fun Boolean.toLong(): Long = (if (this) 1 else 0).toLong()

/**
 * This function will return a "score" of a rectangle relative to a query. A negative score means
 * that the rectangle should be ignored, and a lower (but non-negative) score means that the
 * rectangle is close and overlapping in the direction of the axis in question.
 *
 * @param axis the direction/axis along which we are scoring
 * @param queryL the left of the rect we are finding the nearest neighbors of
 * @param queryT the top of the rect we are finding the nearest neighbors of
 * @param queryR the right of the rect we are finding the nearest neighbors of
 * @param queryB the bottom of the rect we are finding the nearest neighbors of
 * @param l the left of the rect which is the "neighbor" we are scoring
 * @param t the top of the rect which is the "neighbor" we are scoring
 * @param r the right of the rect which is the "neighbor" we are scoring
 * @param b the bottom of the rect which is the "neighbor" we are scoring
 * @see AxisNorth
 * @see AxisWest
 * @see AxisEast
 * @see AxisSouth
 */
// TODO: consider just passing in TopLeft/BottomRight longs in order to reduce the number of
//  parameters here.
internal fun distanceScore(
    axis: Int,
    queryL: Int,
    queryT: Int,
    queryR: Int,
    queryB: Int,
    l: Int,
    t: Int,
    r: Int,
    b: Int,
): Int {
    return when (axis) {
        AxisNorth ->
            distanceScoreAlongAxis(
                distanceMin = queryT,
                distanceMax = b,
                queryCrossAxisMax = queryR,
                queryCrossAxisMin = queryL,
                crossAxisMax = r,
                crossAxisMin = l,
            )
        AxisEast ->
            distanceScoreAlongAxis(
                distanceMin = l,
                distanceMax = queryR,
                queryCrossAxisMax = queryB,
                queryCrossAxisMin = queryT,
                crossAxisMax = b,
                crossAxisMin = t,
            )
        AxisSouth ->
            distanceScoreAlongAxis(
                distanceMin = t,
                distanceMax = queryB,
                queryCrossAxisMax = queryR,
                queryCrossAxisMin = queryL,
                crossAxisMax = r,
                crossAxisMin = l,
            )
        AxisWest ->
            distanceScoreAlongAxis(
                distanceMin = queryL,
                distanceMax = r,
                queryCrossAxisMax = queryB,
                queryCrossAxisMin = queryT,
                crossAxisMax = b,
                crossAxisMin = t,
            )
        else -> Int.MAX_VALUE
    }
}

/**
 * This function will return a "score" of a rectangle relative to a query. A negative score means
 * that the rectangle should be ignored, and a low score means that
 */
internal fun distanceScoreAlongAxis(
    distanceMin: Int,
    distanceMax: Int,
    queryCrossAxisMax: Int,
    queryCrossAxisMin: Int,
    crossAxisMax: Int,
    crossAxisMin: Int,
): Int {
    // small positive means it is close to the right, negative means there is overlap or it is to
    // the left, which we will reject. We want small and positive.
    val distanceAlongAxis = distanceMin - distanceMax
    val maxOverlapPossible = queryCrossAxisMax - queryCrossAxisMin
    // 0 with full overlap, increasingly large negative numbers without
    val overlap =
        maxOverlapPossible + max(queryCrossAxisMin, crossAxisMin) -
            min(queryCrossAxisMax, crossAxisMax)

    return (distanceAlongAxis + 1) * (overlap + 1)
}
