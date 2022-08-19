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

package androidx.collection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame

internal class SparseArrayCompatJvmTest {

    @Test
    fun cloning() {
        val source = SparseArrayCompat<String>()
        source.put(10, "hello")
        source.put(20, "world")

        val dest: SparseArrayCompat<String> = source.clone()
        assertNotSame(source, dest)

        for (i in 0 until source.size()) {
            assertEquals(source.keyAt(i), dest.keyAt(i))
            assertEquals(source.valueAt(i), dest.valueAt(i))
        }
    }

    @Test
    fun valueAt_outOfBounds() {
        val source = SparseArrayCompat<String>(10)
        assertEquals(0, source.size())

        assertFailsWith<ArrayIndexOutOfBoundsException> {
            source.valueAt(10000)
        }
    }
}
