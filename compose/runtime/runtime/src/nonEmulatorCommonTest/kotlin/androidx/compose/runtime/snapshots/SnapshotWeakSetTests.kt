/*
 * Copyright 2022 The Android Open Source Project
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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SnapshotWeakSetTests {
    @Test
    fun canCreateWeakSet() {
        val set = SnapshotWeakSet<Any>()
        assertValidSet(set)
    }

    @Test
    fun canAddItem() {
        val item = Any()
        val set = SnapshotWeakSet<Any>()
        val added = set.add(item)
        assertTrue(added)
        // Asserting size 1 as the size will be 1 even if `item` is collected. The size does not
        // reduce until `removeIf` is called.
        assertEquals(1, set.size)
        val addedAgain = set.add(item)
        assertFalse(addedAgain)
        assertValidSet(set)
    }

    @Test
    fun canAdd100Items() {
        val items = Array(100) { Any() }
        val set = SnapshotWeakSet<Any>()
        items.forEach {
            val added = set.add(it)
            assertTrue(added)
            assertValidSet(set)
        }
        // We can assert the sizes are the same even if any of the items have been collected.
        assertEquals(items.size, set.size)
        items.forEach {
            val added = set.add(it)
            assertFalse(added)
            assertValidSet(set)
        }
    }

    @Test
    fun canRemoveSomeItems() {
        val items = Array(100) { Any() }
        val toRemove = items.filterIndexed { index, _ -> index % 2 == 0 }.toSet()
        val set = SnapshotWeakSet<Any>()
        items.forEach { set.add(it) }
        set.removeIf { it in toRemove }
        // The size might be lower than the number we removed if any of the items added have been
        // collected.
        assertTrue(items.size - toRemove.size >= set.size)
        assertValidSet(set)
    }

    @Test
    fun canRemoveAllItems() {
        val items = Array(100) { Any() }
        val set = SnapshotWeakSet<Any>()
        items.forEach { set.add(it) }
        set.removeIf { true }
        assertEquals(0, set.size)
        assertValidSet(set)
    }

    private fun <T : Any> assertValidSet(set: SnapshotWeakSet<T>) = assertTrue(set.isValid())
}