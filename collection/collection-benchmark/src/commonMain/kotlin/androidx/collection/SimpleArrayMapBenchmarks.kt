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

internal class SimpleArrayMapCreateBenchmark(
    private val sourceMap: Map<Int, String>,
) : CollectionBenchmark {
    override fun measuredBlock() {
        val map = SimpleArrayMap<Int, String>()
        for ((key, value) in sourceMap) {
            map.put(key, value)
        }
    }
}

internal class SimpleArrayMapContainsKeyBenchmark(
    sourceMap: Map<Int, String>,
) : CollectionBenchmark {
    // Split the source map into two lists, one with elements in the created map, one not.
    val src = sourceMap.toList()
    val inList = src.slice(0 until src.size / 2)
    val inListKeys = inList.map { it.first }
    val outListKeys = src.slice(src.size / 2 until src.size).map { it.first }

    val map = SimpleArrayMap<Int, String>()

    init {
        for ((key, value) in inList) {
            map.put(key, value)
        }
    }

    override fun measuredBlock() {
        for (key in inListKeys) {
            if (!map.containsKey(key)) {
                throw AssertionError("Should never get here")
            }
        }

        for (key in outListKeys) {
            if (map.containsKey(key)) {
                throw AssertionError("Should never get here")
            }
        }
    }
}

internal class SimpleArrayMapAddAllThenRemoveIndividuallyBenchmark(
    private val sourceMap: Map<Int, String>
) : CollectionBenchmark {
    val sourceSimpleArrayMap = SimpleArrayMap<Int, String>(sourceMap.size)

    init {
        for ((key, value) in sourceMap) {
            sourceSimpleArrayMap.put(key, value)
        }
    }

    var map = SimpleArrayMap<Int, String>(sourceSimpleArrayMap.size())

    override fun measuredBlock() {
        map.putAll(sourceSimpleArrayMap)
        for (key in sourceMap.keys) {
            map.remove(key)
        }
    }
}

internal fun createSourceMap(size: Int, sparse: Boolean): Map<Int, String> {
    return mutableMapOf<Int, String>().apply {
        val keyFactory: () -> Int =
            if (sparse) {
                // Despite the fixed seed, the algorithm which produces random values may vary
                // across
                // OS versions. Since we're not doing cross-device comparison this is acceptable.
                val random = Random(0);
                {
                    val value: Int
                    while (true) {
                        val candidate = random.nextInt()
                        if (candidate !in this) {
                            value = candidate
                            break
                        }
                    }
                    value
                }
            } else {
                var value = 0
                { value++ }
            }
        repeat(size) {
            val key = keyFactory()
            this.put(key, "value of $key")
        }
        check(size == this.size)
    }
}
