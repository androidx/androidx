/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.serialization.schema

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [Reserved].
 */
class ReservedTest {
    @Test
    fun testContainsIds() {
        val reserved = Reserved(ids = setOf(1, 2))
        assertTrue(1 in reserved)
        assertTrue(2 in reserved)
        assertFalse(3 in reserved)
    }

    @Test
    fun testContainsNames() {
        val reserved = Reserved(names = setOf("foo", "bar"))
        assertTrue("foo" in reserved)
        assertTrue("bar" in reserved)
        assertFalse("quux" in reserved)
    }

    @Test
    fun testContainsIdRanges() {
        val reserved = Reserved(idRanges = setOf(1..10, 21..40))

        for (i in 1..10) {
            assertTrue(i in reserved)
        }

        for (i in 11..20) {
            assertFalse(i in reserved)
        }

        for (i in 21..40) {
            assertTrue(i in reserved)
        }
    }
}