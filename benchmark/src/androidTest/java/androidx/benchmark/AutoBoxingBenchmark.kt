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

package androidx.benchmark

import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@LargeTest
@RunWith(JUnit4::class)
class AutoBoxingBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun integerArtCacheAllocBenchmark() {
        var i = Integer(1000)
        benchmarkRule.measure {
            if (i < 100) {
                i = Integer(i.toInt() + 1)
            } else {
                i = Integer(0)
            }
        }
    }

    @Test
    fun integerAllocBenchmark() {
        var i = Integer(1000)
        benchmarkRule.measure {
            if (i < 1100) {
                i = Integer(i.toInt() + 1)
            } else {
                i = Integer(1000)
            }
        }
    }
}
