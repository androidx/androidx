/*
 * Copyright 2022 The Android Open Source Project
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

import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State

@State(Scope.Benchmark)
open class LruCacheBenchmark {
    private val source = Array(10_000) { "value $it" }

    @Benchmark
    open fun allHits() {
        val cache = LruCache<String, String>(source.size)
        for (key in source) {
            cache.put(key, key)
        }
        for (key in source) {
            val value = cache[key]
            assertEquals(key, value)
        }
        assertEquals(source.size, cache.hitCount())
        assertEquals(0, cache.missCount())
    }

    @Benchmark
    open fun allMisses() {
        val cache = LruCache<String, String>(source.size)
        for (key in source) {
            val value = cache[key]
            assertNull(value)
        }
        assertEquals(0, cache.hitCount())
        assertEquals(source.size, cache.missCount())
    }

    @Benchmark
    open fun customCreate() {
        val cache = object : LruCache<String, String>(2) {
            override fun create(key: String): String {
                return "value_$key"
            }
        }

        cache.put("foo", "1")
        cache.put("bar", "1")
        val value = cache["baz"]
        assertEquals("value_baz", value)
        assertEquals(2, cache.size())
        assertEquals(0, cache.hitCount())
        assertEquals(1, cache.missCount())

        val value2 = cache["baz"]
        assertEquals("value_baz", value2)
        assertEquals(1, cache.hitCount())
        assertEquals(1, cache.missCount())
    }
}
