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

internal class LruCacheCreateThenFetchWithAllHitsBenchmark(
    private val keyList: List<Int>,
    private val size: Int,
) : CollectionBenchmark {
    override fun measuredBlock() {
        val cache = MyCache(size)
        for (e in keyList) {
            cache.get(e)
        }

        for (e in keyList) {
            cache.get(e)
        }
    }
}

internal class LruCacheAllMissesBenchmark(
    private val keyList: List<Int>,
    private val size: Int,
) : CollectionBenchmark {
    override fun measuredBlock() {
        val cache = MyCache(size)
        for (e in keyList) {
            cache.get(e)
        }

        for (e in keyList) {
            cache.get(e)
        }
    }
}

private class MyCache(maxSize: Int) : LruCache<Int, String>(maxSize) {
    override fun create(key: Int): String? = "value of $key"
}

internal fun createKeyList(size: Int) = List(size) { it }
