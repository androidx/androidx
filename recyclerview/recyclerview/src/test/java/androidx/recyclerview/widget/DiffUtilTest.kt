/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.recyclerview.widget

import java.util.Random
import java.util.UUID
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DiffUtilTest {
    private val before = mutableListOf<Item>()
    private val after = mutableListOf<Item>()
    private val log = StringBuilder()
    private val callback = ItemListCallback(
        oldList = before,
        newList = after,
        assertCalls = true
    )

    init {
        Item.idCounter = 0
    }

    @Rule
    @JvmField
    val logOnExceptionWatcher: TestWatcher =
        object : TestWatcher() {
            override fun failed(
                e: Throwable,
                description: Description
            ) {
                System.err.println(
                    """
                    LOG:
                    $log
                    END_LOG
                    """.trimIndent()
                )
            }
        }

    @Test
    fun testNoChange() {
        initWithSize(5)
        check()
    }

    @Test
    fun testAddItems() {
        initWithSize(2)
        add(1)
        check()
    }

    // @Test
    // Used for development
    // @Suppress("unused")
    fun testRandom() {
        for (x in 0..99) {
            for (i in 0..99) {
                for (j in 2..39) {
                    testRandom(i, j)
                }
            }
        }
    }

    @Test
    fun testGen2() {
        initWithSize(5)
        add(5)
        delete(3)
        delete(1)
        check()
    }

    @Test
    fun testGen3() {
        initWithSize(5)
        add(0)
        delete(1)
        delete(3)
        check()
    }

    @Test
    fun testGen4() {
        initWithSize(5)
        add(5)
        add(1)
        add(4)
        add(4)
        check()
    }

    @Test
    fun testGen5() {
        initWithSize(5)
        delete(0)
        delete(2)
        add(0)
        add(2)
        check()
    }

    @Test
    fun testGen6() {
        initWithSize(2)
        delete(0)
        delete(0)
        check()
    }

    @Test
    fun testGen7() {
        initWithSize(3)
        move(2, 0)
        delete(2)
        add(2)
        check()
    }

    @Test
    fun testGen8() {
        initWithSize(3)
        delete(1)
        add(0)
        move(2, 0)
        check()
    }

    @Test
    fun testGen9() {
        initWithSize(2)
        add(2)
        move(0, 2)
        check()
    }

    @Test
    fun testGen10() {
        initWithSize(3)
        move(0, 1)
        move(1, 2)
        add(0)
        check()
    }

    @Test
    fun testGen11() {
        initWithSize(4)
        move(2, 0)
        move(2, 3)
        check()
    }

    @Test
    fun testGen12() {
        initWithSize(4)
        move(3, 0)
        move(2, 1)
        check()
    }

    @Test
    fun testGen13() {
        initWithSize(4)
        move(3, 2)
        move(0, 3)
        check()
    }

    @Test
    fun testGen14() {
        initWithSize(4)
        move(3, 2)
        add(4)
        move(0, 4)
        check()
    }

    @Test
    fun testGen15() {
        initWithSize(1)
        update(0)
        update(0)
        update(0)
        check()
    }

    @Test
    fun testGen16() {
        initWithSize(1)
        update(0)
        move(0, 0)
        move(0, 0)
        add(0)
        check()
    }

    @Test
    fun testGen17() {
        initWithSize(2)
        move(1, 0)
        add(2)
        update(1)
        add(0)
        check()
    }

    @Test
    fun testGen18() {
        initWithSize(2)
        updateWithPayload(0)
        check()
    }

    @Test
    fun testGen19() {
        initWithSize(3)
        move(1, 1)
        delete(2)
        move(0, 1)
        add(0)
        update(1)
        add(1)
        updateWithPayload(2)
        add(1)
        delete(1)
        updateWithPayload(3)
        add(2)
        move(2, 1)
        add(2)
        delete(2)
        delete(1)
        check()
    }

    @Test
    fun testOneItem() {
        initWithSize(1)
        check()
    }

    @Test
    fun testEmpty() {
        initWithSize(0)
        check()
    }

    @Test
    fun testAdd1() {
        initWithSize(1)
        add(1)
        check()
    }

    @Test
    fun testMove1() {
        initWithSize(3)
        move(0, 2)
        check()
    }

    @Test
    fun tmp() {
        initWithSize(4)
        move(0, 2)
        check()
    }

    @Test
    fun testUpdate1() {
        initWithSize(3)
        update(2)
        check()
    }

    @Test
    fun testUpdate2() {
        initWithSize(2)
        add(1)
        update(1)
        update(2)
        check()
    }

    @Test
    fun testDisableMoveDetection() {
        initWithSize(5)
        move(0, 4)
        val applied = applyUpdates(
            before,
            DiffUtil.calculateDiff(callback, false)
        )
        assertThat(
            applied.size,
            `is`(5)
        )
        assertThat(
            applied[4].newItem,
            `is`(true)
        )
        assertThat(
            applied.contains(before[0]),
            `is`(false)
        )
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun convertOldPositionToNew_tooSmall() {
        initWithSize(2)
        update(2)
        calculate().convertOldPositionToNew(-1)
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun convertOldPositionToNew_tooLarge() {
        initWithSize(2)
        update(2)
        calculate().convertOldPositionToNew(2)
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun convertNewPositionToOld_tooSmall() {
        initWithSize(2)
        update(2)
        calculate().convertNewPositionToOld(-1)
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun convertNewPositionToOld_tooLarge() {
        initWithSize(2)
        update(2)
        calculate().convertNewPositionToOld(2)
    }

    private fun calculate() = DiffUtil.calculateDiff(callback, true)

    @Test
    fun duplicate() {
        before.addAll(listOf(Item(false), Item(false)))
        after.addAll(listOf(before[0], before[1], Item(true), before[1]))
        check()
    }

    private fun testRandom(initialSize: Int, operationCount: Int) {
        log.setLength(0)
        Item.idCounter = 0
        initWithSize(initialSize)
        for (i in 0 until operationCount) {
            val op = sRand.nextInt(6)
            when (op) {
                0 -> add(sRand.nextInt(after.size + 1))
                1 -> if (after.isNotEmpty()) {
                    delete(sRand.nextInt(after.size))
                }
                2 -> // move
                    if (after.size > 0) {
                        move(
                            sRand.nextInt(after.size),
                            sRand.nextInt(after.size)
                        )
                    }
                3 -> // update
                    if (after.size > 0) {
                        update(sRand.nextInt(after.size))
                    }
                4 -> // update with payload
                    if (after.size > 0) {
                        updateWithPayload(sRand.nextInt(after.size))
                    }
                5 -> // duplicate
                    if (after.size > 0) {
                        duplicate(
                            sRand.nextInt(after.size),
                            sRand.nextInt(after.size)
                        )
                    }
            }
        }
        check()
    }

    private fun check() {
        val result = calculate()
        log("before", before)
        log("after", after)
        // test diff dispatch
        val applied = applyUpdates(before, result)

        assertEquals(applied, after)
        // test position conversion
        val missingBeforePosition = mutableSetOf<Int>()
        val afterCopy = after.toMutableList()
        before.indices.forEach { oldPos ->
            val newPos = result.convertOldPositionToNew(oldPos)
            if (newPos != DiffUtil.DiffResult.NO_POSITION) {
                assertEquals(before[oldPos].id, after[newPos].id)
                // remove from the copy so that we can do not exists checks for unfound elements
                afterCopy.remove(after[newPos])
            } else {
                missingBeforePosition.add(oldPos)
            }
        }
        missingBeforePosition.forEach {
            assertFalse(afterCopy.contains(before[it]))
        }

        try {
            result.convertOldPositionToNew(before.size)
            Assert.fail("out of bounds should occur")
        } catch (e: IndexOutOfBoundsException) { // expected
        }

        val missingAfterPositions = mutableSetOf<Int>()
        val beforeCopy = before.toMutableList()
        after.indices.forEach { newPos ->
            val oldPos = result.convertNewPositionToOld(newPos)
            if (oldPos != DiffUtil.DiffResult.NO_POSITION) {
                assertEquals(after[newPos].id, before[oldPos].id)
                beforeCopy.remove(before[oldPos])
            } else {
                missingAfterPositions.add(newPos)
            }
        }
        missingAfterPositions.forEach {
            assertFalse(beforeCopy.contains(after[it]))
        }

        try {
            result.convertNewPositionToOld(after.size)
            Assert.fail("out of bounds should occur")
        } catch (e: IndexOutOfBoundsException) { // expected
        }
    }

    private fun initWithSize(size: Int) {
        before.clear()
        after.clear()
        repeat(size) {
            before.add(Item(false))
        }
        after.addAll(before)
        log.append("initWithSize($size);\n")
    }

    private fun log(title: String, items: List<*>) {
        log.append(title).append(":").append(items.size).append("\n")
        items.forEach { item ->
            log.append("  ").append(item).append("\n")
        }
    }

    private fun assertEquals(
        applied: List<Item>,
        after: List<Item>
    ) {
        log("applied", applied)
        val report = log.toString()
        val duplicateDiffs = computeExpectedNewItemsForExisting(after)

        // in theory we can get duplicateDiff[it.id] time "Add" event for existing items
        assertThat(
            report,
            applied.size,
            `is`(after.size)
        )
        after.indices.forEach { index ->
            val item = applied[index]
            if (after[index].newItem) {
                assertThat(
                    report,
                    item.newItem,
                    `is`(true)
                )
            } else if (duplicateDiffs.getOrElse(after[index].id) { 0 } > 0 && item.newItem) {
                // a duplicated item might come as a new item, be OK with it
                duplicateDiffs[after[index].id] = duplicateDiffs[after[index].id]!! - 1
            } else if (after[index].changed) {
                assertThat(
                    report,
                    item.newItem,
                    `is`(false)
                )
                assertThat(
                    report,
                    item.changed,
                    `is`(true)
                )
                assertThat(
                    report,
                    item.id,
                    `is`(after[index].id)
                )
                assertThat(
                    report,
                    item.payload,
                    `is`(after[index].payload)
                )
            } else {
                assertThat(
                    report,
                    item,
                    equalTo(
                        after[index]
                    )
                )
            }
        }
    }

    /**
     * When an item is duplicated more than once in the new list, some of those will
     * show up as new items, we should be OK with that but still verify
     *
     * @return mapping for <itemId -> max # of duplicates show up as new in the new list>
     */
    private fun computeExpectedNewItemsForExisting(after: List<Item>): MutableMap<Long, Int> {
        // we might create list w/ duplicates.
        val duplicateDiffs = mutableMapOf<Long, Int>() // id to count
        after.filterNot { it.newItem }.forEach {
            duplicateDiffs[it.id] = 1 + duplicateDiffs.getOrElse(it.id) { 1 }
        }
        before.forEach {
            duplicateDiffs[it.id] = -1 + duplicateDiffs.getOrElse(it.id) { 0 }
        }
        return duplicateDiffs
    }

    private fun applyUpdates(
        before: List<Item>,
        result: DiffUtil.DiffResult
    ): List<Item> {
        val target = mutableListOf<Item>()
        target.addAll(before)
        result.dispatchUpdatesTo(object : ListUpdateCallback {
            override fun onInserted(position: Int, count: Int) {
                repeat(count) {
                    target.add(it + position, Item(true))
                }
            }

            override fun onRemoved(position: Int, count: Int) {
                repeat(count) {
                    target.removeAt(position)
                }
            }

            override fun onMoved(
                fromPosition: Int,
                toPosition: Int
            ) {
                val item = target.removeAt(fromPosition)
                target.add(toPosition, item)
            }

            override fun onChanged(
                position: Int,
                count: Int,
                payload: Any?
            ) {
                repeat(count) { offset ->
                    val positionInList = position + offset
                    val existing = target[positionInList]
                    // make sure we don't update same item twice in callbacks
                    assertThat(
                        existing.changed,
                        `is`(false)
                    )
                    assertThat(
                        existing.newItem,
                        `is`(false)
                    )
                    assertThat(
                        existing.payload,
                        `is`(nullValue())
                    )
                    val replica = existing.copy(
                        changed = true,
                        payload = payload as? String
                    )
                    target.removeAt(positionInList)
                    target.add(positionInList, replica)
                }
            }
        })
        return target
    }

    private fun add(index: Int) {
        after.add(index, Item(true))
        log.append("add(").append(index).append(");\n")
    }

    private fun delete(index: Int) {
        after.removeAt(index)
        log.append("delete(").append(index).append(");\n")
    }

    private fun update(index: Int) {
        val existing = after[index]
        if (existing.newItem) {
            return // new item cannot be changed
        }
        // clean the payload since this might be after an updateWithPayload call
        val replica = existing.copy(
            changed = true,
            payload = null,
            data = UUID.randomUUID().toString()
        )
        after[index] = replica
        log.append("update(").append(index).append(");\n")
    }

    private fun updateWithPayload(index: Int) {
        val existing = after[index]
        if (existing.newItem) {
            return // new item cannot be changed
        }
        val replica = existing.copy(
            changed = true,
            data = UUID.randomUUID().toString(),
            payload = UUID.randomUUID().toString()
        )
        after[index] = replica
        log.append("updateWithPayload(").append(index).append(");\n")
    }

    private fun move(from: Int, to: Int) {
        val removed = after.removeAt(from)
        after.add(to, removed)
        log.append("move(").append(from).append(",").append(to).append(");\n")
    }

    private fun duplicate(pos: Int, to: Int) {
        val item = after[pos]
        after.add(pos, item) // re-use the item so that changes happen on it
        log.append("duplicate(").append(pos).append(",").append(to).append(");\n")
    }

    internal data class Item(
        val id: Long,
        val newItem: Boolean,
        var changed: Boolean = false,
        var payload: String? = null,
        var data: String = UUID.randomUUID().toString()
    ) {
        constructor(newItem: Boolean) : this(id = idCounter++, newItem = newItem)

        companion object {
            var idCounter: Long = 0
        }
    }

    private class ItemListCallback(
        private val oldList: List<Item>,
        private val newList: List<Item>,
        private val assertCalls: Boolean = true
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(
            oldItemIndex: Int,
            newItemIndex: Int
        ): Boolean {
            return oldList[oldItemIndex].id == newList[newItemIndex].id
        }

        override fun areContentsTheSame(
            oldItemIndex: Int,
            newItemIndex: Int
        ): Boolean {
            if (assertCalls) {
                assertThat(
                    oldList[oldItemIndex].id,
                    equalTo(newList[newItemIndex].id)
                )
            }
            return oldList[oldItemIndex].data == newList[newItemIndex].data
        }

        override fun getChangePayload(
            oldItemIndex: Int,
            newItemIndex: Int
        ): Any? {
            if (assertCalls) {
                assertThat(
                    oldList[oldItemIndex].id,
                    equalTo(newList[newItemIndex].id)
                )
                assertThat(
                    oldList[oldItemIndex].data,
                    not(
                        equalTo(
                            newList[newItemIndex].data
                        )
                    )
                )
            }

            return newList[newItemIndex].payload
        }
    }

    companion object {
        private val sRand = Random(System.nanoTime())
    }
}
