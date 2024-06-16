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

internal class ArraySetCreateBenchmark(private val sourceSet: Set<Int>) : CollectionBenchmark {
    override fun measuredBlock() {
        ArraySet(sourceSet)
    }
}

internal class ArraySetContainsElementBenchmark(sourceSet: Set<Int>) : CollectionBenchmark {
    // Split the set into two lists, one with elements in the created set, one not.
    private val src = sourceSet.toList()
    private val inList = src.slice(0 until src.size / 2)
    private val outList = src.slice(src.size / 2 until src.size)

    private val set = ArraySet(inList)

    override fun measuredBlock() {
        for (e in inList) {
            if (e !in set) {
                throw AssertionError("Should never get here")
            }
        }

        for (e in outList) {
            if (e in set) {
                throw AssertionError("Should never get here")
            }
        }
    }
}

internal class ArraySetIndexOfBenchmark(sourceSet: Set<Int>) : CollectionBenchmark {
    // Split the set into two lists, one with elements in the created set, one not.
    private val src = sourceSet.toList()
    private val inList = src.slice(0 until src.size / 2)
    private val outList = src.slice(src.size / 2 until src.size)

    private val set = ArraySet(inList)

    override fun measuredBlock() {
        for (e in inList) {
            if (set.indexOf(e) < 0) {
                throw AssertionError("Should never get here")
            }
        }

        for (e in outList) {
            if (set.indexOf(e) >= 0) {
                throw AssertionError("Should never get here")
            }
        }
    }
}

internal class ArraySetAddAllThenRemoveIndividuallyBenchmark(private val sourceSet: Set<Int>) :
    CollectionBenchmark {
    private val set = ArraySet<Int>(sourceSet.size)

    override fun measuredBlock() {
        set.addAll(sourceSet)
        for (e in sourceSet) {
            set.remove(e)
        }
    }
}

internal fun createSourceSet(size: Int, sparse: Boolean): Set<Int> {
    return mutableSetOf<Int>().apply {
        val valueFactory: () -> Int =
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
        repeat(size) { this.add(valueFactory()) }
        check(size == this.size)
    }
}
