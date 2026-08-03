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

package androidx.compose.runtime.tracing.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.runtime.tracing.Stack
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StackBenchmark {
    @get:Rule val benchmarkRule = BenchmarkRule()

    @Test
    fun jdkReferenceBenchmark() {
        val instance = Any()
        val stack = java.util.Stack<Any>()
        benchmarkRule.measureRepeated {
            repeat(256) { stack.push(instance) }
            repeat(times = 256) { stack.pop() }
        }
    }

    @Test
    fun arrayDequeAsStackBenchmark() {
        val instance = Any()
        val stack = ArrayDeque<Any>(initialCapacity = 128)
        benchmarkRule.measureRepeated {
            repeat(256) { stack += instance }
            repeat(times = 256) { stack.removeLastOrNull() }
        }
    }

    @Test
    fun arrayListAsStackBenchmark() {
        val instance = Any()
        val stack = ArrayList<Any>(128)
        benchmarkRule.measureRepeated {
            repeat(256) { stack += instance }
            repeat(times = 256) { stack.removeLastOrNull() }
        }
    }

    @Test
    fun stackBenchmark() {
        val instance = Any()
        val stack = Stack<Any>(blkCount = 2)
        benchmarkRule.measureRepeated {
            repeat(256) { stack += instance }
            repeat(times = 256) { stack.removeLastOrNull() }
        }
    }
}
