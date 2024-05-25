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

import kotlin.random.Random

internal class SparseArrayGetBenchmark(
    private val map: SparseArrayCompat<String>,
) : CollectionBenchmark {
    val lastKey = map.keyAt(map.size() - 1)

    override fun measuredBlock() {
        map.get(lastKey)
    }
}

internal class SparseArrayContainsKeyBenchmark(
    private val map: SparseArrayCompat<String>,
) : CollectionBenchmark {
    val lastKey = map.keyAt(map.size() - 1)

    override fun measuredBlock() {
        map.containsKey(lastKey)
    }
}

internal class SparseArrayIndexOfKeyBenchmark(
    private val map: SparseArrayCompat<String>,
) : CollectionBenchmark {
    val lastKey = map.keyAt(map.size() - 1)

    override fun measuredBlock() {
        map.indexOfKey(lastKey)
    }
}

internal class SparseArrayIndexOfValueBenchmark(
    private val map: SparseArrayCompat<String>,
) : CollectionBenchmark {
    val lastValue = map.valueAt(map.size() - 1)

    override fun measuredBlock() {
        map.indexOfValue(lastValue)
    }
}

internal fun createFilledSparseArray(size: Int, sparse: Boolean): SparseArrayCompat<String> {
    return SparseArrayCompat<String>().apply {
        val keyFactory: () -> Int =
            if (sparse) {
                // Despite the fixed seed, the algorithm which produces random values may vary
                // across
                // OS versions. Since we're not doing cross-device comparison this is acceptable.
                val random = Random(0);
                {
                    val key: Int
                    while (true) {
                        val candidate = random.nextInt()
                        if (candidate !in this) {
                            key = candidate
                            break
                        }
                    }
                    key
                }
            } else {
                var key = 0
                { key++ }
            }
        repeat(size) {
            val key = keyFactory()
            put(key, "value$key")
        }
        check(size == size())
    }
}
