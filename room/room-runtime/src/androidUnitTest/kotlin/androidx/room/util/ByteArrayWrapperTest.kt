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

package androidx.room.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ByteArrayWrapperTest {
    @Test
    fun validateEqualsAndHashcodeContracts() {
        val array1 = byteArrayOf(1, 2, 3)
        val array2 = byteArrayOf(1, 2, 3)
        val array3 = byteArrayOf(4, 5, 6)

        val wrapper1 = ByteArrayWrapper(array1)
        val wrapper2 = ByteArrayWrapper(array2)
        val wrapper3 = ByteArrayWrapper(array3)

        // Check Equals Contract
        // Reflexivity
        assertEquals(wrapper1, wrapper1)

        // Symmetry
        assertEquals(wrapper1, wrapper2)
        assertEquals(wrapper2, wrapper1)

        // Transitivity
        val wrapper4 = ByteArrayWrapper(array1.clone())
        assertEquals(wrapper1, wrapper2)
        assertEquals(wrapper2, wrapper4)
        assertEquals(wrapper1, wrapper4)

        // Consistency
        assertEquals(wrapper1, wrapper2)
        assertEquals(wrapper1, wrapper2)

        // Check HashCode Contract
        assertEquals(wrapper1.hashCode(), wrapper1.hashCode())

        val hashCode1 = wrapper1.hashCode()
        array1[0] = 4 // Modifying the underlying array should change hash code
        assertNotEquals(hashCode1, wrapper1.hashCode())

        // Equal objects, equal hash codes
        assertEquals(wrapper1.hashCode(), ByteArrayWrapper(array1).hashCode())

        // Unequal objects, unequal hash codes
        assertNotEquals(wrapper1.hashCode(), wrapper3.hashCode())
    }
}
