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

package androidx.compose.runtime.internal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class IdentityHashCodeTest {

    @Test
    fun smokeTest() {
        val a = DefaultImpl()
        val b = DefaultImpl()

        assertEquals(a, a)
        assertNotEquals(a, b)
        assertNotEquals(b, a)

        val set = mutableSetOf<DefaultImpl>()
        set.add(a)
        set.add(a)
        set.add(b)

        assertEquals(set.size, 2)
    }
}

private class DefaultImpl {
    override fun equals(other: Any?): Boolean {
        return this === other
    }

    override fun hashCode(): Int {
        return identityHashCode(this)
    }
}
