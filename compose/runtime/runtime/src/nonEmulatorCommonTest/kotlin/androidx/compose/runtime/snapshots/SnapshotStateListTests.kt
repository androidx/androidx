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

package androidx.compose.runtime.snapshots

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.toMutableStateList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.test.IgnoreJsTarget

class SnapshotStateListTests {
    @Test
    fun canCreateAStateList() {
        mutableStateListOf<Any>()
    }

    @Test
    fun canCreateAStateListOfInts() {
        val list = mutableStateListOf(0, 1, 2, 3, 4)
        list.forEachIndexed { index, item ->
            assertEquals(index, item)
        }
    }

    @Test
    fun validateSize() {
        val list = mutableStateListOf(0, 1, 2, 3)
        assertEquals(4, list.size)
    }

    @Test
    fun validateContains() {
        val list = mutableStateListOf(0, 1, 2, 3, 4)
        (0..4).forEach {
            assertTrue(list.contains(it))
        }
        assertFalse(list.contains(5))
    }

    @Test
    fun validateContainsAll() {
        val list = mutableStateListOf(0, 1, 2, 3, 4)
        assertTrue(list.containsAll(listOf(1, 2, 3)))
        assertFalse(list.containsAll(listOf(0, 2, 3, 5)))
    }

    @Test
    fun validateGet() {
        val list = mutableStateListOf(0, 1, 2, 3)
        (0..3).forEach { assertEquals(it, list[it]) }
    }

    @Test
    fun validateGet_IndexOutOfBound() {
        assertFailsWith(IndexOutOfBoundsException::class) {
            val list = mutableStateListOf(0, 1, 2, 3)
            list[4]
        }
    }

    @Test
    fun validateIsEmpty() {
        val list = mutableStateListOf(0, 1, 2)
        assertFalse(list.isEmpty())
        val emptyList = mutableStateListOf<Any>()
        assertTrue(emptyList.isEmpty())
    }

    @Test
    fun validateIterator() {
        val list = mutableStateListOf(0, 1, 2, 3, 4)
        var expected = 0
        for (item in list) {
            assertEquals(expected++, item)
        }
        assertEquals(5, expected)
    }

    @Test
    fun validateIterator_remove() {
        assertFailsWith(IllegalStateException::class) {
            validate(mutableStateListOf(0, 1, 2, 3, 4)) { normalList ->
                val iterator = normalList.iterator()
                iterator.next()
                iterator.next()
                iterator.remove()
                iterator.remove()
                iterator.next()
                iterator.remove()
            }
        }
    }

    @Test
    fun validateLastIndexOf() {
        val list = mutableStateListOf(0, 1, 2, 3, 4, 0, 1, 2, 3, 4)
        (0..4).forEach {
            assertEquals(it + 5, list.lastIndexOf(it))
        }
    }

    @Test
    fun validateListIterator() {
        val list = mutableStateListOf(0, 1, 2, 3, 4)
        val iterator = list.listIterator()
        repeat(list.size) {
            assertTrue(iterator.hasNext())
            assertEquals(it, list[iterator.nextIndex()])
            assertEquals(it, iterator.next())
        }
        assertFalse(iterator.hasNext())
        repeat(list.size) {
            assertTrue(iterator.hasPrevious())
            assertEquals(list.size - it - 1, list[iterator.previousIndex()])
            assertEquals(list.size - it - 1, iterator.previous())
        }
        assertFalse(iterator.hasPrevious())
    }

    @Test
    fun validateListIterator_add() {
        validate(mutableStateListOf(0, 1, 2, 3, 4, 5, 6)) { list ->
            val iterator = list.listIterator()
            iterator.next()
            iterator.next()
            iterator.add(100)
            iterator.add(101)
            iterator.next()
            iterator.add(102)
            iterator.previous()
            iterator.previous()
            iterator.add(103)
        }
    }

    @Test
    fun validateListIterator_mutationError_set() {
        validate(mutableStateListOf(0, 1, 2, 3, 4)) { list ->
            val iterator = list.listIterator()
            iterator.next()
            iterator.set(100)
            iterator.next()
            iterator.next()
            iterator.set(101)
        }
    }

    @Test
    fun validateListIterator_index() {
        val list = mutableStateListOf(0, 1, 2, 3, 4, 5)
        val iterator = list.listIterator(3)
        repeat(3) {
            assertTrue(iterator.hasNext())
            assertEquals(it + 3, iterator.next())
        }
        assertFalse(iterator.hasNext())
    }

    @Test
    fun validate_subList() {
        val list = mutableStateListOf(0, 1, 2, 3, 4, 5, 6)
        val normalList = listOf(0, 1, 2, 3, 4, 5, 6)
        val subList = list.subList(2, 5)
        val normalSubList = normalList.subList(2, 5)
        expected(normalSubList, subList)
    }

    @Test
    fun validate_subList_size() {
        val list = mutableStateListOf(0, 1, 2, 3, 4, 5, 6)
        val subList = list.subList(0, list.size)
        assertEquals(list.size, subList.size)
    }

    @Test
    fun validate_subList_contains() {
        val list = mutableStateListOf(0, 1, 2, 3, 4, 5, 6)
        val subList = list.subList(2, 5)
        assertFalse(subList.contains(0))
        assertFalse(subList.contains(1))
        assertTrue(subList.contains(2))
        assertTrue(subList.contains(3))
        assertTrue(subList.contains(4))
        assertFalse(subList.contains(5))
        assertFalse(subList.contains(6))
    }

    @Test
    fun validate_subList_containsAll() {
        val list = mutableStateListOf(0, 1, 2, 3, 4, 5, 6)
        val subList = list.subList(2, 5)
        assertTrue(subList.containsAll(listOf(2, 3, 4)))
        assertFalse(subList.containsAll(listOf(2, 3, 4, 5)))
    }

    @Test
    fun validate_subList_get() {
        val list = mutableStateListOf(0, 1, 2, 3, 4, 5, 6)
        val subList = list.subList(2, 5)
        repeat(3) { assertEquals(it + 2, subList[it]) }
    }

    @Test
    fun validate_subList_get_outOfRange() {
        assertFailsWith(IndexOutOfBoundsException::class) {
            val list = mutableStateListOf(0, 1, 2, 3, 4, 5, 6)
            val subList = list.subList(2, 5)
            subList[3]
        }
    }

    @Test
    fun validate_subList_indexOf() {
        val list = mutableStateListOf(0, 1, 2, 3, 4, 5, 6)
        val subList = list.subList(2, 5)
        repeat(subList.size) {
            assertEquals(it, subList.indexOf(it + 2))
        }
        assertEquals(-1, subList.indexOf(0))
        assertEquals(-1, subList.indexOf(5))
    }

    @Test
    fun validate_subList_add() {
        val list = mutableStateListOf(0, 1, 2, 3, 4, 5, 6)
        val normalList = mutableListOf(0, 1, 2, 3, 4, 5, 6)

        val subList = list.subList(2, 5)
        subList.add(100)
        val normalSubList = normalList.subList(2, 5)
        normalSubList.add(100)

        expected(normalSubList, subList)
        expected(normalList, list)
    }

    @Test
    fun validate_subList_add_index() {
        val list = mutableStateListOf(0, 1, 2, 3, 4, 5, 6)
        val normalList = mutableListOf(0, 1, 2, 3, 4, 5, 6)

        val subList = list.subList(2, 5)
        subList.add(1, 100)
        val normalSubList = normalList.subList(2, 5)
        normalSubList.add(1, 100)

        expected(normalSubList, subList)
        expected(normalList, list)
    }

    @Test
    fun validate_subList_addAll() {
        val list = mutableStateListOf(0, 1, 2, 3, 4, 5, 6)
        val normalList = mutableListOf(0, 1, 2, 3, 4, 5, 6)

        val subList = list.subList(2, 5)
        subList.addAll(listOf(100, 101, 102))
        val normalSubList = normalList.subList(2, 5)
        normalSubList.addAll(listOf(100, 101, 102))

        expected(normalSubList, subList)
        expected(normalList, list)
    }

    @Test
    fun validate_subList_addAll_index() {
        val list = mutableStateListOf(0, 1, 2, 3, 4, 5, 6)
        val normalList = mutableListOf(0, 1, 2, 3, 4, 5, 6)

        val subList = list.subList(2, 5)
        subList.addAll(1, listOf(100, 101, 102))
        val normalSubList = normalList.subList(2, 5)
        normalSubList.addAll(1, listOf(100, 101, 102))

        expected(normalSubList, subList)
        expected(normalList, list)
    }

    @Test
    fun validate_subList_clear() {
        val list = mutableStateListOf(0, 1, 2, 3, 4, 5, 6)
        val normalList = mutableListOf(0, 1, 2, 3, 4, 5, 6)

        val subList = list.subList(2, 5)
        subList.clear()
        val normalSubList = normalList.subList(2, 5)
        normalSubList.clear()

        expected(normalSubList, subList)
        expected(normalList, list)
    }

    @Test
    fun validate_subList_listIterator() {
        val list = mutableStateListOf(0, 1, 2, 3, 4)
        val subList = list.subList(1, 4)
        val iterator = subList.listIterator()
        repeat(subList.size) {
            assertTrue(iterator.hasNext())
            assertEquals(it + 1, subList[iterator.nextIndex()])
            assertEquals(it + 1, iterator.next())
        }
        assertFalse(iterator.hasNext())
        repeat(subList.size) {
            assertTrue(iterator.hasPrevious())
            val expectedIndex = subList.size - it - 1
            assertEquals(expectedIndex + 1, subList[iterator.previousIndex()])
            assertEquals(expectedIndex + 1, iterator.previous())
        }
        assertFalse(iterator.hasPrevious())
    }

    @Test
    fun validate_subList_listIterator_add() {
        assertFailsWith(IllegalStateException::class) {
            val list = mutableStateListOf(0, 1, 2, 3, 4)
            val subList = list.subList(1, 4)
            val iterator = subList.listIterator()
            iterator.next()
            iterator.add(1)
        }
    }

    @Test
    fun validate_subList_listIterator_remove() {
        assertFailsWith(IllegalStateException::class) {
            val list = mutableStateListOf(0, 1, 2, 3, 4)
            val subList = list.subList(1, 4)
            val iterator = subList.listIterator()
            iterator.next()
            iterator.remove()
        }
    }

    @Test
    fun validate_subList_listIterator_set() {
        assertFailsWith(IllegalStateException::class) {
            val list = mutableStateListOf(0, 1, 2, 3, 4)
            val subList = list.subList(1, 4)
            val iterator = subList.listIterator()
            iterator.next()
            iterator.set(1)
        }
    }

    @Test
    fun validate_subList_remove() {
        val list = mutableStateListOf(0, 1, 2, 3, 4, 5, 6)
        val normalList = mutableListOf(0, 1, 2, 3, 4, 5, 6)

        val subList = list.subList(2, 5)
        subList.remove(3)
        val normalSubList = normalList.subList(2, 5)
        normalSubList.remove(3)

        expected(normalSubList, subList)
        expected(normalList, list)
    }

    @Test
    fun validate_subList_removeAll() {
        val list = mutableStateListOf(0, 1, 2, 3, 4, 5, 6)
        val normalList = mutableListOf(0, 1, 2, 3, 4, 5, 6)

        val subList = list.subList(2, 5)
        subList.removeAll(listOf(2, 3))
        val normalSubList = normalList.subList(2, 5)
        normalSubList.removeAll(listOf(2, 3))

        expected(normalSubList, subList)
        expected(normalList, list)
    }

    @Test
    fun validate_subList_removeAt() {
        val list = mutableStateListOf(0, 1, 2, 3, 4, 5, 6)
        val normalList = mutableListOf(0, 1, 2, 3, 4, 5, 6)

        val subList = list.subList(2, 5)
        subList.removeAt(1)
        val normalSubList = normalList.subList(2, 5)
        normalSubList.removeAt(1)

        expected(normalSubList, subList)
        expected(normalList, list)
    }

    @Test
    fun validate_subList_retainAll() {
        val list = mutableStateListOf(0, 1, 2, 3, 4, 5, 6)
        val normalList = mutableListOf(0, 1, 2, 3, 4, 5, 6)

        val subList = list.subList(1, 6)
        subList.retainAll(listOf(2, 4, 6, 8))
        val normalSubList = normalList.subList(1, 6)
        normalSubList.retainAll(listOf(2, 4, 6, 8))

        expected(normalSubList, subList)
        expected(normalList, list)
    }

    @Test
    fun validate_subList_set() {
        val list = mutableStateListOf(0, 1, 2, 3, 4, 5, 6)
        val normalList = mutableListOf(0, 1, 2, 3, 4, 5, 6)

        val subList = list.subList(1, 6)
        subList[1] = 100
        val normalSubList = normalList.subList(1, 6)
        normalSubList[1] = 100

        expected(normalSubList, subList)
        expected(normalList, list)
    }

    @Test
    fun validate_subList_subList() {
        val list = mutableStateListOf(0, 1, 2, 3, 4, 5, 6)
        val normalList = mutableListOf(0, 1, 2, 3, 4, 5, 6)

        val subList = list.subList(1, 6)
        val subListSubList = subList.subList(1, 3)
        val normalSubList = normalList.subList(1, 6)
        val normalSubListSubList = normalSubList.subList(1, 3)

        expected(normalSubListSubList, subListSubList)
        expected(normalSubList, subList)
        expected(normalList, list)
    }

    @Test
    fun validate_subList_concurrentChanges() {
        assertFailsWith(ConcurrentModificationException::class) {
            val list = mutableStateListOf(0, 1, 2, 3, 4, 5, 6)
            val subList1 = list.subList(1, 6)
            val subList2 = list.subList(1, 6)
            subList1.remove(3)
            subList2.remove(4)
        }
    }

    @Test
    fun validate_indexOf() {
        val list = mutableStateListOf(0, 1, 2, 3, 4, 5, 6, 7)
        (0..7).forEach { assertEquals(it, list.indexOf(it)) }
        assertEquals(-1, list.indexOf(10))
    }

    @Test
    fun canModifyAStateList_add() {
        val list = mutableStateListOf(0, 1, 2)
        list.add(3)
        assertEquals(4, list.size)
        list.forEachIndexed { index, item ->
            assertEquals(index, item)
        }
    }

    @Test
    fun canModifyAStateList_addIndex() {
        val list = mutableStateListOf(0, 1, 2)
        list.add(0, 3)
        assertEquals(4, list.size)
        assertEquals(3, list[0])
    }

    @Test
    fun canRemoveFromAStateList() {
        val list = mutableStateListOf(0, 1, 2)
        list.remove(1)
        assertEquals(2, list.size)
        assertEquals(0, list[0])
        assertEquals(2, list[1])
    }

    @Test
    fun canRemoveAllFromAStateList() {
        val list = mutableStateListOf(0, 1, 2, 3, 4, 5)
        val normalList = mutableListOf(0, 1, 2, 3, 4, 5)
        list.removeAll(listOf(2, 4))
        normalList.removeAll(listOf(2, 4))
        expected(normalList, list)
    }

    @Test
    fun canRemoveAtFromAStateList() {
        val list = mutableStateListOf(0, 1, 2, 3, 4, 5)
        val normalList = mutableListOf(0, 1, 2, 3, 4, 5)
        list.removeAt(2)
        normalList.removeAt(2)
        expected(normalList, list)
    }

    @Test
    fun canRetainAllOfAStateList() {
        val list = mutableStateListOf(0, 1, 2, 3, 4, 5, 6)
        val normalList = mutableListOf(0, 1, 2, 3, 4, 5, 6)
        list.retainAll(listOf(2, 4, 6, 8))
        normalList.retainAll(listOf(2, 4, 6, 8))
        expected(normalList, list)
    }

    @Test
    fun canSetAnElementOfAStateList() {
        val list = mutableStateListOf(0, 1, 2, 3, 4, 5, 6)
        val normalList = mutableListOf(0, 1, 2, 3, 4, 5, 6)
        list[2] = 100
        normalList[2] = 100
        expected(normalList, list)
    }

    @Test
    fun stateListsCanBeSnapshot() {
        val original = listOf(0, 1, 2, 3, 4, 5, 6)
        val mutableList = original.toMutableList()
        val list = mutableStateListOf(0, 1, 2, 3, 4, 5, 6)
        val snapshot = Snapshot.takeSnapshot()
        try {
            list[1] = 100
            mutableList[1] = 100
            expected(mutableList, list)
            snapshot.enter {
                expected(original, list)
            }
        } finally {
            snapshot.dispose()
        }
        expected(mutableList, list)
    }

    @Test
    @ExperimentalCoroutinesApi
    fun concurrentGlobalModification_add() = runTest {
        repeat(100) {
            val list = mutableStateListOf<Int>()
            coroutineScope {
                repeat(100) { index ->
                    launch(Dispatchers.Default) {
                        list.add(index)
                    }
                }
            }

            repeat(100) {
                assertTrue(list.contains(it))
            }
        }
    }

    @Test(timeout = 10_000)
    @OptIn(ExperimentalCoroutinesApi::class)
    @IgnoreJsTarget // Not relevant in a single threaded environment
    fun concurrentGlobalModifications_addAll(): Unit = runTest {
        repeat(100) {
            val list = mutableStateListOf<Int>()
            coroutineScope {
                repeat(100) { index ->
                    launch(Dispatchers.Default) {
                        list.addAll(0, Array(10) { index * 100 + it }.toList())
                    }
                }
            }

            repeat(100) { index ->
                repeat(10) {
                    assertTrue(list.contains(index * 100 + it))
                }
            }
        }
    }

    @Test(timeout = 30_000)
    @IgnoreJsTarget // Not relevant in a single threaded environment
    fun concurrentMixingWriteApply_add(): Unit = runTest {
        repeat(10) {
            val lists = Array(100) { mutableStateListOf<Int>() }.toList()
            val channel = Channel<Unit>(Channel.CONFLATED)
            coroutineScope {
                // Launch mutator
                launch(Dispatchers.Default) {
                    repeat(100) { index ->
                        lists.fastForEach { list ->
                            list.add(index)
                        }

                        // Simulate the write observer
                        channel.trySend(Unit)
                    }
                    channel.close()
                }

                // Simulate the global snapshot manager
                launch(Dispatchers.Default) {
                    channel.consumeEach {
                        Snapshot.notifyObjectsInitialized()
                    }
                }
            }
        }
        // Should only get here if the above doesn't deadlock.
    }

    @Test(timeout = 30_000)
    @OptIn(ExperimentalCoroutinesApi::class)
    @IgnoreJsTarget // Not relevant in a single threaded environment
    fun concurrentMixingWriteApply_addAll_clear(): Unit = runTest {
        repeat(10) {
            val lists = Array(100) { mutableStateListOf<Int>() }.toList()
            val data = Array(100) { index -> index }.toList()
            val channel = Channel<Unit>(Channel.CONFLATED)
            coroutineScope {
                // Launch mutator
                launch(Dispatchers.Default) {
                    repeat(100) {
                        lists.fastForEach { list ->
                            list.addAll(data)
                            list.clear()
                        }
                        // Simulate the write observer
                        channel.trySend(Unit)
                    }
                    channel.close()
                }

                // Simulate the global snapshot manager
                launch(Dispatchers.Default) {
                    channel.consumeEach {
                        Snapshot.notifyObjectsInitialized()
                    }
                }
            }
        }
        // Should only get here if the above doesn't deadlock.
    }

    @Test(timeout = 30_000)
    @OptIn(ExperimentalCoroutinesApi::class)
    @IgnoreJsTarget // Not relevant in a single threaded environment
    fun concurrentMixingWriteApply_addAll_removeRange(): Unit = runTest {
        repeat(4) {
            val lists = Array(100) { mutableStateListOf<Int>() }.toList()
            val data = Array(100) { index -> index }.toList()
            val channel = Channel<Unit>(Channel.CONFLATED)
            coroutineScope {
                // Launch mutator
                launch(Dispatchers.Default) {
                    repeat(100) {
                        lists.fastForEach { list ->
                            list.addAll(data)
                            list.removeRange(0, data.size)
                        }
                        // Simulate the write observer
                        channel.trySend(Unit)
                    }
                    channel.close()
                }

                // Simulate the global snapshot manager
                launch(Dispatchers.Default) {
                    channel.consumeEach {
                        Snapshot.notifyObjectsInitialized()
                    }
                }
            }
        }
        // Should only get here if the above doesn't deadlock.
    }

    @Test
    fun modificationAcrossSnapshots() {
        val list = mutableStateListOf<Int>()
        repeat(100) {
            Snapshot.withMutableSnapshot {
                list.add(it)
            }
        }
        repeat(100) {
            assertEquals(it, list[it])
        }
    }

    @Test
    fun currentValueOfTheList() {
        val list = mutableStateListOf<Int>()
        val lists = mutableListOf<List<Int>>()
        repeat(100) {
            Snapshot.withMutableSnapshot {
                list.add(it)
                lists.add(list.toList())
            }
        }
        repeat(100) { index ->
            val current = lists[index]
            assertEquals(index + 1, current.size)
            current.forEachIndexed { i, value ->
                assertEquals(i, value)
            }
        }
    }

    @Test
    fun canReverseTheList() {
        validate(List(100) { it }.toMutableStateList()) { list ->
            list.reverse()
        }
    }

    @Test
    fun canReverseUsingIterators() {
        validate(List(100) { it }.toMutableStateList()) { list ->
            val forward = list.listIterator()
            val backward = list.listIterator(list.size)
            val count = list.size shr 1
            repeat(count) {
                val forwardValue = forward.next()
                val backwardValue = backward.previous()
                backward.set(forwardValue)
                forward.set(backwardValue)
            }
        }
        validate(List(101) { it }.toMutableStateList()) { list ->
            val forward = list.listIterator()
            val backward = list.listIterator(list.size)
            val count = list.size shr 1
            repeat(count) {
                val forwardValue = forward.next()
                val backwardValue = backward.previous()
                backward.set(forwardValue)
                forward.set(backwardValue)
            }
        }
    }

    @Test
    fun canIterateForwards() {
        validate(List(100) { it }.toMutableStateList()) { list ->
            val forward = list.listIterator()
            var expected = 0
            var count = 0
            while (forward.hasNext()) {
                count++
                assertEquals(expected++, forward.next())
            }
            assertEquals(100, count)
        }
    }

    @Test
    fun canIterateBackwards() {
        validate(List(100) { it }.toMutableStateList()) { list ->
            val backward = list.listIterator(list.size)
            var expected = 99
            var count = 0
            while (backward.hasPrevious()) {
                count++
                assertEquals(expected--, backward.previous())
            }
            assertEquals(100, count)
        }
    }

    @Test
    fun canShuffleTheList() {
        val list = List(100) { it }.toMutableStateList()
        list.shuffle()
        assertEquals(100, list.distinct().size)
    }

    @Test
    fun canSortTheList() {
        validate(List(100) { it }.toMutableStateList()) { list ->
            list.shuffle()
            list.sort()
        }
    }

    @Test
    fun toStringOfSnapshotStateListDoesNotTriggerReadObserver() {
        val state = mutableStateListOf<Int>(0)
        val normalReads = readsOf {
            state.readable
        }
        assertEquals(1, normalReads)
        val toStringReads = readsOf {
            state.toString()
        }
        assertEquals(0, toStringReads)
    }

    @Test
    fun testValueOfStateListToString() {
        val state = mutableStateListOf(0, 1, 2)
        assertEquals(
            "SnapshotStateList(value=[0, 1, 2])@${state.hashCode()}",
            state.toString()
        )
    }

    private fun <T> validate(list: MutableList<T>, block: (list: MutableList<T>) -> Unit) {
        val normalList = list.toMutableList()
        block(normalList)
        block(list)
        expected(normalList, list)
    }

    private fun <T> expected(expected: List<T>, actual: List<T>) {
        assertEquals(expected.size, actual.size)
        expected.indices.forEach {
            assertEquals(expected[it], actual[it])
        }
    }
}
