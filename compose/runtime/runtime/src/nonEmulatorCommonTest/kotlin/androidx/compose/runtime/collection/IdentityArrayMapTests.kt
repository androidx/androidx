/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.runtime.collection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class Key(val value: Int)

class IdentityArrayMapTests {
    private val keys = Array(100) { Key(it) }

    @Test
    fun canCreateEmptyMap() {
        val map = IdentityArrayMap<Key, Any>()
        assertTrue(map.isEmpty(), "map is not empty")
    }

    @Test
    fun canSetAndGetValues() {
        val map = IdentityArrayMap<Key, String>()
        map[keys[1]] = "One"
        map[keys[2]] = "Two"
        assertEquals("One", map[keys[1]], "map key 1")
        assertEquals("Two", map[keys[2]], "map key 2")
        assertEquals(null, map[keys[3]], "map key 3")
    }

    @Test
    fun canSetAndGetManyValues() {
        val map = IdentityArrayMap<Key, String>()
        repeat(keys.size) {
            map[keys[it]] = it.toString()
        }
        repeat(keys.size) {
            assertEquals(it.toString(), map[keys[it]], "map key $it")
        }
    }

    @Test
    fun canRemoveValues() {
        val map = IdentityArrayMap<Key, Int>()
        repeat(keys.size) {
            map[keys[it]] = it
        }
        map.removeValueIf { value -> value % 2 == 0 }
        assertEquals(keys.size / 2, map.size)
        for (i in 1 until keys.size step 2) {
            assertEquals(i, map[keys[i]], "map key $i")
        }
        for (i in 0 until keys.size step 2) {
            assertEquals(null, map[keys[i]], "map key $i")
        }
        map.removeValueIf { true }
        assertEquals(0, map.size, "map is not empty after removing everything")
    }

    @Test
    fun canRemoveKeys() {
        val map = IdentityArrayMap<Key, Int>()
        repeat(keys.size) {
            map[keys[it]] = it
        }
        map.removeIf { key, _ -> key.value % 2 == 0 }
        assertEquals(keys.size / 2, map.size)
        for (i in 1 until keys.size step 2) {
            assertEquals(i, map[keys[i]], "map key $i")
        }
        for (i in 0 until keys.size step 2) {
            assertEquals(null, map[keys[i]], "map key $i")
        }
        map.removeIf { _, _ -> true }
        assertEquals(0, map.size, "map is not empty after removing everything")
    }

    @Test
    fun canForEachKeysAndValues() {
        val map = IdentityArrayMap<Key, String>()
        repeat(100) {
            map[keys[it]] = it.toString()
        }
        assertEquals(100, map.size)
        var count = 0
        map.forEach { key, value ->
            assertEquals(key.value.toString(), value, "map key ${key.value}")
            count++
        }
        assertEquals(map.size, count, "forEach didn't loop the expected number of times")
    }

    @Test
    fun canRemoveItems() {
        val map = IdentityArrayMap<Key, String>()
        repeat(100) {
            map[keys[it]] = it.toString()
        }

        repeat(100) {
            assertEquals(100 - it, map.size)
            val removed = map.remove(keys[it])
            assertEquals(removed, it.toString(), "Expected to remove $it for ${keys[it]}")
            if (it > 0) {
                assertNull(
                    map.remove(keys[it - 1]),
                    "Expected item ${it - 1} to already be removed"
                )
            }
        }
    }

    @Test // b/195621739
    fun canRemoveWhenFull() {
        val map = IdentityArrayMap<Key, String>()
        repeat(16) {
            map[keys[it]] = it.toString()
        }
        repeat(16) {
            val key = keys[it]
            val removed = map.remove(key)
            assertNotNull(removed)
            assertFalse(map.contains(key))
        }
        assertTrue(map.isEmpty())
    }

    @Test
    fun canClear() {
        val map = IdentityArrayMap<Key, String>()
        repeat(16) {
            map[keys[it]] = it.toString()
        }
        map.clear()
        assertTrue(map.isEmpty())
        assertEquals(0, map.size, "map size should be 0 after calling clear")
    }

    @Test
    fun asMap_create() {
        val count = 16
        val map = IdentityArrayMap<Key, String>()
        repeat(count) {
            map[keys[it]] = it.toString()
        }
        val mapInterface = map.asMap()
        assertNotNull(mapInterface)
    }

    @Test
    fun asMap_entries() {
        val count = 16
        val map = IdentityArrayMap<Key, String>()
        repeat(count) {
            map[keys[it]] = it.toString()
        }
        val mapInterface = map.asMap()
        val entries = mapInterface.entries
        assertNotNull(entries)
    }

    @Test
    fun asMap_entries_size() {
        val count = 16
        val map = IdentityArrayMap<Key, String>()
        repeat(count) {
            map[keys[it]] = it.toString()
        }
        val mapInterface = map.asMap()
        val entries = mapInterface.entries
        assertEquals(count, entries.size)
        map.remove(keys[5])
        assertEquals(count - 1, entries.size)
        map.clear()
        assertEquals(0, entries.size)
    }

    @Test
    fun asMap_entries_isEmpty() {
        val count = 16
        val map = IdentityArrayMap<Key, String>()
        repeat(count) {
            map[keys[it]] = it.toString()
        }
        val mapInterface = map.asMap()
        val entries = mapInterface.entries
        assertFalse(entries.isEmpty())
        map.clear()
        assertTrue(entries.isEmpty())
    }

    @Test
    fun asMap_entries_iterator() {
        val count = 16
        val map = IdentityArrayMap<Key, String>()
        repeat(count) {
            map[keys[it]] = it.toString()
        }
        val mapInterface = map.asMap()
        val entries = mapInterface.entries
        var counted_entries = 0
        for ((key, value) in entries) {
            assertEquals(map[key], value)
            counted_entries++
        }
        assertEquals(count, counted_entries)
    }

    @Test
    fun asMap_entries_containsAll() {
        val count = 16
        val map = IdentityArrayMap<Key, String>()
        repeat(count) {
            map[keys[it]] = it.toString()
        }
        val mapInterface = map.asMap()
        val entries = mapInterface.entries
        val entryInstances = entries.toList()
        val containsAllPositive = entries.containsAll(entryInstances)
        assertTrue(containsAllPositive)
        map.remove(keys[5])
        val containsAllNegative = entries.containsAll(entryInstances)
        assertFalse(containsAllNegative)
    }

    @Test
    fun asMap_entries_contains() {
        val count = 16
        val map = IdentityArrayMap<Key, String>()
        repeat(count) {
            map[keys[it]] = it.toString()
        }
        val mapInterface = map.asMap()
        val entries = mapInterface.entries
        val entryInstances = entries.toList()
        val containsPositive = entries.contains(entryInstances[5])
        assertTrue(containsPositive)
        map.remove(keys[5])
        val entryFive = entryInstances.first { it.key == keys[5] }
        val containsNegative = entries.contains(entryFive)
        assertFalse(containsNegative)
    }

    @Test
    fun asMap_keys() {
        val count = 16
        val map = IdentityArrayMap<Key, String>()
        repeat(count) {
            map[keys[it]] = it.toString()
        }
        val mapInterface = map.asMap()
        val keys = mapInterface.keys
        assertNotNull(keys)
    }

    @Test
    fun asMap_keys_size() {
        val count = 16
        val map = IdentityArrayMap<Key, String>()
        repeat(count) {
            map[keys[it]] = it.toString()
        }
        val mapInterface = map.asMap()
        val iKeys = mapInterface.keys
        assertEquals(count, iKeys.size)
        map.remove(keys[5])
        assertEquals(count - 1, iKeys.size)
    }

    @Test
    fun asMap_keys_isEmpty() {
        val count = 16
        val map = IdentityArrayMap<Key, String>()
        repeat(count) {
            map[keys[it]] = it.toString()
        }
        val mapInterface = map.asMap()
        val keys = mapInterface.keys
        assertFalse(keys.isEmpty())
        map.clear()
        assertTrue(keys.isEmpty())
    }

    @Test
    fun asMap_keys_iterator() {
        val count = 16
        val map = IdentityArrayMap<Key, String>()
        repeat(count) {
            map[keys[it]] = it.toString()
        }
        val mapInterface = map.asMap()
        val keys = mapInterface.keys
        var counted_keys = 0
        for (key in keys) {
            assertTrue(map.contains(key))
            counted_keys++
        }
        assertEquals(count, counted_keys)
    }

    @Test
    fun asMap_keys_containsAll() {
        val count = 16
        val map = IdentityArrayMap<Key, String>()
        repeat(count) {
            map[keys[it]] = it.toString()
        }
        val mapInterface = map.asMap()
        val iKeys = mapInterface.keys
        val keyInstances = iKeys.toList()
        assertTrue(iKeys.containsAll(keyInstances))
        map.remove(keys[5])
        assertFalse(iKeys.containsAll(keyInstances))
    }

    @Test
    fun asMap_keys_contains() {
        val count = 16
        val map = IdentityArrayMap<Key, String>()
        repeat(count) {
            map[keys[it]] = it.toString()
        }
        val mapInterface = map.asMap()
        val iKeys = mapInterface.keys
        assertTrue(iKeys.contains(keys[5]))
        map.remove(keys[5])
        assertFalse(iKeys.contains(keys[5]))
    }

    @Test
    fun asMap_size() {
        val count = 16
        val map = IdentityArrayMap<Key, String>()
        repeat(count) {
            map[keys[it]] = it.toString()
        }
        val mapInterface = map.asMap()
        assertEquals(count, mapInterface.size)
        map.remove(keys[5])
        assertEquals(count - 1, mapInterface.size)
    }

    @Test
    fun asMap_values() {
        val count = 16
        val map = IdentityArrayMap<Key, String>()
        repeat(count) {
            map[keys[it]] = it.toString()
        }
        val mapInterface = map.asMap()
        val values = mapInterface.values
        assertNotNull(values)
    }

    @Test
    fun asMap_values_size() {
        val count = 16
        val map = IdentityArrayMap<Key, String>()
        repeat(count) {
            map[keys[it]] = it.toString()
        }
        val mapInterface = map.asMap()
        val values = mapInterface.values
        assertEquals(count, values.size)
        map.remove(keys[5])
        assertEquals(count - 1, values.size)
        map.clear()
        assertEquals(0, values.size)
    }

    @Test
    fun asMap_values_isEmpty() {
        val count = 16
        val map = IdentityArrayMap<Key, String>()
        repeat(count) {
            map[keys[it]] = it.toString()
        }
        val mapInterface = map.asMap()
        val values = mapInterface.values
        assertFalse(values.isEmpty())
        map.clear()
        assertTrue(values.isEmpty())
    }

    @Test
    fun asMap_values_iterator() {
        val count = 16
        val map = IdentityArrayMap<Key, String>()
        repeat(count) {
            map[keys[it]] = it.toString()
        }
        val mapInterface = map.asMap()
        val values = mapInterface.values
        var countedValues = 0
        for (value in values) {
            var found = false
            map.forEach { _, mapValue ->
                if (value == mapValue) found = true
            }
            assertTrue(found)
            countedValues++
        }
        assertEquals(count, countedValues)
    }

    @Test
    fun asMap_values_containsAll() {
        val count = 16
        val map = IdentityArrayMap<Key, String>()
        repeat(count) {
            map[keys[it]] = it.toString()
        }
        val mapInterface = map.asMap()
        val values = mapInterface.values
        val valueInstances = values.toList()
        assertTrue(values.containsAll(valueInstances))
        map.remove(keys[5])
        assertFalse(values.containsAll(valueInstances))
    }

    @Test
    fun asMap_values_contains() {
        val count = 16
        val map = IdentityArrayMap<Key, String>()
        repeat(count) {
            map[keys[it]] = it.toString()
        }
        val mapInterface = map.asMap()
        val values = mapInterface.values
        assertTrue(values.contains("5"))
        map.remove(keys[5])
        assertFalse(values.contains("5"))
    }

    @Test
    fun asMap_isEmpty() {
        val count = 16
        val map = IdentityArrayMap<Key, String>()
        repeat(count) {
            map[keys[it]] = it.toString()
        }
        val mapInterface = map.asMap()
        assertFalse(mapInterface.isEmpty())
        map.clear()
        assertTrue(mapInterface.isEmpty())
    }

    @Test
    fun asMap_get() {
        val count = 16
        val map = IdentityArrayMap<Key, String>()
        repeat(count) {
            map[keys[it]] = it.toString()
        }
        val mapInterface = map.asMap()
        for (key in keys.take(count)) {
            assertEquals(map[key], mapInterface[key])
        }
    }

    @Test
    fun asMap_containsValue() {
        val count = 16
        val map = IdentityArrayMap<Key, String>()
        repeat(count) {
            map[keys[it]] = it.toString()
        }
        val mapInterface = map.asMap()
        for (i in 0 until count) {
            assertTrue(mapInterface.containsValue(i.toString()))
        }
        assertFalse(mapInterface.containsValue("not there"))
    }

    @Test
    fun asMap_containsKey() {
        val count = 16
        val map = IdentityArrayMap<Key, String>()
        repeat(count) {
            map[keys[it]] = it.toString()
        }
        val mapInterface = map.asMap()
        repeat(count) {
            assertTrue(mapInterface.containsKey(keys[it]))
        }
        assertFalse(mapInterface.containsKey(keys[count]))
    }
}