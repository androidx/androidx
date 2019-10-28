/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.benchmark.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@LargeTest
@RunWith(Parameterized::class)
class ParameterizedBenchmark(
    @Suppress("unused") private val input: Int,
    @Suppress("unused") private val stringInput: String
) {
    companion object {
        @JvmStatic
        @Parameters(name = "size={0},str:{1}")
        fun data(): Collection<Array<Any>> = List(2) { arrayOf(it, "$it=:") }
    }

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun noop() = benchmarkRule.measureRepeated {}
}
