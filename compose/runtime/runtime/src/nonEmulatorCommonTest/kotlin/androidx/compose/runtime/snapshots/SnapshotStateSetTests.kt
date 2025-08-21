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

package androidx.compose.runtime.snapshots

import androidx.compose.runtime.mutableStateSetOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.test.IgnoreJsTarget

class SnapshotStateSetTests {
    @Test
    fun canCreateAStateSet() {
        mutableStateSetOf<Any>()
    }

    @Test
    fun canCreateAStateSetOfInts() {
        val set = mutableStateSetOf(0, 1, 2, 3, 4)
        set.forEachIndexed { index, item -> assertEquals(index, item) }
    }

    @Test
    fun validateSize() {
        val set = mutableStateSetOf(0, 1, 2, 3)
        assertEquals(4, set.size)
    }

    @Test
    fun validateContains() {
        val set = mutableStateSetOf(0, 1, 2, 3, 4)
        (0..4).forEach { assertTrue(set.contains(it)) }
        assertFalse(set.contains(5))
    }

    @Test
    fun validateContainsAll() {
        val set = mutableStateSetOf(0, 1, 2, 3, 4)
        assertTrue(set.containsAll(setOf(1, 2, 3)))
        assertFalse(set.containsAll(setOf(0, 2, 3, 5)))
    }

    @Test
    fun validateIsEmpty() {
        val set = mutableStateSetOf(0, 1, 2)
        assertFalse(set.isEmpty())
        val emptySet = mutableStateSetOf<Any>()
        assertTrue(emptySet.isEmpty())
    }

    @Test
    fun validateIterator() {
        val set = mutableStateSetOf(0, 1, 2, 3, 4)
        var expected = 0
        for (item in set) {
            assertEquals(expected++, item)
        }
        assertEquals(5, expected)
    }

    @Test
    fun validateIterator_ordered() {
        val set = mutableStateSetOf(0, 1, 2, 3, 4)
        assertEquals(listOf(0, 1, 2, 3, 4), set.iterator().asSequence().toList())
    }

    @Test
    fun validateIterator_remove() {
        assertFailsWith(IllegalStateException::class) {
            validate(mutableStateSetOf(0, 1, 2, 3, 4)) { normalSet ->
                val iterator = normalSet.iterator()
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
    fun validateIterator_orderedAfterRemove() {
        val set = mutableStateSetOf(0, 1, 2, 3, 4)
        set.remove(2)
        assertEquals(listOf(0, 1, 3, 4), set.iterator().asSequence().toList())
    }

    @Test
    fun canRemoveFromAStateSet() {
        val set = mutableStateSetOf(0, 1, 2)
        set.remove(1)
        assertEquals(2, set.size)
        assertTrue(set.contains(0))
        assertTrue(set.contains(2))
    }

    @Test
    fun canRemoveAllFromAStateSet() {
        val set = mutableStateSetOf(0, 1, 2, 3, 4, 5)
        val normalSet = mutableSetOf(0, 1, 2, 3, 4, 5)
        set.removeAll(setOf(2, 4))
        normalSet.removeAll(setOf(2, 4))
        expected(normalSet, set)
    }

    @Test
    fun canRetainAllOfAStateSet() {
        val set = mutableStateSetOf(0, 1, 2, 3, 4, 5, 6)
        val normalSet = mutableSetOf(0, 1, 2, 3, 4, 5, 6)
        set.retainAll(setOf(2, 4, 6, 8))
        normalSet.retainAll(setOf(2, 4, 6, 8))
        expected(normalSet, set)
    }

    @Test
    fun stateSetsCanBeSnapshot() {
        val original = setOf(0, 1, 2, 3, 4, 5, 6)
        val mutableSet = original.toMutableSet()
        val set = mutableStateSetOf(0, 1, 2, 3, 4, 5, 6)
        val snapshot = Snapshot.takeSnapshot()
        try {
            set.remove(0)
            mutableSet.remove(0)
            expected(mutableSet, set)
            snapshot.enter { expected(original, set) }
        } finally {
            snapshot.dispose()
        }
        expected(mutableSet, set)
    }

    @Test
    @ExperimentalCoroutinesApi
    fun concurrentGlobalModification_add() = runTest {
        repeat(100) {
            val set = mutableStateSetOf<Int>()
            coroutineScope {
                repeat(100) { index -> launch(Dispatchers.Default) { set.add(index) } }
            }

            repeat(100) { assertTrue(set.contains(it)) }
        }
    }

    @Test
    @ExperimentalCoroutinesApi
    fun concurrentGlobalModification_remove() = runTest {
        repeat(100) {
            val set = mutableStateSetOf<Int>()
            repeat(100) { index -> set.add(index) }

            coroutineScope {
                repeat(100) { index -> launch(Dispatchers.Default) { set.remove(index) } }
            }

            repeat(100) { assertFalse(set.contains(it)) }
        }
    }

    @Test
    @IgnoreJsTarget // Not relevant in a single threaded environment
    fun concurrentMixingWriteApply_add() =
        runTest(timeout = 30.seconds) {
            repeat(10) {
                val sets = Array(100) { mutableStateSetOf<Int>() }.toList()
                val channel = Channel<Unit>(Channel.CONFLATED)
                coroutineScope {
                    // Launch mutator
                    launch(Dispatchers.Default) {
                        repeat(100) { index ->
                            sets.fastForEach { set -> set.add(index) }

                            // Simulate the write observer
                            channel.trySend(Unit)
                        }
                        channel.close()
                    }

                    // Simulate the global snapshot manager
                    launch(Dispatchers.Default) {
                        channel.consumeEach { Snapshot.notifyObjectsInitialized() }
                    }
                }
            }
            // Should only get here if the above doesn't deadlock.
        }

    @Test
    fun testWritingANewValueDoesObserveChange() {
        val state = mutableStateSetOf(0, 1, 2)
        val modified = observeGlobalChanges { repeat(4) { state.add(it) } }
        assertTrue(modified.size == 1)
        assertEquals(modified.first(), state)
    }

    @Test
    fun testWritingTheSameValueDoesNotChangeTheSet() {
        val state = mutableStateSetOf(0, 1, 2, 3)
        val modified = observeGlobalChanges { repeat(4) { state.add(it) } }
        assertTrue(modified.isEmpty())
    }

    @Test
    fun toStringOfSnapshotStateSetDoesNotTriggerReadObserver() {
        val state = mutableStateSetOf(0)
        val normalReads = readsOf { state.readable }
        assertEquals(1, normalReads)
        val toStringReads = readsOf { state.toString() }
        assertEquals(0, toStringReads)
    }

    @Test
    fun testValueOfStateSetToString() {
        val state = mutableStateSetOf(0, 1, 2)
        assertEquals("SnapshotStateSet(value=[0, 1, 2])@${state.hashCode()}", state.toString())
    }

    private fun <T> validate(set: MutableSet<T>, block: (set: MutableSet<T>) -> Unit) {
        val normalSet = set.toMutableSet()
        block(normalSet)
        block(set)
        expected(normalSet, set)
    }

    private fun <T> expected(expected: Set<T>, actual: Set<T>) {
        assertEquals(expected.size, actual.size)
        assertEquals(expected.subtract(actual), emptySet())
        // Set is ordered, so should be equivalent when converted to list
        assertEquals(expected.toList(), actual.toList())
    }

    private fun observeGlobalChanges(block: () -> Unit): Set<Any> {
        val result = mutableSetOf<Any>()
        val handle = Snapshot.registerApplyObserver { set, _ -> result.addAll(set) }
        try {
            block()
        } finally {
            Snapshot.sendApplyNotifications()
            handle.dispose()
        }
        return result
    }
}
