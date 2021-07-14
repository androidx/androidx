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

package androidx.paging

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ItemSnapshotListTest {

    @Test
    fun uncounted() {
        val snapshot = ItemSnapshotList(0, 0, List(10) { it })
        assertEquals(List<Int?>(10) { it }, snapshot)
        assertFailsWith<IndexOutOfBoundsException> { snapshot[-1] }
        assertFailsWith<IndexOutOfBoundsException> { snapshot[10] }
    }

    @Test
    fun counted() {
        val snapshot = ItemSnapshotList(5, 5, List(10) { it })
        assertEquals(List(5) { null } + List(10) { it } + List(5) { null }, snapshot)
        assertFailsWith<IndexOutOfBoundsException> { snapshot[-1] }
        assertFailsWith<IndexOutOfBoundsException> { snapshot[20] }
    }
}