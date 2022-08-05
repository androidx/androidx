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
open class SimpleArrayMapBenchmark {
    private val source = List(10_000) { "key $it" to "value $it" }.toMap()

    @Benchmark
    open fun addAllThenRemoveIndividually() {
        val map = SimpleArrayMap<String, String>(source.size)
        for (entry in source) {
            map.put(entry.key, entry.value)
        }

        assertEquals(source.size, map.size())

        for (key in source.keys) {
            map.remove(key)
        }

        assertTrue(map.isEmpty())
    }
}
