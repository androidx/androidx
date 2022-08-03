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
import kotlin.test.assertTrue
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State

@State(Scope.Benchmark)
open class CircularArrayBenchmark {
    private fun generateOrderedStrings(size: Int): Array<String> {
        return Array(size) { "value $it" }
    }

    private val source = generateOrderedStrings(10_000)

    @Benchmark
    open fun addFromHeadAndPopFromTail() {
        val array = CircularArray<String>(8)
        for (element in source) {
            array.addFirst(element)
        }

        assertEquals(source.count(), array.size())

        for (i in source.indices) {
            array.popLast()
        }

        assertTrue(array.isEmpty())
    }
}