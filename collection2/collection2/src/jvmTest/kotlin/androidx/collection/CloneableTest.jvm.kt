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

package androidx.collection

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame

class CloneableTest {
    @Test
    fun longSparseArrayCloneTest() {
        val source = LongSparseArray<String>()
        source.put(42, "cuarenta y dos")
        source.put(3, "tres")
        source.put(11, "once")
        val dest = source.clone()
        assertNotSame(source, dest)

        assertEquals(source.size, dest.size)
        for (i in 0 until source.size) {
            assertEquals(source.keyAt(i), dest.keyAt(i))
            assertEquals(source.valueAt(i), dest.valueAt(i))
        }
    }

    @Test
    fun sparseArrayCloneTest() {
        val source = SparseArray<String>()
        source.put(42, "cuarenta y dos")
        source.put(3, "tres")
        source.put(11, "once")
        val dest = source.clone()
        assertNotSame(source, dest)

        assertEquals(source.size, dest.size)
        for (i in 0 until source.size) {
            assertEquals(source.keyAt(i), dest.keyAt(i))
            assertEquals(source.valueAt(i), dest.valueAt(i))
        }
    }

    @Suppress("DEPRECATION") // For SpareArrayCompat usage
    @Test
    fun sparseArrayCompatCloneTest() {
        val source = SparseArrayCompat<String>()
        source.put(42, "cuarenta y dos")
        source.put(3, "tres")
        source.put(11, "once")
        val dest = source.clone()
        assertNotSame(source, dest)

        assertEquals(source.size, dest.size)
        for (i in 0 until source.size) {
            assertEquals(source.keyAt(i), dest.keyAt(i))
            assertEquals(source.valueAt(i), dest.valueAt(i))
        }
    }
}