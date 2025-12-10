/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.text

import kotlin.test.Test
import kotlin.test.assertEquals

class WeakKeysCacheTest {
    data class MyKey(val key: Int)
    data class MyValue(val value: String)

    // We can't call GC manually, so just testing the basic correctness
    @Test
    fun testCaching() {
        val cache = WeakKeysCache<MyKey, MyValue>()
        val key1 = MyKey(1)
        var created = 0

        val value1 = cache.getOrPut(key1) {
            created++
            MyValue("100")
        }
        assertEquals("100", value1.value)
        assertEquals(1, created)

        val value2 = cache.getOrPut(key1) { // using the same key1
            created++
            MyValue("200")
        }
        assertEquals("100", value2.value, "Should use cached")
        assertEquals(1, created, "Should use cached")

        val key2 = MyKey(1) // equal key
        val value3 = cache.getOrPut(key2) {
            created++
            MyValue("300")
        }
        // WeakMap uses identity, so k2 is a different key.
        assertEquals("100", value3.value)
        assertEquals(1, created)

        val key3 = MyKey(2) // new key
        val value4 = cache.getOrPut(key3) {
            created++
            MyValue("200")
        }

        assertEquals("200", value4.value)
        assertEquals(2, created)
    }
}
