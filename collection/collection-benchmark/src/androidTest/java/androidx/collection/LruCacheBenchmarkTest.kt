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

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

private class MyCache(maxSize: Int) : LruCache<Int, String>(maxSize) {
    override fun create(key: Int): String? = "value of $key"
}

@RunWith(Parameterized::class)
class LruCacheBenchmarkTest(val size: Int) {

    val keyList = (0 until size).toList()

    @get:Rule
    val benchmark = BenchmarkRule()

    @Test
    fun createThenFetchWithAllHits() {
        benchmark.measureRepeated {
            val cache = MyCache(size)
            for (e in keyList) {
                cache.get(e)
            }

            for (e in keyList) {
                cache.get(e)
            }
        }
    }

    @Test
    fun allMisses() {
        benchmark.measureRepeated {
            val cache = MyCache(size / 2)
            for (e in keyList) {
                cache.get(e)
            }
        }
    }

    companion object {
        @JvmStatic
        @Parameters(name = "size={0}")
        fun parameters() = buildParameters(
            listOf(10, 100, 1000),
        )
    }
}
