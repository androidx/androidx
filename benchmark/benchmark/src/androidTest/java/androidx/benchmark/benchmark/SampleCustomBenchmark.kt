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

import androidx.benchmark.ExperimentalBenchmarkConfigApi
import androidx.benchmark.MetricCapture
import androidx.benchmark.MicrobenchmarkConfig
import androidx.benchmark.ProfilerConfig
import androidx.benchmark.TimeCapture
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalBenchmarkConfigApi::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
class SampleCustomBenchmark {
    var counter: Long = 0

    @get:Rule
    val benchmarkRule = BenchmarkRule(
        // Ideally, this config object would be specified per test, but that requires non-trivial
        // changes to BenchmarkRule/BenchmarkState, and can be explored later
        MicrobenchmarkConfig(
            metrics = listOf(
                TimeCapture(), // put TimeCapture early to prioritize it (starts last, stops first)
                object : MetricCapture(
                    // list of names of submetrics this capture will produce
                    listOf("customCounter", "surpriseZero")
                ) {
                    var valueAtPause: Long = 0
                    var pausedOffset: Long = 0

                    override fun captureStart(timeNs: Long) {
                        counter = 0
                    }

                    override fun captureStop(timeNs: Long, output: LongArray, offset: Int) {
                        output[offset] = counter - pausedOffset

                        // note that this number that's reported isn't the metric for one iter, but
                        // for N iters, and is divided on your behalf. This means with a quick
                        // benchmark like the one below, you might see results like:
                        // SampleCustomBenchmark.sample
                        // timeNs            min 25.1,   median 25.1,   max 25.4
                        // customCounter     min 20.0,   median 20.0,   max 20.0
                        // surpriseZero      min  0.0,   median  0.0,   max  0.0
                        // allocationCount   min  0.0,   median  0.0,   max  0.0
                        output[offset + 1] = 1000
                    }

                    override fun capturePaused() {
                        valueAtPause = counter
                    }

                    override fun captureResumed() {
                        pausedOffset += counter - valueAtPause
                    }
                }
            ),
            profiler = ProfilerConfig.MethodTracing()
        )
    )
    @Test
    fun sample() {
        benchmarkRule.measureRepeated {
            repeat(20) {
                counter++
            }
            runWithTimingDisabled {
                counter++ // this is ignored, so customCounter output is simply 20
            }
        }
    }
}
