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

import androidx.benchmark.BlackHole
import androidx.benchmark.ExperimentalBlackHoleApi
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlin.random.Random
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalBlackHoleApi::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
class BlackHoleBenchmark {
    @get:Rule val benchmarkRule = BenchmarkRule()

    @Test
    fun noBlackHole() {
        var double = Random.nextDouble()
        benchmarkRule.measureRepeated { repeat(10) { double /= 1.92837f } }
    }

    @Test
    fun falseBlackHole() {
        var double = Random.nextDouble()
        benchmarkRule.measureRepeated { repeat(10) { double /= 1.92837f } }
        println("double is $double")
    }

    @Test
    fun blackHole_inner() {
        var double = Random.nextDouble()
        benchmarkRule.measureRepeated {
            repeat(10) { double /= 1.92837f }
            BlackHole.consume(double)
        }
    }

    @Test
    fun blackHole_outer() {
        var double = Random.nextDouble()
        benchmarkRule.measureRepeated { repeat(10) { double /= 1.92837f } }
        BlackHole.consume(double)
    }
}
