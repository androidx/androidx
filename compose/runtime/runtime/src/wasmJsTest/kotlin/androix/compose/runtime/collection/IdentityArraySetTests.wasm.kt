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

import androidx.compose.runtime.*
import androidx.compose.runtime.collection.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * This is partial k/wasm-specific copy of [IdentityArraySetTest].
 * K/wasm can't reuse some common tests because of different [identityHashCode] implementation,
 * which doesn't guarantee unique values on k/wasm.
 * Not unique values are okay for the purpose of [IdentityArraySet], [IdentityArrayIntMap] and [IdentityArrayMap].
 * For details: https://kotlinlang.slack.com/archives/G010KHY484C/p1706547846376149
 */
@OptIn(InternalComposeApi::class)
class IdentityArraySetWasmTest {
    private val set: IdentityArraySet<IdentityArraySetTest.Stuff> = IdentityArraySet()

    private val list = listOf(
        IdentityArraySetTest.Stuff(10),
        IdentityArraySetTest.Stuff(12),
        IdentityArraySetTest.Stuff(1),
        IdentityArraySetTest.Stuff(30),
        IdentityArraySetTest.Stuff(10)
    )

    @Test
    fun addValueForward() {
        list.forEach { set.add(it) }
        assertEquals(list.size, set.size)
        var previousItem = set[0]
        for (i in 1 until set.size) {
            val item = set[i]
            assertTrue(identityHashCode(previousItem) <= identityHashCode(item))
            previousItem = item
        }
    }

    @Test
    fun addValueReversed() {
        list.asReversed().forEach { set.add(it) }
        assertEquals(list.size, set.size)
        var previousItem = set[0]
        for (i in 1 until set.size) {
            val item = set[i]
            assertTrue(identityHashCode(previousItem) <= identityHashCode(item))
            previousItem = item
        }
    }

    @Test
    fun addExistingValue() {
        list.forEach { set.add(it) }
        list.asReversed().forEach { set.add(it) }

        assertEquals(list.size, set.size)
        var previousItem = set[0]
        for (i in 1 until set.size) {
            val item = set[i]
            assertTrue(identityHashCode(previousItem) <= identityHashCode(item))
            previousItem = item
        }
    }
}