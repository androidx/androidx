/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.benchmark.perfetto

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class RowTest {
    @Test
    fun basic() {
        val map = mapOf<String, Any?>("name" to "Name", "ts" to 0L, "dur" to 1L)
        val row = rowOf("name" to "Name", "ts" to 0L, "dur" to 1L)

        assertEquals(map, row)
        assertEquals(map.hashCode(), row.hashCode())
        assertEquals(map.toString(), row.toString())
    }

    @Test
    fun gettersSetters() {
        val row = rowOf(
            "string" to "foo",
            "double" to 0.0,
            "long" to 1L,
            "bytes" to byteArrayOf(0x00, 0x01),
            "null" to null
        )

        assertEquals("foo", row.string("string"))
        assertEquals("foo", row.nullableString("string"))
        assertContentEquals(byteArrayOf(0x00, 0x01), row.bytes("bytes"))
        assertContentEquals(byteArrayOf(0x00, 0x01), row.nullableBytes("bytes"))
        assertEquals(0.0, row.double("double"))
        assertEquals(0.0, row.nullableDouble("double"))
        assertEquals(1L, row.long("long"))
        assertEquals(1L, row.nullableLong("long"))

        assertNull(row.nullableString("null"))
        assertNull(row.nullableBytes("null"))
        assertNull(row.nullableDouble("null"))
        assertNull(row.nullableLong("null"))
    }
}
