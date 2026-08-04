/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.runtime.tracing.test

import androidx.compose.runtime.tracing.Pool
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PoolTest {
    @Test
    fun basicPooling() {
        val capacity = 2
        var remaining = 2
        val pool =
            Pool(capacity = capacity) {
                require(remaining >= 0)
                remaining -= 1
                Any()
            }
        val elements = mutableListOf<Any>()
        elements += pool.obtain()
        elements += pool.obtain()
        pool.release(elements.removeAt(elements.lastIndex))
        remaining += 1
        pool.release(elements.removeAt(elements.lastIndex))
        remaining += 1
        assertEquals(2, remaining)
        assertEquals(1, pool.lastIdx)
    }

    @Test
    fun poolingWithFallback() {
        val pool = Pool(capacity = 1) { Any() }
        val elements = mutableListOf<Any>()
        elements += pool.obtain()
        assertEquals(-1, pool.lastIdx)
        elements += assertNotNull(pool.obtain())
        elements.forEach { pool.release(it) }
        assertEquals(0, pool.lastIdx)
    }
}
