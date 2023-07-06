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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class ArraySetCompatTest {

    @Test
    fun testCanNotIteratePastEnd() {
        val set = ArraySet<String>()
        set.add("value")
        val iterator: Iterator<String> = set.iterator()
        assertTrue(iterator.hasNext())
        assertEquals("value", iterator.next())
        assertFalse(iterator.hasNext())

        assertFailsWith<NoSuchElementException> {
            iterator.next()
        }
    }
}
