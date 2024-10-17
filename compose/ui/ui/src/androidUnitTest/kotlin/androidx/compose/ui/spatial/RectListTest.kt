/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.spatial

import androidx.collection.mutableIntListOf
import androidx.compose.ui.util.fastForEach
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RectListTest {

    @Test
    fun testInsert() {
        val list = RectList()
        list.insert(1, 1, 1, 2, 2)
        assertIntersections(list, 1, 1, 2, 2, setOf(1))
    }

    @Test
    fun testInsertsAndIntersections() {
        val list = RectList()
        // top left, 1x1 rect at 1,1
        list.insert(1, 1, 1, 2, 2)
        // top right, 1x1 rect at 11,1
        list.insert(2, 11, 1, 12, 2)
        // bottom left, 1x1 rect at 1,11
        list.insert(3, 1, 11, 2, 12)
        // bottom right, 1x1 rect at 11,11
        list.insert(4, 11, 11, 12, 12)
        // middle, 2,2 rect at 9,9
        list.insert(5, 9, 9, 11, 11)
        // top left, 1x1 rect at 5,5
        list.insert(6, 5, 5, 6, 6)

        // 1x1 rect at 3,3. nothing intersects.
        assertIntersections(list, 3, 3, 4, 4, emptySet())

        // top left
        assertIntersections(list, 0, 0, 10, 10, setOf(1, 5, 6))

        // top right
        assertIntersections(list, 10, 0, 20, 10, setOf(5, 2))

        // bottom left
        assertIntersections(list, 0, 10, 10, 20, setOf(5, 3))

        // bottom right
        assertIntersections(list, 10, 10, 20, 20, setOf(5, 4))
    }

    @Test
    fun testInsertExampleData() {
        val list = RectList()
        val testData = exampleLayoutRects
        for (i in testData.indices) {
            val rect = testData[i]
            list.insert(
                i,
                rect[0],
                rect[1],
                rect[2],
                rect[3],
            )
        }
    }

    @Test
    fun insertUpdateClearUpdatedRemove() {
        val list = RectList()
        list.insert(1, 1, 1, 2, 2)
        list.update(1, 2, 2, 3, 3)
        list.remove(1)
        list.clearUpdated()
        list.defragment()
        assertEquals(0, list.size)
    }

    @Test
    fun testFindIntersectingPoint() {
        val testData = exampleLayoutRects
        val queries = pointerInputQueries

        // first 17 rects are big enough that they cover all queries
        val bigRects = listOf(0, 1, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 210, 223)

        val expectedResults =
            arrayOf(
                bigRects +
                    listOf(43, 44, 45, 46, 88, 106, 107, 161, 162, 163, 164, 170, 171, 173, 174),
                bigRects + listOf(43, 44, 45, 46, 47, 48, 49, 55, 56),
                bigRects + listOf(43, 44, 45, 46, 47, 48, 57, 58, 59, 65, 66, 67, 68),
                bigRects + listOf(43, 44, 45, 46, 88, 89, 93, 94, 95, 96, 97, 98, 99, 104),
                bigRects + listOf(16, 17, 18, 25, 26),
            )

        // we do a manual `rectContainsPoint` query here to validate that for all of the expected
        // results, it returns true
        for (i in expectedResults.indices) {
            val x = queries[i][0]
            val y = queries[i][1]
            val results = expectedResults[i]
            for (j in results.indices) {
                val itemId = results[j]
                val rect = exampleLayoutRects[itemId]
                val (l, r, t, b) = rect
                assert(
                    rectContainsPoint(
                        x,
                        y,
                        l,
                        r,
                        t,
                        b,
                    )
                )
            }
        }

        // populate the list
        val rectList = RectList()
        for (i in testData.indices) {
            val rect = testData[i]
            rectList.insert(
                i,
                rect[0],
                rect[1],
                rect[2],
                rect[3],
            )
        }
        // assert that forEachIntersection returns the expected results for each query
        for (i in queries.indices) {
            val list = mutableListOf<Int>()
            val point = queries[i]
            rectList.forEachIntersection(
                point[0],
                point[1],
            ) {
                list.add(it)
            }
            assertEquals(expectedResults[i].sorted(), list.sorted())
        }
    }

    private fun insertRecursive(qt: RectList, item: Item, scrollableId: Int) {
        val bounds = item.bounds

        qt.insert(
            item.id,
            bounds[0],
            bounds[1],
            bounds[2],
            bounds[3],
            parentId = scrollableId,
        )
        item.children.fastForEach {
            insertRecursive(qt, it, if (item.scrollable) item.id else scrollableId)
        }
    }

    @Test
    fun testUpdate() {
        val testData = exampleLayoutRects
        val list = RectList()
        insertRecursive(list, rootItem, -1)
        val bounds = testData[100]

        assertValueHasRect(list, 100, bounds[0], bounds[1], bounds[2], bounds[3])

        list.update(100, 1, 2, 3, 4)

        assertValueHasRect(list, 100, 1, 2, 3, 4)
    }

    private fun assertValueHasRect(list: RectList, itemId: Int, l: Int, t: Int, r: Int, b: Int) {
        var called = false
        val expected = "[$l,$t,$r,$b]"
        list.withRect(itemId) { w, x, y, z ->
            called = true
            assertEquals(expected, "[$w,$x,$y,$z]")
        }
        if (!called) {
            error("RectList did not have an item with id $itemId")
        }
    }

    @Test
    fun testUpdateAllItems() {
        val testData = exampleLayoutRects
        val r = Random(1234)
        val list = RectList()
        insertRecursive(list, rootItem, -1)
        for (i in testData.indices) {
            val rect = testData[i]
            val x = r.nextInt(-100, 100)
            val y = r.nextInt(-100, 100)
            list.update(
                i,
                min(max(rect[0] + x, 0), 1439),
                min(max(rect[1] + y, 0), 3119),
                min(max(rect[2] + x, 0), 1439),
                min(max(rect[3] + y, 0), 3119),
            )
        }
    }

    @Test
    fun testUpdateScrollableContainer() {
        val scrollableItems = scrollableItems
        val r = Random(1234)
        val qt = RectList()
        insertRecursive(qt, rootItem, -1)
        scrollableItems.fastForEach {
            val x = r.nextInt(-100, 100)
            val y = r.nextInt(-100, 100)
            val bounds = it.bounds
            qt.update(
                it.id,
                max(bounds[0] + x, 0),
                max(bounds[1] + y, 0),
                max(bounds[2] + x, 0),
                max(bounds[3] + y, 0),
            )
        }
    }

    @Test
    fun testNearestNeighbor() {
        val list = RectList()
        for (x in 0 until 10) {
            for (y in 0 until 10) {
                val id = x * 10 + y
                list.insert(id, 10 * x, 10 * y, 10 * x + 10, 10 * y + 10)
            }
        }

        val expectedResults =
            arrayOf(
                // arrays of [x, y, score]
                intArrayOf(4, 3, 1), // immediate to the right should definitely be the winner
                intArrayOf(
                    4,
                    2,
                    11
                ), // "up one" should tie with "down one" but still be a lowish score
                intArrayOf(
                    4,
                    4,
                    11
                ), // "up one" should tie with "down one" but still be a lowish score
                // TODO: we can tweak the scoring algorithm to have a higher penalty for not
                //  overlapping, which might put this rectangle in 2nd place. The current focus algo
                //  seems to heavily prioritize "in beam" elements, which are ones that would have
                //  overlap, and might place this rectangle higher
                intArrayOf(5, 3, 11), // two to the right should not win, but also have a low score.
            )

        var i = 0
        // nearest neighbor to the right of
        list.findKNearestNeighbors(AxisEast, 4, 30, 30, 40, 40) { score, id, _, _, _, _ ->
            val x = id / 10
            val y = id % 10
            val expected = expectedResults[i]
            assertEquals(expected[0], x)
            assertEquals(expected[1], y)
            assertEquals(expected[2], score)
            i++
        }
    }

    @Test
    fun testFindNearestNeighborInDirection() {
        val testData = exampleLayoutRects
        val queries = nearestNeighborQueries
        val numberOfResults = 4
        val qt = RectList()
        for (i in testData.indices) {
            val rect = testData[i]
            qt.insert(
                i,
                rect[0],
                rect[1],
                rect[2],
                rect[3],
            )
        }
        for (i in queries.indices) {
            for (direction in 1..4) {
                val list = mutableIntListOf()
                val bounds = queries[i]
                qt.findKNearestNeighbors(
                    direction,
                    numberOfResults,
                    bounds[0],
                    bounds[1],
                    bounds[2],
                    bounds[3],
                ) { _, id, _, _, _, _ ->
                    list.add(id)
                }
            }
        }
    }

    @Test
    fun testRectanglePacking() {
        val rect = packXY(1, 2)
        assertEquals(1, unpackX(rect))
        assertEquals(2, unpackY(rect))
    }

    @Test
    fun testMaxValueRectanglePacking() {
        val maxValue = Int.MAX_VALUE

        val rect = packXY(maxValue, 0)
        assertEquals(maxValue, unpackX(rect))
        assertEquals(0, unpackY(rect))

        val rect1 = packXY(0, maxValue)
        assertEquals(0, unpackX(rect1))
        assertEquals(maxValue, unpackY(rect1))

        val rect2 = packXY(maxValue, maxValue)
        assertEquals(maxValue, unpackX(rect2))
        assertEquals(maxValue, unpackY(rect2))
    }

    @Test
    fun testMinValueRectanglePacking() {
        val minValue = Int.MIN_VALUE

        val rect = packXY(minValue, 0)
        assertEquals(minValue, unpackX(rect))
        assertEquals(0, unpackY(rect))

        val rect1 = packXY(0, minValue)
        assertEquals(0, unpackX(rect1))
        assertEquals(minValue, unpackY(rect1))

        val rect2 = packXY(minValue, minValue)
        assertEquals(minValue, unpackX(rect2))
        assertEquals(minValue, unpackY(rect2))
    }

    @Test
    fun testMetaPacking() {
        val meta =
            packMeta(
                itemId = 1,
                parentId = 2,
                lastChildOffset = 3,
                updated = true,
                focusable = false,
                gesturable = true
            )
        assertEquals(1, unpackMetaValue(meta))
        assertEquals(2, unpackMetaParentId(meta))
        assertEquals(3, unpackMetaLastChildOffset(meta))
        assertEquals(1, unpackMetaUpdated(meta))
        assertEquals(0, unpackMetaFocusable(meta))
        assertEquals(1, unpackMetaGesturable(meta))
    }

    @Test
    fun testMetaPackingNegativeScrollableValue() {
        val meta =
            packMeta(
                itemId = 10,
                parentId = -1,
                lastChildOffset = 0,
                updated = true,
                focusable = true,
                gesturable = false,
            )
        assertEquals(10, unpackMetaValue(meta))
        // TODO: this actually returns 268,435,455. Not sure if we need to change this or not.
        // assertEquals(-1, unpackMetaParentScrollableValue(meta))
        assertEquals(1, unpackMetaUpdated(meta))
        assertEquals(1, unpackMetaFocusable(meta))
        assertEquals(0, unpackMetaGesturable(meta))
    }

    private fun rectIntersectsRect(src: Rect, l: Int, t: Int, r: Int, b: Int): Boolean {
        return rectIntersectsRect(
            packXY(src.l, src.t),
            packXY(src.r, src.b),
            packXY(l, t),
            packXY(r, b),
        )
    }

    private fun distanceScore(axis: Int, query: Rect, target: Rect): Int {
        return distanceScore(
            axis,
            query.l,
            query.t,
            query.r,
            query.b,
            target.l,
            target.t,
            target.r,
            target.b,
        )
    }

    @Test
    fun testRectIntersectsRect() {
        val src = Rect(10, 10, 20, 20)

        // Not overlapping or touching
        // ====

        // top left
        assertFalse(rectIntersectsRect(src, 1, 1, 2, 2))

        // top right
        assertFalse(rectIntersectsRect(src, 24, 1, 25, 2))

        // bottom left
        assertFalse(rectIntersectsRect(src, 1, 23, 2, 24))

        // bottom right
        assertFalse(rectIntersectsRect(src, 24, 24, 25, 25))

        // top
        assertFalse(rectIntersectsRect(src, 15, 5, 16, 6))

        // left
        assertFalse(rectIntersectsRect(src, 5, 15, 6, 16))

        // bottom
        assertFalse(rectIntersectsRect(src, 15, 25, 16, 26))

        // right
        assertFalse(rectIntersectsRect(src, 25, 15, 26, 16))

        // Touching but not Overlapping
        // ====

        // just touches top left corner
        assertTrue(rectIntersectsRect(src, 1, 1, 10, 10))

        // just touches top right corner
        assertTrue(rectIntersectsRect(src, 20, 1, 30, 10))

        // just touches bottom right corner
        assertTrue(rectIntersectsRect(src, 20, 20, 30, 30))

        // just touches bottom left corner
        assertTrue(rectIntersectsRect(src, 1, 20, 10, 30))

        // left side is touching but not overlapping
        assertTrue(rectIntersectsRect(src, 1, 10, 10, 20))

        // right side is touching but not overlapping
        assertTrue(rectIntersectsRect(src, 20, 10, 30, 20))

        // top side is touching but not overlapping
        assertTrue(rectIntersectsRect(src, 10, 1, 20, 10))

        // bottom side is touching but not overlapping
        assertTrue(rectIntersectsRect(src, 10, 20, 20, 30))

        // Clear Intersection
        // ===

        // partial overlap in top left corner
        assertTrue(rectIntersectsRect(src, 1, 1, 11, 11))

        // src is inside of dest
        assertTrue(rectIntersectsRect(src, 1, 1, 30, 30))

        // dest is inside of src
        assertTrue(rectIntersectsRect(src, 15, 15, 16, 16))

        // full exact overlap
        assertTrue(rectIntersectsRect(src, 10, 10, 20, 20))

        // Zero Area Rectangles
        // ===

        // destination is zero rect outside of src
        assertFalse(rectIntersectsRect(src, 1, 1, 1, 1))

        // destination is zero rect inside of src
        assertTrue(rectIntersectsRect(src, 15, 15, 15, 15))

        // destination is zero rect with height inside of src
        assertTrue(rectIntersectsRect(src, 15, 15, 15, 16))

        // destination is zero rect with width inside of src
        assertTrue(rectIntersectsRect(src, 15, 15, 16, 15))

        // src is zero rect outside of dest
        assertFalse(
            rectIntersectsRect(
                Rect(1, 1, 1, 1),
                10,
                10,
                20,
                20,
            )
        )

        // src is zero rect inside of dest
        assertTrue(
            rectIntersectsRect(
                Rect(15, 15, 15, 15),
                10,
                10,
                20,
                20,
            )
        )

        // src is zero rect with height inside of dest
        assertTrue(
            rectIntersectsRect(
                Rect(15, 15, 15, 16),
                10,
                10,
                20,
                20,
            )
        )

        // src is zero rect with width inside of dest
        assertTrue(
            rectIntersectsRect(
                Rect(15, 15, 16, 15),
                10,
                10,
                20,
                20,
            )
        )
    }

    @Test
    fun testDistanceScore() {
        val queryRect = Rect(10, 10, 20, 20)

        // Negative Distance (opposite axis)
        // ===

        // Any rectangle which overlaps with the query rectangle should be
        // disallowed (negative value)
        assertNegative(distanceScore(AxisEast, queryRect, Rect(11, 11, 19, 19)))

        assertNegative(distanceScore(AxisSouth, queryRect, Rect(11, 11, 19, 19)))

        assertNegative(distanceScore(AxisWest, queryRect, Rect(11, 11, 19, 19)))

        assertNegative(distanceScore(AxisNorth, queryRect, Rect(11, 11, 19, 19)))

        // Perfect Overlaps, edges touching
        // ===

        // "perfect" overlap to the east of this rect, we expect
        // a low but positive score
        assertEquals(distanceScore(AxisEast, queryRect, Rect(20, 10, 30, 20)), 1)

        // "perfect" overlap to the south of this rect, we expect
        // a low but positive score
        assertEquals(distanceScore(AxisSouth, queryRect, Rect(10, 20, 20, 30)), 1)

        // "perfect" overlap to the west of this rect, we expect
        // a low but positive score
        assertEquals(distanceScore(AxisWest, queryRect, Rect(0, 10, 10, 20)), 1)

        // "perfect" overlap to the north of this rect, we expect
        // a low but positive score
        assertEquals(distanceScore(AxisNorth, queryRect, Rect(10, 0, 20, 10)), 1)

        // 1,1 rectangle 2px away along axis. Should be positive, but smaller number
        // ===

        assertEquals(distanceScore(AxisEast, queryRect, Rect(22, 15, 23, 16)), 30)

        assertEquals(distanceScore(AxisSouth, queryRect, Rect(15, 22, 16, 23)), 30)

        assertEquals(distanceScore(AxisWest, queryRect, Rect(7, 15, 8, 16)), 30)

        assertEquals(distanceScore(AxisNorth, queryRect, Rect(15, 7, 16, 8)), 30)

        // 1,1 rectangle 10px away along axis. Should be positive, but larger number
        // ===

        assertEquals(distanceScore(AxisEast, queryRect, Rect(30, 15, 31, 16)), 110)

        assertEquals(distanceScore(AxisSouth, queryRect, Rect(15, 30, 16, 31)), 110)

        assertEquals(distanceScore(AxisWest, queryRect, Rect(0, 15, 1, 16)), 100)

        assertEquals(distanceScore(AxisNorth, queryRect, Rect(15, 0, 16, 1)), 100)
    }

    @Test
    fun testDefragment() {
        val r = RectList()

        val toRemove = listOf(2, 7, 8)

        for (i in 0 until 10) {
            r.insert(
                i,
                1,
                1,
                2,
                2,
            )
        }

        assertEquals(30, r.itemsSize)

        for (i in toRemove) {
            r.remove(i)
        }

        // itemsSize still won't change, since the removed items are just
        // tombstoned at this point
        assertEquals(30, r.itemsSize)

        for (i in 0 until 10) {
            if (i !in toRemove) {
                assertRectWithIdEquals(r, i, 1, 1, 2, 2)
            }
        }

        r.defragment()

        assertEquals(21, r.itemsSize)

        for (i in 0 until 10) {
            if (i !in toRemove) {
                assertRectWithIdEquals(r, i, 1, 1, 2, 2)
            }
        }
    }

    @Test
    fun testUpdateScrollable2() {
        val r = RectList()

        // insert scrollable container
        r.insert(
            1,
            10,
            10,
            20,
            20,
        )

        // insert child container
        r.insert(
            2,
            10,
            10,
            20,
            20,
            parentId = 1,
        )

        assertRectWithIdEquals(r, 2, 10, 10, 20, 20)

        // move child items up by 1
        r.updateSubhierarchy(
            id = 1,
            deltaX = 0,
            deltaY = -1,
        )

        assertRectWithIdEquals(r, 2, 10, 9, 20, 19)

        // move child items up by 10 more
        r.updateSubhierarchy(
            id = 1,
            deltaX = 0,
            deltaY = -10,
        )

        assertRectWithIdEquals(r, 2, 10, -1, 20, 9)
    }

    // TODO: test update scrollable behavior
    // TODO: test point intersection

    private data class Rect(val l: Int, val t: Int, val r: Int, val b: Int)
}

fun assertNegative(actual: Int) {
    assert(actual < 0) { "Expected negative value, got $actual" }
}

internal fun assertIntersections(
    grid: RectList,
    l: Int,
    t: Int,
    r: Int,
    b: Int,
    expected: Set<Int>
) {
    val actualSet = mutableSetOf<Int>()
    grid.forEachIntersection(l, t, r, b) {
        assert(actualSet.add(it)) { "Encountered $it more than once" }
    }
    assertEquals(expected, actualSet)
}

internal fun rectContainsPoint(
    x: Int,
    y: Int,
    l: Int,
    t: Int,
    r: Int,
    b: Int,
): Boolean {
    return (l < x) and (x < r) and (t < y) and (y < b)
}

internal fun assertRectWithIdEquals(
    rectList: RectList,
    id: Int,
    l: Int,
    t: Int,
    r: Int,
    b: Int,
) {
    rectList.withRect(id) { w, x, y, z ->
        assertRectEquals(
            l,
            t,
            r,
            b,
            w,
            x,
            y,
            z,
        )
    }
}

fun assertRectEquals(
    l1: Int,
    t1: Int,
    r1: Int,
    b1: Int,
    l2: Int,
    t2: Int,
    r2: Int,
    b2: Int,
) {
    assert(l1 == l2 && t1 == t2 && r1 == r2 && b1 == b2) {
        "Expected: [$l1, $t1, $r1, $b1] Actual: [$l2, $t2, $r2, $b2]"
    }
}
